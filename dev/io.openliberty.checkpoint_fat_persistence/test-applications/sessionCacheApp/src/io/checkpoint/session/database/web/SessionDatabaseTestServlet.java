/*******************************************************************************
 * Copyright (c) 2017, 2023 IBM Corporation and others.
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
package io.checkpoint.session.database.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/SessionDatabaseTestServlet")
public class SessionDatabaseTestServlet extends FATServlet {
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
