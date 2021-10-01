/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.junit.rules.ExternalResource;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.fat.common.servers.ServerBootstrapUtils;

import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.impl.LibertyServerWrapper;
import componenttest.topology.utils.CommonMergeTools;
import componenttest.topology.utils.FileUtils;

@LibertyServerWrapper
public class TestServer extends ExternalResource {
    private final Class<?> thisClass = TestServer.class;

    public static final String CWWKG0016I_STARTING_SERVER_UPDATE = "CWWKG0016I";
    public static final String CWWKG0017I_SERVER_CONFIG_UPDATED = "CWWKG0017I";
    public static final String CWWKG0018I_SERVER_CONFIG_NOT_UPDATED = "CWWKG0018I";

    protected static String hostName = "localhost";

    protected LibertyServer server = null;
    protected String origServerXml = null;
    protected String serverName = null;
    protected String lastServer = null;
    protected boolean serverWasRestarted = false;
    protected boolean checkForSecurityReady = true;
    protected boolean restoreServerBetweenTests = true;
    protected String thisServerType = null;
    protected List<String> lastCheckApps = new ArrayList<String>();
    protected String[] passwordsUsedWithServer = null;
    protected String serverHostName = null;
    protected String serverCanonicalHostName = null;
    protected String serverHostIp = null;
    protected Integer serverHttpPort = null;
    protected Integer serverHttpsPort = null;
    protected String[] ignoredServerExceptions = null;
    protected String callback = null;
    protected String callbackFeature;
    protected int retryTimeoutCount = 0;
    protected int overrideRestartWaitTime = 0;
    protected int sslWaitTimeoutCount = 0;

    protected CommonMessageTools msgUtils = new CommonMessageTools();
    protected ServerBootstrapUtils bootstrapUtils = new ServerBootstrapUtils();

    public TestServer(String requestedServer, String serverXML, String callbackHandler, String feature) {
        this(requestedServer, serverXML);
        try {
            installCallbackHandler(callbackHandler, feature);
        } catch (Exception e) {
            Log.info(thisClass, "TestServer 3 parm constructor could NOT install the callback", e.toString());
        }
    }

    public TestServer(String requestedServer, String serverXML) {
        server = LibertyServerFactory.getLibertyServer(requestedServer, null, true);
        serverName = requestedServer;
        // Handle using a server xml with a name other than server.xml
        if (serverXML != null) {
            origServerXml = buildFullServerConfigPath(server, serverXML);
        } else {
            origServerXml = server.getServerRoot() + File.separator + "server.xml";
        }
        Log.info(thisClass, "TestServer-I d1", "origServerXml set to: " + origServerXml);
    }

    public TestServer() {
    }

    public void setServerType(String type) {
        thisServerType = type;
    }

    public String getServerType() {
        return thisServerType;
    }

    public LibertyServer getServer() {
        return server;
    }

    public String getServerXml() throws Exception {
        return server.getServerConfigurationFile().toString();
    }

    public Integer getHttpDefaultPort() {
        Log.info(thisClass, "getHttpDefaultPort", "JESSE: server: " + server); // TODO REMOVE
        Log.info(thisClass, "getHttpDefaultPort", "JESSE: port:   " + server.getHttpDefaultPort()); // TODO REMOVE
        return server.getHttpDefaultPort();
    }

    public Integer getHttpDefaultSecurePort() {
        Log.info(thisClass, "getHttpDefaultSecurePort", "JESSE: server: " + server); // TODO REMOVE
        Log.info(thisClass, "getHttpDefaultSecurePort", "JESSE: port:   " + server.getHttpDefaultSecurePort()); // TODO REMOVE
        return server.getHttpDefaultSecurePort();
    }

    public String getHttpString() {
        Log.info(thisClass, "getHttpString", String.valueOf(getHttpDefaultPort()));
        String result = "http://" + hostName + ":" + getHttpDefaultPort();
        Log.info(thisClass, "getHttpString", "Result: " + result);
        return result;
    }

    public String getHttpsString() {
        Log.info(thisClass, "getHttpsString", String.valueOf(getHttpDefaultSecurePort()));
        String result = "https://" + hostName + ":" + getHttpDefaultSecurePort();
        Log.info(thisClass, "getHttpsString", "Result: " + result);
        return result;
    }

    public String getServerHttpString() {
        Log.info(thisClass, "getServerHttpString", getServerHttpPort().toString());
        String result = "http://" + hostName + ":" + getServerHttpPort();
        Log.info(thisClass, "getServerHttpString", "Result: " + result);
        return result;
    }

    public String getServerHttpsString() {
        Log.info(thisClass, "getServerHttpsString", getServerHttpsPort().toString());
        String result = "https://" + hostName + ":" + getServerHttpsPort();
        Log.info(thisClass, "getServerHttpsString", "Result: " + result);
        return result;
    }

    public String getServerHttpCanonicalString() {
        Log.info(thisClass, "getServerHttpCanonicalString", getServerHttpPort().toString());
        String result = "http://" + serverCanonicalHostName + ":" + getServerHttpPort();
        Log.info(thisClass, "getServerHttpCanonicalString", "Result: " + result);
        return result;
    }

    public String getServerHttpsCanonicalString() {
        Log.info(thisClass, "getServerHttpsCanonicalString", getServerHttpsPort().toString());
        String result = "https://" + serverCanonicalHostName + ":" + getServerHttpsPort();
        Log.info(thisClass, "getServerHttpsCanonicalString", "Result: " + result);
        return result;
    }

    public void setServerHttpPort(Integer port) {
        this.serverHttpPort = port;
    }

    public void setServerHttpsPort(Integer port) {
        this.serverHttpsPort = port;
    }

    public Integer getServerHttpPort() {
        return this.serverHttpPort;
    }

    public Integer getServerHttpsPort() {
        return serverHttpsPort;
    }

    public void setSkipSecurityReadyMsg() {
        Log.info(thisClass, "setSkipSecurityReadyMsg", "Disable security check");
        checkForSecurityReady = false;
    }

    public void resetCheckForSecurityReady() {
        checkForSecurityReady = true;
    }

    public void setPasswordsUsedWithServer(String[] listOfPasswords) {
        passwordsUsedWithServer = listOfPasswords;
    }

    public void setIgnoredServerExceptions(String[] ignoredExceptions) {
        this.ignoredServerExceptions = ignoredExceptions.clone();
    }

    public String[] getIgnoredServerExceptions() {
        return this.ignoredServerExceptions;
    }

    public void setServerHostname(String hostname) {
        serverHostName = hostname;
    }

    public String getServerHostname() {
        return serverHostName;
    }

    public void setServerCanonicalHostname(String hostname) {
        serverCanonicalHostName = hostname;
    }

    public String getServerCanonicalHostname() {
        return serverCanonicalHostName;
    }

    public void setServerHostIp(String ip) {
        serverHostIp = ip;
    }

    public String getServerHostIp() {
        return serverHostIp;
    }

    public void setRestoreServerBetweenTests(boolean restore) {
        restoreServerBetweenTests = restore;
    }

    public boolean getRestoreServerBetweenTests() {
        return restoreServerBetweenTests;
    }

    public void addIgnoredServerException(String exception) {
        addIgnoredServerExceptions(exception);
    }

    public void addIgnoredServerExceptions(String... exceptions) {
        String method = "addIgnoredServerException";

        String[] currentExceptions = getIgnoredServerExceptions();
        if (currentExceptions == null) {
            currentExceptions = new String[0];
        }
        Log.info(thisClass, method, "Current exception list: " + Arrays.toString(currentExceptions));

        List<String> currentExceptionsList = new ArrayList<String>(Arrays.asList(currentExceptions));

        for (String exception : exceptions) {
            if ((!currentExceptionsList.isEmpty()) && (currentExceptionsList.contains(exception))) {
                Log.info(thisClass, method, "NOT adding message(s) [" + exception + "] to the list of ignored server exceptions as it is already in the list");
            } else {
                Log.info(thisClass, method, "Adding message(s) [" + exception + "] to the list of ignored server exceptions");
                currentExceptionsList.add(exception);
            }
        }

        String[] updatedExceptions = currentExceptionsList.toArray(new String[currentExceptionsList.size()]);
        Log.info(thisClass, method, "New exception list: " + Arrays.toString(updatedExceptions));
        setIgnoredServerExceptions(updatedExceptions);
    }

    private List<String> getCurrentExceptionsList() {
        String thisMethod = "getCurrentExceptionsList";
        String[] currentExceptions = getIgnoredServerExceptions();
        if (currentExceptions == null) {
            currentExceptions = new String[0];
        }
        Log.info(thisClass, thisMethod, "Current exception list: " + Arrays.toString(currentExceptions));

        List<String> currentExceptionsList = new ArrayList<String>(Arrays.asList(currentExceptions));
        return currentExceptionsList;
    }

    private void updateIgnoredExceptionList(List<String> newExceptionsList) {
        String thisMethod = "updateIgnoredExceptionList";
        String[] updatedExceptions = newExceptionsList.toArray(new String[newExceptionsList.size()]);
        Log.info(thisClass, thisMethod, "New exception list: " + Arrays.toString(updatedExceptions));
        setIgnoredServerExceptions(updatedExceptions);
    }

    protected LibertyServer chooseServerFromServerNameList(String[] serverNameList) {
        String thisMethod = "chooseServerFromServerNameList";
        LibertyServer chosenServer = null;
        for (String aServer : serverNameList) {
            chosenServer = LibertyServerFactory.getLibertyServer(aServer);
        }
        Log.info(thisClass, thisMethod, "ServerType: " + getServerType());
        return chosenServer;
    }

    protected LibertyServer getLibertServerFromServerName(String requestedServer) {
        return LibertyServerFactory.getLibertyServer(requestedServer);
    }

    public void setOriginalServerXmlName(String serverXML) {
        if (serverXML != null) {
            origServerXml = buildFullServerConfigPath(server, serverXML);
        } else {
            origServerXml = server.getServerRoot() + File.separator + "server.xml";
        }
    }

    public void setServerNameAndHostIp() throws UnknownHostException {
        String thisMethod = "setServerNameAndHostIp";
        InetAddress addr = InetAddress.getLocalHost();
        serverHostName = addr.getHostName();
        serverCanonicalHostName = addr.getCanonicalHostName();
        serverHostIp = addr.toString().split("/")[1];
        Log.info(thisClass, thisMethod, "IP address: " + serverHostIp);
        Log.info(thisClass, thisMethod, "Hostname: " + serverHostName);
        Log.info(thisClass, thisMethod, "Canonical Hostname: " + serverCanonicalHostName);
    }

    /**
     * Sets a mark at the end of each of the three server logs. Sets each log up for any kind of ensuing search.
     */
    public void setMarkToEndOfLogs() {
        String methodName = "setMarkToEndOfLogs";
        if (!server.isStarted()) {
            return;
        }
        try {
            Log.info(thisClass, methodName, "setting marks for: " + server.getServerName());
            server.setMarkToEndOfLog(); // console.log
            server.setMarkToEndOfLog(server.getMatchingLogFile("messages.log"));
            server.setMarkToEndOfLog(server.getMatchingLogFile("trace.log"));
        } catch (Exception e) {
            Log.error(thisClass, methodName, e, "Failure setting the mark at the end of one or more of the server logs.");
        }
    }

    public int getRetryTimeoutCount() {
        return retryTimeoutCount;
    }

    public void setOverrideRestartWaitTime(int inOverrideRestartWaitTime) {
        overrideRestartWaitTime = inOverrideRestartWaitTime;
    }

    public int getOverrideRestartWaitTime() {
        return overrideRestartWaitTime;
    }

    /**
     * Starts the current server using the server configuration file provided.
     *
     * @param checkApps - List of apps to be validated as ready upon server start
     * @param waitForMessages - List of regular expressions to be waited for upon server start
     * @param reportViaJunit - boolean indicating whether failures should be reported via JUnit or if we should just
     *            log a message
     */
    public void startServer(String serverXml, String testName, List<String> checkApps, List<String> waitForMessages, boolean reportViaJunit, int[] requiredPorts) throws Exception {
        String thisMethod = "startServer";
        msgUtils.printMethodName(thisMethod);

        addAppsToValidate(checkApps);

        lastServer = buildFullServerConfigPath(serverXml);
        copyNewServerConfig(lastServer, testName);

        if (requiredPorts != null) {
            for (Integer port : requiredPorts) {
                // port timeout is 4 minutes - pass in seconds (routine will add one 5 second wait
                //              checkPortsOpen(Constants.PORT_TIMED_WAIT_TIMEOUT + 1, port);
            }
        }
        waitForMessages = addMiscStartMessages(waitForMessages, checkApps, testName);
        startServerWorker(testName, waitForMessages, reportViaJunit, 1);
    }

    public void startServerWorker(String testName, List<String> waitForMessages, boolean reportViaJunit, int tryNum) throws Exception {

        String thisMethod = "startServerWorker";
        int tryLimit = 2;
        try {
            startServerBasedOnTestName(testName);
            if (tryNum < tryLimit) {
                validateStartMessages(waitForMessages, false);
            } else {
                validateStartMessages(waitForMessages, reportViaJunit);
            }
        } catch (Exception e) {
            Log.error(thisClass, thisMethod, e, "Something went wrong trying to start the server (exception thrown)");
            if (tryNum >= tryLimit) {
                throw e;
            }
            // if we haven't exceeded the retry count, try to stop and restart the server
            // update the retryTimeoutCount for retrys only, not when we've exceeded our retry count
            retryTimeoutCount += 1;
            retryStartServer(testName, waitForMessages, reportViaJunit, tryNum + 1);
        }

        if (tryNum < tryLimit) {
            if (doesLogContainKnownIntermittentStartupFailures()) {
                Log.info(thisClass, thisMethod, "Something went wrong trying to start the server");
                retryStartServer(testName, waitForMessages, reportViaJunit, tryNum + 1);
            }
        }
    }

    private void addAppsToValidate(List<String> checkApps) {
        String thisMethod = "addAppsToValidate";
        // setup list of apps to verify running state of
        if (checkApps != null && !checkApps.isEmpty()) {
            for (String app : checkApps) {
                Log.info(thisClass, thisMethod, "Adding " + app + " to list of apps to wait for");
                server.addInstalledAppForValidation(app);
            }
        } else {
            Log.info(thisClass, thisMethod, "No additional start messages to search for");
        }
    }

    private void startServerBasedOnTestName(String testName) throws Exception {

        String thisMethod = "startServerBasedOnTestName";
        Log.info(thisClass, thisMethod, "Starting server: " + serverName);

        if (testName != null && testName.equals("skip")) {
            server.startServer(true, false);
        } else {
            // startserver automatically validates that any added apps have actually started
            server.startServer();
        }
    }

    protected boolean doesLogContainKnownIntermittentStartupFailures() throws Exception {

        boolean found = false;
        // TODO: Add other exceptions that we think a retry is appropriate for
        List<String> failureMsgs = Arrays.asList("CWWKE0701E", "java.lang.NoClassDefFoundError", "Unable to establish loopback",
                                                 "com.ibm.wsspi.channelfw.exception.ChannelException");
        for (String msg : failureMsgs) {
            List<String> msgFound = server.findStringsInLogs(msg);
            if (msgFound != null && !msgFound.isEmpty()) {
                found = true;
                Log.info(thisClass, "checkForIntermittentStartupFailures", "This failure was encountered during startup: " + msgFound);
            }
        }

        return found;
    }

    protected void retryStartServer(String testName, List<String> waitForMessages, boolean reportViaJunit, int tryNum) throws Exception {
        String thisMethod = "retryStartServer";
        Log.info(thisClass, thisMethod, "Retry the start to avoid failures on truly intermittent errors");
        stopFailedServer();
        startServerWorker(testName, waitForMessages, reportViaJunit, tryNum);

    }

    protected void stopFailedServer() throws Exception {
        // stop the server - don't check for error messages - we know there were some, but we're going to try to start again
        // don't use server.restartServer as it'll look for error messages, ...
        server.stopServer(".*");
    }

    protected List<String> addMiscStartMessages(List<String> startMessages, List<String> checkApps, String testName) {
        if (startMessages == null) {
            startMessages = new ArrayList<String>();
        }
        if (checkForSecurityReady) {
            startMessages.add("CWWKS0008I");
            // reset check flag - caller has to make us skip it each time
            resetCheckForSecurityReady();
        }

        startMessages = addAppStartedMessages(startMessages, checkApps);
        return startMessages;
    }

    /**
     * Create a list of Application start messages based on the apps that are installed
     *
     * @return list of installed app started successfully messages
     */
    public List<String> addInstalledAppStartedMessages() {
        return addInstalledAppStartedMessages(null);
    }

    /**
     * Add Application start messages based on the apps that are installed
     *
     * @return updated list of start messages
     */
    public List<String> addInstalledAppStartedMessages(List<String> startMessages) {
        Set<String> appSet = server.listAllInstalledAppsForValidation();
        if (appSet == null) {
            return startMessages;
        }
        List<String> apps = new ArrayList<String>(appSet);
        return addAppStartedMessages(startMessages, apps);

    }

    /**
     * Add Application start messages based on the list of applications passed in
     *
     * @return updated list of start messages
     */
    public List<String> addAppStartedMessages(List<String> startMessages, List<String> checkApps) {
        if (startMessages == null) {
            startMessages = new ArrayList<String>();
        }
        if (checkApps != null) {
            for (String app : checkApps) {
                Log.info(thisClass, "addMiscStartMessages", "checkApp is: " + app);
                if (app.equals("SAML_Demo")) {
                    startMessages.add(Constants.MSG_APP_READY + ":.*samldemo");
                } else {
                    if (app.equals("oauthtaidemo")) {
                        startMessages.add(Constants.MSG_APP_READY + ":.*oauth2tai");
                    } else {
                        startMessages.add(Constants.MSG_APP_READY + ":.*" + app);
                    }
                }
            }
        }
        for (String msg : startMessages) {
            Log.info(thisClass, "addAppStartedMessages", "Start Message to check: " + msg);
        }
        return startMessages;
    }

    /**
     * Dynamically reconfigures the running server to use the new server configuration provided. No additional messages
     * will be waited for, the server will not be restarted, and any message wait failures will not be reported through
     * JUnit.
     *
     * @param newServerXml
     * @throws exception
     */
    public void reconfigServer(String newServerXml) throws Exception {
        reconfigServer(newServerXml, null, Constants.NO_EXTRA_MSGS, false, Constants.NO_JUNIT_REPORTING);
    }

    /**
     * Dynamically reconfigures the running server to use the new server configuration provided. No additional messages
     * will be waited for, the server will not be restarted, and any message wait failures will be reported through
     * JUnit.
     *
     * @param newServerXml
     * @param testName - Test name that should be included in messages
     * @throws exception
     */
    public void reconfigServer(String newServerXml, String testName) throws Exception {
        reconfigServer(newServerXml, testName, Constants.NO_EXTRA_MSGS, false, Constants.JUNIT_REPORTING);
    }

    /**
     * Dynamically reconfigures the running server to use the new server configuration provided. Waits for all expected messages
     * indicating that the server configuration was updated.
     *
     * @param newServerXml
     * @param testName - Test name that should be included in messages
     * @param waitForMessages - List of regular expressions to be waited for upon server update/restart
     * @param reportViaJunit - boolean indicating whether failures should be reported via JUnit or if we should just
     *            log a message
     * @throws exception
     */
    public void reconfigServer(String newServerXml, String testName, List<String> waitForMessages, boolean reportViaJunit) throws Exception {
        reconfigServer(newServerXml, testName, waitForMessages, false, reportViaJunit);
    }

    /**
     * Reconfigures the current server to use the new server configuration provided. Waits for all expected messages
     * indicating that the server configuration was updated.
     *
     * @param newServerXml
     * @param testName - Test name that should be included in messages
     * @param waitForMessages - List of regular expressions to be waited for upon server update/restart
     * @param restartServer - boolean indicating whether the server should be restarted
     * @param reportViaJunit - boolean indicating whether failures should be reported via JUnit or if we should just
     *            log a message
     * @throws exception
     */
    public void reconfigServer(String newServerXml, String testName, List<String> waitForMessages, boolean restartServer, boolean reportViaJunit) throws Exception {
        String thisMethod = "reconfigServer";
        msgUtils.printMethodName(thisMethod);

        Log.info(thisClass, thisMethod, "************** Starting server.xml update for: " + testName);
        try {
            if (!isReconfigNecessary(newServerXml)) {
                return;
            }

            if (restartServer) {
                restartServer(newServerXml, testName, lastCheckApps, waitForMessages, reportViaJunit);
                return;
            }

            updateAndRestartServer(newServerXml, testName, reportViaJunit, waitForMessages);

            System.err.flush();

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        } finally {
            Log.info(thisClass, thisMethod, "************** Completed server.xml update: " + testName);
        }
    }

    private boolean isReconfigNecessary(String newServerConfigFile) {
        String thisMethod = "isReconfigNecessary";
        if (!server.isStarted()) {
            Log.info(thisClass, thisMethod, "Server " + serverName + " is not started - reconfig not necessary");
            return false;
        }
        if (isConfigFileNullOrIdenticalToCurrentConfig(newServerConfigFile)) {
            return false;
        }
        return true;
    }

    private boolean isConfigFileNullOrIdenticalToCurrentConfig(String copyFromFile) {
        String thisMethod = "isConfigFileNullOrIdenticalToCurrentConfig";
        if (copyFromFile == null) {
            Log.info(thisClass, thisMethod, "No new server xml specified for: " + serverName + " - reconfig not necessary");
            return true;
        }
        String fullServerPath = buildFullServerConfigPath(copyFromFile);
        Log.info(thisClass, thisMethod, "current server config: " + fullServerPath + " - last server config: " + lastServer);
        if (configFileMatchesLastConfigFile(fullServerPath)) {
            Log.info(thisClass, thisMethod, "The SAME SERVER CONFIG file is being used - reconfig not necessary");
            return true;
        }
        return false;
    }

    private boolean configFileMatchesLastConfigFile(String fullServerConfigPath) {
        return fullServerConfigPath.equals(lastServer);
    }

    private void updateAndRestartServer(String copyFromFile, String testName, Boolean reportViaJunit, List<String> startMessages) throws Exception {
        // mark the end of the log - all work will occur after this point
        server.setMarkToEndOfLog();

        // Update the server config by replacing the server.xml
        // If the copyFromFile has a slash in it, assume the full path was given. Otherwise, assume the same server's config subdirectory.
        String fullServerPath = buildFullServerConfigPath(copyFromFile);
        lastServer = fullServerPath;

        if (Constants.FORCE_SERVER_RESTART) {
            restartServer(copyFromFile, testName, lastCheckApps, startMessages, reportViaJunit);
            return;
        }

        copyNewServerConfig(fullServerPath, testName);
        addIgnoredServerException(MessageConstants.CWWKG0014E_XML_PARSER_ERROR);
        waitForServer(testName, startMessages, reportViaJunit);
    }

    /**
     * Restarts the current server using the server configuration file provided.
     *
     * @param serverXml
     * @param testName - Test name that should be included in messages
     * @param checkApps - List of apps to be validated as ready upon server start
     * @param waitForMessages - List of regular expressions to be waited for upon server start
     * @param reportViaJunit - boolean indicating whether failures should be reported via JUnit or if we should just
     *            log a message
     * @throws exception
     */
    public void restartServer(String serverXml, String testName, List<String> checkApps, List<String> waitForMessages, boolean reportViaJunit) throws Exception {
        server.setMarkToEndOfLog();
        server.stopServer(ignoredServerExceptions);
        // Make sure that all files are backed up to the timestamped server directory under autoFVT
        server.waitForStringInLog("Successfully recovered server");
        // Remove server configs that had been backed up to the testServers/ directory
        removeServerConfigFiles();

        startServer(serverXml, testName, checkApps, waitForMessages, reportViaJunit, null);

        if (checkApps != null) {
            lastCheckApps.addAll(checkApps);
        }
    }

    /**
     * Restores the server to its original configuration.
     */
    public void restoreServer() throws Exception {
        String thisMethod = "restoreServer";
        msgUtils.printMethodName(thisMethod);

        if (origServerXml != null) {
            Log.info(thisClass, thisMethod, "Restoring original file: " + origServerXml);
            reconfigServer(origServerXml);
        } else {
            Log.info(thisClass, thisMethod, "Original server file not set");
        }
    }

    /**
     * Stops the server if it is currently running.
     */
    public void stopServer() throws Exception {
        if (server != null && server.isStarted()) {
            // ignore quiesce issues during server shutdown
            addIgnoredServerExceptions(MessageConstants.CWWKE1102W_QUIESCE_WARNING, MessageConstants.CWWKE1106W_QUIESCE_LISTENERS_NOT_COMPLETE,
                                       MessageConstants.CWWKE1107W_QUIESCE_WAITING_ON_THREAD);
            // sometimes a port is in use during startup, but is available when tests run - the tests will have issues if
            // the port remains blocked and will generate their own errors - ignore this hiccup during the shutdown checks.
            addIgnoredServerException(MessageConstants.CWWKO0221E_PORT_IN_USE);
            // ignore shutdown timing issues
            addIgnoredServerExceptions(MessageConstants.CWWKO0227E_EXECUTOR_SERVICE_MISSING);
            // ignore ssl restart warnings - if they caused problems, tests would also be failing
            addIgnoredServerExceptions(MessageConstants.SSL_NOT_RESTARTED_PROPERLY);
            // ignore ssl message - runtime retries and can proceed (sometimes) when it can't tests will fail when they don't get the correct response
            server.addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0801E_UNABLE_TO_INIT_SSL));

            server.stopServer(ignoredServerExceptions);
        }
        unInstallCallbackHandler(callback, callbackFeature);
    }

    /**
     * Restores the original server config and sets the mark to the end of the logs.
     */
    public void endTestServer() throws Exception {
        if (server == null) {
            return;
        }
        try {
            if (getRestoreServerBetweenTests()) {
                restoreServer();
            } else {
                Log.info(thisClass, "endTestServer", "Class requests that server is NOT reconfigured between testcases");
            }
            // Set the mark between tests so we can find config output at the very beginning of the next test if needed
            setMarkToEndOfLogs();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    /**
     * Stops the running server and copies the original server configuration file back to the server root as
     * server.xml.
     */
    public void tearDownServer() throws Exception {
        String thisMethod = "tearDownServer";
        try {
            if (server != null) {
                stopServer();
            } else {
                Log.info(thisClass, thisMethod, "Server instance for \"" + origServerXml + "\" was null");
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
            throw e;
        } finally {
            copyNewServerConfig(origServerXml, thisMethod);
        }
    }

    /**
     * Waits for the server to complete a configuration update. Also waits for all messages included in startMessages
     * to appear in the log.
     *
     * @param testName - Test name that should be included in messages
     * @param reportViaJunit - boolean indicating whether failures should be reported via JUnit or if we should just
     *            log a message
     * @param waitForMessages - List of regular expressions to be waited for
     * @throws Exception
     */
    public void waitForServer(String testName, List<String> waitForMessages, boolean reportViaJunit) throws Exception {
        String thisMethod = "waitForServer";
        try {
            // make sure that the server detected the update
            waitForUpdateDetectedMessage();

            boolean isUpdateRequired = isUpdateRequired();
            if (isUpdateRequired) {
                waitForSuccessfulUpdateAndStartMessages(reportViaJunit, waitForMessages);
            } else {
                Log.info(thisClass, thisMethod, "noUpdateMsg: " + isUpdateRequired);
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
     * Waits for the CWWKG0016I message saying server update has started.
     */
    private void waitForUpdateDetectedMessage() {
        String thisMethod = "waitForUpdateDetectedMessage";
        if (server.waitForStringInLogUsingMark(CWWKG0016I_STARTING_SERVER_UPDATE) != null) {
            Log.info(thisClass, thisMethod, "Server detected the update");
        } else {
            Log.info(thisClass, thisMethod, "Server did NOT detect the update - will continue waiting for additional messages");
        }
    }

    /**
     * Returns whether the CWWKG0018I message saying the server was not updated appears in the log.
     */
    private boolean isUpdateRequired() {
        return server.verifyStringNotInLogUsingMark(CWWKG0018I_SERVER_CONFIG_NOT_UPDATED, 2500) == null;
    }

    /**
     * Waits for the CWWKG0017I message saying that the server was successfully updated, then waits for each of the additional
     * messages provided.
     */
    private void waitForSuccessfulUpdateAndStartMessages(boolean reportViaJunit, List<String> startMessages) throws Exception {
        String thisMethod = "waitForSuccessfulUpdateAndStartMessages";

        // Wait for "server configuration was successfully updated in X seconds" message
        String updateMsg = "";
        if (overrideRestartWaitTime == 0) {
            updateMsg = server.waitForStringInLogUsingMark(CWWKG0017I_SERVER_CONFIG_UPDATED);
        } else {
            updateMsg = server.waitForStringInLogUsingMark(CWWKG0017I_SERVER_CONFIG_UPDATED, overrideRestartWaitTime);
        }
        Log.info(thisClass, thisMethod, "Server update msg: " + updateMsg);
        assertNotNull("Did not encounter the CWWKG0017I message saying the server configuration was successfully updated.", updateMsg);

        waitForAppsToReboot();
        waitForSSLRestart();
        validateStartMessages(startMessages, reportViaJunit);
    }

    private void waitForAppsToReboot() {
        Set<String> apps = server.listAllInstalledAppsForValidation();
        for (String app : apps) {
            if (!wasAppStopped(app)) {
                // This app does not appear to have been stopped, so don't do any further checking to see if it's been restarted
                continue;
            }
            waitForAppToBeReady(app);
        }
    }

    private boolean wasAppStopped(String app) {
        String method = "wasAppStopped";
        Log.info(thisClass, method, "Waiting to see if app [" + app + "] was stopped");
        boolean appStillRunning = server.verifyStringNotInLogUsingMark(Constants.CWWKZ0009I_APP_STOPPED_SUCCESSFULLY + ".*" + app, 2000) == null;
        if (appStillRunning) {
            Log.info(thisClass, method, "App [" + app + "] does not appear to have been stopped");
            return false;
        }
        Log.info(thisClass, method, "App [" + app + "] was found to have been stopped");
        return true;
    }

    private boolean waitForAppToBeReady(String app) {
        String method = "waitForAppToBeReady";
        Log.info(thisClass, method, "Waiting for app [" + app + "] to be ready");
        boolean appNotReady = server.verifyStringNotInLogUsingMark(Constants.CWWKZ0003I_APP_UPDATED + ".*" + app, 2000) == null;
        if (appNotReady) {
            Log.warning(thisClass, "Failed to find message saying that app [" + app + "] was ready.");
            return false;
        }
        Log.info(thisClass, method, "App [" + app + "] was found to be ready");
        return true;
    }

    private void waitForSSLRestart() throws Exception {

        String thisMethod = "waitForSSLRestart";
        Log.info(thisClass, thisMethod, "Checking for SSL restart for server: " + server.getServerName());

        // look for the "CWWKO0220I: TCP Channel defaultHttpEndpoint-ssl has stopped listening for requests on host " message
        // if we find it, then wait for "CWWKO0219I: TCP Channel defaultHttpEndpoint-ssl has been started and is now listening for requests on host"
        String sslStopMsg = server.waitForStringInLogUsingMark("CWWKO0220I:.*defaultHttpEndpoint-ssl.*", 500);
        if (sslStopMsg != null) {
            String sslStartMsg = server.waitForStringInLogUsingMark("CWWKO0219I:.*defaultHttpEndpoint-ssl.*");
            if (sslStartMsg == null) {
                Log.warning(thisClass, "SSL may not have started properly - future failures may be due to this");
                sslWaitTimeoutCount += 1;
            } else {
                Log.info(thisClass, thisMethod, "SSL appears have restarted properly");
            }
        } else {
            Log.info(thisClass, thisMethod, "Did not detect a restart of the SSL port");
            sslWaitTimeoutCount += 1;
        }

    }

    public int getSslWaitTimeoutCount() {
        return sslWaitTimeoutCount;
    }

    /**
     * Searches for a message string in the specified server log
     *
     * @param expected
     *            - a validationMsg type to search (contains the log to search and the string to search for)
     * @throws exception
     */
    public void waitForValueInServerLog(validationData expected) throws Exception {
        String thisMethod = "waitForValueInServerLog";
        try {
            Log.info(thisClass, thisMethod, "checkType is: " + expected.getCheckType());

            String logName = getGenericLogName(expected.getWhere());
            String expectedValue = expected.getValidationValue();
            Log.info(thisClass, thisMethod, "Searching for [" + expectedValue + "] in " + logName);

            String searchResult = server.waitForStringInLogUsingMark(expectedValue, server.getMatchingLogFile(logName));
            msgUtils.assertTrueAndLog(thisMethod, expected.getPrintMsg() + " Was expecting to find " + expectedValue + " in " + logName + ", but did not find it there!",
                                      searchResult != null);
            Log.info(thisClass, thisMethod, "Found message: " + expectedValue);

        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Failure searching for string [" + expected.getValidationValue() + "] in " + expected.getWhere());
            throw e;
        }
    }

    /** TODO *************************************** Path-based methods *****************************************/

    protected String[] getServerDirectoryNames() {
        File file = new File("./publish/servers");
        String[] directories = file.list(new FilenameFilter() {
            @Override
            public boolean accept(File current, String name) {
                return new File(current, name).isDirectory();
            }
        });
        return directories;
    }

    /**
     * Builds the fully qualified runtime server path.
     */
    public String getServerFileLoc() throws Exception {
        try {
            return (new File(server.getServerConfigurationPath().replace('\\', '/'))).getParent();
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
    }

    /**
     * Builds the fully qualified path of the {@code <Install>/build.image/wlp/usr/servers/<serverName>/testServers} directory
     */
    public File getTestServerDir() throws Exception {
        try {
            return new File(getServerFileLoc() + "/testServers");
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
    }

    /**
     * Deletes the server config files in the {@code build.image/wlp/usr/servers/<serverName>/testServers} directory (used before
     * starting a new server).
     */
    public void removeServerConfigFiles() throws Exception {
        File testServerDir = getTestServerDir();
        if (testServerDir.exists()) {
            FileUtils.recursiveDelete(testServerDir);
        }
    }

    /**
     * Copy the specified server config file to server.xml. Make a copy of the server config in the testServers sub-directory for
     * debug use later.
     *
     * @param copyFromFile - File to copy into the server's root directory as server.xml
     */
    public void copyNewServerConfig(String copyFromFile, String testName) throws Exception {
        String thisMethod = "copyNewServerConfig";
        if (copyFromFile == null || copyFromFile.isEmpty()) {
            Log.info(thisClass, thisMethod, "Provided config file is null or empty; server config will not be changed");
            return;
        }
        File testServerDir = getTestServerDir();
        String serverFileLoc = getServerFileLoc();
        try {
            String testPrintName = testName;
            if (testName == null) {
                testPrintName = "default";
            }
            mergeAndCopyNewServerConfig(copyFromFile, testServerDir, serverFileLoc, testPrintName);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
    }

    private void mergeAndCopyNewServerConfig(String newServerConfigFile, File testServerDir, String serverFileLoc, String testPrintName) throws Exception {
        String thisMethod = "mergeAndCopyNewServerConfig";
        Log.info(thisClass, thisMethod, "Merging server.xml for '" + newServerConfigFile 
                + "' with test server directory '" + testServerDir 
                + "' and server file location '" + serverFileLoc +"'.");
        CommonMergeTools merge = new CommonMergeTools();
        if (merge.mergeFile(newServerConfigFile, server.getServerSharedPath() + "config", serverFileLoc)) {
            newServerConfigFile = newServerConfigFile.replace(".xml", "_Merged.xml");
        }

        String backupConfigName = testPrintName + "_server.xml";
        Log.info(thisClass, thisMethod, "Backing up server.xml for test: " + testPrintName + " to " + testServerDir.toString() + "/" + backupConfigName);
        LibertyFileManager.copyFileIntoLiberty(server.getMachine(), testServerDir.toString(), backupConfigName, newServerConfigFile);

        Log.info(thisClass, thisMethod, "Copying: " + newServerConfigFile + " to " + serverFileLoc);
        LibertyFileManager.copyFileIntoLiberty(server.getMachine(), serverFileLoc, "server.xml", newServerConfigFile);
    }

    protected String buildFullServerConfigPath(String copyFromFile) {
        String fullServerPath;
        if (copyFromFile.contains(File.separator)) {
            fullServerPath = copyFromFile;
        } else {
            fullServerPath = buildFullServerConfigPath(server, copyFromFile);
        }
        return fullServerPath;
    }

    /**
     * Builds and returns the absolute path to the specified file within the configs/ directory under the given
     * server's root directory.
     *
     * @param theServer - The server instance containing the specified file
     * @param fileName - Name of the file within the configs/ directory to build the path for
     * @return The absolute path to the specified file within the server's configs/ directory
     */
    public String buildFullServerConfigPath(LibertyServer theServer, String fileName) {
        String serverFileName = theServer.getServerRoot() + File.separator + "configs" + File.separator + fileName;
        Log.info(thisClass, "buildFullServerConfigPath", "serverFileName: " + serverFileName);
        return serverFileName;
    }

    /**
     * Builds and returns the absolute path of the default server configuration directory.
     *
     * @return The absolute path to the server configuration directory
     */
    public String buildFullDefaultServerConfigPath() {
        String serverConfigPath = server.getServerConfigurationPath();
        Log.info(thisClass, "buildFullDefaultServerConfigPath", "serverConfigPath: " + serverConfigPath);
        return serverConfigPath;
    }

    /**
     * Builds and returns the absolute path to the imports/ directory under the given server's root directory.
     *
     * @param theServer
     * @return The absolute path to the server's imports/ directory
     */
    public String buildFullServerImportsPath(LibertyServer theServer) {
        String serverFileName = theServer.getServerRoot() + File.separator + "imports" + File.separator;
        Log.info(thisClass, "buildFullServerImportsPath", "serverFileName: " + serverFileName);
        return serverFileName;
    }

    /**
     * Searches and waits for message strings in the default log and reports success/failure of the search either via a
     * message and possibly JUnit reporting.
     *
     * @param waitForMessages - List of regular expression strings to wait for in the default log
     * @param reportViaJunit - boolean indicating whether failures should be reported via JUnit or if we should just
     *            log a message
     */
    public void validateStartMessages(List<String> waitForMessages, boolean reportViaJunit) throws Exception {
        validateStartMessages(waitForMessages, reportViaJunit, true);
    }

    /**
     * Searches and waits for message strings in the default log and reports success/failure of the search either via a
     * message and possibly JUnit reporting. If expectedResult is false, the passed messages are expected NOT to be
     * found.
     *
     * @param waitForMessages - List of regular expression strings to wait for in the default log
     * @param reportViaJunit - boolean indicating whether failures should be reported via JUnit or if we should just
     *            log a message
     * @param expectedResult - If true, the messages specified are expected to be found. Otherwise, the passed messages
     *            are expected NOT to be found.
     * @throws Exception
     */
    public void validateStartMessages(List<String> waitForMessages, boolean reportViaJunit, boolean expectedResult) throws Exception {
        String thisMethod = "validateStartMessages";

        if (waitForMessages == null) {
            Log.info(thisClass, thisMethod, "No additional messages need to be located for this server start/restart");
        } else {
            for (String searchMsg : waitForMessages) {
                waitForMessage(searchMsg, reportViaJunit, expectedResult);
            }
        }
    }

    private void waitForMessage(String searchMsg, boolean reportViaJunit, boolean expectedResult) throws Exception {
        String thisMethod = "waitForMessage";
        Log.info(thisClass, thisMethod, "Server test message verification: Searching for message: " + searchMsg);

        String locatedMsg = server.waitForStringInLogUsingMark(searchMsg);
        Log.info(thisClass, thisMethod, "Message content found is: " + locatedMsg);

        if (reportViaJunit) {
            String msg = "Searched the default server log and did NOT find expected message: \"" + searchMsg + "\" after waiting default timeout";
            if (!expectedResult) {
                msg = "Searched the default server log and found unexpected message: \"" + searchMsg + "\" after waiting default timeout";
            }
            msgUtils.assertAndLog(thisMethod, msg, locatedMsg != null, expectedResult);
        } else {
            if (expectedResult) {
                if (locatedMsg == null) {
                    throw new Exception("Expected to find: " + searchMsg + " but did not - not reporting via Junit");
                }
            } else {
                if (locatedMsg != null) {
                    throw new Exception("Expected NOT to find: " + searchMsg + " but did - not reporting via Junit");
                }
            }
        }
    }

    /**
     * Searches for a message string in the specified server log.
     *
     * @param expected - a validationMsg type to search (contains the log to search and the string to search for)
     * @throws Exception
     */
    public void validateWithServerLog(String checkType, String where, String errorMsg, String valueToCheck) throws Exception {
        String thisMethod = "validateWithServerLog";

        try {
            Log.info(thisClass, thisMethod, "checkType is: " + checkType);
            String searchLogName = getGenericLogName(where);
            Log.info(thisClass, thisMethod, "Checking log: ", searchLogName);
            Log.info(thisClass, thisMethod, "Searching for: " + valueToCheck);

            boolean result = false;
            boolean expectedResult = true;
            String additionalErrorMsg = "";

            if (checkType != null && checkType.equals(Constants.STRING_DOES_NOT_CONTAIN)) {
                // Expecting NOT to find the value in the server logs
                Log.info(thisClass, thisMethod, "NOT expecting to find the specified value");
                expectedResult = false;
                // Shorten the timeout to avoid waiting the full 2-4 minutes searching for a message we expect NOT to find
                result = (server.waitForStringInLog(valueToCheck, 10 * 1000, server.getMatchingLogFile(searchLogName)) != null);
                additionalErrorMsg = "Did NOT expect to find " + valueToCheck + " in " + searchLogName + ", but did find it.";
            } else {
                // Expecting the value to be found in the server logs
                Log.info(thisClass, thisMethod, "Expecting to find the specified value");
                result = (server.waitForStringInLogUsingMark(valueToCheck, server.getMatchingLogFile(searchLogName)) != null);
                additionalErrorMsg = "Was expecting to find " + valueToCheck + " in " + searchLogName + " but did not find it there!";
            }

            msgUtils.assertAndLog(thisMethod, errorMsg + " " + additionalErrorMsg, result, expectedResult);
            Log.info(thisClass, thisMethod, (expectedResult ? "Found" : "Did not find") + " message: " + valueToCheck);
        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Failure searching for string in " + where);
            throw e;
        }
    }

    /**
     * Searches for and returns the line containing message string in the specified server log.
     *
     * @param valueToCheck - identified unique string to determine the line containing the string
     * @param where - which log to search for
     * @exception - throws error if no string is found
     *
     * @return - returns the string of the line found within the specified server log
     *
     */
    public String searchValueInServerLog(String valueToCheck, String where) throws Exception {
        String thisMethod = "searchValueInServerLog";
        String val = null;
        RemoteFile outputFile = server.getMatchingLogFile(getGenericLogName(where));
        try {
            val = server.waitForStringInLogUsingMark(valueToCheck, outputFile);
            msgUtils.assertAndLog(thisMethod,
                                  "Was expecting to find " + valueToCheck + " in " + where + " but did not find it there!",
                                  val != null, true);
        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Failure searching for " + valueToCheck + " in " + where);
            return val;
        }
        return val;
    }

    /**
     * Searches for passwords in the server logs.
     *
     * @param expected - a validationMsg type to search (contains the log to search and the string to search for)
     * @throws Exception
     */
    public int searchForPasswordsInLogs(String where) throws Exception {
        String thisMethod = "searchForPasswordsInLogs";

        Log.info(thisClass, thisMethod, "Entering: " + thisMethod);
        int count = 0;

        try {

            if (passwordsUsedWithServer != null && passwordsUsedWithServer.length != 0) {
                String searchLogName = getGenericLogName(where);
                Log.info(thisClass, thisMethod, "Checking log: ", searchLogName + " for passwords");
                for (String pw : passwordsUsedWithServer) {

                    server.resetLogOffsets();
                    Log.info(thisClass, thisMethod, "Searching for: " + pw);
                    if (server.waitForStringInLog(pw, 0, server.getMatchingLogFile(searchLogName)) == null) {
                        Log.info(thisClass, thisMethod, "Did NOT find: " + pw + " in log: " + searchLogName);
                        count++;
                    } else {
                        throw new RuntimeException("Found password " + pw + " in the " + searchLogName + " and should NOT have!");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Failure searching for string in " + where);
            throw e;
        }
        return count;
    }

    /**
     * Returns the generic name of the the log name specified.
     *
     * @param specificLogName
     * @return - One of "messages.log," "console.log," or "trace.log." Default return value is "messages.log."
     */
    public String getGenericLogName(String specificLogName) {
        Log.info(thisClass, "getGenericLogName", "Log name passed in is: " + specificLogName);
        if (specificLogName.contains(Constants.MESSAGES_LOG)) {
            return Constants.MESSAGES_LOG;
        }
        if (specificLogName.contains(Constants.CONSOLE_LOG)) {
            return Constants.CONSOLE_LOG;
        }
        if (specificLogName.contains(Constants.TRACE_LOG)) {
            return Constants.TRACE_LOG;
        }
        return specificLogName;
    }

    protected void checkPortsOpen(boolean retry, Integer port) {

    }

    protected void checkPortsOpen(Integer waitTime, Integer port) {

        Integer retryCount = (waitTime / 5) + 1;
        ServerSocket socket = null;
        try {
            Log.info(thisClass, "checkPortsOpen", "Checking on port: " + port);
            // Create unbounded socket
            socket = new ServerSocket();
            // This allows the socket to close and others to bind to it even if its in TIME_WAIT state
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(port));
        } catch (Exception ex) {
            Log.error(thisClass, "checkPortsOpen", ex, "port " + port + " is currently bound");
//            printProcessHoldingPort(getHttpDefaultPort());
            if (retryCount > 0) {

                Log.info(thisClass, "checkPortsOpen", "Waiting 5 seconds and trying again");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    // Not a lot to do
                }
                // Do this out of the try block, even if we are interrupted we want to try once more
                checkPortsOpen(retryCount - 1, port);
            }
        } finally {
            if (null != socket) {
                try {
                    // With setReuseAddress set to true we should free up our socket and allow
                    // someone else to bind to it even if we are in TIME_WAIT state.
                    socket.close();
                } catch (IOException ioe) {
                    // not a lot to do
                }
            }
        }
    }

    public void installCallbackHandler(String callbackHandler, String feature) throws Exception {

        String cbhFullName = "." + File.separator + "publish" + File.separator + "bundles" + File.separator + callbackHandler + ".jar";
        Log.info(thisClass, "installCallbackHandler", "Looking for file: " + cbhFullName);
        File f = new File(cbhFullName);
        if (f.exists()) {
            Log.info(thisClass, "installCallbackHandler", "Installing callback handler: " + callbackHandler);
            server.installUserBundle(callbackHandler);
            callback = callbackHandler;
            Log.info(thisClass, "installCallbackHandler", "Installing feature: " + feature);
            server.installUserFeature(feature);
            callbackFeature = feature;
        }
    }


    public void unInstallCallbackHandler(String callbackHandler, String feature) throws Exception {
        if (feature != null) {
            Log.info(thisClass, "unInstallCallbackHandler", "Un-Installing callback handler feature: " + feature);
            server.uninstallUserFeature(feature);
        }
        if (callback != null) {
            Log.info(thisClass, "unInstallCallbackHandler", "Un-Installing callback handler: " + callbackHandler);
            server.uninstallUserBundle(callbackHandler);
        }
    }


    /** TODO *************************************** Bootstrap utils *****************************************/

    public String getBootstrapPropertiesFilePath() throws Exception {
        String thisMethod = "getBootstrapPropertiesFilePath";
        String bootProps = getServerFileLoc() + "/bootstrap.properties";
        Log.info(thisClass, thisMethod, "Bootstrap property file path: " + bootProps);
        return bootProps;
    }

    public String getBootstrapProperty(String key) throws Exception {
        Properties serverProperties = this.getServer().getBootstrapProperties();
        return serverProperties.getProperty(key, null);
    }

    public String getJvmOptionsFilePath() throws Exception {
        String thisMethod = "getJvmOptionsFilePath";
        String jvmProps = getServerFileLoc() + "/jvm.options";
        Log.info(thisClass, thisMethod, "jvm.options property file path: " + jvmProps);
        return jvmProps;
    }
}
