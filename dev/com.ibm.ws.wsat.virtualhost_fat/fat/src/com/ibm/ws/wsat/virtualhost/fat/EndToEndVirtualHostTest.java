/*******************************************************************************
 * Copyright 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.wsat.virtualhost.fat;

import static junit.framework.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.transaction.fat.util.FATUtils;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * FAT-4: Two-server end-to-end WS-AT transaction test with virtualHostRef
 * configured on the coordinator (server1). Verifies that WS-AT 2PC completes
 * correctly when the coordinator's WS-AT endpoints are restricted to a
 * non-default port via virtualHostRef — i.e., server1 publishes EPRs containing
 * the virtualHost port, and server2 can successfully reach those EPRs for 2PC.
 *
 * Reuses the testCoordinator/testParticipant applications from wsat.common_fat
 * (coordinator servlet + Derby-backed participant web service).
 */
@RunWith(FATRunner.class)
public class EndToEndVirtualHostTest {

    private static final LibertyServer server1 =
        LibertyServerFactory.getLibertyServer("WsatEndToEndServer1");
    private static final LibertyServer server2 =
        LibertyServerFactory.getLibertyServer("WsatEndToEndServer2");

    private static String coordRoot;
    private static String partRoot;
    private static String remote;

    @BeforeClass
    public static void setUp() throws Exception {
        // server2's HTTP default port must be set to the secondary port before start,
        // matching the httpEndpoint in WsatEndToEndServer2/server.xml.
        server2.setHttpDefaultPort(server2.getHttpSecondaryPort());

        ShrinkHelper.defaultDropinApp(server1, "testCoordinator", "com.ibm.ws.wsat.coor.*");
        ShrinkHelper.defaultDropinApp(server2, "testParticipant", "com.ibm.ws.wsat.part.*");

        FATUtils.startServers(server1, server2);

        coordRoot = "http://" + server1.getHostname() + ":"
                    + server1.getHttpDefaultPort() + "/testCoordinator";
        partRoot = "http://" + server2.getHostname() + ":"
                   + server2.getHttpSecondaryPort() + "/testParticipant";
        remote = partRoot + "/WSATTestService";
    }

    @AfterClass
    public static void tearDown() throws Exception {
        FATUtils.stopServers(server1, server2);
    }

    /**
     * FAT-4a: Commit with virtualHostRef on coordinator.
     * Verifies that WS-AT 2PC commit completes end-to-end when the coordinator
     * restricts its WS-AT endpoints to the virtualHost (secondary) port.
     */
    @Test
    public void testCommitWithVirtualHostRef() throws Exception {
        invokeOp("init");
        assertEquals("0/0", invokeOp("query"));
        invokeOp("commit", "value=GOOD");
        assertEquals("GOOD/GOOD", invokeOp("query"));
    }

    /**
     * FAT-4b: Rollback with virtualHostRef on coordinator.
     * Verifies that WS-AT 2PC rollback completes end-to-end.
     */
    @Test
    public void testRollbackWithVirtualHostRef() throws Exception {
        invokeOp("init");
        assertEquals("0/0", invokeOp("query"));
        invokeOp("rollback", "value=BAD");
        assertEquals("0/0", invokeOp("query"));
    }

    /**
     * FAT-4c: Remote failure with virtualHostRef on coordinator.
     * Verifies that a remote participant failure causes a rollback via WS-AT 2PC.
     */
    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testRemoteFailureWithVirtualHostRef() throws Exception {
        invokeOp("init");
        assertEquals("0/0", invokeOp("query"));
        invokeOp("commit", "value=REMOTE-FAIL");
        assertEquals("0/0", invokeOp("query"));
    }

    private String invokeOp(String op, String... parms) throws Exception {
        String uri = coordRoot + "?op=" + op + "&remote=" + remote;
        for (String parm : parms) {
            uri += "&" + parm;
        }
        URL url = new URL(uri);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestMethod("GET");
        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
        return br.readLine();
    }
}
