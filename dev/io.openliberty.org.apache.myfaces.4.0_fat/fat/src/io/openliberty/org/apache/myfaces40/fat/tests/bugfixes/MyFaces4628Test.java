/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.myfaces40.fat.tests.bugfixes;

import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * https://github.com/OpenLiberty/open-liberty/issues/26390
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class MyFaces4628Test {

    protected static final Class<?> clazz = MyFaces4628Test.class;

    private static final String APP_NAME = "MYFACES-4628";

    @Server("faces40_myfaces4628")
    public static LibertyServer server;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war",
                                      "io.openliberty.org.apache.faces40.fat.myfaces4628.bean");

        server.startServer(MyFaces4628Test.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /*
     * Bug where submitted value was not check properly against disabled values. 
     */
    @Test
    public void testMyFaces4628() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = HttpUtils.createURL(server, "/" + APP_NAME + "/test.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);
            HtmlSelect select = (HtmlSelect)page.getElementById("form:select");
            select.setSelectedAttribute(select.getOptionByValue("two"), true);
            page = page.getElementById("form:submit").click();
            Log.info(clazz, name.getMethodName(), page.asXml());

            assertTrue("Item two was not selected!", page.getElementById("form:output").getTextContent().contains("Value is: 'two'"));

        }
    }

       /*
        * Related issue (Also a TCK Test):
        * https://github.com/eclipse-ee4j/mojarra/issues/4330 
        * Faces should check if a submitted value is disabled on the server-end. 
        * If so, then it should present a validation error. 
        */
    @Test
    public void testIssue4330() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = HttpUtils.createURL(server, "/" + APP_NAME + "/test.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);
            HtmlSelect select = (HtmlSelect)page.getElementById("form:select");
            HtmlOption option = select.getOptionByValue("three");

            option.removeAttribute("disabled"); // remove disable attribute manually 
            select.setSelectedAttribute(option, true);
            page = page.getElementById("form:submit").click();
            Log.info(clazz, name.getMethodName(), page.asXml());

            assertTrue("Validation Error failed to appear!", page.asText().contains("Validation Error: Value is required."));

        }
    }
}
