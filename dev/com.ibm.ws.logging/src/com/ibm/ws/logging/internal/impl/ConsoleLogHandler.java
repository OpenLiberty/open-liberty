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
        /*
         * Given an 'object' we must determine what type of log event it originates from.
         * Knowing that it is a *Data object, we can figure what type of source it is.
         */
        GenericData genData = null;
        Integer levelVal = null;
        if (event instanceof LogTraceData) {
            genData = ((LogTraceData) event).getGenData();
            levelVal = ((LogTraceData) event).getLevelValue();
        } else if (event instanceof GenericData) {
            genData = (GenericData) event;
        }

        String messageOutput = null;
        boolean isStderr = false;
        if (format.equals(LoggingConstants.JSON_FORMAT)) {
            String eventsourceType = getSourceTypeFromDataObject(genData);
            if (eventsourceType.equals(CollectorConstants.MESSAGES_SOURCE) && levelVal != null) {
                if (levelVal >= consoleLogLevel) {
                    messageOutput = (String) formatEvent(eventsourceType, CollectorConstants.MEMORY, genData, null, MAXFIELDLENGTH);
                }
            } else {
                messageOutput = (String) formatEvent(eventsourceType, CollectorConstants.MEMORY, genData, null, MAXFIELDLENGTH);
            }
        } else if (format.equals(LoggingConstants.DEFAULT_CONSOLE_FORMAT) && formatter != null) {
            //if traceFilename=stdout write everything to console.log in trace format
            String logLevel = ((LogTraceData) event).getLogLevel();
            if (isTraceStdout) {
                //check if message need to be written to stderr
                if (levelVal == WsLevel.ERROR.intValue() || levelVal == WsLevel.FATAL.intValue()) {
                    isStderr = true;
                }
                messageOutput = formatter.traceFormatGenData(genData);
            } // copySystemStream and stderr/stdout level=700
            else if (copySystemStreams && (levelVal == 700)) {
                if (logLevel != null) {
                    if (logLevel.equals("SystemErr")) {
                        isStderr = true;
                    }
                }
                messageOutput = formatter.formatStreamOutput(genData);
            }
            //if !isTraceStdout && level >= consoleloglevel
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

    @Override
    public void setWriter(Object writer) {
        this.sysLogHolder = (SystemLogHolder) writer;
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

    public void setCopySystemStreams(boolean copySystemStreams) {
        this.copySystemStreams = copySystemStreams;
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
}
