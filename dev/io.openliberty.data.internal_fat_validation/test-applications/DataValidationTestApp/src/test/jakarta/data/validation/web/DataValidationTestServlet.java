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
package test.jakarta.data.validation.web;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.inject.Inject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.Test;

import componenttest.app.FATServlet;
import test.jakarta.data.validation.web.Entitlement.Frequency;

@DataSourceDefinition(name = "java:module/jdbc/DerbyDataSource",
                      className = "org.apache.derby.jdbc.EmbeddedXADataSource",
                      databaseName = "memory:testdb",
                      user = "dbuser1",
                      password = "dbpwd1",
                      properties = "createDatabase=create")
@SuppressWarnings("serial")
@WebServlet("/*")
public class DataValidationTestServlet extends FATServlet {

    @Inject
    Entitlements entitlements; // for @Entity from Jakarta Persistence

    Validator validator;

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
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    /**
     * Attempt to save a Jakarta Persistence entity that violated constraints for Pattern.
     */
    @Test
    public void testSaveInvalidPatternPersistenceEntity(HttpServletRequest request, PrintWriter response) {
        Entitlement e = new Entitlement(2, "MEDICARE", "person1@openliberty.io", Frequency.AS_NEEDED, 65, null, null, null);
        Set<?> violations = Collections.emptySet();
        try {
            entitlements.save(e);
            // TODO omit the next line once Jakarta Data provider does the validation for us
            violations = validator.validate(e);
        } catch (ConstraintViolationException x) {
            violations = x.getConstraintViolations();
        }
        assertEquals(violations.toString(), 1, violations.size());
    }

    /**
     * Save a Jakarta Persistence entity that has no constraint violations.
     */
    @Test
    public void testSaveValidPersistenceEntity(HttpServletRequest request, PrintWriter response) {
        Entitlement e = new Entitlement(1, "US-SOCIALSECURITY", "person2@openliberty.io", Frequency.MONTHLY, 62, null, Float.valueOf(0), BigDecimal.valueOf(4555));
        Set<?> violations = Collections.emptySet();
        try {
            entitlements.save(e);
            // TODO the following should be unnecessary once it is done by the Jakarta Data provider:
            violations = validator.validate(e);
        } catch (ConstraintViolationException x) {
            violations = x.getConstraintViolations();
        }
        assertEquals(Collections.EMPTY_SET, violations);
    }
}
