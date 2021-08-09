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

package com.ibm.ws.sib.admin.internal;

import java.io.UnsupportedEncodingException;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIDataGraphSchemaNotFoundException;
import com.ibm.websphere.sib.exception.SIMessageException;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.mfp.IncorrectMessageTypeException;
import com.ibm.ws.sib.mfp.JsJmsBytesMessage;
import com.ibm.ws.sib.mfp.JsJmsMapMessage;
import com.ibm.ws.sib.mfp.JsJmsMessage;
import com.ibm.ws.sib.mfp.JsJmsObjectMessage;
import com.ibm.ws.sib.mfp.JsJmsStreamMessage;
import com.ibm.ws.sib.mfp.JsJmsTextMessage;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.MessageType;
import com.ibm.ws.sib.mfp.ObjectFailedToSerializeException;
import com.ibm.ws.sib.processor.exceptions.SIMPException;
import com.ibm.ws.sib.processor.runtime.SIMPQueuedMessageControllable;
import com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageControllable;
import com.ibm.ws.sib.utils.ras.SibTr;

public class JsMessagePoint extends JsObject {

  public static final String $sccsid = "@(#) 1.23 SIB/ws/code/sib.admin.impl/src/com/ibm/ws/sib/admin/impl/JsMessagePoint.java, SIB.admin, WASX.SIB, rr1243.02 07/07/04 11:44:20 [10/25/12 16:31:40]";
  private static final TraceComponent tc = SibTr.register(JsMessagePoint.class, JsConstants.TRGRP_AS, JsConstants.MSG_BUNDLE);

  // Debugging aid
  static {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc, "Source info: @(#)SIB/ws/code/sib.admin.impl/src/com/ibm/ws/sib/admin/impl/JsMessagePoint.java, SIB.admin, WASX.SIB, rr1243.02 1.23");
  }

  // Properties of the MBean
  java.util.Properties props = new java.util.Properties();

  public java.util.Properties getProperties() {
    return (java.util.Properties) props.clone();
  }

  private byte[] getData(byte[] in, java.lang.Integer size) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getData", new Object[] { in, size });
    byte tmp[] = null;
    if (in != null) {
      int len = 1024;
      if (size.intValue() > 0)
        len = size.intValue();
      if (len > in.length)
        len = in.length;
      tmp = new byte[len];
      System.arraycopy(in, 0, tmp, 0, len);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getData", tmp);
    return tmp;
  }

  public byte[] getMessageData(SIMPQueuedMessageControllable qmc, java.lang.Integer size) throws Exception {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getMessageData", size.toString());

    byte[] data = null;

    if (qmc != null) {
      try {
        JsMessage jm = qmc.getJsMessage();
        data = getMessageData(jm, size);
      }
      catch (SIMPException e) {
        // No FFDC code needed
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "getMessageData", e);
        throw e;
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getMessageData");
    return data;
  }

  public byte[] getMessageData(SIMPRemoteMessageControllable rmc, java.lang.Integer size) throws Exception {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getMessageData", size.toString());

    byte[] data = null;

    if (rmc != null) {
      try {
        JsMessage jm = rmc.getJsMessage();
        data = getMessageData(jm, size);
      }
      catch (SIMPException e) {
        // No FFDC code needed
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "getMessageData", e);
        throw e;
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getMessageData");
    return data;
  }

  private byte[] getMessageData(JsMessage jm, java.lang.Integer size) throws IncorrectMessageTypeException, SIDataGraphSchemaNotFoundException, SIMessageException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getMessageData", new Object[] { jm, size });

    byte data[] = null;

    MessageType mt = jm.getJsMessageType();

    if (mt == MessageType.JMS) {

      JsJmsMessage m = jm.makeInboundJmsMessage();

        if (m instanceof JsJmsTextMessage) {
          JsJmsTextMessage msg = (JsJmsTextMessage) m;
          String text = null;
          try {
            text = msg.getText();
          }
          catch (UnsupportedEncodingException e) {
            // No FFDC code needed
          }
          // Check that there is a message body to call getBytes on.
          if (text != null) {
            data = getData(text.getBytes(), size);
          }
        }
        else if (m instanceof JsJmsBytesMessage) {
          JsJmsBytesMessage msg = (JsJmsBytesMessage) m;
          data = getData(msg.getBytes(), size);
        }
        else if (m instanceof JsJmsObjectMessage) {
          JsJmsObjectMessage msg = (JsJmsObjectMessage) m;
          try
          {
        	  data = getData(msg.getSerializedObject(), size);
          }
          catch(ObjectFailedToSerializeException ofse)
          {
              // No FFDC code needed
        	  data = null;
          }
        }
        else if (m instanceof JsJmsMapMessage) {
          JsJmsMapMessage msg = (JsJmsMapMessage) m;
          try {
            data = getData(msg.getUserFriendlyBytes(), size);
          }
          catch (UnsupportedEncodingException e) {
            // No FFDC code needed
          }
        }
        else if (m instanceof JsJmsStreamMessage) {
          JsJmsStreamMessage msg = (JsJmsStreamMessage) m;
          try {
            data = getData(msg.getUserFriendlyBytes(), size);
          }
          catch (UnsupportedEncodingException e) {
            // No FFDC code needed
          }
        }
        else {
        }
    }
    

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getMessageData", data);
    return data;
  }
}
