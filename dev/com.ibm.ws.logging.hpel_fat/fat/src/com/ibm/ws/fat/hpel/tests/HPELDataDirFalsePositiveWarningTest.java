/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
import java.util.List;
import java.util.logging.Logger;

import com.ibm.websphere.simplicity.ApplicationServer;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.ws.fat.VerboseTestCase;
import com.ibm.ws.fat.hpel.setup.HpelSetup;
import com.ibm.ws.fat.ras.util.CommonTasks;

/**
 * Test case for defect 91932: HPEL gives false positive warning on disk space after changing dataDirectory
 * Test Scenario: Start the server by logDirectory as logs → modify it to logX and delete the old directory (logs) , no warning message should be generated.
 * 
 */

public class HPELDataDirFalsePositiveWarningTest extends VerboseTestCase {

    private final static String loggerName = HPELDataDirFalsePositiveWarningTest.class.getName();
    private final static Logger logger = Logger.getLogger(loggerName);
    private final static String MESSAGE_LOG = "logs/console.log";

    private ApplicationServer appServ = null;

    RemoteFile backup = null;

    public HPELDataDirFalsePositiveWarningTest(String name) {
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
            appServ.stop();
            appServ.start();
            this.logStepCompleted();
        }

        this.logStep("Configuring server for test case.");
        backup = new RemoteFile(appServ.getBackend().getMachine(), new File(appServ.getBackend().getServerRoot(), "server-backup.xml").getPath());
        if (!backup.exists()) {
            backup.copyFromSource(appServ.getBackend().getServerConfigurationFile());
        }
        appServ.getBackend().updateServerConfiguration(new File(appServ.getBackend().pathToAutoFVTTestFiles, "server-HPELDeleteLogDirectory_1.xml"));
        if (!appServ.getBackend().isStarted()) {
            appServ.start();
        }

        this.logStepCompleted();

    }

    /**
     * Test that HPEL's logDirectory change does not give any warning message.
     * Start the server with logDirectory as "LogX1", during the runtime change the logDirectory to "logX2" and delete the old directory i.e. logX1,
     * No warning message should be created for deleting the old unused directory
     * 
     **/
    public void testLogDirectoryChange() throws Exception {

        this.logStep("Configuring server for test case.");
        appServ.getBackend().updateServerConfiguration(new File(appServ.getBackend().pathToAutoFVTTestFiles, "server-HPELDeleteLogDirectory_2.xml"));
        this.logStepCompleted();

        // Deleting the old directory and checking for the warning message. 

        this.logStep(" Deleting the logs directory to check the warning message");
        appServ.getBackend().deleteFileFromLibertyServerRoot("logX1");
        this.logStepCompleted();

        this.logVerificationPoint("Verifying no warning message generated for old log directory deleted .");
        logger.info("The console.log should not have a error message HPEL0161W ");
        checkWarningMessageForDirectoryDelete();
        this.logVerificationPassed();

    }

    //Check we dont have any warning message for old directory deleted.
    protected void checkWarningMessageForDirectoryDelete() throws Exception {
        List<String> lines = appServ.getBackend().findStringsInFileInLibertyServerRoot("HPEL0161W", MESSAGE_LOG);
        assertEquals("Message HPEL0161W appeared in the console file", 0, lines.size());
    }

    @Override
    public void tearDown() throws Exception {
        // Restore values we saw before changing them in setUp()
        this.logStep("Resetting configuration to pre test values.");
        if (backup != null && backup.exists()) {
            appServ.getBackend().getServerConfigurationFile().copyFromSource(backup);
        }
        this.logStepCompleted();

        // call the super
        super.tearDown();
    }

}