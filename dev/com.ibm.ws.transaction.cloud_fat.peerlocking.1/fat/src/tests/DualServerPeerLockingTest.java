/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
package tests;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.After;
import org.junit.BeforeClass;

import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.transaction.fat.util.TxShrinkHelper;
import com.ibm.ws.transaction.fat.util.TxTestContainerSuite;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.impl.LibertyServer;
import servlets.Simple2PCCloudServlet;

/**
 *
 */
public class DualServerPeerLockingTest extends DualServerDynamicTestBase {

    @Server("com.ibm.ws.transaction_LKCLOUD001")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer firstServer;

    @Server("com.ibm.ws.transaction_LKCLOUD002")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer secondServer;

    @Server("defaultAttributesServer1")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer defaultAttributesServer1;

    @Server("defaultAttributesServer2")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer defaultAttributesServer2;

    @Server("longPeerStaleTimeServer1")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer longPeerStaleTimeServer1;

    @Server("longPeerStaleTimeServer2")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer longPeerStaleTimeServer2;

    @Server("peerLockingDisabledServer1")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer peerLockingDisabledServer1;

    @Server("peerLockingEnabledServer1")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer peerLockingEnabledServer1;

    public static String[] serverNames = new String[] {
                                                        "com.ibm.ws.transaction_LKCLOUD001",
                                                        "com.ibm.ws.transaction_LKCLOUD002",
                                                        "defaultAttributesServer1",
                                                        "defaultAttributesServer2",
                                                        "longPeerStaleTimeServer1",
                                                        "longPeerStaleTimeServer2",
                                                        "peerLockingDisabledServer1",
                                                        "peerLockingEnabledServer1",
    };

    protected LibertyServer[] servers = new LibertyServer[] { server1, server2 };

    @BeforeClass
    public static void setUp() throws Exception {
        System.out.println("NYTRACE: DualServerPeerLockingTest.setUp called");
        servletName = APP_NAME + "/Simple2PCCloudServlet";
        cloud1RecoveryIdentity = "cloud001";

        // Pretend we're Derby even though we're not using test containers here
        TxTestContainerSuite.databaseContainerType = DatabaseContainerType.Derby;

        TxShrinkHelper.defaultApp(firstServer, APP_NAME, APP_PATH, "servlets.*");
        TxShrinkHelper.defaultApp(secondServer, APP_NAME, APP_PATH, "servlets.*");
        TxShrinkHelper.defaultApp(defaultAttributesServer1, APP_NAME, APP_PATH, "servlets.*");
        TxShrinkHelper.defaultApp(defaultAttributesServer2, APP_NAME, APP_PATH, "servlets.*");
        TxShrinkHelper.defaultApp(longPeerStaleTimeServer1, APP_NAME, APP_PATH, "servlets.*");
        TxShrinkHelper.defaultApp(longPeerStaleTimeServer2, APP_NAME, APP_PATH, "servlets.*");
        TxShrinkHelper.defaultApp(peerLockingDisabledServer1, APP_NAME, APP_PATH, "servlets.*");
        TxShrinkHelper.defaultApp(peerLockingEnabledServer1, APP_NAME, APP_PATH, "servlets.*");

        firstServer.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        secondServer.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        secondServer.useSecondaryHTTPPort();
        defaultAttributesServer1.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        defaultAttributesServer2.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        defaultAttributesServer2.useSecondaryHTTPPort();
        longPeerStaleTimeServer1.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        longPeerStaleTimeServer2.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        longPeerStaleTimeServer2.useSecondaryHTTPPort();
        peerLockingDisabledServer1.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        peerLockingEnabledServer1.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
    }

    /**
     *
     */
    public DualServerPeerLockingTest() {
        super();
    }

    @After
    public void tearDown() throws Exception {
        tidyServersAfterTest(servers);
    }

    @Override
    public void dynamicTest(LibertyServer s1, LibertyServer s2, int test, int resourceCount) throws Exception {
        String testSuffix = String.format("%03d", test);
        dynamicTest(s1, s2, testSuffix, resourceCount);
    }

    protected void dynamicTest(LibertyServer s1, LibertyServer s2, String testSuffix, int resourceCount) throws Exception {
        final String method = "dynamicTest";

        server1 = s1;
        server2 = s2;

        servers = new LibertyServer[] { s1, s2 };

        // Start Server1
        FATUtils.startServers(server1);

        try {
            // We expect this to fail since it is gonna crash the server
            runTest(server1, servletName, "setupRec" + testSuffix);
        } catch (IOException e) {
        }

        // wait for 1st server to have gone away
        assertNotNull(server1.getServerName() + " did not crash", server1.waitForStringInTrace(XAResourceImpl.DUMP_STATE));

        // Start Server2
        FATUtils.startServers(server2);

        // wait for 2nd server to perform peer recovery
        assertNotNull(server2.getServerName() + " did not perform peer recovery",
                      server2.waitForStringInTrace("Performed recovery for " + cloud1RecoveryIdentity, FATUtils.LOG_SEARCH_TIMEOUT));

        // flush the resource states - retry a few times if this fails
        FATUtils.runWithRetries(() -> runTestWithResponse(server2, servletName, "dumpState").toString());

        //Stop server2
        FATUtils.stopServers(server2);

        // restart 1st server
        FATUtils.startServers(server1);

        assertNotNull("Recovery incomplete on " + server1.getServerName(), server1.waitForStringInTrace("WTRN0133I"));

        // check resource states - retry a few times if this fails
        FATUtils.runWithRetries(() -> runTestWithResponse(server1, servletName, "checkRec" + testSuffix).toString());

        // Bounce first server to clear log
        FATUtils.stopServers(new String[] { "CWWKN0005W" }, server1);
        FATUtils.startServers(server1);

        // Check log was cleared
        assertNotNull("Transactions left in transaction log on " + server1.getServerName(), server1.waitForStringInTrace("WTRN0135I", FATUtils.LOG_SEARCH_TIMEOUT));
        assertNotNull("XAResources left in partner log on " + server1.getServerName(), server1.waitForStringInTrace("WTRN0134I.*0", FATUtils.LOG_SEARCH_TIMEOUT));
    }

    @Override
    public void setUp(LibertyServer server) throws Exception {
    }
}