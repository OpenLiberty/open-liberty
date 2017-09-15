/*******************************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.fat.hpel.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.simplicity.ApplicationServer;
import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.ws.fat.VerboseTestCase;
import com.ibm.ws.fat.hpel.setup.HpelSetup;
import com.ibm.ws.fat.ras.util.CommonTasks;

/**
 * Test case for defect 702533: Hpel leaves empty instance repositories behind
 * 
 * @author olteamh
 * 
 */

public class HpelDeleteEmptyDirectories extends VerboseTestCase {

    private final static String loggerName = HpelDeleteEmptyDirectories.class.getName();
    private final static Logger logger = Logger.getLogger(loggerName);
    private final static String tempTraceSpec = "*=info:" + loggerName + "*=all"; // trace spec needed for this test case.
    String defaultTraceSpec = null; // original trace spec
    boolean defaultLogPurgeEnabled; // original purgeBySize
    boolean defaultTracePurgeEnabled; // original purgeBySize
    int defaultLogPurgeSize;
    int defaultTracePurgeSize;

    RemoteFile backup = null;

    public HpelDeleteEmptyDirectories(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        // Call super.SetUp() cause we still want it's setup as well
        super.setUp();
        ApplicationServer appServ = HpelSetup.getServerUnderTest();
        // Confirm HPEL is enabled
        if (!CommonTasks.isHpelEnabled(appServ)) {
            // HPEL is not enabled.
            this.logStep("HPEL is not enabled on " + appServ.getName() + ", attempting to enable.");
            CommonTasks.setHpelEnabled(appServ, true);
            appServ.stop();
            appServ.start();
            this.logStepCompleted();
        }

        backup = new RemoteFile(appServ.getBackend().getMachine(), new File(appServ.getBackend().getServerRoot(), "server-backup.xml").getPath());
        if (!backup.exists()) {
            backup.copyFromSource(appServ.getBackend().getServerConfigurationFile());
        }
        appServ.getBackend().updateServerConfiguration(new File(appServ.getBackend().pathToAutoFVTTestFiles, "server-HpelDeleteEmptyDirectories.xml"));

//		ConfigObject logChild = CommonTasks.getBinaryLogChild(appServ);
//		defaultLogPurgeEnabled = logChild.getAttributeByName("purgeBySizeEnabled").getValueAsBoolean();
//		// Test requires that purgeBySize be enabled.
//		logChild.getAttributeByName("purgeBySizeEnabled").setValue(true);		
//		defaultLogPurgeSize = logChild.getAttributeByName("purgeMaxSize").getValueAsInt();
//		// Speed up test by setting purge size to minimum possible
//		logChild.getAttributeByName("purgeMaxSize").setValue(10);
//		
//		ConfigObject traceChild = CommonTasks.getBinaryTraceChild(appServ);
//		defaultTracePurgeEnabled = traceChild.getAttributeByName("purgeBySizeEnabled").getValueAsBoolean();
//		// Test requires that purgeBySize be enabled.
//		traceChild.getAttributeByName("purgeBySizeEnabled").setValue(true);		
//		defaultTracePurgeSize = traceChild.getAttributeByName("purgeMaxSize").getValueAsInt();
//		// Speed up test by setting purge size to minimum possible
//		traceChild.getAttributeByName("purgeMaxSize").setValue(10);

        defaultTraceSpec = CommonTasks.getHpelTraceSpec(appServ);
        CommonTasks.setHpelTraceSpec(appServ, tempTraceSpec);

        // Sync changes before restarting the server.
        HpelSetup.getCellUnderTest().getWorkspace().saveAndSync();

        // stop the server
        if (appServ.getBackend().isStarted()) {
//        if (appServ.getServerStatus().equals(ProcessStatus.RUNNING)) {
            // The server is running.
            this.logStep("The server is running, attempting to stop.");
            appServ.stop();
            this.logStepCompleted();
        }

        // restart the server
        if (!appServ.getBackend().isStarted()) {
//        if (!appServ.getServerStatus().equals(ProcessStatus.RUNNING)) {
            // The server is not running.
            this.logStep("The server is not running, attempting to start.");
            appServ.start();
            this.logStepCompleted();
        }
    }

    /**
     * This test writes enough entries to cause HPEL to delete the .wbl files from the
     * previous server instance and checks that the parent directories are deleted when empty
     **/
    public void testHPELDeleteEmptyInstanceDirectories() throws Exception {
        String repositoryDirString = null;
        int logPurgeMaxSize = 0;
        int tracePurgeMaxSize = 0;
        int logLoopsToDo = 0;
        int traceLoopsToDo = 0;
        ApplicationServer appServ = null;
        RemoteFile serverLogdataDir = null;
        RemoteFile serverTracedataDir = null;
        Machine remoteMachine = null;

        this.logStep("Building parameters");
        appServ = HpelSetup.getServerUnderTest();
        remoteMachine = appServ.getNode().getMachine();

        RemoteFile repositoryDir = CommonTasks.getBinaryLogDir(HpelSetup.getServerUnderTest());
        repositoryDirString = repositoryDir.getAbsolutePath();

        serverLogdataDir = new RemoteFile(remoteMachine, repositoryDir, "logdata");
        serverTracedataDir = new RemoteFile(remoteMachine, repositoryDir, "tracedata");

        logPurgeMaxSize = 10; //CommonTasks.getBinaryLogChild(appServ).getAttributeByName("purgeMaxSize").getValueAsInt();
        tracePurgeMaxSize = 10; //CommonTasks.getBinaryTraceChild(appServ).getAttributeByName("purgeMaxSize").getValueAsInt();
        logger.log(Level.INFO, "HPEL trace spec is: " + CommonTasks.getHpelTraceSpec(appServ));
        logger.log(Level.INFO, "Running the test case for repository directory: " + repositoryDirString);
        logger.log(Level.INFO, "Running the test case for logdata directory: " + serverLogdataDir);
        logger.log(Level.INFO, "Running the test case for tracedata directory: " + serverTracedataDir);
        logger.log(Level.INFO, "Running the test case with trace purge size: " + tracePurgeMaxSize);
        logger.log(Level.INFO, "Running the test case with log purge size: " + logPurgeMaxSize);
        logger.log(Level.INFO, "Application server is named: " + appServ.getName());

        //Make sure the arguments are valid
        assertNotNull("The repository directory is null", repositoryDirString);
        assertNotNull("The repository log directory is null", serverLogdataDir);
        assertNotNull("The repository  tracedirectory is null", serverTracedataDir);

        //Get list of all directories in repository before the test starts
        RemoteFile[] preLogDirs = listInstanceDirectories(serverLogdataDir);
        RemoteFile[] preTraceDirs = listInstanceDirectories(serverTracedataDir);

        //all empty instance repository dirs should have been deleted when the server starts
        for (int m = 0; m < preLogDirs.length; m++) {
            assertTrue("An empty log instance directory " + preLogDirs[m] + " has been found after server start up", preLogDirs[m].list(true).length != 0);
        }

        for (int m = 0; m < preTraceDirs.length; m++) {
            assertTrue("An empty trace instance directory " + preTraceDirs[m] + " has been found after server start up", preTraceDirs[m].list(true).length != 0);
        }

        // Calculate number of loops of logging to perform
        int logBytesToGo = (logPurgeMaxSize * 1024 * 1024); // bytes remaining before max
        int traceBytesToGo = (tracePurgeMaxSize * 1024 * 1024);
        logLoopsToDo = (logBytesToGo / 100);
        traceLoopsToDo = (traceBytesToGo / 100);
        logger.log(Level.INFO, "Maximum log iterations: " + logLoopsToDo);
        logger.log(Level.INFO, "Maximum trace iterations: " + traceLoopsToDo);

        // Log messages on application server.
        this.logStep("Creating log messages.");

        // Write some log records to have a reference point for the big loop bellow.
        CommonTasks.createLogEntries(appServ, loggerName, "Initial log record for delete empty instance repositories test.",
                                     null, 10, CommonTasks.LOGS, 0);

        RemoteFile logFile = getMostRecentLog(serverLogdataDir);
        assertNotNull("Did not find any WBL files in log directory " + serverLogdataDir, logFile);
        for (int i = 0; i < logLoopsToDo; i += 100) {
            CommonTasks.createLogEntries(appServ, loggerName, "Log record for delete empty instance repositories test.",
                                         null, 100, CommonTasks.LOGS, 0);
            // Keep logging until all files we had before the test are purged
            if (!logFile.exists()) {
                logger.log(Level.INFO, "Exiting writting log records after {0} loops", i);
                break;
            }
        }

        // Write some trace records to have a reference point for the big loop bellow.
        CommonTasks.createLogEntries(appServ, loggerName, "Initial trace record for delete empty instance repositories test.",
                                     null, 10, CommonTasks.TRACE, 0);

        RemoteFile traceFile = getMostRecentLog(serverTracedataDir);
        assertNotNull("Did not find any WBL files in trace directory " + serverTracedataDir, traceFile);
        for (int i = 0; i < traceLoopsToDo; i += 100) {
            CommonTasks.createLogEntries(appServ, loggerName, "Trace record for delete empty instance repositories test.",
                                         null, 100, CommonTasks.TRACE, 0);
            // Keep logging until all files we had before the test are purged
            if (!traceFile.exists()) {
                logger.log(Level.INFO, "Exiting writting trace records after {0} loops", i);
                break;
            }
        }

        this.logStepCompleted();

        // Short pause to ensure that any deletes HPEL did are processed before we recalculate.
        Thread.sleep(2500);

        RemoteFile[] postLogDirs = listInstanceDirectories(serverLogdataDir);
        RemoteFile[] postTraceDirs = listInstanceDirectories(serverTracedataDir);

        //Check that there are no empty instance directories at the end of the test
        for (int m = 0; m < postLogDirs.length; m++) {
            assertTrue("An empty log instance directory has been found", postLogDirs[m].list(true).length != 0);

        }
        for (int m = 0; m < postTraceDirs.length; m++) {
            assertTrue("An empty trace instance directory has been found", postTraceDirs[m].list(true).length != 0);
        }

    }

    @Override
    public void tearDown() throws Exception {
        ApplicationServer appServ = HpelSetup.getServerUnderTest();

        // Restore values we saw before changing them in setUp()
        this.logStep("Resetting configuration to pre test values.");
        if (backup != null && backup.exists()) {
            appServ.getBackend().getServerConfigurationFile().copyFromSource(backup);
        }

//		ConfigObject logChild = CommonTasks.getBinaryTraceChild(appServ);
//		logChild.getAttributeByName("purgeBySizeEnabled").setValue(defaultLogPurgeEnabled);		
//		logChild.getAttributeByName("purgeMaxSize").setValue(defaultLogPurgeSize);
//		
//		ConfigObject traceChild = CommonTasks.getBinaryTraceChild(appServ);
//		traceChild.getAttributeByName("purgeBySizeEnabled").setValue(defaultTracePurgeEnabled);		
//		traceChild.getAttributeByName("purgeMaxSize").setValue(defaultTracePurgeSize);

        CommonTasks.setHpelTraceSpec(appServ, defaultTraceSpec);

        HpelSetup.getCellUnderTest().getWorkspace().saveAndSync();

        // No need to restart server in Liberty if it's not bootstrap.properties changes.
//		this.logStep("Restarting server " + appServ.getName() + " to enable settings available before the test.");
//		appServ.stop();
//		appServ.start();
//		this.logStepCompleted();

        // call the super
        super.tearDown();
    }

    // Find most recent WBL file in the repository
    private RemoteFile getMostRecentLog(RemoteFile repositoryBase) throws Exception {
        assertNotNull("RepositoryBase was unexpectedly null", repositoryBase);
        assertTrue(repositoryBase + " does not exist", repositoryBase.exists());
        assertTrue(repositoryBase + " is not a directory", repositoryBase.isDirectory());
        RemoteFile[] fileArray = repositoryBase.list(true);
        assertNotNull("Failed to list files in " + repositoryBase, fileArray);
        RemoteFile result = null;
        long timestamp = -1;
        for (RemoteFile curFile : fileArray) {
            String name = curFile.getName();
            if (curFile.isFile() && name.endsWith(".wbl")) {
                long otherTimestamp = Long.parseLong(name.substring(0, name.length() - 4));
                if (otherTimestamp > timestamp) {
                    result = curFile;
                    timestamp = otherTimestamp;
                }
            }
        }
        return result;
    }

    private RemoteFile[] listInstanceDirectories(RemoteFile repositoryBase) throws Exception {
        assertNotNull("RepositoryBase was unexpectedly null", repositoryBase);
        ArrayList<RemoteFile> curFiles = new ArrayList<RemoteFile>();

        assertTrue(repositoryBase + " does not exist", repositoryBase.exists());
        assertTrue(repositoryBase + " is not a directory", repositoryBase.isDirectory());
        RemoteFile[] fileArray = repositoryBase.list(true);
        assertNotNull("Failed to list files in " + repositoryBase, fileArray);
        // recursive=true cause all files and directories return from the call, so no need to make calls in subdirectories.
        for (RemoteFile curFile : fileArray) {
            if (curFile.isDirectory()) {
                curFiles.add(curFile);
            }
        }

        logger.log(Level.INFO, "Listing " + repositoryBase + " found " + curFiles.size() + " directories");
        return curFiles.toArray(new RemoteFile[curFiles.size()]);
    }

}