/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.myfaces41.fat.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.logging.Logger;

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

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.org.apache.myfaces41.fat.JSFUtils;

/*
 * Tests Spec changes related to CDI
 *
 * Spec 1739: Require firing events for @Initialized, @BeforeDestroyed, @Destroyed for build-in scopes
 * - https://github.com/jakartaee/faces/issues/1739
 * - Applies to FlowScoped and ViewScoped
 *
 * Spec 1342: Support @Inject of current flow like "@Inject Flow currentFlow"
 * - https://github.com/jakartaee/faces/issues/1342
 */
@RunWith(FATRunner.class)
public class Faces41CDITests {

    private static final String FLOW_APP_NAME = "Flow_Spec1342_Spec1739";
    private static final String VIEW_APP_NAME = "ViewScopeEvents_Spec1739";
    protected static final Class<?> c = Faces41CDITests.class;

    private static final Logger LOG = Logger.getLogger(Faces41CDITests.class.getName());

    private boolean debug = true; // Logs Pages as XML when true

    @Rule
    public TestName name = new TestName();

    @Server("faces41_cdiServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, FLOW_APP_NAME + ".war",
                                      "io.openliberty.org.apache.myfaces41.fat.flow.beans");

        ShrinkHelper.defaultDropinApp(server, VIEW_APP_NAME + ".war",
                                      "io.openliberty.org.apache.myfaces41.fat.view.bean");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(Faces41CDITests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }

    }

    /**
     * Tests Spec 1739 for FlowScoped.
     * 1) Enter flow to create the FlowScoped Bean
     * 2) Verify the @Initialized method occurred (via FacesMessage on the page)
     * 3) Exit the flow
     * 4) Verify the @BeforeDestroyed method occurred (via FacesMessage on the page)
     * 5) Verify the @Destroyed method occurred (via FacesMessage on the page)
     *
     * @throws Exception
     */
    @Test
    public void testFlowScopedEventsFire() throws Exception {

        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(server, FLOW_APP_NAME, "index.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            HtmlElement enterFlowButton = (HtmlElement) page.getElementById("simple");
            page = enterFlowButton.click();

            logPage(page);

            assertTrue("@Initialized message not found on Page!", page.asText().contains("Initialized FLOW"));

            HtmlElement exitFlowButton = (HtmlElement) page.getElementById("button2");
            page = exitFlowButton.click();

            logPage(page);

            assertTrue("@BeforeDestroyed message not found on Page!", page.asText().contains("BeforeDestroyed FLOW"));
            assertTrue("@Destroyed message not found on Page!", page.asText().contains("Destroyed FLOW"));
        }

    }

    /**
     * Tests Spec 1739 for ViewScoped.
     * 1) To to a page to start a view
     * 2) Verify the @Initialized method occurred (via FacesMessage on the page)
     * 3) Click button to vist another
     * 4) Verify the @BeforeDestroyed method occurred (via FacesMessage on the page)
     * 5) Verify the @Destroyed method occurred (via FacesMessage on the page)
     *
     * @throws Exception
     */
    @Test
    public void testViewScopedEventsFire() throws Exception {

        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(server, VIEW_APP_NAME, "view.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            logPage(page);

            assertTrue("@Initialized message not found on Page!", page.asText().contains("Initialized VIEW"));

            HtmlElement nextViewButton = (HtmlElement) page.getElementById("form:goToView2");
            page = nextViewButton.click();

            logPage(page);

            assertTrue("@BeforeDestroyed message not found on Page!", page.asText().contains("BeforeDestroyed VIEW"));
            assertTrue("@Destroyed message not found on Page!", page.asText().contains("Destroyed VIEW"));
        }
    }

    /**
     * Enter a flow and check that a flow object is returned via EL.
     *
     * @throws Exception
     */
    @Test
    public void testFlowInjection() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(server, FLOW_APP_NAME, "index.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            LOG.info(page.asXml());

            HtmlElement enterFlowbutton = (HtmlElement) page.getElementById("simple");
            page = enterFlowbutton.click();

            logPage(page);

            assertTrue("Flow injection failed!", page.asText().contains("Current Flow via Injection: org.apache.myfaces.flow.FlowImpl@"));
        }
    }

    /**
     * Test injecting a flow when outside a flow. A WELD error should occur: WELD-000052: Cannot return null from a non-dependent producer method.
     *
     * Test looks for a 500 status code.
     *
     * @throws Exception
     */
    @Test
    public void testFlowInjectionOutsideOfFlow() throws Exception {
        try (WebClient webClient = new WebClient()) {

            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

            URL url = JSFUtils.createHttpUrl(server, FLOW_APP_NAME, "noflow.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            logPage(page);

            assertEquals("Wrong status code occurred!", 500, page.getWebResponse().getStatusCode());
        }
    }

    public void logPage(HtmlPage page) {
        if (debug) {
            LOG.info(page.asXml());
        }
    }

}
