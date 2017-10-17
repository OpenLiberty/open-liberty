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
package testservlet40.war.servlets;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*
 * Servlet 4.0 Clarification 2 and 3
 */

@WebServlet("/ServletClarification")
public class ServletClarification extends HttpServlet {
    private static final long serialVersionUID = 1L;
    ServletConfig config;

    public ServletClarification() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletOutputStream sos = response.getOutputStream();
        ServletContext context = request.getServletContext();
        String nullAttributeName = null;
        String value;
        Object name;
        boolean initParam;

        if ((value = request.getParameter("TestNullName")) != null) {
            try {
                if (value.equals("getAttribute")) {
                    name = context.getAttribute(nullAttributeName);
                } else if (value.equals("getInitParameter")) {
                    name = context.getInitParameter(nullAttributeName);
                } else if (value.equals("setAttribute")) {
                    context.setAttribute(nullAttributeName, "DummyValue");
                } else if (value.equals("setInitParameter")) {
                    //This is technically not correct.  context.setInitParameter CAN NOT be called directly from inside a servlet (you will get
                    // IllegalStateException when doing so).  It can only be
                    //called during the application startup (via onStartup() of SCI or ServletContextListener.contextInitialized())
                    //However, since the new check for Null name is at the very top of setInitParameter, we can get away with that because
                    //it will throw NPE immediately before even going down the path which eventually throws IllegalStateException.
                    initParam = context.setInitParameter(nullAttributeName, "DummyValue");
                } else if (value.equals("setGetValidAttribute")) {
                    context.setAttribute("attName", "Servlet4");
                    if (context.getAttribute("attName").toString().equals("Servlet4")) {
                        sos.println("Test [" + value + "]: setAttribute and getAttribute, PASS");
                    } else {
                        sos.println("Test [" + value + "]: setAttribute and getAttribute, FAIL");
                    }
                    return;
                }
            } catch (NullPointerException e) {
                sos.println("Test [" + value + "]: Caught expected NPE, PASS, print e ->" + e);
                return;
            } catch (Exception e) {
                sos.println(value + ": Exception is not NPE type, FAIL");
                return;
            }

            sos.println(value + ": no exception but expecting one, FAIL");
        } else {
            sos.println("Hello World");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
