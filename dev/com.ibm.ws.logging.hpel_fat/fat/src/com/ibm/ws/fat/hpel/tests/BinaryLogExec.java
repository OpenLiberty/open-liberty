/*******************************************************************************
 * Copyright (c) 2002, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.fat.hpel.tests;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.simplicity.ApplicationServer;
import com.ibm.websphere.simplicity.Node;
import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.ws.fat.VerboseTestCase;
import com.ibm.ws.fat.hpel.setup.HpelSetup;
import com.ibm.ws.fat.ras.util.CommonTasks;
import com.ibm.ws.logging.hpel.FormatSet;

import componenttest.topology.impl.JavaInfo;

public class BinaryLogExec extends VerboseTestCase {

    private static final Logger thisLogger = Logger.getLogger(BinaryLogExec.class.getName());
    private final String outFileName = this.getName() + "_" + new Date().getTime() + ".lv_out.log";
    RemoteFile rOutLog = null;
    RemoteFile rProfRootDir = null;
    RemoteFile rProfBinFile = null;

    private ApplicationServer appServ = null;

    RemoteFile backup = null;

    public BinaryLogExec(String name) {
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

        // Liberty profile root is the install root.
        rProfRootDir = new RemoteFile(HpelSetup.getNodeUnderTest().getMachine(), HpelSetup.getServerUnderTest().getBackend().getInstallRoot());
//        rProfRootDir = new RemoteFile(HpelSetup.getNodeUnderTest().getMachine(), HpelSetup.getNodeUnderTest().getProfileDir());
        rProfBinFile = new RemoteFile(HpelSetup.getNodeUnderTest().getMachine(), rProfRootDir, "bin");
        // Setting the bootstrap with trace specification to get the trace logs.
        CommonTasks.addBootstrapProperty(appServ, "com.ibm.ws.logging.trace.specification", "*=fine=enabled");
        appServ.stop();
        appServ.start();

    }

    /**
     * Tests that binaryLog correctly report an invalid option when one is passed to it. e.g. binaryLog -invalidAction
     *
     * @throws Exception
     */
    public void testbinaryLogInvalidOption() throws Exception {
        this.logStep("executing binaryLog on " + HpelSetup.getNodeUnderTest().getProfileName());
        // generate a random option that is between 3 and 15 characters long starting with "bad".
        String alphabet = "abcdefghijklmnopqrstuvwxyz";
        StringBuffer invalidOption = new StringBuffer();
        invalidOption.append("-bad"); // need the option flag first.
        Random rnd = new Random();
        for (int i = 1; i <= rnd.nextInt(12); i++) {
            invalidOption.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
        }

        StringBuffer expectedResponse = new StringBuffer();
        // (WI 234118) We cannot check for message text that gets translated, because it causes the test to fail in non-English locales.
        // Updating this match to only use the non-translated words and argument in the message text.
        // expectedResponse.append("The specified action ");
        // expectedResponse.append(invalidOption);
        // expectedResponse.append(" is not valid.");
        // expectedResponse.append("\\s+"); //match as a regular expression
        // expectedResponse.append("For usage information use binaryLog help.");
        expectedResponse.append(".*" + invalidOption + ".*\\s+");
        expectedResponse.append(".*binaryLog help.*");
        ProgramOutput lvPrgmOut = exeBinaryLog(new String[] { invalidOption.toString() });
        this.logVerificationPoint("Verifying binaryLog std out/err and status return code.");
        logMsg("    === BinaryLog's stdout: === ");
        logMsg(lvPrgmOut.getStdout());
        logMsg(" ");
        if (lvPrgmOut.getStderr().length() > 0) {
            // LogViewer reported some errors.
            logMsg("    === BinaryLogs's std.err: ===");
            logMsg(lvPrgmOut.getStderr());
        }

        assertTrue("Failed assertion that logViewer exited with an error return code", (lvPrgmOut.getReturnCode() != 0));
        assertTrue("Failed assertion that logViewer reported invalid option.  Where //s is any number of spaces, expected="
                   + expectedResponse.toString().trim() + ".  result=" + lvPrgmOut.getStdout().trim(),
                   Pattern.matches(expectedResponse.toString(), lvPrgmOut.getStdout().trim()));

        this.logVerificationPassed();
        this.logStepCompleted();
    }

    /**
     * Tests that binaryLog copy action works as expected.
     * Steps:
     * 1. Create log entries and than use copy action to move the logs file to new location with minDate and maxDate specified.
     * 2. view the log entries again with minDate and maxDate specified
     * 3. Both isoDateFormat and the default dateFormat are tested for minDate and maxDate
     *
     * @throws Exception
     */
    public void testbinaryLogCopyExecutes() throws Exception {
        // need to have messages for the binaryLog to process.
        long entry1Time = System.currentTimeMillis();
        CommonTasks.createLogEntries(HpelSetup.getServerUnderTest(), BinaryLogExec.class.getName(), "Some Msg goes here", null, 50, CommonTasks.LOGS, -1);
        long entry2Time = System.currentTimeMillis();

        Date minDate = new Date(entry1Time);
        Date maxDate = new Date(entry2Time);

        DateFormat formatterISO = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

        // Java 9 has a different default date format than previous Java versions. Since ant may be running a different
        // Java version than the server, we need to explicitly set the default date format according to which JDK the server
        // is using. We can't just use the default for the ant JDK, it may not match that of the server.
        DateFormat formatterDefault = null;
        if (JavaInfo.forServer(HpelSetup.getServerUnderTest().getBackend()).majorVersion() >= 9) {
            formatterDefault = FormatSet.customizeDateFormat(new SimpleDateFormat("yy.MM.dd, HH:mm:ss:SSS"));
        } else {
            formatterDefault = FormatSet.customizeDateFormat(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM));
        }

        Node node = HpelSetup.getNodeUnderTest();
        rOutLog = new RemoteFile(node.getMachine(), node.getMachine().getTempDir(), outFileName);

        this.logStep("executing binaryLog on " + node.getProfileName());

        String arg1 = "copy";
        String arg2 = rOutLog.getAbsolutePath();
        String arg3 = "--minDate=" + formatterDefault.format(minDate);
        String arg4 = "--maxDate=" + formatterISO.format(maxDate);

        ProgramOutput lvPrgmOut;

        lvPrgmOut = exeBinaryLog(new String[] { arg1, CommonTasks.getBinaryLogDir(HpelSetup.getServerUnderTest()).getAbsolutePath(), arg2, arg3, arg4 });
        this.logVerificationPoint("Verifying binaryLog std out/err and status return code.");
        logMsg("    === BinaryLog's stdout: ===  return code is : " + lvPrgmOut.getReturnCode());
        logMsg(lvPrgmOut.getStdout());
        logMsg(" ");
        if (lvPrgmOut.getStderr().length() > 0) {
            // LogViewer reported some errors.
            logMsg("    === BinaryLog's std.err: ===");
            logMsg(lvPrgmOut.getStderr());
        }
        assertTrue("Failed assertion that binaryLog exited with successful return code", (lvPrgmOut.getReturnCode() == 0)); // Test that output returns Success Code (0)
        assertTrue("Failed assertion that binaryLog did produce an output file", rOutLog.exists()); // Test output repository exists

        ProgramOutput lvPrgmOut2 = exeBinaryLog(new String[] { "view", arg2, arg3, arg4 });
        assertTrue("Failed assertion that binaryLog exited with successful return code", (lvPrgmOut2.getReturnCode() == 0));
        assertTrue("Failed assertion that binaryLog did produce an output file", lvPrgmOut2.getStdout().contains("Some Msg goes here"));
        this.logVerificationPassed();
        this.logStepCompleted();

    }

    /**
     * Tests that binaryLog view action works as expected.
     * Steps:
     * 1. Create log entries and than use view action to view the contents of log in text format.
     * 2. View the log entries again using --isoDateFormat option
     *
     * @throws Exception
     */
    public void testbinaryLogViewExecutes() throws Exception {
        // need to have messages for the binaryLog to process.
        CommonTasks.createLogEntries(HpelSetup.getServerUnderTest(), BinaryLogExec.class.getName(), "Some Msg goes here", null, 25, CommonTasks.LOGS, -1);

        Node node = HpelSetup.getNodeUnderTest();
        rOutLog = new RemoteFile(node.getMachine(), node.getMachine().getTempDir(), outFileName);

        this.logStep("executing binaryLog on " + node.getProfileName());

        String arg1 = "view";
        String arg2 = "--isoDateFormat";

        ProgramOutput lvPrgmOut;

        lvPrgmOut = exeBinaryLog(new String[] { arg1, CommonTasks.getBinaryLogDir(HpelSetup.getServerUnderTest()).getAbsolutePath() + "/logdata" });

        this.logVerificationPoint("Verifying binaryLog std out/err and status return code.");
        logMsg("    === BinaryLog's stdout: === ");
        logMsg(lvPrgmOut.getStdout());
        logMsg(" ");
        if (lvPrgmOut.getStderr().length() > 0) {
            // LogViewer reported some errors.
            logMsg("    === BinaryLog's std.err: ===");
            logMsg(lvPrgmOut.getStderr());
        }

        Pattern p1 = null;
        if (JavaInfo.forServer(appServ.getBackend()).majorVersion() >= 9) {
            p1 = Pattern.compile("\\d{1,2}/\\d{1,2}/\\d{1,2}, \\d{1,2}:\\d{1,2}:\\d{1,2}:\\d\\d\\d");
        } else {
            p1 = Pattern.compile("\\d{1,2}/\\d{1,2}/\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}:\\d\\d\\d");
        }
        Matcher m1 = p1.matcher(lvPrgmOut.getStdout());
        assertTrue("Failed assertion that binaryLog exited with successful return code", (lvPrgmOut.getReturnCode() == 0));
        assertTrue("Failed assertion that binaryLog did produce an output file", lvPrgmOut.getStdout().contains("Some Msg goes here"));
        assertTrue("Failed assertion that binaryLog displayed default date format", m1.find());

        lvPrgmOut = exeBinaryLog(new String[] { arg1, CommonTasks.getBinaryLogDir(HpelSetup.getServerUnderTest()).getAbsolutePath() + "/logdata", arg2 });
        logMsg("    === BinaryLog's stdout: === ");
        logMsg(lvPrgmOut.getStdout());
        logMsg(" ");
        if (lvPrgmOut.getStderr().length() > 0) {
            // LogViewer reported some errors.
            logMsg("    === BinaryLog's std.err: ===");
            logMsg(lvPrgmOut.getStderr());
        }

        Pattern p2 = Pattern.compile("\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d\\.\\d\\d\\d");
        Matcher m2 = p2.matcher(lvPrgmOut.getStdout());
        assertTrue("Failed assertion that binaryLog exited with successful return code", (lvPrgmOut.getReturnCode() == 0));
        assertTrue("Failed assertion that binaryLog did produce an output file", lvPrgmOut.getStdout().contains("Some Msg goes here"));
        assertTrue("Failed assertion that binaryLog displayed default date format", m2.find());
        this.logVerificationPassed();
        this.logStepCompleted();

    }

    /**
     * Tests that binaryLog view action works as expected where is executed at usr/servers directory.
     *
     * @throws Exception
     */
    public void testbinaryLogViewExecutesAtServersDir() throws Exception {

        // need to have messages for the binaryLog to process.
        CommonTasks.createLogEntries(HpelSetup.getServerUnderTest(), BinaryLogExec.class.getName(), "Some Msg goes here", null, 25, CommonTasks.LOGS, -1);

        Node node = HpelSetup.getNodeUnderTest();
        rOutLog = new RemoteFile(node.getMachine(), node.getMachine().getTempDir(), outFileName);

        this.logStep("executing binaryLog for " + HpelSetup.SERVER_NAME);

        String arg1 = "view";

        ProgramOutput lvPrgmOut;

        RemoteFile usrDir = new RemoteFile(HpelSetup.getNodeUnderTest().getMachine(), rProfRootDir, "usr");
        RemoteFile serversDir = new RemoteFile(HpelSetup.getNodeUnderTest().getMachine(), usrDir, "servers");

        lvPrgmOut = exeBinaryLog(new String[] { arg1, HpelSetup.SERVER_NAME }, serversDir.getAbsolutePath());

        this.logVerificationPoint("Verifying binaryLog std out/err and status return code.");
        logMsg("    === BinaryLog's stdout: === ");
        logMsg(lvPrgmOut.getStdout());
        logMsg(" ");
        if (lvPrgmOut.getStderr().length() > 0) {
            // LogViewer reported some errors.
            logMsg("    === BinaryLog's std.err: ===");
            logMsg(lvPrgmOut.getStderr());
        }

        Pattern p1 = Pattern.compile("\\d{1,2}/\\d{1,2}/\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}:\\d\\d\\d");
        Matcher m1 = p1.matcher(lvPrgmOut.getStdout());
        assertTrue("Failed assertion that binaryLog exited with successful return code", (lvPrgmOut.getReturnCode() == 0));
        assertTrue("Failed assertion that binaryLog did produce an output file", lvPrgmOut.getStdout().contains("Some Msg goes here"));
        assertTrue("Failed assertion that binaryLog displayed default date format", m1.find());

        this.logVerificationPassed();
        this.logStepCompleted();
    }

    /**
     * Tests that binaryLog view action works as expected.
     * Steps:
     * 1.Create log entries and then use listInstances action to view instances of the binary logs.
     * 2.use listInstances again with --isoDateFormat option
     *
     * @throws Exception
     */
    public void testbinaryLogListInstancesExecutes() throws Exception {
        // need to have messages for the binaryLog to process.
        CommonTasks.createLogEntries(HpelSetup.getServerUnderTest(), BinaryLogExec.class.getName(), "Some Msg goes here", null, 25, CommonTasks.LOGS, -1);

        Node node = HpelSetup.getNodeUnderTest();
        rOutLog = new RemoteFile(node.getMachine(), node.getMachine().getTempDir(), outFileName);

        this.logStep("executing binaryLog on " + node.getProfileName());

        String arg1 = "listInstances";
        String arg2 = "--isoDateFormat";

        ProgramOutput lvPrgmOut;

        lvPrgmOut = exeBinaryLog(new String[] { arg1, CommonTasks.getBinaryLogDir(HpelSetup.getServerUnderTest()).getAbsolutePath() + "/logdata" });

        this.logVerificationPoint("Verifying binaryLog std out/err and status return code.");
        logMsg("    === BinaryLog's stdout: === ");
        logMsg(lvPrgmOut.getStdout());
        logMsg(" ");
        if (lvPrgmOut.getStderr().length() > 0) {
            // LogViewer reported some errors.
            logMsg("    === BinaryLog's std.err: ===");
            logMsg(lvPrgmOut.getStderr());
        }

        Pattern p1 = null;
        if (JavaInfo.forServer(appServ.getBackend()).majorVersion() >= 9) {
            p1 = Pattern.compile("\\d{1,2}/\\d{1,2}/\\d{1,2}, \\d{1,2}:\\d{1,2}:\\d{1,2}:\\d\\d\\d");
        } else {
            p1 = Pattern.compile("\\d{1,2}/\\d{1,2}/\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}:\\d\\d\\d");
        }
        Matcher m1 = p1.matcher(lvPrgmOut.getStdout());
        assertTrue("Failed assertion that binaryLog exited with successful return code", (lvPrgmOut.getReturnCode() == 0));
        // (WI 234118) We cannot perform this check because "Instance ID" is translated, which causes the test to fail in non-English locales. There is no ID or
        // non-translated portion of the message to even try to match.
        // assertTrue("Failed assertion that binaryLog did produce an output file", lvPrgmOut.getStdout().contains("Instance ID "));
        assertTrue("Failed assertion that binaryLog displayed default date format", m1.find());

        lvPrgmOut = exeBinaryLog(new String[] { arg1, CommonTasks.getBinaryLogDir(HpelSetup.getServerUnderTest()).getAbsolutePath() + "/logdata", arg2 });
        logMsg("    === BinaryLog's stdout: === ");
        logMsg(lvPrgmOut.getStdout());
        logMsg(" ");
        if (lvPrgmOut.getStderr().length() > 0) {
            // LogViewer reported some errors.
            logMsg("    === BinaryLog's std.err: ===");
            logMsg(lvPrgmOut.getStderr());
        }
        Pattern p2 = Pattern.compile("\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d\\.\\d\\d\\d");
        Matcher m2 = p2.matcher(lvPrgmOut.getStdout());
        assertTrue("Failed assertion that binaryLog exited with successful return code", (lvPrgmOut.getReturnCode() == 0));
        // (WI 234118) We cannot perform this check because "Instance ID" is translated, which causes the test to fail in non-English locales. There is no ID or
        // non-translated portion of the message to even try to match.
        // assertTrue("Failed assertion that binaryLog did produce an output file", lvPrgmOut.getStdout().contains("Instance ID "));
        assertTrue("Failed assertion that binaryLog displayed default date format", m2.find());
        this.logVerificationPassed();
        this.logStepCompleted();

    }

    /**
     * Testing the listInstances action of BinaryLog command. Also testing --includeInstance Filter option of View.
     *
     * @throws Exception
     */
    public void testHeaderProcessName() throws Exception {
        this.logStep("get instance list from " + HpelSetup.getNodeUnderTest().getProfileName());

        ProgramOutput lvPrgmOut = exeBinaryLog(new String[] { "listInstances", CommonTasks.getBinaryLogDir(HpelSetup.getServerUnderTest()).getAbsolutePath() });
        String out = lvPrgmOut.getStdout();

        // (WI 234118) We cannot rely on matching "Instance ID" as the header because that message is translated when running in non-English locales, causing this
        // test to fail. Instead, match the lines starting with a number assuming those are the instance IDs.
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
        this.logStepCompleted();

        this.logStep("Verify procName and procId for each retrieved instance");

        boolean isZOS = HpelSetup.getNodeUnderTest().getMachine().getOperatingSystem().equals(OperatingSystem.ZOS);
        boolean hasController = false;
        boolean hasServant = false;
        ApplicationServer server = HpelSetup.getServerUnderTest();
        for (String id : ids) {
            this.logVerificationPoint("Verifying instance " + id);

            // c:\sumam\Liberty\workspace3\build.image\wlp\bin>binaryLog view server1 --includeInstance=latest

            lvPrgmOut = exeBinaryLog(new String[] { "view", CommonTasks.getBinaryLogDir(HpelSetup.getServerUnderTest()).getAbsolutePath(), "--includeInstance=" + id });

            out = lvPrgmOut.getStdout();
            this.logMsg("id ouptut : " + out);
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
                    expProcName = server.getShortName();
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
                expProcName = server.getBackend().getServerName();
//                expProcName = server.getCellName() + "\\" + server.getNodeName() + "\\" + server.getName();
                // Don't verify process Id since it is not available in Liberty FAT
//                expProcId = server.getProcessIdString();
//                assertEquals("Process ID is incorrect", expProcId, procId);
            }
            assertEquals("Process Name is incorrect", expProcName, procName);

            this.logVerificationPassed();
        }

        this.logVerificationPoint("Verifying completeness of the test");
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
        this.logVerificationPassed();

        this.logStepCompleted();
    }

    /**
     * A method to drive the execution of the binaryLog tool on the remote machine/server that is being tested.
     *
     * @throws Exception
     */
    private ProgramOutput exeBinaryLog(String[] cmdLineOptions) throws Exception {
        return exeBinaryLog(cmdLineOptions, null);
    }

    private ProgramOutput exeBinaryLog(String[] cmdLineOptions, String workDir) throws Exception {
        // make platform agnostic to handle .sh and .bat
        String exeExt = "";
        final String BINARY_LOG = "binaryLog";

        //if non-iSeries
//              if (!HpelSetup.getNodeUnderTest().getMachine().getOperatingSystem().equals(OperatingSystem.ISERIES)){
        if (HpelSetup.getNodeUnderTest().getMachine().getOperatingSystem().equals(OperatingSystem.WINDOWS)) {
            exeExt = ".bat";
//                      } else {
//                              exeExt = ".sh";
        }

        StringBuilder cmd = new StringBuilder(100);
        cmd.append(rProfBinFile.getAbsolutePath()).append(HpelSetup.getNodeUnderTest().getMachine().getOperatingSystem().getFileSeparator());
        cmd.append(BINARY_LOG).append(exeExt).append(" ");

        for (String cmdOption : cmdLineOptions) {
            if (!cmdOption.isEmpty()) {
                cmd.append("\"" + cmdOption + "\" ");
            }
        }

        logMsg("executing: " + cmd.toString());
//        logMsg("executing: " + rProfBinFile.getAbsolutePath() + HpelSetup.getNodeUnderTest().getMachine().getOperatingSystem().getFileSeparator() + cmd.toString());
//        return HpelSetup.getNodeUnderTest().getMachine().execute(LOG_VIEWER + exeExt, cmdLineOptions, rProfBinFile.getAbsolutePath());
        return HpelSetup.getNodeUnderTest().getMachine().execute(cmd.toString(), workDir == null ? rProfBinFile.getAbsolutePath() : workDir);

//              }
//              //iSeries LogViewer needs to be executed in the shell qsh
//              else{
//                      return HpelSetup.getNodeUnderTest().getMachine().executeQSH(LOG_VIEWER, cmdLineOptions, rProfBinFile.getAbsolutePath(), null);
//              }
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

    /**
     * Determine if we should or should not execute this test. Returns true if the test should NOT be ran.
     **/
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

    /**
     * A simple method used to log messages from this test case to the test case's logs
     */
    public void logMsg(String msg) {
        if (thisLogger.isLoggable(Level.INFO)) {
            thisLogger.info(msg);
        }
    }

}