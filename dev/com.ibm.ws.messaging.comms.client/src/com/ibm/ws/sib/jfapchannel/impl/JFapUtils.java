/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer;
import com.ibm.ws.sib.utils.ras.SibTr;

//@notracescan@
// Don't trace scan this file as it does low level tracing and doesn't need guards
// in this file -- guards should be performed prior to calling methods in this file

/**
 * Misc utilities.  Contains utility methods too jfap specific to go into the
 * sib.utils package.
 */
public class JFapUtils {

  private static final TraceComponent tc = SibTr.register(JFapUtils.class, com.ibm.ws.sib.utils.TraceGroups.TRGRP_JFAPSUMMARY, JFapChannelConstants.MSG_BUNDLE);

   /**
    * Inner class to allow JFAPSUMMARY trace to be enabled via SIBMessageTraceJfap
    */
   private static final class SIBMessageTraceJfap
   {
     private static final TraceComponent tcjfs = SibTr.register(SIBMessageTraceJfap.class, com.ibm.ws.sib.utils.TraceGroups.TRGRP_MESSAGETRACEJFAP, JFapChannelConstants.MSG_BUNDLE);
   }
  
   /**
    * Produces a debug trace entry for a WsByteBuffer.  This should be used
    * as follows:
    * <code>
    * if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) debugTraceWsByteBuffer(...);
    * </code>
    * @param _this   Reference to the object invoking this method.
    * @param _tc     Reference to TraceComponent to use for outputing trace entry.
    * @param buffer  Buffer to trace.
    * @param amount  Maximum amount of data from the buffer to trace.
    * @param comment A comment to associate with the trace entry.
    */
   public static void debugTraceWsByteBuffer(Object _this, TraceComponent _tc, WsByteBuffer buffer, int amount, String comment)
   {
      byte[] data = null;
      int start;
      int count = amount;
      if (count > buffer.remaining()) count = buffer.remaining();

      if (buffer.hasArray())
      {
         data = buffer.array();
         start = buffer.arrayOffset() + buffer.position();;
      }
      else
      {
         data = new byte[count];
         int pos = buffer.position();
         buffer.get(data);
         buffer.position(pos);
         start = 0;
      }

      StringBuffer sb = new StringBuffer(comment);
      sb.append("\nbuffer hashcode:  ");
      sb.append(buffer.hashCode());
      sb.append("\nbuffer position:  ");
      sb.append(buffer.position());
      sb.append("\nbuffer remaining: ");
      sb.append(buffer.remaining());
      sb.append("\n");
      SibTr.debug(_this, _tc, sb.toString());
      if (count > 0)
         SibTr.bytes(_this, _tc, data, start, count, "First "+count+" bytes of buffer data:");
   }

   /**
    * Copies up to 'amount' bytes from 'src' to 'dst'.  Returns the number of bytes
    * actually copied.
    */
   public static int copyWsByteBuffer(WsByteBuffer src, WsByteBuffer dst, int amount)
   {
      int amountCopied = amount;
      int dstRemaining = dst.remaining();
      int srcRemaining = src.remaining();

      if (amountCopied > dstRemaining) amountCopied = dstRemaining;
      if (amountCopied > srcRemaining) amountCopied = srcRemaining;

      if (amountCopied > 0)
      {
         int srcLimit = src.limit();
         src.limit(src.position()+amountCopied);
         dst.put(src);
         src.limit(srcLimit);
      }

      return amountCopied;
   }

   /**
    * Dumps useful info about a WsByteBuffer (stops short of its actual contents)
    * @param _this
    * @param _tc
    * @param buffer
    * @param string
    */
   public static void debugTraceWsByteBufferInfo(Object _this, TraceComponent _tc, WsByteBuffer buffer, String string)
   {
      if (buffer != null)
      {
         StringBuffer sb = new StringBuffer(buffer.getClass().toString());
         sb.append("@");
         sb.append(Integer.toHexString(System.identityHashCode(buffer)));
         sb.append(" pos=");
         sb.append(buffer.position());
         sb.append(" limit=");
         sb.append(buffer.limit());
         sb.append(" - ");
         sb.append(string);
         SibTr.debug(_this, _tc, sb.toString());
      }
      else
      {
         SibTr.debug(_this, _tc, "WsByteBuffer: null - "+string);
      }
   }

   /**
    * Generate a JFAPSUMMARY message in trace.
    * It is a good idea for the caller to check the main trace TraceComponent.isAnyTracingEnabled() before calling this method.
    *
    * <nl>
    *    <li>Searching on JFAPSUMMARY will output all segments sent and received</li>
    *
    *    <li>On a per connection basis. This is done by doing a search of the form "[client dotted ip address:client port number:server dotted ip address:server port number".
    *    This information is displayed the same on the client and server side of the connection and so can be used to match up client and server traces</li>
    *
    *    <li>On a per conversation basis. This is done by doing a search of the form "[client dotted ip address:client port number:server dotted ip address:server port number:conversation id".
    *    This information is displayed the same on the client and server side of the connection and so can be used to match up client and server traces</li>
    *
    *    <li>On a per request basis. This is done by doing a search of the form "[client dotted ip address:client port number:server dotted ip address:server port number:conversation id:request number]".
    *    This information is displayed the same on the client and server side of the connection and so can be used to match up client and server traces. If a message is sent that isn't a reply, the request number is 0.
    *    If there is no request number available, -1 is used as the request number.</li>
    * </nl>
    *
    * @param callersTrace
    * @param connection
    * @param conversation
    * @param remark
    * @param requestNumber
    */
   public static void debugSummaryMessage(TraceComponent callersTrace, Connection connection, ConversationImpl conversation, String remark, int requestNumber)
   {
      if(tc.isDebugEnabled() || callersTrace.isDebugEnabled() || SIBMessageTraceJfap.tcjfs.isDebugEnabled())
      {
         final StringBuffer sb = new StringBuffer("{JFAPSUMMARY}");

         /*
          * Generate String for filtering.
          * This is of the form:
          * [client ip address:client port:server ip address:server port:conversation id:request number]
          */
         sb.append("[");
         if(connection != null)
         {
            sb.append(connection.getEyeCatcher());
         }
         sb.append(":");
         if(conversation != null)
         {
            sb.append(conversation.getId());
         }
         sb.append(":");
         sb.append(requestNumber);
         sb.append("] ");

         if(connection != null)
         {
            sb.append(Integer.toHexString(System.identityHashCode(connection)));
            sb.append("[");
            sb.append(connection.description);
            sb.append(", closeDeferred=");
            sb.append(connection.isCloseDeferred());
            sb.append(", invalidateDeferred=");
            sb.append(connection.isInvalidateDeferred());
            sb.append("]:");
         }
         else
         {
            sb.append("null[]:");
         }

         if(conversation != null)
         {
            sb.append(Integer.toHexString(System.identityHashCode(conversation)));
            sb.append("[");
            sb.append(conversation.description);
            sb.append("] ");
         }
         else
         {
            sb.append("null[] ");
         }

         if(remark != null)
         {
            sb.append(remark);
         }
         else
         {
            sb.append("null");
         }

         if(SIBMessageTraceJfap.tcjfs.isDebugEnabled())
         {
           SibTr.debug(SIBMessageTraceJfap.tcjfs, sb.toString());
         }
         else if(tc.isDebugEnabled())
         {
            SibTr.debug(tc, sb.toString());
         }
         else if(callersTrace.isDebugEnabled())
         {
            SibTr.debug(callersTrace, sb.toString());
         }
       }
   }

   /**
    * Generate a JFAPSUMMARY message in trace.
    * It is a good idea for the caller to check the main trace TraceComponent.isAnyTracingEnabled() before calling this method.
    *
    * The data output by this message can be searched for in several ways:
    *
    * <nl>
    *    <li>Searching on JFAPSUMMARY will output all segments sent and received</li>
    *
    *    <li>On a per connection basis. This is done by doing a search of the form "[client dotted ip address:client port number:server dotted ip address:server port number".
    *    This information is displayed the same on the client and server side of the connection and so can be used to match up client and server traces</li>
    *
    *    <li>On a per conversation basis. This is done by doing a search of the form "[client dotted ip address:client port number:server dotted ip address:server port number:conversation id".
    *    This information is displayed the same on the client and server side of the connection and so can be used to match up client and server traces</li>
    * </nl>
    *
    * @param callersTrace
    * @param connection
    * @param conversation
    * @param remark
    */
   public static void debugSummaryMessage(TraceComponent callersTrace, Connection connection, ConversationImpl conversation, String remark)
   {
      //Use a request number of -1 to distinguish from valid request numbers which are non-negative.
      debugSummaryMessage(callersTrace, connection, conversation, remark, -1);
   }
}
