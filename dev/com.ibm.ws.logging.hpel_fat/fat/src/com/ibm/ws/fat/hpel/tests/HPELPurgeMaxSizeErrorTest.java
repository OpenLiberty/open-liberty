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
 * Test case for defect 91947 : HPEL purgeMaxSize/purgeMinTime should follow convention for min value from logging
 * Test scenario: Set the server.xml purgeMaxSize = -1 and start the server warning message should be generated
 */

public class HPELPurgeMaxSizeErrorTest extends VerboseTestCase {

    private final static String loggerName = HPELPurgeMaxSizeErrorTest.class.getName();
    private final static Logger logger = Logger.getLogger(loggerName);
    private final static String MESSAGE_LOG = "logs/console.log";

    private ApplicationServer appServ = null;

    RemoteFile backup = null;

    public HPELPurgeMaxSizeErrorTest(String name) {
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
        // Setting the server.xml with purgeMaxSize = -1 
        appServ.getBackend().updateServerConfiguration(new File(appServ.getBackend().pathToAutoFVTTestFiles, "server-HPELPurgeMaxSizeInvalid.xml"));
        if (!appServ.getBackend().isStarted()) {
            appServ.start();
        }

        this.logStepCompleted();

    }

    /**
     * Test that server gives error message for setting purgeMaxSize = -1
     * Set the purgeMaxSize property = -1 in server.xml for both <binaryLog> and <binaryTrace>, start the server and check for the warning message for invalid purgeMaxSize.
     **/
    public void testPurgeMaxSizeProperty() throws Exception {

        this.logVerificationPoint(" Verifying the console.log should have a error message for invalid purgeMaxSize property ");
        logger.info(" The console.log should have a error message CWWKG0075E for setting purgeMaxSize property to -1 ");
        checkErrorMessageForInvalidPurgeMaxSizeExists();
        this.logVerificationPassed();

    }

    //Check if we have one Error Message for invalid purgeMaxSize property.
    protected void checkErrorMessageForInvalidPurgeMaxSizeExists() throws Exception {
        List<String> lines = appServ.getBackend().findStringsInFileInLibertyServerRoot("CWWKG0075E", MESSAGE_LOG);
        assertTrue(" Message CWWKG0075E did not appear in console.log  ", lines.size() > 0);
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