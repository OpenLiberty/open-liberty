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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi12.fat.jarinrar.ejb.MySingletonStartupBean;
import com.ibm.ws.cdi12.fat.jarinrar.rar.Amigo;
import com.ibm.ws.cdi12.fat.jarinrar.rar.TestResourceAdapter;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.rules.repeater.EERepeatTests;
import componenttest.rules.repeater.EERepeatTests.EEVersion;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Tests the scenario where a bean is in a JAR file that is nested in a RAR file.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JarInRarTest extends FATServletClient {

    public static final String SERVER_NAME = "cdi12JarInRar";

    @ClassRule
    public static RepeatTests r = EERepeatTests.with(SERVER_NAME, EEVersion.EE7); //TODO: we need to run EE9 here... EJB is in but test still fails

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        if (TestModeFilter.shouldRun(TestMode.FULL)) {

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
    public static void afterClass() throws Exception {
        server.stopServer();
    }

    @Test
    @Mode(TestMode.FULL)
    public void testBeanFromJarInRarInjectedIntoEJB() throws Exception {
        List<String> msgs = server.findStringsInLogs("MySingletonStartupBean - init - Buenos Dias me Amigo");
        assertEquals("Did not find expected injection message from EJB", 1, msgs.size());
    }
}
