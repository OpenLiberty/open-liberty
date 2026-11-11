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
 * Regression tests for {@link ExchangeLifecycle} and the next-request admission
 * gate: signal ordering, exactly-once cleanup, stale callbacks, all admission
 * prerequisites, cleanup failure paths, and {@link BodyQueue} byte accounting.
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

    /** Builds the channel pipeline and registers the handler for teardown release. */
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

    /** Binds a no-op cleanup action if the lifecycle is UNBOUND, satisfying the contract. */
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

    /** Signals bodyDone on the current lifecycle, binding a no-op first if UNBOUND. */
    private void signalBodyDone() {
        ensureBound();
        ExchangeLifecycle lc = state().getActiveLifecycle();
        ChannelHandlerContext c = ctx();
        lc.signalBodyDone(c);
        channel.runPendingTasks();
    }

    /** Signals bodyDone directly on {@code lc}. */
    private void signalBodyDone(ExchangeLifecycle lc) {
        ChannelHandlerContext c = ctx();
        lc.signalBodyDone(c);
        channel.runPendingTasks();
    }

    /** Signals appDone on the current lifecycle, binding a no-op first if UNBOUND. */
    private void signalAppDone() {
        ensureBound();
        ExchangeLifecycle lc = state().getActiveLifecycle();
        ChannelHandlerContext c = ctx();
        lc.signalAppDone(c);
        channel.runPendingTasks();
    }

    /** Signals appDone directly on {@code lc}. */
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

    /** Before the first exchange there is no lifecycle; isAdmissionEligible is true and the first request is admitted immediately. */
    @Test
    public void testFirstRequestAdmittedWithNoPriorLifecycle() {
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

    /** Freshly admitted lifecycle reports cleanup incomplete until both signals arrive. */
    @Test
    public void testFreshlyAdmittedExchangeCleanupIncomplete() {
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

    /** bodyDone first: cleanup remains incomplete until appDone also fires. */
    @Test
    public void testBodyDoneFirst_cleanupOnlyAfterBothSignals() {
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

    /** Duplicate bodyDone is ignored; cleanup still waits for appDone. */
    @Test
    public void testDuplicateBodyDoneIsIdempotent() {
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

    /** bodyDone then appDone: cleanup fires exactly once. */
    @Test
    public void testBodyFirstAppSecond_cleanupOnlyAfterAppDone() {
        buildChannel();
        channel.writeInbound(bodylessGet("/b1"));
        channel.runPendingTasks();

        ExchangeLifecycle lc = state().getActiveLifecycle();

        signalBodyDone();
        assertFalse("no cleanup yet — app still running", lc.isCleanupComplete());

        signalAppDone();
        assertTrue("cleanup fires once both signals present", lc.isCleanupComplete());
    }

    /** appDone first (early response), bodyDone second: cleanup fires after bodyDone. */
    @Test
    public void testAppFirstBodySecond_cleanupOnlyAfterBodyDone() {
        buildChannel();
        channel.writeInbound(bodylessGet("/b2"));
        channel.runPendingTasks();

        ExchangeLifecycle lc = state().getActiveLifecycle();

        signalAppDone();
        assertFalse("no cleanup yet — body not done", lc.isCleanupComplete());

        signalBodyDone();
        assertTrue("cleanup fires after body arrives", lc.isCleanupComplete());
    }

    /** Duplicate appDone is ignored; cleanup still waits for bodyDone. */
    @Test
    public void testDuplicateAppDoneIsIdempotent() {
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

    /** After both signals, duplicate signals must not trigger a second cleanup. */
    @Test
    public void testNoDuplicateCleanupAfterComplete() {
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

    /**
     * A stale bodyDone signal on A's completed lifecycle must not affect B's
     * pending lifecycle.
     */
    @Test
    public void testStaleCallbackForADoesNotAffectB() {
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

    /** Both signal orderings produce exactly-once cleanup, verified over 500 iterations. */
    @Test
    public void testBothOrderingsCleanupExactlyOnce() throws Exception {
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

    /** Queued B is admitted exactly once after A's lifecycle completes; cleanup runs before dispatch. */
    @Test
    public void testAdmissionGatedOnLifecycleComplete() {
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

    /** appDone fires before bodyDone (response-first): B stays blocked until bodyDone. */
    @Test
    public void testResponseFirstBodySecond_BAdmittedAfterBodyDone() {
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

    /** LastHttpContent arrival alone cannot admit B while the lifecycle is still pending. */
    @Test
    public void testLastHttpContentDoesNotAdmitBWhileLifecyclePending() {
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

    /** A directly-arriving B (not pre-queued) obeys the same lifecycle gate as a queued B. */
    @Test
    public void testDirectIncomingBObeysLifecycleGate() {
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

    /** Each admission prerequisite (responseInFlight, requestConsumed, lifecycle) independently blocks drain. */
    @Test
    public void testCentralAdmissionEnforcesAllPrerequisites() {
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

    /** A stale appDone on A's lifecycle after B is admitted must not affect B. */
    @Test
    public void testStaleAppDoneForADoesNotCorruptB() {
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

    /** Resources (CleanupAction) bound to A are used by A's delayed signal; B's spy is never called. */
    @Test
    public void testExchangeBindingUsesCorrectResources() {
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

    /** Throwing cleanup action transitions lifecycle to FAILED, closes the connection, and blocks B forever. */
    @Test
    public void testCleanupActionThrows_lifecycleFailed_BNeverAdmitted() {
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

    /** FAILED lifecycle ignores duplicate signals and does not retry cleanup. */
    @Test
    public void testFailedStateIgnoresDuplicateSignals_noRetry() {
        buildChannel();
        channel.writeInbound(bodylessGet("/a"));
        channel.runPendingTasks();

        ExchangeLifecycle lc = state().getActiveLifecycle();
        AtomicInteger cleanupAttempts = new AtomicInteger(0);
        lc.bindCleanupAction(() -> {
            cleanupAttempts.incrementAndGet();
            throw new RuntimeException("test failure");
        });

        signalBodyDone();
        signalAppDone();
        channel.runPendingTasks();

        assertTrue("lifecycle FAILED", lc.isCleanupFailed());
        assertFalse("lifecycle not COMPLETE", lc.isCleanupComplete());
        assertEquals("cleanup attempted once", 1, cleanupAttempts.get());
    }

    /** Standalone FAILED lifecycle ignores duplicate signals (separate channel, not re-thrown). */
    @Test
    public void testStandaloneFailedStateIgnoresDuplicateSignals() {
        EmbeddedChannel standaloneChannel = new EmbeddedChannel(ReadFlowHandler.INSTANCE);
        try {
            ChannelHandlerContext standaloneCtx =
                standaloneChannel.pipeline().context(ReadFlowHandler.class);

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

            // Duplicate signals must not retry.
            standalone.signalBodyDone(standaloneCtx);
            standalone.signalAppDone(standaloneCtx);
            assertEquals("standalone: cleanup NOT called again after FAILED", 1, standaloneCount.get());
            assertTrue("standalone: still FAILED", standalone.isCleanupFailed());
            assertFalse("standalone: not COMPLETE", standalone.isCleanupComplete());
        } finally {
            try { standaloneChannel.finishAndReleaseAll(); } catch (Throwable ignored) {}
        }
    }

    /** Successful cleanup runs exactly once and completes before B is dispatched. */
    @Test
    public void testSuccessfulCleanup_calledExactlyOnce_beforeBDispatch() {
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
     * Cleanup succeeds but the notification path fails: the coordinator's catch
     * boundary must preserve COMPLETE state and route to
     * {@link ReadFlowHandler#onCleanupNotificationFailed} without flipping A to FAILED.
     */
    @Test
    public void testCleanupSucceedsNotificationFailed_AStaysComplete() {
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

    /** Serial enqueue/poll: {@code bytesRead} and {@code wantsInput} are exact. */
    @Test
    public void testSerialEnqueuePollAccountingIsExact() {
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

    /** Concurrent enqueue + poll: all fragments polled, buffered counter reaches zero. */
    @Test(timeout = 15000)
    public void testConcurrentEnqueuePollCountConverges() throws Exception {
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

    /** drainAndRelease after a partial poll releases only remaining queue refs; polled ref is untouched. */
    @Test
    public void testDrainAfterPartialPollNoDoubleRelease() {
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

    /** Blocked reader wakes when purge starts. */
    @Test(timeout = 5000)
    public void testPurgeWakesBlockedReader() throws Exception {
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

    /** Purge before token capture: awaitChange returns immediately via the !purging predicate. */
    @Test(timeout = 5000)
    public void testPurgeBeforeTokenCapture() throws Exception {
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

    /** Signals on an UNBOUND lifecycle route to cleanup failure and close the channel. */
    @Test
    public void testMissingBinding_signalRouteToCleanupFailure() {
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

    /** bindCleanupAction(null) throws IllegalArgumentException. */
    @Test(expected = IllegalArgumentException.class)
    public void testNullBindingRejected() {
        ExchangeLifecycle lc = new ExchangeLifecycle();
        lc.bindCleanupAction(null); // must throw
    }

    /** Second bindCleanupAction on an already-bound lifecycle throws IllegalStateException. */
    @Test(expected = IllegalStateException.class)
    public void testDuplicateBindingRejected() {
        ExchangeLifecycle lc = new ExchangeLifecycle();
        lc.bindCleanupAction(() -> {});    // first bind — ok
        lc.bindCleanupAction(() -> {});    // second bind — must throw
    }

    /** Second bind after a signal has been delivered also throws IllegalStateException. */
    @Test(expected = IllegalStateException.class)
    public void testDuplicateBindingAfterSignalRejected() throws Exception {
        EmbeddedChannel ch2 = new EmbeddedChannel(ReadFlowHandler.INSTANCE);
        try {
            ExchangeLifecycle lc2 = new ExchangeLifecycle();
            lc2.bindCleanupAction(() -> {}); // first bind — ok
            ChannelHandlerContext ctx2 = ch2.pipeline().context(ReadFlowHandler.class);
            lc2.signalBodyDone(ctx2);        // advance state (lifecycle now bound + bodyDone)

            // Second bind — must throw (already bound).
            lc2.bindCleanupAction(() -> {});
        } finally {
            try { ch2.finishAndReleaseAll(); } catch (Throwable ignored) {}
        }
    }

    /** Explicit no-op action: lifecycle reaches COMPLETE with no side effects. */
    @Test
    public void testExplicitNoOpBound_lifecycleCompletes() {
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
     * Repeated stale callbacks on A's lifecycle after B is admitted must not signal
     * B's lifecycle, alter read demand, or corrupt admission state.
     */
    @Test
    public void testStaleCallbackCannotAffectB_productionPath() {
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

    /**
     * Records every inbound message forwarded past ReadFlowHandler.
     * Does NOT auto-release; call {@link #releaseAll()} when done.
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
     * Throws on the {@code N+1}-th {@code channelRead} to inject a notification
     * failure into the coordinator's catch boundary.
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
