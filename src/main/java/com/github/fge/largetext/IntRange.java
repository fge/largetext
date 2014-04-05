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

import com.google.common.base.Preconditions;
import com.google.common.collect.Range;

import javax.annotation.concurrent.Immutable;

/**
 * "Reduced" version of Guava's {@link Range} for int primitives
 *
 * <p>The lower bound is always inclusive, and the upper bound is always
 * exclusive.</p>
 *
 * <p>It is customarily developed for the needs of this package; therefore it
 * only has a limited set of methods.</p>
 */
@Immutable
public final class IntRange
{
    private final int lowerBound;
    private final int upperBound;

    public IntRange(final int lowerBound, final int upperBound)
    {
        Preconditions.checkArgument(upperBound >= lowerBound,
            "upper bound must be greater than or equal to lower bound");
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public int getLowerBound()
    {
        return lowerBound;
    }

    public int getUpperBound()
    {
        return upperBound;
    }

    public boolean contains(final int value)
    {
        return value >= lowerBound && value < upperBound;
    }

    public boolean isEmpty()
    {
        return lowerBound == upperBound;
    }

    public boolean encloses(final IntRange other)
    {
        return lowerBound <= other.lowerBound && upperBound >= other.upperBound;
    }

    @Override
    public int hashCode()
    {
        return lowerBound ^ upperBound;
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
        final IntRange other = (IntRange) obj;
        return lowerBound == other.lowerBound
            && upperBound == other.upperBound;
    }

    @Override
    public String toString()
    {
        return String.format("[%d,%d)", lowerBound, upperBound);
    }
}
