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
import com.ibm.ws.ejbcontainer.async.fat.nested.web.NestedAsyncServlet;
import com.ibm.ws.ejbcontainer.async.fat.web.AsyncInheritanceMixServlet;
import com.ibm.ws.ejbcontainer.async.fat.web.BasicMixServlet;
import com.ibm.ws.ejbcontainer.async.fat.web.BasicServlet;
import com.ibm.ws.ejbcontainer.async.fat.web.BasicXmlServlet;
import com.ibm.ws.ejbcontainer.async.fat.web.ExceptionServlet;
import com.ibm.ws.ejbcontainer.async.fat.web.MetaDataCompleteMixServlet;
import com.ibm.ws.ejbcontainer.async.fat.web.ResultsServlet;
import com.ibm.ws.ejbcontainer.async.fat.web.ResultsXmlServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class AsyncCoreTests extends AbstractTest {

    @Server("com.ibm.ws.ejbcontainer.async.fat.AsyncCoreServer")
    @TestServlets({ @TestServlet(servlet = AsyncInheritanceMixServlet.class, contextRoot = "AsyncTestWeb"),
                    @TestServlet(servlet = BasicMixServlet.class, contextRoot = "AsyncTestWeb"),
                    @TestServlet(servlet = BasicServlet.class, contextRoot = "AsyncTestWeb"),
                    @TestServlet(servlet = BasicXmlServlet.class, contextRoot = "AsyncTestWeb"),
                    @TestServlet(servlet = ExceptionServlet.class, contextRoot = "AsyncTestWeb"),
                    @TestServlet(servlet = MetaDataCompleteMixServlet.class, contextRoot = "AsyncTestWeb"),
                    @TestServlet(servlet = ResultsServlet.class, contextRoot = "AsyncTestWeb"),
                    @TestServlet(servlet = ResultsXmlServlet.class, contextRoot = "AsyncTestWeb"),
                    @TestServlet(servlet = NestedAsyncServlet.class, contextRoot = "NestedAsyncTest") })
    public static LibertyServer server;

    @Override
    public LibertyServer getServer() {
        return server;
    }

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().forServers("com.ibm.ws.ejbcontainer.async.fat.AsyncCoreServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.async.fat.AsyncCoreServer")).andWith(new JakartaEE9Action().forServers("com.ibm.ws.ejbcontainer.async.fat.AsyncCoreServer"));

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

        //#################### AsyncTestApp.ear
        JavaArchive AsyncTestEJB_Ann = ShrinkHelper.buildJavaArchive("AsyncTestEJB-Ann.jar", "com.ibm.ws.ejbcontainer.async.fat.ann.ejb.");
        JavaArchive AsyncTestEJB_Mix = ShrinkHelper.buildJavaArchive("AsyncTestEJB-Mix.jar", "com.ibm.ws.ejbcontainer.async.fat.mix.ejb.",
                                                                     "com.ibm.ws.ejbcontainer.async.fat.mix.shared.");
        AsyncTestEJB_Mix = (JavaArchive) ShrinkHelper.addDirectory(AsyncTestEJB_Mix, "test-applications/AsyncTestEJB-Mix.jar/resources");
        JavaArchive AsyncTestEJB_Xml = ShrinkHelper.buildJavaArchive("AsyncTestEJB-Xml.jar", "com.ibm.ws.ejbcontainer.async.fat.xml.ejb.");
        AsyncTestEJB_Xml = (JavaArchive) ShrinkHelper.addDirectory(AsyncTestEJB_Xml, "test-applications/AsyncTestEJB-Xml.jar/resources");
        JavaArchive AsyncTestEJB30_Mix = ShrinkHelper.buildJavaArchive("AsyncTestEJB30-Mix.jar", "com.ibm.ws.ejbcontainer.async.fat.mix.ejb30.");
        AsyncTestEJB30_Mix = (JavaArchive) ShrinkHelper.addDirectory(AsyncTestEJB30_Mix, "test-applications/AsyncTestEJB30-Mix.jar/resources");
        JavaArchive MetaDataAsyncBean = ShrinkHelper.buildJavaArchive("MetaDataAsyncBean.jar", "com.ibm.ws.ejbcontainer.async.fat.mix.mdcomp.ejb.");
        MetaDataAsyncBean = (JavaArchive) ShrinkHelper.addDirectory(MetaDataAsyncBean, "test-applications/MetaDataAsyncBean.jar/resources");
        WebArchive AsyncTestWeb = ShrinkHelper.buildDefaultApp("AsyncTestWeb.war", "com.ibm.ws.ejbcontainer.async.fat.web.");

        EnterpriseArchive AsyncTestApp = ShrinkWrap.create(EnterpriseArchive.class, "AsyncTestApp.ear");
        AsyncTestApp.addAsModule(AsyncTestEJB_Ann).addAsModule(AsyncTestEJB_Mix).addAsModule(AsyncTestEJB_Xml).addAsModule(AsyncTestEJB30_Mix).addAsModule(MetaDataAsyncBean).addAsModule(AsyncTestWeb);
        AsyncTestApp = (EnterpriseArchive) ShrinkHelper.addDirectory(AsyncTestApp, "test-applications/AsyncTestApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, AsyncTestApp, DeployOptions.SERVER_ONLY);

        //#################### NestedAsyncTestApp.ear
        JavaArchive NestedAsyncEJB = ShrinkHelper.buildJavaArchive("NestedAsyncEJB.jar", "com.ibm.ws.ejbcontainer.async.fat.nested.ejb.");
        WebArchive NestedAsyncTest = ShrinkHelper.buildDefaultApp("NestedAsyncTest.war", "com.ibm.ws.ejbcontainer.async.fat.nested.web.");

        EnterpriseArchive NestedAsyncTestApp = ShrinkWrap.create(EnterpriseArchive.class, "NestedAsyncTestApp.ear");
        NestedAsyncTestApp.addAsModule(NestedAsyncEJB).addAsModule(NestedAsyncTest);

        ShrinkHelper.exportDropinAppToServer(server, NestedAsyncTestApp, DeployOptions.SERVER_ONLY);

        // Finally, start server
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        // CNTR0020E - ExceptionServlet
        server.stopServer("CNTR0020E");
    }
}