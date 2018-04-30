/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config13.tck;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.MvnUtils;

/**
 * This is a test class that runs a whole Maven TCK as one test FAT test.
 * There is a detailed output on specific
 */
@RunWith(FATRunner.class)
public class Config13TCKLauncher {

    @Server("Config13TCKServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWMCG0007E", "CWMCG0014E", "CWMCG0015E", "CWMCG5003E", "CWWKZ0002E");
    }

    @Mode(TestMode.EXPERIMENTAL)
    @Test
    @AllowedFFDC // The tested deployment exceptions cause FFDC so we have to allow for this.
    public void launchConfig13Tck() throws Exception {
        MvnUtils.runTCKMvnCmd(server, "com.ibm.ws.microprofile.config.1.3_fat_tck", this.getClass() + ":launchConfig13Tck");
    }

    @Mode(TestMode.LITE)
    @Test
    public void testThatDoesNothingAndCanAlwaysRunAndPass() {
        if (TestModeFilter.FRAMEWORK_TEST_MODE != TestMode.EXPERIMENTAL) {
            System.out.println("\n\n\n");
            System.out.println("TCK MASTER BRANCH RUN NOT REQUESTED: fat.test.mode=" + TestModeFilter.FRAMEWORK_TEST_MODE
                               + ", run with '-Dfat.test.mode=experimental' to run the TCK");
            System.out.println("\n\n\n");
            // In FATs System.out is captured to a file, we try to be kind to any developer and give it to them straight
            if (Boolean.valueOf(System.getProperty("fat.test.localrun"))) {
                try (PrintStream screen = new PrintStream(new FileOutputStream(FileDescriptor.out))) {
                    screen.println("\n\n\n");
                    screen.println("TCK MASTER BRANCH RUN NOT REQUESTED: fat.test.mode=" + TestModeFilter.FRAMEWORK_TEST_MODE
                                   + ", run with '-Dfat.test.mode=experimental' to run the TCK");
                    screen.println("\n\n\n");
                }
            }
        }
    }
}
