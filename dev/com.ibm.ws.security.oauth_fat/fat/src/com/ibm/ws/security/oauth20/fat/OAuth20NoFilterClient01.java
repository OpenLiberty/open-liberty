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

import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

public class OAuth20NoFilterClient01 extends OAuth20TestCommon {

    private static final Class<?> thisClass = OAuth20NoFilterClient01.class;
    private static final String authorizationError = "Error 401";

    /**
     * TestDescription:
     *
     * This is a negative test case for the authorization grant type of
     * "authorization code", using OAuth provider that uses XML file for storing
     * registered clients. There is no TAI filter property defined for the OAuth
     * provider that is used in this scenario. Since the filter property is not
     * defined for this OAuth request, the request is expected to be rejected
     * with an internal server exception. This test verifies that the OAuth
     * filter property (provider_<id>.filter) is required for processing the
     * OAuth request.
     *
     */

    @Before
    public void setupBeforeTest() throws Exception {
        commonSetup("com.ibm.ws.security.oauth-2.0.fat");
    }

    @Test
    public void testOAuthNoFilter() throws Exception {

        final String thisMethod = "testOAuthNoFilter";
        try {

            sslSetup();

            Log.info(thisClass, thisMethod, "Begin");

            authorizeEndpt = httpsStart + "/oauth2/endpoint/OAuthConfigNoFilter/authorize";
            tokenEndpt = httpsStart + "/oauth2/endpoint/OAuthConfigNoFilter/token";
            protectedResource = httpsStart + "/oauth2tai/snooping";
            clientName = "nclient01";
            clientID = "nclient01";

            WebRequest request = null;
            WebResponse response = null;
            String respReceived = null;

            // Create the conversation object which will maintain state for us
            WebConversation wc = new WebConversation();

            Log.info(thisClass, thisMethod, "First client URL: " + firstClientUrl);
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
            HttpUnitOptions.setExceptionsThrownOnErrorStatus(true);
            request = form3.getRequest();
            Log.info(thisClass, thisMethod, "Target URL of request: " + request.getURL());

            printAllCookies(wc);

            response = wc.getResponse(request);
            respReceived = response.getText();
            Log.info(thisClass, thisMethod, "Status code from Authorization server: " + response.getResponseCode());
            Log.info(thisClass, thisMethod, "Response3 from Authorization server:\n " + respReceived);

            // Check if we got the correct exception.
            if (respReceived.contains(authorizationError)) {
                Log.info(thisClass, thisMethod, "Received 401 Authorization Error as expected");
                Log.info(thisClass, thisMethod, "Test Passed!");
            } else {
                fail("Did NOT get expected exception");
            }
        } catch (Exception e) {
            String exception = new String();
            if ((e.getCause() != null)) {
                exception = e.getCause().getMessage();
            } else {
                exception = e.getMessage();
            }
            Log.info(thisClass, thisMethod, "Unexpected Exception occurred:", exception);
            Log.error(thisClass, thisMethod, e, "Exception: ");
            throw e;
        }
    }
}
