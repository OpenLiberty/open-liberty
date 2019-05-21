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

import java.util.List;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.ws.fat.util.SharedServer;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 *
 */
@Mode(TestMode.FULL)
@MinimumJavaLevel(javaLevel = 7)
public class JSF22IncludeTest {
    private static final Logger LOG = Logger.getLogger(JSF22IncludeTest.class.getName());

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("jsf22IncludeTestServer");

    @AfterClass
    public static void testCleanup() throws Exception {
        // test cleanup
    }

    /**
     * Test if the page is properly rendered when using a jsp:include element
     * This is a replacement for the JSF 2.0 test PM25955, which has been disabled
     * in the JSF 2.2 run of the JSF 2.0 bucket
     * 
     * @throws Exception
     */
    @Test
    public void testJSPInclude() throws Exception {
        String methodName = "testJSPInclude";

        WebClient webClient = new WebClient();

        HtmlPage page = (HtmlPage) webClient.getPage(SHARED_SERVER.getServerUrl(true, "/TestJSF2.2/IncludeTest.jsf"));

        LOG.info("Navigating to: /TestJSF2.2/IncludeTest.jsf");

        int statusCode = page.getWebResponse().getStatusCode();

        LOG.info("Checking the satus code, 200 expected : " + statusCode);
        // Check the status code
        if (statusCode != 200) {
            Assert.fail("Test failed! Status Code: " + statusCode + " Page contents: " + page.asXml());
        }

        LOG.info("Checking to make sure the include was properly rendered");
        // Make sure the right text is output
        if (!page.asXml().contains("some text")) {
            Assert.fail("The wrong text was printed! Status Code: " + statusCode + " Page contents: " + page.asXml());
        }

        LOG.info("Ensuring ViewState had a proper ID generated");
        //Make sure the ViewState elements were generated with the proper IDs
        //This is the specific ID we want to look for because it has changed since our 2.0 code
        //which generates them with different IDs
        //This test is in lieu of PM25955 from the JSF 2.0 bucket
        List<DomElement> viewStateElements = page.getElementsByName("javax.faces.ViewState");
        for (DomElement element : viewStateElements) {
            String id = element.getId();

            if (!id.startsWith("j_id__v_")) {
                Assert.fail("ViewState elements were not created with the correct id attribute! id: " + id + " Page contents: " + page.asXml());
            }
        }
    }
}
