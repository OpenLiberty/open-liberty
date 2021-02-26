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

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class OAuth20CustomLoginForm extends OAuth20TestCommon {

    private static final Class<?> thisClass = OAuth20CustomLoginForm.class;

    /**
     * TestDescription:
     *
     * This testcase performs a simple end-end OAuth flow, using httpunit to
     * simulate browser requests. In this scenario, a Web client invokes a front
     * end client application that obtains access token for the client using
     * WebSphere authorization server. The test uses the authorization grant
     * type "authorization code". In this scenario, the client uses the
     * authorization server as an intermediary to obtain the authorization code
     * and then uses this authorization code to request access token from the
     * token endpoint. The authorization server authenticates the resource owner
     * before issuing the authorization code which is redirected to the client.
     * In this scenario, the autoauthz parameter is set to true, so the resource
     * owner does not receive the consent form from the authorizarion server.
     * The test verifies that the Oauth code flow, using the authorization grant
     * type of "authorization code" works correctly.
     *
     */

    @Before
    public void setupBeforeTest() throws Exception {
        commonSetup("com.ibm.ws.security.oauth-2.0.loginform.fat");
    }

    @Test
    public void testOAuthCustomLoginForm() throws Exception {

        final String thisMethod = "testOAuthCustomLoginForm";
        try {

            sslSetup();

            Log.info(thisClass, thisMethod, "Begin");

            WebRequest request = null;
            WebResponse response = null;
            String respReceived = null;

            // Create the conversation object which will maintain state for us
            final WebConversation wc = new WebConversation();

            // Start the OAuth request by invoking the client
            Log.info(thisClass, thisMethod, "firstClientUrl: " + firstClientUrl);
            request = new GetMethodWebRequest(firstClientUrl);

            // Invoke the client
            response = wc.getResponse(request);

            // Read the response page from client jsp
            respReceived = response.getText();
            Log.info(thisClass, thisMethod, "Response from OAuth client: "
                                            + respReceived);

            WebForm form = fillClientForm(response.getForms()[0]);

            Log.info(thisClass, thisMethod, "new prop:" + tokenEndpt);

            // Submit the request
            // Custom OAuth Login
            response = form.submit();
            respReceived = response.getText();
            Log.info(thisClass, thisMethod,
                     "Response1 from Authorization server: " + respReceived);

            WebForm form2 = response.getForms()[0];
            response = form2.submit();
            request = form2.getRequest();
            Log.info(thisClass, thisMethod, "Target URL of request: "
                                            + request.getURL());
            respReceived = response.getText();
            Log.info(thisClass, thisMethod,
                     "Response2 from Authorization server: " + respReceived);

            // Verify that we got the custom login form
            assertTrue("Did not receive custom login form",
                       respReceived.contains("Custom OAuth Login"));

            Log.info(thisClass, thisMethod, "Before GetForm");
            Log.info(thisClass, thisMethod, "forms", response.getForms());
            System.out.flush();
            System.out.println(response.getForms());
            WebForm form3 = response.getForms()[0];
            Log.info(thisClass, thisMethod, "After GetForm");

            String uname1 = form3.getParameterValue("j_username");
            Log.info(thisClass, thisMethod, "Response3 uname: " + uname1);

            form3 = fillLoginForm(form3);
            uname1 = form3.getParameterValue("j_username");
            Log.info(thisClass, thisMethod, "Response3 uname AFTER setting: "
                                            + uname1);

            request = form3.getRequest();
            Log.info(thisClass, thisMethod, "Target URL of request: "
                                            + request.getURL());
            HttpUnitOptions.setExceptionsThrownOnScriptError(false);
            response = wc.getResponse(request);
            respReceived = response.getText();
            Log.info(thisClass, thisMethod,
                     "Response3 from Authorization server: " + respReceived);
            Log.info(thisClass, thisMethod, "Login response code is: "
                                            + response.getResponseCode());

            // Check if we got authorization code
            assertTrue("Did not receive authorization code", respReceived.contains(recvAuthCode));

            // Check if we got access token
            assertTrue("Did not receive access token", respReceived.contains(recvAccessToken));

            // Extract access token from response
            String tokenLine = respReceived.substring(respReceived.indexOf(recvAccessToken), respReceived.indexOf("refresh_token"));

            Log.info(thisClass, thisMethod, "Token line: " + tokenLine);

            String tokenValue = tokenLine.substring(
                                                    recvAccessToken.length() + 1, tokenLine.indexOf("\","));

            Log.info(thisClass, thisMethod, "Token Value: " + tokenValue);

            // Invoke protected resource
            request = new GetMethodWebRequest(protectedResource);
            request.setParameter("access_token", tokenValue);

            response = wc.getResponse(request);
            respReceived = response.getText();
            Log.info(thisClass, thisMethod, "Response from protected app: "
                                            + respReceived);

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
