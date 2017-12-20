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
package com.ibm.ws.webcontainer40.srt.http;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.http.Cookie;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;

import com.ibm.websphere.servlet40.IRequest40;
import com.ibm.ws.webcontainer40.srt.SRTServletRequest40;
import com.ibm.wsspi.genericbnf.HeaderField;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.HttpRequest;

/**
 *
 */
public class HttpPushBuilderTest {

    // Use ClassImposteriser so we can mock classes in addition to interfaces
    private final Mockery context = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    final private IRequest40 IReq40 = context.mock(IRequest40.class);
    final private SRTServletRequest40 srtReq = context.mock(SRTServletRequest40.class);
    final private HttpRequest hReq = context.mock(HttpRequest.class);

    @Test
    public void testAPI_method() {

        // SRTServletRequest40 request, String sessionId, Enumeration<String> headerNames, Cookie[] addedCookies
        HttpPushBuilder pb = new HttpPushBuilder(srtReq, null, null, null);

        Set<String> headerNames = pb.getHeaderNames();
        assertTrue(headerNames.isEmpty());

        boolean caughtNullPointerException = false;
        try {
            pb.method(null);
        } catch (java.lang.NullPointerException exc) {
            caughtNullPointerException = true;
        }
        assertTrue(caughtNullPointerException);

        String[] invalidMethods = { "", "POST", "PUT", "DELETE", "CONNECT", "OPTIONS", "TRACE" };
        for (String invalidMethod : invalidMethods) {
            boolean caughtIllegalArgumentException = false;
            try {
                pb.method(invalidMethod);
            } catch (java.lang.IllegalArgumentException exc) {
                caughtIllegalArgumentException = true;
            }
            assertTrue(caughtIllegalArgumentException);
        }

        String testMethod = "GET";
        pb.method(testMethod);
        String method = pb.getMethod();
        assertTrue(method.equals(testMethod));

    }

    @Test
    public void testAPI_queryString() {

        HttpPushBuilder pb = new HttpPushBuilder(srtReq, null, null, null);

        String testQueryString = "test=queryString";
        pb.queryString(testQueryString);
        String queryString = pb.getQueryString();
        assertTrue(queryString.equals(testQueryString));
    }

    @Test
    public void testAPI_sessionId() {

        HttpPushBuilder pb = new HttpPushBuilder(srtReq, null, null, null);
        assertTrue(pb.getSessionId() == null);

        pb = new HttpPushBuilder(srtReq, "testInboundSessionId", null, null);
        assertTrue(pb.getSessionId().equals("testInboundSessionId"));

        String testSessionId = "testSessionID";
        pb.sessionId(testSessionId);
        String sessionId = pb.getSessionId();
        assertTrue(sessionId.equals(testSessionId));

    }

    @Test
    public void testAPI_path() throws Exception {

        HttpPushBuilder pb = new HttpPushBuilder(srtReq, null, null, null);

        // Test path
        String testPath = "/MyPushedResource";
        pb.path(testPath);
        String path = pb.getPath();
        assertTrue(path.equals(testPath));

        boolean caughtIllegalArgumentException = false;
        try {
            pb.method("GET");
            pb.path(null);
            pb.push();
        } catch (java.lang.IllegalStateException exc) {
            caughtIllegalArgumentException = true;
        }
        assertTrue(caughtIllegalArgumentException);

        pb.path("/testpath");
        pb.queryString("test=queryStringForPush");

        assertTrue(pb.getURI().equals("/testpath"));

        context.checking(new Expectations() {
            {

                oneOf(srtReq).getRequestURI();
                will(returnValue("/UnitTest/TestAPI_path"));

                oneOf(srtReq).getQueryString();
                will(returnValue("test=queryStringFromRequest"));

                oneOf(srtReq).getQueryString();
                will(returnValue("test=queryStringFromRequest"));

                oneOf(srtReq).getIRequest();
                will(returnValue(IReq40));

                oneOf(IReq40).getHttpRequest();
                will(returnValue(hReq));

                oneOf(hReq).pushNewRequest(pb);
            }
        });

        boolean caughtHttp2PushException = false;
        try {
            pb.push();
        } catch (java.lang.IllegalStateException exc) {
            caughtHttp2PushException = true;
        }
        assertFalse(caughtHttp2PushException);

        assertTrue(pb.getPath() == null);
        assertTrue(pb.getQueryString() == null);
        assertTrue("Referer header = " + pb.getHeader("Referer"),pb.getHeader("Referer").equals("/UnitTest/TestAPI_path?test=queryStringFromRequest"));
    }

    @Test
    public void testAPI_Headers() {
        HashMap<String, String> headers = new HashMap<String, String>();

        // Headers which should be part of the push request
        headers.put("Content-Type", "charset=pushypushpush");
        headers.put("Date", "Tue, 07 Feb 2017 13050:00 GMT");
        headers.put("From", "pushbuildertest@us.ibm.com");
        headers.put("MaxForwards", "99");

        StringTokenizer ctE = new StringTokenizer(headers.get("Content-Type"), "&");
        StringTokenizer dE = new StringTokenizer(headers.get("Date"), "&");
        StringTokenizer fE = new StringTokenizer(headers.get("From"), "&");
        StringTokenizer mfE = new StringTokenizer(headers.get("MaxForwards"), "&");

        context.checking(new Expectations() {
            {

                oneOf(srtReq).getHeaders("Content-Type");
                will(returnValue(ctE));

                oneOf(srtReq).getHeaders("Date");
                will(returnValue(dE));

                oneOf(srtReq).getHeaders("From");
                will(returnValue(fE));

                oneOf(srtReq).getHeaders("MaxForwards");
                will(returnValue(mfE));

            }
        });

        Enumeration headerList = Collections.enumeration(headers.keySet());

        HttpPushBuilder pb = new HttpPushBuilder(srtReq, null, headerList, null);

        assertTrue("Expected Content-Type " + headers.get("Content-Type") + " but was " + pb.getHeader("Content-Type"),pb.getHeader("Content-Type").equals(headers.get("Content-Type")));
        assertTrue("Expected Date " + headers.get("Date") + " but was " + pb.getHeader("Date"),pb.getHeader("Date").equals(headers.get("Date")));
        assertTrue("Expected From " + headers.get("From") + " but was " + pb.getHeader("From"),pb.getHeader("From").equals(headers.get("From")));
        assertTrue("Expected MaxForwards " + headers.get("MaxForwards") + " but was " + pb.getHeader("MaxForwards"),pb.getHeader("MaxForwards").equals(headers.get("MaxForwards")));

        Iterator<String> headerNames = pb.getHeaderNames().iterator();
        ArrayList<String> hNames = new ArrayList<String>();
        
        while (headerNames.hasNext()) {
            String name = headerNames.next();
            headers.remove(name);
            hNames.add(name);
        }
        assertTrue(headers.isEmpty());

        for (String name : hNames) {
            pb.removeHeader(name);
        }
        assertTrue(pb.getHeaders().isEmpty());

        pb.addHeader("testAddHeader", "testAddValue1");
        pb.addHeader("testAddHeader", "testAddValue2");
        pb.setHeader("testSetHeader", "testSetValue1");
        pb.setHeader("testSetHeader", "testSetValue2");
        
        Set<HeaderField> hdrs = pb.getHeaders();

        assertTrue(pb.getHeaderNames().size() == 2);
        
        boolean addValue1Found=false, addValue2Found=false,setValue2Found=false;
        
        Iterator<HeaderField> hdrsIterator = hdrs.iterator();
        while (hdrsIterator.hasNext()) {
            HeaderField hdr = hdrsIterator.next();
            if (hdr.getName().equals("testAddHeader")) {
                if (hdr.asString().equals("testAddValue1") && !addValue1Found) {
                    addValue1Found = true;
                } else if (hdr.asString().equals("testAddValue2") && !addValue2Found) {
                    addValue2Found = true;
                } else {
                    assertTrue("Unexpected header found : " + hdr.getName() + "=" + hdr.asString(),false);
                }
            } else if (hdr.getName().equals("testSetHeader")) {
                if (hdr.asString().equals("testSetValue2") && !setValue2Found) {
                    setValue2Found = true;
                } else {
                    assertTrue("Unexpected header found : " + hdr.getName() + "=" + hdr.asString(),false);
                }
            }
        }
        
        assertTrue("Expected testAddHeader value of testAddValue1 but it was not found", addValue1Found);
        assertTrue("Expected testAddHeader value of testAddValue2 but it was not found", addValue2Found);
        assertTrue("Expected testSetHeader value of testSetValue2 but it was not found", setValue2Found);
        

    }

    @Test
    public void test_pushBuilderCookies() {

        Cookie goodCookie = new Cookie("CookieGood", "CookieGoodValue");
        goodCookie.setMaxAge(10);
        goodCookie.setComment("Test Cookie");

        Cookie agedCookie = new Cookie("CookieAged", "CookieAgedValue");
        agedCookie.setMaxAge(0);

        Cookie[] cookies = { goodCookie, agedCookie };
        assertTrue(cookies.length == 2);
        assertTrue(goodCookie.getMaxAge() > 0);

        HttpPushBuilder pb = new HttpPushBuilder(srtReq, null, null, cookies);

        Set<HttpCookie> httpCookies = pb.getCookies();

        assertTrue(httpCookies.size() == 1);

        Iterator<HttpCookie> hCs = httpCookies.iterator();

        while (hCs.hasNext()) {
            HttpCookie hC = hCs.next();
            assertTrue(hC.getName().equals("CookieGood"));
            assertTrue(hC.getValue().equals("CookieGoodValue"));
            assertTrue(hC.getComment().equals("Test Cookie"));
            assertTrue(hC.getMaxAge() == 10);
        }

    }
    
    @Test
    public void testAPI_conditionalHeadersAfterPush() throws Exception {

        HashMap<String, String> headers = new HashMap<String, String>();

        //Headers which should be removed
        headers.put("If-Match", "match");
        headers.put("If-Modified-Since", "Tue, 07 Feb 2017 13050:00 GMT");
        headers.put("If-None-Match", "noneMatch");
        headers.put("If-Range", "range");
        headers.put("If-Unmodified-Since", "Tue, 07 Feb 2017 13050:00 GMT");
        
        StringTokenizer imE = new StringTokenizer(headers.get("If-Match"), "&");
        StringTokenizer imsE = new StringTokenizer(headers.get("If-Modified-Since"), "&");
        StringTokenizer inmE = new StringTokenizer(headers.get("If-None-Match"), "&");
        StringTokenizer irE = new StringTokenizer(headers.get("If-Range"), "&");
        StringTokenizer iusE = new StringTokenizer(headers.get("If-Unmodified-Since"), "&");


        context.checking(new Expectations() {
            {
                oneOf(srtReq).getHeaders("If-Match");
                will(returnValue(imE));
                
                oneOf(srtReq).getHeaders("If-Modified-Since");
                will(returnValue(imsE));
                
                oneOf(srtReq).getHeaders("If-None-Match");
                will(returnValue(inmE));
                
                oneOf(srtReq).getHeaders("If-Range");
                will(returnValue(irE));
                
                oneOf(srtReq).getHeaders("If-Unmodified-Since");
                will(returnValue(iusE));
            }
        });

        Enumeration headerList = Collections.enumeration(headers.keySet());

        HttpPushBuilder pb = new HttpPushBuilder(srtReq, null, headerList, null);

        context.checking(new Expectations() {
            {
                oneOf(srtReq).getRequestURI();
                will(returnValue("/UnitTest/TestAPI_path"));

                oneOf(srtReq).getIRequest();
                will(returnValue(IReq40));

                oneOf(IReq40).getHttpRequest();
                will(returnValue(hReq));

                allowing(hReq).pushNewRequest(pb);
                
                ignoring(srtReq).getQueryString();
            }
        });
        
        //Add the conditional headers to the PB
        for(Map.Entry<String, String> header : headers.entrySet()) {
            pb.addHeader(header.getKey(), header.getValue());
        }

        //Push, then check that the conditional headers were removed
        try {
            pb.path("/testPath");
            pb.push();
        }
        catch (Exception e) {
            fail("Exception thrown during push: " + e.getMessage());
        }
        
        for(Map.Entry<String, String> header : headers.entrySet())
            assertNull("Found unexpected " + header.getKey() + " header after push with value: " + pb.getHeader(header.getKey()), pb.getHeader(header.getKey()));

    }

}
