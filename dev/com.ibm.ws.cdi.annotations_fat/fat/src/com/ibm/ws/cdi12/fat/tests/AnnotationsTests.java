/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
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
import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE9;

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
import com.ibm.ws.cdi.test.dependentscopedproducer.DependentSterotype;
import com.ibm.ws.cdi.test.dependentscopedproducer.servlets.AppScopedMethodServlet;
import com.ibm.ws.cdi.test.dependentscopedproducer.servlets.AppScopedSteryotypedServlet;
import com.ibm.ws.cdi.test.dependentscopedproducer.servlets.NullProducerServlet;
import com.ibm.ws.cdi12.test.defaultdecorator.DefaultDecoratorServlet;
import com.ibm.ws.cdi12.test.priority.lib.JarBean;
import com.ibm.ws.cdi12.test.priority.web.GlobalPriorityTestServlet;
import com.ibm.ws.cdi12.test.priority.web.NoPriorityBean;
import com.ibm.ws.cdi12.test.utils.ChainableListImpl;
import com.ibm.ws.cdi12.test.withAnnotations.WithAnnotationsServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.rules.repeater.EERepeatTests;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Tests for <code>@WithAnnotations</code> used in portable extensions for observing type discovery of beans with certain annotations.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class AnnotationsTests extends FATServletClient {

    public static final String SERVER_NAME = "cdi12BasicServer";

    public static final String DEP_PRODUCER_APP_NAME = "DepProducerApp";
    public static final String DEFAULT_DECORATOR_APP_NAME = "defaultDecoratorApp";
    public static final String GLOBAL_PRIORITY_APP_NAME = "globalPriorityWebApp";
    public static final String WITH_ANNOTATIONS_APP_NAME = "withAnnotationsApp";

    @ClassRule
    public static RepeatTests r = EERepeatTests.with(SERVER_NAME, EE9, EE7); //not bothering to repeat with EE8 ... the EE9 version is mostly a transformed version of the EE8 code

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = AppScopedSteryotypedServlet.class, contextRoot = DEP_PRODUCER_APP_NAME), //LITE
                    @TestServlet(servlet = AppScopedMethodServlet.class, contextRoot = DEP_PRODUCER_APP_NAME), //FULL
                    @TestServlet(servlet = NullProducerServlet.class, contextRoot = DEP_PRODUCER_APP_NAME), //FULL
                    @TestServlet(servlet = DefaultDecoratorServlet.class, contextRoot = DEFAULT_DECORATOR_APP_NAME), //FULL
                    @TestServlet(servlet = GlobalPriorityTestServlet.class, contextRoot = GLOBAL_PRIORITY_APP_NAME), //FULL
                    @TestServlet(servlet = WithAnnotationsServlet.class, contextRoot = WITH_ANNOTATIONS_APP_NAME) }) //FULL
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive depProducer = ShrinkWrap.create(WebArchive.class, DEP_PRODUCER_APP_NAME + ".war")
                                           .addPackages(true, DependentSterotype.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, depProducer, DeployOptions.SERVER_ONLY);

        if (TestModeFilter.shouldRun(TestMode.FULL)) {
            WebArchive defaultDecorator = ShrinkWrap.create(WebArchive.class, DEFAULT_DECORATOR_APP_NAME + ".war")
                                                    .addPackage(DefaultDecoratorServlet.class.getPackage())
                                                    .add(new FileAsset(new File("test-applications/defaultDecoratorApp.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");

            JavaArchive globalPriorityLib = ShrinkWrap.create(JavaArchive.class, "globalPriorityLib.jar")
                                                      .addPackage(JarBean.class.getPackage())
                                                      .add(new FileAsset(new File("test-applications/globalPriorityLib.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");

            JavaArchive utilLib = ShrinkWrap.create(JavaArchive.class, "utilLib.jar")
                                            .addPackage(ChainableListImpl.class.getPackage())
                                            .add(new FileAsset(new File("test-applications/utilLib.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");

            WebArchive globalPriorityWebApp = ShrinkWrap.create(WebArchive.class, GLOBAL_PRIORITY_APP_NAME + ".war")
                                                        .addPackage(NoPriorityBean.class.getPackage())
                                                        .add(new FileAsset(new File("test-applications/globalPriorityWebApp.war/resources/WEB-INF/beans.xml")),
                                                             "/WEB-INF/beans.xml");

            EnterpriseArchive globalPriorityApp = ShrinkWrap.create(EnterpriseArchive.class, "globalPriorityApp.ear")
                                                            .add(new FileAsset(new File("test-applications/globalPriorityApp.ear/resources/META-INF/application.xml")),
                                                                 "/META-INF/application.xml")
                                                            .addAsLibrary(globalPriorityLib)
                                                            .addAsLibrary(utilLib)
                                                            .addAsModule(globalPriorityWebApp);

            WebArchive withAnnotationsApp = ShrinkWrap.create(WebArchive.class, WITH_ANNOTATIONS_APP_NAME + ".war")
                                                      .addPackage(WithAnnotationsServlet.class.getPackage())
                                                      .add(new FileAsset(new File("test-applications/withAnnotationsApp.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                                                      .add(new FileAsset(new File("test-applications/withAnnotationsApp.war/resources/META-INF/services/javax.enterprise.inject.spi.Extension")),
                                                           "/META-INF/services/javax.enterprise.inject.spi.Extension")
                                                      .addAsLibrary(utilLib);

            ShrinkHelper.exportDropinAppToServer(server, withAnnotationsApp, DeployOptions.SERVER_ONLY);
            ShrinkHelper.exportDropinAppToServer(server, globalPriorityApp, DeployOptions.SERVER_ONLY);
            ShrinkHelper.exportDropinAppToServer(server, defaultDecorator, DeployOptions.SERVER_ONLY);
        }

        server.startServer();

    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}
