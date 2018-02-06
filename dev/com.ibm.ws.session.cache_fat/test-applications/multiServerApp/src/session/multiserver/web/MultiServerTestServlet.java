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
package session.multiserver.web;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/MultiServerTestServlet")
public class MultiServerTestServlet extends FATServlet {

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
    }

    /**
     * Complete the testing of session attribute serialization. The call to
     * getSession will require deserializing the object.
     */
    public void testSerialization_complete(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        HttpSession session = request.getSession();
        assertNotNull("HTTP Session was null.", session);
        @SuppressWarnings("unchecked")
        Map<String, Object> sessionMap = (Map<String, Object>) session.getAttribute("map");
        System.out.println("Session is: " + session);
        System.out.println("Session map is: " + sessionMap);

        // Boolean
        Boolean b = (Boolean) session.getAttribute("boolean");
        assertTrue("direct boolean value not deserialized properly", b);
        b = (Boolean) sessionMap.get("boolean");
        assertTrue("indirect boolean value not deserialized properly", b);

        // AppObject
        AppObject object = (AppObject) session.getAttribute("appObject");
        assertNotNull("The appObject was not found in the HTTP session", object);
        assertTrue("direct AppObject not deserialized properly", object.deserialized);
        object = (AppObject) sessionMap.get("appObject");
        assertNotNull("The indirect appObject was not found in the HTTP session", object);
        assertTrue("indirect AppObject not deserialized properly", object.deserialized);
    }

}
