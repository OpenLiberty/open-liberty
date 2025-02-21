/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.classloading.lib.path.test.app;

import static io.openliberty.classloading.classpath.util.TestUtils.TEST_LOAD_RESULT.success_fromLIBLoader;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.classloading.classpath.util.TestUtils.TEST_CLASS_LOAD;
import io.openliberty.classloading.classpath.util.TestUtils.TEST_LOAD_RESULT;

@WebServlet("/LibPathTestServlet")
public class LibPathTestServlet extends FATServlet{

    private static final long serialVersionUID = 1L;

    private void runTest(TEST_LOAD_RESULT expected) {
        TEST_CLASS_LOAD.valueOf(getTestMethod()).testLoadClass(expected, getClass());
    }

    @Test
    public void testLoadLibrary6Class() {
        runTest(success_fromLIBLoader);
    }

    @Test
    public void testLoadLibrary7Class() {
        runTest(success_fromLIBLoader);
    }

    @Test
    public void testLoadLibrary8Class() {
        runTest(success_fromLIBLoader);
    }

    @Test
    public void testLoadLibrary9Class() {
        runTest(success_fromLIBLoader);
    }
}
