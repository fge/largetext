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

import javax.annotation.concurrent.GuardedBy;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.nio.channels.FileChannel.*;

final class CharBufferCache
{
    private static final MessageBundle BUNDLE
        = MessageBundles.getBundle(LargeTextMessages.class);
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
    @GuardedBy("lock")
    private volatile int totalChars = 0;
    @GuardedBy("lock")
    private volatile boolean finished = false;
    @GuardedBy("lock")
    private volatile IOException exception = null;

    @GuardedBy("lock")
    private final PriorityQueue<RequiredCharacters> queue
        = new PriorityQueue<>();

    @GuardedBy("lock")
    private final List<CharWindow> windows = new ArrayList<>();

    private final Lock lock = new ReentrantLock();

    private final FileChannel channel;
    private final Charset charset;
    private final long targetMapSize;
    private final long fileSize;

    CharBufferCache(final FileChannel channel, final Charset charset,
        final long targetMapSize)
        throws IOException
    {
        this.channel = channel;
        this.charset = charset;
        this.targetMapSize = targetMapSize;
        fileSize = channel.size();
    }


    void needChars(final int required)
        throws IOException
    {
        RequiredCharacters waiter = null;

        lock.lock();
        try {
            if (exception != null)
                throw exception;
            final int currentTotal = totalChars;
            if (required > currentTotal) {
                if (finished)
                    throw new IndexOutOfBoundsException();
                waiter = new RequiredCharacters(required);
                queue.add(waiter);
            }
        } finally {
            lock.unlock();
        }

        if (waiter != null)
            try {
                waiter.await();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
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

    private void fillWindows()
    {
        final CharsetDecoder decoder = charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);
        final CharBuffer buf = CharBuffer.allocate((int) targetMapSize);

        long fileOffset = 0L, windowLength;
        int charOffset = 0;
        CharWindow window;
        IOException caught = null;

        while (fileOffset < fileSize) {
            try {
                window = readNextWindow(fileOffset, charOffset, decoder, buf);
                // FIXME
                windowLength = window.getWindowLength();
                if (windowLength == 0L)
                    throw new IOException(BUNDLE.printf("err.invalidData",
                        fileOffset));
            } catch (IOException e) {
                caught = e;
                break;
            }
            lock.lock();
            try {
                windows.add(window);
                charOffset += window.getCharLength();
                totalChars = charOffset;
                fileOffset += windowLength;
                dequeueWaiters(charOffset);
            }   finally {
                lock.unlock();
            }
        }

        lock.lock();
        try {
            exception = caught;
            finished = true;
        } finally {
            lock.unlock();
        }

    }

    private CharWindow readNextWindow(final long fileOffset,
        final int charOffset, final CharsetDecoder decoder,
        final CharBuffer buf)
        throws IOException
    {
        long mapSize = Math.min(targetMapSize, fileSize - fileOffset);

        final MappedByteBuffer mapping = channel.map(MapMode.READ_ONLY,
            fileOffset, mapSize);

        buf.rewind();
        decoder.reset();

        final CoderResult result = decoder.decode(mapping, buf, true);

        // FIXME
        if (result.isUnmappable())
            result.throwException();

        /*
         * Incomplete byte sequence: in this case, the mapping position reflects
         * what was actually read; change the mapping size
         */
        if (result.isMalformed())
            mapSize = (long) mapping.position();

        return new CharWindow(fileOffset, mapSize, charOffset, buf.position());
    }

    @GuardedBy("lock")
    private void dequeueWaiters(final int currentTotal)
    {
        RequiredCharacters waiter;

        while (!queue.isEmpty()) {
            waiter = queue.peek();
            if (waiter.required > currentTotal)
                break;
            queue.remove();
            waiter.wakeUp();
        }
    }
}
