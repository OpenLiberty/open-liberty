/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package session.cache.web;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.junit.Test;

import componenttest.app.FATServlet;
import junit.framework.Assert;

@SuppressWarnings("serial")
@WebServlet("/SessionCacheTestServlet")
public class SessionCacheTestServlet extends FATServlet {

    @Resource(lookup = "jdbc/SessionDS")
    DataSource dataSource;

    /**
     * Evict the active session from memory, if any.
     */
    public void evictSession(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        // We've configured the server to only hold a single session in memory.
        // By creating a new one, we flush the other one from memory.
        request.getSession();
    }

    /**
     * Invalidate the active session, if any.
     */
    public void invalidateSession(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    /**
     * Begin the testing of session attribute serialization. Create the
     * session and set attributes.
     */
    public void testSerialization(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        Map<String, Object> sessionMap = new HashMap<String, Object>();
        session.setAttribute("map", sessionMap);
        System.out.println("Session is: " + session);
        System.out.println("Session map is: " + sessionMap);

        // Test serialization of various types directly (setAttribute) and
        // indirectly (HashMap).

        // Boolean
        Boolean b = true;
        session.setAttribute("boolean", b);
        sessionMap.put("boolean", b);

        // AppObject
        AppObject object = new AppObject();
        session.setAttribute("appObject", object);
        sessionMap.put("appObject", object);

        // DataSource
        session.setAttribute("dataSource", dataSource);
        sessionMap.put("dataSource", dataSource);
    }

    /**
     * Complete the testing of session attribute serialization. The call to
     * getSession will require deserializing the object.
     */
    public void testSerialization_complete(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        HttpSession session = request.getSession(false);
        @SuppressWarnings("unchecked")
        Map<String, Object> sessionMap = (Map<String, Object>) session.getAttribute("map");
        System.out.println("Session is: " + session);
        System.out.println("Session map is: " + sessionMap);

        // Boolean
        Boolean b = (Boolean) session.getAttribute("boolean");
        Assert.assertTrue("direct boolean value not deserialized properly", b);
        b = (Boolean) sessionMap.get("boolean");
        Assert.assertTrue("indirect boolean value not deserialized properly", b);

        // AppObject
        AppObject object = (AppObject) session.getAttribute("appObject");
        Assert.assertNotNull("The appObject was not found in the HTTP session", object);
        Assert.assertTrue("direct AppObject not deserialized properly", object.deserialized);
        object = (AppObject) sessionMap.get("appObject");
        Assert.assertNotNull("The indirect appObject was not found in the HTTP session", object);
        Assert.assertTrue("indirect AppObject not deserialized properly", object.deserialized);

        // DataSource
        DataSource sessionDS = (DataSource) session.getAttribute("dataSource");
        Assert.assertNotNull("The dataSource was not found in the HTTP session", sessionDS);
        sessionDS.getConnection().close();
        sessionDS = (DataSource) sessionMap.get("dataSource");
        Assert.assertNotNull("The indirect dataSource was not found in the HTTP session", sessionDS);
        sessionDS.getConnection().close();
    }

}
