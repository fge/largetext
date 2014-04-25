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

import com.github.fge.largetext.LargeText;
import com.github.fge.largetext.load.TextCache;
import com.github.fge.largetext.load.TextDecoder;
import com.github.fge.largetext.load.TextRange;
import com.github.fge.largetext.range.IntRange;
import com.google.common.collect.ImmutableRangeMap;

import java.nio.CharBuffer;
import java.util.List;
import java.util.Map;

/**
 * Large text file character subsequence provider
 *
 * <p>This class is used both by {@link LargeText} and {@link
 * MultiTextRangeCharSequence}; even though they both <em>do</em> implement
 * {@link CharSequence}, they do not know how to produce subsequences of
 * themselves; they delegate such matters to this class.</p>
 *
 * <p>Note that if the requested range fits into a single {@link TextRange}, a
 * simple {@link CharBuffer} (or subsequence thereof) is returned because this
 * class knows how to produce subsequences of itself.</p>
 *
 * @see TextDecoder#getRanges(IntRange)
 */
public final class CharSequenceFactory
{
    private final TextDecoder decoder;
    private final TextCache loader;

    public CharSequenceFactory(final TextDecoder decoder,
        final TextCache loader)
    {
        this.decoder = decoder;
        this.loader = loader;
    }

    /**
     * Get an appropriate character sequence for the requested range
     *
     * <p>Depending on the requested range and window size, this will return
     * either a ({@link CharSequence#subSequence(int, int)} of a) {@link
     * CharBuffer}, a {@link MultiTextRangeCharSequence}, or even {@link
     * EmptyCharSequence#INSTANCE} if the requested range is empty.</p>
     *
     * @param range the requested range of characters
     * @return the appropriate {@link CharSequence}
     */
    public CharSequence getSequence(final IntRange range)
    {
        if (range.isEmpty())
            return EmptyCharSequence.INSTANCE;
        final List<TextRange> textRanges = decoder.getRanges(range);
        if (textRanges.size() == 1) {
            final TextRange textRange = textRanges.get(0);
            final IntRange charRange = textRange.getCharRange();
            final CharBuffer buffer = loader.load(textRange);
            final int start = range.getLowerBound() - charRange.getLowerBound();
            final int end = range.getUpperBound() - charRange.getLowerBound();
            return buffer.subSequence(start, end);
        }
        final Map<TextRange, CharBuffer> map = loader.loadAll(textRanges);
        final ImmutableRangeMap.Builder<Integer, CharBuffer> builder
            = ImmutableRangeMap.builder();

        for (final Map.Entry<TextRange, CharBuffer> entry: map.entrySet())
            builder.put(entry.getKey().getCharRange().asGuavaRange(),
                entry.getValue());

        return new MultiTextRangeCharSequence(this, range, builder.build());
    }
}
