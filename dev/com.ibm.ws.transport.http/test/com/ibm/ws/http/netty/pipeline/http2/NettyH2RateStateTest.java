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

import static org.junit.Assert.*;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import com.ibm.ws.http.netty.pipeline.http2.NettyH2RateState.QueuedBytesResult;

/**
 * Unit tests for {@link NettyH2RateState} which manages connection-wide HTTP/2
 * flow-control, window state, and parked bytes accounting.
 */
public class NettyH2RateStateTest {

    @Test
    public void testInitialization() {
        NettyH2RateState state = new NettyH2RateState(5, 65535, 1000000L);
        assertEquals(5, state.getMaxLowWindowStreams());
        assertEquals(65535, state.getLowWindowLimit());
        assertEquals(1000000L, state.getMaxQueuedBytes());
        assertEquals(0, state.getLowWindowStreamCount());
        assertEquals(0L, state.getParkedBytes());
        assertEquals(0L, state.getTimedOutBytes());
        assertFalse(state.isQueuedBytesLimitExceeded());
    }

    @Test
    public void testLowWindowStreamsManagement() {
        NettyH2RateState state = new NettyH2RateState(2, 1000, 100000L);

        // Track stream 3
        assertTrue(state.addLowWindowStream(3));
        assertEquals(1, state.getLowWindowStreamCount());
        assertFalse(state.tooManyLowWindowStreams());
        assertFalse(state.wouldExceedLowWindowStreams(3)); // already tracked, shouldn't increase count

        // Track stream 5
        assertTrue(state.addLowWindowStream(5));
        assertEquals(2, state.getLowWindowStreamCount());
        assertFalse(state.tooManyLowWindowStreams()); // exactly on limit (2)
        assertTrue(state.wouldExceedLowWindowStreams(7)); // adding a 3rd would exceed limit of 2

        // Try to add duplicate
        assertFalse(state.addLowWindowStream(5));
        assertEquals(2, state.getLowWindowStreamCount());

        // Add 3rd stream
        assertTrue(state.addLowWindowStream(7));
        assertEquals(3, state.getLowWindowStreamCount());
        assertTrue(state.tooManyLowWindowStreams()); // 3 > 2

        // Remove a stream
        assertTrue(state.removeLowWindowStream(5));
        assertEquals(2, state.getLowWindowStreamCount());
        assertFalse(state.tooManyLowWindowStreams());

        // Remove non-tracked stream
        assertFalse(state.removeLowWindowStream(9));
    }

    @Test
    public void testUpgradeStreamExclusion() {
        NettyH2RateState state = new NettyH2RateState(1, 1000, 100000L);
        state.setUpgradeStreamId(1);
        assertEquals(1, state.getUpgradeStreamId());

        // h2c upgrade stream is excluded from low window tracking
        assertFalse(state.addLowWindowStream(1));
        assertEquals(0, state.getLowWindowStreamCount());
        assertFalse(state.wouldExceedLowWindowStreams(1));
    }

    @Test
    public void testParkedBytesAccounting() {
        NettyH2RateState state = new NettyH2RateState(5, 1000, 100000L);

        final AtomicInteger callbackCount = new AtomicInteger(0);
        final AtomicLong lastStreamId = new AtomicLong(-1);
        final AtomicLong lastOldVal = new AtomicLong(-1);
        final AtomicLong lastNewVal = new AtomicLong(-1);

        state.parkedBytesListener((streamId, oldParked, newParked) -> {
            callbackCount.incrementAndGet();
            lastStreamId.set(streamId);
            lastOldVal.set(oldParked);
            lastNewVal.set(newParked);
        });

        // 1. Park 5000 bytes on stream 3
        state.parkedBytesChanged(3, 5000L);
        assertEquals(5000L, state.getParkedBytes());
        assertEquals(5000L, state.getParkedBytes(3));
        assertEquals(1, callbackCount.get());
        assertEquals(3, lastStreamId.get());
        assertEquals(0L, lastOldVal.get());
        assertEquals(5000L, lastNewVal.get());

        // 2. Add 2000 bytes more to stream 3 (now 7000)
        state.parkedBytesChanged(3, 7000L);
        assertEquals(7000L, state.getParkedBytes());
        assertEquals(7000L, state.getParkedBytes(3));
        assertEquals(2, callbackCount.get());
        assertEquals(3, lastStreamId.get());
        assertEquals(5000L, lastOldVal.get());
        assertEquals(7000L, lastNewVal.get());

        // 3. Park 10000 bytes on stream 5
        state.parkedBytesChanged(5, 10000L);
        assertEquals(17000L, state.getParkedBytes());
        assertEquals(10000L, state.getParkedBytes(5));

        // 4. Verify streams view
        Set<Integer> streams = state.getStreamsWithParkedBytes();
        assertEquals(2, streams.size());
        assertTrue(streams.contains(3));
        assertTrue(streams.contains(5));

        // 5. Zero out stream 3
        state.parkedBytesChanged(3, 0L);
        assertEquals(10000L, state.getParkedBytes());
        assertEquals(0L, state.getParkedBytes(3));
        assertFalse(state.getStreamsWithParkedBytes().contains(3));
    }

    @Test
    public void testQueuedBytesLimitEvaluation() {
        NettyH2RateState state = new NettyH2RateState(5, 1000, 10000L); // Cap is 10000

        // SUCCESS below cap
        assertEquals(QueuedBytesResult.SUCCESS, state.checkQueuedBytes(9000L));
        assertFalse(state.isQueuedBytesLimitExceeded());

        // SUCCESS exactly at cap (exclusive bounds: total > maxQueuedBytes is the trigger)
        assertEquals(QueuedBytesResult.SUCCESS, state.checkQueuedBytes(10000L));
        assertFalse(state.isQueuedBytesLimitExceeded());

        // FIRST_TO_EXCEED above cap
        assertEquals(QueuedBytesResult.FIRST_TO_EXCEED, state.checkQueuedBytes(10001L));
        assertTrue(state.isQueuedBytesLimitExceeded());

        // ALREADY_EXCEEDED on all subsequent checks
        assertEquals(QueuedBytesResult.ALREADY_EXCEEDED, state.checkQueuedBytes(5000L));
        assertEquals(QueuedBytesResult.ALREADY_EXCEEDED, state.checkQueuedBytes(20000L));
    }

    @Test
    public void testQueuedBytesDisabledCheck() {
        NettyH2RateState state = new NettyH2RateState(5, 1000, 0L); // Cap is disabled (<= 0)
        assertEquals(QueuedBytesResult.SUCCESS, state.checkQueuedBytes(999999L));
        assertFalse(state.isQueuedBytesLimitExceeded());
    }

    @Test
    public void testTimedOutBytesAccumulation() {
        NettyH2RateState state = new NettyH2RateState(5, 1000, 10000L); // Cap is 10000

        state.addTimedOutBytes(3000L);
        assertEquals(3000L, state.getTimedOutBytes());

        // Timed-out bytes add to the total checked
        // Total: 6000 parked + 3000 timedout = 9000 <= 10000 (SUCCESS)
        assertEquals(QueuedBytesResult.SUCCESS, state.checkQueuedBytes(6000L));
        assertFalse(state.isQueuedBytesLimitExceeded());

        // Total: 7500 parked + 3000 timedout = 10500 > 10000 (FIRST_TO_EXCEED)
        assertEquals(QueuedBytesResult.FIRST_TO_EXCEED, state.checkQueuedBytes(7500L));
        assertTrue(state.isQueuedBytesLimitExceeded());
    }
}
