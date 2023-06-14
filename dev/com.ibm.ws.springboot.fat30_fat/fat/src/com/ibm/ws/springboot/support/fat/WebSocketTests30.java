/*******************************************************************************
 * Copyright (c) 2018,2023 IBM Corporation and others.
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class WebSocketTests30 extends AbstractSpringTests {

    private WebSocketContainer wsContainer;
    private WebSocketTests30EndpointEcho clientEndpoint;

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

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-3.0", "servlet-6.0", "websocket-2.1"));
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_30_APP_WEBSOCKET;
    }

}