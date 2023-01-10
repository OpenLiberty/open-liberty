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
package com.ibm.ws.security.social.fat.delegated.common;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.social.fat.utils.SocialCommonTest;
import com.ibm.ws.security.social.fat.utils.SocialConstants;
import com.ibm.ws.security.social.fat.utils.SocialTestSettings;

/**
 *
 **/
public class CommonDelegatedTestClass extends SocialCommonTest {

    public static Class<?> thisClass = CommonDelegatedTestClass.class;

    public static final boolean USES_SELECTION_PAGE = true;
    public static final boolean DOES_NOT_USE_SELECTION_PAGE = false;

    public static final String EXTERNAL_USER = "externaluser";
    public static final String EXTERNAL_USER_PWD = "externaluserpwd";

    public static RSCommonTestTools rsTools = new RSCommonTestTools();

    /********************************************** Helper methods **********************************************/

    protected SocialTestSettings getUpdatedTestSettings(String provider, String style, boolean usesSelectionPage) throws Exception {
        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();

        setActionsForProvider(provider, style, usesSelectionPage);

        updatedSocialTestSettings = updateSettingsBasedOnProvider(updatedSocialTestSettings, provider);

        // Common settings for all providers
        updatedSocialTestSettings.setRealm(SocialConstants.BASIC_REALM);

        return updatedSocialTestSettings;
    }

    protected SocialTestSettings updateSettingsBasedOnProvider(SocialTestSettings settings, String provider) throws Exception {
        if (SocialConstants.LIBERTYOP_PROVIDER.equals(provider)) {
            settings = updateLibertyOPSettings(settings, testRPServer);
        } else if (SocialConstants.FACEBOOK_PROVIDER.equals(provider)) {
            settings = updateFacebookSettings(settings, testRPServer);
        } else if (SocialConstants.LINKEDIN_PROVIDER.equals(provider)) {
            settings = updateLinkedinSettings(settings, testRPServer);
        } else if (SocialConstants.TWITTER_PROVIDER.equals(provider)) {
            settings = updateTwitterSettings(settings, testRPServer);
        }

        settings.setProtectedResource(settings.getProtectedResource() + "_" + provider);

        return settings;
    }

}
