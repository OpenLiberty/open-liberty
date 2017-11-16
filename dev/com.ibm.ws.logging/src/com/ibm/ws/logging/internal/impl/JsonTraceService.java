/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import com.ibm.websphere.logging.WsLevel;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.collector.manager.buffer.BufferManagerImpl;
import com.ibm.ws.logging.RoutedMessage;
import com.ibm.ws.logging.WsMessageRouter;
import com.ibm.ws.logging.WsTraceRouter;
import com.ibm.ws.logging.collector.CollectorConstants;
import com.ibm.ws.logging.source.LogSource;
import com.ibm.ws.logging.source.TraceSource;
import com.ibm.ws.logging.utils.CollectorManagerPipelineUtils;
import com.ibm.wsspi.collector.manager.SyncrhonousHandler;
import com.ibm.wsspi.logging.MessageRouter;
import com.ibm.wsspi.logprovider.LogProviderConfig;

/**
 *
 */
public class JsonTraceService extends BaseTraceService {

    private final boolean isJSON = true;
    private volatile LogSource logSource = null;
    private volatile TraceSource traceSource = null;
    private volatile MessageLogHandler messageLogHandler = null;
    //private volatile ConsoleLogHandler consoleLogHandler = null;
    private volatile BufferManagerImpl logConduit;
    private volatile BufferManagerImpl traceConduit;
    private volatile CollectorManagerPipelineUtils collectorMgrPipelineUtils = null;

    // for now always have it configured?
    private static volatile boolean isMessageJsonConfigured = false;
    private static volatile boolean isConsoleJsonConfigured = false;
    private static volatile Object sync = new Object();

    private volatile String serverName = null;
    private volatile String wlpUserDir = null;

    @Override
    public synchronized void update(LogProviderConfig config) {
        super.update(config);
        //Need LogProviderConfigImpl to get additional information specifically for JsonTraceService
        LogProviderConfigImpl jsonTRConfig = (LogProviderConfigImpl) config;

        //Need to know serverName and wlpUserDir is and pass it to an Handler created by JsonTrService
        //for correct json outoput.
        serverName = jsonTRConfig.getServerName();
        wlpUserDir = jsonTRConfig.getWlpUsrDir();

        if (collectorMgrPipelineUtils == null) {
            collectorMgrPipelineUtils = CollectorManagerPipelineUtils.getInstance();
            collectorMgrPipelineUtils.setJsonTrService(isJSON);//DYKC-temp this should be true.. because we are a jsontraceservice
        }

        //Sources
        logSource = collectorMgrPipelineUtils.getLogSource();
        traceSource = collectorMgrPipelineUtils.getTraceSource();

        //Conduits
        logConduit = collectorMgrPipelineUtils.getLogConduit();
        traceConduit = collectorMgrPipelineUtils.getTraceConduit();

        /*
         * Check if 'json' is configured
         * if 'messageFormat' is set to json PLUS 'messageSources' set start up messageLogHandler
         * if 'consoleFormat' is set to json PLUS 'consoleSource' set start up consoleLogHandler
         */

        //String messageLogConfiguration = "json"; //DYKC-temp //retrieve configured value getConfiguration()
        String messageFormat = jsonTRConfig.getMessageFormat();
        String consoleFormat = jsonTRConfig.getConsoleFormat();

        //Retrieve sourceLists
        List<String> messageSourceList = new ArrayList<String>(jsonTRConfig.getMessageSource());
        List<String> consoleSourceList = new ArrayList<String>(jsonTRConfig.getConsoleSource());

        /*
         * DYKC-temp
         * Filter out Message and Trace from messageSourceList
         * This is so that Handler doesn't 'subscribe' message or trace
         * and kicks off an undesired BufferManagerImpl instance.
         */

        //DYKC-debug System.out.println("messageSourceList " + messageSourceList);
        List<String> filterdSourceList = new ArrayList<String>(messageSourceList);
        filterdSourceList.remove(CollectorConstants.TRACE_CONFIG_VAL);
        filterdSourceList.remove(CollectorConstants.MESSAGES_CONFIG_VAL);

        //DYKC-debug System.out.println("filtered " + filterdSourceList);

        //if messageFormat has been configured to 'json'
        if (messageFormat.toLowerCase().equals(LoggingConstants.JSON_FORMAT)) {
            //If there exists no messageLogHandler, create one; otherwise call modified();
            if (messageLogHandler == null) {
                messageLogHandler = new MessageLogHandler(serverName, wlpUserDir, filterdSourceList);
                messageLogHandler.setSync(sync);
                //Make the utils aware of the handler
                collectorMgrPipelineUtils.setHandler(messageLogHandler);
            } else {
                messageLogHandler.modified(filterdSourceList);
            }
            //for any 'updates'
            messageLogHandler.setFileLogHolder(messagesLog);

            isMessageJsonConfigured = true; //DYKC-temp not the best way, configure to json.

            //configure messages and/or trace
            setUnsetLogTrace(messageSourceList, messageLogHandler);
        }

//          if (consoleLogConfigured){
//          if (consoleLogHandler == null) {
//              consoleLogHandler = new ConsoleLogHandler(serverName, wlpUserDir);
//              consoleLogHandler.setStreamWriter();
//          }
//          isConsoleJsonConfigured = true;
//          }

        //DYKC-tempdebug forcing messageLogHandler to take both as there is no configuration/external logic yet
        //logConduit.addSyncHandler(messageLogHandler);
        //traceConduit.addSyncHandler(messageLogHandler);
        // }
    }

    /*
     * Based on config (sourceList), need to add the syncrhonized to configured source/conduit
     * i.e If user wanted message, assign message, if use wants both, assign both
     */
    private void setUnsetLogTrace(List<String> sourceList, SyncrhonousHandler handler) {
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
        String message = formatter.messageLogFormat(logRecord, logRecord.getMessage());
        //messagesLog.writeRecord(message); //OLD
        //DYKC
        if (!isMessageJsonConfigured && !isConsoleJsonConfigured) {
            messagesLog.writeRecord(message);
            //messageLogHandler.writeToLogNormal(message); //This replaces
        }

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
            /*
             * //DYKC-review
             * If no Routers are set, then there is no way for a message event to go through to LogSource
             * if JSON has been configured. Put this in place (for now) to directly send to logSource by skipping
             * the router.
             *
             * When the Router is fully initialized then it will go through to the Router and then LogSource as appropriate.
             *
             * logSource should only be not null (i.e active) if we are 'JsonTrService' because only 'JsonTrService' will
             * appropriately call setupCollectorManagerPipeline()
             */
            if (logSource != null && (isMessageJsonConfigured || isConsoleJsonConfigured)) {
                logSource.publish(routedMessage); //DYKC-temp currently directly sends to handler that we gave to it.
            }
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
                        /*
                         * //DYKC
                         * If no Routers are set, then there is no way for a trace event to go through to TraceSource
                         * if JSON has been configured. Put this in place (for now) to directly send to logSource by skipping
                         * the router.
                         *
                         * When the Router is fully initialized then it will go through to the Router and then TraceSource as appropriate.
                         *
                         * traceSource should only be not null (i.e active) if we are 'JsonTrService' because only 'JsonTrService' will
                         * appropriately call setupCollectorManagerPipeline()
                         */
                        if (logSource != null && (isMessageJsonConfigured || isConsoleJsonConfigured)) {
                            traceSource.publish(routedTrace);
                        }
                    }
                }
            }
        }
        return retMe;
    }

    @Override
    protected void initializeWriters(LogProviderConfigImpl config) {
        synchronized (sync) {
            super.initializeWriters(config);
        }
    }

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

            if (!isMessageJsonConfigured && !isConsoleJsonConfigured) {
                //messageLogHandler.writeToLogNormal(messageLogFormat);
                //keep old behaviour.. otherwise we're just sending it to handler to write to the same log anyways
                messagesLog.writeRecord(messageLogFormat);
                //messageLogHandler.writeToLogNormal(messageLogFormat);
            }

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
}
