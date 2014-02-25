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

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

// TODO!
public final class LargeTextFile
    implements CharSequence, Closeable
{
    // 256K for now
    private static final long MAPPING_LENGTH = 1L << 28;

    private final Charset charset;
    private final FileChannel channel;
    private final List<CharWindow> windows = new ArrayList<>();

    public LargeTextFile(final String name, final Charset charset)
        throws IOException
    {
        this.charset = charset;
        channel = FileChannel.open(Paths.get(name), StandardOpenOption.READ);
    }

    public LargeTextFile(final String name)
        throws IOException
    {
        this(name, StandardCharsets.UTF_8);
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
    public CharSequence subSequence(final int start, final int end)
    {
        return null;
    }

    @Override
    public void close()
        throws IOException
    {
        channel.close();
    }
}
