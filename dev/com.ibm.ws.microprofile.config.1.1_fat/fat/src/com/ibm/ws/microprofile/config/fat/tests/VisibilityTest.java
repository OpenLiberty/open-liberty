/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.config.fat.tests;

import static com.ibm.ws.microprofile.config.fat.tests.visibility.earlib.VisibilityTestConstants.EAR_LIB_CONFIG_PROPERTY;
import static com.ibm.ws.microprofile.config.fat.tests.visibility.earlib.VisibilityTestConstants.EAR_LIB_VALUE;
import static com.ibm.ws.microprofile.config.fat.tests.visibility.earlib.VisibilityTestConstants.EJB_JAR_CONFIG_PROPERTY;
import static com.ibm.ws.microprofile.config.fat.tests.visibility.earlib.VisibilityTestConstants.EJB_JAR_VALUE;
import static com.ibm.ws.microprofile.config.fat.tests.visibility.earlib.VisibilityTestConstants.WAR_CONFIG_PROPERTY;
import static com.ibm.ws.microprofile.config.fat.tests.visibility.earlib.VisibilityTestConstants.WAR_VALUE;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.microprofile.config.fat.suite.SharedShrinkWrapApps;
import com.ibm.ws.microprofile.config.fat.tests.visibility.earlib.EarLibBean;
import com.ibm.ws.microprofile.config.fat.tests.visibility.ejbjar.VisibilityTestEjb;
import com.ibm.ws.microprofile.config.fat.tests.visibility.war.VisibilityTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.config.fat.repeat.ConfigRepeatActions;

/**
 * Tests which config sources can be seen from different locations within an .ear
 */
@RunWith(FATRunner.class)
public class VisibilityTest extends FATServletClient {

    public static final String SERVER_NAME = "VisibilityServer";
    public static final String APP_NAME = "VisibilityTest";

    @Server(SERVER_NAME)
    @TestServlet(servlet = VisibilityTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = ConfigRepeatActions.repeatDefault(SERVER_NAME);

    @BeforeClass
    public static void setUp() throws Exception {
        JavaArchive testAppUtils = SharedShrinkWrapApps.getTestAppUtilsJar();

        PropertiesAsset warConfig = new PropertiesAsset()
                                                         .addProperty(WAR_CONFIG_PROPERTY, WAR_VALUE);
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(VisibilityTestServlet.class.getPackage())
                                   .addAsResource(warConfig, "/META-INF/microprofile-config.properties");

        PropertiesAsset ejbJarConfig = new PropertiesAsset()
                                                            .addProperty(EJB_JAR_CONFIG_PROPERTY, EJB_JAR_VALUE);
        JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, APP_NAME + "ejb.jar")
                                       .addPackage(VisibilityTestEjb.class.getPackage())
                                       .addAsResource(ejbJarConfig, "/META-INF/microprofile-config.properties");

        PropertiesAsset earLibConfig = new PropertiesAsset()
                                                            .addProperty(EAR_LIB_CONFIG_PROPERTY, EAR_LIB_VALUE);
        JavaArchive earLibJar = ShrinkWrap.create(JavaArchive.class, APP_NAME + "-earlib.jar")
                                          .addPackage(EarLibBean.class.getPackage())
                                          .addAsResource(earLibConfig, "/META-INF/microprofile-config.properties");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear")
                                          .addAsModule(war)
                                          .addAsModule(ejbJar)
                                          .addAsLibrary(earLibJar)
                                          .addAsLibrary(testAppUtils);

        ShrinkHelper.exportDropinAppToServer(server, ear, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}
