/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.fat.locking;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;

/**
 * Lightweight stress test of persistent executor
 */
public class PXLockTest {
    private static final LibertyServer server = FATSuite.server;

    private static final String APP_NAME = "pxlocktest";

    @Rule
    public TestName testName = new TestName();

    public static final JdbcDatabaseContainer<?> testContainer = FATSuite.testContainer;

    /**
     * Runs a test in the servlet.
     *
     * @param test Test name to supply as an argument to the servlet
     * @return output of the servlet
     * @throws IOException if an error occurs
     */
    protected static StringBuilder runInServlet(String test) throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/pxlocktest?test=" + test);
        Log.info(PXLockTest.class, "runInServlet", "URL is " + url);
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
                Log.info(PXLockTest.class, "runInServlet", line);
            }

            // Look for success message, otherwise fail test
            if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0) {
                Log.info(PXLockTest.class, "runInServlet", "failed to find completed successfully message");
                fail("Missing success message in output. " + lines);
            }

            return lines;
        } finally {
            con.disconnect();
            Log.info(PXLockTest.class, "runInServlet", "disconnected from servlet");
        }
    }

    /**
     * Before running any tests, start the server
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setUp() throws Exception {
        //Get driver info
        server.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());

        //Setup datasource properties
        DatabaseContainerUtil.setupDataSourceProperties(server, testContainer);

        //Add application to server
        ShrinkHelper.defaultApp(server, APP_NAME, "web");

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
            try {
                runInServlet("verifyTasksRunMultipleTimes");
            } finally {
                // wait for tasks to stop running
                long waitForTaskCompletions = TimeUnit.SECONDS.toMillis(10);
                Thread.sleep(waitForTaskCompletions);
                server.stopServer();
            }
    }

    @Test
    public void testScheduleAtFixedRate() throws Exception {
        runInServlet("testScheduleAtFixedRate");
    }

    @Test
    public void testScheduleAtFixedRateCreateProp() throws Exception {
        runInServlet("testScheduleAtFixedRateCreateProp");
    }

    @Test
    public void testScheduleAtFixedRateSuspend() throws Exception {
        runInServlet("testScheduleAtFixedRateSuspend");
    }

    @Test
    public void testScheduleAtFixedRateSuspendGetStatus() throws Exception {
        runInServlet("testScheduleAtFixedRateSuspendGetStatus");
    }

    @Test
    public void testScheduleCallableWithTrigger() throws Exception {
        runInServlet("testScheduleCallableWithTrigger");
    }

    @Test
    public void testScheduleCallableWithTriggerGetStatus() throws Exception {
        runInServlet("testScheduleCallableWithTriggerGetStatus");
    }

    @Test
    public void testScheduleCallableWithTriggerSuspendGetStatus() throws Exception {
        runInServlet("testScheduleCallableWithTriggerSuspendGetStatus");
    }

    @Test
    public void testScheduleCallableWithTriggerSuspendGetStatusCreateProp() throws Exception {
        runInServlet("testScheduleCallableWithTriggerSuspendGetStatusCreateProp");
    }

    @Test
    public void testScheduleRunnableWithTrigger() throws Exception {
        runInServlet("testScheduleRunnableWithTrigger");
    }

    @Test
    public void testScheduleRunnableWithTriggerCreateProp() throws Exception {
        runInServlet("testScheduleRunnableWithTriggerCreateProp");
    }
}