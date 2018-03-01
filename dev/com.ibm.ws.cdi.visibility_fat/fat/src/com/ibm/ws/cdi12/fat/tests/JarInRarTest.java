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
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.fat.util.BuildShrinkWrap;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

import com.ibm.websphere.simplicity.ShrinkHelper;

/**
 * Tests the scenario where a bean is in a JAR file that is nested in a RAR file.
 */
public class JarInRarTest {

    private static LibertyServer server;
    
    private static boolean hasSetup = false;

    @BeforeClass
    public static void beforeClass() throws Exception {
        
        server = LibertyServerFactory.getStartedLibertyServer("cdi12JarInRar");

        if (hasSetup) {
            // Server already set up, app should have already been deployed when the server was started up, make sure the app has started
            assertNotNull("jarInRar started message", server.waitForStringInLog("CWWKZ0001I.*jarInRar"));
            return;
        }

        JavaArchive jarInRarJar = ShrinkWrap.create(JavaArchive.class,"jarInRar.jar")
                        .addClass("com.ibm.ws.cdi12.fat.jarinrar.rar.Amigo")
                        .addClass("com.ibm.ws.cdi12.fat.jarinrar.rar.TestResourceAdapter")
                        .add(new FileAsset(new File("test-applications/jarInRar.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");

        JavaArchive jarInRarEjb = ShrinkWrap.create(JavaArchive.class,"jarInRarEjb.jar")
                        .addClass("com.ibm.ws.cdi12.fat.jarinrar.ejb.MySingletonStartupBean")
                        .add(new FileAsset(new File("test-applications/jarInRarEjb.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml")
                        .addAsManifestResource(new File("test-applications/jarInRarEjb.jar/resources/META-INF/MANIFEST.MF"));  

        ResourceAdapterArchive jarInRarRar = ShrinkWrap.create(ResourceAdapterArchive.class,"jarInRar.rar")
                        .addAsLibrary(jarInRarJar)
                        .add(new FileAsset(new File("test-applications/jarInRar.rar/resources/META-INF/ra.xml")), "/META-INF/ra.xml");

        EnterpriseArchive jarInRarEar = ShrinkWrap.create(EnterpriseArchive.class,"jarInRar.ear")
                        .addAsModule(jarInRarEjb)
                        .addAsModule(jarInRarRar);
        
        ShrinkHelper.exportDropinAppToServer(server, jarInRarEar);
        assertNotNull("jarInRar started message", server.waitForStringInLogUsingMark("CWWKZ0001I.*jarInRar"));
        hasSetup = true;
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }
       
    @Test
    public void testBeanFromJarInRarInjectedIntoEJB() throws Exception {
        List<String> msgs = server.findStringsInLogs("MySingletonStartupBean - init - Buenos Dias me Amigo");
        assertEquals("Did not find expected injection message from EJB", 1, msgs.size());
    }
}
