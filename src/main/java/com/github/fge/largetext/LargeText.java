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
import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.Closeable;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * A large text file as a {@link CharSequence}: base abstract class
 *
 * <p>There are two possible versions: a non thread safe version and a thread
 * safe version. Expect a performance drop of 40% in the worst case scenario
 * if you use a thread safe version.</p>
 *
 * <p>The reason for the two different implementations is that this class
 * implements the principle of locality: when a caller calls {@link
 * CharSequence#charAt(int)}, chances are high that the next call to this method
 * will hit the same {@link TextRange}. As such, the class keeps the last loaded
 * text range and its matching buffer "at hand". The difference between the two
 * implementations is how these two elements are retained:</p>
 *
 * <ul>
 *     <li>in the non thread safe version, those are simple,
 *     non-{@code volatile}, instance variables;</li>
 *     <li>in the thread safe version, those are in a {@link ThreadLocal} inner
 *     class instance.</li>
 * </ul>
 *
 * <p>All other methods of {@link CharSequence} are implemented directly by this
 * class and are not overridable.</p>
 *
 * <p><strong>Important note!</strong> This class implements {@link Closeable}
 * (and therefore {@link AutoCloseable}); the recommended use is therefore to
 * use it in a try-with-resources statement:</p>
 *
 * <pre>
 *     try (
 *         final LargeText largeText = factory.load(somePath);
 *         // or factory.loadThreadSafe(somePath)
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
 * @see NotThreadSafeLargeText
 * @see ThreadSafeLargeText
 */
@ParametersAreNonnullByDefault
public abstract class LargeText
    implements CharSequence, Closeable
{
    private static final Logger LOGGER
        = Logger.getLogger(LargeText.class.getCanonicalName());
    protected static final IntRange EMPTY_RANGE = new IntRange(0, 0);
    protected static final CharBuffer EMPTY_BUFFER = CharBuffer.allocate(0);

    protected final FileChannel channel;
    protected final TextDecoder decoder;
    protected final TextCache loader;
    private final CharSequenceFactory factory;

    /**
     * The only protected constructor
     *
     * @param channel the {@link FileChannel} to the (hopefully text) file
     * @param charset the character encoding to use
     * @param quantity the quantity of size units
     * @param sizeUnit the size unit
     * @throws IOException failed to build a decoder
     */
    protected LargeText(final FileChannel channel, final Charset charset,
        final int quantity, final SizeUnit sizeUnit)
        throws IOException
    {
        this.channel = Preconditions.checkNotNull(channel,
            "file channel cannot be null");
        Preconditions.checkNotNull(charset, "charset cannot be null");
        Preconditions.checkNotNull(sizeUnit, "size unit cannot be null");
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
    public final int length()
    {
        return decoder.getTotalChars();
    }

    /**
     * Obtain a subsequence from this sequence
     *
     * <p>Calls to this method are delegated to a {@link CharSequenceFactory}.
     * </p>
     *
     * @param start the starting index of the subsequence (inclusive)
     * @param end the end index of the subsequence (exclusive)
     * @return a subsequence
     */
    @Override
    public final CharSequence subSequence(final int start, final int end)
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
    public final void close()
        throws IOException
    {
        try (
            final TextDecoder thisDecoder = decoder;
            final FileChannel thisChannel = channel;
        ) {
            LOGGER.fine("END; cache statistics: " + loader);
        }
    }

    /**
     * *gasp* the whole instance as a string...
     *
     * <p>Basically this is a string representing the whole text file!</p>
     *
     * @return something veeery huge
     */
    @Nonnull
    @Override
    public final String toString()
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

