/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.fat;

import static org.junit.Assert.assertNotNull;

import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/*
 * Tests the enablement of cxf.multipart.attachment property
 * in class SwAOutInterceptor
 */
@RunWith(FATRunner.class)
public class LibertyCXFPropertiesTest {

    @Server("PropertyTestServer")
    public static LibertyServer server;

    private final Class<?> c = LibertyCXFPropertiesTest.class;
    private static final int CONN_TIMEOUT = 10;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "webServiceRefFeatures", "com.ibm.ws.test.client.stub",
                                      "com.ibm.ws.test.wsfeatures.client",
                                      "com.ibm.ws.test.wsfeatures.client.handler",
                                      "com.ibm.ws.test.wsfeatures.handler",
                                      "com.ibm.ws.test.wsfeatures.service");

        server.startServer("PropertySettingTest.log");
        server.waitForStringInLog("PropertySettingTest");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testCxfMultipartAttachmentProperty() throws Exception {
        HttpURLConnection con = null;
        try {
            URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/webServiceRefFeatures/service");
            Log.info(c, "testCxfMultipartAttachmentProperty",
                     "Calling Application with URL=" + url.toString());
            con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
        } finally {
            if (con != null) {
                try {
                    con.disconnect();
                } catch (Exception e) {
                }
            }
        }
        assertNotNull("Property cxf.multipart.attachment is failed to be enabled",
                      server.waitForStringInTrace("skipAttachmentOutput: getAttachments returned"));

    }

}
