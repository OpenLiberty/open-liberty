/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.simplicity.LocalFile;
import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.common.apiservices.Bootstrap;
import componenttest.exception.TopologyException;
import componenttest.topology.utils.PrivHelper;

public class LibertyClientFactory {
    //track the known Liberty clients by test class name, so that suites don't clean up clients from other test classes in the suite
    private static Map<String, Set<LibertyClient>> knownLibertyClients = new HashMap<String, Set<LibertyClient>>();
    private static Class<?> c = LibertyClientFactory.class;
    private static final Boolean BACKUP_REQUIRED = shouldBackup();

    private static final boolean DELETE_RUN_FATS = Boolean.parseBoolean(PrivHelper.getProperty("delete.run.fats", "false"));

    /**
     * This method will return a newly created LibertyClient instance with the specified client name
     *
     * @return A stopped Liberty Client instance
     * @throws Exception
     */
    public static LibertyClient getLibertyClient(String clientName) {
        return getLibertyClient(clientName, null, true);
    }

    /**
     * This method will return a newly created LibertyClient instance with the specified client name
     *
     * @return A stopped Liberty Client instance
     * @throws Exception
     */
    public static LibertyClient getLibertyClient(String clientName, Bootstrap bootstrap, boolean ignoreCache) {
        return getLibertyClient(clientName, bootstrap, ignoreCache, false);
    }

    private static LibertyClient getLibertyClient(String clientName, Bootstrap bootstrap, boolean ignoreCache, boolean installClientFromSampleJar) {
        try {
            if (clientName == null)
                throw new Exception("Liberty client name cannot be null.");

            LibertyClient lc = null;

            //Associate the LibertyClient with the calling test name
            String testClassName = getCallerClassNameFromStack();
            Log.info(LibertyClientFactory.class, "getLibertyClient", "testClassName: " + testClassName);

            //synchronize while we look for an existing client with this name
            //and create one if it doesn't exist yet
            //hold the lock until we finish copying client information
            //since we don't want to return while the client state is incomplete
            synchronized (knownLibertyClients) {
                Set<LibertyClient> knownClients = knownLibertyClients.get(testClassName);
                if (knownClients == null) {
                    knownClients = new HashSet<LibertyClient>();
                    knownLibertyClients.put(testClassName, knownClients);
                }

                if (!ignoreCache) {
                    for (LibertyClient s : knownClients) {
                        if (s.getClientName().equals(clientName)) {
                            lc = s;
                            Log.info(LibertyClientFactory.class, "getLibertyClient", "found existing client for " + testClassName + ", reissuing");
                            break;
                        }
                    }
                } else {
                    Log.info(LibertyClientFactory.class, "getLibertyClient", "Ignoring cache for request for " + clientName);
                }

                //if we haven't yet encountered this client, then create the instance and
                //copy the autoFVT contents to the clients dir and backup if necessary etc
                if (lc == null) {
                    if (bootstrap == null) {
                        Log.info(LibertyClientFactory.class, "getLibertyClient", "using default bootstrapping.properties");
                        bootstrap = Bootstrap.getInstance();
                    } else {
                        Log.info(LibertyClientFactory.class, "getLibertyClient", "using supplied bootstrapping.properties");
                    }
                    lc = new LibertyClient(clientName, bootstrap);

                    if (installClientFromSampleJar) {
                        if (!LibertyFileManager.libertyFileExists(lc.getMachine(), lc.getClientRoot())) {
                            //Samples don't overwrite, so only bother running it if the client directory isn't already there
                            lc.installSampleWithExternalDependencies(clientName);
                        } else {
                            Log.warning(LibertyClientFactory.class, "Client directory for sample " + clientName + " already exists - executing sample installer was SKIPPED.");
                        }
                    } else {
                        //copy the published FAT client content for the test
                        recursivelyCopyDirectory(lc.getMachine(), new LocalFile(lc.getPathToAutoFVTNamedClient()), new RemoteFile(lc.getMachine(), lc.getClientRoot()));

                    }

                    RemoteFile[] autoInstall = applicationsToVerify(lc);

                    //Now we backup just before we install the applications ONLY if RTC Run
                    if (BACKUP_REQUIRED) {
                        preTestBackUp(lc);
                    }
                    if (autoInstall != null) {
                        //There are applications to install so we need to install them all :)
                        Log.info(c, "getLibertyClient", "Found dropins folder with applications to verify!");
                        addAppsToVerificationList(autoInstall, lc);
                    }
                }

                knownClients.add(lc);

                return lc;
            }
        } catch (Exception e) {
            Log.error(c, "getLibertyClient", e);
            throw new RuntimeException("Error getting client", e);
        }
    }

    /**
     * This method will return a newly created LibertyClient instance with the specified client name
     *
     * @return A started Liberty Client instance
     * @throws Exception
     */
    public static LibertyClient getStartedLibertyClient(String clientName) {
        try {
            LibertyClient lc = getLibertyClient(clientName);
            return lc;
        } catch (Exception e) {
            Log.error(c, "getStartedLibertyClient", e);
            throw new RuntimeException("Error getting running client", e);
        }
    }

    /**
     * This method will install a sample client and download any external dependencies defined in it. Once the client is installed the client.xml will be exchanged with a default
     * client XML, that includes fatTestPorts.xml and the sample client.xml. The bootstrap.properties will be exchanged with a default properties file that includes the
     * "../testports.properties" file and the sample properties file. The client will then be added to the list of known clients and returned.
     *
     * @param clientName The name of the client to install, must be matched by a local file named clientName.jar in the lib/LibertyFATTestFiles folder (populated from publish/files
     *            in a FAT test project)
     * @param bootstrap The bootstrap to use on the client
     * @param ignoreCache <code>false</code> if we should load a cached client if available
     * @return The client
     *
     */
    public static LibertyClient installSampleClient(String clientName, Bootstrap bootstrap, boolean ignoreCache) {
        return getLibertyClient(clientName, bootstrap, ignoreCache, true);
    }

    public static LibertyClient installSampleClient(String clientName) {
        return installSampleClient(clientName, null, false);
    }

    /**
     * This method should not be ran by the user, it is ran by the JUnit runner at the end of each test
     * to recover the clients.
     *
     * @param testClassName the name of the FAT test class to recover known clients for
     * @throws Exception
     */
    public static void recoverAllclients(String testClassName) throws Exception {
        Log.info(c, "recoverAllclients", "Now recovering all clients known to test class: " + testClassName);

        //If backups were required, so is recovery
        if (BACKUP_REQUIRED) {
            // Iteration through this data structure must be synchronized as
            // so it can not be modified during iteration
            synchronized (knownLibertyClients) {
                for (LibertyClient lc : getKnownLibertyClients(testClassName)) {
                    postTestRecover(lc);
                }
            }
        } else {
            //we weren't backing up, if we're running with delete run fats in RTC
            //we should delete the client to save space
            if (DELETE_RUN_FATS) {
                synchronized (knownLibertyClients) {
                    for (LibertyClient lc : getKnownLibertyClients(testClassName)) {
                        LibertyFileManager.deleteLibertyDirectoryAndContents(lc.getMachine(), lc.getClientRoot());
                    }
                }
            }
        }
    }

    private static RemoteFile[] applicationsToVerify(LibertyClient lc) {
        try {
            LocalFile appFolder = new LocalFile(lc.getPathToAutoFVTNamedClient() + "dropins/");
            if (!!!appFolder.exists())
                return null; // The autoinstall folder doesn't exist so return false.
            RemoteFile[] files = appFolder.list(false);
            if (files.length > 0)
                return files;
            else
                return null;
        } catch (Exception e) {
            Log.error(c, "", e);
            return null;
        }
    }

    private static void addAppsToVerificationList(RemoteFile[] files, LibertyClient lc) throws Exception {
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
                    Log.info(c, "addAppsToVerificationList", "Adding " + onlyAppName + " to the startup verification list");
                    lc.autoInstallApp(onlyAppName);
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

    private static void recursivelyCopyDirectory(Machine machine, LocalFile localDirectory, RemoteFile destination) throws Exception {
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
    private static ArrayList<String> listDirectoryContents(LocalFile clientDir) throws Exception {
        final String method = "clientDirectoryContents";
        Log.entering(c, method);
        if (!clientDir.isDirectory() || !clientDir.exists())
            throw new TopologyException("The specified directoryPath \'"
                                        + clientDir.getAbsolutePath() + "\' was not a directory");

        RemoteFile[] firstLevelFiles = clientDir.list(false);
        ArrayList<String> firstLevelFileNames = new ArrayList<String>();

        for (RemoteFile f : firstLevelFiles) {
            firstLevelFileNames.add(f.getName());
        }
        return firstLevelFileNames;
    }

    private static LocalFile getClientBackupZip(LibertyClient client) throws Exception {
        // Backup lives in the FAT project
        LocalFile backupDir = new LocalFile("build/backup");
        if (!backupDir.exists())
            backupDir.mkdirs();

        String backupFile = client.getClientName() + ".backup.zip";
        LocalFile backup = new LocalFile(backupDir, backupFile);

        return backup;
    }

    /**
     * This method is used to backup all the client's at the start. BEFORE anything is done to them.
     * If they already exist.
     */
    private static void preTestBackUp(LibertyClient client) throws Exception {
        final String METHOD = "preTestBackUp";
        Machine m = client.getMachine();

        LocalFile backup = getClientBackupZip(client);

        // Client is in the build.image/wlp/usr/clients dir
        RemoteFile usrclientsDir = new RemoteFile(m, client.getClientRoot()).getParentFile(); //should be /wlp/usr/clients

        if (backup.exists()) {
            Log.info(c, METHOD, "Backup file already exists... skipping backup");
            return;
        }
        Log.info(c, METHOD, "Backing up Client: " + client.getClientName() + " to zip file: " + backup.getAbsolutePath());

        String workDir = usrclientsDir.getAbsolutePath();
        String command = client.getMachineJavaJarCommandPath();
        String[] param = { "cMf", backup.getAbsolutePath(), client.getClientName() };
        ProgramOutput o = m.execute(command, param, workDir);
        if (o.getReturnCode() == 0) {
            Log.info(c, METHOD, "Successfully backed up client: " + client.getClientName() + " to zip file: " + backup.getAbsolutePath());
        } else {
            Log.warning(c, "Backup jar process failed with return code " + o.getReturnCode());
            Log.warning(c, "Backup jar process failed with error " + o.getStderr());
            Log.warning(c, "Backup jar process failed with output " + o.getStdout());
        }
    }

    /**
     * This method is used to return the tested client back to the state it was in before
     * testing
     */
    private static void postTestRecover(LibertyClient client) throws Exception {
        final String METHOD = "postTestRecover";
        Machine m = client.getMachine();
        RemoteFile usrclientsDir = new RemoteFile(m, client.getClientRoot()).getParentFile(); //should be /wlp/usr/clients

        LocalFile backup = getClientBackupZip(client);
        if (!backup.exists()) {
            Log.info(c, METHOD, "Backup file doesn't exist... skipping recovery");
            return;
        }
        Log.info(c, METHOD, "Recovering Client: " + client.getClientName() + " from zip file: " + backup.getAbsolutePath());

        RemoteFile clientFolder = new RemoteFile(m, client.getClientRoot());
        if (!!!clientFolder.delete()) {
            Log.warning(c, "Unable to delete old clientFolder. Recovery failed!");
            // retry up to 5 seconds
            for (int i = 0; i < 5; i++) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    // try again
                }
                if (!!!clientFolder.delete()) {
                    Log.warning(c, "Unable to delete old clientFolder. Retry failed!");
                } else {
                    break;
                }
                // on the last try, mark the client with the failed cleanup.
                if (i == 4) {
                    Log.warning(c, "The clean client will not be restored.");
                    client.setClientCleanupProblem(true);
                    return;
                }
            }
        }

        String workDir = usrclientsDir.getAbsolutePath();
        String command = client.getMachineJavaJarCommandPath();
        String[] param = { "xf", backup.getAbsolutePath() };
        ProgramOutput o = m.execute(command, param, workDir);
        if (o.getReturnCode() == 0) {
            Log.info(c, METHOD, "Successfully recovered client: " + client.getClientName() + " from zip file: " + backup.getAbsolutePath());
        } else {
            Log.warning(c, "Recovery unjar process failed with return code " + o.getReturnCode());
            Log.warning(c, "Recovery unjar process failed with error " + o.getStderr());
            Log.warning(c, "Recovery unjar process failed with output " + o.getStdout());
        }
        client.setClientCleanupProblem(false);
    }

    /**
     * Checks if we should backup existing clients with the same name
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
     * @return the list of knownLibertyClients
     */
    public static Collection<LibertyClient> getKnownLibertyClients(String testClassName) {
        Log.finer(c, "getKnownLibertyClients", "Getting known liberty clients for test: " + testClassName);
        Set<LibertyClient> clients = new HashSet<LibertyClient>();
        synchronized (knownLibertyClients) {
            Set<LibertyClient> knownClients = knownLibertyClients.get(testClassName);
            if (knownClients != null)
                clients.addAll(knownClients);
            //could be that the client is known to a super type of this test
            //any clients defined (via getLibertyClient or getStartedLibertyClient) on a
            //super could be used by a test, so they qualify as known to the test
            try {
                Class<?> c = Class.forName(testClassName);
                Class<?> superClass = c.getSuperclass();
                while (superClass != null && !!!superClass.equals(Object.class)) {
                    Set<LibertyClient> superclients = knownLibertyClients.get(superClass.getName());
                    if (superclients != null)
                        clients.addAll(superclients);
                    superClass = superClass.getSuperclass();
                }
            } catch (ClassNotFoundException e) {
                Log.error(c, "getKnownLibertyClients", e);
            }
        }
        return clients;
    }

    /**
     * @return the list of FFDC logs
     */
    public static ArrayList<String> retrieveFFDCFile(LibertyClient client) throws Exception {
        return client.listFFDCFiles(client.getClientName());

    }

    /**
     * Hack to get the calling test class name from the stack.
     *
     * @param methodName the name of the method that is being called
     */
    private static String getCallerClassNameFromStack() {
        String thisClassName = LibertyClientFactory.class.getName();
        String previousElementClass = null;
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if (previousElementClass != null && previousElementClass.equals(thisClassName)) {
                //the last thing in the stack before the current element was LibertyClientFactory
                //if the current element is not also LibertyClientFactory then we found our caller
                String currentClassName = element.getClassName();
                if (!!!currentClassName.equals(thisClassName))
                    return currentClassName;
            }
            previousElementClass = element.getClassName();
        }
        return null;
    }
}
