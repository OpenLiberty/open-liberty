/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.common.logging;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.cxf.common.i18n.BundleUtils;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * A container for static utility methods related to logging.
 * By default, CXF logs to java.util.logging. An application can change this. To log to another system, the
 * application must provide an object that extends {@link AbstractDelegatingLogger}, and advertise that class
 * via one of the following mechanisms:
 * <ul>
 * <li>Create a file, in the classpath, named META-INF/cxf/org.apache.cxf.Logger.
 * This file should contain the fully-qualified name
 * of the class, with no comments, on a single line.</li>
 * <li>Call {@link #setLoggerClass(Class)} with a Class<?> reference to the logger class.</li>
 * </ul>
 * CXF provides {@link Slf4jLogger} to use slf4j instead of java.util.logging.
 */

public final class LogUtils {
    public static final String KEY = "org.apache.cxf.Logger";

    private static final Object[] NO_PARAMETERS = new Object[0];

    private static Class<?> loggerClass = null;

    /**
     * Prevents instantiation.
     */
    private LogUtils() {}

// Liberty Change for CXF Begin
//    static {
//        JDKBugHacks.doHacks();
//
//        try {
//
//            String cname = null;
//            try {
//                cname = AccessController.doPrivileged(new PrivilegedAction<String>() {
//                    @Override
//                    public String run() {
//                        return System.getProperty(KEY);
//                    }
//                });
//            } catch (Throwable t) {
//                //ignore - likely security exception or similar that won't allow
//                //access to the system properties.   We'll continue with other methods
//            }
//            if (StringUtils.isEmpty(cname)) {
//                InputStream ins = Thread.currentThread().getContextClassLoader()
//                                .getResourceAsStream("META-INF/cxf/" + KEY);
//                if (ins == null) {
//                    ins = ClassLoader.getSystemResourceAsStream("META-INF/cxf/" + KEY);
//                }
//                if (ins != null) {
//                    BufferedReader din = new BufferedReader(new InputStreamReader(ins));
//                    try {
//                        cname = din.readLine();
//                    } finally {
//                        din.close();
//                    }
//                }
//            }
//            if (StringUtils.isEmpty(cname)) {
//                try {
//                    // This Class.forName likely will barf in OSGi, but it's OK
//                    // as we'll just use j.u.l and pax-logging will pick it up fine
//                    // If we don't call this and there isn't a slf4j impl avail,
//                    // you get warnings printed to stderr about NOPLoggers and such
//                    Class.forName("org.slf4j.impl.StaticLoggerBinder");
//                    Class<?> cls = Class.forName("org.slf4j.LoggerFactory");
//                    Class<?> fcls = cls.getMethod("getILoggerFactory").invoke(null).getClass();
//                    String clsName = fcls.getName();
//                    if (clsName.contains("NOPLogger")) {
//                        //no real slf4j implementation, use j.u.l
//                        cname = null;
//                    } else if (clsName.contains("Log4j")) {
//                        cname = "org.apache.cxf.common.logging.Log4jLogger";
//                    } else if (clsName.contains("JCL")) {
//                        cls = Class.forName("org.apache.commons.logging.LogFactory");
//                        fcls = cls.getMethod("getFactory").invoke(null).getClass();
//                        if (fcls.getName().contains("Log4j")) {
//                            cname = "org.apache.cxf.common.logging.Log4jLogger";
//                        }
//                    } else if (clsName.contains("JDK14")
//                               || clsName.contains("pax.logging")) {
//                        //both of these we can use the appropriate j.u.l API's
//                        //directly and have it work properly
//                        cname = null;
//                    } else {
//                        // Cannot really detect where it's logging so we'll
//                        // go ahead and use the Slf4jLogger directly
//                        cname = "org.apache.cxf.common.logging.Slf4jLogger";
//                    }
//                } catch (Throwable t) {
//                    //ignore - Slf4j not available
//                }
//            }
//            if (!StringUtils.isEmpty(cname)) {
//                try {
//                    loggerClass = Class.forName(cname.trim(), true,
//                                                Thread.currentThread().getContextClassLoader());
//                } catch (Throwable ex) {
//                    loggerClass = Class.forName(cname.trim());
//                }
//                getLogger(LogUtils.class).fine("Using " + loggerClass.getName() + " for logging.");
//            }
//        } catch (Throwable ex) {
//            //ignore - if we get here, some issue prevented the logger class from being loaded.
//            //maybe a ClassNotFound or NoClassDefFound or similar.   Just use j.u.l
//            loggerClass = null;
//        }
//    }
//Liberty Change for CXF End

    /**
     * Specify a logger class that inherits from {@link AbstractDelegatingLogger}.
     * Enable users to use their own logger implementation.
     */
    public static void setLoggerClass(Class<? extends AbstractDelegatingLogger> cls) {
        loggerClass = cls;
    }

    /**
     * Get a Logger with the associated default resource bundle for the class.
     *
     * @param cls the Class to contain the Logger
     * @return an appropriate Logger
     */
    public static Logger getLogger(Class<?> cls) {
        //Liberty Change for CXF Begain
        return createLogger(cls, null, cls.getName() + getClassLoader(cls));
        //Liberty Change for CXF End
    }

    /**
     * Get a Logger with an associated resource bundle.
     *
     * @param cls the Class to contain the Logger
     * @param resourcename the resource name
     * @return an appropriate Logger
     */
    public static Logger getLogger(Class<?> cls, String resourcename) {
        //Liberty Change for CXF Begain
        return createLogger(cls, resourcename, cls.getName() + getClassLoader(cls));
        //Liberty Change for CXF End
    }

    /**
     * Get a Logger with an associated resource bundle.
     *
     * @param cls the Class to contain the Logger (to find resources)
     * @param resourcename the resource name
     * @param loggerName the full name for the logger
     * @return an appropriate Logger
     */
    public static Logger getLogger(Class<?> cls,
                                   String resourcename,
                                   String loggerName) {
        return createLogger(cls, resourcename, loggerName);
    }

    /**
     * Get a Logger with the associated default resource bundle for the class.
     *
     * @param cls the Class to contain the Logger
     * @return an appropriate Logger
     */
    public static Logger getL7dLogger(Class<?> cls) {
        //Liberty Change for CXF Begin
        return createLogger(cls, null, cls.getName() + getClassLoader(cls));
        //Liberty Change for CXF End
    }

    /**
     * Get a Logger with an associated resource bundle.
     *
     * @param cls the Class to contain the Logger
     * @param resourcename the resource name
     * @return an appropriate Logger
     */
    public static Logger getL7dLogger(Class<?> cls, String resourcename) {
        //Liberty Change for CXF Begain
        return createLogger(cls, resourcename, cls.getName() + getClassLoader(cls));
        //Liberty Change for CXF End
    }

    /**
     * Get a Logger with an associated resource bundle.
     *
     * @param cls the Class to contain the Logger (to find resources)
     * @param resourcename the resource name
     * @param loggerName the full name for the logger
     * @return an appropriate Logger
     */
    public static Logger getL7dLogger(Class<?> cls,
                                      String resourcename,
                                      String loggerName) {
        return createLogger(cls, resourcename, loggerName);
    }

    /**
     * Create a logger
     */
    @FFDCIgnore({ MissingResourceException.class, IllegalArgumentException.class })
    protected static Logger createLogger(final Class<?> cls,
                                         String name,
                                         String loggerName) {
        ClassLoader orig = getContextClassLoader();
        ClassLoader n = getClassLoader(cls);
        if (n != null) {
            setContextClassLoader(n);
        }
        String bundleName = name;
        try {
            Logger logger = null;
            ResourceBundle b = null;
            if (bundleName == null) {
                //grab the bundle prior to the call to Logger.getLogger(...) so the
                //ResourceBundle can be loaded outside the big sync block that getLogger really is
                bundleName = BundleUtils.getBundleName(cls);
                try {
                    b = BundleUtils.getBundle(cls);
                } catch (MissingResourceException rex) {
                    //ignore
                }
            } else {
                bundleName = BundleUtils.getBundleName(cls, bundleName);
                try {
                    b = BundleUtils.getBundle(cls, bundleName);
                } catch (MissingResourceException rex) {
                    //ignore
                }
            }
            if (b != null) {
                b.getLocale();
            }

            if (loggerClass != null) {
                try {
                    Constructor<?> cns = loggerClass.getConstructor(String.class, String.class);
                    if (name == null) {
                        try {
                            return (Logger) cns.newInstance(loggerName, bundleName);
                        } catch (InvocationTargetException ite) {
                            if (ite.getTargetException() instanceof MissingResourceException) {
                                return (Logger) cns.newInstance(loggerName, null);
                            }
                            throw ite;
                        }
                    }
                    try {
                        return (Logger) cns.newInstance(loggerName, bundleName);
                    } catch (InvocationTargetException ite) {
                        if (ite.getTargetException() instanceof MissingResourceException) {
                            throw (MissingResourceException) ite.getTargetException();
                        }
                        throw ite;
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            try {
                logger = Logger.getLogger(loggerName, bundleName); //NOPMD
            } catch (IllegalArgumentException iae) {
                //likely a mismatch on the bundle name, just return the default
                logger = Logger.getLogger(loggerName); //NOPMD
            } catch (MissingResourceException rex) {
                logger = Logger.getLogger(loggerName); //NOPMD
            } finally {
                b = null;
            }
            return logger;
        } finally {
            if (n != orig) {
                setContextClassLoader(orig);
            }
        }
    }

    private static void setContextClassLoader(final ClassLoader classLoader) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
               @Override
                public Object run() {
                    Thread.currentThread().setContextClassLoader(classLoader);
                    return null;
                }
            });
        } else {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
    }

    private static ClassLoader getContextClassLoader() {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    return Thread.currentThread().getContextClassLoader();
                }
            });
        }
        return Thread.currentThread().getContextClassLoader();
    }

    private static ClassLoader getClassLoader(final Class<?> clazz) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    return clazz.getClassLoader();
                }
            });
        }
        return clazz.getClassLoader();
    }
    /**
     * Allows both parameter substitution and a typed Throwable to be logged.
     *
     * @param logger the Logger the log to
     * @param level the severity level
     * @param message the log message
     * @param throwable the Throwable to log
     * @param parameter the parameter to substitute into message
     */
    public static void log(Logger logger,
                           Level level,
                           String message,
                           Throwable throwable,
                           Object parameter) {
        if (logger.isLoggable(level)) {
            final String formattedMessage =
                            MessageFormat.format(localize(logger, message), parameter);
            doLog(logger, level, formattedMessage, throwable);
        }
    }

    /**
     * Allows both parameter substitution and a typed Throwable to be logged.
     *
     * @param logger the Logger the log to
     * @param level the severity level
     * @param message the log message
     * @param throwable the Throwable to log
     * @param parameters the parameters to substitute into message
     */
    public static void log(Logger logger,
                           Level level,
                           String message,
                           Throwable throwable,
                           Object... parameters) {
        if (logger.isLoggable(level)) {
            final String formattedMessage =
                            MessageFormat.format(localize(logger, message), parameters);
            doLog(logger, level, formattedMessage, throwable);
        }
    }

    /**
     * Checks log level and logs
     *
     * @param logger the Logger the log to
     * @param level the severity level
     * @param message the log message
     */
    public static void log(Logger logger,
                           Level level,
                           String message) {
        log(logger, level, message, NO_PARAMETERS);
    }

    /**
     * Checks log level and logs
     *
     * @param logger the Logger the log to
     * @param level the severity level
     * @param message the log message
     * @param throwable the Throwable to log
     */
    public static void log(Logger logger,
                           Level level,
                           String message,
                           Throwable throwable) {
        log(logger, level, message, throwable, NO_PARAMETERS);
    }

    /**
     * Checks log level and logs
     *
     * @param logger the Logger the log to
     * @param level the severity level
     * @param message the log message
     * @param parameter the parameter to substitute into message
     */
    public static void log(Logger logger,
                           Level level,
                           String message,
                           Object parameter) {
        log(logger, level, message, new Object[] { parameter });
    }

    /**
     * Checks log level and logs
     *
     * @param logger the Logger the log to
     * @param level the severity level
     * @param message the log message
     * @param parameters the parameters to substitute into message
     */
    public static void log(Logger logger,
                           Level level,
                           String message,
                           Object[] parameters) {
        if (logger.isLoggable(level)) {
            String msg = localize(logger, message);
            try {
                msg = MessageFormat.format(msg, parameters);
            } catch (IllegalArgumentException ex) {
                //ignore, log as is
            }
            doLog(logger, level, msg, null);
        }
    }

    private static void doLog(Logger log, Level level, String msg, Throwable t) {
        LogRecord record = new LogRecord(level, msg);

        record.setLoggerName(log.getName());
        record.setResourceBundleName(log.getResourceBundleName());
        record.setResourceBundle(log.getResourceBundle());

        if (t != null) {
            record.setThrown(t);
        }

        //try to get the right class name/method name - just trace
        //back the stack till we get out of this class
        StackTraceElement stack[] = (new Throwable()).getStackTrace();
        String cname = LogUtils.class.getName();
        for (int x = 0; x < stack.length; x++) {
            StackTraceElement frame = stack[x];
            if (!frame.getClassName().equals(cname)) {
                record.setSourceClassName(frame.getClassName());
                record.setSourceMethodName(frame.getMethodName());
                break;
            }
        }
        log.log(record);
    }

    /**
     * Retrieve localized message retrieved from a logger's resource
     * bundle.
     *
     * @param logger the Logger
     * @param message the message to be localized
     */
    private static String localize(Logger logger, String message) {
        ResourceBundle bundle = logger.getResourceBundle();
        try {
            return bundle != null ? bundle.getString(message) : message;
        } catch (MissingResourceException ex) {
            //string not in the bundle
            return message;
        }
    }

}
