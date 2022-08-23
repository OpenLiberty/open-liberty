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
package com.ibm.ws.security.common.claimPropagation.fat.CommonTests;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.fat.common.utils.MySkipRule;
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
    protected static final Boolean thirdPartyPropagatedTestClaimsInIdTokenTrue = true;
    protected static final Boolean thirdPartyPropagatedTestClaimsInIdTokenFalse = false;
    protected static final Boolean thirdPartyPropagatedGroupIdsInIdTokenTrue = true;
    protected static final Boolean thirdPartyPropagatedGroupIdsInIdTokenFalse = false;
    protected static final String[] noThirdPartyParm = null;
    protected static final String[] oneThirdPartyParm = new String[] { "testProp1" };
    protected static final String[] allUniqueThirdPartyParms = new String[] { "testProp1", "testProp2" };
    protected static final String[] allUniqueIntermedParms = new String[] { "testProp3", "testProp4" };
    protected static final String[] allThirdPartyParms = new String[] { "testProp1", "testProp2", "testProp5" };
    protected static final String[] allIntermedParms = new String[] { "testProp3", "testProp4", "testProp5" };
    protected static final String[] allSharedParms = new String[] { "testProp5" };

    protected static final String socialFlow = "Social_Flow";

    protected static String repeatAction = null;

    // create vars that extending method can set to allow use of test cases for oidc, and maybe some day saml
    protected static String[] steps = null;
    protected static String loginStep = null;

    protected static String OPServerName = "com.ibm.ws.security.claimPropagation_fat.op";
    protected static String ExternalOPServerName = "com.ibm.ws.security.claimPropagation_fat.op.external";
    protected static String RPServerName = "com.ibm.ws.security.claimPropagation_fat.rp";
    protected static String SocialServerName = "com.ibm.ws.security.claimPropagation_fat.social";
    public static TestServer testExternalOPServer = null;
    protected static Map<String, String> externalProps = new HashMap<String, String>();
    protected static Map<String, String> intermedProps = new HashMap<String, String>();

    @Rule
    public static final TestRule conditIgnoreRule = new ConditionalIgnoreRule();

    public static class skipIfExternalLDAP extends MySkipRule {
        @Override
        public Boolean callSpecificCheck() {

            try {
                String ldapName = testExternalOPServer.getBootstrapProperty("ldap.server.4.name");
                Log.info(thisClass, "skipIfExternalLDAP", ldapName);
                if (!ldapName.equals("localhost")) {
                    testSkipped();
                    return true;
                }
                return false;
            } catch (Exception e) {
                testSkipped();
                return true;
            }
        }

    }

    private void setParmValues(String user) {
        // Intermediate OP server properties
        intermedProps.put("testProp1", "N/A");
        intermedProps.put("testProp2", "N/A");
        intermedProps.put("testProp3", user);
        intermedProps.put("testProp4", user);
        intermedProps.put("testProp5", user);

        // External OP server properties
        externalProps.put("testProp1", "1 919 555 5555");
        externalProps.put("testProp2", user + "@ibm.com");
        externalProps.put("testProp3", "N/A");
        externalProps.put("testProp4", "N/A");
        externalProps.put("testProp5", "1 919 555 5555");
    }

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
        expectations = vData.addExpectation(expectations, loginStep, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "\"" + claim + "\" was either not found or was not correct in the subject", null, ".*" + claim + ".*" + value);
        if (testSettings.getFlowType().equals(socialFlow)) {
            expectations = vData.addExpectation(expectations, loginStep, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "\"" + claim + "\" was either not found or was not correct in the subject", null, ".*UserInfo: claims: name:.*" + claim + ".*" + value);
        }

        return expectations;
    }

    private List<validationData> addShouldMatch_idToken(List<validationData> expectations, String claim, String value) throws Exception {

        expectations = vData.addExpectation(expectations, loginStep, Constants.RESPONSE_ID_TOKEN, Constants.STRING_MATCHES, "\"" + claim + "\" was either not found or was not correct in the id_token", claim, value);
        expectations = vData.addExpectation(expectations, loginStep, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "\"" + claim + "\" was either not found or was not correct in the subject", null, ".*" + claim + ".*" + value);

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
        if (claim.equals("groupIds")) {
            expectations = vData.addExpectation(expectations, loginStep, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "\"" + claim + "\" was found in the subject with a value that it should NOT have", null, claim + "=[]");
        } else {
            expectations = vData.addExpectation(expectations, loginStep, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "\"" + claim + "\" was found in the subject and should NOT have been", null, claim);
        }
        return expectations;
    }

    /**
     * Set expectations for groupIds.
     * The user that is used for most of the tests is a member of a group and the external OP will ALWAYS return that group info.
     * The intermed OP may or may not include that claim based on:
     * 1) the setting of thirdPartyIDTokenClaims in the OIDC config
     * 2) grant type (auth_code vs implicit)
     * If the intermed OP will use its own group info, the inclusion of the intermed group value will depend on that OP using a
     * registry or using a registry with the test user
     *
     * @param expectations
     *            - existing expectations that we'll add to
     * @param thirdPartyPropagatedIdToken
     *            - flag indicating if the external OP's claims will be propagated (means the external OP returns the claim & the
     *            intermed OP includes gorupIds in thirdPartyIDTokenClaims)
     * @return The updated set of expecations
     * @throws Exception
     */
    private List<validationData> setGroupIdsExpectations(List<validationData> expectations, boolean thirdPartyPropagatedIdToken) throws Exception {

        Log.info(thisClass, "setGroupIdsExpectations", "Propagate Id Token: " + thirdPartyPropagatedIdToken);

        // if groupIds is in the third party token, that will override the local value
        if (thirdPartyPropagatedIdToken || repeatAction.contains(Constants.IMPLICIT_GRANT_TYPE)) {
            expectations = addShouldMatch_idToken(expectations, "groupIds", "group1");
        } else {
            if (repeatAction.contains(WithRegistry_withUser)) {
                expectations = addShouldMatch_idToken(expectations, "groupIds", "groupA");
            } else {
                expectations = addShouldNotContain_idToken(expectations, "groupIds");
            }
        }

        return expectations;
    }

    /**
     * Set expectations for groupIds for cases where the user is NOT in a group on the thrid party OP
     * The user that is used for the tests that use this method are NOT in a group in the registry of the third party OP
     * and therefore the ID token returned will NOT have groupIds set.
     * If the intermed OP will use its own group info when it has the user and group(s) in its own registry and the flow is NOT
     * implicit
     *
     * @param expectations
     *            - existing expectations that we'll add to
     * @return The updated set of expectations
     * @throws Exception
     */
    private List<validationData> setGroupIdsExpectations(List<validationData> expectations) throws Exception {
        if (repeatAction.contains(WithRegistry_withUser) && !repeatAction.contains(Constants.IMPLICIT_GRANT_TYPE)) {
            expectations = addShouldMatch_idToken(expectations, "groupIds", "groupA");
            expectations = addShouldMatch_idToken(expectations, "groupIds", "groupB");
        } else {
            expectations = addShouldNotContain_idToken(expectations, "groupIds");
        }
        return expectations;

    }

    /**
     * Set expectations for claims (testParms) that are to be included in the ID token when both the external OP and the intermed
     * OP generate claims with the same name.
     * if the external OP returns the claim, and the intermed OP propagates it, the ID token will contain the claim with the value
     * from the 2rd party OP
     * If the external OP does not return the claim, or the intermed OP does not propagate the claim, we expect the claim to
     * contain the intermed OP value when the intermed OP's registry contains the user that will populate the claim, otherwise,
     * expect the claim to not exist in the ID Token.
     * This method will call the method that sets the expectations for claims that are exclusive to the third party OP or intermed
     * OP after setting the expectations for shared claims
     *
     * @param expectationsexisting
     *            - expectations that we'll add to
     * @param thirdPartyParms
     *            - all 3rd party OP claims that should be found in the ID Token
     * @param thirdPartyPropagatedIdToken
     *            - flag indicating if the external OP's claims will be propagated (means the external OP returns the claim & the
     *            intermed OP includes the claim in thirdPartyIDTokenClaims)
     * @return The updated set of expectations
     * @throws Exception
     */
    private List<validationData> setTestParmExpectationsWithConflict(List<validationData> expectations, String[] thirdPartyParms, boolean thirdPartyPropagatedIdToken) throws Exception {

        for (String parm : allSharedParms) {
            if (thirdPartyPropagatedIdToken) {
                expectations = addShouldContain_idToken(expectations, parm, externalProps.get(parm));
            } else {
                if (repeatAction.contains(WithRegistry_withUser)) {
                    expectations = addShouldContain_idToken(expectations, parm, intermedProps.get(parm));
                } else {
                    expectations = addShouldNotContain_idToken(expectations, parm);
                }
            }
        }
        return setTestParmExpectations(expectations, thirdPartyParms, thirdPartyPropagatedIdToken, false);

    }

    /**
     * cases calling this method should not expect the shared claim
     *
     * @param expectations
     *            - expectations that we'll add to
     * @param thirdPartyParms
     * @param thirdPartyPropagatedIdToken
     * @return
     * @throws Exception
     */
    private List<validationData> setTestParmExpectations(List<validationData> expectations, String[] thirdPartyParms, boolean thirdPartyPropagatedIdToken) throws Exception {
        return setTestParmExpectations(expectations, thirdPartyParms, thirdPartyPropagatedIdToken, true);
    }

    //TODO
    /**
     * Create expectations for (non groupIds) test claims.
     * Ensure that the claims created by the 3rd party OP ARE included when they are in the exteral OP's ID Token and then
     * propagated by the intermed OP
     * Ensure that the claims created by the 3rd party OP ARE NOT included when they are either NOT in the exteral OP's ID Token
     * or not propagated by the intermed OP
     * Ensure that the claims created by the intermed OP ARE included when they are created because the local user exists
     * Ensure that the claims created by the intermed OP ARE NOT included when they can't be created because the local user does
     * not exist
     * Ensure that the shared claim does not exist if the setTestProp5DoesNotExist flag is true
     *
     * @param expectations
     *            - expectations that we'll add to
     * @param parmList
     *            - the third party claims that are included in the 3rd party ID Token and propagated by the intermed OP
     * @param thirdPartyPropagatedIdToken
     *            -
     * @param setTestProp5DoesNotExist
     *            - flag indicating that the prop that could be shared between the two OPs will not exist for this test case -
     *            create an expectation that ensures that it does not exist
     * @return The updated set of expectations
     * @throws Exception
     */
    private List<validationData> setTestParmExpectations(List<validationData> expectations, String[] parmList, boolean thirdPartyPropagatedIdToken, boolean setTestProp5DoesNotExist) throws Exception {
        // set expectations for all 3rd party only claims - claims that both OPs can return are handled separately
        for (String parm : allUniqueThirdPartyParms) {
            if (parmList != null && validationTools.isInList(parmList, parm) && thirdPartyPropagatedIdToken) {
                expectations = addShouldContain_idToken(expectations, parm, externalProps.get(parm));
            } else {
                expectations = addShouldNotContain_idToken(expectations, parm);
            }
        }

        // add checks for parms defined by the intermediate OP (no propagation involved)
        for (String parm : allUniqueIntermedParms) {

            if (repeatAction.contains(WithRegistry_withUser)) {
                expectations = addShouldContain_idToken(expectations, parm, intermedProps.get(parm));
            } else {
                expectations = addShouldNotContain_idToken(expectations, parm);
            }

        }

        if (setTestProp5DoesNotExist) {
            expectations = addShouldNotContain_idToken(expectations, "testProp5");
        }
        return expectations;
    }

    /**
     * Set expecatations to ensure the proper iss value
     *
     * @param expectations
     *            - expectations that we'll add to
     * @param provider
     *            - the provider value we expect
     * @returnThe updated set of expectations
     * @throws Exception
     */
    private List<validationData> setIssExpectations(List<validationData> expectations, String provider) throws Exception {

        expectations = addShouldContain_idToken(expectations, "iss", testOPServer.getHttpsString() + "/oidc/endpoint/OP_" + provider);

        return expectations;
    }

    private String getRealmName() throws Exception {
        String realmName = "BasicRealm";

        if (repeatAction.contains(WithoutRegistry)) {
            realmName = "WIMRegistry";
        }

        if (repeatAction.contains(Constants.IMPLICIT_GRANT_TYPE)) {
            realmName = "BasicRealm";
        }

        return realmName;
    }

    /**
     * Set expecatations to ensure the proper realm name value
     *
     * @param expectations
     *            - expectations that we'll add to
     * @returnThe updated set of expectations
     * @throws Exception
     */
    private List<validationData> setRealmNameExpectations(List<validationData> expectations) throws Exception {

        expectations = addShouldContain_idToken(expectations, "realmName", getRealmName());

        return expectations;
    }

    /**
     * Set expecatations to ensure the proper realm name value
     *
     * @param expectations
     *            - expectations that we'll add to
     * @param appName
     *            - aud contains part of the appname, so the passed appName can be use to create the aud to valdiate
     * @returnThe updated set of expectations
     * @throws Exception
     */
    private List<validationData> setAudExpectations(List<validationData> expectations, String appName) throws Exception {

        // tests are set up to use clients and providers whose names are based off of the appNames
        String clientId = appName.replace("_3rdPartyDoesNotPropagate", "").replace("_3rdPartyPropagates", "");
        expectations = addShouldContain_idToken(expectations, "aud", clientId);

        return expectations;
    }

    /***************************************************** Tests *****************************************************/

    /**
     * All of the tests are run with the intermed OP configured in 3 different ways
     * 1) with a registry that contains the user that we test with
     * 2) with a registry that does not contain the user that we test with
     * 3) without a registry
     * Each of these different variations are also run using either the auth_code or implicit flows.
     * The flow and registry setup will affect what we expect in the resulting ID token.
     * The set<...>Expectation methods that are called will set their specific expectations appropriately
     */

    /**
     * The Third party OP will populate the idToken that it creates with the groupIds claim (by default)
     * The Third party OP does not populate the idToken that it creates with the any extra claims
     * The intermediate OP does not have thirdPartyIDTokenClaims configured.
     * No third party OP claims will be included in the idToken produced by the intermediate OP.
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
        setParmValues(updatedTestSettings.getUserName());

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedGroupIdsInIdTokenFalse);
        expectations = setTestParmExpectations(expectations, noThirdPartyParm, thirdPartyPropagatedTestClaimsInIdTokenFalse);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName);

        genericRP(_testName, wc, updatedTestSettings, steps, expectations);
    }

    /**
     * The Third party OP will populate the idToken that it creates with the groupIds claim (by default)
     * The Third party OP does not populate the idToken that it creates with the any extra claims
     * The intermediate OP does have thirdPartyIDTokenClaims configured with "groupIds".
     * Only the third party OP groupIds claim will be included in the idToken produced by the intermediate OP.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void ThirdPartyIDTokenClaims_groupIds_3rdPartyDoesNotPropagate() throws Exception {

        String appName = "propagateGroupIdsIdTokenClaims_3rdPartyDoesNotPropagate";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));
        setParmValues(updatedTestSettings.getUserName());

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedGroupIdsInIdTokenTrue);
        expectations = setTestParmExpectations(expectations, allUniqueIntermedParms, thirdPartyPropagatedTestClaimsInIdTokenFalse);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName);
        genericRP(_testName, wc, updatedTestSettings, steps, expectations);
    }

    /**
     * The Third party OP can not populate the idToken that it creates with the groupIds claim because the user that this test
     * case uses does not belong to a group.
     * The Third party OP does not populate the idToken that it creates with the any extra claims
     * The intermediate OP does have thirdPartyIDTokenClaims configured with "groupIds".
     * No third party OP claims will be included in the idToken produced by the intermediate OP.
     * (groupIds, testProps3, testProps4 will exist in the idToken based on the registry used by the intermed OP in the test
     * instance)
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void ThirdPartyIDTokenClaims_groupIds_3rdPartyDoesNotPropagate_3rdPartyUserNotInGroup() throws Exception {

        String appName = "propagateGroupIdsIdTokenClaims_3rdPartyDoesNotPropagate";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));
        updatedTestSettings.setUserName("LDAPUser5");
        updatedTestSettings.setUserPassword("security");
        setParmValues(updatedTestSettings.getUserName());

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations);
        expectations = setTestParmExpectations(expectations, allUniqueIntermedParms, thirdPartyPropagatedTestClaimsInIdTokenFalse);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName);
        genericRP(_testName, wc, updatedTestSettings, steps, expectations);
    }

    /**
     * The Third party OP will populate the idToken that it creates with the groupIds claim (by default)
     * The Third party OP does not populate the idToken that it creates with the any extra claims
     * The intermediate OP does have thirdPartyIDTokenClaims configured with "testProp1".
     * NO third party claims will be included in the idToken produced by the intermediate OP.
     * (groupIds, testProps3, testProps4 will exist in the idToken based on the registry used by the intermed OP in the test
     * instance)
     *
     * @throws Exception
     */
    @Test
    public void ThirdPartyIDTokenClaims_1TestClaim_3rdPartyDoesNotPropagate() throws Exception {

        String appName = "propagate1TestClaimIdTokenClaims_3rdPartyDoesNotPropagate";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));
        setParmValues(updatedTestSettings.getUserName());

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedGroupIdsInIdTokenFalse);
        expectations = setTestParmExpectations(expectations, noThirdPartyParm, thirdPartyPropagatedTestClaimsInIdTokenFalse);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName);
        genericRP(_testName, wc, updatedTestSettings, steps, expectations);
    }

    /**
     * The Third party OP will populate the idToken that it creates with the groupIds claim (by default)
     * The Third party OP does not populate the idToken that it creates with the any extra claims
     * The intermediate OP does have thirdPartyIDTokenClaims configured with "testProp1,testProp2".
     * NO third party claims will be included in the idToken produced by the intermediate OP.
     * (groupIds, testProps3, testProps4 will exist in the idToken based on the registry used by the intermed OP in the test
     * instance)
     *
     * @throws Exception
     */
    @Test
    public void ThirdPartyIDTokenClaims_2TestClaims_3rdPartyDoesNotPropagate() throws Exception {

        String appName = "propagate2TestClaimsIdTokenClaims_3rdPartyDoesNotPropagate";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));
        setParmValues(updatedTestSettings.getUserName());

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedGroupIdsInIdTokenFalse);
        expectations = setTestParmExpectations(expectations, noThirdPartyParm, thirdPartyPropagatedTestClaimsInIdTokenFalse);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName);
        genericRP(_testName, wc, updatedTestSettings, steps, expectations);
    }

    /**
     * The Third party OP will populate the idToken that it creates with the groupIds claim (by default)
     * The Third party OP does not populate the idToken that it creates with the any extra claims
     * The intermediate OP does have thirdPartyIDTokenClaims configured with "testProp1,testProp2,testProp5".
     * NO third party claims will be included in the idToken produced by the intermediate OP.
     * (groupIds, testProps3, testProps4 will exist in the idToken based on the registry used by the intermed OP in the test
     * instance)
     *
     * @throws Exception
     */
    @Test
    public void ThirdPartyIDTokenClaims_conflictingTestClaims_3rdPartyDoesNotPropagate() throws Exception {

        String appName = "propagate3TestClaimsIdTokenClaims_3rdPartyDoesNotPropagate";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));
        setParmValues(updatedTestSettings.getUserName());

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedGroupIdsInIdTokenFalse);
        expectations = setTestParmExpectationsWithConflict(expectations, noThirdPartyParm, thirdPartyPropagatedTestClaimsInIdTokenFalse);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName);
        genericRP(_testName, wc, updatedTestSettings, steps, expectations);
    }

    /**
     * The Third party OP will populate the idToken that it creates with the groupIds claim (by default)
     * The Third party OP populate the idToken with all extra test claims
     * The intermediate OP does not have thirdPartyIDTokenClaims configured.
     * No third party OP claims will be included in the idToken produced by the intermediate OP.
     * (groupIds, testProps3, testProps4 will exist in the idToken based on the registry used by the intermed OP in the test
     * instance)
     *
     * @throws Exception
     */
    @Test
    public void ThirdPartyIDTokenClaims_none_3rdPartyPropagates() throws Exception {

        String appName = "noExtraClaims_3rdPartyPropagates";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));
        setParmValues(updatedTestSettings.getUserName());

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedGroupIdsInIdTokenFalse);
        expectations = setTestParmExpectations(expectations, noThirdPartyParm, thirdPartyPropagatedTestClaimsInIdTokenFalse);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName);

        genericRP(_testName, wc, updatedTestSettings, steps, expectations);
    }

    /**
     * The Third party OP will populate the idToken that it creates with the groupIds claim (by default)
     * The Third party OP populate the idToken with all extra test claims
     * The intermediate OP does have thirdPartyIDTokenClaims configured with "groupIds".
     * Only the third party OP groupIds claim will be included in the idToken produced by the intermediate OP.
     * (testProps3, testProps4 will exist in the idToken based on the registry used by the intermed OP in the test instance)
     *
     * @throws Exception
     */
    @Test
    public void ThirdPartyIDTokenClaims_groupIds_3rdPartyPropagates() throws Exception {

        String appName = "propagateGroupIdsIdTokenClaims_3rdPartyPropagates";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));
        setParmValues(updatedTestSettings.getUserName());

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedGroupIdsInIdTokenTrue);
        expectations = setTestParmExpectations(expectations, noThirdPartyParm, thirdPartyPropagatedTestClaimsInIdTokenFalse);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName);

        genericRP(_testName, wc, updatedTestSettings, steps, expectations);
    }

    /**
     * The Third party OP can not populate the idToken that it creates with the groupIds claim because the user that this test
     * case uses does not belong to a group.
     * The Third party OP populate the idToken with all extra test claims
     * The intermediate OP does have thirdPartyIDTokenClaims configured with "groupIds".
     * No third party OP claims will be included in the idToken produced by the intermediate OP.
     * (groupIds, testProps3, testProps4 will exist in the idToken based on the registry used by the intermed OP in the test
     * instance)
     *
     * @throws Exception
     */
    @Test
    public void ThirdPartyIDTokenClaims_groupIds_3rdPartyPropagates_3rdPartyUserNotInGroup() throws Exception {

        String appName = "propagateGroupIdsIdTokenClaims_3rdPartyPropagates";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));
        updatedTestSettings.setUserName("LDAPUser5");
        updatedTestSettings.setUserPassword("security");
        setParmValues(updatedTestSettings.getUserName());

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations);
        expectations = setTestParmExpectations(expectations, noThirdPartyParm, thirdPartyPropagatedTestClaimsInIdTokenFalse);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName);

        genericRP(_testName, wc, updatedTestSettings, steps, expectations);
    }

    /**
     * The Third party OP will populate the idToken that it creates with the groupIds claim (by default)
     * The Third party OP populate the idToken with all extra test claims
     * The intermediate OP does have thirdPartyIDTokenClaims configured with "testProps1".
     * Only the third party OP testProp1 claim will be included in the idToken produced by the intermediate OP.
     * (groupIds, testProps3, testProps4 will exist in the idToken based on the registry used by the intermed OP in the test
     * instance)
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalLDAP.class)
    @Test
    public void ThirdPartyIDTokenClaims_1TestClaim_3rdPartyPropagates() throws Exception {

        String appName = "propagate1TestClaimIdTokenClaims_3rdPartyPropagates";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));
        setParmValues(updatedTestSettings.getUserName());

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedGroupIdsInIdTokenFalse);
        expectations = setTestParmExpectations(expectations, oneThirdPartyParm, thirdPartyPropagatedTestClaimsInIdTokenTrue);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName);

        genericRP(_testName, wc, updatedTestSettings, steps, expectations);
    }

    /**
     * The Third party OP will populate the idToken that it creates with the groupIds claim (by default)
     * The Third party OP populate the idToken with all extra test claims
     * The intermediate OP does have thirdPartyIDTokenClaims configured with "testProps1,testProp2".
     * The third party OP testProp1 and testProp2 claims will be included in the idToken produced by the intermediate OP.
     * (groupIds, testProps3, testProps4 will exist in the idToken based on the registry used by the intermed OP in the test
     * instance)
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalLDAP.class)
    @Test
    public void ThirdPartyIDTokenClaims_2TestClaims_3rdPartyPropagates() throws Exception {

        String appName = "propagate2TestClaimsIdTokenClaims_3rdPartyPropagates";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));
        setParmValues(updatedTestSettings.getUserName());

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedGroupIdsInIdTokenFalse);
        expectations = setTestParmExpectations(expectations, allUniqueThirdPartyParms, thirdPartyPropagatedTestClaimsInIdTokenTrue);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName);

        genericRP(_testName, wc, updatedTestSettings, steps, expectations);
    }

    /**
     * The Third party OP will populate the idToken that it creates with the groupIds claim (by default)
     * The Third party OP populate the idToken with all extra test claims
     * The intermediate OP does have thirdPartyIDTokenClaims configured with "testProps1,testProp2, testProp5".
     * Tthe third party OP testProp1, testProp2 and testProp5 claims will be included in the idToken produced by the intermediate
     * OP.
     * (groupIds, testProps3, testProps4 will exist in the idToken based on the registry used by the intermed OP in the test
     * instance)
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalLDAP.class)
    @Test
    public void ThirdPartyIDTokenClaims_ConflictingTestClaims_3rdPartyPropagates() throws Exception {

        String appName = "propagate3TestClaimsIdTokenClaims_3rdPartyPropagates";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));
        setParmValues(updatedTestSettings.getUserName());

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedGroupIdsInIdTokenFalse);
        expectations = setTestParmExpectationsWithConflict(expectations, allThirdPartyParms, thirdPartyPropagatedTestClaimsInIdTokenTrue);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName);

        genericRP(_testName, wc, updatedTestSettings, steps, expectations);
    }

    /**
     * Test that the iss claim that is included in the intermed OP id token is the value from the external OP
     * even when no extra claims are propagated from the external OP
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void ThirdPartyIDTokenClaims_none_thirdPartyIdTokenClaims_issClaim() throws Exception {

        String appName = "issClaimInIdToken";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));
        setParmValues(updatedTestSettings.getUserName());

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedGroupIdsInIdTokenFalse);
        expectations = setTestParmExpectations(expectations, noThirdPartyParm, thirdPartyPropagatedTestClaimsInIdTokenFalse);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName);

        genericRP(_testName, wc, updatedTestSettings, steps, expectations);
    }

    /**
     * Test that the aud claim that is included in the intermed OP id token is the value from the external OP
     * even when no extra claims are propagated from the external OP
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void ThirdPartyIDTokenClaims_none_thirdPartyIdTokenClaims_audClaim() throws Exception {

        String appName = "audClaimInIdToken";
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));
        setParmValues(updatedTestSettings.getUserName());

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = setGroupIdsExpectations(expectations, thirdPartyPropagatedGroupIdsInIdTokenFalse);
        expectations = setTestParmExpectations(expectations, noThirdPartyParm, thirdPartyPropagatedTestClaimsInIdTokenFalse);
        expectations = setRealmNameExpectations(expectations);
        expectations = setIssExpectations(expectations, appName);
        expectations = setAudExpectations(expectations, appName);

        genericRP(_testName, wc, updatedTestSettings, steps, expectations);
    }

}
