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

import com.github.fge.largetext.LargeTextMessages;
import com.github.fge.msgsimple.bundle.MessageBundle;
import com.github.fge.msgsimple.load.MessageBundles;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import javax.annotation.concurrent.GuardedBy;
import java.io.Closeable;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static java.nio.channels.FileChannel.*;

final class TextDecoder
    implements Closeable
{
    private static final MessageBundle BUNDLE
        = MessageBundles.getBundle(LargeTextMessages.class);

    private static final ThreadFactory THREAD_FACTORY
        = new ThreadFactoryBuilder().setDaemon(true).build();

    private final ExecutorService executor
        = Executors.newSingleThreadExecutor(THREAD_FACTORY);

    private final DecodingStatus status = new DecodingStatus();

    @GuardedBy("ranges")
    private final RangeMap<Integer, TextRange> ranges = TreeRangeMap.create();

    private final FileChannel channel;
    private final Charset charset;
    private final long fileSize;
    private final long targetMapSize;

    TextDecoder(final FileChannel channel, final Charset charset,
        final long targetMapSize)
        throws IOException
    {
        this.channel = channel;
        fileSize = channel.size();
        this.targetMapSize = targetMapSize;
        this.charset = charset;
        executor.submit(getTask());
    }

    private Runnable getTask()
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
                TextRange range;

                while (byteOffset < fileSize) {
                    try {
                        range = nextRange(byteOffset, charOffset, decoder,
                            charMap);
                        if (range.getByteRange().isEmpty())
                            throw new IOException(
                                BUNDLE.printf("err.invalidData", byteOffset));
                    } catch (IOException e) {
                        status.setFailed(e);
                        break;
                    }
                    byteOffset = range.getByteRange().upperEndpoint();
                    charOffset = range.getCharRange().upperEndpoint();
                    status.setNrChars(charOffset);
                    synchronized (ranges) {
                        ranges.put(range.getCharRange(), range);
                    }
                }
                status.setFinished(charOffset);
            }

        };
    }

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

    public Iterable<TextRange> getRanges(final int start, final int end)
    {
        final Range<Integer> charRange = Range.closedOpen(start, end);
        try {
            needChars(end);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted", e);
        }
        final Iterable<TextRange> ret;
        synchronized (ranges) {
            ret = ranges.subRangeMap(charRange).asMapOfRanges().values();
        }
        return ImmutableList.copyOf(ret);
    }

    @Override
    public void close()
        throws IOException
    {
        executor.shutdown();
    }

    private void needChars(final int needed)
        throws InterruptedException
    {
        final CharWaiter waiter = new CharWaiter(needed);
        if (status.addWaiter(waiter))
            waiter.await();
    }

    private TextRange nextRange(final long byteOffset, final int charOffset,
        final CharsetDecoder decoder, final CharBuffer charMap)
        throws IOException
    {
        long nrBytes = Math.min(targetMapSize, fileSize - byteOffset);

        final MappedByteBuffer byteMap
            = channel.map(MapMode.READ_ONLY, byteOffset, nrBytes);

        charMap.rewind();
        decoder.reset();

        final CoderResult result = decoder.decode(byteMap, charMap, true);

        // FIXME
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

    public static void main(final String... args)
        throws IOException
    {
        final Path path = Paths.get("/usr/share/dict/words");
        final FileChannel channel = FileChannel.open(path,
            StandardOpenOption.READ);
        final TextDecoder decoder = new TextDecoder(channel,
            StandardCharsets.UTF_8, 20_000L);
        System.out.println(decoder.getRange(298389));
    }
}
