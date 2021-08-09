/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.logging;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;


/**
 * MyfacesLogger wraps JDK 1.4 logging to provided a large number of 
 * extra convenience methods.  MyfacesLoggers are created off of
 * Packages or Classes (not arbitrary names) to force
 * proper logging hierarchies.
 * 
 * Has predefined Logger for javax.faces related packages, like
 * {@link MyfacesLogger#APPLICATION_LOGGER} for javax.faces.application and related
 * 
 * Original code copied from TrinidadLogger
 * 
 */
public class MyfacesLogger
{

    private static final String LOGGER_NAME_PREFIX = "org.apache.myfaces.";

    /** for javax.faces.application and related  */
    public static final MyfacesLogger APPLICATION_LOGGER
            = MyfacesLogger.createMyfacesLogger(LOGGER_NAME_PREFIX + "application");

    /** for javax.faces.component and related  */
    public static final MyfacesLogger COMPONENT_LOGGER
            = MyfacesLogger.createMyfacesLogger(LOGGER_NAME_PREFIX + "component");

    /** for javax.faces.component.html and related  */
    public static final MyfacesLogger COMPONENT_HTML_LOGGER
            = MyfacesLogger.createMyfacesLogger(LOGGER_NAME_PREFIX + "component.html");

    /** for javax.faces.component.behavior and related  */
    public static final MyfacesLogger COMPONENT_BEHAVIOR_LOGGER
            = MyfacesLogger.createMyfacesLogger(LOGGER_NAME_PREFIX + "component.behavior");

    /** for javax.faces.component.visit and related  */
    public static final MyfacesLogger COMPONENT_VISIT_LOGGER
            = MyfacesLogger.createMyfacesLogger(LOGGER_NAME_PREFIX + "component.visit");

    /** for javax.faces.context and related  */
    public static final MyfacesLogger CONTEXT_LOGGER
            = MyfacesLogger.createMyfacesLogger(LOGGER_NAME_PREFIX + "context");

    /** for javax.faces.convert and related  */
    public static final MyfacesLogger CONVERT_LOGGER
            = MyfacesLogger.createMyfacesLogger(LOGGER_NAME_PREFIX + "convert");

    /** for javax.faces.event and related  */
    public static final MyfacesLogger EVENT_LOGGER
            = MyfacesLogger.createMyfacesLogger(LOGGER_NAME_PREFIX + "event");

    /** for javax.faces.lifecycle and related  */
    public static final MyfacesLogger LIFECYCLE_LOGGER
            = MyfacesLogger.createMyfacesLogger(LOGGER_NAME_PREFIX + "lifecycle");

    /** for javax.faces.model and related  */
    public static final MyfacesLogger MODEL_LOGGER
            = MyfacesLogger.createMyfacesLogger(LOGGER_NAME_PREFIX + "model");

    /** for javax.faces.render and related  */
    public static final MyfacesLogger RENDER_LOGGER
            = MyfacesLogger.createMyfacesLogger(LOGGER_NAME_PREFIX + "render");

    /** for javax.faces.validator and related  */
    public static final MyfacesLogger VALIDATOR_LOGGER
            = MyfacesLogger.createMyfacesLogger(LOGGER_NAME_PREFIX + "validator");

    /** for javax.faces.view and related  */
    public static final MyfacesLogger VIEW_LOGGER
            = MyfacesLogger.createMyfacesLogger(LOGGER_NAME_PREFIX + "view");

    /** for javax.faces.view.facelets and related  */
    public static final MyfacesLogger VIEW_FACELETS_LOGGER
            = MyfacesLogger.createMyfacesLogger(LOGGER_NAME_PREFIX + "view.facelets");

    /** for {@link javax.faces.application.Resource} and related  (does not have own javax.faces. package) */
    public static final MyfacesLogger RESOURCE_LOGGER
            = MyfacesLogger.createMyfacesLogger(LOGGER_NAME_PREFIX + "resource");

    /** for myfaces config */ 
    public static final MyfacesLogger CONFIG_LOGGER
            = MyfacesLogger.createMyfacesLogger(LOGGER_NAME_PREFIX + "config");


    private MyfacesLogger(Logger log)
    {
        _log = log;
    }

    /**
     * Get the Java logger from an Myfaces Logger.
     * 
     * @return a java Logger instance
     */
    public Logger getLogger()
    {
        return _log;
    }

    /**
     * Find or create a logger for a named subsystem.  If a logger has
     * already been created with the given name it is returned.  Otherwise
     * a new logger is created.
     * <p>
     * If a new logger is created its log level will be configured
     * based on the LogManager configuration and it will configured
     * to also send logging output to its parent's handlers.  It will
     * be registered in the LogManager global namespace.
     * 
     * @param name        A name for the logger.  This should
     *                be a dot-separated name and should normally
     *                be based on the package name or class name
     *                of the subsystem, such as java.net
     *                or javax.swing
     * @return a suitable Logger
     */
    private static MyfacesLogger createMyfacesLogger(String name) 
    {
        if (name == null)
        {
            throw new IllegalArgumentException(_LOG.getMessage(
                    "LOGGER_NAME_REQUIRED"));
        }

        Logger log;

        if (name.startsWith("javax.faces"))
        {
            log = Logger.getLogger(name, _API_LOGGER_BUNDLE);
        }
        else if (name.startsWith("org.apache.myfaces."))
        {
            log = Logger.getLogger(name, _IMPL_LOGGER_BUNDLE);
        }
        else
        {
            log = Logger.getLogger(name);
        }

        return new MyfacesLogger(log);
    }

    /**
     * Find or create a logger for a named subsystem.  If a logger has 
     * already been created with the given name it is returned.  Otherwise
     * a new logger is created.
     * <p>
     * If a new logger is created its log level will be configured
     * based on the LogManager and it will configured to also send logging
     * output to its parent loggers Handlers.  It will be registered in
     * the LogManager global namespace.
     * <p>
     * If the named Logger already exists and does not yet have a
     * localization resource bundle then the given resource bundle 
     * name is used.  If the named Logger already exists and has
     * a different resource bundle name then an IllegalArgumentException
     * is thrown.
     * <p>
     * @param name    A name for the logger.  This should
     *                be a dot-separated name and should normally
     *                be based on the package name or class name
     *                of the subsystem, such as java.net
     *                or javax.swing
     * @param     resourceBundleName  name of ResourceBundle to be used for localizing
     *                messages for this logger.
     * @return a suitable Logger
     * @throws MissingResourceException if the named ResourceBundle cannot be found.
     * @throws IllegalArgumentException if the Logger already exists and uses
     *           a different resource bundle name.
     */
    private static MyfacesLogger createMyfacesLogger(String name, String resourceBundleName) 
    {
        if (name == null)
        {
            throw new IllegalArgumentException(_LOG.getMessage(
                    "LOGGER_NAME_REQUIRED"));
        }

        Logger log = Logger.getLogger(name, resourceBundleName);

        return new MyfacesLogger(log);
    }


    /**
     * Find or create a logger for a named subsystem.  If a logger has
     * already been created with the given name it is returned.  Otherwise
     * a new logger is created.
     * <p>
     * If a new logger is created its log level will be configured
     * based on the LogManager configuration and it will configured
     * to also send logging output to its parent's handlers.  It will
     * be registered in the LogManager global namespace.
     * 
     * @param c       A class instance for the logger.  
     * @return a suitable Logger
     */
    public static MyfacesLogger createMyfacesLogger(Class<?> c) 
    {
        if (c == null)
        {
            throw new IllegalArgumentException(_LOG.getMessage(
                    "CLASS_REQUIRED"));
        }
        String name = c.getName();
        return createMyfacesLogger(name);
    }

    /**
     * Find or create a logger for a named subsystem.  If a logger has
     * already been created with the given name it is returned.  Otherwise
     * a new logger is created.
     * <p>
     * If a new logger is created its log level will be configured
     * based on the LogManager configuration and it will configured
     * to also send logging output to its parent's handlers.  It will
     * be registered in the LogManager global namespace.
     * 
     * @param c       A class instance for the logger.  
     * @param     resourceBundleName  name of ResourceBundle to be used for localizing
     *                messages for this logger.
     *        
     * @return a suitable Logger
     */
    public static MyfacesLogger createMyfacesLogger(Class<?> c, String resourceBundleName) 
    {
        if (c == null)
        {
            throw new IllegalArgumentException(_LOG.getMessage(
                    "CLASS_REQUIRED"));
        }
        String name = c.getName();
        return createMyfacesLogger(name, resourceBundleName);
    }

    /**
     * Find or create a logger for a named subsystem.  If a logger has
     * already been created with the given name it is returned.  Otherwise
     * a new logger is created.
     * <p>
     * If a new logger is created its log level will be configured
     * based on the LogManager configuration and it will configured
     * to also send logging output to its parent's handlers.  It will
     * be registered in the LogManager global namespace.
     * 
     * @param p       A Package instance for the logger.  
     * @return a suitable Logger
     */

    public static MyfacesLogger createMyfacesLogger(Package p)
    {
        if (p == null)
        {
            throw new IllegalArgumentException(_LOG.getMessage(
                    "PACKAGE_REQUIRED"));
        }
        String name = p.getName();
        return createMyfacesLogger(name);
    }

    /**
     * Find or create a logger for a named subsystem.  If a logger has
     * already been created with the given name it is returned.  Otherwise
     * a new logger is created.
     * <p>
     * If a new logger is created its log level will be configured
     * based on the LogManager configuration and it will configured
     * to also send logging output to its parent's handlers.  It will
     * be registered in the LogManager global namespace.
     * 
     * @param p       A Package instance for the logger.  
     * @param     resourceBundleName  name of ResourceBundle to be used for localizing
     *                messages for this logger.
     *        
     * @return a suitable Logger
     */

    public static MyfacesLogger createMyfacesLogger(Package p, String resourceBundleName)
    {
        if (p == null)
        {
            throw new IllegalArgumentException(_LOG.getMessage(
                    "PACKAGE_REQUIRED"));
        }
        String name = p.getName();
        return createMyfacesLogger(name, resourceBundleName);
    }

    /**
     * Log a LogRecord.
     * <p>
     * All the other logging methods in this class call through
     * this method to actually perform any logging.  Subclasses can
     * override this single method to capture all log activity.
     *
     * @param record the LogRecord to be published
     */
    public void log(LogRecord record) 
    {
        _log.log(record);
    }


    //================================================================
    // Start of convenience methods WITHOUT className and methodName
    //================================================================

    /**
     * Log a message, with no arguments.
     * <p>
     * If the logger is currently enabled for the given message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg   The string message (or a key in the message catalog)
     */
    public void log(String msg) 
    {
        log(Level.FINE, msg);
    }

    /**
     * Log a message, with no arguments.
     * <p>
     * If the logger is currently enabled for the given message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param level   One of the message level identifiers, e.g. SEVERE
     * @param   msg   The string message (or a key in the message catalog)
     */
    public void log(Level level, String msg) 
    {
        if (isLoggable(level))
        {
            MyfacesLogRecord lr = new MyfacesLogRecord(level, msg);
            doLog(lr);  
        }
    }

    /**
     * Log a message, with one object parameter.
     * <p>
     * If the logger is currently enabled for the given message 
     * level then a corresponding LogRecord is created and forwarded 
     * to all the registered output Handler objects.
     * <p>
     * @param level   One of the message level identifiers, e.g. SEVERE
     * @param   msg   The string message (or a key in the message catalog)
     * @param   param1    parameter to the message
     */
    public void log(Level level, String msg, Object param1) 
    {
        if (isLoggable(level))
        {
            MyfacesLogRecord lr = new MyfacesLogRecord(level, msg);
            Object params[] = { param1};
            lr.setParameters(params);
            doLog(lr);  
        }
    }

    /**
     * Log a message, with an array of object arguments.
     * <p>
     * If the logger is currently enabled for the given message 
     * level then a corresponding LogRecord is created and forwarded 
     * to all the registered output Handler objects.
     * <p>
     * @param level   One of the message level identifiers, e.g. SEVERE
     * @param   msg   The string message (or a key in the message catalog)
     * @param   params    array of parameters to the message
     */
    public void log(Level level, String msg, Object params[]) 
    {
        if (isLoggable(level))
        {
            MyfacesLogRecord lr = new MyfacesLogRecord(level, msg);
            lr.setParameters(params);
            doLog(lr);  
        }
    }

    /**
     * Log a message, with associated Throwable information.
     * <p>
     * If the logger is currently enabled for the given message 
     * level then the given arguments are stored in a LogRecord
     * which is forwarded to all registered output handlers.
     * <p>
     * Note that the thrown argument is stored in the LogRecord thrown
     * property, rather than the LogRecord parameters property.  Thus is it
     * processed specially by output Formatters and is not treated
     * as a formatting parameter to the LogRecord message property.
     * <p>
     * @param level   One of the message level identifiers, e.g. SEVERE
     * @param   msg   The string message (or a key in the message catalog)
     * @param   thrown  Throwable associated with log message.
     */
    public void log(Level level, String msg, Throwable thrown) 
    {
        if (isLoggable(level))
        {
            MyfacesLogRecord lr = new MyfacesLogRecord(level, msg);
            lr.setThrown(thrown);
            doLog(lr);  
        }
    }

    //================================================================
    // Start of convenience methods WITH className and methodName
    //================================================================

    /**
     * Log a message, specifying source class and method,
     * with no arguments.
     * <p>
     * If the logger is currently enabled for the given message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param level   One of the message level identifiers, e.g. SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   msg   The string message (or a key in the message catalog)
     */
    public void logp(Level level, String sourceClass, String sourceMethod, String msg) 
    {
        if (isLoggable(level))
        {
            MyfacesLogRecord lr = new MyfacesLogRecord(level, msg);
            lr.setSourceClassName(sourceClass);
            lr.setSourceMethodName(sourceMethod);
            doLog(lr);  
        }
    }

    /**
     * Log a message, specifying source class and method,
     * with a single object parameter to the log message.
     * <p>
     * If the logger is currently enabled for the given message 
     * level then a corresponding LogRecord is created and forwarded 
     * to all the registered output Handler objects.
     * <p>
     * @param level   One of the message level identifiers, e.g. SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   msg    The string message (or a key in the message catalog)
     * @param   param1    Parameter to the log message.
     */
    public void logp(Level level, String sourceClass, String sourceMethod,
            String msg, Object param1) 
    {
        if (isLoggable(level))
        {
            MyfacesLogRecord lr = new MyfacesLogRecord(level, msg);
            lr.setSourceClassName(sourceClass);
            lr.setSourceMethodName(sourceMethod);
            Object params[] = { param1};
            lr.setParameters(params);
            doLog(lr);  
        }
    }

    /**
     * Log a message, specifying source class and method,
     * with an array of object arguments.
     * <p>
     * If the logger is currently enabled for the given message 
     * level then a corresponding LogRecord is created and forwarded 
     * to all the registered output Handler objects.
     * <p>
     * @param level   One of the message level identifiers, e.g. SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   msg   The string message (or a key in the message catalog)
     * @param   params    Array of parameters to the message
     */
    public void logp(Level level, String sourceClass, String sourceMethod,
            String msg, Object params[]) 
    {
        if (isLoggable(level))
        {
            MyfacesLogRecord lr = new MyfacesLogRecord(level, msg);
            lr.setSourceClassName(sourceClass);
            lr.setSourceMethodName(sourceMethod);
            lr.setParameters(params);
            doLog(lr);  
        }
    }

    /**
     * Log a message, specifying source class and method,
     * with associated Throwable information.
     * <p>
     * If the logger is currently enabled for the given message 
     * level then the given arguments are stored in a LogRecord
     * which is forwarded to all registered output handlers.
     * <p>
     * Note that the thrown argument is stored in the LogRecord thrown
     * property, rather than the LogRecord parameters property.  Thus is it
     * processed specially by output Formatters and is not treated
     * as a formatting parameter to the LogRecord message property.
     * <p>
     * @param level   One of the message level identifiers, e.g. SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   msg   The string message (or a key in the message catalog)
     * @param   thrown  Throwable associated with log message.
     */
    public void logp(Level level, String sourceClass, String sourceMethod,
            String msg, Throwable thrown) 
    {
        if (isLoggable(level))
        {
            MyfacesLogRecord lr = new MyfacesLogRecord(level, msg);
            lr.setSourceClassName(sourceClass);
            lr.setSourceMethodName(sourceMethod);
            lr.setThrown(thrown);
            doLog(lr);  
        }
    }


    /**
     * Log a message, specifying source class, method, and resource bundle name
     * with no arguments.
     * <p>
     * If the logger is currently enabled for the given message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * The msg string is localized using the named resource bundle.  If the
     * resource bundle name is null, then the msg string is not localized.
     * <p>
     * @param level   One of the message level identifiers, e.g. SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   bundleName     name of resource bundle to localize msg
     * @param   msg   The string message (or a key in the message catalog)
     * @throws  MissingResourceException if no suitable ResourceBundle can
     *        be found.
     */

    public void logrb(Level level, String sourceClass, String sourceMethod, 
            String bundleName, String msg) 
    {
        if (isLoggable(level))
        {
            MyfacesLogRecord lr = new MyfacesLogRecord(level, msg);
            lr.setSourceClassName(sourceClass);
            lr.setSourceMethodName(sourceMethod);
            doLog(lr,bundleName);  
        }
    }

    /**
     * Log a message, specifying source class, method, and resource bundle name,
     * with a single object parameter to the log message.
     * <p>
     * If the logger is currently enabled for the given message 
     * level then a corresponding LogRecord is created and forwarded 
     * to all the registered output Handler objects.
     * <p>
     * The msg string is localized using the named resource bundle.  If the
     * resource bundle name is null, then the msg string is not localized.
     * <p>
     * @param level   One of the message level identifiers, e.g. SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   bundleName     name of resource bundle to localize msg
     * @param   msg    The string message (or a key in the message catalog)
     * @param   param1    Parameter to the log message.
     * @throws  MissingResourceException if no suitable ResourceBundle can
     *        be found.
     */
    public void logrb(Level level, String sourceClass, String sourceMethod,
            String bundleName, String msg, Object param1) 
    {
        if (isLoggable(level))
        {
            MyfacesLogRecord lr = new MyfacesLogRecord(level, msg);
            lr.setSourceClassName(sourceClass);
            lr.setSourceMethodName(sourceMethod);
            Object params[] = { param1};
            lr.setParameters(params);
            doLog(lr,bundleName);  
        }
    }

    /**
     * Log a message, specifying source class, method, and resource bundle name,
     * with an array of object arguments.
     * <p>
     * If the logger is currently enabled for the given message 
     * level then a corresponding LogRecord is created and forwarded 
     * to all the registered output Handler objects.
     * <p>
     * The msg string is localized using the named resource bundle.  If the
     * resource bundle name is null, then the msg string is not localized.
     * <p>
     * @param level   One of the message level identifiers, e.g. SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   bundleName     name of resource bundle to localize msg
     * @param   msg   The string message (or a key in the message catalog)
     * @param   params    Array of parameters to the message
     * @throws  MissingResourceException if no suitable ResourceBundle can
     *        be found.
     */
    public void logrb(Level level, String sourceClass, String sourceMethod,
            String bundleName, String msg, Object params[]) 
    {
        if (isLoggable(level))
        {
            MyfacesLogRecord lr = new MyfacesLogRecord(level, msg);
            lr.setSourceClassName(sourceClass);
            lr.setSourceMethodName(sourceMethod);
            lr.setParameters(params);
            doLog(lr,bundleName);  
        }
    }

    /**
     * Log a message, specifying source class, method, and resource bundle name,
     * with associated Throwable information.
     * <p>
     * If the logger is currently enabled for the given message 
     * level then the given arguments are stored in a LogRecord
     * which is forwarded to all registered output handlers.
     * <p>
     * The msg string is localized using the named resource bundle.  If the
     * resource bundle name is null, then the msg string is not localized.
     * <p>
     * Note that the thrown argument is stored in the LogRecord thrown
     * property, rather than the LogRecord parameters property.  Thus is it
     * processed specially by output Formatters and is not treated
     * as a formatting parameter to the LogRecord message property.
     * <p>
     * @param level   One of the message level identifiers, e.g. SEVERE
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that issued the logging request
     * @param   bundleName     name of resource bundle to localize msg
     * @param   msg   The string message (or a key in the message catalog)
     * @param   thrown  Throwable associated with log message.
     * @throws  MissingResourceException if no suitable ResourceBundle can
     *        be found.
     */
    public void logrb(Level level, String sourceClass, String sourceMethod,
            String bundleName, String msg, Throwable thrown) 
    {
        if (isLoggable(level))
        {
            MyfacesLogRecord lr = new MyfacesLogRecord(level, msg);
            lr.setSourceClassName(sourceClass);
            lr.setSourceMethodName(sourceMethod);
            lr.setThrown(thrown);
            doLog(lr,bundleName);  
        }
    }


    //======================================================================
    // Start of convenience methods for logging method entries and returns.
    //======================================================================

    /**
     * Log a method entry.
     * <p>
     * This is a convenience method that can be used to log entry
     * to a method.  A LogRecord with message "ENTRY", log level
     * FINER, and the given sourceMethod and sourceClass is logged.
     * <p>
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that is being entered
     */
    public void entering(String sourceClass, String sourceMethod) 
    {
        _log.entering(sourceClass, sourceMethod);
    }

    /**
     * Log a method entry, with one parameter.
     * <p>
     * This is a convenience method that can be used to log entry
     * to a method.  A LogRecord with message "ENTRY {0}", log level
     * FINER, and the given sourceMethod, sourceClass, and parameter
     * is logged.
     * <p>
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that is being entered
     * @param   param1           parameter to the method being entered
     */
    public void entering(String sourceClass, String sourceMethod, Object param1) 
    {
        _log.entering(sourceClass, sourceMethod, param1);
    }

    /**
     * Log a method entry, with an array of parameters.
     * <p>
     * This is a convenience method that can be used to log entry
     * to a method.  A LogRecord with message "ENTRY" (followed by a 
     * format {N} indicator for each entry in the parameter array), 
     * log level FINER, and the given sourceMethod, sourceClass, and 
     * parameters is logged.
     * <p>
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of method that is being entered
     * @param   params           array of parameters to the method being entered
     */
    public void entering(String sourceClass, String sourceMethod, Object params[]) 
    {
        _log.entering(sourceClass, sourceMethod, params);
    }

    /**
     * Log a method return.
     * <p>
     * This is a convenience method that can be used to log returning
     * from a method.  A LogRecord with message "RETURN", log level
     * FINER, and the given sourceMethod and sourceClass is logged.
     * <p>
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of the method 
     */
    public void exiting(String sourceClass, String sourceMethod) 
    {
        _log.exiting(sourceClass, sourceMethod);
    }


    /**
     * Log a method return, with result object.
     * <p>
     * This is a convenience method that can be used to log returning
     * from a method.  A LogRecord with message "RETURN {0}", log level
     * FINER, and the gives sourceMethod, sourceClass, and result
     * object is logged.
     * <p>
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod   name of the method 
     * @param   result  Object that is being returned
     */
    public void exiting(String sourceClass, String sourceMethod, Object result) 
    {
        _log.exiting(sourceClass, sourceMethod, result);
    }

    /**
     * Log throwing an exception.
     * <p>
     * This is a convenience method to log that a method is
     * terminating by throwing an exception.  The logging is done 
     * using the FINER level.
     * <p>
     * If the logger is currently enabled for the given message 
     * level then the given arguments are stored in a LogRecord
     * which is forwarded to all registered output handlers.  The
     * LogRecord's message is set to "THROW".
     * <p>
     * Note that the thrown argument is stored in the LogRecord thrown
     * property, rather than the LogRecord parameters property.  Thus is it
     * processed specially by output Formatters and is not treated
     * as a formatting parameter to the LogRecord message property.
     * <p>
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod  name of the method.
     * @param   thrown  The Throwable that is being thrown.
     */
    public void throwing(String sourceClass, String sourceMethod, Throwable thrown) 
    {
        _log.throwing(sourceClass, sourceMethod, thrown);
    }

    //=======================================================================
    // Start of simple convenience methods using level names as method names
    //=======================================================================

    /**
     * Log a SEVERE message.
     * <p>
     * If the logger is currently enabled for the SEVERE message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg   The string message (or a key in the message catalog)
     */
    public void severe(String msg) 
    {
        //_log.severe(msg);
        log(Level.SEVERE,msg);
    }

    /**
     * Log a WARNING message.
     * <p>
     * If the logger is currently enabled for the WARNING message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg   The string message (or a key in the message catalog)
     */
    public void warning(String msg) 
    {
        //_log.warning(msg);
        log(Level.WARNING,msg);
    }

    /**
     * Log an INFO message.
     * <p>
     * If the logger is currently enabled for the INFO message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg   The string message (or a key in the message catalog)
     */
    public void info(String msg) 
    {
        //_log.info(msg);
        log(Level.INFO,msg);
    }

    /**
     * Log a CONFIG message.
     * <p>
     * If the logger is currently enabled for the CONFIG message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg   The string message (or a key in the message catalog)
     */
    public void config(String msg) 
    {
        //_log.config(msg);
        log(Level.CONFIG,msg);
    }

    /**
     * Log a FINE message.
     * <p>
     * If the logger is currently enabled for the FINE message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg   The string message (or a key in the message catalog)
     */
    public void fine(String msg) 
    {
        //_log.fine(msg);
        log(Level.FINE,msg);
    }

    /**
     * Log a FINER message.
     * <p>
     * If the logger is currently enabled for the FINER message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg   The string message (or a key in the message catalog)
     */
    public void finer(String msg) 
    {
        //_log.finer(msg);
        log(Level.FINER,msg);
    }

    /**
     * Log a FINEST message.
     * <p>
     * If the logger is currently enabled for the FINEST message 
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg   The string message (or a key in the message catalog)
     */
    public void finest(String msg) 
    {
        //_log.finest(msg);
        log(Level.FINEST,msg);
    }

    /**
     * Log throwing an exception.
     * 
     * Comparing to Java Logging function
     * 
     *     Logger.throwing(sourceClass, sourceMethod, thrown) 
     * 
     * this function takes one more parameter "level" so that developers can 
     * specify the logging level of an exception. Developers should pass value
     * for the "level" parameter using following convention,
     * <p>
     * Level.SEVERE -- Serious exceptions or error conditions such that an 
     * application can no longer run.
     * <p>
     * Level.WARNING -- Exceptions or errors that are not fatal, but an 
     * application will run with some problems.
     * <p>
     * 
     * @param level Java Logging level
     * @param sourceClass name of class that issued the logging request
     * @param sourceMethod name of the method
     * @param thrown The Throwable that is being thrown
     */
    public void throwing(
            Level  level,
            String sourceClass,
            String sourceMethod,
            Throwable thrown
            )
    {
        logp(level, sourceClass, sourceMethod, null, thrown);
    }

    /**
     * Log a SEVERE message, with no arguments.
     * <p>
     * The message is forwarded to appropriate Java Logger objects. 
     * <p>
     * @param sourceClass  the name of the class that issued the logging request 
     * @param sourceMethod the name of the method that issued the logging request 
     * @param msg          the string message (or a key in the resource bundle)
     */
    public void severe(
            String sourceClass,
            String sourceMethod,
            String msg
            )
    {
        logp(Level.SEVERE, sourceClass, sourceMethod, msg);
    }

    /**
     * Log a SEVERE message, with one object parameter.
     * <p>
     * The message is forwarded to appropriate Java Logger objects. 
     * <p>
     * @param sourceClass  the name of the class that issued the logging request 
     * @param sourceMethod the name of the method that issued the logging request 
     * @param msg          the string message (or a key in the resource bundle)
     * @param param1       a parameter to the message
     */
    public void severe(
            String sourceClass,
            String sourceMethod,
            String msg,
            Object param1
            )
    {
        logp(Level.SEVERE, sourceClass, sourceMethod, msg, param1);
    }

    /**
     * Log a SEVERE message, with an array of object arguments.
     * <p>
     * The message is forwarded to appropriate Java Logger objects. 
     * <p>
     * @param sourceClass  the name of the class that issued the logging request 
     * @param sourceMethod the name of the method that issued the logging request 
     * @param msg          the string message (or a key in the resource bundle)
     * @param params       an array of parameters to the message
     */
    public void severe(
            String sourceClass,
            String sourceMethod,
            String msg,
            Object[] params
            )
    {
        logp(Level.SEVERE, sourceClass, sourceMethod, msg, params);
    }

    /**
     * Log a WARNING message, with no arguments.
     * <p>
     * The message is forwarded to appropriate Java Logger objects. 
     * <p>
     * @param sourceClass  the name of the class that issued the logging request 
     * @param sourceMethod the name of the method that issued the logging request 
     * @param msg          the string message (or a key in the resource bundle)
     */
    public void warning(
            String sourceClass,
            String sourceMethod,
            String msg
            )
    {
        logp(Level.WARNING, sourceClass, sourceMethod, msg);
    }

    /**
     * Log a WARNING message, with one object parameter.
     * <p>
     * The message is forwarded to appropriate Java Logger objects. 
     * <p>
     * @param sourceClass  the name of the class that issued the logging request 
     * @param sourceMethod the name of the method that issued the logging request 
     * @param msg          the string message (or a key in the resource bundle)
     * @param param1       a parameter to the message
     */
    public void warning(
            String sourceClass,
            String sourceMethod,
            String msg,
            Object param1
            )
    {
        logp(Level.WARNING, sourceClass, sourceMethod, msg, param1);
    }

    /**
     * Log a WARNING message, with an array of object arguments.
     * <p>
     * The message is forwarded to appropriate Java Logger objects. 
     * <p>
     * @param sourceClass  the name of the class that issued the logging request 
     * @param sourceMethod the name of the method that issued the logging request 
     * @param msg          the string message (or a key in the resource bundle)
     * @param params       an array of parameters to the message
     */
    public void warning(
            String sourceClass,
            String sourceMethod,
            String msg,
            Object[] params
            )
    {
        logp(Level.WARNING, sourceClass, sourceMethod, msg, params);
    }

    /**
     * Log a INFO message, with no arguments.
     * <p>
     * The message is forwarded to appropriate Java Logger objects. 
     * <p>
     * @param sourceClass  the name of the class that issued the logging request 
     * @param sourceMethod the name of the method that issued the logging request 
     * @param msg          the string message (or a key in the resource bundle)
     */
    public void info(
            String sourceClass,
            String sourceMethod,
            String msg
            )
    {
        logp(Level.INFO, sourceClass, sourceMethod, msg);
    }

    /**
     * Log a INFO message, with one object parameter.
     * <p>
     * The message is forwarded to appropriate Java Logger objects. 
     * <p>
     * @param sourceClass  the name of the class that issued the logging request 
     * @param sourceMethod the name of the method that issued the logging request 
     * @param msg          the string message (or a key in the resource bundle)
     * @param param1       a parameter to the message
     */
    public void info(
            String sourceClass,
            String sourceMethod,
            String msg,
            Object param1
            )
    {
        logp(Level.INFO, sourceClass, sourceMethod, msg, param1);
    }

    /**
     * Log a INFO message, with an array of object arguments.
     * <p>
     * The message is forwarded to appropriate Java Logger objects. 
     * <p>
     * @param sourceClass  the name of the class that issued the logging request 
     * @param sourceMethod the name of the method that issued the logging request 
     * @param msg          the string message (or a key in the resource bundle)
     * @param params       an array of parameters to the message
     */
    public void info(
            String sourceClass,
            String sourceMethod,
            String msg,
            Object[] params
            )
    {
        logp(Level.INFO, sourceClass, sourceMethod, msg, params);
    }

    /**
     * Log a CONFIG message, with no arguments.
     * <p>
     * The message is forwarded to appropriate Java Logger objects. 
     * <p>
     * @param sourceClass  the name of the class that issued the logging request 
     * @param sourceMethod the name of the method that issued the logging request 
     * @param msg          the string message (or a key in the resource bundle)
     */
    public void config(
            String sourceClass,
            String sourceMethod,
            String msg
            )
    {
        logp(Level.CONFIG, sourceClass, sourceMethod, msg);
    }

    /**
     * Log a CONFIG message, with one object parameter.
     * <p>
     * The message is forwarded to appropriate Java Logger objects. 
     * <p>
     * @param sourceClass  the name of the class that issued the logging request 
     * @param sourceMethod the name of the method that issued the logging request 
     * @param msg          the string message (or a key in the resource bundle)
     * @param param1       a parameter to the message
     */
    public void config(
            String sourceClass,
            String sourceMethod,
            String msg,
            Object param1
            )
    {
        _log.logp(Level.CONFIG, sourceClass, sourceMethod, msg, param1);
    }

    /**
     * Log a CONFIG message, with an array of object arguments.
     * <p>
     * The message is forwarded to appropriate Java Logger objects. 
     * <p>
     * @param sourceClass  the name of the class that issued the logging request 
     * @param sourceMethod the name of the method that issued the logging request 
     * @param msg          the string message (or a key in the resource bundle)
     * @param params       an array of parameters to the message
     */
    public void config(
            String sourceClass,
            String sourceMethod,
            String msg,
            Object[] params
            )
    {
        logp(Level.CONFIG, sourceClass, sourceMethod, msg, params);
    }

    /**
     * Log a FINE message, with no arguments.
     * <p>
     * The message is forwarded to appropriate Java Logger objects. 
     * <p>
     * @param sourceClass  the name of the class that issued the logging request 
     * @param sourceMethod the name of the method that issued the logging request 
     * @param msg          the string message (or a key in the resource bundle)
     */
    public void fine(
            String sourceClass,
            String sourceMethod,
            String msg
            )
    {
        logp(Level.FINE, sourceClass, sourceMethod, msg);
    }

    /**
     * Log a FINE message, with one object parameter.
     * <p>
     * The message is forwarded to appropriate Java Logger objects. 
     * <p>
     * @param sourceClass  the name of the class that issued the logging request 
     * @param sourceMethod the name of the method that issued the logging request 
     * @param msg          the string message (or a key in the resource bundle)
     * @param param1       a parameter to the message
     */
    public void fine(
            String sourceClass,
            String sourceMethod,
            String msg,
            Object param1
            )
    {
        logp(Level.FINE, sourceClass, sourceMethod, msg, param1);
    }

    /**
     * Log a FINE message, with an array of object arguments.
     * <p>
     * The message is forwarded to appropriate Java Logger objects. 
     * <p>
     * @param sourceClass  the name of the class that issued the logging request 
     * @param sourceMethod the name of the method that issued the logging request 
     * @param msg          the string message (or a key in the resource bundle)
     * @param params       an array of parameters to the message
     */
    public void fine(
            String sourceClass,
            String sourceMethod,
            String msg,
            Object[] params
            )
    {
        logp(Level.FINE, sourceClass, sourceMethod, msg, params);
    }

    /**
     * Log a FINER message, with no arguments.
     * <p>
     * The message is forwarded to appropriate Java Logger objects. 
     * <p>
     * @param sourceClass  the name of the class that issued the logging request 
     * @param sourceMethod the name of the method that issued the logging request 
     * @param msg          the string message (or a key in the resource bundle)
     */
    public void finer(
            String sourceClass,
            String sourceMethod,
            String msg
            )
    {
        logp(Level.FINER, sourceClass, sourceMethod, msg);
    }

    /**
     * Log a FINER message, with one object parameter.
     * <p>
     * The message is forwarded to appropriate Java Logger objects. 
     * <p>
     * @param sourceClass  the name of the class that issued the logging request 
     * @param sourceMethod the name of the method that issued the logging request 
     * @param msg          the string message (or a key in the resource bundle)
     * @param param1       a parameter to the message
     */
    public void finer(
            String sourceClass,
            String sourceMethod,
            String msg,
            Object param1
            )
    {
        logp(Level.FINER, sourceClass, sourceMethod, msg, param1);
    }

    /**
     * Log a FINER message, with an array of object arguments.
     * <p>
     * The message is forwarded to appropriate Java Logger objects. 
     * <p>
     * @param sourceClass  the name of the class that issued the logging request 
     * @param sourceMethod the name of the method that issued the logging request 
     * @param msg          the string message (or a key in the resource bundle)
     * @param params       an array of parameters to the message
     */
    public void finer(
            String sourceClass,
            String sourceMethod,
            String msg,
            Object[] params
            )
    {
        logp(Level.FINER, sourceClass, sourceMethod, msg, params);
    }

    /**
     * Log a FINEST message, with no arguments.
     * <p>
     * The message is forwarded to appropriate Java Logger objects. 
     * <p>
     * @param sourceClass  the name of the class that issued the logging request 
     * @param sourceMethod the name of the method that issued the logging request 
     * @param msg          the string message (or a key in the resource bundle)
     */
    public void finest(
            String sourceClass,
            String sourceMethod,
            String msg
            )
    {
        logp(Level.FINEST, sourceClass, sourceMethod, msg);
    }

    /**
     * Log a FINEST message, with one object parameter.
     * <p>
     * The message is forwarded to appropriate Java Logger objects. 
     * <p>
     * @param sourceClass  the name of the class that issued the logging request 
     * @param sourceMethod the name of the method that issued the logging request 
     * @param msg          the string message (or a key in the resource bundle)
     * @param param1       a parameter to the message
     */
    public void finest(
            String sourceClass,
            String sourceMethod,
            String msg,
            Object param1
            )
    {
        logp(Level.FINEST, sourceClass, sourceMethod, msg, param1);
    }

    /**
     * Log a FINEST message, with an array of object arguments.
     * <p>
     * The message is forwarded to appropriate Java Logger objects. 
     * <p>
     * @param sourceClass  the name of the class that issued the logging request 
     * @param sourceMethod the name of the method that issued the logging request 
     * @param msg          the string message (or a key in the resource bundle)
     * @param params       an array of parameters to the message
     */
    public void finest(
            String sourceClass,
            String sourceMethod,
            String msg,
            Object[] params
            )
    {
        logp(Level.FINEST, sourceClass, sourceMethod, msg, params);
    }

    /**
     * Log a message, with an list of object arguments.
     * <p>
     * The message is forwarded to appropriate Java Logger objects. 
     * <p>
     * @param sourceClass  the name of the class that issued the logging request 
     * @param sourceMethod the name of the method that issued the logging request 
     * @param msg          the string message (or a key in the resource bundle)
     * @param params1      Parameter 1 to the log message
     * @param params2      Parameter 2 to the log message
     * @param params3      Parameter 3 to the log message
     */
    public void logp(Level level,
            String sourceClass,
            String sourceMethod,
            String msg,
            Object params1,
            Object params2,
            Object params3
            )
    {
        logp(level,sourceClass, sourceMethod, msg, new Object[] {params1, params2, params3});
    }

    //================================================================
    // End of convenience methods 
    //================================================================

    /**
     * Set the log level specifying which message levels will be
     * logged by this logger.  Message levels lower than this
     * value will be discarded.  The level value Level.OFF
     * can be used to turn off logging.
     * <p>
     * If the new level is null, it means that this node should
     * inherit its level from its nearest ancestor with a specific
     * (non-null) level value.
     * 
     * @param newLevel   the new value for the log level (may be null)
     * @exception  SecurityException  if a security manager exists and if
     *             the caller does not have LoggingPermission("control").
     */
    public void setLevel(Level newLevel) throws SecurityException 
    {
        _log.setLevel(newLevel);
    }

    /**
     * Get the log Level that has been specified for this Logger.
     * The result may be null, which means that this logger's
     * effective level will be inherited from its parent.
     *
     * @return    this Logger's level
     */
    public Level getLevel() 
    {
        return _log.getLevel();
    }

    /**
     * Check if a message of the given level would actually be logged
     * by this logger.  This check is based on the Loggers effective level,
     * which may be inherited from its parent.
     *
     * @param level   a message logging level
     * @return    true if the given message level is currently being logged.
     */
    public boolean isLoggable(Level level) 
    {
        return _log.isLoggable(level);
    }

    /**
     * Get the name for this logger.
     * @return logger name.  Will be null for anonymous Loggers.
     */
    public String getName() 
    {
        return _log.getName();  
    }

    /**
     * Add a log Handler to receive logging messages.
     * <p>
     * By default, Loggers also send their output to their parent logger.
     * Typically the root Logger is configured with a set of Handlers
     * that essentially act as default handlers for all loggers.
     *
     * @param handler a logging Handler
     * @exception  SecurityException  if a security manager exists and if
     *             the caller does not have LoggingPermission("control").
     */
    public void addHandler(Handler handler) throws SecurityException 
    {
        _log.addHandler(handler);
    }

    /**
     * Remove a log Handler.
     * <P>
     * Returns silently if the given Handler is not found.
     * 
     * @param handler a logging Handler
     * @exception  SecurityException  if a security manager exists and if
     *             the caller does not have LoggingPermission("control").
     */
    public void removeHandler(Handler handler) throws SecurityException 
    {
        _log.removeHandler(handler);
    }

    /**
     * Get the Handlers associated with this logger.
     * <p>
     * @return  an array of all registered Handlers
     */
    public Handler[] getHandlers() 
    {
        return _log.getHandlers();
    }

    /**
     * Specify whether or not this logger should send its output
     * to it's parent Logger.  This means that any LogRecords will
     * also be written to the parent's Handlers, and potentially
     * to its parent, recursively up the namespace.
     *
     * @param useParentHandlers   true if output is to be sent to the
     *        logger's parent.
     * @exception  SecurityException  if a security manager exists and if
     *             the caller does not have LoggingPermission("control").
     */
    public void setUseParentHandlers(boolean useParentHandlers) 
    {
        _log.setUseParentHandlers(useParentHandlers);
    }

    /**
     * Discover whether or not this logger is sending its output
     * to its parent logger.
     *
     * @return  true if output is to be sent to the logger's parent
     */
    public boolean getUseParentHandlers() 
    {
        return _log.getUseParentHandlers();
    }

    /**
     * Return the parent for this Logger.
     * <p>
     * This method returns the nearest extant parent in the namespace.
     * Thus if a Logger is called "a.b.c.d", and a Logger called "a.b"
     * has been created but no logger "a.b.c" exists, then a call of
     * getParent on the Logger "a.b.c.d" will return the Logger "a.b".
     * <p>
     * The result will be null if it is called on the root Logger
     * in the namespace.
     * 
     * @return nearest existing parent Logger 
     */
    public Logger getParent() 
    {
        return _log.getParent();
    }

    /**
     * Set the parent for this Logger.  This method is used by
     * the LogManager to update a Logger when the namespace changes.
     * <p>
     * It should not be called from application code.
     * <p>
     * @param  parent   the new parent logger
     * @exception  SecurityException  if a security manager exists and if
     *             the caller does not have LoggingPermission("control").
     */
    public void setParent(Logger parent) 
    {
        _log.setParent(parent);
    }


    private ResourceBundle findResourceBundle(String name) 
    {
        // Return a null bundle for a null name.
        if (name == null)
        {
            return null;
        }

        Locale currentLocale = Locale.getDefault();
        return ResourceBundle.getBundle(name, currentLocale);

    }


    private void doLog(LogRecord lr, String rbname) 
    {
        lr.setLoggerName(_log.getName());
        if (rbname != null)
        {
            lr.setResourceBundleName(rbname);
            lr.setResourceBundle(findResourceBundle(rbname));
        }
        log(lr);
    }

    private void doLog(LogRecord lr) 
    {
        lr.setLoggerName(_log.getName());
        String ebname = _log.getResourceBundleName();
        if (ebname != null)
        {
            lr.setResourceBundleName(ebname);
            lr.setResourceBundle(_log.getResourceBundle());
        }
        _log.log(lr);
    }

    public void severe(Throwable t)
    {
        severe(null, t);
    }

    public void severe(String message, Throwable t)
    {
        log(Level.SEVERE, message, t);
    }

    public void severe(String message, Object param)
    {
        log(Level.SEVERE, message, param);
    }

    public void severe(String message, Object[] params)
    {
        log(Level.SEVERE, message, params);
    }


    public void warning(Throwable t)
    {
        warning(null, t);
    }

    public void warning(String message, Throwable t)
    {
        log(Level.WARNING, message, t);
    }

    public void warning(String message, Object param)
    {
        log(Level.WARNING, message, param);
    }

    public void warning(String message, Object[] params)
    {
        log(Level.WARNING, message, params);
    }

    public void info(Throwable t)
    {
        info(null, t);
    }

    public void info(String message, Throwable t)
    {
        log(Level.INFO, message, t);
    }

    public void info(String message, Object param)
    {
        log(Level.INFO, message, param);
    }

    public void info(String message, Object[] params)
    {
        log(Level.INFO, message, params);
    }

    public void fine(Throwable t)
    {
        fine(null, t);
    }

    public void fine(String message, Throwable t)
    {
        log(Level.FINE, message, t);
    }

    public void fine(String message, Object param)
    {
        log(Level.FINE, message, param);
    }

    public void fine(String message, Object[] params)
    {
        log(Level.FINE, message, params);
    }

    public void finer(Throwable t)
    {
        finer(null, t);
    }

    public void finer(String message, Throwable t)
    {
        log(Level.FINER, message, t);
    }

    public void finer(String message, Object param)
    {
        log(Level.FINER, message, param);
    }

    public void finer(String message, Object[] params)
    {
        log(Level.FINER, message, params);
    }


    public void finest(Throwable t)
    {
        finest(null, t);
    }

    public void finest(String message, Throwable t)
    {
        log(Level.FINEST, message, t);
    }

    public void finest(String message, Object param)
    {
        log(Level.FINEST, message, param);
    }

    public void finest(String message, Object[] params)
    {
        log(Level.FINEST, message, params);
    }

    /**
     * Returns true if severe messages should be logged.
     */
    public boolean isSevere()
    {
        return isLoggable(Level.SEVERE);
    }

    /**
     * Returns true if warning messages should be logged.
     */
    public boolean isWarning()
    {
        return isLoggable(Level.WARNING);
    }


    /**
     * Returns true if info messages should be logged.
     */
    public boolean isInfo()
    {
        return isLoggable(Level.INFO);
    }


    /**
     * Returns true if config messages should be logged.
     */
    public boolean isConfig()
    {
        return isLoggable(Level.CONFIG);
    }


    /**
     * Returns true if fine messages should be logged.
     */
    public boolean isFine()
    {
        return isLoggable(Level.FINE);
    }


    /**
     * Returns true if finer messages should be logged.
     */
    public boolean isFiner()
    {
        return isLoggable(Level.FINER);
    }

    /**
     * Returns true if finest messages should be logged.
     */
    public boolean isFinest()
    {
        return isLoggable(Level.FINEST);
    }

    /**
     * Returns message string in default locale
     */
    public String getMessage(String key)
    {
        try
        {
            return _log.getResourceBundle().getString(key);
        }
        catch (MissingResourceException mre)
        {
            return key;
        }
    }

    /**
     * Returns message string in default locale
     */
    public String getMessage(MyfacesLogKey key)
    {
        try
        {
            String name = key.name();
            return _log.getResourceBundle().getString(name);
        }
        catch (MissingResourceException mre)
        {
            return key.name();
        }
    }

    /**
     * Returns message string in default locale
     */
    public MyfacesLogMessage getMyfacesMessage(MyfacesLogKey key)
    {
        MyfacesLogMessage facesMessage = new MyfacesLogMessage();
        try
        {

            String name = key.name();
            String summary = _log.getResourceBundle().getString(name);
            facesMessage.setSummary(summary);
            try
            {
                String detail = _log.getResourceBundle().getString(name +"_detail");
                facesMessage.setDetail(detail);
            }
            catch (MissingResourceException e)
            {
                facesMessage.setDetail(name);
            }

            try
            {
                String related = _log.getResourceBundle().getString(name +"_related");
                facesMessage.setRelated(related);
            }
            catch (MissingResourceException e)
            {
                /// ignore
            }
            return facesMessage;
        }
        catch (MissingResourceException mre)
        {
            facesMessage.setSummary(key.name());
            return facesMessage;
        }
    }

    /**
     * Returns formated string in default locale
     */
    public String getMessage(String key, Object... params)
    {
        String message = getMessage(key);
        MessageFormat fmt = new MessageFormat(message);  
        return fmt.format(params);  
    }

    /**
     * Returns formated string in default locale
     */
    public String getMessage(String key, Object param)
    {
        return getMessage(key, new Object[]{param});
    }

    private Logger _log;

    /** Currenly this logger is used only in myfaces-impl:  therefore LoggerBundle is same for both api and impl*/
    private static final String _API_LOGGER_BUNDLE = "org.apache.myfaces.resource.LoggerBundle";

    private static final String _IMPL_LOGGER_BUNDLE = "org.apache.myfaces.resource.LoggerBundle";

    private static final MyfacesLogger _LOG = MyfacesLogger.createMyfacesLogger(
            MyfacesLogger.class);
}
