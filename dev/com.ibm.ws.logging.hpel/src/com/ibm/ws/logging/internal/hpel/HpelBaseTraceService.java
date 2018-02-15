/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.hpel;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import com.ibm.websphere.logging.WsLevel;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.boot.logging.LoggerHandlerManager;
import com.ibm.ws.logging.internal.impl.BaseTraceService;
import com.ibm.ws.logging.internal.impl.LogProviderConfigImpl;
import com.ibm.ws.logging.internal.impl.RoutedMessageImpl;
import com.ibm.wsspi.logging.MessageRouter;
import com.ibm.wsspi.logprovider.LogProviderConfig;

/**
 *
 */
//Just a comment
public class HpelBaseTraceService extends BaseTraceService {
    private final HpelTraceServiceWriter trWriter = new HpelTraceServiceWriter(this);

    @Override
    public synchronized void update(LogProviderConfig config) {
        super.update(config);
        collectorMgrPipelineUtils.setJsonTrService(false);
        logConduit.removeSyncHandler(consoleLogHandler);
    }

    /** {@inheritDoc} */
    @Override
    public void echo(SystemLogHolder holder, LogRecord logRecord) {
        if (copySystemStreams) {
            writeFilteredStreamOutput(holder, logRecord);
        }
        trWriter.repositoryPublish(logRecord);
    }

    boolean notifyConsole(LogRecord logRecord) {
        int levelValue = logRecord.getLevel().intValue();

        if (levelValue >= Level.INFO.intValue()) {
            // Obtain the "formatted" string --> MessageFormat.format(msg, logParams);
            String txt = formatter.formatMessage(logRecord);

            // Look for external log handlers. They may suppress "normal" log
            // processing, which would prevent it from showing up in other logs.
            // This has to be checked in this method: direct invocation of system.out
            // and system.err are not subject to message routing.
            MessageRouter router = externalMessageRouter.get();

            if (router != null) {
                boolean logNormally = router.route(txt, logRecord);
                if (!logNormally)
                    return false;
            }

            if (levelValue >= consoleLogLevel.intValue()) {
                // Send some messages to the system streams
                if (levelValue == WsLevel.ERROR.intValue() || levelValue == WsLevel.FATAL.intValue()) {
                    // WsLevel.ERROR and Level.SEVERE have the same int value
                    // SEVERE, ERROR, FATAL messages are routed to System.err
                    writeStreamOutput(systemErr, formatter.consoleLogFormat(logRecord, txt), false);
                } else {
                    // messages othwerwise above the filter are routed to system out
                    writeStreamOutput(systemOut, formatter.consoleLogFormat(logRecord, txt), false);
                }
            }
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void publishLogRecord(LogRecord logRecord) {
        String formattedMsg = null;
        String formattedVerboseMsg = null;

        Level level = logRecord.getLevel();
        int levelValue = level.intValue();

        if (levelValue >= Level.INFO.intValue()) {
            if (externalMessageRouter.get() != null || internalMessageRouter.get() != null) { // ***THIS IS THE PERFORMANCE OPTIMIZATION***
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
            }
            trWriter.repositoryPublish(logRecord);
        }
        //Route other types of logs (<INFO) to trace (RTC237423)
        else {
            if (TraceComponent.isAnyTracingEnabled()) {
                TraceWriter detailLog = traceLog;
                publishTraceLogRecord(detailLog, logRecord, NULL_ID, formattedMsg, formattedVerboseMsg);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void publishTraceLogRecord(TraceWriter detailLog, LogRecord logRecord, Object id, String formattedMsg, String formattedVerboseMsg) {
        if (formattedVerboseMsg == null) {
            formattedVerboseMsg = formatter.formatVerboseMessage(logRecord, formattedMsg, false);
        }
        //RTC237423: This method (specifically SimpleDateFormat) significantly slows down logging when enabled
        //but the results of this call are not actually used anywhere (for traces), so it can be disabled for now
        //String traceDetail = formatter.traceLogFormat(logRecord, id, formattedMsg, formattedVerboseMsg);
        invokeTraceRouters(new RoutedMessageImpl(formattedMsg, formattedVerboseMsg, null, logRecord));
        trWriter.repositoryPublish(logRecord);
    }

    /** {@inheritDoc} */
    @Override
    protected void initializeWriters(LogProviderConfigImpl config) {
        trWriter.configure((HpelTraceServiceConfig) config);
    }

    @Override
    protected void registerLoggerHandlerSingleton() {
        LoggerHandlerManager.setSingleton(new Handler() {
            @Override
            public void publish(LogRecord logRecord) {
                HpelBaseTraceService.this.publishLogRecord(logRecord);
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        });
    }

    @Override
    protected void unregisterLoggerHandlerSingleton() {
        LoggerHandlerManager.unsetSingleton();
        trWriter.stop();
    }
}
