/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.myfaces40.fat.tests;

import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Tests for the <h:inputText/> type attribute which is a new feature for Faces 4.0.
 */
@RunWith(FATRunner.class)
public class InputTextTypeTest {
    private static final Logger LOG = Logger.getLogger(InputTextTypeTest.class.getName());
    private static final String APP_NAME = "InputTextType";

    @Server("faces40_inputTextTypeServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war");

        server.startServer(InputTextTypeTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("testCleanUp : stop server");

        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Test the <h:inputText/> type attribute.
     *
     * 1) Test the default type attribute.
     * 2) Test a non default type attribute.
     * 3) Test the discommended type attributes.
     *
     * @throws Exception
     */
    @Test
    public void testInputTextType() throws Exception {
        URL url = HttpUtils.createURL(server, "/" + APP_NAME + "/InputTextType.xhtml");

        try (WebClient webClient = new WebClient()) {
            String type;
            HtmlInput input;

            // Drive a request to the page.
            HtmlPage page = (HtmlPage) webClient.getPage(url);
            LOG.info(page.asText());
            LOG.info(page.asXml());

            // default
            input = (HtmlInput) page.getElementById("form1:defaultType");
            type = input.getTypeAttribute();
            assertTrue("Type attribute should have been \"text\" but was: " + type, type.equals("text"));

            // email - non default
            input = (HtmlInput) page.getElementById("form1:emailType");
            type = input.getTypeAttribute();
            assertTrue("Type attribute should have been \"email\" but was: " + type, type.equals("email"));

            // Ensure expected messages are output for types that have a better alternative.
            /*
             * <h:inputText type="hidden"> is discommended, you should instead use <h:inputHidden>
             * <h:inputText type="password"> is discommended, you should instead use <h:inputSecret>
             * <h:inputText type="checkbox"> is discommended, you should instead use <h:selectBooleanCheckbox> or <h:selectManyCheckbox>
             * <h:inputText type="radio"> is discommended, you should instead use <h:selectOneRadio>
             * <h:inputText type="file"> is discommended, you should instead use <h:inputFile>
             * <h:inputText type="submit"> is discommended, you should instead use <h:commandButton>
             * <h:inputText type="image"> is discommended, you should instead use <h:commandButton type="image">
             * <h:inputText type="reset"> is discommended, you should instead use <h:commandButton type="reset">
             * <h:inputText type="button"> is discommended, you should instead use <h:commandButton type="button"> or <h:button>
             */

            // Count the number of occurrences of "discommended" which is a common word in all the messages.
            int numberOccurrences = 0;
            Pattern pattern = Pattern.compile("discommended");
            Matcher matcher = pattern.matcher(page.asText());
            while (matcher.find()) {
                numberOccurrences++;
            }

            assertTrue("The number of messages should have been 9 but was: " + numberOccurrences, numberOccurrences == 9);
        }
    }
}
