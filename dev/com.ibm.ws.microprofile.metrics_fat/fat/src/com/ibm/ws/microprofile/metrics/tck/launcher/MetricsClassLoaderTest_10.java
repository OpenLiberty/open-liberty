/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics.tck.launcher;

import java.io.File;

import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * This is a test class that runs to test if an metric app is still
 * loaded by checking if the classloader is still there
 */
@RunWith(FATRunner.class)
public class MetricsClassLoaderTest_10 extends MetricsClassLoaderTest {
    private static Class<?> c = MetricsClassLoaderTest_10.class;
    private final String REMOVE_APP_CONFIG = "server_disableMetricApp_10.xml";

    @Server("MetricsClassLoaderServer_1.0")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        //build shared Library and put it into <server>/libs
        JavaArchive jar = ShrinkHelper.buildJavaArchive("SharedLibrary", "com.ibm.ws.microprofile.metrics.classloader.utility");
        jar.as(ZipExporter.class).exportTo(new File("SharedLibrary.jar"), true);
        server.copyFileToLibertyServerRoot(new File("").getAbsolutePath(), "libs", "SharedLibrary.jar");

        //build and deploy checkerServlet and metric-servlet
        ShrinkHelper.defaultApp(server, "metric-servlet", "com.ibm.ws.microprofile.metrics.fat.metric.servlet");
        ShrinkHelper.defaultApp(server, "checkerServlet", "com.ibm.ws.microprofile.metrics.fat.checker.servlet");

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Override
    public LibertyServer getServer() {
        return server;
    }

    /** {@inheritDoc} */
    @Override
    public String getAppRemovalConfig() {
        return REMOVE_APP_CONFIG;
    }

}
