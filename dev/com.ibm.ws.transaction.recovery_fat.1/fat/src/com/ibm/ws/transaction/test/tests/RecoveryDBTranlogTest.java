/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
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
package com.ibm.ws.transaction.test.tests;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.transaction.fat.util.FATUtils;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Example Shrinkwrap FAT project:
 * <li> Application packaging is done in the @BeforeClass, instead of ant scripting.
 * <li> Injects servers via @Server annotation. Annotation value corresponds to the
 * server directory name in 'publish/servers/%annotation_value%' where ports get
 * assigned to the LibertyServer instance when the 'testports.properties' does not
 * get used.
 * <li> Specifies an @RunWith(FATRunner.class) annotation. Traditionally this has been
 * added to bytecode automatically by ant.
 * <li> Uses the @TestServlet annotation to define test servlets. Notice that not all @Test
 * methods are defined in this class. All of the @Test methods are defined on the test
 * servlet referenced by the annotation, and will be run whenever this test class runs.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class RecoveryDBTranlogTest extends FATServletClient {

    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = APP_NAME + "/RecoveryServlet";

    @Server("recovery.dblog")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        ShrinkHelper.defaultApp(server, APP_NAME, "com.ibm.ws.transaction.*");

        server.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        FATUtils.startServers(server);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        FATUtils.stopServers(server);
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec000DBLog() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "007");
        FATUtils.recoveryTest(server, SERVLET_NAME, "090");
    }

    @Test
    public void testRec001DBLog() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "001");
    }

    @Test
    public void testRec002DBLog() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "002");
    }

    @Test
    public void testRec003DBLog() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "003");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException",
                           "javax.transaction.RollbackException" })
    public void testRec004DBLog() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "004");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException",
                           "javax.transaction.RollbackException" })
    public void testRec005DBLog() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "005");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException",
                           "javax.transaction.RollbackException" })
    public void testRec006DBLog() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "006");
    }

    @Test
    @Mode(TestMode.LITE)
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec007DBLog() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "007");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec008DBLog() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "008");
    }

    @Test
    public void testRec009DBLog() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "009");
    }

    @Test
    public void testRec010DBLog() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "010");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec011DBLog() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "011");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec012DBLog() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "012");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec013DBLog() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "013");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec014DBLog() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "014");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec015DBLog() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "015");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException",
                           "javax.transaction.RollbackException" })
    public void testRec016DBLog() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "016");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec017DBLog() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "017");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec018DBLog() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "018");
    }

    @Test
    public void testRec047DBLog() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "047");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec048DBLog() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "048");
    }

    @Test
    public void testRec050DBLog() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "050");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec051DBLog() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "051");
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testRec090DBLog() throws Exception {
        FATUtils.recoveryTest(server, SERVLET_NAME, "090");
    }
}
