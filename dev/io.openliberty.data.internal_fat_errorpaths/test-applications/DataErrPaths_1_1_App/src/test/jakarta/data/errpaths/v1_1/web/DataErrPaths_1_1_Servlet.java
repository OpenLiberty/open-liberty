/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package test.jakarta.data.errpaths.v1_1.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import jakarta.annotation.Resource;
import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.data.exceptions.MappingException;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.inject.Inject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.UserTransaction;

import org.junit.Test;

import componenttest.app.FATServlet;

@DataSourceDefinition(name = "java:comp/jdbc/H2",
                      className = "org.h2.jdbcx.JdbcDataSource",
                      url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
                      user = "dbuser1",
                      password = "dbpwd1")
@SuppressWarnings("serial")
@WebServlet("/*")
public class DataErrPaths_1_1_Servlet extends FATServlet {

    @Inject
    Operations ops;

    @Resource
    UserTransaction tx;

    /**
     * Initialize the database with some data that other tests can try to read.
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        ops.define(Operation.of("addition", 2, Character.valueOf('+')));
        ops.define(Operation.of("subtraction", 2, Character.valueOf('-')));
        ops.define(Operation.of("multiplication", 2, Character.valueOf('⨉')));
        ops.define(Operation.of("division", 2, Character.valueOf('÷')));
        ops.define(Operation.of("square", 1, Character.valueOf('²')));
        ops.define(Operation.of("square root", 1, Character.valueOf('√')));
        ops.define(Operation.of("cube", 1, Character.valueOf('³')));
        ops.define(Operation.of("cube root", 1, Character.valueOf('∛')));
        ops.define(Operation.of("pi", 0, Character.valueOf('π')));
        ops.define(Operation.of("equal", 2, Character.valueOf('=')));
        ops.define(Operation.of("greater than", 2, Character.valueOf('>')));
        ops.define(Operation.of("greater than or equal", 2, Character.valueOf('≥')));
        ops.define(Operation.of("less than", 2, Character.valueOf('<')));
        ops.define(Operation.of("less than or equal", 2, Character.valueOf('≤')));
        ops.define(Operation.of("not equal", 2, Character.valueOf('≠')));
        ops.define(Operation.of("element of", 2, Character.valueOf('∈')));
        ops.define(Operation.of("intersection", 2, Character.valueOf('⋂')));
        ops.define(Operation.of("subset of", 2, Character.valueOf('⊂')));
        ops.define(Operation.of("union", 2, Character.valueOf('⋃')));
    }

    /**
     * Verify an error is raised when a total count of elements is requested of
     * a Page that was obtained via a NativeQuery.
     */
    @Test
    public void nativeQueryCountElements() {
        Page<Character> page = ops.binaryOps(PageRequest.ofSize(5));
        assertEquals(true,
                     page.hasContent());
        assertEquals(5L,
                     page.numberOfElements());
        assertEquals(List.of('+', '-', '<', '=', '>'),
                     page.content());
        try {
            long total = page.totalElements();
            fail("Should not be able to count totalElements from a NativeQuery." +
                 " Found " + total);
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                //TODO once NLS is added: !x.getMessage().startsWith("CWWKD????E:") ||
                !x.getMessage().contains("NativeQuery"))
                throw x;
        }

        try {
            long total = page.totalPages();
            fail("Should not be able to count totalPages from a NativeQuery." +
                 " Found " + total);
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                //TODO once NLS is added: !x.getMessage().startsWith("CWWKD????E:") ||
                !x.getMessage().contains("NativeQuery"))
                throw x;
        }
    }
}
