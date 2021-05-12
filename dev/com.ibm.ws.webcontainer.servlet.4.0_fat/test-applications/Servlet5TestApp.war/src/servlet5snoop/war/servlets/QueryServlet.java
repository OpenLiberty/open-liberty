/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package servlet5snoop.war.servlets;

import javax.servlet.*;
import javax.servlet.http.*;

import java.io.PrintWriter;
import java.io.IOException;

/*
* Tests allowQueryParamWithNoEqual for servlet-5.0 and higher 
*/
public class QueryServlet extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("text/html");

        PrintWriter out = response.getWriter();

        String t = request.getParameter("test");

        if(t != null && t.equals("")){
            out.println("SUCCESS! request.getParameter(\"test\") -> \"\" ");
        } else {
            out.println("ERROR! request.getParameter(\"test\") -> " + t + ".");
        }

    }

}
