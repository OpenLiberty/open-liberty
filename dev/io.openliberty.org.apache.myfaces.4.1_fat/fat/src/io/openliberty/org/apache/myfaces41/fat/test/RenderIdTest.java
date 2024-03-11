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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

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
import componenttest.topology.impl.LibertyServer;
import io.openliberty.org.apache.myfaces41.fat.JSFUtils;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.ShrinkHelper;


@RunWith(FATRunner.class)
public class RenderIdTest {

    private static final String APP_NAME = "RenderId_Spec1760";
    protected static final Class<?> c = RenderIdTest.class;

    private static final Logger LOG = Logger.getLogger(ContentLengthTest.class.getName());

    @Rule
    public TestName name = new TestName();

    @Server("faces41_renderIdServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war",
                                      "io.openliberty.org.apache.myfaces41.fat.namespace.view");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(RenderIdTest.class.getSimpleName() + ".log");

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
     * Skipped as this is not yet working in 4.1.0-RC1
     *
     * @throws Exception
     */
    // @Test
    public void verifyIDsAreGeneratedOnHead() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(server, APP_NAME, "index.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // assertNotNull("Id attribute not found on head!", page.getHead().getId());
        }
    }



}
