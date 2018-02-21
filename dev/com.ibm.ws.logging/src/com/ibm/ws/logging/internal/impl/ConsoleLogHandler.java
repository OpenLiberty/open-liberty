/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.impl;

import java.util.List;
import java.util.logging.Level;

import com.ibm.websphere.logging.WsLevel;
import com.ibm.ws.logging.collector.CollectorConstants;
import com.ibm.ws.logging.collector.Formatter;
import com.ibm.ws.logging.data.GenericData;
import com.ibm.ws.logging.internal.impl.BaseTraceService.SystemLogHolder;
import com.ibm.wsspi.collector.manager.SynchronousHandler;

/**
 *
 */
public class ConsoleLogHandler extends JsonLogHandler implements SynchronousHandler, Formatter {

    public static final String COMPONENT_NAME = "com.ibm.ws.logging.internal.impl.ConsoleLogHandler";
    private SystemLogHolder sysLogHolder;

    private Level consoleLogLevel;
    private boolean copySystemStreams;

    public ConsoleLogHandler(String serverName, String wlpUserDir, List<String> sourcesList) {
        super(serverName, wlpUserDir, sourcesList);
    }

    @Override
    public String getHandlerName() {
        return COMPONENT_NAME;
    }

    @Override
    public void synchronousWrite(Object event) {
        /*
         * Given an 'object' we must determine what type of log event it originates from.
         * Knowing that it is a *Data object, we can figure what type of source it is.
         */
        String evensourcetType = getSourceTypeFromDataObject(event);
        int logLevelValue = Integer.MIN_VALUE;
        String loggerName = null;
        String sourceType = null;
        if (event instanceof GenericData) {
            GenericData genData = (GenericData) event;
            loggerName = genData.getLoggerName();
            sourceType = genData.getSourceType();
            Level logRecordLevel = genData.getLogRecordLevel();
            if (logRecordLevel != null) {
                logLevelValue = logRecordLevel.intValue();
            }
        }

        String messageOutput = (String) formatEvent(evensourcetType, CollectorConstants.MEMORY, event, null, MAXFIELDLENGTH);
        synchronized (this) {
            //We will be writing out  accessLog or ffdc or trace
            if (sourceType.equals(CollectorConstants.ACCESS_LOG_SOURCE) ||
                sourceType.equals(CollectorConstants.TRACE_SOURCE) ||
                sourceType.equals(CollectorConstants.FFDC_SOURCE)) {
                sysLogHolder.getOriginalStream().println(messageOutput);
                return;
            }

            /*
             * We only allow two types of console messages to go through:
             *
             * 
             * 1. CopySystemStreams is true AND this message came from BaseTraceService.TrOutputStream which exhibit the following characteristics:
             * - LogLevel of WsLevel.CONFIG
             * - LoggerName of LoggingConstants.SYSTEM_OUT (i.e SystemOut) OR loggerNameLoggingConstants.SYSTEM_ERR (i.e. SystemErr)
             * OR
             * 2. Either this message is greater than or equal to consoleLogLevel (i.e from publishLogRecord)
             *
             */

            if (copySystemStreams &&
                logLevelValue == WsLevel.CONFIG.intValue() &&
                (loggerName.equalsIgnoreCase(LoggingConstants.SYSTEM_OUT) || loggerName.equalsIgnoreCase(LoggingConstants.SYSTEM_ERR))) {
                sysLogHolder.getOriginalStream().println(messageOutput);
                return;
            }

            if (logLevelValue >= consoleLogLevel.intValue()) {
                sysLogHolder.getOriginalStream().println(messageOutput);
                return;
            }

        }
    }

    @Override
    public void setWriter(Object writer) {
        this.sysLogHolder = (SystemLogHolder) writer;
    }

    public Level getConsoleLogLevel() {
        return consoleLogLevel;
    }

    public void setConsoleLogLevel(Level consoleLogLevel) {
        this.consoleLogLevel = consoleLogLevel;
    }

    public boolean getCopySystemStreams() {
        return copySystemStreams;
    }

    public void setCopySystemStreams(boolean copySystemStreams) {
        this.copySystemStreams = copySystemStreams;
    }
}
