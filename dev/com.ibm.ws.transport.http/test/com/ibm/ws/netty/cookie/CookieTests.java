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
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundServiceContextImpl;
import com.ibm.ws.http.netty.message.NettyRequestMessage;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.channel.HttpServiceContext;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.ws.http.netty.message.NettyBaseMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import com.ibm.wsspi.http.channel.HttpServiceContext;

/**
 * Provides a series of tests verifying the cookie parsing (inbound)
 * and marshalling (outbound) functionalities of the {@link NettyBaseMessage}.
 * It uses a {@link TestableNettyMessage}, in place of the formal
 * {@code NettyRequestMessage} or {@code NettyResponseMessage}, in order
 * to minimize dependencies and amount of required mocked objects.
 */
@RunWith(Enclosed.class)
public class CookieTests {

    /**
     * Indicates whether the message should treate cookies as inbound
     * (parse "Cookie") or outbound (write "Set-Cookie").
     */
    private enum Mode{
        INBOUND, OUTBOUND
    }
    private static HttpChannelConfig channelConfig;
    private static HttpServiceContext serviceContext;

    /**
     * Builds a testable message configured to be either inbound or outbound. 
     * The direction flow allows testing the functionality of cookie parsing
     * for {@link NettyBaseMessage}.
     * 
     * @param inbound {@link Mode} indicating if message is inbound or outbound
     * @return an initialized testable message object
     */
    private static TestableNettyMessage createMessage(Mode mode){
        DefaultHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test");

        TestableNettyMessage message = new TestableNettyMessage();
        message.incoming(Mode.INBOUND.equals(mode)? true:false);
        message.testInit(request, serviceContext, channelConfig);

        return message;
    }

    /**
     * Configures the mocked configurations and objects that will be
     * used and shared by the test methods. The {@code NettyBaseMessage.init(...)}
     * makes use of these objects for each tested scenario.
     * 
     * Any additional required mocked behavior should be provided by the specific
     * test requiring it.
     */
    public static void commonSetup(){
        channelConfig = mock(HttpChannelConfig.class);
        when(channelConfig.getLimitOnNumberOfHeaders()).thenReturn(100);
        when(channelConfig.getLimitOfFieldSize()) .thenReturn(1024);

        serviceContext = mock(HttpServiceContext.class);
    }

    public static class InboundTests{
        Mode mode = Mode.INBOUND;
        @Before
        public void setup(){
            CookieTests.commonSetup();
        }

        @Test
        public void testRequestCookiesInbound() {

            TestableNettyMessage message = createMessage(mode);
            message.getNettyHeaders().add("Cookie", "session=abc123; foo=bar");

            HttpCookie session = message.getCookie("session"); 
            assertThat(session, notNullValue());
            assertThat(session.getValue(), is("abc123"));

            HttpCookie foo = message.getCookie("foo");   
            assertThat(foo, notNullValue());
            assertThat(foo.getValue(), is("bar"));
        }

        @Test
        public void testInboundCookieWithDomainAndPath() {
            TestableNettyMessage message = createMessage(mode);
            message.getNettyHeaders().add("Cookie", "name1=value1; $Version=1; $Domain=myhost; $Path=/servlet_jsh_cookie_web");
            List<HttpCookie> cookies = message.getAllCookies();

            assertThat(cookies, hasSize(1));
            HttpCookie cookie = cookies.get(0);
            assertThat(cookie.getName(), is("name1"));
            assertThat(cookie.getValue(),  is("value1"));
            assertThat(cookie.getDomain(), is("myhost"));
            assertThat(cookie.getPath(), is("/servlet_jsh_cookie_web"));
    
        }


    }

    public static class OutboundTests{
        Mode mode = Mode.OUTBOUND;
        @Before
        public void setup(){
            CookieTests.commonSetup();
        }

        @Test
        public void testOutboundCookieMarshalling() {

            TestableNettyMessage message = createMessage(mode);

            HttpCookie cookie = new HttpCookie("testCookie", "val123");
            message.setCookie(cookie, HttpHeaderKeys.HDR_SET_COOKIE);
            message.processCookies();

            HttpHeaders headers  = message.getNettyHeaders();
            List<String> setCookies = headers.getAll("Set-Cookie");
            assertThat(setCookies, hasSize(1));
            assertThat(setCookies.get(0), containsString("testCookie=val123"));
        }

    }

    public static class EdgeCaseTests{

        @Before
        public void setup(){
            CookieTests.commonSetup();
        }

        @Test 
        public void testsMalformedCookieNoName(){
            TestableNettyMessage message = createMessage(Mode.INBOUND);
            message.getNettyHeaders().add("Cookie", "=badNameWithValue");
            List<HttpCookie> cookies = message.getAllCookies();
            assertThat(cookies, is(empty()));
        }

        //Backward compatibility
        //utf-8 values
        //case sensitivity
        //trailing comma-semicolon
        //removing cookie
        //duplicate cookie
        //quoted values

    }

    /**
     * Minimal testing class for {@link NettyBaseMessage} that provides the following:
     *  -> A toggle for {@code incoming(boolean)} to set inbound or outbound mode, to
     *     test different cookie handling paths.
     *  -> A {@code testInit(...)} method to invoke the base class initialization with 
     *     minimal arguments.
     * 
     * For ease, this class also provides a {@code getNettyHeaders()} for direct inspection
     * of the underlying header lines that would be passed on to the pipeline or container.
     */
    private static class TestableNettyMessage extends NettyBaseMessage {

        public void testInit(HttpMessage message, HttpServiceContext context, HttpChannelConfig config) {
            super.init(message, context, config);
        }

        public HttpHeaders getNettyHeaders() {
            return this.headers;
        }
    }

}
