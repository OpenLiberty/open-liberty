/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.ws.gvt.rest.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.net.HttpURLConnection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class GvtTest extends BaseTestCase {

    @Server("com.ibm.gvt.server")
    public static LibertyServer server;
    private String contentString;

    @Before
    public void before() throws Exception {
        server.startServer();
        waitForDefaultHttpsEndpoint(server);
    }

    @After
    public void after() throws Exception {

        stopServer(server);
    }

    /**
     * GVT for Unicode compliance of JMXConnector REST api - Create Notification Registry.
     *
     * @throws Exception if there was an unforeseen error getting the certificates.
     */
    @Test
    public void testUnicodeForNotification() throws Exception {
        final String methodName = "testUnicodeForNotification()";

        contentString = HttpUtils.postRequest(server, ENDPOINT_NOTIFICATION, 200, "application/json", USER1_NAME, USER1_PASSWORD,
                                              "application/json",
                                              DELIVERY_INTERVAL);
        /*
         * Check response is empty or not.
         */
        assertFalse("Empty response returned while creating Notification registry:", contentString.isEmpty());
        Log.info(GvtTest.class, methodName, "HTTP post response contents: \n" + contentString);
    }

    /**
     * GVT for Unicode compliance of JMXConnector REST api - Retrieves list of MBeans by filtered Query Expression.
     *
     * @throws Exception if there was an unforeseen error getting the certificates.
     */
    @Test
    public void testUnicodeForMbeans() throws Exception {
        final String methodName = "testUnicodeForMbeans()";

        contentString = HttpUtils.postRequest(server, ENDPOINT_MBEAN, 200, "application/json", USER1_NAME, USER1_PASSWORD,
                                              "application/json",
                                              CLASS_NAME);
        /*
         * Check response is empty or not.
         */
        assertFalse("Empty response returned while fetching mbean :", contentString.isEmpty());
        Log.info(GvtTest.class, methodName, "HTTP post response contents: \n" + contentString);
    }

    @Test
    public void testUTF8() throws Exception {

        HttpURLConnection con = HttpUtils.getHttpConnectionForUTF(server);

        assertEquals(200, con.getResponseCode());

        assertEquals("text/html; charset=UTF-8", con.getHeaderField("Content-Type"));

    }
}
