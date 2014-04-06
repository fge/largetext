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
 * Limited-purpose range classes
 *
 * <p>These classes are modeled on Guava's {@link
 * com.google.common.collect.Range}, but only cover a small subset of its
 * functionalities.</p>
 *
 * <p>The motivation, particularly for {@link
 * com.github.fge.largetext.range.IntRange}, is to make {@link
 * java.lang.CharSequence#charAt(int)} cheaper; in {@link
 * com.github.fge.largetext.LargeText}, for performance reasons, the latest
 * loaded character buffer is kept in memory, along with its absolute range in
 * the file; when {@code charAt()} is called, it is checked that the character
 * index is within the current range.</p>
 *
 * <p>This was done using {@link
 * com.google.common.collect.Range#contains(Comparable)}, but since {@code
 * Range} does not have primitives as arguments, it was autoboxing the {@code
 * int} value each time; when this operation is called more than 100000 times
 * per <em>second</em>, the cost becomes significant. {@code IntRange}'s
 * {@code .contains()} does not do such autoboxing.</p>
 */
package com.github.fge.largetext.range;