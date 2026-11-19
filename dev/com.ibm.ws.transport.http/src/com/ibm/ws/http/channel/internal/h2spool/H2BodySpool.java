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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

import io.netty.buffer.ByteBuf;

/**
 * Bounded, per-stream storage for an ordinary HTTP/2 request before END_STREAM.
 * The resource lease owns aggregate storage and file-descriptor reservations.
 */
public final class H2BodySpool {
    private static final int MAX_CHUNK_BYTES = 16 * 1024;

    private final Path directory;
    private final long maxStoredBytes;
    private final int maxPendingBytes;
    private final int maxDataFrameBytes;
    private final int chunkBytes;
    private final Executor writer;
    private final Executor cleanupExecutor;
    private final H2SpoolResources.Lease lease;
    private final BiConsumer<Integer, Throwable> onChunkSettled;
    private final Object lock = new Object();
    private final Object drainLock = new Object();
    private final ArrayDeque<Chunk> pending = new ArrayDeque<>();
    private final CompletableFuture<H2SealedBody> sealed = new CompletableFuture<>();
    private volatile CompletableFuture<Void> cleaned = new CompletableFuture<>();

    private Chunk tail;
    private long acceptedBytes;
    private long writtenBytes;
    private int allocatedBytes;
    private int pendingBytes;
    private boolean rejectionNotified;
    private boolean terminalFailureNotified;
    private boolean workerScheduled;
    private boolean cleanupScheduled;
    private boolean endRequested;
    private boolean aborted;
    private boolean transferred;
    private Throwable abortCause;
    private Path path;
    private FileChannel channel;

    /** Both executors must do file work off the event loop; cleanup must outlive writer shutdown. */
    H2BodySpool(Path directory, long maxStoredBytes, int maxPendingBytes, int maxDataFrameBytes, Executor writer,
                Executor cleanupExecutor, H2SpoolResources.Lease lease, BiConsumer<Integer, Throwable> onChunkSettled) {
        if (directory == null || writer == null || cleanupExecutor == null || lease == null || onChunkSettled == null
                        || maxStoredBytes < 0 || maxDataFrameBytes <= 0 || maxPendingBytes < maxDataFrameBytes) {
            throw new IllegalArgumentException("Invalid HTTP/2 body spool capacity");
        }
        this.directory = directory;
        this.maxStoredBytes = maxStoredBytes;
        this.maxPendingBytes = maxPendingBytes;
        this.maxDataFrameBytes = maxDataFrameBytes;
        this.chunkBytes = Math.min(MAX_CHUNK_BYTES, maxPendingBytes);
        this.writer = writer;
        this.cleanupExecutor = cleanupExecutor;
        this.lease = lease;
        this.onChunkSettled = onChunkSettled;
    }

    /**
     * Copies DATA into reserved, finite byte chunks; no ByteBuf is retained.
     * On capacity rejection no body bytes are copied. The stream's chunk
     * callback reports each settled chunk so the adapter can release deferred
     * H2 credit; bounded early credit may already have been granted on admission.
     */
    public void offer(ByteBuf data) throws CapacityException {
        if (data == null) {
            throw new NullPointerException("data");
        }
        int length = data.readableBytes();
        if (length > maxDataFrameBytes) {
            throw new IllegalArgumentException("DATA exceeds configured HTTP/2 frame bound");
        }
        boolean schedule = false;
        synchronized (lock) {
            if (aborted || endRequested || transferred) {
                throw new IllegalStateException("HTTP/2 body spool is terminal");
            }
            if (length == 0) {
                return;
            }
            int availableInTail = tail == null ? 0 : chunkBytes - tail.used;
            long remaining = Math.max(0L, (long) length - availableInTail);
            long newChunks = (remaining + chunkBytes - 1) / chunkBytes;
            if (length > maxStoredBytes - acceptedBytes) {
                throw new CapacityException("HTTP/2 body storage quota exceeded");
            }
            if (length > maxPendingBytes - pendingBytes || allocatedBytes + newChunks * chunkBytes > maxPendingBytes) {
                throw new CapacityException("HTTP/2 body pending-byte quota exceeded");
            }
            int newAllocation = (int) (newChunks * chunkBytes);
            int created = 0;
            if (!lease.reserve(length, newAllocation)) {
                throw new CapacityException("HTTP/2 connection spool quota exceeded");
            }

            Chunk oldTail = tail;
            int oldTailUsed = oldTail == null ? 0 : oldTail.used;
            try {
                int sourceIndex = data.readerIndex();
                int left = length;
                while (left > 0) {
                    if (tail == null || tail.used == chunkBytes) {
                        tail = new Chunk(chunkBytes);
                        pending.addLast(tail);
                        created++;
                        allocatedBytes += chunkBytes;
                    }
                    int copied = Math.min(left, chunkBytes - tail.used);
                    data.getBytes(sourceIndex, tail.bytes, tail.used, copied);
                    tail.used += copied;
                    sourceIndex += copied;
                    left -= copied;
                }
            } catch (RuntimeException | Error e) {
                if (oldTail != null) {
                    Arrays.fill(oldTail.bytes, oldTailUsed, oldTail.bytes.length, (byte) 0);
                    oldTail.used = oldTailUsed;
                }
                for (int i = 0; i < created; i++) {
                    Chunk chunk = pending.removeLast();
                    Arrays.fill(chunk.bytes, (byte) 0);
                    allocatedBytes -= chunkBytes;
                }
                tail = oldTail;
                lease.releaseOffer(length, newAllocation);
                throw e;
            }
            pendingBytes += length;
            acceptedBytes += length;
            if (!workerScheduled) {
                workerScheduled = true;
                schedule = true;
            }
        }
        if (schedule) {
            RuntimeException rejection = scheduleWriter();
            if (rejection != null) {
                // This DATA was accepted; the caller must reset the stream now.
                throw rejection;
            }
        }
    }

    /** Completes after ordered writes and transfers exclusive file ownership. */
    public CompletionStage<H2SealedBody> seal() {
        boolean schedule = false;
        synchronized (lock) {
            if (!endRequested && !aborted && !transferred) {
                endRequested = true;
                if (!workerScheduled) {
                    workerScheduled = true;
                    schedule = true;
                }
            }
        }
        if (schedule) {
            scheduleWriter();
        }
        return sealed;
    }

    /** After seal the returned H2SealedBody owns cleanup. */
    public CompletionStage<Void> abort(Throwable cause) {
        Throwable terminal = cause == null ? new IOException("HTTP/2 body spool aborted") : cause;
        CompletableFuture<Void> attempt;
        synchronized (lock) {
            if (transferred) {
                return CompletableFuture.completedFuture(null);
            }
            if (!aborted) {
                aborted = true;
                abortCause = terminal;
            }
            if (cleaned.isCompletedExceptionally()) {
                cleaned = new CompletableFuture<>();
                cleanupScheduled = false;
            }
            attempt = cleaned;
        }
        requestCleanup();
        return attempt;
    }

    /** Accepted application DATA bytes, excluding H2 padding. */
    public long acceptedBytes() {
        synchronized (lock) {
            return acceptedBytes;
        }
    }

    private RuntimeException scheduleWriter() {
        try {
            writer.execute(this::drain);
            return null;
        } catch (RuntimeException rejected) {
            fail(rejected);
            requestCleanup();
            return rejected;
        }
    }

    private void requestCleanup() {
        synchronized (lock) {
            if (transferred || cleanupScheduled) {
                return;
            }
            cleanupScheduled = true;
        }
        try {
            cleanupExecutor.execute(this::drain);
        } catch (RuntimeException rejected) {
            discardWithoutFileIO(rejected);
        }
    }

    /** A cancelled queued writer may run after cleanup; only one task owns the file at a time. */
    private void drain() {
        synchronized (drainLock) {
            synchronized (lock) {
                if (transferred) {
                    return;
                }
            }
            while (true) {
                Chunk next;
                synchronized (lock) {
                    if (aborted) {
                        break;
                    }
                    next = pending.pollFirst();
                    if (next == null && !endRequested) {
                        workerScheduled = false;
                        return;
                    }
                    if (next == tail) {
                        tail = null;
                    }
                }
                if (next == null) {
                    finish();
                    return;
                }
                write(next);
            }
            cleanupAborted();
        }
    }

    private void write(Chunk chunk) {
        Throwable failure = null;
        try {
            ensureFile();
            synchronized (lock) {
                if (aborted) {
                    // The stream was cancelled after this chunk was dequeued.
                    // Settle it with the original cause without reporting a new IO failure.
                    failure = abortCause;
                    return;
                }
            }
            ByteBuffer buffer = ByteBuffer.wrap(chunk.bytes, 0, chunk.used);
            while (buffer.hasRemaining()) {
                if (channel.write(buffer) <= 0) {
                    throw new IOException("No progress writing HTTP/2 body spool");
                }
            }
        } catch (Throwable e) {
            failure = e;
            fail(e);
        } finally {
            Arrays.fill(chunk.bytes, (byte) 0);
            synchronized (lock) {
                pendingBytes -= chunk.used;
                allocatedBytes -= chunk.bytes.length;
                if (failure == null) {
                    writtenBytes += chunk.used;
                } else {
                    terminalFailureNotified = true;
                }
            }
            lease.releasePending(chunk.bytes.length);
            notifyChunkSettled(chunk.used, failure);
        }
    }

    private void finish() {
        try {
            ensureFile();
            channel.force(false);
            H2SealedBody body;
            synchronized (lock) {
                if (aborted) {
                    body = null;
                } else {
                    if (writtenBytes != acceptedBytes || pendingBytes != 0 || allocatedBytes != 0) {
                        throw new IOException("Incomplete HTTP/2 body spool");
                    }
                    body = new H2SealedBody(path, channel, writtenBytes, cleanupExecutor, lease);
                    path = null;
                    channel = null;
                    transferred = true;
                    workerScheduled = false;
                }
            }
            if (body == null) {
                cleanupAborted();
                return;
            }
            if (!lease.onSealed(body)) {
                IOException closedConnection = new IOException("HTTP/2 connection closed before body delivery");
                sealed.completeExceptionally(closedConnection);
                body.closeAsync().whenComplete((ignored, error) -> {
                    if (error == null) {
                        cleaned.complete(null);
                    } else {
                        cleaned.completeExceptionally(error);
                    }
                });
                return;
            }
            sealed.complete(body);
            cleaned.complete(null);
        } catch (Throwable e) {
            fail(e);
            cleanupAborted();
        }
    }

    private void ensureFile() throws IOException {
        if (channel != null) {
            return;
        }
        Path created;
        try {
            created = Files.createTempFile(directory, "h2-body-", ".tmp",
                            PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        } catch (UnsupportedOperationException e) {
            throw new IOException("Private HTTP/2 body spool requires POSIX file permissions", e);
        }
        path = created;
        try {
            channel = FileChannel.open(created, StandardOpenOption.READ, StandardOpenOption.WRITE);
        } catch (IOException | RuntimeException e) {
            try {
                Files.deleteIfExists(created);
                path = null;
            } catch (IOException cleanupFailure) {
                e.addSuppressed(cleanupFailure); // keep path for the abort cleanup retry
            }
            throw e;
        }
    }

    private void fail(Throwable cause) {
        synchronized (lock) {
            if (!aborted) {
                aborted = true;
                abortCause = cause;
            }
        }
    }

    /** A second executor rejection discards RAM, but retains file ownership for retry. */
    private void discardWithoutFileIO(Throwable cause) {
        int releasedPending;
        int discardedBytes = 0;
        boolean notifyFailure;
        synchronized (lock) {
            tail = null;
            releasedPending = 0;
            while (!pending.isEmpty()) {
                Chunk chunk = pending.removeFirst();
                Arrays.fill(chunk.bytes, (byte) 0);
                pendingBytes -= chunk.used;
                allocatedBytes -= chunk.bytes.length;
                releasedPending += chunk.bytes.length;
                discardedBytes += chunk.used;
            }
            notifyFailure = !rejectionNotified && (discardedBytes > 0 || !terminalFailureNotified);
            rejectionNotified = true;
            if (notifyFailure) {
                terminalFailureNotified = true;
            }
            // Keep the lease's pending count in step with the detached queue.
            lease.releasePending(releasedPending);
        }
        IOException cleanupFailure = new IOException("HTTP/2 body cleanup executor rejected file cleanup", cause);
        // Failure only: the callback defers reset to the event loop, even if this caller is that loop.
        if (notifyFailure) {
            notifyChunkSettled(discardedBytes, cleanupFailure);
        }
        sealed.completeExceptionally(cleanupFailure);
        cleaned.completeExceptionally(cleanupFailure);
    }

    private void cleanupAborted() {
        List<Chunk> discarded = new ArrayList<>();
        Throwable cause;
        int releasedPending;
        boolean notifyEmptyFailure;
        synchronized (lock) {
            while (!pending.isEmpty()) {
                discarded.add(pending.removeFirst());
            }
            tail = null;
            pendingBytes = 0;
            releasedPending = allocatedBytes;
            allocatedBytes = 0;
            workerScheduled = false;
            cause = abortCause == null ? new IOException("HTTP/2 body spool aborted") : abortCause;
            notifyEmptyFailure = discarded.isEmpty() && !terminalFailureNotified;
            terminalFailureNotified = true;
        }
        for (Chunk chunk : discarded) {
            Arrays.fill(chunk.bytes, (byte) 0);
        }
        lease.releasePending(releasedPending);
        if (notifyEmptyFailure) {
            notifyChunkSettled(0, cause);
        }
        for (Chunk chunk : discarded) {
            notifyChunkSettled(chunk.used, cause);
        }
        Throwable cleanupFailure = null;
        try {
            if (channel != null) {
                channel.close();
            }
        } catch (IOException | RuntimeException | Error e) {
            cleanupFailure = e;
        }
        try {
            if (path != null) {
                Files.deleteIfExists(path);
            }
        } catch (IOException | RuntimeException | Error e) {
            if (cleanupFailure == null) {
                cleanupFailure = e;
            } else {
                cleanupFailure.addSuppressed(e);
            }
        }
        sealed.completeExceptionally(cause);
        if (cleanupFailure == null) {
            synchronized (lock) {
                channel = null;
                path = null;
            }
            lease.releaseAfterDelete();
            cleaned.complete(null);
        } else {
            cleaned.completeExceptionally(cleanupFailure);
        }
    }

    private void notifyChunkSettled(int bytes, Throwable failure) {
        try {
            onChunkSettled.accept(bytes, failure);
        } catch (RuntimeException | Error callbackFailure) {
            fail(callbackFailure);
        }
    }

    public static final class CapacityException extends Exception {
        private static final long serialVersionUID = 1L;

        CapacityException(String message) {
            super(message);
        }
    }

    private static final class Chunk {
        final byte[] bytes;
        int used;

        Chunk(int capacity) {
            bytes = new byte[capacity];
        }
    }
}
