/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
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
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.Statement;

import javax.naming.Context;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

@WebServlet("/UpdateDatabase")
public class UpdateDatabaseServlet extends HttpServlet {
	private static final long serialVersionUID = 2348372610390969412L;

	/**
     * Message written to servlet to indicate that is has been successfully invoked.
     */
    private static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    /**
     * Invokes test name found in "test" parameter passed to servlet.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("test");
        String idString = request.getParameter("taskid");
        PrintWriter out = response.getWriter();

        try {
            out.println(getClass().getSimpleName() + " is starting " + test + "<br>");
            System.out.println("-----> " + test + " starting");
            if (idString == null) {
            	getClass().getMethod(test, PrintWriter.class).invoke(this, out);
            } else {
            	long id = Long.parseLong(idString);
            	getClass().getMethod(test,  PrintWriter.class, Long.class).invoke(this, out, id);
            }
            System.out.println("<----- " + test + " successful");
            out.println(test + " " + SUCCESS_MESSAGE);
        } catch (Throwable x) {
            if (x instanceof InvocationTargetException)
                x = x.getCause();
            System.out.println("<----- " + test + " failed:");
            x.printStackTrace(System.out);
            out.println("<pre>ERROR in " + test + ":");
            x.printStackTrace(out);
            out.println("</pre>");
        } finally {
            out.flush();
            out.close();
        }
    }
    
    /**
     * This is the second part of the retry count wrap test.
     * We'll update the database so that the retry failure count is set to a
     * high value, for our task.
     * 
     * This has to be in a separate part because when it runs, the persistence
     * service will be disabled.  The @Resource annotations in the main servlet
     * will not resolve and the servlet will not load.
     */
    public void testRetryCountWrap_2(PrintWriter out, Long id) throws Exception {
    	Context ic = new javax.naming.InitialContext();
    	DataSource ds = (DataSource)ic.lookup("java:comp/env/jdbc/db");
    	Connection c = ds.getConnection();
    	c.setAutoCommit(true);

    	String query = new String("UPDATE APP.SCHDTASK SET RFAILS=32765 WHERE ID=" + id.toString());
    	System.out.println("Running SQL: " + query);
    	Statement s = c.createStatement();
    	s.execute(query);
    	
    	s.close();
    	c.close();
    }
}
