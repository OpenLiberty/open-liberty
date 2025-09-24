/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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

package com.ibm.ws.ejbcontainer.singleton.startup.fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import singleton.property.web.AllowWorkNoStartupServlet;
import singleton.property.web.AllowWorkStartupServlet;

/**
 * Test the behavior of the com.ibm.websphere.ejbcontainer.blockWorkUntilAppStartedWaitTime
 * property for applications with and without Startup Singleton beans when the
 * com.ibm.websphere.ejbcontainer.blockWorkUntilAppStarted property has been disabled.
 *
 * During application startup of an application that contains Startup Singleton bean,
 * outside work will be blocked. The default time that this work will be blocked before
 * an exception is thrown in 2 minutes. However, if the "blockWorkUnitlAppStartedWaitTime"
 * property is set then outside work will be blocked until the application has completed
 * startup processing or until the "blockWorkUntilAppStartedWaitTime" threshold has been met.
 *
 * During application startup of an application that does not contain a Startup Singleton bean,
 * outside work will not be blocked.
 *
 * The following property values have been set in the jvm.options:
 * -Dcom.ibm.websphere.ejbcontainer.blockWorkUntilAppStartedWaitTime=20
 * -Dcom.ibm.websphere.ejbcontainer.blockWorkUntilAppStarted=false
 */
@RunWith(FATRunner.class)
public class PropertyBlockWorkDisabledTest extends FATServletClient {

    @Server("PropertyBlockWorkDisabledServer")
    @TestServlets({ @TestServlet(servlet = AllowWorkNoStartupServlet.class, contextRoot = "SingletonPropertyTest"),
                    @TestServlet(servlet = AllowWorkStartupServlet.class, contextRoot = "SingletonPropertyTest") })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        // Use ShrinkHelper to build the ears

        // -------------- SingletonPropertyIntf ------------
        JavaArchive SingletonPropertyIntf = ShrinkHelper.buildJavaArchive("SingletonPropertyIntf.jar", "singleton.property.shared");
        ShrinkHelper.exportToServer(server, "lib", SingletonPropertyIntf, DeployOptions.SERVER_ONLY);

        // -------------- SingletonPropertyStartup ------------
        JavaArchive SingletonPropertyStartupBean = ShrinkHelper.buildJavaArchive("SingletonPropertyStartupBean.jar", "singleton.property.startup.ejb.");
        ShrinkHelper.addDirectory(SingletonPropertyStartupBean, "test-applications/SingletonPropertyStartupBean.jar/resources");
        EnterpriseArchive SingletonPropertyStartup = ShrinkWrap.create(EnterpriseArchive.class, "SingletonPropertyStartup.ear");
        SingletonPropertyStartup.addAsModules(SingletonPropertyStartupBean);
        ShrinkHelper.exportAppToServer(server, SingletonPropertyStartup, DeployOptions.DISABLE_VALIDATION, DeployOptions.SERVER_ONLY);

        // -------------- SingletonPropertyNoStartup ------------
        JavaArchive SingletonPropertyNoStartupBean = ShrinkHelper.buildJavaArchive("SingletonPropertyNoStartupBean.jar", "singleton.property.nostartup.ejb.");
        WebArchive SingletonPropertyNoStartupWeb = ShrinkHelper.buildDefaultApp("SingletonPropertyNoStartupWeb.war", "singleton.property.nostartup.web.");
        EnterpriseArchive SingletonPropertyNoStartup = ShrinkWrap.create(EnterpriseArchive.class, "SingletonPropertyNoStartup.ear");
        SingletonPropertyNoStartup.addAsModules(SingletonPropertyNoStartupBean);
        SingletonPropertyNoStartup.addAsModules(SingletonPropertyNoStartupWeb);
        // TODO : com.ibm.websphere.ejbcontainer.blockWorkUntilAppStarted not supported on Liberty
        // ShrinkHelper.exportAppToServer(server, SingletonPropertyNoStartup, DeployOptions.DISABLE_VALIDATION, DeployOptions.SERVER_ONLY);

        // -------------- SingletonPropertyTest ------------
        WebArchive SingletonPropertyTest = ShrinkHelper.buildDefaultApp("SingletonPropertyTest.war", "singleton.property.web.");
        ShrinkHelper.exportAppToServer(server, SingletonPropertyTest, DeployOptions.SERVER_ONLY);

        // servers will be started in each test
        server.startServer();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
}
