/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.source;

import java.util.Map;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Class the holds information related a record in messages log file
 */
@Trivial
public class MessageLogData {

    private final long datetime;

    private final int threadId;

    private final String loggerName;

    private final String logLevel;

    private final String logLevelRaw;

    private final String messageId;

    private final String message;

    private final String methodName;

    private final String className;

    private final Map<String, String> extensions;

    private final String sequence;

    // PI76200
    private final Throwable throwable;

    /**
     * Construct a MessageLogData object without a Throwable object
     */
    public MessageLogData(long datetime, int threadId, String loggerName,
                          String logLevel, String logLevelRaw, String messageId, String message,
                          String methodName, String className,
                          Map<String, String> extensions, String sequence) {
        this(datetime, threadId, loggerName, logLevel, logLevelRaw, messageId, message, methodName, className, extensions, sequence, null);

    }

    /**
     * Construct a MessageLogData object with a Throwable object
     */
    public MessageLogData(long datetime, int threadId, String loggerName,
                          String logLevel, String logLevelRaw, String messageId, String message,
                          String methodName, String className,
                          Map<String, String> extensions, String sequence, Throwable throwable) {
        this.datetime = datetime;
        this.threadId = threadId;
        this.loggerName = loggerName;
        this.logLevel = logLevel;
        this.logLevelRaw = logLevelRaw;
        this.messageId = messageId;
        this.message = message;

        //Binary log related
        this.methodName = methodName;
        this.className = className;
        this.extensions = extensions;
        this.sequence = sequence;
        this.throwable = throwable; //PI76200
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

    public String getMessageID() {
        return messageId;
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

    // PI76200
    public Throwable getThrown() {
        return throwable;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "MessageLogData [datetime=" + datetime + ", threadId=" + threadId + ", loggerName=" + loggerName + ", logLevel=" + logLevel + ", logLevelRaw=" + logLevelRaw
               + ", messageId=" + messageId + ", message=" + message + ", methodName=" + methodName + ", className=" + className + ", extensions=" + extensions + ", sequence="
               + sequence + ", throwable=" + throwable + "]";
    }

}
