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

import java.net.UnknownHostException;

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
public class OAuth20PublicClient01 extends OAuth20TestCommon {

    private static final Class<?> thisClass = OAuth20PublicClient01.class;

    /**
     * TestDescription:
     * - Client obtains access token by invoking front end application which invokes the token endpoint.
     * - Grant type: authorization code
     * - Public client; client secret not sent to authorization server
     * - Authorization server used as intermediary to obtain access token from token endpoint
     * - Authorization server authenticates resource owner
     * - Server issues authorization code and redirects it to client
     * - autoauthz: true (resource owner does not receive consent form from authorization server)
     *
     * The testing of the logout endpoint is less than exhaustive. An exhaustive test would require:
     * 1) a protected app on the rp that can call servletRequest.logout (to clear rp cookies.)
     * 2) then that app redirects to the OP logout endpoint (to clear ltpa cookie if present)
     * 3) then the protected resource is accessed again, and the user must re-authenticate.
     * (that path was confirmed manually)
     */

    @Before
    public void setupBeforeTest() throws Exception {
        commonSetup("com.ibm.ws.security.oauth-2.0.fat");
    }

    @Test
    public void testOAuthAuthCodeAndLogoutEndpoint() throws Exception {

        final String thisMethod = "testOAuthAuthCodeAndLogoutEndpoint";
        try {

            sslSetup();

            Log.info(thisClass, thisMethod, "Begin");

            authorizeEndpt = httpsStart + "/oauth2/endpoint/OAuthConfigPublic/authorize";
            tokenEndpt = httpsStart + "/oauth2/endpoint/OAuthConfigPublic/token";
            String logoutEndpt = httpsStart + "/oauth2/endpoint/OAuthConfigPublic/logout";
            protectedResource = httpsStart + "/oauth2tai/sniffing";
            clientName = "pclient01";
            clientID = "pclient01";
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

            WebForm form = fillClientForm(response.getForms()[0]);

            // Set Client secret
            form.setParameter("client_secret", clientSecret);

            // Submit the request
            response = form.submit();
            respReceived = response.getText();
            Log.info(thisClass, thisMethod, "Response1 from Authorization server: " + respReceived);

            WebForm form2 = response.getForms()[0];
            response = form2.submit();
            request = form2.getRequest();
            Log.info(thisClass, thisMethod, "Target URL of request: " + request.getURL());
            respReceived = response.getText();
            Log.info(thisClass, thisMethod, "Response2 from Authorization server: " + respReceived);

            // Should receive prompt for resource owner to login
            assertTrue("Did not receive login form", respReceived.contains("j_username"));

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
            request.setParameter("access_token", tokenValue);

            response = wc.getResponse(request);
            respReceived = response.getText();

            // Make sure we receive the app
            assertTrue("Could not invoke protected application", respReceived.contains(appTitle));

            // Response should not have an ltpa token
            assertFalse("Response has an ltpa token, but should not", hasLTPAToken(response));

            // now go hit the logout endpoint on the OP, we should get a successful logout.

            Log.info(thisClass, thisMethod, "logout URL: " + logoutEndpt);

            // Start the OAuth request by invoking the client
            request = new GetMethodWebRequest(logoutEndpt);

            // Invoke the client
            response = wc.getResponse(request);

            respReceived = response.getText();
            Log.info(thisClass, thisMethod, "Response from OAuth logout endpoint: "
                                            + respReceived);
            assertTrue("did not get expected logout page ", respReceived.contains("Logout successful"));

            // now try endpoint with custom logout page defined, should get a unknown host exception
            Log.info(thisClass, thisMethod, "check custom logout page");
            String logoutEndpt2 = httpsStart + "/oauth2/endpoint/OAuthConfigNoFilter/logout";
            boolean caught = false;
            try {
                WebConversation wc2 = new WebConversation();
                WebRequest request2 = new GetMethodWebRequest(logoutEndpt2);
                WebResponse response2 = wc2.getResponse(request2);
                respReceived = response2.getText();
            } catch (UnknownHostException e) {
                caught = true;
            }
            assertTrue("did not catch expected UnknownHostException an invalid logoutRedirectURL is specified ", caught);

            Log.info(thisClass, thisMethod, "Test Passed!");

        } catch (Exception e) {

            Log.info(thisClass, thisMethod, "Exception occurred:");
            Log.error(thisClass, thisMethod, e, "Exception: ");
            throw e;
        }

    }

}
