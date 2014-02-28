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
final class RequiredChars
    implements Comparable<RequiredChars>
{
    private final int required;
    private final CountDownLatch latch = new CountDownLatch(1);

    RequiredChars(final int required)
    {
        this.required = required;
    }

    int getRequired()
    {
        return required;
    }

    void await()
        throws InterruptedException
    {
        latch.await();
    }

    void wakeUp()
    {
        latch.countDown();
    }

    @Override
    public int compareTo(final RequiredChars o)
    {
        return Integer.compare(required, o.required);
    }
}
