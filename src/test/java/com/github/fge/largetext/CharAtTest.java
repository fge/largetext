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

import com.github.fge.msgsimple.bundle.MessageBundle;
import com.github.fge.msgsimple.load.MessageBundles;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.testng.Assert.*;

public final class CharAtTest
{
    private static final MessageBundle BUNDLE
        = MessageBundles.getBundle(LargeTextMessages.class);

    private static final String INPUT = "Ë`ajê1RfD%";

    private Path tempDir;

    @BeforeClass
    public void createTempDir()
        throws IOException
    {
        tempDir = Files.createTempDirectory("largetext-tests");
    }

    @DataProvider
    public Iterator<Object[]> indicesAndCharacters()
    {
        final List<Object[]> list = new ArrayList<>();
        list.add(new Object [] { 4, 'ê' });
        list.add(new Object [] { 99999, '%' });
        list.add(new Object [] { 35042, 'a' });
        list.add(new Object [] { 12000, 'Ë' });

        return list.iterator();
    }

    @Test(dataProvider = "indicesAndCharacters")
    public void chatAtWorks(final int index, final char c)
        throws IOException
    {
        final Path file = createFile(INPUT, StandardCharsets.UTF_8, 10000);

        try (
            final LargeTextFile textFile = new LargeTextFile(file.toString(),
                StandardCharsets.UTF_8, 2500L);
        ) {
            assertEquals(textFile.charAt(index), c);
        }
    }

    @AfterClass
    public void removeTempDir()
        throws IOException
    {
        /*
         * Code adapted from:
         *
         * http://javatutorialhq.com/java/example-source-code/io/nio/delete-directory/
         */
        Files.walkFileTree(tempDir, new FileVisitor<Path>()
        {
            @Override
            public FileVisitResult preVisitDirectory(final Path dir,
                final BasicFileAttributes attrs)
                throws IOException
            {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(final Path file,
                final BasicFileAttributes attrs)
                throws IOException
            {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(final Path file,
                final IOException exc)
                throws IOException
            {
                throw exc;
            }

            @Override
            public FileVisitResult postVisitDirectory(final Path dir,
                final IOException exc)
                throws IOException
            {
                // FIXME: normally, .visitFileFailed() should have failed at
                // this point -- shouldn't it?
                if (exc != null)
                    throw exc;
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private Path createFile(final String s, final Charset charset,
        final int occurrences)
        throws IOException
    {
        final Path ret = Files.createTempFile(tempDir, "foo", "txt");
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
