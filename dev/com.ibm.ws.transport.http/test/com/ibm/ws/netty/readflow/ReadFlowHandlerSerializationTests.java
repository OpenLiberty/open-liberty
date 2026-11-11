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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Test;

import com.ibm.ws.http.netty.pipeline.inbound.read.ExchangeLifecycle;
import com.ibm.ws.http.netty.pipeline.inbound.read.FlowState;
import com.ibm.ws.http.netty.pipeline.inbound.read.ReadFlowHandler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
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
 * Unit tests for HTTP/1.1 exchange serialisation in {@link ReadFlowHandler}.
 * Uses {@link EmbeddedChannel} for deterministic, synchronous event-loop execution.
 * Pipeline: ReadFlowHandler → CapturingHandler.
 */
public class ReadFlowHandlerSerializationTests {

    private EmbeddedChannel channel;

    @After
    public void teardown() {
        if (channel != null) {
            try { channel.finishAndReleaseAll(); } catch (Throwable ignored) {}
        }
    }


    private CapturingHandler buildChannel() {
        CapturingHandler cap = new CapturingHandler();
        channel = new EmbeddedChannel(ReadFlowHandler.INSTANCE, cap);
        channel.runPendingTasks();
        return cap;
    }

    /** Bodyless GET (no Content-Length, not chunked). refCnt=1 on return. */
    private static FullHttpRequest bodylessGet(String uri) {
        return new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri,
                                          Unpooled.EMPTY_BUFFER);
    }

    /** POST request with declared Content-Length body (headers only, not full). */
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

    /** 200 OK, Content-Length:0. {@link DefaultFullHttpResponse} — completes on its own write. */
    private static HttpResponse fullOkNoBody() {
        DefaultFullHttpResponse r = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.EMPTY_BUFFER);
        r.headers().set("Content-Length", "0");
        r.headers().set("Connection", "keep-alive");
        return r;
    }

    /** 200 OK, chunked. Completes only when the separate {@link LastHttpContent} write succeeds. */
    private static HttpResponse streamingOk() {
        DefaultHttpResponse r = new DefaultHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        r.headers().set("Transfer-Encoding", "chunked");
        r.headers().set("Connection", "keep-alive");
        return r;
    }

    /** 200 OK, Content-Length set but NOT a FullHttpResponse; completes only on terminal write. */
    private static HttpResponse contentLengthOk(int len) {
        DefaultHttpResponse r = new DefaultHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        r.headers().set("Content-Length", len);
        r.headers().set("Connection", "keep-alive");
        return r;
    }

    /** 204 No Content — no body, not chunked. */
    private static HttpResponse noContentResponse() {
        DefaultFullHttpResponse r = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT, Unpooled.EMPTY_BUFFER);
        r.headers().set("Connection", "keep-alive");
        return r;
    }

    /** 100 Continue informational. */
    private static HttpResponse continueResponse() {
        DefaultFullHttpResponse r = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE, Unpooled.EMPTY_BUFFER);
        r.headers().set("Content-Length", "0");
        return r;
    }

    private FlowState state() {
        return channel.attr(ReadFlowHandler.FLOW_KEY).get();
    }

    /** Write outbound and flush all pending tasks. */
    private void writeOut(Object msg) {
        channel.writeOutbound(msg);
        channel.runPendingTasks();
    }

    /** Write outbound with a manually-controlled promise for independent success/failure. */
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

    private void fail(ChannelPromise p) {
        p.setFailure(new IOException("simulated write failure"));
        channel.runPendingTasks();
    }

    /**
     * Signals bodyDone and appDone on the active lifecycle so the admission gate
     * opens for the next exchange.
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
            // Already bound by a prior call — nothing to do.
        }
        ChannelHandlerContext ctx = channel.pipeline().context(ReadFlowHandler.class);
        lc.signalBodyDone(ctx);
        lc.signalAppDone(ctx);
        channel.runPendingTasks();
    }

    /** Writes the response outbound and completes the exchange lifecycle. */
    private void writeOutAndComplete(Object msg) {
        writeOut(msg);
        completeExchangeLifecycle();
    }

    @Test
    public void testRequestBGatedUntilResponseATerminalWriteSucceeds() {
        CapturingHandler cap = buildChannel();

        FullHttpRequest reqA = bodylessGet("/a");
        channel.writeInbound(reqA);
        channel.runPendingTasks();

        assertEquals("A dispatched immediately", 1, cap.requests.size());
        assertTrue("responseInFlight after A admitted", state().isResponseInFlight());

        // Write A's streaming response headers then a body chunk.
        writeOut(streamingOk());
        writeOut(bodyChunk((byte) 1));

        // B arrives while A's exchange is open.
        FullHttpRequest reqB = bodylessGet("/b");
        int refBefore = reqB.refCnt();   // 1 before writeInbound
        channel.writeInbound(reqB);
        channel.runPendingTasks();

        assertEquals("B must NOT be dispatched while A is open", 1, cap.requests.size());
        assertTrue("B must be in the pending queue", state().hasPendingAdmission());
        // No extra retain: refCnt stays at its initial value because the pipeline
        // delivered the sole reference directly to ReadFlowHandler's queue.
        assertEquals("refCnt unchanged — no spurious retain", refBefore, reqB.refCnt());

        // Complete A's response and lifecycle.
        writeOutAndComplete(new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER));

        assertEquals("B dispatched after A's terminal write succeeds", 2, cap.requests.size());
        assertEquals("/b", cap.requests.get(1).uri());
        assertFalse("pending queue empty after drain", state().hasPendingAdmission());
    }

    @Test
    public void testZeroLengthOrdinaryResponseWaitsForTerminalWrite() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(bodylessGet("/a"));
        channel.runPendingTasks();

        // Queue B.
        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("B gated", 1, cap.requests.size());

        // Write a non-full Content-Length:0 response header (NOT a FullHttpResponse).
        // This must NOT complete the exchange — the terminal LastHttpContent is separate.
        ChannelPromise headerPromise = writeOutManual(contentLengthOk(0));
        succeed(headerPromise);

        // Header write succeeded, but B must still be gated.
        assertEquals("B must NOT be admitted after header-only write", 1, cap.requests.size());
        assertTrue("responseInFlight still true", state().isResponseInFlight());

        // Now write the terminal LastHttpContent and complete the lifecycle.
        writeOutAndComplete(new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER));

        assertEquals("B admitted after terminal write", 2, cap.requests.size());
    }

    @Test
    public void testFullHttpResponseCompletesOnOwnWrite() {
        CapturingHandler cap = buildChannel();

        // A with Expect:100-continue so we can send a 100 before the real response.
        DefaultHttpRequest reqA = new DefaultHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.POST, "/a");
        reqA.headers().set("Content-Length", "3");
        reqA.headers().set("Expect", "100-continue");
        channel.writeInbound(reqA);
        channel.runPendingTasks();
        assertEquals("A dispatched", 1, cap.requests.size());

        // Queue B.
        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("B gated", 1, cap.requests.size());

        // 100 Continue must NOT release the exchange.
        writeOut(continueResponse());
        assertTrue("responseInFlight must remain true after 100-Continue",
                   state().isResponseInFlight());
        assertEquals("B still gated after 100-Continue", 1, cap.requests.size());

        // Send A's body then complete with a FullHttpResponse (self-contained).
        channel.writeInbound(lastContent((byte) 1, (byte) 2, (byte) 3));
        channel.runPendingTasks();

        // FullHttpResponse — contains body, implements LastHttpContent.
        DefaultFullHttpResponse fullResp = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                Unpooled.copiedBuffer(new byte[]{7, 8}));
        fullResp.headers().set("Content-Length", "2");
        fullResp.headers().set("Connection", "keep-alive");
        writeOut(fullResp);
        completeExchangeLifecycle();

        assertEquals("B admitted after FullHttpResponse write", 2, cap.requests.size());
    }

    @Test
    public void test204CompletesOnHeaderWrite() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(bodylessGet("/a"));
        channel.runPendingTasks();

        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("B gated", 1, cap.requests.size());

        writeOut(noContentResponse());  // 204 — FullHttpResponse, no body
        completeExchangeLifecycle();

        assertEquals("B admitted after 204", 2, cap.requests.size());
    }

    @Test
    public void testEarlierHeaderWriteFailurePreventsConnectionReuse() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(bodylessGet("/a"));
        channel.runPendingTasks();

        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("B gated", 1, cap.requests.size());

        // Header write fails → exchange poisoned → channel closed immediately.
        ChannelPromise headerPromise = writeOutManual(streamingOk());
        fail(headerPromise);

        assertFalse("channel closed after header write failure", channel.isActive());
        assertEquals("B never dispatched", 1, cap.requests.size());
    }

    @Test
    public void testBodyChunkWriteFailurePoisonsExchange() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(bodylessGet("/a"));
        channel.runPendingTasks();

        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();

        // Headers succeed.
        succeed(writeOutManual(streamingOk()));
        assertEquals("B still gated", 1, cap.requests.size());
        assertTrue("channel alive after successful header", channel.isActive());

        // A body chunk fails — poisons the exchange.
        ChannelPromise chunkPromise = writeOutManual(bodyChunk((byte) 1));
        fail(chunkPromise);

        assertFalse("channel closed after body-chunk write failure", channel.isActive());
        assertEquals("B never dispatched", 1, cap.requests.size());
    }

    @Test
    public void testBodyChunkFailureThenTerminalSuccessDoesNotReuseConnection() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(bodylessGet("/a"));
        channel.runPendingTasks();

        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();

        succeed(writeOutManual(streamingOk()));

        // Body chunk fails, poisoning the exchange immediately.
        fail(writeOutManual(bodyChunk((byte) 1)));

        // Channel is already closed; assert B was never dispatched.
        assertFalse("channel closed by poison", channel.isActive());
        assertEquals("B never dispatched regardless of later terminal", 1, cap.requests.size());
    }

    @Test
    public void testTerminalWriteFailurePreventsConnectionReuse() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(bodylessGet("/a"));
        channel.runPendingTasks();

        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();

        // Header and a body chunk both succeed; only the terminal write fails.
        succeed(writeOutManual(streamingOk()));
        succeed(writeOutManual(bodyChunk((byte) 1)));
        assertEquals("B still gated", 1, cap.requests.size());
        assertTrue("channel alive after successful header+chunk", channel.isActive());

        ChannelPromise terminalPromise = writeOutManual(new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER));
        fail(terminalPromise);

        assertFalse("channel closed after terminal write failure", channel.isActive());
        assertEquals("B never dispatched", 1, cap.requests.size());
    }


    /**
     * Early error response with unread body: B is gated until the request body
     * is fully drained, not just until A's terminal write.
     */
    @Test
    public void testEarlyErrorResponseGatesBUntilBodyDrained() {
        CapturingHandler cap = buildChannel();

        // A arrives with a 3-byte body that the application will not read.
        channel.writeInbound(requestWithBody("/a", 3));
        channel.runPendingTasks();
        assertEquals("A dispatched", 1, cap.requests.size());
        assertFalse("A body not consumed yet", state().isRequestConsumed());

        // Queue B while A's body is still unconsumed.
        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("B gated — A body outstanding", 1, cap.requests.size());

        // A sends a chunked 400 error response (streaming, non-full) — no body read.
        writeOut(streamingOk()); // response headers (non-full)
        writeOut(new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER)); // terminal write

        // After A's terminal write: B must still be gated because requestConsumed=false.
        assertEquals("B still gated — body not yet drained", 1, cap.requests.size());
        assertFalse("requestConsumed still false after response terminal write",
                    state().isRequestConsumed());

        // Async purge: body bytes arrive (simulates ReadFlowHandler driving reads
        // after onResponseComplete calls setBodyReadWanted(true)).
        channel.writeInbound(lastContent((byte) 1, (byte) 2, (byte) 3));
        channel.runPendingTasks();
        // Lifecycle signals fire after body completes (simulating dispatcher/app cleanup).
        completeExchangeLifecycle();

        // Body now consumed and lifecycle complete; B admitted.
        assertTrue("requestConsumed after LastHttpContent", state().isRequestConsumed());
        assertEquals("B admitted after body drained", 2, cap.requests.size());
    }

    /**
     * Body arrives in multiple fragments; response terminal write happens
     * between the first and last fragment. B must wait for the final fragment.
     */
    @Test
    public void testBodySplitAcrossFragmentsResponseInMiddle() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(requestWithBody("/a", 6));
        channel.runPendingTasks();

        // First fragment arrives; body not complete.
        channel.writeInbound(bodyChunk((byte) 1, (byte) 2, (byte) 3));
        channel.runPendingTasks();
        assertFalse("partial body not consumed", state().isRequestConsumed());

        // Queue B.
        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("B gated", 1, cap.requests.size());

        // Response terminal write fires before the last body fragment.
        writeOut(fullOkNoBody());
        assertEquals("B still gated — last fragment not arrived", 1, cap.requests.size());

        // Last body fragment completes the purge.
        channel.writeInbound(lastContent((byte) 4, (byte) 5, (byte) 6));
        channel.runPendingTasks();
        completeExchangeLifecycle();

        assertTrue("requestConsumed after final fragment", state().isRequestConsumed());
        assertEquals("B admitted after last fragment", 2, cap.requests.size());
    }

    /** Body fully received before the response: B is admitted immediately on the terminal write. */
    @Test
    public void testBodyFullyReceivedBeforeResponseTerminalWriteAdmitsBImmediately() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(requestWithBody("/a", 4));
        channel.runPendingTasks();

        // Full body arrives before response.
        channel.writeInbound(bodyChunk((byte) 10, (byte) 20));
        channel.writeInbound(lastContent((byte) 30, (byte) 40));
        channel.runPendingTasks();
        assertTrue("requestConsumed after full body", state().isRequestConsumed());

        // Queue B.
        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("B gated (response still in flight)", 1, cap.requests.size());

        // Terminal response write — body already consumed, B admitted immediately.
        writeOutAndComplete(fullOkNoBody());
        assertEquals("B admitted immediately — body was already drained", 2, cap.requests.size());
    }

    /**
     * Explicit Connection:close on response: no purge needed, channel must
     * close after the terminal write. B is never admitted.
     */
    @Test
    public void testExplicitConnectionCloseSkipsPurgeAndClosesChannel() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(requestWithBody("/a", 3));
        channel.runPendingTasks();

        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("B gated", 1, cap.requests.size());

        // Explicit Connection: close in the response.
        DefaultFullHttpResponse closeResp = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST, Unpooled.EMPTY_BUFFER);
        closeResp.headers().set("Content-Length", "0");
        closeResp.headers().set("Connection", "close");
        writeOut(closeResp);

        // keepAliveAllowed becomes false → channel closes, B never admitted.
        assertFalse("channel closed after Connection:close response", channel.isActive());
        assertEquals("B never admitted", 1, cap.requests.size());
    }

    /** B arrives in the purge window (response done, body outstanding): the !requestConsumed gate parks B. */
    @Test
    public void testBArrivingDuringPurgeWindowIsGated() {
        CapturingHandler cap = buildChannel();

        // A with unread body.
        channel.writeInbound(requestWithBody("/a", 3));
        channel.runPendingTasks();
        assertEquals("A dispatched", 1, cap.requests.size());

        // A's response completes before the body is drained.
        // At this point no B in queue yet, requestConsumed=false.
        writeOut(fullOkNoBody());
        assertFalse("requestConsumed still false after response", state().isRequestConsumed());

        // B arrives AFTER response complete but BEFORE A's body drain finishes.
        // The gate must use !requestConsumed to park B.
        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("B gated during purge window", 1, cap.requests.size());

        // A's body drain completes.
        channel.writeInbound(lastContent((byte) 1, (byte) 2, (byte) 3));
        channel.runPendingTasks();
        completeExchangeLifecycle();

        assertTrue("requestConsumed after body", state().isRequestConsumed());
        assertEquals("B admitted after purge", 2, cap.requests.size());
    }

    /**
     * Three pipelined requests where A has an unread body and B/C are queued.
     * B must not be dispatched until A's body is drained; C must not be dispatched
     * until B's response is complete.
     */
    @Test
    public void testPipelinedRequestsAfterBodyDrain() {
        CapturingHandler cap = buildChannel();

        // A with unread body.
        channel.writeInbound(requestWithBody("/a", 2));
        channel.runPendingTasks();
        assertEquals("A dispatched", 1, cap.requests.size());

        // B and C queued while A is active.
        channel.writeInbound(bodylessGet("/b"));
        channel.writeInbound(bodylessGet("/c"));
        channel.runPendingTasks();
        assertEquals("Only A dispatched", 1, cap.requests.size());

        // A's response terminal write; body still outstanding.
        writeOut(fullOkNoBody());
        assertEquals("B still gated — A body not drained", 1, cap.requests.size());

        // A's body arrives.
        channel.writeInbound(lastContent((byte) 5, (byte) 6));
        channel.runPendingTasks();
        completeExchangeLifecycle(); // complete A's lifecycle

        assertEquals("B dispatched after A body drained", 2, cap.requests.size());

        // B's response.
        writeOutAndComplete(fullOkNoBody());
        assertEquals("C dispatched after B response", 3, cap.requests.size());

        // Verify ordering.
        assertEquals("/a", cap.requests.get(0).uri());
        assertEquals("/b", cap.requests.get(1).uri());
        assertEquals("/c", cap.requests.get(2).uri());
    }

    /**
     * Fully-consumed normal request: keep-alive reuse works as expected.
     */
    @Test
    public void testNormalKeepAliveRequestUnaffectedByPurgeChanges() {
        CapturingHandler cap = buildChannel();

        // A with fully-read body.
        channel.writeInbound(requestWithBody("/a", 3));
        channel.writeInbound(lastContent((byte) 1, (byte) 2, (byte) 3));
        channel.runPendingTasks();
        assertTrue("A requestConsumed", state().isRequestConsumed());

        writeOutAndComplete(fullOkNoBody());
        assertTrue("channel alive", channel.isActive());

        // B on the same connection.
        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("B dispatched immediately", 2, cap.requests.size());

        writeOut(fullOkNoBody());  // B's response — no need to complete lifecycle for this test
        assertTrue("channel still alive after B", channel.isActive());
    }

    @Test
    public void testQueuedContentReleasedOnChannelClose() {
        buildChannel();

        channel.writeInbound(bodylessGet("/a"));  // A admitted
        channel.runPendingTasks();

        // B and a body chunk for B are queued while A is open.
        HttpRequest reqB = requestWithBody("/b", 4);
        channel.writeInbound(reqB);
        HttpContent chunk = bodyChunk((byte) 10, (byte) 20);
        channel.writeInbound(chunk);
        LastHttpContent last = lastContent((byte) 30, (byte) 40);
        channel.writeInbound(last);
        channel.runPendingTasks();

        assertTrue("pending queue non-empty", state().hasPendingAdmission());

        // Close without completing A's response.
        channel.close();
        channel.runPendingTasks();

        assertFalse("queue released on close", state().hasPendingAdmission());
        // Objects had refCnt=1 when enqueued; releaseQueue should have decremented
        // them to 0.  The EmbeddedChannel's finishAndReleaseAll handles residuals.
    }

    @Test
    public void testQueuedContentReleasedAfterSuccessfulDrain() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(bodylessGet("/a"));
        channel.runPendingTasks();

        // Use a real (non-singleton) empty buffer so refCnt is independently
        // trackable.  Unpooled.EMPTY_BUFFER is an EmptyByteBuf whose refCnt()
        // always returns 1 and whose release() is a no-op, making a post-release
        // refCnt==0 assertion impossible.
        FullHttpRequest reqB = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, "/b", Unpooled.buffer(0));
        channel.writeInbound(reqB);
        channel.runPendingTasks();
        assertTrue("B queued", state().hasPendingAdmission());

        // Complete A — B should be drained and its ref transferred to downstream.
        writeOutAndComplete(fullOkNoBody());

        assertEquals("B dispatched", 2, cap.requests.size());
        assertFalse("queue empty", state().hasPendingAdmission());
        // refCnt: created=1, no extra retain (excess-retain fix), downstream
        // released it to 0 via CapturingHandler.channelRead → ReferenceCountUtil.release().
        assertEquals("B's ref released by downstream", 0, reqB.refCnt());
    }

    @Test
    public void testFragmentedBodyContinuesStreamingDuringActiveExchange() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(requestWithBody("/a", 6));
        channel.runPendingTasks();
        assertEquals("A dispatched", 1, cap.requests.size());
        assertFalse("requestConsumed false for body request", state().isRequestConsumed());

        channel.writeInbound(bodyChunk((byte) 1, (byte) 2, (byte) 3));
        channel.runPendingTasks();
        assertEquals("first chunk forwarded", 1, cap.contents.size());

        channel.writeInbound(lastContent((byte) 4, (byte) 5, (byte) 6));
        channel.runPendingTasks();
        assertEquals("terminal content forwarded", 2, cap.contents.size());
        assertTrue("requestConsumed after terminal content", state().isRequestConsumed());

        writeOut(fullOkNoBody());
        assertTrue("channel still active", channel.isActive());
    }

    @Test
    public void testPendingReadDemandSatisfiedByQueueDrain() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(bodylessGet("/a"));
        channel.runPendingTasks();

        // B queued while A active.
        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("Only A dispatched", 1, cap.requests.size());
        assertTrue("B pending", state().hasPendingAdmission());

        // Complete A — B must be drained from queue, not from a new socket read.
        writeOutAndComplete(fullOkNoBody());
        assertEquals("B dispatched from queue", 2, cap.requests.size());
        assertFalse("queue empty", state().hasPendingAdmission());

        // Complete B (no pending C; just close check).
        writeOut(fullOkNoBody());
        assertTrue("channel alive", channel.isActive());
    }

    @Test
    public void testMultipleRequestsFromSameReadRemainSerialized() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(bodylessGet("/a"));
        channel.writeInbound(bodylessGet("/b"));
        channel.writeInbound(bodylessGet("/c"));
        channel.runPendingTasks();

        assertEquals("Only A dispatched initially", 1, cap.requests.size());

        writeOutAndComplete(fullOkNoBody());
        assertEquals("B dispatched after A", 2, cap.requests.size());

        writeOutAndComplete(fullOkNoBody());
        assertEquals("C dispatched after B", 3, cap.requests.size());

        writeOut(fullOkNoBody()); // C's response — no further B to admit
        assertEquals("/a", cap.requests.get(0).uri());
        assertEquals("/b", cap.requests.get(1).uri());
        assertEquals("/c", cap.requests.get(2).uri());
    }

    @Test
    public void testQueuedBodyChunksReplayedWithRequest() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(bodylessGet("/a"));
        channel.runPendingTasks();

        // B's headers + body arrive while A is active.
        channel.writeInbound(requestWithBody("/b", 4));
        channel.writeInbound(bodyChunk((byte) 10, (byte) 20));
        channel.writeInbound(lastContent((byte) 30, (byte) 40));
        channel.runPendingTasks();

        assertEquals("B still gated", 1, cap.requests.size());

        // Complete A.
        writeOutAndComplete(fullOkNoBody());

        assertEquals("B dispatched", 2, cap.requests.size());
        // Body chunks for B must have been forwarded.
        assertFalse("B body content forwarded", cap.contents.isEmpty());
    }

    @Test
    public void testExchangeIdAdvancesAndStaleGuardWorks() {
        FlowState state = new FlowState();

        assertEquals("initial id is 0", 0L, state.getActiveExchangeId());

        long id1 = state.nextExchangeId();
        assertEquals(1L, id1);
        assertEquals(1L, state.getActiveExchangeId());

        long id2 = state.nextExchangeId();
        assertEquals(2L, id2);

        // A callback carrying id1 should be detected as stale when id2 is active.
        assertTrue("stale: id1 != activeExchangeId", state.getActiveExchangeId() != id1);
    }

    /**
     * {@code setClosedOrUpgraded} stops reading, disallows keep-alive, and
     * releases the pending admission queue.
     */
    @Test
    public void testUpgradeOrCloseSetsStopReadingAndReleasesQueue() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(bodylessGet("/a"));
        channel.runPendingTasks();

        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertTrue("B pending before upgrade", state().hasPendingAdmission());

        ReadFlowHandler.setClosedOrUpgraded(channel);
        channel.runPendingTasks();

        assertTrue("stoppedReading after upgrade", state().stoppedReading());
        assertFalse("keepAliveAllowed false", state().isKeepAliveAllowed());
        assertFalse("queue released after upgrade", state().hasPendingAdmission());
    }

    /** Plain (non-full) 204 response: exchange must not complete on the header write alone. */
    @Test
    public void testPlain204HeaderWriteDoesNotAdmitB() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(bodylessGet("/a"));
        channel.runPendingTasks();

        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("B gated", 1, cap.requests.size());

        // Write a plain (non-full) 204 response — this is an HttpResponse but
        // NOT a FullHttpResponse, so the exchange must NOT complete here.
        DefaultHttpResponse plain204 = new DefaultHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT);
        plain204.headers().set("Connection", "keep-alive");
        ChannelPromise headerPromise = writeOutManual(plain204);
        succeed(headerPromise);

        // Header write succeeded but exchange must still be in-flight.
        assertEquals("B must NOT be admitted after plain-204 header write", 1, cap.requests.size());
        assertTrue("responseInFlight still true after plain-204 header", state().isResponseInFlight());

        // Now the terminal LastHttpContent arrives — this closes the exchange.
        writeOutAndComplete(new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER));

        assertEquals("B admitted after terminal write for plain 204", 2, cap.requests.size());
    }

    /**
     * Plain (non-full) 304 Not Modified response: same contract as 204,
     * the exchange must not complete on the header write alone.
     */
    @Test
    public void testPlain304HeaderWriteDoesNotAdmitB() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(bodylessGet("/a"));
        channel.runPendingTasks();

        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("B gated", 1, cap.requests.size());

        DefaultHttpResponse plain304 = new DefaultHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_MODIFIED);
        plain304.headers().set("Connection", "keep-alive");
        ChannelPromise headerPromise = writeOutManual(plain304);
        succeed(headerPromise);

        assertEquals("B must NOT be admitted after plain-304 header write", 1, cap.requests.size());
        assertTrue("responseInFlight still true after plain-304 header", state().isResponseInFlight());

        writeOutAndComplete(new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER));

        assertEquals("B admitted after terminal write for plain 304", 2, cap.requests.size());
    }

    /**
     * Plain (non-full) response to a HEAD request: the exchange completes only
     * on the terminal write, not on the header write.
     */
    @Test
    public void testPlainHeadResponseHeaderWriteDoesNotAdmitB() {
        CapturingHandler cap = buildChannel();

        // Request A is a HEAD.
        DefaultFullHttpRequest headA = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.HEAD, "/a", Unpooled.EMPTY_BUFFER);
        headA.headers().set("Connection", "keep-alive");
        channel.writeInbound(headA);
        channel.runPendingTasks();
        assertEquals("HEAD A dispatched", 1, cap.requests.size());

        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("B gated", 1, cap.requests.size());

        // Plain (non-full) HttpResponse for the HEAD — must NOT complete the exchange.
        DefaultHttpResponse plainHead = new DefaultHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        plainHead.headers().set("Content-Length", "100"); // would-be body length, never sent
        plainHead.headers().set("Connection", "keep-alive");
        ChannelPromise headerPromise = writeOutManual(plainHead);
        succeed(headerPromise);

        assertEquals("B must NOT be admitted after plain HEAD header write", 1, cap.requests.size());
        assertTrue("responseInFlight still true after plain HEAD header", state().isResponseInFlight());

        // Terminal write completes the exchange.
        writeOutAndComplete(new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER));

        assertEquals("B admitted after terminal write for HEAD response", 2, cap.requests.size());
    }

    /**
     * Informational (100 Continue) response must not touch {@code responseInFlight}
     * and must not trigger admission of the next request.
     */
    @Test
    public void testInformationalResponseDoesNotReleaseB() {
        CapturingHandler cap = buildChannel();

        DefaultHttpRequest reqA = new DefaultHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.POST, "/a");
        reqA.headers().set("Content-Length", "4");
        reqA.headers().set("Expect", "100-continue");
        channel.writeInbound(reqA);
        channel.runPendingTasks();
        assertEquals("A dispatched", 1, cap.requests.size());

        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("B gated before 100-Continue", 1, cap.requests.size());

        // 100 Continue — must not affect responseInFlight or admit B.
        writeOut(continueResponse());

        assertTrue("responseInFlight unchanged after 100-Continue", state().isResponseInFlight());
        assertEquals("B still gated after 100-Continue", 1, cap.requests.size());

        // Finish A: body then real response.
        channel.writeInbound(lastContent((byte) 1, (byte) 2, (byte) 3, (byte) 4));
        channel.runPendingTasks();

        DefaultFullHttpResponse resp = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.EMPTY_BUFFER);
        resp.headers().set("Content-Length", "0");
        resp.headers().set("Connection", "keep-alive");
        writeOut(resp);
        completeExchangeLifecycle();

        assertEquals("B admitted after real response", 2, cap.requests.size());
    }

    /** Raw {@link ByteBuf} write failure poisons the exchange; subsequent terminal success does not rescue it. */
    @Test
    public void testRawByteBufFailurePoisonsExchange() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(bodylessGet("/a"));
        channel.runPendingTasks();

        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("B gated", 1, cap.requests.size());

        // Write response headers (streaming, non-full).
        succeed(writeOutManual(streamingOk()));

        // Write a raw ByteBuf body chunk — this simulates what
        // NettyTCPWriteRequestContext does for Content-Length responses.
        ByteBuf rawBody = Unpooled.copiedBuffer(new byte[]{1, 2, 3});
        ChannelPromise rawPromise = writeOutManual(rawBody);

        // The raw write fails.
        fail(rawPromise);

        // Exchange must be poisoned and channel closed immediately.
        assertFalse("channel closed after raw ByteBuf write failure", channel.isActive());
        assertEquals("B never dispatched", 1, cap.requests.size());
    }

    /** Raw ByteBuf failure then terminal success: B is still never admitted. */
    @Test
    public void testRawByteBufFailureThenTerminalSuccessDoesNotAdmitB() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(bodylessGet("/a"));
        channel.runPendingTasks();

        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();

        succeed(writeOutManual(streamingOk()));

        ByteBuf rawBody = Unpooled.copiedBuffer(new byte[]{1, 2, 3});
        ChannelPromise rawPromise = writeOutManual(rawBody);

        // Fail the raw body write — poisons exchange, closes channel.
        fail(rawPromise);
        assertFalse("channel closed by poison", channel.isActive());
        assertEquals("B never dispatched regardless of later terminal", 1, cap.requests.size());
    }

    /** Raw ByteBuf write succeeds: exchange completes normally and B is admitted after the terminal write. */
    @Test
    public void testRawByteBufSuccessAllowsNormalCompletion() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(bodylessGet("/a"));
        channel.runPendingTasks();

        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("B gated", 1, cap.requests.size());

        succeed(writeOutManual(streamingOk()));

        // Successful raw ByteBuf write.
        ByteBuf rawBody = Unpooled.copiedBuffer(new byte[]{1, 2, 3});
        succeed(writeOutManual(rawBody));

        // B still gated — terminal write not yet sent.
        assertEquals("B still gated after successful raw write", 1, cap.requests.size());
        assertTrue("channel still active", channel.isActive());

        // Terminal write closes the exchange.
        writeOutAndComplete(new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER));

        assertEquals("B admitted after terminal write", 2, cap.requests.size());
        assertTrue("channel still active", channel.isActive());
    }

    /**
     * Body arrives across multiple read cycles after the response completes;
     * the handler must reschedule a read after each non-terminal fragment.
     */
    @Test
    public void testPurgeReadsRescheduledAfterNonTerminalFragment() {
        CapturingHandler cap = buildChannel();

        // A with 6-byte body that the application will not read.
        channel.writeInbound(requestWithBody("/a", 6));
        channel.runPendingTasks();
        assertEquals("A dispatched", 1, cap.requests.size());

        // Response completes before body is fully received.
        writeOut(fullOkNoBody());
        // After response, requestConsumed=false, bodyReadWanted=true via onResponseComplete.
        assertFalse("requestConsumed still false after response", state().isRequestConsumed());
        assertTrue("bodyReadWanted after response", state().isBodyReadWanted());

        // First fragment — non-terminal; channelReadComplete must reschedule.
        channel.writeInbound(bodyChunk((byte) 1, (byte) 2, (byte) 3));
        channel.runPendingTasks();
        // channelReadComplete should have rescheduled a read; state must not be stuck.
        assertFalse("requestConsumed still false after first fragment", state().isRequestConsumed());
        assertTrue("channel still active", channel.isActive());

        // Second (terminal) fragment — purge complete.
        channel.writeInbound(lastContent((byte) 4, (byte) 5, (byte) 6));
        channel.runPendingTasks();
        assertTrue("requestConsumed after terminal fragment", state().isRequestConsumed());
        assertTrue("channel still active after purge", channel.isActive());
    }

    /** Three body fragments arrive after the response; B is admitted only after the last. */
    @Test
    public void testPurgeMultipleFragmentsAllRescheduled() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(requestWithBody("/a", 9));
        channel.runPendingTasks();

        // Queue B.
        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("B gated", 1, cap.requests.size());

        // Response finishes before any body arrives.
        writeOut(fullOkNoBody());
        assertEquals("B still gated after response", 1, cap.requests.size());
        assertFalse("requestConsumed false", state().isRequestConsumed());

        // Fragment 1.
        channel.writeInbound(bodyChunk((byte) 1, (byte) 2, (byte) 3));
        channel.runPendingTasks();
        assertEquals("B still gated after fragment 1", 1, cap.requests.size());
        assertFalse("requestConsumed false after fragment 1", state().isRequestConsumed());

        // Fragment 2.
        channel.writeInbound(bodyChunk((byte) 4, (byte) 5, (byte) 6));
        channel.runPendingTasks();
        assertEquals("B still gated after fragment 2", 1, cap.requests.size());
        assertFalse("requestConsumed false after fragment 2", state().isRequestConsumed());

        // Terminal fragment.
        channel.writeInbound(lastContent((byte) 7, (byte) 8, (byte) 9));
        channel.runPendingTasks();
        completeExchangeLifecycle();
        assertTrue("requestConsumed after terminal", state().isRequestConsumed());
        assertEquals("B admitted after all fragments purged", 2, cap.requests.size());
    }

    /** Calling markRequestConsumed twice is idempotent; no double admission. */
    @Test
    public void testMarkRequestConsumedIsIdempotent() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(requestWithBody("/a", 2));
        channel.runPendingTasks();

        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();

        writeOut(fullOkNoBody());

        // Terminal body drains normally; complete lifecycle so B can be admitted.
        channel.writeInbound(lastContent((byte) 1, (byte) 2));
        channel.runPendingTasks();
        completeExchangeLifecycle();
        assertTrue("requestConsumed", state().isRequestConsumed());
        assertEquals("B admitted once", 2, cap.requests.size());

        // Mark again — must be idempotent: B must not be dispatched twice, no crash.
        ReadFlowHandler.markRequestConsumed(channel);
        channel.runPendingTasks();

        // The count must remain 2 — no duplicate admission.
        assertEquals("no duplicate admission", 2, cap.requests.size());
    }

    /** Body fully buffered before the response write: B is admitted immediately on the terminal write. */
    @Test
    public void testBufferedBodyBeforeResponseAdmitsBImmediately() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(requestWithBody("/a", 4));
        channel.runPendingTasks();

        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("B gated during response", 1, cap.requests.size());

        // Full body arrives before response terminal.
        channel.writeInbound(bodyChunk((byte) 10, (byte) 20));
        channel.writeInbound(lastContent((byte) 30, (byte) 40));
        channel.runPendingTasks();
        assertTrue("requestConsumed after body", state().isRequestConsumed());
        assertEquals("B still gated (response in flight)", 1, cap.requests.size());

        // Response terminal write — B must be admitted immediately.
        writeOutAndComplete(fullOkNoBody());
        assertEquals("B admitted after response terminal", 2, cap.requests.size());
    }

    /**
     * Disconnect during purge: no reuse but also no crash or double-release.
     * The connection must close cleanly; B is never dispatched.
     */
    @Test
    public void testDisconnectDuringPurgeClosesCleanly() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(requestWithBody("/a", 6));
        channel.runPendingTasks();

        // Queue B.
        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();

        // Response complete — purge in progress.
        writeOut(fullOkNoBody());
        assertFalse("requestConsumed false", state().isRequestConsumed());

        // Partial fragment.
        channel.writeInbound(bodyChunk((byte) 1, (byte) 2));
        channel.runPendingTasks();

        // Channel closes mid-purge.
        channel.close();
        channel.runPendingTasks();

        assertFalse("channel closed", channel.isActive());
        assertFalse("queue released on close", state().hasPendingAdmission());
        assertEquals("B never admitted", 1, cap.requests.size());
    }

    /** Connection:close during purge: channel closes immediately; no purge reads attempted. */
    @Test
    public void testConnectionCloseSkipsPurge() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(requestWithBody("/a", 6));
        channel.runPendingTasks();

        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();

        // Connection: close response — keepAlive=false.
        DefaultFullHttpResponse closeResp = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.EMPTY_BUFFER);
        closeResp.headers().set("Connection", "close");
        closeResp.headers().set("Content-Length", "0");
        writeOut(closeResp);

        assertFalse("channel closed after Connection:close", channel.isActive());
        assertEquals("B never admitted", 1, cap.requests.size());
    }

    /** Fully-consumed normal request: keep-alive reuse is unaffected. */
    @Test
    public void testFullyConsumedRequestKeepAliveUnchanged() {
        CapturingHandler cap = buildChannel();

        // A: body fully read before response.
        channel.writeInbound(requestWithBody("/a", 4));
        channel.writeInbound(lastContent((byte) 1, (byte) 2, (byte) 3, (byte) 4));
        channel.runPendingTasks();
        assertTrue("A requestConsumed", state().isRequestConsumed());

        writeOutAndComplete(fullOkNoBody());
        assertTrue("channel alive", channel.isActive());

        // B arrives on the same socket — same-socket reuse.
        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("B dispatched immediately", 2, cap.requests.size());

        writeOut(fullOkNoBody()); // B's response — no further request expected
        assertTrue("channel alive after B", channel.isActive());
    }

    /**
     * Chunked request body with empty terminal {@link LastHttpContent} —
     * the trailing empty chunk must still mark the request as consumed.
     */
    @Test
    public void testChunkedBodyWithEmptyTerminalMarksConsumed() {
        CapturingHandler cap = buildChannel();

        DefaultHttpRequest chunked = new DefaultHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.POST, "/a");
        chunked.headers().set("Transfer-Encoding", "chunked");
        chunked.headers().set("Connection", "keep-alive");
        channel.writeInbound(chunked);
        channel.runPendingTasks();
        assertFalse("requestConsumed false for chunked", state().isRequestConsumed());

        // Data chunk.
        channel.writeInbound(bodyChunk((byte) 0x41, (byte) 0x42));
        channel.runPendingTasks();

        // Empty terminal (0-sized chunk).
        channel.writeInbound(new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER));
        channel.runPendingTasks();
        assertTrue("requestConsumed after empty terminal", state().isRequestConsumed());

        writeOutAndComplete(fullOkNoBody());
        assertTrue("channel alive", channel.isActive());

        // B on same socket.
        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("B dispatched", 2, cap.requests.size());
    }

    /**
     * Both orderings (response-first and body-first) result in B admitted
     * exactly once.
     */
    @Test
    public void testResponseFirstAndPurgeFirstOrderingBothWork() {
        // Case A: response-first.
        {
            CapturingHandler cap = buildChannel();

            channel.writeInbound(requestWithBody("/a", 2));
            channel.runPendingTasks();
            channel.writeInbound(bodylessGet("/b"));
            channel.runPendingTasks();

            writeOut(fullOkNoBody()); // response first
            assertEquals("A: B gated after response", 1, cap.requests.size());

            channel.writeInbound(lastContent((byte) 1, (byte) 2)); // body second
            channel.runPendingTasks();
            completeExchangeLifecycle();
            assertEquals("A: B admitted after body", 2, cap.requests.size());

            try { channel.finishAndReleaseAll(); } catch (Throwable ignored) {}
            channel = null;
        }

        // Case B: purge-first.
        {
            CapturingHandler cap = buildChannel();

            channel.writeInbound(requestWithBody("/a", 2));
            channel.runPendingTasks();
            channel.writeInbound(bodylessGet("/b"));
            channel.runPendingTasks();

            channel.writeInbound(lastContent((byte) 1, (byte) 2)); // body first
            channel.runPendingTasks();
            assertTrue("B: requestConsumed after body", state().isRequestConsumed());
            assertEquals("B: B still gated (response in flight)", 1, cap.requests.size());

            writeOutAndComplete(fullOkNoBody()); // response second
            assertEquals("B: B admitted immediately", 2, cap.requests.size());
        }
    }

    /** Response first, body later: B is gated until markRequestConsumed fires on the terminal fragment. */
    @Test
    public void testCleanupOrderingResponseFirstBodySecond() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(requestWithBody("/a", 4));
        channel.runPendingTasks();
        assertEquals("A dispatched", 1, cap.requests.size());

        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("B gated", 1, cap.requests.size());

        // Response terminal write — body not yet drained.
        writeOut(fullOkNoBody());
        assertEquals("B still gated: cleanup not done (body outstanding)", 1, cap.requests.size());
        assertFalse("requestConsumed false after response", state().isRequestConsumed());

        // Partial body — still not done.
        channel.writeInbound(bodyChunk((byte) 1, (byte) 2));
        channel.runPendingTasks();
        assertEquals("B still gated after partial body", 1, cap.requests.size());
        assertFalse("requestConsumed false after partial body", state().isRequestConsumed());

        // Terminal body — triggers setBodyComplete() → isc.clear() →
        // markRequestConsumed() → drainPendingAdmission().
        channel.writeInbound(lastContent((byte) 3, (byte) 4));
        channel.runPendingTasks();
        completeExchangeLifecycle();

        assertTrue("requestConsumed after terminal body", state().isRequestConsumed());
        assertEquals("B admitted after cleanup sequence completes", 2, cap.requests.size());
        assertFalse("no pending admission after B", state().hasPendingAdmission());
    }

    /** Body drained first, response later: B is gated until the terminal response write then admitted immediately. */
    @Test
    public void testCleanupOrderingBodyFirstResponseSecond() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(requestWithBody("/a", 4));
        channel.runPendingTasks();

        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("B gated", 1, cap.requests.size());

        // Full body arrives before response.
        channel.writeInbound(bodyChunk((byte) 10, (byte) 20));
        channel.writeInbound(lastContent((byte) 30, (byte) 40));
        channel.runPendingTasks();
        assertTrue("requestConsumed after full body", state().isRequestConsumed());
        assertEquals("B still gated: response in flight", 1, cap.requests.size());

        // Response terminal — B admitted immediately without waiting for another body read.
        writeOutAndComplete(fullOkNoBody());
        assertEquals("B admitted immediately: both conditions met", 2, cap.requests.size());
        assertFalse("no pending after B", state().hasPendingAdmission());
    }

    /**
     * Stale markRequestConsumed call after B is admitted must not cause a
     * second admission of B or admit a phantom request C.
     */
    @Test
    public void testStaleMarkRequestConsumedDoesNotAdmitExtraRequest() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(requestWithBody("/a", 2));
        channel.runPendingTasks();

        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();

        writeOut(fullOkNoBody()); // response-first

        channel.writeInbound(lastContent((byte) 1, (byte) 2));
        channel.runPendingTasks();
        completeExchangeLifecycle();
        assertEquals("B admitted", 2, cap.requests.size());
        assertTrue("requestConsumed", state().isRequestConsumed());

        // Stale call — must be idempotent.
        ReadFlowHandler.markRequestConsumed(channel);
        channel.runPendingTasks();

        assertEquals("no extra admission from stale call", 2, cap.requests.size());
        assertTrue("channel still alive", channel.isActive());
    }

    /**
     * Early error response with unread body: B is gated until the terminal
     * body fragment arrives and markRequestConsumed fires.
     */
    @Test
    public void testEarlyErrorResponseGatesBUntilBodyComplete() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(requestWithBody("/a", 3));
        channel.runPendingTasks();
        assertEquals("A dispatched", 1, cap.requests.size());

        // Queue B.
        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("B gated", 1, cap.requests.size());

        // Early 400 response (simulated via streaming response + terminal write).
        writeOut(streamingOk());
        writeOut(new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER));

        // Response done but body not yet received — B must remain gated.
        assertEquals("B still gated after error response", 1, cap.requests.size());
        assertFalse("requestConsumed false: body not complete", state().isRequestConsumed());

        // Terminal body arrives — cleanup fires.
        channel.writeInbound(lastContent((byte) 1, (byte) 2, (byte) 3));
        channel.runPendingTasks();
        completeExchangeLifecycle();

        assertTrue("requestConsumed after body", state().isRequestConsumed());
        assertEquals("B admitted after body complete", 2, cap.requests.size());
    }

    /** Records inbound {@link HttpRequest} and {@link HttpContent}; releases each after recording. */
    private static class CapturingHandler extends ChannelDuplexHandler {
        final List<HttpRequest> requests = new ArrayList<>();
        final List<HttpContent> contents = new ArrayList<>();

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof HttpRequest) {
                requests.add((HttpRequest) msg);
            }
            if (msg instanceof HttpContent) {
                contents.add((HttpContent) msg);
            }
            ReferenceCountUtil.release(msg);
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
                throws Exception {
            super.write(ctx, msg, promise);
        }
    }
}
