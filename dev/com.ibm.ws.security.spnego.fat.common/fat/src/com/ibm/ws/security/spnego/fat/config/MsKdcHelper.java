/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.spnego.fat.config;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Map;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.junit.Ignore;

import com.ibm.websphere.simplicity.LocalFile;
import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

@Ignore("This is not a test")
public class MsKdcHelper extends KdcHelper {

    private final Class<?> thisClass = MsKdcHelper.class;

    private final String KRB5_DEFAULT_LOCATION = "/Windows/";

    public String customSpnName = null;

    protected MsKdcHelper(LibertyServer server) {

        this(server, InitClass.KDC_HOSTNAME, InitClass.KDC_USER, InitClass.KDC_USER_PWD, InitClass.KDC_REALM);
    }

    public MsKdcHelper(LibertyServer server, String kdcHost, String kdcUser, String kdcPassword, String kdcRealm) {
        super(server, kdcHost, kdcUser, kdcPassword, kdcRealm);

        krb5DefaultLocation = KRB5_DEFAULT_LOCATION;

        setDefaultUserName(null);
    }

    public MsKdcHelper(LibertyServer server, String kdcUser, String kdcPassword, String kdcRealm) {

        super(server, kdcUser, kdcPassword, kdcRealm);
        krb5DefaultLocation = KRB5_DEFAULT_LOCATION;

        setDefaultUserName(null);

    }

    /**
     * Sets the default name to be used for the user to be created or modified on the KDC.
     *
     * @param suffix Suffix to override {@link #userSuffix} with.
     */
    private void setDefaultUserName(String suffix) {
        String methodName = "setDefaultUserName";

        String userSuffix = (suffix != null) ? suffix : "_http";

        try {
            defaultUserName = InitClass.serverShortHostName + userSuffix;
            Log.info(thisClass, methodName, "Default user name set to: " + defaultUserName);
        } catch (Exception e) {
            Log.warning(thisClass, "Failed to set default user name: " + e.getMessage());
        }
    }

    /**
     * Performs the steps necessary to create a user, associate an SPN with the user, and create a keytab file on a
     * Microsoft KDC machine.
     *
     * @param spnRealm         - Realm name to use in the SPN that will be created. If null, no realm will be appended.
     * @param useCanonicalName - Boolean indicating whether the full canonical name of the host should be used for the SPN.
     * @param optionalCmdArgs  - Optional map of command line arguments and values to pass to the script to create the user and keytab.
     * @throws Exception
     */
    @Override
    public void createSpnAndKeytab(String spnRealm, boolean useCanonicalName, Map<String, String> optionalCmdArgs) throws Exception {
        String methodName = "createSpnAndKeytab";
        Log.info(thisClass, methodName, "Creating SPN and keytab with MS KDC");
        setDefaultUserName((!useCanonicalName) ? "_httpSN" : null); // Was _httpSHORTNAME, but samaccountname can only be 20 chars

        try {
            Log.info(thisClass, methodName, "Copying files needed for Kerberos");
            // TODO Why are we copying these to the server root?
            server.copyFileToLibertyServerRoot(SPNEGOConstants.KERBEROS, SPNEGOConstants.CREATE_WIN_USER_LOCAL_FILE);
            server.copyFileToLibertyServerRoot(SPNEGOConstants.KERBEROS, SPNEGOConstants.REMOTE_WIN_USER_LOCAL_FILE);
            server.copyFileToLibertyServerRoot(SPNEGOConstants.KERBEROS, SPNEGOConstants.CREATE_WIN_KEYTAB_LOCAL_FILE);
            server.copyFileToLibertyServerRoot(SPNEGOConstants.KERBEROS, SPNEGOConstants.LOCALHOST_KEYTAB_FILE);

            // Connect to MSKDC and push out vbs files to add/remove users
            Log.info(thisClass, methodName, "Connecting to KDC");
            Machine kdcMachine = getKdcMachine();
            if (InitClass.sendvbs) {
                Log.info(thisClass, methodName, "We need to push the vbs script files");
                pushVbsAndBatFilesToKDCMachine(kdcMachine);
            } else {
                Log.info(thisClass, methodName, "No need to push the vbs script files");
            }
            String canonicalHostName = InitClass.serverCanonicalHostName;
            String shortHostName = InitClass.serverShortHostName;
            String hostnameForSpn = canonicalHostName;
            String remoteKeytabPath = null;
            String keytabName = shortHostName + SPNEGOConstants.KRB5_KEYTAB_TEMP_SUFFIX;
            if (!useCanonicalName) {
                hostnameForSpn = shortHostName;
            }

            createUserAndKeytabFile(kdcMachine, hostnameForSpn, spnRealm, useCanonicalName, optionalCmdArgs);

            remoteKeytabPath = SPNEGOConstants.CYGWIN_HOME_REALM_1 + shortHostName + SPNEGOConstants.KRB5_KEYTAB_TEMP_SUFFIX;

            String localKeytabPath = server.getServerRoot() + SPNEGOConstants.KRB_RESOURCE_LOCATION + shortHostName + SPNEGOConstants.KRB5_KEYTAB_TEMP_SUFFIX;
            retrieveFile(kdcMachine, remoteKeytabPath, localKeytabPath);
            mergeKeytabFiles(keytabName);
            renameKeytabToDefaultName(keytabName);

            boolean hasConfigLocation = checkServerXMLForString(KRB5_CONFIG_ATTRIBUTE);
            Log.info(thisClass, methodName, KRB5_CONFIG_ATTRIBUTE + " found in server.xml: " + hasConfigLocation);
            boolean hasKeytabLocation = checkServerXMLForString(KRB5_KEYTAB_ATTRIBUTE);
            Log.info(thisClass, methodName, KRB5_KEYTAB_ATTRIBUTE + " found in server.xml: " + hasKeytabLocation);

            // Update all references to "keytabLocation" in the Kerberos config file to the appropriate keytab path
            String filePath = server.getServerRoot() + SPNEGOConstants.KRB_RESOURCE_LOCATION + SPNEGOConstants.KRB5_CONF_FILE;
            if (hasKeytabLocation) {
                // Keytab location is specified in the server config; use its value as the replacement string
                replaceStringInFile(filePath, "keytabLocation", keytabFilePath);
            } else {
                // No keytab location found in the server config; use the default keytab location for this operating system
                replaceStringInFile(filePath, "keytabLocation", krb5DefaultLocation + SPNEGOConstants.KRB5_KEYTAB_FILE);
            }

            // Copy the Kerberos config and keytab files to the default location if their locations are not given in the server config
            String confFile = (hasConfigLocation) ? null : SPNEGOConstants.KRB5_CONF_FILE;
            String keytabFile = (hasKeytabLocation) ? null : SPNEGOConstants.KRB5_KEYTAB_FILE;
            Log.info(thisClass, methodName, "The location for config file " + hasConfigLocation + " The location for keytab: " + hasKeytabLocation);
            Log.info(thisClass, methodName, "The value of the config file is: " + confFile + " The value of the keytab is: " + keytabFile);
            copyConfFilesToDefaultLocation(confFile, keytabFile);
            Log.info(thisClass, methodName, "Deleting the newly created keytab file from the KDC Machine: " + SPNEGOConstants.KRB5_KEYTAB_FILE);

            InitClass.sendvbs = false;

        } catch (Exception e) {
            Log.info(thisClass, methodName, "Got unexpected exception: " + CommonTest.maskHostnameAndPassword(e.getMessage()));
            throw (e);
        }

        Log.info(thisClass, methodName, "Finished creating SPN and keytab");
    }

    @Override
    public void createUserAndSpn(String user, Map<String, String> optionalCmdArgs, String... spns) throws Exception {
        String methodName = "createUserAndSpn";

        if (spns.length == 0) {
            throw new Exception("No SPNs provided to be set for user " + user + ". At least one SPN must be set for the user.");
        }

        String localScriptName = SPNEGOConstants.CREATE_WIN_USER_SET_SPN_LOCAL_FILE;
        String remoteScriptName = getUniqueRemoteFileName(SPNEGOConstants.CREATE_WIN_USER_SET_SPN_REMOTE_FILE);
        server.copyFileToLibertyServerRoot(SPNEGOConstants.KERBEROS, localScriptName);

        Log.info(thisClass, methodName, "Pushing to remote machine");
        if (InitClass.needToPushwinSetSPN) {
            Log.info(thisClass, methodName, "We need to push the create WinUserSetSPN script");
            pushLocalFileToRemoteMachine(getKdcMachine(), localScriptName, remoteScriptName);
        } else {
            Log.info(thisClass, methodName, "No need to push the create WinUserSetSPN script");
        }

        String kdcShortName = getKdcShortName();
        String createUserAndSpnCommand = "./" + remoteScriptName + " " + getUniqueRemoteFileName(SPNEGOConstants.REMOVE_WIN_USER_REMOTE_FILE) + " "
                                         + getUniqueRemoteFileName(SPNEGOConstants.CREATE_WIN_USER_REMOTE_FILE) + " " + user + " " + InitClass.USER_PWD + " HTTP " + spns[0] + " "
                                         + kdcShortName;

        if (optionalCmdArgs != null) {
            for (String argument : optionalCmdArgs.keySet()) {
                String value = optionalCmdArgs.get(argument);
                Log.info(thisClass, methodName, "Adding optional command argument " + argument + "=" + value);
                createUserAndSpnCommand += " " + argument + " " + value;
            }
        }

        Log.info(thisClass, methodName, "Create user and setting initial SPN on KDC using command");
        ProgramOutput output = executeRemoteCommand(remoteScriptName, createUserAndSpnCommand, true);

        if (output.getReturnCode() != 0) {
            throw new RemoteException("Creating user or setting SPN failed with return code.");
        }

        // Set all additional SPNs specified, if any
        for (int i = 1; i < spns.length; i++) {
            String spn = spns[i];
            setSpnForUser(user, spn);
        }
        InitClass.needToPushwinSetSPN = false;
    }

    @Override
    public void setSpnForUser(String user, String spnHost) throws Exception {
        String methodName = "setSpnForUser";
        Log.info(thisClass, methodName, "Setting SPN: " + spnHost + " for user: " + user);

        String localScriptName = SPNEGOConstants.SET_USER_SPN_LOCAL_FILE;
        String remoteScriptName = getUniqueRemoteFileName(SPNEGOConstants.SET_USER_SPN_REMOTE_FILE);
        server.copyFileToLibertyServerRoot(SPNEGOConstants.KERBEROS, localScriptName);
        if (InitClass.needToPushsetUserSPN) {
            Log.info(thisClass, methodName, "We need to push the setUserSPN script to the KDC");
            pushLocalFileToRemoteMachine(getKdcMachine(), localScriptName, remoteScriptName);
        } else {
            Log.info(thisClass, methodName, "We don't need to push the setUserSPN script to the KDC");
        }

        String setUserSpnCommand = "./" + remoteScriptName + " " + user + " HTTP " + spnHost;
        ProgramOutput output = executeRemoteCommand(remoteScriptName, setUserSpnCommand, true);

        if (output.getReturnCode() != 0) {
            throw new RemoteException("Setting SPN for user failed with return code " + output.getReturnCode());
        }
        InitClass.needToPushsetUserSPN = false;
    }

    /**
     * Deletes the default user and associated SPN(s).
     *
     * @throws Exception
     */
    @Override
    public void deleteUser() throws Exception {
        deleteUser(getDefaultUserName());
    }

    @Override
    public void deleteUser(String user) throws Exception {
        String methodName = "deleteUser";

        String kdcShortName = getKdcShortName();

        String deleteUserCommand = "cscript " + getUniqueRemoteFileName(SPNEGOConstants.REMOVE_WIN_USER_REMOTE_FILE) + " -user " + user + " -host " + kdcShortName;
        String deleteUserCommandMasked = "cscript " + getUniqueRemoteFileName(SPNEGOConstants.REMOVE_WIN_USER_REMOTE_FILE) + " -user " + user + " -host " + "kdcShortName";
        Log.info(thisClass, methodName, "Deleting user on KDC using command: " + deleteUserCommandMasked);
        ProgramOutput output = executeRemoteCommand(null, deleteUserCommand, true);
        if (output.getReturnCode() != 0) {
            throw new RemoteException("Deleting user failed...");
        }
    }

    @Override
    public void deleteSpnForUser(String user, String spn) throws Exception {
        String methodName = "deleteSpnForUser";
        Log.info(thisClass, methodName, "Deleting SPN: " + "spn" + " for user: " + user);

        String localScriptName = SPNEGOConstants.DELETE_USER_SPN_LOCAL_FILE;
        String remoteScriptName = getUniqueRemoteFileName(SPNEGOConstants.DELETE_USER_SPN_REMOTE_FILE);
        server.copyFileToLibertyServerRoot(SPNEGOConstants.KERBEROS, localScriptName);

        if (InitClass.needToPushdeleteUserSPN) {
            Log.info(thisClass, methodName, "We need to push the delete user spn script to the KDC");
            pushLocalFileToRemoteMachine(getKdcMachine(), localScriptName, remoteScriptName);
        } else {
            Log.info(thisClass, methodName, "We don't need to push the delete user spn script to the KDC");
        }

        String setUserSpnCommand = "./" + remoteScriptName + " " + user + " HTTP " + spn;
        ProgramOutput output = executeRemoteCommand(remoteScriptName, setUserSpnCommand, true);

        if (output.getReturnCode() != 0) {
            throw new RemoteException("Deleting SPN for user failed with return code " + output.getReturnCode());
        }
        InitClass.needToPushdeleteUserSPN = false;
    }

    @Override
    public void addSpnToKeytab(String user, String spn) throws Exception {
        String methodName = "addSpnToKeytab";
        Log.info(thisClass, methodName, "Adding SPN: " + "spn" + " for user: " + user + " to existing keytab file");

        String localScriptName = SPNEGOConstants.ADD_SPN_TO_KEYTAB_LOCAL_FILE;
        String remoteScriptName = getUniqueRemoteFileName(SPNEGOConstants.ADD_SPN_TO_KEYTAB_REMOTE_FILE);
        server.copyFileToLibertyServerRoot(SPNEGOConstants.KERBEROS, localScriptName);
        if (InitClass.needToPushaddSPNKeytab) {
            Log.info(thisClass, methodName, "We need to push the addSPNKeytab script to the KDC");
            pushLocalFileToRemoteMachine(getKdcMachine(), localScriptName, remoteScriptName);

        } else {
            Log.info(thisClass, methodName, "We don't need to push the addSPNKeytab script to the KDC");
        }
        String shortHostName = InitClass.serverShortHostName;

        String addSpnToKeytabCommand = "./" + remoteScriptName + " " + user + " " + InitClass.USER_PWD + " HTTP " + spn + " "
                                       + shortHostName + SPNEGOConstants.KRB5_KEYTAB_TEMP_SUFFIX + " " + getKdcRealm();
        ProgramOutput output = executeRemoteCommand(remoteScriptName, addSpnToKeytabCommand, true);
        InitClass.needToPushaddSPNKeytab = false;

        if (output.getReturnCode() != 0) {
            throw new RemoteException("Adding SPN to keytab failed with return code " + output.getReturnCode());
        }

        // Retrieve the new keytab file from the KDC machine
        String remoteKeytabPath = SPNEGOConstants.CYGWIN_HOME_REALM_1 + shortHostName + SPNEGOConstants.KRB5_KEYTAB_TEMP_SUFFIX;
        String localKeytabPath = server.getServerRoot() + SPNEGOConstants.KRB_RESOURCE_LOCATION + shortHostName + SPNEGOConstants.KRB5_KEYTAB_TEMP_SUFFIX;
        Machine kdcMachine = getKdcMachine();
        retrieveFile(kdcMachine, remoteKeytabPath, localKeytabPath);
        renameKeytabToDefaultName(shortHostName + SPNEGOConstants.KRB5_KEYTAB_TEMP_SUFFIX);
    }

    /**
     * 4/19/20017 We don't need to push out the scripts everytime we run the spnego bucket.
     * If changes are made to the scripts then the scripts need to be added manually.
     * Push the .vbs and .bat scripts that run through user and keytab creation to the KDC.
     *
     * @param kdcMachine
     * @throws Exception
     */
    private void pushVbsAndBatFilesToKDCMachine(Machine kdcMachine) throws Exception {
        pushLocalFileToRemoteMachine(kdcMachine, SPNEGOConstants.CREATE_WIN_USER_LOCAL_FILE, getUniqueRemoteFileName(SPNEGOConstants.CREATE_WIN_USER_REMOTE_FILE));
        pushLocalFileToRemoteMachine(kdcMachine, SPNEGOConstants.REMOTE_WIN_USER_LOCAL_FILE, getUniqueRemoteFileName(SPNEGOConstants.REMOVE_WIN_USER_REMOTE_FILE));
        pushLocalFileToRemoteMachine(kdcMachine, SPNEGOConstants.CREATE_WIN_KEYTAB_LOCAL_FILE, getUniqueRemoteFileName(SPNEGOConstants.CREATE_WIN_KEYTAB_REMOTE_FILE));
        pushLocalFileToRemoteMachine(kdcMachine, SPNEGOConstants.KRB5_LOCAL_KEYTAB_LOCAL_FILE, SPNEGOConstants.KRB5_LOCAL_KEYTAB_REMOTE_FILE);
    }

    /**
     * 4/19/2017 We don't need to push out the scripts everytime we run the spnego bucket.
     * If changes are made to the scripts then the scripts need to be added manually.
     * Pushes the local file within the server root to the remote file location within the Cygwin home directory of the
     * specified machine.
     *
     * @param kdcMachine
     * @param localFile
     * @param remoteFile
     * @throws Exception
     */
    private void pushLocalFileToRemoteMachine(Machine remoteMachine, String localFile, String remoteFile) throws Exception {
        LocalFile lFile = new LocalFile(server.getServerRoot() + localFile);
        RemoteFile rFile = new RemoteFile(remoteMachine, SPNEGOConstants.CYGWIN_HOME_REALM_1 + remoteFile);
        copyLocalFileToRemoteFile(lFile, rFile);
    }

    /**
     * Uses an existing .bat file to create a user on the Active Directory machine, sets the SPN for the user, and
     * creates an appropriate keytab file.
     *
     * @param kdcMachine
     * @param canonicalHostName
     * @param spnRealm          - Realm name to use in the SPN that will be created. If null, no realm will be appended.
     * @param useCanonicalName  - Boolean indicating whether or not the canonical hostname should be used when
     *                              creating the SPN for the user.
     * @param optionalCmdArgs   - Optional map of command line arguments and values to pass to the script to create the user and keytab.
     * @throws Exception
     */
    private void createUserAndKeytabFile(Machine kdcMachine, String canonicalHostName, String spnRealm, boolean useCanonicalName,
                                         Map<String, String> optionalCmdArgs) throws Exception {
        String methodName = "createUserAndKeytabFile";

        String kdcShortName = getKdcShortName();
        String shortHostName = InitClass.getShortHostName(canonicalHostName, false);

        Log.info(thisClass, methodName, "shortHostName: " + shortHostName);

        String nameForSpn = canonicalHostName;
        if (!useCanonicalName) {
            nameForSpn = InitClass.serverShortHostName;
        }
        if (spnRealm != null && !spnRealm.isEmpty()) {
            nameForSpn = nameForSpn + "@" + spnRealm;
        }
        if (customSpnName != null) {
            nameForSpn = customSpnName;
        }

        String scriptFile = getUniqueRemoteFileName(SPNEGOConstants.CREATE_WIN_KEYTAB_REMOTE_FILE);
        String createWinKeyTabFileCommand = "./" + scriptFile + " " + getUniqueRemoteFileName(SPNEGOConstants.REMOVE_WIN_USER_REMOTE_FILE)
                                            + " " + getUniqueRemoteFileName(SPNEGOConstants.CREATE_WIN_USER_REMOTE_FILE) + " "
                                            + getDefaultUserName() + " " + InitClass.USER_PWD + " HTTP " + nameForSpn + " "
                                            + InitClass.serverShortHostName + SPNEGOConstants.KRB5_KEYTAB_TEMP_SUFFIX + " " + getKdcRealm() + " " + kdcShortName;

        if (optionalCmdArgs != null) {
            for (String argument : optionalCmdArgs.keySet()) {
                String value = optionalCmdArgs.get(argument);
                Log.info(thisClass, methodName, "Adding optional command argument " + argument + "=" + value);
                createWinKeyTabFileCommand += " " + argument + " " + value;
            }
        }

        Log.info(thisClass, methodName, "Creating user and keytab on KDC using command.");
        ProgramOutput output = executeRemoteCommand(scriptFile, createWinKeyTabFileCommand, true);
        if (output.getStdout().isEmpty() && output.getStderr().isEmpty()) {
            Log.info(thisClass, methodName, "The command did not ran successfully, trying to re-run 2 more times.");
            for (int i = 0; i < 2; i++) {
                if (output.getStdout().isEmpty() && output.getStderr().isEmpty()) {
                    output = executeRemoteCommand(scriptFile, createWinKeyTabFileCommand, true);
                }
            }
        }
        if (output.getStdout().isEmpty() && output.getStderr().isEmpty()) {
            Log.info(thisClass, methodName, "The command did not run successfully, Expect failures.");
        }

        if (output.getReturnCode() != 0) {
            throw new RemoteException("Creating SPN or keytab failed.");
        }
    }

    /**
     * Executes a remote command on the KDC host.
     *
     * @param scriptFile       - Expected script file to be used in the command. If not null, the permissions of this file
     *                             are set to 777.
     * @param command
     * @param retryUponFailure - Should the command be tried again if the initial execution failed.
     * @return
     * @throws Exception
     */
    private ProgramOutput executeRemoteCommand(String scriptFile, String command, boolean retryUponFailure) throws Exception {
        String methodName = "executeRemoteCommand";
        Log.info(thisClass, methodName, "Connecting to KDC");
        Machine kdcMachine = getKdcMachine();

        Log.info(thisClass, methodName, "The kdc machine: " + InitClass.getKDCHostnameMask(kdcMachine.getHostname()) + "is connected: " + kdcMachine.isConnected());

        ProgramOutput output = null;
        SshClient sshClient = getSshClient();
        try {
            ClientSession sshSession = getSshSession(sshClient, kdcMachine);
            try {

                String chmodCMD = "chmod 777 " + scriptFile;
                if (scriptFile != null && (((scriptFile == getUniqueRemoteFileName(SPNEGOConstants.ADD_SPN_TO_KEYTAB_REMOTE_FILE)) && InitClass.needToPushaddSPNKeytab)) ||
                    ((scriptFile == getUniqueRemoteFileName(SPNEGOConstants.DELETE_USER_SPN_REMOTE_FILE)) && InitClass.needToPushdeleteUserSPN) ||
                    ((scriptFile == getUniqueRemoteFileName(SPNEGOConstants.SET_USER_SPN_REMOTE_FILE)) && InitClass.needToPushsetUserSPN) ||
                    ((scriptFile == getUniqueRemoteFileName(SPNEGOConstants.CREATE_WIN_USER_SET_SPN_REMOTE_FILE)) && InitClass.needToPushwinSetSPN) ||
                    ((scriptFile == getUniqueRemoteFileName(SPNEGOConstants.CREATE_WIN_KEYTAB_REMOTE_FILE)) && InitClass.sendvbs) ||
                    ((scriptFile == getUniqueRemoteFileName(SPNEGOConstants.REMOVE_WIN_USER_REMOTE_FILE)) && InitClass.sendvbs)) {

                    Log.info(thisClass, methodName, "Call chmod 777 on script using command: " + chmodCMD);
                    output = executeSshCommand(sshSession, chmodCMD, 30);
                } else {
                    Log.info(thisClass, methodName, "The following command will not be run for: " + chmodCMD + " for the script file: " + scriptFile);
                }

                Log.info(thisClass, methodName, "Executing command --> " + command);
                boolean failed = false;
                try {
                    output = executeSshCommand(sshSession, command, 120);
                    failed = output.getReturnCode() != 0;
                } catch (IOException e) {
                    if (!retryUponFailure) {
                        throw e;
                    }

                    /*
                     * This will cause timeouts to retry, but that should be pretty unusual.
                     */
                    failed = true;
                }

                if (retryUponFailure && failed) {
                    Log.info(thisClass, methodName, "Remote command failed with return code " + output.getReturnCode());
                    Log.info(thisClass, methodName, "Sleeping for a few seconds to see if that helps resolve any problems encountered");
                    Thread.sleep(15 * 1000);
                    Log.info(thisClass, methodName, "Attempting to retry the command");
                    output = executeRemoteCommand(scriptFile, command, false);
                }
            } finally {
                sshSession.close();
            }
        } finally {
            sshClient.stop();
        }
        return output;
    }

    @Override
    public void teardown() {
        final String methodName = "teardown()";

        /*
         * Delete the default user.
         */
        try {
            deleteUser();
        } catch (Exception e) {
            Log.error(thisClass, methodName, e, "Error deleting the user " + defaultUserName + " from the KDC on teardown.");
        }

        /*
         * Delete the krb5.keytab.
         *
         * TODO Seems this file name could / should be unique.
         */
        try {
            deleteRemoteFileFromRemoteMachine(getKdcMachine(), SPNEGOConstants.KRB5_KEYTAB_FILE);
        } catch (Exception e) {
            Log.error(thisClass, methodName, e, "Error deleting the file " + SPNEGOConstants.KRB5_KEYTAB_FILE + " from the KDC on teardown.");
        }

        /*
         * Delete the scripts that were copied over to the KDC.
         */
        if (!InitClass.needToPushaddSPNKeytab) {
            String file = getUniqueRemoteFileName(SPNEGOConstants.ADD_SPN_TO_KEYTAB_REMOTE_FILE);
            try {
                deleteRemoteFileFromRemoteMachine(getKdcMachine(), file);
            } catch (Exception e) {
                Log.error(thisClass, methodName, e, "Error deleting the file " + file + " from the KDC on teardown.");
            }
        }
        if (!InitClass.sendvbs) {
            String file = getUniqueRemoteFileName(SPNEGOConstants.CREATE_WIN_KEYTAB_REMOTE_FILE);
            try {
                deleteRemoteFileFromRemoteMachine(getKdcMachine(), file);
            } catch (Exception e) {
                Log.error(thisClass, methodName, e, "Error deleting the file " + file + " from the KDC on teardown.");
            }

            file = getUniqueRemoteFileName(SPNEGOConstants.CREATE_WIN_USER_REMOTE_FILE);
            try {
                deleteRemoteFileFromRemoteMachine(getKdcMachine(), file);
            } catch (Exception e) {
                Log.error(thisClass, methodName, e, "Error deleting the file " + file + " from the KDC on teardown.");
            }

            file = getUniqueRemoteFileName(SPNEGOConstants.REMOVE_WIN_USER_REMOTE_FILE);
            try {
                deleteRemoteFileFromRemoteMachine(getKdcMachine(), file);
            } catch (Exception e) {
                Log.error(thisClass, methodName, e, "Error deleting the file " + file + " from the KDC on teardown.");
            }
        }
        if (!InitClass.needToPushwinSetSPN) {
            String file = getUniqueRemoteFileName(SPNEGOConstants.CREATE_WIN_USER_SET_SPN_REMOTE_FILE);
            try {
                deleteRemoteFileFromRemoteMachine(getKdcMachine(), file);
            } catch (Exception e) {
                Log.error(thisClass, methodName, e, "Error deleting the file " + file + " from the KDC on teardown.");
            }
        }
        if (!InitClass.needToPushdeleteUserSPN) {
            String file = getUniqueRemoteFileName(SPNEGOConstants.DELETE_USER_SPN_REMOTE_FILE);
            try {
                deleteRemoteFileFromRemoteMachine(getKdcMachine(), file);
            } catch (Exception e) {
                Log.error(thisClass, methodName, e, "Error deleting the file " + file + " from the KDC on teardown.");
            }
        }
        if (!InitClass.needToPushsetUserSPN) {
            String file = getUniqueRemoteFileName(SPNEGOConstants.SET_USER_SPN_REMOTE_FILE);
            try {
                deleteRemoteFileFromRemoteMachine(getKdcMachine(), file);
            } catch (Exception e) {
                Log.error(thisClass, methodName, e, "Error deleting the file " + file + " from the KDC on teardown.");
            }
        }

        /*
         * Don't EVER delete the localhost_HTTP_krb5.keytab from the remote machine. There are other changes
         * made to stop the test from getting this far if the short host name comes back as localhost,
         * but prevent it just in case.
         *
         * TODO Seems we could use the getUniqueRemoteFileName for this file as well.
         */
        if (!"localhost".equalsIgnoreCase(InitClass.serverShortHostName)) {
            String file = InitClass.serverShortHostName + SPNEGOConstants.KRB5_KEYTAB_TEMP_SUFFIX;
            try {
                deleteRemoteFileFromRemoteMachine(getKdcMachine(), file);
            } catch (Exception e) {
                Log.error(thisClass, methodName, e, "Error deleting the file " + file + " from the KDC on teardown.");
            }
        }
    }

    /**
     * Gets a unique remote file name based on the local short host name. The file name is unique for this host,
     * and should help avoid conflicts when pushing files to the KDC host.
     *
     * @param remoteFileName The base remote file name.
     * @return A unique filename for the specified remote file name. The name should be consistent
     *         for this host and the duration of the test.
     */
    private String getUniqueRemoteFileName(String remoteFileName) {
        return InitClass.serverShortHostName + "_" + remoteFileName;
    }
}
