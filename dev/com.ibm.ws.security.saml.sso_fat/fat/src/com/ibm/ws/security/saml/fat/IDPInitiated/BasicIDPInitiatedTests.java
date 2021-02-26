/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.fat.IDPInitiated;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.saml.fat.common.BasicSAMLTests;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;
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
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class BasicIDPInitiatedTests extends BasicSAMLTests {

    private static final Class<?> thisClass = BasicIDPInitiatedTests.class;

    @BeforeClass
    public static void setupBeforeTest() throws Exception {

        flowType = SAMLConstants.IDP_INITIATED;

        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");
        // add any additional messages that you want the "start" to wait for
        // we should wait for any providers that this test requires
        List<String> extraMsgs = getDefaultSAMLStartMsgs();

        List<String> extraApps = new ArrayList<String>();
        extraApps.add(SAMLConstants.SAML_CLIENT_APP);

        startSPWithIDPServer("com.ibm.ws.security.saml.sso-2.0_fat", "server_1.xml", extraMsgs, extraApps, true);

        testSAMLServer.addIgnoredServerException(SAMLMessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES);
    }

    //@Mode(TestMode.LITE)
    @Test
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    public void basicIDPInitiatedTests_noIdAssertNoUser_IDPSign_IDPEncrypt_FederationMismatch() throws Exception {

        testSAMLServer.reconfigServer("server_8_FedMismatch.xml", _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp5", true);
        updatedTestSettings.setSpecificIDPChallenge(2);

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML POST response", null, cttools.getResponseTitle(updatedTestSettings.getIdpRoot()));
        expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML Response", null, SAMLConstants.SAML_RESPONSE);
        //expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not fail finding the correct federation", null, SAMLMessageConstants.CWWKS5021E_IDP_METADATA_MISSING_ISSUER) ;
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not fail finding the correct federation", SAMLMessageConstants.CWWKS5045E_INVALID_ISSUER);

        genericSAML(_testName, webClient, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, expectations);

    }

    // TODO - chc - can't set the "relayState/RelayState" in the request with Shibboleth - they use the target by default
    // need to investigate hacking up the token...

    //    /**
    //     *
    //     * This testcase performs a simple IdP initiated SAML Web SSO, using
    //     * httpunit to simulate browser requests. In this scenario, a Web client
    //     * accesses a static Web page on IdP and obtains a a SAML HTTP-POST link
    //     * to an application installed on a WebSphere SP. When the Web client
    //     * invokes the SP application, it is redirected to a TFIM IdP which issues
    //     * a login challenge to the Web client. The Web Client fills in the login
    //     * form and after a successful login, receives a SAML 2.0 token from the
    //     * TFIM IdP. The client invokes the SP application by sending the SAML
    //     * 2.0 token in the HTTP POST request. In this sceanrio, the IdP is invoked
    //     * without specifying the target parameter (RelayState is not specified)
    //     * and there is no targetUrl property configured in TAI. Since the ACS
    //     * does not know which target application needs to be invoked, the HTTP
    //     * POST request is expected to fail with an appropriate exception.
    //     */
    //    //@Mode(TestMode.LITE)
    //    @Test
    //    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    //    public void basicIDPInitiatedTests_TargetAppMissing_noRelayState() throws Exception {
    //
    //        //testSAMLServer.reconfigServer("server_8.xml", _testName, Constants.NO_EXTRA_MSGS, Constants.JUNIT_REPORTING);
    //        WebClient webClient = getWebClient();
    //
    //        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
    //        updatedTestSettings.updatePartnerInSettings("sp1", true);
    //        updatedTestSettings.setSpTargetApp(null);
    //        updatedTestSettings.setSpAlternateApp(null);
    //        updatedTestSettings.setRelayState(null);
    //
    //        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
    //        //		expectations = vData.addResponseStatusExpectation(expectations, SAMLConstants.INVOKE_SP_WITH_SAML_RESPONSE, Constants.FORBIDDEN_STATUS)  ; // Constants.NOT_FOUND_STATUS
    //        //		expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML POST response", null, SAMLConstants.SAML_POST_RESPONSE);
    //        //		expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML Response", null, SAMLConstants.SAML_RESPONSE);
    //        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not received exception that target app was missing.", null, SAMLMessageConstants.CWWKS5041E_RELAY_STATE_PARAM_MISSING);
    //        //GORDON expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_SP_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not received exception that target app was missing.", null, "Error 404: CWWKS5003E") ;
    //        //GORDON expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_SP_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not received exception that target app was missing.", null, "provider as a valid request") ;
    //
    //        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, expectations);
    //
    //    }
    //
    //    //@Mode(TestMode.LITE)
    //    @Test
    //    public void basicIDPInitiatedTests_TargetAppMissing_relayStateSet() throws Exception {
    //
    //        WebClient webClient = getWebClient();
    //
    //        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
    //        updatedTestSettings.updatePartnerInSettings("sp1", true);
    ////chc        updatedTestSettings.setSpTargetApp(null);
    //        updatedTestSettings.setRelayState(updatedTestSettings.getSpAlternateApp());
    //
    //        List<validationData> expectations = vData.addSuccessStatusCodes();
    //        expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML POST response", null, cttools.getResponseTitle(updatedTestSettings.getIdpRoot()));
    //        expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML Response", null, SAMLConstants.SAML_RESPONSE);
    //        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get to the Simple Servlet", null, SAMLConstants.APP2_TITLE);
    //        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_DEFAULT_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not land alternate app", null, SAMLConstants.APP2_TITLE);
    //
    //        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, expectations);
    //
    //    }
    //
    //// TODO - chc - Shibboleth using the target to set the relayState - havne't figured out how to override that value - need to look into hacking up the saml token, but, not sure that'll do the trick
    //    //@Mode(TestMode.LITE)
    ////    @Test
    //    public void basicIDPInitiatedTests_TargetApp_relayState_mismatch() throws Exception {
    //
    //        WebClient webClient = getWebClient();
    //
    //        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
    //        updatedTestSettings.updatePartnerInSettings("sp1", true);
    //        updatedTestSettings.setRelayState(updatedTestSettings.getSpAlternateApp());
    //
    //        List<validationData> expectations = vData.addSuccessStatusCodes();
    //        expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML POST response", null, cttools.getResponseTitle(updatedTestSettings.getIdpRoot()));
    //        expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML Response", null, SAMLConstants.SAML_RESPONSE);
    //        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not get to the Snoop Servlet", null, SAMLConstants.APP1_TITLE);
    //        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_DEFAULT_APP, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not land on default app", null, SAMLConstants.APP1_TITLE);
    //
    //        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, expectations);
    //
    //    }

}
