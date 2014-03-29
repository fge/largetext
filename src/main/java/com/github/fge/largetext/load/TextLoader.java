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

package com.github.fge.largetext.load;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Range;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static java.nio.channels.FileChannel.*;

public final class TextLoader
{
    private final FileChannel channel;
    private final Charset charset;

    /*
     * This is why we need Guava: we want cache expiry, and it has this builtin.
     *
     * TODO: implement our own cache for this purpose?
     */
    private final LoadingCache<TextRange, CharBuffer> cache;

    public TextLoader(final FileChannel channel, final Charset charset)
    {
        this.channel = channel;
        this.charset = charset;
        cache = CacheBuilder.<TextRange, CharBuffer>newBuilder()
            .expireAfterAccess(30L, TimeUnit.SECONDS)
            .recordStats().build(loader());
    }

    public CharBuffer load(final TextRange textRange)
        throws IOException
    {
        try {
            return cache.get(textRange);
        } catch (ExecutionException e) {
            throw (IOException) e.getCause();
        }
    }

    public Map<TextRange, CharBuffer> loadAll(
        final Iterable<TextRange> iterable)
    {
        try {
            return cache.getAll(iterable);
        } catch (ExecutionException e) {
            throw new RuntimeException("Unhandled exception", e.getCause());
        }
    }

    private CacheLoader<TextRange, CharBuffer> loader()
    {
        return new CacheLoader<TextRange, CharBuffer>()
        {
            @Override
            public CharBuffer load(@Nonnull final TextRange key)
                throws IOException
            {
                final Range<Long> byteRange = key.getByteRange();
                final MappedByteBuffer buffer = channel.map(MapMode.READ_ONLY,
                    byteRange.lowerEndpoint(), byteRange.upperEndpoint());
                final CharsetDecoder decoder = charset.newDecoder();
                return decoder.decode(buffer);
            }
        };
    }

    @Override
    public String toString()
    {
        return cache.stats().toString();
    }
}
