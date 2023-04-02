/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
    boolean opCookieMatchesPrevious = true;

    boolean clientCookieExists = true;
    boolean clientCookieMatchesPrevious = true;

    boolean opJSessionIdExists = true;
    boolean opJSessionIdMatchesPrevious = true;

    boolean clientJSessionIdExists = true;
    boolean clientJSessionIdMatchesPrevious = true;

    boolean spCookieExists = true;
    boolean spCookieMatchesPrevious = true;

    boolean idpCookieExists = true;
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
    boolean isUsingHttpLogout = false;
    boolean isUsingEndSessionWithHttpLogout = false;
    boolean isUsingLogoutWithHttpLogout = false;
    boolean isUsingJwtToken = true;

    public AfterLogoutStates() {
        isUsingOidcNotSocial = true;
        isUsingEndSessionNotHttpLogout = true;
        isUsingJwtToken = true;
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
     *            when http req.logout is used, either logout or end_session is invoked on the OP (call made from the logout test
     *            app)
     * @param tokenType
     *            access_token is an opaque or jwt token
     */
    public AfterLogoutStates(boolean usesRealBCLEndpoint, String flowType, String logoutMethod, String sessionEndpoint, String tokenType) {

        if (flowType == null || flowType.equals(Constants.RP_FLOW)) {
            Log.info(thisClass, "AfterLogoutStates", "flowType: " + flowType + " (in OIDC path)");
            isUsingOidcNotSocial = true;
        } else {
            Log.info(thisClass, "AfterLogoutStates", "flowType: " + flowType + " (in Social path)");
            isUsingOidcNotSocial = false;
        }
        if (logoutMethod.equals(Constants.END_SESSION) || logoutMethod.equals(Constants.SAML)) {
            isUsingEndSessionNotHttpLogout = true;
        } else {
            if (logoutMethod.equals(Constants.LOGOUT_ENDPOINT)) {
                isUsingLogoutNotHttpLogout = true;
            } else {
                isUsingHttpLogout = true;
                if (sessionEndpoint.equals(Constants.LOGOUT_ENDPOINT)) {
                    isUsingLogoutWithHttpLogout = true;
                } else {
                    isUsingEndSessionWithHttpLogout = true;
                }
            }
        }

        if (tokenType == null || tokenType.equals(Constants.JWT_TOKEN)) {
            isUsingJwtToken = true;
        } else {
            isUsingJwtToken = false;
        }

        // set the states of cookies and tokens after logout based on:
        // the type of logout/end_session,
        // whether the BCL endpoint is a real bcl endpoint, or a test app
        setAccessTokenExpectedStateAfterLogout();
        setRefreshTokenExpectedStateAfterLogout();
        setOPCookiesExpectedStatesAfterLogout();
        setClientCookiesExpectedStatesAfterLogout(usesRealBCLEndpoint);
        setReuseWebClientExpectedStateAfterLogout(usesRealBCLEndpoint);
        setIDPCookiesExpectedStatesAfterLogout(usesRealBCLEndpoint);

    }

    public void printStates() {

        String thisMethod = "AfterLogoutStates";

        Log.info(thisClass, thisMethod, "opCookieExists: " + opCookieExists);
        Log.info(thisClass, thisMethod, "opCookieMatchesPrevious: " + opCookieMatchesPrevious);

        Log.info(thisClass, thisMethod, "clientCookieExists: " + clientCookieExists);
        Log.info(thisClass, thisMethod, "clientCookieMatchesPrevious: " + clientCookieMatchesPrevious);

        Log.info(thisClass, thisMethod, "opJSessionIdExists: " + opJSessionIdExists);
        Log.info(thisClass, thisMethod, "opJSessionIdMatchesPrevious: " + opJSessionIdMatchesPrevious);

        Log.info(thisClass, thisMethod, "clientJSessionIdExists: " + clientJSessionIdExists);
        Log.info(thisClass, thisMethod, "clientJSessionIdMatchesPrevious: " + clientJSessionIdMatchesPrevious);

        Log.info(thisClass, thisMethod, "spCookieExists: " + spCookieExists);
        Log.info(thisClass, thisMethod, "spCookieMatchesPrevious: " + spCookieMatchesPrevious);

        Log.info(thisClass, thisMethod, "idpCookieExists: " + idpCookieExists);
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
        Log.info(thisClass, thisMethod, "isUsingHttpLogout: " + isUsingHttpLogout);
        Log.info(thisClass, thisMethod, "isUsingEndSessionWithHttpLogout: " + isUsingEndSessionWithHttpLogout);
        Log.info(thisClass, thisMethod, "isUsingLogoutWithHttpLogout: " + isUsingLogoutWithHttpLogout);
        Log.info(thisClass, thisMethod, "isUsingJwtToken: " + isUsingJwtToken);

    }

    /**
     * Access_tokens will be invalidated when:
     * end_session is used (this means all ways it can be invoked (from test request, from client app, from SAML)
     * req.logout is used (this means all ways that it can be invoked (from test request to the OP, or from client app)
     * So, even in cases where we're using a test app as the bcl endpoint (in the OP), the access_token should no longer be valid.
     * The token is cleaned up independent of the bcl call.
     *
     */
    public void setAccessTokenExpectedStateAfterLogout() {

        accessTokenValid = false;

    }

    /**
     * refresh_token will be invalidated when:
     * end_session is used (this means all ways it can be invoked (from test request, from client app, from SAML)
     * req.logout is used (this means all ways that it can be invoked (from test request to the OP, or from client app)
     * So, even in cases where we're using a test app as the bcl endpoint (in the OP), the refresh_token should no longer be
     * valid. The token is cleaned up independent of the bcl call.
     *
     * in the case where the logout endpoint is used on the OP, as long as a BCL endpoint is coded (valid or not), the
     * refresh_token will get cleaned up.
     */
    public void setRefreshTokenExpectedStateAfterLogout() {

        refreshTokenValid = false;

    }

    /**
     * The OP SSO cookie wll be removed when:
     * end_session is used (this means all ways it can be invoked (from test request, from client app, from SAML)
     * req.logout is used (this means all ways that it can be invoked (from test request to the OP, or from client app)
     * So, even in cases where we're using a test app as the bcl endpoint (in the OP), the OP SSO cookie should no longer be
     * valid. The cookie is cleaned up independent of the bcl call.
     *
     */
    public void setOPCookiesExpectedStatesAfterLogout() {

        opCookieExists = false;
        opCookieMatchesPrevious = false;
        opJSessionIdExists = true;
        opJSessionIdMatchesPrevious = false;

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
    public void setClientCookiesExpectedStatesAfterLogout(boolean usesRealBCLEndpoint) {

        if (usesRealBCLEndpoint || isUsingHttpLogout) {
            clientCookieExists = false;
            clientCookieMatchesPrevious = false;
        }

    }

    public void setReuseWebClientExpectedStateAfterLogout(boolean usesRealBCLEndpoint) {

        if (usesRealBCLEndpoint || isUsingHttpLogout) {
            appSessionAccess = false;
        }
    }

    // TODO - update when SAML support is added
    public void setIDPCookiesExpectedStatesAfterLogout(boolean usesRealBCLEndpoint) {

        spCookieExists = false;
        spCookieMatchesPrevious = false;

        idpCookieExists = false;
        idpCookieMatchesPrevious = false;

    }

    /************* cookie methods **************/
    public void setOPSSOCookiesRemoved() throws Exception {

        opCookieExists = false;
        opCookieMatchesPrevious = false;

    }

    public void setOPJSessionIdRemoved() throws Exception {

        opJSessionIdExists = false;
        opJSessionIdMatchesPrevious = false;

    }

    public void setOPAllCookiesRemoved() throws Exception {

        setOPSSOCookiesRemoved();
        setOPJSessionIdRemoved();
    }

    public void setOPNoCookiesRemoved() throws Exception {

        opCookieExists = true;
        opCookieMatchesPrevious = true;
        opJSessionIdExists = true;
        opJSessionIdMatchesPrevious = true;

    }

    public void setClientSSOCookiesRemoved() throws Exception {

        clientCookieExists = false;
        clientCookieMatchesPrevious = false;

    }

    public void setClientJSessionIdRemoved() throws Exception {

        clientJSessionIdExists = false;
        clientJSessionIdMatchesPrevious = false;

    }

    public void setClientAllCookiesRemoved() throws Exception {

        setClientSSOCookiesRemoved();
        setClientJSessionIdRemoved();
    }

    public void setClientNoCookiesRemoved() throws Exception {

        clientCookieExists = true;
        clientCookieMatchesPrevious = true;
        clientJSessionIdExists = true;
        clientJSessionIdMatchesPrevious = true;

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
    //    public void setOpCookieExists(boolean value) {
    //        opCookieExists = value;
    //    }

    public boolean getOpCookieExists() {
        return opCookieExists;
    }

    //    public void setOpCookieMatchesPrevious(boolean value) {
    //        opCookieMatchesPrevious = value;
    //    }

    public boolean getOpCookieMatchesPrevious() {
        return opCookieMatchesPrevious;
    }

    //    public void setClientCookieExists(boolean value) {
    //        clientCookieExists = value;
    //    }

    public boolean getClientCookieExists() {
        return clientCookieExists;
    }

    //    public void setClientCookieMatchesPrevious(boolean value) {
    //        clientCookieMatchesPrevious = value;
    //    }

    public boolean getClientCookieMatchesPrevious() {
        return clientCookieMatchesPrevious;
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

    //    public void setClientJSessionIdExists(boolean value) {
    //        clientJSessionIdExists = value;
    //    }

    public boolean getClientJSessionIdExists() {
        return clientJSessionIdExists;
    }

    public void setClientJSessionIdMatchesPrevious(boolean value) {
        clientJSessionIdMatchesPrevious = value;

    }

    public boolean getClientJSessionIdMatchesPrevious() {
        return clientJSessionIdMatchesPrevious;

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
}
