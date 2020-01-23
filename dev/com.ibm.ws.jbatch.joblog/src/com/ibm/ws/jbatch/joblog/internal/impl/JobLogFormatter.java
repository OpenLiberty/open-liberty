/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.joblog.internal.impl;

import java.lang.reflect.Array;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import com.ibm.ejs.ras.Traceable;
import com.ibm.ejs.ras.Untraceable;
import com.ibm.websphere.logging.WsLevel;
import com.ibm.websphere.ras.DataFormatHelper;
import com.ibm.websphere.ras.TruncatableThrowable;
import com.ibm.websphere.ras.annotation.Trivial;

@Trivial
public class JobLogFormatter extends Formatter {

    public static final String banner = "********************************************************************************";

    public static final Object NULL_ID = null;
    public static final String NULL_FORMATTED_MSG = null;

    static final int basicNameLength = 13;
    static final int enhancedNameLength = 60;
    static final int MAX_DATA_LENGTH = 1024 * 16;

    static final String nl = System.getProperty("line.separator");
    static final String pad8 = "        ";
    static final String basicPadding = "                                 ";
    static final String advancedPadding = "          ";
    static final String enhancedPadding = "                                                                                                               ";

    static final String nullParamString = "null"; // consistency with message formatting
    static final String badParamString = "<malformed parameter>";
    static final String emptyString = "";
    static final String emptyStringReplacement = "\"\"";
    static final String ENTRY = "Entry ";
    static final String EXIT = "Exit ";

    /**
     * Array used to convert integers to hex values
     */
    private static final char hexChars[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    private static final String NAME_FORMAT = "[%-8s] ";

    enum LevelFormat {
        FATAL(WsLevel.FATAL, " F "),
        ERROR(WsLevel.ERROR, " E "),
        WARNING(Level.WARNING, " W "),
        AUDIT(WsLevel.AUDIT, " A "),
        INFO(Level.INFO, " I "),
        CONFIG(Level.CONFIG, " C "),
        DETAIL(WsLevel.DETAIL, " D "),
        EVENT(WsLevel.FINE, " 1 "),
        FINE(Level.FINE, " 1 "),
        FINER(Level.FINER, " 2 "),
        FINEST(Level.FINEST, " 3 ");

        final Level level;
        final int id;
        final String marker;

        LevelFormat(Level l, String s) {
            level = l;
            id = l.intValue();
            marker = s;
        }

        public static LevelFormat findFormat(Level level) {
            int levelId = level.intValue();
            for (LevelFormat f : LevelFormat.values()) {
                if (level == f.level)
                    return f;
                else if (levelId == f.id) {
                    return f;
                }
            }
            return null;
        }
    }

    private static final String N_INFO = String.format(NAME_FORMAT, Level.INFO.getLocalizedName()),
                    N_AUDIT = String.format(NAME_FORMAT, WsLevel.AUDIT.getLocalizedName()),
                    N_EVENT = String.format(NAME_FORMAT, WsLevel.EVENT.getLocalizedName()),
                    N_WARN = String.format(NAME_FORMAT, Level.WARNING.getLocalizedName()),
                    N_ERROR = String.format(NAME_FORMAT, WsLevel.ERROR.getLocalizedName()),
                    N_FATAL = String.format(NAME_FORMAT, WsLevel.FATAL.getLocalizedName());

    public static final String levelToString(Level level) {
        if (level != null) {
            int l = level.intValue();

            if (l == WsLevel.FATAL.intValue())
                return N_FATAL;
            if (l == WsLevel.ERROR.intValue())
                return N_ERROR;
            if (l == Level.WARNING.intValue())
                return N_WARN;
            if (l == WsLevel.AUDIT.intValue())
                return N_AUDIT;
            if (l == Level.INFO.intValue())
                return N_INFO;

            if (level == WsLevel.EVENT)
                return N_EVENT;
        }
        return "";
    }

    /**
	 */
    public JobLogFormatter() {}

    /**
     * Format the given log record and return the formatted string.
     * The resulting formatted String will normally include a localized and formated version of
     * the LogRecord's message field. The Formatter.formatMessage convenience method can (optionally) be
     * used to localize and format the message field.
     * 
     * @param record the log record to be formatted.
     * @return the formatted log record
     */
    @Override
    public String format(LogRecord r) {
        String detail = getDetailString(r);
        String text = formatMessage(r);

        return createFormattedString(r, NULL_ID, text, detail);
    }

    /**
     * Finds any JobLogEntryDetail objects in the log record parameters, converts them
     * to a String, and removes them from the list of parameters.
     * 
     * @param r
     * @return
     */
    private String getDetailString(LogRecord record) {
        String result = "";
        Object[] logParams = record.getParameters();
        if (logParams != null && logParams.length > 0) {
            for (int i = 0; i < logParams.length; i++) {
                if (logParams[i] instanceof JobLogEntryDetail) {
                    result = result.concat(logParams[i].toString());
                    record.setParameters(removeArg(logParams, i));
                }
            }
        }
        return result;
    }

    /**
     * {@inheritDoc} <br />
     * We override this method because in some JVMs, it is synchronized (why on earth?!?!).
     */
    @Override
    public String formatMessage(LogRecord logRecord) {
        return formatMessage(logRecord, logRecord.getParameters(), true);
    }

    /**
     * Get the "formatted" message text.
     * <p>
     * If useResourceBundle is true, it will look in the resource bundle for
     * the message text (as a key), and will then use MessageFormat.format on
     * the returned string if there are any substitutions in the string, e.g. {0}.
     * <p>
     * If useResourceBundle is false, we're calling this for a trace-level (untranslated)
     * message: just construct the trace text by appending the result of
     * formatObj(...) to the log record message.
     * 
     * @param logRecord
     * @param logParams the parameters for the message
     * @param useResourceBundle
     * @return
     */
    private String formatMessage(LogRecord logRecord, Object[] logParams, boolean useResourceBundle) {
        final String txt;

        boolean hasParams = (logParams != null && logParams.length > 0);

        String msg = logRecord.getMessage();

        if (useResourceBundle) {
            ResourceBundle rb = logRecord.getResourceBundle();
            if (rb != null) {
                try {
                    msg = rb.getString(msg);
                } catch (Exception e) {
                }
            }
        }

        if (msg != null && hasParams && msg.contains("{0")) {
            Object[] formattedParams = new Object[logParams.length];
            for (int i = 0; i < logParams.length; i++) {
                // Do truncated stack traces where appropriate
                if (logParams[i] instanceof TruncatableThrowable) {
                    formattedParams[i] = DataFormatHelper.throwableToString((TruncatableThrowable) logParams[i]);
                } else if (logParams[i] instanceof Throwable) {
                    formattedParams[i] = DataFormatHelper.throwableToString(new TruncatableThrowable((Throwable) logParams[i]));
                } else {
                    formattedParams[i] = logParams[i];
                }
                // Would any of the other parameters benefit from our whizzy formatting? 
            }
            // If this is a parameter list, use MessageFormat to sort it out
            txt = MessageFormat.format(msg, formattedParams);
        } else {
            txt = msg + ((hasParams) ? " " + formatObj(logParams) : "");
        }
        return txt;
    }

    /**
     * Format the given record into the desired trace format
     * 
     * @param name
     * @param sym
     * @param method
     * @param id
     * @param level
     * @param txt
     * @return String
     */
    private String createFormattedString(LogRecord logRecord, Object id, String txt, String detail) {

        String method = logRecord.getSourceMethodName();
        String className = StringUtils.firstNonEmpty(logRecord.getSourceClassName(), logRecord.getLoggerName());

        String stackTrace = getStackTrace(logRecord);

        StringBuilder sb = new StringBuilder(256);

        // Common header
        sb.append('[').append(DataFormatHelper.formatTime(logRecord.getMillis())).append("] ");

        // Sub-job details, if present
        if (detail != null && detail.length() > 0) {
            sb.append("[").append(detail).append("] ");
        }

        if (className != null) {
            sb.append(String.format("%-60.60s   ", className));
        } else {
            sb.append(String.format("%-60.60s   ", " "));
        }

        if (method != null) {
            sb.append(method + " ");
        }

        // append formatted message -- includes formatted args
        sb.append(txt);
        if (stackTrace != null) {
            sb.append(nl + basicPadding).append(stackTrace);
        }

        sb.append(nl);

        return sb.toString();
    }

    /**
     * @param logRecord
     * @return
     */
    private String getStackTrace(LogRecord logRecord) {
        Throwable t = logRecord.getThrown();
        if (t != null) {
            return DataFormatHelper.throwableToString(t);
        }
        return null;
    }

    /**
     * Method used to print out traced objects which recognizes and handles arrays and throwables.
     * 
     * @param objs
     * @return String
     */
    public String formatObj(Object objs) {

        String ans = "";

        final String nlPad;

        // Pad amount changes based on trace format
        /**
         * if (TraceFormat.ADVANCED.equals(traceFormat)) {
         * nlPad = nlAdvancedPadding;
         * } else if (TraceFormat.BASIC.equals(traceFormat)) {
         * nlPad = nlBasicPadding;
         * } else {
         * nlPad = nlEnhancedPadding;
         * }
         */
        nlPad = nl + basicPadding;

        final String nlPadA = nlPad + " ";

        if (objs != null) {
            if (objs.getClass().isArray()) {
                StringBuilder sb = new StringBuilder();
                final int len = Array.getLength(objs);

                if (objs.getClass().getName().equals("[B")) { // Byte array
                    final int COLUMNS = 32;

                    byte b[] = (byte[]) objs;
                    int printLen = len > MAX_DATA_LENGTH ? MAX_DATA_LENGTH : len;
                    sb.append(nlPad).append(objs.toString()).append(",len=").append(len);

                    for (int i = 0; i < printLen; i++) {
                        if (i % COLUMNS == 0) // offset
                            sb.append(nlPadA + '|' + DataFormatHelper.padHexString(i, 4) + '|');

                        if (i % 4 == 0) // group in words
                            sb.append(" ");

                        sb.append(hexChars[(b[i] >> 4) & 0xF]);
                        sb.append(hexChars[b[i] & 0xF]);
                    }

                    if (printLen != len)
                        sb.append(nlPadA).append(" ...");
                } else if (objs.getClass().getName().equals("[C")) {
                    // Character array - Just print it as a string rather than a single
                    // character on each line
                    sb.append((char[]) objs);
                } else { // not a Byte or char array                   
                    for (int i = 0; i < len; i++) {
                        String s = formatObj(Array.get(objs, i));
                        if (s.startsWith(nl))
                            sb.append(s);
                        else
                            sb.append(nlPad + s);

                        if (sb.length() > MAX_DATA_LENGTH) {
                            sb.append(nlPad + "...");
                            break;
                        }
                    }
                }

                ans = sb.toString();
            } else if (objs instanceof Untraceable) {
                ans = nlPad + objs.getClass().getName(); // Use only the
                // class name
                // of the object
            } else if (objs instanceof Traceable) {
                try {
                    ans = nlPad + ((Traceable) objs).toTraceString();
                } catch (Exception e) {
                    // No FFDC code needed
                    ans = "<Exception " + e + " caught while calling toTraceString() on object " + objs.getClass().getName() + "@"
                          + Integer.toHexString(System.identityHashCode(objs)) + ">";
                }
            } else if (objs instanceof TruncatableThrowable) {
                ans = nlPad + DataFormatHelper.throwableToString((TruncatableThrowable) objs);
            } else if (objs instanceof Throwable) {
                ans = nlPad + DataFormatHelper.throwableToString(new TruncatableThrowable((Throwable) objs));
            } else {
                try { // Protect ourselves from badly behaved toString methods
                    ans = nlPad + objs.toString();
                } catch (Exception e) {
                    // No FFDC code needed
                    ans = "<Exception " + e + " caught while calling toString() on object " + objs.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(objs))
                          + ">";
                }
            }
        } else {
            // objs is null - print out "null"
            ans = nlPad + nullParamString;
        }

        return ans;
    }

    private Object[] removeArg(Object[] args, int indexToRemove) {
        Object[] result = new Object[args.length - 1];
        int j = 0;
        for (int i = 0; i < args.length; i++) {
            if (i != indexToRemove) {
                result[j] = args[i];
                j++;
            }
        }
        return result;
    }

}
