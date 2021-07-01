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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.Subject;

import org.ietf.jgss.Oid;
import org.junit.Ignore;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.SSLServletClient;
import com.ibm.ws.webcontainer.security.test.servlets.ServletClient;

import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

/**
 * Note:
 * 1. User registry:
 * At this time, the test uses users, passwords, roles, and groups predefined in server.xml as
 * test user registry.
 *
 *
 * 2. The constraints (which servlets can be accessed by which user/group/role) are defined in web.xml
 *
 */
@Ignore("This is not a test")
public class CommonTestHelper {

    private final static Class<?> thisClass = CommonTestHelper.class;

    private static String lastServer = null;

    protected List<String> lastCheckApps = new ArrayList<String>();
    private String[] shutdownMsgs = new String[0];

    // To be set by child class
    protected LibertyServer server;
    protected ServletClient client;
    protected SSLServletClient sslClient;

    public CommonTestHelper(LibertyServer server, ServletClient client, SSLServletClient sslClient) {
        this.server = server;
        this.client = client;
        this.sslClient = sslClient;
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
        reconfigureServer(newServerXml, testName, SPNEGOConstants.NO_MSGS, restartServer);
    }

    public void reconfigureServer(String newServerXml, String testName, boolean restartServer, boolean needsJDKConversion) throws Exception {
        reconfigureServer(newServerXml, testName, SPNEGOConstants.NO_MSGS, restartServer, needsJDKConversion);
    }

    public void reconfigureServer(String newServerXml, String testName, List<String> waitForMessages, boolean restartServer) throws Exception {
        reconfigureServer(newServerXml, testName, waitForMessages, restartServer, false);
    }

    /**
     * Reconfigures the current server to use the new server configuration provided.
     *
     * @param newServerXml - Either an absolute path to a server config file or the name of the server config file to
     *            use under the server's configs/ directory
     * @param testName
     * @param waitForMessages - List of regular expressions to be waited for upon server start
     * @param restartServer - boolean indicating whether the server should be restarted
     * @param needsJDKConversion - boolean indicates whether the server needs to be reconfigured for jdk 11 testing.
     * @throws Exception
     */
    public void reconfigureServer(String newServerXml, String testName, List<String> waitForMessages, boolean restartServer, boolean needsJDKConversion) throws Exception {
        String thisMethod = "reconfigureServer";

        if (InitClass.OTHER_SUPPORT_JDKS && needsJDKConversion) {
            Log.info(thisClass, thisMethod, "Converting server.xml: " + newServerXml + " to jdk 11 format.");
            newServerXml = convertServerXMLTojdk11(newServerXml);
            Log.info(thisClass, thisMethod, "Server.xml was succesfully converted to: " + newServerXml);

        } else {
            Log.info(thisClass, thisMethod, "The server.xml does not need to be modified to jdk 11 format");
        }

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
    public void restartServer(String serverXml, String testName, List<String> checkApps, List<String> waitForMessages) throws Exception {
        String thisMethod = "restartServer";
        if (server.isStarted()) {
            server.setMarkToEndOfLog();
            server.stopServer(shutdownMsgs);
            // Make sure that all files are backed up to the timestamped server directory under autoFVT
            server.waitForStringInLog("Successfully recovered server");
            Log.info(thisClass, thisMethod, "Resetting shutdown message list to empty list");
            shutdownMsgs = new String[0];
        }

        startServer(serverXml, checkApps, waitForMessages);

        if (checkApps != null) {
            lastCheckApps.addAll(checkApps);
        }
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
    public void startServer(String serverXml, List<String> checkApps) throws Exception {
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
    public void startServer(String serverXml, List<String> checkApps, List<String> waitForMessages) throws Exception {
        String thisMethod = "startServer";

        // Set up list of apps to verify running state of
        if (checkApps != null && !checkApps.isEmpty()) {
            String thisApp = null;
            Iterator<String> apps = checkApps.iterator();
            while (apps.hasNext()) {
                thisApp = apps.next();
                Log.info(thisClass, thisMethod, "Adding " + thisApp + " to list of apps to wait for");
                server.addInstalledAppForValidation(thisApp);
            }
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
        server.startServer();

        waitForMessages(waitForMessages, true);
    }

    public void setShutdownMessages(String... shutdownMessages) {
        String thisMethod = "setShutdownMessages";
        for (String msg : shutdownMessages) {
            Log.info(thisClass, thisMethod, "Adding message to list of messages to check on server stop: " + msg);
        }
        shutdownMsgs = shutdownMessages;
        Log.info(thisClass, thisMethod, "Tracking " + shutdownMsgs.length + " shutdown messages");
    }

    public void addShutdownMessages(String... shutdownMessages) {
        String thisMethod = "addShutdownMessages";
        for (String msg : shutdownMessages) {
            Log.info(thisClass, thisMethod, "Adding message to list of messages to check on server stop: " + msg);
        }

        String[] destArray = new String[shutdownMsgs.length + shutdownMessages.length];
        System.arraycopy(shutdownMsgs, 0, destArray, 0, shutdownMsgs.length);
        System.arraycopy(shutdownMessages, 0, destArray, shutdownMsgs.length, shutdownMessages.length);
        shutdownMsgs = destArray;
    }

    public String[] getShutdownMessages() {
        return shutdownMsgs;
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

            server.waitForStringInLogUsingMark("CWWKG001[7-8]I", 10000);
            waitForMessages(startMessages, true);

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

    /**
     * Create a SPNEGO token for the target server using the passed parameters.
     *
     * @param username
     * @param password
     * @param targetServer
     * @param krb5ConfPath - Path to krb5.conf file relative to server root path
     * @param krb5Helper
     * @return
     * @throws Exception
     */
    public String createSpnegoToken(String username, String password, String targetServer, String krb5ConfPath, Krb5Helper krb5Helper) throws Exception {
        return createSpnegoToken(username, password, targetServer, null, null, krb5ConfPath, krb5Helper, true);
    }

    /**
     * Create a SPNEGO token for the target server using the passed parameters. This method will not rely on a
     * krb5.conf file, but will instead use the realm and host name of the KDC provided when performing the Kerberos
     * login.
     *
     * @param username
     * @param password
     * @param targetServer
     * @param realm
     * @param kdcHostName
     * @param krb5Helper
     * @return
     * @throws Exception
     */
    public String createSpnegoToken(String username, String password, String targetServer, String realm, String kdcHostName, Krb5Helper krb5Helper) throws Exception {
        return createSpnegoToken(username, password, targetServer, realm, kdcHostName, null, krb5Helper, true);
    }

    /**
     * Create a SPNEGO token for the target server using the passed parameters.
     *
     * @param username
     * @param password
     * @param targetServer
     * @param realm
     * @param kdcHostName
     * @param krb5ConfPath - Path to krb5.conf file relative to server root path
     * @param krb5Helper
     * @param includeClientGSSCredentialInSubject
     * @return
     * @throws Exception
     */
    public String createSpnegoToken(String username, String password, String targetServer, String realm, String kdcHostName, String krb5ConfPath,
                                    Krb5Helper krb5Helper, boolean includeClientGSSCredentialInSubject) throws Exception {

        return createToken(username, password, targetServer, realm, kdcHostName, krb5ConfPath, krb5Helper, includeClientGSSCredentialInSubject,
                           Krb5Helper.SPNEGO_MECH_OID);
    }

    /**
     * Create a SPNEGO or Kerberos token base on mechOid for the target server using the passed parameters.
     *
     * @param username
     * @param password
     * @param targetServer
     * @param realm
     * @param kdcHostName
     * @param krb5ConfPath - Path to krb5.conf file relative to server root path
     * @param krb5Helper
     * @param includeClientGSSCredentialInSubject
     * @param mechOid
     * @return
     * @throws Exception
     */
    public String createToken(String username, String password, String targetServer, String realm, String kdcHostName, String krb5ConfPath,
                              Krb5Helper krb5Helper, boolean includeClientGSSCredentialInSubject, Oid mechOid) throws Exception {
        return createToken(username, password, targetServer, realm, kdcHostName, krb5ConfPath, krb5Helper, includeClientGSSCredentialInSubject,
                           Krb5Helper.SPNEGO_MECH_OID, null);
    }

    /**
     * Create a SPNEGO or Kerberos token base on mechOid for the target server using the passed parameters.
     *
     * @param username
     * @param password
     * @param targetServer
     * @param realm
     * @param kdcHostName
     * @param krb5ConfPath - Path to krb5.conf file relative to server root path
     * @param krb5Helper
     * @param includeClientGSSCredentialInSubject
     * @param mechOid
     * @param jaasLoginContextEntry
     * @return
     * @throws Exception
     */
    public String createToken(String username, String password, String targetServer, String realm, String kdcHostName, String krb5ConfPath,
                              Krb5Helper krb5Helper, boolean includeClientGSSCredentialInSubject, Oid mechOid, String jaasLoginContextEntry) throws Exception {
        String method = "createToken";
        String targetSpn = "HTTP/" + targetServer;
        Log.info(thisClass, method, "Target SPN: " + targetSpn);
        String krb5Config = null;
        if (krb5ConfPath != null) {
            krb5Config = server.getServerRoot() + krb5ConfPath;
        }

        Subject subject = null;
        String krbName = username;
        if (realm != null) {
            krbName = username + "@" + realm;
            subject = krb5Helper.kerberosLogin(server, username, password, null, realm, kdcHostName, jaasLoginContextEntry);
        } else {
            subject = krb5Helper.kerberosLogin(server, username, password, krb5Config);
        }

        return krb5Helper.createToken(subject, krbName, targetSpn, includeClientGSSCredentialInSubject, mechOid);
    }

    public String getTestSystemFullyQualifiedDomainName() throws UnknownHostException {
        return InitClass.serverCanonicalHostName;
    }

    /**
     * Creates a map of header names and values using the passed parameters.
     *
     * @param authorizationHeader
     * @param userAgent
     * @param host
     * @param remoteAddr
     * @return
     */
    public Map<String, String> setTestHeaders(String authorizationHeader, String userAgent, String host, String remoteAddr) {
        Map<String, String> headers = new HashMap<String, String>();
        if (authorizationHeader != null) {
            headers.put(SPNEGOConstants.HEADER_AUTHORIZATION, authorizationHeader);
        }
        if (userAgent != null) {
            headers.put(SPNEGOConstants.HEADER_USER_AGENT, userAgent);
        }
        if (host != null) {
            headers.put(SPNEGOConstants.HEADER_HOST, host);
        }
        if (remoteAddr != null) {
            headers.put(SPNEGOConstants.HEADER_REMOTE_ADDR, remoteAddr);
        }
        return headers;
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
        waitForMessages(messages, expectedResult, SPNEGOConstants.DEFAULT_LOG_SEARCH_TIMEOUT);
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

    public static String convertServerXMLTojdk11(String originalServerXML) {
        String newServerXML = originalServerXML.substring(0, originalServerXML.length() - 4);
        return newServerXML + "_jdk11.xml";
    }

}
