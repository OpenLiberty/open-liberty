/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE7;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EERepeatTests;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class WarLibsAccessWarBeansTest extends FATServletClient {

    public static final String SERVER_NAME = "cdi12WarLibsAccessWarServer";

    @ClassRule
    public static RepeatTests r = EERepeatTests.with(SERVER_NAME, EE7); //This test should be merged into BasicVisibilityTests when jaxrs-3.0 (EE9) is available

    public static final String WAR_LIB_ACCESS_APP_NAME = "warLibAccessBeansInWar";

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = com.ibm.ws.cdi12.test.warLibAccessBeansInWar.TestServlet.class, contextRoot = WAR_LIB_ACCESS_APP_NAME) }) //LITE
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {

        JavaArchive warLibAccessBeansInWarLibJar = ShrinkWrap.create(JavaArchive.class, "warLibAccessBeansInWarJar.jar")
                                                             .addClass(com.ibm.ws.cdi12.test.warLibAccessBeansInWarJar.TestInjectionClass.class)
                                                             .addClass(com.ibm.ws.cdi12.test.warLibAccessBeansInWarJar.WarBeanInterface.class)
                                                             .add(new FileAsset(new File("test-applications/warLibAccessBeansInWarJar.jar/resources/WEB-INF/beans.xml")),
                                                                  "/WEB-INF/beans.xml");

        JavaArchive warLibAccessBeansInWarJar = ShrinkWrap.create(JavaArchive.class, "warLibAccessBeansInWar2.jar")
                                                          .addClass(com.ibm.ws.cdi12.test.warLibAccessBeansInWarJar2.TestInjectionClass2.class)
                                                          .addClass(com.ibm.ws.cdi12.test.warLibAccessBeansInWarJar2.WarBeanInterface2.class);

        WebArchive warLibAccessBeansInWar = ShrinkWrap.create(WebArchive.class, "warLibAccessBeansInWar.war")
                                                      .addAsManifestResource(new File("test-applications/warLibAccessBeansInWar.war/resources/META-INF/MANIFEST.MF"))
                                                      .addClass(com.ibm.ws.cdi12.test.warLibAccessBeansInWar.TestServlet.class)
                                                      .addClass(com.ibm.ws.cdi12.test.warLibAccessBeansInWar.WarBean.class)
                                                      .add(new FileAsset(new File("test-applications/warLibAccessBeansInWar.war/resources/WEB-INF/beans.xml")),
                                                           "/WEB-INF/beans.xml")
                                                      .addAsLibrary(warLibAccessBeansInWarLibJar);

        EnterpriseArchive warLibAccessBeansInWarEAR = ShrinkWrap.create(EnterpriseArchive.class, "warLibAccessBeansInWar.ear")
                                                                .add(new FileAsset(new File("test-applications/warLibAccessBeansInWar.ear/resources/META-INF/application.xml")),
                                                                     "/META-INF/application.xml")
                                                                .addAsModule(warLibAccessBeansInWar)
                                                                .addAsModule(warLibAccessBeansInWarJar);
        ShrinkHelper.exportDropinAppToServer(server, warLibAccessBeansInWarEAR, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}
