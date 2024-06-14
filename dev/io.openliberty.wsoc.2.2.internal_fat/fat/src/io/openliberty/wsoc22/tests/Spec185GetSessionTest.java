
/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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

import org.asynchttpclient.Dsl;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;
import org.asynchttpclient.ws.WebSocketListener;
import org.asynchttpclient.ws.WebSocket;

/**
 * Verifies SendResult#getSession works per Spec #185
 * https://github.com/jakartaee/websocket/issues/185
 */
@RunWith(FATRunner.class)
public class Spec185GetSessionTest {

    @Server("sessionTestServer")
    public static LibertyServer LS;

    private static final Logger LOG = Logger.getLogger(Spec185GetSessionTest.class.getName());

    private static final String SESSION_WAR_NAME = "session";

    @BeforeClass
    public static void setUp() throws Exception {

        // Build the war app and add the dependencies
        ShrinkHelper.defaultDropinApp(LS, SESSION_WAR_NAME + ".war", "io.openliberty.wsoc.spec185");

        LS.startServer(Spec185GetSessionTest.class.getSimpleName() + ".log");
        LS.waitForStringInLog("CWWKZ0001I.* " + SESSION_WAR_NAME);
    }

    @AfterClass
    public static void tearDown() throws Exception {

        if (LS != null && LS.isStarted()) {
            LS.stopServer();
        }

    }

    /*
     * Tests that the session after the message is sent is not null
     * by comparing IDs before and after the message
     */
    @Test
    public void testSession() throws Exception {
        WebSocketUpgradeHandler.Builder upgradeHandlerBuilder = new WebSocketUpgradeHandler.Builder();
        WebSocketUpgradeHandler wsHandler = upgradeHandlerBuilder
                .addWebSocketListener(new WebSocketListener() {
                    @Override
                    public void onOpen(WebSocket websocket) {
                        // WebSocket connection opened
                        LOG.info("Opened Websocket");
                    }

                    @Override
                    public void onClose(WebSocket websocket, int code, String reason) {
                        // WebSocket connection closed
                        LOG.info("Closed Websocket");                       
                    }

                    @Override
                    public void onError(Throwable t) {
                        // WebSocket connection error
                        LOG.info("Session Error Occurred: " + t);
                    }

                    @Override
                    public void onTextFrame(String payload, boolean finalFragment, int rsv){
                        // Log message
                        LOG.info("Debugging: " + payload);
                    }
                }).build();

        WebSocket webSocketClient = Dsl.asyncHttpClient()
                .prepareGet("ws://" + 
                            LS.getHostname() + ":" + 
                            LS.getHttpDefaultPort() + "/" +
                            SESSION_WAR_NAME +
                            "/echo")
                .setRequestTimeout(5000)
                .execute(wsHandler)
                .get();
        
        if (webSocketClient.isOpen()) {
            LOG.info("sending message");
            webSocketClient.sendTextFrame("test message");
        }

        String msgSession = LS.waitForStringInLog("MSG SESSION: ", LS.getConsoleLogFile());
        String resultSession = LS.waitForStringInLog("RESULT SESSION: ", LS.getConsoleLogFile());
        assertNotNull("The following String was not found in the log: ", msgSession);
        assertNotNull("The following String was not found in the log: ", resultSession);
        LOG.info(msgSession);
        LOG.info(resultSession);
        assertTrue("The Session ID's were not the same.", msgSession.substring(msgSession.indexOf(": ")).equals(resultSession.substring(resultSession.indexOf(": "))));
    }

}
