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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.ibm.websphere.simplicity.log.Log;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

public class OAuth20Client01Common extends OAuth20TestCommon {

    private static final Class<?> thisClass = OAuth20Client01Common.class;

    public void testOAuthDerbyCodeFlow() throws Exception {
        this.clientName = "dclient01";
        this.clientID = "dclient01";

        // setup up the Derby DataBase entries
        setupDerbyEntries("testSchema1");

        testOAuthCommonCodeFlow();
    }

    public void testOAuthCustomStoreCodeFlow() throws Exception {
        this.clientName = "dclient01";
        this.clientID = "dclient01";

        // setup up the MongoDB DataBase entries
        setupMongDBEntries("testSchema1");

        testOAuthCommonCodeFlow();
    }

    // NOTE: Should only allow java.sql.SQLRecoverableException. Other AllowedFFDCs added here TEMPORARILY
    // to stop the build being red due to defect 98925. When that is fixed, remove the other entries!!
    public void testOAuthCommonCodeFlow() throws Exception {

        final String thisMethod = "testOAuthCommonCodeFlow";
        try {

            sslSetup();

            Log.info(thisClass, thisMethod, "Begin");

            WebRequest request = null;
            WebResponse response = null;
            String respReceived = null;

            authorizeEndpt = httpsStart
                             + "/oauth2/endpoint/OAuthConfigDerby/authorize";
            tokenEndpt = httpsStart + "/oauth2/endpoint/OAuthConfigDerby/token";
            protectedResource = httpsStart + "/oauth2tai/ssodemo";

            final WebConversation wc = new WebConversation();

            // Start the OAuth request by invoking the client
            Log.info(thisClass, thisMethod, "firstClientUrl: " + firstClientUrl);
            request = new GetMethodWebRequest(firstClientUrl);

            // Invoke the client
            response = wc.getResponse(request);

            // Read the response page from client jsp
            respReceived = response.getText();
            Log.info(thisClass, thisMethod, "Response from OAuth client: " + respReceived);

            WebForm form = fillClientForm(response.getForms()[0]);

            // Submit the request
            response = form.submit();
            respReceived = response.getText();
            Log.info(thisClass, thisMethod, "Response1 from Authorization server: "
                                            + respReceived);

            WebForm form2 = response.getForms()[0];
            response = form2.submit();
            request = form2.getRequest();
            Log.info(thisClass, thisMethod, "Target URL of request: " + request.getURL());
            respReceived = response.getText();
            System.out.println("Response2 from Authorization server: "
                               + respReceived);

            // Should receive prompt for resource owner to login
            assertTrue("Received an empty getForms() ", response.getForms().length != 0);

            WebForm form3 = response.getForms()[0];

            String uname1 = form3.getParameterValue("j_username");
            Log.info(thisClass, thisMethod, "Response3 uname: " + uname1);

            form3 = fillLoginForm(form3);

            // Submit login form
            HttpUnitOptions.setExceptionsThrownOnScriptError(false);
            request = form3.getRequest();
            Log.info(thisClass, thisMethod, "Target URL of request: " + request.getURL());
            response = wc.getResponse(request);
            respReceived = response.getText();
            Log.info(thisClass, thisMethod, "Response3 from Authorization server: "
                                            + respReceived);
            // Check if we got authorization code
            assertTrue("Did not receive authorization code", respReceived.contains(recvAuthCode));

            // Check if we got access token
            assertTrue("Did not receive access token", respReceived.contains(recvAccessToken));

            // Extract access token from response
            // This is also testing that refresh token is not issued since client doesn't not have permission for refresh token
            String tokenLine = respReceived.substring(respReceived.indexOf(recvAccessToken), respReceived.indexOf("token_type"));

            Log.info(thisClass, thisMethod, "Token line: " + tokenLine);

            String tokenValue = tokenLine.substring(
                                                    recvAccessToken.length() + 1, tokenLine.indexOf("\","));

            Log.info(thisClass, thisMethod, "Token Value: " + tokenValue);

            // Invoke protected resource

            request = new GetMethodWebRequest(protectedResource);
            request.setParameter("access_token", tokenValue);

            response = wc.getResponse(request);
            respReceived = response.getText();
            Log.info(thisClass, thisMethod, "Response from protected app: " + respReceived);

            // Make sure we receive the app
            assertTrue("Could not invoke protected application", respReceived.contains(appTitle));

            // Response should not have an ltpa token
            assertFalse("Response has an ltpa token, but should not", hasLTPAToken(response));

            Log.info(thisClass, thisMethod, "Test Passed!");

        } catch (Exception e) {

            Log.info(thisClass, thisMethod, "Exception occurred:");
            Log.error(thisClass, thisMethod, e, "Exception: ");

            throw e;
        }

    }

}
