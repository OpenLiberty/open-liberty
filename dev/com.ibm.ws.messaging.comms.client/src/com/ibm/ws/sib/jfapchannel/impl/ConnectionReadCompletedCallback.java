/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.jfapchannel.AcceptListener;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.JFapHeartbeatTimeoutException;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBufferPool;
import com.ibm.ws.sib.jfapchannel.framework.IOConnectionContext;
import com.ibm.ws.sib.jfapchannel.framework.IOReadCompletedCallback;
import com.ibm.ws.sib.jfapchannel.framework.IOReadRequestContext;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnection;
import com.ibm.ws.sib.utils.RuntimeInfo;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;

/**
 * Callback used to notify a connection that a read operation has completed.
 * Each connection should have exactly one of these callbacks.
 * @author prestona
 */
public class ConnectionReadCompletedCallback implements IOReadCompletedCallback
{
   private static final TraceComponent tc = SibTr.register(ConnectionReadCompletedCallback.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);
	
   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.common.impl/src/com/ibm/ws/sib/jfapchannel/impl/ConnectionReadCompletedCallback.java, SIB.comms, WASX.SIB, uu1215.01 1.58");
   }
	
   private static class SynchronizedFlag
   {
      private boolean flag;
      protected synchronized void set() {flag = true;}
      protected synchronized void clear() {flag = false;}
      protected boolean isSet() {return flag;}
   }

   // Connection this callback belongs to.	
   private Connection thisConnection = null;
	
   private ConversationImpl conversation;

//   private TCPConnectionContext tcpCtx;                                 // F184828
   private IOConnectionContext tcpCtx;
//   private TCPReadRequestContext readCtx;                               // F184828
   private IOReadRequestContext readCtx;

   // The packet number we are expecting on the next transmission.
//   private byte expectedPacketNumber = (short) 0;                       // F173152

   // Have we been sent a physical close request by our peer.
   private volatile boolean receivePhysicalCloseRequest = false;        // F174722

   // Are we currently awaiting a response to a heartbeat?
   private final SynchronizedFlag awaitingHeartbeatResponse = new SynchronizedFlag();

   // Saves the value of thisConnection.getHeartbeatTimeout() at the
   // point where we issue a heartbeat to our peer.  Because the value
   // returned from this method can change at any time, we must remember
   // it to avoid badness if we have to recalculate the amount of time
   // remaining for the heartbeat response.
   private int currentHeartbeatTimeout;                                 // F175658

   // Set when the connection we are reading from is closing and we should
   // not issue any further read requests.  Anyone testing or setting this
   // flag should synchronise on the connectionClosingLock object to
   // prevent problems with concurrency.
   private boolean connectionClosing = false;                           // F183461

   // Locking object, used to ensure the integrity of the connectionClosing
   // flag.
   private Object connectionClosingLock = new Object();                 // F183461

   // Parser for inbound transmissions.
   private InboundTransmissionParser xmitParser;                        // F181603.2

   // Is this the first ever invocation of this classes complete method?
   private boolean isFirstCompleteInvocation = true;                    // F181603.2

   // This object is synchronized on when updating the invocation count
   private Object invocationCountLock = new Object();

   // The number of times we have been invoked with complete() on this thread
   private int invocationCount = 0;

   // The last thread complete was invoked on
   private Thread lastInvokedOnThread = null;

   // The number of times we will allow complete() to be called recursively before thread switching
   private static final int MAX_INVOCATIONS_BEFORE_THREAD_SWITCH = 10;

   /**
    * Create a new read completed callback.
    * @param c
    * @param onClient
    * @param al
    * @param ct
    * @param conv
    */	
   protected ConnectionReadCompletedCallback(Connection c,
                                             boolean onClient,
                                             AcceptListener al,
                                             ConversationTable ct,
                                             ConversationImpl conv,
                                             IOConnectionContext x) // F184828
   throws SIResourceException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", new Object[] {c, ""+onClient, al, ct, conv, x});
      thisConnection = c;
//      onClientSide = onClient;
//      acceptListener = al;
//      conversationTable = ct;
      conversation = conv;
      tcpCtx = x;
      readCtx = x.getReadInterface();

      xmitParser = new InboundTransmissionParser(c, al, onClient);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }

   /**
    * Called by the next channel in the chain when an outstanding read request has completed
    * successfully.
    *
    * @see com.ibm.wsspi.tcp.channel.TCPReadCompletedCallback#complete(com.ibm.wsspi.channel.framework.VirtualConnection, com.ibm.wsspi.tcp.channel.TCPReadRequestContext)
    */
   public void complete(NetworkConnection vc, IOReadRequestContext rctx)
   {	
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "complete", new Object[] {vc, rctx});
      if (thisConnection.isLoggingIOEvents()) thisConnection.getConnectionEventRecorder().logDebug("complete method invoked on read context "+System.identityHashCode(rctx));

      // First update the invocation count. If we are being called back on the same thread as
      // last time, increment the counter. Otherwise, start counting from 1 again.
      synchronized (invocationCountLock)
      {
         if (lastInvokedOnThread == Thread.currentThread())
         {
            invocationCount++;
         }
         else
         {
            invocationCount = 1;
            lastInvokedOnThread = Thread.currentThread();
         }
      }

      try
      {
         synchronized(this)
         {
            boolean done = false;
            do
            {
               done = true;
               WsByteBuffer contextBuffer = rctx.getBuffer();

               contextBuffer.flip();

               // Notify PMI that read has completed.
               if (conversation.getConversationType() == Conversation.CLIENT)
               {
                  xmitParser.setType(Conversation.CLIENT);
               }
               else if (conversation.getConversationType() == Conversation.ME)
               {
                  xmitParser.setType(Conversation.ME);
               }

               if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JFapUtils.debugTraceWsByteBuffer(this, tc, contextBuffer, 16, "data received");

               xmitParser.parse(contextBuffer);

         		if ((rctx != null) && (!receivePhysicalCloseRequest))
         		{
                  // Calculate the amount of time before the request times out
                  // This is the mechanism by which we implement heartbeat
                  // intervals and time outs.
                  int timeout;
                  if (awaitingHeartbeatResponse.isSet())
                  {
                     // We can only reach this point in the code if we have
                     // made a heartbeat request but this callback was driven
                     // because of a non-heartbeat response.  In previous versions
                     // of the code we were careful to calculate the remaining time and
                     // make another read request.  This caused timeouts ala defect
                     // 363463.  So, now the code is more generous and resets its
                     // timer to the full heartbeat timeout value every time any
                     // non-heartbeat trasmission is received from our peer.  This isn't
                     // unreasonable as the fact we are receiving data indicates that the
                     // peer is probably still healthy.
                     timeout = currentHeartbeatTimeout*1000;
                     if (timeout < 1) timeout = 1;
                  }
                  else
                  {
                     // We are not awaiting a heartbeat response, use the
                     // heartbeat interval as our timeout.
                     timeout = thisConnection.getHeartbeatInterval()*1000;
                  }

                  if (timeout > 0)
                  {
                     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "setting heartbeat timeout to: "+timeout+" milliseconds");
                  }
                  else
                  {
                     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "not using a heartbeat timeout");
                     timeout = IOReadRequestContext.NO_TIMEOUT;
                  }

                  boolean closing = false;
                  synchronized(connectionClosingLock)
                  {
                     closing = connectionClosing;
                  }

                  //If the connection is closing/closed our buffers will have been released, so make sure we don't use them again.
                  if (!closing)
                  {
                     // Crude way to ensure we end up with the right sized read buffer.
                     if (isFirstCompleteInvocation)
                     {
                        int readBufferSize =
                           Integer.parseInt(RuntimeInfo.getProperty("com.ibm.ws.sib.jfapchannel.DEFAULT_READ_BUFFER_SIZE", ""+JFapChannelConstants.DEFAULT_READ_BUFFER_SIZE));
                        if (!contextBuffer.isDirect() || (contextBuffer.capacity() < readBufferSize))
                        {
                           //Make sure we release the other buffer so we don't leak memory.
                           contextBuffer.release();

                           contextBuffer = WsByteBufferPool.getInstance().allocateDirect(readBufferSize);
                           rctx.setBuffer(contextBuffer);
                        }
                        isFirstCompleteInvocation = false;
                     }

                     contextBuffer.clear();

                     // Decide whether to explictly request a thread switch. We'll do this if we
                     // have been recursively called more than MAX_INVOCATIONS_BEFORE_THREAD_SWITCH
                     boolean forceQueue = false;
                     synchronized (invocationCountLock)
                     {
                        if (invocationCount > MAX_INVOCATIONS_BEFORE_THREAD_SWITCH)
                        {
                           forceQueue = true;
                           lastInvokedOnThread = null;
                        }
                     }

                     if (thisConnection.isLoggingIOEvents()) thisConnection.getConnectionEventRecorder().logDebug("invoking readCtx.read() on context "+System.identityHashCode(rctx)+" with a timeout of "+timeout);
                     done = (rctx.read(1, this, forceQueue, timeout) == null);
                   
                  }
         		}
            }
            while(!done);
         }
      }
      catch(Error error)
      {
         FFDCFilter.processException
         (error, "com.ibm.ws.sib.jfapchannel.impl.ConnectionReadCompletedCallback", JFapChannelConstants.CONNREADCOMPCALLBACK_COMPLETE_01, thisConnection.getDiagnostics(true));
         if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, error);

         // It might appear slightly odd for this code to catch Error (especially since the JDK docs say
         // that Error means that something has gone so badly wrong that you should abandon all hope).
         // This code makes one final stab at putting out some diagnostics about what happened (if we
         // propagate the Error up to the TCP Channel, it is sometimes lost) and closing down the
         // connection.  I figured that we might as well try to do something - as we can hardly make
         // things worse... (famous last words)

         thisConnection.invalidate(false, error, "Error caught in ConnectionReadCompletedCallback.complete()");

         // Re-throw the error to ensure that it causes the maximum devastation.
         // The JVM is probably very ill if an Error is thrown so attempt no recovery.
         throw error;
      }
      catch(RuntimeException runtimeException)
      {
         FFDCFilter.processException
         (runtimeException, "com.ibm.ws.sib.jfapchannel.impl.ConnectionReadCompletedCallback", JFapChannelConstants.CONNREADCOMPCALLBACK_COMPLETE_02, thisConnection.getDiagnostics(true));
         if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, runtimeException);

         // We can reasonably try to recover from a runtime exception by invalidating the associated
         // connection.  This should drive the underlying TCP/IP socket to be closed.
         thisConnection.invalidate(false, runtimeException, "RuntimeException caught in ConnectionReadCompletedCallback.complete");

         // Don't throw the RuntimeException on as we risk blowing away part of the TCP channel.
      }

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "complete");
	}


   /**
    * Part of the read completed callback interface.  Notified when an error
    * occurres during a read operation.
    */
   public void error(NetworkConnection vc,
                     IOReadRequestContext rrc,
                     IOException t)               // F176003, F184828, D194678
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "error", new Object[] {vc, rrc, t});  // F184828
      if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled() && (t != null)) SibTr.exception(this, tc, t);
      if (thisConnection.isLoggingIOEvents()) thisConnection.getConnectionEventRecorder().logDebug("error method invoked on read context "+System.identityHashCode(rrc)+" with exception "+t);

      try
      {
         synchronized(this)
         {
            // begin F174772
            if (receivePhysicalCloseRequest)
            {
               // Ignore any errors when we are physically closing they could be
               // because of a race condidition between write operations and the
               // close of the socket.
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "ignoring as in process of close");
            }
            // end F174772
            // begin F175658
            else if (t instanceof SocketTimeoutException)
            {
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "error is as a result of a timeout");

               // We should only enter this arm of the if statement
               // if we have timed out on a previous request.  This
               // must be heartbeat realted.
               if (awaitingHeartbeatResponse.isSet())
               {
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "timed out waiting for heartbeat response");  // F177053
                  thisConnection.getConnectionEventRecorder().logDebug("timed out waiting for heartbeat response");

                  // At this point issue a message to the console
                  String remoteHostAddress = "<Unknown>";
                  String remoteHostPort = "<Unknown>";
                  String chainName = thisConnection.chainName;

                  if (tcpCtx != null)
                  {
                     InetAddress addr = tcpCtx.getRemoteAddress();
                     if (addr != null)
                     {
                        remoteHostAddress = addr.getHostAddress();
                     }
                     remoteHostPort = ""+tcpCtx.getRemotePort();
                  }

                  if (thisConnection.isInbound())
                  {
                     if (conversation.getConversationType() == Conversation.ME)
                     {
                        SibTr.error(tc, SibTr.Suppressor.ALL_FOR_A_WHILE_SIMILAR_INSERTS,
                                    "ME_NOT_RESPONDING_SICJ0041",
                                    new Object[] { remoteHostAddress, chainName, ""+currentHeartbeatTimeout });
                     }
                     else if (conversation.getConversationType() == Conversation.CLIENT)
                     {
                        SibTr.error(tc, SibTr.Suppressor.ALL_FOR_A_WHILE_SIMILAR_INSERTS,
                                    "CLIENT_NOT_RESPONDING_SICJ0042",
                                    new Object[] { remoteHostAddress, chainName, ""+currentHeartbeatTimeout });
                     }
                  }
                  else  // we are Outbound
                  {
                     if (conversation.getConversationType() == Conversation.ME)
                     {
                        SibTr.error(tc, SibTr.Suppressor.ALL_FOR_A_WHILE_SIMILAR_INSERTS,
                                    "ME_NOT_RESPONDING_OUTBOUND_SICJ0070",
                                    new Object[] { remoteHostAddress, remoteHostPort, chainName, ""+currentHeartbeatTimeout });
                     }
                     else if (conversation.getConversationType() == Conversation.CLIENT)
                     {
                        SibTr.error(tc, SibTr.Suppressor.ALL_FOR_A_WHILE_SIMILAR_INSERTS,
                                    "CLIENT_NOT_RESPONDING_OUTBOUND_SICJ0071",
                                    new Object[] { remoteHostAddress, remoteHostPort, chainName, ""+currentHeartbeatTimeout });
                     }
                  }

                  // Close the underlying connection.  This takes out all
                  // the conversations it shares, wakes up exchanges and
                  // delivers exceptions to the conversation receive listeners.
                  // begin F176003
                  JFapHeartbeatTimeoutException exception =
                     new JFapHeartbeatTimeoutException("Connection dropped after heartbeat request went unacknowledged");
                  exception.initCause(t);
                  FFDCFilter.processException
                  (exception, "com.ibm.ws.sib.jfapchannel.impl.ConnectionReadCompletedCallback", JFapChannelConstants.CONNREADCOMPCALLBACK_ERROR_03, thisConnection.getDiagnostics(true));
                  thisConnection.invalidate(false, exception, "heartbeat request was not acknowledged");

                  // end F176003

                  
               }
               // being F177053
               else if (thisConnection.getHeartbeatInterval() == 0)
               {
                  // Oh dear.  This is a bit of a irratating situation we find
                  // ourselves in.  It looks like someone has turned off heartbeating
                  // yet - whilst it was switched on we asked to be notified of a
                  // timeout.  We should probably just ignore this.
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "timed out but heartbeating now switched off");

                  // begin D183461
                  NetworkConnection rvc = null;
                  synchronized(connectionClosingLock)
                  {
                     if (!connectionClosing)
                     {
                        if (thisConnection.isLoggingIOEvents()) thisConnection.getConnectionEventRecorder().logDebug("invoking readCtx.read() on context "+System.identityHashCode(readCtx)+" with no timeout");
                        rvc = readCtx.read(1, this, false, IOReadRequestContext.NO_TIMEOUT);      // F184828
                     }
                  }
                  // end D183461
                  if (rvc != null)
                     complete(rvc, readCtx);
                 
               }
               // end F177053
               else
               {
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "sending heartbeat request");  // F177053, D224570

                  // We timed out and we weren't awaiting a heartbeat
                  // response.  This must mean that we timedout on the
                  // heartbeat interval.  Issue a heartbeat request to
                  // our peer.

                  // Remember state about the fact that we are about to
                  // issue a heartbeat request.
                  awaitingHeartbeatResponse.set();
                  currentHeartbeatTimeout = thisConnection.getHeartbeatTimeout();

                  thisConnection.getConnectionEventRecorder().logDebug("sending heartbeat request and waiting up to "+currentHeartbeatTimeout+" seconds for a response");

                  // Send a heartbeat request to our peer.
                  // begin D221868
                  SIException sendException = null;
                  try
                  {
                     thisConnection.sendHeartbeat();
                  }
                  catch(SIConnectionLostException e)
                  {
                     // No FFDC code needed
                     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Exception received when heartbeating peer ("+e.toString()+").  Dropping connection");
                     sendException = e;
                  }
                  catch(SIConnectionDroppedException e)
                  {
                     // No FFDC code needed
                     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Exception received when heartbeating peer ("+e.toString()+").  Dropping connection");
                     sendException = e;
                  }

                  if (sendException == null)
                  {
                     // Resume our outstanding read request - but this time,
                     // set the timeout to our heartbeat timeout time.  This is
                     // because we are waiting for the response.
                     int timeout = currentHeartbeatTimeout*1000;
                     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "setting heartbeat timeout to: "+timeout+" milliseconds");   // F177053

                     // begin F177053, D183461
                     NetworkConnection rvc = null;
                     synchronized(connectionClosingLock)
                     {
                        if (!connectionClosing)
                        {
                           if (thisConnection.isLoggingIOEvents()) thisConnection.getConnectionEventRecorder().logDebug("invoking readCtx.read() on context "+System.identityHashCode(readCtx)+" with a timeout of "+timeout);
                           rvc = readCtx.read(1, this, false, timeout);    // F184828
                        }
                     }
                     if (rvc != null)
                        complete(rvc, readCtx);
                     // end F177053, D183461
                  }
                  else
                  {
                     // We failed to send a heartbeat request to our peer.
                     thisConnection.invalidate(false, sendException, "exception caught while attempting to send heartbeat");  // D224570

                  
                  }
                  // end D221868
               }
            }
            // end F175658
            else                                                                    // F174772
            {                                                                       // F174772
               // begin F176003
               // We used to set the first parameter to true here meaning that we
               // need to notify the peer that we are having a bit of trouble.
               // However, I believe that to be incorrect as I think that if this
               // method is called then the socket is already dead - hence trying
               // to notify the peer would mean that we would hang.
               thisConnection.invalidate(false, t, "IOException received for connection - "+t.getMessage());   // D179618, D224570
               // end F176003
             
               //Note that this also deals with the buffer returned by getBuffer.
         		final IOReadRequestContext req = readCtx;
               final WsByteBuffer[] buffers = req.getBuffers();
         		if (buffers != null)
         		{
         		   for(final WsByteBuffer buffer: buffers)
                  {
                     //Try and release the buffer.
                     //Absorb any exceptions if it gets released by another thread (for example by Connection.nonThreadSafePhysicalClose).
                     try
                     {
                        buffer.release();
                     }
                     catch(RuntimeException e)
                     {
                        //No FFDC code needed
                        if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Caught exception on releasing buffer.", e);
                     }
                  }
                  
                  req.setBuffers(null);
         		}
         		else
         		{
         			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Request has no buffers: "+req);
         		}
            }
         }
      }
      catch(Error error)
      {
         FFDCFilter.processException
         (error, "com.ibm.ws.sib.jfapchannel.impl.ConnectionReadCompletedCallback", JFapChannelConstants.CONNREADCOMPCALLBACK_ERROR_05, thisConnection.getDiagnostics(true));
         if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, error);

         // It might appear slightly odd for this code to catch Error (especially since the JDK docs say
         // that Error means that something has gone so badly wrong that you should abandon all hope).
         // This code makes one final stab at putting out some diagnostics about what happened (if we
         // propagate the Error up to the TCP Channel, it is sometimes lost) and closing down the
         // connection.  I figured that we might as well try to do something - as we can hardly make
         // things worse... (famous last words)

         thisConnection.invalidate(false, error, "Error caught in ConnectionReadCompletedCallback.error()");

         // Re-throw the error to ensure that it causes the maximum devastation.
         // The JVM is probably very ill if an Error is thrown so attempt no recovery.
         throw error;
      }
      catch(RuntimeException runtimeException)
      {
         FFDCFilter.processException
         (runtimeException, "com.ibm.ws.sib.jfapchannel.impl.ConnectionReadCompletedCallback", JFapChannelConstants.CONNREADCOMPCALLBACK_ERROR_06, thisConnection.getDiagnostics(true));
         if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, runtimeException);

         // We can reasonably try to recover from a runtime exception by invalidating the associated
         // connection.  This should drive the underlying TCP/IP socket to be closed.
         thisConnection.invalidate(false, runtimeException, "RuntimeException caught in ConnectionReadCompletedCallback.error()");

         // Don't throw the RuntimeException on as we risk blowing away part of the TCP channel.
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "error");
   }

   // begin D183461
   /**
    * Notified by the connection just before a physical close occurres.  This notification is
    * used to ensure that we do not schedule any further I/O requests.  Care is taken to
    * synchronize correctly as this classes close method may be being executed on a different
    * thread from that calling physical close (or on the same thread...).
    */
   protected void physicalCloseNotification()
   {
      synchronized(connectionClosingLock)
      {
         connectionClosing = true;
      }
   }
   // end D183461

   // begin F181603.2
   protected void heartbeatReceived()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "heartbeatReceived");
      synchronized(awaitingHeartbeatResponse)
      {
         if (awaitingHeartbeatResponse.isSet())
         {
            awaitingHeartbeatResponse.clear();
            thisConnection.getConnectionEventRecorder().logDebug("received heartbeat response");
         }
         else
         {
          // Well, that's odd, it doesn't look like we asked for a
          // heartbeat.  Better log this...
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Spurious heartbeat!");
         }
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "heartbeatReceived");
   }
   // end F181603.2

   // begin F181603.2
   protected void stopReceiving()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "stopReceiving");
      receivePhysicalCloseRequest = true;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "stopReceiving", ""+receivePhysicalCloseRequest);
   }
   // end F181603.2
}
