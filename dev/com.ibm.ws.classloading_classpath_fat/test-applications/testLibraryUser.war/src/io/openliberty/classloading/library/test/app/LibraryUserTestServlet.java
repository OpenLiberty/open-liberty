/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.classloading.library.test.app;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/LibraryUserTestServlet")
public class LibraryUserTestServlet extends FATServlet{

    private static final long serialVersionUID = 1L;

    @Test
    public void testLibraryUser() throws ClassNotFoundException {
        ClassLoader cl1 = (ClassLoader) System.getProperties().get("test.library.user.loader");
        assertNotNull("test.library.user.loader is null", cl1);
        Class<?> lib1Class = Class.forName("io.openliberty.classloading.classpath.test.lib1.Lib1");
        ClassLoader cl2 = lib1Class.getClassLoader();
        assertTrue("Loaders do not match: " + cl1 + " " + cl2, cl1 == cl2);
    }
}
