/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

    private void fillForm(HtmlPage page, String form, String input) {
        HtmlTextInput inputObj = (HtmlTextInput) page.getElementById(form + ":inputs:" + input);
        inputObj.setValueAttribute("1");
        inputObj.fireEvent("change");
        FATSuite.logOutputForDebugging(server, page.asXml(), form + input + ".html");
    }

    private void fillInputs(HtmlPage page, String form, List<HtmlTextInput> inputs) {
        inputs.add(0, (HtmlTextInput) page.getElementById(form + ":inputs:input1"));
        inputs.add(1, (HtmlTextInput) page.getElementById(form + ":inputs:input2"));
        inputs.add(2, (HtmlTextInput) page.getElementById(form + ":inputs:input3"));
    }

    @Test
    public void testAjaxRenderAndExecute() throws Exception {

        try (WebClient webClient = new WebClient()) {
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());

            URL url = HttpUtils.createURL(server, "/" + APP_NAME + "/ajaxRenderExecuteThis.xhtml");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            List<HtmlTextInput> inputs = new ArrayList<>(3);
            String actualMessage, expectedMessage;

            // fill form1 input1
            fillForm(page, "form1", "input1");
            fillInputs(page, "form1", inputs);
            actualMessage = page.getElementById("form1:messages").asText();
            expectedMessage = "setForm1input1:1\nsetForm1input2:";
            assertEquals("input1 is filled with 1", "1", inputs.get(0).getValueAttribute());
            assertEquals("input2 is empty", "", inputs.get(1).getValueAttribute());
            assertEquals("input3 is empty", "", inputs.get(2).getValueAttribute());
            assertEquals("input1 is filled and input2 is empty", expectedMessage, actualMessage);

            // fill form1 input2
            fillForm(page, "form1", "input2");
            fillInputs(page, "form1", inputs);
            actualMessage = page.getElementById("form1:messages").asText();
            expectedMessage = "setForm1input1:1\nsetForm1input2:1";
            assertEquals("input1 is filled with 1", "1", inputs.get(0).getValueAttribute());
            assertEquals("input2 is filled with 1", "1", inputs.get(1).getValueAttribute());
            assertEquals("input3 is empty", "", inputs.get(2).getValueAttribute());
            assertEquals("input1 is filled and input2 is filled", expectedMessage, actualMessage);

            // fill form1 input3
            fillForm(page, "form1", "input3");
            fillInputs(page, "form1", inputs);
            actualMessage = page.getElementById("form1:messages").asText();
            expectedMessage = "setForm1input3:1";
            assertEquals("input1 is refreshed to empty string", "", inputs.get(0).getValueAttribute());
            assertEquals("input2 is refreshed to empty string", "", inputs.get(1).getValueAttribute());
            assertEquals("input3 is filled and refreshed", "1x", inputs.get(2).getValueAttribute());
            assertEquals("input3 is filled", expectedMessage, actualMessage);

            // fill form2 input1
            fillForm(page, "form2", "input1");
            fillInputs(page, "form2", inputs);
            actualMessage = page.getElementById("form2:messages").asText();
            expectedMessage = "setForm2input1:1\nsetForm2input2:";
            assertEquals("input1 is filled with 1", "1", inputs.get(0).getValueAttribute());
            assertEquals("input2 is empty", "", inputs.get(1).getValueAttribute());
            assertEquals("input3 is empty", "", inputs.get(2).getValueAttribute());
            assertEquals("input1 is filled and input2 is empty", expectedMessage, actualMessage);

            // fill form2 input2
            fillForm(page, "form2", "input2");
            fillInputs(page, "form2", inputs);
            actualMessage = page.getElementById("form2:messages").asText();
            expectedMessage = "setForm2input1:1\nsetForm2input2:1";
            assertEquals("input1 is filled with 1", "1", inputs.get(0).getValueAttribute());
            assertEquals("input2 is filled with 1", "1", inputs.get(1).getValueAttribute());
            assertEquals("input3 is empty", "", inputs.get(2).getValueAttribute());
            assertEquals("input1 is filled and input2 is filled", expectedMessage, actualMessage);

            // fill form2 input3
            fillForm(page, "form2", "input3");
            fillInputs(page, "form2", inputs);
            actualMessage = page.getElementById("form2:messages").asText();
            expectedMessage = "setForm2input3:1";
            assertEquals("input1 is refreshed to empty string", "", inputs.get(0).getValueAttribute());
            assertEquals("input2 is refreshed to empty string", "", inputs.get(1).getValueAttribute());
            assertEquals("input3 is filled and refreshed", "1x", inputs.get(2).getValueAttribute());
            assertEquals("input3 is filled", expectedMessage, actualMessage);

            // fill form3 input1
            fillForm(page, "form3", "input1");
            fillInputs(page, "form3", inputs);
            actualMessage = page.getElementById("form3:messages").asText();
            expectedMessage = "setForm3input1:1\nsetForm3input2:";
            assertEquals("input1 is refreshed to 1x", "1x", inputs.get(0).getValueAttribute());
            assertEquals("input2 is refreshed to x", "x", inputs.get(1).getValueAttribute());
            assertEquals("input3 is empty", "", inputs.get(2).getValueAttribute());
            assertEquals("input1 is filled and input2 is empty", expectedMessage, actualMessage);

            // fill form3 input2
            fillForm(page, "form3", "input2");
            fillInputs(page, "form3", inputs);
            actualMessage = page.getElementById("form3:messages").asText();
            expectedMessage = "setForm3input1:1x\nsetForm3input2:1";
            assertEquals("input1 is refreshed to 1xx", "1xx", inputs.get(0).getValueAttribute());
            assertEquals("input2 is refreshed to 1x", "1x", inputs.get(1).getValueAttribute());
            assertEquals("input3 is empty", "", inputs.get(2).getValueAttribute());
            assertEquals("input1 is filled and input2 is filled", expectedMessage, actualMessage);

            // fill form3 input3
            fillForm(page, "form3", "input3");
            fillInputs(page, "form3", inputs);
            actualMessage = page.getElementById("form3:messages").asText();
            expectedMessage = "setForm3input3:1";
            assertEquals("input1 is refreshed to empty string", "", inputs.get(0).getValueAttribute());
            assertEquals("input2 is refreshed to empty string", "", inputs.get(1).getValueAttribute());
            assertEquals("input3 is filled and refreshed", "1x", inputs.get(2).getValueAttribute());
            assertEquals("input3 is filled", expectedMessage, actualMessage);
        }
    }
}
