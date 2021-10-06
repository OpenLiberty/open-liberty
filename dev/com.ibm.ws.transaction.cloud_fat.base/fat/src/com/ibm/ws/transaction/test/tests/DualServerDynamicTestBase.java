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
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.transaction.fat.util.SetupRunner;

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

    private static DatabaseContainerType databaseContainerType;

    private SetupRunner runner = new SetupRunner() {
        @Override
        public void run(LibertyServer s) throws Exception {
            setUp(s);
        }
    };

    public void dynamicTest(LibertyServer server1, LibertyServer server2, int test, int resourceCount) throws Exception {
        final String method = "dynamicTest";
        final String id = String.format("%03d", test);

        // Start Servers
        if (databaseContainerType != DatabaseContainerType.Derby) {
            FATUtils.startServers(runner, server1, server2);
        } else {
            FATUtils.startServers(runner, server1);
        }

        try {
            // We expect this to fail since it is gonna crash the server
            runTestWithResponse(server1, servletName, "setupRec" + id);
        } catch (Throwable e) {
        }

        // wait for 1st server to have gone away
        assertNotNull(server1.getServerName() + " did not crash", server1.waitForStringInTrace("Dump State:"));

        server1.postStopServerArchive(); // must explicitly collect since crashed server

        // Now start server2
        if (databaseContainerType == DatabaseContainerType.Derby) {
            FATUtils.startServers(runner, server2);
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
        FATUtils.stopServers(server2);

        // restart 1st server
        FATUtils.startServers(runner, server1);

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
        FATUtils.stopServers(server1);
        FATUtils.startServers(runner, server1);

        // Check log was cleared
        assertNotNull("Transactions left in transaction log on " + server1.getServerName(), server1.waitForStringInTrace("WTRN0135I"));
        assertNotNull("XAResources left in partner log on " + server1.getServerName(), server1.waitForStringInTrace("WTRN0134I.*0"));

        // Finally stop server1
        FATUtils.stopServers(server1);
    }

    protected void tidyServersAfterTest(LibertyServer... servers) throws Exception {
        FATUtils.stopServers(servers);
        for (LibertyServer server : servers) {
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
