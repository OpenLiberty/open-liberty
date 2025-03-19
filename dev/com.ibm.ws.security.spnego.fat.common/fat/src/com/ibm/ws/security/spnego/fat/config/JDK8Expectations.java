/*******************************************************************************
 * Copyright (c) 2018, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.spnego.fat.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.security.auth.login.LoginException;

import org.ietf.jgss.GSSException;

import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.SSLBasicAuthClient;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public class JDK8Expectations implements JDKExpectationTestClass {
///////////////////////////////////////////////////////
// Negative test expected results for S4U2 Test Cases//
///////////////////////////////////////////////////////
    private static final String responseCheckForNullSpn1 = "Null SPN Test #1 Succeeded";
    private static final String responseCheckForNullSpn2 = "Null SPN Test #2 Succeeded";
    private static final String responseCheckForNullUpn1 = "Null UPN Test #1 Succeeded";
    private static final String responseCheckForNullUpn2 = "Null UPN Test #2 Succeeded";
    private static final String responseCheckForNullSubject1 = "Null Subject Test #1 Succeeded";
    private static final String responseCheckForNullUserId1 = "Null Userid Test #1 Succeeded";
    private static final String responseCheckForNullUserId2 = "Null Userid Test #2 Succeeded";
    private static final String responseCheckForNullPassword1 = "Null Password Test #1 Succeeded";
    private static final String responseCheckForNullPassword2 = "Null Password Test #2 Succeeded";
    private static final String responseCheckForBadUserId1 = "Bad Userid Test #1 Succeeded";
    private static final String responseCheckForBadUserId2 = "Bad Userid Test #2 Succeeded";
    private static final String responseCheckForBadSpn1 = "Bad SPN Test #1 Succeeded";
    private static final String responseCheckForBadSpn2 = "Bad SPN Test #2 Succeeded";

///////////////////////////////
//Liberty Server Expectations//
///////////////////////////////
    @Override
    public void serverUpdate(LibertyServer myServer) {
        assertNotNull("FeatureManager did not report update was complete", myServer.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Application did not start", myServer.waitForStringInLog("CWWKZ0001I"));
        assertNotNull("LTPA configuration did not report it was ready", myServer.waitForStringInLog("CWWKS4105I"));
    }

////////////////////
//KDC Expectations//
////////////////////
    @Override
    public void KDCErr_NoSupportForEncryptionType(LoginException e) {
        assertTrue("Expected exception was thrown but error message was unexpected.", e.getMessage().contains("KDC has no support for encryption type"));

    }

    @Override
    public void KDCErr_InaccessibleKDC(Exception e) {
        assertTrue("Did not receive expected exception message.", e.getMessage().contains("UnresolvedAddressException"));
    }

//////////////////////////////
//Succesful Servlet Response//
//////////////////////////////
    @Override
    public void successfulServletResponse(String response, BasicAuthClient myClient, String user, boolean isEmployee, boolean isManager) {
        assertTrue("Expected to receive a successful response but found a problem.", myClient.verifyResponse(response, user, isEmployee, isManager));
    }

/////////////////////////////////////
//NTLM Token Error Code Expectation//
/////////////////////////////////////
    @Override
    public void ntlmTokenReceivedErrorCode(String response) {
        assertTrue("Expected to receive the default error message for 'NTLM token received' but it was not received.",
                   response.contains(MessageConstants.NTLM_TOKEN_RECEIVED_CWWKS4307E));
    }

    @Override
    public void ntlmTokenContentCustomErrorPage(String response) {
        assertTrue("Expected to receive the defined content type for 'NTLM token received' custom error page but it was not received.",
                   response.contains("text/example"));
        assertTrue("Expected to receive the defined page encoding for 'NTLM token received' custom error page but it was not received.",
                   response.contains("ISO-8859-1"));
    }

    @Override
    public void ntlmtokenReceivedCustomErrorPage(String response, String errorPage) {
        assertTrue("Expected to receive the defined error message for 'NTLM token received' but it was not received.",
                   response.contains(errorPage));
    }

//////////////////////////////////
//Custom cache key expectations//
////////////////////////////////
    @Override
    public void responseContainsCustomCacheKey(String response, String customCacheKey) {
        assertTrue("Response should contain a custom cache key for user " + InitClass.COMMON_TOKEN_USER + " but one was not found.",
                   response.contains(customCacheKey + InitClass.COMMON_TOKEN_USER));
    }

    @Override
    public void responseContainsNullCacheKey(String response, String customCacheKey) {
        assertTrue("Response should contain a null custom cache key value but one was not found.",
                   response.contains(customCacheKey + "null"));
        assertFalse("Response should not contain a custom cache key for user " + InitClass.COMMON_TOKEN_USER + " but one was found.",
                    response.contains(customCacheKey + InitClass.COMMON_TOKEN_USER));
    }

//////////////////////////
//SSO Token Expectations//
//////////////////////////
    @Override
    public void isSSOCookiePresent(String ssoCookie) {
        assertNotNull("Did not obtain SSO cookie despite successfully accessing protected resource.", ssoCookie);

    }

    @Override
    public void isSSOCookiePresent(String response, String ssoCookieName) {
        assertTrue("Received SSO Cookie, but should not have.",
                   response.contains("Response Cookie: " + ssoCookieName) && !response.contains("Response Cookie: " + ssoCookieName + "=Null"));
    }

    @Override
    public void isSSOCookieNotPresent(String response, String ssoCookieName) {
        assertTrue("Received SSO Cookie, but should not have.", response.contains("Response Cookie: " + ssoCookieName + "=Null"));
    }

//should I merge this one with ssoCookieIsNull and invalidvalue?
    @Override
    public void responseContainsSSOCookie(String response, String ssoCookieName, String ssoCookie) {
        assertTrue("Did not find SSO cookie with expected value.", response.contains(ssoCookieName + " value: " + ssoCookie));
    }

    @Override
    public void ssoCookieIsNull(String response, String ssoCookieName, String ssoCookie) {
        assertTrue("Non-null SSO cookie value found despite invalid cookie being sent.", response.contains(ssoCookieName + " value: null"));
    }

    @Override
    public void ssoCookieInvalidValue(String response, String ssoCookieName, String ssoCookie) {
        assertTrue("Did not find SSO cookie with expected invalid value.", response.contains(ssoCookieName + " value: " + ssoCookie));
    }

////////////////////////
//SPNEGO Servlet Calls//
////////////////////////
    @Override
    public void successfulExpectationsSpnegoServletCall(String response, String user, boolean areGSSCredPresent) {
        if (areGSSCredPresent) {
            responseShouldContaiGSSCredentials(response);
            assertTrue("GSS credentials did not have the correct \"Owner\" value of \"" + SPNEGOConstants.OWNER_STRING + user + "\"",
                       response.contains(SPNEGOConstants.OWNER_STRING + user));
        } else {
            assertTrue("Response should contain GSS credentials but none were found.", (!response.contains(SPNEGOConstants.GSS_CREDENTIAL_STRING)));
        }
    }

    @Override
    public void successfulExpectationsSpnegoServletCallForMappedUser(String response, String spnegoTokenUser) {
// GSS credentials will reflect the original user, not the mapped user
        successfulExpectationsSpnegoServletCall(response, spnegoTokenUser, true);
    }

    @Override
    public void successfulSpnegoServletCallSSLClient(String response, SSLBasicAuthClient mySslClient) {
        assertTrue("Expected to receive a successful response but found a problem.",
                   mySslClient.verifyResponse(response, InitClass.COMMON_TOKEN_USER, InitClass.COMMON_TOKEN_USER_IS_EMPLOYEE, InitClass.COMMON_TOKEN_USER_IS_MANAGER));
        responseShouldContaiGSSCredentials(response);
//        assertTrue("GSS credentials did not have the correct \"Owner\" value of \"" + SPNEGOConstants.OWNER_STRING + InitClass.COMMON_TOKEN_USER + "\"",
//                   response.contains(SPNEGOConstants.OWNER_STRING + InitClass.COMMON_TOKEN_USER));
    }

    @Override
    public void unsuccesfulSpnegoServletCall(String response, String ownerInformation) {
        assertFalse("Response should NOT contain GSS credentials but credentials were found.", response.contains(SPNEGOConstants.GSS_CREDENTIAL_STRING));
        assertFalse("Response should NOT contain Owner information related to GSS credentials.", response.contains(ownerInformation));
    }

//////////////////////////////////
//SPNEGO Error Code Expectations//
//////////////////////////////////
    @Override
    public void responseShouldContainSpnegoErrorCode(String response) {
        assertTrue("Response should contain SPNEGO error message.", response.contains(MessageConstants.NTLM_TOKEN_RECEIVED_CWWKS4307E));
    }

    @Override
    public void responseContainsInvalidCredentialError(String response) {
        assertTrue("The response should have had Invalid credentials but it did not.", response.contains("major string: Invalid credentials"));
    }

    @Override
    public void spnegoNotSupported(String response) {
        spnegoNotSupportedContentCustomErrorPage(response, null, false);
    }

    @Override
    public void spnegoNotSupportedCustomErrorPage(String response, String errorPage) {
        spnegoNotSupportedContentCustomErrorPage(response, errorPage, true);
    }

    @Override
    public void spnegoNotSupportedContentCustomErrorPage(String response, boolean customErrorPage) {
        spnegoNotSupportedContentCustomErrorPage(response, null, customErrorPage);
    }

    @Override
    public void spnegoNotSupportedContentCustomErrorPage(String response, String errorPage, boolean customErrorPage) {

        if (errorPage != null) {
            assertTrue("Expected to receive the defined error message for 'SPNEGO not supported' but it was not received.",
                       response.contains(errorPage));
        } else if (errorPage == null && customErrorPage) {
            assertTrue("Expected to receive the defined content type for 'SPNEGO not supported' custom error page but it was not received.",
                       response.contains("text/plain"));
            assertTrue("Expected to receive the defined page encoding for 'SPNEGO not supported' custom error page but it was not received.",
                       response.contains("US-ASCII"));
        } else {
            assertTrue("Expected to receive the default error message for 'SPNEGO not supported' but it was not received.",
                       response.contains(MessageConstants.SPNEGO_NOT_SUPPORTED_CWWKS4306E));
        }
    }

    @Override
    public void spnegoTokenNotFound(String response) {
        assertTrue("The response should have had a the spnego token but it was not there.", response.contains("token:"));
    }

//////////////////////////////////////
//SPNEGO GSS Credential Expectations//
//////////////////////////////////////
//SHOULD I COMBINE WITH responseShouldContainCorrectGSSCredOwner
    @Override
    public void responseShouldContaiGSSCredentials(String response) {
//        assertTrue("Response should contain GSS credentials but none were found.", response.contains(SPNEGOConstants.GSS_CREDENTIAL_STRING));
    }

    @Override
    public void responseShouldContainCorrectGSSCredOwner(String response) {
        assertTrue("GSS credentials did not have the correct \"Owner\" value of \"" + SPNEGOConstants.OWNER_STRING + InitClass.COMMON_TOKEN_USER + "\"",
                   response.contains(SPNEGOConstants.OWNER_STRING + InitClass.COMMON_TOKEN_USER));

    }

    @Override
    public void spnegoInvalidCredential(GSSException e) {
        assertTrue("Expected exception was thrown but error message was unexpected.", e.getMajorString().contains("Invalid credentials"));
    }

///////////////////////////////////
//s4u2Proxy and Self expectations//
///////////////////////////////////
    @Override
    public void s4u2Proxy_NotEnabled(String response) {
        assertTrue("The response should have had CWWKS4343E error message saying S4U2proxy is not enabled.",
                   response.contains("CWWKS4343E"));
    }

    @Override
    public void s4u2Proxy_NoClassDefFoundError(String response) {
        assertTrue("The response should have had a NoClassDefFoundError but it did not.",
                   response.contains("java.lang.NoClassDefFoundError: com.ibm.websphere.security.s4u2proxy.SpnegoHelper"));

    }

    @Override
    public void s4u2_responseContainsToken(String response, String responseCheckForSubjectTest) {
        s4u2_responseContainsToken(response, responseCheckForSubjectTest, false);
    }

    @Override
    public void s4u2_responseContainsToken(String response, String responseCheckForSubjectTest, boolean responseHasMultipleToken) {
        if (responseHasMultipleToken) {
            assertTrue("The response should have had SPNEGO token but did not",
                       response.contains("token1"));
            assertTrue("The response should have had SPNEGO token but did not",
                       response.contains("token2"));
            assertTrue("The response should have success message", response.contains(responseCheckForSubjectTest));
        } else {
            assertTrue("The response should have had SPNEGO token but did not", response.contains("token"));
            assertTrue("The response should have success message", response.contains(responseCheckForSubjectTest));
        }
    }

    @Override
    public void s4u2_responseFromBackendServer(String response, String user) {
        assertTrue("Did not find expected principal value in backend servlet response.", response.contains("Principal: " + user + "@" + InitClass.KDC_REALM));
    }

    @Override
    public void s4u2_responseContainsDifferentTokens(String response) {
        assertTrue("The response should have had a the spnego token but it was not there.", response.contains("We were able to obtain the following spnego token:"));
        assertTrue("The response should have had a second spnego token but it was not there.", response.contains("We were able to obtain the following second spnego token:"));
        assertTrue("The tokens were supposed to be different but they weren't.", response.contains("Spnego Token 1 and Spnego Token 2 are different"));
    }

    @Override
    public void s4u2_validateKerberosAndGSSCred(String response) {
        assertTrue("The response should have had the GSSName listed", response.contains("GSSCredential name is: user1" + "@" + InitClass.KDC_REALM));
        assertTrue("The response should have had the Krb5ProxyCredential listed", response.contains("Krb5ProxyCredential"));
        assertTrue("The response should have had Self=Kerberos listed", response.contains("Self=Kerberos credential"));
        assertTrue("The response should have had proper ticket listed listed", response.contains("Ticket=com.ibm.security.krb5.internal.Ticket"));
        assertTrue("The response should have had Client listed", response.contains("Client=user1" + "@" + InitClass.KDC_REALM));
    }

    @Override
    public void s4u2_validateNegativeResponse(String response) {
        assertTrue("The response should have Null SPN 1 success message", response.contains(responseCheckForNullSpn1));
        assertTrue("The response should have Null SPN 2 success message", response.contains(responseCheckForNullSpn2));
        assertTrue("The response should have Null UPN 1 success message", response.contains(responseCheckForNullUpn1));
        assertTrue("The response should have Null UPN 2 success message", response.contains(responseCheckForNullUpn2));
        assertTrue("The response should have Null Subject 1 success message", response.contains(responseCheckForNullSubject1));
        assertTrue("The response should have Null UserId 1 success message", response.contains(responseCheckForNullUserId1));
        assertTrue("The response should have Null UserId 2 success message", response.contains(responseCheckForNullUserId2));
        assertTrue("The response should have Null Password 1 success message", response.contains(responseCheckForNullPassword1));
        assertTrue("The response should have Null Password 2 success message", response.contains(responseCheckForNullPassword2));
        assertTrue("The response should have Bad UserId 1 success message", response.contains(responseCheckForBadUserId1));
        assertTrue("The response should have Bad UserId 2 success message", response.contains(responseCheckForBadUserId2));
        assertTrue("The response should have Bad SPN 1 success message", response.contains(responseCheckForBadSpn1));
        assertTrue("The response should have Bad SPN 2 success message", response.contains(responseCheckForBadSpn2));
    }

    @Override
    public void s4u2_checkForFailedSubject(String response, String responseCheckForFailure) {
        assertTrue("The response should have failure message", response.contains(responseCheckForFailure));
    }

    @Override
    public void s4u2_krbCredNotPresent(String response) {
        assertFalse("The response should not have the Krb5ProxyCredential listed", response.contains("Krb5ProxyCredential"));
        assertFalse("The response should have had Self=Kerberos listed", response.contains("Self=Kerberos credential"));
    }

}
