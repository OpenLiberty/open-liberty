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
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.genericbnf.internal.HeaderHandler;
import com.ibm.ws.http.channel.internal.HttpRequestMessageImpl;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

/**
 * Test code for the HeaderHandler class.
 */
public class HeaderHandlerTest {
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
     * Test method.
     */
    @Test
    public void testMain() {
        try {
            HttpRequestMessageImpl req = new HttpRequestMessageImpl();
            req.appendHeader(HttpHeaderKeys.HDR_CACHE_CONTROL,
                             ",,bogus  , no-CAche=\"Set-cookie, set-COOKie2\",   , ak=   testing,=");
            HeaderHandler handler = new HeaderHandler(req, ',', HttpHeaderKeys.HDR_CACHE_CONTROL);
            Iterator<String> i = handler.getValues();
            String[] testValues = { "bogus" };
            int count = 0;
            while (i.hasNext()) {
                assertEquals(testValues[count], i.next());
                count++;
            }
            assertEquals(1, count);

            // test no-cache block
            i = handler.getValues("no-cache");
            count = 0;
            testValues = new String[] { "set-cookie", "set-cookie2" };
            while (i.hasNext()) {
                assertEquals(testValues[count], i.next());
                count++;
            }
            assertEquals(2, count);

            // test random other apis
            assertFalse(handler.contains("notfound"));
            assertFalse(handler.contains("ak"));
            assertTrue(handler.containsKey("ak"));
            assertTrue(handler.contains("no-cache", "SET-COOKIE2"));
            assertFalse(handler.contains("notfound", "seriously"));
            assertTrue(handler.remove("no-cache", "Set-Cookie"));
            assertTrue(handler.contains("no-cache", "Set-cookie2"));
            String marshall = handler.marshall();
            assertTrue((-1 != marshall.indexOf("bogus")));
            assertTrue((-1 != marshall.indexOf("ak=testing")));
            assertTrue((-1 != marshall.indexOf("no-cache=set-cookie2")));
            assertEquals(1, handler.removeKey("no-Cache"));
            assertTrue(handler.add("more", "things"));
            assertTrue(handler.add("more", "evenmore"));
            assertFalse(handler.add("more", "evenmore"));
            marshall = handler.marshall();
            assertTrue((-1 != marshall.indexOf("bogus")));
            assertTrue((-1 != marshall.indexOf("ak=testing")));
            assertTrue((-1 != marshall.indexOf("more=\"things, evenmore\"")));
            assertEquals(4, handler.numValues());
            assertTrue(handler.remove("ak", "testing"));
            assertFalse(handler.remove("ak", "testing"));
            assertFalse(handler.containsKey("ak"));
            handler.clear();
            assertEquals("", handler.marshall());
            assertEquals(0, handler.numValues());

            // test single digit value
            req.setHeader("Cache-Control", "single= ,    max-age=0, no-cache  =  \"set-cookie  , s2  \"");
            handler = new HeaderHandler(req, ',', HttpHeaderKeys.HDR_CACHE_CONTROL);
            i = handler.getValues("max-age");
            assertTrue(i.hasNext());
            assertEquals("0", i.next());
            assertFalse(i.hasNext());
            marshall = handler.marshall();
            assertTrue((-1 != marshall.indexOf("single=\"\"")));
            assertTrue((-1 != marshall.indexOf("max-age=0")));
            assertTrue((-1 != marshall.indexOf("no-cache=\"set-cookie, s2\"")));
            handler.clear();

            req.setHeader("Cache-Control", "max-age=86400");
            req.setHeader("set-cookie", "jsessionid=test");
            handler = new HeaderHandler(req, ',', HttpHeaderKeys.HDR_CACHE_CONTROL);
            handler.add("no-cache", "set-cookie");
            assertEquals("max-age=86400, no-cache=set-cookie", handler.marshall());
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testMain", t);
        }
    }

}
