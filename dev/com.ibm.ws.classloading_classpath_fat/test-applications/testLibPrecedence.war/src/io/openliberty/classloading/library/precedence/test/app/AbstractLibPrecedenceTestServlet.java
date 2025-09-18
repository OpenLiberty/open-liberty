/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.classloading.library.precedence.test.app;

import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB1;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB2;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB3;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB4;
import static io.openliberty.classloading.classpath.util.TestUtils.assertCommonResourceFromArchive;
import static io.openliberty.classloading.classpath.util.TestUtils.assertCommonResourceFromArchives;
import static io.openliberty.classloading.classpath.util.TestUtils.assertResourceFromArchive;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.classloading.classpath.test.lib1.Lib1;
import io.openliberty.classloading.classpath.test.lib2.Lib2;
import io.openliberty.classloading.classpath.test.lib3.Lib3;
import io.openliberty.classloading.classpath.test.lib4.Lib4;
import io.openliberty.classloading.libs.util.CodeSourceUtil;
import test.bundle.api1.a.API_A1;
import test.bundle.api1.b.API_B1;
import test.bundle.api1.c.API_C1;
import test.bundle.api1.tester1.APITester1;
import test.bundle.api2.a.API_A2;
import test.bundle.api2.b.API_B2;
import test.bundle.api2.c.API_C2;
import test.bundle.api2.tester2.APITester2;
import test.bundle.api3.a.API_A3;
import test.bundle.api3.b.API_B3;
import test.bundle.api3.c.API_C3;
import test.bundle.api3.tester3.APITester3;
import test.bundle.api4.a.API_A4;
import test.bundle.api4.b.API_B4;
import test.bundle.api4.c.API_C4;
import test.bundle.api4.tester4.APITester4;

public abstract class AbstractLibPrecedenceTestServlet extends FATServlet{

    private static final long serialVersionUID = 1L;

    protected enum ExpectedCodeSource {
        testLib1Jar("testLib1.jar", true),
        testLib2Jar("testLib2.jar", true),
        testBundleAPIJar("test.bundle.api.jar", false);

        public final String codeSource;
        public final boolean expectVerifyError;
        ExpectedCodeSource(String codeSource, boolean expectVerifyError) {
            this.codeSource = codeSource;
            this.expectVerifyError = expectVerifyError;
        }
    }

    private final ExpectedCodeSource api1;
    private final ExpectedCodeSource api2;
    private final ExpectedCodeSource api3;
    private final ExpectedCodeSource api4;

    /**
     *
     */
    public AbstractLibPrecedenceTestServlet(ExpectedCodeSource api1, ExpectedCodeSource api2, ExpectedCodeSource api3, ExpectedCodeSource api4) {
        this.api1 = api1;
        this.api2 = api2;
        this.api3 = api3;
        this.api4 = api4;
    }

    @Test
    public void testLibDelegation1() throws ClassNotFoundException {
        assertEquals("Wrong jar used API_C1.", api1.codeSource, CodeSourceUtil.getClassCodeSourceFileName(new API_C1().getClass()));
        assertEquals("Wrong jar used API_B1.", api1.codeSource, CodeSourceUtil.getClassCodeSourceFileName(new API_B1().getClass()));
        assertEquals("Wrong jar used API_A1.", api1.codeSource, CodeSourceUtil.getClassCodeSourceFileName(new API_A1().getClass()));

        tryUsingFeatureAPITester1();
    }

    /**
     *
     */
    private void tryUsingFeatureAPITester1() {
        try {
            API_C1 apiC1 = APITester1.loadC();
            apiC1.testC();
            if (api1.expectVerifyError) {
                fail("Expected verify error here");
            }
        } catch (LinkageError e) {
            if (!api1.expectVerifyError) {
                throw e;
            }
        }
    }

    @Test
    public void testLibDelegation2() throws ClassNotFoundException {
        assertEquals("Wrong jar used API_C2.", api2.codeSource, CodeSourceUtil.getClassCodeSourceFileName(new API_C2().getClass()));
        assertEquals("Wrong jar used API_B2.", api2.codeSource, CodeSourceUtil.getClassCodeSourceFileName(new API_B2().getClass()));
        assertEquals("Wrong jar used API_A2.", api2.codeSource, CodeSourceUtil.getClassCodeSourceFileName(new API_A2().getClass()));

        tryUsingFeatureAPITester2();
    }

    /**
     *
     */
    private void tryUsingFeatureAPITester2() {
        try {
            API_C2 apiC2 = APITester2.loadC();
            apiC2.testC();
            if (api2.expectVerifyError) {
                fail("Expected verify error here");
            }
        } catch (LinkageError e) {
            if (!api2.expectVerifyError) {
                throw e;
            }
        }
    }

    @Test
    public void testLibDelegation3() throws ClassNotFoundException {

        assertEquals("Wrong jar used API_C3.", api3.codeSource, CodeSourceUtil.getClassCodeSourceFileName(new API_C3().getClass()));
        assertEquals("Wrong jar used API_B3.", api3.codeSource, CodeSourceUtil.getClassCodeSourceFileName(new API_B3().getClass()));
        assertEquals("Wrong jar used API_A3.", api3.codeSource, CodeSourceUtil.getClassCodeSourceFileName(new API_A3().getClass()));

        tryUsingFeatureAPITester3();
    }

    /**
     *
     */
    private void tryUsingFeatureAPITester3() {
        try {
            API_C3 apiC3 = APITester3.loadC();
            apiC3.testC();
            if (api3.expectVerifyError) {
                fail("Expected verify error here");
            }
        } catch (LinkageError e) {
            if (!api3.expectVerifyError) {
                throw e;
            }
        }
    }

    @Test
    public void testLibDelegation4() throws ClassNotFoundException {

        assertEquals("Wrong jar used API_C4.", api4.codeSource, CodeSourceUtil.getClassCodeSourceFileName(new API_C4().getClass()));
        assertEquals("Wrong jar used API_B4.", api4.codeSource, CodeSourceUtil.getClassCodeSourceFileName(new API_B4().getClass()));
        assertEquals("Wrong jar used API_A4.", api4.codeSource, CodeSourceUtil.getClassCodeSourceFileName(new API_A4().getClass()));

        tryUsingFeatureAPITester4();
    }

    /**
     *
     */
    private void tryUsingFeatureAPITester4() {
        try {
            API_C4 apiC4 = APITester4.loadC();
            apiC4.testC();
            if (api4.expectVerifyError) {
                fail("Expected verify error here");
            }
        } catch (LinkageError e) {
            if (!api4.expectVerifyError) {
                throw e;
            }
        }
    }

    @Test
    public void testLibAccess() {
        // Make sure that all 4 libraries can be accessed with their unique class names
        new Lib1().toString();
        new Lib2().toString();
        new Lib3().toString();
        new Lib4().toString();
    }

    @Test
    public void testGetCommonResourcesOrder() {
        List<String> expectedOrder = Arrays.asList(TEST_LIB1, //
                                                   TEST_LIB2, //
                                                   TEST_LIB3, //
                                                   TEST_LIB4);
        assertCommonResourceFromArchives(getClass(), expectedOrder);
    }

    @Test
    public void testGetCommonResource() {
        assertCommonResourceFromArchive(getClass(), TEST_LIB1);
    }

    @Test
    public void testGetLibResource() {
        assertResourceFromArchive("lib1.properties", getClass(), TEST_LIB1);
        assertResourceFromArchive("lib2.properties", getClass(), TEST_LIB2);
        assertResourceFromArchive("lib3.properties", getClass(), TEST_LIB3);
        assertResourceFromArchive("lib4.properties", getClass(), TEST_LIB4);
    }
}
