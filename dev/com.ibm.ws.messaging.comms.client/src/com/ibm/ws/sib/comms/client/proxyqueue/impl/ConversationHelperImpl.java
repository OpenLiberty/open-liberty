/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.client.proxyqueue.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConnection;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.client.ClientConversationState;
import com.ibm.ws.sib.comms.client.ClientJFapCommunicator;
import com.ibm.ws.sib.comms.client.OrderingContextProxy;
import com.ibm.ws.sib.comms.common.CommsByteBuffer;
import com.ibm.ws.sib.comms.common.CommsLightTrace;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.HandshakeProperties;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.Conversation.ThrottlingPolicy;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.AsynchConsumerCallback;
import com.ibm.wsspi.sib.core.OrderingContext;
import com.ibm.wsspi.sib.core.SIMessageHandle;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SIMessageNotLockedException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;

/**
 * Implementation of the conversation helper interface.
 * Communicates requests via a conversation to an ME.
 */
public class ConversationHelperImpl extends ClientJFapCommunicator implements ConversationHelper
{
   /** Class name for FFDC's */
   private static String CLASS_NAME = ConversationHelperImpl.class.getName();

   // The conversation to send (or exchange) our requests using.
   private Conversation conversation = null;

   // The ID of the session object we are conversing on behalf of.
   private short sessionId = 0;

   // The ID of the connection object we are conversing on behalf of.
   private int connectionObjectId = 0;

   // The ID of the proxy queue we servicing.
   private short proxyQueueId = (short) 0;

   private static final TraceComponent tc =
         SibTr.register(
            ConversationHelper.class,
            CommsConstants.MSG_GROUP,
            CommsConstants.MSG_BUNDLE);

   /** NLS handle */
   private static final TraceNLS nls = TraceNLS.getTraceNLS(CommsConstants.MSG_BUNDLE);

   static {
      // Trace the class information
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc, "Source info: @(#)SIB/ws/code/sib.comms.client.impl/src/com/ibm/ws/sib/comms/client/proxyqueue/impl/ConversationHelperImpl.java, SIB.comms, WASX.SIB, uu1215.01 1.58");
   }

   /**
    * Creates a new instance.
    * @param conversation The conversation we will communicate over.
    * @param proxyQueueId The proxy queue ID for the proxy queue we
    * will be sending data on behalf of.
    */
   public ConversationHelperImpl(Conversation conversation, short proxyQueueId)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", new Object[]{conversation, proxyQueueId});

      this.conversation = conversation;
      this.proxyQueueId = proxyQueueId;

      setConversation(conversation);

      // Get the connection object ID
      ClientConversationState state = (ClientConversationState) conversation.getAttachment();
      connectionObjectId = state.getConnectionObjectID();

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }

   /**
    * Sends a request to unset the asynchronous consumer.
    */
   public void unsetAsynchConsumer(boolean stoppable)                                                   //SIB0115d.comms
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIErrorException,
             SIIncorrectCallException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "unsetAsynchConsumer");

      if (sessionId == 0)
      {
         // If the session Id = 0, then no one called setSessionId(). As such we are unable to flow
         // to the server as we do not know which session to instruct the server to use.
         SIErrorException e = new SIErrorException(
            nls.getFormattedMessage("SESSION_ID_HAS_NOT_BEEN_SET_SICO1043", null, null)
         );

         FFDCFilter.processException(e, CLASS_NAME + ".unsetAsyncConsumer",
                                     CommsConstants.CONVERSATIONHELPERIMPL_01, this);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, e.getMessage(), e);

         throw e;
      }

      CommsByteBuffer request = getCommsByteBuffer();
      // Connection object id
      request.putShort(connectionObjectId);
      // Consumer session id
      request.putShort(sessionId);

      // Pass on call to server
      CommsByteBuffer reply = null;
      try
      {
         reply = jfapExchange(request,
                              (stoppable ? JFapChannelConstants.SEG_DEREGISTER_STOPPABLE_ASYNC_CONSUMER : JFapChannelConstants.SEG_DEREGISTER_ASYNC_CONSUMER), //SIB0115d.comms
                              JFapChannelConstants.PRIORITY_MEDIUM,
                              true);
      }
      catch (SIConnectionLostException e)
      {
         // No FFDC Code needed
         // Converting this to a connection dropped as that is all we can throw
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Connection was lost", e);

         throw new SIConnectionDroppedException(e.getMessage(), e);
      }

      // Confirm appropriate data returned
      try
      {
         short err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_DEREGISTER_ASYNC_CONSUMER_R);
         if (err != CommsConstants.SI_NO_EXCEPTION)
         {
            checkFor_SISessionUnavailableException(reply, err);
            checkFor_SISessionDroppedException(reply, err);
            checkFor_SIConnectionUnavailableException(reply, err);
            checkFor_SIConnectionDroppedException(reply, err);
            checkFor_SIIncorrectCallException(reply, err);
            checkFor_SIErrorException(reply, err);
            defaultChecker(reply, err);
         }
      }
      finally
      {
         if (reply != null) reply.release();
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "unsetAsynchConsumer");
   }

   /**
    * Sends a request to set the asynchronous consumer.
    *
    * @param consumer
    * @param maxActiveMessages
    * @param messageLockExpiry
    * @param maxBatchSize
    * @param orderContext
    * @param maxSequentialFailures
    * @param hiddenMessageDelay
    */
   public void setAsynchConsumer(AsynchConsumerCallback consumer,
                                 int maxActiveMessages,
                                 long messageLockExpiry,
                                 int maxBatchSize,
                                 OrderingContext orderContext,
                                 int maxSequentialFailures,                                             //SIB0115d.comms
                                 long hiddenMessageDelay,
                                 boolean stoppable)                                                             //472879
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIErrorException,
             SIIncorrectCallException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setAsynchConsumer",
                                           new Object[]
                                           {
                                              consumer,
                                              maxActiveMessages,
                                              messageLockExpiry,
                                              maxBatchSize,
                                              orderContext,
                                              maxSequentialFailures,                                    //SIB0115d.comms
                                              hiddenMessageDelay,
                                              stoppable                                                         //472879
                                           });

      if (sessionId == 0)
      {
         // If the session Id = 0, then no one called setSessionId(). As such we are unable to flow
         // to the server as we do not know which session to instruct the server to use.
         SIErrorException e = new SIErrorException(
            nls.getFormattedMessage("SESSION_ID_HAS_NOT_BEEN_SET_SICO1043", null, null)
         );

         FFDCFilter.processException(e, CLASS_NAME + ".setAsyncConsumer",
                                     CommsConstants.CONVERSATIONHELPERIMPL_02, this);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Session Id was 0", e);

         throw e;
      }

      CommsByteBuffer request = getCommsByteBuffer();
      // Connection object id
      request.putShort(connectionObjectId);
      // Consumer session id
      request.putShort(sessionId);
      // Now put the message order context id if we have one
      if (orderContext != null)
      {
         request.putShort(((OrderingContextProxy)orderContext).getId());
      }
      else
      {
         request.putShort(CommsConstants.NO_ORDER_CONTEXT);
      }
      // Client session id - this is the proxy queue ID
      request.putShort(proxyQueueId);
      // Max active messages
      request.putInt(maxActiveMessages);
      // Message lock expiry
      request.putLong(messageLockExpiry);
      // Max batch size
      request.putInt(maxBatchSize);

      // If callback is Stoppable then send maxSequentialFailures & hiddenMessageDelay then change the
      // Segment Id to Stoppable  SIB0115d.comms
      int JFapSegmentId = JFapChannelConstants.SEG_REGISTER_ASYNC_CONSUMER;                             //SIB0115d.comms

      if (stoppable) {                                                                           //SIB0115d.comms,472879
        request.putInt(maxSequentialFailures);                                                          //SIB0115d.comms
        request.putLong(hiddenMessageDelay);
        JFapSegmentId = JFapChannelConstants.SEG_REGISTER_STOPPABLE_ASYNC_CONSUMER;                     //SIB0115d.comms
      }                                                                                                 //SIB0115d.comms

      CommsByteBuffer reply = null;
      try
      {
         // Pass on call to server
         reply = jfapExchange(request,
                              JFapSegmentId,                                                            //SIB0115d.comms
                              JFapChannelConstants.PRIORITY_MEDIUM,
                              true);
      }
      catch (SIConnectionLostException e)
      {
         // No FFDC Code needed
         // Converting this to a connection dropped as that is all we can throw
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Connection was lost", e);

         throw new SIConnectionDroppedException(e.getMessage(), e);
      }

      // Confirm appropriate data returned
      try
      {
         short err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_REGISTER_ASYNC_CONSUMER_R);
         if (err != CommsConstants.SI_NO_EXCEPTION)
         {
            checkFor_SISessionUnavailableException(reply, err);
            checkFor_SISessionDroppedException(reply, err);
            checkFor_SIConnectionUnavailableException(reply, err);
            checkFor_SIConnectionDroppedException(reply, err);
            checkFor_SIIncorrectCallException(reply, err);
            checkFor_SIErrorException(reply, err);
            defaultChecker(reply, err);
         }
      }
      finally
      {
         if (reply != null) reply.release();
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setAsynchConsumer");
   }

   /**
    * Sends a request to start the session.
    */
   public void sendStart() throws SIConnectionDroppedException, SIConnectionLostException {                     //471642
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "start");
     sendStart(false);            // Always use proper start
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "start");
   }                                                                                                            //471642

   /**
    * Sends a request to start the session.
    *
    * @param restart indictaes if this is an 'internal' restart (to get more async messages) or an application start
    */
   public void sendStart(boolean restart) throws SIConnectionDroppedException, SIConnectionLostException        //471642
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "sendStart","restart="+restart);
      if (sessionId == 0)
      {
         // If the session Id = 0, then no one called setSessionId(). As such we are unable to flow
         // to the server as we do not know which session to instruct the server to use.
         SIErrorException e = new SIErrorException(
            nls.getFormattedMessage("SESSION_ID_HAS_NOT_BEEN_SET_SICO1043", null, null)
         );

         FFDCFilter.processException(e, CLASS_NAME + ".sendStart",
                                     CommsConstants.CONVERSATIONHELPERIMPL_03, this);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, e.getMessage(), e);

         throw e;
      }

      CommsByteBuffer request = getCommsByteBuffer();
      // Connection object id
      request.putShort((short) connectionObjectId);
      // Consumer session id
      request.putShort(sessionId);

      final HandshakeProperties handshakeProps = conversation.getHandshakeProperties();
      final short fapVersion = handshakeProps.getFapLevel();

      // If a restart was requested and the conversation FAP version is >= 9 then send a restart segmentId so that
      // the other end knows that it should check the consumer session state is not stopped before processing the
      // request. When a stoppable async consumer callback is in use it is possible for message processor to stop
      // the consumer session in the server but for the notification not to reach the client until after the client
      // has requested the next batch of messages. By using a restart segmentId the client can tell the comms server
      // end to check the session state before processing the request. If it turns out that message processor has
      // stopped the consumer session then the start request is ignored.
      jfapSend(request,                                                                                         //471642
               (restart && fapVersion >= JFapChannelConstants.FAP_VERSION_9) ? JFapChannelConstants.SEG_RESTART_SESS : JFapChannelConstants.SEG_START_SESS,
               JFapChannelConstants.PRIORITY_MEDIUM,
               true,
               ThrottlingPolicy.BLOCK_THREAD);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "sendStart");
   }                                                                                                            //471642

   /**
    * Sends a request to stop the session.
    */
   public void exchangeStop()
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException,
             SIErrorException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "exchangeStop");

      if (sessionId == 0)
      {
         // If the session Id = 0, then no one called setSessionId(). As such we are unable to flow
         // to the server as we do not know which session to instruct the server to use.
         SIErrorException e = new SIErrorException(
            nls.getFormattedMessage("SESSION_ID_HAS_NOT_BEEN_SET_SICO1043", null, null)
         );

         FFDCFilter.processException(e, CLASS_NAME + ".exchangeStop",
                                     CommsConstants.CONVERSATIONHELPERIMPL_04, this);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, e.getMessage(), e);

         throw e;
      }

      CommsByteBuffer request = getCommsByteBuffer();
      // Connection object id
      request.putShort(connectionObjectId);
      // Consumer session id
      request.putShort(sessionId);

      // Pass on call to server
      final CommsByteBuffer reply = jfapExchange(request,
                                                 JFapChannelConstants.SEG_STOP_SESS,
                                                 JFapChannelConstants.PRIORITY_MEDIUM,
                                                 true);

      // Confirm appropriate data returned
      try
      {
         short err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_STOP_SESS_R);
         if (err != CommsConstants.SI_NO_EXCEPTION)
         {
            checkFor_SISessionUnavailableException(reply, err);
            checkFor_SISessionDroppedException(reply, err);
            checkFor_SIConnectionUnavailableException(reply, err);
            checkFor_SIConnectionDroppedException(reply, err);
            checkFor_SIConnectionLostException(reply, err);
            checkFor_SIResourceException(reply, err);
            checkFor_SIErrorException(reply, err);
            defaultChecker(reply, err);
         }
      }
      finally
      {
         if (reply != null) reply.release();
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "exchangeStop");
   }

   /**
    * Sends an unlockAll request.
    */
   public void unlockAll()
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException,
             SIErrorException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "unlockAll");

      if (sessionId == 0)
      {
         // If the session Id = 0, then no one called setSessionId(). As such we are unable to flow
         // to the server as we do not know which session to instruct the server to use.
         SIErrorException e = new SIErrorException(
            nls.getFormattedMessage("SESSION_ID_HAS_NOT_BEEN_SET_SICO1043", null, null)
         );

         FFDCFilter.processException(e, CLASS_NAME + ".unlockAll",
                                     CommsConstants.CONVERSATIONHELPERIMPL_05, this);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, e.getMessage(), e);

         throw e;
      }

      CommsByteBuffer request = getCommsByteBuffer();
      // Connection object id
      request.putShort(connectionObjectId);
      // Consumer session id
      request.putShort(sessionId);

      // Pass on call to server
      final CommsByteBuffer reply = jfapExchange(request,
                                                 JFapChannelConstants.SEG_UNLOCK_ALL,
                                                 JFapChannelConstants.PRIORITY_MEDIUM,
                                                 true);

      // Confirm appropriate data returned
      try
      {
         short err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_UNLOCK_ALL_R);
         if (err != CommsConstants.SI_NO_EXCEPTION)
         {
            checkFor_SISessionUnavailableException(reply, err);
            checkFor_SISessionDroppedException(reply, err);
            checkFor_SIConnectionUnavailableException(reply, err);
            checkFor_SIConnectionDroppedException(reply, err);
            checkFor_SIResourceException(reply, err);
            checkFor_SIErrorException(reply, err);
            defaultChecker(reply, err);
         }
      }
      finally
      {
         if (reply != null) reply.release();
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "unlockAll");
   }

   /**
    * Closes the current session.
    */
   public void closeSession()
      throws SIResourceException, SIConnectionLostException,
             SIErrorException, SIConnectionDroppedException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "closeSession");

      if (sessionId == 0)
      {
         // If the session Id = 0, then no one called setSessionId(). As such we are unable to flow
         // to the server as we do not know which session to instruct the server to use.
         SIErrorException e = new SIErrorException(
            nls.getFormattedMessage("SESSION_ID_HAS_NOT_BEEN_SET_SICO1043", null, null)
         );

         FFDCFilter.processException(e, CLASS_NAME + ".closeSession",
                                     CommsConstants.CONVERSATIONHELPERIMPL_06, this);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, e.getMessage(), e);

         throw e;
      }

      CommsByteBuffer request = getCommsByteBuffer();
      // Connection object id
      request.putShort(connectionObjectId);
      // Consumer session id
      request.putShort(sessionId);

      // Pass on call to server
      final CommsByteBuffer reply = jfapExchange(request,
                                                 JFapChannelConstants.SEG_CLOSE_CONSUMER_SESS,
                                                 JFapChannelConstants.PRIORITY_MEDIUM,
                                                 true);

      // Confirm appropriate data returned
      try
      {
         short err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_CLOSE_CONSUMER_SESS_R);
         if (err != CommsConstants.SI_NO_EXCEPTION)
         {
            checkFor_SIConnectionDroppedException(reply, err);
            checkFor_SIConnectionLostException(reply, err);
            checkFor_SIResourceException(reply, err);
            checkFor_SIErrorException(reply, err);
            defaultChecker(reply, err);
         }
      }
      finally
      {
         if (reply != null) reply.release();
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "closeSession");
   }

   /**
    * Requests more messages (read ahead)
    *
    * @param receivedBytes
    * @param requestedBytes
    */
   public void requestMoreMessages(int receivedBytes, int requestedBytes)
      throws SIConnectionDroppedException, SIConnectionLostException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "requestMoreMessages",
                                           new Object[]{""+receivedBytes, ""+requestedBytes});

      if (sessionId == 0)
      {
         // If the session Id = 0, then no one called setSessionId(). As such we are unable to flow
         // to the server as we do not know which session to instruct the server to use.
         SIErrorException e = new SIErrorException(
            nls.getFormattedMessage("SESSION_ID_HAS_NOT_BEEN_SET_SICO1043", null, null)
         );

         FFDCFilter.processException(e, CLASS_NAME + ".requestMoreMessages",
                                     CommsConstants.CONVERSATIONHELPERIMPL_07, this);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, e.getMessage(), e);

         throw e;
      }

      CommsByteBuffer request = getCommsByteBuffer();
      // Connection object id
      request.putShort(connectionObjectId);
      // Consumer session id
      request.putShort(sessionId);
      // Received bytes
      request.putInt(receivedBytes);
      // Requested bytes
      request.putInt(requestedBytes);

      // Pass on call to server
      jfapSend(request,
               JFapChannelConstants.SEG_REQUEST_MSGS,
               JFapChannelConstants.PRIORITY_MEDIUM,
               true,
               ThrottlingPolicy.BLOCK_THREAD);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "requestMoreMessages");
   }

   /**
    * Deletes a set of messages based on their IDs in the scope
    * of a specific transaction.
    *
    * @param msgIDs
    * @param tran
    * @param priority
    */
   public void deleteMessages(SIMessageHandle[] msgHandles,
                              SITransaction tran,
                              int priority)
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException, SILimitExceededException,
             SIIncorrectCallException, SIMessageNotLockedException,
             SIErrorException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "deleteMessages", new Object[]{msgHandles, tran, priority});

      if (TraceComponent.isAnyTracingEnabled()) {
        CommsLightTrace.traceMessageIds(tc, "DeleteMsgTrace", msgHandles);
      }

      if (sessionId == 0)
      {
         // If the session Id = 0, then no one called setSessionId(). As such we are unable to flow
         // to the server as we do not know which session to instruct the server to use.
         SIErrorException e = new SIErrorException(
            nls.getFormattedMessage("SESSION_ID_HAS_NOT_BEEN_SET_SICO1043", null, null)
         );

         FFDCFilter.processException(e, CLASS_NAME + ".deleteMessages",
                                     CommsConstants.CONVERSATIONHELPERIMPL_08, this);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, e.getMessage(), e);

         throw e;
      }

      if (msgHandles == null)
      {
         // Some null message id's are no good to us
         SIErrorException e = new SIErrorException(
            nls.getFormattedMessage("NULL_MESSAGE_IDS_PASSED_IN_SICO1044", null, null)
         );

         FFDCFilter.processException(e, CLASS_NAME + ".deleteMessages",
                                     CommsConstants.CONVERSATIONHELPERIMPL_09, this);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, e.getMessage(), e);

         throw e;
      }

      CommsByteBuffer request = getCommsByteBuffer();
      // Connection object id
      request.putShort(connectionObjectId);
      // Consumer session id
      request.putShort(sessionId);
      // Transaction id
      request.putSITransaction(tran);
      // Number of msgIds sent
      request.putSIMessageHandles(msgHandles);

      // Pass on call to server - note that if we are transacted we only fire and forget.
      // If not, we exchange.
      if (tran == null)
      {
         final CommsByteBuffer reply = jfapExchange(request,
                                                    JFapChannelConstants.SEG_DELETE_SET,
                                                    priority,
                                                    true);

         // Confirm appropriate data returned
         try
         {
            short err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_DELETE_SET_R);
            if (err != CommsConstants.SI_NO_EXCEPTION)
            {
               checkFor_SISessionUnavailableException(reply, err);
               checkFor_SISessionDroppedException(reply, err);
               checkFor_SIConnectionUnavailableException(reply, err);
               checkFor_SIConnectionDroppedException(reply, err);
               checkFor_SIConnectionLostException(reply, err);
               checkFor_SIResourceException(reply, err);
               checkFor_SILimitExceededException(reply, err);
               checkFor_SIIncorrectCallException(reply, err);
               checkFor_SIMessageNotLockedException(reply, err);
               checkFor_SIErrorException(reply, err);
               defaultChecker(reply, err);
            }
         }
         finally
         {
            if (reply != null) reply.release();
         }
      }
      else
      {
         jfapSend(request,
                  JFapChannelConstants.SEG_DELETE_SET_NOREPLY,
                  priority,
                  true,
                  ThrottlingPolicy.BLOCK_THREAD);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "deleteMessages");
   }

   /**
    * Requests the remote consumer callback is flushed.
    */
   public void flushConsumer()
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException,
             SIErrorException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "flushConsumer");

      if (sessionId == 0)
      {
         // If the session Id = 0, then no one called setSessionId(). As such we are unable to flow
         // to the server as we do not know which session to instruct the server to use.
         SIErrorException e = new SIErrorException(
            nls.getFormattedMessage("SESSION_ID_HAS_NOT_BEEN_SET_SICO1043", null, null)
         );

         FFDCFilter.processException(e, CLASS_NAME + ".flushConsumer",
                                     CommsConstants.CONVERSATIONHELPERIMPL_10, this);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, e.getMessage(), e);

         throw e;
      }

      CommsByteBuffer request = getCommsByteBuffer();
      // Connection object id
      request.putShort(connectionObjectId);
      // Consumer session id
      request.putShort(sessionId);

      // Pass on call to server
      final CommsByteBuffer reply = jfapExchange(request,
                                                 JFapChannelConstants.SEG_FLUSH_SESS,
                                                 JFapChannelConstants.PRIORITY_MEDIUM,
                                                 true);

      try
      {
         short err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_FLUSH_SESS_R);
         if (err != CommsConstants.SI_NO_EXCEPTION)
         {
            checkFor_SISessionUnavailableException(reply, err);
            checkFor_SISessionDroppedException(reply, err);
            checkFor_SIConnectionUnavailableException(reply, err);
            checkFor_SIConnectionDroppedException(reply, err);
            checkFor_SIConnectionLostException(reply, err);
            checkFor_SIResourceException(reply, err);
            checkFor_SIErrorException(reply, err);
            defaultChecker(reply, err);
         }
      }
      finally
      {
         if (reply != null) reply.release();
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "flushConsumer");
   }


   /**
    * This method will set the ID of the session that we will
    * flow to the server to identify us.
    *
    * This method can only be called once.
    *
    * @param sessionId The session ID.
    */
   public void setSessionId(short sessionId)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setSessionId", ""+sessionId);
      if (this.sessionId == 0 && sessionId != 0)
      {
         this.sessionId = sessionId;
      }
      else
      {
         // If the session Id is being set twice this is badness. The conversation helper is
         // associated one to one to a proxy queue, that in turn is associated one to one with a
         // consumer session. They aren't re-used, so calling this twice indicates some bug.
         SIErrorException e = new SIErrorException(
            nls.getFormattedMessage("SESSION_ID_HAS_ALREADY_BEEN_SET_SICO1045", null, null)
         );

         FFDCFilter.processException(e, CLASS_NAME + ".setSessionId",
                                     CommsConstants.CONVERSATIONHELPERIMPL_11, this);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, e.getMessage(), e);

         throw e;
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setSessionId");
   }

   /**
    * Unlocks a set of messages (based on messaged ID).
    *
    * @param msgIds
    *
    * @see com.ibm.ws.sib.comms.client.proxyqueue.impl.ConversationHelper#unlockSet(Long[])
    */
   public void unlockSet(SIMessageHandle[] msgHandles)
      throws SIIncorrectCallException, SIMessageNotLockedException,
             SIConnectionDroppedException, SIConnectionLostException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "unlockSet", msgHandles);

      if (sessionId == 0)
      {
         // If the session Id = 0, then no one called setSessionId(). As such we are unable to flow
         // to the server as we do not know which session to instruct the server to use.
         SIErrorException e = new SIErrorException(
            nls.getFormattedMessage("SESSION_ID_HAS_NOT_BEEN_SET_SICO1043", null, null)
         );

         FFDCFilter.processException(e, CLASS_NAME + ".unlockSet",
                                     CommsConstants.CONVERSATIONHELPERIMPL_12, this);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, e.getMessage(), e);

         throw e;
      }

      if (msgHandles == null)
      {
         // Null msg Id's are no good
         SIErrorException e = new SIErrorException(
            nls.getFormattedMessage("NULL_MESSAGE_IDS_PASSED_IN_SICO1044", null, null)
         );

         FFDCFilter.processException(e, CLASS_NAME + ".unlockSet",
                                     CommsConstants.CONVERSATIONHELPERIMPL_13, this);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, e.getMessage(), e);

         throw e;
      }

      CommsByteBuffer request = getCommsByteBuffer();
      // Connection object id
      request.putShort(connectionObjectId);
      // Consumer session id
      request.putShort(sessionId);
      // Message handles
      request.putSIMessageHandles(msgHandles);
      
      //Flowing incrementLockCount field is only valid for faps 7, 8 and greater than 9.
      //In this case we should always flow a 1 (true) so we don't change existing behaviour.
      final int fapLevel = getConversation().getHandshakeProperties().getFapLevel();
      if(!(fapLevel < JFapChannelConstants.FAP_VERSION_7 || fapLevel == JFapChannelConstants.FAP_VERSION_9))
      {
         request.put((byte)1);
      }

      // Pass on call to server
      jfapSend(request,
               JFapChannelConstants.SEG_UNLOCK_SET_NOREPLY,
               JFapChannelConstants.PRIORITY_MEDIUM,
               true,
               ThrottlingPolicy.BLOCK_THREAD);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "unlockSet");
   }

   /**
    * Requests another batch of messages (asynch proxy queue)
    *
    * @see com.ibm.ws.sib.comms.client.proxyqueue.impl.ConversationHelper#requestNextMessageBatch()
    */
   public void requestNextMessageBatch()
      throws SIConnectionDroppedException, SIConnectionLostException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "requestNextMessageBatch");

      // This is done by simply requesting the ME end starts up its asynchronous consumer to deliver us another
      // batch of messages however since this is not a start from the application we ask for a restart so that
      // we can take into account whether the consumer session has been stopped by message processor or not.
      sendStart(true);                                                                                          //471642

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "requestNextMessageBatch");
        }

   /**
    * Exchanges a request to reset the browse cursor.
    */
   public void exchangeResetBrowse()
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException,
             SIErrorException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "exchangeResetBrowse");

      if (sessionId == 0)
      {
         // If the session Id = 0, then no one called setSessionId(). As such we are unable to flow
         // to the server as we do not know which session to instruct the server to use.
         SIErrorException e = new SIErrorException(
            nls.getFormattedMessage("SESSION_ID_HAS_NOT_BEEN_SET_SICO1043", null, null)
         );

         FFDCFilter.processException(e, CLASS_NAME + ".exchangeResetBrowse",
                                     CommsConstants.CONVERSATIONHELPERIMPL_14, this);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, e.getMessage(), e);

         throw e;
      }

      CommsByteBuffer request = getCommsByteBuffer();
      request.putShort(connectionObjectId);       // connection id
      request.putShort(sessionId);                // browser session id

      // Exchange request with ME.
      final CommsByteBuffer reply = jfapExchange(request,
                                                 JFapChannelConstants.SEG_RESET_BROWSE,
                                                 JFapChannelConstants.PRIORITY_MEDIUM,
                                                 true);

      // Confirm appropriate data returned
      try
      {
         short err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_RESET_BROWSE_R);
         if (err != CommsConstants.SI_NO_EXCEPTION)
         {
            checkFor_SISessionUnavailableException(reply, err);
            checkFor_SISessionDroppedException(reply, err);
            checkFor_SIConnectionUnavailableException(reply, err);
            checkFor_SIConnectionDroppedException(reply, err);
            checkFor_SIConnectionLostException(reply, err);
            checkFor_SIResourceException(reply, err);
            checkFor_SIErrorException(reply, err);
            defaultChecker(reply, err);
         }
      }
      finally
      {
         if (reply != null) reply.release();
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "exchangeResetBrowse");
   }

   /**
    * @return Returns the comms connection associated with this conversation.
    */
   public CommsConnection getCommsConnection()
   {
      ClientConversationState ccs = (ClientConversationState) conversation.getAttachment();
      return ccs.getCommsConnection();
   }
}
