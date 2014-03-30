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
 * Main user interface
 *
 * <p>Start by building a {@link com.github.fge.largetext.LargeTextFactory},
 * then use it to obtain {@link com.github.fge.largetext.LargeText} instances.
 * </p>
 *
 * <p>You will need to create a {@link java.nio.file.Path} to the file you wish
 * to use, for instance using {@link java.nio.file.Paths#get(String, String...)}
 * if your file is on the "main" filesystem; but this will work with any path
 * from any filesystem, as long as this filesystem has {@link
 * java.nio.channels.SeekableByteChannel} support (see {@link
 * java.nio.file.Files#newByteChannel(java.nio.file.Path,
 * java.nio.file.OpenOption...)}).</p>
 */
package com.github.fge.largetext;