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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;

/**
 * Thin wrapper of {@link Files#createTempFile(String, String, FileAttribute[])} that
 * removes temporary file on {@link #close()}.
 */
public final class TemporaryFile implements AutoCloseable
{
    private final Path path;

    /**
     * @throws IOException if fails to create temporary file
     */
    public TemporaryFile()
            throws IOException
    {
        this(null, null);
    }

    /**
     * @param prefix the prefix string to be used in generated file's name
     * @param suffix the suffix string to be used in generated file's name
     * @throws IOException if fails to create temporary file
     */
    public TemporaryFile(String suffix, String prefix)
            throws IOException
    {
        this.path = Files.createTempFile(suffix, prefix);
    }

    public Path getPath()
    {
        return path;
    }

    @Override
    public void close()
            throws IOException
    {
        Files.delete(path);
    }
}
