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

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.LoggingTest;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

public class AppExtensionTest extends LoggingTest {

    private static LibertyServer server;

    @Override
    protected SharedServer getSharedServer() {
        return null;
    }

    @BeforeClass
    public static void setUp() throws Exception {

        JavaArchive applicationExtensionJar = ShrinkWrap.create(JavaArchive.class,"applicationExtension.jar")
                        .addClass("test.PlainExtension")
                        .addClass("bean.InLibJarBean")
                        .add(new FileAsset(new File("test-applications/applicationExtension.jar/resources/META-INF/services/javax.enterprise.inject.spi.Extension")), "/META-INF/services/javax.enterprise.inject.spi.Extension")
                        .add(new FileAsset(new File("test-applications/applicationExtension.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");

        WebArchive applicationExtension = ShrinkWrap.create(WebArchive.class, "applicationExtension.war")
                        .addClass("main.TestServlet")
                        .addClass("main.bean.InSameWarBean")
                        .addAsLibrary(applicationExtensionJar);

        server = LibertyServerFactory.getStartedLibertyServer("cdi12AppExtensionServer");
        server.setMarkToEndOfLog(server.getDefaultLogFile());
        ShrinkHelper.exportDropinAppToServer(server, applicationExtension);
        server.waitForStringInLogUsingMark("CWWKZ000[13]I.*applicationExtension"); // App started or updated

    }

    @Test
    public void testAppServlet() throws Exception {

        HttpUtils.findStringInUrl(server, "/applicationExtension/TestServlet", "In Same WAR : created in ", "In lib JAR  : created in");
    }

    @Test
    public void testAppExtensionLoaded() throws Exception {
        Assert.assertFalse("Test for before bean discovery event",
                           server.findStringsInLogs("PlainExtension: beginning the scanning process").isEmpty());
        Assert.assertFalse("Test for processing annotation type event",
                           server.findStringsInLogs("PlainExtension: scanning type->").isEmpty());
        Assert.assertFalse("Test for after bean discovery event",
                           server.findStringsInLogs("PlainExtension: finished the scanning process").isEmpty());
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

}
