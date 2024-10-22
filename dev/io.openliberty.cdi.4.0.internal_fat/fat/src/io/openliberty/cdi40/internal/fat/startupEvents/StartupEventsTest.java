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

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.CDIArchiveHelper;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.beansxml.BeansAsset.DiscoveryMode;
import com.ibm.ws.fat.util.jmx.mbeans.ApplicationMBean;

import componenttest.annotation.Server;
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
import junit.framework.Assert;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class StartupEventsTest extends FATServletClient {
    public static final String SERVER_NAME = "StartupEventsServer";

    public static final String STARTUP_EVENTS_APP_NAME = "StartupEvents";
    public static final String SERVLET_PATH = "events";

    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, true, EERepeatActions.EE10, EERepeatActions.EE11);

    @Server(SERVER_NAME)
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

    @Test
    public void runStartupAndShutdownEvents() throws Exception {

        /*
         * Unfortunately there's no better way to do this. We need these to run before app shutdown
         * componentest 2.0 doesn't provide FixMethodOrder. And the test framework doesn't provide
         * any guarantees of the relative ordering between tests here and tests on the servlet.
         * So unfortunately we have to run this as one big test.
         */
        runTest(server, STARTUP_EVENTS_APP_NAME + "/" + SERVLET_PATH, "testWarStartupEvents");
        runTest(server, STARTUP_EVENTS_APP_NAME + "/" + SERVLET_PATH, "testEarLibStartupEvents");
        runTest(server, STARTUP_EVENTS_APP_NAME + "/" + SERVLET_PATH, "testSharedLibStartupEvents");
        runTest(server, STARTUP_EVENTS_APP_NAME + "/" + SERVLET_PATH, "testStartupEJBStartupEvents");

        server.setMarkToEndOfLog();
        ApplicationMBean mbean = server.getApplicationMBean(STARTUP_EVENTS_APP_NAME);
        mbean.stop();

        List<String> failures = server.findStringsInLogs("===TEST FAIL");
        List<String> exceptions = server.findStringsInLogs("===TEST EXCEPTION");

        Assert.assertTrue("Tests did not pass: " + System.lineSeparator()
                          + String.join(System.lineSeparator(), failures) + System.lineSeparator()
                          + String.join(System.lineSeparator(), exceptions),
                          failures.isEmpty() && exceptions.isEmpty());

        List<String> expectedLogOutput = List.of("===TEST PASS io.openliberty.cdi40.internal.fat.startupEvents.ear.war.WarApplicationScopedBean.observeDestroy",
                                                 "===TEST PASS io.openliberty.cdi40.internal.fat.startupEvents.sharedLib.SharedLibApplicationScopedBean.observeDestroy",
                                                 "===TEST PASS io.openliberty.cdi40.internal.fat.startupEvents.ear.lib.EarLibApplicationScopedBean.observeDestroy",
                                                 "===TEST PASS io.openliberty.cdi40.internal.fat.startupEvents.ear.ejb.EjbApplicationScopedBean.observeDestroy",
                                                 "===TEST PASS io.openliberty.cdi40.internal.fat.startupEvents.ear.war.WarApplicationScopedBean.observeShutdown",
                                                 "===TEST PASS io.openliberty.cdi40.internal.fat.startupEvents.sharedLib.SharedLibApplicationScopedBean.observeShutdown",
                                                 "===TEST PASS io.openliberty.cdi40.internal.fat.startupEvents.ear.lib.EarLibApplicationScopedBean.observeShutdown",
                                                 "===TEST PASS io.openliberty.cdi40.internal.fat.startupEvents.ear.ejb.EjbApplicationScopedBean.observeShutdown");

        server.waitForStringsInLogUsingMark(expectedLogOutput);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}
