/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package testservleta.jar.servlets;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@WebServlet("/ServletA")
public class ServletA extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public ServletA() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletOutputStream sos = response.getOutputStream();      
        ServletContext sc = request.getServletContext();
        
        String servletName = "A";
        
        // SCI
        String sciRanMsg = (String)(sc.getAttribute("Sci_" + servletName + "_RanMsg"));    
        String sciOrder = (String)(sc.getAttribute("sciOrder"));  
        
        // Listener
        String listenerRanMsg = (String)(sc.getAttribute("Listener_" + servletName + "_Ran_Msg"));    
        String listenerOrder = (String)(sc.getAttribute("listenerOrder")); 
        
        sos.println("Hello From Servlet " + servletName + ". \n"  
                     + sciRanMsg 
                     + "\nSCI Order [ " + sciOrder + " ]\n" 
                     + listenerRanMsg 
                     + "\nListener order [ " + listenerOrder + " ]");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

}