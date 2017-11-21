/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.test.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.channelfw.internal.ChannelDataImpl;
import com.ibm.ws.channelfw.internal.InboundVirtualConnectionFactoryImpl;
import com.ibm.ws.genericbnf.internal.GenericUtils;
import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpRequestMessageImpl;
import com.ibm.ws.http.channel.test.api.testobjects.MockOutboundSC;
import com.ibm.ws.http.channel.test.api.testobjects.MockRequestMessage;
import com.ibm.ws.http.internal.HttpDateFormatImpl;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;
import com.ibm.wsspi.bytebuffer.WsByteBufferUtils;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.genericbnf.GenericKeys;
import com.ibm.wsspi.genericbnf.HeaderField;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.HttpDateFormat;
import com.ibm.wsspi.http.channel.values.ConnectionValues;
import com.ibm.wsspi.http.channel.values.ContentEncodingValues;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.http.channel.values.MethodValues;
import com.ibm.wsspi.http.channel.values.SchemeValues;
import com.ibm.wsspi.http.channel.values.TransferEncodingValues;
import com.ibm.wsspi.http.channel.values.VersionValues;

/**
 * Class to test all of the methods from HttpRequestMessage down to BNFHeaders.
 */
public class HttpRequestMessageImplTest {
    private static SharedOutputManager outputMgr;

    /** The local reference to test objects. */
    private HttpRequestMessageImpl request = null;

    /**
     * Capture stdout/stderr output to the manager.
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    /**
     * Final teardown work when class is exiting.
     * 
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    /**
     * Individual teardown after each test.
     * 
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation
        outputMgr.resetStreams();
        if (null != getRequest()) {
            getRequest().destroy();
            this.request = null;
        }
    }

    /**
     * Used to set up the test. This method is called by JUnit before each of
     * the tests are executed.
     */
    @Before
    public void setUp() {
        HttpChannelConfig cfg = new HttpChannelConfig(
                        new ChannelDataImpl("HTTP", null,
                                        new HashMap<Object, Object>(), 10,
                                        ChannelFrameworkFactory.getChannelFramework()));
        MockOutboundSC sc = new MockOutboundSC(
                        new InboundVirtualConnectionFactoryImpl().createConnection(), cfg);
        this.request = new MockRequestMessage(sc);
    }

    protected WsByteBuffer[] duplicateBuffers(WsByteBuffer[] list) {
        if (null == list) {
            return null;
        }
        WsByteBuffer[] output = new WsByteBuffer[list.length];
        for (int i = 0; i < list.length; i++) {
            output[i] = list[i].duplicate();
        }
        return output;
    }

    /**
     * Get access to the request message.
     * 
     * @return HttpRequestMessageImpl
     */
    private HttpRequestMessageImpl getRequest() {
        return this.request;
    }

    /**
     * Test various portions of the request messages.
     */
    @Test
    public void testMain() {
        try {
            HttpDateFormat df = new HttpDateFormatImpl();
            // *****************************************************************
            // Test basic header APIs
            // *****************************************************************

            // @ Tested API - setHeader(String, String)
            // @ Tested API - containsHeader(String)
            getRequest().setHeader("TestSetHeader", "TestValue1");
            assertTrue(getRequest().containsHeader("TestSetHeader"));

            // @ Tested API - setHeader(HeaderKeys, String)
            // @ Tested API - containsHeader(HeaderKeys)
            getRequest().clear();
            getRequest().setHeader(HttpHeaderKeys.HDR_CONNECTION, "close");
            assertTrue(getRequest().containsHeader(HttpHeaderKeys.HDR_CONNECTION));

            // @ Tested API - setHeader(String, byte[])
            // @ Tested API - getHeaderAsString(HeaderKeys)
            getRequest().clear();
            getRequest().setHeader(HttpHeaderKeys.HDR_CONNECTION, "close".getBytes());
            String val = getRequest().getHeader(HttpHeaderKeys.HDR_CONNECTION).asString();
            assertEquals(val, "close");

            // @ Tested API - setHeader(String, byte[])
            // @ Tested API - getHeaderAsString(String)
            getRequest().clear();
            getRequest().setHeader("TestSetHeader", "TestValue1".getBytes());
            val = getRequest().getHeader("TestSetHeader").asString();
            assertEquals(val, "TestValue1");

            // @ Tested API - appendHeader(HeaderKeys, byte[])
            // @ Tested API - getHeaderAsByteArray(HeaderKeys)
            getRequest().clear();
            getRequest().appendHeader(HttpHeaderKeys.HDR_ACCEPT, "AcceptValue".getBytes());
            byte[] hdr = getRequest().getHeader(HttpHeaderKeys.HDR_ACCEPT).asBytes();
            byte[] acceptBytes = "AcceptValue".getBytes();
            assertEquals(hdr.length, acceptBytes.length);
            for (int i = 0; i < hdr.length; i++) {
                if (hdr[i] != acceptBytes[i]) {
                    fail();
                }
            }

            // @ Tested API - appendHeader(String, byte[])
            // @ Tested API - getHeaderAsByteArray(String)
            getRequest().clear();
            getRequest().appendHeader("Accept", "AcceptValue".getBytes());
            hdr = getRequest().getHeader("Accept").asBytes();
            assertEquals(hdr.length, acceptBytes.length);
            for (int i = 0; i < hdr.length; i++) {
                if (hdr[i] != acceptBytes[i]) {
                    fail();
                }
            }

            // @ Tested API - appendHeader(String, String)
            // @ Tested API - getHeaderValues(HeaderKeys)
            getRequest().clear();
            getRequest().appendHeader("Accept", "AcceptValue");
            List<HeaderField> hdrValues = getRequest().getHeaders(HttpHeaderKeys.HDR_ACCEPT);
            assertEquals(1, hdrValues.size());
            assertEquals("AcceptValue", hdrValues.get(0).asString());

            // @ Tested API - appendHeader(HeaderKeys, String)
            // @ Tested API - getHeaderValues(String)
            getRequest().clear();
            getRequest().appendHeader(HttpHeaderKeys.HDR_ACCEPT, "AcceptValue");
            hdrValues = getRequest().getHeaders("Accept");
            assertEquals(1, hdrValues.size());
            assertEquals("AcceptValue", hdrValues.get(0).asString());

            // @ Tested API - instancesOfHeader(HeaderKeys)
            // @ Tested API - instancesOfHeader(String)
            // @ Tested API - appendHeader(HeaderKeys, String)
            getRequest().clear();
            getRequest().appendHeader(HttpHeaderKeys.HDR_ACCEPT, "AcceptValue");
            getRequest().appendHeader(HttpHeaderKeys.HDR_ACCEPT, "AcceptValue2");
            assertEquals(getRequest().getNumberOfHeaderInstances(HttpHeaderKeys.HDR_ACCEPT), 2);
            assertEquals(getRequest().getNumberOfHeaderInstances("Accept"), 2);

            // @ Tested API - appendHeader(HeaderKeys, String)
            // @ Tested API - getAllHeaders()
            getRequest().clear();
            getRequest().appendHeader(HttpHeaderKeys.HDR_ACCEPT, "AcceptValue");
            List<HeaderField> hdrs = getRequest().getAllHeaders();
            assertEquals(1, hdrs.size());

            // @ Tested API - removeHeader(HeaderKeys)
            // @ Tested API - removeHeader(String)
            getRequest().clear();
            getRequest().appendHeader(HttpHeaderKeys.HDR_ACCEPT, "AcceptValue");
            getRequest().appendHeader("Test", "TestValue");
            getRequest().removeHeader(HttpHeaderKeys.HDR_ACCEPT);
            getRequest().removeHeader("Test");
            assertEquals(0, getRequest().getAllHeaders().size());

            // @ Tested API - removeHeader(HeaderKeys, int)
            // @ Tested API - removeHeader(String, int)
            getRequest().clear();
            getRequest().appendHeader(HttpHeaderKeys.HDR_ACCEPT, "AcceptValue");
            getRequest().appendHeader(HttpHeaderKeys.HDR_ACCEPT, "Accept2");
            getRequest().appendHeader("Test", "TestValue");
            getRequest().removeHeader(HttpHeaderKeys.HDR_ACCEPT, 0);
            getRequest().removeHeader("Test", 0);
            assertEquals(1, getRequest().getNumberOfHeaderInstances(
                                                                    HttpHeaderKeys.HDR_ACCEPT));
            assertEquals(0, getRequest().getNumberOfHeaderInstances("Test"));

            // @ Tested API ... saving unknown headers and pulling with different
            // case
            getRequest().clear();
            String value = "testing";
            getRequest().setHeader("myIntHeader", value);
            assertTrue(getRequest().containsHeader("MyINTHeader".getBytes()));
            assertEquals(value, getRequest().getHeader("myIntHeader").asString());
            assertEquals(value, getRequest().getHeader("MYINTHEADER").asString());
            assertEquals(value, getRequest().getHeader("myINTHeaDeR").asString());

            // *****************************************************************
            // Test the Cookie specific APIs
            // *****************************************************************

            // @Tested API - setCookie(String, String, HeaderKeys);
            // @Tested API - getCookie(String)
            getRequest().clear();
            getRequest().setCookie("TestCookie", "TestCookieValue", HttpHeaderKeys.HDR_COOKIE);
            getRequest().setHeader(HttpHeaderKeys.HDR_COOKIE2, "TestCookie1=TestCookieValue1");
            HttpCookie cookie1 = getRequest().getCookie("TestCookie");
            HttpCookie cookie2 = getRequest().getCookie("TestCookie1");
            assertNotNull(cookie1);
            assertNotNull(cookie2);
            assertEquals(cookie1.getName(), "TestCookie");
            assertEquals(cookie1.getValue(), "TestCookieValue");
            assertEquals(cookie2.getName(), "TestCookie1");
            assertEquals(cookie2.getValue(), "TestCookieValue1");

            // Test looking for a non-existant cookie
            HttpCookie cookie = getRequest().getCookie("nothere");
            assertNull(cookie);

            // @Tested API- setCookie(Cookie, HeaderKeys);
            getRequest().clear();
            cookie = new HttpCookie("TestCookie", "TestCookieValue");
            getRequest().setCookie(cookie, HttpHeaderKeys.HDR_COOKIE);
            cookie = getRequest().getCookie("TestCookie");
            assertNotNull(cookie);
            assertEquals(cookie.getName(), "TestCookie");
            assertEquals(cookie.getValue(), "TestCookieValue");

            // Add a list of cookie objects
            // @ Tested API - removeCookie(String, HeaderKeys)
            // @ Tested API - getAllCookies()
            getRequest().clear();
            assertEquals(0, getRequest().getAllCookies().size());
            getRequest().setCookie(new HttpCookie("TestCookie1", "TestCookieValue1"),
                                   HttpHeaderKeys.HDR_COOKIE);
            getRequest().setCookie("TestCookie2", "TestCookieValue2",
                                   HttpHeaderKeys.HDR_COOKIE);
            getRequest().setCookie(new HttpCookie("TestCookie3", "TestCookieValue3"),
                                   HttpHeaderKeys.HDR_COOKIE2);
            List<HttpCookie> cookieList = getRequest().getAllCookies();
            assertNotNull(cookieList);
            assertEquals(3, cookieList.size());
            assertTrue(getRequest().removeCookie("TestCookie1", HttpHeaderKeys.HDR_COOKIE));
            assertTrue(getRequest().removeCookie("TestCookie3", HttpHeaderKeys.HDR_COOKIE2));
            cookieList = getRequest().getAllCookies();
            assertNotNull(cookieList);
            assertEquals(1, cookieList.size());

            // Test setting the header directly

            getRequest().clear();
            getRequest().setHeader(HttpHeaderKeys.HDR_COOKIE,
                                   "TestCookie1=TestCookieValue1;"
                                                   + "TestCookie2=TestCookieValue2;"
                                                   + "TestCookie3=TestCookieValue3;"
                                                   + "TestCookie4=TestCookieValue4");
            cookieList = getRequest().getAllCookies();
            assertNotNull(cookieList);
            assertEquals(4, cookieList.size());

            // Test marshalling cookies

            getRequest().clear();
            cookie = new HttpCookie("TestCookie1", "TestCookieValue1");
            cookie.setPath("www.ibm.com");
            cookie.setVersion(1);
            getRequest().setCookie(cookie, HttpHeaderKeys.HDR_COOKIE);
            getRequest().setCookie("TestCookie2", "TestCookieValue2",
                                   HttpHeaderKeys.HDR_COOKIE);
            getRequest().setCookie(new HttpCookie("TestCookie3", "TestCookieValue3"),
                                   HttpHeaderKeys.HDR_COOKIE2);
            getRequest().setMethod("GET");
            getRequest().setRequestURL("/");

            // serialize the cookies
            // @Tested API preMarshallMessage()
            WsByteBuffer[] marshalledMessage = duplicateBuffers(getRequest().marshallMessage());

            // deserialize the cookies
            getRequest().clear();
            if (null != marshalledMessage) {
                for (int i = 0; i < marshalledMessage.length; i++) {
                    if (getRequest().parseMessage(marshalledMessage[i], true)) {
                        break;
                    }
                }
            }

            cookieList = getRequest().getAllCookies();
            assertNotNull(cookieList);
            assertEquals(3, cookieList.size());

            // Test error conditions now
            assertFalse(getRequest().setCookie(null, HttpHeaderKeys.HDR_COOKIE));
            assertFalse(getRequest().setCookie(null, null, null));
            assertFalse(getRequest().removeCookie("TestCookie", null));
            assertFalse(getRequest().containsCookie(null, null));
            assertNull(getRequest().getCookie(null));

            // @Tested API ... getCookieValue
            getRequest().clear();
            getRequest().setHeader(HttpHeaderKeys.HDR_COOKIE, "myCookie=myValue");
            assertEquals(new String(getRequest().getCookieValue("myCookie")), "myValue");

            // test case-insensitive failing
            getRequest().setHeader(HttpHeaderKeys.HDR_COOKIE, "myCookie=myValue");
            assertNull(getRequest().getCookieValue("MYCOOKIE"));

            // test a non-existant cookie name
            assertNull(getRequest().getCookieValue("myCookie2"));

            // test a non-existent cookie value
            getRequest().setHeader(HttpHeaderKeys.HDR_COOKIE, "myCookie=myValue; n2");
            assertNull(getRequest().getCookieValue("n2"));

            // test an invalid [name=] cookie
            getRequest().setHeader(HttpHeaderKeys.HDR_COOKIE, "myCookie=myValue; n2=");
            assertNull(getRequest().getCookieValue("n2"));

            // test adding whitespace to the end of that invalid string
            getRequest().setHeader(HttpHeaderKeys.HDR_COOKIE, "myCookie=myValue; n2= ");
            assertNull(getRequest().getCookieValue("n2"));

            // verify the searching of just the names works
            getRequest().setHeader(HttpHeaderKeys.HDR_COOKIE, "myCookie=myValue; n2= ");
            assertNull(getRequest().getCookieValue("myValue"));

            // test a partial match
            assertNull(getRequest().getCookieValue("myCook"));

            // test a second cookie
            getRequest().setHeader(HttpHeaderKeys.HDR_COOKIE, "myCookie=myValue; n2=test");
            assertEquals("test", new String(getRequest().getCookieValue("n2")));
            assertEquals("myValue", new String(getRequest().getCookieValue("myCookie")));

            // test with comma
            getRequest().setHeader(HttpHeaderKeys.HDR_COOKIE, "myCookie=myValue, n2=test");
            assertEquals("test", new String(getRequest().getCookieValue("n2")));
            assertEquals("myValue", new String(getRequest().getCookieValue("myCookie")));

            // test trimming out whitespace around a value
            getRequest().setHeader(HttpHeaderKeys.HDR_COOKIE, "myCookie=  myValue  ; n2=test");
            assertEquals("myValue", new String(getRequest().getCookieValue("myCookie")));
            assertEquals("test", new String(getRequest().getCookieValue("n2")));

            // test trimming out whitespace around a value (with comma)
            getRequest().setHeader(HttpHeaderKeys.HDR_COOKIE, "myCookie=  myValue  , n2=test");
            assertEquals("myValue", new String(getRequest().getCookieValue("myCookie")));
            assertEquals("test", new String(getRequest().getCookieValue("n2")));

            // test skipping past other cookie parameters to find the 2nd cookie
            getRequest().setHeader(HttpHeaderKeys.HDR_COOKIE, "myCookie=myValue; path=/; $version=1; n2=value");
            assertEquals("myValue", new String(getRequest().getCookieValue("myCookie")));
            assertEquals("value", new String(getRequest().getCookieValue("n2")));

            // test skipping past other cookie parameters to find the 2nd cookie (with comma)
            getRequest().setHeader(HttpHeaderKeys.HDR_COOKIE, "myCookie=myValue; path=/; $version=1, n2=value");
            assertEquals("myValue", new String(getRequest().getCookieValue("myCookie")));
            assertEquals("value", new String(getRequest().getCookieValue("n2")));

            // test when the header itself doesn't exist
            getRequest().removeHeader(HttpHeaderKeys.HDR_COOKIE);
            assertNull(getRequest().getCookieValue("n2"));

            // test when multiple header instances exist
            getRequest().setHeader(HttpHeaderKeys.HDR_COOKIE, "myCookie=myValue; n2=test");
            getRequest().appendHeader(HttpHeaderKeys.HDR_COOKIE, "myCookie2=value2; path=/; n3=n3value");
            assertEquals("value2", new String(getRequest().getCookieValue("myCookie2")));
            assertEquals("n3value", new String(getRequest().getCookieValue("n3")));

            // test when multiple header instances exist (with comma)
            getRequest().setHeader(HttpHeaderKeys.HDR_COOKIE, "myCookie=myValue, n2=test");
            getRequest().appendHeader(HttpHeaderKeys.HDR_COOKIE, "myCookie2=value2; path=/, n3=n3value");
            assertEquals("value2", new String(getRequest().getCookieValue("myCookie2")));
            assertEquals("n3value", new String(getRequest().getCookieValue("n3")));

            // test quoted cookie name
            getRequest().setHeader(HttpHeaderKeys.HDR_COOKIE, "myCookie=myValue; \"n2\"=test");
            assertEquals("test", new String(getRequest().getCookieValue("n2")));

            // test quoted cookie name (with comma)
            getRequest().setHeader(HttpHeaderKeys.HDR_COOKIE, "myCookie=myValue, \"n2\"=test");
            assertEquals("test", new String(getRequest().getCookieValue("n2")));

            // test quoted cookie value
            getRequest().setHeader(HttpHeaderKeys.HDR_COOKIE, "myCookie=\"myValue\"; n2=test");
            assertEquals("\"myValue\"", new String(getRequest().getCookieValue("myCookie")));

            // test quoted cookie value (with comma)
            getRequest().setHeader(HttpHeaderKeys.HDR_COOKIE, "myCookie=\"myValue\", n2=test");
            assertEquals("\"myValue\"", new String(getRequest().getCookieValue("myCookie")));

            // test searching with a quoted cookie name
            assertEquals("\"myValue\"", new String(getRequest().getCookieValue("\"myCookie\"")));

            // test invalid quoted cookie name
            getRequest().setHeader(HttpHeaderKeys.HDR_COOKIE, "myCookie=myValue; \"n2=test; in valid=x");
            assertNull(getRequest().getCookieValue("n2"));

            // test invalid quoted cookie name (with comma)
            getRequest().setHeader(HttpHeaderKeys.HDR_COOKIE, "myCookie=myValue; \"n2=test, in valid=x");
            assertNull(getRequest().getCookieValue("n2"));

            // test cookie names with invalid whitespace
            getRequest().setHeader(HttpHeaderKeys.HDR_COOKIE, "in valid=x");
            assertNull(getRequest().getCookieValue("in"));

            // test invalid quoted cookie value
            getRequest().setHeader(HttpHeaderKeys.HDR_COOKIE, "myCookie=\"myValue; n2=test a");
            getRequest().appendHeader(HttpHeaderKeys.HDR_COOKIE, "cookie2=value2  ");
            assertEquals("\"myValue", new String(getRequest().getCookieValue("myCookie")));
            assertEquals("test a", new String(getRequest().getCookieValue("n2")));

            // test invalid quoted cookie value (with comma)
            getRequest().setHeader(HttpHeaderKeys.HDR_COOKIE, "myCookie=\"myValue, n2=test a");
            getRequest().appendHeader(HttpHeaderKeys.HDR_COOKIE, "cookie2=value2  ");
            assertEquals("\"myValue", new String(getRequest().getCookieValue("myCookie")));
            assertEquals("test a", new String(getRequest().getCookieValue("n2")));

            // test trailing white space after a cookie value
            assertEquals("value2", new String(getRequest().getCookieValue("cookie2")));

            // Test the getAllCookie by name apis
            // @Tested API - getAllCookieValues(String)
            // @Tested API - getAllCookies(String)

            getRequest().clear();
            getRequest().setHeader("Cookie", "jsessionid=s1;bogus=test;jsessionid=s2");
            getRequest().setHeader("Cookie2", "bogus2=test2;jsessionid=s3");
            List<String> cvalues = getRequest().getAllCookieValues("jsessionid");
            assertEquals(3, cvalues.size());
            assertEquals("s1", cvalues.get(0));
            assertEquals("s2", cvalues.get(1));
            assertEquals("s3", cvalues.get(2));

            getRequest().clear();
            getRequest().setHeader("Cookie", "ws=this;id=that");
            getRequest().setHeader("Cookie2", "ignore=me;id=something;id=otherthing");
            List<HttpCookie> cookies = getRequest().getAllCookies("id");
            assertEquals(3, cookies.size());
            assertEquals("that", cookies.get(0).getValue());
            assertEquals("something", cookies.get(1).getValue());
            assertEquals("otherthing", cookies.get(2).getValue());

            // *****************************************************************
            // Test the request method APIs
            // *****************************************************************

            // @ Tested API - setMethod(String)
            // @ Tested API - setMethod(byte[])
            // @ Tested API - setMethod(MethodValues)
            // @ Tested API - getMethod()
            // @ Tested API - getMethodValue()
            getRequest().clear();
            getRequest().setMethod("GET");
            assertEquals("GET", getRequest().getMethod());
            getRequest().setMethod("POST".getBytes());
            assertEquals("POST", getRequest().getMethod());
            getRequest().setMethod(MethodValues.CONNECT);
            assertEquals(MethodValues.CONNECT, getRequest().getMethodValue());

            // *****************************************************************
            // Test the Connection header APIs
            // *****************************************************************

            // @ Tested API - setConnection(ConnectionValues)
            // @ Tested API - setConnection(ConnectionValues[])
            // @ Tested API - getConnection()
            getRequest().clear();
            getRequest().setConnection(ConnectionValues.CLOSE);
            ConnectionValues[] connValues = getRequest().getConnection();
            assertTrue(connValues[0].equals(ConnectionValues.CLOSE));
            ConnectionValues[] connList = new ConnectionValues[] {
                                                                  ConnectionValues.TE,
                                                                  ConnectionValues.KEEPALIVE };
            getRequest().setConnection(connList);
            connValues = getRequest().getConnection();
            assertEquals(connList.length, connValues.length);
            for (int i = 0; i < connList.length; i++) {
                assertTrue(connList[i].equals(connValues[i]));
            }

            // *****************************************************************
            // Test the ContentEncoding header APIs
            // *****************************************************************

            // @ Tested API - setContentEncoding(ContentEncodingValues)
            // @ Tested API - setContentEncoding(ContentEncodingValues[])
            // @ Tested API - getContentEncoding()
            getRequest().clear();
            getRequest().setContentEncoding(ContentEncodingValues.GZIP);
            ContentEncodingValues[] ceList = getRequest().getContentEncoding();
            assertEquals(ceList.length, 1);
            assertTrue(ceList[0].equals(ContentEncodingValues.GZIP));
            ceList = new ContentEncodingValues[] {
                                                  ContentEncodingValues.COMPRESS,
                                                  ContentEncodingValues.XGZIP };
            getRequest().setContentEncoding(ceList);
            ContentEncodingValues[] ceValues = getRequest().getContentEncoding();
            assertEquals(ceList.length, ceValues.length);
            for (int i = 0; i < ceList.length; i++) {
                assertTrue(ceList[i].equals(ceValues[i]));
            }

            // Test passing in empty lists explicitly
            ceList = new ContentEncodingValues[0];
            getRequest().setContentEncoding(ceList);
            ceValues = getRequest().getContentEncoding();
            assertEquals(1, ceValues.length);
            assertEquals(ContentEncodingValues.NOTSET, ceValues[0]);

            // Test for empty list scenario
            getRequest().setContentEncoding(ContentEncodingValues.NOTSET);
            ceValues = getRequest().getContentEncoding();
            assertEquals(1, ceValues.length);
            assertEquals(ContentEncodingValues.NOTSET, ceValues[0]);

            // *****************************************************************
            // Test the Content-Length header APIs
            // *****************************************************************

            // @ Tested API - setContentLength(long)
            // @ Tested API - getContentLength()
            getRequest().clear();
            getRequest().setContentLength(100);
            assertEquals(100, getRequest().getContentLength());

            // Test setting long values           
            getRequest().clear();
            getRequest().setContentLength(Integer.MAX_VALUE + 1L);
            assertEquals(Integer.MAX_VALUE + 1L, getRequest().getContentLength());
            getRequest().clear();
            getRequest().setContentLength(Long.MAX_VALUE);
            assertEquals(Long.MAX_VALUE, getRequest().getContentLength());
            getRequest().setHeader(HttpHeaderKeys.HDR_CONTENT_LENGTH, Long.toString(Integer.MAX_VALUE + 1L));
            assertEquals(Integer.MAX_VALUE + 1L, getRequest().getContentLength());
            getRequest().clear();
            getRequest().setHeader(HttpHeaderKeys.HDR_CONTENT_LENGTH, Long.toString(Long.MAX_VALUE));
            assertEquals(Long.MAX_VALUE, getRequest().getContentLength());

            // *****************************************************************
            // Test the URI specific APIs
            // *****************************************************************

            getRequest().clear();

            // Test simple URIs

            // @ Tested API - setRequestURI(byte[])
            // @ Tested API - setRequestURI(String)
            // @ Tested API - setRequestURL(String)
            // @ Tested API - getRequestURIAsByteArray()
            // @ Tested API - getRequestURI()
            // @ Tested API - getScheme()
            // @ Tested API - getQueryString()
            getRequest().setRequestURI("/index.html".getBytes());
            assertEquals(new String(getRequest().getRequestURIAsByteArray()), "/index.html");
            getRequest().setRequestURI("/string.html");
            assertEquals(getRequest().getRequestURI(), "/string.html");
            getRequest().setRequestURL("https://localhost:80/url.html?options");
            assertEquals(getRequest().getScheme(), "https");
            assertEquals(getRequest().getRequestURI(), "/url.html");
            assertEquals(getRequest().getQueryString(), "options");

            getRequest().setRequestURL("//host/index.html?myoptions");
            assertEquals("myoptions", getRequest().getQueryString());
            assertEquals("//host/index.html", getRequest().getRequestURI());

            getRequest().setRequestURL("/index?");
            assertNull(getRequest().getQueryString());
            assertEquals("/index", getRequest().getRequestURI());

            getRequest().setRequestURL("*?queries");
            assertEquals("*", getRequest().getRequestURI());
            assertEquals("queries", getRequest().getQueryString());

            getRequest().setRequestURL("http://host:/uri");
            assertEquals("host", getRequest().getURLHost());
            assertEquals(80, getRequest().getURLPort());

            getRequest().setRequestURL("http://host:80");
            assertEquals("host", getRequest().getURLHost());
            assertEquals(80, getRequest().getURLPort());
            assertEquals("/", getRequest().getRequestURI());

            getRequest().setRequestURL("http://host:80/");
            assertEquals("host", getRequest().getURLHost());
            assertEquals(80, getRequest().getURLPort());
            assertEquals("/", getRequest().getRequestURI());

            try {
                getRequest().setRequestURL("https:/test");
                fail();
            } catch (Exception e) {
                // verify scheme :// parsing works
            }
            try {
                getRequest().setRequestURL("noendtoscheme");
                fail();
            } catch (Exception e) {
                // verify scheme with no end is caught
            }
            try {
                getRequest().setRequestURL("*fail");
                fail();
            } catch (Exception e) {
                // verify URI with invalid leading * fails
            }
            try {
                getRequest().setRequestURL("http://[ipv6badaddress/index.html");
                fail();
            } catch (Exception e) {
                // verify a neverending IPv6 IP fails the parse
            }

            // Test the URI variations now

            getRequest().setRequestURI("/index?");
            assertNull(getRequest().getQueryString());
            assertEquals("/index", getRequest().getRequestURI());
            assertEquals("http", getRequest().getScheme());
            assertNull(getRequest().getURLHost());
            assertEquals(-1, getRequest().getURLPort());

            getRequest().setRequestURI("*?queries");
            assertEquals("*", getRequest().getRequestURI());
            assertEquals("queries", getRequest().getQueryString());

            try {
                getRequest().setRequestURI("a");
                fail();
            } catch (Exception e) {
                // verify invalid single char is handled
            }
            try {
                getRequest().setRequestURI("*index");
                fail();
            } catch (Exception e) {
                // verify invalid leading * is handled
            }
            try {
                getRequest().setRequestURI("");
                fail();
            } catch (Exception e) {
                // verify empty data is handled
            }

            // Test a userinfo being present in the URL
            getRequest().setRequestURL("http://user:password@host/index.html");
            assertEquals("host", getRequest().getURLHost());
            assertEquals("http", getRequest().getScheme());

            // *****************************************************************
            // Test target host/port APIs
            // *****************************************************************

            // @ Tested API - getURLHost()
            // @ Tested API - getURLPort()
            // @ Tested API - getVirtualHost()
            // @ Tested API - getVirtualPort()

            getRequest().clear();

            // Test hostname formats
            getRequest().setRequestURL("http://urlhost/index.html");
            assertEquals("urlhost", getRequest().getURLHost());
            assertEquals(80, getRequest().getURLPort());
            getRequest().setHeader("Host", "hostheader");
            assertEquals("urlhost", getRequest().getVirtualHost());
            getRequest().setRequestURI("/index.html");
            assertEquals("hostheader", getRequest().getVirtualHost());
            assertEquals(-1, getRequest().getVirtualPort());
            getRequest().clear();
            getRequest().setHeader("Host", "");
            assertEquals(null, getRequest().getVirtualHost());
            getRequest().clear();

            // Test hostname formats with ports
            getRequest().setRequestURL("http://urlhost:80/index.html");
            assertEquals("urlhost", getRequest().getURLHost());
            assertEquals(80, getRequest().getURLPort());
            assertEquals("urlhost", getRequest().getVirtualHost());
            assertEquals(80, getRequest().getVirtualPort());
            getRequest().clear();
            getRequest().setHeader("Host", "hostheader:9080");
            getRequest().setRequestURI("/index.html");
            assertEquals("hostheader", getRequest().getVirtualHost());
            assertEquals(9080, getRequest().getVirtualPort());
            getRequest().clear();

            // Test IP formats
            getRequest().setRequestURL("http://1.2.3.4/index.html");
            assertEquals("1.2.3.4", getRequest().getURLHost());
            assertEquals(80, getRequest().getURLPort());
            getRequest().setHeader("Host", "5.6.7.8");
            assertEquals("1.2.3.4", getRequest().getVirtualHost());
            getRequest().setRequestURI("/index.html");
            assertEquals("5.6.7.8", getRequest().getVirtualHost());
            assertEquals(-1, getRequest().getVirtualPort());
            getRequest().clear();

            // Test IP formats with port
            getRequest().setRequestURL("http://1.2.3.4:88/index.html");
            assertEquals("1.2.3.4", getRequest().getURLHost());
            assertEquals(88, getRequest().getURLPort());
            assertEquals("1.2.3.4", getRequest().getVirtualHost());
            assertEquals(88, getRequest().getVirtualPort());
            getRequest().clear();
            getRequest().setHeader("Host", "5.6.7.8:9088");
            getRequest().setRequestURI("/index.html");
            assertEquals("5.6.7.8", getRequest().getVirtualHost());
            assertEquals(9088, getRequest().getVirtualPort());
            getRequest().clear();

            // Test IPv6 IP formats
            getRequest().setRequestURL("http://[urlhost]/index.html");
            assertEquals("[urlhost]", getRequest().getURLHost());
            assertEquals(80, getRequest().getURLPort());
            getRequest().setHeader("Host", "[hostheader]");
            assertEquals("[urlhost]", getRequest().getVirtualHost());
            getRequest().setRequestURI("/index.html");
            assertEquals("[hostheader]", getRequest().getVirtualHost());
            assertEquals(-1, getRequest().getVirtualPort());
            getRequest().clear();

            // Test IPv6 IP formats with port
            getRequest().setRequestURL("http://[urlhost]:88/index.html");
            assertEquals("[urlhost]", getRequest().getURLHost());
            assertEquals(88, getRequest().getURLPort());
            assertEquals("[urlhost]", getRequest().getVirtualHost());
            assertEquals(88, getRequest().getVirtualPort());
            getRequest().clear();
            getRequest().setHeader("Host", "[hostheader]:9088");
            getRequest().setRequestURI("/index.html");
            assertEquals("[hostheader]", getRequest().getVirtualHost());
            assertEquals(9088, getRequest().getVirtualPort());
            getRequest().clear();

            // test the URL creation API
            getRequest().setRequestURI("/index.html");
            getRequest().setHeader("Host", "[ipv6]:443");
            assertEquals("http://[ipv6]:443/index.html", getRequest().getRequestURLAsString());

            // *****************************************************************
            // Test the query string APIs
            // *****************************************************************

            getRequest().clear();

            // @ Tested API - setRequestURL(byte[])
            // @ Tested API - setQueryString(byte[])
            // @ Tested API - setQueryString(String)
            // @ Tested API - getQueryStringAsByteArray
            // @ Tested API - getQueryString()
            getRequest().setRequestURL("ftp://localhost:80/ftp.html?myoptions=y".getBytes());
            assertEquals(new String(getRequest().getQueryStringAsByteArray()), "myoptions=y");
            getRequest().setQueryString("youroptions".getBytes());
            assertEquals(getRequest().getQueryString(), "youroptions");
            getRequest().setQueryString("superoptions");
            assertEquals(getRequest().getQueryString(), "superoptions");

            // *****************************************************************
            // Test the Transfer-Encoding header APIs
            // *****************************************************************

            getRequest().clear();

            // @ Tested API - setTransferEncoding(TransferEncodingValues)
            // @ Tested API - setTransferEncoding(TransferEncodingValues[])
            // @ Tested API - getTransferEncoding()
            getRequest().setTransferEncoding(TransferEncodingValues.CHUNKED);
            TransferEncodingValues[] teValues = getRequest().getTransferEncoding();
            assertEquals(teValues[0], TransferEncodingValues.CHUNKED);
            TransferEncodingValues[] teList = new TransferEncodingValues[] {
                                                                            TransferEncodingValues.COMPRESS,
                                                                            TransferEncodingValues.DEFLATE };
            getRequest().setTransferEncoding(teList);
            teValues = getRequest().getTransferEncoding();
            assertEquals(teList.length, teValues.length);
            for (int i = 0; i < teList.length; i++) {
                assertTrue(teList[i].equals(teValues[i]));
            }

            // *****************************************************************
            // Test the duplicate method
            // *****************************************************************

            getRequest().clear();

            // @ Tested API - duplicate()
            getRequest().setRequestURI("/index.html");
            getRequest().setHeader("Test", "TestValue");
            getRequest().setHeader(HttpHeaderKeys.HDR_TRAILER, "Connection");
            getRequest().getTrailers().setHeader("Connection", "Close");

            HttpRequestMessageImpl duplicate =
                            (HttpRequestMessageImpl) getRequest().duplicate();
            assertNotNull(duplicate);
            WsByteBuffer[] hdrData = duplicateBuffers(duplicate.marshallHeaders(null));
            assertEquals(1, hdrData.length);
            byte[] data = new byte[hdrData[0].limit()];
            hdrData[0].get(data);
            byte[] compareData =
                            "Test: TestValue\r\nTrailer: Connection\r\n\r\n".getBytes();
            assertEquals(data.length, compareData.length);
            for (int i = 0; i < data.length; i++) {
                assertEquals(data[i], compareData[i]);
            }

            assertTrue(duplicate.containsHeader("Test"));
            assertTrue(duplicate.getTrailers().containsHeader(
                                                              HttpHeaderKeys.HDR_CONNECTION));

            duplicate.destroy();
            WsByteBufferUtils.releaseBufferArray(hdrData);

            // *****************************************************************
            // Test the request scheme related APIs
            // *****************************************************************

            getRequest().clear();

            // @ Tested API - setScheme(byte[])
            // @ Tested API - setScheme(String)
            // @ Tested API - setScheme(SchemeValues)
            // @ Tested API - getScheme()
            // @ Tested API - getSchemeValue()
            getRequest().setScheme("http".getBytes());
            assertEquals(getRequest().getScheme(), "http");
            getRequest().setScheme("ftp");
            assertEquals(getRequest().getSchemeValue(), SchemeValues.FTP);
            getRequest().setScheme(SchemeValues.HTTPS);
            assertEquals(getRequest().getScheme(), "https");

            // *****************************************************************
            // Test the header tokenizer APIs
            // *****************************************************************

            getRequest().clear();

            // @ Tested API getHeaderTokens(String, byte)
            // @ Tested API getHeaderTokens(HeaderKeys, byte)
            getRequest().appendHeader(HttpHeaderKeys.HDR_TRAILER,
                                      "Content-Language, Date, trailer_test1, trailer_test2");
            List<byte[]> parsedHeaderTokens = getRequest().getHeader(
                                                                     HttpHeaderKeys.HDR_TRAILER.getName()).asTokens((byte) ',');
            assertEquals(parsedHeaderTokens.size(), 4);

            // @ Tested API getHeaderTokens(String, byte, int)
            getRequest().clear();
            getRequest().appendHeader(HttpHeaderKeys.HDR_TRAILER,
                                      "Content-Language, Date, trailer_test1, trailer_test2");
            getRequest().appendHeader(HttpHeaderKeys.HDR_TRAILER,
                                      "This$is$a$sample$test");
            parsedHeaderTokens = getRequest().getHeaders(
                                                         HttpHeaderKeys.HDR_TRAILER.getName()).get(1).asTokens((byte) '$');
            assertEquals(parsedHeaderTokens.size(), 5);

            // @ Tested API getHeaderTokens(HeaderKeys, byte, int)
            getRequest().clear();
            getRequest().appendHeader(HttpHeaderKeys.HDR_TRAILER,
                                      "Content-Language, Date, trailer_test1, trailer_test2");
            getRequest().appendHeader(HttpHeaderKeys.HDR_TRAILER,
                                      "This$is a$sample test");
            parsedHeaderTokens = getRequest().getHeaders(
                                                         HttpHeaderKeys.HDR_TRAILER).get(1).asTokens((byte) '$');
            assertEquals(parsedHeaderTokens.size(), 5);

            // *****************************************************************
            // Test the marshalling/parsing code
            // *****************************************************************

            getRequest().clear();

            // Test known first line request headers
            getRequest().setMethod(MethodValues.POST);
            getRequest().setRequestURI("/index.html");
            getRequest().setVersion(VersionValues.V09);
            WsByteBuffer[] mData = null;
            boolean rc = false;
            // marshall the request first line
            mData = getRequest().marshallBinaryMessage();

            // parse the request first line
            for (int i = 0; i < mData.length; i++) {
                rc = getRequest().parseBinaryFirstLine(mData[i]);
            }
            assertTrue(rc);

            // Test unknown first line information

            getRequest().clear();
            getRequest().setMethod(MethodValues.find("SUPERGET"));
            getRequest().setRequestURI("/index.html");
            getRequest().setVersion(VersionValues.find("HTTP/3.0"));
            // marshall the request first line
            mData = getRequest().marshallBinaryMessage();

            // parse the request first line
            for (int i = 0; i < mData.length; i++) {
                rc = getRequest().parseBinaryFirstLine(mData[i]);
            }
            assertTrue(rc);

            // Test Known and Unknown first line with request headers

            getRequest().clear();
            getRequest().setMethod(MethodValues.GET);
            getRequest().setRequestURI("/index.html");
            getRequest().setVersion(VersionValues.V09);
            getRequest().setQueryString("This is a test Query String");
            getRequest().setHeader("TestUnknownHdr1", "test-cookie1".getBytes());
            getRequest().setHeader("TestUnknownHdr2", "test-cookie2".getBytes());
            // marshall the request
            mData = getRequest().marshallBinaryMessage();

            // parse the request
            for (int i = 0; i < mData.length; i++) {
                rc = getRequest().parseBinaryMessage(mData[i]);
            }
            assertTrue(rc);

            // Test forward proxy type requests

            getRequest().clear();
            getRequest().setRequestURL("http://target/index.html");
            assertEquals("http://target:80/index.html", getRequest().getRequestURLAsString());

            getRequest().setRequestURL("https://target/index.html");
            assertEquals("https://target:443/index.html", getRequest().getRequestURLAsString());

            getRequest().setRequestURL("https://target:3127/index.html");
            assertEquals("https://target:3127/index.html", getRequest().getRequestURLAsString());

            // *****************************************************************
            // Test the Connection header APIs
            // *****************************************************************

            getRequest().clear();

            // test setConnection

            // test single known value
            String test = "Close";
            getRequest().setHeader(HttpHeaderKeys.HDR_CONNECTION, test);
            int[] ords = new int[] { ConnectionValues.CLOSE.getOrdinal() };
            String[] names = null;
            rc = verifyConnection(ords, names, getRequest().getConnection());
            assertTrue(rc);

            // test single unknown value
            test = "What";
            getRequest().removeHeader(HttpHeaderKeys.HDR_CONNECTION);
            getRequest().setHeader(HttpHeaderKeys.HDR_CONNECTION, test);
            ords[0] = ConnectionValues.find("What").getOrdinal();
            names = new String[] { "What" };
            rc = verifyConnection(ords, names, getRequest().getConnection());
            assertTrue(rc);

            // test multiple known values
            test = "Close, TE";
            getRequest().removeHeader(HttpHeaderKeys.HDR_CONNECTION);
            getRequest().setHeader(HttpHeaderKeys.HDR_CONNECTION, test);
            ords = new int[] {
                              ConnectionValues.CLOSE.getOrdinal(),
                              ConnectionValues.TE.getOrdinal() };
            rc = verifyConnection(ords, names, getRequest().getConnection());
            assertTrue(rc);

            // test multiple unknown values
            test = "What, Where";
            getRequest().removeHeader(HttpHeaderKeys.HDR_CONNECTION);
            getRequest().setHeader(HttpHeaderKeys.HDR_CONNECTION, test);
            ords[0] = ConnectionValues.find("What").getOrdinal();
            ords[1] = ConnectionValues.find("Where").getOrdinal();
            names = new String[] { "What", "Where" };
            rc = verifyConnection(ords, names, getRequest().getConnection());
            assertTrue(rc);

            // test mixed values
            test = "Close, What";
            getRequest().removeHeader(HttpHeaderKeys.HDR_CONNECTION);
            getRequest().setHeader(HttpHeaderKeys.HDR_CONNECTION, test);
            ords[0] = ConnectionValues.CLOSE.getOrdinal();
            ords[1] = ConnectionValues.find("What").getOrdinal();
            names[1] = "What";
            rc = verifyConnection(ords, names, getRequest().getConnection());
            assertTrue(rc);

            // test mixed values in other order
            test = "What, Close";
            getRequest().removeHeader(HttpHeaderKeys.HDR_CONNECTION);
            getRequest().setHeader(HttpHeaderKeys.HDR_CONNECTION, test);
            ords[0] = ConnectionValues.find("What").getOrdinal();
            ords[1] = ConnectionValues.CLOSE.getOrdinal();
            names[0] = "What";
            rc = verifyConnection(ords, names, getRequest().getConnection());
            assertTrue(rc);

            // test leading spaces
            test = "  Close, TE";
            getRequest().removeHeader(HttpHeaderKeys.HDR_CONNECTION);
            getRequest().setHeader(HttpHeaderKeys.HDR_CONNECTION, test);
            ords[0] = ConnectionValues.CLOSE.getOrdinal();
            ords[1] = ConnectionValues.TE.getOrdinal();
            rc = verifyConnection(ords, names, getRequest().getConnection());
            assertTrue(rc);

            // test trailing spaces
            test = "Close, TE   ";
            getRequest().removeHeader(HttpHeaderKeys.HDR_CONNECTION);
            getRequest().setHeader(HttpHeaderKeys.HDR_CONNECTION, test);
            rc = verifyConnection(ords, names, getRequest().getConnection());
            assertTrue(rc);

            // test trailing spaces on multiple instances
            test = "Close   , TE   ";
            getRequest().removeHeader(HttpHeaderKeys.HDR_CONNECTION);
            getRequest().setHeader(HttpHeaderKeys.HDR_CONNECTION, test);
            rc = verifyConnection(ords, names, getRequest().getConnection());
            assertTrue(rc);

            // test mixing everything
            test = "Close   ,   What  ,   TE   , Where    ";
            getRequest().removeHeader(HttpHeaderKeys.HDR_CONNECTION);
            getRequest().setHeader(HttpHeaderKeys.HDR_CONNECTION, test);
            ords = new int[] {
                              ConnectionValues.CLOSE.getOrdinal(),
                              ConnectionValues.find("What").getOrdinal(),
                              ConnectionValues.TE.getOrdinal(),
                              ConnectionValues.find("Where").getOrdinal() };
            names = new String[] { null, "What", null, "Where" };
            rc = verifyConnection(ords, names, getRequest().getConnection());
            assertTrue(rc);

            // test spaces in middle of token
            test = "Close   TE  , Keep-Alive";
            getRequest().removeHeader(HttpHeaderKeys.HDR_CONNECTION);
            getRequest().setHeader(HttpHeaderKeys.HDR_CONNECTION, test);
            ords = new int[] {
                              ConnectionValues.find("Close   TE").getOrdinal(),
                              ConnectionValues.KEEPALIVE.getOrdinal() };
            names = new String[] { "Close   TE", null };
            rc = verifyConnection(ords, names, getRequest().getConnection());
            assertTrue(rc);
            assertTrue(getRequest().isKeepAliveSet());

            // test empty token
            test = "  , keep-ALIvE, TE";
            getRequest().removeHeader(HttpHeaderKeys.HDR_CONNECTION);
            getRequest().setHeader(HttpHeaderKeys.HDR_CONNECTION, test);
            ords[0] = ConnectionValues.KEEPALIVE.getOrdinal();
            ords[1] = ConnectionValues.TE.getOrdinal();
            rc = verifyConnection(ords, names, getRequest().getConnection());
            assertTrue(rc);
            assertTrue(getRequest().isKeepAliveSet());

            // test partial match
            test = " TE , KEEP-alive   ,   Clos";
            getRequest().removeHeader(HttpHeaderKeys.HDR_CONNECTION);
            getRequest().setHeader(HttpHeaderKeys.HDR_CONNECTION, test);
            ords = new int[] {
                              ConnectionValues.TE.getOrdinal(),
                              ConnectionValues.KEEPALIVE.getOrdinal(),
                              ConnectionValues.find("Clos").getOrdinal() };
            names = new String[] { null, null, "Clos" };
            rc = verifyConnection(ords, names, getRequest().getConnection());
            assertTrue(rc);

            // ******************************************************************
            // Test multiple header instances with special APIs
            // ******************************************************************

            getRequest().setVersion(VersionValues.V11);
            getRequest().setConnection(ConnectionValues.NOTSET);
            getRequest().setHeader(HttpHeaderKeys.HDR_CONNECTION, "Close");
            getRequest().appendHeader(HttpHeaderKeys.HDR_CONNECTION, "TE");
            getRequest().appendHeader(HttpHeaderKeys.HDR_CONNECTION, "What");
            ords = new int[] {
                              ConnectionValues.CLOSE.getOrdinal(),
                              ConnectionValues.TE.getOrdinal(),
                              ConnectionValues.find("What").getOrdinal() };
            names = new String[] { "Close", "TE", "What" };
            rc = verifyConnection(ords, names, getRequest().getConnection());
            assertTrue(rc);
            assertTrue(getRequest().isCloseSet());
            assertFalse(getRequest().isKeepAliveSet());
            getRequest().removeHeader(HttpHeaderKeys.HDR_CONNECTION, 1);
            assertTrue(getRequest().isCloseSet());
            assertEquals(2, getRequest().getConnection().length);
            getRequest().removeHeader(HttpHeaderKeys.HDR_CONNECTION);
            assertFalse(getRequest().isCloseSet());
            assertTrue(getRequest().isKeepAliveSet()); // this is a 1.1 request
            assertEquals(ConnectionValues.NOTSET, getRequest().getConnection()[0]);

            getRequest().setHeader(HttpHeaderKeys.HDR_TRANSFER_ENCODING, "identity");
            assertFalse(getRequest().isChunkedEncodingSet());
            getRequest().appendHeader(HttpHeaderKeys.HDR_TRANSFER_ENCODING, "chunked");
            getRequest().appendHeader(HttpHeaderKeys.HDR_TRANSFER_ENCODING, "What");
            ords = new int[] {
                              TransferEncodingValues.IDENTITY.getOrdinal(),
                              TransferEncodingValues.CHUNKED.getOrdinal(),
                              TransferEncodingValues.find("What").getOrdinal() };
            names = new String[] { "identity", "chunked", "What" };
            rc = verifyConnection(ords, names, getRequest().getTransferEncoding());
            assertTrue(rc);
            assertTrue(getRequest().isChunkedEncodingSet());
            getRequest().removeHeader(HttpHeaderKeys.HDR_TRANSFER_ENCODING, 1);
            assertFalse(getRequest().isChunkedEncodingSet());
            assertEquals(2, getRequest().getTransferEncoding().length);
            getRequest().removeHeader(HttpHeaderKeys.HDR_TRANSFER_ENCODING);
            assertFalse(getRequest().isChunkedEncodingSet());
            assertEquals(TransferEncodingValues.NOTSET,
                         getRequest().getTransferEncoding()[0]);

            getRequest().setHeader(HttpHeaderKeys.HDR_CONTENT_ENCODING, "gzip");
            getRequest().appendHeader(HttpHeaderKeys.HDR_CONTENT_ENCODING, "identity");
            getRequest().appendHeader(HttpHeaderKeys.HDR_CONTENT_ENCODING, "something");
            ords = new int[] {
                              ContentEncodingValues.GZIP.getOrdinal(),
                              ContentEncodingValues.IDENTITY.getOrdinal(),
                              ContentEncodingValues.find("something").getOrdinal() };
            names = new String[] { "gzip", "identify", "something" };
            rc = verifyConnection(ords, names, getRequest().getContentEncoding());
            assertTrue(rc);
            getRequest().removeHeader(HttpHeaderKeys.HDR_CONTENT_ENCODING, 0);
            assertEquals(ContentEncodingValues.IDENTITY,
                         getRequest().getContentEncoding()[0]);
            assertEquals(2, getRequest().getContentEncoding().length);
            getRequest().removeHeader(HttpHeaderKeys.HDR_CONTENT_ENCODING);
            assertEquals(ContentEncodingValues.NOTSET,
                         getRequest().getContentEncoding()[0]);
            assertEquals(1, getRequest().getContentEncoding().length);

            getRequest().setHeader(HttpHeaderKeys.HDR_EXPECT, "119-tag");
            assertFalse(getRequest().isExpect100Continue());
            getRequest().appendHeader(HttpHeaderKeys.HDR_EXPECT, "100-continue");
            assertTrue(getRequest().isExpect100Continue());
            assertEquals("119-tag, 100-continue",
                         GenericUtils.getEnglishString(getRequest().getExpect()));
            getRequest().appendHeader(HttpHeaderKeys.HDR_EXPECT, "199-end");
            assertTrue(getRequest().isExpect100Continue());
            assertEquals("119-tag, 100-continue, 199-end",
                         GenericUtils.getEnglishString(getRequest().getExpect()));
            getRequest().removeHeader(HttpHeaderKeys.HDR_EXPECT, 2);
            assertEquals("119-tag, 100-continue",
                         GenericUtils.getEnglishString(getRequest().getExpect()));
            assertTrue(getRequest().isExpect100Continue());
            getRequest().removeHeader(HttpHeaderKeys.HDR_EXPECT);
            assertFalse(getRequest().isExpect100Continue());
            assertNull(getRequest().getExpect());

            // *****************************************************************
            // Test the special headerkey filter APIs
            // *****************************************************************

            // test adding multiple instances of the integer headers
            getRequest().setHeader(HttpHeaderKeys.HDR_CONTENT_LENGTH, "50");
            assertEquals(50, getRequest().getContentLength());
            getRequest().appendHeader(HttpHeaderKeys.HDR_CONTENT_LENGTH, "100");
            assertEquals(50, getRequest().getContentLength());
            // test removing one of the multiple instances
            getRequest().removeHeader(HttpHeaderKeys.HDR_CONTENT_LENGTH, 1);
            assertEquals(50, getRequest().getContentLength());
            assertEquals(50, getRequest().getHeader(HttpHeaderKeys.HDR_CONTENT_LENGTH).asInteger());
            assertEquals(1, getRequest().getNumberOfHeaderInstances(HttpHeaderKeys.HDR_CONTENT_LENGTH));

            // test multiple header instances and the getTransferEncoding array
            getRequest().setHeader(HttpHeaderKeys.HDR_TRANSFER_ENCODING, "chunked");
            assertTrue(getRequest().isChunkedEncodingSet());
            getRequest().appendHeader(HttpHeaderKeys.HDR_TRANSFER_ENCODING, "gzip");
            TransferEncodingValues[] tev = new TransferEncodingValues[] {
                                                                         TransferEncodingValues.CHUNKED,
                                                                         TransferEncodingValues.GZIP
            };
            TransferEncodingValues[] msgTEV = getRequest().getTransferEncoding();
            assertEquals(tev.length, msgTEV.length);
            for (int i = 0; i < msgTEV.length; i++) {
                assertEquals(tev[i], msgTEV[i]);
            }
            // test the setHeader() deleting all the values
            getRequest().setHeader(HttpHeaderKeys.HDR_TRANSFER_ENCODING, "compress");
            assertFalse(getRequest().isChunkedEncodingSet());
            msgTEV = getRequest().getTransferEncoding();
            assertEquals(1, msgTEV.length);
            assertEquals(TransferEncodingValues.COMPRESS, msgTEV[0]);
            // test detection of proper undefined values
            getRequest().appendHeader(HttpHeaderKeys.HDR_TRANSFER_ENCODING, "x-y");
            getRequest().appendHeader(HttpHeaderKeys.HDR_TRANSFER_ENCODING, "abc");
            getRequest().removeHeader(HttpHeaderKeys.HDR_TRANSFER_ENCODING, 2);
            msgTEV = getRequest().getTransferEncoding();
            assertEquals(2, msgTEV.length);
            assertEquals(TransferEncodingValues.COMPRESS, msgTEV[0]);
            assertEquals("x-y", msgTEV[1].getName());

            getRequest().setHeader(HttpHeaderKeys.HDR_CONTENT_ENCODING, "gzip");
            getRequest().appendHeader(HttpHeaderKeys.HDR_CONTENT_ENCODING, "zipper");
            getRequest().appendHeader(HttpHeaderKeys.HDR_CONTENT_ENCODING, "zippest");
            getRequest().removeContentEncodingHeader("zippest".getBytes());
            ContentEncodingValues[] msgCEV = getRequest().getContentEncoding();
            assertEquals(ContentEncodingValues.GZIP, msgCEV[0]);
            assertTrue(msgCEV[1].isUndefined());
            assertEquals("zipper", msgCEV[1].getName());

            getRequest().setHeader(HttpHeaderKeys.HDR_CONTENT_ENCODING, "gzip");
            getRequest().appendHeader(HttpHeaderKeys.HDR_CONTENT_ENCODING, "zipper");
            getRequest().appendHeader(HttpHeaderKeys.HDR_CONTENT_ENCODING, "zippest");
            getRequest().removeOutermostEncoding();
            assertEquals(2, getRequest().getContentEncoding().length);
            getRequest().removeOutermostEncoding();
            msgCEV = getRequest().getContentEncoding();
            assertEquals("gzip", msgCEV[0].getName());
            assertEquals(1, msgCEV.length);
            getRequest().removeOutermostEncoding();
            msgCEV = getRequest().getContentEncoding();
            assertEquals(1, msgCEV.length);
            assertEquals(ContentEncodingValues.NOTSET, msgCEV[0]);

            // *****************************************************************
            // Test the header as Date APIs
            // *****************************************************************

            getRequest().clear();

            // @Tested API - getHeaderAsDate(HeaderKey)
            // @Tested API - getHeaderAsDate(byte[], int)
            Date date1 = new Date();
            String dateValue = df.getRFC1123Time(date1);
            getRequest().setHeader(HttpHeaderKeys.HDR_EXPIRES, dateValue);

            // the milliseconds are lost during the conversions so date1 does not
            // equals date2 through getTime() comparisons or Date.equals(Date)
            assertEquals(date1.toString(),
                         getRequest().getHeader(HttpHeaderKeys.HDR_EXPIRES).asDate().toString());

            assertEquals(date1.toString(),
                         getRequest().getHeader(HttpHeaderKeys.HDR_EXPIRES.getByteArray()).asDate().toString());

            // Test the list versions
            // @ Tested API -- getHeaderDateValues(HeaderKeys)

            String dateValue1 = df.getRFC1123Time();
            Thread.sleep(1000);
            String dateValue2 = df.getRFC1123Time();
            getRequest().setHeader(HttpHeaderKeys.HDR_$WSCC, dateValue1);
            getRequest().appendHeader(HttpHeaderKeys.HDR_$WSCC, dateValue2);

            List<HeaderField> dateList = getRequest().getHeaders(HttpHeaderKeys.HDR_$WSCC);
            assertEquals(dateValue1, df.getRFC1123Time(dateList.get(0).asDate()));
            assertEquals(dateValue2, df.getRFC1123Time(dateList.get(1).asDate()));

            // 335326 - verify parsing of lowercase works too
            getRequest().setHeader("test", "sat, 01 jan 2000 00:00:01 gmt");
            getRequest().getHeader("test").asDate();

            // *****************************************************************
            // Test the header as Integer APIs
            // *****************************************************************

            getRequest().clear();

            // @ Tested API -- getHeaderAsInteger(HeaderKeys)
            // @ Tested API -- getHeaderAsInteger(String, int)
            int int1 = 505789;
            getRequest().setHeader(HttpHeaderKeys.HDR_$WSAT, "" + int1);

            assertEquals(int1, getRequest().getHeader(HttpHeaderKeys.HDR_$WSAT).asInteger());

            assertEquals(int1, getRequest().getHeader("$WSAT").asInteger());

            // Test the list versions
            // @ Tested API -- getHeaderIntegerValues(HeaderKeys)

            int1 = 4398459;
            int int2 = 12312634;
            getRequest().setHeader(HttpHeaderKeys.HDR_$WSRA, "" + int1);
            getRequest().appendHeader(HttpHeaderKeys.HDR_$WSRA, "" + int2);

            List<HeaderField> intList = getRequest().getHeaders(HttpHeaderKeys.HDR_$WSRA);
            assertEquals(int1, intList.get(0).asInteger());
            assertEquals(int2, intList.get(1).asInteger());

            // *****************************************************************
            // Test the mime-type APIs
            // *****************************************************************

            getRequest().clear();

            // @ Tested API getMIMEType() ... <type>
            // @ Tested API getMIMEType() ... <type>;<charset>
            getRequest().setHeader(HttpHeaderKeys.HDR_CONTENT_TYPE, "text/html");
            String mime = getRequest().getMIMEType();
            assertEquals(mime, "text/html");
            getRequest().setHeader(HttpHeaderKeys.HDR_CONTENT_TYPE, "test;charset=en_us");
            mime = getRequest().getMIMEType();
            assertEquals(mime, "test");

            // *****************************************************************
            // Test the header parsing code
            // *****************************************************************

            getRequest().clear();

            // Test small indirect buffer parsing where second token straddles
            WsByteBufferPoolManager mgr = ChannelFrameworkFactory.getBufferManager();
            WsByteBuffer buff1 = mgr.allocate(1024);
            WsByteBuffer buff2 = mgr.allocate(32);
            buff1.put("GET /012345678901234567890123456".getBytes());
            buff2.put("789 HTTP/1.0\r\n\r\n".getBytes());
            buff1.flip();
            buff2.flip();
            assertFalse(getRequest().parseMessage(buff1, true));
            assertTrue(getRequest().parseMessage(buff2, true));
            assertEquals(MethodValues.GET, getRequest().getMethodValue());
            assertEquals("/012345678901234567890123456789", getRequest().getRequestURI());
            assertEquals(VersionValues.V10, getRequest().getVersionValue());
            buff1.release();
            buff2.release();

            // Test small direct buffer parsing where second token straddles
            getRequest().clear();
            buff1 = mgr.allocateDirect(32);
            buff2 = mgr.allocateDirect(32);
            buff1.put("GET /012345678901234567890123456".getBytes());
            buff2.put("789 HTTP/1.0\r\n\r\n".getBytes());
            buff1.flip();
            buff2.flip();
            assertFalse(getRequest().parseMessage(buff1, true));
            assertTrue(getRequest().parseMessage(buff2, true));
            assertEquals(MethodValues.GET, getRequest().getMethodValue());
            assertEquals("/012345678901234567890123456789", getRequest().getRequestURI());
            assertEquals(VersionValues.V10, getRequest().getVersionValue());
            buff1.release();
            buff2.release();

            // Test small indirect buffer parsing where first token straddles
            getRequest().clear();
            buff1 = mgr.allocate(32);
            buff2 = mgr.allocate(32);
            buff1.put("GETAABBCCDDEEFFGGHHIIJJKKLLMMNNO".getBytes());
            buff2.put("O / HTTP/0001.1\r\n\r\n".getBytes());
            buff1.flip();
            buff2.flip();
            assertFalse(getRequest().parseMessage(buff1, true));
            assertTrue(getRequest().parseMessage(buff2, true));
            assertEquals(getRequest().getMethod(),
                         "GETAABBCCDDEEFFGGHHIIJJKKLLMMNNOO");
            assertEquals("/", getRequest().getRequestURI());
            assertEquals(VersionValues.V11, getRequest().getVersionValue());
            buff1.release();
            buff2.release();

            // Test small direct buffer parsing where first token straddles
            getRequest().clear();
            buff1 = mgr.allocateDirect(32);
            buff2 = mgr.allocateDirect(32);
            buff1.put("GETAABBCCDDEEFFGGHHIIJJKKLLMMNNO".getBytes());
            buff2.put("O / HTTP/1.1\r\n\r\n".getBytes());
            buff1.flip();
            buff2.flip();
            assertFalse(getRequest().parseMessage(buff1, true));
            assertTrue(getRequest().parseMessage(buff2, true));
            assertEquals(getRequest().getMethod(),
                         "GETAABBCCDDEEFFGGHHIIJJKKLLMMNNOO");
            assertEquals("/", getRequest().getRequestURI());
            assertEquals(VersionValues.V11, getRequest().getVersionValue());
            buff1.release();
            buff2.release();

            // Test small indirect buffer parsing where first token ends at end
            // of byteCache
            getRequest().clear();
            buff1 = mgr.allocate(32);
            buff2 = mgr.allocate(32);
            WsByteBuffer buff3 = mgr.allocate(32);
            buff1.put("GETAABBCCDDEEFFGGHHIIJJKKLLMMNN ".getBytes());
            buff2.put("/0123456789012345678901234567890".getBytes());
            buff3.put("1 HTTP/1.0\r\n\r\n".getBytes());
            buff1.flip();
            buff2.flip();
            buff3.flip();
            assertFalse(getRequest().parseMessage(buff1, true));
            assertFalse(getRequest().parseMessage(buff2, true));
            assertTrue(getRequest().parseMessage(buff3, true));
            assertEquals(getRequest().getMethod(), "GETAABBCCDDEEFFGGHHIIJJKKLLMMNN");
            assertEquals("/01234567890123456789012345678901", getRequest().getRequestURI());
            assertEquals(VersionValues.V10, getRequest().getVersionValue());
            buff1.release();
            buff2.release();
            buff3.release();

            // Test small direct buffer parsing where first token delimiter ends
            // on byteCache last character
            getRequest().clear();
            buff1 = mgr.allocateDirect(32);
            buff2 = mgr.allocateDirect(32);
            buff3 = mgr.allocateDirect(32);
            buff1.put("GETAABBCCDDEEFFGGHHIIJJKKLLMMNN ".getBytes());
            buff2.put("/0123456789012345678901234567890".getBytes());
            buff3.put("1 HTTP/1.0\r\n\r\n".getBytes());
            buff1.flip();
            buff2.flip();
            buff3.flip();
            assertFalse(getRequest().parseMessage(buff1, true));
            assertFalse(getRequest().parseMessage(buff2, true));
            assertTrue(getRequest().parseMessage(buff3, true));
            assertEquals(getRequest().getMethod(), "GETAABBCCDDEEFFGGHHIIJJKKLLMMNN");
            assertEquals("/01234567890123456789012345678901", getRequest().getRequestURI());
            assertEquals(VersionValues.V10, getRequest().getVersionValue());
            buff1.release();
            buff2.release();
            buff3.release();

            // test direct buffers when a token straddles bytecaches
            getRequest().clear();
            buff1 = mgr.allocateDirect(1024);
            buff1.put(("GETXXXXXXXXXXXXXXXXXXXXXXXXXXX /MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM "
                       + "HTTP/1.0\r\n\r\n").getBytes());
            buff1.flip();
            getRequest().parseMessage(buff1, true);
            assertEquals("GETXXXXXXXXXXXXXXXXXXXXXXXXXXX", getRequest().getMethod());
            assertEquals("/MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM", getRequest().getRequestURI());
            assertEquals(VersionValues.V10, getRequest().getVersionValue());
            buff1.release();

            // test direct buffers when a token has one character in the last
            // bytecache but the rest in the following one
            getRequest().clear();
            buff1 = mgr.allocateDirect(1024);
            buff1.put("GET /index.htmlaafdafaweafasdfaqadsfa HTTP/1.0\r\nTestingsomeaa: a".getBytes());
            buff1.put("ll\r\n\r\n".getBytes());
            buff1.flip();
            getRequest().parseMessage(buff1, true);
            assertEquals("GET", getRequest().getMethod());
            assertEquals(VersionValues.V10, getRequest().getVersionValue());
            assertEquals("all", getRequest().getHeader("Testingsomeaa").asString());
            buff1.release();

            // test header names straddling buffers
            getRequest().clear();
            buff1 = mgr.allocateDirect(32);
            buff1.put("GET /index.html HTTP/1.1\r\nCon".getBytes());
            buff1.flip();
            getRequest().parseMessage(buff1, true);
            buff1.clear();
            buff1.put("tent-Length: 85\r\nCont".getBytes());
            buff1.flip();
            getRequest().parseMessage(buff1, true);
            buff1.clear();
            buff1.put("ent".getBytes());
            buff1.flip();
            getRequest().parseMessage(buff1, true);
            buff1.clear();
            buff1.put("-Encoding: gzip\r\n\r\n".getBytes());
            buff1.flip();
            getRequest().parseMessage(buff1, true);
            assertEquals(85, getRequest().getContentLength());
            assertEquals(ContentEncodingValues.GZIP,
                         getRequest().getContentEncoding()[0]);
            buff1.release();

            // *****************************************************************
            // Test the query parameter APIs
            // *****************************************************************

            getRequest().clear();
            getRequest().setQueryString("itcs=300&http=405&test+ing=this&http=409&say%20what=you");
            Enumeration<String> enum1 = getRequest().getParameterNames();
            assertTrue(enum1.hasMoreElements());
            String[] vals = getRequest().getParameterValues("itcs");
            assertEquals(1, vals.length);
            assertEquals("300", vals[0]);
            vals = getRequest().getParameterValues("http");
            assertEquals(2, vals.length);
            assertEquals("405", vals[0]);
            assertEquals("409", vals[1]);
            vals = getRequest().getParameterValues("test ing");
            assertEquals(1, vals.length);
            assertEquals("this", vals[0]);
            vals = getRequest().getParameterValues("say what");
            assertEquals(1, vals.length);
            assertEquals("you", vals[0]);

            assertEquals("405", getRequest().getParameter("http"));
            assertEquals("you", getRequest().getParameter("say what"));
            assertNull(getRequest().getParameter("nothere"));

            getRequest().setQueryString("invalid+parameter%20withoutequals");
            enum1 = getRequest().getParameterNames();
            assertFalse(enum1.hasMoreElements());

            // ****************************************************************
            // Test duplication
            // ****************************************************************

            getRequest().clear();
            getRequest().setMethod(MethodValues.DELETE);
            getRequest().setRequestURI("/index.html?q1=v1");
            getRequest().setHeader("host", "localhost");
            getRequest().setHeader("Unknown", "value");
            getRequest().setCookie("c1", "v1", HttpHeaderKeys.HDR_COOKIE);
            getRequest().setLimitOfTokenSize(500);
            HttpRequestMessageImpl r2 = (HttpRequestMessageImpl) getRequest().duplicate();
            assertEquals(getRequest().getLimitOfTokenSize(), r2.getLimitOfTokenSize());
            assertEquals("localhost", r2.getHeader("host").asString());
            assertEquals("value", r2.getHeader("unknown").asString());
            assertEquals("DELETE", r2.getMethod());
            assertEquals("/index.html", r2.getRequestURI());
            assertEquals("q1=v1", r2.getQueryString());
            assertEquals("v1", r2.getCookie("c1").getValue());

            // ****************************************************************
            // Test empty header handling (for webservices)
            // ****************************************************************

            getRequest().clear();
            getRequest().setHeader("SOAPAction", new byte[0]);
            assertEquals("", getRequest().getHeader("SOAPAction").asString());
            assertEquals(0, getRequest().getHeader("SOAPAction").asBytes().length);
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testMain", t);
        }
    }

    /**
     * Utility method for verifying the connection information
     * 
     * @param ords
     * @param names
     * @param list
     * @return boolean
     */
    @SuppressWarnings("unused")
    private boolean verifyConnection(int[] ords, String[] names, GenericKeys[] list) {
        for (int i = 0; i < ords.length; i++) {
            if (ords[i] != list[i].getOrdinal()) {
                return false;
            }
        }
        return true;
    }
}
