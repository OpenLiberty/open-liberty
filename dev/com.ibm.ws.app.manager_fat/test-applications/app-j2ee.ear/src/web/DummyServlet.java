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
package web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class for Servlet: DummyServlet
 *
 * @web.servlet
 *              name="DummyServlet"
 *              display-name="DummyServlet"
 *
 * @web.servlet-mapping
 *                      url-pattern="/DummyServlet"
 * 
 */
public class DummyServlet extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {
    /**  */
    private static final long serialVersionUID = 1L;

    /*
     * (non-Java-doc)
     * 
     * @see javax.servlet.http.HttpServlet#HttpServlet()
     */
    public DummyServlet() {
        super();
    }

    /*
     * (non-Java-doc)
     * 
     * @see javax.servlet.http.HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doIt(request, response);
    }

    /*
     * (non-Java-doc)
     * 
     * @see javax.servlet.http.HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doIt(request, response);
    }

    private void doIt(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String param = request.getParameter("param");
        if (param == null) {
            param = "";
        }
        System.out.println("DummyServlet called with param = " + param);

        if ("RuntimeException".equals(param)) {
            throw new NullPointerException("Generated NullPointerException");
        } else if ("MyRuntimeException".equals(param)) {
            throw new MyRuntimeException("Generated MyRuntimeException");
        } else if ("MyException".equals(param)) {
            throw new ServletException("Generated ServletException with MyException cause", new MyException("Generated MyException as cause in ServletException"));
        } else if ("MySubException".equals(param)) {
            throw new ServletException("Generated ServletException with MySubException cause", new MySubException("Generated MySubException as cause in ServletException"));
        } else {
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.write("<html><body>");
            out.write("<h3>For testing this servlet:</h3>");
            out.write("<ul>");
            out.write("<li>RuntimeException --> <a href=\"?param=RuntimeException\">Expected page: RuntimeException page</a></li>");
            out.write("<li>MyRuntimeException --> <a href=\"?param=MyRuntimeException\">Expected page: RuntimeException page</a></li>");
            out.write("<li>MyException --> <a href=\"?param=MyException\">Expected page: MyException page</a></li>");
            out.write("<li>MySubException --> <a href=\"?param=MySubException\">Expected page: MyException page</a></li>");
            out.write("</ul>");
            out.write("</body></html>");
            out.close();
        }

    }
}