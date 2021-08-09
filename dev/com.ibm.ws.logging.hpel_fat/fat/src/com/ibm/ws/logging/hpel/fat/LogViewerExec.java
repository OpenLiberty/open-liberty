/*******************************************************************************
 * Copyright (c) 2010, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
//%Z% %I% %W% %G% %U% [%H% %T%]
/*
 * Change History:
 *
 * Reason         Version    Date       User id     Description
 * ----------------------------------------------------------------------------
 * F000896.23216	8.0		08/25/2010	shighbar	Update test case for z/OS support.
 * 653858           8.0     09/08/2010  spaungam    Invalid option test doesn't work in CT
 * 677722           8.0     11/05/2010  mcasile     msgid key removed from logviewer nls
 * 677210           8.0     11/05/2010  spaungam    logviewer script is missing from iSeries
 * 690576           8.0     02/16/2011  spaungam    use qsh shell on iSeries with new simplicity
 * 695788			8.0		03/09/2011	shighbar	Always set repositoryDir param to avoid LV hang on user input.
 * 712273           8.0     09/16/2011  belyi       Add test for process name in the header.
 */
package com.ibm.ws.logging.hpel.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.fat.util.Props;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * This task is to read from an active servers HPEL repository in basic format and to verify there are no errors and to
 * check for key output.
 *
 *
 */
@RunWith(FATRunner.class)
public class LogViewerExec {

    private static final Logger thisLogger = Logger.getLogger(LogViewerExec.class.getName());
    private final String outFileName = LogViewerExec.class.getName() + ".lv_out.log";
    static RemoteFile rOutLog = null;
    static RemoteFile rProfRootDir = null;
    static RemoteFile rProfBinFile = null;
    @Server("HpelServer")
    public static LibertyServer server;

    /**
     * Configures instance resources to initialize this TestCase.
     *
     * @throws Exception
     *                       if a problem happens while configuring the test fixture.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "LogFat", "com.ibm.ws.logging.hpel");
        ShrinkHelper.defaultDropinApp(server, "HpelFat", "com.ibm.ws.logging.hpel.servlet");
        // Confirm HPEL is enabled
        if (!CommonTasks.isHpelEnabled(server)) {
            // HPEL is not enabled.
            CommonTasks.writeLogMsg(Level.INFO, "HPEL is not enabled on " + server.getServerName() + ", attempting to enable.");
            CommonTasks.setHpelEnabled(server, true);
            // server.restartServer(); Bug# 17199
            server.stopServer();
            server.startServer();

        }

        assertTrue("Failed assertion that HPEL is enabled", CommonTasks.isHpelEnabled(server));

        // Liberty profile root is the install root.
        rProfRootDir = new RemoteFile(server.getMachine(), server.getInstallRoot());
//        rProfRootDir = new RemoteFile(HpelSetup.getNodeUnderTest().getMachine(), HpelSetup.getNodeUnderTest().getProfileDir());
        rProfBinFile = new RemoteFile(server.getMachine(), rProfRootDir, "bin");

    }

    /**
     * A method to drive the execution of the logViewer tool on the remote machine/server that is being tested.
     *
     * @throws Exception
     */
    private ProgramOutput exeLogViewer(String[] cmdLineOptions) throws Exception {
        // make platform agnostic to handle .sh and .bat
        String exeExt = "";
        final String LOG_VIEWER = "logViewer";

        //if non-iSeries
//		if (!HpelSetup.getNodeUnderTest().getMachine().getOperatingSystem().equals(OperatingSystem.ISERIES)){
        if (server.getMachine().getOperatingSystem().equals(OperatingSystem.WINDOWS)) {
            exeExt = ".bat";
//			} else {
//				exeExt = ".sh";
        }

        StringBuilder cmd = new StringBuilder(100);
        cmd.append(rProfBinFile.getAbsolutePath()).append(server.getMachine().getOperatingSystem().getFileSeparator());
        cmd.append(LOG_VIEWER).append(exeExt).append(" ");

        for (String cmdOption : cmdLineOptions) {
            if (!cmdOption.isEmpty()) {
                cmd.append("\"" + cmdOption + "\" ");
            }
        }
        logMsg("executing: " + cmd.toString());
//        logMsg("executing: " + rProfBinFile.getAbsolutePath() + HpelSetup.getNodeUnderTest().getMachine().getOperatingSystem().getFileSeparator() + cmd.toString());
//        return HpelSetup.getNodeUnderTest().getMachine().execute(LOG_VIEWER + exeExt, cmdLineOptions, rProfBinFile.getAbsolutePath());
        return server.getMachine().execute(cmd.toString(), rProfBinFile.getAbsolutePath());
//		}
//		//iSeries LogViewer needs to be executed in the shell qsh
//		else{
//			return HpelSetup.getNodeUnderTest().getMachine().executeQSH(LOG_VIEWER, cmdLineOptions, rProfBinFile.getAbsolutePath(), null);
//		}
    }

    /**
     * Tests that LogViewer can be executed on the target machine and that no error's are thrown. If the repositories
     * are in the default location - logViewer will execute against default values; i.e. against the log and trace data
     * located in the profile's log directory etc. If the configuration shows that the logdata is being written to
     * another location; that location will be passed to logViewer. In the case where the log data is being written to
     * another location, only the log data will be ran against.
     *
     * @throws Exception
     */
    @Test
    public void testLogViewerExecutes() throws Exception {
        // need to have messages for the logViewer to process.
        CommonTasks.createLogEntries(server, LogViewerExec.class.getName(), "Some Msg goes here", null, 25, CommonTasks.LOGS, -1);
        rOutLog = new RemoteFile(server.getMachine(), server.getMachine().getTempDir(), outFileName);

        CommonTasks.writeLogMsg(Level.INFO, "executing logViewer on " + server.getServerName());

        String arg1 = "-outLog";
        String arg2 = rOutLog.getAbsolutePath();

        ProgramOutput lvPrgmOut;

        lvPrgmOut = exeLogViewer(new String[] { arg1, arg2, "-repositoryDir", CommonTasks.getBinaryLogDir(server).getAbsolutePath() });

        CommonTasks.writeLogMsg(Level.INFO, "Verifying logViewer std out/err and status return code.");
        logMsg("    === LogViewer's stdout: === ");
        logMsg(lvPrgmOut.getStdout());
        logMsg(" ");
        if (lvPrgmOut.getStderr().length() > 0) {
            // LogViewer reported some errors.
            logMsg("    === LogViewer std.err: ===");
            logMsg(lvPrgmOut.getStderr());
        }
        assertTrue("Failed assertion that logViewer exited with successful return code", (lvPrgmOut.getReturnCode() == 0));

        assertTrue("Failed assertion that logViewer did produce an output file", rOutLog.exists());

        // pull the resulting text based log over to test machine.
        Props props = Props.getInstance();
        CommonTasks.writeLogMsg(Level.INFO, "Pulling logViewer output file ( " + outFileName + " ) to results dir");
        RemoteFile lResultsDir = new RemoteFile(Machine.getLocalMachine(), props.getFileProperty(Props.DIR_LOG).getCanonicalPath());
        RemoteFile lOutLog = new RemoteFile(Machine.getLocalMachine(), lResultsDir, outFileName);

        lOutLog.copyFromSource(rOutLog, true); // Even though file is text we are getting transfer exceptions that
        // appear to be because logViewer output is UTF8 or contains other
        // special characters. Transferring in binary seems to correct this.

    }

    /**
     * Tests that logViewer correctly report an invalid option when one is passed to it.
     *
     * @throws Exception
     */
    @Test
    public void testLogViewerInvalidOption() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "executing logViewer on " + server.getServerName());
        // generate a random option that is between 3 and 15 characters long startServering with "bad".
        String alphabet = "abcdefghijklmnopqrstuvwxyz";
        StringBuffer invalidOption = new StringBuffer();
        invalidOption.append("-bad"); // need the option flag first.
        Random rnd = new Random();
        for (int i = 1; i <= rnd.nextInt(12); i++) {
            invalidOption.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
        }

        StringBuffer expectedResponse = new StringBuffer();
        expectedResponse.append("Unknown Argument: ");
        expectedResponse.append(invalidOption);
        expectedResponse.append("\\s+"); //match as a regular expression
        expectedResponse.append("Use option -help for usage information.");

        ProgramOutput lvPrgmOut = exeLogViewer(new String[] { invalidOption.toString() });
        CommonTasks.writeLogMsg(Level.INFO, "Verifying logViewer std out/err and status return code.");
        logMsg("    === LogViewer's stdout: === ");
        logMsg(lvPrgmOut.getStdout());
        logMsg(" ");
        if (lvPrgmOut.getStderr().length() > 0) {
            // LogViewer reported some errors.
            logMsg("    === LogViewer std.err: ===");
            logMsg(lvPrgmOut.getStderr());
        }

        assertTrue("Failed assertion that logViewer exited with an error return code", (lvPrgmOut.getReturnCode() != 0));
        assertTrue("Failed assertion that logViewer reported invalid option.  Where //s is any number of spaces, expected="
                   + expectedResponse.toString().trim() + ".  result=" + lvPrgmOut.getStdout().trim(),
                   Pattern.matches(expectedResponse.toString(), lvPrgmOut.getStdout().trim()));

    }

    @Test
    public void testHeaderProcessName() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "get instance list from " + server.getServerName());

        ProgramOutput lvPrgmOut = exeLogViewer(new String[] { "-listInstances", "-repositoryDir", CommonTasks.getBinaryLogDir(server).getAbsolutePath() });
        String out = lvPrgmOut.getStdout();

        // (WI 234118) We cannot rely on matching "Instance ID" as the header because that message is translated when running in non-English locales, causing this
        // test to fail. Instead, match the lines startServering with a number assuming those are the instance IDs.
        // Collect IDs belonging to the current instance run.
        int indx;
        String controllerID = null;
        ArrayList<String> ids = new ArrayList<String>();
        for (String outLine : out.split("\\r?\\n")) {
            Pattern p = Pattern.compile("^\\s*(\\d+(?:/\\S+)?)\\s");
            Matcher m = p.matcher(outLine);
            if (m.find()) {
                String newID = m.group(1);
                logMsg("Processing instance id " + newID);
                indx = newID.indexOf('/');
                String newControllerID = indx < 0 ? newID : newID.substring(0, indx);
                logMsg("It belongs to controller " + newControllerID);
                if (controllerID == null || !controllerID.equals(newControllerID)) {
                    // It's a new controller's instance, reset the list of IDs.
                    logMsg("It's a new controller, reset list of instances");
                    ids.clear();
                    controllerID = newControllerID;
                }
                ids.add(newID);
            }
        }

        assertFalse("No instance IDs found", ids.isEmpty());

        CommonTasks.writeLogMsg(Level.INFO, "Verify procName and procId for each retrieved instance");

        boolean isZOS = server.getMachine().getOperatingSystem().equals(OperatingSystem.ZOS);
        boolean hasController = false;
        boolean hasServant = false;
        for (String id : ids) {
            CommonTasks.writeLogMsg(Level.INFO, "Verifying instance " + id);

            lvPrgmOut = exeLogViewer(new String[] { "-instance", id, "-repositoryDir", CommonTasks.getBinaryLogDir(server).getAbsolutePath() });
            out = lvPrgmOut.getStdout();
            // Should be okay matching the English text here because this is not a translated message.
            int indx1 = out.indexOf(" running with process name ");
            int indx2 = out.indexOf(" and process id ", indx1);
            assertTrue("Instance with ID " + id + " does not have proper header", indx1 > 0 && indx2 > indx1);
            String procName = out.substring(indx1 + 27, indx2);
            String procId = out.substring(indx2 + 16).split("\\s*\\n\\s*", 2)[0];
            String expProcName;
            String expProcId;
            if (isZOS) {
                indx = id.indexOf("/");
                if (indx < 0) {
                    // This is controller, take name values from Simplicity, can't verify jobID this way though
                    expProcName = server.getServerName();
                    hasController = true;
                } else {
                    // This is servant, take values from instance ID.
                    indx1 = id.indexOf("-", indx);
                    indx2 = id.indexOf("_", indx1);
                    assertTrue("Instance ID " + id + " is incorrectly formatted", indx1 > indx && indx2 > indx1);
                    expProcName = id.substring(indx1 + 1, indx2);
                    expProcId = id.substring(indx2 + 1);
                    hasServant = true;
                    assertEquals("Process ID is incorrect", expProcId, procId);
                }
            } else {
                // On Liberty procName is just the server name.
                expProcName = server.getServerName();
//                expProcName = server.getCellName() + "\\" + server.getNodeName() + "\\" + server.getName();
                // Don't verify process Id since it is not available in Liberty FAT
//                expProcId = server.getProcessIdString();
//                assertEquals("Process ID is incorrect", expProcId, procId);
            }
            assertEquals("Process Name is incorrect", expProcName, procName);

        }

        CommonTasks.writeLogMsg(Level.INFO, "Verifying completeness of the test");
        if (isZOS) {
            if (!hasController && !hasServant) {
                fail("Test on zOS is missing both controller and servant instances");
            }
            if (!hasController) {
                fail("Test on zOS is missing controller instance");
            }
            if (!hasServant) {
                fail("Test on zOS is missing at least one servant instance");
            }
        } else if (ids.size() == 0) {
            fail("Test on distributed has no instances to verify");
        }

    }

    /**
     * Clean up instance resources to initialize this TestCase.
     *
     * @throws Exception
     *                       if a problem happens while configuring the test fixture.
     */
    @AfterClass
    public static void tearDown() throws Exception {
        // delete the output log on the remote machine.
        if ((rOutLog != null) && (rOutLog.exists()))
            rOutLog.delete();

        // call the super
    }

    /**
     * A simple method used to log messages from this test case to the test case's logs
     */
    public void logMsg(String msg) {
        if (thisLogger.isLoggable(Level.INFO)) {
            thisLogger.info(msg);
        }
    }

}
