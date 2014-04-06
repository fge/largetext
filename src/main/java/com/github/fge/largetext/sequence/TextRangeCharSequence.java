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

/**
 * One {@link CharSequence} built out of one or more {@link TextRange}s
 *
 * <p>Such a character sequence will always "carry along" the {@link
 * CharSequenceFactory} it has been created with, in order to be able to
 * create {@link #subSequence(int, int) subsequences} of itself.</p>
 *
 * @see CharSequenceFactory
 */
public abstract class TextRangeCharSequence
    implements CharSequence
{
    protected final IntRange range;
    protected final int lowerBound;

    /**
     * Protected constructor
     *
     * @param range the <em>absolute</em> requested range
     */
    protected TextRangeCharSequence(final IntRange range)
    {
        this.range = range;
        lowerBound = range.getLowerBound();
    }

    @Override
    public final int length()
    {
        // Since ranges are always closed at the end, this works
        return range.getUpperBound() - lowerBound;
    }

    @Override
    public final CharSequence subSequence(final int start, final int end)
    {
        final IntRange newRange
            = new IntRange(lowerBound + start, lowerBound + end);
        if (!range.encloses(newRange))
            throw new ArrayIndexOutOfBoundsException();
        if (newRange.isEmpty())
            return EmptyCharSequence.INSTANCE;
        if (range.equals(newRange))
            return this;
        return doSubSequence(newRange);
    }

    protected abstract CharSequence doSubSequence(
        final IntRange newRange);

    @Override
    public final char charAt(final int index)
    {
        final int realIndex = index + lowerBound;
        if (!range.contains(realIndex))
            throw new ArrayIndexOutOfBoundsException(index);
        return doCharAt(realIndex);
    }

    protected abstract char doCharAt(final int realIndex);

    @Override
    public abstract String toString();
}
