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
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;
import com.ibm.ws.security.saml20.fat.commonTest.utils.RSCommonUtils;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * These tests verify the behavior of the pkixTrustEngine trustedIssuers attribute
 * These tests are run in a 1 server and in a 2 server environment.
 * A variety of valid and invalid trustedIssuers are specified
 *
 * @author chrisc
 *
 */
@LibertyServerWrapper
@Mode(TestMode.FULL)
public class RSSamlTrustedIssuersTests extends SAMLCommonTest {

    private static final Class<?> thisClass = RSSamlTrustedIssuersTests.class;
    protected static String SPServerName = "com.ibm.ws.security.saml.sso-2.0_fat.jaxrs.sp";
    protected static String APPServerName = "com.ibm.ws.security.saml.sso-2.0_fat.jaxrs.rs";
    protected static String MergedServerName = "com.ibm.ws.security.saml.sso-2.0_fat.jaxrs.merged_sp_rs";
    protected static RSCommonUtils commonUtils = new RSCommonUtils();

    // Part of me hates to have one line tests, but when everything they're
    // doing is the same (when the only difference is the app
    // which causes the difference in behavior),
    // I just can't justify writing the same code over and over again, so, here
    // is the common code for the positive and negative
    // pkixTrustEngine tests...
    /**
     * Positive test - common test method that sets up expectations and runs a
     * valid test where the issuer is valid
     *
     * @param appName
     *            - name of the app to invoke
     * @throws Exception
     */
    private void commonRunTest_successful(String appName) throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = updateTestSettings(appName);

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, updatedTestSettings);

        genericSAML(_testName, webClient, updatedTestSettings, throughJAXRSGet, expectations);

    }

    /**
     * Negative test - common test method that sets up expectations and runs a
     * test that fails due to having an invalid issuer
     *
     * @param appName
     *            - name of the app to invoke
     * @throws Exception
     */
    private void commonRunTest_missingIssuer(String appName) throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = updateTestSettings(appName);

        List<validationData> expectations = null;

        expectations = msgUtils.addrsSamlUnauthorizedExpectation(SAMLConstants.INVOKE_JAXRS_GET, null);
        expectations = helpers.addMessageExpectation(testAppServer, expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.APP_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not see the expected message saying that the issuer is not trusted in messages.log.", SAMLMessageConstants.CWWKS5045E_INVALID_ISSUER);

        genericSAML(_testName, webClient, updatedTestSettings, throughJAXRSGet, expectations);

    }

    /**
     * Update the settings if needed - if we're using an app other than the
     * default, we need to update several fields in the settings. Instead of
     * having each test method contain that logic, put it here in one place
     *
     * @param appName
     *            - the name of the app that we'll use - update test settings to
     *            use this app
     * @return - returns either the original settings if we're using the default
     *         app, or update the settings with the new app name
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
     * The rs saml config has a pkixTrustEngine which specifies trustedIssuers
     * containing valid isser data (list of valid issuers). The runtime will be
     * able to validate the issuer in the response with the value in the
     * trustedIssuers attribute, so the test will be successful.
     */
    @Mode(TestMode.LITE)
    @Test
    //	@AllowedFFDC("java.lang.NoClassDefFoundError")
    public void RSSamlTrustedIssuersTests_IssuerInTrustedIssuersTest() throws Exception {

        commonRunTest_successful("goodIssuers");

    }

    /**
     * The rs saml config has a pkixTrustEngine which specifies trustedIssuers
     * containing invalid isser data. The runtime will be not able to validate
     * the issuer in the response with the value in the trustedIssuers
     * attribute, so the test will fail.
     */
    @ExpectedFFDC({ "com.ibm.ws.security.saml.error.SamlException" })
    @Mode(TestMode.LITE)
    @Test
    public void RSSamlTrustedIssuersTests_IssuerNOTInTrustedIssuersTest() throws Exception {

        commonRunTest_missingIssuer("badIssuers");

    }

    /**
     * The rs saml config has a pkixTrustEngine which specifies trustedIssuers
     * containing an empty value. This value will cause the default of all
     * issuers to be trusted. The runtime will be able to validate the issuer in
     * the response with the value in the trustedIssuers attribute, so the test
     * will be successful.
     */
    @Test
    public void RSSamlTrustedIssuersTests_EmptyTrustedIssuersTest() throws Exception {

        commonRunTest_successful("emptyIssuers");

    }

    /**
     * The rs saml config has a pkixTrustEngine which specifies trustedIssuers
     * containing ALL_ISSUERS. The runtime will be able to validate the issuer
     * in the response with the value in the trustedIssuers attribute, so the
     * test will be successful.
     */
    @Mode(TestMode.LITE)
    @Test
    public void RSSamlTrustedIssuersTests_AllIssuersTrustedIssuersTest() throws Exception {

        commonRunTest_successful("allIssuers");

    }

    /**
     * The rs saml config has a pkixTrustEngine which specifies trustedIssuers
     * containing invalid issuer values and then ALL_ISSUERS. The runtime will
     * be able to validate the issuer in the response with the value in the
     * trustedIssuers attribute, so the test will be successful.
     */
    @Test
    public void RSSamlTrustedIssuersTests_MixedBadFirstThenAllIssuersTest() throws Exception {

        commonRunTest_successful("mixedBadFirstThenAllIssuers");

    }

    /**
     * The rs saml config has a pkixTrustEngine which specifies trustedIssuers
     * containing ALL_ISSUERS and then invalid issuer values. The runtime will
     * be able to validate the issuer in the response with the value in the
     * trustedIssuers attribute, so the test will be successful.
     */
    @Test
    public void RSSamlTrustedIssuersTests_MixedAllIssuersFirstThenBadTest() throws Exception {

        commonRunTest_successful("mixedAllIssuersFirstThenBad");

    }

    /**
     * The rs saml config has a pkixTrustEngine which specifies trustedIssuers
     * containing valid issuer values and then ALL_ISSUERS. The runtime will be
     * able to validate the issuer in the response with the value in the
     * trustedIssuers attribute, so the test will be successful.
     */
    @Test
    public void RSSamlTrustedIssuersTests_MixedGoodFirstThenAllIssuersTest() throws Exception {

        commonRunTest_successful("mixedGoodFirstThenAllIssuers");

    }

    /**
     * The rs saml config has a pkixTrustEngine which specifies trustedIssuers
     * containing ALL_ISSUERS and then valid issuer values. The runtime will be
     * able to validate the issuer in the response with the value in the
     * trustedIssuers attribute, so the test will be successful.
     */
    @Test
    public void RSSamlTrustedIssuersTests_MixedAllIssuersFirstThenGoodTest() throws Exception {

        commonRunTest_successful("mixedAllIssuersFirstThenGood");

    }

    /**
     * The rs saml config has a pkixTrustEngine which specifies trustedIssuers
     * containing ALL_ISSUERSJUNK. The runtime will not be able to validate the
     * issuer in the response with the value in the trustedIssuers attribute, so
     * the test will fail.
     */
    @ExpectedFFDC({ "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void RSSamlTrustedIssuersTests_AllIssuersPlusJunkTest() throws Exception {

        commonRunTest_missingIssuer("allIssuersPlusJunk");

    }

    /**
     * The rs saml config has a pkixTrustEngine which specifies trustedIssuers
     * containing JUNKALL_ISSUERS. The runtime will not be able to validate the
     * issuer in the response with the value in the trustedIssuers attribute, so
     * the test will be successful.
     */
    @ExpectedFFDC({ "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void RSSamlTrustedIssuersTests_JunkPlusAllIssuersTest() throws Exception {

        commonRunTest_missingIssuer("junkPlusAllIssuers");

    }

    /**
     * The rs saml config has a pkixTrustEngine which specifies trustedIssuers
     * containing "ALL_ISSUERS FOO". The runtime will not be able to validate
     * the issuer in the response with the value in the trustedIssuers
     * attribute, so the test will be successful.
     */
    @ExpectedFFDC({ "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void RSSamlTrustedIssuersTests_AllIssuersSpaceFooTest() throws Exception {

        commonRunTest_missingIssuer("allIssuersSpaceFoo");

    }

    /**
     * The rs saml config has a pkixTrustEngine which specifies trustedIssuers
     * containing "FOO ALL_ISSUERS". The runtime will not be able to validate
     * the issuer in the response with the value in the trustedIssuers
     * attribute, so the test will be successful.
     */
    @ExpectedFFDC({ "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void RSSamlTrustedIssuersTests_FooSpaceAllIssuersTest() throws Exception {

        commonRunTest_missingIssuer("fooSpaceAllIssuers");

    }

    /**
     * The rs saml config has a pkixTrustEngine which specifies trustedIssuers
     * containing ALL_ISSUERS and then valid issuer values. They will be
     * separated by comma space. The runtime will be able to validate the issuer
     * in the response with the value in the trustedIssuers attribute, so the
     * test will be successful.
     */
    @Test
    public void RSSamlTrustedIssuersTests_CommaSpaceTest() throws Exception {

        commonRunTest_successful("commaSpace");

    }

    /**
     * The rs saml config has a pkixTrustEngine which specifies trustedIssuers
     * containing "ALL_ISSUERS <valid issuer>,ALL_ISSUERS <valid_issuer>, ...".
     * The runtime will not be able to validate the issuer in the response with
     * the value in the trustedIssuers attribute, so the test will be
     * successful.
     */
    @ExpectedFFDC({ "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void RSSamlTrustedIssuersTests_SpaceInIssuersTest() throws Exception {

        commonRunTest_missingIssuer("spaceInIssuers");

    }
}
