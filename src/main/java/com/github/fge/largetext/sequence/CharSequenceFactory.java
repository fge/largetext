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

import com.github.fge.largetext.load.TextDecoder;
import com.github.fge.largetext.load.TextLoader;
import com.github.fge.largetext.load.TextRange;
import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;

import java.nio.CharBuffer;
import java.util.List;
import java.util.Map;

public final class CharSequenceFactory
{
    private final TextDecoder decoder;
    private final TextLoader loader;

    public CharSequenceFactory(final TextDecoder decoder,
        final TextLoader loader)
    {
        this.decoder = decoder;
        this.loader = loader;
    }

    public TextRangeCharSequence getSequence(final Range<Integer> range)
    {
        final List<TextRange> textRanges = decoder.getRanges(range);
        if (textRanges.size() == 1) {
            final TextRange textRange = textRanges.get(0);
            final CharBuffer buffer = loader.load(textRange);
            return new SingleTextRangeCharSequence(this, range,
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
