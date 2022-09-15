/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.pages31.fat.tests;

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
 *  
 */
@RunWith(FATRunner.class)
public class MiscTests {
    private static final String APP_NAME = "Misc";

    private static final Logger LOG = Logger.getLogger(MiscTests.class.getName());

    @Server("pagesServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war");
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

        assertNotNull("jsp:plugin skip message not found!", server.waitForStringInTrace("Skipping the jsp:plugin element as it is a no operation for Pages 3.1+"));

        server.resetLogMarks();

    }
}
