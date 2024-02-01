/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class OAuth20DynamicConfig01 extends OAuth20TestCommon {

    private static final Class<?> thisClass = OAuth20DynamicConfig01.class;

    /**
     * TestDescription:
     *
     * This testcase performs a simple end-end OAuth flow with a provider that
     * is correctly defined - OAuthConfigSample. Then the provider is modified to
     * delete the necessary client, client01, and the same flow is attempted again.
     * The request should fail with a message that the client was not found. Then
     * the client is added back and the flow is attempted again, this one should
     * succeed.
     *
     */

    @Before
    public void setupBeforeTest() throws Exception {
        commonSetup("com.ibm.ws.security.oauth-2.0.fat", "CWWKG0033W");
    }

    @Test
    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidClientException" })
    public void testOAuthAuthBasicFlow() throws Exception {

        final String thisMethod = "testOAuthAuthBasicFlow";
        try {

            sslSetup();

            Log.info(thisClass, thisMethod, "Begin");

            WebRequest request = null;
            WebResponse response = null;
            String respReceived = null;

            // Create the conversation object which will maintain state for us
            WebConversation wc = new WebConversation();

            // Start the OAuth request by invoking the client
            Log.info(thisClass, thisMethod, "firstClientUrl: "
                                            + firstClientUrl);
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

            Log.info(thisClass, thisMethod, "First Pass of Test Passed!");

            // Now remove client01
            setServerConfiguration("noclient.server.xml");

            // Create the conversation object which will maintain state for us
            wc = new WebConversation();

            // Start the OAuth request by invoking the client
            Log.info(thisClass, thisMethod, "firstClientUrl: "
                                            + firstClientUrl);
            request = new GetMethodWebRequest(firstClientUrl);

            // the apps are restarted sometimes (not all of the time, so, we can't just wait for the restart) - retry if we get a 404 indicating that a restart hasn't completed yet
            for (int i = 0; i <= 6; i++) {
                try {
                    // Invoke the client
                    response = wc.getResponse(request);
                } catch (Exception e) {
                    if (e.getMessage().contains("404") && i < 6) {
                        Log.info(thisClass, thisMethod, "Config update not really completed yet - test apps have not fully restarted - will sleep for 30 seconds and try again");
                        Thread.sleep(30000);
                        // sleep and try again
                    } else {
                        throw (e);
                    }
                }
            }
            // Read the response page from client jsp
            respReceived = response.getText();
            Log.info(thisClass, thisMethod, "Response from OAuth client: "
                                            + respReceived);

            form = fillClientForm(response.getForms()[0]);

            Log.info(thisClass, thisMethod, "new prop:" + tokenEndpt);

            // Submit the request
            response = form.submit();
            respReceived = response.getText();
            Log.info(thisClass, thisMethod,
                     "Response1 from Authorization server: " + respReceived);

            form2 = response.getForms()[0];
            response = form2.submit();
            request = form2.getRequest();
            Log.info(thisClass, thisMethod, "Target URL of request: "
                                            + request.getURL());
            respReceived = response.getText();
            Log.info(thisClass, thisMethod,
                     "Response2 from Authorization server: " + respReceived);

            // Should not receive prompt for resource owner to login
            //comment out the following since we don't even present consent form when there is an error such as this...
            //Log.info(thisClass, thisMethod, "Before GetForm");
            //Log.info(thisClass, thisMethod, "forms", response.getForms());
            //System.out.flush();
            //System.out.println(response.getForms());
            //form3 = response.getForms()[0];
            //Log.info(thisClass, thisMethod, "After GetForm");

            //uname1 = form3.getParameterValue("j_username");
            //Log.info(thisClass, thisMethod, "Response3 uname: " + uname1);

            //form3 = fillLoginForm(form3);
            //uname1 = form3.getParameterValue("j_username");
            //Log.info(thisClass, thisMethod, "Response3 uname AFTER setting: "
            //                              + uname1);

            //request = form3.getRequest();
            //Log.info(thisClass, thisMethod, "Target URL of request: "
            //                              + request.getURL());
            //HttpUnitOptions.setExceptionsThrownOnScriptError(false);
            //response = wc.getResponse(request);
            //respReceived = response.getText();
            //Log.info(thisClass, thisMethod,
            //       "Response3 from Authorization server: " + respReceived);
            Log.info(thisClass, thisMethod, "Login response code is: "
                                            + response.getResponseCode());

            // Verify that client01 was not found
            assertTrue("Did not receive expected client01 not found response",
                       respReceived.contains("The OAuth service provider could not find the client because the client name is not valid. Contact your system administrator to resolve the problem."));

            Log.info(thisClass, thisMethod, "Second Pass of Test Passed!");

            // Now restore client01
            setServerConfiguration("original.server.xml");

            // Create the conversation object which will maintain state for us
            wc = new WebConversation();

            // Start the OAuth request by invoking the client
            Log.info(thisClass, thisMethod, "firstClientUrl: "
                                            + firstClientUrl);
            request = new GetMethodWebRequest(firstClientUrl);

            // Invoke the client
            response = wc.getResponse(request);

            // Read the response page from client jsp
            respReceived = response.getText();
            Log.info(thisClass, thisMethod, "Response from OAuth client: "
                                            + respReceived);

            form = fillClientForm(response.getForms()[0]);

            Log.info(thisClass, thisMethod, "new prop:" + tokenEndpt);

            // Submit the request
            response = form.submit();
            respReceived = response.getText();
            Log.info(thisClass, thisMethod,
                     "Response1 from Authorization server: " + respReceived);

            form2 = response.getForms()[0];
            response = form2.submit();
            request = form2.getRequest();
            Log.info(thisClass, thisMethod, "Target URL of request: "
                                            + request.getURL());
            respReceived = response.getText();
            Log.info(thisClass, thisMethod,
                     "Response2 from Authorization server: " + respReceived);

            // Should receive prompt for resource owner to login

            Log.info(thisClass, thisMethod, "Before GetForm");
            Log.info(thisClass, thisMethod, "forms", response.getForms());
            System.out.flush();
            System.out.println(response.getForms());
            form3 = response.getForms()[0];
            Log.info(thisClass, thisMethod, "After GetForm");

            uname1 = form3.getParameterValue("j_username");
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
            tokenLine = respReceived.substring(respReceived.indexOf(recvAccessToken), respReceived.indexOf("refresh_token"));

            Log.info(thisClass, thisMethod, "Token line: " + tokenLine);

            tokenValue = tokenLine.substring(
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

    /**
     * This is an internal method used to set the server.xml
     */
    private static void setServerConfiguration(String serverXML) throws Exception {
        // Update server.xml
        Log.info(thisClass, "setServerConfiguration", "setServerConfigurationFile to : " + serverXML);
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("/" + serverXML);
        Log.info(thisClass, "setServerConfiguration",
                 "waitForStringInLogUsingMark: CWWKG0017I: The server configuration was successfully updated.");
        server.waitForStringInLogUsingMark("CWWKG0017I");
    }
}