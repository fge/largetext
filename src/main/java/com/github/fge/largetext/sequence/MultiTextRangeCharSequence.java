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
import com.google.common.collect.RangeMap;

import java.nio.CharBuffer;
import java.util.Map;
import java.util.Objects;

public final class MultiTextRangeCharSequence
    extends TextRangeCharSequence
{
    private final RangeMap<Integer, CharBuffer> rangeMap;

    public MultiTextRangeCharSequence(final CharSequenceFactory factory,
        final Range<Integer> range,
        final RangeMap<Integer, CharBuffer> rangeMap)
    {
        super(factory, range);
        this.rangeMap = rangeMap;
    }

    @Override
    protected char doCharAt(final int realIndex)
    {
        final Map.Entry<Range<Integer>, CharBuffer> entry
            = rangeMap.getEntry(realIndex);
        Objects.requireNonNull(entry, "entry should not have been null here");
        final int offset = entry.getKey().lowerEndpoint();
        return entry.getValue().charAt(realIndex - offset);
    }
}
