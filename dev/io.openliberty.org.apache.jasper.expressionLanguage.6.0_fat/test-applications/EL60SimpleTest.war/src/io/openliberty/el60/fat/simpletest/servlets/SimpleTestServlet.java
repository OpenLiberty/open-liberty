/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.el60.fat.simpletest.servlets;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.el.ELProcessor;
import jakarta.servlet.annotation.WebServlet;

/**
 * Servlet for a simple Expression Language 6.0 test.
 *
 * This Servlet doesn't actually test new Expression Language 6.0 function but rather is just
 * a single test to ensure that we run at least one test for this FAT bucket.
 *
 * This Servlet can be removed once we add more Expression Language 6.0 tests.
 */
@WebServlet({ "/SimpleTest" })
public class SimpleTestServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    ELProcessor elp;

    public SimpleTestServlet() {
        super();

        elp = new ELProcessor();
    }

    @Test
    public void testSimple() throws Exception {
        assertTrue("2 + 2 == 4 was not true", elp.eval("2 + 2 == 4"));
    }
}
