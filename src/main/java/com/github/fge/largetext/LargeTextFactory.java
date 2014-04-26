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

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * Factory to obtain {@link LargeText} instances
 *
 * <p>With this factory, you are able to specify two essential parameters:</p>
 *
 * <ul>
 *     <li>which character encoding ({@link Charset}) to use when decoding the
 *     file;</li>
 *     <li>what window size to use when decoding.</li>
 * </ul>
 *
 * <p>Sample usage:</p>
 *
 * <pre>
 *     // Default factory
 *     final LargeTextFactory factory = LargeTextFactory.defaultFactory();
 *     // Custom factory: use UTF-16 LE for encoding and a 256 MiB window size
 *     final LargeTextFactory factory = LargeTextFactory.newBuilder()
 *         .setCharset(StandardCharsets.UTF_16LE)
 *         .setWindowSize(256, SizeUnit.MiB);
 * </pre>
 *
 * <p>The defaults are to use UTF-8 ({@link StandardCharsets#UTF_8}) as a
 * character encoding, and a 2 MiB window size.</p>
 *
 * @see LargeTextFactory.Builder
 * @see SizeUnit
 * @see LargeText
 */
@Immutable
@ParametersAreNonnullByDefault
public final class LargeTextFactory
{
    private final Charset charset;
    private final SizeUnit sizeUnit;
    private final int quantity;

    /**
     * Obtain a builder for a new factory
     *
     * @return a builder, with default values
     */
    public static Builder newBuilder()
    {
        return new Builder();
    }

    /**
     * Obtain a factory with default values
     *
     * @return the factory
     */
    public static LargeTextFactory defaultFactory()
    {
        return new Builder().build();
    }

    private LargeTextFactory(final Builder builder)
    {
        charset = builder.charset;
        sizeUnit = builder.sizeUnit;
        quantity = builder.quantity;
    }

    /**
     * Obtain a {@link LargeText} instance from a given {@link Path}
     *
     * @param path the path to use
     * @return the large text instance
     * @throws IOException failed to open a (read only) {@link FileChannel} for
     * the given path
     *
     * @see FileChannel#open(Path, OpenOption...)
     */
    public LargeText fromPath(final Path path)
        throws IOException
    {
        final FileChannel channel = FileChannel.open(path,
            StandardOpenOption.READ);
        return new LargeText(channel, charset, quantity, sizeUnit);
    }

    /**
     * A {@link com.github.fge.largetext.LargeTextFactory} builder
     */
    @NotThreadSafe
    public static final class Builder
    {
        private static final long MIN_WINDOW_SIZE = 1024L;
        private static final long MAX_WINDOW_SIZE = (long) Integer.MAX_VALUE;

        private Charset charset = StandardCharsets.UTF_8;
        private SizeUnit sizeUnit = SizeUnit.MiB;
        private int quantity = 2;

        private Builder()
        {
        }

        /**
         * Set the character encoding to use for this factory
         *
         * @param charset the charset
         * @return this
         * @throws NullPointerException charset is null
         */
        public Builder setCharset(@Nonnull final Charset charset)
        {
            this.charset = Objects.requireNonNull(charset,
                "charset cannot be null");
            return this;
        }

        /**
         * Set the character encoding to use for this factory, by name
         *
         * @param charsetByName the name of the {@link Charset}
         * @return this
         * @throws NullPointerException argument is null
         *
         * @see Charset#forName(String)
         */
        public Builder setCharsetByName(@Nonnull final String charsetByName)
        {
            final Charset c = Charset.forName(charsetByName);
            return setCharset(c);
        }

        /**
         * Set the window size for this factory
         *
         * @param quantity the size unit quantity
         * @param sizeUnit the size unit
         * @return this
         * @throws NullPointerException size unit is null
         * @throws IllegalArgumentException window size is less than 1 KiB, or
         * greater than or equal to 2 GiB
         *
         * @see SizeUnit
         */
        public Builder setWindowSize(final int quantity,
            @Nonnull final SizeUnit sizeUnit)
        {
            if (quantity <= 0)
                throw new IllegalArgumentException("window size must be " +
                    "strictly positive");
            this.quantity = quantity;
            this.sizeUnit = Objects.requireNonNull(sizeUnit,
                "window size unit must not be null");
            final long targetWindowSize = sizeUnit.sizeInBytes(quantity);
            Preconditions.checkArgument(targetWindowSize >= MIN_WINDOW_SIZE,
                "window size must be at least 1024 bytes");
            Preconditions.checkArgument(targetWindowSize < MAX_WINDOW_SIZE,
                "window size must be strictly lower than 2 GiB");
            return this;
        }

        /**
         * Build the factory
         *
         * @return a new factory
         */
        public LargeTextFactory build()
        {
            return new LargeTextFactory(this);
        }
    }
}
