/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.pages31.fat.tests;

import static componenttest.annotation.SkipForRepeat.EE11_OR_LATER_FEATURES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

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


/**
 *  Tests new changes added in Pages 3.1 that don't really need a a standalone test class. 
 *  - IsThreadSafe is depcrecated, so we check for a warning 
 *  - Verify jsp:plugin is skipped 
 *  - Verify imports are available in Expression Language (not just scriplets), i.e. ${ }
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class MiscTests {
    private static final String APP_NAME = "Misc";

    private static final Logger LOG = Logger.getLogger(MiscTests.class.getName());

    @Server("pagesServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "io.openliberty.pages31.fat.misc", "io.openliberty.pages31.fat.misc.other");
        server.startServer();
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer("CWWJS0003W");
        }
    }

    /**
     *
     * @throws Exception if something goes horribly wrong
     */
    @SkipForRepeat(EE11_OR_LATER_FEATURES)
    @Test
    public void testIsThreadSafeLogsWarning() throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, APP_NAME, "isThreadSafe.jsp");
        LOG.info("url: " + url);

        server.setMarkToEndOfLog();

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());

        // CWWJS0003W: Per the Pages 3.1 specification, pages developers are strongly advised not to use the deprecated page directive isThreadSafe. 
        assertNotNull("CWWJS0003W Warning not found!", server.waitForStringInLogUsingMark("CWWJS0003W"));

        server.resetLogMarks();
    }

    /**
     *
     * @throws Exception if something goes horribly wrong
     */
    @SkipForRepeat(EE11_OR_LATER_FEATURES)
    @Test
    public void testJspPluginNoOp() throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, APP_NAME, "jspPlugin.jsp");
        LOG.info("url: " + url);

        server.setMarkToEndOfLog();

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());

        assertTrue("jsp:plugin may have been generated!",  response.getText().contains("Nothing should be generated  in between.") ); 

        assertNotNull("jsp:plugin skip message not found!", server.waitForStringInTrace("Skipping the jsp:plugin element as it is a no operation for Pages 3.1+"));

        server.resetLogMarks();

    }

    /**
     *
     * @throws Exception if something goes horribly wrong
     */
    @Test
    public void testImportsAreAvailableViaExpressionLanguage() throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, APP_NAME, "imports.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());

        assertTrue("Imports not found in Expression Language Environment", response.getText().contains("LIGHT"));
        assertTrue("Imports not found in Expression Language Environment", response.getText().contains("CAFFE NERO"));
        assertTrue("Imports not found in Expression Language Environment", response.getText().contains("CANE SUGAR"));
    }

    /**
     * Verify if static imports can be accessed through a JSP with both Expression Language and Pages expressions
     * Interface field only available through JSP
     *
     * @throws Exception if something goes horribly wrong
     */
    @Test
    public void testStaticImportsAreAvailable() throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, APP_NAME, "staticImports.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());

        assertEquals(200, response.getResponseCode()); // Verifies no exception occurs if a page tries to import a field from an interface via EL
        assertTrue("Static field not found in Expression Language Environment", response.getText().contains("EL Field expression: CAFFE NERO"));
        assertTrue("Static method not found in Expression Language Environment", response.getText().contains("EL Method expression: LATTE"));
        assertTrue("Static field not found in Pages Environment", response.getText().contains("JSP Field expression: CAFFE NERO"));
        assertTrue("Static method not found in Pages Environment", response.getText().contains("JSP Method expression: LATTE"));
        assertTrue("Interface field not found in Pages Environment", response.getText().contains("JSP Static Interface Field expression: INTERFACE_FIELD"));
    }


}
