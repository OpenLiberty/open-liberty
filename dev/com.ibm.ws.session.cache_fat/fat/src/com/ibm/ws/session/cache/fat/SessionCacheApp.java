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

    public String invokeServlet(String testName, List<String> session) throws Exception {
        return FATSuite.run(s, APP_NAME + '/' + SERVLET_NAME, testName, session);
    }

    public String invalidateSession(List<String> session) throws Exception {
        return FATSuite.run(s, APP_NAME + '/' + SERVLET_NAME, "invalidateSession", session);
    }

    /**
     * @return the id of the session into which the session property was put
     */
    public String sessionPut(String key, String value, List<String> session, boolean createSession) throws Exception {
        String response = invokeServlet("sessionPut&key=" + key + "&value=" + value + "&createSession=" + createSession, session);
        int start = response.indexOf("session id: [") + 13;
        return response.substring(start, response.indexOf(']', start));
    }

    public void sessionGet(String key, String expectedValue, List<String> session) throws Exception {
        invokeServlet("sessionGet&key=" + key + "&expectedValue=" + expectedValue, session);
    }

}
