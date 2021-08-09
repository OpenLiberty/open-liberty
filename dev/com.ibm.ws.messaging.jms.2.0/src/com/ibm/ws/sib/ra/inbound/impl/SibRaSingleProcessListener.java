/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.ra.inbound.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.IllegalStateException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;
import javax.resource.spi.work.WorkManager;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.ra.impl.SibRaUtils;
import com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.LockedMessageEnumeration;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SIMessageNotLockedException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;
import com.ibm.wsspi.sib.pacing.AsynchDispatchScheduler;
import com.ibm.wsspi.util.FastSerializableHashMap;

/**
 * Dispatches messages when the message-driven beans are located in the same
 * process as the listener.
 */
final class SibRaSingleProcessListener extends SibRaListener {

    /**
     * The work manager with which items of work are scheduled.
     */
    private final WorkManager _workManager;

    /**
     * Count of the number of currently in process work items. When this hits
     * the maximum the session is stopped until it drops down again. This logic
     * can be removed once the core SPI includes the ability to set a maximum
     * number of locked/transactional messages.
     */
    private int _workCount;
    
    /**
     * Lock Object to be used when changing and viewing the _workCount 
     * variable. Taking this lock allows consumeMessages to be called, this will
     * be ok as it will schedule work on another thread and we should then lock
     * in workAccepted. 
     */
    private Object _workCountLock = new WorkCountLockObject();

    /*
     * Empty inner class so we have a name to the lock above
     */
    private class WorkCountLockObject
    {
    }

    /**
     * The maximum number of work items that should be processing at once.
     */
    private final int _maxWorkCount;
    
    /**
     * Flag indicating whether <code>BEST_EFFORT_NON_PERSISTENT</code>
     * messages should be deleted in <code>consumeMessages</code>. This is
     * <code>true</code> if the endpoint method is non-transactional.
     */
    private final boolean _deleteBestEffortNonPersistentMessages;

    /**
     * The activation specification for which this listener was created.
     */
    private final ActivationSpec _activationSpec;

    /**
     * The name of the bus on which the destination to which we are listening
     * resides.
     */
    private final String _busName;

    /**
     * The name of the messaging engine to which the listener is connected.
     */
    private final String _meName;
    
    /**
     * The UUID of the messaging engine to which the listener is connected.
     */
    private final String _meUuid;

    /**
     * The name of the destination to which the listener is attached.
     */
    private final String _destinationName;

    /**
     * The component to use for trace.
     */
    private static final TraceComponent TRACE = SibRaUtils
            .getTraceComponent(SibRaSingleProcessListener.class);

    /**
     * The component to use for trace for <code>SibRaWork</code>.
     */
    private static final TraceComponent WORK_TRACE = SibRaUtils
            .getTraceComponent(SibRaSingleProcessListener.class);

    /**
     * The name of this class.
     */
    private static final String CLASS_NAME = SibRaSingleProcessListener.class
            .getName();

    /**
     * The name of the SibRaWork class.
     */
    private static final String WORK_CLASS_NAME = SibRaWork.class.getName();


    /**
     * Constructor. Caches a reference to the work manager.
     *
     * @param connection
     *            a connection to the messaging engine
     * @param destination
     *            the destination to listen on
     * @param messageEndpointFactory
     *            the message endpoint factory
     * @throws ResourceException
     *             if a session could not be created or started or the callback
     *             could not be registered
     */
    SibRaSingleProcessListener(final SibRaMessagingEngineConnection connection,
            final SIDestinationAddress destination, MessageEndpointFactory messageEndpointFactory) throws ResourceException {

        super(connection, destination, messageEndpointFactory);

        final String methodName = "SibRaSingleProcessListener";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { connection,
                    destination, messageEndpointFactory });
        }

        _workManager = _connection.getEndpointActivation().getWorkManager();
        
        /*
         * _maxWorkCount is obtained from _maxActiveMessages, which in turn is read
         * from maxConcurrency in the activation spec. If strict message ordering
         * is switched on then _maxActiveMessages is set to 1 by the base class
         * (SibRaListener) constructor. The variable _maxActiveMessages could actually
         * be used instead of _maxWorkCount inside this class but the variable _maxWorkCount
         * better describes what this field is being used for on distributed systems. For
         * z/OS the class SibRaMultiProcessListener is used and there we do use maxActiveMessages
         * to cap the maximum number of messages that the endpoint can process at one time.
         */
        _maxWorkCount = _maxActiveMessages;
        
        /*
         * Obtain the properties for the RAHandler
         */
        SibRaEndpointConfiguration endpointConfiguration = _connection.getEndpointConfiguration();
        _activationSpec = endpointConfiguration.getActivationSpec();
        _meName = connection.getConnection().getMeName();
        _meUuid = connection.getConnection().getMeUuid();
        _busName = connection.getBusName();
        _destinationName = destination.getDestinationName();

        _deleteBestEffortNonPersistentMessages = !connection
                .getEndpointActivation().isEndpointMethodTransactional();

        // Call startConsumer method here rather than in base class to ensure
        // that this class has been initialized before any messages can be
        // delivered.
        startConsumer ();

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * The consumeMessages method is invoked by the message processor with an enumeration
     * containing one or more messages locked to this consumer. The consumeMessages method
     * will either call the internalConsumeMessage method directly, or, if XD wishes to suspend
     * the processing this method will be called then XD decides to resume processing. This
     * method retrieves the messages from the enumeration. It then schedules a piece of work
     * that will create the dispatcher on a new thread.
     *
     * @param lockedMessages
     *            the enumeration of locked messages
     * @param asynchDispatchScheduler
     *        the XD scheduler, the cached version if we are called from resume
     */
    public void internalConsumeMessages(final LockedMessageEnumeration lockedMessages,
        final AsynchDispatchScheduler asynchDispatchScheduler) {

        final String methodName = "internalConsumeMessages";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { lockedMessages, asynchDispatchScheduler });
        }

        try
        {
         

           /*
            * Retrieve messages
            */

           final List<DispatchableMessage> messages = new ArrayList<DispatchableMessage> ();
           SIBusMessage message;
           while ((message = lockedMessages.nextLocked()) != null)
           {

        	   // chetan liberty change : 
        	   // RAHandler and WAS.exitpoint is not applicable in liberty.Hence removing it
        	   final Map handlerContext = new FastSerializableHashMap();

        	   DispatchableMessage dispatchableMessage = new DispatchableMessage(message, handlerContext);
        	   messages.add(dispatchableMessage);

        	   /*
        	    * If required, delete best-effort nonpersistent messages
        	    */
        	   if (_deleteUnrecoverableMessages
        			   && (_unrecoverableReliability.compareTo(message.getReliability ()) >= 0)) {
        		   lockedMessages.deleteCurrent(null);
        	   }

           }

           //Only schedule some work if we have some messages
           if (messages.size() != 0)
           {
             final SibRaWork work = new SibRaWork();

             /*
              * Schedule messages for dispatch
              * The asynchDispatchScheduler will either
              * be a cached version if we are been resumed from a
              * suspend or a newly obtained version
              */
             work.schedule(messages, asynchDispatchScheduler, this);
           }

        } catch (final Throwable throwable) {

            FFDCFilter.processException(throwable, CLASS_NAME + "."
                    + methodName, "1:375:1.42", this);
            SibTr.error(TRACE, "RETRIEVE_MESSAGES_CWSIV1100", new Object[] {
                    throwable, lockedMessages });

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Perform any processing on a message that is required whilst creating the
     * cached locked message enumeration.
     * @param message The message to process
     * @param lockedMessages The locked message enumeration the message is in. The cursor
     * will currently be pointing at the message passed as a parameter.
     * @throws SIIncorrectCallException
     * @throws SIResourceException
     * @throws SIMessageNotLockedException
     * @throws SILimitExceededException
     * @throws SIConnectionLostException
     * @throws SIConnectionUnavailableException
     * @throws SISessionUnavailableException
     * @throws SIConnectionDroppedException
     * @throws SISessionDroppedException
     */
    protected void processCachedMessage (SIBusMessage message,
        LockedMessageEnumeration lockedMessages) throws SISessionDroppedException,
        SIConnectionDroppedException, SISessionUnavailableException,
        SIConnectionUnavailableException, SIConnectionLostException,
        SILimitExceededException, SIMessageNotLockedException,
        SIResourceException, SIIncorrectCallException {

      /*
       * If required, delete best-effort nonpersistent messages
       */
      if (_deleteBestEffortNonPersistentMessages
              && Reliability.BEST_EFFORT_NONPERSISTENT.equals(message
                      .getReliability())) {

          lockedMessages.deleteCurrent(null);

      }
    }

    /**
     * Indicates whether the resource adapter is using bifurcated consumer
     * sessions. If so, the <code>ConsumerSession</code> created by the
     * listener needs to be bifurcatable.
     *
     * @return always returns <code>false</code>
     */
    boolean isSessionBifurcated() {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            final String methodName = "isSessionBifurcated";
            SibTr.entry(this, TRACE, methodName);
            SibTr.exit(this, TRACE, methodName, Boolean.FALSE);
        }
        return false;

    }

    /**
     * Returns the maximum number of active messages that should be associated
     * with this listener at any one time.
     *
     * @return int max active messages
     */
    int getMaxActiveMessages()
    {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            final String methodName = "getMaxActiveMessages";
            SibTr.entry(this, TRACE, methodName);
        }

        // The field max active messages is used to represent the default max 
        // active messages, however, on distributed systems we ignore this 
        // setting (as we will control the concurrency ourselves) and return
        // 0 - meaning there is no limit (or 1 if strict message ordering is on).
        // This method is called by the base class constructor when it registers
        // the async consumer with processor.
        int maxActiveMsgs = _strictMessageOrdering ? 1 : 0;

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
          final String methodName = "getMaxActiveMessages";
          SibTr.exit(this, TRACE, methodName, maxActiveMsgs);
        }
        return maxActiveMsgs;
    }

    /**
     * Returns the expiry time for message locks.
     *
     * @return zero to indicate that message locks should not be used
     */
    long getMessageLockExpiry() {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            final String methodName = "getMessageLockExpiry";
            SibTr.entry(this, TRACE, methodName);
            SibTr.exit(this, TRACE, methodName, "0");
        }
        return 0;

    }

    /**
     * Returns a string generator containing the fields for this class.
     *
     * @return a string generator
     */
    protected SibRaStringGenerator getStringGenerator() {

        final SibRaStringGenerator generator = super.getStringGenerator();
        generator.addField("workManager", _workManager);
        generator.addField("maxActiveMessages", _maxActiveMessages);
        return generator;

    }

    /**
     * Work implementation that creates a dispatcher when run.
     */
    private final class SibRaWork implements Work, WorkListener {

        /**
         * The dispatchableMessages to pass to the dispatcher.
         */
        private List _messages;

        /**
         * The dispatcher created when initially run.
         */
        private SibRaDispatcher _dispatcher;

        /**
         * The XD scheduler associated with this work
         */
        private AtomicReference<AsynchDispatchScheduler> _asynchDispatchSchedulerRef = new AtomicReference<AsynchDispatchScheduler>();

        /**
         * The SibRaListener object that needs to be passed to the
         * dispatchers.
         */
        private AtomicReference<SibRaListener> _listenerRef = new AtomicReference<SibRaListener>();

        /**
         * Schedules the given list of messages for dispatch.
         *
         * @param messages
         *            the messages to dispatch
         * @param asynchDispatchScheduler
         *        the XD asynchDispatchScheduler
         * @param listener
         *            the listener consuming the messages
         */
        public void schedule(final List messages, final AsynchDispatchScheduler asynchDispatchScheduler,
                                SibRaListener listener) {

            final String methodName = "schedule";
            if (TraceComponent.isAnyTracingEnabled() && WORK_TRACE.isEntryEnabled()) {
                SibTr.entry(this, WORK_TRACE, methodName, new Object[] { messages, asynchDispatchScheduler, listener } );
            }

            _messages = messages;
            _asynchDispatchSchedulerRef.set(asynchDispatchScheduler);
            _listenerRef.set(listener);

            try {

                /*
                 * Schedule work
                 */

                _workManager.scheduleWork(this, WorkManager.INDEFINITE, null,
                        this);

            } catch (final WorkException exception) {

                FFDCFilter.processException(exception, WORK_CLASS_NAME + "."
                        + methodName, "1:559:1.42", this);
                if (TraceComponent.isAnyTracingEnabled() && WORK_TRACE.isEventEnabled()) {
                    SibTr.exception(this, WORK_TRACE, exception);
                }
                unlockMessages(false);

            }

            if (TraceComponent.isAnyTracingEnabled() && WORK_TRACE.isEntryEnabled()) {
                SibTr.exit(this, WORK_TRACE, methodName);
            }

        }

        /**
         * Called by the work manager to run this work item. Creates a
         * dispatcher for the message ids.
         */
        public synchronized void run() {

            final String methodName = "run";
            if (TraceComponent.isAnyTracingEnabled() && WORK_TRACE.isEntryEnabled()) {
                SibTr.entry(this, WORK_TRACE, methodName);
            }

            try {

                if (_dispatcher == null) {

                    _dispatcher = _connection.createDispatcher(_session, _unrecoverableReliability, _maxFailedDeliveries, _sequentialFailureThreshold);

                }

                _dispatcher.dispatch(_messages, _asynchDispatchSchedulerRef.get(), _listenerRef.get());

            } catch (final IllegalStateException exception) {

                // No FFDC code needed

                if (TraceComponent.isAnyTracingEnabled() && WORK_TRACE.isEventEnabled()) {
                    SibTr.exception(this, WORK_TRACE, exception);
                }

                // Ignore - the endpoint has been deactivated since the work was
                // submitted

            } catch (final Throwable throwable) {

                FFDCFilter.processException(throwable, WORK_CLASS_NAME + "."
                        + methodName, "1:608:1.42", this);
                if (TraceComponent.isAnyTracingEnabled() && WORK_TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, throwable);
                }

            }
            finally
            {
              if (_dispatcher != null)
              {
                _connection.closeDispatcher(_dispatcher);
              }
            }

            if (TraceComponent.isAnyTracingEnabled() && WORK_TRACE.isEntryEnabled()) {
                SibTr.exit(this, WORK_TRACE, methodName);
            }

        }

        /**
         * Called by the work manager on a running work iterm to indicate that
         * it should stop as soon as possible. Cancels the dispatcher.
         */
        public synchronized void release() {

            final String methodName = "release";
            if (TraceComponent.isAnyTracingEnabled() && WORK_TRACE.isEntryEnabled()) {
                SibTr.entry(this, WORK_TRACE, methodName);
            }

            if (_dispatcher != null) {
                _dispatcher.cancel();
            }

            if (TraceComponent.isAnyTracingEnabled() && WORK_TRACE.isEntryEnabled()) {
                SibTr.exit(this, WORK_TRACE, methodName);
            }

        }

        /**
         * Invoked by the work manager when a piece of work has been accepted.
         * Increments the work count and stops the session if the maximum
         * concurrency has been reached.
         *
         * @param workEvent
         *            the work event
         */
        public void workAccepted(final WorkEvent workEvent) {

            final String methodName = "workAccepted";
            if (TraceComponent.isAnyTracingEnabled() && WORK_TRACE.isEntryEnabled()) {
                SibTr.entry(this, WORK_TRACE, methodName, workEvent);
            }
            synchronized (_workCountLock) {

                _workCount++;

                /*
                 * Stop session if maximum concurrency has been reached
                 * We only set the flag here, and will perform the actual
                 * stop later. This is because we may not hold the processor
                 * AsynchConsumerBusy lock at this point, in which case we
                 * would deadlock. We know this code is only ever called from
                 * within internalConsumeMessages(), so the actual stop is
                 * performed in consumeMessages() after internalConsumeMessages()
                 * completes.
                 */
                
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                    SibTr.debug(TRACE, "_workCount: " + _workCount + " _maxWorkCount: " + _maxWorkCount);
                }

                if (_workCount == _maxWorkCount) {
                	sibPacingSessionStarted = false;
                }

            }
            
            if (TraceComponent.isAnyTracingEnabled() && WORK_TRACE.isEntryEnabled()) {
                SibTr.exit(this, WORK_TRACE, methodName);
            }

        }

        /**
         * Invoked by the work manager when a piece of work has been rejected.
         * Unlocks the messages assigned to that piece of work and then calls
         * <code>workEnded</code>.
         *
         * @param workEvent
         *            the work event
         */
        public void workRejected(final WorkEvent workEvent) {

            final String methodName = "workRejected";
            if (TraceComponent.isAnyTracingEnabled() && WORK_TRACE.isEntryEnabled()) {
                SibTr.entry(this, WORK_TRACE, methodName, workEvent);
            }

            unlockMessages(false);
            workEnded();

            if (TraceComponent.isAnyTracingEnabled() && WORK_TRACE.isEntryEnabled()) {
                SibTr.exit(this, WORK_TRACE, methodName);
            }

        }

        /**
         * Invoked by the work manager when a piece of work has been started.
         *
         * @param workEvent
         *            the work event
         */
        public void workStarted(final WorkEvent workEvent) {

            if (TraceComponent.isAnyTracingEnabled() && WORK_TRACE.isEntryEnabled()) {
                final String methodName = "workStarted";
                SibTr.entry(this, WORK_TRACE, methodName, workEvent);
                SibTr.exit(this, WORK_TRACE, methodName);
            }

        }

        /**
         * Invoked by the work manager when a piece of work has been completed.
         * Calls <code>workEnded</code>.
         *
         * @param workEvent
         *            the work event
         */
        public void workCompleted(final WorkEvent workEvent) {

            final String methodName = "workCompleted";
            if (TraceComponent.isAnyTracingEnabled() && WORK_TRACE.isEntryEnabled()) {
                SibTr.entry(this, WORK_TRACE, methodName, workEvent);
            }

            workEnded();

            if (TraceComponent.isAnyTracingEnabled() && WORK_TRACE.isEntryEnabled()) {
                SibTr.exit(this, WORK_TRACE, methodName);
            }

        }

        /**
         * Pushes this work item onto the free stack. Decrements the count of
         * work items and, if it has just dropped below the maximum, restart the
         * session.
         */
        private void workEnded() {

            final String methodName = "workEnded";
            if (TraceComponent.isAnyTracingEnabled() && WORK_TRACE.isEntryEnabled()) {
                SibTr.entry(this, WORK_TRACE, methodName);
            }
            
            synchronized (_workCountLock) {
            	
            	if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                    SibTr.debug(TRACE, "_workCount: " + _workCount + " _maxWorkCount: " + _maxWorkCount);
                }
            	
            	synchronized(insideConsumeMessagesLock)
            	{
	                if (_workCount == _maxWorkCount) {
	
	                    try {
	                    	sibPacingSessionStarted = true;
	                    	
	                    	// If we're insideConsumeMessages, then we're dirty. Any number of start/stop
	                    	// operations could be going on - and we don't want to risk deadlocking.
	                    	// Therefore we don't do anything, just set the flag (above). consumeMessages()
	                    	// will stop the session as required once it has finished - it's implicitly
	                    	// started.
	                    	if(!insideConsumeMessages)
	                    	{
		                        final boolean deliverImmediately = false;
		                        startSession(deliverImmediately);
	                    	}
	
	                    } catch (final SIException exception) {
	
	                        FFDCFilter.processException(exception, WORK_CLASS_NAME
	                                + "." + methodName, "1:795:1.42", this);
	                        if (WORK_TRACE.isEventEnabled()) {
	                            SibTr.exception(this, WORK_TRACE, exception);
	                        }
	
	                    } catch (final SIErrorException exception) {
	
	                        FFDCFilter.processException(exception, WORK_CLASS_NAME
	                                + "." + methodName, "1:803:1.42", this);
	                        if (WORK_TRACE.isEventEnabled()) {
	                            SibTr.exception(this, WORK_TRACE, exception);
	                        }
	
	                    }
	
	                }
            	}

                _workCount--;

            }
            
            if (TraceComponent.isAnyTracingEnabled() && WORK_TRACE.isEntryEnabled()) {
                SibTr.exit(this, WORK_TRACE, methodName);
            }

        }

        /**
         * Unlocks the current messages.
         */
        private void unlockMessages(boolean incrementDeliveryCount) {

            final String methodName = "unlockMessages";
            if (TraceComponent.isAnyTracingEnabled() && WORK_TRACE.isEntryEnabled()) {
                SibTr.entry(this, WORK_TRACE, methodName);
            }

            /*
             * Only attempt to unlock messages once
             */

            if (_messages != null) {

                List<SIBusMessage> listOfSIBusMessages = new ArrayList<SIBusMessage> ();
                try {
                    for (int i = 0; i < _messages.size(); i++)
                    {
                      listOfSIBusMessages.add(((DispatchableMessage)_messages.get(i)).getMessage());
                    }
                    _session.unlockSet(getMessageHandles(listOfSIBusMessages), incrementDeliveryCount);

                } catch (final SIException exception) {

                    FFDCFilter.processException(exception, WORK_CLASS_NAME
                            + "." + methodName, "1:850:1.42", this);
                    if (TraceComponent.isAnyTracingEnabled() && WORK_TRACE.isEventEnabled()) {
                        SibTr.exception(this, WORK_TRACE, exception);
                    }

                } catch (final SIErrorException exception) {

                    FFDCFilter.processException(exception, WORK_CLASS_NAME
                            + "." + methodName, "1:858:1.42", this);
                    if (TraceComponent.isAnyTracingEnabled() && WORK_TRACE.isEventEnabled()) {
                        SibTr.exception(this, WORK_TRACE, exception);
                    }

                }

                _messages = null;

            }

            if (TraceComponent.isAnyTracingEnabled() && WORK_TRACE.isEntryEnabled()) {
                SibTr.exit(this, WORK_TRACE, methodName);
            }

        }

        public String toString() {

            final SibRaStringGenerator generator = new SibRaStringGenerator(
                    this);
            generator.addField("messages", _messages);
            generator.addField("dispatcher", _dispatcher);
            generator.addField("deleteUnrecoverableMessages",
                    _deleteUnrecoverableMessages);
            generator.addParent("SibRaSingleProcessListener.this",
                    SibRaSingleProcessListener.this);
            return generator.getStringRepresentation();

        }

    }

}
