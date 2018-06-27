/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
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

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.boot.logging.LoggerHandlerManager;
import com.ibm.ws.logging.RoutedMessage;
import com.ibm.ws.logging.internal.impl.BaseTraceService;
import com.ibm.ws.logging.internal.impl.LogProviderConfigImpl;
import com.ibm.ws.logging.internal.impl.RoutedMessageImpl;
import com.ibm.wsspi.logging.MessageRouter;

/**
 *
 */
//Just a comment
public class HpelBaseTraceService extends BaseTraceService {
    private final HpelTraceServiceWriter trWriter = new HpelTraceServiceWriter(this);

    /** {@inheritDoc} */
    @Override
    public void echo(SystemLogHolder holder, LogRecord logRecord) {
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
            formattedMsg = formatter.formatMessage(logRecord);
            formattedVerboseMsg = formatter.formatVerboseMessage(logRecord, formattedMsg);

            RoutedMessage routedMessage = null;
            if (externalMessageRouter.get() != null || internalMessageRouter.get() != null) {
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

            trWriter.repositoryPublish(logRecord);

            //If any messages configured to be hidden then those will not be written to console.log and will be redirected to logdata/tracedata
            if (isMessageHidden(formattedMsg)) {
                return;
            }

            if (logSource != null) {
                publishToLogSource(routedMessage);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void publishTraceLogRecord(TraceWriter detailLog, LogRecord logRecord, Object id, String formattedMsg, String formattedVerboseMsg) {
        if (formattedVerboseMsg == null) {
            formattedVerboseMsg = formatter.formatVerboseMessage(logRecord, formattedMsg, false);
        }
        RoutedMessage routedTrace = new RoutedMessageImpl(formattedMsg, formattedVerboseMsg, null, logRecord);
        //RTC237423: This method (specifically SimpleDateFormat) significantly slows down logging when enabled
        //but the results of this call are not actually used anywhere (for traces), so it can be disabled for now
        //String traceDetail = formatter.traceLogFormat(logRecord, id, formattedMsg, formattedVerboseMsg);
        invokeTraceRouters(routedTrace);
        try {
            if (!(counterForTraceSource.incrementCount() > 2)) {
                if (traceSource != null) {
                    traceSource.publish(routedTrace, id);
                }
            }
        } finally {
            counterForTraceRouter.decrementCount();
        }
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
                Level level = logRecord.getLevel();
                int levelValue = level.intValue();
                if (levelValue >= Level.INFO.intValue()) {
                    HpelBaseTraceService.this.publishLogRecord(logRecord);
                } else {
                    if (TraceComponent.isAnyTracingEnabled()) {
                        HpelBaseTraceService.this.publishTraceLogRecord(traceLog, logRecord, NULL_ID, null, null);
                    }
                }
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
