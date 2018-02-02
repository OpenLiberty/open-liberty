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
        if (event instanceof LogTraceData) {
            genData = ((LogTraceData) event).getGenData();
        } else if (event instanceof GenericData) {
            genData = (GenericData) event;
        }

        String evensourcetType = getSourceTypeFromDataObject(genData);
        String messageOutput = null;
        boolean isStderr = false;
        if (format.equals(LoggingConstants.JSON_FORMAT)) {
            messageOutput = (String) formatEvent(evensourcetType, CollectorConstants.MEMORY, genData, null, MAXFIELDLENGTH);
        }
        //add in formatter here for basic
        else if (format.equals(LoggingConstants.DEFAULT_CONSOLE_FORMAT) && formatter != null) {
            //if traceFilename=stdout write everything to console.log in trace format
            Integer levelVal = ((LogTraceData) event).getLevelValue();
            if (isTraceStdout) {
                //check if message need to be written to stderr
                if (levelVal == WsLevel.ERROR.intValue() || levelVal == WsLevel.FATAL.intValue()) {
                    isStderr = true;
                }
                messageOutput = formatter.traceFormatGenData(genData);
            } // copySystemStream and stderr/stdout level=700
            else if (copySystemStreams && (levelVal == 700)) {
                messageOutput = formatter.filteredStreamOutput(genData);

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
                sysErrHolder.getOriginalStream().println(messageOutput);
            } else if (messageOutput != null) {
                sysLogHolder.getOriginalStream().println(messageOutput);
            }
        }

    }

    @Override
    public void setWriter(Object writer) {
        this.sysLogHolder = (SystemLogHolder) writer;
    }

    /**
     * @param formatter the formatter to set
     */
    public void setFormatter(BaseTraceFormatter formatter) {
        this.formatter = formatter;
    }

    /**
     * @return the consoleLogLevel
     */
    public void setConsoleLogLevel(Integer consoleLogLevel) {
        this.consoleLogLevel = consoleLogLevel;
    }

    /**
     * @param format the format to set
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * @param copySystemStreams the copySystemStreams to set
     */
    public void setCopySystemStreams(boolean copySystemStreams) {
        this.copySystemStreams = copySystemStreams;
    }

    /**
     * @param sysErrHolder the sysErrHolder to set
     */
    public void setSysErrHolder(SystemLogHolder sysErrHolder) {
        this.sysErrHolder = sysErrHolder;
    }

    public void setIsTraceStdout(boolean isTraceStdout) {
        this.isTraceStdout = isTraceStdout;
    }

}
