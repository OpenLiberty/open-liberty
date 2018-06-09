/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.logging.hpel.reader.filters;

import java.util.regex.Pattern;

import com.ibm.websphere.logging.hpel.reader.LogQueryBean;
import com.ibm.websphere.logging.hpel.reader.LogRecordFilter;
import com.ibm.websphere.logging.hpel.reader.RepositoryLogRecord;

/**
 * filter class for all remote reader functionality. Note that an attempt is made to do all heavy processing in the
 * constructor so that the per-record invocations go as quickly as possible.
 */
public class MultipleCriteriaFilter implements LogRecordFilter {
    private boolean checkDate = false; // To support start/stop dates for single instance, this is needed

    private LevelFilter levelFilter = null;
    private Pattern[] includeLoggers; // Regular expressions gsub'd and compiled for fast compares
    private Pattern[] excludeLoggers;
    private final int[] threadIDs; // ThreadIds converted from hex string to ints to match record
    private Pattern[] messageContent; // MessageContent is seen as a string that matches some content in the record
    private Pattern[] excludeMessages;
    private long endDate; // endDate to check if needed
    private long startDate; // startDate to check if needed

    /**
     * construct the filter for the read API
     *
     * @param logQueryBean bean/object with all query information
     */
    public MultipleCriteriaFilter(LogQueryBean logQueryBean) {
        if (logQueryBean.getMinTime() != null || logQueryBean.getMaxTime() != null) {
            checkDate = true;
            startDate = logQueryBean.getMinTime() != null ? logQueryBean.getMinTime().getTime() : 0;
            endDate = logQueryBean.getMaxTime() != null ? logQueryBean.getMaxTime().getTime() : Long.MAX_VALUE;
        }
        if (logQueryBean.getMinLevel() != null || logQueryBean.getMaxLevel() != null) {
            levelFilter = new LevelFilter(logQueryBean.getMinLevel(), logQueryBean.getMaxLevel());
        }
        if (logQueryBean.getMessageContent() != null) {
            messageContent = compile(logQueryBean.getMessageContent());
        }
        if (logQueryBean.getExcludeMessages() != null) {
            excludeMessages = compile(logQueryBean.getExcludeMessages());
        }
        if (logQueryBean.getIncludeLoggers() != null) {
            includeLoggers = compile(logQueryBean.getIncludeLoggers());
        }
        if (logQueryBean.getExcludeLoggers() != null) {
            excludeLoggers = compile(logQueryBean.getExcludeLoggers());
        }
        threadIDs = logQueryBean.getThreadIDs();
    }

    private Pattern[] compile(String[] patterns) {
        Pattern[] result = null;
        if (patterns != null) {
            result = new Pattern[patterns.length];
            for (int i = 0; i < patterns.length; i++) {
                // Don't need to catch exception here since LogQueryBean won't
                // let illegal pattern to be set in its attributes.
                result[i] = LogQueryBean.compile(patterns[i]);
            }
        }
        return result;
    }

    /**
     * filter current record per criteria passed in. Date filtering done prior to filter invocation
     * any failure of a criteria results in a false return (ie: don't accept record)
     *
     * @param record RepositoryLogRecord to filter
     * @return true or false as to keeping this record
     */
    @Override
    public boolean accept(RepositoryLogRecord record) {
        if (checkDate) {
            if (record.getMillis() > endDate || startDate > record.getMillis())
                return false;
        }
        if (levelFilter != null && !levelFilter.accept(record)) {
            return false;
        }
        String message = (record.getFormattedMessage() != null) ? record.getFormattedMessage() : record.getRawMessage();
        if (this.messageContent != null) { // If messageContentChecking, get proper field and do contains
            boolean match = false;
            for (Pattern pattern : this.messageContent) {
                if (pattern.matcher(message).find()) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                return false;
            }
        }
        if (this.excludeMessages != null) { // If excludeMessages checking, any match means record does not meet criteria and not to include
            boolean matchExcMessage = false;
            for (Pattern excMessage : this.excludeMessages) {
                if (excMessage.matcher(message).find()) {
                    matchExcMessage = true;
                    break;
                }
            }
            if (matchExcMessage) {
                return false;
            }
        }
        String loggerName = record.getLoggerName();
        if (this.includeLoggers != null) { // If includeLogger checking, look for a match to any in the array
            boolean matchIncLogger = false;
            for (Pattern incLogger : this.includeLoggers) {
                if (loggerName != null && incLogger.matcher(loggerName).find()) {
                    matchIncLogger = true;
                    break;
                }
            }
            if (!matchIncLogger)
                return false;
        }
        if (this.excludeLoggers != null) { // If excludeLogger checking, any match means record does not meet criteria
            for (Pattern excLogger : this.excludeLoggers) {
                if (loggerName != null && excLogger.matcher(loggerName).find())
                    return false;
            }
        }
        if (this.threadIDs != null) { // If thread checking, simple int equality on anything in array passes test
            for (int hexThread : this.threadIDs) {
                if (hexThread == record.getThreadID())
                    return true;
            }
            return false;
        }
        return true;
    }

}
