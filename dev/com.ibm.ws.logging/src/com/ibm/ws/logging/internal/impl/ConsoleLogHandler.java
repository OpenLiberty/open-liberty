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

public class ConsoleLogHandler extends JsonLogHandler implements SynchronousHandler, Formatter {

    public static final String COMPONENT_NAME = "com.ibm.ws.logging.internal.impl.ConsoleLogHandler";

    //References for the SystemLogHolders for SysErr and SysOut from BaseTraceService
    private SystemLogHolder sysErrHolder;
    private SystemLogHolder sysOutHolder;
    private boolean isTraceStdout = false;

    //The 'format' we are in: Json or basic (default is basic)
    private String format = LoggingConstants.DEFAULT_MESSAGE_FORMAT;
    private BaseTraceFormatter formatter = null;
    private Integer consoleLogLevel = null;
    private boolean copySystemStreams = false;

    private BaseTraceService baseTraceService = null;

    public ConsoleLogHandler(String serverName, String wlpUserDir, List<String> sourcesList) {
        super(serverName, wlpUserDir, sourcesList);
    }

    @Override
    public String getHandlerName() {
        return COMPONENT_NAME;
    }

    @Override
    public void synchronousWrite(Object event) {
        if (sysOutHolder == null) {
            return;
        }

        //The message to be written out. TODO: catch nulls later when formatting and output message
        String messageOutput = null;
        //Identify if console Message is intended for Stderr - each 'new' write sets it to false.
        boolean isStderr = false;

        /*
         * Given a message 'object' we must determine what type of log event it originates from
         * so that we can extract relevant information.
         *
         * Information such as "levelValue" is extracted if the original message is a LogTraceData.
         */
        GenericData genData = null;
        Integer levelVal = null;
        String loggerName = null;
        if (event instanceof LogTraceData) {
            levelVal = ((LogTraceData) event).getLevelValue();
        }

        genData = (GenericData) event;

        loggerName = genData.getLoggerName();

        /*
         * To write out to the console must determine if we are JSON or BASIC
         * 1. JSON
         * a) Message
         * - Check if it is above consoleLogLevel OR if it is from SysOut/SysErr AND copySystemStreams is true
         * then format as JSON
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
        if (format.equals(LoggingConstants.JSON_FORMAT)) {
            String eventsourceType = getSourceTypeFromDataObject(genData);
            if (eventsourceType.equals(CollectorConstants.MESSAGES_SOURCE) && levelVal != null) {
                if (levelVal >= consoleLogLevel ||
                    (copySystemStreams && isOriginFromSystemStreams(levelVal, loggerName))) {
                    messageOutput = (String) formatEvent(eventsourceType, CollectorConstants.MEMORY, genData, null, MAXFIELDLENGTH);
                }
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
            else if (copySystemStreams && isOriginFromSystemStreams(levelVal, loggerName)) {
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
        //check if message need to be written to stderr or stdout
        if (isStderr) {
            baseTraceService.writeStreamOutput(sysErrHolder, messageOutput, false);
            //sysErrHolder.getOriginalStream().println(messageOutput);
        } else if (messageOutput != null) {
            //sysOutHolder.getOriginalStream().println(messageOutput);
            baseTraceService.writeStreamOutput(sysOutHolder, messageOutput, false);
        }
    }

    /**
     * Return if a message originated from SystemOut or SystemErr
     * This requires the levelValue and loggerName of the message event
     * This specifically checks if it came from BaseTraceService.OutputStream.flush() -> echo()
     * BaseTraceService.OutputStream.flush() creates a logRecord with WsLevel.CONFIG level and loggerName
     * of the SysLogHolder's name (e.g. SystemOut, SystemErr) before passing onto echo()
     *
     * @param levelVal int value of level value of message
     * @param loggerName loggername of which the message originated from
     * @return
     */
    private boolean isOriginFromSystemStreams(Integer levelVal, String loggerName) {
        return (levelVal == WsLevel.CONFIG.intValue()) &&
               (loggerName.equalsIgnoreCase(LoggingConstants.SYSTEM_OUT) || loggerName.equalsIgnoreCase(LoggingConstants.SYSTEM_ERR));
    }

    /**
     * Set copySystemStreams value that was determined from config by BasegTraceService
     *
     * @param copySystemStreams value to determine whether to copysystemstreams or not
     */
    public void setCopySystemStreams(boolean copySystemStreams) {
        this.copySystemStreams = copySystemStreams;
    }

    /**
     * Set BaseTraceFormatter passed from BaseTraceService
     *
     * @param formatter the BaseTraceFormatter to use
     */
    public void setFormatter(BaseTraceFormatter formatter) {
        this.formatter = formatter;
    }

    /**
     * Set consoleLogLevel passed from BaseTraceService
     *
     * @param consoleLogLevel consoleLogLevel
     */
    public void setConsoleLogLevel(Integer consoleLogLevel) {
        this.consoleLogLevel = consoleLogLevel;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    /**
     * Because consoleLogHolder has 'two' writers, SystemOut and SystemErr
     * please use the overloaded setWriter(Object, Object) to set both SystemLogHolders
     * for SystemOut and SystemErr respectfully.
     *
     * @param writer SystemLogHolder object for SystemOut
     */
    public void setWriter(Object writer) {
        this.sysOutHolder = (SystemLogHolder) writer;
    }

    /**
     * Set the writers for SystemOut and SystemErr respectfully
     *
     * @param sysLogHolder SystemLogHolder object for SystemOut
     * @param sysErrHolder SystemLogHolder object for SystemErr
     */
    public void setWriter(Object sysLogHolder, Object sysErrHolder) {
        this.sysOutHolder = (SystemLogHolder) sysLogHolder;
        this.sysErrHolder = (SystemLogHolder) sysErrHolder;
    }

    /**
     * Set the writer for SystemOut
     *
     * @param sysLogHolder SystemLogHolder object for SystemOut
     */
    public void setSysOutHolder(SystemLogHolder sysLogHolder) {
        this.sysOutHolder = sysLogHolder;
    }

    /**
     * Set the writer for SystemErr
     *
     * @param sysLogHolder SystemLogHolder object for SystemErr
     */
    public void setSysErrHolder(SystemLogHolder sysErrHolder) {
        this.sysErrHolder = sysErrHolder;
    }

    /**
     * Set TraceStdOut value, which is a value calculated by BaseTraceService whether user set traceFileName to stdout
     *
     * @param isTraceStdout value calculated by BaseTraceService whether user set traceFileName to stdout
     */
    public void setTraceStdout(boolean isTraceStdout) {
        this.isTraceStdout = isTraceStdout;
    }

    /**
     * Set reference to BaseTraceService instance
     *
     * @param basetraceService reference to BaseTraceService
     */
    public void setBaseTraceService(BaseTraceService basetraceService) {
        this.baseTraceService = basetraceService;
    }
}
