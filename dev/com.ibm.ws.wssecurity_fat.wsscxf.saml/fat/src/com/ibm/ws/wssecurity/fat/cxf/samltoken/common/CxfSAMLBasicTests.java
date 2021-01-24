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

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
//import com.ibm.ws.security.fat.common.tooling.ValidationData.validationData;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTest;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
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
public class CxfSAMLBasicTests extends SAMLCommonTest {

    private static final Class<?> thisClass = CxfSAMLBasicTests.class;
    protected static CXFSAMLCommonUtils commonUtils = new CXFSAMLCommonUtils();
    protected static String servicePort = null;
    protected static String serviceSecurePort = null;

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, which invokes
     * a jax-ws cxf SAML web service.
     * Service client passes a flag with a value of "cxf" to indicate that a
     * user/password should be included in the service context. This is NOT
     * needed or used as we've specified SAML in the policy. Unlike using
     * usernametoken which doesn't require it either, in this case, it won't
     * even be looked at.
     * Test should succeed in accessing the server side service.
     *
     */
    //1/20/2021 added the FULL mode
    @Mode(TestMode.FULL)
    @AllowedFFDC(value = { "java.lang.Exception" })
    @Test
    public void CxfSAMLBasicTests_validUserPw_test() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, null, "user1", "user1pwd", "SAMLSOAPService2", "SAMLSoapPort2", "", "False", null, null);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings));

    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, which invokes
     * a jax-ws cxf SAML web service.
     * Service client passes a flag with a value of "ibm" to indicate that a
     * user/password should NOT be included in the service context. This is NOT
     * needed or used as we've specified SAML in the policy.
     * Test should succeed in accessing the server side service.
     *
     */
    @Mode(TestMode.LITE)
    @AllowedFFDC(value = { "java.lang.Exception" })
    @Test
    public void CxfSAMLBasicTests_noUserPw_test() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, null, null, null, "SAMLSOAPService2", "SAMLSoapPort2", "", "False", null, null);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings));

    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, which invokes
     * a jax-ws cxf SAML web service.
     * Service client passes a flag with a value of "cxfbadpswd" to indicate that a
     * user/badpassword should be included in the service context. This is NOT
     * needed or used as we've specified SAML in the policy. Unlike using
     * usernametoken which doesn't require it either, but will validate
     * it, in this case, it won't even be looked at.
     * Test should succeed in accessing the server side service.
     *
     */
    //1/20/2021 added the FULL mode
    @Mode(TestMode.FULL)
    @AllowedFFDC(value = { "java.lang.Exception" })
    @Test
    public void CxfSAMLBasicTests_validUserBadPw_test() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, null, "user1", "badpswd123", "SAMLSOAPService2", "SAMLSoapPort2", "", "False", null, null);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings));
        //		genericSAML(_testName, wc, updatedTestSettings, standardFlow, helpers.setErrorSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_SERVICE_UNAUTHORIZED));

    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, which invokes
     * a jax-ws cxf SAML web service.
     * Service client passes a flag with a value of "cxfbaduser" to indicate that a
     * baduser/password should be included in the service context. This is NOT
     * needed or used as we've specified SAML in the policy. Unlike using
     * usernametoken which doesn't require it either, but will validate
     * it, in this case, it won't even be looked at.
     * Test should succeed in accessing the server side service.
     *
     */
    //1/20/2021 added the FULL mode
    @Mode(TestMode.FULL)
    @AllowedFFDC(value = { "java.lang.Exception" })
    @Test
    public void CxfSAMLBasicTests_badUserValidPw_test() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, null, "baduser123", "security", "SAMLSOAPService2", "SAMLSoapPort2", "", "False", null, null);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings));
        //		genericSAML(_testName, wc, updatedTestSettings, standardFlow, helpers.setErrorSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_SERVICE_UNAUTHORIZED));

    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, which invokes
     * a jax-ws cxf SAML web service.
     * Service client passes a flag with a value of "ibm" to indicate that a
     * user/password should NOT be included in the service context. This is NOT
     * needed or used as we've specified SAML in the policy.
     * Test should succeed in accessing the server side service.
     *
     */
    //1/20/2021 added the FULL mode
    @Mode(TestMode.FULL)
    @AllowedFFDC(value = { "java.lang.Exception" })
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void CxfSAMLBasicTests_SAMLTokenMissingSignature_test() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, null, null, null, "SAMLSOAPService2", "SAMLSoapPort2", "", "False", null, null);
        updatedTestSettings.setRemoveTagInResponse("ds:Signature"); // the whole ds:Signature element

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE_AGAIN, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive message that the SAML Token did not validate.", SAMLMessageConstants.CWWKS5048E_ERROR_VERIFYING_SIGNATURE);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, expectations);

    }

    /**
     * TestDescription:
     *
     * In this scenario, the Web service provider expects Nonce and Created in the
     * Username token, but the client does not send Nonce and Created in the request.
     * The client request is expected to be rejected with an appropriate exception.
     *
     */

    //1/20/2021 added the FULL mode
    @Mode(TestMode.FULL)
    @Test
    public void CxfSAMLBasicTests_clientUserNameTokenPolicy_test() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        String expectedResponse = "{http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702}SamlToken";
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        updatedTestSettings.setCXFSettings(_testName, null, servicePort, serviceSecurePort, "user1", "user1pwd", "SAMLSOAPService2", "SAMLSoapPort2", "", "False", null, commonUtils.processClientWsdl("ClientNotSamlTokenWebSvc.wsdl", servicePort));

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setErrorSAMLCXFExpectations(null, flowType, updatedTestSettings, expectedResponse));
    }

}
