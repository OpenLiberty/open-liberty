/*******************************************************************************
 * Copyright (c) 2002, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
//%Z% %I% %W% %G% %U% [%H% %T%]
/**
 * DESCRIPTION:
 *
 * Change History:
 *
 * Reason               Version         Date         User id    Description
 * ----------------------------------------------------------------------------
 * 95263                8.5.5           06/13/2013   sumam      Fixed the test case for binaryLog utility command
 * rtc240434            17.0            05/01/2017   gkwan      Added test case for binaryLog view at servers dir
 * GH 12035             18.0            08/06/2018   pgunapal   Added test case for --excludeMessage
 * 270471               20.0            01/20/2020   halimlee   Added condition cases for different Locale settings
 */

package com.ibm.ws.logging.hpel.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.logging.hpel.FormatSet;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class BinaryLogExec {

    private static final Logger thisLogger = Logger.getLogger(BinaryLogExec.class.getName());
    private final String outFileName = BinaryLogExec.class.getName() + "_" + new Date().getTime() + ".lv_out.log";
    RemoteFile rOutLog = null;
    static RemoteFile rProfRootDir = null;
    static RemoteFile rProfBinFile = null;

    @Server("HpelServer")
    public static LibertyServer server;

    RemoteFile backup = null;

    @BeforeClass
    public static void setUp() throws Exception {
        // Call super.SetUp() cause we still want it's setup as well
        // Confirm HPEL is enabled
        assumeTrue(!skipTest());
        ShrinkHelper.defaultDropinApp(server, "LogFat", "com.ibm.ws.logging.hpel");
        ShrinkHelper.defaultDropinApp(server, "HpelFat", "com.ibm.ws.logging.hpel.servlet");

        // Liberty profile root is the install root.
        rProfRootDir = server.getMachine().getFile(server.getInstallRoot());
//        rProfRootDir = HpelSetup.getNodeUnderTest().getMachine().getFile(HpelSetup.getNodeUnderTest().getProfileDir());
        rProfBinFile = server.getMachine().getFile(rProfRootDir, "bin");
        // Setting the bootstrap with trace specification to get the trace logs.
        CommonTasks.addBootstrapProperty(server, "com.ibm.ws.logging.trace.specification", "*=fine=enabled");
        server.stopServer();
        server.startServer();

    }

    /**
     * Tests that binaryLog correctly report an invalid option when one is passed to it. e.g. binaryLog -invalidAction
     *
     * @throws Exception
     */
    @Test
    public void testbinaryLogInvalidOption() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "executing binaryLog on " + server.getServerName());
        // generate a random option that is between 3 and 15 characters long startServering with "bad".
        String alphabet = "abcdefghijklmnopqrstuvwxyz";
        StringBuffer invalidOption = new StringBuffer();
        invalidOption.append("-bad"); // need the option flag first.
        Random rnd = new Random();
        for (int i = 1; i <= rnd.nextInt(12); i++) {
            invalidOption.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
        }

        // StringBuffer expectedResponse = new StringBuffer();
        // (WI 234118) We cannot check for message text that gets translated, because it causes the test to fail in non-English locales.
        // Updating this match to only use the non-translated words and argument in the message text.
        // expectedResponse.append("The specified action ");
        // expectedResponse.append(invalidOption);
        // expectedResponse.append(" is not valid.");
        // expectedResponse.append("\\s+"); //match as a regular expression
        // expectedResponse.append("For usage information use binaryLog help.");
        // expectedResponse.append(".*" + invalidOption + ".*\\s+");
        // expectedResponse.append(".*binaryLog help.*");
        ProgramOutput lvPrgmOut = exeBinaryLog(new String[] { invalidOption.toString() });
        CommonTasks.writeLogMsg(Level.INFO, "Verifying binaryLog std out/err and status return code.");
        logMsg("    === BinaryLog's stdout: === ");
        logMsg(lvPrgmOut.getStdout());
        logMsg(" ");
        if (lvPrgmOut.getStderr().length() > 0) {
            // LogViewer reported some errors.
            logMsg("    === BinaryLogs's std.err: ===");
            logMsg(lvPrgmOut.getStderr());
        }

        assertTrue("Failed assertion that logViewer exited with an error return code", (lvPrgmOut.getReturnCode() != 0));
        assertTrue("logViewer did not report invalid option. Expected="
                   + invalidOption + ".  result=" + lvPrgmOut.getStdout().trim(),
                   lvPrgmOut.getStdout().contains(invalidOption));
        assertTrue("logViewer message did not contain binaryLog help. Result=" + lvPrgmOut.getStdout().trim(),
                   lvPrgmOut.getStdout().contains("binaryLog help"));

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
    @Test
    public void testbinaryLogCopyExecutes() throws Exception {
        // need to have messages for the binaryLog to process.
        long entry1Time = System.currentTimeMillis();
        CommonTasks.createLogEntries(server, BinaryLogExec.class.getName(), "Some Msg goes here", null, 50, CommonTasks.LOGS, -1);
        long entry2Time = System.currentTimeMillis();

        Date minDate = new Date(entry1Time);
        Date maxDate = new Date(entry2Time);

        DateFormat formatterISO = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

        DateFormat formatterDefault = null;

        //Get the environment's locale setting
        Locale localeSetting = Locale.getDefault(Locale.Category.FORMAT);
        String localeSettingStr = localeSetting.toString();
        logMsg("    === Current environment's locale setting : " + localeSettingStr);

        // In Java 9 the default date format added a comma between the date and time
        // [9/25/18 7:49:24:078 UTC]  ===>  [9/25/18, 7:49:24:078 UTC]
        // Different Locale settings have different default date format for Java version >= 9
        if (JavaInfo.forServer(server).majorVersion() >= 9) {
            if (localeSettingStr.equalsIgnoreCase("en_CA")) {
                formatterDefault = FormatSet.customizeDateFormat(new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss:SSS"));
            } else if (localeSettingStr.equalsIgnoreCase("en_GB")) {
                formatterDefault = FormatSet.customizeDateFormat(new SimpleDateFormat("dd/MM/yyyy, HH:mm:ss:SSS"));
            } else {
                formatterDefault = FormatSet.customizeDateFormat(new SimpleDateFormat("MM/dd/yy, HH:mm:ss:SSS"));
            }
        } else {
            formatterDefault = FormatSet.customizeDateFormat(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM));
        }

        rOutLog = server.getMachine().getFile(server.getMachine().getTempDir(), outFileName);

        CommonTasks.writeLogMsg(Level.INFO, "executing binaryLog on " + server.getServerName());

        String arg1 = "copy";
        String arg2 = rOutLog.getAbsolutePath();
        String arg3 = "--minDate=" + formatterDefault.format(minDate);
        String arg4 = "--maxDate=" + formatterISO.format(maxDate);

        ProgramOutput lvPrgmOut;

        lvPrgmOut = exeBinaryLog(new String[] { arg1, CommonTasks.getBinaryLogDir(server).getAbsolutePath(), arg2, arg3, arg4 });
        CommonTasks.writeLogMsg(Level.INFO, "Verifying binaryLog std out/err and status return code.");
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

    }

    /**
     * Tests that binaryLog view action works as expected.
     * Steps:
     * 1. Create log entries and than use view action to view the contents of log in text format.
     * 2. View the log entries again using --isoDateFormat option
     * 3. View the log entries again using --excludeMessage option, and hide one of the create log entries.
     *
     * @throws Exception
     */
    @Test
    public void testbinaryLogViewExecutes() throws Exception {
        // need to have messages for the binaryLog to process.
        CommonTasks.createLogEntries(server, BinaryLogExec.class.getName(), "Some Msg goes here", null, 25, CommonTasks.LOGS, -1);

        // create message to hide
        CommonTasks.createLogEntries(server, BinaryLogExec.class.getName(), "Some Msg goes here to be excluded.", null, 25, CommonTasks.LOGS, -1);

        rOutLog = server.getMachine().getFile(server.getMachine().getTempDir(), outFileName);

        CommonTasks.writeLogMsg(Level.INFO, "executing binaryLog on " + server.getServerName());

        String arg1 = "view";
        String arg2 = "--isoDateFormat";
        String arg3 = "--excludeMessage=*Msg*excluded*";

        ProgramOutput lvPrgmOut;

        logMsg("Executing BinaryLog to view log entries with no options");
        lvPrgmOut = exeBinaryLog(new String[] { arg1, CommonTasks.getBinaryLogDir(server).getAbsolutePath() + "/logdata" });

        CommonTasks.writeLogMsg(Level.INFO, "Verifying binaryLog std out/err and status return code.");
        logMsg("    === BinaryLog's stdout: === ");
        logMsg(lvPrgmOut.getStdout());
        logMsg(" ");
        if (lvPrgmOut.getStderr().length() > 0) {
            // LogViewer reported some errors.
            logMsg("    === BinaryLog's std.err: ===");
            logMsg(lvPrgmOut.getStderr());
        }

        Pattern p1 = null;
        //Get the environment's locale setting
        Locale localeSetting = Locale.getDefault(Locale.Category.FORMAT);
        String localeSettingStr = localeSetting.toString();
        logMsg("    === Current environment's locale setting : " + localeSettingStr);

        // In Java 9 the default date format added a comma between the date and time
        // [9/25/18 7:49:24:078 UTC]  ===>  [9/25/18, 7:49:24:078 UTC]
        // Different Locale settings have different default date format for Java version >= 9
        if (JavaInfo.forServer(server).majorVersion() >= 9) {
            if (localeSettingStr.equalsIgnoreCase("en_CA")) {
                p1 = Pattern.compile("\\d{1,4}-\\d{1,2}-\\d{1,2}, \\d{1,2}:\\d{1,2}:\\d{1,2}:\\d\\d\\d");
            } else if (localeSettingStr.equalsIgnoreCase("en_GB")) {
                p1 = Pattern.compile("\\d{1,2}/\\d{1,2}/\\d{1,4}, \\d{1,2}:\\d{1,2}:\\d{1,2}:\\d\\d\\d");
            } else {
                p1 = Pattern.compile("\\d{1,2}/\\d{1,2}/\\d{1,2}, \\d{1,2}:\\d{1,2}:\\d{1,2}:\\d\\d\\d");
            }
        } else {
            p1 = Pattern.compile("\\d{1,2}/\\d{1,2}/\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}:\\d\\d\\d");
        }
        Matcher m1 = p1.matcher(lvPrgmOut.getStdout());

        assertTrue("Failed assertion that binaryLog exited with successful return code", (lvPrgmOut.getReturnCode() == 0));
        assertTrue("Failed assertion that binaryLog did produce an output file", lvPrgmOut.getStdout().contains("Some Msg goes here"));
        assertTrue("Failed assertion that binaryLog displayed default date format", m1.find());

        logMsg("Executing BinaryLog to view log entries with --isoDateFormat option");
        lvPrgmOut = exeBinaryLog(new String[] { arg1, CommonTasks.getBinaryLogDir(server).getAbsolutePath() + "/logdata", arg2 });
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

        logMsg("Executing BinaryLog to view log entries with --excludeMessage option");
        lvPrgmOut = exeBinaryLog(new String[] { arg1, CommonTasks.getBinaryLogDir(server).getAbsolutePath() + "/logdata", arg3 });
        logMsg("    === BinaryLog's stdout: === ");
        logMsg(lvPrgmOut.getStdout());
        logMsg(" ");
        if (lvPrgmOut.getStderr().length() > 0) {
            // LogViewer reported some errors.
            logMsg("    === BinaryLog's std.err: ===");
            logMsg(lvPrgmOut.getStderr());
        }

        Matcher m3 = p1.matcher(lvPrgmOut.getStdout());
        assertTrue("Failed assertion that binaryLog exited with successful return code", (lvPrgmOut.getReturnCode() == 0));
        assertTrue("Failed assertion that binaryLog did produce an output file", lvPrgmOut.getStdout().contains("Some Msg goes here"));
        assertFalse("Failed assertion that binaryLog contains the excluded the message", lvPrgmOut.getStdout().contains("Some Msg goes here to be excluded."));
        assertTrue("Failed assertion that binaryLog displayed default date format", m3.find());

    }

    /**
     * Tests that binaryLog view action works as expected where is executed at usr/servers directory.
     *
     * @throws Exception
     */
    @Test
    public void testbinaryLogViewExecutesAtServersDir() throws Exception {

        // need to have messages for the binaryLog to process.
        CommonTasks.createLogEntries(server, BinaryLogExec.class.getName(), "Some Msg goes here", null, 25, CommonTasks.LOGS, -1);

        rOutLog = server.getMachine().getFile(server.getMachine().getTempDir(), outFileName);

        CommonTasks.writeLogMsg(Level.INFO, "executing binaryLog for " + server.getServerName());

        String arg1 = "view";

        ProgramOutput lvPrgmOut;

        RemoteFile usrDir = server.getMachine().getFile(rProfRootDir, "usr");
        RemoteFile serversDir = server.getMachine().getFile(usrDir, "servers");

        lvPrgmOut = exeBinaryLog(new String[] { arg1, server.getServerName() }, serversDir.getAbsolutePath());

        CommonTasks.writeLogMsg(Level.INFO, "Verifying binaryLog std out/err and status return code.");
        logMsg("    === BinaryLog's stdout: === ");
        logMsg(lvPrgmOut.getStdout());
        logMsg(" ");
        if (lvPrgmOut.getStderr().length() > 0) {
            // LogViewer reported some errors.
            logMsg("    === BinaryLog's std.err: ===");
            logMsg(lvPrgmOut.getStderr());
        }

        Pattern p1 = null;

        //Get the environment's locale setting
        Locale localeSetting = Locale.getDefault(Locale.Category.FORMAT);
        String localeSettingStr = localeSetting.toString();
        logMsg("    === Current environment's locale setting : " + localeSettingStr);

        // In Java 9 the default date format added a comma between the date and time
        // [9/25/18 7:49:24:078 UTC]  ===>  [9/25/18, 7:49:24:078 UTC]
        // Different Locale settings have different default date format for Java version >= 9
        if (JavaInfo.forServer(server).majorVersion() >= 9) {
            if (localeSettingStr.equalsIgnoreCase("en_CA")) {
                p1 = Pattern.compile("\\d{1,4}-\\d{1,2}-\\d{1,2}, \\d{1,2}:\\d{1,2}:\\d{1,2}:\\d\\d\\d");
            } else if (localeSettingStr.equalsIgnoreCase("en_GB")) {
                p1 = Pattern.compile("\\d{1,2}/\\d{1,2}/\\d{1,4}, \\d{1,2}:\\d{1,2}:\\d{1,2}:\\d\\d\\d");
            } else {
                p1 = Pattern.compile("\\d{1,2}/\\d{1,2}/\\d{1,2}, \\d{1,2}:\\d{1,2}:\\d{1,2}:\\d\\d\\d");
            }
        } else {
            p1 = Pattern.compile("\\d{1,2}/\\d{1,2}/\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}:\\d\\d\\d");
        }

        Matcher m1 = p1.matcher(lvPrgmOut.getStdout());
        assertTrue("Failed assertion that binaryLog exited with successful return code", (lvPrgmOut.getReturnCode() == 0));
        assertTrue("Failed assertion that binaryLog did produce an output file", lvPrgmOut.getStdout().contains("Some Msg goes here"));
        assertTrue("Failed assertion that binaryLog displayed default date format", m1.find());

    }

    /**
     * Tests that binaryLog view action works as expected.
     * Steps:
     * 1.Create log entries and then use listInstances action to view instances of the binary logs.
     * 2.use listInstances again with --isoDateFormat option
     *
     * @throws Exception
     */
    @Test
    public void testbinaryLogListInstancesExecutes() throws Exception {
        // need to have messages for the binaryLog to process.
        CommonTasks.createLogEntries(server, BinaryLogExec.class.getName(), "Some Msg goes here", null, 25, CommonTasks.LOGS, -1);

        rOutLog = server.getMachine().getFile(server.getMachine().getTempDir(), outFileName);

        CommonTasks.writeLogMsg(Level.INFO, "executing binaryLog on " + server.getServerName());

        String arg1 = "listInstances";
        String arg2 = "--isoDateFormat";

        ProgramOutput lvPrgmOut;

        lvPrgmOut = exeBinaryLog(new String[] { arg1, CommonTasks.getBinaryLogDir(server).getAbsolutePath() + "/logdata" });

        CommonTasks.writeLogMsg(Level.INFO, "Verifying binaryLog std out/err and status return code.");
        logMsg("    === BinaryLog's stdout: === ");
        logMsg(lvPrgmOut.getStdout());
        logMsg(" ");
        if (lvPrgmOut.getStderr().length() > 0) {
            // LogViewer reported some errors.
            logMsg("    === BinaryLog's std.err: ===");
            logMsg(lvPrgmOut.getStderr());
        }

        Pattern p1 = null;

        //Get the environment's locale setting
        Locale localeSetting = Locale.getDefault(Locale.Category.FORMAT);
        String localeSettingStr = localeSetting.toString();
        logMsg("    === Current environment's locale setting : " + localeSettingStr);

        // In Java 9 the default date format added a comma between the date and time
        // [9/25/18 7:49:24:078 UTC]  ===>  [9/25/18, 7:49:24:078 UTC]
        // Different Locale settings have different default date format for Java version >= 9
        if (JavaInfo.forServer(server).majorVersion() >= 9) {
            if (localeSettingStr.equalsIgnoreCase("en_CA")) {
                p1 = Pattern.compile("\\d{1,4}-\\d{1,2}-\\d{1,2}, \\d{1,2}:\\d{1,2}:\\d{1,2}:\\d\\d\\d");
            } else if (localeSettingStr.equalsIgnoreCase("en_GB")) {
                p1 = Pattern.compile("\\d{1,2}/\\d{1,2}/\\d{1,4}, \\d{1,2}:\\d{1,2}:\\d{1,2}:\\d\\d\\d");
            } else {
                p1 = Pattern.compile("\\d{1,2}/\\d{1,2}/\\d{1,2}, \\d{1,2}:\\d{1,2}:\\d{1,2}:\\d\\d\\d");
            }
        } else {
            p1 = Pattern.compile("\\d{1,2}/\\d{1,2}/\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}:\\d\\d\\d");
        }

        Matcher m1 = p1.matcher(lvPrgmOut.getStdout());
        assertTrue("Failed assertion that binaryLog exited with successful return code", (lvPrgmOut.getReturnCode() == 0));
        // (WI 234118) We cannot perform this check because "Instance ID" is translated, which causes the test to fail in non-English locales. There is no ID or
        // non-translated portion of the message to even try to match.
        // assertTrue("Failed assertion that binaryLog did produce an output file", lvPrgmOut.getStdout().contains("Instance ID "));

        assertTrue("Failed assertion that binaryLog displayed default date format", m1.find());

        lvPrgmOut = exeBinaryLog(new String[] { arg1, CommonTasks.getBinaryLogDir(server).getAbsolutePath() + "/logdata", arg2 });
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

    }

    /**
     * Testing the listInstances action of BinaryLog command. Also testing --includeInstance Filter option of View.
     *
     * @throws Exception
     */
    @Test
    public void testHeaderProcessName() throws Exception {
        CommonTasks.writeLogMsg(Level.INFO, "get instance list from " + server.getServerName());

        ProgramOutput lvPrgmOut = exeBinaryLog(new String[] { "listInstances", CommonTasks.getBinaryLogDir(server).getAbsolutePath() });
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

            // c:\sumam\Liberty\workspace3\build.image\wlp\bin>binaryLog view server1 --includeInstance=latest

            lvPrgmOut = exeBinaryLog(new String[] { "view", CommonTasks.getBinaryLogDir(server).getAbsolutePath(), "--includeInstance=" + id });

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
        if (server.getMachine().getOperatingSystem().equals(OperatingSystem.WINDOWS)) {
            exeExt = ".bat";
//                      } else {
//                              exeExt = ".sh";
        }

        StringBuilder cmd = new StringBuilder(100);
        cmd.append(rProfBinFile.getAbsolutePath()).append(server.getMachine().getOperatingSystem().getFileSeparator());
        cmd.append(BINARY_LOG).append(exeExt).append(" ");

        for (String cmdOption : cmdLineOptions) {
            if (!cmdOption.isEmpty()) {
                cmd.append("\"" + cmdOption + "\" ");
            }
        }

        logMsg("executing: " + cmd.toString());
//        logMsg("executing: " + rProfBinFile.getAbsolutePath() + HpelSetup.getNodeUnderTest().getMachine().getOperatingSystem().getFileSeparator() + cmd.toString());
//        return HpelSetup.getNodeUnderTest().getMachine().execute(LOG_VIEWER + exeExt, cmdLineOptions, rProfBinFile.getAbsolutePath());
        return server.getMachine().execute(cmd.toString(), workDir == null ? rProfBinFile.getAbsolutePath() : workDir);

//              }
//              //iSeries LogViewer needs to be executed in the shell qsh
//              else{
//                      return HpelSetup.getNodeUnderTest().getMachine().executeQSH(LOG_VIEWER, cmdLineOptions, rProfBinFile.getAbsolutePath(), null);
//              }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Restore values we saw before changing them in setUp()
        CommonTasks.writeLogMsg(Level.INFO, "Resetting configuration to pre test values.");
//        if (backup != null && backup.exists()) {
//            server.getServerConfigurationFile().copyFromSource(backup);
//        }
        if (server != null && server.isStarted()) {
            server.stopServer();
        }

    }

    /**
     * Determine if we should or should not execute this test. Returns true if the test should NOT be ran.
     **/

    public static boolean skipTest() {
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

    /**
     * A simple method used to log messages from this test case to the test case's logs
     */
    public void logMsg(String msg) {
        if (thisLogger.isLoggable(Level.INFO)) {
            thisLogger.info(msg);
        }
    }

}