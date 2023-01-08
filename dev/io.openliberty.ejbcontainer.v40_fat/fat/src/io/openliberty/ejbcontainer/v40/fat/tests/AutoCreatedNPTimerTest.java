/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package io.openliberty.ejbcontainer.v40.fat.tests;

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
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.ejbcontainer.fat.timer.auto.np.web.AutoCreatedNPTimerServlet;

@RunWith(FATRunner.class)
public class AutoCreatedNPTimerTest extends FATServletClient {
    public static final String AUTO_WAR_NAME = "AutoNPTimersWeb";
    private static final String SERVLET = "AutoNPTimersWeb/AutoCreatedNPTimerServlet";

    @Server("AutoNPTimerServer")
    @TestServlet(servlet = AutoCreatedNPTimerServlet.class, contextRoot = AUTO_WAR_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        // Use ShrinkHelper to build the Ears & Wars

        //#################### AutoNPTimersApp.ear
        JavaArchive AutoNPTimersEJB = ShrinkHelper.buildJavaArchive("AutoNPTimersEJB.jar", "io.openliberty.ejbcontainer.fat.timer.auto.np.ejb.");
        AutoNPTimersEJB = (JavaArchive) ShrinkHelper.addDirectory(AutoNPTimersEJB, "test-applications/AutoNPTimersEJB.jar/resources");
        WebArchive AutoNPTimersWeb = ShrinkHelper.buildDefaultApp("AutoNPTimersWeb.war", "io.openliberty.ejbcontainer.fat.timer.auto.np.web.");

        EnterpriseArchive AutoNPTimersApp = ShrinkWrap.create(EnterpriseArchive.class, "AutoNPTimersApp.ear");
        AutoNPTimersApp.addAsModule(AutoNPTimersEJB).addAsModule(AutoNPTimersWeb);

        ShrinkHelper.exportDropinAppToServer(server, AutoNPTimersApp, DeployOptions.SERVER_ONLY);

        // Finally, start server
        server.startServer();

        FATServletClient.runTest(server, SERVLET, "setup");
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        FATServletClient.runTest(server, SERVLET, "cleanup");
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
}
