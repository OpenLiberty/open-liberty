/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE7_FULL;
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

import com.ibm.cdi.test.ClassLoadPrereqLoggerServlet;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi12.test.rootClassLoader.extension.RandomBean;
import com.ibm.ws.cdi12.test.rootClassLoader.web.RootClassLoaderServlet;
import com.ibm.ws.cdi12.test.web1.Web1Servlet;
import com.ibm.ws.cdi12.test.web2.Web2Servlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.rules.repeater.EERepeatTests;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jp.test.RunServlet;

@RunWith(FATRunner.class)
public class BasicVisibilityTests extends FATServletClient {

    public static final String SERVER_NAME = "cdi12BasicServer";

    @ClassRule
    public static RepeatTests r = EERepeatTests.with(SERVER_NAME, EE9, EE7_FULL);

    public static final String CLASS_LOAD_APP_NAME = "TestClassLoadPrereqLogger";
    public static final String ROOT_CLASSLOADER_APP_NAME = "rootClassLoaderApp";

    public static final String MULTI_MOD1_APP_NAME = "multiModuleAppWeb1";
    public static final String MULTI_MOD2_APP_NAME = "multiModuleAppWeb2";
    public static final String MULTI_MOD3_APP_NAME = "multiModuleAppWeb3";
    public static final String MULTI_MOD4_APP_NAME = "multiModuleAppWeb4";
    public static final String PACKAGE_PRIVATE_APP_NAME = "packagePrivateAccessApp";

    public static final String WAR_LIB_ACCESS_APP_NAME = "warLibAccessBeansInWar";

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = RunServlet.class, contextRoot = PACKAGE_PRIVATE_APP_NAME), //LITE
                    @TestServlet(servlet = Web1Servlet.class, contextRoot = MULTI_MOD1_APP_NAME), //FULL
                    @TestServlet(servlet = Web2Servlet.class, contextRoot = MULTI_MOD2_APP_NAME), //FULL
                    @TestServlet(servlet = Web1Servlet.class, contextRoot = MULTI_MOD3_APP_NAME), //FULL
                    @TestServlet(servlet = Web2Servlet.class, contextRoot = MULTI_MOD4_APP_NAME), //FULL
                    @TestServlet(servlet = ClassLoadPrereqLoggerServlet.class, contextRoot = CLASS_LOAD_APP_NAME), //FULL
                    @TestServlet(servlet = RootClassLoaderServlet.class, contextRoot = ROOT_CLASSLOADER_APP_NAME),
                    @TestServlet(servlet = com.ibm.ws.cdi12.test.warLibAccessBeansInWar.TestServlet.class, contextRoot = WAR_LIB_ACCESS_APP_NAME) }) //LITE
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        if (TestModeFilter.shouldRun(TestMode.FULL)) {
            WebArchive testClassLoadWAR = ShrinkWrap.create(WebArchive.class, CLASS_LOAD_APP_NAME + ".war")
                                                    .addClass(ClassLoadPrereqLoggerServlet.class.getName())
                                                    .add(new FileAsset(new File("test-applications/" + CLASS_LOAD_APP_NAME + ".war/resources/WEB-INF/web.xml")),
                                                         "/WEB-INF/web.xml");
            ShrinkHelper.exportDropinAppToServer(server, testClassLoadWAR, DeployOptions.SERVER_ONLY);

            /////////////////

            JavaArchive multiModuleAppLib3 = ShrinkWrap.create(JavaArchive.class, "multiModuleAppLib3.jar")
                                                       .addClass(com.ibm.ws.cdi12.test.lib3.BasicBean3A.class)
                                                       .addClass(com.ibm.ws.cdi12.test.lib3.BasicBean3.class)
                                                       .addClass(com.ibm.ws.cdi12.test.lib3.CustomNormalScoped.class);

            JavaArchive multiModuleAppLib2 = ShrinkWrap.create(JavaArchive.class, "multiModuleAppLib2.jar")
                                                       .addClass(com.ibm.ws.cdi12.test.lib2.BasicBean2.class)
                                                       .add(new FileAsset(new File("test-applications/multiModuleAppLib2.jar/resources/META-INF/beans.xml")),
                                                            "/META-INF/beans.xml");

            WebArchive multiModuleAppWeb1 = ShrinkWrap.create(WebArchive.class, "multiModuleAppWeb1.war")
                                                      .addClass(com.ibm.ws.cdi12.test.web1.Web1Servlet.class)
                                                      .add(new FileAsset(new File("test-applications/multiModuleAppWeb1.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");

            JavaArchive multiModuleAppLib1 = ShrinkWrap.create(JavaArchive.class, "multiModuleAppLib1.jar")
                                                       .addClass(com.ibm.ws.cdi12.test.lib1.BasicBean1.class)
                                                       .addClass(com.ibm.ws.cdi12.test.lib1.BasicBean1A.class)
                                                       .add(new FileAsset(new File("test-applications/multiModuleAppLib1.jar/resources/META-INF/beans.xml")),
                                                            "/META-INF/beans.xml");

            WebArchive multiModuleAppWeb2 = ShrinkWrap.create(WebArchive.class, "multiModuleAppWeb2.war")
                                                      .addClass(com.ibm.ws.cdi12.test.web2.Web2Servlet.class)
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

            /////////////////
        }

        JavaArchive rootClassLoaderExtension = ShrinkWrap.create(JavaArchive.class, "rootClassLoaderExtension.jar")
                                                         .addPackage(RandomBean.class.getPackage())
                                                         .add(new FileAsset(new File("test-applications/rootClassLoaderExtension.jar/resources/META-INF/services/javax.enterprise.inject.spi.Extension")),
                                                              "/META-INF/services/javax.enterprise.inject.spi.Extension");

        WebArchive rootClassLoaderWAR = ShrinkWrap.create(WebArchive.class, ROOT_CLASSLOADER_APP_NAME + ".war")
                                                  .add(new FileAsset(new File("test-applications/" + ROOT_CLASSLOADER_APP_NAME + ".war/resources/META-INF/permissions.xml")),
                                                       "/META-INF/permissions.xml")
                                                  .addClass(RootClassLoaderServlet.class)
                                                  .addAsLibrary(rootClassLoaderExtension);

        ShrinkHelper.exportDropinAppToServer(server, rootClassLoaderWAR, DeployOptions.SERVER_ONLY);

        /////////////////

        WebArchive packagePrivateAccessApp = ShrinkWrap.create(WebArchive.class, "packagePrivateAccessApp.war")
                                                       .addClass(jp.test.RunServlet.class)
                                                       .addClass("jp.test.bean.MyBeanHolder") //MyBeanHolder is package scoped
                                                       .addClass(jp.test.bean.MyExecutor.class)
                                                       .addClass(jp.test.bean.MyBean.class)
                                                       .add(new FileAsset(new File("test-applications/packagePrivateAccessApp.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml");

        ShrinkHelper.exportDropinAppToServer(server, packagePrivateAccessApp, DeployOptions.SERVER_ONLY);

        //////////////////

        //From WarLibsAccessWarBeansTest

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
        server.stopServer("SRVE9967W");
    }

}
