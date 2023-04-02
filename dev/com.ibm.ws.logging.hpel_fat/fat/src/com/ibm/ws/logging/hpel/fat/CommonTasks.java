/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
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
package com.ibm.ws.logging.hpel.fat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xml.sax.SAXException;

import com.ibm.websphere.simplicity.PortType;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

import componenttest.topology.impl.LibertyServer;

/**
 * Holding class for driving common logic across multiple test cases/drivers
 *
 */
public class CommonTasks {

    public static final String HPEL_APP_CONTEXT_ROOT = "HpelFat";
    public static final String HPEL_APP_CREATE_LOG_JSP = "LogCreator.jsp";
    public static final String LOGS = "Logs";
    public static final String TRACE = "Trace";
    public static final String LOGS_TRACE = "LogsAndTrace";

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
        OutputStream os = bootstrapFile.openForWriting(true);
        bootstrapProps.store(os, null);
        os.close();
        return true;

        // TODO Enabling or disabling HPEL requires us to bounce the server - don't know if we should do that here or
        // not though? for now that is the caller's responsibility as they may not want it restarted and or have other
        // changes they are making before the restart.
    }

    public static String getHpelTraceSpec(LibertyServer aServer) throws Exception {
        LibertyServer server = aServer;
        String traceSpec = server.getServerConfiguration().getLogging().getTraceSpecification();
        return traceSpec == null ? "" : traceSpec;
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
        }

    }

    public static RemoteFile getBinaryLogDir(LibertyServer aServer) throws Exception {
        String logDir = aServer.getServerConfiguration().getLogging().getLogDirectory();
        return logDir == null ? aServer.getFileFromLibertyServerRoot("logs") : new RemoteFile(aServer.getMachine(), logDir);
    }

    public static RemoteFile getBinaryTraceDir(LibertyServer aServer) throws Exception {
        String logDir = aServer.getServerConfiguration().getLogging().getLogDirectory();
        return logDir == null ? aServer.getFileFromLibertyServerRoot("logs") : new RemoteFile(aServer.getMachine(), logDir);
    }

    public static RemoteFile getTextLogDir(LibertyServer aServer) throws Exception {
        String logDir = aServer.getServerConfiguration().getLogging().getLogDirectory();
        return logDir == null ? aServer.getFileFromLibertyServerRoot("logs") : new RemoteFile(aServer.getMachine(), logDir);
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
        InputStream is = bootstrapFile.openForReading();
        bootstrapProps.load(is);
        bootstrapProps.remove(propertyName);
        is.close();
        OutputStream os = bootstrapFile.openForWriting(false);
        bootstrapProps.store(os, null);
        os.close();

    }
}
