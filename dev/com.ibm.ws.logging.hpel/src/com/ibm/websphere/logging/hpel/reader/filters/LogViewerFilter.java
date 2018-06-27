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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.ibm.websphere.logging.hpel.reader.LogQueryBean;
import com.ibm.websphere.logging.hpel.reader.LogRecordFilter;
import com.ibm.websphere.logging.hpel.reader.RepositoryLogRecord;

/**
 * Implementation of the {@link LogRecordFilter} interface using multiple
 * parameters to filter log records.
 *
 * @ibm-api
 */
public class LogViewerFilter implements LogRecordFilter {

    private static final String CLASS_NAME = LogViewerFilter.class.getName();
    private static final Logger log = Logger.getLogger(CLASS_NAME);
    private Level minLevel = null;
    private Level maxLevel = null;
    private Date startDate = null;
    private Date stopDate = null;
    private String includeLoggers = null; // a comma separated list of loggers
    // to include
    private String excludeLoggers = null; // a comma separated list of loggers
    // to exclude
    private String[] inLoggers = null;
    private String[] exLoggers = null;
    private int intThreadID = 0;
    private Pattern messageRegExp;
    private String excludeMessages = null; // a comma separated list of messages
    // to exclude
    private final List<Pattern> excludeMessagesRegExpPatterns = new ArrayList<Pattern>();
    private final List<String> includeExtensionRegExpKeys = new ArrayList<String>();
    private final List<Pattern> includeExtensionRegExpPatterns = new ArrayList<Pattern>();

    // filter flags, if true include as filter criteria
    private boolean filterLevel = false;
    private boolean filterTime = false;
    private boolean filterLoggers = false;
    private boolean filterThread = false;
    private boolean filterMessage = false;

    public final static class Extension {
        final String key;
        final String value;

        public Extension(StringBuilder key, StringBuilder value) throws IllegalArgumentException {
            if (value == null) {
                throw new IllegalArgumentException("Value can not be 'null'");
            }
            if (key == null) {
                this.key = value.toString();
                this.value = null;
            } else {
                this.key = key.toString();
                this.value = value.toString();
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(key).append("=").append(value);
            return sb.toString();
        }
    }

    final private Extension[] extensions;

    /**
     * Creates instance to filter on time range, level range, lists of
     * included/excluded loggers, list of excluded messages, and thread ID. A
     * <code>null</code> value can be supplied for any of the parameters that
     * you do not wish to filter on.
     *
     * @param startDate minimum {@link Date} value that will be accepted by
     *            the filter
     * @param stopDate maximum {@link Date} value that will be accepted by
     *            the filter
     * @param minLevel minimum {@link Level} that will be accepted by the
     *            filter
     * @param maxLevel maximum {@link Level} that will be accepted by the
     *            filter
     * @param includeLoggers comma separated list of loggers that will be
     *            accepted by the filter. The wildcard character '*' may be used.
     * @param excludeLoggers comma separated list of loggers that will be
     *            rejected by the filter. The wildcard character '*' may be used.
     * @param threadID ID of the thread that will be accepted by the filter
     * @param message message string to match in the message of a record. The
     *            wildcard character '*' may be used.
     * @param excludeMessages comma separated list of messages that will be
     *            rejected by the filter. The wildcard character '*' may be used.
     * @param extensions comma separated list of records with specified extension
     *            name and value that will be accepted by the filter. The wildcard
     *            characters '*' or '?' may be used.
     */
    public LogViewerFilter(Date startDate, Date stopDate, Level minLevel, Level maxLevel, String includeLoggers,
                           String excludeLoggers, String threadID, String message, String excludeMessages, List<Extension> extensions) {
        String methodName = "<init>";
        if ((startDate != null) || (stopDate != null)) {
            if (startDate != null) {
                this.startDate = (Date) startDate.clone();
            }
            if (stopDate != null) {
                this.stopDate = (Date) stopDate.clone();
            }
            filterTime = true;
        }
        if ((minLevel != null) || (maxLevel != null)) {
            if (minLevel != null) {
                this.minLevel = minLevel;
            }
            if (maxLevel != null) {
                this.maxLevel = maxLevel;
            }
            filterLevel = true;
        }
        if ((includeLoggers != null) || (excludeLoggers != null)) {
            if (includeLoggers != null) {
                this.includeLoggers = includeLoggers;
                inLoggers = includeLoggers.split(",");
                for (int i = 0; i < inLoggers.length; i++) {
                    inLoggers[i] = inLoggers[i].trim();
                }
            }
            if (excludeLoggers != null) {
                this.excludeLoggers = excludeLoggers;
                exLoggers = excludeLoggers.split(",");
                for (int i = 0; i < exLoggers.length; i++) {
                    exLoggers[i] = exLoggers[i].trim();
                }
            }
            filterLoggers = true;
        }
        if (threadID != null) {
            try {
                intThreadID = Integer.parseInt(threadID, 16); //convert from hex
                filterThread = true;
            } catch (NumberFormatException e) {
                if (log.isLoggable(Level.FINE)) {
                    log.logp(Level.FINE, CLASS_NAME, methodName, "Unable to parse thread ID. Filtering by thread is disabled");
                }
                filterThread = false;
            }
        }
        if (message != null) {
            try {
                this.messageRegExp = getRegularExpression(message);
                filterMessage = true;
            } catch (IllegalArgumentException ae) {
                if (log.isLoggable(Level.FINE)) {
                    log.logp(Level.FINE, CLASS_NAME, methodName, "The message option " + message + " contains an invalid expession. The message filtering is disabled.");
                }
            }
        }
        if (excludeMessages != null) {
            this.excludeMessages = excludeMessages;
            String[] exMessages = excludeMessages.split(",");
            for (int i = 0; i < exMessages.length; i++) {
                exMessages[i] = exMessages[i].trim();
                this.excludeMessagesRegExpPatterns.add(getRegularExpression(exMessages[i]));
            }
            filterMessage = true;
        }
        if (extensions != null && extensions.size() > 0) {
            this.extensions = extensions.toArray(new Extension[extensions.size()]);
            for (Extension ext : this.extensions) {
                this.includeExtensionRegExpKeys.add(ext.key);
                this.includeExtensionRegExpPatterns.add(getRegularExpression(ext.value));
            }
        } else {
            this.extensions = null;
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.logging.hpel.reader.api.LogRecordFilter#accept(com.ibm.ws.
     * logging.hpel.reader.api.RepositoryLogRecord)
     */
    @Override
    public boolean accept(RepositoryLogRecord record) {
        /*
         * Logic operates under assumption that record will be accepted by default, therefore
         * filtering check if record should be excluded
         */

        // Filtering by threadID
        if (filterThread) {
            if (intThreadID != record.getThreadID()) {
                return false; // no need to continue, this record does not pass
            }
        }

        // Filtering by level
        if (filterLevel) {
            if (minLevel != null) {
                if (record.getLevel().intValue() < minLevel.intValue()) {
                    return false; // no need to continue, this record does not pass
                }
            }
            if (maxLevel != null) {
                if (record.getLevel().intValue() > maxLevel.intValue()) {
                    return false; // no need to continue, this record does not pass
                }
            }
        }

        // Filtering by Date/Time
        if (filterTime) {
            Date recordDate = new Date();
            recordDate.setTime(record.getMillis());
            if (startDate != null) {
                if (recordDate.before(startDate)) {
                    return false; // no need to continue, this record does not
                    // pass
                }
            }
            if (stopDate != null) {
                if (recordDate.after(stopDate)) {
                    return false; // no need to continue, this record does not
                    // pass
                }
            }
        }

        // Filtering by Logger
        if (filterLoggers) {
            /*
             * Can't return false at first filter out in case of loggers cause it's possible
             * that a more specific include could over-ride the exclude. So we check them all -
             * and track the highest include and exclude matches. If there are no matches for
             * either include or exclude default is to exclude it.
             */
            int excludeMatchLevel = -1;
            int includeMatchLevel = -1;

            if (excludeLoggers != null) {
                for (String x : exLoggers) {
                    if (record.getLoggerName() != null && record.getLoggerName().matches(x.replace("*", ".*"))) {
                        int tmpInt = x.split("\\.").length;
                        if (tmpInt > excludeMatchLevel)
                            // This match is higher than any previous matches
                            excludeMatchLevel = tmpInt;
                    }
                }
            } else
                excludeMatchLevel = 0; // Equivalent of excludeLoggers="*"

            if (includeLoggers != null) {
                for (String x : inLoggers) {
                    if (record.getLoggerName() != null && record.getLoggerName().matches(x.replace("*", ".*"))) {
                        int tmpInt = x.split("\\.").length;
                        if (tmpInt > includeMatchLevel)
                            // This match is higher than any previous matches
                            includeMatchLevel = tmpInt;
                    }
                }
            } else
                includeMatchLevel = 0; // Equivalent of includeLoggers="*"

            // Only want to accept the record if it matches an include, and does
            // not match an exclude (of greater value if it matches both).
            if (includeLoggers == null) {
                // default include is *
            }

            // Using equals on match level, so default will be to exclude a record if user has
            // defined both include and exclude Logger criteria and we did not match on either
            // case
            if (includeMatchLevel <= excludeMatchLevel)
                return false;
        }

        // Filtering by message
        if (filterMessage) {
            String recordMessage = (record.getFormattedMessage() != null) ? record.getFormattedMessage() : record.getRawMessage();

            if (recordMessage == null) {
                return false;
            }

            recordMessage = recordMessage.replace("\n", "").replace("\r", "");

            if (messageRegExp != null) {
                boolean foundmessagekey = messageRegExp.matcher(recordMessage).find();
                if (!foundmessagekey) {
                    return false;
                }
            }

            if (excludeMessages != null) {
                for (Pattern p : excludeMessagesRegExpPatterns) {
                    boolean excludeMsg = p.matcher(recordMessage).find();
                    if (excludeMsg) {
                        // The Log record message will be excluded from the Log viewer.
                        return false;
                    }
                }
            }

        }

        // Filtering by extension
        if (extensions != null) {
            boolean match = false;
            for (Extension extension : extensions) {
                String value = record.getExtension(extension.key);
                if (value != null) {

                    if (extension.value == null || includeExtensionRegExpPatterns.get(includeExtensionRegExpKeys.indexOf(extension.key)).matcher(value).find()) {
                        match = true;
                        break;
                    }
                }
            }
            if (!match) {
                return false;
            }
        }

        // if we made it this far, then the record should be good.
        return true;
    }

    private Pattern getRegularExpression(String rawString) throws IllegalArgumentException {
        return LogQueryBean.compile(rawString);
    }

}
