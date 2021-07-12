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

    /**
     * @deprecated Use {@link #dynamicTest(LibertyServer,LibertyServer,int,int)} instead
     */
    @Deprecated
    public void dynamicTest(int test, int resourceCount) throws Exception {
        dynamicTest(server1, server2, test, resourceCount);
    }

    public void dynamicTest(LibertyServer server1, LibertyServer server2, int test, int resourceCount) throws Exception {
        String testSuffix = String.format("%03d", test);
        dynamicTest(server1, server2, testSuffix, resourceCount);
    }

    protected void tidyServerAfterTest(LibertyServer s) throws Exception {
        if (s.isStarted()) {
            s.stopServer(new String[] { "CWWKE0701E" });
        }
        try {
            final RemoteFile rf = s.getFileFromLibertySharedDir(LastingXAResourceImpl.STATE_FILE_ROOT);
            if (rf.exists()) {
                rf.delete();
            }
        } catch (FileNotFoundException e) {
            // Already gone
        }

    }

    public void dynamicTest(LibertyServer server1, LibertyServer server2, String id, int resourceCount) throws Exception {
        final String method = "dynamicTest";
        StringBuilder sb = null;
        Log.info(this.getClass(), method, "Starting dynamic test in DualServerDynamicDBRotationTest");
        // Start Server1
        startServers(server1);
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
        server2.setHttpDefaultPort(Cloud2ServerPort);
        startServers(server2);

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
        server1.stopServer(new String[] { "CWWKE0701E" });
        startServers(server1);

        // Check log was cleared
        assertNotNull("Transactions left in transaction log on " + server1.getServerName(), server1.waitForStringInTrace("WTRN0135I"));
        assertNotNull("XAResources left in partner log on " + server1.getServerName(), server1.waitForStringInTrace("WTRN0134I.*0"));
    }

    private void startServers(LibertyServer... servers) {
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
                assertNull("Failed to start server: " + t.getMessage() + (po == null ? "" : " " + po.getStdout()), t);
            }
        }
    }

    /**
     * @param server
     * @throws Exception
     */
    protected abstract void setUp(LibertyServer server) throws Exception;
}
