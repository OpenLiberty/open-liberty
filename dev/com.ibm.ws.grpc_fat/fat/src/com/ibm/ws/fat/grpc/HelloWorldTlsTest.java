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

import java.io.IOException;
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

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class HelloWorldTlsTest extends FATServletClient {

    protected static final Class<?> c = HelloWorldTlsTest.class;

    private static final Logger LOG = Logger.getLogger(c.getName());

    @Rule
    public TestName name = new TestName();

    @Server("HelloWorldServerTls")
    public static LibertyServer helloWorldTlsServer;

    private static final Set<String> clientAppName = Collections.singleton("HelloWorldClient");
    private static final String TLS_MUTUAL_AUTH = "grpc.server.tls.mutual.auth.xml";
    private static final String TLS_INVALID_CLIENT_TRUST_STORE = "grpc.server.tls.invalid.trust.xml";

    @BeforeClass
    public static void setUp() throws Exception {
        // add all classes from com.ibm.ws.grpc.fat.helloworld.service and io.grpc.examples.helloworld
        // to a new app HelloWorldService.war
        ShrinkHelper.defaultDropinApp(helloWorldTlsServer, "HelloWorldService.war",
                                      "com.ibm.ws.grpc.fat.helloworld.service",
                                      "io.grpc.examples.helloworld");

        // add all classes from com.ibm.ws.grpc.fat.helloworld.client, io.grpc.examples.helloworld,
        // and com.ibm.ws.fat.grpc.tls to a new app HelloWorldClient.war.
        ShrinkHelper.defaultDropinApp(helloWorldTlsServer, "HelloWorldClient.war",
                                      "com.ibm.ws.grpc.fat.helloworld.client",
                                      "io.grpc.examples.helloworld");

        helloWorldTlsServer.startServer(HelloWorldTlsTest.class.getSimpleName() + ".log");
        assertNotNull("CWWKO0219I.*ssl not recieved", helloWorldTlsServer.waitForStringInLog("CWWKO0219I.*ssl"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // SRVE0777E for testHelloWorldWithTlsInvalidClientTrustStore case
        helloWorldTlsServer.stopServer("SRVE0777E");
    }

    /**
     * testHelloWorld() with TLS enabled.
     * This test will only be performed if the native JDK 9+ ALPN provider is available.
     *
     * @throws Exception
     */
    @Test
    public void testHelloWorldWithTls() throws Exception {
        if (!checkJavaVersion()) {
            return;
        }
        testHelloWorldTlsCommon();
    }

    /**
     * testHelloWorld() with TLS mutual authentication.
     * This test will only be performed if the native JDK 9+ ALPN provider is available.
     *
     * @throws Exception
     */
    @Test
    public void testHelloWorldWithTlsMutualAuth() throws Exception {
        if (!checkJavaVersion()) {
            return;
        }
        GrpcTestUtils.setServerConfiguration(helloWorldTlsServer, null, TLS_MUTUAL_AUTH, clientAppName, LOG);
        testHelloWorldTlsCommon();
    }

    /**
     * testHelloWorld() an invalid client trust store configured.
     * This test will only be performed if the native JDK 9+ ALPN provider is available.
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC("io.grpc.StatusRuntimeException")
    public void testHelloWorldWithTlsInvalidClientTrustStore() throws Exception {
        if (!checkJavaVersion()) {
            return;
        }
        GrpcTestUtils.setServerConfiguration(helloWorldTlsServer, null, TLS_INVALID_CLIENT_TRUST_STORE, clientAppName, LOG);
        Exception clientException = null;

        String contextRoot = "HelloWorldClient";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = GrpcTestUtils.createHttpUrl(helloWorldTlsServer, contextRoot, "grpcClient");

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
            inputPort.setValueAttribute(String.valueOf(helloWorldTlsServer.getHttpDefaultSecurePort()));

            // set the hostname
            HtmlTextInput inputHost = (HtmlTextInput) form.getInputByName("address");
            inputHost.setValueAttribute(helloWorldTlsServer.getHostname());

            // enable TLS
            HtmlTextInput inputTls = (HtmlTextInput) form.getInputByName("useTls");
            inputTls.setValueAttribute("true");

            String serverRoot = helloWorldTlsServer.getServerRoot();
            HtmlTextInput serverPath = (HtmlTextInput) form.getInputByName("serverPath");
            serverPath.setValueAttribute(serverRoot);

            // submit, and execute the RPC
            HtmlSubmitInput submitButton = form.getInputByName("submit");
            page = submitButton.click();

        } catch (Exception e) {
            clientException = e;
            Log.info(c, name.getMethodName(), "exception caught: " + e);
        }
        assertTrue("An error is expected for this case", clientException != null);
    }

    private void testHelloWorldTlsCommon() throws Exception {
        String contextRoot = "HelloWorldClient";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = GrpcTestUtils.createHttpUrl(helloWorldTlsServer, contextRoot, "grpcClient");

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
            inputPort.setValueAttribute(String.valueOf(helloWorldTlsServer.getHttpDefaultSecurePort()));

            // set the hostname
            HtmlTextInput inputHost = (HtmlTextInput) form.getInputByName("address");
            inputHost.setValueAttribute(helloWorldTlsServer.getHostname());

            // enable TLS
            HtmlTextInput inputTls = (HtmlTextInput) form.getInputByName("useTls");
            inputTls.setValueAttribute("true");

            String serverRoot = helloWorldTlsServer.getServerRoot();
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

    private boolean checkJavaVersion() throws IOException {
        if (JavaInfo.forServer(helloWorldTlsServer).majorVersion() < 9) {
            Log.info(c, name.getMethodName(), "IBM JDK8 ALPN is not yet supported by the netty grpc client;"
                                              + " this test will be skipped until that support is added");
            return false;
        }
        return true;
    }
}
