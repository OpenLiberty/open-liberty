/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.spnego.fat.config;

import javax.security.auth.login.LoginException;

import org.ietf.jgss.GSSException;

import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.SSLBasicAuthClient;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public abstract interface JDKExpectationTestClass {

    ///////////////////////////////
    //Liberty Server Expectations//
    ///////////////////////////////
    public void serverUpdate(LibertyServer myServer);

    ////////////////////
    //KDC Expectations//
    ////////////////////
    public void KDCErr_NoSupportForEncryptionType(LoginException e);

    public void KDCErr_InaccessibleKDC(Exception e);

    //////////////////////////////
    //Succesful Servlet Response//
    //////////////////////////////
    public void successfulServletResponse(String response, BasicAuthClient myClient, String user, boolean isEmployee, boolean isManager);

    /////////////////////////////////////
    //NTLM Token Error Code Expectation//
    /////////////////////////////////////
    public void ntlmTokenReceivedErrorCode(String response);

    public void ntlmTokenContentCustomErrorPage(String response);

    public void ntlmtokenReceivedCustomErrorPage(String response, String errorPage);

    //////////////////////////////////
    //Custom cache key expectations//
    ////////////////////////////////
    public void responseContainsCustomCacheKey(String response, String customCacheKey);

    public void responseContainsNullCacheKey(String response, String customCacheKey);

    //////////////////////////
    //SSO Token Expectations//
    //////////////////////////
    public void isSSOCookiePresent(String ssoCookie);

    public void isSSOCookiePresent(String response, String ssoCookieName);

    public void isSSOCookieNotPresent(String response, String ssoCookieName);

    //should I merge this one with ssoCookieIsNull and invalidvalue?
    public void responseContainsSSOCookie(String response, String ssoCookieName, String ssoCookie);

    public void ssoCookieIsNull(String response, String ssoCookieName, String ssoCookie);

    public void ssoCookieInvalidValue(String response, String ssoCookieName, String ssoCookie);

    ////////////////////////
    //SPNEGO Servlet Calls//
    ////////////////////////
    public void successfulExpectationsSpnegoServletCall(String response, String user, boolean areGSSCredPresent);

    public void successfulExpectationsSpnegoServletCallForMappedUser(String response, String spnegoTokenUser);

    public void successfulSpnegoServletCallSSLClient(String response, SSLBasicAuthClient mySslClient);

    public void unsuccesfulSpnegoServletCall(String response, String ownerInformation);

    //////////////////////////////////
    //SPNEGO Error Code Expectations//
    //////////////////////////////////
    public void responseShouldContainSpnegoErrorCode(String response);

    public void responseContainsInvalidCredentialError(String response);

    public void spnegoNotSupported(String response);

    public void spnegoNotSupportedCustomErrorPage(String response, String errorPage);

    public void spnegoNotSupportedContentCustomErrorPage(String response, boolean customErrorPage);

    public void spnegoNotSupportedContentCustomErrorPage(String response, String errorPage, boolean customErrorPage);

    public void spnegoTokenNotFound(String response);

    //////////////////////////////////////
    //SPNEGO GSS Credential Expectations//
    //////////////////////////////////////
    //SHOULD I COMBINE WITH responseShouldContainCorrectGSSCredOwner
    public void responseShouldContaiGSSCredentials(String response);

    public void responseShouldContainCorrectGSSCredOwner(String response);

    public void spnegoInvalidCredential(GSSException e);

    ///////////////////////////////////
    //s4u2Proxy and Self expectations//
    ///////////////////////////////////
    public void s4u2Proxy_NotEnabled(String response);

    public void s4u2Proxy_NoClassDefFoundError(String response);

    public void s4u2_responseContainsToken(String response, String responseCheckForSubjectTest);

    public void s4u2_responseContainsToken(String response, String responseCheckForSubjectTest, boolean responseHasMultipleToken);

    public void s4u2_responseFromBackendServer(String response, String user);

    public void s4u2_responseContainsDifferentTokens(String response);

    public void s4u2_validateKerberosAndGSSCred(String response);

    public void s4u2_validateNegativeResponse(String response);

    public void s4u2_checkForFailedSubject(String response, String responseCheckForFailure);

    public void s4u2_krbCredNotPresent(String response);

}
