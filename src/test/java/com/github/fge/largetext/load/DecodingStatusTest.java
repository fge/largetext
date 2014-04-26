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
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.testng.Assert.*;

public final class DecodingStatusTest
{
    private DecodingStatus status;
    private ExecutorService executor;

    @BeforeMethod
    public void initExecutor()
    {
        final ThreadFactory factory = new ThreadFactoryBuilder()
            .setDaemon(true).build();
        executor = Executors.newSingleThreadExecutor(factory);
        status = new DecodingStatus();
    }

    @Test
    public void canAddZeroWaiterToNewStatus()
    {
        final CharWaiter waiter = new CharWaiter(0);
        assertTrue(status.addWaiter(waiter));
    }

    @Test
    public void waiterIsNotAwakenWhenNotEnoughCharsAreAvailable()
        throws InterruptedException, ExecutionException
    {
        final CharWaiter waiter = new CharWaiter(30);
        status.addWaiter(waiter);

        final Future<Boolean> future = executor.submit(newWaiter(waiter));

        status.setNrChars(20);

        try {
            future.get(50L, TimeUnit.MILLISECONDS);
            fail("I shouldn't have reached this point!");
        } catch (TimeoutException ignored) {
            assertTrue(true);
        }

        executor.shutdownNow();
        assertFalse(future.get());
    }

    @Test
    public void waiterIsAwakenedWhenEnoughCharsAreAvailable()
        throws InterruptedException, ExecutionException
    {
        final CharWaiter waiter = new CharWaiter(30);
        status.addWaiter(waiter);

        final Future<Boolean> future = executor.submit(newWaiter(waiter));

        status.setNrChars(40);

        assertTrue(future.get());
        executor.shutdownNow();
    }

    @Test
    public void willNotAddWaiterWhenEnoughCharsAreAvailable()
    {
        status.setNrChars(30);
        final CharWaiter waiter = new CharWaiter(20);
        assertFalse(status.addWaiter(waiter));
    }

    @Test
    public void cannotAddWaiterWithTooManyChars()
    {
        status.setFinished(30);
        try {
            status.addWaiter(new CharWaiter(40));
            fail("I shouldn't have reached this point!");
        } catch (IndexOutOfBoundsException ignored) {
            assertTrue(true);
        }
    }

    @Test
    public void waiterWithTooManyCharsRaisesException()
        throws InterruptedException
    {
        final CharWaiter waiter = new CharWaiter(40);
        status.addWaiter(waiter);

        final Future<Boolean> future = executor.submit(newWaiter(waiter));

        status.setFinished(30);

        try {
            future.get();
            fail("I shouldn't have reached this point!");
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof IndexOutOfBoundsException);
        }
        executor.shutdownNow();
    }

    @Test
    public void cannotAddNewWaiterWhenIOExceptionIsRaised()
    {
        final IOException exception = new IOException();
        status.setFailed(exception);
        try {
            status.addWaiter(new CharWaiter(0));
            fail("I shouldn't have reached this point!");
        } catch (LargeTextException e) {
            assertSame(e.getCause(), exception);
        }
    }

    @Test
    public void waiterThrowsExceptionOnIOException()
        throws InterruptedException
    {
        final CharWaiter waiter = new CharWaiter(20);
        status.addWaiter(waiter);

        final Future<Boolean> future = executor.submit(newWaiter(waiter));

        final IOException exception = new IOException();
        status.setFailed(exception);

        try {
            future.get();
            fail("I shouldn't have reached this point!");
        } catch (ExecutionException e) {
            final Throwable cause = e.getCause();
            assertTrue(cause instanceof LargeTextException);
            assertSame(cause.getCause(), exception);
        }

        executor.shutdownNow();
    }

    private static Callable<Boolean> newWaiter(final CharWaiter waiter)
    {
        return new Callable<Boolean>()
        {
            @Override
            public Boolean call()
            {
                try {
                    waiter.await();
                    return true;
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        };
    }
}
