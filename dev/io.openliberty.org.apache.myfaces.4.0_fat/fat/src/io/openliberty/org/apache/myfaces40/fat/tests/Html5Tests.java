/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.myfaces40.fat.tests;

import static org.junit.Assert.assertEquals;

import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.org.apache.myfaces40.fat.JSFUtils;

/**
 * Tests for verifying HTML 5 behavior of Faces 4.0.
 */
@RunWith(FATRunner.class)
public class Html5Tests {
    @Rule
    public TestName name = new TestName();

    String contextRoot = "HTML5Tests";

    protected static final Class<?> c = Html5Tests.class;

    @Server("faces40_html5Server")
    public static LibertyServer faces40_html5Server;

    @BeforeClass
    public static void setup() throws Exception {

        ShrinkHelper.defaultDropinApp(faces40_html5Server, "HTML5Tests.war",
                                      "io.openliberty.org.apache.faces40.fat.html5");

        faces40_html5Server.startServer(Html5Tests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (faces40_html5Server != null && faces40_html5Server.isStarted()) {
            faces40_html5Server.stopServer();
        }
    }

    /**
     * Test the 'type' attribute for &lt;link&gt; is not rendered when using outputStylesheet in HTML 5.
     *
     * @throws Exception
     */
    @Test
    public void testOutputStylesheet_Html5() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(faces40_html5Server, contextRoot, "outputStylesheetHtml5.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            DomElement linkElement = page.getElementsByTagName("link").get(0);
            assertEquals(DomElement.ATTRIBUTE_NOT_DEFINED, linkElement.getAttribute("type"));
        }
    }

    /**
     * Test the 'type' attribute for &lt;link&gt; is rendered when using outputStylesheet prior to HTML 5.
     * This test is intended to prevent HTML 5 behavior from being accidently back-ported.
     *
     * @throws Exception
     */
    @SkipForRepeat(SkipForRepeat.EE11_OR_LATER_FEATURES) //MYFACES-4681 
    @Test
    public void testOutputStylesheet_PreHtml5() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(faces40_html5Server, contextRoot, "outputStylesheet.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            DomElement linkElement = page.getElementsByTagName("link").get(0);
            assertEquals("text/css", linkElement.getAttribute("type"));
        }
    }

    /**
     * Test the 'type' attribute for &lt;script&gt; is not rendered when using outputScript in HTML 5.
     *
     * @throws Exception
     */
    @Test
    public void testOutputScript_Html5() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(faces40_html5Server, contextRoot, "outputScriptHtml5.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            DomElement linkElement = page.getElementsByTagName("script").get(0);
            assertEquals(DomElement.ATTRIBUTE_NOT_DEFINED, linkElement.getAttribute("type"));
        }
    }

    /**
     * Test the 'type' attribute for &lt;script&gt; is rendered when using outputScript prior to HTML 5.
     * This test is intended to prevent HTML 5 behavior from being accidently back-ported.
     *
     * @throws Exception
     */
    @SkipForRepeat(SkipForRepeat.EE11_OR_LATER_FEATURES) //MYFACES-4681 
    @Test
    public void testOutputScript_PreHtml5() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(faces40_html5Server, contextRoot, "outputScript.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            DomElement linkElement = page.getElementsByTagName("script").get(0);
            assertEquals("text/javascript", linkElement.getAttribute("type"));
        }
    }
}
