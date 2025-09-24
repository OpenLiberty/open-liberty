/*******************************************************************************
 * Copyright (c) 2014, 2025 IBM Corporation and others.
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

package io.openliberty.checkpoint.fat.security.jaspic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;

public class JASPITestBase {

    public static final String DEFAULT_BASICAUTH_SERVLET_NAME = "ServletName: JASPIBasicAuthServlet";
    public static final String NO_NAME_VALIDATION = "NONE";
    public static final String DEFAULT_APP = "JASPIBasicAuthServlet";
    protected final String DEFAULT_JASPI_PROVIDER = "bob";

    // Jaspi test users
    protected final String jaspi_basicRoleUser = "jaspiuser1";
    protected final String jaspi_basicRolePwd = "s3cur1ty";
    protected final String jaspi_invalidUser = "invalidUserName";
    protected final String jaspi_invalidPwd = "invalidPassword";
    protected final String jaspiValidateRequest = "JASPI validateRequest called with auth provider=";
    protected final String jaspiSecureResponse = "JASPI secureResponse called with auth provider=";
    protected final String getRemoteUserFound = "getRemoteUser: ";
    protected final String getUserPrincipalFound = "getUserPrincipal: WSPrincipal:";

    // Values to be verified in messages
    protected static final String MSG_JASPI_AUTHENTICATION_FAILED = "CWWKS1652A:.*";
    protected static final String PROVIDER_AUTHENTICATION_FAILED = "Invalid user or password";
    protected static final String MSG_JASPI_PROVIDER_ACTIVATED = "CWWKS1653I";

    // Jaspi helper methods
    protected static void verifyServerStarted(LibertyServer server) {
        assertNotNull("FeatureManager did not report update was complete",
                      server.waitForStringInLogUsingMark("CWWKF0008I"));
        assertNotNull("Security service did not report it was ready",
                      server.waitForStringInLogUsingMark("CWWKS0008I"));
    }

    protected static void verifyServerUpdated(LibertyServer server) {
        assertNotNull("Feature update wasn't complete",
                      server.waitForStringInLogUsingMark("CWWKF0008I"));
        assertNotNull("The server configuration wasn't updated.",
                      server.waitForStringInLogUsingMark("CWWKG0017I:.*"));

    }

    protected static void verifyServerUpdatedWithJaspi(LibertyServer server, String appName) {
        verifyServerUpdated(server);
        assertNotNull("The JASPI user feature did not report it was ready",
                      server.waitForStringInLogUsingMark(MSG_JASPI_PROVIDER_ACTIVATED));
        if (JakartaEEAction.isEE11Active()) {
            assertNotNull("The feature manager did not report the JASPI provider is included in features.",
                          server.waitForStringInLogUsingMark("CWWKF0012I.*" + "usr:jaspicUserTestFeature-3.1"));
        } else if (JakartaEEAction.isEE10Active()) {
            assertNotNull("The feature manager did not report the JASPI provider is included in features.",
                          server.waitForStringInLogUsingMark("CWWKF0012I.*" + "usr:jaspicUserTestFeature-3.0"));
        } else if (JakartaEEAction.isEE9Active()) {
            assertNotNull("The feature manager did not report the JASPI provider is included in features.",
                          server.waitForStringInLogUsingMark("CWWKF0012I.*" + "usr:jaspicUserTestFeature-2.0"));
        } else {
            assertNotNull("The feature manager did not report the JASPI provider is included in features.",
                          server.waitForStringInLogUsingMark("CWWKF0012I.*" + "usr:jaspicUserTestFeature-1.0"));
        }
        // Need to wait for the application started message.
        assertNotNull("URL not available " + server.waitForStringInLogUsingMark("CWWKT0016I.*" + appName + ".*"));
    }

    protected static void verifyServerStartedWithJaspiFeature(LibertyServer server) {
        verifyServerStarted(server);
        if (JakartaEEAction.isEE11Active()) {
            assertNotNull("The feature manager did not report the JASPI provider is included in features.",
                          server.waitForStringInLogUsingMark("CWWKF0012I.*" + "usr:jaspicUserTestFeature-3.1"));
        } else if (JakartaEEAction.isEE10Active()) {
            assertNotNull("The feature manager did not report the JASPI provider is included in features.",
                          server.waitForStringInLogUsingMark("CWWKF0012I.*" + "usr:jaspicUserTestFeature-3.0"));
        } else if (JakartaEEAction.isEE9Active()) {
            assertNotNull("The feature manager did not report the JASPI provider is included in features.",
                          server.waitForStringInLogUsingMark("CWWKF0012I.*" + "usr:jaspicUserTestFeature-2.0"));
        } else {
            assertNotNull("The feature manager did not report the JASPI provider is included in features.",
                          server.waitForStringInLogUsingMark("CWWKF0012I.*" + "usr:jaspicUserTestFeature-1.0"));
        }

    }

    // Values to be set by the child class
    protected LibertyServer server;
    protected Class<?> logClass;

    protected JASPITestBase(LibertyServer server, Class<?> logClass) {
        this.server = server;
        this.logClass = logClass;
    }

    protected String getCurrentTestName() {
        return "Test name not set";
    }

    protected String executeGetRequestBasicAuthCreds(DefaultHttpClient httpClient, String url, String userid, String password, int expectedStatusCode) throws Exception {

        String methodName = "executeGetRequestBasicAuthCreds";
        Log.info(logClass, getCurrentTestName(), "Servlet url: " + url + " userid: " + userid + ", password: " + password + ", expectedStatusCode=" + expectedStatusCode
                                                 + " , method=" + methodName);
        HttpGet getMethod = new HttpGet(url);
        if (userid != null)
            httpClient.getCredentialsProvider()
                            .setCredentials(new AuthScope("localhost", AuthScope.ANY_PORT, AuthScope.ANY_REALM),
                                            new UsernamePasswordCredentials(userid, password));
        HttpResponse response = httpClient.execute(getMethod);
        Log.info(logClass, methodName, "Actual response: " + response.toString());

        return processResponse(response, expectedStatusCode);

    }

    protected String executeGetRequestNoAuthCreds(DefaultHttpClient httpClient, String url, int expectedStatusCode) throws Exception {
        return executeGetRequestBasicAuthCreds(httpClient, url, null, null, expectedStatusCode);
    }

    /**
     * Process the response from an http invocation, such as validating
     * the status code, extracting the response entity...
     *
     * @param response           the HttpResponse
     * @param expectedStatusCode
     * @return The response entity text, or null if request failed
     * @throws IOException
     */
    protected String processResponse(HttpResponse response,
                                     int expectedStatusCode) throws IOException {
        String methodName = "processResponse";

        Log.info(logClass, methodName, "getMethod status: " + response.getStatusLine());
        HttpEntity entity = response.getEntity();
        String content = EntityUtils.toString(entity);
        Log.info(logClass, methodName, "Servlet full response content: \n" + content);
        EntityUtils.consume(entity);

        assertEquals("Expected " + expectedStatusCode + " was not returned",
                     expectedStatusCode, response.getStatusLine().getStatusCode());

        return content;
    }

    protected void verifyJaspiAuthenticationProcessedByProvider(String response, String jaspiProvider, String servletName) {
        Log.info(logClass, "verifyJaspiAuthenticationProcessedByProvider", "Verify response contains Servlet name and JASPI authentication processed");
        if (servletName != NO_NAME_VALIDATION)
            mustContain(response, servletName);
        verifyJaspiAuthenticationUsed(response, jaspiProvider);
    }

    protected void verifyJaspiAuthenticationUsed(String response, String jaspiProvider) {
        Log.info(logClass, "verifyJaspiAuthenticationUsed", "Verify response shows JASPI validateRequest and secureResponse called");
        mustContain(response, jaspiValidateRequest + jaspiProvider);
        mustContain(response, jaspiSecureResponse + jaspiProvider);
    }

    private void mustContain(String response, String target) {
        assertTrue("Expected result " + target + " not found in response", response.contains(target));
    }

    protected void verifyJaspiAuthenticationProcessedInMessageLog() {
        Log.info(logClass, "verifyJaspiAuthenticationProcessedInMessageLog", "Verify messages.log contains isMandatory=true and calls to validateRequest and secureResponse ");
        assertNotNull("Messages.log did not show that security runtime indicated that isMandatory=true and the JASPI provider must protect this resource. ",
                      server.waitForStringInLogUsingMark("JASPI_PROTECTED isMandatory=true"));
        assertNotNull("Messages.log did not show JASPI provider validateRequest was called. ",
                      server.waitForStringInLogUsingMark("validateRequest"));
        assertNotNull("Messages.log did not show JASPI provider secureResponse was called. ",
                      server.waitForStringInLogUsingMark("secureResponse"));
    }

    protected void verifyUserResponse(String response, String getUserPrincipal, String getRemoteUser) {
        Log.info(logClass, "verifyUserResponse", "Verify response contains: " + getUserPrincipal + ", " + getRemoteUser);
        mustContain(response, getUserPrincipal);
        mustContain(response, getRemoteUser);
    }

    protected void verifyMessageReceivedInMessageLog(String message) {
        assertNotNull("The messages.log file should contain the following message but did not --" + message,
                      server.waitForStringInLogUsingMark(message));
    }

    protected String buildQueryString(String operation, String layer, String appContext, String providerClass) {
        String queryString = operation + "**" + layer + "**" + appContext + "**" + providerClass;
        return queryString;
    }

}