/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.classloading.classpath.test.war2;

import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_CLASS_PATH2_APP;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_EJB1;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_EJB2;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_EJB3;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB1;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB10;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB12;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB13;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB14;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB16;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB17;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB2;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB3;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB4;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB6;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_RAR1;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_RESOURCE_ADAPTOR;
import static io.openliberty.classloading.classpath.util.TestUtils.assertCommonResourceFromArchive;
import static io.openliberty.classloading.classpath.util.TestUtils.assertCommonResourceFromArchives;
import static io.openliberty.classloading.classpath.util.TestUtils.TEST_LOAD_RESULT.failure;
import static io.openliberty.classloading.classpath.util.TestUtils.TEST_LOAD_RESULT.success_fromEARLoader;
import static io.openliberty.classloading.classpath.util.TestUtils.TEST_LOAD_RESULT.success_fromWARLoader;

import java.util.Arrays;
import java.util.List;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.classloading.classpath.util.TestUtils.TEST_CLASS_LOAD;
import io.openliberty.classloading.classpath.util.TestUtils.TEST_LOAD_RESULT;

@WebServlet("/ClassPathEarLoaderServletTest2")
public class ClassPathEarLoaderServletTest2 extends FATServlet{
    private static final long serialVersionUID = 1L;

    @Test
    public void testGetResource() {
        assertCommonResourceFromArchive(getClass(), TEST_EJB1);
    }

    @Test
    public void testGetResources() {
        List<String> expectedOrder = Arrays.asList(TEST_EJB1, //
                                                   TEST_EJB2, //
                                                   TEST_LIB4, //
                                                   TEST_RAR1, //
                                                   TEST_RESOURCE_ADAPTOR, //
                                                   TEST_LIB17, //
                                                   TEST_LIB6, //
                                                   TEST_LIB1, //
                                                   TEST_LIB2, //
                                                   TEST_LIB3, //
                                                   TEST_LIB13, //
                                                   TEST_LIB14, //
                                                   TEST_LIB10, //
                                                   TEST_LIB12, //
                                                   TEST_LIB16, //
                                                   TEST_CLASS_PATH2_APP + "_root", //
                                                   TEST_CLASS_PATH2_APP + "_webInf", //
                                                   TEST_EJB3);
        assertCommonResourceFromArchives(getClass(), expectedOrder);
    }

    private void runTest(TEST_LOAD_RESULT expected) {
        TEST_CLASS_LOAD.valueOf(getTestMethod()).testLoadClass(expected, getClass());
    }

    @Test
    public void testLoadLibrary1Class() {
        runTest(success_fromEARLoader);
    }

    @Test
    public void testLoadLibrary2Class() {
        runTest(success_fromEARLoader);
    }

    @Test
    public void testLoadLibrary3Class() {
        runTest(success_fromEARLoader);
    }

    @Test
    public void testLoadLibrary4Class() {
        runTest(success_fromEARLoader);
    }

    @Test
    public void testLoadLibrary5Class() {
        runTest(failure);
    }

    @Test
    public void testLoadLibrary6Class() {
        runTest(success_fromEARLoader);
    }

    @Test
    public void testLoadLibrary7Class() {
        runTest(failure);
    }

    @Test
    public void testLoadLibrary8Class() {
        runTest(failure);
    }

    @Test
    public void testLoadLibrary9Class() {
        runTest(failure);
    }

    @Test
    public void testLoadLibrary10Class() {
        runTest(success_fromEARLoader);
    }

    @Test
    public void testLoadLibrary11Class() {
        runTest(failure);
    }

    @Test
    public void testLoadLibrary12Class() {
        runTest(success_fromEARLoader);
    }

    @Test
    public void testLoadLibrary13Class() {
        runTest(success_fromEARLoader);
    }

    @Test
    public void testLoadLibrary14Class() {
        runTest(success_fromEARLoader);
    }

    @Test
    public void testLoadLibrary15Class() {
        runTest(failure);
    }

    @Test
    public void testLoadLibrary16Class() {
        runTest(success_fromEARLoader);
    }

    @Test
    public void testLoadLibrary17Class() {
        runTest(success_fromEARLoader);
    }

    @Test
    public void testLoadEJB1Class() {
        runTest(success_fromEARLoader);
    }

    @Test
    public void testLoadEJB2Class() {
        runTest(success_fromEARLoader);
    }

    @Test
    public void testLoadEJB3Class() {
        runTest(success_fromWARLoader);
    }

    @Test
    public void testLoadRARLib1Class() {
        runTest(success_fromEARLoader);
    }

    @Test
    public void testLoadRARLib2Class() {
        runTest(success_fromEARLoader);
    }
}
