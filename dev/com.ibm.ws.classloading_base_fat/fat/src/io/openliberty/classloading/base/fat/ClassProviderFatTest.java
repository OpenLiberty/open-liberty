/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
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
package io.openliberty.classloading.base.fat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.topology.impl.LibertyServerFactory;

import com.ibm.ws.classloading.test.ejb.MyStartupSingletonBean;
import test.HelloA;
import test.HelloB;
import test.HelloC;
import web.SharedLibraryServlet;

/**
 * Tests for class providers (mock JCA resource adapters as class providers).
 * Extends CommonLibFatTest and re-runs all 6 tests with classProviderRef
 * instead of commonLibraryRef.
 * Migrated from WS-CD-Open com.ibm.ws.classloading.ClassProviderFatTest.
 */
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class ClassProviderFatTest extends CommonLibFatTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        server = LibertyServerFactory.getLibertyServer("classloader_FAT_Server");
        server.installSystemBundle("mock.jca");
        server.installSystemFeature("mock.jca-1.0");
        server.installSystemFeature("classloadingfatlibertyinternals-1.0");

        // Deploy sharedLib.war
        WebArchive sharedLibWar = ShrinkWrap.create(WebArchive.class, "sharedLib.war")
                .addClass(SharedLibraryServlet.class);
        ShrinkHelper.addDirectory(sharedLibWar, "test-applications/sharedLib.war/resources");
        ShrinkHelper.exportToServer(server, "apps", sharedLibWar);

        JavaArchive libA = ShrinkHelper.buildJavaArchive("sharedLibraryA.jar", HelloA.class.getPackage().getName())
                .addAsResource(new org.jboss.shrinkwrap.api.asset.StringAsset("resource:libraryA"), "resource.txt")
                .addAsResource(org.jboss.shrinkwrap.api.asset.EmptyAsset.INSTANCE, "test/.keep");
        ShrinkHelper.exportToServer(server, "SharedLibraryA", libA, DeployOptions.OVERWRITE);

        JavaArchive libB = ShrinkHelper.buildJavaArchive("sharedLibraryB.jar", HelloB.class.getPackage().getName())
                .addAsResource(new org.jboss.shrinkwrap.api.asset.StringAsset("resource:libraryB"), "resource.txt")
                .addAsResource(org.jboss.shrinkwrap.api.asset.EmptyAsset.INSTANCE, "test/.keep");
        ShrinkHelper.exportToServer(server, "SharedLibraryB", libB, DeployOptions.OVERWRITE);

        JavaArchive libC = ShrinkHelper.buildJavaArchive("sharedLibraryC.jar", HelloC.class.getPackage().getName())
                .addAsResource(new org.jboss.shrinkwrap.api.asset.StringAsset("resource:libraryC"), "resource.txt")
                .addAsResource(org.jboss.shrinkwrap.api.asset.EmptyAsset.INSTANCE, "test/.keep");
        ShrinkHelper.exportToServer(server, "SharedLibraryC", libC, DeployOptions.OVERWRITE);

        JavaArchive ejbJar = ShrinkHelper.buildJavaArchive("sharedLibEJB.jar",
                MyStartupSingletonBean.class.getPackage().getName());
        EnterpriseArchive ejbEar = ShrinkWrap.create(EnterpriseArchive.class, "sharedLibEJB.ear")
                .addAsModule(ejbJar);
        ShrinkHelper.exportToServer(server, "apps", ejbEar);

        SharedLibFatTest.setConfig(server, "ClassProvider/server.xml", "ClassProviderFatTest",
                                   SHAREDLIB1_STARTED,
                                   SHAREDLIB2_STARTED,
                                   SHAREDLIB3_STARTED,
                                   SHAREDLIB4_STARTED,
                                   SHAREDLIB5_STARTED);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer();
        server.uninstallSystemBundle("mock.jca");
        server.uninstallSystemFeature("mock.jca-1.0");
        server.uninstallSystemFeature("classloadingfatlibertyinternals-1.0");
    }

    @Override
    @Test
    public void testCommonLibraryLoadClass() throws Exception {
        super.testCommonLibraryLoadClass();
    }

    @Override
    @Test
    public void testCommonLibraryReadResource() throws Exception {
        super.testCommonLibraryReadResource();
    }

    @Override
    @Test
    public void testCommonLibraryReadDirectoryResource() throws Exception {
        super.testCommonLibraryReadDirectoryResource();
    }

    @Override
    @Test
    public void testCommonLibraryReadDirectoryResources() throws Exception {
        super.testCommonLibraryReadDirectoryResources();
    }

    @Override
    @Test
    public void testCommonLibraryReadResources() throws Exception {
        super.testCommonLibraryReadResources();
    }

    @Override
    @Test
    public void testCommonLibraryReadResourceSearchOrder() throws Exception {
        super.testCommonLibraryReadResourceSearchOrder();
    }
}
