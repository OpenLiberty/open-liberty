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
public class OAuth20BadCredsClient01 extends OAuth20TestCommon {

    private static final Class<?> thisClass = OAuth20BadCredsClient01.class;

    /**
     * TestDescription:
     *
     * This test case tests the authorization grant type of
     * "authorization code". In this scenario, the client obtains authorization
     * code by invoking the authorization endpoint and sends the authorization
     * code to the token endpoint to get the access token. The client then
     * invokes a protected resource by sending the access token. In this
     * sceanrio, the "autoauthz" paramter is set to true which will bypass the
     * consent or approval form.
     *
     */

    @Before
    public void setupBeforeTest() throws Exception {
        commonSetup("com.ibm.ws.security.oauth-2.0.fat", "CWIML4537E");
    }

    @Test
    public void OAuth20BadCreds() throws Exception {

        final String thisMethod = "OAuth20BadCreds";
        try {

            sslSetup();

            Log.info(thisClass, thisMethod, "Begin");

            adminPswd = "badpswd1";
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
            Log.info(thisClass, thisMethod, "Target URL1 of request: "
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
            // We should get login prompt again.
            assertTrue("Did not receive Login form on bad password",
                       respReceived.contains(loginForm));

        } catch (Exception e) {
            Log.info(thisClass, thisMethod, "Exception occurred:");
            String exception = new String();

            if ((e.getCause() != null)) {
                exception = e.getCause().getMessage();
            } else {
                exception = e.getMessage();
            }
            // Check if we got the correct exception.
            Log.info(thisClass, thisMethod, "The exception from server:  "
                                            + exception);
            Log.error(thisClass, thisMethod, e, "Unexpected Exception: ");
            throw e;
        }

    }

}
