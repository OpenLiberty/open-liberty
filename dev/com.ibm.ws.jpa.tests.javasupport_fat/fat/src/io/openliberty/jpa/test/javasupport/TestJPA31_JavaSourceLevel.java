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
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
@MinimumJavaLevel(javaLevel = 11)
public class TestJPA31_JavaSourceLevel extends AbstractTestJavaSourceLevel {
    @Server("javaSupportServer_JPA31")
    public static LibertyServer server_jss_jpa31;

    @BeforeClass
    public static void setup() throws Exception {
        if (JavaInfo.JAVA_VERSION >= 11) {
            setupFAT(server_jss_jpa31, true);
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (JavaInfo.JAVA_VERSION >= 11) {
            shutdownFAT(server_jss_jpa31);
        }
    }

    // Jakarta EE 10 requires a minimum JDK runtime of 11, however application code using an earlier JDK should be okay.

    @Test
    @MinimumJavaLevel(javaLevel = 11)
    public void testJPA31WithJava07() throws Exception {
        runTest(server_jss_jpa31, 7);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 11)
    public void testJPA31WithJava08() throws Exception {
        runTest(server_jss_jpa31, 8);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 11)
    public void testJPA31WithJava09() throws Exception {
        runTest(server_jss_jpa31, 9);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 11)
    public void testJPA31WithJava10() throws Exception {
        runTest(server_jss_jpa31, 10);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 11)
    public void testJPA31WithJava11() throws Exception {
        runTest(server_jss_jpa31, 11);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 12)
    public void testJPA31WithJava12() throws Exception {
        runTest(server_jss_jpa31, 12);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 13)
    public void testJPA31WithJava13() throws Exception {
        runTest(server_jss_jpa31, 13);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 14)
    public void testJPA31WithJava14() throws Exception {
        runTest(server_jss_jpa31, 14);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 15)
    public void testJPA31WithJava15() throws Exception {
        runTest(server_jss_jpa31, 15);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 16)
    public void testJPA31WithJava16() throws Exception {
        runTest(server_jss_jpa31, 16);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 17)
    public void testJPA31WithJava17() throws Exception {
        runTest(server_jss_jpa31, 17);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 18)
    public void testJPA31WithJava18() throws Exception {
        runTest(server_jss_jpa31, 18);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 19)
    public void testJPA31WithJava19() throws Exception {
        runTest(server_jss_jpa31, 19);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 20)
    public void testJPA31WithJava20() throws Exception {
        runTest(server_jss_jpa31, 20);
    }
}
