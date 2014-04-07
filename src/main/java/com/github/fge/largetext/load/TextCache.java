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

import com.github.fge.largetext.range.LongRange;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
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

/**
 * A thread-safe, concurrent-friendly loader of {@link CharBuffer}s from a large
 * text file
 *
 * <p>This class will load character sequences from a large text file as
 * described by {@link TextRange} instances (which are produced by a {@link
 * TextDecoder} instance).</p>
 *
 * <p>This uses Guava's {@link LoadingCache} to do the job. The default expiry
 * policy (not configurable at the moment) is to expire entries 30 seconds after
 * they were last accessed.</p>
 */
@ThreadSafe
public final class TextCache
{
    private final FileChannel channel;
    private final Charset charset;

    /*
     * This is why we need Guava: we want cache expiry, and it has this builtin.
     *
     * TODO: implement our own cache for this purpose?
     */
    private final LoadingCache<TextRange, CharBuffer> cache;

    public TextCache(final FileChannel channel, final Charset charset)
    {
        this.channel = channel;
        this.charset = charset;
        cache = CacheBuilder.<TextRange, CharBuffer>newBuilder()
            .expireAfterAccess(30L, TimeUnit.SECONDS)
            .recordStats().build(loader());
    }

    /**
     * Load one buffer matching a {@link TextRange}
     *
     * <p>Note that it calls {@link LoadingCache#getUnchecked(Object)};
     * therefore all loading failures will throw an <em>unchecked</em>
     * exception.</p>
     *
     * @param textRange the text range
     * @return the matching {@link CharBuffer}
     */
    public CharBuffer load(final TextRange textRange)
    {
        return cache.getUnchecked(textRange);
    }


    /**
     * Return a map of buffers from a series of text ranges
     *
     * <p>The map entries will be ordered the same way as ranges appear in the
     * supplied {@link Iterable}. Keys of the map will be ranges and values will
     * be the loaded {@link CharBuffer}s.</p>
     *
     * @param ranges the iterable of ranges
     * @return the matching map
     */
    public Map<TextRange, CharBuffer> loadAll(final Iterable<TextRange> ranges)
    {
        try {
            return cache.getAll(ranges);
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
                final LongRange byteRange = key.getByteRange();
                final long start = byteRange.getLowerBound();
                final long size = byteRange.getUpperBound() - start;
                final MappedByteBuffer buffer = channel.map(MapMode.READ_ONLY,
                    start, size);
                final CharsetDecoder decoder = charset.newDecoder();
                return decoder.decode(buffer).asReadOnlyBuffer();
            }
        };
    }

    @Override
    public String toString()
    {
        return cache.stats().toString();
    }
}
