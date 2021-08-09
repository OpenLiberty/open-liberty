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

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpoint;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.ra.impl.SibRaUtils;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.AbstractConsumerSession;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SIMessageHandle;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.SIXAResource;
import com.ibm.wsspi.sib.core.exception.SIMessageNotLockedException;
import com.ibm.wsspi.sib.core.trm.SibTrmConstants;

/**
 * Dispatches messages to a transactional message endpoint. Creates an endpoint
 * with an <code>XAResource</code> and then, after calling
 * <code>beforeDelivery</code> on the endpoint, deletes the message under that
 * transaction.
 */
class SibRaTransactionalDispatcher extends SibRaDispatcher
{

    /**
     * The <code>XAResource</code> enlisted in the transaction and used to
     * delete the messages.
     */
    private SIXAResource _xaResource;

    /**
     * The <code>SibRaXaResource</code> that wraps a <code>XAResource</code> that
     * is used for tracking whether a transaction was rolled back.
     */
    private SibRaXaResource _sibXaResource;

    /**
     * The name of the bus to which the dispatcher is connected.
     */
    private final String _busName;

    /**
     * The component to use for trace.
     */
    private static final TraceComponent TRACE = SibRaUtils
            .getTraceComponent(SibRaTransactionalDispatcher.class);

    /**
     * Provides access to NLS enabled messages.
     */
    private static TraceNLS NLS = SibRaUtils.getTraceNls();

    /**
     * The name of this class.
     */
    private static final String CLASS_NAME = SibRaTransactionalDispatcher.class
            .getName();

    private static final String FFDC_PROBE_1 = "1";

    private static final String FFDC_PROBE_2 = "2";

    private static final String FFDC_PROBE_3 = "3";

    private static final String FFDC_PROBE_4 = "4";

    
    /**
     * Constructor.
     *
     * @param connection
     *            the parent connection for the session
     * @param session
     *            the session from which the messages were received
     * @param endpointActivation
     *            the endpoint activation which lead to these messages being
     *            received
     * @param busName
     *            the busName
     * @throws ResourceException
     *             if the dispatcher cannot be successfully created
     */
    protected SibRaTransactionalDispatcher(final SibRaMessagingEngineConnection connection,
            final AbstractConsumerSession session,
            final SibRaEndpointActivation endpointActivation,
            final String busName,
            final Reliability unrecoveredReliability,
            final int maxFailedDeliveries,
            final int sequentialFailureThreshold) throws ResourceException {

        super(connection, session, endpointActivation, unrecoveredReliability, maxFailedDeliveries, sequentialFailureThreshold);

        final String methodName = "SibRaTransactionalDispatcher";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName, new Object[] { connection,
                    session, endpointActivation, busName });
        }

        _busName = busName;

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(this, TRACE, methodName);
        }
    }

    /**
     * Returns the transaction, if any, used to deliver the message.
     *
     * @return the transaction
     * @throws ResourceException
     *             if the transaction could not be obtained
     */
    protected SITransaction getTransaction() throws ResourceException
    {
        final String methodName = "getTransaction";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName);
        }

        final SITransaction transaction = getXaResource();

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(this, TRACE, methodName, transaction);
        }
        return transaction;
    }

    /**
     * Returns the <code>XAResource</code> used to deliver the messages under.
     *
     * @return the <code>XAResource</code>
     * @throws ResourceException
     *             if the <code>XAResource</code> could not be created
     */
    private SIXAResource getXaResource() throws ResourceException
    {
        final String methodName = "getXaResource";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName);
        }

        if (_xaResource == null)
        {
            try
            {
              _xaResource = _connection.getSIXAResource();
            }
            catch (final SIException exception)
            {
                FFDCFilter.processException(exception, CLASS_NAME + "."
                        + methodName, FFDC_PROBE_1, this);
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled())
                {
                    SibTr.exception(this, TRACE, exception);
                }
                throw new ResourceException(NLS.getFormattedMessage(
                        ("XARESOURCE_EXCEPTION_CWSIV0650"), new Object[] {
                                exception, _connection }, null), exception);
            }
            catch (final SIErrorException exception)
            {
                FFDCFilter.processException(exception, CLASS_NAME + "."
                        + methodName, FFDC_PROBE_3, this);
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }
                throw new ResourceException(NLS.getFormattedMessage(
                        ("XARESOURCE_EXCEPTION_CWSIV0650"), new Object[] {
                                exception, _connection }, null), exception);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(this, TRACE, methodName, _xaResource);
        }
        return _xaResource;
    }

    /**
     * Creates an endpoint. Passes an <code>XAResource</code> on the creation.
     * It is wrapped in a <code>SibRaXaResource</code>.
     *
     * @return the endpoint
     * @throws ResourceException
     *             if the endpoint could not be created
     */
    protected MessageEndpoint createEndpoint() throws ResourceException
    {
        final String methodName = "createEndpoint";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName);
        }


            // Ensure that in non-WAS environments the activation spec used to create
            // the current connection specified a specific messaging engine. This is
            // required so that we always connect to the same messaging engine when
            // using the same activation specification to permit transaction recovery.

        	//chetan liberty change :
        	//getTarget() check is removed since in liberty we have 1 ME per liberty profile
            if ((_endpointConfiguration.getTargetSignificance().equals(SibTrmConstants.TARGET_SIGNIFICANCE_REQUIRED)) &&
                (_endpointConfiguration.getTargetType().equals(SibTrmConstants.TARGET_TYPE_ME))) {
                _sibXaResource = new SibRaXaResource (getXaResource());
            } else {
              String p0;          // Connection property name
              String p1;          // Required connection property value

              if (!_endpointConfiguration.getTargetSignificance().equals(SibTrmConstants.TARGET_SIGNIFICANCE_REQUIRED)) {
                p0 = SibTrmConstants.TARGET_SIGNIFICANCE;
                p1 = SibTrmConstants.TARGET_SIGNIFICANCE_REQUIRED;
              } else if (!_endpointConfiguration.getTargetType().equals(SibTrmConstants.TARGET_TYPE_ME)) {
                p0 = SibTrmConstants.TARGET_TYPE;
                p1 = SibTrmConstants.TARGET_TYPE_ME;
              } else {
                p0 = SibTrmConstants.TARGET_GROUP;
                p1 = "!null";
              }

              SibTr.error(TRACE, "ME_NAME_REQUIRED_CWSIV0652",new Object[] {p0,p1});
              throw new NotSupportedException(NLS.getFormattedMessage("ME_NAME_REQUIRED_CWSIV0652", new Object[] {p0,p1}, null));
            }
        

        final MessageEndpoint endpoint = _endpointFactory
                .createEndpoint(_sibXaResource);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(this, TRACE, methodName, endpoint);
        }
        return endpoint;
    }

    /**
     * Invoked before delivery of a message. Calls <code>beforeDelivery</code>
     * on the endpoint.
     *
     * @param message
     *            the message that is about to be delivered
     * @throws ResourceAdapterInternalException
     *             if the endpoint method does not exist or the
     *             <code>XAResource</code> is not enlisted
     * @throws ResourceException
     *             if before delivery failed
     */
    protected void beforeDelivery(MessageEndpoint endpoint) throws ResourceException
    {
                final String methodName = "beforeDelivery";
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
                    SibTr.entry(this, TRACE, methodName);
                }

                /*
                 * Invoke beforeDelivery on the endpoint
                 */

                try
        {
                    endpoint.beforeDelivery(_invoker.getEndpointMethod());
                }
        catch (final NoSuchMethodException exception)
        {
                    FFDCFilter.processException(exception, CLASS_NAME + "."
                            + methodName, FFDC_PROBE_2, this);
                    if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled())
            {
                        SibTr.exception(this, TRACE, exception);
                    }
                    throw new ResourceAdapterInternalException(NLS.getFormattedMessage(
                            ("BEFORE_DELIVERY_CWSIV0651"), new Object[] { exception,
                                    endpoint }, null), exception);
                }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(this, TRACE, methodName);
        }
        }

    /**
     * Invoked before delivery of a message. Calls <code>beforeDelivery</code>
     * on the endpoint and then deletes the message under that transaction.
     *
     * @param message
     *            the message that is about to be delivered
     * @throws ResourceAdapterInternalException
     *             if the endpoint method does not exist or the
     *             <code>XAResource</code> is not enlisted
     * @throws ResourceException
     *             if before delivery failed
     */
    protected void beforeDelivery(final SIBusMessage message, MessageEndpoint endpoint)
            throws ResourceException
    {
        final String methodName = "beforeDelivery";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName, message);
        }

        // This will call before delivery on the endpoint
        beforeDelivery (endpoint);

        try
        {
            // If we are enlisted in the xa transaction then delete the message
            // under that transaction.
            if ((_xaResource != null) && (_xaResource.isEnlisted ()))
            {
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled ())
                {
                    SibTr.debug(this, TRACE, "Deleting the message under the xa transaction (Message=" + message + ") (xaResource=" + _xaResource);
                }

                deleteMessage(message, _xaResource);
            }
            else
            {
                // If we have not been enlisted then we can assume that we have
                // inherited the transaction from the local thread which was imported
                // from the message itself. As such we can not delete the message
                // under this transaction because if the message rolls back the message
                // will be redelivered and we can not reuse the same transaction since
                // it is no longer a valid transaction. Once the transaction has been
                // imported then the message has done its "work" and we can auto commit
                // the deletion.
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled ())
                {
                    SibTr.debug(this, TRACE, "xaResource is either null or is not enlisted so deleting the message non transactional (auto commit) (Message=" + message + ")");
                }

                deleteMessage(message, null);
            }
        }
        catch (final ResourceException exception)
        {
            FFDCFilter.processException(exception, CLASS_NAME + "."
                    + methodName, FFDC_PROBE_4, this);

            endpoint.afterDelivery();

            throw exception;
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(this, TRACE, methodName);
        }
    }

    /**
     * The method reads a message from a handle. This is z/os specific method
     *
     * @param The handle associated with the message to read
     * @return The message
     */
    protected SIBusMessage readMessage (SIMessageHandle handle)
                throws ResourceException, SIMessageNotLockedException
    {
        final String methodName = "readMessage";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName, handle);
        }

        SIBusMessage message = null;

        // If we are enlisted in the xa transaction then delete the message
        // under that transaction.
        if ((_xaResource != null) && (_xaResource.isEnlisted ()))
        {
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled ())
            {
                SibTr.debug(this, TRACE, "Deleting the message under the xa transaction (Handle=" + handle + ") (xaResource=" + _xaResource + ")");
            }

            message = readAndDeleteMessage(handle, _xaResource);
        }
        else
        {
            // If we have not been enlisted then we can assume that we have
            // inherited the transaction from the local thread which was imported
            // from the message itself. As such we can not delete the message
            // under this transaction because if the message rolls back the message
            // will be redelivered and we can not reuse the same transaction since
            // it is no longer a valid transaction. Once the transaction has been
            // imported then the message has done its "work" and we can auto commit
            // the deletion.
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled ())
            {
                SibTr.debug(this, TRACE, "xaResource is either null or is not enlisted so deleting the message non transactional (auto commit) (Handle=" + handle + ")");
            }

            message = readAndDeleteMessage(handle, null);
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(this, TRACE, methodName, message);
        }

        return message;
    }

    /**
     * Invoked after delivery of a message. Calls <code>afterDelivery</code>
     * on the endpoint.
     *
     * @param message
     *            the message that was delivered
     * @param success
     *            flag indicating whether delivery was successful
     * @throws ResourceException
     *             if after delivery failed
     */
    protected void afterDelivery(final SIBusMessage message, MessageEndpoint endpoint,
            final boolean success) throws ResourceException
    {
        final String methodName = "afterDelivery";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.entry(this, TRACE, methodName, new Object[] { message,
                    Boolean.valueOf(success) });
        }

        endpoint.afterDelivery();

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            SibTr.exit(this, TRACE, methodName);
        }
    }

    /**
     * Invoked after all messages in the batch have been delivered. Does
     * nothing.
     */
    protected void cleanup()
    {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled())
        {
            final String methodName = "cleanup";
            SibTr.entry(this, TRACE, methodName);
            SibTr.exit(this, TRACE, methodName);
        }
    }

    /**
     * Returns a string generator containing the fields for this class.
     *
     * @return a string generator
     */
    protected SibRaStringGenerator getStringGenerator()
    {
        final SibRaStringGenerator generator = super.getStringGenerator();
        generator.addField("sibXaResource", _sibXaResource);
        generator.addField("xaResource", _xaResource);
        return generator;
    }

    /**
     * Checks if the transaction was rolled back or not.
     *
     * @return true if the transaction was rolled back
     */
    protected boolean isTransactionRolledBack ()
    {
        return _sibXaResource.isTransactionRolledBack ();
    }

}
