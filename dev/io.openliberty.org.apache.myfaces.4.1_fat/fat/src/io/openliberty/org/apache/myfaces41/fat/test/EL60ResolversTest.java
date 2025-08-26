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

/*
 * Testing OptionalELResolver and RecordELResolver are picked up by Faces 4.1 
 */
@RunWith(FATRunner.class)
public class EL60ResolversTest {

    private static final String APP_NAME = "EL60Resolvers";
    protected static final Class<?> c = EL60ResolversTest.class;

    private static final Logger LOG = Logger.getLogger(EL60ResolversTest.class.getName());

    @Rule
    public TestName name = new TestName();

    @Server("faces41_resolverServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war",
                                      "io.openliberty.org.apache.myfaces41.fat.optional",
                                      "io.openliberty.org.apache.myfaces41.fat.record");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(EL60ResolversTest.class.getSimpleName() + ".log");

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
     * 
     * Documentation:
     * https://jakarta.ee/specifications/expression-language/6.0/apidocs/jakarta.el/jakarta/el/optionalelresolver
     * 
     * @throws Exception
     */
    @Test
    public void testOptionalResolver() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(server, APP_NAME, "optional.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            logPage(page);

            /*
             * anotherSimpleBean is not null, so it should be returned. Then testString should be resolved
             * ${simpleBeanWithOptionalProperty.anotherSimpleBean.testString}
             */
            assertTrue("Optional Fails for Bean", page.asText().contains("Value Present Optional: \"Inner Text\""));

            /*
             * testString is not null, so the value should be returned -- ${simpleBeanWithOptionalProperty.testString}
             */
            assertTrue("Optional Fails for String", page.asText().contains("Empty Optional: \"~~testString~~\""));

            /*
             * optionalString is null, so no value should be returned -- ${simpleBeanWithOptionalProperty.optionalString}
             */
            assertTrue("Optional Fails for Empty", page.asText().contains("Empty Optional: \"\""));

        }
    }

    /**
     * 
     * Documentation:
     * https://jakarta.ee/specifications/expression-language/6.0/apidocs/jakarta.el/jakarta/el/recordelresolver
     * 
     * @throws Exception
     */
    @Test
    public void testRecordResolver() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(server, APP_NAME, "record.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            logPage(page);

            assertTrue("Record Not Resolved via Expression Language!", page.asText().contains("property1: abc"));
            assertTrue("Record Not Resolved via Expression Language!", page.asText().contains("property2: def"));
        }
    }

    public void logPage(HtmlPage page) {
        LOG.info(page.asXml());
    }

}
