/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
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

    private SystemLogHolder sysErrHolder;
    private SystemLogHolder sysOutHolder;
    private boolean isTraceStdout = false;

    private String format = LoggingConstants.DEFAULT_CONSOLE_FORMAT;
    private BaseTraceFormatter basicFormatter = null;
    private Integer consoleLogLevel = null;

    private static boolean copySystemStreams = false;

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
        if (event instanceof LogTraceData) {
            genData = (LogTraceData) event;
            levelVal = ((LogTraceData) event).getLevelValue();
        } else if (event instanceof GenericData) {
            genData = (GenericData) event;
        } else {
            throw new IllegalArgumentException("event not an instance of GenericData");
        }

        String eventSourceName = getSourceNameFromDataObject(genData);

        /*
         * To write out to the console must determine if we are JSON or DEV / SIMPLE (default message format) or the deprecated format name BASIC
         * 1. (JSON OR not a message/log-source event) AND NOT ( tracefile=stdout + dev/simple format config)
         * Note: The "not a message/log-source event condition" is to ensure any non-message sources that were on-route to the consoleLogHandler
         * before a switch to 'dev' will be properly formatted as JSON instead of directly going to basic formatter.
         * a) Message
         * - Check if it is above consoleLogLevel OR if it is from SysOut/SysErr AND copySystemStreams is true
         * then format as JSON
         * b) Not Message (i.e. AccessLog, Trace, FFDC)
         * - format as JSON
         * 2. DEV - There can be three message origins for dev messages
         * a) tracefileName == stdout
         * - Checks if levelVal was ERROR or FATAL > indicates stderr
         * - format as Trace
         * b) Second check if this message originated from echo() and copySystemStreams is true
         * - Also check if Level is CONFIG and loggerName is SYSOUT or SYSERR
         * - Format as streamOutput (This does filtering/truncating of stack traces and append [err] as necessary)
         * c) Lastly this leaves message origin from publishLogRecord()
         * - We must check if it is above consoleLogLevel to format it
         */
        if ((format.equals(LoggingConstants.JSON_FORMAT) || !eventSourceName.equals(CollectorConstants.MESSAGES_SOURCE))
            && (!((format.equals(LoggingConstants.DEFAULT_CONSOLE_FORMAT) || format.equals(LoggingConstants.DEFAULT_MESSAGE_FORMAT)
                   || format.equals(LoggingConstants.DEPRECATED_DEFAULT_FORMAT))
                  && eventSourceName.equals(CollectorConstants.TRACE_SOURCE) && isTraceStdout))) {

            //First retrieve a cached JSON  message if possible, if not, format it and store it.
            if (genData.getJsonMessage() == null) {
                genData.setJsonMessage((String) formatEvent(eventSourceName, CollectorConstants.MEMORY, event, null, MAXFIELDLENGTH));
            }
            messageOutput = genData.getJsonMessage();

            /*
             * Go through JSON console filters to identify if we want to output this message or not. For example, if a cached message is
             * retrieved and it is a message, but is not above the console log level nor is is from sysout/syserr (wrt to copySystem Streams) then
             * we don't want to print this message out. Then check if messageOutput is null. This shouldn't happen, but if it is we need to generate the formatted
             * output
             */
            if (eventSourceName.equals(CollectorConstants.MESSAGES_SOURCE) && levelVal != null) {
                if (levelVal >= consoleLogLevel || (copySystemStreams && (levelVal == WsLevel.CONFIG.intValue()))) {
                    if (messageOutput == null) {
                        messageOutput = (String) formatEvent(eventSourceName, CollectorConstants.MEMORY, genData, null, MAXFIELDLENGTH);
                    }
                } else {
                    return;
                }
            } else {
                if (messageOutput == null) {
                    messageOutput = (String) formatEvent(eventSourceName, CollectorConstants.MEMORY, genData, null, MAXFIELDLENGTH);
                }
            }

        } else if ((format.equals(LoggingConstants.DEFAULT_CONSOLE_FORMAT) || format.equals(LoggingConstants.DEFAULT_MESSAGE_FORMAT)
                    || format.equals(LoggingConstants.DEPRECATED_DEFAULT_FORMAT))
                   && basicFormatter != null) {
            //if traceFilename=stdout write everything to console.log in trace format
            String logLevel = ((LogTraceData) event).getLoglevel();
            if (isTraceStdout) {
                //check if message need to be written to stderr
                if (levelVal == WsLevel.ERROR.intValue() || levelVal == WsLevel.FATAL.intValue()) {
                    isStderr = true;
                }
                messageOutput = basicFormatter.traceFormatGenData(genData);
            } // copySystemStream and stderr/stdout (i.e WsLevel.CONFIG)
            else if (copySystemStreams && (levelVal == WsLevel.CONFIG.intValue())) {
                if (logLevel != null) {
                    if (logLevel.equals(LoggingConstants.SYSTEM_ERR)) {
                        isStderr = true;
                    }
                }
                messageOutput = format.equals(LoggingConstants.DEFAULT_MESSAGE_FORMAT) ? basicFormatter.messageLogFormat(genData) : basicFormatter.formatStreamOutput(genData);

                //Null return values means we are suppressing a stack trace.. and we don't want to write a 'null' so we return.
                if (messageOutput == null)
                    return;
            }
            //Lastly origin of message is from publishLogRecord and we need to check if it is above consoleLogLevel
            else if (levelVal >= consoleLogLevel) {
                //need to use formatmessage filter
                if (levelVal == WsLevel.ERROR.intValue() || levelVal == WsLevel.FATAL.intValue()) {
                    isStderr = true;
                }

                messageOutput = format.equals(LoggingConstants.DEFAULT_MESSAGE_FORMAT) ? basicFormatter.messageLogFormat(genData) : basicFormatter.consoleLogFormat(genData);
            }
        }

        //Write out to stderr or stdout
        if (isStderr) {
            baseTraceService.writeStreamOutput(sysErrHolder, messageOutput, false);
        } else if (messageOutput != null) {
            baseTraceService.writeStreamOutput(sysOutHolder, messageOutput, false);
        }

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
     * Set copySystemStreams value that was determined from config by BaseTraceService
     *
     * @param copySystemStreams value to determine whether to copysystemstreams or not
     */
    public void setCopySystemStreams(boolean copySystemStreams) {
        this.copySystemStreams = copySystemStreams;
    }

    /**
     * Set BaseTraceFormatter passed from BaseTraceService
     * This Formatter is used to format the dev, simple, or basic (deprecated) log events
     * that pass through
     *
     * @param formatter the BaseTraceFormatter to use
     */
    public void setBasicFormatter(BaseTraceFormatter formatter) {
        this.basicFormatter = formatter;
    }

    /**
     * Set consoleLogLevel passed from BaseTraceService
     *
     * @param consoleLogLevel consoleLogLevel
     */
    public void setConsoleLogLevel(Integer consoleLogLevel) {
        this.consoleLogLevel = consoleLogLevel;
    }

    /**
     * The format to set (i.e. DEV, SIMPLE, or JSON)
     *
     * @param format the format to set (i.e. DEV, SIMPLE, or JSON)
     */
    public void setFormat(String format) {
        this.format = format;
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
    public void setBaseTraceService(BaseTraceService BTS) {
        this.baseTraceService = BTS;
    }
}
