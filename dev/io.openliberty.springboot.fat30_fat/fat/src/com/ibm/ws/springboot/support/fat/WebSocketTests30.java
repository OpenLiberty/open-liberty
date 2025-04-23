/*******************************************************************************
 * Copyright (c) 2018,2025 IBM Corporation and others.
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
package com.ibm.ws.springboot.support.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class WebSocketTests30 extends AbstractSpringTests {

    private WebSocketContainer wsContainer;
    private WebSocketTests30EndpointEcho clientEndpoint;

    // java.lang.RuntimeException: Could not find an implementation class.
    // at jakarta.websocket.ContainerProvider.getWebSocketContainer(ContainerProvider.java:59)
    //
    // https://stackoverflow.com/questions/27751630/websocket-client-could-not-find-an-implementation-class
    //   "Note that 1.x versions of Tyrus use the javax.websocket API
    //   from Java EE, while 2.x versions use jakarta.websocket from its
    //   successor Jakarta EE. So you can also get the error if you use
    //   a version Tyrus that is not compatible with the websocket API
    //   that you are using."
    //
    // Current dependencies:
    //   'org.eclipse.jetty.websocket:websocket-api:9.4.5.v20170502',
    //   'org.eclipse.jetty.websocket:websocket-common:9.4.5.v20170502',
    //   'org.eclipse.jetty:jetty-util:9.4.7.RC0',
    //   'org.eclipse.jetty:jetty-websocket:8.2.0.v20160908',
    //
    // https://mvnrepository.com/artifact/javax.websocket/javax.websocket-api
    // https://mvnrepository.com/artifact/jakarta.websocket/jakarta.websocket-api

    // https://mvnrepository.com/artifact/jakarta.websocket/jakarta.websocket-api
    // @Grapes(
    //   @Grab(group='jakarta.websocket', module='jakarta.websocket-api', version='2.1.0', scope='provided')
    // )
    // https://mvnrepository.com/artifact/jakarta.websocket/jakarta.websocket-client-api

    @Before
    public void setUp() throws Exception {
        wsContainer = ContainerProvider.getWebSocketContainer();
        clientEndpoint = new WebSocketTests30EndpointEcho();
    }

    @Test
    public void testEchoWebSocket30() throws Exception {
        Log.info(getClass(), "testWebSocket30", wsContainer.toString());
        Session session = wsContainer.connectToServer(clientEndpoint, new URI("ws://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/echo"));
        assertNotNull("Session cannot be null", session);
        assertTrue("Session is not open", session.isOpen());
        CountDownLatch latch = new CountDownLatch(1);
        clientEndpoint.sendMessage("Hello World", latch);
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("Expected message from server not found", "Did you say: Hello World", clientEndpoint.getMessageFromServer());
    }

    /**
     * Test websocket using a custom websocket configurer.
     *
     * The application registers a custom websocket handler and a abstract handshake handler.
     * The org.springframework.web.socket.server.support.AbstractHandshakeHandler looksup websphere websocket code which is now being removed in spring framework 7.x.
     * This test should fail for future Spring Boot versions using spring framework 7.x, we might have to use the websocket-2.2 feature and do some implementation changes if
     * required.
     *
     * @throws Exception
     */
    @Test
    public void testEchoWithCustomWebsocketHandler() throws Exception {
        Log.info(getClass(), "testEchoWithCustomWebsocketHandler", wsContainer.toString());
        Session session = wsContainer.connectToServer(clientEndpoint, new URI("ws://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/customHandler"));
        assertNotNull("Session cannot be null", session);
        assertTrue("Session is not open", session.isOpen());
        CountDownLatch latch = new CountDownLatch(1);
        clientEndpoint.sendMessage("Hello Websocket", latch);
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("Expected message from server not found", "Did you say: Hello Websocket", clientEndpoint.getMessageFromServer());
    }

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-3.0", "servlet-6.0", "websocket-2.1"));
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_30_APP_WEBSOCKET;
    }

    @AfterClass
    public static void stopTestServer() throws Exception {
        //[WARNING ] SRVE8094W: WARNING: Cannot set header. Response already committed.  Stack trace of errant attempt to set header:
        //at com.ibm.ws.webcontainer.srt.SRTServletResponse.setHeader(SRTServletResponse.java:1845)
        server.stopServer(true, "SRVE8094W");
    }

}
