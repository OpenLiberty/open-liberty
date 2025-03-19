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

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.org.apache.myfaces41.fat.JSFUtils;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.ShrinkHelper;

import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gargoylesoftware.htmlunit.html.HtmlElement;

/*
 * Test for the following Spec Issue: 
 * https://github.com/jakartaee/faces/issues/1263
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class RowStatePreservedTest {

    private static final String APP_NAME = "rowStatePreserved_Spec1263";
    protected static final Class<?> c = RowStatePreservedTest.class;

    private static final Logger LOG = Logger.getLogger(RowStatePreservedTest.class.getName());

    @Rule
    public TestName name = new TestName();

    @Server("faces41_rowStatePreserved")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war",
                                      "io.openliberty.org.apache.myfaces41.fat.rowStatePreserved");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(RowStatePreservedTest.class.getSimpleName() + ".log");

    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Tests the UIRepeat rowStatePreserved property.  
     * The property is set to true in the app. 
     * 
     * Verify that the inputs are not cleared when the forms are submited.
     *
     * @throws Exception
     */
    @Test
    public void testrowStatePreservedOnUIRepeat() throws Exception {

        String inputOneID = "val:0:mainForm:text";
        String inputTwoID = "val:1:mainForm:text";

        String submitOneID = "val:0:mainForm:submit";
        String submitTwoID = "val:1:mainForm:submit";

        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(server, APP_NAME, "index.xhtml");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            HtmlTextInput inputText1 = (HtmlTextInput) page.getElementById(inputOneID);
            inputText1.type("Test-One");

            HtmlElement submitButton1 = (HtmlElement) page.getElementById(submitOneID);
            page = submitButton1.click();

            HtmlTextInput inputText2 = (HtmlTextInput) page.getElementById(inputTwoID);
            inputText2.type("Test-Two");

            HtmlElement submitButton2 = (HtmlElement) page.getElementById(submitTwoID);
            page = submitButton2.click();

            logPage(page);

            inputText1 = (HtmlTextInput) page.getElementById(inputOneID);
            inputText2 = (HtmlTextInput) page.getElementById(inputTwoID);

            assertEquals("Text Not Found on First Input!", "Test-One",  inputText1.getValueAttribute());
            assertEquals("Text Not Found! on Second Input!", "Test-Two",  inputText2.getValueAttribute());
        }
    }

    public void logPage(HtmlPage page) {
        LOG.info(page.asXml());
    }

}
