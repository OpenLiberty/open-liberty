/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.channel.h2internal;

import java.util.concurrent.TimeUnit;

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
    private static int maxResetFrames = 100;
    private static int resetFrameWindow = 30000; // milliseconds
    private static int maxStreamsRefused = 100;

    private volatile long controlFrameCount = 0L;
    private volatile int resetCount = 0;

    private volatile int emptyFrameReceivedCount = 0;
    private volatile int inboundResetCount = 0;
    private volatile int refusedStreamCount = 0;
    private volatile long startResetTime = System.nanoTime();

    public H2RateState(int maxResetFrames, int resetFrameWindow, int maxStreamsRefused) {
        this.maxResetFrames = maxResetFrames;
        this.resetFrameWindow = resetFrameWindow;
        this.maxStreamsRefused = maxStreamsRefused;
    }

    // Counting the number of outgoing resets
    public synchronized void setStreamReset() {
        resetCount++;
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

    public synchronized int getRefusedStreamCount() {
        return refusedStreamCount;
    }

    public synchronized int getMaxStreamsRefused() {
        return maxStreamsRefused;
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
        if ((controlFrameCount > maxReadControlFrameCount) || (resetCount > maxResetFrameCount)) {
            return true;
        }
        return false;
    }

    /**
     * @return true if the connection is considered to be misbehaving
     */
    public synchronized boolean isInboundResetsInTimeExceeded() {
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
                        Tr.debug(tc, "number of resets in time is " + inboundResetCount);
                    }
                    if (inboundResetCount >= maxResetFrames) {
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
                    inboundResetCount = 0;
                }
            } else {
                // Unlimited time window
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "number of resets in unlimited time is " + inboundResetCount);
                }
                if (inboundResetCount >= maxResetFrames) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "reset frames in unlimited window exceeded: " + inboundResetCount);
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
            inboundResetCount++;
        }
    }

    public synchronized void incrementInboundResetFrameCount() {
        inboundResetCount++;
    }

    /**
     * @return true if the connection is considered to be misbehaving
     */
    public synchronized boolean isInboundControlRatioExceeded(int totalClientStreams) {
        if (inboundResetCount > maxResetFrameCount) {
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
        result.append("inboundResetCount: " + inboundResetCount);
        result.append(newLine);
        result.append("outboundResetCount: " + resetCount);
        result.append(newLine);
        result.append("resetTimePeriod: " + getReadableTime(System.nanoTime() - startResetTime));

        return result.toString();
    }
}
