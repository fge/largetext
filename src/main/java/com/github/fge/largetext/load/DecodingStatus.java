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

import com.github.fge.largetext.LargeText;
import com.github.fge.largetext.LargeTextException;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;

/**
 * The "thread shepherd" for a text decoding operation
 *
 * <p>This class handles two types of callers:</p>
 *
 * <ul>
 *     <li>callers to {@link CharSequence#charAt(int)} and {@link
 *     CharSequence#subSequence(int, int)}, via {@link CharWaiter}s;</li>
 *     <li>callers to {@link CharSequence#length()}, via its {@link
 *     #getTotalSize()} method.</li>
 * </ul>
 *
 * <p>The callers of the first two charsequence operations only need to wait for
 * a given number of characters to be (successfully!) decoded, while the callers
 * of the third need to wait for the whole decoding process to (successfully,
 * again) terminate.</p>
 *
 * <p>This class maintains the first category of callers using a {@link
 * PriorityQueue} of {@link CharWaiter} instances, and the second category with
 * a single {@link CountDownLatch}.</p>
 *
 * <p>There is one such class per {@link LargeText} instance. It is created,
 * and updated by, the (also unique) {@link TextDecoder} instance which is the
 * decoding workhorse.</p>
 *
 * <p>See the documentation of each method for details on the inner workings.
 * </p>
 *
 * @see CharWaiter
 * @see TextDecoder#needChars(int)
 * @see TextDecoder#getTotalChars()
 */
@ThreadSafe // since all methods are synchronized; but this part needs a rewrite
public final class DecodingStatus
{
    private boolean finished = false;
    private int nrChars = -1;
    private IOException exception = null;
    private final Queue<CharWaiter> waiters = new PriorityQueue<>();
    private final CountDownLatch endLatch = new CountDownLatch(1);

    /**
     * Add (if needed) one {@link CharWaiter} to the queue of waiters
     *
     * <p>Callers to {@link LargeText#charAt(int)} may reach {@link
     * TextDecoder#needChars(int)} which will create a {@link CharWaiter}
     * instance and then call this method.</p>
     *
     * <p>The waiter is queue if <em>and only if</em> the number of currently
     * available characters is less than what it requests for; in which case
     * it will {@link CharWaiter#await()} for this class to wake it up.</p>
     *
     * @param waiter the waiter to add
     * @return true if and only if the waiter was actually queued, false
     * otherwise
     * @throws IndexOutOfBoundsException decoding was already finished, and
     * waiter asked for more characters than what is available
     */
    public synchronized boolean addWaiter(final CharWaiter waiter)
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

    /**
     * Update the number of available characters successfully decoded
     *
     * <p>When this method is called, all waiters in the queue requiring this
     * number of chars or less are dequeued and woken up (see {@link
     * CharWaiter#wakeUp()}.</p>
     *
     * @param nrChars the number of available characters
     *
     * @see CharWaiter#setNrChars(int)
     */
    public synchronized void setNrChars(final int nrChars)
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

    /**
     * Record a decoding failure
     *
     * <p>When such a failure happens, all waiters (including callers to
     * {@link LargeText#length()} are "woken up" with a {@link
     * LargeTextException}; the cause of this exception is set to the exception
     * passed as an argument.</p>
     *
     * @param exception the exception raised by the decoding process
     *
     * @see CharWaiter#setException(IOException)
     * @see #getTotalSize()
     */
    public synchronized void setFailed(final IOException exception)
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

    /**
     * Notify that the decoding operation is finished
     *
     * <p>This works very similarly to {@link #setNrChars(int)}, except that
     * all waiters in the queue are woken up along with all callers of {@link
     * LargeText#length()}.</p>
     *
     * <p>The logic to handle the case where too many characters have been
     * waited for is handled in {@link CharWaiter}.</p>
     *
     * @param nrChars the number of available characters
     *
     * @see CharWaiter#setNrChars(int)
     * @see CharWaiter#wakeUp()
     */
    public synchronized void setFinished(final int nrChars)
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

    /**
     * Method called by all callers of {@link LargeText#length()}
     *
     * <p>Callers of this method will sleep and be woken up if one of three
     * conditions happen:</p>
     *
     * <ul>
     *     <li>the dedocing operation has successfully completed;</li>
     *     <li>the current thread has been interrupted;</li>
     *     <li>the decoding operation terminated with an error.</li>
     * </ul>
     *
     * <p>In the two last scenarios, threads are "woken up" with a {@link
     * LargeTextException}; the cause is set to the relevant error.</p>
     *
     * @return the number of characters
     *
     * @throws LargeTextException caller has been interrupted, or decoding
     * operation has failed
     */
    public int getTotalSize()
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
