/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openlibery.classloading.patch.library.test.app;

import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB1;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB2;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB3;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB4;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB5;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB6;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB7;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB8;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_PATCH_LIB_APP;
import static io.openliberty.classloading.classpath.util.TestUtils.assertCommonResourceFromArchive;
import static io.openliberty.classloading.classpath.util.TestUtils.assertCommonResourceFromArchives;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.classloading.libs.util.CodeSourceUtil;

@WebServlet("/PatchLibraryTestServlet")
public class PatchLibraryTestServlet extends FATServlet{

    private static final long serialVersionUID = 1L;
    private static final String PATCH_PACKAGE = "io.openlibery.classloading.patch.library.test.app.";

    @Test
    public void testAPatch() throws ClassNotFoundException {
        assertEquals("Wrong jar used.", "testLib1.jar", CodeSourceUtil.getClassCodeSourceFileName(Class.forName(PATCH_PACKAGE + "a.APatch")));
    }

    @Test
    public void testBPatch() throws ClassNotFoundException {
        assertEquals("Wrong jar used.", "testLib2.jar", CodeSourceUtil.getClassCodeSourceFileName(Class.forName(PATCH_PACKAGE + "b.BPatch")));
    }

    @Test
    public void testCPatch() throws ClassNotFoundException {
        assertEquals("Wrong jar used.", "testLib3.jar", CodeSourceUtil.getClassCodeSourceFileName(Class.forName(PATCH_PACKAGE + "c.CPatch")));
    }

    @Test
    public void testDPatch() throws ClassNotFoundException {
        assertEquals("Wrong jar used.", "testLib4.jar", CodeSourceUtil.getClassCodeSourceFileName(Class.forName(PATCH_PACKAGE + "d.DPatch")));
    }

    @Test
    public void testEPatch() throws ClassNotFoundException {
        assertEquals("Wrong jar used.", "testPatchLib.war", CodeSourceUtil.getClassCodeSourceFileName(Class.forName(PATCH_PACKAGE + "e.EPatch")));
    }

    @Test
    public void testFPatch() throws ClassNotFoundException {
        assertEquals("Wrong jar used.", "testPatchLib.war", CodeSourceUtil.getClassCodeSourceFileName(Class.forName(PATCH_PACKAGE + "f.FPatch")));
    }

    @Test
    public void testGPatch() throws ClassNotFoundException {
        assertEquals("Wrong jar used.", "testPatchLib.war", CodeSourceUtil.getClassCodeSourceFileName(Class.forName(PATCH_PACKAGE + "g.GPatch")));
    }

    @Test
    public void testHPatch() throws ClassNotFoundException {
        assertEquals("Wrong jar used.", "testPatchLib.war", CodeSourceUtil.getClassCodeSourceFileName(Class.forName(PATCH_PACKAGE + "h.HPatch")));
    }

    @Test
    public void testGetResourcesOrder() {
        List<String> expectedOrder = Arrays.asList(TEST_LIB1, //
                                                   TEST_LIB2, //
                                                   TEST_LIB3, //
                                                   TEST_LIB4, //
                                                   // TODO not sure why this WEB-INF/classes and root are reversed from the other WAR in EAR tests
                                                   // It may have something to do with WAR Class-Path headers
                                                   TEST_PATCH_LIB_APP + "_webInf", //
                                                   TEST_PATCH_LIB_APP + "_root", //
                                                   TEST_LIB5, //
                                                   TEST_LIB6, //
                                                   TEST_LIB7, //
                                                   TEST_LIB8);
        assertCommonResourceFromArchives(getClass(), expectedOrder);
    }

    @Test
    public void testGetResource() {
        assertCommonResourceFromArchive(getClass(), TEST_LIB1);
    }
}
