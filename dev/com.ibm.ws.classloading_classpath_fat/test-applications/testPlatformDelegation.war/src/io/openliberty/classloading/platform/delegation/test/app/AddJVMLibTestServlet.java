/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.classloading.platform.delegation.test.app;

import static io.openliberty.classloading.classpath.fat.FATSuite.LIB6_CLASS_NAME;
import static junit.framework.Assert.fail;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/AddJVMLibTestServlet")
public class AddJVMLibTestServlet extends FATServlet{

    private static final long serialVersionUID = 1L;


    @Test
    public void testLoadLibrary6Class() {
        try {
            Class.forName(LIB6_CLASS_NAME);
        } catch (ClassNotFoundException e) {
            fail("Could not load library class configured with the JVM: " + e);
        }
    }

}
