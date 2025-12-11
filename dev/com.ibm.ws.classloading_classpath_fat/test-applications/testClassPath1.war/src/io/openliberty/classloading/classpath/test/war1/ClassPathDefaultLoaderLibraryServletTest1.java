/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.classloading.classpath.test.war1;

import static io.openliberty.classloading.classpath.fat.FATSuite.LIB7_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.LIB8_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_CLASS_PATH1_APP;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_EJB1;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_EJB2;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB1;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB10;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB12;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB14;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB16;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB17;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB18;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB2;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB3;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB4;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB6;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB7;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB8;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_RAR1;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_RESOURCE_ADAPTOR;
import static io.openliberty.classloading.classpath.util.TestUtils.assertCommonResourceFromArchive;
import static io.openliberty.classloading.classpath.util.TestUtils.assertCommonResourceFromArchives;
import static io.openliberty.classloading.classpath.util.TestUtils.assertLoadClass;
import static io.openliberty.classloading.classpath.util.TestUtils.assertLoadClassNotLoadedWithLoaders;
import static io.openliberty.classloading.classpath.util.TestUtils.TEST_LOAD_RESULT.success_fromEARLoader;

import java.util.Arrays;
import java.util.List;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.classloading.classpath.util.TestUtils.TEST_CLASS_LOAD;
import io.openliberty.classloading.classpath.util.TestUtils.TEST_LOAD_RESULT;

@WebServlet("/PrivateLibraryServletTest1")
public class ClassPathDefaultLoaderLibraryServletTest1 extends FATServlet{
    private static final long serialVersionUID = 1L;

    @Test
    public void testGetResource() {
        assertCommonResourceFromArchive(getClass(), TEST_EJB1);
    }

    @Test
    public void testGetResources() {
        List<String> expectedOrder = Arrays.asList(TEST_EJB1, //
                                                   TEST_LIB14, //
                                                   TEST_EJB2, //
                                                   TEST_LIB18, //
                                                   TEST_LIB4, //
                                                   TEST_LIB6, //
                                                   TEST_RAR1, //
                                                   TEST_LIB10, //
                                                   TEST_RESOURCE_ADAPTOR, //
                                                   TEST_LIB12, //
                                                   TEST_LIB16, //
                                                   TEST_LIB17, //
                                                   TEST_LIB7, //
                                                   TEST_LIB8, //
                                                   TEST_CLASS_PATH1_APP + "_root", //
                                                   TEST_LIB1, //
                                                   TEST_LIB2, //
                                                   TEST_LIB3, //
                                                   TEST_CLASS_PATH1_APP + "_webInf");
        assertCommonResourceFromArchives(getClass(), expectedOrder);
    }

    private void runTest(TEST_LOAD_RESULT expected) {
        TEST_CLASS_LOAD.valueOf(getTestMethod()).testLoadClass(expected, getClass());
    }

    @Test
    public void testLoadLibrary7Class() {
        runTest(success_fromEARLoader);
    }

    @Test
    public void testLoadLibrary8Class() throws ClassNotFoundException {
        Class<?> found = assertLoadClassNotLoadedWithLoaders(getClass(), LIB8_CLASS_NAME, getClass().getClassLoader(), getClass().getClassLoader().getParent());
        Class<?> lib7Class = Class.forName(LIB7_CLASS_NAME);
        assertLoadClass(lib7Class, LIB8_CLASS_NAME, found.getClassLoader());
    }

    @Test
    public void testGetResourcesFromEarClassLoader() throws ClassNotFoundException {
        List<String> expectedOrder = Arrays.asList(TEST_EJB1, //
                                                   TEST_LIB14, //
                                                   TEST_EJB2, //
                                                   TEST_LIB18, //
                                                   TEST_LIB4, //
                                                   TEST_LIB6, //
                                                   TEST_RAR1, //
                                                   TEST_LIB10, //
                                                   TEST_RESOURCE_ADAPTOR, //
                                                   TEST_LIB12, //
                                                   TEST_LIB16, //
                                                   TEST_LIB17, //
                                                   TEST_LIB7, //
                                                   TEST_LIB8);
        Class<?> lib7Class = Class.forName(LIB7_CLASS_NAME);
        assertCommonResourceFromArchives(lib7Class, expectedOrder);
    }
}
