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

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.sib.mfp.JsMessageHandle;
import com.ibm.ws.sib.mfp.JsMessageHandleRestorer;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.util.ArrayUtil;
import com.ibm.ws.sib.utils.HexString;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SIMessageHandle;

/**
 *  This class extends and implements the abstract com.ibm.ws.sib.mfp.JsMessageHandleRestorer
 *  class and provides the concrete implementations of the methods for
 *  creating restoring JsMessageHandles.
 *  <p>
 *  The class must be public so that the abstract class static
 *  initialization can create an instance of it at runtime.
 *
 */
public class JsMessageHandleRestorerImpl extends JsMessageHandleRestorer {

  private static TraceComponent tc = SibTr.register(JsMessageHandleRestorerImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);
  private static final TraceNLS nls = TraceNLS.getTraceNLS(MfpConstants.MSG_BUNDLE);


  /**
   *  Restore a JsMessageHandle from a byte[].
   *
   *  @param data             The byte array containing the flattened JsMessageHandle
   *
   *  @return SIMessageHandle The restored MessageHandle (the object returned is
   *  guaranteed to actually be a JsMessageHandle).
   *
   *  @exception IllegalArgumentException Thrown if the parameter is null or contains
   *  data that cannot be understood.
   */
  public SIMessageHandle restoreFromBytes(byte[] data)
      throws IllegalArgumentException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "restoreFromBytes");

    // Minimum valid length is an array with a short version number, integer (of value 0) for uuid length
    // and long for value.  Any byte [] shorter than this is invalid for restoration.
    int minValidArrayLength = ArrayUtil.SHORT_SIZE + ArrayUtil.INT_SIZE + ArrayUtil.LONG_SIZE;

    if ((data == null))
    {
      String exString = nls.getFormattedMessage("NULL_HANDLE_PASSED_FOR_RESTORE_CWSIF0032",
          new Object[] {"NULL"},
          "JsMessageHandleRestorer.restoreFromBytes called with NULL.");

      IllegalArgumentException e = new IllegalArgumentException(exString);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "restoreFromBytes called with NULL, throwing IllegalArgumentException");
      throw e;
    }
    else if (data.length < minValidArrayLength)
    {
      String exString = nls.getFormattedMessage("RESTORE_FROM_BYTES_ARRAY_TOO_SHORT_CWSIF0033"
          ,null
          ,"Array length was shorter than the minimum valid array length.");

      IllegalArgumentException e = new IllegalArgumentException(exString);

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
        String formattedBytes = SibTr.formatBytes(data,0,data.length);
        SibTr.debug(tc, "Throwing IllegalArgumentException : " + exString, formattedBytes);
      }
      throw e;
    }

    int offset = 0;

    // Read the Version number
    short vNumber = ArrayUtil.readShort(data, offset);
    offset += ArrayUtil.SHORT_SIZE;

    // 1 is the only valid versionNumber at the moment
    if (vNumber != 1)
    {


      String exString = nls.getFormattedMessage("RESTORE_FROM_BYTES_UNKNOWN_VERSION_NUMBER_CWSIF0034"
          ,null
          ,"Unknown version information detected in MessageHandle : "+vNumber);

      IllegalArgumentException e = new IllegalArgumentException(exString);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
        String formattedBytes = SibTr.formatBytes(data,0,data.length);
        SibTr.debug (tc, "Throwing IllegalArgumentException : " + exString, formattedBytes);
      }
      throw e;
    }

    int uuidLength = ArrayUtil.readInt(data, offset);
    offset += ArrayUtil.INT_SIZE;

    // the data array should contain the short version num, int uuid length, the uuid and long 'value'
    // so, uuid length must be (data.length - (short + int + long) )
    //                                        (short + int + long = minValidArrayLength)
    // We do not need to externalise exactly what sums we are doing to check things - just tell
    // them that something isn't right.

    if (uuidLength != (data.length - minValidArrayLength) )
    {


      String exString = nls.getFormattedMessage("RESTORE_FROM_BYTES_INTERNAL_LENGTH_CHECK_FAIL_CWSIF0035"
          ,null
          ,"Invalid data passed to in restoreFromBytes. Internal length checks do not match.");

      IllegalArgumentException e = new IllegalArgumentException(exString);

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
        String formattedBytes = SibTr.formatBytes(data,0,data.length);
        SibTr.debug(tc, "Throwing IllegalArgumentException : " + exString, formattedBytes);
      }
      throw e;
    }

    JsMessageHandle handle = null;

    if (uuidLength == 0)
    {
      // there is no uuid, leave the uuid as null and construct the handle with just a value
      long value = ArrayUtil.readLong(data, offset);
      handle = new JsMessageHandleImpl(value);
    }
    else
    {
      // read the uuid bytes in and construct a SIBUuid8 from it
      // then construct the handle
      byte [] uuidBytes = ArrayUtil.readBytes(data, offset, uuidLength);
      offset += uuidLength;
      SIBUuid8 uuid = new SIBUuid8(uuidBytes);
      long value = ArrayUtil.readLong(data, offset);
      handle = new JsMessageHandleImpl(uuid, Long.valueOf(value));
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "restoreFromBytes", handle);
    return handle;
  }

  /**
   *  Restore a JsMessageHandle from a String.
   *
   *  @param data             The String containing the flattened JsMessageHandle.
   *
   *
   *  @return SIMessageHandle The restored MessageHandle (the object returned is
   *  guaranteed to actually be a JsMessageHandle).
   *
   *  @exception IllegalArgumentException Thrown if the parameter is null or contains
   *  data that cannot be understood.
   */
  public SIMessageHandle restoreFromString(String data)
      throws IllegalArgumentException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "restoreFromString");

    if ((data == null) || data.equals(""))
    {
      String badValueType;
      if (data==null) {badValueType = "NULL";}
      else {badValueType = "Empty string";}

      String exString = nls.getFormattedMessage("NULL_HANDLE_PASSED_FOR_RESTORE_CWSIF0032"
          ,new Object[] {badValueType}
          ,"restoreFromString called with invalid parameter of "+badValueType+".");

      IllegalArgumentException e = new IllegalArgumentException(exString);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "restoreFromString called with invalid parameter of "+badValueType+".", e);
      throw e;
    }

    byte [] bytes = HexString.hexToBin(data,0);
    // try to restore, if there is an IllegalArgumentException let it propagate up
    SIMessageHandle handle = restoreFromBytes(bytes);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "restoreFromString");
    return handle;
  }
}
