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
package test.app;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Web endpoint for the FAT test to hit and cause the associated ServletContext listener
 * to be driven.
 */
@WebServlet("/TestServlet")
public class SlowStopAppServlet extends HttpServlet {
    private static final long serialVersionUID = -1805130382987223508L;

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String timeout = request.getParameter("timeout");
        if ((timeout != null) && !timeout.isEmpty()) {
            //if a timeout is specified for the context, pass it through
            request.getServletContext().setAttribute("timeout", timeout);
        }
        response.getWriter().print("Hello from inside the test servlet");
        response.getWriter().flush();
    }
}
