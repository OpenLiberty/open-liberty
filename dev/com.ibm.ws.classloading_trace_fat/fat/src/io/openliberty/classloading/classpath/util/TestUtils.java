/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.classloading.classpath.util;

import static io.openliberty.classloading.classpath.fat.FATSuite.EJB_LIB1_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.LIB12_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.LIB17_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.LIB1_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.LIB2_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.RAR_LIB1_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.RAR_LIB2_CLASS_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import junit.framework.AssertionFailedError;

/**
 *
 */
public class TestUtils {

    private static final Method findLibrary;
    private static final Throwable findLibraryException;
    static {
        Method m = null;
        Throwable t = null;
        try {
            m = ClassLoader.class.getDeclaredMethod("findLibrary", String.class);
            m.setAccessible(true);
        } catch (Throwable e) {
            t = e;
        }
        findLibrary = m;
        findLibraryException = t;
    }

    // Classloader type substrings expected inside classloader=[...]
    public static final String APP_CL = "AppClassLoader";
    public static final String PL_CL  = "ParentLastClassLoader";

    // Delegation mode strings
    public static final String PF = "PF"; // parent-first
    public static final String PL = "PL"; // parent-last

    // Domain strings embedded in each hop of the delegation path.
    // A hop has the form: AppClassLoader@<hex>:<domain>:<id>:<mode>
    // For GatewayClassLoader the hop has the form: GatewayClassLoader@<hex>:bundle=[<bundle-name>:0.0.0]
    public static final String DOMAIN_WEB_MODULE    = "WebModule";
    public static final String DOMAIN_EAR           = "EARApplication";
    public static final String DOMAIN_SHARED_LIB    = "Shared Library";
    public static final String DOMAIN_GATEWAY       = "GatewayClassLoader";

    public static final String TRACE_CLASS_DEFINE_SUCCESS  = ".*was successfully defined.*";
    public static final String TRACE_CLASS_LOAD_SUCCESS    = ".*was successfully loaded.*";
    public static final String TRACE_CLASS_DEFINE_FAIL     = ".*failed to be defined.*";
    public static final String TRACE_LOCAL_CLASSPATH       = ".*on the local classpath.*";
    public static final String TRACE_BY_PARENT             = ".*by parent classloader.*";
    public static final String TRACE_BY_COMMON_LIB         = ".*by common library loader.*";
    public static final String TRACE_CLASS_LOAD_FAIL       = ".*failed to load.*";
    public static final String TRACE_LIBERTY_API_PACKAGES  = ".*liberty API packages.*";
    public static final String TRACE_NOT_FOUND             = ".*not found.*";

    public static final String CLASS_REGEX     = "Class=\\[.*";
    public static final String RESOURCE_REGEX  = "Resource=\\[.*";
    public static final String RESOURCES_REGEX = "Resources=\\[.*";

    // Field tokens used in trace verification
    private static final String FIELD_CLASS           = "Class=[";
    private static final String FIELD_CLASSLOADER     = "classloader=[";
    private static final String FIELD_LOCATION        = "location=[";
    public  static final String FIELD_DELEGATION_PATH = "delegation path=[";

    /**
     * Verifies a class-defined-successfully trace line emitted by
     * {@code AppClassLoader.definePackageAndClass}.
     *
     * <p>Expected format:
     * {@code Class=[<name>] was successfully defined; classloader=[<type>@<hex>:...]; location=[<url>]}
     *
     * @param traceLine   raw trace line containing {@value #TRACE_CLASS_DEFINE_SUCCESS}
     * @param className   expected binary class name
     * @param classLoader classloader type substring, e.g. {@value #APP_CL} or {@value #PL_CL}
     * @param location    substring expected inside {@code location=[...]}
     */
    public static void checkClassLoadTrace(String traceLine, String className,
                                           String classLoader, String location) {
        assertNotNull("Expected trace for " + className + " not found", traceLine);
        assertTrue("Trace should contain 'Class=[" + className + "]'", traceLine.contains(FIELD_CLASS + className));
        assertTrue("Trace should contain '" + FIELD_CLASSLOADER + "'", traceLine.contains(FIELD_CLASSLOADER));
        assertTrue("Trace should reference classloader type " + classLoader, traceLine.contains(classLoader));
        if (location != null) {
            assertTrue("Trace should contain '" + FIELD_LOCATION + "'", traceLine.contains(FIELD_LOCATION));
            assertTrue("Trace should reference location " + location, traceLine.contains(location));
        }
        if (classLoader.equals(APP_CL) || classLoader.equals(PL_CL)) {
            String delegationMode = classLoader.equals(PL_CL) ? PL : PF;
            assertTrue("Trace should reference delegation mode " + delegationMode, traceLine.contains(delegationMode));
        }
    }

    /**
     * Verifies a class-define-failed trace line emitted by
     * {@code AppClassLoader.definePackageAndClass} when {@code defineClass()} throws.
     *
     * <p>Expected format:
     * {@code Class=[<name>] failed to be defined; classloader=[<type>@<hex>:...]; location=[<url>]}
     *
     * @param traceLine   raw trace line containing {@value #TRACE_CLASS_DEFINE_FAIL}
     * @param className   expected binary class name
     * @param classLoader classloader type substring, e.g. {@value #APP_CL} or {@value #PL_CL}
     * @param location    substring expected inside {@code location=[...]}
     */
    public static void checkClassFailTrace(String traceLine, String className, String classLoader, String location) {
        assertNotNull("Expected class-define-failed trace for " + className + " not found", traceLine);
        assertTrue("Trace should contain 'Class=[" + className + "]'", traceLine.contains(FIELD_CLASS + className));
        assertTrue("Trace should contain '" + FIELD_CLASSLOADER + "'", traceLine.contains(FIELD_CLASSLOADER));
        assertTrue("Trace should reference classloader type " + classLoader, traceLine.contains(classLoader));
        assertTrue("Trace should contain '" + FIELD_LOCATION + "'", traceLine.contains(FIELD_LOCATION));
        assertTrue("Trace should reference location " + location, traceLine.contains(location));
        if (classLoader.equals(APP_CL) || classLoader.equals(PL_CL)) {
            String delegationMode = classLoader.equals(PL_CL) ? PL : PF;
            assertTrue("Trace should reference delegation mode " + delegationMode, traceLine.contains(delegationMode));
        }
    }

    /**
     * Verifies a {@code getResource()} trace line emitted by any classloader.
     *
     * <p>The actual format depends on where the resource was found:
     * <ul>
     *   <li><b>AppClassLoader / ParentLastClassLoader — found on local classpath:</b><br>
     *       {@code Resource=[<name>] found at location=[<url>] on the local classpath;
     *       classloader=[<cl>]; delegation path=[<path>]}</li>
     *   <li><b>AppClassLoader / ParentLastClassLoader — found via non-AppClassLoader parent:</b><br>
     *       {@code Resource=[<name>] found at location=[<url>] by parent classloader=[<cl>];
     *       delegation path=[<path>]}</li>
     *   <li><b>AppClassLoader / ParentLastClassLoader — not found:</b><br>
     *       {@code Resource=[<name>] not found; classloader=[<cl>]}</li>
     *   <li><b>GatewayClassLoader — found:</b><br>
     *       {@code Resource=[<name>] found at location=[<url>] from liberty API packages
     *       by classloader=[<cl>]}</li>
     *   <li><b>GatewayClassLoader — not found:</b><br>
     *       {@code Resource=[<name>] was not found by classloader=[<cl>]}</li>
     * </ul>
     *
     * @param traceLine    raw trace line; must not be {@code null}
     * @param resourceName substring expected inside {@code Resource=[...]}
     * @param classLoader  classloader type substring, e.g. {@value #APP_CL}, {@value #PL_CL},
     *                     or {@code "GatewayClassLoader"}
     * @param expectFound  if {@code true} asserts a {@code location=[...]} field is present and
     *                     non-null; if {@code false} asserts "not found" appears and no location field
     */
    public static void checkResourceTrace(String traceLine,
                                          String resourceName, String classLoader, boolean expectFound) {
        assertNotNull("Expected resource trace for " + resourceName + " not found", traceLine);
        assertTrue("Trace should reference resource name " + resourceName, traceLine.contains(resourceName));
        assertTrue("Trace should reference classloader type " + classLoader, traceLine.contains(classLoader));
        if (expectFound) {
            assertTrue("Trace should contain 'found at location='", traceLine.contains("found at location=["));
            assertTrue("Trace should contain a non-null location", !traceLine.contains("location=[null]"));
        } else {
            assertTrue("Trace should indicate resource was not found", traceLine.contains("not found"));
        }
        if (classLoader.equals(APP_CL) || classLoader.equals(PL_CL)) {
            String delegationMode = classLoader.equals(PL_CL) ? PL : PF;
            assertTrue("Trace should reference delegation mode " + delegationMode, traceLine.contains(delegationMode));
        }
    }

    /**
     * Verifies a {@code getResources()} trace line emitted by any classloader.
     *
     * <p>The actual format depends on where the resources were found:
     * <ul>
     *   <li><b>AppClassLoader / ParentLastClassLoader — found on local classpath:</b><br>
     *       {@code Resources=[<name>] found at locations=<list> on the local classpath;
     *       classloader=[<cl>]; delegation path=[<path>]}</li>
     *   <li><b>AppClassLoader / ParentLastClassLoader — not found:</b><br>
     *       {@code Resources=[<name>] not found by classloader=[<cl>]}</li>
     *   <li><b>GatewayClassLoader — found:</b><br>
     *       {@code Resources=[<name>] found at locations=<list> from liberty API packages
     *       by classloader=[<cl>]}</li>
     *   <li><b>GatewayClassLoader — not found:</b><br>
     *       {@code Resources=[<name>] not found; classloader=[<cl>]}</li>
     * </ul>
     *
     * @param traceLine    raw trace line; must not be {@code null}
     * @param resourceName substring expected inside {@code Resources=[...]}
     * @param classLoader  classloader type substring, e.g. {@value #APP_CL}, {@value #PL_CL},
     *                     or {@code "GatewayClassLoader"}
     * @param expectFound  if {@code true} asserts a non-empty {@code locations=} list is present;
     *                     if {@code false} asserts "not found" appears and locations list is empty
     */
    public static void checkResourcesTrace(String traceLine,
                                           String resourceName, String classLoader, boolean expectFound) {
        assertNotNull("Expected resources trace for " + resourceName + " not found", traceLine);
        assertTrue("Trace should reference resource name " + resourceName, traceLine.contains(resourceName));
        assertTrue("Trace should reference classloader type " + classLoader, traceLine.contains(classLoader));
        if (expectFound) {
            assertTrue("Trace should contain 'found at locations='", traceLine.contains("found at locations="));
            int locIdx = traceLine.indexOf("locations=") + "locations=".length();
            assertTrue("locations list should not be empty for a found resource",
                       !traceLine.substring(locIdx).startsWith("[]"));
        } else {
            assertTrue("Trace should indicate resources were not found", traceLine.contains("not found"));
        }
        if (classLoader.equals(APP_CL) || classLoader.equals(PL_CL)) {
            String delegationMode = classLoader.equals(PL_CL) ? PL : PF;
            assertTrue("Trace should reference delegation mode " + delegationMode, traceLine.contains(delegationMode));
        }
    }

    /**
     * Verifies that a resource trace line contains a {@code delegation path=[...]} field
     * whose hops match the supplied domain segments in order.
     *
     * <p>The delegation path format is:
     * <pre>
     * delegation path=[AppClassLoader@&lt;hex&gt;:&lt;domain&gt;:&lt;id&gt;:&lt;mode&gt; -> ...]
     * </pre>
     * Each {@code hopDomain} argument is matched against the corresponding hop in the path.
     * For example, to assert the path traverses WAR → EAR → Shared Library:
     * <pre>
     * checkDelegationPath(traceLine, DOMAIN_WEB_MODULE, DOMAIN_EAR, DOMAIN_SHARED_LIB);
     * </pre>
     * To assert a two-hop WAR → EAR path (resource found on EAR local classpath):
     * <pre>
     * checkDelegationPath(traceLine, DOMAIN_WEB_MODULE, DOMAIN_EAR);
     * </pre>
     *
     * @param traceLine  raw trace line containing {@code delegation path=[}
     * @param hopDomains expected domain substrings for each hop, in order
     */
    public static void checkDelegationPath(String traceLine, String... hopDomains) {
        assertNotNull("Trace line must not be null when checking delegation path", traceLine);
        int idx = traceLine.indexOf(FIELD_DELEGATION_PATH);
        assertTrue("Trace line should contain '" + FIELD_DELEGATION_PATH + "': " + traceLine, idx >= 0);
        String pathValue = traceLine.substring(idx + FIELD_DELEGATION_PATH.length(),
                                               traceLine.indexOf(']', idx + FIELD_DELEGATION_PATH.length()));

        String[] hops = pathValue.split(" -> ");
        assertEquals("delegation path should have " + hopDomains.length + " hop(s) but was: [" + pathValue + "]",
                     hopDomains.length, hops.length);
        for (int i = 0; i < hopDomains.length; i++) {
            assertTrue("delegation path hop " + i + " should contain '" + hopDomains[i]
                       + "' but was: [" + hops[i] + "]",
                       hops[i].contains(hopDomains[i]));
        }
    }

    /**
     * @param resource
     * @param testClassPath1App
     */
    public static void assertCommonResourceFromArchive(Class<?> clazz, String expected) {
        assertResourceFromArchive("common.properties", clazz, expected);
    }

    public static void assertResourceFromArchive(String resourceName, Class<?> clazz, String expected) {
        URL resource = clazz.getResource("/io/openliberty/classloading/test/resources/" + resourceName);
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
            assertEquals("Wrong resource found for i=" + i + " urls=" + urls, expectedOrder.get(i), readFromArchive(urls.get(i)));
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

    public static void assertFindLibrary(String fromClassName, String libraryName, boolean succeed) {
        if (findLibraryException != null) {
            throw createAssertionFailedError("findLibrary method error.", findLibraryException);
        }
        String expectedLibraryFileName = System.mapLibraryName(libraryName);
        String resultPath = null;
        try {
            Class<?> fromClass = Class.forName(fromClassName);
            ClassLoader fromLoader = fromClass.getClassLoader();
            resultPath = (String) findLibrary.invoke(fromLoader, libraryName);
        } catch (Throwable e) {
            throw createAssertionFailedError("findLibrary invoke error.", e);
        }

        if (succeed) {
            assertNotNull("No library found: " + libraryName, resultPath);
            assertTrue("Wrong path result to library: " + resultPath, resultPath.endsWith(expectedLibraryFileName));
        } else {
            assertNull("Did not expect to find library: " + libraryName, resultPath);
        }
    }

    public static enum TEST_CLASS_LOAD {
        testLoadEJB1Class(EJB_LIB1_CLASS_NAME),
        testLoadLibrary1Class(LIB1_CLASS_NAME),
        testLoadLibrary2Class(LIB2_CLASS_NAME),
        testLoadLibrary12Class(LIB12_CLASS_NAME),
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
