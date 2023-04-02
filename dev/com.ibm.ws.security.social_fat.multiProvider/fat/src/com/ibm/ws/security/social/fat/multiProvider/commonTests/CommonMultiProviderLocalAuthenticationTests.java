/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
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
package com.ibm.ws.security.social.fat.multiProvider.commonTests;

import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.social.fat.utils.SocialConstants;
import com.ibm.ws.security.social.fat.utils.SocialTestSettings;

public class CommonMultiProviderLocalAuthenticationTests extends CommonMultiProviderTests {

    public static Class<?> thisClass = CommonMultiProviderLocalAuthenticationTests.class;

    public static final String VALID_USERNAME = "testuser";
    public static final String VALID_PASSWORD = "testuserpwd";

    public static final String[] ACTIONS_WITH_LOCAL_AUTHENTICATION = SocialConstants.LIBERTYOP_INVOKE_SOCIAL_LOGIN_WITH_SELECTION_AND_USER_CREDS_ACTIONS;

    /**
     * Sets the Liberty OP test actions, sets the standard Liberty OP test settings, and updates the settings to select the
     * specified provider to perform social login.
     */
    protected SocialTestSettings updateSettingsForSocialLogin(SocialTestSettings socialTestSettings, String configId, String testStyle) throws Exception {
        setActionsForProvider(SocialConstants.LIBERTYOP_PROVIDER, testStyle, true);

        socialTestSettings = updateBasicTestSettings(socialTestSettings);

        socialTestSettings.setProvider(configId);
        socialTestSettings.setProviderButton(getProviderButtonValue(configId));
        socialTestSettings.setProviderButtonDisplay(socialTestSettings.getProviderButtonDisplay());

        return socialTestSettings;
    }

    /**
     * Sets the standard Liberty OP test settings and updates the settings to use a username, password, and realm that should
     * match the registry that is expected to be configured in the server.
     */
    protected SocialTestSettings updateSettingsForLocalAuthentication(SocialTestSettings socialTestSettings) throws Exception {
        socialTestSettings = updateBasicTestSettings(socialTestSettings);

        socialTestSettings.setUserName(VALID_USERNAME);
        socialTestSettings.setUserPassword(VALID_PASSWORD);
        socialTestSettings.setRealm(SocialConstants.BASIC_REALM);

        return socialTestSettings;
    }

    SocialTestSettings updateBasicTestSettings(SocialTestSettings socialTestSettings) throws Exception {
        return updateLibertyOPSettings(socialTestSettings);
    }

    String getProviderButtonValue(String configId) {
        return configId + "1";
    }

    /**
     * Verifies that the social media selection page contains all of the strings that are only output when local authentication
     * is enabled.
     */
    protected List<validationData> setLocalAuthenticationEnabledExpectations(List<validationData> expectations) throws Exception {
        List<String> valuesToLookFor = getLocalAuthenticationPageValuesToCheck();
        for (String value : valuesToLookFor) {
            expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Should have found a " + value + " entry on the page when local authentication is enabled.", null, value);
        }
        return expectations;
    }

    /**
     * Verifies that there is no mention on the social media selection page of any strings that are only output when local
     * authentication is enabled.
     */
    protected List<validationData> setLocalAuthenticationDisabledExpectations(List<validationData> expectations) throws Exception {
        List<String> valuesToLookFor = getLocalAuthenticationPageValuesToCheck();
        for (String value : valuesToLookFor) {
            expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_DOES_NOT_CONTAIN, "Should not have found a " + value + " entry on the page when local authentication is disabled.", null, value);
        }
        return expectations;
    }

    List<String> getLocalAuthenticationPageValuesToCheck() {
        List<String> values = new ArrayList<String>();
        values.add(SocialConstants.J_SECURITY_CHECK);
        values.add(SocialConstants.J_USERNAME);
        values.add(SocialConstants.J_PASSWORD);
        return values;
    }

}
