/*******************************************************************************
 * Copyright (c) 2004, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.server.clientsupport;

import java.io.IOException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

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
import com.ibm.ws.sib.comms.server.clientsupport.CATConsumer.State;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.SendListener;
import com.ibm.ws.sib.jfapchannel.Conversation.ThrottlingPolicy;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
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
   public enum State { NEW, STOPPED, STARTING, STARTED, STOPPING, PAUSED, CLOSED, UNDEFINED; // Put UNDEFINED in here for any class that doesn't implement getState() properly.
	   
	   // A couple of methods that should make the code using this enum a bit more readable.
	   public boolean isStarted() {
		   return this.equals(STARTED);
	   }
	   
	   public boolean isStopped() {
		   return this.equals(STOPPED);
	   }
	   
	   public boolean isStarting() {
		   return this.equals(STARTING);
	   }
	   
	   public boolean isStopping() {
		   return this.equals(STOPPING);
	   }
	   
	   public boolean isTransitioning() {
		   return this.equals(NEW) || this.equals(STARTING) || this.equals(STOPPING) || this.equals(PAUSED);
	   }
	   
   } 

   private State state = State.STOPPED;
   Thread transitioningThread = null;
   ReentrantLock stateLock = new ReentrantLock();
   private Condition stateTransition = stateLock.newCondition();
   

   public State getState() {
	   stateLock.lock();
	   try {
		   if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
			   SibTr.debug( tc , "State = " + state );
			   return state;
	   }
	   
	   finally {
		   stateLock.unlock();
	   }
   }

   /**
    * Sets a new state, but only traces a warning on potentially problematic state changes instead of creating an FFDC.
    * This is primarily intended for setting the state during consumer initialization when we know what we're doing
    * 
    * @param newState
    * @return the previous state
    */
   protected State setStateWithoutFDC(State newState) {
	   return setState(newState, false);
   }
   
   
   /**
    * Sets a new state
    * @param newState
    * @return the previous state
    */
   public State setState(State newState) {
	   return setState(newState, true);
   }

	   /**
	    * Sets a new state
	    * @param newState
	    * @return the previous state
	    */
   private State setState(State newState, boolean allowFFDC) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setState", allowFFDC);

	   State oldState = State.UNDEFINED;
	   stateLock.lock();
	   try {
		   oldState = state;

		   if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			   SibTr.debug( tc , this.getClass().getName() + ":" + getDestinationName() + ", Setting state. Old state =  " + state + ", new state = " + newState);
		   }
			   
			   // Is there a potential issue with the state change? Check for the expected changes. Other might be "valid" as responses to an earlier error if we're tidying up.
			   boolean warn = false;
			   boolean ffdc = false;
			   
			   switch (state) {
			   	case NEW:
			   		// This is set for CATProxyConsumer when it is initialized, so the only state we should be able to transition to is STARTING as the consumer is started
			   		ffdc = !(newState == State.STARTING); // Can we get directly to started if we get into consumeMessages()? Should that method step through starting and started just to be safe?
			   		break;
				case STOPPED:
					// If we're stopped, then all we can do is get started or paused during an unlock
					// However, if a client makes a stop request, this will get driven anyway, so we'll shift back to stopping state in order to process that request, even though there shouldn't be anything to do
					warn = (newState == State.STOPPING);
					ffdc = (!(warn || newState == State.STARTING || newState  == State.PAUSED));
					break;
				case STARTING:
					warn = (newState == State.STOPPING); // Can happen if deliverImmediately is true
					ffdc = !warn && (newState != State.STARTED);
					break;
				case STARTED:
					// If we're started, then all we can do is begin stopping or be paused during an unlock
					// However, if a client makes a start request, this will get driven anyway, so we'll shift back to starting state in order to process that request, even though there shouldn't be anything to do
					warn = (newState == State.STARTING);
					ffdc = (!(warn || newState == State.STOPPING || newState == State.PAUSED));
					break;
				case STOPPING:
					ffdc = (newState != State.STOPPED);
					break;
				case PAUSED:
					ffdc = (!(newState == State.STARTED || newState == State.STOPPED));
					break;
				case CLOSED:
				case UNDEFINED:
					// Never mind, we don't use these at the moment.

				   }
			   
			   if (warn || ffdc) {
				   
				   // Something looks suspicious, so trace it in all cases.
				   if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			              SibTr.debug(tc, " WARNING: possible error in state transition", (Thread.currentThread().getStackTrace()) );
				   }
				   
				   if (ffdc && allowFFDC) {
					   Exception e = new Exception("Possible error in state transition");
					      FFDCFilter.processException(e, CLASS_NAME + ".setState",
                                  CommsConstants.CATCONSUMER_SETSTATE_01,
                                  this);
				   }
				   
			       
			   }
		   
		   state = newState;
		   
		   
		   // If anyone is waiting for us to complete something, we should give them a signal
		   // signalAll if we've moved to a non-transitional state.
		   if (state.isTransitioning())
		   {
				   transitioningThread = Thread.currentThread();

		   }
		   else {
			   transitioningThread = null;
			   stateTransition.signalAll();
		   }

		   return oldState;
	   }
	   finally {
		   stateLock.unlock();
		      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setState");
		   
	   }
   }
   
   public void awaitStableState() throws InterruptedException {

	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "awaitStableState");
	   
		stateLock.lock();
		try {

			if (Thread.currentThread().equals(transitioningThread)) {
				
				   if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					   SibTr.debug( tc , "consumer on " + getDestinationName() + " state is transitioning, but it was this thread that set the transition state" );
				   }

				
				return;
			}
			
			while (state.isTransitioning()) {
					stateTransition.await();
			}

		} finally {
			stateLock.unlock();
		}

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "awaitStableState");

		return;
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

	  State fallbackState = State.UNDEFINED;
      try
      {
         // Set the started flag before starting the session in case deliverImmediately is true in which case message processor
         // may immediately deliver a message to the async consumer (consumeMessages) which will stop the session and set
         // started=false. We don't want this method setting started=true after consumeMessages has set it false hence the
         // need to set started=true before starting the session.
    	  // Transition through states cleanly
    	  stateLock.lock();
    	  
    	  try {
    		  awaitStableState();
    		  fallbackState = setState(State.STARTING);
    	  }
    	  finally {
    		  stateLock.unlock();
    	  }
         getConsumerSession().start(deliverImmediately);
         stateLock.lock();
         try {
             awaitStableState();
             State currentState = getState();
             
             if (State.STARTING == currentState)
            	 fallbackState = State.STARTED;
             else {
            	 // Hmm, what changed while we were in start()?
            	 if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "State changed while session was starting. State is now " + currentState);
            	 fallbackState = State.UNDEFINED;
             }
         }
         finally {
        	 stateLock.unlock();
         }
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
      catch (Exception e) //(SIException e)
      {
         //No FFDC code needed

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
      finally {
    	  if (fallbackState != State.UNDEFINED) setState(fallbackState);
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

	  State fallback = State.UNDEFINED;
      
      try
      {

    	  // Transition through states cleanly
        	  stateLock.lock();
        	  
        	  try {
        		  awaitStableState();
            	  fallback = setState(State.STOPPING);
        	  }
        	  finally {
        		  stateLock.unlock();
        	  }
        	  
        	  getConsumerSession().stop();
        	  
        	  
        	  // LDS TODO: This next bit mirrors what is in start(). However, in that case there's the chance of different behaviour based on the deliverImmediately setting. Can anything like that happen here? If not, do we really need this extra checking or can we simply assume that we can switch to STOPPED state now that the stop() method has returned?
              stateLock.lock();
              try {
                  awaitStableState();
                  State currentState = getState();
                  
                  if (State.STOPPING == currentState)
                	  fallback = State.STOPPED;
                  else {
                 	 // Hmm, what changed while we were in stop()?
                 	 if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "State changed while session was stopping. State is now " + currentState);
                 	fallback = State.UNDEFINED;
                  }
              }
              finally {
             	 stateLock.unlock();
              }
        	  
        	  fallback = State.STOPPED;

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
      catch (Exception e) //(SIException e)
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
      finally {
		  if (fallback != State.UNDEFINED) setState(fallback);
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

   protected String getDestinationName() {
	   String destinationName = "Unknown";
	   try {
		   destinationName = getConsumerSession().getDestinationAddress().getDestinationName();
	   }
	   catch (Throwable t) {
		   if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.debug(tc, "failed to read destination name for consumerSession", t);
	   }
	   return destinationName;
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
   
   
   // PH20984
   /**
    * Create a formatted dump of the current state.
    * @param writer
    */
   public void dump(FormattedWriter writer) {
       if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
           SibTr.entry(this, tc, "dump", new Object[] { writer });

       try {
           writer.newLine();
           writer.startTag(this.getClass().getSimpleName());
           writer.indent();

           writer.newLine();
           writer.taggedValue("toString", toString());
           ConsumerSession consumerSession = getConsumerSession();
           if (consumerSession != null)
               consumerSession.dump(writer);
           writer.outdent();
           writer.newLine();
           writer.endTag(this.getClass().getSimpleName());

        } catch (Throwable t) {
            // No FFDC Code Needed
            try {
                writer.write("\nUnable to dump " + this + " " + t);
            } catch (IOException e) {
                e.printStackTrace();
            }
       }

       if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
           SibTr.exit(this, tc, "dump");
   }
}
