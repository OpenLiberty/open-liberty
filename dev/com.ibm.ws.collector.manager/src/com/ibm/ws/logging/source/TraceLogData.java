/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.logging.source;

import java.util.Map;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Class the holds information related a record in trace log file
 */
@Trivial
public class TraceLogData {

    private final long datetime;

    private final int threadId;

    private final String loggerName;

    private final String logLevel;

    private final String logLevelRaw;

    private final String message;

    private final String methodName;

    private final String className;

    private final Map<String, String> extensions;

    private final String sequence;

    public TraceLogData(long datetime, int threadId, String loggerName,
                        String logLevel, String logLevelRaw, String message,
                        String methodName, String className,
                        Map<String, String> extensions, String sequence) {
        this.datetime = datetime;
        this.threadId = threadId;
        this.loggerName = loggerName;
        this.logLevel = logLevel;
        this.logLevelRaw = logLevelRaw;
        this.message = message;
        this.methodName = methodName;
        this.className = className;
        this.extensions = extensions;
        this.sequence = sequence;
    }

    public long getDatetime() {
        return datetime;
    }

    public int getThreadID() {
        return threadId;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public String getLogLevelRaw() {
        return logLevelRaw;
    }

    public String getMessage() {
        return message;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getClassName() {
        return className;
    }

    public Map<String, String> getExtensions() {
        return extensions;
    }

    public String getSequence() {
        return sequence;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "TraceLogData [datetime=" + datetime + ", threadId=" + threadId + ", loggerName=" + loggerName + ", logLevel=" + logLevel + ", logLevelRaw=" + logLevelRaw
               + ", message=" + message + ", methodName=" + methodName + ", className=" + className + ", extensions=" + extensions + ", sequence="
               + sequence + "]";
    }

}
