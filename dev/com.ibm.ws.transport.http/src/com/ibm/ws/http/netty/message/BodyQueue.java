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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

final public class BodyQueue {

    /** One deferred settlement may cover several DATA frames copied into one chunk. */
    public interface AggregatingCompletion extends Runnable {
        void merge(AggregatingCompletion next);
    }

    private static final int DEFAULT_HIGH = 256 * 1024;
    private static final int DEFAULT_LOW = 64 * 1024;
    private static final int H2_CHUNK_SIZE = 16 * 1024;

    private final ConcurrentLinkedQueue<Fragment> queue = new ConcurrentLinkedQueue<>();
    private final int lowWater, highWater;
    private final AtomicInteger buffered = new AtomicInteger();
    private final int maxBufferedBytes;
    private final Object h2QueueLock = new Object();
    private Fragment h2Tail;
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
        this(allocator, high, low, 0);
    }

    private BodyQueue(ByteBufAllocator allocator, int high, int low, int maxBufferedBytes) {
        this.allocator = allocator;
        this.highWater = Math.max(high, low);
        this.lowWater = low;
        this.maxBufferedBytes = maxBufferedBytes;
    }

    /** A single negotiated DATA frame must fit even when its size is configured above the default budget. */
    public static BodyQueue forHttp2(ByteBufAllocator allocator, int maxFrameSize) {
        return new BodyQueue(allocator, DEFAULT_HIGH, DEFAULT_LOW, Math.max(DEFAULT_HIGH, maxFrameSize));
    }

    /**
     * Retains HTTP/1 input or copies HTTP/2 DATA into bounded queue chunks.
     * The caller retains ownership of {@code buf} in either case.
     */
    public void enqueueRetained(ByteBuf buf) {
        enqueueRetained(buf, null);
    }

    /**
     * Transfers credit completion with the queued HTTP/2 bytes. The callback
     * must be nonthrowing and runs after every chunk holding the DATA was copied
     * or discarded. Returns false when the queue lacks byte capacity.
     */
    public boolean enqueueRetained(ByteBuf buf, Runnable onTransferredOrDiscarded) {
        int readable = buf.readableBytes();

        // Count unconditionally so size-limit enforcement is accurate whether
        // or not this fragment is actually retained.
        bytesReceived += readable;

        // If a purge is in progress the queue does not acquire a reference.
        // The caller's finally block releases the enclosing HttpContent.
        // Do NOT call release() here: we did not retain, so we do not release.
        if (purging) {
            if (onTransferredOrDiscarded != null) {
                onTransferredOrDiscarded.run();
            }
            return false;
        }
        if (maxBufferedBytes > 0) {
            return enqueueHttp2(buf, readable, onTransferredOrDiscarded);
        }
        Fragment fragment = new Fragment(buf, onTransferredOrDiscarded);
        buf.retain();
        buffered.addAndGet(readable);
        try {
            queue.add(fragment);
        } catch (RuntimeException | Error failure) {
            buffered.addAndGet(-readable);
            fragment.release();
            throw failure;
        }
        signalChange();
        return true;
    }

    private boolean enqueueHttp2(ByteBuf source, int readable, Runnable completion) {
        if (readable == 0) {
            if (completion != null) {
                completion.run();
            }
            return true;
        }

        boolean accepted = false;
        synchronized (h2QueueLock) {
            if (readable <= maxBufferedBytes - buffered.get()) {
                // Large frames already have a bounded count under the byte cap.
                // Retain them directly so a negotiated large frame is not copied
                // on the event loop merely to reduce tiny-frame queue entries.
                if (readable >= H2_CHUNK_SIZE) {
                    Fragment fragment = new Fragment(source, this);
                    source.retain();
                    try {
                        queue.add(fragment);
                        fragment.account(readable, completion);
                        buffered.addAndGet(readable);
                        h2Tail = null;
                        accepted = true;
                    } catch (RuntimeException | Error failure) {
                        queue.remove(fragment);
                        source.release();
                        throw failure;
                    }
                } else {
                    Fragment target = h2Tail;
                    boolean newChunk = target == null || target.buffer.writableBytes() < readable;
                    if (newChunk) {
                        ByteBuf copy = allocator.buffer(H2_CHUNK_SIZE, H2_CHUNK_SIZE);
                        try {
                            target = new Fragment(copy, this);
                        } catch (RuntimeException | Error failure) {
                            copy.release();
                            throw failure;
                        }
                    }
                    int oldWriterIndex = target.buffer.writerIndex();
                    try {
                        target.buffer.writeBytes(source, source.readerIndex(), readable);
                        boolean appendable = target.buffer.isWritable();
                        if (newChunk) {
                            queue.add(target);
                        }
                        target.account(readable, completion);
                        buffered.addAndGet(readable);
                        h2Tail = appendable ? target : null;
                        accepted = true;
                    } catch (RuntimeException | Error failure) {
                        if (newChunk) {
                            queue.remove(target);
                            target.buffer.release();
                        } else {
                            target.buffer.writerIndex(oldWriterIndex);
                        }
                        throw failure;
                    }
                }
            }
        }
        if (!accepted) {
            if (completion != null) {
                completion.run();
            }
            return false;
        }
        signalChange();
        return true;
    }

    public ByteBuf poll() {
        Fragment fragment = pollFragment();
        if (fragment == null) {
            return null;
        }
        if (fragment.onTransferredOrDiscarded != null || fragment.owner != null) {
            fragment.release();
            throw new IllegalStateException("HTTP/2 fragment must be consumed with its credit callback");
        }
        return fragment.buffer;
    }

    public Fragment pollFragment() {
        if (maxBufferedBytes > 0) {
            synchronized (h2QueueLock) {
                Fragment fragment = queue.poll();
                if (fragment == h2Tail) {
                    h2Tail = null;
                }
                return fragment;
            }
        }
        Fragment fragment = queue.poll();
        if (fragment != null) {
            buffered.addAndGet(-fragment.buffer.readableBytes());
        }
        return fragment;
    }

    public static final class Fragment {
        private final ByteBuf buffer;
        private final Runnable onTransferredOrDiscarded;
        private final BodyQueue owner;
        private Runnable completion;
        private int accountedBytes;
        private final AtomicBoolean released = new AtomicBoolean();

        private Fragment(ByteBuf buffer, Runnable onTransferredOrDiscarded) {
            this.buffer = buffer;
            this.onTransferredOrDiscarded = onTransferredOrDiscarded;
            this.owner = null;
            this.completion = null;
        }

        private Fragment(ByteBuf buffer, BodyQueue owner) {
            this.buffer = buffer;
            this.onTransferredOrDiscarded = null;
            this.owner = owner;
            this.completion = null;
        }

        private void account(int bytes, Runnable completion) {
            if (completion != null) {
                if (!(completion instanceof AggregatingCompletion)) {
                    throw new IllegalArgumentException("HTTP/2 DATA credit must support chunk aggregation");
                }
                if (this.completion == null) {
                    this.completion = completion;
                } else {
                    ((AggregatingCompletion) this.completion).merge((AggregatingCompletion) completion);
                }
            }
            accountedBytes += bytes;
        }

        public ByteBuf buffer() {
            return buffer;
        }

        /** Release the retained ByteBuf before restoring flow credit. */
        public void release() {
            if (released.compareAndSet(false, true)) {
                try {
                    buffer.release();
                } finally {
                    if (owner != null) {
                        owner.buffered.addAndGet(-accountedBytes);
                        if (completion != null) {
                            completion.run();
                        }
                    } else if (onTransferredOrDiscarded != null) {
                        onTransferredOrDiscarded.run();
                    }
                }
            }
        }
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

        Fragment fragment;
        while ((fragment = pollFragment()) != null) {
            fragment.release();
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
