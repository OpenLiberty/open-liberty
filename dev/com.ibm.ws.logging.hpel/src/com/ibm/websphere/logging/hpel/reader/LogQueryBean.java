/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.logging.hpel.reader;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Simple bean class to hold more static parts of a given query. The client layer in the remote reader infraStructure
 * has several scenarios where it must reQuery the host with various differences (ie: populating the next serverInstance
 * or populating the next n rows of the current server instance). This stores the parts of the query less likely to
 * change in those scenarios.
 *
 * @ibm-api
 */
public class LogQueryBean implements Serializable {
    private static final long serialVersionUID = -6635572279743895675L;
    private Date minTime;
    private Date maxTime;
    private Level minLevel;
    private Level maxLevel;
    private String[] messageContent;
    private String[] excludeMessages;
    private String[] includeLoggers;
    private String[] excludeLoggers;
    private int[] threadIDs;

    /**
     * default constructor, allows use of the set methods
     */
    public LogQueryBean() {}

    /**
     * gets the current value of the minimum time
     *
     * @return minimum time
     */
    public Date getMinTime() {
        return minTime;
    }

    /**
     * gets the current value of the maximum time
     *
     * @return maximum time
     */
    public Date getMaxTime() {
        return maxTime;
    }

    /**
     * sets the current value for the minimum and maximum time
     *
     * @param minTime minimum time
     * @param maxTime maximum time
     * @throws IllegalArgumentException if minTime is later than maxTime
     */
    public void setTime(Date minTime, Date maxTime) throws IllegalArgumentException {
        if (minTime != null && maxTime != null && minTime.after(maxTime)) {
            throw new IllegalArgumentException("Value of the minTime parameter should specify time before the time specified by the value of the maxTime parameter");
        }
        this.minTime = minTime;
        this.maxTime = maxTime;
    }

    /**
     * gets the current value of minimum level
     *
     * @return minimum level
     */
    public Level getMinLevel() {
        return minLevel;
    }

    /**
     * gets current value of maximum level
     *
     * @return maximum level
     */
    public Level getMaxLevel() {
        return maxLevel;
    }

    /**
     * sets the current value for the minimum and maximum levels
     *
     * @param minLevel minimum level
     * @throws IllegalArgumentException if minLevel is bigger than maxLevel
     */
    public void setLevels(Level minLevel, Level maxLevel) throws IllegalArgumentException {
        if (minLevel != null && maxLevel != null && minLevel.intValue() > maxLevel.intValue()) {
            throw new IllegalArgumentException("Value of the minLevel parameter should specify level not larger than the value of the maxLevel parametere.");
        }
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    /**
     * gets current array of message content search strings
     *
     * @return message content string (string to search message content for)
     */
    public String[] getMessageContent() {
        return messageContent;
    }

    /**
     * sets string array of message contents
     *
     * @param messageContent array of strings, representing content to search messages for
     * @throws IllegalArgumentException if string contains illegal patterns
     */
    public void setMessageContent(String[] messageContent) throws IllegalArgumentException {
        verifyPatterns(messageContent);
        this.messageContent = messageContent;
    }

    /**
     * gets the current array of messages to exclude (all regular expressions)
     *
     * @return exclude logger array
     */
    public String[] getExcludeMessages() {
        return excludeMessages;
    }

    /**
     * sets string array of messages to exclude in query
     *
     * @param excludeMessages array of strings, each being a regular expression search for messages to exclude
     * @throws IllegalArgumentException if strings contain illegal patterns
     */
    public void setExcludeMessages(String[] excludeMessages) throws IllegalArgumentException {
        verifyPatterns(excludeMessages);
        this.excludeMessages = excludeMessages;
    }

    /**
     * gets the current array of loggers to include (all regular expressions)
     *
     * @return include logger array
     */
    public String[] getIncludeLoggers() {
        return includeLoggers;
    }

    /**
     * sets string array of loggers to include in query
     *
     * @param includeLoggers array of strings, each being a regular expression search for loggers
     * @throws IllegalArgumentException if strings contain illegal patterns
     */
    public void setIncludeLoggers(String[] includeLoggers) throws IllegalArgumentException {
        verifyPatterns(includeLoggers);
        this.includeLoggers = includeLoggers;
    }

    /**
     * gets the current array of loggers to exclude (all regular expressions)
     *
     * @return exclude logger array
     */
    public String[] getExcludeLoggers() {
        return excludeLoggers;
    }

    /**
     * sets string array of loggers to exclude in query
     *
     * @param excludeLoggers array of strings, each being a regular expression search for loggers to exclude
     * @throws IllegalArgumentException if strings contain illegal patterns
     */
    public void setExcludeLoggers(String[] excludeLoggers) throws IllegalArgumentException {
        verifyPatterns(excludeLoggers);
        this.excludeLoggers = excludeLoggers;
    }

    private static void verifyPatterns(String[] patterns) throws IllegalArgumentException {
        if (patterns != null) {
            try {
                for (String pattern : patterns) { // Compile each string
                    compile(pattern);
                }
            } catch (PatternSyntaxException e) {
                throw new IllegalArgumentException("Pattern array contains an invalid regular expression", e);
            }
        }
    }

    /*
     * The method bellow is copied directly from com.ibm.ws.console.core.ConfigFileHelper
     * class to make string matching in WebUI log viewer compatible with the rest of the
     * WebSphere console. If that compatibility is required this method would need to be
     * adjusted each time corresponding method in ConfigFileHelper is changed.
     */
    /**
     * compiles pattern string into regular expression Pattern object.
     *
     * @param pattern string containing WebSphere console specific pattern
     * @return compiled version of the string
     * @throws IllegalArgumentException if pattern is null or contains an illegal pattern.
     */
    public static Pattern compile(String pattern) throws IllegalArgumentException {
        if (pattern == null) {
            throw new IllegalArgumentException("Pattern can not be null");
        }

        if (pattern.startsWith("/") && pattern.endsWith("/") && pattern.length() > 1) {
            // This is a regular expression
            // So create matcher as is
            try {
                return Pattern.compile(pattern.substring(1, pattern.length() - 1));
            } catch (PatternSyntaxException e) {
                throw new IllegalArgumentException("Pattern contains an invalid expression", e);
            }
        }

        StringBuilder rEPattern = new StringBuilder("^");

        for (int i = 0; i < pattern.length(); i++) {
            if (pattern.charAt(i) == '*')
                rEPattern.append('.');
            if (pattern.charAt(i) == '?') {
                rEPattern.append('.');
            } else {
                rEPattern.append(pattern.charAt(i));
            }
        }

        rEPattern.append("$");

        try {
            return Pattern.compile(rEPattern.toString());
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Pattern contains an invalid expression", e);
        }
    }

    /**
     * gets current array of threads to search for specifically
     *
     * @return array of threads in search
     */
    public int[] getThreadIDs() {
        return threadIDs;
    }

    /**
     * sets integer array of thread ids to search on
     *
     * @param threadIDs array of thread ids (null if not filtering on threadId)
     */
    public void setThreadIDs(int[] threadIDs) {
        this.threadIDs = threadIDs;
    }

    /**
     * sets string array, each string representing the hex value of a thread to search on
     *
     * @param threadIDs array of thread ids (hex) (null if not filtering on threadId)
     * @throws IllegalArgumentException if strings contain something other than hexadecimal values.
     */
    public void setThreadIDs(String[] threadIDs) throws IllegalArgumentException {
        int[] threads = null;
        if (threadIDs != null) {
            threads = new int[threadIDs.length];
            try {
                for (int i = 0; i < threads.length; i++) {
                    threads[i] = Integer.parseInt(threadIDs[i], 16);
                }
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Strings in the threadIDs array should be all in hexadecimal format");
            }
        }
        this.threadIDs = threads;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(excludeLoggers);
        result = prime * result + Arrays.hashCode(includeLoggers);
        result = prime * result
                 + ((maxLevel == null) ? 0 : maxLevel.hashCode());
        result = prime * result + ((maxTime == null) ? 0 : maxTime.hashCode());
        result = prime * result + Arrays.hashCode(messageContent);
        result = prime * result + Arrays.hashCode(excludeMessages);
        result = prime * result
                 + ((minLevel == null) ? 0 : minLevel.hashCode());
        result = prime * result + ((minTime == null) ? 0 : minTime.hashCode());
        result = prime * result + Arrays.hashCode(threadIDs);
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LogQueryBean other = (LogQueryBean) obj;
        if (!Arrays.equals(excludeLoggers, other.excludeLoggers))
            return false;
        if (!Arrays.equals(includeLoggers, other.includeLoggers))
            return false;
        if (maxLevel == null) {
            if (other.maxLevel != null)
                return false;
        } else if (!maxLevel.equals(other.maxLevel))
            return false;
        if (maxTime == null) {
            if (other.maxTime != null)
                return false;
        } else if (!maxTime.equals(other.maxTime))
            return false;
        if (!Arrays.equals(messageContent, other.messageContent))
            return false;
        if (!Arrays.equals(excludeMessages, other.excludeMessages))
            return false;
        if (minLevel == null) {
            if (other.minLevel != null)
                return false;
        } else if (!minLevel.equals(other.minLevel))
            return false;
        if (minTime == null) {
            if (other.minTime != null)
                return false;
        } else if (!minTime.equals(other.minTime))
            return false;
        if (!Arrays.equals(threadIDs, other.threadIDs))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "LogQueryBean [excludeLoggers=" + Arrays.toString(excludeLoggers) + ", includeLoggers=" + Arrays.toString(includeLoggers) + ", maxLevel=" + maxLevel + ", maxTime="
               + maxTime + ", messageContent=" + Arrays.toString(messageContent) + ", excludeMessages=" + Arrays.toString(excludeMessages) + ", minLevel=" + minLevel + ", minTime="
               + minTime + ", threadIDs=" + Arrays.toString(threadIDs)
               + "]";
    }
}
