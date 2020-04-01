package com.ibm.ws.install.packaging.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileFilter;
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

public abstract class InstallPackagesToolTest {

    private static final Class<?> c = InstallPackagesToolTest.class;
    public static LibertyServer server;
    public static String installRoot;
    public static boolean isLinux = System.getProperty("os.name").toLowerCase().contains("linux");
    protected static List<String> cleanFiles;
    protected static List<String> cleanDirectories;

    //Need to ensure JAVA_HOME is set correctly - can't rely on user's environment to be set to the same Java as the build/runtime environment
    protected static Properties _envVars = new Properties();
    public static boolean connectedToRepo = true;
    public static Logger logger = Logger.getLogger("com.ibm.ws.install.packaging.fat");
    public static String javaHome;
    public static Boolean isjavaHomeExecutable = false;
    public static String currentDirectory = ".";
    public static String autoFVTRoot = currentDirectory;
    public static String publishPath = autoFVTRoot + "/publish";
    public static String testVersion = "19.0.0.12";
    public static String testRPMName = publishPath + "/packages.test/" + "openliberty-" + testVersion + "-1.noarch.rpm";
    public static String testDEBName = publishPath + "/packages.test/" + "openliberty_1" + testVersion + "-1ubuntu1_all.deb";

    public static String currentVersion = "20.0.0.3";
    public static String currentRPMName = publishPath + "/packages.current/" + "openliberty-" + "*" + "+-1.noarch.rpm";

    public static String currentDEBName = publishPath + "/packages.current/" + "openliberty_1" + "*" + "-1ubuntu1_all.deb";

    public static String packageExt = ".none";
    public static Boolean packagesBuilt = false;

    protected static Boolean isSupportedOS() throws Exception {
        final String METHOD_NAME = "isSupportedOS";
        Boolean isSupportedOS = false;
        Boolean isSupportedArch = false;
        Boolean isSupportedOSDistribution = false;

        String arch = System.getProperty("os.arch");
        Boolean isPOWER = false;
        Boolean isIntel = false;

        //check for a supported OS Distribution (RHEL or Ubuntu)
        if ((isLinuxRHEL() || isLinuxUbuntu())) {
            isSupportedOS = true;
        }

        //check for a supported Architecture - x86_64 or power
        if (arch.equalsIgnoreCase("power") || arch.equalsIgnoreCase("ppc64le")) {
            Log.info(c, METHOD_NAME, "power architecture detected.");
            isSupportedArch = true;
        } else if (arch.equalsIgnoreCase("x86_64")
                   || arch.equalsIgnoreCase("amd64")) {
            Log.info(c, METHOD_NAME, "x86 architecture detected.");
            isSupportedArch = true;
        }

        if ((isSupportedArch) && (isSupportedOSDistribution)) {
            Log.info(c, METHOD_NAME, "Supported OS & Arch detected.");
            isSupportedOS = true;
        }

        return isSupportedOS;
    }

    public static File[] findFilesForId(File dir, final String id) {
        return dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().equals("a_id_" + id + ".zip");
            }
        });
    }

    /**
     * Setup the environment.
     *
     * @throws Exception
     */
    protected static void setupEnv() throws Exception {
        final String METHOD_NAME = "setup";
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.install.packaging_fat");
        installRoot = server.getInstallRoot();

        Log.info(c, METHOD_NAME, "installRoot: " + installRoot);
        javaHome = server.getMachineJavaJDK();
        Log.info(c, METHOD_NAME, "javaHome: " + javaHome);
        // check if javaHome is executable
        isjavaHomeExecutable = isFileExecutable(javaHome);

        cleanDirectories = new ArrayList<String>();
        cleanFiles = new ArrayList<String>();

        currentDirectory = new File(".").getCanonicalPath();
        Log.info(c, METHOD_NAME, "currentDirectory: " + currentDirectory);
        autoFVTRoot = currentDirectory;
        publishPath = autoFVTRoot + "/publish";
        testRPMName = publishPath + "/packages.test/" + "openliberty-" + testVersion + "-1.noarch.rpm";
        testDEBName = publishPath + "/packages.test/" + "openliberty_" + testVersion + "-1ubuntu1_all.deb";

        currentRPMName = publishPath + "/packages.current/" + "openliberty" + "*" + ".rpm";
        currentDEBName = publishPath + "/packages.current/" + "openliberty" + "*" + ".deb";
        Log.info(c, METHOD_NAME, "testRPMName: " + testRPMName);
        String packageDirName = publishPath + "/packages.current";

        if (isLinuxUbuntu()) {
            packageExt = ".deb";

        } else if (isLinuxRHEL()) {
            packageExt = ".rpm";
        }
        Log.info(c, METHOD_NAME, "packageExt set to: " + packageExt);

        String[] listPackageParam = { packageDirName + "/*" + packageExt };

        ProgramOutput po1 = null;

        po1 = runCommand(METHOD_NAME, "ls", listPackageParam);
        if ((po1.getReturnCode() == 0)) {
            Log.info(c, METHOD_NAME, "package found");
            packagesBuilt = true;

            // Try to uninstall package, in case it was not cleanly uninstalled before.
            Log.info(c, METHOD_NAME, "try to Uninstall package before running tests.");
            ProgramOutput po4 = uninstallPackage(METHOD_NAME, packageExt);

        } else {
            Log.info(c, METHOD_NAME, "package not found");
            packagesBuilt = false;
        }
    }

    protected static Boolean isFileExecutable(String filePath) throws Exception {
        String METHOD_NAME = "isFileExecutable";
        Boolean RC = false;
        String[] po1args = { "-m -v " + filePath + "|sed \"1 d\" | cut -c 10 |grep -v x" };
        ProgramOutput po1 = runCommand(METHOD_NAME, "namei", po1args);
        if (po1.getReturnCode() == 0) {
            RC = false;
            String[] po2args = { "-m -v " + filePath };
            ProgramOutput po2 = runCommand(METHOD_NAME, "namei", po2args);
            Log.info(c, METHOD_NAME, "Non executable directory or file found.:\n" + po2.getStdout());
        } else {
            RC = true;
        }
        return RC;
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
        /*
         * Remove files left behind by rpm/deb install
         * Should re-implement this using java
         * /var/lib/openliberty
         * /var/log/openliberty
         * /var/run/openliberty
         * /etc/init.d/openliberty
         * /usr/share/doc/openliberty
         * /usr/share/openliberty
         */
        String[] param1 = { "rm -rf", "/var/lib/openliberty", "/var/log/openliberty", "/var/run/openliberty",
                            "etc/init.d/openliberty", "/usr/share/doc/openliberty", "/usr/share/openliberty" };
        ProgramOutput po1 = runCommand(METHOD_NAME, "sudo", param1);
        Log.info(c, METHOD_NAME, "remove /var/lib/openliberty, /var/log/openliberty and /var/run/openliberty RC:" + po1.getReturnCode());
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

    protected static boolean isLinuxRHEL() throws Exception {
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
        Log.info(c, testcase, "stderr:" + po.getStderr());
        return po;
    }

    /**
     * Install older test version of package. Only to be used as part of testing updates.
     *
     * @param METHOD_NAME
     * @param packageExt
     * @return
     * @throws Exception
     */
    protected static ProgramOutput installOldPackage(String METHOD_NAME, String packageExt) throws Exception {
        String[] rpmparam1 = { "yum", "localinstall", "-y", testRPMName };
        String[] debparam1 = { "apt-get", "install", "-y", testDEBName };

        ProgramOutput po1 = null;

        if (packageExt == ".rpm") {
            po1 = runCommand(METHOD_NAME, "sudo", rpmparam1);
        } else if (packageExt == ".deb") {
            po1 = runCommand(METHOD_NAME, "sudo", debparam1);

        } else {
            //log usage error
        }
        return po1;

    }

    /**
     * Install current version of package.
     *
     * @param METHOD_NAME
     * @param packageExt  - valid values .rpm or .deb (to specify which type of package to install)
     * @return
     * @throws Exception
     */
    protected static ProgramOutput installCurrentPackage(String METHOD_NAME, String packageExt) throws Exception {
        String updateDEBName = publishPath + "/packages.current/" + "openliberty*.deb";
        String updateRPMName = publishPath + "/packages.current/" + "openliberty*.rpm";
        String[] rpmparam2 = { "yum", "localinstall", "-y", updateRPMName };
        String[] debparam2 = { "apt-get", "install", "-y", updateDEBName };

        ProgramOutput po2 = null;

        if (packageExt == ".rpm") {
            po2 = runCommand(METHOD_NAME, "sudo", rpmparam2);
        } else if (packageExt == ".deb") {
            po2 = runCommand(METHOD_NAME, "sudo", debparam2);

        } else {
            //log usage error
        }

        return po2;

    }

    /**
     * Roll back to older version of package.
     *
     * @param METHOD_NAME
     * @param packageExt  - valid values .rpm or .deb (to specify which type of package to install)
     * @return
     * @throws Exception
     */
    protected static ProgramOutput rollbackPackage(String METHOD_NAME, String packageExt) throws Exception {
        String rollbackDEBName = publishPath + "/packages.test/" + "openliberty*.deb";
        String rollbackRPMName = publishPath + "/packages.test/" + "openliberty*.rpm";
        String[] rpmparam2 = { "yum", "downgrade", "-y", rollbackRPMName };
        String[] debparam2 = { "apt-get", "install", "-y", "--allow-downgrades", rollbackDEBName };

        ProgramOutput po2 = null;

        if (packageExt == ".rpm") {
            po2 = runCommand(METHOD_NAME, "sudo", rpmparam2);
        } else if (packageExt == ".deb") {
            po2 = runCommand(METHOD_NAME, "sudo", debparam2);

        } else {
            //log usage error
        }
        return po2;
    }

    /**
     * Uninstall package.
     *
     * @param METHOD_NAME
     * @param packageExt  - valid values .rpm or .deb (to specify which type of package to install)
     * @return
     * @throws Exception
     */
    protected static ProgramOutput uninstallPackage(String METHOD_NAME, String packageExt) throws Exception {
        String[] rpmparam2 = { "yum", "remove", "-y", "openliberty" };
        String[] debparam2 = { "apt-get", "remove", "-y", "openliberty" };

        ProgramOutput po2 = null;

        if (packageExt == ".rpm") {
            po2 = runCommand(METHOD_NAME, "sudo", rpmparam2);
        } else if (packageExt == ".deb") {
            po2 = runCommand(METHOD_NAME, "sudo", debparam2);

        } else {
            //log usage error
        }
        return po2;
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

        // check for pid file if server was started
//        if ( (serviceCommand == "start")||(serviceCommand == "restart") ) {
        File pidFile = new File("/var/run/openliberty/" + serverName + ".pid");
        if (pidFile.exists()) {
            Log.info(c, METHOD_NAME, "server pid file exists");
            String[] param5 = { "cat", "/var/run/openliberty/" + serverName + ".pid" };
            ProgramOutput po5 = runCommand(METHOD_NAME, "sudo", param5);
        } else {
            Log.info(c, METHOD_NAME, "server pid file does not exist");
        }

//        }
        return po1;
    }

    /**
     * checkUserGroupOwnership will check that all files in the specified folder are owned by the specified owner and group
     *
     * @param METHOD_NAME
     * @param User
     * @param Group
     * @param Folder      - folder to check
     * @return - true if all files are owned by User, and belong to group Group
     * @throws Exception
     */
    protected static boolean checkUserGroupOwnership(String METHOD_NAME, String User, String Group, String Folder) throws Exception {

        Boolean returnCode = false;
        Log.info(c, METHOD_NAME, "Checking ownership of :" + Folder + "\nExpected User:" + User + "\nExpected Group:" + Group + "\n");
        //find . \! -user openliberty -print
        String[] param1 = { "find", Folder, "\\!", "-user " + User + " -print " };
        ProgramOutput po1 = runCommand(METHOD_NAME, "sudo", param1);

        String[] param2 = { "find", Folder, "\\!", "-group " + Group + " -print" };
        ProgramOutput po2 = runCommand(METHOD_NAME, "sudo", param2);

        // check that po1 and po2 returned no files
        if ((po1.getStdout().length() == 0) && (po2.getStdout().length() == 0)) {
            returnCode = true;
        } else {
            returnCode = false;
        }

        return returnCode;
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
