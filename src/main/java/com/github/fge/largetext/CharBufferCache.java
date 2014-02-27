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

import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

final class CharBufferCache
{
    /*
     * Use daemon threads. We don't give control to the user about the
     * ExecutorService, and we don't have a reliable way to shut it down (a JVM
     * shutdown hook does not get involved on a webapp shutdown, so we cannot
     * use that...).
     */
    private static final ThreadFactory THREAD_FACTORY = new ThreadFactory()
    {
        private final ThreadFactory factory = Executors.defaultThreadFactory();

        @Override
        public Thread newThread(final Runnable r)
        {
            final Thread ret = factory.newThread(r);
            ret.setDaemon(true);
            return ret;
        }
    };

    private final ExecutorService executor
        = Executors.newCachedThreadPool(THREAD_FACTORY);

    // FIXME: make the two variables below volatile instead?
    private final AtomicInteger totalChars = new AtomicInteger(0);
    private final AtomicBoolean finished = new AtomicBoolean(false);

    private final PriorityQueue<RequiredCharacters> queue
        = new PriorityQueue<>();

    private final List<CharWindow> windows = new ArrayList<>();

    private final Lock lock = new ReentrantLock();

    private final FileChannel channel;
    private final Charset charset;
    private final long targetMapSize;

    CharBufferCache(final FileChannel channel, final Charset charset,
        final long targetMapSize)
    {
        this.channel = channel;
        this.charset = charset;
        this.targetMapSize = targetMapSize;
    }


    void needChars(final int required)
    {
    }

    /*
     * Inspired from http://stackoverflow.com/a/22055231/1093528
     */
    private final class RequiredCharacters
        implements Comparable<RequiredCharacters>
    {
        private final int required;
        private final CountDownLatch latch = new CountDownLatch(1);

        private RequiredCharacters(final int required)
        {
            this.required = required;
        }

        private void await()
            throws InterruptedException
        {
            latch.await();
        }

        private void wakeUp()
        {
            latch.countDown();
        }

        @Override
        public int compareTo(final RequiredCharacters o)
        {
            return Integer.compare(required, o.required);
        }
    }
}
