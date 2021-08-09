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
import java.util.Calendar;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
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
import com.ibm.ws.jsf22.fat.JSFUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests to execute on the jsfTestServer2 that use HtmlUnit.
 */
@RunWith(FATRunner.class)
public class JSFHtmlUnit {
    @Rule
    public TestName name = new TestName();

    String contextRoot = "JSF22TestResources";

    protected static final Class<?> c = JSF22ResourceLibraryContractHtmlUnit.class;

    @Server("jsfTestServer2")
    public static LibertyServer jsfTestServer2;

    @BeforeClass
    public static void setup() throws Exception {
        // Create the JSF22TestResourcesJar that is used in JSF22TestResourcesWar
        JavaArchive JSF22TestResourcesJar = ShrinkHelper.buildJavaArchive("JSF22TestResources.jar", "");

        // Create the JSF22TestResources.war application
        WebArchive JSF22TestResourcesWar = ShrinkHelper.buildDefaultApp("JSF22TestResources.war", "com.ibm.ws.jsf22.fat.resources.*");
        JSF22TestResourcesWar.addAsLibraries(JSF22TestResourcesJar);
        ShrinkHelper.addDirectory(JSF22TestResourcesWar, "test-applications" + "/JSF22TestResources.jar");

        // Create the JSF22BackwardCompatibilityTests.war application
        WebArchive JSF22BackwardCompatibilityTestsWar = ShrinkHelper.buildDefaultApp("JSF22BackwardCompatibilityTests.war", "com.ibm.ws.jsf22.fat.backwards.*");

        // Add both wars to the server
        ShrinkHelper.exportDropinAppToServer(jsfTestServer2, JSF22BackwardCompatibilityTestsWar);
        ShrinkHelper.exportDropinAppToServer(jsfTestServer2, JSF22TestResourcesWar);

        jsfTestServer2.startServer(JSFHtmlUnit.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsfTestServer2 != null && jsfTestServer2.isStarted()) {
            jsfTestServer2.stopServer();
        }
    }

    /**
     * Test if directory "/WEB-INF/resources" is being used properly as a resource directory
     *
     * The javax.faces.WEBAPP_RESOURCES_DIRECTORY context-parameter has been set in the web.xml pointing
     * to the new resource directory.
     *
     * @throws Exception
     */
    @Test
    public void testNewResourceDirectory() throws Exception {

        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "personInformation.jsf");
            HtmlPage personInfoPage = (HtmlPage) webClient.getPage(url);

            assertTrue(personInfoPage.asText().contains("Please provide the following information:"));
            // Get the form that we are dealing with and within that form
            HtmlForm form = personInfoPage.getFormByName("personInfoForm");

            // Find the fields and submit button
            HtmlTextInput firstNameTextField = form.getInputByName("personInfoForm:firstName");
            HtmlTextInput lastNameTextField = form.getInputByName("personInfoForm:lastName");
            HtmlTextInput favoriteAnimalTextField = form.getInputByName("personInfoForm:favoriteAnimal");

            HtmlSubmitInput button = form.getInputByName("personInfoForm:showMessageButton");

            // Change the value of the text field
            firstNameTextField.setValueAttribute("John");
            lastNameTextField.setValueAttribute("Smith");
            favoriteAnimalTextField.setValueAttribute("Dogs");

            // Now submit the form by clicking the button and get back the second page.
            HtmlPage messagePage = button.click();

            assertTrue(messagePage.asText().contains("Hello"));
            assertTrue(messagePage.asText().contains("John Smith!"));
            assertTrue(messagePage.asText().contains("Thanks for providing the required information. Now we know that your favorite animal is:"));
            assertTrue(messagePage.asText().contains("Dogs"));
        }
    }

    /**
     * Test the mapping of viewId to a resource path
     *
     * Given a viewId of a resource file, map that viewId to a URL resource path
     * so it can be used in a Facelet view. This test ensures that the mapping is
     * correct by driving a request to the Facelet view and checking the response.
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test
    public void testMapViewIdtoResourcePath() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "mapViewIdToResource.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            assertTrue(page.asText().contains("Using a template inside /WEB-INF/resources/templates"));
            assertTrue(page.asText().contains("This template is working as expected!"));
            assertTrue(page.asText().contains("JSF22TestResources.war!/WEB-INF/resources/templates/basicTemplate.xhtml"));
        }
    }

    /**
     * Test loading a view from an external location, in this case, using the class-path
     *
     * Given a viewId of a resource file from an external location, map that viewId (class-path)
     * to a URL so it can be used to load a Facelet view. This test ensures that the Facelet view is
     * loaded correctly with the resource from external location by driving a request and
     * checking the response.
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test
    public void testLoadViewFromExternalLocation() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "loadViewFromExternalLocation.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            assertTrue(page.asText().contains("Using a template loaded from external location using class-path /META-INF/resources/templates"));
            assertTrue(page.asText().contains("This template is working as expected!"));
            assertTrue(page.asText().contains("/WEB-INF/lib/JSF22TestResources.jar!/META-INF/resources/templates/basicTemplate.xhtml"));
        }
    }

    /**
     * Test the userAgentNeedsUpdateMethod for True condition
     *
     * Tests the method when user-agent requesting the resource needs an update.
     * That is when the requested variant has been modified since the time
     * specified in the "If-Modified-Since" field.
     *
     * Set the "If-Modified-Since" request header to any date in the past and
     * drive a request to the page. By setting this, "Last-Modified" field
     * will be set to the date when the app started. Hence if the "Last-Modified" date
     * is greater than "If-Modified-Since", the agent needs to update.
     *
     * Ensure that the userAgentNeedsUpdate method returns true.
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test
    public void testUserAgentNeedsUpdateTrueCondition() throws Exception {
        try (WebClient webClient = new WebClient()) {
            Calendar calendar = Calendar.getInstance();

            webClient.addRequestHeader("If-Modified-Since", "Thu, 01 Jan " + (calendar.get(Calendar.YEAR) - 1) + " 00:00:00 GMT");

            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "testUserAgentNeedsUpdateMethod.jsf");

            HtmlPage trueConditionPage = (HtmlPage) webClient.getPage(url);
            Log.info(c, name.getMethodName(), "Response true: " + trueConditionPage.asText());

            assertTrue(trueConditionPage.asText().contains("Request Headers:"));
            assertTrue(trueConditionPage.asText().contains("If-Modified-Since"));
            assertTrue(trueConditionPage.asText().contains("User Agent Needs Update Result: true"));
        }
    }

    /**
     * Test the userAgentNeedsUpdateMethod for False condition
     *
     * Tests the method when user-agent doesn't need an update of the resource.
     * That is when the requested variant has not been modified since the time
     * specified in the "If-Modified-Since" field.
     *
     * Set the "If-Modified-Since" request header to any date in the future and
     * drive a request to the page. By setting this, "Last-Modified" field
     * will be set to the date when the app started. Hence if the "Last-Modified" date
     * is lower or equal than "If-Modified-Since", the agent does not need to update.
     *
     * Ensure that the userAgentNeedsUpdate method returns false.
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test
    public void testUserAgentNeedsUpdateFalseCondition() throws Exception {
        try (WebClient webClient = new WebClient()) {
            Calendar calendar = Calendar.getInstance();

            webClient.addRequestHeader("If-Modified-Since", "Thu, 01 Jan " + (calendar.get(Calendar.YEAR) + 1) + " 00:00:00 GMT");

            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "testUserAgentNeedsUpdateMethod.jsf");

            HtmlPage falseConditionPage = (HtmlPage) webClient.getPage(url);
            Log.info(c, name.getMethodName(), "Response false: " + falseConditionPage.asText());

            assertTrue(falseConditionPage.asText().contains("Request Headers:"));
            assertTrue(falseConditionPage.asText().contains("If-Modified-Since"));
            assertTrue(falseConditionPage.asText().contains("User Agent Needs Update Result: false"));
        }
    }

    /**
     * Test exception thrown by processValueChange from MethodExpressionValueChangeListener
     * and processAction from MethodExpressionActionListener.
     *
     * Ensure that exceptions thrown are not instances of an AbortProcessingException
     * by driving a request and checking the response.
     *
     * This resulted in an AbortProcessingException
     *
     * @throws Exception
     */
    @Test
    public void testProcessValueChangeAndProcessActionMethods() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfTestServer2, "JSF22BackwardCompatibilityTests", "testProcessValueChangeAndProcessAction.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            assertTrue(page.asText().contains("Write a word:"));
            assertTrue(page.asText().contains("Write a name:"));

            // Get the form that we are dealing with and within that form
            HtmlForm form = page.getFormByName("testForm");

            // Find the fields and submit button
            HtmlTextInput wordTextField = form.getInputByName("wordInput");
            HtmlTextInput nameTextField = form.getInputByName("nameInput");

            HtmlSubmitInput button = form.getInputByName("submitButton");

            // Change the value of the text field
            wordTextField.setValueAttribute("Dolphins");
            nameTextField.setValueAttribute("John");

            // Now submit the form by clicking the button and get back the second page.
            HtmlPage resultPage = button.click();

            assertTrue(resultPage.asText().contains("Testing ExceptionFromProcessValueChange: Exception thrown is correct since it is an instance of ELException"));
            assertTrue(resultPage.asText().contains("Testing ExceptionFromProcessAction: Exception thrown is correct since it is an instance of ELException"));
            assertTrue(resultPage.asText().contains("Testing ELException: NullPointerException. Exception thrown is correct since it is an instance of NullPointerException"));
            assertTrue(resultPage.asText().contains("Word: Dolphins"));
            assertTrue(resultPage.asText().contains("Name: John"));
        }
    }

    /**
     * Test getType method from the CompositeComponetELResolver by checking
     * the returned type (class) for untyped and typed composite components.
     *
     * Ensure that there are no null values in the response
     *
     *
     * @throws Exception
     */
    @Test
    public void testGetTypeFromCompositeComponentELResolver() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfTestServer2, "JSF22BackwardCompatibilityTests", "testCompositeComponentAttribute.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Response: " + page.asText());

            assertTrue(page.asText().contains("Untyped Literal From Map - Attribute: untypedXliteral, Type: Object"));
            assertTrue(page.asText().contains("Typed Literal From Map - Attribute: typedXliteral, Type: Integer"));
            Log.info(c, name.getMethodName(), "one");
            assertTrue(page.asText().contains("Untyped Unset From Map - Attribute: untypedXunset, Type: Object"));
            assertTrue(page.asText().contains("Typed Unset From Map - Attribute: typedXunset, Type: Dog"));
            Log.info(c, name.getMethodName(), "one");
            assertTrue(page.asText().contains("Untyped WideEL From Map - Attribute: untypedXwideEL, Type: Animal"));
            assertTrue(page.asText().contains("Typed WideEL From Map - Attribute: typedXwideEL, Type: Dog"));
            Log.info(c, name.getMethodName(), "one");
            assertTrue(page.asText().contains("Untyped MediumEL From Map - Attribute: untypedXmediumEL, Type: Dog"));
            assertTrue(page.asText().contains("Typed MediumEL From Map - Attribute: typedXmediumEL, Type: Dog"));
            Log.info(c, name.getMethodName(), "one");
            assertTrue(page.asText().contains("Untyped NarrowEL From Map - Attribute: untypedXnarrowEL, Type: Pitbull"));
            assertTrue(page.asText().contains("Typed NarrowEL From Map - Attribute: typedXnarrowEL, Type: Pitbull"));
            Log.info(c, name.getMethodName(), "one");
            assertTrue(page.asText().contains("Untyped NullEL From Map - Attribute: untypedXnullEL, Type: Dog"));
            assertTrue(page.asText().contains("Typed NullEL From Map - Attribute: typedXnullEL, Type: Dog"));
        }
    }

    /**
     * Test for 169347: Port MYFACES-3956 / MYFACES-3954, fixes for ResourceHandler.libraryExists(...)
     *
     * Make sure the Library exists.
     *
     * @throws Exception
     */
    @Test
    public void testifLibraryExist() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfTestServer2, "JSF22TestResources", "checkifLibraryExists.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Response: " + page.asText());

            assertTrue(page.asText().contains("Library Exist: true"));
        }
    }
}
