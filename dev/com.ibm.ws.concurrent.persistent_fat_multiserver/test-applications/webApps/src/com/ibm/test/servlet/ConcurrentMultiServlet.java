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
package com.ibm.test.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import com.ibm.websphere.concurrent.persistent.PersistentExecutor;

/**
 * Basic Servlet
 */

public class ConcurrentMultiServlet extends HttpServlet {

    private static final long serialVersionUID = -4282328943084578818L;

    /**
     * Message written to servlet to indicate that is has been successfully invoked.
     */
    private static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    @Resource(lookup = "concurrent/myScheduler")
    private PersistentExecutor scheduler;

    @Resource(lookup = "jdbc/testDB", name = "java:module/env/jdbc/testDBRef")
    private DataSource testDB;

    @Resource(lookup = "ActualDefaultPort")
    Integer defaultPort;
    /**
     * Interval in milliseconds between polling for task results.
     */
    private static final long POLL_INTERVAL = 200;

    /**
     * Maximum number of nanoseconds to wait for a task to finish.
     */
    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(1);

    /**
     * Invokes test name found in "test" parameter passed to servlet.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("test");
        PrintWriter out = response.getWriter();
        System.out.println("Getting request for server " + request.getRemotePort());
        System.out.println("Default HTTP Port = " + defaultPort);

        try {
            out.println("ConcurrentMultiServlet is starting " + test + "<br>");
            System.out.println("-----> " + test + " starting");
            getClass().getMethod(test, HttpServletRequest.class, PrintWriter.class).invoke(this, request, out);
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
     * Submit a persistent task to run immediately and verify that it runs successfully.
     */
    public void testExecute(HttpServletRequest request, PrintWriter out) throws Exception {
        int keyIndex = Integer.parseInt(request.getParameter("taskNumber"));
        System.out.println("Begining testExecute with Index " + keyIndex);
        Runnable task = new DBIncrementTask("testExecute" + keyIndex);
        scheduler.execute(task);
        pollForTableEntry("testExecute" + keyIndex, 1);

    }

    /**
     * Utility method that waits for a key/value to appear in the database.
     */
    private void pollForTableEntry(String key, int expectedValue) throws Exception {
        System.out.println("Polling for table entry");
        Integer value = null;
        Connection con = testDB.getConnection();
        try {
            PreparedStatement pstmt = con.prepareStatement("SELECT MYVALUE FROM MYTABLE WHERE MYKEY=?");
            pstmt.setString(1, key);
            for (long start = System.nanoTime(); System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL)) {
                ResultSet result = pstmt.executeQuery();
                if (result.next()) {
                    value = result.getInt(1);
                    if (value != null && value.equals(expectedValue))
                        return;
                } else
                    value = null;
            } ;
        } finally {
            con.close();
        }
        if (value == null)
            throw new Exception("Entry with key " + key + " not found");
        else
            throw new Exception("Unexpected value " + value + " for entry with key " + key);
    }

    /**
     * Initialize the scheduler tables and a table used by the application
     */
    @Override
    public void init() throws ServletException {

        try {
            if (testDB != null) {
                Connection con = testDB.getConnection();

                try {
                    Statement stmt = con.createStatement();
                    try {
                        stmt.executeUpdate("DELETE FROM MYTABLE"); // delete any entries from previous test run
                    } catch (SQLException x) {
                        stmt.executeUpdate("CREATE TABLE MYTABLE (MYKEY VARCHAR(80) NOT NULL PRIMARY KEY, MYVALUE INT)");
                    }
                } finally {
                    con.close();
                }
            } else {
                System.out.println(" Connection is null");
                throw new ServletException("Connection is null");
            }
        } catch (SQLException x) {
            System.out.println("SQL Exception creating connection to the DB " + x.toString());
            throw new ServletException(x);
        }
    }

}