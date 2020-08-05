/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.fat.grpc;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class GrpcMetricsTest extends FATServletClient {

    protected static final Class<?> c = GrpcMetricsTest.class;

    @Rule
    public TestName name = new TestName();

    @Server("HelloWorldServer")
    public static LibertyServer grpcMetricsServer; //GrpcMetricsServer

    @BeforeClass
    public static void setUp() throws Exception {
        // add all classes from com.ibm.ws.grpc.fat.helloworld.service and io.grpc.examples.helloworld
        // to a new app HelloWorldService.war
        ShrinkHelper.defaultDropinApp(grpcMetricsServer, "HelloWorldService.war",
                                      "com.ibm.ws.grpc.fat.helloworld.service",
                                      "io.grpc.examples.helloworld");

        // add all classes from com.ibm.ws.grpc.fat.helloworld.client, io.grpc.examples.helloworld,
        // and com.ibm.ws.fat.grpc.tls to a new app HelloWorldClient.war.
        ShrinkHelper.defaultDropinApp(grpcMetricsServer, "HelloWorldClient.war",
                                      "com.ibm.ws.grpc.fat.helloworld.client",
                                      "io.grpc.examples.helloworld",
                                      "com.ibm.ws.fat.grpc.tls");

        grpcMetricsServer.startServer(GrpcMetricsTest.class.getSimpleName() + ".log");
        assertNotNull("CWWKO0219I.*ssl not recieved", grpcMetricsServer.waitForStringInLog("CWWKO0219I.*ssl"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        grpcMetricsServer.stopServer();
    }

    /**
     * Tests gRPC server-side and client-side metrics.
     *
     * @throws Exception
     */
    @Test
    public void testGrpcMetrics() throws Exception {
        String contextRoot = "HelloWorldClient";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = GrpcTestUtils.createHttpUrl(grpcMetricsServer, contextRoot, "grpcClient");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            assertTrue("the servlet was not loaded correctly",
                       page.asText().contains("gRPC helloworld client example"));

            HtmlForm form = page.getFormByName("form1");

            // set a name, which we'll expect the RPC to return
            HtmlTextInput inputText = (HtmlTextInput) form.getInputByName("user");
            inputText.setValueAttribute("us3r1");

            // set the port
            HtmlTextInput inputPort = (HtmlTextInput) form.getInputByName("port");
            inputPort.setValueAttribute(String.valueOf(grpcMetricsServer.getHttpDefaultPort()));

            // set the hostname
            HtmlTextInput inputHost = (HtmlTextInput) form.getInputByName("address");
            inputHost.setValueAttribute(grpcMetricsServer.getHostname());

            // submit, and execute the RPC
            HtmlSubmitInput submitButton = form.getInputByName("submit");
            page = submitButton.click();

            // check the gRPC client-side metrics
            checkMetric("/metrics/vendor/grpc.client.rpcStarted.total", "1");
            checkMetric("/metrics/vendor/grpc.client.rpcCompleted.total", "1");
            checkMetric("/metrics/vendor/grpc.client.sentMessages.total", "1");
            checkMetric("/metrics/vendor/grpc.client.receivedMessages.total", "1");

            // check the gRPC server-side metrics
            checkMetric("/metrics/vendor/grpc.server.rpcStarted.total", "1");
            checkMetric("/metrics/vendor/grpc.server.rpcCompleted.total", "1");
            checkMetric("/metrics/vendor/grpc.server.sentMessages.total", "1");
            checkMetric("/metrics/vendor/grpc.server.receivedMessages.total", "1");

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            assertTrue("the gRPC request did not complete correctly", page.asText().contains("us3r1"));
        }
    }

    /**
     * Verifies the given metric by comparing the actual value with the given value
     *
     * @param metricName    - the metric to verify
     * @param expectedValue - the expected value
     * @return the actual value received from the Metrics endpoint
     */
    private String checkMetric(String metricName, String expectedValue) {
        HttpURLConnection con = null;
        try {
            URL url = new URL("http://" + grpcMetricsServer.getHostname() + ":" + grpcMetricsServer.getHttpDefaultPort() + metricName);
            int retcode;
            con = (HttpURLConnection) url.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            retcode = con.getResponseCode();
            if (retcode != 200) {
                fail("Bad return code from Metrics method call. Expected 200, got " + retcode);

                return null;
            }

            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);

            BufferedReader br = new BufferedReader(isr);

            String metricValue = null;
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                if (!line.startsWith("#")) {
                    String[] mertricAttr = line.split(" ");
                    if (mertricAttr.length > 0) {
                        metricValue = mertricAttr[mertricAttr.length - 1];
                        break;
                    }
                }
            }

            if (metricValue == null || !metricValue.equals(expectedValue)) {
                fail(String.format("Incorrect metric value [%s]. Expected [%s], got [%s]", metricName, expectedValue, metricValue));
            }
            return metricValue;

        } catch (Exception e) {
            fail("Caught unexpected exception: " + e);
            return null;
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }

    }
}
