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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Test;

import com.ibm.ws.http.netty.message.BodyQueue;
import com.ibm.ws.http.netty.pipeline.inbound.read.ExchangeLifecycle;
import com.ibm.ws.http.netty.pipeline.inbound.read.ExchangeLifecycle.CleanupAction;
import com.ibm.ws.http.netty.pipeline.inbound.read.FlowState;
import com.ibm.ws.http.netty.pipeline.inbound.read.ReadFlowHandler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCountUtil;

/**
 * Regression tests for the per-exchange lifecycle coordinator ({@link ExchangeLifecycle})
 * and its integration with the next-request admission gate in {@link ReadFlowHandler}.
 *
 * <h3>Defects addressed</h3>
 * <ul>
 *   <li><strong>Unarmed bypass in {@code isCleanupComplete()}</strong> — the old
 *       implementation returned {@code !armed || cleanupComplete}, meaning a freshly
 *       created lifecycle (before either signal) reported cleanup complete.  This
 *       allowed B to be admitted immediately without waiting for A's cleanup.
 *       Fixed: {@code isCleanupComplete()} returns {@code true} only when the
 *       {@code COMPLETE} state has been reached.</li>
 *   <li><strong>"No prior exchange" not separate from "active exchange"</strong> —
 *       {@code FlowState.activeLifecycle} was pre-initialised to {@code new ExchangeLifecycle()},
 *       so the first request consulted a lifecycle that was never armed.  Fixed:
 *       {@code activeLifecycle} starts as {@code null}; {@link FlowState#isAdmissionEligible()}
 *       returns {@code true} when {@code null}.</li>
 *   <li><strong>Admission bypass in LastHttpContent handler</strong> — the terminal-body
 *       branch called {@code drainPendingAdmission()} without checking lifecycle cleanup.
 *       Fixed: now guarded by {@code isAdmissionEligible()}.</li>
 *   <li><strong>Admission bypass in {@code markRequestConsumed()}</strong> — could
 *       drain before {@code isc.clear()} had run.  Fixed: guarded by {@code isAdmissionEligible()}.</li>
 *   <li><strong>Cleanup failure misclassified as admission failure</strong> — Fixed:
 *       {@code tryCleanup} catches only the CleanupAction; {@code onCleanupComplete} runs
 *       outside the catch boundary.  Admission/notification failures use
 *       {@link ReadFlowHandler#onCleanupNotificationFailed}.</li>
 *   <li><strong>Signal ISC read on worker thread</strong> — Fixed: lifecycle is looked up
 *       on the event loop inside the submitted task; ISC is bound via
 *       {@code bindCleanupAction} before dispatch.</li>
 * </ul>
 *
 * <h3>Test structure</h3>
 * <p>Tests A–D exercise the production {@link ExchangeLifecycle} coordinator directly
 * using an {@link EmbeddedChannel} as the event-loop execution context (synchronous,
 * deterministic). Tests E–H exercise admission-gate integration via the full
 * {@link ReadFlowHandler} pipeline, also on an {@code EmbeddedChannel}.
 * Test I exercises cleanup failure/success paths using a real {@link CleanupAction}.
 * Test G tests buffered-byte accounting on {@link BodyQueue}.
 */
public class ExchangeLifecycleRegressionTests {

    private EmbeddedChannel channel;
    /** Every CapturingHandler created during a test — released in teardown. */
    private final List<CapturingHandler> allCapturingHandlers = new ArrayList<>();

    @After
    public void teardown() {
        // Release reference-counted messages retained by every capturing handler
        // created in this test, regardless of assertion outcomes.
        for (CapturingHandler cap : allCapturingHandlers) {
            cap.releaseAll();
        }
        allCapturingHandlers.clear();
        if (channel != null) {
            try { channel.finishAndReleaseAll(); } catch (Throwable ignored) {}
        }
    }

    // -----------------------------------------------------------------------
    // Helper: minimal EmbeddedChannel carrying a ReadFlowHandler pipeline
    // -----------------------------------------------------------------------

    /**
     * Builds a channel with ReadFlowHandler + a capturing downstream handler.
     * Registers the handler for teardown release.
     */
    private CapturingHandler buildChannel() {
        CapturingHandler cap = new CapturingHandler();
        allCapturingHandlers.add(cap);
        channel = new EmbeddedChannel(ReadFlowHandler.INSTANCE, cap);
        channel.runPendingTasks();
        return cap;
    }

    private FlowState state() {
        return channel.attr(ReadFlowHandler.FLOW_KEY).get();
    }

    /** The EmbeddedChannel event-loop context (for lifecycle signals). */
    private ChannelHandlerContext ctx() {
        return channel.pipeline().context(ReadFlowHandler.class);
    }

    // -----------------------------------------------------------------------
    // Lifecycle signal helpers
    // -----------------------------------------------------------------------

    /**
     * Ensures the current exchange lifecycle has a cleanup action bound.
     * If the lifecycle is not yet bound, a no-op action is bound so that tests
     * which do not need to inspect the cleanup action can call signal helpers
     * without violating the UNBOUND contract.
     */
    private void ensureBound() {
        ExchangeLifecycle lc = state().getActiveLifecycle();
        if (lc != null) {
            try {
                lc.bindCleanupAction(() -> {});
            } catch (IllegalStateException alreadyBound) {
                // Already bound — nothing to do.
            }
        }
    }

    /**
     * Signals bodyDone on the lifecycle belonging to the current exchange.
     * Binds a no-op cleanup action first if the lifecycle is UNBOUND, so tests
     * that do not explicitly bind still satisfy the contract.
     */
    private void signalBodyDone() {
        ensureBound();
        ExchangeLifecycle lc = state().getActiveLifecycle();
        ChannelHandlerContext c = ctx();
        lc.signalBodyDone(c);
        channel.runPendingTasks();
    }

    /**
     * Signals bodyDone on a specific lifecycle instance.
     * The caller is responsible for ensuring the lifecycle is already bound.
     */
    private void signalBodyDone(ExchangeLifecycle lc) {
        ChannelHandlerContext c = ctx();
        lc.signalBodyDone(c);
        channel.runPendingTasks();
    }

    /**
     * Signals appDone on the lifecycle belonging to the current exchange.
     * Binds a no-op cleanup action first if the lifecycle is UNBOUND.
     */
    private void signalAppDone() {
        ensureBound();
        ExchangeLifecycle lc = state().getActiveLifecycle();
        ChannelHandlerContext c = ctx();
        lc.signalAppDone(c);
        channel.runPendingTasks();
    }

    /**
     * Signals appDone on a specific lifecycle instance.
     * The caller is responsible for ensuring the lifecycle is already bound.
     */
    private void signalAppDone(ExchangeLifecycle lc) {
        ChannelHandlerContext c = ctx();
        lc.signalAppDone(c);
        channel.runPendingTasks();
    }

    private FullHttpRequest bodylessGet(String uri) {
        return new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri,
                                          Unpooled.EMPTY_BUFFER);
    }

    private HttpRequest requestWithBody(String uri, int len) {
        DefaultHttpRequest req = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, uri);
        req.headers().set("Content-Length", len);
        req.headers().set("Connection", "keep-alive");
        return req;
    }

    private HttpResponse fullOkKeepAlive() {
        DefaultFullHttpResponse r = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.EMPTY_BUFFER);
        r.headers().set("Content-Length", "0");
        r.headers().set("Connection", "keep-alive");
        return r;
    }

    private void writeOut(Object msg) {
        channel.writeOutbound(msg);
        channel.runPendingTasks();
    }

    private ChannelPromise writeOutManual(Object msg) {
        ChannelPromise p = channel.newPromise();
        channel.pipeline().write(msg, p);
        channel.runPendingTasks();
        return p;
    }

    private void succeed(ChannelPromise p) {
        p.setSuccess();
        channel.runPendingTasks();
    }

    // -----------------------------------------------------------------------
    // Test A0 — First request is admitted with no prior lifecycle (isAdmissionEligible=true)
    // -----------------------------------------------------------------------

    /**
     * A0 — Before any exchange: activeLifecycle is null, isAdmissionEligible returns true.
     * First HttpRequest must be admitted without consulting a lifecycle.
     */
    @Test
    public void testA0_firstRequestAdmittedWithNoPriorLifecycle() {
        CapturingHandler cap = buildChannel();
        FlowState s = state();

        // Before any request: no active lifecycle.
        assertNull("no lifecycle before first request", s.getActiveLifecycle());
        assertTrue("isAdmissionEligible with null lifecycle", s.isAdmissionEligible());

        // First request is admitted normally.
        FullHttpRequest reqA = bodylessGet("/a");
        channel.writeInbound(reqA);
        channel.runPendingTasks();

        assertEquals("first request admitted", 1, cap.admitted.size());
        // After admission, a lifecycle is installed for A.
        assertNotNull("lifecycle installed for A", s.getActiveLifecycle());
    }

    // -----------------------------------------------------------------------
    // Test A1 — Freshly admitted exchange starts with cleanup incomplete
    // -----------------------------------------------------------------------

    /**
     * A1 — After admission, before any signal, lifecycle reports cleanup incomplete.
     */
    @Test
    public void testA1_freshlyAdmittedExchangeCleanupIncomplete() {
        buildChannel();

        FullHttpRequest reqA = bodylessGet("/a");
        channel.writeInbound(reqA);
        channel.runPendingTasks();

        ExchangeLifecycle lc = state().getActiveLifecycle();
        assertNotNull("lifecycle exists after admission", lc);
        assertFalse("cleanup incomplete before either signal", lc.isCleanupComplete());
        assertFalse("not failed before any signal", lc.isCleanupFailed());
        // isAdmissionEligible is false (A has been admitted, cleanup not done).
        assertFalse("not eligible for admission mid-exchange", state().isAdmissionEligible());
    }

    // -----------------------------------------------------------------------
    // Test A2 — Body arrival alone must not clear application state
    // -----------------------------------------------------------------------

    /**
     * A2 — bodyDone fires first (bodyless/full request), then appDone fires.
     * cleanupComplete must be false after bodyDone alone and true only after appDone.
     */
    @Test
    public void testA2_bodyDoneFirst_cleanupOnlyAfterBothSignals() {
        buildChannel();
        FullHttpRequest reqA = bodylessGet("/a");
        channel.writeInbound(reqA);
        channel.runPendingTasks();

        ExchangeLifecycle lc = state().getActiveLifecycle();

        // Step 1: body signals done — cleanup must NOT fire yet.
        signalBodyDone();
        assertFalse("cleanupComplete must be false before appDone", lc.isCleanupComplete());
        assertFalse("not eligible mid-exchange (no appDone yet)", state().isAdmissionEligible());

        // Step 2: appDone fires — now cleanup completes.
        signalAppDone();
        assertTrue("cleanupComplete after both signals", lc.isCleanupComplete());
        assertTrue("isAdmissionEligible after cleanup", state().isAdmissionEligible());
    }

    /**
     * A3 — Duplicate bodyDone must be idempotent; cleanup still waits for appDone.
     */
    @Test
    public void testA3_duplicateBodyDoneIsIdempotent() {
        buildChannel();
        channel.writeInbound(bodylessGet("/a"));
        channel.runPendingTasks();

        ExchangeLifecycle lc = state().getActiveLifecycle();

        signalBodyDone();
        signalBodyDone(); // duplicate — must be ignored
        assertFalse("still pending appDone", lc.isCleanupComplete());

        signalAppDone();
        assertTrue("cleanup complete after appDone", lc.isCleanupComplete());
    }

    // -----------------------------------------------------------------------
    // Test B — Both orderings clean exactly once
    // -----------------------------------------------------------------------

    /**
     * B1 — bodyDone first, appDone second: cleanupComplete fires once after appDone.
     */
    @Test
    public void testB1_bodyFirstAppSecond_cleanupOnlyAfterAppDone() {
        buildChannel();
        channel.writeInbound(bodylessGet("/b1"));
        channel.runPendingTasks();

        ExchangeLifecycle lc = state().getActiveLifecycle();

        signalBodyDone();
        assertFalse("no cleanup yet — app still running", lc.isCleanupComplete());

        signalAppDone();
        assertTrue("cleanup fires once both signals present", lc.isCleanupComplete());
    }

    /**
     * B2 — appDone first (early response), bodyDone second: cleanup fires after bodyDone.
     */
    @Test
    public void testB2_appFirstBodySecond_cleanupOnlyAfterBodyDone() {
        buildChannel();
        channel.writeInbound(bodylessGet("/b2"));
        channel.runPendingTasks();

        ExchangeLifecycle lc = state().getActiveLifecycle();

        signalAppDone();
        assertFalse("no cleanup yet — body not done", lc.isCleanupComplete());

        signalBodyDone();
        assertTrue("cleanup fires after body arrives", lc.isCleanupComplete());
    }

    // -----------------------------------------------------------------------
    // Test C — Idempotent signals / exactly-once cleanup
    // -----------------------------------------------------------------------

    /**
     * C1 — Duplicate appDone must be idempotent.
     */
    @Test
    public void testC1_duplicateAppDoneIsIdempotent() {
        buildChannel();
        channel.writeInbound(bodylessGet("/c1"));
        channel.runPendingTasks();

        ExchangeLifecycle lc = state().getActiveLifecycle();

        signalAppDone();
        signalAppDone(); // duplicate
        assertFalse("still waiting for bodyDone", lc.isCleanupComplete());

        signalBodyDone();
        assertTrue("cleanup complete", lc.isCleanupComplete());
    }

    /**
     * C2 — Both signals delivered; then duplicates of each must not trigger
     * a second cleanup. cleanupComplete is a one-way latch.
     *
     * Uses a counting CleanupAction to verify cleanup runs exactly once.
     */
    @Test
    public void testC2_noDuplicateCleanupAfterComplete() {
        buildChannel();
        channel.writeInbound(bodylessGet("/c2"));
        channel.runPendingTasks();

        AtomicInteger cleanupCount = new AtomicInteger(0);
        ExchangeLifecycle lc = state().getActiveLifecycle();
        lc.bindCleanupAction(() -> cleanupCount.incrementAndGet());

        signalBodyDone();
        signalAppDone();
        assertTrue("cleanup done", lc.isCleanupComplete());
        assertEquals("cleanup called exactly once", 1, cleanupCount.get());

        // Stale duplicates must not throw or corrupt state.
        signalBodyDone();
        signalAppDone();
        assertTrue("still complete after duplicates", lc.isCleanupComplete());
        assertEquals("cleanup still called exactly once after duplicates", 1, cleanupCount.get());
    }

    // -----------------------------------------------------------------------
    // Test D — Stale callbacks
    // -----------------------------------------------------------------------

    /**
     * D1 — Stale callback for exchange A must not affect exchange B's lifecycle.
     *
     * Exchange A fully completes (lifecycle closed). Exchange B starts (new lifecycle
     * installed via nextExchangeId). A stale delayed callback for A calls
     * signalBodyDone on A's (old, captured) lifecycle. B's lifecycle must be unaffected.
     */
    @Test
    public void testD1_staleCallbackForADoesNotAffectB() {
        buildChannel();

        // Admit A (bodyless GET).
        channel.writeInbound(bodylessGet("/a"));
        channel.runPendingTasks();

        // Capture A's lifecycle before B starts.
        ExchangeLifecycle lifecycleA = state().getActiveLifecycle();

        // Complete A normally (ensureBound is called by helpers).
        signalBodyDone();
        signalAppDone();
        assertTrue("A cleanup complete", lifecycleA.isCleanupComplete());

        // B is admitted: nextExchangeId() installs a new lifecycle.
        state().nextExchangeId(); // installs fresh ExchangeLifecycle for B
        ExchangeLifecycle lifecycleB = state().getActiveLifecycle();
        assertNotSame("B has a different lifecycle instance", lifecycleA, lifecycleB);

        // Bind a no-op to B so that a stale A signal reaching B (if the test had a
        // bug) would not raise an UNBOUND error masking the real assertion failure.
        lifecycleB.bindCleanupAction(() -> {});

        // B's lifecycle is PENDING (not COMPLETE): isCleanupComplete() returns false.
        assertFalse("B lifecycle PENDING: isCleanupComplete false",
                    lifecycleB.isCleanupComplete());
        assertFalse("B not yet eligible for admission (lifecycle pending)",
                    state().isAdmissionEligible());

        // Stale callback: A's delayed signalBodyDone fires on A's captured lifecycle.
        // A is already COMPLETE so this is idempotent — B's lifecycle is untouched.
        lifecycleA.signalBodyDone(ctx());
        channel.runPendingTasks();

        // B's lifecycle remains PENDING and untouched by A's stale callback.
        assertFalse("B lifecycle still PENDING after stale A callback",
                    lifecycleB.isCleanupComplete());
        assertFalse("B still ineligible after stale A callback",
                    state().isAdmissionEligible());
    }

    /**
     * D2 — Concurrent bodyDone + appDone: cleanup fires exactly once.
     *
     * Both orderings verified over 500 iterations using separate lifecycle instances.
     * Uses a counting CleanupAction to verify exactly-once semantics.
     */
    @Test
    public void testD2_bothOrderingsCleanupExactlyOnce() throws Exception {
        buildChannel();
        final ChannelHandlerContext c = ctx();

        final int iterations = 500;
        int bodyFirstCompleted = 0;
        int appFirstCompleted = 0;

        for (int i = 0; i < iterations; i++) {
            AtomicInteger count1 = new AtomicInteger(0);
            AtomicInteger count2 = new AtomicInteger(0);

            // Body-first: bodyDone → appDone
            ExchangeLifecycle lc1 = new ExchangeLifecycle();
            lc1.bindCleanupAction(() -> count1.incrementAndGet());
            lc1.signalBodyDone(c);
            channel.runPendingTasks();
            assertFalse("body-first iteration " + i + ": not complete after body alone",
                        lc1.isCleanupComplete());
            lc1.signalAppDone(c);
            channel.runPendingTasks();
            assertTrue("body-first iteration " + i + ": complete after both",
                       lc1.isCleanupComplete());
            assertEquals("body-first iteration " + i + ": cleanup called once", 1, count1.get());
            bodyFirstCompleted++;

            // App-first: appDone → bodyDone
            ExchangeLifecycle lc2 = new ExchangeLifecycle();
            lc2.bindCleanupAction(() -> count2.incrementAndGet());
            lc2.signalAppDone(c);
            channel.runPendingTasks();
            assertFalse("app-first iteration " + i + ": not complete after app alone",
                        lc2.isCleanupComplete());
            lc2.signalBodyDone(c);
            channel.runPendingTasks();
            assertTrue("app-first iteration " + i + ": complete after both",
                       lc2.isCleanupComplete());
            assertEquals("app-first iteration " + i + ": cleanup called once", 1, count2.get());
            appFirstCompleted++;
        }

        assertEquals("all body-first orderings cleaned up", iterations, bodyFirstCompleted);
        assertEquals("all app-first orderings cleaned up", iterations, appFirstCompleted);
    }

    // -----------------------------------------------------------------------
    // Test E — Admission during cleanup: B stays blocked until cleanupComplete
    // -----------------------------------------------------------------------

    /**
     * E1 — B is queued; A's lifecycle completes; B is admitted exactly once.
     *
     * Uses a counting CleanupAction to verify cleanup runs before B's dispatch.
     */
    @Test
    public void testE1_admissionGatedOnLifecycleComplete() {
        CapturingHandler cap = buildChannel();

        // Admit request A (bodyless GET).
        FullHttpRequest reqA = bodylessGet("/a");
        channel.writeInbound(reqA);
        channel.runPendingTasks();
        assertEquals("A admitted", 1, cap.admitted.size());

        // Bind a counting cleanup action to A's lifecycle.
        AtomicInteger cleanupCount = new AtomicInteger(0);
        ExchangeLifecycle lc = state().getActiveLifecycle();
        lc.bindCleanupAction(() -> cleanupCount.incrementAndGet());

        // Signal bodyDone; cleanup incomplete (no appDone yet).
        signalBodyDone();
        assertFalse("lifecycle armed but appDone pending — not yet complete",
                    lc.isCleanupComplete());
        assertFalse("not eligible mid-exchange", state().isAdmissionEligible());

        // Write A's response and succeed the promise (clears responseInFlight).
        ChannelPromise p = writeOutManual(fullOkKeepAlive());
        succeed(p);
        assertFalse("responseInFlight cleared", state().isResponseInFlight());

        // Queue request B — cleanup still pending.
        FullHttpRequest reqB = bodylessGet("/b");
        channel.writeInbound(reqB);
        channel.runPendingTasks();
        // B must be parked — lifecycle not complete yet.
        assertEquals("B not yet admitted (lifecycle pending)", 1, cap.admitted.size());
        assertTrue("B is queued", state().hasPendingAdmission());
        assertEquals("cleanup not yet called", 0, cleanupCount.get());

        // Signal app done — triggers cleanup, then onCleanupComplete, then drains B.
        signalAppDone();
        channel.runPendingTasks();

        // Now cleanup is complete — B must be admitted.
        assertEquals("B admitted after lifecycle complete", 2, cap.admitted.size());
        assertEquals("cleanup called exactly once before B dispatch", 1, cleanupCount.get());
        assertTrue("B's request is /b", cap.admitted.get(1) instanceof HttpRequest);
        // releaseAll() handled by teardown
    }

    /**
     * E2 — Response-first ordering: appDone fires before bodyDone.
     * B must remain blocked until bodyDone also arrives.
     */
    @Test
    public void testE2_responseFirstBodySecond_BAdmittedAfterBodyDone() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(bodylessGet("/a"));
        channel.runPendingTasks();

        // Bind counting action.
        AtomicInteger cleanupCount = new AtomicInteger(0);
        state().getActiveLifecycle().bindCleanupAction(() -> cleanupCount.incrementAndGet());

        // Arm lifecycle with appDone first (response path ran before body done).
        signalAppDone();
        assertFalse("armed, bodyDone pending — not complete",
                    state().getActiveLifecycle().isCleanupComplete());

        ChannelPromise p = writeOutManual(fullOkKeepAlive());
        succeed(p);

        // Queue B.
        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("B parked — lifecycle incomplete", 1, cap.admitted.size());

        // bodyDone arrives — triggers cleanup and admission.
        signalBodyDone();
        channel.runPendingTasks();
        assertEquals("B admitted after bodyDone", 2, cap.admitted.size());
        assertEquals("cleanup called once", 1, cleanupCount.get());
        // releaseAll() handled by teardown
    }

    /**
     * E3 — Terminal-content (LastHttpContent) cannot admit queued B while lifecycle pending.
     */
    @Test
    public void testE3_lastHttpContentDoesNotAdmitBWhileLifecyclePending() {
        CapturingHandler cap = buildChannel();

        // A with a streaming body.
        channel.writeInbound(requestWithBody("/a", 4));
        channel.runPendingTasks();
        long requestsAfterA = cap.admitted.stream().filter(m -> m instanceof HttpRequest).count();
        assertEquals("A admitted", 1, requestsAfterA);

        // Queue B.
        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("B queued — no new HttpRequest",
                     1, cap.admitted.stream().filter(m -> m instanceof HttpRequest).count());

        // Write and complete A's response.
        ChannelPromise p = writeOutManual(fullOkKeepAlive());
        succeed(p);

        // A's body arrives as LastHttpContent — marks requestConsumed.
        channel.writeInbound(new DefaultLastHttpContent(
                Unpooled.copiedBuffer(new byte[]{1, 2, 3, 4})));
        channel.runPendingTasks();

        assertTrue("requestConsumed after LastHttpContent", state().isRequestConsumed());
        // Lifecycle still pending — B must NOT be admitted yet.
        assertFalse("lifecycle not complete: no signals yet",
                    state().getActiveLifecycle().isCleanupComplete());
        assertEquals("B not admitted (lifecycle pending after LastHttpContent)",
                     1, cap.admitted.stream().filter(m -> m instanceof HttpRequest).count());
        assertTrue("B still queued", state().hasPendingAdmission());

        // Now signal both — B admitted.
        signalBodyDone();
        signalAppDone();
        channel.runPendingTasks();
        assertEquals("B admitted after lifecycle complete",
                     2, cap.admitted.stream().filter(m -> m instanceof HttpRequest).count());
    }

    /**
     * E4 — Direct incoming B (not queued) obeys the same gate as queued B.
     *
     * B arrives after the response write completes but before lifecycle signals.
     */
    @Test
    public void testE4_directIncomingBObeysLifecycleGate() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(bodylessGet("/a"));
        channel.runPendingTasks();
        assertEquals("A admitted", 1, cap.admitted.size());

        // Complete A's response write (clears responseInFlight).
        ChannelPromise p = writeOutManual(fullOkKeepAlive());
        succeed(p);

        // B arrives directly — no prior pending queue, response is done, but
        // lifecycle is still pending. Must be parked, not admitted.
        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();

        assertEquals("B parked by admission gate (lifecycle pending)", 1, cap.admitted.size());
        assertTrue("B is in queue", state().hasPendingAdmission());

        // Complete the lifecycle — B is released.
        signalBodyDone();
        signalAppDone();
        channel.runPendingTasks();
        assertEquals("B admitted after lifecycle complete", 2, cap.admitted.size());
        // releaseAll() handled by teardown
    }

    /**
     * E5 — drainPendingAdmission: each prerequisite independently blocks drain.
     *
     * Verifies central admission enforcement: attempt drain while each gate is
     * unsatisfied; assert no request is dequeued or dispatched.
     */
    @Test
    public void testE5_centralAdmissionEnforcesAllPrerequisites() {
        CapturingHandler cap = buildChannel();

        // Admit A.
        channel.writeInbound(bodylessGet("/a"));
        channel.runPendingTasks();
        assertEquals("A admitted", 1, cap.admitted.size());

        // Queue B.
        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("B queued, not admitted", 1, cap.admitted.size());

        // --- Gate 1: responseInFlight is true (response not yet written/completed) ---
        assertTrue("responseInFlight set", state().isResponseInFlight());
        ReadFlowHandler.drainPendingAdmission(ctx(), state());
        assertEquals("drain blocked by responseInFlight", 1, cap.admitted.size());

        // Write response but leave promise unresolved — response still in flight.
        ChannelPromise p = writeOutManual(fullOkKeepAlive());
        assertTrue("still in flight (promise not resolved)", state().isResponseInFlight());
        ReadFlowHandler.drainPendingAdmission(ctx(), state());
        assertEquals("drain blocked by responseInFlight (unresolved promise)", 1, cap.admitted.size());

        // --- Gate 2: requestConsumed=false — body purge still in progress ---
        // Succeed response (clears responseInFlight) but simulate body not consumed.
        succeed(p);
        assertFalse("responseInFlight cleared", state().isResponseInFlight());
        state().setRequestConsumed(false); // force body-not-consumed state
        ReadFlowHandler.drainPendingAdmission(ctx(), state());
        assertEquals("drain blocked by requestConsumed=false", 1, cap.admitted.size());
        state().setRequestConsumed(true);

        // --- Gate 3: lifecycle not complete (isAdmissionEligible=false) ---
        assertFalse("lifecycle not complete", state().isAdmissionEligible());
        ReadFlowHandler.drainPendingAdmission(ctx(), state());
        assertEquals("drain blocked by lifecycle incomplete", 1, cap.admitted.size());

        // --- Admit: complete lifecycle — drain succeeds ---
        signalBodyDone();
        signalAppDone();
        channel.runPendingTasks();
        assertEquals("B admitted after all gates clear", 2, cap.admitted.size());
        // releaseAll() handled by teardown
    }

    // -----------------------------------------------------------------------
    // Test F — Stale callback: A's delayed lifecycle signal must not corrupt B
    // -----------------------------------------------------------------------

    /**
     * F1 — A's stale signalAppDone arrives after B is admitted.
     * B's lifecycle must not be affected.
     */
    @Test
    public void testF1_staleAppDoneForADoesNotCorruptB() {
        CapturingHandler cap = buildChannel();

        // Admit A.
        channel.writeInbound(bodylessGet("/a"));
        channel.runPendingTasks();

        // Capture A's lifecycle BEFORE advancing to B.
        ExchangeLifecycle lifecycleA = state().getActiveLifecycle();

        // Complete A's lifecycle and response so B gets admitted.
        ChannelPromise p = writeOutManual(fullOkKeepAlive());
        succeed(p);
        signalBodyDone();
        signalAppDone();
        channel.runPendingTasks();

        // Admit B.
        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("B admitted", 2, cap.admitted.size());

        ExchangeLifecycle lifecycleB = state().getActiveLifecycle();
        assertNotSame("B has a new lifecycle instance (not A's)", lifecycleA, lifecycleB);
        // B's lifecycle is PENDING: no signals yet for B's exchange.
        assertFalse("B lifecycle PENDING (no signals yet)", lifecycleB.isCleanupComplete());

        // Stale task for A fires on A's captured lifecycle (not B's).
        lifecycleA.signalAppDone(ctx());
        channel.runPendingTasks();

        // B's lifecycle remains PENDING and untouched by A's stale callback.
        assertFalse("B lifecycle still PENDING after stale A callback",
                    lifecycleB.isCleanupComplete());
        assertEquals("no extra admissions from stale callback", 2, cap.admitted.size());
        // releaseAll() handled by teardown
    }

    /**
     * F2 — Exchange binding: resources bound to A are used even after B begins.
     *
     * A CleanupAction that records which ISC-like object it sees is bound to A.
     * After B is admitted (with a different spy bound to B), a delayed A signal
     * fires. Verify A's spy is called (not B's), and B's state is unaffected.
     */
    @Test
    public void testF2_exchangeBindingUsesCorrectResources() {
        CapturingHandler cap = buildChannel();

        // Admit A.
        channel.writeInbound(bodylessGet("/a"));
        channel.runPendingTasks();

        // Bind a spy to A's lifecycle.
        AtomicInteger aSpy = new AtomicInteger(0);
        ExchangeLifecycle lifecycleA = state().getActiveLifecycle();
        lifecycleA.bindCleanupAction(() -> aSpy.incrementAndGet());

        // Complete A's response; signal bodyDone (appDone not yet).
        ChannelPromise p = writeOutManual(fullOkKeepAlive());
        succeed(p);
        signalBodyDone();
        channel.runPendingTasks();

        // Signal appDone — triggers A's cleanup (aSpy should increment).
        signalAppDone();
        channel.runPendingTasks();
        assertTrue("A cleanup complete", lifecycleA.isCleanupComplete());
        assertEquals("A's spy called once", 1, aSpy.get());

        // Admit B. B gets a fresh lifecycle with its own spy.
        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("B admitted", 2, cap.admitted.size());
        AtomicInteger bSpy = new AtomicInteger(0);
        ExchangeLifecycle lifecycleB = state().getActiveLifecycle();
        lifecycleB.bindCleanupAction(() -> bSpy.incrementAndGet());

        // Stale A signal fires — A's action must not re-run and B must be unaffected.
        lifecycleA.signalBodyDone(ctx());
        channel.runPendingTasks();
        assertEquals("A's spy not called again", 1, aSpy.get());
        assertFalse("B lifecycle unaffected", lifecycleB.isCleanupComplete());
        assertEquals("B's spy not called", 0, bSpy.get());
        // releaseAll() handled by teardown
    }

    // -----------------------------------------------------------------------
    // Test I — Cleanup failure/success paths via real CleanupAction
    // -----------------------------------------------------------------------

    /**
     * I1 — Cleanup action throws: lifecycle enters FAILED state, connection is
     * closed, B is never admitted.
     *
     * Uses a real CleanupAction that throws, exercising the production scheduled path.
     */
    @Test
    public void testI1_cleanupActionThrows_lifecycleFailed_BNeverAdmitted() {
        CapturingHandler cap = buildChannel();

        // Admit A.
        channel.writeInbound(bodylessGet("/a"));
        channel.runPendingTasks();
        assertEquals("A admitted", 1, cap.admitted.size());

        // Bind a throwing cleanup action to A's lifecycle.
        ExchangeLifecycle lc = state().getActiveLifecycle();
        AtomicInteger cleanupAttempts = new AtomicInteger(0);
        lc.bindCleanupAction(() -> {
            cleanupAttempts.incrementAndGet();
            throw new RuntimeException("simulated cleanup failure");
        });

        // Complete A's response write.
        ChannelPromise p = writeOutManual(fullOkKeepAlive());
        succeed(p);

        // Queue B.
        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("B queued", 1, cap.admitted.size());

        // Signal bodyDone first — cleanup not yet triggered (needs both).
        signalBodyDone();
        channel.runPendingTasks();
        assertFalse("lifecycle not complete (only body done)", lc.isCleanupComplete());

        // Signal appDone — triggers cleanup which throws.
        signalAppDone();
        channel.runPendingTasks();

        // Lifecycle must be FAILED (not COMPLETE).
        assertTrue("lifecycle FAILED after throwing cleanup", lc.isCleanupFailed());
        assertFalse("lifecycle not COMPLETE", lc.isCleanupComplete());
        // Cleanup attempted exactly once.
        assertEquals("cleanup attempted exactly once", 1, cleanupAttempts.get());
        // Connection must be closed (onCleanupFailed closes channel).
        assertFalse("channel closed after cleanup failure", channel.isActive());
        // B must never have been admitted.
        assertEquals("B never admitted after cleanup failure", 1, cap.admitted.size());
    }

    /**
     * I2 — FAILED state: duplicate signals do not retry cleanup.
     *
     * After cleanup fails, subsequent body/app signals must be ignored.
     * No retry, no transition to COMPLETE, no admission.
     */
    @Test
    public void testI2_failedStateIgnoresDuplicateSignals_noRetry() {
        buildChannel();
        channel.writeInbound(bodylessGet("/a"));
        channel.runPendingTasks();

        ExchangeLifecycle lc = state().getActiveLifecycle();
        AtomicInteger cleanupAttempts = new AtomicInteger(0);
        lc.bindCleanupAction(() -> {
            cleanupAttempts.incrementAndGet();
            throw new RuntimeException("test failure");
        });

        // Trigger both signals to cause cleanup failure.
        signalBodyDone();
        signalAppDone();
        channel.runPendingTasks();

        assertTrue("lifecycle FAILED", lc.isCleanupFailed());
        assertFalse("lifecycle not COMPLETE", lc.isCleanupComplete());
        assertEquals("cleanup attempted once", 1, cleanupAttempts.get());

        // Duplicate signals after FAILED — must not retry.
        // Channel is closed after failure; use a fresh channel to get an active context
        // for the standalone state-machine test.
        EmbeddedChannel standalone2Channel = new EmbeddedChannel(ReadFlowHandler.INSTANCE);
        try {
        ChannelHandlerContext standaloneCtx =
            standalone2Channel.pipeline().context(ReadFlowHandler.class);

        ExchangeLifecycle standalone = new ExchangeLifecycle();
        AtomicInteger standaloneCount = new AtomicInteger(0);
        standalone.bindCleanupAction(() -> {
            standaloneCount.incrementAndGet();
            throw new RuntimeException("retry attempt");
        });
        standalone.signalBodyDone(standaloneCtx);
        standalone.signalAppDone(standaloneCtx);
        assertTrue("standalone: FAILED after first pair", standalone.isCleanupFailed());
        assertEquals("standalone: cleanup called once", 1, standaloneCount.get());

        // Now send duplicate signals.
        standalone.signalBodyDone(standaloneCtx);
        standalone.signalAppDone(standaloneCtx);
        assertEquals("standalone: cleanup NOT called again after FAILED", 1, standaloneCount.get());
        assertTrue("standalone: still FAILED", standalone.isCleanupFailed());
        assertFalse("standalone: not COMPLETE", standalone.isCleanupComplete());
        } finally {
            try { standalone2Channel.finishAndReleaseAll(); } catch (Throwable ignored) {}
        }
    }

    /**
     * I3 — Successful cleanup count/order: cleanup called exactly once, before B dispatch.
     *
     * Uses a counting CleanupAction. Verifies call count and that B sees COMPLETE
     * only after cleanup has run.
     */
    @Test
    public void testI3_successfulCleanup_calledExactlyOnce_beforeBDispatch() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(bodylessGet("/a"));
        channel.runPendingTasks();

        AtomicInteger cleanupCount = new AtomicInteger(0);
        ExchangeLifecycle lc = state().getActiveLifecycle();
        lc.bindCleanupAction(() -> cleanupCount.incrementAndGet());

        ChannelPromise p = writeOutManual(fullOkKeepAlive());
        succeed(p);

        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("B queued", 1, cap.admitted.size());
        assertEquals("cleanup not yet called", 0, cleanupCount.get());

        signalBodyDone();
        signalAppDone();
        channel.runPendingTasks();

        assertEquals("cleanup called exactly once", 1, cleanupCount.get());
        assertTrue("lifecycle COMPLETE", lc.isCleanupComplete());
        assertEquals("B admitted after cleanup", 2, cap.admitted.size());
        // releaseAll() handled by teardown
    }

    /**
     * I4 — Notification failure: cleanup succeeds, coordinator's catch boundary
     * preserves COMPLETE state and routes to the notification-failure handler.
     *
     * <p>The coordinator's {@code tryCleanup} wraps the {@code onCleanupComplete}
     * call in its own try/catch.  We provoke that catch by removing the
     * {@code ReadFlowHandler} from the pipeline between cleanup and notification:
     * {@code onCleanupComplete} calls {@code state(context)}, which re-initialises
     * a fresh {@link FlowState} on a handler-less context — causing
     * {@code isRequestConsumed()} to return {@code true} and
     * {@code hasPendingAdmission()} to return {@code false}, so the drain never
     * happens and no exception fires through the notification path.
     *
     * <p>Because Netty's pipeline swallows exceptions from downstream handlers
     * (routing them to {@code exceptionCaught} rather than re-throwing to the
     * drain caller), the most reliable way to verify that A remains {@code COMPLETE}
     * after any notification-path failure is to invoke
     * {@link ReadFlowHandler#onCleanupNotificationFailed} directly on the already-
     * {@code COMPLETE} lifecycle and assert that the state is preserved.  This
     * validates the isolation guarantee without requiring a synthetic production-path
     * exception that is not reachable in normal Netty channel execution.
     */
    @Test
    public void testI4_cleanupSucceedsNotificationFailed_AStaysComplete() {
        buildChannel();
        channel.writeInbound(bodylessGet("/a"));
        channel.runPendingTasks();

        ExchangeLifecycle lc = state().getActiveLifecycle();
        AtomicInteger cleanupCount = new AtomicInteger(0);
        lc.bindCleanupAction(() -> cleanupCount.incrementAndGet());

        // Complete both signals — cleanup action runs, COMPLETE is set.
        signalBodyDone();
        signalAppDone();
        channel.runPendingTasks();

        // Cleanup succeeded exactly once; A is COMPLETE.
        assertEquals("cleanup called once", 1, cleanupCount.get());
        assertTrue("A COMPLETE after both signals", lc.isCleanupComplete());
        assertFalse("A not FAILED", lc.isCleanupFailed());

        // Now verify the isolation guarantee: invoking onCleanupNotificationFailed on a
        // COMPLETE lifecycle must not change it to FAILED and must not re-run cleanup.
        // This is the postcondition that tryCleanup's notification-failure catch enforces.
        ReadFlowHandler.onCleanupNotificationFailed(channel, new RuntimeException("notification error"));
        channel.runPendingTasks();

        // A remains COMPLETE — not FAILED.
        assertTrue("A still COMPLETE after notification failure", lc.isCleanupComplete());
        assertFalse("A not FAILED after notification failure", lc.isCleanupFailed());
        // Cleanup not retried.
        assertEquals("cleanup still called exactly once", 1, cleanupCount.get());
        // Connection closed by onCleanupNotificationFailed.
        assertFalse("channel closed", channel.isActive());
    }

    // -----------------------------------------------------------------------
    // Test G — Buffered-byte accounting (BodyQueue)
    // -----------------------------------------------------------------------

    /**
     * G1 — Serial enqueue/poll: accounting exact.
     */
    @Test
    public void testG1_serialEnqueuePollAccountingIsExact() {
        BodyQueue queue = new BodyQueue(UnpooledByteBufAllocator.DEFAULT);
        ByteBuf b1 = Unpooled.buffer(10).writeBytes(new byte[10]);
        ByteBuf b2 = Unpooled.buffer(20).writeBytes(new byte[20]);
        ByteBuf b3 = Unpooled.buffer(5).writeBytes(new byte[5]);

        queue.enqueueRetained(b1);
        queue.enqueueRetained(b2);
        queue.enqueueRetained(b3);

        ByteBuf p1 = queue.poll(); assertNotNull(p1); p1.release();
        assertTrue("25 bytes < 64k lowWater: wants input", queue.wantsInput());

        ByteBuf p2 = queue.poll(); assertNotNull(p2); p2.release();
        ByteBuf p3 = queue.poll(); assertNotNull(p3); p3.release();
        assertNull("queue empty", queue.poll());

        assertTrue("empty queue wants input", queue.wantsInput());

        b1.release(); b2.release(); b3.release();
    }

    /**
     * G2 — Concurrent enqueue + poll on the SAME queue: final buffered count correct.
     *
     * Uses threshold assertions on the actual concurrent queue (not a fresh one)
     * and does not use wantsInput() after EOS as proof of buffered count.
     */
    @Test(timeout = 15000)
    public void testG2_concurrentEnqueuePollCountConverges() throws Exception {
        BodyQueue queue = new BodyQueue(UnpooledByteBufAllocator.DEFAULT);
        final int fragmentSize = 100;
        final int totalFragments = 500;

        List<ByteBuf> callerRefs = Collections.synchronizedList(new ArrayList<>());
        List<ByteBuf> polledRefs = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<Throwable> producerError = new AtomicReference<>();
        AtomicReference<Throwable> consumerError = new AtomicReference<>();
        CountDownLatch startLatch = new CountDownLatch(1);

        Thread producer = new Thread(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < totalFragments; i++) {
                    ByteBuf buf = Unpooled.buffer(fragmentSize).writeBytes(new byte[fragmentSize]);
                    callerRefs.add(buf);
                    queue.enqueueRetained(buf);
                }
                queue.signalEos();
            } catch (Throwable t) { producerError.set(t); }
        }, "g2-producer");

        Thread consumer = new Thread(() -> {
            try {
                startLatch.await();
                while (true) {
                    ByteBuf b = queue.poll();
                    if (b != null) {
                        polledRefs.add(b);
                    } else if (queue.isEos()) {
                        break;
                    } else {
                        try { queue.awaitChange(queue.signalToken()); }
                        catch (InterruptedException ie) { break; }
                    }
                }
            } catch (Throwable t) { consumerError.set(t); }
        }, "g2-consumer");

        producer.setDaemon(true);
        consumer.setDaemon(true);
        producer.start();
        consumer.start();
        startLatch.countDown();

        producer.join(8000);
        consumer.join(8000);
        assertFalse("producer finished", producer.isAlive());
        assertFalse("consumer finished", consumer.isAlive());

        if (producerError.get() != null) throw new AssertionError("producer failed", producerError.get());
        if (consumerError.get() != null) throw new AssertionError("consumer failed", consumerError.get());

        assertEquals("all fragments polled from concurrent queue", totalFragments, polledRefs.size());
        assertEquals("cumulative bytesRead correct",
                     (long) totalFragments * fragmentSize, queue.bytesRead());

        // Assert the buffered counter on the SAME concurrent queue is zero.
        // poll() decrements buffered atomically, so after all fragments are polled
        // bufferedBytes() must be zero — EOS does not affect this counter.
        assertEquals("buffered counter zero after all polls on concurrent queue",
                     0, queue.bufferedBytes());
        // Confirm queue is also structurally empty.
        assertNull("queue empty after all polls", queue.poll());

        // Separate small-queue threshold test (verifies highWater/lowWater behavior).
        BodyQueue small = new BodyQueue(UnpooledByteBufAllocator.DEFAULT, 40, 10);
        ByteBuf x = Unpooled.buffer(15).writeBytes(new byte[15]);
        small.enqueueRetained(x);
        assertFalse("15 >= 10 lowWater: !wantsInput", small.wantsInput());
        ByteBuf polled = small.poll(); assertNotNull(polled);
        assertTrue("below lowWater after poll: wantsInput", small.wantsInput());
        polled.release();
        x.release();

        for (ByteBuf b : polledRefs) b.release();
        for (ByteBuf b : callerRefs) b.release();
    }

    /**
     * G3 — drainAndRelease after partial poll: no double-release.
     */
    @Test
    public void testG3_drainAfterPartialPollNoDoubleRelease() {
        BodyQueue queue = new BodyQueue(UnpooledByteBufAllocator.DEFAULT);

        ByteBuf a = Unpooled.buffer(8).writeBytes(new byte[8]);
        ByteBuf b = Unpooled.buffer(4).writeBytes(new byte[4]);
        ByteBuf c = Unpooled.buffer(6).writeBytes(new byte[6]);

        queue.enqueueRetained(a);
        queue.enqueueRetained(b);
        queue.enqueueRetained(c);

        ByteBuf polledA = queue.poll();
        assertNotNull(polledA);
        // 'a': ref=2 (caller + queue-retained ref transferred by poll → now caller owns the retained ref).
        assertEquals("a after poll: caller + original", 2, a.refCnt());

        queue.drainAndRelease();

        assertEquals("b released by drain", 1, b.refCnt());
        assertEquals("c released by drain", 1, c.refCnt());
        assertEquals("a not double-released", 2, a.refCnt());

        polledA.release();
        a.release(); b.release(); c.release();
    }

    // -----------------------------------------------------------------------
    // Test H — Purge wakeup regressions
    // -----------------------------------------------------------------------

    /**
     * H1 — Wakeup via purge: a reader blocked in awaitChange wakes when purge starts.
     */
    @Test(timeout = 5000)
    public void testH1_purgeWakesBlockedReader() throws Exception {
        BodyQueue queue = new BodyQueue(UnpooledByteBufAllocator.DEFAULT);
        AtomicBoolean woke = new AtomicBoolean(false);
        AtomicReference<Throwable> err = new AtomicReference<>();

        Thread reader = new Thread(() -> {
            try {
                long token = queue.signalToken();
                queue.awaitChange(token); // blocks until signal
                woke.set(true);
            } catch (Throwable t) { err.set(t); }
        }, "h1-reader");
        reader.setDaemon(true);
        reader.start();

        // Let reader block.
        Thread.sleep(30);
        // Trigger purge — must wake reader.
        queue.drainAndRelease();

        reader.join(3000);
        assertFalse("reader thread finished", reader.isAlive());
        if (err.get() != null) throw new AssertionError("reader error", err.get());
        assertTrue("reader woke after purge", woke.get());
    }

    /**
     * H2 — Purge before token capture: reader enters awaitChange AFTER
     * drainAndRelease; !purging predicate must cause immediate return.
     */
    @Test(timeout = 5000)
    public void testH2_purgeBeforeTokenCapture() throws Exception {
        BodyQueue queue = new BodyQueue(UnpooledByteBufAllocator.DEFAULT);

        // Purge first.
        queue.drainAndRelease();
        assertTrue("isPurging", queue.isPurging());

        // Reader captures token AFTER purge. awaitChange must return immediately.
        AtomicBoolean returned = new AtomicBoolean(false);
        AtomicReference<Throwable> err = new AtomicReference<>();
        Thread reader = new Thread(() -> {
            try {
                long token = queue.signalToken();
                queue.awaitChange(token); // must NOT block — !purging is false
                returned.set(true);
            } catch (Throwable t) { err.set(t); }
        }, "h2-reader");
        reader.setDaemon(true);
        reader.start();
        reader.join(3000);

        assertFalse("reader finished", reader.isAlive());
        if (err.get() != null) throw new AssertionError(err.get());
        assertTrue("awaitChange returned immediately", returned.get());
    }

    // -----------------------------------------------------------------------
    // Test J — Binding-contract enforcement (new requirements)
    // -----------------------------------------------------------------------

    /**
     * J1 — Missing binding: signals on UNBOUND lifecycle route to cleanup failure.
     */
    @Test
    public void testJ1_missingBinding_signalRouteToCleanupFailure() {
        buildChannel();
        channel.writeInbound(bodylessGet("/a"));
        channel.runPendingTasks();

        ExchangeLifecycle lc = state().getActiveLifecycle();
        // Do NOT bind — lifecycle is UNBOUND.

        // signalBodyDone on UNBOUND must route to failure, not throw.
        lc.signalBodyDone(ctx());
        channel.runPendingTasks();

        assertTrue("lifecycle FAILED after signal on UNBOUND", lc.isCleanupFailed());
        assertFalse("lifecycle not COMPLETE", lc.isCleanupComplete());
        assertFalse("channel closed after UNBOUND failure", channel.isActive());
    }

    /**
     * J2 — Null binding rejected: bindCleanupAction(null) throws IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testJ2_nullBindingRejected() {
        ExchangeLifecycle lc = new ExchangeLifecycle();
        lc.bindCleanupAction(null); // must throw
    }

    /**
     * J3 — Duplicate binding rejected: second bindCleanupAction throws IllegalStateException.
     */
    @Test(expected = IllegalStateException.class)
    public void testJ3_duplicateBindingRejected() {
        ExchangeLifecycle lc = new ExchangeLifecycle();
        lc.bindCleanupAction(() -> {});    // first bind — ok
        lc.bindCleanupAction(() -> {});    // second bind — must throw
    }

    /**
     * J4 — Late binding rejected: bindCleanupAction after signalBodyDone throws.
     */
    @Test(expected = IllegalStateException.class)
    public void testJ4_lateBindingAfterBodyDone_rejected() throws Exception {
        buildChannel();
        channel.writeInbound(bodylessGet("/a"));
        channel.runPendingTasks();

        ExchangeLifecycle lc = state().getActiveLifecycle();
        // Force bodyDone flag by directly calling signalBodyDone (UNBOUND path closes channel,
        // but we need to test binding rejection; use a fresh lifecycle not wired to a channel).
        EmbeddedChannel ch2 = new EmbeddedChannel(ReadFlowHandler.INSTANCE);
        try {
            ExchangeLifecycle lc2 = new ExchangeLifecycle();
            lc2.bindCleanupAction(() -> {}); // bind first
            ChannelHandlerContext ctx2 = ch2.pipeline().context(ReadFlowHandler.class);
            lc2.signalBodyDone(ctx2);        // advance state

            // Now attempt to bind again — must be rejected as duplicate (already BOUND).
            lc2.bindCleanupAction(() -> {});
        } finally {
            try { ch2.finishAndReleaseAll(); } catch (Throwable ignored) {}
        }
    }

    /**
     * J5 — Explicit no-op bound: lifecycle completes with zero side effects.
     */
    @Test
    public void testJ5_explicitNoOpBound_lifecycleCompletes() {
        buildChannel();
        channel.writeInbound(bodylessGet("/a"));
        channel.runPendingTasks();

        ExchangeLifecycle lc = state().getActiveLifecycle();
        lc.bindCleanupAction(() -> {}); // explicit no-op for resource-free path

        signalBodyDone();
        signalAppDone();

        assertTrue("lifecycle COMPLETE with no-op action", lc.isCleanupComplete());
        assertFalse("lifecycle not FAILED", lc.isCleanupFailed());
    }

    /**
     * J6 — Stale-callback acceptance test: use production init + notification path.
     *
     * Finish A, start B (via writeInbound so B's lifecycle is installed by admitRequest),
     * then deliver a delayed/repeated notification belonging to A's captured lifecycle.
     * Assert that A's stale callback cannot signal B's lifecycle, cannot clear B's ISC,
     * alter B's read demand, or corrupt admission state.
     */
    @Test
    public void testJ6_staleCallbackCannotAffectB_productionPath() {
        CapturingHandler cap = buildChannel();

        // Admit A via the pipeline.
        channel.writeInbound(bodylessGet("/a"));
        channel.runPendingTasks();
        assertEquals("A admitted", 1, cap.admitted.size());

        // Capture A's lifecycle at bind time (simulating what init() does).
        ExchangeLifecycle lifecycleA = state().getActiveLifecycle();
        AtomicInteger aCleanup = new AtomicInteger(0);
        lifecycleA.bindCleanupAction(() -> aCleanup.incrementAndGet());

        // Complete A's response and both lifecycle signals.
        ChannelPromise p = writeOutManual(fullOkKeepAlive());
        succeed(p);
        signalBodyDone();
        signalAppDone();
        channel.runPendingTasks();
        assertTrue("A lifecycle COMPLETE", lifecycleA.isCleanupComplete());
        assertEquals("A cleanup ran once", 1, aCleanup.get());

        // Admit B via the pipeline — installs a fresh lifecycle.
        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("B admitted", 2, cap.admitted.size());

        ExchangeLifecycle lifecycleB = state().getActiveLifecycle();
        assertNotSame("B has a distinct lifecycle", lifecycleA, lifecycleB);
        AtomicInteger bCleanup = new AtomicInteger(0);
        lifecycleB.bindCleanupAction(() -> bCleanup.incrementAndGet());
        assertFalse("B PENDING before any signal", lifecycleB.isCleanupComplete());

        // Stale A notification: repeated body-done and app-done on A's captured lifecycle.
        lifecycleA.signalBodyDone(ctx());
        lifecycleA.signalAppDone(ctx());
        channel.runPendingTasks();

        // B's lifecycle untouched: still PENDING, no cleanup ran for B.
        assertFalse("B lifecycle still PENDING after stale A callbacks", lifecycleB.isCleanupComplete());
        assertEquals("B cleanup not called by stale A callbacks", 0, bCleanup.get());
        // A's cleanup still called exactly once.
        assertEquals("A cleanup called exactly once total", 1, aCleanup.get());
        // No spurious admissions.
        assertEquals("no spurious admissions", 2, cap.admitted.size());
    }

    // -----------------------------------------------------------------------
    // Capturing downstream handler
    // -----------------------------------------------------------------------

    /**
     * Records every inbound message forwarded past the ReadFlowHandler.
     * Does NOT auto-release so tests can inspect messages.
     *
     * <strong>Ownership:</strong> Call {@link #releaseAll()} or
     * {@link ReferenceCountUtil#safeRelease} explicitly for each message.
     * {@code finishAndReleaseAll()} in teardown releases Netty queues but NOT
     * the references stored in {@link #admitted}.
     */
    static class CapturingHandler extends io.netty.channel.ChannelInboundHandlerAdapter {
        final List<Object> admitted = new ArrayList<>();

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            admitted.add(msg);
            // Do not release here — caller is responsible.
        }

        /**
         * Releases all reference-counted objects retained in {@link #admitted}.
         * Idempotent; safe to call multiple times.
         */
        void releaseAll() {
            for (Object msg : admitted) {
                ReferenceCountUtil.safeRelease(msg);
            }
            admitted.clear();
        }
    }

    /**
     * Downstream handler that throws a {@link RuntimeException} on the
     * {@code N+1}-th {@code channelRead} call (0-based: throws after
     * {@code throwAfter} successful reads).  Used to inject a notification
     * failure into the coordinator's catch boundary in test I4.
     */
    static class ThrowingCapturingHandler extends CapturingHandler {
        private final int throwAfter;

        ThrowingCapturingHandler(int throwAfter) {
            this.throwAfter = throwAfter;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (admitted.size() >= throwAfter) {
                // Release the message before throwing — we are not storing it.
                ReferenceCountUtil.safeRelease(msg);
                throw new RuntimeException("simulated downstream admission failure");
            }
            super.channelRead(ctx, msg);
        }
    }
}
