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
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class OAuth20PublicClient02 extends OAuth20TestCommon {

    private static final Class<?> thisClass = OAuth20PublicClient02.class;

    /**
     * TestDescription:
     * - Client obtains access token by invoking front end application which invokes the token endpoint.
     * - Grant type: resource owner password credentials
     * - Public client; client secret not sent to authorization server
     * - Authorization server used as intermediary to obtain access token from token endpoint
     * - autoauthz: true (resource owner does not receive consent form from authorization server)
     */

    @Before
    public void setupBeforeTest() throws Exception {
        commonSetup("com.ibm.ws.security.oauth-2.0.fat");
    }

    @Test
    public void testOAuthResourceOwnerPwdCredentials() throws Exception {

        final String thisMethod = "testOAuthResourceOwnerPwdCredentials";
        try {

            sslSetup();

            Log.info(thisClass, thisMethod, "Begin");

            firstClientUrl = httpStart + "/" + Constants.OAUTHCLIENT_APP + "/resourceowner.jsp";
            authorizeEndpt = httpsStart + "/oauth2/endpoint/OAuthConfigPublic/authorize";
            tokenEndpt = httpsStart + "/oauth2/endpoint/OAuthConfigPublic/token";
            protectedResource = httpsStart + "/oauth2tai/sniffing";
            clientName = "pclient02";
            clientID = "pclient02";
            clientSecret = "";

            WebRequest request = null;
            WebResponse response = null;
            String respReceived = null;

            // Create the conversation object which will maintain state for us
            WebConversation wc = new WebConversation();

            // Start the OAuth request by invoking the client
            Log.info(thisClass, thisMethod, "First client URL: " + firstClientUrl);
            request = new GetMethodWebRequest(firstClientUrl);

            // Invoke the client
            response = wc.getResponse(request);

            // Read the response page from client jsp
            respReceived = response.getText();
            Log.info(thisClass, thisMethod, "Response from OAuth client: " + respReceived);

            WebForm form = fillClientForm2(response.getForms()[0]);

            // Submit the request
            response = form.submit();
            respReceived = response.getText();
            Log.info(thisClass, thisMethod, "Response1 from Authorization server: " + respReceived);

            // Check if we received access token
            assertTrue("Did not receive access token", respReceived.contains(recvAccessToken));

            // Check if protected app was invoked successfully

            assertTrue("Could not invoke protected application:", respReceived.contains(snoopServlet));

            Log.info(thisClass, thisMethod, "Test Passed!");

        } catch (Exception e) {

            Log.info(thisClass, thisMethod, "Exception occurred:");
            Log.error(thisClass, thisMethod, e, "Exception: ");
            throw e;
        }

    }

}
