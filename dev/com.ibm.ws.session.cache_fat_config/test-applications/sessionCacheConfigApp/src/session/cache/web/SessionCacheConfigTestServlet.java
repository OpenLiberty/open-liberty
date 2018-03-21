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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/SessionCacheConfigTestServlet")
public class SessionCacheConfigTestServlet extends FATServlet {
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
}
