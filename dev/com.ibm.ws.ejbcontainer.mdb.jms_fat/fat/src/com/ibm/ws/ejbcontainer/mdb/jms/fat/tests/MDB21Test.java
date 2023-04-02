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
public class MDB21Test extends FATServletClient {

    @Server("ejbcontainer.mdb.jms.fat.mdb21")
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("ejbcontainer.mdb.jms.fat.mdb21")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("ejbcontainer.mdb.jms.fat.mdb21")).andWith(new JakartaEE9Action().fullFATOnly().forServers("ejbcontainer.mdb.jms.fat.mdb21")).andWith(new JakartaEE10Action().fullFATOnly().forServers("ejbcontainer.mdb.jms.fat.mdb21"));

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the Ears & Wars

        //#################### MDB21TestApp.ear
        JavaArchive MDB21TestEJB = ShrinkHelper.buildJavaArchive("MDB21TestEJB.jar", "com.ibm.ws.ejbcontainer.mdb.jms.ejb21.");
        MDB21TestEJB = (JavaArchive) ShrinkHelper.addDirectory(MDB21TestEJB, "test-applications/MDB21TestEJB.jar/resources");
        WebArchive MDB21TestWeb = ShrinkHelper.buildDefaultApp("MDB21TestWeb.war", "com.ibm.ws.ejbcontainer.mdb.jms.web.");

        EnterpriseArchive MDB21TestApp = ShrinkWrap.create(EnterpriseArchive.class, "MDB21TestApp.ear");
        MDB21TestApp.addAsModule(MDB21TestEJB).addAsModule(MDB21TestWeb);
        MDB21TestApp = (EnterpriseArchive) ShrinkHelper.addDirectory(MDB21TestApp, "test-applications/MDB21TestApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, MDB21TestApp, DeployOptions.SERVER_ONLY);

        // Finally, start server
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CNTR0047E", "WTRN0017W");
    }

    private final void runTest() throws Exception {
        FATServletClient.runTest(server, "MDB21TestWeb/MDB21Servlet", getTestMethodSimpleName());
    }

    private void runTest(String testStep) throws Exception {
        FATServletClient.runTest(server, "MDB21TestWeb/MDB21Servlet", testStep);
    }

    @Test
    @ExpectedFFDC({ "javax.transaction.NotSupportedException" })
    public void testBMTIA() throws Exception {
        runTest();
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