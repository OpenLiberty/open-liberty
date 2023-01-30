/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

package io.openliberty.jpa.test.javasupport;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
@MinimumJavaLevel(javaLevel = 8)
public class TestJPA22_JavaSourceLevel extends AbstractTestJavaSourceLevel {
    @Server("javaSupportServer_JPA22")
    public static LibertyServer server_jss_jpa22;

    @BeforeClass
    public static void setup() throws Exception {
        setupFAT(server_jss_jpa22, false);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        shutdownFAT(server_jss_jpa22);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 7)
    public void testJPA22WithJava07() throws Exception {
        runTest(server_jss_jpa22, 7);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 8)
    public void testJPA22WithJava08() throws Exception {
        runTest(server_jss_jpa22, 8);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 9)
    public void testJPA22WithJava09() throws Exception {
        runTest(server_jss_jpa22, 9);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 10)
    public void testJPA22WithJava10() throws Exception {
        runTest(server_jss_jpa22, 10);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 11)
    public void testJPA22WithJava11() throws Exception {
        runTest(server_jss_jpa22, 11);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 12)
    public void testJPA22WithJava12() throws Exception {
        runTest(server_jss_jpa22, 12);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 13)
    public void testJPA22WithJava13() throws Exception {
        runTest(server_jss_jpa22, 13);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 14)
    public void testJPA22WithJava14() throws Exception {
        runTest(server_jss_jpa22, 14);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 15)
    public void testJPA22WithJava15() throws Exception {
        runTest(server_jss_jpa22, 15);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 16)
    public void testJPA22WithJava16() throws Exception {
        runTest(server_jss_jpa22, 16);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 17)
    public void testJPA22WithJava17() throws Exception {
        runTest(server_jss_jpa22, 17);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 18)
    public void testJPA22WithJava18() throws Exception {
        runTest(server_jss_jpa22, 18);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 19)
    public void testJPA22WithJava19() throws Exception {
        runTest(server_jss_jpa22, 19);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 20)
    public void testJPA22WithJava20() throws Exception {
        runTest(server_jss_jpa22, 20);
    }
}
