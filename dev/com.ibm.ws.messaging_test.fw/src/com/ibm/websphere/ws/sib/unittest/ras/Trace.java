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
package com.ibm.websphere.ws.sib.unittest.ras;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import com.ibm.ejs.ras.SibTraceBaseClass;
import com.ibm.ws.sib.unittest.ras.Logger;
import com.ibm.ws.sib.utils.ras.SibMessage;

/**
 * <p>This class is designed to allow users to manipulate the unit test RAS
 *   infrastructure, to enable, disable and do assertions on the log.
 * </p>
 *
 * <p>SIB build component: sib.unittest.ras</p>
 *
 * @author nottinga
 * @version 1.14
 * @since 1.0
 */
public class Trace
{
  /** Trace specs that mean disable */
  private static final String[] DISABLING_SPECS = { "info=enabled", "*=info", "*=all=disabled", "info", "" };
  
  /** The old System.out value */
  private static final PrintStream _oldOut = System.out;
  
  /** The old System.err value */
  private static final PrintStream _oldErr = System.err;
  
  /** The SibMessageListener we can attach to SibMessage when logging SibMessage */
  private static SibMessageListener _sibMessageListener = new SibMessageListener();
  
  /** Is the message listener attached to sib message? */
  private static boolean _listenerAttached = false;
  
  /** The base trace spec (null if no base trace spec has been provided */
  private static String _baseTraceSpec = null;
  
  /** Has the trace file been created? */
  private static boolean _createdTraceFile = false;
  
  /** Have we captured System.out and System.Err and are sending them to trace? */
  private static boolean _capturedOutAndErr = false;
  
  /* -------------------------------------------------------------------------- */
  /* setBaseTraceSpec method
  /* -------------------------------------------------------------------------- */
  /**
   * Set the base trace spec which will be used if no other trace spec is provided
   * or the trace is reset. Note that this method has the side effect of also
   * resetting the trace
   * 
   * @param baseTraceSpec
   */
  public static void setBaseTraceSpec(String baseTraceSpec)
  {
    _baseTraceSpec = baseTraceSpec;
   resetTrace(); // Implicit reset    
  }
  
  /* ------------------------------------------------------------------------ */
  /* enableTrace method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method can be used to enable trace. It takes a WAS trace specification
   * 
   * @param traceSpec The WAS trace specification.
   */
  public static void enableTrace(String traceSpec)
  {
    if (doesTraceSpecEnableTrace(traceSpec))
      doEnable(traceSpec);
    else
    {
      disableTrace();
    }
  }

  /* -------------------------------------------------------------------------- */
  /* doesTraceSpecEnableTrace method
  /* -------------------------------------------------------------------------- */
  /**
   * Determine if a particular trace specification is one that enables trace
   * (i.e. at least one trace group will generate trace data) or one that disables
   * trace
   * 
   * @param traceSpec The trace specification to be examined
   * @return true if the trace specification will generate at least some trace
   */
  private static boolean doesTraceSpecEnableTrace(String traceSpec)
  {
    // Is it a trace spec that really means disable?
    boolean enable = true;
    if (traceSpec == null)
    {
      enable = false;
    }
    else
    {
      for(int i=0; enable && i<DISABLING_SPECS.length; i++)
      {
        if (DISABLING_SPECS[i].equals(traceSpec))
        {
          enable = false;
        }
      }
    }
    return enable;
  }

  /* ------------------------------------------------------------------------ */
  /* disableTrace method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method can be used to disable the tracing support. 
   */
  public static void disableTrace()
  {
    doDisable();
    SibTraceBaseClass.setAllTracingEnabled(false);
    SibTraceBaseClass.setSomeTracingEnabled(false);
  }

  /* -------------------------------------------------------------------------- */
  /* resetTrace method
  /* -------------------------------------------------------------------------- */
  /**
   * Reset the trace back to its base state
   */
  public static void resetTrace()
  {
    if (_baseTraceSpec != null)
    {
      doEnable(_baseTraceSpec);
    }
    else
    {
      doDisable();
      SibTraceBaseClass.setAllTracingEnabled(SibTraceBaseClass.isEmmaEnabled());
      SibTraceBaseClass.setSomeTracingEnabled(SibTraceBaseClass.isEmmaEnabled());
    }
  }
  
  /* -------------------------------------------------------------------------- */
  /* doEnable method
  /* -------------------------------------------------------------------------- */
  /**
   * @param traceSpec
   */
  private static void doEnable(String traceSpec)
  {
    // Create the trace file if we haven't done so yet
    if (!_createdTraceFile)
    {
      String logsDir = System.getProperty("LOGGING_DIR", "logs");
      File traceDir = new File(logsDir, "trace");
      traceDir.mkdirs();
      File traceFile = new File(traceDir, "trace.log");
      try
      {
        Logger.setTraceLocation(traceFile);
      }
      catch (IOException ioe)
      {
        ioe.printStackTrace();
      }
      
      _createdTraceFile = true;
    }
 
    // Ensure System.out and System.err get copied to trace
    if (!_capturedOutAndErr)
    {
      Logger.captureSystemOut();
      Logger.captureSystemErr();
      _capturedOutAndErr = true;
    }

    // Indicate that at least some trace is enabled
    SibTraceBaseClass.setSomeTracingEnabled(true);
    
    // Let the logging infrastructure know what the trace spec is
    Logger.setLoggingSpecification(traceSpec);
  }
 
  /* -------------------------------------------------------------------------- */
  /* doDisable method
  /* -------------------------------------------------------------------------- */
  /**
   * Mark the trace as disabled, regardless of anything else
   */
  private static void doDisable()
  {
    // Let the logging infrastructure know that the trace spec is changing
    Logger.setLoggingSpecification(null);

    // Stop copying System.out and System.Err (if copying them)
    if (_capturedOutAndErr)
    {
      System.setOut(_oldOut);
      System.setErr(_oldErr);
      _capturedOutAndErr = false;
    }
  }
  
  /* ------------------------------------------------------------------------ */
  /* enableAssertionSupport method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method is used to enable the assertion support in trace. 
   */
  public static void enableAssertionSupport()
  {
    if (!_listenerAttached)
    {
      SibMessage.addListener(_sibMessageListener);
      _listenerAttached = true;
    }
  }
  
  /* ------------------------------------------------------------------------ */
  /* disableAssertionSupport method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method is used to disable assertion support in trace. 
   */
  public static void disableAssertionSupport()
  {
    if (_listenerAttached)
    {
      SibMessage.removeListener(_sibMessageListener);
      _sibMessageListener.reset(); // To recover the memory
      _listenerAttached = false;
    }
  }

  /* ------------------------------------------------------------------------ */
  /* reset method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method can be used to reset the assertion state. 
   */
  public static void reset()
  {
    _sibMessageListener.reset();
  }
  
  /* ------------------------------------------------------------------------ */
  /* assertLogged method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method allows the caller to check that a trace statement has been
   * executed that would result in an entry in the log.
   * 
   * @param record The log record to assert the existence of.
   */
  public static void assertLogged(LogRecord record)
  {
    _sibMessageListener.assertLogged(record);
  }
  
  /* ------------------------------------------------------------------------ */
  /* assertNotLogged method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method allows the caller to check that a trace statement has not been
   * executed that would result in an entry in the log.
   * 
   * @param record The log record that should not have been logged.
   */
  public static void assertNotLogged(LogRecord record)
  {
    _sibMessageListener.assertNotLogged(record);
  }
}
