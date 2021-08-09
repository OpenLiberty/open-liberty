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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MyFilter implements Filter {
    /*
     * (non-Java-doc)
     * 
     * @see java.lang.Object#Object()
     */
    public MyFilter() {
        super();
    }

    /*
     * (non-Java-doc)
     * 
     * @see javax.servlet.Filter#init(FilterConfig arg0)
     */
    @Override
    public void init(FilterConfig arg0) throws ServletException {
        // TODO Auto-generated method stub
    }

    /*
     * (non-Java-doc)
     * 
     * @see javax.servlet.Filter#doFilter(ServletRequest arg0, ServletResponse arg1, FilterChain arg2)
     */
    @Override
    public void doFilter(ServletRequest arg0, ServletResponse arg1, FilterChain filterChain) throws IOException, ServletException {

        if (true) {
            throw new NullPointerException("PIKK");
        }

        HttpServletRequest request = (HttpServletRequest) arg0;
        HttpServletResponse response = (HttpServletResponse) arg1;

        String param = request.getParameter("param");
        System.out.println("MyFilter called with param: " + param);
        //filterChain.doFilter(request, response);

        if (param == null || param.trim().length() == 0) {
            // Terminate request with a page
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.print("Filter page as a result of no param");
            out.close();
        } else if ("500".equals(param)) {
            sendError(request, response, 500, new NullPointerException("NullPointerException generated"));
        } else if ("ServletException".equals(param)) {
            //sendError(request, response, 500, new ServletException("ServletException generated") );
            throw new ServletException("Bla bla");
        } else if ("MyException".equals(param)) {
            //sendError(request, response, 500, new MyException("ServletException generated") );
            throw new ServletException("Bla bla", new MyException("ServletException generated"));
        } else if ("MySubException".equals(param)) {
            throw new ServletException("Bla bla", new MySubException("ServletException generated"));
            //sendError(request, response, 500, new MySubException("ServletException generated") );
        } else {
            filterChain.doFilter(request, response);
        }

    }

    /**
     * Copy of WebWorks DispatcherUtils method.
     * 
     * @param request
     * @param response
     * @param code
     * @param e
     */
    public void sendError(HttpServletRequest request, HttpServletResponse response, int code, Exception e) {
        try {
            // send a http error response to use the servlet defined error handler
            // make the exception availible to the web.xml defined error page
            request.setAttribute("javax.servlet.error.exception", e);

            // for compatibility
            request.setAttribute("javax.servlet.jsp.jspException", e);

            // send the error response
            response.sendError(code, e.getMessage());
        } catch (IOException e1) {
            // we're already sending an error, not much else we can do if more stuff breaks
        }
    }

    /*
     * (non-Java-doc)
     * 
     * @see javax.servlet.Filter#destroy()
     */
    @Override
    public void destroy() {
        // TODO Auto-generated method stub
    }

}