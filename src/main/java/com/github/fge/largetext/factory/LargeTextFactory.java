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

package com.github.fge.largetext.factory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public final class LargeTextFactory
{
    private final Charset charset;
    private final SizeUnit sizeUnit;
    private final int quantity;

    public static Builder newBuilder()
    {
        return new Builder();
    }

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

    public LargeText fromPath(final Path path)
        throws IOException
    {
        final FileChannel channel = FileChannel.open(path,
            StandardOpenOption.READ);
        return new LargeText(channel, charset, quantity, sizeUnit);
    }

    public static final class Builder
    {
        private static final long MAX_WINDOW_SIZE = (long) Integer.MAX_VALUE;

        private Charset charset = StandardCharsets.UTF_8;
        private SizeUnit sizeUnit = SizeUnit.MiB;
        private int quantity = 2;

        private Builder()
        {
        }

        public Builder setCharset(@Nonnull final Charset charset)
        {
            this.charset = Objects.requireNonNull(charset,
                "charset cannot be null");
            return this;
        }

        public Builder setCharsetByName(@Nonnull final String charsetByName)
        {
            final Charset c = Charset.forName(charsetByName);
            return setCharset(c);
        }

        public Builder setWindowSize(final int quantity,
            final SizeUnit sizeUnit)
        {
            if (quantity <= 0)
                throw new IllegalArgumentException("window size must be " +
                    "strictly positive");
            this.quantity = quantity;
            this.sizeUnit = Objects.requireNonNull(sizeUnit,
                "window size unit must not be null");
            if (sizeUnit.sizeInBytes(quantity) > MAX_WINDOW_SIZE)
                throw new IllegalArgumentException("window size cannot exceed" +
                    " 2^31 - 1 bytes");
            return this;
        }

        public LargeTextFactory build()
        {
            return new LargeTextFactory(this);
        }
    }
}
