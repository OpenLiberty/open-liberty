/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package io.openliberty.ssl.fat.jssehelper;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocketFactory;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ssl.JSSEHelper;

import componenttest.app.FATServlet;
import junit.framework.Assert;

/*
 * Get the SSLContext/SSLSocket factory from JSSEHelper then use HttpsURLConnection to create a secure
 * connection to the test endpoint. Verify that the outbound connections honor Liberty's configured
 * ssl config. Connections should honor specific config when an alias is supplied and use the default
 * config when no alias is supplied.
 *
 * Any test permutations where you set the SSLAlias on the Properties should either get the
 * default config if 'tryDefault=true' or throw a com.ibm.websphere.ssl.SSLConfigurationNotAvailableException
 * if 'tryDefault=false'
 */
@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JSSEHelperClientTestServlet")
public class JSSEHelperClientTestServlet extends FATServlet {

    private static final String URI_CONTEXT_ROOT = "https://localhost:" + Integer.getInteger("bvt.prop.HTTP_default.secure") + "/jssehelper/simple";

    @Test
    public void testDefaultSSLContext() {
        boolean exceptionExpected = false;
        boolean exceptionCaught = false;

        try {
            SSLContext sslContext = JSSEHelper.getInstance().getSSLContext(null, null, null);
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            verifySSLContext(sslContext.getClass().getName());
            verifySSLSocketFactory(sslSocketFactory.getClass().getName());

            sendRequestWithCustomSSLSocketFactory(sslSocketFactory, exceptionExpected);
        } catch (SSLHandshakeException e) {
            exceptionCaught = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        // verify that no javax.net.ssl.SSLHandshakeException was thrown
        assertEquals(exceptionExpected, exceptionCaught);
    }

    @Test
    public void testCustomSSLContext() {
        boolean exceptionExpected = true;
        boolean exceptionCaught = false;

        try {
            SSLContext sslContext = JSSEHelper.getInstance().getSSLContext("mySSLConfig", null, null);
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            verifySSLContext(sslContext.getClass().getName());
            verifySSLSocketFactory(sslSocketFactory.getClass().getName());

            sendRequestWithCustomSSLSocketFactory(sslSocketFactory, exceptionExpected);
        } catch (SSLHandshakeException e) {
            exceptionCaught = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        // verify that a javax.net.ssl.SSLHandshakeException was thrown
        assertEquals(exceptionExpected, exceptionCaught);
    }

    @Test
    public void testInvalidSSLContextWithTryDefault() {
        boolean exceptionExpected = false;
        boolean exceptionCaught = false;

        try {
            SSLContext sslContext = JSSEHelper.getInstance().getSSLContext("invalid", null, null, true);
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            verifySSLContext(sslContext.getClass().getName());
            verifySSLSocketFactory(sslSocketFactory.getClass().getName());

            sendRequestWithCustomSSLSocketFactory(sslSocketFactory, exceptionExpected);
        } catch (SSLHandshakeException e) {
            exceptionCaught = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        // verify that no javax.net.ssl.SSLHandshakeException was thrown
        // we are expecting to get the default config
        assertEquals(exceptionExpected, exceptionCaught);
    }

    @Test
    public void testDefaultSSLSocketFactory() {
        boolean exceptionExpected = false;
        boolean exceptionCaught = false;

        try {
            SSLSocketFactory sslSocketFactory = JSSEHelper.getInstance().getSSLSocketFactory(null, null);

            verifySSLSocketFactory(sslSocketFactory.getClass().getName());

            sendRequestWithCustomSSLSocketFactory(sslSocketFactory, exceptionExpected);
        } catch (SSLHandshakeException e) {
            exceptionCaught = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        // verify that no javax.net.ssl.SSLHandshakeException was thrown
        assertEquals(exceptionExpected, exceptionCaught);
    }

    @Test
    public void testCustomSSLSocketFactory() {
        boolean exceptionExpected = true;
        boolean exceptionCaught = false;

        try {
            SSLSocketFactory sslSocketFactory = JSSEHelper.getInstance().getSSLSocketFactory("mySSLConfig", null, null);

            verifySSLSocketFactory(sslSocketFactory.getClass().getName());

            sendRequestWithCustomSSLSocketFactory(sslSocketFactory, exceptionExpected);
        } catch (SSLHandshakeException e) {
            exceptionCaught = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        // verify that a javax.net.ssl.SSLHandshakeException was thrown
        assertEquals(exceptionExpected, exceptionCaught);
    }

    @Test
    public void testInvalidSSLSocketFactory() {
        boolean exceptionExpected = false;
        boolean exceptionCaught = false;

        try {
            SSLSocketFactory sslSocketFactory = JSSEHelper.getInstance().getSSLSocketFactory("invalid", null, null);

            verifySSLSocketFactory(sslSocketFactory.getClass().getName());

            sendRequestWithCustomSSLSocketFactory(sslSocketFactory, exceptionExpected);
        } catch (SSLHandshakeException e) {
            exceptionCaught = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        // verify that no javax.net.ssl.SSLHandshakeException was thrown
        // JSSEHelper.getSSLSocketFactory() looks up the properties with 'tryDefault=true'
        assertEquals(exceptionExpected, exceptionCaught);
    }

    private void sendRequestWithCustomSSLSocketFactory(SSLSocketFactory sslSocketFactory, boolean failureExpected) throws SSLHandshakeException {
        int responseCode = -1;
        StringBuffer sb = new StringBuffer();

        try {
            // Set the default SSLContext for the application
            HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory);

            // URL to connect to (replace with your URL)
            URL url = new URL(URI_CONTEXT_ROOT);

            // Open connection
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

            // Set request method (GET, POST, etc.)
            connection.setRequestMethod("GET");

            // Get response code
            responseCode = connection.getResponseCode();

            // Read response
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            // Disconnect
            connection.disconnect();

            if (failureExpected) {
                Assert.fail("Test should have thrown a javax.net.ssl.SSLHandshakeException");
            }

            assertEquals(200, responseCode);
            assertEquals("Hello World!", sb.toString());

        } catch (javax.net.ssl.SSLHandshakeException e) {
            // re-throw so the test can verify it was thrown
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void verifySSLContext(String sslContextName) {
        System.out.println("SSLContext=" + sslContextName);

        // Verify that we are using LibertySSLContext
        Assert.assertTrue("com.ibm.ws.ssl.LibertySSLContext".equals(sslContextName));
    }

    private void verifySSLSocketFactory(String sslSocketFactoryName) {
        System.out.println("SSLSocketFactory=" + sslSocketFactoryName);

        // Verify that we are using LibertySSLSocketFactoryWrapper
        Assert.assertTrue("com.ibm.ws.ssl.LibertySSLSocketFactoryWrapper".equals(sslSocketFactoryName));
    }
}