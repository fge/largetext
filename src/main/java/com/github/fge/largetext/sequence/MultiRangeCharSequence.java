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
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A {@link CharSequence} spanning more than one {@link TextRange}
 *
 * <p>Don't use directly!</p>
 */
@Immutable
@ParametersAreNonnullByDefault
public final class MultiRangeCharSequence
    implements CharSequence
{
    private final IntRange range;
    private final int lowerBound;
    private final CharSequenceFactory factory;
    private final RangeMap<Integer, CharBuffer> rangeMap;

    /**
     * Constructor
     *
     * @param factory the char sequence factory to use (for {@link
     * #subSequence(int, int)})
     * @param range the requested <em>absolute</em> range
     * @param rangeMap map of absolute ranges and their matching char buffers
     */
    public MultiRangeCharSequence(final CharSequenceFactory factory,
        final IntRange range, final RangeMap<Integer, CharBuffer> rangeMap)
    {
        this.range = Objects.requireNonNull(range, "range cannot be null");
        lowerBound = range.getLowerBound();
        this.factory = Objects.requireNonNull(factory,
            "factory cannot be null");
        this.rangeMap = Objects.requireNonNull(rangeMap,
            "range map cannot be null");
    }

    @Override
    public char charAt(final int index)
    {
        final int realIndex = index + lowerBound;
        if (!range.contains(realIndex))
            throw new IndexOutOfBoundsException(index + " out of range");
        final Map.Entry<Range<Integer>, CharBuffer> entry
            = rangeMap.getEntry(realIndex);
        Objects.requireNonNull(entry, "entry should not have been null here");
        final int offset = entry.getKey().lowerEndpoint();
        return entry.getValue().charAt(realIndex - offset);
    }

    @Override
    public CharSequence subSequence(final int start, final int end)
    {
        final IntRange newRange
            = new IntRange(lowerBound + start, lowerBound + end);
        if (!range.encloses(newRange))
            throw new IndexOutOfBoundsException("illegal range requested: "
                + newRange);
        if (newRange.isEmpty())
            return EmptyCharSequence.INSTANCE;
        if (range.equals(newRange))
            return this;
        return factory.getSequence(newRange);
    }

    @Override
    public int length()
    {
        // Since ranges are always closed at the end, this works
        return range.getUpperBound() - lowerBound;
    }

    @Nonnull
    @Override
    public String toString()
    {
        final Map<Range<Integer>, CharBuffer> map = rangeMap.asMapOfRanges();
        final List<Map.Entry<Range<Integer>, CharBuffer>> list
            = new ArrayList<>(map.entrySet());
        final Range<Integer> first = list.get(0).getKey();
        final Range<Integer> last = list.get(list.size() - 1).getKey();
        final StringBuilder sb
            = new StringBuilder(last.upperEndpoint() - first.lowerEndpoint());

        for (final CharBuffer buffer: map.values())
            sb.append(buffer);

        final int start = lowerBound - first.lowerEndpoint();
        final int end = range.getUpperBound() - first.lowerEndpoint();
        return sb.subSequence(start, end).toString();
    }
}
