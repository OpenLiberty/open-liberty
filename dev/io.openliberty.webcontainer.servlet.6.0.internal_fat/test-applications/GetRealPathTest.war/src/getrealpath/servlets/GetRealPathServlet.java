/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package getrealpath.servlets;

import java.io.IOException;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Test Servlet that calls ServletContext.getRealPath(String path) to ensure
 * that a path with and without a "/" returns the same real path.
 *
 */
@WebServlet("/GetRealPathServlet")
public class GetRealPathServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = GetRealPathServlet.class.getName();

    public GetRealPathServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletOutputStream sos = response.getOutputStream();
        ServletContext servletContext = request.getServletContext();
        String getRealPathWithoutForwardSlash = servletContext.getRealPath("index.html");
        String getRealPathWithForwardSlash = servletContext.getRealPath("/index.html");
        sos.println("ServletContext getRealPath no forward slash: " + getRealPathWithoutForwardSlash);
        sos.println("ServletContext getRealPath with forward slash: " + getRealPathWithForwardSlash);

        if (getRealPathWithoutForwardSlash != null && getRealPathWithForwardSlash != null) {
            if (getRealPathWithoutForwardSlash.equals(getRealPathWithForwardSlash)) {
                sos.println("ServletContext getRealPath returned the same value with and without a forward slash. Test PASSED!");
            } else {
                sos.println("ServletContext getRealPath did not return the same value with and without a forward slash. Test FAILED!");
            }
        } else {
            sos.println("ServletContext getRealPath returned a null value.");
        }
    }
}
