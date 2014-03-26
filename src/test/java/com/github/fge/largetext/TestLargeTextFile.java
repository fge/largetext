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

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TestLargeTextFile
{
    public static void main(final String... args)
        throws IOException
    {
        final LargeTextFile textFile
            = new LargeTextFile("/usr/share/dict/words");
        final Pattern pattern = Pattern.compile("^[\\p{Alpha}]+s$",
            Pattern.MULTILINE);

        final Matcher matcher = pattern.matcher(textFile);

        while (matcher.find())
            System.out.println(matcher.group());

        int i = 23;
    }
}
