/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.annotations.fat.tests;

import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE7;
import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE9;

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
import com.ibm.ws.cdi.annotations.fat.apps.defaultDecorator.DefaultDecoratorServlet;
import com.ibm.ws.cdi.annotations.fat.apps.dependentScopedProducer.AppScopedMethodServlet;
import com.ibm.ws.cdi.annotations.fat.apps.dependentScopedProducer.AppScopedSteryotypedServlet;
import com.ibm.ws.cdi.annotations.fat.apps.dependentScopedProducer.NullProducerServlet;
import com.ibm.ws.cdi.annotations.fat.apps.globalPriority.GlobalPriorityTestServlet;
import com.ibm.ws.cdi.annotations.fat.apps.globalPriority.lib.JarBean;
import com.ibm.ws.cdi.annotations.fat.apps.utils.ChainableListImpl;
import com.ibm.ws.cdi.annotations.fat.apps.withAnnotations.WithAnnotationsExtension;
import com.ibm.ws.cdi.annotations.fat.apps.withAnnotations.WithAnnotationsServlet;

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
                                           .addPackages(true, AppScopedSteryotypedServlet.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, depProducer, DeployOptions.SERVER_ONLY);

        if (TestModeFilter.shouldRun(TestMode.FULL)) {
            WebArchive defaultDecorator = ShrinkWrap.create(WebArchive.class, DEFAULT_DECORATOR_APP_NAME + ".war")
                                                    .addPackage(DefaultDecoratorServlet.class.getPackage());
            CDIArchiveHelper.addBeansXML(defaultDecorator, DefaultDecoratorServlet.class);

            JavaArchive globalPriorityLib = ShrinkWrap.create(JavaArchive.class, "globalPriorityLib.jar")
                                                      .addPackage(JarBean.class.getPackage());
            CDIArchiveHelper.addBeansXML(globalPriorityLib, JarBean.class);

            JavaArchive utilLib = ShrinkWrap.create(JavaArchive.class, "utilLib.jar")
                                            .addPackage(ChainableListImpl.class.getPackage());
            CDIArchiveHelper.addEmptyBeansXML(utilLib);

            WebArchive globalPriorityWebApp = ShrinkWrap.create(WebArchive.class, GLOBAL_PRIORITY_APP_NAME + ".war")
                                                        .addPackage(GlobalPriorityTestServlet.class.getPackage());
            CDIArchiveHelper.addEmptyBeansXML(globalPriorityWebApp);

            EnterpriseArchive globalPriorityApp = ShrinkWrap.create(EnterpriseArchive.class, "globalPriorityApp.ear")
                                                            .addAsLibrary(globalPriorityLib)
                                                            .addAsLibrary(utilLib)
                                                            .addAsModule(globalPriorityWebApp);

            WebArchive withAnnotationsApp = ShrinkWrap.create(WebArchive.class, WITH_ANNOTATIONS_APP_NAME + ".war")
                                                      .addPackage(WithAnnotationsServlet.class.getPackage())
                                                      .addAsLibrary(utilLib);
            CDIArchiveHelper.addEmptyBeansXML(withAnnotationsApp);
            CDIArchiveHelper.addCDIExtensionService(withAnnotationsApp, WithAnnotationsExtension.class);

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
