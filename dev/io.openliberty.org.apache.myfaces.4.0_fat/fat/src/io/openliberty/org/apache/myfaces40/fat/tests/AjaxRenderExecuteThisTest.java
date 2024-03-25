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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.org.apache.myfaces40.fat.FATSuite;

/**
 * Tests for the execute="@this" and render="@this" within a nested composite component.
 * This is a bug that was fixed for Faces 4.0, but still has an outstanding spec interpretation to be resolved.
 * More tests might need to be added depending on the outcome of issue: https://github.com/jakartaee/faces/issues/1567
 */
@RunWith(FATRunner.class)
@SkipForRepeat({SkipForRepeat.NO_MODIFICATION,SkipForRepeat.EE11_FEATURES}) // Skipped due to HTMLUnit / JavaScript incompatibility (New JS in RC5)
public class AjaxRenderExecuteThisTest {

    protected static final Class<?> clazz = AjaxRenderExecuteThisTest.class;

    private static final String APP_NAME = "AjaxRenderExecuteThisTest";

    @Server("faces40_ajaxRenderExecuteThis")
    public static LibertyServer server;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive app = ShrinkHelper.buildDefaultApp(APP_NAME + ".war", "io.openliberty.org.apache.faces40.fat.ajax.beans");

//        //Test differences between myfaces and mojarra if needed
//        app.addAsLibraries(new File("publish/files/mojarra40/").listFiles());
//        app.addAsDirectories("publish/files/permissions");

        ShrinkHelper.exportDropinAppToServer(server, app);

        server.startServer(AjaxRenderExecuteThisTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    private void fillForm(HtmlPage page, String form, String input, String value) {
        HtmlTextInput inputObj = (HtmlTextInput) page.getElementById(form + ":inputs:" + input);
        inputObj.setValueAttribute(value);
        inputObj.fireEvent("change");
        FATSuite.logOutputForDebugging(server, page.asXml(), form + "." + input + ".html");
    }

    private void fillInputs(HtmlPage page, String form, List<HtmlTextInput> addressAttributes) {
        addressAttributes.add(0, (HtmlTextInput) page.getElementById(form + ":inputs:street"));
        addressAttributes.add(1, (HtmlTextInput) page.getElementById(form + ":inputs:city"));
        addressAttributes.add(2, (HtmlTextInput) page.getElementById(form + ":inputs:state"));
    }

    private void replaceAllMessages(List<String> messageList, String... newMessages) {
        messageList.clear();
        messageList.addAll(List.of(newMessages));
    }

    private void assertListEqual(List<String> expectedMessages, List<String> actualMessages) {
        for (String expectedMessage : expectedMessages) {
            try {
                assertNotNull(actualMessages.remove(expectedMessage));
            } catch (AssertionError ae) {
                throw new AssertionError("Expected message " + expectedMessage + " was was not in list of actual messages");
            }
        }

        try {
            assertTrue(actualMessages.isEmpty());
        } catch (AssertionError ae) {
            throw new AssertionError("Actual message(s) [" + String.join(",", actualMessages) + "] where not expected.");
        }
    }

    @Test
    public void testAjaxRender() throws Exception {
        try (WebClient webClient = new WebClient()) {
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());

            URL url = HttpUtils.createURL(server, "/" + APP_NAME + "/ajaxRenderExecuteThis.xhtml");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            //Address attributes: street, city, state
            List<HtmlTextInput> addressAttributes = new ArrayList<>(3);
            List<String> actualMessages = new ArrayList<>();
            List<String> expectedMessages = new ArrayList<>();

            //Test attributes
            String form = "testRender";

            // Test street
            fillForm(page, form, "street", "aberdeen way");
            fillInputs(page, form, addressAttributes);

            replaceAllMessages(actualMessages, page.getElementById(form + ":messages").asText().split(System.lineSeparator()));
            replaceAllMessages(expectedMessages, "setTestRenderStreet:aberdeen way", "setTestRenderCity:");

            assertEquals("street contained unexpected result", "aberdeen way", addressAttributes.get(0).getValueAttribute());
            assertEquals("city contained unexpected result", "", addressAttributes.get(1).getValueAttribute());
            assertEquals("state contained unexpected result", "", addressAttributes.get(2).getValueAttribute());
            assertListEqual(expectedMessages, actualMessages);

            // Test city
            fillForm(page, form, "city", "big lake");
            fillInputs(page, form, addressAttributes);

            replaceAllMessages(actualMessages, page.getElementById(form + ":messages").asText().split(System.lineSeparator()));
            replaceAllMessages(expectedMessages, "setTestRenderStreet:aberdeen way", "setTestRenderCity:big lake");

            assertEquals("street contained unexpected result", "aberdeen way", addressAttributes.get(0).getValueAttribute());
            assertEquals("city contained unexpected result", "big lake", addressAttributes.get(1).getValueAttribute());
            assertEquals("state contained unexpected result", "", addressAttributes.get(2).getValueAttribute());
            assertListEqual(expectedMessages, actualMessages);

            // Test state
            fillForm(page, form, "state", "minnesota");
            fillInputs(page, form, addressAttributes);

            replaceAllMessages(actualMessages, page.getElementById(form + ":messages").asText().split(System.lineSeparator()));
            replaceAllMessages(expectedMessages, "setTestRenderState:minnesota");

            assertEquals("street contained unexpected result", "", addressAttributes.get(0).getValueAttribute());
            assertEquals("city contained unexpected result", "", addressAttributes.get(1).getValueAttribute());
            assertEquals("state contained unexpected result", "minnesota^", addressAttributes.get(2).getValueAttribute());
            assertListEqual(expectedMessages, actualMessages);
        }
    }

    @Test
    public void testAjaxExecuteThis() throws Exception {
        try (WebClient webClient = new WebClient()) {
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());

            URL url = HttpUtils.createURL(server, "/" + APP_NAME + "/ajaxRenderExecuteThis.xhtml");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Inputs / Outputs
            List<HtmlTextInput> inputs = new ArrayList<>(3);
            List<String> actualMessages = new ArrayList<>();
            List<String> expectedMessages = new ArrayList<>();

            //Test attributes
            String form = "testExecuteThis";

            // Test street
            fillForm(page, form, "street", "w 4th street");
            fillInputs(page, form, inputs);

            replaceAllMessages(actualMessages, page.getElementById(form + ":messages").asText().split(System.lineSeparator()));
            replaceAllMessages(expectedMessages, "setTestExecuteThisStreet:w 4th street", "setTestExecuteThisCity:");

            assertEquals("street contained unexpected result", "w 4th street", inputs.get(0).getValueAttribute());
            assertEquals("city contained unexpected result", "", inputs.get(1).getValueAttribute());
            assertEquals("state contained unexpected result", "", inputs.get(2).getValueAttribute());
            assertListEqual(expectedMessages, actualMessages);

            // Test city
            fillForm(page, form, "city", "red wing");
            fillInputs(page, form, inputs);

            replaceAllMessages(actualMessages, page.getElementById(form + ":messages").asText().split(System.lineSeparator()));
            replaceAllMessages(expectedMessages, "setTestExecuteThisStreet:w 4th street", "setTestExecuteThisCity:red wing");

            assertEquals("street contained unexpected result", "w 4th street", inputs.get(0).getValueAttribute());
            assertEquals("city contained unexpected result", "red wing", inputs.get(1).getValueAttribute());
            assertEquals("state contained unexpected result", "", inputs.get(2).getValueAttribute());
            assertListEqual(expectedMessages, actualMessages);

            // Test state
            fillForm(page, form, "state", "minnesota");
            fillInputs(page, form, inputs);

            replaceAllMessages(actualMessages, page.getElementById(form + ":messages").asText().split(System.lineSeparator()));
            replaceAllMessages(expectedMessages, "setTestExecuteThisState:minnesota");

            assertEquals("street contained unexpected result", "", inputs.get(0).getValueAttribute());
            assertEquals("city contained unexpected result", "", inputs.get(1).getValueAttribute());
            assertEquals("state contained unexpected result", "minnesota^", inputs.get(2).getValueAttribute());
            assertListEqual(expectedMessages, actualMessages);
        }

    }

    @Test
    public void testAjaxRenderThis() throws Exception {

        try (WebClient webClient = new WebClient()) {
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());

            URL url = HttpUtils.createURL(server, "/" + APP_NAME + "/ajaxRenderExecuteThis.xhtml");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Inputs / Outputs
            List<HtmlTextInput> inputs = new ArrayList<>(3);
            List<String> actualMessages = new ArrayList<>();
            List<String> expectedMessages = new ArrayList<>();

            //Test attributes
            String form = "testRenderThis";

            // Test street
            fillForm(page, form, "street", "hilbert ave");
            fillInputs(page, form, inputs);

            replaceAllMessages(actualMessages, page.getElementById(form + ":messages").asText().split(System.lineSeparator()));
            replaceAllMessages(expectedMessages, "setTestRenderThisStreet:hilbert ave", "setTestRenderThisCity:");

            assertEquals("street contained unexpected result", "hilbert ave^", inputs.get(0).getValueAttribute());
            assertEquals("city contained unexpected result", "^", inputs.get(1).getValueAttribute());
            assertEquals("state contained unexpected result", "", inputs.get(2).getValueAttribute());
            assertListEqual(expectedMessages, actualMessages);

            // Test city
            fillForm(page, form, "city", "winona");
            fillInputs(page, form, inputs);

            replaceAllMessages(actualMessages, page.getElementById(form + ":messages").asText().split(System.lineSeparator()));
            replaceAllMessages(expectedMessages, "setTestRenderThisStreet:hilbert ave^", "setTestRenderThisCity:winona");

            assertEquals("street contained unexpected result", "hilbert ave^^", inputs.get(0).getValueAttribute());
            assertEquals("city contained unexpected result", "winona^", inputs.get(1).getValueAttribute());
            assertEquals("state contained unexpected result", "", inputs.get(2).getValueAttribute());
            assertListEqual(expectedMessages, actualMessages);

            // Test state
            fillForm(page, form, "state", "minnesota");
            fillInputs(page, form, inputs);

            replaceAllMessages(actualMessages, page.getElementById(form + ":messages").asText().split(System.lineSeparator()));
            replaceAllMessages(expectedMessages, "setTestRenderThisState:minnesota");

            assertEquals("street contained unexpected result", "", inputs.get(0).getValueAttribute());
            assertEquals("city contained unexpected result", "", inputs.get(1).getValueAttribute());
            assertEquals("state contained unexpected result", "minnesota^", inputs.get(2).getValueAttribute());
            assertListEqual(expectedMessages, actualMessages);
        }
    }
}
