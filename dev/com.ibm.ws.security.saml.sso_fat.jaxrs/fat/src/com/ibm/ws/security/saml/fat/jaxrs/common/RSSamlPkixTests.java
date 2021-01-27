/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.fat.jaxrs.common;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTest;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;
import com.ibm.ws.security.saml20.fat.commonTest.utils.RSCommonUtils;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * These tests verify the behavior of the pkixTrustEngine
 * These tests are run once with a server level trust store that has
 * the cert issued by the IDP and once with the server trust that does
 * NOT contain the cert issued by the IDP.
 *
 * @author chrisc
 *
 */
@LibertyServerWrapper
@Mode(TestMode.FULL)
public class RSSamlPkixTests extends SAMLCommonTest {

    private static final Class<?> thisClass = RSSamlPkixTests.class;
    protected static String SPServerName = "com.ibm.ws.security.saml.sso-2.0_fat.jaxrs.sp";
    protected static String APPServerName = "com.ibm.ws.security.saml.sso-2.0_fat.jaxrs.rs";
    protected static RSCommonUtils commonUtils = new RSCommonUtils();
    protected static String servicePort = null;
    protected static String serviceSecurePort = null;
    protected static String APPServerConfig = "";
    protected static Boolean serverHasIDPCert = true;

    // @BeforeClass
    public static void setupBeforeTest() throws Exception {

        flowType = SAMLConstants.IDP_INITIATED;

        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");
        // add any additional messages that you want the "start" to wait for
        // we should wait for any providers that this test requires
        List<String> extraMsgs = getDefaultSAMLStartMsgs();

        List<String> extraMsgs2 = new ArrayList<String>();
        extraMsgs2.add(SAMLMessageConstants.CWPKI0807W_KEYSTORE_NOT_FOUND);
        extraMsgs2.add(SAMLMessageConstants.CWPKI0809W_FAILURE_LOADING_KEYSTORE);
        extraMsgs2.add(SAMLMessageConstants.CWPKI0033E_KEYSTORE_DID_NOT_LOAD);

        List<String> extraApps = new ArrayList<String>();
        extraApps.add(SAMLConstants.SAML_CLIENT_APP);
        List<String> extraApps2 = new ArrayList<String>();
        extraApps2.add("helloworld");

        copyMetaData = false;

        testAppServer = commonSetUp(APPServerName, APPServerConfig, SAMLConstants.SAML_ONLY_SETUP, SAMLConstants.APP_SERVER_TYPE, extraApps2, extraMsgs2, false);
        commonUtils.fixServer2Ports(testAppServer);
        startSPWithIDPServer(SPServerName, "server_1.xml", extraMsgs, extraApps, true);

        setActionsForFlowType(flowType);

        // set default values for jaxrs settings
        testSettings.setRSSettings();

        // set test app
        testSettings.setSpTargetApp(testAppServer.getServerHttpsString() + "/" + SAMLConstants.PARTIAL_HELLO_WORLD_URI);
        testSettings.setSpDefaultApp(testAppServer.getServerHttpsString() + "/" + SAMLConstants.PARTIAL_HELLO_WORLD_URI);
        testSettings.updatePartnerInSettings("sp1", true);

        testAppServer.addIgnoredServerExceptions(SAMLMessageConstants.CWPKI0807W_KEYSTORE_NOT_FOUND, SAMLMessageConstants.CWPKI0809W_FAILURE_LOADING_KEYSTORE, SAMLMessageConstants.CWPKI0033E_KEYSTORE_DID_NOT_LOAD, SAMLMessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES);
        // Allow the warning on the ignored attributes of samlWebSso20 inboundPropagation true or false
        testSAMLServer.addIgnoredServerException(SAMLMessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES);

    }

    //  Part of me hates to have one line tests, but when everything they're doing is the same (when the only difference is the app
    //  (which causes the difference in behavior),
    //  I just can't justify writing the same code over and over again, so, here is the common code for the positive and negative
    // 	pkixTrustEngine tests...
    /**
     * Positive test - the server's trust store content doesn't matter
     *
     * @param appName
     *            - name of the app to invoke
     * @throws Exception
     */
    private void commonRunTest_GoodServerHasCert_GoodServerDoesNotHaveCert(String appName) throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = updateTestSettings(appName);

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, updatedTestSettings);

        genericSAML(_testName, webClient, updatedTestSettings, throughJAXRSGet, expectations);

    }

    /**
     * Positive test when the server trust has the IDP's cert, negative when it does not
     *
     * @param appName
     *            - name of the app to invoke
     * @throws Exception
     */
    private void commonRunTest_GoodServerHasCert_5049EServerDoesNotHaveCert(String appName) throws Exception {
        commonRunTest_GoodServerHasCert_5049EServerDoesNotHaveCert(serverHasIDPCert, appName);
    }

    /**
     * Negative test - the server's trust store content doesn't really matter
     *
     * @param appName
     *            - name of the app to invoke
     * @throws Exception
     */
    private void commonRunTest_5049EServerDoesNotHaveCert(String appName) throws Exception {
        commonRunTest_GoodServerHasCert_5049EServerDoesNotHaveCert(false, appName);
    }

    /**
     * common test method that sents up expectations based on wheter we expect a failure or not
     *
     * @param expectFailure
     *            - true/false - does setup result in an exception?
     * @param appName
     *            - name of the app to invoke
     * @throws Exception
     */
    private void commonRunTest_GoodServerHasCert_5049EServerDoesNotHaveCert(Boolean shouldSucceed, String appName) throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = updateTestSettings(appName);

        List<validationData> expectations = null;
        if (shouldSucceed) {
            expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, updatedTestSettings);
        } else {
            expectations = msgUtils.addrsSamlUnauthorizedExpectation(SAMLConstants.INVOKE_JAXRS_GET, null);

            expectations = helpers.addMessageExpectation(testAppServer, expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.APP_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not see the expected message saying the signature was not trusted or valid in messages.log.", SAMLMessageConstants.CWWKS5049E_SIGNATURE_NOT_TRUSTED_OR_VALID);
        }

        genericSAML(_testName, webClient, updatedTestSettings, throughJAXRSGet, expectations);

    }

    /**
     * Update the settings if needed - if we're using an app other than the default, we need to update
     * several fields in the settings. Instead of having each test method contain that login, put
     * it here in one place
     *
     * @param appName
     *            - the name of the app that we'll use - update test settings to use this app
     * @return - returns either the original settings if we're using the default app, or update the settings with the new app name
     * @throws Exception
     */
    private SAMLTestSettings updateTestSettings(String appName) throws Exception {
        SAMLTestSettings updatedTestSettings = null;
        if (appName == null) {
            updatedTestSettings = testSettings.copyTestSettings();
        } else {
            updatedTestSettings = commonUtils.changeTestApps(testAppServer, testSettings, appName);
        }
        return updatedTestSettings;

    }

    /**
     * The basic RS SAML tests use a configuration that has NO pkixTrustEngine defined - so we've covered that case there
     */

    /**
     * The rs saml config has a pkixTrustEngine which does not specify trustAnchor.
     * It will then get the trustStoreRef from the default SSL config.
     * Extended by PkixJaxrsWithCertInServerIDPInitiatedTests
     * The default trustStoreRef is serverStore which points to samlSslServerTrust.jks
     * This jks file has the self-signed certificates from the tfim IdP. So, the test passes
     * Extended by PkixJaxrsWithoutCertInServerIDPInitiatedTests
     * The default trustStoreRef is serverStore which points to sslServerTrust.jks
     * This jks file does NOT have the self-signed certificates from the tfim IdP. So, the test fails
     */
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.ws.security.SecurityPolicyException" })
    @Test
    public void RSSamlPkixTests_defaultTrustAnchorTest() throws Exception {

        commonRunTest_GoodServerHasCert_5049EServerDoesNotHaveCert(null);

    }

    /**
     * The rs saml config has a pkixTrustEngine which specifies the trustAnchor
     * as serverStore. It points to samlSslServerTrust.jks
     * The jks file has the self-signed certificates from the tfim IdP. So, the test passes
     */
    @Mode(TestMode.LITE)
    @Test
    //	@AllowedFFDC("java.lang.NoClassDefFoundError")
    public void RSSamlPkixTests_SpecifiedTrustAnchorTest() throws Exception {

        commonRunTest_GoodServerHasCert_GoodServerDoesNotHaveCert("serverTrust");

    }

    // BasicSAMLJaxRSTests_samlCertNotInRSSamlTrust_wantAssertionsSigned_true in BasicSAMLJaxRSTests covers the case
    // where the trustAnchor points to a jks file that does NOT have the certificate used in the saml assertion

    /**
     * The rs saml config has a pkixTrustEngine which specifies the trustAnchor
     * as serverSToreMissingSAMl. This trust store does NOT contain the cert for the IDP server.
     * So, the test fails
     */
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.ws.security.SecurityPolicyException" })
    @Mode(TestMode.LITE)
    @Test
    public void RSSamlPkixTests_badTrustAnchorTest() throws Exception {

        commonRunTest_5049EServerDoesNotHaveCert("missingServerTrust");

    }

    /**
     * The rs saml config has a pkixTrustEngine which specifies the trustAnchor
     * as missingJKSFil. This jks file for this trust configuration does not exist
     * So, the test fails
     */
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.ws.security.SecurityPolicyException", "java.security.KeyStoreException" })
    @Test
    public void RSSamlPkixTests_badTrustAnchorReference() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = commonUtils.changeTestApps(testAppServer, testSettings, "missingJKSFile");

        List<validationData> expectations = msgUtils.addrsSamlUnauthorizedExpectation(SAMLConstants.INVOKE_JAXRS_GET, null);

        expectations = helpers.addMessageExpectation(testAppServer, expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.APP_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not see the expected message saying the signature was not trusted or valid in messages.log.", SAMLMessageConstants.CWWKS5049E_SIGNATURE_NOT_TRUSTED_OR_VALID);
        expectations = helpers.addMessageExpectation(testAppServer, expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.APP_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not see the expected message saying there was an internal server error loading the jks file in messages.log.", SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR);

        genericSAML(_testName, webClient, updatedTestSettings, throughJAXRSGet, expectations);
    }

    /***************************************************************
     * No TrustAnchor tests
     *****************************************************************************/
    /**
     * The rs saml config has a pkixTrustEngine which specifies the x509Certificate.
     * The file refereced by this attribute contains the certificate used by the
     * IDP.
     * So, this test passes
     */
    @Test
    @Mode(TestMode.LITE)
    public void RSSamlPkixTests_noTrustAnchor_goodX509Certificate() throws Exception {

        commonRunTest_GoodServerHasCert_GoodServerDoesNotHaveCert("goodX509Certificate");

    }

    /**
     * The rs saml config has a pkixTrustEngine which specifies the x509Certificate.
     * The file refereced by this attribute contains an expired certificate.
     * Extended by PkixJaxrsWithCertInServerIDPInitiatedTests, the tests should pass as the default server truststore has the
     * IDP's certificate
     * Extended by PkixJaxrsWithoutCertInServerIDPInitiatedTests, the tests should fail as the default server truststore does
     * NOT have the IDP's certificate
     */
    @Mode(TestMode.LITE)
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.ws.security.SecurityPolicyException", "java.security.KeyStoreException" })
    @Test
    public void RSSamlPkixTests_noTrustAnchor_badX509Certificate() throws Exception {

        commonRunTest_GoodServerHasCert_5049EServerDoesNotHaveCert("badX509Certificate");

    }

    /**
     * The rs saml config has a pkixTrustEngine which specifies 2 x509Certificates and NO trustAnchor.
     * The first file referenced by this attribute contains an invalid certificate.
     * The second file referenced by this attribute contains the valid certificate.
     * The certificate is found
     * So, the test passes
     */
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.ws.security.SecurityPolicyException", "java.security.KeyStoreException" })
    @Test
    public void RSSamlPkixTests_noTrustAnchor_multipleX509Certificates_1bad1good() throws Exception {

        commonRunTest_GoodServerHasCert_GoodServerDoesNotHaveCert("multiX509Certificate_badGood");

    }

    /**
     * The rs saml config has a pkixTrustEngine which specifies 2 x509Certificates and NO trustAnchor.
     * The first file referenced by this attribute contains the valid certificate.
     * The second file referenced by this attribute contains an invalid certificate.
     * The certificate is found
     * So, the test passes
     */
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.ws.security.SecurityPolicyException", "java.security.KeyStoreException" })
    @Test
    public void RSSamlPkixTests_noTrustAnchor_multipleX509Certificates_1good1bad() throws Exception {

        commonRunTest_GoodServerHasCert_GoodServerDoesNotHaveCert("multiX509Certificate_goodBad");

    }

    /**
     * The rs saml config has a pkixTrustEngine which specifies 2 x509Certificates and NO trustAnchor.
     * The first file referenced by this attribute contains an invalid certificate.
     * The second file referenced by this attribute contains an invalid certificate.
     * Extended by PkixJaxrsWithCertInServerIDPInitiatedTests, the tests should pass as the default server truststore has the
     * IDP's certificate
     * Extended by PkixJaxrsWithoutCertInServerIDPInitiatedTests, the tests should fail as the default server truststore does
     * NOT have the IDP's certificate
     */
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.ws.security.SecurityPolicyException", "java.security.KeyStoreException" })
    @Test
    public void RSSamlPkixTests_noTrustAnchor_multipleX509Certificates_2bad() throws Exception {

        commonRunTest_GoodServerHasCert_5049EServerDoesNotHaveCert("multiX509Certificate_badBad");

    }

    /***************************************************************
     * Good TrustAnchor tests
     ***************************************************************************/
    /**
     * The rs saml config has a pkixTrustEngine which specifies a good x509Certificate and good trustAnchor.
     * The x509Certificate file referenced by this attribute contains a valid certificate.
     * The trustAnchor file referenced by this attribute contains a valid certificate.
     * The certificate is found
     * So, the test passes
     */
    @Test
    public void RSSamlPkixTests_goodTrustAnchor_goodX509Certificate() throws Exception {

        commonRunTest_GoodServerHasCert_GoodServerDoesNotHaveCert("goodTA_goodX509Certificate");

    }

    /**
     * The rs saml config has a pkixTrustEngine which specifies a good x509Certificate and good trustAnchor.
     * The x509Certificate file referenced by this attribute contains an invalid certificate.
     * The trustAnchor file referenced by this attribute contains the valid certificate.
     * The certificate is found
     * So, the test passes
     */
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.ws.security.SecurityPolicyException", "java.security.KeyStoreException" })
    @Test
    public void RSSamlPkixTests_goodTrustAnchor_badX509Certificate() throws Exception {

        commonRunTest_GoodServerHasCert_GoodServerDoesNotHaveCert("goodTA_badX509Certificate");

    }

    /**
     * The rs saml config has a pkixTrustEngine which specifies 2 x509Certificate and a good trustAnchor.
     * The first x509Certificate file referenced by this attribute contains an invalid certificate.
     * The second x509Certificate file referenced by this attribute contains the valid certificate.
     * The trustAnchor file referenced by this attribute contains a valid certificate.
     * The certificate is found
     * So, the test passes
     */
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.ws.security.SecurityPolicyException", "java.security.KeyStoreException" })
    @Test
    public void RSSamlPkixTests_goodTrustAnchor_multipleX509Certificates_1bad1good() throws Exception {

        commonRunTest_GoodServerHasCert_GoodServerDoesNotHaveCert("goodTA_multiX509Certificate_badGood");

    }

    /**
     * The rs saml config has a pkixTrustEngine which specifies 2 x509Certificate and a good trustAnchor.
     * The first x509Certificate file referenced by this attribute contains the valid certificate.
     * The second x509Certificate file referenced by this attribute contains an invalid certificate.
     * The trustAnchor file referenced by this attribute contains a valid certificate.
     * The certificate is found
     * So, the test passes
     */
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.ws.security.SecurityPolicyException", "java.security.KeyStoreException" })
    @Test
    public void RSSamlPkixTests_goodTrustAnchor_multipleX509Certificates_1good1bad() throws Exception {

        commonRunTest_GoodServerHasCert_GoodServerDoesNotHaveCert("goodTA_multiX509Certificate_goodBad");

    }

    /**
     * The rs saml config has a pkixTrustEngine which specifies 2 x509Certificate and a good trustAnchor.
     * The first x509Certificate file referenced by this attribute contains an invalid certificate.
     * The second x509Certificate file referenced by this attribute contains an invalid certificate.
     * The trustAnchor file referenced by this attribute contains a valid certificate.
     * The certificate is found
     * So, the test passes
     */
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.ws.security.SecurityPolicyException", "java.security.KeyStoreException" })
    @Test
    public void RSSamlPkixTests_goodTrustAnchor_multipleX509Certificates_2bad() throws Exception {

        commonRunTest_GoodServerHasCert_GoodServerDoesNotHaveCert("goodTA_multiX509Certificate_badBad");

    }

    /***************************************************************
     * Bad TrustAnchor tests
     ***************************************************************************/
    /**
     * The rs saml config has a pkixTrustEngine which specifies a good x509Certificate and bad trustAnchor.
     * The x509Certificate file referenced by this attribute contains a valid certificate.
     * The trustAnchor file referenced by this attribute does NOT contain a valid certificate.
     * The certificate is found
     * So, the test passes
     */
    @Test
    public void RSSamlPkixTests_badTrustAnchor_goodX509Certificate() throws Exception {

        commonRunTest_GoodServerHasCert_GoodServerDoesNotHaveCert("badTA_goodX509Certificate");

    }

    /**
     * The rs saml config has a pkixTrustEngine which specifies a bad x509Certificate and good trustAnchor.
     * The x509Certificate file referenced by this attribute contains an invalid certificate.
     * The trustAnchor file referenced by this attribute does NOT contain a valid certificate.
     * The certificate is NOT found
     * So, the test fails
     */
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.ws.security.SecurityPolicyException", "java.security.KeyStoreException" })
    @Test
    public void RSSamlPkixTests_badTrustAnchor_badX509Certificate() throws Exception {

        commonRunTest_5049EServerDoesNotHaveCert("badTA_badX509Certificate");

    }

    /**
     * The rs saml config has a pkixTrustEngine which specifies 2 x509Certificate and a bad trustAnchor.
     * The first x509Certificate file referenced by this attribute contains an invalid certificate.
     * The second x509Certificate file referenced by this attribute contains the valid certificate.
     * The trustAnchor file referenced by this attribute does NOT contain a valid certificate.
     * The certificate is found
     * So, the test passes
     */
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.ws.security.SecurityPolicyException", "java.security.KeyStoreException" })
    @Test
    public void RSSamlPkixTests_badTrustAnchor_multipleX509Certificates_1bad1good() throws Exception {

        commonRunTest_GoodServerHasCert_GoodServerDoesNotHaveCert("badTA_multiX509Certificate_badGood");

    }

    /**
     * The rs saml config has a pkixTrustEngine which specifies 2 x509Certificate and a bad trustAnchor.
     * The first x509Certificate file referenced by this attribute contains the valid certificate.
     * The second x509Certificate file referenced by this attribute contains an invalid certificate.
     * The trustAnchor file referenced by this attribute does NOT contain a valid certificate.
     * The certificate is found
     * So, the test passes
     */
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.ws.security.SecurityPolicyException", "java.security.KeyStoreException" })
    @Test
    public void RSSamlPkixTests_badTrustAnchor_multipleX509Certificates_1good1bad() throws Exception {

        commonRunTest_GoodServerHasCert_GoodServerDoesNotHaveCert("badTA_multiX509Certificate_goodBad");

    }

    /**
     * The rs saml config has a pkixTrustEngine which specifies 2 x509Certificate and a bad trustAnchor.
     * The first x509Certificate file referenced by this attribute contains an invalid certificate.
     * The second x509Certificate file referenced by this attribute contains an invalid certificate.
     * The trustAnchor file referenced by this attribute does NOT contain a valid certificate.
     * The certificate is NOT found
     * So, the test fails
     */
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.ws.security.SecurityPolicyException", "java.security.KeyStoreException" })
    @Test
    public void RSSamlPkixTests_badTrustAnchor_multipleX509Certificates_2bad() throws Exception {

        commonRunTest_5049EServerDoesNotHaveCert("badTA_multiX509Certificate_badBad");

    }

    /**
     * The rs saml config has multiple pkixTrustEngine specified for an rs_saml config.
     * The config tooling won't flag an error, our runtime code only has access to the
     * one value that the config tooling loads. So, for now add a test that specifies 2
     * pkixTrustEngines and makes sure that we use the second entry.
     */
    @Test
    public void RSSamlPkixTests_multiplePkixTrustEngines() throws Exception {

        commonRunTest_GoodServerHasCert_GoodServerDoesNotHaveCert("multiPKIX");

    }
}
