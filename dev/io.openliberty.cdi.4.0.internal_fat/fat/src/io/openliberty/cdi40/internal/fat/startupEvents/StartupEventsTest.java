/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.cdi40.internal.fat.startupEvents;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.CDIArchiveHelper;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.beansxml.BeansAsset.DiscoveryMode;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.cdi40.internal.fat.startupEvents.ear.lib.EarLibApplicationScopedBean;
import io.openliberty.cdi40.internal.fat.startupEvents.ear.war.StartupEventsServlet;
import io.openliberty.cdi40.internal.fat.startupEvents.ear.war.WarApplicationScopedBean;
import io.openliberty.cdi40.internal.fat.startupEvents.sharedLib.AbstractObserver;
import io.openliberty.cdi40.internal.fat.startupEvents.sharedLib.SharedLibApplicationScopedBean;

@RunWith(FATRunner.class)
public class StartupEventsTest {
    public static final String SERVER_NAME = "StartupEventsServer";

    public static final String STARTUP_EVENTS_APP_NAME = "StartupEvents";

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = StartupEventsServlet.class, contextRoot = STARTUP_EVENTS_APP_NAME)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        JavaArchive startupEventsSharedLib = ShrinkWrap.create(JavaArchive.class, STARTUP_EVENTS_APP_NAME + "SharedLib.jar")
                                                       .addClass(AbstractObserver.class)
                                                       .addClass(SharedLibApplicationScopedBean.class);

        JavaArchive startupEventsEarLib = ShrinkWrap.create(JavaArchive.class, STARTUP_EVENTS_APP_NAME + ".jar")
                                                    .addClass(EarLibApplicationScopedBean.class);
        CDIArchiveHelper.addBeansXML(startupEventsEarLib, DiscoveryMode.ANNOTATED);

        WebArchive startupEventsWar = ShrinkWrap.create(WebArchive.class, STARTUP_EVENTS_APP_NAME + ".war")
                                                .addClass(StartupEventsServlet.class)
                                                .addClass(WarApplicationScopedBean.class);
        CDIArchiveHelper.addBeansXML(startupEventsWar, DiscoveryMode.ANNOTATED);

        EnterpriseArchive startupEventsEar = ShrinkWrap.create(EnterpriseArchive.class, STARTUP_EVENTS_APP_NAME + ".ear")
                                                       .addAsLibrary(startupEventsEarLib)
                                                       .addAsModule(startupEventsWar);

        ShrinkHelper.exportToServer(server, "StartupEventsSharedLibrary", startupEventsSharedLib, DeployOptions.SERVER_ONLY);

        ShrinkHelper.exportAppToServer(server, startupEventsEar, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}
