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

import java.util.Arrays;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

public class OAuth20Client03Common extends OAuth20TestCommon {

    private static final Class<?> thisClass = OAuth20Client03Common.class;

    public void setupBeforeTest() throws Exception {
    }

    public void testOAuthDerbyCodeFlow() throws Exception {
        clientName = "dclient01";
        clientID = "dclient01";

        // setup up the Derby DataBase entries
        setupDerbyEntries("fred"); //server configured as "bob"

        testOAuthCommonCodeFlow();
    }

    public void testOAuthCustomStoreCodeFlow() throws Exception {
        clientName = "dclient01";
        clientID = "dclient01";

        // setup up the MongoDB DataBase entries
        setupMongDBEntries("fred");

        testOAuthCommonCodeFlow();
    }
    // NOTE: Should only allow java.sql.SQLRecoverableException. Other AllowedFFDCs added here TEMPORARILY
    // to stop the build being red due to defect 98925. When that is fixed, remove the other entries!!

    public void testOAuthCommonCodeFlow() throws Exception {

        final String thisMethod = "testOAuthDerbyCodeFlow";
        try {

            sslSetup();

            Log.info(thisClass, thisMethod, "Begin");

            WebRequest request = null;
            WebResponse response = null;
            String respReceived = null;
            authorizeEndpt = httpsStart + "/oauth2/endpoint/OAuthConfigDerby/authorize";
            tokenEndpt = httpsStart + "/oauth2/endpoint/OAuthConfigDerby/token";
            protectedResource = httpsStart + "/oauth2tai/ssodemo";

            final WebConversation wc = new WebConversation();

            // Start the OAuth request by invoking the client
            Log.info(thisClass, thisMethod, "firstClientUrl: " + firstClientUrl);
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
            Log.info(thisClass, thisMethod, "Target URL of request: " + request.getURL());
            respReceived = response.getText();
            System.out.println("Response2 from Authorization server: " + respReceived);

            /*
             * If we are running a custom OAuthStore with MongoDB, we don't expect there to be an error returned for
             * using an invalid schema when accessing a table in the DB.
             */
            if (!isRunningCustomStore) {
                verifyLogMessage(MessageConstants.CWWKS1460E_ERROR_PERFORMING_DB_OPERATION + ".+" + "SELECT" + ".+" + clientID);
                verifyLogMessage("OAUTH20CLIENTCONFIG' does not exist");
            }

            Log.info(thisClass, thisMethod, "Test Passed!");

        } catch (Exception e) {

            Log.info(thisClass, thisMethod, "Exception occurred:");
            Log.error(thisClass, thisMethod, e, "Exception: ");

            throw e;
        }

    }

    void verifyLogMessage(String logMsg) {
        assertTrue("Did not find expected messages [" + logMsg + "] in server log.", (server.waitForStringInLogUsingMark(logMsg, 0) != null));
        server.addIgnoredErrors(Arrays.asList(logMsg));
    }

}
