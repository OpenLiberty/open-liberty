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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.scp.ScpClient;
import org.apache.sshd.client.scp.ScpClientCreator;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.channel.Channel;
import org.apache.sshd.common.channel.ChannelListener;
import org.junit.Ignore;

import com.ibm.websphere.simplicity.ConnectionInfo;
import com.ibm.websphere.simplicity.LocalFile;
import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

@Ignore("This is not a test")
public abstract class KdcHelper {

    public final static Class<?> thisClass = KdcHelper.class;

    /**
     * Active Directory appears to allow user names with a max of 20 characters. We add "_http" to the end of the host name to
     * create the user name, so the host name we use for the user name must be 15 characters or less.
     */
    public static final int CANONICAL_HOST_NAME_CHAR_LIMIT = 15;

    private String kdcHost = null;
    private String kdcUser = null;
    private String kdcPassword = null;
    private String kdcRealm = null;

    protected boolean createWinNT = false;
    protected boolean copiedAtDefault = false;
    protected boolean waskrbiniCreatedAtDefault = false;
    protected boolean waskeyTabCreatedAtDefault = false;
    protected boolean copiedAtWindowsDir = false;
    protected boolean copiedkrbiniAtWindowsDir = false;
    protected boolean copiedkeyTabAtWindowsDir = false;

    protected String keytabFilePath = null;
    protected String krb5DefaultLocation = null;
    protected String defaultUserName = null;
    protected String kdcHostName = null;
    protected Machine kdcMachine = null;

    protected final String KRB5_CONFIG_ATTRIBUTE = "krb5Config";
    protected final String KRB5_KEYTAB_ATTRIBUTE = "krb5Keytab";

    protected OperatingSystem serverOS = null;
    public LibertyServer server;

    protected KdcHelper(LibertyServer server) {
        String methodName = "constructor";
        this.server = server;

        try {
            serverOS = server.getMachine().getOperatingSystem();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.info(thisClass, methodName, "OS for target system is " + serverOS.toString());
    }

    protected KdcHelper(LibertyServer server, String kdcHost, String kdcUser, String kdcPassword, String kdcRealm) {
        this(server);
        this.kdcHost = kdcHost;
        this.kdcUser = kdcUser;
        this.kdcPassword = kdcPassword;
        this.kdcRealm = kdcRealm;
    }

    protected KdcHelper(LibertyServer server, String kdcUser, String kdcPassword, String kdcRealm) {
        this(server);
        this.kdcHost = InitClass.KDC_HOSTNAME;
        this.kdcPassword = kdcPassword;
        this.kdcUser = kdcUser;
        this.kdcRealm = kdcRealm;
    }

    /**
     * Creates a default user, sets a corresponding SPN, and creates the keytab file for the appropriate KDC host.
     *
     * @param useCanonicalName - Boolean indicating whether the full canonical name of the host should be used for the SPN.
     * @throws Exception
     */
    public void createSpnAndKeytab(boolean useCanonicalName) throws Exception {
        createSpnAndKeytab(SPNEGOConstants.DEFAULT_REALM, useCanonicalName, null);
    }

    /**
     * Creates a default user, sets a corresponding SPN, and creates the keytab file for the appropriate KDC host.
     *
     * @param spnRealm         - Realm name to use in the SPN that will be created. If null, no realm will be appended to the SPN.
     * @param useCanonicalName - Boolean indicating whether the full canonical name of the host should be used for the SPN.
     * @param optionalCmdArgs  - Optional map of command line arguments and values to pass to the script to create the user and keytab.
     * @throws Exception
     */
    public abstract void createSpnAndKeytab(String spnRealm, boolean useCanonicalName, Map<String, String> optionalCmdArgs) throws Exception;

    /**
     * Creates the specified user on the KDC machine and sets the given SPN(s) for the user. The SPN for the user created
     * will not be added to any keytab file.
     *
     * @param user
     * @param optionalCmdArgs
     * @param spns
     * @throws Exception
     */
    public abstract void createUserAndSpn(String user, Map<String, String> optionalCmdArgs, String... spns) throws Exception;

    /**
     * Sets the provided SPN for the given user. This method expects the user to exist on the default KDC machine.
     *
     * @param user
     * @param spnHost
     * @throws Exception
     */
    public abstract void setSpnForUser(String user, String spnHost) throws Exception;

    /**
     * Deletes the default user and associated SPN.
     *
     * @throws Exception
     */
    public abstract void deleteUser() throws Exception;

    /**
     * Deletes the specified user.
     *
     * @param user
     * @throws Exception
     */
    public abstract void deleteUser(String user) throws Exception;

    /**
     * Deletes the provided SPN for the given user.
     *
     * @param user
     * @param spn
     * @throws Exception
     */
    public abstract void deleteSpnForUser(String user, String spn) throws Exception;

    /**
     * Adds the given SPN for the specified user to the keytab file.
     *
     * @param user
     * @param spn
     * @throws Exception
     */
    public abstract void addSpnToKeytab(String user, String spn) throws Exception;

    /**
     * Cleanup any test artifacts on the KDC.
     *
     * @throws Exception If there was an issue cleaning files.
     */
    public abstract void teardown() throws Exception;

    public String getKdcHost() {
        return kdcHost;
    }

    public String getKdcUser() {
        return kdcUser;
    }

    public String getKdcPassword() {
        return kdcPassword;
    }

    public String getKdcRealm() {
        return kdcRealm;
    }

    public String getDefaultUserName() {
        return defaultUserName;
    }

    /**
     * Gets the Machine representation of the KDC host.
     *
     * @return
     * @throws Exception
     */
    public Machine getKdcMachine() throws Exception {
        if (kdcMachine == null) {
            Log.info(thisClass, "getKdcMachine", "Getting KDC....");
            ConnectionInfo connInfo = new ConnectionInfo(kdcHost, kdcUser, kdcPassword);
            kdcMachine = Machine.getMachine(connInfo);
            return kdcMachine;
        }
        return kdcMachine;
    }

    public String getKdcHostName() throws Exception {
        if (kdcHostName == null) {
            return getKdcMachine().getHostname();
        }
        return kdcHostName;
    }

    /**
     * Returns the short name of the KDC machine. The short name is equivalent to the segment of the full KDC host name up
     * to the first '.' character, if one is present. Otherwise, the short name is equivalent to the full KDC host name.
     *
     * @return
     * @throws Exception
     */
    public String getKdcShortName() throws Exception {
        String hostName = getKdcHostName();
        if (hostName.indexOf(".") != -1) {
            return hostName.substring(0, hostName.indexOf("."));
        }
        return hostName;
    }

    /**
     * Replaces all occurrences of a string within a file with a new string. The string to be replaced will be used as
     * a regular expression, so regex special characters should be escaped appropriately.
     *
     * @param filePath
     * @param origStrRegex - Regular expression representing the value to be replaced.
     * @param newStr       - The new string value that will replace each occurrence of origStrRegex that is found.
     * @throws Exception
     */
    public void replaceStringInFile(String filePath, String origStrRegex, String newStr) throws Exception {
        String methodName = "replaceStringInFile";
        try {
            Log.info(thisClass, methodName, "Replacing: [" + origStrRegex + "] with newStr: [" + newStr + "] in file: " + filePath);
            File inp = new File(filePath);
            InputStreamReader inputStream = new InputStreamReader(new FileInputStream(inp));
            BufferedReader dataStream = new BufferedReader(inputStream);

            Vector<String> vec = new Vector<String>(200, 200);
            String line = null;
            boolean substitutionMade = false;
            while ((line = dataStream.readLine()) != null) {
                String strTmp = line;
                line = line.replaceAll(origStrRegex, newStr);
                if (!strTmp.equals(line)) {
                    substitutionMade = true;
                    Log.info(thisClass, methodName, "before:" + strTmp);
                    Log.info(thisClass, methodName, "after :" + line);
                }
                vec.addElement(line);
            }

            dataStream.close();

            if (substitutionMade) {
                OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(inp));
                PrintWriter ps = new PrintWriter(osw, true);

                for (int i = 0; i < vec.size(); i++) {
                    line = vec.elementAt(i);
                    ps.println(line);
                }
                ps.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Copies a file on the specified KDC machine to the path specified on the local machine.
     *
     * @param kdcMachine
     * @param remoteFileName
     * @param copyToFileName
     * @throws Exception
     */
    protected void retrieveFile(Machine kdcMachine, String remoteFileName, String copyToFileName) throws Exception {
        String methodName = "retrieveFile";

        SshClient sshClient = getSshClient();
        try {
            try (ClientSession sshSession = getSshSession(sshClient, kdcMachine)) {

                if (remoteFileExists(sshSession, remoteFileName)) {
                    Log.info(thisClass, methodName, "No retrieval was performed because the remote file provided does not exist: " + remoteFileName);
                    return;
                }

                Log.info(thisClass, methodName, "Local file: " + copyToFileName + " remote file: " + remoteFileName);

                //        boolean result = RXAProvider.copy(rFile, lFile, true);
                boolean result = copyFromRemoteFile(sshSession, remoteFileName, copyToFileName);

                Log.info(thisClass, methodName, "Copy remote file to local file result: " + result);
            }
        } finally {
            sshClient.stop();
        }
    }

    /**
     * Performs a keytab merge command (<code>ktab -m</code>) to merge the specified keytab file with the localhost
     * keytab file in the server root directory.
     *
     * @param keytabFileName - Name of the keytab file under the Kerberos resource location.
     */
    protected void mergeKeytabFiles(String keytabFileName) {
        mergeKeytabFiles(SPNEGOConstants.LOCALHOST_KEYTAB_FILE, SPNEGOConstants.KRB_RESOURCE_LOCATION + keytabFileName);
    }

    /**
     * Performs a keytab merge command (<code>ktab -m</code>) to merge the source keytab file with the destination
     * keytab file. Both file paths are relative to the server root directory.
     *
     * @param srcKeytabFileName
     * @param dstKeytabFileName
     */
    public void mergeKeytabFiles(String srcKeytabFileName, String dstKeytabFileName) {
        String methodName = "mergeKeytabFiles";
        String javaHome = System.getProperty("java.home");
        Log.info(thisClass, methodName, "JAVA HOME is " + javaHome);

        try {
            LocalFile sourceKeytab = new LocalFile(server.getServerRoot() + srcKeytabFileName);
            LocalFile destinationKeytab = new LocalFile(server.getServerRoot() + dstKeytabFileName);
            if (InitClass.IBM_JDK_V8_LOWER) {
                String ktabCommand = javaHome + "/bin/java com.ibm.security.krb5.internal.tools.Ktab -m " + sourceKeytab + " " + destinationKeytab;
                Log.info(thisClass, methodName, "Issuing ktab command: " + ktabCommand);
                Machine localMachine = server.getMachine();
                ProgramOutput output = localMachine.execute(ktabCommand);

                Log.info(thisClass, methodName, "ktab merge command.");
                if (output.getReturnCode() != 0) {
                    throw new RemoteException("Keytab merge failed.");
                }
            }
            Log.info(thisClass, methodName, "Merging keytab files " + destinationKeytab + " and " + sourceKeytab);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Renames the specified keytab file within the Kerberos resource location to the default keytab file name.
     *
     * @param currentKeytabFileName
     * @throws Exception
     */
    protected void renameKeytabToDefaultName(String currentKeytabFileName) throws Exception {
        String methodName = "renameKeytabToDefaultName";
        Log.info(thisClass, methodName, "Renaming keytab file to " + SPNEGOConstants.KRB5_KEYTAB_FILE);
        try {
            LocalFile localFile = new LocalFile(server.getServerRoot() + SPNEGOConstants.KRB_RESOURCE_LOCATION + SPNEGOConstants.KRB5_KEYTAB_FILE);
            if (localFile.exists()) {
                Log.info(thisClass, methodName, "Deleting existing keytab file in the server root: " + localFile.getAbsolutePath());
                boolean deleteSucceeded = localFile.delete();
                Log.info(thisClass, methodName, "Deletion operation succeeded: " + deleteSucceeded);
            }
            String oldFileName = SPNEGOConstants.KRB_RESOURCE_LOCATION + currentKeytabFileName;
            String newFileName = SPNEGOConstants.KRB_RESOURCE_LOCATION + SPNEGOConstants.KRB5_KEYTAB_FILE;
            server.renameLibertyServerRootFile(oldFileName, newFileName);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Copies the specified local file to the remote file destination, overwriting the remote file if it already exists.
     *
     * @param localFile
     * @param remoteFile
     * @throws Exception
     */
    protected void copyLocalFileToRemoteFile(LocalFile localFile, RemoteFile remoteFile) throws Exception {
        String methodName = "copyLocalFileToRemoteFile";
        Log.info(thisClass, methodName, "Local file: " + localFile.toString() + " remote file: " + remoteFile.getMachine().getHostname() + ":" + remoteFile.toString());
        boolean result = true;

        for (int i = 1; i <= 5; i++) {
            try {
//                result = localFile.copyToDest(remoteFile);

                SshClient sshClient = getSshClient();
                try (ClientSession sshSession = getSshSession(sshClient, remoteFile.getMachine())) {
                    result = copyLocalFileToRemote(sshSession, localFile.getAbsolutePath(), remoteFile.getAbsolutePath());
                } finally {
                    sshClient.stop();
                }
                break;
            } catch (ConnectException ce) {
                Log.info(thisClass, methodName, "Failed to copy local file to remote file due to: " + CommonTest.maskHostnameAndPassword(ce.getMessage()));
                Log.info(thisClass, methodName, "Sleeping for 5 seconds and then will attempt to copy the file again. Try " + (i + 1) + " of 5.");
                Thread.sleep(5 * 1000);
            } catch (NumberFormatException nf) {
                Log.info(thisClass, methodName, "A NumberFormatException, this does not affect test performance, ignore it...");
                break;
            } catch (Exception e) {
                Log.error(thisClass, methodName, e, "Exception thrown: " + e);
                result = false;
                break;
            }
        }

        Log.info(thisClass, methodName, "Copy file to remote host result: " + result);
    }

    /**
     * Copies the specified local file to the remote file destination, overwriting the remote file if it already exists.
     *
     * @param remoteFile
     * @throws Exception
     */
    protected void removeRemoteFileFromRemoteMachine(RemoteFile remoteFile) throws Exception {
        String methodName = "removeRemoteFileFromRemoteMachine";
        Log.info(thisClass, methodName, "Remote File: " + remoteFile.toString());
        boolean result = true;
        SshClient sshClient = getSshClient();
        try {
//            result = remoteFile.delete();

            try (ClientSession sshSession = getSshSession(sshClient, remoteFile.getMachine())) {
                result = deleteRemoteFile(sshSession, remoteFile.getAbsolutePath());
            }
        } catch (IOException ce) {
            Log.info(thisClass, methodName, "Failed to delete remote file to remote file due to: " + CommonTest.maskHostnameAndPassword(ce.getMessage()));
            Log.info(thisClass, methodName, "Sleeping for 5 seconds and then will attempt to delete the file again");
            Thread.sleep(5 * 1000);
//            result = remoteFile.delete();

            try (ClientSession sshSession = getSshSession(sshClient, remoteFile.getMachine())) {
                result = deleteRemoteFile(sshSession, remoteFile.getAbsolutePath());
            }

        } finally {
            sshClient.stop();
        }

        Log.info(thisClass, methodName, "Remove file from remote host result: " + result);
    }

    /**
     * Copies the Kerberos configuration and keytab files from the Kerberos resource location within the server root
     * to the KDC-dependent default location. Specifying null for either argument will result in no move operation
     * being done on that file.
     *
     * @param confFile
     * @param keytabFile
     * @throws Exception
     */
    public void copyConfFilesToDefaultLocation(String confFile, String keytabFile) throws Exception {
        String methodName = "copyConfFilesToDefaultLocation";
        try {

            if (confFile != null) {
                Log.info(thisClass, methodName, "Kerberos configuration file will be copied to default location");
                RemoteFile confFilePath = server.getFileFromLibertyServerRoot(SPNEGOConstants.KRB_RESOURCE_LOCATION.substring(1) + confFile);
                String defaultConfFileName = SPNEGOConstants.KRB5_CONF_FILE;
                //If and only if the OS is Windows, we change the file name to be the krb5.ini
                if (serverOS.toString().equals("WINDOWS")) {
                    defaultConfFileName = SPNEGOConstants.KRB5_INI_FILE;
                }
                //We now proceed to try and copy the krb conf file at the default location.
                LocalFile confDefaultFilePath = new LocalFile(krb5DefaultLocation + defaultConfFileName);
                copiedAtDefault = copyKrbFilestoDefaultLocation(defaultConfFileName, confFilePath, confDefaultFilePath);
                Log.info(thisClass, methodName, "Kerberos configuration file move result: " + copiedAtDefault);
                //We confirm that the file we tried to copy was the krb5.ini.
                if (defaultConfFileName != SPNEGOConstants.KRB5_INI_FILE) {
                    copiedAtDefault = false;
                }
                //If we were able to copy the file then we trigger the flag that let us know that we created the file on this run.
                if (copiedAtDefault) {
                    waskrbiniCreatedAtDefault = true;
                }
                //if we were able to create the file and it was created at the windows directory, we trigger the flag that we
                //did this on this run.
                if (copiedAtWindowsDir && copiedAtDefault) {
                    copiedkrbiniAtWindowsDir = true;
                }
                Log.info(thisClass, methodName, "Is the filed copied at the default location: krb5.ini: " + copiedAtDefault);
            } else {
                Log.info(thisClass, methodName, "Kerberos configuration file will NOT be copied to default location");
            }

            if (keytabFile != null) {

                Log.info(thisClass, methodName, "Kerberos keytab file will be copied to default location");
                //We proceed to try and copy the keytab at the default location.
                RemoteFile keytabFilePath = server.getFileFromLibertyServerRoot(SPNEGOConstants.KRB_RESOURCE_LOCATION.substring(1) + keytabFile);
                LocalFile keytabDefaultFilePath = new LocalFile(krb5DefaultLocation + SPNEGOConstants.KRB5_KEYTAB_FILE);
                copiedAtDefault = copyKrbFilestoDefaultLocation(SPNEGOConstants.KRB5_KEYTAB_FILE, keytabFilePath, keytabDefaultFilePath);
                Log.info(thisClass, methodName, "Kerberos keytab file move result: " + copiedAtDefault);
                //If we were able to copy the file at the default location, we trigger the flag that we did this on this run.
                if (copiedAtDefault) {
                    waskeyTabCreatedAtDefault = true;
                }
                //If we were able to copy this file and it was on the windows directory, we trigger the flag that we did this on this run.
                if (copiedAtWindowsDir && copiedAtDefault) {
                    copiedkeyTabAtWindowsDir = true;
                }
            } else {
                Log.info(thisClass, methodName, "Kerberos keytab file will NOT be copied to default location");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * This method copies the krb conf and keytab at their default location. It will try to do it using this idea:
     * 1. Will try to write it on the Windows Folder.
     * 2. If that is not possible because of an exception, we will write it at the winnt folder.
     * 3. If that is not possible then we will create the winnt folder and copy the files there.
     *
     * @param file:            the file (krbconf or keytab) to be copied
     * @param FilePath:        Current path where the file exist
     * @param DefaultFilePath: Default path where we will copy these files.
     * @return if the copy was sucessful.
     * @throws Exception
     */
    private boolean copyKrbFilestoDefaultLocation(String file, RemoteFile FilePath, LocalFile DefaultFilePath) throws Exception {
        boolean result = false;
        String methodName = "copyKrbFilestoDefaultLocation";
        OperatingSystem osName = server.getMachine().getOperatingSystem();
        //Only create the file if the file does not exist already
        if (DefaultFilePath.exists()) {
            Log.info(thisClass, methodName, "The file already exist, no need to create one.");
            return result;
        }
        try { //Copy the krb files at the Windows default location
//            result = RXAProvider.copy(FilePath, DefaultFilePath, true);
            result = DefaultFilePath.copyFromSource(FilePath, true);

            //if we were able to copy then update the flag saying that we were able to do this
            //at the windows directory.
            if (result) {
                copiedAtWindowsDir = true;
                return result;
            }
        } catch (Exception e) {
            Log.info(thisClass, methodName, "It was not possible to write at the Windows folder, trying to write at the winnt location:" + e);
            //If we were not able to write it at that location, trying to write it at the WINNT folder if this is a Windows OS ONLY.
            if (osName.equals(OperatingSystem.WINDOWS)) {
                try {
                    DefaultFilePath = new LocalFile(SPNEGOConstants.WINNT_DEFAULT_LOCATION + file);
                    //If file already exist at the WINNT location then let's just return.
                    if (DefaultFilePath.exists()) {
                        Log.info(thisClass, methodName, "The file already exist, no need to create one.");
                        return result;
                    } else {
                        //otherwise let's just try to copy the file at the winnt dir.
//                        result = RXAProvider.copy(FilePath, DefaultFilePath, true);
                        result = DefaultFilePath.copyFromSource(FilePath, true);
                    }
                } catch (Exception e2) {
                    //We were not able to write at the winnt folder, because we already confirmed this is a Windows Machine we will go ahead and create the folder.
                    Log.info(thisClass, methodName, "It was not possible to write at the winnt. Trying to create the directory:" + e2);
                    try {
                        //We will try to create the winnt directory
                        createWinNT = (new File(SPNEGOConstants.WINNT_DEFAULT_LOCATION)).mkdir();
                        //and copy the file at that location.
//                        result = RXAProvider.copy(FilePath, DefaultFilePath, true);
                        result = DefaultFilePath.copyFromSource(FilePath, true);
                    } catch (Exception e3) {
                        //If we were not able to create the winnt then we throw an exception.
                        Log.info(thisClass, methodName, "It was not possible to create winnt folder:" + e3);
                    }
                }
            } else {
                Log.info(thisClass, methodName,
                         "This is not Windows OS. We will not attempt to create the winnt folder. The following exception was thrown while writting the file at the default location: "
                                                + e);
                return result;
            }
        }
        return result;
    }

    /**
     * This method deletes the krb5.keytab file from the default location.
     * If it exists, it will be deleted regardless of whether it was created by this instance of KdcHelper.
     *
     * @throws Exception
     */
    public void deleteKeytabFileAtDefaultLocation() throws Exception {
        String methodName = "deleteKeytabFileAtDefaultLocation()";
        Log.info(thisClass, methodName, "Deleting " + SPNEGOConstants.KRB5_KEYTAB_FILE + " at the default location(s) if it exists.");

        String defaultWindowsFilePath = SPNEGOConstants.KRB5_DEFAULT_LOCATION + SPNEGOConstants.KRB5_KEYTAB_FILE;
        String defaultWinntFilePath = SPNEGOConstants.WINNT_DEFAULT_LOCATION + SPNEGOConstants.KRB5_KEYTAB_FILE;

        LocalFile keytabWindowsDefaultFilePath = new LocalFile(defaultWindowsFilePath);
        LocalFile keytabwinntDefaultFilePath = new LocalFile(defaultWinntFilePath);

        boolean anyExistingKeytabFound = false;
        if (keytabWindowsDefaultFilePath.exists()) {
            Log.info(thisClass, methodName, "Existing keytab file found at " + defaultWindowsFilePath);
            anyExistingKeytabFound = true;
            boolean deleteResult = keytabWindowsDefaultFilePath.delete();
            Log.info(thisClass, methodName, defaultWindowsFilePath + " deleted: " + deleteResult);
        }

        if (keytabwinntDefaultFilePath.exists()) {
            Log.info(thisClass, methodName, "Existing keytab file found at " + defaultWinntFilePath);
            anyExistingKeytabFound = true;
            boolean deleteResult = keytabwinntDefaultFilePath.delete();
            Log.info(thisClass, methodName, defaultWinntFilePath + " deleted: " + deleteResult);
        }

        if (!anyExistingKeytabFound) {
            Log.info(thisClass, methodName, "No keytab files were found at " + defaultWindowsFilePath + " or " + defaultWinntFilePath);
        }
    }

    /**
     * This method removes the krb.conf and keytab files from the default location.
     * If the winnt was created then it also removes that folder in order to make sure we don't
     * leave anything that we created.
     *
     * @throws Exception
     */
    public void removeWinNtFilesAndFolderDefaultLocation() throws Exception {
        String methodName = "removeWinNtFilesAndFolderDefaultLocation()";
        Log.info(thisClass, methodName, "Proceeding to remove the winnt dir, krb5.ini and keytab if they were created during this run.");
        //If the keytab file was created at this method run.
        if (keytabCreatedAtDefault()) {
            //We set the flag to false for future calls.
            resetkeytabCreated();
            LocalFile keytabDefaultFilePath;
            //we check to see if the file was created at the windows dir or at the winnt dir in order to remove the file at the correct location.
            if (copiedkeyTabAtWindowsDir) {
                Log.info(thisClass, methodName, "The Windows folder was created, we will remove the keytab file found at this location.");
                keytabDefaultFilePath = new LocalFile(SPNEGOConstants.KRB5_DEFAULT_LOCATION + SPNEGOConstants.KRB5_KEYTAB_FILE);
            } else {
                Log.info(thisClass, methodName, "The wint folder was NOT created, we will remove the keytab file found at the winnt location.");
                keytabDefaultFilePath = new LocalFile(SPNEGOConstants.WINNT_DEFAULT_LOCATION + SPNEGOConstants.KRB5_KEYTAB_FILE);
            }
            //we remove the file
            boolean resultRmvkeytab = keytabDefaultFilePath.delete();
            Log.info(thisClass, methodName, "We deleted the keytab file from winnt: " + resultRmvkeytab);
        } else {
            //if the file was not created during this run.
            Log.info(thisClass, methodName, "We did not created the keytab on this run, we will not delete it.");
        }
        //Now we do the same but using the krb.ini file. If the file was created.
        if (krbiniCreated()) {
            //set the flag to false for future rns.
            resetkrbiniCreated();
            //Now remove the krb.ini
            LocalFile krbIniDefaultFilePath;
            //Was the file created at the windows or winnt dir.
            if (copiedkrbiniAtWindowsDir) {
                Log.info(thisClass, methodName, "The Windows folder was created, we will remove the krb.ini file found at this location.");
                krbIniDefaultFilePath = new LocalFile(SPNEGOConstants.KRB5_DEFAULT_LOCATION + SPNEGOConstants.KRB5_INI_FILE);
            } else {
                Log.info(thisClass, methodName,
                         "The wint folder was NOT created, we will remove the krb5.ini file found at the winnt location.");
                krbIniDefaultFilePath = new LocalFile(SPNEGOConstants.WINNT_DEFAULT_LOCATION + SPNEGOConstants.KRB5_INI_FILE);
            }
            //remove the file
            boolean resultRmvkrbIni = krbIniDefaultFilePath.delete();
            Log.info(thisClass, methodName, "We deleted the krb.ini file from winnt: " + resultRmvkrbIni);
        } else {
            //if the file was not created then nothing happens.
            Log.info(thisClass, methodName, "We did not created the krb5.ini file on this run, we will not delete it.");
        }
        //now removes the winnt folder if this folder was created by our testcases.
        Log.info(thisClass, methodName, "Deleting winnt folder if the folder exist");
        //if the directory was created on this run.
        if (winNtFolderCreated()) {
            //set the flag to false for future runs.
            resetwinNtCreated();
            //go ahead and remove the dir.
            boolean result = new LocalFile(SPNEGOConstants.WINNT_DEFAULT_LOCATION).delete();
            Log.info(thisClass, methodName, "The result of deleting this folder is: " + result);
        } else {
            //the dir was not created on this run.
            Log.info(thisClass, methodName, "We didn't created the folder, so we are NOT going to delete it.");
        }
    }

    /**
     * Returns if we created the keytab file at the winnt location.
     *
     * @returns if the keytab file was created.
     */
    private boolean keytabCreatedAtDefault() {
        return waskeyTabCreatedAtDefault;
    }

    /**
     * To make sure we remove and create this file as needed.
     */
    private void resetkeytabCreated() {
        waskeyTabCreatedAtDefault = false;
    }

    /**
     * Returns if we created the krb5.ini file while running a Windows OS Machine.
     *
     * @return if we created a krbini file
     */
    private boolean krbiniCreated() {
        return waskrbiniCreatedAtDefault;
    }

    /**
     * To make sure we remove and create this file as needed.
     */
    private void resetkrbiniCreated() {
        waskrbiniCreatedAtDefault = false;
    }

    /**
     * Returns if we created the winnt folder
     *
     * @return if the folder was created
     */
    private boolean winNtFolderCreated() {
        return createWinNT;
    }

    /**
     * To make sure we remove and create this folder as needed.
     */
    private void resetwinNtCreated() {
        createWinNT = false;
    }

    /**
     * Scans the server.xml file for the given string and returns true if the string is contained anywhere in the
     * config. If the keytab attribute is specified as the string to find and is found, its corresponding value is
     * recorded as the configured keytab file path.
     *
     * @param findString
     * @return
     * @throws Exception
     */
    protected boolean checkServerXMLForString(String findString) throws Exception {
        String methodName = "checkServerXMLForString";
        String serverConfig = server.getServerRoot() + "/server.xml";
        boolean stringFound = false;

        try {
            Log.info(thisClass, methodName, "Checking for \"" + findString + "\" in server config:" + serverConfig);
            File inp = new File(serverConfig);
            InputStreamReader inputStream = new InputStreamReader(new FileInputStream(inp));
            BufferedReader dataStream = new BufferedReader(inputStream);

            String line = null;
            while ((line = dataStream.readLine()) != null) {
                if (line.contains(findString)) {
                    stringFound = true;
                    Log.info(thisClass, methodName, "Found \"" + findString + "\" in server.xml: " + line.trim());
                    if (findString.equals(KRB5_KEYTAB_ATTRIBUTE)) {
                        Log.info(thisClass, methodName, "Keytab attribute found; it will be used to set the keytab file path");
                        setKeytabFilePath(line);
                    }
                    break;
                }
            }
            dataStream.close();

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return stringFound;
    }

    /**
     * Records the keytab file path based on the string provided. If line contains the ${server.config.dir} variable,
     * the variable is replaced with the server root path before extracting the keytab location.
     *
     * @param line - String expected to contain the keytab location in property=value format. If the value portion
     *                 contains the ${server.config.dir} variable, it is replaced with the server root path. The value
     *                 portion is then set as the keytab file path.
     * @throws Exception
     */
    private void setKeytabFilePath(String line) throws Exception {
        String methodName = "setKeytabFilePath";
        // Strip off anything before the actual keytab attribute
        String tempLocation = line.substring(line.indexOf(KRB5_KEYTAB_ATTRIBUTE));
        String serverConfigVariable = "${server.config.dir}";

        if (tempLocation.contains(serverConfigVariable)) {
            // Substitute the server root for all occurrences of the server config variable
            String serverConfigVariableRegex = "\\$\\{server.config.dir\\}";
            tempLocation = tempLocation.replaceAll(serverConfigVariableRegex, server.getServerRoot());
            Log.info(thisClass, methodName, "Keytab location after server root variable substitution: " + tempLocation);
        }

        int locationStartIndex = tempLocation.indexOf("=") + 1;
        int locationEndIndex = tempLocation.length();
        if (tempLocation.charAt(locationStartIndex) == '"') {
            // Attribute is enclosed in quotes; find the index of the closing quote
            locationEndIndex = tempLocation.indexOf('"', locationStartIndex + 1);
        }

        keytabFilePath = tempLocation.substring(locationStartIndex, locationEndIndex + 1);
        Log.info(thisClass, methodName, "Keytab location set to: " + keytabFilePath);
        if (keytabFilePath == null) {
            throw new RemoteException("Failed to get keytab location");
        }
    }

    /**
     * Deletes a remote file found the the CYGWIN home directory.
     *
     * @param kdcMachine
     * @param remoteFile
     * @throws Exception
     */
    public void deleteRemoteFileFromRemoteMachine(Machine remoteMachine, String remoteFile) throws Exception {

        RemoteFile rFile = new RemoteFile(remoteMachine, SPNEGOConstants.CYGWIN_HOME_REALM_1 + remoteFile);
        removeRemoteFileFromRemoteMachine(rFile);
    }

    /**
     * Get a (started) SshClient.
     *
     * @return The SshClient.
     */
    protected SshClient getSshClient() {
        SshClient sshClient = SshClient.setUpDefaultClient();
        sshClient.start();
        return sshClient;
    }

    /**
     * Get an SSH ClientSession to the specified machine.
     *
     * @param sshClient The SSH client.
     * @param machine   The machine to connect to.
     * @return The session.
     * @throws IOException If there was an error getting an SSH session to the machine.
     */
    protected ClientSession getSshSession(SshClient sshClient, Machine machine) throws IOException {
        ClientSession session = sshClient.connect(machine.getUsername(), machine.getHostname(), 22).verify(30, TimeUnit.SECONDS).getSession();
        session.addPasswordIdentity(machine.getPassword());
        session.auth().verify(30, TimeUnit.SECONDS).isSuccess();
        return session;
    }

    /**
     * Execute the command over the SSH session.
     *
     * @param sshSession The SSH ClientSession.
     * @param command    The command to execute.
     * @param timeout    Timeout for the command, in seconds.
     * @return The ProgramOutput.
     * @throws IOException If there was an error executing the command.
     */
    protected ProgramOutput executeSshCommand(ClientSession sshSession, String command, int timeout) throws IOException {
        final String methodName = "executeSshCommand";
        Log.info(thisClass, methodName, "Executing SSH command --> \"{1}\" with a {2}s timeout on session {0}", new Object[] { sshSession, command, timeout });

        try (ByteArrayOutputStream stdout = new ByteArrayOutputStream();
                        ChannelExec channel = sshSession.createExecChannel(command)) {

            /*
             * Redirect stdout and stderr to one stream. I don't capture each separately b/c I want
             * to see the output in temporal order.
             */
            channel.setOut(stdout);
            channel.setErr(stdout);
            channel.addChannelListener(new SshChannelListener());

            try {
                long remainingTimeoutMs = TimeUnit.SECONDS.toMillis(timeout);

                /*
                 * Open the execution channel, and verify it is open.
                 */
                long startTimeMs = System.currentTimeMillis();
                channel.open().verify(remainingTimeoutMs, TimeUnit.MILLISECONDS);
                remainingTimeoutMs -= System.currentTimeMillis() - startTimeMs;

                if (remainingTimeoutMs <= 0) {
                    Log.info(thisClass, methodName, "The SSH command timed out.");
                    throw new IOException("Timed out trying to open a channel with the host to execute the SSH command. The timeout was " + timeout + " seconds.");
                }

                /*
                 * Execute the command on the channel and wait for it to complete.
                 */
                Set<ClientChannelEvent> ccEvents = channel.waitFor(EnumSet.of(ClientChannelEvent.EXIT_STATUS), remainingTimeoutMs);
                Log.info(thisClass, methodName, "Client channel returned the following events: " + ccEvents);

                /*
                 * Did the command timeout? If so throw an exception.
                 */
                if (ccEvents.contains(ClientChannelEvent.TIMEOUT)) {
                    Log.info(thisClass, methodName, "The SSH command timed out. The timeout was " + timeout + " seconds.");
                    throw new IOException("The SSH command timed out while executing. The timeout was " + timeout + " seconds.");
                }

                return new ProgramOutput(command, channel.getExitStatus(), new String(stdout.toByteArray()), null);
            } finally {
                try {
                    channel.close(false);
                } catch (Throwable t) {
                    // Ignore.
                }

                logSshOutput(new String(stdout.toByteArray()).trim());
            }
        }
    }

    /**
     * Log output from an SSH channel.
     *
     * @param stdout Standard output from the channel.
     */
    private static void logSshOutput(String stdout) {
        final String methodName = "logSshOutput";

        /*
         * Process stdout.
         */
        if (stdout.isEmpty()) {
            stdout = "    [OUTPUT] <NONE>";
        } else {
            /*
             * Add "    [OUTPUT] " to the beginning of each line. The split
             * method might be resource intensive if we have large strings.
             */
            stdout = Arrays.stream(stdout.split("\\r?\\n"))
                            .filter(line -> true)
                            .map(line -> "    [OUTPUT] " + line + System.lineSeparator())
                            .collect(Collectors.joining());
        }

        Log.info(thisClass, methodName, "SSH command output: \n{0}", stdout);
    }

    /**
     * Determine whether the remote file exists.
     *
     * @param sshSession     The SSH session to use to check for the file.
     * @param remoteFileName The remote file name.
     * @return True if the file exists, false otherwise.
     * @throws IOException If there was an error checking the file.
     */
    protected boolean remoteFileExists(ClientSession sshSession, String remoteFileName) throws IOException {
        remoteFileName = remoteFileName.replace("\\", "/"); // Convert windows path to Linux
        return executeSshCommand(sshSession, "test -f " + remoteFileName, 5).getReturnCode() != 0;
    }

    /**
     * Copy a remote file to a local file.
     *
     * @param sshSession The SSH session to use to copy the file.
     * @param remoteFile The remote file to copy.
     * @param localFile  The local file to copy to.
     * @return True if the copy succeeded, false otherwise.
     */
    protected boolean copyFromRemoteFile(ClientSession sshSession, String remoteFile, String localFile) {
        remoteFile = remoteFile.replace("\\", "/"); // Convert windows path to Linux

        Log.info(thisClass, "copyFromRemoteFile", "Copying remote file " + remoteFile + " to local file " + localFile);

        boolean success = false;

        try {
            ScpClient scpClient = ScpClientCreator.instance().createScpClient(sshSession);
            scpClient.download(remoteFile, new FileOutputStream(localFile));
            success = true;
        } catch (IOException e) {
            Log.error(thisClass, "copyFromRemoteFile", e, "SCP encountered an error downloading the remote file "
                                                          + sshSession.getRemoteAddress() + ":" + remoteFile + " to " + localFile);
        }

        /*
         * Validate the copy by looking at the size.
         *
         * NOTE: Originally added this code to validate the file size matches that on the remote system,
         * but had issues where the standard output with the file size was not coming back in a timely
         * fashion, so now we will rely on the SCP download throwing an IOException on failure.
         */
//        long remoteSize = Long.valueOf(executeSshCommand(sshSession, "wc -c < " + remoteFile, 10).getStdout().trim());
//        long localSize = new File(localFile).length();
//        boolean success = remoteSize == localSize;

        Log.info(thisClass, "copyFromRemoteFile", "Copy from remote file was successful? " + success);
        return success;
    }

    /**
     * Copy a local file to a remote file.
     *
     * @param sshSession The SSH session to use to copy the file.
     * @param localFile  The local file to copy from.
     * @param remoteFile The remote file to copy to.
     * @return True if the copy succeeded, false otherwise.
     */
    protected boolean copyLocalFileToRemote(ClientSession sshSession, String localFile, String remoteFile) {
        remoteFile = remoteFile.replace("\\", "/"); // Convert windows path to Linux

        Log.info(thisClass, "copyLocalFileToRemote", "Copying local file " + localFile + " to remote file " + remoteFile);

        boolean success = false;
        try {
            ScpClient scpClient = ScpClientCreator.instance().createScpClient(sshSession);
            scpClient.upload(localFile, remoteFile);
            success = true;
        } catch (IOException e) {
            Log.error(thisClass, "copyLocalFileToRemote", e, "SCP encountered an error uploading the local file "
                                                             + localFile + " to " + sshSession.getRemoteAddress() + ":" + remoteFile);
        }

        /*
         * Validate the copy by looking at the size.
         *
         * NOTE: Originally added this code to validate the file size matches that on the remote system,
         * but had issues where the standard output with the file size was not coming back in a timely
         * fashion, so now we will rely on the SCP upload throwing an IOException on failure.
         */
//        long remoteSize = Long.valueOf(executeSshCommand(sshSession, "wc -c < " + remoteFile, 10).getStdout().trim());
//        long localSize = new File(localFile).length();
//        boolean success = remoteSize == localSize;

        Log.info(thisClass, "copyLocalFileToRemote", "Copy to remote file was successful? " + success);
        return success;
    }

    /**
     * Delete a remote file.
     *
     * @param sshSession The SSH session to use to delete the file.
     * @param remoteFile The remote file to delete.
     * @return True if the delete succeeded, false otherwise.
     * @throws IOException If the delete failed for some reason.
     */
    protected boolean deleteRemoteFile(ClientSession sshSession, String remoteFile) throws IOException {
        remoteFile = remoteFile.replace("\\", "/"); // Convert windows path to Linux

        Log.info(thisClass, "deleteRemoteFile", "Deleting remote file " + remoteFile);
        boolean success = executeSshCommand(sshSession, "rm -f " + remoteFile, 5).getReturnCode() == 0;
        Log.info(thisClass, "deleteRemoteFile", "Delete of remote file was successful? " + success);
        return success;
    }

    /**
     * Handler for listenting to SSH channel events.
     */
    class SshChannelListener implements ChannelListener {
        private Class<?> thisClass = SshChannelListener.class;

        @Override
        public void channelInitialized(Channel channel) {
            Log.info(thisClass, "channelInitialized", "Channel: " + channel);
        }

        @Override
        public void channelOpenSuccess(Channel channel) {
            Log.info(thisClass, "channelOpenSuccess", "Channel: " + channel);
        }

        @Override
        public void channelOpenFailure(Channel channel,
                                       Throwable reason) {
            Log.error(thisClass, "channelOpenFailure", reason, "Channel: " + channel);
        }

        @Override
        public void channelClosed(Channel channel, Throwable reason) {
            Log.error(thisClass, "channelClosed", reason, "Channel: " + channel);
        }

        @Override
        public void channelStateChanged(Channel channel, String hint) {
            Log.info(thisClass, "channelStateChanged", "Channel: " + channel + ", hint: " + hint);
        }
    }
}
