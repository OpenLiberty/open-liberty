//%Z% %I% %W% %G% %U% [%H% %T%]
/**
 *
 * DESCRIPTION:
 *
 * Change History:
 *
 * Reason       Version	    Date        User id     Description
 * ----------------------------------------------------------------------------
 * 91932        8.5.5     13/06/2013    sumam     Test case for defect 91932.
 *
 */

package com.ibm.ws.logging.hpel.fat;

import static org.junit.Assert.assertEquals;

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
 * Test case for defect 91932: HPEL gives false positive warning on disk space after changing dataDirectory
 * Test Scenario: startServer the server by logDirectory as logs → modify it to logX and delete the old directory (logs) , no warning message should be generated.
 *
 */
@RunWith(FATRunner.class)
public class HPELDataDirFalsePositiveWarningTest {

    private final static String loggerName = HPELDataDirFalsePositiveWarningTest.class.getName();
    private final static Logger logger = Logger.getLogger(loggerName);
    private final static String MESSAGE_LOG = "logs/HPELDataDirFalsePositiveWarningTest.log";

    @Server("HpelServer")
    public static LibertyServer server;

    static RemoteFile backup = null;

    @BeforeClass
    public static void setUp() throws Exception {
        // Confirm HPEL is enabled
        ShrinkHelper.defaultDropinApp(server, "LogFat", "com.ibm.ws.logging.hpel");
        ShrinkHelper.defaultDropinApp(server, "HpelFat", "com.ibm.ws.logging.hpel.servlet");
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
        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles, "server-HPELDeleteLogDirectory_1.xml"));
        if (!server.isStarted()) {
            server.startServer();
        }

    }

    /**
     * Test that HPEL's logDirectory change does not give any warning message.
     * startServer the server with logDirectory as "LogX1", during the runtime change the logDirectory to "logX2" and delete the old directory i.e. logX1,
     * No warning message should be created for deleting the old unused directory
     *
     **/
    @Test
    public void testLogDirectoryChange() throws Exception {

        CommonTasks.writeLogMsg(Level.INFO, "Configuring server for test case.");
        server.updateServerConfiguration(new File(server.pathToAutoFVTTestFiles, "server-HPELDeleteLogDirectory_2.xml"));

        // Deleting the old directory and checking for the warning message.

        CommonTasks.writeLogMsg(Level.INFO, " Deleting the logs directory to check the warning message");
        server.deleteFileFromLibertyServerRoot("logX1");

        CommonTasks.writeLogMsg(Level.INFO, "Verifying no warning message generated for old log directory deleted .");
        logger.info("The console.log should not have a error message HPEL0161W ");
        checkWarningMessageForDirectoryDelete();

    }

    //Check we dont have any warning message for old directory deleted.
    protected void checkWarningMessageForDirectoryDelete() throws Exception {
        List<String> lines = server.findStringsInFileInLibertyServerRoot("HPEL0161W", MESSAGE_LOG);
        assertEquals("Message HPEL0161W appeared in the console file", 0, lines.size());
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