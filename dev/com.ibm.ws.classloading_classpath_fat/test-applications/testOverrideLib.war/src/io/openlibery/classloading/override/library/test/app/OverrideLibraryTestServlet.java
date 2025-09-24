/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openlibery.classloading.override.library.test.app;

import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB1;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB2;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB3;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB4;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB5;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB6;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB7;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB8;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB9;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_OVERRIDE_LIB_APP;
import static io.openliberty.classloading.classpath.util.TestUtils.assertCommonResourceFromArchive;
import static io.openliberty.classloading.classpath.util.TestUtils.assertCommonResourceFromArchives;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.classloading.libs.util.CodeSourceUtil;

@WebServlet("/OverrideLibraryTestServlet")
public class OverrideLibraryTestServlet extends FATServlet{

    private static final long serialVersionUID = 1L;
    private static final String OVERRIDE_PACKAGE = "io.openlibery.classloading.override.library.test.app.";

    @Test
    public void testAOverride() throws ClassNotFoundException {
        assertEquals("Wrong jar used.", "testLib1.jar", CodeSourceUtil.getClassCodeSourceFileName(Class.forName(OVERRIDE_PACKAGE + "a.AOverride")));
    }

    @Test
    public void testBOverride() throws ClassNotFoundException {
        assertEquals("Wrong jar used.", "testLib2.jar", CodeSourceUtil.getClassCodeSourceFileName(Class.forName(OVERRIDE_PACKAGE + "b.BOverride")));
    }

    @Test
    public void testCOverride() throws ClassNotFoundException {
        assertEquals("Wrong jar used.", "testLib3.jar", CodeSourceUtil.getClassCodeSourceFileName(Class.forName(OVERRIDE_PACKAGE + "c.COverride")));
    }

    @Test
    public void testDOverride() throws ClassNotFoundException {
        assertEquals("Wrong jar used.", "testLib4.jar", CodeSourceUtil.getClassCodeSourceFileName(Class.forName(OVERRIDE_PACKAGE + "d.DOverride")));
    }

    @Test
    public void testEOverride() throws ClassNotFoundException {
        assertEquals("Wrong jar used.", "testOverrideLib.war", CodeSourceUtil.getClassCodeSourceFileName(Class.forName(OVERRIDE_PACKAGE + "e.EOverride")));
    }

    @Test
    public void testFOverride() throws ClassNotFoundException {
        assertEquals("Wrong jar used.", "testOverrideLib.war", CodeSourceUtil.getClassCodeSourceFileName(Class.forName(OVERRIDE_PACKAGE + "f.FOverride")));
    }

    @Test
    public void testGOverride() throws ClassNotFoundException {
        assertEquals("Wrong jar used.", "testOverrideLib.war", CodeSourceUtil.getClassCodeSourceFileName(Class.forName(OVERRIDE_PACKAGE + "g.GOverride")));
    }

    @Test
    public void testHOverride() throws ClassNotFoundException {
        assertEquals("Wrong jar used.", "testOverrideLib.war", CodeSourceUtil.getClassCodeSourceFileName(Class.forName(OVERRIDE_PACKAGE + "h.HOverride")));
    }

    @Test
    public void testRAOverride() throws ClassNotFoundException {
        assertEquals("Wrong jar used.", "testLib9.jar", CodeSourceUtil.getClassCodeSourceFileName(Class.forName("dummy.ra.DummyME")));
    }

    @Test
    public void testGetResourcesOrder() {
        List<String> expectedOrder = Arrays.asList(TEST_LIB1, //
                                                   TEST_LIB2, //
                                                   TEST_LIB3, //
                                                   TEST_LIB4, //
                                                   // TODO not sure why this WEB-INF/classes and root are reversed from the other WAR in EAR tests
                                                   // It may have something to do with WAR Class-Path headers
                                                   TEST_OVERRIDE_LIB_APP + "_webInf", //
                                                   TEST_OVERRIDE_LIB_APP + "_root", //
                                                   TEST_LIB5, //
                                                   TEST_LIB6, //
                                                   TEST_LIB7, //
                                                   TEST_LIB8,
                                                   TEST_LIB9);
        assertCommonResourceFromArchives(getClass(), expectedOrder);
    }

    @Test
    public void testGetResource() {
        assertCommonResourceFromArchive(getClass(), TEST_LIB1);
    }
}
