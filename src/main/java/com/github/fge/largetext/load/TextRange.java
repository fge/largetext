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

package com.github.fge.largetext.load;

import com.google.common.collect.Range;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

@Immutable
public final class TextRange
    implements Comparable<TextRange>
{
    private final Range<Integer> charRange;
    private final Range<Long> byteRange;

    public TextRange(final long byteOffset, final long nrBytes,
        final int charOffset, final int nrChars)
    {
        byteRange = Range.closedOpen(byteOffset, byteOffset + nrBytes);
        charRange = Range.closedOpen(charOffset, charOffset + nrChars);
    }

    public Range<Integer> getCharRange()
    {
        return charRange;
    }

    public Range<Long> getByteRange()
    {
        return byteRange;
    }

    @Override
    public int compareTo(@Nonnull final TextRange o)
    {
        return Integer.compare(charRange.lowerEndpoint(),
            o.charRange.lowerEndpoint());
    }

    @Override
    public int hashCode()
    {
        return charRange.hashCode();
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (obj == null)
            return false;
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        final TextRange other = (TextRange) obj;
        return charRange.equals(other.charRange);
    }

    @Override
    public String toString()
    {
        return "char range: " + charRange + "; byte range: " + byteRange;
    }
}
