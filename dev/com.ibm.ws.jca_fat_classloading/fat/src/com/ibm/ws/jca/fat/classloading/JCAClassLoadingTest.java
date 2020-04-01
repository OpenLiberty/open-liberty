/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.fat.classloading;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import componenttest.annotation.ExpectedFFDC;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * General tests that don't involve updating configuration while the server is running.
 */
public class JCAClassLoadingTest {

    @Rule
    public TestName testName = new TestName();

    private static LibertyServer server;

    private static final String ClassLoadingTestApp = "ClassLoadingApp";
    private static final String ClassLoadingTestServlet = "fvtweb";

    /**
     * Utility method to run a test in a servlet.
     *
     * @param test Test name to supply as an argument to the servlet
     * @return output of the servlet
     */
    private StringBuilder runInServlet(String test, String webmodule) throws IOException {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + webmodule + "?test=" + test);
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
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jca.fat.classloading");
        server.addInstalledAppForValidation(ClassLoadingTestApp);
    }

    @Before
    public void setUpPerTest() throws Exception {
        System.out.println("*********** enter: " + testName.getMethodName());
    }

    @After
    public void tearDownPerTest() throws Exception {
        if (server.isStarted())
            server.stopServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted())
            server.stopServer();
    }

    @Test
    public void testLoadResourceAdapterClassFromSingleApp() throws Exception {

        server.setServerConfigurationFile("default_server.xml");
        server.startServer(testName.getMethodName());

        runInServlet("testLoadResourceAdapterClassFromSingleApp", ClassLoadingTestServlet);
    }

    @Test
    public void testApiTypeVisibilityNone() throws Exception {
        String consoleLogFileName = testName.getMethodName() + ".log";

        server.setServerConfigurationFile(testName.getMethodName() + "_server.xml");
        server.startServer(consoleLogFileName);

        runInServlet("testApiTypeVisibilityNone", ClassLoadingTestServlet);
    }

    @Test
    public void testApiTypeVisibilityAll() throws Exception {
        String consoleLogFileName = testName.getMethodName() + ".log";

        server.setServerConfigurationFile(testName.getMethodName() + "_server.xml");
        server.startServer(consoleLogFileName);

        runInServlet("testApiTypeVisibilityAll", ClassLoadingTestServlet);
    }

    // This test passes because the resource adapter's class loader gateway is set to an invalid
    // apiTypeVisibility value, which the class loading service accepts and causes the loader to
    // lacks access to "spec" classes.  There is no error message for an invalid apiTypeVisbility
    // value.

    @ExpectedFFDC({ "java.lang.NoClassDefFoundError" })
    @Test
    public void testApiTypeVisibilityInvalid() throws Exception {
        String consoleLogFileName = testName.getMethodName() + ".log";

        server.setServerConfigurationFile(testName.getMethodName() + "_server.xml");
        server.removeInstalledAppForValidation(ClassLoadingTestApp);
        server.startServer(consoleLogFileName);

        String msg = server
                        .waitForStringInLogUsingMark("J2CA7002E: An exception occurred while installing the resource adapter ATVInvalid_RA. The exception message is: java.lang.NoClassDefFoundError: javax.resource.spi.ResourceAdapter");
        assertNotNull("Resource adapter lacks [spec] class loading api type visibility", msg);

        // put the app back on the server when we are done
        server.stopServer("J2CA7002E");
        server.addInstalledAppForValidation(ClassLoadingTestApp);
    }

    @Test
    public void testApiTypeVisibilityMatch() throws Exception {
        String consoleLogFileName = testName.getMethodName() + ".log";

        server.setServerConfigurationFile(testName.getMethodName() + "_server.xml");
        server.startServer(consoleLogFileName);

        runInServlet("testApiTypeVisibilityMatch", ClassLoadingTestServlet);
    }

    @Test
    public void testInvalidClassProviderRef() throws Exception {
        String consoleLogFileName = testName.getMethodName() + ".log";

        server.setServerConfigurationFile(testName.getMethodName() + "_server.xml");
        server.startServer(consoleLogFileName);

        String msg = server
                        .waitForStringInLogUsingMark("CWWKG0033W: The value \\[invalidRef\\] specified for the reference attribute \\[classProviderRef\\] was not found in the configuration");
        assertNotNull("Expected to see message 'CWWKG0033W' in logs but did not find it when using an invalid classProviderRef.", msg);

        // Manually do the stop here so we don't ignore this exception for all tests in the @After stop
        server.stopServer("CWWKG0033W");
    }

    @Test
    public void testApiTypeVisibilityMismatch() throws Exception {
        String consoleLogFileName = testName.getMethodName() + ".log";

        server.setServerConfigurationFile(testName.getMethodName() + "_server.xml");
        server.removeInstalledAppForValidation(ClassLoadingTestApp);
        server.startServer(consoleLogFileName);

        String msg = server.waitForStringInLogUsingMark("CWWKL0033W");
        assertNotNull("Expected to see error message 'CWWKL0033W' when applicaiton and resource adapter apiTypeVisibility do not match.", msg);

        // put the app back on the server when we are done
        server.stopServer("CWWKL0033W");
        server.addInstalledAppForValidation(ClassLoadingTestApp);
    }

    @Test
    public void testClassSpaceRestriction() throws Exception {
        String consoleLogFileName = testName.getMethodName() + ".log";

        server.setServerConfigurationFile(testName.getMethodName() + "_server.xml");
        server.startServer(consoleLogFileName);

        runInServlet(testName.getMethodName(), ClassLoadingTestServlet);
    }
}
