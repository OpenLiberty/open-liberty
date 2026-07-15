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
package io.openliberty.classloading.trace.war;

import javax.servlet.annotation.WebServlet;

import componenttest.app.FATServlet;
import io.openliberty.classloading.classpath.test.ejb1.EjbLib1;
import io.openliberty.classloading.classpath.test.lib1.Lib1;
import io.openliberty.classloading.classpath.test.lib12.Lib12;
import io.openliberty.classloading.classpath.test.lib17.Lib17;
import io.openliberty.classloading.classpath.test.lib2.Lib2;
import io.openliberty.classloading.classpath.test.rar1.RarLib1;
import io.openliberty.classloading.classpath.test.rar1.RarLib2;
import io.openliberty.classloading.trace.lib.TestLibraryClass;
import test.bundle.api1.a.API_A1;
import test.bundle.api1.b.API_B1;
import test.bundle.api1.c.API_C1;
import test.bundle.api2.a.API_A2;
import test.bundle.api2.b.API_B2;
import test.bundle.api2.c.API_C2;

/**
 * Test servlet that loads a class from a library to verify classloading trace
 */
@WebServlet("/TraceTestServlet")
public class TraceTestServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    /**
     * Test method that loads a class from the library
     * This will trigger classloading trace messages
     */
    public void testLoadLibraryClass() {
        // Load the library class - this should generate trace output
        TestLibraryClass libClass = new TestLibraryClass();
        String message = libClass.getMessage();
        
        // Verify the class was loaded successfully
        if (message == null || !message.contains("TestLibraryClass")) {
            throw new RuntimeException("Failed to load TestLibraryClass from library");
        }
    }

    /**
     * Test method that loads classes from TEST_LIB1_JAR module
     * This includes Lib1 and API classes (API_A1, API_B1, API_C1)
     */
    public void testLoadLib1Classes() {
        // Load Lib1 class from TEST_LIB1_JAR
        Lib1 lib1 = new Lib1();
        String lib1Message = lib1.getMessage();
        if (lib1Message == null || !lib1Message.contains("Lib1")) {
            throw new RuntimeException("Failed to load Lib1 from TEST_LIB1_JAR");
        }

        // Load API classes from TEST_LIB1_JAR
        API_A1 apiA1 = new API_A1();
        API_B1 apiB1 = new API_B1();
        API_C1 apiC1 = new API_C1();
        
        if (apiA1.getMessage() == null || apiB1.getMessage() == null || apiC1.getMessage() == null) {
            throw new RuntimeException("Failed to load API classes from TEST_LIB1_JAR");
        }
    }

    /**
     * Test method that loads classes from TEST_LIB2_JAR (EAR lib/)
     * This includes Lib2 and API classes (API_A2, API_B2, API_C2)
     */
    public void testLoadLib2Classes() {
        // Load Lib2 class from TEST_LIB2_JAR
        Lib2 lib2 = new Lib2();
        String lib2Message = lib2.getMessage();
        if (lib2Message == null || !lib2Message.contains("Lib2")) {
            throw new RuntimeException("Failed to load Lib2 from TEST_LIB2_JAR");
        }

        // Load API classes from TEST_LIB2_JAR
        API_A2 apiA2 = new API_A2();
        API_B2 apiB2 = new API_B2();
        API_C2 apiC2 = new API_C2();
        
        if (apiA2.getMessage() == null || apiB2.getMessage() == null || apiC2.getMessage() == null) {
            throw new RuntimeException("Failed to load API classes from TEST_LIB2_JAR");
        }
    }

    /**
     * Test method that loads classes from TEST_EJB1_JAR module
     * This includes EjbLib1 class
     */
    public void testLoadEjbClasses() {
        // Load EjbLib1 class from TEST_EJB1_JAR
        EjbLib1 ejbLib1 = new EjbLib1();
        String ejbMessage = ejbLib1.getMessage();
        
        if (ejbMessage == null || !ejbMessage.contains("EjbLib1")) {
            throw new RuntimeException("Failed to load EjbLib1 from TEST_EJB1_JAR");
        }
    }

    /**
     * Test method that loads classes from TEST_RAR1_RAR module
     * This includes RarLib1, RarLib2, Lib12, and Lib17
     */
    public void testLoadRarClasses() {
        // Load RarLib1 from TEST_RAR1_RAR
        RarLib1 rarLib1 = new RarLib1();
        String rarLib1Message = rarLib1.getMessage();
        if (rarLib1Message == null || !rarLib1Message.contains("RarLib1")) {
            throw new RuntimeException("Failed to load RarLib1 from TEST_RAR1_RAR");
        }

        // Load RarLib2 from TEST_RAR1_RAR
        RarLib2 rarLib2 = new RarLib2();
        String rarLib2Message = rarLib2.getMessage();
        if (rarLib2Message == null || !rarLib2Message.contains("RarLib2")) {
            throw new RuntimeException("Failed to load RarLib2 from TEST_RAR1_RAR");
        }

        // Load Lib12 from TEST_RAR1_RAR (in testlib/ directory)
        Lib12 lib12 = new Lib12();
        String lib12Message = lib12.getMessage();
        if (lib12Message == null || !lib12Message.contains("Lib12")) {
            throw new RuntimeException("Failed to load Lib12 from TEST_RAR1_RAR");
        }

        // Load Lib17 from TEST_RAR1_RAR (embedded in testResourceAdaptor.jar)
        Lib17 lib17 = new Lib17();
        String lib17Message = lib17.getMessage();
        if (lib17Message == null || !lib17Message.contains("Lib17")) {
            throw new RuntimeException("Failed to load Lib17 from TEST_RAR1_RAR");
        }
    }
}
