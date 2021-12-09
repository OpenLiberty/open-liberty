/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.request.timing.app;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.naming.InitialContext;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;

@WebFilter(urlPatterns = "/RequestTimingServlet")
public class TestFilter implements Filter {

    /**
     * Indicates if we have already created tables for this tests.
     */
    static boolean intialized;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request.getParameter("testName").equalsIgnoreCase("testBasicQueryToSingle")) {
            PrintWriter pw = response.getWriter();
            String url = request instanceof HttpServletRequest ? ((HttpServletRequest) request).getRequestURL().toString() : "N/A";
            pw.println("from filter, processing url: " + url);
            try {
                testBasicQueryToSingle(pw);
            } catch (Exception e) {
                pw.println("Exception caught from filter " + e.getMessage());
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }

    /**
     * Run a basic query to the single database.
     */
    public void testBasicQueryToSingle(PrintWriter pw) throws Exception {
        InitialContext ctx = new InitialContext();
        String dsName = "jdbc/exampleDS";
        DataSource ds = (DataSource) ctx.lookup(dsName);
        Connection con = ds.getConnection();

        try {
            // Not longer need to setup tables, it will be done thru a servlet call at init
            Statement stmt = con.createStatement();
            ResultSet result = stmt.executeQuery("select country from cities where name='Poughkeepsie'");
            if (!result.next())
                throw new Exception("Entry missing from database");
            String value = result.getString(1);
            pw.println("filter - testBasicQueryToSingle reached value from dsName=" + dsName + " and found value: "
                       + value);
            if (!"HydePark".equals(value))
                throw new Exception("Incorrect value: " + value);
        } finally {
            con.close();
        }
    }

}
