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

import java.util.logging.Level;
import java.util.logging.LogRecord;

import com.ibm.websphere.logging.WsLevel;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.collector.manager.buffer.BufferManagerImpl;
import com.ibm.ws.logging.RoutedMessage;
import com.ibm.ws.logging.WsMessageRouter;
import com.ibm.ws.logging.source.LogSource;
import com.ibm.ws.logging.source.TraceSource;
import com.ibm.ws.logging.utils.HandlerUtils;
import com.ibm.wsspi.logging.MessageRouter;
import com.ibm.wsspi.logprovider.LogProviderConfig;

/**
 *
 */
public class JsonTraceService extends BaseTraceService {

    private volatile LogSource logSource = null;//IF JsonTrService, this moves aswell
    private volatile TraceSource traceSource = null;//IF JsonTrService, this moves aswell
    private volatile MessageLogHandler messageLogHandler = null;//IF JsonTrService, this moves aswell
    private volatile BufferManagerImpl logConduit;//IF JsonTrService, this moves aswell
    private volatile BufferManagerImpl traceConduit;//IF JsonTrService, this moves aswell
    private volatile HandlerUtils handlerUtils = null; //IF JsonTrService, this moves aswell

    @Override
    public synchronized void update(LogProviderConfig config) {
        super.update(config);
        setupCollectorManagerPipeline();
    }

    /*
     * Set up that pipeline. Should only appear in JsonTrService????
     *
     */
    private void setupCollectorManagerPipeline() {
        /*
         * //DYKC how to avoid other BTS from starting this up due to 'updates' calls in HPEL and JSR47.
         * If we don't have a handlerUtils, we definitely need one.
         * Should we always have this enabled?
         *
         */
        if (handlerUtils == null) {
            handlerUtils = HandlerUtils.getInstance();

            //Sources
            logSource = handlerUtils.getLogSource();
            traceSource = handlerUtils.getTraceSource();

            //Conduits
            logConduit = handlerUtils.getLogConduit();
            traceConduit = handlerUtils.getTraceConduit();

            System.out.println("BASE TRACE SERVICE LogConduit is = " + logConduit.toString());
            System.out.println("BASE TRACE SERVICE logSource is = " + logSource.toString());
        }
        //Create Handler and pass it to CMBootStrap
        if (messageLogHandler == null) {
            messageLogHandler = new MessageLogHandler(serverName, wlpUserDir);
            messageLogHandler.setFileLogHolder(messagesLog);
            logSource.setHandler(messageLogHandler); //DYKC-temp hardwire handler to source
            handlerUtils.setHandler(messageLogHandler);
        }

    }

    @Override
    public void echo(SystemLogHolder holder, LogRecord logRecord) {
        TraceWriter detailLog = traceLog;

        // Tee to messages.log (always)
        String message = formatter.messageLogFormat(logRecord, logRecord.getMessage());
        //messagesLog.writeRecord(message); //OLD
        //DYKC
        //If not configured do:
        //If not configured, skip;
        messageLogHandler.writeToLogNormal(message); //This replaces

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
             * //DYKC
             * If no Routers are set, then there is no way for a message event to go through to LogSource
             * if JSON has been configured. Put this in place (for now) to directly send to logSource by skipping
             * the router.
             *
             * When the Router is fully initialized then it will go through to the Router and then LogSource as appropriate.
             *
             * logSource should only be not null (i.e active) if we are 'JsonTrService' because only 'JsonTrService' will
             * appropriately call setupCollectorManagerPipeline()
             */
            if (logSource != null) {
                logSource.publish(routedMessage);
            }
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

            //DYKC
            // messages.log  //send directly.
            //messagesLog.writeRecord(messageLogFormat); // OLD
            //if not configured - do this - Its nice that we've already got invoke Message Routers above.
            //But will need to make sure it doesn't go ahead and JSONIFIES
            // update ( which recieves configuration) will dictate if it gets configured?

            //DYKC-ccode if (notConfigured){
            messageLogHandler.writeToLogNormal(messageLogFormat);
            //DYKC-ccode }

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
