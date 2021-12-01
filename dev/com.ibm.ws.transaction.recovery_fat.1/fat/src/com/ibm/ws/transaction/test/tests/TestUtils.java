/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 *
 */
public class TestUtils {

    private static final FATServletClient fsc = new FATServletClient();

    public static final int LOG_SEARCH_TIMEOUT = 300000;

    public static void recoveryTest(LibertyServer server, String servletName, String id) throws Exception {
        recoveryTest(server, server, servletName, id);
    }

    public static void recoveryTest(LibertyServer crashingServer, LibertyServer recoveringServer, String servletName, String id) throws Exception {
        final String method = "recoveryTest";

        try {
            // We expect this to fail since it is gonna crash the server
            FATServletClient.runTest(crashingServer, servletName, "setupRec" + id);
            restartServer(crashingServer);
            fail(crashingServer.getServerName() + " did not crash as expected");
        } catch (Exception e) {
            Log.info(TestUtils.class, method, "setupRec" + id + " crashed as expected");
        }

        assertNotNull(crashingServer.getServerName() + " didn't crash properly", crashingServer.waitForStringInLog("Dump State:"));

        crashingServer.postStopServerArchive(); // must explicitly collect since server start failed

        FATUtils.startServers(recoveringServer);

        // Server appears to have started ok
        assertNotNull(recoveringServer.getServerName() + " didn't recover properly",
                      recoveringServer.waitForStringInTrace("Performed recovery for " + recoveringServer.getServerName()));

        int attempt = 0;
        while (true) {
            Log.info(TestUtils.class, method, "calling checkRec" + id);
            try {
                final StringBuilder sb = fsc.runTestWithResponse(recoveringServer, servletName, "checkRec" + id);
                Log.info(TestUtils.class, method, "checkRec" + id + " returned: " + sb);
                break;
            } catch (Exception e) {
                Log.error(TestUtils.class, method, e);
                if (++attempt < 5) {
                    Thread.sleep(10000);
                } else {
                    // Something is seriously wrong with this server instance. Reset so the next test has a chance
                    restartServer(recoveringServer);
                    throw e;
                }
            }
        }
    }

    private static void restartServer(LibertyServer s) {
        try {
            FATUtils.stopServers(new String[] { ".*" }, s);
            FATUtils.startServers(s);
            s.printProcessHoldingPort(s.getHttpDefaultPort());
        } catch (Exception e) {
            Log.error(TestUtils.class, "restartServer", e);
        }
    }

}