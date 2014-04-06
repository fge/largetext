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

package com.github.fge.largetext.sequence;

import com.github.fge.largetext.load.TextRange;
import com.github.fge.largetext.range.IntRange;

import java.nio.CharBuffer;

/**
 * A {@link CharSequence} spanning only one {@link TextRange}
 *
 * <p>Do not use directly!</p>
 */
public final class SingleTextRangeCharSequence
    extends TextRangeCharSequence
{
    private final CharBuffer buffer;
    private final IntRange bufferRange;

    /**
     * Constructor
     *
     * @param range the <em>absolute</em> requested range
     * @param bufferRange the <em>absolute</em> range of the buffer
     * @param buffer the character buffer matching this range
     */
    public SingleTextRangeCharSequence(final IntRange range,
        final IntRange bufferRange, final CharBuffer buffer)
    {
        super(range);
        this.buffer = buffer;
        this.bufferRange = bufferRange;
    }

    @Override
    protected char doCharAt(final int realIndex)
    {
        return buffer.charAt(realIndex - bufferRange.getLowerBound());
    }

    @Override
    protected CharSequence doSubSequence(final IntRange newRange)
    {
        return new SingleTextRangeCharSequence(newRange, bufferRange, buffer);
    }


    @Override
    public String toString()
    {
        final int start = range.getLowerBound() - bufferRange.getLowerBound();
        final int end = range.getUpperBound() - bufferRange.getLowerBound();
        return buffer.subSequence(start, end).toString();
    }
}
