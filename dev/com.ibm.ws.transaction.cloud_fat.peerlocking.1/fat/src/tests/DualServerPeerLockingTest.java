/*******************************************************************************
 * Copyright (c) 2019, 2025 IBM Corporation and others.
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
import java.util.Arrays;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;

import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.transaction.fat.util.FATUtils;

import componenttest.annotation.Server;
import componenttest.topology.impl.LibertyServer;

public class DualServerPeerLockingTest extends DualServerDynamicTestBase {

    @Server("com.ibm.ws.transaction_LKCLOUD001")
    public static LibertyServer s1;

    @Server("com.ibm.ws.transaction_LKCLOUD002")
    public static LibertyServer s2;

    @Server("defaultAttributesServer1")
    public static LibertyServer defaultAttributesServer1;

    @Server("defaultAttributesServer2")
    public static LibertyServer defaultAttributesServer2;

    @Server("longPeerStaleTimeServer1")
    public static LibertyServer longPeerStaleTimeServer1;

    @Server("longPeerStaleTimeServer2")
    public static LibertyServer longPeerStaleTimeServer2;

    @Server("peerLockingDisabledServer1")
    public static LibertyServer peerLockingDisabledServer1;

    @Server("peerLockingEnabledServer1")
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

    @BeforeClass
    public static void setUp() throws Exception {
        server1 = s1;
        server2 = s2;

        servletName = APP_NAME + "/Simple2PCCloudServlet";
        cloud1RecoveryIdentity = "cloud001";
        final WebArchive app = ShrinkHelper.buildDefaultApp(APP_NAME, "servlets.*");
        final DeployOptions[] dO = new DeployOptions[0];

        ShrinkHelper.exportAppToServer(server1, app, dO);
        ShrinkHelper.exportAppToServer(server2, app, dO);
        ShrinkHelper.exportAppToServer(defaultAttributesServer1, app, dO);
        ShrinkHelper.exportAppToServer(defaultAttributesServer2, app, dO);
        ShrinkHelper.exportAppToServer(longPeerStaleTimeServer1, app, dO);
        ShrinkHelper.exportAppToServer(longPeerStaleTimeServer2, app, dO);
        ShrinkHelper.exportAppToServer(peerLockingDisabledServer1, app, dO);
        ShrinkHelper.exportAppToServer(peerLockingEnabledServer1, app, dO);

        s1.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        s2.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        s2.useSecondaryHTTPPort();
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

    @Override
    public void dynamicTest(LibertyServer s1, LibertyServer s2, int test, int resourceCount) throws Exception {
        String testSuffix = String.format("%03d", test);
        dynamicTest(s1, s2, testSuffix, resourceCount);
    }

    protected void dynamicTest(LibertyServer s1, LibertyServer s2, String testSuffix, int resourceCount) throws Exception {

        server1 = s1;
        server2 = s2;

        serversToCleanup = Arrays.asList(s1, s2);

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