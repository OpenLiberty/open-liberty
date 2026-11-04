/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.pipeline.http2;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpMessages;

/**
 * Tracks per-connection HTTP/2 rate and flow-control state for the Netty transport
 */
public class NettyH2RateState {

    private static final TraceComponent tc = Tr.register(NettyH2RateState.class,
                                                         HttpMessages.HTTP_TRACE_NAME,
                                                         HttpMessages.HTTP_BUNDLE);

    private final int  maxLowWindowStreams;
    private final int  lowWindowLimit;
    private final long maxQueuedBytes;

    /** Stream IDs currently tracked as having a low remote flow-control window. */
    private final Set<Integer> lowWindowStreams = new HashSet<>();

    /**
     * Bytes that were parked on streams reset by the deferred-write timeout.
     * They are never given back, so a peer that keeps letting writes time out
     * moves the connection towards maxQueuedBytes (legacy handleWriteTimeout parity).
     */
    private long    timedOutBytes            = 0L;
    private boolean queuedBytesLimitExceeded = false;

    /**
     * Outbound DATA bytes Netty's remote flow controller is holding, per stream, as reported by
     * LibertyUpgradeCodec.ParkedBytesReporter. These are Netty's own counts
     * ({@code StreamState.pendingBytes()}), not predictions. Streams with 0 are not kept.
     */
    private final Map<Integer, Long> parkedBytesByStream = new HashMap<>();

    /** Sum of {@link #parkedBytesByStream}. */
    private long parkedBytes = 0L;

    /** Told whenever a stream's parked bytes change (the encoder wrapper, for its write timeouts). */
    private ParkedBytesListener parkedBytesListener;

    /**
     * Notified when a stream's parked byte count changes.
     */
    public interface ParkedBytesListener {
        /**
         * @param streamId  the stream whose parked bytes changed
         * @param oldParked the previous count
         * @param newParked the new count; 0 = nothing left in the flow controller (written, or dropped
         *                      because the stream was reset or closed)
         */
        void parkedBytesChanged(int streamId, long oldParked, long newParked);
    }

    private int upgradeStreamId     = -1;

    /**
     * Result of a checkQueuedBytes call.
     */
    public enum QueuedBytesResult {
        /** Bytes accounted; within limit. */
        SUCCESS,
        /** This call crossed the threshold — caller should send GOAWAY. */
        FIRST_TO_EXCEED,
        /** Limit was already exceeded by a prior call — caller should fail silently. */
        ALREADY_EXCEEDED
    }

    
    public NettyH2RateState(HttpChannelConfig config) {
        this(config.getH2MaxLowWindowStreams(), config.getH2LowWindowLimit(), config.getH2MaxQueuedBytes());
    }

    public NettyH2RateState(int maxLowWindowStreams, int lowWindowLimit, long maxQueuedBytes) {
        this.maxLowWindowStreams = maxLowWindowStreams;
        this.lowWindowLimit      = lowWindowLimit;
        this.maxQueuedBytes      = maxQueuedBytes;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "NettyH2RateState created; maxLowWindowStreams=" + maxLowWindowStreams
                         + " lowWindowLimit=" + lowWindowLimit
                         + " maxQueuedBytes=" + maxQueuedBytes);
        }
    }

    public int getLowWindowLimit() {
        return lowWindowLimit;
    }

    public int getMaxLowWindowStreams() {
        return maxLowWindowStreams;
    }

    public int getLowWindowStreamCount() {
        return lowWindowStreams.size();
    }

    public long getMaxQueuedBytes() {
        return maxQueuedBytes;
    }

    public void setUpgradeStreamId(int streamId) {
        this.upgradeStreamId = streamId;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "setUpgradeStreamId: stream " + streamId + " excluded from low window accounting");
        }
    }

    public int getUpgradeStreamId() {
        return upgradeStreamId;
    }

    /**
     * Add a stream to the low window tracking set.
     * @param streamId the ID of the stream with low window
     * @return true if the stream was newly added
     */
    public boolean addLowWindowStream(int streamId) {
        if (streamId == upgradeStreamId) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "addLowWindowStream: stream " + streamId + " is the h2c upgrade stream, not counted");
            }
            return false;
        }
        boolean added = lowWindowStreams.add(streamId);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "addLowWindowStream: stream " + streamId
                         + (added ? " added" : " already tracked")
                         + ", total: " + lowWindowStreams.size());
        }
        return added;
    }

    /**
     * Remove a stream from low-window tracking set.
     * Should be called when the stream's window recovers above the threshold
     * WINDOW_UPDATE received or when the stream closes.
     *
     * @return true if the stream was being tracked
     */
    public boolean removeLowWindowStream(int streamId) {
        boolean removed = lowWindowStreams.remove(streamId);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() && removed) {
            Tr.debug(tc, "removeLowWindowStream: stream " + streamId
                         + " removed, total: " + lowWindowStreams.size());
        }
        return removed;
    }

    /**
     * @return true if the current low-window stream count exceeds the configured limit
     */
    public boolean tooManyLowWindowStreams() {
        if (maxLowWindowStreams > 0 && lowWindowStreams.size() > maxLowWindowStreams) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "tooManyLowWindowStreams: " + lowWindowStreams.size()
                             + " > " + maxLowWindowStreams);
            }
            return true;
        }
        return false;
    }

    /**
     * Pre-admission check: would adding streamId to the tracking set
     * cause the count to exceed the configured limit
     *
     * @return true if adding the stream would exceed the limit
     */
    public boolean wouldExceedLowWindowStreams(int streamId) {
        if (streamId == upgradeStreamId || maxLowWindowStreams <= 0) {
            return false;
        }
        // If streamId is already tracked, adding it again changes nothing.
        int projected = lowWindowStreams.contains(streamId)
                        ? lowWindowStreams.size()
                        : lowWindowStreams.size() + 1;
        return projected > maxLowWindowStreams;
    }

    /**
     * Record a stream's parked byte count. Called by the flow controller (through
     * LibertyUpgradeCodec.ParkedBytesReporter) every time that count changes: a frame is queued or
     * merged, bytes are written out, or queued frames are dropped on reset/close.
     * <p>
     * Event loop only, like everything else in this class.
     *
     * @param streamId the stream
     * @param parked   the flow controller's pending bytes for that stream, after the change
     */
    public void parkedBytesChanged(int streamId, long parked) {
        Long previous = parked == 0 ? parkedBytesByStream.remove(streamId)
                                    : parkedBytesByStream.put(streamId, parked);
        long old = previous == null ? 0L : previous;
        if (old == parked) {
            return; // a window-only change
        }
        parkedBytes += parked - old;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "parkedBytesChanged: stream " + streamId + " " + old + " -> " + parked
                         + ", connection total " + parkedBytes);
        }
        if (parkedBytesListener != null) {
            parkedBytesListener.parkedBytesChanged(streamId, old, parked);
        }
    }

    public void parkedBytesListener(ParkedBytesListener listener) {
        this.parkedBytesListener = listener;
    }

    /** @return bytes the remote flow controller holds across all streams */
    public long getParkedBytes() {
        return parkedBytes;
    }

    /** @return bytes the remote flow controller holds for {@code streamId} */
    public long getParkedBytes(int streamId) {
        Long parked = parkedBytesByStream.get(streamId);
        return parked == null ? 0L : parked;
    }

    /** @return the streams that currently have parked bytes (a live, read-only view) */
    public Set<Integer> getStreamsWithParkedBytes() {
        return Collections.unmodifiableSet(parkedBytesByStream.keySet());
    }

    /**
     * Check what the flow controller holds right now (plus timed-out bytes) against maxQueuedBytes.
     * Used after a flush, when whatever is still held really is parked.
     *
     * @see #checkQueuedBytes(long)
     */
    public QueuedBytesResult checkQueuedBytes() {
        return checkQueuedBytes(parkedBytes);
    }

    /**
     * Check a parked byte count (plus timed-out bytes) against maxQueuedBytes.
     * <p>
     * Pass the current total ({@link #checkQueuedBytes()}), or, before a frame is queued, a lower
     * bound on what will still be parked after the next flush. Either way the count is never higher
     * than what will really be parked, so a trip is never a false positive.
     *
     * @param parkedBytes bytes that are, or will certainly be, held in the remote flow controller
     * @return SUCCESS if within the limit (or the check is disabled); FIRST_TO_EXCEED the first time
     *         the limit is crossed (caller sends GOAWAY); ALREADY_EXCEEDED on every call after that
     */
    public QueuedBytesResult checkQueuedBytes(long parkedBytes) {
        if (maxQueuedBytes <= 0) {
            return QueuedBytesResult.SUCCESS; // check disabled
        }
        if (queuedBytesLimitExceeded) {
            return QueuedBytesResult.ALREADY_EXCEEDED;
        }
        long total = parkedBytes + timedOutBytes;
        if (total > maxQueuedBytes) {
            queuedBytesLimitExceeded = true;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "checkQueuedBytes: LIMIT EXCEEDED - parked " + parkedBytes
                             + " + timed out " + timedOutBytes + " = " + total
                             + " > " + maxQueuedBytes);
            }
            return QueuedBytesResult.FIRST_TO_EXCEED;
        }
        return QueuedBytesResult.SUCCESS;
    }

    /**
     * @return true once checkQueuedBytes has returned FIRST_TO_EXCEED
     */
    public boolean isQueuedBytesLimitExceeded() {
        return queuedBytesLimitExceeded;
    }

    /**
     * Record bytes that were parked on a stream when its write timeout fired. They stay charged to
     * the connection for its lifetime (legacy handleWriteTimeout parity), even after the reset drops
     * them from the flow controller.
     */
    public void addTimedOutBytes(long bytes) {
        if (bytes <= 0) {
            return;
        }
        timedOutBytes += bytes;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "addTimedOutBytes: +" + bytes + ", total timed out: " + timedOutBytes);
        }
    }

    public long getTimedOutBytes() {
        return timedOutBytes;
    }
}
