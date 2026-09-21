/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.netty.readflow;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;

import com.ibm.ws.http.channel.internal.HttpConfigConstants;
import com.ibm.ws.http.netty.NettyHttpChannelConfig;
import com.ibm.ws.http.netty.NettyHttpChannelConfig.NettyConfigBuilder;
import com.ibm.ws.http.netty.pipeline.inbound.read.FlowState;
import com.ibm.ws.http.netty.pipeline.inbound.read.PurgeStartedEvent;
import com.ibm.ws.http.netty.pipeline.inbound.read.ReadFlowHandler;
import com.ibm.ws.http.netty.pipeline.inbound.read.RequestConsumedEvent;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCountUtil;
import io.openliberty.http.netty.timeout.TimeoutHandler;
import io.openliberty.http.netty.timeout.exception.PersistTimeoutException;
import io.openliberty.http.netty.timeout.exception.ReadTimeoutException;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Regression tests for the timeout phase transitions driven by
 * {@link PurgeStartedEvent} and {@link RequestConsumedEvent}.
 *
 * <p>Pipeline: TimeoutHandler → ReadFlowHandler → CapturingHandler
 *
 * <p>Uses {@link EmbeddedChannel#advanceTimeBy} for deterministic scheduling —
 * no wall-clock sleeps.
 *
 * <h3>Expected phase sequence during purge</h3>
 * <pre>
 *   response terminal write completes → (no timer if purge pending)
 *   PurgeStartedEvent                 → READ phase (readTimeout)
 *   each HttpContent fragment         → resetRead (readTimeout resets)
 *   LastHttpContent arrives           → READ cancelled
 *   RequestConsumedEvent              → PERSIST phase (persistTimeout)
 *   next HttpRequest arrives          → PERSIST cancelled
 * </pre>
 */
public class TimeoutHandlerPhaseTransitionTests {

    /** Values passed to PROPNAME_READ_TIMEOUT / PROPNAME_PERSIST_TIMEOUT.
     *  Those properties are in SECONDS; HttpChannelConfig multiplies by 1000
     *  internally. TimeoutHandler.readTimeout / persistTimeout are therefore
     *  already in milliseconds.  The *_MS constants are what advanceTimeBy
     *  should use — i.e. 1000 × the seconds values. */
    private static final int READ_TIMEOUT_SEC    = 5;
    private static final int PERSIST_TIMEOUT_SEC = 3;
    private static final int READ_TIMEOUT_MS    = READ_TIMEOUT_SEC    * 1000;
    private static final int PERSIST_TIMEOUT_MS = PERSIST_TIMEOUT_SEC * 1000;

    private EmbeddedChannel channel;

    @After
    public void teardown() {
        if (channel != null) {
            try { channel.finishAndReleaseAll(); } catch (Throwable ignored) {}
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Build a channel matching the real production pipeline order:
     * ReadFlowHandler → TimeoutHandler → ExceptionCapture → CapturingHandler.
     *
     * ReadFlowHandler fires user events inbound (fireUserEventTriggered),
     * which travel toward the tail — i.e. toward TimeoutHandler and beyond.
     * TimeoutHandler must therefore be downstream of ReadFlowHandler.
     *
     * ExceptionCapture sits after TimeoutHandler so that fireExceptionCaught
     * is intercepted before it falls off the end of the pipeline unrecorded.
     */
    private CapturingHandler buildChannel() {
        NettyHttpChannelConfig cfg = new NettyConfigBuilder()
                .with(NettyHttpChannelConfig.ConfigElement.HTTP_OPTIONS, httpOpts(READ_TIMEOUT_SEC, PERSIST_TIMEOUT_SEC))
                .build();
        TimeoutHandler timeout = new TimeoutHandler(cfg);
        exceptions = new ExceptionCapture();
        CapturingHandler cap = new CapturingHandler();
        channel = new EmbeddedChannel(ReadFlowHandler.INSTANCE, timeout, exceptions, cap);
        channel.runPendingTasks();
        return cap;
    }

    /** Captured exceptions from TimeoutHandler.fireExceptionCaught. */
    private ExceptionCapture exceptions;

    /** Build the config map expected by NettyHttpChannelConfig.
     *  PROPNAME_READ_TIMEOUT and PROPNAME_PERSIST_TIMEOUT are in SECONDS. */
    private static Map<String, Object> httpOpts(int readSec, int persistSec) {
        Map<String, Object> m = new HashMap<>();
        m.put(HttpConfigConstants.PROPNAME_READ_TIMEOUT,    readSec);
        m.put(HttpConfigConstants.PROPNAME_PERSIST_TIMEOUT, persistSec);
        m.put(HttpConfigConstants.PROPNAME_KEEPALIVE_ENABLED, true);
        return m;
    }

    private static String phase(EmbeddedChannel ch) throws ReflectiveOperationException {
        TimeoutHandler h = ch.pipeline().get(TimeoutHandler.class);
        Field f = TimeoutHandler.class.getDeclaredField("phase");
        f.setAccessible(true);
        return f.get(h).toString();
    }

    private static HttpRequest requestWithBody(String uri, int len) {
        DefaultHttpRequest req = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, uri);
        req.headers().set("Content-Length", len);
        req.headers().set("Connection", "keep-alive");
        return req;
    }

    private static HttpContent bodyChunk(byte... data) {
        return new DefaultHttpContent(Unpooled.copiedBuffer(data));
    }

    private static LastHttpContent lastContent(byte... data) {
        return new DefaultLastHttpContent(Unpooled.copiedBuffer(data));
    }

    private static DefaultFullHttpResponse fullOkNoBody() {
        DefaultFullHttpResponse r = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.EMPTY_BUFFER);
        r.headers().set("Content-Length", "0");
        r.headers().set("Connection", "keep-alive");
        return r;
    }

    private FlowState state() {
        return channel.attr(ReadFlowHandler.FLOW_KEY).get();
    }

    private void writeOut(Object msg) {
        channel.writeOutbound(msg);
        channel.runPendingTasks();
    }

    // -----------------------------------------------------------------------
    // Test 1 — PurgeStartedEvent arms READ phase
    //
    // When the response completes before the request body, ReadFlowHandler
    // fires PurgeStartedEvent. TimeoutHandler must transition to READ.
    // -----------------------------------------------------------------------
    @Test
    public void testPurgeStartedEventArmsReadPhase() throws Exception {
        buildChannel();

        channel.writeInbound(requestWithBody("/a", 3));
        channel.runPendingTasks();

        // Response completes; body not yet received.
        writeOut(fullOkNoBody());

        // onResponseComplete fires PurgeStartedEvent → READ phase.
        assertFalse("requestConsumed still false", state().isRequestConsumed());
        assertEquals("Phase must be READ after PurgeStartedEvent", "READ", phase(channel));
    }

    // -----------------------------------------------------------------------
    // Test 2 — Each body fragment resets the read timer
    //
    // While in READ phase, each arriving HttpContent chunk resets the timer.
    // Advancing by less than readTimeout before each fragment must not fire.
    // -----------------------------------------------------------------------
    @Test
    public void testPurgeFragmentsResetReadTimer() throws Exception {
        buildChannel();

        channel.writeInbound(requestWithBody("/a", 6));
        channel.runPendingTasks();
        writeOut(fullOkNoBody());
        assertEquals("READ phase after purge start", "READ", phase(channel));

        // Advance almost to timeout — no exception yet.
        channel.advanceTimeBy(READ_TIMEOUT_MS - 100, TimeUnit.MILLISECONDS);
        channel.runScheduledPendingTasks();
        assertNull("No timeout before fragment", extractException(channel));

        // First fragment arrives — resets the timer.
        channel.writeInbound(bodyChunk((byte) 1, (byte) 2, (byte) 3));
        channel.runPendingTasks();
        assertEquals("Still READ after fragment", "READ", phase(channel));

        // Advance almost to timeout again — timer was reset, so still no exception.
        channel.advanceTimeBy(READ_TIMEOUT_MS - 100, TimeUnit.MILLISECONDS);
        channel.runScheduledPendingTasks();
        assertNull("No timeout after reset by fragment", extractException(channel));
    }

    // -----------------------------------------------------------------------
    // Test 3 — Read timeout fires if no fragment arrives during purge
    //
    // If the client stops sending the body during purge, the read timeout
    // must fire — not the persist timeout and not silence.
    // -----------------------------------------------------------------------
    @Test
    public void testReadTimeoutFiresDuringStallledPurge() throws Exception {
        buildChannel();

        // Use a second exchange so firstRequest=false when the purge read
        // timeout fires — on the first request TimeoutHandler re-arms once
        // and then closes silently (matching the initial-idle-close behaviour).
        channel.writeInbound(requestWithBody("/a", 2));
        channel.writeInbound(lastContent((byte) 1, (byte) 2));
        channel.runPendingTasks();
        writeOut(fullOkNoBody());
        // Drain first exchange persist window without timing out.
        channel.advanceTimeBy(PERSIST_TIMEOUT_MS - 100, TimeUnit.MILLISECONDS);
        channel.runScheduledPendingTasks();
        assertNull("No timeout mid-persist", exceptions.get());

        // Second exchange — body left unread, purge path.
        channel.writeInbound(requestWithBody("/b", 6));
        channel.runPendingTasks();
        writeOut(fullOkNoBody());
        assertEquals("READ phase during purge on second exchange", "READ", phase(channel));

        // Advance past read timeout — firstRequest=false so ReadTimeoutException fires.
        channel.advanceTimeBy(READ_TIMEOUT_MS + 100, TimeUnit.MILLISECONDS);
        channel.runScheduledPendingTasks();

        Throwable t = exceptions.get();
        assertNotNull("Read timeout must fire during stalled purge", t);
        assertTrue("Must be ReadTimeoutException, not PersistTimeoutException",
                   t instanceof ReadTimeoutException);
    }

    // -----------------------------------------------------------------------
    // Test 4 — RequestConsumedEvent transitions READ → PERSIST
    //
    // When LastHttpContent arrives, READ is cancelled and RequestConsumedEvent
    // transitions the phase to PERSIST.
    // -----------------------------------------------------------------------
    @Test
    public void testRequestConsumedEventTransitionsToPersist() throws Exception {
        buildChannel();

        channel.writeInbound(requestWithBody("/a", 3));
        channel.runPendingTasks();
        writeOut(fullOkNoBody());
        assertEquals("READ phase during purge", "READ", phase(channel));

        // Terminal body fragment — purge complete.
        channel.writeInbound(lastContent((byte) 1, (byte) 2, (byte) 3));
        channel.runPendingTasks();

        assertTrue("requestConsumed after LastHttpContent", state().isRequestConsumed());
        assertEquals("Phase must be PERSIST after purge completes", "PERSIST", phase(channel));
    }

    // -----------------------------------------------------------------------
    // Test 5 — Persist timeout fires after purge if no next request arrives
    //
    // After purge completes and PERSIST is armed, the persist timeout must
    // fire if the client is silent. It must NOT be the read timeout.
    // -----------------------------------------------------------------------
    @Test
    public void testPersistTimeoutFiresAfterPurgeIfClientSilent() throws Exception {
        buildChannel();

        // Complete one exchange normally so firstRequest=false, then purge.
        channel.writeInbound(requestWithBody("/a", 2));
        channel.writeInbound(lastContent((byte) 1, (byte) 2));
        channel.runPendingTasks();
        writeOut(fullOkNoBody());
        channel.advanceTimeBy(PERSIST_TIMEOUT_MS - 100, TimeUnit.MILLISECONDS);
        channel.runScheduledPendingTasks();

        // Second exchange — unread body, purge, then silence.
        channel.writeInbound(requestWithBody("/b", 2));
        channel.runPendingTasks();
        writeOut(fullOkNoBody());
        channel.writeInbound(lastContent((byte) 3, (byte) 4));
        channel.runPendingTasks();
        assertEquals("PERSIST phase after purge", "PERSIST", phase(channel));

        // Advance past persist timeout — PersistTimeoutException must fire.
        channel.advanceTimeBy(PERSIST_TIMEOUT_MS + 100, TimeUnit.MILLISECONDS);
        channel.runScheduledPendingTasks();

        Throwable t = exceptions.get();
        assertNotNull("Persist timeout must fire", t);
        assertTrue("Must be PersistTimeoutException", t instanceof PersistTimeoutException);
    }

    // -----------------------------------------------------------------------
    // Test 6 — Persist timeout does NOT fire within its window
    //
    // If the next request arrives before the persist timeout expires, no
    // exception fires and the phase transitions to OFF/READ as normal.
    // -----------------------------------------------------------------------
    @Test
    public void testPersistTimeoutCancelledByNextRequest() throws Exception {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(requestWithBody("/a", 2));
        channel.runPendingTasks();
        writeOut(fullOkNoBody());
        channel.writeInbound(lastContent((byte) 1, (byte) 2));
        channel.runPendingTasks();
        assertEquals("PERSIST after purge", "PERSIST", phase(channel));

        // Advance partially into persist window — no timeout yet.
        channel.advanceTimeBy(PERSIST_TIMEOUT_MS / 2, TimeUnit.MILLISECONDS);
        channel.runScheduledPendingTasks();
        assertNull("No timeout mid-persist-window", extractException(channel));

        // Next request arrives — cancels PERSIST.
        channel.writeInbound(new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, "/b", Unpooled.EMPTY_BUFFER));
        channel.runPendingTasks();

        assertNull("No timeout after next request", extractException(channel));
        assertEquals("B admitted", 2, cap.requests.size());
        assertTrue("Channel still alive", channel.isActive());
    }

    // -----------------------------------------------------------------------
    // Test 7 — Body fully consumed BEFORE response: PERSIST armed immediately
    //
    // If the body is already consumed when the response terminal write
    // completes, no PurgeStartedEvent fires and PERSIST is armed directly
    // by RequestConsumedEvent from verifyNeedRead.
    // -----------------------------------------------------------------------
    @Test
    public void testBodyConsumedBeforeResponseArmsPersistDirectly() throws Exception {
        buildChannel();

        channel.writeInbound(requestWithBody("/a", 3));
        channel.writeInbound(lastContent((byte) 1, (byte) 2, (byte) 3));
        channel.runPendingTasks();
        assertTrue("Body consumed before response", state().isRequestConsumed());

        writeOut(fullOkNoBody());

        // No purge needed: PERSIST armed directly via RequestConsumedEvent.
        assertEquals("PERSIST phase immediately", "PERSIST", phase(channel));
    }

    // -----------------------------------------------------------------------
    // Test 8 — Multi-fragment purge: read timer resets on each fragment,
    //          PERSIST only armed after all fragments consumed
    //
    // Three fragments arrive across separate read cycles. The read timer
    // must reset on each one, and PERSIST must not be armed until the last.
    // -----------------------------------------------------------------------
    @Test
    public void testMultiFragmentPurgeReadTimerResetsEachTime() throws Exception {
        buildChannel();

        channel.writeInbound(requestWithBody("/a", 9));
        channel.runPendingTasks();
        writeOut(fullOkNoBody());
        assertEquals("READ during purge", "READ", phase(channel));

        // Fragment 1.
        channel.advanceTimeBy(READ_TIMEOUT_MS / 2, TimeUnit.MILLISECONDS);
        channel.runScheduledPendingTasks();
        channel.writeInbound(bodyChunk((byte) 1, (byte) 2, (byte) 3));
        channel.runPendingTasks();
        assertEquals("READ after fragment 1", "READ", phase(channel));
        assertNull("No timeout after fragment 1", extractException(channel));

        // Fragment 2 — advances past original timer but reset means no fire.
        channel.advanceTimeBy(READ_TIMEOUT_MS / 2, TimeUnit.MILLISECONDS);
        channel.runScheduledPendingTasks();
        channel.writeInbound(bodyChunk((byte) 4, (byte) 5, (byte) 6));
        channel.runPendingTasks();
        assertEquals("READ after fragment 2", "READ", phase(channel));
        assertNull("No timeout after fragment 2", extractException(channel));

        // Terminal fragment — purge complete.
        channel.writeInbound(lastContent((byte) 7, (byte) 8, (byte) 9));
        channel.runPendingTasks();
        assertEquals("PERSIST after terminal fragment", "PERSIST", phase(channel));
        assertNull("No timeout at PERSIST start", extractException(channel));
    }

    // -----------------------------------------------------------------------
    // Test 9 — Fully consumed request (no purge): no READ phase, just PERSIST
    //
    // Normal keep-alive: app reads the body, then the response is written.
    // There should be no READ phase for purge; PERSIST arms after response.
    // -----------------------------------------------------------------------
    @Test
    public void testNormalKeepAliveNeverEntersPurgeReadPhase() throws Exception {
        buildChannel();

        channel.writeInbound(requestWithBody("/a", 2));
        channel.writeInbound(lastContent((byte) 5, (byte) 6));
        channel.runPendingTasks();

        writeOut(fullOkNoBody());

        // Must go straight to PERSIST — no READ phase for purge.
        assertEquals("PERSIST directly, no purge READ phase", "PERSIST", phase(channel));
    }

    // -----------------------------------------------------------------------
    // Test 10 — PurgeStartedEvent and RequestConsumedEvent fire exactly once
    //
    // A capturing user-event handler counts the events. Both must fire
    // exactly once per exchange, and not at all on normal fully-consumed flow.
    // -----------------------------------------------------------------------
    @Test
    public void testPurgeEventsFireExactlyOncePerExchange() {
        EventCapture events = new EventCapture();
        NettyHttpChannelConfig cfg = new NettyConfigBuilder()
                .with(NettyHttpChannelConfig.ConfigElement.HTTP_OPTIONS,
                      httpOpts(READ_TIMEOUT_SEC, PERSIST_TIMEOUT_SEC))
                .build();
        // Real order: ReadFlowHandler → TimeoutHandler → EventCapture → CapturingHandler
        channel = new EmbeddedChannel(ReadFlowHandler.INSTANCE, new TimeoutHandler(cfg),
                                      events, new CapturingHandler());
        channel.runPendingTasks();

        channel.writeInbound(requestWithBody("/a", 2));
        channel.runPendingTasks();
        writeOut(fullOkNoBody());
        assertEquals("PurgeStarted fires once", 1, events.purgeStarted);
        assertEquals("RequestConsumed not yet", 0, events.requestConsumed);

        channel.writeInbound(lastContent((byte) 1, (byte) 2));
        channel.runPendingTasks();
        assertEquals("PurgeStarted still once", 1, events.purgeStarted);
        assertEquals("RequestConsumed fires once", 1, events.requestConsumed);
    }

    @Test
    public void testNoEventsOnNormalConsumedRequest() {
        EventCapture events = new EventCapture();
        NettyHttpChannelConfig cfg = new NettyConfigBuilder()
                .with(NettyHttpChannelConfig.ConfigElement.HTTP_OPTIONS,
                      httpOpts(READ_TIMEOUT_SEC, PERSIST_TIMEOUT_SEC))
                .build();
        // Real order: ReadFlowHandler → TimeoutHandler → EventCapture → CapturingHandler
        channel = new EmbeddedChannel(ReadFlowHandler.INSTANCE, new TimeoutHandler(cfg),
                                      events, new CapturingHandler());
        channel.runPendingTasks();

        // Body fully consumed before response — no purge needed.
        channel.writeInbound(requestWithBody("/a", 2));
        channel.writeInbound(lastContent((byte) 1, (byte) 2));
        channel.runPendingTasks();
        writeOut(fullOkNoBody());

        assertEquals("PurgeStarted must NOT fire on normal request", 0, events.purgeStarted);
        assertEquals("RequestConsumed fires once via verifyNeedRead", 1, events.requestConsumed);
    }


    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static Throwable extractException(EmbeddedChannel ch) {
        try {
            ch.checkException();
            return null;
        } catch (Throwable t) {
            return t;
        }
    }

    /** Intercepts fireExceptionCaught before it falls off the pipeline end. */
    private static class ExceptionCapture extends ChannelDuplexHandler {
        private final AtomicReference<Throwable> caught = new AtomicReference<>();

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            caught.compareAndSet(null, cause);
            // Don't propagate — prevents EmbeddedChannel from re-throwing
            // the exception on every subsequent operation.
        }

        Throwable get() { return caught.get(); }
    }

    /** Counts PurgeStartedEvent and RequestConsumedEvent instances. */
    private static class EventCapture extends ChannelDuplexHandler {
        int purgeStarted    = 0;
        int requestConsumed = 0;

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof PurgeStartedEvent)    purgeStarted++;
            if (evt instanceof RequestConsumedEvent) requestConsumed++;
            super.userEventTriggered(ctx, evt);
        }
    }

    /** Records admitted HttpRequests and releases all messages. */
    private static class CapturingHandler extends ChannelDuplexHandler {
        final List<HttpRequest> requests = new ArrayList<>();

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof HttpRequest) requests.add((HttpRequest) msg);
            ReferenceCountUtil.release(msg);
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
                throws Exception {
            super.write(ctx, msg, promise);
        }
    }
}
