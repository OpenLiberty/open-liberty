/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package servlets;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/GetAltVirtualServerNameServlet1")
public class GetAltVirtualServerNameServlet1 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private ServletContext context;

    @Override
    public void init(ServletConfig config) {
        context = config.getServletContext();
    }

    public GetAltVirtualServerNameServlet1() {
        super();
        context = null;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletOutputStream sos = response.getOutputStream();

        if (context != null)
        {
            String expectedServerName = request.getParameter("serverName");
            String vsn = context.getVirtualServerName();
            //String vsn = "localhost";
            if (vsn == null) {
                sos.println("FAIL: VirtualServerName was null");
            } else {
                sos.println((vsn.equals(expectedServerName)) ? "SUCCESS: VirtualServerName was as expected : " + vsn : "FAIL: VirtualServerName was : " + vsn);
            }
        } else {
            sos.println("FAIL : ServletContext was null");
        }

    }

}
