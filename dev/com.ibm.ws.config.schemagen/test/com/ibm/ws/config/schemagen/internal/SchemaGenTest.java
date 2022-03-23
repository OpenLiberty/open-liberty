package com.ibm.ws.config.schemagen.internal;
/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.junit.Test;

import test.common.SharedOutputManager;

/**
 *
 */
public class SchemaGenTest {

    public static String WLP_BIN_DIR = "../build.image/wlp/bin";
    public static String OUTPUT_FILE = "schemaGenOutput.xsd";
    public static String HELP_OPTION = "-help";
    public static String SCHEMAGEN_BAT = "./schemaGen.bat";
    public static String SCHEMAGEN_LINUX_SCRIPT = "./schemaGen";

    @Test
    public void testSchemaGenNoParms() {
        System.out.println("==================== testSchemaGenNoParms ...");

        try {
            ProcessBuilder pb;
            if (isWindows()) {
                pb = new ProcessBuilder("cmd", "/c", SCHEMAGEN_BAT);
            } else {
                pb = new ProcessBuilder(SCHEMAGEN_LINUX_SCRIPT);
            }

            File dir = new File(WLP_BIN_DIR);
            pb.directory(dir);
            Process p = pb.start();

            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            
            boolean usageAppears = false;     
            boolean encodingAppears = false;
                    
            if (p.isAlive()) {
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                    if (line.indexOf("Usage") != -1) {
                        usageAppears = true;
                    }
                    if (line.indexOf("--encoding") != -1) {
                        encodingAppears = true;
                    }
                }
            }
            
            assertTrue("'Usage' should appear in command output", usageAppears);
            assertFalse("'--encoding' should NOT appear in command output when no arguments passed.", encodingAppears);
            System.out.println("PASSED");
            
        } catch (IOException ioe) {
            System.out.println("Caught exception [" + ioe.getMessage() + "]");
            ioe.printStackTrace();
        }
    }
    
    @Test
    public void testSchemaGenHelp() {
        System.out.println("==================== testSchemaGenHelp ...");

        try {
            ProcessBuilder pb;
            if (isWindows()) {
                pb = new ProcessBuilder("cmd", "/c", SCHEMAGEN_BAT, HELP_OPTION);
            } else {
                pb = new ProcessBuilder(SCHEMAGEN_LINUX_SCRIPT, HELP_OPTION);
            }

            File dir = new File(WLP_BIN_DIR);
            pb.directory(dir);
            Process p = pb.start();

            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            
            boolean usageAppears = false;     
            boolean encodingAppears = false;

            if (p.isAlive()) {
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                    if (line.indexOf("Usage") != -1) {
                        usageAppears = true;
                    }
                    if (line.indexOf("--encoding") != -1) {
                        encodingAppears = true;
                    }
                }
            }
            
            assertTrue("'Usage' should appear in command output", usageAppears);
            assertTrue("'--encoding' should appear in command output when " + HELP_OPTION + " is passed.", encodingAppears);
            System.out.println("PASSED");
                    
        } catch (IOException ioe) {
            System.out.println("Caught exception [" + ioe.getMessage() + "]");
            ioe.printStackTrace();
        }
    }
    
    @Test
    public void testSchemaGenOutput() {
        System.out.println("==================== testSchemaGenOutput ...");

        try {
            ProcessBuilder pb;
            if (isWindows()) {
                pb = new ProcessBuilder("cmd", "/c", SCHEMAGEN_BAT,  OUTPUT_FILE);
            } else {
                pb = new ProcessBuilder(SCHEMAGEN_LINUX_SCRIPT,  OUTPUT_FILE);
            }

            File dir = new File(WLP_BIN_DIR);
            pb.directory(dir);
            Process p = pb.start();

            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            
            // The command should not generate any output to stdio or stderr
            if (p.isAlive()) {
                assertNull("Stream should be null", br.readLine());
            }
            
            File outputFile = new File(WLP_BIN_DIR + "/" + OUTPUT_FILE);        
            assertTrue("File [" + outputFile.getName() + "] should exist.", outputFile.exists());
            outputFile.delete();
            System.out.println("PASSED");
            
            //        // Display WLP_BIN_DIR directory listing
            //        File f = new File(WLP_BIN_DIR);
            //        String[] files = f.list();
            //        Arrays.sort(files);
            //        for (String s : files) {
            //            System.out.println("   " + s);
            //        }

        } catch (IOException ioe) {
            System.out.println("Caught exception [" + ioe.getMessage() + "]");
            ioe.printStackTrace();
        }
    }

    public boolean isWindows() {
        String os = System.getProperty("os.name");
        if (os.startsWith("Win")) {
            return true;
        }
        return false;
    }

}
