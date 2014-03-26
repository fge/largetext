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

import com.google.common.collect.ImmutableList;

import java.util.List;

final class MultiCharWindowSubsequence
    implements CharSequence
{
    private final CharBufferLoader loader;
    private final List<CharWindow> windows;
    private final int beginOffset;
    private final int endOffset;


    @SuppressWarnings("unchecked")
    MultiCharWindowSubsequence(final CharBufferLoader loader,
        final List<CharWindow> windows, final int absoluteStart,
        final int absoluteEnd)
    {
        this.loader = loader;
        this.windows = ImmutableList.copyOf(windows);
        final  int offset = absoluteStart - windows.get(0).getCharOffset();
        beginOffset = absoluteStart - offset;
        endOffset = absoluteEnd - offset;
    }

    @Override
    public int length()
    {
        return 0;
    }

    @Override
    public char charAt(final int index)
    {
        return 0;
    }

    @Override
    public CharSequence subSequence(int start, int end)
    {
        return null;
    }

    private int findWindowForCharIndex(final int absoluteIndex)
    {
        for (int index = 0; index < windows.size(); index++)
            if (windows.get(index).containsCharAtIndex(absoluteIndex))
                return index;

        throw new IndexOutOfBoundsException();
    }

    private static final class CharRange
    {
        private final int start; // inclusive
        private final int end;   // exclusive

        private CharRange(final int start, final int end)
        {
            this.start = start;
            this.end = end;
        }

        private boolean containsIndex(final int index)
        {
            return index >= start && index < end;
        }
    }
}
