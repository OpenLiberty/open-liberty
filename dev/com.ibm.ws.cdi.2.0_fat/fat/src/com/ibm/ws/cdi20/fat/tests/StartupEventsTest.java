/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi20.fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.CDIArchiveHelper;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.beansxml.BeansAsset.DiscoveryMode;
import com.ibm.ws.cdi20.fat.apps.events.ejb.SingletonStartupBean;
import com.ibm.ws.cdi20.fat.apps.events.war.StartupEventsServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class StartupEventsTest extends FATServletClient {
    public static final String SERVER_NAME = "cdi20StartupEventsServer";

    public static final String STARTUP_EVENTS_APP_NAME = "StartupEvents";

    @ClassRule
    public static RepeatTests r = FATSuite.defaultRepeat(SERVER_NAME);

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = StartupEventsServlet.class, contextRoot = STARTUP_EVENTS_APP_NAME + ".war")
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        JavaArchive startupEventsEJB = ShrinkWrap.create(JavaArchive.class, STARTUP_EVENTS_APP_NAME + ".jar")
                                                 .addPackage(SingletonStartupBean.class.getPackage());
        CDIArchiveHelper.addBeansXML(startupEventsEJB, DiscoveryMode.ANNOTATED);

        WebArchive startupEventsWar = ShrinkWrap.create(WebArchive.class, STARTUP_EVENTS_APP_NAME + ".war")
                                                .addPackage(StartupEventsServlet.class.getPackage());
        CDIArchiveHelper.addBeansXML(startupEventsWar, DiscoveryMode.ANNOTATED);

        EnterpriseArchive startupEventsEar = ShrinkWrap.create(EnterpriseArchive.class, STARTUP_EVENTS_APP_NAME + ".ear");
        startupEventsEar.addAsModule(startupEventsEJB);
        startupEventsEar.addAsModule(startupEventsWar);

        ShrinkHelper.exportDropinAppToServer(server, startupEventsEar, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}
