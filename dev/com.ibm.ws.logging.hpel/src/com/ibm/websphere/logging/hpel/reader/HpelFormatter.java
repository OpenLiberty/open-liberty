/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.logging.hpel.reader;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import com.ibm.ejs.ras.hpel.Messages;
import com.ibm.ws.logging.hpel.FormatSet;
import com.ibm.ws.logging.hpel.impl.LogRecordWrapper;
import com.ibm.ws.logging.internal.WsLogRecord;

/**
 * Abstract class for formatters used to convert HPEL log records into a formatted string output. This class
 * should be used for getting formatter instances.
 *
 * @ibm-api
 */
public abstract class HpelFormatter {

    /** <Q>Basic</Q> type format used in legacy text log files */
    public static final String FORMAT_BASIC = "Basic";

    /** <Q>Advanced</Q> type format used in legacy text log files */
    public static final String FORMAT_ADVANCED = "Advanced";

    /** Common Base Event Format */
    public static final String FORMAT_CBE101 = "CBE-1.0.1";

    /** Json Format */
    public static final String FORMAT_JSON = "Json";

    protected String lineSeparator = getProperty("line.separator");
    protected Properties headerProps = new Properties(); // 660484

    /** Locale this formatters uses for record messages and date formatting */
    protected Locale locale = null;
    protected TimeZone timeZone = TimeZone.getDefault();
    protected DateFormat dateFormat = FormatSet.customizeDateFormat(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM));

    protected static final String nullParamString = "null"; // <null> // for consistency with message formatting
    private static final String svEmptyString = "";
    private static final String svEmptyStringReplacement = "\"\"";

    /**
     * Returns the HpelFormatter subclass instance to be used for formatting a RepositoryLogRecord. The formatter returned
     * is based on the formatStyle requested.
     *
     * @param formatStyle the style of the HpelFormatter instance to be returned. Valid values must match (case-insensitive) {@link #FORMAT_BASIC}, {@link #FORMAT_ADVANCED}, or
     *            {@link #FORMAT_CBE101}
     *
     * @return a HpelFormatter instance that matches the requested formatter style
     *
     * @throws IllegalArgumentException when the formatterName is invalid due to a <code>null</code> value passed or a formatter name
     *             that is not recognized
     *
     * @see TimeZone
     */
    public static HpelFormatter getFormatter(String formatStyle) {
        if (formatStyle != null && !"".equals(formatStyle)) {
            if (formatStyle.equalsIgnoreCase(FORMAT_BASIC)) {
                return new HpelBasicFormatter();
            } else if (formatStyle.equalsIgnoreCase(FORMAT_ADVANCED)) {
                return new HpelAdvancedFormatter();
            } else if (formatStyle.equalsIgnoreCase(FORMAT_CBE101)) {
                return new HpelCBEFormatter();
            } else if (formatStyle.equalsIgnoreCase(FORMAT_JSON)) {
                return new HpelJsonFormatter();
            }
        }

        throw new IllegalArgumentException(formatStyle + " is not a valid formatter style");
    }

    /**
     * @param string
     * @return
     */
    private String getProperty(String string) {
        final String temp = string;
        try {
            String prop = AccessController.doPrivileged(
                                                        new PrivilegedAction<String>() {
                                                            @Override
                                                            public String run() {
                                                                return System.getProperty(temp);
                                                            }
                                                        });
            return prop;
        } catch (SecurityException se) {
            // LOG THE EXCEPTION
            return null;
        }
    }

    /**
     * Custom level extending standard set provided by java.util.logging.Level class
     */
    private static class CustomLevel extends Level {
        private static final long serialVersionUID = 3927638037228330703L;

        private CustomLevel(String name, int level, String resourceBundleName) {
            super(name, level, resourceBundleName);
        }
    };

    /**
     * Map between level and it's one character id.
     */
    protected final static Map<Level, String> customLevels = new HashMap<Level, String>();

    /**
     * Adds extra level recognizable by this formatter and Level.parse()
     *
     * @param name name of the level
     * @param intValue integer value of the level
     * @param id one character id to use for this level. If it's <code>null</code> first character of the level name will be used
     * @param resourceBundleName resource bundle to use for level name translation
     * @return newly generated level instance
     */
    public static Level addCustomLevel(String name, int intValue, String id, String resourceBundleName) {
        if (name == null) {
            throw new IllegalArgumentException("Parameter name can not be 'null' in this call.");
        }
        // Don't add duplicate entries but allow different names for the same level and the same name for different levels.
        Level level = null;
        try {
            level = Level.parse(name);
        } catch (IllegalArgumentException ex) {
            // Assume that there's no such level.
        }
        // If there's no such level or it has different integer value create a new one.
        if (level == null || level.intValue() != intValue) {
            level = new CustomLevel(name, intValue, resourceBundleName);
        }
        addCustomLevel(level, id);
        return level;
    }

    /**
     * Adds level to be treated specially by this formatter.
     *
     * @param level level to be added to the special list
     * @param id one character id to use for this level. If it's <code>null</code> first character of the level name will be used
     */
    public static void addCustomLevel(Level level, String id) {
        if (level == null) {
            throw new IllegalArgumentException("Parameter level can not be 'null' in this call");
        }
        if (id == null) {
            id = level.getName().substring(0, 1);
        }
        customLevels.put(level, id);
    }

    /**
     * Helper class representing one line of the customary defined header.
     */
    protected class CustomHeaderLine {
        private final String pattern;
        private final ArrayList<String[]> vars = new ArrayList<String[]>();

        private CustomHeaderLine(String line) {
            StringBuilder sb = new StringBuilder();
            int index = 0;
            int start = line.indexOf('{');
            int rest = 0;
            while (start >= 0) {
                int end = line.indexOf('}', start);
                if (end < 0) {
                    break;
                }
                if (end - start > 2) {
                    String var = line.substring(start + 1, end).trim();
                    if (!var.isEmpty()) {
                        vars.add(var.split("\\s*\\|\\s*"));
                        sb.append(line.substring(rest, start + 1));
                        sb.append(Integer.toString(index++));
                        rest = end;
                    }
                }
                start = line.indexOf('{', end);
            }
            sb.append(line.substring(rest));
            pattern = sb.toString();
        }

        /**
         * Return header line with variable parameters substituted with their values in the header.
         * Variables used in the substitution are removed from the header set.
         *
         * @param header set of header properties
         * @return header line or <code>null</code> if one of the parameters is missing from the header.
         */
        public String formatLine(Properties header) {
            if (vars.size() == 0) {
                return pattern;
            }

            // Collect values for all variables.
            Object[] args = new Object[vars.size()];
            int index = 0;
            for (String[] varList : vars) {
                String value = null;
                for (String var : varList) {
                    value = header.getProperty(var);
                    if (value != null && !value.isEmpty()) {
                        args[index++] = value;
                        break;
                    }
                }
                // A value necessary for variable substitution is missing, can't provide the line.
                if (value == null || value.isEmpty()) {
                    return null;
                }
            }

            return MessageFormat.format(pattern, args);
        }
    }

    /**
     * Set of lines to use in the custom header.
     */
    protected CustomHeaderLine[] customHeader = {};

    /**
     * sets new customer header format specification
     *
     * @param header array of Strings containing optional parameters with name of the header value to
     *            use in them. For example: <Q>Java version: {java.version}</Q>.
     */
    public void setCustomHeader(String[] header) {
        if (header == null) {
            throw new IllegalArgumentException("Custom header can't be null.");
        }
        customHeader = new CustomHeaderLine[header.length];
        for (int i = 0; i < header.length; i++) {
            customHeader[i] = new CustomHeaderLine(header[i]);
        }
    }

    /**
     * Returns the ID of the time zone to be used in the formatted log record header timestamp
     *
     * @return time zone ID
     *
     * @see TimeZone
     *
     */
    public String getTimeZoneID() {
        return timeZone.getID();
    }

    /**
     * Sets the ID of the time zone to be used in the formatted log record header timestamp
     *
     * @param timeZoneId new timeZoneID attribute value.
     *
     * @throws IllegalArgumentException when the time zone ID is not valid.
     */
    public void setTimeZoneID(String timeZoneId) {
        if (verifyTimeZoneID(timeZoneId)) {
            this.timeZone = TimeZone.getTimeZone(timeZoneId);
        } else {
            throw new IllegalArgumentException(timeZoneId + " is not a valid time zone");
        }

    }

    /**
     * Returns the formatter's locale that will be used to localize the log record's date timestamp and message.
     *
     * @return the locale that will be used to localize the log record
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Sets the formatter locale and the dateFormat that will be used to localize a log record being formatted. The formatter locale will be used
     * when {@link #formatRecord(RepositoryLogRecord)} is invoked. It is possible to format a log record with a locale other than
     * one set by this method using {@link #formatRecord(RepositoryLogRecord, Locale)}. the formatter locale will be set to the system
     * locale until this method gets invoked. The dateFormat can be either the default format or the ISO-8601 format.
     *
     * @param locale the Locale to be used for localizing the log record, <code>null</code> to disable message localization.
     * @param flag to use ISO-8601 date format for output.
     */
    public void setDateFormat(Locale locale, boolean isoDateFormat) {
        this.locale = locale;
        if (null == locale) {
            dateFormat = FormatSet.customizeDateFormat(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM), isoDateFormat);
        } else {
            dateFormat = FormatSet.customizeDateFormat(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale), isoDateFormat);
        }
    }

    /**
     * Sets the Properties that is used to populate the formatter's header. This Properties used in the
     * parameter should be a Properties returned from invoking ServerInstanceLogRecordList.getHeader().
     *
     * @see ServerInstanceLogRecordList
     */
    public void setHeaderProps(Properties properties) {
        if (properties == null) {
            throw new IllegalArgumentException("Argument 'sysProps' cannot be null.");
        }
        this.headerProps = properties;
    }

    /**
     * Returns the Properties that is used to populate the formatter's header.
     */
    public Properties getHeaderProps() {
        return headerProps;
    }

    /**
     * Returns the formatted version of the record's message in a specified
     * locale with any place holder parameters substituted with values.
     * <p>
     * In cases where localization is successful, the formatted message
     * is the localized message with any place holder values filled in with
     * corresponding parameter values.
     * <p>
     * In cases where the localized message is <code>null</code>, the formatted message
     * is the raw message with any place holder values filled in with
     * corresponding parameter values.
     * <p>
     * This is the same behavior as found in {@link java.util.logging.LogRecord#getMessage()}
     *
     * @param record log record to read the message from.
     * @param tmpLocale locale to translate message into.
     * @return the fully localized and formatted message.
     * @see java.util.logging.LogRecord#getMessage()
     */
    public String formatMessage(RepositoryLogRecord record, Locale tmpLocale) {
        // no localization case (CBE or non-CBE)
        if (record.getLocalizable() == WsLogRecord.REQUIRES_NO_LOCALIZATION) {
            String traceString = record.getRawMessage(); // LIDB2667.13 start
            Object[] parms = record.getParameters();

            return formatUnlocalized(traceString, parms); // LIDB2667.13 end
        }

        if (tmpLocale == null) {
            tmpLocale = locale;
        }

        String localizedMsg = null;
        // If no locale specified or it's the same we have translation for already - use translated string.
        if (tmpLocale == null || tmpLocale.toString().equals(record.getMessageLocale())) {
            localizedMsg = record.getLocalizedMessage();
        }
        // if we can't use translated string translate message now.
        if (localizedMsg == null) {
            localizedMsg = translateMessage(record, tmpLocale);
        }

        // If message is empty no localization or formating is necessary.
        if (localizedMsg == null) {
            return "";
        }

        // sub values from parameters if needed.
        try {
            localizedMsg = MessageFormat.format(localizedMsg, record.getParameters());
        } catch (IllegalArgumentException iae) {
            // some trace messages have {} in them that are not parameters.
            // consume exception and don't format.
        }

        return localizedMsg;
    }

    // D233515.1 - added method
    /**
     * Returns an array of parameters that matches the input parms except:
     * Parameters that are empty strings are converted to </Q></Q> (2 double quotes)
     *
     * @param parms
     * @return Object[]
     */
    public static Object[] convertParameters(Object[] parms) {

        if (parms == null)
            return null;

        if (parms.length == 0)
            return parms;

        // create a new array to hold the output
        Object[] newParms = new Object[parms.length];

        // convert empty strings to ""
        for (int i = 0; i < parms.length; i++) {
            if (parms[i] instanceof String && ((String) parms[i]).equals(svEmptyString)) {
                newParms[i] = svEmptyStringReplacement;
            } else {
                newParms[i] = parms[i];
            }
        }

        return newParms;
    }

    /**
     * Used for WsLogRecords or CommonBaseEventLogRecords that specify REQUIRES_NO_LOCALIZATION,
     *
     * tries to format, and if unsuccessful in using any parameters appends them per unusedParmHandling.
     *
     * @return the formatted trace
     */
    protected String formatUnlocalized(String traceString, Object[] parms) { // added for LIDB2667.13
        //
        // handle messages that require no localization (essentially trace)
        //

        // get ready to append parameters
        Object[] newParms = convertParameters(parms); //D233515.1

        if (newParms == null)
            return traceString;
        else {
            String formattedTrace;

            if (traceString.indexOf('{') >= 0) {
                formattedTrace = Messages.getFormattedMessageFromLocalizedMessage(traceString, newParms, true);
            } else {
                formattedTrace = traceString;
            }

            if (formattedTrace.equals(traceString)) {
                // parms weren't used -- append them so they aren't lost
                return appendUnusedParms(traceString, newParms);
            } else
                return formattedTrace;
        }
    }

    /**
     * Add parameters to the end of a message based on policy indicated by unusedParmHandling
     * parameter.
     *
     * @param message Message to append arguments onto
     * @param args Arguments to append to message
     * @return String new copy of message with parameters appended, or message itself if nothing was appended
     */
    protected abstract String appendUnusedParms(String message, Object[] args);

    /**
     * @param record log record to read the message from.
     * @param tmpLocale locale to translate message into.
     * @return the fully localized message.
     */
    public static String translateMessage(RepositoryLogRecord record, Locale tmpLocale) {
        if (null == record) {
            throw new IllegalArgumentException("Record cannot be null");
        }

        String rawMessage = record.getRawMessage();

        // If not localization required or no associated resource bundle use raw message directly.
        if (rawMessage == null || tmpLocale == null || record.getLocalizable() == WsLogRecord.REQUIRES_NO_LOCALIZATION ||
            record.getResourceBundleName() == null) {
            return rawMessage;
        } else {
            String msgKey = rawMessage.replace(' ', '.');
            return Messages.getStringFromBundle(record.getResourceBundleName(), msgKey, tmpLocale, rawMessage);
        }

    }

    /**
     * Formats a RepositoryLogRecord using the formatter's locale
     *
     * @param record log record to be formatted
     * @return the resulting formatted string output.
     */
    public String formatRecord(RepositoryLogRecord record) {
        if (null == record) {
            throw new IllegalArgumentException("Record cannot be null");
        }
        return formatRecord(record, (Locale) null);
    }

    /**
     * Formats a RepositoryLogRecord using the locale specified.
     *
     * @param record the RepositoryLogRecord to be formatted
     * @param locale the locale to use when formatting this record.
     *
     * @return the formatted RepositoryLogRecord as string output.
     */
    public abstract String formatRecord(RepositoryLogRecord record, Locale locale);

    /**
     * Formats a LogRecord using the formatter's locale
     *
     * @param record log record to be formatted
     * @return the resulting formatted string output.
     */
    public String formatRecord(LogRecord record) {
        return formatRecord(new LogRecordWrapper(record));
    }

    /**
     * Gets the formatter's header. This method will return an empty array if the
     * formatter does not have a header as part of the formatter's format style
     *
     * return the header as an array of strings
     */
    public abstract String[] getHeader();

    /**
     * Gets the formatter's footer. This method will return an empty string if the formatter
     * does not have a footer as part of the formatter's format style.
     *
     * @return the footer as a string
     */
    public abstract String getFooter();

    protected static boolean verifyTimeZoneID(String timeZoneID) {
        if (timeZoneID != null) {
            TimeZone gmt = TimeZone.getTimeZone("GMT");
            // If timeZoneID is and id of the "GMT" time zone it's a legal time zone
            if (timeZoneID.equals(gmt.getID())) {
                return true;
            }
            // If timeZoneID is a legal time zone it would result in other than "GMT" time zone
            TimeZone tzId = TimeZone.getTimeZone(timeZoneID);
            return !gmt.equals(tzId);

        }
        return false;
    }

    protected static void formatThreadID(RepositoryLogRecord r, StringBuilder buffer) {
        // add the 8 (hex) digit thread ID
        for (int i = Integer.toHexString(r.getThreadID()).length(); i < 8; i++) {
            buffer.append('0');
        }
        buffer.append(Integer.toHexString(r.getThreadID()));
        buffer.append(" ");
    }

    public String getLineSeparator() {
        return lineSeparator;
    }

    public void setLineSeparator(String lineSeparator) {
        this.lineSeparator = lineSeparator;
    }

    public void setStartDatetime(long headerStartDatetime) {

    }

}
