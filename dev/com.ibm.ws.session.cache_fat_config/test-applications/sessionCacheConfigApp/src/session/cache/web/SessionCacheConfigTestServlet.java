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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedList;
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
     * Gets the current value of an attribute from the cache and writes it to the servlet response
     */
    public void getValueFromCache(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String sessionId = request.getParameter("sessionId");

        if (sessionId == null) {
            HttpSession session = request.getSession(false);
            sessionId = session.getId();
        }

        String attrName = request.getParameter("attribute");
        String key = sessionId + '.' + attrName;

        // need to use same config file as server.xml
        String hazelcastConfigLoc = InitialContext.doLookup("hazelcast/configlocation");
        System.setProperty("hazelcast.config", hazelcastConfigLoc);

        byte[] bytes;
        Cache<String, byte[]> cache = Caching.getCache("com.ibm.ws.session.attr.default_host%2FsessionCacheConfigApp", String.class, byte[].class);
        try {
            bytes = cache.get(key);
        } finally {
            cache.close();
        }

        Object value = toObject(bytes);

        System.out.println("Found value of " + value + " in the cache. As bytes, this is " + EOLN + Arrays.toString(bytes));

        response.getWriter().write("value from cache: [" + value + "]");
    }

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
    public void testCacheContains(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String sessionId = request.getParameter("sessionId");

        if (sessionId == null) {
            HttpSession session = request.getSession(false);
            sessionId = session.getId();
        }

        String attrName = request.getParameter("attribute");
        String key = sessionId + '.' + attrName;

        String expected = request.getParameter("value");
        String type = request.getParameter("type");
        Object expectedValue = toType(type, expected);

        testCacheContains(key, expectedValue);
    }

    /**
     * Verify that the cache contains the specified attribute and value.
     */
    private void testCacheContains(String key, Object expectedValue) throws Exception {
        byte[] expectedBytes = expectedValue == null ? null : toBytes(expectedValue);

        System.out.println("testCacheContains cache entry " + key + " should have value: " + expectedValue);
        System.out.println("as a byte array, this is: " + Arrays.toString(expectedBytes));

        // need to use same config file as server.xml
        String hazelcastConfigLoc = InitialContext.doLookup("hazelcast/configlocation");
        System.setProperty("hazelcast.config", hazelcastConfigLoc);

        byte[] bytes;
        Cache<String, byte[]> cache = Caching.getCache("com.ibm.ws.session.attr.default_host%2FsessionCacheConfigApp", String.class, byte[].class);
        try {
            bytes = cache.get(key);
        } finally {
            cache.close();
        }

        assertTrue("Expected cache entry " + key + " to have value " + expectedValue + ", not " + toObject(bytes) + ". " + EOLN +
                   "Bytes expected: " + Arrays.toString(expectedBytes) + EOLN +
                   "Bytes observed: " + Arrays.toString(bytes),
                   Arrays.equals(expectedBytes, bytes));
    }

    /**
     * Verify that the cache either does not contain specified attribute or its value does not match.
     */
    private void testCacheEntryDoesNotMatch(String key, Object unexpectedValue) throws Exception {
        byte[] unexpectedBytes = unexpectedValue == null ? null : toBytes(unexpectedValue);

        System.out.println("testCacheEntryDoesNotMatch cache entry " + key + " will be checked to verify the value is not: " + unexpectedValue);
        System.out.println("as a byte array, this is: " + Arrays.toString(unexpectedBytes));

        // need to use same config file as server.xml
        String hazelcastConfigLoc = InitialContext.doLookup("hazelcast/configlocation");
        System.setProperty("hazelcast.config", hazelcastConfigLoc);

        byte[] bytes;
        Cache<String, byte[]> cache = Caching.getCache("com.ibm.ws.session.attr.default_host%2FsessionCacheConfigApp", String.class, byte[].class);
        if (cache == null) // cache can be null if test case disables the sessionCache-1.0 feature
            bytes = null;
        else
            try {
                bytes = cache.get(key);
            } finally {
                cache.close();
            }

        assertFalse("Not expecting cache entry " + key + " to have value " + unexpectedValue + ". " + EOLN +
                    "Bytes observed: " + Arrays.toString(bytes),
                    Arrays.equals(unexpectedBytes, bytes));
    }

    /**
     * Use IBMSession.sync to request a manual update of the persistent store and verify that an update that
     * was made under a previous servlet request goes into effect immediately.
     */
    public void testManualSync(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String attrName = request.getParameter("attribute");

        String stringValue = request.getParameter("value");
        String type = request.getParameter("type");
        Object value = toType(type, stringValue);

        HttpSession session = request.getSession(false);

        ((IBMSession) session).sync();

        // Verify that attribute has been persisted to the cache
        String key = session.getId() + '.' + attrName;
        testCacheContains(key, value);
    }

    /**
     * Use IBMSession.sync to request a manual update of the persistent store for an update that is made
     * within the same servlet request. Verify that the update goes into effect immediately.
     */
    public void testManualUpdate(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String attrName = request.getParameter("attribute");

        String stringValue = request.getParameter("value");
        String type = request.getParameter("type");
        Object value = toType(type, stringValue);

        HttpSession session = request.getSession(true);
        session.setAttribute(attrName, value);

        // Verify that attribute does not get persisted to the cache yet
        String key = session.getId() + '.' + attrName;
        testCacheEntryDoesNotMatch(key, value);

        ((IBMSession) session).sync();

        // Verify that attribute has been persisted to the cache
        testCacheContains(key, value);
    }

    /**
     * Poll the cache for a particular attribute value to appear.
     */
    public void testPollCache(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String sessionId = request.getParameter("sessionId");

        if (sessionId == null) {
            HttpSession session = request.getSession(false);
            sessionId = session.getId();
        }

        String attrName = request.getParameter("attribute");
        String key = sessionId + '.' + attrName;

        String expected = request.getParameter("value");
        String type = request.getParameter("type");
        Object expectedValue = toType(type, expected);

        testPollCache(key, expectedValue);
    }

    /**
     * Poll the cache for a particular attribute value to appear.
     */
    private void testPollCache(String key, Object expectedValue) throws Exception {
        byte[] expectedBytes = expectedValue == null ? null : toBytes(expectedValue);

        System.out.println("testPollCache cache entry " + key + " should eventually have value: " + expectedValue);
        System.out.println("as a byte array, this is: " + Arrays.toString(expectedBytes));

        // need to use same config file as server.xml
        String hazelcastConfigLoc = InitialContext.doLookup("hazelcast/configlocation");
        System.setProperty("hazelcast.config", hazelcastConfigLoc);

        boolean found = false;
        byte[] bytes = null;
        Cache<String, byte[]> cache = Caching.getCache("com.ibm.ws.session.attr.default_host%2FsessionCacheConfigApp", String.class, byte[].class);
        try {
            for (long start = System.nanoTime(); !found && System.nanoTime() - start < TIMEOUT_NS; TimeUnit.MILLISECONDS.sleep(500)) {
                bytes = cache.get(key);
                found = Arrays.equals(expectedBytes, bytes);
            }
        } finally {
            cache.close();
        }

        assertTrue("Expected cache entry " + key + " to have value " + expectedValue + ", not " + toObject(bytes) + ". " + EOLN +
                   "Bytes expected: " + Arrays.toString(expectedBytes) + EOLN +
                   "Bytes observed: " + Arrays.toString(bytes),
                   found);
    }

    /**
     * Set the value of a session attribute.
     * Precondition: in order for the test logic to be valid, the session attribute must not already have the same value.
     */
    public void testSetAttribute(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String attrName = request.getParameter("attribute");

        String stringValue = request.getParameter("value");
        String type = request.getParameter("type");
        Object value = toType(type, stringValue);

        HttpSession session = request.getSession(true);
        session.setAttribute(attrName, value);

        // Verify that attribute does not get persisted to the cache yet
        String key = session.getId() + '.' + attrName;
        testCacheEntryDoesNotMatch(key, value);
    }

    /**
     * Verify that all session attributes are written to the cache regardless of whether setAttribute is invoked.
     */
    public void testWriteContents_ALL_SESSION_ATTRIBUTES(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession(true);
        try {
            LinkedList<Long> list = new LinkedList<>();
            list.addAll(Arrays.asList(150l, 151l, 152l));

            session.setAttribute("asaset", false);
            session.setAttribute("asaget", new BitSet(8));
            session.setAttribute("asamod", list);

            // Write all attributes to the cache
            ((IBMSession) session).sync();

            session.setAttribute("asaset", true); // set
            ((BitSet) session.getAttribute("asaget")).flip(0, 3); // get and mutate
            list.add(153l); // mutate without get

            // Write to cache per the writeContents
            ((IBMSession) session).sync();

            // Check the cache for values expected per writeContents=ALL_SESSION_ATTRIBUTES
            BitSet expectedBits = new BitSet(8);
            expectedBits.flip(0, 3);
            String sessionId = session.getId();
            testCacheContains(sessionId + ".asaset", true);
            testCacheContains(sessionId + ".asaget", expectedBits);
            testCacheContains(sessionId + ".asamod", list);
        } finally {
            session.invalidate();
        }
    }

    /**
     * Verify that all session attributes that have been touched via getAttribute or setAttribute are written to the cache.
     */
    public void testWriteContents_GET_AND_SET_ATTRIBUTES(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession(true);
        try {
            LinkedList<Long> list = new LinkedList<>();
            list.addAll(Arrays.asList(350l, 351l, 352l));

            @SuppressWarnings("unchecked")
            LinkedList<Long> originalList = (LinkedList<Long>) list.clone();

            session.setAttribute("gsaset", (byte) 353);
            session.setAttribute("gsaget", new BitSet(8));
            session.setAttribute("gsamod", list);

            // Write all attributes to the cache
            ((IBMSession) session).sync();

            session.setAttribute("gsaset", (byte) 354); // set
            ((BitSet) session.getAttribute("gsaget")).flip(4, 7); // get and mutate
            list.add(355l); // mutate without get

            // Write to cache per the writeContents
            ((IBMSession) session).sync();

            // Check the cache for values expected per writeContents=GET_AND_SET_ATTRIBUTES
            BitSet expectedBits = new BitSet(8);
            expectedBits.flip(4, 7);
            String sessionId = session.getId();
            testCacheContains(sessionId + ".gsaset", (byte) 354); // updated
            testCacheContains(sessionId + ".gsaget", expectedBits); // updated
            testCacheContains(sessionId + ".gsamod", originalList); // not updated
        } finally {
            session.invalidate();
        }
    }

    /**
     * Verify that only attributes for which setAttribute is invoked are written to the cache.
     */
    public void testWriteContents_ONLY_SET_ATTRIBUTES(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession(true);
        try {
            LinkedList<Long> list = new LinkedList<>();
            list.addAll(Arrays.asList(250l, 251l, 252l));

            @SuppressWarnings("unchecked")
            LinkedList<Long> originalList = (LinkedList<Long>) list.clone();

            session.setAttribute("asaset", 's');
            session.setAttribute("asaget", new BitSet(8));
            session.setAttribute("asamod", list);

            // Write all attributes to the cache
            ((IBMSession) session).sync();

            session.setAttribute("asaset", 'S'); // set
            ((BitSet) session.getAttribute("asaget")).flip(2, 6); // get and mutate
            list.add(253l); // mutate without get

            // Write to cache per the writeContents
            ((IBMSession) session).sync();

            // Check the cache for values expected per writeContents=ONLY_SET_ATTRIBUTES
            String sessionId = session.getId();
            testCacheContains(sessionId + ".asaset", 'S'); // updated
            testCacheContains(sessionId + ".asaget", new BitSet(8)); // not updated
            testCacheContains(sessionId + ".asamod", originalList); // not updated
        } finally {
            session.invalidate();
        }
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
