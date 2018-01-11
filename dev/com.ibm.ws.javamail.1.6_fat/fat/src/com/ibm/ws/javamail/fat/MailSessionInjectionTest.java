/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javamail.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class MailSessionInjectionTest {

    protected static LibertyServer server;
    private static ServerConfiguration originalServerConfig;
    private static final String fvtweb = "fvtweb";
    private static final String fvtapp = "fvtapp";

    @Rule
    public TestName testName = new TestName();

    /**
     * Utility method to run a test on JCAFVTServlet.
     *
     * @param test Test name to supply as an argument to the servlet
     * @return output of the servlet
     * @throws IOException if an error occurs
     */
    private StringBuilder runInServlet(String test, String webmodule) throws IOException {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + webmodule + "?test=" + test);
        for (int numRetries = 2;; numRetries--) {
            Log.info(getClass(), "runInServlet", "URL is " + url);
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
            } catch (FileNotFoundException x) {
                if (numRetries > 0)
                    try {
                        Log.info(getClass(), "runInServlet", x + " occurred - will retry after 10 seconds");
                        Thread.sleep(10000);
                    } catch (InterruptedException interruption) {
                    }
                else
                    throw x;
            } finally {
                con.disconnect();
            }
        }
    }

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.javamail.fat");

        server.addInstalledAppForValidation(fvtapp);
        server.startServer("MailSessionDDTest.log");
        server.waitForStringInLog("CWWKE0002I");
        assertNotNull("FeatureManager should report update is complete",
                      server.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Server should report it has started",
                      server.waitForStringInLog("CWWKF0011I"));

        originalServerConfig = server.getServerConfiguration().clone();

    }

    /**
     * Before running each test, restore to the original configuration.
     *
     * @throws Exception
     */
    @Before
    public void setUpPerTest() throws Exception {
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(originalServerConfig);
        //server.waitForConfigUpdateInLogUsingMark(appNames);
        Log.info(getClass(), "setUpPerTest", "server configuration restored");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server.stopServer("J2CA0086W.*State:STATE_TRAN_WRAPPER_INUSE", // EXPECTED: One test intentionally leaves an open connection
                              "CWWKG0007W"); // let Nathan handle this : The system could not delete C:\Users\IBM_ADMIN\Documents\workspace\build.image/wlp/usr/servers\com.ibm.ws.jca.fat\workarea\org.eclipse.osgi\9\data\configs\com.ibm.ws.jca.jmsConnectionFactory.properties_83!-723947066
        } finally {
            if (originalServerConfig != null)
                server.updateServerConfiguration(originalServerConfig);
        }
    }

    @Test
    public void testDDSessionCreated() throws Exception {
        runInServlet("testDDJavamailSessionCreated", fvtweb);
    }

    @Test
    public void testAnnotationSessionCreated() throws Exception {
        runInServlet("testAnnotationJavamailSessionCreated", fvtweb);
    }

    @Test
    public void testMergedSession() throws Exception {
        runInServlet("testMergedJavamailSessionCreated", fvtweb);
    }

    @Test
    public void testEJBMailSessionCreated() throws Exception {
        runInServlet("testEjbJavamailSessionCreated", fvtweb);
    }
}
