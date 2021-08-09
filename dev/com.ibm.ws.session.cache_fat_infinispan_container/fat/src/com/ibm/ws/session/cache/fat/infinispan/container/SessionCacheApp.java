/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session.cache.fat.infinispan.container;

import java.util.List;
import java.util.Objects;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServer;

/**
 * Helper class for making requests to the running application
 */
public class SessionCacheApp {

    static final String APP_NAME = "sessionCacheApp";
    static final String SERVLET_NAME = "SessionCacheTestServlet";
    final LibertyServer s;

    /**
     * Package app and exports it to the server
     *
     * @param isDropinApp - True: Export to dropins dir. False: Export to apps dir.
     */
    public SessionCacheApp(LibertyServer s, boolean isDropinApp, String... packages) throws Exception {
        Objects.requireNonNull(s);
        this.s = s;
        if (isDropinApp)
            ShrinkHelper.defaultDropinApp(s, APP_NAME, packages);
        else
            ShrinkHelper.defaultApp(s, APP_NAME, packages);
    }

    public String invokeServlet(String testName, List<String> session) throws Exception {
        return FATSuite.run(s, APP_NAME + '/' + SERVLET_NAME, testName, session);
    }

    /**
     * Runs the InvalidateSession method.
     *
     * @see session.cache.infinispan.web.SessionCacheTestServlet#invalidateSession
     */
    public String invalidateSession(List<String> session) throws Exception {
        return FATSuite.run(s, APP_NAME + '/' + SERVLET_NAME, "invalidateSession", session);
    }

    /**
     * @param <T>
     * @return the id of the session into which the session property was put
     * @see session.cache.infinispan.web.SessionCacheTestServlet#sessionPut
     */
    public <T> String sessionPut(String key, T value, List<String> session, boolean createSession) throws Exception {
        String type = value == null ? String.class.getName() : value.getClass().getName();
        String response = invokeServlet("sessionPut&key=" + key + "&value=" + value + "&type=" + type + "&createSession=" + createSession, session);
        String findInResponse = "session id: [";
        int start = response.indexOf(findInResponse) + findInResponse.length();
        return response.substring(start, response.indexOf(']', start));
    }

    /**
     * @param <T>
     * @see session.cache.infinispan.web.SessionCacheTestServlet#sessionGet
     */
    public <T> void sessionGet(String key, T expectedValue, List<String> session) throws Exception {
        String type = expectedValue == null ? String.class.getName() : expectedValue.getClass().getName();
        invokeServlet("sessionGet&key=" + key + "&expectedValue=" + expectedValue + "&type=" + type, session);
    }

}
