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
public class OAuth20PublicClient03 extends OAuth20TestCommon {

    private static final Class<?> thisClass = OAuth20PublicClient03.class;

    /**
     * TestDescription:
     *
     * This test case performs a simple end-end OAuth flow, using httpunit to
     * simulate browser requests. In this scenario, a Web client invokes a front
     * end client application that obtains access token for the client using
     * WebSphere authorization server. The test uses the authorization grant
     * type "client credentials" and OAuth public client. For a public client,
     * the client secret is not sent to the authorization server. In this
     * scenario, the client uses the authorization server as an intermediary to
     * obtain the access token from the token endpoint by sending the resource
     * owner's credentials. In this scenario, the autoauthz parameter is set to
     * true, so the resource owner does not receive the consent form from the
     * authorizarion server. The test verifies that the Oauth code flow, using
     * the authorization grant type of "client credentials" works correctly for
     * a public client.
     *
     */

    @Before
    public void setupBeforeTest() throws Exception {
        commonSetup("com.ibm.ws.security.oauth-2.0.fat");
    }

    @Test
    public void testOAuthClientCreds() throws Exception {

        final String thisMethod = "testOAuthClientCreds";
        try {

            sslSetup();

            Log.info(thisClass, thisMethod, "Begin");

            firstClientUrl = httpStart + "/" + Constants.OAUTHCLIENT_APP + "/clientcred.jsp";
            authorizeEndpt = httpsStart
                             + "/oauth2/endpoint/OAuthConfigPublic/authorize";
            tokenEndpt = httpsStart
                         + "/oauth2/endpoint/OAuthConfigPublic/token";
            protectedResource = httpsStart + "/oauth2tai/sniffing";
            clientName = "user2";
            clientID = "user2";
            clientSecret = "security";
            adminUser = null;
            adminPswd = null;
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
            // Check if we received both scope
            assertTrue("Did not receive scope1", respReceived.contains("scope1"));
            assertTrue("Did not receive scope2", respReceived.contains("scope2"));

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
