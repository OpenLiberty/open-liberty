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
package com.ibm.ws.sib.comms.server.clientsupport;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.util.am.Alarm;
import com.ibm.ejs.util.am.AlarmManager;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.common.CommsUtils;
import com.ibm.ws.sib.comms.server.ConversationState;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.SendListener;
import com.ibm.ws.sib.jfapchannel.Conversation.ThrottlingPolicy;
import com.ibm.ws.sib.processor.MPConsumerSession;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.OrderingContext;

/**
 * <p>This is the wrapped version of a synchronous consumer. While
 * extending the main consumer class this overrides the receive
 * method to perform synchronous receiving.
 *
 * <p>However, to ensure that we do not block any thread if a timeout
 * is specified this is implemented asynchrously under the covers by
 * this class.
 *
 * @author Gareth Matthews
 */
public class CATSessSynchConsumer extends CATConsumer
{
   /** Class name for FFDC's */
   private static String CLASS_NAME = CATSessSynchConsumer.class.getName();

   /** Reference to the CATMainConsumer */
   private CATMainConsumer mainConsumer = null;

   /** The async reader we are using for synchronous receives */
   private CATSyncAsynchReader asynchReader = null;

   /**
    * A flag to indicate whether the client thinks we are
    * started or stopped, as opposed to what we have done to
    * the session on the client's behalf
    */
   private boolean logicallyStarted = false;

   /** Trace */
   private static final TraceComponent tc = SibTr.register(CATSessSynchConsumer.class,
                                                           CommsConstants.MSG_GROUP,
                                                           CommsConstants.MSG_BUNDLE);

   /** Log class info on load */
   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Source info: @(#)SIB/ws/code/sib.comms.server.impl/src/com/ibm/ws/sib/comms/server/clientsupport/CATSessSynchConsumer.java, SIB.comms, WASX.SIB, aa1225.01 1.55");
   }

   /**
    * Constructs a new synchronous consumer.
    *
    * @param mainConsumer The main consumer
    */
   public CATSessSynchConsumer(CATMainConsumer mainConsumer)
   {
      super();

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", mainConsumer);

      this.mainConsumer = mainConsumer;
      if (mainConsumer.isStarted()) logicallyStarted = true;

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
    * <p>This method is called when receive is called on this consumer
    * session. This must do an asychronous receive so that this method
    * returns control to the TCP thread.
    *
    * <p>Note that this method does not reply to the client. This is
    * done by the async reader class.
    *
    * <p>Timeouts that are flown as part of the FAP are different to those
    * which would normally be passed into the <code>receive()</code> method.
    * These are:
    *
    * <ul>
    *   <li>Timeout = -1: No wait should be performed</li>
    *   <li>Timeout =  0: Wait forever<li>
    *   <li>Otherwise   : Wait for the specified number of milliseconds</li>
    * </ul>
    *
    * @param requestNumber The request number the async callback should reply with.
    * @param transaction The current transaction.
    * @param timeout The timeout of this receive.
    */
   public void receive(int requestNumber,
                       int transaction,
                       long timeout)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "receive",
                                           new Object[]{requestNumber, transaction, timeout});

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
         SibTr.debug(this, tc, "RQ: " + requestNumber + ", Timeout: " + timeout);

         if (timeout == -1)
         {
            SibTr.debug(this, tc, "Emulating a receiveNoWait()");
         }
         else if (timeout == 0)
         {
            SibTr.debug(this, tc, "Emulating an indefinate receive()");
         }
         else
         {
            SibTr.debug(this, tc, "Emulating a receive() for " + timeout + "ms");
         }
      }

      requestsReceived++;

      // First ensure the session is stopped
      try
      {
         if (mainConsumer.isStarted()) getConsumerSession().stop();
      }
      catch (SIException sis)
      {
         //No FFDC code needed
         //Only FFDC if we haven't received a meTerminated event.
         if(!((ConversationState)getConversation().getAttachment()).hasMETerminated())
         {
            FFDCFilter.processException(sis, CLASS_NAME + ".receive",
                                        CommsConstants.CATSESSSYNCHCONSUMER_RECEIVE_01, this);
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, sis.getMessage(), sis);

         // At this point we can not do much - so we will carry on
         // It is likely that if an exception was thrown here something
         // fairly bad went wrong and this should be flagged by subsequent
         // actions in this method
      }

      // Have we an async reader for this session?
      if (asynchReader == null)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Creating async reader for first time");

         asynchReader = new CATSyncAsynchReader(transaction,        // The transaction ID
                                                getConversation(),  // The conversation
                                                mainConsumer,       // The main consumer
                                                requestNumber);     // The initial request #
         try
         {
            // Here we need to examine the config parameter that will denote whether we are telling
            // MP to inline our async callbacks or not. We will default to false, but this can
            // be overrideen.
            boolean inlineCallbacks =
               CommsUtils.getRuntimeBooleanProperty(CommsConstants.INLINE_ASYNC_CBACKS_KEY,
                                                    CommsConstants.INLINE_ASYNC_CBACKS);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Inline async callbacks: " + inlineCallbacks);

            // Here we need to examine the unrecoverable reliability setting for the session.
            // The deal is that if we are not transacted then we want to ensure that as little
            // overhead takes place as possible. As such, if we are not transacted we will
            // override the sessions unrecoverable reliability with the highest reliability
            // - making everything unrecoverable.
            // We can only override this by casting to the special MP form of consumer session

            Reliability unrecov = getUnrecoverableReliability();
            if (transaction == CommsConstants.NO_TRANSACTION)
            {
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Setting unrecoverable reliability to max");
               unrecov = Reliability.ASSURED_PERSISTENT;
            }

            MPConsumerSession mpSession = (MPConsumerSession) getConsumerSession();
            mpSession.registerAsynchConsumerCallback(asynchReader,    // The reader
                                                     0,               // Max active messages
                                                     0,               // Message lock expiry
                                                     1,               // Batch size
                                                     unrecov,         // Unrecov reliability
                                                     inlineCallbacks, // Inline
                                                     null);           // Ordering context
         }
         catch (SIException s)
         {
            //No FFDC code needed
            //Only FFDC if we haven't received a meTerminated event.
            if(!((ConversationState)getConversation().getAttachment()).hasMETerminated())
            {
               FFDCFilter.processException(s, CLASS_NAME + ".receive",
                                           CommsConstants.CATSESSSYNCHCONSUMER_RECEIVE_02, this);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, s.getMessage(), s);

            // Inform the client - note that this will mark the reader
            // complete so that only one response is sent to the client
            asynchReader.sendErrorToClient(s,
                                           CommsConstants.CATSESSSYNCHCONSUMER_RECEIVE_02);

            // Kill me
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "receive");
            return;
         }
      }
      else
      {
         // Inform the consumer that we have not receieved a message
         asynchReader.setComplete(false);
         // Save the request number in the async consumer
         asynchReader.setRequestNumber(requestNumber);
         // Ensure we tell the reader which transaction to operate under
         asynchReader.setTransaction(transaction);
      }

      // Flush the consumer as a one shot to see if we can get
      // a message
      try
      {
         if (logicallyStarted) mainConsumer.getConsumerSession().activateAsynchConsumer(true);
      }
      catch (SIException s)
      {
         //No FFDC code needed
         //Only FFDC if we haven't received a meTerminated event.
         if(!((ConversationState)getConversation().getAttachment()).hasMETerminated())
         {
            FFDCFilter.processException(s, CLASS_NAME + ".receive",
                                        CommsConstants.CATSESSSYNCHCONSUMER_RECEIVE_03, this);
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, s.getMessage(), s);

         // Inform the client - note that this will mark the reader
         // complete so that only one response is sent to the client
         asynchReader.sendErrorToClient(s, CommsConstants.CATSESSSYNCHCONSUMER_RECEIVE_03);

         // Kill me
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "receive");
         return;
      }

      // If the consumer received a message then we are done
      if (!asynchReader.isComplete())
      {
         // If we specified a timeout, then start the consumer
         // and start the timer
         if (timeout == -1)
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "No message received");

            asynchReader.sendNoMessageToClient();
         }
         else
         {
            asynchReader.setCurrentlyDoingReceiveWithWait(true);
            
            //At this point we are poised to go asynch so we need to register asynchReader as a SICoreConnectionListener.
            try
            {
               final MPConsumerSession mpSession = (MPConsumerSession)getConsumerSession();
               mpSession.getConnection().addConnectionListener(asynchReader);
            }        
            catch(SIException s)
            {
               //No FFDC code needed
               //Only FFDC if we haven't received a meTerminated event.
               if(!((ConversationState)getConversation().getAttachment()).hasMETerminated())
               {
                  FFDCFilter.processException(s, CLASS_NAME + ".receive", CommsConstants.CATSESSSYNCHCONSUMER_RECEIVE_04, this);
               }
               
               if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, s.getMessage(), s);
               
               //Inform the client - note that this will mark the reader
               //complete so that only one response is sent to the client
               asynchReader.sendErrorToClient(s, CommsConstants.CATSESSSYNCHCONSUMER_RECEIVE_04);
               
               if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "receive");
               return;
            }

            // If we are waiting indefinately, then we do not need a wake up call
            if (timeout != 0)
            {
               // Start the timer and associate it with the reader
               CATTimer catTimer = new CATTimer(asynchReader);
               Alarm alarm = AlarmManager.createNonDeferrable(timeout, catTimer, "RQ: "+requestNumber);
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                  SibTr.debug(this, tc, "Setting async readers alarm to: " + alarm.toString());
               asynchReader.setCATTimer(alarm);
            }

            // Now start the message delivery
            if (logicallyStarted)
            {
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                  SibTr.debug(this, tc, "Starting async consumer. Timeout = " + timeout + "ms");
               mainConsumer.start(requestNumber, true, false, null);
            }
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "receive");
   }

   /**
    * Marks this session as started. If we are currently doing a receiveWithWait()
    * then actually starts the session for us so that message delivery can
    * continue.
    *
    * @param requestNumber
    * @param deliverImmediately The deliver immediately flag is ignored for this type of receive
    * @param sendReply
    * @param sendListener
    */
   public void start(int requestNumber, boolean deliverImmediately, boolean sendReply, SendListener sendListener)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "start",
                                           new Object[]{requestNumber, deliverImmediately});

      logicallyStarted = true;

      // If the async reader is currently in the middle of a receiveWithWait() then all we need to
      // do is call start() on the CATConsumer class which will start the session and then send
      // a reply if one is required
      if (asynchReader.isCurrentlyDoingReceiveWithWait())
      {
         super.start(requestNumber, true, sendReply, sendListener);
      }
      // If we do not need to actually start the session then we still need to reply (potentially)
      // so ensure we do this here.
      else
      {
         if (sendReply)
         {
            try
            {
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
                                           CommsConstants.CATSESSSYNCHCONSUMER_START_01,
                                           this);

               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, e.getMessage(), e);

               SibTr.error(tc, "COMMUNICATION_ERROR_SICO2013", e);

               sendListener.errorOccurred(null, getConversation());
            }
         }
         else
         {
            sendListener.dataSent(getConversation());
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "start");
   }

   /**
    * Stops the session and marks it as stopped.
    *
    * @param requestNumber
    * @param sendListener
    */
   public void stop(int requestNumber, SendListener sendListener)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "stop", requestNumber);

      logicallyStarted = false;
      super.stop(requestNumber, sendListener);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "stop");
   }

   /**
    * Closes the session. If we are currently doing a receiveWithWait then
    * that will be interrupted and a response will be sent to the client.
    *
    * @param requestNumber
    */
   public void close(int requestNumber)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "close", requestNumber);

      // Deregister any error callback created for this connection.
      if (asynchReader != null)
      {
              try
              {
                 mainConsumer.getConsumerSession().getConnection().removeConnectionListener(asynchReader);
              }
              catch (SIException e)
              {
            //No FFDC code needed
            //Only FFDC if we haven't received a meTerminated event.
            if(!((ConversationState)getConversation().getAttachment()).hasMETerminated())
            {
               FFDCFilter.processException(e, CLASS_NAME + ".close",
                     CommsConstants.CATSESSSYNCHCONSUMER_CLOSE_01, this);
            }

                 if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, e);
              }

        if (asynchReader.isCurrentlyDoingReceiveWithWait()) asynchReader.sendNoMessageToClient();
      }
      super.close(requestNumber);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "close");
   }

   /**
    * This will cause the synchronous session to become asynchrnous.
    * As such, this class will not handle the session anymore, so we
    * inform the main consumer and that will then switch over the
    * delegated class to handle the consumer.
    *
    * @param requestNumber
    * @param maxActiveMessages
    * @param messageLockExpiry
    * @param batchsize
    * @param orderContext
    */
   public void setAsynchConsumerCallback(int requestNumber,
                                         int maxActiveMessages,
                                         long messageLockExpiry,
                                         int batchsize,
                                         OrderingContext orderContext)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setAsynchConsumerCallback",
                                           new Object[]
                                           {
                                              requestNumber,
                                              maxActiveMessages,
                                              messageLockExpiry,
                                              batchsize,
                                              orderContext
                                           });

      mainConsumer.setAsynchConsumerCallback(requestNumber,
                                             maxActiveMessages,
                                             messageLockExpiry,
                                             batchsize,
                                             orderContext);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setAsynchConsumerCallback");
   }

   /**
    * @return Returns a String representing the status of this consumer.
    */
   public String toString()
   {
      String s = "CATSessSyncConsumer@" + Integer.toHexString(hashCode()) +
                 ": logicallyStarted: " + logicallyStarted +
                 ", requestsReceived: " + requestsReceived +
                 ", messagesSent: " + messagesSent;

      if (asynchReader != null)
      {
         s += ", " + asynchReader.toString();
      }

      return s;
   }
}
