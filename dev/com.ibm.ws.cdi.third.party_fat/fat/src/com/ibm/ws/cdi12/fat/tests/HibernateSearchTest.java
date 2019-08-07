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

import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.LoggingTest;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

//@Mode(TestMode.FULL)
public class HibernateSearchTest extends LoggingTest {

    private static LibertyServer server;

    @Override
    protected SharedServer getSharedServer() {
        return null;
    }

    private static void addJars(WebArchive hibernateSearchTest) {

       File libsDir = new File ("test-applications/hibernateSearchTest.war/resources/WEB-INF/lib");
       if (! libsDir.isDirectory()) {
           throw (new IllegalStateException("The libs directory is not a directory"));
       }

       for (File jar : libsDir.listFiles()) {
           hibernateSearchTest.add(new FileAsset(jar), "/WEB-INF/lib/"+jar.getName());
       }
    }

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive hibernateSearchTest = ShrinkWrap.create(WebArchive.class, "hibernateSearchTest.war")
                        .addPackages(true, "cdi.hibernate.test")
                        .add(new FileAsset(new File("test-applications/hibernateSearchTest.war/resources/META-INF/persistence.xml")), "/META-INF/persistence.xml")
                        .add(new FileAsset(new File("test-applications/hibernateSearchTest.war/resources/META-INF/jpaorm.xml")), "/META-INF/jpaorm.xml");

        addJars(hibernateSearchTest);

        server = LibertyServerFactory.getLibertyServer("cdi20HibernateSearchServer");
        ShrinkHelper.exportAppToServer(server, hibernateSearchTest);
        server.startServer();
        server.waitForStringInLogUsingMark("CWWKZ0001I.*Application hibernateSearchTest.war started");
    }

    @Test
    public void testHibernateSearch() throws Exception {
        
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
