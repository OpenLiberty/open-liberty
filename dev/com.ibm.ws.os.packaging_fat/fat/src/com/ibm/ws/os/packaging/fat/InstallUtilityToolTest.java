package com.ibm.ws.os.packaging.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Logger; 

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
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
     *            The server instance.
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
	String echo = "sudo echo 'JAVA_HOME=" +javaHome+ "'> ";
        boolean openLibExists = openLib.exists();
        boolean usrDirExists = usrDir.exists();
        if (openLibExists) {
            logger.info("/var/lib/openliberty found. OpenLiberty is Installed");
        }
        else {
            logger.info("OpenLiberty did not install successfully");
        }

        File sharedDir = new File("/var/lib/openliberty/usr/shared");
        File serverFile = new File(installRoot + "/server.env");
        String[] param1s = {"/var/lib/openliberty/usr/shared"};
        ProgramOutput po1 = runCommand("createShared", "sudo mkdir", param1s);
        boolean sharedExists = sharedDir.exists();
        if (sharedExists) {
            logger.info("directory was created successfully");
        }
         else {

            logger.info("failed trying to create the directory");
        }
        String[] param2s = {installRoot +  "/server.env"};
        ProgramOutput po2 = runCommand("createServerFile",echo, param2s);
        boolean serverEnvExists = serverFile.exists();
        if (serverEnvExists) {
            logger.info("file was created successfully");
        }
         else {
            logger.info("failed trying to create the file");
        }
        String[] param6s = {installRoot +  "/server.env /var/lib/openliberty/usr/shared"};
        ProgramOutput po6 = runCommand("moveServerFile", "sudo mv", param6s);
        String[] param3s = { "-R", "openliberty:openliberty", "/var/lib/openliberty/usr/shared" };
        ProgramOutput po3 = runCommand("sharedPerm", "sudo chown", param3s);
        String[] param4s = { "-R", "openliberty:openliberty", "/var/lib/openliberty/usr/shared/server.env" };
        ProgramOutput po4= runCommand("serverPerm", "sudo chown", param4s);
        String[] param5s = {"ls -l","/var/lib/openliberty/usr/shared"};
        ProgramOutput po5= runCommand("listFilesInShared", "sudo", param5s);
        String output = po5.getStdout();
        logger.info(output);
	String[] param7s = {javaHome+ "/bin/java"};
        ProgramOutput po7= runCommand("listFilesInJava", "ls -l", param7s);
        String output2 = po7.getStdout();
        logger.info(output2);
        String[] param8s = {"777", javaHome+ "/bin/java"};
        ProgramOutput po8= runCommand("changeJavaPerm", "chmod", param8s);
        String[] param9s = {javaHome+ "/bin/java"};
        ProgramOutput po9= runCommand("listFilesInJava", "ls -l", param9s);
        String output3 = po9.getStdout();
        logger.info(output3);
        exiting(c, METHOD_NAME);
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
        if (isLinux){
            String content = new Scanner(new File("/etc/os-release")).useDelimiter("\\Z").next();
            if (content.contains("ubuntu")){
                return true;
            }
        }
        return false;
    }

    protected static boolean isLinuxRhel() throws Exception {
        if (isLinux){
            String content = new Scanner(new File("/etc/os-release")).useDelimiter("\\Z").next();
            if (content.contains("rhel")){
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
        ProgramOutput po = server.getMachine().execute(command, params, installRoot,_envVars);
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
