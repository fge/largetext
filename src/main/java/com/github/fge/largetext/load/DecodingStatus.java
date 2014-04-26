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

import com.github.fge.largetext.LargeTextException;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;

/**
 * The watchdog class for a text decoding operation
 *
 * <p>This class takes care of {@link CharWaiter}s and callers to {@link
 * TextDecoder#getTotalChars()}.</p>
 *
 * <p>The decoding process in {@link TextDecoder} will update the internal
 * status of this object when the decoding operation makes progress; on an
 * update, this class will wake up the relevant waiters.</p>
 *
 * <p>In the event of an error, all waiters are woken up.</p>
 *
 * @see CharWaiter
 */
@ThreadSafe
final class DecodingStatus
{
    private boolean finished = false;
    private int nrChars = -1;
    private IOException exception = null;
    private final Queue<CharWaiter> waiters = new PriorityQueue<>();
    private final CountDownLatch endLatch = new CountDownLatch(1);

    synchronized boolean addWaiter(final CharWaiter waiter)
    {
        if (exception != null)
            throw new LargeTextException("decoding error", exception);
        final int required = waiter.getRequired();
        if (required <= nrChars)
            return false;
        if (!finished) {
            waiters.add(waiter);
            return true;
        }
        if (required > nrChars)
            throw new IndexOutOfBoundsException("out of bounds:" + required
                + " characters requested but only " + nrChars + " available");
        return false;
    }

    synchronized void setNrChars(final int nrChars)
    {
        this.nrChars = nrChars;
        CharWaiter waiter;
        while (!waiters.isEmpty()) {
            waiter = waiters.peek();
            if (waiter.getRequired() > nrChars)
                break;
            waiter.setNrChars(nrChars);
            waiters.remove().wakeUp();
        }
    }

    synchronized void setFailed(final IOException exception)
    {
        this.exception = exception;
        final List<CharWaiter> list = new ArrayList<>(waiters);
        waiters.clear();
        for (final CharWaiter waiter: list) {
            waiter.setException(exception);
            waiter.wakeUp();
        }
        endLatch.countDown();
    }

    synchronized void setFinished(final int nrChars)
    {
        finished = true;
        this.nrChars = nrChars;
        final List<CharWaiter> list = new ArrayList<>(waiters);
        waiters.clear();
        for (final CharWaiter waiter: list) {
            waiter.setNrChars(nrChars);
            waiter.wakeUp();
        }
        endLatch.countDown();
    }

    int getTotalSize()
    {
        try {
            endLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LargeTextException("interrupted", e);
        }
        if (exception != null)
            throw new LargeTextException("decoding error", exception);
        return nrChars;
    }

    @Override
    public synchronized String toString()
    {
        if (exception != null)
            return "decoding error after reading " + nrChars + " character(s)";
        return "currently decoded: " + nrChars + " character(s); finished: "
            + finished;
    }
}
