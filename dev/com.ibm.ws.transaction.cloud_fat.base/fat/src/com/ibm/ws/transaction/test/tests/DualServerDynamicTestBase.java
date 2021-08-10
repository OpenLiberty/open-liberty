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
import static org.junit.Assert.assertNull;
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
        StringBuilder sb = null;
        Log.info(this.getClass(), method, "Starting dynamic test in DualServerDynamicDBRotationTest");
        // Start Servers
        if (databaseContainerType != DatabaseContainerType.Derby) {
            startServers(server1, server2);
        } else {
            startServers(server1);
        }
        Log.info(this.getClass(), method, "now invoke runTestWithResponse from DualServerDynamicDBRotationTest");
        try {
            // We expect this to fail since it is gonna crash the server
            sb = runTestWithResponse(server1, servletName, "setupRec" + id);
        } catch (Throwable e) {
        }
        Log.info(this.getClass(), method, "back from runTestWithResponse in DualServerDynamicDBRotationTest, sb is " + sb);
        assertNull("setupRec" + id + " returned: " + sb, sb);

        Log.info(this.getClass(), method, "wait for first server to go away in DualServerDynamicDBRotationTest");
        // wait for 1st server to have gone away
        assertNotNull(server1.getServerName() + " did not crash", server1.waitForStringInLog("Dump State:"));

        // Now start server2
        if (databaseContainerType == DatabaseContainerType.Derby) {
            startServers(server2);
        }

        // wait for 2nd server to perform peer recovery
        assertNotNull(server2.getServerName() + " did not perform peer recovery",
                      server2.waitForStringInTrace("Performed recovery for " + cloud1RecoveryIdentity, LOG_SEARCH_TIMEOUT));

        // flush the resource states
        try {
            sb = runTestWithResponse(server2, servletName, "dumpState");
            Log.info(this.getClass(), method, sb.toString());
        } catch (Exception e) {
            Log.error(this.getClass(), method, e);
            fail(e.getMessage());
        }

        //Stop server2
        server2.stopServer((String[]) null);

        // restart 1st server
        server1.resetStarted();
        startServers(server1);

        assertNotNull("Recovery incomplete on " + server1.getServerName(), server1.waitForStringInTrace("WTRN0133I"));

        // check resource states
        Log.info(this.getClass(), method, "calling checkRec" + id);
        try {
            sb = runTestWithResponse(server1, servletName, "checkRec" + id);
        } catch (Exception e) {
            Log.error(this.getClass(), "dynamicTest", e);
            throw e;
        }
        Log.info(this.getClass(), method, "checkRec" + id + " returned: " + sb);

        // Bounce first server to clear log
        server1.stopServer((String[]) null);
        startServers(server1);

        // Check log was cleared
        assertNotNull("Transactions left in transaction log on " + server1.getServerName(), server1.waitForStringInTrace("WTRN0135I"));
        assertNotNull("XAResources left in partner log on " + server1.getServerName(), server1.waitForStringInTrace("WTRN0134I.*0"));
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

    protected void startServers(LibertyServer... servers) {
        final String method = "startServers";

        for (LibertyServer server : servers) {
            assertNotNull("Attempted to start a null server", server);
            ProgramOutput po = null;
            try {
                setUp(server);
                po = server.startServerAndValidate(false, false, false);
                if (po.getReturnCode() != 0) {
                    Log.info(getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
                    Log.info(getClass(), method, "Stdout: " + po.getStdout());
                    Log.info(getClass(), method, "Stderr: " + po.getStderr());
                    throw new Exception(po.getCommand() + " returned " + po.getReturnCode());
                }
                server.validateAppLoaded(APP_NAME);
            } catch (Throwable t) {
                Log.error(getClass(), method, t);
                // We need the logs for this server to debug what the problem was
                try {
                    server.postStopServerArchive();
                } catch (Exception e) {
                    Log.error(getClass(), method, e);
                }
                assertNull("Failed to start server: " + t.getMessage() + (po == null ? "" : " " + po.getStdout()), t);
            }
        }
    }

    /**
     * @param server
     * @throws Exception
     */
    protected abstract void setUp(LibertyServer server) throws Exception;

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
