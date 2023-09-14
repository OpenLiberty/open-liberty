package com.ibm.ws.kernel.boot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.beans.Transient;
import java.io.File;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.OperatingSystem;

import componenttest.common.apiservices.Bootstrap;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.utils.FileUtils;
import componenttest.topology.utils.LibertyServerUtils;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class VerboseLogTest {
    private static LibertyServer server;
    private final static String SERVER_NAME = "com.ibm.ws.kernel.boot.verbose";
    static String executionDir;
    static String serverCommand;
    static File jvmoptionsserverroot;

    @BeforeClass
    public static void before() throws Exception {
        server = LibertyServerFactory.getLibertyServer(SERVER_NAME);
        executionDir = server.getInstallRoot();
        jvmoptionsserverroot = new File(executionDir + "/usr/servers/" + SERVER_NAME + "/jvm.options");
        if (server.getMachine().getOperatingSystem() == OperatingSystem.WINDOWS)
            serverCommand = "bin\\server.bat";
        else
            serverCommand = "bin/server";
    }

    @AfterClass
    public static void after() throws Exception {
        if (server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public static void testDefaultVerboseLogging() {
        // Test with no jvm.options, verbose log should appear by default

        final String METHOD_NAME = "testDefaultVerboseLogging";

        String[] parms = new String[2];
        parms[0] = "start";
        parms[1] = SERVER_NAME;

        ProgramOutput po = server.getMachine().execute(serverCommand, parms, executionDir, envVars);

        server.waitForStringInLog("CWWKF0011I");

        //check if verbosegc log exists
    }

    @Test
    public static void testJvmTurnOfVerbose() {
        // Test with jvm.options to turn off verbose log, no verbose log should appear

        final String METHOD_NAME = "testJvmTurnOfVerbose";

        String[] parms = new String[2];
        parms[0] = "start";
        parms[1] = SERVER_NAME;

        ProgramOutput po = server.getMachine().execute(command, parms, executionDir, envVars);

        server.waitForStringInLog("CWWKF0011I");

        //check if no verbosegc log exists
    }

    @Test
    public static void testJvmChangeVerbose() {
        // Test with jvm.options, change location or file name, the jvm.options log should be the one shown

        final String METHOD_NAME = "testJvmChangeVerbose";

        String[] parms = new String[2];
        parms[0] = "start";
        parms[1] = SERVER_NAME;

        ProgramOutput po = server.getMachine().execute(command, parms, executionDir, envVars);

        server.waitForStringInLog("CWWKF0011I");

        //check if verbosegc log exists
    }

    @Test
    public static void testEnvVarChangeVerbose() {
        // Test with environment variable, change location or file name, the jvm.options log should be the one shown

        final String METHOD_NAME = "testEnvVarChangeVerbose";

        String[] parms = new String[2];
        parms[0] = "start";
        parms[1] = SERVER_NAME;

        ProgramOutput po = server.getMachine().execute(command, parms, executionDir, envVars);

        server.waitForStringInLog("CWWKF0011I");

        //check if verbosegc log exists
    }

    @Test
    public static void testStartChangeVerbose() {
        // Test with command variable, change location or file name, the jvm.options log should be the one shown
        // ex. ./bin/server start --verbose:gc

        final String METHOD_NAME = "testStartChangeVerbose";

        String[] parms = new String[4];
        parms[0] = "start";
        parms[1] = SERVER_NAME;
        parms[2] = "--verbose:gc";

        ProgramOutput po = server.getMachine().execute(command, parms, executionDir, envVars);

        server.waitForStringInLog("CWWKF0011I");

        //check if verbosegc log exists
    }
}
