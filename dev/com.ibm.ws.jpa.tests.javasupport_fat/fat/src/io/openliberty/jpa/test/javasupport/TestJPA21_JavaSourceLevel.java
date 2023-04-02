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
public class TestJPA21_JavaSourceLevel extends AbstractTestJavaSourceLevel {
    @Server("javaSupportServer_JPA21")
    public static LibertyServer server_jss_jpa21;

    @BeforeClass
    public static void setup() throws Exception {
        setupFAT(server_jss_jpa21, false);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        shutdownFAT(server_jss_jpa21);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 7)
    public void testJPA21WithJava07() throws Exception {
        runTest(server_jss_jpa21, 7);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 8)
    public void testJPA21WithJava08() throws Exception {
        runTest(server_jss_jpa21, 8);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 9)
    public void testJPA21WithJava09() throws Exception {
        runTest(server_jss_jpa21, 9);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 10)
    public void testJPA21WithJava10() throws Exception {
        runTest(server_jss_jpa21, 10);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 11)
    public void testJPA21WithJava11() throws Exception {
        runTest(server_jss_jpa21, 11);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 12)
    public void testJPA21WithJava12() throws Exception {
        runTest(server_jss_jpa21, 12);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 13)
    public void testJPA21WithJava13() throws Exception {
        runTest(server_jss_jpa21, 13);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 14)
    public void testJPA21WithJava14() throws Exception {
        runTest(server_jss_jpa21, 14);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 15)
    public void testJPA21WithJava15() throws Exception {
        runTest(server_jss_jpa21, 15);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 16)
    public void testJPA21WithJava16() throws Exception {
        runTest(server_jss_jpa21, 16);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 17)
    public void testJPA21WithJava17() throws Exception {
        runTest(server_jss_jpa21, 17);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 18)
    public void testJPA21WithJava18() throws Exception {
        runTest(server_jss_jpa21, 18);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 19)
    public void testJPA21WithJava19() throws Exception {
        runTest(server_jss_jpa21, 19);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 20)
    public void testJPA21WithJava20() throws Exception {
        runTest(server_jss_jpa21, 20);
    }
}
