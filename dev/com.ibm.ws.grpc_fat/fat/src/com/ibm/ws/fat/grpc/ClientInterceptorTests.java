/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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
import org.junit.Assert;
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
public class ClientInterceptorTests extends FATServletClient {

    protected static final Class<?> c = ClientInterceptorTests.class;

    private static final Logger LOG = Logger.getLogger(c.getName());

    public static final int SHORT_TIMEOUT = 500; // .5 seconds

    private static final String DEFAULT_CONFIG_FILE = "none";
    private static final String GRPC_CLIENT_CONFIG_FILE = "grpc.client.xml";
    private static final String GRPC_CLIENT_INTERCEPTOR_FILE = "grpc.client.interceptor.xml";
    private static final String GRPC_CLIENT_MULTIPLE_INTERCEPTOR_FILE = "grpc.client.multiple.interceptor.xml";
    private static final String GRPC_CLIENT_INVALID_CLIENT_INTERCEPTOR_FILE = "grpc.client.invalid.interceptor.xml";

    private static String serverConfigurationFile = DEFAULT_CONFIG_FILE;

    static ManagedChannel worldChannel;
    private static GreeterBlockingStub worldServiceBlockingStub;

    private static final Set<String> appName = Collections.singleton("HelloWorldClient");

    @Rule
    public TestName name = new TestName();

    // Create two servers, one for gRPC client only and the other for gRPC server only
    @Server("GrpcClientOnly")
    public static LibertyServer grpcClient;

    @Server("GrpcServerOnly")
    public static LibertyServer grpcServer;

    @BeforeClass
    public static void setUp() throws Exception {

        LOG.info("ClientInterceptorTests : setUp() : add helloWorldClient app to the gRPC client");
        // add all classes from com.ibm.ws.grpc.fat.helloworld.client, io.grpc.examples.helloworld,
        // and com.ibm.ws.fat.grpc.tls to a new app HelloWorldClient.war in the server that holds
        // the client.
        ShrinkHelper.defaultDropinApp(grpcClient, "HelloWorldClient.war",
                                      "com.ibm.ws.grpc.fat.helloworld.client",
                                      "io.grpc.examples.helloworld");

        // Drop the server app into a different server
        LOG.info("ClientInterceptorTests : setUp() : add helloWorldServer app to the gRPC server");
        ShrinkHelper.defaultDropinApp(grpcServer, "HelloWorldService.war",
                                      "com.ibm.ws.grpc.fat.helloworld.service",
                                      "io.grpc.examples.helloworld");

        LOG.info("ClientInterceptorTests : setUp() : start the gRPC servers");
        grpcClient.useSecondaryHTTPPort();
        grpcClient.startServer(ClientInterceptorTests.class.getSimpleName() + ".client.log");
        grpcServer.startServer(ClientInterceptorTests.class.getSimpleName() + ".server.log");

        worldChannel = ManagedChannelBuilder.forAddress(grpcClient.getHostname(), grpcClient.getHttpDefaultPort()).usePlaintext().build();
        worldServiceBlockingStub = GreeterGrpc.newBlockingStub(worldChannel);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Exception excep = null;
        GrpcTestUtils.stopGrpcService(worldChannel);

        try {
            if (grpcClient != null && grpcClient.isStarted()) {
                grpcClient.stopServer("CWWKT0301W");
            }
        } catch (Exception e) {
            excep = e;
            Log.error(c, "grpcClient tearDown", e);
        }

        try {
            if (grpcServer != null && grpcServer.isStarted())
                grpcServer.stopServer();
        } catch (Exception e) {
            if (excep == null)
                excep = e;
            Log.error(c, "grpcServer tearDown", e);
        }

        if (excep != null)
            throw excep;
    }

    /**
     * Test a single gRPC service interceptor
     *
     * This test adds a gRPC element without a gRPC interceptor and then updates
     * to a gRPC element with a gRPC interceptor. Make sure the interceptor does not run
     * when not configured, and make sure it runs when configured.
     *
     * <gRPC target="helloworld.Greeter" serverInterceptors="com.ibm.ws.grpc.fat.helloworld.service.HelloWorldServerInterceptor"/>
     *
     * The simple interceptor prints a message to the log.
     *
     * @throws Exception
     *
     **/
    @Test
    public void testSingleClientInterceptor() throws Exception {
        // In this first request, there is no interceptor present in the server.xml file, make sure one doesn't run
        // Update to a config file with a <grpc> element with no interceptor
        LOG.info("ClientInterceptorTests : testSingleClientInterceptor() : update the server.xml file to one with a </grpc> element with no interceptor");
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(grpcClient, serverConfigurationFile, GRPC_CLIENT_CONFIG_FILE, appName, LOG);

        try (WebClient webClient = new WebClient()) {
            executeRPC(webClient, "us3r1");

            //Make sure the Interceptor was not called and did not log a message
            String interceptorHasRun = grpcClient.verifyStringNotInLogUsingMark("com.ibm.ws.grpc.fat.helloworld.client.HelloWorldClientInterceptor has been invoked!",
                                                                                SHORT_TIMEOUT);
            if (interceptorHasRun != null) {
                Assert.fail(c + ": server.xml with <grpc> element no interceptor ran when it should not have in " + SHORT_TIMEOUT + "ms");
            }
        }

        // In this second request, we have loaded a server.xml with one interceptor, make sure it runs
        // Update to a config file with a <grpc> element with Interceptor included
        LOG.info("ClientInterceptorTests : testSingleServerInterceptor() : update the server.xml file to one with a </grpc> element with an interceptor");
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(grpcClient, serverConfigurationFile, GRPC_CLIENT_INTERCEPTOR_FILE, appName, LOG);

        try (WebClient webClient = new WebClient()) {
            executeRPC(webClient, "us3r2");

            //Make sure the Interceptor was called and logged a message
            String interceptorHasRun = grpcClient.waitForStringInLog("com.ibm.ws.grpc.fat.helloworld.client.HelloWorldClientInterceptor has been invoked!",
                                                                     SHORT_TIMEOUT);
            if (interceptorHasRun == null) {
                Assert.fail(c + ": server.xml with <grpc> element no interceptor ran when it should not have in " + SHORT_TIMEOUT + "ms");
            }
        }

    }

    /**
     * Test a multiple gRPC service interceptors
     *
     * This test adds a gRPC element with multiple gRPC interceptors. Make sure they both run.
     *
     * <gRPC target="helloworld.Greeter" serverInterceptors="com.ibm.ws.grpc.fat.helloworld.service.HelloWorldServerInterceptor"/>
     *
     * These simple interceptors prints a message to the log.
     *
     * @throws Exception
     *
     **/
    @Test
    public void testMultipleclientInterceptors() throws Exception {

        // Update to a config file with a <grpc> element with multiple interceptors
        LOG.info("ClientInterceptorTests : testMultipleClientInterceptors() : update the server.xml file to one with a </grpc> element with multiple interceptors");
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(grpcClient, serverConfigurationFile, GRPC_CLIENT_MULTIPLE_INTERCEPTOR_FILE, appName, LOG);

        LOG.info("ClientInterceptorTests : Client port default " + grpcClient.getHttpDefaultPort());
        LOG.info("ClientInterceptorTests : Server port default " + grpcServer.getHttpDefaultPort());

        LOG.info("ClientInterceptorTests : Client port secondary " + grpcClient.getHttpSecondaryPort());
        LOG.info("ClientInterceptorTests : Sserver port secondary " + grpcServer.getHttpSecondaryPort());

        try (WebClient webClient = new WebClient()) {

            executeRPC(webClient, "us3r3");

            //Make sure both Interceptors ran and logged a message
            String interceptorHasRun = grpcClient.waitForStringInLog("com.ibm.ws.grpc.fat.helloworld.client.HelloWorldClientInterceptor has been invoked!",
                                                                     SHORT_TIMEOUT);
            String interceptor2HasRun = grpcClient.waitForStringInLog("com.ibm.ws.grpc.fat.helloworld.client.HelloWorldClientInterceptor2 has been invoked!",
                                                                      SHORT_TIMEOUT);
            if (interceptorHasRun == null || interceptor2HasRun == null) {
                Assert.fail(c + ": server.xml with <grpc> element and two interceptors did not log interceptor messages in " + SHORT_TIMEOUT + "ms");
            }
        }
    }

    /**
     * Test an invalid gRPC client interceptor
     *
     * This test adds a gRPC element with a non-existent gRPC interceptor. Check for a log error.
     *
     * <gRPC target="helloworld.Greeter" serverInterceptors="com.ibm.ws.grpc.fat.helloworld.service.HelloWorldServerInterceptor"/>
     *
     * @throws Exception
     *
     **/
    @Test
    public void testInvalidClientInterceptor() throws Exception {

        LOG.info("ClientInterceptorTests : testInvalidClientInterceptor() : update the server.xml file to one with a </grpc> element with an invalid interceptor");

        // Update to a config file with a <grpc> element with invalid interceptor
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(grpcClient, serverConfigurationFile, GRPC_CLIENT_INVALID_CLIENT_INTERCEPTOR_FILE, appName, LOG);

        try (WebClient webClient = new WebClient()) {
            executeRPC(webClient, "us3r4");
            String interceptorError = grpcClient.waitForStringInLogUsingMark("CWWKT0301W: Could not load gRPC interceptor defined in clientInterceptors",
                                                                             SHORT_TIMEOUT);

            if (interceptorError == null) {
                Assert.fail(c + ": Did not log gRPC client interceptor error msg as expected in " + SHORT_TIMEOUT + "ms");
            }
        }
    }

    private void executeRPC(WebClient webClient, String user) throws Exception {
        String contextRoot = "HelloWorldClient";
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
        inputText.setValueAttribute(user);

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
        assertTrue("the gRPC request did not complete correctly", page.asText().contains(user));
    }
}
