/*
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.tests;

import static org.junit.Assert.assertFalse;

import java.net.URL;
import java.util.List;

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
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jsf22.fat.JSFUtils;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import junit.framework.Assert;

/**
 *
 * /**
 * Component System Event and EL tests for story 153719.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JSFCompELTests {
    @Rule
    public TestName name = new TestName();

    String contextRoot = "TestJSFEL";

    protected static final Class<?> c = JSFCompELTests.class;

    @Server("jsfTestServer2")
    public static LibertyServer jsfTestServer2;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(jsfTestServer2, "TestJSFEL.war", "com.ibm.ws.jsf22.el.*");

        jsfTestServer2.startServer(JSFCompELTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsfTestServer2 != null && jsfTestServer2.isStarted()) {
            //Ignore expected exception
            jsfTestServer2.stopServer("SRVE0777E");
        }
    }

    protected void verifyResponse(String contextRoot, String resource, String expectedResponse) throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, resource);
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            if (page == null) {
                Assert.fail(resource + " did not render properly.");
            }

            if (!page.asText().contains(expectedResponse)) {
                Assert.fail("The page did not contain the following expected response: " + expectedResponse);
            }
        }
    }

    protected void verifyXmlResponse(String contextRoot, String resource, String expectedResponse) throws Exception {
        //return server.verifyResponse(createWebBrowserForTestCase(), resource, expectedResponse);
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, resource);
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            if (page == null) {
                Assert.fail(resource + " did not render properly.");
            }

            if (!page.asXml().contains(expectedResponse)) {
                Assert.fail("The page did not contain the following expected response: " + expectedResponse);
            }
        }
    }

    protected void verifyResponse(String contextRoot, String resource, String... expectedResponseStrings) throws Exception {
        //return server.verifyResponse(createWebBrowserForTestCase(), resource, expectedResponseStrings);
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, resource);
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            if (page == null) {
                Assert.fail(resource + " did not render properly.");
            }

            for (String expectedResponse : expectedResponseStrings) {
                if (!page.asText().contains(expectedResponse)) {
                    Assert.fail("The page did not contain the following expected response: " + expectedResponse);
                }
            }
        }
    }

    //this tests Jira http://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-1164 AND
    ///Jira http://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-1043
    @Test
    public void testELResolverOrderAndComponentSystemEvent() throws Exception {
        // Use the SharedServer to verify a response.
        String[] expectedInResponse = {
                                        "The order and number of ELResolvers from the CompositeELResolver are correct!",
                                        "Invoked JSF 2.2 new methods in ComponentSystemEvent, isAppropriateListener() and processListener()"
        };
        this.verifyResponse(contextRoot, "ComponentEventListener.xhtml", expectedInResponse);
    }

    //this tests Jira http://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-1092
    @Test
    @ExpectedFFDC({ "javax.servlet.ServletException" })
    public void testELException() throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "ELException.xhtml");
            HtmlPage page = (HtmlPage) webClient.getPage(url);
            HtmlForm form = page.getFormByName("form1");
            HtmlSubmitInput button = form.getInputByName("submit");

            // com.meterware.httpunit.WebConversation wc = new WebConversation();
            // com.meterware.httpunit.WebRequest req = new GetMethodWebRequest(SHARED_SERVER.getServerUrl(true, "/TestJSFEL/ELException.xhtml"));
            // com.meterware.httpunit.WebResponse resp = wc.getResponse(req);
            // com.meterware.httpunit.WebForm[] forms = resp.getForms();
            try {
                // forms[0].submit();
                button.click();
            } catch (Throwable t) {
                //do nothing since the test case intentionally throws NullPointerException on  valueChangeListener, NPE gets converted to Throwable
                //when it reaches here
            }

            //Test case on the server, which is ELExceptionBean intentionally throws exception for valueChangeListener. Hence check if it's in the log
            String msgToSearchFor = "javax.servlet.ServletException: javax.el.ELException: java.lang.NullPointerException";
            List<String> msgs = jsfTestServer2.findStringsInLogs(msgToSearchFor);

            //There should be a match so fail if there is not.
            assertFalse(msgs.isEmpty());
        }
    }

    //tests EL30 static methods
    @Test
    public void testEL30StaticMethodField() throws Exception {

        String[] expectedInResponse = {
                                        "static method",
                                        "some param data"
        };
        this.verifyResponse(contextRoot, "EL30StaticFieldMethod.xhtml", expectedInResponse);
    }

    //tests EL30 Operators
    @Test
    public void testEL30Operators() throws Exception {
        String[] expectedInResponse = {
                                        "xy",
                                        "1",
                                        "2",
                                        "12",
                                        "xyz",
                                        "xyz",
                                        "xyz",
                                        "3",
                                        "8",
                                        "3",
                                        "8"
        };
        this.verifyResponse(contextRoot, "EL30Operators.xhtml", expectedInResponse);
    }

    //tests EL30 map/collection
    @Test
    public void testEL30MapCollection() throws Exception {
        String[] expectedInResponse = {
                                        "[4, 3, 5, 3]",
                                        "[1, 4, 3, 2, 5, 3, 1]",
                                        "[5, 4, 3, 3, 2, 1, 1]",
                                        "[1, 4, 3]",
                                        "[3, 2]",
                                        "[1, 4, 3, 2, 5, 3, 1]",
                                        "5",
                                        "5",
                                        "1",
                                        "2.7142857142857144",
                                        "19",
                                        "7",
                                        "true",
                                        "false",
                                        "true",
                                        "1"
        };
        this.verifyResponse(contextRoot, "EL30CollectionMap.xhtml", expectedInResponse);
    }

    //tests EL30 Lambda
    @Test
    public void testEL30Lambda() throws Exception {

        String[] expectedInResponse = {
                                        "9",
                                        "5",
                                        "17",
                                        "64",
                                        "12",
        };
        // Use the SharedServer to verify a response.
        this.verifyResponse(contextRoot, "EL30Lambda.xhtml", expectedInResponse);
    }

    //tests ValueExpression support in f:ajax event=#{bean.method} - https://issues.apache.org/jira/browse/MYFACES-3233
    @Test
    public void testAjaxEvent() throws Exception {
        // Fix the response once RTC is fixed
        String[] expectedInResponse = {
                                        "true"
        };
        this.verifyXmlResponse(contextRoot, "AjaxEvent.xhtml", "true");
    }

}
