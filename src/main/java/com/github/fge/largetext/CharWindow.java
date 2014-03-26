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

import com.github.fge.msgsimple.bundle.MessageBundle;
import com.github.fge.msgsimple.load.MessageBundles;

import javax.annotation.Nonnull;

/**
 * One window within a text file
 */
final class CharWindow
    implements Comparable<CharWindow>
{
    private static final MessageBundle BUNDLE
        = MessageBundles.getBundle(LargeTextMessages.class);

    /**
     * Offset in the file where this window starts
     */
    private final long fileOffset;

    /**
     * Length of this window
     */
    private final long windowLength;

    /**
     * Character offset within the {@link CharSequence}
     */
    private final int charOffset;

    /**
     * Number of characters in this window
     */
    private final int charLength;

    public CharWindow(final long fileOffset,
        final long windowLength, final int charOffset, final int charLength)
    {
        this.fileOffset = fileOffset;
        this.windowLength = windowLength;
        this.charOffset = charOffset;
        this.charLength = charLength;
    }

    public long getFileOffset()
    {
        return fileOffset;
    }

    public long getWindowLength()
    {
        return windowLength;
    }

    public int getCharOffset()
    {
        return charOffset;
    }

    public int getCharLength()
    {
        return charLength;
    }

    public boolean containsCharAtIndex(final int charIndex)
    {
        return charOffset <= charIndex && charIndex < charOffset + charLength;
    }

    public boolean containsRange(final int start, final int end)
    {
        return start >= charOffset && end - start + 1 <= charLength;
    }

    public CharWindow mergeWith(final CharWindow other)
    {
        final int expectedCharOffset = charOffset + charLength;
        final long expectedFileOffset = fileOffset + windowLength;

        BUNDLE.checkArgumentPrintf(other.charOffset == expectedCharOffset,
            "charWindow.wrongCharOffset", expectedCharOffset, other.charOffset);
        BUNDLE.checkArgumentPrintf(other.fileOffset == expectedFileOffset,
            "charWindow.wrongFileOffset", expectedFileOffset, other.fileOffset);

        return new CharWindow(fileOffset, windowLength + other.windowLength,
            charOffset, charLength + other.charLength);
    }

    @Override
    public int compareTo(@Nonnull final CharWindow o)
    {
        return Integer.compare(charOffset, o.charOffset);
    }
}
