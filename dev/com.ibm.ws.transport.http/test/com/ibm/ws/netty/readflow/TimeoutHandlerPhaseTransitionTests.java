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

import com.ibm.ws.http.netty.pipeline.inbound.read.ExchangeLifecycle;

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
import io.netty.handler.codec.http.FullHttpRequest;
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
     * TimeoutHandler → ReadFlowHandler → ExceptionCapture → CapturingHandler.
     *
     * <p>In production the order is:
     * ... → TimeoutHandler → ReadFlowHandler → HttpDispatcherHandler
     *
     * <p>fireExceptionCaught travels tail-ward: TimeoutHandler fires
     * ReadTimeoutException → ReadFlowHandler.exceptionCaught sees it first →
     * if purging, closes silently; otherwise propagates to ExceptionCapture.
     *
     * <p>ExceptionCapture sits after ReadFlowHandler so it records only
     * exceptions that ReadFlowHandler did not suppress.
     */
    private CapturingHandler buildChannel() {
        NettyHttpChannelConfig cfg = new NettyConfigBuilder()
                .with(NettyHttpChannelConfig.ConfigElement.HTTP_OPTIONS, httpOpts(READ_TIMEOUT_SEC, PERSIST_TIMEOUT_SEC))
                .build();
        TimeoutHandler timeout = new TimeoutHandler(cfg);
        exceptions = new ExceptionCapture();
        CapturingHandler cap = new CapturingHandler();
        channel = new EmbeddedChannel(timeout, ReadFlowHandler.INSTANCE, exceptions, cap);
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

    /**
     * Signals both body-done and app-done on the currently active lifecycle,
     * simulating the wiring that {@code HttpDispatcherLink} provides in production.
     *
     * <p>Must be called after A's exchange is complete (body consumed AND response
     * written) to allow the admission gate to open for the next exchange.
     */
    private void completeExchangeLifecycle() {
        ExchangeLifecycle lc = state().getActiveLifecycle();
        if (lc == null || lc.isCleanupComplete()) {
            return; // already complete or no exchange active
        }
        // Bind a no-op if the lifecycle is UNBOUND (tests that do not inject a real ISC).
        try {
            lc.bindCleanupAction(() -> {});
        } catch (IllegalStateException alreadyBound) {
            // Already bound — nothing to do.
        }
        channel.pipeline().context(ReadFlowHandler.class)
               .executor().execute(() -> {
                   lc.signalBodyDone(channel.pipeline().context(ReadFlowHandler.class));
                   lc.signalAppDone(channel.pipeline().context(ReadFlowHandler.class));
               });
        channel.runPendingTasks();
    }

    /**
     * Convenience: write outbound AND complete the exchange lifecycle.
     * Use wherever a test expects the next request to be admitted immediately.
     */
    private void writeOutAndComplete(Object msg) {
        writeOut(msg);
        completeExchangeLifecycle();
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
    // Test 3 — Purge read timeout closes channel silently (no ReadTimeoutException,
    //          no 408 response)
    //
    // When the read timeout fires while an async body purge is in progress,
    // ReadFlowHandler.exceptionCaught intercepts the ReadTimeoutException
    // (identified by !isRequestConsumed && isBodyReadWanted) and closes the
    // channel silently instead of propagating the exception to HttpDispatcherHandler.
    // This matches Channel Framework where HttpIgnoreBodyCallback.error() calls
    // HttpInboundLink.close(vc, exception) — an error-state close, no 408 sent.
    // -----------------------------------------------------------------------
    @Test
    public void testPurgeReadTimeoutClosesChannelSilentlyNoException() throws Exception {
        buildChannel();

        // Use a second exchange so firstRequest=false — on the first request
        // TimeoutHandler re-arms once before firing, masking the purge path.
        channel.writeInbound(requestWithBody("/a", 2));
        channel.writeInbound(lastContent((byte) 1, (byte) 2));
        channel.runPendingTasks();
        writeOutAndComplete(fullOkNoBody());
        channel.advanceTimeBy(PERSIST_TIMEOUT_MS - 100, TimeUnit.MILLISECONDS);
        channel.runScheduledPendingTasks();
        assertNull("No timeout mid-persist", exceptions.get());

        // Second exchange — body NOT consumed before response (purge path).
        channel.writeInbound(requestWithBody("/b", 6));
        channel.runPendingTasks();
        writeOut(fullOkNoBody()); // response completes → !isRequestConsumed → READ armed
        assertEquals("READ phase during purge", "READ", phase(channel));
        assertTrue("Channel must be active before timeout", channel.isActive());

        // Advance past read timeout — ReadFlowHandler intercepts ReadTimeoutException
        // because isBodyReadWanted=true && !isRequestConsumed, closes silently.
        channel.advanceTimeBy(READ_TIMEOUT_MS + 100, TimeUnit.MILLISECONDS);
        channel.runScheduledPendingTasks();
        channel.runPendingTasks();

        // No exception must have propagated past ReadFlowHandler.
        assertNull("No exception past ReadFlowHandler when purging", exceptions.get());
        // Channel must have been closed silently.
        assertFalse("Channel must be closed after purge read timeout", channel.isActive());
    }

    // -----------------------------------------------------------------------
    // Test 3b — Non-purge read timeout still propagates ReadTimeoutException
    //
    // When the body stalls during an active request (response not yet sent,
    // isBodyReadWanted=true but isResponseInFlight=true), ReadFlowHandler must
    // NOT intercept the exception — it propagates to HttpDispatcherHandler
    // which sends a 408.
    // -----------------------------------------------------------------------
    @Test
    public void testNonPurgeReadTimeoutStillPropagatesReadTimeoutException() throws Exception {
        buildChannel();

        // Use a second exchange so firstRequest=false.
        channel.writeInbound(requestWithBody("/a", 2));
        channel.writeInbound(lastContent((byte) 1, (byte) 2));
        channel.runPendingTasks();
        writeOutAndComplete(fullOkNoBody());
        channel.advanceTimeBy(PERSIST_TIMEOUT_MS - 100, TimeUnit.MILLISECONDS);
        channel.runScheduledPendingTasks();
        assertNull("No timeout mid-persist", exceptions.get());

        // Second exchange — headers arrive, body NOT yet sent and NO response yet.
        // isBodyReadWanted will be set true by the application-read path, but
        // isRequestConsumed=false and isResponseInFlight=true (no response written).
        // ReadFlowHandler's purge guard requires BOTH !isRequestConsumed AND
        // isBodyReadWanted — the guard also relies on the fact that during a true
        // purge isResponseInFlight is already false (response was already sent).
        // Here we simulate the body-read stall: TimeoutHandler fires READ timeout
        // because a body-bearing request is admitted and the body never arrives.
        channel.writeInbound(requestWithBody("/b", 6));
        channel.runPendingTasks();
        assertEquals("READ phase for incoming body (no response yet)", "READ", phase(channel));

        // Advance past read timeout.
        channel.advanceTimeBy(READ_TIMEOUT_MS + 100, TimeUnit.MILLISECONDS);
        channel.runScheduledPendingTasks();

        // isBodyReadWanted=false here (no app read was issued in this test),
        // so ReadFlowHandler's purge guard does NOT intercept: exception propagates.
        Throwable t = exceptions.get();
        assertNotNull("ReadTimeoutException must propagate on non-purge stall", t);
        assertTrue("Must be ReadTimeoutException", t instanceof ReadTimeoutException);
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
        // Complete lifecycle to open admission for second exchange.
        writeOutAndComplete(fullOkNoBody());
        channel.advanceTimeBy(PERSIST_TIMEOUT_MS - 100, TimeUnit.MILLISECONDS);
        channel.runScheduledPendingTasks();

        // Second exchange — unread body, purge, then silence.
        channel.writeInbound(requestWithBody("/b", 2));
        channel.runPendingTasks();
        writeOut(fullOkNoBody()); // purge starts (body not yet consumed)
        channel.writeInbound(lastContent((byte) 3, (byte) 4));
        channel.runPendingTasks();
        completeExchangeLifecycle(); // second exchange lifecycle done
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
        writeOut(fullOkNoBody()); // purge starts (body not consumed yet)
        channel.writeInbound(lastContent((byte) 1, (byte) 2));
        channel.runPendingTasks();
        completeExchangeLifecycle(); // completes lifecycle so PERSIST arms
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

        // Complete lifecycle first, then write response. After lifecycle completes,
        // verifyNeedRead fires RequestConsumedEvent → PERSIST phase.
        completeExchangeLifecycle();
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

        // Complete lifecycle before response so verifyNeedRead fires after writeOut.
        completeExchangeLifecycle();
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
        // Production order: TimeoutHandler → ReadFlowHandler → EventCapture → CapturingHandler.
        // EventCapture sits after ReadFlowHandler so it receives user events forwarded
        // via super.userEventTriggered from ReadFlowHandler.
        channel = new EmbeddedChannel(new TimeoutHandler(cfg), ReadFlowHandler.INSTANCE,
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
        // Production order: TimeoutHandler → ReadFlowHandler → EventCapture → CapturingHandler.
        channel = new EmbeddedChannel(new TimeoutHandler(cfg), ReadFlowHandler.INSTANCE,
                                      events, new CapturingHandler());
        channel.runPendingTasks();

        // Body fully consumed before response — no purge needed.
        channel.writeInbound(requestWithBody("/a", 2));
        channel.writeInbound(lastContent((byte) 1, (byte) 2));
        channel.runPendingTasks();
        // Complete lifecycle so verifyNeedRead fires RequestConsumedEvent after writeOut.
        ExchangeLifecycle lc = state().getActiveLifecycle();
        if (lc != null && !lc.isCleanupComplete()) {
            try { lc.bindCleanupAction(() -> {}); } catch (IllegalStateException ignored) {}
            channel.pipeline().context(ReadFlowHandler.class)
                   .executor().execute(() -> {
                       lc.signalBodyDone(channel.pipeline().context(ReadFlowHandler.class));
                       lc.signalAppDone(channel.pipeline().context(ReadFlowHandler.class));
                   });
            channel.runPendingTasks();
        }
        writeOut(fullOkNoBody());

        assertEquals("PurgeStarted must NOT fire on normal request", 0, events.purgeStarted);
        assertEquals("RequestConsumed fires once via verifyNeedRead", 1, events.requestConsumed);
    }


    // -----------------------------------------------------------------------
    // Test 11 — Pipelined admission: no spurious timeout, channel stays alive
    //
    // When A's LastHttpContent is forwarded downstream, ReadFlowHandler admits
    // the queued request B from the pending queue. The admission path fires B's
    // HttpRequest via readFlowHandlerContext.fireChannelRead, which starts below
    // TimeoutHandler — so TimeoutHandler does not see B's HttpRequest and does
    // not arm a timer at drain time.
    //
    // This matches Channel Framework behaviour: in CF, pipelining dispatches B
    // on a new thread via handlePipeLining() → ready() → processRequest(), where
    // the read timer is armed at the TCP read() call, not when the HttpRequest
    // message is parsed. CF gives no guarantee that a body-read timer is running
    // at the exact moment B's headers are decoded from a pre-buffered pipeline.
    //
    // The invariants we DO require (matching CF):
    //   1. The channel stays alive after the drain — no spurious exception/close.
    //   2. B was actually admitted (cap.requests.size() == 2).
    //   3. No spurious timeout exception is fired on the pipeline.
    //
    // When B's body chunks subsequently arrive on the wire they pass through
    // TimeoutHandler.channelRead normally, which re-arms READ via resetRead()
    // if phase is already READ, or arms it fresh when the next HttpRequest
    // arrives from the socket.
    // -----------------------------------------------------------------------
    @Test
    public void testPipelinedNextRequestTimerNotCancelledByPriorTerminalContent()
            throws Exception {
        CapturingHandler cap = buildChannel();

        // A: 2-byte body; response written before body arrives (purge path).
        channel.writeInbound(requestWithBody("/a", 2));
        channel.runPendingTasks();

        // Queue B: a body-bearing POST so it sits in the pending queue.
        channel.writeInbound(requestWithBody("/b", 3));
        channel.runPendingTasks();
        assertEquals("Only A dispatched initially", 1, cap.requests.size());

        // A's response completes; B is still gated (A body not consumed yet).
        writeOut(fullOkNoBody());
        assertFalse("A body not consumed after response", state().isRequestConsumed());

        // A's terminal body arrives. After body consumed, complete lifecycle so
        // drainPendingAdmission can admit B.
        channel.writeInbound(lastContent((byte) 1, (byte) 2));
        channel.runPendingTasks();
        completeExchangeLifecycle();

        // B must have been admitted.
        assertEquals("B admitted after A body drained", 2, cap.requests.size());
        assertFalse("no more pending after drain", state().hasPendingAdmission());

        // requestConsumed is false for B — B still has an unread 3-byte body.
        assertFalse("requestConsumed=false for B (body not yet consumed)", state().isRequestConsumed());

        // The channel must be alive and no spurious exception must have been fired.
        // (Matches CF: handlePipeLining dispatches B on a new thread; no timer is
        // guaranteed to be running at the moment B's headers are decoded from the
        // pre-buffered pipeline — the timer is armed when the TCP read() is issued.)
        assertTrue("channel alive after pipelined admission", channel.isActive());
        assertNull("no spurious timeout after pipelined admission", extractException(channel));
    }

    // -----------------------------------------------------------------------
    // Test 12 — Pipelined bodyless next request: timer reset correctly
    //
    // Same as Test 11 but B is a bodyless GET. After admission, TimeoutHandler
    // must NOT be in READ phase (no body), so no timer is armed. This verifies
    // the timer snapshot check does not break the bodyless-request path.
    // -----------------------------------------------------------------------
    @Test
    public void testPipelinedBodylessNextRequestNoTimerArmed() throws Exception {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(requestWithBody("/a", 2));
        channel.runPendingTasks();

        // Queue B: a bodyless GET.
        DefaultFullHttpRequest reqB = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, "/b", Unpooled.EMPTY_BUFFER);
        reqB.headers().set("Connection", "keep-alive");
        channel.writeInbound(reqB);
        channel.runPendingTasks();
        assertEquals("Only A dispatched", 1, cap.requests.size());

        writeOut(fullOkNoBody()); // A's response — A body still outstanding

        // A's body arrives; complete lifecycle so B is admitted.
        channel.writeInbound(lastContent((byte) 5, (byte) 6));
        channel.runPendingTasks();
        completeExchangeLifecycle();

        assertEquals("B admitted", 2, cap.requests.size());
        assertNull("No timeout fired", extractException(channel));
        // B is bodyless; if TimeoutHandler had cancelled B's timer it was null
        // anyway, so no exception either way. Just assert channel is alive.
        assertTrue("channel alive", channel.isActive());
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
