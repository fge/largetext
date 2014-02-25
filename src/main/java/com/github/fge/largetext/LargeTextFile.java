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
import java.util.List;

// TODO!
public final class LargeTextFile
    implements CharSequence, Closeable
{
    // 256K for now
    private static final long DEFAULT_MAPPING_SIZE = 1L << 18;

    private final Charset charset;
    private final FileChannel channel;
    private final long fileSize;
    private final List<CharWindow> windows = new ArrayList<>();

    public LargeTextFile(final String name, final Charset charset)
        throws IOException
    {
        this.charset = charset;
        channel = FileChannel.open(Paths.get(name), StandardOpenOption.READ);
        fileSize = channel.size();
        fillWindows();
    }

    public LargeTextFile(final String name)
        throws IOException
    {
        this(name, StandardCharsets.UTF_8);
    }

    @Override
    public int length()
    {
        return 0;
    }

    @Override
    public char charAt(final int index)
    {
        return 0;
    }

    @Override
    public CharSequence subSequence(final int start, final int end)
    {
        return null;
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
        final CharBuffer buf = CharBuffer.allocate(1 << 18);

        long fileOffset = 0L, windowLength;
        int charOffset = 0;
        CharWindow window;

        while (fileOffset < fileSize) {
            window = readNextWindow(fileOffset, charOffset, decoder, buf);
            // FIXME
            windowLength = window.getWindowLength();
            if (windowLength == 0L)
                throw new IOException("Unable to read as text file from offset"
                     + fileOffset);
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
        long mappingSize = Math.min(fileOffset + DEFAULT_MAPPING_SIZE,
            fileSize);

        final MappedByteBuffer mapping
            = channel.map(FileChannel.MapMode.READ_ONLY, fileOffset,
                mappingSize);

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
            mappingSize = (long) mapping.position();

        return new CharWindow(fileOffset, mappingSize, charOffset,
            buf.position());
    }
}
