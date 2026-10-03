/*******************************************************************************
 * Copyright 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.wsat.virtualhost.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.transaction.fat.util.FATUtils;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * FAT-3: Tests that the WS-AT EPR URLs published by the server use the
 * virtualHost port, not the default-host port.
 *
 * FAT-3a: WSATConfigServiceImpl logs that it registered the virtualHostRef
 *         variable as "wsatHost". Proves the correct virtual host was picked up
 *         from server.xml for WAB binding and EPR URL construction.
 *
 * FAT-3b: Liberty logs CWWKT0016I when the WS-AT WAB becomes available,
 *         including the full URL it is published on. Assert that this URL
 *         contains the virtualHost (secondary) port, directly proving that the
 *         EPR base URL Liberty would advertise to transaction partners uses
 *         the correct port.
 */
@RunWith(FATRunner.class)
public class VirtualHostEprTest {

    private static final LibertyServer server =
        LibertyServerFactory.getLibertyServer("WsatVhostServer");

    @BeforeClass
    public static void setUp() throws Exception {
        FATUtils.startServers(server);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        FATUtils.stopServers(server);
    }

    /**
     * FAT-3a: Confirm WSATConfigServiceImpl registered the virtualHostRef variable
     * as "wsatHost" during server start. This proves the correct virtual host was
     * picked up from server.xml and stored for WAB binding and EPR URL construction.
     *
     * The trace string is emitted by WSATConfigServiceImpl.registerVirtualHostVariable()
     * at debug level when wsat=all tracing is active (set in bootstrap.properties).
     */
    @Test
    public void testVirtualHostRefRegisteredInTrace() throws Exception {
        String traceMsg = server.waitForStringInTrace(
            "Registered variable: wsat\\.webservice\\.virtualHostRef=wsatHost");
        assertNotNull(
            "Expected trace line confirming wsat.webservice.virtualHostRef=wsatHost was registered",
            traceMsg);
    }

    /**
     * FAT-3b: Confirm that Liberty's CWWKT0016I "Web application available" message
     * for the WS-AT WAB contains the virtualHost (secondary) port, not the
     * default-host port. This message is emitted by Liberty's VirtualHostImpl when
     * the WAB is bound and its URL is constructed — it is the URL that Liberty uses
     * as the base for WS-AT EPRs published to transaction partners.
     */
    @Test
    public void testWabUrlContainsVirtualHostPort() throws Exception {
        int wsatPort = server.getHttpSecondaryPort();
        String msg = server.waitForStringInLog(
            "CWWKT0016I.*wsatHost.*:" + wsatPort + "/ibm/wsatservice");
        assertNotNull(
            "Expected CWWKT0016I confirming WS-AT WAB published on virtualHost port " + wsatPort,
            msg);
    }
}
