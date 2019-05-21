/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.fat.jsf.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.ibm.ws.fat.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;

import componenttest.annotation.MinimumJavaLevel;

/**
 * Tests to execute on the jsfTestServer1 that use HtmlUnit.
 * This particular class executes the tests found in the TestJSF2.2 application.
 * These tests are relatively standalone.
 */
@MinimumJavaLevel(javaLevel = 7)
public class JSFSimpleHtmlUnit extends LoggingTest {
    private static final Logger LOG = Logger.getLogger(JSFSimpleHtmlUnit.class.getName());

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("jsfTestServer1");

    @AfterClass
    public static void testCleanup() throws Exception {
        // test cleanup
    }

    /*
     * A sample HtmlUnit test case for JSF. Just ensure that the basic application is reachable.
     *
     * @throws Exception
     */
    @Test
    public void sampleTest() throws Exception {
        WebClient webClient = new WebClient();
        HtmlPage page = (HtmlPage) webClient.getPage(SHARED_SERVER.getServerUrl(true, "/TestJSF2.2"));
        assertTrue(page.asText().contains("Hello World"));
    }

    /**
     * 167256: create testcase for UIViewParameter.getSubmittedValue() i.e. JAVASERVERFACES_SPEC_PUBLIC-1063 spec clarification
     *
     * Testcase will test first setting the value and then get the submittedValue. Make sure it is of type Object.
     */
    @Test
    public void testEditableValueHoldergetSubmittedValue() throws Exception {

        WebClient webClient = new WebClient();
        HtmlPage page = (HtmlPage) webClient.getPage(SHARED_SERVER.getServerUrl(true, "/TestJSF2.2/testValue.jsf"));
        //LOG.info("testEditableValueHoldergetSubmittedValue:: page " + page.asXml());

        // Click the commandButton to execute the methods and update the page
        HtmlElement button = (HtmlElement) page.getElementById("testForm:test");
        page = button.click();
        LOG.info("testEditableValueHoldergetSubmittedValue:: page now " + page.asXml());
        assertTrue(page.asXml().contains("getSubmittedValue PASS"));
    }

    /**
     * 167254: create testcase for JAVASERVERFACES_SPEC_PUBLIC-1064 spec clarification regd CDATA section
     *
     * Testcase will check the data within the CDATA section is not consumed.
     */
    @Test
    public void testDatainCdataSectionWorks() throws Exception {

        WebClient webClient = new WebClient();
        HtmlPage page = (HtmlPage) webClient.getPage(SHARED_SERVER.getServerUrl(true, "/TestJSF2.2/testCdata.jsf"));
        LOG.info("testEditableValueHoldergetSubmittedValue:: page --> " + page.asText());

        assertTrue(page.asText().contains("Hello World!"));

    }

    /**
     * Test the c:forEach tag using an object with overriden equals method
     * and with an object that is not serializable.
     *
     * Ensure that c:forEach does not use an outdated reference to the object
     * by checking the response of the page.
     *
     * @throws Exception
     */
    @Test
    public void testCForEachWithCustomEqualsAndNonSerializableObjects() throws Exception {
        WebClient webClient = new WebClient();

        HtmlPage page = (HtmlPage) webClient.getPage(SHARED_SERVER.getServerUrl(true, "/TestJSF2.2/forEach-equals.jsf"));

        // Page without clicking the button of the form called Change Items
        assertTrue(page.asText().contains("ForEach equals and non serializable behavior"));

        assertTrue(page.asText().contains("Overriden equals c:forEach: 0 1 2 3 4 0 1 2 3 4"));
        assertTrue(page.asText().contains("Overriden equals ui:repeat: 0 1 2 3 4 0 1 2 3 4"));
        assertTrue(page.asText().contains("Serializable Object c:forEach: 0 1 2 3 4 0 1 2 3 4"));
        assertTrue(page.asText().contains("Serializable Object ui:repeat: 0 1 2 3 4 0 1 2 3 4"));
        assertTrue(page.asText().contains("Non-Serializable Object c:forEach: 0 1 2 3 4 0 1 2 3 4"));
        assertTrue(page.asText().contains("Non-Serializable Object ui:repeat: 0 1 2 3 4 0 1 2 3 4"));

        // Get the form that we are dealing
        HtmlForm changedItemsForm1 = page.getFormByName("testCForEachForm");

        // Get the button to change the items
        HtmlSubmitInput changeItemsButton1 = changedItemsForm1.getInputByName("changeItemsButton");

        // Now submit the form by clicking the button for the first time
        HtmlPage changedItems1 = changeItemsButton1.click();

        // Page after clicking the button Change Items for the first time
        assertTrue(changedItems1.asText().contains("Overriden equals c:forEach: 0 1 2 3 4 0 2 4 6 8"));
        assertTrue(changedItems1.asText().contains("Overriden equals ui:repeat: 0 1 2 3 4 0 2 4 6 8"));
        assertTrue(changedItems1.asText().contains("Serializable Object c:forEach: 0 1 2 3 4 0 2 4 6 8"));
        assertTrue(changedItems1.asText().contains("Serializable Object ui:repeat: 0 1 2 3 4 0 2 4 6 8"));
        assertTrue(changedItems1.asText().contains("Non-Serializable Object c:forEach: 0 1 2 3 4 0 2 4 6 8"));
        assertTrue(changedItems1.asText().contains("Non-Serializable Object ui:repeat: 0 1 2 3 4 0 2 4 6 8"));

        // Get the form that we are dealing
        HtmlForm changedItemsForm2 = changedItems1.getFormByName("testCForEachForm");

        // Get the button to change the items
        HtmlSubmitInput changeItemsButton2 = changedItemsForm2.getInputByName("changeItemsButton");

        // Now submit the form by clicking the button
        HtmlPage changedItems2 = changeItemsButton2.click();

        // Page after clicking the button Change Items for the second time
        assertTrue(changedItems2.asText().contains("Overriden equals c:forEach: 0 1 2 3 4 0 3 6 9 12"));
        assertTrue(changedItems2.asText().contains("Overriden equals ui:repeat: 0 1 2 3 4 0 3 6 9 12"));
        assertTrue(changedItems2.asText().contains("Serializable Object c:forEach: 0 1 2 3 4 0 3 6 9 12"));
        assertTrue(changedItems2.asText().contains("Serializable Object ui:repeat: 0 1 2 3 4 0 3 6 9 12"));
        assertTrue(changedItems2.asText().contains("Non-Serializable Object c:forEach: 0 1 2 3 4 0 3 6 9 12"));
        assertTrue(changedItems2.asText().contains("Non-Serializable Object ui:repeat: 0 1 2 3 4 0 3 6 9 12"));
    }

    /**
     * Test h:commandButton when there is one composite component with
     * comments on interface section.
     *
     * Ensure that after clicking the button for the first time, the action is performed.
     *
     * Action wasn't being performed on first click
     *
     * @throws Exception
     */
    @Test
    public void testHCommandButtonWithCompositeComponentOnFirstClick() throws Exception {
        WebClient webClient = new WebClient();

        HtmlPage page = (HtmlPage) webClient.getPage(SHARED_SERVER.getServerUrl(true, "/TestJSF2.2/testCommandButton.jsf"));

        assertTrue(page.asText().contains("Testing h:commandButton on first click"));

        // Get the form that we are dealing
        HtmlForm testForm = page.getFormByName("testForm");

        // Get the button to be clicked
        HtmlSubmitInput commandButton = testForm.getInputByValue("Execute_Action");

        // Click the button
        HtmlPage resultPage = commandButton.click();

        // Page and response after clicking the button one time
        assertTrue(resultPage.asText().contains("Action was performed on first click!"));
    }

    /**
     * Create a testcase for JAVASERVERFACES_SPEC_PUBLIC-1004 , default size is 1024 as compared to -1 in previous versions
     *
     * @throws Exception
     */
    @Test
    public void check_default_FACELETS_BUFFER_SIZE() throws Exception {
        WebClient webClient = new WebClient();

        // Make a request to a dummy page to ensure that MyFaces initializes if it has not done so already
        webClient.getPage(SHARED_SERVER.getServerUrl(true, "/TestJSF2.2/dummy.jsf"));

        String msg = "No context init parameter 'javax.faces.FACELETS_BUFFER_SIZE' found, using default value '1024'";
        // Check the trace.log
        // There should be a match so fail if there is not.
        assertFalse(msg, SHARED_SERVER.getLibertyServer().findStringsInLogs(msg).isEmpty());
        LOG.info("check_default_FACELETS_BUFFER_SIZE :: Found expected msg in log -->" + msg);
    }

    /**
     * Create a testcase 169346: Port MYFACES-3949, javax.faces.ViewState autocomplete
     *
     * @throws Exception
     */
    @Test
    public void check_defaultLogging_AUTOCOMPLETE_OFF_VIEW_STATE() throws Exception {
        WebClient webClient = new WebClient();

        // Make a request to a dummy page to ensure that MyFaces initializes if it has not done so already
        webClient.getPage(SHARED_SERVER.getServerUrl(true, "/TestJSF2.2/dummy.jsf"));

        String msg = "No context init parameter 'org.apache.myfaces.AUTOCOMPLETE_OFF_VIEW_STATE' found, using default value 'true'";
        // Check the trace.log
        // There should be a match so fail if there is not.
        assertFalse(msg, SHARED_SERVER.getLibertyServer().findStringsInLogs(msg).isEmpty());
        LOG.info("check_defaultLogging_AUTOCOMPLETE_OFF_VIEW_STATE :: Found expected msg in log -->" + msg);
    }

    /**
     * Test for 170574: CTS: JSF 2.2: jsf/api/javax_faces/context/externalcontext/URLClient_extContextGetInitParameterNPETest.jtr
     *
     * Make sure the exception is thrown
     *
     * @throws Exception
     */
    @Test
    public void testifExceptionThrown_nullInitParam() throws Exception {
        WebClient webClient = new WebClient();
        HtmlPage page = null;

        page = (HtmlPage) webClient.getPage(SHARED_SERVER.getServerUrl(true, "/TestJSF2.2/getInitParam.jsf"));

        LOG.info("Response: " + page.asText());

        assertTrue(page.asText().contains("Check NPE: true"));

    }

    /**
     * Test the c:forEach tag with varStatus assigned
     *
     * Ensure that a PropertyNotFoundExcpetion is not thrown
     *
     * @throws Exception
     */
    @Test
    public void testCForEachWithVarStatusAssigned() throws Exception {
        WebClient webClient = new WebClient();
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

        HtmlPage page = (HtmlPage) webClient.getPage(SHARED_SERVER.getServerUrl(true, "/TestJSF2.2/forEach-varStatus.jsf"));

        // Page without clicking the button of the form called Change Items
        assertTrue(page.asText().contains("forEach varStatus assigned"));

        // Click button
        HtmlForm changedItemsForm1 = page.getFormByName("testCForEachForm");
        HtmlSubmitInput changeItemsButton1 = changedItemsForm1.getInputByName("changeItemsButton");
        HtmlPage changedItems1 = changeItemsButton1.click();

        String msgToSearchFor = "PropertyNotFoundException";
        List<String> msg = SHARED_SERVER.getLibertyServer().findStringsInLogs(msgToSearchFor);

        assertFalse("PropertyNotFoundException found in logs ", !msg.isEmpty());
    }
}
