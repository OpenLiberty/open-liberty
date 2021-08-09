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
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.http.channel.internal.cookies.CookieHeaderByteParser;
import com.ibm.ws.http.channel.internal.cookies.CookieUtils;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

/**
 * The Junit tests for the CookieHeaderByteParser.
 * 
 */
public class CookieHeaderByteParserTest {
    // To avoid having a test that starts failing once a year, always set cookies to expire sometime next year
    private static final String NEXT_YEAR = String.valueOf(Calendar.getInstance().get(Calendar.YEAR) + 1).substring(2);
    private static SharedOutputManager outputMgr;

    /** Reference to the cookie parser */
    private final CookieHeaderByteParser parser = new CookieHeaderByteParser();

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
    }

    /**
     * Get access to the parser object.
     * 
     * @return CookieHeaderByteParser
     */
    private CookieHeaderByteParser getParser() {
        return this.parser;
    }

    /**
     * Tests the parsing code.
     */
    @Test
    public void test1() {
        try {
            String cStr = "Version=1; Customer=\"WILE_E_COYOTE\"; Path=\"/acme\";"
                          + " Part_Number=\"Rocket_Launcher_0001\"; secure";
            List<HttpCookie> cList = getParser().parse(
                                                       cStr.getBytes(), HttpHeaderKeys.HDR_SET_COOKIE);
            assertEquals(2, cList.size());
            HttpCookie c = cList.get(0);
            assertEquals("Customer", c.getName());
            assertEquals("WILE_E_COYOTE", c.getValue());
            assertEquals("/acme", c.getPath());
            assertEquals(1, c.getVersion());
            c = cList.get(1);
            assertEquals("Part_Number", c.getName());
            assertEquals("Rocket_Launcher_0001", c.getValue());
            assertTrue(c.isSecure());
            assertEquals(1, c.getVersion());

            // Test Set-Cookie header with comma separated values

            cStr = "Version=1; Customer=\"WILE_E_COYOTE\", Path=\"/acme\","
                   + " Part_Number, secure";
            cList = getParser().parse(
                                      cStr.getBytes(), HttpHeaderKeys.HDR_SET_COOKIE);
            assertEquals(2, cList.size());
            c = cList.get(0);
            assertEquals("Customer", c.getName());
            assertEquals("WILE_E_COYOTE", c.getValue());
            assertEquals("/acme", c.getPath());
            assertEquals(1, c.getVersion());
            c = cList.get(1);
            assertEquals("Part_Number", c.getName());
            assertTrue(c.isSecure());
            assertEquals(1, c.getVersion());

            // Test parsing a Set-Cookie with alot of values filled in

            cStr = " Name=Bob; expires=Wednesday, 29-AUG-" + NEXT_YEAR + " 18:00:40 GMT;"
                   + " path=\"/Rohit\"; secure; Version =1; domain=ibm.com";
            cList = getParser().parse(
                                      cStr.getBytes(), HttpHeaderKeys.HDR_SET_COOKIE);
            assertEquals(1, cList.size());
            c = cList.get(0);
            assertEquals("Name", c.getName());
            assertEquals("Bob", c.getValue());
            assertEquals("/Rohit", c.getPath());
            assertTrue(c.isSecure());
            assertEquals(1, c.getVersion());
            assertTrue(0 < c.getMaxAge());
            assertEquals("ibm.com", c.getDomain());

            // Test parsing two cookies when just a name for one

            cStr = "MyNullCookie;MyCookie2=Rohit";
            cList = getParser().parse(
                                      cStr.getBytes(), HttpHeaderKeys.HDR_SET_COOKIE);
            assertEquals(2, cList.size());
            c = cList.get(0);
            assertEquals("MyNullCookie", c.getName());
            c = cList.get(1);
            assertEquals("MyCookie2", c.getName());
            assertEquals("Rohit", c.getValue());

            // Test parsing a Set-Cookie header with the "secure" flag on

            cStr = "Name=Rohit; secure ;expires=Wednesday,"
                   + " 22-AUG-" + NEXT_YEAR + " 18:00:40 GMT;";
            cList = getParser().parse(cStr.getBytes(), HttpHeaderKeys.HDR_SET_COOKIE);
            assertEquals(1, cList.size());
            c = cList.get(0);
            assertEquals("Name", c.getName());
            assertEquals("Rohit", c.getValue());
            assertTrue(c.isSecure());
            assertTrue(0 < c.getMaxAge());

            // Test parsing an empty cookie list

            cStr = ";;";
            cList = getParser().parse(cStr.getBytes(), HttpHeaderKeys.HDR_COOKIE);
            assertEquals(0, cList.size());

            // Test parsing multiple cookies with empty ones in the middle

            cStr = "Version =1;Name=Rohit; secure ; expires=Wednesday,"
                   + " 29-AUG-" + NEXT_YEAR + " 18:00:40 GMT;;;Family =Kelapure; secure;;";
            cList = getParser().parse(cStr.getBytes(), HttpHeaderKeys.HDR_SET_COOKIE);
            assertEquals(2, cList.size());
            c = cList.get(0);
            assertEquals("Name", c.getName());
            assertEquals("Rohit", c.getValue());
            assertTrue(0 < c.getMaxAge());
            assertEquals(1, c.getVersion());
            assertTrue(c.isSecure());
            c = cList.get(1);
            assertEquals("Family", c.getName());
            assertEquals("Kelapure", c.getValue());
            assertTrue(c.isSecure());
            assertEquals(1, c.getVersion());

            // Test parsing multiple cookies with empty ones at the end

            cStr = ";Name=test1;Version =1;; expires =;Family =Kelapure; secure;;";
            cList = getParser().parse(cStr.getBytes(), HttpHeaderKeys.HDR_SET_COOKIE);
            assertEquals(2, cList.size());
            c = cList.get(0);
            assertEquals("Name", c.getName());
            assertEquals("test1", c.getValue());
            assertEquals(1, c.getVersion());
            c = cList.get(1);
            assertEquals("Family", c.getName());
            assertEquals("Kelapure", c.getValue());
            assertTrue(c.isSecure());
            assertEquals(1, c.getVersion());

            // Test parsing multiple cookies with some missing full data

            cStr = ";Name1=test1;;Name2=test2;"
                   + "			MyNullCookie; MyNullCookie;expires"
                   + "=Wednesday, 29-AUG-" + NEXT_YEAR + " 18:00:40 GMT;"
                   + "TrialName=\"TrialTest\"; Path=\"/acme\"";
            cList = getParser().parse(cStr.getBytes(), HttpHeaderKeys.HDR_SET_COOKIE);
            assertEquals(5, cList.size());
            c = cList.get(0);
            assertEquals("Name1", c.getName());
            assertEquals("test1", c.getValue());
            c = cList.get(1);
            assertEquals("Name2", c.getName());
            assertEquals("test2", c.getValue());
            c = cList.get(2);
            assertEquals("MyNullCookie", c.getName());
            assertEquals("", c.getValue());
            c = cList.get(3);
            assertEquals("MyNullCookie", c.getName());
            assertEquals("", c.getValue());
            assertTrue(0 < c.getMaxAge());
            c = cList.get(4);
            assertEquals("TrialName", c.getName());
            assertEquals("TrialTest", c.getValue());
            assertEquals("/acme", c.getPath());

            // Test parsing multiple cookies with the Version changing in the middle

            cStr = "MyNullCookie;MyCookie2=TestCookie;"
                   + ""
                   + "	;$Version=\"1\"; Customer=\"WILE_E_COYOTE\"; $Path=\"/acme\"";
            cList = getParser().parse(cStr.getBytes(), HttpHeaderKeys.HDR_COOKIE);
            assertEquals(3, cList.size());
            c = cList.get(0);
            assertEquals("MyNullCookie", c.getName());
            assertEquals("", c.getValue());
            assertEquals(0, c.getVersion());
            c = cList.get(1);
            assertEquals("MyCookie2", c.getName());
            assertEquals("TestCookie", c.getValue());
            assertEquals(1, c.getVersion());
            c = cList.get(2);
            assertEquals("Customer", c.getName());
            assertEquals("WILE_E_COYOTE", c.getValue());
            assertEquals(1, c.getVersion());
            assertEquals("/acme", c.getPath());

            // Test attribute name minus $ as a valid cookie

            cStr = "prefix=http, server=LP02UT4, port=80, "
                   + "servletpath=_servlet-tests_GetRequestURLTest";
            cList = getParser().parse(cStr.getBytes(), HttpHeaderKeys.HDR_COOKIE);
            assertEquals(4, cList.size());
            c = cList.get(0);
            assertEquals("prefix", c.getName());
            assertEquals("http", c.getValue());
            c = cList.get(1);
            assertEquals("server", c.getName());
            assertEquals("LP02UT4", c.getValue());
            c = cList.get(2);
            assertEquals("port", c.getName());
            assertEquals("80", c.getValue());
            c = cList.get(3);
            assertEquals("servletpath", c.getName());
            assertEquals("_servlet-tests_GetRequestURLTest", c.getValue());

            // Test Port attribute with quoted comma delimited values

            cStr = "server=LP02UT4; $port=\"80,8080\", cookie2=v2";
            cList = getParser().parse(cStr.getBytes(), HttpHeaderKeys.HDR_COOKIE2);
            assertEquals(2, cList.size());
            c = cList.get(0);
            assertEquals("server", c.getName());
            assertEquals("LP02UT4", c.getValue());
            // can't actually query the Port from a J2EE cookie yet
            c = cList.get(1);
            assertEquals("cookie2", c.getName());
            assertEquals("v2", c.getValue());

            // test discard attribute
            cStr = "name=value; discard";
            cList = getParser().parse(cStr.getBytes(), HttpHeaderKeys.HDR_SET_COOKIE2);
            assertEquals(1, cList.size());
            c = cList.get(0);
            assertEquals("name", c.getName());
            assertEquals("value", c.getValue());
            assertTrue(c.isDiscard());
            c.setVersion(1);
            assertEquals("name=value; Version=1; Discard",
                         CookieUtils.toString(c, HttpHeaderKeys.HDR_SET_COOKIE2, false));
            assertEquals("name=value; Version=1; Max-Age=0",
                         CookieUtils.toString(c, HttpHeaderKeys.HDR_SET_COOKIE, false));
            assertEquals("name=value; Version=1; Discard",
                         CookieUtils.toString(c, HttpHeaderKeys.HDR_SET_COOKIE2, true));
            assertEquals("name=value; Version=1; Max-Age=0",
                         CookieUtils.toString(c, HttpHeaderKeys.HDR_SET_COOKIE, true));

            // test commenturl attribute
            cStr = "name=value; commenturl=something";
            cList = getParser().parse(cStr.getBytes(), HttpHeaderKeys.HDR_SET_COOKIE2);
            assertEquals(1, cList.size());
            c = cList.get(0);
            assertEquals("name", c.getName());
            assertEquals("value", c.getValue());
            assertEquals("something", c.getAttribute("CommentURL"));
            c.setVersion(1);
            assertEquals("name=value; Version=1; CommentURL=something",
                         CookieUtils.toString(c, HttpHeaderKeys.HDR_SET_COOKIE2, false));
            assertEquals("name=value; Version=1",
                         CookieUtils.toString(c, HttpHeaderKeys.HDR_SET_COOKIE, false));
            assertEquals("name=value; Version=1; CommentURL=something",
                         CookieUtils.toString(c, HttpHeaderKeys.HDR_SET_COOKIE2, true));
            assertEquals("name=value; Version=1",
                         CookieUtils.toString(c, HttpHeaderKeys.HDR_SET_COOKIE, true));

            // test HttpOnly attribute
            cStr = "name=value; HttpOnly";
            cList = getParser().parse(cStr.getBytes(), HttpHeaderKeys.HDR_SET_COOKIE2);
            assertEquals(1, cList.size());
            c = cList.get(0);
            assertEquals("name", c.getName());
            assertEquals("value", c.getValue());
            assertTrue(c.isHttpOnly());
            c.setVersion(1);
            assertEquals("name=value; Version=1; HttpOnly",
                         CookieUtils.toString(c, HttpHeaderKeys.HDR_SET_COOKIE2, false));
            assertEquals("name=value; Version=1; HttpOnly",
                         CookieUtils.toString(c, HttpHeaderKeys.HDR_SET_COOKIE, false));
            assertEquals("name=value; Version=1; HttpOnly",
                         CookieUtils.toString(c, HttpHeaderKeys.HDR_SET_COOKIE2, true));
            assertEquals("name=value; Version=1; HttpOnly",
                         CookieUtils.toString(c, HttpHeaderKeys.HDR_SET_COOKIE, true));
            c.setVersion(0);
            assertEquals("name=value; HttpOnly",
                         CookieUtils.toString(c, HttpHeaderKeys.HDR_SET_COOKIE, false));
            assertEquals("name=value; HttpOnly",
                         CookieUtils.toString(c, HttpHeaderKeys.HDR_SET_COOKIE, true));

        } catch (Throwable t) {
            outputMgr.failWithThrowable("test1", t);
        }
    }

}
