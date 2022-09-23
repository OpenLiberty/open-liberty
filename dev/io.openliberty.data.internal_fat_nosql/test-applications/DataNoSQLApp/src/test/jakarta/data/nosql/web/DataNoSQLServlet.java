/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
     * Load classes from the Jakarta NoSQL communication layer.
     */
    @Test
    public void testCommunicationLayerAvailable() throws ClassNotFoundException {
        Class.forName("jakarta.nosql.Condition");
        Class.forName("jakarta.nosql.column.Column");
        Class.forName("jakarta.nosql.document.Document");
        Class.forName("jakarta.nosql.keyvalue.KeyValueEntity");
        Class.forName("jakarta.nosql.query.Query");
    }

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
        Class.forName("jakarta.nosql.mapping.Entity");
        Class.forName("jakarta.nosql.mapping.column.ColumnEntityConverter");
        Class.forName("jakarta.nosql.mapping.document.DocumentEntityConverter");
        Class.forName("jakarta.nosql.mapping.keyvalue.KeyValueEntityConverter");
    }
}
