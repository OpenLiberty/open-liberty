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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.http.channel.internal.cookies.CookieUtils;
import com.ibm.ws.http.internal.HttpDateFormatImpl;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

/**
 * Test methods from the CookiesUtils class.
 * 
 */
public class CookieUtilsTest {
    private static SharedOutputManager outputMgr;

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
     * Primary tests for the CookieUtils class.
     */
    @Test
    public void testMain() {
        try {
            HttpCookie c = new HttpCookie("test", "longago");
            c.setMaxAge(0);
            assertEquals("test=longago; Expires=Thu, 01 Dec 1994 16:00:00 GMT",
                         CookieUtils.toString(c, HttpHeaderKeys.HDR_SET_COOKIE, true));
            assertEquals("test=longago; Expires=Thu, 01-Dec-94 16:00:00 GMT",
                         CookieUtils.toString(c, HttpHeaderKeys.HDR_SET_COOKIE, false));
            HttpCookie myCookie = new HttpCookie("CustomerName", "WILE_E_COYOTE");
            myCookie.setMaxAge(60);
            myCookie.setComment("Great Cookie!");
            myCookie.setPath("/acme");
            myCookie.setDomain("www.ibm.com");
            myCookie.setSecure(true);

            myCookie.setVersion(0);
            assertEquals("CustomerName=WILE_E_COYOTE; $Path=/acme",
                         CookieUtils.toString(myCookie, HttpHeaderKeys.HDR_COOKIE, true));

            myCookie.setVersion(1);
            assertEquals("CustomerName=WILE_E_COYOTE; $Version=1; $Path=\"/acme\"; $Domain=www.ibm.com",
                         CookieUtils.toString(myCookie, HttpHeaderKeys.HDR_COOKIE, true));

            myCookie.setVersion(0);
            myCookie.setMaxAge(Integer.MAX_VALUE);
            String out = CookieUtils.toString(myCookie, HttpHeaderKeys.HDR_SET_COOKIE, false);
            String prefix = "CustomerName=WILE_E_COYOTE; Comment=\"Great Cookie!\"; Expires=";
            String suffix = " GMT; Path=/acme; Domain=www.ibm.com; Secure";
            assertTrue(out.startsWith(prefix));
            assertTrue(out.endsWith(suffix));
            assertNotNull(new HttpDateFormatImpl().parseRFC2109Time(out.substring(prefix.length(), prefix.length() + 27)));

            out = CookieUtils.toString(myCookie, HttpHeaderKeys.HDR_SET_COOKIE, true);
            prefix = "CustomerName=WILE_E_COYOTE; Comment=\"Great Cookie!\"; Expires=";
            suffix = " GMT; Path=/acme; Domain=www.ibm.com; Secure";
            assertTrue(out.startsWith(prefix));
            assertTrue(out.endsWith(suffix));
            assertNotNull(new HttpDateFormatImpl().parseRFC1123Time(out.substring(prefix.length(), prefix.length() + 29)));

            myCookie.setVersion(1);
            assertEquals(
                         "CustomerName=WILE_E_COYOTE; Version=1; Comment=\"Great Cookie!\"; Domain=www.ibm.com; Max-Age=2147483647; Path=\"/acme\"; Secure",
                         CookieUtils.toString(myCookie, HttpHeaderKeys.HDR_SET_COOKIE2, true));

            assertEquals("CustomerName=WILE_E_COYOTE; $Path=/acme",
                         CookieUtils.toString(myCookie, HttpHeaderKeys.HDR_AGE, true));

            try {
                myCookie.setVersion(5);
                assert false;
            } catch (IllegalArgumentException e) {
                // expected exception
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testMain", t);
        }
    }

}
