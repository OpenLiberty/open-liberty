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

import java.net.URL;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf22.fat.CDITestBase;
import com.ibm.ws.jsf22.fat.JSFUtils;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import junit.framework.Assert;

/**
 * This is one of four CDI test applications, with configuration loaded in the following manner:
 * CDITests - WEB-INF/faces-config.xml
 *
 * We're extending CDITestBase, which has common test code.
 *
 * NOTE: These tests should not run with jsf-2.3 feature because constructor injection is not supported.
 * As a result, these tests were modified to run in the JSF 2.3 FAT bucket without constructor injection.
 */
@RunWith(FATRunner.class)
@SkipForRepeat({ EE8_FEATURES, EE9_FEATURES })
public class CDITests extends CDITestBase {
    @Rule
    public TestName name = new TestName();

    String contextRoot = "CDITests";

    protected static final Class<?> c = CDITests.class;

    @Server("jsfCDIServer")
    public static LibertyServer jsfCDIServer;

    @BeforeClass
    public static void setup() throws Exception {

        //CDITests.war uses CDICommon packages
        WebArchive CDITestsWar = ShrinkHelper.buildDefaultApp("CDITests.war", "com.ibm.ws.jsf22.fat.cditests.beans.*", "com.ibm.ws.jsf22.fat.cdicommon.*");

        ShrinkHelper.exportDropinAppToServer(jsfCDIServer, CDITestsWar);

        jsfCDIServer.startServer(CDITests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsfCDIServer != null && jsfCDIServer.isStarted()) {
            jsfCDIServer.stopServer();
        }
    }

    /**
     * Test to ensure that CDI 1.2 injection works for an custom action listener
     * Field, Method and Constructor Injection, and Interceptors. Also
     * tested are use of request and session scope and use of qualifiers.
     *
     * @throws Exception. Content of the response should show if a specific injection failed.
     *
     */
    @Test
    public void testActionListenerInjection_CDITests() throws Exception {
        testActionListenerInjectionByApp("CDITests", jsfCDIServer);
    }

    /**
     * Test to ensure that CDI 1.2 injection works for a custom Navigation Handler
     * Field, Method and Constructor Injection, and Interceptors. Also
     * tested are use of request and session scope and use of qualifiers.
     *
     * @throws Exception. Content of the response should show if a specific injection failed.
     *
     */
    @Test
    public void testNavigationHandlerInjection_CDITests() throws Exception {
        testNavigationHandlerInjectionByApp("CDITests", jsfCDIServer);
    }

    /**
     * Test to ensure that CDI 1.2 injection works for a custom EL Resolver
     * Field, Method and Constructor Injection, and Interceptors. Also
     * tested are use of request scope and use of qualifiers.
     *
     * @throws Exception. Content of the response should show if a specific injection failed.
     *
     */
    @Test
    public void testELResolverInjection_CDITests() throws Exception {
        testELResolverInjectionByApp("CDITests", jsfCDIServer);
    }

    /**
     * Test method and field injection for Custom resource handler. No intercepter or constructor injection on this.
     *
     * Would like to do something more than look for message in logs, a future improvement.
     *
     * @throws Exception
     */
    @Test
    public void testCustomResourceHandlerInjections_CDITests() throws Exception {
        testCustomResourceHandlerInjectionsByApp("CDITests", jsfCDIServer);

    }

    /**
     * Test method and field injection on custom state manager. No intercepter or constructor tests on this.
     *
     * Would like to do something more than look for message in logs, a future improvement.
     *
     * @throws Exception
     */
    @Test
    public void testCustomStateManagerInjections_CDITests() throws Exception {
        testCustomStateManagerInjectionsByApp("CDITests", jsfCDIServer);
    }

    /**
     * Test that hits most of the managed factory classes, and system-event listener, and phase-listener. See faces-config.xml for details.
     * Most factories use delegate constructor method, so they are limited to tested basic field and method injection. Tests also use app scope as
     * request/session are not available to these managed classes that I can tell.
     *
     * @throws Exception
     */
    @Test
    public void testFactoryAndOtherScopeInjections_CDITests() throws Exception {
        testFactoryAndOtherAppScopedInjectionsByApp("CDITests", jsfCDIServer);
    }

    /**
     * Test to ensure that the org.apache.myfaces.spi.InjectionProvider specified in the META-INF/services
     * directory is being loaded and the specified InjectionProvider used.
     *
     * Also ensure that when running on a Server with the cdi-1.2 feature enabled that JSF knows that MyFaces CDI Support
     * is enabled.
     *
     * @throws Exception
     */
    @Test
    public void testInjectionProvider() throws Exception {
        String msgToSearchFor1 = "Using InjectionProvider com.ibm.ws.jsf.spi.impl.WASCDIAnnotationDelegateInjectionProvider";
        String msgToSearchFor2 = "MyFaces CDI support enabled";

        this.verifyResponse("CDITests", "index.xhtml", jsfCDIServer, "Hello Worldy world");

        // Check the trace.log to see if the proper InjectionProvider is being used.
        String isInjectionProviderBeingLoaded = jsfCDIServer.waitForStringInTrace(msgToSearchFor1, 30 * 1000);

        // Reset the log search offset position to avoid conflict with other searches
        jsfCDIServer.resetLogOffsets();

        String isCDISupportEnabled = jsfCDIServer.waitForStringInLog(msgToSearchFor2, 30 * 1000);

        // There should be a match so fail if there is not.
        assertNotNull("The following message was not found in the trace logs: " + msgToSearchFor1,
                      isInjectionProviderBeingLoaded);

        assertNotNull("The following message was not found in the logs: " + msgToSearchFor2,
                      isCDISupportEnabled);
    }

    /**
     * Test to ensure that CDI 1.2 injection works for a basic managed bean.
     * Field, Method and Constructor Injection, and Interceptors. Also
     * tested are use of request scope and use of qualifiers.
     *
     * @throws Exception. Content of the response should show if a specific injection failed.
     *
     */
    @Test
    public void testBeanInjection() throws Exception {
        this.verifyResponse("CDITests", "TestBean.jsf", jsfCDIServer,
                            ":TestBean:", "class com.ibm.ws.jsf22.fat.cdicommon.beans.injected.TestBeanFieldBean",
                            "class com.ibm.ws.jsf22.fat.cdicommon.beans.injected.ConstructorBean",
                            "com.ibm.ws.jsf22.fat.cdicommon.beans.injected.MethodBean",
                            ":TestPlainBeanInterceptor:",
                            ":PostConstructCalled:");
    }

    /**
     *
     * Test CDI injections on CDI ViewScope. Tests constructor, field, and method injections with app, session, request, and dependent scopes.
     * Does some simple verifications of the 4 scopes and instances ( through hashcode) are what is expected for multiple requests.
     */
    @Test
    public void testViewScopeInjections() throws Exception {
        try (WebClient webClient = new WebClient(); WebClient webClient2 = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfCDIServer, contextRoot, "ViewScope.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Make sure the page initially renders correctly
            if (page == null) {
                Assert.fail("ViewScope.xhtml did not render properly.");
            }

            Log.info(c, name.getMethodName(), "First request output is:" + page.asText());

            int app = getAreaHashCode(page, "vab");
            int sess = getAreaHashCode(page, "vsb");
            int req = getAreaHashCode(page, "vrb");
            int dep = getAreaHashCode(page, "vdb");

            HtmlElement button = (HtmlElement) page.getElementById("button:test");
            page = button.click();

            if (page == null) {
                Assert.fail("ViewScope.xhtml did not render properly after button press.");
            }

            Log.info(c, name.getMethodName(), "After button click content is:" + page.asText());

            int app2 = getAreaHashCode(page, "vab");
            int sess2 = getAreaHashCode(page, "vsb");
            int req2 = getAreaHashCode(page, "vrb");
            int dep2 = getAreaHashCode(page, "vdb");

            Assert.assertEquals("App Scoped beans were not identical for consecutive requests.", app, app2);
            Assert.assertEquals("Session Scoped beans were not identical for consecutive requests.", sess, sess2);
            Assert.assertTrue("Request bean is equivalent when it should not be.", (req != req2));
            Assert.assertTrue("Dependent bean is equivalent when it should not be.", (dep != dep2));

            webClient2.getCookieManager().clearCookies();
            HtmlPage page2 = (HtmlPage) webClient2.getPage(url);

            // Make sure the page initially renders correctly
            if (page2 == null) {
                Assert.fail("ViewScope.xhtml did not render properly for second client.");
            }

            Log.info(c, name.getMethodName(), "Second client page request content:" + page2.asText());

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
        // Drive requests to ensure all injected objected are created.
        this.verifyStatusCode("CDITests", "index.xhtml", 200, jsfCDIServer);
        this.verifyStatusCode("CDITests", "TestBean.jsf", 200, jsfCDIServer);
        this.verifyStatusCode("CDITests", "ActionListener.jsf", "form:submitButton", 200, jsfCDIServer);
        this.verifyStatusCode("CDITests", "FactoryInfo.jsf", 200, jsfCDIServer);

        // Restart the app so that preDestory gets called;
        // make sure we reset log offsets correctly
        jsfCDIServer.setMarkToEndOfLog();
        Assert.assertTrue("The CDITests.war application was not restarted.", jsfCDIServer.restartDropinsApplication("CDITests.war"));
        jsfCDIServer.resetLogOffsets();

        // Now check the preDestoys
        assertFalse("CustomActionListener preDestroy not called", jsfCDIServer.findStringsInLogs("CustomActionListener preDestroy called").isEmpty());
        assertFalse("CustomELResolver preDestroy not called", jsfCDIServer.findStringsInLogs("CustomELResolver preDestroy called.").isEmpty());
        assertFalse("CustomNavigationHandler preDestroy not called", jsfCDIServer.findStringsInLogs("CustomNavigationHandler preDestroy called").isEmpty());
        assertFalse("TestBean preDestroy not called", jsfCDIServer.findStringsInLogs("TestBean preDestroy called.").isEmpty());

        assertFalse("CustomStateManager preDestroy not called", jsfCDIServer.findStringsInLogs("CustomNavigationHandler preDestroy called").isEmpty());
        assertFalse("CustomResourceHandler preDestroy not called", jsfCDIServer.findStringsInLogs("TestBean preDestroy called.").isEmpty());

        assertFalse("CustomApplicationFactory preDestroy not called", jsfCDIServer.findStringsInLogs("CustomApplicationFactory preDestroy called.").isEmpty());
        assertFalse("CustomLifecycleFactory preDestroy not called", jsfCDIServer.findStringsInLogs("CustomLifecycleFactory preDestroy called.").isEmpty());
        assertFalse("CustomExceptionHandlerFactory preDestroy not called",
                    jsfCDIServer.findStringsInLogs("CustomExceptionHandlerFactory preDestroy called.").isEmpty());
        assertFalse("CustomExternalContextFactory preDestroy not called",
                    jsfCDIServer.findStringsInLogs("CustomExternalContextFactory preDestroy called.").isEmpty());
        assertFalse("CustomFacesContextFactory preDestroy not called",
                    jsfCDIServer.findStringsInLogs("CustomFacesContextFactory preDestroy called.").isEmpty());
        assertFalse("CustomRenderKitFactory preDestroy not called", jsfCDIServer.findStringsInLogs("CustomRenderKitFactory preDestroy called.").isEmpty());
        assertFalse("CustomViewDeclarationLanguageFactory preDestroy not called",
                    jsfCDIServer.findStringsInLogs("CustomViewDeclarationLanguageFactory preDestroy called.").isEmpty());
        assertFalse("CustomTagHandlerDelegateFactory preDestroy not called",
                    jsfCDIServer.findStringsInLogs("CustomTagHandlerDelegateFactory preDestroy called.").isEmpty());
        assertFalse("CustomPartialViewContextFactory preDestroy not called",
                    jsfCDIServer.findStringsInLogs("CustomPartialViewContextFactory preDestroy called.").isEmpty());
        assertFalse("CustomVisitContextFactory preDestroy not called",
                    jsfCDIServer.findStringsInLogs("CustomVisitContextFactory preDestroy called.").isEmpty());
        assertFalse("CustomPhaseListener preDestroy not called", jsfCDIServer.findStringsInLogs("CustomPhaseListener preDestroy called.").isEmpty());
        assertFalse("CustomSystemEventListener preDestroy not called",
                    jsfCDIServer.findStringsInLogs("CustomSystemEventListener preDestroy called.").isEmpty());
    }
}
