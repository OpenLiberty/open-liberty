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
package com.ibm.ws.security.saml.fat.jaxrs.common;

import java.util.List;

import org.junit.Test;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTest;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;
import com.ibm.ws.security.saml20.fat.commonTest.utils.RSCommonUtils;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServerWrapper;

@LibertyServerWrapper
@Mode(TestMode.FULL)
public class RSSamlBasicTests extends SAMLCommonTest {

    private static final Class<?> thisClass = RSSamlBasicTests.class;
    protected static String SPServerName = "com.ibm.ws.security.saml.sso-2.0_fat.jaxrs.sp";
    protected static String APPServerName = "com.ibm.ws.security.saml.sso-2.0_fat.jaxrs.rs";
    protected static String MergedServerName = "com.ibm.ws.security.saml.sso-2.0_fat.jaxrs.merged_sp_rs";
    protected static RSCommonUtils commonUtils = new RSCommonUtils();
    protected static String servicePort = null;
    protected static String serviceSecurePort = null;

    /**
     * setup test settings that allow us to use a different app
     */
    private SAMLTestSettings changeTestApps(String appExtension) throws Exception {
        return commonUtils.changeTestApps(testAppServer, testSettings, appExtension);
    }

    /**
     * Tests main flow with SAML and JaxRS
     */

    @Mode(TestMode.LITE)
    @Test
    //	@AllowedFFDC("java.lang.NoClassDefFoundError")
    public void RSSamlBasicTests_mainFlow() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, testSettings);

        genericSAML(_testName, webClient, testSettings, throughJAXRSGet, expectations);
    }

    /**
     * Tests send a garbage saml token - it should be detected correctly
     */

    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @AllowedFFDC(value = { "java.lang.NullPointerException", "net.shibboleth.utilities.java.support.xml.XMLParserException", "org.opensaml.messaging.decoder.MessageDecodingException" })
    @Test
    public void RSSamlBasicTests_mangleSAMLToken_sendGarbage() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setSamlTokenReplaceVars("*", "Just send a string of garbage", SAMLConstants.LOCATION_ALL);

        List<validationData> expectations = msgUtils.addrsSamlUnauthorizedExpectation(SAMLConstants.INVOKE_JAXRS_GET, null);
        expectations = helpers.addMessageExpectation(testAppServer, expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.APP_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive exception that the SAML Token did not validate.", SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR);

        genericSAML(_testName, webClient, updatedTestSettings, throughJAXRSGet, expectations);
    }

    /**
     * Tests removes the saml token, but the config requires a signed token, so it'll
     */

    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    //	@Mode(TestMode.LITE)
    @Test
    public void RSSamlBasicTests_mangleSAMLToken_removeSignature_serverRequiresSign() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRemoveTagInResponse("ds:Signature"); // the whole ds:Signature element

        List<validationData> expectations = msgUtils.addrsSamlUnauthorizedExpectation(SAMLConstants.INVOKE_JAXRS_GET, null);
        expectations = helpers.addMessageExpectation(testAppServer, expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.APP_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive exception that the SAML Token did not validate.", SAMLMessageConstants.CWWKS5048E_ERROR_VERIFYING_SIGNATURE);

        genericSAML(_testName, webClient, updatedTestSettings, throughJAXRSGet, expectations);
    }

    /**
     * Tests removes the saml token, but the config allows an unsigned token, so it's ok
     */

    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.messaging.handler.MessageHandlerException" })
    @Test
    public void RSSamlBasicTests_mangleSAMLToken_userNameInAssertion() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();

        // upper case the first character of the username
        String updatedUserName = Character.toUpperCase(updatedTestSettings.getIdpUserName().charAt(0)) + updatedTestSettings.getIdpUserName().substring(1);
        updatedTestSettings.setSamlTokenReplaceVars(updatedTestSettings.getIdpUserName(), updatedUserName, SAMLConstants.LOCATION_ALL);

        List<validationData> expectations = msgUtils.addrsSamlUnauthorizedExpectation(SAMLConstants.INVOKE_JAXRS_GET, null);
        expectations = helpers.addMessageExpectation(testAppServer, expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.APP_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive exception that the SAML Token did not validate.", SAMLMessageConstants.CWWKS5049E_SIGNATURE_NOT_TRUSTED_OR_VALID);

        genericSAML(_testName, webClient, updatedTestSettings, throughJAXRSGet, expectations);
    }

    /**
     * The default server SSL Trust store does NOT contain the cert used in the SAML token
     *
     * @throws Exception
     */
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.messaging.handler.MessageHandlerException" })
    @Mode(TestMode.LITE)
    @Test
    public void RSSamlBasicTests_samlCertNotInDefaultTrust_wantAssertionsSigned_true() throws Exception {

        // in one server tests, we can't have a config with 1 default trust store that has and doesn't have
        // the required cert. So, we need to skip this test in one server tests.
        if (testAppServer == testSAMLServer) {
            return;
        }
        // need to reconfig instead of just using a different rs_saml config because we're updating the ssl config
        testAppServer.reconfigServer(buildSPServerName("server_samlCertNotInDefaultTrust.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = getAndSaveWebClient();

        List<validationData> expectations = msgUtils.addrsSamlUnauthorizedExpectation(SAMLConstants.INVOKE_JAXRS_GET, null);
        expectations = helpers.addMessageExpectation(testAppServer, expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.APP_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not fail with a signature not trusted error.", SAMLMessageConstants.CWWKS5049E_SIGNATURE_NOT_TRUSTED_OR_VALID);

        // make sure we get the error page indicating that we're not authorized
        genericSAML(_testName, webClient, testSettings, throughJAXRSGet, expectations);

    }

    /**
     * The default server SSL Trust store does NOT contain the cert used in the SAML token - even though the config does NOT
     * require
     * a signature - since one is there, it must verify
     *
     * @throws Exception
     */
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "com.ibm.wsspi.channelfw.exception.InvalidChainNameException", "org.opensaml.messaging.handler.MessageHandlerException" })
    @Mode(TestMode.LITE)
    @Test
    public void RSSamlBasicTests_samlCertNotInDefaultTrust_wantAssertionsSigned_false() throws Exception {

        // in one server tests, we can't have a config with 1 default trust store that has and doesn't have
        // the required cert. So, we need to skip this test in one server tests.
        if (testAppServer == testSAMLServer) {
            return;
        }

        // need to reconfig instead of just using a different rs_saml config because we're updating the ssl config
        testAppServer.reconfigServer(buildSPServerName("server_samlCertNotInDefaultTrust_wantAssertionsSignedFalse.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = getAndSaveWebClient();

        SAMLTestSettings updatedTestSettings = changeTestApps("mangled");

        List<validationData> expectations = msgUtils.addrsSamlUnauthorizedExpectation(SAMLConstants.INVOKE_JAXRS_GET, null);
        expectations = helpers.addMessageExpectation(testAppServer, expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.APP_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not fail with a signature not trusted error.", SAMLMessageConstants.CWWKS5049E_SIGNATURE_NOT_TRUSTED_OR_VALID);
        testAppServer.addIgnoredServerException("CWWKE0701E");

        // make sure we get the error page indicating that we're not authorized
        genericSAML(_testName, webClient, updatedTestSettings, throughJAXRSGet, expectations);

    }

    /**
     * The rs_saml SSL Trust store does NOT contain the cert used in the SAML token
     *
     * @throws Exception
     */
    @ExpectedFFDC(value = {"com.ibm.ws.security.saml.error.SamlException", "org.opensaml.messaging.handler.MessageHandlerException"})
    @Mode(TestMode.LITE)
    @Test
    public void RSSamlBasicTests_samlCertNotInRSSamlTrust_wantAssertionsSigned_true() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SAMLTestSettings updatedTestSettings = changeTestApps("badTrust_mangled_true");

        List<validationData> expectations = msgUtils.addrsSamlUnauthorizedExpectation(SAMLConstants.INVOKE_JAXRS_GET, null);
        expectations = helpers.addMessageExpectation(testAppServer, expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.APP_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not fail with a signature not trusted error.", SAMLMessageConstants.CWWKS5049E_SIGNATURE_NOT_TRUSTED_OR_VALID);

        // make sure we get the error page indicating that we're not authorized
        genericSAML(_testName, webClient, updatedTestSettings, throughJAXRSGet, expectations);

    }

    /**
     * The rs_saml SSL Trust store does NOT contain the cert used in the SAML token - even though the config does NOT require
     * a signature - since one is there, it must verify
     *
     * @throws Exception
     */
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException","org.opensaml.messaging.handler.MessageHandlerException" })
    @Mode(TestMode.LITE)
    @Test
    public void RSSamlBasicTests_samlCertNotInRSSamlTrust_wantAssertionsSigned_false() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SAMLTestSettings updatedTestSettings = changeTestApps("badTrust_mangled_false");

        List<validationData> expectations = msgUtils.addrsSamlUnauthorizedExpectation(SAMLConstants.INVOKE_JAXRS_GET, null);
        expectations = helpers.addMessageExpectation(testAppServer, expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.APP_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not fail with a signature not trusted error.", SAMLMessageConstants.CWWKS5049E_SIGNATURE_NOT_TRUSTED_OR_VALID);

        // make sure we get the error page indicating that we're not authorized
        genericSAML(_testName, webClient, updatedTestSettings, throughJAXRSGet, expectations);

    }

    //	@Mode(TestMode.LITE)
    @Test
    public void RSSamlBasicTests_MinConfig() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", "sp2", true);
        updatedTestSettings.setSpTargetApp(testAppServer.getServerHttpsString() + "/" + SAMLConstants.PARTIAL_HELLO_WORLD_URI + "_sp2");
        updatedTestSettings.setRSSettings(SAMLConstants.SAML_DEFAULT_AUTHORIZATION_HEADER_NAME, updatedTestSettings.getRSSettings().getHeaderFormat(), updatedTestSettings.getRSSettings().getSamlTokenFormat());

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, updatedTestSettings);

        genericSAML(_testName, webClient, updatedTestSettings, throughJAXRSGet, expectations);

    }

    @Mode(TestMode.LITE)
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void RSSamlBasicTests_OmitSAMLAssertion() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRSSettings(null, updatedTestSettings.getRSSettings().getHeaderFormat(), updatedTestSettings.getRSSettings().getSamlTokenFormat());

        List<validationData> expectations = msgUtils.addrsSamlUnauthorizedExpectation(SAMLConstants.INVOKE_JAXRS_GET, null);
        expectations = helpers.addMessageExpectation(testAppServer, expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.APP_MESSAGES_LOG, SAMLConstants.STRING_MATCHES, "Did not receive exception that the SAML Assertion was not passed in the header on the request.", ".*" + SAMLMessageConstants.CWWKS5013E_MISSING_SAML_ASSERTION_ERROR + ".*" + "[saml_token]");
        testAppServer.addIgnoredServerException("CWWKS5008E");
        genericSAML(_testName, webClient, updatedTestSettings, throughJAXRSGet, expectations);

    }

    /**
     * Tests
     */

    //@ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.messaging.handler.MessageHandlerException" })
    //@Mode(TestMode.LITE)
    //@Test - IdP is updated to use sha256 signature algorithm , this test is not valid anymore. when we update samlWeb to support more signature algorithms , use this test
    public void RSSamlBasicTests_signatureAlgorithNotSatisfied() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setSpTargetApp(testAppServer.getServerHttpsString() + "/" + SAMLConstants.PARTIAL_HELLO_WORLD_URI + "_sp2");
        updatedTestSettings.setRSSettings(SAMLConstants.SAML_DEFAULT_AUTHORIZATION_HEADER_NAME, updatedTestSettings.getRSSettings().getHeaderFormat(), updatedTestSettings.getRSSettings().getSamlTokenFormat());

        List<validationData> expectations = msgUtils.addrsSamlUnauthorizedExpectation(SAMLConstants.INVOKE_JAXRS_GET, null);
        expectations = helpers.addMessageExpectation(testAppServer, expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.APP_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive exception that the SAML Token did not validate.", SAMLMessageConstants.CWWKS5049E_SIGNATURE_NOT_TRUSTED_OR_VALID);

        genericSAML(_testName, webClient, updatedTestSettings, throughJAXRSGet, expectations);
    }

    /**
     * Tests
     */

    @Mode(TestMode.LITE)
    @Test
    public void RSSamlBasicTests_useSAMLAssertionAgain() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, testSettings);

        Object somePage = genericSAML(_testName, webClient, testSettings, justGetSAMLToken, expectations);

        helpers.runGetMethod(_testName, somePage, testSettings, expectations);

        helpers.runGetMethod(_testName, somePage, testSettings, expectations);

    }

    /*
     * ***************************************************************************************************************************
     * *******************
     */

    /**
     * Tests removes the saml token, but the config allows an unsigned token, so it's ok
     */

    @Test
    public void RSSamlBasicTests_mangleSAMLToken_removeSignature_serverDoesntRequiresSign() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        //		SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        SAMLTestSettings updatedTestSettings = changeTestApps("mangled");
        updatedTestSettings.setRemoveTagInResponse("ds:Signature"); // the whole ds:Signature element

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, updatedTestSettings);

        genericSAML(_testName, webClient, updatedTestSettings, throughJAXRSGet, expectations);
    }

    //	@ExpectedFFDC(value={"com.ibm.ws.security.saml.error.SamlException"})
    @Test
    public void RSSamlBasicTests_mangleSAMLToken_missingNameId() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SAMLTestSettings updatedTestSettings = changeTestApps("mangled");

        // remove the signature and nameid
        updatedTestSettings.setRemoveTagInResponse("saml2:NameID", "ds:Signature");

        List<validationData> expectations = msgUtils.addrsSamlUnauthorizedExpectation(SAMLConstants.INVOKE_JAXRS_GET, null);
        expectations = helpers.addMessageExpectation(testAppServer, expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.APP_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive exception that the SAML Token did not validate.", SAMLMessageConstants.CWWKS5068E_MISSING_ATTRIBUTE);

        genericSAML(_testName, webClient, updatedTestSettings, throughJAXRSGet, expectations);
    }

    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void RSSamlBasicTests_useToken_before_NotBefore() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SAMLTestSettings updatedTestSettings = changeTestApps("mangled");

        updatedTestSettings.setRemoveTagInResponse("ds:Signature");
        updatedTestSettings.setSamlTokenUpdateTimeVars(SAMLConstants.SAML_NOT_BEFORE, SAMLConstants.ADD_TIME, SAMLTestSettings.setTimeArray(0, 0, 10, 0), SAMLConstants.DO_NOT_USE_CURRENT_TIME);

        List<validationData> expectations = msgUtils.addrsSamlUnauthorizedExpectation(SAMLConstants.INVOKE_JAXRS_GET, null);
        expectations = helpers.addMessageExpectation(testAppServer, expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.APP_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not fail with a clockSkew error.", SAMLMessageConstants.CWWKS5057E_NOT_BEFORE_OUT_OF_RANGE);

        genericSAML(_testName, webClient, updatedTestSettings, throughJAXRSGet, expectations);
    }

    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void RSSamlBasicTests_useToken_after_NotOnOrAfter() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SAMLTestSettings updatedTestSettings = changeTestApps("mangled");

        updatedTestSettings.setRemoveTagInResponse("ds:Signature");
        updatedTestSettings.setSamlTokenUpdateTimeVars(SAMLConstants.SAML_NOT_ON_OR_AFTER, SAMLConstants.SUBTRACT_TIME, SAMLTestSettings.setTimeArray(0, 0, 10, 0), SAMLConstants.DO_NOT_USE_CURRENT_TIME);

        List<validationData> expectations = msgUtils.addrsSamlUnauthorizedExpectation(SAMLConstants.INVOKE_JAXRS_GET, null);
        expectations = helpers.addMessageExpectation(testAppServer, expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.APP_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not fail with a clockSkew error.", SAMLMessageConstants.CWWKS5053E_NOT_ON_OR_AFTER_OUT_OF_RANGE);

        genericSAML(_testName, webClient, updatedTestSettings, throughJAXRSGet, expectations);
    }

    /**
     * Set SessionNotOnOrAfter before the current time (accounting for clock skew).
     * Test shows that we get an exception that the SessionNotOnOrAfter is out of bounds - we expect to get the error page with
     * a descriptive msg in the server side log
     *
     * @throws Exception
     */
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Mode(TestMode.LITE)
    @Test
    public void RSSamlBasicTests_useToken_after_SessionNotOnOrAfter() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SAMLTestSettings updatedTestSettings = changeTestApps("mangled");
        updatedTestSettings.setRemoveTagInResponse("ds:Signature");
        updatedTestSettings.setSamlTokenUpdateTimeVars(SAMLConstants.SAML_SESSION_NOT_ON_OR_AFTER, SAMLConstants.SUBTRACT_TIME, SAMLTestSettings.setTimeArray(0, 1, 10, 0), SAMLConstants.DO_NOT_USE_CURRENT_TIME);

        List<validationData> expectations = msgUtils.addrsSamlUnauthorizedExpectation(SAMLConstants.INVOKE_JAXRS_GET, null);
        expectations = helpers.addMessageExpectation(testAppServer, expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.APP_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not fail with a clockSkew error.", SAMLMessageConstants.CWWKS5062E_SESSION_NOT_ON_OR_AFTER_OUT_OF_RANGE);

        // make sure we get the error page indicating that we're not authorized
        genericSAML(_testName, webClient, updatedTestSettings, throughJAXRSGet, expectations);

    }

    /**
     * Call an unprotected app running on the same server.
     * Make sure that we can get to the app directly - no login, ...
     *
     * @throws Exception
     */
    //	@Mode(TestMode.LITE)
    @SkipForRepeat({SkipForRepeat.EE9_FEATURES, SkipForRepeat.EE10_FEATURES})
    @Test
    public void RSSamlBasicTests_unprotectedApp() throws Exception {

        String unAuthenticated = "Principal: WSPrincipal:UNAUTHENTICATED";

        WebClient webClient = getAndSaveWebClient();

        // access unprotected app
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setSpTargetApp(updatedTestSettings.getSpTargetApp().replace("rest", "rest2"));
        updatedTestSettings.setSpDefaultApp(updatedTestSettings.getSpDefaultApp().replace("rest", "rest2"));

        List<validationData> expectations = vData.addSuccessStatusCodes();
        expectations = vData.addExpectation(expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get unauthenticated message from the test app", null, unAuthenticated);
        expectations = vData.addExpectation(expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get unauthenticated message from the test app", null, "Accessed helloworld with no SAML Token");
        genericSAML(_testName, webClient, updatedTestSettings, SAMLConstants.JUST_INVOKE_JAXRS_GET, expectations);

        // access protected app
        List<validationData> expectations2 = commonUtils.getGoodExpectationsForJaxrsGet(flowType, testSettings);
        genericSAML(_testName, webClient, testSettings, throughJAXRSGet, expectations2);

        // access the unprotected app again - make sure we have no credentials
        genericSAML(_testName, webClient, updatedTestSettings, SAMLConstants.JUST_INVOKE_JAXRS_GET, expectations);

    }

    /**
     * Test various header formats along with the supported token formats
     * Header formats:
     * Authorization=[<headerName>=<SAML_HERE>]
     * Authorization=[<headerName>="<SAML_HERE>"]
     * Authorization=[<headerName> <SAML_HERE>]
     * <headerName>=[<SAML_HERE>]
     * Token formats: encoded, compressed & encoded, text
     */
    //chc@Test
    //    public void RSSamlBasicTests_headerFormatVariations() throws Exception {
    //
    //        WebClient webClient = getWebClient();
    //
    //        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, testSettings);
    //
    //        Object somePage = genericSAML(_testName, webClient, testSettings, justGetSAMLToken, expectations);
    //
    //        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
    //
    //        for (String headerFormat : SAMLConstants.SAML_HEADER_FORMATS) {
    //            for (String tokenFormat : SAMLConstants.SAML_TOKEN_FORMATS) {
    //                updatedTestSettings.setRSSettings(updatedTestSettings.getRSSettings().getHeaderName(), headerFormat, tokenFormat);
    //                helpers.runGetMethod(_testName, somePage, updatedTestSettings, expectations);
    //            }
    //        }
    //
    //    }

    @Test
    public void RSSamlBasicTests_headerFormatVariations_ASSERTION_TEXT_ONLY() throws Exception {

        // The Shibboleth server gemerates a SAML Response with embedded new lines (haven't figured out how to prevent that)
        // The new lines cause problems with HTTPURLConnection.  We'll remove the new lines, but, that then causes problems validating
        //   the signature.  So, we have to remove that.  If we do that, we also need to have configs that don't require the response
        //   to be signed, so, update the config
        testAppServer.reconfigServer(buildSPServerName("server_1_text_only.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        if (testAppServer != testSAMLServer) {
            testSAMLServer.reconfigServer(buildSPServerName("server_1_text_only.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        }

        WebClient webClient = getAndSaveWebClient();

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, testSettings);

        Object somePage = genericSAML(_testName, webClient, testSettings, justGetSAMLToken, expectations);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        for (String headerFormat : SAMLConstants.SAML_HEADER_FORMATS) {
            Log.info(thisClass, _testName, "Test with Header format: " + headerFormat);
            String tokenFormat = SAMLConstants.ASSERTION_TEXT_ONLY;
            updatedTestSettings.setRSSettings(updatedTestSettings.getRSSettings().getHeaderName(), headerFormat, tokenFormat);
            helpers.runGetMethod(_testName, somePage, updatedTestSettings, expectations);

        }

    }

    @Test
    public void RSSamlBasicTests_headerFormatVariations_ASSERTION_ENCODED() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, testSettings);

        Object somePage = genericSAML(_testName, webClient, testSettings, justGetSAMLToken, expectations);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();

        for (String headerFormat : SAMLConstants.SAML_HEADER_FORMATS) {
            Log.info(thisClass, _testName, "Test with Header format: " + headerFormat);
            String tokenFormat = SAMLConstants.ASSERTION_ENCODED;
            updatedTestSettings.setRSSettings(updatedTestSettings.getRSSettings().getHeaderName(), headerFormat, tokenFormat);
            helpers.runGetMethod(_testName, somePage, updatedTestSettings, expectations);

        }

    }

    @Test
    public void RSSamlBasicTests_headerFormatVariations_ASSERTION_COMPRESSED_ENCODED() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, testSettings);

        Object somePage = genericSAML(_testName, webClient, testSettings, justGetSAMLToken, expectations);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();

        for (String headerFormat : SAMLConstants.SAML_HEADER_FORMATS) {
            Log.info(thisClass, _testName, "Test with Header format: " + headerFormat);
            String tokenFormat = SAMLConstants.ASSERTION_COMPRESSED_ENCODED;
            updatedTestSettings.setRSSettings(updatedTestSettings.getRSSettings().getHeaderName(), headerFormat, tokenFormat);
            helpers.runGetMethod(_testName, somePage, updatedTestSettings, expectations);

        }

    }

    /**
     * Test the default header names (saml, Saml, SAML)
     */
    @Test
    public void RSSamlBasicTests_defaultHeaderNames() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, testSettings);

        Object somePage = genericSAML(_testName, webClient, testSettings, justGetSAMLToken, expectations);

        SAMLTestSettings updatedTestSettings = changeTestApps("header1");

        String[] defHeaderNames = new String[] { "saml", "Saml", "SAML" };
        for (String headerName : defHeaderNames) {
            updatedTestSettings.setRSSettings(headerName, updatedTestSettings.getRSSettings().getHeaderFormat(), updatedTestSettings.getRSSettings().getSamlTokenFormat());
            helpers.runGetMethod(_testName, somePage, updatedTestSettings, expectations);
        }

    }

    /**
     * Test the other header names (diffHeader, otherHeader, oneMoreHeader)
     * (Use one that is a substring of a valid headername)
     */
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void RSSamlBasicTests_otherHeaderNames() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, testSettings);

        Object somePage = genericSAML(_testName, webClient, testSettings, justGetSAMLToken, expectations);

        SAMLTestSettings updatedTestSettings = changeTestApps("header2");

        String[] defHeaderNames = new String[] { "diffHeader", "otherHeader", "oneMoreHeader" };
        for (String headerName : defHeaderNames) {
            updatedTestSettings.setRSSettings(headerName, updatedTestSettings.getRSSettings().getHeaderFormat(), updatedTestSettings.getRSSettings().getSamlTokenFormat());
            helpers.runGetMethod(_testName, somePage, updatedTestSettings, expectations);
        }
        // now, for fun, try using a headerName that is a substring of a valid header name
        List<validationData> expectations2 = msgUtils.addrsSamlUnauthorizedExpectation(SAMLConstants.INVOKE_JAXRS_GET, null);
        expectations2 = helpers.addMessageExpectation(testAppServer, expectations2, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.APP_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not fail due to an invalid headerName", SAMLMessageConstants.CWWKS5013E_MISSING_SAML_ASSERTION_ERROR);

        updatedTestSettings.setRSSettings("other", updatedTestSettings.getRSSettings().getHeaderFormat(), updatedTestSettings.getRSSettings().getSamlTokenFormat());
        helpers.runGetMethod(_testName, somePage, updatedTestSettings, expectations2);

    }
}
