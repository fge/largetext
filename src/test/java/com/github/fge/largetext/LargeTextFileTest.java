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
import java.nio.ByteBuffer;
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

public final class LargeTextFileTest
{
    private static final MessageBundle BUNDLE
        = MessageBundles.getBundle(LargeTextMessages.class);

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
            final LargeTextFile textFile
                = new LargeTextFile(path.toString(), charset, 1L << 10);
        ) {
            assertEquals(textFile.length(), nrChars);
        } finally {
            Files.delete(path);
        }
    }

    // FIXME: not ideal...
    @Test
    public void invalidByteSequenceGeneratesAnException()
        throws IOException
    {
        final String s = "whatever";
        final byte[] array = s.getBytes(StandardCharsets.UTF_8);
        final ByteBuffer buf = ByteBuffer.allocate(array.length + 1);
        buf.put(array);
        buf.put((byte) 0xfe); // FIXME: random, but works for this test...

        final Path tempFile = tempDir.resolve("invalid.txt");
        Files.write(tempFile, buf.array());

        try (
            final LargeTextFile textFile
                = new LargeTextFile(tempFile.toString());
        ) {
            fail("I should not have reached this place");
        } catch (IOException e) {
            assertEquals(e.getMessage(),
                BUNDLE.printf("err.invalidData", array.length));
        }

        Files.delete(tempFile);
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
