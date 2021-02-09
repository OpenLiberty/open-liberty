//%Z% %I% %W% %G% %U% [%H% %T%]
/**
/*******************************************************************************
 * Copyright (c) 2002, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************
 *
 * @author olteamh
 *
 * DESCRIPTION:
 *
 * Change History:
 *
 * Reason  		 Version	Date        User id     Description
 * ----------------------------------------------------------------------------
 * 702533        8.0      09/01/2011    olteamh     Check that empty hpel instance directories are deleted.
 */

package com.ibm.ws.logging.hpel.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Test case for defect 702533: Hpel leaves empty instance repositories behind
 *
 * @author olteamh
 *
 */
@RunWith(FATRunner.class)
public class HpelDeleteEmptyDirectories {

    private final static String loggerName = HpelDeleteEmptyDirectories.class.getName();
    private final static Logger logger = Logger.getLogger(loggerName);
    private final static String tempTraceSpec = "*=info:" + loggerName + "*=all"; // trace spec needed for this test case.
    static String defaultTraceSpec = null; // original trace spec
    boolean defaultLogPurgeEnabled; // original purgeBySize
    boolean defaultTracePurgeEnabled; // original purgeBySize
    int defaultLogPurgeSize;
    int defaultTracePurgeSize;

    @Server("HpelServer")
    public static LibertyServer server;

    static RemoteFile backup = null;

    @BeforeClass
    public static void setUp() throws Exception {
        // Call super.SetUp() cause we still want it's setup as well
        // Confirm HPEL is enabled
        ShrinkHelper.defaultDropinApp(server, "LogFat", "com.ibm.ws.logging.hpel");
        ShrinkHelper.defaultDropinApp(server, "HpelFat", "com.ibm.ws.logging.hpel.servlet");

        backup = new RemoteFile(server.getMachine(), new File(server.getServerRoot(), "server-backup.xml").getPath());
        if (!backup.exists()) {
            backup.copyFromSource(server.getServerConfigurationFile());
        }
        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles, "server-HpelDeleteEmptyDirectories.xml"));

//		ConfigObject logChild = CommonTasks.getBinaryLogChild(server);
//		defaultLogPurgeEnabled = logChild.getAttributeByName("purgeBySizeEnabled").getValueAsBoolean();
//		// Test requires that purgeBySize be enabled.
//		logChild.getAttributeByName("purgeBySizeEnabled").setValue(true);
//		defaultLogPurgeSize = logChild.getAttributeByName("purgeMaxSize").getValueAsInt();
//		// Speed up test by setting purge size to minimum possible
//		logChild.getAttributeByName("purgeMaxSize").setValue(10);
//
//		ConfigObject traceChild = CommonTasks.getBinaryTraceChild(server);
//		defaultTracePurgeEnabled = traceChild.getAttributeByName("purgeBySizeEnabled").getValueAsBoolean();
//		// Test requires that purgeBySize be enabled.
//		traceChild.getAttributeByName("purgeBySizeEnabled").setValue(true);
//		defaultTracePurgeSize = traceChild.getAttributeByName("purgeMaxSize").getValueAsInt();
//		// Speed up test by setting purge size to minimum possible
//		traceChild.getAttributeByName("purgeMaxSize").setValue(10);

        defaultTraceSpec = CommonTasks.getHpelTraceSpec(server);
        CommonTasks.setHpelTraceSpec(server, tempTraceSpec);
//        HpelSetup.getCellUnderTest().getWorkspace().saveAndSync();

        // stop the server
        if (server.isStarted()) {
//        if (server.getServerStatus().equals(ProcessStatus.RUNNING)) {
            // The server is running.
            CommonTasks.writeLogMsg(Level.INFO, "The server is running, attempting to stop.");
            server.stopServer();
        }

        // restart the server
        if (!server.isStarted()) {
//        if (!server.getServerStatus().equals(ProcessStatus.RUNNING)) {
            // The server is not running.
            CommonTasks.writeLogMsg(Level.INFO, "The server is not running, attempting to start.");
            server.startServer();
        }
    }

    /**
     * This test writes enough entries to cause HPEL to delete the .wbl files from the
     * previous server instance and checks that the parent directories are deleted when empty
     **/
    @Test
    public void testHPELDeleteEmptyInstanceDirectories() throws Exception {
        String repositoryDirString = null;
        int logPurgeMaxSize = 0;
        int tracePurgeMaxSize = 0;
        int logLoopsToDo = 0;
        int traceLoopsToDo = 0;
        RemoteFile serverLogdataDir = null;
        RemoteFile serverTracedataDir = null;
        Machine remoteMachine = null;

        CommonTasks.writeLogMsg(Level.INFO, "Building parameters");
        remoteMachine = server.getMachine();

        RemoteFile repositoryDir = CommonTasks.getBinaryLogDir(server);
        repositoryDirString = repositoryDir.getAbsolutePath();

        serverLogdataDir = new RemoteFile(remoteMachine, repositoryDir, "logdata");
        serverTracedataDir = new RemoteFile(remoteMachine, repositoryDir, "tracedata");

        logPurgeMaxSize = 10; //CommonTasks.getBinaryLogChild(server).getAttributeByName("purgeMaxSize").getValueAsInt();
        tracePurgeMaxSize = 10; //CommonTasks.getBinaryTraceChild(server).getAttributeByName("purgeMaxSize").getValueAsInt();
        logger.log(Level.INFO, "HPEL trace spec is: " + CommonTasks.getHpelTraceSpec(server));
        logger.log(Level.INFO, "Running the test case for repository directory: " + repositoryDirString);
        logger.log(Level.INFO, "Running the test case for logdata directory: " + serverLogdataDir);
        logger.log(Level.INFO, "Running the test case for tracedata directory: " + serverTracedataDir);
        logger.log(Level.INFO, "Running the test case with trace purge size: " + tracePurgeMaxSize);
        logger.log(Level.INFO, "Running the test case with log purge size: " + logPurgeMaxSize);
        logger.log(Level.INFO, "Application server is named: " + server.getServerName());

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
        CommonTasks.writeLogMsg(Level.INFO, "Creating log messages.");

        // Write some log records to have a reference point for the big loop bellow.
        CommonTasks.createLogEntries(server, loggerName, "Initial log record for delete empty instance repositories test.",
                                     null, 10, CommonTasks.LOGS, 0);

        RemoteFile logFile = getMostRecentLog(serverLogdataDir);
        assertNotNull("Did not find any WBL files in log directory " + serverLogdataDir, logFile);
        for (int i = 0; i < logLoopsToDo; i += 100) {
            CommonTasks.createLogEntries(server, loggerName, "Log record for delete empty instance repositories test.",
                                         null, 100, CommonTasks.LOGS, 0);
            // Keep logging until all files we had before the test are purged
            if (!logFile.exists()) {
                logger.log(Level.INFO, "Exiting writting log records after {0} loops", i);
                break;
            }
        }

        // Write some trace records to have a reference point for the big loop bellow.
        CommonTasks.createLogEntries(server, loggerName, "Initial trace record for delete empty instance repositories test.",
                                     null, 10, CommonTasks.TRACE, 0);

        RemoteFile traceFile = getMostRecentLog(serverTracedataDir);
        assertNotNull("Did not find any WBL files in trace directory " + serverTracedataDir, traceFile);
        for (int i = 0; i < traceLoopsToDo; i += 100) {
            CommonTasks.createLogEntries(server, loggerName, "Trace record for delete empty instance repositories test.",
                                         null, 100, CommonTasks.TRACE, 0);
            // Keep logging until all files we had before the test are purged
            if (!traceFile.exists()) {
                logger.log(Level.INFO, "Exiting writting trace records after {0} loops", i);
                break;
            }
        }

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

    @AfterClass
    public static void tearDown() throws Exception {

        // Restore values we saw before changing them in setUp()
        CommonTasks.writeLogMsg(Level.INFO, "Resetting configuration to pre test values.");
        if (backup != null && backup.exists()) {
            server.getServerConfigurationFile().copyFromSource(backup);
        }

//		ConfigObject logChild = CommonTasks.getBinaryTraceChild(server);
//		logChild.getAttributeByName("purgeBySizeEnabled").setValue(defaultLogPurgeEnabled);
//		logChild.getAttributeByName("purgeMaxSize").setValue(defaultLogPurgeSize);
//
//		ConfigObject traceChild = CommonTasks.getBinaryTraceChild(server);
//		traceChild.getAttributeByName("purgeBySizeEnabled").setValue(defaultTracePurgeEnabled);
//		traceChild.getAttributeByName("purgeMaxSize").setValue(defaultTracePurgeSize);

        CommonTasks.setHpelTraceSpec(server, defaultTraceSpec);

//        HpelSetup.getCellUnderTest().getWorkspace().saveAndSync();

        // No need to restart server in Liberty if it's not bootstrap.properties changes.
//		CommonTasks.writeLogMsg(Level.INFO, "Restarting server " + server.getName() + " to enable settings available before the test.");
//		server.stopServer();
//		server.startServer();
//		this.logStepCompleted();

        // call the super
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