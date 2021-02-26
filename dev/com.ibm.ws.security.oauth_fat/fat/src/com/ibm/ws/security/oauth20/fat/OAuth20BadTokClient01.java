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
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class OAuth20BadTokClient01 extends OAuth20TestCommon {

    private static final Class<?> thisClass = OAuth20BadTokClient01.class;

    private static String authenticationError = "Bearer authentication required"; //"Basic authentication required";

    /**
     * TestDescription:
     *
     * This test case tests the authorization grant type of
     * "authorization code". In this scenario, the client obtains authorization
     * code by invoking the authorization endpoint and sends the authorization
     * code to the token endpoint to get the access token. The client then
     * invokes a protected resource by sending the access token. In this
     * sceanrio, the "autoauthz" paramter is set to true which will bypass the
     * consent or approval form.
     *
     */

    @Before
    public void setupBeforeTest() throws Exception {
        commonSetup("com.ibm.ws.security.oauth-2.0.fat");
    }

    @Test
    @AllowedFFDC(value = { "com.ibm.websphere.security.WSSecurityException" })
    public void OAuth20BadToken() throws Exception {

        final String thisMethod = "OAuth20BadToken";
        try {

            sslSetup();

            Log.info(thisClass, thisMethod, "Begin");

            WebRequest request = null;
            WebResponse response = null;
            String respReceived = null;

            Log.info(thisClass, thisMethod, "First client URL: " + firstClientUrl);
            // Create the conversation object which will maintain state for us
            WebConversation wc = new WebConversation();

            // Start the OAuth request by invoking the client
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
            Log.info(thisClass, thisMethod, "Response1 from Authorization server: " + respReceived);

            WebForm form2 = response.getForms()[0];
            response = form2.submit();
            request = form2.getRequest();
            Log.info(thisClass, thisMethod, "Target URL1 of request: " + request.getURL());
            respReceived = response.getText();
            Log.info(thisClass, thisMethod, "Response2 from Authorization server: " + respReceived);

            // Should receive prompt for resource owner to login

            WebForm form3 = response.getForms()[0];

            String uname1 = form3.getParameterValue("j_username");
            Log.info(thisClass, thisMethod, "Response3 uname: " + uname1);

            form3 = fillLoginForm(form3);

            // Submit login form
            HttpUnitOptions.setExceptionsThrownOnScriptError(false);
            request = form3.getRequest();
            Log.info(thisClass, thisMethod, "Target URL of request: " + request.getURL());

            printAllCookies(wc);

            response = wc.getResponse(request);
            respReceived = response.getText();
            Log.info(thisClass, thisMethod, "Response3 from Authorization server: " + respReceived);
            // Check if we got authorization code
            assertTrue("Did not receive authorization code", respReceived.contains(recvAuthCode));

            // Check if we got access token
            assertTrue("Did not receive access token", respReceived.contains(recvAccessToken));

            // Extract access token from response

            String tokenLine = respReceived.substring(respReceived.indexOf(recvAccessToken), respReceived.indexOf("refresh_token"));

            Log.info(thisClass, thisMethod, "Token line: " + tokenLine);

            String tokenValue = tokenLine.substring(recvAccessToken.length() + 1, tokenLine.indexOf("\","));

            Log.info(thisClass, thisMethod, "Token Value: " + tokenValue);

            // Invoke protected resource

            request = new GetMethodWebRequest(protectedResource);

            // Make access token invalid

            tokenValue = tokenValue.substring(5) + "extra";
            Log.info(thisClass, thisMethod, "New Token Value: " + tokenValue);
            request.setParameter("access_token", tokenValue);

            // Invoke app by sending invalid access token
            response = wc.getResponse(request);
            respReceived = response.getText();

            // If we come here, the test has failed.
            Log.info(thisClass, thisMethod, "Did not get expected exception.");
            Log.info(thisClass, thisMethod, respReceived);
            fail("Did NOT get expected exception");

        } catch (Exception e) {
            Log.info(thisClass, thisMethod, "Exception occurred:");
            String exception = new String();

            if ((e.getCause() != null)) {
                exception = e.getCause().getMessage();
            } else {
                exception = e.getMessage();
            }
            // Check if we got the correct exception.
            if (exception.contains(authenticationError)) {
                Log.error(thisClass, thisMethod, e, "Exception: ");
                Log.info(thisClass, thisMethod, "Received authentication error as expected");
                Log.info(thisClass, thisMethod, "Test Passed!");
            } else {
                Log.info(thisClass, thisMethod, "Unexpected exception occurred:");
                Log.error(thisClass, thisMethod, e, "Exception: ");
                throw e;
            }
        }

    }

}
