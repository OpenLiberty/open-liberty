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
public class TestJPA30_JavaSourceLevel extends AbstractTestJavaSourceLevel {
    @Server("javaSupportServer_JPA30")
    public static LibertyServer server_jss_jpa30;

    @BeforeClass
    public static void setup() throws Exception {
        setupFAT(server_jss_jpa30, true);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        shutdownFAT(server_jss_jpa30);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 7)
    public void testJPA30WithJava07() throws Exception {
        runTest(server_jss_jpa30, 7);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 8)
    public void testJPA30WithJava08() throws Exception {
        runTest(server_jss_jpa30, 8);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 9)
    public void testJPA30WithJava09() throws Exception {
        runTest(server_jss_jpa30, 9);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 10)
    public void testJPA30WithJava10() throws Exception {
        runTest(server_jss_jpa30, 10);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 11)
    public void testJPA30WithJava11() throws Exception {
        runTest(server_jss_jpa30, 11);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 12)
    public void testJPA30WithJava12() throws Exception {
        runTest(server_jss_jpa30, 12);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 13)
    public void testJPA30WithJava13() throws Exception {
        runTest(server_jss_jpa30, 13);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 14)
    public void testJPA30WithJava14() throws Exception {
        runTest(server_jss_jpa30, 14);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 15)
    public void testJPA30WithJava15() throws Exception {
        runTest(server_jss_jpa30, 15);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 16)
    public void testJPA30WithJava16() throws Exception {
        runTest(server_jss_jpa30, 16);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 17)
    public void testJPA30WithJava17() throws Exception {
        runTest(server_jss_jpa30, 17);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 18)
    public void testJPA30WithJava18() throws Exception {
        runTest(server_jss_jpa30, 18);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 19)
    public void testJPA30WithJava19() throws Exception {
        runTest(server_jss_jpa30, 19);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 20)
    public void testJPA30WithJava20() throws Exception {
        runTest(server_jss_jpa30, 20);
    }
}
