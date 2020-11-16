/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.ormdiagnostics;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.Assert;

import com.ibm.websphere.simplicity.LocalFile;
import com.ibm.websphere.simplicity.log.Log;

/**
 * Utility class that understands the string format JPARuntimeInspector uses to display data
 * and is used to validate that inspector data. If the output changes in the Inspector, then
 * this class needs to be updated as well.
 */
public class ORMIntrospectorHelper {
    private final static String newLine = System.getProperty("line.separator").toString();

    public static void verifyApplications(String appName,
                                          int numORMFiles,
                                          int jpaClasses,
                                          String[] pUnitRoots,
                                          final String input) {
        final String targetString1 = "################################################################################" + newLine +
                                     "Application \"" + appName + "\":" + newLine +
                                     "   Total ORM Files: " + numORMFiles + newLine +
                                     "   Total JPA Involved Classes: " + jpaClasses;

        boolean roots = true;
        for (String pUnitRoot : pUnitRoots) {
            roots = input.contains(pUnitRoot);
        }
        Assert.assertTrue(input.contains(targetString1) && roots);
    }

    public static void verifyPersistenceUnit(final String pUnitName, final String input) {
        final String targetString = "<persistence-unit name=\"" + pUnitName + "\"";
        Assert.assertTrue(input.contains(targetString));
    }

    public static void verifyApplicationArchives(final List<String> expectedArchives, final String input) {
        String content = "   Application Modules and Archives:" + newLine;

        for (String ea : expectedArchives) {
            content += "     " + ea + newLine;
        }

        Assert.assertTrue(input.contains(content));
    }

    public static void verifyPersistentClasses(final List<JPAClass> classes, final String input) {
        boolean roots = true;
        for (JPAClass clazz : classes) {
            final String content = "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -" + newLine +
                                   "Class: " + clazz.getClassName() + newLine +
                                   "Location: " + clazz.getLocation() + newLine +
                                   newLine +
                                   clazz.getContent();
            roots = input.contains(content);
        }
        Assert.assertTrue(roots);
    }

    public static String extractJPAIntrospection(final LocalFile dumpLocalFile) throws Exception {
        final File dumpFile = new File(dumpLocalFile.getAbsolutePath());
        try (ZipFile zip = new ZipFile(dumpFile)) {
            final Enumeration<? extends ZipEntry> entryEnum = zip.entries();
            while (entryEnum.hasMoreElements()) {
                final ZipEntry zipEntry = entryEnum.nextElement();

                if (zipEntry.getName().endsWith("JPARuntimeInspector.txt")) {
                    // Found JPARuntimeIntrospector.txt
                    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    final byte[] buffer = new byte[4096];
                    try (BufferedInputStream bis = new BufferedInputStream(zip.getInputStream(zipEntry))) {
                        int bytesRead = 0;
                        while ((bytesRead = bis.read(buffer)) != -1) {
                            baos.write(buffer, 0, bytesRead);
                        }
                    }

                    Log.info(ORMIntrospectorHelper.class, "extractJPAIntrospection", "Returning Data (" + baos.size() + " bytes)");
                    return baos.toString();
                }
            }
        }

        Log.info(ORMIntrospectorHelper.class, "extractJPAIntrospection", "Failed to find JPARuntimeInspector.txt");
        return null;
    }

    public static class JPAClass {
        private String className;
        private String location;
        private String content;

        public JPAClass(String className, String location, String content) {
            this.className = className;
            this.location = location;
            this.content = content;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
