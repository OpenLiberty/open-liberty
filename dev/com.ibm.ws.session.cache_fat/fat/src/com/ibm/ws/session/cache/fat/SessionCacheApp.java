/**
 *
 */
package com.ibm.ws.session.cache.fat;

import java.util.List;
import java.util.Objects;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServer;

public class SessionCacheApp {

    static final String APP_NAME = "sessionCacheApp";
    static final String SERVLET_NAME = "SessionCacheTestServlet";
    final LibertyServer s;

    public SessionCacheApp(LibertyServer s) throws Exception {
        Objects.requireNonNull(s);
        this.s = s;
        ShrinkHelper.defaultDropinApp(s, APP_NAME, "session.cache.web");
    }

    public void invokeServlet(String testName, List<String> session) throws Exception {
        FATSuite.run(s, APP_NAME + '/' + SERVLET_NAME, testName, session);
    }

    public void invalidateSession(List<String> session) throws Exception {
        FATSuite.run(s, APP_NAME + '/' + SERVLET_NAME, "invalidateSession", session);
    }

    public void sessionPut(String key, String value, List<String> session, boolean createSession) throws Exception {
        invokeServlet("sessionPut&key=" + key + "&value=" + value + "&createSession=" + createSession, session);
    }

    public void sessionGet(String key, String expectedValue, List<String> session) throws Exception {
        invokeServlet("sessionGet&key=" + key + "&expectedValue=" + expectedValue, session);
    }

}
