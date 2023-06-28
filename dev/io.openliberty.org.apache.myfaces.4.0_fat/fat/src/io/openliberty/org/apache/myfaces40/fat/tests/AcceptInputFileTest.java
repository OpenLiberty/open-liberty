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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.xml.sax.helpers.AttributesImpl;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlFileInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.parser.neko.HtmlUnitNekoHtmlParser;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.org.apache.myfaces40.fat.FATSuite;

/**
 * Tests for the <h:inputFile/> with accept attribute which is a new feature for Faces 4.0.
 *
 * Note: The accept attribute for input elements is processed by a client browser and limits selected files.
 * The htmlunit's WebClient does not replicate this part of the client browser behavior.
 * Additionally, the specification notes that the server should not perform any validation.
 * Therefore, these functional tests only assert that the attribute from the xhtml file is passed through and
 * generated on the resulting HTML response page.
 */
@RunWith(FATRunner.class)
public class AcceptInputFileTest {

    protected static final Class<?> c = AcceptInputFileTest.class;
    private static final String APP_NAME = "AcceptInputFileTest";

    @Server("faces40_acceptInputFileTest")
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
    public void testCaseInsensitiveExtension() throws Exception {
        assertFileExtension(".pdf", ".pdf", 1);
        assertFileExtension(".JPEG", ".jpeg", 3);
    }

    @Test
    public void testMIMEExtension() throws Exception {
        assertFileExtension("audio/*", ".mp3", 1);
        if (TestModeFilter.FRAMEWORK_TEST_MODE == TestMode.FULL) {
            assertFileExtension("image/*", ".raw", 3);
        }
    }

    @Test
    public void testMultipleCaseInsensitiveExtentions() throws Exception {
        assertFileExtension(".png,.TIFF", ".png", 1);
        if (TestModeFilter.FRAMEWORK_TEST_MODE == TestMode.FULL) {
            assertFileExtension(".JPG,.bmp", ".jpg", 3);
        }
    }

    @Test
    @Mode(TestMode.FULL)
    public void testMixedExtentions() throws Exception {
        assertFileExtension("image/*,.pdf", ".pdf", 1);
        assertFileExtension(".gif,video/MP4", ".gif", 3);
    }

    @Test
    @Mode(TestMode.FULL)
    public void testPurePassthroughExtentions() throws Exception {
        assertFileExtension("something.IMadeUp", ".tmp", 1);
        assertFileExtension("something.ElseIMadeUp", ".tmp", 3);
    }

    /**
     * Asserts that the accept attribute is passed through faces and is rendered on the page
     * without any validation or modifications
     *
     * @param accepting - the value of the accept attribute
     * @param fileExt - the extension of the generated file used for testing
     * @param files - the number of files to generate
     * @throws Exception - if any part of the test framework fails.
     */
    private void assertFileExtension(String accepting, String fileExt, final int files) throws Exception {

        URL url = HttpUtils.createURL(server, "/" + APP_NAME + "/AcceptInputFile.xhtml" + //
                                              "?accepting=" + accepting + //
                                              (files > 1 ? "&withMultiple=true" : ""));
        String fieldName = files > 1 ? "multipleSelection" : "singleSelection";

        try (WebClient webClient = new WebClient()) {
            // Drive a request to the page.
            HtmlPage page = (HtmlPage) webClient.getPage(url);
            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".html");

            //Assert the correct accept attribute is set
            HtmlFileInput input = page.getHtmlElementById("form:input");
            assertEquals("Unexpected value for the accept attribute.", accepting, input.getAcceptAttribute());

            //Create file(s) and add it to the input attribute
            List<String> expectedMessages = new ArrayList<>();
            Random rand = new Random();
            IntStream.range(0, files).forEach(i -> {
                int size = rand.nextInt(999) + 1;
                try {
                    File file = generateTempFile("file", fileExt, size);
                    String expectedMessage = "field: " + fieldName + ", name: " + file.getName() + //
                                             ", size: " + size + ", extension: " + //
                                             (fileExt == null ? "NONE" : fileExt);
                    expectedMessages.add(expectedMessage);
                    if (i == 0) {
                        input.setValueAttribute(file.getAbsolutePath());
                    } else {
                        addValueAttribute(input, file.getAbsolutePath());
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            //Click submit
            page = page.getHtmlElementById("form:submit").click();
            HtmlElement messages = page.getHtmlElementById("messages");

            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".clicked.html");
            Iterator<DomElement> iterator = messages.getChildElements().iterator();
            while (iterator.hasNext()) {
                String expected = iterator.next().asText();
                assertTrue("Should have found the expected message in the list", expectedMessages.remove(expected));
            }

            if (!expectedMessages.isEmpty()) {
                String failMessage = "All messages should have been returned, instead got: " + expectedMessages.toString();
                fail(failMessage);
            }
        }
    }

    /**
     * Generates a temporary file and returns the file object
     */
    private static File generateTempFile(String name, String ext, int size) throws IOException {
        Path path = Files.createTempFile(name, ext);
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
