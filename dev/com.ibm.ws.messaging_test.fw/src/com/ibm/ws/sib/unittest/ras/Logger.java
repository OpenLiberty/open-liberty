/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.unittest.ras;

import java.io.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>This logger is used to provide trace of the mediation monkey and summary
 *   engine.
 * </p>
 *
 * <p>SIB build component: mm.logger</p>
 *
 * @author nottinga
 * @version 1.26
 * @since 1.0
 */
public class Logger
{
  /** The source class name of the trace statement */
  private String _source;
  /** The log group the logger is part of */
  private String _logGroup;
  /** The java util logger underlying this logger */
  private java.util.logging.Logger _logger;
  /** A cache of doLog results */
  private AtomicReference<ConcurrentMap<Level,Boolean>> _doLog = new AtomicReference<ConcurrentMap<Level,Boolean>>(new ConcurrentHashMap<Level,Boolean>());
  /** The overriding bundle name (null if NOT over-ridden */
  private String _overridingBundleName = null;
  /** The overriding bundle (null if NOT over-ridden */
  private ResourceBundle _overridingBundle = null;
  /** The log specification */
  private static List<LogSpecElement> _logSpec = new ArrayList<LogSpecElement>();
  /** The log handler for mediation monkey logging */
  private static FileHandler _logHandler = new FileHandler();
  /** The name of the file last passed to the _logHandler */
  private static File _lastFile = null;
  /** The previous trace spec we've been given */
  private static String _previousTraceSpec = null;

  private static HashSet<Logger> _allLoggers = new HashSet<Logger>();

  /** Logger for this class itself!  - used for SystemOut and SystemErr */
  private static final Logger OUR_OWN_LOGGER = new Logger(Logger.class,"");

  /**
   * This variable can be set using a static access method, and is used to insert
   * log messages (error, warning etc) into the testcase log file as well as echo
   * them to SystemOut.
   *
   * Access to this variable is synchronized to prevent it from being set at the same
   * time that it is being written to.
   */
  private static Writer testcaseLogWriter = null;

  /**
   * <p>This is a log specification element.</p>
   */
  private static class LogSpecElement
  {
    /** The class name or log group match */
    private String _elementMatch;
    /** The levels match */
    private Set<String> _levels;
    /** Whether the element match is exact or not */
    private boolean _notExact;
    /** Whether the element covers all levels or not */
    private boolean _allLevels;
    /** Is this enabled or a disabled element? */
    private Boolean _mode;

    /* ---------------------------------------------------------------------- */
    /* LogSpecElement method
    /* ---------------------------------------------------------------------- */
    /**
     * The constructor
     *
     * @param elementMatch The class name part.
     * @param level        The level part.
     * @param enabled      Whether this is enabled or not. always true.
     */
    public LogSpecElement(String elementMatch, String level, boolean enabled)
    {
      _notExact = elementMatch.endsWith("*");

      if (_notExact)
      {
        _elementMatch = elementMatch.substring(0, elementMatch.length() - 1);
      }
      else
      {
        _elementMatch = elementMatch;
      }

      _levels = new HashSet<String>();
      StringTokenizer tokens = new StringTokenizer(level, ",");
      while (tokens.hasMoreTokens())
      {
        _levels.add(tokens.nextToken().toLowerCase());
      }

      _allLevels = "all".equals(level);

      _mode = enabled ? Boolean.TRUE : Boolean.FALSE;
    }

    /* ---------------------------------------------------------------------- */
    /* matchTrace method
    /* ---------------------------------------------------------------------- */
    /**
     * This method works out whether this log spec matches the given log and
     * therefore whether the information should be logged. A result of true
     * means a log will be made.
     *
     * @param source   The source.
     * @param logGroup The log group.
     * @param level    The level.
     * @return         Boolean.TRUE if this element says to log, Boolean.FALSE
     *                  if this element says don't log and null if this element
     *                  doesn't care
     */
    public Boolean matchTrace(String source, String logGroup, Level level)
    {
      Boolean result = null;

      // if all levels are enabled, or the passed in level matches a
      // set level then we do more processing, otherwise false.
      if (_allLevels || _levels.contains(level.getName().toLowerCase()))
      {
        // If we are not doing an exact match check starts with.
        if (_notExact)
        {
          // If the source or logGroup starts with the element match
          if (((source!=null) && source.startsWith(_elementMatch)) ||
              ((logGroup!= null) && logGroup.startsWith(_elementMatch)))
          {
            result = _mode;
          }
        }
        // If it is a * then just return true.
        else if ("*".equals(_elementMatch))
        {
          result = _mode;
        }
        // We are doing an exact match.
        else
        {
          if (((source!= null) && source.equals(_elementMatch)) || ((logGroup != null) && logGroup.equals(_elementMatch)))
          {
            result = _mode;
          }
        }
      }

      return result;
    }
  }

  /**
   * This constructor creates the logger for the specified class.
   *
   * @param source   The class the log is being created for.
   * @param logGroup The log group.
   */
  private Logger(Class source, String logGroup)
  {
    this(source.getName(), logGroup);
  }

  /**
   * Construct a new Logger for a specified class nae
   *
   * @param source    The name of the class
   * @param logGroup  The log group of the class
   */
  private Logger(String source, String logGroup)
  {
    _source = source;
    _logGroup = logGroup;
    _logger = java.util.logging.Logger.getLogger(_source);
    synchronized(Logger.class)
    {
      _logger.addHandler(_logHandler);
    }
    _logger.setLevel(Level.ALL);
    synchronized (_allLoggers)
    {
      _allLoggers.add(this);
    }
  }

  /**
   * Construct a new Logger given the class, log group name and resource bundle name
   *
   * @param source         The class the log is being created for.
   * @param logGroup       The log group
   * @param resourceBundle The resource bundle
   */
  private Logger(Class source, String logGroup, String resourceBundle)
  {
    this(source.getName(), logGroup, resourceBundle);
  }

  /**
   * Construct a new Logger given the class name, log group name and resource bundle name
   *
   * @param source         The class name for which the log is being created.
   * @param logGroup       The log group
   * @param resourceBundle The resource bundle
   */
  private Logger(String source, String logGroup, String resourceBundle)
  {
    _source = source;
    _logGroup = logGroup;
    try
    {
      _logger = java.util.logging.Logger.getLogger(_source, resourceBundle);
    }
    catch (IllegalArgumentException e)
    {
      // Maybe the resource bundle of the existing logger is not
      // the same as the requested one. In this case, get the existing logger,
      // check the resource bundle is different, and then record that we
      // (in this object) need to override the resource bundle.
      _logger = java.util.logging.Logger.getLogger(_source);
      if (resourceBundle.equals(_logger.getResourceBundleName()))
      {
        throw e; // It must have been some other kind of IllegalArgumentException - very strange
      }

      try
      {
        _overridingBundleName = resourceBundle;
        _overridingBundle = java.util.logging.Logger.getAnonymousLogger(resourceBundle).getResourceBundle();
      }
      catch (java.util.MissingResourceException e2)
      {
        System.err.println( "When trying to get the bundle " + resourceBundle +
                            " for the class " + findCaller(e2) +
                            " the following exception was received: "+ e2);
      }
    }
    catch (java.util.MissingResourceException e)
    {
      System.err.println( "When trying to get the bundle " + resourceBundle +
                          " for the class " + findCaller(e) +
                          " the following exception was received: "+ e);
      _logger = java.util.logging.Logger.getLogger(_source);
    }
    synchronized(Logger.class)
    {
      _logger.addHandler(_logHandler);
    }
    _logger.setLevel(Level.ALL);

    synchronized (_allLoggers)
    {
      _allLoggers.add(this);
    }
  }

  /**
   * Figure out the caller class for an exception (i.e. ignore trace classes
   *
   * @param e The exception
   * @return The class name of the caller
   */
  private String findCaller(Exception e)
  {
    String className = "unknown";
    StackTraceElement[] stackTrace = e.getStackTrace();
    boolean keepGoing = true;
    boolean foundTr = false;
    boolean foundSibTr = false;

    for (int i = 0; i < stackTrace.length && keepGoing; i++)
    {
      className = stackTrace[i].getClassName();

      boolean isSibTr = "com.ibm.ws.sib.utils.ras.SibTr".equals(className);
      boolean isTr = "com.ibm.ejs.ras.Tr".equals(className);


      if (foundTr && !isSibTr && !isTr) keepGoing = false; // Found the caller.

      if (foundSibTr && !isSibTr && !isTr) keepGoing = false; // found the caller

      if (isTr && ! foundTr) foundTr = true;
      if (isSibTr && !foundSibTr) foundSibTr = true;
    }

    return className;
  }

  /* -------------------------------------------------------------------------- */
  /* getSource method
  /* -------------------------------------------------------------------------- */
  /**
   * @return The source (class name) for this Logger
   */
  public final String getSource()
  {
    return _source;
  }
  /* ------------------------------------------------------------------------ */
  /* getLogger method
  /* ------------------------------------------------------------------------ */
  /**
   * This method obtains a logger for the specified class. One getLogger method
   * should be called at most once for each class.
   *
   * @param source The class requesting the logger.
   * @return       The created logger.
   */
  public static final Logger getLogger(Class source)
  {
    return getLogger(source, "");
  }

  /* ------------------------------------------------------------------------ */
  /* getLogger method
  /* ------------------------------------------------------------------------ */
  /**
   * This method obtains a logger for the specified class. One getLogger method
   * should be called at most once for each class.
   *
   * @param source   The source of the log.
   * @param logGroup The log group.
   * @return         The logger
   */
  public static final Logger getLogger(Class source, String logGroup)
  {
    return new Logger(source, logGroup);
  }

  /* -------------------------------------------------------------------------- */
  /* getLogger method
  /* -------------------------------------------------------------------------- */
  /**
   * This method obtains a logger for the specified class. One getLogger method
   * should be called at most once for each class.
   *
   * @param source   The source of the log.
   * @return         The logger
   */
  public static final Logger getLogger(String source)
  {
    return new Logger(source, "");
  }
  /* -------------------------------------------------------------------------- */
  /* getLogger method
  /* -------------------------------------------------------------------------- */
  /**
   * This method obtains a logger for the specified class. One getLogger method
   * should be called at most once for each class.
   *
   * @param source   The source of the log.
   * @param logGroup The log group.
   * @return         The logger
   */
  public static final Logger getLogger(String source, String logGroup)
  {
    return new Logger(source, logGroup);
  }

  /* -------------------------------------------------------------------------- */
  /* getLogger method
  /* -------------------------------------------------------------------------- */
  /**
   * This method obtains a logger for the specified class. One getLogger method
   * should be called at most once for each class.
   *
   * @param source   The source of the log.
   * @param logGroup The log group.
   * @param bundle   The NLS bundle to be used for this logger
   * @return         The logger
   */
  public static final Logger getLogger(String source, String logGroup, String bundle)
  {
    return new Logger(source, logGroup, bundle);
  }

  /* -------------------------------------------------------------------------- */
  /* getLogger method
  /* -------------------------------------------------------------------------- */
  /**
   * This method obtains a logger for the specified class. One getLogger method
   * should be called at most once for each class.
   *
   * @param source   The source of the log.
   * @param logGroup The log group.
   * @param bundle   The NLS bundle to be used for this logger
   * @return         The logger
   */
  public static final Logger getLogger(Class source, String logGroup, String bundle)
  {
    return new Logger(source, logGroup, bundle);
  }

  /* ------------------------------------------------------------------------ */
  /* setLoggingSpecification method
  /* ------------------------------------------------------------------------ */
  /**
   * Sets the logging specification.
   *
   * @param logSpec The log spec.
   */
  public static final void setLoggingSpecification(String logSpec)
  {
    if (_previousTraceSpec == null && logSpec == null)
      return; // No need to do anything
    if (_previousTraceSpec != null && _previousTraceSpec.equals(logSpec))
      return; // No need to do anything
    _previousTraceSpec = logSpec;

    System.out.println("*** Trace Specification now: "+logSpec);

    List<LogSpecElement> logSpecList;

    if (logSpec != null)
    {
      StringTokenizer tokens = new StringTokenizer(logSpec, ":");
      logSpecList = new ArrayList<LogSpecElement>(tokens.countTokens());

      while (tokens.hasMoreTokens())
      {
        String token = tokens.nextToken();
        StringTokenizer tokens2 = new StringTokenizer(token, "=");
        switch (tokens2.countTokens())
        {
          case 1:
            // Assume x=all=enabled
            logSpecList.add(new LogSpecElement(tokens2.nextToken(), "all", true));
            break;
          case 2:
            // Assume x=y=enabled
            logSpecList.add(new LogSpecElement(tokens2.nextToken(), tokens2.nextToken(), true));
            break;
          case 3:
            logSpecList.add(new LogSpecElement(tokens2.nextToken(), tokens2.nextToken(), tokens2.nextToken().equalsIgnoreCase("enabled")));
            // Do not support x=y=disabled yet.
        }
      }
    }
    else
    {
      logSpecList = new ArrayList<LogSpecElement>(); // An empty list
    }

    _logSpec = logSpecList;

    synchronized (_allLoggers)
    {
      Iterator<Logger> iter = _allLoggers.iterator();
      while (iter.hasNext())
      {
        Logger logger = iter.next();
        logger.resetDoLog();
      }
    }
  }

  private void resetDoLog()
  {
    _doLog.set(new ConcurrentHashMap<Level,Boolean>());
  }

  /* -------------------------------------------------------------------------- */
  /* setTraceLocation method
  /* -------------------------------------------------------------------------- */
  /**
   * Set the location of the trace file. If the file has not been used as a trace
   * file before (in this class loader) it is erased
   *
   * @param file
   * @throws IOException
   */
  public synchronized static final void setTraceLocation(File file) throws IOException
  {
    if (_logHandler == null)
    {
      _logHandler = new FileHandler();
      _logHandler.setFormatter(new AdvancedFormatter());
    }

    // If the trace location has changed, let the handler know (it will erase the new file)
    if (!file.equals(_lastFile))
    {
      _logHandler.setFile(file);
      _lastFile = file;
    }
  }

  /**
   * Sets the testcase logger so that log messages are shown with the
   * associated test.
   * @param wrtr
   */
  public static final void setTestCaseLogger(Writer wrtr)
  {
    synchronized(Logger.class)
    {
      testcaseLogWriter = wrtr;
    }
  }

  /* -------------------------------------------------------------------------- */
  /* captureSystemOut method
  /* -------------------------------------------------------------------------- */
  /**
   * Capture the System.out PrintStream
   */
  public static final void captureSystemOut()
  {
    if (! (System.out instanceof SplittingPrintStream))
    {
      System.setOut(new SplittingPrintStream(System.out,new LoggingPrintStream("SystemOut", OUR_OWN_LOGGER._logger)));
    }
  }

  /* -------------------------------------------------------------------------- */
  /* captureSystemErr method
  /* -------------------------------------------------------------------------- */
  /**
   * Capture the System.err PrintStream
   */
  public static final void captureSystemErr()
  {
    if (! (System.err instanceof SplittingPrintStream))
    {
      System.setErr(new SplittingPrintStream(System.err,new LoggingPrintStream("SystemErr", OUR_OWN_LOGGER._logger)));
    }
  }

  /* ------------------------------------------------------------------------ */
  /* enter method
  /* ------------------------------------------------------------------------ */
  /**
   * This method logs a method entry of the specified name, with no parameters.
   *
   * @param methodName The method being called.
   */
  public void enter(String methodName)
  {
    if (internalDoLog(Level.ENTRY))
    {
      _logger.logp(Level.ENTRY, _source, methodName, "Entry");
    }
  }

  /* ------------------------------------------------------------------------ */
  /* enter method
  /* ------------------------------------------------------------------------ */
  /**
   * This method logs a method entry of the specified name, with a single
   * argument.
   *
   * @param methodName The name of the method.
   * @param argument   The argument.
   */
  public void enter(String methodName, Object argument)
  {
    if (argument instanceof Object[])
    {
      enter(methodName,(Object[])argument);
    }
    else
    {
      if (internalDoLog(Level.ENTRY))
      {
        _logger.logp(Level.ENTRY, _source, methodName, "Entry {0}", argument);
      }
    }
  }

  /* ------------------------------------------------------------------------ */
  /* enter method
  /* ------------------------------------------------------------------------ */
  /**
   * This method logs a method entry of the specified name, with multiple
   * arguments.
   *
   * @param methodName The name of the method.
   * @param arguments  The arguments in an array.
   */
  public void enter(String methodName, Object[] arguments)
  {
    if (internalDoLog(Level.ENTRY))
    {
      _logger.logp(Level.ENTRY, _source, methodName, formatStringFor("Entry",arguments.length), arguments);
    }
  }

  /* -------------------------------------------------------------------------- */
  /* formatStringFor method
  /* -------------------------------------------------------------------------- */
  /**
   * Return a message format string with n inserts and a prefix
   *
   * @param prefix The prefix of the message format string
   * @param n The number of inserts required
   * @return The message format string
   */
  private String formatStringFor(String prefix, int n)
  {
    StringBuffer message = new StringBuffer();
    if (prefix != null)
    {
      message.append(prefix);
      message.append(" ");
    }
    for (int i = 0; i < n; i++)
    {
      if (i>0) message.append(" ");
      message.append("{" + i + "}");
    }
    return message.toString();
  }

  /* ------------------------------------------------------------------------ */
  /* exit method
  /* ------------------------------------------------------------------------ */
  /**
   * This method traces an exit of a void method.
   *
   * @param methodName The name of the method being left.
   */
  public void exit(String methodName)
  {
    if (internalDoLog(Level.ENTRY))
    {
      _logger.logp(Level.EXIT, _source, methodName, "Exit");
    }
  }

  /* ------------------------------------------------------------------------ */
  /* exit method
  /* ------------------------------------------------------------------------ */
  /**
   * This method traces an exit of a non void method.
   *
   * @param methodName The method being left.
   * @param argument   The object being returned.
   */
  public void exit(String methodName, Object argument)
  {
    if (internalDoLog(Level.ENTRY))
    {
      if(argument instanceof Object[])
      {
      Object[] arguments = (Object[]) argument;
      _logger.logp(Level.EXIT, _source, methodName, formatStringFor("Exit",arguments.length), arguments);
      }
      else
      {
        _logger.logp(Level.EXIT, _source, methodName, "Exit {0}", argument);
      }
    }
  }

  /* ------------------------------------------------------------------------ */
  /* throwing method
  /* ------------------------------------------------------------------------ */
  /**
   * This method traces an exit of a method when an exception is thrown.
   *
   * @param methodName The name of the method.
   * @param t          The exception being thrown.
   */
  public void throwing(String methodName, Throwable t)
  {
    if (internalDoLog(Level.THROWING))
    {
      _logger.logp(Level.THROWING, _source, methodName, "THROWING {0}", extractStackTrace(t));
    }
  }

  /* ------------------------------------------------------------------------ */
  /* caught method
  /* ------------------------------------------------------------------------ */
  /**
   * This method traces an exception being caught.
   *
   * @param methodName The method the exception is caught in.
   * @param t          The exception that was caught.
   */
  public void caught(String methodName, Throwable t)
  {
    if (internalDoLog(Level.CAUGHT))
    {
      _logger.logp(Level.CAUGHT, _source, methodName, "CAUGHT {0}", extractStackTrace(t));
    }
  }

  /* -------------------------------------------------------------------------- */
  /* debug method
  /* -------------------------------------------------------------------------- */
  /**
   * Add a debug message (that will not be formatted) to the trace
   *
   * @param message The message to be traced
   */
  public void debug(String message)
  {
    if (internalDoLog(Level.DEBUG))
    {
      _logger.logp(Level.DEBUG, _source, "", "{0}", message);
    }
  }

  /* -------------------------------------------------------------------------- */
  /* debug method
  /* -------------------------------------------------------------------------- */
  /**
   * Add a debug message (that will not be formatted) to the trace
   *
   * @param message The message to be traced
   * @param args    The object(s) to be traced as well
   */
  public void debug(String message, Object args)
  {
    if (internalDoLog(Level.DEBUG))
    {
      Object[] allArgs = combine(message,args);
      _logger.logp(Level.DEBUG, _source, "", formatStringFor(null, allArgs.length), allArgs);
    }
  }

  /* -------------------------------------------------------------------------- */
  /* dump method
  /* -------------------------------------------------------------------------- */
  /**
   * Add a dump message (that will not be formatted) to the trace
   *
   * @param message
   */
  public void dump(String message)
  {
    if (internalDoLog(Level.DUMP))
    {
      _logger.logp(Level.DUMP, _source, "", "{0}", message);
    }
  }

  /* -------------------------------------------------------------------------- */
  /* dump method
  /* -------------------------------------------------------------------------- */
  /**
   * Add a dump message (that will not be formatted) to the trace
   *
   * @param message
   * @param args    The object(s) to be traced as well
   */
  public void dump(String message, Object args)
  {
    if (internalDoLog(Level.DUMP))
    {
      Object[] allArgs = combine(message,args);
      _logger.logp(Level.DUMP, _source, "", formatStringFor(null, allArgs.length), allArgs);
    }
  }

  /* -------------------------------------------------------------------------- */
  /* event method
  /* -------------------------------------------------------------------------- */
  /**
   * Add an event message (that will not be formatted) to the trace
   *
   * @param message
   */
  public void event(String message)
  {
   if (internalDoLog(Level.EVENT))
    {
      _logger.logp(Level.EVENT, _source, "", "{0}", message);
    }
  }

  /* -------------------------------------------------------------------------- */
  /* event method
  /* -------------------------------------------------------------------------- */
  /**
   * Add an event message (that will not be formatted) to the trace
   *
   * @param message
   * @param args    The object(s) to be traced as well
   */
  public void event(String message, Object args)
  {
    if (internalDoLog(Level.EVENT))
    {
      Object[] allArgs = combine(message,args);
      _logger.logp(Level.EVENT, _source, "", formatStringFor(null, allArgs.length), allArgs);
    }
  }

  /* -------------------------------------------------------------------------- */
  /* uncondEvent method
  /* -------------------------------------------------------------------------- */
  /**
   * Unconditionally (i.e. even if the relevant trace group is disabled) add
   * a message (that will not be formatted) to the trace
   *
   * @param message
   */
  public void uncondEvent(String message)
  {
    _logger.logp(Level.EVENT, _source, "", "{0}", message);
  }

  /* -------------------------------------------------------------------------- */
  /* uncondEvent method
  /* -------------------------------------------------------------------------- */
  /**
   * Unconditionally (i.e. even if the relevant trace group is disabled) add
   * a message (that will not be formatted) to the trace
   *
   * @param args    The object(s) to be traced as well
   * @param message
   */
  public void uncondEvent(String message, Object args)
  {
    Object[] allArgs = combine(message,args);
    _logger.logp(Level.EVENT, _source, "", formatStringFor(null, allArgs.length), allArgs);
  }

  /* -------------------------------------------------------------------------- */
  /* combine method
  /* -------------------------------------------------------------------------- */
  /**
   * Combine a string and an object into an object array. If the object is actually
   * an object array, prepend that string to the object array
   *
   * @param message The string
   * @param args    The object (or object array)
   * @return        The resulting object array (it's always a new object array)
   */
  private Object[] combine(String message,Object args)
  {
    if (args instanceof Object[])
    {
      Object[] originalArgs = (Object[]) args;
      Object[] allArgs = new Object[originalArgs.length+1];
      allArgs[0] = message;
      if (originalArgs.length > 0)
       System.arraycopy(originalArgs, 0, allArgs, 1, originalArgs.length);
      return allArgs;
    }
    else
    {
      return new Object[] {message,args};
    }
  }

  /* ------------------------------------------------------------------------ */
  /* info method
  /* ------------------------------------------------------------------------ */
  /**
   * This method traces an informational message.
   *
   * @param message The message key of the informational message
   */
  public void info(String message)
  {
    log(Level.INFORMATION, message);
  }

  /* ------------------------------------------------------------------------ */
  /* info method
  /* ------------------------------------------------------------------------ */
  /**
   * This method traces an informational message.
   *
   * @param message The message key of the informational message
   * @param args The object or objects to used as inserts
   */
  public void info(String message, Object args)
  {
    log(Level.INFORMATION, message, args);
  }

  /* ------------------------------------------------------------------------ */
  /* error method
  /* ------------------------------------------------------------------------ */
  /**
   * This method traces an error message.
   *
   * @param message The message key of the error message
   */
  public void error(String message)
  {
    log(Level.ERROR, message);
  }

  /* ------------------------------------------------------------------------ */
  /* error method
  /* ------------------------------------------------------------------------ */
  /**
   * This method traces an error message.
   *
   * @param message The message key of the error message
   * @param args The object or objects to used as inserts
   */
  public void error(String message, Object args)
  {
    log(Level.ERROR, message, args);
  }

  /* ------------------------------------------------------------------------ */
  /* audit method
  /* ------------------------------------------------------------------------ */
  /**
   * This method traces an audit message.
   *
   * @param message The message key of the audit message
   */
  public void audit(String message)
  {
    log(Level.AUDIT, message);
  }

  /* ------------------------------------------------------------------------ */
  /* audit method
  /* ------------------------------------------------------------------------ */
  /**
   * This method traces an audit message.
   *
   * @param message The message key of the audit message
   * @param args The object or objects to used as inserts
   */
  public void audit(String message, Object args)
  {
    log(Level.AUDIT, message, args);
  }

  /* ------------------------------------------------------------------------ */
  /* fatal method
  /* ------------------------------------------------------------------------ */
  /**
   * This method traces a fatal message.
   *
   * @param message The message key of the fatal message
   */
  public void fatal(String message)
  {
    log(Level.FATAL, message);
  }

  /* ------------------------------------------------------------------------ */
  /* fatal method
  /* ------------------------------------------------------------------------ */
  /**
   * This method traces a fatal message.
   *
   * @param message The message key of the fatal message
   * @param args The object or objects to used as inserts
   */
  public void fatal(String message, Object args)
  {
    log(Level.FATAL, message, args);
  }

  /* ------------------------------------------------------------------------ */
  /* service method
  /* ------------------------------------------------------------------------ */
  /**
   * This method traces a service message.
   *
   * @param message The message key of the service message
   */
  public void service(String message)
  {
    log(Level.SERVICE, message);
  }

  /* ------------------------------------------------------------------------ */
  /* service method
  /* ------------------------------------------------------------------------ */
  /**
   * This method traces a service message.
   *
   * @param message The message key of the service message
   * @param args The object or objects to used as inserts
   */
  public void service(String message, Object args)
  {
    log(Level.SERVICE, message, args);
  }

  /* -------------------------------------------------------------------------- */
  /* uncondFormattedEvent method
  /* -------------------------------------------------------------------------- */
  /**
   * This method unconditionally (i.e. even if the TraceGroup is disabled) a
   * formatted event
   *
   * @param message The message key of the unconditional event
   */
  public void uncondFormattedEvent(String message)
  {
    _logger.log(Level.EVENT, message);
  }

  /* -------------------------------------------------------------------------- */
  /* uncondFormattedEvent method
  /* -------------------------------------------------------------------------- */
  /**
   * This method unconditionally (i.e. even if the TraceGroup is disabled) a
   * formatted event
   *
   * @param message The message key of the unconditional event
   * @param args The object or objects to used as inserts
   */
  public void uncondFormattedEvent(String message, Object args)
  {
    if (args instanceof Object[])
    {
      _logger.log(Level.EVENT, message, (Object[])args);
    }
    else
    {
      _logger.log(Level.EVENT, message, args);
    }
  }

  /* ------------------------------------------------------------------------ */
  /* warning method
  /* ------------------------------------------------------------------------ */
  /**
   * This method traces a warning message.
   *
   * @param message The message key of the warning message
   */
  public void warning(String message)
  {
    log(Level.WARNING2, message);
  }

  /* ------------------------------------------------------------------------ */
  /* warning method
  /* ------------------------------------------------------------------------ */
  /**
   * This method traces a warning message.
   *
   * @param message The message key of the warning message
   * @param args The object or objects to used as inserts
   */
  public void warning(String message, Object args)
  {
    log(Level.WARNING2, message, args);
  }

  /* -------------------------------------------------------------------------- */
  /* log method
  /* -------------------------------------------------------------------------- */
  /**
   * Log a trace record that uses a message key as a parameter
   *
   * @param level   The trace type
   * @param message The message key
   */
  private void log(Level level, String message)
  {
    if (internalDoLog(level))
    {
      if (_overridingBundleName == null)
        _logger.logp(level, _source, "", message);
      else
        _logger.logrb(level, _source, "", _overridingBundleName, message);
    }

    if (isDefaultLog(level))
    {
      ResourceBundle bundle;
      if (_overridingBundle == null)
        bundle = _logger.getResourceBundle();
      else
      {
        bundle = _overridingBundle;
      }

      if (bundle == null)
      {
        System.out.println(message);
        writeToTestCaseLog(message);
      }
      else
      {
        try
        {
          String msg = bundle.getString(message);
          System.out.println(msg);
          writeToTestCaseLog(msg);
        }
        catch (MissingResourceException mre)
        {
          System.out.println(message);
          writeToTestCaseLog(message);
        }
      }
    }
  }

  /* -------------------------------------------------------------------------- */
  /* log method
  /* -------------------------------------------------------------------------- */
  /**
   * Log a trace record that uses a message key as a parameter
   *
   * @param level   The trace type
   * @param message The message key
   * @param args    The inserts of the message
   */
  public void log(Level level, String message, Object args)
  {
    if (internalDoLog(level))
    {
      if (args instanceof Object[])
      {
        if (_overridingBundleName == null)
          _logger.logp(level, _source, "", message, (Object[])args);
        else
          _logger.logrb(level, _source, "", _overridingBundleName, message, (Object[])args);
      }
      else
      {
        if (_overridingBundleName == null)
          _logger.logp(level, _source, "", message, args);
        else
          _logger.logrb(level, _source, "", _overridingBundleName, message, args);
      }
    }

    if (isDefaultLog(level))
    {
      ResourceBundle bundle;
      if (_overridingBundle == null)
        bundle = _logger.getResourceBundle();
      else
        bundle = _overridingBundle;

      if (bundle == null)
      {
        System.out.println(message);
        writeToTestCaseLog(message);
      }
      else
      {
        try
        {
          String lookedUpMessage = bundle.getString(message);
          Object[] inserts;
          if (args instanceof Object[])
          {
            inserts = (Object[])args;
          }
          else
          {
            inserts = new Object[] {args};
          }

          lookedUpMessage = MessageFormat.format(lookedUpMessage, inserts);
          System.out.println(lookedUpMessage);
          writeToTestCaseLog(lookedUpMessage);
        }
        catch (MissingResourceException mre)
        {
          System.out.println(message);
          writeToTestCaseLog(message);
        }
      }
    }
  }

  /**
   * Writes the specified text to the current testcase log.
   * @param msg the specified text
   */
  private void writeToTestCaseLog(String msg)
  {
    synchronized(Logger.class)
    {
      try
      {
        if (testcaseLogWriter != null) testcaseLogWriter.write(msg);
      } catch(IOException ioe)
      {
        // Fall back to standard mechanisms for reporting this error.
        ioe.printStackTrace();
      }//try
    }//sync

  }

  /* ------------------------------------------------------------------------ */
  /* extractStackTrace method
  /* ------------------------------------------------------------------------ */
  /**
   * This method is used so the stack trace of any exceptions ends up in the
   * log.
   *
   * @param t The exception to get the stack trace of.
   * @return  The stack trace.
   */
  private static final String extractStackTrace(Throwable t)
  {
    ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
    t.printStackTrace(new PrintStream(bytesOut));
    return new String(bytesOut.toByteArray()).intern();
  }

  /* ------------------------------------------------------------------------ */
  /* doLog method
  /* ------------------------------------------------------------------------ */
  /**
   * This method works out if the log should be made based on information in
   * the logging specification.
   *
   * @param level The level being logged.
   * @return      whether to log of not.
   */
  public boolean doLog(Level level)
  {
    return isDefaultLog(level) || internalDoLog(level);
  }

  private boolean internalDoLog(Level level)
  {
    boolean result;
    // Get a reference to the current map. After doing this, we don't care
    // if the doLog is reset (as long as we use our reference :-)
    ConcurrentMap<Level,Boolean> theMap = _doLog.get();

    if (!theMap.containsKey(level))
    {
      result = false;
      for(LogSpecElement element : _logSpec)
      {
        Boolean elementResult = element.matchTrace(_source, _logGroup, level);
        if (elementResult != null)
        {
          result = elementResult;
        }
      }
      theMap.put(level,result);
    }
    else
    {
      result = theMap.get(level);
    }
    return result;
  }

  private boolean isDefaultLog(Level level)
  {
    return (level == Level.INFORMATION ||
            level == Level.WARNING2 ||
            level == Level.ERROR ||
            level == Level.AUDIT);
  }
}
