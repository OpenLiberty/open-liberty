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
package com.ibm.ejs.ras;

import java.util.Properties;

import com.ibm.websphere.ws.sib.unittest.ras.Trace;

/* ************************************************************************** */
/**
 * This class is used to underpin the TraceElement so that the usurping class
 * loader knows to leave this class alone without changing its behaviour for the
 * the sub classes (i.e. they will be affected by the usurping class loader)
 */
/* ************************************************************************** */
public class SibTraceBaseClass
{
  /** The string all code coverage properties are assumed to start with */
  private static final String EMMA = "emma.enabled"; 
  
  /** The name of the trace spec we'll use if specified */
  private static final String UNITTEST_RAS = "trace.spec";
  
  /** The name of the trace spec we'll apply if EMMA code coverage is enabled. */
  private static final String EMMA_UNITTEST_RAS = "emma.trace.spec";
  
  /** Is all tracing enabled? */
  private static boolean _allTracing = false;
  
  /** Are we tracing in an emma mode (i.e. we claim trace is on, but discard all the results) */
  private static boolean _emmaEnabled = false;
  
  /** Is any tracing enabled? */
  private static boolean _anyTracing = false;
  
  /** Decide how we're going to operate */
  static
  {
    Properties props = System.getProperties();
    boolean emma = Boolean.getBoolean(EMMA);
    String traceSpec = props.getProperty(UNITTEST_RAS);

    if (traceSpec != null)
    {
      Trace.setBaseTraceSpec(traceSpec);
    }
    else if (emma)
    {
      // If EMMA is enabled then give a component the chance to nominate a special
      // trace string (for use only under code coverage).
      String emmaSpec = props.getProperty(EMMA_UNITTEST_RAS);
      if (emmaSpec != null)
      {
        Trace.setBaseTraceSpec(emmaSpec);
      } else
      {
        // Default EMMA behaviour is to turn on all trace in order to ensure that
        // the tracepoint branches get hit.
        _anyTracing = true;
        _allTracing = true;
        _emmaEnabled = true;
      }
    }
  }

  /* -------------------------------------------------------------------------- */
  /* setSomeTracingEnabled method
  /* -------------------------------------------------------------------------- */
  /**
   * Allow the unittest ras Trace class to indicate that some trace is enabled
   * 
   * @param flag
   */
  public static void setSomeTracingEnabled(boolean flag)
  {
    _anyTracing = flag;
  }
  
  /* -------------------------------------------------------------------------- */
  /* isAnyTracingEnabled method
  /* -------------------------------------------------------------------------- */
  /**
   * @return true if any tracing has been enabled (or so we claim anyway....)
   */
  public static boolean isAnyTracingEnabled()
  {
    return _anyTracing;
  }
  
  /* -------------------------------------------------------------------------- */
  /* setAllTracingEnabled method
  /* -------------------------------------------------------------------------- */
  /**
   * Allow the unittest ras Trace class to indicate that all trace should be enabled
   * 
   * @param flag
   */
  public static void setAllTracingEnabled(boolean flag)
  {
    _allTracing = flag;
  }
  
  /* -------------------------------------------------------------------------- */
  /* isAllTracingEnabled method
  /* -------------------------------------------------------------------------- */
  /**
   * @return true if any tracing has been enabled (or so we claim anyway....)
   */
  public static boolean isAllTracingEnabled()
  {
    return _allTracing;
  }
  
  /* -------------------------------------------------------------------------- */
  /* isEmmaEnabled method
  /* -------------------------------------------------------------------------- */
  /**
   * @return is emma enabled
   */
  public static boolean isEmmaEnabled()
  {
    return _emmaEnabled;
  }
}
