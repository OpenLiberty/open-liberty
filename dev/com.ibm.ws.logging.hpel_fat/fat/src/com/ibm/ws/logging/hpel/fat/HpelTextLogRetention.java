//%Z% %I% %W% %G% %U% [%H% %T%]
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
 *
 * DESCRIPTION:
 *
 * Change History:
 *
 * Reason  		 Version	Date        User id     Description
 * ----------------------------------------------------------------------------
 * PM48157       8.0      10/03/2011    shighbar    HPEL TextLog retention policy does not remove previous server instances logs.
 */

package com.ibm.ws.logging.hpel.fat;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Test case for defect PM48157 : Hpel TextLog files retention policy not working. The retention policy was previously
 * working per server instance but not across multiple instances. As a result the size of the total TextLogs was able to
 * grow by up to purgeMaxSize each time there was a new instance (server bounce). Test case needs to check for retention
 * not only in a single run but across multiple runs to cover this case.
 *
 */
@RunWith(FATRunner.class)
public class HpelTextLogRetention {

    private final static String loggerName = HpelTextLogRetention.class.getName();
    private final static Logger logger = Logger.getLogger(loggerName);
    private final static int MAX_TEXTLOG_PURGE_SIZE = 10;
    @Server("HpelServer")
    public static LibertyServer server;

    boolean default_TextPurgeEnabled; // original purgeBySize for TextLog
    int default_TextPurgeSize;
    String default_outOfSpaceAction;

    static RemoteFile backup = null;
    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "LogFat", "com.ibm.ws.logging.hpel");
        ShrinkHelper.defaultDropinApp(server, "HpelFat", "com.ibm.ws.logging.hpel.servlet");
        // Confirm HPEL is enabled
        if (!CommonTasks.isHpelEnabled(server)) {
            // HPEL is not enabled.
            CommonTasks.writeLogMsg(Level.INFO, "HPEL is not enabled on " + server.getServerName() + ", attempting to enable.");
            CommonTasks.setHpelEnabled(server, true);
            // RestartServer now to complete switching to HPEL
            server.restartServer();

        }

        CommonTasks.writeLogMsg(Level.INFO, "Configuring server for test case.");
        backup = new RemoteFile(server.getMachine(), new File(server.getServerRoot(), "server-backup.xml").getPath());
        if (!backup.exists()) {
            backup.copyFromSource(server.getServerConfigurationFile());
        }
        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles, "server-HpelTextLogRetention.xml"));

        if (!server.isStarted()) {
            server.startServer();
        }
//        ConfigObject textChild = CommonTasks.getTextLogChild(server);
//        default_TextPurgeEnabled = textChild.getAttributeByName("purgeBySizeEnabled").getValueAsBoolean();
//        // Test requires that purgeBySize be enabled.
//        textChild.getAttributeByName("purgeBySizeEnabled").setValue(true);
//        default_TextPurgeSize = textChild.getAttributeByName("purgeMaxSize").getValueAsInt();
//        // Speed up test by setting purge size to minimum possible
//        textChild.getAttributeByName("purgeMaxSize").setValue(MAX_TEXTLOG_PURGE_SIZE);
//        // Check that outOfSpaceAction is set to PurgeOld
//        default_outOfSpaceAction = textChild.getAttributeByName("outOfSpaceAction").getValueAsString();
//        textChild.getAttributeByName("outOfSpaceAction").setValue("PurgeOld");
//
//        // Sync changes before restartServering the server.
//        HpelSetup.getCellUnderTest().getWorkspace().saveAndSync();

        // restartServer the server
//        CommonTasks.writeLogMsg(Level.INFO, "Starting/restartServering application server to apply config changes.");
//        server.stopServer();
//        server.startServer();
//

    }

    /**
     * Test that HPEL's TextLog size based retention policy works. Both within a single server instance and across
     * server restartServers.
     **/
    @Test
    public void testHPELTextLogSizeRetention() throws Exception {
        RemoteFile TextLogDir = CommonTasks.getTextLogDir(server);
        NumberFormat nf = NumberFormat.getInstance();

        // write enough records to force size based retention policy to kick in.
        // there is ~600 bytes per log record. Using 200 to allow buffer.
        CommonTasks.writeLogMsg(Level.INFO, "Writting log records to fill TextLog repository.");
        long loopsPerFullRepository = (MAX_TEXTLOG_PURGE_SIZE * 1024 * 1024) / 200;
        logger.info("writting " + nf.format(loopsPerFullRepository) + " log loops to produce " + MAX_TEXTLOG_PURGE_SIZE
                    + " MB of data.");
        CommonTasks.createLogEntries(server, loggerName, "Sample log record for the test case " + name.getMethodName() + ".", null,
                                     (int) loopsPerFullRepository, CommonTasks.LOGS, 0);

        CommonTasks.writeLogMsg(Level.INFO, "Verifying that repository has not grown above max purge size.");
        long logsSize = getSizeOfTextLogs(TextLogDir);
        logger.info("The current size of TextLog files in " + TextLogDir.getAbsolutePath() + " is " + nf.format(logsSize));
        assertTrue("TextLog Repository size shouldn't be larger than purgeMaxSize",
                   logsSize <= (MAX_TEXTLOG_PURGE_SIZE * 1024 * 1024));

        CommonTasks.writeLogMsg(Level.INFO, "RestartServering Server in preperation to check that retention policy spans server instances");
        server.stopServer();
        server.startServer();

        CommonTasks.writeLogMsg(Level.INFO, "Writting log records to repository");
        // We don't have to fill the repository - half the repository size will work fine.
        logger.info("writting " + nf.format((int) (loopsPerFullRepository / 2)) + " log loops to produce " + MAX_TEXTLOG_PURGE_SIZE
                                                                                                             / 2
                    + " MB of data.");
        CommonTasks.createLogEntries(server, loggerName, "Sample log record for the test case " + name.getMethodName() + ".", null,
                                     (int) (loopsPerFullRepository / 2), CommonTasks.LOGS, 0);

        CommonTasks.writeLogMsg(Level.INFO, "Verifying that the repository size is still below max purge size.");
        logsSize = getSizeOfTextLogs(TextLogDir);
        logger.info("The current size of TextLog files in " + TextLogDir.getAbsolutePath() + " is " + nf.format(logsSize));
        assertTrue("TextLog Repository size shouldn't be larger than purgeMaxSize after server restartServer.",
                   logsSize <= (MAX_TEXTLOG_PURGE_SIZE * 1024 * 1024));

    }

    /**
     * Returns the total size of TextLog files in the given directory
     *
     * @throws Exception
     **/
    private long getSizeOfTextLogs(RemoteFile dirToCheck) throws Exception {
        long TotalTextRepositorySize = 0;
        RemoteFile[] AllTextLogFiles = dirToCheck.list(false);
        for (RemoteFile i : AllTextLogFiles) {
            if (i.getName().startsWith("TextLog_")) {
                // counting this as a valid TextLog log file.
                TotalTextRepositorySize += i.length();
            }
        }
        return TotalTextRepositorySize;
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Restore values we saw before changing them in setUp()
        CommonTasks.writeLogMsg(Level.INFO, "Resetting configuration to pre test values.");
        if (backup != null && backup.exists()) {
            server.getServerConfigurationFile().copyFromSource(backup);
        }
//        ConfigObject textChild = CommonTasks.getTextLogChild(server);
//        textChild.getAttributeByName("purgeBySizeEnabled").setValue(default_TextPurgeEnabled);
//        textChild.getAttributeByName("purgeMaxSize").setValue(default_TextPurgeSize);
//        textChild.getAttributeByName("outOfSpaceAction").setValue(default_outOfSpaceAction);
//
//        HpelSetup.getCellUnderTest().getWorkspace().saveAndSync();

        // No need to restartServer server in Liberty if it's not bootstrap.properties changes.
//        CommonTasks.writeLogMsg(Level.INFO, "RestartServering server " + server.getName() + " to enable settings available before the test.");
//        server.stopServer();
//        server.startServer();
//

        // call the super
    }

    /**
     * Determine if we should or should not execute this test. Returns true if the test should NOT be ran.
     **/
    public boolean skipTest() {
        // Test does not do any good on z/OS since TextLog is for Controller only - so we can't generate logs to fill up
        // TextLog repository. This may need to be revisited if we implement TextLog for servant.
        try {
            return server.getMachine().getOperatingSystem().equals(OperatingSystem.ZOS);
        } catch (Exception e) {
            CommonTasks.writeLogMsg(Level.SEVERE, "Unable to determine if we are on z/OS or not. Not skipping test");
            e.printStackTrace(System.err);
        }
        return false;
    }

}