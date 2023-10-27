/*******************************************************************************
 * Copyright (c) 2014, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso.fat.config;

import java.util.List;

import org.junit.Test;

import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServerWrapper;

@LibertyServerWrapper
@Mode(TestMode.FULL)
public class SAMLSSLConfigTests extends SAMLConfigCommonTests {

    /********************************************************
     * Tests
     ************************************************************/

    /*************************************************
     * wantAssertionsSigned
     *************************************************/

    /**
     * Config attribute: wantAssertionsSigned This test makes sure that SAML
     * requests are handled properly with the wantAssertionsSigned attribute set
     * to false. The majority (or maybe all other) SAML tests will have this set
     * to true, or not set (which will default to true). So, we'll skip
     * specifically testing that setting here! We'll test with IDP and Solicited
     * SP initiated calls and we'll make requests that in one case return a
     * token with a cert and one without
     */
    // @Mode(TestMode.LITE)
    @Test
    public void test_config_wantAssertionsSigned_false_noIDPSig() throws Exception {

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        SAMLTestSettings updatedTestSettings2 = updatedTestSettings.copyTestSettings();
        updatedTestSettings.setRemoveTagInResponse("ds:Signature");

        testSAMLServer.reconfigServer("server_wantAssertionsSigned_false.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        IDP_initiated_SAML(_testName, updatedTestSettings2, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings2));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings2, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings2));

    }

    /**
     * Config attribute: wantAssertionsSigned This test makes sure that SAML
     * requests are handled properly with the wantAssertionsSigned attribute set
     * to false. This test has the SP configured with a stronger alg than the
     * IDP partner uses - with wantAssertionsSigned set to false, this algorithm
     * mismatch should still be detected ... The majority (or maybe all other)
     * SAML tests will have this set to true, or not set (which will default to
     * true). Even though we don't care if the assertion is signed, if it is
     * signed, it needs to be at least as strong as what the config specifies
     */
    // @Mode(TestMode.LITE)
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException","org.opensaml.messaging.handler.MessageHandlerException" })
    //@Test
    public void test_config_wantAssertionsSigned_false_withWeakerIDPSig() throws Exception {

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        testSAMLServer.reconfigServer("server_wantAssertionsSigned_false_weakerIDPAlg.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        // add standard "Forbidden response checks"
        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Signature could not be verified in SAML Response", null, SAMLMessageConstants.CWWKS5049E_SIGNATURE_NOT_TRUSTED_OR_VALID);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, expectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, expectations);

    }

    /**
     * Config attribute: wantAssertionsSigned This test makes sure that SAML
     * requests are handled properly with the wantAssertionsSigned attribute set
     * to false. This test has the SP configured with a stronger alg than the
     * IDP partner uses - with wantAssertionsSigned set to false, this algorithm
     * mismatch should still be detected ... The majority (or maybe all other)
     * SAML tests will have this set to true, or not set (which will default to
     * true). Even though we don't care if the assertion is signed, if it is
     * signed, we must be able to validate the cert
     */
    // @Mode(TestMode.LITE)
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.messaging.handler.MessageHandlerException" })
    @Test
    public void test_config_wantAssertionsSigned_false_certMissingFromIDPMetaData() throws Exception {

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        testSAMLServer.reconfigServer("server_wantAssertionsSigned_false_certMissingFromIDPMetaData.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        // add standard "Forbidden response checks"
        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Signature could not be verified in SAML Response", SAMLMessageConstants.CWWKS5049E_SIGNATURE_NOT_TRUSTED_OR_VALID);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, expectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, expectations);

    }

    /*************************************************
     * signatureMethodAlgorithm
     *************************************************/

    //	// signatureMethodAlgorithm set to none is to be removed - disable tests
    //	// (dont' delete for now just in case they don't delete that value
    //	/**
    //	 * Config attribute: signatureMethodAlgorithm This test specifies "none" for
    //	 * signatureMethodAlgorithm. The IDP server signs with SHA1 or SHA256
    //	 * depending on the partner used in the test. The SP should accept/verify
    //	 * the signer even though "none" is specified. If no signature is returned,
    //	 * the request should fail as we ask for it to signed (no caring how), but
    //	 * it is NOT Set wantAssertionsSigned to true - make sure that signature
    //	 * checks behave correctly even for the case where we receive none back the
    //	 * none in the signatureMethodAlgorithm should override the
    //	 * wantAssertionsSigned
    //	 */
    //	@Mode(TestMode.LITE)
    //	@Test
    //	public void test_config_signatureMethodAlgorithm_none_wantAssertionsSigned_true() throws Exception {
    //
    //		// configure that uses "none" for the sig alg
    //		testSAMLServer.reconfigServer("server_signatureMethodAlgorithm_none_wantAssertionsSigned_true.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);
    //
    //		msgUtils.printMarker(_testName, "Response Not Signed");
    //		// // test with various alg being returned from the IDP (our IDP can't
    //		// support no signature, so in that case, we'll remove it from the
    //		// response, ...)
    //		// // modify test settings to tell test code to remove the cert from the
    //		// saml token
    //		// // - make it look like the IDP returned an unsigned response
    //		// SAMLTestSettings updatedTestSettings =
    //		// testSettings.copyTestSettings();
    //		// updatedTestSettings.updatePartnerInSettings("sp1", true) ;
    //		// updatedTestSettings.setRemoveTagInResponse("ds:Signature");
    //		//
    //		// // response with no cert
    //		// IDP_initiated_SAML(_testName, updatedTestSettings,
    //		// SAMLConstants.IDP_INITIATED_FLOW,
    //		// helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
    //		// solicited_SP_initiated_SAML(_testName, updatedTestSettings,
    //		// SAMLConstants.SOLICITED_SP_INITIATED_FLOW,
    //		// helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));
    //
    //		// start with an sp that uses sha1
    //		msgUtils.printMarker(_testName, "Response Signed with SHA1");
    //		SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
    //		// updatedTestSettings = testSettings.copyTestSettings();
    //		updatedTestSettings.updatePartnerInSettings("sp1", true);
    //
    //		// invoke partner that uses SHA1
    //		IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
    //		solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));
    //
    //		// modify test settings to use provider2 - this is sha256
    //		msgUtils.printMarker(_testName, "Response Signed with SHA256");
    //		updatedTestSettings = testSettings.copyTestSettings();
    //		updatedTestSettings.updatePartnerInSettings("sp2", true);
    //
    //		// invoke partner that uses SHA256
    //		IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
    //		solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));
    //
    //	}
    //
    //	/**
    //	 * Config attribute: signatureMethodAlgorithm This test specifies "none" for
    //	 * signatureMethodAlgorithm. The IDP server signs with SHA1 or SHA256
    //	 * depending on the partner used in the test. The SP should accept/verify
    //	 * the signer even though "none" is specified. If no signature is returned,
    //	 * the request should fail as we ask for it to signed (no caring how), but
    //	 * it is NOT Set wantAssertionsSigned to false - make sure that everything
    //	 * behaves correctly
    //	 */
    //	@Mode(TestMode.LITE)
    //	@Test
    //	public void test_config_signatureMethodAlgorithm_none_wantAssertionsSigned_false() throws Exception {
    //
    //		// configure that uses "none" for the sig alg
    //		testSAMLServer.reconfigServer("server_signatureMethodAlgorithm_none_wantAssertionsSigned_false.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);
    //
    //		msgUtils.printMarker(_testName, "Response Not Signed");
    //		// test with various alg being returned from the IDP (our IDP can't
    //		// support no signature, so in that case, we'll remove it from the
    //		// response, ...)
    //		// modify test settings to tell test code to remove the cert from the
    //		// saml token
    //		// - make it look like the IDP returned an unsigned response
    //		SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
    //		updatedTestSettings.updatePartnerInSettings("sp1", true);
    //		updatedTestSettings.setRemoveTagInResponse("ds:Signature");
    //
    //		// response with no cert
    //		IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
    //		solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));
    //
    //		// start with an sp that uses sha1
    //		msgUtils.printMarker(_testName, "Response Signed with SHA1");
    //		updatedTestSettings = testSettings.copyTestSettings();
    //		updatedTestSettings.updatePartnerInSettings("sp1", true);
    //
    //		// invoke partner that uses SHA1
    //		IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
    //		solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));
    //
    //		// modify test settings to use provider2 - this is sha256
    //		msgUtils.printMarker(_testName, "Response Signed with SHA256");
    //		updatedTestSettings = testSettings.copyTestSettings();
    //		updatedTestSettings.updatePartnerInSettings("sp2", true);
    //
    //		// invoke partner that uses SHA256
    //		IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
    //		solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));
    //
    //	}

    /**
     * Config attribute: signatureMethodAlgorithm This test specifies "SHA1" for
     * signatureMethodAlgorithm. The IDP server signs with SHA1 or SHA256
     * depending on the partner used in the test. The SP should accept/verify
     * the signer even though "SHA1" is specified.
     */
    //@Mode(TestMode.LITE)
    //@ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    //@Test - we changed the config to work with fips140-3. updated SHA1 to SHA256 in the configs and I need to differentiate fips run from non-fips
    //TODO : enable this if needed
    public void test_config_signatureMethodAlgorithm_SHA1() throws Exception {

        testSAMLServer.reconfigServer("server_signatureMethodAlgorithm_SHA1.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        // test with various alg being returned from the IDP (our IDP can't
        // support no signature, so in that case, we'll remove it from the
        // response, ...)
        // modify test settings to tell test code to remove the cert from the
        // saml token
        msgUtils.printMarker(_testName, "Response Not Signed");
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setRemoveTagInResponse("ds:Signature");

        // add standard "Forbidden response checks"
        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Signature could not be verified in SAML Response", SAMLMessageConstants.CWWKS5048E_ERROR_VERIFYING_SIGNATURE);

        // force response with no cert
        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, expectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, expectations);

        // modify test settings to use provider1 - this is sha1
        msgUtils.printMarker(_testName, "Response with SHA1");
        updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        // invoke partner that uses SHA1
        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));

        // modify test settings to use provider2 - this is sha256
        msgUtils.printMarker(_testName, "Response with SHA256");
        updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp2", true);

        // invoke partner that uses SHA256
        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));

    }

    /**
     * Config attribute: signatureMethodAlgorithm This test specifies "SHA256"
     * for signatureMethodAlgorithm. The IDP server signs with SHA1 or SHA256
     * depending on the partner used in the test. The SP should accept/verify
     * the signer when the IDP uses SHA256, but should fail if SHA1 is used.
     */
    @Mode(TestMode.LITE)
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException","org.opensaml.messaging.handler.MessageHandlerException" })
    @Test
    public void test_config_signatureMethodAlgorithm_SHA256() throws Exception {

        testSAMLServer.reconfigServer("server_signatureMethodAlgorithm_SHA256.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        // No signature will expect the same failure
        // add standard "Forbidden response checks"
        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Signature could not be verified in SAML Response", SAMLMessageConstants.CWWKS5048E_ERROR_VERIFYING_SIGNATURE);

        msgUtils.printMarker(_testName, "Response Not Signed");
        // test with various alg being returned from the IDP (our IDP can't
        // support no signature, so in that case, we'll remove it from the
        // response, ...)
        // modify test settings to tell test code to remove the cert from the
        // saml token
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setRemoveTagInResponse("ds:Signature");

        // force response with no cert
        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, expectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, expectations);

		/* fips140-3 disables sha1 signature algorithm
		 * // modify test settings to use provider1 - this is sha1
		 * msgUtils.printMarker(_testName, "Response with SHA1"); updatedTestSettings =
		 * testSettings.copyTestSettings();
		 * updatedTestSettings.updatePartnerInSettings("sp1", true);
		 * 
		 * // add standard "Forbidden response checks" List<validationData>
		 * expectations2 =
		 * msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE,
		 * null); expectations2 = helpers.addMessageExpectation(testSAMLServer,
		 * expectations2, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE,
		 * SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS,
		 * "Signature could not be verified in SAML Response",
		 * SAMLMessageConstants.CWWKS5049E_SIGNATURE_NOT_TRUSTED_OR_VALID);
		 * 
		 * IDP_initiated_SAML(_testName, updatedTestSettings,
		 * SAMLConstants.IDP_INITIATED_FLOW, expectations2);
		 * solicited_SP_initiated_SAML(_testName, updatedTestSettings,
		 * SAMLConstants.SOLICITED_SP_INITIATED_FLOW, expectations2);
		 */

        // modify test settings to use provider2 - this is sha256
        msgUtils.printMarker(_testName, "Response with SHA256");
        updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp2", true);

        // invoke partner that uses SHA256
        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));

    }

    /**
     * Config attribute: signatureMethodAlgorithm This test specifies "SHA256"
     * for signatureMethodAlgorithm. The SP also specifies a filter that won't
     * match the app that won't match the filter. The IDP server signs with
     * SHA1. IDP initiated - will fail because the flow specifies which local sp
     * to use, so, it'll fail the algorythm check SP initiated - will proceed to
     * use form login because the filter check won't match the app being used
     * (note that this server config also specifies a funky filter for the
     * default sp config (it is normally equiv to a *))
     */
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.messaging.handler.MessageHandlerException" })
    @Mode(TestMode.LITE)
    @Test
    public void test_config_signatureMethodAlgorithm_weakAlgIgnored_useFormLogin() throws Exception {

        testSAMLServer.reconfigServer("server_signatureMethodAlgorithm_SHA256_badFilter.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        // test with various alg being returned from the IDP (our IDP can't
        // support no signature, ...)
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        // for IDP initiated, the filter won't matter - we've already specified
        // sp1 (which has sha256 defined) - so the algo will be too weak
        //List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        //expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Signature could not be verified in SAML Response", SAMLMessageConstants.CWWKS5049E_SIGNATURE_NOT_TRUSTED_OR_VALID);

        // invoke partner that uses SHA1 - should get SAML Token, but we should
        // skip validation and go
        // directly to form login
        List<validationData> expectations2 = vData.addSuccessStatusCodes();
        expectations2 = vData.addExpectation(expectations2, SAMLConstants.BUILD_POST_IDP_INITIATED_REQUEST, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not land on the IDP form login form.", null, cttools.getLoginTitle(updatedTestSettings.getIdpRoot()));
        expectations2 = vData.addExpectation(expectations2, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML POST response", null, cttools.getResponseTitle(updatedTestSettings.getIdpRoot()));
        expectations2 = vData.addExpectation(expectations2, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML Response", null, SAMLConstants.SAML_RESPONSE);
        expectations2 = vData.addExpectation(expectations2, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not land on the normal form login form.", null, SAMLConstants.STANDARD_LOGIN_HEADER);

        expectations2 = vData.addExpectation(expectations2, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not land on the normal form login form.", null, SAMLConstants.STANDARD_LOGIN_HEADER);

        expectations2 = vData.addExpectation(expectations2, SAMLConstants.PROCESS_FORM_LOGIN, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get output showing that there is NO SAML token - it should not be there", null, SAMLConstants.NO_SAML_TOKEN_FOUND_MSG);

        //IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, expectations); // idp initiated will not fail since sp1 there also is using sha256, commenting out this flow.
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_NO_MATCH_FORMLOGIN, expectations2);
    }

    /*************************************************
     * authnRequestsSigned
     *************************************************/

    /**
     * Config attribute: authnRequestsSigned This test specifies "false" for
     * authnRequestsSigned - The TFIM IDP always expects a signed request IDP
     * initiatied flow - should see a normal flow - no failures (the flag has no
     * real meaning in the idp flow) SP initiated flow - should fail when the
     * first request is made because the request is NOT signed
     */
    // @Mode(TestMode.LITE)
    @Test
    public void test_config_authnRequestsSigned_false() throws Exception {

        // test with
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1ReqSigned", true);

        testSAMLServer.reconfigServer("server_authnRequestsSigned_false.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        // SP initiated flow should fail because the outbound request is NOT
        // signed
        // IDP replies directly to the client - SP has no control over the
        // response content in this case (we're checking status and msgs from
        // TFIM)
        List<validationData> expectations1 = vData.addSuccessStatusCodes(null, SAMLConstants.BUILD_POST_IDP_INITIATED_REQUEST);
        expectations1 = vData.addResponseStatusExpectation(expectations1, SAMLConstants.BUILD_POST_IDP_INITIATED_REQUEST, SAMLConstants.BAD_REQUEST_STATUS);
        expectations1 = vData.addExpectation(expectations1, SAMLConstants.BUILD_POST_IDP_INITIATED_REQUEST, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get a failure because the request was NOT signed.", null, SAMLConstants.SHIBBOLETH_IDP_ERROR);
        expectations1 = vData.addExpectation(expectations1, SAMLConstants.BUILD_POST_IDP_INITIATED_REQUEST, SAMLConstants.RESPONSE_MESSAGE, SAMLConstants.STRING_CONTAINS, "Did not get a failure because the request was NOT signed.", null, SAMLConstants.BAD_REQUEST);
        expectations1 = vData.addExpectation(expectations1, SAMLConstants.BUILD_POST_IDP_INITIATED_REQUEST, SAMLConstants.IDP_PROCESS_LOG, SAMLConstants.STRING_CONTAINS, "Did not get a failure because the request was NOT signed", null, "Inbound AuthnRequest was required to be signed but was not");

        List<validationData> expectations2 = vData.addSuccessStatusCodes(null, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST);
        expectations2 = vData.addResponseStatusExpectation(expectations2, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.BAD_REQUEST_STATUS);
        expectations2 = vData.addExpectation(expectations2, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get a failure because the request was NOT signed.", null, SAMLConstants.SHIBBOLETH_IDP_ERROR);
        expectations2 = vData.addExpectation(expectations2, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.RESPONSE_MESSAGE, SAMLConstants.STRING_CONTAINS, "Did not get a failure because the request was NOT signed.", null, SAMLConstants.BAD_REQUEST);
        expectations2 = vData.addExpectation(expectations2, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.IDP_PROCESS_LOG, SAMLConstants.STRING_CONTAINS, "Did not get a failure because the request was NOT signed", null, "Inbound AuthnRequest was required to be signed but was not");

        // invoke partner that uses SHA256
        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW_ONLY_IDP, expectations1);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_ONLY_SP, expectations2);

    }

    /*
     * authnRequestsSigned = true is covered by 99% of the SAML tests - not
     * testing it specifically here
     */

    /*************************************************
     * keyStoreRef
     *************************************************/

    // TODO keystore testing with encryption in mind...
    /**
     * Config attribute: keyStoreRef This test specifies a keystore ref that
     * does not exist - it also specifies that authnRequestsSigned is true We
     * should see a message logged (by the server) that the keystore was not
     * found. IDP initiated - We should not see any failures on the client - the
     * keystore is not really used SP Initiated - We should fail when we make
     * our out bound request (SP can not build the out bound request)
     */
    @Mode(TestMode.LITE)
    @ExpectedFFDC(value = { "java.security.KeyStoreException", "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void test_config_keyStoreRef_invalid_authnRequestsSigned_true() throws Exception {

        testSAMLServer.reconfigServer("server_keyStoreRef_bad_authnRequestsSigned_true.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        // set expectations for SP flow
        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not get message indicating that the keyStoreRef was bad.", SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR);
        expectations = vData.addExpectation(expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not get message indicating that the keyStoreRef was bad.", null, "aBadKeyStoreRef");

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_ONLY_SP, expectations);

    }

    /**
     * Config attribute: keyStoreRef This test specifies a keystore ref that
     * does not exist - it also specifies that authnRequestsSigned is false We
     * should see a message logged (by the server) that the keystore was not
     * found. IDP initiated - We should not see any failures on the client - the
     * keystore is not really used SP Initiated - We should fail when we make
     * our out bound request (IDP fails because the request is not signed)
     */
    // @Mode(TestMode.LITE)
    @Test
    public void test_config_keyStoreRef_invalid_authnRequestsSigned_false() throws Exception {

        testSAMLServer.reconfigServer("server_keyStoreRef_bad_authnRequestsSigned_false.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1ReqSigned", true);

        // SP initiated flow should fail because the outbound request is NOT
        // signed
        // IDP replies directly to the client - SP has no control over the
        // response content in this case (we're checking status and msgs from
        // shibboleth)
        List<validationData> expectations1 = vData.addSuccessStatusCodes(null, SAMLConstants.BUILD_POST_IDP_INITIATED_REQUEST);
        expectations1 = vData.addResponseStatusExpectation(expectations1, SAMLConstants.BUILD_POST_IDP_INITIATED_REQUEST, SAMLConstants.BAD_REQUEST_STATUS);
        expectations1 = vData.addExpectation(expectations1, SAMLConstants.BUILD_POST_IDP_INITIATED_REQUEST, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get a failure because the request was NOT signed.", null, SAMLConstants.SHIBBOLETH_IDP_ERROR);
        expectations1 = vData.addExpectation(expectations1, SAMLConstants.BUILD_POST_IDP_INITIATED_REQUEST, SAMLConstants.RESPONSE_MESSAGE, SAMLConstants.STRING_CONTAINS, "Did not get a failure because the request was NOT signed.", null, SAMLConstants.BAD_REQUEST);
        expectations1 = vData.addExpectation(expectations1, SAMLConstants.BUILD_POST_IDP_INITIATED_REQUEST, SAMLConstants.IDP_PROCESS_LOG, SAMLConstants.STRING_CONTAINS, "Did not get a failure because the request was NOT signed", null, "Inbound AuthnRequest was required to be signed but was not");

        List<validationData> expectations2 = vData.addSuccessStatusCodes(null, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST);
        expectations2 = vData.addResponseStatusExpectation(expectations2, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.BAD_REQUEST_STATUS);
        expectations2 = vData.addExpectation(expectations2, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get a failure because the request was NOT signed.", null, SAMLConstants.SHIBBOLETH_IDP_ERROR);
        expectations2 = vData.addExpectation(expectations2, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.RESPONSE_MESSAGE, SAMLConstants.STRING_CONTAINS, "Did not get a failure because the request was NOT signed.", null, SAMLConstants.BAD_REQUEST);
        expectations2 = vData.addExpectation(expectations2, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.IDP_PROCESS_LOG, SAMLConstants.STRING_CONTAINS, "Did not get a failure because the request was NOT signed", null, "Inbound AuthnRequest was required to be signed but was not");

        // invoke partner that uses SHA256
        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW_ONLY_IDP, expectations1);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_ONLY_SP, expectations2);

    }

    // other tests ----- no certs in keystore

    /**
     * Config attribute: keyStoreRef This test specifies a keystore file that
     * does exist - it also specifies that authnRequestsSigned is false This
     * KeyStore file has NO keys IDP initiated - We should not see any failures
     * on the client - the keystore is not really used SP Initiated - We should
     * fail when we make our outbound request (we can't sign the outbound
     * reqeust)
     */
    // @Mode(TestMode.LITE)
    @ExpectedFFDC(value = { "java.security.cert.CertificateException", "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void test_config_keyStoreRef_noKeys() throws Exception {

        testSAMLServer.reconfigServer("server_keyStoreRef_nokeys.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        // set expectations for SP flow
        // IDP replies directly to the client - SP has no control over frame
        // content or status code
        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not get message indicating that the keyStoreRef was bad.", SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR);
        expectations = vData.addExpectation(expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not get message indicating that the keyStoreRef was bad.", null, "sslspservercert");

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_ONLY_SP, expectations);

    }

    /*************************************************
     * keyAlias
     *************************************************/

    /**
     * Config attribute: keyAlias This test specifies a valid keyAlias in the
     * keystore which should be found and used IDP Initiated - We should not see
     * any failures on the client - the keystore and cert are not used in this
     * flow SP Initiated - We should not see any failures on the client - the
     * configured alias should be found in the KeyStore and used
     */
    // @Mode(TestMode.LITE)
    @Test
    public void test_config_keyAlias_valid() throws Exception {

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));

    }

    /**
     * Config attribute: keyAlias This test specifies a keyAlias that does not
     * exist in the keystore file This tests specifies authnRequestsSigned=true
     * IDP Initiated - We should not see any failures on the client - the
     * keystore and cert are not used in this flow SP Initiated - We will fail
     * when we try to make the outbound request (we can't sign the outbound
     * reqeust)
     */
    // @Mode(TestMode.LITE)
    @ExpectedFFDC(value = { "java.security.cert.CertificateException", "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void test_config_keyAlias_doesNotExistInKeyStore() throws Exception {

        testSAMLServer.reconfigServer("server_keyAlias_doesNotExistInKeyStore.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        // set expectations for SP flow
        // IDP replies directly to the client - SP has no control over frame
        // content or status code
        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not get message indicating that the keyStoreRef was bad. (CWWKS5007E missing)", SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR);
        expectations = vData.addExpectation(expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not get message indicating that the keyStoreRef was bad. (doesNotExistCert missing)", null, "doesNotExistCert");

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_ONLY_SP, expectations);

    }

    /**
     * Config attribute: keyAlias This test specifies no keyAlias the ONLY
     * keyAlias in the keystore and should be found and used IDP Initiated - We
     * should not see any failures on the client - the keystore and cert are not
     * used in this flow SP Initiated - We should not see any failures on the
     * client - the only alias in the keystore should be found and used
     */
    @Mode(TestMode.LITE)
    @Test
    public void test_config_keyAlias_notSpecifiedOnlyOneInKeyStore() throws Exception {

        testSAMLServer.reconfigServer("server_keyAlias_notSpecifiedOnlyOneInKeyStore.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));

    }

    /**
     * Config attribute: keyAlias This test specifies no keyAlias the default
     * keyAlias samlsp exists in the keystore and should be found and used IDP
     * Initiated - We should not see any failures on the client - the keystore
     * and cert are not used in this flow SP Initiated - We should not see any
     * failures on the client - the default alias should be found and used
     */
    // @Mode(TestMode.LITE)
    @Test
    public void test_config_keyAlias_notSpecifiedSamlSpInKeyStore() throws Exception {

        testSAMLServer.reconfigServer("server_keyAlias_notSpecifiedSamlSpInKeyStore.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));

    }

    /*************************************************
     * keyPassword
     *************************************************/

    // keypassword - not specified - default - specified this way in 99% of the
    // other tests!!!

    // keypassword - same as keystore and incorrect one is specified
    // TODO - do we need tests where the keystore is defined in the server.xml
    // with an incorrect password??? Is that all base function, or will there be
    // any SAML specific function?

    /**
     * Config attribute: keyPassword This test specifies a valid keyPassword for
     * the keyAlias in the keystore and should be used IDP Initiated - We should
     * not see any failures on the client - the keystore and cert are not used
     * in this flow SP Initiated - We should not see any failures on the client
     * - the cert should be found, accessible and used
     */
    // @Mode(TestMode.LITE)
    @Test
    public void test_config_keyPassword_valid() throws Exception {

        testSAMLServer.reconfigServer("server_keyPassword_valid.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));

    }

    /**
     * Config attribute: keyPassword This test specifies an invalid keyPassword
     * for the keyAlias in the keystore and we should not be able to access the
     * cert IDP Initiated - We should not see any failures on the client - the
     * keystore and cert are not used in this flow SP Initiated - We should not
     * see any failures on the client - the cert should be found, but is not
     * accessible - we should get an exception
     */
    // @Mode(TestMode.LITE)
    @ExpectedFFDC(value = { "java.security.KeyStoreException", "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void test_config_keyPassword_invalid() throws Exception {

        testSAMLServer.reconfigServer("server_keyPassword_invalid.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        // set expectations for SP flow
        // IDP replies directly to the client - SP has no control over frame
        // content or status code
        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, null);
        expectations = vData.addExpectation(expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not get message indicating that the key could not be recovered.", null, SAMLMessageConstants.CWPKI0812E_ERROR_GETTING_KEY + ".+sslspservercert.+samlKeyStore.+Cannot recover key.+");
        expectations = vData.addExpectation(expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not get message indicating that the cert could not be accessed.", null, SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR + ".+sslspservercert.+samlKeyStore.+");
        testSAMLServer.addIgnoredServerExceptions(SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR, SAMLMessageConstants.CWPKI0812E_ERROR_GETTING_KEY);
        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_ONLY_SP, expectations);

    }

    // @Mode(TestMode.LITE)
    @Test
    public void test_config_keyPassword_matchesKeyStorePw() throws Exception {

        testSAMLServer.reconfigServer("server_keyPassword_sameAsKeyStore.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        // set expectations for SP flow
        // IDP replies directly to the client - SP has no control over frame
        // content or status code

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));

    }

    /*************************************************
     * trustStoreRef
     *************************************************/

    /**
     * Config attribute: trustStoreRef This test
     */

}
