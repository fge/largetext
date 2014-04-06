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

/**
 * Classes dealing with file I/O
 *
 * <p>The fundamental problem here is that what is aimed at being implemented is
 * a {@link java.lang.CharSequence}; none of its methods "support" throwing any
 * kind of exception, so all exceptions thrown by methods in this package are
 * unchecked.</p>
 *
 * <p>Among things that can happen are:</p>
 *
 * <ul>
 *     <li>file I/O errors (either disk failure or, if reading from a network
 *     filesystem, temporary network failure),</li>
 *     <li>decoding error: at some point, the decoding process gives up because
 *     it cannot decode a byte sequence to a character sequence.</li>
 * </ul>
 *
 * <p>Another problem is that decoding takes time. While this will probably not
 * be noticeable for "casual" uses of this package, consider that the decoding
 * process is single threaded for a reason (see {@link
 * com.github.fge.largetext.load.TextDecoder}; if, for instance, you are asking
 * for the number of characters in a 2 GB file, you will have to wait until the
 * decoding process has finished its job.</p>
 *
 * <p>Some notes on memory usage:</p>
 *
 * <ul>
 *     <li>this package makes use of {@link
 *     java.nio.channels.FileChannel#map(java.nio.channels.FileChannel.MapMode,
 *     long, long)} to map raw file contents, so that part is not eating
 *     valuable heap space;</li>
 *     <li>however, decoding into a {@link java.nio.CharBuffer} (this is done
 *     using {@link java.nio.charset.CharsetDecoder#decode(java.nio.ByteBuffer)}
 *     <em>does</em> consume heap space.</li>
 * </ul>
 *
 * <p>The notes above are for the decoding process only, which happens once per
 * file. Memory usage of a {@link com.github.fge.largetext.LargeText} instance
 * depend on how much you require at a time from its attached {@link
 * com.github.fge.largetext.load.TextCache} instance; its backend is a {@link
 * com.google.common.cache.LoadingCache} (from Guava) with an expiry policy of
 * 30 seconds after last <em>access</em>. At this moment, this is not
 * configurable.</p>
 *
 */
package com.github.fge.largetext.load;

