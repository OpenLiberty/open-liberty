/*******************************************************************************
 * Copyright (c) 2014, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.visibility.tests.basic;

import javax.enterprise.inject.spi.Extension;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi.visibility.tests.basic.classloadPrereqWar.ClassLoadPrereqLoggerServlet;
import com.ibm.ws.cdi.visibility.tests.basic.multimodule.lib1Jar.BasicBean1;
import com.ibm.ws.cdi.visibility.tests.basic.multimodule.lib2Jar.BasicBean2;
import com.ibm.ws.cdi.visibility.tests.basic.multimodule.lib3Jar.BasicBean3;
import com.ibm.ws.cdi.visibility.tests.basic.multimodule.web1War.Web1Servlet;
import com.ibm.ws.cdi.visibility.tests.basic.multimodule.web2War.Web2Servlet;
import com.ibm.ws.cdi.visibility.tests.basic.packageAccessWar.PackageAccessTestServlet;
import com.ibm.ws.cdi.visibility.tests.basic.rootClassloaderExtJar.MyExtension;
import com.ibm.ws.cdi.visibility.tests.basic.rootClassloaderExtJar.RandomBean;
import com.ibm.ws.cdi.visibility.tests.basic.rootClassloaderWar.RootClassLoaderServlet;
import com.ibm.ws.cdi.visibility.tests.basic.warlibs.maifestLibJar.TestInjectionClass2;
import com.ibm.ws.cdi.visibility.tests.basic.warlibs.war.WarBean;
import com.ibm.ws.cdi.visibility.tests.basic.warlibs.war.WarLibsTestServlet;
import com.ibm.ws.cdi.visibility.tests.basic.warlibs.webinfLibJar.TestInjectionClass;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class BasicVisibilityTests extends FATServletClient {

    public static final String SERVER_NAME = "cdi12BasicServer";

    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE10, EERepeatActions.EE11, EERepeatActions.EE9, EERepeatActions.EE7);

    public static final String CLASS_LOAD_APP_NAME = "classloadPrereq";
    public static final String ROOT_CLASSLOADER_APP_NAME = "rootClassLoader";

    public static final String MULTI_MOD1_APP_NAME = "multiModuleAppWeb1";
    public static final String MULTI_MOD2_APP_NAME = "multiModuleAppWeb2";
    public static final String MULTI_MOD3_APP_NAME = "multiModuleAppWeb3";
    public static final String MULTI_MOD4_APP_NAME = "multiModuleAppWeb4";
    public static final String PACKAGE_ACCESS_APP_NAME = "packageAccess";

    public static final String WAR_LIB_ACCESS_APP_NAME = "warlibs";

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = PackageAccessTestServlet.class, contextRoot = PACKAGE_ACCESS_APP_NAME), //LITE
                    @TestServlet(servlet = Web1Servlet.class, contextRoot = MULTI_MOD1_APP_NAME), //FULL
                    @TestServlet(servlet = Web2Servlet.class, contextRoot = MULTI_MOD2_APP_NAME), //FULL
                    @TestServlet(servlet = Web1Servlet.class, contextRoot = MULTI_MOD3_APP_NAME), //FULL
                    @TestServlet(servlet = Web2Servlet.class, contextRoot = MULTI_MOD4_APP_NAME), //FULL
                    @TestServlet(servlet = ClassLoadPrereqLoggerServlet.class, contextRoot = CLASS_LOAD_APP_NAME), //FULL
                    @TestServlet(servlet = RootClassLoaderServlet.class, contextRoot = ROOT_CLASSLOADER_APP_NAME), //LITE
                    @TestServlet(servlet = WarLibsTestServlet.class, contextRoot = WAR_LIB_ACCESS_APP_NAME) }) //LITE
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        if (TestModeFilter.shouldRun(TestMode.FULL)) {
            WebArchive testClassLoadWAR = ShrinkWrap.create(WebArchive.class, CLASS_LOAD_APP_NAME + ".war")
                                                    .addClass(ClassLoadPrereqLoggerServlet.class.getName());
            ShrinkHelper.exportDropinAppToServer(server, testClassLoadWAR, DeployOptions.SERVER_ONLY);

            /////////////////

            JavaArchive multiModuleAppLib3 = ShrinkWrap.create(JavaArchive.class, "lib3.jar")
                                                       .addPackage(BasicBean3.class.getPackage())
                                                       .addAsManifestResource(BasicBean3.class.getResource("beans.xml"), "beans.xml");

            JavaArchive multiModuleAppLib2 = ShrinkWrap.create(JavaArchive.class, "lib2.jar")
                                                       .addPackage(BasicBean2.class.getPackage())
                                                       .addAsManifestResource(BasicBean2.class.getResource("beans.xml"), "beans.xml");

            WebArchive multiModuleAppWeb1 = ShrinkWrap.create(WebArchive.class, "web1.war")
                                                      .addPackage(Web1Servlet.class.getPackage())
                                                      .addAsWebInfResource(Web1Servlet.class.getResource("beans.xml"), "beans.xml");

            JavaArchive multiModuleAppLib1 = ShrinkWrap.create(JavaArchive.class, "lib1.jar")
                                                       .addPackage(BasicBean1.class.getPackage())
                                                       .addAsManifestResource(BasicBean1.class.getResource("beans.xml"), "beans.xml");

            WebArchive multiModuleAppWeb2 = ShrinkWrap.create(WebArchive.class, "web2.war")
                                                      .addPackage(Web2Servlet.class.getPackage())
                                                      .addAsWebInfResource(Web2Servlet.class.getResource("beans.xml"), "beans.xml")
                                                      .addAsManifestResource(Web2Servlet.class.getResource("MANIFEST.MF"), "MANIFEST.MF")
                                                      .addAsLibrary(multiModuleAppLib2)
                                                      .addAsLibrary(multiModuleAppLib3);

            EnterpriseArchive multiModuleAppOne = ShrinkWrap.create(EnterpriseArchive.class, "multiModuleApp1.ear")
                                                            .addAsManifestResource("com/ibm/ws/cdi/visibility/tests/basic/multimodule/ear1-application.xml", "application.xml")
                                                            .addAsLibrary(multiModuleAppLib1)
                                                            .addAsModule(multiModuleAppWeb1)
                                                            .addAsModule(multiModuleAppWeb2);

            EnterpriseArchive multiModuleAppTwo = ShrinkWrap.create(EnterpriseArchive.class, "multiModuleApp2.ear")
                                                            .addAsManifestResource("com/ibm/ws/cdi/visibility/tests/basic/multimodule/ear2-application.xml", "application.xml")
                                                            .addAsLibrary(multiModuleAppLib1)
                                                            .addAsLibrary(multiModuleAppLib3)
                                                            .addAsModule(multiModuleAppWeb1)
                                                            .addAsModule(multiModuleAppWeb2);

            ShrinkHelper.exportDropinAppToServer(server, multiModuleAppOne, DeployOptions.SERVER_ONLY);
            ShrinkHelper.exportDropinAppToServer(server, multiModuleAppTwo, DeployOptions.SERVER_ONLY);

            /////////////////
        }

        JavaArchive rootClassLoaderExtension = ShrinkWrap.create(JavaArchive.class, "rootClassLoaderExt.jar")
                                                         .addPackage(RandomBean.class.getPackage())
                                                         .addAsServiceProvider(Extension.class, MyExtension.class);

        WebArchive rootClassLoaderWAR = ShrinkWrap.create(WebArchive.class, ROOT_CLASSLOADER_APP_NAME + ".war")
                                                  .addPackage(RootClassLoaderServlet.class.getPackage())
                                                  .addAsManifestResource(RootClassLoaderServlet.class.getResource("permissions.xml"), "permissions.xml")
                                                  .addAsWebInfResource(RootClassLoaderServlet.class.getResource("beans.xml"), "beans.xml")
                                                  .addAsLibrary(rootClassLoaderExtension);

        ShrinkHelper.exportDropinAppToServer(server, rootClassLoaderWAR, DeployOptions.SERVER_ONLY);

        /////////////////

        WebArchive packageAccessWar = ShrinkWrap.create(WebArchive.class, "packageAccess.war")
                                                .addPackages(true, PackageAccessTestServlet.class.getPackage())
                                                .addAsWebInfResource(PackageAccessTestServlet.class.getResource("beans.xml"), "beans.xml");

        ShrinkHelper.exportDropinAppToServer(server, packageAccessWar, DeployOptions.SERVER_ONLY);

        //////////////////

        //From WarLibsAccessWarBeansTest

        JavaArchive webInfLibJar = ShrinkWrap.create(JavaArchive.class, "warlibsWebInfLib.jar")
                                             .addPackage(TestInjectionClass.class.getPackage())
                                             .addAsManifestResource(TestInjectionClass.class.getResource("beans.xml"), "beans.xml");

        JavaArchive maifestLibJar = ShrinkWrap.create(JavaArchive.class, "warlibsManifestLib.jar")
                                              .addPackage(TestInjectionClass2.class.getPackage())
                                              .addAsManifestResource(TestInjectionClass2.class.getResource("beans.xml"), "beans.xml");

        WebArchive warLibAccessBeansInWar = ShrinkWrap.create(WebArchive.class, "warlibs.war")
                                                      .addPackage(WarBean.class.getPackage())
                                                      .addAsManifestResource(WarBean.class.getResource("MANIFEST.MF"), "MANIFEST.MF")
                                                      .addAsWebInfResource(WarBean.class.getResource("beans.xml"), "beans.xml")
                                                      .addAsLibrary(webInfLibJar);

        EnterpriseArchive warLibAccessBeansInWarEAR = ShrinkWrap.create(EnterpriseArchive.class, "warLibAccessBeansInWar.ear")
                                                                .addAsModule(warLibAccessBeansInWar)
                                                                .addAsModule(maifestLibJar);
        ShrinkHelper.exportDropinAppToServer(server, warLibAccessBeansInWarEAR, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("SRVE9967W");
    }

}
