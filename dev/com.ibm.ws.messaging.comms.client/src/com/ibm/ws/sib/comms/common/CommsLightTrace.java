/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.common;

import javax.transaction.xa.XAException;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.mfp.AbstractMessage;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SIMessageHandle;

/* @notracescan@ - stop automatic addition of isAnyTracingEnabled
 * callers are responsible for checking that before calling any method in this class
 */

/**
 * This class allows minimal tracing of messages and transactions flowing
 * over comms between server and client.
 * Entries are enabled in a trace group that can be enabled independently
 * from full comms trace.
 * Please also see defect 532030 raised against the development stream, which
 * will supersede this basic tracing (using the same trace group) in the future.
 * 
 * Callers should check TraceComponent.isAnyTracingEnabled() before calling
 * into this class.
 */
public class CommsLightTrace
{
   /** The string used for null messages */
   private static final String STRING_NULL = "null";
  
   /** Register Class with Trace Component */
   private static final TraceComponent light_tc = SibTr.register(CommsLightTrace.class, 
                                                                 com.ibm.ws.sib.utils.TraceGroups.TRGRP_MESSAGETRACECOMMS, 
                                                                 CommsConstants.MSG_BUNDLE);
   
   /**
    * Allow minimal tracing of System Message IDs (along with a transaction if on exists)
    * @param callersTrace The trace component of the caller
    * @param action A string (no spaces) provided by the caller to prefix the keyword included in the trace line
    * @param message The message to trace
    */
   public static void traceMessageId(TraceComponent callersTrace, String action, SIBusMessage message)
   {
     if (light_tc.isDebugEnabled() || callersTrace.isDebugEnabled()) {
       _traceMessageId(callersTrace, action, msgToString(message));
     }
   }

   /**
    * Allow minimal tracing of System Message IDs (along with a transaction if on exists)
    * @param callersTrace The trace component of the caller
    * @param action A string (no spaces) provided by the caller to prefix the keyword included in the trace line
    * @param message The message to trace
    */
   public static void traceMessageId(TraceComponent callersTrace, String action, AbstractMessage message)
   {
     if (light_tc.isDebugEnabled() || callersTrace.isDebugEnabled()) {
       _traceMessageId(callersTrace, action, msgToString(message));
     }
   }

   /**
    * Allow minimal tracing of System Message IDs (along with a transaction if on exists)
    * @param callersTrace The trace component of the caller
    * @param action A string (no spaces) provided by the caller to prefix the keyword included in the trace line
    * @param message The message to trace
    */
   private static void _traceMessageId(TraceComponent callersTrace, String action, String messageText)
   {
     // Build the trace string
     String traceText = action + ": " + messageText;

     // Trace using the correct trace group
     if (light_tc.isDebugEnabled()) {
       SibTr.debug(light_tc, traceText);
     }
     else {
       SibTr.debug(callersTrace, traceText);
     }
   }

   /**
    * Allow tracing of messages and transaction when deleting
    * @param callersTrace The trace component of the caller
    * @param action A string (no spaces) provided by the caller to prefix the keyword included in the trace line
    * @param messageHandles The messages handles to trace
    */
   public static void traceMessageIds(TraceComponent callersTrace, String action, SIMessageHandle[] messageHandles)
   {
     if ((light_tc.isDebugEnabled() || callersTrace.isDebugEnabled())
         && messageHandles != null) {

       // Build our trace string
       StringBuffer trcBuffer = new StringBuffer();
       trcBuffer.append(action + ": [");
       for (int i = 0; i < messageHandles.length; i++) {
         if (i != 0) trcBuffer.append(",");
         if (messageHandles[i] != null) {
           trcBuffer.append(messageHandles[i].getSystemMessageId());
         }
       }
       trcBuffer.append("]");

       // Trace using the correct trace group
       if (light_tc.isDebugEnabled()) {
         SibTr.debug(light_tc, trcBuffer.toString());
       }
       else {
         SibTr.debug(callersTrace, trcBuffer.toString());
       }
     }
   }


   /**
    * Trace a transaction associated with an action.
    * @param callersTrace The trace component of the caller
    * @param action A string (no spaces) provided by the caller to prefix the keyword included in the trace line
    * @param transaction An optional transaction associated with the action being performed on the message
    * @param commsId The comms id integer associated (on both sides) with the transaction
    * @param commsFlags In many cases flags are flown over with the transaction, this field can be used for these flags
    */
   public static void traceTransaction(TraceComponent callersTrace, String action, Object transaction, int commsId, int commsFlags)
   {
     if (light_tc.isDebugEnabled() || callersTrace.isDebugEnabled()) {
       // Build the trace string
       String traceText = action + ": " + transaction + 
         " CommsId:" + commsId + " CommsFlags:" + commsFlags;

       // Trace using the correct trace group
       if (light_tc.isDebugEnabled()) {
         SibTr.debug(light_tc, traceText);
       }
       else {
         SibTr.debug(callersTrace, traceText);
       }
     }
   }

   /**
    * Trace an exception.
    * @param callersTrace The trace component of the caller
    * @param ex The exception
    */
   public static void traceException(TraceComponent callersTrace, Throwable ex)
   {
     if (light_tc.isDebugEnabled() || callersTrace.isDebugEnabled()) {

       // Find XA completion code if one exists, as this isn't in a normal dump
       String xaErrStr = null;
       if (ex instanceof XAException) {
         XAException xaex = (XAException)ex; 
         xaErrStr = "XAExceptionErrorCode: " + xaex.errorCode;
       }

       // Trace using the correct trace group
       if (light_tc.isDebugEnabled()) {
         if (xaErrStr != null) SibTr.debug(light_tc, xaErrStr);
         SibTr.exception(light_tc, ex);
       }
       else {
         if (xaErrStr != null) SibTr.debug(callersTrace, xaErrStr);
         SibTr.exception(callersTrace, ex);
       }
     }
   }

   /**
    * Util to prevent full dump of the conversation in this class, as that
    * will print too much data if trace of this class is turned on to
    * get transaction and msgid information
    */
   public static String minimalToString(Object object)
   {
     return object.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(object));
   }

   /**
    * Util to trace the System Message ID of a message along with the default toString
    */
   public static String msgToString(SIBusMessage message)
   {
     if (message == null) return STRING_NULL;
     return message + "[" + message.getSystemMessageId() + "]";
   }

   /**
    * Util to trace the System Message ID (where available) of a message along with the default toString
    */
   public static String msgToString(AbstractMessage message)
   {
     if (message == null) return STRING_NULL;
     if (message instanceof JsMessage) return message + "[" + ((JsMessage)message).getSystemMessageId() + "]";
     return message.toString();
   }
}
