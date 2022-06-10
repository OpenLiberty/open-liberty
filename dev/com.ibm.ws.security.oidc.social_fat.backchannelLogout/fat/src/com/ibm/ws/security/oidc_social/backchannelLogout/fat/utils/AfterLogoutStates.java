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
package com.ibm.ws.security.oidc_social.backchannelLogout.fat.utils;

import com.ibm.websphere.simplicity.log.Log;

/**
 * This class supplies support methods to the back channel logout tests.
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
    boolean isUsingEndSessionNotHttpLogout = true;
    boolean isUsingEndSessionWithHttpLogout = true;
    boolean isUsingJwtToken = true;

    public AfterLogoutStates() {
        isUsingOidcNotSocial = true;
        isUsingEndSessionNotHttpLogout = true;
        isUsingJwtToken = true;
    }

    /**
     * Init an AfaterLogoutStates object for a successful BCL
     *
     * @param flowType
     * @param logoutMethod
     * @param sessionEndpoint
     * @param tokenType
     */
    public AfterLogoutStates(String flowType, String logoutMethod, String sessionEndpoint, String tokenType) {

        if (flowType == null || flowType.equals(Constants.RP_FLOW)) {
            Log.info(thisClass, "AfterLogoutStates", "flowType: " + flowType + " (in OIDC path)");
            isUsingOidcNotSocial = true;
        } else {
            Log.info(thisClass, "AfterLogoutStates", "flowType: " + flowType + " (in Social path)");
            isUsingOidcNotSocial = false;
            // config updated to include jsession
            //            // social flow doesn't create a client jsessionid cookie // TODO - check on this!
            //            clientJSessionIdExists = false;
            //            clientJSessionIdMatchesPrevious = false;
        }
        if (logoutMethod == null || logoutMethod.equals(Constants.END_SESSION)) {
            isUsingEndSessionNotHttpLogout = true;
        } else {
            isUsingEndSessionNotHttpLogout = false;
            if (sessionEndpoint == null || sessionEndpoint.equals(Constants.LOGOUT_ENDPOINT)) {
                isUsingEndSessionWithHttpLogout = false;
            } else {
                isUsingEndSessionWithHttpLogout = true;
            }
        }

        if (tokenType == null || tokenType.equals(Constants.JWT_TOKEN)) {
            isUsingJwtToken = true;
        } else {
            isUsingJwtToken = false;
        }

        setAccessToken();
        setRefreshToken();
        setOPCookies(flowType, logoutMethod, sessionEndpoint, tokenType);
        setClientCookies(flowType, logoutMethod, sessionEndpoint, tokenType);
        setReuseWebClient(flowType, logoutMethod, sessionEndpoint, tokenType);
        setIDPCookies(flowType, logoutMethod, sessionEndpoint, tokenType);
    }

    /**
     * Access_tokens will be invalidated when:
     * end_session is used (this means all ways it can be invoked (from test request, from client app, from SAML)
     * req.logout is used (this means all ways that it can be invoked (from test request to the OP, or from client app)
     * 
     */
    public void setAccessToken() {

        accessTokenValid = false;
    }

    public void setRefreshToken() {

        refreshTokenValid = false;
    }

    public void setOPCookies(String flowType, String logoutMethod, String sessionEndpoint, String tokenType) {

        opCookieExists = false;
        opCookieMatchesPrevious = false;
    }

    public void setClientCookies(String flowType, String logoutMethod, String sessionEndpoint, String tokenType) {

        clientCookieExists = false;
        clientCookieMatchesPrevious = false;

    }

    public void setReuseWebClient(String flowType, String logoutMethod, String sessionEndpoint, String tokenType) {

        appSessionAccess = false;
    }

    public void setIDPCookies(String flowType, String logoutMethod, String sessionEndpoint, String tokenType) {

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

    public void setAllCookiesRemoved() throws Exception {
        setClientAllCookiesRemoved();
        setOPAllCookiesRemoved();
    }

    public void setClientCookieRemovalBasedOnLogoutType() throws Exception {
        if (!isUsingEndSessionNotHttpLogout) { // using a client initiated logout
            //            setClientAllCookiesRemoved();
            clientCookieExists = false;
            clientCookieMatchesPrevious = false;
        }
        if (!isUsingOidcNotSocial) { // TODO remove once social product code is delivered
            clientCookieExists = true;
            clientCookieMatchesPrevious = true;
        }
    }

    /************* Token methods **************/
    public void setAllTokensCleanedUp() throws Exception {
        //        if (isUsingJwtToken) {
        //            setAccessTokenValid(true);
        //        } else {
        setAccessTokenValid(false); // id_token is removed from cache - opaque access_token will fail to validate
        //        }
        setRefreshTokenValid(false);
    }

    /************* Individual state setters/getters **************/
    public void setOpCookieExists(boolean value) {
        opCookieExists = value;
    }

    public boolean getOpCookieExists() {
        return opCookieExists;
    }

    public void setOpCookieMatchesPrevious(boolean value) {
        opCookieMatchesPrevious = value;
    }

    public boolean getOpCookieMatchesPrevious() {
        return opCookieMatchesPrevious;
    }

    public void setClientCookieExists(boolean value) {
        clientCookieExists = value;
    }

    public boolean getClientCookieExists() {
        return clientCookieExists;
    }

    public void setClientCookieMatchesPrevious(boolean value) {
        clientCookieMatchesPrevious = value;
    }

    public boolean getClientCookieMatchesPrevious() {
        return clientCookieMatchesPrevious;
    }

    public void setOpJSessionIdExists(boolean value) {
        opJSessionIdExists = value;
    }

    public boolean getOpJSessionIdExists() {
        return opJSessionIdExists;
    }

    public void setOpJSessionIdMatchesPrevious(boolean value) {
        opJSessionIdMatchesPrevious = value;
    }

    public boolean getOpJSessionIdMatchesPrevious() {
        return opJSessionIdMatchesPrevious;
    }

    public void setClientJSessionIdExists(boolean value) {
        clientJSessionIdExists = value;
    }

    public boolean getClientJSessionIdExists() {
        return clientJSessionIdExists;
    }

    public void setClientJSessionIdMatchesPrevious(boolean value) {
        clientJSessionIdMatchesPrevious = value;

    }

    public boolean getClientJSessionIdMatchesPrevious() {
        return clientJSessionIdMatchesPrevious;

    }

    public void setAppSessionAccess(boolean value) {
        appSessionAccess = value;
    }

    public boolean getAppSessionAccess() {
        return appSessionAccess;
    }

    public void setAccessTokenValid(boolean value) {
        accessTokenValid = value;
    }

    public boolean getAccessTokenValid() {
        return accessTokenValid;
    }

    public void setRefreshTokenValid(boolean value) {
        refreshTokenValid = value;

    }

    public boolean getRefreshTokenValid() {
        return refreshTokenValid;

    }
}
