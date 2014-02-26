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

package com.github.fge.largetext;

import com.github.fge.msgsimple.bundle.MessageBundle;
import com.github.fge.msgsimple.load.MessageBundles;

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
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static java.nio.channels.FileChannel.MapMode;

/*
 * TODO:
 *
 * - find a reasonable size for DEFAULT_MAPPING_SIZE
 * - calculate an optimum size for the CharBuffer used for decoding (probably
 *   by using CharsetDecoder's .averageCharsPerByte())
 * - deal with overly large files (ie, with more than Integer.MAX_VALUE chars)
 * - deal with small sizes
 * - implement .subsequence()
 * - optimize for character sets whose average bytes per char and max bytes per
 *   char are exactly the same
 *
 * FIXME: .length() is quite the nightmare; it requires to read the whole file
 * once. We cannot even optimize construction of 1-byte encodings because of
 * that! (or can we?)
 *
 * NOTE: if using .averageBytesPerChar(), remember to deal with an overflow
 * error. Right now, the created CharBuffer has the same length as the mapping
 * size, which is always enough; this is of course untrue for variable length
 * encodings, the most prominent of them being UTF-8.
 *
 * SCALING: right now, the whole file is read at build time. This is not good.
 *
 * Plan: use a thread to read the character windows; methods will wait on a
 * Condition on the number of characters read so far. One problem with this
 * approach is that no method in the `CharSequence` interface throws an
 * exception, so a RuntimeException will have to be thrown instead.
 */
public final class LargeTextFile
    implements CharSequence, Closeable
{
    private static final MessageBundle BUNDLE
        = MessageBundles.getBundle(LargeTextMessages.class);

    // 256K for now
    private static final long DEFAULT_MAPPING_SIZE = 1L << 18;

    /**
     * Charset used for decoding this file's contents
     */
    private final Charset charset;

    /**
     * {@link FileChannel} used for this file
     */
    private final FileChannel channel;

    private final long mappingSize;

    /**
     * Size of the channel
     */
    private final long fileSize;

    /**
     * Number of characters
     */
    private final int totalChars;

    /**
     * List of {@link CharWindow}s
     */
    private final List<CharWindow> windows = new ArrayList<>();

    public LargeTextFile(final String name, final Charset charset)
        throws IOException
    {
        mappingSize = DEFAULT_MAPPING_SIZE;
        this.charset = charset;
        channel = FileChannel.open(Paths.get(name), StandardOpenOption.READ);
        fileSize = channel.size();
        fillWindows();

        final CharWindow lastWindow = windows.get(windows.size() - 1);
        totalChars = lastWindow.getCharOffset() + lastWindow.getCharLength();
    }

    public LargeTextFile(final String name)
        throws IOException
    {
        this(name, StandardCharsets.UTF_8);
    }

    // For tests only
    LargeTextFile(final String name, final Charset charset,
        final long mappingSize)
        throws IOException
    {
        this.mappingSize = mappingSize;
        this.charset = charset;
        channel = FileChannel.open(Paths.get(name), StandardOpenOption.READ);
        fileSize = channel.size();
        fillWindows();

        final CharWindow lastWindow = windows.get(windows.size() - 1);
        totalChars = lastWindow.getCharOffset() + lastWindow.getCharLength();
    }

    @Override
    public int length()
    {
        return totalChars;
    }

    @Override
    public char charAt(final int index)
    {
        // TODO: argument checking
        final CharWindow window = getWindowForIndex(index);
        final CharBuffer buf;
        try {
            buf = bufferFromWindow(window);
            return buf.charAt(index - window.getCharOffset());
        } catch (IOException e) {
            throw new RuntimeException("I/O error when reading file mapping",
                e);
        }
    }

    @Override
    public CharSequence subSequence(final int start, final int end)
    {
        // TODO: argument checking
        final int index = getIndexOfStartWindow(start);

        final Iterator<CharWindow> iterator = windows.listIterator(index);

        CharWindow window = iterator.next();

        while (!window.containsRange(start, end))
            window = window.mergeWith(iterator.next());

        final int windowStart = window.getCharOffset();

        try {
            final CharBuffer buf;
            buf = bufferFromWindow(window);
            return buf.subSequence(start - windowStart, end - windowStart);
        } catch (IOException e) {
            throw new RuntimeException("I/O error when reading file mapping",
                e);
        }
    }

    @Override
    public void close()
        throws IOException
    {
        channel.close();
    }

    private void fillWindows()
        throws IOException
    {
        final CharsetDecoder decoder = charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);
        final CharBuffer buf = CharBuffer.allocate((int) mappingSize);

        long fileOffset = 0L, windowLength;
        int charOffset = 0;
        CharWindow window;

        while (fileOffset < fileSize) {
            window = readNextWindow(fileOffset, charOffset, decoder, buf);
            // FIXME
            windowLength = window.getWindowLength();
            if (windowLength == 0L)
                throw new IOException(BUNDLE.printf("err.invalidData",
                    fileOffset));
            windows.add(window);
            charOffset += window.getCharLength();
            fileOffset += windowLength;
        }

    }

    private CharWindow readNextWindow(final long fileOffset,
        final int charOffset, final CharsetDecoder decoder,
        final CharBuffer buf)
        throws IOException
    {
        long mapSize = Math.min(mappingSize, fileSize - fileOffset);

        final MappedByteBuffer mapping = channel.map(MapMode.READ_ONLY,
            fileOffset, mapSize);

        buf.rewind();
        decoder.reset();

        final CoderResult result = decoder.decode(mapping, buf, true);

        // FIXME
        if (result.isUnmappable())
            result.throwException();

        /*
         * Incomplete byte sequence: in this case, the mapping position reflects
         * what was actually read; change the mapping size
         */
        if (result.isMalformed())
            mapSize = (long) mapping.position();

        return new CharWindow(fileOffset, mapSize, charOffset,
            buf.position());
    }

    // NOTE: "malformed" indices to be detected in callers
    private CharWindow getWindowForIndex(final int index)
    {
        for (final CharWindow window: windows)
            if (window.containsCharAtIndex(index))
                return window;

        throw new IllegalStateException("should not have reached this point");
    }

    // NOTE: cannot fail at this point
    private CharBuffer bufferFromWindow(final CharWindow window)
        throws IOException
    {
        final MappedByteBuffer buffer = channel.map(MapMode.READ_ONLY,
            window.getFileOffset(), window.getWindowLength());
        return charset.newDecoder().decode(buffer);
    }

    private int getIndexOfStartWindow(final int start)
    {
        CharWindow window;
        for (int i = 0; i < windows.size(); i++) {
            window = windows.get(i);
            if (window.containsCharAtIndex(start))
                return i;
        }

        return -1;
    }
}
