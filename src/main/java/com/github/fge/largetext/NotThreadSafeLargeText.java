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

import com.github.fge.largetext.load.TextRange;
import com.github.fge.largetext.range.IntRange;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;

/**
 * A large text file as a {@link CharSequence}: non thread safe version
 *
 * <p>Despite the not really reassuring name, this is the class you will use the
 * most often.</p>
 *
 * <p>This class's {@code .charAt()} uses regular instance variables to store
 * the current text range and text buffer.</p>
 *
 * @see LargeTextFactory#load(Path)
 */
@NotThreadSafe
@ParametersAreNonnullByDefault
public final class NotThreadSafeLargeText
    extends LargeText
{
    private IntRange range = EMPTY_RANGE;
    private CharBuffer buffer = EMPTY_BUFFER;

    NotThreadSafeLargeText(final FileChannel channel, final Charset charset,
        final int quantity, final SizeUnit sizeUnit)
        throws IOException
    {
        super(channel, charset, quantity, sizeUnit);
    }

    @Override
    public char charAt(final int index)
    {
        if (!range.contains(index)) {
            final TextRange textRange = decoder.getRange(index);
            range = textRange.getCharRange();
            buffer = loader.load(textRange);
        }
        return buffer.charAt(index - range.getLowerBound());
    }
}
