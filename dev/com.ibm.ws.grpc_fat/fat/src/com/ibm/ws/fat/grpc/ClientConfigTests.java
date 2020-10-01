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

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ClientConfigTests extends FATServletClient {

    protected static final Class<?> c = ClientConfigTests.class;
    private static final Logger LOG = Logger.getLogger(c.getName());
    private static final Set<String> appName = Collections.singleton("HelloWorldClient");
    private static final Set<String> appName_srv = Collections.singleton("HelloWorldService");
    private static final String DEFAULT_CONFIG_FILE = "grpc.client.xml";
    private static final String GRPC_CLIENT_ELEMENT = "grpc.client.target.server.xml";
    private static final String GRPC_CLIENT_PARAM = "grpc.client.param.server.xml";
    private static final String GRPC_CLIENT_NOMATCH = "grpc.client.nomatch.server.xml";
    private static final String GRPC_CLIENT_SPEC = "grpc.client.spec.server.xml";
    private static final String GRPC_CLIENT_WILDCARD = "grpc.client.wildcard.server.xml";
    private static final String NO_GRPC_CLIENT_ELEMENT = "grpc.client.notarget.server.xml";
    private static final String GRPC_CLIENT_MSGSIZEINVALID = "grpc.client.invalidmsgsize.server.xml";
    private static final String GRPC_CLIENT_MSGSIZESM = "grpc.client.smallmsgsize.server.xml";
    private static final String GRPCSERVER_SPEC = "grpc.server.spec.server.xml";
    private static final int SHORT_TIMEOUT = 500; // .5 seconds
    private static String serverConfigurationFile = DEFAULT_CONFIG_FILE;

    @Server("GrpcClientOnly")
    public static LibertyServer GrpcClientOnly;

    @Server("GrpcServerOnly")
    public static LibertyServer GrpcServerOnly;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("ClientConfigTests : setUp() : add helloWorldClient app to the grpc client");
        // add all classes from com.ibm.ws.grpc.fat.helloworld.client, io.grpc.examples.helloworld,
        // and com.ibm.ws.fat.grpc.tls to a new app HelloWorldClient.war in the server that holds
        // the client.
        ShrinkHelper.defaultDropinApp(GrpcClientOnly, "HelloWorldClient.war",
                                      "com.ibm.ws.grpc.fat.helloworld.client",
                                      "io.grpc.examples.helloworld");

        // Drop the server app into a different server
        LOG.info("ClientConfigTests : setUp() : add helloWorldServer app to the grpc server");
        ShrinkHelper.defaultDropinApp(GrpcServerOnly, "HelloWorldService.war",
                                      "com.ibm.ws.grpc.fat.helloworld.service",
                                      "io.grpc.examples.helloworld");

        LOG.info("ClientConfigTests : setUp() : start the grpc servers");
        GrpcClientOnly.useSecondaryHTTPPort();
        GrpcClientOnly.startServer(ClientConfigTests.class.getSimpleName() + ".client.log");
        GrpcServerOnly.startServer(ClientConfigTests.class.getSimpleName() + ".server.log");
    }

    @Rule
    public TestName name = new TestName();

    @AfterClass
    public static void tearDown() throws Exception {
        Exception excep = null;

        try {
            if (GrpcClientOnly != null && GrpcClientOnly.isStarted()) {
                /*
                 * CWWKG0083W: expected by testInvalidMaxInboundMessageSize due to invalid message size config
                 * CWWKG0076W: expected when a previous config is still in use because an invalid config was rejected
                 * SRVE0777E: "Exception thrown by application class..." expected with invalid config settings
                 */
                GrpcClientOnly.stopServer("CWWKG0083W", "CWWKG0076W", "SRVE0777E");
            }
        } catch (Exception e) {
            excep = e;
            Log.error(c, "GrpcClientOnly tearDown", e);
        }

        try {
            if (GrpcServerOnly != null && GrpcServerOnly.isStarted())
                GrpcServerOnly.stopServer();
        } catch (Exception e) {
            if (excep == null)
                excep = e;
            Log.error(c, "GrpcServerOnly tearDown", e);
        }

        if (excep != null)
            throw excep;
    }

    /**
     * Add a new <grpcClient/> element and make sure it's applied
     * The original server.xml enables the grpc feature, but has no grpcClient element.
     * Update the server with a server.xml that has a grpcClient element,
     * make sure no errors, send a request.
     *
     * @throws Exception
     *
     **/
    @Test
    public void testAddGrpcClientElement() throws Exception {

        LOG.info("ServiceConfigTests : testAddgrpcClientElement() : update the server.xml file to one with a <grpcClient> element.");

        // Update to a config file with a <grpcClient> element
        setServerConfiguration(GrpcClientOnly, GRPC_CLIENT_ELEMENT);
        GrpcClientOnly.waitForConfigUpdateInLogUsingMark(appName);
        String contextRoot = "HelloWorldClient";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = GrpcTestUtils.createHttpUrl(GrpcClientOnly, contextRoot, "grpcClient");
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
            inputPort.setValueAttribute(String.valueOf(GrpcServerOnly.getHttpDefaultPort()));

            // set the hostname of the gprcserver in the form
            HtmlTextInput inputHost = (HtmlTextInput) form.getInputByName("address");
            inputHost.setValueAttribute(GrpcServerOnly.getHostname());

            // submit to the grpcClient, and execute the RPC
            HtmlSubmitInput submitButton = form.getInputByName("submit");
            page = submitButton.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            assertTrue("the gRPC request did not complete correctly", page.asText().contains("us3r1"));

            //Make sure the Interceptor was called to verify match
            String interceptorHasRun = GrpcClientOnly.waitForStringInLog("com.ibm.ws.grpc.fat.helloworld.client.HelloWorldClientInterceptor has been invoked!",
                                                                         SHORT_TIMEOUT);
            if (interceptorHasRun == null) {
                Assert.fail(c + ": server.xml with <grpcClient> element: no interceptor ran when it should have in " + SHORT_TIMEOUT + "ms");
            }
        }
    }

    /**
     * Update an existing grpcClient element
     *
     * @throws Exception
     *
     **/
    @Test
    public void testUpdateGrpcClientParam() throws Exception {
        LOG.info("ServiceConfigTests : testUpdateGrpcClientParam() : update <grpcClient> element with new parms.");

        // First set a config with a <grpcClient> that wouldn't match the helloworld client
        setServerConfiguration(GrpcClientOnly, GRPC_CLIENT_NOMATCH);
        GrpcClientOnly.waitForConfigUpdateInLogUsingMark(appName);

        // Update to a config with a <grpcClient> element with different parms
        setServerConfiguration(GrpcClientOnly, GRPC_CLIENT_PARAM);
        GrpcClientOnly.waitForConfigUpdateInLogUsingMark(appName);

        String contextRoot = "HelloWorldClient";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = GrpcTestUtils.createHttpUrl(GrpcClientOnly, contextRoot, "grpcClient");
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
            inputPort.setValueAttribute(String.valueOf(GrpcServerOnly.getHttpDefaultPort()));

            // set the hostname of the gprcserver in the form
            HtmlTextInput inputHost = (HtmlTextInput) form.getInputByName("address");
            inputHost.setValueAttribute(GrpcServerOnly.getHostname());

            // submit to the grpcClient, and execute the RPC
            HtmlSubmitInput submitButton = form.getInputByName("submit");
            page = submitButton.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            assertTrue("the gRPC request did not complete correctly", page.asText().contains("us3r1"));

            //Make sure the Interceptor was called to verify match
            String interceptorHasRun = GrpcClientOnly.waitForStringInLog("com.ibm.ws.grpc.fat.helloworld.client.HelloWorldClientInterceptor has been invoked!",
                                                                         SHORT_TIMEOUT);
            if (interceptorHasRun == null) {
                Assert.fail(c + ": server.xml with <grpcClient> element: no interceptor ran when it should have in " + SHORT_TIMEOUT + "ms");
            }
        }
    }

    /**
     * remove an existing grpcClient element
     *
     * @throws Exception
     */
    @Test
    public void testRemoveGrpcClientElement() throws Exception {
        LOG.info("ServiceConfigTests : testRemoveGrpcClientElement() : remove <grpcClient> element.");

        // First set a config with a <grpcClient>
        setServerConfiguration(GrpcClientOnly, GRPC_CLIENT_ELEMENT);
        GrpcClientOnly.waitForConfigUpdateInLogUsingMark(appName);

        // verify this client uses the target
        String contextRoot = "HelloWorldClient";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = GrpcTestUtils.createHttpUrl(GrpcClientOnly, contextRoot, "grpcClient");
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
            inputPort.setValueAttribute(String.valueOf(GrpcServerOnly.getHttpDefaultPort()));

            // set the hostname of the gprcserver in the form
            HtmlTextInput inputHost = (HtmlTextInput) form.getInputByName("address");
            inputHost.setValueAttribute(GrpcServerOnly.getHostname());

            // submit to the grpcClient, and execute the RPC
            HtmlSubmitInput submitButton = form.getInputByName("submit");
            page = submitButton.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            assertTrue("the gRPC request did not complete correctly", page.asText().contains("us3r1"));

            //Make sure the Interceptor was called to verify match
            String interceptorHasRun = GrpcClientOnly.waitForStringInLog("com.ibm.ws.grpc.fat.helloworld.client.HelloWorldClientInterceptor has been invoked!",
                                                                         SHORT_TIMEOUT);
            if (interceptorHasRun == null) {
                Assert.fail(c + ": server.xml with <grpcClient> element: no interceptor ran when it should have in " + SHORT_TIMEOUT + "ms");
            }
        }

        // Update to a config file without a <grpcClient> element
        setServerConfiguration(GrpcClientOnly, NO_GRPC_CLIENT_ELEMENT);
        GrpcClientOnly.waitForConfigUpdateInLogUsingMark(appName);

        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = GrpcTestUtils.createHttpUrl(GrpcClientOnly, contextRoot, "grpcClient");
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
            inputPort.setValueAttribute(String.valueOf(GrpcServerOnly.getHttpDefaultPort()));

            // set the hostname of the gprcserver in the form
            HtmlTextInput inputHost = (HtmlTextInput) form.getInputByName("address");
            inputHost.setValueAttribute(GrpcServerOnly.getHostname());

            // submit to the grpcClient, and execute the RPC
            HtmlSubmitInput submitButton = form.getInputByName("submit");
            page = submitButton.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            assertTrue("the gRPC request did not complete correctly", page.asText().contains("us3r1"));

            //Make sure the Interceptor was not called and did not log a message
            String interceptorHasRun = GrpcClientOnly.verifyStringNotInLogUsingMark("com.ibm.ws.grpc.fat.helloworld.client.HelloWorldClientInterceptor has been invoked!",
                                                                                    SHORT_TIMEOUT);
            if (interceptorHasRun != null) {
                Assert.fail(c + ": server.xml with <grpcClient> element interceptor ran when it should not have");
            }
        }
    }

    /**
     * validate that * matches all outbound calls
     *
     * @throws Exception
     */
    @Test
    public void testClientTargetWildcard() throws Exception {
        LOG.info("ServiceConfigTests : testClientTargetWildcard() : validate that * matches all outbound calls.");

        setServerConfiguration(GrpcClientOnly, GRPC_CLIENT_WILDCARD);
        GrpcClientOnly.waitForConfigUpdateInLogUsingMark(appName);
        String contextRoot = "HelloWorldClient";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = GrpcTestUtils.createHttpUrl(GrpcClientOnly, contextRoot, "grpcClient");
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
            inputPort.setValueAttribute(String.valueOf(GrpcServerOnly.getHttpDefaultPort()));

            // set the hostname of the gprcserver in the form
            HtmlTextInput inputHost = (HtmlTextInput) form.getInputByName("address");
            inputHost.setValueAttribute(GrpcServerOnly.getHostname());

            // submit to the grpcClient, and execute the RPC
            HtmlSubmitInput submitButton = form.getInputByName("submit");
            page = submitButton.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            assertTrue("the gRPC request did not complete correctly", page.asText().contains("us3r1"));

            //Make sure the Interceptor was called to verify match
            String interceptorHasRun = GrpcClientOnly.waitForStringInLog("com.ibm.ws.grpc.fat.helloworld.client.HelloWorldClientInterceptor has been invoked!",
                                                                         SHORT_TIMEOUT);
            if (interceptorHasRun == null) {
                Assert.fail(c + ": server.xml with <grpcClient> element: no interceptor ran when it should have in " + SHORT_TIMEOUT + "ms");
            }
        }
    }

    /**
     * Set a target that matches no existing service paths.
     * Verify with interceptor that should never run.
     *
     * @throws Exception
     */
    @Test
    public void testClientTargetNoMatch() throws Exception {
        LOG.info("ServiceConfigTests : testClientTargetNoMatch() : validate no matches.");

        setServerConfiguration(GrpcClientOnly, GRPC_CLIENT_NOMATCH);
        GrpcClientOnly.waitForConfigUpdateInLogUsingMark(appName);
        String contextRoot = "HelloWorldClient";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = GrpcTestUtils.createHttpUrl(GrpcClientOnly, contextRoot, "grpcClient");
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
            inputPort.setValueAttribute(String.valueOf(GrpcServerOnly.getHttpDefaultPort()));

            // set the hostname of the gprcserver in the form
            HtmlTextInput inputHost = (HtmlTextInput) form.getInputByName("address");
            inputHost.setValueAttribute(GrpcServerOnly.getHostname());

            // submit to the grpcClient, and execute the RPC
            HtmlSubmitInput submitButton = form.getInputByName("submit");
            page = submitButton.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            assertTrue("the gRPC request did not complete correctly", page.asText().contains("us3r1"));

            //Make sure the Interceptor was not called and did not log a message
            String interceptorHasRun = GrpcClientOnly.verifyStringNotInLogUsingMark("com.ibm.ws.grpc.fat.helloworld.client.HelloWorldClientInterceptor has been invoked!",
                                                                                    SHORT_TIMEOUT);
            if (interceptorHasRun != null) {
                Assert.fail(c + ": server.xml with <grpcClient> element interceptor ran when it should not have");
            }
        }
    }

    /**
     * test a specific match
     *
     * @throws Exception
     */
    @Test
    public void testClientTargetSpecificMatch() throws Exception {
        LOG.info("ServiceConfigTests : testClientTargetSpecificMatch() : validate a specific match.");

        // set up client and server with same target name
        setServerConfiguration(GrpcClientOnly, GRPC_CLIENT_SPEC);
        GrpcClientOnly.waitForConfigUpdateInLogUsingMark(appName);

        String contextRoot = "HelloWorldClient";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = GrpcTestUtils.createHttpUrl(GrpcClientOnly, contextRoot, "grpcClient");
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
            inputPort.setValueAttribute(String.valueOf(GrpcServerOnly.getHttpDefaultPort()));

            // set the hostname of the gprcserver in the form
            HtmlTextInput inputHost = (HtmlTextInput) form.getInputByName("address");
            inputHost.setValueAttribute(GrpcServerOnly.getHostname());

            // submit to the grpcClient, and execute the RPC
            HtmlSubmitInput submitButton = form.getInputByName("submit");
            page = submitButton.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            assertTrue("the gRPC request did not complete correctly", page.asText().contains("us3r1"));

            //Make sure the Interceptor was called to verify match
            String interceptorHasRun = GrpcClientOnly.waitForStringInLog("com.ibm.ws.grpc.fat.helloworld.client.HelloWorldClientInterceptor has been invoked!",
                                                                         SHORT_TIMEOUT);
            if (interceptorHasRun == null) {
                Assert.fail(c + ": server.xml with <grpcClient> element: no interceptor ran when it should have in " + SHORT_TIMEOUT + "ms");
            }
        }
    }

    /**
     * test an invalid setting, verify error CWWKG0083W occurs
     *
     * @throws Exception
     */
    @Test
    public void testInvalidMaxInboundMessageSize() throws Exception {
        LOG.info("ServiceConfigTests : testInvalidMaxInboundMessageSize() : test an invalid setting.");

        setServerConfiguration(GrpcClientOnly, GRPC_CLIENT_MSGSIZEINVALID);
        GrpcClientOnly.waitForConfigUpdateInLogUsingMark(appName);
        assertNotNull(GrpcClientOnly.waitForStringInLog("CWWKG0083W.*.junk"));
    }

    /**
     * test a very small setting, send a gRPC message
     * exceeding the value, and check the server error
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC("io.grpc.StatusRuntimeException")
    public void testSmallMaxInboundMessageSize() throws Exception {
        LOG.info("ServiceConfigTests : testSmallMaxInboundMessageSize() : test very small MaxInboundMessageSize.");

        setServerConfiguration(GrpcClientOnly, GRPC_CLIENT_MSGSIZESM);
        GrpcClientOnly.waitForConfigUpdateInLogUsingMark(appName);
        String contextRoot = "HelloWorldClient";
        try (WebClient webClient = new WebClient()) {
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false); // set to false since we'll be expecting a code 500
            // Construct the URL for the test
            URL url = GrpcTestUtils.createHttpUrl(GrpcClientOnly, contextRoot, "grpcClient");
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
            inputPort.setValueAttribute(String.valueOf(GrpcServerOnly.getHttpDefaultPort()));

            // set the hostname of the gprcserver in the form
            HtmlTextInput inputHost = (HtmlTextInput) form.getInputByName("address");
            inputHost.setValueAttribute(GrpcServerOnly.getHostname());

            // submit to the grpcClient, and execute the RPC
            HtmlSubmitInput submitButton = form.getInputByName("submit");
            page = submitButton.click();

            //Make sure the expected error is logged
            String hitMax = GrpcClientOnly.waitForStringInLog("RESOURCE_EXHAUSTED: gRPC message exceeds maximum size 12",
                                                              SHORT_TIMEOUT);
            if (hitMax == null) {
                Assert.fail(c + ": server.xml with <grpcClient> element: did not get expected message size exceeded in " + SHORT_TIMEOUT + "ms");
            }
        }
    }

    /**
     * This method is used to set the server.xml
     */
    private static void setServerConfiguration(LibertyServer server,
                                               String serverXML) throws Exception {
        if (!serverConfigurationFile.equals(serverXML)) {
            // Update server.xml
            LOG.info("ServiceConfigTests : setServerConfiguration setServerConfigurationFile to : " + serverXML);
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile(serverXML);
            server.waitForStringInLog("CWWKG0017I");
            serverConfigurationFile = serverXML;
        }
    }

}
