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

import com.github.fge.largetext.load.TextCache;
import com.github.fge.largetext.load.TextDecoder;
import com.github.fge.largetext.range.IntRange;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;

public final class TextRangeCharSequenceTest
{
    private Path testFile;
    private FileChannel channel;
    private CharSequenceFactory factory;
    private final String testString = Strings.repeat("abcdefghij", 5000);

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
        channel = FileChannel.open(testFile, StandardOpenOption.READ);
        final TextDecoder decoder = new TextDecoder(channel, charset, 1000L);
        final TextCache loader = new TextCache(channel, charset);
        factory = new CharSequenceFactory(decoder, loader);
    }

    @DataProvider
    public Iterator<Object[]> getClassTests()
    {
        final List<Object[]> list = Lists.newArrayList();

        list.add(new Object[] { 0, 1000, CharBuffer.class });
        list.add(new Object[] { 500, 1049, MultiRangeCharSequence.class });

        return list.iterator();
    }

    @Test(dataProvider = "getClassTests")
    public void classesOfGeneratedSequencesAreCorrect(final int low,
        final int high, final Class<?> expected)
    {
        final IntRange range = new IntRange(low, high);
        final CharSequence sequence = factory.getSequence(range);
        final Class<?> actual = sequence.getClass();
        assertThat(expected).overridingErrorMessage(
            "Wrong class! Expected %s, got %s", expected.getCanonicalName(),
            actual.getCanonicalName()
        ).isAssignableFrom(actual);
    }


    @Test
    public void charAtWorksAsExpected()
    {
        final IntRange range = new IntRange(0, 1000);
        final CharSequence sequence = factory.getSequence(range);
        assertEquals(sequence.charAt(19), testString.charAt(19));
    }

    @Test
    public void subsequencesExtractedAreIndexedCorrectly()
    {
        final IntRange range = new IntRange(1000, 2000);
        final CharSequence sequence = factory.getSequence(range);
        final CharSequence subsequence = sequence.subSequence(19, 34);
        final String s = testString.substring(1000, 2000).substring(19, 34);
        assertEquals(subsequence.charAt(0), s.charAt(0));
        assertEquals(subsequence.charAt(12), s.charAt(12));
    }

    @Test
    public void lengthOfASequenceIsCorrectlyCalculated()
    {
        final IntRange range = new IntRange(2601, 2733);
        final CharSequence sequence = factory.getSequence(range);
        assertEquals(sequence.length(), 132);
    }

    @Test
    public void multiRangeCharSequencesWorkCorrectly()
    {
        final IntRange range = new IntRange(2601, 3733);
        final CharSequence sequence = factory.getSequence(range);
        assertEquals(sequence.length(), 1132);
    }

    @AfterClass
    public void deleteFile()
        throws IOException
    {
        channel.close();
        Files.delete(testFile);
    }
}
