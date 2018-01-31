/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.collector.manager.buffer.BufferManagerImpl;
import com.ibm.ws.logging.RoutedMessage;
import com.ibm.ws.logging.WsMessageRouter;
import com.ibm.ws.logging.WsTraceRouter;
import com.ibm.ws.logging.collector.CollectorConstants;
import com.ibm.ws.logging.source.LogSource;
import com.ibm.ws.logging.source.TraceSource;
import com.ibm.ws.logging.utils.CollectorManagerPipelineUtils;
import com.ibm.wsspi.collector.manager.SynchronousHandler;
import com.ibm.wsspi.logging.MessageRouter;
import com.ibm.wsspi.logprovider.LogProviderConfig;

/**
 *
 */
public class JsonTraceService extends BaseTraceService {

    private volatile LogSource logSource = null;
    private volatile TraceSource traceSource = null;
    private volatile MessageLogHandler messageLogHandler = null;
    private volatile ConsoleLogHandler consoleLogHandler = null;
    private volatile BufferManagerImpl logConduit;
    private volatile BufferManagerImpl traceConduit;
    private volatile CollectorManagerPipelineUtils collectorMgrPipelineUtils = null;

    private volatile String serverName = null;
    private volatile String wlpUserDir = null;

    @Override
    public synchronized void update(LogProviderConfig config) {
        super.update(config);

        //Need a LogProviderConfigImpl to get additional information specifically for JsonTraceService
        LogProviderConfigImpl jsonTRConfig = (LogProviderConfigImpl) config;

        /*
         * Need to know the values of wlpServerName and wlpUserDir
         * They are passed into the handlers for use as part of the jsonified output
         */
        serverName = jsonTRConfig.getServerName();
        wlpUserDir = jsonTRConfig.getWlpUsrDir();

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

        /*
         * Retrieve the format setting for message.log and console
         */
        String messageFormat = jsonTRConfig.getMessageFormat();
        String consoleFormat = jsonTRConfig.getConsoleFormat();

        //Retrieve the source lists of both message and console
        List<String> messageSourceList = new ArrayList<String>(jsonTRConfig.getMessageSource());
        List<String> consoleSourceList = new ArrayList<String>(jsonTRConfig.getConsoleSource());

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
        if (messageLogHandler == null) {
            messageLogHandler = new MessageLogHandler(serverName, wlpUserDir, filterdMessageSourceList);
            collectorMgrPipelineUtils.setMessageHandler(messageLogHandler);
            messageLogHandler.setWriter(messagesLog);

        }
        if (consoleLogHandler == null) {
            consoleLogHandler = new ConsoleLogHandler(serverName, wlpUserDir, filterdConsoleSourceList);
            collectorMgrPipelineUtils.setConsoleHandler(consoleLogHandler);
            consoleLogHandler.setWriter(systemOut);
            consoleLogHandler.setSysErrHolder(systemErr);
        }
        /*
         * If messageFormat has been configured to 'basic' - ensure that we are not connecting conduits/bufferManagers to the handler
         * otherwise we would have the undesired effect of writing both 'basic' and 'json' formatted message events
         */
        if (messageFormat.toLowerCase().equals(LoggingConstants.DEFAULT_MESSAGE_FORMAT)) {
            messageLogHandler.setFormat(LoggingConstants.DEFAULT_MESSAGE_FORMAT);
            if (messageLogHandler != null) {
                messageLogHandler.setWriter(messagesLog);
                ArrayList<String> filteredList = new ArrayList<String>();
                filteredList.add("message");
                updateConduitSyncHandlerConnection(filteredList, messageLogHandler);
            }
        }

        /*
         * If consoleFormat has been configured to 'basic' - ensure that we are not connecting conduits/bufferManagers to the handler
         * otherwise we would have the undesired effect of writing both 'basic' and 'json' formatted message events
         */
        if (consoleFormat.toLowerCase().equals(LoggingConstants.DEFAULT_CONSOLE_FORMAT)) {
            if (consoleLogHandler != null) {
                consoleLogHandler.setFormat(LoggingConstants.DEFAULT_CONSOLE_FORMAT);
                ArrayList<String> filteredList = new ArrayList<String>();
                filteredList.add("message");
                if (traceLog == systemOut) {
                    filteredList.add("trace");
                    consoleLogHandler.setIsTraceStdout(true);
                } else {
                    consoleLogHandler.setIsTraceStdout(false);
                }
                updateConduitSyncHandlerConnection(filteredList, consoleLogHandler);
            }
            consoleLogHandler.setCopySystemStreams(copySystemStreams);
            consoleLogHandler.setConsoleLogLevel(consoleLogLevel.intValue());
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
                messageLogHandler.setWriter(messagesLog);
                //for any 'updates' to the FileLogHolder
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
                //Connect the conduits to the handler as necessary
                //if json && messages, trace sourcelist
                consoleLogHandler.modified(filterdConsoleSourceList);
                updateConduitSyncHandlerConnection(consoleSourceList, consoleLogHandler);
            }
        }
        messageLogHandler.setFormatter(formatter);
        consoleLogHandler.setFormatter(formatter);
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

    @Override
    public void echo(SystemLogHolder holder, LogRecord logRecord) {
        TraceWriter detailLog = traceLog;
        // Tee to messages.log (always)

        RoutedMessage routedMessage = new RoutedMessageImpl(logRecord.getMessage(), logRecord.getMessage(), null, logRecord);

        /*
         * Messages sent through LogSource will be received by MessageLogHandler and ConsoleLogHandler
         * if messageFormat and consoleFormat have been set to "json" and "message" is a listed source.
         * However, LogstashCollector and BluemixLogCollector will receive all messages
         */
        invokeMessageRouters(routedMessage);
        if (logSource != null) {
            logSource.publish(routedMessage);
        }
        //send events to handlers
        if (TraceComponent.isAnyTracingEnabled()) {
            publishTraceLogRecord(detailLog, logRecord, NULL_ID, NULL_FORMATTED_MSG, NULL_FORMATTED_MSG);
        }

    }

    @Override
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
    @Override
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
                            } else {
                                earlierTraces.add(routedTrace);
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

    @Override
    public void publishLogRecord(LogRecord logRecord) {
        String formattedMsg = null;
        String formattedVerboseMsg = null;

        Level level = logRecord.getLevel();
        int levelValue = level.intValue();
        TraceWriter detailLog = traceLog;
        //check if tracefilename is stdout

        if (levelValue >= Level.INFO.intValue()) {
            //configuration

            formattedMsg = formatter.formatMessage(logRecord);
            formattedVerboseMsg = formatter.formatVerboseMessage(logRecord, formattedMsg);

            RoutedMessage routedMessage = new RoutedMessageImpl(formattedMsg, formattedVerboseMsg, null, logRecord);

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
                logSource.publish(routedMessage);
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

    @Override
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

        // write to trace.log
        if (detailLog != systemOut) {
            String traceDetail = formatter.traceLogFormat(logRecord, id, formattedMsg, formattedVerboseMsg);
            detailLog.writeRecord(traceDetail);
        }

    }
}
