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

/**
 * Convenience enumeration of standard computing size units
 *
 * <p>Given the scope of this package, these units only go "up to" the gigabyte,
 * either "real" (2^30, {@link SizeUnit#GiB}) or "false" (10^9, {@link
 * SizeUnit#GB}).</p>
 */
public enum SizeUnit
{
    /**
     * Byte (identity)
     */
    B
    {
        @Override
        public long sizeInBytes(final int quantity)
        {
            return (long) quantity;
        }
    },
    /**
     * 2^10
     */
    KiB
    {
        @Override
        public long sizeInBytes(final int quantity)
        {
            return (long) quantity << 10;
        }
    },
    /**
     * 2^20
     */
    MiB
    {
        @Override
        public long sizeInBytes(final int quantity)
        {
            return (long) quantity << 20;
        }
    },
    /**
     * 2^30
     */
    GiB
    {
        @Override
        public long sizeInBytes(final int quantity)
        {
            return (long) quantity << 30;
        }
    },
    /**
     * 10^3
     */
    KB
    {
        @Override
        public long sizeInBytes(final int quantity)
        {
            return (long) quantity * 1_000L;
        }
    },
    /**
     * 10^6
     */
    MB
    {
        @Override
        public long sizeInBytes(final int quantity)
        {
            return (long) quantity * 1_000_000L;
        }
    },
    /**
     * 10^9
     */
    GB
    {
        @Override
        public long sizeInBytes(final int quantity)
        {
            return (long) quantity * 1_000_000_000L;
        }
    };

    /**
     * Obtain the size in bytes of a given quantity
     *
     * @param quantity the quantity
     * @return the size in bytes
     */
    public abstract long sizeInBytes(final int quantity);
}
