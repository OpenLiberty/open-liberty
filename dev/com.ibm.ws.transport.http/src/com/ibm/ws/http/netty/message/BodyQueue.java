/*******************************************************************************
 * Copyright 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.message;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

final public class BodyQueue {

    private static final int DEFAULT_HIGH = 256 * 1024;
    private static final int DEFAULT_LOW = 64 * 1024;

    private final ConcurrentLinkedQueue<ByteBuf> queue = new ConcurrentLinkedQueue<>();
    private final int lowWater, highWater;
    private final AtomicInteger buffered = new AtomicInteger();
    private volatile boolean eos;
    private volatile Throwable error;
    private final ByteBufAllocator allocator;

    /** Total bytes received, including fragments discarded during purge. */
    private volatile long bytesReceived;

    /**
     * True once {@link #drainAndRelease()} has been called; subsequent
     * {@link #enqueueRetained} calls discard without retaining.
     */
    private volatile boolean purging = false;

    private final Object signalLock = new Object();
    private long signal;

    public BodyQueue(ByteBufAllocator allocator) {
        this(allocator, DEFAULT_HIGH, DEFAULT_LOW);
    }

    public BodyQueue(ByteBufAllocator allocator, int high, int low) {
        this.allocator = allocator;
        this.highWater = Math.max(high, low);
        this.lowWater = low;
    }

    /**
     * Enqueues a retained copy of {@code buf} on the event loop. Discards
     * without retaining when purging; the caller's release handles cleanup.
     *
     * @param buf the buffer to enqueue; caller retains ownership.
     */
    public void enqueueRetained(ByteBuf buf) {
        int readable = buf.readableBytes();

        // Count unconditionally so size-limit enforcement is accurate whether
        // or not this fragment is actually retained.
        bytesReceived += readable;

        // If a purge is in progress the queue does not acquire a reference.
        // The caller's finally block releases the enclosing HttpContent.
        // Do NOT call release() here: we did not retain, so we do not release.
        if (purging) {
            return;
        }
        queue.add(buf.retain());
        buffered.addAndGet(readable);
        signalChange();
    }

    public ByteBuf poll() {
        ByteBuf b = queue.poll();
        if (b != null) {
            buffered.addAndGet(-b.readableBytes());
        }
        return b;
    }

    public boolean wantsInput() {
        return error == null && !eos && buffered.get() < lowWater;
    }

    public boolean isEos() {
        return eos && queue.isEmpty();
    }

    /** Whether end of body was signaled, even if buffered bytes remain. */
    public boolean isEosSignaled() {
        return eos;
    }

    private void signalChange() {
        synchronized (signalLock) {
            signal++;
            signalLock.notifyAll();
        }
    }

    public long signalToken() {
        synchronized (signalLock) {
            return signal;
        }
    }

    /**
     * Blocks until the queue state changes, EOS/error is signalled, or purge
     * mode is entered. Callers must re-check {@link #isPurging()} after return.
     */
    public long awaitChange(long lastToken) throws InterruptedException {
        synchronized (signalLock) {
            while (signal == lastToken && !eos && error == null && !purging) {
                signalLock.wait();
            }
            return signal;
        }
    }

    /**
     * Returns the total bytes received, including fragments discarded during
     * purge, for use in cumulative body-size limit enforcement.
     */
    public long bytesRead() {
        return bytesReceived;
    }

    /** Returns the number of bytes currently retained in the queue. */
    public int bufferedBytes() {
        return buffered.get();
    }

    public void signalEos() {
        eos = true;
        signalChange();
    }

    public void signalError(Throwable t) {
        error = t;
        signalChange();
    }

    public Throwable error() {
        return error;
    }

    public void wakeReaders() {
        signalChange();
    }

    /**
     * Marks the queue as purging and releases all retained fragments. Must be
     * called on the event loop (mutually exclusive with {@link #enqueueRetained}).
     * Wakes any reader blocked in {@link #awaitChange} via {@link #signalChange()}.
     */
    public void drainAndRelease() {
        purging = true;

        ByteBuf buf;
        while ((buf = queue.poll()) != null) {
            buffered.addAndGet(-buf.readableBytes());
            buf.release();
        }

        signalChange();
    }

    /**
     * Returns {@code true} if this queue has been put into purge mode via
     * {@link #drainAndRelease()}.
     */
    public boolean isPurging() {
        return purging;
    }
}
