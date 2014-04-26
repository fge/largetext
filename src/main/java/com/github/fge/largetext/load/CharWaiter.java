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

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.PriorityQueue;
import java.util.concurrent.CountDownLatch;

/**
 * A waiter on a number of available characters in a {@link TextDecoder}
 *
 * <p>When it is woken up, it will check for the status of the operation; it
 * will throw a {@link LargeTextException} if the decoding operation has failed,
 * or it has waited to more characters than what is actually available.</p>
 *
 * <p>It implements {@link Comparable} since instances of this class are used in
 * a {@link PriorityQueue}.</p>
 *
 * <p>Inspired from <a href="http://stackoverflow.com/a/22055231/1093528">this
 * StackOverflow answer</a>.</p>
 *
 * @see DecodingStatus
 * @see TextDecoder#needChars(int)
 */
public final class CharWaiter
    implements Comparable<CharWaiter>
{
    private final int required;
    private final CountDownLatch latch = new CountDownLatch(1);

    private int nrChars = 0;
    private IOException exception = null;

    /**
     * Constructor
     *
     * @param required the number of required characters
     */
    public CharWaiter(final int required)
    {
        if (required < 0)
            throw new ArrayIndexOutOfBoundsException(required);
        this.required = required;
    }

    /**
     * Set the number of decoded characters
     *
     * @param nrChars the number of characters
     *
     * @see DecodingStatus#setNrChars(int)
     */
    public void setNrChars(final int nrChars)
    {
        this.nrChars = nrChars;
    }

    /**
     * Set the decoding error if the decoding operation has failed
     *
     * @param exception the decoding error
     *
     * @see DecodingStatus#setFailed(IOException)
     */
    public void setException(final IOException exception)
    {
        this.exception = exception;
    }

    /**
     * Get the number of characters required by this waiter
     *
     * @return the number of required characters
     */
    public int getRequired()
    {
        return required;
    }

    /**
     * Sleep waiting for the number of required characters to be available
     *
     * <p>On wakeup, if the thread has not been interrupted, two errors are
     * possible:</p>
     *
     * <ul>
     *     <li>the number of available chars is <em>less</em> than the number
     *     of chars requested; in this case an {@link IndexOutOfBoundsException}
     *     is thrown;</li>
     *     <li>the decoding process has terminated with an error; in this case,
     *     a {@link LargeTextException} is thrown, which cause is the error.
     *     </li>
     * </ul>
     *
     * @throws InterruptedException thread has been interrupted
     * @throws IndexOutOfBoundsException see description
     * @throws LargeTextException see description
     */
    public void await()
        throws InterruptedException
    {
        latch.await();
        if (exception != null)
            throw new LargeTextException("decoding error", exception);
        if (nrChars < required)
            throw new IndexOutOfBoundsException("out of bounds:" + required
                + " characters requested but only " + nrChars + " available");
    }

    /**
     * Wake up this waiter
     *
     * <p>This is called by {@link DecodingStatus}.</p>
     */
    public void wakeUp()
    {
        latch.countDown();
    }

    @Override
    public int compareTo(@Nonnull final CharWaiter o)
    {
        return Integer.compare(required, o.required);
    }

    @Override
    public String toString()
    {
        return "waiting for " + required + " character(s)";
    }
}
