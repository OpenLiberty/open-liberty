/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.security.jacc_fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.Ignore;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.ServletClient;

import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

import componenttest.topology.impl.LibertyServer.CheckpointInfo;
import io.openliberty.checkpoint.spi.CheckpointPhase;

/**
 *
 */
@Ignore("This is not a test")
public class EJBAnnTestBaseHelper {

    private final Class<?> thisClass = EJBAnnTestBaseHelper.class;

    private static String lastServer = null;

    protected String lastCheckApps = null;

    // To be set by child class
    protected LibertyServer server;
    protected ServletClient client;
    
    protected boolean checkpointEnabled;

    protected EJBAnnTestBaseHelper(LibertyServer server, ServletClient clientclient, boolean checkpointEnabled) {
        this.server = server;
        this.client = client;
        this.checkpointEnabled = checkpointEnabled;
    }

    //----------------------------------
    // utility methods
    //----------------------------------

    /**
     * Reconfigures the current server to use the new server configuration provided. No additional messages will be
     * waited for and any message wait failures will be reported through JUnit.
     *
     * @param newServerXml - Either an absolute path to a server config file or the name of the server config file to
     *            use under the server's configs/ directory
     * @param testName
     * @param restartServer - boolean indicating whether the server should be restarted
     * @throws Exception
     */
    public void reconfigureServer(String newServerXml, String testName, boolean restartServer) throws Exception {
        reconfigureServer(newServerXml, testName, Constants.NO_MSGS, restartServer);
    }

    /**
     * Reconfigures the current server to use the new server configuration provided.
     *
     * @param newServerXml - Either an absolute path to a server config file or the name of the server config file to
     *            use under the server's configs/ directory
     * @param testName
     * @param waitForMessages - List of regular expressions to be waited for upon server start
     * @param restartServer - boolean indicating whether the server should be restarted
     * @throws Exception
     */
    public void reconfigureServer(String newServerXml, String testName, List<String> waitForMessages, boolean restartServer) throws Exception {
        String thisMethod = "reconfigureServer";

        Log.info(thisClass, thisMethod, "Starting server.xml update for: " + testName);
        try {
            if (newServerXml == null) {
                Log.info(thisClass, thisMethod, "No new server xml specified for: " + server.getServerName() + " - skipping reconfig");
                return;
            }

            if (restartServer) {
                restartServer(newServerXml, testName, lastCheckApps, waitForMessages);
                return;
            }

            // Mark the end of the log - all work will occur after this point
            server.setMarkToEndOfLog();

            // Update the server config by replacing the server.xml
            // If copyFromFile has a slash in it, assume the full path was given. Otherwise, assume the same server's config/ subdirectory
            String fullServerPath = null;
            if (newServerXml.contains(File.separator)) {
                fullServerPath = newServerXml;
            } else {
                fullServerPath = buildFullServerConfigPath(server, newServerXml);
            }

            Log.info(thisClass, thisMethod, "New server config: " + fullServerPath + "; Last server config: " + lastServer);
            if (fullServerPath.equals(lastServer)) {
                Log.info(thisClass, thisMethod, "The same server config file is being used - skipping reconfig");
                return;
            }

            lastServer = fullServerPath;
            copyNewServerConfig(fullServerPath);

            if (server.isStarted()) {
                Log.info(thisClass, thisMethod, "Server is already running during reconfig; waiting for appropriate messages");
                waitForServer(testName, waitForMessages);
            } else {
                Log.info(thisClass, thisMethod, "Server was not running during reconfig; starting server and waiting for appropriate messages");
                startServer(null, null, waitForMessages);
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
    }

    /**
     * Restarts the current server using the server configuration file provided.
     *
     * @param serverXml - Either an absolute path to a server config file or the name of the server config file to use
     *            under the server's configs/ directory. If null, a server.xml file is expected to be present in the
     *            server's root directory.
     * @param testName
     * @param checkApps - List of apps to be validated as ready upon server restart
     * @param waitForMessages - List of regular expressions to be waited for upon server restart
     * @throws Exception
     */
    public void restartServer(String serverXml, String testName, String checkApps, List<String> waitForMessages) throws Exception {
        if (server.isStarted()) {
            server.setMarkToEndOfLog();
            server.stopServer();
            // Make sure that all files are backed up to the timestamped server directory under autoFVT
            server.waitForStringInLog("Successfully recovered server");
        }

        startServer(serverXml, checkApps, waitForMessages);
    }

    /**
     * Starts the current server. This method expects a server.xml to be present in the server's root directory. No
     * additional applications will be validated as started, no additional messages will be waited for, and any message
     * wait failures will be reported through JUnit.
     *
     * @throws Exception
     */
    public void startServer() throws Exception {
        startServer(null);
    }

    /**
     * Starts the current server using the server configuration file provided. No additional applications will be
     * validated as started, no additional messages will be waited for, and any message wait failures will be
     * reported through JUnit.
     *
     * @param serverXml - Either an absolute path to a server config file or the name of the server config file to use
     *            under the server's configs/ directory. If null, a server.xml file is expected to be present in the
     *            server's root directory.
     * @throws Exception
     */
    public void startServer(String serverXml) throws Exception {
        startServer(serverXml, null, null);
    }

    /**
     * Starts the current server using the server configuration file provided. No additional messages will be waited
     * for, and any message wait failures will be reported through JUnit.
     *
     * @param serverXml - Either an absolute path to a server config file or the name of the server config file to use
     *            under the server's configs/ directory. If null, a server.xml file is expected to be present in the
     *            server's root directory.
     * @param checkApps - List of apps to be validated as ready upon server start
     * @throws Exception
     */
    public void startServer(String serverXml, String checkApps) throws Exception {
        startServer(serverXml, checkApps, null);
    }

    /**
     * Starts the current server using the server configuration file provided. If no serverXml value is provided, a
     * server.xml file is expected to exist in the server's root directory.
     *
     * @param serverXml - Either an absolute path to a server config file or the name of the server config file to use
     *            under the server's configs/ directory. If null, a server.xml file is expected to be present in the
     *            server's root directory.
     * @param checkApps - List of apps to be validated as ready upon server start
     * @param waitForMessages - List of regular expressions to be waited for upon server start
     * @throws Exception
     */
    public void startServer(String serverXml, String checkApps, List<String> waitForMessages) throws Exception {
        String thisMethod = "startServer";

        // Set up list of apps to verify running state of
        if (checkApps != null) {
            Log.info(thisClass, thisMethod, "Adding " + checkApps + " to list of apps to wait for");
            server.addInstalledAppForValidation(checkApps);

        } else {
            Log.info(thisClass, thisMethod, "No additional application ready messages to search for");
        }

        if (serverXml != null) {
            // If serverXml has a slash in it, assume the full path was given. Otherwise, assume the same server's config/ subdirectory
            if (serverXml.contains(File.separator)) {
                lastServer = serverXml;
            } else {
                lastServer = buildFullServerConfigPath(server, serverXml);
            }
            copyNewServerConfig(lastServer);
        } else {
            Log.info(thisClass, thisMethod, "No server config file name was provided, so we will use the existing server.xml in the server's root directory");
        }

        Log.info(thisClass, thisMethod, "Starting server: " + server.getServerName());
        
        if (checkpointEnabled) {
            CheckpointInfo checkpointInfo = new CheckpointInfo(CheckpointPhase.AFTER_APP_START, true, null);
            server.setCheckpoint(checkpointInfo);
            server.addCheckpointRegexIgnoreMessages("SRVE9967W", "CNTR0338W");
        }
        server.startServer();

        waitForMessages(waitForMessages, true);
    }

    /**
     * Waits for the server to complete a configuration update. Also waits for all messages included in startMessages
     * to appear in the log.
     *
     * @param testName
     * @param startMessages - List of regular expressions to be waited for
     * @throws Exception
     */
    private void waitForServer(String testName, List<String> startMessages) throws Exception {
        String thisMethod = "waitForServer";
        try {
            // Check that the server detected the update; go on in either case - msgs will be logged either way
            if (server.waitForStringInLogUsingMark("CWWKG0016I") != null) {
                Log.info(thisClass, thisMethod, "Server detected the update");
            } else {
                Log.info(thisClass, thisMethod, "Server did NOT detect the update - will continue waiting for additional messages");
            }

            String noUpdateMsg = server.waitForStringInLogUsingMark("CWWKG0018I", 10000);
            // If we didn't get the msg that no update is needed, go on to check for the update completed msg
            if (noUpdateMsg == null) {
                // Wait for msg that server was actually updated
                Log.info(thisClass, thisMethod, "Server update msg: " + server.waitForStringInLogUsingMark("CWWKG0017I"));
                waitForMessages(startMessages, true);
            } else {
                Log.info(thisClass, thisMethod, "noUpdateMsg: " + noUpdateMsg);
                Log.info(thisClass, thisMethod, "Server doesn't need to be updated");
            }

            Log.info(thisClass, thisMethod, "Completed server.xml update: " + testName);
            System.err.flush();
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
    }

    /**
     * Builds and returns the absolute path to the specified file within the configs/ directory under the given
     * server's root directory.
     *
     * @param theServer
     * @param fileName
     * @return
     */
    public String buildFullServerConfigPath(LibertyServer theServer, String fileName) {
        String serverFileName = System.getProperty("user.dir") + File.separator + theServer.getPathToAutoFVTNamedServer() + "configs" + File.separator + fileName;
        Log.info(thisClass, "buildFullServerConfigPath", "serverFileName: " + serverFileName);
        return serverFileName;
    }

    /**
     * Copies the specified file into the server's root directory and renames it to server.xml. This will overwrite
     * any existing server.xml file within the server's root directory without backing up the existing configuration.
     *
     * @param copyFromFile
     * @throws Exception
     */
    public void copyNewServerConfig(String copyFromFile) throws Exception {
        String thisMethod = "copyNewServerConfig";

        String serverFileLoc = (new File(server.getServerConfigurationPath().replace('\\', '/'))).getParent();

        // Update the server config by replacing the server.xml
        if (copyFromFile != null && !copyFromFile.isEmpty()) {
            try {
                Log.info(thisClass, thisMethod, "Copying: " + copyFromFile + " to " + serverFileLoc);
                LibertyFileManager.copyFileIntoLiberty(server.getMachine(), serverFileLoc, "server.xml", copyFromFile);
            } catch (Exception ex) {
                ex.printStackTrace(System.out);
                throw ex;
            }
        }
    }

    public void checkForMessages(boolean expectedResult, String... msgs) throws Exception {
        List<String> checkMsgs = new ArrayList<String>();
        for (String msg : msgs) {
            checkMsgs.add(msg);
        }
        waitForMessages(checkMsgs, expectedResult);
    }

    /**
     * Searches and waits for message strings in the default log and reports success/failure of the search via a
     * message and JUnit. If expectedResult is false, the passed messages are expected NOT to be found.
     *
     * @param messages - List of regular expression strings to wait for in the default log
     * @param expectedResult - If true, the messages specified are expected to be found. Otherwise, the passed messages
     *            are expected NOT to be found.
     * @throws Exception
     */
    public void waitForMessages(List<String> messages, boolean expectedResult) throws Exception {
        waitForMessages(messages, expectedResult, Constants.DEFAULT_LOG_SEARCH_TIMEOUT);
    }

    public void waitForMessages(List<String> messages, boolean expectedResult, int searchTimeOut) throws Exception {
        String thisMethod = "waitForMessages";
        if (messages == null) {
            Log.info(thisClass, thisMethod, "No additional messages need to be located");
            return;
        }
        for (String searchMsg : messages) {
            Log.info(thisClass, thisMethod, "Server test message verification: Searching for message: " + searchMsg);

            String locatedMsg = server.waitForStringInLogUsingMark(searchMsg, searchTimeOut);
            Log.info(thisClass, thisMethod, "Message content found is: " + locatedMsg);
            assertAndLog(thisMethod,
                         "Did not find expected message: \"" + searchMsg + "\" after waiting default timeout",
                         locatedMsg != null, expectedResult);

        }
    }

    /**
     * Logs the state of the test assertion and then invokes the JUnit assertTrue or assertFalse methods, depending on
     * the value of expectedResult, to record the test "status" with JUnit.
     *
     * @param caller - Routine that is requesting the check be performed
     * @param msg - Message that will be recorded if the test assertion fails
     * @param trueFalse - State of the test assertion
     * @param expectedResult - Expected result of the test assertion
     * @return
     */
    private boolean assertAndLog(String caller, String msg, Boolean trueFalse, Boolean expectedResult) {
        Log.info(thisClass, caller, "Test assertion is: " + trueFalse);
        if (expectedResult) {
            if (!trueFalse) {
                Log.info(thisClass, caller, msg);
            }
            assertTrue(msg, trueFalse);
            return true;
        }
        if (trueFalse) {
            Log.info(thisClass, caller, msg);
        }
        assertFalse(msg, trueFalse);
        return false;
    }

    /**
     * Adds the system property values specified to the bootstrap.properties file for use in server configurations for
     * the server provided.
     *
     * @param server - Server for which bootstrap properties file needs to be updated
     * @param properties - Map of bootstrap property names and values to be set
     * @throws Exception
     */
    public void addBootstrapProperties(LibertyServer server, Map<String, String> properties) throws Exception {
        String method = "addBootstrapProperties";
        if (server == null) {
            Log.info(thisClass, method, "No server specified; no bootstrap properties will be written");
            return;
        }
        Log.info(thisClass, method, "Adding necessary system properties to the bootstrap properties file of server: " + server.getServerName());

        RemoteFile bootstrapPropFile = getBootstrapPropFile(server);
        FileInputStream in = prepareBootstrapFileForReading(bootstrapPropFile);
        Properties props = loadProperties(in);

        Set<String> keys = properties.keySet();
        for (String key : keys) {
            String value = properties.get(key);
            Log.info(thisClass, method, key + "=" + value);
            props.setProperty(key, value);
        }

        FileOutputStream out = prepareBootstrapFileForWriting(bootstrapPropFile);
        writeProperties(props, out);
    }

    private RemoteFile getBootstrapPropFile(LibertyServer server) throws Exception {
        String method = "getBootstrapPropFile";

        RemoteFile bootstrapPropFile = null;
        try {
            bootstrapPropFile = server.getServerBootstrapPropertiesFile();
        } catch (Exception e) {
            Log.error(thisClass, method, e, "Error while getting the bootstrap properties file from Liberty server.");
            throw new Exception("Error while getting the bootstrap properties file from Liberty server.");
        }
        return bootstrapPropFile;
    }

    private FileInputStream prepareBootstrapFileForReading(RemoteFile bootstrapPropFile) throws Exception {
        String method = "prepareBootstrapFileForReading";

        FileInputStream input = null;
        try {
            input = (FileInputStream) bootstrapPropFile.openForReading();
        } catch (Exception e) {
            Log.error(thisClass, method, e, "Error while reading the remote bootstrap properties file.");
            throw new Exception("Error while reading the remote bootstrap properties file.");
        }
        return input;
    }

    private Properties loadProperties(FileInputStream input) throws IOException {
        String method = "loadProperties";

        Properties props = new Properties();
        try {
            props.load(input);
        } catch (IOException e) {
            Log.error(thisClass, method, e, "Error while loading properties from file input stream.");
            throw new IOException("Error while loading properties from file input stream.");
        } finally {
            try {
                input.close();
            } catch (IOException e1) {
                Log.error(thisClass, method, e1, "Error while closing the input stream.");
                throw new IOException("Error while closing the input stream.");
            }
        }
        return props;
    }

    private FileOutputStream prepareBootstrapFileForWriting(RemoteFile bootstrapPropFile) throws Exception {
        String method = "prepareBootstrapFileForWriting";

        // Open the remote file for writing with append as false
        FileOutputStream output = null;
        try {
            output = (FileOutputStream) bootstrapPropFile.openForWriting(false);
        } catch (Exception e) {
            Log.error(thisClass, method, e, "Error while writing to remote bootstrap properties file.");
            throw new Exception("Error while writing to remote bootstrap properties file.");
        }
        return output;
    }

    private void writeProperties(Properties props, FileOutputStream output) throws Exception {
        String method = "writeProperties";

        // Write the properties to remote bootstrap properties file
        try {
            props.store(output, null);
            Log.info(thisClass, method, "Added new system properties to bootstrap file");
        } catch (IOException e) {
            Log.error(thisClass, method, e, "Error while reading the remote bootstrap properties file.");
            throw new Exception("Error while reading the remote bootstrap properties file.");
        } finally {
            try {
                output.close();
                Log.info(thisClass, method, "Closed output stream");
            } catch (IOException e) {
                Log.error(thisClass, method, e, "Error while closing the output stream.");
                throw new IOException("Error while closing the output stream.");
            }
        }
    }

    /**
     * Reconfigures the current server to use the new server configuration provided.
     *
     * @param newRoleMappingProps - Either an absolute path to a roleMapping.props file or the name of the roleMapping.props config file to
     *            use under the server's configs/ directory
     * @param testName
     * @param waitForMessages - List of regular expressions to be waited for upon server start
     * @param restartServer - boolean indicating whether the server should be restarted
     * @throws Exception
     */
    public void changeRoleMappingProps(String newRoleMappingProps, String testName, List<String> waitForMessages, boolean restartServer) throws Exception {
        String thisMethod = "changeRoleMappingProps";

        Log.info(thisClass, thisMethod, "Updating roleMappings.props for: " + testName);
        try {
            if (newRoleMappingProps == null) {
                Log.info(thisClass, thisMethod, "No new rolemappings prop specified skipping reconfig");
                return;
            }

            if (restartServer) {
                Log.info(thisClass, thisMethod, "Will Restart the server.");
                if (server.isStarted()) {
                    server.setMarkToEndOfLog();
                    server.stopServer();
                    // Make sure that all files are backed up to the timestamped server directory under autoFVT
                    server.waitForStringInLog("Successfully recovered server");
                }
            }

            String newRoleMappingPropsfullPath = null;
            if (newRoleMappingProps.contains(File.separator)) {
                newRoleMappingPropsfullPath = newRoleMappingProps;
            } else {
                newRoleMappingPropsfullPath = buildFullServerConfigPath(server, newRoleMappingProps);
            }
            Log.info(thisClass, thisMethod, "New roleMapping.props file config location: " + newRoleMappingPropsfullPath);

            String oldRoleMappingsLocation = server.getServerRoot() + Constants.ROLE_MAPPING_PROPS_DEFAULT_LOCATION;

            Log.info(thisClass, thisMethod, "Current RoleMappings file location: " + oldRoleMappingsLocation);

            String oldRoleMappingFilePath = (new File(oldRoleMappingsLocation.replace('\\', '/'))).toString();

            Log.info(thisClass, thisMethod, "Copying: " + newRoleMappingPropsfullPath + " to " + oldRoleMappingFilePath);

            LibertyFileManager.copyFileIntoLiberty(server.getMachine(), oldRoleMappingFilePath, "roleMapping.props", newRoleMappingPropsfullPath);

            if (server.isStarted()) {
                Log.info(thisClass, thisMethod, "Server is already running");
                //need to check if there is any message that we should expect here.
            } else {
                Log.info(thisClass, thisMethod, "Server was not running during reconfig; starting server and waiting for appropriate messages");
                startServer(null, null, waitForMessages);
            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
    }

}
