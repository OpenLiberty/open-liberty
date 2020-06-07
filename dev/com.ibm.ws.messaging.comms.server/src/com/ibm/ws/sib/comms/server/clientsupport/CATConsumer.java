/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.server.clientsupport;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.server.CommsServerByteBufferPool;
import com.ibm.ws.sib.comms.server.ConversationState;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.SendListener;
import com.ibm.ws.sib.jfapchannel.Conversation.ThrottlingPolicy;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.OrderingContext;
import com.ibm.wsspi.sib.core.SIMessageHandle;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;


/**
 * This class acts as the superclass for all the different types of server side consumer we have
 * in comms. As such, there will be different flavours of this consumer depending on whether we
 * are read-ahead for example.
 * <p>
 * When a request comes up from the client for a specific operation on a consumer, such as a receive
 * the call is made on the main consumer (CATMainConsumer) and the main consumer will decide to
 * delegate to say the synchronous consumer flavour of this class. If no specific flavour is
 * available, the call will be invoked on this class. This is normally an error, but there are
 * some cases when invoking methods on this class are valid, during close for example.
 *
 * @author Gareth Matthews
 */
public abstract class CATConsumer
{
   /** Class name for FFDC's */
   private static String CLASS_NAME = CATConsumer.class.getName();

   /** The pool manager */
   protected static final CommsServerByteBufferPool poolManager = CommsServerByteBufferPool.getInstance();

   /** The trace */
   private static final TraceComponent tc = SibTr.register(CATConsumer.class,
                                                           CommsConstants.MSG_GROUP,
                                                           CommsConstants.MSG_BUNDLE);

   /** The NLS reference */
   private static final TraceNLS nls = TraceNLS.getTraceNLS(CommsConstants.MSG_BUNDLE);

   /** Log class info on static load */
   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Source info: @(#)SIB/ws/code/sib.comms.server.impl/src/com/ibm/ws/sib/comms/server/clientsupport/CATConsumer.java, SIB.comms, WASX.SIB, aa1225.01 1.67.1.1");
   }
   // PH20984
   // Could we tidy this code up by incorporating more of the state flags into this enum?
   // The _stopped flag would be an obvious candidate to be incorporated here,
   // but I'm just going to focus on fixing APAR PH20984 for the moment.
   public enum State
   {
      // Put UNDEFINED in here for any class that doesn't implement getState() properly.
      STOPPED, STARTING, STARTED, STOPPING, CLOSED, UNDEFINED;

      // A couple of methods that should make the code using this enum a bit more readable.
      public boolean isStarted()
      {
         return this.equals(STARTED);
      }

      public boolean isStopped()
      {
         return this.equals(STOPPED);
      }

      public boolean isTransitioning()
      {
         return this.equals(STARTING) || this.equals(STOPPING);
      }
   }

   protected State state = State.STOPPED;
   protected ReentrantLock stateLock = new ReentrantLock();
   protected Condition stateTransition = stateLock.newCondition();

   public State getState()
   {
      try
      {
         stateLock.lock();
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
         {
            SibTr.debug(tc, String.format("[@%x] State = " + state, this.hashCode()));
         }
         return state;
      }
      finally
      {
         stateLock.unlock();
      }
   }

   public void setState(State newState)
   {
      try
      {
         stateLock.lock();
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
         {
            SibTr.debug(tc, String.format("[@%x] Setting state from " + state + " to " + newState, this.hashCode()));
            if ( (State.STOPPED==newState&&State.STARTING==state)
               ||(State.STARTED==newState&&State.STOPPING==state)
               )
            {
               SibTr.debug(tc
                          ,String.format("[@%x] WARNING: possible error in state transition"
                                        ,Thread.currentThread().getStackTrace()
                                        )
                          );
            }
         }
         state = newState;
         stateTransition.signal();
      }
      finally
      {
         stateLock.unlock();
      }
   }

   /** Counter of the number of messages sent to the client */
   protected long messagesSent = 0;

   /** Counter of the number of batches sent to the client */
   protected long batchesSent = 0;

   /** Counter of the number of requests to start the session */
   protected long requestsReceived = 0;

   /**
    * Constructor.
    */
   public CATConsumer()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }

   /**
    * @return Returns the real ConsumerSession.
    */
   protected abstract ConsumerSession getConsumerSession();

   /**
    * @return Returns the associated conversation.
    */
   protected abstract Conversation getConversation();

   /**
    * @return Returns the lowest priority for this consumer session so that we know what priority
    *         to process requests needing to go behind all others at.
    */
   protected abstract int getLowestPriority();

   /**
    * @return Returns the client session id for this session.
    */
   protected abstract short getClientSessionId();

   /**
    * @return Returns this sessions's unrecoverable reliability.
    */
   protected abstract Reliability getUnrecoverableReliability();

   /**
    * This method should be handled by the appropriate subclass
    *
    * @param requestNumber
    * @param tran
    * @param timeout
    */
   public void receive(int requestNumber,
                       int tran,
                       long timeout)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "receive",
                                           new Object[]{requestNumber, tran, timeout});

      SIErrorException e = new SIErrorException(
         nls.getFormattedMessage("PROTOCOL_ERROR_SICO2003", null, null)
      );

      FFDCFilter.processException(e, CLASS_NAME + ".receive",
                                  CommsConstants.CATCONSUMER_RECEIVE_01,
                                  this);

      SibTr.error(tc, "PROTOCOL_ERROR_SICO2003", e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "receive");

      // Re-throw this exception so that the client will informed if required
      throw e;
   }

   /**
    * Performs the generic session close. A response is sent to the client when this
    * has been comleted.
    *
    * @param requestNumber
    */
   public void close(int requestNumber)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "close", requestNumber);

      try
      {
        ConsumerSession cs = getConsumerSession();
        if (cs !=null) cs.close();

        try
        {
           getConversation().send(poolManager.allocate(),
                                  JFapChannelConstants.SEG_CLOSE_CONSUMER_SESS_R,
                                  requestNumber,
                                  getLowestPriority(),
                                  true,
                                  ThrottlingPolicy.BLOCK_THREAD,
                                  null);
         }
         catch (SIException e)
         {
            FFDCFilter.processException(e, CLASS_NAME + ".close",
                                        CommsConstants.CATCONSUMER_CLOSE_02,
                                        this);

            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2013", e);
         }
      }
      catch (SIException e)
      {
         //No FFDC code needed
         //Only FFDC if we haven't received a meTerminated event.
         if(!((ConversationState)getConversation().getAttachment()).hasMETerminated())
         {
            FFDCFilter.processException(e, CLASS_NAME + ".close",
                                        CommsConstants.CATCONSUMER_CLOSE_01,
                                        this);
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, e.getMessage(), e);

         StaticCATHelper.sendExceptionToClient(e,
                                               CommsConstants.CATCONSUMER_CLOSE_01,
                                               getConversation(), requestNumber);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "close");
   }

   // SIB0115d.comms start

   public void setAsynchConsumerCallback(int requestNumber,
                                         int maxActiveMessages,
                                         long messageLockExpiry,
                                         int batchsize,
                                         OrderingContext orderContext,
                                         boolean stoppable,
                                         int maxSequentialFailures,
                                         long hiddenMessageDelay)
   {
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setAsynchConsumerCallback",
                                          new Object[]
                                          {
                                             requestNumber,
                                             maxActiveMessages,
                                             messageLockExpiry,
                                             batchsize,
                                             orderContext,
                                             stoppable,
                                             maxSequentialFailures,
                                             hiddenMessageDelay});

     SIErrorException e = new SIErrorException(nls.getFormattedMessage("PROTOCOL_ERROR_SICO2003", null, null));

     FFDCFilter.processException(e, CLASS_NAME + ".setAsynchConsumerCallback",CommsConstants.CATCONSUMER_SETASYNCHCALLBACK_01,this);

     SibTr.error(tc, "PROTOCOL_ERROR_SICO2003", e);

     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setAsynchConsumerCallback");

     // Re-throw this exception so that the client will informed if required
     throw e;
   }

   // SIB0115d.comms end

   /**
    * This method will unset an asynch callback.
    *
    * @param requestNumber
    * @param stoppable async consumer or not
    */
   public void unsetAsynchConsumerCallback(int requestNumber, boolean stoppable)                        //SIB0115d.comms
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "unsetAsynchConsumerCallback", "requestNumber="+requestNumber);

      try
      {
         if (stoppable) {                                                                               //SIB0115d.comms
           getConsumerSession().deregisterStoppableAsynchConsumerCallback();                            //SIB0115d.comms
         } else {                                                                                       //SIB0115d.comms
           getConsumerSession().deregisterAsynchConsumerCallback();
         }                                                                                              //SIB0115d.comms

         try
         {
            getConversation().send(poolManager.allocate(),
                                   JFapChannelConstants.SEG_DEREGISTER_ASYNC_CONSUMER_R,
                                   requestNumber,
                                   getLowestPriority(),
                                   true,
                                   ThrottlingPolicy.BLOCK_THREAD,
                                   null);
         }
         catch (SIException e)
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".unsetAsynchConsumerCallback",
                                        CommsConstants.CATCONSUMER_UNSETASYNCHCALLBACK_01,
                                        this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, e.getMessage(), e);

            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2013", e);
         }
      }
      catch (SIException e)
      {
         //No FFDC code needed
         //Only FFDC if we haven't received a meTerminated event.
         if(!((ConversationState)getConversation().getAttachment()).hasMETerminated())
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".unsetAsynchConsumerCallback",
                                        CommsConstants.CATCONSUMER_UNSETASYNCHCALLBACK_02,
                                        this);
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, e.getMessage(), e);

         StaticCATHelper.sendExceptionToClient(e,
                                               CommsConstants.CATCONSUMER_UNSETASYNCHCALLBACK_02,
                                               getConversation(), requestNumber);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "unsetAsynchConsumerCallback");
   }

   /**
    * Performs the generic session start. No response is normally required to be sent to
    * the client unless the sendReply parameter is true.
    *
    * @param requestNumber
    * @param deliverImmediately
    * @param sendReply Flag to indicate whether a reply should be sent or not
    * @param sendListener A send listener that will be notified when the data is sent
    */
   public void start(int requestNumber, boolean deliverImmediately, boolean sendReply, SendListener sendListener)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "start",
                                           new Object[]{""+requestNumber,
                                                        ""+deliverImmediately,
                                                        ""+sendReply,
                                                        sendListener});

      try
      {
         // Set the started flag before starting the session in case deliverImmediately is true in which case message processor
         // may immediately deliver a message to the async consumer (consumeMessages) which will stop the session and set
         // started=false. We don't want this method setting started=true after consumeMessages has set it false hence the
         // need to set started=true before starting the session.
         if (state.isStopped()) setState(State.STARTING); // try always transition states cleanly
         setState(State.STARTED);
         getConsumerSession().start(deliverImmediately);
         requestsReceived++;

         if (sendReply)
         {
            try
            {
               // The send listener is passed into the send() call so that we can be notified
               // when the data leaves the box
               getConversation().send(poolManager.allocate(),
                                      JFapChannelConstants.SEG_START_SESS_R,
                                      requestNumber,
                                      JFapChannelConstants.PRIORITY_HIGHEST,
                                      true,
                                      ThrottlingPolicy.BLOCK_THREAD,
                                      sendListener);
            }
            catch (SIException e)
            {                              
               FFDCFilter.processException(e,
                                           CLASS_NAME + ".start",
                                           CommsConstants.CATCONSUMER_START_02,
                                           this);

               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, e.getMessage(), e);

               SibTr.error(tc, "COMMUNICATION_ERROR_SICO2013", e);

               // in send() there is at least one example where a sub class of SIException is thrown
               // and errorOccurred() is not called on the listener.  So as a catch all we will do it
               // here safe in the knowledge that we can't do it too many times now.
               
               // if e is an SIConnectionLostException then lets pass it in to errorOccurred(),
               // otherwise null
               SIConnectionLostException cle = (e instanceof SIConnectionLostException) ? 
                                                   (SIConnectionLostException)e : null;
               
               sendListener.errorOccurred(cle, getConversation());
            }
         }
         else
         {
            // If we are not sending a reply, then call the dataSent() method anyway to ensure
            // that if any state needs to be updated it can be done.
            sendListener.dataSent(getConversation());
         }
      }
      catch (SIException e)
      {
         //No FFDC code needed

         // Note that we failed to start the consumer
         setState(State.STOPPED);

         //Only FFDC if we haven't received a meTerminated event.
         if(!((ConversationState)getConversation().getAttachment()).hasMETerminated())
         {
            FFDCFilter.processException(e, CLASS_NAME + ".start",
                                        CommsConstants.CATCONSUMER_START_01,
                                        this);
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, e.getMessage(), e);
         
         // If the start failed and we are sending a reply, we can do this now
         if (sendReply)
         {
            StaticCATHelper.sendExceptionToClient(e, CommsConstants.CATCONSUMER_START_01,
                                                  getConversation(), requestNumber);
         }
         
         // we must now get the sendlistener to post the semaphore, knowing that we cannot
         // do it too many times
         sendListener.errorOccurred(null, getConversation());
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "start");
   }

   /**
    * Since that stopping a consumer is the same for all types of consumer, this method
    * provides that generic shutdown functionality and replies to the client when the
    * <code>stop()</code> has returned.
    *
    * @param requestNumber The request number to reply on.
    * @param sendListener A send listener that will be notified when the data is sent
    */
   public void stop(int requestNumber, SendListener sendListener)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "stop",
                                           new Object[]{requestNumber, sendListener});

      try
      {
         if (state.isStarted()) setState(State.STOPPING); // try always transition states cleanly
         getConsumerSession().stop();
         setState(State.STOPPED);

         // The send listener is passed into the send() call so that we can be notified
         // when the data leaves the box
         try
         {
            getConversation().send(poolManager.allocate(),
                                   JFapChannelConstants.SEG_STOP_SESS_R,
                                   requestNumber,
                                   getLowestPriority(),
                                   true,
                                   ThrottlingPolicy.BLOCK_THREAD,
                                   sendListener);
         }
         catch (SIException e)
         {            
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".stop",
                                        CommsConstants.CATCONSUMER_STOP_02,
                                        this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, e.getMessage(), e);

            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2013", e);

            // in send() there is at least one example where a sub class of SIException is thrown
            // and errorOccurred() is not called on the listener.  So as a catch all we will do it
            // here safe in the knowledge that we can't do it too many times now.
            
            // if e is an SIConnectionLostException then lets pass it in to errorOccurred(),
            // otherwise null
            SIConnectionLostException cle = (e instanceof SIConnectionLostException) ? 
                                                (SIConnectionLostException)e : null;
            
            sendListener.errorOccurred(cle, getConversation());
         }
      }
      catch (SIException e)
      {
         //No FFDC code needed
         //Only FFDC if we haven't received a meTerminated event.
         if(!((ConversationState)getConversation().getAttachment()).hasMETerminated())
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".stop",
                                        CommsConstants.CATCONSUMER_STOP_01,
                                        this);
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, e.getMessage(), e);

         StaticCATHelper.sendExceptionToClient(e,
                                               CommsConstants.CATCONSUMER_STOP_01,
                                               getConversation(), requestNumber);

         // Also notify the send listener
         sendListener.errorOccurred(null, getConversation());
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "stop");
   }

   /**
    * This method should be handled by the appropriate subclass
    *
    * @param requestNumber
    * @param msgIds
    */
   public void readSet(int requestNumber, SIMessageHandle[] msgHandles)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "readSet",
                                           new Object[]{requestNumber, msgHandles});

      SIErrorException e = new SIErrorException(
         nls.getFormattedMessage("PROTOCOL_ERROR_SICO2003", null,null)
      );

      FFDCFilter.processException(e, CLASS_NAME + ".readSet",
                                  CommsConstants.CATCONSUMER_READSET_01,
                                  this);

      SibTr.error(tc, "PROTOCOL_ERROR_SICO2003", e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "readSet");

      // Re-throw this exception so that the client will informed if required
      throw e;
   }

   /**
    * This method should be handled by the appropriate subclass
    *
    * @param requestNumber
    * @param msgIds
    * @param tran
    */
   public void readAndDeleteSet(int requestNumber, SIMessageHandle[] msgHandles, int tran)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "readAndDeleteSet",
                                           new Object[]{requestNumber, msgHandles, tran});

      SIErrorException e = new SIErrorException(
         nls.getFormattedMessage("PROTOCOL_ERROR_SICO2003", null,null)
      );

      FFDCFilter.processException(e, CLASS_NAME + ".readAndDeleteSet",
                                  CommsConstants.CATCONSUMER_READANDDELTESET_01,
                                  this);

      SibTr.error(tc, "PROTOCOL_ERROR_SICO2003", e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "readAndDeleteSet");

      // Re-throw this exception so that the client will informed if required
      throw e;
   }

   /**
    * This method should be handled by the appropriate subclass
    *
    * @param requestNumber
    * @param msgIds
    * @param reply
    */
   public void unlockSet(int requestNumber, SIMessageHandle[] msgHandles, boolean reply)                  // f199593, F219476.2
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "unlockSet",
                                           new Object[]{requestNumber, msgHandles, reply});

      SIErrorException e = new SIErrorException(
         nls.getFormattedMessage("PROTOCOL_ERROR_SICO2003", null,null)
      );

      FFDCFilter.processException(e, CLASS_NAME + ".unlockSet",
                                  CommsConstants.CATCONSUMER_UNLOCKSET_01,
                                  this);

      SibTr.error(tc, "PROTOCOL_ERROR_SICO2003", e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "unlockSet");

      // Re-throw this exception so that the client will informed if required
      throw e;
   }
   
   /** 
    * This method should be handled by the appropriate subclass
    * 
    * @param requestNumber
    * @param msgIds
    * @param reply
    * @param incrementLockCount Indicates whether the lock count should be incremented for this unlock
    */
   public void unlockSet(int requestNumber, SIMessageHandle[] msgHandles, boolean reply, boolean incrementLockCount)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "unlockSet",
                                           new Object[]{requestNumber, msgHandles, reply, incrementLockCount});

      SIErrorException e = new SIErrorException(
         nls.getFormattedMessage("PROTOCOL_ERROR_SICO2003", null,null)
      );

      FFDCFilter.processException(e, CLASS_NAME + ".unlockSet",
                                  CommsConstants.CATCONSUMER_UNLOCKSET_02,
                                  this);

      SibTr.error(tc, "PROTOCOL_ERROR_SICO2003", e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "unlockSet");

      // Re-throw this exception so that the client will informed if required
      throw e;
   }

   /**
    * This method should be handled by the appropriate subclass
    *
    * @param requestNumber
    * @param msgIds
    * @param tran
    * @param reply
    */
   public void deleteSet(int requestNumber,
                         SIMessageHandle[] msgHandles,
                         int tran,
                         boolean reply)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "deleteSet",
                                           new Object[]{requestNumber, msgHandles, tran, reply});

      SIErrorException e = new SIErrorException(
         nls.getFormattedMessage("PROTOCOL_ERROR_SICO2003", null,null)
      );

      FFDCFilter.processException(e, CLASS_NAME + ".deleteSet",
                                  CommsConstants.CATCONSUMER_DELETESET_01,
                                  this);

      SibTr.error(tc, "PROTOCOL_ERROR_SICO2003", e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "deleteSet");

      // Re-throw this exception so that the client will informed if required
      throw e;
   }

   /**
    * This method should be handled by the appropriate subclass
    *
    * @param requestNumber
    */
   public void unlockAll(int requestNumber)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "unlockAll", requestNumber);

      SIErrorException e = new SIErrorException(
         nls.getFormattedMessage("PROTOCOL_ERROR_SICO2003", null, null)
      );

      FFDCFilter.processException(e, CLASS_NAME + ".unlockAll",
                                  CommsConstants.CATCONSUMER_UNLOCKALL_01,
                                  this);

      SibTr.error(tc, "PROTOCOL_ERROR_SICO2003", e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "unlockAll");

      // Re-throw this exception so that the client will informed if required
      throw e;
   }

   /**
    * This method should be handled by the appropriate subclass
    *
    * @param requestNumber
    * @param receivedBytes
    * @param requestedBytes
    */
   public void requestMsgs(int requestNumber, int receivedBytes, int requestedBytes)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "requestMsgs",
                                           new Object[]{requestNumber, receivedBytes, requestedBytes});

      SIErrorException e = new SIErrorException(
         nls.getFormattedMessage("PROTOCOL_ERROR_SICO2003", null,null)
      );

      FFDCFilter.processException(e, CLASS_NAME + ".requestMsgs",
                                  CommsConstants.CATCONSUMER_REQUESTMSGS_01,
                                  this);

      SibTr.error(tc, "PROTOCOL_ERROR_SICO2003", e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "requestMsgs");

      // Re-throw this exception so that the client will informed if required
      throw e;
   }

   /**
    * This method should be handled by the appropriate subclass
    *
    * @param requestNumber
    */
   public void flush(int requestNumber)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "flush", requestNumber);

      SIErrorException e = new SIErrorException(
         nls.getFormattedMessage("PROTOCOL_ERROR_SICO2003", null,null)
      );

      FFDCFilter.processException(e, CLASS_NAME + ".flush",
                                  CommsConstants.CATCONSUMER_FLUSH_01,
                                  this);

      SibTr.error(tc, "PROTOCOL_ERROR_SICO2003", e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "flush");

      // Re-throw this exception so that the client will informed if required
      throw e;
   }

   /**
    * Resets a browser session.  This implementation barfs because this method should
    * never be invoked on anything other than a CATBrowseConsumer (which overrides it).
    */
   public void reset()
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException,
             SIErrorException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "reset");

      SIErrorException e = new SIErrorException(
         nls.getFormattedMessage("PROTOCOL_ERROR_SICO2003", null,null)
      );

      FFDCFilter.processException(e, CLASS_NAME + ".reset",
                                  CommsConstants.CATCONSUMER_RESET_01,
                                  this);

      SibTr.error(tc, "PROTOCOL_ERROR_SICO2003", e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "reset");

      // Re-throw this exception so that the client will informed if required
      throw e;
   }

   /**
    * @return Returns the state of the consumer
    */
   public String toString()
   {
      return getClass().getName() + "@" + Integer.toHexString(hashCode()) +
             ": State:" + state +
             ", messagesSent: " + messagesSent +
             ", batchesSent: " + batchesSent +
             ", startRequestsReceived: " + requestsReceived;
   }
   
   /**
    * This method should be handled by the appropriate subclass
    *
    * @param requestNumber
    * @param incrementUnlockCount 
    */
   public void unlockAll(int requestNumber, boolean incrementUnlockCount)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "unlockAll", new Object[] {requestNumber,incrementUnlockCount});

      SIErrorException e = new SIErrorException(
         nls.getFormattedMessage("PROTOCOL_ERROR_SICO2003", null, null)
      );

      FFDCFilter.processException(e, CLASS_NAME + ".unlockAll",
                                  CommsConstants.CATCONSUMER_UNLOCKALL_02,
                                  this);

      SibTr.error(tc, "PROTOCOL_ERROR_SICO2003", e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "unlockAll");

      // Re-throw this exception so that the client will informed if required
      throw e;
   }
}
