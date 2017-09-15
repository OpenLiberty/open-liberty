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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.resource.ResourceException;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ejbcontainer.mdb.MDBMessageEndpointFactory;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.ra.impl.SibRaUtils;
import com.ibm.ws.sib.ra.inbound.SibRaEndpointConfiguration;
import com.ibm.ws.sib.ra.inbound.SibRaEndpointInvoker;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.BifurcatedConsumerSession;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SIMessageHandle;
import com.ibm.wsspi.sib.core.exception.SIMessageNotLockedException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;

/**
 * Represents the activation of an endpoint in the servant region.
 */
final class SibRaDispatchEndpointActivation extends SibRaEndpointActivation {

    /**
     * The J2EE name of the message-driven bean associated with this endpoint
     * activation.
     */
    private String _j2eeName;

    /**
     * Map from ME UUID to a map of session IDs to sessions.
     */
    private final Map _sessionsByMeUuid = new HashMap();

    /**
     * A map from message-driven bean J2EE names to endpoint activations.
     */
    private static final Map _endpointActivations = new HashMap();

    /**
     * The component to use for trace.
     */
    private static final TraceComponent TRACE = SibRaUtils
            .getTraceComponent(SibRaDispatchEndpointActivation.class);

    /**
     * Provides access to NLS enabled messages.
     */
    private static TraceNLS NLS = SibRaUtils.getTraceNls();

    /**
     * The name of this class.
     */
    private static final String CLASS_NAME = SibRaDispatchEndpointActivation.class
            .getName();


    /**
     * Constructor. Obtains the J2EE name of the message-driven bean and adds
     * this activation to the map keyed off that name.
     *
     * @param resourceAdapter
     *            the resource adapter on which the activation was created
     * @param messageEndpointFactory
     *            the message endpoint factory for this activation
     * @param endpointConfiguration
     *            the endpoint configuration for this activation
     * @param endpointInvoker
     *            the endpoint invoker for this activation
     * @throws ResourceException
     *             if the activation fails
     */
    public SibRaDispatchEndpointActivation(
            final SibRaResourceAdapterImpl resourceAdapter,
            final MessageEndpointFactory messageEndpointFactory,
            final SibRaEndpointConfiguration endpointConfiguration,
            final SibRaEndpointInvoker endpointInvoker,
            final int maxArraySize)
            throws ResourceException {

        super(resourceAdapter, messageEndpointFactory, endpointConfiguration,
                endpointInvoker);

        final String methodName = "SibRaDispatchEndpointActivation";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] {
                    resourceAdapter, messageEndpointFactory,
                    endpointConfiguration, endpointInvoker });
        }

        if (messageEndpointFactory instanceof MDBMessageEndpointFactory) {

        	_j2eeName = ((MDBMessageEndpointFactory)messageEndpointFactory).getMDBKey().toString();

        } else {

            final ResourceException exception = new ResourceAdapterInternalException(
                    NLS.getFormattedMessage("WAS_ENDPOINT_FACTORY_CWSIV1000",
                            new Object[] {
                                    messageEndpointFactory, 
                                    javax.resource.spi.endpoint.MessageEndpointFactory.class
                                            .getName() }, null));
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
            throw exception;

        }

        synchronized (_endpointActivations) {

          // Get the pool of endpoint activations
          SibRaEndpointArray endpointActivationArray = (SibRaEndpointArray) _endpointActivations.get(_j2eeName);

          // If we are the first endpoint activation to be created for this MDB then create the
          // pool.
          if (endpointActivationArray == null) {

            endpointActivationArray = new SibRaEndpointArray (maxArraySize);
            _endpointActivations.put (_j2eeName, endpointActivationArray);

          }

          // Add ourselves to the pool.
          endpointActivationArray.addEndpoint(this);

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Dispatches the message represented by the given token.
     *
     * @param token
     *            the token representing the message
     * @param context
     *            the context associated with the message
     */
    void dispatch(final SibRaMessageToken token, final Map context) {

        final String methodName = "dispatch";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName,
                    new Object[] { token, context });
        }

        SibRaMessagingEngineConnection connection = null;
        BifurcatedConsumerSession session = null;
        SibRaDispatcher dispatcher = null;
        try {

            /*
             * Obtain connection to messaging engine
             */

            connection = getConnection(token.getBusName(), token.getMeUuid());

            /*
             * Obtain bifurcated consumer session
             */

            synchronized (_sessionsByMeUuid) {

                Map sessionsById = (Map) _sessionsByMeUuid.get(token
                        .getMeUuid());

                if (sessionsById == null) {

                    sessionsById = new HashMap();
                    _sessionsByMeUuid.put(token.getMeUuid(), sessionsById);

                }

                session = (BifurcatedConsumerSession) sessionsById
                        .get(Long.valueOf(token.getSessionId()));

                if (session == null) {

                    /*
                     * Create a new bifurcated consumer session
                     */

                    session = connection.getConnection()
                            .createBifurcatedConsumerSession(
                                    token.getSessionId());
                    sessionsById.put(Long.valueOf(token.getSessionId()), session);

                }

            }

            /**
             * Create the first dispatcher
             */
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) SibTr.debug(TRACE, "creating dispatcher for: " + _j2eeName);
            dispatcher = connection.createDispatcher(session, Reliability.getReliability(token.getUnrecoveredReliability()), token.getMaxFailedDeliveries(), token.getSequentialMessageThreshold());

            /*
             * Get the array of message ids
             */
            SIMessageHandle [] msgHandles = token.getMessageHandleArray();
            SIBusMessage [] messages = null;

            /**
             * If we are a non transactional MDB then
             * - If we can delete the messages when they are read then perform a readAndDeleteSet
             * - If the messages should be kept (the message has a higher reliability than unrecovered reliability
             * defined on the session) then just perform a read set.
             * For transactional MDBs do not read the messages yet. This will be done in the dispatcher's
             * dispatch code after a transaction has been started so that the readAndDelete can be performed
             * under this transaction.
             */
            if ((dispatcher instanceof SibRaNonTransactionalDispatcher) && (messages == null)) {
              if (token.isDeleteMessagesWhenRead()) {

                  if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled ()) {
                    SibTr.debug(this, TRACE,
                        "These messages will be deleted when read " + Arrays.toString(token.getMessageHandleArray ()));
                  }

                  messages = session.readAndDeleteSet(token.getMessageHandleArray (), null); // For messages that are being deleted

              } else {

                  if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled ()) {
                    SibTr.debug(this, TRACE,
                        "These messages will be read but not deleted yet " + Arrays.toString(token.getMessageHandleArray ()));
                  }

                  messages = session.readSet (token.getMessageHandleArray ()); // For messages that are not being deleted

              }

            }

            if ((msgHandles != null) && (msgHandles.length > 0)) {

                for (int i = 0; i < msgHandles.length; i++) {

                  /**
                   * Create a new dispatcher if we need one.
                   */
                  if (dispatcher == null) {
                        dispatcher = connection.createDispatcher(session, Reliability.getReliability(token.getUnrecoveredReliability()), token.getMaxFailedDeliveries(), token.getSequentialMessageThreshold());
                  }

                    dispatcher.dispatch(msgHandles[i], messages == null ? null : messages [i], context);

                    // Close the dispatcher and create a new one.
                    connection.closeDispatcher(dispatcher);
                    dispatcher = null;

                }
            } else {

                SibTr.error(TRACE, "READ_SET_CWSIV1051", new Object[] {
                        Long.valueOf(token.getSessionId()), token.getMeUuid(),
                        Arrays.deepToString (msgHandles), token.getJ2EEName() });

            }

        } catch (final SISessionUnavailableException exception) {

            // No FFDC code needed

            /*
             * Session has been closed by listener and message will have been
             * unlocked
             */

            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }

        }
        catch (final SIMessageNotLockedException exception) {

            // No FFDC code needed

            /*
             * Message lock has expired and message will be reassigned
             */

            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }

        }
    catch (SIRollbackException exception) {

            FFDCFilter.processException(exception, CLASS_NAME + "."
                    + methodName, "1:348:1.19", this);
            SibTr.warning(TRACE, "EXCEPTION_CWSIV1052", new Object[] {
                    Long.valueOf(token.getSessionId()), token.getMeUuid(),
                    Arrays.deepToString(token.getMessageHandleArray()), exception });

        } catch (final Exception exception) {

            FFDCFilter.processException(exception, CLASS_NAME + "."
                    + methodName, "1:356:1.19", this);
            SibTr.warning(TRACE, "EXCEPTION_CWSIV1052", new Object[] {
                    Long.valueOf(token.getSessionId()), token.getMeUuid(),
                    Arrays.deepToString(token.getMessageHandleArray()), exception });

            /*
             * Close the dispatcher before closing the connection (if the connection is 
             * closed while a dispatcher is open the closeConnection will just hang waiting
             * for the last dispatcher to be closed).
             */
            if (dispatcher != null) {
                connection.closeDispatcher(dispatcher);
            }

            // 255651: Close the connection to the ME if an unknown error occurs
            // to flush the connection and force a new one to be created
            closeConnection(token.getMeUuid(), false);

            // null out the disptacher so we don't try and close it again.
            dispatcher = null;

        } finally {

            /*
             * Close the dispatcher if its not already been closed
             */

            if (dispatcher != null) {
                connection.closeDispatcher(dispatcher);
            }

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Called on deactivation of this endpoint.
     */
    void deactivate() {

        final String methodName = "deactivate";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        synchronized (_endpointActivations) {

            // Only one EndpointActivation is stored in the ResourceAdapterImpl during endpoint
            // activation so only one deactivate will be called when the resource adapater has its
            // endpointDeactivate method called. We then get hold of the array of endpointActivations
            // we are using and deactivate them all one by one (by calling the SibRaEndpointArray's
            // deactivateAll method.
            SibRaEndpointArray epArray = (SibRaEndpointArray) _endpointActivations.remove(_j2eeName);
            if (epArray != null) {

                // Cycle through and call internalDeactivate on all endpoint activations
                epArray.deactivateAll ();
            }

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * This method performs tidy up on the endpoint activation and calls the base class's
     * deactivate method
     */
    void internalDeactivate ()
    {
      final String methodName = "internalDeactivate";
      if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
          SibTr.entry(this, TRACE, methodName);
      }

      synchronized (_sessionsByMeUuid) {
        _sessionsByMeUuid.clear();
      }

      super.deactivate();

      if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
        SibTr.exit(this, TRACE, methodName);
      }

    }

    /**
     * Closes the connection for the given messaging engine if there is one
     * open. Removes any corresponding sessions from the maps.
     *
     * @param meUuid
     *            the UUID for the messaging engine to close the connection for
     */
    protected void closeConnection(final String meUuid, boolean alreadyClosed) {

        final String methodName = "closeConnection";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, meUuid);
        }

        synchronized (_sessionsByMeUuid) {

            super.closeConnection(meUuid, alreadyClosed);

            _sessionsByMeUuid.remove(meUuid);

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.sib.ra.inbound.impl.SibRaEndpointActivation#addMessagingEngine(com.ibm.ws.sib.admin.JsMessagingEngine)
     */
    void addMessagingEngine(final JsMessagingEngine messagingEngine)
            throws ResourceException {

        // Do nothing

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.sib.ra.inbound.impl.SibRaEndpointActivation#sessionError(com.ibm.ws.sib.ra.inbound.impl.SibRaMessagingEngineConnection,
     *      com.ibm.wsspi.sib.core.ConsumerSession, java.lang.Throwable)
     */
    void sessionError(final SibRaMessagingEngineConnection connection,
            final ConsumerSession session, final Throwable throwable) {

        /*
         * Do nothing - sessions are only used synchronously so the caller will
         * be notified
         */

        final String methodName = "sessionError";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { connection,
                    session, throwable });
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.sib.ra.inbound.impl.SibRaEndpointActivation#connectionError(com.ibm.ws.sib.ra.inbound.impl.SibRaMessagingEngineConnection,
     *      com.ibm.websphere.sib.exception.SIException)
     */
    void connectionError(final SibRaMessagingEngineConnection connection,
            final SIException exception) {

        final String methodName = "connectionError";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { connection,
                    exception });
        }

        /*
         * Close the connection - any future work will attempt to create a new
         * connection
         */

        closeConnection(connection.getConnection().getMeUuid(), true);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.sib.ra.inbound.impl.SibRaEndpointActivation#messagingEngineTerminated(com.ibm.ws.sib.ra.inbound.impl.SibRaMessagingEngineConnection)
     */
    void messagingEngineTerminated(
            final SibRaMessagingEngineConnection connection) {

        final String methodName = "messagingEngineTerminated";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, connection);
        }

        /*
         * Close the connection - any future work will attempt to create a new
         * connection
         */

        closeConnection(connection.getConnection().getMeUuid(), true);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.sib.ra.inbound.impl.SibRaEndpointActivation#messagingEngineQuiescing(com.ibm.ws.sib.ra.inbound.impl.SibRaMessagingEngineConnection)
     */
    void messagingEngineQuiescing(
            final SibRaMessagingEngineConnection connection) {

        final String methodName = "messagingEngineQuiescing";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, connection);
        }

        /*
         * Close the connection - any future work will attempt to create a new
         * connection
         */

        closeConnection(connection.getConnection().getMeUuid(), true);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.sib.ra.SibRaMessagingEngineListener#messagingEngineInitializing(com.ibm.ws.sib.admin.JsMessagingEngine)
     */
    public void messagingEngineInitializing(
            final JsMessagingEngine messagingEngine) {

        // Do nothing

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.sib.ra.SibRaMessagingEngineListener#messagingEngineDestroyed(com.ibm.ws.sib.admin.JsMessagingEngine)
     */
    public void messagingEngineDestroyed(final JsMessagingEngine messagingEngine) {

        // Do nothing

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.sib.ra.SibRaMessagingEngineListener#messagingEngineReloaded(com.ibm.ws.sib.admin.JsMessagingEngine)
     */
    public void messagingEngineReloaded(final JsMessagingEngine engine) {

        // Do nothing

    }

    /**
     * Returns a string generator containing the fields for this class.
     *
     * @return a string generator
     */
    protected SibRaStringGenerator getStringGenerator() {

        final SibRaStringGenerator generator = super.getStringGenerator();
        generator.addField("j2eeName", _j2eeName);
        return generator;

    }

    /**
     * Returns the endpoint activation for the message-driven bean with the
     * given J2EE name. There is an array of endpoint activations and this method
     * will return one by going round robin through them all. The round robin
     * behaviour is obtained by using an internal cursor to iterate through
     * the endpoints in the array.
     *
     * @param j2eeName
     *            the J2EE name of the message-driven bean
     * @return the endpoint activation
     */
    static SibRaDispatchEndpointActivation getEndpointActivation(
            final String j2eeName) {

          final String methodName = "getEndpointActivation";
          if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
              SibTr.entry(SibRaDispatchEndpointActivation.class,
                          TRACE, methodName, new Object [] { j2eeName } );
          }

        SibRaDispatchEndpointActivation endpoint = null;

        synchronized (_endpointActivations) {

          SibRaEndpointArray endpointActivationArray = (SibRaEndpointArray) _endpointActivations.get(j2eeName);

          if (endpointActivationArray != null) {

            // Get the next endpoint
            endpoint = endpointActivationArray.getNextEndpoint();

          }

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
          SibTr.exit(SibRaDispatchEndpointActivation.class, TRACE, methodName, endpoint);
        }

        return endpoint;
    }

    /**
     * This is a baggage class used to store an array of endpoints.
     */
    private static class SibRaEndpointArray {

      /**
       * The position in the array where the next endpoint will be inserted
       */
      private int _insertPoint = -1;

      /**
       * The current cursor position into the array where the endpoint will
       * be read from
       */
      private int _readPoint = -1;

      /**
       * The array of endpoints
       */
      private final SibRaDispatchEndpointActivation [] _endpoints;

      /**
       * Creates the array of endpoints
       * @param maxSize The maximum number of endpoints in the array
       */
      private SibRaEndpointArray (int maxSize)
      {
        _endpoints = new SibRaDispatchEndpointActivation [maxSize];
      }

      /**
       * Add the supplied endpoint to the array
       * @param ep The endpoint to add to the array
       */
      private void addEndpoint (SibRaDispatchEndpointActivation ep)
      {
        _endpoints [++_insertPoint] = ep;
      }

      /**
       * Using round robin approach get the next endpoint in the array.
       * @return The next endpoint to use (using round robin to cycle through them)
       */
      private SibRaDispatchEndpointActivation getNextEndpoint ()
      {
        _readPoint++;

        // If we hit a value higher than the insert point then go back to the start
        // We do not use >= here since _insertPoint is the position that the last
        // endpoint was inserted.
        if (_readPoint > _insertPoint)
        {
          _readPoint = 0;
        }

        return _endpoints [_readPoint];
      }

      /**
       * Go through all the endpoint activations and deactivate them all
       */
      private void deactivateAll ()
      {

        // Go through all the endpoints and call internalDeactivate
        for (int i = 0; i <= _insertPoint; i++)
        {
          _endpoints [i].internalDeactivate();
        }

      }

    }

}
