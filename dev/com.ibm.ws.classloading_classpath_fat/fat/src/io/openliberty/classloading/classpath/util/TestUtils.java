/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.classloading.classpath.util;

import static io.openliberty.classloading.classpath.fat.FATSuite.EJB_LIB1_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.EJB_LIB2_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.EJB_LIB3_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.LIB10_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.LIB11_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.LIB12_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.LIB13_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.LIB14_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.LIB15_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.LIB16_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.LIB17_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.LIB1_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.LIB2_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.LIB3_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.LIB4_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.LIB5_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.LIB6_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.LIB7_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.LIB8_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.LIB9_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.RAR_LIB1_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.RAR_LIB2_CLASS_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import junit.framework.AssertionFailedError;

/**
 *
 */
public class TestUtils {

    /**
     * @param resource
     * @param testClassPath1App
     */
    public static void assertCommonResourceFromArchive(Class<?> clazz, String expected) {
        URL resource = clazz.getResource("/io/openliberty/classloading/test/resources/common.properties");
        assertNotNull("No resource found for expected: " + expected, resource);
        assertEquals("Wrong resource found", expected, readFromArchive(resource));
    }

    private static String readFromArchive(URL resource) {
        try (InputStream in = resource.openStream()) {
            Properties testProps = new Properties();
            testProps.load(in);
            return testProps.getProperty("from.archive");
        } catch (IOException e) {
            throw createAssertionFailedError("Error reading from resource: " + resource, e);
        }
    }

    private static AssertionFailedError createAssertionFailedError(String msg, Throwable t) {
        return (AssertionFailedError) new AssertionFailedError(msg).initCause(t);
    }
    /**
     * @param class1
     * @param expectedOrder
     */
    public static void assertCommonResourceFromArchives(Class<?> clazz, List<String> expectedOrder) {
        List<URL> urls;
        try {
            urls = Collections.list(clazz.getClassLoader().getResources("/io/openliberty/classloading/test/resources/common.properties"));
        } catch (IOException e) {
            throw createAssertionFailedError("Error getting resources", e);
        }

        int i = 0;
        for (; i < expectedOrder.size(); i++) {
            assertTrue("No more resources found to match i=" + i + " for: " + expectedOrder.get(i), i < urls.size());
            assertEquals("Wrong resource found for i=" + i, expectedOrder.get(i), readFromArchive(urls.get(i)));
        }

        if (i < urls.size()) {
            fail("Found more URLs than expected: " + urls.subList(i, urls.size()));
        }
    }

    public static void assertLoadClass(Class<?> fromClass, String className, ClassLoader expectedLoader) {
        try {
            Class<?> loaded = Class.forName(className, false, fromClass.getClassLoader());
            if (expectedLoader != null) {
                assertEquals("Wrong classloader for class: " + loaded, expectedLoader, loaded.getClassLoader());
            }
        } catch (ClassNotFoundException e) {
            throw createAssertionFailedError("Error Loading class: " + className, e);
        }
    }

    public static Class<?> assertLoadClassNotLoadedWithLoaders(Class<?> fromClass, String className, ClassLoader... unexpectedLoaders) {
        try {
            Class<?> loaded = Class.forName(className, false, fromClass.getClassLoader());
            if (unexpectedLoaders != null) {
                for (ClassLoader unexpected : unexpectedLoaders) {
                    assertNotSame("Unexpected Classloader", unexpected, loaded.getClassLoader());
                }
            }
            return loaded;
        } catch (ClassNotFoundException e) {
            throw createAssertionFailedError("Error Loading class: " + className, e);
        }
    }

    public static void assertNotLoadClass(Class<?> fromClass, String className) {
        try {
            Class<?> loaded = Class.forName(className, false, fromClass.getClassLoader());
            throw createAssertionFailedError("Should have failed to load class: " + loaded, null);
        } catch (ClassNotFoundException e) {
            // expected
        }
    }

    public static enum TEST_CLASS_LOAD {
        testLoadEJB1Class(EJB_LIB1_CLASS_NAME),
        testLoadEJB2Class(EJB_LIB2_CLASS_NAME),
        testLoadEJB3Class(EJB_LIB3_CLASS_NAME),
        testLoadLibrary1Class(LIB1_CLASS_NAME),
        testLoadLibrary2Class(LIB2_CLASS_NAME),
        testLoadLibrary3Class(LIB3_CLASS_NAME),
        testLoadLibrary4Class(LIB4_CLASS_NAME),
        testLoadLibrary5Class(LIB5_CLASS_NAME),
        testLoadLibrary6Class(LIB6_CLASS_NAME),
        testLoadLibrary7Class(LIB7_CLASS_NAME),
        testLoadLibrary8Class(LIB8_CLASS_NAME),
        testLoadLibrary9Class(LIB9_CLASS_NAME),
        testLoadLibrary10Class(LIB10_CLASS_NAME),
        testLoadLibrary11Class(LIB11_CLASS_NAME),
        testLoadLibrary12Class(LIB12_CLASS_NAME),
        testLoadLibrary13Class(LIB13_CLASS_NAME),
        testLoadLibrary14Class(LIB14_CLASS_NAME),
        testLoadLibrary15Class(LIB15_CLASS_NAME),
        testLoadLibrary16Class(LIB16_CLASS_NAME),
        testLoadLibrary17Class(LIB17_CLASS_NAME),
        testLoadRARLib1Class(RAR_LIB1_CLASS_NAME),
        testLoadRARLib2Class(RAR_LIB2_CLASS_NAME);

        /**
         * @param ejbLib1ClassName
         */
        TEST_CLASS_LOAD(String className) {
            this.className = className;
        }

        private final String className;
        public void testLoadClass(TEST_LOAD_RESULT expected, Class<?> fromClass) {
            switch (expected) {
                case failure:
                    assertNotLoadClass(fromClass, className);
                    break;
                case success_fromEARLoader:
                    assertLoadClass(fromClass, className, fromClass.getClassLoader().getParent());
                    break;
                case success_fromWARLoader:
                    assertLoadClass(fromClass, className, fromClass.getClassLoader());
                    break;
                case success_fromLIBLoader:
                    // TODO get the library loader?
                    assertLoadClassNotLoadedWithLoaders(fromClass, className, fromClass.getClassLoader());
                default:
                    break;
            }
        }
    }

    public static enum TEST_LOAD_RESULT {
        success_fromEARLoader,
        success_fromWARLoader,
        success_fromLIBLoader,
        failure
    }
}
