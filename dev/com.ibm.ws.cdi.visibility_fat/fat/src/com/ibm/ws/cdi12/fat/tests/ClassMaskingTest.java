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
import com.ibm.ws.cdi.vistest.masked.appclient.Main;
import com.ibm.ws.cdi.vistest.masked.beans.SessionBean1;
import com.ibm.ws.cdi.vistest.masked.test.TestBean;
import com.ibm.ws.cdi.vistest.masked.test.TestBeanAppClientImpl;
import com.ibm.ws.cdi.vistest.masked.test.TestBeanWarImpl;
import com.ibm.ws.cdi.vistest.masked.test.Type1;
import com.ibm.ws.cdi.vistest.masked.test.Type3;
import com.ibm.ws.cdi.vistest.masked.zservlet.MaskedClassTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.rules.repeater.EERepeatTests;
import componenttest.rules.repeater.EERepeatTests.EEVersion;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * This test ensures that if a class in one module is masked by a class in another module, CDI doesn't break.
 * <p>
 * The test was introduced because CDI was assuming that all classes present in a module should be loaded by that modules classloader. However, they could be loaded from another
 * module which is on the application classpath.
 * <p>
 * We also test that beans in an App Client jar are not visible to other modules.
 */
@RunWith(FATRunner.class)
public class ClassMaskingTest extends FATServletClient {

    public static final String SERVER_NAME = "cdi12ClassMasking";

    public static final String MASKED_CLASS_APP_NAME = "maskedClassWeb";

    @ClassRule
    public static RepeatTests r = EERepeatTests.with(SERVER_NAME, EEVersion.EE8); //TODO: we need to run EE9 here... EJB is in but test still fails

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = MaskedClassTestServlet.class, contextRoot = MASKED_CLASS_APP_NAME) }) //FULL
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        if (TestModeFilter.shouldRun(TestMode.FULL)) {
            JavaArchive maskedClassEjb = ShrinkWrap.create(JavaArchive.class, "maskedClassEjb.jar")
                                                   .addClass(Type1.class)
                                                   .addClass(SessionBean1.class)
                                                   .add(new FileAsset(new File("test-applications/maskedClassEjb.jar/file.txt")), "/file.txt");

            WebArchive maskedClassWeb = ShrinkWrap.create(WebArchive.class, "maskedClassWeb.war")
                                                  .addClass(TestBeanWarImpl.class)
                                                  .addClass(Type3.class)
                                                  .addClass(Type1.class)
                                                  .addClass(MaskedClassTestServlet.class)
                                                  .add(new FileAsset(new File("test-applications/maskedClassWeb.war/file.txt")), "/file.txt");

            JavaArchive maskedClassLib = ShrinkWrap.create(JavaArchive.class, "maskedClassLib.jar")
                                                   .addClass(TestBean.class);

            JavaArchive maskedClassZAppClient = ShrinkWrap.create(JavaArchive.class, "maskedClassZAppClient.jar")
                                                          .addClass(TestBeanAppClientImpl.class)
                                                          .addClass(Main.class);

            EnterpriseArchive maskedClassEAR = ShrinkWrap.create(EnterpriseArchive.class, "maskedClass.ear")
                                                         .add(new FileAsset(new File("test-applications/maskedClass.ear/resources/META-INF/permissions.xml")),
                                                              "/META-INF/permissions.xml")
                                                         .addAsModule(maskedClassEjb)
                                                         .addAsModule(maskedClassWeb)
                                                         .addAsModule(maskedClassZAppClient)
                                                         .addAsLibrary(maskedClassLib);

            ShrinkHelper.exportDropinAppToServer(server, maskedClassEAR, DeployOptions.SERVER_ONLY);
        }

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
