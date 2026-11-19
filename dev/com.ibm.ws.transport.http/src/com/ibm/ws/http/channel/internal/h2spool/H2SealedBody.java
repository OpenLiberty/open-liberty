/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.channel.internal.h2spool;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/** An exclusively owned, completed ordinary HTTP/2 body. */
public final class H2SealedBody implements AutoCloseable {
    private static final int MAX_READ_CHUNK_BYTES = 64 * 1024;

    private final Path path;
    private final FileChannel channel;
    private final long length;
    private final Executor cleanupExecutor;
    private final H2SpoolResources.Lease lease;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Object ioLock = new Object();
    private volatile boolean opened;
    private boolean channelClosed;
    private boolean deleted;
    private CompletableFuture<Void> asynchronousClose;

    H2SealedBody(Path path, FileChannel channel, long length, Executor cleanupExecutor, H2SpoolResources.Lease lease) {
        this.path = path;
        this.channel = channel;
        this.length = length;
        this.cleanupExecutor = cleanupExecutor;
        this.lease = lease;
    }

    public long length() {
        return length;
    }

    boolean claimForApplication() {
        return lease.claimForApplication(this);
    }

    /** Uses the read allowance promised when this body was admitted. */
    public ReadLifetime beginReadLifetime() throws IOException {
        // Allocate the handle before committing its lease bit. A failed
        // allocation must not leave a lifetime that nobody can retire.
        ReadLifetime lifetime = new ReadLifetime();
        if (closed.get() || !lease.beginReadLifetime(this)) {
            throw new IOException("HTTP/2 body is closed");
        }
        return lifetime;
    }

    public final class ReadLifetime implements AutoCloseable {
        private final AtomicBoolean retired = new AtomicBoolean();

        private ReadLifetime() {}

        @Override
        public void close() {
            if (retired.compareAndSet(false, true)) {
                lease.retireReadLifetime();
            }
        }
    }

    /**
     * Opens the only reader. The caller must read on an application worker, not
     * on the Netty event loop, and close the reader even after a partial read.
     */
    public synchronized InputStream openInputStream() throws IOException {
        if (closed.get()) {
            throw new IOException("HTTP/2 body is closed");
        }
        if (opened) {
            throw new IOException("HTTP/2 body reader was already opened");
        }
        opened = true;
        return new InputStream() {
            private final byte[] singleByte = new byte[1];
            private long position;
            private boolean readerClosed;

            @Override
            public int read() throws IOException {
                int count = read(singleByte, 0, 1);
                return count < 0 ? -1 : singleByte[0] & 0xff;
            }

            @Override
            public int read(byte[] bytes, int offset, int count) throws IOException {
                if (bytes == null) {
                    throw new NullPointerException("bytes");
                }
                if (offset < 0 || count < 0 || offset > bytes.length - count) {
                    throw new IndexOutOfBoundsException();
                }
                if (readerClosed || closed.get()) {
                    throw new IOException("HTTP/2 body reader is closed");
                }
                if (count == 0) {
                    return 0;
                }
                if (position == length) {
                    close();
                    return -1;
                }

                int boundedCount = (int) Math.min(Math.min((long) count, MAX_READ_CHUNK_BYTES), length - position);
                int read = channel.read(ByteBuffer.wrap(bytes, offset, boundedCount), position);
                if (read <= 0) {
                    throw new IOException("Incomplete HTTP/2 body spool");
                }
                position += read;
                return read;
            }

            @Override
            public void close() throws IOException {
                if (!readerClosed) {
                    readerClosed = true;
                    H2SealedBody.this.close();
                }
            }
        };
    }

    /** A repeatable, bounded read for an application-owned uncompressed replay. */
    public int readAt(long position, byte[] bytes, int offset, int count) throws IOException {
        if (bytes == null) {
            throw new NullPointerException("bytes");
        }
        if (offset < 0 || count < 0 || offset > bytes.length - count || position < 0 || position > length) {
            throw new IndexOutOfBoundsException();
        }
        synchronized (ioLock) {
            if (closed.get()) {
                throw new IOException("HTTP/2 body is closed");
            }
            if (opened) {
                throw new IOException("HTTP/2 one-shot body reader was already opened");
            }
            if (count == 0) {
                return 0;
            }
            if (position == length) {
                return -1;
            }
            int boundedCount = (int) Math.min(Math.min((long) count, MAX_READ_CHUNK_BYTES), length - position);
            int read = channel.read(ByteBuffer.wrap(bytes, offset, boundedCount), position);
            if (read <= 0) {
                throw new IOException("Incomplete HTTP/2 body spool");
            }
            return read;
        }
    }

    /** Schedules close and deletion without doing file I/O on the caller's thread. */
    public synchronized CompletionStage<Void> closeAsync() {
        if (asynchronousClose == null) {
            CompletableFuture<Void> result = new CompletableFuture<>();
            asynchronousClose = result;
            lease.onCloseRequested();
            try {
                cleanupExecutor.execute(() -> {
                    try {
                        close();
                        result.complete(null);
                    } catch (IOException | RuntimeException | Error e) {
                        synchronized (H2SealedBody.this) {
                            asynchronousClose = null; // allow a later deletion retry
                        }
                        result.completeExceptionally(e);
                    }
                });
            } catch (RuntimeException e) {
                asynchronousClose = null; // the executor may accept a later retry
                result.completeExceptionally(e);
            }
            return result;
        }
        return asynchronousClose;
    }

    /** Close and delete the private file. Call this only off the event loop. */
    @Override
    public void close() throws IOException {
        closed.set(true);
        try {
            synchronized (ioLock) {
                IOException failure = null;
                if (!channelClosed) {
                    try {
                        channel.close();
                        channelClosed = true;
                    } catch (IOException e) {
                        failure = e;
                    }
                }
                if (channelClosed && !deleted) {
                    try {
                        Files.deleteIfExists(path);
                        deleted = true;
                    } catch (IOException e) {
                        if (failure == null) {
                            failure = e;
                        } else {
                            failure.addSuppressed(e);
                        }
                    }
                }
                if (failure != null) {
                    throw failure;
                }
                if (channelClosed && deleted) {
                    lease.releaseAfterDelete();
                }
            }
        } finally {
            lease.onCloseRequested();
        }
    }
}
