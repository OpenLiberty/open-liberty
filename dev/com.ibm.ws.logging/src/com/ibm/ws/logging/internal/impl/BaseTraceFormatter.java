/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.impl;

import java.lang.reflect.Array;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import com.ibm.ejs.ras.Untraceable;
import com.ibm.websphere.logging.WsLevel;
import com.ibm.websphere.ras.DataFormatHelper;
import com.ibm.websphere.ras.Traceable;
import com.ibm.websphere.ras.TruncatableThrowable;
import com.ibm.ws.logging.collector.DateFormatHelper;
import com.ibm.ws.logging.data.GenericData;
import com.ibm.ws.logging.data.KeyValuePair;
import com.ibm.ws.logging.data.Pair;
import com.ibm.ws.logging.internal.PackageProcessor;
import com.ibm.ws.logging.internal.WsLogRecord;
import com.ibm.ws.logging.internal.impl.LoggingConstants.TraceFormat;

/**
 *
 */
public class BaseTraceFormatter extends Formatter {

    public static final String banner = "********************************************************************************";

    public static final Object NULL_ID = null;
    public static final String NULL_FORMATTED_MSG = null;

    static final int basicNameLength = 13;
    static final int enhancedNameLength = 60;

    static final String pad8 = "        ";
    static final String basicPadding = "                                 ";
    static final String advancedPadding = "          ";
    static final String enhancedPadding = "                                                                                                               ";
    static final String nlBasicPadding = LoggingConstants.nl + basicPadding;
    static final String nlAdvancedPadding = LoggingConstants.nl + advancedPadding;
    static final String nlEnhancedPadding = LoggingConstants.nl + enhancedPadding;

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

    public static final String levelValToString(Integer level) {
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

            if (level == WsLevel.EVENT.intValue())
                return N_EVENT;
        }
        return "";
    }

    //copied from BTS
    /** Flags for suppressing traceback output to the console */
    private static class StackTraceFlags {
        boolean needsToOutputInternalPackageMarker = false;
        boolean isSuppressingTraces = false;
    }

    /** Track the stack trace printing activity of the current thread */
    private static ThreadLocal<StackTraceFlags> traceFlags = new ThreadLocal<StackTraceFlags>() {
        @Override
        protected StackTraceFlags initialValue() {
            return new StackTraceFlags();
        }
    };

    final TraceFormat traceFormat;

    static boolean useIsoDateFormat = false;

    /**
     * @param traceFormat
     */
    public BaseTraceFormatter(TraceFormat traceFormat) {
        this.traceFormat = traceFormat;
    }

    /**
     * @return configured trace format
     */
    public TraceFormat getTraceFormat() {
        return traceFormat;
    }

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
        String text = formatMessage(r);

        return createFormattedString(r, NULL_ID, text);
    }

    /**
     * Format a detailed record for trace.log. Previously formatted messages may
     * be provided and may be reused if possible.
     *
     * @param logRecord
     * @param id
     * @param formattedMsg the result of {@link #formatMessage}, or null if that
     *            method was not previously called
     * @param formattedVerboseMsg the result of {@link #formatVerboseMessage},
     *            or null if that method was not previously called
     * @return
     */
    public String traceLogFormat(LogRecord logRecord, Object id, String formattedMsg, String formattedVerboseMsg) {
        final String txt;
        if (formattedVerboseMsg == null) {
            // If we don't already have a formatted message... (for Audit or Info or Warning.. )
            // we have to build something instead (while avoiding a useless resource bundle lookup)
            txt = formatVerboseMessage(logRecord, formattedMsg, false);
        } else {
            txt = formattedVerboseMsg;
        }

        return createFormattedString(logRecord, id, txt);
    }

//    /**
//     * Format a detailed record for trace.log. Previously formatted messages may
//     * be provided and may be reused if possible.
//     *
//     * @param logRecord
//     * @param id
//     * @param formattedMsg the result of {@link #formatMessage}, or null if that
//     *            method was not previously called
//     * @param formattedVerboseMsg the result of {@link #formatVerboseMessage},
//     *            or null if that method was not previously called
//     * @return
//     */
//    public String traceLogFormatter(GenericData object) {
//        ArrayList<Pair> pairs = object.getPairs();
//        KeyValuePair kvp = null;
//        String txt;
//        for (Pair p : pairs) {
//
//            if (p instanceof KeyValuePair) {
//
//                kvp = (KeyValuePair) p;
//                if (kvp.getKey().equals("message")) {
//                    txt = kvp.getValue();
//                } else if (kvp.getKey().equals("throwable")) {
//                    throwable = kvp.getValue();
//                } else if (kvp.getKey().equals("levelValue")) {
//                    levelValue = Integer.parseInt(kvp.getValue());
//                }
//
//            }
//        }
////        if (formattedVerboseMsg == null) {
////            // If we don't already have a formatted message... (for Audit or Info or Warning.. )
////            // we have to build something instead (while avoiding a useless resource bundle lookup)
////            txt = formatVerboseMessage(logRecord, formattedMsg, false);
////        } else {
////            txt = formattedVerboseMsg;
////        }
//
//        return createFormattedString(logRecord, id, txt);
//    }

    /**
     * {@inheritDoc} <br />
     * We override this method because in some JVMs, it is synchronized (why on earth?!?!).
     */
    @Override
    public String formatMessage(LogRecord logRecord) {
        if (System.getSecurityManager() == null) {
            return formatMessage(logRecord, logRecord.getParameters(), true);
        } else {
            final LogRecord f_logRecord = logRecord;
            return AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return formatMessage(f_logRecord, f_logRecord.getParameters(), true);
                }
            });
        }

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

        boolean hasParams = logParams != null && logParams.length > 0;

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
                } else if (logParams[i] instanceof Untraceable) {
                    formattedParams[i] = logParams[i].getClass().getName(); // Use only the class name of the object
                } else if (logParams[i] instanceof Traceable) {
                    formattedParams[i] = formatTraceable((Traceable) logParams[i]);
                } else {
                    formattedParams[i] = logParams[i];
                }
                // Would any of the other parameters benefit from our whizzy formatting?
            }
            // If this is a parameter list, use MessageFormat to sort it out
            txt = MessageFormat.format(msg, formattedParams);
        } else {
            txt = msg + (hasParams ? " " + formatObj(logParams) : "");
        }
        return txt;
    }

    /**
     * Format a translatable verbose message. This produces the same result
     * as {@link #formatMessage} unless any parameters need to be modified
     * by {@link #formatVerboseObj}. The previously formatted message may be
     * reused if specified and no parameters need to be modified.
     *
     * @param logRecord
     * @param msg the result of {@link #formatMessage}, or null if that method
     *            was not previously called
     * @return
     */
    public String formatVerboseMessage(LogRecord logRecord, String msg) {
        return formatVerboseMessage(logRecord, msg, true);
    }

    /**
     * Format a verbose message. This produces the same result
     * as {@link #formatMessage} unless any parameters need to be modified
     * by {@link #formatVerboseObj}. The previously formatted message may be
     * reused if specified and no parameters need to be modified.
     *
     * @param logRecord
     * @param formattedMsg the result of {@link #formatMessage}, or null if that
     *            method was not previously called
     * @param useResourceBundle
     * @return the formatted message
     */
    public String formatVerboseMessage(LogRecord logRecord, String formattedMsg, boolean useResourceBundle) {
        Object[] logParams = logRecord.getParameters();
        if (logParams != null) {
            // Loop through the parameters looking for those that are formatted
            // differently in verbose messages.
            for (int i = 0; i < logParams.length; i++) {
                Object logParam = logParams[i];
                if (logParam != null) {
                    Object newLogParam = formatVerboseObj(logParam);
                    if (newLogParam != null) {
                        // We found a parameter with verbose formatting.

                        // Copy the [0-i) parameters unchanged.
                        Object[] newLogParams = new Object[logParams.length];
                        System.arraycopy(logParams, 0, newLogParams, 0, i);

                        // Copy the reformatted i'th parameter.
                        newLogParams[i] = newLogParam;

                        // Loop over the rest of the parameters, reformatting
                        // or copying unchanged as needed.
                        for (i++; i < logParams.length; i++) {
                            logParam = logParams[i];
                            if (logParam != null) {
                                newLogParam = formatVerboseObj(logParam);
                                newLogParams[i] = newLogParam != null ? newLogParam : logParam;
                            }
                        }

                        return formatMessage(logRecord, newLogParams, useResourceBundle);
                    }
                }
            }
        }

        // No reformatting necessary.  Use the already formatted message, or
        // format it now if necessary.
        if (formattedMsg == null) {
            formattedMsg = formatMessage(logRecord, logParams, useResourceBundle);
        }

        return formattedMsg;
    }

    /**
     * Format an object for a verbose message.
     *
     * @param obj the non-null parameter object
     * @return the reformatted object, or null if the object can be used as-is
     */
    private Object formatVerboseObj(Object obj) {
        // Make sure that we don't truncate any stack traces during verbose logging

        if (obj instanceof TruncatableThrowable) {
            TruncatableThrowable truncatable = (TruncatableThrowable) obj;
            final Throwable wrappedException = truncatable.getWrappedException();
            return DataFormatHelper.throwableToString(wrappedException);
        } else if (obj instanceof Throwable) {
            return DataFormatHelper.throwableToString((Throwable) obj);
        }
        return null;
    }

    /**
     * The console log is not structured, and relies on already formatted/translated
     * messages
     *
     * @param logRecord
     * @param txt the result of {@link #formatMessage}
     * @return Formatted string for the console
     */
    public String consoleLogFormat(LogRecord logRecord, String txt) {
        StringBuilder sb = new StringBuilder(256);
        sb.append(BaseTraceFormatter.levelToString(logRecord.getLevel()));
        sb.append(txt);
        Throwable t = logRecord.getThrown();
        if (t != null) {
            String s = t.getLocalizedMessage();
            if (s == null)
                s = t.toString();
            sb.append(LoggingConstants.nl).append(s);
        }

        return sb.toString();
    }

    /**
     * The console log is not structured, and relies on already formatted/translated
     * messages
     *
     * @param logRecord
     * @param txt the result of {@link #formatMessage}
     * @return Formatted string for the console
     */
    public String consoleLogFormatter(GenericData genData, Integer consoleLogLevel) {
        StringBuilder sb = new StringBuilder(256);
//        String name = genData.getSourceType();
        ArrayList<Pair> pairs = genData.getPairs();
        KeyValuePair kvp = null;
        String message = null;
//        Long datetime = null;
//        String levelString = "";
        String throwable = null;
        Integer levelValue = null;
        for (Pair p : pairs) {

            if (p instanceof KeyValuePair) {

                kvp = (KeyValuePair) p;
                if (kvp.getKey().equals("message")) {
                    message = kvp.getValue();
                } else if (kvp.getKey().equals("throwable")) {
                    throwable = kvp.getValue();
                } else if (kvp.getKey().equals("levelValue")) {
                    levelValue = Integer.parseInt(kvp.getValue());
                }

            }
        }
        if (levelValue == WsLevel.ERROR.intValue() || levelValue == WsLevel.FATAL.intValue()) {
            sb.append(BaseTraceFormatter.levelValToString(levelValue));
//          sb.append(levelString);
            sb.append(message);
            if (throwable != null) {
                sb.append(LoggingConstants.nl).append(throwable);
            }
//          if (levelValue == WsLevel.ERROR.intValue() || levelValue == WsLevel.FATAL.intValue()) {
//
//              return sb.toString();
//          } else {
//              return sb.toString();
//          }
            return sb.toString();
        }
        if (levelValue >= consoleLogLevel) {
            sb.append(BaseTraceFormatter.levelValToString(levelValue));
//            sb.append(levelString);
            sb.append(message);
            if (throwable != null) {
                sb.append(LoggingConstants.nl).append(throwable);
            }
//            if (levelValue == WsLevel.ERROR.intValue() || levelValue == WsLevel.FATAL.intValue()) {
//
//                return sb.toString();
//            } else {
//                return sb.toString();
//            }
            return sb.toString();

        }
        return null;
    }

    /**
     * The messages log always uses the same/enhanced format, and relies on already formatted
     * messages. This does the formatting needed to take a message suitable for console.log
     * and wrap it to fit into messages.log.
     *
     * @param logRecord
     * @param formattedVerboseMsg the result of {@link #formatVerboseMessage}
     * @return Formatted string for messages.log
     */
    public String messageLogFormat(LogRecord logRecord, String formattedVerboseMsg) {
        // This is a very light trace format, based on enhanced:
        StringBuilder sb = new StringBuilder(256);
        String sym = getMarker(logRecord);
        String name = nonNullString(logRecord.getLoggerName(), logRecord.getSourceClassName());

        sb.append('[').append(DateFormatHelper.formatTime(logRecord.getMillis(), useIsoDateFormat)).append("] ");
        sb.append(DataFormatHelper.getThreadId()).append(' ');
        formatFixedString(sb, name, enhancedNameLength);
        sb.append(sym); // sym has built-in padding
        sb.append(formattedVerboseMsg);

        if (logRecord.getThrown() != null) {
            String stackTrace = getStackTrace(logRecord);
            if (stackTrace != null)
                sb.append(LoggingConstants.nl).append(stackTrace);
        }

        return sb.toString();
    }

    /**
     * The messages log always uses the same/enhanced format, and relies on already formatted
     * messages. This does the formatting needed to take a message suitable for console.log
     * and wrap it to fit into messages.log.
     *
     * @param logRecord
     * @param formattedVerboseMsg the result of {@link #formatVerboseMessage}
     * @return Formatted string for messages.log
     */
    public String messageLogFormatter(GenericData genData) {
        // This is a very light trace format, based on enhanced:
        StringBuilder sb = new StringBuilder(256);
//        String sym = getMarker(logRecord);
//        String name = nonNullString(logRecord.getLoggerName(), logRecord.getSourceClassName());
        String name = genData.getSourceType();
        ArrayList<Pair> pairs = genData.getPairs();
        KeyValuePair kvp = null;
        String message = "";
        Long datetime = null;
        String level = "";
        String throwable = null;
        for (Pair p : pairs) {

            if (p instanceof KeyValuePair) {

                kvp = (KeyValuePair) p;
                if (kvp.getKey().equals("message")) {
                    message = kvp.getValue();
                } else if (kvp.getKey().equals("ibm_datetime")) {

                    datetime = Long.parseLong(kvp.getValue());

                } else if (kvp.getKey().equals("severity")) {
                    level = kvp.getValue();
                } else if (kvp.getKey().equals("throwable")) {
                    throwable = kvp.getValue();
                }

            }
        }
        sb.append('[').append(DateFormatHelper.formatTime(datetime, useIsoDateFormat)).append("] ");
        sb.append(DataFormatHelper.getThreadId()).append(' ');
        formatFixedString(sb, name, enhancedNameLength);
        sb.append(level + " "); // sym has built-in padding
        sb.append(message);

        if (throwable != null) {
            sb.append(LoggingConstants.nl).append(throwable);
        }

        return sb.toString();
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
    private String createFormattedString(LogRecord logRecord, Object id, String txt) {

        String objId;
        WsLogRecord wsLogRecord = getWsLogRecord(logRecord);

        String name;
        String method = logRecord.getSourceMethodName();
        String className = logRecord.getSourceClassName();

        String stackTrace = getStackTrace(logRecord);
        String sym = getMarker(logRecord);

        StringBuilder sb = new StringBuilder(256);

        // Common header
        sb.append('[').append(DateFormatHelper.formatTime(logRecord.getMillis(), useIsoDateFormat)).append("] ");
        sb.append(DataFormatHelper.getThreadId());

        switch (traceFormat) {
            default:
            case ENHANCED:
                objId = generateObjectId(id, true);
                name = nonNullString(logRecord.getSourceClassName(), logRecord.getLoggerName());

                sb.append(" id=").append(objId).append(' ');
                formatFixedString(sb, name, enhancedNameLength);
                sb.append(sym); // sym has built-in padding
                if (method != null) {
                    sb.append(method).append(' ');
                }

                // append formatted message -- txt includes formatted args
                sb.append(txt);
                if (stackTrace != null)
                    sb.append(LoggingConstants.nl).append(stackTrace);
                break;
            case BASIC:
                name = nonNullString(logRecord.getLoggerName(), logRecord.getSourceClassName());

                sb.append(' '); // pad after thread id
                fixedClassString(sb, name, basicNameLength);
                sb.append(sym);

                if (className != null)
                    sb.append(className);
                sb.append(' '); // yes, this space is always there.

                if (method != null)
                    sb.append(method);
                sb.append(' '); // yes, this space is always there.

                // append formatted message -- includes formatted args
                sb.append(txt);
                if (stackTrace != null)
                    sb.append(nlBasicPadding).append(stackTrace);
                break;
            case ADVANCED:
                objId = generateObjectId(id, false);
                name = nonNullString(logRecord.getLoggerName(), null);

                sb.append(' '); // pad after thread id
                sb.append(sym);

                // next append the correlation id.
                sb.append("UOW=");
                if (wsLogRecord != null)
                    sb.append(wsLogRecord.getCorrelationId());

                // next enter the logger name.
                sb.append(" source=").append(name);

                // append className if non-null
                if (className != null)
                    sb.append(" class=").append(className);

                // append methodName if non-null
                if (method != null)
                    sb.append(" method=").append(method);

                if (id != null)
                    sb.append(" id=").append(objId);

                String x;
                if (wsLogRecord != null) {
                    // next append org, prod, component, if set. Reference equality check is ok here.
                    sb.append(" org=");
                    sb.append(wsLogRecord.getOrganization());
                    sb.append(" prod=");
                    sb.append(wsLogRecord.getProduct());
                    sb.append(" component=");
                    sb.append(wsLogRecord.getComponent());

                    //get thread name
                    x = wsLogRecord.getReporterOrSourceThreadName();
                } else {
                    x = Thread.currentThread().getName();
                }
                if (x != null) {
                    sb.append(" thread=[").append(x).append("]");
                }

                // append formatted message -- txt includes formatted args
                sb.append(nlAdvancedPadding).append(txt);
                if (stackTrace != null)
                    sb.append(nlAdvancedPadding).append(stackTrace);
                break;
        }

        return sb.toString();
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
    public String traceFormatGenData(GenericData gen) {

        ArrayList<Pair> pairs = gen.getPairs();
        KeyValuePair kvp = null;
        String txt = null;
        String stackTrace = null;
        String id = null;
        String objId;

        String sym = null;
//      WsLogRecord wsLogRecord = getWsLogRecord(logRecord);

        String name;
//        String method = logRecord.getSourceMethodName();
//        String className = logRecord.getSourceClassName();
        String className = null;
        String method = null;
        String loggerName = null;
        Long ibm_datetime = null;

        String corrId = null;
        String org = null;
        String prod = null;
        String component = null;
        String wsSourceThreadName = null;
//      String stackTrace = getStackTrace(logRecord);
//        String sym = getMarker(logRecord);
        for (Pair p : pairs) {

            if (p instanceof KeyValuePair) {

                kvp = (KeyValuePair) p;
                if (kvp.getKey().equals("message")) {
                    txt = kvp.getValue();
                } else if (kvp.getKey().equals("throwable")) {
                    stackTrace = kvp.getValue();
                } else if (kvp.getKey().equals("ibm_datetime")) {
                    ibm_datetime = Long.parseLong(kvp.getValue());
                } else if (kvp.getKey().equals("severity")) {
                    sym = " " + kvp.getValue() + " ";
                } else if (kvp.getKey().equals("ibm_className")) {
                    className = kvp.getValue();
                } else if (kvp.getKey().equals("ibm_methodName")) {
                    method = kvp.getValue();
                } else if (kvp.getKey().equals("module")) {
                    loggerName = kvp.getValue();
                } else if (kvp.getKey().equals("objectId")) {
                    id = kvp.getValue();
                } else if (kvp.getKey().equals("correlationId")) {
                    corrId = kvp.getValue();
                } else if (kvp.getKey().equals("org")) {
                    org = kvp.getValue();
                } else if (kvp.getKey().equals("product")) {
                    prod = kvp.getValue();
                } else if (kvp.getKey().equals("component")) {
                    component = kvp.getValue();
                } else if (kvp.getKey().equals("wsSourceThreadName")) {
                    wsSourceThreadName = kvp.getValue();
                }

            }
        }

        StringBuilder sb = new StringBuilder(256);

        // Common header
        sb.append('[').append(DateFormatHelper.formatTime(ibm_datetime, useIsoDateFormat)).append("] ");
        sb.append(DataFormatHelper.getThreadId());

        switch (traceFormat) {
            default:
            case ENHANCED:
                objId = generateObjectId(id, true);
                name = nonNullString(className, loggerName);

                sb.append(" id=").append(objId).append(' ');
                formatFixedString(sb, name, enhancedNameLength);
                sb.append(sym); // sym has built-in padding
                if (method != null) {
                    sb.append(method).append(' ');
                }

                // append formatted message -- txt includes formatted args
                sb.append(txt);
                if (stackTrace != null)
                    sb.append(LoggingConstants.nl).append(stackTrace);
                break;
            case BASIC:
                name = nonNullString(loggerName, className);

                sb.append(' '); // pad after thread id
                fixedClassString(sb, name, basicNameLength);
                sb.append(sym);

                if (className != null)
                    sb.append(className);
                sb.append(' '); // yes, this space is always there.

                if (method != null)
                    sb.append(method);
                sb.append(' '); // yes, this space is always there.

                // append formatted message -- includes formatted args
                sb.append(txt);
                if (stackTrace != null)
                    sb.append(nlBasicPadding).append(stackTrace);
                break;
            case ADVANCED:
                objId = generateObjectId(id, false);
                name = nonNullString(loggerName, null);

                sb.append(' '); // pad after thread id
                sb.append(sym);

                // next append the correlation id.
                sb.append("UOW=");
                if (corrId != null)
                    sb.append(corrId);

                // next enter the logger name.
                sb.append(" source=").append(name);

                // append className if non-null
                if (className != null)
                    sb.append(" class=").append(className);

                // append methodName if non-null
                if (method != null)
                    sb.append(" method=").append(method);

                if (id != null)
                    sb.append(" id=").append(objId);

                String x = null;
                if (org != null && org != "") {
                    // next append org, prod, component, if set. Reference equality check is ok here.
                    sb.append(" org=");
                    sb.append(org);
                    sb.append(" prod=");
                    sb.append(prod);
                    sb.append(" component=");
                    sb.append(component);

                    //get thread name
//                    x = wsLogRecord.getReporterOrSourceThreadName();
                    if (wsSourceThreadName != null) {
                        x = wsSourceThreadName;
                    }
                } else {
                    x = Thread.currentThread().getName();
                }
                if (x != null) {
                    sb.append(" thread=[").append(x).append("]");
                }

                // append formatted message -- txt includes formatted args
                sb.append(nlAdvancedPadding).append(txt);
                if (stackTrace != null)
                    sb.append(nlAdvancedPadding).append(stackTrace);
                break;
        }

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

    private final String generateObjectId(Object id, boolean fixedWidth) {
        String objId;

        if (id != null) {
            objId = Integer.toHexString(System.identityHashCode(id));
            if (objId.length() < 8) {
                StringBuilder builder = new StringBuilder();
                builder.append("00000000");
                builder.append(objId);
                objId = builder.substring(builder.length() - 8);
            }
        } else if (fixedWidth) {
            objId = pad8;
        } else {
            objId = emptyString;
        }
        return objId;
    }

    private void formatFixedString(StringBuilder output, String s, int len) {
        if (s == null)
            s = "null";

        if (s.length() > len) {
            output.append(s.substring(s.length() - len, s.length()));
        } else {
            output.append(s);
            if (len > s.length())
                output.append(enhancedPadding.substring(0, len - s.length())); // append spaces
        }
    }

    private void fixedClassString(StringBuilder output, String s, int len) {
        if (s == null)
            s = "null";

        int i = s.lastIndexOf('.');
        if (i >= 0) {
            s = s.substring(i + 1);
        }

        if (s.length() > len) {
            output.append(s.substring(0, len));
        } else {
            output.append(s);
            if (len > s.length())
                output.append(enhancedPadding.substring(0, len - s.length())); // append spaces
        }
    }

    private String formatTraceable(Traceable t) {
        String formatted;
        try {
            formatted = t.toTraceString();
        } catch (Exception e) {
            // No FFDC code needed
            formatted = "<Exception " + e + " caught while calling toTraceString() on object " + t.getClass().getName() + "@"
                        + Integer.toHexString(System.identityHashCode(t)) + ">";
        }
        return formatted;
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
        if (TraceFormat.ADVANCED.equals(traceFormat)) {
            nlPad = nlAdvancedPadding;
        } else if (TraceFormat.BASIC.equals(traceFormat)) {
            nlPad = nlBasicPadding;
        } else {
            nlPad = nlEnhancedPadding;
        }

        final String nlPadA = nlPad + " ";

        if (objs != null) {
            if (objs.getClass().isArray()) {
                StringBuilder sb = new StringBuilder();
                final int len = Array.getLength(objs);

                if (objs.getClass().getName().equals("[B")) { // Byte array
                    final int COLUMNS = 32;

                    byte b[] = (byte[]) objs;
                    int printLen = len > LoggingConstants.MAX_DATA_LENGTH ? LoggingConstants.MAX_DATA_LENGTH : len;
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
                        if (s.startsWith(LoggingConstants.nl))
                            sb.append(s);
                        else
                            sb.append(nlPad + s);

                        if (sb.length() > LoggingConstants.MAX_DATA_LENGTH) {
                            sb.append(nlPad + "...");
                            break;
                        }
                    }
                }

                ans = sb.toString();
            } else if (objs instanceof Untraceable) {
                ans = nlPad + objs.getClass().getName(); // Use only the class name of the object
            } else if (objs instanceof Traceable) {
                ans = nlPad + formatTraceable((Traceable) objs);
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

    private static String nonNullString(final String parameter, final String alternate) {
        if (parameter != null)
            return parameter;
        if (alternate != null)
            return alternate;
        return "";
    }

    private String getMarker(LogRecord logRecord) {
        Level level = logRecord.getLevel();
        if (level == null)
            return " Z ";

        // err & out are in the mix
        if (level.getName() == LoggingConstants.SYSTEM_ERR)
            return " R ";
        if (level.getName() == LoggingConstants.SYSTEM_OUT)
            return " O ";

        LevelFormat f = LevelFormat.findFormat(level);
        if (f == null)
            return " Z ";

        if (f.level == Level.FINER) {
            // Check for Tr.entry/.exit, which use the ENTRY/EXIT constants, or
            // Logger.entering/.exiting, which use "ENTRY", "ENTRY {0}",
            // "ENTRY {0} {1}", etc. and "RETURN" or "RETURN {0}".
            String message = logRecord.getMessage();
            if (message != null) {
                if (message.equals(ENTRY) || message.startsWith("ENTRY"))
                    return " > ";
                if (message.equals(EXIT) || message.startsWith("RETURN"))
                    return " < ";
            }
        }

        return f.marker;
    }

    /**
     * @return
     */
    private WsLogRecord getWsLogRecord(LogRecord logRecord) {
        try {
            return (WsLogRecord) logRecord;
        } catch (ClassCastException ex) {
            return null;
        }
    }

    protected String filteredStreamOutput(GenericData genData) {
        String txt = null;
//        String stackTrace = null;
        String loglevel = null;
        KeyValuePair kvp = null;

        ArrayList<Pair> pairs = genData.getPairs();
//      String stackTrace = getStackTrace(logRecord);
//        String sym = getMarker(logRecord);
        for (Pair p : pairs) {

            if (p instanceof KeyValuePair) {

                kvp = (KeyValuePair) p;
                if (kvp.getKey().equals("message")) {
                    txt = kvp.getValue();
                } else if (kvp.getKey().equals("loglevel")) {
                    loglevel = kvp.getValue();
                }
            }
        }
        if (!(loglevel.equals("SystemErr") || loglevel.equals("SystemOut"))) {
            return null;
        }
        String message = filterStackTraces(txt);
        if (txt != null) {
            if (loglevel.equals("SystemErr")) {
                message = "[err] " + message;
            }
            return message;
        }
        return txt;
    }

    private String filterStackTraces(String txt) {
        // Check for stack traces, which we may want to trim
        StackTraceFlags stackTraceFlags = traceFlags.get();
        // We have a little thread-local state machine here with four states controlled by two
        // booleans. Our triggers are { "unknown/user code", "just seen IBM code", "second line of IBM code", ">second line of IBM code"}
        // "unknown/user code" -> stackTraceFlags.isSuppressingTraces -> false, stackTraceFlags.needsToOutputInternalPackageMarker -> false
        // "just seen IBM code" -> stackTraceFlags.needsToOutputInternalPackageMarker->true
        // "second line of IBM code" -> stackTraceFlags.needsToOutputInternalPackageMarker->true
        // ">second line of IBM code" -> stackTraceFlags.isSuppressingTraces->true
        // The final two states are optional

        if (txt.startsWith("\tat ")) {
            // This is a stack trace, do a more detailed analysis
            PackageProcessor packageProcessor = PackageProcessor.getPackageProcessor();
            String packageName = PackageProcessor.extractPackageFromStackTraceLine(txt);
            // If we don't have a package processor, don't suppress anything
            if (packageProcessor != null && packageProcessor.isIBMPackage(packageName)) {
                // First internal package, we let through
                // Second one, we suppress but say we did
                // If we're still suppressing, and this is a stack trace, this is easy - we suppress
                if (stackTraceFlags.isSuppressingTraces) {
                    txt = null;
                } else if (stackTraceFlags.needsToOutputInternalPackageMarker) {
                    // Replace the stack trace with something saying we got rid of it
                    txt = "\tat " + TruncatableThrowable.INTERNAL_CLASSES_STRING;
                    // No need to output another marker, we've just output it
                    stackTraceFlags.needsToOutputInternalPackageMarker = false;
                    // Suppress any subsequent IBM frames
                    stackTraceFlags.isSuppressingTraces = true;
                } else {
                    // Let the text through, but make a note not to let anything but an [internal classes] through
                    stackTraceFlags.needsToOutputInternalPackageMarker = true;
                }
            } else {
                // This is user code, third party API, or Java API, so let it through
                // Reset the flags to ensure it gets let through
                stackTraceFlags.isSuppressingTraces = false;
                stackTraceFlags.needsToOutputInternalPackageMarker = false;
            }

        } else {
            // We're no longer processing a stack, so reset all our state
            stackTraceFlags.isSuppressingTraces = false;
            stackTraceFlags.needsToOutputInternalPackageMarker = false;
        }
        return txt;
    }
}
