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

package com.github.fge.largetext.range;

import com.google.common.base.Preconditions;
import com.google.common.collect.Range;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

/**
 * "Reduced" version of Guava's {@link Range} for {@code long}s
 *
 * <p>The lower bound is always inclusive, and the upper bound is always
 * exclusive (same as Guava's {@link Range#closedOpen(Comparable,
 * Comparable)}).</p>
 *
 * <p>It is customarily developed for the needs of this package; therefore it
 * only has a limited set of methods.</p>
 */
@Immutable
@ParametersAreNonnullByDefault
public final class LongRange
{
    private final long lowerBound;
    private final long upperBound;

    /**
     * Constructor
     *
     * @param lowerBound the lower bound (inclusive)
     * @param upperBound the upper bound (exclusive)
     * @throws IllegalArgumentException the upper bound is strictly less than
     * the lower bound.
     */
    public LongRange(final long lowerBound, final long upperBound)
    {
        Preconditions.checkArgument(upperBound >= lowerBound,
            "upper bound must be greater than or equal to lower bound");
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    /**
     * Get the (inclusive) lower bound of this range
     *
     * @return see description
     */
    public long getLowerBound()
    {
        return lowerBound;
    }

    /**
     * Get the (exclusive) upper bound of this range
     *
     * @return see description
     */
    public long getUpperBound()
    {
        return upperBound;
    }

    /**
     * Does this range contain the target value?
     *
     * @param value the value to test
     * @return true if and only if {@code value} is between the lower range
     * (inclusive) and the upper range (exclusive)
     */
    public boolean contains(final long value)
    {
        return value >= lowerBound && value < upperBound;
    }

    /**
     * Is this range empty?
     *
     * @return true if and only if the lower and upper bounds are equal
     */
    public boolean isEmpty()
    {
        return lowerBound == upperBound;
    }

    /**
     * Does this range enclose another range?
     *
     * @param other the other range
     * @return true if the bounds of the other range are within the
     * bounds of this range
     */
    public boolean encloses(final LongRange other)
    {
        Preconditions.checkNotNull(other, "argument cannot be null");
        return lowerBound <= other.lowerBound && upperBound >= other.upperBound;
    }

    /**
     * Append another range to the current range
     *
     * <p>The range as an argument can be appended to the current one if and
     * only if this range's upper bound equals the other range's lower bound. If
     * this is not the case, an {@link IllegalArgumentException} is thrown.</p>
     *
     * @param other the range to append
     * @return a <strong>new</strong> {@code IntRange} instance (since this
     * class is immutable)
     */
    public LongRange append(final LongRange other)
    {
        Preconditions.checkNotNull(other, "argument cannot be null");
        Preconditions.checkArgument(upperBound == other.lowerBound, "lower " +
            "bound of range in argument must be equal to this range's upper " +
            "bound");
        return new LongRange(lowerBound, other.upperBound);
    }

    @Override
    public int hashCode()
    {
        return (int) (lowerBound ^ upperBound);
    }

    @Override
    public boolean equals(@Nullable final Object obj)
    {
        if (obj == null)
            return false;
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        final LongRange other = (LongRange) obj;
        return lowerBound == other.lowerBound
            && upperBound == other.upperBound;
    }

    @Override
    public String toString()
    {
        return String.format("[%d, %d)", lowerBound, upperBound);
    }
}
