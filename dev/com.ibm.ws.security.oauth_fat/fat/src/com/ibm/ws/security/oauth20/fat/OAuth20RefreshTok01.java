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
package com.ibm.ws.security.oauth20.fat;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class OAuth20RefreshTok01 extends OAuth20TestCommon {

    private static final Class<?> thisClass = OAuth20RefreshTok01.class;

    /**
     * TestDescription:
     *
     * In this scenario, an OAuth client first gets a refresh token (along with
     * the access token) using "resource owner credentials" as the authorization
     * grant type and then requests a new access token using the refresh token.
     * The test verifies that an access token can be obtained successfully by
     * using a refresh token.
     *
     */

    @Before
    public void setupBeforeTest() throws Exception {
        commonSetup("com.ibm.ws.security.oauth-2.0.fat");
    }

    @Test
    public void testOAuth20RefreshToken() throws Exception {

        HttpUnitOptions.setExceptionsThrownOnErrorStatus(false);

        final String thisMethod = "testOAuth20RefreshToken";
        try {

            sslSetup();

            Log.info(thisClass, thisMethod, "Begin");

            firstClientUrl = httpStart + "/" + Constants.OAUTHCLIENT_APP + "/resourceowner.jsp";
            clientName = "client04";
            clientID = "client04";

            WebRequest request = null;
            WebResponse response = null;
            String respReceived = null;

            // Create the conversation object which will maintain state for us
            WebConversation wc = new WebConversation();

            // Start the OAuth request by invoking the client
            Log.info(thisClass, thisMethod, "First client URL: "
                                            + firstClientUrl);
            request = new GetMethodWebRequest(firstClientUrl);

            // Invoke the client
            response = wc.getResponse(request);

            // Read the response page from client jsp
            respReceived = response.getText();
            Log.info(thisClass, thisMethod, "Response from OAuth client: "
                                            + respReceived);

            WebForm form = fillClientForm2(response.getForms()[0]);

            // Submit the request
            response = form.submit();
            respReceived = response.getText();
            Log.info(thisClass, thisMethod,
                     "Response1 from Authorization server: " + respReceived);
            // Check if we received access token
            assertTrue("Did not receive access token", respReceived.contains(recvAccessToken));

            String tokenLine = respReceived.substring(respReceived.indexOf(refreshToken), respReceived.indexOf("TITLE"));

            Log.info(thisClass, thisMethod, "Token line: " + tokenLine);

            String tokenValue = tokenLine.substring(refreshToken.length(),
                                                    tokenLine.indexOf("<br"));
            Log.info(thisClass, thisMethod, "Token Value: " + tokenValue);

            // Now, invoke refresh.jsp
            request = new GetMethodWebRequest(refreshTokUrl);

            // Ignore javascript exceptions
            HttpUnitOptions.setExceptionsThrownOnScriptError(false);

            // Invoke the client
            response = wc.getResponse(request);
            respReceived = response.getText();
            Log.info(thisClass, thisMethod, "Response from refresh jsp: "
                                            + respReceived);

            WebForm form2 = response.getForms()[0];

            // Set form values
            form2.setParameter("client_id", clientID);
            form2.setParameter("client_secret", clientSecret);
            form2.setParameter("refresh_token", tokenValue);
            form2.setParameter("token_endpoint", tokenEndpt);
            form2.setParameter("scope", "scope1 scope2");

            response = form2.submit();
            respReceived = response.getText();
            Log.info(thisClass, thisMethod, "Response from token endpt: "
                                            + respReceived);

            // Check if we received access token
            assertTrue("Did not receive access token with refresh token",
                       respReceived.contains(recvAccessToken));

            // If we received access token from a refresh token, test has
            // passed.
            Log.info(thisClass, thisMethod, "Test Passed!");

        } catch (Exception e) {

            Log.info(thisClass, thisMethod, "Exception occurred:");
            Log.error(thisClass, thisMethod, e, "Exception: ");

            throw e;
        }

    }

}
