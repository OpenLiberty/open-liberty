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
package com.ibm.websphere.microprofile.faulttolerance.metrics.fat.tests;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.microprofile.faulttolerance.metrics.fat.tests.removal.MetricListServlet;
import com.ibm.websphere.microprofile.faulttolerance.metrics.fat.tests.removal.RemovalBean;
import com.ibm.websphere.microprofile.faulttolerance.metrics.fat.tests.removal.RemovalServlet;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class MetricRemovalTest {

    @Server("CDIFaultToleranceMetricsRemoval")
    public static LibertyServer server;

    private final static String TEST_METRIC = "ft.com.ibm.websphere.microprofile.faulttolerance.metrics.fat.tests.removal.RemovalBean.doWorkWithRetry.invocations.total";

    @Test
    public void metricRemovalTest() throws Exception {
        WebArchive removalTest = ShrinkWrap.create(WebArchive.class, "removalTest.war")
                        .addClasses(RemovalBean.class, RemovalServlet.class)
                        .addAsManifestResource(MetricRemovalTest.class.getResource("removal/permissions.xml"), "permissions.xml");

        WebArchive metricReporter = ShrinkWrap.create(WebArchive.class, "metricReporter.war")
                        .addClass(MetricListServlet.class)
                        .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                        .addAsManifestResource(MetricRemovalTest.class.getResource("removal/permissions.xml"), "permissions.xml");

        server.startServer();

        try {
            deployApp(removalTest);

            try {
                deployApp(metricReporter);

                // Call the test app
                HttpUtils.findStringInUrl(server, "removalTest/removaltest", "OK");

                // Check that metrics exist
                assertThat(getMetricsPage(), containsString(TEST_METRIC));

            } finally {
                // Remove the test app
                undeployApp(removalTest);
            }

            // Check that metrics do not exist
            assertThat(getMetricsPage(), not(containsString(TEST_METRIC)));
        } finally {
            undeployApp(metricReporter);
        }

    }

    /**
     * Retrieve the list of registered metrics from the {@link MetricListServlet}
     */
    private String getMetricsPage() throws IOException {
        HttpURLConnection con = HttpUtils.getHttpConnection(server, "metricReporter/metriclist");
        BufferedReader reader = HttpUtils.getResponseBody(con, "UTF-8");

        StringBuilder b = new StringBuilder();
        char[] cbuf = new char[1024];
        int charCount;

        while ((charCount = reader.read(cbuf)) > 0) {
            b.append(cbuf, 0, charCount);
        }

        return b.toString();
    }

    /**
     * Deploy an app to a running server using dropins
     * <p>
     * {@link ShrinkHelper#exportDropinAppToServer(LibertyServer, Archive)} also copies the app to the server's publish directory, which isn't what we want when we're deploying and
     * undeploying while the server is running.
     */
    private void deployApp(Archive<?> archive) throws Exception {
        ShrinkHelper.exportArtifact(archive, ".");
        System.out.println("I'm putting the archive here: " + new File(".").getAbsolutePath());
        server.setMarkToEndOfLog();
        server.copyFileToLibertyServerRoot(".", "dropins", archive.getName());
        assertNotNull(archive.getName() + " started message not found", server.waitForStringInLog("CWWKZ000[13]I.*" + getAppName(archive)));
    }

    /**
     * Remove an app from a running server using dropins
     */
    private void undeployApp(Archive<?> archive) throws Exception {
        server.setMarkToEndOfLog();
        server.deleteFileFromLibertyServerRoot("dropins/" + archive.getName());
        assertNotNull(archive.getName() + " stopped message not found", server.waitForStringInLog("CWWKZ0009I.*" + getAppName(archive)));
    }

    private String getAppName(Archive<?> archive) throws Exception {
        // Liberty uses the file name with the extension removed
        String appName = archive.getName();
        if (appName.contains(".")) {
            appName = appName.substring(0, appName.lastIndexOf("."));
        }
        return appName;
    }

}
