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

import java.nio.CharBuffer;

public final class SingleTextRangeCharSequence
    extends TextRangeCharSequence
{
    private final CharBuffer buffer;
    private final Range<Integer> bufferRange;

    public SingleTextRangeCharSequence(final CharSequenceFactory factory,
        final Range<Integer> range, final Range<Integer> bufferRange,
        final CharBuffer buffer)
    {
        super(factory, range);
        this.buffer = buffer;
        this.bufferRange = bufferRange;
    }

    @Override
    protected char doCharAt(final int realIndex)
    {
        return buffer.charAt(realIndex - bufferRange.lowerEndpoint());
    }
}
