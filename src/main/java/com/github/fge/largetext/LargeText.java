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

import com.github.fge.largetext.load.TextCache;
import com.github.fge.largetext.load.TextDecoder;
import com.github.fge.largetext.load.TextRange;
import com.github.fge.largetext.range.IntRange;
import com.github.fge.largetext.sequence.CharSequenceFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * A large text file as a {@link CharSequence}
 *
 * <p>Do not create an instance of this class directly; instead, use a {@link
 * LargeTextFactory}.</p>
 *
 * <p><strong>Important note!</strong> This class implements {@link Closeable}
 * (and therefore {@link AutoCloseable}); the recommended use is therefore to
 * use it in a try-with-resources statement:</p>
 *
 * <pre>
 *     try (
 *         final LargeText largeText = factory.fromPath(somePath);
 *     ) {
 *         // use "largeText" here
 *     }
 * </pre>
 *
 * <p>Failing to close the instance correctly means you leak a file descriptor
 * to the text file you are using!</p>
 *
 * <p><strong>BIG FAT WARNING:</strong> getting the contents of a {@code
 * CharSequence} as a {@link String} using {@link Object#toString()} basically
 * dumps <strong>the contents of the whole file!</strong></p>
 *
 * @see LargeTextFactory
 */
public final class LargeText
    implements CharSequence, Closeable
{
    private final FileChannel channel;
    private final TextDecoder decoder;
    private final TextCache loader;
    private final CharSequenceFactory factory;

    private IntRange currentRange = new IntRange(0, 0);
    private CharBuffer currentBuffer = CharBuffer.allocate(0);

    /**
     * Package local constructor
     *
     * <p>This constructor <strong>does not</strong> do any error checking on
     * its argument; which is why you should really go through a {@link
     * LargeTextFactory} instance instead!</p>
     *
     * @param channel the {@link FileChannel} to the (hopefully text) file
     * @param charset the character encoding to use
     * @param quantity the quantity of size units
     * @param sizeUnit the size unit
     * @throws IOException failed to build a decoder
     */
    LargeText(final FileChannel channel, final Charset charset,
        final int quantity, final SizeUnit sizeUnit)
        throws IOException
    {
        this.channel = channel;
        final long windowSize = sizeUnit.sizeInBytes(quantity);
        decoder = new TextDecoder(channel, charset, windowSize);
        loader = new TextCache(channel, charset);
        factory = new CharSequenceFactory(decoder, loader);
    }

    /**
     * Obtain this file's length in {@code char}s (NOT code points!)
     *
     * <p>What is does is call {@link TextDecoder#getTotalChars()}.</p>
     *
     * @return the number of `char`s in this file
     */
    @Override
    public int length()
    {
        return decoder.getTotalChars();
    }

    @Override
    public char charAt(final int index)
    {
        if (!currentRange.contains(index)) {
            final TextRange textRange = decoder.getRange(index);
            currentRange = textRange.getCharRange();
            currentBuffer = loader.load(textRange);
        }
        return currentBuffer.charAt(index - currentRange.getLowerBound());
    }

    @Override
    public CharSequence subSequence(final int start, final int end)
    {
        return factory.getSequence(new IntRange(start, end));
    }

    /**
     * Close this instance
     *
     * <p>This closes the embedded {@link TextDecoder}, and then the {@link
     * FileChannel} associated with the file.</p>
     *
     * @throws IOException see {@link TextDecoder#close()} and {@link
     * FileChannel#close()}
     */
    @Override
    public void close()
        throws IOException
    {
        decoder.close();
        channel.close();
    }

    /**
     * *gasp* the whole instance as a string...
     *
     * <p>Basically this is a string representing the whole text file!</p>
     *
     * @return something veeery huge
     */
    @Override
    public String toString()
    {
        final int len = length();
        final IntRange range = new IntRange(0, len);
        final List<TextRange> textRanges = decoder.getRanges(range);
        final Map<TextRange, CharBuffer> map = loader.loadAll(textRanges);
        final StringBuilder sb = new StringBuilder(len);
        for (final CharBuffer buffer: map.values())
            sb.append(buffer);
        return sb.toString();
    }
}
