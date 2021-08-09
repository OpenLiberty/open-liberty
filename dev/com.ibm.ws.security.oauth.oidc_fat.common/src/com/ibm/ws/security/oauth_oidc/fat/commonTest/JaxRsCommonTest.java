/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;

import com.ibm.websphere.simplicity.log.Log;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

public class JaxRsCommonTest extends CommonTest {

    private final static Class<?> thisClass = JaxRsCommonTest.class;

    /**
     * Common access token can be used to avoid tests having to go through the entire genericOP flow before calling the protected
     * resource
     */
    protected static String commonPropagationToken = null;
    private static long propagationTokenCreationDate = 0;
    private static long testPropagationTokenLifetimeSeconds = 60;

    protected static boolean defaultUseLdap = useLdap;

    protected static String[] GET_TOKEN_ACTIONS = Constants.BASIC_AUTHENTICATION_ACTIONS;
    protected static String[] GET_TOKEN_AND_INVOKE_APP_ACTIONs = Constants.BASIC_PROTECTED_RESOURCE_ACTIONS;
    protected static String LOGIN_USER = Constants.LOGIN_USER;
    protected static String PERFORM_LOGIN = Constants.PERFORM_LOGIN;
    protected static String[] RS_PROTECTED_RESOURCE_ACTIONS = Constants.BASIC_RS_PROTECTED_RESOURCE_ACTIONS;
    protected static String[] RS_END_TO_END_PROTECTED_RESOURCE_ACTIONS = Constants.BASIC_PROTECTED_RESOURCE_RS_PROTECTED_RESOURCE_ACTIONS;
    private static HashMap<String, String> jwkValidationMap = null;
    protected static int OIDC_ERROR_1 = 1;

    /**
     * Set up the local ACTION variables based on the server type
     * The defaults are the steps/actions used with oidc/oauth, but some tests
     * can run with those servers, or an ISAM server and for ISAM, the steps/actions
     * will be different - this method lets us override the defaults
     *
     */
    public static void setActionsForServerType() throws Exception {
        setActionsForServerType(Constants.OIDC_OP, Constants.ACCESS_TOKEN_KEY);
    }

    public static void setActionsForServerType(String serverType, String tokenType) throws Exception {

        // actions are
        if (serverType.equals(Constants.ISAM_OP)) {
            GET_TOKEN_ACTIONS = Constants.BASIC_CREATE_JWT_TOKEN_ACTIONS;
            GET_TOKEN_AND_INVOKE_APP_ACTIONs = Constants.BASIC_PROTECTED_RESOURCE_WITH_JWT_ACTIONS;
            RS_PROTECTED_RESOURCE_ACTIONS = Constants.BASIC_JWT_RS_PROTECTED_RESOURCE_ACTIONS;
            LOGIN_USER = Constants.LOGIN_USER;
            PERFORM_LOGIN = Constants.PERFORM_ISAM_LOGIN;
            RS_END_TO_END_PROTECTED_RESOURCE_ACTIONS = Constants.BASIC_PROTECTED_RESOURCE_JWT_RS_PROTECTED_RESOURCE_ACTIONS;
        } else {
            GET_TOKEN_ACTIONS = Constants.BASIC_AUTHENTICATION_ACTIONS;
            RS_PROTECTED_RESOURCE_ACTIONS = Constants.BASIC_RS_PROTECTED_RESOURCE_ACTIONS;
            LOGIN_USER = Constants.LOGIN_USER;
            PERFORM_LOGIN = Constants.PERFORM_LOGIN;
            // when the token  type is JWT, we can't invoke the app without using the RS server, so skip those steps
            if (tokenType.equals(Constants.ACCESS_TOKEN_KEY)) {
                GET_TOKEN_AND_INVOKE_APP_ACTIONs = Constants.BASIC_PROTECTED_RESOURCE_ACTIONS;
                RS_END_TO_END_PROTECTED_RESOURCE_ACTIONS = Constants.BASIC_PROTECTED_RESOURCE_RS_PROTECTED_RESOURCE_ACTIONS;
            } else {
                GET_TOKEN_AND_INVOKE_APP_ACTIONs = Constants.BASIC_AUTHENTICATION_ACTIONS;
                RS_END_TO_END_PROTECTED_RESOURCE_ACTIONS = Constants.BASIC_RS_PROTECTED_RESOURCE_ACTIONS;
            }
        }

    }

    /**
     * Goes through the genericOP flow if the common propagation token should be refreshed or if the common token is null.
     * This is intended to be called by a @Before method in test classes that wish to use this functionality. This allows
     * individual
     * tests that simply need a propagation token to invoke the protected resource to use a valid token that has already obtained.
     * Individual tests then do not need to go through the entire genericOP flow.
     */
    @Override
    public void createCommonPropagationToken() {
        String method = "createCommonPropagationToken";
        if (shouldCommonTokenBeRefreshed() || commonPropagationToken == null) {
            Log.info(thisClass, method, "Obtaining a new common propagation token" + ((commonPropagationToken == null) ? " (common token was null)" : ""));

            try {
                WebResponse response = genericOP(_testName, new WebConversation(), testSettings, Constants.BASIC_AUTHENTICATION_ACTIONS, vData.addSuccessStatusCodes());
                commonPropagationToken = validationTools.getTokenForType(testSettings, response);

                propagationTokenCreationDate = System.currentTimeMillis();
                DateFormat formatter = DateFormat.getTimeInstance(DateFormat.LONG);
                Log.info(thisClass, method, "Common propagation token (" + commonPropagationToken + ") created at " + formatter.format(new Date(propagationTokenCreationDate)) + " and will be refreshed in " + testPropagationTokenLifetimeSeconds + " second(s).");

            } catch (Exception e) {
                Log.error(thisClass, method, e, "Failed to obtain a common propagation token. Tests using the common token may fail.");
            }
        }
    }

    /**
     * Returns true if the difference between the current time and the common token creation time exceeds the lifetime
     * we specify in testAccessTokenLifetimeSeconds.
     *
     * @return
     */
    private boolean shouldCommonTokenBeRefreshed() {
        String method = "shouldCommonTokenBeRefreshed";
        long currentTime = System.currentTimeMillis();
        if (((currentTime - propagationTokenCreationDate) / 1000) > testPropagationTokenLifetimeSeconds) {
            Log.info(thisClass, method, "Common token lifetime has exceeded allowed time; recommend a new token should be created.");
            return true;
        }
        return false;
    }

    public static void setJWKValidationMap(HashMap<String, String> validationEndpoints) {
        jwkValidationMap = new HashMap<String, String>(validationEndpoints);
    }

}
