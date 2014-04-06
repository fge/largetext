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
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.testng.Assert.*;

public final class TextRangeCharSequenceTest
{
    private Path testFile;
    private FileChannel channel;
    private CharSequenceFactory factory;

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
            for (int i = 0; i < 5000; i++)
                writer.write("abcdefghij");
            writer.flush();
        }
        channel = FileChannel.open(testFile, StandardOpenOption.READ);
        final TextDecoder decoder = new TextDecoder(channel, charset, 1000L);
        final TextCache loader = new TextCache(channel, charset);
        factory = new CharSequenceFactory(decoder, loader);
    }

    @Test
    public void charAtWorksAsExpected()
    {
        final IntRange range = new IntRange(0, 1000);
        final CharSequence sequence = factory.getSequence(range);
        assertEquals(sequence.charAt(19), 'j');
    }

    @Test
    public void subsequencesExtractedAreIndexedCorrectly()
    {
        final IntRange range = new IntRange(1000, 2000);
        final CharSequence sequence = factory.getSequence(range);
        final CharSequence subsequence = sequence.subSequence(19, 34);
        assertEquals(subsequence.charAt(0), 'j');
        assertEquals(subsequence.charAt(12), 'b');
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
