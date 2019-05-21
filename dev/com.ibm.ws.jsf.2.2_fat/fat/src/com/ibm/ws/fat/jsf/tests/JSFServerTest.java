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

import static org.junit.Assert.assertNotNull;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.fat.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebResponse;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.RunUnlessFeatureBeingTested;

/**
 * All JSF 2.2 tests with all applicable server features enabled.
 *
 * Tests that just need to drive a simple request using our WebBrowser object can be placed in this class.
 *
 * If a test needs HtmlUnit it should more than likely be placed in the JSFHtmlUnit test class.
 */
@MinimumJavaLevel(javaLevel = 7)
public class JSFServerTest extends LoggingTest {
    private static final Logger LOG = Logger.getLogger(JSFServerTest.class.getName());

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("jsfTestServer1");

    @AfterClass
    public static void testCleanup() throws Exception {
        // test cleanup
    }

    protected WebResponse verifyResponse(String resource, String expectedResponse) throws Exception {
        return SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(), resource, expectedResponse);
    }

    protected WebResponse verifyResponse(String resource, String expectedResponse, int numberToMatch, String extraMatch) throws Exception {
        return SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(), resource, expectedResponse, numberToMatch, extraMatch);
    }

    protected WebResponse verifyResponse(WebBrowser wb, String resource, String expectedResponse) throws Exception {
        return SHARED_SERVER.verifyResponse(wb, resource, expectedResponse);
    }

    protected WebResponse verifyResponse(WebBrowser wb, String resource, String... expectedResponseStrings) throws Exception {
        return SHARED_SERVER.verifyResponse(wb, resource, expectedResponseStrings);
    }

    protected WebResponse verifyResponse(WebBrowser wb, String resource, String[] expectedResponses, String[] unexpectedResponses) throws Exception {
        return SHARED_SERVER.verifyResponse(wb, resource, expectedResponses, unexpectedResponses);
    }

    /**
     * Sample test
     *
     * @throws Exception
     *             if something goes horribly wrong
     */
    @Test
    public void testServlet() throws Exception {
        // Use the SharedServer to verify a response.
        this.verifyResponse("/TestJSF2.2", "Hello World");
    }

    /**
     * Test to ensure that the com.ibm.ws.jsf.ext.LibertyWebConfigProviderFactory is being used. If there is not entry trace
     * then we know that something happened and the LibertyWebConfigProviderFactory is not being used as it should be.
     *
     * @throws Exception
     */
    @RunUnlessFeatureBeingTested("jsf-2.3")
    @Test
    public void testLibertyWebConfigProviderFactory() throws Exception {
        String msgToSearchFor = "getWebConfigProvider Entry";

        // Use the SharedServer to verify a response.
        this.verifyResponse("/TestJSF2.2", "Hello World");

        // Check the trace.log to see if the LibertyWebConfigProviderFactory has any entry trace.
        String isLibertyWebConfigProviderFactoryBeingUsed = SHARED_SERVER.getLibertyServer().waitForStringInTrace(msgToSearchFor);

        // There should be a match so fail if there is not.
        assertNotNull("The following message was not found in the trace log: " + msgToSearchFor, isLibertyWebConfigProviderFactoryBeingUsed);
    }

    /**
     * Test to ensure that the com.ibm.ws.jsf.ext.LibertyWebConfigProvider is being used. If there is not entry trace
     * then we know that something happened and the LibertyWebConfigProviderFactory and LibertyWebConfigProvider are not
     * being used as they should be.
     *
     * @throws Exception
     */
    @RunUnlessFeatureBeingTested("jsf-2.3")
    @Test
    public void testLibertyWebConfigProvider() throws Exception {
        String msgToSearchFor = "isErrorPagePresent ENTRY";

        // Use the SharedServer to verify a response.
        this.verifyResponse("/TestJSF2.2", "Hello World");

        // Check the trace.log to see if the LibertyWebConfigProvider has any entry trace.
        String isLibertyWebConfigProviderBeingUsed = SHARED_SERVER.getLibertyServer().waitForStringInTrace(msgToSearchFor);

        // There should be a match so fail if there is not.
        assertNotNull("The following message was not found in the trace log: " + msgToSearchFor, isLibertyWebConfigProviderBeingUsed);
    }

    /**
     * Test to ensure that the com.ibm.ws.jsf.config.resource.LibertyFaceletConfigResourceProvider is being used. If there is not any
     * entry trace then we know that something happened and the LibertyFaceletConfigResourceProvider is not being used as it should be.
     *
     * @throws Exception
     */
    @Test
    public void testLibertyFaceletConfigResourceProvider() throws Exception {
        String msgToSearchFor = "getFaceletTagLibConfigurationResources ENTRY";

        // Use the SharedServer to verify a response.
        this.verifyResponse("/TestJSF2.2", "Hello World");

        // Check the trace.log to see if the LibertyFaceletConfigResourceProvider has any entry trace.
        String isLibertyFaceletConfigResourceProviderBeingUsed = SHARED_SERVER.getLibertyServer().waitForStringInTrace(msgToSearchFor);

        // There should be a match so fail if there is not.
        assertNotNull("The following message was not found in the trace logs: " + msgToSearchFor,
                      isLibertyFaceletConfigResourceProviderBeingUsed);
    }

    /**
     * Test to ensure that the com.ibm.ws.jsfconfig.annotation.WASMyFacesAnnotationProvider is being used. If there is not any
     * entry trace then we know that something happened and the WASMyFacesAnnotationProvider is not being used as it should be.
     *
     * @throws Exception
     */
    @Test
    public void testWASMyFacesAnnotationProvider() throws Exception {
        String msgToSearchFor = "com.ibm.ws.jsf.config.annotation.WASMyFacesAnnotationProvider <init> ENTRY";

        // Use the SharedServer to verify a response.
        this.verifyResponse("/TestJSF2.2", "Hello World");

        // Check the trace.log to see if the WASMyFacesAnnotationProvider has any entry trace.
        String isWASMyFacesAnnotationProviderBeingUsed = SHARED_SERVER.getLibertyServer().waitForStringInTrace(msgToSearchFor);

        // There should be a match so fail if there is not.
        assertNotNull("The following message was not found in the trace logs: " + msgToSearchFor,
                      isWASMyFacesAnnotationProviderBeingUsed);
    }

    /**
     * Test to ensure that the com.ibm.ws.jsfconfig.annotation.WebSphereLifecycleProviderFactory is being used. If there is not any
     * entry trace then we know that something happened and the WebSphereLifecycleProviderFactory is not being used as it should be.
     *
     * @throws Exception
     */
    @Test
    public void testWebSphereLifecycleProviderFactory() throws Exception {
        String msgToSearchFor = "com.ibm.ws.jsf.config.annotation.WebSphereLifecycleProviderFactory <init> ENTRY";

        // Use the SharedServer to verify a response.
        this.verifyResponse("/TestJSF2.2", "Hello World");

        // Check the trace.log to see if the WebSphereLifecycleProviderFactory has any entry trace.
        String isWebSphereLifecycleProviderFactoryBeingUsed = SHARED_SERVER.getLibertyServer().waitForStringInTrace(msgToSearchFor);

        // There should be a match so fail if there is not.
        assertNotNull("The following message was not found in the trace logs: " + msgToSearchFor,
                      isWebSphereLifecycleProviderFactoryBeingUsed);
    }

    /**
     * Test to ensure that the com.ibm.ws.jsfconfig.annotation.WebSphereAnnotationLifecycleProvider is being used. If there is not any
     * entry trace then we know that something happened and the WebSphereAnnotationLifecycleProvider is not being used as it should be.
     *
     * @throws Exception
     */
    @Test
    public void testWebSphereAnnotationLifecycleProvider() throws Exception {
        String msgToSearchFor = "com.ibm.ws.jsf.config.annotation.WebSphereAnnotationLifecycleProvider <init> ENTRY";

        // Use the SharedServer to verify a response.
        this.verifyResponse("/TestJSF2.2", "Hello World");

        // Check the trace.log to see if the WebSphereAnnotationLifecycleProvider has any entry trace.
        String isWebSphereAnnotationLifecycleProviderBeingUsed = SHARED_SERVER.getLibertyServer().waitForStringInTrace(msgToSearchFor);

        // There should be a match so fail if there is not.
        assertNotNull("The following message was not found in the trace logs: " + msgToSearchFor,
                      isWebSphereAnnotationLifecycleProviderBeingUsed);
    }

    /**
     * Test to ensure that when the jsf-2.2 is enabled, beanValidation-1.1 is disabled
     * We do this by looking for a message in the logs
     *
     * @throws Exception
     */
    @Test
    public void testBeanValidation11Disabled() throws Exception {
        String msgToSearchFor = "MyFaces Bean Validation support disabled";

        LOG.info("Requesting a basic HTML page");
        // Use the SharedServer to verify a response.
        this.verifyResponse("/TestJSF2.2", "Hello World");

        LOG.info("Looking for : " + msgToSearchFor);
        // Check the trace.log to see if the LibertyWebConfigProviderFactory has any entry trace.
        String isBeanValidationDisabled = SHARED_SERVER.getLibertyServer().waitForStringInLog(msgToSearchFor);

        LOG.info("Message found after searching logs : " + isBeanValidationDisabled);

        // There should be a match so fail if there is not.
        assertNotNull("The following message was not found in the trace log: " + msgToSearchFor, isBeanValidationDisabled);
    }

    /**
     * Test to ensure that a class annotated with the FaceletsResourceResolver annotation is used by the MyFaces runtime.
     *
     * @throws Exception
     */
    @Test
    public void testFaceletsResourceResolverAnnotation() throws Exception {
        String msgToSearchFor = "FaceletsResourceResolver annotation worked, using custom ResourceResolver";

        // Use the SharedServer to verify a response.
        this.verifyResponse("/JSF22FaceletsResourceResolverAnnotation/", "Hello World");

        // There should be a match so fail if there is not.
        assertNotNull("The following message was not found in the logs: " + msgToSearchFor,
                      SHARED_SERVER.getLibertyServer().waitForStringInLog(msgToSearchFor));

    }
}