/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.myfaces41.fat.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.org.apache.myfaces41.fat.JSFUtils;

/*
 * Verifies the new EL Resolvers are added in Faces 4.1
 * - OptinalELResolver
 * - RecordELResolver
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class Faces41ResolverTests {

    private static final String APP_NAME = "ExpressionLanguageResolvers";

    protected static final Class<?> c = Faces41ResolverTests.class;

    private static final Logger LOG = Logger.getLogger(Faces41ResolverTests.class.getName());

    private boolean debug = true; // Logs Pages as XML when true

    @Rule
    public TestName name = new TestName();

    @Server("faces41_resolversServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war",
                                      "io.openliberty.org.apache.myfaces41.fat.resolver.beans");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(Faces41ResolverTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }

    }

    /**
     *  Expect the Person Record to Resolve
     * 
     * @throws Exception
     */
    @Test
    public void testRecordResolver() throws Exception {

        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(server, APP_NAME, "RecordResolver.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            logPage(page);

            assertTrue("Name not found. RecordResolver Failed!",  page.asText().contains("Username: Watson"));
            assertTrue("Email not found.  RecordResolver Failed!",  page.asText().contains("Email: example@email.com"));
        }

    }

    /**
     *  Expect the Optional to Resolve for both a value present (PersonBean) and an empty optional. 
     * 
     * @throws Exception
     */
    @Test
    public void testOptionalResolver() throws Exception {

        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(server, APP_NAME, "OptionalResolver.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            logPage(page);

            // Quotes added to see empty optional value
            assertTrue("Name not found. OptionalELResolver failed!",  page.asText().contains("Value Present Optional: \"Watson\""));
            assertTrue("Empty Optional failed to resolve!",  page.asText().contains("Empty Optional (abc does not exist): \"\""));
        }

    }

    public void logPage(HtmlPage page){
            LOG.info(page.asXml());
    }

}
