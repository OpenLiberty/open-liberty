/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
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
package io.openliberty.checkpoint.fat;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.junit.Assert;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

public class SessionDatabaseApp {

    static final String APP_NAME = "sessionCacheApp";
    static final String SERVLET_NAME = "SessionDatabaseTestServlet";
    final LibertyServer s;

    public SessionDatabaseApp(LibertyServer s, boolean isDropinApp, String... packages) throws Exception {
        Objects.requireNonNull(s);
        this.s = s;
        if (isDropinApp)
            ShrinkHelper.defaultDropinApp(s, APP_NAME, packages);
        else
            ShrinkHelper.defaultApp(s, APP_NAME, packages);
    }

    public String invokeServlet(String testName, List<String> session) throws Exception {
        return run(s, APP_NAME + '/' + SERVLET_NAME, testName, session);
    }

    public String invalidateSession(List<String> session) throws Exception {
        return run(s, APP_NAME + '/' + SERVLET_NAME, "invalidateSession", session);
    }

    /**
     * @param <T>
     * @return the id of the session into which the session property was put
     */
    public <T> String sessionPut(String key, T value, List<String> session, boolean createSession) throws Exception {
        String type = value == null ? String.class.getName() : value.getClass().getName();
        String response = invokeServlet("sessionPut&key=" + key + "&value=" + value + "&type=" + type + "&createSession=" + createSession, session);
        int start = response.indexOf("session id: [") + 13;
        return response.substring(start, response.indexOf(']', start));
    }

    public <T> void sessionGet(String key, T expectedValue, List<String> session) throws Exception {
        String type = expectedValue == null ? String.class.getName() : expectedValue.getClass().getName();
        invokeServlet("sessionGet&key=" + key + "&expectedValue=" + expectedValue + "&type=" + type, session);
    }

    public static String run(LibertyServer server, String path, String testMethod, List<String> session) throws Exception {
        HttpURLConnection con = HttpUtils.getHttpConnection(server, path + '?' + FATServletClient.TEST_METHOD + '=' + testMethod);
        Log.info(FATSuite.class, "run", "HTTP GET: " + con.getURL());

        if (session != null)
            for (String cookie : session)
                con.addRequestProperty("Cookie", cookie);

        con.connect();
        try {
            String servletResponse = HttpUtils.readConnection(con);

            if (servletResponse == null || !servletResponse.contains(FATServletClient.SUCCESS))
                Assert.fail("Servlet call was not successful: " + servletResponse);

            if (session != null) {
                List<String> setCookies = con.getHeaderFields().get("Set-Cookie");
                if (setCookies != null) {
                    session.clear();
                    for (String setCookie : setCookies)
                        session.add(setCookie.split(";", 2)[0]);
                }
            }

            return servletResponse;
        } finally {
            con.disconnect();
        }
    }

    /**
     * Checks if multicast should be disabled in Hazelcast. We want to disable multicast on z/OS,
     * and when the environment variable disable_multicast_in_fats=true.
     *
     * If you are seeing a lot of NPE errors while running this FAT bucket you might need to set
     * disable_multicast_in_fats to true. This has been needed on some personal Linux systems, as
     * well as when running through a VPN.
     *
     * @return true if multicast should be disabled.
     */
    public static boolean isMulticastDisabled() {
        boolean multicastDisabledProp = Boolean.parseBoolean(System.getenv("disable_multicast_in_fats"));
        String osName = System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT);

        return (multicastDisabledProp || osName.contains("z/os"));
    }
}
