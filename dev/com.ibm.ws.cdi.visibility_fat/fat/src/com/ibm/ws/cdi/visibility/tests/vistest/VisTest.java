/*******************************************************************************
 * Copyright (c) 2015, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.visibility.tests.vistest;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi.visibility.tests.vistest.appClient.AppClientTargetBean;
import com.ibm.ws.cdi.visibility.tests.vistest.appClient.AppClientTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.appClient.main.Main;
import com.ibm.ws.cdi.visibility.tests.vistest.appClientAsAppClientLib.AppClientAsAppClientLibTargetBean;
import com.ibm.ws.cdi.visibility.tests.vistest.appClientAsAppClientLib.AppClientAsAppClientLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.appClientAsEjbLib.AppClientAsEjbLibTargetBean;
import com.ibm.ws.cdi.visibility.tests.vistest.appClientAsEjbLib.AppClientAsEjbLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.appClientAsWarLib.AppClientAsWarLibTargetBean;
import com.ibm.ws.cdi.visibility.tests.vistest.appClientAsWarLib.AppClientAsWarLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.appClientLib.AppClientLibTargetBean;
import com.ibm.ws.cdi.visibility.tests.vistest.appClientLib.AppClientLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.commonLib.CommonLibTargetBean;
import com.ibm.ws.cdi.visibility.tests.vistest.commonLib.CommonLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.earLib.EarLibTargetBean;
import com.ibm.ws.cdi.visibility.tests.vistest.earLib.EarLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.ejb.EjbTargetBean;
import com.ibm.ws.cdi.visibility.tests.vistest.ejb.EjbTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.ejbAppClientLib.EjbAppClientLibTargetBean;
import com.ibm.ws.cdi.visibility.tests.vistest.ejbAppClientLib.EjbAppClientLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.ejbAsAppClientLib.EjbAsAppClientLibTargetBean;
import com.ibm.ws.cdi.visibility.tests.vistest.ejbAsAppClientLib.EjbAsAppClientLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.ejbAsEjbLib.EjbAsEjbLibTargetBean;
import com.ibm.ws.cdi.visibility.tests.vistest.ejbAsEjbLib.EjbAsEjbLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.ejbAsWarLib.EjbAsWarLibTargetBean;
import com.ibm.ws.cdi.visibility.tests.vistest.ejbAsWarLib.EjbAsWarLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.ejbLib.EjbLibTargetBean;
import com.ibm.ws.cdi.visibility.tests.vistest.ejbLib.EjbLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.ejbWarLib.EjbWarLibTargetBean;
import com.ibm.ws.cdi.visibility.tests.vistest.ejbWarLib.EjbWarLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.nonLib.NonLibTargetBean;
import com.ibm.ws.cdi.visibility.tests.vistest.nonLib.NonLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.privateLib.PrivateLibTargetBean;
import com.ibm.ws.cdi.visibility.tests.vistest.privateLib.PrivateLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.war.WarTargetBean;
import com.ibm.ws.cdi.visibility.tests.vistest.war.WarTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.war.servlet.VisibilityTestServlet;
import com.ibm.ws.cdi.visibility.tests.vistest.war2.War2TargetBean;
import com.ibm.ws.cdi.visibility.tests.vistest.war2.War2TestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.warAppClientLib.WarAppClientLibTargetBean;
import com.ibm.ws.cdi.visibility.tests.vistest.warAppClientLib.WarAppClientLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.warLib.WarLibTargetBean;
import com.ibm.ws.cdi.visibility.tests.vistest.warLib.WarLibTestingBean;
import com.ibm.ws.cdi.visibility.tests.vistest.warWebinfLib.WarWebinfLibTargetBean;
import com.ibm.ws.cdi.visibility.tests.vistest.warWebinfLib.WarWebinfLibTestingBean;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyClientFactory;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

/**
 * Tests the visibility of beans between different BDAs
 * <p>
 * We've identified the following interesting places that a bean can exist and we test the visibility between each of them
 * <p>
 * <ul>
 * <li>Ejb - an EJB jar</li>
 * <li>War - a WAR</li>
 * <li>AppClient - an application client jar</li>
 * <li>EjbLib - a jar referenced on the classpath of Ejb</li>
 * <li>WarLib - a jar referenced on the classpath of War</li>
 * <li>WarWebinfLib - a jar included in the WEB-INF/lib directory of War</li>
 * <li>AppClientLib - a jar referenced on the classpath of AppClient</li>
 * <li>EjbWarLib - a jar referenced on the classpath of Ejb and War</li>
 * <li>EjbAppClientLib - a jar referenced on the classpath of Ejb and AppClient</li>
 * <li>WarAppClientLib - a jar referenced on the classpath of War and AppClient</li>
 * <li>EarLib - a jar in the /lib directory of the ear</li>
 * <li>NonLib - a jar in the root of the ear, not referenced from anywhere</li>
 * <li>EjbAsEjbLib - an EJB jar also referenced on the classpath of Ejb</li>
 * <li>EjbAsWarLib - an EJB jar also referenced on the classpath of War</li>
 * <li>EjbAsAppClientLib - an EJB jar also referenced on the classpath of AppClient</li>
 * <li>AppClientAsEjbLib - an application client jar also referenced on the classpath of Ejb</li>
 * <li>AppClientAsWarLib - an application client jar also referenced on the classpath of War</li>
 * <li>AppClientAsAppClientLib - an application client jar also referenced on the classpath of AppClient</li>
 * <li>War2 - another WAR, which does not reference anything else</li>
 * <li>CommonLib - a shared library referenced with commonLibraryRef</li>
 * <li>PrivateLib - a shared library referenced with privateLibraryRef</li>
 * <li>RuntimeExtRegular - a regular runtime extension which can't see application beans</li>
 * <li>RuntimeExtSeeApp - a runtime extension configured so that it can see application beans</li>
 * </ul>
 * <p>
 * The test is conducted by going through a servlet or application client main class, providing the location from which to test visibility. This class will load a
 * TestingBean from the requested location and call its doTest() method which will report which of the TargetBeans are accessible.
 * <p>
 * Each row of the visibility report has a bean location and the number of beans in that location that are visible, separated by a tab character.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class VisTest extends FATServletClient {

    public static final String SERVER_NAME = "visTestServer";
    public static final String CLIENT_NAME = "visTestClient";

    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE10, EERepeatActions.EE11, EERepeatActions.EE8, EERepeatActions.EE7);

    @Server(SERVER_NAME)
    public static LibertyServer server;
    public static LibertyClient client = LibertyClientFactory.getLibertyClient(CLIENT_NAME);

    @BeforeClass
    public static void setUp() throws Exception {

        JavaArchive visTestWarWebinfLib = ShrinkWrap.create(JavaArchive.class, "visTestWarWebinfLib.jar")
                                                    .addClass(WarWebinfLibTargetBean.class)
                                                    .addClass(WarWebinfLibTestingBean.class)
                                                    .addAsManifestResource(WarWebinfLibTestingBean.class.getResource("beans.xml"), "beans.xml");

        WebArchive visTestWar = ShrinkWrap.create(WebArchive.class, "visTestWar.war")
                                          .addClass(WarTargetBean.class)
                                          .addClass(VisibilityTestServlet.class)
                                          .addClass(WarTestingBean.class)
                                          .addAsManifestResource(WarTestingBean.class.getResource("MANIFEST.MF"), "MANIFEST.MF")
                                          .addAsWebInfResource(WarTestingBean.class.getResource("beans.xml"), "beans.xml")
                                          .addAsLibrary(visTestWarWebinfLib);

        JavaArchive visTestEjb = ShrinkWrap.create(JavaArchive.class, "visTestEjb.jar")
                                           .addClass(com.ibm.ws.cdi.visibility.tests.vistest.ejb.dummy.DummySessionBean.class)
                                           .addClass(EjbTargetBean.class)
                                           .addClass(EjbTestingBean.class)
                                           .addAsManifestResource(EjbTestingBean.class.getResource("MANIFEST.MF"), "MANIFEST.MF")
                                           .addAsManifestResource(EjbTestingBean.class.getResource("beans.xml"), "beans.xml");

        JavaArchive visTestAppClient = ShrinkWrap.create(JavaArchive.class, "visTestAppClient.jar")
                                                 .addClass(Main.class)
                                                 .addClass(AppClientTargetBean.class)
                                                 .addClass(AppClientTestingBean.class)
                                                 .addAsManifestResource(AppClientTestingBean.class.getResource("MANIFEST.MF"), "MANIFEST.MF")
                                                 .addAsManifestResource(AppClientTestingBean.class.getResource("beans.xml"), "beans.xml");

        JavaArchive visTestEjbAsEjbLib = ShrinkWrap.create(JavaArchive.class, "visTestEjbAsEjbLib.jar")
                                                   .addClass(com.ibm.ws.cdi.visibility.tests.vistest.ejbAsEjbLib.dummy.DummySessionBean.class)
                                                   .addClass(EjbAsEjbLibTestingBean.class)
                                                   .addClass(EjbAsEjbLibTargetBean.class)
                                                   .addAsManifestResource(EjbAsEjbLibTestingBean.class.getResource("beans.xml"), "beans.xml");

        JavaArchive visTestEjbAsWarLib = ShrinkWrap.create(JavaArchive.class, "visTestEjbAsWarLib.jar")
                                                   .addClass(com.ibm.ws.cdi.visibility.tests.vistest.ejbAsWarLib.dummy.DummySessionBean.class)
                                                   .addClass(EjbAsWarLibTestingBean.class)
                                                   .addClass(EjbAsWarLibTargetBean.class)
                                                   .addAsManifestResource(EjbAsWarLibTestingBean.class.getResource("beans.xml"), "beans.xml");

        JavaArchive visTestEjbAsAppClientLib = ShrinkWrap.create(JavaArchive.class, "visTestEjbAsAppClientLib.jar")
                                                         .addClass(com.ibm.ws.cdi.visibility.tests.vistest.ejbAsAppClientLib.dummy.DummySessionBean.class)
                                                         .addClass(EjbAsAppClientLibTestingBean.class)
                                                         .addClass(EjbAsAppClientLibTargetBean.class)
                                                         .addAsManifestResource(EjbAsAppClientLibTestingBean.class.getResource("beans.xml"), "beans.xml");

        JavaArchive visTestAppClientAsEjbLib = ShrinkWrap.create(JavaArchive.class, "visTestAppClientAsEjbLib.jar")
                                                         .addClass(com.ibm.ws.cdi.visibility.tests.vistest.appClientAsEjbLib.dummy.DummyMain.class)
                                                         .addClass(AppClientAsEjbLibTestingBean.class)
                                                         .addClass(AppClientAsEjbLibTargetBean.class)
                                                         .addAsManifestResource(AppClientAsEjbLibTestingBean.class.getResource("MANIFEST.MF"), "MANIFEST.MF")
                                                         .addAsManifestResource(AppClientAsEjbLibTestingBean.class.getResource("beans.xml"), "beans.xml");

        JavaArchive visTestAppClientAsWarLib = ShrinkWrap.create(JavaArchive.class, "visTestAppClientAsWarLib.jar")
                                                         .addClass(com.ibm.ws.cdi.visibility.tests.vistest.appClientAsWarLib.dummy.DummyMain.class)
                                                         .addClass(AppClientAsWarLibTargetBean.class)
                                                         .addClass(AppClientAsWarLibTestingBean.class)
                                                         .addAsManifestResource(AppClientAsWarLibTestingBean.class.getResource("MANIFEST.MF"), "MANIFEST.MF")
                                                         .addAsManifestResource(AppClientAsWarLibTestingBean.class.getResource("beans.xml"), "beans.xml");

        JavaArchive visTestAppClientAsAppClientLib = ShrinkWrap.create(JavaArchive.class, "visTestAppClientAsAppClientLib.jar")
                                                               .addClass(com.ibm.ws.cdi.visibility.tests.vistest.appClientAsAppClientLib.dummy.DummyMain.class)
                                                               .addClass(AppClientAsAppClientLibTargetBean.class)
                                                               .addClass(AppClientAsAppClientLibTestingBean.class)
                                                               .addAsManifestResource(AppClientAsAppClientLibTestingBean.class.getResource("MANIFEST.MF"), "MANIFEST.MF")
                                                               .addAsManifestResource(AppClientAsAppClientLibTestingBean.class.getResource("beans.xml"), "beans.xml");

        WebArchive visTestWar2 = ShrinkWrap.create(WebArchive.class, "visTestWar2.war")
                                           .addClass(War2TargetBean.class)
                                           .addClass(War2TestingBean.class)
                                           .addClass(com.ibm.ws.cdi.visibility.tests.vistest.war2.servlet.VisibilityTestServlet.class)
                                           .addAsWebInfResource(War2TestingBean.class.getResource("beans.xml"), "beans.xml");

        JavaArchive visTestWarLib = ShrinkWrap.create(JavaArchive.class, "visTestWarLib.jar")
                                              .addClass(WarLibTestingBean.class)
                                              .addClass(WarLibTargetBean.class)
                                              .addAsManifestResource(WarLibTestingBean.class.getResource("beans.xml"), "beans.xml");

        JavaArchive visTestEjbLib = ShrinkWrap.create(JavaArchive.class, "visTestEjbLib.jar")
                                              .addClass(EjbLibTargetBean.class)
                                              .addClass(EjbLibTestingBean.class)
                                              .addAsManifestResource(EarLibTestingBean.class.getResource("beans.xml"), "beans.xml");

        JavaArchive visTestAppClientLib = ShrinkWrap.create(JavaArchive.class, "visTestAppClientLib.jar")
                                                    .addClass(AppClientLibTargetBean.class)
                                                    .addClass(AppClientLibTestingBean.class)
                                                    .addAsManifestResource(AppClientLibTestingBean.class.getResource("beans.xml"), "beans.xml");

        JavaArchive visTestEjbWarLib = ShrinkWrap.create(JavaArchive.class, "visTestEjbWarLib.jar")
                                                 .addClass(EjbWarLibTestingBean.class)
                                                 .addClass(EjbWarLibTargetBean.class)
                                                 .addAsManifestResource(EjbWarLibTestingBean.class.getResource("beans.xml"), "beans.xml");

        JavaArchive visTestEjbAppClientLib = ShrinkWrap.create(JavaArchive.class, "visTestEjbAppClientLib.jar")
                                                       .addClass(EjbAppClientLibTestingBean.class)
                                                       .addClass(EjbAppClientLibTargetBean.class)
                                                       .addAsManifestResource(EjbAppClientLibTestingBean.class.getResource("beans.xml"), "beans.xml");

        JavaArchive visTestWarAppClientLib = ShrinkWrap.create(JavaArchive.class, "visTestWarAppClientLib.jar")
                                                       .addClass(WarAppClientLibTestingBean.class)
                                                       .addClass(WarAppClientLibTargetBean.class)
                                                       .addAsManifestResource(WarAppClientLibTestingBean.class.getResource("beans.xml"), "beans.xml");

        JavaArchive visTestNonLib = ShrinkWrap.create(JavaArchive.class, "visTestNonLib.jar")
                                              .addClass(NonLibTestingBean.class)
                                              .addClass(NonLibTargetBean.class)
                                              .addAsManifestResource(NonLibTestingBean.class.getResource("beans.xml"), "beans.xml");

        JavaArchive visTestEarLib = ShrinkWrap.create(JavaArchive.class, "visTestEarLib.jar")
                                              .addClass(EarLibTargetBean.class)
                                              .addClass(EarLibTestingBean.class)
                                              .addAsManifestResource(EarLibTestingBean.class.getResource("beans.xml"), "beans.xml");

        EnterpriseArchive visTest = ShrinkWrap.create(EnterpriseArchive.class, "visTest.ear")
                                              .addAsModule(visTestWar)
                                              .addAsModule(visTestEjb)
                                              .addAsModule(visTestAppClient)
                                              .addAsModule(visTestEjbAsEjbLib)
                                              .addAsModule(visTestEjbAsWarLib)
                                              .addAsModule(visTestEjbAsAppClientLib)
                                              .addAsModule(visTestAppClientAsEjbLib)
                                              .addAsModule(visTestAppClientAsWarLib)
                                              .addAsModule(visTestAppClientAsAppClientLib)
                                              .addAsModule(visTestWar2)
                                              .addAsModule(visTestWarLib)
                                              .addAsModule(visTestEjbLib)
                                              .addAsModule(visTestAppClientLib)
                                              .addAsModule(visTestEjbWarLib)
                                              .addAsModule(visTestEjbAppClientLib)
                                              .addAsModule(visTestWarAppClientLib)
                                              .addAsModule(visTestNonLib)
                                              .addAsLibrary(visTestEarLib)
                                              .addAsManifestResource(VisTest.class.getResource("permissions.xml"), "permissions.xml");

        JavaArchive visTestCommonLibrary = ShrinkWrap.create(JavaArchive.class, "visTestCommonLib.jar")
                                                     .addClass(CommonLibTargetBean.class)
                                                     .addClass(CommonLibTestingBean.class)
                                                     .addAsManifestResource(CommonLibTestingBean.class.getResource("beans.xml"), "beans.xml");

        JavaArchive visTestPrivateLibrary = ShrinkWrap.create(JavaArchive.class, "visTestPrivateLib.jar")
                                                      .addClass(PrivateLibTargetBean.class)
                                                      .addClass(PrivateLibTestingBean.class)
                                                      .addAsManifestResource(PrivateLibTestingBean.class.getResource("beans.xml"), "beans.xml");

        ShrinkHelper.exportAppToServer(server, visTest, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportToServer(server, "/", visTestPrivateLibrary, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportToServer(server, "/", visTestCommonLibrary, DeployOptions.SERVER_ONLY);

        ShrinkHelper.exportAppToClient(client, visTest, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportToClient(client, "/", visTestPrivateLibrary, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportToClient(client, "/", visTestCommonLibrary, DeployOptions.SERVER_ONLY);

        server.installSystemBundle("visTest.framework");
        server.installSystemBundle("visTest.framework-jakarta");
        server.installSystemBundle("visTest.runtimeExtRegular");
        server.installSystemBundle("visTest.runtimeExtRegular-jakarta");
        server.installSystemBundle("visTest.runtimeExtSeeApp");
        server.installSystemBundle("visTest.runtimeExtSeeApp-jakarta");
        server.installSystemFeature("visTest-1.2");
        server.installSystemFeature("visTest-3.0");

        server.startServer();
        getAppClientResults();
    }

    public static Logger LOG = Logger.getLogger(VisTest.class.getName());

    /**
     * Enumeration of locations of target beans
     * <p>
     * See class documentation for details of each location
     */
    private enum Location {
        InEjb,
        InWar,
        InAppClient,
        InEjbLib,
        InWarLib,
        InWarWebinfLib,
        InAppClientLib,
        InEjbWarLib,
        InEjbAppClientLib,
        InWarAppClientLib,
        InEarLib,
        InNonLib,
        InEjbAsEjbLib,
        InEjbAsWarLib,
        InEjbAsAppClientLib,
        InAppClientAsEjbLib,
        InAppClientAsWarLib,
        InAppClientAsAppClientLib,
        InWar2,
        InCommonLib,
        InPrivateLib,
        InRuntimeExtRegular,
        InRuntimeExtSeeApp,
    }

    /**
     * Set of locations that should be visible from EJBs and their libraries
     */
    Set<Location> EJB_VISIBLE_LOCATIONS = new HashSet<Location>(Arrays.asList(Location.InEjb,
                                                                              Location.InEjbLib,
                                                                              Location.InEjbWarLib,
                                                                              Location.InEjbAppClientLib,
                                                                              Location.InEarLib,
                                                                              Location.InEjbAsEjbLib,
                                                                              Location.InEjbAsWarLib,
                                                                              Location.InEjbAsAppClientLib,
                                                                              Location.InAppClientAsEjbLib,
                                                                              Location.InCommonLib,
                                                                              Location.InPrivateLib,
                                                                              Location.InRuntimeExtRegular,
                                                                              Location.InRuntimeExtSeeApp));

    /**
     * Set of locations that should be visible from WARs and their libraries
     */
    Set<Location> WAR_VISIBLE_LOCATIONS = new HashSet<Location>(Arrays.asList(Location.InEjb,
                                                                              Location.InWar,
                                                                              Location.InEjbLib,
                                                                              Location.InWarLib,
                                                                              Location.InWarWebinfLib,
                                                                              Location.InEjbWarLib,
                                                                              Location.InEjbAppClientLib,
                                                                              Location.InWarAppClientLib,
                                                                              Location.InEarLib,
                                                                              Location.InEjbAsEjbLib,
                                                                              Location.InEjbAsWarLib,
                                                                              Location.InEjbAsAppClientLib,
                                                                              Location.InAppClientAsEjbLib,
                                                                              Location.InAppClientAsWarLib,
                                                                              Location.InCommonLib,
                                                                              Location.InPrivateLib,
                                                                              Location.InRuntimeExtRegular,
                                                                              Location.InRuntimeExtSeeApp));

    /**
     * Set of locations that should be visible from app clients and their libraries
     */
    Set<Location> APP_CLIENT_VISIBLE_LOCATIONS = new HashSet<Location>(Arrays.asList(Location.InAppClient,
                                                                                     Location.InAppClientLib,
                                                                                     Location.InEjbAppClientLib,
                                                                                     Location.InWarAppClientLib,
                                                                                     Location.InEarLib,
                                                                                     Location.InEjbAsAppClientLib,
                                                                                     Location.InAppClientAsAppClientLib,
                                                                                     Location.InCommonLib,
                                                                                     Location.InPrivateLib,
                                                                                     Location.InRuntimeExtRegular,
                                                                                     Location.InRuntimeExtSeeApp));

    /**
     * Set of locations that should be visible from common server libraries
     */
    Set<Location> COMMON_LIB_VISIBLE_LOCATIONS = new HashSet<Location>(Arrays.asList(Location.InCommonLib,
                                                                                     Location.InRuntimeExtRegular,
                                                                                     Location.InRuntimeExtSeeApp));

    /**
     * Set of locations that should be visible from regular runtime extensions
     */
    Set<Location> RUNTIME_EXT_REGULAR_VISIBLE_LOCATIONS = new HashSet<Location>(Arrays.asList(Location.InRuntimeExtRegular));

    /**
     * Set of locations that should be visible from runtime extensions configured to see application beans
     */
    Set<Location> RUNTIME_EXT_SEE_ALL_VISIBLE_LOCATIONS = new HashSet<Location>(Arrays.asList(Location.InEjb,
                                                                                              Location.InWar,
                                                                                              Location.InEjbLib,
                                                                                              Location.InWarLib,
                                                                                              Location.InWarWebinfLib,
                                                                                              Location.InEjbWarLib,
                                                                                              Location.InEjbAppClientLib,
                                                                                              Location.InWarAppClientLib,
                                                                                              Location.InEarLib,
                                                                                              Location.InEjbAsEjbLib,
                                                                                              Location.InEjbAsWarLib,
                                                                                              Location.InEjbAsAppClientLib,
                                                                                              Location.InAppClientAsEjbLib,
                                                                                              Location.InAppClientAsWarLib,
                                                                                              Location.InCommonLib,
                                                                                              Location.InPrivateLib,
                                                                                              Location.InRuntimeExtRegular,
                                                                                              Location.InRuntimeExtSeeApp,
                                                                                              Location.InWar2));

    /**
     * Set of locations that should be visible from runtime extensions configured to see application beans when running as a client
     * <p>
     * This is a different list than on the server for two reasons:
     * <ul>
     * <li>EJB and Web modules are not started on the client, so they can't be visible
     * <li>even when configured to see application beans, runtime extensions can't see application client module beans
     * </ul>
     */
    Set<Location> RUNTIME_EXT_SEE_ALL_CLIENT_VISIBLE_LOCATIONS = new HashSet<Location>(Arrays.asList(Location.InEarLib,
                                                                                                     Location.InCommonLib,
                                                                                                     Location.InPrivateLib,
                                                                                                     Location.InRuntimeExtRegular,
                                                                                                     Location.InRuntimeExtSeeApp));

    private static Map<Location, String> appClientResults = null;

    /**
     * Run the app client and store the results in the appClientResults map
     * <p>
     * One invocation of the app client returns all the relevant results because there's a significant overhead to starting up the client.
     *
     * @throws Exception
     */
    public static void getAppClientResults() throws Exception {

        appClientResults = new HashMap<Location, String>();

        ProgramOutput output = client.startClient();

        LOG.info("GOT THE CLIENT OUTPUT");

        if (output.getReturnCode() != 0) {
            LOG.severe("BAD RETURN CODE");
            throw new Exception("Client returned error: " + output.getReturnCode() + "\nStdout:\n" + output.getStdout() + "\nStderr:\n" + output.getStderr());
        }

        String[] resultSets = output.getStdout().split("----[\r\n]+");

        // Read the result set, skipping the initial section which is the startup messages and the final section which is the shutdown messages
        for (int i = 1; i < resultSets.length - 1; i++) {
            String resultSet = resultSets[i];
            try {
                String[] results = resultSet.split("[\r\n]+", 2);
                Location location = Location.valueOf(results[0]);
                appClientResults.put(location, results[1]);
            } catch (Throwable ex) {
                LOG.warning("FAILED TO PARSE A CLIENT LINE: " + resultSet);
            }
        }
    }

    @Test
    public void testVisibilityFromEjb() throws Exception {
        doTestWithServlet(Location.InEjb, EJB_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromWar() throws Exception {
        doTestWithServlet(Location.InWar, WAR_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromAppClient() throws Exception {
        doTestWithAppClient(Location.InAppClient, APP_CLIENT_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromEjbLib() throws Exception {
        doTestWithServlet(Location.InEjbLib, EJB_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromWarLib() throws Exception {
        doTestWithServlet(Location.InWarLib, WAR_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromWarWebinfLib() throws Exception {
        doTestWithServlet(Location.InWarWebinfLib, WAR_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromAppClientLib() throws Exception {
        doTestWithAppClient(Location.InAppClientLib, APP_CLIENT_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromEjbWarLib() throws Exception {
        doTestWithServlet(Location.InEjbWarLib, EJB_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromEjbAppClientLib() throws Exception {
        doTestWithServlet(Location.InEjbAppClientLib, EJB_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromWarAppClientLib() throws Exception {
        doTestWithServlet(Location.InWarAppClientLib, WAR_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromEarLib() throws Exception {
        doTestWithServlet(Location.InEarLib, EJB_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromEjbAsEjbLib() throws Exception {
        doTestWithServlet(Location.InEjbAsEjbLib, EJB_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromEjbAsWarLib() throws Exception {
        doTestWithServlet(Location.InEjbAsWarLib, EJB_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromEjbAsAppClientLib() throws Exception {
        doTestWithServlet(Location.InEjbAsAppClientLib, EJB_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromAppClientAsEjbLib() throws Exception {
        doTestWithServlet(Location.InAppClientAsEjbLib, EJB_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromAppClientAsWarLib() throws Exception {
        doTestWithServlet(Location.InAppClientAsWarLib, WAR_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromAppClientAsAppClientLib() throws Exception {
        doTestWithAppClient(Location.InAppClientAsAppClientLib, APP_CLIENT_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromCommonLib() throws Exception {
        doTestWithServlet(Location.InCommonLib, COMMON_LIB_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromPrivateLib() throws Exception {
        doTestWithServlet(Location.InPrivateLib, EJB_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromRuntimeExtRegular() throws Exception {
        doTestWithServlet(Location.InRuntimeExtRegular, RUNTIME_EXT_REGULAR_VISIBLE_LOCATIONS);
        doTestWithAppClient(Location.InRuntimeExtRegular, RUNTIME_EXT_REGULAR_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromRuntimeExtSeeAll() throws Exception {
        doTestWithServlet(Location.InRuntimeExtSeeApp, RUNTIME_EXT_SEE_ALL_VISIBLE_LOCATIONS);
        doTestWithAppClient(Location.InRuntimeExtSeeApp, RUNTIME_EXT_SEE_ALL_CLIENT_VISIBLE_LOCATIONS);
    }

    /**
     * Retrieves the visibility of beans from a given location by requesting the information from a servlet. Then checks the result and fails the test if it does not match the
     * expected set of visible locations
     *
     * @param location the location to test visibility from
     * @param visibleLocations the locations which should be visible
     * @throws Exception if there is an error requesting the visibility information or parsing the result.
     */
    private void doTestWithServlet(Location location, Set<Location> visibleLocations) throws Exception {
        String response = HttpUtils.getHttpResponseAsString(server, "/visTestWar/?location=" + location);

        checkResult(response, visibleLocations);
    }

    /**
     * Retrieves the visibility of beans from a given location by looking at the output of the app client. Checks the result and fails the test if it does not match the expected
     * set of visible locations
     *
     * @param location the location to test visibility from
     * @param visibleLocations the locations which should be visible
     * @throws Exception if there is an error requesting the visibility information or parsing the result.
     */
    private void doTestWithAppClient(Location location, Set<Location> visibleLocations) throws Exception {
        String resultString = appClientResults.get(location);
        if (resultString == null) {
            throw new Exception("Client output did not include results for " + location);
        }

        checkResult(resultString, visibleLocations);
    }

    /**
     * Checks that the given result string indicates that only a given list of locations are visible.
     * <p>
     * Fails the test if the parsed result does not match the set of visible locations.
     *
     * @param resultString the result string
     * @param visibleLocations the locations that should be reported as visible in the result string
     * @throws Exception if there is an error parsing the resultString
     */
    private void checkResult(String resultString, Set<Location> visibleLocations) throws Exception {

        Map<Location, Integer> results = parseResult(resultString);

        List<String> errors = new ArrayList<String>();
        for (Location location : Location.values()) {
            Integer count = results.get(location);

            if (count == null) {
                errors.add("No result returned for " + location);
                continue;
            }

            if (count < 0) {
                errors.add("Invalid result for " + location + ": " + count);
                continue;
            }

            if (count > 1) {
                errors.add(count + " instances of bean found for " + location);
                continue;
            }

            if (visibleLocations.contains(location) && count != 1) {
                errors.add("Bean " + location + " is not accessible but should be");
                continue;
            }

            if (!visibleLocations.contains(location) && count != 0) {
                errors.add("Bean " + location + " is accessible but should not be");
                continue;
            }

            // If we get here, result is ok, don't add any errors
        }

        // If we've found any problems, return them
        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Errors found in result: \n");
            for (String error : errors) {
                sb.append(error);
                sb.append("\n");
            }
            fail(sb.toString());
        }
    }

    /**
     * Parse a result string into a map from location to number of beans visible from that location.
     *
     * @param resultString the string to parse
     * @return map from location to number of visible beans
     * @throws Exception if there is an error parsing the result
     */
    private Map<Location, Integer> parseResult(String resultString) throws Exception {
        Map<Location, Integer> results = new HashMap<Location, Integer>();

        if (resultString.startsWith("ERROR")) {
            fail("Error response received:\n" + resultString);
        }

        for (String line : resultString.split("[\r\n]+")) {

            String[] parts = line.split("\t");
            if (parts.length != 2) {
                throw parsingException(line, resultString, null);
            }

            String locationString = parts[0];
            Integer resultCount;
            try {
                resultCount = Integer.valueOf(parts[1]);
            } catch (NumberFormatException ex) {
                throw parsingException(line, resultString, ex);
            }

            Location resultLocation;
            try {
                resultLocation = Location.valueOf(locationString);
            } catch (IllegalArgumentException ex) {
                // Additional result we don't care about
                continue;
            }

            results.put(resultLocation, resultCount);
        }

        return results;
    }

    private Exception parsingException(String line, String response, Throwable cause) {
        return new Exception("Badly formed line: " + line + "\n\nWhole response:\n" + response, cause);
    }

    @AfterClass
    public static void afterClass() throws Exception {

        //We put this in an AutoCloseable because try-with-resource will order the exceptions correctly.
        //That means an exception from stopServer will by the primary exception, and any errors from
        //uninstallSystemFeature will be recorded as suppressed exceptions.
        AutoCloseable uninstallFeatures = () -> {
            server.uninstallSystemFeature("visTest-1.2");
            server.uninstallSystemFeature("visTest-3.0");
        };

        try (AutoCloseable c = uninstallFeatures) {
            if (server != null) {
                server.stopServer();
            }
        }
    }
}
