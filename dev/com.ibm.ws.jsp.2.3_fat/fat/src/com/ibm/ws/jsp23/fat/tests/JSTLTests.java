/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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
package com.ibm.ws.jsp23.fat.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jsp23.fat.JSPUtils;
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
 *  Smoke Tests for JSTL
 */
// No need to run against cdi-2.0 since these tests don't use CDI at all.
@SkipForRepeat("CDI-2.0")
@RunWith(FATRunner.class)
public class JSTLTests {
    private static final String APP_NAME = "TestJSTL";
    private static final String IMPORT_APP_NAME = "TestJSTLImport";
    private static final Logger LOG = Logger.getLogger(JSTLTests.class.getName());

    private String newLine = System.getProperty("line.separator");

    private final String replacePattern = "["+newLine+"\t ]";

    private final String EXPECTED_NAME_SORTED_STOCK_TABLE = "<h2>Name Sorted Stock Table</h2><table id=\"nameSortedStockTable\" border=\"1\" width=\"100%\">" +
    "<tr><th style=\"text-align:left\">Sorted Stock</th><th style=\"text-align:left\">Price</th></tr>" +
    "<tr><td><i>AAPL</i></td><td>131</td></tr><tr><td><i>GME</i></td><td>46</td></tr><tr><td><i>IBM</i></td><td>120</td></tr></table>";
    private final String EXPECTED_PRICE_SORTED_STOCK_TABLE = "<h2>Price Sorted Stock Table</h2><table id=\"priceSortedStockTable\" border=\"1\" width=\"100%\">" + 
    "<tr><th style=\"text-align:left\">Sorted Stock</th><th style=\"text-align:left\">Price</th></tr>" + 
    "<tr><td><i>GME</i></td><td>46</td></tr><tr><td><i>IBM</i></td><td>120</td></tr><tr><td><i>AAPL</i></td><td>131</td></tr></table>";
    private final String EXPECTED_STOCK_TABLE = "<h2>Stock Table</h2><table id=\"stockTable\" border=\"1\" width=\"100%\">" +
    "<tr><th style=\"text-align:left\">Stock</th><th style=\"text-align:left\">Price</th></tr>" +
    "<tr><td><i>AAPL</i></td><td>131</td></tr><tr><td><i>IBM</i></td><td>120</td></tr><tr><td><i>GME</i></td><td>46</td></tr></table>";

    @Server("jstlServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war");
        ShrinkHelper.defaultDropinApp(server, IMPORT_APP_NAME + ".war");

        server.startServer(JSTLTests.class.getSimpleName() + ".log");
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
    public void testCoreTags() throws Exception {
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
    public void testFunctionTags() throws Exception {
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
    public void testXMLTags() throws Exception {
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
     * Basic Test for transform in XML Tags
     *
     * @throws Exception if something goes horribly wrong
     */
    @Test
    public void testXMLTransformTag() throws Exception ,Throwable {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, APP_NAME, "xmlTransformTag.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());
        String cleanedResponse = response.getText().replaceAll(replacePattern, "");
        assertTrue("Something went wrong with the xml transform tags. Couldn't match expected stock table.", cleanedResponse.contains(EXPECTED_STOCK_TABLE.replaceAll(replacePattern, "")));
        assertTrue("Something went wrong with the xml transform tags. Couldn't match expected name sorted stock table.", cleanedResponse.contains(EXPECTED_NAME_SORTED_STOCK_TABLE.replaceAll(replacePattern, "")));
        assertTrue("Something went wrong with the xml transform tags. Couldn't match expected price sorted stock table.", cleanedResponse.contains(EXPECTED_PRICE_SORTED_STOCK_TABLE.replaceAll(replacePattern, "")));
    }

    /**
     * Tests Import of SQL Tag Library
     * 
     * TODO: Add DB to test tags? 
     *
     * @throws Exception if something goes horribly wrong
     */
    @Test
    public void testSqlImport() throws Exception {
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
    public void testFormatTags() throws Exception {
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

    /**
     * Flushes during an import. See below for more details. 
     *  
     * https://github.com/eclipse-ee4j/jstl-api/issues/153
     *
     * @throws Exception if something goes horribly wrong
     */
    @Test
    @Mode(TestMode.FULL)
    public void testImportFlushing() throws Exception {
        WebConversation wc = new WebConversation();

        /**
         *  Expected Output: 
         *  <p> import: </p> 
         *  <p> output: </p>  
         *   Begin
         *   End
         */

        String url = JSPUtils.createHttpUrlString(server, IMPORT_APP_NAME, "index.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);

        String text =  response.getText();
        LOG.info("Servlet response : " + response.getText());

        // Multi Strings are difficult to work with; Used indicies instead to check location
        int beginIndex = text.indexOf("Begin"); // Expected: 605
        int endIndex = text.indexOf("End"); // Expected: 613

        assertTrue("Something went wrong with the flushing within the import tag", 8 == (endIndex - beginIndex));

    }
}
