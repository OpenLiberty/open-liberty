/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.internal.utils;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

import com.ibm.ws.security.saml.sso20.common.CommonMockObjects;
/**
 *
 */
public class ForwardRequestInfoTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.saml.sso20.*=all");
    @Rule
    public TestRule managerRule = outputMgr;
    private static final CommonMockObjects common = new CommonMockObjects();
    private static final Mockery mockery = common.getMockery();
    private static HttpServletResponse response = common.getServletResponse();

    @Rule
    public final TestName testName = new TestName();
    private ForwardRequestInfo forwardRequest = null;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.resetStreams();
        outputMgr.restoreStreams();
        outputMgr.trace("*=all=disabled");
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        System.out.println("Entering test: " + testName.getMethodName());
        forwardRequest = new ForwardRequestInfo("http://idp/login");
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
        System.out.println("Exiting test: " + testName.getMethodName());
    }

    @Test
    public void testHandleFragmentCookiesAndNonce_JavaScript() {
        String methodName = "testHandleFragmentCookiesAndNonce_JavaScript";

        String js = forwardRequest.handleFragmentCookiesAndNonce(response);
        assertNotNull(js);
        //do a cursory check that we're getting back a JavaScript header
        String jslower = js.toLowerCase();
        assertTrue(jslower.contains("<script"));
        assertTrue(jslower.contains("</script>"));
    }

    @Test
    public void testHandleFragmentCookiesAndNonce_MaxAgeNotSet() {
        String methodName = "testHandleFragmentCookiesAndNoncez_MaxAgeNotSet";

        String js = forwardRequest.handleFragmentCookiesAndNonce(response);
        assertNotNull(js);
        //check for the default value
        assertTrue(js.contains("Max-Age="+String.valueOf(10*60)));
    }

    @Test
    public void testHandleFragmentCookiesAndNonce_MaxAgeSet() {
        String methodName = "testHandleFragmentCookiesAndNoncez_MaxAgeSet";

        long maxAgeSec = 300;   //5 minutes
        forwardRequest.setFragmentCookieMaxAge(maxAgeSec*1000); 
        String js = forwardRequest.handleFragmentCookiesAndNonce(response);
        assertNotNull(js);
        assertTrue(js.contains("Max-Age="+String.valueOf(maxAgeSec)));
    }

    @Test
    public void testHandleFragmentCookiesAndNonce_SetNonce() {
        String methodName = "testHandleFragmentCookiesAndNonce_SetNonce";
        mockery.checking(new Expectations() {
            {
                one(response).addHeader(with(any(String.class)), with(any(String.class)));
            }
        });

        // Nonce is now generated in redirectRequest() and stored on the instance.
        // Pre-set it here to simulate what redirectRequest() does before calling
        // handleFragmentCookiesAndNonce() directly.
        forwardRequest.setCspHeader("script-src 'self' 'nonce-%NONCE%' ; object-src 'self'; frame-src 'self'");
        forwardRequest.setNonce("testNonce1234567890123456789012");
        String js = forwardRequest.handleFragmentCookiesAndNonce(response);
        assertNotNull(js);
        assertTrue("Script block must contain the pre-set nonce attribute", js.contains("nonce="));
        assertTrue("Nonce value must match the pre-set value", js.contains("testNonce1234567890123456789012"));
    }

    @Test
    public void testHandleFragmentCookiesAndNonce_NoNonce() {
        String methodName = "testHandleFragmentCookiesAndNonce_NoNonce";
        mockery.checking(new Expectations() {
            {
                one(response).addHeader(with(any(String.class)), with(any(String.class)));
            }
        });

        forwardRequest.setCspHeader("script-src 'self' ; object-src 'self'; frame-src 'self'");
        String js = forwardRequest.handleFragmentCookiesAndNonce(response);
        assertNotNull(js);
        assertFalse(js.contains("nonce="));
    }

    @Test
    public void testHandleFragmentCookiesAndNonce_NoceNull() {
        String methodName = "testHandleFragmentCookiesAndNonce_NoceNull";
        mockery.checking(new Expectations() {
            {
                never(response).addHeader(with(any(String.class)), with(any(String.class)));
            }
        });

        forwardRequest.setCspHeader(null);
        String js = forwardRequest.handleFragmentCookiesAndNonce(response);
        assertNotNull(js);
        assertFalse(js.contains("nonce="));
    }

    // -----------------------------------------------------------------------
    // buildRedirectHtml() — CSP inline event handler fix tests
    // -----------------------------------------------------------------------

    /**
     * The &lt;BODY&gt; tag must NOT carry an onload attribute.
     * The former code emitted &lt;BODY onload="document.forms[0].submit()"&gt;;
     * the fix moves form submission to a &lt;SCRIPT&gt; block so that CSP nonces
     * can cover it (inline event handlers are not covered by nonces).
     */
    @Test
    public void testBuildRedirectHtml_bodyTagHasNoOnloadAttribute() throws Exception {
        ForwardRequestInfo fri = new ForwardRequestInfo("https://idp.example.com/saml/acs");
        fri.bNeedFragment = false; // isolate this test from the cookie/nonce script path
        fri.setParameter("SAMLResponse", new String[] { "dummyValue" });

        String html = fri.buildRedirectHtml(response);
        assertFalse("BODY tag must not carry an onload attribute after the CSP fix",
                    html.toLowerCase().contains("onload="));
    }

    /**
     * The form-submit &lt;SCRIPT&gt; block must appear after &lt;/FORM&gt;,
     * and it must invoke document.forms[0].submit().
     */
    @Test
    public void testBuildRedirectHtml_formSubmitScriptAppearsAfterFormEndTag() throws Exception {
        ForwardRequestInfo fri = new ForwardRequestInfo("https://idp.example.com/saml/acs");
        fri.bNeedFragment = false;
        fri.setParameter("SAMLResponse", new String[] { "dummyValue" });

        String html = fri.buildRedirectHtml(response);
        int formEnd = html.indexOf("</FORM>");
        int scriptPos = html.toLowerCase().indexOf("document.forms[0].submit()");
        assertTrue("</FORM> tag must be present in the generated HTML", formEnd >= 0);
        assertTrue("document.forms[0].submit() call must be present", scriptPos >= 0);
        assertTrue("form-submit script must appear after </FORM>", scriptPos > formEnd);
    }

    /**
     * When a CSP header with a nonce placeholder is configured, both the cookie
     * &lt;SCRIPT&gt; block and the form-submit &lt;SCRIPT&gt; block must share
     * the same nonce value generated once per redirect.
     */
    @Test
    public void testBuildRedirectHtml_bothScriptBlocksShareSameNonce() throws Exception {
        mockery.checking(new Expectations() {{
            // handleFragmentCookiesAndNonce sets the Content-Security-Policy header
            one(response).addHeader(with(equal("Content-Security-Policy")), with(any(String.class)));
        }});

        ForwardRequestInfo fri = new ForwardRequestInfo("https://idp.example.com/saml/acs");
        fri.bNeedFragment = true; // ensure handleFragmentCookiesAndNonce runs so both blocks share the nonce
        fri.setCspHeader("script-src 'self' 'nonce-%NONCE%'");
        fri.setParameter("SAMLResponse", new String[] { "dummyValue" });

        String html = fri.buildRedirectHtml(response);
        // Find all nonce="<value>" occurrences
        java.util.regex.Pattern noncePattern = java.util.regex.Pattern.compile("nonce=\"([^\"]+)\"");
        java.util.regex.Matcher m = noncePattern.matcher(html);
        String firstNonce = null;
        boolean allMatch = true;
        int count = 0;
        while (m.find()) {
            count++;
            if (firstNonce == null) {
                firstNonce = m.group(1);
            } else if (!firstNonce.equals(m.group(1))) {
                allMatch = false;
            }
        }
        assertTrue("At least one nonce attribute must be present in the generated HTML", count > 0);
        assertNotNull("Nonce value must not be null", firstNonce);
        assertTrue("All nonce attributes in the page must share the same value", allMatch);
    }

    /**
     * When the CSP header does not contain the %NONCE% placeholder, no nonce
     * should be generated and the form-submit &lt;SCRIPT&gt; block must have no
     * nonce attribute.
     */
    @Test
    public void testBuildRedirectHtml_noNonceWhenCspHasNoPlaceholder() throws Exception {
        mockery.checking(new Expectations() {{
            one(response).addHeader(with(equal("Content-Security-Policy")), with(any(String.class)));
        }});

        ForwardRequestInfo fri = new ForwardRequestInfo("https://idp.example.com/saml/acs");
        fri.bNeedFragment = true;
        fri.setCspHeader("script-src 'self'"); // no %NONCE% placeholder
        fri.setParameter("SAMLResponse", new String[] { "dummyValue" });

        String html = fri.buildRedirectHtml(response);
        assertFalse("No nonce attribute must appear when CSP has no %NONCE% placeholder",
                    html.contains("nonce="));
    }

    /**
     * When no CSP header is configured (null), the fix must still generate the
     * page without any onload attribute on &lt;BODY&gt;.
     */
    @Test
    public void testBuildRedirectHtml_bodyHasNoOnloadWhenCspHeaderIsNull() throws Exception {
        ForwardRequestInfo fri = new ForwardRequestInfo("https://idp.example.com/saml/acs");
        fri.bNeedFragment = false;
        // cspHeader deliberately left null
        fri.setParameter("SAMLResponse", new String[] { "dummyValue" });

        String html = fri.buildRedirectHtml(response);
        assertFalse("BODY tag must not carry an onload attribute when no CSP header is configured",
                    html.toLowerCase().contains("onload="));
        assertTrue("form-submit script call must still be present even without a CSP header",
                   html.contains("document.forms[0].submit()"));
    }

}
