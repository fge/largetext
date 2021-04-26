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

import org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

public final class ThreadSafeLargeTextTest
{
    private final String testString = RandomStringUtils.random(5_000);
    private final int len = testString.length();
    private final Random random = new Random(System.nanoTime());

    private Path testFile;
    private LargeText largeText;

    @BeforeClass
    public void createFile()
        throws IOException
    {
        final Charset charset = StandardCharsets.UTF_8;
        testFile = Files.createTempFile("foo", "bar");
        try (
            final BufferedWriter writer = Files.newBufferedWriter(testFile,
                charset);
        ) {
            writer.write(testString);
            writer.flush();
        }

        final LargeTextFactory factory = LargeTextFactory.newBuilder()
            .setWindowSize(16, SizeUnit.KiB)
            .build();
        largeText = factory.loadThreadSafe(testFile);
    }

    @Test(threadPoolSize = 30, invocationCount = 200)
    public void threadSafeTextFileActuallyIs()
    {
        final int index = random.nextInt(len);
        final char actual = largeText.charAt(index);
        final char expected = testString.charAt(index);
        assertThat(actual).overridingErrorMessage(
            "Wrong character picked up! Was '%s', expected '%s'",
            actual, expected
        ).isEqualTo(expected);
    }

    @AfterClass
    public void closeEverything()
        throws IOException
    {
        largeText.close();
        Files.delete(testFile);
    }
}
