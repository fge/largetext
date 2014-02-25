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

package com.github.fge;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;

public final class TestCharsetDecoder
{
    /*
     * Found out:
     *
     * - the default action on an unmappable byte sequence is to throw an
     *   exception;
     * - the input `ByteBuffer`'s position _is_ modified after a decoding
     *   action is complete.
     */
    public static void main(final String... args)
        throws CharacterCodingException
    {
        final String input = "Mémé";
        final Charset charset = StandardCharsets.UTF_8;
        final byte[] array = input.getBytes(charset);
        final ByteBuffer buf = ByteBuffer.allocate(array.length + 1);
        buf.put(array);
        buf.put((byte) 0xfe); // Completely random
        buf.rewind();
        final CharsetDecoder decoder = charset.newDecoder();
        buf.limit(6);
        final CharBuffer decoded = decoder.decode(buf);

        System.out.println(buf.position());
        System.out.println(decoded);

    }
}
