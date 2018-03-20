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
package session.cache.web;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.cache.Cache;
import javax.cache.Caching;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.ibm.websphere.servlet.session.IBMSession;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/SessionCacheConfigTestServlet")
public class SessionCacheConfigTestServlet extends FATServlet {
    private static final String EOLN = String.format("%n");

    // Maximum number of nanoseconds for test to wait
    static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    /**
     * Obtains the session id for the current session and writes it to the servlet response
     */
    public void getSessionId(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String sessionId = request.getSession().getId();
        System.out.println("session id is " + sessionId);
        response.getWriter().write("session id: [" + sessionId + "]");
    }

    /**
     * Invalidate the active session, if any.
     */
    public void invalidateSession(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            System.out.println("Invalidating session: " + session.getId());
            session.invalidate();
        }
    }

    /**
     * Verify that the cache contains the specified attribute and value.
     */
    public void testCacheContents(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String sessionId = request.getParameter("sessionId");

        if (sessionId == null) {
            HttpSession session = request.getSession(false);
            sessionId = session.getId();
        }

        String attrName = request.getParameter("attribute");
        String key = sessionId + '.' + attrName;

        String expected = request.getParameter("expected");
        String type = request.getParameter("type");
        Object expectedValue = toType(type, expected);

        testCacheContents(key, expectedValue);
    }

    /**
     * Verify that the cache contains the specified attribute and value.
     */
    private void testCacheContents(String key, Object expectedValue) throws Exception {
        byte[] expectedBytes = expectedValue == null ? null : toBytes(expectedValue);

        System.out.println("testCacheContents cache entry " + key + " should have value: " + expectedValue);
        System.out.println("as a byte array, this is: " + Arrays.toString(expectedBytes));

        // need to use same config file as server.xml
        String hazelcastConfigLoc = InitialContext.doLookup("hazelcast/configlocation");
        System.setProperty("hazelcast.config", hazelcastConfigLoc);

        Cache<String, byte[]> cache = Caching.getCache("com.ibm.ws.session.attr.default_host%2FsessionCacheConfigApp", String.class, byte[].class);

        byte[] bytes = cache.get(key);

        assertTrue("Expected cache entry " + key + " to have value " + expectedValue + ", not " + toObject(bytes) + ". " + EOLN +
                   "Bytes expected: " + Arrays.toString(expectedBytes) + EOLN +
                   "Bytes observed: " + Arrays.toString(bytes),
                   Arrays.equals(expectedBytes, bytes));
    }

    /**
     * Use IBMSession.sync to request a manual update of the persistent store and verify that the update is made immediately.
     */
    public void testManualUpdate(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String attrName = request.getParameter("attribute");

        String stringValue = request.getParameter("value");
        String type = request.getParameter("type");
        Object value = toType(type, stringValue);

        HttpSession session = request.getSession(true);
        session.setAttribute(attrName, value);

        String key = session.getId() + '.' + attrName;
        testCacheContents(key, null);

        ((IBMSession) session).sync();

        testCacheContents(key, value);
    }

    /**
     * Convert an object to the bytes that we would expect to find for it in the cache
     */
    private static final byte[] toBytes(Object o) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(o);
            return bos.toByteArray();
        }
    }

    /**
     * Converts bytes to an object
     */
    private static final Object toObject(byte[] b) {
        if (b == null)
            return null;
        ByteArrayInputStream bin = new ByteArrayInputStream(b);
        try (ObjectInputStream oin = new ObjectInputStream(bin)) {
            return oin.readObject();
        } catch (Throwable x) {
            return "[unable to deserialze due to " + x + "]";
        }
    }

    /**
     * Convert a String value to the specified type.
     * This is valid for the primitive wrapper classes (such as java.lang.Integer)
     * and any other type that has a single argument String constructor.
     */
    private static Object toType(String type, String s) throws Exception {
        if (s == null || "null".equals(s))
            return null;

        if (type == null)
            return s;

        if (Character.class.getName().equals(type))
            return s.charAt(0);

        return Class.forName(type).getConstructor(String.class).newInstance(s);
    }
}
