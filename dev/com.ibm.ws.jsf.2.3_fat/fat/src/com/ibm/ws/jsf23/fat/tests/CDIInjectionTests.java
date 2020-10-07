/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jsf23.fat.CDITestBase;
import com.ibm.ws.jsf23.fat.JSFUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.rules.repeater.JakartaEE9Action;
import junit.framework.Assert;

/**
 * This is one of four CDI test applications, with configuration loaded in the following manner:
 * CDIInjectionTests - WEB-INF/faces-config.xml
 *
 * We're extending CDITestBase, which has common test code.
 */
@RunWith(FATRunner.class)
public class CDIInjectionTests extends CDITestBase {
    private static final Logger LOG = Logger.getLogger(CDIInjectionTests.class.getName());

    @Server("jsf23CDIServer")
    public static LibertyServer jsf23CDIServer;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(jsf23CDIServer, "CDIInjectionTests.war",
                                      "com.ibm.ws.jsf23.fat.cdi.injection.beans.injected",
                                      "com.ibm.ws.jsf23.fat.cdi.injection.beans.viewscope",
                                      "com.ibm.ws.jsf23.fat.cdi.common.beans",
                                      "com.ibm.ws.jsf23.fat.cdi.common.beans.factory",
                                      "com.ibm.ws.jsf23.fat.cdi.common.beans.injected",
                                      "com.ibm.ws.jsf23.fat.cdi.common.managed",
                                      "com.ibm.ws.jsf23.fat.cdi.common.managed.factories",
                                      "com.ibm.ws.jsf23.fat.cdi.common.managed.factories.client.window");

        ShrinkHelper.defaultDropinApp(jsf23CDIServer, "ActionListenerInjection.war",
                                      "com.ibm.ws.jsf23.fat.cdi.common.beans",
                                      "com.ibm.ws.jsf23.fat.cdi.common.beans.factory",
                                      "com.ibm.ws.jsf23.fat.cdi.common.beans.injected",
                                      "com.ibm.ws.jsf23.fat.cdi.common.managed",
                                      "com.ibm.ws.jsf23.fat.cdi.common.managed.factories");

        // Start the server and use the class name so we can find logs easily.
        // Many tests use the same server
        jsf23CDIServer.startServer(CDIInjectionTests.class.getSimpleName() + ".log");

    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsf23CDIServer != null && jsf23CDIServer.isStarted()) {
            jsf23CDIServer.stopServer();
        }
    }

    /**
     * Test to ensure that CDI 2.0 injection works for a custom action listener
     * Field and Method injection, but no Constructor injection.
     * Also tested are use of request and session scope and use of qualifiers.
     *
     * @throws Exception. Content of the response should show if a specific injection failed.
     *
     */
    @Test
    public void testActionListenerInjection_CDIInjectionTests() throws Exception {
        testActionListenerInjectionByApp("ActionListenerInjection", jsf23CDIServer);
    }

    /**
     * Test to ensure that CDI 2.0 injection works for a custom Navigation Handler
     * Field and Method injection, but no Constructor injection.
     * Also tested are use of request and session scope and use of qualifiers.
     *
     * @throws Exception. Content of the response should show if a specific injection failed.
     *
     */
    @Test
    public void testNavigationHandlerInjection_CDIInjectionTests() throws Exception {
        testNavigationHandlerInjectionByApp("CDIInjectionTests", jsf23CDIServer);
    }

    /**
     * Test to ensure that CDI 2.0 injection works for a custom EL Resolver
     * Field and Method injection, but no Constructor injection.
     * Also tested are use of request scope and use of qualifiers.
     *
     * @throws Exception. Content of the response should show if a specific injection failed.
     *
     */
    @Test
    public void testELResolverInjection_CDIInjectionTests() throws Exception {
        testELResolverInjectionByApp("CDIInjectionTests", jsf23CDIServer);
    }

    /**
     * Test method and field injection for Custom resource handler. No intercepter or constructor injection on this.
     *
     * Would like to do something more than look for message in logs, a future improvement.
     *
     * @throws Exception
     */
    @Test
    public void testCustomResourceHandlerInjections_CDIInjectionTests() throws Exception {
        testCustomResourceHandlerInjectionsByApp("CDIInjectionTests", jsf23CDIServer);

    }

    /**
     * Test method and field injection on custom state manager. No intercepter or constructor tests on this.
     *
     * Would like to do something more than look for message in logs, a future improvement.
     *
     * @throws Exception
     */
    @Test
    public void testCustomStateManagerInjections_CDIInjectionTests() throws Exception {
        testCustomStateManagerInjectionsByApp("CDIInjectionTests", jsf23CDIServer);
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
    @Test
    public void testFactoryAndOtherScopeInjections_CDIInjectionTests() throws Exception {
        testFactoryAndOtherAppScopedInjectionsByApp("CDIInjectionTests", jsf23CDIServer);
    }

    /**
     * Test to ensure that the org.apache.myfaces.spi.InjectionProvider specified in the META-INF/services
     * directory is being loaded and the specified InjectionProvider used.
     *
     * Also ensure that when running on a Server with the cdi-2.0 feature enabled that JSF knows that MyFaces CDI Support
     * is enabled.
     *
     * @throws Exception
     */
    @Test
    public void testInjectionProvider() throws Exception {
        String msgToSearchFor1 = "Using InjectionProvider com.ibm.ws.jsf.spi.impl.WASCDIAnnotationDelegateInjectionProvider";

        String msgToSearchFor2 = "MyFaces CDI support enabled";

        if(JakartaEE9Action.isActive()){
          msgToSearchFor2 = "MyFaces Core CDI support enabled";
        }


        this.verifyResponse("CDIInjectionTests", "index.xhtml", "Hello Worldy world", jsf23CDIServer);

        // Check the trace.log to see if the proper InjectionProvider is being used.
        String isInjectionProviderBeingLoaded = jsf23CDIServer.waitForStringInTrace(msgToSearchFor1, 30 * 1000);

        // Reset the log search offset position to avoid conflict with other searches
        jsf23CDIServer.resetLogOffsets();

        String isCDISupportEnabled = jsf23CDIServer.waitForStringInLog(msgToSearchFor2, 30 * 1000);

        // There should be a match so fail if there is not.
        assertNotNull("The following message was not found in the trace logs: " + msgToSearchFor1,
                      isInjectionProviderBeingLoaded);

        assertNotNull("The following message was not found in the logs: " + msgToSearchFor2,
                      isCDISupportEnabled);
    }

    /**
     * Test to ensure that CDI 2.0 injection works for a basic managed bean.
     * Field and Method injection, but no Constructor injection.
     * Also tested are use of request scope and use of qualifiers.
     *
     * @throws Exception. Content of the response should show if a specific injection failed.
     *
     */
    @Test
    public void testBeanInjection() throws Exception {
        this.verifyResponse("CDIInjectionTests", "TestBean.jsf", jsf23CDIServer,
                            ":TestBean:", "class com.ibm.ws.jsf23.fat.cdi.common.beans.injected.TestBeanFieldBean",
                            "com.ibm.ws.jsf23.fat.cdi.common.beans.injected.MethodBean",
                            ":PostConstructCalled:");
    }

    /**
     *
     * Test CDI injections on CDI ViewScope. Tests field, and method injections (no constructor injection) with app, session, request, and dependent scopes.
     * Does some simple verifications of the 4 scopes and instances ( through hashcode) are what is expected for multiple requests.
     */
    @Test
    public void testViewScopeInjections() throws Exception {
        String contextRoot = "CDIInjectionTests";

        try (WebClient webClient = new WebClient(); WebClient webClient2 = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "ViewScope.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Make sure the page initially renders correctly
            if (page == null) {
                Assert.fail("ViewScope.xhtml did not render properly.");
            }

            LOG.info("First request output is:" + page.asText());

            int app = getAreaHashCode(page, "vab");
            int sess = getAreaHashCode(page, "vsb");
            int req = getAreaHashCode(page, "vrb");
            int dep = getAreaHashCode(page, "vdb");

            HtmlElement button = (HtmlElement) page.getElementById("button:test");
            page = button.click();

            if (page == null) {
                Assert.fail("ViewScope.xhtml did not render properly after button press.");
            }

            LOG.info("After button click content is:" + page.asText());

            int app2 = getAreaHashCode(page, "vab");
            int sess2 = getAreaHashCode(page, "vsb");
            int req2 = getAreaHashCode(page, "vrb");
            int dep2 = getAreaHashCode(page, "vdb");

            Assert.assertEquals("App Scoped beans were not identical for consecutive requests.", app, app2);
            Assert.assertEquals("Session Scoped beans were not identical for consecutive requests.", sess, sess2);
            Assert.assertTrue("Request bean is equivalent when it should not be.", (req != req2));
            Assert.assertTrue("Dependent bean is equivalent when it should not be.", (dep != dep2));

            webClient2.getCookieManager().clearCookies();

            // Construct the URL for the test
            url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "ViewScope.jsf");
            HtmlPage page2 = (HtmlPage) webClient2.getPage(url);

            // Make sure the page initially renders correctly
            if (page2 == null) {
                Assert.fail("ViewScope.xhtml did not render properly for second client.");
            }

            LOG.info("Second client page request content:" + page2.asText());

            int app3 = getAreaHashCode(page2, "vab");
            int sess3 = getAreaHashCode(page2, "vsb");
            int req3 = getAreaHashCode(page2, "vrb");
            int dep3 = getAreaHashCode(page2, "vdb");

            Assert.assertEquals("App Scoped beans were not identical for two different clients.", app, app3);
            Assert.assertTrue("Session Scoped bean is equivalent when it should not be.", (sess != sess3));
            Assert.assertTrue("Request bean is equivalent when it should not be.", (req2 != req3));
            Assert.assertTrue("Dependent bean is equivalent when it should not be.", (dep != dep3));
        }

    }

    private int getAreaHashCode(HtmlPage page, String area) {
        int retValue = 0;

        HtmlElement sess = (HtmlElement) page.getElementById(area + "Area");
        if (sess != null) {
            retValue = Integer.valueOf(sess.asText());
        }

        return retValue;
    }

    @Test
    public void testPreDestroyInjection() throws Exception {
        String contextRootCDIInjectionTests = "CDIInjectionTests";
        String contextRootActionListenerInjection = "ActionListenerInjection";

        // Drive requests to ensure all injected objected are created.
        this.verifyStatusCode(contextRootCDIInjectionTests, "index.xhtml", 200, jsf23CDIServer);
        this.verifyStatusCode(contextRootCDIInjectionTests, "index.xhtml", 200, jsf23CDIServer);
        this.verifyStatusCode(contextRootCDIInjectionTests, "TestBean.jsf", 200, jsf23CDIServer);
        this.verifyStatusCode(contextRootActionListenerInjection, "ActionListener.jsf", 200, jsf23CDIServer);

        // Restart the app so that preDestory gets called;
        // make sure we reset log offsets correctly
        jsf23CDIServer.setMarkToEndOfLog();
        jsf23CDIServer.restartDropinsApplication("CDIInjectionTests.war");
        jsf23CDIServer.restartDropinsApplication("ActionListenerInjection.war");
        jsf23CDIServer.resetLogOffsets();

        // Now check the preDestoys
        assertFalse("CustomActionListener preDestroy not called", jsf23CDIServer.findStringsInLogs("CustomActionListener preDestroy called.").isEmpty());
        assertFalse("CustomELResolver preDestroy not called", jsf23CDIServer.findStringsInLogs("CustomELResolver preDestroy called.").isEmpty());
        assertFalse("CustomNavigationHandler preDestroy not called", jsf23CDIServer.findStringsInLogs("CustomNavigationHandler preDestroy called").isEmpty());
        assertFalse("TestBean preDestroy not called", jsf23CDIServer.findStringsInLogs("TestBean preDestroy called.").isEmpty());

        assertFalse("CustomStateManager preDestroy not called", jsf23CDIServer.findStringsInLogs("CustomStateManager preDestroy called").isEmpty());
        assertFalse("CustomResourceHandler preDestroy not called", jsf23CDIServer.findStringsInLogs("CustomResourceHandler preDestroy called.").isEmpty());

        assertFalse("CustomApplicationFactory preDestroy not called", jsf23CDIServer.findStringsInLogs("CustomApplicationFactory preDestroy called.").isEmpty());
        assertFalse("CustomLifecycleFactory preDestroy not called", jsf23CDIServer.findStringsInLogs("CustomLifecycleFactory preDestroy called.").isEmpty());
        assertFalse("CustomExceptionHandlerFactory preDestroy not called",
                    jsf23CDIServer.findStringsInLogs("CustomExceptionHandlerFactory preDestroy called.").isEmpty());
        assertFalse("CustomExternalContextFactory preDestroy not called",
                    jsf23CDIServer.findStringsInLogs("CustomExternalContextFactory preDestroy called.").isEmpty());
        assertFalse("CustomFacesContextFactory preDestroy not called",
                    jsf23CDIServer.findStringsInLogs("CustomFacesContextFactory preDestroy called.").isEmpty());
        assertFalse("CustomRenderKitFactory preDestroy not called", jsf23CDIServer.findStringsInLogs("CustomRenderKitFactory preDestroy called.").isEmpty());
        assertFalse("CustomViewDeclarationLanguageFactory preDestroy not called",
                    jsf23CDIServer.findStringsInLogs("CustomViewDeclarationLanguageFactory preDestroy called.").isEmpty());
        assertFalse("CustomTagHandlerDelegateFactory preDestroy not called",
                    jsf23CDIServer.findStringsInLogs("CustomTagHandlerDelegateFactory preDestroy called.").isEmpty());
        assertFalse("CustomPartialViewContextFactory preDestroy not called",
                    jsf23CDIServer.findStringsInLogs("CustomPartialViewContextFactory preDestroy called.").isEmpty());
        assertFalse("CustomVisitContextFactory preDestroy not called",
                    jsf23CDIServer.findStringsInLogs("CustomVisitContextFactory preDestroy called.").isEmpty());
        assertFalse("CustomPhaseListener preDestroy not called", jsf23CDIServer.findStringsInLogs("CustomPhaseListener preDestroy called.").isEmpty());
        assertFalse("CustomSystemEventListener preDestroy not called",
                    jsf23CDIServer.findStringsInLogs("CustomSystemEventListener preDestroy called.").isEmpty());
    }

}
