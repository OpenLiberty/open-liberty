/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.impl;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.ibm.websphere.logging.WsLevel;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.TruncatableThrowable;
import com.ibm.ws.collector.manager.buffer.BufferManagerEMQHelper;
import com.ibm.ws.collector.manager.buffer.BufferManagerImpl;
import com.ibm.ws.collector.manager.buffer.SimpleRotatingSoftQueue;
import com.ibm.ws.kernel.boot.logging.LoggerHandlerManager;
import com.ibm.ws.kernel.boot.logging.WsLogManager;
import com.ibm.ws.logging.RoutedMessage;
import com.ibm.ws.logging.WsLogHandler;
import com.ibm.ws.logging.WsMessageRouter;
import com.ibm.ws.logging.WsTraceRouter;
import com.ibm.ws.logging.collector.CollectorConstants;
import com.ibm.ws.logging.collector.CollectorJsonHelpers;
import com.ibm.ws.logging.data.AccessLogConfig;
import com.ibm.ws.logging.data.AccessLogData;
import com.ibm.ws.logging.data.AuditData;
import com.ibm.ws.logging.data.FFDCData;
import com.ibm.ws.logging.data.JSONObject;
import com.ibm.ws.logging.data.JSONObject.JSONObjectBuilder;
import com.ibm.ws.logging.data.LogTraceData;
import com.ibm.ws.logging.internal.NLSConstants;
import com.ibm.ws.logging.internal.PackageProcessor;
import com.ibm.ws.logging.internal.TraceSpecification;
import com.ibm.ws.logging.internal.WsLogRecord;
import com.ibm.ws.logging.source.LogSource;
import com.ibm.ws.logging.source.TraceSource;
import com.ibm.ws.logging.utils.CollectorManagerPipelineUtils;
import com.ibm.ws.logging.utils.FileLogHolder;
import com.ibm.ws.logging.utils.RecursionCounter;
import com.ibm.ws.logging.utils.SequenceNumber;
import com.ibm.wsspi.collector.manager.SynchronousHandler;
import com.ibm.wsspi.logging.LogHandler;
import com.ibm.wsspi.logging.MessageRouter;
import com.ibm.wsspi.logprovider.LogProviderConfig;
import com.ibm.wsspi.logprovider.TrService;

/**
 * This is the default delegate used by Tr when another hasn't been specified.
 *
 * Translated message generation via these methods:
 * <ul>
 * <li><code>info(...)</code></li>
 * <li><code>audit(...)</code></li>
 * <li><code>warning(...)</code></li>
 * <li><code>error(...)</code></li>
 * <li><code>fatal(...)</code></li>
 * <li><code>service(...)</code></li>
 * </ul>
 * Untranslated trace generation via these methods:
 * <ul>
 * <li><code>entry(...)</code></li>
 * <li><code>exit(...)</code></li>
 * <li><code>debug(...)</code></li>
 * <li><code>dump(...)</code></li>
 * <li><code>event(...)</code></li>
 * </ul>
 * <p>
 * This class is more complicated than might be expected, and this is why...
 * <p>
 * There are three expected targets for output:
 * <ul>
 * <li>"The console" -- these are messages printed directly to the orignal system out and
 * error streams. There is a level filter (INFO, AUDIT, WARNING, ERROR) that determines
 * which of our translated messages are also routed to "the console". This service does
 * not assume control of log file management for these streams. If the server is launched
 * via the scripts, the system streams are piped to a specified file (or even to /dev/null).
 * In the case of the eclipse tools, It will render that piped output in the console view.
 * As our script merges out and err into one file, we will prefix system error output
 * with [err] to assist the tools in making error messages pretty colors (red).
 * When copySystemStreams is false, we will not copy System.out or System.err to console.log.</li>
 * <li>messages.log -- this file will always exist, and will contain INFO+ messages in
 * addition to system out and err. It will used the specified format (BASIC, ENHANCED,
 * or ADVANCED). System out and err are "formatted"-- they are in the same format as
 * the INFO+ messages. </li>
 * <li>trace.log -- this file may or may not exist. If a trace specification string is
 * used that enables "trace" (event or lower), the trace.log file will be created.
 * It will used the specified format (BASIC, ENHANCED, or ADVANCED). There is another
 * option specifiable only at boot time: if the trace file name is java.util.logging,
 * a fixed handler for JSR47 logging will be created that creates and publishes log
 * records for all calls to the Tr API. Our JSR47 LogHandler will be removed. This is
 * a permanent setting (i.e. the server must be restarted to revert to regular trace
 * processing). In this case, the user is opting out of dynamic/server management
 * of trace settings in favor of JSR47 configuration.</li>
 * </ul>
 * <p>
 * There are some other "complications" -- we want to avoid reformatting stuff
 * where we can, and we need to be able to verify during test that formatted
 * messages were output to the expected streams.
 * <p>
 * Phew.
 */
public class BaseTraceService implements TrService {

    static final PrintStream rawSystemOut = System.out;
    static final PrintStream rawSystemErr = System.err;

    /** Special trace component for system streams: this one "remembers" the original system out */
    protected final SystemLogHolder systemOut;
    /** Special trace component for system streams: this one "remembers" the original system err */
    protected final SystemLogHolder systemErr;

    public static final Object NULL_ID = null;
    public static final Logger NULL_LOGGER = null;
    public static final String NULL_FORMATTED_MSG = null;

    protected static RecursionCounter counterForTraceRouter = new RecursionCounter();
    protected static RecursionCounter counterForMessageRouter = new RecursionCounter();
    protected static RecursionCounter counterForTraceSource = new RecursionCounter();
    protected static RecursionCounter counterForTraceWriter = new RecursionCounter();
    protected static RecursionCounter counterForLogSource = new RecursionCounter();

    private static final int MINUTE = 60000;

    /**
     * Trivial interface for writing "trace" records (this includes logging to messages.log)
     */
    public static interface TraceWriter extends Closeable {
        /** Log a single string */
        public void writeRecord(String record);
    }

    /**
     * External (SPI) MessageRouter.
     *
     * This is a reference set when the osgi bundle is started. The {@link MessageRouter} in the logging osgi bundle
     * looks for registered {@link LogHandler} providers in the service registry.
     */
    protected final AtomicReference<MessageRouter> externalMessageRouter = new AtomicReference<MessageRouter>();

    /**
     * Internal MessageRouter.
     *
     * This is a reference set when the osgi bundle is started. The {@link WsMessageRouter} in the logging osgi bundle
     * looks for registered {@link WsLogHandler} providers in the service registry.
     */
    protected final AtomicReference<WsMessageRouter> internalMessageRouter = new AtomicReference<WsMessageRouter>();
    protected final AtomicReference<WsTraceRouter> internalTraceRouter = new AtomicReference<WsTraceRouter>();

    /** This is the filter for which messages go to "the console". One of INFO, AUDIT, WARNING, ERROR */
    protected volatile Level consoleLogLevel = WsLevel.AUDIT;

    /** Copy System.out and System.err invocations to the original system streams */
    protected volatile boolean copySystemStreams = true;

    /** This is the filter for which messages go to "the console". One of INFO, AUDIT, WARNING, ERROR */
    protected volatile BaseTraceFormatter formatter;

    /** If true, format the date and time format for log entries in messages.log, trace.log, and FFDC files in ISO-8601 format. */
    protected volatile boolean isoDateFormat = false;

    /** If value is logFormat, JSON logs will be formatted to include the fields specified in http access logging's logFormat field */
    protected volatile String jsonAccessLogFields = LoggingConstants.DEFAULT_JSON_ACCESS_LOG_FIELDS;

    /** Writer sending messages to the messages.log file */
    protected volatile TraceWriter messagesLog = null;

    /** Writer sending messages to trace.log */
    protected volatile TraceWriter traceLog = null;

    /** A PrintStream that tees to the original System.out and to our logs. */
    protected TeePrintStream teeOut = null;
    /** A PrintStream that tees to the original System.err and to our logs. */
    protected TeePrintStream teeErr = null;

    /** The header written at the beginning of all log files. */
    private String logHeader;
    /** True if java.lang.instrument is available for trace. */
    private boolean javaLangInstrument;

    /** Check to see if HPEL is enabled **/
    private boolean isHpelEnabled;

    /** Configured message Ids to be suppressed in console/message.log */
    private volatile Collection<String> hideMessageids;

    /** Early msgs issued before MessageRouter is started. */
    protected volatile Queue<RoutedMessage> earlierMessages = new SimpleRotatingSoftQueue<RoutedMessage>(new RoutedMessage[100]);
    protected volatile Queue<RoutedMessage> earlierTraces = new SimpleRotatingSoftQueue<RoutedMessage>(new RoutedMessage[200]);

    protected volatile LogSource logSource = null;
    protected volatile TraceSource traceSource = null;
    protected volatile MessageLogHandler messageLogHandler = null;
    protected volatile ConsoleLogHandler consoleLogHandler = null;
    protected volatile BufferManagerImpl logConduit;
    protected volatile BufferManagerImpl traceConduit;
    protected volatile CollectorManagerPipelineUtils collectorMgrPipelineUtils = null;
    protected volatile Timer earlyMessageTraceKiller_Timer = new Timer();

    protected volatile String serverName = null;
    protected volatile String wlpUserDir = null;
    private static final String OMIT_FIELDS_STRING = "@@@OMIT@@@";

    /** Flags for suppressing traceback output to the console */
    private static class StackTraceFlags {
        boolean needsToOutputInternalPackageMarker = false;
        boolean isSuppressingTraces = false;
    }

    /** Track the stack trace printing activity of the current thread */
    private static ThreadLocal<StackTraceFlags> traceFlags = new ThreadLocal<StackTraceFlags>() {
        @Override
        protected StackTraceFlags initialValue() {
            return new StackTraceFlags();
        }
    };

    /**
     * Called from Tr.getDelegate when BaseTraceService delegate is created
     */
    public BaseTraceService() {
        systemOut = new SystemLogHolder(LoggingConstants.SYSTEM_OUT, System.out);
        systemErr = new SystemLogHolder(LoggingConstants.SYSTEM_ERR, System.err);

        earlyMessageTraceKiller_Timer.schedule(new EarlyMessageTraceCleaner(), 5 * MINUTE); // 5 minutes wait time
    }

    /**
     * {@inheritDoc} <p>
     * This is called at initialization time: The bootstrap/launcher uses reflection to
     * create a LogProvider; in our case, this is the {@link LogProviderImpl}. Our
     * LogProvider creates a {@link LogProviderConfigImpl}, which parses the set
     * of system properties we expect (for FFDC and logging).
     *
     * @param config a {@link LogProviderConfigImpl} containing TrService configuration
     *                   from bootstrap properties
     */
    @Override
    public void init(LogProviderConfig config) {
        // Check to see if the Log provider is Binary Logging
        isHpelEnabled = WsLogManager.isBinaryLoggingEnabled();

        update(config);

        registerLoggerHandlerSingleton();
        // Capture System.out/.err after registerLoggerHandler has initialized
        // LogManager, which might print errors due to misconfiguration.
        captureSystemStreams();
        //Remove EMQ from BufferManager after a certain amount of time has passed
        BufferManagerEMQHelper.removeEMQByTimer();
    }

    protected void registerLoggerHandlerSingleton() {
        // Add our handler to catch and format records produced with logger
        LoggerHandlerManager.setSingleton(new Handler() {
            @Override
            public void publish(LogRecord logRecord) {
                BaseTraceService.this.publishLogRecord(logRecord);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
    }

    protected void unregisterLoggerHandlerSingleton() {
        // Remove the handler to catch and format records produced with logger
        LoggerHandlerManager.unsetSingleton();
    }

    /**
     * {@inheritDoc} <p>
     * This method is triggered by the managed service listening for logging
     * configuration updates from config admin & metatype processing.
     * Tr metatype is defined to preserve variables set in bootstrap.properties
     * so values set there are not unset by metatype defaults.
     *
     * @param config a {@link LogProviderConfigImpl} containing dynamic updates from
     *                   the OSGi managed service.
     */
    @Override
    public synchronized void update(LogProviderConfig config) {
        LogProviderConfigImpl trConfig = (LogProviderConfigImpl) config;
        logHeader = trConfig.getLogHeader();
        javaLangInstrument = trConfig.hasJavaLangInstrument();
        consoleLogLevel = trConfig.getConsoleLogLevel();
        copySystemStreams = trConfig.copySystemStreams();
        hideMessageids = trConfig.getMessagesToHide();
        //add hideMessageIds to log header, only for default logging, since for binary logging, the messages will be only hidden in console.log.
        //This is printed when its configured in bootstrap.properties
        if (hideMessageids.size() > 0 && !isHpelEnabled) {
            logHeader = logHeader.concat("Suppressed message ids: " + hideMessageids).concat((LoggingConstants.nl));
        }
        if (formatter == null || trConfig.getTraceFormat() != formatter.getTraceFormat()) {
            formatter = new BaseTraceFormatter(trConfig.getTraceFormat());
        }

        //Gets the configured boolean value to determine if the date and time should be in ISO-8601 format
        isoDateFormat = trConfig.getIsoDateFormat();
        if (isoDateFormat != BaseTraceFormatter.useIsoDateFormat) {
            BaseTraceFormatter.useIsoDateFormat = isoDateFormat;
        }

        jsonAccessLogFields = trConfig.getjsonAccessLogFields();
        if (jsonAccessLogFields != AccessLogConfig.jsonAccessLogFieldsConfig) {
            AccessLogConfig.jsonAccessLogFieldsConfig = jsonAccessLogFields;
        }

        initializeWriters(trConfig);
        if (hideMessageids.size() > 0) {
            String msgKey = isHpelEnabled ? "MESSAGES_CONFIGURED_HIDDEN_HPEL" : "MESSAGES_CONFIGURED_HIDDEN_2";
            Tr.info(TraceSpecification.getTc(), msgKey, new Object[] { hideMessageids });
        }

        /*
         * Need to know the values of wlpServerName and wlpUserDir
         * They are passed into the handlers for use as part of the jsonified output
         */
        serverName = trConfig.getServerName();
        wlpUserDir = trConfig.getWlpUsrDir();

        //Retrieve collectormgrPiplineUtils
        if (collectorMgrPipelineUtils == null) {
            collectorMgrPipelineUtils = CollectorManagerPipelineUtils.getInstance();
        }

        //Sources
        logSource = collectorMgrPipelineUtils.getLogSource();
        traceSource = collectorMgrPipelineUtils.getTraceSource();

        //Conduits
        logConduit = collectorMgrPipelineUtils.getLogConduit();
        traceConduit = collectorMgrPipelineUtils.getTraceConduit();

        Boolean appsWriteJson = trConfig.getAppsWriteJson();

        /*
         * Retrieve the format setting for message.log and console
         */
        String messageFormat = trConfig.getMessageFormat();
        String consoleFormat = trConfig.getConsoleFormat();

        //Retrieve the source lists of both message and console
        List<String> messageSourceList = new ArrayList<String>(trConfig.getMessageSource());
        List<String> consoleSourceList = new ArrayList<String>(trConfig.getConsoleSource());

        /*
         * Filter out Message and Trace from messageSourceList
         * This is so that Handler doesn't 'subscribe' message or trace
         * and kicks off an undesired BufferManagerImpl instance.
         */
        List<String> filterdMessageSourceList = filterSourcelist(messageSourceList);
        List<String> filterdConsoleSourceList = filterSourcelist(consoleSourceList);

        /*
         * Create the MessageLogHandler and ConsoleLogHandler if they do not exist yet.
         * If we do not then they will not be appropriately registered with CollectorManager
         * when the CollectorManagerConfigurator is registering services. The consequence is that
         * if the user wishes to switch to 'json' messages or console at a later time (other than
         * startup) they will not be able to subscribe to accessLog and ffdc sources.
         */
        if (messageLogHandler == null && messagesLog != null) {
            messageLogHandler = new MessageLogHandler(serverName, wlpUserDir, filterdMessageSourceList);
            collectorMgrPipelineUtils.setMessageHandler(messageLogHandler);
            messageLogHandler.setWriter(messagesLog);
        }

        if (consoleLogHandler == null) {
            consoleLogHandler = new ConsoleLogHandler(serverName, wlpUserDir, filterdConsoleSourceList);
            collectorMgrPipelineUtils.setConsoleHandler(consoleLogHandler);
            consoleLogHandler.setWriter(systemOut, systemErr);
            consoleLogHandler.setBaseTraceService(this);
        }

        commonMessageLogHandlerUpdates();
        commonConsoleLogHandlerUpdates();

        /*
         * If messageFormat has been configured to 'simple' or the deprecated format name 'basic' OR if messageFormat is not a valid format (default to simple)
         * - ensure that we are not connecting conduits/bufferManagers to the handler
         * otherwise we would have the undesired effect of writing both 'simple' and 'json' formatted message events
         */
        if ((messageFormat.toLowerCase().equals(LoggingConstants.DEFAULT_MESSAGE_FORMAT) || messageFormat.toLowerCase().equals(LoggingConstants.DEPRECATED_DEFAULT_FORMAT))
            || !(LoggingConfigUtils.isMessageFormatValueValid(messageFormat))) {
            if (messageLogHandler != null) {
                messageLogHandler.setFormat(LoggingConstants.DEFAULT_MESSAGE_FORMAT);
                messageLogHandler.modified(new ArrayList<String>());
                ArrayList<String> filteredList = new ArrayList<String>();
                filteredList.add(LoggingConstants.DEFAULT_CONSOLE_SOURCE);
                updateConduitSyncHandlerConnection(filteredList, messageLogHandler);
            }
        }

        /*
         * If consoleFormat has been configured to 'dev' or the deprecated format name 'basic' or the default message format 'simple' OR if consoleFormat is not a valid format
         * (default to dev)
         * - ensure that we are not connecting conduits/bufferManagers to the handler
         * otherwise we would have the undesired effect of writing both 'dev'/'simple' and 'json' formatted message events
         */
        if ((consoleFormat.toLowerCase().equals(LoggingConstants.DEFAULT_CONSOLE_FORMAT) || consoleFormat.toLowerCase().equals(LoggingConstants.DEPRECATED_DEFAULT_FORMAT)
             || consoleFormat.toLowerCase().equals(LoggingConstants.DEFAULT_MESSAGE_FORMAT))
            || !(LoggingConfigUtils.isConsoleFormatValueValid(consoleFormat))) {
            if (consoleLogHandler != null) {
                if (consoleFormat.toLowerCase().equals(LoggingConstants.DEFAULT_MESSAGE_FORMAT))
                    consoleLogHandler.setFormat(LoggingConstants.DEFAULT_MESSAGE_FORMAT);
                else
                    consoleLogHandler.setFormat(LoggingConstants.DEFAULT_CONSOLE_FORMAT);

                ArrayList<String> filteredList = new ArrayList<String>();
                filteredList.add(LoggingConstants.DEFAULT_CONSOLE_SOURCE);
                if (traceLog == systemOut) {
                    filteredList.add(LoggingConstants.DEFAULT_TRACE_SOURCE);
                    consoleLogHandler.setTraceStdout(true);
                } else {
                    consoleLogHandler.setTraceStdout(false);
                }
                consoleLogHandler.modified(new ArrayList<String>());
                updateConduitSyncHandlerConnection(filteredList, consoleLogHandler);
            }
        }

        /*
         * If messageFormat has been configured to 'json', create the messageLogHandler as necessary or
         * call modified as necessary, provide it to the collectorMgrPipleLinUtils as necessary and set the
         * messageJsonConfigured flag as appropriate and update the connection between the unique message
         * and trace conduits to the handler.
         */
        if (messageFormat.toLowerCase().equals(LoggingConstants.JSON_FORMAT)) {
            if (messageLogHandler != null) {
                messageLogHandler.setFormat(LoggingConstants.JSON_FORMAT);
                messageLogHandler.setAppsWriteJson(appsWriteJson);
                //Connect the conduits to the handler as necessary
                messageLogHandler.modified(filterdMessageSourceList);
                updateConduitSyncHandlerConnection(messageSourceList, messageLogHandler);
            }

        }

        /*
         * If consoleFormat has been configured to 'json', create the consoleLogHandler as necessary or
         * call modified as necessary, provide it to the collectorMgrPipleLinUtils as necessary and set the
         * consoleJsonConfigured flag as appropriate and update the connection between the unique message
         * and trace conduits to the handler.
         */
        if (consoleFormat.toLowerCase().equals(LoggingConstants.JSON_FORMAT)) {
            if (consoleLogHandler != null) {
                consoleLogHandler.setFormat(LoggingConstants.JSON_FORMAT);
                consoleLogHandler.setAppsWriteJson(appsWriteJson);
                //Connect the conduits to the handler as necessary
                //if json && messages, trace sourcelist
                consoleLogHandler.modified(filterdConsoleSourceList);
                updateConduitSyncHandlerConnection(consoleSourceList, consoleLogHandler);
            }
        }

        applyJsonFields(trConfig.getjsonFields());
    }

    public static void applyJsonFields(String value) {
        if (value == null || value == "" || value.isEmpty()) { //reset all fields to original when server config has ""
            AccessLogData.resetJsonLoggingNameAliases();
            FFDCData.resetJsonLoggingNameAliases();
            LogTraceData.resetJsonLoggingNameAliasesMessage();
            LogTraceData.resetJsonLoggingNameAliasesTrace();
            AuditData.resetJsonLoggingNameAliases();

            // Re-initialize the field mappings
            CollectorJsonHelpers.updateFieldMappings();
            //if no property is set, return
            return;
        }

        TraceComponent tc = Tr.register(LogTraceData.class, NLSConstants.GROUP, NLSConstants.LOGGING_NLS);
        boolean valueFound = false;
        boolean isInvalidEventType = false;
        Map<String, String> messageMap = new HashMap<>();
        Map<String, String> traceMap = new HashMap<>();
        Map<String, String> ffdcMap = new HashMap<>();
        Map<String, String> accessLogMap = new HashMap<>();
        Map<String, String> auditMap = new HashMap<>();
        // For access log data
        Map<String, String> accessLogCookiesMap = new HashMap<>();
        Map<String, String> accessLogRequestHeaderMap = new HashMap<>();
        Map<String, String> accessLogResponseHeaderMap = new HashMap<>();

        List<String> LogTraceList = Arrays.asList(LogTraceData.NAMES1_1);
        List<String> FFDCList = Arrays.asList(FFDCData.NAMES1_1);
        List<String> AccessLogList = Arrays.asList(AccessLogData.NAMES1_1);
        List<String> AuditList = Arrays.asList(AuditData.NAMES1_1);

        String[] keyValuePairs = value.split(","); //split the string to create key-value pairs
        for (String pair : keyValuePairs) //iterate over the pairs
        {
            pair = pair.trim();
            if (pair.endsWith(":"))
                pair = pair + OMIT_FIELDS_STRING;

            String[] entry = pair.split(":"); //split the pairs to get key and value
            entry[0] = entry[0].trim();

            //!pair.endsWith(":") for beta guard for entry length 2 because ie. message:type: will rename message to type
            if (entry.length == 2) {//if the mapped value is intended for all event types
                entry[1] = entry[1].trim();
                //add properties to all the hashmaps and trim whitespaces
                if (LogTraceList.contains(entry[0])) {
                    messageMap.put(entry[0], entry[1]);
                    traceMap.put(entry[0], entry[1]);
                    valueFound = true;
                }
                if (FFDCList.contains(entry[0])) {
                    ffdcMap.put(entry[0], entry[1]);
                    valueFound = true;
                }
                if (AccessLogList.contains(entry[0])) {
                    accessLogMap.put(entry[0], entry[1]);
                    valueFound = true;
                }
                if (AuditList.contains(entry[0])) {
                    auditMap.put(entry[0], entry[1]);
                    valueFound = true;
                }
                //Check if mapped value is an extension
                if (entry[0].startsWith("ext_")) {
                    messageMap.put(entry[0], entry[1]);
                    traceMap.put(entry[0], entry[1]);
                    valueFound = true;
                }

                // headers and cookies are special because there can be multiple instances
                // only applicable to liberty_accesslog
                if (entry[0].contains("ibm_cookie_")) {
                    accessLogCookiesMap.put(entry[0].substring(11), entry[1]);
                    valueFound = true;
                } else if (entry[0].contains("ibm_requestHeader_")) {
                    // headers are NOT case sensitive, so lowercase them all
                    accessLogRequestHeaderMap.put(entry[0].substring(18).toLowerCase(), entry[1]);
                    valueFound = true;
                } else if (entry[0].contains("ibm_responseHeader_")) {
                    accessLogResponseHeaderMap.put(entry[0].substring(19).toLowerCase(), entry[1]);
                    valueFound = true;
                }

                if (!valueFound) {
                    //if the value does not exist in any of the known keys, give a warning
                    Tr.warning(tc, "JSON_FIELDS_NO_MATCH");
                }
                valueFound = false;//reset valueFound boolean

            } else if (entry.length == 3) {
                entry[1] = entry[1].trim();
                entry[2] = entry[2].trim();
                //add properties to their respective hashmaps and trim whitespaces
                if (CollectorConstants.MESSAGES_CONFIG_VAL.equals(entry[0])) {
                    if (LogTraceList.contains(entry[1]) || entry[1].startsWith("ext_")) {
                        messageMap.put(entry[1], entry[2]);
                        valueFound = true;
                    }
                } else if (CollectorConstants.TRACE_CONFIG_VAL.equals(entry[0])) {
                    if (LogTraceList.contains(entry[1]) || entry[1].startsWith("ext_")) {
                        traceMap.put(entry[1], entry[2]);
                        valueFound = true;
                    }
                } else if (CollectorConstants.FFDC_CONFIG_VAL.equals(entry[0])) {
                    if (FFDCList.contains(entry[1])) {
                        ffdcMap.put(entry[1], entry[2]);
                        valueFound = true;
                    }
                } else if (CollectorConstants.ACCESS_CONFIG_VAL.equals(entry[0])) {
                    if (AccessLogList.contains(entry[1])) {
                        accessLogMap.put(entry[1], entry[2]);
                        valueFound = true;
                    }
                    // headers and cookies are special because there can be multiple instances
                    if (entry[1].contains("ibm_cookie_")) {
                        accessLogCookiesMap.put(entry[1].substring(11), entry[2]);
                        valueFound = true;
                    } else if (entry[1].contains("ibm_requestHeader_")) {
                        accessLogRequestHeaderMap.put(entry[1].substring(18).toLowerCase(), entry[2]);
                        valueFound = true;
                    } else if (entry[1].contains("ibm_responseHeader_")) {
                        accessLogResponseHeaderMap.put(entry[1].substring(19).toLowerCase(), entry[2]);
                        valueFound = true;
                    }
                } else if (CollectorConstants.AUDIT_CONFIG_VAL.equals(entry[0])) {
                    if (AuditList.contains(entry[1])) {
                        auditMap.put(entry[1], entry[2]);
                        valueFound = true;
                    }
                } else {
                    isInvalidEventType = true;
                    Tr.warning(tc, "JSON_FIELDS_INCORRECT_EVENT_TYPE");
                }
                if (!valueFound && !isInvalidEventType) {
                    //if the value does not exist in any of the known keys, give a warning
                    Tr.warning(tc, "JSON_FIELDS_NO_MATCH");
                }
                valueFound = false;
                isInvalidEventType = false;
            } else {
                Tr.warning(tc, "JSON_FIELDS_FORMAT_WARNING_2");
            }

        }

        AccessLogData.newJsonLoggingNameAliases(accessLogMap);
        FFDCData.newJsonLoggingNameAliases(ffdcMap);
        LogTraceData.newJsonLoggingNameAliasesMessage(messageMap);
        LogTraceData.newJsonLoggingNameAliasesTrace(traceMap);
        AuditData.newJsonLoggingNameAliases(auditMap);

        // Renaming/omitting cookie and header access log fields
        AccessLogData.populateDataMaps(accessLogCookiesMap, accessLogRequestHeaderMap, accessLogResponseHeaderMap);

        CollectorJsonHelpers.updateFieldMappings();
    }

    /**
     * common consoleLogHandler
     */
    private void commonConsoleLogHandlerUpdates() {
        if (consoleLogHandler != null) {
            consoleLogHandler.setBasicFormatter(formatter);
            consoleLogHandler.setConsoleLogLevel(consoleLogLevel.intValue());
            consoleLogHandler.setCopySystemStreams(copySystemStreams);
        }
    }

    /**
     * common MessageLogHandlerUpdates
     */
    private void commonMessageLogHandlerUpdates() {
        if (messageLogHandler != null) {
            messageLogHandler.setWriter(messagesLog);
            messageLogHandler.setBasicFormatter(formatter);
        }
    }

    /**
     * {@inheritDoc} <p>
     * This is called by the bootstrap launchers after the framework has stopped.
     * It is called as part of a controlled shutdown, and is guaranteed to run.
     */
    @Override
    public void stop() {

        // Make sure to return the system streams to the state they were in
        // before we mucked with them
        restoreSystemStreams();

        unregisterLoggerHandlerSingleton();

        // Close writers, however they were allocated
        LoggingFileUtils.tryToClose(messagesLog);
        LoggingFileUtils.tryToClose(traceLog);
    }

    @Override
    public void register(TraceComponent tc) {
        // trace component set by caller...
    }

    @Override
    public void info(TraceComponent tc, String msgKey, Object... o) {
        LogRecord logRecord = WsLogRecord.createWsLogRecord(tc, Level.INFO, msgKey, o);
        publishLogRecord(logRecord);
    }

    @Override
    public void audit(TraceComponent tc, String msgKey, Object... o) {
        LogRecord logRecord = WsLogRecord.createWsLogRecord(tc, WsLevel.AUDIT, msgKey, o);
        publishLogRecord(logRecord);
    }

    @Override
    public void warning(TraceComponent tc, String msgKey, Object... o) {
        LogRecord logRecord = WsLogRecord.createWsLogRecord(tc, Level.WARNING, msgKey, o);
        publishLogRecord(logRecord);
    }

    @Override
    public void error(TraceComponent tc, String msgKey, Object... o) {
        LogRecord logRecord = WsLogRecord.createWsLogRecord(tc, WsLevel.ERROR, msgKey, o);
        publishLogRecord(logRecord);
    }

    @Override
    public void fatal(TraceComponent tc, String msgKey, Object... o) {
        WsLogRecord logRecord = WsLogRecord.createWsLogRecord(tc, WsLevel.FATAL, msgKey, o);
        publishLogRecord(logRecord);
    }

    @Override
    public void debug(TraceComponent tc, String txt, Object... o) {
        if (tc.isDebugEnabled()) {
            WsLogRecord logRecord = WsLogRecord.createWsLogRecord(tc, Level.FINEST, txt, o);
            publishTraceLogRecord(traceLog, logRecord, NULL_ID, NULL_FORMATTED_MSG, NULL_FORMATTED_MSG);
        }
    }

    @Override
    public void debug(TraceComponent tc, Object id, String txt, Object... o) {
        if (tc.isDebugEnabled()) {
            WsLogRecord logRecord = WsLogRecord.createWsLogRecord(tc, Level.FINEST, txt, o);
            publishTraceLogRecord(traceLog, logRecord, id, NULL_FORMATTED_MSG, NULL_FORMATTED_MSG);
        }
    }

    @Override
    public void entry(TraceComponent tc, String methodName, Object... o) {
        if (tc.isEntryEnabled()) {
            WsLogRecord logRecord = WsLogRecord.createWsLogRecord(tc, Level.FINER, BaseTraceFormatter.ENTRY, o);
            logRecord.setSourceMethodName(methodName);
            publishTraceLogRecord(traceLog, logRecord, null, NULL_FORMATTED_MSG, NULL_FORMATTED_MSG);
        }
    }

    @Override
    public void entry(TraceComponent tc, Object id, String methodName, Object... o) {
        if (tc.isEntryEnabled()) {
            WsLogRecord logRecord = WsLogRecord.createWsLogRecord(tc, Level.FINER, BaseTraceFormatter.ENTRY, o);
            logRecord.setSourceMethodName(methodName);
            publishTraceLogRecord(traceLog, logRecord, id, NULL_FORMATTED_MSG, NULL_FORMATTED_MSG);
        }
    }

    @Override
    public void event(TraceComponent tc, String txt, Object... o) {
        if (tc.isEventEnabled()) {
            WsLogRecord logRecord = WsLogRecord.createWsLogRecord(tc, WsLevel.EVENT, txt, o);
            publishTraceLogRecord(traceLog, logRecord, NULL_ID, NULL_FORMATTED_MSG, NULL_FORMATTED_MSG);
        }
    }

    @Override
    public void event(TraceComponent tc, Object id, String txt, Object... o) {
        if (tc.isEventEnabled()) {
            WsLogRecord logRecord = WsLogRecord.createWsLogRecord(tc, WsLevel.EVENT, txt, o);
            publishTraceLogRecord(traceLog, logRecord, id, NULL_FORMATTED_MSG, NULL_FORMATTED_MSG);
        }
    }

    @Override
    public void exit(TraceComponent tc, String methodName) {
        if (tc.isEntryEnabled()) {
            WsLogRecord logRecord = WsLogRecord.createWsLogRecord(tc, Level.FINER, BaseTraceFormatter.EXIT, null);
            logRecord.setSourceMethodName(methodName);
            publishTraceLogRecord(traceLog, logRecord, NULL_ID, NULL_FORMATTED_MSG, NULL_FORMATTED_MSG);
        }
    }

    @Override
    public void exit(TraceComponent tc, Object id, String methodName) {
        if (tc.isEntryEnabled()) {
            WsLogRecord logRecord = WsLogRecord.createWsLogRecord(tc, Level.FINER, BaseTraceFormatter.EXIT, null);
            logRecord.setSourceMethodName(methodName);
            publishTraceLogRecord(traceLog, logRecord, id, NULL_FORMATTED_MSG, NULL_FORMATTED_MSG);
        }
    }

    @Override
    public void exit(TraceComponent tc, String methodName, Object o) {
        if (tc.isEntryEnabled()) {
            WsLogRecord logRecord = WsLogRecord.createWsLogRecord(tc, Level.FINER, BaseTraceFormatter.EXIT, new Object[] { o });
            logRecord.setSourceMethodName(methodName);
            publishTraceLogRecord(traceLog, logRecord, NULL_ID, NULL_FORMATTED_MSG, NULL_FORMATTED_MSG);
        }
    }

    @Override
    public void exit(TraceComponent tc, Object id, String methodName, Object o) {
        if (tc.isEntryEnabled()) {
            WsLogRecord logRecord = WsLogRecord.createWsLogRecord(tc, Level.FINER, BaseTraceFormatter.EXIT, new Object[] { o });
            logRecord.setSourceMethodName(methodName);
            publishTraceLogRecord(traceLog, logRecord, id, NULL_FORMATTED_MSG, NULL_FORMATTED_MSG);
        }
    }

    @Override
    public void dump(TraceComponent tc, String txt, Object... o) {
        if (tc.isDumpEnabled()) {
            WsLogRecord logRecord = WsLogRecord.createWsLogRecord(tc, Level.FINEST, "Dump: " + txt, o);
            publishTraceLogRecord(traceLog, logRecord, NULL_ID, NULL_FORMATTED_MSG, NULL_FORMATTED_MSG);
        }
    }

    /**
     * Echo data printed (from multiple sources) to System.out and
     * System.err to target streams:
     * <ul>
     * <li>"the console" -- system streams</li>
     * <li>messages.log</li>
     * <li>trace.log if trace is enabled</li>
     * </ul>
     *
     * @see #writeStreamOutput(SystemLogHolder, String, boolean)
     * @see #publishLogRecord(LogRecord)
     */
    public void echo(SystemLogHolder holder, LogRecord logRecord) {
        TraceWriter detailLog = traceLog;
        // Tee to messages.log (always)

        RoutedMessage routedMessage = null;
        if (externalMessageRouter.get() != null) {
            String message = formatter.messageLogFormat(logRecord, logRecord.getMessage());
            routedMessage = new RoutedMessageImpl(logRecord.getMessage(), logRecord.getMessage(), message, logRecord);
        } else {
            routedMessage = new RoutedMessageImpl(logRecord.getMessage(), logRecord.getMessage(), null, logRecord);
        }
        invokeMessageRouters(routedMessage);
        if (logSource != null) {
            publishToLogSource(routedMessage);
        }
        //send events to handlers
        if (TraceComponent.isAnyTracingEnabled()) {
            publishTraceLogRecord(detailLog, logRecord, NULL_ID, NULL_FORMATTED_MSG, NULL_FORMATTED_MSG);
        }
    }

    /**
     * @param holder
     * @param logRecord
     */
    protected void writeFilteredStreamOutput(SystemLogHolder holder, LogRecord logRecord) {
        String txt = filterStackTraces(logRecord.getMessage());
        if (txt != null) {
            writeStreamOutput(holder, txt, true);
        }
    }

    /**
     * @param formattedMsg - the msg. The msgId MUST be at the start of the message.
     *
     * @return true if the message should be "hidden" from messages.log/console.log; false otherwise.
     */
    protected boolean isMessageHidden(String formattedMsg) {

        for (String messageId : hideMessageids) {
            if (formattedMsg.startsWith(messageId)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Route the message thru both the external (SPI) and internal MessageRouters.
     *
     * @return true if the message should be logged normally; false otherwise (meaning
     *         one of the LogHandlers specifically suppressed the message from normal logging).
     */
    protected boolean invokeMessageRouters(RoutedMessage routedMessage) {

        MessageRouter externalMsgRouter = externalMessageRouter.get();
        WsMessageRouter internalMsgRouter = internalMessageRouter.get();

        boolean retMe = true;

        try {
            /*
             * Need a recursion Counter for invokingMessageRouters
             * If an osgi framework debugging options is enabled then a message
             * is sent to sysout/syserr from wsMessageRouterImpl during the
             * Reentrant Read Write lock and will cause an infinite loop -- crash.
             *
             * Setting it to 2 to allows the "osgi" debug statement to occur once.
             * i.e Original message goes through and then osgi statement is emitted and is looped back once
             * and recursion counter will prevent any further statements.
             */
            if (!(counterForMessageRouter.incrementCount() > 2)) {
                if (externalMsgRouter != null) {
                    retMe &= externalMsgRouter.route(routedMessage.getFormattedMsg(), routedMessage.getLogRecord());
                }
                if (internalMsgRouter != null) {
                    retMe &= internalMsgRouter.route(routedMessage);
                } else {
                    String message = formatter.messageLogFormat(routedMessage.getLogRecord(), routedMessage.getFormattedVerboseMsg());
                    RoutedMessage specialRoutedMessage = new RoutedMessageImpl(routedMessage.getFormattedMsg(), routedMessage.getFormattedVerboseMsg(), message, routedMessage.getLogRecord());
                    synchronized (this) {
                        if (earlierMessages != null) {
                            earlierMessages.add(specialRoutedMessage);
                        }
                    }
                }
            }
        } finally {
            counterForMessageRouter.decrementCount();
        }
        return retMe;
    }

    /**
     * Route only trace log records.Messages including Systemout,err will not be routed to trace source to avoid duplicate entries
     */
    protected boolean invokeTraceRouters(RoutedMessage routedTrace) {

        boolean retMe = true;
        LogRecord logRecord = routedTrace.getLogRecord();
        /*
         * Avoid any feedback traces that are emitted after this point.
         * The first time the counter increments is the first pass-through.
         * The second time the counter increments is the second pass-through due
         * to trace emitted. We do not want any more pass-throughs.
         */
        try {
            if (!(counterForTraceRouter.incrementCount() > 2)) {
                if (logRecord != null) {
                    Level level = logRecord.getLevel();
                    int levelValue = level.intValue();
                    if (levelValue < Level.INFO.intValue()) {
                        String levelName = level.getName();
                        if (!(levelName.equals("SystemOut") || levelName.equals("SystemErr"))) { //SystemOut/Err=700
                            WsTraceRouter internalTrRouter = internalTraceRouter.get();
                            if (internalTrRouter != null) {
                                retMe &= internalTrRouter.route(routedTrace);
                            } else if (earlierTraces != null) {
                                synchronized (this) {
                                    if (earlierTraces != null) {
                                        earlierTraces.add(routedTrace);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            counterForTraceRouter.decrementCount();
        }
        return retMe;
    }

    /**
     * Called by WsLogger (via WsHandler) methods to publish a log record.
     * <p>
     * Message data printed to system streams based on the log level, then passed
     * to another method for other trace/message targets.
     *
     * @see #doTraceLogRecord(LogRecord, Logger)
     */
    @Override
    public void publishLogRecord(LogRecord logRecord) {
        String formattedMsg = null;
        String formattedVerboseMsg = null;

        Level level = logRecord.getLevel();
        int levelValue = level.intValue();
        TraceWriter detailLog = traceLog;

        if (levelValue >= Level.INFO.intValue()) {
            formattedMsg = formatter.formatMessage(logRecord);
            formattedVerboseMsg = formatter.formatVerboseMessage(logRecord, formattedMsg);

            RoutedMessage routedMessage = null;
            if (externalMessageRouter.get() != null) {
                String message = formatter.messageLogFormat(logRecord, formattedVerboseMsg);
                routedMessage = new RoutedMessageImpl(formattedMsg, formattedVerboseMsg, message, logRecord);
            } else {
                routedMessage = new RoutedMessageImpl(formattedMsg, formattedVerboseMsg, null, logRecord);
            }
            // Look for external log handlers. They may suppress "normal" log
            // processing, which would prevent it from showing up in other logs.
            // This has to be checked in this method: direct invocation of system.out
            // and system.err are not subject to message routing.
            boolean logNormally = invokeMessageRouters(routedMessage);
            if (!logNormally)
                return;

            //If any messages configured to be hidden then those will not be written to console.log/message.log and redirected to trace.log.
            if (isMessageHidden(formattedMsg)) {
                publishTraceLogRecord(detailLog, logRecord, NULL_ID, formattedMsg, formattedVerboseMsg);
                return;
            }

            /*
             * Messages sent through LogSource will be received by MessageLogHandler and ConsoleLogHandler
             * if messageFormat and consoleFormat have been set to "json" and "message" is a listed source.
             * However, LogstashCollector and BluemixLogCollector will receive all messages
             */

            // logSource only receives "normal" messages and messages that are not hidden.
            if (logSource != null) {
                publishToLogSource(routedMessage);
            }
        }

        // ODD: note that formattedMsg and formattedVerboseMsg will both be NULL if
        // this message is NOT above INFO Level.  However I believe only INFO-Level + above
        // messages are sent to this method.  So the "if (INFO-Level)" check above is probably
        // unnecessary.

        // Proceed to trace processing for all other log records
        if (TraceComponent.isAnyTracingEnabled()) {
            publishTraceLogRecord(detailLog, logRecord, NULL_ID, formattedMsg, formattedVerboseMsg);
        }
    }

    /**
     * @param routedMessage
     */
    protected void publishToLogSource(RoutedMessage routedMessage) {
        try {
            if (!(counterForLogSource.incrementCount() > 2)) {
                logSource.publish(routedMessage);
            }
        } finally {
            counterForLogSource.decrementCount();
        }
    }

    /**
     * Publish a trace log record.
     *
     * @param detailLog           the trace writer
     * @param logRecord
     * @param id                  the trace object id
     * @param formattedMsg        the result of {@link BaseTraceFormatter#formatMessage}
     * @param formattedVerboseMsg the result of {@link BaseTraceFormatter#formatVerboseMessage}
     */
    protected void publishTraceLogRecord(TraceWriter detailLog, LogRecord logRecord, Object id, String formattedMsg, String formattedVerboseMsg) {
        //check if tracefilename is stdout
        if (formattedVerboseMsg == null) {
            formattedVerboseMsg = formatter.formatVerboseMessage(logRecord, formattedMsg, false);
        }
        RoutedMessage routedTrace = new RoutedMessageImpl(formattedMsg, formattedVerboseMsg, null, logRecord);

        invokeTraceRouters(routedTrace);

        /*
         * Avoid any feedback traces that are emitted after this point.
         * The first time the counter increments is the first pass-through.
         * The second time the counter increments is the second pass-through due
         * to trace emitted. We do not want any more pass-throughs.
         */
        try {
            if (!(counterForTraceSource.incrementCount() > 2)) {
                if (logRecord != null) {
                    Level level = logRecord.getLevel();
                    int levelValue = level.intValue();
                    if (levelValue < Level.INFO.intValue()) {
                        String levelName = level.getName();
                        if (!(levelName.equals("SystemOut") || levelName.equals("SystemErr"))) { //SystemOut/Err=700
                            if (traceSource != null) {
                                traceSource.publish(routedTrace, id);
                            }
                        }
                    }
                }
            }
        } finally {
            counterForTraceSource.decrementCount();
        }

        try {
            if (!(counterForTraceWriter.incrementCount() > 1)) {
                // write to trace.log
                if (detailLog != systemOut) {
                    String traceDetail = formatter.traceLogFormat(logRecord, id, formattedMsg, formattedVerboseMsg);
                    detailLog.writeRecord(traceDetail);
                }
            }
        } finally {
            counterForTraceWriter.decrementCount();
        }
    }

    /**
     * Inject the SPI MessageRouter.
     *
     * This is injected by TrConfigurator, which is also SPI. So it's possible for
     * third-party code to implement and inject their own MessageRouter.
     */
    @Override
    public void setMessageRouter(MessageRouter msgRouter) {

        externalMessageRouter.set(msgRouter);

        if (msgRouter instanceof WsMessageRouter) {
            setWsMessageRouter((WsMessageRouter) msgRouter);
        }
    }

    /**
     * Un-inject.
     */
    @Override
    public void unsetMessageRouter(MessageRouter msgRouter) {
        externalMessageRouter.compareAndSet(msgRouter, null);

        if (msgRouter instanceof WsMessageRouter) {
            unsetWsMessageRouter((WsMessageRouter) msgRouter);
        }
    }

    /**
     * Inject the internal WsMessageRouter.
     */
    protected void setWsMessageRouter(WsMessageRouter msgRouter) {
        internalMessageRouter.set(msgRouter);

        // Pass the earlierMessages queue to the router.
        // Now that the internalMessageRouter is non-null, this class will
        // NOT add any more messages to the earlierMessages queue.
        // The MessageRouter basically owns the earlierMessages queue
        // from now on.

        if (earlierMessages != null) {
            synchronized (this) {
                if (earlierMessages != null) {
                    msgRouter.setEarlierMessages(earlierMessages);
                }
            }
        } else {
            msgRouter.setEarlierMessages(null);
        }
    }

    /**
     * Un-inject.
     */
    public void unsetWsMessageRouter(WsMessageRouter msgRouter) {
        internalMessageRouter.compareAndSet(msgRouter, null);
    }

    /**
     * Inject the internal WsMessageRouter.
     */
    @Override
    public void setTraceRouter(WsTraceRouter traceRouter) {

        internalTraceRouter.set(traceRouter);

        // Pass the earlierMessages queue to the router.
        // Now that the internalMessageRouter is non-null, this class will
        // NOT add any more messages to the earlierMessages queue.
        // The MessageRouter basically owns the earlierMessages queue
        // from now on.
        if (earlierTraces != null) {
            synchronized (this) {
                if (earlierTraces != null) {
                    traceRouter.setEarlierTraces(earlierTraces);
                }
            }
        } else {
            traceRouter.setEarlierTraces(null);
        }
    }

    /**
     * Un-inject.
     */
    @Override
    public void unsetTraceRouter(WsTraceRouter traceRouter) {
        internalTraceRouter.compareAndSet(traceRouter, null);
    }

    /**
     * Initialize the log holders for messages and trace. If the TrService is configured
     * at bootstrap time to use JSR-47 logging, the traceLog will not be created here,
     * as the LogRecord should be routed to logger rather than being formatted for
     * the trace file.
     *
     * @param config a {@link LogProviderConfigImpl} containing TrService configuration
     *                   from bootstrap properties
     */
    protected void initializeWriters(LogProviderConfigImpl config) {
        // createFileLog may or may not return the original log holder..
        messagesLog = FileLogHolder.createFileLogHolder(messagesLog,
                                                        newFileLogHeader(false, config),
                                                        config.getLogDirectory(),
                                                        config.getMessageFileName(),
                                                        config.getMaxFiles(),
                                                        config.getMaxFileBytes(),
                                                        config.getNewLogsOnStart());

        // Always create a traceLog when using Tr -- this file won't actually be
        // created until something is logged to it...
        TraceWriter oldWriter = traceLog;
        String fileName = config.getTraceFileName();
        if (fileName.equals("stdout")) {
            traceLog = systemOut;
            LoggingFileUtils.tryToClose(oldWriter);
        } else {
            traceLog = FileLogHolder.createFileLogHolder(oldWriter == systemOut ? null : oldWriter,
                                                         newFileLogHeader(true, config),
                                                         config.getLogDirectory(),
                                                         config.getTraceFileName(),
                                                         config.getMaxFiles(),
                                                         config.getMaxFileBytes(),
                                                         config.getNewLogsOnStart());
            if (!TraceComponent.isAnyTracingEnabled()) {
                ((FileLogHolder) traceLog).releaseFile();
            }
        }

    }

    private FileLogHeader newFileLogHeader(boolean trace, LogProviderConfigImpl config) {
        boolean isJSON = false;
        String messageFormat = config.getMessageFormat();
        if (LoggingConstants.JSON_FORMAT.equals(messageFormat)) {
            isJSON = true;
            String jsonHeader = constructJSONHeader(messageFormat, config);
            return new FileLogHeader(jsonHeader, trace, javaLangInstrument, isJSON);
        }
        return new FileLogHeader(logHeader, trace, javaLangInstrument, isJSON);
    }

    private String constructJSONHeader(String messageFormat, LogProviderConfigImpl config) {
        //retrieve information for header
        String serverHostName = getServerHostName();
        String wlpUserDir = config.getWlpUsrDir();
        String serverName = getServerName(config);
        String datetime = getDatetime();
        String sequenceNumber = getSequenceNumber();
        //construct json header
        JSONObjectBuilder jsonBuilder = new JSONObject.JSONObjectBuilder();

        //@formatter:off
        jsonBuilder.addField(LogTraceData.getTypeKeyJSON(true), "liberty_message", false, false)
        .addField(LogTraceData.getHostKeyJSON(true), serverHostName, false, true)
        .addField(LogTraceData.getUserDirKeyJSON(true), wlpUserDir, false, true)
        .addField(LogTraceData.getServerNameKeyJSON(true), serverName, false, true)
        .addField(LogTraceData.getMessageKeyJSON(true), logHeader, false, true)
        .addField(LogTraceData.getDatetimeKeyJSON(true), datetime, false, true)
        .addField(LogTraceData.getSequenceKeyJSON(true), sequenceNumber, false, true);
        //@formatter:on

        return jsonBuilder.build().toString().concat("\n");
    }

    private String getSequenceNumber() {
        SequenceNumber sequenceNumber = new SequenceNumber();
        long rawSequenceNumber = sequenceNumber.getRawSequenceNumber();
        String sequenceId = null;
        if (sequenceId == null || sequenceId.isEmpty()) {
            sequenceId = SequenceNumber.formatSequenceNumber(System.currentTimeMillis(), rawSequenceNumber);
        }
        return sequenceId;
    }

    private String getDatetime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        String datetime = dateFormat.format(System.currentTimeMillis());
        return datetime;
    }

    private String getServerName(LogProviderConfigImpl config) {
        //Resolve server name to be the DOCKER Container name or the wlp server name.
        String containerName = System.getenv("CONTAINER_NAME");
        if (containerName == null || containerName.equals("") || containerName.length() == 0) {
            this.serverName = config.getServerName();
        } else {
            this.serverName = containerName;
        }
        return serverName;
    }

    private String getServerHostName() {
        String serverHostName = null;
        //Resolve server name to be the DOCKER HOST name or the cannonical host name.
        String containerHost = System.getenv("CONTAINER_HOST");
        if (containerHost == null || containerHost.equals("") || containerHost.length() == 0) {
            try {
                serverHostName = AccessController.doPrivileged(new PrivilegedExceptionAction<String>() {
                    @Override
                    public String run() throws UnknownHostException {
                        return InetAddress.getLocalHost().getCanonicalHostName();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                serverHostName = "";
            }
        } else {
            serverHostName = containerHost;
        }
        return serverHostName;
    }

    public final static class SystemLogHolder extends Level implements TraceWriter {
        private static final long serialVersionUID = 1L;
        transient final PrintStream originalStream;
        transient final TraceComponent tc;

        protected SystemLogHolder(String name, PrintStream origin) {
            super(name, WsLevel.CONFIG.intValue());
            originalStream = origin;
            // Register a trace component with no group
            tc = Tr.register(name, SystemLogHolder.class, (String) null);
        }

        public PrintStream getOriginalStream() {
            return originalStream;
        }

        /** Only used when traceFileName == stdout */
        @Override
        public void writeRecord(String record) {
            originalStream.println(record);
        }

        /** {@inheritDoc} */
        @Override
        public void close() throws IOException {
        }

        /**
         * Only allow "off" as a valid value for toggling system.out
         * or system.err to avoid doing something unexpected.
         *
         * @return true if this stream is "enabled"
         */
        public boolean isEnabled() {
            return tc.getLoggerLevel() != Level.OFF;
        }
    }

    public final static class TeePrintStream extends PrintStream {
        protected final TrOutputStream trStream;

        public TeePrintStream(TrOutputStream trStream, boolean autoFlush) {
            super(trStream, autoFlush);
            this.trStream = trStream;
        }

        @Override
        public synchronized void print(boolean b) {
            TrOutputStream.isPrinting.set(true);
            try {
                super.print(b);
            } finally {
                TrOutputStream.isPrinting.set(false);
                super.flush();
            }
        }

        @Override
        public synchronized void print(char c) {
            TrOutputStream.isPrinting.set(true);
            try {
                super.print(c);
            } finally {
                TrOutputStream.isPrinting.set(false);
                super.flush();
            }
        }

        @Override
        public synchronized void print(int i) {
            TrOutputStream.isPrinting.set(true);
            try {
                super.print(i);
            } finally {
                TrOutputStream.isPrinting.set(false);
                super.flush();
            }
        }

        @Override
        public synchronized void print(long l) {
            TrOutputStream.isPrinting.set(true);
            try {
                super.print(l);
            } finally {
                TrOutputStream.isPrinting.set(false);
                super.flush();
            }
        }

        @Override
        public synchronized void print(float f) {
            TrOutputStream.isPrinting.set(true);
            try {
                super.print(f);
            } finally {
                TrOutputStream.isPrinting.set(false);
                super.flush();
            }
        }

        @Override
        public synchronized void print(double d) {
            TrOutputStream.isPrinting.set(true);
            try {
                super.print(d);
            } finally {
                TrOutputStream.isPrinting.set(false);
                super.flush();
            }
        }

        @Override
        public synchronized void print(char c[]) {
            TrOutputStream.isPrinting.set(true);
            try {
                super.print(c);
            } finally {
                TrOutputStream.isPrinting.set(false);
                super.flush();
            }
        }

        @Override
        public synchronized void print(String s) {
            TrOutputStream.isPrinting.set(true);
            try {
                super.print(s);
            } finally {
                TrOutputStream.isPrinting.set(false);
                super.flush();
            }
        }

        @Override
        public synchronized void print(Object obj) {
            TrOutputStream.isPrinting.set(true);
            try {
                super.print(obj);
            } finally {
                TrOutputStream.isPrinting.set(false);
                super.flush();
            }
        }

        @Override
        public synchronized void println() {
            TrOutputStream.isPrinting.set(true);
            try {
                super.println();
            } finally {
                TrOutputStream.isPrinting.set(false);
                super.flush();
            }
        }

        @Override
        public synchronized void println(boolean b) {
            TrOutputStream.isPrinting.set(true);
            try {
                super.print(b);
            } finally {
                TrOutputStream.isPrinting.set(false);
                super.flush();
            }
        }

        @Override
        public synchronized void println(char c) {
            TrOutputStream.isPrinting.set(true);
            try {
                super.print(c);
            } finally {
                TrOutputStream.isPrinting.set(false);
                super.flush();
            }
        }

        @Override
        public synchronized void println(int i) {
            TrOutputStream.isPrinting.set(true);
            try {
                super.print(i);
            } finally {
                TrOutputStream.isPrinting.set(false);
                super.flush();
            }
        }

        @Override
        public synchronized void println(long l) {
            TrOutputStream.isPrinting.set(true);
            try {
                super.print(l);
            } finally {
                TrOutputStream.isPrinting.set(false);
                super.flush();
            }
        }

        @Override
        public synchronized void println(float f) {
            TrOutputStream.isPrinting.set(true);
            try {
                super.print(f);
            } finally {
                TrOutputStream.isPrinting.set(false);
                super.flush();
            }
        }

        @Override
        public synchronized void println(double d) {
            TrOutputStream.isPrinting.set(true);
            try {
                super.print(d);
            } finally {
                TrOutputStream.isPrinting.set(false);
                super.flush();
            }
        }

        @Override
        public synchronized void println(char c[]) {
            TrOutputStream.isPrinting.set(true);
            try {
                super.print(c);
            } finally {
                TrOutputStream.isPrinting.set(false);
                super.flush();
            }
        }

        @Override
        public synchronized void println(String s) {
            TrOutputStream.isPrinting.set(true);
            try {
                super.print(s);
            } finally {
                TrOutputStream.isPrinting.set(false);
                super.flush();
            }
        }

        @Override
        public synchronized void println(Object obj) {
            TrOutputStream.isPrinting.set(true);
            try {
                super.print(obj);
            } finally {
                TrOutputStream.isPrinting.set(false);
                super.flush();
            }
        }

    }

    /**
     * Use a byte array output stream to capture data written to system out or
     * system err. When flush is called on the stream, a method
     * will be invoked on the BaseTraceService to trace the string with
     * the appropriate trace component.
     */
    public static class TrOutputStream extends ByteArrayOutputStream {
        final SystemLogHolder holder;
        final BaseTraceService service;
        public static ThreadLocal<Boolean> isPrinting = new ThreadLocal<Boolean>() {
            @Override
            protected Boolean initialValue() {
                return Boolean.FALSE;
            }
        };

        public TrOutputStream(SystemLogHolder slh, BaseTraceService service) {
            this.holder = slh;
            this.service = service;
        }

        @Override
        public synchronized void flush() throws IOException {

            /*
             * sPrinting is a ThreadLocal that is set to disable flushing while printing.
             * This helps us ignore flush requests that the JDK automatically creates in the middle of printing large (>8k) strings.
             * We want the whole String to be flushed in one shot for benefit of downstream event consumers.
             */
            if (isPrinting.get())
                return;

            super.flush();

            if (!holder.isEnabled()) {
                super.reset();
                return;
            }

            String entry = this.toString();
            super.reset();

            // No-op on empty messages
            if (entry.isEmpty() || LoggingConstants.nl.equals(entry))
                return;

            if (entry.endsWith(LoggingConstants.nl)) {
                entry = entry.substring(0, entry.length() - LoggingConstants.nlen);
            }

            LogRecord logRecord = new LogRecord(holder, entry);
            logRecord.setLoggerName(holder.getName());

            service.echo(holder, logRecord);
        }
    }

    /**
     * Capture the system stream. The original streams are cached/remembered
     * when the special trace components are created.
     */
    protected void captureSystemStreams() {
        teeOut = new TeePrintStream(new TrOutputStream(systemOut, this), true);
        System.setOut(teeOut);

        teeErr = new TeePrintStream(new TrOutputStream(systemErr, this), true);
        System.setErr(teeErr);
    }

    /**
     * Restore the system streams. The original streams are cached/remembered
     * when the special trace components are created.
     */
    protected void restoreSystemStreams() {
        if (System.out == teeOut)
            System.setOut(systemOut.getOriginalStream());
        if (System.err == teeErr)
            System.setErr(systemErr.getOriginalStream());
    }

    /**
     * Write the text to the associated original stream.
     * This is preserved as a subroutine for extension by other delegates (test, JSR47 logging)
     *
     * @param tc        StreamTraceComponent associated with original stream
     * @param txt       pre-formatted or raw message
     * @param rawStream if true, this is from direct invocation of System.out or System.err
     */
    protected synchronized void writeStreamOutput(SystemLogHolder holder, String txt, boolean rawStream) {
        if (holder == systemErr && rawStream) {
            txt = "[err] " + txt;
        }
        holder.originalStream.println(txt);
    }

    /**
     * Trim stack traces. This isn't as sophisticated as what TruncatableThrowable
     * does, since pass through all code except code which is clearly IBM-internal.
     * This means we pass through Java API and third-party calls - this means we
     * include more frames than we'd like to, but the alternative is to try and buffer
     * the whole stack so we can make passes in two directions (as we do in TruncatableThrowable).
     * However, the buffering seems terribly risky. The other alternative is to stop as soon
     * as we hit the first IBM code, but this means we lose user code in the
     * container-user-container case.
     *
     * Thread-safety is provided by a thread local variable which is used to store state.
     *
     * This filtering also won't trim stack traces which have been converted
     * into a string and then output (rather than coming out line by line).
     *
     * Once we hit something on stderr that's not a stack frame, we reset all our state.
     *
     * @param txt a line of stack trace
     * @return null if the stack trace should be suppressed, or an indicator we're suppressing,
     *         or maybe the original stack trace
     */
    public static String filterStackTraces(String txt) {
        // Check for stack traces, which we may want to trim
        StackTraceFlags stackTraceFlags = traceFlags.get();
        // We have a little thread-local state machine here with four states controlled by two
        // booleans. Our triggers are { "unknown/user code", "just seen IBM code", "second line of IBM code", ">second line of IBM code"}
        // "unknown/user code" -> stackTraceFlags.isSuppressingTraces -> false, stackTraceFlags.needsToOutputInternalPackageMarker -> false
        // "just seen IBM code" -> stackTraceFlags.needsToOutputInternalPackageMarker->true
        // "second line of IBM code" -> stackTraceFlags.needsToOutputInternalPackageMarker->true
        // ">second line of IBM code" -> stackTraceFlags.isSuppressingTraces->true
        // The final two states are optional

        if (txt.startsWith("\tat ")) {
            // This is a stack trace, do a more detailed analysis
            PackageProcessor packageProcessor = PackageProcessor.getPackageProcessor();
            String packageName = PackageProcessor.extractPackageFromStackTraceLine(txt);
            // If we don't have a package processor, don't suppress anything
            if (packageProcessor != null && packageProcessor.isIBMPackage(packageName)) {
                // First internal package, we let through
                // Second one, we suppress but say we did
                // If we're still suppressing, and this is a stack trace, this is easy - we suppress
                if (stackTraceFlags.isSuppressingTraces) {
                    txt = null;
                } else if (stackTraceFlags.needsToOutputInternalPackageMarker) {
                    // Replace the stack trace with something saying we got rid of it
                    txt = "\tat " + TruncatableThrowable.INTERNAL_CLASSES_STRING;
                    // No need to output another marker, we've just output it
                    stackTraceFlags.needsToOutputInternalPackageMarker = false;
                    // Suppress any subsequent IBM frames
                    stackTraceFlags.isSuppressingTraces = true;
                } else {
                    // Let the text through, but make a note not to let anything but an [internal classes] through
                    stackTraceFlags.needsToOutputInternalPackageMarker = true;
                }
            } else {
                // This is user code, third party API, or Java API, so let it through
                // Reset the flags to ensure it gets let through
                stackTraceFlags.isSuppressingTraces = false;
                stackTraceFlags.needsToOutputInternalPackageMarker = false;
            }

        } else {
            // We're no longer processing a stack, so reset all our state
            stackTraceFlags.isSuppressingTraces = false;
            stackTraceFlags.needsToOutputInternalPackageMarker = false;
        }
        return txt;
    }

    /*
     * Helper method to clean up the original source list by removing messages and
     * trace from it. Otherwise, our json handlers will subscribe these and cause
     * collectorManager to create 'new' conduits/Buffermanagers.
     */
    private List<String> filterSourcelist(List<String> sourceList) {
        List<String> filteredList = new ArrayList<String>(sourceList);
        filteredList.remove(CollectorConstants.TRACE_CONFIG_VAL);
        filteredList.remove(CollectorConstants.MESSAGES_CONFIG_VAL);
        return filteredList;
    }

    /*
     * Based on config (sourceList), need to connect the synchronized handler to configured source/conduit..
     * Or disconnect it.
     */
    private void updateConduitSyncHandlerConnection(List<String> sourceList, SynchronousHandler handler) {
        if (sourceList.contains("message")) {
            logConduit.addSyncHandler(handler);
        } else {
            logConduit.removeSyncHandler(handler);
        }

        if (sourceList.contains("trace")) {
            traceConduit.addSyncHandler(handler);
        } else {
            traceConduit.removeSyncHandler(handler);
        }
    }

    private class EarlyMessageTraceCleaner extends TimerTask {
        @Override
        public void run() {
            synchronized (BaseTraceService.this) {
                earlierMessages = null;
                earlierTraces = null;

                /*
                 * With earlierMessages and earlierTraces set to null now,
                 * calling setWsMessageRouter and setTraceRouter will
                 * subsequently null out the earlyMessageQueue and earlyTraceQueue
                 * in their respective routers
                 */
                if (internalMessageRouter.get() != null)
                    BaseTraceService.this.setWsMessageRouter(internalMessageRouter.get());
                if (internalTraceRouter.get() != null)
                    BaseTraceService.this.setTraceRouter(internalTraceRouter.get());
            }
        }
    }
}
