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

import com.ibm.websphere.logging.WsLevel;
import com.ibm.ws.logging.collector.CollectorConstants;
import com.ibm.ws.logging.collector.Formatter;
import com.ibm.ws.logging.data.GenericData;
import com.ibm.ws.logging.data.LogTraceData;
import com.ibm.ws.logging.internal.impl.BaseTraceService.SystemLogHolder;
import com.ibm.wsspi.collector.manager.SynchronousHandler;

/**
 *
 */
public class ConsoleLogHandler extends JsonLogHandler implements SynchronousHandler, Formatter {

    public static final String COMPONENT_NAME = "com.ibm.ws.logging.internal.impl.ConsoleLogHandler";
    //have two writers systemout and systemerr
    private SystemLogHolder sysErrHolder;
    private SystemLogHolder sysLogHolder;
    private boolean isTraceStdout = false;

    //The 'format' we are in: Json or basic (default is basic)
    private String format = LoggingConstants.DEFAULT_MESSAGE_FORMAT;
    private BaseTraceFormatter formatter = null;
    private Integer consoleLogLevel = null;
    private boolean copySystemStreams = false;

    private BaseTraceService BTS = null;

    public ConsoleLogHandler(String serverName, String wlpUserDir, List<String> sourcesList) {
        super(serverName, wlpUserDir, sourcesList);
    }

    @Override
    public String getHandlerName() {
        return COMPONENT_NAME;
    }

    @Override
    public void synchronousWrite(Object event) {
        if (sysLogHolder == null) {
            return;
        }
        String messageOutput = null;

        //Used to identify if console Message is intended for Stderr - each 'new' write sets it to false.
        boolean isStderr = false;

        /*
         * Given an 'object' we must determine what type of log event it originates from.
         * Knowing that it is a *Data object, we can figure what type of source it is.
         */
        GenericData genData = null;
        Integer levelVal = null;
        String loggerName = null;
        if (event instanceof LogTraceData) {
            genData = ((LogTraceData) event).getGenData();
            levelVal = ((LogTraceData) event).getLevelValue();
        } else if (event instanceof GenericData) {
            genData = (GenericData) event;
        }
        loggerName = genData.getLoggerName();

        /*
         * To write out to the console must determine if we are JSON or BASIC
         * 1. JSON
         * a) Message
         * - Check if it is above consoleLogLevel and format as JSON
         * b) Not Message (i.e. AccessLog, Trace, FFDC)
         * - format as JSON
         * 2. BASIC - There can be three message origins for basic messages
         * a) tracefileName == stdout
         * - Checks if levelVal was ERROR or FATAl > indicates stderr
         * - format as Trace
         * b) Second check if this message originated from echo() and copySystemStreams is true
         * - Also check if Level is CONFIG and loggerName is SYSOUT or SYSERR
         * - Format as streamOutput (This does filtering/truncating of stack traces and append [err] as necessary)
         * c) Lastly this leaves message origin from publishLogRecord()
         * - We must check if it is above consoleLogLevel to format it
         */
        String eventsourceType = getSourceTypeFromDataObject(genData);
        if (format.equals(LoggingConstants.JSON_FORMAT)) {
            if (eventsourceType.equals(CollectorConstants.MESSAGES_SOURCE) && levelVal != null) {
                //if (levelVal >= consoleLogLevel) {
                messageOutput = (String) formatEvent(eventsourceType, CollectorConstants.MEMORY, genData, null, MAXFIELDLENGTH);
                //}
            } else {
                messageOutput = (String) formatEvent(eventsourceType, CollectorConstants.MEMORY, genData, null, MAXFIELDLENGTH);
            }
        } else if (format.equals(LoggingConstants.DEFAULT_CONSOLE_FORMAT) && formatter != null) {
            //If traceFilename == stdout write everything to console.log in trace format
            if (isTraceStdout) {
                //Check if message need to be written to stderr
                if (levelVal == WsLevel.ERROR.intValue() || levelVal == WsLevel.FATAL.intValue()) {
                    isStderr = true;
                }
                messageOutput = formatter.traceFormatGenData(genData);

            } // copySystemStream and stderr/stdout (i.e WsLevel.CONFIG) and logger is sysout or syserr
            else if (copySystemStreams &&
                     levelVal == WsLevel.CONFIG.intValue() &&
                     (loggerName.equalsIgnoreCase(LoggingConstants.SYSTEM_OUT) || loggerName.equalsIgnoreCase(LoggingConstants.SYSTEM_ERR))) {
                if (loggerName.equalsIgnoreCase(LoggingConstants.SYSTEM_ERR)) {
                    isStderr = true;
                }
                messageOutput = formatter.formatStreamOutput(genData);

                //Null return values means we are suppressing a stack trace.. and we don't want to write a 'null' so we retur.
                if (messageOutput == null)
                    return;
            }
            //Lastly origin of message is from publishLogRecord and we need to check if it is above consoleLogLevel
            else if (levelVal >= consoleLogLevel) {
                //need to use formatmessage filter
                if (levelVal == WsLevel.ERROR.intValue() || levelVal == WsLevel.FATAL.intValue()) {
                    isStderr = true;
                }
                messageOutput = formatter.consoleLogFormat(genData);
            }
        }
        synchronized (this) {
            //check if message need to be written to stderr or stdout
            if (isStderr) {
                BTS.writeStreamOutput(sysErrHolder, messageOutput, false);
            } else if (messageOutput != null) {
                BTS.writeStreamOutput(sysLogHolder, messageOutput, false);
            }
        }
    }

    public boolean getCopySystemStreams() {
        return copySystemStreams;
    }

    public void setCopySystemStreams(boolean copySystemStreams) {
        this.copySystemStreams = copySystemStreams;
    }

    public void setFormatter(BaseTraceFormatter formatter) {
        this.formatter = formatter;
    }

    public void setConsoleLogLevel(Integer consoleLogLevel) {
        this.consoleLogLevel = consoleLogLevel;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setSysErrHolder(SystemLogHolder sysErrHolder) {
        this.sysErrHolder = sysErrHolder;
    }

    public void setTraceStdout(boolean isTraceStdout) {
        this.isTraceStdout = isTraceStdout;
    }

    /**
     * @param trErr the trErr to set
     */
    public void setBTS(BaseTraceService BTS) {
        this.BTS = BTS;
    }

    @Override
    public void setWriter(Object writer) {
        this.sysLogHolder = (SystemLogHolder) writer;
    }
}
