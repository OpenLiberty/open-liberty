/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
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
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.ibm.websphere.logging.WsLevel;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.TruncatableThrowable;
import com.ibm.ws.collector.manager.buffer.SimpleRotatingSoftQueue;
import com.ibm.ws.kernel.boot.logging.LoggerHandlerManager;
import com.ibm.ws.logging.RoutedMessage;
import com.ibm.ws.logging.WsLogHandler;
import com.ibm.ws.logging.WsMessageRouter;
import com.ibm.ws.logging.WsTraceRouter;
import com.ibm.ws.logging.internal.PackageProcessor;
import com.ibm.ws.logging.internal.TraceSpecification;
import com.ibm.ws.logging.internal.WsLogRecord;
import com.ibm.ws.logging.utils.FileLogHolder;
import com.ibm.ws.logging.utils.RecursionCounter;
import com.ibm.wsspi.collector.manager.BufferManager;
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

    protected static RecursionCounter counter = new RecursionCounter();

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

    /** Configured message Ids to be suppressed in console/message.log */
    private volatile Collection<String> hideMessageids;

    /** Early msgs issued before MessageRouter is started. */
    protected final Queue<RoutedMessage> earlierMessages = new SimpleRotatingSoftQueue<RoutedMessage>(new RoutedMessage[100]);
    protected final Queue<RoutedMessage> earlierTraces = new SimpleRotatingSoftQueue<RoutedMessage>(new RoutedMessage[200]);

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
    }

    /**
     * {@inheritDoc} <p>
     * This is called at initialization time: The bootstrap/launcher uses reflection to
     * create a LogProvider; in our case, this is the {@link LogProviderImpl}. Our
     * LogProvider creates a {@link LogProviderConfigImpl}, which parses the set
     * of system properties we expect (for FFDC and logging).
     *
     * @param config a {@link LogProviderConfigImpl} containing TrService configuration
     *            from bootstrap properties
     */
    @Override
    public void init(LogProviderConfig config) {
        update(config);

        registerLoggerHandlerSingleton();
        // Capture System.out/.err after registerLoggerHandler has initialized
        // LogManager, which might print errors due to misconfiguration.
        captureSystemStreams();
        new java.util.Timer().schedule(
                                       new java.util.TimerTask() {
                                           @Override
                                           public void run() {
                                               BufferManager.removeEMQTrigger();
                                           }
                                       },
                                       BufferManager.EMQ_TIMER);
    }

    protected void registerLoggerHandlerSingleton() {
        // Add our handler to catch and format records produced with logger
        LoggerHandlerManager.setSingleton(new Handler() {
            @Override
            public void publish(LogRecord logRecord) {
                BaseTraceService.this.publishLogRecord(logRecord);
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
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
     *            the OSGi managed service.
     */
    @Override
    public synchronized void update(LogProviderConfig config) {
        LogProviderConfigImpl trConfig = (LogProviderConfigImpl) config;
        logHeader = trConfig.getLogHeader();
        javaLangInstrument = trConfig.hasJavaLangInstrument();
        consoleLogLevel = trConfig.getConsoleLogLevel();
        copySystemStreams = trConfig.copySystemStreams();
        hideMessageids = trConfig.getMessagesToHide();
        //add hideMessageIds to log header. This is printed when its configured in bootstrap.properties
        if (hideMessageids.size() > 0) {
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

        initializeWriters(trConfig);
        if (hideMessageids.size() > 0) {
            Tr.info(TraceSpecification.getTc(), "MESSAGES_CONFIGURED_HIDDEN_2", new Object[] { hideMessageids });
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
        String message = formatter.messageLogFormat(logRecord, logRecord.getMessage());
        messagesLog.writeRecord(message);

        invokeMessageRouters(new RoutedMessageImpl(logRecord.getMessage(), logRecord.getMessage(), message, logRecord));

        if (detailLog == systemOut) {
            // preserve System.out vs. System.err
            publishTraceLogRecord(holder, logRecord, NULL_ID, NULL_FORMATTED_MSG, NULL_FORMATTED_MSG);
        } else {
            if (copySystemStreams) {
                // Tee to console.log if we are copying System.out and System.err to system streams.
                writeFilteredStreamOutput(holder, logRecord);
            }

            if (TraceComponent.isAnyTracingEnabled()) {
                publishTraceLogRecord(detailLog, logRecord, NULL_ID, NULL_FORMATTED_MSG, NULL_FORMATTED_MSG);
            }
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

        if (externalMsgRouter != null) {
            retMe &= externalMsgRouter.route(routedMessage.getFormattedMsg(), routedMessage.getLogRecord());
        }
        if (internalMsgRouter != null) {
            retMe &= internalMsgRouter.route(routedMessage);
        } else {
            earlierMessages.add(routedMessage);
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
            if (!(counter.incrementCount() > 2)) {
                if (logRecord != null) {
                    Level level = logRecord.getLevel();
                    int levelValue = level.intValue();
                    if (levelValue < Level.INFO.intValue()) {
                        String levelName = level.getName();
                        if (!(levelName.equals("SystemOut") || levelName.equals("SystemErr"))) { //SystemOut/Err=700
                            WsTraceRouter internalTrRouter = internalTraceRouter.get();
                            if (internalTrRouter != null) {
                                retMe &= internalTrRouter.route(routedTrace);
                            } else {
                                earlierTraces.add(routedTrace);
                            }
                        }
                    }
                }
            }
        } finally {
            counter.decrementCount();
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
            String messageLogFormat = formatter.messageLogFormat(logRecord, formattedVerboseMsg);

            // Look for external log handlers. They may suppress "normal" log
            // processing, which would prevent it from showing up in other logs.
            // This has to be checked in this method: direct invocation of system.out
            // and system.err are not subject to message routing.
            boolean logNormally = invokeMessageRouters(new RoutedMessageImpl(formattedMsg, formattedVerboseMsg, messageLogFormat, logRecord));
            if (!logNormally)
                return;

            //If any messages configured to be hidden then those will not be written to console.log/message.log and redirected to trace.log.
            if (isMessageHidden(formattedMsg)) {
                publishTraceLogRecord(detailLog, logRecord, NULL_ID, formattedMsg, formattedVerboseMsg);
                return;
            }

            // messages.log  //send directly.
            messagesLog.writeRecord(messageLogFormat);

            // console.log
            if (detailLog == systemOut) {
                // Send all messages directly to the correct system streams, and then be DONE
                if (levelValue == WsLevel.ERROR.intValue() || levelValue == WsLevel.FATAL.intValue()) {
                    // WsLevel.ERROR and Level.SEVERE have the same int value, and are routed to System.err
                    publishTraceLogRecord(systemErr, logRecord, NULL_ID, formattedMsg, formattedVerboseMsg);
                } else {
                    // messages othwerwise above the filter are routed to System.out
                    publishTraceLogRecord(systemOut, logRecord, NULL_ID, formattedMsg, formattedVerboseMsg);
                }
                return; // DONE!!
            } else if (levelValue >= consoleLogLevel.intValue()) {
                // Only route messages permitted by consoleLogLevel
                String consoleMsg = formatter.consoleLogFormat(logRecord, formattedMsg);
                if (levelValue == WsLevel.ERROR.intValue() || levelValue == WsLevel.FATAL.intValue()) {
                    // WsLevel.ERROR and Level.SEVERE have the same int value, and are routed to System.err
                    writeStreamOutput(systemErr, consoleMsg, false);
                } else {
                    // messages othwerwise above the filter are routed to system out
                    writeStreamOutput(systemOut, consoleMsg, false);
                }
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
     * Publish a trace log record.
     *
     * @param detailLog the trace writer
     * @param logRecord
     * @param id the trace object id
     * @param formattedMsg the result of {@link BaseTraceFormatter#formatMessage}
     * @param formattedVerboseMsg the result of {@link BaseTraceFormatter#formatVerboseMessage}
     */
    protected void publishTraceLogRecord(TraceWriter detailLog, LogRecord logRecord, Object id, String formattedMsg, String formattedVerboseMsg) {
        if (formattedVerboseMsg == null) {
            formattedVerboseMsg = formatter.formatVerboseMessage(logRecord, formattedMsg, false);
        }
        String traceDetail = formatter.traceLogFormat(logRecord, id, formattedMsg, formattedVerboseMsg);
        invokeTraceRouters(new RoutedMessageImpl(formattedMsg, formattedVerboseMsg, traceDetail, logRecord));

        if (detailLog == systemOut || detailLog == systemErr) {
            writeStreamOutput((SystemLogHolder) detailLog, traceDetail, false);
        } else {
            detailLog.writeRecord(traceDetail);
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
        msgRouter.setEarlierMessages(earlierMessages);
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
        traceRouter.setEarlierTraces(earlierTraces);
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
     *            from bootstrap properties
     */
    protected void initializeWriters(LogProviderConfigImpl config) {
        // createFileLog may or may not return the original log holder..
        messagesLog = FileLogHolder.createFileLogHolder(messagesLog,
                                                        newFileLogHeader(false),
                                                        config.getLogDirectory(),
                                                        config.getMessageFileName(),
                                                        config.getMaxFiles(),
                                                        config.getMaxFileBytes());

        // Always create a traceLog when using Tr -- this file won't actually be
        // created until something is logged to it...
        TraceWriter oldWriter = traceLog;
        String fileName = config.getTraceFileName();
        if (fileName.equals("stdout")) {
            traceLog = systemOut;
            LoggingFileUtils.tryToClose(oldWriter);
        } else {
            traceLog = FileLogHolder.createFileLogHolder(oldWriter == systemOut ? null : oldWriter,
                                                         newFileLogHeader(true),
                                                         config.getLogDirectory(),
                                                         config.getTraceFileName(),
                                                         config.getMaxFiles(),
                                                         config.getMaxFileBytes());
            if (!TraceComponent.isAnyTracingEnabled()) {
                ((FileLogHolder) traceLog).releaseFile();
            }
        }

    }

    private FileLogHeader newFileLogHeader(boolean trace) {
        return new FileLogHeader(logHeader, trace, javaLangInstrument);
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
        public void close() throws IOException {}

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

        public TrOutputStream(SystemLogHolder slh, BaseTraceService service) {
            this.holder = slh;
            this.service = service;
        }

        @Override
        public synchronized void flush() throws IOException {
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
     * @param tc StreamTraceComponent associated with original stream
     * @param txt pre-formatted or raw message
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
    private String filterStackTraces(String txt) {
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

}
