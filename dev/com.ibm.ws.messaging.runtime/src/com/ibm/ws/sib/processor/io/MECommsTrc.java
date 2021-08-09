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
package com.ibm.ws.sib.processor.io;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.mfp.AbstractMessage;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Class to allow minimal tracing of control messages flowing to/from an ME  
 */
public class MECommsTrc {

  /** Sending of a message of a ME<->ME comms connection */
  public static final String OP_SEND = "--SEND->";
  
  /** Receiving of a message of a ME<->ME comms connection */
  public static final String OP_RECV = "<-RECV--";

  /** Details of a message after it has been fluffed up */
  public static final String OP_SUBMSG_CONSUME = "<-SUBC--";

  private static final TraceComponent tc =
    SibTr.register(
      MECommsTrc.class,
      SIMPConstants.MP_MECOMMS_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
  
  /**
   * Minimal trace entry for ME<->ME comms.
   * Caller should check TraceComponent.isAnyTracingEnabled() before calling.
   * @param _tc The caller's trace component
   * @param operation The operation. See constants in this class.
   * @param targetME The MEUUID of the target ME
   * @param connection The ME<->ME connection (hashCode will be printed). Can be null to indicate no suitable connection was found.
   * @param msg The mess1age being sent over the connection
   */
  public static void traceMessage(TraceComponent _tc, MessageProcessor localME, SIBUuid8 remoteME, String operation, Object connection, AbstractMessage msg) {
    
    // Trace if either the callers trace is enabled, or our own trace group
    if (tc.isDebugEnabled() || _tc.isDebugEnabled()) {

      StringBuilder buff = new StringBuilder();
      buff.append("MECOMMS");

      try {
  
        // Append operation details
        buff.append("[");
        buff.append((localME!=null)?localME.getMessagingEngineName():"???");
        buff.append(operation);
        buff.append(remoteME);
        buff.append("] ");
  
        // Append the details of the message
        try {
          msg.getTraceSummaryLine(buff);      
        } catch (Exception e) {
          // No FFDC code needed
          // We failed to get the message summary line, just print the message class instead.
          // We know this can happen within unit tests, because the MFP getter methods used
          // to build the summary line can throw a NullPointerException if the message is not
          // fully initialized. However, no circumstances are known for this to happen in
          // a 'real' messaging engine.
          String safeMsgSummary = (msg != null)?msg.getClass().getName():"null";
          buff.append(" SUMMARY TRACE FAILED. Message class=" + safeMsgSummary);
        }
        
        // Add details identifying the link/stream/destination at the end of the line.
        // As this is on every line, I've tried to limit it as much as possible
        buff.append(" [C=");
        buff.append((connection!=null)?Integer.toHexString(connection.hashCode()):null);
        buff.append(",D=");
        buff.append(msg.getGuaranteedTargetDestinationDefinitionUUID());
        buff.append(",S=");
        buff.append(msg.getGuaranteedStreamUUID());
        buff.append("]");
        
      } catch (Exception e) {
        // No FFDC code needed
        // We encountered an unexpected runtime exception. Try to print a bit of helpful info.
        String safeMsgSummary = (msg != null)?msg.getClass().getName():"null";
        buff.append(" SUMMARY TRACE FAILED. Message class=" + safeMsgSummary);
      }
      
      // Preferentially trace with our trace group, to allow a grep on this class name
      if (tc.isDebugEnabled()) {
        SibTr.debug(tc, buff.toString());
      }
      else {
        SibTr.debug(_tc, buff.toString());
      }

    }
    
  }
  
}
