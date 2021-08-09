/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.social.fat.utils;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;

public class SocialTestSettings extends TestSettings {

    static private final Class<?> thisClass = SocialTestSettings.class;

    protected String headerName = null;
    protected String headerValue = null;
    protected String loginPage = null;
    protected String userId = null;
    protected String authorizeButton = null;
    //    protected Boolean useSelection = false;
    protected String providerButton = null;
    protected String providerButtonDisplay = null;

    // for request parm, can use the existing requestParms

    public SocialTestSettings() {
    }

    public SocialTestSettings(SocialTestSettings settings) {
        setAllValues(settings);
    }

    public void setAllValues(SocialTestSettings settings) {

        super.setAllValues(settings);

        headerName = settings.headerName;
        headerValue = settings.headerValue;
        loginPage = settings.loginPage;
        userId = settings.userId;
        authorizeButton = settings.authorizeButton;
        //        useSelection = settings.useSelection;
        providerButton = settings.providerButton;
        providerButtonDisplay = settings.providerButtonDisplay;

    }

    @Override
    public SocialTestSettings copyTestSettings() {
        return new SocialTestSettings(this);
    }

    public void printSocialTestSettings() {
        String thisMethod = "printTestSettings";

        Log.info(thisClass, thisMethod, "Social Settings: ");

        Log.info(thisClass, thisMethod, "headerName: " + headerName);
        Log.info(thisClass, thisMethod, "headerValue: " + headerValue);
        Log.info(thisClass, thisMethod, "loginPage: " + loginPage);
        Log.info(thisClass, thisMethod, "userId: " + userId);
        Log.info(thisClass, thisMethod, "authorizeButton: " + authorizeButton);
        //        Log.info(thisClass, thisMethod, "useSelection: " + useSelection);
        Log.info(thisClass, thisMethod, "providerButton: " + providerButton);
        Log.info(thisClass, thisMethod, "providerButtonId: " + providerButtonDisplay);

        printTestSettings();
    }

    /**
     *
     * @param settings
     *            current testSettings - get value for JWT from here
     * @param httpStart
     *            prefix Url string (ie: http://localhost:DefaultPort)
     * @param httpsStart
     *            prefix Url string (ie: https://localhost:DefaultSSLPort)
     * @return returns a JwtBuildSettings object with the default values set -
     *         many really are specific to individual tests and will be updated
     *         by the tests
     */
    public void setDefaultSocialSettings(TestSettings settings, String httpStart, String httpsStart) {

        headerName = null;
        headerValue = null;
        loginPage = null;
        userId = null;
        authorizeButton = null;
        //        useSelection = false;
        providerButton = null;
        providerButtonDisplay = null;

    }

    @Override
    public void setHeaderName(String inHeaderName) {
        headerName = inHeaderName;
    }

    @Override
    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderValue(String inHeaderValue) {
        headerValue = inHeaderValue;
    }

    public String getHeaderValue() {
        return headerValue;
    }

    public void setLoginPage(String inLoginPage) {
        loginPage = inLoginPage;
    }

    public String getLoginPage() {
        return loginPage;
    }

    public void setUserId(String inUserId) {
        userId = inUserId;
    }

    public String getUserId() {
        return userId;
    }

    public void setAuthorizeButton(String inAuthorizeButton) {
        authorizeButton = inAuthorizeButton;
    }

    public String getAuthorizeButton() {
        return authorizeButton;
    }

    //    public void setUseSelection(Boolean inUseSelection) {
    //        useSelection = inUseSelection;
    //    }
    //
    //    public Boolean getUseSelection() {
    //        return useSelection;
    //    }

    public void setProviderButton(String inProviderButton) {
        providerButton = inProviderButton;
    }

    public String getProviderButton() {
        return providerButton;
    }

    public void setProviderButtonDisplay(String inProviderButtonDisplay) {
        providerButtonDisplay = inProviderButtonDisplay;
    }

    public String getProviderButtonDisplay() {
        return providerButtonDisplay;
    }
}
