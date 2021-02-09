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
package com.ibm.ws.security.saml.fat.common;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTest;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.SkipForRepeat;
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
public class BasicSAMLTests extends SAMLCommonTest {

    private static final Class<?> thisClass = BasicSAMLTests.class;

    private static final String MSG_CWWKO0219I_SSL_PORT_READY = "CWWKO0219I:.*ssl.*";

    // example of updating Partner and Federation
    //	SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
    //	// replace the hard coded "sp" with "spChris" as the partner name - needs to match
    //	// the server config currently being used (true for https url)
    //	updatedTestSettings.updatePartnerInSettings("spChris", true) ;
    //	// replace the federation name with the one at index location specified
    //	// refer to SAMLConstants.IDP_PROVIDER_LISTS for the list (true for https url)
    //	updatedTestSettings.updateFederationInSettings(0, true) ;

    @MinimumJavaLevel(javaLevel = 8)
    @Test
    @SkipForRepeat(SkipForRepeat.EE9_FEATURES) // jwtSso-1.0 is not EE9 compliant.
    public void basicSAMLTests_withJwtSsoFeature() throws Exception {
        List<String> extraMsgs = new ArrayList<String>();
        extraMsgs.add("CWWKS9122I:.*sp1/snoop");
        testSAMLServer.reconfigServer(buildSPServerName("server_1_withJwtSsoFeature.xml"), _testName, extraMsgs, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        String lastStep = standardFlowAltAppAgain[standardFlowAltAppAgain.length - 1];

        List<validationData> expectations = helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings);

        // Ensure that the subject principals include a JWT
        String issClaim = "\"iss\":\"https?://[^/]+/jwt/defaultJWT\"";
        String jwtUserPrincipal = "getUserPrincipal: \\{.+" + issClaim;
        expectations = vData.addExpectation(expectations, lastStep, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_MATCHES, "Did not find the expected JWT principal in the alternate app response but should have.", null, jwtUserPrincipal);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlowAltAppAgain, expectations);
    }

    /**
     *
     * tWAS GetVerWebSSOSrvlt01
     *
     * The test verifies that the intended
     * application on SP is invoked successfully by checking the response
     * received from the application. After the first application is invoked
     * successfully, the Web client extracts the LTPA cookie from the response
     * received from the SP, builds a new request, sets the extracted LTPA
     * cookie in the new request and invokes another protected application
     * on SP. Verify that there is no login challenge received from the
     * second application and it is invoked successfully. In this sceanrio,
     * the user ID that is used for authenticating with the IdP does not exist
     * in the WAS user registry and the SP Server config specifies that
     * idAssertion should be used.
     */

    @Test
    public void basicSAMLTests_idAssertNoUser_noIDPSign_noIDPEncrypt() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        String lastStep = standardFlowAltAppAgain[standardFlowAltAppAgain.length - 1];

        List<validationData> expectations = helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings);

        // Ensure that the subject principals do NOT include a JWT
        String jwtUserPrincipal = "getUserPrincipal: {";
        expectations = vData.addExpectation(expectations, lastStep, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_DOES_NOT_CONTAIN, "Found an unexpected JWT principal in the alternate app response.", null, jwtUserPrincipal);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlowAltAppAgain, expectations);
    }

    /**
     * tWAS GetVerWebSSOSrvlt02 - SP initiated will work in Liberty - not porting test - will implement sp initiated tests
     * in a new test class
     *
     * @throws Exception
     */

    /**
     *
     * tWAS GetVerWebSSOSrvlt03
     *
     * In this sceanrio, the user ID that is used for authenticating with the
     * IdP does not exist in the WAS user registry and the SP server config
     * does NOT indicate idAssertion. Also, the login.error.page
     * property is configured to point to a valid URL. Since the user does
     * not exist in the local user registry, the Web client request is
     * expected to fail and re-directed to the login error page.
     *
     */
    // skipping port of GetVerWebSSOSrvlt03 for now - this should fall under tWAS sp type support
    // login error page support not done yet

    /**
     *
     * tWAS GetVerWebSSOSrvlt04
     *
     * The test verifies that the intended
     * application on SP is invoked successfully by checking the response
     * received from the application. In this scenario, the user ID that is
     * used for authenticating with the IdP does exist in the WAS user registry
     * that is configured at the SP. The SAML TAI for this request is configured
     * with the idMap poperty value set to localRealm. Also, the IdP is
     * configured to use rsa-sha256 signature alogorithm for signing the
     * SAML token.
     */
    @Test
    public void basicSAMLTests_noIdAssert_IDPSignSha256_noIDPEncrypt() throws Exception {

        List<String> extraMsgs = getServerReconfigMessages();

        testSAMLServer.reconfigServer(buildSPServerName("server_4.xml"), _testName, extraMsgs, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp2", true);
        updatedTestSettings.setUseIdAssertion(false);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlowAltAppAgain, helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings));

    }

    /**
     *
     * tWAS GetVerWebSSOSrvlt05
     *
     * This testcase performs a simple IdP initiated SAML Web SSO, using
     * httpunit to simulate browser requests. In this scenario, a Web client
     * accesses a static Web page on IdP and obtains a a SAML HTTP-POST link
     * to an application installed on a WebSphere SP. When the Web client
     * invokes the SP application, it is redirected to a TFIM IdP which issues
     * a login challenge to the Web client. The Web Client fills in the login
     * form and after a successful login, receives a SAML 2.0 token from the
     * TFIM IdP. The client invokes the SP application by sending the SAML
     * 2.0 token in the HTTP POST request.
     * In this sceanrio, the user ID that is used for authenticating with the
     * IdP does not exist in the WAS user registry and the TAI custom property -
     * idMap is set to localRealm. Since the idAssertion property is not set
     * and the user does not exist in the local user registry, the test is expected
     * to fail with an appropriate exception.
     */

    //TODO - should we enable this?
    // skipping port of GetVerWebSSOSrvlt05 for now - this should fall under tWAS sp type support
    // @Test
    public void basicSAMLTests_noIdAssertNoUser_IDPSign_IDPEncrypt() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp13", true);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlowAltAppAgain, helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings));

    }

    // skipping port of GetVerWebSSOSrvlt06 - Liberty does not support localRealmThenAssertion - haven't gotten through all the tests, but maybe this would a good place for a group test.

    // skipping port of GetVerWebSSOSrvlt07 - Liberty does not support trustAnySigner

    /**
     *
     * tWAS GetVerWebSSOSrvlt08
     *
     * This testcase performs a simple IdP initiated SAML Web SSO, using
     * httpunit to simulate browser requests. In this scenario, a Web client
     * accesses a static Web page on IdP and obtains a a SAML HTTP-POST link
     * to an application installed on a WebSphere SP. When the Web client
     * invokes the SP application, it is redirected to a TFIM IdP which issues
     * a login challenge to the Web client. The Web Client fills in the login
     * form and after a successful login, receives a SAML 2.0 token from the
     * TFIM IdP. The client invokes the SP application by sending the SAML
     * 2.0 token in the HTTP POST request.
     * In this sceanrio, the IdP is configured to sign the SAML token with the
     * soaprequester key from dsig-sender.ks and the truststore of the SP
     * does not contain the soaprequester key to validate the signature.
     * Since the SAML signature cannot be validated by SP's assertion
     * consumer service, the request is expected to be rejected with an
     * appropriate exception.
     *
     */

    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @AllowedFFDC(value = { "org.opensaml.ws.security.SecurityPolicyException" })
    @Test
    public void basicSAMLTests_noIdAssertNoUser_IDPSignMisMatch_IDPEncrypt() throws Exception {

        List<String> extraMsgs = getServerReconfigMessages();

        testSAMLServer.reconfigServer(buildSPServerName("server_8.xml"), _testName, extraMsgs, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp5", true);
        updatedTestSettings.setSpecificIDPChallenge(2);

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML POST response", null, cttools.getResponseTitle(updatedTestSettings.getIdpRoot()));
        expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML Response", null, SAMLConstants.SAML_RESPONSE);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive exception that the SAML Token did not validate.", SAMLMessageConstants.CWWKS5049E_SIGNATURE_NOT_TRUSTED_OR_VALID);
        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, expectations);

    }

    //	TODO why is basicSAMLTests_noIdAssertNoUser_IDPSign_IDPEncrypt_FederationMismatch commented out?
    ////	@Mode(TestMode.LITE)
    //	@Test
    //	@ExpectedFFDC(value={"com.ibm.ws.security.saml.error.SamlException"})
    //	public void basicSAMLTests_noIdAssertNoUser_IDPSign_IDPEncrypt_FederationMismatch() throws Exception {
    //
    //		testSAMLServer.reconfigServer(buildSPServerName("server_8_FedMismatch.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    //		// Create the conversation object which will maintain state for us
    //		WebConversation wc = new WebConversation();
    //
    //		SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
    //		updatedTestSettings.updatePartnerInSettings("sp5", true) ;
    //		updatedTestSettings.setSpecificIDPChallenge(2) ;
    //
    //		List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_SP_WITH_SAML_RESPONSE, null);
    //		expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML POST response", null, SAMLConstants.SAML_POST_RESPONSE);
    //		expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML Response", null, SAMLConstants.SAML_RESPONSE);
    //		expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_SP_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not fail finding the correct federation", null, SAMLMessageConstants.CWWKS5021E_IDP_METADATA_MISSING_ISSUER) ;
    //		expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_SP_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not fail finding the correct federation", null, "CWWKS5066E");
    //
    //		genericSAML(_testName, wc, updatedTestSettings, standardFlow, expectations);
    //
    //	}

    //	/**
    //	 *
    //	 * This testcase performs a simple IdP initiated SAML Web SSO, using
    //	 * httpunit to simulate browser requests. In this scenario, a Web client
    //	 * accesses a static Web page on IdP and obtains a a SAML HTTP-POST link
    //	 * to an application installed on a WebSphere SP.  When the Web client
    //	 * invokes the SP application, it is redirected to a TFIM IdP which issues
    //	 * a login challenge to the Web client. The Web Client fills in the login
    //	 * form and after a successful login, receives a SAML 2.0 token from the
    //	 * TFIM IdP.  The client invokes the SP application by sending the SAML
    //	 * 2.0 token in the HTTP POST request. In this sceanrio, the IdP is invoked
    //	 * without specifying the target parameter (RelayState is not specified)
    //	 * and there is no targetUrl property configured in TAI. Since the ACS
    //	 * does not know which target application needs to be invoked, the HTTP
    //	 * POST request is expected to fail with an appropriate exception.
    //	 */
    ////	@Mode(TestMode.LITE)
    //	@Test
    //	@ExpectedFFDC(value={"com.ibm.ws.security.saml.error.SamlException"})
    //	public void basicSAMLTests_TargetAppMissing_noRelayState() throws Exception {
    //
    //		//testSAMLServer.reconfigServer(buildSPServerName("server_8.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    //		// Create the conversation object which will maintain state for us
    //		WebConversation wc = new WebConversation();
    //
    //		SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
    //		updatedTestSettings.updatePartnerInSettings("sp1", true) ;
    //		updatedTestSettings.setSpTargetApp(null) ;
    //		updatedTestSettings.setSpAlternateApp(null) ;
    //
    //		List<validationData> expectations = vData.addSuccessStatusCodes(null, SAMLConstants.INVOKE_SP_WITH_SAML_RESPONSE);
    //		expectations = vData.addResponseStatusExpectation(expectations, SAMLConstants.INVOKE_SP_WITH_SAML_RESPONSE, SAMLConstants.FORBIDDEN_STATUS)  ; // SAMLConstants.NOT_FOUND_STATUS
    //		expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML POST response", null, SAMLConstants.SAML_POST_RESPONSE);
    //		expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML Response", null, SAMLConstants.SAML_RESPONSE);
    //		expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_SP_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not received exception that target app was missing.", null, SAMLMessageConstants.CWWKS5041E_RELAY_STATE_PARAM_MISSING) ;
    //		//GORDON expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_SP_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not received exception that target app was missing.", null, "Error 404: CWWKS5003E") ;
    //		//GORDON expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_SP_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not received exception that target app was missing.", null, "provider as a valid request") ;
    //
    //		genericSAML(_testName, wc, updatedTestSettings, standardFlow, expectations);
    //
    //	}

    //	@Mode(TestMode.LITE)
    //	@Test
    //	public void basicSAMLTests_TargetAppMissing_noRelayState_useSAMLTokenAnyway() throws Exception {
    //
    //		//testSAMLServer.reconfigServer(buildSPServerName("server_8.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    //		// Create the conversation object which will maintain state for us
    //		WebConversation wc = new WebConversation();
    //
    //		SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
    //		updatedTestSettings.updatePartnerInSettings("sp1", true) ;
    //		updatedTestSettings.setSpTargetApp(null) ;
    //
    //		List<validationData> expectations = vData.addSuccessStatusCodes(null, SAMLConstants.INVOKE_SP_WITH_SAML_RESPONSE);
    //		expectations = vData.addResponseStatusExpectation(expectations, SAMLConstants.INVOKE_SP_WITH_SAML_RESPONSE, SAMLConstants.NOT_FOUND_STATUS)  ;
    //		expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML POST response", null, SAMLConstants.SAML_POST_RESPONSE);
    //		expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML Response", null, SAMLConstants.SAML_RESPONSE);
    //		expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_SP_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not received exception that target app was missing.", null, SAMLMessageConstants.CWWKS5003E_ENDPOINT_NOT_SUPPORTED) ;
    //		expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_DEFAULT_APP, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not land on default app", null, SAMLConstants.APP1_TITLE);
    //
    //		genericSAML(_testName, wc, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW_INVOKE_DEF_APP_AGAIN, expectations);
    //
    //	}

    // skipping port of GetVerWebSSOSrvlt10 - Liberty does not support targetUrl in the server.xml (like we had TAI support in tWAS), nor does it have useRelayStateForTarget
    // skipping port of GetVerWebSSOSrvlt11 - Liberty does not support targetUrl in the server.xml (like we had TAI support in tWAS), nor does it have useRelayStateForTarget

    ////	@Mode(TestMode.LITE)
    //	@Test
    //	public void basicSAMLTests_TargetAppMissing_relayStateSet() throws Exception {
    //
    //		// Create the conversation object which will maintain state for us
    //		WebConversation wc = new WebConversation();
    //
    //		SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
    //		updatedTestSettings.updatePartnerInSettings("sp1", true) ;
    //		updatedTestSettings.setSpTargetApp(null) ;
    //		updatedTestSettings.setRelayState(updatedTestSettings.getSpAlternateApp()) ;
    //
    //		List<validationData> expectations = vData.addSuccessStatusCodes();
    //		expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML POST response", null, SAMLConstants.SAML_POST_RESPONSE);
    //		expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML Response", null, SAMLConstants.SAML_RESPONSE);
    //		expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_SP_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get to the Simple Servlet", null, SAMLConstants.APP2_TITLE) ;
    //		expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_DEFAULT_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not land alternate app", null, SAMLConstants.APP2_TITLE);
    //
    //		genericSAML(_testName, wc, updatedTestSettings, standardFlow, expectations);
    //
    //	}
    //
    ////	@Mode(TestMode.LITE)
    //	@Test
    //	public void basicSAMLTests_TargetApp_relayState_mismatch() throws Exception {
    //
    //		WebConversation wc = new WebConversation();
    //
    //		SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
    //		updatedTestSettings.updatePartnerInSettings("sp1", true) ;
    //		updatedTestSettings.setRelayState(updatedTestSettings.getSpAlternateApp()) ;
    //
    //		List<validationData> expectations = vData.addSuccessStatusCodes();
    //		expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML POST response", null, SAMLConstants.SAML_POST_RESPONSE);
    //		expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML Response", null, SAMLConstants.SAML_RESPONSE);
    //		expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_SP_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not get to the Snoop Servlet", null, SAMLConstants.APP1_TITLE) ;
    //		expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_DEFAULT_APP, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not land on default app", null, SAMLConstants.APP1_TITLE);
    //
    //		genericSAML(_testName, wc, updatedTestSettings, standardFlow, expectations);
    //
    //	}

    /**
     * * The test verifies that the intended
     * application on SP is invoked successfully by checking the response
     * received from the application. After the first application is invoked
     * successfully, the Web client extracts the LTPA cookie from the response
     * received from the SP, builds a new request, sets the extracted LTPA
     * cookie in the new request and invokes another protected application
     * on SP. Verify that there is no login challenge received from the
     * second application and it is invoked successfully. In this sceanrio,
     * the user ID that is used for authenticating with the IdP does not exist
     * in the WAS user registry and the SP Server config specifies that
     * idAssertion should be used. SAML token is encrypted.
     *
     * @throws Exception
     */
    @Test
    public void basicSAMLTests_noIdAssert_IDPSign_IDPEncrypt() throws Exception {

        List<String> extraMsgs = getServerReconfigMessages();

        testSAMLServer.reconfigServer(buildSPServerName("server_13.xml"), _testName, extraMsgs, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp13", true);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlowAltAppAgain, helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings));

    }

    // my creations

    /***
     * This test has a SP server config that specifies a filter that will not match
     * the app that we're invoking. This test should show that we authenticate on
     * the IDP (because that's where we start the process), but, we will end up
     * getting a normal form login because the requested app does not match the SAML filter.
     * Security will use the "default" auth mechanism defined for this app.
     *
     * @throws Exception
     */
    @Test
    public void basicSAMLTests_idAssertNoUser_noIDPSign_noIDPEncrypt_badFilter() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_1_badFilter.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        List<validationData> expectations = vData.addSuccessStatusCodes();
        expectations = vData.addExpectation(expectations, SAMLConstants.BUILD_POST_IDP_INITIATED_REQUEST, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not land on the IDP form login form.", null, cttools.getLoginTitle(updatedTestSettings.getIdpRoot()));
        expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML POST response", null, cttools.getResponseTitle(updatedTestSettings.getIdpRoot()));
        expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML Response", null, SAMLConstants.SAML_RESPONSE);
        // nothing will be logged in the SP about the filter
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not land on the normal form login form.", null, SAMLConstants.STANDARD_LOGIN_HEADER);
        expectations = vData.addExpectation(expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not land on the normal form login form.", null, SAMLConstants.STANDARD_LOGIN_HEADER);
        expectations = vData.addExpectation(expectations, SAMLConstants.PROCESS_FORM_LOGIN, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get output showing that there is NO SAML token - it should not be there", null, SAMLConstants.NO_SAML_TOKEN_FOUND_MSG);

        genericSAML(_testName, webClient, updatedTestSettings, formLoginFlow, expectations);

    }

    // This test has an SP server config that does not specify a filter - therefore, ALL apps are protected
    // by SAML.  Make sure we get the normal SAML flow and get to the app.
    @Test
    public void basicSAMLTests_noIdAssertNoUser_noIDPSign_noIDPEncrypt_noFilter() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_1_noFilter.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        testSAMLServer.addIgnoredServerException(SAMLMessageConstants.CWWKG0033W_REF_VALUE_NOT_FOUND_IN_CONFIG);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlowAltAppAgain, helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings));

    }

    /**
     * This tests tries to use the same SAML token twice
     * The SP should detect that the token was used before and not allow it's use again.
     *
     * @throws Exception
     */
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Mode(TestMode.LITE)
    @Test
    public void basicSAMLTests_useSAMLTokenAgain_replayAttack() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE_AGAIN, null);

        if ((!cttools.isIDPADFS(updatedTestSettings.getIdpRoot())) && (flowType.contains(SAMLConstants.IDP_INITIATED) || flowType.contains(SAMLConstants.UNSOLICITED_SP_INITIATED))) {
            expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE_AGAIN, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive a failure due to a replay attack.", SAMLMessageConstants.CWWKS5082E_ASSERTION_ALREADY_PROCESSED);
        } else {
            expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE_AGAIN, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive a failure due to a replay attack.", SAMLMessageConstants.CWWKS5029E_RELAY_STATE_NOT_RECOGNIZED);
        }

        genericSAML(_testName, webClient, updatedTestSettings, reuseSAMLToken, expectations);

    }

    /**
     * This tests keeps a copy of the original SAML Token. It then alters a copy
     * of the returned SAML token (making it invalid).
     * We pass this token along to ACS which fails because of the content.
     * We then try to use the original token - that should work successfully.
     *
     * @throws Exception
     */
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Mode(TestMode.LITE)
    @Test
    public void basicSAMLTests_badSAMLToken_thenGoodSAMLToken_usuallyNoReplayAttack() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        SAMLTestSettings updatedTestSettings2 = updatedTestSettings.copyTestSettings();
        updatedTestSettings.setRemoveTagInResponse("ds:Signature"); // the whole ds:Signature element

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive message that the SAML Token did not validate.", SAMLMessageConstants.CWWKS5048E_ERROR_VERIFYING_SIGNATURE);

        List<validationData> expectations2;
        if ((flowType.contains(SAMLConstants.IDP_INITIATED) || flowType.contains(SAMLConstants.UNSOLICITED_SP_INITIATED)) && !cttools.isIDPADFS(updatedTestSettings.getIdpRoot())) {
            expectations2 = helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings);
        } else {
            expectations2 = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
            expectations2 = helpers.addMessageExpectation(testSAMLServer, expectations2, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE_AGAIN, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive a failure due to a replay attack.", SAMLMessageConstants.CWWKS5029E_RELAY_STATE_NOT_RECOGNIZED);
        }

        printTestTrace("basicSAMLTests_badSAMLToken_thenGoodSAMLToken_usuallyNoReplayAttack", "Before getting token");

        // get the saml token
        Object origPage = genericSAML(_testName, webClient, updatedTestSettings, justGetSAMLToken, helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings));
        Object thePage = origPage;
        // update a copy of the SAML Token - removing the signature - expect failures
        printTestTrace("basicSAMLTests_badSAMLToken_thenGoodSAMLToken_usuallyNoReplayAttack", "Before using bad response");
        thePage = helpers.invokeACSWithSAMLResponse(_testName, webClient, thePage, updatedTestSettings, expectations);
        // use the original SAML Token - all should work fine
        printTestTrace("basicSAMLTests_badSAMLToken_thenGoodSAMLToken_usuallyNoReplayAttack", "Before using original response again");
        thePage = helpers.invokeACSWithSAMLResponse(_testName, webClient, origPage, updatedTestSettings2, expectations2);

    }

    @Test
    public void basicSAMLTests_mangleSAMLToken_userNameInAssertion_notSigned() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1NotSigned", true);
        // upper case the first character of the username
        String updatedUserName = Character.toUpperCase(updatedTestSettings.getIdpUserName().charAt(0)) + updatedTestSettings.getIdpUserName().substring(1);
        updatedTestSettings.setSamlTokenReplaceVars(updatedTestSettings.getIdpUserName(), updatedUserName, SAMLConstants.LOCATION_ALL);

        List<validationData> expectations = helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, expectations);

    }

    @ExpectedFFDC(value = { "org.opensaml.ws.security.SecurityPolicyException", "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void basicSAMLTests_mangleSAMLToken_userNameInAssertion_signed() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        // upper case the first character of the username
        String updatedUserName = Character.toUpperCase(updatedTestSettings.getIdpUserName().charAt(0)) + updatedTestSettings.getIdpUserName().substring(1);
        updatedTestSettings.setSamlTokenReplaceVars(updatedTestSettings.getIdpUserName(), updatedUserName, SAMLConstants.LOCATION_ALL);

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive exception that the SAML Token did not validate.", SAMLMessageConstants.CWWKS5049E_SIGNATURE_NOT_TRUSTED_OR_VALID);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, expectations);

    }

    @ExpectedFFDC(value = { "org.opensaml.ws.message.decoder.MessageDecodingException" })
    @Test
    public void basicSAMLTests_mangleSAMLToken_badXMLFormatInResponse() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setSamlTokenReplaceVars("saml2p:Response", "saml2b:Response", SAMLConstants.LOCATION_ALL);

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive exception that the SAML Token did not validate.", null, "XML Parsing Error");
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive exception that the SAML Token did not validate.", SAMLMessageConstants.CWWKS5018E_SAML_RESPONSE_CANNOT_BE_DECODED);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, expectations);

    }

    @ExpectedFFDC(value = { "org.opensaml.ws.message.decoder.MessageDecodingException" })
    @Test
    public void basicSAMLTests_mangleSAMLToken_sendGarbage() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setSamlTokenReplaceVars("*", "Just send a string of garbage", SAMLConstants.LOCATION_ALL);

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive exception that the SAML Token did not validate.", null, "XML Parsing Error");
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive exception that the SAML Token did not validate.", SAMLMessageConstants.CWWKS5018E_SAML_RESPONSE_CANNOT_BE_DECODED);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, expectations);

    }

    /**
     * This test replaces the SAML token in the response with a token that is missing the signature.
     * The SP server config expects the response to be signed.
     *
     * @throws Exception
     */
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void basicSAMLTests_mangleSAMLToken_removeSignature_serverRequiresSign() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        updatedTestSettings.setRemoveTagInResponse("ds:Signature"); // the whole ds:Signature element
        // make sure that we get the correct error msgs - to the requesting client as well as in the server logs
        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive message that the SAML Token did not validate.", SAMLMessageConstants.CWWKS5048E_ERROR_VERIFYING_SIGNATURE);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, expectations);

    }

    /**
     * This test gets a token that is NOT signed
     * The SP server config does not expect the response to be signed.
     * The Token should be treated as valid
     *
     * @throws Exception
     */
    @Test
    public void basicSAMLTests_notSignedToken_serverDoesntRequireSign() throws Exception {

        //        testSAMLServer.reconfigServer(buildSPServerName("server_1_notSigned.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1NotSigned", true);
        //        updatedTestSettings.setRemoveTagInResponse("ds:Signature"); // the whole ds:Signature element

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings));

    }

    /**
     * This test replaces the SAML token in the response with a token that is missing the nameId which we should not allow.
     * The test also uses a response that is not signed (this allows us to mangle the response without that
     * being the thing that the validation fails for.
     * The SP server config does not expect the response to be signed.
     *
     * @throws Exception
     */
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void basicSAMLTests_mangleSAMLToken_missingNameId() throws Exception {

        //        testSAMLServer.reconfigServer(buildSPServerName("server_1_notSigned.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1NotSigned", true);

        // remove the signature and nameid
        updatedTestSettings.setRemoveTagInResponse("saml2:NameID");

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive exception that the SAML Token did not validate.", SAMLMessageConstants.CWWKS5068E_MISSING_ATTRIBUTE);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, expectations);

    }

    /**
     *
     * The test verifies that we can invoke multiple apps without clearing cookies
     */

    @Test
    public void basicSAMLTests_invoke_ACS_withoutClearingCookies() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlowKeepingCookies, helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings));

    }

    /**
     *
     * The test verifies the correct behavior with an unprotected app
     *
     */
    @Test
    public void basicSAMLTests_unProtectedApp_then_protectedApp_SPCookie() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setSpDefaultApp(updatedTestSettings.getSpDefaultApp().replace("snoop", "NotProtectedSnoop"));
        updatedTestSettings.setSpTargetApp(updatedTestSettings.getSpDefaultApp().replace("snoop", "NotProtectedSnoop"));

        String[] theFlow;
        List<validationData> expectations = null;
        if (flowType.contains(SAMLConstants.IDP_INITIATED)) {
            theFlow = standardFlowExtendedKeepingCookies;
            expectations = helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings);
            expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not recieve Private Credential: Saml20Token in Alternate app output.", null, "Private Credential: Saml20Token");
            expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.COOKIES, SAMLConstants.STRING_CONTAINS, "Conversation did NOT have an SP Cookie.", SAMLConstants.SP_COOKIE_PREFIX, null);
        } else {
            // for this flow, make sure that we land on the app and that we do NOT have any SP cookies
            List<validationData> expectationsSolSP = vData.addSuccessStatusCodes();
            expectationsSolSP = vData.addExpectation(expectationsSolSP, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not get to the Snoop Servlet", null, SAMLConstants.APP1_TITLE);
            expectationsSolSP = vData.addExpectation(expectationsSolSP, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.COOKIES, SAMLConstants.STRING_DOES_NOT_CONTAIN, "Conversation had an SP Cookie.", SAMLConstants.SP_COOKIE_PREFIX, null);
            expectationsSolSP = vData.addExpectation(expectationsSolSP, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_DOES_NOT_CONTAIN, "Output contained the issuer and should not have", null, "SAMLIssuerName:" + testSettings.getIdpIssuer());

            // call the unprotected app and make sure we get to it - make sure there are no SP cookies afterwards
            // Then, we'll invoke the protected app in the same conversation and make sure that we
            // have to go through a normal Solicited SP flow.
            genericSAML(_testName, webClient, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_ONLY_SP, expectationsSolSP);
            // change the app that acs will invoke back to the protected snoop
            updatedTestSettings.setSpDefaultApp(updatedTestSettings.getSpDefaultApp().replace("NotProtectedSnoop", "snoop"));
            updatedTestSettings.setSpTargetApp(updatedTestSettings.getSpDefaultApp().replace("NotProtectedSnoop", "snoop"));

            // we've handled the unprotected app just fine, now with the same conversation, invoke the protected snoop - we can
            // do this by invoking the normal solicited or unsolicited flow as they start by invoking "snoop"
            // Since there are no cookies in the conversation, we'll get prompted to login
            theFlow = standardFlowExtendedKeepingCookies;
            expectations = helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings);

        }
        genericSAML(_testName, webClient, updatedTestSettings, theFlow, expectations);

    }

    /**
     *
     * The test verifies the correct behavior with an unprotected app
     *
     */
    @Test
    public void basicSAMLTests_protectedApp_then_unProtectedApp_SPCookie() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setSpAlternateApp(updatedTestSettings.getSpDefaultApp().replace("snoop", "NotProtectedSimpleServlet"));

        String[] theFlow;
        List<validationData> expectations = null;

        theFlow = standardFlowExtendedKeepingCookies;
        expectations = helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings);
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not recieve Private Credential: Saml20Token in Alternate app output.", null, "Private Credential: Saml20Token");
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.COOKIES, SAMLConstants.STRING_CONTAINS, "Conversation did NOT have an SP Cookie.", SAMLConstants.SP_COOKIE_PREFIX, null);
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Output did NOT contain the issuer and should have", null, "SAMLIssuerName:" + testSettings.getIdpIssuer());

        genericSAML(_testName, webClient, updatedTestSettings, theFlow, expectations);

    }

    /**
     *
     * The test verifies the correct behavior with an unprotected app
     *
     */
    @Test
    public void basicSAMLTests_unProtectedApp_then_protectedApp_LTPAToken() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_1_ltpaToken.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setAccessTokenType(SAMLConstants.LTPA_ACCESS_TOKEN_TYPE);
        updatedTestSettings.setSpDefaultApp(updatedTestSettings.getSpDefaultApp().replace("snoop", "NotProtectedSnoop"));
        updatedTestSettings.setSpTargetApp(updatedTestSettings.getSpDefaultApp().replace("snoop", "NotProtectedSnoop"));

        String[] theFlow;
        List<validationData> expectations = null;
        if (flowType.contains(SAMLConstants.IDP_INITIATED)) {
            theFlow = standardFlowExtendedKeepingCookies;
            expectations = helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings);
            expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not recieve Private Credential: Saml20Token in Alternate app output.", null, "Private Credential: Saml20Token");
            expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.COOKIES, SAMLConstants.STRING_CONTAINS, "Conversation did NOT have an LTPA Token.", SAMLConstants.LTPA_TOKEN_NAME, null);
        } else {
            // for this flow, make sure that we land on the app and that we do NOT have any SP cookies
            List<validationData> expectationsSolSP = vData.addSuccessStatusCodes();
            expectationsSolSP = vData.addExpectation(expectationsSolSP, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not get to the Snoop Servlet", null, SAMLConstants.APP1_TITLE);
            expectationsSolSP = vData.addExpectation(expectationsSolSP, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.COOKIES, SAMLConstants.STRING_DOES_NOT_CONTAIN, "Conversation had an LTPA Token.", SAMLConstants.LTPA_TOKEN_NAME, null);
            expectationsSolSP = vData.addExpectation(expectationsSolSP, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_DOES_NOT_CONTAIN, "Output contained the issuer and should not have", null, "SAMLIssuerName:" + testSettings.getIdpIssuer());

            // call the unprotected app and make sure we get to it - make sure there are no SP cookies afterwards
            // Then, we'll invoke the protected app in the same conversation and make sure that we
            // have to go through a normal Solicited SP flow.
            genericSAML(_testName, webClient, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_ONLY_SP, expectationsSolSP);
            // change the app that acs will invoke back to the protected snoop
            updatedTestSettings.setSpDefaultApp(updatedTestSettings.getSpDefaultApp().replace("NotProtectedSnoop", "snoop"));
            updatedTestSettings.setSpTargetApp(updatedTestSettings.getSpDefaultApp().replace("NotProtectedSnoop", "snoop"));

            // we've handled the unprotected app just fine, now with the same conversation, invoke the protected snoop - we can
            // do this by invoking the normal solicited or unsolicited flow as they start by invoking "snoop"
            // Since there are no cookies in the conversation, we'll get prompted to login
            theFlow = standardFlowExtendedKeepingCookies;
            expectations = helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings);

        }
        genericSAML(_testName, webClient, updatedTestSettings, theFlow, expectations);

    }

    /**
     *
     * The test verifies the correct behavior with an unprotected app
     *
     */
    @Test
    public void basicSAMLTests_protectedApp_then_unProtectedApp_LTPAToken() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_1_ltpaToken.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setAccessTokenType(SAMLConstants.LTPA_ACCESS_TOKEN_TYPE);
        updatedTestSettings.setSpAlternateApp(updatedTestSettings.getSpDefaultApp().replace("snoop", "NotProtectedSimpleServlet"));

        String[] theFlow;
        List<validationData> expectations = null;

        theFlow = standardFlowExtendedKeepingCookies;
        expectations = helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings);
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not recieve Private Credential: Saml20Token in Alternate app output.", null, "Private Credential: Saml20Token");
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.COOKIES, SAMLConstants.STRING_CONTAINS, "Conversation did NOT have an LTPA Token.", SAMLConstants.LTPA_TOKEN_NAME, null);
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Output did NOT contain the issuer and should have", null, "SAMLIssuerName:" + testSettings.getIdpIssuer());

        genericSAML(_testName, webClient, updatedTestSettings, theFlow, expectations);

    }

    @Mode(TestMode.LITE)
    // all flows get the SamlException, only the IDP and Unsolicited flows get SecurityPolicyException
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @AllowedFFDC(value = { "org.opensaml.ws.security.SecurityPolicyException" })
    @Test
    public void basicSAMLTests_badLTPAToken_missingIDPSSODescriptor() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_1_ltpaToken_missingIDPSSODescriptor.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setAccessTokenType(SAMLConstants.LTPA_ALTERED_ACCESS_TOKEN_TYPE);

        List<validationData> expectations;

        // failure occurs at different steps of the process
        String[] theFlow;
        Log.info(thisClass, _testName, "flow type is : " + flowType);
        if (flowType.matches(SAMLConstants.SOLICITED_SP_INITIATED)) {
            theFlow = SAMLConstants.SOLICITED_SP_INITIATED_FLOW_ONLY_SP;
            expectations = msgUtils.addForbiddenExpectation(SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, null);
            expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive exception that the SAML Token did not validate.", SAMLMessageConstants.CWWKS5023E_NO_IDPSSODESCRIPTOR);
        } else {
            theFlow = standardFlow;
            expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
            expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive exception that the SAML Token did not validate.", SAMLMessageConstants.CWWKS5049E_SIGNATURE_NOT_TRUSTED_OR_VALID);
            //            expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive exception that the SAML Token did not validate.", SAMLMessageConstants.CWWKS5045E_INVALID_ISSUER);
        }

        genericSAML(_testName, webClient, updatedTestSettings, theFlow, expectations);

    }

    /**
     * Test makes sure that we can handle a chained cert used by the IDP. This test uses IDP Metadata
     *
     * @throws Exception
     */
    @Test
    public void basicSAMLTests_chainedCert_withMetaData() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_chainedCert.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp50", true);
        updatedTestSettings.setSpecificIDPChallenge(6);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlowAltAppAgain, helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings));

    }

    /**
     * Test makes sure that we can handle a chained cert used by the IDP. This test has NO IDP Metadata
     * The leaf cert ONLY is in the key and trust store of the SP
     *
     * @throws Exception
     */
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void basicSAMLTests_chainedCert_noMetaData_leafInKeyStore() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_chainedCert_useLeafInKeyStore.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp50", true);
        updatedTestSettings.setSpecificIDPChallenge(6);

        List<validationData> expectations;

        // failure occurs at different steps of the process
        String[] theFlow;
        Log.info(thisClass, _testName, "flow type is : " + flowType);
        theFlow = standardFlowAltAppAgain;
        expectations = helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings);

        genericSAML(_testName, webClient, updatedTestSettings, theFlow, expectations);

    }

    /**
     * Test makes sure that we can handle a chained cert used by the IDP. This test has NO IDP Metadata
     * The intermediate cert only is in the key and trust store of the SP
     *
     * @throws Exception
     */
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Mode(TestMode.LITE)
    @Test
    public void basicSAMLTests_chainedCert_noMetaData_intermediateInKeyStore() throws Exception {
        if (System.getProperty("os.name").contains("Mac")) {
            Log.info(thisClass, _testName, "Aborting the test due to issues with the IBM JDK on Mac. See Defect 249821.");
            // Defect 249821: IBM JDKs on Mac have trouble with this test because some underlying open source code is making a call
            // to CertPathBuilder.getInstance("PKIX") instead of using the CertPathBuilder.getInstance(String, String) or
            // CertPathBuilder.getInstance(String, Provider) method to specify a particular provider.
            return;
        }

        testSAMLServer.reconfigServer(buildSPServerName("server_chainedCert_useIntermediateInKeyStore.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp50", true);
        updatedTestSettings.setSpecificIDPChallenge(6);

        List<validationData> expectations;

        // failure occurs at different steps of the process
        String[] theFlow;
        Log.info(thisClass, _testName, "flow type is : " + flowType);
        theFlow = standardFlowAltAppAgain;
        expectations = helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings);

        genericSAML(_testName, webClient, updatedTestSettings, theFlow, expectations);

    }

    /**
     * Test makes sure that we can handle a chained cert used by the IDP. This test has NO IDP Metadata
     * The root cert only is in the key and trust store of the SP
     *
     * @throws Exception
     */
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    // re-enable when 195531 is fixed
    @Test
    public void basicSAMLTests_chainedCert_noMetaData_rootInKeyStore() throws Exception {
        if (System.getProperty("os.name").contains("Mac")) {
            Log.info(thisClass, _testName, "Aborting the test due to issues with the IBM JDK on Mac. See Defect 249821.");
            // Defect 249821: IBM JDKs on Mac have trouble with this test because some underlying open source code is making a call
            // to CertPathBuilder.getInstance("PKIX") instead of using the CertPathBuilder.getInstance(String, String) or
            // CertPathBuilder.getInstance(String, Provider) method to specify a particular provider.
            return;
        }

        testSAMLServer.reconfigServer(buildSPServerName("server_chainedCert_useRootInKeyStore.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp50", true);
        updatedTestSettings.setSpecificIDPChallenge(6);

        List<validationData> expectations;

        // failure occurs at different steps of the process
        String[] theFlow;
        Log.info(thisClass, _testName, "flow type is : " + flowType);
        theFlow = standardFlowAltAppAgain;
        expectations = helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings);

        genericSAML(_testName, webClient, updatedTestSettings, theFlow, expectations);

    }

    @Mode(TestMode.LITE)
    @Test
    public void basicSAMLTests_invokeACS_withNoToken_usingGet() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        List<validationData> expectations = vData.addSuccessStatusCodes(null, SAMLConstants.GENERIC_INVOKE_PAGE);
        expectations = vData.addResponseStatusExpectation(expectations, SAMLConstants.GENERIC_INVOKE_PAGE, SAMLConstants.INTERNAL_SERVER_ERROR_STATUS);
        expectations = vData.addExpectation(expectations, SAMLConstants.GENERIC_INVOKE_PAGE, SAMLConstants.RESPONSE_MESSAGE, SAMLConstants.STRING_CONTAINS, "GET is NOT supported, request should have returned a status code of 500", null, SAMLConstants.INTERNAL_SERVER_ERROR_MSG);

        String acsUrl = updatedTestSettings.getSpConsumer() + "/acs";
        helpers.genericInvokePage(_testName, webClient, acsUrl, HttpMethod.GET, updatedTestSettings, expectations);

    }

    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Mode(TestMode.LITE)
    @Test
    public void basicSAMLTests_invokeACS_withNoToken_usingPost() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.GENERIC_INVOKE_PAGE, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.GENERIC_INVOKE_PAGE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive exception that the SAML Token did not validate.", SAMLMessageConstants.CWWKS5041E_RELAY_STATE_PARAM_MISSING);
        String acsUrl = updatedTestSettings.getSpConsumer() + "/acs";
        helpers.genericInvokePage(_testName, webClient, acsUrl, HttpMethod.POST, updatedTestSettings, expectations);

    }

    private List<String> getServerReconfigMessages() {
        List<String> extraMsgs = new ArrayList<String>();
        extraMsgs.add(MSG_CWWKO0219I_SSL_PORT_READY);
        return extraMsgs;
    }

}
