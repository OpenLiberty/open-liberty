/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
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
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.PersistentExecutor;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.database.container.DatabaseContainerFactory;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;

/**
 * Lightweight stress test of persistent executor
 */
public class PXLockTestWithFailoverEnabled {
    private static final LibertyServer server = FATSuite.server;
    private static ServerConfiguration originalConfig;

    private static final String APP_NAME = "pxlocktest";

    @Rule
    public TestName testName = new TestName();

    @ClassRule
    public static final JdbcDatabaseContainer<?> testContainer = DatabaseContainerFactory.create();

    /**
     * Runs a test in the servlet.
     *
     * @param test Test name to supply as an argument to the servlet
     * @return output of the servlet
     * @throws IOException if an error occurs
     */
    protected static StringBuilder runInServlet(String test) throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/pxlocktest?test=" + test);
        Log.info(PXLockTestWithFailoverEnabled.class, "runInServlet", "URL is " + url);
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
                Log.info(PXLockTestWithFailoverEnabled.class, "runInServlet", line);
            }

            // Look for success message, otherwise fail test
            if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0) {
                Log.info(PXLockTestWithFailoverEnabled.class, "runInServlet", "failed to find completed successfully message");
                fail("Missing success message in output. " + lines);
            }

            return lines;
        } finally {
            con.disconnect();
            Log.info(PXLockTestWithFailoverEnabled.class, "runInServlet", "disconnected from servlet");
        }
    }

    /**
     * Before running any tests, start the server
     */
    @BeforeClass
    public static void setUp() throws Exception {
        //Configure PersistantExecutor with failover threshold
        originalConfig = server.getServerConfiguration();
        ServerConfiguration config = originalConfig.clone();
        PersistentExecutor myPersistentExecutor = config.getPersistentExecutors().getBy("jndiName", "concurrent/myPersistentExecutor");
        myPersistentExecutor.setMissedTaskThreshold("45s");
        myPersistentExecutor.setExtraAttribute("ignore.minimum.for.test.use.only", "true");
        server.updateServerConfiguration(config);

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
        if (server != null)
            try {
                if (server.isStarted())
                    try {
                        runInServlet("verifyTasksRunMultipleTimes");
                    } finally {
                        // wait for tasks to stop running
                        long waitForTaskCompletions = TimeUnit.SECONDS.toMillis(10);
                        Thread.sleep(waitForTaskCompletions);
                        server.stopServer("CWWKC1511W.*CancellationException" // task execution failed because it was canceled
                        );
                    }
            } finally {
                if (originalConfig != null)
                    server.updateServerConfiguration(originalConfig);
            }
    }

    @Test
    public void testScheduleAtFixedRateFE() throws Exception {
        runInServlet("testScheduleAtFixedRate");
    }

    @Test
    public void testScheduleAtFixedRateCreatePropFE() throws Exception {
        runInServlet("testScheduleAtFixedRateCreateProp");
    }

    @Test
    public void testScheduleAtFixedRateSuspendFE() throws Exception {
        runInServlet("testScheduleAtFixedRateSuspend");
    }

    @Test
    public void testScheduleAtFixedRateSuspendGetStatusFE() throws Exception {
        runInServlet("testScheduleAtFixedRateSuspendGetStatus");
    }

    @Test
    public void testScheduleCallableWithTriggerFE() throws Exception {
        runInServlet("testScheduleCallableWithTrigger");
    }

    @Test
    public void testScheduleCallableWithTriggerGetStatusFE() throws Exception {
        runInServlet("testScheduleCallableWithTriggerGetStatus");
    }

    @Test
    public void testScheduleCallableWithTriggerSuspendGetStatusFE() throws Exception {
        runInServlet("testScheduleCallableWithTriggerSuspendGetStatus");
    }

    @Test
    public void testScheduleCallableWithTriggerSuspendGetStatusCreatePropFE() throws Exception {
        runInServlet("testScheduleCallableWithTriggerSuspendGetStatusCreateProp");
    }

    @Test
    public void testScheduleRunnableWithTriggerFE() throws Exception {
        runInServlet("testScheduleRunnableWithTrigger");
    }

    @Test
    public void testScheduleRunnableWithTriggerCreatePropFE() throws Exception {
        runInServlet("testScheduleRunnableWithTriggerCreateProp");
    }
}