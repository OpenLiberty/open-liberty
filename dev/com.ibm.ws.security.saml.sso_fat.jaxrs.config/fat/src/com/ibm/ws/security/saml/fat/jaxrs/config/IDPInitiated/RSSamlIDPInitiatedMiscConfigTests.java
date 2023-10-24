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
package com.ibm.ws.security.saml.fat.jaxrs.config.IDPInitiated;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.fat.common.Utils;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.saml.fat.jaxrs.config.utils.RSSamlConfigSettings;
import com.ibm.ws.security.saml.fat.jaxrs.config.utils.RSSamlProviderSettings;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings.RSSettings;
import com.ibm.ws.security.saml20.fat.commonTest.config.settings.AuthFilterSettings;
import com.ibm.ws.security.saml20.fat.commonTest.config.settings.RequestUrlSettings;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * In general, these tests perform a simple IdP initiated SAML Web SSO, using
 * httpunit to simulate browser requests. In this scenario, a Web client
 * accesses a static Web page on IdP and obtains a a SAML HTTP-POST link to an
 * application installed on a WebSphere SP. When the Web client invokes the SP
 * application, it is redirected to a TFIM IdP which issues a login challenge to
 * the Web client. The Web Client fills in the login form and after a successful
 * login, receives a SAML 2.0 token from the TFIM IdP. The client invokes the SP
 * application by sending the SAML 2.0 token in the HTTP POST request.
 */
@LibertyServerWrapper
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class RSSamlIDPInitiatedMiscConfigTests extends RSSamlIDPInitiatedConfigCommonTests {

    /*****************************************
     * TESTS
     **************************************/

    @Mode(TestMode.LITE)
    @Test
    // @AllowedFFDC("java.lang.NoClassDefFoundError")
    public void RSSamlIDPInitiatedConfigTests_mainFlow() throws Exception {

        testAppServer.reconfigServer(buildSPServerName("server_1.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = getAndSaveWebClient();

        List<validationData> expectations = vData.addSuccessStatusCodes();

        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive the correct status code", null, SAMLConstants.HELLO_WORLD_STRING);

        genericSAML(_testName, webClient, testSettings, throughJAXRSGet, expectations);
    }

    /*
     * This test verifies the realm, user and group that are in the subject. The
     * values are set based on several configuration options. These tests will
     * modify the configuration and then check for the appropriate values in the
     * subject.
     */

    /**************************************
     * headerName
     **************************************/

    /**
     * Test purpose: - headerName: Empty string Expected results: - The default
     * header name "SAML" should be used. - The SAML token should be
     * successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_headerName_empty() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        RSSamlProviderSettings updatedProviderSettings = updatedRsSamlSettings.getDefaultRSSamlProviderSettings();
        String headerName = "";
        updatedProviderSettings.setHeaderName(headerName);

        // The header name ultimately used should be "SAML"
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        RSSettings rsSettings = updatedTestSettings.getRSSettings();
        rsSettings.setHeaderName(SAMLConstants.SAML_DEFAULT_AUTHORIZATION_HEADER_NAME);

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, updatedTestSettings);

        generalConfigTest(updatedRsSamlSettings, expectations, updatedTestSettings);
    }

    /**
     * Test purpose: - headerName: "SomeValue" - Access the protected resource
     * using the configured header name. - Access the protected resource again
     * but with the test settings set to use a different header name. Expected
     * results: - The SAML token should be successfully processed by JAX-RS for
     * the appropriate header name. - The second request should fail with a 401
     * because the header names will not match.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_headerName_nonEmpty() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        RSSamlProviderSettings updatedProviderSettings = updatedRsSamlSettings.getDefaultRSSamlProviderSettings();
        String headerName = "SomeValue";
        updatedProviderSettings.setHeaderName(headerName);

        // Update the header name in the test settings
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        RSSettings rsSettings = updatedTestSettings.getRSSettings();
        rsSettings.setHeaderName(headerName);

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, updatedTestSettings);

        generalConfigTest(updatedRsSamlSettings, expectations, updatedTestSettings);

        // Update the header name in the test settings to something other than
        // the configured header name
        rsSettings.setHeaderName(headerName + "-notFound");

        // Expect a 401 since the header name used by the application will not
        // match the configured header name
        expectations = get401ExpectationsForJaxrsGet("Did not get the expected message saying the header couldn't be found.",
                SAMLMessageConstants.CWWKS5013E_MISSING_SAML_ASSERTION_ERROR + "\\[" + headerName);

        generalConfigTest(updatedRsSamlSettings, expectations, updatedTestSettings);
    }

    /**
     * Test purpose: - headerName: "Some value with spaces." Expected results: -
     * depending on which syntax is used to pass the header/value, the runtime
     * may/may not be able to deal with the leading/trailing (mainly trailing)
     * spaces. We will document a restriction regardign spaces in the header
     * name. - We will just test with embedded spaces
     *
     * @throws Exception
     */
    /**
     * Update test as runtime was updated to honor
     * RFC 9110 - disallow spaces
     *
     * @throws Exception
     */
    public void common_headerName_withSpaces(String format, String headerName, int status) throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        RSSamlProviderSettings updatedProviderSettings = updatedRsSamlSettings.getDefaultRSSamlProviderSettings();
        updatedProviderSettings.setHeaderName(headerName);

        // Update the header name in the test settings
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        RSSettings rsSettings = updatedTestSettings.getRSSettings();
        rsSettings.setHeaderName(headerName);
        rsSettings.setHeaderFormat(format);

        List<validationData> expectations = null;
        if (status == SAMLConstants.BAD_REQUEST_STATUS) {
            expectations = vData.addSuccessStatusCodes(null, SAMLConstants.INVOKE_JAXRS_GET);
            expectations = vData.addResponseStatusExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, status);
            expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.RESPONSE_MESSAGE, SAMLConstants.STRING_CONTAINS, "Did not get a failure because the header name contained spaces.", null, SAMLConstants.BAD_REQUEST);
        } else {
            commonUtils.getGoodExpectationsForJaxrsGet(flowType, updatedTestSettings);
        }

        generalConfigTest(updatedRsSamlSettings, expectations, updatedTestSettings);
    }

    /**
     * Pass header as Authorization=<header>=<value>
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_headerName_nonEmptyWithSpaces_formatHeaderEqualsValue() throws Exception {

        common_headerName_withSpaces(SAMLConstants.HEADER_FORMAT_AUTHZ_NAME_EQUALS_VALUE, "Some value with  spaces.", SAMLConstants.OK_STATUS);
    }

    /**
     * Pass header as Authorization=<header>="<value>"
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_headerName_nonEmptyWithSpaces_formatHeaderEqualsQuotedValue() throws Exception {

        common_headerName_withSpaces(SAMLConstants.HEADER_FORMAT_AUTHZ_NAME_EQUALS_QUOTED_VALUE, "Some value with  spaces.", SAMLConstants.OK_STATUS);
    }

    /**
     * Pass header as Authorization=<header> <value>
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_headerName_nonEmptyWithSpaces_formatHeaderSpaceValue() throws Exception {

        common_headerName_withSpaces(SAMLConstants.HEADER_FORMAT_AUTHZ_NAME_SPACE_VALUE, "Some value with  spaces.", SAMLConstants.OK_STATUS);
    }

    /**
     * Pass header as <header>=<value>
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "java.lang.IllegalArgumentException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_headerName_nonEmptyWithSpaces_formatNoAuthHeaderEqualsValue() throws Exception {

        common_headerName_withSpaces(SAMLConstants.HEADER_FORMAT_NAME_EQUALS_VALUE, "Some value with  spaces.", SAMLConstants.BAD_REQUEST_STATUS);
    }

    /**************************************
     * disableLtpaCookie
     **************************************/

    /**
     * Test purpose: - disableLtpaCookie: false Expected results: - An LTPA
     * cookie should be set in the successful RS invocation response. - The SAML
     * token should be successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_disableLtpaCookie_false() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        RSSamlProviderSettings updatedProviderSettings = updatedRsSamlSettings.getDefaultRSSamlProviderSettings();
        updatedProviderSettings.setDisableLtpaCookie("false");

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, testSettings);

        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.RESPONSE_HEADER, SAMLConstants.STRING_CONTAINS,
                "Did not receive the expected LTPA cookie", null, SAMLConstants.LTPA_TOKEN_NAME);

        generalConfigTest(updatedRsSamlSettings, expectations, testSettings);
    }

    /**
     * Test purpose: - disableLtpaCookie: true Expected results: - An LTPA
     * cookie should NOT be set in the successful RS invocation response. - The
     * SAML token should be successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_disableLtpaCookie_true() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        RSSamlProviderSettings updatedProviderSettings = updatedRsSamlSettings.getDefaultRSSamlProviderSettings();
        updatedProviderSettings.setDisableLtpaCookie("true");

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, testSettings);

        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.RESPONSE_HEADER, SAMLConstants.STRING_DOES_NOT_CONTAIN,
                "Received an unexpected LTPA cookie", null, SAMLConstants.LTPA_TOKEN_NAME);

        generalConfigTest(updatedRsSamlSettings, expectations, testSettings);
    }

    /**************************************
     * clockSkew
     **************************************/

    /**
     * Test purpose: - clockSkew: 0s - Add 10 minutes to the NotBefore attribute
     * in the SAML token Expected results: - 401 when invoking JAX-RS. -
     * CWWKS5057E message in the app server log saying the NotBefore attribute
     * was out of range.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_clockSkew_0s_NotBefore_outOfRange() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        RSSamlProviderSettings updatedProviderSettings = updatedRsSamlSettings.getDefaultRSSamlProviderSettings();
        updatedProviderSettings.setWantAssertionsSigned("false");
        updatedProviderSettings.setClockSkew("0s");

        // Add time to the NotBefore attribute
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRemoveTagInResponse("ds:Signature");
        updatedTestSettings.setSamlTokenUpdateTimeVars(SAMLConstants.SAML_NOT_BEFORE, SAMLConstants.ADD_TIME,
                SAMLTestSettings.setTimeArray(0, 0, 10, 0), SAMLConstants.DO_NOT_USE_CURRENT_TIME);

        List<validationData> expectations = get401ExpectationsForJaxrsGet("Did not get expected CWWKS5057E message for an out of range NotBefore attribute.",
                SAMLMessageConstants.CWWKS5057E_NOT_BEFORE_OUT_OF_RANGE);

        generalConfigTest(updatedRsSamlSettings, expectations, updatedTestSettings);
    }

    /**
     * Test purpose: - clockSkew: 0s - Subtract 10 minutes from the NotOnOrAfter
     * attribute in the SAML token Expected results: - 401 when invoking JAX-RS.
     * - CWWKS5053E message in the app server log saying the NotOnOrAfter
     * attribute was out of range.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_clockSkew_0s_NotOnOrAfter_outOfRange() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        RSSamlProviderSettings updatedProviderSettings = updatedRsSamlSettings.getDefaultRSSamlProviderSettings();
        updatedProviderSettings.setWantAssertionsSigned("false");
        updatedProviderSettings.setClockSkew("0s");

        // Subtract time from the NotOnOrAfter attribute
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRemoveTagInResponse("ds:Signature");
        updatedTestSettings.setSamlTokenUpdateTimeVars(SAMLConstants.SAML_NOT_ON_OR_AFTER, SAMLConstants.SUBTRACT_TIME,
                SAMLTestSettings.setTimeArray(0, 0, 10, 0), SAMLConstants.DO_NOT_USE_CURRENT_TIME);

        List<validationData> expectations = get401ExpectationsForJaxrsGet("Did not get expected CWWKS5053E message for an out of range NotOnOrAfter attribute.",
                SAMLMessageConstants.CWWKS5053E_NOT_ON_OR_AFTER_OUT_OF_RANGE);

        generalConfigTest(updatedRsSamlSettings, expectations, updatedTestSettings);
    }

    /**
     * Test purpose: - clockSkew: 0s - Subtract 1 hour and 5 minutes from the
     * SessionNotOnOrAfter attribute in the SAML token Expected results: - 401
     * when invoking JAX-RS. - CWWKS5062E message in the app server log saying
     * the SessionNotOnOrAfter attribute was out of range.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_clockSkew_0s_SessionNotOnOrAfter_outOfRange() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        RSSamlProviderSettings updatedProviderSettings = updatedRsSamlSettings.getDefaultRSSamlProviderSettings();
        updatedProviderSettings.setWantAssertionsSigned("false");
        updatedProviderSettings.setClockSkew("0s");

        // Subtract time from the SessionNotOnOrAfter attribute
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRemoveTagInResponse("ds:Signature");
        updatedTestSettings.setSamlTokenUpdateTimeVars(SAMLConstants.SAML_SESSION_NOT_ON_OR_AFTER, SAMLConstants.SUBTRACT_TIME,
                SAMLTestSettings.setTimeArray(0, 1, 5, 0), SAMLConstants.DO_NOT_USE_CURRENT_TIME);

        List<validationData> expectations = get401ExpectationsForJaxrsGet("Did not get expected CWWKS5062E message for an out of range SessionNotOnOrAfter attribute.",
                SAMLMessageConstants.CWWKS5062E_SESSION_NOT_ON_OR_AFTER_OUT_OF_RANGE);

        generalConfigTest(updatedRsSamlSettings, expectations, updatedTestSettings);
    }

    /**
     * Test purpose: - clockSkew: 30m - Add 10 minutes to the NotBefore
     * attribute in the SAML token Expected results: - NotBefore attribute still
     * falls within allowable clock skew. - The SAML token should be
     * successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_clockSkew_30m_modifyNotBefore() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        RSSamlProviderSettings updatedProviderSettings = updatedRsSamlSettings.getDefaultRSSamlProviderSettings();
        updatedProviderSettings.setWantAssertionsSigned("false");
        updatedProviderSettings.setClockSkew("30m");

        // Add time to the NotBefore attribute
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRemoveTagInResponse("ds:Signature");
        updatedTestSettings.setSamlTokenUpdateTimeVars(SAMLConstants.SAML_NOT_BEFORE, SAMLConstants.ADD_TIME,
                SAMLTestSettings.setTimeArray(0, 0, 10, 0), SAMLConstants.DO_NOT_USE_CURRENT_TIME);

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, updatedTestSettings);

        generalConfigTest(updatedRsSamlSettings, expectations, updatedTestSettings);
    }

    /**
     * Test purpose: - clockSkew: 30m - Subtract 10 minutes from the
     * NotOnOrAfter attribute in the SAML token Expected results: - NotOnOrAfter
     * attribute still falls within allowable clock skew. - The SAML token
     * should be successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_clockSkew_30m_modifyNotOnOrAfter() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        RSSamlProviderSettings updatedProviderSettings = updatedRsSamlSettings.getDefaultRSSamlProviderSettings();
        updatedProviderSettings.setWantAssertionsSigned("false");
        updatedProviderSettings.setClockSkew("30m");

        // Subtract time from the NotOnOrAfter attribute
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRemoveTagInResponse("ds:Signature");
        updatedTestSettings.setSamlTokenUpdateTimeVars(SAMLConstants.SAML_NOT_ON_OR_AFTER, SAMLConstants.SUBTRACT_TIME,
                SAMLTestSettings.setTimeArray(0, 0, 10, 0), SAMLConstants.DO_NOT_USE_CURRENT_TIME);

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, updatedTestSettings);

        generalConfigTest(updatedRsSamlSettings, expectations, updatedTestSettings);
    }

    /**
     * Test purpose: - clockSkew: 30m - Subtract 1 hour and 10 minutes from the
     * SessionNotOnOrAfter attribute in the SAML token - Default session length
     * is 1 hour, so this should ensure that the SessionNotOnOrAfter attribute
     * is in the past Expected results: - SessionNotOnOrAfter attribute still
     * falls within allowable clock skew. - The SAML token should be
     * successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_clockSkew_30m_modifySessionNotOnOrAfter() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        RSSamlProviderSettings updatedProviderSettings = updatedRsSamlSettings.getDefaultRSSamlProviderSettings();
        updatedProviderSettings.setWantAssertionsSigned("false");
        updatedProviderSettings.setClockSkew("30m");

        // Subtract time from the SessionNotOnOrAfter attribute
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRemoveTagInResponse("ds:Signature");
        updatedTestSettings.setSamlTokenUpdateTimeVars(SAMLConstants.SAML_SESSION_NOT_ON_OR_AFTER, SAMLConstants.SUBTRACT_TIME,
                SAMLTestSettings.setTimeArray(0, 1, 10, 0), SAMLConstants.DO_NOT_USE_CURRENT_TIME);

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, updatedTestSettings);

        generalConfigTest(updatedRsSamlSettings, expectations, updatedTestSettings);
    }

    /**************************************
     * audiences
     **************************************/

    /**
     * Test purpose: - audiences: Not specified (default value: ANY) Expected
     * results: - The SAML token should be successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_audiences_missing() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        RSSamlProviderSettings updatedProviderSettings = updatedRsSamlSettings.getDefaultRSSamlProviderSettings();
        updatedProviderSettings.setAudiences(null);

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, testSettings);

        generalConfigTest(updatedRsSamlSettings, expectations, testSettings);
    }

    /**
     * Test purpose: - audiences: Empty string Expected results: - Should behave
     * the same as if the audiences attribute was not specified. - The SAML
     * token should be successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_audiences_empty() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        RSSamlProviderSettings updatedProviderSettings = updatedRsSamlSettings.getDefaultRSSamlProviderSettings();
        updatedProviderSettings.setAudiences("");

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, testSettings);

        generalConfigTest(updatedRsSamlSettings, expectations, testSettings);
    }

    /**
     * Test purpose: - audiences: "other" Expected results: - 401 when invoking
     * JAX-RS. - CWWKS5060E message in the app server log saying the audience
     * value in the SAML assertion was not valid.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_audiences_other() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        RSSamlProviderSettings updatedProviderSettings = updatedRsSamlSettings.getDefaultRSSamlProviderSettings();
        updatedProviderSettings.setAudiences("other");

        List<validationData> expectations = get401ExpectationsForJaxrsGet("Did not find the expected message saying the audience value in the SAML assertion was not valid.",
                SAMLMessageConstants.CWWKS5060E_AUDIENCE_NOT_VALID);

        generalConfigTest(updatedRsSamlSettings, expectations, testSettings);
    }

    /**
     * Test purpose: - audiences: List of audiences, none of which are "ANY" or
     * the audience listed in the SAML assertion Expected results: - 401 when
     * invoking JAX-RS. - CWWKS5060E message in the app server log saying the
     * audience value in the SAML assertion was not valid.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_audiences_multipleAllInvalid() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        RSSamlProviderSettings updatedProviderSettings = updatedRsSamlSettings.getDefaultRSSamlProviderSettings();

        // Get the selected IDP to make sure that a substring of the valid
        // audience will not suffice
        String selectedIdp = testSettings.getSelectedIDPServerName();
        updatedProviderSettings.setAudiences("one, two, " + selectedIdp);

        List<validationData> expectations = get401ExpectationsForJaxrsGet("Did not find the expected message saying the audience value in the SAML assertion was not valid.",
                SAMLMessageConstants.CWWKS5060E_AUDIENCE_NOT_VALID);

        generalConfigTest(updatedRsSamlSettings, expectations, testSettings);
    }

    /**
     * Test purpose: - audiences: List of audiences, one of which is "ANY"
     * Expected results: - The SAML token should be successfully processed by
     * JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_audiences_multipleIncludingAny() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        RSSamlProviderSettings updatedProviderSettings = updatedRsSamlSettings.getDefaultRSSamlProviderSettings();
        updatedProviderSettings.setAudiences("one, two, ANY");

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, testSettings);

        generalConfigTest(updatedRsSamlSettings, expectations, testSettings);
    }

    /**************************************
     * realmName
     **************************************/

    /**
     * Test purpose: - realmName: Empty string Expected results: - Should behave
     * the same as if the realmName attribute was not specified. - The SAML
     * token should be successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_realm_empty() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        RSSamlProviderSettings updatedProviderSettings = updatedRsSamlSettings.getDefaultRSSamlProviderSettings();
        updatedProviderSettings.setRealmName("");

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, testSettings);

        generalConfigTest(updatedRsSamlSettings, expectations, testSettings);
    }

    /**
     * Test purpose: - realmName: "NewRealm" Expected results: - The SAML token
     * should be successfully processed by JAX-RS. - Credentials in the
     * resulting subject should reflect the specified realm.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_realm_NewRealm() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        RSSamlProviderSettings updatedProviderSettings = updatedRsSamlSettings.getDefaultRSSamlProviderSettings();
        updatedProviderSettings.setRealmName("NewRealm");

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, testSettings, DO_NOT_CHECK_REALM);

        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS,
                "Did not find the expected realm within the user subject's public credentials.", null, "realmName=NewRealm");
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS,
                "Did not find the expected realm within the private credentials.", null, "com.ibm.wsspi.security.cred.realm=NewRealm");

        generalConfigTest(updatedRsSamlSettings, expectations, testSettings);
    }

    /**
     * Test purpose: - realmName: "NewRealm" - mapToUserRegistry: User Expected
     * results: - The SAML token should be successfully processed by JAX-RS. -
     * Credentials in the resulting subject should reflect the realm of the
     * configured registry.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_realm_NewRealm_mapToUserRegistry_User() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        updatedRsSamlSettings.setRegistryFiles("${server.config.dir}/imports/BasicRegistry.xml");

        RSSamlProviderSettings updatedProviderSettings = updatedRsSamlSettings.getDefaultRSSamlProviderSettings();
        updatedProviderSettings.setRealmName("NewRealm");
        updatedProviderSettings.setMapToUserRegistry("User");

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, testSettings, DO_NOT_CHECK_REALM);

        // When mapToUserRegistry="User", the realm should be set to that of the
        // registry
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS,
                "Did not find the expected realm within the user subject's public credentials.", null, "realmName=BasicRealm");

        generalConfigTest(updatedRsSamlSettings, expectations, testSettings);
    }

    /**************************************
     * authFilterRef
     **************************************/

    /**
     * Test purpose: - authFilterRef: Empty string Expected results: - Should
     * behave the same as if the authFilterRef attribute was not specified. -
     * The SAML token should be successfully processed by JAX-RS. - CWWKG0033W
     * message should be in the server logs saying the ref value for
     * authFilterRef couldn't be found in the config.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_authFilterRef_empty() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        RSSamlProviderSettings updatedProviderSettings = updatedRsSamlSettings.getDefaultRSSamlProviderSettings();
        updatedProviderSettings.setAuthFilterRef("");

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, testSettings);

        expectations = addMessageExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.APP_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS,
                "Did not find expected warning message in the logs for empty authFilterRef.", SAMLMessageConstants.CWWKG0033W_REF_VALUE_NOT_FOUND_IN_CONFIG);

        generalConfigTest(updatedRsSamlSettings, expectations, testSettings);
    }

    /**
     * Test purpose: - authFilterRef: Refers to default auth filter Expected
     * results: - The SAML token should be successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_authFilterRef_defaultAuthFilter() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        setUpDefaultAuthFilterSettings(updatedRsSamlSettings);

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, testSettings);

        generalConfigTest(updatedRsSamlSettings, expectations, testSettings);
    }

    /**
     * Test purpose: - authFilterRef: Refers to non-existent auth filter
     * Expected results: - Should behave the same as if the authFilterRef
     * attribute was not specified. - The SAML token should be successfully
     * processed by JAX-RS. - CWWKG0033W message should be in the server logs
     * saying the ref value for authFilterRef couldn't be found in the config.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_authFilterRef_nonExistentFilter() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        RSSamlProviderSettings rsSamlProvider = updatedRsSamlSettings.getDefaultRSSamlProviderSettings();
        rsSamlProvider.setAuthFilterRef("nonExistent");

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, testSettings);

        expectations = addMessageExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.APP_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS,
                "Did not find expected warning message in the logs for non-existent authFilterRef.", SAMLMessageConstants.CWWKG0033W_REF_VALUE_NOT_FOUND_IN_CONFIG);

        generalConfigTest(updatedRsSamlSettings, expectations, testSettings);
    }

    /**
     * Test purpose: - authFilterRef: Refers to non-existent auth filter - An
     * auth filter is configured to protect the JAX-RS resource Expected
     * results: - Should behave the same as if the authFilterRef attribute was
     * not specified. - The SAML token should be successfully processed by
     * JAX-RS. - CWWKG0033W message should be in the server logs saying the ref
     * value for authFilterRef couldn't be found in the config.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_authFilterRef_nonExistentFilter_defaultFilterConfigured() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        setUpDefaultAuthFilterSettings(updatedRsSamlSettings);

        RSSamlProviderSettings rsSamlProvider = updatedRsSamlSettings.getDefaultRSSamlProviderSettings();
        rsSamlProvider.setAuthFilterRef("nonExistentButDefaulConfigured");

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, testSettings);

        expectations = addMessageExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.APP_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS,
                "Did not find expected warning message in the logs for non-existent authFilterRef.", SAMLMessageConstants.CWWKG0033W_REF_VALUE_NOT_FOUND_IN_CONFIG);

        generalConfigTest(updatedRsSamlSettings, expectations, testSettings);
    }

    /**
     * Test purpose: - authFilterRef: Auth filter is configured to protect a
     * resource other than the JAX-RS resource - The JAX-RS resource is
     * protected, so authentication/authorization must be performed. - RS SAML
     * is not configured to protect this resource and proper credentials are not
     * provided. Expected results: - 401 when invoking JAX-RS.
     *
     * @throws Exception
     */
    // @Mode(TestMode.LITE)
    @Test
    public void RSSamlIDPInitiatedConfigTests_authFilterRef_filterProtectsOtherResource() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        // Create and set the default auth filter settings
        updatedRsSamlSettings.setDefaultAuthFilterSettings();

        setUpAuthFilterSettings(updatedRsSamlSettings, "myAuthFilter", "myapp/resource");

        // 1) due to task 201738, we no longer get the RegistryException if no
        // UserRegistry defined.
        // 2) In the helloworld.war file (see
        // com.ibm.ws.saml.sso-20_fat.jaxrs.config), it does not specify a login
        // form or error handling
        // we only get the 401 error code.
        List<validationData> expectations = vData.addSuccessStatusCodes(null, SAMLConstants.INVOKE_JAXRS_GET);
        expectations = vData.addResponseStatusExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.UNAUTHORIZED_STATUS);

        generalConfigTest(updatedRsSamlSettings, expectations, testSettings);
    }

    /**
     * Test purpose: - authFilterRef: Refers to the default auth filter - Two
     * other RS SAML providers are configured to use the same auth filter
     * Expected results: - 403 when invoking JAX-RS. - CWWKS5203E message in the
     * app server log saying the runtime didn't know which service provider to
     * use. - CWWKS3005E message in the app server log saying a UserRegistry
     * implementation service wasn't available.
     *
     * @throws Exception
     */
    // @Mode(TestMode.LITE)
    @ExpectedFFDC({ "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_authFilterRef_defaultAuthFilter_multipleRsSamlProviders() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        setUpDefaultAuthFilterSettings(updatedRsSamlSettings);

        // Get the id of the default auth filter
        AuthFilterSettings authFilter = updatedRsSamlSettings.getDefaultAuthFilterSettings();
        String authFilterId = authFilter.getId();

        // Create a second RS SAML provider whose authFilterRef points to the
        // same auth filter as the first RS SAML provider
        RSSamlConfigSettings rsSamlSettings2 = rsConfigSettings.copyConfigSettings();
        RSSamlProviderSettings rsSamlProvider2 = rsSamlSettings2.getDefaultRSSamlProviderSettings();
        rsSamlProvider2.setId("rsSaml2");
        rsSamlProvider2.setAuthFilterRef(authFilterId);

        // Create a third RS SAML provider whose authFilterRef points to the
        // same auth filter as the first RS SAML provider
        RSSamlConfigSettings rsSamlSettings3 = rsConfigSettings.copyConfigSettings();
        RSSamlProviderSettings rsSamlProvider3 = rsSamlSettings3.getDefaultRSSamlProviderSettings();
        rsSamlProvider3.setId("rsSaml3");
        rsSamlProvider3.setAuthFilterRef(authFilterId);

        // Add the new RS SAML providers to the configuration
        updatedRsSamlSettings.setRSSamlProviderSettings(rsSamlProvider2.getId(), rsSamlProvider2);
        updatedRsSamlSettings.setRSSamlProviderSettings(rsSamlProvider3.getId(), rsSamlProvider3);

        List<validationData> expectations = getSamlErrorExpectationsForJaxrsGet("Did not receive expected CWWKS5077E message saying the server couldn't select a SAML provider.",
                SAMLMessageConstants.CWWKS5077E_CANNOT_SELECT_SAML_PROVIDER);
        // addMessageExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET,
        // SAMLConstants.APP_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS,
        // "Did not get expected message saying no user registry service was
        // available.", SAMLMessageConstants.CWWKS3005E_NO_USER_REGISTRY);

        generalConfigTest(updatedRsSamlSettings, expectations, testSettings);
    }

    /**
     * Test purpose: - authFilterRef: Refers to default auth filter - Second
     * auth filter defined also protects the JAX-RS resource. - Third auth
     * filter defined protects an unrelated resource. Expected results: - The
     * SAML token should be successfully processed by JAX-RS. - The second and
     * third auth filters should never be processed.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_authFilterRef_defaultAuthFilter_multipleFilters() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        setUpDefaultAuthFilterSettings(updatedRsSamlSettings);

        // Create a second auth filter that also protects the helloworld
        // resource
        AuthFilterSettings authFilter2 = new AuthFilterSettings();
        authFilter2.setDefaultRequestUrlSettings();
        authFilter2.setId("myAuthFilter2");
        RequestUrlSettings authFilter2ReqUrlSettings = authFilter2.getDefaultRequestUrlSettings();
        authFilter2ReqUrlSettings.setUrlPattern(SAMLConstants.PARTIAL_HELLO_WORLD_URI);

        // Create a third auth filter with default settings (does not protect
        // helloworld resource)
        AuthFilterSettings authFilter3 = new AuthFilterSettings();
        authFilter3.setDefaultRequestUrlSettings();
        authFilter3.setId("myAuthFilter3");

        // Add the new auth filters to the configuration
        updatedRsSamlSettings.setAuthFilterSettings(authFilter2.getId(), authFilter2);
        updatedRsSamlSettings.setAuthFilterSettings(authFilter3.getId(), authFilter3);

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, testSettings);

        expectations = addMessageExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.APP_MESSAGES_LOG, SAMLConstants.STRING_DOES_NOT_CONTAIN,
                "Found log message saying myAuthFilter2 has been processed when it should NOT have been.", SAMLMessageConstants.CWWKS4358I_AUTH_FILTER_PROCESSED + "myAuthFilter2");
        expectations = addMessageExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.APP_MESSAGES_LOG, SAMLConstants.STRING_DOES_NOT_CONTAIN,
                "Found log message saying myAuthFilter3 has been processed when it should NOT have been.", SAMLMessageConstants.CWWKS4358I_AUTH_FILTER_PROCESSED + "myAuthFilter3");

        generalConfigTest(updatedRsSamlSettings, expectations, testSettings);
    }

    /*****************************
     * Pure SAML and SAML JAX-RS interop tests
     *****************************/

    /**
     * Test purpose: - enabled: false Expected results: - Enabled should be
     * honored for inbound propagation configurations - 401 when invoking
     * JAX-RS.
     *
     * @throws Exception
     */
    // @Mode(TestMode.LITE)
    @Test
    public void RSSamlIDPInitiatedConfigTests_enabled_false() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();

        RSSamlProviderSettings rsSamlProviderSettings = updatedRsSamlSettings.getDefaultRSSamlProviderSettings();
        rsSamlProviderSettings.nullifyPureSamlAttributes();

        // Disable this samlWebSso20 element
        rsSamlProviderSettings.setEnabled("false");

        // 1) due to task 201738, we no longer get the RegistryException if no
        // UserRegistry defined.
        // 2) In the helloworld.war file (see
        // com.ibm.ws.saml.sso-20_fat.jaxrs.config), it does not specify a login
        // form or error handling
        // we only get the 401 error code.
        List<validationData> expectations = vData.addSuccessStatusCodes(null, SAMLConstants.INVOKE_JAXRS_GET);
        expectations = vData.addResponseStatusExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.UNAUTHORIZED_STATUS);

        generalConfigTest(updatedRsSamlSettings, expectations, testSettings);

    }

    /**
     * Test purpose: - idpMetadata = Some invalid path Expected results: - The
     * idpMetadata attribute should be ignored and have no effect on the
     * runtime. - The SAML token should be successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_invalidIdpMetadataPath() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        RSSamlProviderSettings defaultRsSettings = updatedRsSamlSettings.getDefaultRSSamlProviderSettings();
        defaultRsSettings.setIdpMetadata("some/invalid/path");

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, testSettings, DO_NOT_CHECK_REALM);

        generalConfigTest(updatedRsSamlSettings, expectations, testSettings);

    }

    /**
     * Test purpose: - Set all pure SAML attributes to random or non-default
     * values Expected results: - The runtime should ignore all pure SAML
     * attributes. - The SAML token should be successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_setAllPureSamlAttributes() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();

        RSSamlProviderSettings rsSamlProviderSettings = updatedRsSamlSettings.getDefaultRSSamlProviderSettings();
        rsSamlProviderSettings.nullifyPureSamlAttributes();

        // Set pure SAML attributes to non-default, random, and or potentially
        // invalid values; all of these should be ignored by the runtime
        rsSamlProviderSettings.setAuthnRequestsSigned(Utils.getRandomSelection("true", "false"));
        rsSamlProviderSettings.setForceAuthn(Utils.getRandomSelection("true", "false"));
        rsSamlProviderSettings.setIsPassive(Utils.getRandomSelection("true", "false"));
        rsSamlProviderSettings.setAllowCreate(Utils.getRandomSelection("true", "false"));
        rsSamlProviderSettings.setAuthnContextClassRef("someURI/orOther.doesn'tMatter");
        rsSamlProviderSettings.setAuthnContextComparisonType(Utils.getRandomSelection("exact", "minimum", "maximum", "better"));
        rsSamlProviderSettings.setNameIDFormat(Utils.getRandomSelection("unspecified", "email", "x509SubjectName", "windowsDomainQualifiedName", "kerberos", "entity", "persistent", "transient", "encrypted", "customize"));
        rsSamlProviderSettings.setCustomizeNameIDFormat("otherFormat");
        rsSamlProviderSettings.setIdpMetadata("bootstrap.properties");
        rsSamlProviderSettings.setLoginPageURL("server.xml");
        rsSamlProviderSettings.setErrorPageURL("nonExistent/other.html");
        rsSamlProviderSettings.setTokenReplayTimeout("1ms");
        rsSamlProviderSettings.setSessionNotOnOrAfter("1ms");
        rsSamlProviderSettings.setSpHostAndPort("nothing.exists.here.ibm.com:9080/#test");
        rsSamlProviderSettings.setHttpsRequired("true");
        rsSamlProviderSettings.setAllowCustomCacheKey("false");
        rsSamlProviderSettings.setCreateSession("false");

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, testSettings, DO_NOT_CHECK_REALM);

        // Set the target app to use HTTP instead of HTTPS (since we set
        // httpsRequired="true")
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        String targetApp = updatedTestSettings.getSpTargetApp();
        targetApp = targetApp.replace("https", "http");
        targetApp = targetApp.replaceAll("localhost:[0-9]+", "localhost:" + testAppServer.getServerHttpPort());
        updatedTestSettings.setSpTargetApp(targetApp);

        generalConfigTest(updatedRsSamlSettings, expectations, updatedTestSettings);

    }

    /**
     * Test purpose: - inboundPropagation = "none" - headerName = Some unknown
     * value - audiences = Some unknown value Expected results: - The pure
     * inbound propagation config attributes should be ignored and have no
     * effect on the runtime. - There should be no problem obtaining the SAML
     * token. - The JAX-RS app invocation should not go through, so the "Hello
     * world!" string shouldn't be seen.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_inboundNone_setInboundProps() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        RSSamlProviderSettings defaultRsSettings = new RSSamlProviderSettings();
        defaultRsSettings.setInboundPropagation("none");
        defaultRsSettings.setAudiences("none");
        defaultRsSettings.setHeaderName("SomethingOdd");
        defaultRsSettings.setSignatureMethodAlgorithm("SHA256");

        // Update the bootstrap prop for IdP server since that variable doesn't
        // get set for the app server
        String idpMetadata = defaultRsSettings.getIdpMetadata();
        idpMetadata = idpMetadata.replace("${tfimIdpServer}", testSettings.getIdpRoot());
        defaultRsSettings.setIdpMetadata(idpMetadata);

        updatedRsSamlSettings.setRSSamlProviderSettings(defaultRsSettings.getId(), defaultRsSettings);

        // "Pure" RS SAML config attributes should have no effect on the "pure"
        // SAML flow
        List<validationData> expectations = helpers.setDefaultGoodSAMLExpectations(flowType, testSettings);

        // Should not successfully reach the RS app
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_DOES_NOT_CONTAIN, "Unexpectedly received the \"" + SAMLConstants.HELLO_WORLD_STRING + "\" string.", null, SAMLConstants.HELLO_WORLD_STRING);

        generalConfigTest(updatedRsSamlSettings, expectations, testSettings);

    }

}
