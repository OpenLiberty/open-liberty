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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.org.apache.myfaces41.fat.JSFUtils;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.ShrinkHelper;


@RunWith(FATRunner.class)
public class UUIDConverterTest {

    private static final String APP_NAME = "uuidConverter_Spec1819";
    protected static final Class<?> c = UUIDConverterTest.class;

    private static final Logger LOG = Logger.getLogger(UUIDConverterTest.class.getName());

    @Rule
    public TestName name = new TestName();

    @Server("faces41_uuidServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "io.openliberty.org.apache.myfaces41.fat.uuidconverter.beans");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(UUIDConverterTest.class.getSimpleName() + ".log");

    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            // SRVE0777E: Exception thrown by application class  - ClassCastException
            // SRVE0315E: An exception occurred: java.lang.Throwable: jakarta.servlet.ServletException: class java.lang.String cannot be cast to class java.util.UUID 
            server.stopServer("SRVE0777E","SRVE0315E"); 
        }

    }

    @Before
    public void setupPerTest() throws Exception {
        server.setMarkToEndOfLog();
    }


    /**
     *  Verify the UUID conversion works with an explicit converter (f:converter tag)
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulExplicitUUIDConverter() throws Exception {
        try (WebClient webClient = new WebClient()) {

            server.setMarkToEndOfLog();

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(server, APP_NAME, "index_explicit_converter.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);


            HtmlElement uuidElement = (HtmlElement) page.getElementById("form:uuid");
            String clientWindowJS = uuidElement.asText();

            // Get the form.
            HtmlForm form = page.getFormByName("form");

            // Get the button and click it.
            HtmlSubmitInput contentLengthButton = form.getInputByName("form:button");
            page = contentLengthButton.click();

            System.out.println(uuidElement.getTextContent());

            assertTrue("UUID not found on page!",  page.asText().contains(uuidElement.getTextContent()));
            
            assertFalse("Set ID call not found!", server.findStringsInLogsAndTraceUsingMark("SETID: class java.util.UUID").isEmpty());

            server.resetLogMarks();
        }
    }

    /**
     *   Verify the UUID conversion works with an implicitly converter (no tag or converter specified)
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulImplicitUUIDConverter() throws Exception {
        try (WebClient webClient = new WebClient()) {

            server.setMarkToEndOfLog();

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(server, APP_NAME, "index_implicit_converter.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);


            HtmlElement uuidElement = (HtmlElement) page.getElementById("form:uuid");
            String clientWindowJS = uuidElement.asText();

            // Get the form.
            HtmlForm form = page.getFormByName("form");

            // Get the button and click it.
            HtmlSubmitInput contentLengthButton = form.getInputByName("form:button");
            page = contentLengthButton.click();

            System.out.println(uuidElement.getTextContent());

            assertTrue("UUID not found on page!",  page.asText().contains(uuidElement.getTextContent()));
            
            assertFalse("Set ID call not found!", server.findStringsInLogsAndTraceUsingMark("SETID: class java.util.UUID").isEmpty());

            server.resetLogMarks();
        }
    }

    /**
     *  Expect conversion to fail because it tests a non-uuid string value (FailBean#id) 
     * 
     * @throws Exception
     */
    @AllowedFFDC({"java.lang.ClassCastException","jakarta.servlet.ServletException"})
    @Test
    public void testFailingUUIDConverter() throws Exception {
        try (WebClient webClient = new WebClient()) {

            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(server, APP_NAME, "fail.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            assertTrue("UUID should have failed. Page did not contain a ClassCastException", page.asText().contains("java.lang.ClassCastException"));
        }
    }



}
