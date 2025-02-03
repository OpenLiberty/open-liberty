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

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundServiceContextImpl;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.ws.http.netty.message.NettyRequestMessage;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.genericbnf.HeaderField;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.channel.HttpServiceContext;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.ws.http.netty.message.NettyBaseMessage;
import com.ibm.ws.http.netty.message.NettyBaseMessage.MessageType;
import static com.ibm.ws.http.netty.message.NettyBaseMessage.MessageType.*;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
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

    private static HttpChannelConfig channelConfig;
    private static HttpServiceContext serviceContext;

    /**
     * Builds a testable message configured to be a request or response. It
     * is defaulted to being considered an inbound message. Tests that require
     * outbound are required to specifically configure the outbound flag.
     * This allows testing the functionality of cookie parsing
     * for {@link NettyBaseMessage}.
     * 
     * @param mode {@link MessageType} indicating if message is a request or response.
     * @return an initialized testable message object
     */
    private static TestableNettyMessage createMessage(MessageType type){
        HttpMessage message = null;
        if(type == REQUEST){
            message = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test");
        }else if(type == RESPONSE){
            message = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        }

        Objects.requireNonNull(message);

        TestableNettyMessage testMessage = new TestableNettyMessage();
        testMessage.testInit(message, serviceContext, channelConfig);
        testMessage.setMessageType(type);

        return testMessage;
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

    private static void setEE11Mode(boolean ee11) throws Exception{
        Field ee11Field = HttpDispatcher.class.getDeclaredField("isEE11");
        ee11Field.setAccessible(true);
        ee11Field.set(null, ee11);
    }

    public static class InboundTests{
        @Before
        public void setup(){
            CookieTests.commonSetup();
        }

        @Test
        public void testRequestCookiesInbound() {

            TestableNettyMessage message = createMessage(REQUEST);
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
            TestableNettyMessage message = createMessage(REQUEST);
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

    public static class ResponseTests{
        @Before
        public void setup(){
            CookieTests.commonSetup();
        }

        @Test
        public void testOutboundCookieMarshalling() {

            TestableNettyMessage message = createMessage(RESPONSE);

            HttpCookie cookie = new HttpCookie("testCookie", "val123");
            message.setCookie(cookie, HttpHeaderKeys.HDR_SET_COOKIE);
            message.processCookies();

            HttpHeaders headers  = message.getNettyHeaders();
            List<String> setCookies = headers.getAll("Set-Cookie");
            assertThat(setCookies, hasSize(1));
            assertThat(setCookies.get(0), containsString("testCookie=val123"));
        }

        @Test
        public void testRemoveCookieFromResponse(){
            TestableNettyMessage message = createMessage(RESPONSE);
            HttpCookie cookie = new HttpCookie("testCookie","testValue");
            message.setCookie(cookie, HttpHeaderKeys.HDR_SET_COOKIE);
            boolean removed = message.removeCookie("testCookie", HttpHeaderKeys.HDR_SET_COOKIE);
            assertThat(removed, is(true));
            message.processCookies();
            List<String> setCookies = message.getNettyHeaders().getAll("Set-Cookie");
            assertThat(setCookies, is(empty()));
        }

        @Test
        public void testSetCookieAfterCommitedResponse(){
            TestableNettyMessage message = createMessage(RESPONSE);
            message.setCommitted();
            HttpCookie cookie = new HttpCookie("testCookie", "testValue");
            boolean result = message.setCookie(cookie, HttpHeaderKeys.HDR_SET_COOKIE);
            assertThat(result, is(false));
        }

        @Test
        public void testSetCookieHeaderAndCookieObject(){
            TestableNettyMessage message = createMessage(RESPONSE);
            HttpCookie cookie = new HttpCookie("cookieObject", "cookieValue");
            boolean setResult = message.setCookie(cookie, HttpHeaderKeys.HDR_SET_COOKIE);
            assertThat(setResult, is(true));

            message.appendHeader("Set-Cookie", "cookieHeader=cookieHeaderValue");
            message.processCookies();

            List<HttpCookie> cookies = message.getAllCookies();
            for(HttpCookie c :cookies){
                System.out.println(c.getName()+":"+c.getValue());
            }
            assertThat(cookies, hasSize(2));

            HeaderField header = message.getHeader("Set-Cookie");
            String value = Objects.nonNull(header)? header.asString():"";
            System.out.println(value);
        }
    }

        

    public static class EdgeCaseTests{

        @Before
        public void setup(){
            CookieTests.commonSetup();
        }

        @Test 
        public void testsMalformedCookieNoName(){
            TestableNettyMessage message = createMessage(REQUEST);
            message.getNettyHeaders().add("Cookie", "=badNameWithValue");
            List<HttpCookie> cookies = message.getAllCookies();
            assertThat(cookies, is(empty()));
        }

        @Test 
        public void testEmptyCookieValue(){
            TestableNettyMessage message = createMessage(REQUEST);
            message.getNettyHeaders().add("Cookie", "emptyCookie=");
            HttpCookie cookie = message.getCookie("emptyCookie");
            assertThat(cookie, notNullValue());
            assertThat(cookie.getValue(), is(""));

            byte[] valueBytes = message.getCookieValue("emptyCookie");
            assertNull(valueBytes);

        }

        @Test 
        public void testQuotedCookieValues(){
            TestableNettyMessage message = createMessage(REQUEST);
            message.getNettyHeaders().add("Cookie", "quotedCookie=\"quotedValue\"");
            HttpCookie cookie = message.getCookie("quotedCookie");
            assertThat(cookie, notNullValue());
            assertThat(cookie.getValue(), is("quotedValue"));
        }

        @Test 
        public void testQuotedCookieValuesNonEE11() throws Exception{
            
            setEE11Mode(false);

            TestableNettyMessage message = createMessage(REQUEST);
            message.getNettyHeaders().add("Cookie", "quotedCookie=\"quotedValue\"");
            HttpCookie cookie = message.getCookie("quotedCookie");
            assertThat("Non-EE11: Cookie should be parsed", cookie, notNullValue());
            assertThat("Non-EE11: Cookie value should have quotes removed", cookie.getValue(), is("quotedValue"));
            
       }

       @Test
        public void testQuotedCookieValuesEE11() throws Exception{
            setEE11Mode(true);

            TestableNettyMessage message = createMessage(REQUEST);
            message.getNettyHeaders().add("Cookie", "quotedCookie=\"quotedValue\"");
            HttpCookie cookie = message.getCookie("quotedCookie");
            assertThat(cookie, notNullValue());
            assertThat(cookie.getValue(), is("\"quotedValue\""));
        }

        @Test 
        public void testNonAsciiCookieName(){
            TestableNettyMessage message = createMessage(REQUEST);
            message.getNettyHeaders().add("Cookie", "Mayagüez:IsInvalidName");
            HttpCookie cookie = message.getCookie("Mayagüez");
            assertThat(cookie, is(nullValue()));
        }

        @Test
        public void testNonAsciiCookieValue(){
            // This test verifies that cookie values are decoded using the legacy
            // conversion (which effectively treats the bytes as ISO-8859-1) rather than using UTF-8.
            // As a result, a value that should be "Mayagüez" when properly decoded in UTF-8 is instead
            // decoded as "MayagÃ¼ez".
            TestableNettyMessage message = createMessage(REQUEST);
            message.getNettyHeaders().add("Cookie", "ISO88591=Mayagüez");
            HttpCookie cookie = message.getCookie("ISO88591");
            assertThat(cookie, notNullValue());
            assertThat(cookie.getValue(), is("MayagÃ¼ez"));
        }

        @Test
        public void testTrailingDelimiterCookieHeader(){
            TestableNettyMessage message = createMessage(REQUEST);
            message.getNettyHeaders().add("Cookie", "trailing=value;");
            List<HttpCookie> cookies = message.getAllCookies();
            assertThat(cookies, hasSize(1));
            HttpCookie cookie = cookies.get(0);
            assertThat(cookie.getName(), is("trailing"));
            assertThat(cookie.getValue(), is("value"));
        }

        @Test
        public void testDuplicateCookieNameSingleHeader(){
            TestableNettyMessage message = createMessage(REQUEST);
            message.getNettyHeaders().add("Cookie", "duplicate=cookie1; duplicate=cookie2");
            List<HttpCookie> cookies = message.getAllCookies("duplicate");
            assertThat(cookies, hasSize(2));
            assertThat(cookies.get(0).getValue(), is("cookie1"));
            assertThat(cookies.get(1).getValue(), is("cookie2"));
        }

        @Test 
        public void testCookieWithInternalExtraWhitespace() {
            TestableNettyMessage message = createMessage(REQUEST);
            message.getNettyHeaders().add("Cookie", "cookie  =  valid   value");
            HttpCookie cookie = message.getCookie("cookie");
            assertThat(cookie, notNullValue());
            assertThat(cookie.getValue(), is("valid   value"));
        }

        @Test
        public void testCookieWithMultipleEquals(){
            TestableNettyMessage message = createMessage(REQUEST);
            message.getNettyHeaders().add("Cookie", "twoEquals=first=second");
            HttpCookie cookie = message.getCookie("twoEquals");
            assertThat(cookie, notNullValue());
            assertThat(cookie.getValue(), is("first=second"));
        }

        @Test(expected = IllegalArgumentException.class)
        public void testEmptyCookieHeader(){
            TestableNettyMessage message = createMessage(REQUEST);
            message.getNettyHeaders().add("Cookie", "   ");
            //TODO: Check Legacy parsing to see if we accept empty cookie headers

        }

        @Test 
        public void testCookieHeaderStartsWithVersion(){
            TestableNettyMessage message = createMessage(REQUEST);
            message.getNettyHeaders().add("Cookie", "$Version=1; cookie=value; $Path=/");
            List<HttpCookie> cookies = message.getAllCookies();
            assertThat(cookies, hasSize(1));
            HttpCookie cookie = cookies.get(0);
            assertThat(cookie.getValue(), is("value"));
            assertThat(cookie.getVersion(), is(1));
            assertThat(cookie.getPath(), is("/"));
        }

        @Test 
        public void testCookieWithoutEquals(){
            TestableNettyMessage message = createMessage(REQUEST);
            message.getNettyHeaders().add("Cookie", "noEquals");
            HttpCookie cookie = message.getCookie("noEquals");
            assertThat(cookie.getName(), is("noEquals"));

            //NOTE: RFC 6265, a cookie header is expected to consist of one or more cookie-pairs, 
            //      and each cookie-pair is defined as a cookie-name followed by an "=" and then 
            //      a cookie-value. In legacy we are more lenient and will still create the cookie.
        }
  
        @Test
        public void testTrailingCommaCookieHeader(){
            TestableNettyMessage message = createMessage(REQUEST);
            message.getNettyHeaders().add("Cookie", "comma=value,");
            List<HttpCookie> cookies = message.getAllCookies();
            assertThat(cookies, hasSize(1));
            HttpCookie cookie = cookies.get(0);
            assertThat(cookie.getName(), is("comma"));
            assertThat(cookie.getValue(), is("value"));
        }

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
