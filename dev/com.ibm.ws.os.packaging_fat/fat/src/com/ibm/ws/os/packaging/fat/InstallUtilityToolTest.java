/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.os.packaging.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Logger;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public abstract class InstallUtilityToolTest {

    private static final Class<?> c = InstallUtilityToolTest.class;
    public static LibertyServer server;
    public static String installRoot;
    public static boolean isLinux = System.getProperty("os.name").toLowerCase().contains("linux");
    protected static List<String> cleanFiles;
    protected static List<String> cleanDirectories;
    //Need to ensure JAVA_HOME is set correctly - can't rely on user's environment to be set to the same Java as the build/runtime environment
    protected static Properties _envVars = new Properties();
    public static boolean connectedToRepo = true;
    public static Logger logger = Logger.getLogger("com.api.jar");
    public static String javaHome;

    /**
     * Setup the environment.
     *
     * @param svr
     *                The server instance.
     *
     * @throws Exception
     */
    protected static void setupEnv() throws Exception {
        final String METHOD_NAME = "setup";
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.os.packaging_fat");
        installRoot = server.getInstallRoot();
        Log.info(c, METHOD_NAME, "installRoot: " + installRoot);
        javaHome = server.getMachineJavaJDK();
        Log.info(c, METHOD_NAME, "javaHome: " + javaHome);
        cleanDirectories = new ArrayList<String>();
        cleanFiles = new ArrayList<String>();
    }

    protected static void createServerEnv() throws Exception {
        String METHOD_NAME = "createServerEnv";
        entering(c, METHOD_NAME);
        File openLib = new File("/var/lib/openliberty");
        File usrDir = new File("/var/lib/openliberty/usr");
        String echo = "sudo echo 'JAVA_HOME=" + javaHome + "'> ";
        boolean openLibExists = openLib.exists();
        boolean usrDirExists = usrDir.exists();
        if (openLibExists) {
            logger.info("/var/lib/openliberty found. OpenLiberty is Installed");
        } else {
            logger.info("OpenLiberty did not install successfully");
        }

        String[] param8s = { "chmod", "-R", "a+X", "/home" };
        ProgramOutput po8 = runCommand("changeJavaPerm", "sudo", param8s);
        String[] param9s = { javaHome + "/bin/java" };
        ProgramOutput po9 = runCommand("listFilesInJava", "ls -l", param9s);
        String output3 = po9.getStdout();
        logger.info(output3);

        //DEBUG
        String[] param10 = { "/etc/os-release" };
        ProgramOutput po10 = runCommand("OSRelease", "cat", param10);
        String output10 = po10.getStdout();
        logger.info(output10);

        String[] param11 = { "-laR " + javaHome + "/lib/jli" };
        ProgramOutput po11 = runCommand("list libjli", "ls", param11);
        String output11 = po11.getStdout();
        logger.info(output11);
        exiting(c, METHOD_NAME);

        String[] param12 = { "-version" };
        ProgramOutput po12 = runCommand("javaVersion", javaHome + "/bin/java", param12);
        String output12 = po12.getStdout();
        logger.info(output12);

        //env
        String[] param13 = { "" };
        ProgramOutput po13 = runCommand("env", "env", param13);
        String output13 = po13.getStdout();
        logger.info(output13);

        // ldd
        String[] param14 = { javaHome + "/bin/java" };
        ProgramOutput po14 = runCommand("ldd", "ldd", param14);
        String output14 = po14.getStdout();
        logger.info(output14);

        exiting(c, METHOD_NAME);
    }

    /**
     *
     * Run openliberty service
     *
     * @param METHOD_NAME
     * @param serviceCommand - can be start/stop/restart/status
     * @param serverName     - openliberty server name i.e. defaultServer
     * @return
     * @throws Exception
     */

    protected static ProgramOutput serviceCommand(String METHOD_NAME, String serviceCommand, String serverName) throws Exception {

        String[] param1 = { "systemctl", serviceCommand, "openliberty@" + serverName + ".service" };
        ProgramOutput po1 = runCommand(METHOD_NAME, "sudo", param1);

        if (po1.getReturnCode() != 0) {
            Log.info(c, METHOD_NAME, "command:" + serviceCommand + "failed");

            String[] param3 = { "systemctl", "status", "openliberty@" + serverName + ".service" };
            ProgramOutput po3 = runCommand(METHOD_NAME, "sudo", param3);

            String[] param4 = { "cat", "/etc/init.d/openliberty" };
            ProgramOutput po4 = runCommand(METHOD_NAME, "sudo", param4);
        }

        File pidFile = new File("/var/run/openliberty/" + serverName + ".pid");
        if (pidFile.exists()) {
            Log.info(c, METHOD_NAME, "server pid file exists");
            String[] param5 = { "cat", "/var/run/openliberty/" + serverName + ".pid" };
            ProgramOutput po5 = runCommand(METHOD_NAME, "sudo", param5);
        } else {
            Log.info(c, METHOD_NAME, "server pid file does not exist");
        }
        return po1;
    }

    protected static void entering(Class<?> c, String METHOD_NAME) {
        Log.info(c, METHOD_NAME, "---- " + METHOD_NAME + " : entering ----------------------------");
    }

    protected static void exiting(Class<?> c, String METHOD_NAME) {
        Log.info(c, METHOD_NAME, "---- " + METHOD_NAME + " : exiting ----------------------------");
    }

    /**
     * This method removes all the testing artifacts from the server directories.
     *
     * @throws Exception
     */
    protected static void cleanupEnv() throws Exception {
        final String METHOD_NAME = "cleanupEnv";
        entering(c, METHOD_NAME);
        if (server.isStarted())
            server.stopServer();
        for (String cFile : cleanFiles) {
            server.deleteFileFromLibertyInstallRoot(cFile);
            Log.info(c, METHOD_NAME, "delete " + cFile);
        }
        for (String cDir : cleanDirectories) {
            server.deleteDirectoryFromLibertyInstallRoot(cDir);
            Log.info(c, METHOD_NAME, "delete " + cDir);
        }
        exiting(c, METHOD_NAME);
    }

    protected static boolean isLinuxUbuntu() throws Exception {
        if (isLinux) {
            String content = new Scanner(new File("/etc/os-release")).useDelimiter("\\Z").next();
            if (content.contains("ubuntu")) {
                return true;
            }
        }
        return false;
    }

    protected static boolean isLinuxRhel() throws Exception {
        if (isLinux) {
            String content = new Scanner(new File("/etc/os-release")).useDelimiter("\\Z").next();
            if (content.contains("rhel")) {
                return true;
            }
        }
        return false;
    }

    protected static ProgramOutput runCommand(String testcase, String command, String[] params) throws Exception {
        String args = "";
        for (String param : params) {
            args = args + " " + param;
        }
        Log.info(c, testcase,
                 "command: " + command + " " + args);
        ProgramOutput po = server.getMachine().execute(command, params, installRoot, _envVars);
        Log.info(c, testcase, po.getStdout());
        Log.info(c, testcase, command + " command exit code: " + po.getReturnCode());
        return po;
    }

    protected static void remove(Collection<String> files) throws Exception {
        String METHOD_NAME = "remove";
        entering(c, METHOD_NAME);
        for (String f : files) {
            server.deleteFileFromLibertyInstallRoot(f);
            Log.info(c, METHOD_NAME, "delete " + f);
        }
        exiting(c, METHOD_NAME);
    }

    protected static void remove(Collection<String> files, Collection<String> directories) throws Exception {
        String METHOD_NAME = "remove";
        entering(c, METHOD_NAME);
        for (String f : files) {
            server.deleteFileFromLibertyInstallRoot(f);
            Log.info(c, METHOD_NAME, "delete " + f);
        }
        for (String d : directories) {
            server.deleteDirectoryFromLibertyInstallRoot(d);
            Log.info(c, METHOD_NAME, "delete " + d);
        }
        exiting(c, METHOD_NAME);
    }

    protected static void assertFilesExist(String msg, String[] filePaths) throws Exception {
        for (String filePath : filePaths) {
            assertTrue(msg + ": " + filePath + " does not exist.", server.fileExistsInLibertyInstallRoot(filePath));
        }
    }

    protected static void assertFilesNotExist(String msg, String[] filePaths) throws Exception {
        for (String filePath : filePaths) {
            assertFalse(msg + ": " + filePath + " exists.", server.fileExistsInLibertyInstallRoot(filePath));
        }
    }
}
