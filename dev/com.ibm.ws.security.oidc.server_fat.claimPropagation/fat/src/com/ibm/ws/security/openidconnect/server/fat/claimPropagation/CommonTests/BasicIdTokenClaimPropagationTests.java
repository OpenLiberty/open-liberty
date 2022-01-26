/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

public class BasicIdTokenClaimPropagationTests extends CommonTest {

    private static final Class<?> thisClass = BasicIdTokenClaimPropagationTests.class;
    public static RSCommonTestTools rsTools = new RSCommonTestTools();
    //
    protected static String WithRegistry_withUser = "WithRegistry_withUser";
    protected static String WithRegistry_withoutUser = "WithRegistry_withoutUser";
    protected static String WithoutRegistry = "WithoutRegistry";
    protected static String WithRegistry_withUser_implicit = "WithRegistry_withUser_implicit";
    protected static String WithRegistry_withoutUser_implicit = "WithRegistry_withoutUser_implicit";
    protected static String WithoutRegistry_implicit = "WithoutRegistry_implicit";
    protected static final Boolean thirdPartyPropagatedIdTokenTrue = true;
    protected static final Boolean thirdPartyPropagatedIdTokenFalse = false;
    protected static final String[] noThirdPartyParm = null;
    protected static final String[] oneThirdPartyParm = new String[] { "testProp1" };
    protected static final String[] allThirdPartyParms = new String[] { "testProp1", "testProp2" };
    protected static final String[] allIntermedParms = new String[] { "testProp3", "testProp4" };
    protected static String repeatAction = null;

    // create vars that extending method can set to allow use of test cases for oidc, and maybe some day saml
    protected static String[] steps = null;
    protected static String loginStep = null;

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
    private List<validationData> addShouldContain_idToken(List<validationData> expectations, String claim, String value) throws Exception {

        expectations = vData.addExpectation(expectations, loginStep, Constants.RESPONSE_ID_TOKEN, Constants.STRING_CONTAINS, "\"" + claim + "\" was either not found or was not correct in the id_token", claim, value);
        return expectations;
    }

    private List<validationData> addShouldMatch_idToken(List<validationData> expectations, String claim, String value) throws Exception {

        expectations = vData.addExpectation(expectations, loginStep, Constants.RESPONSE_ID_TOKEN, Constants.STRING_MATCHES, "\"" + claim + "\" was either not found or was not correct in the id_token", claim, value);
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
    private List<validationData> addShouldNotContain_idToken(List<validationData> expectations, String claim) throws Exception {

        expectations = vData.addExpectation(expectations, loginStep, Constants.RESPONSE_ID_TOKEN, Constants.STRING_CONTAINS, "\"" + claim + "\" was found in the id_token and should NOT have been", claim, null);
        return expectations;
    }

    private List<validationData> setGroupIdsExpectations(List<validationData> expectations, boolean thirdPartyPropagatedIdToken) throws Exception {

        Log.info(thisClass, "setGroupIdsExpectations", "Propagate Id Token: " + thirdPartyPropagatedIdToken);
        // TODO - should I add a check for groupIds=[group:SomeRealm/group2, group:SomeRealm/group1] (realm and group, not just group)?
        // Update for propagated groups and local groups

        // if groupIds is in the third party token, that will override the local value
        if (thirdPartyPropagatedIdToken || repeatAction.contains(Constants.IMPLICIT_GRANT_TYPE)) {
            expectations = addShouldMatch_idToken(expectations, "groupIds", "group1");
            expectations = addShouldMatch_idToken(expectations, "groupIds", "group2");
        } else {
            if (repeatAction.contains(WithRegistry_withUser)) {
                expectations = addShouldMatch_idToken(expectations, "groupIds", "group3");
            } else {
                expectations = addShouldNotContain_idToken(expectations, "groupIds");
            }
        }

        return expectations;
    }

    private List<validationData> setTestParmExpectations(List<validationData> expectations, String[] thirdPartyParms, boolean thirdPartyPropagatedIdToken) throws Exception {

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

        }

        // add checks for parms defined by the intermediate OP (no propagation involved)
        for (String parm : allIntermedParms) {

            if (repeatAction.contains(WithRegistry_withUser)) {
                expectations = addShouldContain_idToken(expectations, parm, testSettings.getUserName());
            } else {
                expectations = addShouldNotContain_idToken(expectations, parm);
            }

        }

        return expectations;
    }

    private List<validationData> setIssExpectations(List<validationData> expectations, String provider) throws Exception {

        expectations = addShouldContain_idToken(expectations, "iss", testOPServer.getHttpsString() + "/oidc/endpoint/OP_" + provider);

        return expectations;
    }

    private String getRealmName() throws Exception {
        String realmName = "SomeRealm";

        if (repeatAction.contains(WithoutRegistry)) {
            realmName = "WIMRegistry";
        }

        if (repeatAction.contains(Constants.IMPLICIT_GRANT_TYPE)) {
            realmName = "BasicRealm";
        }

        return realmName;
    }

    private List<validationData> setRealmNameExpectations(List<validationData> expectations) throws Exception {

        expectations = addShouldContain_idToken(expectations, "realmName", getRealmName());

        return expectations;
    }

    private List<validationData> setAudExpectations(List<validationData> expectations, String appName) throws Exception {

        // tests are set up to use clients and providers whose names are based off of the appNames
        String clientId = appName.replace("_3rdPartyDoesNotPropagate", "").replace("_3rdPartyPropagates", "");
        expectations = addShouldContain_idToken(expectations, "aud", clientId);

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
    public void ThirdPartyIDTokenClaims_none_3rdPartyDoesNotPropagate() throws Exception {

        String appName = "noExtraClaims_3rdPartyDoesNotPropagate";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedIdTokenFalse);
        expectations = setTestParmExpectations(expectations, noThirdPartyParm, thirdPartyPropagatedIdTokenFalse);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName);

        genericRP(_testName, wc, updatedTestSettings, steps, expectations);
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
    public void ThirdPartyIDTokenClaims_groupIds_3rdPartyDoesNotPropagate() throws Exception {

        String appName = "propagateGroupIdsIdTokenClaims_3rdPartyDoesNotPropagate";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedIdTokenTrue);
        expectations = setTestParmExpectations(expectations, allIntermedParms, thirdPartyPropagatedIdTokenFalse);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName);
        genericRP(_testName, wc, updatedTestSettings, steps, expectations);
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
    public void ThirdPartyIDTokenClaims_1TestClaim_3rdPartyDoesNotPropagate() throws Exception {

        String appName = "propagate1TestClaimIdTokenClaims_3rdPartyDoesNotPropagate";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedIdTokenFalse);
        expectations = setTestParmExpectations(expectations, noThirdPartyParm, thirdPartyPropagatedIdTokenFalse);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName);
        genericRP(_testName, wc, updatedTestSettings, steps, expectations);
    }

    @Test
    public void ThirdPartyIDTokenClaims_2TestClaims_3rdPartyDoesNotPropagate() throws Exception {

        String appName = "propagate2TestClaimsIdTokenClaims_3rdPartyDoesNotPropagate";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedIdTokenFalse);
        expectations = setTestParmExpectations(expectations, noThirdPartyParm, thirdPartyPropagatedIdTokenFalse);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName);
        genericRP(_testName, wc, updatedTestSettings, steps, expectations);
    }

    @Test
    public void ThirdPartyIDTokenClaims_none_3rdPartyPropagates() throws Exception {

        String appName = "noExtraClaims_3rdPartyPropagates";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedIdTokenFalse);
        expectations = setTestParmExpectations(expectations, noThirdPartyParm, thirdPartyPropagatedIdTokenFalse);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName);

        genericRP(_testName, wc, updatedTestSettings, steps, expectations);
    }

    @Test
    public void ThirdPartyIDTokenClaims_groupIds_3rdPartyPropagates() throws Exception {

        String appName = "propagateGroupIdsIdTokenClaims_3rdPartyPropagates";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedIdTokenTrue);
        expectations = setTestParmExpectations(expectations, noThirdPartyParm, thirdPartyPropagatedIdTokenFalse);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName);

        genericRP(_testName, wc, updatedTestSettings, steps, expectations);
    }

    @Test
    public void ThirdPartyIDTokenClaims_1TestClaim_3rdPartyPropagates() throws Exception {

        String appName = "propagate1TestClaimIdTokenClaims_3rdPartyPropagates";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedIdTokenFalse);
        expectations = setTestParmExpectations(expectations, oneThirdPartyParm, thirdPartyPropagatedIdTokenTrue);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName);

        genericRP(_testName, wc, updatedTestSettings, steps, expectations);
    }

    @Test
    public void ThirdPartyIDTokenClaims_2TestClaims_3rdPartyPropagates() throws Exception {

        String appName = "propagate2TestClaimsIdTokenClaims_3rdPartyPropagates";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedIdTokenFalse);
        expectations = setTestParmExpectations(expectations, allThirdPartyParms, thirdPartyPropagatedIdTokenTrue);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName);

        genericRP(_testName, wc, updatedTestSettings, steps, expectations);
    }

    // add test to propagate testProp that was not included in thrid party token

    // Test claims from 3rd Party OP override local OP value (ie: iss)
    @Mode(TestMode.LITE)
    @Test
    public void ThirdPartyIDTokenClaims_none_thirdPartyIdTokenClaims_issClaim() throws Exception {

        String appName = "issClaimInIdToken";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedIdTokenFalse);
        expectations = setTestParmExpectations(expectations, noThirdPartyParm, thirdPartyPropagatedIdTokenFalse);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName);

        genericRP(_testName, wc, updatedTestSettings, steps, expectations);
    }

    //    //    @Mode(TestMode.LITE)
    //    @Test
    //    public void ThirdPartyIDTokenClaims_none_thirdPartyIdTokenClaims_subClaim() throws Exception {
    //
    //        WebClient webClient = getAndSaveWebClient(true);
    //        TestSettings updatedTestSettings = testSettings.copyTestSettings();
    //        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/subClaimInIdToken"));
    //
    //        List<validationData> expectations = vData.addSuccessStatusCodes(null);
    //        // sub is not propagated, so we should find the intermediate OP's value (not the third party value)
    //        expectations = addShouldContain_both(expectations, "sub", updatedTestSettings.getUserName());
    //        expectations = addShouldNotContain_both(expectations, "testProp1");
    //        expectations = addShouldNotContain_both(expectations, "testProp2");
    //        expectations = addShouldNotContain_both(expectations, "groupIds");
    //        genericRP(_testName, wc, updatedTestSettings, steps, expectations);
    //    }
    //

    @Mode(TestMode.LITE)
    @Test
    public void ThirdPartyIDTokenClaims_none_thirdPartyIdTokenClaims_audClaim() throws Exception {

        String appName = "audClaimInIdToken";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedIdTokenFalse);
        expectations = setTestParmExpectations(expectations, noThirdPartyParm, thirdPartyPropagatedIdTokenFalse);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName);

        genericRP(_testName, wc, updatedTestSettings, steps, expectations);
    }

}
