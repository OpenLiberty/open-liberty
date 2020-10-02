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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf22.fat.JSFUtils;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;

/*
 * All JSF 2.2 tests with all applicable server features enabled.
 *
 * Tests that just need to drive a simple request using our WebBrowser object can be placed in this class.
 *
 * If a test needs HtmlUnit it should more than likely be placed in the JSFHtmlUnit test class.
 */
@RunWith(FATRunner.class)
public class JSFServerTest {
    @Rule
    public TestName name = new TestName();

    String contextRoot = "TestJSF2.2";

    protected static final Class<?> c = JSFServerTest.class;

    @Server("jsfTestServer1")
    public static LibertyServer jsfTestServer1;

    @BeforeClass
    public static void setup() throws Exception {

        // Create the TestJSF2.2.war and JSF22FaceletsResourceResolverAnnotation.war applications
        ShrinkHelper.defaultDropinApp(jsfTestServer1, "TestJSF2.2.war", "com.ibm.ws.fat.jsf22.basic.*");
        ShrinkHelper.defaultDropinApp(jsfTestServer1, "JSF22FaceletsResourceResolverAnnotation.war", "com.ibm.ws.jsf22.resourceresolver");

        jsfTestServer1.startServer(JSFServerTest.class.getSimpleName() + ".log");

    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsfTestServer1 != null && jsfTestServer1.isStarted()) {
            jsfTestServer1.stopServer();
        }
    }

    /**
     * Sample test
     * Ensure nothing has gone horribly wrong.
     *
     * @throws Exception
     */
    @Test
    public void testServlet() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            assertTrue(page.asText().contains("Hello World"));
        }
    }

    /**
     * Test to ensure that the com.ibm.ws.jsf.ext.LibertyWebConfigProviderFactory is being used. If there is not entry trace
     * then we know that something happened and the LibertyWebConfigProviderFactory is not being used as it should be.
     *
     * @throws Exception
     */
    @Test
    @SkipForRepeat({ EE8_FEATURES, EE9_FEATURES })
    public void testLibertyWebConfigProviderFactory() throws Exception {
        String msgToSearchFor = "getWebConfigProvider Entry";

        // Check the trace.log to see if the LibertyWebConfigProviderFactory has any entry trace.
        String isLibertyWebConfigProviderFactoryBeingUsed = jsfTestServer1.waitForStringInTrace(msgToSearchFor);

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
    @Test
    @SkipForRepeat({ EE8_FEATURES, EE9_FEATURES })
    public void testLibertyWebConfigProvider() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "");
            // Ensure the isErrorPagePresent message is logged in the trace during the RESTORE_VIEW phase.
            webClient.getPage(url);

            String msgToSearchFor = "isErrorPagePresent ENTRY";

            // Check the trace.log to see if the LibertyWebConfigProvider has any entry trace.
            String isLibertyWebConfigProviderBeingUsed = jsfTestServer1.waitForStringInTrace(msgToSearchFor);

            // There should be a match so fail if there is not.
            assertNotNull("The following message was not found in the trace log: " + msgToSearchFor, isLibertyWebConfigProviderBeingUsed);
        }
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

        // Check the trace.log to see if the LibertyFaceletConfigResourceProvider has any entry trace.
        String isLibertyFaceletConfigResourceProviderBeingUsed = jsfTestServer1.waitForStringInTrace(msgToSearchFor);

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

        // Check the trace.log to see if the WASMyFacesAnnotationProvider has any entry trace.
        String isWASMyFacesAnnotationProviderBeingUsed = jsfTestServer1.waitForStringInTrace(msgToSearchFor);

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

        // Check the trace.log to see if the WebSphereLifecycleProviderFactory has any entry trace.
        String isWebSphereLifecycleProviderFactoryBeingUsed = jsfTestServer1.waitForStringInTrace(msgToSearchFor);

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

        // Check the trace.log to see if the WebSphereAnnotationLifecycleProvider has any entry trace.
        String isWebSphereAnnotationLifecycleProviderBeingUsed = jsfTestServer1.waitForStringInTrace(msgToSearchFor);

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
        String msgToSearchForMyFaces30 = "MyFaces Core Bean Validation support disabled";

        if (JakartaEE9Action.isActive()) {
            Log.info(c, name.getMethodName(), "Looking for : " + msgToSearchForMyFaces30);
            // Check the trace.log to see if the LibertyWebConfigProviderFactory has any entry trace.
            String isBeanValidationDisabled = jsfTestServer1.waitForStringInLog(msgToSearchForMyFaces30);

            Log.info(c, name.getMethodName(), "Message found after searching logs : " + isBeanValidationDisabled);

            // There should be a match so fail if there is not.
            assertNotNull("The following message was not found in the trace log: " + msgToSearchForMyFaces30, isBeanValidationDisabled);
        } else {
            Log.info(c, name.getMethodName(), "Looking for : " + msgToSearchFor);
            // Check the trace.log to see if the LibertyWebConfigProviderFactory has any entry trace.
            String isBeanValidationDisabled = jsfTestServer1.waitForStringInLog(msgToSearchFor);

            Log.info(c, name.getMethodName(), "Message found after searching logs : " + isBeanValidationDisabled);

            // There should be a match so fail if there is not.
            assertNotNull("The following message was not found in the trace log: " + msgToSearchFor, isBeanValidationDisabled);
        }
    }

    /**
     * Test to ensure that a class annotated with the FaceletsResourceResolver annotation is used by the MyFaces runtime.
     *
     * @throws Exception
     */
    @Test
    public void testFaceletsResourceResolverAnnotation() throws Exception {
        String msgToSearchFor = "FaceletsResourceResolver annotation worked, using custom ResourceResolver";

        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfTestServer1, "JSF22FaceletsResourceResolverAnnotation", "index.xhtml");
            HtmlPage page = (HtmlPage) webClient.getPage(url);
            assertTrue(page.asText().contains("Hello World"));

            // There should be a match so fail if there is not.
            assertNotNull("The following message was not found in the logs: " + msgToSearchFor,
                          jsfTestServer1.waitForStringInLog(msgToSearchFor));
        }
    }
}