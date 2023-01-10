/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.pages31.fat.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import io.openliberty.pages31.fat.JSPUtils;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

// For testFormatTags
import java.util.Date;
import java.text.DateFormat;
import java.util.Locale;

/**
 *  Smoke Tests for JSTL EE10 Tags
 */
@RunWith(FATRunner.class)
public class TagsTestsEE10 {
    private static final String APP_NAME = "TestTagsEE10";

    private static final Logger LOG = Logger.getLogger(TagsTestsEE10.class.getName());

    private String newLine = System.getProperty("line.separator");

    @Server("pagesServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war");
        server.startServer(TagsTestsEE10.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Basic Test for a subset of the Core Tags
     *
     * @throws Exception if something goes horribly wrong
     */
    @Test
    public void testEE10CoreTags() throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, APP_NAME, "coreTags.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());


        assertTrue("#1 Something went wrong with the core tags", response.getText().contains("Item 1"));
        assertTrue("#2 Something went wrong with the core tags", response.getText().contains("c:when works!"));

    }

    /**
     * Basic Test for a subset of the Function Tags
     *
     * @throws Exception if something goes horribly wrong
     */
    @Test
    public void testEE10FunctionTags() throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, APP_NAME, "functionTags.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());

        assertTrue("#1 Something went wrong with function tags", response.getText().contains("Function Tag Works"));
        assertTrue("#2 Something went wrong with the function tags", response.getText().contains("Length of 'Tags' is: 4"));

    }

    /**
     * Basic Test for a subset of the XML Tags
     *
     * @throws Exception if something goes horribly wrong
     */
    @Test
    public void testEE10XMLTags() throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, APP_NAME, "xmlTags.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());

        assertTrue("Something went wrong with the xml tags", response.getText().contains("IBM is trading above 100"));

    }

    /**
     * Tests Import of SQL Tag Library
     * 
     * TODO: Add DB to test tags? 
     *
     * @throws Exception if something goes horribly wrong
     */
    @Test
    public void testEE10SqlImport() throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, APP_NAME, "sqlTagLibImport.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());

        assertTrue("Something went wrong with the SQL tag library import", response.getText().contains("Success!"));

    }

    /**
     * Basic Test for a subset of the Format Tags
     *
     * @throws Exception if something goes horribly wrong
     */
    @Test
    public void testEE10FormatTags() throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, APP_NAME, "formatTags.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());

        Date today = new Date();
        DateFormat dateFormatter = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US);
        String dateOut = dateFormatter.format(today);

        assertTrue("Something went wrong with format tags. Did not find expected string: " + dateOut,
            response.getText().contains(dateOut));

        assertTrue("Something went wrong with the parse tags!",
            response.getText().contains("Sat Jan 01 00:00:00")); 

    }
}
