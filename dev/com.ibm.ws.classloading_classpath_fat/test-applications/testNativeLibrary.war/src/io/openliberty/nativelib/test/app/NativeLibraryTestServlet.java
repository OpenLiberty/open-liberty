/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.nativelib.test.app;

import static io.openliberty.classloading.classpath.fat.FATSuite.LIB1_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.LIB2_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.LIB3_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.LIB4_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.LIB5_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.LIB6_CLASS_NAME;
import static io.openliberty.classloading.classpath.util.TestUtils.assertFindLibrary;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/NativeLilbraryTestServlet")
public class NativeLibraryTestServlet extends FATServlet{

    private static final long serialVersionUID = 1L;


    @Test
    public void testPathPrivateLibraryNativeFromLibSuccess() {
        // The library class can find its own native from its class loader
        assertFindLibrary(LIB1_CLASS_NAME, "privateNative1", true);
    }

    @Test
    public void testPathPrivateLibraryNativeFromAppSuccess() {
        // The app class can find the native from the private library because it uses the same class loader
        assertFindLibrary(getClass().getName(), "privateNative1", true);
    }

    @Test
    public void testPathCommonLibraryNativeFromLibSuccess() {
        // The library class can find its own native from its class loader
        assertFindLibrary(LIB2_CLASS_NAME, "commonNative2", true);
    }

    @Test
    public void testPathCommonLibraryNativeFormAppFail() {
        // The app class can NOT find the native from the common library because it uses a different class loader
        assertFindLibrary(getClass().getName(), "commonNative2", false);
    }

    @Test
    public void testFilePrivateLibraryNativeFromLibSuccess() {
        // The library class can find its own native from its class loader
        assertFindLibrary(LIB3_CLASS_NAME, "privateNative3", true);
    }

    @Test
    public void testFilePrivateLibraryNativeFromAppSuccess() {
        // The app class can find the native from the private library because it uses the same class loader
        assertFindLibrary(getClass().getName(), "privateNative3", true);
    }

    @Test
    public void testFileCommonLibraryNativeFromLibSuccess() {
        // The library class can find its own native from its class loader
        assertFindLibrary(LIB4_CLASS_NAME, "commonNative4", true);
    }

    @Test
    public void testFileCommonLibraryNativeFormAppFail() {
        // The app class can NOT find the native from the common library because it uses a different class loader
        assertFindLibrary(getClass().getName(), "commonNative4", false);
    }

    @Test
    public void testFileSetPrivateLibraryNativeFromLibSuccess() {
        // The library class can find its own native from its class loader
        assertFindLibrary(LIB5_CLASS_NAME, "privateNative5", true);
    }

    @Test
    public void testFileSetPrivateLibraryNativeFromAppSuccess() {
        // The app class can find the native from the private library because it uses the same class loader
        assertFindLibrary(getClass().getName(), "privateNative5", true);
    }

    @Test
    public void testFileSetCommonLibraryNativeFromLibSuccess() {
        // The library class can find its own native from its class loader
        assertFindLibrary(LIB6_CLASS_NAME, "commonNative6", true);
    }

    @Test
    public void testFileSetCommonLibraryNativeFormAppFail() {
        // The app class can NOT find the native from the common library because it uses a different class loader
        assertFindLibrary(getClass().getName(), "commonNative6", false);
    }
}
