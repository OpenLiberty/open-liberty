/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.wsoc.tests;

import java.util.Set;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
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
public class WebSocket11Test extends LoggingTest {

    @ClassRule
    public static SharedServer SS = new SharedServer("webSocket11Server", false);

    private static WebServerSetup bwst = new WebServerSetup(SS);

    @Rule
    public final TestRule notOnZRule = new OnlyRunNotOnZRule();

    private final WsocTest wt = new WsocTest(SS, false);

    private final WebSocketVersion11Test pt = new WebSocketVersion11Test(wt);

    private static final String WEBSOCKET_11_WAR_NAME = "websocket11";

    private static final Logger LOG = Logger.getLogger(WebSocket11Test.class.getName());

    @BeforeClass
    public static void setUp() throws Exception {
        // Build the war app and add the dependencies
        WebArchive Websocket11App = ShrinkHelper.buildDefaultApp(WEBSOCKET_11_WAR_NAME + ".war",
                                                                 "websocket11.war",
                                                                 "io.openliberty.wsoc.common");
        Websocket11App = (WebArchive) ShrinkHelper.addDirectory(Websocket11App, "test-applications/" + WEBSOCKET_11_WAR_NAME + ".war/resources");
        // Verify if the apps are in the server before trying to deploy them
        if (SS.getLibertyServer().isStarted()) {
            Set<String> appInstalled = SS.getLibertyServer().getInstalledAppNames(WEBSOCKET_11_WAR_NAME);
            LOG.info("addAppToServer : " + WEBSOCKET_11_WAR_NAME + " already installed : " + !appInstalled.isEmpty());
            if (appInstalled.isEmpty())
                ShrinkHelper.exportDropinAppToServer(SS.getLibertyServer(), Websocket11App);
        }
        SS.startIfNotStarted();
        SS.getLibertyServer().waitForStringInLog("CWWKZ0001I.* " + WEBSOCKET_11_WAR_NAME);
        bwst.setUp();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (SS.getLibertyServer() != null && SS.getLibertyServer().isStarted()) {
            SS.getLibertyServer().stopServer(null);
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

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.util.LoggingTest#getSharedServer()
     */
    @Override
    protected SharedServer getSharedServer() {
        return SS;
    }

}