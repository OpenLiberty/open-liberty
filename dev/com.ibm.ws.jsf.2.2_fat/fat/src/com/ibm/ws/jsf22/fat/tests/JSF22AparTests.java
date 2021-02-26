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

import static componenttest.annotation.SkipForRepeat.EE8_FEATURES;
import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Arrays;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlHiddenInput;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf22.fat.JSFUtils;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;
import junit.framework.Assert;

/**
 * Tests to execute on the jsfAparServer that use HtmlUnit.
 * Only execute on Java7 since the jsf-2.2 feature requires servlet-3.1
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JSF22AparTests {
    @Rule
    public TestName name = new TestName();

    protected static final Class<?> c = JSF22AparTests.class;

    @Server("jsfAparServer")
    public static LibertyServer jsfAparServer;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(jsfAparServer, "PI47600.war", "com.ibm.ws.jsf22.fat.tests.PI47600");

        JavaArchive PI30335Jar = ShrinkHelper.buildJavaArchive("PI30335.jar", "com.ibm.ws.jsf22.fat.tests.PI30335");
        WebArchive PI30335DefaultWar = ShrinkHelper.buildDefaultApp("PI30335_Default.war", "");
        PI30335DefaultWar.addAsLibraries(PI30335Jar);
        ShrinkHelper.exportDropinAppToServer(jsfAparServer, PI30335DefaultWar);

        WebArchive PI30335FalseWar = ShrinkHelper.buildDefaultApp("PI30335_False.war", "");
        PI30335FalseWar.addAsLibraries(PI30335Jar);
        ShrinkHelper.exportDropinAppToServer(jsfAparServer, PI30335FalseWar);

        JavaArchive PH01566Jar = ShrinkHelper.buildJavaArchive("PH01566.jar", "");
        WebArchive PH01566War = ShrinkHelper.buildDefaultApp("PH01566.war", "com.ibm.ws.jsf22.fat.PH01566");
        PH01566War.addAsLibraries(PH01566Jar);
        ShrinkHelper.exportDropinAppToServer(jsfAparServer, PH01566War);

        ShrinkHelper.defaultDropinApp(jsfAparServer, "PI50108.war", "com.ibm.ws.jsf22.fat.tests.PI50108");

        ShrinkHelper.defaultDropinApp(jsfAparServer, "PI46218Flow1.war", "com.ibm.ws.jsf22.fat.PI46218Flow1.*");

        ShrinkHelper.defaultDropinApp(jsfAparServer, "PI46218Flow2.war", "com.ibm.ws.jsf22.fat.PI46218Flow2.*");

        ShrinkHelper.defaultDropinApp(jsfAparServer, "PI57255CDI.war", "com.ibm.ws.jsf22.fat.PI57255.*");

        ShrinkHelper.defaultDropinApp(jsfAparServer, "PI59422.war", "com.ibm.ws.jsf22.fat.PI59422");

        ShrinkHelper.defaultDropinApp(jsfAparServer, "PI63135.war", "com.ibm.ws.jsf22.fat.PI63135");

        ShrinkHelper.defaultDropinApp(jsfAparServer, "ViewScopedCDIBean.war", "com.ibm.ws.jsf22.fat.viewscopedcdi");

        ShrinkHelper.defaultDropinApp(jsfAparServer, "ViewScopedJSFBean.war", "com.ibm.ws.jsf22.fat.viewscopedjsf");

        ShrinkHelper.defaultDropinApp(jsfAparServer, "PI64714.war", "com.ibm.ws.jsf22.fat.PI64714");

        ShrinkHelper.defaultDropinApp(jsfAparServer, "PI64718.war", "com.ibm.ws.jsf22.fat.PI64718");

        ShrinkHelper.defaultDropinApp(jsfAparServer, "PI67525.war", "com.ibm.ws.jsf22.fat.PI67525");

        ShrinkHelper.defaultDropinApp(jsfAparServer, "PI85492.war", "com.ibm.ws.jsf22.fat.PI85492");

        ShrinkHelper.defaultDropinApp(jsfAparServer, "PI89363.war", "com.ibm.ws.jsf22.fat.PI89363");

        ShrinkHelper.defaultDropinApp(jsfAparServer, "PI79562.war", "");

        ShrinkHelper.defaultDropinApp(jsfAparServer, "PI57255Default.war", "");

        ShrinkHelper.defaultDropinApp(jsfAparServer, "PI51038.war", "");

        ShrinkHelper.defaultDropinApp(jsfAparServer, "PI51038_Default.war", "");

        ShrinkHelper.defaultDropinApp(jsfAparServer, "PI89363StrictJSF2.war", "");

        ShrinkHelper.defaultDropinApp(jsfAparServer, "PI89168.war", "");

        ShrinkHelper.defaultDropinApp(jsfAparServer, "PI90507.war", "com.ibm.ws.jsf22.fat.PI90507");

        ShrinkHelper.defaultDropinApp(jsfAparServer, "PI90391.war", "");

        ShrinkHelper.defaultDropinApp(jsfAparServer, "PH06008.war", "");

        jsfAparServer.startServer(JSF22AparTests.class.getSimpleName() + ".log");

    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsfAparServer != null && jsfAparServer.isStarted()) {
            /*
             * restartDropinsApplication may cause a warning in the logs.
             * CWWKZ0014W: The application PI90507 could not be started as it could not be found at location
             * It should ignored by the server for now.
             */
            jsfAparServer.stopServer("CWWKZ0014W");
        }
    }

    protected void verifyResponse(String contextRoot, String resource, String expectedResponse, LibertyServer server) throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(server, contextRoot, resource);
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            if (page == null) {
                Assert.fail(resource + " did not render properly.");
            }

            if (!page.asText().contains(expectedResponse)) {
                Assert.fail("The page did not contain the following expected response: " + expectedResponse);
            }
        }
    }

    /**
     * This will test that, in a facelet, a "class" attribute can be attached to a custom tag.
     * Prior to PI47600, doing that resulted in the "custom property class is not writable" exception.
     *
     * @throws Exception
     */
    @Test
    public void testPI47600() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfAparServer, "PI47600", "");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            assertTrue("testPI47600:: the app output was null!", page != null);
            Log.info(c, name.getMethodName(), "testPI47600:: page --> " + page.asXml());

            assertFalse("testPI47600:: Component property class not writable exception thrown!",
                        page.asText().contains("Component property class is not writable"));

            assertTrue("testPI47600:: the 'class' attribute was not rendered correctly",
                       page.asXml().contains("class=\"custom_tag\""));
        }
    }

    /**
     * This will test the default behavior of PI30335 with the jsf-2.2 feature. The com.ibm.ws.jsf_fat_jsf2.2 will already
     * execute the PI30335 test case with the custom property set to true. This test is to ensure that not setting the custom
     * property with the jsf-2.2 feature will result in the same behavior.
     *
     * @throws Exception
     */
    @Test
    public void testPI30335DefaultBehavior() throws Exception {
        String msgToSearchFor = "ManagedBean Ref: com.ibm.ws.jsf22.fat.tests.PI30335.ManagedBean2";

        this.verifyResponse("PI30335_Default", "", "ManagedBean1", jsfAparServer);

        // Check the logs to see if the message was found.
        String managedBeanReference = jsfAparServer.waitForStringInLog(msgToSearchFor);

        // There should be a match so fail if there is not
        assertNotNull("The following message was not found in the logs: " + msgToSearchFor, managedBeanReference);
    }

    /**
     * This will test the behavior of PI30335 with the custom property set to false with the jsf-2.2 feature.
     *
     * @throws Exception
     */
    @Test
    public void testPI30335PropertySetToFalse() throws Exception {
        String msgToSearchFor = "ManagedBean Ref: null";

        this.verifyResponse("PI30335_False", "", "ManagedBean1", jsfAparServer);

        // Check the logs to see if the message was found
        String managedBeanReference = jsfAparServer.waitForStringInLog(msgToSearchFor);

        // There should be a match so fail if there is not
        assertNotNull("The following message was not found in the logs: " + msgToSearchFor, managedBeanReference);
    }

    /**
     * This tests PI50108: ViewScope Binding should function correctly. We'll check that the test page
     * is written correctly, and that the correct messages are written to SystemOut when a button is clicked
     *
     * @throws Exception
     */
    @Test
    public void testPI50108() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfAparServer, "PI50108", "");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Make sure the test page was rendered correctly
            assertTrue("testPI50108:: the app output was null!", page != null);
            Log.info(c, name.getMethodName(), "testPI50108:: page --> " + page.asXml());
            assertTrue("testPI50108:: Page was not written!",
                       page.asText().contains("ViewScope Binding Test"));

            // Check the logs to see if the "Post Construct fired!!" message was found.
            String logReference = jsfAparServer.waitForStringInLog("Post Construct fired!!");
            assertNotNull("The following message was not found in the logs: " + "Post Construct fired!!", logReference);

            // Make sure the test page was rendered correctly
            page = ((HtmlElement) page.getElementById("testTable:1:testButton")).click();
            assertTrue("testPI50108:: Page was not written!",
                       page.asText().contains("ViewScope Binding Test"));

            // Check the logs to see if the "Clicked!!" message was printed out
            logReference = jsfAparServer.waitForStringInLog("Clicked!!");
            assertNotNull("The following message was not found in the logs: " + "Clicked!!", logReference);
        }
    }

    /**
     * This tests PI51038: the EL 3.0 ImportHandler should function correctly.
     *
     * @throws Exception
     */
    @Test
    public void testPI51038() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfAparServer, "PI51038", "");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Make sure the test page was rendered correctly
            assertTrue("testPI51038:: the app output was null!", page != null);
            Log.info(c, name.getMethodName(), "testPI51038:: page --> " + page.asXml());
            assertTrue("testPI51038:: Page was not written!",
                       page.asText().contains("EL ImportHandler Test"));

            assertTrue("testPI51038:: Int value not returned correctly!",
                       page.asText().contains("Zeroes trailing 16 base 2: 4"));
            assertTrue("testPI51038:: Lambda comparison failed!!",
                       page.asText().contains("Is 5 gt 6: 0"));
        }
    }

    /**
     * This tests PI51038 default behavior: the EL 3.0 ImportHandler should NOT function correctly,
     * since the new context parameter is left to its default value of false
     *
     * @throws Exception
     */
    @Test
    public void testPI51038_Default() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfAparServer, "PI51038_Default", "");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Make sure the test page was rendered correctly
            assertTrue("testPI51038_Default:: the app output was null!", page != null);
            Log.info(c, name.getMethodName(), "testPI51038:: page --> " + page.asXml());
            assertTrue("testPI51038_Default:: Page was not written!",
                       page.asText().contains("EL ImportHandler Test"));

            assertTrue("testPI51038_Default:: Incorrect output",
                       page.asText().contains("Zeroes trailing 16 base 2:"));
            // We're expecting the EL import to fail, so the correct value shouldn't be printed here
            assertTrue("testPI51038_Default:: A value was returned when it shouldn't have been!!",
                       !page.asText().contains("4"));
            assertTrue("testPI51038_Default:: Incorrect output!!",
                       page.asText().contains("Is 5 gt 6:"));
            // Again, we don't want to see the EL expression evaluated here
            assertTrue("testPI51038_Default:: A value was returned when it shouldn't have been!!",
                       !page.asText().contains("0"));
        }
    }

    /**
     * PI46218 tests
     * (1) multiple beans can share the same name across different web modules, and
     * (2) Faces Flows are discoverable in different WAR files of the same EAR file
     *
     * @throws Exception
     */
    @Test
    public void testPI46218() throws Exception {
        testSimpleFlow("/PI46218Flow1", "test-flow-1");
        testSimpleFlow("/PI46218Flow2", "test-flow-2");
    }

    /***
     * Tests that CDI is available after a non-CDI application is loaded first.
     *
     * To load the non-CDI application first, a request is made to the non-CDI application
     * after a server restart.
     */
    @Test
    public void testPI57255() throws Exception {
        URL url = JSFUtils.createHttpUrl(jsfAparServer, "PI57255Default", "");

        // Need to restart the applications to make sure the non-CDI application is loaded first
        Assert.assertTrue("The PI57255Default.war application was not restarted.", jsfAparServer.restartDropinsApplication("PI57255Default.war"));
        Assert.assertTrue("The PI57255CDI.war application was not restarted.", jsfAparServer.restartDropinsApplication("PI57255CDI.war"));

        try (WebClient webClient = new WebClient()) {
            HtmlPage page;

            // Make a request to the non-CDI application first to make this application load first
            Log.info(c, name.getMethodName(), "Making a request to " + url);
            page = webClient.getPage(url);
            assertTrue("Page did not have expected response: Hello World", page.asText().contains("Hello World"));

            // Use a flow to test that CDI is available for the CDI-enabled application
            testSimpleFlow("/PI57255CDI", "sample-flow");
        }
    }

    /***
     * Tests that the flow finalizer is called before the flows beans are destroyed as required
     * by the specification.
     *
     * @throws Exception
     */
    @Test
    public void testPI59422() throws Exception {
        URL url = JSFUtils.createHttpUrl(jsfAparServer, "PI59422", "index.xhtml");

        // The messages written to the logs need to occur in the following order:
        final String[] messages = new String[] {
                                                 "PI59422: SampleFlow initialized.",
                                                 "PI59422: PostConstruct was invoked!",
                                                 "PI59422: SampleFlow finalized.",
                                                 "PI59422: PreDestroy was invoked!"
        };

        try (WebClient webClient = new WebClient()) {
            HtmlPage page;

            Log.info(c, name.getMethodName(), "Making a request to " + url);
            page = webClient.getPage(url);

            page = findAndClickButton(page, "form1:enterTestFlowButton");
            page = findAndClickButton(page, "form1:homeButton");

            // Search the logs to make sure the messages are logged in the correct order
            for (String str : messages) {
                String ret = jsfAparServer.waitForStringInLogUsingLastOffset(str);
                assertTrue("Did not find the log message in the expected order: " + str, ret != null);
            }
        }
    }

    /*
     * A helper method for PI46218 and PI57255 that will step through a flow and perform various checks.
     */
    private void testSimpleFlow(String contextRoot, String flowId) throws Exception {
        URL url = JSFUtils.createHttpUrl(jsfAparServer, contextRoot, "");
        final String message = "flow-test";
        try (WebClient webClient = new WebClient()) {
            HtmlPage page;

            Log.info(c, name.getMethodName(), "Beginning test for " + flowId);

            Log.info(c, name.getMethodName(), "Making a request to " + url);
            page = webClient.getPage(url);
            assertTrue("The page should not have a flow ID!", verifyFlowId(page, "no flow ID"));

            // Enter the flow and verify the flow ID is correct
            page = findAndClickButton(page, "form1:enterTestFlowButton");
            assertTrue("The page should have a flow ID \'" + flowId + "\'", verifyFlowId(page, flowId));

            // Enter a message into the text input and advance to the next flow
            ((HtmlTextInput) page.getElementById("form1:messageInput")).type(message);
            page = findAndClickButton(page, "form1:submitButton");
            assertTrue("The page should have a flow ID \'" + flowId + "\'", verifyFlowId(page, flowId));
            assertTrue("The page should contain the message \'" + message + "\'", page.asText().contains("The message entered was " + message));

            // Go back to the home view
            page = findAndClickButton(page, "form1:homeButton");
            assertTrue("The page should not have a flow ID!", verifyFlowId(page, "no flow ID"));
        }
    }

    /**
     * Helper method to verify that the page contains the right value for the flow ID
     */
    private static boolean verifyFlowId(HtmlPage page, String flowId) {
        return page.asText().contains("Flow ID: " + flowId);
    }

    /**
     * Helper method to find a button on the page, click it, and return the resulting page
     */
    private static HtmlPage findAndClickButton(HtmlPage page, String buttonId) throws Exception {
        return ((HtmlElement) page.getElementById(buttonId)).click();
    }

    @Test
    public void testPI63135() throws Exception {
        try (WebClient webClient = new WebClient()) {
            HtmlPage page;
            URL url = JSFUtils.createHttpUrl(jsfAparServer, "PI63135", "index.xhtml");

            Log.info(c, name.getMethodName(), "Making a request to " + url);
            page = webClient.getPage(url);
            page = findAndClickButton(page, "form1:submitButton");

            // Search the response to make sure the message is null
            final String expectedResponse = "PI63135: Message is NULL!";
            final String response = page.asText();
            assertTrue("Did not find the expected message in the response. Expected \'" + expectedResponse + "\' but found: " + response, response.contains(expectedResponse));
        }
    }

    /**
     * This test case will drive a request to a page that uses a ViewScoped JSF ManagedBean. A
     * button will then be clicked that will invalidate the session and the test will look to ensure
     * that the PreDestroy method on the JSF Managed Bean was invoked.
     *
     * @throws Exception
     */
    @Test
    public void testViewScopedJSFManagedBeanPreDestroy() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfAparServer, "ViewScopedJSFBean", "");

            Log.info(c, name.getMethodName(), "Making a request to " + url);

            HtmlPage page = webClient.getPage(url);

            page = findAndClickButton(page, "form1:button1");

            String str = "ViewScopedBean1 PreDestroy Invoked";
            String ret = jsfAparServer.waitForStringInLogUsingLastOffset(str);

            Log.info(c, name.getMethodName(), "Return value : " + ret);
            assertTrue("The PreDestroy method of the JSF ViewScoped bean was not invoked on session invalidation " + str, ret != null);
        }
    }

    /**
     * This test case will drive a request to a page that uses a ViewScoped JSF ManagedBean and
     * a CDI Named ViewScoped Bean. A button will then be clicked that will invalidate the session
     * and the test will look to ensure that the PreDestroy method on the CDI bean and JSF bean are
     * invoked.
     *
     * @throws Exception
     */
    @Test
    public void testViewScopedCDIManagedBeanPreDestroy() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfAparServer, "ViewScopedCDIBean", "");

            Log.info(c, name.getMethodName(), "Making a request to " + url);

            HtmlPage page = webClient.getPage(url);

            page = findAndClickButton(page, "form1:button1");

            String str1 = "ViewScopedBean2 PreDestroy Invoked";
            String str2 = "ViewScopedCDIBean PreDestroy Invoked";

            String ret1 = jsfAparServer.waitForStringInLog(str1);
            String ret2 = jsfAparServer.waitForStringInLog(str2);

            Log.info(c, name.getMethodName(), "Return value 1: " + ret1);
            Log.info(c, name.getMethodName(), "Return value 2: " + ret2);

            assertTrue("The PreDestroy methods of the JSF ViewScoped bean and CDI "
                       + "ViewScoped bean were not invoked on session invalidation. "
                       + "JSF: " + str1 + " CDI: " + str2, (ret1 != null) && (ret2 != null));
        }
    }

    /**
     * Inn the event of a validation error, JSF should not overwrite a user-configured message
     * severity. Previous behavior was that every message for the view would be overwritten to have a
     * severity of ERROR. In the case of this test, the message should retain its FATAL severity;
     * on the view, FATAL messages are assigned to a green text style, so we should find a green
     * message output
     *
     * If there is no text written to the form, a ValidatorException should be thrown. If text is
     * written, however, a Faces Message should be printed out.
     */
    @Test
    public void testPI64714() throws Exception {
        try (WebClient webClient = new WebClient()) {
            HtmlPage page;
            URL url = JSFUtils.createHttpUrl(jsfAparServer, "PI64714", "index.xhtml");

            Log.info(c, name.getMethodName(), "PI64714: Making a request to " + url);
            page = webClient.getPage(url);
            page = findAndClickButton(page, "form:execute");

            // we're expecting to find a green message on the page, with the given text
            final String color = "green";
            final String validatorException = "ValidatorException#SEVERITY_FATAL";
            String response = page.asXml();

            assertTrue("PI64714: A green validator exception should have been output, and it was not: \n\n"
                       + response, response.contains(color) && response.contains(validatorException));

            final String facesException = "FacesContext#SEVERITY_FATAL";

            // to get the faces message instead of the validator exception, we need to add some text
            HtmlInput intputBox = (HtmlInput) page.getHtmlElementById("form:input");
            intputBox.setValueAttribute("test string");
            page = findAndClickButton(page, "form:execute");
            response = page.asXml();

            assertTrue("PI64714: A green faces message should have been output, and it was not: \n"
                       + response, response.contains(color) && response.contains(facesException));
        }
    }

    /**
     * Verifies that validators defined for a UISelectMany component are invoked when there
     * are no selections made.
     *
     * The test page has four UISelectMany components, and each has three checkboxes. Validation
     * should fail unless exactly two checkboxes are selected for each component.
     */
    @Test
    public void testPI64718() throws Exception {
        try (WebClient webClient = new WebClient()) {
            HtmlPage page;
            URL url = JSFUtils.createHttpUrl(jsfAparServer, "PI64718", "index.xhtml");

            Log.info(c, name.getMethodName(), "PI64718: Making a request to " + url);
            page = webClient.getPage(url);
            page = findAndClickButton(page, "form:execute");

            // we're expecting to find these validator exceptions, which are defined in the app's index bean
            final String validatorException1 = "form:DATA1: size must be between 2 and 2";
            final String validatorException2 = "form:DATA2: size must be between 2 and 2";
            final String validatorException3 = "form:DATA3: ListSize is not 2";
            final String validatorException4 = "form:DATA4: ArraySize is not 2";

            String response = page.asXml();

            assertTrue("PI64718: the page response did not contain " + validatorException1,
                       response.contains(validatorException1));
            assertTrue("PI64718: the page response did not contain " + validatorException2,
                       response.contains(validatorException2));
            assertTrue("PI64718: the page response did not contain " + validatorException3,
                       response.contains(validatorException3));
            assertTrue("PI64718: the page response did not contain " + validatorException4,
                       response.contains(validatorException4));

            // we'll now check 2/4 boxes on all components except for the last one
            HtmlCheckBoxInput input = page.getHtmlElementById("form:DATA1:0");
            input.setChecked(true);
            input = page.getHtmlElementById("form:DATA1:2");
            input.setChecked(true);
            input = page.getHtmlElementById("form:DATA2:0");
            input.setChecked(true);
            input = page.getHtmlElementById("form:DATA2:1");
            input.setChecked(true);
            input = page.getHtmlElementById("form:DATA3:1");
            input.setChecked(true);
            input = page.getHtmlElementById("form:DATA3:2");
            input.setChecked(true);
            input = page.getHtmlElementById("form:DATA4:1");
            input.setChecked(true);
            page = findAndClickButton(page, "form:execute");
            response = page.asXml();

            // There should only be one validation error - form:DATA4 was only checked once
            assertTrue("PI64718: the page response should not contain: " + validatorException1,
                       !response.contains(validatorException1));
            assertTrue("PI64718: the page response should not contain: " + validatorException2,
                       !response.contains(validatorException2));
            assertTrue("PI64718: the page response should not contain: " + validatorException3,
                       !response.contains(validatorException3));
            assertTrue("PI64718: the page response did not contain: " + validatorException4,
                       response.contains(validatorException4));

            // Set 2/4 boxes on the last component
            input = page.getHtmlElementById("form:DATA4:0");
            input.setChecked(true);
            page = findAndClickButton(page, "form:execute");
            response = page.asXml();

            // There should now be no validation errors
            assertTrue("PI64718: the page response should not contain: " + validatorException1,
                       !response.contains(validatorException1));
            assertTrue("PI64718: the page response should not contain: " + validatorException2,
                       !response.contains(validatorException2));
            assertTrue("PI64718: the page response should not contain: " + validatorException3,
                       !response.contains(validatorException3));
            assertTrue("PI64718: the page response should not contain: " + validatorException4,
                       !response.contains(validatorException4));
        }
    }

    /**
     * This test case will drive a request to a page and verify that a Validation Error message is
     * shown when using the h:inputFile tag, the require attribute is set to true and
     * no file is attached.
     *
     * @throws Exception
     */
    @Test
    public void testPI67525() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfAparServer, "PI67525", "index.xhtml");

            Log.info(c, name.getMethodName(), "PI67525: Making a request to " + url);

            HtmlPage page = webClient.getPage(url);

            page = findAndClickButton(page, "form:execute");

            String validationErrorMsg = "Validation Error: Value is required";
            String response = page.asXml();

            assertTrue("PI67525: A validation error message should have been output, and it was not: \n"
                       + response, response.contains(validationErrorMsg));
        }
    }

    /**
     * Tests APAR PI79562. When javax.faces.WEBAPP_RESOURCES_DIRECTORY is set with a param-value that has a leading "/",
     * ensure that a StringOutOfBoundsException is not thrown, and that the page renders correctly.
     *
     * Note that javax.faces.WEBAPP_RESOURCES_DIRECTORY is not valid in JSF 2.0, so this FAT only targets the jsf-2.2 feature.
     *
     * @throws Exception
     */
    @Test
    public void testPI79562() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfAparServer, "PI79562", "faces/index.xhtml");
            // final String url = SHARED_SERVER.getServerUrl(true, "/PI79562/faces/index.xhtml");

            Log.info(c, name.getMethodName(), "PI79562: Making a request to " + url);

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Make sure the test page was rendered correctly
            assertTrue("PI79562: the app output was null!", page != null);

            // Check response
            assertTrue("PI79562: The expected text was not printed!\n" + page.asXml(),
                       page.asText().contains("PI79562: test passed"));
        }
    }

    /**
     * Tests APAR PI85492; make sure the output is not flushed too early in the RENDER_RESPONSE phase
     *
     * @throws Exception
     */
    @Test
    public void testPI85492() throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfAparServer, "PI85492", "");

            Log.info(c, name.getMethodName(), "PI85492: Making a request to " + url);

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Make sure the test page was rendered correctly
            assertTrue("PI85492: the app output was null!", page != null);

            String msgToSearchFor = "PI85492 Resonse commited = false";

            // Check the logs to see if the message was found.
            String msgResult = jsfAparServer.waitForStringInLog(msgToSearchFor, 20000);
            assertNotNull("PI85492: the response was committed too early.", msgResult);
        }
    }

    /**
     * Tests APAR PI89363; make sure a ProtectedViewException is NOT thrown
     * in Chrome browser when you are in a stateless view with
     * view protection set and Origin header is sent in the request.
     *
     * Context param STRICT_JSF_2_ORIGIN_HEADER_APP_PATH is set to false (default)
     * so we don't strictly want to follow JSF 2.x spec.
     *
     * @throws Exception
     */
    @Test
    public void testPI89363() throws Exception {
        try (WebClient webClient = new WebClient(BrowserVersion.CHROME)) {
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

            URL url = JSFUtils.createHttpUrl(jsfAparServer, "PI89363", "index.xhtml");

            Log.info(c, name.getMethodName(), "PI89363: Making a request to " + url);

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Make sure the test page was rendered correctly
            assertTrue("PI89363: the app output was null!", page != null);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), "testPI89363:: initial page --> " + page.asXml());

            // Get the anchor and click
            HtmlAnchor goToStatelessLink = (HtmlAnchor) page.getElementById("goToStateless");
            page = goToStatelessLink.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), "testPI89363:: stateless page --> " + page.asXml());

            // Check response
            assertTrue("PI89363: Stateless View response message was not found!\n" + page.asText(),
                       page.asText().contains("Stateless View"));

            // Add Origin header to the request header map to manually trigger the issue in https://issues.apache.org/jira/browse/MYFACES-4058
            String originHeader = url.toString().substring(0, url.toString().indexOf("/PI89363"));
            Log.info(c, name.getMethodName(), "PI89363: Origin Header --> " + originHeader);
            webClient.addRequestHeader("Origin", originHeader);

            // Get the form
            HtmlForm statelessForm = page.getFormByName("statelessForm");

            // Get the button and then click it
            HtmlSubmitInput statelessSubmitButton = statelessForm.getInputByName("statelessForm:statelessSubmitButton");
            page = statelessSubmitButton.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), "testPI89363:: final page --> " + page.asText());

            // Check that no ProtectedViewException was thrown
            assertTrue("PI89363: ProtectedViewException was thrown!\n" + page.asText(),
                       !page.asText().contains("ProtectedViewException"));
            // Check the final view response message
            assertTrue("PI89363: Final View response message was not found!\n" + page.asText(),
                       page.asText().contains("Final View"));
        }
    }

    /**
     * Tests APAR PI89363; make sure a ProtectedViewException is thrown
     * in Chrome browser when you are in a stateless view with
     * view protection set and Origin header is sent in the request.
     *
     * Context param STRICT_JSF_2_ORIGIN_HEADER_APP_PATH is set to true
     * so we strictly want to follow JSF 2.x spec.
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC({ "javax.servlet.ServletException" })
    public void testPI89363StrictJSF2() throws Exception {
        try (WebClient webClient = new WebClient(BrowserVersion.CHROME)) {
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

            jsfAparServer.addIgnoredErrors(Arrays.asList("SRVE0777E:.*"));
            URL url = JSFUtils.createHttpUrl(jsfAparServer, "PI89363StrictJSF2", "index.xhtml");

            Log.info(c, name.getMethodName(), "PI89363StrictJSF2: Making a request to " + url);

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Make sure the test page was rendered correctly
            assertTrue("PI89363StrictJSF2: the app output was null!", page != null);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), "testPI89363StrictJSF2:: initial page --> " + page.asXml());

            // Get the anchor and click
            HtmlAnchor goToStatelessLink = (HtmlAnchor) page.getElementById("goToStateless");
            page = goToStatelessLink.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), "testPI89363StrictJSF2:: stateless page --> " + page.asXml());

            // Check response
            assertTrue("PI89363StrictJSF2: Stateless View response message was not found!\n" + page.asText(),
                       page.asText().contains("Stateless View"));

            // Add Origin header to the request header map to manually trigger the issue in https://issues.apache.org/jira/browse/MYFACES-4058
            String originHeader = url.toString().substring(0, url.toString().indexOf("/PI89363StrictJSF2"));
            Log.info(c, name.getMethodName(), "PI89363StrictJSF2: Origin Header --> " + originHeader);
            webClient.addRequestHeader("Origin", originHeader);

            // Get the form
            HtmlForm statelessForm = page.getFormByName("statelessForm");

            // Get the button and then click it
            HtmlSubmitInput statelessSubmitButton = statelessForm.getInputByName("statelessForm:statelessSubmitButton");
            page = statelessSubmitButton.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), "testPI89363StrictJSF2:: final page --> " + page.asText());

            // Check that a ProtectedViewException was thrown
            assertTrue("PI89363StrictJSF2: ProtectedViewException was thrown!\n" + page.asText(),
                       page.asText().contains("ProtectedViewException"));
        }
    }

    /**
     * Tests APAR PI89168; make sure a FacesException is thrown when
     * you are in a stateful view and the value of hidden input javax.faces.ViewState
     * is changed to "stateless".
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC({ "javax.servlet.ServletException" })
    public void testPI89168() throws Exception {
        try (WebClient webClient = new WebClient()) {
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

            // Add expected ServletException
            String expectedSRVE0777E = "SRVE0777E";
            jsfAparServer.addIgnoredErrors(Arrays.asList(expectedSRVE0777E));

            URL url = JSFUtils.createHttpUrl(jsfAparServer, "PI89168", "index.xhtml");

            Log.info(c, name.getMethodName(), "PI89168: Making a request to " + url);

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Make sure the test page was rendered correctly
            assertTrue("PI89168: the app output was null!", page != null);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), "testPI89168:: initial page --> " + page.asXml());

            // Get the anchor and click
            HtmlAnchor goToStatefulLink = (HtmlAnchor) page.getElementById("goToStateful");
            page = goToStatefulLink.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), "testPI89168:: stateful page --> " + page.asXml());

            // Get the form
            HtmlForm statefulForm = page.getFormByName("statefulForm");

            // Change the value of ViewState to stateless
            HtmlHiddenInput viewStateInput = (HtmlHiddenInput) statefulForm.getInputByName((JakartaEE9Action.isActive() ? "jakarta." : "javax.") + "faces.ViewState");
            viewStateInput.setValueAttribute("stateless");

            // Get the button and then click it
            HtmlSubmitInput statefulSubmitButton = statefulForm.getInputByName("statefulForm:statefulSubmitButton");
            page = statefulSubmitButton.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), "testPI89168:: final page --> " + page.asText());

            // Check that a FacesException is thrown
            assertTrue("PI89168: FacesException was not thrown!\n" + page.asText(),
                       page.asText().contains((JakartaEE9Action.isActive() ? "jakarta." : "javax.") + "faces.FacesException: unable to create view \"/statefulView.xhtml\""));
        }
    }

    private void verifyFlowScopeValue(HtmlPage page, String message) {
        assertTrue("The flowScope value was incorrect; expected " + message + "\n" + page.asText(),
                   page.asText().contains(message));
    }

    /**
     * Tests APAR PI90391: that flow re-entry works.
     * This test navigates through two flows, making sure flow variables and IDs are retained correctly:
     * flow1 -> flow2 -> flow1 -> flow2
     * -> return (back in flow 1) -> return (back in flow2) -> return (back in flow 1) -> return (no flow)
     */
    @Test
    public void testPI90391() throws Exception {
        URL url = JSFUtils.createHttpUrl(jsfAparServer, "PI90391", "");
        try (WebClient webClient = new WebClient()) {
            HtmlPage page;
            String message;
            String value = "no flowscope value";

            // hit the index ("/PI90391/")
            Log.info(c, name.getMethodName(), "Making a request to " + url);
            page = webClient.getPage(url);
            verifyFlowId(page, "no flow ID");

            // enter flow 1 (flow1)
            page = findAndClickButton(page, "flow1");
            verifyFlowScopeValue(page, value);

            // enter "1" (inputValue)
            message = "1";
            ((HtmlTextInput) page.getElementById("inputValue")).type(message);

            // page 2 (page2)
            value = "value: 1";
            page = findAndClickButton(page, "page2");
            verifyFlowScopeValue(page, value);

            // call flow 2 (flow2)
            value = "no flowscope value";
            page = findAndClickButton(page, "flow2");
            verifyFlowId(page, "flow2");
            verifyFlowScopeValue(page, value);

            // enter "2" (inputValue)
            message = "2";
            ((HtmlTextInput) page.getElementById("inputValue")).type(message);

            // flow 2 page 2 (page2)
            value = "value: 2";
            page = findAndClickButton(page, "page2");
            verifyFlowScopeValue(page, value);

            // call flow 1 (flow1)
            value = "no flowscope value";
            page = findAndClickButton(page, "flow1");
            verifyFlowId(page, "flow1");
            verifyFlowScopeValue(page, value);

            // enter "3" (inputValue)
            message = "3";
            ((HtmlTextInput) page.getElementById("inputValue")).type(message);

            // page 2 (page2)
            value = "value: 3";
            page = findAndClickButton(page, "page2");
            verifyFlowScopeValue(page, value);

            // call flow 2 (flow2)
            value = "no flowscope value";
            page = findAndClickButton(page, "flow2");
            verifyFlowId(page, "flow2");
            verifyFlowScopeValue(page, value);

            // index (index)
            page = findAndClickButton(page, "index");

            // verify " Current flow ID: flow1", "Current flowscope value: 3"
            value = "value: 3";
            verifyFlowId(page, "flow1");
            verifyFlowScopeValue(page, value);

            // enter flow 1 (flow1)
            page = findAndClickButton(page, "flow1");

            // index (index)
            page = findAndClickButton(page, "index");

            // verify "Current flow ID: flow2", "Current flowscope value: 2"
            value = "value: 2";
            verifyFlowId(page, "flow2");
            verifyFlowScopeValue(page, value);

            // enter flow 1 (flow1)
            page = findAndClickButton(page, "flow1");

            // index (index)
            page = findAndClickButton(page, "index");

            // verify "Current flow ID: flow1", "Current flowscope value: 1"
            value = "value: 1";
            verifyFlowId(page, "flow1");
            verifyFlowScopeValue(page, value);

            // enter flow 1 (flow1)
            page = findAndClickButton(page, "flow1");

            // index (index)
            page = findAndClickButton(page, "index");

            // verify "Current flow ID: no flow ID", "Current flowscope value: no flowscope value"
            value = "no flowscope value";
            verifyFlowId(page, "no flow ID");
            verifyFlowScopeValue(page, value);
        }
    }

    /**
     * Tests APAR PI90507: Facelet Action Listener without binding attribute
     *
     * Ensure that when com.ibm.ws.jsf.DisableFaceletActionListenerPreDestroy is set to true,
     * preDestroy is never called on the Action Listener registered via facelet.
     *
     * This prevents a list of injected JSF artifacts to grow.
     *
     * @throws Exception
     */
    @SkipForRepeat({ EE8_FEATURES, EE9_FEATURES })
    @Test
    public void testPI90507NonBindingCase() throws Exception {
        try (WebClient webClient = new WebClient()) {
            // Set up search mark and restart the app so that we can check to see if preDestroy is called
            jsfAparServer.setMarkToEndOfLog();
            Assert.assertTrue("The PI90507.war application was not restarted.", jsfAparServer.restartDropinsApplication("PI90507.war"));

            URL url = JSFUtils.createHttpUrl(jsfAparServer, "PI90507", "actionListenerNonBinding.xhtml");

            Log.info(c, name.getMethodName(), "PI90507: Making a request to " + url);

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Make sure the test page was rendered correctly
            assertTrue("PI90507: the app output was null!", page != null);

            // Get the button to click
            HtmlSubmitInput submitButton = (HtmlSubmitInput) page.getElementById("form1:submitButton");
            submitButton.click();

            // Verify that PostConstruct is called
            assertNotNull("PostConstruct was not called",
                          jsfAparServer.waitForStringInLogUsingMark("Post construct from TestActionListener"));

            // Verify that PreDestroy is not being called
            assertTrue("PreDestroy was called",
                       jsfAparServer.findStringsInLogs("Pre destroy from TestActionListener").size() == 0);

            // clean up the log marks
            jsfAparServer.resetLogMarks();
        }
    }

    /**
     * Tests APAR PI90507: Facelet Action Listener with binding attribute
     *
     * Ensure that when com.ibm.ws.jsf.DisableFaceletActionListenerPreDestroy is set to true,
     * preDestroy is never called on the Action Listener registered via facelet.
     *
     * This prevents a list of injected JSF artifacts to grow.
     *
     * @throws Exception
     */
    @SkipForRepeat({ EE8_FEATURES, EE9_FEATURES })
    @Test
    public void testPI90507BindingCase() throws Exception {
        try (WebClient webClient = new WebClient()) {
            // Set up search mark and restart the app so that we can check to see if preDestroy is called
            jsfAparServer.setMarkToEndOfLog();
            Assert.assertTrue("The PI90507.war application was not restarted.", jsfAparServer.restartDropinsApplication("PI90507.war"));

            URL url = JSFUtils.createHttpUrl(jsfAparServer, "PI90507", "actionListenerBinding.xhtml");

            Log.info(c, name.getMethodName(), "PI90507: Making a request to " + url);

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Make sure the test page was rendered correctly
            assertTrue("PI90507: the app output was null!", page != null);

            // Get the button to click
            HtmlSubmitInput submitButton = (HtmlSubmitInput) page.getElementById("form1:submitButton");
            submitButton.click();

            // Verify that PostConstruct is called
            assertNotNull("PostConstruct was not called",
                          jsfAparServer.waitForStringInLogUsingMark("Post construct from TestActionListener"));

            // Verify that PreDestroy is not being called
            assertTrue("PreDestroy was called",
                       jsfAparServer.findStringsInLogs("Pre destroy from TestActionListener").size() == 0);

            // clean up the log marks
            jsfAparServer.resetLogMarks();
        }
    }

    /**
     * Tests APAR PH01566: Faces Servlet mapping defined in web-fragment.xml does not work
     * Also tests PH09730: a regression caused by PH01566 wherein unloadable servlet definitions prevent initialization
     *
     * Make sure that a JSF application with and invalid servlet defined and its faces servlet mapping defined in a web fragment jar
     * starts up correctly
     */
    @Test
    public void testPH01566_and_PH09730() throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfAparServer, "PH01566", "index.xhtml");

            Log.info(c, name.getMethodName(), "PH01566: Making a request to " + url);
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Make sure the test page was rendered correctly
            assertTrue("PH01566: the app output was null!", page != null);

            Log.info(c, name.getMethodName(), page.asText());

            // Check for the expected text in the target page
            assertTrue("testPH01566:: Application did not start up correctly!",
                       page.asText().contains("application initialization succeeded"));
        }
    }

    /**
     * Tests APAR PH06008: Transient attribute defined in a template is inherited by the final page correctly.
     *
     * Make sure that no 'unable to create view' exception is thrown when the Transient attribute is being
     *
     * inherited. Clicking submit should reload the page without errors.
     *
     * @throws Exception
     */
    @Test
    public void testPH06008() throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfAparServer, "PH06008", "index.xhtml");

            Log.info(c, name.getMethodName(), "PH06008: Making a request to " + url);
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Clicking Page.....");
            HtmlSubmitInput submitButton = (HtmlSubmitInput) page.getElementById("form:submit");
            submitButton.click();
            Log.info(c, name.getMethodName(), "On next page! Testing asserts next...");
            // Make sure the test page was rendered correctly
            assertTrue("PH06008: the app output was null!", page != null);

            // Check for the expected text in the target page
            assertTrue("testPH06008:: Page reload threw an error!",
                       page.asText().contains("PH06008 success!"));
        }
    }
}
