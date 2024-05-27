/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package io.openliberty.http.monitor.fat.jspApp;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Normal Servlet
 */
@WebServlet("/failServlet")
public class FailServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /** {@inheritDoc} */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        //accepts query parameter that helps decide how to fail
        switch (req.getParameter("failMode")) {
            case "zero":
                divideArith(resp);
                break;
            case "custom":
                customResponse(resp);
                break;
            case "io":
                throwIO(resp);
                break;
            case "iae":
                throwIAE(resp);
                break;
            default:
                break;
        }
        resp.getWriter().append("Hello, lets fail together!");
    }

    private void divideArith(HttpServletResponse resp) {
        int x = 4 / 0;
    }

    private void customResponse(HttpServletResponse resp) {
        resp.setStatus(456);
    }

    private void throwIO(HttpServletResponse resp) throws IOException {
        throw new IOException("I'm an IO Exception");
    }

    private void throwIAE(HttpServletResponse resp) {
        throw new IllegalArgumentException("I'm an IA Exception");
    }
}
