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

import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB6;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB7;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB8;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB9;
import static io.openliberty.classloading.classpath.util.TestUtils.assertCommonResourceFromArchive;
import static io.openliberty.classloading.classpath.util.TestUtils.assertCommonResourceFromArchives;
import static io.openliberty.classloading.classpath.util.TestUtils.TEST_LOAD_RESULT.success_fromLIBLoader;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URL;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.ServiceLoader;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.classloading.classpath.util.TestUtils.TEST_CLASS_LOAD;
import io.openliberty.classloading.classpath.util.TestUtils.TEST_LOAD_RESULT;

@WebServlet("/PlatformDelegationTestServlet")
public class PlatformDelegationTestServlet extends FATServlet{

    private static final long serialVersionUID = 1L;

    private void runTest(TEST_LOAD_RESULT expected) {
        TEST_CLASS_LOAD.valueOf(getTestMethod()).testLoadClass(expected, getClass());
    }

    @Test
    public void testLoadLibrary6Class() {
        runTest(success_fromLIBLoader);
    }

    @Test
    public void testLoadLibrary7Class() {
        runTest(success_fromLIBLoader);
    }

    @Test
    public void testLoadLibrary8Class() {
        runTest(success_fromLIBLoader);
    }

    @Test
    public void testLoadLibrary9Class() {
        runTest(success_fromLIBLoader);
    }

    @Test
    public void testGetCommonResourcesOrder() {
        List<String> expectedOrder = Arrays.asList(TEST_LIB6, //
                                                   TEST_LIB7, //
                                                   TEST_LIB8, //
                                                   TEST_LIB9);
        assertCommonResourceFromArchives(getClass(), expectedOrder);
    }

    @Test
    public void testGetCommonResource() {
        assertCommonResourceFromArchive(getClass(), TEST_LIB6);
    }

    @Test
    public void testLoadKernelClass() {
        try {
            Class.forName("com.ibm.wsspi.kernel.embeddable.ServerBuilder");
            System.out.println("testLoadKernelClass: CLASS FOUND");
        } catch (ClassNotFoundException e) {
            System.out.println("testLoadKernelClass: CLASS NOT FOUND");
        }
    }
    @Test
    public void testLoadPlatformClassDoesNotExist() {
        // try to load a java.lang class that doesn't exist
        try {
            Class.forName("java.lang.PlatformDelegationTest");
        } catch (ClassNotFoundException e) {
            // expected
        }
    }

    @Test
    public void testLoadPlatformClassDoesExist() throws ClassNotFoundException {
        // try to load a platform class that doesn't exist
        Class.forName("java.util.concurrent.atomic.AtomicReferenceArray");
    }

    @Test
    public void testLoadPlatformXAException() throws ClassNotFoundException {
        try {
            Class.forName("javax.transaction.xa.XAException");
            System.out.println("testLoadPlatformXAException: CLASS FOUND");
        } catch (ClassNotFoundException e) {
            System.out.println("testLoadPlatformXAException: CLASS NOT FOUND");
        }
    }

    @Test
    public void testGetPlatformResourceDoesNotExist() {
        // look for a resource from java/lang that doesn't exist
        assertNull("/java/lang/platform-delegation-test.txt", getClass().getResource("/java/lang/platform-delegation-test.txt"));
    }

    @Test
    public void testGetPlatformResourceDoesExist() {
        // look for a resource from java/lang that should exist
        assertNotNull("/java/lang/String.class", getClass().getResource("/java/lang/String.class"));
    }

    @Test
    public void testGetPlatformResourcesDoesNotExist() throws IOException {
        // look for a resource from java/lang that doesn't exist
        Enumeration<URL> result = getClass().getClassLoader().getResources("java/lang/platform-delegation-test.txt");
        if (result != null && result.hasMoreElements()) {
            fail("Should not find resources: java/lang/platform-delegation-test.txt");
        }
    }

    @Test
    public void testGetPlatformResourcesDoesExist() throws IOException {
        // look for a resource from java/lang that should exist
        Enumeration<URL> result = getClass().getClassLoader().getResources("java/lang/String.class");
        if (result == null || !result.hasMoreElements()) {
            fail("Should find resources: java/lang/String.class");
        }

        int count = Collections.list(result).size();
        System.out.println("testGetPlatformResourcesDoesExist: count=" + count);
    }

    @Test
    public void testPlatformService() {
        doServiceLoaderCheck(FileSystemProvider.class);
    }

    /**
     * @param service
     */
    private void doServiceLoaderCheck(Class<?> serviceClass) {
        System.out.println("Testing platform service: " + serviceClass);
        ServiceLoader<?> serviceLoader = ServiceLoader.load(serviceClass);
        Object s = serviceLoader.iterator().next();
        System.out.println("Got platform service : " + s);
    }
}
