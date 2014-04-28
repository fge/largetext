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

import com.github.fge.largetext.load.TextRange;
import com.github.fge.largetext.range.IntRange;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Closeable;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

/**
 * A large text file as a {@link CharSequence}
 *
 * <p>Do not create an instance of this class directly; instead, use a {@link
 * LargeTextFactory}.</p>
 *
 * <p><strong>Important note!</strong> This class implements {@link Closeable}
 * (and therefore {@link AutoCloseable}); the recommended use is therefore to
 * use it in a try-with-resources statement:</p>
 *
 * <pre>
 *     try (
 *         final LargeText largeText = factory.fromPath(somePath);
 *     ) {
 *         // use "largeText" here
 *     }
 * </pre>
 *
 * <p>Failing to close the instance correctly means you leak a file descriptor
 * to the text file you are using!</p>
 *
 * <p><strong>BIG FAT WARNING:</strong> getting the contents of a {@code
 * CharSequence} as a {@link String} using {@link Object#toString()} basically
 * dumps <strong>the contents of the whole file!</strong></p>
 *
 * @see LargeTextFactory
 */
@ThreadSafe
@ParametersAreNonnullByDefault
public final class ThreadSafeLargeText
    extends LargeText
{
    private static final ThreadLocal<CurrentBuffer> CURRENT
        = new ThreadLocal<>();

    static {
        CURRENT.set(new CurrentBuffer(EMPTY_RANGE, EMPTY_BUFFER));
    }

    ThreadSafeLargeText(final FileChannel channel, final Charset charset,
        final int quantity, final SizeUnit sizeUnit)
        throws IOException
    {
        super(channel, charset, quantity, sizeUnit);
    }

    @Override
    public char charAt(final int index)
    {
        final CurrentBuffer buf = CURRENT.get();
        if (buf.containsIndex(index))
            return buf.charAt(index);
        final TextRange textRange = decoder.getRange(index);
        final IntRange range = textRange.getCharRange();
        final CharBuffer buffer = loader.load(textRange);
        CURRENT.set(new CurrentBuffer(range, buffer));
        return buffer.charAt(index - range.getLowerBound());
    }

    private static final class CurrentBuffer
    {
        private final IntRange range;
        private final CharBuffer buffer;

        private CurrentBuffer(final IntRange range, final CharBuffer buffer)
        {
            this.range = range;
            this.buffer = buffer;
        }

        private boolean containsIndex(final int index)
        {
            return range.contains(index);
        }

        private char charAt(final int index)
        {
            return buffer.charAt(index - range.getLowerBound());
        }
    }
}
