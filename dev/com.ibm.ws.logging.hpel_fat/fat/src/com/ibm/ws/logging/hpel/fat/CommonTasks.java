/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
//%Z% %I% %W% %G% %U% [%H% %T%]
/*
 * Change History:
 *
 * Reason  		  Version 	 Date       User id     Description
 * ----------------------------------------------------------------------------
 * F017049-28712	8.0		06/29/2010	shighbar	Updates for SSFAT test cases and new useful methods in prep for z/OS support.
 * F17049-32205		8.0		10/04/2010	shighbar	Generalize common methods to support additional ServerTypes.
 * 681388			8.0		12/07/2010  shighbar	Add method to start server with non-standard mBean timeout.
 * 689639.fvt       8.0     02/09/2011  spaungam    sensitive filtering needs to be enabled earlier
 * 695538			8.0		03/08/2011	shighbar	testMergedRepositories failures on z/OS.
 * 707366			8.0		06/01/2011	shighbar	Hpel Internal Trace failures / test case expansion
 * PM41930			8.0		08/23/2011	spaungam	FFDC loggers to be asynchronous
 * 721541           8.0     10/26/2011  spaungam    Ensure exceptions appear with logging
 */
package com.ibm.ws.logging.hpel.fat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xml.sax.SAXException;

import com.ibm.websphere.simplicity.Cell;
import com.ibm.websphere.simplicity.Node;
import com.ibm.websphere.simplicity.PortType;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.Server;
import com.ibm.websphere.simplicity.ServerType;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.runtime.ProcessStatus;
import com.ibm.ws.fat.util.StopWatch;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

import componenttest.topology.impl.LibertyServer;

/**
 * Holding class for driving common logic across multiple test cases/drivers
 *
 */
public class CommonTasks {

    public static final String HPEL_APP_CONTEXT_ROOT = "HpelFat";
    public static final String HPEL_APP_GENLOG_JSP = "ivtLogSleep.jsp";
    public static final String HPEL_APP_CREATE_LOG_JSP = "LogCreator.jsp";
    public static final String RAS_ZIPPER_UTIL_JSP = "RasZipper.jsp";
    public static final String LOGS = "Logs";
    public static final String TRACE = "Trace";
    public static final String LOGS_TRACE = "LogsAndTrace";

    public static enum zOSProcessType {
        Control, Adjunct, Servant
    }

    private static final int SERVER_START_MBEAN_TIMEOUT = 240;

//    private static final String JYTHON_RAS_RAWTRACE_SWITCH = "RasConfigEnableRawTrace.jy";

    /**
     * Makes use of the ivtLogSleep.jsp in the sample application to drive creation of log records.
     *
     * @param numCycles
     *                       How many iterations to run (each iteration generates multiple log records, one of each level). Must be
     *                       a positive integer, or else only onecycle will be ran
     *
     * @param cycleDelay
     *                       The delay you want between each cycle in seconds. Useful for having steady stream of log entries
     *                       created. If not a positive number, zero will be used.
     * @throws Exception
     * @throws SAXException
     * @throws IOException
     * @throws MalformedURLException
     */
//    @Deprecated
//    public static void genLogEntries(int numCycles, int cycleDelay) throws MalformedURLException, IOException, SAXException, Exception {
//        if (numCycles <= 0) {
//            numCycles = 1;
//        }
//        if (cycleDelay <= 0) {
//            cycleDelay = 0;
//        }
//
//        String jsp = "/" + HPEL_APP_CONTEXT_ROOT + "/" + HPEL_APP_GENLOG_JSP;
//        String jspParams = "?ActionParm=trace&NumberOfIterations=" + Integer.toString(numCycles) + "&CycleDelay="
//                           + Integer.toString(cycleDelay);
//
//        WebConversation wc = new WebConversation();
//        writeLogMsg(Level.INFO, "    Getting URL: " + CommonActions.getUrl(HpelSetup.getServerUnderTest(), jsp + jspParams, false));
//        WebResponse response = wc.getResponse(CommonActions.getUrl(HpelSetup.getServerUnderTest(), jsp + jspParams, false));
//
//        int rc = response.getResponseCode();
//        writeLogMsg(Level.INFO, "      Response Code for jsp is: " + Integer.toString(rc));
//
//    }

    /**
     * @Deprecated A shortcut to createLogEntries(LibertyServer appServer, String loggerName, String message, Level
     *             level, int iterations, String repository, int sleepTime) with no sleep time and assuming executing
     *             against the default application server for the test bucket.
     */
//    @Deprecated
//    public static WebResponse createLogEntries(String loggerName, String message, Level level, int iterations, String repository)
//                    throws MalformedURLException, IOException, SAXException, Exception {
//        return createLogEntries(loggerName, message, level, iterations, repository, -1);
//    }

    /**
     * @Deprecated A shortcut to createLogEntries(LibertyServer appServer, String loggerName, String message, Level
     *             level, int iterations, String repository, int sleepTime) without the requirement to pass in
     *             application server.
     */
//    @Deprecated
//    public static WebResponse createLogEntries(String loggerName, String message, Level level, int iterations, String repository,
//                                               int sleepTime) throws MalformedURLException, IOException, SAXException, Exception {
//        return createLogEntries(HpelSetup.getServerUnderTest(), loggerName, message, level, iterations, repository, sleepTime);
//    }

    /**
     * Makes use of the LogCreator.jsp in the RAS sample application to drive creation of log records.
     *
     * @param appServer
     *                       The application server on which the log entries should be created.
     * @param loggerName
     *                       The name of the logger the JSP is to use. Default loggerName: Default_HPELTestLogger
     * @param message
     *                       The message the logger is to log. Default message: Default HPEL Message
     * @param level
     *                       The level of message to be logged. Choices are ALL, SEVERE, WARNING, INFO, FINE, FINER, FINEST Default
     *                       value: ALL
     * @param iterations
     *                       How many iterations to run (each iteration generates multiple log records, one of each level). Must be
     *                       a positive integer. Default value: 49
     * @param repository
     *                       The repository to create the messages into. There are 3 choices, CommonTasks.LOGS, CommonTasks.TRACE
     *                       or CommonTasks.LOGS_TRACE Default value: CommonTasks.LOGS
     * @param sleepTime
     *                       The time between iterations in milliseconds.
     * @param async
     *                       Value of the logger's (if it is a WAS WsLogger) async value to be set to.
     *
     * @return WebResponse
     *
     * @throws Exception
     * @throws SAXException
     * @throws IOException
     * @throws MalformedURLException
     */
    public static WebResponse createLogEntries(LibertyServer appServer, String loggerName, String message, Level level,
                                               int iterations, String repository, int sleepTime, boolean async) throws MalformedURLException, IOException, SAXException, Exception {
        String jspParams = "?ActionParam=";
        String jsp = "/" + HPEL_APP_CONTEXT_ROOT + "/" + HPEL_APP_CREATE_LOG_JSP;
        String actionParam = CommonTasks.LOGS;

        // make the generation of both messages and logs the default
        if (repository != null) {
            actionParam = repository;
        }
        jspParams += actionParam + "&";

        // check to see if anything was passed, if nothing was, all of the JSP's defaults can be used
        if ((loggerName != null) || (message != null) || (level != null) || (iterations > 0)) {
            String s1, s2, s3, s4, s5 = null, s6;

            if (loggerName != null) {
                s1 = "LoggerName=" + loggerName;
            } else {
                s1 = "LoggerName=Default_HPELTestLogger";
            }
            if (message != null) {
                s2 = "Message=" + message;
            } else {
                s2 = "Message='Default HPEL Message'";
            }
            if (level != null) {
                s3 = "Level=" + level.getName();
            } else {
                s3 = "Level=ALL";
            }
            if (iterations > 0) {
                s4 = "Iterations=" + Integer.toString(iterations);
            } else {
                s4 = "Iterations=49";
            }

            if (sleepTime > -1)
                s5 = "Sleep=" + sleepTime;

            s6 = async ? "Async=true" : "Async=false";

            jspParams += s1 + "&" + s2 + "&" + s3 + "&" + s4 + "&" + s5 + "&" + s6;
        }

        WebConversation wc = new WebConversation();
        WebResponse response = wc.getResponse(getUrl(appServer, jsp + jspParams, false));

        return response;
    }

    /**
     * Builds the URL for a servlet based on the WC_defaulthost or
     * WC_defaulthost_secure port
     *
     * @param server
     *                   the server where your application is running
     * @param uri
     *                   the URI of the resource you want to access; for example:
     *                   /module/servlet
     * @param https
     *                   true if you want to use https; false if you want to use http
     * @return a URL for the specified servlet
     * @throws NullPointerException
     *                                  if the specified server is null
     * @throws Exception
     *                                  if a Simplicity error occurs
     */
    public static String getUrl(LibertyServer server, String uri, boolean https) throws Exception {
        StringBuffer url = new StringBuffer();
        if (https) {
            url.append("https://");
        } else {
            url.append("http://");
        }
        url.append(server.getHostname());
        url.append(":");
        if (https) {
            url.append(server.getPort(PortType.WC_defaulthost_secure));
        } else {
            url.append(server.getPort(PortType.WC_defaulthost));
        }
        if (uri != null) {
            url.append(uri);
        }
        return url.toString();
    }

    /**
     * Makes use of the LogCreator.jsp in the RAS sample application to drive creation of log records.
     *
     * @param appServer
     *                       The application server on which the log entries should be created.
     * @param loggerName
     *                       The name of the logger the JSP is to use. Default loggerName: Default_HPELTestLogger
     * @param message
     *                       The message the logger is to log. Default message: Default HPEL Message
     * @param level
     *                       The level of message to be logged. Choices are ALL, SEVERE, WARNING, INFO, FINE, FINER, FINEST Default
     *                       value: ALL
     * @param iterations
     *                       How many iterations to run (each iteration generates multiple log records, one of each level). Must be
     *                       a positive integer. Default value: 49
     * @param repository
     *                       The repository to create the messages into. There are 3 choices, CommonTasks.LOGS, CommonTasks.TRACE
     *                       or CommonTasks.LOGS_TRACE Default value: CommonTasks.LOGS
     * @param sleepTime
     *                       The time between iterations in milliseconds.
     *
     * @return WebResponse
     *
     * @throws Exception
     * @throws SAXException
     * @throws IOException
     * @throws MalformedURLException
     */
    public static WebResponse createLogEntries(LibertyServer appServer, String loggerName, String message, Level level,
                                               int iterations, String repository, int sleepTime) throws MalformedURLException, IOException, SAXException, Exception {

        return createLogEntries(appServer, loggerName, message, level, iterations, repository, sleepTime, false);
    }

    /**
     * Returns a ConfigObject representing the RAS Logging Service configuration in the model.
     *
     * @param aServer
     *                    aServer The server you want the RAS Logging Service ConfigObject for
     *
     * @return ulsConfigObj
     */
//    public static ConfigObject getLegacyLoggingService(Server aServer) throws Exception {
//        ConfigObject ulsConfigObj = ConfigObject.getConfigObject(aServer, aServer.getConfigId(), "RASLoggingService");
//        return ulsConfigObj;
//    }

    /**
     * Returns a ConfigObject representing the HPEL configuration in the model.
     *
     * @param aServer
     *                    aServer The server you want the HPEL ConfigObject for
     *
     * @return ulsConfigObj
     */
//    public static ConfigObject getUnifiedLoggingService(Server aServer) throws Exception {
//        ConfigObject ulsConfigObj = ConfigObject.getConfigObject(aServer, aServer.getConfigId(), "HighPerformanceExtensibleLogging");
//        return ulsConfigObj;
//    }

    /**
     * Returns a ConfigObject representing the child HPEL configuration in the model.
     *
     * @param aServer
     *                      The server you want the child HPEL ConfigObject for.
     * @param childName
     *                      The name of the child configuration object you want to retrieve.
     *
     * @return ulsConfigObj
     */
//    private static ConfigObject getUnifiedLoggingServiceChild(Server aServer, String childName) throws Exception {
//        // this fails due to bug in Simplicity.
//        // return ConfigObject.getConfigObject(HpelSetup.getServerUnderTest(),
//        // getUnifiedLoggingService().getConfigIdentifier(), childName);
//
//        List<ConfigObject> binChild1 = getUnifiedLoggingService(aServer).getChildObjectListByName(childName);
//        if (binChild1.iterator().hasNext())
//            return binChild1.iterator().next(); // We should only get one child back.
//        else
//            return null;
//    }

    /**
     * Returns a ConfigObject representing the HPEL Binary Log configuration in the model.
     *
     * @param aServer
     *                    The server you want the HPEL Binary Log ConfigObject for.
     *
     * @return The ConfigObject for the HPEL Binary Log.
     */
//    public static ConfigObject getBinaryLogChild(Server aServer) throws Exception {
//        return getUnifiedLoggingServiceChild(aServer, "HPELLog");
//    }

    /**
     * Returns a ConfigObject representing the HPEL Binary Trace configuration in the model.
     *
     * @param aServer
     *                    The server you want the HPEL Binary Trace ConfigObject for.
     *
     * @return The ConfigObject for the HPEL Binary Trace.
     */
//    public static ConfigObject getBinaryTraceChild(Server aServer) throws Exception {
//        return getUnifiedLoggingServiceChild(aServer, "HPELTrace");
//    }

    /**
     * Returns a ConfigObject representing the HPEL Text Log configuration in the model.
     *
     * @param aServer
     *                    The server you want the HPEL Text Log ConfigObject for.
     *
     * @return The ConfigObject for the HPEL Text Log.
     */
//    public static ConfigObject getTextLogChild(Server aServer) throws Exception {
//        return getUnifiedLoggingServiceChild(aServer, "HPELTextLog");
//    }

//    @Deprecated
//    public static boolean isHpelEnabled() throws Exception {
//        return isHpelEnabled(HpelSetup.getServerUnderTest());
//    }

    public static boolean isHpelEnabled(LibertyServer aServer) throws Exception {
        LibertyServer server = aServer;
        RemoteFile bootstrapFile = server.getServerBootstrapPropertiesFile();
        if (bootstrapFile == null || !bootstrapFile.exists()) {
            return false;
        }
        Properties bootstrapProps = new Properties();
        InputStream is = bootstrapFile.openForReading();
        bootstrapProps.load(is);
        is.close();
        String logProvider = bootstrapProps.getProperty("websphere.log.provider");
        return logProvider != null && logProvider.startsWith("binaryLogging-");
//        if (getUnifiedLoggingService(aServer) == null) {
//            // For backward compatibility - we assume if HPEL config object is null then server.xml is not updated and
//            // therefore HPEL must not be enabled since the ConfigObject is returning null.
//            writeLogMsg(Level.SEVERE, "isHpelEnabled method got null back from UnifiedLoggingService");
//            if (aServer == null) {
//                writeLogMsg(Level.SEVERE, "The application server passed to isHpelEnabled is null");
//            }
//            return false;
//        }
//        boolean tempresult = (getUnifiedLoggingService(aServer).getAttributeByName("enable").getValueAsBoolean() && !getLegacyLoggingService(
//                                                                                                                                             aServer).getAttributeByName("enable").getValueAsBoolean());
//        return tempresult;
    }

    public static boolean isHpelSensitiveTraceEnabled(LibertyServer aServer) throws Exception {
        //if in hpel mode, get HighPerformanceExtensibleLogging
//        ConfigObject configObj = getUnifiedLoggingService(aServer).getAttributeByName("rawTraceFilterEnabled");
//
//        if (configObj != null) {
//            return configObj.getValueAsBoolean();
//        }
//
//        writeLogMsg(Level.INFO, "isHpelSensitiveTraceEnabled configObj is null");
        return false;
    }

    public static boolean setHpelSensitiveTraceEnabled(LibertyServer aServer, boolean enable) throws Exception {
        if (enable == isHpelSensitiveTraceEnabled(aServer)) {
            // the server is already set to the value we are trying to set. So do nothing.
            return false;
        }

        throw new UnsupportedOperationException("SensitiveTrace is not supported yet");
//        Workspace tempWorkSpace = HpelSetup.getCellUnderTest().getWorkspace();
//        getUnifiedLoggingService(aServer).getAttributeByName("rawTraceFilterEnabled").setValue(enable);
//        tempWorkSpace.saveAndSync();
//        return true;
    }

    public static boolean isRasSensitiveTraceEnabled() throws Exception {
        throw new UnsupportedOperationException("SensitiveTrace is not supported yet");
//              String[] scriptArgs = null;
//              scriptArgs = new String[3];
//              scriptArgs[0] = HpelSetup.getCellUnderTest().getName();
//              scriptArgs[1] = HpelSetup.getNodeUnderTest().getName();
//              scriptArgs[2] = HpelSetup.getServerUnderTest().getName();

//		String result = RASWsadminScripts.exeJythonScript(JYTHON_RAS_RAWTRACE_SWITCH, scriptArgs);
//		writeLogMsg(Level.INFO, "isRasSensitiveTraceEnabled result of wsadmin: " +  result);
//
//		return "true".equalsIgnoreCase(result);
    }

    public static boolean setRasSensitiveTraceEnabled(boolean enable) throws Exception {
        if (enable == isRasSensitiveTraceEnabled()) {
            // the server is already set to the value we are trying to set. So do nothing.
            return false;
        }

        throw new UnsupportedOperationException("SensitiveTrace is not supported yet");
//        String[] scriptArgs = null;
//        scriptArgs = new String[5];
//        scriptArgs[0] = HpelSetup.getCellUnderTest().getName();
//        scriptArgs[1] = HpelSetup.getNodeUnderTest().getName();
//        scriptArgs[2] = HpelSetup.getServerUnderTest().getName();
//        scriptArgs[3] = "set";
//        scriptArgs[4] = Boolean.toString(enable);
//
//        String result = RASWsadminScripts.exeJythonScript(JYTHON_RAS_RAWTRACE_SWITCH, scriptArgs);
//        writeLogMsg(Level.INFO, "setRasSensitiveTraceEnabled result of wsadmin: " + result);
//
//        return true;

    }

    /**
     * Sets if HPEL is enabled or not in the configuration.
     *
     * @param aServer
     *                    The server you want to enable or disable HPEL on.
     *
     * @return True if changes were needed / made. False no changes were required, as in the configuration already matched what was requested.
     */
    public static boolean setHpelEnabled(LibertyServer aServer, boolean enable) throws Exception {
        if (enable == isHpelEnabled(aServer)) {
            // the server is already set to the value we are trying to set. So do nothing.
            return false;
        }
        LibertyServer server = aServer;
        RemoteFile bootstrapFile = server.getServerBootstrapPropertiesFile();
        if (bootstrapFile == null) {
            return false;
        }
        Properties bootstrapProps = new Properties();
        bootstrapProps.setProperty("websphere.log.provider", "binaryLogging-1.0");
        bootstrapProps.store(bootstrapFile.openForWriting(true), null);
        return true;
//        Workspace tempWorkSpace = HpelSetup.getCellUnderTest().getWorkspace();
//        getUnifiedLoggingService(aServer).getAttributeByName("enable").setValue(enable);
//        getLegacyLoggingService(aServer).getAttributeByName("enable").setValue(!enable);
//        tempWorkSpace.saveAndSync();
//        return true;

        // TODO Enabling or disabling HPEL requires us to bounce the server - don't know if we should do that here or
        // not though? for now that is the caller's responsibility as they may not want it restarted and or have other
        // changes they are making before the restart.
    }

    /**
     * Sets if HPEL is enabled or not in the configuration, and bounces the application server if needed.
     *
     * @param aServer
     *                    The server you want to enable or disable HPEL on. The server will only be bounced if the configuration does not already match what was requested.
     *
     * @return True if changes were needed/made, and the server was bounced. False - no changes were required, as in the configuration already matched what was requested.
     */
    public static boolean setHpelEnabledAndBounce(LibertyServer aServer, boolean enable) throws Exception {
        if (setHpelEnabled(aServer, enable)) {
            aServer.stopServer();
            aServer.startServer();
            return true;
        }
        return false;
    }

    public static boolean isTextLogEnabled(LibertyServer aServer) throws Exception {
        LibertyServer server = aServer;
        BufferedReader config = new BufferedReader(new InputStreamReader(server.getServerConfigurationFile().openForReading()));
        String line;
        boolean result = false;
        boolean inLogging = false;
        while ((line = config.readLine()) != null) {
            int index = 0;
            if (!inLogging) {
                int index1 = line.indexOf("<logging");
                if (index1 >= 0) {
                    inLogging = true;
                    index = index1;
                }
            }
            if (inLogging) {
                if (!result) {
                    int index2 = line.indexOf("<textLog", index);
                    if (index2 >= 0) {
                        result = true;
                        index = index2;
                    }
                }
                int index2 = line.indexOf("</logging>", index);
                if (index2 >= 0) {
                    inLogging = false;
                }
            }
        }
        return result;
//        return (getTextLogChild(aServer).getAttributeByName("enabled").getValueAsBoolean());
    }

    public static String getHpelTraceSpec(LibertyServer aServer) throws Exception {
        LibertyServer server = aServer;
        String traceSpec = server.getServerConfiguration().getLogging().getTraceSpecification();
        return traceSpec == null ? "" : traceSpec;
//        return (getUnifiedLoggingService(aServer).getAttributeByName("startupTraceSpec").getValueAsString());
    }

    /**
     * Sets the HPEL trace specification in the application server's configurations. Changes to the trace specification
     * with this method will not take effect until after the application server is bounced.
     *
     * @param aServer
     *                         The server you want the new trace specification applied to.
     * @param newTraceSpec
     *                         The new trace specification, such as 'com.ibm.myClass*=all' etc. Passing a null String is equivalent
     *                         to specifying a trace string of '*=info'
     *
     * @throws Exception
     */
    public static void setHpelTraceSpec(LibertyServer aServer, String newTraceSpec) throws Exception {
        if (null == newTraceSpec) {
            // null spec is equivalent to default no trace spec.
            newTraceSpec = "*=info";
        }

        // check if we need to update the spec or if it's already set to what we want.
        if (getHpelTraceSpec(aServer).equals(newTraceSpec)) {
            writeLogMsg(Level.FINE, "HPEL Trace Spec already set to " + newTraceSpec + ". No change needed.");
            return;
        } else {
            LibertyServer server = aServer;
            ServerConfiguration config = server.getServerConfiguration();
            config.getLogging().setTraceSpecification(newTraceSpec);
            server.updateServerConfiguration(config);
//            writeLogMsg(Level.FINE, "Setting the HPEL Trace Spec to " + newTraceSpec);
//            Workspace tmpWS = HpelSetup.getCellUnderTest().getWorkspace();
//            getUnifiedLoggingService(aServer).getAttributeByName("startupTraceSpec").setValue(newTraceSpec);
//            tmpWS.saveAndSync();
        }

    }

    public static RemoteFile getBinaryLogDir(LibertyServer aServer) throws Exception {
        String logDir = aServer.getServerConfiguration().getLogging().getLogDirectory();
        return logDir == null ? aServer.getFileFromLibertyServerRoot("logs") : new RemoteFile(aServer.getMachine(), logDir);
//        return new RemoteFile(HpelSetup.getNodeUnderTest().getMachine(), aServer.expandString(getBinaryLogChild(aServer)
//                        .getAttributeByName("dataDirectory").getValueAsString()));
    }

    public static RemoteFile getBinaryTraceDir(LibertyServer aServer) throws Exception {
        String logDir = aServer.getServerConfiguration().getLogging().getLogDirectory();
        return logDir == null ? aServer.getFileFromLibertyServerRoot("logs") : new RemoteFile(aServer.getMachine(), logDir);
//        return new RemoteFile(aServer.getNode().getMachine(), aServer.expandString(getBinaryTraceChild(aServer)
//                        .getAttributeByName("dataDirectory").getValueAsString()));
    }

    public static RemoteFile getTextLogDir(LibertyServer aServer) throws Exception {
        String logDir = aServer.getServerConfiguration().getLogging().getLogDirectory();
        return logDir == null ? aServer.getFileFromLibertyServerRoot("logs") : new RemoteFile(aServer.getMachine(), logDir);
//        return new RemoteFile(aServer.getNode().getMachine(), aServer.expandString(getTextLogChild(aServer).getAttributeByName(
//                                                                                                                               "dataDirectory").getValueAsString()));
    }

    /**
     * A utility method to get log repository from a remote application server. The entire log repository will be copied
     * to the location specified.
     *
     * @param aServer
     *                           The Application Server from which to retrieve the log repository from.
     *
     * @param destinationDir
     *                           A RemoteFile handle to the directory where you want the log repository copied to.
     *
     * @return true if the copy was successful.
     */
    public static boolean getLogRepositoryFromServer(LibertyServer aServer, RemoteFile destinationDir) throws Exception {
        RemoteFile remoteLogsDir = CommonTasks.getBinaryLogDir(aServer);
        RemoteFile remoteLogRepository = new RemoteFile(remoteLogsDir.getMachine(), remoteLogsDir, "logdata");

        return destinationDir.copyFromSource(remoteLogRepository, true, true);
    }

    /**
     * A utility method to get a compressed log repository from a remote application server. The entire log repository
     * will be copied to the location specified.
     *
     * @param aServer
     *                               The Application Server from which to retrieve the log repository from.
     *
     * @param destinationZipFile
     *                               A RemoteFile handle to the zip file where you want the zipped log repository copied to to. Note if the
     *                               file already exists it will be overwritten.
     *
     * @return true if the copy was successful.
     */
//    public static boolean getZippedLogRepositoryFromServer(LibertyServer aServer, RemoteFile destinationZipFile) throws Exception {
//        return zipAndCopyARepositoryFromServer(aServer, destinationZipFile, CommonTasks.LOGS);
//    }

    /**
     * A utility method to get trace repository from a remote application server. The entire trace repository will be
     * copied to the location specified.
     *
     * @param aServer
     *                           The Application Server from which to retrieve the trace repository from.
     *
     * @param destinationDir
     *                           A RemoteFile handle to the directory where you want the trace repository copied to.
     *
     * @return true if the copy was successful.
     */
//    public static boolean getTraceRepositoryFromServer(LibertyServer aServer, RemoteFile destinationDir) throws Exception {
//        RemoteFile remoteTraceDir = CommonTasks.getBinaryTraceDir(aServer);
//        RemoteFile remoteTraceRepository = new RemoteFile(remoteTraceDir.getMachine(), remoteTraceDir, "tracedata");
//
//        return destinationDir.copyFromSource(remoteTraceRepository, true, true);
//    }

    /**
     * A utility method to get a compressed trace repository from a remote application server. The entire trace repository
     * will be copied to the location specified.
     *
     * @param aServer
     *                               The Application Server from which to retrieve the log repository from.
     *
     * @param destinationZipFile
     *                               A RemoteFile handle to the zip file where you want the zipped log repository copied to to. Note if the
     *                               file already exists it will be overwritten.
     *
     * @return true if the copy was successful.
     */
//    public static boolean getZippedTraceRepositoryFromServer(LibertyServer aServer, RemoteFile destinationZipFile) throws Exception {
//        return zipAndCopyARepositoryFromServer(aServer, destinationZipFile, CommonTasks.TRACE);
//    }

//    private static boolean zipAndCopyARepositoryFromServer(LibertyServer aServer, RemoteFile destinationZipFile, String repositoryType) throws Exception {
//        RemoteFile remoteRepositoryDir = null;
//        String repositorySubDirName = "logdata"; // default is logs
//        if (repositoryType.equals(CommonTasks.TRACE)) {
//            remoteRepositoryDir = CommonTasks.getBinaryTraceDir(aServer);
//            repositorySubDirName = "tracedata";
//        } else
//            remoteRepositoryDir = CommonTasks.getBinaryLogDir(aServer);
//
//        Machine rMachine = remoteRepositoryDir.getMachine();
//        RemoteFile remoteRepository = new RemoteFile(rMachine, remoteRepositoryDir, repositorySubDirName);
//
//        String tmpZipFileName = ".tmp_" + Thread.currentThread().getId() + "_" + System.currentTimeMillis() + ".zip";
//        RemoteFile rProfileLogsDir = new RemoteFile(rMachine, aServer.getNode().getProfileDir() + rMachine.getOperatingSystem().getFileSeparator() + "logs");
//        RemoteFile tmpZipFile = new RemoteFile(rMachine, rProfileLogsDir, tmpZipFileName);
//
//        // 1. Have the files zipped up on the server.
//        // Build the URL to this test case
//        String URLsuffix = "/" + HPEL_APP_CONTEXT_ROOT + "/" + RAS_ZIPPER_UTIL_JSP;
//        String URL = CommonActions.getUrl(aServer, URLsuffix, false);
//
//        writeLogMsg(Level.FINE, "Calling RazZipper to compress remote files");
//        writeLogMsg(Level.FINE, "URL: " + URL);
//        writeLogMsg(Level.FINE, "inputFile: " + remoteRepository.getAbsolutePath());
//        writeLogMsg(Level.FINE, "outputZipArchive: " + tmpZipFile.getAbsolutePath());
//
//        WebRequest request = new PostMethodWebRequest(URL);
//        request.setParameter("inputFile", remoteRepository.getAbsolutePath());
//        request.setParameter("outputZipArchive", tmpZipFile.getAbsolutePath());
//        // request.setParameter("archiveComment", "" );
//
//        WebConversation wc = new WebConversation();
//        WebResponse response = null;
//        response = wc.getResponse(request);
//        if (response == null)
//            writeLogMsg(Level.WARNING, "The response from RasZipper is null");
//        if (response.getResponseCode() != 200)
//            writeLogMsg(Level.WARNING, "Unxpected response code from RasZipper. Recieved response code of: "
//                                       + response.getResponseCode());
//
//        // 2. Copy the zip file to the destination
//        boolean returneCode = destinationZipFile.copyFromSource(tmpZipFile, true, true);
//        if (!tmpZipFile.delete()) // clean up our tmp file.
//            writeLogMsg(Level.WARNING, "Could not delete tmp zip file " + tmpZipFile.getAbsolutePath());
//        return returneCode;
//    }

    /**
     * A utility method to get log and trace repository from a remote application server. The entire repository will be
     * copied to the location specified.
     *
     * @param aServer
     *                           The Application Server from which to retrieve the repository from.
     *
     * @param destinationDir
     *                           A RemoteFile handle to the directory where you want the repository copied to.
     *
     * @return true if the copy was successful.
     */
    public static boolean getRepositoryFromServer(LibertyServer aServer, RemoteFile destinationDir) throws Exception {
        RemoteFile rRepLogDataDir = new RemoteFile(aServer.getMachine(), CommonTasks.getBinaryLogDir(aServer), "logdata");
        RemoteFile rRepTraceDataDir = new RemoteFile(aServer.getMachine(), CommonTasks.getBinaryTraceDir(aServer), "tracedata");
        boolean logCopyResult = false;
        boolean traceCopyResult = false;
        if (rRepLogDataDir.exists()) {
            // copy the logdata dir
            RemoteFile destLogDataDir = new RemoteFile(destinationDir.getMachine(), destinationDir, "logdata");
            logCopyResult = destLogDataDir.copyFromSource(rRepLogDataDir, true, true);
        }
        if (rRepTraceDataDir.exists()) {
            // copy the logdata dir
            RemoteFile destTraceDataDir = new RemoteFile(destinationDir.getMachine(), destinationDir, "tracedata");
            traceCopyResult = destTraceDataDir.copyFromSource(rRepTraceDataDir, true, true);
        }

        return (logCopyResult && traceCopyResult);
    }

    /**
     * A simple method used to log messages to the output.txt log
     *
     * @param logLevel
     *                     The level you want the message written as. If null will default to INFO
     *
     * @param msg
     *                     The message to log.
     */
    public static void writeLogMsg(Level logLevel, String msg) {
        // default to INFO if level is null
        if (logLevel == null)
            logLevel = Level.INFO;
        // determine who called me
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        StackTraceElement element = stackTraceElements[3]; // get the latest entry after the calls to get stracktrace
        Logger bucketLogger = Logger.getLogger(element.getClassName());
        bucketLogger.logp(logLevel, element.getClassName(), element.getMethodName(), msg);
    }

    /**
     * A simple method used to log messages to the output.txt log
     *
     * @param logLevel
     *                     The level you want the message written as. If null will default to INFO
     *
     * @param msg
     *                     The message to log.
     * @param thrown
     *                     Throwable to be logged
     */
    public static void writeLogMsg(Level logLevel, String msg, Throwable thrown) {
        // default to INFO if level is null
        if (logLevel == null)
            logLevel = Level.INFO;
        // determine who called me
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        StackTraceElement element = stackTraceElements[3]; // get the latest entry after the calls to get stracktrace
        Logger bucketLogger = Logger.getLogger(element.getClassName());
        bucketLogger.logp(logLevel, element.getClassName(), element.getMethodName(), msg, thrown);
    }

    /**
     * Stops and removes all Application Servers, Web Servers, and Proxy Servers in the specified cell, except for those
     * specified in the excludes set
     *
     * @param cell
     *                            The cell where you want to remove servers.
     * @param excludedServers
     *                            The Set of servers you want to keep.
     * @throws Exception
     *                       Thrown if any server can't be removed
     */
    public static void removeUnusedServers(Cell cell, Set<Server> excludedServers) throws Exception {
        if (cell == null) {
            return;
        }
        try {
            Set<Server> allServers = cell.getServers();
            for (Server server : allServers) {
                ServerType type = server.getServerType();
                if (ServerType.PROXY_SERVER.equals(type) || ServerType.WEB_SERVER.equals(type)
                    || ServerType.APPLICATION_SERVER.equals(type)) {
                    if (!excludedServers.contains(server)) {
                        writeLogMsg(Level.INFO, "Removing the server " + server.getName());
                        remove(server);
                    }
                }
            }
        } catch (Exception e) {
            throw new Exception("Unable to remove unused Servers in the Cell named " + cell.getName(), e);
        }
    }

    /**
     * Removes a Server from the WebSphere configuration (and Simplicity object model) using Simplicity. If the server
     * is currently running, it will be stopped before it's removed.
     *
     * @param server
     *                   The Server you wish to remove.
     * @throws Exception
     *                       If Simplicity fails to remove the server.
     */
    public static void remove(Server server) throws Exception {
        if (server == null) {
            return; // nothing to do
        }
        Level logAtlevel = Level.INFO;
        writeLogMsg(logAtlevel, "Removing a Server...");
        ProcessStatus status = server.getServerStatus();
        String serverName = server.getName();

        writeLogMsg(logAtlevel, "  Type: " + server.getServerType().toString());
        writeLogMsg(logAtlevel, "  Name: " + serverName);
        Node node = server.getNode();
        if (node == null) {
            writeLogMsg(logAtlevel, "  Node: null");
        } else {
            writeLogMsg(logAtlevel, "  Node: " + node.getName());
            Cell cell = node.getCell();
            if (cell == null) {
                writeLogMsg(logAtlevel, "  Cell: null");
            } else {
                writeLogMsg(logAtlevel, "  Cell: " + cell.getName());
            }
        }
        writeLogMsg(logAtlevel, "  Status: " + server.getName());

        StopWatch timer = new StopWatch();
        timer.start();
        if (ProcessStatus.RUNNING.equals(status)) {
            writeLogMsg(logAtlevel, "Since the server is currently running, it will be stopped and then removed.");
            server.stop();
        }
        server.getNode().deleteServer(serverName);
        timer.stop();

        writeLogMsg(logAtlevel, "The Server was removed after: " + timer.getTimeElapsedAsString());

    }

    /**
     * Starts a server and explicitly sets the timeout value for the mBean start operation. On slower systems,
     * especially z/OS where the server startup takes longer use of this start method may be needed. Using this method
     * uses a higher timeout value when starting the application server.
     *
     * @param serverToStart
     *                          The Server you wish to start.
     * @throws Exception
     *                       If Simplicity fails to start the server.
     */
    public static void startServer(Server serverToStart) throws Exception {
        serverToStart.start(SERVER_START_MBEAN_TIMEOUT);
    }

    /**
     * To add new properties to bootstrap file
     *
     * @param appServer
     * @param propertyName
     * @param propertyValue
     * @throws Exception
     */
    public static void addBootstrapProperty(LibertyServer appServer, String propertyName, String propertyValue) throws Exception {

        LibertyServer server = appServer;
        RemoteFile bootstrapFile = server.getServerBootstrapPropertiesFile();
        Properties bootstrapProps = new Properties();
        bootstrapProps.setProperty(propertyName, propertyValue);
        OutputStream os = bootstrapFile.openForWriting(true);
        bootstrapProps.store(os, null);
        os.close();

    }

    /**
     * To remove new properties to bootstrap file
     *
     * @param appServer
     * @param propertyName
     * @param propertyValue
     * @throws Exception
     */
    public static void removeBootstrapProperty(LibertyServer appServer, String propertyName) throws Exception {

        LibertyServer server = appServer;
        RemoteFile bootstrapFile = server.getServerBootstrapPropertiesFile();
        Properties bootstrapProps = new Properties();
        bootstrapProps.load(bootstrapFile.openForReading());
        bootstrapProps.remove(propertyName);
        bootstrapProps.store(bootstrapFile.openForWriting(false), null);

    }

    /**
     * Returns a ConfigObject representing the JVMEntries for an Application Server. The JVMEntries is a child object of
     * processDefinition. It's useful for getting at the systemProperties / customProperties configured for an
     * application server.
     *
     * @param appServ
     *                     The Server you want the JVMEntries ConfigObject for.
     * @param procType
     *                     Optional. Specifies which zOSProcessType type for this server you want to get the JVMEntries for. If
     *                     the value is null, then Servant is used as a default. This parameter is ignored on non z/OS platforms.
     *
     * @throws Exception
     */
//    public static ConfigObject getJVMEntries(Server appServ, zOSProcessType procType) throws Exception {
//        if (procType == null)
//            procType = zOSProcessType.Servant;
//
//        ConfigObject jvmProcessDefCfgObj = null;
//        if (appServ.getNode().getMachine().getOperatingSystem().equals(OperatingSystem.ZOS)) {
//            // We are on z/OS. Need to get the process def for the procType.
//            CommonTasks.writeLogMsg(Level.INFO, "z/OS detected. Getting the Process Definition ConfigObject for " + procType.toString());
//            List<ConfigObject> jvmProcessDefCfgObjList = ConfigObject.getConfigObjectList(appServ, appServ.getConfigId(),
//                                                                                          "JavaProcessDef");
//            for (ConfigObject cfgObj : jvmProcessDefCfgObjList) {
//                if (cfgObj.getAttributeByName("processType").getValueAsString().equals(procType.toString())) {
//                    // We have the zOSProcessType process def.
//                    CommonTasks.writeLogMsg(Level.INFO, "Found the " + procType + " Process Definition.");
//                    jvmProcessDefCfgObj = cfgObj;
//                    break; // no need to complete the for loop
//                } else {
//                    CommonTasks.writeLogMsg(Level.INFO, "Found the Process Definition for "
//                                                        + cfgObj.getAttributeByName("processType").getValueAsString());
//                }
//            }
//        } else {
//            // distributed systems have only one JavaProcessDef configObject
//            CommonTasks.writeLogMsg(Level.INFO, "Getting the Process Definition ConfigObject");
//            jvmProcessDefCfgObj = ConfigObject.getConfigObject(appServ, appServ.getConfigId(), "JavaProcessDef");
//        }
//        if (jvmProcessDefCfgObj == null)
//            CommonTasks.writeLogMsg(Level.WARNING, "Warning Process Definition ConfigObject is null.");
//        ConfigObject jvmEntriesCfgObj = ConfigObject.getConfigObject(appServ, jvmProcessDefCfgObj.getConfigIdentifier(),
//                                                                     "JavaVirtualMachine");
//        return jvmEntriesCfgObj;
//    }

    /**
     * Sets JVM system property / custom JVM property.
     *
     * @param theServer
     *                          The application server on which to set the JVM system property.
     * @param zProcType
     *                          The zOSProcessType to use if on z/OS platform. Determine which process type updated on z/OS.
     * @param propertyName
     *                          The property name.
     * @param propertyValue
     *                          The value to set the property to. If the value passed in is null, and a property already exists in the
     *                          config, the property will be removed from the config.
     * @param requiredProp
     *                          A boolean representing if the property is required or not.
     * @return boolean Boolean value of true if changes to the configuration were required, false if no changes were
     *         needed.
     */
//    public static boolean setCustomJVMPropertyInConfig(LibertyServer theServer, CommonTasks.zOSProcessType zProcType,
//                                                       String propertyName, String propertyValue, boolean requiredProp) throws Exception {
//        CommonTasks.writeLogMsg(Level.INFO, "Setting custom property " + propertyName + " with a value of " + propertyValue);
//        Workspace tmpWS = theServer.getNode().getCell().getWorkspace();
//        ConfigObject jvmEntriesCfgObj = CommonTasks.getJVMEntries(theServer, zProcType);
//        if (null == jvmEntriesCfgObj)
//            throw new Exception("Could not obtain Process JVMEntries");
//
//        // Check if and entry for propertyName already exists
//        ConfigObject aProperty = null;
//        List<ConfigObject> jvmCustomProps = jvmEntriesCfgObj.getChildObjects();
//        if (null == jvmCustomProps)
//            throw new Exception("Could not obtain JVM Properties");
//
//        for (ConfigObject i : jvmCustomProps) {
//            // check if we have an existing matching property.
//            if ((i.getName().equals("systemProperties"))
//                    && i.getAttributeByName("name").getValueAsString().equalsIgnoreCase(propertyName)) {
//                CommonTasks.writeLogMsg(Level.INFO, "Found system Propery " + propertyName + " in config with a value of "
//                                                    + i.getAttributeByName("value").getValueAsString());
//                aProperty = i;
//                break;
//            }
//        }
//
//        if (aProperty != null) {
//            // The property already exists in the configuration.
//            if (null == propertyValue) { // remove the property since the value passed in was null.
//                CommonTasks.writeLogMsg(Level.FINE, "Removing the systemPropery " + propertyName + " since set value is null.");
//                jvmCustomProps.remove(aProperty);
//            } else {
//                if ((propertyValue.equals(aProperty.getAttributeByName("value").getValueAsString()))
//                        && (requiredProp == aProperty.getAttributeByName("required").getValueAsBoolean())) {
//                    // The property is already set to what we want. Do nothing.
//                    CommonTasks.writeLogMsg(Level.INFO, "The property " + propertyName + " already set to " + propertyValue
//                                                        + ". No changes were made.");
//                    return false; // return that no changes were needed.
//                } else {
//                    // The property is not set to what we want. Change the value.
//                    aProperty.getAttributeByName("value").setValue(propertyValue);
//                    aProperty.getAttributeByName("required").setValue(requiredProp);
//                    CommonTasks.writeLogMsg(Level.INFO, "Updated JVM custom property " + propertyName + " with a value of "
//                                                        + propertyValue);
//                }
//            }
//        } else {
//            if (null != propertyValue) { // don't create if propertyValue was null
//                // The property does not already exist.
//                CommonTasks.writeLogMsg(Level.FINE, "The property " + propertyName + " does not exists.");
//
//                // we need to create the property and set it's value.
//                CommonTasks.writeLogMsg(Level.INFO, "Creating the new JVM custom / System property " + propertyName);
//                ConfigObject newJvmProperty = ConfigObject.createConfigObject(theServer, "Property", jvmEntriesCfgObj);
//                CommonTasks.writeLogMsg(Level.FINE, "Setting attributes / name and value for new JVM custom property");
//                newJvmProperty.getAttributeByName("name").setValue(propertyName);
//                newJvmProperty.getAttributeByName("value").setValue(propertyValue);
//                newJvmProperty.getAttributeByName("required").setValue("false");
//            }
//        }
//
//        // if we have not already returned false, then we changed something and need to save & sync before we return
//        // true that we have changed the configuration.
//        CommonTasks.writeLogMsg(Level.FINE, "Calling saveAndSync() on the workspace to persist changes.");
//        tmpWS.saveAndSync();
//        return true;
//    }

    /**
     * Gets the value as a String of a JVM system property / custom JVM property. If the property does not exist a value
     * of null is returned.
     *
     * @param theServer
     *                         The application server on which to set the JVM system property.
     * @param zProcType
     *                         The zOSProcessType to use if on z/OS platform. Determine which process type updated on z/OS.
     * @param propertyName
     *                         The property name.
     * @return propertyValue The value to set the property to. Returns null if property is not found.
     */
//    public String getCustomJVMPropertyInConfig(LibertyServer theServer, CommonTasks.zOSProcessType zProcType, String propertyName)
//                    throws Exception {
//        CommonTasks.writeLogMsg(Level.INFO, "Getting custom property " + propertyName);
//        ConfigObject jvmEntriesCfgObj = CommonTasks.getJVMEntries(theServer, zProcType);
//        if (null == jvmEntriesCfgObj)
//            throw new Exception("Could not obtain Process JVMEntries");
//
//        // Check if and entry for propertyName already exists
//        List<ConfigObject> jvmCustomProps = jvmEntriesCfgObj.getChildObjects();
//        if (null == jvmCustomProps)
//            throw new Exception("Could not obtain JVM Properties"); // there may not be any child props but there should
//        // be child objects still.
//
//        for (ConfigObject i : jvmCustomProps) {
//            // check if we have an existing property.
//            if ((i.getName().equals("systemProperties"))
//                    && i.getAttributeByName("name").getValueAsString().equalsIgnoreCase(propertyName)) {
//                CommonTasks.writeLogMsg(Level.INFO, "Found system Propery " + propertyName + " in config with a value of "
//                                                    + i.getAttributeByName("value").getValueAsString());
//                return i.getAttributeByName("value").getValueAsString();
//            }
//        }
//        return null;
//    }
}
