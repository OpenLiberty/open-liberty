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

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi.test.dependentscopedproducer.servlets.AppScopedMethodServlet;
import com.ibm.ws.cdi.test.dependentscopedproducer.servlets.AppScopedSteryotypedServlet;
import com.ibm.ws.cdi.test.dependentscopedproducer.servlets.NullProducerServlet;
import com.ibm.ws.cdi12.test.defaultdecorator.DefaultDecoratorServlet;
import com.ibm.ws.cdi12.test.priority.GlobalPriorityTestServlet;
import com.ibm.ws.cdi12.test.withAnnotations.WithAnnotationsServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Tests for <code>@WithAnnotations</code> used in portable extensions for observing type discovery of beans with certain annotations.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class AnnotationsTests extends FATServletClient {

    public static final String DEP_PRODUCER_APP_NAME = "DepProducerApp";
    public static final String DEFAULT_DECORATOR_APP_NAME = "defaultDecoratorApp";
    public static final String GLOBAL_PRIORITY_APP_NAME = "globalPriorityWebApp";
    public static final String WITH_ANNOTATIONS_APP_NAME = "withAnnotationsApp";

    @Server("cdi12BasicServer")
    @TestServlets({
                    @TestServlet(servlet = AppScopedSteryotypedServlet.class, contextRoot = DEP_PRODUCER_APP_NAME),
                    @TestServlet(servlet = AppScopedMethodServlet.class, contextRoot = DEP_PRODUCER_APP_NAME),
                    @TestServlet(servlet = NullProducerServlet.class, contextRoot = DEP_PRODUCER_APP_NAME),
                    @TestServlet(servlet = DefaultDecoratorServlet.class, contextRoot = DEFAULT_DECORATOR_APP_NAME),
                    @TestServlet(servlet = GlobalPriorityTestServlet.class, contextRoot = GLOBAL_PRIORITY_APP_NAME),
                    @TestServlet(servlet = WithAnnotationsServlet.class, contextRoot = WITH_ANNOTATIONS_APP_NAME) })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive depProducer = ShrinkWrap.create(WebArchive.class, DEP_PRODUCER_APP_NAME + ".war")
                                           .addPackages(true, "com.ibm.ws.cdi.test.dependentscopedproducer");

        WebArchive defaultDecorator = ShrinkWrap.create(WebArchive.class, DEFAULT_DECORATOR_APP_NAME + ".war")
                                                .addClass("com.ibm.ws.cdi12.test.defaultdecorator.ConversationDecorator")
                                                .addClass("com.ibm.ws.cdi12.test.defaultdecorator.DefaultDecoratorServlet")
                                                .add(new FileAsset(new File("test-applications/defaultDecoratorApp.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");

        JavaArchive globalPriorityLib = ShrinkWrap.create(JavaArchive.class, "globalPriorityLib.jar")
                                                  .addClass("com.ibm.ws.cdi12.test.priority.JarBean")
                                                  .addClass("com.ibm.ws.cdi12.test.priority.helpers.AbstractBean")
                                                  .addClass("com.ibm.ws.cdi12.test.priority.helpers.AbstractInterceptor")
                                                  .addClass("com.ibm.ws.cdi12.test.priority.helpers.RelativePriority")
                                                  .addClass("com.ibm.ws.cdi12.test.priority.helpers.Bean")
                                                  .addClass("com.ibm.ws.cdi12.test.priority.helpers.AbstractDecorator")
                                                  .addClass("com.ibm.ws.cdi12.test.priority.JarDecorator")
                                                  .addClass("com.ibm.ws.cdi12.test.priority.LocalJarInterceptor")
                                                  .addClass("com.ibm.ws.cdi12.test.priority.LocalJarDecorator")
                                                  .addClass("com.ibm.ws.cdi12.test.priority.FromJar")
                                                  .addClass("com.ibm.ws.cdi12.test.priority.JarInterceptor")
                                                  .add(new FileAsset(new File("test-applications/globalPriorityLib.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");

        JavaArchive utilLib = ShrinkWrap.create(JavaArchive.class, "utilLib.jar")
                                        .addClass("com.ibm.ws.cdi12.test.utils.ChainableListImpl")
                                        .addClass("com.ibm.ws.cdi12.test.utils.Intercepted")
                                        .addClass("com.ibm.ws.cdi12.test.utils.ChainableList")
                                        .addClass("com.ibm.ws.cdi12.test.utils.Utils")
                                        .addClass("com.ibm.ws.cdi12.test.utils.SimpleAbstract")
                                        .addClass("com.ibm.ws.cdi12.test.utils.ForwardingList")
                                        .add(new FileAsset(new File("test-applications/utilLib.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");

        WebArchive globalPriorityWebApp = ShrinkWrap.create(WebArchive.class, GLOBAL_PRIORITY_APP_NAME + ".war")
                                                    .addClass("com.ibm.ws.cdi12.test.priority.NoPriorityBean")
                                                    .addClass("com.ibm.ws.cdi12.test.priority.WarInterceptor")
                                                    .addClass("com.ibm.ws.cdi12.test.priority.FromWar")
                                                    .addClass("com.ibm.ws.cdi12.test.priority.WarBean")
                                                    .addClass("com.ibm.ws.cdi12.test.priority.WarDecorator")
                                                    .addClass("com.ibm.ws.cdi12.test.priority.GlobalPriorityTestServlet")
                                                    .add(new FileAsset(new File("test-applications/globalPriorityWebApp.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");

        EnterpriseArchive globalPriorityApp = ShrinkWrap.create(EnterpriseArchive.class, "globalPriorityApp.ear")
                                                        .add(new FileAsset(new File("test-applications/globalPriorityApp.ear/resources/META-INF/application.xml")),
                                                             "/META-INF/application.xml")
                                                        .addAsLibrary(globalPriorityLib)
                                                        .addAsLibrary(utilLib)
                                                        .addAsModule(globalPriorityWebApp);

        WebArchive withAnnotationsApp = ShrinkWrap.create(WebArchive.class, WITH_ANNOTATIONS_APP_NAME + ".war")
                                                  .addClass("com.ibm.ws.cdi12.test.withAnnotations.WithAnnotationsServlet")
                                                  .addClass("com.ibm.ws.cdi12.test.withAnnotations.WithAnnotationsExtension")
                                                  .addClass("com.ibm.ws.cdi12.test.withAnnotations.NonAnnotatedBean")
                                                  .addClass("com.ibm.ws.cdi12.test.withAnnotations.RequestScopedBean")
                                                  .addClass("com.ibm.ws.cdi12.test.withAnnotations.ApplicationScopedBean")
                                                  .add(new FileAsset(new File("test-applications/withAnnotationsApp.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                                                  .add(new FileAsset(new File("test-applications/withAnnotationsApp.war/resources/META-INF/services/javax.enterprise.inject.spi.Extension")),
                                                       "/META-INF/services/javax.enterprise.inject.spi.Extension")
                                                  .addAsLibrary(utilLib);

        ShrinkHelper.exportDropinAppToServer(server, withAnnotationsApp, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, globalPriorityApp, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, defaultDecorator, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, depProducer, DeployOptions.SERVER_ONLY);

        server.startServer();

    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}
