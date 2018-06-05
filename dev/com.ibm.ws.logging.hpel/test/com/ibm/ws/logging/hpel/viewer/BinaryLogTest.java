/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.hpel.viewer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.logging.hpel.FormatSet;
import com.ibm.ws.logging.internal.hpel.LocaleUtils;

import test.common.SharedOutputManager;

public class BinaryLogTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Install directory property set by bvt.xml.
     */
    private static final String WLP_INSTALL_DIR = System.getProperty("install.dir", "../build.image/wlp");

    /**
     * True if running on Windows and the .bat file should be used.
     */
    private static final boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");

    /**
     * Environment variable that can be set to test the UNIX script on Windows.
     */
    private static final String WLP_CYGWIN_HOME = System.getenv("WLP_CYGWIN_HOME");

    Logger logger = Logger.getLogger(getClass().getName());

    @Rule
    public TestRule outputRule = outputMgr;

    /*
     * Tests that actually launch binaryLog
     */

    /*
     * binaryLog help
     */
//    @Test
    public void testHelp() throws Exception {
        List<String>[] returnOutput = new List[2];
        returnOutput = execute(Arrays.asList("help"));
        List<String> argOutput = returnOutput[0];
        System.out.println(argOutput);
        assertTrue("Output should be multiline", argOutput.size() > 0);
        assertTrue("First line of output should contain a Usage string, Usage: binaryLog action", argOutput.get(0).startsWith("Usage: binaryLog action"));
    }

    /*
     * binaryLog help view
     */
    @Test
    public void testHelpView() throws Exception {
        List<String>[] returnOutput = new List[2];
        returnOutput = execute(Arrays.asList("help", "view"));
        List<String> argOutput = returnOutput[0];
        System.out.println(argOutput);
        assertTrue("Output should be multiline", argOutput.size() > 0);
        assertTrue("First line of output should contain a Usage string, Usage: binaryLog view", argOutput.get(0).startsWith("Usage: binaryLog view"));
        assertTrue("There should be a line contain --isoDateFormat options", argOutput.get(60).contains("--isoDateFormat"));
    }

    /*
     * binaryLog help copy
     */
    @Test
    public void testHelpCopy() throws Exception {
        List<String>[] returnOutput = new List[2];
        returnOutput = execute(Arrays.asList("help", "copy"));
        List<String> argOutput = returnOutput[0];
        System.out.println(argOutput);
        assertTrue("Output should be multiline", argOutput.size() > 0);
        assertTrue("First line of output should contain a Usage string, Usage: binaryLog copy", argOutput.get(0).startsWith("Usage: binaryLog copy"));
    }

    /*
     * binaryLog help listInstances
     */
    @Test
    public void testHelpListInstances() throws Exception {
        List<String>[] returnOutput = new List[2];
        returnOutput = execute(Arrays.asList("help", "listInstances"));
        List<String> argOutput = returnOutput[0];
        System.out.println(argOutput);
        assertTrue("Output should be multiline", argOutput.size() > 0);
        assertTrue("First line of output should contain a Usage string, Usage: binaryLog listInstances", argOutput.get(0).startsWith("Usage: binaryLog listInstances"));
        assertTrue("There should be a line contain --isoDateFormat options", argOutput.get(13).contains("--isoDateFormat"));
    }

    /*
     * binaryLog help ASDFASDF
     */
    @Test
    public void testHelpASDFASDF() throws Exception {
        List<String>[] returnOutput = new List[2];
        returnOutput = execute(Arrays.asList("help", "ASDFASDF"));
        List<String> argOutput = returnOutput[0];
        System.out.println(argOutput);
        assertTrue("Output should be multiline", argOutput.size() > 0);
        assertTrue("First line of output should contain a Usage string, Usage: binaryLog action", argOutput.get(0).startsWith("Usage: binaryLog action"));
    }

    /*
     * binaryLog view with parameters
     */
    @Test
    public void testParseCmdLineArgsView() throws Exception {

        String action = "view";
        File repositoryDir = new File("repositoryDir");
        File targetDir = null;

        String[] commandArgs = new String[] { "--includeExtension=thread=someThread,requestID=someRequestID",
                                              "--minLevel=FINE",
                                              "--maxLevel=INFO",
                                              "--includeLogger=inc*",
                                              "--excludeLogger=exc*",
                                              "--includeThread=2a",
                                              "--locale=es_ES",
                                              "--includeMessage=*msg*",
                                              "--excludeMessage=*abcd*" };
        String expectedConfig = "latestInstance=false, minLevel=FINE, maxLevel=INFO, startDate=null, stopDate=null, includeLoggers=inc*, excludeLoggers=exc*, hexThreadID=2a, tailInterval=-1, locale=es_es, message=*msg*, excludeMessages=*abcd*, extensions=[thread=someThread, requestID=someRequestID], encoding=null, action=view";
        helpTestParseCmdLineArgs(action, commandArgs, repositoryDir, targetDir, expectedConfig);

        commandArgs = new String[] { "--monitor" };
        expectedConfig = "latestInstance=false, minLevel=null, maxLevel=null, startDate=null, stopDate=null, includeLoggers=null, excludeLoggers=null, hexThreadID=null, tailInterval=1, locale=null, message=null, excludeMessages=null, extensions=[], encoding=null, action=view";
        helpTestParseCmdLineArgs(action, commandArgs, repositoryDir, targetDir, expectedConfig);

        commandArgs = new String[] { "--isoDateFormat" };
        expectedConfig = "latestInstance=false, minLevel=null, maxLevel=null, startDate=null, stopDate=null, includeLoggers=null, excludeLoggers=null, hexThreadID=null, tailInterval=-1, locale=null, message=null, excludeMessages=null, extensions=[], encoding=null, action=view";
        helpTestParseCmdLineArgs(action, commandArgs, repositoryDir, targetDir, expectedConfig);

    }

    /*
     * parameter parsing tests
     * This test case is to test the various scenarios of passing -minDate and -maxDate to view action
     * Following scenarios are been covered under the test
     * 1. Passing YYYY for mindDate/maxDate e.g. 24-02-2013
     * 2. Passing YY for minDate/maxDate e.g. 24-02-13
     * 3. Passing MM for minDate/maxDate e.g. 24-02-2013
     * 4. Passing M for minDate/maxDate e.g. 24-2-2013
     * 5. Passing Time along with date for minDate/MaxDate e.g. 24-2-2013 11.12.13:999
     * 6. Passing iso-8601 date format for minDate/maxDate e.g. 2013-02-24 or 2013-02-24T
     * 7. Passing iso-8601 date and time format for minDate/maxDate e.g.2013-02-04T11:12:13.999-0500
     */

    @Test
    public void testParseCmdLineArgsViewWithDates() throws Exception {

        if (isLocaleToBeTested()) {
            String action = "view";
            File repositoryDir = new File("repositoryDir");

            String expectedMinDate = LocaleUtils.getLocaleBasedMinDate();
            String expectedMaxDate = LocaleUtils.getLocaleBasedMaxDate();

            if (expectedMinDate != null && expectedMaxDate != null) {

                System.out.println(" Testing started for YYYY Pattern  ");
                testYYYYPattern(action, repositoryDir, expectedMinDate, expectedMaxDate);
                System.out.println(" Testined ended for YYYY Pattern ");

                System.out.println(" Testing started for Unpadded Month ");
                testUnpaddedMonth(action, repositoryDir, expectedMinDate, expectedMaxDate);
                System.out.println(" Testing ended for Unpadded Month ");

                System.out.println(" Testing started for Padded Month ");
                testPaddedMonth(action, repositoryDir, expectedMinDate, expectedMaxDate);
                System.out.println(" Testing ended for Padded Month ");

                System.out.println(" Testing started for Time Pattern ");
                testTimePattern(action, repositoryDir, expectedMinDate, expectedMaxDate);
                System.out.println(" Testing ended for Time Pattern ");

                System.out.println(" Testing started for YY Pattern ");
                testYYPattern(action, repositoryDir, expectedMinDate, expectedMaxDate);
                System.out.println(" Testing ended for YY Pattern ");

                System.out.println(" Testing started for iso-8601 Date Pattern ");
                testISODatePattern(action, repositoryDir, expectedMinDate, expectedMaxDate);
                System.out.println(" Testing ended for iso-8601 Date Pattern ");

                System.out.println(" Testing started for iso-8601 Date Time Pattern ");
                testISODateTimePattern(action, repositoryDir, expectedMinDate, expectedMaxDate);
                System.out.println(" Testing ended for YY Pattern ");
            }
        }

    }

    /**
     *
     * This test case is used to test scenario where user passes YYYY pattern for minDate/maxDate. e.g. 24-02-2013
     *
     * @param action
     * @param repositoryDir
     * @param expectedMinDate
     * @param expectedMaxDate
     */
    private void testYYYYPattern(String action, File repositoryDir, String expectedMinDate, String expectedMaxDate) {

        String[] commandArgs = new String[] { "--minDate=" + LocaleUtils.getTestDate("24-02-2013"), "--maxDate=" + LocaleUtils.getTestDate("24-2-2013") };
        testParseCmdLine(action, expectedMinDate, expectedMaxDate, commandArgs, repositoryDir);
    }

    /**
     * This test case is used to test scenario where user passes YY pattern for minDate/maxDate. e.g. 24-02-13
     *
     * @param action
     * @param repositoryDir
     * @param expectedMinDate
     * @param expectedMaxDate
     */
    private void testYYPattern(String action, File repositoryDir, String expectedMinDate, String expectedMaxDate) {

        if (LocaleUtils.isLocaleSkippedForTimePatternTesting()) {
            try {

                DateFormat dateTimeFormat = FormatSet.customizeDateFormat(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM));
                dateTimeFormat.setLenient(false);
                SimpleDateFormat sdf = new SimpleDateFormat("dd/M/yy HH:mm:ss:SSS");
                sdf.setLenient(false);
                expectedMinDate = dateTimeFormat.format(sdf.parse("24/2/13 00:00:00:000"));
                expectedMaxDate = dateTimeFormat.format(sdf.parse("24/2/13 23:59:59:999"));
                String[] commandArgs = new String[] { "--minDate=" + LocaleUtils.getTestDate("24-02-13"), "--maxDate=" + LocaleUtils.getTestDate("24-2-13") };
                testParseCmdLine(action, expectedMinDate, expectedMaxDate, commandArgs, repositoryDir);
            } catch (ParseException e) {
                e.printStackTrace();
            }

        }

    }

    /**
     * This test case test the scenario where user passes date and time for minDate/maxDate e.g. 24-02-2013 11.12.13:999
     *
     * @param action
     * @param repositoryDir
     * @param expectedMinDate
     * @param expectedMaxDate
     */
    private void testTimePattern(String action, File repositoryDir, String expectedMinDate, String expectedMaxDate) {
        if (LocaleUtils.isLocaleSkippedForTimePatternTesting()) {
            String[] commandArgs = new String[] { "--minDate=" + LocaleUtils.getTestDateTime("24-02-2013", false), "--maxDate=" + LocaleUtils.getTestDate("24-02-2013") }; // Testing the scenario ofpadded month parse accordingly into MM/M
            if (expectedMinDate.contains("00:00:00:000")) {
                expectedMinDate = expectedMinDate.replace("00:00:00:000", "11:12:13:999");
                testParseCmdLine(action, expectedMinDate, expectedMaxDate, commandArgs, repositoryDir);

            } else if (expectedMinDate.contains("0:00:00:000")) {
                expectedMinDate = expectedMinDate.replace("0:00:00:000", "11:12:13:999");
                testParseCmdLine(action, expectedMinDate, expectedMaxDate, commandArgs, repositoryDir);

            } else if (expectedMinDate.contains("00.00.00:000")) {
                expectedMinDate = expectedMinDate.replace("00.00.00:000", "11.12.13:999");
                testParseCmdLine(action, expectedMinDate, expectedMaxDate, commandArgs, repositoryDir);

            } else if (expectedMinDate.contains("0.00.00:000")) {
                expectedMinDate = expectedMinDate.replace("0.00.00:000", "11.12.13:999");
                testParseCmdLine(action, expectedMinDate, expectedMaxDate, commandArgs, repositoryDir);
            }
        }

    }

    /**
     * This test case test the scenario where user passes date and time for minDate/maxDate in iso-8601 format e.g. 2013-02-24 or 2013-02-24T
     *
     * @param action
     * @param repositoryDir
     * @param expectedMinDate
     * @param expectedMaxDate
     */
    private void testISODatePattern(String action, File repositoryDir, String expectedMinDate, String expectedMaxDate) {
        String[] commandArgs = new String[] { "--minDate=2013-02-24", "--maxDate=2013-02-24T" };
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdtf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        sdf.setLenient(true);
        try {
            expectedMinDate = sdtf.format(sdf.parse("2013-02-24"));
            expectedMaxDate = sdtf.format(sdf.parse("2013-02-24T"));
            expectedMaxDate = expectedMaxDate.replace("00:00:00.000", "23:59:59.999");
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            e.printStackTrace();
        }
        testParseCmdLine(action, expectedMinDate, expectedMaxDate, commandArgs, repositoryDir, true);
    }

    /**
     * This test case test the scenario where user passes date and time for minDate/maxDate in iso-8601 format e.g. 2013-02-24T11:12:13.999-0500
     *
     * @param action
     * @param repositoryDir
     * @param expectedMinDate
     * @param expectedMaxDate
     */
    private void testISODateTimePattern(String action, File repositoryDir, String expectedMinDate, String expectedMaxDate) {
        String[] commandArgs = new String[] { "--minDate=2013-02-24T00:00:00.000-0500", "--maxDate=2013-02-24T23:59:59.999-0500" };
        SimpleDateFormat sdtf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        sdtf.setLenient(true);
        try {
            expectedMinDate = sdtf.format(sdtf.parse("2013-02-24T00:00:00.000-0500"));
            expectedMaxDate = sdtf.format(sdtf.parse("2013-02-24T23:59:59.999-0500"));
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            e.printStackTrace();
        }
        testParseCmdLine(action, expectedMinDate, expectedMaxDate, commandArgs, repositoryDir, true);
    }

    /**
     * This test case will test the scenario where user is passing padded month. e.g. 24-02-2013
     *
     * @param action
     * @param repositoryDir
     * @param expectedMinDate
     * @param expectedMaxDate
     */
    private void testPaddedMonth(String action, File repositoryDir, String expectedMinDate, String expectedMaxDate) {
        String dateStr = LocaleUtils.getTestDate("24-02-2013");
        String[] commandArgs = new String[] { "--minDate=" + dateStr, "--maxDate=" + dateStr };
        testParseCmdLine(action, expectedMinDate, expectedMaxDate, commandArgs, repositoryDir);

    }

    /**
     * This test case will test the scenario where user will be passing unpadded month to binaryLog for minDate/maxDate e.g. 24-2-2013
     *
     * @param action
     * @param repositoryDir
     * @param expectedMinDate
     * @param expectedMaxDate
     */
    private void testUnpaddedMonth(String action, File repositoryDir, String expectedMinDate, String expectedMaxDate) {
        String[] commandArgs = new String[] { "--minDate=" + LocaleUtils.getTestDate("24-2-2013"), "--maxDate=" + LocaleUtils.getTestDate("24-2-2013") };
        testParseCmdLine(action, expectedMinDate, expectedMaxDate, commandArgs, repositoryDir);

    }

    /**
     * @param action
     * @param expectedMinDate
     * @param expectedMaxDate
     * @param commandArgs
     * @param repositoryDir
     */
    private void testParseCmdLine(String action, String expectedMinDate, String expectedMaxDate, String[] commandArgs, File repositoryDir, boolean isoDateFormat) {

        String expectedConfig = "latestInstance=false, minLevel=null, maxLevel=null, startDate="
                                + expectedMinDate
                                + ", stopDate="
                                + expectedMaxDate
                                + ", includeLoggers=null, excludeLoggers=null, hexThreadID=null, tailInterval=-1, locale=null, message=null, excludeMessages=null, extensions=[], encoding=null, action=view";
        helpTestParseCmdLineArgs(action, commandArgs, repositoryDir, null, expectedConfig, isoDateFormat);

    }

    /**
     * @param action
     * @param expectedMinDate
     * @param expectedMaxDate
     * @param commandArgs
     * @param repositoryDir
     */
    private void testParseCmdLine(String action, String expectedMinDate, String expectedMaxDate, String[] commandArgs, File repositoryDir) {
        testParseCmdLine(action, expectedMinDate, expectedMaxDate, commandArgs, repositoryDir, false);
    }

    /**
     *
     */
    private boolean isLocaleToBeTested() {
        if (Locale.getDefault().toString().equalsIgnoreCase("ja_JP_JP") || Locale.getDefault().toString().equalsIgnoreCase("zh_HK"))
            return false;
        else
            return true;

    }

    @Test
    public void testParseCmdLineArgsCopy() throws Exception {

        String action = "copy";
        File repositoryDir = new File("repositoryDir");
        File targetDir = new File("targetDir");
        String[] commandArgs;
        String expectedConfig;

        commandArgs = new String[] { "--includeExtension=thread=someThread,requestID=someRequestID",
                                     "--minLevel=FINE",
                                     "--maxLevel=INFO",
                                     "--includeLogger=inc*",
                                     "--excludeLogger=exc*",
                                     "--includeThread=2a",
                                     "--includeMessage=*msg*",
                                     "--excludeMessage=*abcd*",
                                     "--includeInstance=latest" };
        expectedConfig = "latestInstance=true, minLevel=FINE, maxLevel=INFO, startDate=null, stopDate=null, includeLoggers=inc*, excludeLoggers=exc*, hexThreadID=2a, tailInterval=-1, locale=null, message=*msg*, excludeMessages=*abcd*, extensions=[thread=someThread, requestID=someRequestID], encoding=null, action=copy";
        helpTestParseCmdLineArgs(action, commandArgs, repositoryDir, targetDir, expectedConfig);
    }

    /**
     * @param action
     * @param commandArgs
     * @param repositoryDir
     * @param targetDir
     * @param expectedConfig
     * @param isoDateFormat
     */
    private void helpTestParseCmdLineArgs(String action, String[] commandArgs, File repositoryDir, File targetDir, String expectedConfig, boolean isoDateFormat) {
        String[] args = commandArgs;
        BinaryLog viewer = new BinaryLog(action, repositoryDir, targetDir);
        if (isoDateFormat)
            viewer.useISODateFormatObjects();
        String prettyArgs = getPrettyArgs(args);
        viewer.parseCmdLineArgs(args);
        String actualConfig = viewer.toString();
        logger.info("Comparing actual and expected output ");
        logger.info(" Actual output: " + actualConfig);
        logger.info("  Expected output : " + expectedConfig);

        assertTrue("BinaryLog state not as expected for [binaryLog view repositoryDir " + prettyArgs + "]\n" +
                   "expected: [" + expectedConfig + "]\n" +
                   "actual  : [" + actualConfig + "]\n",
                   actualConfig.equals(expectedConfig));

    }

    /**
     * @param action
     * @param commandArgs
     * @param repositoryDir
     * @param targetDir
     * @param expectedConfig
     */
    private void helpTestParseCmdLineArgs(String action, String[] commandArgs, File repositoryDir, File targetDir, String expectedConfig) {
        helpTestParseCmdLineArgs(action, commandArgs, repositoryDir, targetDir, expectedConfig, false);

    }

    private void helpTestParseCmdLineArgsThrowable(String action, String[] commandArgs, File repositoryDir, File targetDir, String expectedThrowableMessage) {
        String[] args = commandArgs;
        BinaryLog viewer = new BinaryLog(action, repositoryDir, targetDir);
        String prettyArgs = getPrettyArgs(args);
        String actualThrowableMessage = "";
        boolean throwableThrown = false;
        try {
            viewer.parseCmdLineArgs(args);
        } catch (Throwable t) {
            actualThrowableMessage = t.getMessage();
            throwableThrown = true;
        }

        if (!throwableThrown)
            assertFalse("BinaryLog request [binaryLog view repositoryDir " + prettyArgs + "]\n" +
                        "expected exception with text [" + expectedThrowableMessage + "] but no exception was thrown",
                        throwableThrown);

        else
            assertTrue("BinaryLog request [binaryLog view repositoryDir " + prettyArgs + "]\n" +
                       "expected exception text [" + expectedThrowableMessage + "]\n" +
                       "actual exception text [" + ((actualThrowableMessage != null) ? actualThrowableMessage : "null") + "]",
                       actualThrowableMessage.equals(expectedThrowableMessage));

    }

    private static String getPrettyArgs(String[] args) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (String arg : args) {
            if (first) {
                first = false;
            } else {
                sb.append(" ");
            }

            sb.append("\"").append(arg).append("\"");
        }
        return sb.toString();
    }

    private static List[] execute(List<String> args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<String>();
        if (isWindows && WLP_CYGWIN_HOME == null) {
            command.add(WLP_INSTALL_DIR + "/bin/binaryLog.bat");
        } else {
            if (WLP_CYGWIN_HOME == null) {
                command.add("/bin/sh");
            } else {
                command.add(WLP_CYGWIN_HOME + "/bin/sh");
            }
            command.add("-x");
            command.add(WLP_INSTALL_DIR + "/bin/binaryLog");
        }
        command.addAll(args);

        System.out.println("Executing " + command);

        ProcessBuilder builder = new ProcessBuilder();
        System.out.println("***env:" + builder.environment());
        builder.command(command);

        final Process p = builder.start();
        List<String> output = new ArrayList<String>();
        List<String> error = new ArrayList<String>();

        Thread stderrCopier = new Thread(new OutputStreamCopier(p.getErrorStream(), error));
        stderrCopier.start();
        new OutputStreamCopier(p.getInputStream(), output).run();

        stderrCopier.join();
        p.waitFor();

        int exitValue = p.exitValue();
        if (exitValue != 0) {
            throw new IOException(command.get(0) + " failed (" + exitValue + "): " + output + " ERROR: " + error);
        }

        List[] returnList = new List[2];
        returnList[0] = output;
        returnList[1] = error;

        return returnList;
    }

    private static class OutputStreamCopier implements Runnable {
        private final InputStream in;
        private final List<String> output;

        OutputStreamCopier(InputStream in, List<String> lines) {
            this.in = in;
            this.output = lines;
        }

        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                boolean inEval = false;
                int carryover = 0;

                for (String line; (line = reader.readLine()) != null;) {
                    // Filter empty lines and sh -x trace output.
                    if (inEval) {
                        System.out.println("(trace eval) " + line);
                        if (line.trim().equals("'")) {
                            inEval = false;
                        }
                    } else if (line.equals("+ eval '")) {
                        inEval = true;
                        System.out.println("(trace eval) " + line);
                    } else if (carryover > 0) {
                        carryover--;
                        System.out.println("(trace) " + line);
                    } else if (line.startsWith("+") || line.equals("'")) {
                        int index = 0;
                        index = line.indexOf("+", index + 1);
                        while (index != -1) {
                            index = line.indexOf("+", index + 1);
                            carryover++;
                        }
                        System.out.println("(trace) " + line);
                    } else if (!line.isEmpty()) {
                        synchronized (output) {
                            output.add(line);
                        }
                        System.out.println(line);
                    }
                }
            } catch (IOException ex) {
                throw new Error(ex);
            }
        }
    }

}
