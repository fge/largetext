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

import com.google.common.collect.Range;

public abstract class TextRangeCharSequence
    implements CharSequence
{
    protected final CharSequenceFactory factory;
    protected final Range<Integer> range;
    protected final int lowerBound;

    protected TextRangeCharSequence(final CharSequenceFactory factory,
        final Range<Integer> range)
    {
        this.factory = factory;
        this.range = range;
        lowerBound = range.lowerEndpoint();
    }

    @Override
    public final int length()
    {
        // Since ranges are always closed at the end, this works
        return range.upperEndpoint() - lowerBound;
    }

    @Override
    public final CharSequence subSequence(final int start, final int end)
    {
        final Range<Integer> targetRange
            = Range.closedOpen(lowerBound + start, lowerBound + end);
        if (!range.encloses(targetRange))
            throw new ArrayIndexOutOfBoundsException();
        if (targetRange.isEmpty())
            return EmptyCharSequence.INSTANCE;
        if (range.equals(targetRange))
            return this;
        return factory.getSequence(targetRange);
    }

    @Override
    public final char charAt(final int index)
    {
        final int realIndex = index + lowerBound;
        if (!range.contains(realIndex))
            throw new ArrayIndexOutOfBoundsException(index);
        return doCharAt(realIndex);
    }

    protected abstract char doCharAt(final int realIndex);
}
