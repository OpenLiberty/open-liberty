/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package websphere.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import websphere.listener.StandardServletContextListener;
import websphere.listener.WebSphereApplicationListener;

/**
 * Tests the legacy WebSphere servlet event API
 *
 * https://openliberty.io/docs/latest/reference/javadoc/api/servlet-4.0.com.ibm.websphere.servlet.event.html
 *
 * These APIs were from WAS 4.0+ time frame. Application should use the servlet standard APIs instead!
 *
 * Since these APIs are still around, this test is added to cover the test gap in this area.
 */

@WebServlet(urlPatterns = { "/ServletEvent", "/FilterErrorEvent" })
public class EventServlet extends HttpServlet {
    private static final String CLASS_NAME = EventServlet.class.getName();

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String temp;
        _log(">>>>> service() ENTRY ");

        //retrieve the context attributes set by standard and WebSphere servlet API.
        String WebSphereAPI = (String) req.getServletContext().getAttribute(WebSphereApplicationListener.WEBSPHERE_ATT);
        String StandardAPI = (String) req.getServletContext().getAttribute(StandardServletContextListener.STANDARD_ATT);

        temp = "\t\t\t\t>>>(service)>>> Context attribute from WebSphere API [" + WebSphereAPI + "]\n" + "\t\t\t\t>>>(service)>>> Context attribute from Standard API ["
               + StandardAPI + "]\n";
        log(temp);
        WebSphereApplicationListener.OUTBUFFER.append(temp);

        _log("<<<<< service() EXIT");
    }

    private void _log(String s) {
        System.out.println(CLASS_NAME + " " + s);
    }
}
