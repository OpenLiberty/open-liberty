/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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
package com.ibm.ws.security.backchannelLogout.fat.utils;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;

import componenttest.custom.junit.runner.RepeatTestFilter;

/**
 * This class supplies methods to set the expected states of token and cookies after logout or end_session is invoked.
 * Things that will affect the state are:
 * using logout or end_session on the OP
 * using http req.logout()
 * are we using a real back channel endpoint or a test app that will just log a message indicating that bcl was called (since the
 * real bcl endpoint doesn't give any indication tht it was called)
 *
 */

public class AfterLogoutStates {

    protected static Class<?> thisClass = AfterLogoutStates.class;

    // by default assume everything exists
    // cookie states
    boolean opCookieExists = true;
    boolean opCookieStillValid = true;
    boolean opCookieMatchesPrevious = true;

    boolean clientCookieExists = true;
    boolean clientCookieStillValid = true;
    boolean clientCookieMatchesPrevious = true;
    boolean client2CookieExists = true;
    boolean client2CookieStillValid = true;
    boolean client2CookieMatchesPrevious = true;

    boolean opJSessionIdExists = true;
    boolean opJSessionIdStillValid = true;
    boolean opJSessionIdMatchesPrevious = true;

    boolean clientJSessionIdExists = true;
    boolean clientJSessionIdStillValid = true;
    boolean clientJSessionIdMatchesPrevious = true;
    boolean client2JSessionIdExists = true;
    boolean client2JSessionIdStillValid = true;
    boolean client2JSessionIdMatchesPrevious = true;

    boolean spCookieExists = true;
    boolean spCookieStillValid = true;
    boolean spCookieMatchesPrevious = true;

    boolean idpCookieExists = true;
    boolean idpCookieStillValid = true;
    boolean idpCookieMatchesPrevious = true;

    // Session access
    boolean appSessionAccess = true;

    // token states
    boolean accessTokenValid = true;
    boolean refreshTokenValid = true;
    // TODO may need SAML token values too

    // config flags
    boolean isUsingOidcNotSocial = true;
    boolean isUsingEndSessionNotHttpLogout = false;
    boolean isUsingLogoutNotHttpLogout = false;
    boolean isUsingHttpLogoutOnRP = false;
    boolean isUsingHttpLogoutOnOP = false;
    boolean isUsingEndSessionWithHttpLogout = false;
    boolean isUsingLogoutWithHttpLogout = false;
    boolean isUsingRevokeNotLogout = false;
    boolean isUsingIbmSecurityLogout = false;
    //    boolean isUsingJwtToken = true;
    boolean isUsingIntrospect = true;
    boolean isUsingInvalidIntrospect = false;

    boolean isUsingSaml = false;

    boolean isTokenLimitExceeded = false;

    public enum BCL_FORM {
        VALID, TEST_BCL, OMITTED, INVALID
    }

    public AfterLogoutStates() {
        isUsingOidcNotSocial = true;
        isUsingEndSessionNotHttpLogout = true;
        //        isUsingJwtToken = true;
    }

    /**
     * Init an AfaterLogoutStates object. This object will set the expected states of cookies and tokens after we run an
     * end_session/logout.
     * What we expect to exist or not exist will be based on the type of logout being done, is the calling test using the real bcl
     * endpoint, or just a test app
     *
     * @param usesRealBCLEndpoint
     *            is the test using the real bcl endpoint or a test app that will just log messages
     * @param flowType
     *            the type of test setup being used - and OP with an oidc or social client, or an OP with SAML and an oidc client
     * @param logoutMethod
     *            the method used to logout - end_session invoked in the OP, http req.logout in the client, or a SAML IDP logout
     * @param sessionEndpoint
     *            when http req.logout is used, either logout or end_session can be invoked on the OP (call made from the logout
     *            test
     *            app)
     */
    //    public AfterLogoutStates(boolean usesRealBCLEndpoint, TestSettings settings, VariationSettings vSettings) {
    //    }

    public AfterLogoutStates(BCL_FORM bcl_form, TestSettings settings, VariationSettings vSettings) {

        isUsingSaml = vSettings.usesSAML;

        if (settings.getFlowType() == null || settings.getFlowType().equals(Constants.RP_FLOW)) {
            Log.info(thisClass, "AfterLogoutStates", "flowType: " + settings.getFlowType() + " (in OIDC path)");
            isUsingOidcNotSocial = true;
        } else {
            Log.info(thisClass, "AfterLogoutStates", "flowType: " + settings.getFlowType() + " (in Social path)");
            isUsingOidcNotSocial = false;
        }

        if (vSettings.logoutMethodTested.equals(Constants.END_SESSION) || vSettings.logoutMethodTested.equals(Constants.SAML_IDP_INITIATED_LOGOUT)) {
            isUsingEndSessionNotHttpLogout = true;
            Log.info(thisClass, "AfterLogoutStates", "1");
        } else {
            if (vSettings.logoutMethodTested.equals(Constants.LOGOUT_ENDPOINT)) {
                isUsingLogoutNotHttpLogout = true;
                Log.info(thisClass, "AfterLogoutStates", "2");
            } else {
                if (vSettings.logoutMethodTested.equals(Constants.IBM_SECURITY_LOGOUT)) {
                    isUsingLogoutNotHttpLogout = false;
                    isUsingIbmSecurityLogout = true;
                    Log.info(thisClass, "AfterLogoutStates", "3");
                } else {
                    if (vSettings.logoutMethodTested.equals(Constants.REVOCATION_ENDPOINT)) {
                        isUsingRevokeNotLogout = true;
                    } else {
                        Log.info(thisClass, "AfterLogoutStates", "reqLogoutServer: " + vSettings.reqLogoutServer);
                        Log.info(thisClass, "AfterLogoutStates", "4");
                        if (vSettings.reqLogoutServer.equals(Constants.OIDC_RP)) {
                            Log.info(thisClass, "AfterLogoutStates", "5");
                            isUsingHttpLogoutOnRP = true;
                            isUsingHttpLogoutOnOP = false;
                        } else {
                            Log.info(thisClass, "AfterLogoutStates", "6");
                            isUsingHttpLogoutOnRP = false;
                            isUsingHttpLogoutOnOP = true;
                        }
                        if (vSettings.sessionLogoutEndpoint != null) {
                            if (vSettings.sessionLogoutEndpoint.equals(Constants.LOGOUT_ENDPOINT)) {
                                isUsingLogoutWithHttpLogout = true;
                            } else {
                                isUsingEndSessionWithHttpLogout = true;
                            }
                        }
                    }
                }
            }
        }

        // set the states of cookies and tokens after logout based on:
        // the type of logout/end_session,
        // whether the BCL endpoint is a real bcl endpoint, or a test app
        setAccessTokenExpectedStateAfterLogout(bcl_form, vSettings);
        setRefreshTokenExpectedStateAfterLogout(bcl_form, vSettings);
        setOPCookiesExpectedStatesAfterLogout(vSettings);
        setClientCookiesExpectedStatesAfterLogout(bcl_form, vSettings);
        setReuseWebClientExpectedStateAfterLogout(bcl_form, vSettings);
        setSPCookiesExpectedStatesAfterLogout(bcl_form, vSettings);
        setIDPCookiesExpectedStatesAfterLogout(bcl_form);

    }

    public void printStates() {

        String thisMethod = "AfterLogoutStates";

        Log.info(thisClass, thisMethod, "opCookieExists: " + opCookieExists);
        Log.info(thisClass, thisMethod, "opCookieStillValid: " + opCookieStillValid);
        Log.info(thisClass, thisMethod, "opCookieMatchesPrevious: " + opCookieMatchesPrevious);

        Log.info(thisClass, thisMethod, "clientCookieExists: " + clientCookieExists);
        Log.info(thisClass, thisMethod, "clientCookieStillValid: " + clientCookieStillValid);
        Log.info(thisClass, thisMethod, "clientCookieMatchesPrevious: " + clientCookieMatchesPrevious);

        Log.info(thisClass, thisMethod, "opJSessionIdExists: " + opJSessionIdExists);
        Log.info(thisClass, thisMethod, "opJSessionIdStillValid: " + opJSessionIdStillValid);
        Log.info(thisClass, thisMethod, "opJSessionIdMatchesPrevious: " + opJSessionIdMatchesPrevious);

        Log.info(thisClass, thisMethod, "clientJSessionIdExists: " + clientJSessionIdExists);
        Log.info(thisClass, thisMethod, "clientJSessionIdStillValid: " + clientJSessionIdStillValid);
        Log.info(thisClass, thisMethod, "clientJSessionIdMatchesPrevious: " + clientJSessionIdMatchesPrevious);

        Log.info(thisClass, thisMethod, "spCookieExists: " + spCookieExists);
        Log.info(thisClass, thisMethod, "spCookieStillValid: " + spCookieStillValid);
        Log.info(thisClass, thisMethod, "spCookieMatchesPrevious: " + spCookieMatchesPrevious);

        Log.info(thisClass, thisMethod, "idpCookieExists: " + idpCookieExists);
        Log.info(thisClass, thisMethod, "idpCookieStillValid: " + idpCookieStillValid);
        Log.info(thisClass, thisMethod, "idpCookieMatchesPrevious: " + idpCookieMatchesPrevious);

        // Session access
        Log.info(thisClass, thisMethod, "appSessionAccess: " + appSessionAccess);

        // token states
        Log.info(thisClass, thisMethod, "accessTokenValid: " + accessTokenValid);
        Log.info(thisClass, thisMethod, "refreshTokenValid: " + refreshTokenValid);
        // TODO may need SAML token values too

        // config flags
        Log.info(thisClass, thisMethod, "isUsingOidcNotSocial: " + isUsingOidcNotSocial);
        Log.info(thisClass, thisMethod, "isUsingEndSessionNotHttpLogout: " + isUsingEndSessionNotHttpLogout);
        Log.info(thisClass, thisMethod, "isUsingLogoutNotHttpLogout: " + isUsingLogoutNotHttpLogout);
        Log.info(thisClass, thisMethod, "isUsingHttpLogoutOnRP: " + isUsingHttpLogoutOnRP);
        Log.info(thisClass, thisMethod, "isUsingHttpLogoutOnOP: " + isUsingHttpLogoutOnOP);
        Log.info(thisClass, thisMethod, "isUsingEndSessionWithHttpLogout: " + isUsingEndSessionWithHttpLogout);
        Log.info(thisClass, thisMethod, "isUsingLogoutWithHttpLogout: " + isUsingLogoutWithHttpLogout);
        //        Log.info(thisClass, thisMethod, "isUsingJwtToken: " + isUsingJwtToken);
        Log.info(thisClass, thisMethod, "isUsingIntrospect: " + isUsingIntrospect);
        Log.info(thisClass, thisMethod, "isTokenLimitExceeded: " + isTokenLimitExceeded);

    }

    /**
     * Access_tokens will be invalidated when:
     * end_session is used (this means all ways it can be invoked (from test request, from client app, from SAML)
     * req.logout is used (this means all ways that it can be invoked (from test request to the OP, or from client app)
     * So, even in cases where we're using a test app as the bcl endpoint (in the OP), the access_token should no longer be valid.
     * The token is cleaned up independent of the bcl call.
     *
     */
    public void setAccessTokenExpectedStateAfterLogout(BCL_FORM bcl_form, VariationSettings vSettings) {

        // the real bcl endpoint, and revoke remove/disable the access_token - it shouldn't be cleaned up in other cases
        if ((bcl_form != BCL_FORM.OMITTED && vSettings.flowUsesBCL) || vSettings.isRevokeEndpointInvoked) {
            accessTokenValid = false;
        } else {
            accessTokenValid = true;
        }

    }

    /**
     * refresh_token will be invalidated when:
     * end_session is used (this means all ways it can be invoked (from test request, from client app, from SAML)
     * req.logout is used (this means all ways that it can be invoked (from test request to the OP, or from client app)
     * So, even in cases where we're using a test app as the bcl endpoint (in the OP), the refresh_token should no longer be
     * valid. The token is cleaned up independent of the bcl call.
     * req.logout called on the OP will result in a bcl request to be made
     *
     * in the case where the logout endpoint is used on the OP, as long as a BCL endpoint is coded (valid or not), the
     * refresh_token will get cleaned up.
     */
    public void setRefreshTokenExpectedStateAfterLogout(BCL_FORM bcl_form, VariationSettings vSettings) {

        if ((bcl_form != BCL_FORM.OMITTED && vSettings.flowUsesBCL) || vSettings.isEndSessionEndpointInvoked) {
            refreshTokenValid = false;
        } else {
            refreshTokenValid = true;
        }

    }

    /**
     * The OP SSO cookie will be removed when:
     * end_session is used (this means all ways it can be invoked (from test request, from client app, from SAML)
     * req.logout is used (this means all ways that it can be invoked (from test request to the OP, or from client app)
     * So, even in cases where we're using a test app as the bcl endpoint (in the OP), the OP SSO cookie should no longer be
     * valid. The cookie is cleaned up independent of the bcl call.
     *
     */
    public void setOPCookiesExpectedStatesAfterLogout(VariationSettings vSettings) {

        if (vSettings.isOPReqLogoutInvoked) {
            opCookieExists = false;
            opCookieStillValid = false;
            if (isUsingSaml) {
                opCookieMatchesPrevious = true;
            } else {
                opCookieMatchesPrevious = false;
            }
        } else {
            if (isUsingSaml) {
                opCookieExists = false;
                opCookieStillValid = false;
                opCookieMatchesPrevious = true;
            } else {
                opCookieExists = true;
                opCookieMatchesPrevious = true;
            }
        }
        //        if (isUsingHttpLogoutOnRP && !(isUsingLogoutWithHttpLogout || isUsingEndSessionWithHttpLogout)) {
        //            opCookieExists = true;
        //        } else {
        //            opCookieExists = false;
        //        }
        opJSessionIdExists = true;
        opJSessionIdMatchesPrevious = false;
        //        // The OP cookie won't be set/used when using SAML, so it'll match (null before logout and null after logout)
        //        if (isUsingSaml) {
        //            opCookieMatchesPrevious = true;
        //        } else {
        //            if (isUsingHttpLogoutOnRP && !(isUsingLogoutWithHttpLogout || isUsingEndSessionWithHttpLogout)) {
        //                opCookieMatchesPrevious = true;
        //            } else {
        //                opCookieMatchesPrevious = false;
        //            }
        //        }

    }

    /**
     * The client (either oidc or social) SSO cookie wll be removed when:
     * end_session is used and we're using a valid bcl endopint - we need the client to process the request.
     * req.logout is used from client app on the client
     * So, the client cookie is only going to be cleaned up when logout is done on the client - for our testing, this should be
     * done in
     * all cases, except when using end_session invoked on the OP where we have a test app specified as the bcl endpoint
     *
     */
    public void setClientCookiesExpectedStatesAfterLogout(BCL_FORM bcl_form, VariationSettings vSettings) {

        // TODO - verify this
        // the cookies are cleaned up when req.logout is invoked on the RP, or we using the real bcl endpoint - end_session will invalidate the client cookie in a round about way
        if (vSettings.logoutMethodTested == Constants.HTTP_SESSION && vSettings.sessionLogoutEndpoint == null) {
            if (RepeatTestFilter.getRepeatActionsAsString().contains(Constants.OIDC__OP)) {
                clientCookieExists = true;
                clientCookieStillValid = true;
                clientCookieMatchesPrevious = true;
            } else {
                clientCookieExists = true;
                clientCookieStillValid = false;
                clientCookieMatchesPrevious = false;
            }
            return;
        }
        if (vSettings.isOPReqLogoutInvoked && (bcl_form == BCL_FORM.VALID) || vSettings.isRPReqLogoutInvoked) {
            //        if (vSettings.isRPReqLogoutInvoked || (usesRealBCLEndpoint && vSettings.flowUsesBCL) || vSettings.isEndSessionEndpointInvoked) {
            //        if ((usesRealBCLEndpoint || isUsingHttpLogoutOnRP) && !isUsingHttpLogoutOnOP && (!(isUsingLogoutNotHttpLogout && isUsingSaml))) {
            clientCookieExists = false;
            clientCookieStillValid = false;
            clientCookieMatchesPrevious = false;
        }

    }

    public void setReuseWebClientExpectedStateAfterLogout(BCL_FORM bcl_form, VariationSettings vSettings) {

        Log.info(thisClass, "setReuseWebClientExpectedStateAfterLogout", "usesRealBCLEndpoint: " + (bcl_form != BCL_FORM.OMITTED) + " isUsingHttpLogout: " + isUsingHttpLogoutOnRP + " isUsingLogoutNotHttpLogout: " + isUsingLogoutNotHttpLogout + " isUsingSaml: " + isUsingSaml);

        //        if ((usesRealBCLEndpoint || isUsingHttpLogoutOnRP) && (!(isUsingLogoutNotHttpLogout && isUsingSaml) || isUsingHttpLogoutOnOP)) {
        Log.info(thisClass, "setReuseWebClientExpectedStateAfterLogout", bcl_form.toString());
        Log.info(thisClass, "setReuseWebClientExpectedStateAfterLogout", Boolean.toString(vSettings.isOPReqLogoutInvoked));
        Log.info(thisClass, "setReuseWebClientExpectedStateAfterLogout", Boolean.toString(vSettings.isRPReqLogoutInvoked));

        if ((vSettings.isOPReqLogoutInvoked && (bcl_form == BCL_FORM.VALID) || vSettings.isRPReqLogoutInvoked) && !(vSettings.logoutMethodTested.equals(Constants.HTTP_SESSION) && vSettings.sessionLogoutEndpoint == null)) {
            appSessionAccess = false;
        }
    }

    public void setSPCookiesExpectedStatesAfterLogout(BCL_FORM bcl_form, VariationSettings vSettings) {

        // The SP cookie won't be set/used when NOT using SAML, so it'll match (null before logout and null after logout)
        //        if (isUsingSaml) {
        //            spCookieExists = false;
        //            spCookieMatchesPrevious = false;
        //        } else {
        //            if ((bcl_form != BCL_FORM.OMITTED) && vSettings.flowUsesBCL || vSettings.isRPReqLogoutInvoked) {
        //                spCookieExists = false;
        //            }
        //            spCookieMatchesPrevious = true;
        //        }

        if (!isUsingSaml) {
            spCookieExists = false;
            spCookieStillValid = false;
            spCookieMatchesPrevious = false;
        } else {
            if (vSettings.isOPReqLogoutInvoked) {
                spCookieExists = false;
                spCookieStillValid = false;
                spCookieMatchesPrevious = false;
            } else {
                spCookieExists = true;
                spCookieMatchesPrevious = true;
            }
        }

    }

    public void setIDPCookiesExpectedStatesAfterLogout(BCL_FORM bcl_form) {

        idpCookieExists = false;
        idpCookieMatchesPrevious = false;

    }

    /************* cookie methods **************/
    public void setOPSSOCookiesRemoved() throws Exception {

        opCookieExists = false;
        opCookieStillValid = false;
        opCookieMatchesPrevious = false;

    }

    public void setOPJSessionIdRemoved() throws Exception {

        opJSessionIdExists = false;
        opJSessionIdStillValid = false;
        opJSessionIdMatchesPrevious = false;

    }

    public void setOPAllCookiesRemoved() throws Exception {

        setOPSSOCookiesRemoved();
        setOPJSessionIdRemoved();
    }

    public void setOPNoCookiesRemoved(boolean isSaml) throws Exception {

        if (isSaml) {
            opCookieExists = false;
            opCookieStillValid = false;
            opCookieMatchesPrevious = false;
        } else {
            opCookieExists = true;
            opCookieStillValid = true;
            opCookieMatchesPrevious = true;
        }
        opJSessionIdExists = true;
        opJSessionIdMatchesPrevious = true;

    }

    public void setClientSSOCookiesRemoved() throws Exception {

        clientCookieExists = false;
        clientCookieStillValid = false;
        clientCookieMatchesPrevious = false;

    }

    public void setClientJSessionIdRemoved() throws Exception {

        clientJSessionIdExists = false;
        clientJSessionIdStillValid = false;
        clientJSessionIdMatchesPrevious = false;

    }

    public void setClientAllCookiesRemoved() throws Exception {

        setClientSSOCookiesRemoved();
        setClientJSessionIdRemoved();
    }

    public void setClientNoCookiesRemoved() throws Exception {

        clientCookieExists = true;
        clientCookieStillValid = true;
        clientCookieMatchesPrevious = true;
        clientJSessionIdExists = true;
        clientJSessionIdStillValid = true;
        clientJSessionIdMatchesPrevious = true;

    }

    public void setSPCookiesNotRemoved(boolean isSaml) throws Exception {

        if (isSaml) {
            spCookieExists = true;
            spCookieStillValid = true;
            spCookieMatchesPrevious = true;
        } else {
            spCookieExists = false;
            spCookieStillValid = false;
            spCookieMatchesPrevious = true;
        }

    }

    //    public void setAllCookiesRemoved() throws Exception {
    //        setClientAllCookiesRemoved();
    //        setOPAllCookiesRemoved();
    //    }

    /************* Token methods **************/
    public void setAllTokensCleanedUp() throws Exception {

        setIsAccessTokenValid(false); // id_token is removed from cache - opaque access_token will fail to validate
        setIsRefreshTokenValid(false);
    }

    /************* Individual state setters/getters **************/
    public void setOpCookieExists(boolean value) {
        opCookieExists = value;
    }

    public boolean getOpCookieExists() {
        return opCookieExists;
    }

    public void setOpCookieStillValid(boolean value) {
        opCookieStillValid = value;
    }

    public boolean getOpCookieStillValid() {
        return opCookieStillValid;
    }

    //    public void setOpCookieMatchesPrevious(boolean value) {
    //        opCookieMatchesPrevious = value;
    //    }

    public boolean getOpCookieMatchesPrevious() {
        return opCookieMatchesPrevious;
    }

    public void setClientCookieExists(boolean value) {
        clientCookieExists = value;
    }

    public boolean getClientCookieExists() {
        return clientCookieExists;
    }

    public void setClientCookieStillValid(boolean value) {
        clientCookieStillValid = value;
    }

    public boolean getClientCookieStillValid() {
        return clientCookieStillValid;
    }

    public void setClientCookieMatchesPrevious(boolean value) {
        clientCookieMatchesPrevious = value;
    }

    public boolean getClientCookieMatchesPrevious() {
        return clientCookieMatchesPrevious;
    }

    public void setClient2CookieExists(boolean value) {
        client2CookieExists = value;
    }

    public boolean getClient2CookieExists() {
        return client2CookieExists;
    }

    public void setClient2CookieStillValid(boolean value) {
        client2CookieStillValid = value;
    }

    public boolean getClient2CookieStillValid() {
        return client2CookieStillValid;
    }

    public void setClient2CookieMatchesPrevious(boolean value) {
        client2CookieMatchesPrevious = value;
    }

    public boolean getClient2CookieMatchesPrevious() {
        return client2CookieMatchesPrevious;
    }

    //    public void setOpJSessionIdExists(boolean value) {
    //        opJSessionIdExists = value;
    //    }

    public boolean getOpJSessionIdExists() {
        return opJSessionIdExists;
    }

    //    public void setOpJSessionIdMatchesPrevious(boolean value) {
    //        opJSessionIdMatchesPrevious = value;
    //    }

    public boolean getOpJSessionIdMatchesPrevious() {
        return opJSessionIdMatchesPrevious;
    }

    //    public void setOpJSessionIdMatchesPrevious(boolean value) {
    //        opJSessionIdMatchesPrevious = value;
    //    }

    public boolean getOpJSessionIdStillValid() {
        return opJSessionIdStillValid;
    }

    //    public void setClientJSessionIdExists(boolean value) {
    //        clientJSessionIdExists = value;
    //    }

    public boolean getClientJSessionIdExists() {
        return clientJSessionIdExists;
    }

    public boolean getClientJSessionIdStillValid() {
        return clientJSessionIdStillValid;
    }

    public void setClientJSessionIdMatchesPrevious(boolean value) {
        clientJSessionIdMatchesPrevious = value;

    }

    public boolean getClientJSessionIdMatchesPrevious() {
        return clientJSessionIdMatchesPrevious;

    }

    public boolean getClient2JSessionIdExists() {
        return client2JSessionIdExists;
    }

    public boolean getClient2JSessionIdStillValid() {
        return client2JSessionIdStillValid;
    }

    public void setClient2JSessionIdMatchesPrevious(boolean value) {
        client2JSessionIdMatchesPrevious = value;

    }

    public boolean getClient2JSessionIdMatchesPrevious() {
        return client2JSessionIdMatchesPrevious;

    }

    public boolean getSpCookieExists() {
        return spCookieExists;
    }

    public void setSpCookieExists(boolean value) {
        spCookieExists = value;
    }

    public boolean getSpCookieStillValid() {
        return spCookieStillValid;
    }

    public void setSpCookieStillValid(boolean value) {
        spCookieStillValid = value;
    }

    public boolean getSpCookieMatchesPrevious() {
        return spCookieMatchesPrevious;
    }

    public void setSpCookieMatchesPrevious(boolean value) {
        spCookieMatchesPrevious = value;
    }

    public boolean getIdpCookieExists() {
        return idpCookieExists;
    }

    public boolean getIdpCookieMatchesPrevious() {
        return idpCookieMatchesPrevious;
    }

    public void setIsAppSessionAccess(boolean value) {
        appSessionAccess = value;
    }

    public boolean getIsAppSessionAccess() {
        return appSessionAccess;
    }

    public void setIsAccessTokenValid(boolean value) {
        accessTokenValid = value;
    }

    public boolean getIsAccessTokenValid() {
        return accessTokenValid;
    }

    public void setIsRefreshTokenValid(boolean value) {
        refreshTokenValid = value;

    }

    public boolean getIsRefreshTokenValid() {
        return refreshTokenValid;

    }

    public void setIsUsingIntrospect(boolean value) {
        isUsingIntrospect = value;
    }

    public boolean getIsUsingIntrospect() {
        return isUsingIntrospect;
    }

    public void setIsUsingInvalidIntrospect(boolean value) {
        isUsingInvalidIntrospect = value;
    }

    public boolean getIsUsingInvalidIntrospect() {
        return isUsingInvalidIntrospect;
    }

    public void setIsTokenLimitExceeded(boolean value) {
        isTokenLimitExceeded = value;
    }

    public boolean getIsTokenLimitExceeded() {
        return isTokenLimitExceeded;
    }
}
