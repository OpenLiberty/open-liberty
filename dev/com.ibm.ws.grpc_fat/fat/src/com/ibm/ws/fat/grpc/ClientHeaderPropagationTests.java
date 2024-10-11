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

import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Arrays;
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

/**
 * the flow should look like this:
 * 1. set <grpcTarget target="match" headersToPropagate="testHeader"/>
 * 2. make a request to some servlet like HelloWorldClientServlet which will do some
 * outbound grpc call. Make sure to set a header testHeader=testValue on this initial request.
 * 3. verify that testHeader is included with the grpc request that’s made to the
 * test grpc service. The easiest way to do this will be to create a server interceptor
 * that implements interceptCall(), which will give you easy access to the metadata (headers).
 *
 * Also test the security related metadata here since the nature of the checks are similar.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ClientHeaderPropagationTests extends FATServletClient {

    protected static final Class<?> c = ClientHeaderPropagationTests.class;
    private static final Logger LOG = Logger.getLogger(c.getName());
    private static final Set<String> appName = Collections.singleton("HelloWorldClient");
    private static final Set<String> appName_srv = Collections.singleton("HelloWorldService");
    private static final String DEFAULT_CONFIG_FILE = "grpc.server.xml";
    private static final String GRPC_CLIENT_HTP_MATCH = "grpc.client.htp.match.server.xml";
    private static final String GRPC_CLIENT_HTP_MULTIMATCH = "grpc.client.htp.multimatch.server.xml";
    private static final String GRPC_CLIENT_HTP_NOMATCH = "grpc.client.htp.nomatch.server.xml";
    private static final String GRPC_CLIENT_SECHEADER = "grpc.client.secheader.server.xml";
    private static final int SHORT_TIMEOUT = 500; // .5 seconds
    private static String serverConfigurationFile = DEFAULT_CONFIG_FILE;

    @Server("GrpcServer")
    public static LibertyServer GrpcServer;

    @BeforeClass
    public static void setUp() throws Exception {
        GrpcServer.addIgnoredErrors(Arrays.asList("CWPKI0063W"));
        GrpcServer.deleteAllDropinApplications();
        GrpcServer.removeAllInstalledAppsForValidation();
        LOG.info("ClientHeaderPropagationTests : setUp() : add HelloWorldClient and HelloWorldService apps to the server");
        ShrinkHelper.defaultDropinApp(GrpcServer, "HelloWorldClient.war",
                                      "com.ibm.ws.grpc.fat.helloworld.client",
                                      "io.grpc.examples.helloworld");

        // Drop in the server app, same server
        ShrinkHelper.defaultDropinApp(GrpcServer, "HelloWorldService.war",
                                      "com.ibm.ws.grpc.fat.helloworld.service",
                                      "io.grpc.examples.helloworld");

        LOG.info("ClientHeaderPropagationTests : setUp() : start the grpc server");
        GrpcServer.startServer(ClientHeaderPropagationTests.class.getSimpleName() + ".server.log");
    }

    @Rule
    public TestName name = new TestName();

    @AfterClass
    public static void tearDown() throws Exception {
        // Setting serverConfigurationFile to null forces a server.xml update (when GrpcTestUtils.setServerConfiguration() is first called) on the repeat run
        // If not set to null, test failures may occur (since the incorrect server.xml could be used)
        serverConfigurationFile = null;

        // Stop the server
        if (GrpcServer != null && GrpcServer.isStarted()) {
            GrpcServer.stopServer();
        }
    }

    /**
     * test no match in headersToPropagate
     *
     * @throws Exception
     */
    @Test
    public void testNoHeaderMatches() throws Exception {
        LOG.info("ClientHeaderPropagationTests : testNoHeaderMatches() : test no match in headersToPropagate.");

        // First set a config with a <grpcTarget> that wouldn't match a header
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(GrpcServer, serverConfigurationFile, GRPC_CLIENT_HTP_NOMATCH, appName, LOG);

        String contextRoot = "HelloWorldClient";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = GrpcTestUtils.createHttpUrl(GrpcServer, contextRoot, "grpcClient");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            webClient.addRequestHeader("testHeader", "123");

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            assertTrue("the servlet was not loaded correctly",
                       page.asText().contains("gRPC helloworld client example"));

            HtmlForm form = page.getFormByName("form1");

            // set a name in the form, which we'll expect the RPC to return
            HtmlTextInput inputText = (HtmlTextInput) form.getInputByName("user");
            inputText.setValueAttribute("us3r1");

            // set the port of the grpcserver in the form
            HtmlTextInput inputPort = (HtmlTextInput) form.getInputByName("port");
            inputPort.setValueAttribute(String.valueOf(GrpcServer.getHttpDefaultPort()));

            // set the hostname of the gprcserver in the form
            HtmlTextInput inputHost = (HtmlTextInput) form.getInputByName("address");
            inputHost.setValueAttribute(GrpcServer.getHostname());

            // submit to the grpcClient, and execute the RPC
            HtmlSubmitInput submitButton = form.getInputByName("submit");
            page = submitButton.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            assertTrue("the gRPC request did not complete correctly", page.asText().contains("us3r1"));

            //Make sure the Interceptor was called to verify match
            String interceptorHasRun = GrpcServer.waitForStringInLog("com.ibm.ws.grpc.fat.helloworld.service.HelloWorldServerInterceptor3 has been invoked!",
                                                                     SHORT_TIMEOUT);
            if (interceptorHasRun == null) {
                Assert.fail(c + ": server.xml with <grpcTarget> element: no interceptor ran when it should have in " + SHORT_TIMEOUT + "ms");
            }

            //Make sure the testHeader is not displayed by the Interceptor
            String headerFound = GrpcServer.verifyStringNotInLogUsingMark("test[H|h]eader=123", SHORT_TIMEOUT);
            if (headerFound != null) {
                Assert.fail(c + ": testHeader found in nomatch case when it should not have");
            }
        }
    }

    /**
     * test a single match in headersToPropagate
     *
     * @throws Exception
     */
    @Test
    public void testSingleHeaderMatch() throws Exception {
        LOG.info("ClientHeaderPropagationTests : testSingleHeaderMatch() : test a single match in headersToPropagate.");

        // First set a config with a <grpcTarget> that matches a header
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(GrpcServer, serverConfigurationFile, GRPC_CLIENT_HTP_MATCH, appName, LOG);

        String contextRoot = "HelloWorldClient";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = GrpcTestUtils.createHttpUrl(GrpcServer, contextRoot, "grpcClient");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            webClient.addRequestHeader("testHeader", "123");

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            assertTrue("the servlet was not loaded correctly",
                       page.asText().contains("gRPC helloworld client example"));

            HtmlForm form = page.getFormByName("form1");

            // set a name in the form, which we'll expect the RPC to return
            HtmlTextInput inputText = (HtmlTextInput) form.getInputByName("user");
            inputText.setValueAttribute("us3r1");

            // set the port of the grpcserver in the form
            HtmlTextInput inputPort = (HtmlTextInput) form.getInputByName("port");
            inputPort.setValueAttribute(String.valueOf(GrpcServer.getHttpDefaultPort()));

            // set the hostname of the gprcserver in the form
            HtmlTextInput inputHost = (HtmlTextInput) form.getInputByName("address");
            inputHost.setValueAttribute(GrpcServer.getHostname());

            // submit to the grpcClient, and execute the RPC
            HtmlSubmitInput submitButton = form.getInputByName("submit");
            page = submitButton.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            assertTrue("the gRPC request did not complete correctly", page.asText().contains("us3r1"));

            //Make sure the Interceptor was called to verify match
            String interceptorHasRun = GrpcServer.waitForStringInLog("com.ibm.ws.grpc.fat.helloworld.service.HelloWorldServerInterceptor3 has been invoked!",
                                                                     SHORT_TIMEOUT);
            if (interceptorHasRun == null) {
                Assert.fail(c + ": server.xml with <grpcTarget> element: no interceptor ran when it should have in " + SHORT_TIMEOUT + "ms");
            }

            // make sure expected header was found
            String headerFound = GrpcServer.waitForStringInLog("test[H|h]eader=123", SHORT_TIMEOUT);
            if (headerFound == null) {
                Assert.fail(c + ": testHeader=123 not found when it should have in " + SHORT_TIMEOUT + "ms");
            }
        }
    }

    /**
     * test multiple matches in headersToPropagate
     *
     * @throws Exception
     */
    @Test
    public void testMultipleHeaderMatches() throws Exception {
        LOG.info("ClientHeaderPropagationTests : testMultipleHeaderMatches() : test multiple matches in headersToPropagate.");

        // First set a config with a <grpcTarget> that matches a header
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(GrpcServer, serverConfigurationFile, GRPC_CLIENT_HTP_MULTIMATCH, appName, LOG);

        String contextRoot = "HelloWorldClient";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = GrpcTestUtils.createHttpUrl(GrpcServer, contextRoot, "grpcClient");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            webClient.addRequestHeader("testHeader", "123");
            webClient.addRequestHeader("testHeader1", "456");
            webClient.addRequestHeader("testHeader2", "789");

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            assertTrue("the servlet was not loaded correctly",
                       page.asText().contains("gRPC helloworld client example"));

            HtmlForm form = page.getFormByName("form1");

            // set a name in the form, which we'll expect the RPC to return
            HtmlTextInput inputText = (HtmlTextInput) form.getInputByName("user");
            inputText.setValueAttribute("us3r1");

            // set the port of the grpcserver in the form
            HtmlTextInput inputPort = (HtmlTextInput) form.getInputByName("port");
            inputPort.setValueAttribute(String.valueOf(GrpcServer.getHttpDefaultPort()));

            // set the hostname of the gprcserver in the form
            HtmlTextInput inputHost = (HtmlTextInput) form.getInputByName("address");
            inputHost.setValueAttribute(GrpcServer.getHostname());

            // submit to the grpcClient, and execute the RPC
            HtmlSubmitInput submitButton = form.getInputByName("submit");
            page = submitButton.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            assertTrue("the gRPC request did not complete correctly", page.asText().contains("us3r1"));

            //Make sure the Interceptor was called to verify match
            String interceptorHasRun = GrpcServer.waitForStringInLog("com.ibm.ws.grpc.fat.helloworld.service.HelloWorldServerInterceptor3 has been invoked!",
                                                                     SHORT_TIMEOUT);
            if (interceptorHasRun == null) {
                Assert.fail(c + ": server.xml with <grpcTarget> element: no interceptor ran when it should have in " + SHORT_TIMEOUT + "ms");
            }

            // make sure expected headers were found
            String headerFound = GrpcServer.waitForStringInLog("test[H|h]eader1=456,test[H|h]eader2=789", SHORT_TIMEOUT);
            if (headerFound == null) {
                Assert.fail(c + ": testHeader1 or testHeader2 not found when it should have in " + SHORT_TIMEOUT + "ms");
            }
            //Make sure the testHeader is not displayed by the Interceptor
            headerFound = GrpcServer.verifyStringNotInLogUsingMark("test[H|h]eader=123", SHORT_TIMEOUT);
            if (headerFound != null) {
                Assert.fail(c + ": testHeader found when it should not have");
            }
        }
    }

    /**
     * test security metadata overrideAuthority
     *
     * @throws Exception
     */
    @Test
    public void testOverrideAuthority() throws Exception {
        LOG.info("ClientHeaderPropagationTests : testOverrideAuthority() : test overrideAuthority is propagated.");

        // First set a config with a <grpcTarget> that matches a header
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(GrpcServer, serverConfigurationFile, GRPC_CLIENT_SECHEADER, appName, LOG);

        String contextRoot = "HelloWorldClient";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = GrpcTestUtils.createHttpUrl(GrpcServer, contextRoot, "grpcClient");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            assertTrue("the servlet was not loaded correctly",
                       page.asText().contains("gRPC helloworld client example"));

            HtmlForm form = page.getFormByName("form1");

            // set a name in the form, which we'll expect the RPC to return
            HtmlTextInput inputText = (HtmlTextInput) form.getInputByName("user");
            inputText.setValueAttribute("us3r1");

            // set the port of the grpcserver in the form
            HtmlTextInput inputPort = (HtmlTextInput) form.getInputByName("port");
            inputPort.setValueAttribute(String.valueOf(GrpcServer.getHttpDefaultPort()));

            // set the hostname of the gprcserver in the form
            HtmlTextInput inputHost = (HtmlTextInput) form.getInputByName("address");
            inputHost.setValueAttribute(GrpcServer.getHostname());

            // submit to the grpcClient, and execute the RPC
            HtmlSubmitInput submitButton = form.getInputByName("submit");
            page = submitButton.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            assertTrue("the gRPC request did not complete correctly", page.asText().contains("us3r1"));

            //Make sure the Interceptor was called to verify match
            String interceptorHasRun = GrpcServer.waitForStringInLog("com.ibm.ws.grpc.fat.helloworld.service.HelloWorldServerInterceptor3 has been invoked!",
                                                                     SHORT_TIMEOUT);
            if (interceptorHasRun == null) {
                Assert.fail(c + ": server.xml with <grpcTarget> element: no interceptor ran when it should have in " + SHORT_TIMEOUT + "ms");
            }

            // make sure expected header was found
            String headerFound = GrpcServer.waitForStringInLog("TestDomain123", SHORT_TIMEOUT);
            if (headerFound == null) {
                Assert.fail(c + ": overrideAuthority not found when it should have in " + SHORT_TIMEOUT + "ms");
            }
        }
    }

    /**
     * test security metadata userAgent
     *
     * @throws Exception
     */
    @Test
    public void testUserAgent() throws Exception {
        LOG.info("ClientHeaderPropagationTests : testUserAgent() : test userAgent is propagated.");

        // First set a config with a <grpcTarget> that matches a header
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(GrpcServer, serverConfigurationFile, GRPC_CLIENT_SECHEADER, appName, LOG);

        String contextRoot = "HelloWorldClient";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = GrpcTestUtils.createHttpUrl(GrpcServer, contextRoot, "grpcClient");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            assertTrue("the servlet was not loaded correctly",
                       page.asText().contains("gRPC helloworld client example"));

            HtmlForm form = page.getFormByName("form1");

            // set a name in the form, which we'll expect the RPC to return
            HtmlTextInput inputText = (HtmlTextInput) form.getInputByName("user");
            inputText.setValueAttribute("us3r1");

            // set the port of the grpcserver in the form
            HtmlTextInput inputPort = (HtmlTextInput) form.getInputByName("port");
            inputPort.setValueAttribute(String.valueOf(GrpcServer.getHttpDefaultPort()));

            // set the hostname of the gprcserver in the form
            HtmlTextInput inputHost = (HtmlTextInput) form.getInputByName("address");
            inputHost.setValueAttribute(GrpcServer.getHostname());

            // submit to the grpcClient, and execute the RPC
            HtmlSubmitInput submitButton = form.getInputByName("submit");
            page = submitButton.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            assertTrue("the gRPC request did not complete correctly", page.asText().contains("us3r1"));

            //Make sure the Interceptor was called to verify match
            String interceptorHasRun = GrpcServer.waitForStringInLog("com.ibm.ws.grpc.fat.helloworld.service.HelloWorldServerInterceptor3 has been invoked!",
                                                                     SHORT_TIMEOUT);
            if (interceptorHasRun == null) {
                Assert.fail(c + ": server.xml with <grpcTarget> element: no interceptor ran when it should have in " + SHORT_TIMEOUT + "ms");
            }

            // make sure expected header was found
            String headerFound = GrpcServer.waitForStringInLog("[U|u]ser-[A|a]gent=Agent456", SHORT_TIMEOUT);
            if (headerFound == null) {
                Assert.fail(c + ": userAgent not found when it should have in " + SHORT_TIMEOUT + "ms");
            }
        }
    }
}
