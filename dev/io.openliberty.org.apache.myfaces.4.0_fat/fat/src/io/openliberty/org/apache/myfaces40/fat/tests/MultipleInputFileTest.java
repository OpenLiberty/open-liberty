/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.xml.sax.helpers.AttributesImpl;

import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlFileInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.parser.neko.HtmlUnitNekoHtmlParser;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.org.apache.myfaces40.fat.FATSuite;

/**
 * Tests for the <h:inputFile/> with multiple attribute which is a new feature for Faces 4.0.
 *
 * <pre>
 * Testing matrix:
 *
 * | Multiple Attribute | Async Javascript | Number of files |
 * | ================== | ================ | =============== |
 * |      UNSET         |      false       |      single     |
 * |      UNSET         |      false       |     multiple    |
 * |    "multiple"      |      false       |      single     |
 * |    "multiple"      |      false       |     multiple    |
 * |      UNSET         |      true        |      single     |
 * |      UNSET         |      true        |     multiple    |
 * |    "multiple"      |      true        |      single     |
 * |    "multiple"      |      true        |     multiple    |
 *
 * </pre>
 */
@RunWith(FATRunner.class)
public class MultipleInputFileTest {

    protected static final Class<?> c = MultipleInputFileTest.class;
    private static final String APP_NAME = "MultipleInputFileTest";

    private static enum Form {
        NO_MULTIPLE_NO_AJAX("testNoMultipleNoAjax"),
        WITH_MULTIPLE_NO_AJAX("testWithMultipleNoAjax"),
        NO_MULTIPLE_WITH_AJAX("testNoMultipleWithAjax"),
        WITH_MULTIPLE_WITH_AJAX("testWithMultipleWithAjax");

        String testParam;

        Form(String testParam) {
            this.testParam = testParam + "=true";
        }
    }

    private static enum Field {
        SINGLE("singleSelection"),
        MULTIPLE("multipleSelection");

        String name;

        Field(String name) {
            this.name = name;
        }
    }

    @Server("faces40_multipleInputFileTest")
    public static LibertyServer server;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "io.openliberty.org.apache.faces40.fat.inputfile.beans");
        server.startServer(c.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testNoMultiple_NoAjax_SingleFile() throws Exception {
        URL url = HttpUtils.createURL(server, "/" + APP_NAME + "/MutipleInputFile.xhtml" + "?" + Form.NO_MULTIPLE_NO_AJAX.testParam);

        try (WebClient webClient = new WebClient()) {
            // Drive a request to the page.
            HtmlPage page = (HtmlPage) webClient.getPage(url);
            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".html");

            assertSingleSelection(page, Field.SINGLE);
        }
    }

    @Test
    public void testNoMultiple_NoAjax_MultipleFiles() throws Exception {
        URL url = HttpUtils.createURL(server, "/" + APP_NAME + "/MutipleInputFile.xhtml" + "?" + Form.NO_MULTIPLE_NO_AJAX.testParam);

        try (WebClient webClient = new WebClient()) {
            // Drive a request to the page.
            HtmlPage page = (HtmlPage) webClient.getPage(url);
            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".html");

            assertMultipleSelectionProducesSingleResult(page);
        }
    }

    @Test
    public void testWithMultiple_NoAjax_SingleFile() throws Exception {
        URL url = HttpUtils.createURL(server, "/" + APP_NAME + "/MutipleInputFile.xhtml" + "?" + Form.WITH_MULTIPLE_NO_AJAX.testParam);

        try (WebClient webClient = new WebClient()) {
            // Drive a request to the page.
            HtmlPage page = (HtmlPage) webClient.getPage(url);
            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".html");

            assertSingleSelection(page, Field.MULTIPLE);
        }
    }

    @Test
    public void testWithMultiple_NoAjax_MultipleFiles() throws Exception {
        URL url = HttpUtils.createURL(server, "/" + APP_NAME + "/MutipleInputFile.xhtml" + "?" + Form.WITH_MULTIPLE_NO_AJAX.testParam);

        try (WebClient webClient = new WebClient()) {
            // Drive a request to the page.
            HtmlPage page = (HtmlPage) webClient.getPage(url);
            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".html");

            assertMultipleSelection(page);
        }
    }

    @Test
    @Mode(FULL)
    @SkipForRepeat({SkipForRepeat.NO_MODIFICATION,SkipForRepeat.EE11_FEATURES}) // Skipped due to HTMLUnit / JavaScript incompatibility (New JS in RC5)
    public void testNoMultiple_WithAjax_SingleFile() throws Exception {
        URL url = HttpUtils.createURL(server, "/" + APP_NAME + "/MutipleInputFile.xhtml" + "?" + Form.NO_MULTIPLE_WITH_AJAX.testParam);

        try (WebClient webClient = new WebClient()) {
            // Drive a request to the page.
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());
            HtmlPage page = (HtmlPage) webClient.getPage(url);
            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".html");

            assertSingleSelection(page, Field.SINGLE);
        }
    }

    @Test
    @Mode(FULL)
    @SkipForRepeat({SkipForRepeat.NO_MODIFICATION,SkipForRepeat.EE11_FEATURES}) // Skipped due to HTMLUnit / JavaScript incompatibility (New JS in RC5)
    public void testNoMultiple_WithAjax_MultipleFiles() throws Exception {
        URL url = HttpUtils.createURL(server, "/" + APP_NAME + "/MutipleInputFile.xhtml" + "?" + Form.NO_MULTIPLE_WITH_AJAX.testParam);

        try (WebClient webClient = new WebClient()) {
            // Drive a request to the page.
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());
            HtmlPage page = (HtmlPage) webClient.getPage(url);
            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".html");

            assertMultipleSelectionProducesSingleResult(page);
        }
    }

    @Test
    @Mode(FULL)
    @SkipForRepeat({SkipForRepeat.NO_MODIFICATION,SkipForRepeat.EE11_FEATURES}) // Skipped due to HTMLUnit / JavaScript incompatibility (New JS in RC5)
    public void testWithMultiple_WithAjax_SingleFile() throws Exception {
        URL url = HttpUtils.createURL(server, "/" + APP_NAME + "/MutipleInputFile.xhtml" + "?" + Form.WITH_MULTIPLE_WITH_AJAX.testParam);

        try (WebClient webClient = new WebClient()) {
            // Drive a request to the page.
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());
            HtmlPage page = (HtmlPage) webClient.getPage(url);
            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".html");

            assertSingleSelection(page, Field.MULTIPLE);
        }
    }

    @Test
    @Mode(FULL)
    @SkipForRepeat({SkipForRepeat.NO_MODIFICATION,SkipForRepeat.EE11_FEATURES}) // Skipped due to HTMLUnit / JavaScript incompatibility (New JS in RC5)
    public void testWithMultiple_WithAjax_MultipleFiles() throws Exception {
        URL url = HttpUtils.createURL(server, "/" + APP_NAME + "/MutipleInputFile.xhtml" + "?" + Form.WITH_MULTIPLE_WITH_AJAX.testParam);

        try (WebClient webClient = new WebClient()) {
            // Drive a request to the page.
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());
            HtmlPage page = (HtmlPage) webClient.getPage(url);
            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".html");

            assertMultipleSelection(page);
        }
    }

    /**
     * Asserts that a single file selection results in the correct messages output.
     * This test will assert the correct result for both single, and multiple form inputs.
     *
     * @param page - The html page
     * @param field - is the form input using single, or multiple inputs
     * @throws Exception - Thrown if we were not able to execute the assertion testing
     */
    private void assertSingleSelection(HtmlPage page, Field field) throws Exception {
        //Assert the correct multiple attribute is set
        HtmlFileInput input = page.getHtmlElementById("form:input");
        if (field == Field.MULTIPLE) {
            assertEquals("Multiple attribute is set", "multiple", input.getAttribute("multiple"));
        } else {
            assertEquals("Multiple attribute should NOT be set", "", input.getAttribute("multiple"));
        }

        //Create a single file and add it to the input attribute
        File file = generateTempFile("file", "bin", 123);
        input.setValueAttribute(file.getAbsolutePath());

        //Click submit
        page = page.getHtmlElementById("form:submit").click();
        FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".clicked.html");

        //Assert expected messages are present
        HtmlElement messages = page.getHtmlElementById("messages");
        assertEquals("There is 1 message", 1, messages.getChildElementCount());

        Iterator<DomElement> iterator = messages.getChildElements().iterator();
        assertEquals("Uploaded file has been received", "field: " + field.name + ", name: " + file.getName() + ", size: " + file.length(), iterator.next().asText());
        assertFalse("Iterator should not have had another element", iterator.hasNext());
    }

    /**
     * Asserts that a multiple file selection results in the correct messages output.
     *
     * @param page - The html page
     * @throws Exception - Thrown if we were not able to execute the assertion testing
     */
    private void assertMultipleSelection(HtmlPage page) throws Exception {
        //Get the file input element
        HtmlFileInput input = page.getHtmlElementById("form:input");
        assertEquals("Multiple attribute is set", "multiple", input.getAttribute("multiple"));

        //Create files and add them to the input attribute
        File file1 = generateTempFile("file1", "bin", 123);
        File file2 = generateTempFile("file2", "bin", 234);
        File file3 = generateTempFile("file3", "bin", 345);
        input.setValueAttribute(file1.getAbsolutePath());
        addValueAttribute(input, file2.getAbsolutePath());
        addValueAttribute(input, file3.getAbsolutePath());

        //Click submit
        page = page.getHtmlElementById("form:submit").click();
        FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".clicked.html");

        //Assert messages are expected
        HtmlElement messages = page.getHtmlElementById("messages");
        assertEquals("There are 3 messages", 3, messages.getChildElementCount());

        Iterator<DomElement> iterator = messages.getChildElements().iterator();
        assertEquals("First uploaded file has been received", "field: multipleSelection, name: " + file1.getName() + ", size: " + file1.length(),
                     iterator.next().asText());
        assertEquals("Second uploaded file has been received", "field: multipleSelection, name: " + file2.getName() + ", size: " + file2.length(),
                     iterator.next().asText());
        assertEquals("Third uploaded file has been received", "field: multipleSelection, name: " + file3.getName() + ", size: " + file3.length(),
                     iterator.next().asText());
        assertFalse("Iterator should not have had another element", iterator.hasNext());
    }

    /**
     * Asserts that a multiple file selection results in a single output.
     *
     * @param page - The html page
     * @throws IOException
     */
    private void assertMultipleSelectionProducesSingleResult(HtmlPage page) throws IOException {
        //Get the file input element
        HtmlFileInput input = page.getHtmlElementById("form:input");
        assertEquals("Multiple attribute should NOT be set", "", input.getAttribute("multiple"));

        //Create files and add them to the input attribute
        File file1 = generateTempFile("file1", "bin", 123);
        File file2 = generateTempFile("file2", "bin", 234);
        File file3 = generateTempFile("file3", "bin", 345);
        input.setValueAttribute(file1.getAbsolutePath());
        addValueAttribute(input, file2.getAbsolutePath());
        addValueAttribute(input, file3.getAbsolutePath());

        //Click submit
        page = page.getHtmlElementById("form:submit").click();
        FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".clicked.html");

        //Assert messages are expected
        HtmlElement messages = page.getHtmlElementById("messages");
        assertEquals("There is 1 message", 1, messages.getChildElementCount());

        Iterator<DomElement> iterator = messages.getChildElements().iterator();
        assertEquals("Uploaded file has been received", "field: singleSelection, name: " + file1.getName() + ", size: " + file1.length(), iterator.next().asText());
        assertFalse("Iterator should not have had another element", iterator.hasNext());
    }

    /**
     * Generates a temporary file and returns the file object
     */
    private static File generateTempFile(String name, String ext, int size) throws IOException {
        Path path = Files.createTempFile(name, "." + ext);
        byte[] content = new byte[size];
        Files.write(path, content, StandardOpenOption.APPEND);
        return path.toFile();
    }

    /**
     * HtmlUnit's HtmlFileInput doesn't support submitting multiple values.
     * The below is a work-around, found on https://stackoverflow.com/a/19654060
     */
    private static void addValueAttribute(HtmlFileInput input, String valueAttribute) {
        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute(null, null, "type", null, "file");
        attributes.addAttribute(null, null, "name", null, input.getNameAttribute());
        HtmlFileInput cloned = (HtmlFileInput) new HtmlUnitNekoHtmlParser().getFactory("input").createElementNS(input.getPage(), null, "input", attributes);
        input.getParentNode().appendChild(cloned);
        cloned.setValueAttribute(valueAttribute);
    }
}
