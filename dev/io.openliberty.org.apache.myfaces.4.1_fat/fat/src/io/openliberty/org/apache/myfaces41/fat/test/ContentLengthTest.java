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
import org.junit.Assert;

import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.org.apache.myfaces41.fat.JSFUtils;

/**
 * https://github.com/jakartaee/faces/issues/1764
 * 
 * Test to ensure that we can modify the Content Length using the ExternalContext#setResponseContentLengthLong API.
 */
@RunWith(FATRunner.class)
public class ContentLengthTest {
    private static final Logger LOG = Logger.getLogger(ContentLengthTest.class.getName());

    private static final String APP_NAME = "SetResponseContentLengthLong_Spec1829";
    protected static final Class<?> c = ContentLengthTest.class;

    @Rule
    public TestName name = new TestName();

    @Server("faces41_contentLengthLongServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war",
                                      "io.openliberty.org.apache.myfaces41.fat.contentlengthlong.beans");
        // Start the server and use the class name so we can find logs easily.
        server.startServer(ContentLengthTest.class.getSimpleName() + ".log");

    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Before
    public void setupPerTest() throws Exception {
        server.setMarkToEndOfLog();
    }

    /**
     *  Verfies the content length is 1000 when set via the bean call. 
     *
     * @throws Exception
     */
    @Test
    public void verifySetResponseContentLengthLong() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(server, APP_NAME, "index.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Get the form.
            HtmlForm form = page.getFormByName("form");

            // Get the button and click it.
            HtmlSubmitInput contentLengthButton = form.getInputByName("form:button");
            page = contentLengthButton.click();

            String expectedContentLength = "1000";
            Boolean isFound = false;

            List<NameValuePair> headers = page.getWebResponse().getResponseHeaders();
            // Look at the headers
            for (NameValuePair header : headers) {
                LOG.info(header.getName() + ":" + header.getValue());
                if (header.getName().equals("Content-Length")) {
                    assertEquals("Content Length did not match!", expectedContentLength, header.getValue());
                    isFound = true;
                }
            }

            if(!isFound){
                Assert.fail("Content-Length Header not found!");
            }

        }
    }

}
