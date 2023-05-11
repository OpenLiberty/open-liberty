/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
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

import static org.junit.Assert.assertNotNull;

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
     * Verify that implementation of a repository class can be injected. It won't be usable yet.
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
     * TODO refactor to a more useful test
     *
     * @throws Exception
     */
    //TODO enable when able to save entities
    //@Test
    public void testBasicNoSql() throws Exception {
        Employee e = new Employee(10L, "Irene", "BasicTest", "Engineer", "Rochester", 2010, 35, 60L);

        employees.save(e);

    }
}
