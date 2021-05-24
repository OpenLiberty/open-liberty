/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.fat.jaxrs.CommonTests;

import java.util.List;

import org.junit.Test;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;

/**
 * This is the test class that contains basic OpenID Connect and OAuth tests for Relying Party (RP)
 * This class focuses on testing the RP code where it gets the token(token to user) mapping information from an SPI that is
 * bundled with a user feature.
 * Test server configuration should include the user feature.
 * When a valid(non-null, non-empty values) user information is returned from the SPI, authenticated subject will be created with
 * this information.
 * If the user information(user name, realm name, unique name and group) from the SPI is not valid, then the RP will continue with
 * the default configuration(where we'll get the user name using the userIdentifier claim,
 * get the realm using the realmName or realmIdentifier claim or "iss" claim, and get the unique name using the
 * uniqueUserIdentifier claim etc.).
 * 
 * This contains both positive and negative tests.
 * 
 * mapIdentityToRegsitryUser - false
 * userIdentifier (claim)
 * groupIdentifier (claim)
 * realmName (String)
 * realmIdentifier (claim)
 * uniqueUserIdentifier (claim)
 * 
 * 
 * 
 * This test depends upon the following users
 * testuser - not in any groups
 **/

public class TokenMappingResolverGenericTest extends CommonTest {

    protected static String targetProvider = null;
    protected static String targetPort = null;
    protected static String targetType = null;
    protected static String targetEndpoint = null;
    protected static String[] goodActions = null;
    protected static String OPServerName = "com.ibm.ws.security.openidconnect.client-1.0_fat.jaxrs.op";
    protected static String RPServerName = "com.ibm.ws.security.openidconnect.client-1.0_fat.jaxrs.rp";

    public static String[] test_GOOD_LOGIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT;
    public static String[] test_GOOD_POST_LOGIN_ACTIONS = Constants.GOOD_OIDC_POST_LOGIN_ACTIONS_SKIP_CONSENT;
    public static String[] test_GOOD_LOGIN_AGAIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_AGAIN_ACTIONS;
    public static String[] test_LOGIN_PAGE_ONLY = Constants.GET_LOGIN_PAGE_ONLY;
    public static String test_FinalAction = Constants.LOGIN_USER;

    /*
     * 
     * This test exercises the flow of the RP function where user name will be resolved by the SPI
     * 
     * 
     * user id specified by the configuration - "sub" claim = "testuser"
     * user id returned by the spi = spi_resolved_user_for_rp
     * RP should use the "spi_resolved_user_for_rp" to create the authenticated subject and the realm and group, unique id values
     * are from the configuration
     * mapIdentitytoRegistryUser is set to true
     * <P> Expected Results:
     * <OL>
     * Subject created with the following:
     * <LI> SecurityName - spi_resolved_user_for_rp
     * <LI> RealmName - BasicRPRealm
     * </OL>
     */

    @Test
    public void TokenMappingSpiUserFeature2ServerTests_onlyUserfromTheSpi() throws Exception {

        String securityNameSearchString = "WSCredential SecurityName=" + "spi_resolved_user_for_rp";
        //String realmSearchString = "WSCredential RealmName=" + "spi_resolved_realm";
        String realmSearchString = "WSCredential RealmName=" + "BasicRPRealm";

        testSettings.setTestURL(testRPServer.getHttpsString() + "/helloworld/rest/helloworld_userMap");
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");

        List<validationData> expectations = setGoodHelloWorldExpectations(updatedTestSettings);
        expectations = vData.addExpectation(expectations,
                test_FinalAction,
                Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Did not receive the expected security name settings.",
                null, securityNameSearchString);
        expectations = vData.addExpectation(expectations,
                test_FinalAction,
                Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Did not receive the expected realm name settings.", null,
                realmSearchString);
        WebConversation wc = new WebConversation();
        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /*
     * 
     * This test exercises the flow of the RP function where user name, realm and groups will be resolved by the SPI
     * The SPI implementation returns user, realm, group, unique ID.
     * user name should be the claim specified by the userIdentifier attribute
     * unique id should be the claim specified by the uniqueUserIdentifier attribute
     * realm should be specified by the realmName attribute
     * user id returned by the spi = "spi_resolved_user_for_rp"
     * mapIdentitytoRegistryUser is set to false.
     * So the realm, group and unique id values are the SPI resolved values.
     * RP should use this user "spi_resolved_user_for_rp" to create the authenticated subject in this case.
     * <P> Expected Results:
     * <OL>
     * Subject created with the following:
     * <LI> SecurityName - spi_resolved_user_for_rp
     * <LI> RealmName - spi_resolved_realm_for_rp
     * </OL>
     */

    @Test
    public void TokenMappingSpiUserFeature2ServerTests_allValues_fromTheSpi() throws Exception {

        String securityNameSearchString = "WSCredential SecurityName=" + "spi_resolved_user_for_rp";
        String realmSearchString = "WSCredential RealmName=" + "spi_resolved_realm_for_rp";

        testSettings.setTestURL(testRPServer.getHttpsString() + "/helloworld/rest/helloworld_noUserMapToReg");
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");

        List<validationData> expectations = setGoodHelloWorldExpectations(updatedTestSettings);
        expectations = vData.addExpectation(expectations,
                test_FinalAction,
                Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Did not receive the expected security name settings.",
                null, securityNameSearchString);
        expectations = vData.addExpectation(expectations,
                test_FinalAction,
                Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Did not receive the expected realm name settings.", null,
                realmSearchString);
        WebConversation wc = new WebConversation();
        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    public List<validationData> setGoodHelloWorldExpectations(TestSettings settings) throws Exception {
        return setGoodHelloWorldExpectations(null, settings);
    }

    public List<validationData> setGoodHelloWorldExpectations(List<validationData> expectations, TestSettings settings) throws Exception {

        if (expectations == null) {
            expectations = vData.addSuccessStatusCodes();
        }

        if (settings.getWhere() == Constants.HEADER) {
            //expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not get to the HelloWorld App", null, Constants.HELLOWORLD_WITH_HEADER);
            expectations = vData.addExpectation(expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not get to the HelloWorld App", null, Constants.HELLOWORLD_WITH_HEADER);
        } else {
            //expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not get to the HelloWorld App", null, Constants.HELLOWORLD_WITH_PARM);
            expectations = vData.addExpectation(expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not get to the HelloWorld App", null, Constants.HELLOWORLD_MSG);
        }

        return expectations;

    }

}
