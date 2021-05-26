/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.fat.jaxrs.common;

import java.util.List;

import org.junit.Test;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTest;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;
import com.ibm.ws.security.saml20.fat.commonTest.utils.RSCommonUtils;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

@LibertyServerWrapper
@Mode(TestMode.FULL)
public class RSSamlAPITests extends SAMLCommonTest {

    private static final Class<?> thisClass = RSSamlAPITests.class;
    protected static String SPServerName = "com.ibm.ws.security.saml.sso-2.0_fat.jaxrs.sp";
    protected static String APPServerName = "com.ibm.ws.security.saml.sso-2.0_fat.jaxrs.rs";
    protected static RSCommonUtils commonUtils = new RSCommonUtils();
    protected static String servicePort = null;
    protected static String serviceSecurePort = null;
    protected static String[] actionList = null;
    protected static String endAction = null;
    protected static String className = null;

    /**
     * Call an app on the SP server.
     * use the getEncodedSaml20Token api to retrieve the encoded saml assertion
     * Invoke another test app on the RS server passing the encoded saml assertion in the header
     * 1) if this test is run in the IDP Initiated flow, we will get null for the saml
     * assertion and will NOT be able to access the next test app
     * 2) if this test is run in the Solicted SP Initated flow, we will get a valid encode saml
     * assertion and be able to access the next test app
     *
     * @throws Exception
     */
    //	@Mode(TestMode.LITE)
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.ws.message.decoder.MessageDecodingException", "org.opensaml.xml.parse.XMLParserException" })
    @Test
    public void RSSamlAPITests_useJaxRSCLientServlet_encodedAssertion() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRSSettings(updatedTestSettings.getRSSettings().getHeaderName(), updatedTestSettings.getRSSettings().getHeaderFormat(), SAMLConstants.ASSERTION_ENCODED);

        List<validationData> expectations = null;
        if (flowType.equals(SAMLConstants.IDP_INITIATED)) {

            expectations = vData.addSuccessStatusCodes(null, endAction);
            expectations = vData.addResponseStatusExpectation(expectations, endAction, SAMLConstants.UNAUTHORIZED_STATUS);
            expectations = helpers.addMessageExpectation(testSAMLServer, expectations, endAction, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did NOT find a message indicating that the SAML Token was not found in the Subject", SAMLMessageConstants.CWWKS5251W_SAML_TOKEN_NOT_IN_SUBJECT);

        } else {
            expectations = commonUtils.getGoodExpectationsForJaxrsAIPTests(flowType, updatedTestSettings);
        }

        genericSAML(_testName, webClient, updatedTestSettings, actionList, expectations);

    }

    /**
     * Call an app on the SP server.
     * use the getEncodedSaml20Token api to retrieve the compressed and encoded saml assertion
     * Invoke another test app on the RS server passing the compressed and encoded saml assertion in the header
     * 1) if this test is run in the IDP Initiated flow, we will get null for the saml
     * assertion and will NOT be able to access the next test app
     * 2) if this test is run in the Solicted SP Initated flow, we will get a valid compressed and encoded saml
     * assertion and be able to access the next test app
     *
     * @throws Exception
     */
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.ws.message.decoder.MessageDecodingException", "org.opensaml.xml.parse.XMLParserException" })
    //	@Mode(TestMode.LITE)
    @Test
    public void RSSamlAPITests_useJaxRSCLientServlet_compressedEncodedAssertion() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRSSettings(updatedTestSettings.getRSSettings().getHeaderName(), updatedTestSettings.getRSSettings().getHeaderFormat(), SAMLConstants.ASSERTION_COMPRESSED_ENCODED);

        List<validationData> expectations = null;
        if (flowType.equals(SAMLConstants.IDP_INITIATED)) {

            expectations = vData.addSuccessStatusCodes(null, endAction);
            expectations = vData.addResponseStatusExpectation(expectations, endAction, SAMLConstants.UNAUTHORIZED_STATUS);
            expectations = helpers.addMessageExpectation(testSAMLServer, expectations, endAction, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did NOT find a message indicating that the SAML Token was not found in the Subject", SAMLMessageConstants.CWWKS5251W_SAML_TOKEN_NOT_IN_SUBJECT);

            expectations = commonUtils.getGoodExpectationsForJaxrsAIPTests(flowType, updatedTestSettings);
        } else {
            expectations = vData.addSuccessStatusCodes();
        }

        genericSAML(_testName, webClient, updatedTestSettings, actionList, expectations);

    }

    /**
     * Call an app on the SP server.
     * use the getSaml20Token api to retrieve the text saml assertion
     * Invoke another test app on the RS server passing the text saml assertion in the header
     * 1) if this test is run in the IDP Initiated flow, we will get null for the saml
     * assertion and will NOT be able to access the next test app
     * 2) if this test is run in the Solicted SP Initated flow, we will get a valid text saml
     * assertion and be able to access the next test app
     *
     * @throws Exception
     */
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.ws.message.decoder.MessageDecodingException", "org.opensaml.xml.parse.XMLParserException" })
    //	@Mode(TestMode.LITE)
    @Test
    public void RSSamlAPITests_useJaxRSCLientServlet_textAssertion() throws Exception {

        testAppServer.reconfigServer(buildSPServerName("server_apiTest_text_only.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        if (testAppServer != testSAMLServer) {
            testSAMLServer.reconfigServer(buildSPServerName("server_apiTest_text_only.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        }

        WebClient webClient = getAndSaveWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRSSettings(updatedTestSettings.getRSSettings().getHeaderName(), updatedTestSettings.getRSSettings().getHeaderFormat(), SAMLConstants.ASSERTION_TEXT_ONLY);

        List<validationData> expectations = null;
        if (flowType.equals(SAMLConstants.IDP_INITIATED)) {
            // chc - update expectations when Aruna fixes the NPE
            expectations = vData.addSuccessStatusCodes(null, endAction);
            expectations = vData.addResponseStatusExpectation(expectations, endAction, SAMLConstants.UNAUTHORIZED_STATUS);
            expectations = helpers.addMessageExpectation(testSAMLServer, expectations, endAction, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did NOT find a message indicating that the SAML Token was not found in the Subject", SAMLMessageConstants.CWWKS5251W_SAML_TOKEN_NOT_IN_SUBJECT);
        } else {
            expectations = commonUtils.getGoodExpectationsForJaxrsAIPTests(flowType, updatedTestSettings);
        }

        genericSAML(_testName, webClient, updatedTestSettings, actionList, expectations);

    }

    /**
     * Call an app on the SP server.
     * Invoke another test app on the RS server passing junk as the saml assertion in the header
     * 1) if this test is run in the IDP Initiated flow, we will NOT be able to access the next test app
     * 2) if this test is run in the Solicted SP Initated flow, we will NOT be able to access the next test app
     *
     * @throws Exception
     */
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.ws.message.decoder.MessageDecodingException", "org.opensaml.xml.parse.XMLParserException" })
    //	@Mode(TestMode.LITE)
    @Test
    public void RSSamlAPITests_useJaxRSCLientServlet_junkInAssertion() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRSSettings(updatedTestSettings.getRSSettings().getHeaderName(), updatedTestSettings.getRSSettings().getHeaderFormat(), "junk");

        List<validationData> expectations = vData.addSuccessStatusCodes(null, endAction);
        expectations = vData.addResponseStatusExpectation(expectations, endAction, SAMLConstants.UNAUTHORIZED_STATUS);
        expectations = vData.addExpectation(expectations, endAction, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive and unauthorized message", null, SAMLConstants.HTTP_UNAUTHORIZED_EXCEPTION);

        genericSAML(_testName, webClient, updatedTestSettings, actionList, expectations);

    }

    /**
     * Call an app on the SP server.
     * Invoke another test app on the RS server passing an empty saml assertion in the header
     * 1) if this test is run in the IDP Initiated flow, we will NOT be able to access the next test app
     * 2) if this test is run in the Solicted SP Initated flow, we will NOT be able to access the next test app
     *
     * @throws Exception
     */
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    //	@Mode(TestMode.LITE)
    @Test
    public void RSSamlAPITests_useJaxRSCLientServlet_emptyAssertion() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRSSettings(updatedTestSettings.getRSSettings().getHeaderName(), updatedTestSettings.getRSSettings().getHeaderFormat(), "empty");

        List<validationData> expectations = vData.addSuccessStatusCodes(null, endAction);
        expectations = vData.addResponseStatusExpectation(expectations, endAction, SAMLConstants.UNAUTHORIZED_STATUS);
        expectations = vData.addExpectation(expectations, endAction, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive and unauthorized message", null, SAMLConstants.HTTP_UNAUTHORIZED_EXCEPTION);

        genericSAML(_testName, webClient, updatedTestSettings, actionList, expectations);

    }

    /**
     * Call an app on the SP server.
     * Set the client property "com.ibm.ws.jaxrs.client.saml.sendToken" to "true" and then
     * Invoke another test app on the RS server via the client that we just added this property to
     * 1) if this test is run in the IDP Initiated flow, the runtime will pass no saml assertion to the RS
     * server and we will NOT be able to access the next test app
     * 2) if this test is run in the Solicted SP Initated flow, the runtime will pass the current saml
     * assertion and will be able to access the next test app
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.ws.message.decoder.MessageDecodingException", "org.opensaml.xml.parse.XMLParserException" })
    @Test
    public void RSSamlAPITests_useJaxRSCLientServlet_runtimePropagateToken_setStringTrue() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRSSettings("Authorization", SAMLConstants.SAML_HEADER_5t, SAMLConstants.ASSERTION_ENCODED);

        List<validationData> expectations = null;
        if (flowType.equals(SAMLConstants.IDP_INITIATED)) {

            expectations = vData.addSuccessStatusCodes(null, endAction);
            expectations = vData.addResponseStatusExpectation(expectations, endAction, SAMLConstants.UNAUTHORIZED_STATUS);
            expectations = helpers.addMessageExpectation(testSAMLServer, expectations, endAction, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did NOT find a message indicating that the SAML Token was not found in the Subject", SAMLMessageConstants.CWWKS5251W_SAML_TOKEN_NOT_IN_SUBJECT);

        } else {
            expectations = commonUtils.getGoodExpectationsForJaxrsAIPTests(flowType, updatedTestSettings);
        }

        genericSAML(_testName, webClient, updatedTestSettings, actionList, expectations);

    }

    /**
     * Call an app on the SP server.
     * Set the client property "com.ibm.ws.jaxrs.client.saml.sendToken" to true and then
     * Invoke another test app on the RS server via the client that we just added this property to
     * 1) if this test is run in the IDP Initiated flow, the runtime will pass no saml assertion to the RS
     * server and we will NOT be able to access the next test app
     * 2) if this test is run in the Solicted SP Initated flow, the runtime will pass the current saml
     * assertion and will be able to access the next test app
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.ws.message.decoder.MessageDecodingException", "org.opensaml.xml.parse.XMLParserException" })
    @Test
    public void RSSamlAPITests_useJaxRSCLientServlet_runtimePropagateToken_setBooleanTrue() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRSSettings("Authorization", SAMLConstants.SAML_HEADER_6t, SAMLConstants.ASSERTION_ENCODED);

        List<validationData> expectations = null;
        if (flowType.equals(SAMLConstants.IDP_INITIATED)) {

            expectations = vData.addSuccessStatusCodes(null, endAction);
            expectations = vData.addResponseStatusExpectation(expectations, endAction, SAMLConstants.UNAUTHORIZED_STATUS);
            expectations = helpers.addMessageExpectation(testSAMLServer, expectations, endAction, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did NOT find a message indicating that the SAML Token was not found in the Subject", SAMLMessageConstants.CWWKS5251W_SAML_TOKEN_NOT_IN_SUBJECT);

        } else {
            expectations = commonUtils.getGoodExpectationsForJaxrsAIPTests(flowType, updatedTestSettings);
        }

        genericSAML(_testName, webClient, updatedTestSettings, actionList, expectations);

    }

    /**
     * Call an app on the SP server.
     * Set the client property "com.ibm.ws.jaxrs.client.saml.sendToken" to "false" and then
     * Invoke another test app on the RS server via the client that we just added this property to
     * The request will fail with a 403 as we're not able to access the app - we set the flag to NOT propagate
     * the token, but we don't pass the token...
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.ws.message.decoder.MessageDecodingException", "org.opensaml.xml.parse.XMLParserException" })
    @Test
    public void RSSamlAPITests_useJaxRSCLientServlet_runtimePropagateToken_setStringFalse() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRSSettings("Authorization", SAMLConstants.SAML_HEADER_5f, SAMLConstants.ASSERTION_ENCODED);

        List<validationData> expectations = null;
        expectations = vData.addSuccessStatusCodes(null, endAction);
        expectations = vData.addResponseStatusExpectation(expectations, endAction, SAMLConstants.UNAUTHORIZED_STATUS);
        if (flowType.equals(SAMLConstants.IDP_INITIATED)) {
            expectations = helpers.addMessageExpectation(testSAMLServer, expectations, endAction, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did NOT find a message indicating that the SAML Token was not found in the Subject", SAMLMessageConstants.CWWKS5251W_SAML_TOKEN_NOT_IN_SUBJECT);
        } else {
            expectations = helpers.addMessageExpectation(testSAMLServer, expectations, endAction, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did NOT find a message indicating that the we are not authorized (no token was passed).", SAMLConstants.HTTP_UNAUTHORIZED_EXCEPTION);
        }

        genericSAML(_testName, webClient, updatedTestSettings, actionList, expectations);

    }

    /**
     * Call an app on the SP server.
     * Set the client property "com.ibm.ws.jaxrs.client.saml.sendToken" to false and then
     * Invoke another test app on the RS server via the client that we just added this property to
     * The request will fail with a 403 as we're not able to access the app - we set the flag to NOT propagate
     * the token, but we don't pass the token...
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.ws.message.decoder.MessageDecodingException", "org.opensaml.xml.parse.XMLParserException" })
    @Test
    public void RSSamlAPITests_useJaxRSCLientServlet_runtimePropagateToken_setBooleanFalse() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRSSettings("Authorization", SAMLConstants.SAML_HEADER_6f, SAMLConstants.ASSERTION_ENCODED);

        List<validationData> expectations = null;
        expectations = vData.addSuccessStatusCodes(null, endAction);
        expectations = vData.addResponseStatusExpectation(expectations, endAction, SAMLConstants.UNAUTHORIZED_STATUS);
        if (flowType.equals(SAMLConstants.IDP_INITIATED)) {
            expectations = helpers.addMessageExpectation(testSAMLServer, expectations, endAction, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did NOT find a message indicating that the SAML Token was not found in the Subject", SAMLMessageConstants.CWWKS5251W_SAML_TOKEN_NOT_IN_SUBJECT);
        } else {
            expectations = helpers.addMessageExpectation(testSAMLServer, expectations, endAction, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did NOT find a message indicating that the we are not authorized (no token was passed).", SAMLConstants.HTTP_UNAUTHORIZED_EXCEPTION);
        }

        genericSAML(_testName, webClient, updatedTestSettings, actionList, expectations);

    }
}
