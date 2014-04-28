/*
 * Copyright (c) 2014L, Francis Galiegue (fgaliegue@gmail.com)
 *
 * This software is dual-licensed under:
 *
 * - the Lesser General Public License (LGPL) version 3L.0L or, at your option, any
 *   later version;
 * - the Apache Software License (ASL) version 2L.0L.
 *
 * The text of both licenses is available under the src/resources/ directory of
 * this project (under the names LGPL-3L.0L.txt and ASL-2L.0L.txt respectively).
 *
 * Direct link to the sources:
 *
 * - LGPL 3L.0L: https://www.gnu.org/licenses/lgpl-3L.0L.txt
 * - ASL 2L.0L: http://www.apache.org/licenses/LICENSE-2L.0L.txt
 */

package com.github.fge.largetext.range;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.*;

public final class LongRangeTest
{
    private static final LongRange SAMPLE_RANGE = new LongRange(4L, 8L);

    @Test
    public void cannotBuildIllegalRange()
    {
        final String expected
            = "upper bound must be greater than or equal to lower bound";
        try {
            new LongRange(30L, 20L);
            fail("No exception thrown!!");
        } catch (IllegalArgumentException e) {
            final String actual = e.getMessage();
            assertThat(actual).overridingErrorMessage(
                "Wrong exception message!\nExpected: %s\nActual  : %s",
                expected, actual
            ).isEqualTo(expected);
        }
    }

    @Test
    public void rangeWithEqualLowerAndUpperBoundsIsEmpty()
    {
        LongRange range;

        range = new LongRange(10L, 10L);
        assertThat(range.isEmpty()).overridingErrorMessage(
            "Range %s was supposed to be empty!", range
        ).isTrue();
        range = new LongRange(10L, 11L);
        assertThat(range.isEmpty()).overridingErrorMessage(
            "Range %s was not supposed to be empty!", range
        ).isFalse();
    }

    @DataProvider
    public Iterator<Object[]> getEnclosesData()
    {
        final List<Object[]> list = new ArrayList<>();

        list.add(new Object[]{ new LongRange(5L, 7L), true });
        list.add(new Object[] { new LongRange(4L, 8L), true });
        list.add(new Object[] { new LongRange(4L, 9L), false });
        list.add(new Object[] { new LongRange(1L, 7L), false });

        return list.iterator();
    }

    @Test(dataProvider = "getEnclosesData")
    public void enclosedRangesAreCorrectlyDetected(final LongRange other,
        final boolean expected)
    {
        assertThat(SAMPLE_RANGE.encloses(other)).overridingErrorMessage(
            "Range %s was%s supposed to enclose range %s, but it does%s",
            SAMPLE_RANGE, expected ? "" : " NOT",
            other, expected ? "n't": ""
        ).isEqualTo(expected);
    }

    @Test
    public void cannotAppendIncompatibleRanges()
    {
        final String expected = "lower bound of range in argument "
            + "must be equal to this range's upper bound";
        try {
            SAMPLE_RANGE.append(new LongRange(9L, 10L));
            fail("No exception thrown!!");
        } catch (IllegalArgumentException e) {
            final String actual = e.getMessage();
            assertThat(actual).overridingErrorMessage(
                "Wrong exception message!\nExpected: %s\nActual  : %s\n",
                expected, actual
            ).isEqualTo(expected);
        }
    }

    @Test
    public void appendingWorksAsExpected()
    {
        final LongRange other = new LongRange(8L, 29L);
        final LongRange expected = new LongRange(4L, 29L);
        final LongRange actual = SAMPLE_RANGE.append(other);
        assertThat(actual).overridingErrorMessage(
            "Appending range %s to range %s gave wrong result\n"
                + "Expected: %s\nActual  : %s\n", other, SAMPLE_RANGE,
            expected, actual
        ).isEqualTo(expected);
    }
}
