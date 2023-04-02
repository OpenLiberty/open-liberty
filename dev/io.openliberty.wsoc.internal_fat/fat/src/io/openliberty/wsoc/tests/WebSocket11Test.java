/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
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
package io.openliberty.wsoc.tests;

import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.wsoc.tests.all.WebSocketVersion11Test;
import io.openliberty.wsoc.util.OnlyRunNotOnZRule;
import io.openliberty.wsoc.util.WebServerSetup;
import io.openliberty.wsoc.util.wsoc.WsocTest;

/**
 * Tests WebSocket Stuff
 *
 * @author unknown
 */
@RunWith(FATRunner.class)
public class WebSocket11Test {
    public static final String SERVER_NAME = "webSocket11Server";
    @Server(SERVER_NAME)

    public static LibertyServer LS;

    private static WebServerSetup bwst = null;

    @Rule
    public final TestRule notOnZRule = new OnlyRunNotOnZRule();

    private static WsocTest wt = null;
    private static WebSocketVersion11Test pt = null;

    private static final String WEBSOCKET_11_WAR_NAME = "websocket11";

    private static final Logger LOG = Logger.getLogger(WebSocket11Test.class.getName());

    @BeforeClass
    public static void setUp() throws Exception {
        // Build the war app and add the dependencies
        WebArchive Websocket11App = ShrinkHelper.buildDefaultApp(WEBSOCKET_11_WAR_NAME + ".war",
                                                                 "websocket11.war",
                                                                 "io.openliberty.wsoc.common");
        Websocket11App = (WebArchive) ShrinkHelper.addDirectory(Websocket11App, "test-applications/" + WEBSOCKET_11_WAR_NAME + ".war/resources");
        ShrinkHelper.exportDropinAppToServer(LS, Websocket11App);

        LS.startServer();
        LS.waitForStringInLog("CWWKZ0001I.* " + WEBSOCKET_11_WAR_NAME);

        bwst = new WebServerSetup(LS);
        bwst.setUp();
        wt = new WsocTest(LS, false);
        pt = new WebSocketVersion11Test(wt);
    }

    @AfterClass
    public static void tearDown() throws Exception {

        // give the system 10 seconds to settle down before stopping
        try {
            Thread.sleep(10000);
        } catch (InterruptedException x) {

        }

        if (LS != null && LS.isStarted()) {
            LS.stopServer();
        }
        bwst.tearDown();
    }

    //
    //
    // WebSocket 1.1 TESTS
    //
    //

    @Mode(TestMode.LITE)
    @Test
    public void testProgrammaticTextSuccess() throws Exception {
        pt.testProgrammaticTextSuccess();
    }

    @Mode(TestMode.LITE)
    @Test
    public void testProgrammaticReaderSuccess() throws Exception {
        pt.testProgrammaticReaderSuccess();
    }

    @Mode(TestMode.LITE)
    @Test
    public void testProgrammaticPartialTextSuccess() throws Exception {
        pt.testProgrammaticPartialTextSuccess();
    }

    @Mode(TestMode.LITE)
    @Test
    public void testClientAnnoWholeServerProgPartial() throws Exception {
        pt.testClientAnnoWholeServerProgPartial();
    }

    @Mode(TestMode.LITE)
    @Test
    public void testProgrammaticInputStreamSuccess() throws Exception {
        pt.testProgrammaticInputStreamSuccess();
    }

}