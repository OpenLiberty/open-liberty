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
import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf22.fat.JSFUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests to execute on the jsfTestServer1 that use HtmlUnit.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JSF22ViewActionAndPhaseIdTests {
    @Rule
    public TestName name = new TestName();

    String contextRoot = "TestJSF22ViewAction";

    protected static final Class<?> c = JSF22ViewActionAndPhaseIdTests.class;

    @Server("jsfTestServer1")
    public static LibertyServer jsfTestServer1;

    @BeforeClass
    public static void setup() throws Exception {

        ShrinkHelper.defaultDropinApp(jsfTestServer1, "TestJSF22ViewAction.war", "com.ibm.ws.jsf22.fat.viewaction.*");

        jsfTestServer1.startServer(JSF22ViewActionAndPhaseIdTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsfTestServer1 != null && jsfTestServer1.isStarted()) {
            jsfTestServer1.stopServer();
        }
    }

    /**
     * Test the viewAction in it's default behavior, which will not be called on a postback.
     * This test will first reset the counter in the first viewAction call to a value of zero.
     * The second viewAction will increment a counter on an initial page load but not on a postback.
     * Therefore, when the page loads for the first time, the counter will be set to zero (0), but then
     * also incremented to one (1) at the initial rendering since this is not a postback.
     * By clicking the command button on the page will result in a postback call, and we should NOT see the counter increment because this is a postback.
     * Also, the resetCounter method should not be called for the same reason.
     *
     * @throws Exception
     */
    @Test
    public void testViewActionDefault() throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "testViewActionDefault.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Navigating to: /TestJSF22ViewAction/testViewActionDefault.jsf");

            // ensure the initial value of postback is false and count is 1.
            HtmlElement postBackCheck = (HtmlElement) page.getElementById("form1:postBackCheck");
            Log.info(c, name.getMethodName(), "initial page load postback value : " + postBackCheck);
            Assert.assertEquals("false", postBackCheck.asText());
            HtmlElement countDisplay = (HtmlElement) page.getElementById("form1:countDislay");
            Log.info(c, name.getMethodName(), "initial page load count value : " + countDisplay);
            Assert.assertEquals("1", countDisplay.asText());

            HtmlElement button = (HtmlElement) page.getElementById("form1:countButton");
            page = button.click();

            postBackCheck = (HtmlElement) page.getElementById("form1:postBackCheck");
            countDisplay = (HtmlElement) page.getElementById("form1:countDislay");

            Log.info(c, name.getMethodName(), "After button click, postback value : " + postBackCheck.asText());
            Assert.assertEquals("true", postBackCheck.asText());

            //should not increment on a postback.
            Log.info(c, name.getMethodName(), "After button click, count value : " + countDisplay);
            Assert.assertEquals("1", countDisplay.asText());
        }
    }

    /**
     * Test the viewAction attribute onPostback.
     * This test will first reset the counter in the first viewAction call to a value of zero.
     * The second viewAction will increment a counter on both an initial page load and also on a postback.
     * Therefore, when the page loads for the first time, the counter will be set to zero (0), but then also incremented to one (1) at the initial rendering.
     * By clicking the command button on the page will result in a postback call, and we should see the counter increment to two (2) because the bean's incrementCounter method
     * was called (because of the postback attribute), but the resetCounter method was not called (because by default viewActions are not called on postbacks.
     *
     * @throws Exception
     */
    @Test
    public void testViewActionPostback() throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "testViewActionPostback.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Navigating to: /TestJSF22ViewAction/testViewActionPostback.jsf");

            // ensure the initial value of postback is false and count is 1.
            HtmlElement postBackCheck = (HtmlElement) page.getElementById("form1:postBackCheck");
            Log.info(c, name.getMethodName(), "initial page load postback value : " + postBackCheck);
            Assert.assertEquals("false", postBackCheck.asText());
            HtmlElement countDisplay = (HtmlElement) page.getElementById("form1:countDislay");
            Log.info(c, name.getMethodName(), "initial page load count value : " + countDisplay);
            Assert.assertEquals("1", countDisplay.asText());

            HtmlElement button = (HtmlElement) page.getElementById("form1:countButton");
            page = button.click();

            postBackCheck = (HtmlElement) page.getElementById("form1:postBackCheck");
            countDisplay = (HtmlElement) page.getElementById("form1:countDislay");

            Log.info(c, name.getMethodName(), "After button click, postback value : " + postBackCheck);
            Assert.assertEquals("true", postBackCheck.asText());

            //should increment on a postback.
            Log.info(c, name.getMethodName(), "After button click, count value : " + countDisplay);
            Assert.assertEquals("2", countDisplay.asText());
        }
    }

    /**
     *
     * Test the viewAction by verifying a number entered is within the range.
     * This also uses a navigation rule to route the request to the proper page.
     *
     * @throws Exception
     */
    @Test
    public void testViewActionNavigationValid() throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "testViewActionNavigation.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Navigating to: /TestJSF22ViewAction/testViewActionNavigation.jsf");

            // enter a valid number between 1 and 100
            HtmlTextInput input = (HtmlTextInput) page.getElementById("form1:inputNumber");
            input.type("8", false, false, false);

            HtmlElement button = (HtmlElement) page.getElementById("form1:button1");
            page = button.click();

            HtmlElement output = (HtmlElement) page.getElementById("formViewActionResult:outputNumber1");

            Log.info(c, name.getMethodName(), "value expected is 8, actual value : " + output);
            Assert.assertEquals("8", output.asText());
        }
    }

    /**
     *
     * Test the viewAction by verifying a number entered is within the range.
     * This case will be a number outide the range.
     * This also uses a navigation rule to route the request to the proper page.
     *
     * @throws Exception
     */
    @Test
    public void testViewActionNavigationInvalid() throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "testViewActionNavigation.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Navigating to: /TestJSF22ViewAction/testViewActionNavigation.jsf");

            // enter an invalid number between 1 and 100
            HtmlTextInput input = (HtmlTextInput) page.getElementById("form1:inputNumber");
            input.type("9999", false, false, false);

            HtmlElement button = (HtmlElement) page.getElementById("form1:button1");
            page = button.click();

            Log.info(c, name.getMethodName(), "Entered 9999, should see the message 'The number you entered is invalid.'");
            assertTrue(page.asText().contains("The number you entered is invalid."));
        }
    }

    /**
     * Request a page with the f:viewAction immediate attribute set to true.
     * This should be called in the APPLY_REQUEST_VALUES phase.
     * Also, this will test the PhaseId's new methods, getName() and phaseIdValueOf(String)
     *
     * @throws Exception
     */
    @Test
    public void testViewActionImmediate() throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "testViewActionImmediate.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Navigating to: /TestJSF22ViewAction/testViewActionImmediate.jsf");
            assertTrue(page.asText().contains("PhaseId.getName(): APPLY_REQUEST_VALUES PhaseId.phaseIdValueOf(): APPLY_REQUEST_VALUES(2)"));
        }
    }

    /**
     * Request a page with the f:viewAction phase attribute set to APPLY_REQUEST_VALUES
     * Also, this will test the PhaseId's new methods, getName() and phaseIdValueOf(String)
     *
     * @throws Exception
     */
    @Test
    public void testViewActionARVPhase() throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "testViewActionARVPhase.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Navigating to: /TestJSF22ViewAction/testViewActionARVPhase.jsf");
            assertTrue(page.asText().contains("PhaseId.getName(): APPLY_REQUEST_VALUES PhaseId.phaseIdValueOf(): APPLY_REQUEST_VALUES(2)"));
        }
    }

    /**
     * Request a page with the f:viewAction phase attribute set to PROCESS_VALIDATIONS
     * Also, this will test the PhaseId's new methods, getName() and phaseIdValueOf(String)
     *
     * @throws Exception
     */
    @Test
    public void testViewActionPVPhase() throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "testViewActionPVPhase.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Navigating to: /TestJSF22ViewAction/testViewActionPVPhase.jsf");
            assertTrue(page.asText().contains("PhaseId.getName(): PROCESS_VALIDATIONS PhaseId.phaseIdValueOf(): PROCESS_VALIDATIONS(3)"));
        }
    }

    /**
     * Request a page with the f:viewAction phase attribute set to UPDATE_MODEL_VALUES
     * Also, this will test the PhaseId's new methods, getName() and phaseIdValueOf(String)
     *
     * @throws Exception
     */
    @Test
    public void testViewActionUMVPhase() throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "testViewActionUMVPhase.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Navigating to: /TestJSF22ViewAction/testViewActionUMVPhase.jsf");
            assertTrue(page.asText().contains("PhaseId.getName(): UPDATE_MODEL_VALUES PhaseId.phaseIdValueOf(): UPDATE_MODEL_VALUES(4)"));
        }
    }

    /**
     * Request a page with the f:viewAction phase attribute set to UPDATE_MODEL_VALUES
     * Also, this will test the PhaseId's new methods, getName() and phaseIdValueOf(String)
     *
     * @throws Exception
     */
    @Test
    public void testViewActionIAPhase() throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "testViewActionIAPhase.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Navigating to: /TestJSF22ViewAction/testViewActionIAPhase.jsf");
            assertTrue(page.asText().contains("PhaseId.getName(): INVOKE_APPLICATION PhaseId.phaseIdValueOf(): INVOKE_APPLICATION(5)"));
        }
    }

    /**
     * Request a simple JSF page that will show messages from two phase listeners for the RESTORE_VIEW
     * and RENDER_RESPONSE phases which could not be tested with the viewAction phase attribute.
     * This will test the PhaseId's new methods, getName() and phaseIdValueOf(String).
     *
     * @throws Exception
     */
    @Test
    public void testRestoreViewRenderResponsePhase() throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "testRestoreViewRenderResponsePhase.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Navigating to: /TestJSF22ViewAction/testRestoreViewRenderResponsePhase.jsf");
            assertTrue(page.asText().contains("PhaseListener Message: PhaseId.getName(): RESTORE_VIEW PhaseId.phaseIdValueOf(): RESTORE_VIEW(1)"));
            assertTrue(page.asText().contains("PhaseListener Message: PhaseId.getName(): RENDER_RESPONSE PhaseId.phaseIdValueOf(): RENDER_RESPONSE(6)"));
        }
    }

    /**
     * Request a simple JSF page that contains an empty f:metadata component.
     * A phase listener is called for all phases and will add a message on each phase.
     * In this case, only the RESTORE_VIEW and RENDER_RESPONSE phases should be called.
     *
     * @throws Exception
     */
    @Test
    public void testEmptyMetatdata() throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "testEmptyMetadata.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Navigating to: /TestJSF22ViewAction/testEmptyMetadata.jsf");
            assertTrue(page.asText().contains("Metadata test: RESTORE_VIEW"));
            assertFalse(page.asText().contains("Metadata test: APPLY_REQUEST_VALUES"));
            assertFalse(page.asText().contains("Metadata test: PROCESS_VALIDATIONS"));
            assertFalse(page.asText().contains("Metadata test: UPDATE_MODEL_VALUES"));
            assertFalse(page.asText().contains("Metadata test: INVOKE_APPLICATION"));
            assertTrue(page.asText().contains("Metadata test: RENDER_RESPONSE"));
        }
    }

    /**
     * Request a simple JSF page that contains an non-empty f:metadata component (contains a viewParam component).
     * A phase listener is called for all phases and will add a message on each phase.
     * In this case, all of the phases should be called.
     *
     * @throws Exception
     */
    @Test
    public void testNonEmptyMetatdata() throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "testNonEmptyMetadata.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Navigating to: /TestJSF22ViewAction/testNonEmptyMetadata.jsf");
            assertTrue(page.asText().contains("Metadata test: RESTORE_VIEW"));
            assertTrue(page.asText().contains("Metadata test: APPLY_REQUEST_VALUES"));
            assertTrue(page.asText().contains("Metadata test: PROCESS_VALIDATIONS"));
            assertTrue(page.asText().contains("Metadata test: UPDATE_MODEL_VALUES"));
            assertTrue(page.asText().contains("Metadata test: INVOKE_APPLICATION"));
            assertTrue(page.asText().contains("Metadata test: RENDER_RESPONSE"));
        }
    }
}
