/*******************************************************************************
 * Copyright (c) 2019, 2025 IBM Corporation and others.
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
package java11.multirelease.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import componenttest.annotation.MaximumJavaLevel;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.app.FATServlet;
import java11.multirelease.jar.MRClass_Overridden;

@SuppressWarnings("serial")
@WebServlet("/MultiReleaseJarTestServlet")
public class MultiReleaseJarTestServlet extends FATServlet {

    public static final String EXPECTED_JAVA_LEVEL = "expectedJavaLevel";
    static final String JAR_PKG = "java11.multirelease.jar.";

    public void testOverriddenClass(HttpServletRequest request, HttpServletResponse response) throws Exception {
        int expectedJavaLevel = Integer.parseInt(request.getParameter(EXPECTED_JAVA_LEVEL));
        assertEquals(expectedJavaLevel, MRClass_Overridden.getJavaVersion());
        if (expectedJavaLevel > 8) {
            String classPath = MRClass_Overridden.class.getName().replace('.', '/') + ".class";
            checkMRresourceAvailable(classPath, expectedJavaLevel);
        }
    }

    @Test
    @MinimumJavaLevel(javaLevel = 9)
    public void testRequireJava9Class() throws Exception {
        checkMRClassAvailable("MRClass_RequireJava09", 9);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 10)
    public void testRequireJava10Class() throws Exception {
        checkMRClassAvailable("MRClass_RequireJava10", 10);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 11)
    public void testRequireJava11Class() throws Exception {
        checkMRClassAvailable("MRClass_RequireJava11", 11);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 12)
    public void testRequireJava12Class() throws Exception {
        checkMRClassAvailable("MRClass_RequireJava12", 12);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 13)
    public void testRequireJava13Class() throws Exception {
        checkMRClassAvailable("MRClass_RequireJava13", 13);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 14)
    public void testRequireJava14Class() throws Exception {
        checkMRClassAvailable("MRClass_RequireJava14", 14);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 15)
    public void testRequireJava15Class() throws Exception {
        checkMRClassAvailable("MRClass_RequireJava15", 15);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 16)
    public void testRequireJava16Class() throws Exception {
        checkMRClassAvailable("MRClass_RequireJava16", 16);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 17)
    public void testRequireJava17Class() throws Exception {
        checkMRClassAvailable("MRClass_RequireJava17", 17);
    }

    @Test
    @MaximumJavaLevel(javaLevel = 8)
    public void testJava9ClassNOTAvailable() throws Exception {
        checkMRClassNOTAvailable("MRClass_RequireJava09");
    }

    @Test
    @MaximumJavaLevel(javaLevel = 9)
    public void testJava10ClassNOTAvailable() throws Exception {
        checkMRClassNOTAvailable("MRClass_RequireJava10");
    }

    @Test
    @MaximumJavaLevel(javaLevel = 10)
    public void testJava11ClassNOTAvailable() throws Exception {
        checkMRClassNOTAvailable("MRClass_RequireJava11");
    }

    @Test
    @MaximumJavaLevel(javaLevel = 11)
    public void testJava12ClassNOTAvailable() throws Exception {
        checkMRClassNOTAvailable("MRClass_RequireJava12");
    }

    @Test
    @MaximumJavaLevel(javaLevel = 12)
    public void testJava13ClassNOTAvailable() throws Exception {
        checkMRClassNOTAvailable("MRClass_RequireJava13");
    }

    @Test
    @MaximumJavaLevel(javaLevel = 13)
    public void testJava14ClassNOTAvailable() throws Exception {
        checkMRClassNOTAvailable("MRClass_RequireJava14");
    }

    @Test
    @MaximumJavaLevel(javaLevel = 14)
    public void testJava15ClassNOTAvailable() throws Exception {
        checkMRClassNOTAvailable("MRClass_RequireJava15");
    }

    @Test
    @MaximumJavaLevel(javaLevel = 15)
    public void testJava16ClassNOTAvailable() throws Exception {
        checkMRClassNOTAvailable("MRClass_RequireJava16");
    }

    @Test
    @MaximumJavaLevel(javaLevel = 16)
    public void testJava17ClassNOTAvailable() throws Exception {
        checkMRClassNOTAvailable("MRClass_RequireJava17");
    }

    @Test
    @MinimumJavaLevel(javaLevel = 9)
    public void testResourceJava9URL() throws Exception {
        checkMRresourceAvailable(9);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 10)
    public void testResourceJava10URL() throws Exception {
        checkMRresourceAvailable(10);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 11)
    public void testResourceJava11URL() throws Exception {
        checkMRresourceAvailable(11);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 12)
    public void testResourceJava12URL() throws Exception {
        checkMRresourceAvailable(12);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 13)
    public void testResourceJava13URL() throws Exception {
        checkMRresourceAvailable(13);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 14)
    public void testResourceJava14URL() throws Exception {
        checkMRresourceAvailable(14);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 15)
    public void testResourceJava15URL() throws Exception {
        checkMRresourceAvailable(15);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 16)
    public void testResourceJava16URL() throws Exception {
        checkMRresourceAvailable(16);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 17)
    public void testResourceJava17URL() throws Exception {
        checkMRresourceAvailable(17);
    }

    private void checkMRClassNOTAvailable(String className) throws Exception {
        System.out.println("Attempting to load " + JAR_PKG + className);
        try {
            Class.forName(JAR_PKG + className);
            fail("Should not be able to load class " + JAR_PKG + className);
        } catch (ClassNotFoundException expected) {
            System.out.println("Got expected CNFE");
        }
    }

    private void checkMRClassAvailable(String className, int expectedVersion) throws Exception {
        // Check that class is loadable in a basic sense
        System.out.println("Attempting to load " + JAR_PKG + className);
        Class<?> MRClass = Class.forName(JAR_PKG + className);

        // Static method on the class should have proper result
        int methodResult = (int) MRClass.getMethod("getJavaVersion").invoke(null);
        assertEquals(expectedVersion, methodResult);

        // Static inner class should also be loadable and accessible
        Class<?> MRStaticInnerClass = Class.forName(JAR_PKG + className + "$MRStaticInnerClass_Overridden");
        methodResult = (int) MRStaticInnerClass.getMethod("getJavaVersion").invoke(null);
        assertEquals(expectedVersion, methodResult);

        // Inner class should be loadable and accessible
        Class<?> MRInnerClass = Class.forName(JAR_PKG + className + "$MRInnerClass_Overridden");
        Object mrClass = MRClass.newInstance();
        Object mrInnerClass = MRInnerClass.getDeclaredConstructor(MRClass).newInstance(mrClass);
        methodResult = (int) MRInnerClass.getMethod("getJavaVersion").invoke(mrInnerClass);
        assertEquals(expectedVersion, methodResult);
    }

// All MR classes follow this pattern
//    public class MRClass_RequireJavaXX {
//
//        private static final int JAVA_VERSION = XX;
//
//        public static int getJavaVersion() {
//            return JAVA_VERSION;
//        }
//        public class MRInnerClass_Overridden {
//            public int getJavaVersion() {
//                return JAVA_VERSION;
//            }
//        }
//        public static class MRStaticInnerClass_Overridden {
//            public static int getJavaVersion() {
//                return JAVA_VERSION;
//            }
//        }
//    }

    private void checkMRresourceAvailable(int resourceJavaVersion) throws NumberFormatException, IOException {
        String stringJavaVersion = Integer.toString(resourceJavaVersion);
        if (resourceJavaVersion < 10) {
            stringJavaVersion = "0" + stringJavaVersion;
        }
        String resourcePath = "java11/multirelease/jar/MRClass_RequireJava" + stringJavaVersion + ".class";
        checkMRresourceAvailable(resourcePath, resourceJavaVersion);
    }

    private void checkMRresourceAvailable(String resourcePath, int resourceJavaVersion) throws IOException {
        System.out.println("Attempting to getResource " + resourcePath);
        ClassLoader loader = getClass().getClassLoader();
        URL resource = loader.getResource(resourcePath);
        assertNotNull("No resource found: " + resourcePath, resource);
        System.out.println("Found resource: " + resource.toExternalForm());
        try (InputStream in = resource.openStream()) {
            // do nothing
        }
        String urlPath = resource.getPath();
        int bangSlash = urlPath.indexOf("!/");
        urlPath = urlPath.substring(bangSlash + 2);
        assertTrue("Wrong path for " + urlPath, urlPath.startsWith("META-INF/versions/" + resourceJavaVersion));
    }

}
