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

import com.google.common.base.Supplier;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.ThrowableAssert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public final class NotThreadSafeLargeTextTest
{
    private static final SizeUnit windowSizeUnit = SizeUnit.KiB;
    private static final int windowSize = 16;
    // safe to cast as max window size supported by LargeText is Integer.MAX_VALUE
    private static final int len = (int) windowSizeUnit.sizeInBytes(windowSize);
    private static final Charset charset = StandardCharsets.UTF_8;
    private static final LargeTextFactory defaultFactory;
    private static final LargeTextFactory replaceFactory;
    private static final LargeTextFactory ignoreFactory;
    private final String testString = RandomStringUtils.random(len);
    private final Random random = new Random(System.nanoTime());

    static {
        final LargeTextFactory.Builder builder = LargeTextFactory.newBuilder()
                .setWindowSize(windowSize, windowSizeUnit);

        defaultFactory = builder.setCharset(charset).build();
        replaceFactory = builder.setCharsetDecoder(new Supplier<CharsetDecoder>()
        {
            @Override
            public CharsetDecoder get()
            {
                return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPLACE);
            }
        }).build();
        ignoreFactory = builder.setCharsetDecoder(new Supplier<CharsetDecoder>()
        {
            @Override
            public CharsetDecoder get()
            {
                return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.IGNORE);
            }
        }).build();
    }

    private Path testFile;

    @BeforeClass
    public void createFile()
        throws IOException
    {
        testFile = Files.createTempFile("foo", "bar");
        try (
            final BufferedWriter writer = Files.newBufferedWriter(testFile,
                charset);
        ) {
            writer.write(testString);
            writer.flush();
        }
    }

    @DataProvider
    public Iterator<Object[]> getFactoriesAndIndices()
    {
        final List<Object[]> list = new ArrayList<>();

        for (final LargeTextFactory factory : Arrays.asList(defaultFactory, replaceFactory, ignoreFactory))
            for (int i = 0; i < 2_000; i++)
                list.add(new Object[] { factory, random.nextInt(len) });

        return list.iterator();
    }

    @Test(dataProvider = "getFactoriesAndIndices")
    public void nonThreadSafeLargeTextWorks(final LargeTextFactory factory, final int index)
            throws IOException
    {
        try (LargeText text = factory.load(testFile)) {
            final char actual = text.charAt(index);
            final char expected = text.charAt(index);
            assertThat(actual).overridingErrorMessage(
                    "Wrong character picked up! Was '%s', expected '%s'",
                    actual, expected
            ).isEqualTo(expected);
        }
    }

    @Test
    public void reportsMalformedCharacterByDefault()
            throws IOException
    {
        try (final TemporaryFile tmp = new TemporaryFile()) {
            final Path path = tmp.getPath();
            Files.write(path, new byte[]{
                    (byte) 0x48,  // H
                    (byte) 0x65,  // e
                    (byte) 0x6C,  // l
                    (byte) 0xD0,  // ???
                    (byte) 0x6F   // o
            });

            try (final LargeText text = defaultFactory.load(path)) {
                assertThatThrownBy(new ThrowableAssert.ThrowingCallable()
                {
                    @Override
                    public void call()
                    {
                        text.charAt(4);
                    }
                }).isInstanceOf(LargeTextException.class).hasMessage("decoding error");
            }
        }
    }

    @Test
    public void replacesMalformedCharacterWhenDecoderIsConfiguredToReplace()
            throws IOException
    {
        try (final TemporaryFile tmp = new TemporaryFile()) {
            final Path path = tmp.getPath();
            Files.write(path, new byte[]{
                    (byte) 0x48,  // H
                    (byte) 0x65,  // e
                    (byte) 0x6C,  // l
                    (byte) 0xD0,  // ???
                    (byte) 0x6F   // o
            });

            try (final LargeText text = replaceFactory.load(path)) {
                assertThat(text.charAt(3)).isEqualTo('�');
                assertThat(text.charAt(4)).isEqualTo('o');
                assertThat(text.toString()).isEqualTo("Hel�o");
            }
        }
    }

    @Test
    public void replacesMalformedCharacterOnUpperBoundWhenDecoderIsConfiguredToReplace()
            throws IOException
    {
        int size = (int) windowSizeUnit.sizeInBytes(windowSize);
        final byte[] content = new byte[size + 2];
        Arrays.fill(content, (byte) 0x61);  // a
        content[size] = (byte) 0xD0;  // ???

        try (final TemporaryFile tmp = new TemporaryFile()) {
            final Path path = tmp.getPath();
            Files.write(path, content);

            try (final LargeText text = replaceFactory.load(path)) {
                assertThat(text.charAt(size - 1)).isEqualTo('a');
                assertThat(text.charAt(size)).isEqualTo('�');
                assertThat(text.charAt(size + 1)).isEqualTo('a');
            }
        }
    }

    @Test
    public void ignoresMalformedCharacterWhenDecoderIsConfiguredToIgnore()
            throws IOException
    {
        try (final TemporaryFile tmp = new TemporaryFile()) {
            final Path path = tmp.getPath();
            Files.write(path, new byte[]{
                    (byte) 0x48,  // H
                    (byte) 0x65,  // e
                    (byte) 0x6C,  // l
                    (byte) 0xD0,  // ???
                    (byte) 0x6F   // o
            });

            try (final LargeText text = ignoreFactory.load(path)) {
                assertThat(text.charAt(3)).isEqualTo('o');
                assertThat(text.toString()).isEqualTo("Helo");
            }
        }
    }

    @AfterClass
    public void deleteFile()
        throws IOException
    {
        Files.delete(testFile);
    }

}
