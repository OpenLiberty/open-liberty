/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLSyntaxErrorException;

import jakarta.annotation.Resource;
import jakarta.data.exceptions.DataException;
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
     * Adds new data to tables. The data must not already exist.
     */
    public void testEntitiesCanBeAdded(HttpServletRequest request, PrintWriter response) {
        assertEquals(false, employees.existsById(111222));
        employees.save(new Employee(111222, "Dan", "TestDropCreateTables", 52));

        assertEquals(false, students.existsById(1234));
        students.save(new Student(1234, "Dylan", "TestDropCreateTables", 12));
    }

    /**
     * Verifies that the entities do not have tables.
     */
    public void testEntitiesDoNotHaveTables(HttpServletRequest request, PrintWriter response) {
        try {
            boolean foundEntry = employees.existsById(111222);
            fail("Employee table should not be found. Contains entry? " + foundEntry);
        } catch (DataException x) {
            boolean expectedException = false;
            for (Throwable cause = x.getCause(); cause != null && !expectedException; cause = cause.getCause())
                expectedException = cause instanceof SQLSyntaxErrorException;
            if (!expectedException)
                throw x;
        }

        try {
            boolean foundEntry = students.existsById(1234);
            fail("Student table should not be found. Contains entry? " + foundEntry);
        } catch (DataException x) {
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
        assertEquals(true, employees.existsById(111222));
        assertEquals(true, students.existsById(1234));
    }

    /**
     * Verifies that entities are not found in the database.
     */
    public void testEntitiesNotFound(HttpServletRequest request, PrintWriter response) {
        assertEquals(false, employees.existsById(111222));
        assertEquals(false, students.existsById(1234));
    }

    /**
     * Verifies that Column annotation from a MappedSuperclass is honored.
     */
    @Test
    public void testMappedSuperclass(HttpServletRequest request, PrintWriter response) {
        employees.save(new Employee(222333, null, "TestMappedSuperclass", 25));
        try {
            employees.save(new Employee(333444, "TestMappedSuperclass", null, 35));
        } catch (DataException x) {
            boolean expectedException = false;
            for (Throwable cause = x.getCause(); cause != null && !expectedException; cause = cause.getCause())
                expectedException = cause instanceof SQLIntegrityConstraintViolationException;
            if (!expectedException)
                throw x;
        }
    }

}
