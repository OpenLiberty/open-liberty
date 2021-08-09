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

package com.ibm.websphere.security.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class PasswordUtilAPITest {

    private final Class<?> thisClass = PasswordUtilAPITest.class;
    protected DefaultHttpClient client = new DefaultHttpClient();

    @Rule
    public TestName name = new TestName();

    /*
     * test PasswordUtil class is not visible from the application when
     * passwordUtilities-1.0 feature does not exist. (the negative test is done by PublicAPITestSecurityDisabled class
     */
    @Test
    public void testPasswordUtilApiDisabled() throws Exception {
        String method = name.getMethodName();
        Log.info(thisClass, method, "Entering test " + method);
        LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.websphere.security.fat.passwordutil");
        SecurityFatUtils.transformApps(myServer, "PasswordUtilAPI.war");

        try {
            myServer.startServer(true);
            String urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort() + "/PasswordUtilAPI";
            String className = "PasswordUtil";
            String encodeInput = "&input=sensitiveText";
            String decodeInput = "&input=%7bxor%7dLDoxLDYrNik6CzonKw%3d%3d";
            String expectedOutput = "NoClassDefFoundError: ";

            invokeServlet(urlBase, className, "encode", encodeInput, 200, expectedOutput);
            invokeServlet(urlBase, className, "decode", decodeInput, 200, expectedOutput);
        } finally {
            myServer.stopServer();
        }
        Log.info(thisClass, method, "Exiting test " + method);
    }

    /*
     * test PasswordUtil class is visible from the application when
     * passwordUtilities-1.0 feature exists. (the negative test is done by PublicAPITestSecurityDisabled class
     */
    @Test
    public void testPasswordUtilApiEnabled() throws Exception {
        String method = name.getMethodName();
        Log.info(thisClass, method, "Entering test " + method);
        // stop server, and then update the feature.
        LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.websphere.security.fat.passwordutil.api");
        SecurityFatUtils.transformApps(myServer, "PasswordUtilAPI.war");

        try {
            myServer.startServer(true);
            String urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort() + "/PasswordUtilAPI";
            String className = "PasswordUtil";
            String encodeInput = "&input=sensitiveText";
            String decodeInput = "&input=%7bxor%7dLDoxLDYrNik6CzonKw%3d%3d";
            String expectedEncodeOutput = "encode output is: {xor}LDoxLDYrNik6CzonKw==";
            String expectedDecodeOutput = "decode output is: sensitiveText";

            invokeServlet(urlBase, className, "encode", encodeInput, 200, expectedEncodeOutput);
            invokeServlet(urlBase, className, "decode", decodeInput, 200, expectedDecodeOutput);
        } finally {
            myServer.stopServer();
        }

        Log.info(thisClass, method, "Exiting test " + method);

    }

    protected String invokeServlet(String urlBase, String className, String methodName, String parms,
                                   int expectedStatusCode, String expectedResponseText) {
        String method = name.getMethodName();
        String url = urlBase + "?class=" + className + "&method=" + methodName;
        if (parms != null && !parms.isEmpty())
            url = url + "&" + parms;
        String response = invokeServlet(url, expectedStatusCode);
        Log.info(thisClass, method, "response = " + response);
        assertNotNull("response was null", response);
        assertTrue("Expected: " + expectedResponseText + ", in response text: " + response,
                   response.indexOf(expectedResponseText) >= 0);
        return response;
    }

    protected String invokeServlet(String url, int expectedStatusCode) {
        String method = name.getMethodName();
        String content = null;
        Log.info(thisClass, method, "url=" + url + " expectedStatusCode=" + expectedStatusCode);
        try {
            HttpGet getMethod = new HttpGet(url);
            HttpResponse response = client.execute(getMethod);
            Log.info(thisClass, method, "response = " + response.toString());
            HttpEntity entity = response.getEntity();
            content = EntityUtils.toString(entity);
            Log.info(thisClass, method, "Servlet response: " + content);
            EntityUtils.consume(entity);

            assertEquals("Expected status code: " + expectedStatusCode + ", received: " + response.getStatusLine().getStatusCode(),
                         expectedStatusCode, response.getStatusLine().getStatusCode());
            return content;
        } catch (Exception e) {
            Log.info(thisClass, method, "Caught unexpected exception: " + e);
            return null;
        }
    }

}