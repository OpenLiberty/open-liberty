/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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
package test.jakarta.data.nosql.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/*")
public class DataNoSQLServlet extends FATServlet {
    private static final long serialVersionUID = 1L;
    private static final long TIMEOUT_MINUTES = 2L;

    @Inject
    Employees employees;

    /**
     * Verify that implementation of a repository class can be injected.
     */
    @Test
    public void testInjectRepository() {
        assertNotNull(employees);
    }

    /**
     * Load classes from the Jakarta NoSQL mapping layer.
     */
    @Test
    public void testMappingLayerAvailable() throws ClassNotFoundException {
        Class.forName("jakarta.nosql.Entity");
        Class.forName("jakarta.nosql.column.ColumnTemplate");
        Class.forName("jakarta.nosql.document.DocumentTemplate");
        Class.forName("jakarta.nosql.keyvalue.KeyValueTemplate");

    }

    /**
     * Basic test for using jNoSQL as a Jakarta Data implementation.
     * Creates three entities, saves them, and tests a query method.
     *
     * @throws Exception
     */
    @Test
    public void testBasicQuery() throws Exception {
        Employee mark = new Employee(10L, "Mark", "BasicTest", "Engineer", "Rochester", 2010, 35, 60f);
        Employee dan = new Employee(11L, "Dan", "BasicTest", "Engineer", "Rochester", 2010, 35, 50f);
        Employee scott = new Employee(12L, "Scott", "BasicTest", "Engineer", "Rochester", 2010, 35, 80f);

        employees.save(mark);
        employees.save(dan);
        employees.save(scott);

        assertEquals(Stream.of("Mark", "Dan")
                        .sorted(Comparator.naturalOrder())
                        .collect(Collectors.toList()),
                     employees.findByWageLessThanEqual(70f)
                                     .map(c -> c.firstName)
                                     .sorted(Comparator.naturalOrder())
                                     .collect(Collectors.toList()));
    }
}
