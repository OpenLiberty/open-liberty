/*******************************************************************************
 * Copyright (c) 2015, 2025 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.remote.fat.tests;

import java.util.Collections;

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
import com.ibm.websphere.simplicity.config.EJBAsynchronousElement;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.web.AdvBasicCMTStatelessRemoteServlet;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.web.AdvCompCMTStatelessLocalServlet;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.web.AdvCompCMTStatelessRemoteServlet;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.web.AdvCompViewCMTStatelessLocalServlet;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.web.AdvCompViewCMTStatelessRemoteServlet;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.web.BMTVerificationServlet;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.web.BasicCMTStatelessRemoteServlet;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.web.CompCMTStatelessLocalServlet;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.web.CompCMTStatelessRemoteServlet;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.web.EmptyCMTStatelessRemoteServlet;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.web.RemoteBusinessInterfaceServlet;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.web.TxAttrComp2Servlet;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.web.TxAttrComp3Servlet;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.web.TxAttrCompView2Servlet;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.web.TxAttrCompView3Servlet;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.web.TxAttrMixedClassMethodOverrideServlet;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.web.TxAttrOverrideServlet;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.web.TxAttrServlet;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.web.AnnotationOverByXMLTxAttrServlet;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.web.CMTVerificationServlet;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.web.ExternalBeanClassWithAnnServlet;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.web.ExternalBeanClassWithNoAnnServlet;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.web.MixBMTVerificationServlet;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.web.MixCompCMTStatelessLocalServlet;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.web.MixCompCMTStatelessRemoteServlet;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.web.RemoteStatelessTwoNamesServlet;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.web.StatelessTwoNamesServlet;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.web.TxAttrMixedAnnotationXMLServlet;
import com.ibm.ws.ejbcontainer.remote.fat.basic.BasicRemoteTestServlet;
import com.ibm.ws.ejbcontainer.remote.fat.crossapp.client.CrossAppTestServlet;
import com.ibm.ws.ejbcontainer.remote.fat.home.EJBHomeTestServlet;
import com.ibm.ws.ejbcontainer.remote.fat.home2x.web.EJBHome2xTestServlet;
import com.ibm.ws.ejbcontainer.remote.fat.tx.RemoteTxTestServlet;
import com.ibm.ws.ejbcontainer.remote.misc.jitdeploy.web.ExceptionServlet;
import com.ibm.ws.ejbcontainer.remote.misc.jitdeploy.web.IDLEntityStubServlet;
import com.ibm.ws.ejbcontainer.remote.singleton.ann.web.InitializeFailureServlet;
import com.ibm.ws.ejbcontainer.remote.singleton.mix.web.PassivationServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class RemoteTests extends AbstractTest {

    @Server("com.ibm.ws.ejbcontainer.remote.fat.RemoteServer")
    @TestServlets({ @TestServlet(servlet = BasicRemoteTestServlet.class, contextRoot = "BasicRemote"),
                    @TestServlet(servlet = CrossAppTestServlet.class, contextRoot = "CrossAppRemoteClient"),
                    @TestServlet(servlet = EJBHome2xTestServlet.class, contextRoot = "EJBHome2xTestWeb"),
                    @TestServlet(servlet = EJBHomeTestServlet.class, contextRoot = "EJBHomeTest"),
                    @TestServlet(servlet = ExceptionServlet.class, contextRoot = "JitDeployWeb"),
                    @TestServlet(servlet = IDLEntityStubServlet.class, contextRoot = "JitDeployWeb"),
                    @TestServlet(servlet = RemoteTxTestServlet.class, contextRoot = "RemoteTx"),
                    @TestServlet(servlet = InitializeFailureServlet.class, contextRoot = "SingletonWeb"),
                    @TestServlet(servlet = PassivationServlet.class, contextRoot = "SingletonWeb"),
                    @TestServlet(servlet = AdvBasicCMTStatelessRemoteServlet.class, contextRoot = "StatelessAnnWeb"),
                    @TestServlet(servlet = AdvCompCMTStatelessLocalServlet.class, contextRoot = "StatelessAnnWeb"),
                    @TestServlet(servlet = AdvCompCMTStatelessRemoteServlet.class, contextRoot = "StatelessAnnWeb"),
                    @TestServlet(servlet = AdvCompViewCMTStatelessLocalServlet.class, contextRoot = "StatelessAnnWeb"),
                    @TestServlet(servlet = AdvCompViewCMTStatelessRemoteServlet.class, contextRoot = "StatelessAnnWeb"),
                    @TestServlet(servlet = BasicCMTStatelessRemoteServlet.class, contextRoot = "StatelessAnnWeb"),
                    @TestServlet(servlet = BMTVerificationServlet.class, contextRoot = "StatelessAnnWeb"),
                    @TestServlet(servlet = CompCMTStatelessLocalServlet.class, contextRoot = "StatelessAnnWeb"),
                    @TestServlet(servlet = CompCMTStatelessRemoteServlet.class, contextRoot = "StatelessAnnWeb"),
                    @TestServlet(servlet = EmptyCMTStatelessRemoteServlet.class, contextRoot = "StatelessAnnWeb"),
                    @TestServlet(servlet = RemoteBusinessInterfaceServlet.class, contextRoot = "StatelessAnnWeb"),
                    @TestServlet(servlet = TxAttrComp2Servlet.class, contextRoot = "StatelessAnnWeb"),
                    @TestServlet(servlet = TxAttrComp3Servlet.class, contextRoot = "StatelessAnnWeb"),
                    @TestServlet(servlet = TxAttrCompView2Servlet.class, contextRoot = "StatelessAnnWeb"),
                    @TestServlet(servlet = TxAttrCompView3Servlet.class, contextRoot = "StatelessAnnWeb"),
                    @TestServlet(servlet = TxAttrMixedClassMethodOverrideServlet.class, contextRoot = "StatelessAnnWeb"),
                    @TestServlet(servlet = TxAttrOverrideServlet.class, contextRoot = "StatelessAnnWeb"),
                    @TestServlet(servlet = TxAttrServlet.class, contextRoot = "StatelessAnnWeb"),
                    @TestServlet(servlet = AnnotationOverByXMLTxAttrServlet.class, contextRoot = "StatelessMixWeb"),
                    @TestServlet(servlet = MixBMTVerificationServlet.class, contextRoot = "StatelessMixWeb"),
                    @TestServlet(servlet = CMTVerificationServlet.class, contextRoot = "StatelessMixWeb"),
                    @TestServlet(servlet = MixCompCMTStatelessLocalServlet.class, contextRoot = "StatelessMixWeb"),
                    @TestServlet(servlet = MixCompCMTStatelessRemoteServlet.class, contextRoot = "StatelessMixWeb"),
                    @TestServlet(servlet = ExternalBeanClassWithAnnServlet.class, contextRoot = "StatelessMixWeb"),
                    @TestServlet(servlet = ExternalBeanClassWithNoAnnServlet.class, contextRoot = "StatelessMixWeb"),
                    @TestServlet(servlet = RemoteStatelessTwoNamesServlet.class, contextRoot = "StatelessMixWeb"),
                    @TestServlet(servlet = StatelessTwoNamesServlet.class, contextRoot = "StatelessMixWeb"),
                    @TestServlet(servlet = TxAttrMixedAnnotationXMLServlet.class, contextRoot = "StatelessMixWeb") })
    public static LibertyServer server;

    @Override
    public LibertyServer getServer() {
        return server;
    }

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().forServers("com.ibm.ws.ejbcontainer.remote.fat.RemoteServer")).andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.remote.fat.RemoteServer")).andWith(FeatureReplacementAction.EE9_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11).forServers("com.ibm.ws.ejbcontainer.remote.fat.RemoteServer")).andWith(FeatureReplacementAction.EE10_FEATURES().forServers("com.ibm.ws.ejbcontainer.remote.fat.RemoteServer"));

    @BeforeClass
    public static void beforeClass() throws Exception {
        // Use ShrinkHelper to build the Ears & Wars

        //#################### InitTxRecoveryLogApp.ear (Automatically initializes transaction recovery logs)
        JavaArchive InitTxRecoveryLogEJBJar = ShrinkHelper.buildJavaArchive("InitTxRecoveryLogEJB.jar", "com.ibm.ws.ejbcontainer.init.recovery.ejb.");

        EnterpriseArchive InitTxRecoveryLogApp = ShrinkWrap.create(EnterpriseArchive.class, "InitTxRecoveryLogApp.ear");
        InitTxRecoveryLogApp.addAsModule(InitTxRecoveryLogEJBJar);

        ShrinkHelper.exportDropinAppToServer(server, InitTxRecoveryLogApp, DeployOptions.SERVER_ONLY);

        //#################### BasicRemote.war
        WebArchive BasicRemoteWeb = ShrinkHelper.buildDefaultApp("BasicRemote.war", "com.ibm.ws.ejbcontainer.remote.fat.basic.");
        BasicRemoteWeb = (WebArchive) ShrinkHelper.addDirectory(BasicRemoteWeb, "test-applications/BasicRemote.war/resources");

        ShrinkHelper.exportDropinAppToServer(server, BasicRemoteWeb, DeployOptions.SERVER_ONLY);

        //#################### CrossAppRemoteClient.war
        JavaArchive CrossAppRemoteSharedJar = ShrinkHelper.buildJavaArchive("CrossAppRemoteShared.jar", "com.ibm.ws.ejbcontainer.remote.fat.crossapp.shared.");
        WebArchive CrossAppRemoteClient = ShrinkHelper.buildDefaultApp("CrossAppRemoteClient.war", "com.ibm.ws.ejbcontainer.remote.fat.crossapp.client.");
        CrossAppRemoteClient.addAsLibrary(CrossAppRemoteSharedJar);
        CrossAppRemoteClient = (WebArchive) ShrinkHelper.addDirectory(CrossAppRemoteClient, "test-applications/CrossAppRemoteClient.war/resources");

        ShrinkHelper.exportDropinAppToServer(server, CrossAppRemoteClient, DeployOptions.SERVER_ONLY);

        //#################### CrossAppRemoteEJB.war
        WebArchive CrossAppRemoteEJB = ShrinkHelper.buildDefaultApp("CrossAppRemoteEJB.war", "com.ibm.ws.ejbcontainer.remote.fat.crossapp.ejb.");
        CrossAppRemoteEJB.addAsLibrary(CrossAppRemoteSharedJar);

        ShrinkHelper.exportDropinAppToServer(server, CrossAppRemoteEJB, DeployOptions.SERVER_ONLY);

        //#################### CrossApp2xTest.ear
        JavaArchive CrossApp2xEJB = ShrinkHelper.buildJavaArchive("CrossApp2xEJB.jar", "com.ibm.ws.ejbcontainer.remote.fat.crossapp.home2x.ejb.");
        CrossApp2xEJB = (JavaArchive) ShrinkHelper.addDirectory(CrossApp2xEJB, "test-applications/CrossApp2xEJB.jar/resources");

        EnterpriseArchive CrossApp2xTest = ShrinkWrap.create(EnterpriseArchive.class, "CrossApp2xTest.ear");
        CrossApp2xTest.addAsModule(CrossApp2xEJB).addAsLibraries(CrossAppRemoteSharedJar);
        CrossApp2xTest = (EnterpriseArchive) ShrinkHelper.addDirectory(CrossApp2xTest, "test-applications/CrossApp2xTest.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, CrossApp2xTest, DeployOptions.SERVER_ONLY);

        //#################### EJBHome2xTest.ear
        JavaArchive EJBHome2xTestEJB = ShrinkHelper.buildJavaArchive("EJBHome2xTestEJB.jar", "com.ibm.ws.ejbcontainer.remote.fat.home2x.ejb.");
        EJBHome2xTestEJB = (JavaArchive) ShrinkHelper.addDirectory(EJBHome2xTestEJB, "test-applications/EJBHome2xTestEJB.jar/resources");
        WebArchive EJBHome2xTestWeb = ShrinkHelper.buildDefaultApp("EJBHome2xTestWeb.war", "com.ibm.ws.ejbcontainer.remote.fat.home2x.web.");

        EnterpriseArchive EJBHome2xTest = ShrinkWrap.create(EnterpriseArchive.class, "EJBHome2xTest.ear");
        EJBHome2xTest.addAsModule(EJBHome2xTestEJB).addAsModule(EJBHome2xTestWeb);
        EJBHome2xTest = (EnterpriseArchive) ShrinkHelper.addDirectory(EJBHome2xTest, "test-applications/EJBHome2xTest.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, EJBHome2xTest, DeployOptions.SERVER_ONLY);

        //#################### EJBHomeTest.war
        WebArchive EJBHomeTest = ShrinkHelper.buildDefaultApp("EJBHomeTest.war", "com.ibm.ws.ejbcontainer.remote.fat.home.");
        EJBHomeTest = (WebArchive) ShrinkHelper.addDirectory(EJBHomeTest, "test-applications/EJBHomeTest.war/resources");

        ShrinkHelper.exportDropinAppToServer(server, EJBHomeTest, DeployOptions.SERVER_ONLY);

        //#################### JitDeployApp
        JavaArchive JitDeployEJBJar = ShrinkHelper.buildJavaArchive("JitDeployEJB.jar", "com.ibm.ws.ejbcontainer.remote.misc.jitdeploy.ejb.");
        WebArchive JitDeployWeb = ShrinkHelper.buildDefaultApp("JitDeployWeb.war", "com.ibm.ws.ejbcontainer.remote.misc.jitdeploy.web.");

        EnterpriseArchive JitDeployApp = ShrinkWrap.create(EnterpriseArchive.class, "JitDeployApp.ear");
        JitDeployApp.addAsModule(JitDeployEJBJar).addAsModule(JitDeployWeb);
        JitDeployApp = (EnterpriseArchive) ShrinkHelper.addDirectory(JitDeployApp, "test-applications/JitDeployApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, JitDeployApp, DeployOptions.SERVER_ONLY);

        //#################### RemoteTx.war
        WebArchive RemoteTx = ShrinkHelper.buildDefaultApp("RemoteTx.war", "com.ibm.ws.ejbcontainer.remote.fat.tx.");
        RemoteTx = (WebArchive) ShrinkHelper.addDirectory(RemoteTx, "test-applications/RemoteTx.war/resources");

        ShrinkHelper.exportDropinAppToServer(server, RemoteTx, DeployOptions.SERVER_ONLY);

        //#################### SingletonApp.ear
        JavaArchive SingletonAnnEJBJar = ShrinkHelper.buildJavaArchive("SingletonAnnEJB.jar", "com.ibm.ws.ejbcontainer.remote.singleton.ann.ejb.",
                                                                       "com.ibm.ws.ejbcontainer.remote.singleton.ann.shared.");
        JavaArchive SingletonMixEJBJar = ShrinkHelper.buildJavaArchive("SingletonMixEJB.jar", "com.ibm.ws.ejbcontainer.remote.singleton.mix.ejb.",
                                                                       "com.ibm.ws.ejbcontainer.remote.singleton.mix.shared.");
        WebArchive SingletonWeb = ShrinkHelper.buildDefaultApp("SingletonWeb.war", "com.ibm.ws.ejbcontainer.remote.singleton.ann.web.",
                                                               "com.ibm.ws.ejbcontainer.remote.singleton.mix.web.");

        EnterpriseArchive SingletonApp = ShrinkWrap.create(EnterpriseArchive.class, "SingletonApp.ear");
        SingletonApp.addAsModule(SingletonAnnEJBJar).addAsModule(SingletonMixEJBJar).addAsModule(SingletonWeb);
        SingletonApp = (EnterpriseArchive) ShrinkHelper.addDirectory(SingletonApp, "test-applications/SingletonApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, SingletonApp, DeployOptions.SERVER_ONLY);

        //#################### StatelessAnnTestApp
        JavaArchive StatelessAnnEJBJar = ShrinkHelper.buildJavaArchive("StatelessAnnEJB.jar", "com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb.");
        WebArchive StatelessAnnWeb = ShrinkHelper.buildDefaultApp("StatelessAnnWeb.war", "com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.web.");

        EnterpriseArchive StatelessAnnApp = ShrinkWrap.create(EnterpriseArchive.class, "StatelessAnnTest.ear");
        StatelessAnnApp.addAsModule(StatelessAnnEJBJar).addAsModule(StatelessAnnWeb);
        StatelessAnnApp = (EnterpriseArchive) ShrinkHelper.addDirectory(StatelessAnnApp, "test-applications/StatelessAnnTest.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, StatelessAnnApp, DeployOptions.SERVER_ONLY);

        //#################### StatelessMixTestApp
        JavaArchive StatelessMixASMDescEJBJar = ShrinkHelper.buildJavaArchive("StatelessMixASMDescEJB.jar", "com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.asmdesc.");
        JavaArchive StatelessMixEJBJar = ShrinkHelper.buildJavaArchive("StatelessMixEJB.jar", "com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.ejb.");
        JavaArchive StatelessMixIntfJar = ShrinkHelper.buildJavaArchive("StatelessMixIntf.jar", "com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.sc2.");
        JavaArchive StatelessMixMDCEJBJar = ShrinkHelper.buildJavaArchive("StatelessMixMDCEJB.jar", "com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.mdc.");
        JavaArchive StatelessMixSCEJBJar = ShrinkHelper.buildJavaArchive("StatelessMixSCEJB.jar", "com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.sc1.");
        WebArchive StatelessMixWeb = ShrinkHelper.buildDefaultApp("StatelessMixWeb.war", "com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.web.");

        EnterpriseArchive StatelessMixApp = ShrinkWrap.create(EnterpriseArchive.class, "StatelessMixTest.ear");
        StatelessMixApp.addAsModule(StatelessMixASMDescEJBJar).addAsModule(StatelessMixEJBJar).addAsModule(StatelessMixMDCEJBJar).addAsModule(StatelessMixSCEJBJar).addAsModule(StatelessMixWeb);
        StatelessMixApp.addAsLibrary(StatelessMixIntfJar);
        StatelessMixApp = (EnterpriseArchive) ShrinkHelper.addDirectory(StatelessMixApp, "test-applications/StatelessMixTest.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, StatelessMixApp, DeployOptions.SERVER_ONLY);

        // Finally, start server
        server.startServer();

    }

    @AfterClass
    public static void afterClass() throws Exception {
        // CNTR0019E - testMandatoryAttribThrowsExcp, testMandatoryAttribThrowsExcpOnLocalInt, + many
        // CNTR0020E - testBusinessRMISystemException, testBusinessRemoteSystemException, + many
        // CNTR0021E - testBusinessRMITransactionException, testBusinessRemoteTransactionException
        // CNTR0328W - testAsyncConfigMaxUnclaimedRemoteResults, testAsyncConfigUnclaimedRemoteResultTimeout
        // CNTR5101W - testEJBHomeRecursiveStubs
        // CWNEN0028E - testAnnInjectionFailure, testAnnDependsOnFailure
        // WTRN0074E - testBusinessRMITransactionException, testBusinessRemoteTransactionException
        // CWWKG0014E - intermittently caused by server.xml being momentarily missing during server reconfig
        if (server != null && server.isStarted()) {
            server.stopServer("CNTR0019E", "CNTR0020E", "CNTR0021E", "CNTR0328W", "CNTR5101W", "CWNEN0028E", "WTRN0074E", "CWWKG0014E");
        }
    }

    private void updateServerConfiguration(ServerConfiguration config) throws Exception {
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton("BasicRemote"));
    }

    private void restoreServerConfiguration() throws Exception {
        server.setMarkToEndOfLog();
        server.restoreServerConfiguration();
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton("BasicRemote"));
    }

    @Test
    public void testAsyncConfigMaxUnclaimedRemoteResults() throws Exception {
        server.saveServerConfiguration();
        ServerConfiguration config = server.getServerConfiguration();
        EJBAsynchronousElement asynchronous = new EJBAsynchronousElement();
        asynchronous.setMaxUnclaimedRemoteResults(1);
        config.getEJBContainer().setAsynchronous(asynchronous);
        try {
            updateServerConfiguration(config);
            runTest("BasicRemote/BasicRemoteTestServlet");
        } finally {
            restoreServerConfiguration();
        }
    }

    @Test
    public void testAsyncConfigUnclaimedRemoteResultTimeout() throws Exception {
        server.saveServerConfiguration();
        ServerConfiguration config = server.getServerConfiguration();
        EJBAsynchronousElement asynchronous = new EJBAsynchronousElement();
        asynchronous.setUnclaimedRemoteResultTimeout("1s");
        asynchronous.setExtraAttribute("scheduledExecutorService.target", "(deferrable=false)");
        config.getEJBContainer().setAsynchronous(asynchronous);
        try {
            updateServerConfiguration(config);
            runTest("BasicRemote/BasicRemoteTestServlet");
        } finally {
            restoreServerConfiguration();
        }
    }
}