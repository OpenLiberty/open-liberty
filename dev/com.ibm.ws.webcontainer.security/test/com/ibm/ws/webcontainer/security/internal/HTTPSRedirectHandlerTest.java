/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

import com.ibm.ws.webcontainer.security.WebRequest;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;

/**
 *
 */
public class HTTPSRedirectHandlerTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final WebRequest webRequest = mock.mock(WebRequest.class);
    private final HttpServletRequest req = mock.mock(HttpServletRequest.class);
    private final SRTServletRequest srtReq = mock.mock(SRTServletRequest.class);
    private final HTTPSRedirectHandler httpsRedirectHandler = new HTTPSRedirectHandler();

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.HTTPSRedirectHandler#shouldRedirectToHttps(com.ibm.ws.webcontainer.security.WebRequest)}.
     */
    @Test
    public void shouldRedirectToHttps_insecureRequestSSLNotRequired() {
        mock.checking(new Expectations() {
            {
                one(webRequest).getHttpServletRequest();
                will(returnValue(req));
                one(req).isSecure();
                will(returnValue(false));
                one(webRequest).isSSLRequired();
                will(returnValue(false));
            }
        });
        assertFalse(httpsRedirectHandler.shouldRedirectToHttps(webRequest));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.HTTPSRedirectHandler#shouldRedirectToHttps(com.ibm.ws.webcontainer.security.WebRequest)}.
     */
    @Test
    public void shouldRedirectToHttps_secureRequestSSLNotRequired() {
        mock.checking(new Expectations() {
            {
                one(webRequest).getHttpServletRequest();
                will(returnValue(req));
                one(req).isSecure();
                will(returnValue(true));
            }
        });
        assertFalse(httpsRedirectHandler.shouldRedirectToHttps(webRequest));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.HTTPSRedirectHandler#shouldRedirectToHttps(com.ibm.ws.webcontainer.security.WebRequest)}.
     */
    @Test
    public void shouldRedirectToHttps_secureRequestSSLRequired() {
        mock.checking(new Expectations() {
            {
                one(webRequest).getHttpServletRequest();
                will(returnValue(req));
                one(req).isSecure();
                will(returnValue(true));
            }
        });
        assertFalse(httpsRedirectHandler.shouldRedirectToHttps(webRequest));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.HTTPSRedirectHandler#shouldRedirectToHttps(com.ibm.ws.webcontainer.security.WebRequest)}.
     */
    @Test
    public void shouldRedirectToHttps_insecureRequestSSLRequired() {
        mock.checking(new Expectations() {
            {
                one(webRequest).getHttpServletRequest();
                will(returnValue(req));
                one(req).isSecure();
                will(returnValue(false));
                one(webRequest).isSSLRequired();
                will(returnValue(true));
            }
        });
        assertTrue(httpsRedirectHandler.shouldRedirectToHttps(webRequest));
    }

    /**
     * Check the output for the SSL port warning.
     * 
     * @param portIsNull TODO
     * @param urlMalformedExc TODO
     */
    private void checkForSSLPortError(boolean portIsNull, boolean urlMalformedExc) {
        if (portIsNull) {
            assertTrue(
                       "Expected message was not logged",
                       outputMgr.checkForStandardErr("CWWKS9113E:"));

        } else if (urlMalformedExc) {
            assertTrue(
                       "Expected message was not logged",
                       outputMgr.checkForStandardErr("CWWKS9114E:"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.HTTPSRedirectHandler#getHTTPSRedirectWebReply(javax.servlet.http.HttpServletRequest)}.
     * 
     * In the case where the HttpServletRequest is not what we expect, we fail to 403.
     */
    @Test
    public void getHTTPSRedirectWebReply_unexpectedHttpServletRequestObject() {
        WebReply reply = httpsRedirectHandler.getHTTPSRedirectWebReply(req);
        assertEquals("Web reply status code should be 403", HttpServletResponse.SC_FORBIDDEN, reply.getStatusCode());
        checkForSSLPortError(true, false);
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.HTTPSRedirectHandler#getHTTPSRedirectWebReply(javax.servlet.http.HttpServletRequest)}.
     */
    @Test
    public void getHTTPSRedirectWebReply_noHttpsPort() {
        mock.checking(new Expectations() {
            {
                one(srtReq).getPrivateAttribute("SecurityRedirectPort");
                will(returnValue(null));
            }
        });
        WebReply reply = httpsRedirectHandler.getHTTPSRedirectWebReply(srtReq);
        assertEquals("Web reply status code should be 403", HttpServletResponse.SC_FORBIDDEN, reply.getStatusCode());
        checkForSSLPortError(true, false);
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.HTTPSRedirectHandler#getHTTPSRedirectWebReply(javax.servlet.http.HttpServletRequest)}.
     */
    @Test
    public void getHTTPSRedirectWebReply_malformedURL() {
        final StringBuffer buff = new StringBuffer("host/request");
        mock.checking(new Expectations() {
            {
                one(srtReq).getPrivateAttribute("SecurityRedirectPort");
                will(returnValue(Integer.valueOf(9043)));
                one(srtReq).getRequestURL();
                will(returnValue(buff));
            }
        });
        WebReply reply = httpsRedirectHandler.getHTTPSRedirectWebReply(srtReq);
        assertEquals("Web reply status code should be 403", HttpServletResponse.SC_FORBIDDEN, reply.getStatusCode());
        checkForSSLPortError(false, true);
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.HTTPSRedirectHandler#getHTTPSRedirectWebReply(javax.servlet.http.HttpServletRequest)}.
     */
    @Test
    public void getHTTPSRedirectWebReply_validReplyWithNoExplicitPort() {
        final String httpsPort = "9043";
        final String queryString = "queryString";
        final StringBuffer buff = new StringBuffer("http://host/request");
        mock.checking(new Expectations() {
            {
                one(srtReq).getPrivateAttribute("SecurityRedirectPort");
                will(returnValue(Integer.valueOf(9043)));
                one(srtReq).getRequestURL();
                will(returnValue(buff));
                one(srtReq).getQueryString();
                will(returnValue(queryString));
            }
        });
        WebReply reply = httpsRedirectHandler.getHTTPSRedirectWebReply(srtReq);
        assertEquals("Web reply status code should be 302", HttpServletResponse.SC_MOVED_TEMPORARILY, reply.getStatusCode());
        String expectedURL = "https://host:" + httpsPort + "/request?" + queryString;
        assertEquals("Web reply message should have the redirect url", expectedURL, reply.message);
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.HTTPSRedirectHandler#getHTTPSRedirectWebReply(javax.servlet.http.HttpServletRequest)}.
     */
    @Test
    public void getHTTPSRedirectWebReply_validReplyWithNoQueryString() {
        final String httpsPort = "9043";
        final StringBuffer buff = new StringBuffer("http://host/request");
        mock.checking(new Expectations() {
            {
                one(srtReq).getPrivateAttribute("SecurityRedirectPort");
                will(returnValue(Integer.valueOf(9043)));
                one(srtReq).getRequestURL();
                will(returnValue(buff));
                one(srtReq).getQueryString();
                will(returnValue(null));
            }
        });
        WebReply reply = httpsRedirectHandler.getHTTPSRedirectWebReply(srtReq);
        assertEquals("Web reply status code should be 302", HttpServletResponse.SC_MOVED_TEMPORARILY, reply.getStatusCode());
        String expectedURL = "https://host:" + httpsPort + "/request";
        assertEquals("Web reply message should have the redirect url", expectedURL, reply.message);
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.HTTPSRedirectHandler#getHTTPSRedirectWebReply(javax.servlet.http.HttpServletRequest)}.
     */
    @Test
    public void getHTTPSRedirectWebReply_validReplyWithExplicitPort() {
        final String httpPort = "9080";
        final String httpsPort = "9043";
        final String queryString = "queryString";
        final StringBuffer buff = new StringBuffer("http://host:" + httpPort + "/request");
        mock.checking(new Expectations() {
            {
                one(srtReq).getPrivateAttribute("SecurityRedirectPort");
                will(returnValue(Integer.valueOf(9043)));
                one(srtReq).getRequestURL();
                will(returnValue(buff));
                one(srtReq).getQueryString();
                will(returnValue(queryString));
            }
        });
        WebReply reply = httpsRedirectHandler.getHTTPSRedirectWebReply(srtReq);
        assertEquals("Web reply status code should be 302", HttpServletResponse.SC_MOVED_TEMPORARILY, reply.getStatusCode());
        String expectedURL = "https://host:" + httpsPort + "/request?" + queryString;
        assertEquals("Web reply message should have the redirect url", expectedURL, reply.message);
    }
}
