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
package com.ibm.ws.webcontainer.security.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.webcontainer.security.AuthenticateApi;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

public class FormLogoutExtensionProcessorTest {

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final AuthenticateApi authApi = mock.mock(AuthenticateApi.class);
    private final IServletContext ctx = mock.mock(IServletContext.class);
    private final HttpServletRequest req = mock.mock(HttpServletRequest.class);
    private final HttpServletResponse resp = mock.mock(HttpServletResponse.class);
    private final WebAppConfig wac = mock.mock(WebAppConfig.class);
    private final WebAppSecurityConfig wasc = mock.mock(WebAppSecurityConfigImpl.class);

    private final String logoutPage = "http://localhost:9080/logout.html";

    /**
     * common set of Expectations shared by all the
     * test methods
     *
     */
    @Before
    public void setup() throws Exception {
        mock.checking(new Expectations() {
            {
                // code for FormLogoutExtensionProcessor ctor
                allowing(ctx).getWebAppConfig();
                will(returnValue(wac));
                allowing(wac).getApplicationName();
                will(returnValue("Unittest_Sample_App"));
                // TODO: Also manipulate absoluteUri = true?

                // formLogout()
                one(authApi).logout(req, resp, wasc);

                // verifyLogoutURL()
                allowing(wasc).getAllowLogoutPageRedirectToAnyHost();
                will(returnValue(false));

            }
        });
    }

    /**
     * Test successful logout.
     *
     * @throws Exception
     */
    @Test
    public void testLogoutSuccess() throws Exception {

        mock.checking(new Expectations() {
            {
                one(resp).getStatus();
                one(authApi).returnSubjectOnLogout();
                // redirectLogoutExitPage()
                one(req).getParameter("logoutExitPage");
                will(returnValue(logoutPage));

                // Back in redirectLogoutExitPage()
                // Make sure that the response is redirected to the logout page
                one(resp).encodeURL(logoutPage);
                will(returnValue(logoutPage));
                one(resp).sendRedirect(logoutPage);
                allowing(req).getCookies();
                allowing(wasc).getSSOCookieName();
            }
        });

        FormLogoutExtensionProcessor processorDouble = new FormLogoutExtensionProcessor(ctx, wasc, authApi);
        processorDouble.handleRequest(req, resp);
        mock.assertIsSatisfied();
    }

    /**
     * Test scenario where request URL host does not equal logout URL host.
     * Default logout should be used.
     *
     * @throws Exception
     */
    @Test
    public void testLogoutReqUrlHost_NotEqualsExitPageHost() throws Exception {

        final String otherHostLogoutURL = "http://sparky.austin.ibm.com:9080/logout.html";
        final java.io.PrintWriter pw = mock.mock(java.io.PrintWriter.class);

        mock.checking(new Expectations() {
            {
                one(resp).getStatus();
                one(authApi).returnSubjectOnLogout();

                // redirectLogoutExitPage(): we are using a different host than the one
                // in the request URL, so this is where this test flow diverges from the
                // "normal" scenario in the previous test.
                one(req).getParameter("logoutExitPage");
                will(returnValue(otherHostLogoutURL));

                // Because logout URL host is not localhost, acceptURL should be false
                // and flow should be in try block of isRequestURLEqualsExitPage()
                one(req).getRequestURL();
                will(returnValue(new StringBuffer("http://localhost:9080/snoop")));
                // Back in redirectLogoutExitPage(), flow should go into
                // useDefaultLogoutMsg(). We shouldn't be redirected to our
                // the specified logout URL because its hostname doesn't match
                // the request URL's.
                one(resp).getWriter();
                will(returnValue(pw));
                one(pw).println(FormLogoutExtensionProcessor.DEFAULT_LOGOUT_MSG);
                one(wasc).getLogoutPageRedirectDomainList();
                allowing(req).getCookies();
                allowing(wasc).getSSOCookieName();
                allowing(req).getRequestURL();
                allowing(req).getParameter("logoutExitPage");
            }
        });

        FormLogoutExtensionProcessor processorDouble = new FormLogoutExtensionProcessor(ctx, wasc, authApi);
        processorDouble.handleRequest(req, resp);
        mock.assertIsSatisfied();
    }

    @Test
    public void testIsLogoutPageURLValid() throws Exception {
        //List<String> logoutPageRedirectDomainList = new ArrayList<String>();
        //logoutPageRedirectDomainList.add("austin.ibm.com");
        final String otherHostLogoutURL = "http://localhost:9080 2 3 Location: http://localhost:9080";
        final java.io.PrintWriter pw = mock.mock(java.io.PrintWriter.class);

        mock.checking(new Expectations() {
            {
                one(req).getParameter("logoutExitPage");
                will(returnValue(otherHostLogoutURL));
                one(req).getRequestURL();
                will(returnValue(new StringBuffer("http://localhost:9080/snoop")));
                one(resp).getWriter();
                will(returnValue(pw));
                one(pw).println(FormLogoutExtensionProcessor.DEFAULT_LOGOUT_MSG);
                one(wasc).getLogoutPageRedirectDomainList();
            }
        });

        FormLogoutExtensionProcessor processorDouble = new FormLogoutExtensionProcessor(ctx, wasc, authApi);
        assertFalse(processorDouble.verifyLogoutURL(req, otherHostLogoutURL));
    }

    // Note that we cannot simulate code flow through if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) blocks
    @Test
    public void testIsLogoutPageMatchDomainNameList() throws Exception {
        List<String> logoutPageRedirectDomainList = new ArrayList<String>();
        logoutPageRedirectDomainList.add("austin.ibm.com");
        final String otherHostLogoutURL = "http://sparky.austin.ibm.com:9080/logout.html";
        final java.io.PrintWriter pw = mock.mock(java.io.PrintWriter.class);

        mock.checking(new Expectations() {
            {
                one(req).getParameter("logoutExitPage");
                will(returnValue(otherHostLogoutURL));
                one(req).getRequestURL();
                will(returnValue(new StringBuffer("http://localhost:9080/snoop")));
                one(resp).getWriter();
                will(returnValue(pw));
                one(pw).println(FormLogoutExtensionProcessor.DEFAULT_LOGOUT_MSG);
                one(wasc).getLogoutPageRedirectDomainList();
            }
        });

        FormLogoutExtensionProcessor processorDouble = new FormLogoutExtensionProcessor(ctx, wasc, authApi);
        assertTrue(processorDouble.isLogoutPageMatchDomainNameList(otherHostLogoutURL, "sparky.austin.ibm.com", logoutPageRedirectDomainList));
    }

    @Test
    public void testIsLogoutPageMatchDomainNameList_noMatch() throws Exception {
        List<String> logoutPageRedirectDomainList = new ArrayList<String>();
        logoutPageRedirectDomainList.add("austin.ibm.com");
        final String otherHostLogoutURL = "http://sparky.austin.ibm.com:9080/logout.html";
        final java.io.PrintWriter pw = mock.mock(java.io.PrintWriter.class);

        mock.checking(new Expectations() {
            {
                one(req).getParameter("logoutExitPage");
                will(returnValue(otherHostLogoutURL));
                one(req).getRequestURL();
                will(returnValue(new StringBuffer("http://localhost:9080/snoop")));
                one(resp).getWriter();
                will(returnValue(pw));
                one(pw).println(FormLogoutExtensionProcessor.DEFAULT_LOGOUT_MSG);
                one(wasc).getLogoutPageRedirectDomainList();
            }
        });

        FormLogoutExtensionProcessor processorDouble = new FormLogoutExtensionProcessor(ctx, wasc, authApi);
        assertFalse(processorDouble.isLogoutPageMatchDomainNameList(otherHostLogoutURL, "sparky.austin.ibm.com", null));
        List<String> logoutPageRedirectDomainList2 = new ArrayList<String>();
        logoutPageRedirectDomainList2.add("raleigh.ibm.com");
        assertFalse(processorDouble.isLogoutPageMatchDomainNameList(otherHostLogoutURL, "sparky.austin.ibm.com", logoutPageRedirectDomainList2));
    }

}
