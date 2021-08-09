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

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.channelfw.internal.ChannelDataImpl;
import com.ibm.ws.channelfw.internal.InboundVirtualConnectionFactoryImpl;
import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpConfigConstants;
import com.ibm.ws.http.channel.internal.HttpResponseMessageImpl;
import com.ibm.ws.http.channel.test.api.testobjects.MockInboundSC;
import com.ibm.ws.http.channel.test.api.testobjects.MockResponseMessage;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.http.channel.values.StatusCodes;
import com.ibm.wsspi.http.channel.values.VersionValues;

/**
 * The methods common to HttpBaseMessage and lower are tested by the
 * HttpRequestMessageImplTestCase class. This class will only test the
 * Response specific methods.
 */
public class HttpResponseMessageImplTest {
    private static SharedOutputManager outputMgr;

    /** The local reference to a test object. */
    private HttpResponseMessageImpl response = null;
    protected ChannelDataImpl cdi;
    protected HttpChannelConfig config;

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
        this.response.destroy();
        this.response = null;
    }

    /**
     * Create a new message for use and update the chain with the input
     * configuration key/value pair.
     * 
     * @param key
     * @param value
     */
    private void createNewMessage(String key, String value) {
        this.response.clear();
        Map<Object, Object> map = this.cdi.getPropertyBag();
        map.remove(HttpConfigConstants.PROPNAME_SERVER_HEADER_VALUE);
        map.remove(HttpConfigConstants.PROPNAME_REMOVE_SERVER_HEADER);
        map.remove(HttpConfigConstants.PROPNAME_COOKIES_CONFIGURE_NOCACHE);
        if (null != key) {
            map.put(key, value);
        }
        this.config.updateConfig(this.cdi);
    }

    /**
     * Used to set up the test. This method is called by JUnit before each of
     * the tests are executed.
     */
    @Before
    public void setUp() {
        this.cdi = new ChannelDataImpl("HTTP", null, new HashMap<Object, Object>(), 10,
                        ChannelFrameworkFactory.getChannelFramework());
        this.config = new HttpChannelConfig(this.cdi);
        MockInboundSC sc = new MockInboundSC(new InboundVirtualConnectionFactoryImpl().createConnection(), this.config);
        this.response = new MockResponseMessage(sc);
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
     * Get access to the response message.
     * 
     * @return HttpResponseMessageImpl
     */
    private HttpResponseMessageImpl getResponse() {
        return this.response;
    }

    /**
     * Test interacting with the response messages
     */
    @Test
    public void testMain() {
        try {
            // *****************************************************************
            // Test the reason phrase APIs
            // *****************************************************************

            // @ Tested API - setReasonPhrase(byte[])
            getResponse().setReasonPhrase("OK".getBytes());

            // @ Tested API - getReasonPhraseBytes()
            assertEquals("OK", new String(getResponse().getReasonPhraseBytes()));

            // @ Tested API - setReasonPhrase(String)
            getResponse().setReasonPhrase("Not found");

            // @ Tested API - getReasonPhrase()
            assertEquals("Not found", getResponse().getReasonPhrase());

            // *****************************************************************
            // Test the status code APIs
            // *****************************************************************

            getResponse().clear();

            // @ Tested API - setStatusCode(int)
            getResponse().setStatusCode(200);

            // @ Tested API - getStatusCode()
            assertEquals(StatusCodes.OK, getResponse().getStatusCode());

            // @ Tested API - setStatusCode(StatusCodes)
            getResponse().setStatusCode(StatusCodes.CONFLICT);

            // @ Tested API - getStatusCodeAsInt()
            assertEquals(409, getResponse().getStatusCodeAsInt());

            // test the default reason phrase code
            assertEquals(StatusCodes.CONFLICT.getDefaultPhrase(), getResponse().getReasonPhrase());

            // *****************************************************************
            // Test the duplicate method
            // *****************************************************************

            getResponse().clear();
            getResponse().setHeader("Test", "TestValue");

            // @ Tested API - duplicate()
            HttpResponseMessageImpl duplicate =
                            (HttpResponseMessageImpl) getResponse().duplicate();
            assertNotNull(duplicate);

            // @ Tested API - marshallHeaders()
            WsByteBuffer[] hdrData = duplicate.marshallHeaders(null);
            assertEquals(1, hdrData.length);

            byte[] data = new byte[hdrData[0].limit()];
            hdrData[0].get(data);
            byte[] compareData = "Test: TestValue\r\n\r\n".getBytes();
            boolean match = true;
            if (data.length == compareData.length) {
                for (int i = 0; i < data.length; i++) {
                    if (data[i] != compareData[i]) {
                        match = false;
                        break;
                    }
                }
            }

            assertTrue(duplicate.containsHeader("Test"));
            assertTrue(match);

            duplicate.destroy();

            // *****************************************************************
            // Test the Cookie APIs
            // *****************************************************************

            getResponse().clear();

            // Test setting cookies with strings
            // @Tested API - setCookie(String, String, HeaderKeys)
            // @Tested API - getCookie(String)

            getResponse().setCookie("TestCookie", "TestCookieValue",
                                    HttpHeaderKeys.HDR_SET_COOKIE);
            getResponse().setHeader(HttpHeaderKeys.HDR_SET_COOKIE,
                                    "TestCookie1=TestCookieValue1");
            HttpCookie cookie1 = getResponse().getCookie("TestCookie");
            HttpCookie cookie2 = getResponse().getCookie("TestCookie1");
            assertNotNull(cookie1);
            assertNotNull(cookie2);
            assertEquals(cookie1.getName(), "TestCookie");
            assertEquals(cookie1.getValue(), "TestCookieValue");
            assertEquals(cookie2.getName(), "TestCookie1");
            assertEquals(cookie2.getValue(), "TestCookieValue1");

            // Test a non-existant cookie

            HttpCookie cookie = getResponse().getCookie("nothere");
            assertNull(cookie);

            // Test setting cookies with actual Cookie object
            // @Tested API - setCookie(Cookie, HeaderKeys)

            getResponse().clear();
            cookie = new HttpCookie("TestCookie", "TestCookieValue");
            getResponse().setCookie(cookie, HttpHeaderKeys.HDR_SET_COOKIE);
            cookie = getResponse().getCookie("TestCookie");
            assertNotNull(cookie);
            assertEquals(cookie.getName(), "TestCookie");
            assertEquals(cookie.getValue(), "TestCookieValue");

            // Test adding multiple cookies
            // @Tested API - getAllCookies()

            getResponse().clear();
            getResponse().setCookie(new HttpCookie("TestCookie1", "TestCookieValue1"),
                                    HttpHeaderKeys.HDR_SET_COOKIE);
            getResponse().setCookie("TestCookie2", "TestCookieValue2",
                                    HttpHeaderKeys.HDR_SET_COOKIE);
            getResponse().setCookie(new HttpCookie("TestCookie3", "TestCookieValue3"),
                                    HttpHeaderKeys.HDR_SET_COOKIE);
            getResponse().setHeader(HttpHeaderKeys.HDR_SET_COOKIE,
                                    "TestCookie4=TestCookieValue4");
            List<HttpCookie> cookieList = getResponse().getAllCookies();
            assertNotNull(cookieList);
            assertEquals(4, cookieList.size());

            // Test removing some of those added cookies
            // @Tested API - removeCookie(String, HeaderKeys)

            assertTrue(getResponse().removeCookie(
                                                  "TestCookie2", HttpHeaderKeys.HDR_SET_COOKIE));
            assertTrue(getResponse().removeCookie(
                                                  "TestCookie3", HttpHeaderKeys.HDR_SET_COOKIE));
            cookieList = getResponse().getAllCookies();
            assertEquals(2, cookieList.size());

            // Test setting header directly first

            getResponse().clear();
            getResponse().setHeader(HttpHeaderKeys.HDR_SET_COOKIE,
                                    "TestCookie1=TestCookieValue1;"
                                                    + "TestCookie2=TestCookieValue2;"
                                                    + "TestCookie3=TestCookieValue3;"
                                                    + "TestCookie4=TestCookieValue4");
            cookieList = getResponse().getAllCookies();
            assertNotNull(cookieList);
            assertEquals(4, cookieList.size());

            // Test trying to add to a committed msg

            getResponse().clear();
            getResponse().setCommitted();
            assertFalse(getResponse().setCookie(
                                                new HttpCookie("C1", "V1"), HttpHeaderKeys.HDR_SET_COOKIE));
            assertFalse(getResponse().setCookie("c2", "v2", HttpHeaderKeys.HDR_SET_COOKIE));
            cookieList = getResponse().getAllCookies();
            assertEquals(0, cookieList.size());

            // Test error conditions now
            assertFalse(getResponse().setCookie(null, HttpHeaderKeys.HDR_SET_COOKIE));
            assertFalse(getResponse().setCookie(null, null, null));
            assertFalse(getResponse().removeCookie("TestCookie", null));
            assertFalse(getResponse().containsCookie(null, null));
            assertNull(getResponse().getCookie(null));

            // Test more variations on adding cookies and querying values
            // @Tested API - getCookieValue(String)

            getResponse().clear();
            getResponse().setHeader(HttpHeaderKeys.HDR_SET_COOKIE, "myCookie=myValue");
            byte[] val = getResponse().getCookieValue("myCookie");
            assertEquals("myValue", new String(val));

            // test case-insensitive non-matchs
            getResponse().setHeader(HttpHeaderKeys.HDR_SET_COOKIE, "myCookie=myValue");
            val = getResponse().getCookieValue("MYCOOKIE");
            assertNull(val);

            getResponse().setHeader(HttpHeaderKeys.HDR_SET_COOKIE2,
                                    "nullCookie; myCookie2=myValue");
            val = getResponse().getCookieValue("myCookie2");
            assertEquals("myValue", new String(val));

            // Test the getAllCookie by name apis
            // @Tested API - getAllCookieValues(String)
            // @Tested API - getAllCookies(String)

            getResponse().clear();
            getResponse().setHeader("Set-Cookie", "jsessionid=blah1;bogus=test;jsessionid=blah2");
            getResponse().setHeader("Set-Cookie2", "bogus2=test2;jsessionid=blah3");
            List<String> cvalues = getResponse().getAllCookieValues("jsessionid");
            assertEquals(3, cvalues.size());
            assertEquals("blah1", cvalues.get(0));
            assertEquals("blah2", cvalues.get(1));
            assertEquals("blah3", cvalues.get(2));

            getResponse().clear();
            getResponse().setHeader("Set-Cookie", "ws=this;id=that");
            getResponse().setHeader("Set-Cookie2", "id=something;id=otherthing");
            List<HttpCookie> list = getResponse().getAllCookies("id");
            assertEquals(3, list.size());
            assertEquals("that", list.get(0).getValue());
            assertEquals("something", list.get(1).getValue());
            assertEquals("otherthing", list.get(2).getValue());

            // *****************************************************************
            // Test the marshalling/parsing of the first line code
            // *****************************************************************

            getResponse().clear();

            // Test known first line bits

            getResponse().setVersion(VersionValues.V10);
            getResponse().setStatusCode(StatusCodes.ACCEPTED);
            getResponse().setReasonPhrase("Test Reason phrase");
            WsByteBuffer[] mData = null;
            boolean rc = false;
            // marshall the response first line
            mData = getResponse().marshallBinaryMessage();

            // parse the response first line
            for (int i = 0; i < mData.length; i++) {
                rc = getResponse().parseBinaryFirstLine(mData[i]);
            }
            assertTrue(rc);

            // Test unknown first line bits
            getResponse().clear();
            getResponse().setVersion(VersionValues.find("HTTP/3.0"));
            getResponse().setStatusCode(StatusCodes.CONFLICT);
            mData = null;
            rc = false;
            // marshall the response first line
            mData = getResponse().marshallBinaryMessage();

            // parse the response first line
            for (int i = 0; i < mData.length; i++) {
                rc = getResponse().parseBinaryFirstLine(mData[i]);
            }
            assertTrue(rc);

            // Test mixing known and unknown first line bits
            getResponse().clear();
            getResponse().setVersion(VersionValues.V10);
            getResponse().setStatusCode(StatusCodes.ACCEPTED);
            getResponse().setReasonPhrase("This is a test reason phrase".getBytes());
            getResponse().setHeader("TestUnknownHdr1", "test-cookie1".getBytes());
            getResponse().setHeader("TestUnknownHdr2", "test-cookie2".getBytes());
            mData = null;
            rc = false;
            // marshall the response
            mData = getResponse().marshallBinaryMessage();
            // parse the response
            for (int i = 0; i < mData.length; i++) {
                rc = getResponse().parseBinaryMessage(mData[i]);
            }
            assertTrue(rc);

            // *****************************************************************
            // Test the mime-type APIs
            // *****************************************************************

            getResponse().clear();

            // Test the charset APIs
            // @Tested API - getCharset()
            // @Tested API - setCharset()

            // test regular string
            getResponse().setHeader(HttpHeaderKeys.HDR_CONTENT_TYPE, "charset=UTF-8");
            assertEquals("UTF-8", getResponse().getCharset().toString());

            // test quoted string
            getResponse().setHeader(HttpHeaderKeys.HDR_CONTENT_TYPE, "charset=\"latin1\"");
            assertEquals("ISO-8859-1", getResponse().getCharset().toString());

            // test regular string with extra data after
            getResponse().setHeader(HttpHeaderKeys.HDR_CONTENT_TYPE, "charset=latin3;test");
            assertEquals("ISO-8859-3", getResponse().getCharset().toString());

            // test quoted string with extra data after
            getResponse().setHeader(HttpHeaderKeys.HDR_CONTENT_TYPE, "charset=\"latin4\";test");
            assertEquals("ISO-8859-4", getResponse().getCharset().toString());

            // test with preceding data
            getResponse().setHeader(HttpHeaderKeys.HDR_CONTENT_TYPE, "bogus;charset=ISO-2022-KR");
            assertEquals("ISO-2022-KR", getResponse().getCharset().toString());

            // test with no charset in header
            getResponse().setHeader(HttpHeaderKeys.HDR_CONTENT_TYPE, "nothing");
            assertEquals("ISO-8859-1", getResponse().getCharset().toString());

            // test with no header present
            getResponse().removeHeader(HttpHeaderKeys.HDR_CONTENT_TYPE);
            assertEquals("ISO-8859-1", getResponse().getCharset().toString());

            // test setting just the charset
            getResponse().removeHeader(HttpHeaderKeys.HDR_CONTENT_TYPE);
            getResponse().setCharset(Charset.forName("latin4"));
            assertEquals("ISO-8859-4", getResponse().getCharset().toString());
            assertEquals("text/html;charset=ISO-8859-4",
                         getResponse().getHeader(HttpHeaderKeys.HDR_CONTENT_TYPE).asString());

            // test overlaying the charset
            getResponse().setHeader(HttpHeaderKeys.HDR_CONTENT_TYPE, "text/html;charset=UTF-8");
            getResponse().setCharset(Charset.forName("latin1"));
            assertEquals("ISO-8859-1", getResponse().getCharset().toString());
            assertEquals("text/html;charset=ISO-8859-1",
                         getResponse().getHeader(HttpHeaderKeys.HDR_CONTENT_TYPE).asString());

            // Test the mimetype APIs
            // @Tested API - getMIMEType()
            // @Tested API - setMIMEType(String)

            // test simple header with just type
            getResponse().setHeader(HttpHeaderKeys.HDR_CONTENT_TYPE, "text/html");
            assertEquals("text/html", getResponse().getMIMEType());

            // test header with type;charset
            getResponse().setHeader(HttpHeaderKeys.HDR_CONTENT_TYPE, "text/html;charset=UTF-8");
            assertEquals("text/html", getResponse().getMIMEType());

            // test missing header
            getResponse().removeHeader(HttpHeaderKeys.HDR_CONTENT_TYPE);
            assertNull(getResponse().getMIMEType());

            // test setting just the mimetype
            getResponse().removeHeader(HttpHeaderKeys.HDR_CONTENT_TYPE);
            getResponse().setMIMEType("text/html");
            assertEquals("text/html", getResponse().getMIMEType());

            // test replacing an existing mimetype
            getResponse().setHeader(HttpHeaderKeys.HDR_CONTENT_TYPE, "text;charset=UTF-8");
            getResponse().setMIMEType("text/html");
            assertEquals("text/html;charset=UTF-8",
                         getResponse().getHeader(HttpHeaderKeys.HDR_CONTENT_TYPE).asString());

            // ****************************************************************
            // Test duplication
            // ****************************************************************

            getResponse().clear();
            getResponse().setStatusCode(500);
            getResponse().setReasonPhrase("bad reason");
            getResponse().setHeader("host", "localhost");
            getResponse().setHeader("Unknown", "value");
            getResponse().setCookie("c1", "v1", HttpHeaderKeys.HDR_SET_COOKIE);
            getResponse().setLimitOfTokenSize(500);
            HttpResponseMessageImpl r2 = (HttpResponseMessageImpl) getResponse().duplicate();
            assertEquals(getResponse().getLimitOfTokenSize(), r2.getLimitOfTokenSize());
            assertEquals("localhost", r2.getHeader("host").asString());
            assertEquals("value", r2.getHeader("unknown").asString());
            assertEquals(500, r2.getStatusCodeAsInt());
            assertEquals("bad reason", r2.getReasonPhrase());
            assertEquals("v1", r2.getCookie("c1").getValue());
            getResponse().setLimitOfTokenSize(16384);

            // 411712 - test for NPE after remove+append paths
            getResponse().removeHeader("host");
            getResponse().appendHeader("host", "l2");
            assertEquals(2, getResponse().getNumberOfHeaders());
            assertNotNull(getResponse().getCookie("c1"));
            r2 = (HttpResponseMessageImpl) getResponse().duplicate();
            assertEquals(2, r2.getNumberOfHeaders());

            // ****************************************************************
            // Test APIs for setting header values based on larger byte[]s
            // LIDB2356-41: byte[]/offset/length support
            // ****************************************************************

            // test appendHeader
            getResponse().clear();
            String sTemp = "0123456789abcdef0123";
            byte[] temp = sTemp.getBytes();
            getResponse().appendHeader("Host", temp, 0, 20);
            assertEquals(sTemp, getResponse().getHeader("Host").asString());
            getResponse().removeHeader("Host");
            getResponse().appendHeader("Host".getBytes(), temp, 10, 10);
            assertEquals(sTemp.substring(10, 20), getResponse().getHeader("Host").asString());
            getResponse().removeHeader("Host");
            getResponse().appendHeader(HttpHeaderKeys.HDR_HOST, temp, 0, 10);
            assertEquals(sTemp.substring(0, 10), getResponse().getHeader("Host").asString());
            getResponse().removeHeader("Host");

            // test setHeader
            getResponse().setHeader("Host", temp, 0, 20);
            assertEquals(sTemp, getResponse().getHeader("Host").asString());
            // don't remove, test the overlay
            getResponse().setHeader("Host".getBytes(), temp, 10, 10);
            assertEquals(sTemp.substring(10, 20), getResponse().getHeader("Host").asString());
            getResponse().removeHeader("Host");
            getResponse().setHeader(HttpHeaderKeys.HDR_HOST, temp, 0, 10);
            assertEquals(sTemp.substring(0, 10), getResponse().getHeader("Host").asString());
            getResponse().removeHeader("Host");

            // test marshalling
            getResponse().setHeader("Host", temp, 0, 10);
            getResponse().setHeader("T2", temp, 10, 5);
            getResponse().setHeader("ETag", temp, 15, 5);
            // marshall the response, should be adding the above header
            mData = duplicateBuffers(getResponse().marshallMessage());
            getResponse().clear();

            // parse the response
            for (int i = 0; i < mData.length; i++) {
                getResponse().parseMessage(mData[i], true);
            }
            assertEquals(sTemp.substring(0, 10), getResponse().getHeader("Host").asString());
            assertEquals(sTemp.substring(10, 15), getResponse().getHeader("T2").asString());
            assertEquals(sTemp.substring(15, 20), getResponse().getHeader("ETag").asString());

            // ****************************************************************
            // Test Server header control
            // ****************************************************************

            createNewMessage(HttpConfigConstants.PROPNAME_SERVER_HEADER_VALUE,
                             "TestServer/1.0");
            // marshall the response, should be adding the above header
            mData = duplicateBuffers(getResponse().marshallMessage());
            getResponse().clear();

            // parse the response
            for (int i = 0; i < mData.length; i++) {
                getResponse().parseMessage(mData[i], true);
            }
            assertEquals("TestServer/1.0", getResponse().getHeader("Server").asString());

            // marshall the response, test not-overriding existing header
            getResponse().setHeader("Server", "Explicit Value/0.9");
            mData = duplicateBuffers(getResponse().marshallMessage());
            getResponse().clear();

            // parse the response
            for (int i = 0; i < mData.length; i++) {
                getResponse().parseMessage(mData[i], true);
            }
            assertEquals("Explicit Value/0.9", getResponse().getHeader("Server").asString());

            createNewMessage(HttpConfigConstants.PROPNAME_REMOVE_SERVER_HEADER, "true");
            getResponse().setHeader("Server", "ShouldNotSeeThis/7.0");
            // marshall the response, should be removing the above header
            mData = duplicateBuffers(getResponse().marshallMessage());
            getResponse().clear();

            // parse the response
            for (int i = 0; i < mData.length; i++) {
                getResponse().parseMessage(mData[i], true);
            }
            assertNull(getResponse().getHeader("Server").asString());

            createNewMessage(HttpConfigConstants.PROPNAME_REMOVE_SERVER_HEADER, "false");
            getResponse().setHeader("Server", "ShouldSeeThis/4.0");
            // marshall the response, should be leaving the above header
            mData = duplicateBuffers(getResponse().marshallMessage());
            getResponse().clear();

            // parse the response
            for (int i = 0; i < mData.length; i++) {
                getResponse().parseMessage(mData[i], true);
            }
            assertEquals("ShouldSeeThis/4.0", getResponse().getHeader("Server").asString());

            // *****************************************************************
            // Test Cache-Control related headers
            // PK20531
            // *****************************************************************

            // test adding the cache headers
            getResponse().clear();
            getResponse().setHeader("Set-Cookie", "jsessionid=nocache_default");
            mData = duplicateBuffers(getResponse().marshallMessage());
            getResponse().clear();
            for (int i = 0; i < mData.length; i++) {
                getResponse().parseMessage(mData[i], true);
            }
            assertTrue(getResponse().containsHeader("Expires"));
            assertEquals("no-cache=\"set-cookie, set-cookie2\"",
                         getResponse().getHeader("Cache-Control").asString());

            // test adding Response Split
            getResponse().clear();
            getResponse().setHeader("split-header0", "split_value_BEGIN0_\u560A_END0:it");
            mData = duplicateBuffers(getResponse().marshallMessage());
            getResponse().clear();
            for (int i = 0; i < mData.length; i++) {
                getResponse().parseMessage(mData[i], true);
            }
            assertTrue(getResponse().containsHeader("split-header0"));
            assertEquals("split_value_BEGIN0_?_END0:it",
                         getResponse().getHeader("split-header0").asString());

            // test Response Split via append header
            getResponse().clear();
            getResponse().appendHeader("split-header1", "split_appendHeader_BEGIN1_\u570A_END1:it");
            mData = duplicateBuffers(getResponse().marshallMessage());
            getResponse().clear();
            for (int i = 0; i < mData.length; i++) {
                getResponse().parseMessage(mData[i], true);
            }
            assertTrue(getResponse().containsHeader("split-header1"));
            assertEquals("split_appendHeader_BEGIN1_?_END1:it",
                         getResponse().getHeader("split-header1").asString());
            
            getResponse().clear();
            getResponse().appendHeader("split-header2", "split_\r\n \n appendHeader_BEGIN2_\u570A_END2:it");
            mData = duplicateBuffers(getResponse().marshallMessage());
            getResponse().clear();
            for (int i = 0; i < mData.length; i++) {
                getResponse().parseMessage(mData[i], true);
            }
            assertTrue(getResponse().containsHeader("split-header2"));
            assertEquals("split_     appendHeader_BEGIN2_?_END2:it",
                         getResponse().getHeader("split-header2").asString());
            
            // test not adding the headers
            createNewMessage(HttpConfigConstants.PROPNAME_COOKIES_CONFIGURE_NOCACHE, "false");
            getResponse().setHeader("Set-Cookie", "jsessionid=nocache_false");
            mData = duplicateBuffers(getResponse().marshallMessage());
            getResponse().clear();
            for (int i = 0; i < mData.length; i++) {
                getResponse().parseMessage(mData[i], true);
            }
            assertFalse(getResponse().containsHeader("Expires"));
            assertFalse(getResponse().containsHeader("Cache-Control"));

            // test updating existing cache headers
            createNewMessage(HttpConfigConstants.PROPNAME_COOKIES_CONFIGURE_NOCACHE, "true");
            getResponse().setHeader("Expires", "test");
            getResponse().setHeader("Cache-Control", "no-cache");
            getResponse().setHeader("Set-Cookie", "jsessionid=blah");
            mData = duplicateBuffers(getResponse().marshallMessage());
            getResponse().clear();
            for (int i = 0; i < mData.length; i++) {
                getResponse().parseMessage(mData[i], true);
            }
            assertEquals("test", getResponse().getHeader("Expires").asString());
            assertEquals("no-cache", getResponse().getHeader("Cache-Control").asString());

            // PK24115 - test various content-lengths
            getResponse().setContentLength(-1);
            assertEquals(null, getResponse().getHeader("Content-length").asString());
            getResponse().setContentLength(-400);
            assertEquals(null, getResponse().getHeader("Content-length").asString());
            assertEquals(-1, getResponse().getContentLength());
            getResponse().setContentLength(0);
            assertEquals(0, getResponse().getContentLength());

            // Test setting long values                     
            getResponse().clear();
            getResponse().setContentLength(Integer.MAX_VALUE + 1L);
            assertEquals(Integer.MAX_VALUE + 1L, getResponse().getContentLength());
            getResponse().clear();
            getResponse().setContentLength(Long.MAX_VALUE);
            assertEquals(Long.MAX_VALUE, getResponse().getContentLength());
            getResponse().setHeader(HttpHeaderKeys.HDR_CONTENT_LENGTH, Long.toString(Integer.MAX_VALUE + 1L));
            assertEquals(Integer.MAX_VALUE + 1L, getResponse().getContentLength());
            getResponse().clear();
            getResponse().setHeader(HttpHeaderKeys.HDR_CONTENT_LENGTH, Long.toString(Long.MAX_VALUE));
            assertEquals(Long.MAX_VALUE, getResponse().getContentLength());

            // 371070 - test whitespace after unknown headers
            WsByteBuffer buff = ChannelFrameworkFactory.getBufferManager().allocate(8192);
            buff.clear();
            buff.put("HTTP/1.1 200 OK\r\nv:t1\r\nV  : testheader\r\nV : testheader2\r\n\r\n".getBytes());
            buff.flip();
            getResponse().clear();
            getResponse().parseMessage(buff, true);
            buff.release();
            assertEquals(3, getResponse().getNumberOfHeaderInstances("V"));
            assertEquals("t1", getResponse().getHeader("v").asString());
            assertEquals("testheader", getResponse().getHeaders("V").get(1).asString());
            assertEquals("testheader2", getResponse().getHeaders("v").get(2).asString());

            // 371070 - run one of the SIP message torture tests here
            String test = "HTTP/1.0 200 OK\r\n"
                          +
                          "Call-ID: 1-2496@9.51.252.37\r\n"
                          +
                          "CSeq: 3882340 INVITE\r\n"
                          +
                          "Via: SIP/2.0/TCP 9.51.252.37:5060\r\n"
                          +
                          "Via: SIP/2.0/TCP sip32.example.com\r\n"
                          +
                          "V: SIP/2.0/TCP sip31.example.com\r\n"
                          +
                          "Via: SIP/2.0/TCP sip30.example.com\r\n"
                          +
                          "ViA: SIP/2.0/TCP sip29.example.com\r\n"
                          +
                          "VIa: SIP/2.0/TCP sip28.example.com\r\n"
                          +
                          "VIA: SIP/2.0/TCP sip27.example.com\r\n"
                          +
                          "via: SIP/2.0/TCP sip26.example.com\r\n"
                          +
                          "viA: SIP/2.0/TCP sip25.example.com\r\n"
                          +
                          "vIa: SIP/2.0/TCP sip24.example.com\r\n"
                          +
                          "vIA: SIP/2.0/TCP sip23.example.com\r\n"
                          +
                          "V : SIP/2.0/TCP sip22.example.com\r\n"
                          +
                          "v : SIP/2.0/TCP sip21.example.com\r\n"
                          +
                          "V  : SIP/2.0/TCP sip20.example.com\r\n"
                          +
                          "v   : SIP/2.0/TCP sip19.example.com\r\n"
                          +
                          "Via: SIP/2.0/TCP sip18.example.com\r\n"
                          +
                          "Via: SIP/2.0/TCP sip17.example.com\r\n"
                          +
                          "Via: SIP/2.0/TCP sip16.example.com\r\n"
                          +
                          "Via: SIP/2.0/TCP sip15.example.com\r\n"
                          +
                          "Via: SIP/2.0/TCP sip14.example.com\r\n"
                          +
                          "Via: SIP/2.0/TCP sip13.example.com\r\n"
                          +
                          "Via: SIP/2.0/TCP sip12.example.com\r\n"
                          +
                          "Via: SIP/2.0/TCP sip11.example.com\r\n"
                          +
                          "Via: SIP/2.0/TCP sip10.example.com\r\n"
                          +
                          "Via: SIP/2.0/TCP sip9.example.com\r\n"
                          +
                          "Via: SIP/2.0/TCP sip8.example.com\r\n"
                          +
                          "Via: SIP/2.0/TCP sip7.example.com\r\n"
                          +
                          "Via: SIP/2.0/TCP sip6.example.com\r\n"
                          +
                          "Via: SIP/2.0/TCP sip5.example.com\r\n"
                          +
                          "Via: SIP/2.0/TCP sip4.example.com\r\n"
                          +
                          "Via: SIP/2.0/TCP sip3.example.com\r\n"
                          +
                          "Via: SIP/2.0/TCP sip2.example.com\r\n"
                          +
                          "Via: SIP/2.0/TCP sip1.example.com\r\n"
                          +
                          "Via: SIP/2.0/TCP host.example.com;received=192.0.2.5;branch=verylonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglongbranchvalue\r\n"
                          +
                          "Max-Forwards: 70\r\n"
                          +
                          "Contact: <sip:amazinglylongcallernameamazinglylongcallernameamazinglylongcallernameamazinglylongcallernameamazinglylongcallername@host5.example.net>\r\n"
                          +
                          "Content-Type: application/sdp\r\n"
                          +
                          "Content-Length: 152\r\n"
                          +
                          "F: sip:amazinglylongcallernameamazinglylongcallernameamazinglylongcallernameamazinglylongcallernameamazinglylongcallername@example.net;tag=12982982982982982982982982982982982982982982982982982982982982982982982982982982982982982982982982982982982982982982982982982982982982982982982982982982424;unknownheaderparamnamenamenamenamenamenamenamenamenamenamenamenamenamenamenamenamenamenamenamename=unknowheaderparamvaluevaluevaluevaluevaluevaluevaluevaluevaluevaluevaluevaluevaluevaluevalue;unknownValuelessparamnameparamnameparamnameparamnameparamnameparamnameparamnameparamnameparamnameparamname\r\n"
                          +
                          "Unknown-LongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLong-Name: unknown-longlonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglong-value; unknown-longlonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglong-parameter-name = unknown-longlonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglong-parameter-value\r\n"
                          +
                          "\r\n";
            buff = ChannelFrameworkFactory.getBufferManager().allocate(8192);
            buff.put(test.getBytes());
            buff.flip();
            getResponse().clear();
            getResponse().parseMessage(buff, true);
            buff.release();
            assertEquals(152, getResponse().getContentLength());
            assertEquals(5, getResponse().getNumberOfHeaderInstances("V"));
            assertEquals(29, getResponse().getNumberOfHeaderInstances("Via"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testMain", t);
        }
    }

}