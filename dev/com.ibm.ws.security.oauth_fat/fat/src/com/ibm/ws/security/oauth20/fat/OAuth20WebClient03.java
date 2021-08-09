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
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class OAuth20WebClient03 extends OAuth20TestCommon {

    private static final Class<?> thisClass = OAuth20WebClient03.class;

    /**
     * TestDescription:
     *
     * This test case performs a simple end-end OAuth flow, using httpunit to
     * simulate browser requests. In this scenario, a Web client invokes a front
     * end client application that obtains access token for the client using
     * WebSphere authorization server. The test uses the authorization grant
     * type "implicit" . In this scenario, the client uses the authorization
     * server as an intermediary to obtain the access token, without invoking
     * the token endpoint. The authorization server authenticates the resource
     * owner before issuing the access token. In this scenario, the autoauthz
     * parameter is set to true, so the resource owner does not receive the
     * consent form from the authorizarion server. The test verifies that the
     * Oauth code flow, using the authorization grant type of "implicit" works
     * correctly.
     *
     */

    @Before
    public void setupBeforeTest() throws Exception {
        commonSetup("com.ibm.ws.security.oauth-2.0.fat");
    }

    @Test
    public void testOAuthImplicit() throws Exception {

        final String thisMethod = "testOAuthImplicit";
        try {

            sslSetup();

            Log.info(thisClass, thisMethod, "Begin");

            clientName = "client03";
            clientID = "client03";
            autoauthz = "true";

            WebRequest request = null;
            WebResponse response = null;
            String respReceived = null;

            // Create the conversation object which will maintain state for us
            final WebConversation wc = new WebConversation();

            // Invoke authorization server
            request = fillAuthorizationForm(new GetMethodWebRequest(authorizeEndpt));

            Log.info(thisClass, thisMethod, "Target URL of first request: "
                                            + request.getURL());
            Log.info(thisClass, thisMethod, "Filled in form: ");
            System.out.println(request);
            response = wc.getResponse(request);
            respReceived = response.getText();

            Log.info(thisClass, thisMethod,
                     "Response1 received from Auth server:   ");
            Log.info(thisClass, thisMethod, respReceived);

            // Make sure expected response is received
            assertTrue("Did not get expected Login prompt", respReceived.contains(loginPrompt));

            // Set user ID and password in login form
            WebForm form = fillLoginForm(response.getForms()[0]);
            Log.info(thisClass, thisMethod, "id : "
                                            + form.getParameterValue("j_username"));
            Log.info(thisClass, thisMethod, "password : "
                                            + form.getParameterValue("j_password"));

            // Submit login form
            HttpUnitOptions.setExceptionsThrownOnScriptError(false);
            request = form.getRequest();
            Log.info(thisClass, thisMethod, "Filled in form: ");
            System.out.println(request);
            response = wc.getResponse(request);

            /*
             * Redirect URI, with fragment should contain access token The URL
             * from where the response was sent is the redirect URL, with
             * fragment.
             */

            String respUrl = response.getURL().toString();

            Log.info(thisClass, thisMethod, "URL that sent the response:   ");
            Log.info(thisClass, thisMethod, respUrl);

            Log.info(thisClass, thisMethod, "Redirect contents: ");
            Log.info(thisClass, thisMethod, response.getText());

            // If the redirect URI contains access token, the test has passed.
            assertTrue("Did not receive access token in redirect URI ", respUrl.contains(redirectAccessToken));

            Log.info(thisClass, thisMethod, "Test Passed!");
        } catch (Exception e) {

            Log.info(thisClass, thisMethod, "Exception occurred:");
            Log.error(thisClass, thisMethod, e, "Exception: ");
            throw e;
        }

    }

}
