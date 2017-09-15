/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.processor.impl;

/* Import required classes */
import java.util.Properties;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIApiConstants;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.websphere.sib.management.SibNotificationConstants;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.admin.RuntimeEventListener;
import com.ibm.ws.sib.mfp.JsApiMessage;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.MessageCopyFailedException;
import com.ibm.ws.sib.mfp.ProtocolType;
import com.ibm.ws.sib.msgstore.MessageStoreRuntimeException;
import com.ibm.ws.sib.processor.ExceptionDestinationHandler;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.UndeliverableReturnCode;
import com.ibm.ws.sib.processor.exceptions.SIMPLimitExceededException;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.InputHandler;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.impl.store.items.MessageItem;
import com.ibm.ws.sib.processor.utils.UserTrace;
import com.ibm.ws.sib.security.auth.OperationType;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;

/**
 * Exception destination class
 * <p>When a message cannot be delivered to a destination due to problems that
 * may occur, the message may be rerouted to an exception destination. This
 * exception destination is encapsulated in an ExceptionDestinatioin class along
 * with methods to peform the redirect operations. Every destination has an
 * instance of an ExceptionDestination class.
 */

public class ExceptionDestinationHandlerImpl implements ExceptionDestinationHandler
{

    // NLS for component
    private static final TraceNLS nls_mt =
                    TraceNLS.getTraceNLS(SIMPConstants.TRACE_MESSAGE_RESOURCE_BUNDLE);

    /**
     * Trace for the component
     */

    // NLS for component
    private static final TraceNLS nls =
                    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

    private static final TraceComponent tc =
                    SibTr.register(
                                   ExceptionDestinationHandlerImpl.class,
                                   SIMPConstants.MP_TRACE_GROUP,
                                   SIMPConstants.RESOURCE_BUNDLE);

    /**
     * The destination that we failed to deliver to
     */

    private DestinationHandler _originalDestination;

    /**
     * The exception destination we are currently sending undeliverable messages to
     */

    private String _exceptionDestinationName;

    /**
     * The following lock is used to synchronize threads accessing the critical section
     * where exception destination is set for an undeliverable message.
     */
    private final Object _exceptionDestinationLock = new Object();

    /**
     * An Inputhandler to the exception destination. N.B. We do not create a producersession
     * to the destination.
     */

    private InputHandler _inputHandler = null;

    /**
     * The destination handler representing the exception destination.
     */

    private DestinationHandler _exceptionDestination;

    /**
     * The MessageProcessor reference that enables us to lookup destinations
     */

    private final MessageProcessor _messageProcessor;

    /**
     * This holds the name of the current default exception destination. There is
     * one default exception destination on the ME. A default exception destination
     * will always exist - this is enforced at the admin level.
     */

    private String _defaultExceptionDestinationName = null;

    /**
     * The ReportHandler that manages the generation and send of a report
     * message. We use this here to generate any exception reports.
     */

    private ReportHandler _reportHandler = null;

    /*
     * Used to construct the name of the default exception destination for a messaging
     * engine
     */
    public static String constructDefaultExceptionDestinationName(String messagingEngineName)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "constructDefaultExceptionDestinationName", messagingEngineName);

        //Venu Liberty change
        //Removing ME name from Exception destination name as for Liberty profile exception destination name is
        //just _SYSTEM.Exception.Destination
        final String rc = SIMPConstants.SYSTEM_DEFAULT_EXCEPTION_DESTINATION_PREFIX;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "constructDefaultExceptionDestinationName", rc);

        return rc;
    }

    /*
     * Returns a String representing the name of the default exception destination
     * for this messaging engine
     */
    public String getDefaultExceptionDestinationName()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getDefaultExceptionDestinationName");

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getDefaultExceptionDestinationName", _defaultExceptionDestinationName);

        return _defaultExceptionDestinationName;
    }

    /**
     * Constructor. Takes the destination as a parameter.
     * 
     * @param DestinationHandler - The destination to set
     */

    public ExceptionDestinationHandlerImpl(DestinationHandler originalDestination)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "ExceptionDestinationHandlerImpl", originalDestination);

        // Set the destination associated with this instance.
        _originalDestination = originalDestination;

        // Get the message processor reference
        _messageProcessor = originalDestination.getMessageProcessor();

        // Get the default exception destination name
        final String meName = _messageProcessor.getMessagingEngineName();
        _defaultExceptionDestinationName = constructDefaultExceptionDestinationName(meName);

        // Set the initial exception destination to the default
        _exceptionDestinationName = _defaultExceptionDestinationName;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "ExceptionDestinationHandlerImpl", this);
    }

    /**
     * Constructor. Takes the destination name as a parameter.
     * Looks up the destination and creates a connection to the ME.
     * 
     * @param String - The destination to set
     */

    public ExceptionDestinationHandlerImpl(SIDestinationAddress destinationAddr, MessageProcessor mp)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "ExceptionDestinationHandlerImpl", new Object[] { destinationAddr, mp });

        _messageProcessor = mp;

        // Set the destination associated with this ExceptionDestination instance
        setDestination(destinationAddr);

        // Get the default exception destination name
        final String meName = _messageProcessor.getMessagingEngineName();
        _defaultExceptionDestinationName = constructDefaultExceptionDestinationName(meName);

        // Set the initial exception destination to the default
        _exceptionDestinationName = _defaultExceptionDestinationName;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "ExceptionDestinationHandlerImpl", this);
    }

    /**
     * Sets the destination that could not be delivered to.
     * 
     * @param destination The destination to set
     */

    public void setDestination(DestinationHandler originalDestination)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setDestination", originalDestination);

        _originalDestination = originalDestination;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setDestination");
    }

    /**
     * Sets the destination that could not be delivered to. Looks up the destination
     * given its name.
     * 
     * @param destinationName - The destination to set
     */

    @Override
    public void setDestination(SIDestinationAddress destinationAddr)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setDestination", destinationAddr);

        if (destinationAddr != null)
        {
            try
            {
                _originalDestination = _messageProcessor.getDestinationManager().getDestination((JsDestinationAddress) destinationAddr, true);
            } catch (SIException e)
            {
                // No FFDC code needed

                /* Generate warning but retry with default exception destination */
                SibTr.warning(tc, "EXCEPTION_DESTINATION_WARNING_CWSIP0291",
                              new Object[] { destinationAddr.getDestinationName(), _messageProcessor.getMessagingEngineName(), e });

                // Create a handler to the default if provided destination is inaccessible
                _originalDestination = null;
            }
        }
        else
            _originalDestination = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setDestination", _originalDestination);
    }

    /**
     * Check whether it will be possible to place a message on the exception destination.
     * 
     * @return rc reason code
     */
    public int checkCanExceptionMessage()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkCanExceptionMessage");

        // Return code
        int rc = DestinationHandler.OUTPUT_HANDLER_NOT_FOUND;

        String newExceptionDestination = null;

        boolean usingDefault = false;

        if (_originalDestination != null)
        {
            newExceptionDestination = _originalDestination.getExceptionDestination();
            if (newExceptionDestination == null || newExceptionDestination.equals(""))
            {
                // Simply return with OUTPUT_HANDLER_NOT_FOUND
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "checkCanExceptionMessage", rc);
                return rc;
            }
            if (newExceptionDestination.equals(JsConstants.DEFAULT_EXCEPTION_DESTINATION))
            {
                newExceptionDestination = _defaultExceptionDestinationName;
                usingDefault = true;
            }
        }
        else
        {
            newExceptionDestination = _defaultExceptionDestinationName;
            usingDefault = true;
        }

        try
        {
            DestinationHandler exceptionDestHandler =
                            _messageProcessor.getDestinationManager().
                                            getDestination(newExceptionDestination, true);

            rc = exceptionDestHandler.checkCanAcceptMessage(null, null);
        } catch (SIException e)
        {
            // No FFDC code needed

            SibTr.exception(tc, e);
        }

        // If we havent checked the system default destination, then do that now
        if (rc != DestinationHandler.OUTPUT_HANDLER_FOUND && !usingDefault)
        {
            try
            {
                DestinationHandler exceptionDestHandler =
                                _messageProcessor.getDestinationManager().
                                                getDestination(_defaultExceptionDestinationName, true);

                rc = exceptionDestHandler.checkCanAcceptMessage(null, null);
            } catch (SIException e)
            {
                // No FFDC code needed

                SibTr.exception(tc, e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkCanExceptionMessage", Integer.valueOf(rc));

        return rc;
    }

    /**
     * This method contains the routine used to handle an undeliverable message. The
     * method examines the attributes of a message to determine what to do with it.
     * 
     * It is possible for a message to be discarded, blocked, or sent to an exception
     * destination.
     * 
     * @param msg - The undeliverable message
     * @param tran - The transaction that the message was delivered under
     * @param exceptionReason - The reason why the message could not be delivered
     * @param exceptionStrings - A list of inserts to place into an error message
     * @return A code indicating what we did with the message
     */
    public UndeliverableReturnCode handleUndeliverableMessage(
                                                              SIMPMessage message,
                                                              TransactionCommon tran,
                                                              int exceptionReason,
                                                              String[] exceptionStrings)
    {
        // F001333-14610
        // Delegate down onto the new method passing a null
        // subscription ID.
        return handleUndeliverableMessage(message, tran, exceptionReason, exceptionStrings, null);
    }

    // F001333-14610
    // New method that accepts a subscription ID for the undeliverable
    // message for inclusion in the exception data.
    public UndeliverableReturnCode handleUndeliverableMessage(
                                                              SIMPMessage message,
                                                              TransactionCommon tran,
                                                              int exceptionReason,
                                                              String[] exceptionStrings,
                                                              String subscriptionID)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "handleUndeliverableMessage",
                        new Object[] { message,
                                      tran,
                                      Integer.valueOf(exceptionReason),
                                      exceptionStrings,
                                      subscriptionID });

        UndeliverableReturnCode rc =
                        handleUndeliverableMessage(message,
                                                   null, // alternateUser
                                                   tran,
                                                   exceptionReason,
                                                   exceptionStrings,
                                                   _messageProcessor.getMessagingEngineUuid(),
                                                   subscriptionID);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "handleUndeliverableMessage", rc);

        return rc;
    }

    /**
     * Wrapper method for handleUndeliverableMessage. This version will be called
     * from an external component via the
     * com.ibm.ws.sib.processor.ExceptionDestinationHandler interface. E.g. we
     * need to access this routine from the MQLink in the comms component.
     * 
     * @param msg - The undeliverable message
     * @param tran - The transaction that the message was delivered under
     * @param exceptionReason - The reason why the message could not be delivered
     * @param exceptionStrings - A list of inserts to place into an error message
     * @return A code indicating what we did with the message
     */
    @Override
    public UndeliverableReturnCode handleUndeliverableMessage(
                                                              SIBusMessage msg,
                                                              String alternateUser,
                                                              TransactionCommon tran,
                                                              int exceptionReason,
                                                              String[] exceptionStrings)
    {
        // F001333-14610
        // Delegate down onto the new method passing a null
        // subscription ID.
        return handleUndeliverableMessage(msg, alternateUser, tran, exceptionReason, exceptionStrings, null);
    }

    // F001333-14610
    // New method that accepts a subscription ID for the undeliverable
    // message for inclusion in the exception data.
    public UndeliverableReturnCode handleUndeliverableMessage(
                                                              SIBusMessage msg,
                                                              String alternateUser,
                                                              TransactionCommon tran,
                                                              int exceptionReason,
                                                              String[] exceptionStrings,
                                                              String subscriptionID)
    {
        //No need to call getSent() since this will occur as part of the send
        //insde handleUndeliverableMessage
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "handleUndeliverableMessage",
                        new Object[] { msg, alternateUser, tran, Integer.valueOf(exceptionReason), exceptionStrings, subscriptionID });

        UndeliverableReturnCode rc =
                        handleUndeliverableMessage(new MessageItem((JsMessage) msg),
                                                   alternateUser,
                                                   tran,
                                                   exceptionReason,
                                                   exceptionStrings,
                                                   _messageProcessor.getMessagingEngineUuid(),
                                                   subscriptionID);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "handleUndeliverableMessage", rc);
        return rc;
    }

    /**
     * This method contains the routine used to handle an undeliverable message. The
     * method examines the attributes of a message to determine what to do with it.
     * 
     * It is possible for a message to be discarded, blocked, or sent to an exception
     * destination.
     * 
     * @param msg - The undeliverable message
     * @param tran - The transaction that the message was delivered under
     * @param exceptionReason - The reason why the message could not be delivered
     * @param exceptionStrings - A list of inserts to place into an error message
     * @param sourceCellule - The cellule this message has come from
     * @return A code indicating what we did with the message
     */
    private UndeliverableReturnCode handleUndeliverableMessage(
                                                               SIMPMessage message,
                                                               String alternateUser,
                                                               TransactionCommon tran,
                                                               int exceptionReason,
                                                               String[] exceptionStrings,
                                                               SIBUuid8 sourceMEUuid,
                                                               String subscriptionID)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "handleUndeliverableMessage",
                        new Object[] { message,
                                      alternateUser,
                                      tran,
                                      Integer.valueOf(exceptionReason),
                                      exceptionStrings,
                                      sourceMEUuid,
                                      subscriptionID });

        boolean sendComplete = false;
        boolean defaultInUse = false; // Assume non-default exception destination is being used.
        UndeliverableReturnCode rc = UndeliverableReturnCode.OK;
        LocalTransaction localTran = null;

        rc = checkMessage(message);
        if (rc == UndeliverableReturnCode.BLOCK)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "handleUndeliverableMessage", rc);
            return rc;
        }

        // Unless we're told to discard this message, try to send it to an exception destination
        if (rc != UndeliverableReturnCode.DISCARD)
        {
            JsMessage msg = null;

            try {
                msg = message.getMessage().getReceived();
            } catch (MessageCopyFailedException e)
            {
                // FFDC
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl.handleUndeliverableMessage",
                                            "1:568:1.137",
                                            this);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "handleUndeliverableMessage", UndeliverableReturnCode.ERROR);
                return UndeliverableReturnCode.ERROR;
            }

            // F001333-14610
            // Pass the subscription ID to be set on the message properties.
            msg = setMessageProperties(msg, exceptionReason, exceptionStrings, subscriptionID);

            /*
             * If Destination is not null, get the exception destination.
             * If it is null - this indicates the default exception destination was required
             * so use this instead.
             */

            String newExceptionDestination = null;

            if (_originalDestination != null)
            {
                newExceptionDestination = _originalDestination.getExceptionDestination();
                if (newExceptionDestination == null || newExceptionDestination.equals(""))
                {

                    if (_originalDestination.isToBeDeleted()) {
                        newExceptionDestination = _defaultExceptionDestinationName;
                        defaultInUse = true;
                    }
                    else
                    {
                        // In the past, this could only arise if we were forcing the put after the message
                        // had arrived across a link but the destination was full. i.e. BLOCK was returned but 
                        // forcePut was true so that ordering was broken (but thats what happens with links).
                        // This situation is now handled elsewhere in the code (see defect 4644630 so wE'll return an error
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "handleUndeliverableMessage", UndeliverableReturnCode.ERROR);
                        return UndeliverableReturnCode.ERROR;
                    }
                }
                if (newExceptionDestination.equals(JsConstants.DEFAULT_EXCEPTION_DESTINATION))
                {
                    newExceptionDestination = _defaultExceptionDestinationName;
                    defaultInUse = true;
                }
            }
            else
            {
                newExceptionDestination = _defaultExceptionDestinationName;
                defaultInUse = true;
            }

            /*
             * Entering synch block.
             * We MUST put the ExceptionDestination instance on a destination. Ideally
             * we would have an instance per consumer point (since there will be a
             * producerSession instance per consumerpoint). We cannot do this because we
             * cannot get references to the consumerpoints at the time we need to use the
             * ExceptionDestination.
             * 
             * Rather than perform a destination lookup every time we call
             * this method, we synchronize on one instance. If a different dest is needed
             * then we do a lookup within the synchronized block. This
             * introduces an optimisation for consecutive sends to the same exception
             * destination.
             */

            synchronized (_exceptionDestinationLock)
            {

                /*
                 * If inputhandler ref has not been created yet, or exception destination has changed
                 * since the last call of this method, then get the inputhandler associated
                 * with the given exception destination
                 * 
                 * This inputhandler will be used to send messages to the exception destination
                 */

                if (_inputHandler == null || !newExceptionDestination.equals(_exceptionDestinationName))
                {
                    _exceptionDestinationName = newExceptionDestination;

                    /*
                     * Try to get an inputhandler to the exception destination specified in the
                     * destination's definition. If this fails, (e.g. if the exception destination
                     * does not exist) then attempt to get one to the default (which should ALWAYS
                     * succeed). If this fails then report a fatal error.
                     */

                    //Get the named destination from the destination manager
                    try
                    {
                        _exceptionDestination =
                                        _messageProcessor.getDestinationManager().
                                                        getDestination(_exceptionDestinationName, true);

                        // Check whether bus security is enabled
                        if (_messageProcessor.isBusSecure())
                        {
                            // Check authority to access exc destination
                            AccessResult result = checkExceptionDestinationAccess(msg,
                                                                                  null,
                                                                                  alternateUser,
                                                                                  defaultInUse);

                            // Handle access denied
                            if (!result.isAllowed())
                            {
                                // Report and audit the event
                                handleAccessDenied(result, msg, alternateUser);

                                // Return with "BLOCK". This is the same behaviour as things like
                                // q full, etc. It means, for example, that messages will be backed
                                // up on transmission queues if they cannot be delivered to either
                                // target or exception destination.
                                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                    SibTr.exit(tc, "handleUndeliverableMessage", UndeliverableReturnCode.BLOCK);
                                return UndeliverableReturnCode.BLOCK;
                            }
                        }

                        // Set the priority in the message based on the exception destinations default
                        msg.setPriority(_exceptionDestination.getDefaultPriority());

                        // Get the inputhandler associated with it
                        _inputHandler = _exceptionDestination.getInputHandler(_exceptionDestination.isPubSub() ? ProtocolType.PUBSUBINPUT : ProtocolType.UNICASTINPUT,
                                                                              _messageProcessor.getMessagingEngineUuid(),
                                                                              null);
                    } catch (SIException e)
                    {
                        // No FFDC code needed

                        SibTr.exception(tc, e);

                        /*
                         * If it was the default exception destination we failed to reference, then exit.
                         * Otherwise retry with the default
                         */

                        if (defaultInUse)
                        {

                            /*
                             * ERROR : SYSTEM.DEFAULT.EXCEPTION.DESTINATION.<MENAME> queue did
                             * not exist on the ME. This should ALWAYS exist.
                             */
                            FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl.handleUndeliverableMessage",
                                                        "1:710:1.137",
                                                        this);

                            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                                        new Object[] {
                                                      "com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl",
                                                      "1:716:1.137",
                                                      e,
                                                      _exceptionDestinationName });

                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                SibTr.exit(tc, "handleUndeliverableMessage", UndeliverableReturnCode.ERROR);
                            return UndeliverableReturnCode.ERROR;
                        }

                        /* Generate warning but retry with default exception destination */
                        SibTr.warning(tc, "EXCEPTION_DESTINATION_WARNING_CWSIP0291",
                                      new Object[] { _exceptionDestinationName, _messageProcessor.getMessagingEngineName(), e });

                        /* Get the default and retry */
                        _exceptionDestinationName = _defaultExceptionDestinationName;

                        /* Retry on the default ex.dest. */
                        //Get the named destination from the destination manager
                        try
                        {
                            _exceptionDestination =
                                            _messageProcessor.getDestinationManager().
                                                            getDestination(_exceptionDestinationName, true);

                            // Check whether bus security is enabled
                            if (_messageProcessor.isBusSecure())
                            {
                                // Check authority to access to default exc destination
                                AccessResult result = checkExceptionDestinationAccess(msg,
                                                                                      null,
                                                                                      alternateUser,
                                                                                      true);

                                // Handle access denied
                                if (!result.isAllowed())
                                {
                                    // Report and audit the event
                                    handleAccessDenied(result, msg, alternateUser);

                                    // Return with "BLOCK". This is the same behaviour as things like
                                    // q full, etc. It means, for example, that messages will be backed
                                    // up on transmission queues if they cannot be delivered to either
                                    // target or exception destination.
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                        SibTr.exit(tc, "handleUndeliverableMessage", UndeliverableReturnCode.BLOCK);
                                    return UndeliverableReturnCode.BLOCK;
                                }
                            }

                            // Set the priority in the message based on the exception destinations default
                            msg.setPriority(_exceptionDestination.getDefaultPriority());

                            // Get the inputhandler associated with it
                            _inputHandler = _exceptionDestination.getInputHandler(_exceptionDestination.isPubSub() ? ProtocolType.PUBSUBINPUT : ProtocolType.UNICASTINPUT,
                                                                                  _messageProcessor.getMessagingEngineUuid(),
                                                                                  null);

                            defaultInUse = true;
                        } catch (SIException e2)
                        {
                            /*
                             * ERROR : SYSTEM.DEFAULT.EXCEPTION.DESTINATION.<MENAME> queue did
                             * not exist on the ME. This should ALWAYS exist.
                             */

                            FFDCFilter.processException(
                                                        e2,
                                                        "com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl.handleUndeliverableMessage",
                                                        "1:786:1.137",
                                                        this);

                            SibTr.exception(tc, e);
                            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                                        new Object[] {
                                                      "com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl",
                                                      "1:793:1.137",
                                                      e2,
                                                      _exceptionDestinationName });

                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                SibTr.exit(tc, "handleUndeliverableMessage", UndeliverableReturnCode.ERROR);
                            return UndeliverableReturnCode.ERROR;
                        }
                    } // Inputhandler to an exception destination obtained
                }

                /*
                 * Send the message using whichever exception destination we have chosen. If it
                 * is unsuccessful and it is not aimed at the default - then get an inputhandler
                 * to the default and retry the send. If a send to the default fails (which will
                 * be on the local ME) then retry every 10 seconds.
                 */

                sendComplete = false;

                while (!sendComplete)
                {

                    // 533027 We need to create a new MessageItem each time round the loop,
                    // as we could fail at an unexpected point (such as while allocating an
                    // id to the message), and hence leave the MessageItem in an inconsistent state
                    // (such as with callbacks still registered).
                    MessageItem newItem = new MessageItem(msg);
                    //Set this in the msgItem as we want to generate a new systemUUID if needed
                    newItem.setRequiresNewId(message.getRequiresNewId());

                    try
                    {
                        newItem.setStoreAtSendTime(message.isToBeStoredAtSendTime());

                        TransactionCommon transaction = null;

                        if (tran == null)
                        {
                            localTran = _exceptionDestination.getTxManager().createLocalTransaction(false);
                            transaction = localTran;
                        }
                        else
                        {
                            transaction = tran;
                        }

                        _inputHandler.handleMessage(newItem,
                                                    transaction, sourceMEUuid);

                        JsMessagingEngine me = _messageProcessor.getMessagingEngine();

                        if (me.isEventNotificationEnabled() ||
                            UserTrace.tc_mt.isDebugEnabled())
                        {
                            String apiMsgId = null;
                            String correlationId = null;

                            if (message.getMessage() instanceof JsApiMessage)
                            {
                                apiMsgId = ((JsApiMessage) message.getMessage()).getApiMessageId();
                                correlationId = ((JsApiMessage) message.getMessage()).getCorrelationId();
                            }
                            else
                            {
                                if (message.getMessage().getApiMessageIdAsBytes() != null)
                                    apiMsgId = new String(message.getMessage().getApiMessageIdAsBytes());

                                if (message.getMessage().getCorrelationIdAsBytes() != null)
                                    correlationId = new String(message.getMessage().getCorrelationIdAsBytes());
                            }

                            if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())
                            {
                                if (message.getMessage().isApiMessage())
                                    SibTr.debug(UserTrace.tc_mt,
                                                nls_mt.getFormattedMessage(
                                                                           "MESSAGE_EXCEPTION_DESTINATIONED_CWSJU0012",
                                                                           new Object[] {
                                                                                         apiMsgId,
                                                                                         message.getMessage().getSystemMessageId(),
                                                                                         correlationId,
                                                                                         _exceptionDestinationName,
                                                                                         Integer.valueOf(exceptionReason),
                                                                                         message.getMessage().getExceptionMessage() },
                                                                           null));
                            }

                            if (me.isEventNotificationEnabled())
                            {
                                fireMessageExceptionedEvent(apiMsgId,
                                                            message,
                                                            exceptionReason);
                            }
                        }

                        /* Send was successful so exit while loop and return OK */
                        sendComplete = true;
                    } catch (SIException e)
                    {
                        // No FFDC code needed
                        if (!defaultInUse)
                        {
                            /* Generate warning but retry with default exception destination */
                            SibTr.warning(tc, "EXCEPTION_DESTINATION_WARNING_CWSIP0291",
                                          new Object[] { _exceptionDestinationName, _messageProcessor.getMessagingEngineName(), e });

                            UndeliverableReturnCode rc2 = sendToDefaultExceptionDestination(msg);
                            // If the above worked then carry on, if it failed then return
                            if (rc2 != UndeliverableReturnCode.OK)
                            {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                    SibTr.exit(tc, "handleUndeliverableMessage", rc2);
                                return rc2;
                            }
                            else
                            {
                                //Everything was ok, set that we are using the default

                                defaultInUse = true;
                            }
                        }
                        else
                        {
                            if (e instanceof SIMPLimitExceededException)
                            {
                                // Warning : Default exception destination full
                                SibTr.warning(tc, "EXCEPTION_DESTINATION_WARNING_CWSIP0291",
                                              new Object[] { _exceptionDestinationName, _messageProcessor.getMessagingEngineName(), e });

                                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                    SibTr.exit(tc, "handleUndeliverableMessage", UndeliverableReturnCode.BLOCK);
                                return UndeliverableReturnCode.BLOCK;
                            }

                            /*
                             * ERROR : Cannot deliver to local SYSTEM.DEFAULT.EXCEPTION.DESTINATION.
                             */

                            FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl.handleUndeliverableMessage",
                                                        "1:938:1.137",
                                                        this);

                            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                                        new Object[] {
                                                      "com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl",
                                                      "1:944:1.137",
                                                      e,
                                                      _exceptionDestinationName });

                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                SibTr.exit(tc, "handleUndeliverableMessage", UndeliverableReturnCode.ERROR);
                            return UndeliverableReturnCode.ERROR;
                        }
                    } catch (MessageStoreRuntimeException e)
                    {
                        // No FFDC code needed
                        if (!defaultInUse)
                        {
                            UndeliverableReturnCode rc2 = sendToDefaultExceptionDestination(msg);
                            // If the above worked then carry on, if it failed then return
                            if (rc2 != UndeliverableReturnCode.OK)
                            {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                    SibTr.exit(tc, "handleUndeliverableMessage", rc2);
                                return rc2;
                            }
                            else
                            {
                                //Everything was ok, set that we are using the default
                                defaultInUse = true;
                            }
                        }
                        else
                        {
                            /*
                             * ERROR : Cannot deliver to local SYSTEM.DEFAULT.EXCEPTION.DESTINATION.
                             */

                            FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl.handleUndeliverableMessage",
                                                        "1:981:1.137",
                                                        this);
                            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                                        new Object[] {
                                                      "com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl",
                                                      "1:986:1.137",
                                                      e,
                                                      _exceptionDestinationName });

                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                SibTr.exit(tc, "handleUndeliverableMessage", UndeliverableReturnCode.ERROR);
                            return UndeliverableReturnCode.ERROR;

                        }
                    }
                }
            } // End of synchronisation

        }

        // If report messages required - generate and send here
        if (message.getMessage().getReportException() != null)
        {
            // Create the ReportHandler object if not already created
            if (_reportHandler == null)
                _reportHandler = new ReportHandler(_messageProcessor);

            TransactionCommon transaction = null;
            if (tran == null)
            {
                transaction = localTran;
            }
            else
            {
                transaction = tran;
            }

            try
            {
                _reportHandler.handleMessage(message, transaction, SIApiConstants.REPORT_EXCEPTION);
            } catch (Exception e)
            {
                // ERROR : Cannot send report message
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl.handleUndeliverableMessage",
                                            "1:1029:1.137",
                                            this);

                SibTr.error(tc, "EXCEPTION_DESTINATION_ERROR_CWSIP0295",
                            new Object[] { _exceptionDestinationName, _messageProcessor.getMessagingEngineName() });

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "handleUndeliverableMessage", UndeliverableReturnCode.ERROR);
                return UndeliverableReturnCode.ERROR;
            }
        }

        if (localTran != null)
        {
            try
            {
                localTran.commit();
            } catch (SIException e)
            {
                /*
                 * ERROR : Cannot deliver to local SYSTEM.DEFAULT.EXCEPTION.DESTINATION.
                 */

                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl.handleUndeliverableMessage",
                                            "1:1056:1.137",
                                            this);
                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                            new Object[] {
                                          "com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl",
                                          "1:1061:1.137",
                                          e,
                                          _exceptionDestinationName });

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "handleUndeliverableMessage", UndeliverableReturnCode.ERROR);
                return UndeliverableReturnCode.ERROR;
            }
        }

        // If we got here then everything worked ok
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "handleUndeliverableMessage", rc);
        return rc;
    }

    /**
     * Works out whether the message should be made to block awaiting
     * a remote ME, or posted on the local one.
     * <p>
     * If the message has requested that order be preserved, we cannot send this
     * message to an exception destination for fear of breaking that ordering.
     * Therefore we block the message.
     * <p>
     * A null exception destination name indicates we do not wish to use an
     * exception destination.
     * <p>
     * 
     * @param message The message we are routing through.
     * @return True if the message should be blocked, and should continue
     *         to be sent to a remote ME, false if the message should be re-posted
     *         locally on this ME.
     * 
     */
    private boolean isBlockRequired(SIMPMessage message)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "isBlockRequired", message);

        boolean isBlockRequired = false;

        if (_originalDestination != null)
        {
            String exceptionDestination = _originalDestination.getExceptionDestination();
            // urwashi probable fix
            boolean toBedeleted = _originalDestination.isToBeDeleted();

            if ((exceptionDestination == null) || (exceptionDestination.equals(""))) // urwashi probable fix complete
            {
                if (!toBedeleted) {
                    isBlockRequired = true;
                }
            }

        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "isBlockRequired", Boolean.valueOf(isBlockRequired));

        return isBlockRequired;
    }

    private JsMessage setMessageProperties(JsMessage msg,
                                           int exceptionReason,
                                           String[] exceptionStrings,
                                           String subscriptionID)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setMessageProperties", new Object[] { msg, Integer.valueOf(exceptionReason), exceptionStrings, subscriptionID });

        // Add the reason code (which indicates why the message could not be delivered) to
        // the message along with the string inserts for the error message
        msg.setExceptionReason(exceptionReason);
        msg.setExceptionInserts(exceptionStrings);

        // Add current time to the exceptionDestinationPutTime field of the message
        msg.setExceptionTimestamp(System.currentTimeMillis());

        // F001333-14610
        // Add subscription ID
        msg.setExceptionProblemSubscription(subscriptionID);

        // Clear relevant message properties
        msg.setReportCOA(null);
        msg.setReportCOD(null);
        msg.setReportException(null);
        msg.setReportExpiry(null);
        msg.setReportNAN(null);
        msg.setReportPAN(null);
        msg.setForwardRoutingPath(null);
        msg.setRoutingDestination(null);

        /*
         * Set the delivery delay to 0 since we are moving item to exception destination.
         * 
         * <p>
         * Otherwise the message will be locked until the delivery delay time.
         * Consider a scenario where the message is delivery delay locked for 4 min
         * and the destination is deleted.When its being relocated to exception destination
         * it will again be locked for 5 min.So totally the message will not be available
         * for 9min(5+4).
         * <p>
         * JMS20 spec does not say anything about this specifically.
         * Hence its logical to set to 0 so that it will be available immediately in the
         * exception destination.
         */
        msg.setDeliveryDelay(0);

        // Set the "problem" destination
        if (_originalDestination != null)
            msg.setExceptionProblemDestination(_originalDestination.getName());
        else
            msg.setExceptionProblemDestination(null);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setMessageProperties", msg);
        return msg;
    }

    /**
     * sendToExceptionDestination. This method is
     * exposed on the SICoreConnection interface of the Core SPI. It is used
     * to put a message to an exception destination from within a mediation.
     * 
     * This method differs in that any exception destination delivery errors
     * result in an exception being thrown.
     * 
     * @param siMessage - The undeliverable message
     * @param tran - The transaction that the message was delivered under
     * @param exceptionReason - The reason why the message could not be delivered
     * @param exceptionStrings - A list of inserts to place into an error message
     */

    public void sendToExceptionDestination(
                                           SIBusMessage siMessage,
                                           String alternateUser,
                                           TransactionCommon tran,
                                           int exceptionReason,
                                           ConnectionImpl conn,
                                           String[] exceptionStrings) throws SINotPossibleInCurrentConfigurationException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "sendToExceptionDestination",
                        new Object[] { siMessage, alternateUser, tran, Integer.valueOf(exceptionReason), conn, exceptionStrings });

        boolean sendComplete = false;
        boolean defaultInUse = false; // Assume non-default exception destination is being used.
        LocalTransaction localTran = null;

        JsMessage jsMessage = (JsMessage) siMessage;

        // It isn't really valid to give us a message with no reliability set in it but
        // as it's part of the Core SPI I guess it could happen. For that reason, rather than
        // rejecting it we patch it up (so we don't start rejecting messages we'd previously
        // of accpeted -e.g. in the unit tests).
        if (jsMessage.getReliability().equals(Reliability.NONE))
        {
            // If we've got the original destination, use the default reliability of that
            if (_originalDestination != null)
            {
                jsMessage.setReliability(_originalDestination.getDefaultReliability());
            }
            // Otherwise, get it from the system exception destination
            else
            {
                try
                {
                    _exceptionDestination =
                                    _messageProcessor.getDestinationManager().
                                                    getDestination(_exceptionDestinationName, true);

                    jsMessage.setReliability(_exceptionDestination.getDefaultReliability());
                } catch (SIException e3)
                {
                    /*
                     * ERROR : SYSTEM.DEFAULT.EXCEPTION.DESTINATION.<MENAME> queue did
                     * not exist on the ME. This should ALWAYS exist.
                     */

                    FFDCFilter.processException(
                                                e3,
                                                "com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl.sendToExceptionDestination",
                                                "1:1279:1.137",
                                                this);

                    SibTr.exception(tc, e3);

                    SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                                new Object[] {
                                              "com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl",
                                              "1:1287:1.137",
                                              e3,
                                              _exceptionDestinationName });

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "sendToExceptionDestination", e3);
                    throw new SIErrorException(e3);
                }
            }
        }

        SIMPMessage message = new MessageItem(jsMessage);

        UndeliverableReturnCode rc = checkMessage(message);
        if (rc == UndeliverableReturnCode.BLOCK)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "sendToExceptionDestination", rc);

            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0005",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl",
                                      "1:1309:1.137",
                                      rc });
            throw new SINotPossibleInCurrentConfigurationException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0005",
                                                    new Object[] {
                                                                  "com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl",
                                                                  "1:1316:1.137",
                                                                  rc },
                                                    null));
        }

        if (rc != UndeliverableReturnCode.DISCARD)
        {
            JsMessage msg = null;

            try
            {
                msg = message.getMessage().getReceived();
            } catch (MessageCopyFailedException e)
            {
                // FFDC
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl.sendToExceptionDestination",
                                            "1:1335:1.137",
                                            this);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "sendToExceptionDestination", e);

                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                            new Object[] {
                                          "com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl",
                                          "1:1344:1.137",
                                          e });

                throw new SIErrorException(
                                nls.getFormattedMessage(
                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                        new Object[] {
                                                                      "com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl",
                                                                      "1:1352:1.137",
                                                                      e },
                                                        null), e);
            }

            // F001333-14610
            // Pass null for the subscription ID as this code path is not currently
            // used in any scenario where a subscription would be present.
            msg = setMessageProperties(msg, exceptionReason, exceptionStrings, null);

            /*
             * If Destination is not null, get the exception destination.
             * If it is null - this indicates the default exception destination was required
             * so use this instead.
             */

            String newExceptionDestination = null;

            if (_originalDestination != null)
            {
                newExceptionDestination = _originalDestination.getExceptionDestination();
                if (newExceptionDestination.equals(JsConstants.DEFAULT_EXCEPTION_DESTINATION))
                {
                    newExceptionDestination = _defaultExceptionDestinationName;
                    defaultInUse = true;
                }
            }
            else
            {
                newExceptionDestination = _defaultExceptionDestinationName;
                defaultInUse = true;
            }

            /*
             * Entering synch block.
             * We MUST put the ExceptionDestination instance on a destination. Ideally
             * we would have an instance per consumer point (since there will be a
             * producerSession instance per consumerpoint). We cannot do this because we
             * cannot get references to the consumerpoints at the time we need to use the
             * ExceptionDestination.
             * 
             * Rather than perform a destination lookup every time we call
             * this method, we synchronize on one instance. If a different dest is needed
             * then we do a lookup within the synchronized block. This
             * introduces an optimisation for consecutive sends to the same exception
             * destination.
             */

            synchronized (_exceptionDestinationLock)
            {

                /*
                 * If inputhandler ref has not been created yet, or exception destination has changed
                 * since the last call of this method, then get the inputhandler associated
                 * with the given exception destination
                 * 
                 * This inputhandler will be used to send messages to the exception destination
                 */

                if (_inputHandler == null || !newExceptionDestination.equals(_exceptionDestinationName))
                {

                    _exceptionDestinationName = newExceptionDestination;

                    /*
                     * Try to get an inputhandler to the exception destination specified in the
                     * destination's definition. If this fails, (e.g. if the exception destination
                     * does not exist) then attempt to get one to the default (which should ALWAYS
                     * succeed). If this fails then report a fatal error.
                     */

                    //Get the named destination from the destination manager
                    try
                    {
                        _exceptionDestination =
                                        _messageProcessor.getDestinationManager().
                                                        getDestination(_exceptionDestinationName, true);

                        // Check whether bus security is enabled
                        if (_messageProcessor.isBusSecure())
                        {
                            // Check authority to access to exc destination
                            AccessResult result = checkExceptionDestinationAccess(msg,
                                                                                  conn,
                                                                                  alternateUser,
                                                                                  defaultInUse);

                            // Handle access denied
                            if (!result.isAllowed())
                            {
                                // Report and audit the event
                                handleAccessDenied(result, msg, alternateUser);

                                // Behave as per "BLOCK". This is the same behaviour as things like
                                // q full, etc. It means, for example, that messages will be backed
                                // up on transmission queues if they cannot be delivered to either
                                // target or exception destination.
                                rc = UndeliverableReturnCode.BLOCK;
                            }
                        }

                        // Set the priority in the message based on the exception destinations default
                        msg.setPriority(_exceptionDestination.getDefaultPriority());

                        // Get the inputhandler associated with it
                        _inputHandler = _exceptionDestination.getInputHandler(_exceptionDestination.isPubSub() ? ProtocolType.PUBSUBINPUT : ProtocolType.UNICASTINPUT,
                                                                              _messageProcessor.getMessagingEngineUuid(),
                                                                              null);
                    } catch (SIException e)
                    {
                        // No FFDC code needed

                        SibTr.exception(tc, e);

                        /*
                         * If it was the default exception destination we failed to reference, then exit.
                         * Otherwise retry with the default
                         */

                        if (defaultInUse)
                        {

                            /*
                             * ERROR : SYSTEM.DEFAULT.EXCEPTION.DESTINATION.<MENAME> queue did
                             * not exist on the ME. This should ALWAYS exist.
                             */

                            FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl.sendToExceptionDestination",
                                                        "1:1483:1.137",
                                                        this);

                            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                                        new Object[] {
                                                      "com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl",
                                                      "1:1489:1.137",
                                                      e,
                                                      _exceptionDestinationName });

                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                SibTr.exit(tc, "sendToExceptionDestination", e);

                            throw new SIErrorException(e);
                        }

                        /* Generate warning but retry with default exception destination */
                        SibTr.warning(tc, "EXCEPTION_DESTINATION_WARNING_CWSIP0291",
                                      new Object[] { _exceptionDestinationName, _messageProcessor.getMessagingEngineName(), e });

                        /* Get the default and retry */
                        _exceptionDestinationName = _defaultExceptionDestinationName;

                        /* Retry on the default ex.dest. */
                        //Get the named destination from the destination manager
                        try
                        {
                            _exceptionDestination =
                                            _messageProcessor.getDestinationManager().
                                                            getDestination(_exceptionDestinationName, true);

                            // Check whether bus security is enabled
                            if (_messageProcessor.isBusSecure())
                            {
                                // Check authority to access to default exc destination
                                AccessResult result = checkExceptionDestinationAccess(msg,
                                                                                      conn,
                                                                                      alternateUser,
                                                                                      true);

                                // Handle access denied
                                if (!result.isAllowed())
                                {
                                    // Report and audit the event
                                    handleAccessDenied(result, msg, alternateUser);

                                    // Behave as per "BLOCK". This is the same behaviour as things like
                                    // q full, etc. It means, for example, that messages will be backed
                                    // up on transmission queues if they cannot be delivered to either
                                    // target or exception destination.
                                    rc = UndeliverableReturnCode.BLOCK;
                                }
                            }

                            // Set the priority in the message based on the exception destinations default
                            msg.setPriority(_exceptionDestination.getDefaultPriority());

                            // Get the inputhandler associated with it
                            _inputHandler = _exceptionDestination.getInputHandler(_exceptionDestination.isPubSub() ? ProtocolType.PUBSUBINPUT : ProtocolType.UNICASTINPUT,
                                                                                  _messageProcessor.getMessagingEngineUuid(),
                                                                                  null);

                            defaultInUse = true;
                        } catch (SIException e2)
                        {
                            /*
                             * ERROR : SYSTEM.DEFAULT.EXCEPTION.DESTINATION.<MENAME> queue did
                             * not exist on the ME. This should ALWAYS exist.
                             */

                            FFDCFilter.processException(
                                                        e2,
                                                        "com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl.sendToExceptionDestination",
                                                        "1:1558:1.137",
                                                        this);

                            SibTr.exception(tc, e2);

                            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                                        new Object[] {
                                                      "com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl",
                                                      "1:1566:1.137",
                                                      e2,
                                                      _exceptionDestinationName });

                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                SibTr.exit(tc, "sendToExceptionDestination", e2);
                            throw new SIErrorException(e2);
                        }
                    } // Inputhandler to an exception destination obtained
                }

                // If the put to the exc dest was not authorized, then this flag will
                // have been set to BLOCK. Here we throw an exception
                if (rc == UndeliverableReturnCode.BLOCK)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "sendToExceptionDestination", rc);

                    throw new SINotAuthorizedException(
                                    nls.getFormattedMessage(
                                                            "EXCEPTION_DESTINATION_AUTH_ERROR_SIMP0314",
                                                            new Object[] { _exceptionDestinationName,
                                                                          msg.getSecurityUserid() },
                                                            null));
                }

                /*
                 * Send the message using whichever exception destination we have chosen. If it
                 * is unsuccessful and it is not aimed at the default - then get an inputhandler
                 * to the default and retry the send. If a send to the default fails (which will
                 * be on the local ME) then retry every 10 seconds.
                 */

                sendComplete = false;

                while (!sendComplete)
                {
                    // 533027 We need to create a new MessageItem each time round the loop,
                    // as we could fail at an unexpected point (such as while allocating an
                    // id to the message), and hence leave the MessageItem in an inconsistent state
                    // (such as with callbacks still registered).
                    MessageItem newItem = new MessageItem(msg);

                    try
                    {
                        newItem.setStoreAtSendTime(message.isToBeStoredAtSendTime());

                        TransactionCommon transaction = null;
                        if (tran == null)
                        {
                            localTran = _exceptionDestination.getTxManager().createLocalTransaction(false);
                            transaction = localTran;
                        }
                        else
                        {
                            transaction = tran;
                        }

                        _inputHandler.handleMessage(newItem, transaction, _messageProcessor.getMessagingEngineUuid());

                        // Report that the message has been successfully exceptioned.
                        JsMessagingEngine me = _messageProcessor.getMessagingEngine();
                        if (me.isEventNotificationEnabled() ||
                            UserTrace.tc_mt.isDebugEnabled())
                        {
                            String apiMsgId = null;

                            if (message.getMessage() instanceof JsApiMessage)
                                apiMsgId = ((JsApiMessage) message.getMessage()).getApiMessageId();
                            else
                            {
                                if (message.getMessage().getApiMessageIdAsBytes() != null)
                                    apiMsgId = message.getMessage().getApiMessageIdAsBytes().toString();
                            }

                            if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())
                            {
                                if (message.getMessage().isApiMessage())
                                    SibTr.debug(UserTrace.tc_mt,
                                                nls_mt.getFormattedMessage(
                                                                           "MESSAGE_EXCEPTION_DESTINATIONED_CWSJU0012",
                                                                           new Object[] {
                                                                                         apiMsgId,
                                                                                         message.getMessage().getSystemMessageId(),
                                                                                         _exceptionDestinationName,
                                                                                         Integer.valueOf(exceptionReason),
                                                                                         message.getMessage().getExceptionMessage() },
                                                                           null));
                            }

                            if (me.isEventNotificationEnabled())
                            {
                                fireMessageExceptionedEvent(apiMsgId,
                                                            message,
                                                            exceptionReason);
                            }
                        }

                        /* Send was successful so exit while loop and return OK */
                        sendComplete = true;
                    } catch (SIException e)
                    {
                        // No FFDC code needed
                        if (!defaultInUse)
                        {

                            /* Generate warning but retry with default exception destination */
                            SibTr.warning(tc, "EXCEPTION_DESTINATION_WARNING_CWSIP0291",
                                          new Object[] { _exceptionDestinationName, _messageProcessor.getMessagingEngineName(), e });

                            /* Obtain inputhandler to default exception destination and send to this */
                            _exceptionDestinationName = _defaultExceptionDestinationName;
                            try
                            {
                                _exceptionDestination =
                                                _messageProcessor.getDestinationManager().
                                                                getDestination(_exceptionDestinationName, true);

                                // Set the priority in the message based on the exception destinations default
                                msg.setPriority(_exceptionDestination.getDefaultPriority());

                                // Get the inputhandler associated with it
                                _inputHandler = _exceptionDestination.getInputHandler(_exceptionDestination.isPubSub() ? ProtocolType.PUBSUBINPUT : ProtocolType.UNICASTINPUT,
                                                                                      _messageProcessor.getMessagingEngineUuid(),
                                                                                      null);

                                defaultInUse = true;
                            } catch (SIException e2)
                            {
                                FFDCFilter.processException(
                                                            e2,
                                                            "com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl.sendToExceptionDestination",
                                                            "1:1702:1.137",
                                                            this);

                                /*
                                 * ERROR : SYSTEM.DEFAULT.EXCEPTION.DESTINATION.<MENAME> queue did
                                 * not exist on the ME. This should ALWAYS exist.
                                 */

                                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                                            new Object[] {
                                                          "com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl",
                                                          "1:1713:1.137",
                                                          e2,
                                                          _exceptionDestinationName });

                                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                    SibTr.exit(tc, "sendToExceptionDestination", e2);
                                throw new SIErrorException(e2);
                            }
                        }
                        else
                        {
                            if (e instanceof SIMPLimitExceededException)
                            {
                                // Warning : Default exception destination full
                                SibTr.warning(tc, "EXCEPTION_DESTINATION_WARNING_CWSIP0291",
                                              new Object[] { _exceptionDestinationName, _messageProcessor.getMessagingEngineName(), e });

                                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                    SibTr.exit(tc, "sendToExceptionDestination", UndeliverableReturnCode.BLOCK);
                                throw (SIMPLimitExceededException) e;
                            }
                            /*
                             * ERROR : Cannot deliver to local SYSTEM.DEFAULT.EXCEPTION.DESTINATION.
                             */

                            FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl.sendToExceptionDestination",
                                                        "1:1741:1.137",
                                                        this);

                            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                                        new Object[] {
                                                      "com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl",
                                                      "1:1747:1.137",
                                                      e,
                                                      _exceptionDestinationName });

                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                SibTr.exit(tc, "sendToExceptionDestination", e);
                            throw new SIErrorException(e);

                        }
                    }
                }
            } // End of synchronisation
        }

        // If report messages required - generate and send here
        if (message.getMessage().getReportException() != null)
        {
            // Create the ReportHandler object if not already created
            if (_reportHandler == null)
                _reportHandler = new ReportHandler(_messageProcessor);

            TransactionCommon transaction;
            if (tran == null)
            {
                transaction = localTran;
            }
            else
            {
                transaction = tran;
            }

            try
            {
                _reportHandler.handleMessage(message, transaction, SIApiConstants.REPORT_EXCEPTION);
            } catch (SIException e)
            {
                /*
                 * ERROR : Cannot send report message
                 */

                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl.sendToExceptionDestination",
                                            "1:1791:1.137",
                                            this);

                SibTr.error(tc, "EXCEPTION_DESTINATION_ERROR_CWSIP0295",
                            new Object[] { _exceptionDestinationName, _messageProcessor.getMessagingEngineName() });

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "sendToExceptionDestination", e);
                throw new SIResourceException(e);
            }
        }

        if (localTran != null)
        {
            try
            {
                localTran.commit();
            } catch (SIException e)
            {
                /*
                 * ERROR : Cannot deliver to local SYSTEM.DEFAULT.EXCEPTION.DESTINATION.
                 */

                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl.sendToExceptionDestination",
                                            "1:1819:1.137",
                                            this);
                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                            new Object[] {
                                          "com.ibm.ws.sib.processor.impl.sendToExceptionDestination",
                                          "1:1824:1.137",
                                          e,
                                          _exceptionDestinationName });

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "sendToExceptionDestination", UndeliverableReturnCode.ERROR);

                throw new SIResourceException(e);
            }
        }

        // If we got here then everything worked ok
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "sendToExceptionDestination");

    }

    /**
     * Checks that a message is valid for delivery to an exception
     * destination
     * 
     * @param message
     * @return
     */
    private UndeliverableReturnCode checkMessage(SIMPMessage message)
    {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkMessage", message);

        UndeliverableReturnCode rc = UndeliverableReturnCode.OK;

        // F001333:E3
        // If the message's reliability equals or is less than the configured ExceptionDiscardReliability
        // then the message shouldn't be sent on to the exception destination, instead it should simply
        // be thrown away (the default setting is BestEffort).

        // We'll always chuck away BestEffort messages, but if we have the original destination we'll base
        // our decision on its configuration
        // (If the _originalDestination is null then we do not have the original destination's config to hand,
        // for example, in the case of  cleaning up a deleted destination)

        Reliability discardReliabilityThreshold = Reliability.BEST_EFFORT_NONPERSISTENT;
        if (_originalDestination != null)
            discardReliabilityThreshold = _originalDestination.getExceptionDiscardReliability();

        if (message.getReliability().compareTo(discardReliabilityThreshold) <= 0)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Message reliability (" + message.getReliability() + ") <= Exception reliability (" +
                                discardReliabilityThreshold + ")");

            rc = UndeliverableReturnCode.DISCARD;
        }

        // Discard messages from temporary destinations.
        else if (_originalDestination != null &&
                 _originalDestination.isTemporary())
            rc = UndeliverableReturnCode.DISCARD;

        // If the discardMessage option is set, then we discard the message rather
        // than send to the exception destination
        else if (Boolean.TRUE.equals(message.getMessage().getReportDiscardMsg()))
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Message discarded at sender's request");

            rc = UndeliverableReturnCode.DISCARD;
        }

        // Decide whether we want to block the message or not.
        else if (isBlockRequired(message))
            rc = UndeliverableReturnCode.BLOCK;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkMessage", rc);

        return rc;
    }

    /**
     * Checks authority to access exception destination.
     * 
     * @param message
     * @return
     */
    private AccessResult checkExceptionDestinationAccess(JsMessage msg,
                                                         ConnectionImpl conn,
                                                         String alternateUser,
                                                         boolean defaultInUse)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkExceptionDestinationAccess", new Object[] { msg,
                                                                             conn,
                                                                             alternateUser,
                                                                             Boolean.valueOf(defaultInUse) });

        // Will drive the form of sib.security checkDestinationAccess() that
        // takes a JsMessage
        SecurityContext secContext = new SecurityContext(msg,
                        alternateUser,
                        null,
                        _messageProcessor.getAuthorisationUtils());
        boolean allowed = true;
        boolean identityAdoptionAllowed = true;

        if (defaultInUse)
        {
            // We're using the default exc destination for the ME, call
            // checkDestinationAccess() with the default exc dest prefix
            // rather than the fully qualified name

            // First we do the alternate user check. If an alternateUser was set then
            // (A )We need to determine whether the connected subject has the authority to
            //     perform alternate user checks. It is assumed that if there is no connection
            //     associated with this call, then we're privileged anyway.
            // (B) We need to do the destination access check with the supplied alternate
            //     user.

            if (conn != null)// conn will be null if we've followed the handleUndeliverableMessage route
                             // (privileged) rather than the sendToExceptionDestination route
                             // where the Core SPI has been driven

            {
                // Careful, we need a sec context for the connection rather than that associated
                // with the message.
                SecurityContext connSecContext = new SecurityContext(conn.getSecuritySubject(),
                                alternateUser,
                                null,
                                _messageProcessor.getAuthorisationUtils());
                // Alternate user check
                if (alternateUser != null)
                {
                    if (!_messageProcessor.
                                    getAccessChecker().
                                    checkDestinationAccess(connSecContext,
                                                           null, // home bus
                                                           SIMPConstants.SYSTEM_DEFAULT_EXCEPTION_DESTINATION_PREFIX,
                                                           OperationType.IDENTITY_ADOPTER))
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(tc, "checkExceptionDestinationAccess", "not authorized to perform alternate user checks on this destination");

                        allowed = false;
                        identityAdoptionAllowed = false;
                    }
                }

                // Now check access authority on the destination itself
                if (allowed)
                {
                    if (!_messageProcessor.
                                    getAccessChecker().
                                    checkDestinationAccess(connSecContext,
                                                           null, // home bus
                                                           SIMPConstants.SYSTEM_DEFAULT_EXCEPTION_DESTINATION_PREFIX,
                                                           OperationType.SEND))
                    {
                        allowed = false;
                    }
                }
            }
            else // handleUndeliverableMessage route
            {
                if (!_messageProcessor.
                                getAccessChecker().
                                checkDestinationAccess(secContext,
                                                       null, // home bus
                                                       SIMPConstants.SYSTEM_DEFAULT_EXCEPTION_DESTINATION_PREFIX,
                                                       OperationType.SEND))
                {
                    allowed = false;
                }
            }
        }
        else
        {
            // Same style of processing as above
            if (conn != null) // conn will be null if we've followed the handleUndeliverableMessage route
                              // (privileged) rather than the sendToExceptionDestination route
                              // where the Core SPI has been driven
            {
                // Careful, we need a sec context for the connection rather than that associated
                // with the message.
                SecurityContext connSecContext = new SecurityContext(conn.getSecuritySubject(),
                                alternateUser,
                                null,
                                _messageProcessor.getAuthorisationUtils());

                // Alternate user check
                if (alternateUser != null)
                {
                    if (!_exceptionDestination.
                                    checkDestinationAccess(connSecContext,
                                                           OperationType.IDENTITY_ADOPTER))
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(tc, "checkExceptionDestinationAccess", "not authorized to perform alternate user checks on this destination");

                        allowed = false;
                        identityAdoptionAllowed = false;
                    }
                }

                // Now check access authority on the destination itself
                if (allowed)
                {
                    if (!_exceptionDestination.
                                    checkDestinationAccess(connSecContext,
                                                           OperationType.SEND))
                    {
                        allowed = false;
                    }
                }

            }
            else // handleUndeliverableMessage route
            {
                // Check authority to produce to destination
                if (!_exceptionDestination.
                                checkDestinationAccess(secContext,
                                                       OperationType.SEND))
                {
                    allowed = false;
                }
            }
        }

        AccessResult result = new AccessResult(allowed, identityAdoptionAllowed);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc,
                       "checkExceptionDestinationAccess",
                       result);

        return result;
    }

    /**
     * Fire an event if eventing is enabled and write an audit record.
     * 
     * @param result
     * @param msg
     * @param alternateUser
     */
    private void handleAccessDenied(AccessResult result,
                                    JsMessage msg,
                                    String alternateUser)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "handleAccessDenied", new Object[] { result,
                                                                msg,
                                                                alternateUser });

        // Determine the username and operation for messaging
        String userName = null;
        OperationType operationType = null;

        if (result.didIdentityAdoptionFail())
        {
            userName = msg.getSecurityUserid();
            operationType = OperationType.IDENTITY_ADOPTER;
        }
        else
        {
            operationType = OperationType.SEND;

            if (alternateUser != null)
                userName = alternateUser;
            else
                userName = msg.getSecurityUserid();
        }

        // Build the message for the Exception and the Notification
        String nlsMessage =
                        nls.getFormattedMessage("USER_NOT_AUTH_EXCEPTION_DEST_ERROR_CWSIP0313",
                                                new Object[] { userName,
                                                              _exceptionDestinationName },
                                                null);

        // Fire a Notification if Eventing is enabled
        _messageProcessor.
                        getAccessChecker().
                        fireDestinationAccessNotAuthorizedEvent(_exceptionDestinationName,
                                                                userName,
                                                                operationType,
                                                                nlsMessage);

        // Report the not authorized condition to the console
        SibTr.warning(tc,
                      "USER_NOT_AUTH_EXCEPTION_DEST_ERROR_CWSIP0313",
                      new Object[] { userName,
                                    _exceptionDestinationName }
                        );

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc,
                       "handleAccessDenied");
    }

    /**
     * Holds the information acquired from authorization checks
     */
    private class AccessResult
    {
        private boolean _allowed = false;
        private boolean _identityAdoptionFailed = false;

        AccessResult(boolean allowed,
                     boolean identityAdoptionFailed)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(tc, "AccessResult",
                            new Object[] { Boolean.valueOf(allowed), Boolean.valueOf(identityAdoptionFailed) });
            _allowed = allowed;
            _identityAdoptionFailed = identityAdoptionFailed;
        }

        /**
         * @return
         */
        public boolean isAllowed()
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                SibTr.entry(tc, "isAllowed");
                SibTr.exit(tc, "isAllowed", Boolean.valueOf(_allowed));
            }
            return _allowed;
        }

        /**
         * @return
         */
        public boolean didIdentityAdoptionFail()
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                SibTr.entry(tc, "didIdentityAdoptionFail");
                SibTr.exit(tc, "didIdentityAdoptionFail", Boolean.valueOf(_identityAdoptionFailed));
            }
            return _identityAdoptionFailed;
        }

        @Override
        public String toString() {
            return "_allowed=" + Boolean.valueOf(_allowed) + ",_identityAdoptionFailed=" + Boolean.valueOf(_identityAdoptionFailed);
        }
    }

    /**
     * Fire an event notification of type TYPE_SIB_MESSAGE_EXCEPTIONED
     * 
     * @param newState
     */
    private void fireMessageExceptionedEvent(String apiMsgId,
                                             SIMPMessage message,
                                             int exceptionReason)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc,
                        "fireMessageExceptionedEvent",
                        new Object[] { apiMsgId,
                                      message,
                                      Integer.valueOf(exceptionReason) });

        JsMessagingEngine me = _messageProcessor.getMessagingEngine();
        RuntimeEventListener listener = _messageProcessor.getRuntimeEventListener();

        String systemMessageId = message.getMessage().getSystemMessageId();

        // Check that we have a RuntimeEventListener
        if (listener != null)
        {
            // Build the message for the Notification

            String nlsMessage =
                            nls_mt.getFormattedMessage(
                                                       "MESSAGE_EXCEPTION_DESTINATIONED_CWSJU0012",
                                                       new Object[] {
                                                                     apiMsgId,
                                                                     systemMessageId,
                                                                     _exceptionDestinationName,
                                                                     Integer.valueOf(exceptionReason),
                                                                     message.getMessage().getExceptionMessage() },
                                                       null);

            // Build the properties for the Notification
            Properties props = new Properties();

            // Set values for the intended destination
            String intendedDestinationName = "";
            String intendedDestinationUuid = SIBUuid12.toZeroString();

            if (_originalDestination != null)
            {
                intendedDestinationName = _originalDestination.getName();
                intendedDestinationUuid = _originalDestination.getUuid().toString();
            }
            props.put(SibNotificationConstants.KEY_INTENDED_DESTINATION_NAME,
                      intendedDestinationName);
            props.put(SibNotificationConstants.KEY_INTENDED_DESTINATION_UUID,
                      intendedDestinationUuid);

            // Set values for the exception destination
            props.put(SibNotificationConstants.KEY_EXCEPTION_DESTINATION_NAME,
                      _exceptionDestination.getName());
            props.put(SibNotificationConstants.KEY_EXCEPTION_DESTINATION_UUID,
                      _exceptionDestination.getUuid().toString());

            props.put(SibNotificationConstants.KEY_SYSTEM_MESSAGE_IDENTIFIER,
                      (systemMessageId == null) ? "" : systemMessageId);

            props.put(SibNotificationConstants.KEY_MESSAGE_EXCEPTION_REASON,
                      String.valueOf(exceptionReason));

            // Fire the event
            listener.runtimeEventOccurred(me,
                                          SibNotificationConstants.TYPE_SIB_MESSAGE_EXCEPTIONED,
                                          nlsMessage,
                                          props);
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Null RuntimeEventListener, cannot fire event");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "fireMessageExceptionedEvent");
    }

    private UndeliverableReturnCode sendToDefaultExceptionDestination(JsMessage msg)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "sendToDefaultExceptionDestination", msg);

        /* Obtain inputhandler to default exception destination and send to this */
        _exceptionDestinationName = _defaultExceptionDestinationName;
        try
        {
            _exceptionDestination =
                            _messageProcessor.getDestinationManager().
                                            getDestination(_exceptionDestinationName, true);

            // Set the priority in the message based on the exception destinations default
            msg.setPriority(_exceptionDestination.getDefaultPriority());

            // Get the inputhandler associated with it        
            _inputHandler = _exceptionDestination.getInputHandler(_exceptionDestination.isPubSub() ? ProtocolType.PUBSUBINPUT : ProtocolType.UNICASTINPUT,
                                                                  _messageProcessor.getMessagingEngineUuid(),
                                                                  null);

        } catch (SIException e)
        {
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl.sendToDefaultExceptionDestination",
                                        "1:2283:1.137",
                                        this);

            /*
             * ERROR : SYSTEM.DEFAULT.EXCEPTION.DESTINATION.<MENAME> queue did
             * not exist on the ME. This should ALWAYS exist.
             */

            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl",
                                      "1:2294:1.137",
                                      e,
                                      _exceptionDestinationName });

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "sendToDefaultExceptionDestination", UndeliverableReturnCode.ERROR);
            return UndeliverableReturnCode.ERROR;
        } catch (MessageStoreRuntimeException e)
        {
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl.sendToDefaultExceptionDestination",
                                        "1:2307:1.137",
                                        this);

            /*
             * ERROR : SYSTEM.DEFAULT.EXCEPTION.DESTINATION.<MENAME> queue did
             * not exist on the ME. This should ALWAYS exist.
             */

            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl",
                                      "1:2318:1.137",
                                      e,
                                      _exceptionDestinationName });

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "sendToDefaultExceptionDestination", UndeliverableReturnCode.ERROR);
            return UndeliverableReturnCode.ERROR;
        }

        // Sending to the default exception destination worked, return OK
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "sendToDefaultExceptionDestination", UndeliverableReturnCode.OK);
        return UndeliverableReturnCode.OK;
    }

}
