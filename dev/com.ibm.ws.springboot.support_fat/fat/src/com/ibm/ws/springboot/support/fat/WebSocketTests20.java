/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class WebSocketTests20 extends AbstractSpringTests {

    private WebSocketContainer wsContainer;
    private ClientEchoWebSocketEndpoint clientEndpoint;

    @Before
    public void setUp() throws Exception {
        wsContainer = ContainerProvider.getWebSocketContainer();
        clientEndpoint = new ClientEchoWebSocketEndpoint();
    }

    @Test
    public void testEchoWebSocket20() throws Exception {
        Log.info(getClass(), "testWebSocket20", wsContainer.toString());
        Session session = wsContainer.connectToServer(clientEndpoint, new URI("ws://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/echo"));
        assertNotNull("Session cannot be null", session);
        assertTrue("Session is not open", session.isOpen());
        CountDownLatch latch = new CountDownLatch(1);
        clientEndpoint.sendMessage("Hello World", latch);
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("Expected message from server not found", "Did you say: Hello World", clientEndpoint.getMessageFromServer());
    }

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-2.0", "servlet-3.1", "websocket-1.1"));
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_20_APP_WEBSOCKET;
    }

}