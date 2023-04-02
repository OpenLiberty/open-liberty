/*******************************************************************************
 * Copyright (c) 2015, 2023 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.mdb.jms.fat.tests;

import static org.junit.Assert.assertNotNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 *
 */
@RunWith(FATRunner.class)
public class MDB20Test extends FATServletClient {

    @Server("ejbcontainer.mdb.jms.fat.mdb20")
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().forServers("ejbcontainer.mdb.jms.fat.mdb20")).andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly().forServers("ejbcontainer.mdb.jms.fat.mdb20")).andWith(new JakartaEE9Action().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11).forServers("ejbcontainer.mdb.jms.fat.mdb20")).andWith(new JakartaEE10Action().forServers("ejbcontainer.mdb.jms.fat.mdb20"));

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the Ears & Wars

        //#################### MDB20TestApp.ear
        JavaArchive MDB20TestEJB = ShrinkHelper.buildJavaArchive("MDB20TestEJB.jar", "com.ibm.ws.ejbcontainer.mdb.jms.ejb20.");
        MDB20TestEJB = (JavaArchive) ShrinkHelper.addDirectory(MDB20TestEJB, "test-applications/MDB20TestEJB.jar/resources");
        WebArchive MDB20TestWeb = ShrinkHelper.buildDefaultApp("MDB20TestWeb.war", "com.ibm.ws.ejbcontainer.mdb.jms.web.");

        EnterpriseArchive MDB20TestApp = ShrinkWrap.create(EnterpriseArchive.class, "MDB20TestApp.ear");
        MDB20TestApp.addAsModule(MDB20TestEJB).addAsModule(MDB20TestWeb);
        MDB20TestApp = (EnterpriseArchive) ShrinkHelper.addDirectory(MDB20TestApp, "test-applications/MDB20TestApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, MDB20TestApp, DeployOptions.SERVER_ONLY);

        // Finally, start server
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CNTR0047E", "WTRN0017W");
    }

    private final void runTest() throws Exception {
        FATServletClient.runTest(server, "MDB20TestWeb/MDB20Servlet", getTestMethodSimpleName());
    }

    private void runTest(String testStep) throws Exception {
        FATServletClient.runTest(server, "MDB20TestWeb/MDB20Servlet", testStep);
    }

    @Test
    @ExpectedFFDC({ "javax.transaction.NotSupportedException" })
    public void testBMTIA() throws Exception {
        try {
            runTest();
        } catch (Throwable t) {
            Log.info(this.getClass(), "testBMTIA", "dumping server for throwable=" + t);
            server.dumpServer("testBMTIA_dump");
        }
    }

    @Test
    @ExpectedFFDC({ "com.ibm.websphere.csi.CSITransactionRolledbackException", "javax.ejb.TransactionRolledbackLocalException" })
    public void testBMTNoCommit() throws Exception {
        runTest();
    }

    @Test
    public void testCMTIA() throws Exception {
        runTest();
    }

    @Test
    public void testNonDurableTopic() throws Exception {
        runTest();
    }

    @Test
    public void testDurableTopic() throws Exception {
        runTest("testDurableTopic1");
        try {
            server.stopServer("CNTR0047E", "WTRN0017W");
        } finally {
            server.startServer();
            assertNotNull(server.waitForStringInLog("CWWKF0011I"));
            runTest("testDurableTopic2");
        }
    }

    @Test
    public void testMessageSelector() throws Exception {
        runTest();
    }

    @Test
    public void testCMTNotSupported() throws Exception {
        runTest();
    }

    @Test
    public void testCMTRequired() throws Exception {
        runTest();
    }

    @Test
    @ExpectedFFDC({ "com.ibm.websphere.csi.CSITransactionRolledbackException" })
    public void testCMTRequiredRollback() throws Exception {
        // The FFDC for this test occurs every time, but sometimes isn't detected until after the test completes
        server.setMarkToEndOfLog();
        runTest();
        assertNotNull(server.waitForStringInLogUsingMark("com.ibm.websphere.csi.CSITransactionRolledbackException"));
        Thread.sleep(500);
    }
}