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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

import componenttest.topology.impl.LibertyServer;

/**
 * This is a test class that runs to test if an metric app is still
 * loaded by checking if the classloader is still there
 */
public abstract class MetricsClassLoaderTest {
    private static Class<?> c = MetricsClassLoaderTest.class;

    private final String METRIC_SERVLET = "/metric-servlet/metricServlet";
    private final String CHECK_SERVLET = "/checkerServlet/checkServlet";

    public abstract LibertyServer getServer();

    public abstract String getAppRemovalConfig();

    @Test
    public void testClassLoaderUnloads() throws Exception {
        getServlet(METRIC_SERVLET);
        String classLoaderBeforeUpdate = getServlet(CHECK_SERVLET).trim();

        /*
         * checkServlet returns either a classloader's (string) value or "null"
         * We want it to be not null.
         */
        Assert.assertFalse("Classloader was not obtained - null", classLoaderBeforeUpdate.equals("null"));
        Assert.assertFalse("Classloader was not obtained - N/A", classLoaderBeforeUpdate.equals("N/A"));

        //update sever.xml to remove metrics app
        String line = setConfig(getAppRemovalConfig());
        Assert.assertNotNull("Both CWWKG0017I and CWWKG0018I are not found", line);

        String classLoaderAfterUpdate = null;

        //wait 120 seconds in total if need be
        for (int i = 0; i < 240; i++) {

            //force some gcs
            System.gc();
            Runtime.getRuntime().gc();

            Thread.sleep(1000);
            classLoaderAfterUpdate = getServlet(CHECK_SERVLET).trim();
            if (classLoaderAfterUpdate.equals("null"))
                break;
        }
        /*
         * checkServlet returns a classloader's (string) value or "null"
         * We want it to be "null" now
         */
        Assert.assertFalse("var: classLoaderAfterupdate shouldn't be a null", classLoaderAfterUpdate == null);

        if (!classLoaderAfterUpdate.equals("null")) {
            getServer().javadumpThreads();
        }
        Assert.assertTrue("ClassLoader still in memory; App not properly unloaded classLoaderAfterupdate = " + classLoaderAfterUpdate, classLoaderAfterUpdate.equals("null"));

    }

    private String getServlet(String servletPath) throws IOException {
        String sURL = "http://" + getServer().getHostname() + ":" + getServer().getHttpDefaultPort() + servletPath;
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

    private String setConfig(String fileName) throws Exception {
        getServer().setServerConfigurationFile(fileName);
        return getServer().waitForStringInLogUsingMark("CWWKG0017I.*|CWWKG0018I.*");
    }
}
