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
import static io.openliberty.classloading.classpath.fat.FATSuite.LIB18_CLASS_NAME;
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 *
 */
public class PrintingUtils {

    public static void printClassPathStuff(Class<?> clazz) {
        synchronized (System.out) {
            System.out.println();
            System.out.println("##### printClassPathStuff ##### " + clazz.getSimpleName());
            printCommonResourceFromArchive(clazz);
            printCommonResourceFromArchives(clazz);
            System.out.println("   ### printLoadClass ###");
            printLoadClass(clazz, LIB1_CLASS_NAME);
            printLoadClass(clazz, LIB2_CLASS_NAME);
            printLoadClass(clazz, LIB3_CLASS_NAME);
            printLoadClass(clazz, LIB4_CLASS_NAME);
            printLoadClass(clazz, LIB5_CLASS_NAME);
            printLoadClass(clazz, LIB6_CLASS_NAME);
            printLoadClass(clazz, LIB7_CLASS_NAME);
            printLoadClass(clazz, LIB8_CLASS_NAME);
            printLoadClass(clazz, LIB9_CLASS_NAME);
            printLoadClass(clazz, LIB10_CLASS_NAME);
            printLoadClass(clazz, LIB11_CLASS_NAME);
            printLoadClass(clazz, LIB12_CLASS_NAME);
            printLoadClass(clazz, LIB13_CLASS_NAME);
            printLoadClass(clazz, LIB14_CLASS_NAME);
            printLoadClass(clazz, LIB15_CLASS_NAME);
            printLoadClass(clazz, LIB16_CLASS_NAME);
            printLoadClass(clazz, LIB17_CLASS_NAME);
            printLoadClass(clazz, LIB18_CLASS_NAME);
            printLoadClass(clazz, EJB_LIB1_CLASS_NAME);
            printLoadClass(clazz, EJB_LIB2_CLASS_NAME);
            printLoadClass(clazz, EJB_LIB3_CLASS_NAME);
            printLoadClass(clazz, RAR_LIB1_CLASS_NAME);
            printLoadClass(clazz, RAR_LIB2_CLASS_NAME);
            System.out.println();
        }
    }

    private static void printCommonResourceFromArchive(Class<?> clazz) {
        System.out.println("   ### printCommonResourceFromArchive ###");
        URL resource = clazz.getResource("/io/openliberty/classloading/test/resources/common.properties");
        printURL(clazz, resource);
    }

    private static void printURL(Class<?> clazz, URL resource) {
        try {
            System.out.println("      " + clazz.getName() + " got resource: " + resource + " with content: " + readFromArchive(resource));
        } catch (IOException e) {
            throw new RuntimeException("Error reading from: " + resource, e);
        }
    }

    private static String readFromArchive(URL resource) throws IOException {
        if (resource == null) {
            return "null";
        }
        try (InputStream in = resource.openStream()) {
            Properties testProps = new Properties();
            testProps.load(in);
            return testProps.getProperty("from.archive");
        }
    }

    private static void printCommonResourceFromArchives(Class<?> clazz) {
        System.out.println("   ### printCommonResourceFromArchives ###");

        List<URL> resources;
        try {
            resources = Collections.list(clazz.getClassLoader().getResources("/io/openliberty/classloading/test/resources/common.properties"));
        } catch (IOException e) {
            throw new RuntimeException("Error getting resources", e);
        }
        resources.forEach((r) -> printURL(clazz, r));
    }

    private static void printLoadClass(Class<?> fromClass, String className) {
        try {
            Class<?> loaded = Class.forName(className, false, fromClass.getClassLoader());
            System.out.println("      From class: " + fromClass.getSimpleName() + " Found class: " + loaded.getSimpleName() + " with class loader: " + loaded.getClassLoader() + " using class loader: " + fromClass.getClassLoader());
        } catch (ClassNotFoundException e) {
            System.out.println("      From class: " + fromClass.getSimpleName() + " class not found: " + className + " using class loader: " + fromClass.getClassLoader());
        }
    }
}
