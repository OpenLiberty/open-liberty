/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.cxf.samltoken.common;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTest;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;
import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * The testcases in this class were ported from tWAS' test SamlWebSSOTests.
 * If a tWAS test is not applicable, it will be noted in the comments below.
 * If a tWAS test fits better into another test class, it will be noted
 * which test project/class it now resides in.
 * In general, these tests perform a simple IdP initiated SAML Web SSO, using
 * httpunit to simulate browser requests. In this scenario, a Web client
 * accesses a static Web page on IdP and obtains a a SAML HTTP-POST link
 * to an application installed on a WebSphere SP. When the Web client
 * invokes the SP application, it is redirected to a TFIM IdP which issues
 * a login challenge to the Web client. The Web Client fills in the login
 * form and after a successful login, receives a SAML 2.0 token from the
 * TFIM IdP. The client invokes the SP application by sending the SAML
 * 2.0 token in the HTTP POST request.
 */
@LibertyServerWrapper
//1/20/2021 the FULL mode at class level was already in CL FAT and will not work for LITE in OL, instead, we mix the FULL modes in the test case level below
//with the existing LITE mode
//@Mode(TestMode.FULL)
//1/21/2021 added
@RunWith(FATRunner.class)
public class CxfSSLSAMLBasicTests extends SAMLCommonTest {

    private static final Class<?> thisClass = CxfSSLSAMLBasicTests.class;
    protected static String servicePort = null;
    protected static String serviceSecurePort = null;
    protected static CXFSAMLCommonUtils commonUtils = new CXFSAMLCommonUtils();

    /**
     * TestDescription:
     * 
     * This test invokes a jax-ws cxf service client, which invokes
     * a jax-ws cxf SAML web service.
     * Transport/Https is in the server side policy.
     * The service client uses the server side policy.
     * The service client invokes the request using https.
     * Test should succeed in accessing the server side service.
     * 
     */
    @Mode(TestMode.LITE)
    @AllowedFFDC(value = { "java.lang.Exception" })
    @Test
    public void testSAMLCxfSvcClient_TransportEnabled() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, serviceSecurePort, "user1", "user1pwd", "SamlTokenTransportSecure",
                "SamlTokenTransportSecurePort", "", "False", null, null);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SSL_SAML_TOKEN_SERVICE));

    }

    /**
     * TestDescription:
     * 
     * This test invokes a jax-ws cxf service client, which invokes
     * a jax-ws cxf SAML web service.
     * Transport/Https is in the server side policy.
     * The service client uses the server side policy.
     * The service client invokes the request using http only.
     * Test should fail as policy can't be enforced.
     * 
     */
    //1/20/2021 added FULL mode
    @Mode(TestMode.FULL)
    @AllowedFFDC(value = { "java.lang.Exception" })
    @Test
    public void testSAMLCxfSvcClient_TransportEnabled_httpFromClient() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, null, "user1", "user1pwd", "SamlTokenTransportSecure",
                "SamlTokenTransportSecurePort", "", "False", null, null);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setErrorSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_SERVICE_HTTPS_NOT_USED));

    }

    /**
     * TestDescription:
     * 
     * This test invokes a jax-ws cxf service client, which invokes
     * a jax-ws cxf SAML web service.
     * Transport/Https is NOT in the server side policy.
     * The service client uses the server side policy.
     * The service client invokes the request using https.
     * Test should succeed in accessing the server side service as the client side is
     * "more secure" than the server
     * 
     */
    @Mode(TestMode.LITE)
    @AllowedFFDC(value = { "java.lang.Exception" })
    @Test
    public void testSAMLCxfSvcClient_TransportNotEnabled_httpsFromClient() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, serviceSecurePort, "user1", "user1pwd", "SAMLSOAPService2",
                "SAMLSoapPort2", "", "False", null, null);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings));

    }

}
