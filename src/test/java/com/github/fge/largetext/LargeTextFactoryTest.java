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

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public final class LargeTextFactoryTest
{
    private LargeTextFactory.Builder builder;

    @BeforeMethod
    public void initBuilder()
    {
        builder = LargeTextFactory.newBuilder();
    }

    @Test
    public void cannotSetNullCharset()
    {
        try {
            builder.setCharset(null);
            fail("No exception thrown!!");
        } catch (NullPointerException e) {
            assertEquals(e.getMessage(), "charset cannot be null");
        }
    }

    @Test
    public void windowSizeMustBeStrictlyPositive()
    {
        try {
            builder.setWindowSize(0, SizeUnit.MiB);
            fail("No exception thrown!!");
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "window size must be strictly " +
                "positive");
        }
    }

    @Test
    public void windowSizeUnitMustNotBeNull()
    {
        try {
            builder.setWindowSize(2, null);
            fail("No exception thrown!!");
        } catch (NullPointerException e) {
            assertEquals(e.getMessage(), "window size unit must not be null");
        }
    }

    @Test
    public void windowSizeCannotExceed2GiB()
    {
        try {
            builder.setWindowSize(20_000_000, SizeUnit.MiB);
            fail("No exception thrown!!");
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "window size must be strictly lower " +
                "than 2 GiB");
        }
    }

    @Test
    public void windowSizeCannotBeLessThan1KiB()
    {
        try {
            builder.setWindowSize(1023, SizeUnit.B);
            fail("No exception thrown!!");
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "window size must be at least 1024 " +
                "bytes");
        }
    }

}
