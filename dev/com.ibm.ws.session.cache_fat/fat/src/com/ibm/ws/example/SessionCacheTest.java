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
package com.ibm.ws.example;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;
import session.cache.web.SessionCacheTestServlet;

@RunWith(FATRunner.class)
public class SessionCacheTest extends FATServletClient {

    public static final String APP_NAME = "sessionCacheApp";

    @Server("sessionCacheServer")
    @TestServlet(servlet = SessionCacheTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "session.cache.web");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    /**
     * Ensure that various types of objects can be stored in a session,
     * serialized when the session is evicted from memory, and deserialized
     * when the session is accessed again.
     */
    @Test
    public void testSerialization() throws Exception {
        List<String> session = new ArrayList<>();
        run("testSerialization", session);
        try {
            evictSession();
            run("testSerialization_complete", session);
        } finally {
            invalidateSession(session);
        }
    }

    private void invalidateSession(List<String> session) throws Exception {
        run("invalidateSession", session);
    }

    private void evictSession() throws Exception {
        run("evictSession", null);
    }

    private void run(String testMethod, List<String> session) throws Exception {
        HttpURLConnection con = HttpUtils.getHttpConnection(server, APP_NAME + "/SessionCacheTestServlet?" + FATServletClient.TEST_METHOD + '=' + testMethod);

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
