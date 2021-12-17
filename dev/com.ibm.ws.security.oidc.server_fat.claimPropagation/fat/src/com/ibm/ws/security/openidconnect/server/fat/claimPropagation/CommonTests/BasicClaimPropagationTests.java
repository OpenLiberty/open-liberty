/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.fat.claimPropagation.CommonTests;

import java.util.List;

import org.junit.Test;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;

public class BasicClaimPropagationTests extends CommonTest {

    private static final Class<?> thisClass = BasicClaimPropagationTests.class;
    public static RSCommonTestTools rsTools = new RSCommonTestTools();
    //
    protected static String WithRegistry_withUser = "WithRegistry_withUser";
    protected static String WithRegistry_withoutUser = "WithRegistry_withoutUser";
    protected static String WithoutRegistry = "WithoutRegistry";
    protected static final Boolean thirdPartyPropagatedIdTokenTrue = true;
    protected static final Boolean thirdPartyPropagatedIdTokenFalse = false;
    protected static final Boolean thirdPartyPropagatedAccessTokenTrue = true;
    protected static final Boolean thirdPartyPropagatedAccessTokenFalse = false;
    protected static final String[] noThirdPartyParm = null;
    protected static final String[] oneThirdPartyParm = new String[] { "testProp1" };
    protected static final String[] allThirdPartyParms = new String[] { "testProp1", "testProp2" };
    protected static final String[] allIntermedParms = new String[] { "testProp3", "testProp4" };

    protected static String OPServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.claimPropagation.op";
    protected static String ExternalOPServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.claimPropagation.op.external";
    protected static String RPServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.claimPropagation.rp";
    public static TestServer testExternalOPServer = null;

    /**
     * Add an expectation for a claim that should be found in the ID_Token
     *
     * @param expectations
     *            - current expectations
     * @param claim
     *            - claim that should be found in the ID_Token
     * @param value
     *            - value for the claim that should be found in the ID_Token
     * @return - updated expectations
     * @throws Exception
     */
    private List<validationData> addShouldContain_access_token(List<validationData> expectations, String claim, String value) throws Exception {

        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_JWT_TOKEN, Constants.STRING_CONTAINS, "\"" + claim + "\" was either not found or was not correct in the JWT token", claim, value);
        return expectations;
    }

    private List<validationData> addShouldContain_idToken(List<validationData> expectations, String claim, String value) throws Exception {

        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_ID_TOKEN, Constants.STRING_CONTAINS, "\"" + claim + "\" was either not found or was not correct in the id_token", claim, value);
        return expectations;
    }

    private List<validationData> addShouldMatch_access_token(List<validationData> expectations, String claim, String value) throws Exception {

        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_JWT_TOKEN, Constants.STRING_MATCHES, "\"" + claim + "\" was either not found or was not correct in the JWT token", claim, value);
        return expectations;
    }

    private List<validationData> addShouldMatch_idToken(List<validationData> expectations, String claim, String value) throws Exception {

        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_ID_TOKEN, Constants.STRING_MATCHES, "\"" + claim + "\" was either not found or was not correct in the id_token", claim, value);
        return expectations;
    }

    /**
     * Add an expectation for a claim that should NOT be found in the ID_Token
     *
     * @param expectations
     *            - current expectations
     * @param claim
     *            - claim that should NOT be found in the ID_Token
     * @return - updated expectations
     * @throws Exception
     */
    private List<validationData> addShouldNotContain_access_token(List<validationData> expectations, String claim) throws Exception {

        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_JWT_TOKEN, Constants.STRING_CONTAINS, "\"" + claim + "\" was found in the access token and should NOT have been", claim, null);
        return expectations;
    }

    private List<validationData> addShouldNotContain_idToken(List<validationData> expectations, String claim) throws Exception {

        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_ID_TOKEN, Constants.STRING_CONTAINS, "\"" + claim + "\" was found in the id_token and should NOT have been", claim, null);
        return expectations;
    }

    private List<validationData> setGroupIdsExpectations(List<validationData> expectations, boolean thirdPartyPropagatedIdToken, boolean thirdPartyPropagatedAccessToken) throws Exception {

        // TODO - should I add a check for groupIds=[group:SomeRealm/group2, group:SomeRealm/group1] (realm and group, not just group)?

        if (RepeatTestFilter.isRepeatActionActive(WithoutRegistry)) {
            if (thirdPartyPropagatedIdToken) {
                expectations = addShouldMatch_idToken(expectations, "groupIds", "group1");
                expectations = addShouldMatch_idToken(expectations, "groupIds", "group2");
            } else {
                expectations = addShouldNotContain_idToken(expectations, "groupIds");
            }
            if (thirdPartyPropagatedAccessToken) {
                expectations = addShouldMatch_access_token(expectations, "groupIds", "group1");
                expectations = addShouldMatch_access_token(expectations, "groupIds", "group2");
            } else {
                expectations = addShouldNotContain_access_token(expectations, "groupIds");
            }
        } else {
            if (RepeatTestFilter.isRepeatActionActive(WithRegistry_withUser)) {
                expectations = addShouldMatch_idToken(expectations, "groupIds", "group3");
                expectations = addShouldMatch_access_token(expectations, "groupIds", "group3");
            } else {
                expectations = addShouldNotContain_idToken(expectations, "groupIds");
                expectations = addShouldNotContain_access_token(expectations, "groupIds");
            }
        }

        return expectations;
    }

    private List<validationData> setTestParmExpectations(List<validationData> expectations, String[] thirdPartyParms, boolean thirdPartyPropagatedIdToken, boolean thirdPartyPropagatedAccessToken) throws Exception {

        for (String parm : allThirdPartyParms) {
            if (thirdPartyParms != null && validationTools.isInList(thirdPartyParms, parm)) {
                if (thirdPartyPropagatedIdToken) {
                    expectations = addShouldContain_idToken(expectations, parm, testSettings.getUserName());
                } else {
                    expectations = addShouldNotContain_idToken(expectations, parm);
                }
            } else {
                expectations = addShouldNotContain_idToken(expectations, parm);
            }

            if (thirdPartyParms != null && validationTools.isInList(thirdPartyParms, parm)) {
                if (thirdPartyPropagatedAccessToken) {
                    expectations = addShouldContain_access_token(expectations, parm, testSettings.getUserName());
                } else {
                    expectations = addShouldNotContain_access_token(expectations, parm);
                }
            } else {
                expectations = addShouldNotContain_access_token(expectations, parm);
            }
        }

        // add checks for parms defined by the intermediate OP (no propagation involved)
        for (String parm : allIntermedParms) {

            if (RepeatTestFilter.isRepeatActionActive(WithRegistry_withUser)) {
                expectations = addShouldContain_idToken(expectations, parm, testSettings.getUserName());
                expectations = addShouldContain_access_token(expectations, parm, testSettings.getUserName());
            } else {
                expectations = addShouldNotContain_idToken(expectations, parm);
                expectations = addShouldNotContain_access_token(expectations, parm);
            }

        }

        return expectations;
    }

    private List<validationData> setIssExpectations(List<validationData> expectations, String provider) throws Exception {

        expectations = addShouldContain_idToken(expectations, "iss", testOPServer.getHttpsString() + "/oidc/endpoint/OP_" + provider);
        expectations = addShouldContain_access_token(expectations, "iss", testOPServer.getHttpsString() + "/oidc/endpoint/OP_" + provider);

        return expectations;
    }

    private String getRealmName() throws Exception {
        String realmName = "SomeRealm";

        if (RepeatTestFilter.isRepeatActionActive(WithoutRegistry)) {
            realmName = "WIMRegistry";
        }
        return realmName;
    }

    private List<validationData> setRealmNameExpectations(List<validationData> expectations) throws Exception {

        expectations = addShouldContain_idToken(expectations, "realmName", getRealmName());
        expectations = addShouldContain_access_token(expectations, "realmName", getRealmName());

        return expectations;
    }

    private List<validationData> setAudExpectations(List<validationData> expectations, String appName, boolean thirdPartyPropagatedAccessToken) throws Exception {

        // tests are set up to use clients and providers whose names are based off of the appNames
        String clientId = appName.replace("_3rdPartyDoesNotPropagate", "").replace("_3rdPartyPropagates", "");
        expectations = addShouldContain_idToken(expectations, "aud", clientId);
        if (thirdPartyPropagatedAccessToken) {
            expectations = addShouldContain_access_token(expectations, "aud", clientId);
        } else {
            expectations = addShouldNotContain_access_token(expectations, "aud");
        }

        return expectations;
    }

    /***************************************************** Tests *****************************************************/

    /**
     * The Third part OP will populate the idToken and access_token that it creates with the groupIds claim (by default)
     * and 2 other test specified claims.
     * The intermediate OP does not have thirdPartyIDTokenClaims or thirdPartyAccessTokenClaims configured. The groupIds
     * claim from the thrid party OP will not be included in the idToken or access_token produced by the intermediate OP.
     * Test confirms that the groupIds claim is NOT in either the idToken nor the access_token
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void BasicClaimPropagationTests_thirdPartyIDTokenClaims_none_thirdPartyAccessTokenClaims_none_3rdPartyDoesNotPropagate() throws Exception {

        String appName = "noExtraClaims_3rdPartyDoesNotPropagate";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedIdTokenFalse, thirdPartyPropagatedAccessTokenFalse);
        expectations = setTestParmExpectations(expectations, noThirdPartyParm, thirdPartyPropagatedIdTokenFalse, thirdPartyPropagatedAccessTokenFalse);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName, thirdPartyPropagatedAccessTokenFalse);

        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
    }

    /**
     * The Third part OP will populate the idToken and access_token that it creates with the groupIds claim (by default)
     * and 2 other test specified claims.
     * The intermediate OP does not have thirdPartyIDTokenClaims or thirdPartyAccessTokenClaims configured. The groupIds
     * claim from the thrid party OP will not be included in the idToken or access_token produced by the intermediate OP.
     * Test confirms that the groupIds claim is NOT in either the idToken nor the access_token
     *
     * @throws Exception
     */
    @Test
    public void BasicClaimPropagationTests_thirdPartyIDTokenClaims_groupIds_thirdPartyAccessTokenClaims_none_3rdPartyDoesNotPropagate() throws Exception {

        String appName = "propagateGroupIdsIdTokenClaims_3rdPartyDoesNotPropagate";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedIdTokenTrue, thirdPartyPropagatedAccessTokenFalse);
        expectations = setTestParmExpectations(expectations, noThirdPartyParm, thirdPartyPropagatedIdTokenFalse, thirdPartyPropagatedAccessTokenFalse);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName, thirdPartyPropagatedAccessTokenFalse);
        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
    }

    /**
     * Test that the intermediate OP will not contain the test claim that it says to propagate when that claim
     * does NOT exist in the token returned from the third party OP. Make sure that there are no error messages
     * logged in the intermediate OP server log (the framework searches for unexpected messages automatically)
     *
     * @throws Exception
     */
    //    @Mode(TestMode.LITE)
    @Test
    public void BasicClaimPropagationTests_thirdPartyIDTokenClaims_1TestClaim_thirdPartyAccessTokenClaims_none_3rdPartyDoesNotPropagate() throws Exception {

        String appName = "propagate1TestClaimIdTokenClaims_3rdPartyDoesNotPropagate";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedIdTokenFalse, thirdPartyPropagatedAccessTokenFalse);
        expectations = setTestParmExpectations(expectations, noThirdPartyParm, thirdPartyPropagatedIdTokenFalse, thirdPartyPropagatedAccessTokenFalse);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName, thirdPartyPropagatedAccessTokenFalse);
        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
    }

    @Test
    public void BasicClaimPropagationTests_thirdPartyIDTokenClaims_2TestClaims_thirdPartyAccessTokenClaims_none_3rdPartyDoesNotPropagate() throws Exception {

        String appName = "propagate2TestClaimsIdTokenClaims_3rdPartyDoesNotPropagate";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedIdTokenFalse, thirdPartyPropagatedAccessTokenFalse);
        expectations = setTestParmExpectations(expectations, noThirdPartyParm, thirdPartyPropagatedIdTokenFalse, thirdPartyPropagatedAccessTokenFalse);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName, thirdPartyPropagatedAccessTokenFalse);
        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
    }

    /**
     * groupIds are always included as a claim in the third party token (if groups exist in the registry)
     *
     * @throws Exception
     */
    @Test
    public void BasicClaimPropagationTests_thirdPartyIDTokenClaims_none_thirdPartyAccessTokenClaims_groupIds_3rdPartyDoesNotPropagate() throws Exception {

        String appName = "propagateGroupIdsAccessTokenClaims_3rdPartyDoesNotPropagate";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedIdTokenFalse, thirdPartyPropagatedAccessTokenTrue);
        expectations = setTestParmExpectations(expectations, noThirdPartyParm, thirdPartyPropagatedIdTokenFalse, thirdPartyPropagatedAccessTokenFalse);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName, thirdPartyPropagatedAccessTokenFalse);

        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
    }

    @Test
    public void BasicClaimPropagationTests_thirdPartyIDTokenClaims_none_thirdPartyAccessTokenClaims_1TestClaim_3rdPartyDoesNotPropagate() throws Exception {

        String appName = "propagate1TestClaimAccessTokenClaims_3rdPartyDoesNotPropagate";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedIdTokenFalse, thirdPartyPropagatedAccessTokenFalse);
        expectations = setTestParmExpectations(expectations, noThirdPartyParm, thirdPartyPropagatedIdTokenFalse, thirdPartyPropagatedAccessTokenTrue);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName, thirdPartyPropagatedAccessTokenFalse);

        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
    }

    @Test
    public void BasicClaimPropagationTests_thirdPartyIDTokenClaims_none_thirdPartyAccessTokenClaims_2TestClaims_3rdPartyDoesNotPropagate() throws Exception {

        String appName = "propagate2TestClaimsAccessTokenClaims_3rdPartyDoesNotPropagate";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedIdTokenFalse, thirdPartyPropagatedAccessTokenFalse);
        expectations = setTestParmExpectations(expectations, noThirdPartyParm, thirdPartyPropagatedIdTokenFalse, thirdPartyPropagatedAccessTokenTrue);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName, thirdPartyPropagatedAccessTokenFalse);

        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
    }

    @Test
    public void BasicClaimPropagationTests_thirdPartyIDTokenClaims_none_thirdPartyAccessTokenClaims_none_3rdPartytPropagates() throws Exception {

        String appName = "noExtraClaims_3rdPartyPropagates";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedIdTokenFalse, thirdPartyPropagatedAccessTokenFalse);
        expectations = setTestParmExpectations(expectations, noThirdPartyParm, thirdPartyPropagatedIdTokenFalse, thirdPartyPropagatedAccessTokenFalse);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName, thirdPartyPropagatedAccessTokenFalse);

        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
    }

    @Test
    public void BasicClaimPropagationTests_thirdPartyIDTokenClaims_groupIds_thirdPartyAccessTokenClaims_none_3rdPartyPropagates() throws Exception {

        String appName = "propagateGroupIdsIdTokenClaims_3rdPartyPropagates";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedIdTokenTrue, thirdPartyPropagatedAccessTokenFalse);
        expectations = setTestParmExpectations(expectations, noThirdPartyParm, thirdPartyPropagatedIdTokenFalse, thirdPartyPropagatedAccessTokenFalse);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName, thirdPartyPropagatedAccessTokenFalse);

        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
    }

    @Test
    public void BasicClaimPropagationTests_thirdPartyIDTokenClaims_1TestClaim_thirdPartyAccessTokenClaims_none_3rdPartyPropagates() throws Exception {

        String appName = "propagate1TestClaimIdTokenClaims_3rdPartyPropagates";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedIdTokenFalse, thirdPartyPropagatedAccessTokenFalse);
        expectations = setTestParmExpectations(expectations, oneThirdPartyParm, thirdPartyPropagatedIdTokenTrue, thirdPartyPropagatedAccessTokenFalse);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName, thirdPartyPropagatedAccessTokenFalse);

        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
    }

    @Test
    public void BasicClaimPropagationTests_thirdPartyIDTokenClaims_2TestClaims_thirdPartyAccessTokenClaims_none_3rdPartyPropagates() throws Exception {

        String appName = "propagate2TestClaimsIdTokenClaims_3rdPartyPropagates";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedIdTokenFalse, thirdPartyPropagatedAccessTokenFalse);
        expectations = setTestParmExpectations(expectations, allThirdPartyParms, thirdPartyPropagatedIdTokenTrue, thirdPartyPropagatedAccessTokenFalse);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName, thirdPartyPropagatedAccessTokenFalse);

        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
    }

    // add test to propagate testProp that was not included in thrid party token

    @Test
    public void BasicClaimPropagationTests_thirdPartyIDTokenClaims_none_thirdPartyAccessTokenClaims_groupIds_3rdPartyPropagates() throws Exception {

        String appName = "propagateGroupIdsAccessTokenClaims_3rdPartyPropagates";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedIdTokenFalse, thirdPartyPropagatedAccessTokenTrue);
        expectations = setTestParmExpectations(expectations, noThirdPartyParm, thirdPartyPropagatedIdTokenFalse, thirdPartyPropagatedAccessTokenFalse);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName, thirdPartyPropagatedAccessTokenFalse);

        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
    }

    @Test
    public void BasicClaimPropagationTests_thirdPartyIDTokenClaims_none_thirdPartyAccessTokenClaims_1TestClaim_3rdPartyPropagates() throws Exception {

        String appName = "propagate1TestClaimAccessTokenClaims_3rdPartyPropagates";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedIdTokenFalse, thirdPartyPropagatedAccessTokenFalse);
        expectations = setTestParmExpectations(expectations, oneThirdPartyParm, thirdPartyPropagatedIdTokenFalse, thirdPartyPropagatedAccessTokenTrue);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName, thirdPartyPropagatedAccessTokenFalse);

        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
    }

    @Test
    public void BasicClaimPropagationTests_thirdPartyIDTokenClaims_none_thirdPartyAccessTokenClaims_2TestClaims_3rdPartyPropagates() throws Exception {

        String appName = "propagate2TestClaimsAccessTokenClaims_3rdPartyPropagates";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedIdTokenFalse, thirdPartyPropagatedAccessTokenFalse);
        expectations = setTestParmExpectations(expectations, allThirdPartyParms, thirdPartyPropagatedIdTokenFalse, thirdPartyPropagatedAccessTokenTrue);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName, thirdPartyPropagatedAccessTokenFalse);

        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
    }

    // Test claims from 3rd Party OP override local OP value (ie: iss)
    @Mode(TestMode.LITE)
    @Test
    public void BasicClaimPropagationTests_thirdPartyIDTokenClaims_none_thirdPartyIdTokenClaims_issClaim() throws Exception {

        String appName = "issClaimInIdToken";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedIdTokenFalse, thirdPartyPropagatedAccessTokenFalse);
        expectations = setTestParmExpectations(expectations, noThirdPartyParm, thirdPartyPropagatedIdTokenFalse, thirdPartyPropagatedAccessTokenFalse);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName, thirdPartyPropagatedAccessTokenFalse);

        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
    }

    @Mode(TestMode.LITE)
    @Test
    public void BasicClaimPropagationTests_thirdPartyIDTokenClaims_none_thirdPartyAccessTokenClaims_issClaim() throws Exception {

        String appName = "issClaimInAccessToken";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedIdTokenFalse, thirdPartyPropagatedAccessTokenFalse);
        expectations = setTestParmExpectations(expectations, noThirdPartyParm, thirdPartyPropagatedIdTokenFalse, thirdPartyPropagatedAccessTokenFalse);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName, thirdPartyPropagatedAccessTokenFalse);

        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
    }

    //    //    @Mode(TestMode.LITE)
    //    @Test
    //    public void BasicClaimPropagationTests_thirdPartyIDTokenClaims_none_thirdPartyIdTokenClaims_subClaim() throws Exception {
    //
    //        WebConversation wc = new WebConversation();
    //        TestSettings updatedTestSettings = testSettings.copyTestSettings();
    //        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/subClaimInIdToken"));
    //
    //        List<validationData> expectations = vData.addSuccessStatusCodes(null);
    //        // sub is not propagated, so we should find the intermediate OP's value (not the third party value)
    //        expectations = addShouldContain_both(expectations, "sub", updatedTestSettings.getUserName());
    //        expectations = addShouldNotContain_both(expectations, "testProp1");
    //        expectations = addShouldNotContain_both(expectations, "testProp2");
    //        expectations = addShouldNotContain_both(expectations, "groupIds");
    //        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
    //    }
    //
    //    //    @Mode(TestMode.LITE)
    //    @Test
    //    public void BasicClaimPropagationTests_thirdPartyIDTokenClaims_none_thirdPartyAccessTokenClaims_subClaim() throws Exception {
    //
    //        WebConversation wc = new WebConversation();
    //        TestSettings updatedTestSettings = testSettings.copyTestSettings();
    //        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/subClaimInAccessToken"));
    //
    //        List<validationData> expectations = vData.addSuccessStatusCodes(null);
    //        // sub is not propagated, so we should find the intermediate OP's value (not the third party value)
    //        expectations = addShouldContain_both(expectations, "sub", updatedTestSettings.getUserName());
    //        expectations = addShouldNotContain_both(expectations, "testProp1");
    //        expectations = addShouldNotContain_both(expectations, "testProp2");
    //        expectations = addShouldNotContain_both(expectations, "groupIds");
    //        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
    //    }

    @Mode(TestMode.LITE)
    @Test
    public void BasicClaimPropagationTests_thirdPartyIDTokenClaims_none_thirdPartyIdTokenClaims_audClaim() throws Exception {

        String appName = "audClaimInIdToken";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedIdTokenFalse, thirdPartyPropagatedAccessTokenFalse);
        expectations = setTestParmExpectations(expectations, noThirdPartyParm, thirdPartyPropagatedIdTokenFalse, thirdPartyPropagatedAccessTokenFalse);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName, thirdPartyPropagatedAccessTokenFalse);

        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
    }

    @Mode(TestMode.LITE)
    @Test
    public void BasicClaimPropagationTests_thirdPartyIDTokenClaims_none_thirdPartyAccessTokenClaims_audClaim() throws Exception {

        String appName = "audClaimInAccessToken";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedIdTokenFalse, thirdPartyPropagatedAccessTokenFalse);
        expectations = setTestParmExpectations(expectations, noThirdPartyParm, thirdPartyPropagatedIdTokenFalse, thirdPartyPropagatedAccessTokenFalse);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName, thirdPartyPropagatedAccessTokenTrue);

        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
    }

    /**
     * Create a
     */
    //    {"sub":"testuser","azp":"oidcclient_noExtraClaims","realmName":"WIMRegistry","scope":["openid"],"iss":"https:\/\/localhost:8920\/oidc\/endpoint\/OP_noExtraClaims_3rdPartyDoesNotPropagate","token_type":"Bearer","exp":1639512363,"iat":1639505163}
}
