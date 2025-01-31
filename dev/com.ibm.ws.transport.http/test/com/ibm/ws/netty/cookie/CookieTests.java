/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.netty.cookie;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.spy;

import java.net.InetSocketAddress;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundServiceContextImpl;
import com.ibm.ws.http.netty.message.NettyRequestMessage;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;



public class CookieTests{

    private ChannelHandlerContext context;
    private Channel channel;
    private ChannelPipeline pipeline;
    private VirtualConnection virtualConnection;
    private HttpInboundServiceContextImpl spyInboundCtx;

    @Before
    public void setUp() {
        context = mock(ChannelHandlerContext.class);
        channel = mock(Channel.class);
        pipeline = mock(ChannelPipeline.class);

        // Stub so no null addresses
        when(context.channel()).thenReturn(channel);
        when(channel.pipeline()).thenReturn(pipeline);

        InetSocketAddress local = new InetSocketAddress("127.0.0.1", 1234);
        InetSocketAddress remote = new InetSocketAddress("192.168.1.100", 5678);
        when(channel.localAddress()).thenReturn(local);
        when(channel.remoteAddress()).thenReturn(remote);

        virtualConnection = mock(VirtualConnection.class);

        HttpInboundServiceContextImpl realImpl = new HttpInboundServiceContextImpl(context, virtualConnection);
        spyInboundCtx = spy(realImpl);
    }

    @Test
    public void testRequestCookiesInbound() {
    
        doReturn(true).when(spyInboundCtx).isInboundConnection();

        FullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test");
        req.headers().add("Cookie", "session=abc123; foo=bar");

        // Construct the NettyRequestMessage
        NettyRequestMessage message = new NettyRequestMessage(req, spyInboundCtx, context);

        // Verify cookie parsing
        HttpCookie session = message.getCookie("session");
        assertThat(session, notNullValue());
        assertThat(session.getValue(), is("abc123"));

        HttpCookie foo = message.getCookie("foo");
        assertThat(foo, notNullValue());
        assertThat(foo.getValue(), is("bar"));
    }

    @Test
    public void testInboundCookieWithDomainAndPathAttributes() {
        // 1) Mark this scenario as inbound
        doReturn(true).when(spyInboundCtx).isInboundConnection();

        // 2) Create a FullHttpRequest with the 'Cookie' header containing domain/path attributes
        //    e.g. "Cookie: name1=value1; Domain=myhost; Path=/servlet_jsh_cookie_web"
        FullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test");
        req.headers().add("Cookie", "name1=value1; $Version=1; $Domain=myhost; $Path=/servlet_jsh_cookie_web");

        // 3) Construct the NettyRequestMessage (which calls the base cookie logic)
        NettyRequestMessage message = new NettyRequestMessage(req, spyInboundCtx, context);

        // 4) Retrieve all cookies (or call getCookie if you prefer)
        List<HttpCookie> cookies = message.getAllCookies();
        // We expect only one cookie: "name1=value1". Domain and Path shouldn't spawn extra cookies.
        System.out.println(cookies);

        assertThat(cookies, hasSize(1));

        HttpCookie cookie = cookies.get(0);
        assertThat(cookie.getName(), is("name1"));
        assertThat(cookie.getValue(), is("value1"));
        assertThat(cookie.getDomain(), is("myhost"));
        assertThat(cookie.getPath(), is("/servlet_jsh_cookie_web"));

    }

}
