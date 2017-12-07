/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

import com.ibm.websphere.simplicity.LocalFile;
import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.common.apiservices.Bootstrap;
import componenttest.exception.TopologyException;
import componenttest.topology.utils.PrivHelper;

public class LibertyServerFactory {
    //track the known Liberty servers by test class name, so that suites don't clean up servers from other test classes in the suite
    private static Map<String, Set<LibertyServer>> knownLibertyServers = new HashMap<String, Set<LibertyServer>>();
    private static Class<?> c = LibertyServerFactory.class;
    private static final Boolean BACKUP_REQUIRED = shouldBackup();

    // To enable running Liberty as a Windows Service.
    public static enum WinServiceOption {
        ON,
        OFF
    }

    private static final boolean DELETE_RUN_FATS = PrivHelper.getBoolean("delete.run.fats");

    /**
     * This method will return a newly created LibertyServer instance with the specified server name
     *
     * @return A stopped Liberty Server instance
     * @throws Exception
     */
    public static LibertyServer getLibertyServer(String serverName) {
        return getLibertyServer(serverName, null, true);
    }

    /**
     * This method will return a newly created LibertyServer instance with the specified server name
     *
     * @param serverName The name of the server
     * @param testClassName The name of the class to associate with the server.
     * @return A stopped Liberty Server instance
     */
    public static LibertyServer getLibertyServer(String serverName, Class<?> testClass) {
        return getLibertyServer(serverName, null, true, false, false, testClass.getCanonicalName());
    }

    /**
     * This method will return a newly created LibertyServer instance with the specified server name.
     * Note if the ignoreCache parameter is set to false, then the returned LibertyServer instance may
     * not be newly-created.
     *
     * @return A stopped Liberty Server instance
     * @throws Exception
     */
    public static LibertyServer getLibertyServer(String serverName, Bootstrap bootstrap, boolean ignoreCache) {
        return getLibertyServer(serverName, bootstrap, ignoreCache, false, false, null);
    }

    public static LibertyServer getExistingLibertyServer(String serverName) {
        return getLibertyServer(serverName, null, false, false, true, null);
    }

    public static LibertyServer getExistingLibertyServer(String serverName, Bootstrap bootstrap) {
        return getLibertyServer(serverName, bootstrap, false, false, true, null);
    }

    public static LibertyServer getLibertyServer(String serverName, WinServiceOption wsOpt) {
        return getLibertyServer(serverName, null, true, false, false, null, wsOpt);
    }

    private static LibertyServer getLibertyServer(String serverName, Bootstrap bootstrap, boolean ignoreCache, boolean installServerFromSampleJar, boolean usePreviouslyConfigured,
                                                  String testClassName) {

        return getLibertyServer(serverName, bootstrap, ignoreCache, installServerFromSampleJar, usePreviouslyConfigured, testClassName,
                                WinServiceOption.OFF);
    }

    private static LibertyServer getLibertyServer(String serverName, Bootstrap bootstrap, boolean ignoreCache, boolean installServerFromSampleJar, boolean usePreviouslyConfigured,
                                                  String testClassName, WinServiceOption winServiceOpt) {
        try {
            if (serverName == null)
                throw new Exception("Liberty server name cannot be null.");

            LibertyServer ls = null;

            WinServiceOption windowsServiceOption = winServiceOpt;
            if (windowsServiceOption == WinServiceOption.ON) {
                if (hasWinAdminRights() == false) {
                    windowsServiceOption = WinServiceOption.OFF;
                }
            }

            //Associate the LibertyServer with the calling test name
            if (testClassName == null)
                testClassName = getCallerClassNameFromStack();
            Log.info(LibertyServerFactory.class, "getLibertyServer", "server=" + serverName + "   testClassName=" + testClassName);

            //synchronize while we look for an existing server with this name
            //and create one if it doesn't exist yet
            //hold the lock until we finish copying server information
            //since we don't want to return while the server state is incomplete
            synchronized (knownLibertyServers) {
                Set<LibertyServer> knownServers = knownLibertyServers.get(testClassName);
                if (knownServers == null) {
                    knownServers = new HashSet<LibertyServer>();
                    knownLibertyServers.put(testClassName, knownServers);
                }

                if (!ignoreCache) {
                    String userDir = (bootstrap == null) ? null : bootstrap.getValue("libertyUserDir"); // usually null
                    for (LibertyServer s : knownServers) {
                        if (s.getServerName().equals(serverName)) {
                            // If the server we're getting has a custom user directory
                            // then make sure the known server has the same one.
                            // If not it's a different server with the same name.
                            if (userDir != null && !userDir.equals(s.getUserDir()))
                                continue;
                            ls = s;
                            ls.unTidy(); // mark this server as unTidy so that tidyAllKnownServers will stop it after the next test runs
                            Log.info(LibertyServerFactory.class, "getLibertyServer", "found existing server for " + testClassName + ", reissuing");
                            break;
                        }
                    }
                } else {
                    Log.finer(LibertyServerFactory.class, "getLibertyServer", "Ignoring cache for request for " + serverName);
                }

                //if we haven't yet encountered this server, then create the instance and
                //copy the autoFVT contents to the servers dir and backup if necessary etc
                if (ls == null) {
                    if (bootstrap == null) {
                        Log.finer(LibertyServerFactory.class, "getLibertyServer", "using default bootstrapping.properties");
                        bootstrap = Bootstrap.getInstance();
                    } else {
                        Log.info(LibertyServerFactory.class, "getLibertyServer", "using supplied bootstrapping.properties");
                    }
                    ls = new LibertyServer(serverName, bootstrap, ignoreCache, usePreviouslyConfigured, windowsServiceOption);

                    if (!usePreviouslyConfigured) {
                        if (installServerFromSampleJar) {
                            if (!LibertyFileManager.libertyFileExists(ls.getMachine(), ls.getServerRoot())) {
                                //Samples don't overwrite, so only bother running it if the server directory isn't already there
                                ls.installSampleWithExternalDependencies(serverName);
                            } else {
                                Log.warning(LibertyServerFactory.class, "Server directory for sample " + serverName + " already exists - executing sample installer was SKIPPED.");
                            }
                        } else {
                            //copy the published FAT server content for the test
                            recursivelyCopyDirectory(ls.getMachine(), new LocalFile(ls.getPathToAutoFVTNamedServer()), new RemoteFile(ls.getMachine(), ls.getServerRoot()));

                            //copy any shared content
                            LocalFile sharedFolder = new LocalFile(LibertyServer.PATH_TO_AUTOFVT_SHARED);
                            if (sharedFolder.exists())
                                recursivelyCopyDirectory(ls.getMachine(), sharedFolder, new RemoteFile(ls.getMachine(), ls.getServerSharedPath()));
                        }

                        RemoteFile[] autoInstall = applicationsToVerify(ls);

                        if (ls.getJvmOptionsAsMap().containsKey("-Ddelay.start.applications")) {
                            removeHeldApplicationsFromDropins(ls, ls.getJvmOptionsAsMap().get("-Ddelay.start.applications"));
                        }

                        //Now we backup just before we install the applications ONLY if RTC Run
                        if (BACKUP_REQUIRED) {
                            preTestBackUp(ls);
                        }
                        if (autoInstall != null) {
                            //There are applications to install so we need to install them all :)
                            Log.info(c, "getLibertyServer", "Found dropins folder with applications to verify!");
                            addAppsToVerificationList(autoInstall, ls);
                        }
                    }
                }

                knownServers.add(ls);

                return ls;
            }
        } catch (Exception e) {
            Log.error(c, "getLibertyServer", e);
            throw new RuntimeException("Error getting server", e);
        }
    }

    public static boolean hasWinAdminRights() {

        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("win");

        if (!isWindows) {
            return false;
        }

        PrintStream systemErr = System.err;
        synchronized (systemErr) {
            System.setErr(new PrintStream(new OutputStream() {
                @Override
                public void write(int i) throws IOException {}
            }));
            try {
                Long time = new Long(System.currentTimeMillis());
                String unique = time.toString();
                Preferences prefs = Preferences.systemRoot();

                prefs.put(unique, "RemoveMe"); // SecurityException on Windows if code is not running with admin rights.

                prefs.remove(unique);
                prefs.flush();

                return true;

            } catch (Exception e) {
                return false;

            } finally {
                System.setErr(systemErr);
            }
        }
    }

    /**
     * @param ls
     * @param appNames
     * @param bootstrap
     */
    private static void removeHeldApplicationsFromDropins(LibertyServer ls, String appNames) {

        for (String appName : appNames.split(",")) {

            try {
                ls.removeDropinsApplications(appName);
                Log.info(c, "removeHeldApplicationsFromDropins", "moved app {0} out of dropins folder", appName);
            } catch (Exception e) {
                Log.error(c, "removeHeldApplicationsFromDropins", e);
            }

        }

    }

    /**
     * This method will return a newly created LibertyServer instance with the specified server name
     *
     * @return A started Liberty Server instance
     * @throws Exception
     */
    public static LibertyServer getStartedLibertyServer(String serverName) {
        try {
            LibertyServer ls = getLibertyServer(serverName);
            ls.startServer();
            return ls;
        } catch (Exception e) {
            Log.error(c, "getStartedLibertyServer", e);
            throw new RuntimeException("Error getting running server", e);
        }
    }

    /**
     * This method will install a sample server and download any external dependencies defined in it. Once the server is installed the server.xml will be exchanged with a default
     * server XML, that includes fatTestPorts.xml and the sample server.xml. The bootstrap.properties will be exchanged with a default properties file that includes the
     * "../testports.properties" file and the sample properties file. The server will then be added to the list of known servers and returned.
     *
     * @param serverName The name of the server to install, must be matched by a local file named serverName.jar in the lib/LibertyFATTestFiles folder (populated from publish/files
     *            in a FAT test project)
     * @param bootstrap The bootstrap to use on the server
     * @param ignoreCache <code>false</code> if we should load a cached server if available
     * @return The server
     *
     */
    public static LibertyServer installSampleServer(String serverName, Bootstrap bootstrap, boolean ignoreCache) {
        return getLibertyServer(serverName, bootstrap, ignoreCache, true, false, null);
    }

    public static LibertyServer installSampleServer(String serverName) {
        return installSampleServer(serverName, null, false);
    }

    /**
     * This method should not be ran by the user, it is ran by the JUnit
     * runner at the end of each test to ensure that post test tidying is
     * done.
     *
     * Once a server has under gone the tidy action, it needs to be removed
     * from the list of known servers, or subsequent cleanups will fail.
     * This will happen in the case of FATSuite being the entry point with
     * multiple test classes.
     *
     * @param testClassName, the name of the test class for which servers should be tidied
     * @throws Exception
     */
    public static void tidyAllKnownServers(String testClassName) throws Exception {
        Log.info(c, "tidyAllKnownServers", "Now post-test-tidying all servers known to test: " + testClassName);
        Exception unexpectedException = null;
        // Iteration through this data structure must be synchronized as
        // so it can not be modified during iteration
        synchronized (knownLibertyServers) {
            Collection<LibertyServer> knownServers = getKnownLibertyServers(testClassName);
            if (knownServers != null) {
                Iterator<LibertyServer> itr = knownServers.iterator();
                while (itr.hasNext()) {
                    LibertyServer ls = itr.next();
                    Log.info(c, "tidyAllKnownServers", "Tidying server: " + ls.getServerName() + " hash:" + ls.hashCode());
                    try {
                        if (!ls.isTidy()) {
                            ls.postTestTidy();
                        }
                    } catch (Exception e) {
                        if (unexpectedException == null)
                            unexpectedException = e;
                    }
                }
            }
        }

        if (unexpectedException != null) {
            throw unexpectedException;
        }
    }

    /**
     * This method should not be ran by the user, it is ran by the JUnit runner at the end of each test
     * to recover the servers.
     *
     * @param testClassName the name of the FAT test class to recover known servers for
     * @throws Exception
     */
    public static void recoverAllServers(String testClassName) throws Exception {
        Log.finer(c, "recoverAllServers", "Now recovering all servers known to test class: " + testClassName);

        //If backups were required, so is recovery
        if (BACKUP_REQUIRED) {
            // Iteration through this data structure must be synchronized as
            // so it can not be modified during iteration
            synchronized (knownLibertyServers) {
                for (LibertyServer ls : getKnownLibertyServers(testClassName)) {
                    if (ls.needsPostTestRecover()) {
                        postTestRecover(ls);
                        ls.setNeedsPostRecover(false);
                    }
                }
            }
        } else {
            //we weren't backing up, if we're running with delete run fats in RTC
            //we should delete the server to save space
            if (DELETE_RUN_FATS) {
                synchronized (knownLibertyServers) {
                    for (LibertyServer ls : getKnownLibertyServers(testClassName)) {
                        LibertyFileManager.deleteLibertyDirectoryAndContents(ls.getMachine(), ls.getServerRoot());
                    }
                }
            }
        }
    }

    private static RemoteFile[] applicationsToVerify(LibertyServer ls) {
        return applicationsToVerify(ls, ls.getPathToAutoFVTNamedServer());
    }

    private static RemoteFile[] applicationsToVerify(LibertyServer ls, String s) {
        int appCount = 0;
        try {
            LocalFile appFolder = new LocalFile(s + "dropins/");
            if (!!!appFolder.exists()) {
                return null; // The autoinstall folder doesn't exist so return false.
            }
            RemoteFile[] files = appFolder.list(false);
            if (files.length > 0) {
                return files;
            } else {
                return null;
            }
        } catch (Exception e) {
            Log.error(c, "", e);
            return null;
        } finally {
            Log.finer(c, "applicationsToVerify", appCount + " on server " + ls.getServerName());
        }
    }

    // For dynamically created servers
    public static void addAppsToVerificationList(LibertyServer ls) throws Exception {
        addAppsToVerificationList(applicationsToVerify(ls, ls.getServerRoot() + "/"), ls);
    }

    private static void addAppsToVerificationList(RemoteFile[] files, LibertyServer ls) throws Exception {
        try {
            for (RemoteFile f : files) {
                try {
                    String onlyAppName = f.getName();
                    if (onlyAppName.endsWith(".xml")) {
                        onlyAppName = onlyAppName.substring(0, onlyAppName.length() - 4);
                    }
                    if (onlyAppName.endsWith(".ear") || onlyAppName.endsWith(".eba") || onlyAppName.endsWith(".war") ||
                        onlyAppName.endsWith(".jar") || onlyAppName.endsWith(".rar") || onlyAppName.endsWith(".zip") ||
                        onlyAppName.endsWith(".esa")) {
                        onlyAppName = onlyAppName.substring(0, onlyAppName.length() - 4);
                    }
                    if (onlyAppName.endsWith(".js")) {
                        onlyAppName = onlyAppName.substring(0, onlyAppName.length() - 3);
                    }
                    if (onlyAppName.endsWith(".jsar")) {
                        onlyAppName = onlyAppName.substring(0, onlyAppName.length() - 5);
                    }
                    Log.info(c, "addAppsToVerificationList", "Adding " + onlyAppName + " to the startup verification list for server " + ls.getServerName());
                    ls.autoInstallApp(onlyAppName);
                } catch (TopologyException e) {
                    //Most likely an error with installing a directory so log and carry on
                    Log.error(c, "installApplications", e);
                } catch (Exception e) {
                    //Not a 'can't install a directory' Exception so throw
                    throw e;
                }
            }
        } catch (Exception e) {
            Log.error(c, "installApplications", e);
            throw e;
        }
    }

    public static void recursivelyCopyDirectory(Machine machine, LocalFile localDirectory, RemoteFile destination) throws Exception {
        String method = "recursivelyCopyDirectory";
        Log.entering(c, method);
        destination.mkdir();
        ArrayList<String> logs = new ArrayList<String>();
        logs = listDirectoryContents(localDirectory);
        for (String l : logs) {
            Log.finer(c, "recursivelyCopyDirectory", "Getting: " + l);
            LocalFile toCopy = new LocalFile(localDirectory, l);
            RemoteFile toReceive = new RemoteFile(machine, destination, l);
            if (toCopy.isDirectory()) {
                // Recurse
                recursivelyCopyDirectory(machine, toCopy, toReceive);
            } else {
                toReceive.copyFromSource(toCopy);
                Log.finer(c, "recursivelyCopyDirectory", l + " copied to " + toReceive.getAbsolutePath());
            }
        }
        Log.exiting(c, method);
    }

    @Deprecated
    private static ArrayList<String> listDirectoryContents(LocalFile serverDir) throws Exception {
        final String method = "serverDirectoryContents";
        Log.entering(c, method);
        if (!serverDir.isDirectory() || !serverDir.exists())
            throw new TopologyException("The specified directoryPath \'"
                                        + serverDir.getAbsolutePath() + "\' was not a directory");

        RemoteFile[] firstLevelFiles = serverDir.list(false);
        ArrayList<String> firstLevelFileNames = new ArrayList<String>();

        for (RemoteFile f : firstLevelFiles) {
            firstLevelFileNames.add(f.getName());
        }
        return firstLevelFileNames;
    }

    private static LocalFile getServerBackupZip(LibertyServer server) throws Exception {
        // Backup lives in the FAT project
        LocalFile backupDir = new LocalFile("build/backup");
        if (!backupDir.exists())
            backupDir.mkdirs();

        String backupFile = server.getServerName() + ".backup.zip";
        LocalFile backup = new LocalFile(backupDir, backupFile);

        return backup;
    }

    /**
     * This method is used to backup all the server's at the start. BEFORE anything is done to them.
     * If they already exist.
     */
    private static void preTestBackUp(LibertyServer server) throws Exception {
        final String METHOD = "preTestBackUp";
        Machine m = server.getMachine();

        LocalFile backup = getServerBackupZip(server);

        // Server is in the build.image/wlp/usr/servers dir
        RemoteFile usrServersDir = new RemoteFile(m, server.getServerRoot()).getParentFile(); //should be /wlp/usr/servers

        if (backup.exists()) {
            return;
        }
        Log.finer(c, METHOD, "Backing up Server: " + server.getServerName() + " to zip file: " + backup.getAbsolutePath());

        String workDir = usrServersDir.getAbsolutePath();
        String command = server.getMachineJavaJarCommandPath();
        String[] param = { "cMf", backup.getAbsolutePath(), server.getServerName() };
        ProgramOutput o = m.execute(command, param, workDir);
        if (o.getReturnCode() == 0) {
            Log.finer(c, METHOD, "Successfully backed up server: " + server.getServerName() + " to zip file: " + backup.getAbsolutePath());
        } else {
            Log.warning(c, "Backup jar process failed with return code " + o.getReturnCode());
            Log.warning(c, "Backup jar process failed with error " + o.getStderr());
            Log.warning(c, "Backup jar process failed with output " + o.getStdout());
        }
    }

    /**
     * This method is used to return the tested server back to the state it was in before
     * testing
     */
    private static void postTestRecover(LibertyServer server) throws Exception {
        final String METHOD = "postTestRecover";
        Machine m = server.getMachine();
        RemoteFile usrServersDir = new RemoteFile(m, server.getServerRoot()).getParentFile(); //should be /wlp/usr/servers

        LocalFile backup = getServerBackupZip(server);
        if (!backup.exists()) {
            Log.info(c, METHOD, "Backup file doesn't exist... skipping recovery");
            return;
        }
        Log.finer(c, METHOD, "Recovering Server: " + server.getServerName() + " from zip file: " + backup.getAbsolutePath());

        RemoteFile serverFolder = new RemoteFile(m, server.getServerRoot());
        if (!!!serverFolder.delete()) {
            Log.warning(c, "Unable to delete old serverFolder. Recovery failed!");
            // retry up to 5 seconds
            for (int i = 0; i < 5; i++) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    // try again
                }
                if (!!!serverFolder.delete()) {
                    Log.warning(c, "Unable to delete old serverFolder. Retry failed!");
                } else {
                    break;
                }
                // on the last try, mark the server with the failed cleanup.
                if (i == 4) {
                    Log.warning(c, "The clean server will not be restored.");
                    server.setServerCleanupProblem(true);
                    return;
                }
            }
        }

        String workDir = usrServersDir.getAbsolutePath();
        String command = server.getMachineJavaJarCommandPath();
        String[] param = { "xf", backup.getAbsolutePath() };
        ProgramOutput o = m.execute(command, param, workDir);
        if (o.getReturnCode() == 0) {
            Log.finer(c, METHOD, "Successfully recovered server: " + server.getServerName() + " from zip file: " + backup.getAbsolutePath());
        } else {
            Log.warning(c, "Recovery unjar process failed with return code " + o.getReturnCode());
            Log.warning(c, "Recovery unjar process failed with error " + o.getStderr());
            Log.warning(c, "Recovery unjar process failed with output " + o.getStdout());
        }
        server.setServerCleanupProblem(false);
    }

    /**
     * Checks if we should backup existing servers with the same name
     * and sets the flag.
     * We backup if skip.backup is not set or false.
     * We don't backup if we aren't running in a local workspace.
     */
    private static boolean shouldBackup() {
        try {
            Bootstrap b = Bootstrap.getInstance();
            if (Boolean.parseBoolean(b.getValue("skip.backup"))) {
                return false;
            }
            if (DELETE_RUN_FATS) {
                //don't bother backing up if we're going to delete the run fats
                return false;
            }
            return true;
        } catch (Exception e) {
            //if we couldn't figure out whether we should backup do it just in case
            return true;
        }
    }

    /**
     * @return the list of knownLibertyServers
     */
    public static Collection<LibertyServer> getKnownLibertyServers(String testClassName) {
        Log.finer(c, "getKnownLibertyServers", "Getting known liberty servers for test: " + testClassName);
        Set<LibertyServer> servers = new HashSet<LibertyServer>();
        synchronized (knownLibertyServers) {
            Set<LibertyServer> knownServers = knownLibertyServers.get(testClassName);
            if (knownServers != null)
                servers.addAll(knownServers);
            //could be that the server is known to a super type of this test
            //any servers defined (via getLibertyServer or getStartedLibertyServer) on a
            //super could be used by a test, so they qualify as known to the test
            try {
                Class<?> c = Class.forName(testClassName);
                Class<?> superClass = c.getSuperclass();
                while (superClass != null && !!!superClass.equals(Object.class)) {
                    Set<LibertyServer> superServers = knownLibertyServers.get(superClass.getName());
                    if (superServers != null)
                        servers.addAll(superServers);
                    superClass = superClass.getSuperclass();
                }
            } catch (ClassNotFoundException e) {
                Log.error(c, "getKnownLibertyServers", e);
            }
        }
        return servers;
    }

    /**
     * @return the list of FFDC logs
     */
    public static ArrayList<String> retrieveFFDCFile(LibertyServer server) throws Exception {
        return server.listFFDCFiles(server.getServerName());

    }

    /**
     * Hack to get the calling test class name from the stack.
     *
     * @param methodName the name of the method that is being called
     */
    private static String getCallerClassNameFromStack() {
        String thisClassName = LibertyServerFactory.class.getName();
        String previousElementClass = null;
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if (previousElementClass != null && previousElementClass.equals(thisClassName)) {
                //the last thing in the stack before the current element was LibertyServerFactory
                //if the current element is not also LibertyServerFactory then we found our caller
                String currentClassName = element.getClassName();
                if (!!!currentClassName.equals(thisClassName))
                    return currentClassName;
            }
            previousElementClass = element.getClassName();
        }
        return null;
    }

}
