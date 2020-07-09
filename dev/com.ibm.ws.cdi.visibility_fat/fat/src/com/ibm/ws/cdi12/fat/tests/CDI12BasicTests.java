/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
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
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi12.classload.prereq.test.ClassLoadPrereqServlet;
import com.ibm.ws.cdi12.test.lib1.BasicBean1;
import com.ibm.ws.cdi12.test.lib1.BasicBean1A;
import com.ibm.ws.cdi12.test.lib2.BasicBean2;
import com.ibm.ws.cdi12.test.lib3.BasicBean3;
import com.ibm.ws.cdi12.test.lib3.BasicBean3A;
import com.ibm.ws.cdi12.test.lib3.CustomNormalScoped;
import com.ibm.ws.cdi12.test.web1.Web1Servlet;
import com.ibm.ws.cdi12.test.web2.Web2Servlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EERepeatTests;
import componenttest.rules.repeater.EERepeatTests.EEVersion;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class CDI12BasicTests extends FATServletClient {

    public static final String SERVER_NAME = "cdi12BasicServer";

    public static final String CLASSLOAD_PREREQ_APP_NAME = "TestClassLoadPrereqLogger";
    public static final String MULTI_MODULE_WEB_APP_NAME_1 = "multiModuleAppWeb1";
    public static final String MULTI_MODULE_WEB_APP_NAME_2 = "multiModuleAppWeb2";
    public static final String MULTI_MODULE_WEB_APP_NAME_3 = "multiModuleAppWeb3";
    public static final String MULTI_MODULE_WEB_APP_NAME_4 = "multiModuleAppWeb4";

    @ClassRule
    public static RepeatTests r = EERepeatTests.with(SERVER_NAME, EEVersion.EE9, EEVersion.EE7);

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = ClassLoadPrereqServlet.class, contextRoot = CLASSLOAD_PREREQ_APP_NAME),
                    @TestServlet(servlet = Web1Servlet.class, contextRoot = MULTI_MODULE_WEB_APP_NAME_1),
                    @TestServlet(servlet = Web2Servlet.class, contextRoot = MULTI_MODULE_WEB_APP_NAME_2),
                    @TestServlet(servlet = Web1Servlet.class, contextRoot = MULTI_MODULE_WEB_APP_NAME_3),
                    @TestServlet(servlet = Web2Servlet.class, contextRoot = MULTI_MODULE_WEB_APP_NAME_4) })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive classLoadPrereqWar = ShrinkWrap.create(WebArchive.class, "TestClassLoadPrereqLogger.war")
                                                  .addClass(ClassLoadPrereqServlet.class)
                                                  .add(new FileAsset(new File("test-applications/TestClassLoadPrereqLogger.war/resources/WEB-INF/web.xml")),
                                                       "/WEB-INF/web.xml");

        ShrinkHelper.exportDropinAppToServer(server, classLoadPrereqWar, DeployOptions.SERVER_ONLY);

        JavaArchive multiModuleAppLib3 = ShrinkWrap.create(JavaArchive.class, "multiModuleAppLib3.jar")
                                                   .addClass(BasicBean3A.class)
                                                   .addClass(BasicBean3.class)
                                                   .addClass(CustomNormalScoped.class);

        JavaArchive multiModuleAppLib2 = ShrinkWrap.create(JavaArchive.class, "multiModuleAppLib2.jar")
                                                   .addClass(BasicBean2.class)
                                                   .add(new FileAsset(new File("test-applications/multiModuleAppLib2.jar/resources/META-INF/beans.xml")),
                                                        "/META-INF/beans.xml");

        WebArchive multiModuleAppWeb1 = ShrinkWrap.create(WebArchive.class, "multiModuleAppWeb1.war")
                                                  .addClass(Web1Servlet.class)
                                                  .add(new FileAsset(new File("test-applications/multiModuleAppWeb1.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");

        JavaArchive multiModuleAppLib1 = ShrinkWrap.create(JavaArchive.class, "multiModuleAppLib1.jar")
                                                   .addClass(BasicBean1.class)
                                                   .addClass(BasicBean1A.class)
                                                   .add(new FileAsset(new File("test-applications/multiModuleAppLib1.jar/resources/META-INF/beans.xml")),
                                                        "/META-INF/beans.xml");

        WebArchive multiModuleAppWeb2 = ShrinkWrap.create(WebArchive.class, "multiModuleAppWeb2.war")
                                                  .addClass(Web2Servlet.class)
                                                  .add(new FileAsset(new File("test-applications/multiModuleAppWeb2.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                                                  .addAsManifestResource(new File("test-applications/multiModuleAppWeb2.war/resources/META-INF/MANIFEST.MF"))
                                                  .addAsLibrary(multiModuleAppLib2)
                                                  .addAsLibrary(multiModuleAppLib3);

        EnterpriseArchive multiModuleAppOne = ShrinkWrap.create(EnterpriseArchive.class, "multiModuleApp1.ear")
                                                        .add(new FileAsset(new File("test-applications/multiModuleApp1.ear/resources/META-INF/application.xml")),
                                                             "/META-INF/application.xml")
                                                        .addAsLibrary(multiModuleAppLib1)
                                                        .addAsModule(multiModuleAppWeb1)
                                                        .addAsModule(multiModuleAppWeb2);

        EnterpriseArchive multiModuleAppTwo = ShrinkWrap.create(EnterpriseArchive.class, "multiModuleApp2.ear")
                                                        .add(new FileAsset(new File("test-applications/multiModuleApp2.ear/resources/META-INF/application.xml")),
                                                             "/META-INF/application.xml")
                                                        .addAsLibrary(multiModuleAppLib1)
                                                        .addAsLibrary(multiModuleAppLib3)
                                                        .addAsModule(multiModuleAppWeb1)
                                                        .addAsModule(multiModuleAppWeb2);

        ShrinkHelper.exportDropinAppToServer(server, multiModuleAppOne, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, multiModuleAppTwo, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("SRVE9967W");
    }

}
