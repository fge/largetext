/*
 * Copyright (c) 2014, Francis Galiegue (fgaliegue@gmail.com)
 *
 * This software is dual-licensed under:
 *
 * - the Lesser General Public License (LGPL) version 3.0 or, at your option, any
 *   later version;
 * - the Apache Software License (ASL) version 2.0.
 *
 * The text of both licenses is available under the src/resources/ directory of
 * this project (under the names LGPL-3.0.txt and ASL-2.0.txt respectively).
 *
 * Direct link to the sources:
 *
 * - LGPL 3.0: https://www.gnu.org/licenses/lgpl-3.0.txt
 * - ASL 2.0: http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package com.github.fge.largetext.load;

import com.github.fge.largetext.LargeText;
import com.github.fge.largetext.LargeTextFactory;
import com.github.fge.largetext.range.IntRange;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Closeable;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;


/**
 * Text file decoder
 *
 * <p>This is the first core class of this package (the second is {@link
 * TextCache}. Its role is to decode a text file chunk by chunk. The size of
 * chunks to use is determined when you build your {@link LargeTextFactory}.</p>
 *
 * <p>{@link LargeText} will call upon this class to obtain a {@link TextRange}
 * (or a list of them) containing the character at a given index (or the range
 * of characters), by using the methods {@link #getRange(int)} and {@link
 * #getRanges(IntRange)} respectively.</p>
 *
 * <p>These methods are blocking, but they <em>do not</em> throw {@link
 * InterruptedException}; if an interruption occurs, these methods reset the
 * thread interruption status and throw the appropriate {@link
 * RuntimeException} (for instance, an {@link ArrayIndexOutOfBoundsException} if
 * the requested offset exceeds the number of characters in the file).</p>
 *
 * <p>Implementation note: this class uses a <em>single threaded</em> {@link
 * ExecutorService} to perform the decoding operation. Decoding is not done in
 * parallel, and cannot be, since it is not guaranteeed that a byte mapping can
 * be decoded exactly to a character sequence (for instance, using UTF-8, the
 * end of the mapping may contain one byte only of a three-byte sequence).</p>
 *
 * @see DecodingStatus
 */
@ThreadSafe
public final class TextDecoder
    implements Closeable
{
    private static final ThreadFactory THREAD_FACTORY
        = new ThreadFactoryBuilder().setNameFormat("text-decoder").build();

    private final ExecutorService executor
        = Executors.newSingleThreadExecutor(THREAD_FACTORY);

    private final DecodingStatus status = new DecodingStatus();

    @GuardedBy("ranges")
    private final RangeMap<Integer, TextRange> ranges = TreeRangeMap.create();

    private final FileChannel channel;
    private final Charset charset;
    private final long fileSize;
    private final long targetMapSize;

    /**
     * Constructor; don't use directly!
     *
     * @param channel the {@link FileChannel} to the target file
     * @param charset the character encoding to use
     * @param targetMapSize the target byte mapping size
     * @throws IOException error obtaining information on the channel
     */
    public TextDecoder(final FileChannel channel, final Charset charset,
        final long targetMapSize)
        throws IOException
    {
        this.channel = channel;
        fileSize = channel.size();
        this.targetMapSize = targetMapSize;
        this.charset = charset;
        executor.submit(decodingTask());
    }

    /**
     * Return the appropriate text range containing the character at the given
     * offset
     *
     * @param charOffset the offset
     * @return the appropriate {@link TextRange}
     * @throws RuntimeException method has been interrupted, or a decoding error
     * has occurred
     * @throws ArrayIndexOutOfBoundsException offset requested is out of range
     */
    public TextRange getRange(final int charOffset)
    {
        try {
            needChars(charOffset + 1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted", e);
        }
        synchronized (ranges) {
            return ranges.get(charOffset);
        }
    }

    /**
     * Return an ordered iterable of text ranges covering the requested range
     *
     * @param range  the range
     * @return the appropriate list of text ranges
     * @throws RuntimeException method has been interrupted, or a decoding error
     * has occurred
     * @throws ArrayIndexOutOfBoundsException range is out of bounds for this
     * decoder
     */
    public List<TextRange> getRanges(final IntRange range)
    {
        try {
            needChars(range.getUpperBound());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted", e);
        }
        final Collection<TextRange> ret;
        synchronized (ranges) {
            ret = ranges.subRangeMap(range.asGuavaRange())
                .asMapOfRanges().values();
        }
        return ImmutableList.copyOf(ret);
    }

    /**
     * Return the total number of characters in this decoder
     *
     * <p>This method sleeps until the decoding operation finishes (either
     * successfully or with an error).</p>
     *
     * @return the total number of characters
     * @throws RuntimeException method has been interrupted, or a decoding error
     * has occurred
     *
     * @see DecodingStatus#getTotalSize()
     */
    public int getTotalChars()
    {
        return status.getTotalSize();
    }

    @Override
    public void close()
        throws IOException
    {
        executor.shutdownNow();
    }

    private void needChars(final int needed)
        throws InterruptedException
    {
        final CharWaiter waiter = new CharWaiter(needed);
        if (status.addWaiter(waiter))
            waiter.await();
    }

    // TODO: move to another class?
    private Runnable decodingTask()
    {
        return new Runnable()
        {
            @Override
            public void run()
            {
                final CharsetDecoder decoder = charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
                final CharBuffer charMap
                    = CharBuffer.allocate((int) targetMapSize);

                long byteOffset = 0L;
                int charOffset = 0;
                TextRange textRange;

                while (byteOffset < fileSize) {
                    try {
                        textRange = nextRange(byteOffset, charOffset, decoder,
                            charMap);
                        if (textRange.getByteRange().isEmpty())
                            throw new IOException("unable to read file as text "
                                + "starting from byte offset " + byteOffset);
                    } catch (IOException e) {
                        status.setFailed(e);
                        break;
                    }
                    byteOffset = textRange.getByteRange().getUpperBound();
                    charOffset = textRange.getCharRange().getUpperBound();
                    status.setNrChars(charOffset);
                    synchronized (ranges) {
                        ranges.put(textRange.getCharRange().asGuavaRange(),
                            textRange);
                    }
                }
                status.setFinished(charOffset);
            }

        };
    }

    private TextRange nextRange(final long byteOffset, final int charOffset,
        final CharsetDecoder decoder, final CharBuffer charMap)
        throws IOException
    {
        long nrBytes = Math.min(targetMapSize, fileSize - byteOffset);

        final MappedByteBuffer byteMap
            = channel.map(FileChannel.MapMode.READ_ONLY, byteOffset, nrBytes);

        charMap.rewind();
        decoder.reset();

        final CoderResult result = decoder.decode(byteMap, charMap, true);

        /*
         * Unmappable character... It _can_ happen even with a decoder, see
         * http://stackoverflow.com/a/22902806/1093528
         */
        if (result.isUnmappable())
            result.throwException();

        /*
         * Incomplete byte sequence: in this case, the mapping position reflects
         * what was actually read; change the mapping size
         */
        if (result.isMalformed())
            nrBytes = (long) byteMap.position();

        return new TextRange(byteOffset, nrBytes, charOffset,
            charMap.position());
    }
}
