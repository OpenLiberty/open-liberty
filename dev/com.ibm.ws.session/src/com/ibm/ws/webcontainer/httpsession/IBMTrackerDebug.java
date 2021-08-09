/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.httpsession;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.session.SessionContextRegistry;

public class IBMTrackerDebug extends HttpServlet {

    private static final long serialVersionUID = -1990952537442218792L;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    public String getServletInfo() {
        return "provides dump of IBMSessionContextImpl state";
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        ServletOutputStream out = response.getOutputStream();
        out.println("<html><body>");

        out.println(SessionContextRegistry.getTrackerData()); // *dbc2.2

        out.println("</body></html>");
    }

}
