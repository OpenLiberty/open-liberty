//%Z% %I% %W% %G% %U% [%H% %T%]
/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************
 *
 * DESCRIPTION:
 *
 * Change History:
 *
 * Reason       Version     Date        User id     Description
 * ----------------------------------------------------------------------------
 * 91947         8.5.5     13/06/2013    sumam     Test case for defect 91947.
 */

package com.ibm.ws.logging.hpel.fat;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Test case for defect 91947 : HPEL purgeMaxSize/purgeMinTime should follow convention for min value from logging
 * Test scenario: Set the server.xml purgeMaxSize = -1 and startServer the server warning message should be generated
 */
@RunWith(FATRunner.class)
public class HPELPurgeMaxSizeErrorTest {

    private final static String loggerName = HPELPurgeMaxSizeErrorTest.class.getName();
    private final static Logger logger = Logger.getLogger(loggerName);
    private final static String MESSAGE_LOG = "logs/HPELPurgeMaxSizeErrorTest.log";

    @Server("HpelServer")
    public static LibertyServer server;
    static RemoteFile backup = null;

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
            server.stopServer();
            server.startServer();
        }

        CommonTasks.writeLogMsg(Level.INFO, "Configuring server for test case.");
        backup = new RemoteFile(server.getMachine(), new File(server.getServerRoot(), "server-backup.xml").getPath());
        if (!backup.exists()) {
            backup.copyFromSource(server.getServerConfigurationFile());
        }
        // Setting the server.xml with purgeMaxSize = -1
        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles, "server-HPELPurgeMaxSizeInvalid.xml"));
        if (!server.isStarted()) {
            server.startServer();
        }

    }

    /**
     * Test that server gives error message for setting purgeMaxSize = -1
     * Set the purgeMaxSize property = -1 in server.xml for both <binaryLog> and <binaryTrace>, startServer the server and check for the warning message for invalid purgeMaxSize.
     **/
    @Test
    public void testPurgeMaxSizeProperty() throws Exception {

        CommonTasks.writeLogMsg(Level.INFO, " Verifying the console.log should have a error message for invalid purgeMaxSize property ");
        logger.info(" The console.log should have a error message CWWKG0075E for setting purgeMaxSize property to -1 ");
        checkErrorMessageForInvalidPurgeMaxSizeExists();

    }

    //Check if we have one Error Message for invalid purgeMaxSize property.
    protected void checkErrorMessageForInvalidPurgeMaxSizeExists() throws Exception {
        List<String> lines = server.findStringsInFileInLibertyServerRoot("CWWKG0075E", MESSAGE_LOG);
        assertTrue(" Message CWWKG0075E did not appear in console.log  ", lines.size() > 0);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Restore values we saw before changing them in setUp()
        CommonTasks.writeLogMsg(Level.INFO, "Resetting configuration to pre test values.");
        if (backup != null && backup.exists()) {
            server.getServerConfigurationFile().copyFromSource(backup);
        }

    }

}