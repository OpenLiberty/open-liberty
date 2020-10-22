/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf.container.fat.tests;

import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jsf.container.fat.FATSuite;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

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
public class CDIFlowsTests extends FATServletClient {

    private static final String MOJARRA_APP = "JSF22CDIFacesFlows";
    private static final String MYFACES_APP = "JSF22CDIFacesFlows_MyFaces";

    public static LibertyServer server = JSF22FlowsTests.server;

    @BeforeClass
    public static void setup() throws Exception {
        server.removeAllInstalledAppsForValidation();

        WebArchive mojarraApp = ShrinkWrap.create(WebArchive.class, MOJARRA_APP + ".war")
                        .addPackage("jsf.cdi.flow.beans");
        mojarraApp = FATSuite.addMojarra(mojarraApp);
        mojarraApp = (WebArchive) ShrinkHelper.addDirectory(mojarraApp, "test-applications/" + MOJARRA_APP + "/resources");
        ShrinkHelper.exportToServer(server, "dropins", mojarraApp);
        server.addInstalledAppForValidation(MOJARRA_APP);

        WebArchive myfacesApp = ShrinkWrap.create(WebArchive.class, MYFACES_APP + ".war")
                        .addPackage("jsf.cdi.flow.beans");
        myfacesApp = FATSuite.addMyFaces(myfacesApp);
        myfacesApp = (WebArchive) ShrinkHelper.addDirectory(myfacesApp, "test-applications/" + MOJARRA_APP + "/resources");
        ShrinkHelper.exportToServer(server, "dropins", myfacesApp);
        server.addInstalledAppForValidation(MYFACES_APP);

        server.startServer(CDIFlowsTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        server.stopServer();
    }

    /**
     * Ensure that each app is running with the appropriate provider so that
     * we don't accidentally run both apps with a single provider.
     */
    @Test
    public void verifyAppProviders() throws Exception {
        server.resetLogMarks();
        server.waitForStringInLogUsingMark("Initializing Mojarra .* for context '/" + MOJARRA_APP + "'");
        server.resetLogMarks();
        server.waitForStringInLogUsingMark("MyFaces CDI support enabled");
    }

    /**
     * Verify the behavior of a simple flow which is defined via a *-flow.xml configuration,
     * and which utilizes a simple flowScoped bean
     */
    @Test
    public void JSF22Flows_TestSimpleBean() throws Exception {
        JSF22FlowsTests.testSimpleCase("simpleBean", MOJARRA_APP);
    }

    @Test
    public void JSF22Flows_TestSimpleBean_MyFaces() throws Exception {
        JSF22FlowsTests.testSimpleCase("simpleBean", MYFACES_APP);
    }

    /**
     * Verify the behavior of a simple flow which is defined entirely programmatically
     */
    @Test
    public void JSF22Flows_TestFlowBuilder() throws Exception {
        JSF22FlowsTests.testSimpleCase("simpleFlowBuilder", MOJARRA_APP);
    }

    @Test
    public void JSF22Flows_TestFlowBuilder_MyFaces() throws Exception {
        JSF22FlowsTests.testSimpleCase("simpleFlowBuilder", MYFACES_APP);
    }

    /**
     * Verify the behavior of nested flow set in which one flow is defined declaratively and another is
     * defined programmatically
     */
    @Test
    public void JSF22Flows_TestMixedConfiguration() throws Exception {
        JSF22FlowsTests.testNestedFlows("mixedNested1", "mixedNested2", "mixedNested", MOJARRA_APP);
    }

    @Test
    public void JSF22Flows_TestMixedConfiguration_MyFaces() throws Exception {
        JSF22FlowsTests.testNestedFlows("mixedNested1", "mixedNested2", "mixedNested", MYFACES_APP);
    }

    /**
     * Verify the FlowBuilder initializer() and finalizer()
     * 1. Navigate to first page
     * 2. verify that <Current testBean.testValue value: test String> is on the page
     * 3. enter something into textbox, navigate to page 2, verify page2 info is the same
     * 4. navigate to return page
     * 5. verify that <Count: 1> is on the page
     * 6. return home
     */
    @Test
    public void JSF22Flows_TestInitializerAndFinalizer() throws Exception {
        testInitializerAndFinalizer(MOJARRA_APP);
    }

    @Test
    public void JSF22Flows_TestInitializerAndFinalizer_MyFaces() throws Exception {
        testInitializerAndFinalizer(MYFACES_APP);
    }

    /**
     * Verify the FlowBuilder initializer() and finalizer()
     */
    @Test
    public void JSF22Flows_TestProgrammaticSwitch() throws Exception {
        JSF22FlowsTests.testFlowSwitch("programmaticSwitch", MOJARRA_APP);
    }

    @Test
    public void JSF22Flows_TestProgrammaticSwitch_MyFaces() throws Exception {
        JSF22FlowsTests.testFlowSwitch("programmaticSwitch", MYFACES_APP);
    }

    /**
     * Helper method to test the initializer and finalizer application
     */
    private void testInitializerAndFinalizer(String app) throws Exception {
        // Navigate to the index
        WebClient webClient = JSF22FlowsTests.getWebClient();
        HtmlPage page = JSF22FlowsTests.getIndex(webClient, app);
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
