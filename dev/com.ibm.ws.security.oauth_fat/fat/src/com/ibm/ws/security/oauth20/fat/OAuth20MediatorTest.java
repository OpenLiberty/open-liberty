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

import static org.junit.Assert.assertNotNull;
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
public class OAuth20MediatorTest extends OAuth20TestCommon {

    private static final Class<?> thisClass = OAuth20MediatorTest.class;

    /**
     * TestDescription:
     *
     * This testcase performs a simple end-end OAuth flow with a provider that
     * has a custom mediator plugin. The custom mediator will fail all requests,
     * The test is designed to verify that the mediator is loaded and invoked.
     */

    @Before
    public void setupBeforeTest() throws Exception {
        commonSetup("com.ibm.ws.security.oauth-2.0.fat");
        assertNotNull("The mediator shared lib did not activate",
                      server.waitForStringInLog("CWWKS1402I.*OAuthMediatorProvider"));
    }

    @Test
    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20MediatorException" })
    public void testMediatorPlugin() throws Exception {
        HttpUnitOptions.setExceptionsThrownOnErrorStatus(false);

        final String thisMethod = "testMediatorPlugin";
        try {

            sslSetup();

            Log.info(thisClass, thisMethod, "Begin");

            authorizeEndpt = httpsStart + "/oauth2/endpoint/OAuthMediatorProvider/authorize";
            tokenEndpt = httpsStart + "/oauth2/endpoint/OAuthMediatorProvider/token";
            protectedResource = httpsStart + "/oauth2tai/snorking";
            clientName = "mediatorclient";
            clientID = "mediatorclient";

            WebRequest request = null;
            WebResponse response = null;
            String respReceived = null;

            // Create the conversation object which will maintain state for us
            final WebConversation wc = new WebConversation();

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

            // Check if we got authorization code
            assertTrue("Did not receive authorization code", respReceived.contains(recvAuthCode));

            // Check if we got access token
            assertTrue("Mediator did not fail the request", respReceived.contains("test deliberate fail"));

            Log.info(thisClass, thisMethod, "Test Passed!");

        } catch (Exception e) {

            Log.info(thisClass, thisMethod, "Exception occurred:");
            Log.error(thisClass, thisMethod, e, "Exception: ");

            throw e;
        }
    }
}