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
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class HelloWorldTest extends FATServletClient {

    protected static final Class<?> c = HelloWorldTest.class;

    @Rule
    public TestName name = new TestName();

    @Server("HelloWorldServer")
    public static LibertyServer helloWorldServer;

    @BeforeClass
    public static void setUp() throws Exception {
        // add all classes from com.ibm.ws.grpc.fat.helloworld.service and io.grpc.examples.helloworld
        // to a new app HelloWorldService.war
        ShrinkHelper.defaultDropinApp(helloWorldServer, "HelloWorldService.war",
                                      "com.ibm.ws.grpc.fat.helloworld.service",
                                      "io.grpc.examples.helloworld");

        // add all classes from com.ibm.ws.grpc.fat.helloworld.client, io.grpc.examples.helloworld,
        // and com.ibm.ws.fat.grpc.tls to a new app HelloWorldClient.war.
        ShrinkHelper.defaultDropinApp(helloWorldServer, "HelloWorldClient.war",
                                      "com.ibm.ws.grpc.fat.helloworld.client",
                                      "io.grpc.examples.helloworld",
                                      "com.ibm.ws.fat.grpc.tls");

        helloWorldServer.startServer(HelloWorldTest.class.getSimpleName() + ".log");
        assertNotNull("CWWKO0219I.*ssl not recieved", helloWorldServer.waitForStringInLog("CWWKO0219I.*ssl"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        helloWorldServer.stopServer();
    }

    /**
     * Tests a basic gRPC helloworld app. HelloWorldClient.war contains a servlet that can be
     * used to connect to a gRPC service that will deployed via HelloWorldService.war.
     *
     * @throws Exception
     */
    @Test
    public void testHelloWorld() throws Exception {
        String contextRoot = "HelloWorldClient";
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
            assertTrue("the gRPC request did not complete correctly", page.asText().contains("us3r1"));
        }
    }

    /**
     * testHelloWorld() with TLS enabled.
     * This test will only be performed if the native JDK 9+ ALPN provider is available.
     *
     * @throws Exception
     */
    @Test
    public void testHelloWorldWithTls() throws Exception {

        if (JavaInfo.forServer(helloWorldServer).majorVersion() < 9) {
            Log.info(c, name.getMethodName(), "IBM JDK8 ALPN is not yet supported by the netty grpc client;"
                                              + " this test will be skipped until that support is added");
            return;
        }

        String contextRoot = "HelloWorldClient";
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
            inputText.setValueAttribute("us3r2");

            // set the port
            HtmlTextInput inputPort = (HtmlTextInput) form.getInputByName("port");
            inputPort.setValueAttribute(String.valueOf(helloWorldServer.getHttpDefaultSecurePort()));

            // set the hostname
            HtmlTextInput inputHost = (HtmlTextInput) form.getInputByName("address");
            inputHost.setValueAttribute(helloWorldServer.getHostname());

            // enable TLS
            HtmlTextInput inputTls = (HtmlTextInput) form.getInputByName("useTls");
            inputTls.setValueAttribute("true");

            String serverRoot = helloWorldServer.getServerRoot();
            HtmlTextInput serverPath = (HtmlTextInput) form.getInputByName("serverPath");
            serverPath.setValueAttribute(serverRoot);

            // submit, and execute the RPC
            HtmlSubmitInput submitButton = form.getInputByName("submit");
            page = submitButton.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            assertTrue("the gRPC request did not complete correctly", page.asText().contains("us3r2"));
        }
    }
}
