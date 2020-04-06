/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.fat.mbean;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Tests for persistent scheduled executor with task execution disabled
 */
@RunWith(FATRunner.class)
public class PersistentExecutorMBeanTest {

    private static final LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.concurrent.persistent.fat.mbean");

    private static final Set<String> appNames = Collections.singleton("persistentmbeantest");
    
    private static final String APP_NAME = "persistentmbeantest";

    @Rule
    public TestName testName = new TestName();

    /**
     * Runs a test in the servlet.
     *
     * @param test Test name to supply as an argument to the servlet
     * @return output of the servlet
     * @throws IOException if an error occurs
     */
    protected static StringBuilder runInServlet(String test, String jndi) throws Exception {
        String j = jndi == null ? " " : "&jndi=".concat(jndi);
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/persistentmbeantest?test=" + test
                          + "&userdir=" + server.getUserDir() + j);
        Log.info(PersistentExecutorMBeanTest.class, "runInServlet", "URL is " + url);
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

            // Send output from servlet to console output
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                lines.append(line).append(sep);
                Log.info(PersistentExecutorMBeanTest.class, "runInServlet", line);
            }

            // Look for success message, otherwise fail test
            if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0) {
                Log.info(PersistentExecutorMBeanTest.class, "runInServlet", "failed to find completed successfully message");
                fail("Missing success message in output. " + lines);
            }

            return lines;
        } finally {
            con.disconnect();
            Log.info(PersistentExecutorMBeanTest.class, "runInServlet", "disconnected from servlet");
        }
    }

    /**
     * Before running any tests, start the server
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "web");
        server.startServer();
    }

    /**
     * After completing all tests, stop the server.
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted())
            server.stopServer();
    }

    @Test
    public void testMBeanCreation() throws Exception {
        runInServlet("testMBeanCreation", null);
    }

    @Test
    public void testfindAndRemovePartitionInfo() throws Exception {
        runInServlet("testfindPartitionInfo", null);
        runInServlet("testRemovePartitionInfo", null);
    }

    @Test
    public void testFindTaskIds() throws Exception {
        runInServlet("testFindTaskIds", null);
    }

    @Test
    public void testPersistentExecutorMBeanClassIsNotAPI() throws Exception {
        runInServlet("testPersistentExecutorMBeanClassIsNotAPI", null);
    }

    @Test
    public void testTransfer() throws Exception {
        runInServlet("testTransfer", null);
    }

    @Test
    public void testSwitchJndiName() throws Exception {
        runInServlet("findMBeanByJndiName", "concurrent/switchExecutor");
        server.setServerConfigurationFile("server2.xml");
        server.waitForConfigUpdateInLogUsingMark(appNames);
        runInServlet("findMBeanByJndiName", "concurrent/hctiwsExecutor");

        runInServlet("missMBeanByJndiName", "concurrent/switchExecutor");

    }

    @Test
    public void testObjectName() throws Exception {
        runInServlet("testObjectName", null);
    }

}