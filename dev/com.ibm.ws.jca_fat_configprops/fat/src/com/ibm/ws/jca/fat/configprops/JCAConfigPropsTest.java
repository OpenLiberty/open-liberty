/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.fat.configprops;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * General tests that don't involve updating configuration while the server is running.
 */
public class JCAConfigPropsTest {
    private static final String APP_NAME = "fvtweb";

    private static LibertyServer server;

    /**
     * Utility method to run a test on ConfigPropsRATestServlet.
     *
     * @param test Test name to supply as an argument to the servlet
     * @return output of the servlet
     * @throws IOException if an error occurs
     */
    private StringBuilder runInServlet(String test) throws IOException {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/fvtweb?test=" + test);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();
            for (String line = br.readLine(); line != null; line = br.readLine())
                lines.append(line).append(sep);

            if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0)
                fail("Missing success message in output. " + lines);

            return lines;
        } finally {
            con.disconnect();
        }
    }

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jca.fat.configprops");

        server.addInstalledAppForValidation(APP_NAME);
        server.startServer();
        server.waitForStringInLog("CWWKE0002I");
        assertNotNull("FeatureManager should report update is complete",
                      server.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Server should report it has started",
                      server.waitForStringInLog("CWWKF0011I"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testMCFAnnotationOverridesRAWLPExtension() throws Exception {
        runInServlet("testMCFAnnotationOverridesRAWLPExtension");
    }

    @Test
    public void testMCFDDOverridesMCFAnnotation() throws Exception {
        runInServlet("testMCFDDOverridesMCFAnnotation");
    }

    @Test
    public void testMCFJavaBean() throws Exception {
        runInServlet("testMCFJavaBean");
    }

    @Test
    public void testRAAnnotationOverridesRAJavaBean() throws Exception {
        runInServlet("testRAAnnotationOverridesRAJavaBean");
    }

    @Test
    public void testRADeploymentDescriptorOverridesRAAnnotation() throws Exception {
        runInServlet("testRADeploymentDescriptorOverridesRAAnnotation");
    }

    @Test
    public void testRAJavaBeanOverridesMCFJavaBean() throws Exception {
        runInServlet("testRAJavaBeanOverridesMCFJavaBean");
    }

    @Test
    public void testRAWLPExtensionOverridesRADeploymentDescriptor() throws Exception {
        runInServlet("testRAWLPExtensionOverridesRADeploymentDescriptor");
    }

    @Test
    public void testServerXMLOverridesWLPExtension() throws Exception {
        runInServlet("testServerXMLOverridesWLPExtension");
    }

    @Test
    public void testWLPExtensionOverridesMCFDeploymentDescriptor() throws Exception {
        runInServlet("testWLPExtensionOverridesMCFDeploymentDescriptor");
    }
}
