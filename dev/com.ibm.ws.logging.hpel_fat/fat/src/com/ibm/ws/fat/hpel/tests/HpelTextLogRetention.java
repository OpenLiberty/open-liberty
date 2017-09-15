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
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.simplicity.ApplicationServer;
import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.ws.fat.VerboseTestCase;
import com.ibm.ws.fat.hpel.setup.HpelSetup;
import com.ibm.ws.fat.ras.util.CommonTasks;

/**
 * Test case for defect PM48157 : Hpel TextLog files retention policy not working. The retention policy was previously
 * working per server instance but not across multiple instances. As a result the size of the total TextLogs was able to
 * grow by up to purgeMaxSize each time there was a new instance (server bounce). Test case needs to check for retention
 * not only in a single run but across multiple runs to cover this case.
 * 
 */

public class HpelTextLogRetention extends VerboseTestCase {

    private final static String loggerName = HpelTextLogRetention.class.getName();
    private final static Logger logger = Logger.getLogger(loggerName);
    private final static int MAX_TEXTLOG_PURGE_SIZE = 10;
    private ApplicationServer appServ = null;

    boolean default_TextPurgeEnabled; // original purgeBySize for TextLog
    int default_TextPurgeSize;
    String default_outOfSpaceAction;

    RemoteFile backup = null;

    public HpelTextLogRetention(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        // Call super.SetUp() cause we still want it's setup as well
        super.setUp();
        appServ = HpelSetup.getServerUnderTest();
        // Confirm HPEL is enabled
        if (!CommonTasks.isHpelEnabled(appServ)) {
            // HPEL is not enabled.
            this.logStep("HPEL is not enabled on " + appServ.getName() + ", attempting to enable.");
            CommonTasks.setHpelEnabled(appServ, true);
            // Restart now to complete switching to HPEL
            appServ.restart();
            this.logStepCompleted();
        }

        this.logStep("Configuring server for test case.");
        backup = new RemoteFile(appServ.getBackend().getMachine(), new File(appServ.getBackend().getServerRoot(), "server-backup.xml").getPath());
        if (!backup.exists()) {
            backup.copyFromSource(appServ.getBackend().getServerConfigurationFile());
        }
        appServ.getBackend().updateServerConfiguration(new File(appServ.getBackend().pathToAutoFVTTestFiles, "server-HpelTextLogRetention.xml"));

        if (!appServ.getBackend().isStarted()) {
            appServ.start();
        }
//        ConfigObject textChild = CommonTasks.getTextLogChild(appServ);
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
//        // Sync changes before restarting the server.
//        HpelSetup.getCellUnderTest().getWorkspace().saveAndSync();
        this.logStepCompleted();

        // restart the server
//        this.logStep("Starting/restarting application server to apply config changes.");
//        appServ.stop();
//        appServ.start();
//        this.logStepCompleted();

    }

    /**
     * Test that HPEL's TextLog size based retention policy works. Both within a single server instance and across
     * server restarts.
     **/
    public void testHPELTextLogSizeRetention() throws Exception {
        RemoteFile TextLogDir = CommonTasks.getTextLogDir(appServ);
        NumberFormat nf = NumberFormat.getInstance();

        // write enough records to force size based retention policy to kick in.
        // there is ~600 bytes per log record. Using 200 to allow buffer.
        this.logStep("Writting log records to fill TextLog repository.");
        long loopsPerFullRepository = (MAX_TEXTLOG_PURGE_SIZE * 1024 * 1024) / 200;
        logger.info("writting " + nf.format(loopsPerFullRepository) + " log loops to produce " + MAX_TEXTLOG_PURGE_SIZE
                    + " MB of data.");
        CommonTasks.createLogEntries(appServ, loggerName, "Sample log record for the test case " + this.getName() + ".", null,
                                     (int) loopsPerFullRepository, CommonTasks.LOGS, 0);
        this.logStepCompleted();

        this.logVerificationPoint("Verifying that repository has not grown above max purge size.");
        long logsSize = getSizeOfTextLogs(TextLogDir);
        logger.info("The current size of TextLog files in " + TextLogDir.getAbsolutePath() + " is " + nf.format(logsSize));
        assertTrue("TextLog Repository size shouldn't be larger than purgeMaxSize",
                   logsSize <= (MAX_TEXTLOG_PURGE_SIZE * 1024 * 1024));
        this.logVerificationPassed();

        this.logStep("Restarting Server in preperation to check that retention policy spans server instances");
        appServ.stop();
        appServ.start();

        this.logStep("Writting log records to repository");
        // We don't have to fill the repository - half the repository size will work fine.
        logger.info("writting " + nf.format((int) (loopsPerFullRepository / 2)) + " log loops to produce " + MAX_TEXTLOG_PURGE_SIZE
                    / 2 + " MB of data.");
        CommonTasks.createLogEntries(appServ, loggerName, "Sample log record for the test case " + this.getName() + ".", null,
                                     (int) (loopsPerFullRepository / 2), CommonTasks.LOGS, 0);
        this.logStepCompleted();

        this.logVerificationPoint("Verifying that the repository size is still below max purge size.");
        logsSize = getSizeOfTextLogs(TextLogDir);
        logger.info("The current size of TextLog files in " + TextLogDir.getAbsolutePath() + " is " + nf.format(logsSize));
        assertTrue("TextLog Repository size shouldn't be larger than purgeMaxSize after server restart.",
                   logsSize <= (MAX_TEXTLOG_PURGE_SIZE * 1024 * 1024));
        this.logVerificationPassed();
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

    @Override
    public void tearDown() throws Exception {
        // Restore values we saw before changing them in setUp()
        this.logStep("Resetting configuration to pre test values.");
        if (backup != null && backup.exists()) {
            appServ.getBackend().getServerConfigurationFile().copyFromSource(backup);
        }
//        ConfigObject textChild = CommonTasks.getTextLogChild(appServ);
//        textChild.getAttributeByName("purgeBySizeEnabled").setValue(default_TextPurgeEnabled);
//        textChild.getAttributeByName("purgeMaxSize").setValue(default_TextPurgeSize);
//        textChild.getAttributeByName("outOfSpaceAction").setValue(default_outOfSpaceAction);
//
//        HpelSetup.getCellUnderTest().getWorkspace().saveAndSync();
        this.logStepCompleted();

        // No need to restart server in Liberty if it's not bootstrap.properties changes.
//        this.logStep("Restarting server " + appServ.getName() + " to enable settings available before the test.");
//        appServ.stop();
//        appServ.start();
//        this.logStepCompleted();

        // call the super
        super.tearDown();
    }

    /**
     * Determine if we should or should not execute this test. Returns true if the test should NOT be ran.
     * **/
    @Override
    public boolean skipTest() {
        // Test does not do any good on z/OS since TextLog is for Controller only - so we can't generate logs to fill up
        // TextLog repository. This may need to be revisited if we implement TextLog for servant.
        try {
            return HpelSetup.getNodeUnderTest().getMachine().getOperatingSystem().equals(OperatingSystem.ZOS);
        } catch (Exception e) {
            CommonTasks.writeLogMsg(Level.SEVERE, "Unable to determine if we are on z/OS or not. Not skipping test");
            e.printStackTrace(System.err);
        }
        return false;
    }

}