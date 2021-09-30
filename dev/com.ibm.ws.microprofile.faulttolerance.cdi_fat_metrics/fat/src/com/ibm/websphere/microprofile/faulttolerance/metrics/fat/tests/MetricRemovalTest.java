/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance.metrics.fat.tests;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
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
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.microprofile.faulttolerance.metrics.fat.tests.removal.RemovalBean;
import com.ibm.websphere.microprofile.faulttolerance.metrics.fat.tests.removal.RemovalServlet;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.faulttolerance.fat.repeat.RepeatFaultTolerance;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class MetricRemovalTest {

    private static final String SERVER_NAME = "CDIFaultToleranceMetricsRemoval";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    //mpMetrics-4.0 isn't ready yet so can't run against MP50
//    @ClassRule
//    public static RepeatTests r = RepeatFaultTolerance.repeatDefault(SERVER_NAME)
//                    .andWith(RepeatFaultTolerance.ft11metrics20Features(SERVER_NAME));

    @ClassRule
    public static RepeatTests r = RepeatFaultTolerance.repeat(SERVER_NAME, TestMode.FULL, MicroProfileActions.MP40, MicroProfileActions.MP20)
                    .andWith(RepeatFaultTolerance.ft11metrics20Features(SERVER_NAME));

    // FT 1.x, 2.x & Metrics 2.0+
    private final static String TEST_METRIC = "ft_com_ibm_websphere_microprofile_faulttolerance_metrics_fat_tests_removal_RemovalBean_doWorkWithRetry_invocations_total";
    // FT 1.x, 2.x & Metrics 1.x
    private final static String TEST_METRIC2 = "ft_com_ibm_websphere_microprofile_faulttolerance_metrics_fat_tests_removal_removal_bean_do_work_with_retry_invocations_total";
    // FT 3.0 & Metrics 2.0+
    private final static String TEST_METRIC3 = "ft_retry_retries_total";

    @Test
    public void metricRemovalTest() throws Exception {
        WebArchive removalTest = ShrinkWrap.create(WebArchive.class, "removalTest.war")
                        .addClasses(RemovalBean.class, RemovalServlet.class)
                        .addAsManifestResource(MetricRemovalTest.class.getResource("removal/permissions.xml"), "permissions.xml");

        try {
            server.startServer();
            deployApp(removalTest);

            try {
                // Call the test app
                HttpUtils.findStringInUrl(server, "removalTest/removaltest", "OK");

                // Check that metrics exist
                assertThat(getMetricsPage(), anyOf(containsString(TEST_METRIC),
                                                   containsString(TEST_METRIC2),
                                                   containsString(TEST_METRIC3)));

            } finally {
                // Remove the test app
                undeployApp(removalTest);
            }

            // Check that metrics do not exist
            assertThat(getMetricsPage(), allOf(not(containsString(TEST_METRIC)),
                                               not(containsString(TEST_METRIC2)),
                                               not(containsString(TEST_METRIC3))));
        } finally {
            server.stopServer();
        }

    }

    /**
     * Retrieve the list of registered metrics from the /metrics endpoint
     */
    private String getMetricsPage() throws IOException {
        HttpURLConnection con = HttpUtils.getHttpConnection(server, "metrics");
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
