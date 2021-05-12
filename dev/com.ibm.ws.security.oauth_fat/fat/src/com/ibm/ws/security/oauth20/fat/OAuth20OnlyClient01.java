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
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class OAuth20OnlyClient01 extends OAuth20TestCommon {

    private static final Class<?> thisClass = OAuth20OnlyClient01.class;

    static private String basicAuthReqd = "Bearer authentication required"; // "Basic authentication required";
    static private String basicAuthReqd2 = "Basic authentication required"; // "Basic authentication required";

    /**
     * TestDescription:
     *
     * This scenario tests the oauthOnly TAI property of OAuth TAI. The test
     * invokes a protected resource which is protected by an OAuth provider that
     * has oauthOnly property set to true and verifies that the request is
     * rejected without prompting for the login form. When the test invokes a
     * protected resource that has the oauthOnly property set to false, it
     * verifies that the application can be invoked after a successful login.
     * The test verifies that the OAuth TAI property - provider_<id>.oauthOnly
     * is working correctly.
     *
     */

    @Before
    public void setupBeforeTest() throws Exception {
        commonSetup("com.ibm.ws.security.oauth-2.0.fat");
    }

    @Test
    public void testOAuthOnly() throws Exception {

        final String thisMethod = "testOAuthOnly";
        WebResponse response = null;
        try {

            sslSetup();

            Log.info(thisClass, thisMethod, "Begin");

            protectedResource = httpsStart + "/oauth2tai/snoop";
            clientName = "dclient01";
            clientID = "dclient01";
            WebRequest request = null;
            String respReceived = null;
            HttpUnitOptions.setExceptionsThrownOnErrorStatus(true);
            HttpUnitOptions.setExceptionsThrownOnScriptError(true);
            // Create the conversation object which will maintain state for us
            WebConversation wc = new WebConversation();

            // Invoke a protected resource directly which has oauthOnly set to
            // true.
            request = new GetMethodWebRequest(protectedResource);

            // Check the response
            response = wc.getResponse(request);
            respReceived = response.getText();
            Log.info(thisClass, thisMethod, "Response from protected Res1: " + respReceived);
            // If we come here, test has failed.
            fail("Did not get exception for oauthOnly true resource");
        } catch (Exception e) {

            Log.info(thisClass, thisMethod, "Exception occurred:", e);
            String exception = new String();
//                      String respReceived = response.getText();
//                      Log.info(thisClass, thisMethod, "Response from protected Res1: " + respReceived);

            if ((e.getCause() != null)) {
                exception = e.getCause().getMessage();
            } else {
                exception = e.getMessage();
            }
            // Check if we got the correct exception.
            Log.info(thisClass, thisMethod, "The exception for Resource1: " + exception);
            if (exception.contains(basicAuthReqd)) {
                Log.info(thisClass, thisMethod, "Received the expected 401 exception");
            } else {
                fail("Did not get expected exception saying Bearer authentication is required");
            }
        }

        // Now invoke a resource that has oauthOnly set to false.
        WebConversation wc2 = new WebConversation();
        try {

            protectedResource = httpsStart + "/oauth2tai/ssodemo";
            WebRequest request = new GetMethodWebRequest(protectedResource);
            response = null;
            String respReceived = null;

            // Check the response
            response = wc2.getResponse(request);
            respReceived = response.getText();
            Log.info(thisClass, thisMethod, "Response from protected Res2: " + respReceived);
            // If we come here, test has failed.
            fail("Did not get exception for oauthOnly false resource");
        } catch (Exception e) {
            Log.info(thisClass, thisMethod, "Exception occurred:", e);
            String exception = new String();

            if ((e.getCause() != null)) {
                exception = e.getCause().getMessage();
            } else {
                exception = e.getMessage();
            }
            // Check if we got basicauth required exception.
            Log.info(thisClass, thisMethod, "The exception for Resource2:  " + exception);
            if (exception.contains(basicAuthReqd2)) {
                Log.info(thisClass, thisMethod, "Received the expected Basic authentication required exception");
            } else {
                fail("Did not get expected exception for OAuthOnly false resource");
            }
        }

    }

}
