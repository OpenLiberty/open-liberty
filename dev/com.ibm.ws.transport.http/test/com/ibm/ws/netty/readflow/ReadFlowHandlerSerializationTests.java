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
import java.util.List;

import io.netty.buffer.ByteBuf;

import org.junit.After;
import org.junit.Test;

import com.ibm.ws.http.netty.pipeline.inbound.read.FlowState;
import com.ibm.ws.http.netty.pipeline.inbound.read.ReadFlowHandler;

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
 *
 * Uses Netty's {@link EmbeddedChannel} for deterministic, synchronous event-loop
 * execution. Controllable {@link ChannelPromise} objects are used where write
 * outcomes need to be varied — no timing sleeps.
 *
 * Pipeline: ReadFlowHandler → CapturingHandler
 *
 * Ownership conventions used throughout:
 *   - {@code EmbeddedChannel.writeInbound(msg)} does NOT retain {@code msg}; it
 *     calls {@code pipeline.fireChannelRead(msg)} which hands the sole ref to the
 *     first handler. That handler either forwards it (transferring ownership) or
 *     parks it (taking ownership without an extra retain).
 *   - {@code EmbeddedChannel.writeOutbound(msg)} passes the message down the
 *     outbound pipeline. {@link ReadFlowHandler#write} intercepts it for promise
 *     listeners and then forwards via {@code super.write}.
 */
public class ReadFlowHandlerSerializationTests {

    private EmbeddedChannel channel;

    @After
    public void teardown() {
        if (channel != null) {
            try { channel.finishAndReleaseAll(); } catch (Throwable ignored) {}
        }
    }

    // -----------------------------------------------------------------------
    // Pipeline / helper factory
    // -----------------------------------------------------------------------

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

    /**
     * A self-contained 200 OK with Content-Length:0. This is a
     * {@link DefaultFullHttpResponse} (also implements {@link LastHttpContent}).
     * It should complete the exchange on its single write.
     */
    private static HttpResponse fullOkNoBody() {
        DefaultFullHttpResponse r = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.EMPTY_BUFFER);
        r.headers().set("Content-Length", "0");
        r.headers().set("Connection", "keep-alive");
        return r;
    }

    /**
     * A streaming (non-full) 200 OK with chunked transfer encoding.
     * Exchange completes only when the separate {@link LastHttpContent} write
     * succeeds.
     */
    private static HttpResponse streamingOk() {
        DefaultHttpResponse r = new DefaultHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        r.headers().set("Transfer-Encoding", "chunked");
        r.headers().set("Connection", "keep-alive");
        return r;
    }

    /**
     * A streaming (non-full) 200 OK with an explicit Content-Length body.
     * Even though Content-Length is known, this is not a FullHttpResponse;
     * the exchange must NOT complete until the terminal LastHttpContent write.
     */
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

    /**
     * Write outbound using a manually-controlled promise so we can force
     * success or failure independently.
     */
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
        p.setFailure(new java.io.IOException("simulated write failure"));
        channel.runPendingTasks();
    }

    // -----------------------------------------------------------------------
    // Test 1 — B arrives before A's delayed terminal write; B stays undispatched
    // -----------------------------------------------------------------------
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

        // Complete A's response.
        writeOut(new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER));

        assertEquals("B dispatched after A's terminal write succeeds", 2, cap.requests.size());
        assertEquals("/b", cap.requests.get(1).uri());
        assertFalse("pending queue empty after drain", state().hasPendingAdmission());
    }

    // -----------------------------------------------------------------------
    // Test 2 — Zero-length ordinary response: must wait for a separate terminal
    // write, NOT complete on the header write alone.
    // -----------------------------------------------------------------------
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

        // Now write the terminal LastHttpContent.
        writeOut(new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER));

        assertEquals("B admitted after terminal write", 2, cap.requests.size());
    }

    // -----------------------------------------------------------------------
    // Test 3 — FullHttpResponse (body-bearing): completes on its own write;
    //           informational response does NOT release B.
    // -----------------------------------------------------------------------
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

        assertEquals("B admitted after FullHttpResponse write", 2, cap.requests.size());
    }

    // -----------------------------------------------------------------------
    // Test 4 — 204 No Content completes on its header write (no separate
    //           LastHttpContent expected from the codec for 204).
    // -----------------------------------------------------------------------
    @Test
    public void test204CompletesOnHeaderWrite() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(bodylessGet("/a"));
        channel.runPendingTasks();

        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("B gated", 1, cap.requests.size());

        writeOut(noContentResponse());  // 204 — FullHttpResponse, no body

        assertEquals("B admitted after 204", 2, cap.requests.size());
    }

    // -----------------------------------------------------------------------
    // Test 5 — Write-failure cases: header fail, body-chunk fail, terminal fail,
    //           and body-chunk fail followed by terminal success (still must close).
    // -----------------------------------------------------------------------
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

    // -----------------------------------------------------------------------
    // Tests 5e–5j — Async body purge / incomplete-body regression coverage
    //
    // Root cause: prepareNettyCloseForIncompleteRequestBody() forced
    // Connection:close and set RESPONSE_CLOSE_BEFORE_REQUEST_BODY_COMPLETE,
    // which caused the drain to be skipped in nettyClose. With requestConsumed
    // remaining false and no further reads scheduled, the connection stalled
    // until a 30-second PersistTimeoutException fired.
    //
    // Fix: onResponseComplete now gates on requestConsumed. When the body is
    // still unread, it calls setBodyReadWanted(true) so the existing
    // requestRead() path delivers remaining body chunks. Once LastHttpContent
    // arrives, markRequestConsumed fires, which calls verifyNeedRead /
    // drainPendingAdmission — reusing those methods as intended.
    // -----------------------------------------------------------------------

    /**
     * 5e — Early error response with unread body: B must NOT be admitted until
     * the remaining body bytes are drained (LastHttpContent arrives).
     *
     * This is the direct regression for the PersistTimeoutException stall:
     * A sends a 400 error response before consuming its 3-byte request body.
     * B must wait for A's body to finish — not just A's terminal write.
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

        // Body now consumed; B admitted via markRequestConsumed → drainPendingAdmission.
        assertTrue("requestConsumed after LastHttpContent", state().isRequestConsumed());
        assertEquals("B admitted after body drained", 2, cap.requests.size());
    }

    /**
     * 5f — Body arrives in multiple fragments; response terminal write happens
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

        assertTrue("requestConsumed after final fragment", state().isRequestConsumed());
        assertEquals("B admitted after last fragment", 2, cap.requests.size());
    }

    /**
     * 5g — Body fully received BEFORE the response terminal write. Since
     * requestConsumed is already true when onResponseComplete fires, B is
     * admitted immediately without waiting (via drainPendingAdmission).
     */
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
        writeOut(fullOkNoBody());
        assertEquals("B admitted immediately — body was already drained", 2, cap.requests.size());
    }

    /**
     * 5h — Explicit Connection:close on response: no purge needed, channel must
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

    /**
     * 5i-pre — B arrives AFTER A's response completes but BEFORE A's body is fully
     * drained. The !requestConsumed gate must park B even though responseInFlight
     * is already false and the pending queue was empty at that moment.
     */
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

        assertTrue("requestConsumed after body", state().isRequestConsumed());
        assertEquals("B admitted after purge", 2, cap.requests.size());
    }

    /**
     * 5i — Three pipelined requests where A has an unread body and B/C are queued.
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
        assertEquals("B dispatched after A body drained", 2, cap.requests.size());

        // B's response.
        writeOut(fullOkNoBody());
        assertEquals("C dispatched after B response", 3, cap.requests.size());

        // Verify ordering.
        assertEquals("/a", cap.requests.get(0).uri());
        assertEquals("/b", cap.requests.get(1).uri());
        assertEquals("/c", cap.requests.get(2).uri());
    }

    /**
     * 5j — Fully-consumed normal request: keep-alive reuse works as before;
     * no regression from the purge path changes.
     */
    @Test
    public void testNormalKeepAliveRequestUnaffectedByPurgeChanges() {
        CapturingHandler cap = buildChannel();

        // A with fully-read body.
        channel.writeInbound(requestWithBody("/a", 3));
        channel.writeInbound(lastContent((byte) 1, (byte) 2, (byte) 3));
        channel.runPendingTasks();
        assertTrue("A requestConsumed", state().isRequestConsumed());

        writeOut(fullOkNoBody());
        assertTrue("channel alive", channel.isActive());

        // B on the same connection.
        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertEquals("B dispatched immediately", 2, cap.requests.size());

        writeOut(fullOkNoBody());
        assertTrue("channel still alive after B", channel.isActive());
    }

    // -----------------------------------------------------------------------
    // Test 6 — Queued reference-counted content released on drain and on close.
    // -----------------------------------------------------------------------
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
        writeOut(fullOkNoBody());

        assertEquals("B dispatched", 2, cap.requests.size());
        assertFalse("queue empty", state().hasPendingAdmission());
        // refCnt: created=1, no extra retain (excess-retain fix), downstream
        // released it to 0 via CapturingHandler.channelRead → ReferenceCountUtil.release().
        assertEquals("B's ref released by downstream", 0, reqB.refCnt());
    }

    // -----------------------------------------------------------------------
    // Test 7 — Fragmented request body continues streaming during active exchange.
    // -----------------------------------------------------------------------
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

    // -----------------------------------------------------------------------
    // Test 8 — Pending read demand satisfied from queue (no duplicate socket
    //           reads when admission queue holds the next request).
    // -----------------------------------------------------------------------
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
        writeOut(fullOkNoBody());
        assertEquals("B dispatched from queue", 2, cap.requests.size());
        assertFalse("queue empty", state().hasPendingAdmission());

        // Complete B.
        writeOut(fullOkNoBody());
        assertTrue("channel alive", channel.isActive());
    }

    // -----------------------------------------------------------------------
    // Test 9 — Multiple requests from same read window remain serialised.
    // -----------------------------------------------------------------------
    @Test
    public void testMultipleRequestsFromSameReadRemainSerialized() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(bodylessGet("/a"));
        channel.writeInbound(bodylessGet("/b"));
        channel.writeInbound(bodylessGet("/c"));
        channel.runPendingTasks();

        assertEquals("Only A dispatched initially", 1, cap.requests.size());

        writeOut(fullOkNoBody());
        assertEquals("B dispatched after A", 2, cap.requests.size());

        writeOut(fullOkNoBody());
        assertEquals("C dispatched after B", 3, cap.requests.size());

        writeOut(fullOkNoBody());
        assertEquals("/a", cap.requests.get(0).uri());
        assertEquals("/b", cap.requests.get(1).uri());
        assertEquals("/c", cap.requests.get(2).uri());
    }

    // -----------------------------------------------------------------------
    // Test 10 — Queued body chunks replayed with their request after drain.
    // -----------------------------------------------------------------------
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
        writeOut(fullOkNoBody());

        assertEquals("B dispatched", 2, cap.requests.size());
        // Body chunks for B must have been forwarded.
        assertFalse("B body content forwarded", cap.contents.isEmpty());
    }

    // -----------------------------------------------------------------------
    // Test 11 — Exchange id advances monotonically; stale guard logic works.
    // -----------------------------------------------------------------------
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

    // -----------------------------------------------------------------------
    // Test 12 — setClosedOrUpgraded stops reading and releases queue.
    // -----------------------------------------------------------------------
    @Test
    public void testUpgradeOrCloseSetsStopReadingAndReleasesQueue() {
        CapturingHandler cap = buildChannel();

        channel.writeInbound(bodylessGet("/a"));
        channel.runPendingTasks();

        channel.writeInbound(bodylessGet("/b"));
        channel.runPendingTasks();
        assertTrue("B pending before upgrade", state().hasPendingAdmission());

        ReadFlowHandler.setClosedOrUpgraded(channel.pipeline().context(ReadFlowHandler.class));
        channel.runPendingTasks();

        assertTrue("stoppedReading after upgrade", state().stoppedReading());
        assertFalse("keepAliveAllowed false", state().isKeepAliveAllowed());
        assertFalse("queue released after upgrade", state().hasPendingAdmission());
    }

    // -----------------------------------------------------------------------
    // Regression tests for the two HTTP/1.1 pipelining blockers
    // -----------------------------------------------------------------------

    /**
     * Blocker 1a — plain (non-full) 204 No Content response.
     *
     * A {@link DefaultHttpResponse} (not a {@code FullHttpResponse}) with status
     * 204 must NOT complete the exchange on its header write.  The shortcut that
     * previously used {@code !isResponseBodyPermitted(code)} was removed;
     * completion now requires an actual terminal write.
     *
     * B must remain blocked until the separate {@link LastHttpContent} write
     * succeeds.
     */
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
        writeOut(new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER));

        assertEquals("B admitted after terminal write for plain 204", 2, cap.requests.size());
    }

    /**
     * Blocker 1b — plain (non-full) 304 Not Modified response.
     *
     * Same contract as 204: the exchange must not complete on the header write.
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

        writeOut(new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER));

        assertEquals("B admitted after terminal write for plain 304", 2, cap.requests.size());
    }

    /**
     * Blocker 1c — plain (non-full) response to a HEAD request.
     *
     * The handler must not use {@code state.isHeadRequest()} as a shortcut
     * for self-contained detection.  A plain {@link DefaultHttpResponse} for
     * a HEAD request completes only on the terminal write.
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
        writeOut(new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER));

        assertEquals("B admitted after terminal write for HEAD response", 2, cap.requests.size());
    }

    /**
     * Blocker 1d — informational (100 Continue) response does NOT release B.
     *
     * Informational responses must not touch {@code responseInFlight} and must
     * not trigger admission of the next request.
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

        assertEquals("B admitted after real response", 2, cap.requests.size());
    }

    /**
     * Blocker 2a — raw ByteBuf failure followed by terminal success.
     *
     * When a fixed-length HTTP/1 body is written as a raw {@link ByteBuf}
     * (as done by {@code NettyTCPWriteRequestContext} for Content-Length responses)
     * and that write fails, the exchange must be poisoned.  Even if the terminal
     * {@link LastHttpContent} write subsequently succeeds, B must never be admitted
     * and the connection must be closed.
     */
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

    /**
     * Blocker 2b — raw ByteBuf failure: terminal success does not rescue the exchange.
     *
     * Even if the terminal write has already been enqueued and its promise resolves
     * successfully after the raw body write fails, B must still not be admitted.
     */
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

    /**
     * Blocker 2c — raw ByteBuf write succeeds; full exchange completes normally.
     *
     * A raw ByteBuf write that succeeds must not interfere with normal admission.
     * B must be admitted after the terminal write succeeds.
     */
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
        writeOut(new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER));

        assertEquals("B admitted after terminal write", 2, cap.requests.size());
        assertTrue("channel still active", channel.isActive());
    }

    // -----------------------------------------------------------------------
    // Inner capturing handler
    // -----------------------------------------------------------------------

    /**
     * Records every inbound {@link HttpRequest} and {@link HttpContent}.
     * Releases each message after recording so the reference count drops to
     * zero as expected by tests that check for leaks.
     */
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
