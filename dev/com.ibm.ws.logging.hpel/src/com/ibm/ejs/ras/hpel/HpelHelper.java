/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.ras.hpel;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.ibm.websphere.logging.hpel.reader.HpelFormatter;
import com.ibm.websphere.logging.hpel.reader.ServerInstanceLogRecordList;
import com.ibm.ws.logging.hpel.LogRepositoryManager;
import com.ibm.ws.logging.hpel.LogRepositoryWriter;
import com.ibm.ws.logging.hpel.handlers.LogRecordHandler;
import com.ibm.ws.logging.hpel.impl.LogRepositoryBaseImpl;
import com.ibm.ws.logging.hpel.impl.LogRepositoryManagerImpl;
import com.ibm.ws.logging.hpel.impl.LogRepositoryWriterImpl;
import com.ibm.ws.logging.object.hpel.LogRecordStack;

public class HpelHelper {
    private static final String CLASS_NAME = HpelHelper.class.getName();
    private static final Logger log = Logger.getLogger(CLASS_NAME);
    /**
     * Duration to sleep before the next check if ownership file found to be empty.
     */
    static final long EMPTY_FILE_CHECK_SLEEP = 1000;
    /**
     * Number of sleep/check iterations to perform on an empty ownership file until assuming it's a stale file.
     */
    static final int EMPTY_FILE_CHECK_COUNT = 3;

    /**
     * Array used to convert integers to hex values
     */
    private final static char[] hexChars = {
                                            '0', '1', '2', '3', '4', '5', '6', '7',
                                            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    /**
     * Indicator if LogRecord.getThreadId() should be used as current thread Id. Otherwise, Thread.getId() will be used
     */
    private final static boolean useJULThreadId = AccessController.doPrivileged(
                    new PrivilegedAction<Boolean>() {
                        @Override
                        public Boolean run() {
                            return Boolean.parseBoolean(System.getProperty("com.ibm.websphere.logging.useJULThreadID", "false"));
                        }
                    });

    private static String os = getSystemProperty("os.name").trim();
    private static boolean isZOS = ((os.equals("OS/390") || os.equals("z/OS")));

    private static String processId = "";
    private static Properties customProps;
    private static String[] customFormat;
    private static TimeZone sysTimeZone = TimeZone.getDefault();

    public static void setCustomHeaderProperties(Properties properties) {
        customProps = properties;
    }

    /**
     * Set custom format to use printing header information.
     *
     * @param headerFormat array of string representing header lines.
     */
    public static void setCustomHeaderFormat(String[] headerFormat) {
        customFormat = new String[headerFormat.length];
        System.arraycopy(headerFormat, 0, customFormat, 0, headerFormat.length);
    }

    public static void setPid(String pid) {
        processId = pid == null ? processId : pid;
    }

    /**
     * Gets Header information as a Propeties instance.
     *
     * @return new Properties instance filled with necessary information.
     */
    public static Properties getHeaderAsProperties() {
        Properties result = new Properties();

        if (customProps != null) {
            result.putAll(customProps);
        }

        result.put(ServerInstanceLogRecordList.HEADER_PROCESSID, processId);
        result.put(ServerInstanceLogRecordList.HEADER_SERVER_TIMEZONE, TimeZone.getDefault().getID());
        result.put(ServerInstanceLogRecordList.HEADER_SERVER_LOCALE_LANGUAGE, Locale.getDefault().getLanguage());
        result.put(ServerInstanceLogRecordList.HEADER_SERVER_LOCALE_COUNTRY, Locale.getDefault().getCountry());

        addSystemPropertyIfPresent(result, "java.fullversion");
        addSystemPropertyIfPresent(result, "java.version");
        addSystemPropertyIfPresent(result, "os.name");
        addSystemPropertyIfPresent(result, "os.version");
        addSystemPropertyIfPresent(result, "java.compiler");
        addSystemPropertyIfPresent(result, "java.vm.name");
//        addSystemPropertyIfPresent(result, "was.install.root"); // WAS specific
//        addSystemPropertyIfPresent(result, "user.install.root"); // WAS specific
        addSystemPropertyIfPresent(result, "java.home");
//        addSystemPropertyIfPresent(result, "ws.ext.dirs"); // WAS specific
        addSystemPropertyIfPresent(result, "java.class.path");
        addSystemPropertyIfPresent(result, "java.library.path");
        // Add property to know if server is configured to convert depricated
        // messages or not.
//        addSystemPropertyIfPresent(result, "com.ibm.websphere.logging.messageId.version");// WAS specific

        // Add CBE related values
        addSystemPropertyIfPresent(result, "os.arch");
        // try {
        // addIfPresent(result, ServerInstanceLogRecordList.HEADER_HOSTNAME,
        // getHostName());
        // } catch (Throwable t) {
        // // Ignore just don't put anything.
        // }
        addIfPresent(result, ServerInstanceLogRecordList.HEADER_ISZOS, isZOS ? "Y" : null);
        // addIfPresent(result, ServerInstanceLogRecordList.HEADER_ISSERVER,
        // RasHelper.isServer() ? "Y" : null);
        // addIfPresent(result, ServerInstanceLogRecordList.HEADER_ISTHINCLIENT,
        // ManagerAdmin.isThinClient() ? "Y" : null);
        // if (isZos) {
        // addIfPresent(result, ServerInstanceLogRecordList.HEADER_PROCESSNAME,
        // ZRasHelper.ProcessInfo.getPId());
        // addIfPresent(result,
        // ServerInstanceLogRecordList.HEADER_ADDRESSSPACEID,
        // ZRasHelper.ProcessInfo.getAddressSpaceId());
        // addIfPresent(result, ServerInstanceLogRecordList.HEADER_JOBNAME,
        // ZRasHelper.ProcessInfo.getJobName());
        // addIfPresent(result, ServerInstanceLogRecordList.HEADER_SERVER_NAME,
        // ZRasHelper.ProcessInfo.getServer());
        // addIfPresent(result, ServerInstanceLogRecordList.HEADER_JOBID,
        // ZRasHelper.ProcessInfo.getSystemJobId());
        // addIfPresent(result, ServerInstanceLogRecordList.HEADER_SYSTEMNAME,
        // ZRasHelper.ProcessInfo.getSystemName());
        // }

        return result;
    }

    /**
     * Format a <code>Throwable</code> instance for the trace stream.
     * <p>
     *
     * @param t
     *            A non-null Throwable
     * @return a String containing the stack trace of the specified Throwable,
     *         plus any nested exceptions. The string "none" is returned if the
     *         Throwable is null.
     */
    public final static String throwableToString(Throwable t) {
        StringWriter s = new StringWriter();
        PrintWriter p = new PrintWriter(s);
        printStackTrace(t, p);
        return escape(s.toString()); // D512713
    }

    /**
     * Create an HPEL handler. This handler will not split out trace and log data (since retention not that specific).
     * This also speeds up query since no internal merge is done. If separate Trace is needed, simply specify the threshold
     * level on the LogRecordHandler constructor, construct a traceWriter in the same way that the logWriter is specified,
     * and add the traceWriter to the handler
     *
     * @param logRepositoryLoc Location of the log repository
     * @param pid Process ID to use
     * @param label Label to be included in subDirectories or files
     * @param minLevel Minimum level to be handled by this handler
     * @param traceRepositoryLoc If trace records to be handled, this is the location of the trace repository. If trace not
     *            needed, then this can be null
     * @param useDirTree multiLevel hierarchy or flat storage
     * @return
     */
    public static Handler getHpelHandler(String repositoryLoc, String pid, String label,
                                         boolean useDirTree, Properties overrideProps) {
        LogRecordHandler handler = null;
        String methodName = "getHpelHandler";

        try { // Set up log and traceWriter for the handler (trace only if min level < INFO
            handler = new LogRecordHandler(Level.OFF.intValue(), LogRepositoryBaseImpl.KNOWN_FORMATTERS[0],
                            overrideProps); // hpel handler
            File repositoryLocation = new File(repositoryLoc + "logdata");
            LogRepositoryManager manager = new LogRepositoryManagerImpl(repositoryLocation, pid, label, useDirTree);
            LogRepositoryWriter logWriter = new LogRepositoryWriterImpl(manager);
            handler.setLogWriter(logWriter);
        } catch (Exception e) {
            if (log.isLoggable(Level.FINE)) {
                log.logp(Level.FINE, CLASS_NAME, methodName, "Error in setting up handler: " + e);
            }
            return null;
        }
        return handler;
    }

    /**
     * Format the backtrace for the given Throwable onto the given PrintWriter.
     */
    private final static void printStackTrace(Throwable t, PrintWriter p) {
        if (t == null) {
            p.println("none");
            return;
        }

        // Capture any exceptions while printing stack so we return what we have
        // so far
        try {
            // Print the stack trace of the Throwable.
            t.printStackTrace(p);
        } catch (Throwable e) {
            // Do not throw exception, add a message to PrintWriter and return
            // what we have so far.
            p.println("<Encountered exception while printing stack trace>");
            p.println(e.getClass().getName() + ": " + e.getMessage());
            return;
        }

        // Handle the degenerate case of a TraceException which contains an
        // array of Throwables.
//        if (t instanceof TraceException) {
//            TraceException te = (TraceException) t;
//            Throwable subexs[] = te.getSubexceptions();
//            if (subexs == null)
//                return;
//            for (int i = 0; i < subexs.length; i++) {
//                p.println("----- Begin backtrace for subexception[" + i + "]");
//                printStackTrace(subexs[i], p);
//            }
//            return;
//        }

        // Normal Throwables may contain a single nested Throwable. So this
        // Throwable could be the
        // start of a chain of Throwables. We want to obtain the stack traces of
        // all Throwables in
        // the chain so we can see the entire history, execution path and root
        // exception.
        // Some Throwables that support nesting provide support for
        // automatically walking the chain
        // by providing the appropriate printStackTrace() overrides. Others do
        // not, which means we
        // need to manually walk the chain if we want to obtain all the stack
        // traces.
        // If printStackTrace is called on a Throwable chain that contains
        // Throwables that have
        // overriden printStackTrace, the recursion will end after printing out
        // the stack trace of
        // the first Throwable that does not provide an override. If
        // printStackTrace is called on
        // a chain where the base Throwable does not provide the override, then
        // only the stack trace
        // of the base Throwable will have been printed. Therefore, to obtain
        // the next Throwable that
        // needs its stack trace printed, simply walk the chain starting at the
        // base, find the first
        // Throwable that does not provide an override, then start with that
        // guys nested Throwable.

        // updated to support J2SE1.4's nested Throwables.

        boolean autoRecursion = true;
        Throwable tNext = null;

        // Capture any exceptions while calculating nexted exception so we return what we have
        // so far
        try {
            // Compute tNext - the first Throwable in the stack that doesn't recurse
            // properly
            while (autoRecursion) {

                // all known throwables printStackTrace methods recurse properly
                // when cause is set.
                tNext = t.getCause();
                if (tNext != null) {
                    t = tNext;
                } else {
                    tNext = getNestedThrowable(t);
                    if (tNext == null) {
                        // no cause or nested Throwable - done
                        return;
                    }

                    // some of the known throwables with custom nesting don't
                    // recurse properly
                    if (pstRecursesOnNested(t)) {
                        t = tNext;
                    } else {
                        autoRecursion = false;
                    }
                }

            }
        } catch (Throwable e) {
            // Do not throw exception, add a message to PrintWriter and return
            // what we have so far.
            p.println("<Encountered exception while calculating a nested throwable>");
            p.println(e.getClass().getName() + ": " + e.getMessage());
            try {
                // Try to convert that exception into a stack trace.
                e.printStackTrace(p);
            } catch (Throwable e2) {
                p.println("<Caught exception while printing stack trace from failed nested calculation>");
                p.println(e2.getClass().getName() + ": " + e2.getMessage());
            }
            return;
        }

        // tNext will not be null at this point

        // Have found the first Throwable that doesn't include nested throwable
        // in printStackTrace
        // Obtain the nested Throwable and keep going.
        p.println("---- Begin backtrace for Nested Throwables");
        printStackTrace(tNext, p);

        /*
         * while (pstRecurses(t)) { t = getNestedThrowable(t); if (t == null)
         * return; }
         * 
         * // Have found the first Throwable that doesn't override
         * printStackTrace. // Obtain the nested Throwable and keep going. t =
         * getNestedThrowable(t); if (t == null) return;
         * p.println("---- Begin backtrace for Nested Throwables");
         * printStackTrace(t, p);
         */
    }

    /**
     * Returns the thread identifier for the calling thread used in log files.
     * This thread Id is used in WAS log and trace files. Depending on the value
     * assigned to com.ibm.websphere.logging.useJULThreadID system property it
     * will be an eight character hexadecimal string representation of either
     * LogRecord.getThreadId() or Thread.getId() value.
     *
     * @return String the thread id
     */
    public static String getThreadId() {
        // return Integer.toHexString(Thread.currentThread().hashCode());

        if (useJULThreadId) {
            // create a dummy LogRecord
            LogRecord logRecord = new LogRecord(Level.FINE, "x");

            return getThreadId(logRecord);
        } else {
            return threadIdToString(getIntThreadId());
        }
    }

    /**
     * Returns the thread identifier from the supplied LogRecord.
     *
     * @param logRecord LogRecord to retrieve thread id from
     * @return String the thread id
     */
    public static String getThreadId(LogRecord logRecord) {
        return threadIdToString(logRecord.getThreadID());
    }

    /**
     * Converts provided thread id into eight character hexadecimal string
     *
     * @param threadId id to convert
     * @return String representation of the thread id
     */
    public static String threadIdToString(int threadId) {
        StringBuffer buffer = new StringBuffer(8);

        // pad the HexString ThreadId so that it is always 8 characters long.
        for (int shift = 7; shift >= 0; shift--) {
            buffer.append(hexChars[(threadId >> (shift << 2)) & 0xF]);
        }

        return buffer.toString();
    }

    /**
     * Retrieve thread id to use in WAS log and trace files. This method
     * is used by Handlers and takes care of thread id type as well as
     * records published by asynchronous loggers.
     *
     * @param logRecord published log record
     * @return integer value for thread id to use in WAS log and trace files.
     */
    public static int getActiveThreadId(LogRecord logRecord) {
        if (useJULThreadId) {
            return logRecord.getThreadID();
        } else {
            return LogRecordStack.getThreadID();
        }
    }

    /**
     * Retrieves current thread id if it can be used in WAS log and trace files.
     *
     * @return thread id or -1 if LogRecord.getThreadID() should be used instead
     */
    public static int getIntThreadId() {
        return useJULThreadId ? -1 : (int) (Thread.currentThread().getId() & 0xFFFFFFFF);
    }

//    public static byte[] printHeader(OutputStream output, Properties p) { // D422679.1
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        PrintStream ps = new PrintStream(baos);
//        printHeader(ps, p);
//        // Flush the stream and retrieve the byte array.
//        ps.flush();
//        byte[] bytes = baos.toByteArray();
//
//        if (output != null) {
//            try {
//                output.write(bytes);
//            } catch (Throwable t) {
//                // suppress
//            }
//        }
//
//        return bytes;
//    }

    private static void addIfPresent(Properties result, String key, String value) {
        if (value != null) {
            result.put(key, value);
        }
    }

    private static void addSystemPropertyIfPresent(Properties result, String systemProperty) {
        addIfPresent(result, systemProperty, HpelHelper.getSystemProperty(systemProperty));
    }

    // D512713 - method to convert non-printable chars into a printable string for the log.
    private final static String escape(String src) {
        if (src == null) {
            return "";
        }
        StringBuffer result = null;
        for (int i = 0, max = src.length(), delta = 0; i < max; i++) {
            char c = src.charAt(i);
            if (!Character.isWhitespace(c) && Character.isISOControl(c) || Character.getType(c) == Character.UNASSIGNED) {
                String hexVal = Integer.toHexString(c);
                String replacement = "\\u" + ("0000" + hexVal).substring(hexVal.length());
                if (result == null) {
                    result = new StringBuffer(src);
                }
                result.replace(i + delta, i + delta + 1, replacement);
                delta += replacement.length() - 1;
            }
        }
        if (result == null) {
            return src;
        } else {
            return result.toString();
        }
    }

    /**
     * Determine if the specified Throwable contains a nested Throwable and return a reference
     * to that Throwable, if it exists.
     * <p>
     *
     * @param a non-null Throwable
     * @return a reference to the nested Throwable. Null is returned if the specified Throwable
     *         does not support nesting of Throwables or if the nested Throwable is null.
     */
    private final static Throwable getNestedThrowable(Throwable t)
    {
        // This is the current list of Throwables that we know of that support nested
        // exceptions. Add to the list if more show up.

        // D200273
        // Throwables that support nested exceptions using getCause are not special any more since
        // in JDK1.4 that's what it's supposed to be called.

        //    if (t instanceof com.ibm.ws.exception.WsNestedException)					//D200273
        //       return ((com.ibm.ws.exception.WsNestedException)t).getCause();			//D200273
        Class<?> cName = t.getClass();
        if (cName.getName().equals("org.omg.CORBA.portable.UnknownException"))
            return (Throwable) getFieldValue(t, "originalEx");
        if (t instanceof java.rmi.RemoteException)
            return ((java.rmi.RemoteException) t).detail;
        if (t instanceof java.lang.reflect.InvocationTargetException)
            return ((java.lang.reflect.InvocationTargetException) t).getTargetException();
        if (t instanceof javax.naming.NamingException)
            return ((javax.naming.NamingException) t).getRootCause();
        // 131536 package javax.ejb doesnot exist.Implemented Reflection API.
//        if (t instanceof javax.ejb.EJBException)
//            return ((javax.ejb.EJBException)t).getCausedByException();
        if (cName.getName().equals("javax.ejb.EJBException"))
            return invokeMethod(t, "getCausedByException");
        if (t instanceof java.sql.SQLException)
            return ((java.sql.SQLException) t).getNextException();
        if (cName.getName().equals("javax.mail.MessagingException"))
            return invokeMethod(t, "getNextException");
        if (cName.getName().equals("org.xml.sax.SAXException"))
            return invokeMethod(t, "getException");
        //    if (t instanceof javax.xml.transform.TransformerException) 				//D200273
        //      return ((javax.xml.transform.TransformerException)t).getCause();		//D200273
        if (cName.getName().equals("javax.servlet.jsp.JspException"))
            return invokeMethod(t, "getCause");
        if (cName.getName().equals("javax.servlet.ServletException"))
            return invokeMethod(t, "getRootCause");
        if (cName.getName().equals("javax.resource.ResourceException"))
            return invokeMethod(t, "getCause");
        if (cName.getName().equals("javax.jms.JMSException"))
            return invokeMethod(t, "getLinkedException");
        if (t instanceof java.lang.reflect.UndeclaredThrowableException)
            return ((java.lang.reflect.UndeclaredThrowableException) t).getUndeclaredThrowable();
        if (t instanceof java.io.WriteAbortedException)
            return ((java.io.WriteAbortedException) t).detail;
        if (t instanceof java.rmi.server.ServerCloneException)
            return ((java.rmi.server.ServerCloneException) t).detail;
        if (t instanceof java.security.PrivilegedActionException)
            return ((java.security.PrivilegedActionException) t).getException();
        // These Exceptions are not used in WebSphere currently, or the contact has
        // indicated we shouldn't process them. Leave here as comments, in case this changes.
        /*
         * if (t instanceof java.lang.ClassNotFoundException)
         * return ((java.lang.ClassNotFoundException)t).getException();
         */
        return null;
    }

    ///////////////////////////////////////////////////////////
    //
    // Privileged Security Helper Methods for the System class
    //
    //////////////////////////////////////////////////////////

    /**
     * @param t
     * @param string
     * @return
     */
    private static Throwable invokeMethod(Throwable t, String exceptionMethod) {
        Class cName = t.getClass();
        String methodName = "invokeMethod";
        Object obj = null;
        try {
            Method method = getMethod(cName, exceptionMethod);
            if (method != null) {
                obj = method.invoke(t, null);
            }
        } catch (IllegalArgumentException e) {
            if (log.isLoggable(Level.FINE))
                log.logp(Level.FINE, CLASS_NAME, methodName, "The method " + exceptionMethod + " is not an member of the " + cName);
        } catch (IllegalAccessException e) {
            if (log.isLoggable(Level.FINE))
                log.logp(Level.FINE, CLASS_NAME, methodName, "The " + cName + " does not have access to the method " + exceptionMethod);
        } catch (InvocationTargetException e) {
            if (log.isLoggable(Level.FINE)) {
                log.logp(Level.FINE, CLASS_NAME, methodName, "The method threw an exception " + e.getMessage());
            }
        } catch (Throwable e) {
            if (log.isLoggable(Level.FINE)) {
                log.logp(Level.FINE, CLASS_NAME, methodName, "The method threw an exception " + e.getMessage());
            }
        }
        return (Throwable) obj;
    }

    /**
     * @param cName
     * @param exceptionMethod
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Method getMethod(final Class cName, final String exceptionMethod) {
        Method method = null;
        try {
            method = AccessController.doPrivileged(
                            new PrivilegedExceptionAction<Method>() {
                                @Override
                                public Method run() throws Exception {
                                    return cName.getMethod(exceptionMethod, null);
                                }
                            }
                            );
        } catch (PrivilegedActionException e) {
            if (log.isLoggable(Level.FINE)) {
                log.logp(Level.FINE, CLASS_NAME, exceptionMethod, "PrivilegedActionException Cause " + e.getMessage());
            }
        }
        return method;
    }

    private static Object getFieldValue(Throwable t, String fieldName) {
        Object obj = null;
        try {
            Field field = getField(t.getClass(), fieldName);
            if (field != null) {
                obj = field.get(t);
            }
        } catch (Throwable e) {
            if (log.isLoggable(Level.FINE)) {
                log.logp(Level.FINE, CLASS_NAME, fieldName, "The Field.get threw an exception " + e);
            }
        }
        return obj;
    }

    private static Field getField(final Class<?> klass, final String fieldName) {
        Field field = null;
        try {
            field = AccessController.doPrivileged(new PrivilegedExceptionAction<Field>() {
                @Override
                public Field run() throws Exception {
                    return klass.getField(fieldName);
                }
            });
        } catch (PrivilegedActionException e) {
            if (log.isLoggable(Level.FINE)) {
                log.logp(Level.FINE, CLASS_NAME, "getField", "PrivilegedActionException Cause " + e.getCause());
            }
        }
        return field;
    }

    /**
     * Using privileged security, retrieve the specified System property
     * <p>
     *
     * @param propName a non-null, non-empty String that specifies the property. The caller must
     *            guarantee that this String is not null and not the empty String.
     * @return the <code>String</code> value of the specified system property, or null. If there
     *         is no property associated with the specified key, null is returned. If a SecurityException
     *         is encounted while trying to retrieve the value, the exception is logged and absorbed and
     *         null is returned.
     */
    public static String getSystemProperty(String propName)
    {
        final String temp = propName;
        try {
            String prop = AccessController.doPrivileged(
                            new PrivilegedAction<String>() {
                                @Override
                                public String run() {
                                    return System.getProperty(temp);
                                }
                            }
                            );
            return prop;
        } catch (SecurityException se) {
            // LOG THE EXCEPTION
            return null;
        }
    }

    //This method removed for D200273
    /**
     * Determine if the specified Throwable has provided an override for printStackTrace
     * that recurses and prints out the stack of any nested exception.
     * <p>
     *
     * @param a non-null Throwable.
     * @return true if the Throwable provides the appropriate override method, false otherwise.
     */
    /*
     * private final static boolean pstRecurses(Throwable t)
     * {
     * if (t instanceof com.ibm.ws.exception.WsNestedException)
     * return true;
     * if (t instanceof java.rmi.RemoteException)
     * return true;
     * if (t instanceof java.lang.reflect.InvocationTargetException)
     * return true;
     * if (t instanceof javax.naming.NamingException)
     * return true;
     * if (t instanceof javax.ejb.EJBException)
     * return true;
     * if (t instanceof javax.xml.transform.TransformerException)
     * return true;
     * if (t instanceof java.lang.reflect.UndeclaredThrowableException)
     * return true;
     * if (t instanceof java.lang.ClassNotFoundException)
     * return true;
     * if (t instanceof java.rmi.server.ServerCloneException)
     * return true;
     * if (t instanceof java.security.PrivilegedActionException)
     * return true;
     * return false;
     * }
     */

    /**
     * Determine if the specified Throwable prints out the stack of a nested throwable.
     * <p>
     *
     * @param a non-null Throwable.
     * @return true if the Throwable provides the appropriate behaviour, false otherwise
     */
    private final static boolean pstRecursesOnNested(Throwable t)
    {
        //This method added for D200273

        // the following Throwables don't print out the nested throwable when printStackTrace is called
        if (t instanceof org.omg.CORBA.portable.UnknownException)
            return false;
        Class cName = t.getClass();
        if (cName.getName() != null && cName.getName().equals("javax.mail.MessagingException"))
            return false;
        if (t instanceof org.xml.sax.SAXException)
            return false;
        if (cName.getName() != null && cName.getName().equals("javax.servlet.jsp.JspException"))
            return false;
        if (cName.getName() != null && cName.getName().equals("javax.servlet.ServletException"))
            return false;
        if (t instanceof javax.naming.NamingException)
            return false;
        if (t instanceof java.sql.SQLException)
            return false;
        if (cName.getName() != null && cName.getName().equals("javax.jms.JMSException"))
            return false;

        return true;
    }

    public static String getProcessId() {

        return processId;
    }

    /**
     * @param ps
     * @param p
     */
    public static void printHeader(PrintStream ps, Properties p) {
        HpelFormatter formatter = HpelFormatter.getFormatter(HpelFormatter.FORMAT_BASIC);
        if (customFormat != null) {
            formatter.setCustomHeader(customFormat);
        }
        formatter.setHeaderProps(p);

        for (String line : formatter.getHeader()) {
            ps.println(line);
        }
    }

    /**
     * Return a DateFormat object that can be used to format timestamps in the
     * System.out, System.err and TraceOutput logs.
     */
    static DateFormat getBasicDateFormatter()
    {
        String pattern;
        int patternLength;
        int endOfSecsIndex;
        // Retrieve a standard Java DateFormat object with desired format.
        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
        if (formatter instanceof SimpleDateFormat) {
            // Retrieve the pattern from the formatter, since we will need to modify it.
            SimpleDateFormat sdFormatter = (SimpleDateFormat) formatter;
            pattern = sdFormatter.toPattern();
            // Append milliseconds and timezone after seconds
            patternLength = pattern.length();
            endOfSecsIndex = pattern.lastIndexOf('s') + 1;
            String newPattern = pattern.substring(0, endOfSecsIndex) + ":SSS z";
            if (endOfSecsIndex < patternLength)
                newPattern += pattern.substring(endOfSecsIndex, patternLength);
            // 0-23 hour clock (get rid of any other clock formats and am/pm)
            newPattern = newPattern.replace('h', 'H');
            newPattern = newPattern.replace('K', 'H');
            newPattern = newPattern.replace('k', 'H');
            newPattern = newPattern.replace('a', ' ');
            newPattern = newPattern.trim();
            sdFormatter.applyPattern(newPattern);
            formatter = sdFormatter;
        }
        else {
            formatter = new SimpleDateFormat("yy.MM.dd HH:mm:ss:SSS z");
        }
        // PK13288 Start
        if (sysTimeZone != null) {
            formatter.setTimeZone(sysTimeZone);
        }
        // PK13288 End.
        return formatter;
    }

    /**
     * Exception thrown when ownership file is a directory. Message value is the ownership file path.
     */
    public static class OwnershipDirectoryException extends Exception {
        private static final long serialVersionUID = -5222218174834106488L;

        OwnershipDirectoryException(String ownerFile) {
            super(ownerFile);
        }
    }

    /**
     * Exception thrown when file is owned by different server than requested. Message value is the name of the owner server.
     */
    public static class OwnershipServerException extends Exception {
        private static final long serialVersionUID = -2499557857950787416L;

        OwnershipServerException(String otherServer) {
            super(otherServer);
        }
    }
}
