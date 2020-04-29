/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
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
 * Reason               Version        Date        User id     Description
 * ----------------------------------------------------------------------------
 * GH 12035             18.0           08/06/2018  pgunapal    Added test case for hideMessages in HPEL
 */
package com.ibm.ws.logging.hpel.fat;

import static org.junit.Assert.assertFalse;

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
 * Test scenario: Set the server.xml hideMessages logging attribute to hide a message and start the server.
 * The hidden message should not be displayed in the console.log, but visible in the log/trace data repository.
 */
@RunWith(FATRunner.class)
public class HPELHideMessagesTest {

    private final static String loggerName = HPELHideMessagesTest.class.getName();
    private final static Logger logger = Logger.getLogger(loggerName);
    private final static String CONSOLE_LOG = "logs/HPELHideMessagesTest.log";

    @Server("HpelServer")
    public static LibertyServer server;

    static RemoteFile backup = null;

    @BeforeClass
    public static void setUp() throws Exception {
        // Call super.SetUp() cause we still want it's setup as well
        ShrinkHelper.defaultDropinApp(server, "LogFat", "com.ibm.ws.logging.hpel");
        ShrinkHelper.defaultDropinApp(server, "HpelFat", "com.ibm.ws.logging.hpel.servlet");
        // Confirm HPEL is enabled
        if (!CommonTasks.isHpelEnabled(server)) {
            // HPEL is not enabled.
            CommonTasks.writeLogMsg(Level.INFO, "HPEL is not enabled on " + server.getServerName() + ", attempting to enable.");
            CommonTasks.setHpelEnabled(server, true);
            // Restart now to complete switching to HPEL
            server.restartServer();

        }

        CommonTasks.writeLogMsg(Level.INFO, "Configuring server for test case.");
        backup = new RemoteFile(server.getMachine(), new File(server.getServerRoot(), "server-backup.xml").getPath());
        if (!backup.exists()) {
            backup.copyFromSource(server.getServerConfigurationFile());
        }
        // Setting the server.xml with the hideMessages logging attribute
        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles, "server-HPELHideMessagesTest.xml"));
        // Restart server
        server.stopServer();
        server.startServer();
    }

    /**
     * Test that server hides the messages in the console.log, when binary logging is enabled
     * Set the hideMessages logging attribute to hide message that start with "CWWKF0012I" in server.xml, start the server and check the console.log file.
     **/
    @Test
    public void testHPELHideMessageLoggingAttribute() throws Exception {

        CommonTasks.writeLogMsg(Level.INFO, " Verifying the console.log should not have the message containing CWWKF0012I messageID. ");
        logger.info(" The console.log should not have the message containing CWWKF0012I messageID");
        checkIfMessageInConsoleLogExists("CWWKF0012I:");
    }

    // Check if the hidden message does not show up in the console.log
    protected void checkIfMessageInConsoleLogExists(String searchStr) throws Exception {
        List<String> lines = server.findStringsInFileInLibertyServerRoot(searchStr, CONSOLE_LOG);
        assertFalse(" Message CWWKF0012I did appear in console.log  ", lines.size() > 0);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Restore values we saw before changing them in setUp()
        CommonTasks.writeLogMsg(Level.INFO, "Resetting configuration to pre test values.");
        if (backup != null && backup.exists()) {
            server.getServerConfigurationFile().copyFromSource(backup);
        }
        if (server != null && server.isStarted()) {
            server.stopServer();
        }

    }

}