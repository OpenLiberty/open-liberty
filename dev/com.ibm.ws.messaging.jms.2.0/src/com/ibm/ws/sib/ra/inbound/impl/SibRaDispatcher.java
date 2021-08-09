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

package com.ibm.ws.sib.ra.inbound.impl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ejbcontainer.mdb.MDBMessageEndpointFactory;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.ra.impl.SibRaUtils;
import com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration;
import com.ibm.ws.sib.ra.inbound.SibRaEndpointInvoker;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.AbstractConsumerSession;
import com.ibm.wsspi.sib.core.BifurcatedConsumerSession;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SIMessageHandle;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.SIUncoordinatedTransaction;
import com.ibm.wsspi.sib.core.exception.SIMessageNotLockedException;
import com.ibm.wsspi.sib.pacing.AsynchDispatchScheduler;
import com.ibm.wsspi.sib.pacing.MessagePacingControl;
import com.ibm.wsspi.sib.pacing.MessagePacingControlFactory;

/**
 * Dispatcher for delivering messages to a message endpoint.
 */
abstract class SibRaDispatcher {

    /**
     * Flag indicating whether the dispatcher has been cancelled. When set to
     * <code>true</code> the dispatcher should stop delivering messages.
     */
    private volatile boolean _cancelled = false;

    /**
     * The connection to the messaging engine.
     */
    protected final SICoreConnection _connection;
    
    /**
     * The RA's SICoreConnection wrapper
     */
    protected final SibRaMessagingEngineConnection _meConnection;

    /**
     * The session from which the messages were received.
     */
    protected final AbstractConsumerSession _session;

    /**
     * The factory on which to create the message endpoint.
     */
    protected final MessageEndpointFactory _endpointFactory;

    /**
     * The invoker used to deliver the message to the endpoint.
     */
    protected final SibRaEndpointInvoker _invoker;

    /**
     * The configuration for the endpoint.
     */
    protected final SibRaEndpointConfiguration _endpointConfiguration;

    /**
     * Flag indicating whether messages that have their <code>reliability</code>
     * value set to lower than the sessions <code>unrecoveredReliability</code>
     * and as such have already been deleted in <code>consumeMessages</code>.
     * This is <code>true</code> if the endpoint method is non-transactional.
     */
    protected boolean _deleteUnrecoverableMessages;

    /**
     * The unrecovered reliability that the consumer session was created with.
     */
    protected Reliability _unrecoverableReliability;

    /**
     * The request metrics singleton.
     */
  //  private final SIBPmiRm _requestMetrics;

    protected HashMap _reliabilityPreInvoke = new HashMap();

    // This property is a static as its per server and its an expensive call to obtain this information.
    // If this property is null then we'll read the property in the constructor. If no property was found 
    // then we set this value to the empty string to prevent it from being read again (since its an 
    // expensive call).
    private static String _debugMEName = null;

    /**
     * The maximum number of failed deliveries allowed
     */
    private int _maxFailedDeliveries;
    
    /**
     * The number of sequential message failures so far
     */
    private int _sequentialFailureMessageCount = 0;
    
    /**
     * The configure threshold for the number of sequential failures.
     * When this is reached a warning message will be omitted.
     */
    private int _sequentialFailureThreshold;
    
    /**
     * The component to use for trace.
     */
    private static TraceComponent TRACE = SibRaUtils
            .getTraceComponent(SibRaDispatcher.class);

    /**
     * Provides access to NLS enabled messages.
     */
    private static TraceNLS NLS = SibRaUtils.getTraceNls();

    /**
     * The name of this class.
     */
    private static final String CLASS_NAME = SibRaDispatcher.class.getName();

    // Cache these for performance
    private Class _mepClass;
    private Method _mepGetJ2EEMethod;
    private String _mepJ2EEName;

    static void resetMEName ()
    {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) 
        {
            SibTr.debug(TRACE, "Resetting MEName");
        }
      _debugMEName = null;
    }

    /**
     * Constructor. Creates an endpoint and obtains the request metrics
     * singleton.
     *
     * @param connection
     *            the parent connection for the session
     * @param session
     *            the session from which the messages were received
     * @param endpointActivation
     *            the endpoint activation which lead to these messages being
     *            received
     * @param unrecoveredReliability
     *            the unrecoveredReliability value for the destination
     * @param maxFailedDeliveries
     *            the maxFailedDeliveries value for the destination
     * @param sequentialFailureThreshold
     *            the sequentialFailureThreshold value for the destination
     * @throws ResourceAdapterInternalException
     *             if the request metrics instance could not be obtained
     * @throws ResourceException
     *             if the endpoint could not be created
     */
    SibRaDispatcher(final SibRaMessagingEngineConnection connection,
            final AbstractConsumerSession session,
            final SibRaEndpointActivation endpointActivation,
            final Reliability unrecoveredReliability,
            final int maxFailedDeliveries,
            final int sequentialFailureThreshold)
            throws ResourceException {

        final String methodName = "SibRaDispatcher";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { connection,
                session, endpointActivation, unrecoveredReliability, maxFailedDeliveries, 
                sequentialFailureThreshold });
        }

        // By default best effort non persistant messages are not deleted.
        // This can be altered by classes deriving from this class
        _deleteUnrecoverableMessages = false;

        _meConnection = connection;
        _connection = connection.getConnection();
        _session = session;
        _endpointConfiguration = endpointActivation.getEndpointConfiguration();
        _invoker = endpointActivation.getEndpointInvoker();
        _endpointFactory = endpointActivation.getMessageEndpointFactory();
        _unrecoverableReliability = unrecoveredReliability;
        _sequentialFailureThreshold = sequentialFailureThreshold;
        _maxFailedDeliveries = maxFailedDeliveries;

        // Obtain the test flag indicating if the ME name that the MDB is running on should be added to the
        // JMS message (this does nothing for core SPI RA).
        if (_debugMEName == null)
        {
        	// chetan liberty code change
        	// since we dont have custom property in liberty, i am setting _debugMEName empty
        	// later if required we can get the mename if required.
        	// Code required for testing. Check to see if a flag is set, if so we will put extra data into the message
        	// being given to the MDB
        	_debugMEName = "";

        	if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) 
        	{
        		SibTr.debug(this, TRACE, "MEName set to " + _debugMEName);
        	}
        }
        // End of obtaining test flag section

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }


    /**
     * Checks if we should process more work or not.
     * @return true if the work has been canceled or the connection to the ME has been closed
     */
    private boolean isCancelled ()
    {
        return (_cancelled || _meConnection.isClosed());
    }

    /**
     * Dispatches the given message to the message endpoint. zOS only
     *
     * @param message
     *            the message to dispatch (non transactional mdbs will have already read
     *            the message but transactional mdbs read the message in this method).
     * @param messageHandle
     *                    the handle to the message, this can be used to obtain the real
     *            message.
     * @param handlerContext
     *            the handler context map
     * @throws ResourceException
     *             if the dispatch fails
     */
    synchronized void dispatch(final SIMessageHandle messageHandle, SIBusMessage message,
            final Map handlerContext) throws ResourceException {

        final String methodName = "dispatch";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { messageHandle, message,
                    handlerContext });
        }

        if (_session == null) {

            /*
             * The dispatcher has been closed - return safe in the knowledge
             * that the message will have already been unlocked by the close of
             * the session
             */

            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
                SibTr.exit(this, TRACE, methodName);
            }
            return;

        }

        boolean delivered = false;

        /**
         * The endpoint to deliver the message to.
         */
        MessageEndpoint endpoint = null;

        try {

          /**
           * Obtain The endpoint to deliver the message to.
           */
          endpoint = createEndpoint();

            /*
             * Obtain information for request metrics and handlers
             */

            final SIDestinationAddress destination = _session
                    .getDestinationAddress();
            final String busName = destination.getBusName();
            final String meName = _connection.getMeName();
            final String destinationName = destination.getDestinationName();
            final String messageSelector = _endpointConfiguration
                    .getMessageSelector();
            final ActivationSpec activationSpec = _endpointConfiguration
                    .getActivationSpec();

            boolean success = false;
            // Only proceed if not cancelled and handler invocation is
            // successful
            if (!isCancelled ()) {

                try {

                    beforeDelivery(endpoint);

                    delivered = true;

                    try {

                      // 304501.ra - Call the pre MDB invoke on XD
                      MessagePacingControl mpc = MessagePacingControlFactory.getInstance();
                      if (mpc != null && mpc.isActive()) {

                        mpc.preMdbInvoke(this, null);

                      }

                      // If we are transactional we won't have read the message yet so perform a read and
                      // delete inside the transaction.
                      if (message == null)
                      {
                        message = readMessage (messageHandle); // null op for non trans mdb's
                      }

                      _reliabilityPreInvoke.put(messageHandle, message.getReliability());

                      success = _invoker.invokeEndpoint(endpoint,
                          message, _session, getTransaction(), _debugMEName);

                      // 304501.ra - Call the post MDB invoke method on XD
                      if (mpc != null && mpc.isActive()) {

                        mpc.postMdbInvoke (this, null);

                      }
                    } catch (final SIMessageNotLockedException exception) {

                        // No FFDC code needed

                                                /*
                                                 * Message lock has expired and message will be
                                                 * reassigned
                                                 */

                                                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                                                        SibTr.exception(this, TRACE, exception);
                                                }

                                    /*
                                     * Message lock has expired and message will be reassigned so drop out of dispatch
                                     */
                                                return;

                    } finally {

                        afterDelivery(message, endpoint, success);

                    }

                } finally {


                    // Call isTransactionRolledBack to clear up
                    // the data stored in the SibRaXaResource hashtable even if
                    // we are not using PMI
                    boolean tranRolledBack = isTransactionRolledBack ();

                    // Success should only be true if the endpoint
                    // was successfully invoked and the transaction
                    // was not rolled back.
                      success = success && !tranRolledBack;
 
                      if (success)
                      {
                        //This message was sucessfully delivered so reset all sequential message counters
                        clearSequentialFailureMessageCount();
                      }
                      else
                      {
                        // Make sure we have a message and the redelivered count exists
                        if ((message != null) && (((JsMessage)message).getRedeliveredCount() != null))
                        {
                            // message failed check its redelivery threshold
                            // we need to check for n - 1 where n is the max failed deliveries. The redeliveredCount means the 
                            // number of REdeliveries so we need to add one to this to align with the maxfaileddeliveries count.
                            if ( (((JsMessage)message).getRedeliveredCount().intValue() + 1) == (_maxFailedDeliveries - 1))
                            {
                              //We are about to hit the threshold
                              addToSequentialFailureMessageCount();
                            }
                        }
                        else
                        {
                            if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) 
                            {
                                if (null == message)
                                {
                                    SibTr.debug(TRACE, "The message was null");
                                }
                                else
                                {
                                    SibTr.debug(TRACE, "Unable to obtain the redelivered count from message " + message);
                                }
                            }
                        }
                      }
                  }

            } else {

              // One or more of the handlers failed. Force a increment on the
              // retry count of the message.
                if (!isCancelled ())
                {

                  if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                        SibTr.debug(this, TRACE, "An RA Handler failed - increasing retry count on message by rolling back a delete on the message:",
                                    message);
                  }

                increaseRetryCount(messageHandle);

                // Set delivered to be true so that we don't try and unlock it later.
                // The process of deleting and rolling back will unlock the mesage.
                delivered = true;

                }
            }

        } finally {

          try {

            cleanup();

            // If possible, unlock an undelivered message

            if ((!delivered) && (message!= null)) {

                if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                            SibTr.debug(this, TRACE, "Unlocking undelivered message:",
                                            message);
                }

                try {

                // If we do not delete unrecoverable messages
                // or if we do delete unrecoverable message but
                // this particular message is recoverable then unlock the
                // message.
                boolean canDeleteMessage = (_unrecoverableReliability.compareTo(message.getReliability ()) >= 0);
                if ((!_deleteUnrecoverableMessages)
                        || (!(canDeleteMessage))) {

                    _session.unlockSet(new SIMessageHandle[] { message
                            .getMessageHandle() });
                }

              } catch (final SIException exception) {

                // No FFDC code needed
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }

              }

            }

          } finally {

            if (endpoint != null) {

                endpoint.release ();

            }

          }

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Dispatches the given messages to the message endpoint.
     *
     * @param messages
     *            the messages to dispatch
     * @throws ResourceException
     *             if the dispatch fails
     */
    synchronized void dispatch(final List messages, AsynchDispatchScheduler scheduler, SibRaListener listener) throws ResourceException {

    	final String methodName = "dispatch";
    	if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
    		SibTr.entry(this, TRACE, methodName, new Object[] {messages, scheduler, listener});
    	}

    	if (_session == null) {

    		/*
    		 * The dispatcher has been closed - return safe in the knowledge
    		 * that the messages will have already been unlocked by the close of
    		 * the session
    		 */

    		if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
    			SibTr.exit(this, TRACE, methodName);
    		}
    		return;

    	}

    	/*
    	 * Keep a list of the messages for which delivery has not been attempted
    	 */

    	final List undeliveredDispatchableMessages = new ArrayList(messages);


    	/**
    	 * The endpoint to deliver the message to.
    	 */
    	MessageEndpoint endpoint = null;

    	try {

    		/*
    		 * Obtain an endpoint
    		 */
    		endpoint = createEndpoint();

    		/*
    		 * Obtain information for request metrics and handlers
    		 */
    		final SIDestinationAddress destination = _session
    				.getDestinationAddress();
    		final String busName = destination.getBusName();
    		final String meName = _connection.getMeName();
    		final String destinationName = destination.getDestinationName();
    		final String messageSelector = _endpointConfiguration
    				.getMessageSelector();
    		final ActivationSpec activationSpec = _endpointConfiguration
    				.getActivationSpec();

    		for (final Iterator iterator = messages.iterator(); iterator
    				.hasNext()
    				&& !isCancelled ();) {

    			final DispatchableMessage dispatchableMessage = (DispatchableMessage) iterator.next();

    			final SIBusMessage message = dispatchableMessage.getMessage();
    			boolean success = false;
    			try {

    				beforeDelivery(message, endpoint);

    				undeliveredDispatchableMessages.remove(dispatchableMessage);


    				try {

    					// 304501.ra - Call the pre MDB invoke on XD
    					MessagePacingControl mpc = MessagePacingControlFactory.getInstance();
    					if (mpc != null && mpc.isActive()) {

    						mpc.preMdbInvoke(this, scheduler);

    					}

    					_reliabilityPreInvoke.put(message.getMessageHandle(), message.getReliability());

    					success = _invoker.invokeEndpoint(endpoint,
    							message, _session, getTransaction(), _debugMEName);

    					// 304501.ra - Call the post MDB invoke method on XD
    					if (mpc != null && mpc.isActive()) {

    						mpc.postMdbInvoke (this, scheduler);

    					}

    				} finally {

    					afterDelivery(message, endpoint, success);

    				}

    			} finally {


    				// Call isTransactionRolledBack to clear up
    				// the data stored in the SibRaXaResource hashtable even if
    				// we are not using PMI
    				boolean tranRolledBack = isTransactionRolledBack ();

    				// Success should only be true if the endpoint
    				// was successfully invoked and the transaction
    				// was not rolled back.
    				success = success && !tranRolledBack;

    				if (success)
    				{
    					//This message was sucessfully delivered so reset all sequential message counters
    					clearSequentialFailureMessageCount();
    				}
    				else
    				{
    					// Make sure we have a message and the redelivered count exists
    					if ((message != null) && (((JsMessage)message).getRedeliveredCount() != null))
    					{
    						// message failed check its redelivery threshold
    						// we need to check for n - 1 where n is the max failed deliveries. The redeliveredCount means the 
    						// number of REdeliveries so we need to add one to this to align with the maxfaileddeliveries count.
    						if ( (((JsMessage)message).getRedeliveredCount().intValue() + 1) == (_maxFailedDeliveries - 1))
    						{
    							//We are about to hit the threshold
    							addToSequentialFailureMessageCount();
    						}
    					}
    					else
    					{
    						if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) 
    						{
    							if (null == message)
    							{
    								SibTr.debug(TRACE, "The message was null");
    							}
    							else
    							{
    								SibTr.debug(TRACE, "Unable to obtain the redelivered count from message " + message);
    							}
    						}
    					}
    				}

    			}


    		}

    	} finally {

    		cleanup();

    		try {

    			// If possible, unlock any undelivered messages

    			if (undeliveredDispatchableMessages.size() > 0) {

    				try {

    					// If we delete best effort non persistent messages then
    					// remove any best effort non persitent messages from the
    					// list.
    					if (_deleteUnrecoverableMessages) {

    						for (final Iterator iterator = undeliveredDispatchableMessages
    								.iterator(); iterator.hasNext();) {

    							final DispatchableMessage dispatchableMsg = (DispatchableMessage) iterator.next();
    							final SIBusMessage msg = dispatchableMsg.getMessage();

    							if (_unrecoverableReliability.compareTo(msg.getReliability ()) >= 0) {

    								if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
    									SibTr.debug(this,
    											TRACE,
    											"Removing the following message as it has already been deleted: ",
    											msg);
    								}

    								iterator.remove();

    							}

    						}

    					}

    					// Make sure that we still have some undelivered messages
    					// since they may have all been deleted
    					if (undeliveredDispatchableMessages.size() > 0) {

    						if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
    							SibTr.debug(this, TRACE,
    									"Unlocking undelivered messages:",
    									undeliveredDispatchableMessages);
    						}

    						unlockMessages(undeliveredDispatchableMessages, false);

    					}

    				} catch (final ResourceException exception) {

    					// No FFDC code needed
    					if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
    						SibTr.exception(this, TRACE, exception);
    					}

    				}

    			}

    		} finally {

    			if (endpoint != null) {

    				endpoint.release();

    			}
    		}


    	}

    	if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
    		SibTr.exit(this, TRACE, methodName);
    	}

    }

    /**
     * Closes the session associated with this dispatcher and releases the
     * message endpoint.
     */
    void close() {

        final String methodName = "close";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        /*
         * Cancel any ongoing dispatch
         */
        _cancelled = true;

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Cancels this dispatcher. The dispatcher should not begin to deliver any
     * more messages after this method returns but may still be in the process
     * of delivering a message.
     */
    void cancel() {

        final String methodName = "cancel";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        _cancelled = true;

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Unlocks a list of messages.
     *
     * @param messageList
     *            the messages to unlock
     * @throws ResourceException
     *             if the unlock fails
     */
    protected final void unlockMessages(final List messageList, boolean incrementDeliveryCount)
            throws ResourceException {

        final String methodName = "unlockMessages";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, messageList);
        }

        if (messageList.size() > 0) {

            List listOfSIBusMessages = new ArrayList();
            try {
                  for (int i = 0; i < messageList.size(); i++)
                  {
                    listOfSIBusMessages.add(((DispatchableMessage)messageList.get(i)).getMessage());
                  }
                _session.unlockSet(getMessageHandles(listOfSIBusMessages), incrementDeliveryCount);

            } catch (final SIException exception) {

                FFDCFilter.processException(exception, CLASS_NAME + "."
                        + methodName, "1:1200:1.73", this);
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }
                throw new ResourceAdapterInternalException(NLS
                        .getFormattedMessage(("UNLOCK_EXCEPTION_CWSIV0601"),
                                new Object[] { exception, listOfSIBusMessages }, null),
                        exception);

            } catch (final SIErrorException exception) {

                FFDCFilter.processException(exception, CLASS_NAME + "."
                        + methodName, "1:1212:1.73", this);
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }
                throw new ResourceAdapterInternalException(NLS
                        .getFormattedMessage(("UNLOCK_EXCEPTION_CWSIV0601"),
                                new Object[] { exception, listOfSIBusMessages }, null),
                        exception);

            }
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Read and deletes the message under the given transaction.
     *
     * @param handle
     *            the handle of the message to read and delete
     * @param transaction
     *            the transaction to delete it under, if any
     * @return The message that has been read from the supplied handle
     * @throws ResourceException
     *             if the delete fails
     */
    protected final SIBusMessage readAndDeleteMessage(SIMessageHandle handle,
            SITransaction transaction) throws ResourceException, SIMessageNotLockedException {

        final String methodName = "deleteMessage";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { handle,
                    transaction });
        }

        SIBusMessage message = null;

        if (_session instanceof BifurcatedConsumerSession) {

            try {

                if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled ()) {
                        SibTr.debug(this, TRACE,
                                        "We have a bifucated consumer session so attempting to perform readAndDelete");
                }

                SIBusMessage [] messageList = ((BifurcatedConsumerSession) _session).readAndDeleteSet(new SIMessageHandle[] { handle }, transaction);

                if (messageList.length != 1) {

                    throw new ResourceAdapterInternalException(NLS.getFormattedMessage(
                            ("MESSAGE_LIST_INCORRECT_CWSIV0607"), new Object[] { "" + messageList.length, messageList,
                                        handle, transaction }, null));

                }

                if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled ()) {
                        SibTr.debug(this, TRACE,
                                        "Getting the message from the message list");
                }

                message = messageList [0];

            } catch (SIMessageNotLockedException exception) {

                // No FFDC code needed

                                /*
                                 * Message lock has expired and message will be
                                 * reassigned. Throw this back up to the called to
                                 * catch.
                                 */
                throw exception;

            }catch (final SIException exception) {

                FFDCFilter.processException(exception, CLASS_NAME + "."
                        + methodName, "1:1292:1.73", this);
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }
                throw new ResourceAdapterInternalException(NLS.getFormattedMessage(
                        ("DELETE_EXCEPTION_CWSIV0608"), new Object[] { exception,
                                        handle, transaction }, null), exception);

            } catch (final SIErrorException exception) {

                FFDCFilter.processException(exception, CLASS_NAME + "."
                        + methodName, "1:1303:1.73", this);
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }
                throw new ResourceAdapterInternalException(NLS.getFormattedMessage(
                        ("DELETE_EXCEPTION_CWSIV0608"), new Object[] { exception,
                                        handle, transaction }, null), exception);

            }

        } else {

            throw new ResourceAdapterInternalException(NLS.getFormattedMessage(
                    ("INVALID_SESSION_CWSIV0606"),
                                        new Object[] { _session, handle, transaction, BifurcatedConsumerSession.class.getName () },
                                        null));

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName, message);
        }

        return message;

    }

    /**
     * Deletes the given message under the given transaction.
     *
     * @param message
     *            the message to delete
     * @param transaction
     *            the transaction to delete it under, if any
     * @throws ResourceException
     *             if the delete fails
     */
    protected final void deleteMessage(SIBusMessage message,
            SITransaction transaction) throws ResourceException {

        final String methodName = "deleteMessage";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { message,
                    transaction });
        }

        deleteMessages (new SIMessageHandle [] { message.getMessageHandle () }, transaction);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    protected final void deleteMessages (final SIMessageHandle [] handles,
                final SITransaction transaction) throws ResourceException {

        final String methodName = "deleteMessages";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { handles,
                    transaction });
        }
        try {

            _session.deleteSet(handles, transaction);

        } catch (final SIException exception) {

            FFDCFilter.processException(exception, CLASS_NAME + "."
                    + methodName, "1:1372:1.73", this);
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
            throw new ResourceAdapterInternalException(NLS
                    .getFormattedMessage(
                            ("DELETE_SET_EXCEPTION_CWSIV0603"),
                            new Object[] { exception, handles,
                                    transaction }, null), exception);

        } catch (final SIErrorException exception) {

            FFDCFilter.processException(exception, CLASS_NAME + "."
                    + methodName, "1:1385:1.73", this);
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
            throw new ResourceAdapterInternalException(NLS
                    .getFormattedMessage(
                            ("DELETE_SET_EXCEPTION_CWSIV0603"),
                            new Object[] { exception, handles,
                                    transaction }, null), exception);

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
                SibTr.exit(this, TRACE, methodName);
        }
    }

    /**
     * Returns an array of message handles for the given list of messages.
     *
     * @param messageList
     *            the list of messages
     * @return an array of message handles
     */
    protected static final SIMessageHandle[] getMessageHandles(
            final List messageList) {

        final String methodName = "getMessageIds";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(TRACE, methodName, messageList);
        }

        final SIMessageHandle[] msgHandles = new SIMessageHandle[messageList
                .size()];
        for (int i = 0; i < messageList.size(); i++) {
            SIBusMessage message = (SIBusMessage) messageList.get(i);
            msgHandles[i] = message.getMessageHandle();
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(TRACE, methodName, msgHandles);
        }
        return msgHandles;

    }

    /**
     * Returns the transaction, if any, used to deliver the message.
     *
     * @return the transaction
     * @throws ResourceException
     *             if the transaction could not be obtained
     */
    abstract protected SITransaction getTransaction() throws ResourceException;

    /**
     * Creates an endpoint.
     *
     * @return the endpoint
     * @throws ResourceException
     *             if the endpoint could not be created
     */
    abstract protected MessageEndpoint createEndpoint()
            throws ResourceException;

    /**
     * Invoked before delivery of a message (distributed version).
     *
     * @param message
     *            the message that is about to be delivered
     * @throws ResourceException
     *             if before delivery failed
     */
    abstract protected void beforeDelivery(SIBusMessage message, MessageEndpoint endpoint)
            throws ResourceException;

    /**
     * Reads the message from the supplied message handle
     * @param handle The handle of the message the read
     * @return The message associated with the supplied handle
     * @throws ResourceException if there was a problem reading the message
     * @throws SIMessageNotLockedException if the message has expired
     */
    abstract SIBusMessage readMessage (SIMessageHandle handle) throws ResourceException, SIMessageNotLockedException;

    /**
     * Invoked before delivery of a message (zos version).
     *
     * @param message
     *            the message that is about to be delivered
     * @throws ResourceException
     *             if before delivery failed
     */
    abstract protected void beforeDelivery(MessageEndpoint endpoint)
            throws ResourceException;

    /**
     * Invoked after delivery of a message.
     *
     * @param message
     *            the message that was delivered
     * @param success
     *            flag indicating whether delivery was successful
     * @throws ResourceException
     *             if after delivery failed
     */
    abstract protected void afterDelivery(SIBusMessage message, MessageEndpoint endpoint,
                                          boolean success)
            throws ResourceException;

    /**
     * Invoked after all messages in the batch have been delivered.
     */
    abstract protected void cleanup();

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    public final String toString() {

        return getStringGenerator().getStringRepresentation();

    }

    /**
     * Returns true if the transaction was rolled back. By default there are no
     * transactions and hence there was no roll back
     *
     * @return If the transaction was rolled back
     */
    protected boolean isTransactionRolledBack () {

        return false;
        
    }

    
    /**
     * This method is used as a workaround to increase the retry count on a message.
     * The better solution is to allow us to unlock a message passing in a parameter
     * which states if we wish to increase the retry count or not
     *
     * @param msg The message to increase the retry count on
     */
    protected void increaseRetryCount (final SIMessageHandle msgHandle) {

        final String methodName = "increaseRetryCount";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        try {

            SIUncoordinatedTransaction localTran = _connection.createUncoordinatedTransaction();
            deleteMessages(new SIMessageHandle [] { msgHandle }, localTran);
            localTran.rollback();

        } catch (Exception exception) {

          FFDCFilter.processException(exception, CLASS_NAME + "."
              + methodName, "1:1547:1.73", this);
          if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
              SibTr.exception(this, TRACE, exception);
          }
        }

      if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
          SibTr.exit(this, TRACE, methodName);
      }

    }
    
    /**
     * Returns a string generator containing the fields for this class.
     *
     * @return a string generator
     */
    protected SibRaStringGenerator getStringGenerator() {

        final SibRaStringGenerator generator = new SibRaStringGenerator(this);
        generator.addField("cancelled", _cancelled);
        generator.addField("connection", _connection);
        generator.addField("session", _session);
        generator.addField("endpointFactory", _endpointFactory);
        generator.addField("invoker", _invoker);
        generator.addField("endpointConfiguration", _endpointConfiguration);
        generator.addField("requestMetries",null /*_requestMetrics*/);
        return generator;

    }
    
    private void addToSequentialFailureMessageCount()
    {
      final String methodName = "addToSequentialFailureMessageCount";
      if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
          SibTr.entry(this, TRACE, methodName);
      }
      
      _sequentialFailureMessageCount = _sequentialFailureMessageCount + 1;
      
      if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
        SibTr.debug(this, TRACE, "_sequentialFailureMessageCount is: "+_sequentialFailureMessageCount);
      }
      
      if (_sequentialFailureMessageCount == _sequentialFailureThreshold)
      {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
          SibTr.debug(this, TRACE, "Reached threshold of : " + _sequentialFailureThreshold + " warning user");
        }
        warnUserOfSequentialMessageFailure();
      }
      
      if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
        SibTr.exit(this, TRACE, methodName);
      }
    }
    
    private void warnUserOfSequentialMessageFailure()
    {
      final String methodName = "warnUserOfSequentialMessageFailure";
      if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
          SibTr.entry(this, TRACE, methodName);
      }

      // Get j2ee name
      // print message to log
      try
      {
    	  _mepJ2EEName = ((MDBMessageEndpointFactory)_endpointFactory).getMDBKey().toString();

    	  if (null != _mepJ2EEName)
    	  {
    		  // Output a message that the user should deactivate the endpoint
    		  SibTr.warning(TRACE, NLS.getFormattedMessage("MESSAGE_ENDPOINT_SHOULD_BE_DEACTIVATED_CWSIV0605",
    				  new Object[] {_mepJ2EEName, _session.getDestinationAddress()}, null ));
    	  }

      }catch (Exception ex)
      {
          FFDCFilter.processException(ex, CLASS_NAME + "."
                          + methodName, "1:1650:1.73", this);
                      if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                        SibTr.exception(this, TRACE, ex);
                      }
      }
      
      if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
        SibTr.exit(this, TRACE, methodName);
      }
    }
    
    private void clearSequentialFailureMessageCount()
    {
      final String methodName = "clearSequentialFailureMessageCount";
      if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
          SibTr.entry(this, TRACE, methodName);
      }
      
      _sequentialFailureMessageCount = 0;

      if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
          SibTr.exit(this, TRACE, methodName);
      }
    }

}
