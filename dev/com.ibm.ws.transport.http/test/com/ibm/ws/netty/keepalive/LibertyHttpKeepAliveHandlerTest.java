/*******************************************************************************
 * Copyright 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.netty.keepalive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Test;

import com.ibm.ws.http.netty.NettyHttpChannelConfig;
import com.ibm.ws.http.netty.NettyHttpConstants;
import com.ibm.ws.http.netty.NettyHttpConstants.ProtocolName;
import com.ibm.ws.http.netty.ProtocolState;
import com.ibm.ws.http.netty.ProtocolState.ProtocolSource;
import com.ibm.ws.http.netty.pipeline.inbound.LibertyHttpKeepAliveHandler;

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

/**
 * Unit tests for {@link LibertyHttpKeepAliveHandler}.
 *
 * <p>The handler is the single place where Liberty enforces the
 * {@code keepAliveEnabled} and {@code maxKeepAliveRequests} httpOptions on the
 * Netty HTTP/1.x path. It does so by setting {@code Connection: close} on the
 * <em>inbound</em> request, which Netty's parent {@code HttpServerKeepAliveHandler}
 * then honors when the response is written.
 *
 * <p>Each test drives the handler inside an {@link EmbeddedChannel}, writes one
 * or more inbound requests, and inspects the {@code Connection} header on the
 * request that comes out the other side. The config is a Mockito mock, so no
 * server is needed.
 *
 * <p>The connection's protocol is established through {@link ProtocolState}
 * exactly as the real pipeline initializers do. This matters because the
 * handler decides whether to skip enforcement (HTTP/2) based on
 * {@link ProtocolState}, <strong>not</strong> on any request header. A client
 * controls request headers, so trusting {@code x-http2-stream-id} would let a
 * client switch enforcement off.
 */
public class LibertyHttpKeepAliveHandlerTest {

    /** Name of the header Netty's HTTP/2-to-HTTP/1 adapter adds. A client can also send it. */
    private static final String STREAM_ID_HEADER = "x-http2-stream-id";

    private EmbeddedChannel channel;

    @After
    public void tearDown() {
        if (channel != null) {
            try {
                // Releases any buffers still queued, for example HttpContent frames.
                channel.finishAndReleaseAll();
            } catch (Throwable t) {
                // Ignore: we are only cleaning up.
            }
        }
    }

    // ---------------------------------------------------------------------
    // keepAliveEnabled / maxKeepAliveRequests behavior on an HTTP/1.1 connection
    // ---------------------------------------------------------------------

    /**
     * With {@code keepAliveEnabled=false} the very first request must be marked
     * {@code Connection: close}, even though no request limit is configured.
     */
    @Test
    public void keepAliveDisabled_setsConnectionClose() {
        channel = newChannel(config(false, -1), ProtocolName.HTTP1, ProtocolSource.CLEARTEXT_HTTP1);

        HttpRequest request = sendRequest(HttpVersion.HTTP_1_1);

        assertTrue("keepAliveEnabled=false must set Connection: close", isConnectionClose(request));
    }

    /**
     * With keep-alive enabled and {@code maxKeepAliveRequests=-1} (unlimited),
     * the handler must never ask for the connection to be closed, no matter
     * how many requests arrive.
     */
    @Test
    public void noMaxConfigured_neverSetsClose() {
        channel = newChannel(config(true, -1), ProtocolName.HTTP1, ProtocolSource.CLEARTEXT_HTTP1);

        for (int i = 1; i <= 5; i++) {
            HttpRequest request = sendRequest(HttpVersion.HTTP_1_1);
            assertFalse("Request " + i + " must not be closed when max is unlimited",
                        isConnectionClose(request));
        }
        assertEquals("All five requests should be counted", Integer.valueOf(5), requestCount());
    }

    /**
     * With {@code maxKeepAliveRequests=3}, requests 1 and 2 are left alone and
     * request 3 (the limit) is marked {@code Connection: close} so the
     * connection is closed after its response.
     */
    @Test
    public void maxRequests_closesOnlyOnNthRequest() {
        channel = newChannel(config(true, 3), ProtocolName.HTTP1, ProtocolSource.CLEARTEXT_HTTP1);

        assertFalse("Request 1 of 3 must stay open", isConnectionClose(sendRequest(HttpVersion.HTTP_1_1)));
        assertFalse("Request 2 of 3 must stay open", isConnectionClose(sendRequest(HttpVersion.HTTP_1_1)));
        assertTrue("Request 3 of 3 must be closed", isConnectionClose(sendRequest(HttpVersion.HTTP_1_1)));
    }

    /**
     * Boundary case: {@code maxKeepAliveRequests=1} means the first request is
     * already the last one. The handler compares {@code count + 1 >= max}, so a
     * strict {@code >} would break this case specifically.
     */
    @Test
    public void maxOne_closesFirstRequest() {
        channel = newChannel(config(true, 1), ProtocolName.HTTP1, ProtocolSource.CLEARTEXT_HTTP1);

        assertTrue("max=1 must close the first request", isConnectionClose(sendRequest(HttpVersion.HTTP_1_1)));
    }

    // ---------------------------------------------------------------------
    // Protocol detection: the header-spoofing regression
    // ---------------------------------------------------------------------

    /**
     * Regression test for the header-spoofing bypass.
     *
     * <p>A client sends a request on an HTTP/1.1 connection that carries an
     * {@code x-http2-stream-id} header. An earlier version of the handler treated
     * that header as proof of HTTP/2 and skipped counting and enforcement, so a
     * client could defeat {@code keepAliveEnabled=false} and
     * {@code maxKeepAliveRequests}. The handler must instead rely on
     * {@link ProtocolState}, which says this connection is HTTP/1.1, so the
     * request is still counted and still closed.
     */
    @Test
    public void http1_spoofedStreamIdHeader_stillEnforced() {
        channel = newChannel(config(false, -1), ProtocolName.HTTP1, ProtocolSource.CLEARTEXT_HTTP1);

        HttpRequest request = newRequest(HttpVersion.HTTP_1_1);
        request.headers().set(STREAM_ID_HEADER, "3"); // spoofed by the client
        HttpRequest out = writeAndRead(request);

        assertTrue("A spoofed x-http2-stream-id header must not bypass keepAliveEnabled=false",
                   isConnectionClose(out));
        assertEquals("The spoofed request must still be counted", Integer.valueOf(1), requestCount());
    }

    /**
     * Same spoofing scenario against the request limit: every request on an
     * HTTP/1.1 connection carries the spoofed header, yet the limit must still
     * be reached and enforced on request 2.
     */
    @Test
    public void http1_spoofedStreamIdHeader_maxRequestsStillEnforced() {
        channel = newChannel(config(true, 2), ProtocolName.HTTP1, ProtocolSource.CLEARTEXT_HTTP1);

        HttpRequest first = newRequest(HttpVersion.HTTP_1_1);
        first.headers().set(STREAM_ID_HEADER, "3");
        assertFalse("Request 1 of 2 must stay open", isConnectionClose(writeAndRead(first)));

        HttpRequest second = newRequest(HttpVersion.HTTP_1_1);
        second.headers().set(STREAM_ID_HEADER, "5");
        assertTrue("Request 2 of 2 must be closed despite the spoofed header",
                   isConnectionClose(writeAndRead(second)));
    }

    /**
     * On a connection whose established protocol is HTTP/2 the handler must do
     * nothing: no counting and no {@code Connection: close}, even when
     * keep-alive is disabled and the limit is 1. Keep-alive is an HTTP/1.x
     * concept, and the HTTP/2 codec manages its own connection lifecycle.
     *
     * <p>In production the handler is never installed on HTTP/2 pipelines, so
     * this guards the defensive check against an unexpected pipeline change.
     */
    @Test
    public void http2Connection_isNotCountedOrClosed() {
        channel = newChannel(config(false, 1), ProtocolName.HTTP2, ProtocolSource.ALPN_HTTP2);

        HttpRequest request = sendRequest(HttpVersion.HTTP_1_1);

        assertFalse("HTTP/2 connections must not get Connection: close", isConnectionClose(request));
        assertNull("HTTP/2 requests must not be counted", requestCount());
    }

    /**
     * HTTP/1.0 connections are HTTP/1.x, so they must be counted and enforced
     * just like HTTP/1.1.
     */
    @Test
    public void http10Connection_isCounted() {
        channel = newChannel(config(true, 1), ProtocolName.HTTP10, ProtocolSource.CLEARTEXT_HTTP10);

        HttpRequest request = sendRequest(HttpVersion.HTTP_1_0);

        assertTrue("HTTP/1.0 connections must be enforced", isConnectionClose(request));
        assertEquals("The HTTP/1.0 request must be counted", Integer.valueOf(1), requestCount());
    }

    /**
     * If no protocol has been established (state is {@code UNKNOWN}) the
     * handler must fail toward enforcement, not skip it. Only an explicit HTTP/2
     * connection is exempt.
     */
    @Test
    public void unknownProtocol_failsTowardEnforcement() {
        // Deliberately do not call ProtocolState.establish.
        channel = newChannel(config(false, -1), null, null);
        assertEquals(ProtocolName.UNKNOWN, ProtocolState.current(channel));

        HttpRequest request = sendRequest(HttpVersion.HTTP_1_1);

        assertTrue("An unknown protocol must still be enforced", isConnectionClose(request));
        assertEquals("The request must be counted", Integer.valueOf(1), requestCount());
    }

    // ---------------------------------------------------------------------
    // Counter behavior
    // ---------------------------------------------------------------------

    /**
     * Only {@link HttpRequest} messages count toward the limit. A request that
     * has a body arrives as one {@code HttpRequest} followed by
     * {@code HttpContent} frames and a {@code LastHttpContent}. Those frames
     * must not inflate the counter, or a single POST could use up the whole
     * request limit.
     */
    @Test
    public void nonRequestMessages_doNotIncrementCounter() {
        channel = newChannel(config(true, 100), ProtocolName.HTTP1, ProtocolSource.CLEARTEXT_HTTP1);

        channel.writeInbound(newRequest(HttpVersion.HTTP_1_1));
        channel.writeInbound(new DefaultHttpContent(Unpooled.wrappedBuffer(new byte[] { 1, 2, 3 })));
        channel.writeInbound(new DefaultHttpContent(Unpooled.wrappedBuffer(new byte[] { 4, 5, 6 })));
        channel.writeInbound(new DefaultLastHttpContent());

        assertEquals("Only the HttpRequest should be counted, not its body frames",
                     Integer.valueOf(1), requestCount());
    }

    // ---------------------------------------------------------------------
    // Upgrade requests (WebSocket / Servlet upgrade)
    // ---------------------------------------------------------------------

    /**
     * Control for the upgrade tests below: an ordinary request with
     * {@code keepAliveEnabled=false} and an ordinary {@code 200} response must
     * close the connection after the response. This proves the harness drives
     * Netty's parent {@code HttpServerKeepAliveHandler} the way production does.
     */
    @Test
    public void nonUpgrade_keepAliveDisabled_200Response_closesChannel() {
        channel = newChannel(config(false, -1), ProtocolName.HTTP1, ProtocolSource.CLEARTEXT_HTTP1);
        sendRequest(HttpVersion.HTTP_1_1);

        FullHttpResponse out = writeResponse(newResponse(HttpResponseStatus.OK));

        assertTrue("The response must tell the client the connection is closing",
                   out.headers().containsValue(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE, true));
        assertFalse("The channel must be closed after the response", channel.isOpen());
    }

    /**
     * A client asks for a protocol upgrade (WebSocket, Servlet {@code HttpUpgradeHandler})
     * and the server agrees with {@code 101 Switching Protocols}. Even with
     * {@code keepAliveEnabled=false} the 101 must still say
     * {@code Connection: Upgrade} and the channel must stay open, because the
     * upgraded protocol now owns the socket.
     *
     * <p>The handler marks this request {@code Connection: close}, so this test
     * pins down what Netty's parent {@code HttpServerKeepAliveHandler} does with
     * a 101 in that situation: it leaves the response alone and does not close
     * the channel. It guards against a Netty upgrade or a change to this
     * handler altering that. The end-to-end behavior, including the server's own
     * upgrade handling, is covered by the servlet upgrade tests in
     * {@code KeepAliveEnforcementTest}.
     */
    @Test
    public void upgrade_keepAliveDisabled_101Response_staysUpgradeAndOpen() {
        channel = newChannel(config(false, -1), ProtocolName.HTTP1, ProtocolSource.CLEARTEXT_HTTP1);
        writeAndRead(newUpgradeRequest());

        FullHttpResponse switching = newResponse(HttpResponseStatus.SWITCHING_PROTOCOLS);
        switching.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE);
        switching.headers().set(HttpHeaderNames.UPGRADE, HttpHeaderValues.WEBSOCKET);
        FullHttpResponse out = writeResponse(switching);

        assertTrue("A 101 response must keep Connection: Upgrade",
                   out.headers().containsValue(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE, true));
        assertFalse("A 101 response must not become Connection: close",
                    out.headers().containsValue(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE, true));
        assertTrue("The channel must stay open for the upgraded protocol", channel.isOpen());
    }

    /**
     * The upgrade exemption must be decided by the <em>server's response</em>,
     * never by request headers alone. A client can put {@code Connection: Upgrade}
     * and {@code Upgrade: websocket} on every request. If those headers were
     * enough to skip enforcement, the client could defeat
     * {@code keepAliveEnabled=false} and {@code maxKeepAliveRequests}, which is
     * the same class of bypass as the spoofed {@code x-http2-stream-id} header.
     *
     * <p>Here the client asks to upgrade but the server answers an ordinary
     * {@code 200}, so keep-alive is still enforced.
     */
    @Test
    public void upgradeHeadersOnRequest_but200Response_stillClosesChannel() {
        channel = newChannel(config(false, -1), ProtocolName.HTTP1, ProtocolSource.CLEARTEXT_HTTP1);
        writeAndRead(newUpgradeRequest());

        FullHttpResponse out = writeResponse(newResponse(HttpResponseStatus.OK));

        assertTrue("Upgrade headers on the request must not bypass keepAliveEnabled=false",
                   out.headers().containsValue(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE, true));
        assertFalse("The channel must be closed after a non-101 response", channel.isOpen());
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /**
     * Builds a mock config.
     *
     * @param keepAliveEnabled value returned by {@code isKeepAliveEnabled()}
     * @param maxRequests      value returned by {@code getMaximumPersistentRequests()};
     *                             {@code -1} means unlimited
     */
    private static NettyHttpChannelConfig config(boolean keepAliveEnabled, int maxRequests) {
        NettyHttpChannelConfig config = mock(NettyHttpChannelConfig.class);
        when(config.isKeepAliveEnabled()).thenReturn(keepAliveEnabled);
        when(config.getMaximumPersistentRequests()).thenReturn(maxRequests);
        return config;
    }

    /**
     * Creates an {@link EmbeddedChannel} containing only the handler under test
     * and, when {@code protocol} is non-null, establishes the connection's
     * protocol the same way the real pipeline initializers do.
     */
    private static EmbeddedChannel newChannel(NettyHttpChannelConfig config, ProtocolName protocol, ProtocolSource source) {
        EmbeddedChannel embedded = new EmbeddedChannel(new LibertyHttpKeepAliveHandler(config));
        if (protocol != null) {
            ProtocolState.establish(embedded, protocol, source);
        }
        return embedded;
    }

    /** Creates a bare GET request with no Connection header. */
    private static HttpRequest newRequest(HttpVersion version) {
        return new DefaultHttpRequest(version, HttpMethod.GET, "/");
    }

    /** Creates a GET request asking to upgrade to WebSocket. */
    private static HttpRequest newUpgradeRequest() {
        HttpRequest request = newRequest(HttpVersion.HTTP_1_1);
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE);
        request.headers().set(HttpHeaderNames.UPGRADE, HttpHeaderValues.WEBSOCKET);
        return request;
    }

    /** Creates an empty response with a self-defined length ({@code Content-Length: 0}). */
    private static FullHttpResponse newResponse(HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
        return response;
    }

    /** Writes a response outbound through the handler and returns what reaches the wire. */
    private FullHttpResponse writeResponse(FullHttpResponse response) {
        channel.writeOutbound(response);
        channel.runPendingTasks();
        FullHttpResponse out = channel.readOutbound();
        assertNotNull("The handler must forward the response", out);
        return out;
    }

    /** Writes a new GET request through the handler and returns what comes out. */
    private HttpRequest sendRequest(HttpVersion version) {
        return writeAndRead(newRequest(version));
    }

    /** Writes the request inbound and reads it back after the handler has processed it. */
    private HttpRequest writeAndRead(HttpRequest request) {
        channel.writeInbound(request);
        HttpRequest out = channel.readInbound();
        assertNotNull("The handler must forward the request downstream", out);
        return out;
    }

    /** True when the request carries {@code Connection: close}. */
    private static boolean isConnectionClose(HttpRequest request) {
        String connection = request.headers().get(HttpHeaderNames.CONNECTION);
        return connection != null && HttpHeaderValues.CLOSE.toString().equalsIgnoreCase(connection);
    }

    /** The handler's per-channel request count, or null if it never counted. */
    private Integer requestCount() {
        return channel.attr(NettyHttpConstants.NUMBER_OF_HTTP_REQUESTS).get();
    }
}
