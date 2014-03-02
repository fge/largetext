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

import com.github.fge.msgsimple.bundle.MessageBundle;
import com.github.fge.msgsimple.load.MessageBundles;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import javax.annotation.concurrent.GuardedBy;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static java.nio.channels.FileChannel.*;

public final class TextFileDecoder
{
    private static final MessageBundle BUNDLE
        = MessageBundles.getBundle(LargeTextMessages.class);

    private static final ThreadFactory THREAD_FACTORY
        = new ThreadFactoryBuilder().setDaemon(true).build();

    private final ExecutorService executor
        = Executors.newFixedThreadPool(2, THREAD_FACTORY);

    @GuardedBy("this")
    private final DecodingStatus status = new DecodingStatus();

    private final List<CharWindow> windows = new CopyOnWriteArrayList<>();

    @GuardedBy("status")
    private final Queue<RequiredChars> queue = new PriorityQueue<>();

    private final FileChannel channel;
    private final Charset charset;
    private final long fileSize;
    private final long targetMapSize = 1L << 20;

    public TextFileDecoder(final FileChannel channel, final Charset charset)
        throws IOException
    {
        this.channel = channel;
        fileSize = channel.size();
        this.charset = charset;
    }

    void needChars(final int needed)
    {
        final RequiredChars requiredChars;

        synchronized (status) {
            if (status.finished) {
                if (status.exception != null)
                    throw new RuntimeException(status.exception);
                if (needed > status.nrChars)
                    throw new ArrayIndexOutOfBoundsException();
                return;
            }
            if (needed <= status.nrChars)
                return;
            requiredChars = RequiredChars.require(needed);
            queue.add(requiredChars);
        }

        try {
            requiredChars.await();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }


    private void fillWindows()
    {
        final CharsetDecoder decoder = charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);
        final CharBuffer buf = CharBuffer.allocate((int) targetMapSize);

        long fileOffset = 0L, windowLength;
        int charOffset = 0;
        CharWindow window;
        IOException caught = null;

        while (fileOffset < fileSize) {
            try {
                window = readNextWindow(fileOffset, charOffset, decoder, buf);
                // FIXME
                windowLength = window.getWindowLength();
                if (windowLength == 0L)
                    throw new IOException(BUNDLE.printf("err.invalidData",
                        fileOffset));
            } catch (IOException e) {
                caught = e;
                break;
            }
            windows.add(window);
            fileOffset += windowLength;
            charOffset += window.getCharLength();
            synchronized (status) {
                status.nrChars = charOffset;
                dequeueUntil(charOffset);
            }
        }

        synchronized (status) {
            status.finished = true;
            status.exception = caught;
            queue.clear();
        }
    }

    private void dequeueUntil(final int nrChars)
    {
        RequiredChars requiredChars;

        while (!queue.isEmpty()) {
            requiredChars = queue.peek();
            if (requiredChars.getRequired() > nrChars)
                return;
            queue.remove().wakeUp();
        }
    }

    private CharWindow readNextWindow(final long fileOffset,
        final int charOffset, final CharsetDecoder decoder,
        final CharBuffer buf)
        throws IOException
    {
        long mapSize = Math.min(targetMapSize, fileSize - fileOffset);

        final MappedByteBuffer mapping = channel.map(MapMode.READ_ONLY,
            fileOffset, mapSize);

        buf.rewind();
        decoder.reset();

        final CoderResult result = decoder.decode(mapping, buf, true);

        // FIXME
        if (result.isUnmappable())
            result.throwException();

        /*
         * Incomplete byte sequence: in this case, the mapping position reflects
         * what was actually read; change the mapping size
         */
        if (result.isMalformed())
            mapSize = (long) mapping.position();

        return new CharWindow(fileOffset, mapSize, charOffset, buf.position());
    }


    private static final class DecodingStatus
    {
        private boolean finished = false;
        private int nrChars = 0;
        private IOException exception = null;
    }
}
