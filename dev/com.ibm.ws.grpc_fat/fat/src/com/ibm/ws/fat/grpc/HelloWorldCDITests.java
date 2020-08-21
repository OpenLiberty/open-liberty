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
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class HelloWorldCDITests extends FATServletClient {

    protected static final Class<?> c = HelloWorldCDITests.class;

    private static final Logger LOG = Logger.getLogger(c.getName());

    private static final String DEFAULT_CONFIG_FILE = "grpc.server.xml";

    private static final String CDI_ENABLED = "grpc.server.cdi.xml";
    private static final String CDI_ENABLED_INTERCEPTOR = "grpc.server.cdi.interceptor.xml";

    private static String serverConfigurationFile = DEFAULT_CONFIG_FILE;

    @Rule
    public TestName name = new TestName();

    @Server("HelloWorldServer")
    public static LibertyServer helloWorldServer;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(helloWorldServer, "HelloWorldService.war",
                                      "com.ibm.ws.grpc.fat.helloworld.service",
                                      "io.grpc.examples.helloworld");

        ShrinkHelper.defaultDropinApp(helloWorldServer, "HelloWorldClient.war",
                                      "com.ibm.ws.grpc.fat.helloworld.client",
                                      "io.grpc.examples.helloworld",
                                      "com.ibm.ws.fat.grpc.tls");

        helloWorldServer.startServer(HelloWorldCDITests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        helloWorldServer.stopServer();
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

    /**
     * Tests that the CDI-enabled GreetingCDIBean is injected correctly when cdi-2.0 is enabled
     *
     * @throws Exception
     */
    @Test
    public void testCDIService() throws Exception {
        String contextRoot = "HelloWorldClient";

        // enable cdi-2.0
        setServerConfiguration(helloWorldServer, CDI_ENABLED);
        helloWorldServer.waitForConfigUpdateInLogUsingMark(Collections.singleton("HelloWorldService"));

        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = GrpcTestUtils.createHttpUrl(helloWorldServer, contextRoot, "grpcClient");

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
            inputPort.setValueAttribute(String.valueOf(helloWorldServer.getHttpDefaultPort()));

            // set the hostname
            HtmlTextInput inputHost = (HtmlTextInput) form.getInputByName("address");
            inputHost.setValueAttribute(helloWorldServer.getHostname());

            // submit, and execute the RPC
            HtmlSubmitInput submitButton = form.getInputByName("submit");
            page = submitButton.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            assertTrue("the gRPC CDI request did not complete correctly", page.asText().contains("howdy from GreetingCDIBean us3r1"));
        }
    }

    /**
     * Tests that the CDI-enabled GreetingCDIBean is injected into HelloWorldServerCDIInterceptor when cdi-2.0 is enabled
     *
     * @throws Exception
     */
    @Test
    public void testCDIInterceptor() throws Exception {
        String contextRoot = "HelloWorldClient";

        // enable cdi-2.0
        setServerConfiguration(helloWorldServer, CDI_ENABLED_INTERCEPTOR);
        helloWorldServer.waitForConfigUpdateInLogUsingMark(Collections.singleton("HelloWorldService"));

        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = GrpcTestUtils.createHttpUrl(helloWorldServer, contextRoot, "grpcClient");

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
            inputPort.setValueAttribute(String.valueOf(helloWorldServer.getHttpDefaultPort()));

            // set the hostname
            HtmlTextInput inputHost = (HtmlTextInput) form.getInputByName("address");
            inputHost.setValueAttribute(helloWorldServer.getHostname());

            // submit, and execute the RPC
            HtmlSubmitInput submitButton = form.getInputByName("submit");
            page = submitButton.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            assertTrue("the gRPC CDI interceptor was not invoked correctly", page.asText().contains("server CDI interceptor invoked"));
            assertTrue("the gRPC request did not complete correctly", page.asText().contains("us3r1"));
        }
    }
}
