/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.org.apache.myfaces40.fat.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlRadioButtonInput;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlUnorderedList;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.org.apache.faces40.fat.layoutattribute.Item;
import io.openliberty.org.apache.faces40.fat.layoutattribute.ShoppingCenterBean;
import io.openliberty.org.apache.myfaces40.fat.FATSuite;

/**
 * Tests for the <h:selectManyCheckBox/> and <h:selectOneRadio/> layout="list" attribute which is a new feature for Faces 4.0.
 */
@RunWith(FATRunner.class)
public class LayoutAttributeTests {

    //Constants
    private static final Class<?> c = LayoutAttributeTests.class;
    private static final String LAYOUT_ATT_TEST_APP_NAME = "LayoutAttribute";
    private static final String SELECT_MANY_CHECKBOX_PAGE = "selectManyCheckBox.xhtml";
    private static final String SELECT_ONE_RADIO_PAGE = "selectOneRadio.xhtml";

    private static final String LAYOUT_KEY = "layout";
    private static final String BORDER_KEY = "border";

    private static boolean usingMyFaces = true;

    //Variables
    private URL testURL;
    private static List<Item> items;

    //Data structures
    private enum Forms {
        DEFAULT("testNoLayoutNoBorder=true"),
        WITH_LAYOUT("testWithLayoutNoBorder=true"),
        WITH_LAYOUT_WITH_BORDER("testWithLayoutWithBorder=true");

        public String query;

        Forms(String query) {
            this.query = query;
        }
    }

    @Server("faces40_layoutAttribute")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkHelper.buildDefaultApp(LAYOUT_ATT_TEST_APP_NAME + ".war", "io.openliberty.org.apache.faces40.fat.layoutattribute");

        // Test differences between MyFaces and Mojarra if needed
        // app.addAsLibraries(new File("publish/files/mojarra40/").listFiles());
        // app.addAsDirectories("publish/files/permissions");
        // usingMyFaces = false;

        ShrinkHelper.exportDropinAppToServer(server, app);

        ShoppingCenterBean center = new ShoppingCenterBean();
        center.populateInventory();
        items = center.getShops().stream().flatMap(shop -> shop.getItems().stream()).collect(Collectors.toList());

        server.startServer(c.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        Log.info(c, "testCleanup", "stop server");

        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Rule
    public TestName name = new TestName();

    @Test
    public void testSelectManyCheckBoxDefault() throws Exception {
        //Test default layout and border
        testURL = createURL(SELECT_MANY_CHECKBOX_PAGE, Forms.DEFAULT, null, null);
        try (WebClient webClient = new WebClient()) {
            HtmlPage page = (HtmlPage) webClient.getPage(testURL);
            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".html");

            assertCorrectLayout(page, HtmlCheckBoxInput.class, items.get(0), true, false, null);
        }
    }

    @Test
    public void testSelectManyCheckBoxLayout() throws Exception {
        //Test layout=list
        testURL = createURL(SELECT_MANY_CHECKBOX_PAGE, Forms.WITH_LAYOUT, "list", null);
        try (WebClient webClient = new WebClient()) {
            // Drive a request to the page.
            HtmlPage page = (HtmlPage) webClient.getPage(testURL);
            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".list.html");

            assertCorrectLayout(page, HtmlCheckBoxInput.class, items.get(1), false, false, null);
        }

        //test layout=pageDirection
        testURL = createURL(SELECT_MANY_CHECKBOX_PAGE, Forms.WITH_LAYOUT, "pageDirection", null);
        try (WebClient webClient = new WebClient()) {
            // Drive a request to the page.
            HtmlPage page = (HtmlPage) webClient.getPage(testURL);
            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".pageDirection.html");

            assertCorrectLayout(page, HtmlCheckBoxInput.class, items.get(2), true, false, null);
        }

        //test layout=invalidLayout is ignored and default is used
        testURL = createURL(SELECT_MANY_CHECKBOX_PAGE, Forms.WITH_LAYOUT, "invalidLayout", null);
        try (WebClient webClient = new WebClient()) {
            // Drive a request to the page.
            HtmlPage page = (HtmlPage) webClient.getPage(testURL);
            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".invalid.html");

            server.setTraceMarkToEndOfDefaultTrace();
            assertCorrectLayout(page, HtmlCheckBoxInput.class, items.get(3), true, false, null);
            if (usingMyFaces)
                assertTrue(server.findStringsInLogsAndTraceUsingMark("Wrong layout attribute for component form:input: invalidLayout").size() > 0);
        }

    }

    @Test
    public void testSelectManyCheckBoxLayoutAndBorder() throws Exception {
        //Test layout=list and border=5
        testURL = createURL(SELECT_MANY_CHECKBOX_PAGE, Forms.WITH_LAYOUT_WITH_BORDER, "list", "5");
        try (WebClient webClient = new WebClient()) {
            // Drive a request to the page.
            HtmlPage page = (HtmlPage) webClient.getPage(testURL);
            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".list.html");

            assertCorrectLayout(page, HtmlCheckBoxInput.class, items.get(4), false, false, 5);
        }

        //Test layout=pageDirection and border=10
        testURL = createURL(SELECT_MANY_CHECKBOX_PAGE, Forms.WITH_LAYOUT_WITH_BORDER, "pageDirection", "10");
        try (WebClient webClient = new WebClient()) {
            // Drive a request to the page.
            HtmlPage page = (HtmlPage) webClient.getPage(testURL);
            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".pageDirection.html");

            assertCorrectLayout(page, HtmlCheckBoxInput.class, items.get(5), true, true, 10);
        }

        //Test layout=invalidLayout and border=15
        testURL = createURL(SELECT_MANY_CHECKBOX_PAGE, Forms.WITH_LAYOUT_WITH_BORDER, "invalidLayout", "15");
        try (WebClient webClient = new WebClient()) {
            // Drive a request to the page.
            HtmlPage page = (HtmlPage) webClient.getPage(testURL);
            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".invalid.html");

            server.setTraceMarkToEndOfDefaultTrace();
            assertCorrectLayout(page, HtmlCheckBoxInput.class, items.get(6), true, true, 15);
            if (usingMyFaces)
                assertTrue(server.findStringsInLogsAndTraceUsingMark("Wrong layout attribute for component form:input: invalidLayout").size() > 0);
        }
    }

    @Test
    public void testSelectOneRadioDefault() throws Exception {
        //Test default layout and border
        testURL = createURL(SELECT_ONE_RADIO_PAGE, Forms.DEFAULT, null, null);
        try (WebClient webClient = new WebClient()) {
            // Drive a request to the page.
            HtmlPage page = (HtmlPage) webClient.getPage(testURL);
            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".html");

            assertCorrectLayout(page, HtmlRadioButtonInput.class, items.get(0), true, false, null);
        }
    }

    @Test
    public void testSelectOneRadioLayout() throws Exception {
        //Test layout=list
        testURL = createURL(SELECT_ONE_RADIO_PAGE, Forms.WITH_LAYOUT, "list", null);
        try (WebClient webClient = new WebClient()) {
            // Drive a request to the page.
            HtmlPage page = (HtmlPage) webClient.getPage(testURL);
            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".list.html");

            assertCorrectLayout(page, HtmlRadioButtonInput.class, items.get(1), false, false, null);
        }

        //test layout=pageDirection
        testURL = createURL(SELECT_ONE_RADIO_PAGE, Forms.WITH_LAYOUT, "pageDirection", null);
        try (WebClient webClient = new WebClient()) {
            // Drive a request to the page.
            HtmlPage page = (HtmlPage) webClient.getPage(testURL);
            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".pageDirection.html");

            assertCorrectLayout(page, HtmlRadioButtonInput.class, items.get(2), true, false, null);
        }

        //test layout=invalidLayout
        testURL = createURL(SELECT_ONE_RADIO_PAGE, Forms.WITH_LAYOUT, "invalidLayout", null);
        try (WebClient webClient = new WebClient()) {
            // Drive a request to the page.
            HtmlPage page = (HtmlPage) webClient.getPage(testURL);
            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".invalid.html");

            server.setTraceMarkToEndOfDefaultTrace();
            assertCorrectLayout(page, HtmlRadioButtonInput.class, items.get(3), true, false, null);
            if (usingMyFaces)
                assertTrue(server.findStringsInLogsAndTraceUsingMark("Wrong layout 'invalidLayout' defined for component").size() > 0);
        }
    }

    @Test
    public void testSelectOneRadioLayoutAndBorder() throws Exception {
        //Test layout=list and border=5
        testURL = createURL(SELECT_ONE_RADIO_PAGE, Forms.WITH_LAYOUT_WITH_BORDER, "list", "5");
        try (WebClient webClient = new WebClient()) {
            // Drive a request to the page.
            HtmlPage page = (HtmlPage) webClient.getPage(testURL);
            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".list.html");

            assertCorrectLayout(page, HtmlRadioButtonInput.class, items.get(4), false, false, 5);
        }

        //Test layout=pageDirection and border=10
        testURL = createURL(SELECT_ONE_RADIO_PAGE, Forms.WITH_LAYOUT_WITH_BORDER, "pageDirection", "10");
        try (WebClient webClient = new WebClient()) {
            // Drive a request to the page.
            HtmlPage page = (HtmlPage) webClient.getPage(testURL);
            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".pageDirection.html");

            assertCorrectLayout(page, HtmlRadioButtonInput.class, items.get(5), true, true, 10);
        }

        //Test layout=invalidLayout and border=15
        testURL = createURL(SELECT_ONE_RADIO_PAGE, Forms.WITH_LAYOUT_WITH_BORDER, "invalidLayout", "15");
        try (WebClient webClient = new WebClient()) {
            // Drive a request to the page.
            HtmlPage page = (HtmlPage) webClient.getPage(testURL);
            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".invalid.html");

            server.setTraceMarkToEndOfDefaultTrace();
            assertCorrectLayout(page, HtmlRadioButtonInput.class, items.get(6), true, true, 15);
            if (usingMyFaces)
                assertTrue(server.findStringsInLogsAndTraceUsingMark("Wrong layout 'invalidLayout' defined for component").size() > 0);
        }
    }

    /**
     * Helper method to create a URL to a specific form and page. While adding parameters for layout and border.
     *
     * @param page the xhtml page
     * @param form the name of the form
     * @param layout the value of the layout param
     * @param border the value of the border param
     * @return URL - the url to the page
     * @throws MalformedURLException - an exception is the URL cannot be created
     */
    private URL createURL(String page, Forms form, String layout, String border) throws MalformedURLException {
        String query = "?";
        query += form.query;
        query += layout == null ? "" : "&" + LAYOUT_KEY + "=" + layout;
        query += border == null ? "" : "&" + BORDER_KEY + "=" + border;
        URL result = HttpUtils.createURL(server, "/" + LAYOUT_ATT_TEST_APP_NAME + "/" + page + query);
        Log.info(c, "createURL", "Generated URL: " + result.toString());
        return result;
    }

    /**
     * Asserts the correct page layout and values based on a few variables:
     *
     * @param page - The page XML content
     * @param InputType - The type of select being tested Select or Radio
     * @param testItem - An actual item from our CDI bean to make sure is present
     * @param assertTable - True - expect a table element to be present: False - expect a ul element
     * @param assertBorder - True - expect border to be present on the select type: False - expect no border to be present
     * @param borderPixels - Assert the correct number of border pixels are present
     */
    private void assertCorrectLayout(final HtmlPage page, final Class<?> InputType, Item testItem, boolean assertTable, boolean assertBorder, Integer borderPixels) {
        assertNotNull("Should have found an element with ID: form:input", page.getElementById("form:input"));

        if (assertTable) {
            //General assertions
            assertTrue("table element should have been present", page.asXml().contains("<table"));
            assertFalse("ul element should NOT have been present", page.asXml().contains("<ul"));

            //Specific assertions input
            assertTrue("Element with ID form:input should have been a table", page.getElementById("form:input") instanceof HtmlTable);
        } else {
            //General assertions
            assertFalse("table element should NOT have been present", page.asXml().contains("<table"));
            assertTrue("ul element should have been present", page.asXml().contains("<ul"));

            //Specific assertions input
            assertTrue("Element with ID form:input should have been an unordered list", page.getElementById("form:input") instanceof HtmlUnorderedList);
        }

        if (assertBorder) {
            assertFalse("Element with ID form:input should have had a border attribute", page.getElementById("form:input").getAttribute("border").isBlank());
            assertEquals("Element with ID form:input should have had a border with value: " + borderPixels.intValue() + " instead of "
                         + page.getElementById("form:input").getAttribute("border"), borderPixels.intValue(),
                         Integer.parseInt(page.getElementById("form:input").getAttribute("border")));
        } else {
            assertTrue("Element with ID form:input should NOT have had a border attribute", page.getElementById("form:input").getAttribute("border").isEmpty());
        }

        //Specific assertions for input type
        String formId = "form:input:" + items.indexOf(testItem);
        assertNotNull("Element with ID form:input:" + formId + " should exist", page.getElementById(formId));
        assertTrue("Element with ID form:input:" + formId + " should have type " + InputType.getSimpleName(), InputType.isInstance(page.getElementById(formId)));
        assertNotNull("Element with ID form:input:" + formId + " should have a value attribute", page.getElementById(formId).getAttribute("value"));
        assertEquals("Element with ID form:input:" + formId + " should have a value of " + testItem.getId() + " but was " + page.getElementById(formId).getAttribute("value"),
                     testItem.getId(),
                     Long.parseLong(page.getElementById(formId).getAttribute("value")));
    }
}
