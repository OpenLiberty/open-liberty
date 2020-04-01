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

import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jsf22.fat.JSFUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * A collection of tests for the JSF 2.2 Faces Flows feature
 * All of these tests make use of the programmatic/CDI facilities provided by JSF.
 *
 * Flows features tested include:
 * faces-config.xml configuration
 * *-flow.xml configuration
 * switches
 * parameters, nested flows
 * JAR packaging
 * explicit navigation cases
 *
 * @author Bill Lucy
 */
@RunWith(FATRunner.class)
public class CDIFlowsTests {
    @Rule
    public TestName name = new TestName();

    String contextRoot = "CDIFacesFlows";

    protected static final Class<?> c = CDIFlowsTests.class;

    @Server("jsfCDIFlowsServer")
    public static LibertyServer jsfCDIFlowsServer;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(jsfCDIFlowsServer, "CDIFacesFlows.war", "com.ibm.ws.jsf22.fat.cdiflows.beans");

        jsfCDIFlowsServer.startServer(CDIFlowsTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsfCDIFlowsServer != null && jsfCDIFlowsServer.isStarted()) {
            jsfCDIFlowsServer.stopServer();
        }
    }

    /**
     * Verify the behavior of a simple flow which is defined via a *-flow.xml configuration,
     * and which utilizes a simple flowScoped bean
     *
     * @throws Exception
     */
    @Test
    public void JSF22Flows_TestSimpleBean() throws Exception {
        URL url = JSFUtils.createHttpUrl(jsfCDIFlowsServer, contextRoot, "");
        JSF22FlowsTests.testSimpleCase("simpleBean", url);
    }

    /**
     * Verify the behavior of a simple flow which is defined entirely programmatically
     *
     * @throws Exception
     */
    @Test
    public void JSF22Flows_TestFlowBuilder() throws Exception {
        URL url = JSFUtils.createHttpUrl(jsfCDIFlowsServer, contextRoot, "");
        JSF22FlowsTests.testSimpleCase("simpleFlowBuilder", url);
    }

    /**
     * Verify the behavior of nested flow set in which one flow is defined declaratively and another is
     * defined programmatically
     *
     * @throws Exception
     */
    @Test
    public void JSF22Flows_TestMixedConfiguration() throws Exception {
        URL url = JSFUtils.createHttpUrl(jsfCDIFlowsServer, contextRoot, "");
        JSF22FlowsTests.testNestedFlows("mixedNested1", "mixedNested2", "mixedNested", url);
    }

    /**
     * Verify that we can define and use a custom navigation handler
     *
     * @throws Exception
     */
    //@Test
    public void JSF22Flows_TestCustomNavigationHandler() throws Exception {
        // Still running into an NPE here; we need to check for that in NavigationHandlerImpl
        URL url = JSFUtils.createHttpUrl(jsfCDIFlowsServer, "JSF22FacesFlowsNavigation", "");
        JSF22FlowsTests.testSimpleCase("customNavigationHandler", url);
    }

    /**
     * Verify the FlowBuilder initializer() and finalizer()
     *
     * @throws Exception
     */
    @Test
    public void JSF22Flows_TestInitializerAndFinalizer() throws Exception {
        /*
         * 1. Navigate to first page
         * 2. verify that <Current testBean.testValue value: test String> is on the page
         * 3. enter something into textbox, navigate to page 2, verify page2 info is the same
         * 4. navigate to return page
         * 5. verify that <Count: 1> is on the page
         * 6. return home
         */
        testInitializerAndFinalizer();
    }

    /**
     * Verify the FlowBuilder initializer() and finalizer()
     *
     * @throws Exception
     */
    @Test
    public void JSF22Flows_TestProgrammaticSwitch() throws Exception {
        URL url = JSFUtils.createHttpUrl(jsfCDIFlowsServer, contextRoot, "");
        JSF22FlowsTests.testFlowSwitch("programmaticSwitch", url);
    }

    /**
     * Helper method to test the initializer and finalizer application
     */
    private void testInitializerAndFinalizer() throws Exception {
        // Navigate to the index
        try (WebClient webClient = JSF22FlowsTests.getWebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfCDIFlowsServer, contextRoot, "");
            HtmlPage page = JSF22FlowsTests.getIndex(webClient, url);
            String flowID = "initializeFinalize";

            /*
             * Navigate to the first page and make sure the initialize() method set testBean correctly
             */
            page = JSF22FlowsTests.findAndClickButton(page, flowID);
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("initialize() and finalize() flow page 1"));
            JSF22FlowsTests.assertInFlow(page, flowID);
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Current flowscope value: no flowscope value"));
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Current testBeanInitFinalize.testValue value: test string"));

            // Assign flowscope value
            HtmlInput inputField = (HtmlInput) page.getElementById("inputValue");
            inputField.setValueAttribute("test string");

            // Click submit: we should navigate to page 2, and the bean values should persist
            page = JSF22FlowsTests.findAndClickButton(page, "button1");
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("initialize() and finalize() flow page 2"));
            JSF22FlowsTests.assertInFlow(page, flowID);
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Current flowscope value: test string"));
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Current testBeanInitFinalize.testValue value: test string"));

            // Exit flow to the return page;
            // Make sure that the finalize() method was called correctly (the printed count text will update)
            page = JSF22FlowsTests.findAndClickButton(page, "button2");
            JSF22FlowsTests.assertNotInFlow(page);
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Count: 1"));
        }
    }
}
