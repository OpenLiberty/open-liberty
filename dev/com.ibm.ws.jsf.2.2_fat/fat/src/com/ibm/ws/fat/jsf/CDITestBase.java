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
package com.ibm.ws.fat.jsf;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import junit.framework.Assert;

import org.junit.AfterClass;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.ws.fat.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebResponse;

/**
 * This is the base class which is extended by the other JSF 2.2 CDI tests.
 * The methods here are executed by those other test classes with their various configurations.
 */
public class CDITestBase extends LoggingTest {
    private static final Logger LOG = Logger.getLogger(CDITestBase.class.getName());

    @AfterClass
    public static void testCleanup() throws Exception {
        // test cleanup
    }

    protected WebResponse verifyResponse(String resource, String expectedResponse, SharedServer sharedServer) throws Exception {
        return sharedServer.verifyResponse(createWebBrowserForTestCase(), resource, expectedResponse);
    }

    protected WebResponse verifyResponse(String resource, SharedServer sharedServer, String... expectedResponseStrings) throws Exception {
        return sharedServer.verifyResponse(createWebBrowserForTestCase(), resource, expectedResponseStrings);
    }

    protected void verifyStatusCode(String resource, int statusCode, SharedServer sharedServer) throws Exception {
        sharedServer.verifyStatusCode(createWebBrowserForTestCase(), resource, statusCode);
    }

    protected void verifyStatusCode(String resource, String buttonID, int statusCode, SharedServer sharedServer) throws Exception {
        WebClient webClient = new WebClient();

        HtmlPage page = (HtmlPage) webClient.getPage(sharedServer.getServerUrl(true, resource));

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
     * Test to ensure that CDI 1.2 injection works for an custom action listener
     * Field, Method and Constructor Injection, and Interceptors. Also
     * tested are use of request and session scope and use of qualifiers.
     * 
     * @throws Exception. Content of the response should show if a specific injection failed.
     * 
     */
    protected void testActionListenerInjectionByApp(String app, SharedServer sharedServer) throws Exception {

        WebClient webClient = new WebClient();

        HtmlPage page = (HtmlPage) webClient.getPage(sharedServer.getServerUrl(true, "/" + app + "/ActionListener.jsf"));

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
        assertTrue(testBeanValue.asText().contains("cdi.beans.injected.ManagedBeanFieldBean"));
        assertTrue(testBeanValue.asText().contains("cdi.beans.injected.ConstructorBean"));
        assertTrue(testBeanValue.asText().contains("cdi.beans.injected.MethodBean"));
        assertTrue(testBeanValue.asText().contains(":TestActionListenerInterceptor:"));
        assertTrue(testBeanValue.asText().contains(":PostConstructCalled:"));

    }

    /**
     * Test to ensure that CDI 1.2 injection works for a custom Navigation Handler
     * Field, Method and Constructor Injection, and Interceptors. Also
     * tested are use of request and session scope and use of qualifiers.
     * 
     * @throws Exception. Content of the response should show if a specific injection failed.
     * 
     */
    protected void testNavigationHandlerInjectionByApp(String app, SharedServer sharedServer) throws Exception {

        WebClient webClient = new WebClient();

        HtmlPage page = (HtmlPage) webClient.getPage(sharedServer.getServerUrl(true, "/" + app + "/NavigationHandler.jsf"));

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
        assertTrue(testBeanValue.asText().contains("cdi.beans.injected.ManagedBeanFieldBean"));
        assertTrue(testBeanValue.asText().contains("cdi.beans.injected.ConstructorBean"));
        assertTrue(testBeanValue.asText().contains("cdi.beans.injected.MethodBean"));
        assertTrue(testBeanValue.asText().contains(":TestNavigationHandlerInterceptor:"));
        assertTrue(testBeanValue.asText().contains(":PostConstructCalled:"));

    }

    /**
     * Test to ensure that CDI 1.2 injection works for a custom EL Resolver
     * Field, Method and Constructor Injection, and Interceptors. Also
     * tested are use of request scope and use of qualifiers.
     * 
     * @throws Exception. Content of the response should show if a specific injection failed.
     * 
     */
    protected void testELResolverInjectionByApp(String app, SharedServer sharedServer) throws Exception {

        // Use the SharedServer to verify a response.
        verifyResponse("/" + app + "/TestResolver.jsf", sharedServer,
                       ":TestCustomBean:", ":CustomELResolver:",
                       "cdi.beans.injected.ManagedBeanFieldBean",
                       "cdi.beans.injected.ConstructorBean",
                       "cdi.beans.injected.MethodBean",
                       ":TestCustomResolverInterceptor:",
                       ":PostConstructCalled:");
    }

    /**
     * Test that hits most of the managed factory classes, and system-event listener, and phase-listener. See faces-config.xml for details.
     * Most factories use delegate constructor method, so they are limited to tested basic field and method injection. Tests also use app scope as
     * request/session are not available to these managed classes that I can tell.
     * 
     * @throws Exception
     */
    protected void testFactoryAndOtherAppScopedInjectionsByApp(String app, SharedServer sharedServer) throws Exception {

        WebClient webClient = new WebClient();

        HtmlPage page = (HtmlPage) webClient.getPage(sharedServer.getServerUrl(true,
                                                                               "/" + app + "/FactoryInfo.jsf"));

        if (page == null) {
            Assert.fail("FactoryInfo.jsf did not render properly.");
        }

        String output = page.asText();

        LOG.info("Factory output value: " + output);

        // Verify we are matching each of the factories configured in faces-config.xml ( or Annotated configuration populator config)
        assertTrue("Did not find FactoryCount:11 in response.", output.contains("FactoryCount:12"));

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
        findInResponse(output, "CustomPhaseListener|beforePhase|FactoryDepBean:FactoryAppBean:FactoryAppBean:PostConstructCalled");
        findInResponse(output, "CustomSystemEventListener|processEvent|FactoryDepBean:FactoryAppBean:FactoryAppBean:PostConstructCalled");

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
    protected void testCustomResourceHandlerInjectionsByApp(String app, SharedServer sharedServer) throws Exception {

        // Use the SharedServer to verify a response.
        this.verifyResponse("/" + app + "/index.jsf", "Hello Worldy world", sharedServer);

        String msg = "JSF22: CustomResourceHandler libraryExists called: result- class cdi.beans.injected.MethodBean::class cdi.beans.injected.ManagedBeanFieldBean::PostConstructCalled:/"
                     + app;

        // Check the trace.log to see if the proper InjectionProvider is being used.
        String isResourceHandlerMessage =
                        sharedServer.getLibertyServer().waitForStringInLog(msg);

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
    protected void testCustomStateManagerInjectionsByApp(String app, SharedServer sharedServer) throws Exception {

        WebClient webClient = new WebClient();

        HtmlPage page = (HtmlPage) webClient.getPage(sharedServer.getServerUrl(true, "/" + app + "/ActionListener.jsf"));

        if (page == null) {
            Assert.fail("ActionListener.jsf did not render properly.");
        }

        // Click link to execute the methods and update the page; this should cause the message below 
        // to be output in the log
        HtmlElement button = (HtmlElement) page.getElementById("form:submitButton");
        page = button.click();

        String msg = "JSF22: CustomStateManager isSavingStateInClient called: result- class cdi.beans.injected.MethodBean::class cdi.beans.injected.ManagedBeanFieldBean::PostConstructCalled:/"
                     + app;

        // Check the trace.log to see if the proper InjectionProvider is being used.
        String isResourceHandlerMessage =
                        sharedServer.getLibertyServer().waitForStringInLog(msg);

        // There should be a match so fail if there is not.
        assertNotNull("The following message was not found in the trace logs: " + msg,
                      isResourceHandlerMessage);

    }
}
