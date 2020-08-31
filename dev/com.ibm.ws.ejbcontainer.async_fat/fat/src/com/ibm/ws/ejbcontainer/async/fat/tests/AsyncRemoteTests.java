/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.async.fat.tests;

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
import com.ibm.ws.ejbcontainer.async.fat.cancel.web.FutureCancelLocalTestServlet;
import com.ibm.ws.ejbcontainer.async.fat.cancel.web.FutureCancelRemoteTestServlet;
import com.ibm.ws.ejbcontainer.async.fat.fafRemote.web.AsyncFireAndForgetRemoteServlet;
import com.ibm.ws.ejbcontainer.async.fat.farRemote.web.ResultsStatelessRemoteServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class AsyncRemoteTests extends AbstractTest {

    @Server("com.ibm.ws.ejbcontainer.async.fat.AsyncRemoteServer")
    @TestServlets({ @TestServlet(servlet = AsyncFireAndForgetRemoteServlet.class, contextRoot = "AsyncFafRemoteWeb"),
                    @TestServlet(servlet = ResultsStatelessRemoteServlet.class, contextRoot = "AsyncFarRemoteWeb"),
                    @TestServlet(servlet = FutureCancelLocalTestServlet.class, contextRoot = "AsyncFutureCancelTest"),
                    @TestServlet(servlet = FutureCancelRemoteTestServlet.class, contextRoot = "AsyncFutureCancelTest") })
    public static LibertyServer server;

    @Override
    public LibertyServer getServer() {
        return server;
    }

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.async.fat.AsyncRemoteServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.async.fat.AsyncRemoteServer")).andWith(new JakartaEE9Action().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.async.fat.AsyncRemoteServer"));

    @BeforeClass
    public static void beforeClass() throws Exception {
        // cleanup from prior repeat actions
        server.deleteAllDropinApplications();
        server.removeAllInstalledAppsForValidation();

        // Use ShrinkHelper to build the Ears & Wars

        //#################### InitTxRecoveryLogApp.ear (Automatically initializes transaction recovery logs)
        JavaArchive InitTxRecoveryLogEJBJar = ShrinkHelper.buildJavaArchive("InitTxRecoveryLogEJB.jar", "com.ibm.ws.ejbcontainer.init.recovery.ejb.");

        EnterpriseArchive InitTxRecoveryLogApp = ShrinkWrap.create(EnterpriseArchive.class, "InitTxRecoveryLogApp.ear");
        InitTxRecoveryLogApp.addAsModule(InitTxRecoveryLogEJBJar);

        ShrinkHelper.exportDropinAppToServer(server, InitTxRecoveryLogApp, DeployOptions.SERVER_ONLY);

        //#################### AsyncFafRemoteApp.ear
        JavaArchive AsyncFafRemoteEJB = ShrinkHelper.buildJavaArchive("AsyncFafRemoteEJB.jar", "com.ibm.ws.ejbcontainer.async.fat.fafRemote.ejb.");
        WebArchive AsyncFafRemoteWeb = ShrinkHelper.buildDefaultApp("AsyncFafRemoteWeb.war", "com.ibm.ws.ejbcontainer.async.fat.fafRemote.web.");
        EnterpriseArchive AsyncFafRemoteApp = ShrinkWrap.create(EnterpriseArchive.class, "AsyncFafRemoteApp.ear");
        AsyncFafRemoteApp.addAsModule(AsyncFafRemoteEJB).addAsModule(AsyncFafRemoteWeb);
        AsyncFafRemoteApp = (EnterpriseArchive) ShrinkHelper.addDirectory(AsyncFafRemoteApp, "test-applications/AsyncFafRemoteApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, AsyncFafRemoteApp, DeployOptions.SERVER_ONLY);

        //#################### AsyncFarRemoteTest.ear
        JavaArchive AsyncFarRemoteEJB = ShrinkHelper.buildJavaArchive("AsyncFarRemoteEJB.jar", "com.ibm.ws.ejbcontainer.async.fat.farRemote.ejb.");
        WebArchive AsyncFarRemoteWeb = ShrinkHelper.buildDefaultApp("AsyncFarRemoteWeb.war", "com.ibm.ws.ejbcontainer.async.fat.farRemote.web.");
        EnterpriseArchive AsyncFarRemoteTest = ShrinkWrap.create(EnterpriseArchive.class, "AsyncFarRemoteTest.ear");
        AsyncFarRemoteTest.addAsModule(AsyncFarRemoteEJB).addAsModule(AsyncFarRemoteWeb);
        AsyncFarRemoteTest = (EnterpriseArchive) ShrinkHelper.addDirectory(AsyncFarRemoteTest, "test-applications/AsyncFarRemoteTest.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, AsyncFarRemoteTest, DeployOptions.SERVER_ONLY);

        //#################### AsyncFutureCancelTest.war
        WebArchive AsyncFutureCancelTest = ShrinkHelper.buildDefaultApp("AsyncFutureCancelTest.war", "com.ibm.ws.ejbcontainer.async.fat.cancel.web.");
        AsyncFutureCancelTest = (WebArchive) ShrinkHelper.addDirectory(AsyncFutureCancelTest, "test-applications/AsyncFutureCancelTest.war/resources");

        ShrinkHelper.exportDropinAppToServer(server, AsyncFutureCancelTest, DeployOptions.SERVER_ONLY);

        // Finally, start server
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        // CNTR0328W - AsyncFutureCancelRemoteTest/testAsyncRecancelledTrueParameter
        // CWWKE1102W - AsyncFutureCancelLocalTest: Remove when ExecutorServiceImpl.RunnableWrapper properly handles cancel
        // CWWKE1107W - AsyncFutureCancelLocalTest: Remove when ExecutorServiceImpl.RunnableWrapper properly handles cancel
        server.stopServer("CNTR0328W", "CWWKE1102W", "CWWKE1107W");
    }

}