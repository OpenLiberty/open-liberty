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
package com.ibm.ws.webserver.plugin.utility.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * This test class contains tests for actually testing the function of the merge action of
 * the pluginUtility command-line tool.
 *
 */
@RunWith(FATRunner.class)
public class PluginUtilityMergeTest {
    
    private static final Class<?> c = PluginUtilityMergeTest.class;
    private static final LibertyServer defaultServer = FATSuite.defaultServer;
    
    private static final String MERGED_PLUGIN_CFG_FILENAME = "merged-plugin-cfg.xml";
    private static final String TEST_MERGED_PLUGIN_CFG_FILENAME = "test_merged-plugin-cfg.xml";
    
    static Machine machine;
    String defaultServerInstallRoot = defaultServer.getInstallRoot();
    
    @BeforeClass
    public static void setUp() throws Exception {

        machine = defaultServer.getMachine();
        defaultServer.startServer();


        // Pause for the smarter planet message
        assertNotNull("The smarter planet message did not get printed",
                defaultServer.waitForStringInLog("CWWKF0011I"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            if (defaultServer.isStarted()) {
                defaultServer.stopServer();
            }
        } catch (Exception e) {
            Log.info(c, "tearDown", "Failed to stop server: " + e);
        }   
    }
    
    @Before
    public void cleanUp() throws Exception {
        String methodName = "cleanUp";
        // Delete files that could be present before each test
        if(defaultServer.fileExistsInLibertyServerRoot(MERGED_PLUGIN_CFG_FILENAME)) {
            Log.info(c, methodName, "deleting " + MERGED_PLUGIN_CFG_FILENAME);
            defaultServer.deleteFileFromLibertyServerRoot(MERGED_PLUGIN_CFG_FILENAME);
        }
        
        if(defaultServer.fileExistsInLibertyServerRoot(TEST_MERGED_PLUGIN_CFG_FILENAME)) {
            Log.info(c, methodName, "deleting " + TEST_MERGED_PLUGIN_CFG_FILENAME);
            defaultServer.deleteFileFromLibertyServerRoot(TEST_MERGED_PLUGIN_CFG_FILENAME);
        }
        
        if(defaultServer.fileExistsInLibertyServerRoot("plugin-cfg1.xml")){
            Log.info(c, methodName, "deleting plugin-cfg1.xml");
            defaultServer.deleteFileFromLibertyServerRoot("plugin-cfg1.xml");
        }
        
        if(defaultServer.fileExistsInLibertyServerRoot("plugin-cfg2.xml")){
            Log.info(c, methodName, "deleting plugin-cfg2.xml");
            defaultServer.deleteFileFromLibertyServerRoot("plugin-cfg2.xml");
        }
        
    } 

    /**
     * This is a test to ensure that two plugin-cfg.xml files can be successfully merged together.
     * 1) The plugin-cfg.xml files are existing (part of the test bucket).
     * 2) The plugin-cfg.xml files are transferred to the defaultServer
     * 3) The following command is issued: 
     *      pluginUtility merge --sourcePath=<defaultServer server Root>
     * This tests the directory merge (merge all plugin-cfg.xml files in a provided source directory).
     * @throws Exception
     */
    @Test
    public void testPluginUtilityMergeFilePath() throws Exception {

        final String methodName = "testPluginUtilityMergeFilePath";
        Log.entering(c, methodName);
        String targetDir = defaultServer.getServerRoot();
        Log.info(c, methodName, "targetDir: " + targetDir);

        File testFile = new File(targetDir + "/" + MERGED_PLUGIN_CFG_FILENAME);

        defaultServer.copyFileToLibertyServerRoot("plugin-cfg1.xml");
        defaultServer.copyFileToLibertyServerRoot("plugin-cfg2.xml");

        // Use targetDir as the working directory and by default if no targetPath is specified the 
        // merged-plugin-cfg.xml will be output to the current working directory
        ProgramOutput po = machine.execute(defaultServerInstallRoot + "/bin/pluginUtility",
                new String[] {"merge",
                        "--sourcePath=" + targetDir
        }, targetDir);

        Log.info(c, methodName, "-merge result:\n" + po.getStdout());
        assertEquals("pluginUtility task should complete with return code as 0.", 0, po.getReturnCode());
        assertTrue("Merged plugin-cfg.xml does not exist under " + targetDir , testFile.exists());

        Log.exiting(c, methodName);

    }

    /**
     * This is a test to ensure that two plugin-cfg.xml files can be successfully merged together.
     * 1) The plugin-cfg.xml files are existing (part of the test bucket).
     * 2) The plugin-cfg.xml files are transferred to the defaultServer
     * 3) The following command is issued: 
     *      pluginUtility merge --sourcePath=path-to-plugin-cfg.xml, path-to-plugin-cfg.xml
     * This tests the file merge (merge all plugin-cfg.xml files provided in a comma delineated list).
     * @throws Exception
     */
    @Test
    public void testPluginUtilityMergeTwoFiles() throws Exception {

        final String methodName = "testPluginUtilityMergeTwoFiles";
        Log.entering(c, methodName);
        String targetDir = defaultServer.getServerRoot();
        Log.info(c, methodName, "targetDir: " + targetDir);

        File testFile = new File(targetDir + "/" + MERGED_PLUGIN_CFG_FILENAME);

        defaultServer.copyFileToLibertyServerRoot("plugin-cfg1.xml");
        defaultServer.copyFileToLibertyServerRoot("plugin-cfg2.xml");

        // Use targetDir as the working directory and by default if no targetPath is specified the 
        // merged-plugin-cfg.xml will be output to the current working directory
        ProgramOutput po = machine.execute(defaultServerInstallRoot + "/bin/pluginUtility",
                new String[] {"merge",
                        "--sourcePath=" + targetDir+"/plugin-cfg1.xml," + targetDir + "/plugin-cfg2.xml"
        }, targetDir);

        Log.info(c, methodName, "-merge result:\n" + po.getStdout());
        assertEquals("pluginUtility task should complete with return code as 0.", 0, po.getReturnCode());
        assertTrue("Merged plugin-cfg.xml does not exist under " + targetDir , testFile.exists());

        Log.exiting(c, methodName);

    }
    
    /**
     * This is a test to ensure that two plugin-cfg.xml files can be successfully merged together and
     * that the resulting merged-plugin-cfg.xml is output in the targetPath specified.
     * 
     * 1) The plugin-cfg.xml files are existing (part of the test bucket).
     * 2) The plugin-cfg.xml files are transferred to the defaultServer
     * 3) The following command is issued: 
     *      pluginUtility merge --sourcePath=<defaultServer server Root> --targetPath=<defaultServer server Root
     * 4) The working directory of defaultServerInstallRoot/bin/ will be used to ensure targetPath is used properly.
     *     If targetPath is not used properly then the merged-plugin-cfg.xml would be in the workingDir rather than the targetPath 
     *     specified.
     *
     * This test will specify a directory to targetPath so the default name will be used
     *  
     * @throws Exception
     */
    @Test
    public void testPluginUtilityMergeFilePathUsingTargetPathDir() throws Exception {
        final String methodName = "testPluginUtilityMergeFilePathUsingTargetDirectory";
        Log.entering(c, methodName);
        String targetDir = defaultServer.getServerRoot();
        String workingDir = defaultServerInstallRoot + "/bin";
        
        Log.info(c, methodName, "targetDir: " + targetDir);
        Log.info(c, methodName, "workingDir: " + workingDir);

        File testFile = new File(targetDir + "/" + MERGED_PLUGIN_CFG_FILENAME );

        defaultServer.copyFileToLibertyServerRoot("plugin-cfg1.xml");
        defaultServer.copyFileToLibertyServerRoot("plugin-cfg2.xml");

        // Use workingDir as the working directory and specify the targetPath to equal the targetDir
        // to ensure that the merged-plugin-cfg.xml is created in the targetPath specified vs the 
        // current working directory.
        ProgramOutput po = machine.execute(defaultServerInstallRoot + "/bin/pluginUtility",
                new String[] {"merge",
                        "--sourcePath=" + targetDir,
                        "--targetPath=" + targetDir
        }, workingDir);

        Log.info(c, methodName, "-merge result:\n" + po.getStdout());
        assertEquals("pluginUtility task should complete with return code as 0.", 0, po.getReturnCode());
        assertTrue("Merged plugin-cfg.xml does not exist under " + targetDir , testFile.exists());

        Log.exiting(c, methodName);
    }
    
       /**
     * This is a test to ensure that two plugin-cfg.xml files can be successfully merged together and
     * that the resulting merged-plugin-cfg.xml is output in the targetPath specified.
     * 
     * 1) The plugin-cfg.xml files are existing (part of the test bucket).
     * 2) The plugin-cfg.xml files are transferred to the defaultServer
     * 3) The following command is issued: 
     *      pluginUtility merge --sourcePath=<defaultServer server Root> --targetPath=<defaultServer server Root
     * 4) The working directory of defaultServerInstallRoot/bin/ will be used to ensure targetPath is used properly.
     *     If targetPath is not used properly then the merged-plugin-cfg.xml would be in the workingDir rather than the targetPath 
     *     specified.
     *
     * This test will specify a fully qualified file name to targetPath so the default filename won't be used.
     *  
     * @throws Exception
     */
    @Test
    public void testPluginUtilityMergeFilePathUsingTargetPathFile() throws Exception {
        final String methodName = "testPluginUtilityMergeFilePathUsingTargetDirectory";
        Log.entering(c, methodName);
        String targetDir = defaultServer.getServerRoot();
        String workingDir = defaultServerInstallRoot + "/bin";
        
        Log.info(c, methodName, "targetDir: " + targetDir);
        Log.info(c, methodName, "workingDir: " + workingDir);

        File testFile = new File(targetDir + "/" + TEST_MERGED_PLUGIN_CFG_FILENAME );

        defaultServer.copyFileToLibertyServerRoot("plugin-cfg1.xml");
        defaultServer.copyFileToLibertyServerRoot("plugin-cfg2.xml");

        // Use workingDir as the working directory and specify the targetPath to be a fully qualified path
        // to ensure that the merged plugin-cfg.xml is created in the targetPath specified vs the 
        // current working directory using the name given and not the default merged-plugin-cfg.xml.
        ProgramOutput po = machine.execute(defaultServerInstallRoot + "/bin/pluginUtility",
                new String[] {"merge",
                        "--sourcePath=" + targetDir,
                        "--targetPath=" + testFile.getAbsolutePath()
        }, workingDir);

        Log.info(c, methodName, "-merge result:\n" + po.getStdout());
        assertEquals("pluginUtility task should complete with return code as 0.", 0, po.getReturnCode());
        assertTrue("Merged plugin-cfg.xml does not exist: " + testFile.getAbsolutePath() , testFile.exists());

        Log.exiting(c, methodName);
    }
    
    /**
     * Test to ensure that if a directory is specified to --sourcePath and there is just one
     * plugin-cfg.xml file present in that directory then no error message is reported.
     * 
     * This test case was included in PluginUtilityMergeTest vs PluginUtilityOutputTest since it copies a file
     * and this test already contains the appropriate @Before method to ensure the files are cleaned up 
     * before each test is executed.
     * 
     * @throws Exception
     */
    @Test
    public void testPluginUtilityMergeOneFile() throws Exception {
        String methodName = "testPluginUtilityMergeOneFile";
        Log.entering(c, methodName);
        
        String targetDir = defaultServer.getServerRoot();
        String workDir = defaultServer.getServerRoot();
        
        File testFile = new File(targetDir + "/" + TEST_MERGED_PLUGIN_CFG_FILENAME );
        
        // Copy just one plugin-cfg.xml file to the ServerRoot directory
        defaultServer.copyFileToLibertyServerRoot("plugin-cfg1.xml");
        
        // Use workingDir as the working directory and specify the targetPath to be a fully qualified path
        // to ensure that the merged plugin-cfg.xml is created in the targetPath specified vs the 
        // current working directory using the name given and not the default merged-plugin-cfg.xml.
        ProgramOutput po = machine.execute(defaultServerInstallRoot + "/bin/pluginUtility",
                new String[] {"merge",
                    "--sourcePath=" + workDir,
                    "--targetPath=" + testFile.getAbsolutePath()
                }, workDir);

        Log.info(c, methodName, "-merge result:\n" + po.getStdout());
        assertEquals("pluginUtility task should complete with return code as 0.", 0, po.getReturnCode());
        assertTrue("Merged plugin-cfg.xml does not exist: " + testFile.getAbsolutePath() , testFile.exists());
        
        Log.exiting(c,methodName);
    }
    
    /**
     * Test to ensure that if a directory is specified to --sourcePath and there is not at least one
     * plugin-cfg.xml file present in that directory an error message is given and processing is aborted.
     * 
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test
    public void testPluginUtilityMergeAtLeastOneFile() throws Exception {
        String methodName = "testPluginUtilityMergeAtLeastOneFile";
        Log.entering(c, methodName);
        
        String workDir = defaultServer.getServerRoot();
        
        // Don't copy any files to the working directory and invoke utility, expecting error   
        ProgramOutput po = machine.execute(defaultServerInstallRoot + "/bin/pluginUtility",
                new String[] {"merge",
                    "--sourcePath=" + workDir
                }, workDir);

        Log.info(c, methodName, "-merge result:\n" + po.getStdout());

        assertEquals("pluginUtility task should complete with return code as 0.", 0, po.getReturnCode());
        assertTrue("An error message should have been printed if only one file was in the directory and the merge action was invoked." ,
                po.getStdout().contains("Provide at least one plug-in configuration file to do the merge."));
        
        Log.exiting(c,methodName);
    }
}
