/*******************************************************************************
 * Copyright (c) 2019, 2022 IBM Corporation and others.
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
package suite.r80.base.jca16.tranlvl;

import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jca.fat.regr.util.JCAFATTest;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerFactory;
import suite.r80.base.jca16.TestSetupUtils;

/**
 *
 */
@RunWith(FATRunner.class)
public class TranLvlTest extends JCAFATTest {

    private final static Class<?> c = TranLvlTest.class;
    private final String servletName = "TranLvlTestServlet";
    private final static String NoTransaction = "transactionSupport              : NoTransaction";
    private final static String LocalTransaction = "transactionSupport              : LocalTransaction";
    private final static String XATransaction = "transactionSupport              : XATransaction";

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jca.fat.regr", null, false);
        server.setServerConfigurationFile("TranLvl_server.xml"); // set config

        TestSetupUtils.setUpFvtApp(server);

//      Package adapter_jca16_tranlvl_Loc_Loc adapter_jca16_tranlvl_Loc_Loc.rar
        JavaArchive resourceAdapter_jar = ShrinkWrap.create(JavaArchive.class, "ResourceAdapter.jar");
        resourceAdapter_jar.addPackages(true, "com.ibm.adapter");
        resourceAdapter_jar.addPackages(true, "com.ibm.ejs.ras");

        ResourceAdapterArchive adapter_jca16_tranlvl_Loc_Loc = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                 "adapter_jca16_tranlvl_Loc_Loc.rar").addAsLibrary(resourceAdapter_jar).addAsManifestResource(new File(TestSetupUtils.raDir + "adapter_jca16_tranlvl_Loc_Loc-ra.xml"), "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", adapter_jca16_tranlvl_Loc_Loc);

//      Package adapter_jca16_tranlvl_Loc_No adapter_jca16_tranlvl_Loc_No.rar
        ResourceAdapterArchive adapter_jca16_tranlvl_Loc_No = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                "adapter_jca16_tranlvl_Loc_No.rar").addAsLibrary(resourceAdapter_jar).addAsManifestResource(new File(TestSetupUtils.raDir + "adapter_jca16_tranlvl_Loc_No-ra.xml"), "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", adapter_jca16_tranlvl_Loc_No);

//      Package adapter_jca16_tranlvl_Loc_XA adapter_jca16_tranlvl_Loc_XA.rar
        ResourceAdapterArchive adapter_jca16_tranlvl_Loc_XA = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                "adapter_jca16_tranlvl_Loc_XA.rar").addAsLibrary(resourceAdapter_jar).addAsManifestResource(new File(TestSetupUtils.raDir + "adapter_jca16_tranlvl_Loc_XA-ra.xml"), "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", adapter_jca16_tranlvl_Loc_XA);

//      Package adapter_jca16_tranlvl_No_Loc adapter_jca16_tranlvl_No_Loc.rar
        ResourceAdapterArchive adapter_jca16_tranlvl_No_Loc = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                "adapter_jca16_tranlvl_No_Loc.rar").addAsLibrary(resourceAdapter_jar).addAsManifestResource(new File(TestSetupUtils.raDir + "adapter_jca16_tranlvl_No_Loc-ra.xml"), "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", adapter_jca16_tranlvl_No_Loc);

//      Package adapter_jca16_tranlvl_No_No adapter_jca16_tranlvl_No_No.rar
        ResourceAdapterArchive adapter_jca16_tranlvl_No_No = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                               "adapter_jca16_tranlvl_No_No.rar").addAsLibrary(resourceAdapter_jar).addAsManifestResource(new File(TestSetupUtils.raDir + "adapter_jca16_tranlvl_No_No-ra.xml"), "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", adapter_jca16_tranlvl_No_No);

//      Package adapter_jca16_tranlvl_No_XA adapter_jca16_tranlvl_No_XA.rar
        ResourceAdapterArchive adapter_jca16_tranlvl_No_XA = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                               "adapter_jca16_tranlvl_No_XA.rar").addAsLibrary(resourceAdapter_jar).addAsManifestResource(new File(TestSetupUtils.raDir + "adapter_jca16_tranlvl_No_XA-ra.xml"), "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", adapter_jca16_tranlvl_No_XA);

//      Package adapter_jca16_tranlvl_TranSupportNotImplemented adapter_jca16_tranlvl_TranSupportNotImplemented.rar
        ResourceAdapterArchive adapter_jca16_tranlvl_TranSupportNotImplemented = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                   "adapter_jca16_tranlvl_TranSupportNotImplemented.rar").addAsLibrary(resourceAdapter_jar).addAsManifestResource(new File(TestSetupUtils.raDir + "adapter_jca16_tranlvl_TranSupportNotImplemented-ra.xml"), "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", adapter_jca16_tranlvl_TranSupportNotImplemented);

//      Package adapter_jca16_tranlvl_XA_Loc adapter_jca16_tranlvl_XA_Loc.rar
        ResourceAdapterArchive adapter_jca16_tranlvl_XA_Loc = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                "adapter_jca16_tranlvl_XA_Loc.rar").addAsLibrary(resourceAdapter_jar).addAsManifestResource(new File(TestSetupUtils.raDir + "adapter_jca16_tranlvl_XA_Loc-ra.xml"), "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", adapter_jca16_tranlvl_XA_Loc);

//      Package adapter_jca16_tranlvl_XA_No adapter_jca16_tranlvl_XA_No.rar
        ResourceAdapterArchive adapter_jca16_tranlvl_XA_No = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                               "adapter_jca16_tranlvl_XA_No.rar").addAsLibrary(resourceAdapter_jar).addAsManifestResource(new File(TestSetupUtils.raDir + "adapter_jca16_tranlvl_XA_No-ra.xml"), "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", adapter_jca16_tranlvl_XA_No);

//      Package adapter_jca16_tranlvl_XA_XA adapter_jca16_tranlvl_XA_XA.rar
        ResourceAdapterArchive adapter_jca16_tranlvl_XA_XA = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                               "adapter_jca16_tranlvl_XA_XA.rar").addAsLibrary(resourceAdapter_jar).addAsManifestResource(new File(TestSetupUtils.raDir + "adapter_jca16_tranlvl_XA_XA-ra.xml"), "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", adapter_jca16_tranlvl_XA_XA);

        server.startServer("TranLvlTest.log");
        server.waitForMultipleStringsInLog(10, "J2CA7001I");
        server.waitForStringInLog("CWWKZ0001I:.*fvtapp"); // Wait for application start.
        server.waitForStringInLog("CWWKF0011I");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted()) {
            server.stopServer("CNTR4015W: .*SampleMdb", // EXPECTED
                              "CWWKE0701E"); // tolerated due to 'com.ibm.ws.jca.internal.ConnectorModuleMetatypeBundleImpl.removeMetatype()' tolerating shutdown errors
        }
    }

    private void executeTest(String result) throws Exception {
        server.setMarkToEndOfLog(server.getMatchingLogFile("trace.log"));
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
        assertNotNull("Trace should contain String " + result, server.findStringsInLogsAndTraceUsingMark(result));
    }

    @Mode(TestMode.LITE)
    @Test
    public void testTranLocNo() throws Exception {
        executeTest(NoTransaction);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testTranLocLoc() throws Exception {
        executeTest(LocalTransaction);
    }

    @Mode(TestMode.FULL)
    @Test
    @ExpectedFFDC("java.lang.IllegalArgumentException")
    public void testTranLocXA() throws Exception {
        //error
        executeTest("java.lang.IllegalArgumentException: javax.resource.spi.ManagedConnectionFactory:XATransaction, javax.resource.spi.Connector:LocalTransaction");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testTranNoNo() throws Exception {
        executeTest(NoTransaction);
    }

    @Mode(TestMode.FULL)
    @Test
    @ExpectedFFDC("java.lang.IllegalArgumentException")
    public void testTranNoLoc() throws Exception {
        //error
        executeTest("java.lang.IllegalArgumentException: javax.resource.spi.ManagedConnectionFactory:LocalTransaction, javax.resource.spi.Connector:NoTransaction");
    }

    @Mode(TestMode.FULL)
    @Test
    @ExpectedFFDC("java.lang.IllegalArgumentException")
    public void testTranNoXA() throws Exception {
        //error
        executeTest("java.lang.IllegalArgumentException: javax.resource.spi.ManagedConnectionFactory:XATransaction, javax.resource.spi.Connector:NoTransaction");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testTranXANo() throws Exception {
        executeTest(NoTransaction);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testTranXALoc() throws Exception {
        executeTest(LocalTransaction);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testTranXAXA() throws Exception {
        executeTest(XATransaction);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testTranSupportNotImplemented() throws Exception {
        executeTest(XATransaction);
    }
}
