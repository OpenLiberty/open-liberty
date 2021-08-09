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
package com.ibm.ws.sib.ffdc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ibm.ws.ffdc.IncidentStream;
import com.ibm.ws.sib.utils.ffdc.SibDiagnosticModule;

/* ************************************************************************** */
/**
 * A class that allows a unit test to declare that some ffdcs are expected and
 * to allow inspection of those FFDCs.
 *
 */
/* ************************************************************************** */
public class FFDCEngine
{
  private static boolean _ffdcsCauseError = true;
  private static boolean _ffdcsGetPrinted = false;

  private static List<FFDCEntry> _ffdcs = new ArrayList<FFDCEntry>();

  private static Set<Class> _expected = new HashSet<Class>();

  /**
   * Diagnostic module to be driven by processException
   */
  private static SibDiagnosticModule _diagModule = null;

  /**
   * The incident stream object that will be used to log the FFDC.
   */
  private static IncidentStream _incidentStream = null;


  /* -------------------------------------------------------------------------- */
  /* getFFDCs method
  /* -------------------------------------------------------------------------- */
  /**
   * @return the list of FFDCs since the start of the JVM or the last reset call
   */
  public static List getFFDCs()
  {
    return _ffdcs;
  }

  /* -------------------------------------------------------------------------- */
  /* reset method
  /* -------------------------------------------------------------------------- */
  /**
   * Reset the list of recorded FFDCs
   */
  public static void reset()
  {
    _ffdcsCauseError = true;
    _ffdcsGetPrinted = false;
    _ffdcs = new ArrayList<FFDCEntry>();
    _expected = new HashSet<Class>();
    _diagModule = null;
    _incidentStream = null;

  }

  /* -------------------------------------------------------------------------- */
  /* setupDiagnosticModule
  /* -------------------------------------------------------------------------- */
  /**
   * Set up the diagnostic module for use by processException.
   * @param diag      The diagnostic module to be invoked <b>in addition</b> to
   *                  the normal diagnostic module processing
   * @param incStream The incident stream to be passed when invoking the diagnostic module
   */
  public static void setupDiagnosticModule(SibDiagnosticModule diag, IncidentStream incStream)
  {
    if (diag == null) throw new IllegalArgumentException("NULL SibDiagnosticModule");
    if (incStream == null) throw new IllegalArgumentException("NULL IncidentStream");

    _diagModule = diag;
    _incidentStream = incStream;

  }

  /* -------------------------------------------------------------------------- */
  /* allowAllFFDCs method
  /* -------------------------------------------------------------------------- */
  /**
   * Allow all FFDCs to proceed without causing an FFDCAssertionError (Note:
   * passing null as the exception on a processException call will ALWAYS cause
   * an FFDCAssertionError to be thrown
   */
  public static void allowAllFFDCs()
  {
    _ffdcsCauseError = false;
  }

  /* -------------------------------------------------------------------------- */
  /* printFFDCs method
  /* -------------------------------------------------------------------------- */
  /**
   * Print all FFDCs until reset is called
   */
  public static void printAllFFDCs()
  {
    _ffdcsGetPrinted = true;
  }

  /* -------------------------------------------------------------------------- */
  /* processException method
  /* -------------------------------------------------------------------------- */
  /**
   * Process an exception given to the engine
   *
   * @param th          The Throwable given to the engine (null is always wrong)
   * @param sourceId    The id of the source generating the FFDC
   * @param probeId     The probe id in the source identifying where the FFDC was generated
   * @param callerThis  The object generating the FFDC (possibly null)
   * @param objectArray An array of objects to the passed to the engine (possibly null)
   */
  public static void processException(Throwable th, String sourceId, String probeId, Object callerThis, Object[] objectArray)
  {
    if (th instanceof FFDCAssertionError)
    {
      // Just rethrow the FFDCAssertionError - DON'T relog it or create an even bigger one!
      throw (FFDCAssertionError)th;
    }
    else
    {
      FFDCEntry newEntry = new FFDCEntry(th,sourceId,probeId,callerThis,objectArray);

      _ffdcs.add(newEntry);
      if (_ffdcsGetPrinted)
      {
        System.out.println(newEntry.toString());
      }

      // If there has been a diagnostic module registered, then drive it now.
      if (_diagModule != null) _diagModule.ffdcDumpDefault(th, _incidentStream, callerThis, objectArray, sourceId);

      if (th == null)
        throw new FFDCAssertionError("null passed as the Throwable to processException");

      if (_ffdcsCauseError)
      {
        boolean expected = false;

        for(Iterator it = _expected.iterator(); !expected && it.hasNext();)
        {
          Class cl = (Class)it.next();
          if (cl.isInstance(th))
            expected = true;
        }

        if (!expected)
        throw new FFDCAssertionError(th,"processException("+th+",\""+sourceId+"\",\""+probeId+"\","+callerThis+","+Arrays.toString(objectArray)+")",newEntry);
      }
    }
  }

  /* -------------------------------------------------------------------------- */
  /* expecting method
  /* -------------------------------------------------------------------------- */
  /**
   * @param cl The class of the exception that is expected
   */
  public static void expecting(Class cl)
  {
    _expected.add(cl);
  }
}
