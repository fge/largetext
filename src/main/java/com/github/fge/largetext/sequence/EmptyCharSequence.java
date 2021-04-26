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

import javax.annotation.concurrent.Immutable;

/**
 * An empty (0-length) character sequence
 */
@Immutable
public enum EmptyCharSequence
    implements CharSequence
{
    INSTANCE;

    @Override
    public int length()
    {
        return 0;
    }

    @Override
    public char charAt(final int index)
    {
        throw new IndexOutOfBoundsException(index + " index out of range");
    }

    @Override
    public CharSequence subSequence(final int start, final int end)
    {
        if (start != 0)
            throw new IndexOutOfBoundsException(start + " index out of range");
        if (end != 0)
            throw new IndexOutOfBoundsException(end + " index out of range");
        return this;
    }

    @Override
    public String toString() {
        return "";
    }
}
