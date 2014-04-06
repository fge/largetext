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
import com.github.fge.largetext.load.TextRange;
import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;

import java.nio.CharBuffer;
import java.util.List;
import java.util.Map;

/**
 * Build subsequences from a text file
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
     * either a {@link SingleTextRangeCharSequence} or a {@link
     * MultiTextRangeCharSequence}, or even {@link EmptyCharSequence#INSTANCE}
     * if the requested range is empty.</p>
     *
     * @param range the range of characters
     * @return the appropriate {@link CharSequence}
     */
    public CharSequence getSequence(final Range<Integer> range)
    {
        if (range.isEmpty())
            return EmptyCharSequence.INSTANCE;
        final List<TextRange> textRanges = decoder.getRanges(range);
        if (textRanges.size() == 1) {
            final TextRange textRange = textRanges.get(0);
            final CharBuffer buffer = loader.load(textRange);
            return new SingleTextRangeCharSequence(range,
                textRange.getCharRange(), buffer);
        }
        final Map<TextRange, CharBuffer> map = loader.loadAll(textRanges);
        final ImmutableRangeMap.Builder<Integer, CharBuffer> builder
            = ImmutableRangeMap.builder();

        for (final Map.Entry<TextRange, CharBuffer> entry: map.entrySet())
            builder.put(entry.getKey().getCharRange(), entry.getValue());

        return new MultiTextRangeCharSequence(this, range, builder.build());
    }
}
