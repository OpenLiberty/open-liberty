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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamConstants;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.management.CacheMXBean;
import javax.cache.management.CacheStatisticsMXBean;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
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
     * Utility method to obtain the cache manager instance of the CacheStoreService
     */
    private CacheManager getCacheManager() throws Exception {
        Class<?> c = Thread.currentThread().getContextClassLoader().getClass();
        ClassLoader cl = c.getClassLoader();

        Class<?> FrameworkUtil = cl.loadClass("org.osgi.framework.FrameworkUtil");
        Class<?> ServiceReference = cl.loadClass("org.osgi.framework.ServiceReference");

        Object bundle = FrameworkUtil
                        .getMethod("getBundle", Class.class)
                        .invoke(null, c);
        Object bundleContext = bundle.getClass()
                        .getMethod("getBundleContext")
                        .invoke(bundle);
        Object ref = bundleContext.getClass()
                        .getMethod("getServiceReference", String.class)
                        .invoke(bundleContext, "com.ibm.ws.session.SessionStoreService");
        Object cacheStoreService = bundleContext.getClass()
                        .getMethod("getService", ServiceReference)
                        .invoke(bundleContext, ref);
        try {
            Field f = cacheStoreService.getClass().getDeclaredField("cacheManager");
            f.setAccessible(true);
            return (CacheManager) f.get(cacheStoreService);
        } finally {
            bundleContext.getClass()
                            .getMethod("ungetService", ServiceReference)
                            .invoke(bundleContext, ref);
        }
    }

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
    public void invalidateSession(HttpServletRequest request, HttpServletResponse response) throws IOException {
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

        boolean useURI = Boolean.parseBoolean(request.getParameter("useURI"));

        if (useURI)
            testCacheViaURIContains(key, expectedValue);
        else
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
     * Verify that the cache contains the specified attribute and value.
     */
    private void testCacheViaURIContains(String key, Object expectedValue) throws Exception {
        byte[] expectedBytes = expectedValue == null ? null : toBytes(expectedValue);

        System.out.println("testCacheContains cache entry " + key + " should have value: " + expectedValue);
        System.out.println("as a byte array, this is: " + Arrays.toString(expectedBytes));

        CacheManager cacheManager = getCacheManager();
        Cache<String, byte[]> cache = cacheManager.getCache("com.ibm.ws.session.attr.default_host%2FsessionCacheConfigApp", String.class, byte[].class);
        byte[] bytes = cache.get(key);

        assertTrue("Expected cache entry " + key + " to have value " + expectedValue + ", not " + toObject(bytes) + ". " + EOLN +
                   "Bytes expected: " + Arrays.toString(expectedBytes) + EOLN +
                   "Bytes observed: " + Arrays.toString(bytes),
                   Arrays.equals(expectedBytes, bytes));
    }

    /**
     * Set the value of a session attribute.
     */
    public void testGetAttribute(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String attrName = request.getParameter("attribute");

        String stringValue = request.getParameter("value");
        String type = request.getParameter("type");
        Object value = toType(type, stringValue);

        HttpSession session = request.getSession(false);
        assertEquals(value, session.getAttribute(attrName));
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
     * Verify that CacheMXBean and CacheStatisticsMXBean provided for each of the caches created by the sessionCache feature
     * can be obtained and report statistics about the cache.
     */
    public void testMXBeansEnabled(HttpServletRequest request, HttpServletResponse response) throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        // CacheMXBean for session meta info cache
        CacheMXBean metaInfoCacheMXBean = //
                        JMX.newMBeanProxy(mbs,
                                          new ObjectName("javax.cache:type=CacheConfiguration,CacheManager=hazelcast,Cache=com.ibm.ws.session.meta.default_host%2FsessionCacheConfigApp"),
                                          CacheMXBean.class);
        assertEquals(String.class.getName(), metaInfoCacheMXBean.getKeyType());
        assertEquals(ArrayList.class.getName(), metaInfoCacheMXBean.getValueType());
        assertTrue(metaInfoCacheMXBean.isManagementEnabled());
        assertTrue(metaInfoCacheMXBean.isStatisticsEnabled());

        // CacheMXBean for session attributes cache
        CacheMXBean attrCacheMXBean = //
                        JMX.newMBeanProxy(mbs,
                                          new ObjectName("javax.cache:type=CacheConfiguration,CacheManager=hazelcast,Cache=com.ibm.ws.session.attr.default_host%2FsessionCacheConfigApp"),
                                          CacheMXBean.class);
        assertEquals(String.class.getName(), attrCacheMXBean.getKeyType());
        assertEquals("[B", attrCacheMXBean.getValueType()); // byte[]
        assertTrue(attrCacheMXBean.isManagementEnabled());
        assertTrue(attrCacheMXBean.isStatisticsEnabled());

        // CacheStatisticsMXBean for session meta info cache
        CacheStatisticsMXBean metaInfoCacheStatsMXBean = //
                        JMX.newMBeanProxy(mbs,
                                          new ObjectName("javax.cache:type=CacheStatistics,CacheManager=hazelcast,Cache=com.ibm.ws.session.meta.default_host%2FsessionCacheConfigApp"),
                                          CacheStatisticsMXBean.class);
        metaInfoCacheStatsMXBean.clear();
        assertEquals(0, metaInfoCacheStatsMXBean.getCacheEvictions());

        // CacheStatisticsMXBean for session attributes cache
        CacheStatisticsMXBean attrCacheStatsMXBean = //
                        JMX.newMBeanProxy(mbs,
                                          new ObjectName("javax.cache:type=CacheStatistics,CacheManager=hazelcast,Cache=com.ibm.ws.session.attr.default_host%2FsessionCacheConfigApp"),
                                          CacheStatisticsMXBean.class);
        assertEquals(0, attrCacheStatsMXBean.getCacheRemovals());

        HttpSession session = request.getSession();
        request.getSession().setAttribute("testMXBeans", 12.3f);
        ((IBMSession) session).sync();

        request.getSession().removeAttribute("testMXBeans");
        ((IBMSession) session).sync();

        // cannot check the value because the JCache provider might not immediately update the statistics
        attrCacheStatsMXBean.getCacheRemovals();

        session.invalidate();
    }

    /**
     * Verify that CacheMXBean and CacheStatisticsMXBean are not registered.
     */
    public void testMXBeansNotEnabled(HttpServletRequest request, HttpServletResponse response) throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        Set<ObjectName> found = mbs.queryNames(new ObjectName("javax.cache:*"), null);
        assertEquals(found.toString(), 0, found.size());
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
     * Verify that sessions are not available, even if requesting a new session
     */
    public void testSessionCacheNotAvailable(HttpServletRequest request, HttpServletResponse response) throws Exception {
        assertNull("Should not be able to obtain session", request.getSession(true));
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
     * Set the value of a session attribute.
     * Precondition: in order for the test logic to be valid, the session attribute must not already have the same value.
     */
    public void testSetAttributeOnly(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String attrName = request.getParameter("attribute");

        String stringValue = request.getParameter("value");
        String type = request.getParameter("type");
        Object value = toType(type, stringValue);

        HttpSession session = request.getSession(true);
        session.setAttribute(attrName, value);
    }

    /**
     * Set the value of a session attribute and specify a maxInactiveInterval for the session.
     */
    public void testSetAttributeWithTimeout(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String attrName = request.getParameter("attribute");

        String stringValue = request.getParameter("value");
        String type = request.getParameter("type");
        Object value = toType(type, stringValue);

        int maxInactiveInterval = Integer.parseInt(request.getParameter("maxInactiveInterval"));

        HttpSession session = request.getSession(true);
        String sessionId = session.getId();
        System.out.println("session id is " + sessionId);
        response.getWriter().write("session id: [" + sessionId + "]");

        session.setAttribute(attrName, value);

        session.setMaxInactiveInterval(maxInactiveInterval);
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
        if (o instanceof Character) {
            byte[] bytes = new byte[] { 0, 6, 0, 0 };
            char value = (char) o;
            for (int i = 2 + 1; i >= 2; --i) {
                bytes[i] = (byte) value;
                value = (char) ((value) >> 8);
            }
            return bytes;
        } else if (o instanceof Integer) {
            byte[] bytes = new byte[] { 0, 1, 0, 0, 0, 0, 0 };
            int pos = 2;
            int numWritten = 0;
            int v = ((Integer) o).intValue();

            if ((v & ~0x7F) == 0) {
                bytes[pos++] = ((byte) v);
                numWritten = 3;
            } else {
                while (true) {
                    if ((v & ~0x7F) == 0) {
                        bytes[pos++] = ((byte) v);
                        numWritten = pos;
                        break;
                    } else {
                        bytes[pos++] = (byte) ((v & 0x7F) | 0x80);
                        v >>>= 7;
                    }
                }
            }
            if (numWritten < 7) {
                byte[] smallArray = new byte[numWritten];
                System.arraycopy(bytes, 0, smallArray, 0, numWritten);
                return smallArray;
            }
            return bytes;
        } else if (o instanceof Byte) {
            return new byte[] { 0, 3, ((Byte) o).byteValue() };
        } else if (o instanceof Boolean) {
            final byte[] TRUE_BYTES = { 0, 7, 1 };
            final byte[] FALSE_BYTES = { 0, 7, 0 };
            return ((Boolean) o).booleanValue() ? TRUE_BYTES : FALSE_BYTES;
        }
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

        final byte[] OBJECT_OUTPUT_STREAM_HEADER = new byte[] {
                                                                (byte) (ObjectStreamConstants.STREAM_MAGIC >>> 8),
                                                                (byte) (ObjectStreamConstants.STREAM_MAGIC >>> 0)
        };

        if (b.length >= 4
            && b[0] == OBJECT_OUTPUT_STREAM_HEADER[0]
            && b[1] == OBJECT_OUTPUT_STREAM_HEADER[1]) {
            //This was serialized using the standard method, deserialize with readObject
            ByteArrayInputStream bin = new ByteArrayInputStream(b);
            try (ObjectInputStream oin = new ObjectInputStream(bin)) {
                return oin.readObject();
            } catch (Throwable x) {
                return "[unable to deserialze due to " + x + "]";
            }
        } else if (b[0] == 0) {
            //This was written directly to bytes, so read directly from bytes
            if (b[1] == 1) {
                //integer
                int offset = 2;
                byte tmp = b[offset++];
                if (tmp >= 0) {
                    return Integer.valueOf(tmp);
                }
                int result = tmp & 0x7f;
                if ((tmp = b[offset++]) >= 0) {
                    result |= tmp << 7;
                } else {
                    result |= (tmp & 0x7f) << 7;
                    if ((tmp = b[offset++]) >= 0) {
                        result |= tmp << 14;
                    } else {
                        result |= (tmp & 0x7f) << 14;
                        if ((tmp = b[offset++]) >= 0) {
                            result |= tmp << 21;
                        } else {
                            result |= (tmp & 0x7f) << 21;
                            result |= (tmp = b[offset++]) << 28;
                            if (tmp < 0) {
                                // Discard upper 32 bits.
                                for (int i = 0; i < 5; i++) {
                                    if (b[offset++] >= 0) {
                                        return Integer.valueOf(result);
                                    }
                                }
                                return "[unable to deserialze due to Varint representation is invalid or exceeds 32-bit value]";
                            }
                        }
                    }
                }
            } else if (b[1] == 6) {
                //character
                char result = 0x0;
                for (int i = 2; i < 2 + 2; ++i) {
                    result = (char) ((result) << 8);
                    result |= (b[i] & 0x00FF);
                }
                return Character.valueOf(result);
            } else if (b[1] == 3) {
                //byte
                return Byte.valueOf(b[2]);
            } else if (b[1] == 7) {
                return b[2] == 0 ? Boolean.FALSE : Boolean.TRUE;
            } else {
                return "[unable to deserialze due to unexpected primitive type " + b[1] + ".  Need to add support for reading bytes of this type in the test toObject method]";
            }

        }
        return "[unable to deserialze due to invalid stream header]";
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
