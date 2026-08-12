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

import java.util.HashSet;
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

    private long    queuedBytesCount         = 0L;
    private boolean queuedBytesLimitExceeded = false;
    private volatile int upgradeStreamId     = -1;

    /**
     * Result of a  tryIncrementQueuedBytes call.
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

    public synchronized int getLowWindowStreamCount() {
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
    public synchronized boolean addLowWindowStream(int streamId) {
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
    public synchronized boolean removeLowWindowStream(int streamId) {
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
    public synchronized boolean tooManyLowWindowStreams() {
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
     * Pre-admission check: would adding {@code streamId} to the tracking set
     * cause the count to exceed the configured limit
     *
     * @return true if adding the stream would exceed the limit
     */
    public synchronized boolean wouldExceedLowWindowStreams(int streamId) {
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
     * Attempt to increment the queued outbound bytes that are
     * about to be parked in Netty's flow-control queue.
     *
     * @return SUCCESS if within limit; FIRST_TO_EXCEED if this call crossed the
     *         threshold (caller should send GOAWAY); ALREADY_EXCEEDED if a prior
     *         call already crossed it (caller should fail silently)
     */
    public synchronized QueuedBytesResult tryIncrementQueuedBytes(long bytes) {
        if (maxQueuedBytes <= 0) {
            return QueuedBytesResult.SUCCESS; // check disabled
        }
        if (queuedBytesLimitExceeded) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "tryIncrementQueuedBytes: REJECTED - limit already exceeded, current: "
                             + queuedBytesCount);
            }
            return QueuedBytesResult.ALREADY_EXCEEDED;
        }
        long newTotal = queuedBytesCount + bytes;
        if (newTotal > maxQueuedBytes) {
            queuedBytesLimitExceeded = true;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "tryIncrementQueuedBytes: LIMIT EXCEEDED - would be " + newTotal
                             + " bytes, limit is " + maxQueuedBytes
                             + ", rejecting " + bytes + " bytes");
            }
            return QueuedBytesResult.FIRST_TO_EXCEED;
        }
        queuedBytesCount = newTotal;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "tryIncrementQueuedBytes: added " + bytes
                         + " bytes, total queued: " + queuedBytesCount);
        }
        return QueuedBytesResult.SUCCESS;
    }

    /**
     * Decrement the queued-bytes counter when a previously accounted frame has
     * been written or discarded.
     */
    public synchronized void decrementQueuedBytes(long bytes) {
        queuedBytesCount -= bytes;
        if (queuedBytesCount < 0) {
            queuedBytesCount = 0; // Safety check
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "decrementQueuedBytes: -" + bytes
                         + ", total now: " + queuedBytesCount);
        }
    }
}
