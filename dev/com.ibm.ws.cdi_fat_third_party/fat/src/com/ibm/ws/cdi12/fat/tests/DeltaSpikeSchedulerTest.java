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
public class DeltaSpikeSchedulerTest extends LoggingTest {

    private static LibertyServer server;

    @Override
    protected SharedServer getSharedServer() {
        return null;
    }

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive deltaspikeTest = ShrinkWrap.create(WebArchive.class, "deltaspikeTest.war")
                        .addPackage("com.ibm.ws.cdi.deltaspike.scheduler")
                        .add(new FileAsset(new File("test-applications/deltaspikeTest.war/resources/WEB-INF/quartz-config.xml")), "/WEB-INF/quartz-config.xml")
                        .add(new FileAsset(new File("test-applications/deltaspikeTest.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/deltaspikeTest.war/resources/WEB-INF/lib/deltaspike-scheduler-module-impl-1.5.0.jar")), "/WEB-INF/lib/deltaspike-scheduler-module-impl-1.5.0.jar")
                        .add(new FileAsset(new File("test-applications/deltaspikeTest.war/resources/WEB-INF/lib/quartz-2.2.1.jar")), "/WEB-INF/lib/quartz-2.2.1.jar")
                        .add(new FileAsset(new File("test-applications/deltaspikeTest.war/resources/WEB-INF/lib/deltaspike-core-impl-1.5.0.jar")), "/WEB-INF/lib/deltaspike-core-impl-1.5.0.jar")
                        .add(new FileAsset(new File("test-applications/deltaspikeTest.war/resources/WEB-INF/lib/deltaspike-scheduler-module-api-1.5.0.jar")), "/WEB-INF/lib/deltaspike-scheduler-module-api-1.5.0.jar")
                        .add(new FileAsset(new File("test-applications/deltaspikeTest.war/resources/WEB-INF/lib/deltaspike-cdictrl-weld-1.5.0.jar")), "/WEB-INF/lib/deltaspike-cdictrl-weld-1.5.0.jar")
                        .add(new FileAsset(new File("test-applications/deltaspikeTest.war/resources/WEB-INF/lib/slf4j-jdk14-1.7.7.jar")), "/WEB-INF/lib/slf4j-jdk14-1.7.7.jar")
                        .add(new FileAsset(new File("test-applications/deltaspikeTest.war/resources/WEB-INF/lib/slf4j-api-1.7.7.jar")), "/WEB-INF/lib/slf4j-api-1.7.7.jar")
                        .add(new FileAsset(new File("test-applications/deltaspikeTest.war/resources/WEB-INF/lib/deltaspike-cdictrl-api-1.5.0.jar")), "/WEB-INF/lib/deltaspike-cdictrl-api-1.5.0.jar")
                        .add(new FileAsset(new File("test-applications/deltaspikeTest.war/resources/WEB-INF/lib/deltaspike-core-api-1.5.0.jar")), "/WEB-INF/lib/deltaspike-core-api-1.5.0.jar")
                        .add(new FileAsset(new File("test-applications/deltaspikeTest.war/resources/META-INF/permissions.xml")), "/META-INF/permissions.xml")
                        .add(new FileAsset(new File("test-applications/deltaspikeTest.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");

        server = LibertyServerFactory.getLibertyServer("cdi12DeltaSpikeServer");
        ShrinkHelper.exportAppToServer(server, deltaspikeTest);
        server.startServer();
        server.waitForStringInLogUsingMark("CWWKZ0001I.*Application deltaspikeTest started");
    }

    @Test
    @AllowedFFDC("java.lang.NoClassDefFoundError")
    public void testSchedulingeJob() throws Exception {
        int count = 0;
        boolean found = false;
        while ((count < 6) && (!found)) {
            Thread.sleep(1000); //sleep for 1s
            found = !server.findStringsInLogs("#increase called by com.ibm.ws.cdi.deltaspike.scheduler.MyScheduler").isEmpty();
            count++;
        }
        Assert.assertTrue("Test for deltaspike scheduler ", found);

    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWNEN0047W");
    }
}
