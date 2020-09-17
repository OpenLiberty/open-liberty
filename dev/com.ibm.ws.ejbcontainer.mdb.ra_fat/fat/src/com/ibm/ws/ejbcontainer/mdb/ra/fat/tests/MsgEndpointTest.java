/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.mdb.ra.fat.tests;

import static org.junit.Assert.assertFalse;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 *
 */
@RunWith(FATRunner.class)
public class MsgEndpointTest extends FATServletClient {

    @Server("ejbcontainer.mdb.ra.fat.MsgEndpointServer")
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("ejbcontainer.mdb.ra.fat.MsgEndpointServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("ejbcontainer.mdb.ra.fat.MsgEndpointServer"));

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the Ears & Wars

        //#################### MsgEndpointApp.ear
        JavaArchive MsgEndpointEJB = ShrinkHelper.buildJavaArchive("MsgEndpointEJB.jar", "com.ibm.ws.ejbcontainer.fat.msgendpoint.ejb.");
        MsgEndpointEJB = (JavaArchive) ShrinkHelper.addDirectory(MsgEndpointEJB, "test-applications/MsgEndpointEJB.jar/resources");
        WebArchive MsgEndpointWeb = ShrinkHelper.buildDefaultApp("MsgEndpointWeb.war", "com.ibm.ws.ejbcontainer.fat.msgendpoint.web.");

        EnterpriseArchive MsgEndpointApp = ShrinkWrap.create(EnterpriseArchive.class, "MsgEndpointApp.ear");
        MsgEndpointApp.addAsModule(MsgEndpointEJB).addAsModule(MsgEndpointWeb);
        MsgEndpointApp = (EnterpriseArchive) ShrinkHelper.addDirectory(MsgEndpointApp, "test-applications/MsgEndpointApp.ear/resources");

        ShrinkHelper.exportAppToServer(server, MsgEndpointApp, DeployOptions.SERVER_ONLY);
        server.addInstalledAppForValidation("MsgEndpointApp");

        //#################### AdapterForEJB.jar  (RAR Implementation)
        JavaArchive AdapterForEJBJar = ShrinkHelper.buildJavaArchive("AdapterForEJB.jar", "com.ibm.ws.ejbcontainer.fat.rar.*");
        ShrinkHelper.exportToServer(server, "ralib", AdapterForEJBJar, DeployOptions.SERVER_ONLY);

        //#################### AdapterForEJB.rar
        ResourceAdapterArchive AdapterForEJBRar = ShrinkWrap.create(ResourceAdapterArchive.class, "AdapterForEJB.rar");
        ShrinkHelper.addDirectory(AdapterForEJBRar, "test-resourceadapters/AdapterForEJB.rar/resources");
        ShrinkHelper.exportToServer(server, "connectors", AdapterForEJBRar, DeployOptions.SERVER_ONLY);

        // Finally, start server
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CNTR0020E", "CNTR0047E", "CNTR0067W", "J2CA8501E", "WLTC0017E", "CNTR4015W",
                          "CWWKE0701E.*resourceadapter\\-class=com\\.ibm\\.ws\\.ejbcontainer\\.fat\\.rar\\.core\\.FVTAdapterImpl");
    }

    private final void runTest(String servlet) throws Exception {
        FATServletClient.runTest(server, servlet, getTestMethodSimpleName());
    }

    List<String> tranMsgs = null;
    List<String> tranMsgs2 = null;

    @Before
    public void clearTranMsgs() throws Exception {
        tranMsgs = null;
        tranMsgs2 = null;
    }

    @Test
    public void testXAOptionARequired() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/NonJMS_MDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTNonJMS is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testNonXAOptionARequired() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/NonJMS_MDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTNonJMS is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testXAOptionBRequired() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/NonJMS_MDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTNonJMS is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testNonXAOptionBRequired() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/NonJMS_MDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTNonJMS is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testXAOptionANotSupported() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/NonJMS_MDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTNonJMS is in a local transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testNonXAOptionANotSupported() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/NonJMS_MDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTNonJMS is in a local transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testXAOptionBNotSupported() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/NonJMS_MDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTNonJMS is in a local transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testNonXAOptionBNotSupported() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/NonJMS_MDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTNonJMS is in a local transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testXAOptionABMT() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/NonJMS_MDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointBMTNonJMS is in a local transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testNonXAOptionABMT() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/NonJMS_MDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointBMTNonJMS is in a local transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testXAOptionBBMT() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/NonJMS_MDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointBMTNonJMS is in a local transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testNonXAOptionBBMT() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/NonJMS_MDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointBMTNonJMS is in a local transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testXAOptionARequiredInherited() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/NonJMS_MDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTNonJMS is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testXAOptionBRequiredInherited() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/NonJMS_MDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTNonJMS is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testNonJMSOptionANonImportedTx() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/NonJMS_MMServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTNonJMS is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
        tranMsgs2 = server.findStringsInLogsAndTraceUsingMark("EndpointBMTNonJMS is in a local transaction");
        assertFalse(tranMsgs2.isEmpty());
    }

    @Test
    public void testNonJMSOptionAImportedTx() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/NonJMS_MMServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTNonJMS is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
        tranMsgs2 = server.findStringsInLogsAndTraceUsingMark("EndpointBMTNonJMS is in a local transaction");
        assertFalse(tranMsgs2.isEmpty());
    }

    @Test
    public void testNonJMSOptionBNonImportedTx() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/NonJMS_MMServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTNonJMS is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
        tranMsgs2 = server.findStringsInLogsAndTraceUsingMark("EndpointBMTNonJMS is in a local transaction");
        assertFalse(tranMsgs2.isEmpty());
    }

    @Test
    public void testNonJMSOptionBImportedTx() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/NonJMS_MMServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTNonJMS is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
        tranMsgs2 = server.findStringsInLogsAndTraceUsingMark("EndpointBMTNonJMS is in a local transaction");
        assertFalse(tranMsgs2.isEmpty());
    }

    @Test
    public void testRequiredXAOptionA() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/JMS_MDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTJMS is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testRequiredNonXAOptionA() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/JMS_MDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTJMS is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testRequiredXAOptionB() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/JMS_MDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTJMS is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testRequiredNonXAOptionB() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/JMS_MDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTJMS is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testNotSupportedXAOptionA() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/JMS_MDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTJMS is in a local transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testNotSupportedNonXAOptionA() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/JMS_MDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTJMS is in a local transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testNotSupportedXAOptionB() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/JMS_MDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTJMS is in a local transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testNotSupportedNonXAOptionB() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/JMS_MDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTJMS is in a local transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testBMTXAOptionA() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/JMS_MDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointBMTJMS is in a local transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testBMTNonXAOptionA() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/JMS_MDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointBMTJMS is in a local transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testBMTXAOptionB() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/JMS_MDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointBMTJMS is in a local transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testBMTNonXAOptionB() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/JMS_MDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointBMTJMS is in a local transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testOptionA() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/JMS_MMServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTJMS is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
        tranMsgs2 = server.findStringsInLogsAndTraceUsingMark("EndpointBMTJMS is in a local transaction");
        assertFalse(tranMsgs2.isEmpty());
    }

    @Test
    public void testOptionAImportedTx() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/JMS_MMServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTJMS is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
        tranMsgs2 = server.findStringsInLogsAndTraceUsingMark("EndpointBMTJMS is in a local transaction");
        assertFalse(tranMsgs2.isEmpty());
    }

    @Test
    public void testOptionB() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/JMS_MMServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTJMS is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
        tranMsgs2 = server.findStringsInLogsAndTraceUsingMark("EndpointBMTJMS is in a local transaction");
        assertFalse(tranMsgs2.isEmpty());
    }

    @Test
    public void testOptionBImportedTx() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/JMS_MMServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTJMS is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
        tranMsgs2 = server.findStringsInLogsAndTraceUsingMark("EndpointBMTJMS is in a local transaction");
        assertFalse(tranMsgs2.isEmpty());
    }

    @Test
    public void testImportedTxOptionBNonXA_BMT_NotSupported() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/MsgEndpoint_CDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("ConcurBMTNonJMS is in a local transaction");
        assertFalse(tranMsgs.isEmpty());
        tranMsgs2 = server.findStringsInLogsAndTraceUsingMark("ConcurCMTNonJMS is in a local transaction");
        assertFalse(tranMsgs2.isEmpty());
    }

    @Test
    public void testNonImportedTxOptionAXA_NotSupported_Required() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/MsgEndpoint_CDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("ConcurCMTNonJMS is in a local transaction");
        assertFalse(tranMsgs.isEmpty());
        tranMsgs2 = server.findStringsInLogsAndTraceUsingMark("ConcurCMTNonJMS is in a global transaction");
        assertFalse(tranMsgs2.isEmpty());
    }

    @Test
    public void testImportedTxOptionBNonXATwoMessages_Required_BMT() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/MsgEndpoint_CDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("ConcurCMTNonJMS is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
        tranMsgs2 = server.findStringsInLogsAndTraceUsingMark("ConcurBMTNonJMS is in a local transaction");
        assertFalse(tranMsgs2.isEmpty());
    }

    @ExpectedFFDC({
                    "java.lang.RuntimeException",
                    "com.ibm.ws.LocalTransaction.RolledbackException"
    })
    @Test
    public void testNonImportedTxOptionBXA_BMT_NotSupported() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/MsgEndpoint_CDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("ConcurBMTNonJMS is in a local transaction");
        assertFalse(tranMsgs.isEmpty());
        tranMsgs2 = server.findStringsInLogsAndTraceUsingMark("ConcurCMTException is in a local transaction");
        assertFalse(tranMsgs2.isEmpty());
    }

    @ExpectedFFDC({
                    "java.lang.RuntimeException",
                    "com.ibm.websphere.csi.CSITransactionRolledbackException"
    })
    @Test
    public void testImportedTxOptionANonXAException_NotSupported_Required() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/MsgEndpoint_CDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("ConcurCMTNonJMS is in a local transaction");
        assertFalse(tranMsgs.isEmpty());
        tranMsgs2 = server.findStringsInLogsAndTraceUsingMark("ConcurCMTException is in a global transaction");
        assertFalse(tranMsgs2.isEmpty());
    }

    @ExpectedFFDC({
                    "java.lang.RuntimeException",
                    "com.ibm.ws.LocalTransaction.RolledbackException"
    })
    @Test
    public void testNonImportedTxOptionBXAException_Required_BMT() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/MsgEndpoint_CDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("ConcurCMTNonJMS is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
        tranMsgs2 = server.findStringsInLogsAndTraceUsingMark("ConcurBMTException is in a local transaction");
        assertFalse(tranMsgs2.isEmpty());
    }

    @Test
    public void testNonImportedTxOptionAXA_BMT_NotSupported() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/MsgEndpoint_CDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointBMTNonJMS is in a local transaction");
        assertFalse(tranMsgs.isEmpty());
        tranMsgs2 = server.findStringsInLogsAndTraceUsingMark("EndpointCMTNonJMS is in a local transaction");
        assertFalse(tranMsgs2.isEmpty());
    }

    @Test
    public void testImportedTxOptionANonXA_NotSupported_Required() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/MsgEndpoint_CDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTNonJMS is in a local transaction");
        assertFalse(tranMsgs.isEmpty());
        tranMsgs2 = server.findStringsInLogsAndTraceUsingMark("EndpointCMTNonJMS is in a global transaction");
        assertFalse(tranMsgs2.isEmpty());
    }

    @Test
    public void testImportedTxOptionBNonXA_Required_BMT() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/MsgEndpoint_CDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTNonJMS is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
        tranMsgs2 = server.findStringsInLogsAndTraceUsingMark("EndpointBMTNonJMS is in a local transaction");
        assertFalse(tranMsgs2.isEmpty());
    }

    @ExpectedFFDC({
                    "java.lang.RuntimeException",
                    "com.ibm.ws.LocalTransaction.RolledbackException"
    })
    @Test
    public void testImportedTxOptionAXA_BMT_NotSupported() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/MsgEndpoint_CDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointBMTNonJMS is in a local transaction");
        assertFalse(tranMsgs.isEmpty());
        tranMsgs2 = server.findStringsInLogsAndTraceUsingMark("EndpointCMTException is in a local transaction");
        assertFalse(tranMsgs2.isEmpty());
    }

    @ExpectedFFDC({
                    "javax.ejb.TransactionRolledbackLocalException",
                    "com.ibm.websphere.csi.CSITransactionRolledbackException",
                    "java.lang.RuntimeException"
    })
    @Test
    public void testImportedTxOptionBNonXA_NotSupported_Required() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/MsgEndpoint_CDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTNonJMS is in a local transaction");
        assertFalse(tranMsgs.isEmpty());
        tranMsgs2 = server.findStringsInLogsAndTraceUsingMark("EndpointCMTException is in a global transaction");
        assertFalse(tranMsgs2.isEmpty());
    }

    @ExpectedFFDC({
                    "java.lang.RuntimeException",
                    "com.ibm.ws.LocalTransaction.RolledbackException"
    })
    @Test
    public void testNonImportedTxOptionBXA_Required_BMT() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/MsgEndpoint_CDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTNonJMS is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
        tranMsgs2 = server.findStringsInLogsAndTraceUsingMark("EndpointBMTException is in a local transaction");
        assertFalse(tranMsgs2.isEmpty());
    }

    @Test
    public void testNonImportedTxOptionANonXA_BMT() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/MsgEndpoint_CDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("ConcurBMTNonJMS is in a local transaction");
        assertFalse(tranMsgs.isEmpty());
        tranMsgs2 = server.findStringsInLogsAndTraceUsingMark("ConcurBMTNonJMS is in a local transaction");
        assertFalse(tranMsgs2.isEmpty());
    }

    @Test
    public void testImportedTxOptionBNonXA_NotSupported() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/MsgEndpoint_CDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("ConcurCMTNonJMS is in a local transaction");
        assertFalse(tranMsgs.isEmpty());
        tranMsgs2 = server.findStringsInLogsAndTraceUsingMark("ConcurCMTNonJMS is in a local transaction");
        assertFalse(tranMsgs2.isEmpty());
    }

    @Test
    public void testNonImportedTxOptionAXA_Required() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/MsgEndpoint_CDServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("ConcurCMTNonJMS is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
        tranMsgs2 = server.findStringsInLogsAndTraceUsingMark("ConcurCMTNonJMS is in a global transaction");
        assertFalse(tranMsgs2.isEmpty());
    }

    @Test
    public void testRequired() throws Exception {
        runTest("MsgEndpointWeb/MsgEndpoint_IFServlet");
    }

    @Test
    public void testNotSupported() throws Exception {
        runTest("MsgEndpointWeb/MsgEndpoint_IFServlet");
    }

    @Test
    public void testBMT() throws Exception {
        runTest("MsgEndpointWeb/MsgEndpoint_IFServlet");
    }

    @ExpectedFFDC("com.ibm.websphere.csi.CSITransactionRolledbackException")
    @Test
    public void testXAWrongBeanMethod() throws Exception {
        runTest("MsgEndpointWeb/MsgEndpoint_SMServlet");
    }

    @ExpectedFFDC({
                    "javax.ejb.TransactionRolledbackLocalException",
                    "com.ibm.websphere.csi.CSITransactionRolledbackException"
    })
    @Test
    public void testXAMissingAfterDelivery() throws Exception {
        runTest("MsgEndpointWeb/MsgEndpoint_SMServlet");
    }

    @Test
    public void testXAAfterDeliveryTwice() throws Exception {
        runTest("MsgEndpointWeb/MsgEndpoint_SMServlet");
    }

    @ExpectedFFDC({
                    "javax.ejb.TransactionRolledbackLocalException",
                    "com.ibm.websphere.csi.CSITransactionRolledbackException"
    })
    @Test
    public void testBeforeDeliveryAfterMethod() throws Exception {
        runTest("MsgEndpointWeb/MsgEndpoint_SMServlet");
    }

    @ExpectedFFDC({
                    "javax.ejb.TransactionRolledbackLocalException",
                    "com.ibm.websphere.csi.CSITransactionRolledbackException"
    })
    @Test
    public void testBeanMethodTwice() throws Exception {
        runTest("MsgEndpointWeb/MsgEndpoint_SMServlet");
    }

    @Test
    public void testReuseMessageEndpoint() throws Exception {
        runTest("MsgEndpointWeb/MsgEndpoint_SMServlet");
    }

    @Test
    public void testBeforeDeliveryTwiceWithAfterDelivery() throws Exception {
        runTest("MsgEndpointWeb/MsgEndpoint_SMServlet");
    }

    @Test
    public void testMissingBeforeDelivery() throws Exception {
        runTest("MsgEndpointWeb/MsgEndpoint_SMServlet");
    }

    @ExpectedFFDC({
                    "javax.ejb.TransactionRolledbackLocalException",
                    "com.ibm.websphere.csi.CSITransactionRolledbackException"
    })
    @Test
    public void testBeforeDeliveryTwice() throws Exception {
        runTest("MsgEndpointWeb/MsgEndpoint_SMServlet");
    }

    @Test
    public void testReleaseBeforeMethod() throws Exception {
        runTest("MsgEndpointWeb/MsgEndpoint_SMServlet");
    }

    @ExpectedFFDC({
                    "javax.ejb.TransactionRolledbackLocalException",
                    "com.ibm.websphere.csi.CSITransactionRolledbackException"
    })
    @Test
    public void testMissingBeanMethodAndAfterDelivery() throws Exception {
        runTest("MsgEndpointWeb/MsgEndpoint_SMServlet");
    }

    @Test
    public void testNotTimedObject() throws Exception {
        runTest("MsgEndpointWeb/TimerMDBOperationsServlet");
    }

    @Test
    public void testEJBCreate() throws Exception {
        runTest("MsgEndpointWeb/TimerMDBOperationsServlet");
    }

    @Test
    @Mode(Mode.TestMode.FULL)
    public void testOnMessage() throws Exception {
        runTest("MsgEndpointWeb/TimerMDBOperationsServlet");
    }

    @Test
    @Mode(Mode.TestMode.FULL)
    public void testEJBTimeout() throws Exception {
        runTest("MsgEndpointWeb/TimerMDBOperationsServlet");
    }

    @Test
    public void testCMTEJBTimeoutMessageDrivenContext() throws Exception {
        runTest("MsgEndpointWeb/TimerMDBOperationsServlet");
    }

    @Test
    public void testCMTConstructor() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/TimerMDBOperationsServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("MDBTimedCMTFailBean caught expected IllegalStateException");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testCMTOnMessage() throws Exception {
        runTest("MsgEndpointWeb/TimerMDBOperationsServlet");
    }

    @Test
    public void testSetMessageDrivenContext() throws Exception {
        runTest("MsgEndpointWeb/TimerMDBOperationsServlet");
    }

    @Test
    public void testBMTEJBTimeoutMessageDrivenContext() throws Exception {
        runTest("MsgEndpointWeb/TimerMDBOperationsServlet");
    }

    @Test
    public void testBMTOnMessage() throws Exception {
        runTest("MsgEndpointWeb/TimerMDBOperationsServlet");
    }

    @Test
    public void testBMTConstructor() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/TimerMDBOperationsServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("MDBTimedBMTFailBean caught expected IllegalStateException");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testXAOptionACommit() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/MsgEndpoint_TXServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTNonJMS is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testNonXAOptionACommit() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/MsgEndpoint_TXServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTNonJMS is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testXAOptionBCommit() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/MsgEndpoint_TXServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTNonJMS is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testNonXAOptionBCommit() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/MsgEndpoint_TXServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTNonJMS is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testXAOptionARollback() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/MsgEndpoint_TXServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTNonJMS is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testNonXAOptionARollback() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/MsgEndpoint_TXServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTNonJMS is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testXAOptionBRollback() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/MsgEndpoint_TXServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTNonJMS is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testNonXAOptionBRollback() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/MsgEndpoint_TXServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("EndpointCMTNonJMS is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testXAOptionAMDBRollback() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/MsgEndpoint_TXServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("CMTRollback is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testNonXAOptionAMDBRollback() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/MsgEndpoint_TXServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("CMTRollback is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testXAOptionBMDBRollback() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/MsgEndpoint_TXServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("CMTRollback is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    @Test
    public void testNonXAOptionBMDBRollback() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/MsgEndpoint_TXServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("CMTRollback is in a global transaction");
        assertFalse(tranMsgs.isEmpty());
    }

    // Test that when autoStart is set to true for an activation spec the endpoint
    // is activated on server start
    @Test
    public void testEndpointStartedWithAutoStartTrue() throws Exception {
        runTest("MsgEndpointWeb/MsgEndpoint_PCServlet");
    }

    // Test that when autoStart is set to false for an activation spec the endpoint
    // is not activated on server start
    @Test
    public void testEndpointNotStartWithAutoStartFalse() throws Exception {
        runTest("MsgEndpointWeb/MsgEndpoint_PCServlet");
        List<String> infoMessage = server.findStringsInLogsAndTrace("CNTR4116I.*EndpointBMTNonJMSNeverStarted");
        assertFalse("Did not find expected CNTR4116I message in logs", infoMessage.isEmpty());
    }

    // Test that when autoStart is set to false for a JMS activation spec the endpoint
    // is not activated on server start
    @Test
    public void testEndpointNotStartWithAutoStartFalseJMS() throws Exception {
        runTest("MsgEndpointWeb/MsgEndpoint_PCServlet");
        List<String> infoMessage = server.findStringsInLogsAndTrace("CNTR4116I.*EndpointBMTJMSAutoStartFalse");
        assertFalse("Did not find expected CNTR4116I message in logs", infoMessage.isEmpty());
    }

    // Test that when autoStart is set to false for an activation spec the endpoint
    // is activated after a resume command is issued, paused after a pause command is issued,
    // and then resumed again after another resume command is issued
    @Test
    public void testResumeAndPauseEndpointWithAutoStartFalse() throws Exception {
        runTest("MsgEndpointWeb/MsgEndpoint_PCServlet");
    }

    // Test utilizing two mdbs with the same activation spec with autoStart=false
    @Test
    public void testMultipleEndpointSameActSpec() throws Exception {
        runTest("MsgEndpointWeb/MsgEndpoint_PCServlet");
    }

    // Test to ensure that in the case of incorrect config where
    // an activation spec is not correctly bound to the MDB
    // the app is still allowed to start and a CNTR4015W message is logged
    @Test
    public void testMDBWithNoActSpecBinding() throws Exception {
        runTest("MsgEndpointWeb/MsgEndpoint_PCServlet");
        List<String> infoMessage = server.findStringsInLogsAndTrace("CNTR4015W.*EndpointBMTNonJMSNoActSpec");
        assertFalse("Did not find expected CNTR4015W message in logs", infoMessage.isEmpty());
    }

    // Test that we can get the list of pauseable endpoints from the mbean and that it
    // contains one of our message endpoints.
    @Test
    public void testEndpointGetListOfNames() throws Exception {
        runTest("MsgEndpointWeb/MsgEndpoint_PCServlet");
    }

    // Test to ensure message endpoints can be paused, at which point they will not accept messages,
    // and they can be resumed, at which point they will accept messages again.
    @Test
    public void testEndpointPauseAndResume() throws Exception {
        runTest("MsgEndpointWeb/MsgEndpoint_PCServlet");
    }

    // Test that the correct message is thrown when attempting to pause an inactive endpoint.
    // Also checks that the endpoint remains inactive.
    @Test
    public void testPauseInactive() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/MsgEndpoint_PCServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("CNTR4117I");
        assertFalse(tranMsgs.isEmpty());
    }

    // Test that the correct message is thrown when attempting to resume an active endpoint.
    // Also checks that the endpoint remains active.
    @Test
    public void testResumeActive() throws Exception {
        server.setMarkToEndOfLog();
        runTest("MsgEndpointWeb/MsgEndpoint_PCServlet");
        tranMsgs = server.findStringsInLogsAndTraceUsingMark("CNTR4118I");
        assertFalse(tranMsgs.isEmpty());
    }
}