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
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class OAuth20UnRegClient02 extends OAuth20TestCommon {

    private static final Class<?> thisClass = OAuth20UnRegClient02.class;

    /**
     * TestDescription:
     *
     * This is a negative test case for the authorization grant type of
     * "authorization code", using OAuth provider that uses JDBC database for
     * storing registered clients. In this scenario, a non-registered client
     * tries to obtain authorization code from the authorization server. Since
     * the client is not registered, the request is expected to be rejected with
     * an appropriate exception. This test verifies that the OAuth authorization
     * server will reject any unregistered OAuth client if it tries to obtain
     * access token.
     *
     */

    @Before
    public void setupBeforeTest() throws Exception {
        commonSetup("com.ibm.ws.security.oauth-2.0.fat");
    }

    @Test
    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidClientException" })
    public void testOAuthUnRegJDBC() throws Exception {

        final String thisMethod = "testOAuthUnRegJDBC";
        try {

            sslSetup();

            Log.info(thisClass, thisMethod, "Begin");

            tokenEndpt = "/oauth2/endpoint/OAuthConfigDerby/token";
            protectedResource = "/oauth2tai/ssodemo";
            clientName = "unreg02";
            clientID = "unreg02";
            String clientNotFound = "CWOAU0061E: The OAuth service provider could not find the client because the client name is not valid. Contact your system administrator to resolve the problem.";
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
            // Should receive exception that client is not found
            assertTrue("Did not receive expected exception:" + clientNotFound,
                       respReceived.contains(clientNotFound));

            Log.info(thisClass, thisMethod, "Test Passed!");

        } catch (Exception e) {

            Log.info(thisClass, thisMethod, "Exception occurred:");
            Log.error(thisClass, thisMethod, e, "Exception: ");
            throw e;
        }

    }

}
