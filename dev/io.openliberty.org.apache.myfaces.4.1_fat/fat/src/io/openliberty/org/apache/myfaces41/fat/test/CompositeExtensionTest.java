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

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.org.apache.myfaces41.fat.JSFUtils;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class CompositeExtensionTest {

    private static final String APP_NAME = "compositeExtension_Spec1549";
    protected static final Class<?> c = CompositeExtensionTest.class;

    private static final Logger LOG = Logger.getLogger(CompositeExtensionTest.class.getName());

    @Rule
    public TestName name = new TestName();

    @Server("faces41_compositeExtensionServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(CompositeExtensionTest.class.getSimpleName() + ".log");

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
     *  Verify that composite:extension is marked as deprecated in the logs
     *
     * @throws Exception
     */
    @Test
    public void verifyCompositeExtensionWarning() throws Exception {

             try (WebClient webClient = new WebClient()) {
     
                 URL url = JSFUtils.createHttpUrl(server, APP_NAME, "index.xhtml");
                 HtmlPage page = (HtmlPage) webClient.getPage(url);

                 assertEquals(1, server.findStringsInLogs("The tag composite:extension is deprecated as of 4.1.").size());
             }
    }



}
