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
package com.ibm.ws.sib.comms.server.clientsupport;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.server.CommsServerByteBuffer;
import com.ibm.ws.sib.comms.server.ConversationState;
import com.ibm.ws.sib.comms.server.IdToTransactionTable;
import com.ibm.ws.sib.comms.server.ServerLinkLevelState;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.HandshakeProperties;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.SendListener;
import com.ibm.ws.sib.jfapchannel.Conversation.ThrottlingPolicy;
import com.ibm.ws.sib.mfp.IncorrectMessageTypeException;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.MessageCopyFailedException;
import com.ibm.ws.sib.mfp.MessageEncodeFailedException;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.OrderingContext;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SIMessageHandle;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;


/**
 * This class  the specific consumer handler for read-ahead consumer sessions.
 *
 * @author Mike Schmitt
 */
public class CATProxyConsumer extends CATConsumer
{
  /**
   * The ProxyConsumerSendListner used when we send a message so we make
   * sure the data got sent. If not then we invalidate the conversation 
   */
  private static class ProxyConsumerSendListener implements SendListener
  {

    public void dataSent(Conversation conversation)
    {
      // data sent no need to worry
    }

    public void errorOccurred(SIConnectionLostException exception, Conversation conversation)
    {
      // There was a error when sending the sibus message
      // invalidate the connection
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Invalidating conversation: "+ conversation+" because the send failed due to: "+ exception);
      
      conversation.getConnectionReference().invalidate(false, exception, "errorOccurred when sending SIBus Message");
    }
  }

   /** Class name for FFDC's */
   private static String CLASS_NAME = CATProxyConsumer.class.getName();

   /** Trace */
   private static final TraceComponent tc = SibTr.register(CATProxyConsumer.class,
                                                           CommsConstants.MSG_GROUP,
                                                           CommsConstants.MSG_BUNDLE);

   /** Log class info on static load */
   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Source info: @(#) SIB/ws/code/sib.comms.server.impl/src/com/ibm/ws/sib/comms/server/clientsupport/CATProxyConsumer.java, SIB.comms, WASX.SIB, aa1225.01 1.78.1.2");
   }

   /** The send listener instance (shared between all instance of this class) */
   private static final SendListener INVALIDATE_CONNECTION_ON_ERROR = new ProxyConsumerSendListener();
   
   /** The callback instance that will be notified when messages are available */
   private CATAsynchReadAheadReader callback;

   /** The handle on the main consumer class */
   private CATMainConsumer mainConsumer;

   /** The amount of bytes we have sent to the client */
   private int sentBytes = 0;

   /** The amount of bytes that the client wants to 'read-ahead' */
   private int requestedBytes = 0;
   
   /**
    * Constructor
    *
    * @param mainConsumer
    */
   public CATProxyConsumer(CATMainConsumer mainConsumer)
   {
      super();

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", mainConsumer);

      this.mainConsumer = mainConsumer;

      // We won't worry about the lock here as we're still constructing the consumer
      requestedBytes = mainConsumer.getRequestedBytes();
      callback = new CATAsynchReadAheadReader(this,mainConsumer);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }

   /**
    * @return Returns the actual SI ConsumerSession
    */
   protected ConsumerSession getConsumerSession()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getConsumerSession");
      ConsumerSession sess = mainConsumer.getConsumerSession();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getConsumerSession", sess);
      return sess;
   }

   /**
    * @return Returns the conversation.
    */
   protected Conversation getConversation()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getConversation");
      Conversation conv = mainConsumer.getConversation();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getConversation", conv);
      return conv;
   }

   /**
    * @return Returns the session lowest priority.
    */
   protected int getLowestPriority()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getLowestPriority");
      int lowestPri = mainConsumer.getLowestPriority();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getLowestPriority", lowestPri);
      return lowestPri;
   }

   /**
    * @return Returns the client session Id.
    */
   protected short getClientSessionId()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getClientSessionId");
      short sessId = mainConsumer.getClientSessionId();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getClientSessionId");
      return sessId;
   }

   /**
    * @return Returns the sessions unrecoverable reliability.
    */
   protected Reliability getUnrecoverableReliability()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getUnrecoverableReliability");
      Reliability rel = mainConsumer.getUnrecoverableReliability();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getUnrecoverableReliability");
      return rel;
   }

   /**
    * Sets the async consumer for a read ahead session.
    *
    * @param requestNumber
    * @param maxActiveMessages
    * @param messageLockExpiry
    * @param batchsize
    * @param orderContext This paramter is ignored for read ahead consumers.
    * @param stoppable (ignored)
    * @param maxSequentialFailures (ignored)
    * @param hiddenMessageDelay
    */
   public void setAsynchConsumerCallback(int requestNumber,
                                         int maxActiveMessages,
                                         long messageLockExpiry,
                                         int batchsize,
                                         OrderingContext orderContext,
                                         boolean stoppable,                                             //SIB0115d.comms
                                         int maxSequentialFailures,
                                         long hiddenMessageDelay)                                       //SIB0115d.comms
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "setAsynchConsumerCallback",
                                           new Object[]
                                           {
                                              requestNumber,
                                              maxActiveMessages,
                                              messageLockExpiry,
                                              batchsize,
                                              orderContext,
                                              stoppable,                                                //SIB0115d.comms
                                              maxSequentialFailures,                                    //SIB0115d.comms
                                              hiddenMessageDelay
                                           });

      try
      {
         // Note: per the design document, a readahead consumer callback should always be created
         // with a batch size of 1 - this case should never happen.
         if (batchsize > 1)
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
               SibTr.debug(this, tc, "*** The batch size submitted to CATProxyConsumer was great than 1");
            }
         }

         getConsumerSession().registerAsynchConsumerCallback(callback,
                                                             maxActiveMessages,
                                                             messageLockExpiry,
                                                             batchsize,
                                                             null);
      }
      catch (Exception e)
      {
         // No FFDC code needed

         // Any exceptions we get, throw back to the caller (us) so that
         // we are aware that something went wrong
         RuntimeException r = new RuntimeException(e.getMessage(), e);
         throw r;
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setAsynchConsumerCallback");
   }

   /**
    * This method will unlock a set of locked messages that have been delivered to
    * us (the server) which we have then passed on to the client.
    *
    * @param requestNumber The request number that replies should be sent with.
    * @param msgHandles The array of message id's that should be unlocked.
    * @param reply Whether this will demand a reply.
    */
   public void unlockSet(int requestNumber, SIMessageHandle[] msgHandles, boolean reply)                  // f199593, F219476.2
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "unlockSet",
                                           new Object[]{requestNumber, msgHandles, reply});

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Request to delete " + msgHandles.length + " message(s)");

      try
      {
         getConsumerSession().unlockSet(msgHandles);

         if (reply)
         {
            try
            {
               getConversation().send(poolManager.allocate(),
                                      JFapChannelConstants.SEG_UNLOCK_SET_R,
                                      requestNumber,
                                      JFapChannelConstants.PRIORITY_MEDIUM,
                                      true,
                                      ThrottlingPolicy.BLOCK_THREAD,
                                      null);
            }
            catch (SIException e)
            {
               FFDCFilter.processException(e,
                                           CLASS_NAME + ".unlockSet",
                                           CommsConstants.CATPROXYCONSUMER_UNLOCKSET_02,
                                           this);

               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, e.getMessage(), e);

               SibTr.error(tc, "COMMUNICATION_ERROR_SICO2014", e);
            }
         }
      }
      catch (SIException e)
      {
         //No FFDC code needed
         //Only FFDC if we haven't received a meTerminated event.
         if(!((ConversationState)getConversation().getAttachment()).hasMETerminated())
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".unlockSet",
                                        CommsConstants.CATPROXYCONSUMER_UNLOCKSET_01,
                                        this);
         }

         if (reply)
         {
            StaticCATHelper.sendExceptionToClient(e,
                                                  CommsConstants.CATPROXYCONSUMER_UNLOCKSET_01,
                                                  getConversation(),
                                                  requestNumber);
         }
         else
         {
            SibTr.error(tc, "UNABLE_TO_UNLOCK_MSGS_SICO2006", e);
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "unlockSet");
   }

   /**
    * This method will unlock a set of locked messages that have been delivered to
    * us (the server) which we have then passed on to the client.
    * 
    * @param requestNumber The request number that replies should be sent with.
    * @param msgIds The array of message id's that should be unlocked.
    * @param reply Whether this will demand a reply.
    * @param incrementLockCount Indicates whether the lock count should be incremented for this unlock
    */
   public void unlockSet(int requestNumber, SIMessageHandle[] msgHandles, boolean reply, boolean incrementLockCount)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "unlockSet",
                                           new Object[]{requestNumber, msgHandles, reply, incrementLockCount});

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Request to delete " + msgHandles.length + " message(s)");

      try
      {
         getConsumerSession().unlockSet(msgHandles, incrementLockCount);

         if (reply)
         {
            try
            {
               getConversation().send(poolManager.allocate(),
                                      JFapChannelConstants.SEG_UNLOCK_SET_R,
                                      requestNumber,
                                      JFapChannelConstants.PRIORITY_MEDIUM,
                                      true,
                                      ThrottlingPolicy.BLOCK_THREAD,
                                      null);
            }
            catch (SIException e)
            {
               FFDCFilter.processException(e,
                                           CLASS_NAME + ".unlockSet",
                                           CommsConstants.CATPROXYCONSUMER_UNLOCKSET_04,
                                           this);

               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, e.getMessage(), e);

               SibTr.error(tc, "COMMUNICATION_ERROR_SICO2014", e);
            }
         }
      }
      catch (SIException e)
      {
         //No FFDC code needed
         //Only FFDC if we haven't received a meTerminated event.
         if(!((ConversationState)getConversation().getAttachment()).hasMETerminated())
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".unlockSet",
                                        CommsConstants.CATPROXYCONSUMER_UNLOCKSET_03,
                                        this);
         }

         if (reply)
         {
            StaticCATHelper.sendExceptionToClient(e,
                                                  CommsConstants.CATPROXYCONSUMER_UNLOCKSET_03,
                                                  getConversation(),
                                                  requestNumber);
         }
         else
         {
            SibTr.error(tc, "UNABLE_TO_UNLOCK_MSGS_SICO2006", e);
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "unlockSet");
   }
   
   /**
    * This method will inform the ME that we have consumed messages that are
    * currently locked on our behalf.
    *
    * @param requestNumber The request number that replies should be sent with.
    * @param msgHandles The array of message id's that should be deleted.
    * @param tran
    * @param reply
    */
   public void deleteSet(int requestNumber, SIMessageHandle[] msgHandles, int tran, boolean reply)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "deleteSet",
                                           new Object[]{requestNumber, msgHandles, tran, reply});

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
         SibTr.debug(this, tc, "Request to delete " + msgHandles.length + " message(s)");
         if (reply) SibTr.debug(this, tc, "Client is expecting a reply");
      }

      try
      {
         SITransaction siTran =
                ((ServerLinkLevelState)getConversation().getLinkLevelAttachment()).getTransactionTable().get(tran);

         if (siTran != IdToTransactionTable.INVALID_TRANSACTION)
         {
                getConsumerSession().deleteSet(msgHandles, siTran);
         }

         try
         {
            if (reply)
            {
               getConversation().send(poolManager.allocate(),
                                      JFapChannelConstants.SEG_DELETE_SET_R,
                                      requestNumber,
                                      JFapChannelConstants.PRIORITY_MEDIUM,
                                      true,
                                      ThrottlingPolicy.BLOCK_THREAD,
                                      null);
            }
         }
         catch (SIException e)
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".deleteSet",
                                        CommsConstants.CATPROXYCONSUMER_DELETESET_02,
                                        this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, e.getMessage(), e);

            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2014", e);
         }
      }
      catch (SIException e)
      {
         //No FFDC code needed
         //Only FFDC if we haven't received a meTerminated event.
         if(!((ConversationState)getConversation().getAttachment()).hasMETerminated())
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".deleteSet",
                                        CommsConstants.CATPROXYCONSUMER_DELETESET_01,
                                        this);
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, e.getMessage(), e);

         if (reply)
         {
            StaticCATHelper.sendExceptionToClient(e,
                                                  CommsConstants.CATPROXYCONSUMER_DELETESET_01,
                                                  getConversation(), requestNumber);
         }
         else
         {
            SibTr.error(tc, "UNABLE_TO_DELETE_MSGS_SICO2007", e);

            StaticCATHelper.sendAsyncExceptionToClient(e,
                                                       CommsConstants.CATPROXYCONSUMER_DELETESET_01,     // d186970
                                                       getClientSessionId(), getConversation(), 0);      // d172528
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "deleteSet");
   }

   /**
    * This method will unlock all messages that are currently locked on the
    * ME. This is usually flowed when a failure on the client has occurred
    * and the proxy queues would like to reset to a known good state.
    * <p>
    * It is possible though, that while we are processing this code to
    * perform the unlock that there may be messages already on their way
    * down to the client. The proxy will throw these messages away as they
    * will be unlocked by the server during this call, and the client does
    * not want to receive them twice.
    * <p>
    * This is acheived through the message batch number that is flown
    * with every async message. When the proxy queue issues an <code>unlockAll()</code>
    * it increments it's message batch number. We also increment our server side
    * message batch number, but only after that <code>unlockAll()</code> has
    * completed. Therefore, any messages received by the client with an 'old'
    * message batch number can be safely discarded.
    *
    * @param requestNumber The request number that replies should be sent with.
    */
   public void unlockAll(int requestNumber)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "unlockAll", requestNumber);

      try
      {
         // Stop the session to prevent any (more) messages going
         if (mainConsumer.isStarted())
         {
            getConsumerSession().stop();
            setState(State.STOPPED);
         }

         // Increment the message batch
         mainConsumer.incremenetMessageBatchNumber();

         // Now perform the actual unlockAll
         getConsumerSession().unlockAll();

         // Reset the sent bytes to zero. Do this inside a lock to prevent us updating the counter
         // at the same time as anyone else.
         setSentBytes(0);

         short jfapPriority = JFapChannelConstants.getJFAPPriority(Integer.valueOf(mainConsumer.getLowestPriority()));
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Sending with JFAP priority of " + jfapPriority);

         try
         {
            // and reply
            getConversation().send(poolManager.allocate(),
                                   JFapChannelConstants.SEG_UNLOCK_ALL_R,
                                   requestNumber,
                                   jfapPriority,
                                   true,
                                   ThrottlingPolicy.BLOCK_THREAD,
                                   null);

            // Now restart the session
            if (mainConsumer.isStarted())
            {
                getConsumerSession().start(false);
                setState(State.STARTED);
            }
         }
         catch (SIException e)
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".unlockAll",
                                        CommsConstants.CATPROXYCONSUMER_UNLOCKALL_01,
                                        this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, e.getMessage(), e);

            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2014", e);
         }
      }
      catch (SIException e)
      {
         //No FFDC code needed
         //Only FFDC if we haven't received a meTerminated event.
         if(!((ConversationState)getConversation().getAttachment()).hasMETerminated())
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".unlockAll",
                                        CommsConstants.CATPROXYCONSUMER_UNLOCKALL_02,
                                        this);
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, e.getMessage(), e);

         StaticCATHelper.sendExceptionToClient(e,
                                               CommsConstants.CATPROXYCONSUMER_UNLOCKALL_02,
                                               getConversation(), requestNumber);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "unlockAll");
   }

   /**
    * This method is used to completely make sure that there is no message available.
    * This could occur if the only messages available are extremely large ones and
    * are not able to be stored on a proxy queue. As such, this method will force at least
    * one message out, but only if there is one available.
    * <p>
    * This method will also be called as a check to ensure the server queue is definately
    * empty if the proxy queue is also empty.
    *
    * @param requestNumber
    */
   public void flush(int requestNumber)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "flush", requestNumber);

      try
      {
         // activateAsynchConsumer() can only be called when the consumer is stopped
         if (mainConsumer.isStarted()) getConsumerSession().stop();
         getConsumerSession().activateAsynchConsumer(true);
         if (mainConsumer.isStarted()) getConsumerSession().start(false);

         short jfapPriority = JFapChannelConstants.getJFAPPriority(Integer.valueOf(mainConsumer.getLowestPriority()));
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Sending with JFAP priority of " + jfapPriority);

         try
         {
            getConversation().send(poolManager.allocate(),
                                   JFapChannelConstants.SEG_FLUSH_SESS_R,
                                   requestNumber,
                                   jfapPriority,
                                   true,
                                   ThrottlingPolicy.BLOCK_THREAD,
                                   null);
         }
         catch (SIException e)
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".flush",
                                        CommsConstants.CATPROXYCONSUMER_FLUSH_01,
                                        this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, e.getMessage(), e);

            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2014", e);
         }
      }
      catch (SIException e)
      {
         //No FFDC code needed
         //Only FFDC if we haven't received a meTerminated event.
         if(!((ConversationState)getConversation().getAttachment()).hasMETerminated())
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".flush",
                                        CommsConstants.CATPROXYCONSUMER_FLUSH_02,
                                        this);
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, e.getMessage(), e);

         StaticCATHelper.sendExceptionToClient(e,
                                               CommsConstants.CATPROXYCONSUMER_FLUSH_02,
                                               getConversation(), requestNumber);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "flush");
   }

   /**
    * This method will send a message to the attached client.
    *
    * @param sibMessage
    *
    * @return Returns the length of the message sent
    *
    * @throws MessageEncodeFailedException
    * @throws IncorrectMessageTypeException
    * @throws MessageCopyFailedException
    */
   int sendMessage(SIBusMessage sibMessage)
      throws MessageCopyFailedException,
             IncorrectMessageTypeException,
             MessageEncodeFailedException,
             UnsupportedEncodingException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "sendMessage", sibMessage);

      int msgLen = 0;

      // If we are at FAP9 or above we can do a 'chunked' send of the message in seperate
      // slices to make life easier on the Java memory manager
      final HandshakeProperties props = getConversation().getHandshakeProperties();
      if (props.getFapLevel() >= JFapChannelConstants.FAP_VERSION_9)
      {
         msgLen = sendChunkedMessage(sibMessage);
      }
      else
      {
         msgLen = sendEntireMessage(sibMessage, null);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "sendMessage", msgLen);
      return msgLen;
   }

   /**
    * This method will send the message to our peer in chunks as given to us by MFP. This is much
    * easier on the Java memory manager as it doesn't require the allocation of an enormous byte
    * array.
    *
    * @param sibMessage
    *
    * @return Returns the entire length of the message just sent.
    *
    * @throws MessageCopyFailedException
    * @throws IncorrectMessageTypeException
    * @throws MessageEncodeFailedException
    * @throws UnsupportedEncodingException
    */
   private int sendChunkedMessage(SIBusMessage sibMessage)
      throws MessageCopyFailedException,
             IncorrectMessageTypeException,
             MessageEncodeFailedException,
             UnsupportedEncodingException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "sendChunkedMessage", sibMessage);

      int msgLen = 0;

      // First of all we must encode the message ourselves
      CommsServerByteBuffer buffer = poolManager.allocate();
      ConversationState convState = (ConversationState) getConversation().getAttachment();

      try
      {
         List<DataSlice> messageSlices = buffer.encodeFast((JsMessage) sibMessage,
                                                           convState.getCommsConnection(),
                                                           getConversation());

         // Do a check on the size of the message. If it is less than our threshold, forget the
         // chunking and simply send the message as one
         for (DataSlice slice : messageSlices) msgLen += slice.getLength();
         if (msgLen < CommsConstants.MINIMUM_MESSAGE_SIZE_FOR_CHUNKING)
         {
            // The message is a tiddler, send it in one
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Message is smaller than " +
                                                           CommsConstants.MINIMUM_MESSAGE_SIZE_FOR_CHUNKING);
            sendEntireMessage(sibMessage, messageSlices);
         }
         else
         {
            short jfapPriority = JFapChannelConstants.getJFAPPriority(sibMessage.getPriority());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Sending with JFAP priority of " + jfapPriority);

            // Now we have the slices, send each one in turn. Each slice contains all the header
            // information so that the client code knows what to do with the message

            for (int x = 0; x < messageSlices.size(); x++)
            {
               DataSlice slice = messageSlices.get(x);
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Sending slice:", slice);

               boolean first = (x == 0);
               boolean last = (x == (messageSlices.size() - 1));
               byte flags = 0;

               // Work out the flags to send
               if (first) flags |= CommsConstants.CHUNKED_MESSAGE_FIRST;
               if (last)  flags |= CommsConstants.CHUNKED_MESSAGE_LAST;
               else if (!first) flags |= CommsConstants.CHUNKED_MESSAGE_MIDDLE;
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Flags: " + flags);

               if (!first)
               {
                  // This isn't the first slice, grab a fresh buffer
                  buffer = poolManager.allocate();
               }

               // Now add all the header information
               buffer.putShort(convState.getConnectionObjectId());
               buffer.putShort(mainConsumer.getClientSessionId());
               buffer.putShort(mainConsumer.getMessageBatchNumber());  // BIT16 Message batch
               buffer.put(flags);
               buffer.putDataSlice(slice);

               getConversation().send(buffer,
                                      JFapChannelConstants.SEG_CHUNKED_PROXY_MESSAGE,
                                      0,                                         // No request number
                                      jfapPriority,
                                      false,
                                      ThrottlingPolicy.BLOCK_THREAD,
                                      INVALIDATE_CONNECTION_ON_ERROR);
            }

            messagesSent++;
         }
      }
      catch (SIException e)
      {
         FFDCFilter.processException(e,
                                     CLASS_NAME + ".sendChunkedMessage",
                                     CommsConstants.CATPROXYCONSUMER_SEND_CHUNKED_MSG_01,
                                     this);

         SibTr.error(tc, "COMMUNICATION_ERROR_SICO2014", e);
         msgLen = 0;
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "sendChunkedMessage", msgLen);
      return msgLen;
   }

   /**
    * Send the entire message in one big buffer. If the messageSlices parameter is not null then
    * the message has already been encoded and does not need to be done again. This may be in the
    * case where the message was destined to be sent in chunks but is so small that it does not
    * seem worth it.
    *
    * @param sibMessage The entire message to send.
    * @param messageSlices The already encoded message slices.
    *
    * @return Returns the length of the message sent to the client.
    *
    * @throws MessageCopyFailedException
    * @throws IncorrectMessageTypeException
    * @throws MessageEncodeFailedException
    * @throws UnsupportedEncodingException
    */
   private int sendEntireMessage(SIBusMessage sibMessage, List<DataSlice> messageSlices)
      throws MessageCopyFailedException,
             IncorrectMessageTypeException,
             MessageEncodeFailedException,
             UnsupportedEncodingException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "sendEntireMessage",
                                                                                   new Object[]{sibMessage, messageSlices});

      int msgLen = 0;

      try
      {
         CommsServerByteBuffer buffer = poolManager.allocate();

         ConversationState convState = (ConversationState) getConversation().getAttachment();
         buffer.putShort(convState.getConnectionObjectId());
         buffer.putShort(mainConsumer.getClientSessionId());
         buffer.putShort(mainConsumer.getMessageBatchNumber());  // BIT16 Message batch
         // Put the entire message into the buffer in whatever way is suitable
         if (messageSlices == null)
         {
            msgLen = buffer.putMessage((JsMessage) sibMessage,
                                       convState.getCommsConnection(),
                                       getConversation());
         }
         else
         {
            msgLen = buffer.putMessgeWithoutEncode(messageSlices);
         }

         short jfapPriority = JFapChannelConstants.getJFAPPriority(sibMessage.getPriority());
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Sending with JFAP priority of " + jfapPriority);

         getConversation().send(buffer,
                                JFapChannelConstants.SEG_PROXY_MESSAGE,
                                0,                                         // No request number
                                jfapPriority,
                                false,
                                ThrottlingPolicy.BLOCK_THREAD,
                                INVALIDATE_CONNECTION_ON_ERROR);

         messagesSent++;
      }
      catch (SIException e1)
      {
         FFDCFilter.processException(e1,
                                     CLASS_NAME + ".sendEntireMessage",
                                     CommsConstants.CATPROXYCONSUMER_SEND_MSG_01,
                                     this);

         SibTr.error(tc, "COMMUNICATION_ERROR_SICO2014", e1);
         msgLen = 0;
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "sendEntireMessage", msgLen);
      return msgLen;
   }

   /**
    * This method is called when the client requests for more messages
    * to be sent to them. In this case they will inform us how many bytes they
    * have given to the client (receivedBytes) and the maximum amount of bytes they
    * want to keep in the proxy queue (requestedBytes).
    * <p>
    * At this point, we will update all our counters. The amount of bytes that
    * we have sent to the client will be reduced by the amount of bytes that the
    * client tell us they have dished out to the client. We can also update
    * the amount of bytes that the client requests. In practise it is unlikely that
    * this value will change. However, it could :-).
    * <p>
    * If at this point we then find that the amount of bytes we have sent is less
    * than the amount of bytes the client has requested, we will start the session.
    * If it is greater, then we will stop the session.
    *
    * @param requestNumber
    * @param receivedBytes The amount of bytes the proxy queue has given to the client app.
    * @param reqBytes The maximum amount of bytes the client wants to keep in the proxy queue.
    */
   public void requestMsgs(int requestNumber, int receivedBytes, int reqBytes)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "requestMsgs",
                                                                                   new Object[]
                                                                                   {
                                                                                      ""+requestNumber,
                                                                                      ""+receivedBytes,
                                                                                      ""+reqBytes
                                                                                   });

      try
      {
         // Update the counters
         // Make sure nobody else can update either the counters or the consumer state while we're doing this
         // Once we've decided what to do, we can release the locks (which does mean the state might get updated
         // elsewhere while we're still acting on our decision).
         int sent = 0;
         boolean startSession = false;
         boolean stopSession = false;
         stateLock.lock();
         try
         {
            while (state.isTransitioning()) stateTransition.await();

            setSentBytes(getSentBytes() - receivedBytes);
            setRequestedBytes(reqBytes);

            sent = getSentBytes();

            // Should we start or stop the consumerSession based on the new counter values?
            if (sent < reqBytes)
            {
               // We've not retrieved enough bytes, we might need to (re)start the session.
               if (!state.isStarted())
               {
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                  {
                     SibTr.debug(this
                                ,tc
                                ,String.format("[@%x] Starting the session (sentBytes (%d) < requestedBytes (%d) && !started)"
                                              ,this.hashCode()
                                              ,sent
                                              ,reqBytes
                                              )
                                );
                  }
                  this.setState(State.STARTING);
                  startSession = true;
               }
               else
               {
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                  {
                     SibTr.debug(this, tc, String.format("[@%x] Already started", this.hashCode()));
                  }
               }
            }
            else // (sent >= reqBytes)
            {
               if (!state.isStopped())
               {
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                  {
                     SibTr.debug(this
                                ,tc
                                ,String.format("[@%x] Stopping the session (sentBytes (%d) >= requestedBytes (%d))"
                                              ,this.hashCode()
                                              ,sent
                                              ,reqBytes
                                              )
                                );
                  }
                  this.setState(State.STOPPING);
                  stopSession = true;
               }
               else
               {
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                  {
                     SibTr.debug(this, tc, String.format("[@%x] Already stopped", this.hashCode()));
                  }
               }
            }
         }
         finally
         {
            stateLock.unlock();
         }

         if (startSession)
         {
            try
            {
               getConsumerSession().start(false);
               setState(State.STARTED);
            }
            catch(Exception e)
            {
               // We failed to start the consumerSession. To be safe, we should reset the state variable so we can try again later
               setState(State.STOPPED);
               throw e;
            }
         }
         if (stopSession)
         {
            try
            {
               setState(State.STOPPING); // try always transition states cleanly
               getConsumerSession().stop();
               setState(State.STOPPED);
            }
            catch(Exception e)
            {
               // Well, this is interesting. We attempted to stop the consumerSession, but failed. So, what state should we assume it's now in?
               // Let's, in absence of any further evidence, assume that it's effectively stopped, even if that di dn't happen cleanly
               setState(State.STOPPED);
               throw e;
            }
         }
      }
      catch (Exception e) //(SIException e)
      {
         //No FFDC code needed
         //Only FFDC if we haven't received a meTerminated event.
         if(!((ConversationState)getConversation().getAttachment()).hasMETerminated())
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".requestMsgs",
                                        CommsConstants.CATPROXYCONSUMER_REQUEST_MSGS_01,
                                        this);
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, e.getMessage(), e);

         StaticCATHelper.sendAsyncExceptionToClient(e,
                                                    CommsConstants.CATPROXYCONSUMER_REQUEST_MSGS_01,  // d186970
                                                    getClientSessionId(), getConversation(), 0);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "requestMsgs");
   }

   /**
    * @return Returns the amount of bytes that the client
    * has requested to keep in the proxy queue.
    */
   public int getRequestedBytes()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getRequestedBytes");
      // state lock for byte count changes as state transitions are based on these values 
      stateLock.lock(); //PH20984
      try
      {
         return requestedBytes;
      }
      finally
      {
         stateLock.unlock(); // PH20984
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getRequestedBytes", requestedBytes);
      }
   }

   /**
    * This method can be used to set the amount of bytes that the client
    * has requested to keep in the proxy queue.
    *
    * @param newRequestedBytes
    */
   public void setRequestedBytes(int newRequestedBytes)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setRequestedBytes", newRequestedBytes);
      // state lock for byte count changes as state transitions are based on these values 
      stateLock.lock(); // PH20984
      try
      {
         requestedBytes = newRequestedBytes;
      }
      finally
      {
         stateLock.unlock(); // PH20984
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setRequestedBytes");
      return;
   }

   /**
    * @return Returns the amounts of bytes we have sent to the client.
    */
   public int getSentBytes()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getSentBytes");
      // state lock for byte count changes as state transitions are based on these values 
      stateLock.lock(); // PH20984
      try
      {
         return sentBytes;
      }
      finally
      {
         stateLock.unlock(); // PH20984
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getSentBytes", sentBytes);
      }
   }

   /**
    * This method will update the amount of bytes we have sent to the
    * client.
    *
    * @param newSentBytes
    */
   public void setSentBytes(int newSentBytes)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setSentBytes", newSentBytes);
      // state lock for byte count changes as state transitions are based on these values 
      stateLock.lock(); // PH20984
      try
      {
         sentBytes = newSentBytes;
      }
      finally
      {
         stateLock.unlock(); // PH20984
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setSentBytes");
   }

   /**
    * PH20984
    * Performs the counter update required by CATAsynchReadAheadReader.consumeMessages()
    * Once a message has been sent to the client, the sentBytes counter must be updated, and if this means that the requested
    * number of bytes have been sent the CATAsynchReadAheadReader needs to stop the consumer.
    * By performing the update and check in a single method here, we can more easily lock around the behaviour and make sure
    * that no other thread alters the counters while we're doing it.
    *
    * @param msgLen the length of the last message sent
    * @return whether the total number of sent bytes is at least equal to the number of bytes currently requested
    */
   public boolean updateConsumedBytes(int msgLen)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "updateConsumedBytes", msgLen);
      // state lock for byte count changes as state transitions are based on these values 
      stateLock.lock();
      boolean stopConsumer = false;
      try
      {
         sentBytes += msgLen;
         stopConsumer = (sentBytes >= requestedBytes);
         return stopConsumer;
      }
      finally
      {
         stateLock.unlock();
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "updateConsumedBytes", stopConsumer);
      }
   }


   /**
    * This method will update the session lowest priority value.
    *
    * @param pri
    */
   public void setLowestPriority(short pri)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setLowestPriority", pri);
      mainConsumer.setLowestPriority(pri);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setLowestPriority");
   }
   
   /**
    * This method will unlock all messages that are currently locked on the
    * ME. This is usually flowed when a unlockAll(incrementUnlockCount) is called from client 
    * and the proxy queues would like to reset to a known good state.
    * <p>
    * It is possible though, that while we are processing this code to
    * perform the unlock that there may be messages already on their way
    * down to the client. The proxy will throw these messages away as they
    * will be unlocked by the server during this call, and the client does
    * not want to receive them twice.
    * <p>
    * This is acheived through the message batch number that is flown
    * with every async message. When the proxy queue issues an <code>unlockAll()</code>
    * it increments it's message batch number. We also increment our server side
    * message batch number, but only after that <code>unlockAll()</code> has
    * completed. Therefore, any messages received by the client with an 'old'
    * message batch number can be safely discarded.
    *
    * @param requestNumber The request number that replies should be sent with.
    * @param incrementUnlockCount Option to increment the unlock count or  not on unlock of messages
    */
   public void unlockAll(int requestNumber, boolean incrementUnlockCount)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "unlockAll", new Object[] {requestNumber,incrementUnlockCount});

      try
      {
         // Stop the session to prevent any (more) messages going
         if (mainConsumer.isStarted())
         {
            setState(State.STOPPING); // try always transition states cleanly
            getConsumerSession().stop();
            setState(State.STOPPED);
         }

         // Increment the message batch
         mainConsumer.incremenetMessageBatchNumber();

         // Now perform the actual unlockAll
         getConsumerSession().unlockAll(incrementUnlockCount);

         // Reset the sent bytes to zero. Do this inside a lock to prevent us updating the counter
         // at the same time as anyone else.
         setSentBytes(0);

         short jfapPriority = JFapChannelConstants.getJFAPPriority(Integer.valueOf(mainConsumer.getLowestPriority()));
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Sending with JFAP priority of " + jfapPriority);

         try
         {
            // and reply
            getConversation().send(poolManager.allocate(),
                                   JFapChannelConstants.SEG_UNLOCK_ALL_NO_INC_LOCK_COUNT_R,
                                   requestNumber,
                                   jfapPriority,
                                   true,
                                   ThrottlingPolicy.BLOCK_THREAD,
                                   null);

            // Now restart the session
            if (mainConsumer.isStarted())
            {
                setState(State.STARTING); // try always transition states cleanly
                getConsumerSession().start(false);
                setState(State.STARTED);
            }
         }
         catch (SIException e)
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".unlockAll",
                                        CommsConstants.CATPROXYCONSUMER_UNLOCKALL_03,
                                        this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, e.getMessage(), e);

            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2014", e);
         }
      }
      catch (SIException e)
      {
         //No FFDC code needed
         //Only FFDC if we haven't received a meTerminated event.
         if(!((ConversationState)getConversation().getAttachment()).hasMETerminated())
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".unlockAll",
                                        CommsConstants.CATPROXYCONSUMER_UNLOCKALL_04,
                                        this);
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, e.getMessage(), e);

         StaticCATHelper.sendExceptionToClient(e,
                                               CommsConstants.CATPROXYCONSUMER_UNLOCKALL_04,
                                               getConversation(), requestNumber);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "unlockAll");
   }

}
