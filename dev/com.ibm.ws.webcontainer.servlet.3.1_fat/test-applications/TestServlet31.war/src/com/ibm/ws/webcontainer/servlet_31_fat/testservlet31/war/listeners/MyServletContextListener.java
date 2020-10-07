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
package com.ibm.ws.webcontainer.servlet_31_fat.testservlet31.war.listeners;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebListener;

import com.ibm.ws.webcontainer.servlet_31_fat.testservlet31.war.servlets.MyServlet;

@WebListener
public class MyServletContextListener implements ServletContextListener {

    public MyServletContextListener() {
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext sc = sce.getServletContext();
        try {
            // 130998: For testing Section 8.1.1 of the Servlet 3.1 spec, “@WebServlet”.
            // Programmatically adding servlet with a name different from that specified in annotation.
            // This should create a new instance of the MyServlet with a different mapping. 
            sc.log("programmatically creating and mapping a servlet");
            MyServlet ps3 = sc.createServlet(MyServlet.class);
            ServletRegistration.Dynamic reg = sc.addServlet("MyProgrammaticServlet", ps3);
            if (reg != null) {
                reg.addMapping("/ProgrammaticServlet");
            }

        } catch (ServletException e) {
            e.printStackTrace();
        }
    }
}