/*******************************************************************************
 * Copyright 2023 IBM Corporation and others.
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
package io.openliberty.checkpoint.fat;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.fat.util.FATUtils;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
@CheckpointTest
public class RecoveryTestBase extends FATServletClient {

    static final String SERVER_NAME = "checkpointTransactionRecovery";
    public static final String APP_NAME = "transactionrecovery";
    public static final String SERVLET_NAME = APP_NAME + "/RecoveryServlet";

    public static LibertyServer server;

    // TODO: Discuss adding abstraction and support to FATUtils and
    // RecoveryTestBase to enable reuse for InstantOn test cases.
    protected static void setUp(LibertyServer ls) throws Exception {
        Log.info(RecoveryTestBase.class, "setup", ls.getServerName());

        server = ls;

        //ShrinkHelper.defaultApp(server, APP_NAME, "servlets.recovery.*");

        // Restore the server
        //server.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        FATUtils.startServers(server);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        FATUtils.stopServers(server);
        FATUtils.deleteXARecoveryDat(server);
    }

//    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRecovery000() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "007");
        FATUtils.recoveryTest(server, SERVLET_NAME, "090");
    }

//   @Mode(TestMode.LITE)
    @Test
    public void testRecovery001() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "001");
    }

    @Test
    public void testRecovery002() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "002");
    }

    @Test
    public void testRecovery003() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "003");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException",
                           "javax.transaction.RollbackException" })
    public void testRecovery004() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "004");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException",
                           "javax.transaction.RollbackException" })
    public void testRecovery005() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "005");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException",
                           "javax.transaction.RollbackException" })
    public void testRecovery006() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "006");
    }

    @Test
    @Mode(TestMode.LITE)
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRecovery007() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "007");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRecovery008() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "008");
    }

    @Test
    public void testRecovery009() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "009");
    }

    @Test
    public void testRecovery010() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "010");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRecovery011() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "011");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRecovery012() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "012");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRecovery013() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "013");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRecovery014() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "014");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRecovery015() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "015");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException",
                           "javax.transaction.RollbackException" })
    public void testRecovery016() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "016");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRecovery017() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "017");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRecovery018() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "018");
    }

    @Test
    public void testRecovery047() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "047");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRecovery048() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "048");
    }

    @Test
    public void testRecovery050() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "050");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRecovery051() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "051");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRecovery090() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "090");
    }
}
