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

import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

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
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.GreeterGrpc.GreeterBlockingStub;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ClientKeepAliveTests extends FATServletClient {

    protected static final Class<?> c = ClientKeepAliveTests.class;

    private static final Logger LOG = Logger.getLogger(c.getName());

    public static final int SHORT_TIMEOUT = 500; // .5 seconds

    private static final String DEFAULT_CONFIG_FILE = "none";
    private static final String GRPC_CLIENT_KEEPALIVE_CONFIG_TIME_FILE = "grpc.client.keepalive.time.xml";
    private static final String GRPC_CLIENT_KEEPALIVE_CONFIG_TIMEOUT_FILE = "grpc.client.keepalive.timeout.xml";

    private static String serverConfigurationFile = DEFAULT_CONFIG_FILE;

    static ManagedChannel worldChannel;
    private static GreeterBlockingStub worldServiceBlockingStub;

    private static String hwc = new String("HelloWorldclient.war");

    private static final Set<String> appName = Collections.singleton("HelloWorldClient");

    @Rule
    public TestName name = new TestName();

    // Create two servers, one for grpc client only and the other for grpc server only
    @Server("GrpcClientOnly")
    public static LibertyServer grpcClient;

    @Server("GrpcServerOnly")
    public static LibertyServer grpcServer;

    @BeforeClass
    public static void setUp() throws Exception {

        LOG.info("ClientInterceptorTests : setUp() : add helloWorldClient app to the grpc client");
        // add all classes from com.ibm.ws.grpc.fat.helloworld.client, io.grpc.examples.helloworld,
        // and com.ibm.ws.fat.grpc.tls to a new app HelloWorldClient.war in the server that holds
        // the client.
        ShrinkHelper.defaultDropinApp(grpcClient, "HelloWorldClient.war",
                                      "com.ibm.ws.grpc.fat.helloworld.client",
                                      "io.grpc.examples.helloworld");

        // Drop the server app into a different server
        LOG.info("ClientInterceptorTests : setUp() : add helloWorldServer app to the grpc server");
        ShrinkHelper.defaultDropinApp(grpcServer, "HelloWorldService.war",
                                      "com.ibm.ws.grpc.fat.helloworld.service",
                                      "io.grpc.examples.helloworld");

        LOG.info("ClientInterceptorTests : setUp() : start the grpc servers");
        grpcClient.useSecondaryHTTPPort();
        grpcClient.startServer(ClientKeepAliveTests.class.getSimpleName() + ".client.log");
        grpcServer.startServer(ClientKeepAliveTests.class.getSimpleName() + ".server.log");

        worldChannel = ManagedChannelBuilder.forAddress(grpcClient.getHostname(), grpcClient.getHttpDefaultPort()).usePlaintext().build();
        worldServiceBlockingStub = GreeterGrpc.newBlockingStub(worldChannel);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        worldChannel.shutdownNow();
        grpcClient.stopServer();
        grpcServer.stopServer();
    }

    /**
     * Test the Grpc client keepAliveTime.
     *
     * Sets the time without read activity before sending a keepalive ping.
     *
     * <grpc target="*" KeepAliveTime=60/>
     *
     * @throws Exception
     *
     **/
    @Test
    public void testKeepAliveTime() throws Exception {

        // Update to a config file with a <grpc> element with the keepalive time value set
        LOG.info("ClientInterceptorTests : testKeepAliveTime() : update the server.xml file to one with a </grpc> element with KeepAliveTime");
        GrpcTestUtils.setServerConfiguration(grpcClient, null, GRPC_CLIENT_KEEPALIVE_CONFIG_TIME_FILE, appName, LOG);
        grpcClient.waitForConfigUpdateInLogUsingMark(appName);

        String contextRoot = "HelloWorldClient";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = GrpcTestUtils.createHttpUrl(grpcClient, contextRoot, "grpcClient");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            assertTrue("the servlet was not loaded correctly",
                       page.asText().contains("gRPC helloworld client example"));

            HtmlForm form = page.getFormByName("form1");

            // set a name in the form, which we'll expect the RPC to return
            HtmlTextInput inputText = (HtmlTextInput) form.getInputByName("user");
            inputText.setValueAttribute("us3r3");

            // set the port in the form
            HtmlTextInput inputPort = (HtmlTextInput) form.getInputByName("port");
            inputPort.setValueAttribute(String.valueOf(grpcServer.getHttpDefaultPort()));

            // set the hostname in the form
            HtmlTextInput inputHost = (HtmlTextInput) form.getInputByName("address");
            inputHost.setValueAttribute(grpcServer.getHostname());

            // submit, and execute the RPC
            HtmlSubmitInput submitButton = form.getInputByName("submit");
            page = submitButton.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            assertTrue("the gRPC request did not complete correctly", page.asText().contains("us3r3"));

        }
    }

    /**
     * Test the Grpc client KeepAliveTimeout
     *
     * Sets the time waiting for read activity after sending a keepalive ping.
     * If the time expires without any read activity on the connection, the connection
     * is considered dead.
     *
     * * <grpc target="*" KeepAliveTimeout=60/>
     *
     * @throws Exception
     *
     **/
    @Test
    public void testKeepAliveTimeout() throws Exception {

        LOG.info("ClientKeepAliveTests : testKeepAliveTimeout() : update the server.xml file to one with the KeepAliveTimeout param");

        // Update to a config file with a <grpc> element with the keepalive time value set
        GrpcTestUtils.setServerConfiguration(grpcClient, null, GRPC_CLIENT_KEEPALIVE_CONFIG_TIMEOUT_FILE, appName, LOG);

        grpcClient.waitForConfigUpdateInLogUsingMark(appName);

        String contextRoot = "HelloWorldClient";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = GrpcTestUtils.createHttpUrl(grpcClient, contextRoot, "grpcClient");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            assertTrue("the servlet was not loaded correctly",
                       page.asText().contains("gRPC helloworld client example"));

            HtmlForm form = page.getFormByName("form1");

            // set a name in the form, which we'll expect the RPC to return
            HtmlTextInput inputText = (HtmlTextInput) form.getInputByName("user");
            inputText.setValueAttribute("us3r3");

            // set the port in the form
            HtmlTextInput inputPort = (HtmlTextInput) form.getInputByName("port");
            inputPort.setValueAttribute(String.valueOf(grpcServer.getHttpDefaultPort()));

            // set the hostname in the form
            HtmlTextInput inputHost = (HtmlTextInput) form.getInputByName("address");
            inputHost.setValueAttribute(grpcServer.getHostname());

            // submit, and execute the RPC
            HtmlSubmitInput submitButton = form.getInputByName("submit");
            page = submitButton.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            assertTrue("the gRPC request did not complete correctly", page.asText().contains("us3r3"));
        }

    }

}
