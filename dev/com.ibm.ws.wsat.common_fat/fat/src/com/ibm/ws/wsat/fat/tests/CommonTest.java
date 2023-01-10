/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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
package com.ibm.ws.wsat.fat.tests;

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

/*
 * Simple WSAT end-to-end test
 */
@RunWith(FATRunner.class)
public class CommonTest {

    private static LibertyServer coordinator = LibertyServerFactory.getLibertyServer("coordinator");
    private static LibertyServer participant = LibertyServerFactory.getLibertyServer("participant");

    private static String coordRoot;
    private static String partRoot;
    private static String remote;

    @BeforeClass
    public static void before() throws Exception {
    	
    	participant.setHttpDefaultPort(participant.getHttpSecondaryPort());
    	
        ShrinkHelper.defaultDropinApp(coordinator, "testCoordinator", "com.ibm.ws.wsat.coor.*");
        ShrinkHelper.defaultDropinApp(participant, "testParticipant", "com.ibm.ws.wsat.part.*");

        FATUtils.startServers(coordinator, participant);

        coordRoot = "http://" + coordinator.getHostname() + ":" + coordinator.getHttpDefaultPort() + "/testCoordinator";
        partRoot = "http://" + participant.getHostname() + ":" + participant.getHttpSecondaryPort() + "/testParticipant";

        remote = partRoot + "/WSATTestService";
    }

    @AfterClass
    public static void after() throws Exception {
    	FATUtils.stopServers(coordinator, participant);
    }

    @Test
    public void testCommit() throws Exception {
        invokeOp("init");
        assertEquals("0/0", invokeOp("query"));

        // Not sure this one actually proves very much.  It will likely pass 
        // even without WS-AT transactions present :-(
        invokeOp("commit", "value=GOOD");
        assertEquals("GOOD/GOOD", invokeOp("query"));
    }

    @Test
    public void testRollback() throws Exception {
        invokeOp("init");
        assertEquals("0/0", invokeOp("query"));

        // This one does need a distributed tran for it to pass :-)
        invokeOp("rollback", "value=BAD");
        assertEquals("0/0", invokeOp("query"));
    }

    @Test
    public void testLocalFailure() throws Exception {
        invokeOp("init");
        assertEquals("0/0", invokeOp("query"));

        invokeOp("commit", "value=LOCAL-FAIL");
        assertEquals("0/0", invokeOp("query"));
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void testRemoteFailure() throws Exception {
        invokeOp("init");
        assertEquals("0/0", invokeOp("query"));

        invokeOp("commit", "value=REMOTE-FAIL");
        assertEquals("0/0", invokeOp("query"));
    }

    @Test
    public void testRemoteException() throws Exception {
        invokeOp("init");
        assertEquals("0/0", invokeOp("query"));

        invokeOp("commit", "value=REMOTE-EX");
        assertEquals("REMOTE-EX/REMOTE-EX", invokeOp("query"));
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
