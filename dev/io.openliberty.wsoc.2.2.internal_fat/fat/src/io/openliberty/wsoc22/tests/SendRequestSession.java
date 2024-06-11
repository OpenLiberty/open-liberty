
/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.wsoc22.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebBrowserFactory;
import com.ibm.ws.fat.util.browser.WebResponse;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

import org.asynchttpclient.ws.*;
import org.asynchttpclient.*;
import io.netty.util.concurrent.*;
/**
 * WebSocket 2.2 Test
 */
@RunWith(FATRunner.class)
public class SendRequestSession {

    @Server("sessionTestServer")
    public static LibertyServer LS;

    private static final Logger LOG = Logger.getLogger(SendRequestSession.class.getName());

    private static final String SESSION_WAR_NAME = "session";

    @BeforeClass
    public static void setUp() throws Exception {

        // Build the war app and add the dependencies
        WebArchive SessionApp = ShrinkHelper.buildDefaultApp(SESSION_WAR_NAME + ".war",
                "session.war",
                "session.war.*");

        SessionApp = (WebArchive) ShrinkHelper.addDirectory(SessionApp,
                "test-applications/" + SESSION_WAR_NAME + ".war/resources");
        ShrinkHelper.exportDropinAppToServer(LS, SessionApp);

        LS.startServer();
        LS.waitForStringInLog("CWWKZ0001I.* " + SESSION_WAR_NAME);
    }

    @AfterClass
    public static void tearDown() throws Exception {

        // give the system .5 seconds to settle down before stopping
        try {
            Thread.sleep(500);
        } catch (InterruptedException x) {

        }

        if (LS != null && LS.isStarted()) {
            LS.stopServer();
        }

    }

    /*
     * Tests that the session after the message is sent is not null
     * by comparing IDs before and after the message
     */
    @Mode(TestMode.LITE)
    @Test
    public void testSession() throws Exception {
        WebSocketUpgradeHandler.Builder upgradeHandlerBuilder = new WebSocketUpgradeHandler.Builder();
        WebSocketUpgradeHandler wsHandler = upgradeHandlerBuilder
                .addWebSocketListener(new WebSocketListener() {
                    @Override
                    public void onOpen(WebSocket websocket) {
                        // WebSocket connection opened
                        System.out.println("Opened Websocket");
                    }

                    @Override
                    public void onClose(WebSocket websocket, int code, String reason) {
                        // WebSocket connection closed                        
                    }

                    @Override
                    public void onError(Throwable t) {
                        // WebSocket connection error
                    }

                    @Override
                    public void onTextFrame(String payload, boolean finalFragment, int rsv){
                        // Log message
                        System.out.println("Debugging: " + payload);
                    }
                }).build();

        WebSocket webSocketClient = Dsl.asyncHttpClient()
                .prepareGet("ws://" + 
                            LS.getHostname() + ":" + 
                            LS.getHttpDefaultPort() + 
                            "/session/echo")
                .setRequestTimeout(5000)
                .execute(wsHandler)
                .get();
        
        if (webSocketClient.isOpen()) {
            System.out.println("sending message");
            webSocketClient.sendTextFrame("test message");
        }

        String msgSession = LS.waitForStringInLog("MSG SESSION: ", LS.getConsoleLogFile());
        String resultSession = LS.waitForStringInLog("RESULT SESSION: ", LS.getConsoleLogFile());
        assertNotNull("The following String was not found in the log: ", msgSession);
        assertNotNull("The following String was not found in the log: ", resultSession);
        System.out.println(msgSession);
        System.out.println(resultSession);
        assertTrue("The Session ID's were not the same.", msgSession.substring(msgSession.indexOf(": ")).equals(resultSession.substring(resultSession.indexOf(": "))));
    }

}
