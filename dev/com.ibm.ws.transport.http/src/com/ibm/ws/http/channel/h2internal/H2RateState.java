/*******************************************************************************
 * Copyright (c) 2019, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.channel.h2internal;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.h2internal.frames.Frame;
import com.ibm.ws.http.channel.internal.HttpMessages;

/**
 * Keeps track of the rate at which HTTP/2 control frames are sent and received on a given connection,
 * with the intention of detecting misbehaving clients. A few measures are tracked:
 *
 * - control frames read as a ratio of the number of non-control frames read
 * - control frames written as a ratio of the number of non-control frames written
 * - number of empty (zero-length body) data frames received on a given stream
 * - number of streams reset (outbound)
 * - number of streams that received a reset frame in a time period
 * - number of streams refused because they were over the max concurrent stream limit
 */
public class H2RateState {

    private static final TraceComponent tc = Tr.register(H2RateState.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    private static final int maxReadControlFrameCount = 10000;
    private static final int maxResetFrameCount = 5000;
    private static final int maxEmptyFrameCount = 500;
    // configurable
    private int maxResetFrames = 100;
    private int resetFrameWindow = 30000; // milliseconds
    private int maxStreamsRefused = 100;
    private int maxLowWindowStreams = 20;
    private int lowWindowLimit = 16384;
    private long maxQueuedBytes = 2 * 1024 * 1024; // 2 MB

    private volatile long controlFrameCount = 0L;
    private volatile long queuedBytesCount = 0L;
    private final AtomicBoolean queuedBytesLimitExceeded = new AtomicBoolean(false);

    private volatile int emptyFrameReceivedCount = 0;
    private volatile int resetFrameCount = 0; //tracks both inbound and outbound resets
    private volatile int refusedStreamCount = 0;
    private volatile long startResetTime = System.nanoTime();

    // Track streams that are currently in low window state to prevent double-decrement
    private final Set<Integer> lowWindowStreams = new HashSet<>();


    /**
     * Result enum for tryIncrementQueuedBytes operation
     */
    public enum QueuedBytesResult {
        /** Bytes were successfully added to the queue */
        SUCCESS,
        /** This thread is the first to exceed the limit - should send GOAWAY */
        FIRST_TO_EXCEED,
        /** Limit was already exceeded by another thread - should fail silently */
        ALREADY_EXCEEDED
    }

    public H2RateState(int maxResetFrames, int resetFrameWindow, int maxStreamsRefused, int maxLowWindowStreams, int lowWindowLimit, long maxQueuedBytes) {
        this.maxResetFrames = maxResetFrames;
        this.resetFrameWindow = resetFrameWindow;
        this.maxStreamsRefused = maxStreamsRefused;
        this.maxLowWindowStreams = maxLowWindowStreams;
        this.lowWindowLimit = lowWindowLimit;
        this.maxQueuedBytes = maxQueuedBytes;
    }

    public synchronized void incrementReadControlFrameCount() {
        controlFrameCount++;
    }

    public synchronized void incrementReadNonControlFrameCount() {
        controlFrameCount = controlFrameCount / 2;
    }

    public synchronized void incrementEmptyFrameReceivedCount() {
        emptyFrameReceivedCount++;
    }

    public synchronized void incrementRefusedStreamCount() {
        refusedStreamCount++;
    }

    /**
     * Add a stream to the low window tracking set.
     * This method tracks the stream ID to prevent double-counting.
     *
     * @param streamId the ID of the stream with low window
     * @return true if the stream was added (wasn't already tracked), false otherwise
     */
    public synchronized boolean addLowWindowStream(int streamId) {
        boolean added = lowWindowStreams.add(streamId);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            if (added) {
                Tr.debug(tc, "addLowWindowStream: stream " + streamId + " added, total count: " + lowWindowStreams.size());
            } else {
                Tr.debug(tc, "addLowWindowStream: stream " + streamId + " already tracked");
            }
        }
        return added;
    }

    /**
     * Remove a stream from the low window tracking set.
     * This should be called when:
     * 1. The stream receives a window update that exceeds the low window limit
     * 2. The stream successfully writes data despite having a low window (proving it's still valid)
     *
     * @param streamId the ID of the stream that is no longer in low window state
     * @return true if the stream was removed (was being tracked), false otherwise
     */
    public synchronized boolean removeLowWindowStream(int streamId) {
        boolean removed = lowWindowStreams.remove(streamId);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            if (removed) {
                Tr.debug(tc, "removeLowWindowStream: stream " + streamId + " removed, total count: " + lowWindowStreams.size());
            } else {
                Tr.debug(tc, "removeLowWindowStream: stream " + streamId + " was not being tracked");
            }
        }
        return removed;
    }

    /**
     * Get the current count of low window streams.
     *
     * @return the number of streams currently tracked as having low windows
     */
    public synchronized int getLowWindowStreamCount() {
        return lowWindowStreams.size();
    }

    public synchronized int getRefusedStreamCount() {
        return refusedStreamCount;
    }

    public synchronized int getMaxStreamsRefused() {
        return maxStreamsRefused;
    }

    public synchronized boolean tooManyLowWindowStreams() {
        int count = lowWindowStreams.size();
        if ((maxLowWindowStreams > 0) && (count > maxLowWindowStreams)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "connection has exceeded the maximum number of low window streams: " + count + " > " + maxLowWindowStreams);
            }
            return true;
        }
        return false;
    }

    public synchronized int getLowWindowLimit() {
        return lowWindowLimit;
    }

    /**
     * Attempt to increment the queued bytes count. This method will fail if:
     * 1. The limit has already been exceeded by another thread
     * 2. Adding these bytes would exceed the limit
     *
     * This is an atomic operation that prevents race conditions where multiple
     * threads might try to queue data simultaneously.
     *
     * @param bytes the number of bytes to add to the queue
     * @return QueuedBytesResult indicating success, first to exceed, or already exceeded
     */
    public synchronized QueuedBytesResult tryIncrementQueuedBytes(long bytes) {
        // If limit already exceeded, reject immediately
        if (queuedBytesLimitExceeded.get()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "tryIncrementQueuedBytes: REJECTED - limit already exceeded, current: " + queuedBytesCount);
            }
            return QueuedBytesResult.ALREADY_EXCEEDED;
        }
        // Check if adding these bytes would exceed the limit
        long newTotal = queuedBytesCount + bytes;
        if (newTotal > maxQueuedBytes) {
            queuedBytesLimitExceeded.set(true);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "tryIncrementQueuedBytes: LIMIT EXCEEDED - would be " + newTotal +
                         " bytes, limit is " + maxQueuedBytes + ", rejecting " + bytes + " bytes");
            }
            return QueuedBytesResult.FIRST_TO_EXCEED;
        }
        // Safe to add
        queuedBytesCount = newTotal;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "tryIncrementQueuedBytes: added " + bytes + " bytes, total queued: " + queuedBytesCount);
        }
        return QueuedBytesResult.SUCCESS;
    }

    public synchronized void decrementQueuedBytes(long bytes) {
        queuedBytesCount -= bytes;
        if (queuedBytesCount < 0) {
            queuedBytesCount = 0; // Safety check
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "decrementQueuedBytes: removed " + bytes + " bytes, total queued: " + queuedBytesCount);
        }
    }

    public synchronized boolean tooManyStreamsRefused() {
        if ((maxStreamsRefused > 0) && (refusedStreamCount > maxStreamsRefused)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "streams refused: " + refusedStreamCount + " is greater than max: " + maxStreamsRefused);
            }
            return true;
        }
        return false;
    }

    /**
     * @return true if the connection is considered to be misbehaving
     */
    public synchronized boolean isControlRatioExceeded() {
        if ((controlFrameCount > maxReadControlFrameCount) || (resetFrameCount > maxResetFrameCount)) {
            return true;
        }
        return false;
    }

    /**
     * @return true if the connection is considered to be misbehaving by sending or receiving too many reset frames within the rest time window
     */
    public synchronized boolean isResetsInTimeExceeded() {
        // Are we checking the reset frames/time ?
        if (maxResetFrames > 0) {
            // Is the window limited?
            if (resetFrameWindow > 0) {
                long curResetTime = System.nanoTime();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "setting curResetTime: " + curResetTime);
                }

                if (curResetTime - startResetTime < TimeUnit.MILLISECONDS.toNanos(resetFrameWindow)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "number of resets in time is " + resetFrameCount);
                    }
                    if (resetFrameCount >= maxResetFrames) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "too many resets within time");
                        }
                        return true;
                    }

                } else {
                    // Start over with a new window
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "restarting reset frame time window " + curResetTime);
                    }
                    startResetTime = curResetTime;
                    resetFrameCount = 0;
                }
            } else {
                // Unlimited time window
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "number of resets in unlimited time is " + resetFrameCount);
                }
                if (resetFrameCount >= maxResetFrames) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "reset frames in unlimited window exceeded: " + resetFrameCount);
                    }
                    return true;
                }
            }
        }
        return false;

    }

    /*
     * As each h2 frame is received, update connection counters
     */
    public void updateCounters(Frame frame) {
        if (isControlFrame(frame)) {
            incrementReadControlFrameCount();
        } else {
            incrementReadNonControlFrameCount();
        }
        if (frame.getFrameType() == FrameTypes.RST_STREAM) {
            incrementResetFrameCount();
        }
    }

    public synchronized void incrementResetFrameCount() {
        resetFrameCount++;
    }

    /**
     * @return true if the connection is considered to be misbehaving
     */
    public synchronized boolean isInboundControlRatioExceeded(int totalClientStreams) {
        if (resetFrameCount > maxResetFrameCount) {
            return true;
        }
        return false;

    }

    /**
     * @param emptyFrameCount the number of empty frames received on a stream
     * @return true if emptyFrameCount exceeds the limit for a well-behaved stream
     */
    public synchronized boolean isStreamMisbehaving() {
        return emptyFrameReceivedCount > maxEmptyFrameCount;
    }

    /**
     * @param frame
     * @return true if frame is a control frame
     */
    public static boolean isControlFrame(Frame frame) {
        switch (frame.getFrameType()) {
            case PRIORITY:
                return true;
            case RST_STREAM:
                return true;
            case SETTINGS:
                return true;
            case PING:
                return true;
            case GOAWAY:
                return true;
            case WINDOW_UPDATE:
                return true;
            default:
                return false;
        }
    }

    private String getReadableTime(Long nanos) {

        long tempSec = nanos / (1000 * 1000 * 1000);
        long sec = tempSec % 60;
        long min = (tempSec / 60) % 60;
        long hour = (tempSec / (60 * 60)) % 24;
        long day = (tempSec / (24 * 60 * 60)) % 24;

        return String.format("%dd %dh %dm %ds", day, hour, min, sec);

    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        String newLine = System.getProperty("line.separator");

        result.append(this.getClass().getName());
        result.append(newLine);
        result.append("controlFrameRatio: " + controlFrameCount);
        result.append(newLine);
        result.append("refusedStreamCount: " + refusedStreamCount);
        result.append(newLine);
        result.append("resetFrameCount: " + resetFrameCount);
        result.append(newLine);
        result.append("resetTimePeriod: " + getReadableTime(System.nanoTime() - startResetTime));

        return result.toString();
    }
}
