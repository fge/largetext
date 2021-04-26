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

package com.github.fge.largetext.load;

import com.github.fge.largetext.LargeText;
import com.github.fge.largetext.LargeTextFactory;
import com.github.fge.largetext.SizeUnit;
import com.github.fge.largetext.TemporaryFile;
import com.google.common.base.Strings;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class TextDecoderTest
{
    // ConcurrentModificationException is thrown when view of ranges is
    // copied to ImmutableList and decoding task hasn't finished yet (
    // writes to ranges map).
    //
    // It is hard to reproduce ConcurrentModificationException without
    // modification of TextDecoder. It is doable in debug mode.
    //
    // Set breakpoints (one that suspend single thread, not all threads) at:
    // * TreeMap:1696 (SubMapIterator#nextEntry start)
    // * TextDecoder:233 (synchronized block in decodingTask)
    //
    // Execute this test in debug mode.
    //
    // * resume until breakpoint in nextEntry becomes available
    //   (usually takes a hit or two)
    // * switch to "text-decoder" thread
    // * step through synchronized block
    // * switch back to nextEntry thread
    // * step through until m.modCount != expectedModCount because put
    //   happened
    @Test
    public void reproduceGetRangesConcurrentModificationException()
            throws IOException
    {
        final String string = Strings.repeat("abcde", 5_000);
        try (final TemporaryFile tmp = new TemporaryFile()) {
            final Path path = tmp.getPath();

            Files.write(path, string.getBytes(StandardCharsets.UTF_8));

            final LargeTextFactory factory = LargeTextFactory.newBuilder()
                    .setCharset(StandardCharsets.US_ASCII)
                    .setWindowSize(1024, SizeUnit.B)
                    .build();

            try (final LargeText text = factory.load(path)) {
                text.subSequence(0, 450);
            }
        }
    }
}
