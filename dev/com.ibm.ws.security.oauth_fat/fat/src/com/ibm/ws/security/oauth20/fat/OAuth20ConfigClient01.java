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
public class OAuth20ConfigClient01 extends OAuth20TestCommon {

    private static final Class<?> thisClass = OAuth20ConfigClient01.class;

    /**
     * TestDescription:
     *
     * This scenario tests the authorization grant type of "auth code" when the TAI properties
     * are defined in the provider configuration file, instead of in the WAS security.xml.
     * In this scenario, the OAuth provider file contains the following TAI properties:
     * <parameter name="filter" type="tai" customizable="true">
     * <value>request-url%=snooping</value>
     * </parameter>
     * <parameter name="oauthOnly" type="tai" customizable="true">
     * <value>true</value>
     * </parameter>
     * The test verifies that the OAuth provider TAI properties can be processed successfully
     * from the provider configuration file.
     *
     */

    @Before
    public void setupBeforeTest() throws Exception {
        commonSetup("com.ibm.ws.security.oauth-2.0.fat");
    }

    @Test
    public void testOAuth20Config() throws Exception {

        final String thisMethod = "testOAuth20Config";
        try {

            sslSetup();

            Log.info(thisClass, thisMethod, "Begin");

            authorizeEndpt = httpsStart
                             + "/oauth2/endpoint/OAuthConfigTai/authorize";
            tokenEndpt = httpsStart + "/oauth2/endpoint/OAuthConfigTai/token";
            protectedResource = httpsStart + "/oauth2tai/snooping";

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
            Log.info(thisClass, thisMethod, "Response from OAuth client: "
                                            + respReceived);

            WebForm form = fillClientForm(response.getForms()[0]);

            // Submit the request
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

            // Should receive prompt for resource owner to login

            WebForm form3 = response.getForms()[0];

            String uname1 = form3.getParameterValue("j_username");
            Log.info(thisClass, thisMethod, "Response3 uname: " + uname1);

            form3 = fillLoginForm(form3);

            // Submit login form
            HttpUnitOptions.setExceptionsThrownOnScriptError(false);
            request = form3.getRequest();
            Log.info(thisClass, thisMethod, "Target URL of request: "
                                            + request.getURL());
            response = wc.getResponse(request);
            respReceived = response.getText();
            Log.info(thisClass, thisMethod,
                     "Response3 from Authorization server: " + respReceived);
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
