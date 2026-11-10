/*******************************************************************************
 * Copyright (c) 2015, 2026 IBM Corporation and others.
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
package com.ibm.ws.security.csiv2.fat.common;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.junit.rules.ExternalResource;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.impl.LibertyServerWrapper;
import componenttest.topology.utils.FileUtils;

/**
 * Adapted from OIDC TestServer to work with CSIv2 FAT tests.
 */
@LibertyServerWrapper
public class TestServer extends ExternalResource {

    private final Class<?> thisClass = TestServer.class;
    protected LibertyServer server = null;
    protected String origServerXml = null;
    /*
     * The csiv2 servers are exempt from log validation on stop (they are listed in WS-CD-Open's
     * ant_build/resources/configuration/exemptServersList.txt). The tests deliberately configure combinations that log
     * warnings and errors, such as CWWKS9680W for establishTrustInClient=Never. Passing ".*" ignores all of them.
     */
    public static final String SKIP_LOG_VALIDATION = ".*";

    protected String serverName = null;
    protected String lastServer = null;
    protected Boolean serverWasRestarted = false;
    protected HashSet<String> lastCheckApps = new HashSet<String>();
    protected static String hostName = "localhost";
    protected String thisServerType = Constants.CSIv2_DOWNSTREAM;
    protected Boolean checkForSecurityReady = true;
    protected CommonMessageTools msgUtils = new CommonMessageTools();

    public boolean allTestsRestartServer = false;

    public void setAllTestsRestartServer(Boolean flag) {
        allTestsRestartServer = flag;
    }

    /**
     * @param requestedServer
     * @param serverXML
     * @param testType
     */
    public TestServer(String requestedServer, String serverXML, String testType) {
        server = LibertyServerFactory.getLibertyServer(requestedServer);
        serverName = requestedServer;
        /* handle using a server xml with a name other than server.xml */
        if (serverXML != null) {
            origServerXml = buildFullServerConfigPath(server, serverXML);
        } else {
            origServerXml = server.getServerRoot() + File.separator + "server.xml";
        }
        if (testType.equals(Constants.CSIv2_DOWNSTREAM)) {
            setServerType(Constants.CSIv2_DOWNSTREAM);
        } else if (testType.equals(Constants.CSIv2_UPSTREAM)) {
            setServerType(Constants.CSIv2_UPSTREAM);
        } else if (testType.equals(Constants.CSIv2_INTERMEDIATE)) {
            setServerType(Constants.CSIv2_INTERMEDIATE);
        } else {
            setServerType("OTHER");
        }

        // Set the default ports on the LibertyServer instance to avoid conflicts opening ports.
        server.setHttpDefaultPort(getHttpDefaultPort());
        server.setHttpDefaultSecurePort(getHttpDefaultSecurePort());
    }

    public String buildFullServerConfigPath(LibertyServer theServer, String fileName) {
        // String serverFileName = System.getProperty("user.dir") + File.separator + theServer.getPathToAutoFVTNamedServer() + "configs" + File.separator + fileName;
        String serverFileName = "configs" + File.separator + fileName;
        Log.info(thisClass, "buildFullServerConfigPath", "serverFileName: " + serverFileName);
        return serverFileName;
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

    /**
     * Deletes the server config files in the
     * build.image/wlp/usr/servers/<serverName>/testServers directory (used
     * before starting a new server)
     */
    public void removeServerConfigFiles() throws Exception {
        File testServerDir = getTestServerDir();
        if (testServerDir.exists()) {
            FileUtils.recursiveDelete(testServerDir);
        }
    }

    /**
     * Builds the fully qualified path of the
     * <Install>/build.image/wlp/usr/servers/<serverName>/testServers directory
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
     * Builds the fully qualified runtime server path
     */
    public String getServerFileLoc() throws Exception {
        try {
            return (new File(server.getServerConfigurationPath().replace('\\', '/'))).getParent();

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
    }

    public List<String> getDefaultStartMessages(String testType) {

        List<String> startMsgs = new ArrayList<String>();

        if (testType.equals(Constants.CSIv2_DOWNSTREAM)) {
            // startMsgs.add("Add some useful downstream server to look at during startup");
        }

        return startMsgs;
    }

    public HashSet<String> getDefaultTestApps(String testType) {
        HashSet<String> testApps = new HashSet<String>();
        return testApps;
    }

    public void setSkipSecurityReadyMsg() {
        checkForSecurityReady = false;
    }

    /**
     * Starts the specified server and waits for all required msgs to appear in
     * the log
     *
     * @param serverXML
     *                           - the server config file to copy
     * @param checkApps
     *                           - thi list of applications that should should be waited on
     *                           during a server start
     * @param messages
     *                           - any additional messages that we should wait for in the logs
     *                           (of type validationMsg which also indicates which log should
     *                           be checked)
     * @param testName
     *                           - the test name that should be included in messages
     * @param reportViaJunit
     *                           - boolean indicating if failures should be reportef via junit,
     *                           or if we should just log a message
     * @throws exception
     */
    public void startServer(String serverXML, HashSet<String> checkApps,
                            List<String> startMessages, String testName, Boolean reportViaJunit) throws Exception {

        String thisApp = null;
        String thisMethod = "startServer";
        msgUtils.printMethodName(thisMethod);

        /* setup list of apps to verify running state of */
        if (checkApps != null && !checkApps.isEmpty()) {
            Iterator<String> apps = checkApps.iterator();
            while (apps.hasNext()) {
                thisApp = apps.next();
                Log.info(thisClass, thisMethod, "Adding " + thisApp + " to list of apps to wait for");
                server.addInstalledAppForValidation(thisApp);
            }
        } else {
            Log.info(thisClass, thisMethod, "No additional start messages to search for");
        }

        if (serverXML.contains(File.separator)) {
            lastServer = serverXML;
        } else {
            lastServer = buildFullServerConfigPath(server, serverXML);
        }
        copyNewServerConfig(lastServer, testName);

        Log.info(thisClass, thisMethod, "Starting server: " + serverName);
        if (testName != null && testName.equals("skip")) {
            server.startServer(true, false);
        } else {
            server.startServer();
        }

        if (startMessages == null) {
            startMessages = new ArrayList<String>();
        }
        if (checkForSecurityReady) {
            startMessages.add("CWWKS0008I");
            // reset check flag - caller has to make us skip it each time
            resetCheckForSecurityReady();
        }

//        if (!isInvalidSSLConfigTest(testName)) {
//            startMessages.add("CWWKO0219I.*ssl");
//        }

        validateStartMessages(startMessages, reportViaJunit);
    }

    /**
     * Reconfigures the running server, passes in the server config file passed
     * in and defaulting all other parameters passed into the real
     * reconfigServer routine (testName is set to null, reportViaJunit is set to
     * false, and startMessages is set to null)
     *
     * @param copyFromFile
     *                           - the server config file to copy
     * @param name
     *                           - the test name that should be included in messages
     * @param reportViaJunit
     *                           - boolean indicating if failures should be reported via junit,
     *                           or if we should just log a message
     * @param messages
     *                           - any additional messages that we should wait for in the logs
     *                           (of type validationMsg which also indicates which log should
     *                           be checked)
     * @throws exception
     */
    public void reconfigServer(String copyFromFile) throws Exception {
        reconfigServer(copyFromFile, null, Constants.NO_JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);
    }

    /**
     * Reconfigures the running server, copies a new server config and then
     * waits for all expected messages (indicating that the server configuration
     * was updated)
     *
     * @param copyFromFile
     *                           - the server config file to copy
     * @param testName
     *                           - the test name that should be included in messages
     * @param reportViaJunit
     *                           - boolean indicating if failures should be reported via junit,
     *                           or if we should just log a message
     * @param messages
     *                           - any additional messages that we should wait for in the logs
     *                           (of type validationMsg which also indicates which log should
     *                           be checked)
     * @throws exception
     */
    public void reconfigServer(String copyFromFile, String testName,
                               Boolean reportViaJunit, List<String> startMessages) throws Exception {

        String thisMethod = "reconfigServer";
        String fullServerPath = null;
        msgUtils.printMethodName(thisMethod);

        Log.info(thisClass, thisMethod,
                 "************** Starting server.xml update for: " + testName);
        try {
            if (copyFromFile == null) {
                Log.info(thisClass, thisMethod, "No new server xml specified for: " + serverName + " - skipping reconfig");;
                return;
            }

            //              if (serverWasRestarted == true || Constants.FORCE_SERVER_RESTART) {
            if (serverWasRestarted == true) {
                restartServer(copyFromFile, testName, lastCheckApps, reportViaJunit, startMessages);
                return;
            }
            // mark the end of the log - all work will occur after this point
            server.setMarkToEndOfLog();

            // update the server config by replacing the server.xml...
            // if the copyFromFile has a slash in it, assume the full path was given, otherwise,
            // assume the same server's config subdirectory...
            if (copyFromFile.contains(File.separator)) {
                fullServerPath = copyFromFile;
            } else {
                fullServerPath = buildFullServerConfigPath(server, copyFromFile);
            }

            Log.info(thisClass, thisMethod, "current server config: " + fullServerPath + " - last server config: " + lastServer);
            if (fullServerPath.equals(lastServer)) {
                Log.info(thisClass, thisMethod, "The SAME SERVER CONFIG file is being used - skipping reconfig");;
                return;
            } else {
                lastServer = fullServerPath;
                if (Constants.FORCE_SERVER_RESTART) {
                    restartServer(copyFromFile, testName, lastCheckApps, reportViaJunit, startMessages);
                    return;
                } else {
                    copyNewServerConfig(fullServerPath, testName);
                }
            }

            waitForServer(testName, reportViaJunit, startMessages);
            // check there is no CWWKS9582E message, if there is the error, force to restart, retry 3 times.
            for (int i = 0; i < 3; i++) {
                if (server.verifyStringNotInLogUsingMark("CWWKS9582E", 10000) == null) {
                    break;
                } else {
                    //retry
                    Log.info(thisClass, thisMethod, "CWWKS9582E message is logged, restarting the server.");
                    restartServer(copyFromFile, testName, lastCheckApps, reportViaJunit, startMessages);
                }
            }
            Log.info(thisClass, thisMethod, "reconfigServer is completed.");
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
    }

    /**
     * Stops current server, cleans up server config xml's, starts a new server
     * after copying a new server config and then waits for all expected
     * messages
     *
     * @param copyFromFile   - the server config file to copy
     * @param testName
     *                           - the test name that should be included in messages
     * @param checkApps
     *                           - thi list of applications that should should be waited on
     *                           during a server start
     * @param reportViaJunit
     *                           - boolean indicating if failures should be reportef via junit,
     *                           or if we should just log a message
     * @param messages
     *                           - any additional messages that we should wait for in the logs
     *                           (of type validationMsg which also indicates which log should
     *                           be checked)
     * @throws exception
     */
    public void restartServer(String copyFromFile, String testName,
                              HashSet<String> checkApps, Boolean reportViaJunit, List<String> startMessages) throws Exception {

        // mark the end of the log - all work will occur after this point
        server.setMarkToEndOfLog();
        server.stopServer(SKIP_LOG_VALIDATION);
        // make sure that all files are backed up to the timestamped server directory under autoFVT
        server.waitForStringInLog("Successfully recovered server");
        // before removing all of the server definitions...
        removeServerConfigFiles();

        startServer(copyFromFile, checkApps, startMessages, testName, reportViaJunit);

        serverWasRestarted = true;
        if (checkApps != null) {
            lastCheckApps.addAll(checkApps);
        }

    }

    public void waitForServer(String testName,
                              Boolean reportViaJunit, List<String> startMessages) throws Exception {

        String thisMethod = "waitForServer";
        try {
            // make sure that the server detected the update
            // not checking the result - go on in either case - msgs will be
            // logged
            // in either case
            if (server.waitForStringInLogUsingMark("CWWKG0016I") != null) {
                Log.info(thisClass, thisMethod, "Server detected the update");
            } else {
                Log.info(
                         thisClass,
                         thisMethod,
                         "Server did NOT detect the update - will continue waiting for additional messages");
            }

            boolean noUpdateMsg = server.verifyStringNotInLogUsingMark(
                                                                       "CWWKG0018I", 10000) == null;
            // if we didn't get the msg that we don't need an update, go on to
            // check for the update completed msg
            if (noUpdateMsg) {
                // waiting for msg that server was actually updated...
                String updateMsg = server.waitForStringInLogUsingMark("CWWKG0017I");
                Log.info(
                         thisClass,
                         thisMethod,
                         "Server update msg: " + updateMsg);
                assertNotNull("Server update message was null", updateMsg);
                validateStartMessages(startMessages, reportViaJunit);
            } else {
                Log.info(thisClass, thisMethod, "noUpdateMsg: " + noUpdateMsg);
                Log.info(thisClass, thisMethod,
                         "Server doesn't need to be updated");
            }

            Log.info(thisClass, thisMethod,
                     "************** Completed server.xml update: " + testName);

            System.err.flush();

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
    }

    /**
     * Copy the specified server config file to server.xml. Make a copy of the
     * server config in the testServers sub-directory for debug use later
     *
     * @param copyFromFile
     *                         - the file to copy
     * @param testName
     *                         - the name of the test that should be included in log messages
     */
    public void copyNewServerConfig(String copyFromFile, String testName) throws Exception {
        server.setServerConfigurationFile(lastServer); // TODO: Copy merged file.

//        String thisMethod = "copyNewServerConfig";
//        String testPrintName;
//
//        File testServerDir = getTestServerDir();
//        String serverFileLoc = getServerFileLoc();
//
//        // update the server config by replacing the server.xml...
//        if (copyFromFile != null && !copyFromFile.isEmpty()) {
//
//            try {
//                if (testName == null) {
//                    testPrintName = "default";
//                } else {
//                    testPrintName = testName;
//                }
//                Log.info(thisClass, thisMethod, "Merging server.xml for : " + copyFromFile);
//                CommonMergeTools merge = new CommonMergeTools();
//                if (merge.mergeFile(copyFromFile, serverFileLoc))
//                {
//                    copyFromFile = copyFromFile.replace(".xml", "_Merged.xml");
//                }
//
//                Log.info(thisClass, thisMethod, "Backing up server.xml for test: " + testPrintName + " to " + testServerDir.toString() + "/" + testPrintName + "_server.xml");
//                LibertyFileManager.copyFileIntoLiberty(server.getMachine(),
//                                                       testServerDir.toString(), testPrintName + "_server.xml", copyFromFile);
//
//                Log.info(thisClass, thisMethod, "Copying: " + copyFromFile + " to "
//                                                + serverFileLoc);
//                LibertyFileManager.copyFileIntoLiberty(server.getMachine(),
//                                                       serverFileLoc, "server.xml", copyFromFile);
//            } catch (Exception ex) {
//                ex.printStackTrace(System.out);
//                throw ex;
//            }
//        }
    }

    public void resetCheckForSecurityReady() {
        checkForSecurityReady = true;
    }

    /**
     * @param testName
     * @return
     */
    private boolean isInvalidSSLConfigTest(String testName) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Searches for message strings in the log specified for each string,
     * reports success/failure of search either via a message, or via junit
     * reporting
     *
     * @param searchMessage
     *                           - validationMsg type list (includes log to search in and the
     *                           string to search for)
     * @param reportViaJunit
     *                           - indicates if success/failure should reported via junit
     *                           reporting, or just a message
     */
    public void validateStartMessages(List<String> startMessages, Boolean reportViaJunit) throws Exception {

        String thisMethod = "validateStartMessages";

        if (startMessages == null) {
            Log.info(thisClass, thisMethod,
                     "No additional messages need to be located for this server start/restart");
        } else {
            for (String searchMsg : startMessages) {
                //String theLog = searchMsg.getWhere();
                //String theMsg = searchMsg.getSearchMsg() ;
                Log.info(thisClass, thisMethod,
                         "Server test message verification: Searching  for message: " + searchMsg);

                String locatedMsg = server.waitForStringInLogUsingMark(searchMsg);
                Log.info(thisClass, thisMethod, "Message content found is: " + locatedMsg);
                if (reportViaJunit == Constants.JUNIT_REPORTING) {
                    msgUtils.assertTrueAndLog(thisMethod,
                                              "Start/reStarting server and did not find expected message: \""
                                                          + searchMsg + "\" after waiting default timeout",
                                              locatedMsg != null);
                }

            }
        }
    }

    public Integer getHttpDefaultPort() {
        if (thisServerType.equals(Constants.CSIv2_DOWNSTREAM)) {
            return Integer.getInteger("HTTP_secondary");
        } else {
            if (thisServerType.equals(Constants.CSIv2_UPSTREAM)) {
                return Integer.getInteger("HTTP_default");
            } else {
                return server.getHttpDefaultPort();
            }
        }
    }

    public Integer getHttpDefaultSecurePort() {
        if (thisServerType.equals(Constants.CSIv2_DOWNSTREAM)) {
            return Integer.getInteger("HTTP_secondary.secure");
        } else {
            if (thisServerType.equals(Constants.CSIv2_UPSTREAM)) {
                return Integer.getInteger("HTTP_default.secure");
            } else {
                return server.getHttpDefaultSecurePort();
            }
        }
    }

    public String getIIOPDefaultPort() {
        String port = "";
        if (thisServerType.equals(Constants.CSIv2_DOWNSTREAM)) {
            port = System.getProperty("IIOP");
        } else if (thisServerType.equals(Constants.CSIv2_UPSTREAM)) {
            port = System.getProperty("IIOP.secondary");
        } else if (thisServerType.equals(Constants.CSIv2_INTERMEDIATE)) {
            port = System.getProperty("IIOP.tertiary");
        }
        return port;
    }

    public String getIIOPDefaultSecurePort() {
        String port = "";
        if (thisServerType.equals(Constants.CSIv2_DOWNSTREAM)) {
            port = System.getProperty("IIOP.secure");
        } else if (thisServerType.equals(Constants.CSIv2_UPSTREAM)) {
            port = System.getProperty("IIOP.secondary.secure");
        } else if (thisServerType.equals(Constants.CSIv2_INTERMEDIATE)) {
            port = System.getProperty("IIOP.tertiary.secure");
        }
        return port;
    }

    public String getHttpString() {
        Log.info(thisClass, "getHttpString", getHttpDefaultPort().toString());
        return "http://" + hostName + ":" + getHttpDefaultPort();
    }

    public String getHttpsString() {
        Log.info(thisClass, "getHttpsString", getHttpDefaultSecurePort().toString());
        return "https://" + hostName + ":" + getHttpDefaultSecurePort();
    }

    public void tearDownServer() throws Exception {
        try {
            if (server != null) {
                stopServer();
                copyNewServerConfig(origServerXml, "tearDown");
            } else {
                Log.info(thisClass, "tearDownServer", "Server instance for \"" + origServerXml + "\" was null");
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    public void stopServer() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(SKIP_LOG_VALIDATION);
        }
    }

    /**
     * Restore the original server config and set the end of log mark
     *
     * @throws Exception
     */
    public void endTestServer() throws Exception {

        try {
            if (server != null) {
                restoreServer();

                // set the mark between tests so we can find config output
                // at the very beginning of the next test if needed...
                setMarkToEndOfLogs();
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    public void restoreServer() throws Exception {

        String thisMethod = "restoreServer";
        msgUtils.printMethodName(thisMethod);

        if (origServerXml != null && !allTestsRestartServer) {
            Log.info(thisClass, thisMethod, "Restoring original file: " + origServerXml);
            reconfigServer(origServerXml);
            serverWasRestarted = false;
        }
    }

    /**
     * Sets a mark at the end of each of the 3 logs - to setup for any kind of
     * search to follow
     */
    public void setMarkToEndOfLogs() {

        try {
            server.setMarkToEndOfLog(); // console.log
            server.setMarkToEndOfLog(server.getMatchingLogFile("messages.log"));
            server.setMarkToEndOfLog(server.getMatchingLogFile("trace.log"));
        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, "setMarkToEndOfLogs", e,
                      "Failure setting the Mark at the end of one or more of the serverlogs");
        }
    }

    /**
     * Remove app list from validation during server startup
     *
     * @param appList
     */
    public void removeAppsFromValidation() {
        server.removeAllInstalledAppsForValidation();
    }

}
