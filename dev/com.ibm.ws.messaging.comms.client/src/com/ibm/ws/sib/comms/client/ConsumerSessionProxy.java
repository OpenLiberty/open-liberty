/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.client;

import java.util.ArrayList;
import java.util.List;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.client.proxyqueue.AsynchConsumerProxyQueue;
import com.ibm.ws.sib.comms.client.proxyqueue.ProxyQueueConversationGroup;
import com.ibm.ws.sib.comms.client.proxyqueue.ProxyQueueConversationGroupFactory;
import com.ibm.ws.sib.comms.common.CommsByteBuffer;
import com.ibm.ws.sib.comms.common.CommsLightTrace;
import com.ibm.ws.sib.comms.common.CommsUtils;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.HandshakeProperties;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.Conversation.ThrottlingPolicy;
import com.ibm.ws.sib.mfp.MessageDecodeFailedException;
import com.ibm.ws.sib.mfp.impl.JsMessageFactory;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.AsynchConsumerCallback;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.DestinationType;
import com.ibm.wsspi.sib.core.OrderingContext;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SIMessageHandle;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.StoppableAsynchConsumerCallback;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SIMessageNotLockedException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;

/**
 * A proxy representation of a Core ConsumerSession object.
 * Core API calls made to this proxy object from topology
 * are routed to an appropriate Messaging engine for execution.
 * Where the call on the Messaging Engine returns a new
 * Core Object, this is stored on the server and sufficient
 * data is returned to create a new proxy object.
 * <p>
 *
 * <strong>Some notes on synchonization</strong>
 * This class, along with the proxy queue and queue code implements
 * the Core SPI synchronization requirements as described in its
 * Javadoc.  This is done by synchronizing on two main objects:
 * <ul>
 * <li> The synchLock monitor is held whilst performing synchronous
 *      operations - e.g. unlockSet.</li>
 * <li> The callbackLock monitor is held whilst performing sensitive
 *      asynchronous operations - ie delivery of messages to an
 *      asynchronous consumer and certain other methods
 *      (e.g. stop) that need to ensure no asynchronous deliver
 *      of messages is taking place whilst they execute.</li>
 * </ul>
 */

//@ThreadSafe
public class ConsumerSessionProxy extends DestinationSessionProxy implements ConsumerSession
{
   /** Register Class with Trace Component */
   private static final TraceComponent tc = SibTr.register(ConsumerSessionProxy.class,
                                                           CommsConstants.MSG_GROUP,
                                                           CommsConstants.MSG_BUNDLE);

   /** Our NLS reference object */
   private static final TraceNLS nls = TraceNLS.getTraceNLS(CommsConstants.MSG_BUNDLE);

   /** Class name for FFDC's */
   private static String CLASS_NAME = ConsumerSessionProxy.class.getName();

   private AsynchConsumerProxyQueue proxyQueue = null;

   private boolean isReadAhead = false;

   /* Lock taken during aynchronous message delivery to synchronise start/stop/reg/dereg & close    */
   /* operations between callback & non-callback threads.  When taken this lock should be acquired  */
   /* before any other locks in this class. Start/stop/reg & dereg operations performed on a        */
   /* callback thread are deferred until after the user callback has completed and then executed -  */
   /* it is this deferral behaviour that requires the callbackLock to ensure that cross thread      */
   /* semantics are maintained. Close uses the lock to ensure that the consumer session is not      */
   /* closed while an asynchronous callback is in progress.                                         */
   private final Object callbackLock = new Object();

   /* Lock taken during synchronous operations */
   private Object synchLock = new Object();

   boolean asynchConsumerRegistered = false;
   
   private String destName = null;

   /* Enumerations for state of session */
   enum StateEnum
   {
      STOPPED, STOPPING, CLOSED, CLOSING, STARTED, STOPPING_THEN_STARTED
   };
   private volatile StateEnum state = StateEnum.STOPPED; // Not always referenced under a lock so make volatile

   /* Object used for excluding LME operations */
   private Object lmeMonitor = new Object();

   /** The unrecoverable reliability for this session */
   private Reliability unrecoverableReliabilty = null;

   /** The consumer session Id (this is the Id generated by the Message Processor) */
   private long messageProcessorId;

   /** Type of the destination this consumer was created for */
   private final DestinationType destType;

   /** The ordering context used with the current asynch consumer */
   private OrderingContextProxy currentOrderingContext = null;

   /**
    * If messages are being received in slices, this is where the slices are held until they are
    * complete and ready to be re-assembled.
    */
   private List<DataSlice> pendingMessageSlices = null;

   /** An object to lock on when modifying the pendingMessageSlices map */
   private Object pendingMessageSliceLock = new Object();

   // Threads need to be able to tell that a receive with wait is in progress so that they
   // we can reject attempts to receive with no wait
   private volatile boolean executingReceiveWithWait = false;

   /**
    * This enumeration represents the states that the consumer session can be transitioned into
    * from within a thread which is executing an asynchronous callback registered with the session.
    */
   private enum CallbackThreadState
   {
      STOPPED_REGISTERED,     // The session is asynchronous and stopped.
      STOPPED_DEREGISTERED,   // The session is synchronous and stopped.
      STARTED_DEREGISTERED,   // The session is synchronous and started.
      STARTED_REGISTERED,     // The session is asynchronous and started.
      CLOSED                  // The session is closed.
   };

   /**
    * This member variable is used to track any session state transitions which occure when
    * a thread executing an asynchronous consumer callback, registered with this session,
    * attempts to change the state of the session.
    */
   private CallbackThreadState callbackThreadState = CallbackThreadState.STARTED_REGISTERED;

   /**
    * Tese variables remember the parameters supplied to the last registerAsynchConsumer() method
    * call performed from within an thread running the asynchronous consumer callback associated
    * with this session.
    */
   private AsynchConsumerCallback callbackThreadCallback = null;
   private int callbackThreadMaxActiveMessages;
   private long callbackThreadMessageLockExpiry;
   private int callbackThreadMaxBatchSize;
   private OrderingContext callbackThreadOrderingContext;
   private int callbackMaxSequentialFailures;
   private long callbackHiddenMessageDelay;
   private boolean callbackStoppable;


   /** Log Source code level on static load of class */
   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Source info: @(#)SIB/ws/code/sib.comms.client.impl/src/com/ibm/ws/sib/comms/client/ConsumerSessionProxy.java, SIB.comms, WASX.SIB, uu1215.01 1.137");
   }

   /**
    * Resets the various states we keep pertaining to any calls made to this consumer session from
    * within an asynchronous callback associated with the consumer session.  This should be called
    * from the comms code which invokes AsynchConsumerCallback.consumeMessages() methods.
    */
   public void resetCallbackThreadState()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "resetCallbackThreadState");
      callbackThreadState = CallbackThreadState.STARTED_REGISTERED;
      callbackThreadCallback = null;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "resetCallbackThreadState");
   }

   /**
    * Performs any consumer session actions which were defered because they were invoked from
    * within the asynchronous consumer callback associated with this session.  This should be
    * called from the comms code which invokes AsynchConsumerCallback.consumeMessages() methods.
    * @return true, if the asynchronous consumer should continue to consume messages (i.e. it
    *         has been left running).  False, if the asynchronous consumer is stopped.
    */
   public boolean performInCallbackActions()
   throws SIConnectionLostException,
          SIResourceException,
          SIErrorException,
          SISessionDroppedException,
          SISessionUnavailableException,
          SIConnectionUnavailableException,
          SIIncorrectCallException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "performInCallbackActions");
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "callbackThreadState="+callbackThreadState);

      boolean result = false;
      switch(callbackThreadState)
      {
         case CLOSED:
            close();
            result = false;
            break;
         case STOPPED_DEREGISTERED:
            stop();
            deregisterAsynchConsumerCallback();
            result = false;
            break;
         case STOPPED_REGISTERED:
            stop();
            if (callbackThreadCallback != null)
            {
               if (callbackStoppable)
               {
                  registerStoppableAsynchConsumerCallback((StoppableAsynchConsumerCallback)callbackThreadCallback,
                                                          callbackThreadMaxActiveMessages,
                                                          callbackThreadMessageLockExpiry,
                                                          callbackThreadMaxBatchSize,
                                                          callbackThreadOrderingContext,
                                                          callbackMaxSequentialFailures,
                                                          callbackHiddenMessageDelay);
               }
               else
               {
                  registerAsynchConsumerCallback(callbackThreadCallback,
                                                 callbackThreadMaxActiveMessages,
                                                 callbackThreadMessageLockExpiry,
                                                 callbackThreadMaxBatchSize,
                                                 callbackThreadOrderingContext);
               }
            }
            result = false;
            break;
         case STARTED_DEREGISTERED:
            stop();
            deregisterAsynchConsumerCallback();
            start(false);
            result = true;
            break;
         case STARTED_REGISTERED:
            if (callbackThreadCallback != null)
            {
               stop();
               if (callbackStoppable)
               {
                  registerStoppableAsynchConsumerCallback((StoppableAsynchConsumerCallback)callbackThreadCallback,
                                                          callbackThreadMaxActiveMessages,
                                                          callbackThreadMessageLockExpiry,
                                                          callbackThreadMaxBatchSize,
                                                          callbackThreadOrderingContext,
                                                          callbackMaxSequentialFailures,
                                                          callbackHiddenMessageDelay);
               }
               else
               {
                  registerAsynchConsumerCallback(callbackThreadCallback,
                                                 callbackThreadMaxActiveMessages,
                                                 callbackThreadMessageLockExpiry,
                                                 callbackThreadMaxBatchSize,
                                                 callbackThreadOrderingContext);
               }
               start(false);
            }
            result = true;
            break;
         default:
            FFDCFilter.processException(new Exception(), CLASS_NAME + ".performInCallbackActions",
                                        CommsConstants.CONSUMERSESSIONPROXY_INCALLBACKACT_01, callbackThreadState);
            result = false;
            break;
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "performInCallbackActions", result);
      return result;
   }

   /**
    * Constructs a new proxy representation of a ConsumerSession object
    *
    * @param con The conversation on which to send
    * @param cp This is the owning connection proxy
    * @param buf The ByteBuffer containing data necessary to construct this session object
    * @param readAheadProxyQueue
    * @param unrecoverableReliability The unrecoverable reliability for this consumer.
    * @param destAddr The destination address the user gave when creating the session
    * @param destType The destination type the user gave when creating the session.
    * @param messageProcessorId The id as assigned by the message processor on the server
    */
   public ConsumerSessionProxy(Conversation con, ConnectionProxy cp,
                               CommsByteBuffer buf, AsynchConsumerProxyQueue readAheadProxyQueue,
                               Reliability unrecoverableReliability,
                               SIDestinationAddress destAddr,
                               DestinationType destType,
                               long messageProcessorId)
   {
      super(con, cp);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>",
                                           new Object[]{con, cp, buf, readAheadProxyQueue, unrecoverableReliability, destAddr, destType, messageProcessorId});

      setDestinationAddress(destAddr);
      destName = destAddr.getDestinationName();
      inflateData(buf);
      this.destType = destType;
      isReadAhead = readAheadProxyQueue != null;
      this.proxyQueue = readAheadProxyQueue;
      this.unrecoverableReliabilty = unrecoverableReliability;
      this.messageProcessorId = messageProcessorId;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }

   /**
    * Unit test constructor.
    */
   public ConsumerSessionProxy()
   {
      super(null, null);

      setProxyID((short) 123);
      destType = null;
   }

   /**
    * @return Returns this sessions unrecoverable reliability
    */
   public Reliability getUnrecoverableReliability()
   {
      return unrecoverableReliabilty;
   }

   /**
    * This method is called when part of a message is received asynchronously, as a result of a
    * receive request before the actual request has completed. This allows a message to be sent
    * back in individual parts rather than one (potentially) very large message which is easier on
    * the Java memory manager.
    *
    * @param buffer
    */
   void addMessagePart(CommsByteBuffer buffer)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "addMessagePart", buffer);

      byte flags = buffer.get();
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Flags: ", flags);

      // Simple add the slice onto our list
      synchronized (pendingMessageSliceLock)
      {
         if (pendingMessageSlices == null)
         {
            pendingMessageSlices = new ArrayList<DataSlice>();
         }

         pendingMessageSlices.add(buffer.getDataSlice());

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Message parts: ", pendingMessageSlices);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "addMessagePart");
   }

   /**
    * Proxies the identically named method on the server.
    *
    * @param tran
    *
    * @return JsMessage
    *
    * @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
    * @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
    * @throws com.ibm.websphere.sib.exception.SIResourceException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
    * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
    * @throws com.ibm.websphere.sib.exception.SIErrorException
    * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
    * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
    */
   public SIBusMessage receiveNoWait(SITransaction tran)
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException, SILimitExceededException,
             SIErrorException,
             SINotAuthorizedException,
             SIIncorrectCallException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "receiveNoWait", tran);

      SIBusMessage mess = null;

      if (state == StateEnum.CLOSED || state == StateEnum.CLOSING) {
        throw new SISessionUnavailableException(nls.getFormattedMessage("SESSION_CLOSED_SICO1013", null, null));
      }

      if (executingReceiveWithWait) {
        throw new SIIncorrectCallException(nls.getFormattedMessage("ALREADY_IN_RECEIVE_WAIT_SICO1060", null, "ALREADY_IN_RECEIVE_WAIT_SICO1060"));
      }

      // XCT instrumentation for SIBus
      //lohith liberty change
     /* if (XctSettings.isAnyEnabled())
      {
         Xct xct = Xct.current();
         if(xct.annotationsEnabled())
         {
            Annotation annotation= new Annotation(XctJmsConstants.XCT_SIBUS).add(XctJmsConstants.XCT_PROXY_RECEIVE_NO_WAIT);
            annotation.associate(XctJmsConstants.XCT_DEST_NAME,destName);
            annotation.add(new Annotation(XctJmsConstants.XCT_DEST_TYPE).add(destType.toString()));
            xct.begin(annotation);
         }
         else
            xct.begin();
      }*/
      
      // Take synch lock to exclude other synchronous operations
      synchronized(synchLock)
      {
         // Now we need to synchronise on the transaction object if there is one.
         if (tran != null)
         {
            synchronized (tran)
            {
               // Check transaction is in a valid state.
               // Enlisted for an XA UOW and not rolledback or
               // completed for a local transaction.
               if (!((Transaction) tran).isValid())
               {
                  throw new SIIncorrectCallException(
                     nls.getFormattedMessage("TRANSACTION_COMPLETE_SICO1022", null, null)
                  );
               }

               mess = _receiveNoWait(tran);
            }
         }
         else
         {
            mess = _receiveNoWait(null);
         }
      }
      
      // XCT instrumentation for SIBus
      if(mess != null)
      {
    	  //lohith liberty change
       /*  if (XctSettings.isAnyEnabled())
         {
            Xct xct = Xct.current();
            if(xct.annotationsEnabled())
            {
               Annotation annotation= new Annotation(XctJmsConstants.XCT_SIBUS).add(XctJmsConstants.XCT_PROXY_RECEIVE_NO_WAIT);
           
               String xctCorrelationID = mess.getXctCorrelationID();  		                
               if(xctCorrelationID != null)
               {  
                  try
                  {
                     String[] xctIds = Xct.getXctIdsFromString(xctCorrelationID);
                     annotation.associate(XctJmsConstants.XCT_ID,xctIds[0]) ;
                     annotation.associate(XctJmsConstants.XCT_ROOT_ID,xctIds[1]);
                  }
                  catch(IllegalArgumentException e)
                  {
                     //No FFDC Code needed
                     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "Ignoring: Invalid XCT Correlation ID " + e);
                  }
               } 
            
               xct.end(annotation);                  
            }          
            else         
               xct.end();     
         }*/
      }
      else
      {
    	//lohith liberty change  
    	  /*
         if (XctSettings.isAnyEnabled())
         {
            Xct xct = Xct.current();
            if(xct.annotationsEnabled())
            {
               Annotation annotation= new Annotation(XctJmsConstants.XCT_SIBUS).add(XctJmsConstants.XCT_PROXY_RECEIVE_NO_WAIT);            
               annotation.add(XctJmsConstants.XCT_NO_MESSAGE);
               xct.end(annotation);                  
            }
            else         
               xct.end(); 
         }            
      */}

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "receiveNoWait");
      return mess;
   }

   /**
    * This private version of receiveNoWait just simply processes the receive
    * request without doing any checking on the state of the connection, session
    * or transaction and without getting any locks needed. This method should
    * be called by a method that does do this.
    *
    * @param tran
    *
    * @return JsMessage
    */
   private SIBusMessage _receiveNoWait(SITransaction tran)
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException, SILimitExceededException,
             SIErrorException,
             SINotAuthorizedException,
             SIIncorrectCallException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "_receiveNoWait");

      SIBusMessage mess = null;


      boolean consumeSuccessful = false;
      // If we are using a proxy queue then do a receiveNoWait on that guy instead
      if (isReadAhead)
      {
         try
         {
            // Note that we don't pass on unrecoverableReliability -
            // See my note in _registerAsyncConsumerCallback
            mess = proxyQueue.receiveNoWait(tran);
         }
         catch (MessageDecodeFailedException mdfe)
         {
            FFDCFilter.processException(mdfe, CLASS_NAME + "._receiveNoWait",
                                        CommsConstants.CONSUMERSESSIONPROXY_RCVNOWAIT_01, this);

            SIResourceException resourceException =
               new SIResourceException(
                  nls.getFormattedMessage("UNABLE_TO_CREATE_JSMESSAGE_SICO1002", null, null)
               );

            resourceException.initCause(mdfe);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "receiveNoWait", resourceException);

            throw resourceException;
         }
      }
      else
      {
         mess = performReceive(-1, tran);
      }

      consumeSuccessful = true;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "_receiveNoWait");
      return mess;
   }

   /**
    * Proxies the identically named method on the server.
    *
    * @param tran
    * @param timeout
    *
    * @return JsMessage
    *
    * @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
    * @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
    * @throws com.ibm.websphere.sib.exception.SIResourceException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
    * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
    * @throws com.ibm.websphere.sib.exception.SIErrorException
    * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
    * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
    */
   public SIBusMessage receiveWithWait(SITransaction tran, long timeout)
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException, SILimitExceededException,
             SIErrorException,
             SINotAuthorizedException,
             SIIncorrectCallException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "receiveWithWait", new Object[] {tran, ""+timeout});

      SIBusMessage mess = null;

      if (state == StateEnum.CLOSED || state == StateEnum.CLOSING) {
        throw new SISessionUnavailableException(nls.getFormattedMessage("SESSION_CLOSED_SICO1013", null, null));
      }

      executingReceiveWithWait = true;

      // XCT instrumentation for SIBus
      //lohith liberty change
    /*  if (XctSettings.isAnyEnabled())
      {
         Xct xct = Xct.current();
         if(xct.annotationsEnabled())
         {
            Annotation annotation= new Annotation(XctJmsConstants.XCT_SIBUS).add(XctJmsConstants.XCT_PROXY_RECEIVE_WITH_WAIT);
            annotation.associate(XctJmsConstants.XCT_DEST_NAME,destName);
            annotation.add(new Annotation(XctJmsConstants.XCT_DEST_TYPE).add(destType.toString()));
        
            xct.begin(annotation);
         }
         else
            xct.begin();
      }*/
      
      try
      {
         // Now we need to synchronise on the transaction object if there is one.
         if (tran != null)
         {
            synchronized (tran)
            {
               // Check transaction is in a valid state.
               // Enlisted for an XA UOW and not rolledback or
               // completed for a local transaction.
               if (!((Transaction) tran).isValid())
               {
                  throw new SIIncorrectCallException(
                     nls.getFormattedMessage("TRANSACTION_COMPLETE_SICO1022", null, null)
                  );
               }

               mess = _receiveWithWait(tran, timeout );
            }
         }
         else
         {
            mess = _receiveWithWait(null, timeout);
         }
      } finally {
        executingReceiveWithWait = false;
          
         // XCT instrumentation for SIBus
        
        //lohith liberty change
         if(mess != null)
         {/*
            if (XctSettings.isAnyEnabled())
            {
               Xct xct = Xct.current();
               if(xct.annotationsEnabled())
               {
                  Annotation annotation= new Annotation(XctJmsConstants.XCT_SIBUS).add(XctJmsConstants.XCT_PROXY_RECEIVE_WITH_WAIT);
             
                  String xctCorrelationID = mess.getXctCorrelationID();
    		                
                  if(xctCorrelationID != null)
                  {
                     try
                     {
                        String[] xctIds = Xct.getXctIdsFromString(xctCorrelationID);
                        annotation.associate(XctJmsConstants.XCT_ID,xctIds[0]) ;
                        annotation.associate(XctJmsConstants.XCT_ROOT_ID,xctIds[1]);
                     }
                     catch(IllegalArgumentException e)
                     {
                        //No FFDC Code needed
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                           SibTr.debug(tc, "Ignoring: Invalid XCT Correlation ID " + e);
                     }
                  }               
                  xct.end(annotation);                  
               }
               else         
                  xct.end();         
            }
         */}
         else
         {/*
            if (XctSettings.isAnyEnabled())
            {
               Xct xct = Xct.current();
               if(xct.annotationsEnabled())
               {
                  Annotation annotation= new Annotation(XctJmsConstants.XCT_SIBUS).add(XctJmsConstants.XCT_PROXY_RECEIVE_WITH_WAIT);            
                  annotation.add(XctJmsConstants.XCT_NO_MESSAGE);
                  xct.end(annotation);                  
               }
            else         
               xct.end(); 
            }            
         */}
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "receiveWithWait");
      return mess;

   }

   /**
    * This private version of receiveWithWait just simply processes the receive
    * request without doing any checking on the state of the connection, session
    * or transaction and without getting any locks needed. This method should
    * be called by a method that does do this.
    *
    * @param tran
    * @param timeout
    *
    * @return JsMessage
    *
    * @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
    * @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
    * @throws com.ibm.websphere.sib.exception.SIResourceException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
    * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
    * @throws com.ibm.websphere.sib.exception.SIErrorException
    * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
    * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
    */
   private SIBusMessage _receiveWithWait(SITransaction tran, long timeout)
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException, SILimitExceededException,
             SIErrorException,
             SINotAuthorizedException,
             SIIncorrectCallException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "_receiveWithWait");

      SIBusMessage mess = null;

      boolean successfulConsume = false;
      // If we are using a proxy queue then do a receiveNoWait
      // on that guy instead
      if (isReadAhead)
      {
         try
         {
            // Note that we don't pass on the unrecoverableReliability -
            // See my note in _AsynchConsumerCallback.
            mess = proxyQueue.receiveWithWait(timeout, tran);
         }
         catch (MessageDecodeFailedException mdfe)
         {
            FFDCFilter.processException(mdfe, CLASS_NAME + "._receiveWithWait",
                                        CommsConstants.CONSUMERSESSIONPROXY_RCVWITHWAIT_01, this);

            SIResourceException resourceException =
               new SIResourceException(
                  nls.getFormattedMessage("UNABLE_TO_CREATE_JSMESSAGE_SICO1002", null, null)
               );

            resourceException.initCause(mdfe);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "receiveWithWait", resourceException);

            throw resourceException;
         }
      }
      else
      {
         mess = performReceive(timeout, tran);
      }

      successfulConsume = true;


      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "_receiveWithWait");
      return mess;
   }

   /**
    * This method does an actual receive by flowing the appropriate calls to
    * the server. It is used so that we can synchronize receive and receiveNoWait
    * (allowing them to be called at the same time) but without duplicating the
    * actual receive code.
    *
    * @param timeout
    * @param tran
    *
    * @return Returns a JsMessage if one was received, otherwise null.
    */
   private SIBusMessage performReceive(long timeout, SITransaction tran)
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException, SILimitExceededException,
             SIErrorException,
             SINotAuthorizedException,
             SIIncorrectCallException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "performReceive", new Object[] { ""+timeout, tran });

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, ">>>> performReceive invoked with conversation: " + getConversation());

      // If we have got into this method but we have a proxy queue or we are read ahead then we
      // should throw this exception to indicate that we do not allow this
      if (proxyQueue != null && !isReadAhead)
      {
         throw new SIIncorrectCallException(
            nls.getFormattedMessage("INCORRECT_RECEIVE_CALL_SICO1061", null, "INCORRECT_RECEIVE_CALL_SICO1061")
         );
      }

      if (timeout < -1)
      {
         // The valid timeout values into this method are -1, 0 and any positive integer.
         // Any other values suck.
         SIErrorException e = new SIErrorException(
            nls.getFormattedMessage("INVALID_PROP_SICO8004", new Object[] { ""+timeout }, null)
         );

         FFDCFilter.processException(e, CLASS_NAME + ".performReceive",
                                     CommsConstants.CONSUMERSESSIONPROXY_PERFORMRCV_02, this);

         throw e;
      }

      SIBusMessage mess = null;

      // Now perform the exchange to receive the session message on the server
      CommsByteBuffer request = getCommsByteBuffer();

      request.putShort(getConnectionObjectID());
      request.putShort(getProxyID());
      request.putSITransaction(tran);
      // Add Timeout (0 would indicate no wait should be performed)
      // -1 inidicates indefinate wait should be performed
      request.putLong(timeout);

      // Pass on call to server
      CommsByteBuffer reply = jfapExchange(request,
                                           JFapChannelConstants.SEG_RECEIVE_SESS_MSG,
                                           JFapChannelConstants.PRIORITY_MEDIUM,
                                           true);

      try
      {
         short err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_RECEIVE_SESS_MSG_R);
         if (err != CommsConstants.SI_NO_EXCEPTION)
         {
            checkFor_SISessionUnavailableException(reply, err);
            checkFor_SISessionDroppedException(reply, err);
            checkFor_SIConnectionUnavailableException(reply, err);
            checkFor_SIConnectionDroppedException(reply, err);
            checkFor_SIResourceException(reply, err);
            checkFor_SIConnectionLostException(reply, err);
            checkFor_SILimitExceededException(reply, err);
            checkFor_SINotAuthorizedException(reply, err);
            checkFor_SIIncorrectCallException(reply, err);
            checkFor_SIErrorException(reply, err);
            defaultChecker(reply, err);
         }

         // If the pending message slices are null then we are simply receiving an entire message
         // as opposed to the message in chunks
         synchronized (pendingMessageSliceLock)
         {
            if (pendingMessageSlices == null)
            {
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Received the entire message");

               // Now build a JsMessage from the returned data. Note that a message length of -1
               // indicates that no message has been returned and a null value should be returned to the
               // caller.
               reply.getShort();                // The connection object Id
               reply.getShort();                // The consumer session Id

               mess = reply.getMessage(getCommsConnection());
            }
            else
            {
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Received the final slice");

               reply.getShort();                // The connection object Id
               reply.getShort();                // The consumer session Id

               // Now get the final data slice
               addMessagePart(reply);

               // Now re-assemble the message
               try
               {
                  mess = JsMessageFactory.getInstance().createInboundJsMessage(pendingMessageSlices,
                                                                               getCommsConnection());
               }
               catch (Exception e)
               {
                  FFDCFilter.processException(e, CLASS_NAME + ".performReceive",
                                              CommsConstants.CONSUMERSESSIONPROXY_PERFORMRCV_02,
                                              this);

                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Failed to recreate message", e);

                  throw new SIResourceException(e);
               }
            }

            if (TraceComponent.isAnyTracingEnabled())
              CommsLightTrace.traceMessageId(tc, "ReceiveMsgTrace", mess);

            // Clear the reference to this
            pendingMessageSlices = null;
         }
      }
      finally
      {
         // Ensure the buffer is preserved as it now backs the message we are returning
         if (reply != null) reply.release(false);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "performReceive", mess);
      return mess;
   }

   /**
    * Sends a close flow to the peer.
    */
   private void _close()
      throws SIResourceException, SIConnectionLostException, SIErrorException,
             SIConnectionDroppedException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "_close");

      CommsByteBuffer request = getCommsByteBuffer();

      // Build Message Header
      request.putShort(getConnectionObjectID());
      request.putShort(getProxyID());

      // Pass on call to server
      CommsByteBuffer reply = jfapExchange(request,
                                           JFapChannelConstants.SEG_CLOSE_CONSUMER_SESS,
                                           JFapChannelConstants.PRIORITY_MEDIUM,
                                           true);

      try
      {
         short err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_CLOSE_CONSUMER_SESS_R);
         if (err != CommsConstants.SI_NO_EXCEPTION)
         {
            checkFor_SIConnectionDroppedException(reply, err);
            checkFor_SIResourceException(reply, err);
            checkFor_SIConnectionLostException(reply, err);
            checkFor_SIErrorException(reply, err);
            defaultChecker(reply, err);
         }
      }
      finally
      {
         if (reply != null) reply.release();
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "_close");
   }

   /**
    * Closes the consumer session.
    *
    * @param closingConnection A value of true should be specified if this session is being closed
    * because the connection that created it is being closed.  If this is the case then no data is
    * flowed across the network.
    *
    * @throws SIResourceException
    * @throws SIConnectionLostException
    * @throws SIErrorException
    * @throws SIConnectionDroppedException
    */
   public void close(boolean closingConnection)
      throws SIResourceException, SIConnectionLostException,
             SIErrorException, SIConnectionDroppedException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "close", closingConnection);

      if (executingOnCallbackThread())
      {
         callbackThreadState = CallbackThreadState.CLOSED;
      }
      else
      {
         synchronized (callbackLock) {
           boolean closeProxyQueue = false;

           // Take the synchLock to exclude other synchronous calls
           synchronized(synchLock)
           {
              if ((state != StateEnum.CLOSED) && (state != StateEnum.CLOSING))
              {
                 state = StateEnum.CLOSING;
                 // If we have a proxy queue, notify it that we
                 // are starting to close (this allows it to wake up
                 // receiveWithWait calls).
                 if (proxyQueue != null) closeProxyQueue = true;
                 else
                 {
                    // If we don't have a proxy queue (and thus cannot
                    // have an asynchronous callback) then call _close
                    // to send the close flow across the network.
                    state = StateEnum.CLOSED;
                    if (!closingConnection && !isClosed())
                    {
                       _close();
                       if (currentOrderingContext != null)
                       {
                          currentOrderingContext.decrementUseCount();
                          currentOrderingContext = null;
                       }
                    }
                 }
              }
           }

           if (closeProxyQueue) proxyQueue.closing();

           // Take the synchLock so that we can change the state
           // of the session.
           synchronized(synchLock)
           {
              if (state == StateEnum.CLOSING)
              {
                 state = StateEnum.CLOSED;
                 if (!closingConnection && proxyQueue != null)
                 {
                    // If we have a proxy queue, tell it that we
                    // are closed - this will exchange the close
                    // flow across the network for us.
                    proxyQueue.closed();
                    if (currentOrderingContext != null)
                    {
                       currentOrderingContext.decrementUseCount();
                       currentOrderingContext = null;
                    }
                 }
              }
           }
         } // synchronized (callbackLock)
      }

      // Remove the consumer from the connection
      getConnectionProxy().consumerClosedNotification(getProxyID());

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "close");
   }

   /**
    * Closes the ConsumerSession. Any subsequent attempt to call methods on the
    * ConsumerSession will be ignored.
    *
    * @throws com.ibm.websphere.sib.exception.SIResourceException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
    * @throws com.ibm.websphere.sib.exception.SIErrorException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
    */
   public void close()
      throws SIResourceException, SIConnectionLostException, SIErrorException,
             SIConnectionDroppedException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "close");
      close(false);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "close");
   }

   /**
    * Starts delivery (both synchronous and asynchronous).
    *
    * @param deliverImmediately Whether we can deliver messages on this thread.
    *
    * @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
    * @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
    * @throws com.ibm.websphere.sib.exception.SIResourceException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
    * @throws com.ibm.websphere.sib.exception.SIErrorException
    */
   public void start(boolean deliverImmediately)
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException,
             SIErrorException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "start", ""+deliverImmediately);

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "**** start invoked with conversation: " + getConversation());

      if (executingOnCallbackThread())
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Start is being called on the callback thread, callbackThreadState="+callbackThreadState);

         if (callbackThreadState == CallbackThreadState.CLOSED ||
             state == StateEnum.CLOSED || state == StateEnum.CLOSING)
         {
               throw new SISessionUnavailableException(nls.getFormattedMessage("SESSION_CLOSED_SICO1013", null, null));
         }
         else if (callbackThreadState == CallbackThreadState.STOPPED_DEREGISTERED)
         {
            callbackThreadState = CallbackThreadState.STARTED_DEREGISTERED;
         }
         else if (callbackThreadState == CallbackThreadState.STOPPED_REGISTERED)
         {
            callbackThreadState = CallbackThreadState.STARTED_REGISTERED;
         }
      }
      else
      {
         synchronized (callbackLock) {
           if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Got asynch lock");

           // Take the synchLock as we intend to change the state
           // of the session and thus must exclude other threads
           // from doing the same.
           synchronized(synchLock)
           {
              if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Got sync lock");
              if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Current state is: " + state);

              if (state == StateEnum.STOPPING)
              {
                 // If we are in stopping state, then another thread must be halfway through
                 // the stop() method.  Go into STOPPING_THEN_STARTED state so that that the
                 // other thread can finish this start operation and we avoid blocking this
                 // thread.
                 if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "currently stopping - will perform start on thread invoking stop() method");
                 state = StateEnum.STOPPING_THEN_STARTED;
              }
              else if (state == StateEnum.CLOSED || state == StateEnum.CLOSING)
              {
                 throw new SISessionUnavailableException(nls.getFormattedMessage("SESSION_CLOSED_SICO1013", null, null));
              }
              else if (state != StateEnum.STARTED)
              {
                 startInternal();
              }
           }
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "start");
   }

   /**
    * Common start code.  This is called from the start() method, and also the stop() method.
    * In the latter case, this is done to avoid blocking the start() method if it is invoked
    * concurrently with the stop() method.
    * <p>
    * Note: this method must only be called while the syncLock monitor is held.
    * @throws SISessionUnavailableException
    * @throws SISessionDroppedException
    * @throws SIConnectionUnavailableException
    * @throws SIConnectionDroppedException
    * @throws SIResourceException
    * @throws SIConnectionLostException
    * @throws SIErrorException
    */
   private void startInternal()
   throws SISessionUnavailableException, SISessionDroppedException,
          SIConnectionUnavailableException, SIConnectionDroppedException,
          SIResourceException, SIConnectionLostException,
          SIErrorException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "startInternal");
      state = StateEnum.STARTED;

      // If there is a proxy queue registered, defer start
      // processing to it.  Otherwise send the start
      // flow to our peer ourselves.
      if (proxyQueue != null) proxyQueue.start();
      else
      {
         CommsByteBuffer request = getCommsByteBuffer();

         // Build Message Header
         request.putShort(getConnectionObjectID());
         request.putShort(getProxyID());

         // At this stage we would prefer to exchange the start to ensure that the
         // calls are correctly ordered at the server as the next call could be a
         // receive with a transaction - and that will be ordered seperately.
         // However, we only do this if we are >= FAP3.
         final HandshakeProperties props = getConversation().getHandshakeProperties();
         if (props.getFapLevel() >= JFapChannelConstants.FAP_VERSION_3)
         {
            // Pass on call to server
            CommsByteBuffer reply = jfapExchange(request,
                                                 JFapChannelConstants.SEG_START_SESS,
                                                 JFapChannelConstants.PRIORITY_MEDIUM,
                                                 true);

            try
            {
               short err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_START_SESS_R);
               if (err != CommsConstants.SI_NO_EXCEPTION)
               {
                  checkFor_SISessionUnavailableException(reply, err);
                  checkFor_SISessionDroppedException(reply, err);
                  checkFor_SIConnectionUnavailableException(reply, err);
                  checkFor_SIConnectionDroppedException(reply, err);
                  checkFor_SIResourceException(reply, err);
                  checkFor_SIConnectionLostException(reply, err);
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
            // Just send it instead
            jfapSend(request,
                     JFapChannelConstants.SEG_START_SESS,
                     JFapChannelConstants.PRIORITY_MEDIUM,
                     true,
                     ThrottlingPolicy.BLOCK_THREAD);
         }
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "startInternal");
   }

   /**
    * Stops delivery (both synchronous and asynchronous).
    *
    * @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
    * @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
    * @throws com.ibm.websphere.sib.exception.SIResourceException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
    * @throws com.ibm.websphere.sib.exception.SIErrorException
    */
   public void stop()
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException,
             SIErrorException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "stop");
      stopInternal(true);         // Notify our peer of the stopped state
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "stop");
   }

   /**
    * Stops delivery (both synchronous and asynchronous).
    *
    * @param notify peer that we are stopping/stopped, even if true there may be other
    * reasons why we do not notify our peer, if false we never notify our peer
    *
    * @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
    * @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
    * @throws com.ibm.websphere.sib.exception.SIResourceException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
    * @throws com.ibm.websphere.sib.exception.SIErrorException
    */
   private void stopInternal (boolean notifypeer) throws SISessionUnavailableException, SISessionDroppedException,
                                                         SIConnectionUnavailableException, SIConnectionDroppedException,
                                                         SIResourceException, SIConnectionLostException,
                                                         SIErrorException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "stopInternal","notifypeer="+notifypeer);

      if (executingOnCallbackThread())
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "called from within asynch consumer callback, callbackThreadState="+callbackThreadState);

         if (callbackThreadState == CallbackThreadState.CLOSED || state == StateEnum.CLOSED || state == StateEnum.CLOSING)
         {
            throw new SISessionUnavailableException(nls.getFormattedMessage("SESSION_CLOSED_SICO1013", null, null));
         }
         else if (state == StateEnum.STOPPING || state == StateEnum.STOPPED)
         {
           if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Session already stopping/stopped");
         }
         else if (callbackThreadState == CallbackThreadState.STARTED_DEREGISTERED)
         {
            callbackThreadState = CallbackThreadState.STOPPED_DEREGISTERED;
         }
         else if (callbackThreadState == CallbackThreadState.STARTED_REGISTERED)
         {
            callbackThreadState = CallbackThreadState.STOPPED_REGISTERED;
         }
      }
      else
      {
        boolean stopProxyQueue = false;

        synchronized (callbackLock) {

           if (state == StateEnum.CLOSED || state == StateEnum.CLOSING) {
             throw new SISessionUnavailableException(nls.getFormattedMessage("SESSION_CLOSED_SICO1013", null, null));
           }

           // Take the synchronous lock so that we may change the state of this session without other threads being able to do the same
           synchronized(synchLock)
           {
              if (state != StateEnum.STOPPED && state != StateEnum.STOPPING && state != StateEnum.STOPPING_THEN_STARTED)
              {
                 // Set state to stopping.  This will block anyone
                 // attempting to start the session until we can fully
                 // transition into the stopped state.
                 state = StateEnum.STOPPING;

                 // If we have a proxy queue, notify it of our intention
                 // to stop.  This should prevent any new asynchronous
                 // delivery attempts being made (any in progress will,
                 // however, run to completion).
                 if (proxyQueue != null) stopProxyQueue = true;
                 else
                 {
                    // We do not have a proxy queue (and hence cannot have
                    // an asynchronous consumer) - send a stop session flow
                    // to our peer.
                    state = StateEnum.STOPPED;

                    if (notifypeer) {
                      CommsByteBuffer request = getCommsByteBuffer();

                      // Build Message Header
                      request.putShort(getConnectionObjectID());
                      request.putShort(getProxyID());

                      // Pass on call to server
                      CommsByteBuffer reply = jfapExchange(request,
                                                           JFapChannelConstants.SEG_STOP_SESS,
                                                           JFapChannelConstants.PRIORITY_MEDIUM,
                                                           true);

                      try
                      {
                         short err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_STOP_SESS_R);
                         if (err != CommsConstants.SI_NO_EXCEPTION)
                         {
                            checkFor_SISessionUnavailableException(reply, err);
                            checkFor_SISessionDroppedException(reply, err);
                            checkFor_SIConnectionUnavailableException(reply, err);
                            checkFor_SIConnectionDroppedException(reply, err);
                            checkFor_SIResourceException(reply, err);
                            checkFor_SIConnectionLostException(reply, err);
                            checkFor_SIErrorException(reply, err);
                            defaultChecker(reply, err);
                         }
                      }
                      finally
                      {
                         if (reply != null) reply.release();
                      }
                    }
                 }
              }
           }
        } // synchronized (callbackLock)

        // Transition proxy queue into stopping state outside of synchronized blocks. This is
        // because the call can block while any outstanding proxy messages are delivered to the
        // asynch consumer callbacks.
        if (stopProxyQueue) proxyQueue.stopping(notifypeer);

        synchronized (callbackLock) {
           // Take the syncLock so that we can change the state of the session
           synchronized(synchLock)
           {
              boolean doStart = (state == StateEnum.STOPPING_THEN_STARTED);

              if ((state == StateEnum.STOPPING) || (state == StateEnum.STOPPING_THEN_STARTED))
              {
                 state = StateEnum.STOPPED;
                 // If we have a proxy queue, notify it that we are
                 // stopped - so that it may choose to send a stop flow
                 // to our peer.
                 if (proxyQueue != null) proxyQueue.stopped();
              }

              if (doStart)
              {
                 startInternal();
              }
           }
        } // synchronized (callbackLock)
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "stopInternal");
   }

   /**
    * Unlocks all messages assigned to this ConsumerSession.
    * Provided to support recover() in the JMS client-ack case.
    *
    * @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
    * @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
    * @throws com.ibm.websphere.sib.exception.SIResourceException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
    * @throws com.ibm.websphere.sib.exception.SIErrorException
    */
   public void unlockAll()
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException,
             SIErrorException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "unlockAll");

      if (state == StateEnum.CLOSED || state == StateEnum.CLOSING) {
        throw new SISessionUnavailableException(nls.getFormattedMessage("SESSION_CLOSED_SICO1013", null, null));
      }

      // Take the syncLock so that we can exclude other threads from operating on the session while we unlock the messages
      synchronized(synchLock)
      {
         if (proxyQueue != null)
         {
            proxyQueue.unlockAll();
         }
         else
         {
            // Now perform the exchange to unlockAll the messages assigned to the session on the server
            CommsByteBuffer request = getCommsByteBuffer();
            request.putShort(getConnectionObjectID());
            request.putShort(getProxyID());

            // Pass on call to server
            CommsByteBuffer reply = jfapExchange(request,
                                                 JFapChannelConstants.SEG_UNLOCK_ALL,
                                                 JFapChannelConstants.PRIORITY_MEDIUM,
                                                 true);

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
                  checkFor_SIConnectionLostException(reply, err);
                  checkFor_SIErrorException(reply, err);
                  defaultChecker(reply, err);
               }
            }
            finally
            {
               if (reply != null) reply.release();
            }
         }
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "unlockAll");
   }

   /**
    * Used by comms to unlock a set of msgs.
    *
    * @param msgHandles
    *
    * @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
    * @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
    * @throws com.ibm.websphere.sib.exception.SIResourceException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
    * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
    * @throws SIMessageNotLockedException
    * @throws com.ibm.websphere.sib.exception.SIErrorException
    */
   public void unlockSet(SIMessageHandle[] msgHandles)
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException,
             SIIncorrectCallException, SIMessageNotLockedException,
             SIErrorException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "unlockSet",
                                           new Object[] {msgHandles.length + " msg ids"});
      
      unlockSet(msgHandles, true); //True is the default as it matches with old behaviour.
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "unlockSet");
   }

   /* (non-Javadoc)
    * @see com.ibm.wsspi.sib.core.AbstractConsumerSession#unlockSet(com.ibm.wsspi.sib.core.SIMessageHandle[], boolean)
    */
   public void unlockSet(SIMessageHandle[] msgHandles, boolean incrementLockCount)
   throws SISessionUnavailableException, SISessionDroppedException,
          SIConnectionUnavailableException, SIConnectionDroppedException,
          SIResourceException, SIConnectionLostException,
          SIIncorrectCallException, SIMessageNotLockedException,
          SIErrorException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "unlockSet",
                                        new Object[] {msgHandles.length + " msg ids", Boolean.valueOf(incrementLockCount)});

      if (state == StateEnum.CLOSED || state == StateEnum.CLOSING) {
        throw new SISessionUnavailableException(nls.getFormattedMessage("SESSION_CLOSED_SICO1013", null, null));
      }

     // Take the syncLock so that we can exclude other threads from perating on the session while we unlock the messages
     synchronized(synchLock)
     {
        CommsByteBuffer request = getCommsByteBuffer();
  
        request.putShort(getConnectionObjectID());
        request.putShort(getProxyID());
        request.putSIMessageHandles(msgHandles);
        
        //Flowing incrementLockCount is only valid for faps 7, 8 and greater 9
        final int fapLevel = getConversation().getHandshakeProperties().getFapLevel();
        if(!(fapLevel < JFapChannelConstants.FAP_VERSION_7 || fapLevel == JFapChannelConstants.FAP_VERSION_9))
        {
           request.put((byte)((incrementLockCount) ? 1: 0));
        }
  
        CommsByteBuffer reply = jfapExchange(request,
                                             JFapChannelConstants.SEG_UNLOCK_SET,
                                             JFapChannelConstants.PRIORITY_MEDIUM,
                                             true);
  
        try
        {
  
           short err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_UNLOCK_SET_R);
           if (err != CommsConstants.SI_NO_EXCEPTION)
           {
              checkFor_SISessionUnavailableException(reply, err);
              checkFor_SISessionDroppedException(reply, err);
              checkFor_SIConnectionUnavailableException(reply, err);
              checkFor_SIConnectionDroppedException(reply, err);
              checkFor_SIResourceException(reply, err);
              checkFor_SIConnectionLostException(reply, err);
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
   
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "unlockSet");
  }
   
   /**
    * Used to delete a set of messages.
    *
    * @param msgHandles
    * @param tran
    */
   private void _deleteSet(SIMessageHandle[] msgHandles, SITransaction tran)
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException, SILimitExceededException,
             SIIncorrectCallException, SIMessageNotLockedException,
             SIErrorException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "_deleteSet",
                                           new Object[] { msgHandles.length + " msg handles", tran });

      if (TraceComponent.isAnyTracingEnabled()) {
        CommsLightTrace.traceMessageIds(tc, "DeleteSetMsgTrace", msgHandles);
      }

      CommsByteBuffer request = getCommsByteBuffer();

      request.putShort(getConnectionObjectID());
      request.putShort(getProxyID());
      request.putSITransaction(tran);
      request.putSIMessageHandles(msgHandles);

      CommsByteBuffer reply = jfapExchange(request,
                                           JFapChannelConstants.SEG_DELETE_SET,
                                           JFapChannelConstants.PRIORITY_MEDIUM,
                                           true);

      try
      {
         short err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_DELETE_SET_R);
         if (err != CommsConstants.SI_NO_EXCEPTION)
         {
            checkFor_SISessionUnavailableException(reply, err);
            checkFor_SISessionDroppedException(reply, err);
            checkFor_SIConnectionUnavailableException(reply, err);
            checkFor_SIConnectionDroppedException(reply, err);
            checkFor_SIResourceException(reply, err);
            checkFor_SIConnectionLostException(reply, err);
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

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "_deleteSet");
   }

   /**
    *
    * @see com.ibm.wsspi.sib.core.AbstractConsumerSession#deleteSet(com.ibm.wsspi.sib.core.SIMessageHandle[], com.ibm.wsspi.sib.core.SITransaction)
    */
   public void deleteSet(SIMessageHandle[] msgHandles, SITransaction tran)
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException, SILimitExceededException,
             SIIncorrectCallException, SIMessageNotLockedException,
             SIErrorException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "deleteSet",
                                           new Object[] { msgHandles.length + " msg handles", tran });

      if (state == StateEnum.CLOSED || state == StateEnum.CLOSING) {
        throw new SISessionUnavailableException(nls.getFormattedMessage("SESSION_CLOSED_SICO1013", null, null));
      }

      // Take the synchronous lock so that we can exclude other threads from operating on this session while we delete the set of messages
      synchronized(synchLock)
      {
         if (tran == null) _deleteSet(msgHandles, null);
         else
         {
            synchronized(tran)
            {
               // Check transaction is in a valid state.
               // Enlisted for an XA UOW and not rolledback or
               // completed for a local transaction.
               if (!((Transaction) tran).isValid())
               {
                  throw new SIIncorrectCallException(
                     nls.getFormattedMessage("TRANSACTION_COMPLETE_SICO1022", null, null)
                  );
               }

               _deleteSet(msgHandles, tran);
            }
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "deleteSet");
   }

   /**
    * This method will turn this consumer from a synchronous consumer to asynchronous
    * consumer, registering the callback as the means by which to consume messages.
    * <p>
    * Calling this method successively causes the existing callback to be discarded
    * and replaced with the new one. Supplying null as the callback will have exactly
    * the same effect as to deregistering the consumer (and will cause the state to
    * back to synchronous).
    *
    * @param callback
    * @param maxActiveMessages
    * @param messageLockExpiry
    * @param maxBatchSize
    * @param orderingContext
    *
    * @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
    * @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
    * @throws com.ibm.websphere.sib.exception.SIErrorException
    * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
    */
   public void registerAsynchConsumerCallback(AsynchConsumerCallback callback,
                                              int maxActiveMessages,
                                              long messageLockExpiry,
                                              int maxBatchSize,
                                              OrderingContext orderingContext)
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIErrorException,
             SIIncorrectCallException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "registerAsynchConsumerCallback",
                                           new Object[]
                                           {
                                              callback,
                                              maxActiveMessages,
                                              messageLockExpiry,
                                              maxBatchSize,
                                              orderingContext
                                           });

      if (executingOnCallbackThread())
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "called from within asynch consumer callback, callbackThreadState="+callbackThreadState);

         if (callbackThreadState == CallbackThreadState.CLOSED ||
             state == StateEnum.CLOSED || state == StateEnum.CLOSING)
         {
            throw new SISessionUnavailableException(nls.getFormattedMessage("SESSION_CLOSED_SICO1013", null, null));
         }
         else if (callbackThreadState == CallbackThreadState.STARTED_DEREGISTERED ||
                  callbackThreadState == CallbackThreadState.STARTED_REGISTERED)
         {
            throw new SIIncorrectCallException(nls.getFormattedMessage("CALLBACK_CHANGE_WHILE_STARTED_SICO1015", null, null));
         }
         else if (callback == null)
         {
            if (callbackThreadState == CallbackThreadState.STOPPED_REGISTERED)
            {
               callbackThreadState = CallbackThreadState.STOPPED_DEREGISTERED;
            }
         }
         else
         {
            callbackThreadCallback = callback;
            callbackThreadMaxActiveMessages = maxActiveMessages;
            callbackThreadMessageLockExpiry = messageLockExpiry;
            callbackThreadMaxBatchSize = maxBatchSize;
            callbackThreadOrderingContext = orderingContext;
            callbackStoppable = false;

            if (callbackThreadState == CallbackThreadState.STOPPED_DEREGISTERED)
            {
               callbackThreadState = CallbackThreadState.STOPPED_REGISTERED;
            }
         }
      }
      else
      {
         synchronized (callbackLock) {
            synchronized(synchLock)
            {

               _registerAsynchConsumerCallback( callback,
                      maxActiveMessages,
                      messageLockExpiry,
                      maxBatchSize,
                      orderingContext,
                      0,                                                                                   //SIB0115d.comms
                      0,
                      false);                                                                                      //472879

               if (proxyQueue != null) proxyQueue.setAsynchCallback(callback,
                                                                    maxActiveMessages,
                                                                    messageLockExpiry,
                                                                    maxBatchSize,
                                                                    orderingContext,
                                                                    0,                                     //SIB0115d.comms
                                                                    0,
                                                                    false);                                        //472879
            }
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "registerAsynchConsumerCallback");
   }

   /**
    * Actually performs the async register. Registration is mainly left up
    * to the proxy queues. They will also deal with the situation where we
    * register a different callback.
    *
    * @param callback
    * @param maxActiveMessages
    * @param messageLockExpiry
    * @param maxBatchSize
    * @param orderContext
    * @param stoppable
    *
    * @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
    * @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
    * @throws com.ibm.websphere.sib.exception.SIErrorException
    * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
    */
   private void _registerAsynchConsumerCallback(AsynchConsumerCallback callback,
                                                int maxActiveMessages,
                                                long messageLockExpiry,
                                                int maxBatchSize,
                                                OrderingContext orderContext,
                                                int maxSequentialFailures,                              //SIB0115d.comms
                                                long hiddenMessageDelay,
                                                boolean stoppable)                                              //472879
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIErrorException,
             SIIncorrectCallException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "_registerAsynchConsumerCallback",
                                                                                   new Object[]
                                                                                   {
                                                                                      callback,
                                                                                      maxActiveMessages,
                                                                                      messageLockExpiry,
                                                                                      maxBatchSize,
                                                                                      orderContext,
                                                                                      maxSequentialFailures,
                                                                                      hiddenMessageDelay,
                                                                                      stoppable
                                                                                   });

      boolean completed = false;

      if (callback != null)
      {
         // Before we flow the register, ensure we increment the use count on the order context
         // if they passed one. This is to ensure one gets created if it does not exist
         // already.
         OrderingContextProxy oc = (OrderingContextProxy) orderContext;

         // Did they supply an ordering context for us to use?
         if (oc == null)
         {
            // If they did not, see if we already have an ordered async consumer registered, If so,
            // decrement the use count on the old order context and discard it.
            if (currentOrderingContext != null)
            {
               currentOrderingContext.decrementUseCount();
               currentOrderingContext = null;
            }
         }
         // They did supply an ordering context to use
         else
         {
            // Do we already have an un-ordered async consumer registered?
            // If so, we should increment the count on the new one.
            if (currentOrderingContext == null && asynchConsumerRegistered)
            {
               oc.incrementUseCount();
            }
            else if (currentOrderingContext != null && asynchConsumerRegistered)
            {
               // Do we already have an ordered async consumer registered?
               if (oc == currentOrderingContext)
               {
                  // Is it the same? If it is - do nothing.
               }
               else
               {
                  // If it is not the same, decrement the old, and increment the new.
                  currentOrderingContext.decrementUseCount();
                  oc.incrementUseCount();
               }
            }
            // Otherwise, new registration - so increment the use count
            else if (!asynchConsumerRegistered)
            {
               oc.incrementUseCount();
            }
         }

         try
         {
            try
            {
               if (isReadAhead)
               {
                  if ((maxActiveMessages != 0) || (messageLockExpiry != 0) || (stoppable))                      //472879
                  {
                     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() && stoppable) { //SIB0115d.comms,472879
                       SibTr.debug(this, tc, "Forcing Read Ahead off because callback is Stoppable");   //SIB0115d.comms
                     }                                                                                  //SIB0115d.comms

                     // If we had a read ahead proxy queue and someone has attempted to
                     // register an asynchronous consumer with either maxActiveMessages
                     // or messageLockExpiry - trash the read ahead proxy queue and
                     // substitute it with an asynch proxy queue.  This is because we
                     // currently do not support these options on read ahead. Using a               s
                     // Stoppable callback also forced read ahead off.
                     short seqNumber = proxyQueue.getCurrentMessageBatchSequenceNumber();
                     short id = proxyQueue.getId();
                     ProxyQueueConversationGroup pqcg =
                        ((ClientConversationState) getConversation().getAttachment()).getProxyQueueConversationGroup();
                     seqNumber++;

                     isReadAhead = false;

                     proxyQueue = pqcg.createAsynchConsumerProxyQueue(id, seqNumber, orderContext);
                     proxyQueue.setConsumerSession(this);
                     proxyQueue.setAsynchCallback(callback, maxActiveMessages, messageLockExpiry, maxBatchSize, orderContext, maxSequentialFailures, hiddenMessageDelay, stoppable); //SIB0115d.comms,472879
                  }
                  else
                  {
                     proxyQueue.setAsynchCallback(callback,
                                                  maxActiveMessages,
                                                  messageLockExpiry,
                                                  maxBatchSize,
                                                  null,
                                                  maxSequentialFailures,                                //SIB0115d.comms
                                                  hiddenMessageDelay,
                                                  stoppable);                                                   //472879
                  }
               }
               // If async proxy queue is not null, then we already have a callback registered
               // and the caller would like to replace the callback or switch to / from
               // ordered delivery
               else if (proxyQueue != null) // No readahead
               {
                  proxyQueue.setAsynchCallback(callback,
                                               maxActiveMessages,
                                               messageLockExpiry,
                                               maxBatchSize,
                                               orderContext,
                                               maxSequentialFailures,                                   //SIB0115d.comms
                                               hiddenMessageDelay,
                                               stoppable);                                                      //472879
               }
               else // Create a new Proxy Queue
               {
                  ClientConversationState clientConvState = (ClientConversationState) getConversation().getAttachment();
                  ProxyQueueConversationGroup pqcg = clientConvState.getProxyQueueConversationGroup();
                  if (pqcg == null)
                  {
                     ProxyQueueConversationGroupFactory pqFact = ProxyQueueConversationGroupFactory.getRef();
                     pqcg = pqFact.create(getConversation());
                     clientConvState.setProxyQueueConversationGroup(pqcg);
                  }

                  proxyQueue = pqcg.createAsynchConsumerProxyQueue(orderContext);
                  proxyQueue.setConsumerSession(this);
                  proxyQueue.setAsynchCallback(callback,
                                               maxActiveMessages,
                                               messageLockExpiry,
                                               maxBatchSize,
                                               orderContext,
                                               maxSequentialFailures,                                   //SIB0115d.comms
                                               hiddenMessageDelay,
                                               stoppable);                                                      //472879
               }
            }
            catch (SIResourceException e)
            {
               // We failed to re-register the proxy queue. This could be because the call to create
               // a proxy queue or destroy a proxy queue failed. We must rethrow this as an
               // SIConnectionLostException as we will be unsure what the state of the system is
               FFDCFilter.processException(e, CLASS_NAME + "._registerAsynchConsumerCallback",
                                           CommsConstants.CONSUMERSESSIONPROXY_REGASYNC_01, this);

               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Caught a resource exception", e);

               // And rethrow
               throw new SIConnectionDroppedException(e.getMessage(), e);
            }

            completed = true;

            // And stash the order context in case we need it later
            currentOrderingContext = oc;

            asynchConsumerRegistered = true;
         }
         finally
         {
            // Ensure we decrement again if we failed
            if (!completed && oc != null) oc.decrementUseCount();
         }
      }
      else
      {
         // User supplied a null callback - this is the same as a de-reg
         deregisterAsynchConsumerCallback();

         // Ensure we decrement the count when we de-register
         if (currentOrderingContext != null)
         {
            currentOrderingContext.decrementUseCount();

            // And don't reference it anymore
            currentOrderingContext = null;
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "_registerAsynchConsumerCallback");
   }

   /**
    * This call will turn this asynchronous consumer into a synchronous consumer once
    * again and message delivery will only be able to be done through the explicit
    * receive methods.
    *
    * @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
    * @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
    * @throws com.ibm.websphere.sib.exception.SIErrorException
    * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
    */
   public void deregisterAsynchConsumerCallback()
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIErrorException,
             SIIncorrectCallException
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "deregisterAsynchConsumerCallback");
      _deregisterAsynchConsumerCallback(false);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "deregisterAsynchConsumerCallback");
    }

   /**
    * This call will turn this asynchronous consumer into a synchronous consumer once
    * again and message delivery will only be able to be done through the explicit
    * receive methods.
    *
    * @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
    * @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
    * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
    * @throws com.ibm.websphere.sib.exception.SIErrorException
    * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
    */
   private void _deregisterAsynchConsumerCallback(boolean stoppable)
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIErrorException,
             SIIncorrectCallException
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "_deregisterAsynchConsumerCallback","stoppable="+stoppable);

      if (executingOnCallbackThread())
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "called from within asynch consumer callback, callbackThreadState="+callbackThreadState);

         if (callbackThreadState == CallbackThreadState.CLOSED ||
             state == StateEnum.CLOSED || state == StateEnum.CLOSING)
         {
            throw new SISessionUnavailableException(nls.getFormattedMessage("SESSION_CLOSED_SICO1013", null, null));
         }
         else if (callbackThreadState == CallbackThreadState.STARTED_DEREGISTERED ||
                  callbackThreadState == CallbackThreadState.STARTED_REGISTERED)
         {
            throw new SIIncorrectCallException(nls.getFormattedMessage("CALLBACK_CHANGE_WHILE_STARTED_SICO1015", null, null));
         }
         else
         {
            callbackThreadCallback = null;
            if (callbackThreadState == CallbackThreadState.STOPPED_REGISTERED)
            {
               callbackThreadState = CallbackThreadState.STOPPED_DEREGISTERED;
            }
         }
      }
      else
      {
        if (state == StateEnum.STARTED) {
          throw new SIIncorrectCallException(nls.getFormattedMessage("CALLBACK_CHANGE_WHILE_STARTED_SICO1015", null, null));
        }

         synchronized (callbackLock) {
           synchronized(synchLock)
           {
              if (asynchConsumerRegistered)
              {
                 asynchConsumerRegistered = false;

                 proxyQueue.setAsynchCallback(null, 0, 0, 0, null, 0, 0, stoppable);                           //SIB0115d.comms,472879

                 // If we are not read ahead get the queue cleaned up by GC
                 if (!isReadAhead) proxyQueue = null;

                 if (currentOrderingContext != null)
                 {
                    currentOrderingContext.decrementUseCount();
                    currentOrderingContext = null;
                 }
              }

           }
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "_deregisterAsynchConsumerCallback");
    }

   /**
    * This method is designed to drive an async consumer once and once only
    * if it can be driven. However, this is not supported remotely and so
    * calling this method will result in an SICommsException.
    *
    * @param deliveryImmediately
    *
    * @throws SISessionUnavailableException
    * @throws SISessionDroppedException
    * @throws SIConnectionUnavailableException
    * @throws SIConnectionDroppedException
    * @throws SIResourceException
    * @throws SIConnectionLostException
    * @throws SIErrorException
    */
   public void activateAsynchConsumer(boolean deliveryImmediately)
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException,
             SIErrorException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "activateAsynchConsumer", ""+deliveryImmediately);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "activateAsynchConsumer");

      // TODO Need to implement this method
      throw new SIErrorException(
         nls.getFormattedMessage("CLIENT_METHOD_INVALID_SICO1021", new Object[] { "activateAsynchConsumer" }, null)
      );
   }

   /**
    * This method returns the id for this consumer session that was assigned to it by the
    * message processor on the server. This is needed when creating a bifurcated consumer session.
    *
    * @return Returns the id.
    */
   public long getId()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getId");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getId", ""+messageProcessorId);
      return messageProcessorId;
   }

   /**
    * This method is used when an asynchronous exception occurs and we want to send a message to
    * any associated exception listeners on this connection.
    *
    * @param e The exception
    *
    */
   public void deliverAsyncException(Throwable e)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "deliverAsyncException", e);

      if (proxyQueue != null)
      {
         proxyQueue.deliverException(e);
      }
      else
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "** Odd - we are not async?!");
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "deliverAsyncException");
   }

   /**
    * @return Returns true if the current thread is executing an asynchronous callback.
    */
   private boolean executingOnCallbackThread()
   {
      final AsynchConsumerProxyQueue localProxyQueue = proxyQueue;
      
      return (localProxyQueue != null) &&
             (localProxyQueue.getAsynchConsumerThread() == Thread.currentThread());
   }

   /** @return Returns LME monitor object */
   public Object getLMEMonitor()
   {
      return lmeMonitor;
   }

   // SIB0115d.comms start

   /**
    * @see com.ibm.wsspi.sib.core.ConsumerSession#registerStoppableAsynchConsumerCallback(com.ibm.wsspi.sib.core.StoppableAsynchConsumerCallback, int, long, int, com.ibm.wsspi.sib.core.OrderingContext, int)
    */
   public void registerStoppableAsynchConsumerCallback(StoppableAsynchConsumerCallback callback,
                                                       int maxActiveMessages,
                                                       long messageLockExpiry,
                                                       int maxBatchSize,
                                                       OrderingContext orderingContext,
                                                       int maxSequentialFailures,
                                                       long hiddenMessageDelay)
   throws SISessionUnavailableException,
          SISessionDroppedException,
          SIConnectionUnavailableException,
          SIConnectionDroppedException,
          SIIncorrectCallException
   {
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "registerStoppableAsynchConsumerCallback",
                                                                                  new Object[] {callback,
                                                                                                maxActiveMessages,
                                                                                                messageLockExpiry,
                                                                                                maxBatchSize,
                                                                                                orderingContext,
                                                                                                maxSequentialFailures,
                                                                                                hiddenMessageDelay});

     // Check FAP level is suitable for this API call
     final HandshakeProperties props = getConversation().getHandshakeProperties();
     CommsUtils.checkFapLevel(props, JFapChannelConstants.FAP_VERSION_9);

     if (executingOnCallbackThread())
     {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "called from within asynch consumer callback, callbackThreadState="+callbackThreadState);

        if (callbackThreadState == CallbackThreadState.CLOSED ||
            state == StateEnum.CLOSED || state == StateEnum.CLOSING)
        {
           throw new SISessionUnavailableException(nls.getFormattedMessage("SESSION_CLOSED_SICO1013", null, null));
        }
        else if (callbackThreadState == CallbackThreadState.STARTED_DEREGISTERED ||
                 callbackThreadState == CallbackThreadState.STARTED_REGISTERED)
        {
           throw new SIIncorrectCallException(nls.getFormattedMessage("CALLBACK_CHANGE_WHILE_STARTED_SICO1015", null, null));
        }
        else if (callback == null)
        {
           if (callbackThreadState == CallbackThreadState.STOPPED_REGISTERED)
           {
              callbackThreadState = CallbackThreadState.STOPPED_DEREGISTERED;
           }
        }
        else
        {
           callbackThreadCallback = callback;
           callbackThreadMaxActiveMessages = maxActiveMessages;
           callbackThreadMessageLockExpiry = messageLockExpiry;
           callbackThreadMaxBatchSize = maxBatchSize;
           callbackThreadOrderingContext = orderingContext;
           callbackMaxSequentialFailures = maxSequentialFailures;
           callbackHiddenMessageDelay = hiddenMessageDelay;
           callbackStoppable = true;

           if (callbackThreadState == CallbackThreadState.STOPPED_DEREGISTERED)
           {
              callbackThreadState = CallbackThreadState.STOPPED_REGISTERED;
           }
        }
     }
     else
     {
        synchronized (callbackLock) {
          synchronized(synchLock) {
            _registerAsynchConsumerCallback(callback, maxActiveMessages, messageLockExpiry, maxBatchSize, orderingContext, maxSequentialFailures, hiddenMessageDelay, true); //472879
            if (proxyQueue != null) proxyQueue.setAsynchCallback(callback, maxActiveMessages, messageLockExpiry, maxBatchSize, orderingContext, maxSequentialFailures, hiddenMessageDelay, true); //472879
          }
        }
     }

     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "registerStoppableAsynchConsumerCallback");
   }

   /**
    * @see com.ibm.wsspi.sib.core.ConsumerSession#deregisterStoppableAsynchConsumerCallback()
    */
   public void deregisterStoppableAsynchConsumerCallback()
   throws SISessionUnavailableException,
          SISessionDroppedException,
          SIConnectionUnavailableException,
          SIConnectionDroppedException,
          SIIncorrectCallException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "deregisterStoppableAsynchConsumerCallback");

      // Check FAP level is suitable for this API call
      final HandshakeProperties props = getConversation().getHandshakeProperties();
      CommsUtils.checkFapLevel(props, JFapChannelConstants.FAP_VERSION_9);

      // Pass the call on down the stack...
      _deregisterAsynchConsumerCallback(true);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "deregisterStoppableAsynchConsumerCallback");
   }

   /*
    * Internal method called when a stoppable asynchronous session is stopped, the session is put into stopped state and the registered
    * application consumerSessionStopped() method is invoked to inform the application that the session has been stopped.
    */
   public void stoppableConsumerSessionStopped () {
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "stoppableConsumerSessionStopped");

     try {
       stopInternal(false); // No need to notify our peer as the stop came from our peer
     } catch (Exception e) {
       FFDCFilter.processException(e, CLASS_NAME + ".processAsyncSessionStoppedCallback", CommsConstants.CONSUMERSESSIONPROXY_SESSION_STOPPED_03, this);
     }

     final AsynchConsumerCallback asynchConsumerCallback = proxyQueue.getAsynchConsumerCallback();
     if (asynchConsumerCallback instanceof StoppableAsynchConsumerCallback) {
       final StoppableAsynchConsumerCallback stoppableAsynchConsumerCallback = (StoppableAsynchConsumerCallback)asynchConsumerCallback;
       // Protect ourselves from the application as far as possible then call the application
       try {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "** Calling application StoppableAsynchConsumerCallback");
         stoppableAsynchConsumerCallback.consumerSessionStopped();
       } catch (Throwable e) {
         FFDCFilter.processException(e, CLASS_NAME + ".stoppableConsumerSessionStopped", CommsConstants.CONSUMERSESSIONPROXY_SESSION_STOPPED_01, this);
       }
     } else {
       if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "asynchConsumerCallback not an instance of StoppableAsynchConsumerCallback is an instance of "+ asynchConsumerCallback.getClass().getName());
       SIErrorException e = new SIErrorException(nls.getFormattedMessage("WRONG_CLASS_CWSICO8022", new Object[] {asynchConsumerCallback.getClass().getName()}, null));
       FFDCFilter.processException(e, CLASS_NAME + ".processAsyncSessionStoppedCallback", CommsConstants.CONSUMERSESSIONPROXY_SESSION_STOPPED_02, this);
       SibTr.error(tc, "WRONG_CLASS_CWSICO8022", e);
       throw e;
     }

     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "stoppableConsumerSessionStopped");
   }

   // SIB0115d.comms end

   public Object getCallbackLock () {
     return callbackLock;
   }

   /**
    * Callback from a transaction under which this consumer deleted recoverable
    * messages, to inform it of a rollback (when strict redelivery ordering is enabled).
   * @throws SIErrorException 
   * @throws SIResourceException 
   * @throws SIConnectionLostException 
   * @throws SIConnectionUnavailableException 
   * @throws SISessionUnavailableException 
   * @throws SIConnectionDroppedException 
   * @throws SISessionDroppedException 
    */
   protected void rollbackOccurred() throws SISessionDroppedException, SIConnectionDroppedException, SISessionUnavailableException, SIConnectionUnavailableException, SIConnectionLostException, SIResourceException, SIErrorException {     
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
       SibTr.entry(this, tc, "rollbackOccurred");
     
     // Synchronize on the asynch lock and the synch lock, to ensure message delivery is not
     // occurring  on another thread.
     // I considered the possibility of a AB,BA deadlock here, if two asynch consumer threads
     // were to rollback from within asynch consumers concurrently and both of their transactions
     // were associated with both consumers.
     // However, this should not be possible as consumerA should not be able to enlish consumerB
     // in their transaction unless consumerB is in synchronous mode.
     // This method call is only made from within the rollback of a local transaction (no support
     // for strict redelivery ordering on a global transaction). This means we don't have to
     // worry about a rollback occurring due to an asynch transaction timeout.
     synchronized(callbackLock)
     {
       synchronized(synchLock)
       {
         // Request that the proxy queue purges all messages.
         if (proxyQueue != null) {       
           // We do not call straight into unlockAll here, as we need to allow
           // the proxy queue to perform its own locking (on the flush lock)
           proxyQueue.rollbackOccurred();
         }
       }
     }
     
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
       SibTr.exit(this, tc, "rollbackOccurred");
   }

@Override
public void unlockAll(boolean incrementUnlockCount)
		throws SISessionUnavailableException, SISessionDroppedException,
		SIConnectionUnavailableException, SIConnectionDroppedException,
		SIResourceException, SIConnectionLostException,
		SIIncorrectCallException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "unlockAll");

    if (state == StateEnum.CLOSED || state == StateEnum.CLOSING) {
      throw new SISessionUnavailableException(nls.getFormattedMessage("SESSION_CLOSED_SICO1013", null, null));
    }

    // Take the syncLock so that we can exclude other threads from operating on the session while we unlock the messages
    synchronized(synchLock)
    {
       if (proxyQueue != null)
       {
          proxyQueue.unlockAll();
       }
       else
       {
          // Now perform the exchange to unlockAll the messages assigned to the session on the server
          CommsByteBuffer request = getCommsByteBuffer();
          request.putShort(getConnectionObjectID());
          request.putShort(getProxyID());

          // Pass on call to server
          CommsByteBuffer reply = jfapExchange(request,
                                               JFapChannelConstants.SEG_UNLOCK_ALL,
                                               JFapChannelConstants.PRIORITY_MEDIUM,
                                               true);

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
                checkFor_SIConnectionLostException(reply, err);
                checkFor_SIErrorException(reply, err);
                defaultChecker(reply, err);
             }
          }
          finally
          {
             if (reply != null) reply.release();
          }
       }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "unlockAll");
 }
}
