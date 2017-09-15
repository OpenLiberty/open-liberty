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

package com.ibm.ws.sib.mfp.impl;

import java.util.List;

import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.jmf.JMFMessage;
import com.ibm.ws.sib.mfp.jmf.tools.JSFormatter;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ffdc.SibDiagnosticModule;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.ws.sib.utils.RuntimeInfo;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.IncidentStream;


/**
 * This is a diagnostic module for the MFP component.  It will be invoked when any
 * call to FFDCFilter is made for the classes or packages for which it is registered.
 * This allows extra information to be written to the FFDC logs when an error
 * occurs.
 */

public class MfpDiagnostics extends SibDiagnosticModule {
  private static TraceComponent tc = SibTr.register(MfpDiagnostics.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /**
   * Constants to control the amount of message data which is * traced out.
   */
  private final static String DIAGNOSTIC_DATA_LIMIT_PROP = "com.ibm.ws.sib.mfp.DIAGNOSTIC_DATA_LIMIT";
  private final static int DEFAULT_DIAGNOSTIC_DATA_LIMIT_INT = 4096;
  private final static int DIAGNOSTIC_DATA_LIMIT_INT;

  // Initialise DIAGNOSTIC_DATA_LIMIT_INT from custom property if there is one
  // This is the max bytes to dump or trace  from a buffer/slice.  There could
  // be an enormous amount of user data - so we don't want to dump it all unless
  // really necessary.
  // If not set, use a default of 4k which should be enough to contain the
  // Jetstream headers etc and enough data to figure out what is going on.
  static {
    String diagnosticDataLimitString = RuntimeInfo.getProperty(DIAGNOSTIC_DATA_LIMIT_PROP);
    int limit = DEFAULT_DIAGNOSTIC_DATA_LIMIT_INT;
    if(diagnosticDataLimitString != null) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "DIAGNOSTIC_DATA_LIMIT_PROP="+ diagnosticDataLimitString);
      try {
        limit = Integer.parseInt(diagnosticDataLimitString);
      }
      catch(NumberFormatException nfe) {
        // No FFDC code needed
      }
    }
    DIAGNOSTIC_DATA_LIMIT_INT = limit;
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "DIAGNOSTIC_DATA_LIMIT_INT="+ getDiagnosticDataLimitInt());
  }

  // Package level method, as it is used by other MFP impl classes
  static int getDiagnosticDataLimitInt() {
    return DIAGNOSTIC_DATA_LIMIT_INT;
  }

  // No one else should be creating instances of this class
  private MfpDiagnostics() {
  }

  private static MfpDiagnostics singleton = null;

  // These strings list the MFP packages and classes for which this DM is to be
  // registered.  This list can include actual classes, actual pacakges or
  // higher level package prefixes.  Remember that our 'ffdcDump' methods will be
  // invoked for any FFDCFilter call in any of the regsitered packages/classes, so
  // we may need to compromise between registering everything (to ensure we don't
  // forget if we add new classes) and performance when we have 'expected'
  // exceptions occuring (from DataMediators for example).
  private static String[] packageList = {
    "com.ibm.ws.sib.mfp"
  };

  /**
   * Initialise the diagnostic module by creating the singleton instance and registering
   * it with the diagnostic engine in Websphere.
   */
  public static MfpDiagnostics initialize() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "initialize");

    // We should only do this once
    if (singleton == null) {
      singleton = new MfpDiagnostics();
      singleton.register(packageList);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "initialize");
    return singleton;
  }

  /*
   * ffdcDump methods.
   *
   * Any method whose name starts with "ffdcDumpDefault" will get invoked for _every_
   * FFDCFilter that occurs in the registered packages/classes.  There must be at least
   * one such default method defined, or the registration with the diagnostic engine
   * will fail.
   *
   * Methods whose names start "ffdcDump" (but don't have the "Default" bit) will only
   * get invoked if something, somewhere in the depths websphere has been configured
   * to enable them.  Beats me how you configure this stuff, but we don't want any of
   * these so I guess it doesn't matter.
   */

  /**
   * Default ffdc dump routine - always invoked
   */
  public void ffdcDumpDefault(Throwable t, IncidentStream is, Object callerThis, Object[] objs, String sourceId) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "ffdcDumpDefault");

    // First trace the Throwable, so that we can actually spot the exception in the trace
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "FFDC for " + t);
    // Trace the first line of the stacktrace too, as it'll help debugging
    if (t != null) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "  at... " + t.getStackTrace()[0]);
    }

    // Capture the Messaging Engine Name and other default information
    super.captureDefaultInformation(is);

    // The objs parm may contain one request or several. Each call to dumpUsefulStuff()
    // deals with a single request. If it contains several requests, each entry
    // in objs is itself an Object[]. If not, the first entry is just an Object.
    if (objs != null && objs.length > 0) {
      if (objs[0] instanceof Object[]) {
        for (int i = 0; i < objs.length; i++) {
          // Belt & braces - we don't want FFDC to fail because someone inserted something invalid
          if (objs[i] instanceof Object[]) dumpUsefulStuff(is, (Object[])objs[i]);
        }
      }
      else {
        dumpUsefulStuff(is, objs);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "ffdcDumpDefault");
  }


  // Figure out what sort of information is to be dumped, and call the appropriate
  // routine to dump it.
  private void dumpUsefulStuff(IncidentStream is, Object[] objs) {

    // The first parameter is a marker telling us what sort of data we have in subsequent
    // parameters.
    if (objs != null && objs.length > 0) {
      if (objs[0] == MfpConstants.DM_BUFFER && objs.length >= 4)
        dumpJmfBuffer(is, (byte[])objs[1], ((Integer)objs[2]).intValue(), ((Integer)objs[3]).intValue());
      else if (objs[0] == MfpConstants.DM_MESSAGE && objs.length >= 2)
        dumpJmfMessage(is, (JMFMessage)objs[1], objs[2]);
      else if (objs[0] == MfpConstants.DM_SLICES && objs.length >= 2)
        dumpJmfSlices(is, (List<DataSlice>)objs[1]);
    }

  }


  // Dump the contents of a JMF buffer.  This could be large - if there's a lot of
  // user data - so we only dump at most the first 4K bytes.  This should be enough
  // to contain the Jetstream headers etc.
  private void dumpJmfBuffer(IncidentStream is, byte[] frame, int offset, int length) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "dumpJmfBuffer");

    if (frame != null) {
      // If the length passed in is 0 we probably have rubbish in both the length
      // and the offset so display the entire buffer (or at least the first 4k)
      if (length == 0) {
        is.writeLine("Request to dump offset=" + offset + " length=" + length + " implies bad data so dumping buffer from offset 0.","");
        offset = 0;
        length = frame.length;
      }
      // otherwise ensure we can't fall off the end of the buffer
      else if ((offset + length) > frame.length) {
        length = frame.length - offset;
      }
      try {
        String buffer = SibTr.formatBytes(frame, offset, length, getDiagnosticDataLimitInt());
        is.writeLine("JMF data buffer", buffer);
      } catch (Exception e) {
        // No FFDC code needed - we are FFDCing!
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "dumpJmfBuffer failed: " + e);
      }
    } else
      is.writeLine("No JMF buffer data available", "");

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "dumpJmfBuffer");
  }

  // Dump the contents of a JMF message.  We print out the header fields then take
  // a stab at the API fields and the payload.
  // Also write out what sort of message we think we have (first).
  private void dumpJmfMessage(IncidentStream is, JMFMessage jmfMsg, Object msg) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "dumpJmfMessage");

    if (msg != null) {
      is.writeLine("Message is of class: ", msg.getClass().getName());
    }
    if (jmfMsg != null) {
      try {
        String buffer = JSFormatter.format(jmfMsg);
        is.writeLine("JMF message", buffer);
        
      } catch (Exception e) {
        // No FFDC code needed - we are FFDCing!
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "dumpJmfMessage failed: " + e);
      }
    } else
      is.writeLine("No JMF message available", "");

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "dumpJmfMessage");
  }

  // Dump the contents of a set of DataSlices.  This could be large - if there's a lot of
  // user data - so we only dump at most the first 4K bytes of each slice.
  private void dumpJmfSlices(IncidentStream is, List<DataSlice> slices) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "dumpJmfSlices");

    if (slices != null) {
      try {
        is.writeLine("JMF data slices", SibTr.formatSlices(slices, getDiagnosticDataLimitInt()));
      }
      catch (Exception e) {
        // No FFDC code needed - we are FFDCing!
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "dumpJmfSlices failed: " + e);
      }
    }
    else {
      is.writeLine("No JMF DataSlices available", "");
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "dumpJmfSlices");
  }

}
