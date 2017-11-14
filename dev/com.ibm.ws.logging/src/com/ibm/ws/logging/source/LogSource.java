
/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015, 2016
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.logging.source;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.logging.RoutedMessage;
import com.ibm.ws.logging.WsLogHandler;
import com.ibm.ws.logging.internal.WsLogRecord;
import com.ibm.ws.logging.internal.impl.MessageLogHandler;
import com.ibm.ws.logging.synch.ThreadLocalHandler;
import com.ibm.ws.logging.utils.LogFormatUtils;
import com.ibm.wsspi.collector.manager.BufferManager;
import com.ibm.wsspi.collector.manager.Source;

public class LogSource implements Source, WsLogHandler {

    private static final TraceComponent tc = Tr.register(LogSource.class);

    private final String sourceName = "com.ibm.ws.logging.source.message";
    private final String location = "memory";
    private BufferManager bufferMgr = null;
    static Pattern messagePattern;

    //DYKC-temp
    private MessageLogHandler sh = null;

    static {
        messagePattern = Pattern.compile("^([A-Z][\\dA-Z]{3,4})(\\d{4})([A-Z])(:)");
    }

    /**
     *
     */
    public LogSource() {
        System.out.println("LogSource.java - Creating LogSource VERSION 2.0");
    }

    private final AtomicLong seq = new AtomicLong();

    protected void activate(Map<String, Object> configuration) {
        System.out.println("LogSource.java - Activating LogSource");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Activating " + this);
        }
    }

    protected void deactivate(int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, " Deactivating " + this, " reason = " + reason);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setBufferManager(BufferManager bufferMgr) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Setting buffer manager " + this);
        }
        System.out.println("LogSource.java - setting Buffermanger " + bufferMgr.toString());
        this.bufferMgr = bufferMgr;
    }

    /** {@inheritDoc} */
    @Override
    public void unsetBufferManager(BufferManager bufferMgr) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Un-setting buffer manager " + this);
        }
        //Indication that the buffer will no longer be available
        this.bufferMgr = null;
    }

    public BufferManager getBufferManager() {
        return bufferMgr;
    }

    /** {@inheritDoc} */
    @Override
    public String getSourceName() {
        return sourceName;
    }

    /** {@inheritDoc} */
    @Override
    public String getLocation() {
        return location;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public void publish(RoutedMessage routedMessage) {
        //Publish the message if it is not coming from a handler thread
        if (!ThreadLocalHandler.get()) {
            LogRecord logRecord = routedMessage.getLogRecord();
            if (logRecord != null && bufferMgr != null) {
                bufferMgr.add(parse(routedMessage, logRecord));
            }
            //DYKC-temp write directly to handler
            //DYKC-problem
            //DYKC-debug
//            if (logRecord != null && sh != null) {
//                sh.writeJsonifiedEvent(parse(routedMessage, logRecord));
//            }
        }
    }

    //DYKC-temp bypass Conduit/BufferManager
    public void setHandler(MessageLogHandler sh) {
        this.sh = sh;
    }

    public MessageLogData parse(RoutedMessage routedMessage, LogRecord logRecord) {
        String message = routedMessage.getFormattedVerboseMsg();
        if (message == null)
            message = logRecord.getMessage();
        long date = logRecord.getMillis();
        String messageId = null;
        if (message != null)
            messageId = parseMessageId(message);
        int threadId = (int) Thread.currentThread().getId();//logRecord.getThreadID();
        String loggerName = logRecord.getLoggerName();
        String logLevel = LogFormatUtils.mapLevelToType(logRecord);
        String logLevelRaw = LogFormatUtils.mapLevelToRawType(logRecord);
        String methodName = logRecord.getSourceMethodName();
        String className = logRecord.getSourceClassName();
        Map<String, String> extensions = null;
        if (logRecord instanceof WsLogRecord)
            extensions = ((WsLogRecord) logRecord).getExtensions();
        String sequence = date + "_" + String.format("%013X", seq.incrementAndGet());
        Throwable throwable = logRecord.getThrown();

        return new MessageLogData(date, threadId, loggerName, logLevel, logLevelRaw, messageId, message, methodName, className, extensions, sequence, throwable);

    }

    /**
     * @return the message ID for the given message.
     */
    protected String parseMessageId(String msg) {
        String messageId = null;
        Matcher matcher = messagePattern.matcher(msg);
        if (matcher.find())
            messageId = msg.substring(matcher.start(), matcher.end() - 1);
        return messageId;
    }
}
