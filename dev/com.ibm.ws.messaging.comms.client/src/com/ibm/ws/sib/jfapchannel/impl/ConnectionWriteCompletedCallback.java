/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

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
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.common.impl/src/com/ibm/ws/sib/jfapchannel/impl/ConnectionWriteCompletedCallback.java, SIB.comms, WAS70.SIB, uu1215.01 1.46.1.3");
   }

   // Structure used to prioritise senders.
   // Method calls synchronized on 'priorityQueue'.
   private final PriorityQueue priorityQueue;

   // private TCPWriteRequestContext writeCtx;                                         // F184828
   private final IOWriteRequestContext writeCtx;

   // The connection that this callback is associated with.
   private final Connection connection;                                                   // F176003

   // Is the callback currently idle (ie. there is currently no TCP write request
   // outstanding, or we are not about to issue one).
   // Synchronized on 'this'
   private boolean idle = true;                                                     // F176003

   // Synchronized on 'this'
   private boolean terminate = false;                                               // F176003

   // Used to determine if the connection this callback is registered for is in the
   // process of closing.  Since closes can take place asynchronously from the
   // callback being invoked - before examining or setting this flag - synchronize
   // on the connectionCLosedLock object.
   private boolean connectionClosed = false;                                        // D183461

   // Lock object used to prevent concurrent use of connectionClosedFlag.
   private final Object connectionClosedLock = new String("connectionClosedLock");  // D183461

   /**
    * Class for holding pairs of objects in e.g. queues.
    * @param <L> Class of 'left' field.
    * @param <R> Class of 'right' field.
    */
   private static final class Pair<L,R> {
      public final L left;
      public final R right;

      Pair(L left, R right) {
         this.left = left;
         this.right = right;
      }

      @Override
      public String toString() {
         return String.format("{ %s, %s }", left, right);
      }
   }

   // Queue of send listener, conversation pairs for messages whose entire data transmission has been initiated,
   // but not yet completed.
   private final BlockingQueue<Pair<SendListener,Conversation>> sendCallbacks =
           new LinkedBlockingQueue<Pair<SendListener,Conversation>>();

   // Stores any partially sent (ie. didn't fully fit into our transmission buffer)
   // transmission.
   // Synchronized on 'this'
   private TransmissionData partiallySentTransmission = null;                            // F181603.2

   // Is this the first time this callback has been invoked to do work.  Used for
   // one off buffer initialisation.
   private final AtomicBoolean firstInvocation = new AtomicBoolean(true);

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
    * Encourages the JFAP Channel to write data. If the send callback is not currently busy
    * transmitting data, proddling it will cause it to start. If it is already sending, proddling is
    * harmless and doesn't irritate it.
    */
   // being F176003, F181603.2, D192359
   protected void proddle() throws SIConnectionDroppedException {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "proddle");

      boolean useThisThread = false;

      synchronized (priorityQueue) {
         synchronized (this) {
            if (idle) {
               useThisThread = isWorkAvailable();
               idle = !useThisThread;
            }
         }
      }

      if (useThisThread) {
         doWork(false);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "proddle");
   }

   /**
    * Part of the write completed callback interface.  Notified when a write operation
    * has completed.  This code sees if there is any more data to write and setups
    * the next write call as appropriate.
    */
   // begin F181603.2, D192359
   public void complete(NetworkConnection vc, IOWriteRequestContext wctx) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "complete", new Object[] {vc, wctx});
      if (connection.isLoggingIOEvents()) connection.getConnectionEventRecorder().logDebug("complete method invoked on write context "+System.identityHashCode(wctx));

      try {
         doWork(true);
      } catch(SIConnectionDroppedException connectionDroppedException) {
         // No FFDC code needed

         // This has been thrown because the priority queue was purged (most likely on another thread).
         // The exception is thrown to prevent threads in this method looping forever or hanging if the
         // conneciton is invalidate on another thread.  Therefore we simply swallow this exception and
         // allow the thread to exit this method.

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "Caught SIConnectionDroppedException, Priority Queue has been purged");
      } catch(Error error) {
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
      } catch(RuntimeException runtimeException) {
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

   /**
    * Looks to initiate write operations to write all available data from the priority queue to the network.
    * Continues until all data has been sent or until the return from a write call indicates its completion will
    * be asynchronous.
    * Once it has finished writing on this thread, calls the registered send listeners for all messages that have been
    * completely sent.
    * @param hasWritten true iff invoked as a result of a previous (async) write call.
    * @throws SIConnectionDroppedException
    */
   private void doWork(boolean hasWritten) throws SIConnectionDroppedException {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "doWork", Boolean.valueOf(hasWritten));
      final BlockingQueue<Pair<SendListener,Conversation>> readySendCallbacks =
              new LinkedBlockingQueue<Pair<SendListener,Conversation>>();
      boolean hasGoneAsync = false;
      do {
         boolean hasMoreWork;
         do {
            if (hasWritten) {
               sendCallbacks.drainTo(readySendCallbacks);
            }

            hasWritten = false;
            hasMoreWork = false;
            synchronized (priorityQueue) {
               synchronized (this) {
                  if (!isWorkAvailable()) break;
               }
            }

            final WsByteBuffer writeBuffer = getWriteContextBuffer();
            writeBuffer.clear();
            if (dequeueTransmissionData(writeBuffer)) {
               synchronized (connectionClosedLock) {
                  if (!connectionClosed) {
                     writeBuffer.flip();

                     if (connection.isLoggingIOEvents()) connection.getConnectionEventRecorder().logDebug("invoking writeCtx.write() on context "+System.identityHashCode(writeCtx)+" to write all data with no timeout");
                     final NetworkConnection vc = writeCtx.write(IOWriteRequestContext.WRITE_ALL_DATA, this, false, IOWriteRequestContext.NO_TIMEOUT);
                     hasGoneAsync = (vc == null);
                     hasWritten = true;
                     hasMoreWork = !hasGoneAsync;
                  }
               }
            }
         } while (hasMoreWork);
      } while (!hasGoneAsync && !switchToIdle());

      // This thread is no longer tasked with writing activity, so it is now safe to notify the "ready" send listeners -
      // i.e. those whose messages have been completely sent.
      // (It was not safe to do so prior to this point, as the listeners may create more messages to be sent, which
      //  could end in deadlock if this thread were still tasked with the writing.)
      notifyReadySendListeners(readySendCallbacks);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "doWork");
   }

   /**
    * Call send listener callback for each entry in the given queue.
    * Any exception thrown from a listener's callback will cause the connection to be invalidated.
    * @param readySendCallbacks
    */
   private void notifyReadySendListeners(BlockingQueue<Pair<SendListener, Conversation>> readySendCallbacks) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "notifyReadySendListeners", readySendCallbacks);
      try {
         for (Pair<SendListener, Conversation> callback : readySendCallbacks) {
            callback.left.dataSent(callback.right);
         }
      } catch (Throwable t) {
         FFDCFilter.processException
                 (t, "com.ibm.ws.sib.jfapchannel.impl.ConnectionWriteCompletedCallback", JFapChannelConstants.CONNWRITECOMPCALLBACK_COMPLETE_01, connection.getDiagnostics(true));
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "exception invoking send listener data sent");
         if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(tc, t);
         connection.invalidate(true, t, "send listener threw exception");  // D224570
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "notifyReadySendListeners");
   }

   /**
    * Switch the 'idle' flag back to 'true', provided that there is no work available.
    * @return true iff 'idle' was set to 'true'; false iff there is now work available.
    * @throws SIConnectionDroppedException
    */
   private boolean switchToIdle() throws SIConnectionDroppedException {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "switchToIdle");
      final boolean noMoreWork;
      synchronized (priorityQueue) {
         synchronized (this) {
            noMoreWork = !isWorkAvailable();
            idle = noMoreWork;
         }
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "switchToIdle", Boolean.valueOf(noMoreWork));
      return noMoreWork;
   }

   /**
    * Returns the single WsByteBuffer set in 'writeCtx', ensuring that it is a direct byte buffer and is of
    * sufficient capacity.
    * @return the (single, non-null) byte buffer set in 'writeCtx'.
    */
   private WsByteBuffer getWriteContextBuffer() {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getWriteContextBuffer");
      WsByteBuffer writeBuffer = getSoleWriteContextBuffer();

      if (firstInvocation.compareAndSet(true, false) || (writeBuffer == null)) {
         final int writeBufferSize =
                 Integer.parseInt(RuntimeInfo.getProperty("com.ibm.ws.sib.jfapchannel.DEFAULT_WRITE_BUFFER_SIZE", "" + JFapChannelConstants.DEFAULT_WRITE_BUFFER_SIZE));

         if ((writeBuffer != null) && (!writeBuffer.isDirect() || writeBuffer.capacity() < writeBufferSize)) {
            writeBuffer.release();
            writeBuffer = null;
         }

         if (writeBuffer == null) {
            writeBuffer = WsByteBufferPool.getInstance().allocateDirect(writeBufferSize);      // F196678.10
            writeCtx.setBuffer(writeBuffer);
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getWriteContextBuffer", writeBuffer);
      return writeBuffer;
   }

   /**
    * Returns the first WsByteBuffer set in 'writeCtx', ensuring that it is the sole byte buffer set in 'writeCtx', and
    * releasing any other byte buffers that had been registered there.
    * @return the sole byte buffer set in 'writeCtx'; maybe null if there is no such byte buffer.
    */
   private WsByteBuffer getSoleWriteContextBuffer() {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getSoleWriteContextBuffer");
      WsByteBuffer writeBuffer = null;
      final WsByteBuffer[] writeBuffers = writeCtx.getBuffers();
      if (writeBuffers != null) {
         final int writeBuffersSize = writeBuffers.length;
         if (writeBuffersSize > 0) {
            writeBuffer = writeBuffers[0];
            if (writeBuffersSize > 1) {
               writeCtx.setBuffer(writeBuffer);
               for (int i = 1; i < writeBuffersSize; i++) {
                  if (writeBuffers[i] != null) writeBuffers[i].release();
               }
            }
         }
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getSoleWriteContextBuffer", writeBuffer);
      return writeBuffer;
   }

   /**
    * Part of the write callback.  Notified if an error occurs while processing
    * a write request.
    * MS:4 implement better close logic (reliability)
    */
    public void error(NetworkConnection vc, IOWriteRequestContext wrc, IOException t) {
       if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "error", new Object[]{vc, wrc, t});      // F176003, F184828
       if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled() && (t != null)) SibTr.exception(this, tc, t);                     // F176003
       if (connection.isLoggingIOEvents()) connection.getConnectionEventRecorder().logDebug("error method invoked on write context " + System.identityHashCode(wrc) + " with exception " + t);
       try {
          //Note that this also deals with the buffer returned by getBuffer.
          WsByteBuffer[] buffers;
          buffers = writeCtx.getBuffers();
          writeCtx.setBuffers(null);

          if (buffers != null) {
             for (WsByteBuffer buffer : buffers) {
                try {
                   if (buffer != null) buffer.release();
                } catch (RuntimeException e) {
                   //Absorb any exceptions if it gets released by another thread (for example by Connection.nonThreadSafePhysicalClose).
                   //No FFDC code needed
                   if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Caught exception on releasing buffer.", e);
                }
             }
          } else {
             if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Request has no buffers: " + writeCtx);
          }

          // Deal with the error by invalidating the connection.  That'll teach 'em.
          final String message = "IOException received - " + t == null ? "" : t.getMessage();
          connection.invalidate(false, t, message);  // F176003, F224570
       } catch (Error error) {
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
       } catch (RuntimeException runtimeException) {
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

   /**
    * Register notification that the physical underlying connection has been closed.
    */
   protected void physicalCloseNotification() {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "physicalCloseNotification");
      synchronized(connectionClosedLock) {
         connectionClosed = true;
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "physicalCloseNotification");
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
    * @return boolean True iff some data was copied.  False if no data was available to
    * be copied.
    */
   private boolean dequeueTransmissionData(WsByteBuffer bufferToFill) throws SIConnectionDroppedException {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "dequeueTransmissionData", bufferToFill);

      boolean exhaustedTransmissionsToSend = false;
      boolean dataCopied = false;
      boolean isTerminal = false;
      TransmissionData data;
      synchronized (this) {
         data = partiallySentTransmission;
      }
      try {
         do {
            if (data == null) {
               synchronized (priorityQueue) {
                  data = priorityQueue.dequeue();
               }
            }

            if (data == null) {
               exhaustedTransmissionsToSend = true;
            } else {
               boolean finishedThisTransmission = data.buildTransmission(bufferToFill);
               dataCopied = true;

               if (finishedThisTransmission) {
                  final SendListener sendListener = data.getSendListener();
                  if (sendListener != null) {
                     sendCallbacks.add(new Pair<SendListener, Conversation>(sendListener, data.getConversation()));
                  }
                  //Is this transmission the last one that should be sent over this connection?
                  isTerminal = data.isTerminal();
                  data.release();                           // D226242
                  data = null;
               }
            }
         } while (!exhaustedTransmissionsToSend && !isTerminal && (bufferToFill.remaining() > 0));
      } finally {
         synchronized (this) {
            partiallySentTransmission = data;
            terminate = isTerminal;
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "dequeueTransmissionData", Boolean.valueOf(dataCopied));
      return dataCopied;
   }

   // begin D192359

   /**
    * Returns true iff there is more work available to be processed.
    * @return true iff there is more work available to be processed.
    * @throws SIConnectionDroppedException
    */
   private boolean isWorkAvailable() throws SIConnectionDroppedException {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "isWorkAvailable");
      final boolean isWork;
      if (terminate) {
         isWork = false;
      } else {
         isWork = (partiallySentTransmission != null) || !priorityQueue.isEmpty();
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "isWorkAvailable", Boolean.valueOf(isWork));
      return isWork;
   }
   // end D192359
}
