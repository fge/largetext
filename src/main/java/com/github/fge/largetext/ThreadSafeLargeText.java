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
import com.google.common.base.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.file.Path;

/**
 * A large text file as a {@link CharSequence}: thread safe version
 *
 * <p>You will need to use an instance of this class, and not the non thread
 * safe one, if your {@code LargeText} instance can potentially be used by
 * several threads concurrently.</p>
 *
 * <p>In order to be thread safe, this implementation uses instances of an inner
 * class holding both the current text range and buffer in a {@link ThreadLocal}
 * variable.</p>
 *
 * @see LargeTextFactory#loadThreadSafe(Path)
 */
@ThreadSafe
@ParametersAreNonnullByDefault
public final class ThreadSafeLargeText
    extends LargeText
{
    private static final CurrentBuffer EMPTY_BUF
        = new CurrentBuffer(EMPTY_RANGE, EMPTY_BUFFER);
    private static final ThreadLocal<CurrentBuffer> CURRENT
        = new ThreadLocal<CurrentBuffer>()
        {
            @Override
            protected CurrentBuffer initialValue()
            {
                return EMPTY_BUF;
            }
        };

    ThreadSafeLargeText(final FileChannel channel, final Supplier<CharsetDecoder> decoderSupplier,
        final int quantity, final SizeUnit sizeUnit)
        throws IOException
    {
        super(channel, decoderSupplier, quantity, sizeUnit);
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
