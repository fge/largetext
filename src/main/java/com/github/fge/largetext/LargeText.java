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

import com.github.fge.largetext.load.TextDecoder;
import com.github.fge.largetext.load.TextLoader;
import com.github.fge.largetext.load.TextRange;
import com.github.fge.largetext.sequence.CharSequenceFactory;
import com.google.common.collect.Range;

import java.io.Closeable;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LargeText
    implements CharSequence, Closeable
{
    private final FileChannel channel;
    private final TextDecoder decoder;
    private final TextLoader loader;
    private final CharSequenceFactory factory;

    LargeText(final FileChannel channel, final Charset charset,
        final int quantity, final SizeUnit sizeUnit)
        throws IOException
    {
        this.channel = channel;
        final long windowSize = sizeUnit.sizeInBytes(quantity);
        decoder = new TextDecoder(channel, charset, windowSize);
        loader = new TextLoader(channel, charset);
        factory = new CharSequenceFactory(decoder, loader);
    }

    @Override
    public int length()
    {
        return decoder.getTotalChars();
    }

    @Override
    public char charAt(final int index)
    {
        final TextRange textRange = decoder.getRange(index);
        final CharBuffer buffer = loader.load(textRange);
        return buffer.charAt(index - textRange.getCharRange().lowerEndpoint());
    }

    @Override
    public CharSequence subSequence(final int start, final int end)
    {
        return factory.getSequence(Range.closedOpen(start, end));
    }

    @Override
    public void close()
        throws IOException
    {
        decoder.close();
        channel.close();
    }

    public static void main(final String... args)
        throws IOException
    {
        final LargeTextFactory factory = LargeTextFactory.newBuilder()
            .setCharset(StandardCharsets.UTF_8)
            .setWindowSize(16, SizeUnit.B)
            .build();

        final Path path = Paths.get("/usr/share/dict/words");
        final LargeText largeText = factory.fromPath(path);
        final Pattern pattern = Pattern.compile("^(?![a-zA-Z]+).+$",
            Pattern.MULTILINE);

        final Matcher matcher = pattern.matcher(largeText);
        int count = 0;

        while (matcher.find()) {
            System.out.println(matcher.group());
            count++;
        }
        System.out.println(count + " matches total");
    }
}
