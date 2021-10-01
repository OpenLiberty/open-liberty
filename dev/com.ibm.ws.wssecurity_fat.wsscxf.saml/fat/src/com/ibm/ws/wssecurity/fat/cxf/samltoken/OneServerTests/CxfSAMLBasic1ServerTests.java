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

package com.ibm.ws.wssecurity.fat.cxf.samltoken.OneServerTests;

import static componenttest.annotation.SkipForRepeat.EE8_FEATURES;
import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;
import static componenttest.annotation.SkipForRepeat.NO_MODIFICATION;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;

import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;
import com.ibm.ws.wssecurity.fat.cxf.samltoken.common.CxfSAMLBasicTests;

import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EE8FeatureReplacementAction;
import componenttest.rules.repeater.EmptyAction;
import componenttest.topology.impl.LibertyServerWrapper;
import componenttest.topology.utils.HttpUtils;

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

@SkipForRepeat({ EE9_FEATURES })
@LibertyServerWrapper
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class CxfSAMLBasic1ServerTests extends CxfSAMLBasicTests {

    private static final Class<?> thisClass = CxfSAMLBasicTests.class;
    protected static String audienceRestrictError = "";
    protected static String wantAssertionsSignedError = "";

    @BeforeClass
    public static void setupBeforeTest() throws Exception {

        flowType = chooseRandomFlow();
        idpSupportedType = SAMLConstants.TFIM_TYPE;

        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");

        // add any additional messages that you want the "start" to wait for
        // we should wait for any providers that this test requires
        List<String> extraMsgs = new ArrayList<String>();
        extraMsgs.add("CWWKT0016I.*samlcxfclient.*");
        extraMsgs.add("CWWKS5000I");
        extraMsgs.add("CWWKS5002I");

        List<String> extraApps = new ArrayList<String>();
        extraApps.add(SAMLConstants.SAML_CXF_CLIENT_APP);

        startSPWithIDPServer("com.ibm.ws.wssecurity_fat.saml", "server_2_in_1.xml", SAMLConstants.SAML_SERVER_TYPE, extraMsgs, extraApps, true);

        servicePort = Integer.toString(testSAMLServer.getServerHttpPort());
        serviceSecurePort = Integer.toString(testSAMLServer.getServerHttpsPort());

        setActionsForFlowType(flowType);
        testSettings.setIdpUserName("user1");
        testSettings.setIdpUserPwd("security");
        testSettings.setSpTargetApp(testSAMLServer.getHttpString() + "/samlcxfclient/CxfSamlSvcClient");
        testSettings.setSamlTokenValidationData(testSettings.getIdpUserName(), testSettings.getSamlTokenValidationData().getIssuer(), testSettings.getSamlTokenValidationData().getInResponseTo(), testSettings.getSamlTokenValidationData().getMessageID(), testSettings.getSamlTokenValidationData().getEncryptionKeyUser(), testSettings.getSamlTokenValidationData().getRecipient(), testSettings.getSamlTokenValidationData().getEncryptAlg());

        testSAMLServer.addIgnoredServerExceptions(SAMLMessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES, SAMLMessageConstants.CWWKF0001E_FEATURE_MISSING, SAMLMessageConstants.CWWKG0101W_CONFIG_NOT_VISIBLE_TO_OTHER_BUNDLES);

    }

    /**
     * TestDescription:
     *
     * In this scenario, the SAML token is updated to make it invalid
     * by shifting the IssueInstant past the current time. We keep the
     * clockSkew in the SAML config set to the default (which will allow
     * the token to be valid to ACS). But, we make the clockskew very small
     * with the WSSecurity Provider config. We sleep to make sure that we
     * really are beyond the life of the SAML
     * We should receive a useful message indicating what went wrong.
     * The server message log should have more detailed information about
     * the failure
     *
     */

    @SkipForRepeat({ EE8_FEATURES })
    @ExpectedFFDC(value = { "org.apache.ws.security.WSSecurityException" }, repeatAction = { EmptyAction.ID })
    @Test
    public void CxfSAMLBasicTests_clockSkew_testEE7Only() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_2_in_1_clockskew.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        String expectedResponse = "Some error message";
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setSleepBeforeTokenUse(40);
        updatedTestSettings.setSamlTokenUpdateTimeVars(SAMLConstants.SAML_ISSUE_INSTANT, SAMLConstants.ADD_TIME, SAMLTestSettings.setTimeArray(0, 0, 5, 0), SAMLConstants.DO_NOT_USE_CURRENT_TIME);
        updatedTestSettings.setRemoveTagInResponse("ds:Signature"); // the whole ds:Signature element

        updatedTestSettings.setCXFSettings(_testName, null, servicePort, serviceSecurePort, "user1", "user1pwd", "SAMLSOAPService2", "SAMLSoapPort2", "", "False", null, null);

        List<validationData> expectations = helpers.setErrorSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_GENERAL_FAILURE_MSG);
        
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did NOT fail because the clocks were out of sync.", SAMLMessageConstants.CWWKW0217E_CLOCK_SKEW_ERROR);
        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, expectations);
    }

    @SkipForRepeat({ NO_MODIFICATION })
    @AllowedFFDC(value = { "java.util.MissingResourceException", "org.apache.wss4j.common.ext.WSSecurityException" }, repeatAction = { EE8FeatureReplacementAction.ID })
    @Test
    public void CxfSAMLBasicTests_clockSkew_testEE8Only() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_2_in_1_clockskew_ee8.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        String expectedResponse = "Some error message";
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setSleepBeforeTokenUse(40);
        updatedTestSettings.setSamlTokenUpdateTimeVars(SAMLConstants.SAML_ISSUE_INSTANT, SAMLConstants.ADD_TIME, SAMLTestSettings.setTimeArray(0, 0, 5, 0), SAMLConstants.DO_NOT_USE_CURRENT_TIME);
        updatedTestSettings.setRemoveTagInResponse("ds:Signature"); // the whole ds:Signature element

        updatedTestSettings.setCXFSettings(_testName, null, servicePort, serviceSecurePort, "user1", "user1pwd", "SAMLSOAPService2", "SAMLSoapPort2", "", "False", null, null);

        String CXF_SAML_TOKEN_GENERAL_FAILURE_MSG = "SAML token security failure"; //@AV999 TODO select the message depending on the runtime
        List<validationData> expectations = helpers.setErrorSAMLCXFExpectations(null, flowType, updatedTestSettings, CXF_SAML_TOKEN_GENERAL_FAILURE_MSG);
        
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did NOT fail because the clocks were out of sync.", SAMLMessageConstants.CWWKW0217E_CLOCK_SKEW_ERROR);
        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, expectations);
    }
    
    /**
     * TestDescription:
     *
     * In this scenario, the SAML token is updated to remove the
     * signature.
     * The SAML config will have wantAssertionsSigned set to false, so ACS
     * won't care that the token does NOT have a signature.
     * We won't add wantAssertionsSigned to the WSSecurity Provider.
     * We should recieve a useful message indicating what went wrong.
     * The server message log should have more detailed information about
     * the failure
     *
     */
    
    @SkipForRepeat({ EE8_FEATURES })
    @ExpectedFFDC(value = { "org.apache.ws.security.WSSecurityException" }, repeatAction = { EmptyAction.ID })
    @Test
    public void CxfSAMLBasicTests_wantAssertionsSignedTrue_missingSignatureEE7Only() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_2_in_1_samlWantAssertionsSigned.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        String expectedResponse = "Some error message";
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setRemoveTagInResponse("ds:Signature"); // the whole ds:Signature element

        updatedTestSettings.setCXFSettings(_testName, null, servicePort, serviceSecurePort, "user1", "user1pwd", "SAMLSOAPService2", "SAMLSoapPort2", "", "False", null, null);

        List<validationData> expectations = helpers.setErrorSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_GENERAL_FAILURE_MSG);
        
        // The server is NOT logging an error message indicating the real cause of the failure - defect 251665 has been opened to add a message
        // for now, we can look for a debug message that is logged in the trace... (btw, the error in the commented out expectation is just a best guess at what message will actually be logged)
        //        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did NOT fail because the assertion did NOT require a signature.", SAMLMessageConstants.CWWKS5048E_ERROR_VERIFYING_SIGNATURE);
        
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_TRACE_LOG, SAMLConstants.STRING_CONTAINS, "Did NOT fail because the assertion did NOT require a signature.", "verifySubjectConfirmationMethod A Bearer Assertion was not signed");

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, expectations);
    }

    @SkipForRepeat({ NO_MODIFICATION })
    @AllowedFFDC(value = { "java.util.MissingResourceException","org.apache.wss4j.common.ext.WSSecurityException" }, repeatAction = { EE8FeatureReplacementAction.ID })
    @Test
    public void CxfSAMLBasicTests_wantAssertionsSignedTrue_missingSignatureEE8Only() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_2_in_1_samlWantAssertionsSigned_ee8.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        String expectedResponse = "Some error message";
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setRemoveTagInResponse("ds:Signature"); // the whole ds:Signature element

        updatedTestSettings.setCXFSettings(_testName, null, servicePort, serviceSecurePort, "user1", "user1pwd", "SAMLSOAPService2", "SAMLSoapPort2", "", "False", null, null);

        String CXF_SAML_TOKEN_GENERAL_FAILURE_MSG = "SAML token security failure";
        List<validationData> expectations = helpers.setErrorSAMLCXFExpectations(null, flowType, updatedTestSettings, CXF_SAML_TOKEN_GENERAL_FAILURE_MSG);
        
        // The server is NOT logging an error message indicating the real cause of the failure - defect 251665 has been opened to add a message
        // for now, we can look for a debug message that is logged in the trace... (btw, the error in the commented out expectation is just a best guess at what message will actually be logged)
        //        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did NOT fail because the assertion did NOT require a signature.", SAMLMessageConstants.CWWKS5048E_ERROR_VERIFYING_SIGNATURE);
        
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_TRACE_LOG, SAMLConstants.STRING_CONTAINS, "Did NOT fail because the assertion did NOT require a signature.", "verifySubjectConfirmationMethod A Bearer Assertion was not signed");

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, expectations);
    }
    
    // 	public void CxfSAMLBasicTests_wantAssertionsSignedFalse_missingSignature() - tested with CxfSAMLBasicTests_clockSkew_test
    //		had to set wantAssertionsSigned to false in order to manipulate the time in the SAML Token

    /**
     * TestDescription:
     *
     * In this scenario, the SAML token is updated to remove the
     * signature.
     * This test will have standard SAML Config
     * The WSSecurity Provider will have audienceRestrictions set to a valid list of "audiences"
     * The test Expects success
     *
     */
    
    @SkipForRepeat({ EE8_FEATURES })
    @Test
    public void CxfSAMLBasicTests_audienceRestrictions_multiple_validEE7Only() throws Exception {
        
    	testSAMLServer.reconfigServer(buildSPServerName("server_2_in_1_audienceRestrictions_multiple_valid.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        updatedTestSettings.setCXFSettings(_testName, null, servicePort, serviceSecurePort, "user1", "user1pwd", "SAMLSOAPService2", "SAMLSoapPort2", "", "False", null, null);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings));
    }
    
    @SkipForRepeat({ NO_MODIFICATION })
    @AllowedFFDC(value = { "java.util.MissingResourceException" }, repeatAction = { EE8FeatureReplacementAction.ID })
    @Test
    public void CxfSAMLBasicTests_audienceRestrictions_multiple_validEE8Only() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_2_in_1_audienceRestrictions_multiple_valid_ee8.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        updatedTestSettings.setCXFSettings(_testName, null, servicePort, serviceSecurePort, "user1", "user1pwd", "SAMLSOAPService2", "SAMLSoapPort2", "", "False", null, null);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings));
    }

    @SkipForRepeat({ EE8_FEATURES })
    @ExpectedFFDC(value = { "org.apache.ws.security.WSSecurityException" }, repeatAction = { EmptyAction.ID })
    @Test
    public void CxfSAMLBasicTests_audienceRestrictions_multiple_invalidEE7Only() throws Exception {
        
    	testSAMLServer.reconfigServer(buildSPServerName("server_2_in_1_audienceRestrictions_multiple_invalid.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        updatedTestSettings.setCXFSettings(_testName, null, servicePort, serviceSecurePort, "user1", "user1pwd", "SAMLSOAPService2", "SAMLSoapPort2", "", "False", null, null);

        List<validationData> expectations = helpers.setErrorSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_GENERAL_FAILURE_MSG);
        
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did NOT fail because the audienceRestrictions weren't satisfied.", null, audienceRestrictError);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, expectations);
    }
    
    @SkipForRepeat({ NO_MODIFICATION })
    @AllowedFFDC(value = { "java.util.MissingResourceException", "org.apache.wss4j.common.ext.WSSecurityException" }, repeatAction = { EE8FeatureReplacementAction.ID })
    @Test
    public void CxfSAMLBasicTests_audienceRestrictions_multiple_invalidEE8Only() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_2_in_1_audienceRestrictions_multiple_invalid_ee8.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        updatedTestSettings.setCXFSettings(_testName, null, servicePort, serviceSecurePort, "user1", "user1pwd", "SAMLSOAPService2", "SAMLSoapPort2", "", "False", null, null);

        String CXF_SAML_TOKEN_GENERAL_FAILURE_MSG = "SAML token security failure";
        List<validationData> expectations = helpers.setErrorSAMLCXFExpectations(null, flowType, updatedTestSettings, CXF_SAML_TOKEN_GENERAL_FAILURE_MSG);
        
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did NOT fail because the audienceRestrictions weren't satisfied.", null, audienceRestrictError);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, expectations);
    }

    @SkipForRepeat({ EE8_FEATURES })
    @Test
    public void CxfSAMLBasicTests_audienceRestrictions_single_validEE7Only() throws Exception {
        
    	testSAMLServer.reconfigServer(buildSPServerName("server_2_in_1_audienceRestrictions_single_valid.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        updatedTestSettings.setCXFSettings(_testName, null, servicePort, serviceSecurePort, "user1", "user1pwd", "SAMLSOAPService2", "SAMLSoapPort2", "", "False", null, null);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings));
    }
    
    @SkipForRepeat({ NO_MODIFICATION })
    @AllowedFFDC(value = { "java.util.MissingResourceException" }, repeatAction = { EE8FeatureReplacementAction.ID })
    @Test
    public void CxfSAMLBasicTests_audienceRestrictions_single_validEE8Only() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_2_in_1_audienceRestrictions_single_valid_ee8.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        updatedTestSettings.setCXFSettings(_testName, null, servicePort, serviceSecurePort, "user1", "user1pwd", "SAMLSOAPService2", "SAMLSoapPort2", "", "False", null, null);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings));
    }
    

    @SkipForRepeat({ EE8_FEATURES })
    @ExpectedFFDC(value = { "org.apache.ws.security.WSSecurityException" }, repeatAction = { EmptyAction.ID })
    @Test
    public void CxfSAMLBasicTests_audienceRestrictions_single_invalidEE7Only() throws Exception {
    
    	testSAMLServer.reconfigServer(buildSPServerName("server_2_in_1_audienceRestrictions_single_invalid.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        updatedTestSettings.setCXFSettings(_testName, null, servicePort, serviceSecurePort, "user1", "user1pwd", "SAMLSOAPService2", "SAMLSoapPort2", "", "False", null, null);

        List<validationData> expectations = helpers.setErrorSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_GENERAL_FAILURE_MSG);
        
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did NOT fail because the audienceRestrictions weren't satisfied.", null, audienceRestrictError);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, expectations);
    }
    
    @SkipForRepeat({ NO_MODIFICATION })
    @AllowedFFDC(value = { "java.util.MissingResourceException", "org.apache.wss4j.common.ext.WSSecurityException" }, repeatAction = { EE8FeatureReplacementAction.ID })
    @Test
    public void CxfSAMLBasicTests_audienceRestrictions_single_invalidEE8Only() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_2_in_1_audienceRestrictions_single_invalid_ee8.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        updatedTestSettings.setCXFSettings(_testName, null, servicePort, serviceSecurePort, "user1", "user1pwd", "SAMLSOAPService2", "SAMLSoapPort2", "", "False", null, null);

        String CXF_SAML_TOKEN_GENERAL_FAILURE_MSG = "SAML token security failure";
        List<validationData> expectations = helpers.setErrorSAMLCXFExpectations(null, flowType, updatedTestSettings, CXF_SAML_TOKEN_GENERAL_FAILURE_MSG);
        
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did NOT fail because the audienceRestrictions weren't satisfied.", null, audienceRestrictError);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, expectations);
    }
    

}
