/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
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
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
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
import com.ibm.ws.logging.collector.LogFieldConstants;
import com.ibm.ws.logging.data.GenericData;
import com.ibm.ws.logging.data.KeyValuePair;
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
    static final int COLLECTION_DEPTH_LIMIT = AccessController.doPrivileged(new PrivilegedAction<Integer>() {
        @Override
        public Integer run() {
            return Integer.getInteger("com.ibm.ws.logging.collectionDepthLimit", 8);
        }
    });

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
    static final String SYSOUT = "SystemOut";
    static final String SYSERR = "SystemErr";

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

            //WSLevel.FATAl -> int value of 1100
            if (l == 1100)
                return N_FATAL;
            //WSLevel.ERROR -> int value of 1000
            if (l == 1000)
                return N_ERROR;
            //WSLevel.WARNING -> int value of 900
            if (l == 900)
                return N_WARN;
            //WSLevel.AUDIT -> int value of 850
            if (l == 850)
                return N_AUDIT;
            //WSLevel.INFO -> int value of 800
            if (l == 800)
                return N_INFO;
            //WSLevel.EVENT -> int value of 500
            if (level == 500)
                return N_EVENT;
        }
        return "";
    }

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
     * @param formattedMsg        the result of {@link #formatMessage}, or null if that
     *                                method was not previously called
     * @param formattedVerboseMsg the result of {@link #formatVerboseMessage},
     *                                or null if that method was not previously called
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
     * @param logParams         the parameters for the message
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
     * @param msg       the result of {@link #formatMessage}, or null if that method
     *                      was not previously called
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
     * @param formattedMsg      the result of {@link #formatMessage}, or null if that
     *                              method was not previously called
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
     * @param txt       the result of {@link #formatMessage}
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
     * @param genData
     * @return Formatted string for the console
     */
    public String consoleLogFormat(GenericData genData) {
        StringBuilder sb = new StringBuilder(256);
        KeyValuePair[] pairs = genData.getPairs();
        KeyValuePair kvp = null;
        String message = null;
        String throwable = null;
        Integer levelValue = null;
        for (KeyValuePair p : pairs) {

            if (p != null && !p.isList()) {

                kvp = p;
                if (kvp.getKey().equals(LogFieldConstants.FORMATTEDMSG)) {
                    message = kvp.getStringValue();
                } else if (kvp.getKey().equals(LogFieldConstants.LEVELVALUE)) {
                    levelValue = kvp.getIntValue();
                } else if (kvp.getKey().equals(LogFieldConstants.THROWABLE_LOCALIZED)) {
                    throwable = kvp.getStringValue();
                }
            }
        }

        sb.append(BaseTraceFormatter.levelValToString(levelValue));
        sb.append(message);

        //add throwable localized message if exists
        if (throwable != null) {
            sb.append(LoggingConstants.nl).append(throwable);
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
    public String messageLogFormat(LogRecord logRecord, String formattedVerboseMsg) {
        // This is a very light trace format, based on enhanced:
        StringBuilder sb = new StringBuilder(256);
        String sym = getMarker(logRecord);
        String name = null;
        if (logRecord.getLoggerName() != null && (logRecord.getLoggerName().equals(SYSOUT) ||
                                                  logRecord.getLoggerName().equals(SYSERR)))
            name = nonNullString(logRecord.getLoggerName(), null);
        else
            name = nonNullString(logRecord.getLoggerName(), logRecord.getSourceClassName());

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
     * @param genData
     * @return Formatted string for messages.log
     */
    public String messageLogFormat(GenericData genData) {
        // This is a very light trace format, based on enhanced:
        StringBuilder sb = new StringBuilder(256);
        String name = null;
        KeyValuePair[] pairs = genData.getPairs();
        KeyValuePair kvp = null;
        String message = null;
        Long datetime = null;
        String level = "";
        String loggerName = null;
        String srcClassName = null;
        String throwable = null;
        for (KeyValuePair p : pairs) {

            if (p != null && !p.isList()) {

                kvp = p;
                if (kvp.getKey().equals(LogFieldConstants.MESSAGE)) {
                    message = kvp.getStringValue();
                } else if (kvp.getKey().equals(LogFieldConstants.IBM_DATETIME)) {
                    datetime = kvp.getLongValue();
                } else if (kvp.getKey().equals(LogFieldConstants.SEVERITY)) {
                    level = kvp.getStringValue();
                } else if (kvp.getKey().equals(LogFieldConstants.MODULE)) {
                    loggerName = kvp.getStringValue();
                } else if (kvp.getKey().equals(LogFieldConstants.IBM_CLASSNAME)) {
                    srcClassName = kvp.getStringValue();
                } else if (kvp.getKey().equals(LogFieldConstants.THROWABLE)) {
                    throwable = kvp.getStringValue();
                }

            }
        }
        name = nonNullString(loggerName, srcClassName);
        sb.append('[').append(DateFormatHelper.formatTime(datetime, useIsoDateFormat)).append("] ");
        sb.append(DataFormatHelper.getThreadId()).append(' ');
        formatFixedString(sb, name, enhancedNameLength);
        sb.append(" " + level + " "); // sym has built-in padding
        sb.append(message);

        if (throwable != null) {
            sb.append(LoggingConstants.nl).append(throwable);
        }

        return sb.toString();
    }

    /**
     * The messages log always uses the same/enhanced format, and relies on already formatted
     * messages. This does the formatting needed to take a message suitable for console.log
     * and wrap it to fit into messages.log. This messages log is used for the trace basic logs.
     *
     * @param genData
     * @return Formatted string for messages.log
     */
    public String messageLogFormatTBasic(GenericData genData) {
        // This is a very light trace format, based on enhanced:
        StringBuilder sb = new StringBuilder(256);
        String name = null;
        KeyValuePair[] pairs = genData.getPairs();
        KeyValuePair kvp = null;
        String message = null;
        Long datetime = null;
        String level = "";
        String loggerName = null;
        String srcClassName = null;
        String methodName = null;
        String throwable = null;
        String extendedClassName = null;
        for (KeyValuePair p : pairs) {

            if (p != null && !p.isList()) {

                kvp = p;
                if (kvp.getKey().equals(LogFieldConstants.MESSAGE)) {
                    message = kvp.getStringValue();
                } else if (kvp.getKey().equals(LogFieldConstants.IBM_DATETIME)) {
                    datetime = kvp.getLongValue();
                } else if (kvp.getKey().equals(LogFieldConstants.SEVERITY)) {
                    level = kvp.getStringValue();
                } else if (kvp.getKey().equals(LogFieldConstants.MODULE)) {
                    loggerName = fixedClassString(kvp.getStringValue(), basicNameLength);
                } else if (kvp.getKey().equals(LogFieldConstants.IBM_CLASSNAME)) {
                    extendedClassName = kvp.getStringValue();
                    srcClassName = fixedClassString(extendedClassName, basicNameLength);
                } else if (kvp.getKey().equals(LogFieldConstants.THROWABLE)) {
                    throwable = kvp.getStringValue();
                } else if (kvp.getKey().equals(LogFieldConstants.IBM_METHODNAME)) {
                    methodName = kvp.getStringValue();
                }

            }
        }

        name = nonNullString(loggerName, srcClassName);
        sb.append('[').append(DateFormatHelper.formatTime(datetime, useIsoDateFormat)).append("] ");
        sb.append(DataFormatHelper.getThreadId()).append(' ');
        formatFixedString(sb, name, basicNameLength);

        if (methodName != null) {
            sb.append(" " + level + " "); // sym has built-in padding
            sb.append(extendedClassName + " " + methodName + " ");
        } else {
            sb.append(" " + level + "   "); // sym has built-in padding
        }
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
            case TBASIC:
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
     * @param genData GenericData pass information needed
     * @return String
     */
    public String traceFormatGenData(GenericData genData) {

        KeyValuePair[] pairs = genData.getPairs();
        KeyValuePair kvp = null;
        String txt = null;
        Integer id = null;
        String objId;
        @SuppressWarnings("unused")
        Integer levelVal = null;
        String name;

        String className = null;
        String method = null;
        String loggerName = null;
        Long ibm_datetime = null;

        String corrId = null;
        String org = null;
        String prod = null;
        String component = null;

        String sym = null;
        @SuppressWarnings("unused")
        String logLevel = null;

        String threadName = null;
        String stackTrace = null;
        for (KeyValuePair p : pairs) {

            if (p != null && !p.isList()) {

                kvp = p;
                if (kvp.getKey().equals(LogFieldConstants.MESSAGE)) {
                    txt = kvp.getStringValue();
                } else if (kvp.getKey().equals(LogFieldConstants.IBM_DATETIME)) {
                    ibm_datetime = kvp.getLongValue();
                } else if (kvp.getKey().equals(LogFieldConstants.SEVERITY)) {
                    sym = " " + kvp.getStringValue() + " ";
                } else if (kvp.getKey().equals(LogFieldConstants.IBM_CLASSNAME)) {
                    className = kvp.getStringValue();
                } else if (kvp.getKey().equals(LogFieldConstants.IBM_METHODNAME)) {
                    method = kvp.getStringValue();
                } else if (kvp.getKey().equals(LogFieldConstants.MODULE)) {
                    loggerName = kvp.getStringValue();
                } else if (kvp.getKey().equals(LogFieldConstants.OBJECT_ID)) {
                    id = kvp.getIntValue();
                } else if (kvp.getKey().equals(LogFieldConstants.CORRELATION_ID)) {
                    corrId = kvp.getStringValue();
                } else if (kvp.getKey().equals(LogFieldConstants.ORG)) {
                    org = kvp.getStringValue();
                } else if (kvp.getKey().equals(LogFieldConstants.PRODUCT)) {
                    prod = kvp.getStringValue();
                } else if (kvp.getKey().equals(LogFieldConstants.COMPONENT)) {
                    component = kvp.getStringValue();
                } else if (kvp.getKey().equals(LogFieldConstants.LOGLEVEL)) {
                    logLevel = kvp.getStringValue();
                } else if (kvp.getKey().equals(LogFieldConstants.THREADNAME)) {
                    threadName = kvp.getStringValue();
                } else if (kvp.getKey().equals(LogFieldConstants.LEVELVALUE)) {
                    levelVal = kvp.getIntValue();
                } else if (kvp.getKey().equals(LogFieldConstants.THROWABLE)) {
                    stackTrace = kvp.getStringValue();
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
            case TBASIC:
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

                //check the comparison to WsLogRecord
                if (org != null && prod != null && component != null) {
                    // next append org, prod, component, if set. Reference equality check is ok here.
                    sb.append(" org=");
                    sb.append(org);
                    sb.append(" prod=");
                    sb.append(prod);
                    sb.append(" component=");
                    sb.append(component);

                    //get thread name
                } else {
                    //ibm_threadId replace check if you can use this as the thread
                    sb.append(" thread=[").append(threadName).append("]");
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
        return generateObjectId(System.identityHashCode(id), fixedWidth);
    }

    /**
     * @param id ID from tracesource
     * @return String id converted to hex
     */
    private final String generateObjectId(Integer id, boolean fixedWidth) {
        String objId;

        if (id != null) {
            objId = Integer.toHexString(id);
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

    private String fixedClassString(String s, int len) {
        String output;
        if (s == null)
            s = "null";

        int i = s.lastIndexOf('.');
        if (i >= 0) {
            s = s.substring(i + 1);
        }

        if (s.length() > len) {
            output = s.substring(0, len);
        } else {
            output = s;
        }
        return output;
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
        } else if (TraceFormat.BASIC.equals(traceFormat) || TraceFormat.TBASIC.equals(traceFormat)) {
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
            } else {
                ans = nlPad + formatObject(objs, 0);
            }
        } else {
            // objs is null - print out "null"
            ans = nlPad + nullParamString;
        }

        return ans;
    }

    private String formatObject(Object objs, int depth) {
        if (objs == null) {
            return nullParamString;
        }
        if (depth >= COLLECTION_DEPTH_LIMIT) {
            return "Collection depth limit exceeded";
        }
        if (objs instanceof Untraceable) {
            return objs.getClass().getName(); // Use only the class name of the object
        } else if (objs instanceof Traceable) {
            return formatTraceable((Traceable) objs);
        } else if (objs instanceof TruncatableThrowable) {
            return DataFormatHelper.throwableToString((TruncatableThrowable) objs);
        } else if (objs instanceof Throwable) {
            return DataFormatHelper.throwableToString(new TruncatableThrowable((Throwable) objs));
        } else if (objs instanceof Collection) {
            Object[] objArray = null;
            int retryableExceptionCount = 0;
            while (objArray == null) {
                try {
                    if (retryableExceptionCount >= 100) {
                        return "[Caught too many exceptions while logging collection type " + objs.getClass().getName() + "]";
                    }
                    objArray = ((Collection<?>) objs).toArray();
                } catch (ConcurrentModificationException cme) {
                    // this exception is possible.  Need to retry until it doesn't happen any longer.
                    retryableExceptionCount++;
                } catch (NoSuchElementException nsee) {
                    // this exception is possible.  Need to retry until it doesn't happen any longer.
                    retryableExceptionCount++;
                } catch (Throwable t) {
                    return "[Caught " + t.toString() + " while logging collection type " + objs.getClass().getName() + "]";
                }
            }
            StringBuilder sb = new StringBuilder("[");
            if (objArray.length != 0) {
                sb.append(formatObject(objArray[0], depth + 1));
            }
            for (int i = 1; i < objArray.length; ++i) {
                sb.append(", ").append(formatObject(objArray[i], depth + 1));
            }
            sb.append("]");
            return sb.toString();
        }
        try { // Protect ourselves from badly behaved toString methods
            Class<?> cls = objs.getClass();
            String className = cls.getName();
            if (Proxy.isProxyClass(cls) || className.contains("$Proxy$_$$_Weld")) {
                return "Proxy for " + className + "@" + Integer.toHexString(System.identityHashCode(objs));
            }
            // Security level augmented to overcome a Java 2 sec error at a JAX-WS bundle
            String s = AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return objs.toString();
                }
            });

            return s;
        } catch (Exception e) {
            // No FFDC code needed
            String s = objs == null ? "null" : objs.getClass().getName();
            return "<Exception " + e + " caught while calling toString() on object " + s + "@" +
                   Integer.toHexString(System.identityHashCode(objs)) + ">";
        }
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
     * check casting to WsLogRecord
     *
     * @return
     */
    public WsLogRecord getWsLogRecord(LogRecord logRecord) {
        return (logRecord instanceof WsLogRecord) ? (WsLogRecord) logRecord : null;
    }

    /**
     * Outputs filteredStream of genData
     *
     * @param genData object to filter
     * @return filtered message of the genData
     */
    protected String formatStreamOutput(GenericData genData) {
        String txt = null;
        String loglevel = null;
        KeyValuePair kvp = null;

        KeyValuePair[] pairs = genData.getPairs();
        for (KeyValuePair p : pairs) {

            if (p != null && !p.isList()) {

                kvp = p;
                if (kvp.getKey().equals("message")) {
                    txt = kvp.getStringValue();
                } else if (kvp.getKey().equals("loglevel")) {
                    loglevel = kvp.getStringValue();
                }
            }
        }

        String message = BaseTraceService.filterStackTraces(txt);
        if (message != null) {
            if (loglevel.equals("SystemErr")) {
                message = "[err] " + message;
            }
        }
        return message;
    }
}
