/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.exception.fat.tests;

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
import com.ibm.ws.ejbcontainer.exception.web.ExceptionServlet;
import com.ibm.ws.ejbcontainer.exception2x.web.Exception2xServlet;
import com.ibm.ws.ejbcontainer.exception2x.web.LifecycleMethod2xServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class ExceptionTest {
    @Server("com.ibm.ws.ejbcontainer.exception.fat.ExceptionServer")
    @TestServlets({ @TestServlet(servlet = ExceptionServlet.class, contextRoot = "ExceptionWeb"),
                    @TestServlet(servlet = Exception2xServlet.class, contextRoot = "Exception2xWeb"),
                    @TestServlet(servlet = LifecycleMethod2xServlet.class, contextRoot = "Exception2xWeb") })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.exception.fat.ExceptionServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.exception.fat.ExceptionServer")).andWith(FeatureReplacementAction.EE9_FEATURES().forServers("com.ibm.ws.ejbcontainer.exception.fat.ExceptionServer"));

    @BeforeClass
    public static void beforeClass() throws Exception {
        // Use ShrinkHelper to build the ExceptionApp ear
        JavaArchive ExceptionBean = ShrinkHelper.buildJavaArchive("ExceptionBean.jar", "com.ibm.ws.ejbcontainer.exception.ejb.");
        WebArchive ExceptionWeb = ShrinkHelper.buildDefaultApp("ExceptionWeb.war", "com.ibm.ws.ejbcontainer.exception.web.");

        EnterpriseArchive ExceptionApp = ShrinkWrap.create(EnterpriseArchive.class, "ExceptionApp.ear");
        ExceptionApp.addAsModule(ExceptionBean).addAsModule(ExceptionWeb);
        ExceptionApp = (EnterpriseArchive) ShrinkHelper.addDirectory(ExceptionApp, "test-applications/ExceptionApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, ExceptionApp, DeployOptions.SERVER_ONLY);

        // Use ShrinkHelper to build the Exception2xApp ear
        JavaArchive Exception2xBean = ShrinkHelper.buildJavaArchive("Exception2xBean.jar", "com.ibm.ws.ejbcontainer.exception2x.ejb.");
        Exception2xBean = (JavaArchive) ShrinkHelper.addDirectory(Exception2xBean, "test-applications/Exception2xBean.jar/resources");
        WebArchive Exception2xWeb = ShrinkHelper.buildDefaultApp("Exception2xWeb.war", "com.ibm.ws.ejbcontainer.exception2x.web.");

        EnterpriseArchive Exception2xApp = ShrinkWrap.create(EnterpriseArchive.class, "Exception2xApp.ear");
        Exception2xApp.addAsModule(Exception2xBean).addAsModule(Exception2xWeb);
        Exception2xApp = (EnterpriseArchive) ShrinkHelper.addDirectory(Exception2xApp, "test-applications/Exception2xApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, Exception2xApp, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        // CNTR0019E: EJB threw an unexpected (non-declared) exception during invocation of method "create"
        // CNTR0020E: EJB threw an unexpected (non-declared) exception
        // WLTC0017E: Resources rolled back due to setRollbackOnly() being called
        server.stopServer("CNTR0019E", "CNTR0020E", "WLTC0017E");
    }
}
