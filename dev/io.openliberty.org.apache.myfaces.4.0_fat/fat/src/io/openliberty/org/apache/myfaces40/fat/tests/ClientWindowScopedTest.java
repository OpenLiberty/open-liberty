/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.myfaces40.fat.tests;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.org.apache.myfaces40.fat.FATSuite;

/**
 * Test the new ClientWindowScoped context for Faces-4.0
 * Full test because scanning logs is slow.
 */
@Mode(FULL)
@RunWith(FATRunner.class)
public class ClientWindowScopedTest {

    private static final String APP_NAME = "ClientWindowScopedTest";

    @Server("faces40_clientWindowScopedTest")
    public static LibertyServer server;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "io.openliberty.org.apache.faces40.fat.client.window");

        server.startServer(ClientWindowScopedTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Ensure the same client window is used when navigating between pages in a flow.
     */
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION) // Skipped due to HTMLUnit / JavaScript Incompatabilty (New JS in RC5)
    @Test
    public void testClientWindowReuseInFlow() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = HttpUtils.createURL(server, "/" + APP_NAME + "/clientWindowHierarchy.xhtml");
            HtmlPage page = (HtmlPage) webClient.getPage(url);
            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".html");

            page = page.getHtmlElementById("form:enter").click();
            String flow1Result = page.getHtmlElementById("result1").asText();
            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".enter.html");

            page = page.getHtmlElementById("form:next").click();
            String flow2Result = page.getHtmlElementById("result2").asText();
            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".next.html");

            page = page.getHtmlElementById("form:exit").click();
            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".exit.html");

            assertEquals("Expected FlowScoped beans to be under the same ClientWindowScope", flow1Result, flow2Result);
        }
    }

    /**
     * Ensure a new client window is used when refreshing view.
     */
    @Test
    public void testNewClientWindowSameView() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = HttpUtils.createURL(server, "/" + APP_NAME + "/clientWindowCount.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);
            String action1 = page.getElementById("form").getAttribute("action");
            String jfwid1 = action1.substring(action1.indexOf("jfwid="));
            String id1 = page.getElementById("id").asText();
            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".html");

            page = (HtmlPage) page.refresh();
            String action2 = page.getElementById("form").getAttribute("action");
            String jfwid2 = action2.substring(action1.indexOf("jfwid="));
            String id2 = page.getElementById("id").asText();
            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".refreshed.html");

            assertFalse("Expected a NEW client window on view update (refresh): jfwid=" + jfwid1, jfwid1.equals(jfwid2));
            assertFalse("Expected a NEW client window object to be created (no reuse): id=" + id1, id1.equals(id2));
        }
    }

    /**
     * Ensure that the jakarta.faces.NUMBER_OF_CLIENT_WINDOWS configuration is honored
     * and only two concurrent client window objects are allowed.
     */
    @Test
    public void testNumberOfClientWindows() throws Exception {
        try (WebClient webClient = new WebClient()) {
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());
            URL url = HttpUtils.createURL(server, "/" + APP_NAME + "/clientWindowCount.xhtml");

            //The log we will look for created/destroyed beans
            RemoteFile consoleLogFile = server.getConsoleLogFile();

            //Keep track of the previous id to assert that they get removed
            String initID, removeID;
            List<String> previousIDs = new ArrayList<>();

            //Now we should see newly created client window objects destroyed as they are added.
            List<String> windows = Arrays.asList("window1", "window2", "window3", "window4", "window5");
            for (String key : windows) {
                try {
                    server.setMarkToEndOfLog(consoleLogFile);
                    webClient.openWindow(url, key);
                    List<String> initLines = server.findStringsInLogsUsingMark("FACE40CWO:INIT", consoleLogFile);
                    assertEquals("Expected a single client window object to be created", 1, initLines.size());
                    initID = initLines.get(0).substring(initLines.get(0).lastIndexOf(":"));

                    if (previousIDs.size() == 2) {
                        List<String> removeLines = server.findStringsInLogsUsingMark("FACE40CWO:REMOVE:", consoleLogFile);
                        assertEquals("Expected a single client window object to be removed", 1, removeLines.size());
                        removeID = removeLines.get(0).substring(removeLines.get(0).lastIndexOf(":"));

                        assertTrue("Expected the client window object " + removeID + " to be in the list of previous IDs: " + previousIDs,
                                   previousIDs.remove(removeID));
                    }

                    previousIDs.add(initID);

                } catch (Exception e) {
                    throw new AssertionError("Unable to perform test: " + name.getMethodName() + ", due to exception: " + e.getLocalizedMessage());
                }
            }
        }
    }
}
