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

/**
 * Tests the legacy WebSphere servlet event API
 *
 * https://openliberty.io/docs/latest/reference/javadoc/api/servlet-4.0.com.ibm.websphere.servlet.event.html
 *
 * This servlet throws a ServletException to trigger a ServletErrorListener event
 */

@WebServlet(urlPatterns = "/ServletError")
public class EventErrorServlet extends HttpServlet {
    private static final String CLASS_NAME = EventErrorServlet.class.getName();

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        _log(">>>>> service() ENTRY ");
        _log(">>>>> service() about to throw a ServletException.");

        throw new RuntimeException(CLASS_NAME + " throws Runtime NPE");
    }

    private void _log(String s) {
        System.out.println(CLASS_NAME + " " + s);
    }
}
