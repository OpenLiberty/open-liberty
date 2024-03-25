/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.cdi40.internal.fat.startupEvents;

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

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.cdi40.internal.fat.startupEvents.ear.ejb.StartupSingletonEJB;
import io.openliberty.cdi40.internal.fat.startupEvents.ear.lib.EarLibApplicationScopedBean;
import io.openliberty.cdi40.internal.fat.startupEvents.ear.war.StartupEventsServlet;
import io.openliberty.cdi40.internal.fat.startupEvents.sharedLib.AbstractObserver;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class StartupEventsTest extends FATServletClient {
    public static final String SERVER_NAME = "StartupEventsServer";

    public static final String STARTUP_EVENTS_APP_NAME = "StartupEvents";

    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE10, EERepeatActions.EE11);

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = StartupEventsServlet.class, contextRoot = STARTUP_EVENTS_APP_NAME)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        JavaArchive startupEventsSharedLib = ShrinkWrap.create(JavaArchive.class, STARTUP_EVENTS_APP_NAME + "SharedLib.jar")
                                                       .addPackage(AbstractObserver.class.getPackage());

        JavaArchive startupEventsEarLib = ShrinkWrap.create(JavaArchive.class, STARTUP_EVENTS_APP_NAME + ".jar")
                                                    .addPackage(EarLibApplicationScopedBean.class.getPackage());
        CDIArchiveHelper.addBeansXML(startupEventsEarLib, DiscoveryMode.ANNOTATED);

        WebArchive startupEventsWar = ShrinkWrap.create(WebArchive.class, STARTUP_EVENTS_APP_NAME + ".war")
                                                .addPackage(StartupEventsServlet.class.getPackage());
        CDIArchiveHelper.addBeansXML(startupEventsWar, DiscoveryMode.ANNOTATED);

        JavaArchive startupEventsEJBLib = ShrinkWrap.create(JavaArchive.class, STARTUP_EVENTS_APP_NAME + "EJB.jar")
                                                    .addPackage(StartupSingletonEJB.class.getPackage());
        CDIArchiveHelper.addBeansXML(startupEventsEJBLib, DiscoveryMode.ANNOTATED);

        EnterpriseArchive startupEventsEar = ShrinkWrap.create(EnterpriseArchive.class, STARTUP_EVENTS_APP_NAME + ".ear")
                                                       .addAsLibrary(startupEventsEarLib)
                                                       .addAsModule(startupEventsWar)
                                                       .addAsModule(startupEventsEJBLib);

        ShrinkHelper.exportToServer(server, "StartupEventsSharedLibrary", startupEventsSharedLib, DeployOptions.SERVER_ONLY);

        ShrinkHelper.exportAppToServer(server, startupEventsEar, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}
