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

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.*;

public final class IntRangeTest
{
    private static final IntRange SAMPLE_RANGE = new IntRange(4, 8);

    @Test
    public void cannotBuildIllegalRange()
    {
        final String expected
            = "upper bound must be greater than or equal to lower bound";
        try {
            new IntRange(30, 20);
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
        IntRange range;

        range = new IntRange(10, 10);
        assertThat(range.isEmpty()).overridingErrorMessage(
            "Range %s was supposed to be empty!", range
        ).isTrue();
        range = new IntRange(10, 11);
        assertThat(range.isEmpty()).overridingErrorMessage(
            "Range %s was not supposed to be empty!", range
        ).isFalse();
    }

    @DataProvider
    public Iterator<Object[]> getEnclosesData()
    {
        final List<Object[]> list = new ArrayList<>();

        list.add(new Object[]{ new IntRange(5, 7), true });
        list.add(new Object[] { new IntRange(4, 8), true });
        list.add(new Object[] { new IntRange(4, 9), false });
        list.add(new Object[] { new IntRange(1, 7), false });

        return list.iterator();
    }

    @Test(dataProvider = "getEnclosesData")
    public void enclosedRangesAreCorrectlyDetected(final IntRange other,
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
            SAMPLE_RANGE.append(new IntRange(9, 10));
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
        final IntRange other = new IntRange(8, 29);
        final IntRange expected = new IntRange(4, 29);
        final IntRange actual = SAMPLE_RANGE.append(other);
        assertThat(actual).overridingErrorMessage(
            "Appending range %s to range %s gave wrong result\n"
            + "Expected: %s\nActual  : %s\n", other, SAMPLE_RANGE,
            expected, actual
        ).isEqualTo(expected);
    }
}
