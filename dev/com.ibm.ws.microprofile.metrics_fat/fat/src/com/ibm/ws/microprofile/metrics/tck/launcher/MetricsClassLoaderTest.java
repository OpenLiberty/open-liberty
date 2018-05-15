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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
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
public class MetricsClassLoaderTest {
    private static Class<?> c = MetricsClassLoaderTest.class;

    private static final String METRIC_SERVLET = "/metric-servlet/metricServlet";
    private static final String CHECK_SERVLET = "/checkerServlet/checkServlet";
    private static final String REMOVE_APP_CONFIG = "server_disableMetricApp.xml";

    @Server("MetricsClassLoaderServer")
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

    @Test
    public void testClassLoaderUnloads() throws Exception {
        getServlet(METRIC_SERVLET);
        String classLoaderBeforeUpdate = getServlet(CHECK_SERVLET).trim();

        /*
         * checkServlet returns either a classloader's (string) value or "null"
         * We want it to be not null.
         */
        Assert.assertFalse("Classloader was not obtained", classLoaderBeforeUpdate.equals("null"));

        //update sever.xml to remove metrics app
        String line = setConfig(REMOVE_APP_CONFIG);
        Assert.assertNotNull("Both CWWKG0017I and CWWKG0018I are not found", line);

        String classLoaderAfterUpdate = null;

        //wait 60 seconds in total if need be
        for (int i = 0; i < 60; i++) {

            //force some gcs
            System.gc();
            Runtime.getRuntime().gc();

            Thread.sleep(1000);
            classLoaderAfterUpdate = getServlet(CHECK_SERVLET).trim();
            if (classLoaderBeforeUpdate.equals("null"))
                break;
        }
        /*
         * checkServlet returns a classloader's (string) value or "null"
         * We want it to be "null" now
         */
        Assert.assertFalse("ClassLoaderAfterupdate shouldn't be a null", classLoaderAfterUpdate == null);
        Assert.assertTrue("ClassLoader still in memory; App not properly unloaded classLoaderAfterupdate = " + classLoaderAfterUpdate, classLoaderAfterUpdate.equals("null"));
    }

    private String getServlet(String servletPath) throws IOException {
        String sURL = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + servletPath;
        URL checkerServletURL = new URL(sURL);
        HttpURLConnection con = (HttpURLConnection) checkerServletURL.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            String sep = System.getProperty("line.separator");
            String line = null;
            StringBuilder lines = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            while ((line = br.readLine()) != null && line.length() > 0) {
                lines.append(line).append(sep);
            }
            return lines.toString();
        } finally {
            con.disconnect();
        }
    }

    private static String setConfig(String fileName) throws Exception {
        server.setServerConfigurationFile(fileName);
        return server.waitForStringInLogUsingMark("CWWKG0017I.*|CWWKG0018I.*");
    }

}
