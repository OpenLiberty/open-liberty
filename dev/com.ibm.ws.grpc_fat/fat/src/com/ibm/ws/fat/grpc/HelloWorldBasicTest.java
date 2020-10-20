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

import org.junit.Rule;
import org.junit.rules.TestName;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

public abstract class HelloWorldBasicTest extends FATServletClient {

    protected static final Class<?> c = HelloWorldBasicTest.class;

    @Rule
    public TestName name = new TestName();
    protected LibertyServer serverRef;

    /**
     * Tests a basic gRPC helloworld app. HelloWorldClient.war contains a servlet that can be
     * used to connect to a gRPC service that will deployed via HelloWorldService.war.
     *
     * @throws Exception
     */
    public String runHelloWorldTest() throws Exception {
        String contextRoot = "HelloWorldClient";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = GrpcTestUtils.createHttpUrl(serverRef, contextRoot, "grpcClient");

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
            inputPort.setValueAttribute(String.valueOf(serverRef.getHttpDefaultPort()));

            // set the hostname
            HtmlTextInput inputHost = (HtmlTextInput) form.getInputByName("address");
            inputHost.setValueAttribute(serverRef.getHostname());

            // submit, and execute the RPC
            HtmlSubmitInput submitButton = form.getInputByName("submit");
            page = submitButton.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            return page.asText();
        }
    }

    public String runHelloWorldTlsTest() throws Exception {
        String contextRoot = "HelloWorldClient";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = GrpcTestUtils.createHttpUrl(serverRef, contextRoot, "grpcClient");

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
            inputPort.setValueAttribute(String.valueOf(serverRef.getHttpDefaultSecurePort()));

            // set the hostname
            HtmlTextInput inputHost = (HtmlTextInput) form.getInputByName("address");
            inputHost.setValueAttribute(serverRef.getHostname());

            // enable TLS
            HtmlTextInput inputTls = (HtmlTextInput) form.getInputByName("useTls");
            inputTls.setValueAttribute("true");

            String serverRoot = serverRef.getServerRoot();
            HtmlTextInput serverPath = (HtmlTextInput) form.getInputByName("serverPath");
            serverPath.setValueAttribute(serverRoot);

            // submit, and execute the RPC
            HtmlSubmitInput submitButton = form.getInputByName("submit");
            page = submitButton.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());

            return page.asText();
        }
    }
}
