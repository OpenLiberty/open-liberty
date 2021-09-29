/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.ejbcontainer.remote.fat.ann.sf.web.AdvBasicCMTStatefulAnnRemoteServlet;
import com.ibm.ws.ejbcontainer.remote.fat.ann.sf.web.AdvCompCMTStatefulAnnRemoteServlet;
import com.ibm.ws.ejbcontainer.remote.fat.ann.sf.web.BasicCMTStatefulAnnRemoteServlet;
import com.ibm.ws.ejbcontainer.remote.fat.ann.sf.web.CompCMTStatefulAnnRemoteServlet;
import com.ibm.ws.ejbcontainer.remote.fat.ann.sf.web.EmptyCMTStatefulAnnRemoteServlet;
import com.ibm.ws.ejbcontainer.remote.fat.ann.sf.web.InitCMTStatefulAnnRemoteServlet;
import com.ibm.ws.ejbcontainer.remote.fat.ann.sf.web.NoCMTStatefulAnnRemoteServlet;
import com.ibm.ws.ejbcontainer.remote.fat.ann.sf.web.RemoveBMTStatefulAnnRemoteServlet;
import com.ibm.ws.ejbcontainer.remote.fat.ann.sf.web.RemoveCMTStatefulAnnRemoteServlet;
import com.ibm.ws.ejbcontainer.remote.fat.mix.sf.web.CompCMTStatefulMixRemoteServlet;
import com.ibm.ws.ejbcontainer.remote.fat.mix.sf.web.InterfaceMixRemoteServlet;
import com.ibm.ws.ejbcontainer.remote.fat.mix.sf.web.StatefulPassivationMixServlet;
import com.ibm.ws.ejbcontainer.remote.fat.mix.sf.web.StatefulTwoNamesMixRemoteServlet;
import com.ibm.ws.ejbcontainer.remote.fat.xml.sf.web.AdvBasicCMTStatefulXMLRemoteServlet;
import com.ibm.ws.ejbcontainer.remote.fat.xml.sf.web.AdvCompCMTStatefulXMLRemoteServlet;
import com.ibm.ws.ejbcontainer.remote.fat.xml.sf.web.BasicCMTStatefulXMLRemoteServlet;
import com.ibm.ws.ejbcontainer.remote.fat.xml.sf.web.CompCMTStatefulXMLRemoteServlet;
import com.ibm.ws.ejbcontainer.remote.fat.xml.sf.web.InitCMTStatefulXMLRemoteServlet;
import com.ibm.ws.ejbcontainer.remote.fat.xml.sf.web.RemoveBMTStatefulXMLRemoteServlet;
import com.ibm.ws.ejbcontainer.remote.fat.xml.sf.web.RemoveCMTStatefulXMLRemoteServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class RemoteSessionTests extends AbstractTest {

    @Server("com.ibm.ws.ejbcontainer.remote.fat.RemoteSessionServer")
    @TestServlets({ @TestServlet(servlet = AdvBasicCMTStatefulAnnRemoteServlet.class, contextRoot = "StatefulAnnRemoteWeb"),
                    @TestServlet(servlet = AdvCompCMTStatefulAnnRemoteServlet.class, contextRoot = "StatefulAnnRemoteWeb"),
                    @TestServlet(servlet = BasicCMTStatefulAnnRemoteServlet.class, contextRoot = "StatefulAnnRemoteWeb"),
                    @TestServlet(servlet = CompCMTStatefulAnnRemoteServlet.class, contextRoot = "StatefulAnnRemoteWeb"),
                    @TestServlet(servlet = EmptyCMTStatefulAnnRemoteServlet.class, contextRoot = "StatefulAnnRemoteWeb"),
                    @TestServlet(servlet = InitCMTStatefulAnnRemoteServlet.class, contextRoot = "StatefulAnnRemoteWeb"),
                    @TestServlet(servlet = NoCMTStatefulAnnRemoteServlet.class, contextRoot = "StatefulAnnRemoteWeb"),
                    @TestServlet(servlet = RemoveBMTStatefulAnnRemoteServlet.class, contextRoot = "StatefulAnnRemoteWeb"),
                    @TestServlet(servlet = RemoveCMTStatefulAnnRemoteServlet.class, contextRoot = "StatefulAnnRemoteWeb"),
                    @TestServlet(servlet = CompCMTStatefulMixRemoteServlet.class, contextRoot = "StatefulMixRemoteWeb"),
                    @TestServlet(servlet = InterfaceMixRemoteServlet.class, contextRoot = "StatefulMixRemoteWeb"),
                    @TestServlet(servlet = StatefulPassivationMixServlet.class, contextRoot = "StatefulMixRemoteWeb"),
                    @TestServlet(servlet = StatefulTwoNamesMixRemoteServlet.class, contextRoot = "StatefulMixRemoteWeb"),
                    @TestServlet(servlet = AdvBasicCMTStatefulXMLRemoteServlet.class, contextRoot = "StatefulXMLRemoteWeb"),
                    @TestServlet(servlet = AdvCompCMTStatefulXMLRemoteServlet.class, contextRoot = "StatefulXMLRemoteWeb"),
                    @TestServlet(servlet = BasicCMTStatefulXMLRemoteServlet.class, contextRoot = "StatefulXMLRemoteWeb"),
                    @TestServlet(servlet = CompCMTStatefulXMLRemoteServlet.class, contextRoot = "StatefulXMLRemoteWeb"),
                    @TestServlet(servlet = InitCMTStatefulXMLRemoteServlet.class, contextRoot = "StatefulXMLRemoteWeb"),
                    @TestServlet(servlet = RemoveBMTStatefulXMLRemoteServlet.class, contextRoot = "StatefulXMLRemoteWeb"),
                    @TestServlet(servlet = RemoveCMTStatefulXMLRemoteServlet.class, contextRoot = "StatefulXMLRemoteWeb") })
    public static LibertyServer server;

    @Override
    public LibertyServer getServer() {
        return server;
    }

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.remote.fat.RemoteSessionServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.remote.fat.RemoteSessionServer")).andWith(new JakartaEE9Action().forServers("com.ibm.ws.ejbcontainer.remote.fat.RemoteSessionServer"));

    @BeforeClass
    public static void beforeClass() throws Exception {
        // Use ShrinkHelper to build the Ears & Wars

        //#################### InitTxRecoveryLogApp.ear (Automatically initializes transaction recovery logs)
        JavaArchive InitTxRecoveryLogEJBJar = ShrinkHelper.buildJavaArchive("InitTxRecoveryLogEJB.jar", "com.ibm.ws.ejbcontainer.init.recovery.ejb.");

        EnterpriseArchive InitTxRecoveryLogApp = ShrinkWrap.create(EnterpriseArchive.class, "InitTxRecoveryLogApp.ear");
        InitTxRecoveryLogApp.addAsModule(InitTxRecoveryLogEJBJar);

        ShrinkHelper.exportDropinAppToServer(server, InitTxRecoveryLogApp, DeployOptions.SERVER_ONLY);

        //#################### StatefulAnnRemoteTest.ear
        JavaArchive StatefulAnnRemoteEJB = ShrinkHelper.buildJavaArchive("StatefulAnnRemoteEJB.jar", "com.ibm.ws.ejbcontainer.remote.fat.ann.sf.ejb.");
        WebArchive StatefulAnnRemoteWeb = ShrinkHelper.buildDefaultApp("StatefulAnnRemoteWeb.war", "com.ibm.ws.ejbcontainer.remote.fat.ann.sf.web.");
        StatefulAnnRemoteWeb = (WebArchive) ShrinkHelper.addDirectory(StatefulAnnRemoteWeb, "test-applications/StatefulAnnRemoteWeb.war/resources");

        EnterpriseArchive StatefulAnnRemoteTest = ShrinkWrap.create(EnterpriseArchive.class, "StatefulAnnRemoteTest.ear");
        StatefulAnnRemoteTest.addAsModule(StatefulAnnRemoteEJB).addAsModule(StatefulAnnRemoteWeb);
        StatefulAnnRemoteTest = (EnterpriseArchive) ShrinkHelper.addDirectory(StatefulAnnRemoteTest, "test-applications/StatefulAnnRemoteTest.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, StatefulAnnRemoteTest, DeployOptions.SERVER_ONLY);

        //#################### StatefulMixRemoteTest.ear
        JavaArchive StatefulMixRemoteEJB = ShrinkHelper.buildJavaArchive("StatefulMixRemoteEJB.jar", "com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb.");
        StatefulMixRemoteEJB = (JavaArchive) ShrinkHelper.addDirectory(StatefulMixRemoteEJB, "test-applications/StatefulMixRemoteEJB.jar/resources");
        WebArchive StatefulMixRemoteWeb = ShrinkHelper.buildDefaultApp("StatefulMixRemoteWeb.war", "com.ibm.ws.ejbcontainer.remote.fat.mix.sf.web.");
        StatefulMixRemoteWeb = (WebArchive) ShrinkHelper.addDirectory(StatefulMixRemoteWeb, "test-applications/StatefulMixRemoteWeb.war/resources");

        EnterpriseArchive StatefulMixRemoteTest = ShrinkWrap.create(EnterpriseArchive.class, "StatefulMixRemoteTest.ear");
        StatefulMixRemoteTest.addAsModule(StatefulMixRemoteEJB).addAsModule(StatefulMixRemoteWeb);
        StatefulMixRemoteTest = (EnterpriseArchive) ShrinkHelper.addDirectory(StatefulMixRemoteTest, "test-applications/StatefulMixRemoteTest.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, StatefulMixRemoteTest, DeployOptions.SERVER_ONLY);

        //#################### StatefulXMLRemoteTest.ear
        JavaArchive StatefulXMLRemoteEJB = ShrinkHelper.buildJavaArchive("StatefulXMLRemoteEJB.jar", "com.ibm.ws.ejbcontainer.remote.fat.xml.sf.ejb.");
        StatefulXMLRemoteEJB = (JavaArchive) ShrinkHelper.addDirectory(StatefulXMLRemoteEJB, "test-applications/StatefulXMLRemoteEJB.jar/resources");
        WebArchive StatefulXMLRemoteWeb = ShrinkHelper.buildDefaultApp("StatefulXMLRemoteWeb.war", "com.ibm.ws.ejbcontainer.remote.fat.xml.sf.web.");
        StatefulXMLRemoteWeb = (WebArchive) ShrinkHelper.addDirectory(StatefulXMLRemoteWeb, "test-applications/StatefulXMLRemoteWeb.war/resources");

        EnterpriseArchive StatefulXMLRemoteTest = ShrinkWrap.create(EnterpriseArchive.class, "StatefulXMLRemoteTest.ear");
        StatefulXMLRemoteTest.addAsModule(StatefulXMLRemoteEJB).addAsModule(StatefulXMLRemoteWeb);
        StatefulXMLRemoteTest = (EnterpriseArchive) ShrinkHelper.addDirectory(StatefulXMLRemoteTest, "test-applications/StatefulXMLRemoteTest.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, StatefulXMLRemoteTest, DeployOptions.SERVER_ONLY);

        // Finally, start server
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        // CNTR0020E - testSFSBBizIntVerifyRemoveBMTFailsAnn, testSFSBBizIntVerifyBMTRetainIfExceptionAnn, + many
        // WLTC0017E - testSFSBBizIntVerifyBMTRetainIfExceptionAnn, testSFSBBizIntVerifyBMTRetainIfExceptionXML
        server.stopServer("CNTR0020E", "WLTC0017E");
    }

}