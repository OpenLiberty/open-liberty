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
package com.ibm.ws.webserver.plugin.utility.fat;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.websphere.simplicity.ProgramOutput;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class PluginUtilityEndpointMergeTest {

    private static final Class<?> c = PluginUtilityMergeTest.class;
    private static final LibertyServer defaultServer = FATSuite.defaultServer;
    
    private static final String MERGED_PLUGIN_CFG_FILENAME = "merged-plugin-cfg.xml";
    public static final String pathToAutoFVTTestFiles = "lib/LibertyFATTestFiles/endpointtests/";
    
    static Machine machine;
    String defaultServerInstallRoot = defaultServer.getInstallRoot();
    static Path workingDirectory = Paths.get("endpointtest");
    static Path mergedPluginCfgFile = Paths.get(workingDirectory.toAbsolutePath().toString(),MERGED_PLUGIN_CFG_FILENAME);
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        machine = defaultServer.getMachine();
        Files.createDirectory(workingDirectory);
    }

    @AfterClass
    public static void cleanUpClass() throws Exception {
        // In case there was an accident
        Files.deleteIfExists(mergedPluginCfgFile);     

        // Clean up working directory
        workingDirectory.toFile().delete();  
    }

    @Before
    public void setUp() throws Exception {
        // In case there was an accident
        Files.deleteIfExists(mergedPluginCfgFile);     
    } 

    @After
    public void cleanUp() throws Exception {
        Files.deleteIfExists(mergedPluginCfgFile);     
    } 

    @Test
    public void testEndpointTest1() throws Exception {
        performDirTest("test1");
    }
    
    @Test
    public void testEndpointTest2() throws Exception {
        performDirTest("test2");
    }
    
    @Test
    public void testEndpointTest2_1() throws Exception {
        performDirTest("test2");
    }
    
    @Test
    public void testEndpointTest3() throws Exception {
        performDirTest("vhost");
    }
    
    @Test
    public void testEndpointTest4() throws Exception {
        performDirTest("vhost-name");
    }
    
    @Test
    public void testEndpointTest5() throws Exception {
        performDirTest("vhost-port");
    }
    
    private void performDirTest(String testDir) throws Exception {
        final String methodName = "performDirTest";
        Log.entering(c, methodName);
        Log.info(c, methodName, "Performing test for " + testDir);

        Path testFilesDirectory = Paths.get(pathToAutoFVTTestFiles,testDir);
        Path expectedFile = Paths.get(testFilesDirectory.toAbsolutePath().toString(),"expected.xml");
        Path sourceConfigFiles = Paths.get(testFilesDirectory.toAbsolutePath().toString(),"source");

        Log.info(c, methodName, "working directory: " + workingDirectory.toAbsolutePath().toString());
        ProgramOutput po = machine.execute(defaultServerInstallRoot + "/bin/pluginUtility",
                new String[] {"merge",
                        "--sourcePath=" + sourceConfigFiles.toAbsolutePath().toString()
        }, workingDirectory.toAbsolutePath().toString());

        Log.info(c, methodName, "-merge result:\n" + po.getStdout());
        assertEquals("pluginUtility task should complete with return code as 0.", 0, po.getReturnCode());
        assertTrue("Merged plugin-cfg.xml does not exist under " + workingDirectory.toAbsolutePath().toString(), Files.exists(mergedPluginCfgFile));

        List<XMLIssue> issuesFound = XMLCompare.getInstance().compareEndpoints(expectedFile.toFile(),mergedPluginCfgFile.toFile());
        StringBuffer sb = new StringBuffer();
        boolean compareFailed = issuesFound.size() > 0;
        if(compareFailed) {
            for(XMLIssue issue:issuesFound) {
                sb.append("\t"+issue.getPath()+"\n");
                sb.append("\t\t"+issue.getProblem()+"\n");
            }
            List<String> fileList = convertFileToList(mergedPluginCfgFile.toAbsolutePath().toString());
            Log.info(c, methodName, "Failed merge file contents:");
            for(String line:fileList) {
                Log.info(c, methodName, line);
            }
        }
        assertFalse("Merged plugin-cfg.xml does not match expected plugin file for  " + testDir + ":\n" + sb.toString(), compareFailed);
        
        Log.exiting(c, methodName);

    }

    public List<String> convertFileToList(String filename) {
        List<String> list = new ArrayList<>();

        try (Stream<String> stream = Files.lines(Paths.get(filename))) {
            list = stream.collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }
}
