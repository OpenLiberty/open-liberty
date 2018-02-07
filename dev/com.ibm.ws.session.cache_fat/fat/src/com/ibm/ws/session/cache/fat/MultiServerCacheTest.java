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
package com.ibm.ws.session.cache.fat;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class MultiServerCacheTest extends FATServletClient {

    public static final String APP_NAME = "multiServerApp";

    @Server("sessionCacheServerA")
    public static LibertyServer serverA;

    @Server("sessionCacheServerB")
    public static LibertyServer serverB;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(serverA, APP_NAME, "session.multiserver.web");
        ShrinkHelper.defaultDropinApp(serverB, APP_NAME, "session.multiserver.web");
        serverB.useSecondaryHTTPPort();

        serverA.startServer();
        serverB.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            serverA.stopServer();
        } finally {
            serverB.stopServer();
        }
    }

    /**
     * Ensure that various types of objects can be stored in a session,
     * serialized when the session is evicted from memory, and deserialized
     * when the session is accessed again.
     */
    @AllowedFFDC("java.lang.UnsupportedOperationException") // TODO remove once we implement performInvalidation and possibly other methods
    @Test
    public void testSerialization() throws Exception {
        List<String> session = new ArrayList<>();
        run(serverA, "testSerialization", session);
        run(serverB, "testSerialization_complete", session);
    }

    private void run(LibertyServer server, String testMethod, List<String> session) throws Exception {
        HttpURLConnection con = HttpUtils.getHttpConnection(server, APP_NAME + "/MultiServerTestServlet?" + FATServletClient.TEST_METHOD + '=' + testMethod);

        if (session != null) {
            for (String cookie : session) {
                con.addRequestProperty("Cookie", cookie);
            }
        }

        con.connect();
        try {
            String servletResponse = HttpUtils.readConnection(con);

            if (servletResponse == null || !servletResponse.contains(FATServletClient.SUCCESS))
                Assert.fail("Servlet call was not successful: " + servletResponse);

            if (session != null) {
                List<String> setCookies = con.getHeaderFields().get("Set-Cookie");
                if (setCookies != null) {
                    session.clear();
                    for (String setCookie : setCookies) {
                        session.add(setCookie.split(";", 2)[0]);
                    }
                }
            }
        } finally {
            con.disconnect();
        }
    }
}
