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
package com.ibm.ws.jsp23.fat.tests;

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

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests to execute on the jspSkipMetaInfServer that use HttpUnit/HttpClient along with
 * wc property skipMetaInfResourcesProcessing is true.
 */

@SkipForRepeat("CDI-2.0")
@RunWith(FATRunner.class)
public class JSPSkipMetaInfTests {
    private static final Logger LOG = Logger.getLogger(JSPSkipMetaInfTests.class.getName());
    private static final String TestEDR_APP_NAME = "TestEDR";

    @Server("jspSkipMetaInfServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, TestEDR_APP_NAME + ".war");

        server.startServer(JSPSkipMetaInfTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Verify a taglib under WEB-INF that's suppressed by
     * skipMetaInfResourcesProcessing=true doesn't
     * cause a Null Pointer Exception per issue 20247.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTLD() throws Exception {
        String orgEdrFile = "headerEDR1.jsp";
        String relEdrPath = "../../shared/config/ExtendedDocumentRoot/";
        server.copyFileToLibertyServerRoot(relEdrPath, orgEdrFile);
        String url = JSPUtils.createHttpUrlString(server, TestEDR_APP_NAME, "index.jsp");
        LOG.info("url: " + url);
        WebConversation wc1 = new WebConversation();
        WebRequest request1 = new GetMethodWebRequest(url);
        wc1.getResponse(request1);

        server.deleteFileFromLibertyServerRoot(relEdrPath + orgEdrFile); // cleanup
        Thread.sleep(500L); // ensure file is deleted
        //This test doesn't need to assert anything since the NPE if it happens would be caught by simplicity.
    }

}
