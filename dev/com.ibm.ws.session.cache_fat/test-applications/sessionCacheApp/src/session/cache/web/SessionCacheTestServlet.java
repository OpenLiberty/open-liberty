/*******************************************************************************
 * Copyright (c) 2017,2020 IBM Corporation and others.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.cache.Cache;
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
import javax.servlet.http.HttpSessionListener;
import javax.sql.DataSource;

import com.ibm.websphere.servlet.session.IBMSession;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/SessionCacheTestServlet")
public class SessionCacheTestServlet extends FATServlet {
    // Maximum number of nanoseconds for test to wait
    static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    /**
     * Evict the active session from memory, if any.
     */
    public void evictSession(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        // We've configured the server to only hold a single session in memory.
        // By creating a new one, we flush the other one from memory.
        request.getSession();
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
     * Verify that a session attribute has any of the specified values.
     */
    public void testAttributeIsAnyOf(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String key = request.getParameter("key");
        String expectedValues = request.getParameter("values");
        String type = request.getParameter("type");
        Set<Object> expected = new HashSet<Object>();
        for (String v : expectedValues.split(","))
            expected.add(toType(type, v));

        HttpSession session = request.getSession(false);
        Object actualValue = session.getAttribute(key);
        System.out.println("Got entry: " + key + '=' + actualValue + " from sessionID=" + session.getId());

        response.getWriter().write("session property value: [" + actualValue + "]");

        assertTrue("value is " + actualValue + ", was expecting any of " + expected, expected.contains(actualValue));
    }

    /**
     * Verify that the session contains the specified attribute names.
     */
    public void testAttributeNames(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String[] expectedAttributes = request.getParameter("sessionAttributes").split(",");
        boolean allowOtherAttributes = Boolean.parseBoolean(request.getParameter("allowOtherAttributes"));

        HttpSession session = request.getSession(false);
        Enumeration<String> attributeNames = session.getAttributeNames();

        Collection<String> expected = Arrays.asList(expectedAttributes);
        Collection<String> observed = Collections.list(attributeNames);
        if (allowOtherAttributes)
            assertTrue("Expected " + expected + ". Observed " + observed, observed.containsAll(expected));
        else
            assertEquals(new HashSet<String>(expected), new HashSet<String>(observed));
    }

    /**
     * Test that the reported creation time is reasonably close to the time that we create the session
     * and that the session consistently returns the same value as the creation time.
     */
    public void testCreationTime(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        long now = System.currentTimeMillis();
        HttpSession session = request.getSession(true);
        long creationTime = session.getCreationTime();
        long lastAccessedTime = session.getLastAccessedTime();
        assertEquals(creationTime, lastAccessedTime);

        // reported creation time should be reasonably close to when we requested the session be created
        long diff = creationTime - now;
        assertTrue("unexpectedly large difference from current time: " + diff, Math.abs(diff) < TimeUnit.NANOSECONDS.toMillis(TIMEOUT_NS));

        session.setAttribute("testCreationTime-key1", 3.14159f);

        // creation time should never change
        assertEquals(creationTime, session.getCreationTime());
    }

    /**
     * Test that HttpSessionListeners are notified when sessions are created and/or destroyed.
     */
    @SuppressWarnings("unchecked")
    public void testHttpSessionListener(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String[] expectCreated = request.getParameterValues("sessionCreated");
        String[] expectDestroyed = request.getParameterValues("sessionDestroyed");
        String[] expectNotDestroyed = request.getParameterValues("sessionNotDestroyed");

        String listenerClassName = "session.cache.web." + request.getParameter("listener") + ".SessionListener"; // listener1 or listener2
        Class<HttpSessionListener> sessionListenerClass = (Class<HttpSessionListener>) Class.forName(listenerClassName);

        LinkedBlockingQueue<String> created = (LinkedBlockingQueue<String>) sessionListenerClass.getField("created").get(null);
        LinkedBlockingQueue<String> destroyed = (LinkedBlockingQueue<String>) sessionListenerClass.getField("destroyed").get(null);

        if (expectCreated != null)
            for (String sessionId : expectCreated)
                assertTrue(sessionId, created.contains(sessionId));

        if (expectDestroyed != null)
            for (String sessionId : expectDestroyed)
                assertTrue(sessionId, destroyed.contains(sessionId));

        if (expectNotDestroyed != null)
            for (String sessionId : expectNotDestroyed)
                assertFalse(sessionId, destroyed.contains(sessionId));
    }

    /**
     * Invoke IBMSessionExt.invalidateAll(true)
     */
    public void testInvalidateAll(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        HttpSession session = request.getSession();
        // IBMSessionExt is SPI, so access the public method via reflection,
        AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> session.getClass()
                        .getMethod("invalidateAll", boolean.class)
                        .invoke(session, true));
    }

    /**
     * Test that the last accessed time changes when accessed at different times.
     */
    public void testLastAccessedTime(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        HttpSession session = request.getSession(true);
        long lastAccessedTime = session.getLastAccessedTime();

        TimeUnit.MILLISECONDS.sleep(100); // ensure that the time changes before next access

        assertEquals(lastAccessedTime, session.getLastAccessedTime());

        session.setAttribute("testLastAccessedTime-key1", 2.71828);

        // last accessed time should change
        assertNotSame(lastAccessedTime, session.getLastAccessedTime());
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
                                          new ObjectName("javax.cache:type=CacheConfiguration,CacheManager=hazelcast,Cache=com.ibm.ws.session.meta.default_host%2FsessionCacheApp"),
                                          CacheMXBean.class);
        assertEquals(String.class.getName(), metaInfoCacheMXBean.getKeyType());
        assertEquals(ArrayList.class.getName(), metaInfoCacheMXBean.getValueType());
        assertTrue(metaInfoCacheMXBean.isManagementEnabled());
        assertTrue(metaInfoCacheMXBean.isStatisticsEnabled());

        // CacheMXBean for session attributes cache
        CacheMXBean attrCacheMXBean = //
                        JMX.newMBeanProxy(mbs,
                                          new ObjectName("javax.cache:type=CacheConfiguration,CacheManager=hazelcast,Cache=com.ibm.ws.session.attr.default_host%2FsessionCacheApp"),
                                          CacheMXBean.class);
        assertEquals(String.class.getName(), attrCacheMXBean.getKeyType());
        assertEquals("[B", attrCacheMXBean.getValueType()); // byte[]
        assertTrue(attrCacheMXBean.isManagementEnabled());
        assertTrue(attrCacheMXBean.isStatisticsEnabled());

        // CacheStatisticsMXBean for session meta info cache
        CacheStatisticsMXBean metaInfoCacheStatsMXBean = //
                        JMX.newMBeanProxy(mbs,
                                          new ObjectName("javax.cache:type=CacheStatistics,CacheManager=hazelcast,Cache=com.ibm.ws.session.meta.default_host%2FsessionCacheApp"),
                                          CacheStatisticsMXBean.class);
        metaInfoCacheStatsMXBean.clear();
        assertEquals(0, metaInfoCacheStatsMXBean.getCacheRemovals());

        // CacheStatisticsMXBean for session attributes cache
        CacheStatisticsMXBean attrCacheStatsMXBean = //
                        JMX.newMBeanProxy(mbs,
                                          new ObjectName("javax.cache:type=CacheStatistics,CacheManager=hazelcast,Cache=com.ibm.ws.session.attr.default_host%2FsessionCacheApp"),
                                          CacheStatisticsMXBean.class);
        long initialPuts = attrCacheStatsMXBean.getCachePuts();

        HttpSession session = request.getSession();
        request.getSession().setAttribute("testMXBeans", 25.5f);
        ((IBMSession) session).sync();

        long puts = attrCacheStatsMXBean.getCachePuts();
        // Sometimes this assert is failing with observed value still being the initial value. Seems to be a bug in the JCache provider
        // assertEquals(initialPuts + 1, puts);

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
     * Begin the testing of session attribute serialization. Create the
     * session and set attributes.
     */
    public void testSerialization(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        Map<String, Object> sessionMap = new HashMap<String, Object>();
        session.setAttribute("map", sessionMap);
        System.out.println("Session is: " + session.getId());
        System.out.println("Session map is: " + sessionMap);

        // Test serialization of various types directly (setAttribute) and indirectly (HashMap).

        // String property
        String str = "STRING_PROP";
        session.setAttribute("str", str);
        sessionMap.put("str", str);

        // AppObject property
        AppObject object = new AppObject();
        session.setAttribute("appObject", object);
        sessionMap.put("appObject", object);

        //Test the serialization of performance improved primitives
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2018, 4, 19, 9, 23, 45);
        java.util.Date date = cal.getTime();
        session.setAttribute("date", date);

        java.sql.Date sqlDate = new java.sql.Date(1524236907331l);
        session.setAttribute("sqlDate", sqlDate);

        double d = 55901.55902;
        session.setAttribute("double", d);

        float f = 12345.6789f;
        session.setAttribute("float", f);

        byte b = Byte.parseByte("3");
        session.setAttribute("byte", b);

        Timestamp timestamp = new Timestamp(255073580786543l);
        session.setAttribute("timestamp", timestamp);

        Time time = new Time(345033920786213l);
        session.setAttribute("time", time);

        BigInteger bigint = new BigInteger("1234567891011121314151617181920");
        session.setAttribute("BigInteger", bigint);

        BigInteger negBigInt = new BigInteger("-1234567890000098765432198349834");
        session.setAttribute("NegativeBigInteger", negBigInt);

        BigDecimal bigDecimal = new BigDecimal("12345678910.11121314151617181920");
        session.setAttribute("BigDecimal", bigDecimal);

        BigDecimal negBigDecimal = new BigDecimal("-12345678900000987.65432198349834");
        session.setAttribute("NegativeBigDecimal", negBigDecimal);

        long long0 = 0l;
        session.setAttribute("long0", long0);

        long longNeg1 = -1l;
        session.setAttribute("longNeg1", longNeg1);

        long maxLong = Long.MAX_VALUE;
        session.setAttribute("maxLong", maxLong);

        long minLong = Long.MIN_VALUE;
        session.setAttribute("minLong", minLong);

        int int0 = 0;
        session.setAttribute("int0", int0);

        int intNeg1 = -1;
        session.setAttribute("intNeg1", intNeg1);

        int maxInt = Integer.MAX_VALUE;
        session.setAttribute("maxInt", maxInt);

        int minInt = Integer.MIN_VALUE;
        session.setAttribute("minInt", minInt);

        short sh = 24;
        session.setAttribute("short", sh);

        char ch = 'A';
        session.setAttribute("char", ch);

        boolean bool = true;
        session.setAttribute("boolean", bool);

        byte[] bytes = "Rochester, Minnesota, United States of America".getBytes();
        session.setAttribute("byte array", bytes);

        ArrayList<Integer> arrayList = new ArrayList<Integer>();
        arrayList.add(1000);
        arrayList.add(2);
        session.setAttribute("arrayList", arrayList);

        AtomicInteger atomicInt = new AtomicInteger(55901);
        session.setAttribute("atomicInteger", atomicInt);

        AtomicLong atomicLong = new AtomicLong(559015590255903l);
        session.setAttribute("atomicLong", atomicLong);
    }

    /**
     * Complete the testing of session attribute serialization. The call to
     * getSession will require deserializing the object.
     */
    public void testSerialization_complete(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        HttpSession session = request.getSession(false);
        assertNotNull("Value from session is unexpectedly NULL, most likely due to test infrastructure; check logs for more information.", session);
        @SuppressWarnings("unchecked")
        Map<String, Object> sessionMap = (Map<String, Object>) session.getAttribute("map");
        System.out.println("Session is: " + session.getId());
        System.out.println("Session map is: " + sessionMap);

        // String
        String str = (String) session.getAttribute("str");
        assertEquals("direct String value not deserialized properly", "STRING_PROP", str);
        str = (String) sessionMap.get("str");
        assertEquals("indirect String value not deserialized properly", "STRING_PROP", str);

        // AppObject
        AppObject object = (AppObject) session.getAttribute("appObject");
        assertNotNull("The appObject was not found in the HTTP session", object);
        assertTrue("direct AppObject not deserialized properly", object.deserialized);
        object = (AppObject) sessionMap.get("appObject");
        assertNotNull("The indirect appObject was not found in the HTTP session", object);
        assertTrue("indirect AppObject not deserialized properly", object.deserialized);

        ArrayList<String> attributeNames = Collections.list(session.getAttributeNames());
        int weldCount = 0;
        for (int i = attributeNames.size(); i-- > 0;) {
            String name = attributeNames.get(i);
            if (name.startsWith("WELD_S_") || name.contains(".weld."))
                weldCount++;
        }
        String attributeNamesString = attributeNames.toString();
        assertTrue(attributeNamesString, attributeNames.containsAll(Arrays.asList("map", "str", "appObject")));
        assertEquals(attributeNamesString, weldCount + 29, attributeNames.size());

        //Test the serialization of performance improved primitives (that aren't tested elsewhere)
        java.util.Date actualDate = (Date) session.getAttribute("date");
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2018, 4, 19, 9, 23, 45);
        java.util.Date expectedDate = cal.getTime();
        assertEquals(expectedDate.getTime(), actualDate.getTime());

        java.sql.Date expectedSqlDate = new java.sql.Date(1524236907331l);
        java.sql.Date actualSqlDate = (java.sql.Date) session.getAttribute("sqlDate");
        assertEquals(expectedSqlDate.getTime(), actualSqlDate.getTime());

        double actualDouble = (double) session.getAttribute("double");
        assertEquals(55901.55902, actualDouble, 0);

        float actualFloat = (float) session.getAttribute("float");
        assertEquals(12345.6789f, actualFloat, 0);

        byte expectedByte = Byte.parseByte("3");
        byte actualByte = (byte) session.getAttribute("byte");
        assertEquals(expectedByte, actualByte);

        Timestamp expectedTimestamp = new Timestamp(255073580786543l);
        Timestamp actualTimestamp = (Timestamp) session.getAttribute("timestamp");
        assertEquals(expectedTimestamp.getTime(), actualTimestamp.getTime());

        Time expectedTime = new Time(345033920786213l);
        Time actualTime = (Time) session.getAttribute("time");
        assertEquals(expectedTime.getTime(), actualTime.getTime());

        BigInteger expectedBigint = new BigInteger("1234567891011121314151617181920");
        BigInteger actualBigint = (BigInteger) session.getAttribute("BigInteger");
        assertEquals(expectedBigint.longValue(), actualBigint.longValue());

        BigInteger expectedNegBigint = new BigInteger("-1234567890000098765432198349834");
        BigInteger actualNegBigint = (BigInteger) session.getAttribute("NegativeBigInteger");
        assertEquals(expectedNegBigint.longValue(), actualNegBigint.longValue());

        BigDecimal expectedBigDecimal = new BigDecimal("12345678910.11121314151617181920");
        BigDecimal actualBigDecimal = (BigDecimal) session.getAttribute("BigDecimal");
        assertEquals(expectedBigDecimal.longValue(), actualBigDecimal.longValue());

        BigDecimal expectedNegBigDecimal = new BigDecimal("-12345678900000987.65432198349834");
        BigDecimal actualNegBigDecimal = (BigDecimal) session.getAttribute("NegativeBigDecimal");
        assertEquals(expectedNegBigDecimal.longValue(), actualNegBigDecimal.longValue());

        long actualLong0 = (long) session.getAttribute("long0");
        assertEquals(0l, actualLong0);

        long actualLongNeg1 = (long) session.getAttribute("longNeg1");
        assertEquals(-1l, actualLongNeg1);

        long actualMaxLong = (long) session.getAttribute("maxLong");
        assertEquals(Long.MAX_VALUE, actualMaxLong);

        long actualMinLong = (long) session.getAttribute("minLong");
        assertEquals(Long.MIN_VALUE, actualMinLong);

        long actualint0 = (int) session.getAttribute("int0");
        assertEquals(0, actualint0);

        long actualIntNeg1 = (int) session.getAttribute("intNeg1");
        assertEquals(-1, actualIntNeg1);

        long actualMaxInt = (int) session.getAttribute("maxInt");
        assertEquals(Integer.MAX_VALUE, actualMaxInt);

        long actualMinInt = (int) session.getAttribute("minInt");
        assertEquals(Integer.MIN_VALUE, actualMinInt);

        short actualShort = (short) session.getAttribute("short");
        assertEquals(24, actualShort);

        char expectedChar = 'A';
        char actualChar = (char) session.getAttribute("char");
        assertEquals(expectedChar, actualChar);

        boolean expectedBoolean = true;
        boolean actualBoolean = (boolean) session.getAttribute("boolean");
        assertEquals(expectedBoolean, actualBoolean);

        byte[] expectedBytes = "Rochester, Minnesota, United States of America".getBytes();
        byte[] actualBytes = (byte[]) session.getAttribute("byte array");
        assertTrue(Arrays.equals(expectedBytes, actualBytes));

        @SuppressWarnings({ "unchecked" })
        ArrayList<Integer> actualArrayList = (ArrayList<Integer>) session.getAttribute("arrayList");
        assertEquals(2, actualArrayList.size());
        assertTrue("Expected arrayList to contain 2", actualArrayList.contains(2));
        assertTrue("Expected arrayList to contain 1000", actualArrayList.contains(1000));

        AtomicInteger actualAtomicInt = (AtomicInteger) session.getAttribute("atomicInteger");
        assertEquals(55901, actualAtomicInt.get());

        AtomicLong actualAtomicLong = (AtomicLong) session.getAttribute("atomicLong");
        assertEquals(559015590255903l, actualAtomicLong.get());
    }

    public void testSerializeDataSource(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        HttpSession session = request.getSession();
        Map<String, Object> sessionMap = new HashMap<String, Object>();
        session.setAttribute("map", sessionMap);
        System.out.println("Session is: " + session.getId());
        System.out.println("Session map is: " + sessionMap);

        // DataSource
        DataSource ds = InitialContext.doLookup("java:comp/env/jdbc/derbyRef");
        session.setAttribute("dataSource", ds);
        sessionMap.put("dataSource", ds);
        ds.getConnection().close();
    }

    public void testSerializeDataSource_complete(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        HttpSession session = request.getSession(false);
        @SuppressWarnings("unchecked")
        Map<String, Object> sessionMap = (Map<String, Object>) session.getAttribute("map");
        System.out.println("Session is: " + session.getId());
        System.out.println("Session map is: " + sessionMap);

        // DataSource
        DataSource sessionDS = (DataSource) session.getAttribute("dataSource");
        assertNotNull("The dataSource was not found in the HTTP session", sessionDS);
        sessionDS.getConnection().close();
        sessionDS = (DataSource) sessionMap.get("dataSource");
        assertNotNull("The indirect dataSource was not found in the HTTP session", sessionDS);
        sessionDS.getConnection().close();
    }

    /**
     * Expects that the session is either empty, or if it exists it should not have any of the attributes set
     */
    public void testSessionEmpty(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        HttpSession session = request.getSession(false);
        if (session == null) {
            System.out.println("Session was null");
            return;
        }

        assertNull(session.getAttribute("str"));
        assertNull(session.getAttribute("appObject"));
        assertFalse(session.getAttributeNames().hasMoreElements());
    }

    /**
     * Confirm that a session attribute name is written to the session info cache.
     */
    @SuppressWarnings("rawtypes")
    public void testSessionInfoCache(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String sessionId = request.getParameter("sessionId");
        String expectedAttributes = request.getParameter("attributes");
        boolean allowOtherAttributes = Boolean.parseBoolean(request.getParameter("allowOtherAttributes"));

        List<String> expected = expectedAttributes == null ? Collections.emptyList() : Arrays.asList(expectedAttributes.split(","));

        Cache<String, ArrayList> cache = Caching.getCache("com.ibm.ws.session.meta.default_host%2FsessionCacheApp", String.class, ArrayList.class);
        assertNotNull("Value from cache is unexpectedly NULL, most likely due to test infrastructure; check logs for more information.", cache);

        ArrayList<?> values = cache.get(sessionId);

        //catching test case errors due to test infrastructure
        if (values == null) {
            System.out.println("Value from cache is unexpectedly NULL, most likely due to test infrastructure; skipping rest of test method testSessionInfoCache. Check logs for more information.");
        }

        @SuppressWarnings("unchecked")
        TreeSet<String> attributeNames = (TreeSet<String>) values.get(values.size() - 1); // last entry is the session attribute names

        assertTrue(expected + " not found in " + attributeNames, attributeNames.containsAll(expected));

        if (!allowOtherAttributes)
            assertTrue("Some extra attributes found within " + attributeNames, expected.containsAll(attributeNames));
    }

    /**
     * Confirm that a session attribute and its value are written to the session attributes cache.
     */
    public void testSessionPropertyCache(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String sessionId = request.getParameter("sessionId");
        String key = sessionId + '.' + request.getParameter("key");
        String expectedValues = request.getParameter("values"); // value must be one of the values in this list (null for not present)
        String type = request.getParameter("type");

        Set<Object> expected = new HashSet<Object>();
        for (String v : expectedValues.split(",")) {
            Object o = toType(type, v);
            expected.add(o == null ? null : Arrays.toString(toBytes(o)));
        }

        Cache<String, byte[]> cache = Caching.getCache("com.ibm.ws.session.attr.default_host%2FsessionCacheApp", String.class, byte[].class);

        assertNotNull("Value from cache is unexpectedly NULL, most likely due to test infrastructure; check logs for more information.", cache);

        byte[] bytes = cache.get(key);

        String strValue = bytes == null ? null : Arrays.toString(bytes);
        assertTrue(strValue + " not found in " + expected, expected.contains(strValue));
    }

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
        }
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(o);
            return bos.toByteArray();
        }
    }

    public void sessionPut(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean createSession = Boolean.parseBoolean(request.getParameter("createSession"));
        HttpSession session = request.getSession(createSession);
        if (createSession)
            System.out.println("Created a new session with sessionID=" + session.getId());
        else
            System.out.println("Re-using existing session with sessionID=" + session == null ? null : session.getId());
        String key = request.getParameter("key");
        String value = request.getParameter("value");
        String type = request.getParameter("type");
        Object val = toType(type, value);
        session.setAttribute(key, val);
        String sessionID = session.getId();
        System.out.println("Put entry: " + key + '=' + value + " into sessionID=" + sessionID);
        response.getWriter().write("session id: [" + sessionID + "]");

        // Normally, session postinvoke writes the updates after the servlet returns control to the test logic.
        // This can be a problem if the test logic proceeds to execute further test logic based on the expectation
        // that updates made under the previous servlet request have gone into effect.  Tests that are vulnerable
        // to this can use the sync=true parameter to force update to be made before servlet returns.
        boolean sync = Boolean.parseBoolean(request.getParameter("sync"));
        if (sync)
            ((IBMSession) session).sync();
    }

    public void sessionGet(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        String key = request.getParameter("key");
        String rawExpectedValue = request.getParameter("expectedValue");
        String type = request.getParameter("type");
        boolean compareAsString = Boolean.parseBoolean(request.getParameter("compareAsString")); // useful if the class does not implement .equals
        Object expectedValue = toType(type, rawExpectedValue);

        HttpSession session = request.getSession(false);
        if (expectedValue == null && session == null) {
            System.out.println("Session was null and was expecting null value.");
            return;
        } else if (session == null) {
            fail("Was expecting to get " + key + '=' + expectedValue + ", but instead got a null session.");
        }
        Object actualValue = session.getAttribute(key);
        System.out.println("Got entry: " + key + '=' + actualValue + " from sessionID=" + session.getId());

        if (compareAsString)
            assertEquals(expectedValue.toString(), actualValue.toString());
        else
            assertEquals(expectedValue, actualValue);
    }

    public void sessionRemoveAttribute(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        String key = request.getParameter("key");
        HttpSession session = request.getSession(false);
        session.removeAttribute(key);

        // Normally, session postinvoke writes the updates after the servlet returns control to the test logic.
        // This can be a problem if the test logic proceeds to execute further test logic based on the expectation
        // that updates made under the previous servlet request have gone into effect.  Tests that are vulnerable
        // to this can use the sync=true parameter to force update to be made before servlet returns.
        boolean sync = Boolean.parseBoolean(request.getParameter("sync"));
        if (sync)
            ((IBMSession) session).sync();
    }

    public void sessionGetTimeout(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean createSession = Boolean.parseBoolean(request.getParameter("createSession"));
        HttpSession session = request.getSession(createSession);
        if (createSession)
            System.out.println("Created a new session with sessionID=" + session.getId());
        else
            System.out.println("Re-using existing session with sessionID=" + session == null ? null : session.getId());
        String key = request.getParameter("key");
        String expected = request.getParameter("expectedValue");
        String sessionId = session.getId();

        // poll for entry to be invalidated from cache
        System.setProperty("hazelcast.config", InitialContext.doLookup("jcache/hazelcast.config")); // need to use same config file as server.xml
        @SuppressWarnings("rawtypes")
        Cache<String, ArrayList> cache = Caching.getCache("com.ibm.ws.session.meta.default_host%2FsessionCacheApp", String.class, ArrayList.class);
        for (long start = System.nanoTime(); cache.containsKey(sessionId) && System.nanoTime() - start < TIMEOUT_NS; TimeUnit.MILLISECONDS.sleep(500));

        String actual = (String) session.getAttribute(key);
        assertEquals(expected, actual);
    }

    public void sessionPutTimeout(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        HttpSession session = request.getSession(true);
        String key = request.getParameter("key");
        String value = request.getParameter("value");
        String sessionId = session.getId();
        // poll for entry to be invalidated from cache
        System.setProperty("hazelcast.config", InitialContext.doLookup("jcache/hazelcast.config")); // need to use same config file as server.xml
        @SuppressWarnings("rawtypes")
        Cache<String, ArrayList> cache = Caching.getCache("com.ibm.ws.session.meta.default_host%2FsessionCacheApp", String.class, ArrayList.class);

        for (long start = System.nanoTime(); cache.containsKey(sessionId) && System.nanoTime() - start < TIMEOUT_NS; TimeUnit.MILLISECONDS.sleep(500));
        session.setAttribute(key, value);
        String actualValue = (String) session.getAttribute(key);
        assertEquals(value, actualValue);
    }

    /**
     * Check a value in the Session Cache
     * If value is null check that the key has been removed from the cache.
     * If a session Id is provided, validate that the session exists (value!=null) or has been removed (value==null)
     */
    public void cacheCheck(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        String key = request.getParameter("key");
        String value = request.getParameter("value");
        String sessionId = request.getParameter("sid");
        System.setProperty("hazelcast.config", InitialContext.doLookup("jcache/hazelcast.config")); // need to use same config file as server.xml
        Cache<String, byte[]> cacheA = Caching.getCache("com.ibm.ws.session.attr.default_host%2FsessionCacheApp", String.class, byte[].class);
        byte[] result = cacheA.get(key);
        assertEquals(value, result == null ? null : Arrays.toString(result));

        //Validate session existence/deletion if we pass in a sessionId
        if (sessionId != null) {
            @SuppressWarnings("rawtypes")
            Cache<String, ArrayList> cacheM = Caching.getCache("com.ibm.ws.session.meta.default_host%2FsessionCacheApp", String.class, ArrayList.class);
            assertEquals(cacheM.containsKey(sessionId), value == null ? false : true);
        }
    }

    /**
     * Timeout in the middle of a servlet call then check a value in the Session Cache.
     */
    public void sessionGetTimeoutCacheCheck(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean createSession = Boolean.parseBoolean(request.getParameter("createSession"));
        HttpSession session = request.getSession(createSession);
        if (createSession)
            System.out.println("Created a new session with sessionID=" + session.getId());
        else
            System.out.println("Re-using existing session with sessionID=" + session == null ? null : session.getId());
        String key = request.getParameter("key");
        String expected = request.getParameter("expectedValue");
        String sessionId = session.getId();

        // poll for entry to be invalidated from cache
        System.setProperty("hazelcast.config", InitialContext.doLookup("jcache/hazelcast.config")); // need to use same config file as server.xml
        @SuppressWarnings("rawtypes")
        Cache<String, ArrayList> cache = Caching.getCache("com.ibm.ws.session.meta.default_host%2FsessionCacheApp", String.class, ArrayList.class);
        for (long start = System.nanoTime(); cache.containsKey(sessionId) && System.nanoTime() - start < TIMEOUT_NS; TimeUnit.MILLISECONDS.sleep(500));

        Cache<String, byte[]> cacheAttr = Caching.getCache("com.ibm.ws.session.attr.default_host%2FsessionCacheApp", String.class, byte[].class);

        assertNotNull("Value from cache is unexpectedly NULL, most likely due to test infrastructure; check logs for more information.", cacheAttr);
        byte[] result = cacheAttr.get(key);
        assertEquals(expected, result == null ? null : Arrays.toString(result));
    }

    /**
     * Get a session attribute which is a StringBuffer and append characters,
     * but don't set the attribute with the updated value.
     */
    public void testStringBufferAppendWithoutSetAttribute(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String key = request.getParameter("key");
        HttpSession session = request.getSession(true);
        StringBuffer value = (StringBuffer) session.getAttribute(key);
        value.append("Appended");
    }

    public void testTimeoutExtensionA(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession(true);
        session.setMaxInactiveInterval(500); // seconds
    }

    public void testTimeoutExtensionB(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession(false);
        assertNotNull("Unable to recover existing session.  It may have timed out.", session);
        assertEquals(500, session.getMaxInactiveInterval());
    }

    /**
     * Set the maxInactiveInterval for the given session to 1 second
     */
    public void setMaxInactiveInterval(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        HttpSession session = request.getSession(false);
        session.setMaxInactiveInterval(1);
    }

    /**
     * Convert a String value to the specified type.
     * This is valid for the primitive wrapper classes (such as java.lang.Integer)
     * and any other type that has a single argument String constructor.
     */
    private static Object toType(String type, String s) throws Exception {
        if (s == null || "null".equals(s))
            return null;

        if (Character.class.getName().equals(type))
            return s.charAt(0);

        return Class.forName(type).getConstructor(String.class).newInstance(s);
    }
}
