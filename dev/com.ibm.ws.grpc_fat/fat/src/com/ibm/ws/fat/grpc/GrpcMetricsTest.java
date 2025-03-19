/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.fat.grpc;

import static com.ibm.ws.fat.grpc.monitoring.GrpcMetricsTestUtils.checkMetric;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
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
    private static final Logger LOG = Logger.getLogger(c.getName());

    private static final String GRPC_CLIENT_METRICS = "grpc.client.metrics.server.xml";
    private static final String GRPC_SERVER_METRICS = "grpc.server.metrics.server.xml";
    private static final String GRPC_BOTH_METRICS = "grpc.both.metrics.server.xml";
    private static final Set<String> appName = Collections.singleton("HelloWorldClient");
    private static final Set<String> appName_srv = Collections.singleton("HelloWorldService");
    String clientContextRoot = "HelloWorldClient";

    // keep track of the number of client calls made, at the class level since the client server and app are never restarted
    private static int clientCallCount;

    @Rule
    public TestName name = new TestName();

    @Server("GrpcClientOnly")
    public static LibertyServer GrpcClientOnly;

    @Server("GrpcServerOnly")
    public static LibertyServer GrpcServerOnly;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("GrpcMetricsTest : setUp() : add HelloWorldClient and HelloWorldService to GrpcClientOnly");

        GrpcClientOnly.addIgnoredErrors(Arrays.asList("CWPKI0063W"));
        GrpcServerOnly.addIgnoredErrors(Arrays.asList("CWPKI0063W"));
        ShrinkHelper.defaultDropinApp(GrpcClientOnly, "HelloWorldClient.war",
                                      "com.ibm.ws.grpc.fat.helloworld.client",
                                      "io.grpc.examples.helloworld");

        // add the "service" app to the client server, to be used by testGrpcMetricsSingleServer()
        ShrinkHelper.defaultDropinApp(GrpcClientOnly, "HelloWorldService.war",
                                      "com.ibm.ws.grpc.fat.helloworld.service",
                                      "io.grpc.examples.helloworld");

        LOG.info("GrpcMetricsTest : setUp() : add HelloWorldService app to GrpcServerOnly");
        ShrinkHelper.defaultDropinApp(GrpcServerOnly, "HelloWorldService.war",
                                      "com.ibm.ws.grpc.fat.helloworld.service",
                                      "io.grpc.examples.helloworld");

        LOG.info("GrpcMetricsTest : setUp() : start the grpc servers");
        GrpcClientOnly.useSecondaryHTTPPort();
    }

    @Before
    public void before() {
        clientCallCount = 0;
    }

    @After
    public void after() throws Exception {
        GrpcClientOnly.stopServer();
        GrpcServerOnly.stopServer();
    }

    /**
     * Tests gRPC server-side and client-side metrics, with each client and server app running on separate servers
     *
     * @throws Exception
     */
    @Test
    public void testGrpcMetricsSeparateServers() throws Exception {

        GrpcClientOnly.startServer(GrpcMetricsTest.class.getSimpleName() + ".client.log");
        GrpcServerOnly.startServer(GrpcMetricsTest.class.getSimpleName() + ".server.log");

        // enable mpMetrics-2.3 on both servers
        GrpcTestUtils.setServerConfiguration(GrpcClientOnly, null, GRPC_CLIENT_METRICS, appName, LOG);
        GrpcTestUtils.setServerConfiguration(GrpcServerOnly, null, GRPC_SERVER_METRICS, appName_srv, LOG);

        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = GrpcTestUtils.createHttpUrl(GrpcClientOnly, clientContextRoot, "grpcClient");

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
            inputPort.setValueAttribute(String.valueOf(GrpcServerOnly.getHttpDefaultPort()));

            // set the hostname
            HtmlTextInput inputHost = (HtmlTextInput) form.getInputByName("address");
            inputHost.setValueAttribute(GrpcServerOnly.getHostname());

            // submit, and execute the RPC
            HtmlSubmitInput submitButton = form.getInputByName("submit");
            page = submitButton.click();
            clientCallCount++;

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            assertTrue("the gRPC request did not complete correctly", page.asText().contains("us3r1"));

            // check the gRPC server-side metrics
            checkMetric(GrpcServerOnly, "/metrics/vendor/grpc.server.rpcStarted.total", null, "1");
            checkMetric(GrpcServerOnly, "/metrics/vendor/grpc.server.rpcCompleted.total", null, "1");
            checkMetric(GrpcServerOnly, "/metrics/vendor/grpc.server.sentMessages.total", null, "1");
            checkMetric(GrpcServerOnly, "/metrics/vendor/grpc.server.receivedMessages.total", null, "1");

            // check the gRPC client-side metrics
            checkMetric(GrpcClientOnly, "/metrics/vendor/grpc.client.rpcStarted.total", null, String.valueOf(clientCallCount));
            checkMetric(GrpcClientOnly, "/metrics/vendor/grpc.client.rpcCompleted.total", null, String.valueOf(clientCallCount));
            checkMetric(GrpcClientOnly, "/metrics/vendor/grpc.client.sentMessages.total", null, String.valueOf(clientCallCount));
            checkMetric(GrpcClientOnly, "/metrics/vendor/grpc.client.receivedMessages.total", null, String.valueOf(clientCallCount));
        }
    }

    /**
     * Tests gRPC server-side and client-side metrics, with both apps running on the same server.
     *
     * @throws Exception
     */
    @Test
    public void testGrpcMetricsSingleServer() throws Exception {

        GrpcClientOnly.startServer(GrpcMetricsTest.class.getSimpleName() + ".client.log");

        // enable mpMetrics-2.3 along with grpc-1.0 and grpcClient-1.0 on GrpcClientOnly
        GrpcTestUtils.setServerConfiguration(GrpcClientOnly, null, GRPC_BOTH_METRICS, appName_srv, LOG);

        // set the service call count to zero, we'll be checking the grpc service count on GrpcClientOnly for the first time
        int serverCallCount = 0;

        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = GrpcTestUtils.createHttpUrl(GrpcClientOnly, clientContextRoot, "grpcClient");

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
            inputPort.setValueAttribute(String.valueOf(GrpcClientOnly.getHttpDefaultPort()));

            // set the hostname
            HtmlTextInput inputHost = (HtmlTextInput) form.getInputByName("address");
            inputHost.setValueAttribute(GrpcClientOnly.getHostname());

            // submit, and execute the RPC
            HtmlSubmitInput submitButton = form.getInputByName("submit");

            // submit a few RPCs so we can make sure the metrics are incrementing as expected
            for (int i = 0; i < 5; i++) {
                page = submitButton.click();
                clientCallCount++;
                serverCallCount++;
                Thread.sleep(15);
            }

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            assertTrue("the gRPC request did not complete correctly", page.asText().contains("us3r1"));

            // check the gRPC server-side metrics
            checkMetric(GrpcClientOnly, "/metrics/vendor/grpc.server.rpcStarted.total", null, String.valueOf(serverCallCount));
            checkMetric(GrpcClientOnly, "/metrics/vendor/grpc.server.rpcCompleted.total", null, String.valueOf(serverCallCount));
            checkMetric(GrpcClientOnly, "/metrics/vendor/grpc.server.sentMessages.total", null, String.valueOf(serverCallCount));
            checkMetric(GrpcClientOnly, "/metrics/vendor/grpc.server.receivedMessages.total", null, String.valueOf(serverCallCount));

            // check the gRPC client-side metrics
            checkMetric(GrpcClientOnly, "/metrics/vendor/grpc.client.rpcStarted.total", null, String.valueOf(clientCallCount));
            checkMetric(GrpcClientOnly, "/metrics/vendor/grpc.client.rpcCompleted.total", null, String.valueOf(clientCallCount));
            checkMetric(GrpcClientOnly, "/metrics/vendor/grpc.client.sentMessages.total", null, String.valueOf(clientCallCount));
            checkMetric(GrpcClientOnly, "/metrics/vendor/grpc.client.receivedMessages.total", null, String.valueOf(clientCallCount));
        }
    }
}
