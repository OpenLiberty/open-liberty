/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.visibility.tests.ejb;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi.visibility.tests.ejb.jarInRar.ejbJar.MySingletonStartupBean;
import com.ibm.ws.cdi.visibility.tests.ejb.jarInRar.jar.TestResourceAdapter;
import com.ibm.ws.cdi.visibility.tests.ejb.jarInRar.war.JarInRarTestServlet;
import com.ibm.ws.cdi.visibility.tests.ejb.maskedClass.appClientJar.TestBeanAppClientImpl;
import com.ibm.ws.cdi.visibility.tests.ejb.maskedClass.ejbJar.SessionBean1;
import com.ibm.ws.cdi.visibility.tests.ejb.maskedClass.libJar.TestBean;
import com.ibm.ws.cdi.visibility.tests.ejb.maskedClass.sharedbeans.Type1;
import com.ibm.ws.cdi.visibility.tests.ejb.maskedClass.webWar.MaskedClassTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class EJBVisibilityTests extends FATServletClient {

    public static final String SERVER_NAME = "cdi12EJBServer";

    //Because this test includes Fault Tolerance, the repeat must be done using MicroProfileActions.
    //Fault Tolerance is included as a feature with CDI extension which can see application BDAs.
    //This previously caused issues with masked classes.
    //At some point we need to repeat this test with EE11 but Fault Tolerance does not yet support EE11.
    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(SERVER_NAME,
                                                             MicroProfileActions.MP61,
                                                             MicroProfileActions.MP50,
                                                             MicroProfileActions.MP14);

    public static final String MASKED_CLASS_APP_NAME = "maskedClassWeb";

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = MaskedClassTestServlet.class, contextRoot = MASKED_CLASS_APP_NAME),
                    @TestServlet(servlet = JarInRarTestServlet.class, contextRoot = "jarInRar.war")
    }) //FULL
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        JavaArchive maskedClassEjb = ShrinkWrap.create(JavaArchive.class, "maskedClassEjb.jar")
                                               .addAsManifestResource(SessionBean1.class.getResource("beans.xml"), "beans.xml")
                                               .addPackage(SessionBean1.class.getPackage())
                                               .addPackage(Type1.class.getPackage()); // Shared beans package

        WebArchive maskedClassWeb = ShrinkWrap.create(WebArchive.class, "maskedClassWeb.war")
                                              .addAsWebInfResource(MaskedClassTestServlet.class.getResource("beans.xml"), "beans.xml")
                                              .addPackage(MaskedClassTestServlet.class.getPackage())
                                              .addPackage(Type1.class.getPackage()); // Shared beans package

        JavaArchive maskedClassLib = ShrinkWrap.create(JavaArchive.class, "maskedClassLib.jar")
                                               .addAsManifestResource(TestBean.class.getResource("beans.xml"), "beans.xml")
                                               .addPackage(TestBean.class.getPackage());

        JavaArchive maskedClassAppClient = ShrinkWrap.create(JavaArchive.class, "maskedClassAppClient.jar")
                                                     .addAsManifestResource(TestBeanAppClientImpl.class.getResource("beans.xml"), "beans.xml")
                                                     .addPackage(TestBeanAppClientImpl.class.getPackage())
                                                     .setManifest(TestBeanAppClientImpl.class.getResource("MANIFEST.MF"));

        EnterpriseArchive maskedClassEAR = ShrinkWrap.create(EnterpriseArchive.class, "maskedClass.ear")
                                                     .addAsModule(maskedClassEjb)
                                                     .addAsModule(maskedClassWeb)
                                                     .addAsModule(maskedClassAppClient)
                                                     .addAsLibrary(maskedClassLib)
                                                     .addAsManifestResource("com/ibm/ws/cdi/visibility/tests/ejb/maskedClass/permissions.xml", "permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, maskedClassEAR, DeployOptions.SERVER_ONLY);

        ///////////////////

        JavaArchive jarInRarJar = ShrinkWrap.create(JavaArchive.class, "jarInRar.jar")
                                            .addPackage(TestResourceAdapter.class.getPackage())
                                            .addAsManifestResource(TestResourceAdapter.class.getResource("beans.xml"), "beans.xml");

        JavaArchive jarInRarEjb = ShrinkWrap.create(JavaArchive.class, "jarInRarEjb.jar")
                                            .addPackage(MySingletonStartupBean.class.getPackage())
                                            .addAsManifestResource(MySingletonStartupBean.class.getResource("beans.xml"), "beans.xml")
                                            .setManifest(MySingletonStartupBean.class.getResource("MANIFEST.MF"));

        ResourceAdapterArchive jarInRarRar = ShrinkWrap.create(ResourceAdapterArchive.class, "jarInRar.rar")
                                                       .addAsLibrary(jarInRarJar)
                                                       .addAsManifestResource("com/ibm/ws/cdi/visibility/tests/ejb/jarInRar/rar/ra.xml", "ra.xml");

        WebArchive jarInRarWar = ShrinkWrap.create(WebArchive.class, "jarInRar.war")
                                           .addAsWebInfResource(JarInRarTestServlet.class.getResource("beans.xml"), "beans.xml")
                                           .addPackage(JarInRarTestServlet.class.getPackage());

        EnterpriseArchive jarInRarEar = ShrinkWrap.create(EnterpriseArchive.class, "jarInRar.ear")
                                                  .addAsModule(jarInRarEjb)
                                                  .addAsModule(jarInRarRar)
                                                  .addAsModule(jarInRarWar);

        ShrinkHelper.exportDropinAppToServer(server, jarInRarEar, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}
