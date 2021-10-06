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
package com.ibm.ws.el30.fat.servicelookup;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.el.ELProcessor;

public class TestServiceLookupServlet extends HttpServlet {
 
   public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        try {
            ELProcessor elp = new ELProcessor();
            out.println("<h1>" + "Lookup works!" + "</h1>");
        } catch(Exception e){
            out.println("<p>" + e + "</p>");
        }
      
   }

}
