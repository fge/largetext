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

import java.io.IOException;
import java.nio.CharBuffer;

final class SingleCharWindowSubsequence
    implements CharSequence
{
    private final CharBuffer buffer;
    private final int relativeStart;
    private final int relativeEnd;

    SingleCharWindowSubsequence(final CharBufferLoader loader,
        final CharWindow window, final int absoluteStart,
        final int absoluteEnd)
    {
        try {
            buffer = loader.load(window);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        relativeStart = absoluteStart - window.getCharOffset();
        relativeEnd = absoluteEnd - window.getCharOffset();
    }

    @Override
    public int length()
    {
        return relativeEnd - relativeStart + 1;
    }

    @Override
    public char charAt(final int index)
    {
        return buffer.charAt(index + relativeStart);
    }

    @Override
    public CharSequence subSequence(final int start, final int end)
    {
        if (start < 0)
            throw new IndexOutOfBoundsException();
        return buffer.subSequence(start + relativeStart, end + relativeStart);
    }
}
