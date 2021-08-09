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

import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE7;
import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE9;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi.vistest.maskedClass.appclient.MaskedClassClientMain;
import com.ibm.ws.cdi.vistest.maskedClass.appclient.beans.TestBeanAppClientImpl;
import com.ibm.ws.cdi.vistest.maskedClass.beans.TestBean;
import com.ibm.ws.cdi.vistest.maskedClass.beans.TestBeanWarImpl;
import com.ibm.ws.cdi.vistest.maskedClass.beans.Type1;
import com.ibm.ws.cdi.vistest.maskedClass.beans.Type3;
import com.ibm.ws.cdi.vistest.maskedClass.ejb.SessionBean1;
import com.ibm.ws.cdi.vistest.maskedClass.zservlet.ZMaskedClassTestServlet;
import com.ibm.ws.cdi12.fat.jarinrar.ejb.MySingletonStartupBean;
import com.ibm.ws.cdi12.fat.jarinrar.rar.Amigo;
import com.ibm.ws.cdi12.fat.jarinrar.rar.TestResourceAdapter;

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

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class EJBVisibilityTests extends FATServletClient {

    public static final String SERVER_NAME = "cdi12EJBServer";

    @ClassRule
    public static RepeatTests r = EERepeatTests.with(SERVER_NAME, EE7, EE9);

    public static final String MASKED_CLASS_APP_NAME = "maskedClassWeb";

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = ZMaskedClassTestServlet.class, contextRoot = MASKED_CLASS_APP_NAME) }) //FULL
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        if (TestModeFilter.shouldRun(TestMode.FULL)) {
            JavaArchive maskedClassEjb = ShrinkWrap.create(JavaArchive.class, "maskedClassEjb.jar")
                                                   .addClass(Type1.class)
                                                   .addClass(SessionBean1.class)
                                                   .add(new FileAsset(new File("test-applications/maskedClassEjb.jar/file.txt")), "/file.txt");

            WebArchive maskedClassWeb = ShrinkWrap.create(WebArchive.class, "maskedClassWeb.war")
                                                  .addClass(Type1.class)
                                                  .addClass(Type3.class)
                                                  .addClass(TestBeanWarImpl.class)
                                                  .addClass(ZMaskedClassTestServlet.class)
                                                  .add(new FileAsset(new File("test-applications/maskedClassWeb.war/file.txt")), "/file.txt");

            JavaArchive maskedClassLib = ShrinkWrap.create(JavaArchive.class, "maskedClassLib.jar")
                                                   .addClass(TestBean.class);

            JavaArchive maskedClassZAppClient = ShrinkWrap.create(JavaArchive.class, "maskedClassZAppClient.jar")
                                                          .addClass(TestBeanAppClientImpl.class)
                                                          .addClass(MaskedClassClientMain.class);

            EnterpriseArchive maskedClassEAR = ShrinkWrap.create(EnterpriseArchive.class, "maskedClass.ear")
                                                         .add(new FileAsset(new File("test-applications/maskedClass.ear/resources/META-INF/permissions.xml")),
                                                              "/META-INF/permissions.xml")
                                                         .addAsModule(maskedClassEjb)
                                                         .addAsModule(maskedClassWeb)
                                                         .addAsModule(maskedClassZAppClient)
                                                         .addAsLibrary(maskedClassLib);

            ShrinkHelper.exportDropinAppToServer(server, maskedClassEAR, DeployOptions.SERVER_ONLY);

            ///////////////////

            JavaArchive jarInRarJar = ShrinkWrap.create(JavaArchive.class, "jarInRar.jar")
                                                .addClass(Amigo.class)
                                                .addClass(TestResourceAdapter.class)
                                                .add(new FileAsset(new File("test-applications/jarInRar.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");

            JavaArchive jarInRarEjb = ShrinkWrap.create(JavaArchive.class, "jarInRarEjb.jar")
                                                .addClass(MySingletonStartupBean.class)
                                                .add(new FileAsset(new File("test-applications/jarInRarEjb.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml")
                                                .addAsManifestResource(new File("test-applications/jarInRarEjb.jar/resources/META-INF/MANIFEST.MF"));

            ResourceAdapterArchive jarInRarRar = ShrinkWrap.create(ResourceAdapterArchive.class, "jarInRar.rar")
                                                           .addAsLibrary(jarInRarJar)
                                                           .add(new FileAsset(new File("test-applications/jarInRar.rar/resources/META-INF/ra.xml")), "/META-INF/ra.xml");

            EnterpriseArchive jarInRarEar = ShrinkWrap.create(EnterpriseArchive.class, "jarInRar.ear")
                                                      .addAsModule(jarInRarEjb)
                                                      .addAsModule(jarInRarRar);

            ShrinkHelper.exportDropinAppToServer(server, jarInRarEar, DeployOptions.SERVER_ONLY);

        }

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    @Mode(TestMode.FULL)
    public void testBeanFromJarInRarInjectedIntoEJB() throws Exception {
        List<String> msgs = server.findStringsInLogs("MySingletonStartupBean - init - Buenos Dias me Amigo");
        assertEquals("Did not find expected injection message from EJB", 1, msgs.size());
    }

}
