/*******************************************************************************
 * Copyright (c) 2018,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package session.cache.web.cdi;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicInteger;

import javax.cache.Cache;
import javax.cache.Caching;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/SessionCDITestServlet")
public class SessionCDITestServlet extends FATServlet {
    @Inject
    SessionScopedBean1 bean1;

    @Inject
    SessionScopedBean2 bean2;

    private static final AtomicInteger counter = new AtomicInteger();

    /**
     * Update a session scoped CDI bean with a new value supplied via the "newValue" parameter,
     * writing the previous value to the servlet response.
     */
    public void testUpdateSessionScopedBean(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String newValue = request.getParameter("newValue");

        HttpSession session = request.getSession();
        String sessionId = session.getId();
        System.out.println("session id is " + sessionId);

        PrintWriter responseWriter = response.getWriter();
        responseWriter.write("session id: [" + sessionId + "]");

        String previousValue = bean1.getStringValue();
        System.out.println("previous value: " + previousValue);
        bean1.setStringValue(newValue);
        System.out.println("made update to: " + newValue);

        responseWriter.write("previous value for SessionScopedBean: [" + previousValue + "]");

        bean2.setStr("It is " + counter.incrementAndGet());

        for (Enumeration<String> attrs = session.getAttributeNames(); attrs.hasMoreElements();) {
            String name = attrs.nextElement();
            System.out.println("Session attribute " + name + ": " + session.getAttribute(name));
            if (name.startsWith("WELD_S#"))
                System.out.println("### HASH FOR " + name + " is " + Integer.toHexString(session.getAttribute(name).hashCode()));
        }
    }

    /**
     * Directly read entries from the session attributes cache and writes to the servlet output so that
     * the caller can confirm that the values are written to the cache.
     */
    public void testWeldSessionAttributes(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String sessionId = request.getParameter("sessionId");
        String key0 = sessionId + ".WELD_S#0";
        String key1 = sessionId + ".WELD_S#1";

        Cache<String, byte[]> cache = Caching.getCache("com.ibm.ws.session.attr.default_host%2FsessionCacheApp", String.class, byte[].class);
        assertNotNull("Value from cache is unexpectedly NULL, most likely due to test infrastructure; check logs for more information.", cache);

        byte[] value0 = cache.get(key0);
        byte[] value1 = cache.get(key1);
        cache.close();

        String strValue0 = Arrays.toString(value0);
        String strValue1 = Arrays.toString(value1);

        System.out.println("bytes for " + key0 + ": " + strValue0);
        System.out.println("bytes for " + key1 + ": " + strValue1);

        PrintWriter responseWriter = response.getWriter();
        responseWriter.write("bytes for WELD_S#0: " + strValue0);
        responseWriter.write("bytes for WELD_S#1: " + strValue1);
    }
}
