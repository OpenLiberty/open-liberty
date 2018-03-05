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
package session.cache.web.cdi;

import java.io.IOException;
import java.util.Enumeration;

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
    SessionScopedBean bean;

    /**
     * Update a session scoped CDI bean with a new value supplied via the "newValue" parameter,
     * writing the previous value to the servlet response.
     */
    public void testUpdateSessionScopedBean(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String newValue = request.getParameter("newValue");

        HttpSession session = request.getSession();
        String sessionId = session.getId();
        System.out.println("session id is " + sessionId);

        String previousValue = bean.stringValue;
        System.out.println("previous value: " + previousValue);
        bean.stringValue = newValue;
        System.out.println("made update to: " + newValue);

        response.getWriter().write("previous value for SessionScopedBean: [" + previousValue + "]");

        for (Enumeration<String> attrs = session.getAttributeNames(); attrs.hasMoreElements();) {
            String name = attrs.nextElement();
            System.out.println("Session attribute " + name + ": " + session.getAttribute(name));
        }
    }

}
