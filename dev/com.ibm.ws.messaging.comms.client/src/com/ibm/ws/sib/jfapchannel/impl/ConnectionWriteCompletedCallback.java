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
import java.util.ArrayList;
import java.util.List;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.SendListener;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBufferPool;
import com.ibm.ws.sib.jfapchannel.framework.IOWriteCompletedCallback;
import com.ibm.ws.sib.jfapchannel.framework.IOWriteRequestContext;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnection;
import com.ibm.ws.sib.utils.RuntimeInfo;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;

/**
 * Callback notified when a write operation completes.  These callbacks are
 * owned and registered once per connection.
 * @author prestona
 */
public class ConnectionWriteCompletedCallback implements IOWriteCompletedCallback
{
   private static final TraceComponent tc = SibTr.register(ConnectionWriteCompletedCallback.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);
   
   static   
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.common.impl/src/com/ibm/ws/sib/jfapchannel/impl/ConnectionWriteCompletedCallback.java, SIB.comms, WASX.SIB, uu1215.01 1.46");
   }
		
   // Structure used to prioritse senders.
   private PriorityQueue priorityQueue = null;
   
//   private TCPWriteRequestContext writeCtx;                                         // F184828
   private IOWriteRequestContext writeCtx; 

   // The connection that this callback is associated with.
   private Connection connection;                                                   // F176003

   // The Thread which is processing the TCP write request,                         // PI92182
   // or null if no thread is currently processing the write request.               // PI92182
   private Thread writingThread = null;                                             // PI92182
	
   private boolean terminate = false;                                               // F176003

   // Used to determine if the connection this callback is registered for is in the
   // process of closing.  Since closes can take place asynchronously from the
   // callback being invoked - before examining or setting this flag - synchronize
   // on the connectionCLosedLock object.
   private boolean connectionClosed = false;                                        // D183461
   
   // Lock object used to prevent concurrent use of connectionClosedFlag.
   private Object connectionClosedLock = new Object();                              // D183461

   // List of send listeners and conversations that have been sent but that our 
   // connection complete callback has not been invoked for.
   private List<SendListener> inflightSendListeners = new ArrayList<SendListener>();
   private List<ConversationImpl> inflightConversations = new ArrayList<ConversationImpl>();
   
   // Stores any partially sent (ie. didn't fully fit into our transmission buffer)
   // transmission.
   private TransmissionData partiallySentTransmission = null;                            // F181603.2

   // Is this the first time this callback has been invoked to do work.  Used for
   // one off buffer intialisation.
   private boolean firstInvocation = true;                                          // F181603.2

	/**
    * Creates a new callback which will use the specified queue and send lock.
    * 
    * @param priorityQueue
    *           Used to prioritise sends.
    * @param x
    *           The TCP write context to associate with this callback.
    * @param connection
    *           The connection to associate with this callback.
    */
   public ConnectionWriteCompletedCallback(PriorityQueue priorityQueue, IOWriteRequestContext x, // F184828
            Connection connection) // F176003
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", new Object[] { priorityQueue, x, connection });
      this.priorityQueue = priorityQueue;
      writeCtx = x;
      this.connection = connection; // F176003
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }
   
   /**
    * Encorages the JFAP Channel to write data. If the send callback is not currently busy
    * transmitting data, proddling it will cause it to start. If it is already sending, proddling is
    * harmless and doesn't irritate it.
    */
   // being F176003, F181603.2, D192359    
   protected void proddle() throws SIConnectionDroppedException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "proddle");
      
      boolean writeOnThisThread = false;
      
      // begin D226210
      synchronized(priorityQueue)
      {
         synchronized(this)
         {
           Thread currentThread = Thread.currentThread();                          // PI92182
            if ((writingThread == null || currentThread.equals(writingThread)) && !terminate) {    // PI92182                                         
                if (isWorkAvailable()) {                                            // PI92182
                    writingThread = Thread.currentThread();                         // PI92182
                    writeOnThisThread = true;                                       // PI92182
                } else {                                                            // PI92182
                    writingThread = null;                                           // PI92182
                }
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "writingThread="+writingThread);
         }
      } 
      // end D226210
      
      if (writeOnThisThread)
      {
         WsByteBuffer writeBuffer = writeCtx.getBuffer();
         
         // Crude way to ensure that the buffer associatied with this context is of the
         // expected size and is a direct buffer.
         if (firstInvocation || (writeBuffer == null))
         {
            firstInvocation = false;
            int writeBufferSize =
               Integer.parseInt(RuntimeInfo.getProperty("com.ibm.ws.sib.jfapchannel.DEFAULT_WRITE_BUFFER_SIZE", ""+JFapChannelConstants.DEFAULT_WRITE_BUFFER_SIZE));
            
            if (writeBuffer == null || (!writeBuffer.isDirect()) || (writeBuffer.capacity() < writeBufferSize))
            {
               //Make sure we release the other buffer so we don't leak memory.
               if(writeBuffer != null) writeBuffer.release();
               
               writeBuffer = WsByteBufferPool.getInstance().allocateDirect(writeBufferSize);      // F196678.10
               writeCtx.setBuffer(writeBuffer);   
            }               
         }

         writeBuffer.clear();
         
         if (dequeueTransmissionData(writeBuffer))
         {
            writeBuffer.flip();
            synchronized (this) {                                                   // PI92182
                // Another thread may have cleared writingThread, so claim it anyway.// PI92182 
                writingThread = Thread.currentThread();                             // PI92182
            }                                                                       // PI92182

            NetworkConnection vc = null;
                        
            synchronized(connectionClosedLock)
            {
               if (!connectionClosed)
               {
                  if (connection.isLoggingIOEvents()) connection.getConnectionEventRecorder().logDebug("invoking writeCtx.write() on context "+System.identityHashCode(writeCtx)+" to write all data with no timeout");
                  vc = writeCtx.write(IOWriteRequestContext.WRITE_ALL_DATA, this, false, IOWriteRequestContext.NO_TIMEOUT);
               }
            }
            if (vc != null)
               complete(vc, writeCtx);               
      
         }
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "proddle");
   }   
   // end F176003, F181603.2, D192359
  			
	/**
	 * Part of the write completed callback interface.  Notified when a write operation
	 * has completed.  This code sees if there is any more data to write and setups
	 * the next write call as appropriate. 
	 */
   // begin F181603.2, D192359
	public void complete(NetworkConnection vc, IOWriteRequestContext wctx) // F176003, F177053, F184828
	{
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "complete", new Object[] {vc, wctx});
      if (connection.isLoggingIOEvents()) connection.getConnectionEventRecorder().logDebug("complete method invoked on write context "+System.identityHashCode(wctx));
      
      try
      {
         boolean done = true;
         do
         {
            done = true;
                     
            // Notify people as appropriate
            // begin D217401
            boolean error = false;
            int index = 0;
            int sendListeners = inflightSendListeners.size();
            while((index < sendListeners) && !error)
            {
               SendListener sendListener = inflightSendListeners.get(index);
               if (sendListener != null)
               {
                  try
                  {                   
                     sendListener.dataSent(inflightConversations.get(index));
                  }
                  catch(Throwable t)
                  {
                     FFDCFilter.processException
                        (t, "com.ibm.ws.sib.jfapchannel.impl.ConnectionWriteCompletedCallback", JFapChannelConstants.CONNWRITECOMPCALLBACK_COMPLETE_01, connection.getDiagnostics(true));
                     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "exception invoking send listener data sent");
                     if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(tc, t);
                     connection.invalidate(true, t, "send listener threw exception");  // D224570
                     error = true;               
                  }
               }
               ++index;
            }
            inflightSendListeners.clear();
            inflightConversations.clear();
            // end D217401
                     
            if (error)
            {
               done = true;
               partiallySentTransmission = null;
            }
            else
            {
               done = false;
               WsByteBuffer writeBuffer = null;
               
               // begin D226210
               synchronized(priorityQueue)
               {
                  synchronized(this)
                  {
                     if (terminate)
                     {
                        writingThread = null;                                       // PI92182
                     }
                     else
                     {
                        writeBuffer = writeCtx.getBuffer();
                        
                        // Crude way to ensure that the buffer associatied with this context is of the
                        // expected size and is a direct buffer.
                        if (firstInvocation || (writeBuffer == null))
                        {
                           firstInvocation = false;
                           int writeBufferSize =
                              Integer.parseInt(RuntimeInfo.getProperty("com.ibm.ws.sib.jfapchannel.DEFAULT_WRITE_BUFFER_SIZE", ""+JFapChannelConstants.DEFAULT_WRITE_BUFFER_SIZE));
                     
                           if (writeBuffer == null || (!writeBuffer.isDirect()) || (writeBuffer.capacity() < writeBufferSize))
                           {
                              //Make sure we release the other buffer so we don't leak memory.
                              if(writeBuffer != null) writeBuffer.release();
                              
                              writeBuffer = WsByteBufferPool.getInstance().allocateDirect(writeBufferSize);      // F196678.10
                              writeCtx.setBuffer(writeBuffer);   
                           }
                        }
                        
                        if (!isWorkAvailable())                                     // PI92182
                            writingThread = null;                                   // PI92182
                     }
                     done |= (writingThread == null);                               // PI92182
                  }
               }
               // end D226210
                           
               if (!done)
               {
                  writeBuffer.clear();
                  if (dequeueTransmissionData(writeBuffer))
                  {
                     writeBuffer.flip();
                     vc = null;
                     synchronized(connectionClosedLock)
                     {
                        // begin F193735.3                     
                        if (!connectionClosed)
                        {
                           if (connection.isLoggingIOEvents()) connection.getConnectionEventRecorder().logDebug("invoking writeCtx.write() on context "+System.identityHashCode(writeCtx)+" to write all data with no timeout");
                           vc = writeCtx.write(IOWriteRequestContext.WRITE_ALL_DATA,
                                               this, 
                                               false, 
                                               IOWriteRequestContext.NO_TIMEOUT);
                   
                           
                        }
                        // end F193735.3
                     }
                     done = (vc == null);
                  }
               }
            }
         }
         while(!done);
      }
      catch(SIConnectionDroppedException connectionDroppedException)
      {
         // No FFDC code needed
         
         // This has been thrown because the priority queue was purged (most likely on another thread).
         // The exception is thrown to prevent threads in this method looping forever or hanging if the
         // conneciton is invalidate on another thread.  Therefore we simply swallow this exception and
         // allow the thread to exit this method.
         
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Caught SIConnectionDroppedException, Priority Queue has been purged");
      }
      catch(Error error)
      {
         FFDCFilter.processException
         (error, "com.ibm.ws.sib.jfapchannel.impl.ConnectionWriteCompletedCallback", JFapChannelConstants.CONNWRITECOMPCALLBACK_COMPLETE_03, connection.getDiagnostics(true));
         if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, error);

         // It might appear slightly odd for this code to catch Error (especially since the JDK docs say
         // that Error means that something has gone so badly wrong that you should abandon all hope).
         // This code makes one final stab at putting out some diagnostics about what happened (if we
         // propagate the Error up to the TCP Channel, it is sometimes lost) and closing down the
         // connection.  I figured that we might as well try to do something - as we can hardly make
         // things worse... (famous last words)
         
         connection.invalidate(false, error, "Error caught in ConnectionWriteCompletedCallback.complete()");
         
         // Re-throw the error to ensure that it causes the maximum devastation.
         // The JVM is probably very ill if an Error is thrown so attempt no recovery.
         throw error;
      }
      catch(RuntimeException runtimeException)
      {
         FFDCFilter.processException
         (runtimeException, "com.ibm.ws.sib.jfapchannel.impl.ConnectionWriteCompletedCallback", JFapChannelConstants.CONNWRITECOMPCALLBACK_COMPLETE_04, connection.getDiagnostics(true));
         if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, runtimeException);

         // We can reasonably try to recover from a runtime exception by invalidating the associated
         // connection.  This should drive the underlying TCP/IP socket to be closed.
         connection.invalidate(false, runtimeException, "RuntimeException caught in ConnectionWriteCompletedCallback.complete()");

         // Don't throw the RuntimeException on as we risk blowing away part of the TCP channel.
      }

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "complete");
	}
   // end F181603.2, D192359

	/**
	 * Part of the write callback.  Notified if an error occurres while processing
	 * a write request.
	 * MS:4 implement better close logic (reliability)
	 */
	public void error(NetworkConnection vc, IOWriteRequestContext wrc, IOException t)          // F176003, F184828, D194678 
	{
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "error", new Object[] {vc, wrc, t});      // F176003, F184828
      if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled() && (t != null)) SibTr.exception(this, tc, t);                     // F176003
      if (connection.isLoggingIOEvents()) connection.getConnectionEventRecorder().logDebug("error method invoked on write context "+System.identityHashCode(wrc)+" with exception "+t);
      try
      {
   		IOWriteRequestContext req = writeCtx;

         //Note that this also deals with the buffer returned by getBuffer.
         final WsByteBuffer[] buffers = req.getBuffers();
         if (buffers != null)
         {
            for(final WsByteBuffer buffer: buffers)
            {
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
   
         // Deal with the error by invalidating the connection.  That'll teach 'em.      
         final String message = "IOException received - " + t == null ? "" : t.getMessage();
         connection.invalidate(false, t, message);  // F176003, F224570      
   
         // begin F193735.3
     
      }
      catch(Error error)
      {
         FFDCFilter.processException
         (error, "com.ibm.ws.sib.jfapchannel.impl.ConnectionWriteCompletedCallback", JFapChannelConstants.CONNWRITECOMPCALLBACK_COMPLETE_01, connection.getDiagnostics(true));
         if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, error);

         // It might appear slightly odd for this code to catch Error (especially since the JDK docs say
         // that Error means that something has gone so badly wrong that you should abandon all hope).
         // This code makes one final stab at putting out some diagnostics about what happened (if we
         // propagate the Error up to the TCP Channel, it is sometimes lost) and closing down the
         // connection.  I figured that we might as well try to do something - as we can hardly make
         // things worse... (famous last words)
         
         connection.invalidate(false, error, "Error caught in ConnectionWriteCompletedCallback.error()");
         
         // Re-throw the error to ensure that it causes the maximum devastation.
         // The JVM is probably very ill if an Error is thrown so attempt no recovery.
         throw error;
      }
      catch(RuntimeException runtimeException)
      {
         FFDCFilter.processException
         (runtimeException, "com.ibm.ws.sib.jfapchannel.impl.ConnectionWriteCompletedCallback", JFapChannelConstants.CONNWRITECOMPCALLBACK_COMPLETE_05, connection.getDiagnostics(true));
         if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, runtimeException);

         // We can reasonably try to recover from a runtime exception by invalidating the associated
         // connection.  This should drive the underlying TCP/IP socket to be closed.
         connection.invalidate(false, runtimeException, "RuntimeException caught in ConnectionWriteCompletedCallback.error()");

         // Don't throw the RuntimeException on as we risk blowing away part of the TCP channel.
      }
      
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "error");
	}

   // begin D183461
   protected void physicalCloseNotification()
   {
      synchronized(connectionClosedLock)
      {
         connectionClosed = true;
      }
   }
   // end D183461

   // begin F181603.2, D192359
   /**
    * Copies the next "chunk" of transmission data to send into the supplied
    * buffer.  This method attempts to write as much transmission data as possible
    * into the buffer supplied as an argument.  It may place multiple transmissions
    * into the buffer and tracks what transmissions are "in flight" as well as what
    * transmission (if any) has been partially sent.
    * @param bufferToFill Buffer to fill with transmission data.
    * @return boolean True iff some data was copied.  False if no data was avaiable to
    * be copied. 
    */
   private boolean dequeueTransmissionData(WsByteBuffer bufferToFill) throws SIConnectionDroppedException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "dequeueTransmissionData", bufferToFill);
      
      boolean exhausedTransmissionsToSend = false;      
      boolean dataCopied = false;
      do      
      {
         synchronized(priorityQueue)
         {
            synchronized(this)
            {
               if (partiallySentTransmission == null)
               {
                  partiallySentTransmission = priorityQueue.dequeue();
      
                 
               }
            }
         }
                                 
         if (partiallySentTransmission == null)
            exhausedTransmissionsToSend = true;
         else
         {
            boolean finishedThisTransmission = 
               partiallySentTransmission.buildTransmission(bufferToFill);
            dataCopied = true;
            
            if (finishedThisTransmission)
            {
               //We now have a complete transmission for sending.
               //Is this transmission the last one that should be sent over this connection?
               terminate = partiallySentTransmission.isTerminal();
               
               if (partiallySentTransmission.getSendListener() != null)
               {
                  inflightSendListeners.add(partiallySentTransmission.getSendListener());
                  inflightConversations.add((ConversationImpl)partiallySentTransmission.getConversation());                     
               }

               partiallySentTransmission.release();                           // D226242
               synchronized(this)
               {
                  partiallySentTransmission = null;
               }
            }            
         }
      }
      while(!exhausedTransmissionsToSend && !terminate && (bufferToFill.remaining() > 0));
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "dequeueTransmissionData", ""+dataCopied);
      return dataCopied;
   }
   // end F181603.2, D192359
   	
   // begin D192359
   private boolean isWorkAvailable() throws SIConnectionDroppedException
   {
      return (partiallySentTransmission != null) || (!priorityQueue.isEmpty()); 	
   }
   // end D192359
}
