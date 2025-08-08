/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.classloading.parent.library.inconsistent.test.app;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

/**
 * TODO This is a placeholder test for now.  It doesn't actually assert anything.
 */
@WebServlet("/ParentLibraryInconsistentTestServlet")
public class ParentLibraryInconsistentTestServlet extends FATServlet{

    private static final long serialVersionUID = 1L;

    @Test
    public void testLoadAPIClassFromLibrary() throws ClassNotFoundException {
        // TODO enable this test when it is fixed
        /*
        // try initiating from library first; this should get the class from the API bundle
        assertEquals("Wrong jar used API_A1 library initiating.", "test.bundle.api.jar", API_User.getCodeSourceA1());
        assertEquals("Wrong jar used API_A1 application initiating.", "test.bundle.api.jar", CodeSourceUtil.getClassCodeSourceFileName(Class.forName("test.bundle.api1.a.API_A1")));

        // try initiating from application first; this should also get the class from the API bundle to be consistent
        assertEquals("Wrong jar used API_B1 application initiating.", "test.bundle.api.jar", CodeSourceUtil.getClassCodeSourceFileName(Class.forName("test.bundle.api1.b.API_B1")));
        assertEquals("Wrong jar used API_B1 library initiating.", "test.bundle.api.jar", API_User.getCodeSourceB1());
        */
    }
}
