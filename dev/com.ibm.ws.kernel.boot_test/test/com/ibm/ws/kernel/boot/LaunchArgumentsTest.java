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
package com.ibm.ws.kernel.boot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;
import test.shared.TestUtils;

import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants.VerifyServer;

/**
 *
 */
public class LaunchArgumentsTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule outputRule = outputMgr;

    @BeforeClass
    public static void setUpBeforeClass() {
        TestUtils.cleanTempFiles();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        TestUtils.cleanTempFiles();
    }

    @Test
    public void testProcessBatchFileArgs() {
        Launcher launcher = new Launcher();
        assertEquals(Arrays.asList(),
                     launcher.processBatchFileArgs(makeList()));
        assertEquals(Arrays.asList("defaultServer"),
                     launcher.processBatchFileArgs(makeList("defaultServer")));
        assertEquals(Arrays.asList("--batch-file"),
                     launcher.processBatchFileArgs(makeList("--batch-file")));
        assertEquals(Arrays.asList("--batch-file=--stop"),
                     launcher.processBatchFileArgs(makeList("--batch-file=--stop")));

        assertEquals(Arrays.asList("defaultServer"),
                     launcher.processBatchFileArgs(makeList("--batch-file", "run")));
        assertEquals(Arrays.asList("defaultServer"),
                     launcher.processBatchFileArgs(makeList("--batch-file", "run", "defaultServer")));
        assertEquals(Arrays.asList("myServer"),
                     launcher.processBatchFileArgs(makeList("--batch-file", "run", "myServer")));
        assertEquals(Arrays.asList("defaultServer", "--option"),
                     launcher.processBatchFileArgs(makeList("--batch-file", "run", "--option")));
        assertEquals(Arrays.asList("defaultServer", "--option"),
                     launcher.processBatchFileArgs(makeList("--batch-file", "run", "defaultServer", "--option")));
        assertEquals(Arrays.asList("myServer", "--option"),
                     launcher.processBatchFileArgs(makeList("--batch-file", "run", "myServer", "--option")));

        assertEquals(Arrays.asList("--stop"),
                     launcher.processBatchFileArgs(makeList("--stop")));
        assertEquals(Arrays.asList("--stop", "defaultServer"),
                     launcher.processBatchFileArgs(makeList("--stop", "defaultServer")));
        assertEquals(Arrays.asList("--stop", "myServer"),
                     launcher.processBatchFileArgs(makeList("--stop", "myServer")));
        assertEquals(Arrays.asList("--stop", "--option"),
                     launcher.processBatchFileArgs(makeList("--stop", "--option")));
        assertEquals(Arrays.asList("--stop", "defaultServer", "--option"),
                     launcher.processBatchFileArgs(makeList("--stop", "defaultServer", "--option")));
        assertEquals(Arrays.asList("--stop", "myServer", "--option"),
                     launcher.processBatchFileArgs(makeList("--stop", "myServer", "--option")));

        assertEquals(Arrays.asList("--stop", "defaultServer"),
                     launcher.processBatchFileArgs(makeList("--batch-file=--stop", "stop")));
        assertEquals(Arrays.asList("--stop", "defaultServer"),
                     launcher.processBatchFileArgs(makeList("--batch-file=--stop", "stop", "defaultServer")));
        assertEquals(Arrays.asList("--stop", "myServer"),
                     launcher.processBatchFileArgs(makeList("--batch-file=--stop", "stop", "myServer")));
        assertEquals(Arrays.asList("--stop", "defaultServer", "--option"),
                     launcher.processBatchFileArgs(makeList("--batch-file=--stop", "stop", "--option")));
        assertEquals(Arrays.asList("--stop", "defaultServer", "--option"),
                     launcher.processBatchFileArgs(makeList("--batch-file=--stop", "stop", "defaultServer", "--option")));
        assertEquals(Arrays.asList("--stop", "myServer", "--option"),
                     launcher.processBatchFileArgs(makeList("--batch-file=--stop", "stop", "myServer", "--option")));
    }

    /**
     * Make sure the command line --clean will override the system property
     * value for clean
     */
    @Test
    public void testParameterClean() {
        String[] args = new String[] { "--clean" };
        List<String> cmdArgs = new ArrayList<String>(Arrays.asList(args));

        System.setProperty(BootstrapConstants.INITPROP_OSGI_CLEAN, "none");

        Map<String, String> initProps = new HashMap<String, String>();
        LaunchArguments launchArgs = new LaunchArguments(cmdArgs, initProps);
        ReturnCode rc = launchArgs.getRc();

        assertEquals("InitProps should contain the value to enable a clean start",
                     BootstrapConstants.OSGI_CLEAN_VALUE, initProps.get(BootstrapConstants.INITPROP_OSGI_CLEAN));
        assertNull("The system property value should be removed, as overridden by command line",
                   System.getProperty(BootstrapConstants.OSGI_CLEAN_VALUE));
        assertEquals("VerifyServer should be CREATE_DEFAULT for default start action",
                     VerifyServer.CREATE_DEFAULT, rc.getVerifyServer());
    }

    /**
     * Make sure the command line --autoAcceptSigner will override the system property
     * value for SSL auto-accept-signer-certificate.
     */
    @Test
    public void testParameterAutoAcceptSigner() {
        String[] args = new String[] { "--autoAcceptSigner" };
        List<String> cmdArgs = new ArrayList<String>(Arrays.asList(args));

        System.setProperty(BootstrapConstants.AUTO_ACCEPT_SIGNER, "false");

        Map<String, String> initProps = new HashMap<String, String>();
        LaunchArguments launchArgs = new LaunchArguments(cmdArgs, initProps, true);
        ReturnCode rc = launchArgs.getRc();

        assertEquals("InitProps should contain the value to enable a clean start",
                     "true", initProps.get(BootstrapConstants.AUTO_ACCEPT_SIGNER));
        assertEquals("VerifyServer should be CREATE_DEFAULT for default start action",
                     VerifyServer.CREATE_DEFAULT, rc.getVerifyServer());
    }

    @Test
    public void testParameterEmpty() {
        String[] args = new String[] {};
        List<String> cmdArgs = new ArrayList<String>(Arrays.asList(args));
        Map<String, String> initProps = new HashMap<String, String>();
        LaunchArguments launchArgs = new LaunchArguments(cmdArgs, initProps);
        ReturnCode rc = launchArgs.getRc();

        assertEquals("VerifyServer should be CREATE_DEFAULT for default start action",
                     VerifyServer.CREATE_DEFAULT, rc.getVerifyServer());

        assertEquals("We should be ok!", ReturnCode.OK, rc);
    }

    /**
     */
    @Test
    public void testParameterCreate() {
        String[] args = new String[] { "--create" };
        List<String> cmdArgs = new ArrayList<String>(Arrays.asList(args));
        Map<String, String> initProps = new HashMap<String, String>();

        LaunchArguments launchArgs = new LaunchArguments(cmdArgs, initProps);
        ReturnCode rc = launchArgs.getRc();

        assertEquals("VerifyServer should be CREATE for create action",
                     VerifyServer.CREATE, rc.getVerifyServer());
        assertEquals("We should be set for a create operation", ReturnCode.CREATE_ACTION, rc);
    }

    /**
     */
    @Test
    public void testParameterStop() {
        String[] args = new String[] { "--stop" };
        List<String> cmdArgs = new ArrayList<String>(Arrays.asList(args));
        Map<String, String> initProps = new HashMap<String, String>();
        LaunchArguments launchArgs = new LaunchArguments(cmdArgs, initProps);
        ReturnCode rc = launchArgs.getRc();

        assertEquals("VerifyServer should be EXISTS for stop action",
                     VerifyServer.EXISTS, rc.getVerifyServer());

        assertSame("Stop command should return stop action return code", ReturnCode.STOP_ACTION, rc);
    }

    /**
     */
    @Test
    public void testParameterStatus() {
        String[] args = new String[] { "--status" };
        List<String> cmdArgs = new ArrayList<String>(Arrays.asList(args));
        Map<String, String> initProps = new HashMap<String, String>();
        LaunchArguments launchArgs = new LaunchArguments(cmdArgs, initProps);
        ReturnCode rc = launchArgs.getRc();

        assertEquals("VerifyServer should be EXISTS for status action",
                     VerifyServer.EXISTS, rc.getVerifyServer());

        assertSame("status command should return status action return code", ReturnCode.STATUS_ACTION, rc);
    }

    /**
     */
    @Test
    public void testParameterStatusStart() {
        String[] args = new String[] { "--status:start" };
        List<String> cmdArgs = new ArrayList<String>(Arrays.asList(args));
        Map<String, String> initProps = new HashMap<String, String>();
        LaunchArguments launchArgs = new LaunchArguments(cmdArgs, initProps);
        ReturnCode rc = launchArgs.getRc();

        assertEquals("VerifyServer should be SKIP for status:start action",
                     VerifyServer.SKIP, rc.getVerifyServer());

        assertSame("status:start command should return start status action return code", ReturnCode.START_STATUS_ACTION, rc);
    }

    /**
     */
    @Test
    public void testParameterServerName() {
        String[] args = new String[] { "name1", "name2" };
        List<String> cmdArgs = new ArrayList<String>(Arrays.asList(args));
        Map<String, String> initProps = new HashMap<String, String>();
        LaunchArguments launchArgs = new LaunchArguments(cmdArgs, initProps);

        assertSame("status should be ok after double name", ReturnCode.OK, launchArgs.getRc());
        assertTrue("should see a warning about second name", outputMgr.checkForStandardOut("CWWKE0027W"));
    }

    @Test
    public void testParameterVersion() {
        String[] args = new String[] { "--version" };
        List<String> cmdArgs = new ArrayList<String>(Arrays.asList(args));
        Map<String, String> initProps = new HashMap<String, String>();
        LaunchArguments launchArgs = new LaunchArguments(cmdArgs, initProps);
        ReturnCode rc = launchArgs.getRc();

        assertEquals("VerifyServer should be SKIP for version action",
                     VerifyServer.SKIP, rc.getVerifyServer());

        assertEquals("ReturnCode should select version action", ReturnCode.VERSION_ACTION, rc);
    }

    @Test
    public void testParameterHelp() {
        String[] args = new String[] { "--help" };
        List<String> cmdArgs = new ArrayList<String>(Arrays.asList(args));
        Map<String, String> initProps = new HashMap<String, String>();
        LaunchArguments launchArgs = new LaunchArguments(cmdArgs, initProps);
        ReturnCode rc = launchArgs.getRc();

        assertEquals("VerifyServer should be SKIP for help action",
                     VerifyServer.SKIP, rc.getVerifyServer());

        assertEquals("ReturnCode should select help action", ReturnCode.HELP_ACTION, rc);
    }

    @Test
    public void testParameterHelpArgs() {
        String[] args = new String[] { "--script=bin/server", "--help" };
        List<String> cmdArgs = new ArrayList<String>(Arrays.asList(args));
        Map<String, String> initProps = new HashMap<String, String>();
        LaunchArguments launchArgs = new LaunchArguments(cmdArgs, initProps);

        assertEquals("ReturnCode should select help action", ReturnCode.HELP_ACTION, launchArgs.getRc());
        assertEquals("original argument should be present as an option", "--help", launchArgs.getOption("arg"));
        assertEquals("script should be set as an option", "bin/server", launchArgs.getOption("script"));

        args = new String[] { "--help:usage" };
        cmdArgs = new ArrayList<String>(Arrays.asList(args));
        launchArgs = new LaunchArguments(cmdArgs, initProps);

        assertEquals("ReturnCode should select help action", ReturnCode.HELP_ACTION, launchArgs.getRc());
        assertEquals("original argument should be present as an option", "--help:usage", launchArgs.getOption("arg"));
        assertNull("script should not be set as an option", launchArgs.getOption("script"));
    }

    /**
     */
    @Test
    public void testParameterUnknownBad() {
        String args[] = new String[] { "--garbage" };
        List<String> cmdArgs = new ArrayList<String>(Arrays.asList(args));
        Map<String, String> initProps = new HashMap<String, String>();
        LaunchArguments launchArgs = new LaunchArguments(cmdArgs, initProps);

        assertSame("status should be BAD_ARGUMENT after garbage argument", ReturnCode.BAD_ARGUMENT, launchArgs.getRc());
        assertTrue("should see a error about bad argument", outputMgr.checkForStandardOut("CWWKE0013E"));
    }

    @Test
    public void testParameterUnknownSingleDash() {
        String args[] = new String[] { "-garbage" };
        List<String> cmdArgs = new ArrayList<String>(Arrays.asList(args));
        Map<String, String> initProps = new HashMap<String, String>();
        LaunchArguments launchArgs = new LaunchArguments(cmdArgs, initProps);

        assertSame("status should be BAD_ARGUMENT after garbage argument", ReturnCode.BAD_ARGUMENT, launchArgs.getRc());
        assertTrue("should see a error about bad argument", outputMgr.checkForStandardOut("CWWKE0013E"));
    }

    private List<String> makeList(String... args) {
        return new ArrayList<String>(Arrays.asList(args));
    }
}
