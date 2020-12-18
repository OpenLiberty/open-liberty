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

import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class OAuth20WebClientError extends OAuth20TestCommon {

    private static final Class<?> thisClass = OAuth20WebClientError.class;

    @Before
    public void setupBeforeTest() throws Exception {
        commonSetup("com.ibm.ws.security.oauth-2.0.fat");
    }

    @AllowedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20DuplicateParameterException")
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
            String goodClientRedirect = clientRedirect;
            clientRedirect = httpStart + "/" + Constants.OAUTHCLIENT_APP + "/badredirect.jsp";
            request = fillAuthorizationForm(new GetMethodWebRequest(authorizeEndpt));
            String requestString = request.getURL().toString() + "&redirect_uri=http%3A%2F%2Flocalhost%3A8010%2F" + Constants.OAUTHCLIENT_APP + "%2Fredirect.jsp";

            Log.info(thisClass, thisMethod, "Target URL of first request: "
                                            + requestString); //request.getURL());
            Log.info(thisClass, thisMethod, "Filled in form: ");
            System.out.println(requestString);
            response = wc.getResponse(requestString);
            respReceived = response.getText();

            Log.info(thisClass, thisMethod,
                     "Response1 received from Auth server:   ");
            Log.info(thisClass, thisMethod, respReceived);

            // CWOAU0022E received, no redirections
            int responseCode = response.getResponseCode();
            assertFalse("The response must not be a redirect.", HttpServletResponse.SC_MOVED_TEMPORARILY == responseCode ||
                                                                HttpServletResponse.SC_MOVED_PERMANENTLY == responseCode);
            assertTrue("Did not get expected error message",
                       respReceived.equals("CWOAU0022E: The following OAuth parameter was provided more than once in the request: redirect_uri"));

            // Duplicate client_id
            String goodClientId = clientID;
            clientID = "aBadId";
            request = fillAuthorizationForm(new GetMethodWebRequest(authorizeEndpt));
            requestString = request.getURL().toString() + "&client_id=" + goodClientId;

            Log.info(thisClass, thisMethod, "Target URL of second request: "
                                            + requestString); //request.getURL());
            Log.info(thisClass, thisMethod, "Filled in form: ");
            System.out.println(requestString);
            response = wc.getResponse(requestString);
            respReceived = response.getText();

            Log.info(thisClass, thisMethod,
                     "Response1 received from Auth server:   ");
            Log.info(thisClass, thisMethod, respReceived);

            // CWOAU0022E received, no redirections
            responseCode = response.getResponseCode();
            assertFalse("The response must not be a redirect.", HttpServletResponse.SC_MOVED_TEMPORARILY == responseCode ||
                                                                HttpServletResponse.SC_MOVED_PERMANENTLY == responseCode);
            assertTrue("Did not get expected error message",
                       respReceived.equals("CWOAU0022E: The following OAuth parameter was provided more than once in the request: client_id"));

        } catch (Exception e) {

            Log.info(thisClass, thisMethod, "Exception occurred:");
            Log.error(thisClass, thisMethod, e, "Exception: ");
            throw e;
        }

    }

}
