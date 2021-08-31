/*******************************************************************************
 * Copyright (c) 2019,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;

import com.ibm.tx.jta.ut.util.LastingXAResourceImpl;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.Mode;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/*
 * These tests are based on the original JTAREC recovery tests.
 * Test plan is attached to RTC WI 213854
 */
@Mode
public abstract class DualServerDynamicTestBase extends FATServletClient {

    protected static final int LOG_SEARCH_TIMEOUT = 300000;

    protected static LibertyServer serverTemplate;
    public static final String APP_NAME = "transaction";

    public static LibertyServer server1;
    public static LibertyServer server2;

    public static String servletName;
    public static String cloud1RecoveryIdentity;

    private static DatabaseContainerType databaseContainerType = DatabaseContainerType.Postgres;

    public void dynamicTest(LibertyServer server1, LibertyServer server2, int test, int resourceCount) throws Exception {
        final String method = "dynamicTest";
        final String id = String.format("%03d", test);

        // Start Servers
        if (databaseContainerType != DatabaseContainerType.Derby) {
            startServers(server1, server2);
        } else {
            startServers(server1);
        }

        try {
            // We expect this to fail since it is gonna crash the server
            runTestWithResponse(server1, servletName, "setupRec" + id);
        } catch (Throwable e) {
        }

        // wait for 1st server to have gone away
        assertNotNull(server1.getServerName() + " did not crash", server1.waitForStringInTrace("Dump State:"));

        // Now start server2
        if (databaseContainerType == DatabaseContainerType.Derby) {
            startServers(server2);
        }

        // wait for 2nd server to perform peer recovery
        assertNotNull(server2.getServerName() + " did not perform peer recovery",
                      server2.waitForStringInTrace("Performed recovery for " + cloud1RecoveryIdentity, LOG_SEARCH_TIMEOUT));

        // flush the resource states
        try {
            runTestWithResponse(server2, servletName, "dumpState");
        } catch (Exception e) {
            Log.error(this.getClass(), method, e);
            fail(e.getMessage());
        }

        //Stop server2
        stopServers(server2);

        // restart 1st server
        server1.resetStarted();
        startServers(server1);

        assertNotNull("Recovery incomplete on " + server1.getServerName(), server1.waitForStringInTrace("WTRN0133I"));

        // check resource states
        Log.info(this.getClass(), method, "calling checkRec" + id);
        try {
            runTestWithResponse(server1, servletName, "checkRec" + id);
        } catch (Exception e) {
            Log.error(this.getClass(), "dynamicTest", e);
            throw e;
        }

        // Bounce first server to clear log
        stopServers(server1);
        startServers(server1);

        // Check log was cleared
        assertNotNull("Transactions left in transaction log on " + server1.getServerName(), server1.waitForStringInTrace("WTRN0135I"));
        assertNotNull("XAResources left in partner log on " + server1.getServerName(), server1.waitForStringInTrace("WTRN0134I.*0"));

        // Finally stop server1
        stopServers(server1);
    }

    protected void tidyServersAfterTest(LibertyServer... servers) throws Exception {
        for (LibertyServer server : servers) {
            if (server.isStarted()) {
                server.stopServer();
            }
            try {
                final RemoteFile rf = server.getFileFromLibertySharedDir(LastingXAResourceImpl.STATE_FILE_ROOT);
                if (rf.exists()) {
                    rf.delete();
                }
            } catch (FileNotFoundException e) {
                // Already gone
            }
        }
    }

    protected void startServers(LibertyServer... servers) throws Exception {
        final String method = "startServers";

        for (LibertyServer server : servers) {
            assertNotNull("Attempted to start a null server", server);
            int attempt = 0;
            int maxAttempts = 5;

            do {
                if (attempt++ > 0) {
                    Log.info(getClass(), method, "Waiting 5 seconds after start failure before making attempt " + attempt);
                    try {
                        Thread.sleep(5000);
                    } catch (Exception e) {
                        Log.error(getClass(), method, e);
                    }
                }

                if (server.isStarted()) {
                    String pid = server.getPid();
                    Log.info(getClass(), method,
                             "Server " + server.getServerName() + " is already running." + ((pid != null ? "(pid:" + pid + ")" : "")) + " Maybe it is on the way down.");
                    server.printProcesses();
                    continue;
                }

                ProgramOutput po = null;
                try {
                    setUp(server);
                    po = server.startServerAndValidate(false, false, true);
                } catch (Exception e) {
                    Log.error(getClass(), method, e, "Server start attempt " + attempt + " failed with return code " + (po != null ? po.getReturnCode() : "<unavailable>"));
                }

                if (po != null) {
                    String s;
                    int rc = po.getReturnCode();

                    Log.info(getClass(), method, "ReturnCode: " + rc);

                    s = server.getPid();

                    if (s != null && !s.isEmpty())
                        Log.info(getClass(), method, "Pid: " + s);

                    s = po.getStdout();

                    if (s != null && !s.isEmpty())
                        Log.info(getClass(), method, "Stdout: " + s.trim());

                    s = po.getStderr();

                    if (s != null && !s.isEmpty())
                        Log.info(getClass(), method, "Stderr: " + s.trim());

                    if (rc == 0) {
                        break;
                    } else {
                        String pid = server.getPid();
                        Log.info(getClass(), method,
                                 "Non zero return code starting server " + server.getServerName() + "." + ((pid != null ? "(pid:" + pid + ")" : ""))
                                                     + " Maybe it is on the way down.");
                        server.printProcessHoldingPort(server.getHttpDefaultPort());
                    }
                }
            } while (attempt < maxAttempts);

            if (!server.isStarted()) {
                server.postStopServerArchive();
                throw new Exception("Failed to start " + server.getServerName() + " after " + attempt + " attempts");
            }
        }
    }

    /**
     * @param server
     * @throws Exception
     */
    protected abstract void setUp(LibertyServer server) throws Exception;

    protected void stopServers(LibertyServer... servers) throws Exception {
        final String method = "stopServers";

        for (LibertyServer server : servers) {
            assertNotNull("Attempted to stop a null server", server);
            int attempt = 0;
            int maxAttempts = 5;

            do {
                if (attempt++ > 0) {
                    Log.info(getClass(), method, "Waiting 5 seconds after stop failure before making attempt " + attempt);
                    try {
                        Thread.sleep(5000);
                    } catch (Exception e) {
                        Log.error(getClass(), method, e);
                    }
                }

                if (!server.isStarted()) {
                    Log.info(getClass(), method,
                             "Server " + server.getServerName() + " is not started. Maybe it is on the way up.");
                    continue;
                }

                ProgramOutput po = null;
                try {
                    po = server.stopServer((String[]) null);
                } catch (Exception e) {
                    Log.error(getClass(), method, e, "Server stop attempt " + attempt + " failed with return code " + (po != null ? po.getReturnCode() : "<unavailable>"));
                }

                if (po != null) {
                    String s;
                    int rc = po.getReturnCode();

                    Log.info(getClass(), method, "ReturnCode: " + rc);

                    s = server.getPid();

                    if (s != null && !s.isEmpty())
                        Log.info(getClass(), method, "Pid: " + s);

                    s = po.getStdout();

                    if (s != null && !s.isEmpty())
                        Log.info(getClass(), method, "Stdout: " + s.trim());

                    s = po.getStderr();

                    if (s != null && !s.isEmpty())
                        Log.info(getClass(), method, "Stderr: " + s.trim());

                    if (rc == 0) {
                        break;
                    } else {
                        String pid = server.getPid();
                        Log.info(getClass(), method,
                                 "Non zero return code stopping server " + server.getServerName() + "." + ((pid != null ? "(pid:" + pid + ")" : "")));
                        server.printProcessHoldingPort(server.getHttpDefaultPort());
                    }
                }
            } while (attempt < maxAttempts);

            if (server.isStarted()) {
                server.postStopServerArchive();
                throw new Exception("Failed to stop " + server.getServerName() + " after " + attempt + " attempts");
            }
        }
    }

    /**
     * @param firstServer
     * @param secondServer
     * @param string
     * @param string2
     */
    public static void setup(LibertyServer s1, LibertyServer s2, String servlet, String recoveryId) throws Exception {
        server1 = s1;
        server2 = s2;
        servletName = APP_NAME + "/" + servlet;
        cloud1RecoveryIdentity = recoveryId;

        ShrinkHelper.defaultApp(server1, APP_NAME, "com.ibm.ws.transaction.*");
        ShrinkHelper.defaultApp(server2, APP_NAME, "com.ibm.ws.transaction.*");

        server1.setServerStartTimeout(LOG_SEARCH_TIMEOUT);
        server2.setServerStartTimeout(LOG_SEARCH_TIMEOUT);

        server2.setHttpDefaultPort(server2.getHttpSecondaryPort());
    }

    public static void setDBType(DatabaseContainerType type) {
        databaseContainerType = type;
    }
}
