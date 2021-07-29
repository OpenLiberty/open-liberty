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
    protected static final int Cloud2ServerPort = 9992;

    public static LibertyServer server1;
    public static LibertyServer server2;

    public static String servletName;
    public static String cloud1RecoveryIdentity;

    public void dynamicTest(LibertyServer server1, LibertyServer server2, int test, int resourceCount) throws Exception {
        String testSuffix = String.format("%03d", test);
        dynamicTest(server1, server2, testSuffix, resourceCount);
    }

    public void dynamicTest(LibertyServer server1, LibertyServer server2, String testSuffix, int resourceCount) throws Exception {
        final String method = "dynamicTest";

        StringBuilder sb = null;
        boolean testFailed = false;
        String testFailureString = "";

        // Start Server1
        server1.startServer();

        try {
            // We expect this to fail since it is gonna crash the server
            sb = runTestWithResponse(server1, servletName, "setupRec" + testSuffix);
        } catch (Throwable e) {
            // as expected
            Log.error(this.getClass(), method, e); // TODO remove this
        }
        Log.info(this.getClass(), method, "setupRec" + testSuffix + " returned: " + sb);

        // wait for 1st server to have gone away
        if (server1.waitForStringInLog("Dump State:") == null) {
            testFailed = true;
            testFailureString = "First server did not crash";
        }

        // Now start server2
        if (!testFailed) {
            server2.setHttpDefaultPort(Cloud2ServerPort);
            ProgramOutput po = server2.startServerAndValidate(false, true, true);

            if (po.getReturnCode() != 0) {
                Log.info(this.getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
                Log.info(this.getClass(), method, "Stdout: " + po.getStdout());
                Log.info(this.getClass(), method, "Stderr: " + po.getStderr());
                Exception ex = new Exception("Could not start server2");
                Log.error(this.getClass(), "dynamicTest", ex);
                throw ex;
            }

            // wait for 2nd server to perform peer recovery
            if (server2.waitForStringInTrace("Performed recovery for " + cloud1RecoveryIdentity, LOG_SEARCH_TIMEOUT) == null) {
                testFailed = true;
                testFailureString = "Second server did not perform peer recovery";
            }
        }

        // flush the resource states
        if (!testFailed) {

            try {
                sb = runTestWithResponse(server2, servletName, "dumpState");
                Log.info(this.getClass(), method, sb.toString());
            } catch (Exception e) {
                Log.error(this.getClass(), method, e);
                throw e;
            }

            //Stop server2
            server2.stopServer((String[]) null);

            // restart 1st server
            server1.startServerAndValidate(false, true, true);

            if (server1.waitForStringInTrace("WTRN0133I") == null) {
                testFailed = true;
                testFailureString = "Recovery incomplete on first server";
            }
        }

        if (!testFailed) {

            // check resource states
            Log.info(this.getClass(), method, "calling checkRec" + testSuffix);
            try {
                sb = runTestWithResponse(server1, servletName, "checkRec" + testSuffix);
            } catch (Exception e) {
                Log.error(this.getClass(), "dynamicTest", e);
                throw e;
            }
            Log.info(this.getClass(), method, "checkRec" + testSuffix + " returned: " + sb);

            // Bounce first server to clear log
            server1.stopServer("CWWKN0005W"); // LastingXAResourceImpl handles CWWKN0005W
            server1.startServerAndValidate(false, true, true);

            // Check log was cleared
            if (server1.waitForStringInTrace("WTRN0135I") == null) {
                testFailed = true;
                testFailureString = "Transactions left in transaction log on first server";
            }
            if (!testFailed && (server1.waitForStringInTrace("WTRN0134I.*0") == null)) {
                testFailed = true;
                testFailureString = "XAResources left in partner log on first server";
            }

        }

        tidyServersAfterTest(server1, server2);
        // XA resource data is cleared in setup servlet methods. Probably should do it here.
        if (testFailed)
            fail(testFailureString);
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
    }
}
