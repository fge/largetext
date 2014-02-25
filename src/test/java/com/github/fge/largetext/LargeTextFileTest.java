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

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public final class LargeTextFileTest
{
    private Path tempDir;

    @BeforeClass
    public void createTempDir()
        throws IOException
    {
        tempDir = Files.createTempDirectory("largetext-tests");
    }

    @DataProvider
    public Iterator<Object[]> standardCharsets()
    {
        final List<Object[]> list = new ArrayList<>();

        list.add(new Object[] { StandardCharsets.UTF_8 });
        list.add(new Object[] { StandardCharsets.UTF_16 });
        list.add(new Object[] { StandardCharsets.UTF_16BE });
        list.add(new Object[] { StandardCharsets.UTF_16LE });
        list.add(new Object[] { StandardCharsets.US_ASCII });
        list.add(new Object[] { StandardCharsets.ISO_8859_1 });

        return list.iterator();
    }

    @Test(dataProvider = "standardCharsets")
    public void lengthIsCorrectlyReported(final Charset charset)
        throws IOException
    {
        final String s = "only ASCII chars to start with";
        final int occurrences = 20;
        final int nrChars = s.length() * occurrences;
        final Path path = createFile(s, charset, occurrences);

        try (
            final FileChannel channel = spy(FileChannel.open(path,
                StandardOpenOption.READ));
            final LargeTextFile textFile
                = new LargeTextFile(channel, charset, 1L << 10);
        ) {
            assertEquals(textFile.length(), nrChars);
        } finally {
            Files.delete(path);
        }

    }

    @AfterClass
    public void removeTempDir()
        throws IOException
    {
        Files.delete(tempDir);
    }

    private static Path createFile(final String s, final Charset charset,
        final int occurrences)
        throws IOException
    {
        final Path ret = Files.createTempFile("foo", "txt");
        try (
            final Writer writer = Files.newBufferedWriter(ret, charset);
        ) {
            for (int i = 0; i < occurrences; i++)
                writer.write(s);
            writer.flush();
        }

        return ret;
    }
}
