/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.logging.Logger;

import org.junit.AfterClass;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import componenttest.topology.impl.LibertyServer;
import junit.framework.Assert;

/**
 * This is the base class which is extended by the other JSF 2.3 CDI tests.
 * The methods here are executed by those other test classes with their various configurations.
 */
public abstract class CDITestBase {
    private static final Logger LOG = Logger.getLogger(CDITestBase.class.getName());

    @AfterClass
    public static void testCleanup() throws Exception {
        // test cleanup
    }

    protected void verifyResponse(String contextRoot, String resource, String expectedResponse, LibertyServer server) throws Exception {
        //return server.verifyResponse(createWebBrowserForTestCase(), resource, expectedResponse);
        WebClient webClient = new WebClient();

        URL url = JSFUtils.createHttpUrl(server, contextRoot, resource);
        HtmlPage page = (HtmlPage) webClient.getPage(url);

        if (page == null) {
            Assert.fail(resource + " did not render properly.");
        }

        if (!page.asText().contains(expectedResponse)) {
            Assert.fail("The page did not contain the following expected response: " + expectedResponse);
        }

    }

    protected void verifyResponse(String contextRoot, String resource, LibertyServer server, String... expectedResponseStrings) throws Exception {
        //return server.verifyResponse(createWebBrowserForTestCase(), resource, expectedResponseStrings);
        WebClient webClient = new WebClient();

        URL url = JSFUtils.createHttpUrl(server, contextRoot, resource);
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

    protected void verifyStatusCode(String contextRoot, String resource, int statusCode, LibertyServer server) throws Exception {
        //sharedServer.verifyStatusCode(createWebBrowserForTestCase(), resource, statusCode);
        WebClient webClient = new WebClient();

        // Construct the URL for the test
        URL url = JSFUtils.createHttpUrl(server, contextRoot, resource);
        HtmlPage page = (HtmlPage) webClient.getPage(url);

        if (page == null) {
            Assert.fail(resource + " did not render properly.");
        }

        int responseStatusCode = page.getWebResponse().getStatusCode();

        assertTrue("Status code received: " + responseStatusCode + ", expected : " + statusCode, responseStatusCode == statusCode);
    }

    protected void verifyStatusCode(String contextRoot, String resource, String buttonID, int statusCode, LibertyServer server) throws Exception {
        WebClient webClient = new WebClient();

        // Construct the URL for the test
        URL url = JSFUtils.createHttpUrl(server, contextRoot, resource);
        HtmlPage page = (HtmlPage) webClient.getPage(url);

        if (page == null) {
            Assert.fail(resource + " did not render properly.");
        }

        // Click link to execute the methods and update the page
        HtmlElement button = (HtmlElement) page.getElementById(buttonID);
        page = button.click();

        //Small wait to make sure the call happens.
        Thread.sleep(2000);

        int responseStatusCode = page.getWebResponse().getStatusCode();

        assertTrue("Status code received: " + responseStatusCode + ", expected : " + statusCode, responseStatusCode == statusCode);

    }

    /**
     * Test to ensure that CDI 2.0 injection works for an custom action listener
     * Field and Method injection, but no Constructor injection.
     * Also tested are use of request and session scope and use of qualifiers.
     *
     * @throws Exception. Content of the response should show if a specific injection failed.
     *
     */
    protected void testActionListenerInjectionByApp(String contextRoot, LibertyServer server) throws Exception {

        WebClient webClient = new WebClient();

        URL url = JSFUtils.createHttpUrl(server, contextRoot, "ActionListener.jsf");
        HtmlPage page = (HtmlPage) webClient.getPage(url);

        if (page == null) {
            Assert.fail("ActionListener.jsf did not render properly.");
        }

        // Click link to execute the methods and update the page
        HtmlElement button = (HtmlElement) page.getElementById("form:submitButton");
        page = button.click();

        //Small wait to make sure the call happens.
        Thread.sleep(2000);

        HtmlElement testBeanValue = (HtmlElement) page.getElementById("actionListenerBeanValue");

        assertNotNull(testBeanValue);

        LOG.info("Bean Value = " + testBeanValue.asText());

        assertTrue(testBeanValue.asText().contains(":ActionListenerBean:"));
        assertTrue(testBeanValue.asText().contains(":ActionListener:"));
        assertTrue(testBeanValue.asText().contains("com.ibm.ws.jsf23.fat.cdi.common.beans.injected.ManagedBeanFieldBean"));
        assertTrue(testBeanValue.asText().contains("com.ibm.ws.jsf23.fat.cdi.common.beans.injected.MethodBean"));
        assertTrue(testBeanValue.asText().contains(":PostConstructCalled:"));

    }

    /**
     * Test to ensure that CDI 2.0 injection works for a custom Navigation Handler
     * Field and Method injection but no Constructor Injection.
     * Also tested are use of request and session scope and use of qualifiers.
     *
     * @throws Exception. Content of the response should show if a specific injection failed.
     *
     */
    protected void testNavigationHandlerInjectionByApp(String contextRoot, LibertyServer server) throws Exception {

        WebClient webClient = new WebClient();

        URL url = JSFUtils.createHttpUrl(server, contextRoot, "NavigationHandler.jsf");
        HtmlPage page = (HtmlPage) webClient.getPage(url);

        if (page == null) {
            Assert.fail("NavigationHandler.jsf did not render properly.");
        }

        // Click link to execute the methods and update the page
        HtmlElement button = (HtmlElement) page.getElementById("form:submitButton");
        page = button.click();

        //Small wait to make sure the call happens.
        Thread.sleep(2000);

        HtmlElement testBeanValue = (HtmlElement) page.getElementById("navigationHandlerBeanValue");

        assertNotNull(testBeanValue);

        LOG.info("Bean Value = " + testBeanValue.asText());

        assertTrue(testBeanValue.asText().contains(":NavigationHandlerBean:"));
        assertTrue(testBeanValue.asText().contains(":NavigationHandler:"));
        assertTrue(testBeanValue.asText().contains("com.ibm.ws.jsf23.fat.cdi.common.beans.injected.ManagedBeanFieldBean"));
        assertTrue(testBeanValue.asText().contains("com.ibm.ws.jsf23.fat.cdi.common.beans.injected.MethodBean"));
        assertTrue(testBeanValue.asText().contains(":PostConstructCalled:"));

    }

    /**
     * Test to ensure that CDI 2.0 injection works for a custom EL Resolver
     * Field and Method injection but no Constructor Injection.
     * Also tested are use of request scope and use of qualifiers.
     *
     * @throws Exception. Content of the response should show if a specific injection failed.
     *
     */
    protected void testELResolverInjectionByApp(String contextRoot, LibertyServer server) throws Exception {

        // Use the SharedServer to verify a response.
        verifyResponse(contextRoot, "TestResolver.jsf", server,
                       ":TestCustomBean:", ":CustomELResolver:",
                       "com.ibm.ws.jsf23.fat.cdi.common.beans.injected.ManagedBeanFieldBean",
                       "com.ibm.ws.jsf23.fat.cdi.common.beans.injected.MethodBean",
                       ":PostConstructCalled:");
    }

    /**
     * Test that hits most of the managed factory classes, and system-event listener, and phase-listener. See faces-config.xml for details.
     * Most factories use delegate constructor method, so they are limited to tested basic field and method injection.
     *
     * Test Field and Method injection in system-event and phase listeners. No Constructor injection.
     *
     * Tests also use app scope as
     * request/session are not available to these managed classes that I can tell.
     *
     * @throws Exception
     */
    protected void testFactoryAndOtherAppScopedInjectionsByApp(String contextRoot, LibertyServer server) throws Exception {

        WebClient webClient = new WebClient();

        // Construct the URL for the test
        URL url = JSFUtils.createHttpUrl(server, contextRoot, "FactoryInfo.jsf");
        HtmlPage page = (HtmlPage) webClient.getPage(url);

        if (page == null) {
            Assert.fail("FactoryInfo.jsf did not render properly.");
        }

        String output = page.asText();

        LOG.info("Factory output value: " + output);

        // Verify we are matching each of the factories configured in faces-config.xml ( or Annotated configuration populator config)
        assertTrue("Did not find FactoryCount:14 in response.", output.contains("FactoryCount:14"));

        // Format:  Class | Method | Method injected class: Field injected class : <option> constructor injected class

        findInResponse(output, "CustomApplicationFactory|getApplication|FactoryDepBean:FactoryAppBean:PostConstructCalled");
        findInResponse(output, "CustomLifecycleFactory|getLifecycle|FactoryDepBean:FactoryAppBean:PostConstructCalled");
        findInResponse(output, "CustomExceptionHandlerFactory|getExceptionHandler|FactoryDepBean:FactoryAppBean:PostConstructCalled");
        findInResponse(output, "CustomExternalContextFactory|getExternalContext|FactoryDepBean:FactoryAppBean:PostConstructCalled");
        findInResponse(output, "CustomFacesContextFactory|getFacesContext|FactoryDepBean:FactoryAppBean:PostConstructCalled");
        findInResponse(output, "CustomRenderKitFactory|getRenderKit|FactoryDepBean:FactoryAppBean:PostConstructCalled");
        findInResponse(output, "CustomViewDeclarationLanguageFactory|getViewDeclarationLanguage|FactoryDepBean:FactoryAppBean:PostConstructCalled");
        findInResponse(output, "CustomTagHandlerDelegateFactory|createComponentHandlerDelegate|FactoryDepBean:FactoryAppBean:PostConstructCalled");
        findInResponse(output, "CustomPartialViewContextFactory|getPartialViewContext|FactoryDepBean:FactoryAppBean:PostConstructCalled");
        findInResponse(output, "CustomVisitContextFactory|getVisitContext|FactoryDepBean:FactoryAppBean:PostConstructCalled");
        findInResponse(output, "CustomPhaseListener|beforePhase|FactoryDepBean:FactoryAppBean:PostConstructCalled");
        findInResponse(output, "CustomSystemEventListener|processEvent|FactoryDepBean:FactoryAppBean:PostConstructCalled");
        findInResponse(output, "CustomClientWindowFactory|getClientWindow|FactoryDepBean:FactoryAppBean:PostConstructCalled");
        findInResponse(output, "CustomFaceletCacheFactory|getFaceletCache|FactoryDepBean:FactoryAppBean:PostConstructCalled");

    }

    private void findInResponse(String content, String key) throws Exception {
        assertTrue("Did not find " + key + "  in response",
                   content.contains(key));

    }

    /**
     * Test method and field injection for Custom resource handler. No intercepter or constructor injection on this.
     *
     * Would like to do something more than look for message in logs, a future improvement.
     *
     * @throws Exception
     */
    protected void testCustomResourceHandlerInjectionsByApp(String contextRoot, LibertyServer server) throws Exception {

        // Use the SharedServer to verify a response.
        this.verifyResponse(contextRoot, "index.jsf", "Hello Worldy world", server);

        String msg = "JSF23: CustomResourceHandler libraryExists called: result- class com.ibm.ws.jsf23.fat.cdi.common.beans.injected.MethodBean::class com.ibm.ws.jsf23.fat.cdi.common.beans.injected.ManagedBeanFieldBean::PostConstructCalled:/"
                     + contextRoot;

        // Check the trace.log to see if the proper InjectionProvider is being used.
        String isResourceHandlerMessage = server.waitForStringInLog(msg);

        // There should be a match so fail if there is not.
        assertNotNull("The following message was not found in the trace logs: " + msg,
                      isResourceHandlerMessage);

    }

    /**
     * Test method and field injection on custom state manager. No intercepter or constructor tests on this.
     *
     * Would like to do something more than look for message in logs, a future improvement.
     *
     * @throws Exception
     */
    protected void testCustomStateManagerInjectionsByApp(String contextRoot, LibertyServer server) throws Exception {

        WebClient webClient = new WebClient();

        // Construct the URL for the test
        URL url = JSFUtils.createHttpUrl(server, contextRoot, "NavigationHandler.jsf");
        HtmlPage page = (HtmlPage) webClient.getPage(url);

        if (page == null) {
            Assert.fail("NavigationHandler.jsf did not render properly.");
        }

        // The above request should cause the message below to be output in the log
        String msg = "JSF23: CustomStateManager isSavingStateInClient called: result- class com.ibm.ws.jsf23.fat.cdi.common.beans.injected.MethodBean::class com.ibm.ws.jsf23.fat.cdi.common.beans.injected.ManagedBeanFieldBean::PostConstructCalled:/"
                     + contextRoot;

        // Check the trace.log to see if the proper InjectionProvider is being used.
        String isStateManagerMessage = server.waitForStringInLog(msg);

        // There should be a match so fail if there is not.
        assertNotNull("The following message was not found in the trace logs: " + msg,
                      isStateManagerMessage);

    }

}
