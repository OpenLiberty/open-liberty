/*******************************************************************************
 * Copyright (c) 1997, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

/**
 * Provides a java.util.logging.Formatter implementation similar to WebSphere's SystemOut.log format.
 * The following properties can be set in logging.properties to configure this Formatter:<br>
 * <br>
 * <ul>
 * <li><code>com.ibm.ws.fat.util.GenericFormatter.class.full</code> - controls whether or not to log the full class name (default is false)</li>
 * <li><code>com.ibm.ws.fat.util.GenericFormatter.class.length</code> - controls the length of the class name logged (default is 8)</li>
 * <li><code>com.ibm.ws.fat.util.GenericFormatter.class.log</code> - controls whether or not to log the class name (default is false)</li>
 * <li><code>com.ibm.ws.fat.util.GenericFormatter.level.log</code> - controls whether or not to log the first character of the message level (default is false)</li>
 * <li><code>com.ibm.ws.fat.util.GenericFormatter.method.length</code> - controls the length of the method name logged (default is 8)</li>
 * <li><code>com.ibm.ws.fat.util.GenericFormatter.method.log</code> - controls whether or not to log the method Name (default is false)</li>
 * <li><code>com.ibm.ws.fat.util.GenericFormatter.thread.length</code> - controls the length of the thread ID logged (default is 8)</li>
 * <li><code>com.ibm.ws.fat.util.GenericFormatter.thread.log</code> - controls whether or not to log the thread ID (default is false)</li>
 * <li><code>com.ibm.ws.fat.util.GenericFormatter.time.format</code> - controls whether or not to log the timestamp (default is false)</li>
 * <li><code>com.ibm.ws.fat.util.GenericFormatter.time.log</code> - controls the SimpleDateFormat of the timestamp (default is [MM/dd/yyyy HH:mm:ss:SSS z])</li>
 * </ul>
 *
 * @author Tim Burns
 *
 */
public class GenericFormatter extends Formatter {

    protected static final String PROP_CLASS_FULL = GenericFormatter.class.getName() + ".class.full";
    protected static final String PROP_CLASS_LENGTH = GenericFormatter.class.getName() + ".class.length";
    protected static final String PROP_CLASS_LOG = GenericFormatter.class.getName() + ".class.log";
    protected static final String PROP_LEVEL_LOG = GenericFormatter.class.getName() + ".level.log";
    protected static final String PROP_METHOD_LENGTH = GenericFormatter.class.getName() + ".method.length";
    protected static final String PROP_METHOD_LOG = GenericFormatter.class.getName() + ".method.log";
    protected static final String PROP_THREAD_LENGTH = GenericFormatter.class.getName() + ".thread.length";
    protected static final String PROP_THREAD_LOG = GenericFormatter.class.getName() + ".thread.log";
    protected static final String PROP_TIME_FORMAT = GenericFormatter.class.getName() + ".time.format";
    protected static final String PROP_TIME_LOG = GenericFormatter.class.getName() + ".time.log";

    protected static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("[MM/dd/yyyy HH:mm:ss:SSS z]");
    protected static final int DEFAULT_LENGTH = 8;
    protected static final String LINE_SEPARATOR = System.getProperty("line.separator");
//	protected static final String COLON = ": ";
//	protected static final String CAUSED_BY = "Caused by ";
    protected static final char SPACE = ' ';
    protected static final char ZERO = '0';
    protected static final char DOT = '.';
//	protected static final String AT = "    at ";

    protected boolean logClassName;
    protected int classLength;
    protected boolean logFullClass;
    protected int methodLength;
    protected boolean logLevel;
    protected boolean logMethodName;
    protected boolean logThreadId;
    protected boolean logTimeStamp;
    protected SimpleDateFormat timeFormat;
    protected int threadIdLength;

    /**
     * The primary constructor sets state based on JVM system properties.
     */
    public GenericFormatter() {
        LogManager manager = LogManager.getLogManager();
        this.logFullClass = Boolean.valueOf(manager.getProperty(PROP_CLASS_FULL)).booleanValue();
        this.logClassName = Boolean.valueOf(manager.getProperty(PROP_CLASS_LOG)).booleanValue();
        try {
            this.classLength = Integer.parseInt(manager.getProperty(PROP_CLASS_LENGTH));
        } catch (NumberFormatException e) {
            this.classLength = DEFAULT_LENGTH;
        }
        this.logLevel = Boolean.valueOf(manager.getProperty(PROP_LEVEL_LOG)).booleanValue();
        this.logMethodName = Boolean.valueOf(manager.getProperty(PROP_METHOD_LOG)).booleanValue();
        try {
            this.methodLength = Integer.parseInt(manager.getProperty(PROP_METHOD_LENGTH));
        } catch (NumberFormatException e) {
            this.methodLength = DEFAULT_LENGTH;
        }
        this.logThreadId = Boolean.valueOf(manager.getProperty(PROP_THREAD_LOG)).booleanValue();
        try {
            this.threadIdLength = Integer.parseInt(manager.getProperty(PROP_THREAD_LENGTH));
        } catch (NumberFormatException e) {
            this.threadIdLength = DEFAULT_LENGTH;
        }
        this.logTimeStamp = Boolean.valueOf(manager.getProperty(PROP_TIME_LOG)).booleanValue();
        try {
            this.timeFormat = new SimpleDateFormat(manager.getProperty(PROP_TIME_FORMAT));
        } catch (Exception e) {
            this.timeFormat = DEFAULT_DATE_FORMAT;
        }
    }

    @Override
    public String format(LogRecord record) {
        if (record == null) {
            return "A null LogRecord was formatted";
        }
        StringBuffer buffer = new StringBuffer();
        if (this.logTimeStamp) {
            this.appendTimestamp(buffer, record.getMillis());
            buffer.append(SPACE);
        }
        if (this.logThreadId) {
            this.appendThreadId(buffer, record.getThreadID());
            buffer.append(SPACE);
        }
        if (this.logClassName) {
            this.appendClassName(buffer, record.getSourceClassName());
            buffer.append(SPACE);
        }
        if (this.logMethodName) {
            this.appendMethodName(buffer, record.getSourceMethodName());
            buffer.append(SPACE);
        }
        if (this.logLevel) {
            this.appendLogLevel(buffer, record.getLevel());
            buffer.append(SPACE);
        }
        buffer.append(formatMessage(record));
        buffer.append(LINE_SEPARATOR);

        this.appendThrowable(buffer, record.getThrown());

        return buffer.toString();
    }

    /**
     * Controls whether or not to log the source class name.
     * 
     * @param log true if you want to log the source class name
     */
    public void setLogClassName(boolean log) {
        this.logClassName = log;
    }

    /**
     * Controls whether or not to log the source log level.
     * 
     * @param log true if you want to log the source log level
     */
    public void setLogLevel(boolean log) {
        this.logLevel = log;
    }

    /**
     * Controls whether or not to log the source method name.
     * 
     * @param log true if you want to log the source method name
     */
    public void setLogMethodName(boolean log) {
        this.logMethodName = log;
    }

    /**
     * Controls whether or not to log the source thread ID.
     * 
     * @param log true if you want to log the source thread ID
     */
    public void setLogThreadId(boolean log) {
        this.logThreadId = log;
    }

    /**
     * Controls whether or not to log the time stamp.
     * 
     * @param log true if you want to log the time stamp
     */
    public void setLogTimeStamp(boolean log) {
        this.logTimeStamp = log;
    }

    /**
     * Sets the classLength associated with this instance. If an invalid length
     * is specified, the default length is used.
     * 
     * @param classLength the classLength to set
     */
    public void setClassLength(int classLength) {
        if (classLength > 0) {
            this.classLength = classLength;
        } else {
            this.classLength = DEFAULT_LENGTH;
        }
    }

    /**
     * Sets the logFullClass associated with this instance
     * 
     * @param logFullClass the logFullClass to set
     */
    public void setLogFullClass(boolean logFullClass) {
        this.logFullClass = logFullClass;
    }

    /**
     * Sets the methodLength associated with this instance. If an invalid length
     * is specified, the default length is used.
     * 
     * @param methodLength
     *                         the methodLength to set
     */
    public void setMethodLength(int methodLength) {
        if (methodLength > 0) {
            this.methodLength = methodLength;
        } else {
            this.methodLength = DEFAULT_LENGTH;
        }
    }

    /**
     * Sets the timeFormat associated with this instance
     * 
     * @param timeFormat the timeFormat to set
     */
    public void setTimeFormat(SimpleDateFormat timeFormat) {
        if (timeFormat != null) {
            this.timeFormat = timeFormat;
        }
    }

    /**
     * Sets the threadIdLength associated with this instance. If an invalid length
     * is specified, the default length is used.
     * 
     * @param threadIdLength the threadIdLength to set
     */
    public void setThreadIdLength(int threadIdLength) {
        if (threadIdLength > 0) {
            this.threadIdLength = threadIdLength;
        } else {
            this.threadIdLength = DEFAULT_LENGTH;
        }
    }

    protected void appendThrowable(StringBuffer buffer, Throwable thrown) {
        if (thrown == null) {
            return;
        }
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        thrown.printStackTrace(writer);
        buffer.append(stringWriter.toString());
//		buffer.append(thrown.getClass().getName());
//		buffer.append(COLON);
//		buffer.append(thrown.getMessage());
//		buffer.append(LINE_SEPARATOR);
//		StackTraceElement[] stack = thrown.getStackTrace();
//		for(int i=0; i<stack.length; i++) {
//			buffer.append(AT);
//			buffer.append(stack[i].toString());
//			buffer.append(LINE_SEPARATOR);
//		}
//		Throwable cause = thrown.getCause();
//		if(cause!=null) {
//			buffer.append(CAUSED_BY);
//			this.appendThrowable(buffer, cause);
//		}
    }

    protected void appendLogLevel(StringBuffer buffer, Level level) {
        char c = (level == null) ? 'U' : level.toString().charAt(0);
        buffer.append(c);
    }

    protected void appendMethodName(StringBuffer buffer, String methodName) {
        appendString(buffer, methodName, this.methodLength, false, SPACE, false);
    }

    protected void appendClassName(StringBuffer buffer, String fullClassName) {
        String className = fullClassName;
        if (fullClassName != null && !this.logFullClass) {
            int lastDotIndex = fullClassName.lastIndexOf(DOT);
            lastDotIndex++;
            if (0 < lastDotIndex && lastDotIndex < fullClassName.length()) {
                className = fullClassName.substring(lastDotIndex);
            }
        }
        appendString(buffer, className, this.classLength, false, SPACE, false);
    }

    protected void appendTimestamp(StringBuffer buffer, long millis) {
        buffer.append(this.timeFormat.format(new Date(millis)));
    }

    protected void appendThreadId(StringBuffer buffer, int threadId) {
        String decimal = Integer.toString(threadId);
        appendString(buffer, decimal, this.threadIdLength, true, ZERO, true);
    }

    /**
     * Appends a String 's' to a buffer 'buffer'.
     * 
     * @param buffer
     *                            The buffer you want the String appended to
     * @param s
     *                            The String to append to the buffer
     * @param length
     *                            See the usage for 'allowLonger', and 'prependFiller'
     * @param allowLonger
     *                            If the length of 's' is less than 'length' and 'allowLonger'
     *                            is false, then <code>s.substring(0, length)</code> is
     *                            appended to 'buffer' (else 's' is appended)
     * @param fillChar
     *                            The character you want prepended to Strings that
     *                            do not match the target length
     * @param prependFillChar
     *                            true if you want to prepend the fillChar,<br>
     *                            false if you want to append the fillChar
     */
    protected static void appendString(StringBuffer buffer, String string, int length, boolean allowLonger, char fillChar, boolean prependFillChar) {
        String s = (string == null) ? "(null)" : string;
        int difference = length - s.length();
        if (difference == 0) {
            buffer.append(s);
        } else {
            if (difference < 0) {
                if (allowLonger) {
                    buffer.append(s);
                } else {
                    buffer.append(s.substring(0, length));
                }
            } else {
                if (prependFillChar) {
                    appendFillChar(buffer, fillChar, difference);
                    buffer.append(s);
                } else {
                    buffer.append(s);
                    appendFillChar(buffer, fillChar, difference);
                }
            }
        }
    }

    protected static void appendFillChar(StringBuffer buffer, char fillChar, int length) {
        for (int i = 0; i < length; i++) {
            buffer.append(fillChar);
        }
    }

}
