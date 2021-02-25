/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
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
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

/**
 * Servlet used to test request timing mxbean
 */
public class RequestTimingServlet extends HttpServlet {

    private static final long serialVersionUID = -6326621784513714486L;
    private static final String REQUESTMXBEAN = "WebSphere:type=RequestTimingStats,name=Servlet";
    public static MBeanServer mbeanConn = ManagementFactory.getPlatformMBeanServer();
    // static RequestTimingStatsMXBean mbean;

    /** Used to sleep threads in servlet tests */
    private static CountDownLatch latch = null;

    /**
     * Indicates if we have already created tables for this tests.
     */
    private static boolean initialized;

    public RequestTimingServlet() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");
        String testName = request.getParameter("testName");
        try {
            this.getClass().getMethod(testName, HttpServletRequest.class, HttpServletResponse.class).invoke(this,
                                                                                                            request, response);
        } catch (Exception e) {
            PrintWriter pw = response.getWriter();
            pw.println(testName + ": Unexpected Exception: " + e.getMessage());
            e.printStackTrace(pw);
        }
    }

    /**
     * Handles requests from testActiveRequestsToServer test in
     * RequestTimingMbeanTest class Waits at threads at CountDownLatch and writes
     * message to response
     * 
     * @param request
     * @param response
     * @throws Exception
     */
    public void TestRequestHandler(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter pw = response.getWriter();
        long threadId = Thread.currentThread().getId();
        pw.println("TestRequestHandler reached in TestServlet  Thread: " + threadId);
        if (latch != null) {
            latch.countDown();
            latch.await();
        }
    }

    /**
     * Sets countDownLatch to value found in HTTP request
     * 
     * @param request
     * @param response
     * @throws Exception
     */
    public void setCountDownLatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log("Entry");
        PrintWriter pw = response.getWriter();
        int lc = Integer.parseInt(request.getParameter("value"));
        latch = new CountDownLatch(lc);
        pw.println("CountDownLatch set to " + lc);
        log("Exit");
    }

    /**
     * This function does a sleep in a loop for one second while the countDownLatch
     * value is greater than countToFind. After, 60 seconds/iterations, it throws an
     * InterruptedException. If an InterruptedException is thrown, fail the test.
     * 
     * @param request
     * @param response
     * @throws Exception
     */
    public void waitForCountDownLatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log("Entry");
        PrintWriter pw = response.getWriter();
        int countToFind = Integer.parseInt(request.getParameter("value"));
        int SECONDS_TO_TIMEOUT = 60;
        int loops = 0;
        pw.println("waitForCountDownLatch to be " + countToFind);
        while (latch.getCount() > countToFind) {
            Thread.sleep(1000);
            loops += 1;
            if (loops == SECONDS_TO_TIMEOUT) {
                throw new InterruptedException("waitForCountDownLatch: maximum wait time reached while waiting CountDownLatch value "
                                               + countToFind);
            }
        }
        log("Exit");
    }

    /**
     * Function will decrement CountDownLatch to 0 so it releases any waiting
     * threads. Used for unblocking threads if FAT test fails.
     * 
     * @param request
     * @param response
     * @throws Exception
     */
    public void unblockThreads(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter pw = response.getWriter();
        while (latch != null && latch.getCount() > 0) {
            latch.countDown();
        }
        pw.println("cleanUpThreads has set countdown latch to 0");
    }

    /**
     * Function to test the mbean registration using the mbean server connection
     * 
     * @param request
     * @param response
     * @throws Exception
     */
    public void TestRegistration(HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.getWriter().println(mbeanConn.isRegistered(new ObjectName(REQUESTMXBEAN)));
    }

    /**
     * Function to retrieve the count based on the parameter value
     * 
     * @param request
     * @param response
     * @throws Exception
     */
    public void getCount(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log("Entry");
        PrintWriter pw = response.getWriter();
        String countType = request.getParameter("value");
        long count = (long) mbeanConn.getAttribute(new ObjectName(REQUESTMXBEAN), countType);
        pw.println(count);
    }

    /**
     * Test storing objects in session storage
     * 
     * @param request
     * @param response
     * @throws Exception
     */
    public void testSessionRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter pw = response.getWriter();
        Person person1 = new Person("Regina George", 18, "01/01/2004");
        HttpSession cSession = request.getSession(true);
        cSession.setAttribute("person1", person1);
        Person person2 = (Person) cSession.getAttribute("person1");
        if (person1.getName() != person2.getName()) {
            throw new Exception("Did not get expected Person object from session storage");
        }
        pw.println("testSessionRequest found the correct person from session storage name=" + person2.getName());
        if (latch != null) {
            latch.countDown();
            latch.await();
        }
    }

    /**
     * Run a basic query to the single database.
     */
    public void testBasicQueryToSingle(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter pw = response.getWriter();
        InitialContext ctx = new InitialContext();
        String dsName = "jdbc/exampleDS";
        DataSource ds = (DataSource) ctx.lookup(dsName);
        Connection con = ds.getConnection();
        try {
            Statement stmt = con.createStatement();
            ResultSet result = stmt.executeQuery("select country from cities where name='Rochester'");
            if (!result.next())
                throw new Exception("Entry missing from database");
            String value = result.getString(1);
            pw.println("testBasicQueryToSingle reached value from dsName=" + dsName
                       + " and found value: " + value);
            if (!"Olmsted".equals(value))
                throw new Exception("Incorrect value: " + value);
        } finally {
            try {
                con.close();
            } finally {
                if (latch != null) {
                    latch.countDown();
                    latch.await();
                }
            }
        }
    }

    /**
     * Create tables used by the tests. This is invoked in the setup of fat bucket
     * to initialize the tables needed for each tests.
     *
     * @param datasource
     */
    public void setUpTables(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter pw = response.getWriter();
        InitialContext ctx = new InitialContext();
        String dsName = "jdbc/exampleDS";
        DataSource datasource = (DataSource) ctx.lookup(dsName);

        if (!initialized) {
            pw.println("Not intialized setting up tables");
            Connection con = datasource.getConnection();
            try {
                Statement st = con.createStatement();
                try {
                    st.executeUpdate("drop table cities");
                } catch (SQLException x) {
                }
                st.executeUpdate(
                                 "create table cities (name varchar(50) not null primary key, population int, country varchar(30))");
                st.executeUpdate("insert into cities values ('Rochester', 106769, 'Olmsted')");
                st.executeUpdate("insert into cities values ('Poughkeepsie', 106869, 'HydePark')");
            } finally {
                con.close();
            }
            initialized = true;
        }
        pw.println("setUpTables reached. intialized= " + initialized);
    }

    /**
     * Helper class for testing saving objects in session
     */
    public class Person {
        String name;
        int age;
        String dob;

        public Person(String nameIn, int ageIn, String dobIn) {
            this.name = nameIn;
            this.age = ageIn;
            this.dob = dobIn;
        }

        public String getName() {
            return this.name;
        }
    }
}