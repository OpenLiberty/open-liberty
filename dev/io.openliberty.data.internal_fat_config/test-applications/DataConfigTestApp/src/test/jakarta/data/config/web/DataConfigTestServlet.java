/*******************************************************************************
 * Copyright (c) 2023,2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.config.web;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLSyntaxErrorException;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.UserTransaction;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/*")
public class DataConfigTestServlet extends FATServlet {

    @Inject
    Employees employees;

    @Inject
    Students students;

    @Resource
    private UserTransaction tran;

    /**
     * Invokes test name found in "test" parameter passed to servlet.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("testMethod");
        String invoker = request.getParameter("invokedBy");
        PrintWriter out = response.getWriter();

        try {
            out.println(getClass().getSimpleName() + " is starting " + test + "<br>");
            System.out.println("-----> " + test + "(invoked by " + invoker + ") starting");
            getClass().getMethod(test, HttpServletRequest.class, PrintWriter.class).invoke(this, request, out);
            System.out.println("<----- " + test + "(invoked by " + invoker + ") successful");
            out.println(test + " " + FATServlet.SUCCESS);
        } catch (Throwable x) {
            if (x instanceof InvocationTargetException)
                x = x.getCause();
            System.out.println("<----- " + test + "(invoked by " + invoker + ") failed:");
            x.printStackTrace(System.out);
            out.println("<pre>ERROR in " + test + ":");
            x.printStackTrace(out);
            out.println("</pre>");
        } finally {
            out.flush();
            out.close();
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
    }

    /**
     * A servlet operation that doesn't do anything. It can be used when the
     * Jakarta Data feature is not enabled.
     */
    public void testDoNotUseJakartaData(HttpServletRequest request, PrintWriter response) {
    }

    /**
     * Adds new data to tables. The data must not already exist.
     */
    public void testEntitiesCanBeAdded(HttpServletRequest request, PrintWriter response) {
        assertEquals(false, employees.findById(111222).isPresent());
        employees.save(new Employee(111222, "Dan", "TestDropCreateTables", 52));

        assertEquals(false, students.findById(1234).isPresent());
        students.save(new Student(1234, "Dylan", "TestDropCreateTables", 12));
    }

    /**
     * Verifies that the entities do not have tables.
     */
    public void testEntitiesDoNotHaveTables(HttpServletRequest request, PrintWriter response) {
        try {
            Employee found = employees.findById(111222).orElseGet(() -> null);
            assertEquals("Employee table should not be found.", null, found);
        } catch (RuntimeException x) {
            // Allow other methods of the servlet to be used without Jakarta Data enabled
            if (!x.getClass().getName().startsWith("jakarta.data.exceptions."))
                throw x;
            boolean expectedException = false;
            for (Throwable cause = x.getCause(); cause != null && !expectedException; cause = cause.getCause())
                expectedException = cause instanceof SQLSyntaxErrorException;
            if (!expectedException)
                throw x;
        }

        try {
            Student found = students.findById(1234).orElseGet(() -> null);
            assertEquals("Student table should not be found", null, found);
        } catch (RuntimeException x) {
            // Allow other methods of the servlet to be used without Jakarta Data enabled
            if (!x.getClass().getName().startsWith("jakarta.data.exceptions."))
                throw x;
            boolean expectedException = false;
            for (Throwable cause = x.getCause(); cause != null && !expectedException; cause = cause.getCause())
                expectedException = cause instanceof SQLSyntaxErrorException;
            if (!expectedException)
                throw x;
        }
    }

    /**
     * Verifies that entities are found in the database.
     */
    public void testEntitiesFound(HttpServletRequest request, PrintWriter response) {
        assertEquals(true, employees.findById(111222).isPresent());
        assertEquals(true, students.findById(1234).isPresent());
    }

    /**
     * Verifies that entities are not found in the database.
     */
    public void testEntitiesNotFound(HttpServletRequest request, PrintWriter response) {
        assertEquals(false, employees.findById(111222).isPresent());
        assertEquals(false, students.findById(1234).isPresent());
    }

    /**
     * Verifies that Column annotation from a MappedSuperclass is honored.
     */
    @Test
    public void testMappedSuperclass(HttpServletRequest request, PrintWriter response) {
        employees.save(new Employee(222333, null, "TestMappedSuperclass", 25));
        try {
            employees.save(new Employee(333444, "TestMappedSuperclass", null, 35));
        } catch (RuntimeException x) {
            // Allow other methods of the servlet to be used without Jakarta Data enabled
            if (!x.getClass().getName().startsWith("jakarta.data.exceptions."))
                throw x;
            boolean expectedException = false;
            for (Throwable cause = x.getCause(); cause != null && !expectedException; cause = cause.getCause())
                expectedException = cause instanceof SQLIntegrityConstraintViolationException;
            if (!expectedException)
                throw x;
        }
    }

    /**
     * Verifies that the databaseStore tablePrefix is included in the table name when configured.
     */
    @Test
    public void testTablePrefix(HttpServletRequest request, PrintWriter response) throws SQLException {
        employees.save(new Employee(444555, "Thomas", "testTablePrefix", 40));
        students.save(new Student(4567, "Teresa", "testTablePrefix", 20));

        try (Connection con = employees.getDataSource().getConnection()) {
            assertEquals("dbuser1", con.getMetaData().getUserName().toLowerCase());

            ResultSet result = con.createStatement().executeQuery("SELECT firstName FROM TESTemp WHERE employeeId = 444555");
            assertEquals(true, result.next());
            assertEquals("Thomas", result.getString(1));
        }

        try (Connection con = students.getConnection()) {
            assertEquals("dbuser1", con.getMetaData().getUserName().toLowerCase());

            ResultSet result = con.createStatement().executeQuery("SELECT firstName FROM TESTStudent WHERE studentId = 4567");
            assertEquals(true, result.next());
            assertEquals("Teresa", result.getString(1));
        }
    }
}
