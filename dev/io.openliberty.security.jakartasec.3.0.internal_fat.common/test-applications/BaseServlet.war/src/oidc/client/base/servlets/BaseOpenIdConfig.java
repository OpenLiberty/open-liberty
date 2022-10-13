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
package oidc.client.base.servlets;

import io.openliberty.security.jakartasec.fat.utils.Constants;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;

/**
 * Load the config values from the default config property file.
 * If a value is found for an expected config attribute, load it, if a value is NOT found set this tests suites default value.
 * Test applications can override methods to specify their own "default" values.
 * A value of "UnsetValue" will cause this tooling to return null (this allows the test specified config files to indicate that
 * they don't
 * want even the default config values to be set.)
 */

@Named
@Dependent
public class BaseOpenIdConfig extends MinimumBaseOpenIdConfig {

    public String getRedirectURI() {
        System.out.println("in BaseOpenIdConfig - getRedirectURI");
        String value = "${baseURL}/Callback";
        if (config.containsKey(Constants.REDIRECT_URI)) {
            System.out.println("in BaseOpenIdConfig - getRedirectURI - found value in the config file");
            value = getStringValue(Constants.REDIRECT_URI);
        }
        return value;
    }

    public String getClientId() {

        String value = "client_1";
        if (config.containsKey(Constants.CLIENT_ID)) {
            value = getStringValue(Constants.CLIENT_ID);
        }

        return value;
    }

    public String getClientSecret() {

        String value = "mySharedKeyNowHasToBeLongerStrongerAndMoreSecureAndForHS512EvenLongerToBeStronger";
        if (config.containsKey(Constants.CLIENT_SECRET)) {
            value = getStringValue(Constants.CLIENT_SECRET);
        }

        return value;
    }

    public boolean getUseSessionExpression() {

        //        String value = String.valueOf(true);
        //        if (config.containsKey(Constants.USE_SESSION_EXPRESSION)) {
        //            value = getStringValue(Constants.USE_SESSION_EXPRESSION);
        //        }
        //
        //        return value;
        boolean value = true;
        if (config.containsKey(Constants.USE_SESSION_EXPRESSION)) {
            value = getBooleanValue(Constants.USE_SESSION_EXPRESSION);
        }

        return value;
    }

    //    public boolean getRedirectToOriginalResource() {
    //
    //        if (config.containsKey(Constants.CLIENT_SECRET)) { //fix
    //            return true; //convert real value
    //        }
    //
    //        return false;
    //
    //    }

    public String getCallerNameClaim() {

        System.out.println("in getCallerNameClaim");
        String value = "sub";
        if (config.containsKey(Constants.CALLER_NAME_CLAIM)) {
            value = getStringValue(Constants.CALLER_NAME_CLAIM);
        }

        return value;

    }

    public String getCallerGroupsClaim() {

        String value = "groupIds";
        if (config.containsKey(Constants.CALLER_GROUPS_CLAIM)) {
            value = getStringValue(Constants.CALLER_GROUPS_CLAIM);
        }

        return value;

    }

    public String getBobExperession() {

        return Integer.toString(5);
    }
}
