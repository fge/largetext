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

import java.util.concurrent.CountDownLatch;

/*
 * Inspired from http://stackoverflow.com/a/22055231/1093528
 */
abstract class RequiredChars
    implements Comparable<RequiredChars>
{
    private static final RequiredChars SENTINEL = new RequiredChars()
    {
        @Override
        int getRequired()
        {
            return Integer.MAX_VALUE;
        }

        @Override
        void await()
            throws InterruptedException
        {
        }

        @Override
        void wakeUp()
        {
        }

        @Override
        boolean isSentinel()
        {
            return true;
        }

        @Override
        public int compareTo(final RequiredChars o)
        {
            return o.isSentinel() ? 0 : 1;
        }
    };

    static RequiredChars require(final int required)
    {
        return new RequiredCharsImpl(required);
    }

    static RequiredChars sentinel()
    {
        return SENTINEL;
    }

    abstract int getRequired();

    abstract void await()
        throws InterruptedException;

    abstract void wakeUp();

    abstract boolean isSentinel();

    private static final class RequiredCharsImpl
        extends RequiredChars
    {
        private final int required;
        private final CountDownLatch latch = new CountDownLatch(1);

        private RequiredCharsImpl(final int required)
        {
            this.required = required;
        }

        @Override
        int getRequired()
        {
            return required;
        }

        @Override
        void await()
            throws InterruptedException
        {
            latch.await();
        }

        @Override
        void wakeUp()
        {
            latch.countDown();
        }

        @Override
        boolean isSentinel()
        {
            return false;
        }

        @Override
        public int compareTo(final RequiredChars o)
        {
            return o.isSentinel() ? -1
                : Integer.compare(required, o.getRequired());
        }
    }
}
