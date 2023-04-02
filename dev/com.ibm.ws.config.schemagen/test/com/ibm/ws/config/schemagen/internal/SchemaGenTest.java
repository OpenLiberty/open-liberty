package com.ibm.ws.config.schemagen.internal;
/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.junit.Test;

/**
 *  Tests the schemaGen command that exists in the wlp/bin directory.
 */
public class SchemaGenTest {

    public static final String WLP_BIN_DIR = "../build.image/wlp/bin";
    public static final String OUTPUT_FILE = "schemaGenOutput.xsd";
    public static final String HELP_OPTION = "-help";
    public static final String SCHEMAGEN_BAT = "./schemaGen.bat";
    public static final String SCHEMAGEN_LINUX_SCRIPT = "./schemaGen";
    public static final long TIMEOUT = 30_000_000_000L;  // 30-second timeout
    public static final int MAX_OUTPUT_LINES = 500;      // Reasonable limit to output to standard output by schemaGen
    public static final boolean IS_WINDOWS = isWindows();

    public static boolean isWindows() {
        String os = System.getProperty("os.name");
        if (os.startsWith("Win")) {
            return true;
        }
        return false;
    }

    /**
     * Test that when no parameters are passed, only basic usage info is displayed
     * @throws IOException
     */
    @Test
    public void testSchemaGenNoParms() throws IOException {
        System.out.println("==== testSchemaGenNoParms ====");

        ProcessBuilder pb;
        Process p = null;
        
        try {
            
            if (IS_WINDOWS) {
                pb = new ProcessBuilder("cmd", "/c", SCHEMAGEN_BAT);
            } else {
                pb = new ProcessBuilder(SCHEMAGEN_LINUX_SCRIPT);
            }

            File dir = new File(WLP_BIN_DIR);
            pb.directory(dir);
            p = pb.start();

            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            
            boolean usageAppears = false;     
            boolean encodingAppears = false;
                    
            long startTime = System.nanoTime();
            int lineCounter = 0;

            while ((line = br.readLine()) != null) {
                System.out.println(line);
                
                if (line.indexOf("Usage") != -1) {
                    usageAppears = true;
                }
                
                if (line.indexOf("--encoding") != -1) {
                    encodingAppears = true;
                }

                // Exit loop if we are getting hung
                if (lineCounter++ > MAX_OUTPUT_LINES) {
                    System.out.println("schemaGen usage info exceeded [ " + MAX_OUTPUT_LINES + " ] lines");
                    break;
                } else if (System.nanoTime() - startTime >  TIMEOUT) {
                    System.out.println("SchemaGen exceeded [ " + TIMEOUT + " ] ns  when displaying usage Info");
                    break;
                }
            }
            
            assertTrue("'Usage' should appear in command output", usageAppears);
            assertFalse("'--encoding' should NOT appear in command output when no arguments passed.", encodingAppears);
            System.out.println("PASSED");
            
        } finally {
            if ( p!= null) {
                p.destroy();
            }
        }
    }
    
    /**
     * Test that when -help parameter is passed that help and usage information is displayed.
     * @throws IOException
     */
    @Test
    public void testSchemaGenHelp() throws IOException {
        System.out.println("==== testSchemaGenHelp ====");

        ProcessBuilder pb;
        Process p = null;
        try {

            if (IS_WINDOWS) {
                pb = new ProcessBuilder("cmd", "/c", SCHEMAGEN_BAT, HELP_OPTION);
            } else {
                pb = new ProcessBuilder(SCHEMAGEN_LINUX_SCRIPT, HELP_OPTION);
            }

            File dir = new File(WLP_BIN_DIR);
            pb.directory(dir);
            p = pb.start();

            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            
            boolean usageAppears = false;     
            boolean encodingAppears = false;

            long startTime = System.nanoTime();
            int lineCounter = 0;
            
            while ((line = br.readLine()) != null) {
                System.out.println(line);
                
                if (line.indexOf("Usage") != -1) {
                    usageAppears = true;
                }
                
                if (line.indexOf("--encoding") != -1) {
                    encodingAppears = true;
                }

                // Exit loop if we are getting hung
                if (lineCounter++ > MAX_OUTPUT_LINES) {
                    System.out.println("schemaGen help exceeded [ " + MAX_OUTPUT_LINES + " ] lines");
                    break;
                } else if (System.nanoTime() - startTime >  TIMEOUT) {
                    System.out.println("schemaGen exceeded [ " + TIMEOUT + " ] ns when displaying help");
                    break;
                }
            }
            
            assertTrue("'Usage' should appear in command output", usageAppears);
            assertTrue("'--encoding' should appear in command output when " + HELP_OPTION + " is passed.", encodingAppears);
            System.out.println("PASSED");
                    
        } finally {
            if ( p!= null) {
                p.destroy();
            }
        }
    }
    
    /**
     * Test that when an output file is specified as parameter that the output file is created,
     * and that CWWKG0109I "success" message is created.
     * 
     * @throws IOException
     */
    @Test
    public void testSchemaGenOutput() throws IOException {
        System.out.println("==== testSchemaGenOutput ====");

        ProcessBuilder pb;
        Process p = null;
        try {
            if (IS_WINDOWS) {
                pb = new ProcessBuilder("cmd", "/c", SCHEMAGEN_BAT,  OUTPUT_FILE);
            } else {
                pb = new ProcessBuilder(SCHEMAGEN_LINUX_SCRIPT,  OUTPUT_FILE);
            }

            File dir = new File(WLP_BIN_DIR);
            pb.directory(dir);
            p = pb.start();

            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            
            boolean successMsgAppears = false;     

            long startTime = System.nanoTime();
            int lineCounter = 0;
            
            while ((line = br.readLine()) != null) {
                System.out.println(line);
                
                if (line.indexOf("CWWKG0109I") != -1) {
                    successMsgAppears = true;
                }

                // Exit loop if we are getting hung
                if (lineCounter++ > MAX_OUTPUT_LINES) {
                    System.out.println("schemaGen help exceeded [ " + MAX_OUTPUT_LINES + " ] lines");
                    break;
                } else if (System.nanoTime() - startTime >  TIMEOUT) {
                    System.out.println("schemaGen exceeded [ " + TIMEOUT + " ] ns when displaying help");
                    break;
                }
            }
            
            assertTrue("'CWWKG0109I' should appear in command output", successMsgAppears);
            assertTrue("Should only output 1 message in successful case.", lineCounter==1);
            
            // It should, however, generate the output file.
            File outputFile = new File(WLP_BIN_DIR + "/" + OUTPUT_FILE);        
            assertTrue("File [" + outputFile.getName() + "] should exist.", outputFile.exists());
            outputFile.delete();  // clean up
            System.out.println("PASSED");

        } finally {
            if ( p!= null) {
                p.destroy();
            }
        }
    }
}
