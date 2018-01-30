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
        GenericData genData = ((LogTraceData) event).getGenData();

        String evensourcetType = getSourceTypeFromDataObject(genData);
        String messageOutput = null;
        boolean isStderr = false;
        if (format.equals(LoggingConstants.JSON_FORMAT)) {
            messageOutput = (String) formatEvent(evensourcetType, CollectorConstants.MEMORY, genData, null, MAXFIELDLENGTH);
        }
        //add in formatter here for basic
        else if (format.equals(LoggingConstants.DEFAULT_CONSOLE_FORMAT) && formatter != null) {
            //if traceFilename == systemOut write to console.log in trace format
            //isTraceStdout
            Integer levelVal = ((LogTraceData) event).getLevelValue();
            if (isTraceStdout) {
                //check if message need to be written to stderr
                if (levelVal == WsLevel.ERROR.intValue() || levelVal == WsLevel.FATAL.intValue()) {
                    isStderr = true;
                }
                messageOutput = formatter.traceFormatGenData(genData);
            } else if (copySystemStreams && levelVal == 700) {// copySystemStream and stderr/stdout level 700
                //write
                messageOutput = formatter.filteredStreamOutput(genData);

            }
            //determin if it is system.out/err and !copysystemstream then throw it out
            //if !isTraceStdout && level >= consoleloglevel
            else if (levelVal >= consoleLogLevel) {//not system.out/system.err && tracefilename != stdout
                messageOutput = formatter.consoleLogFormat(genData);
            }
        }
        synchronized (this) {
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
     * @return the formatter
     */
    public BaseTraceFormatter getFormatter() {
        return formatter;
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
    public Integer getConsoleLogLevel() {
        return consoleLogLevel;
    }

    /**
     * @return the consoleLogLevel
     */
    public void setConsoleLogLevel(Integer consoleLogLevel) {
        this.consoleLogLevel = consoleLogLevel;
    }

    /**
     * @return the format
     */
    public String getFormat() {
        return format;
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
     * @return the sysErrHolder
     */
    public SystemLogHolder getSysErrHolder() {
        return sysErrHolder;
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
