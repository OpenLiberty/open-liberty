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

package com.ibm.ws.sib.processor.impl.store.itemstreams;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.SIRCConstants;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.LocalizationDefinition;
import com.ibm.ws.sib.mfp.ProtocolType;
import com.ibm.ws.sib.msgstore.DeliveryDelayDeleteFilter;
import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.Statistics;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.UndeliverableReturnCode;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.ConsumerDispatcher;
import com.ibm.ws.sib.processor.impl.ConsumerDispatcherState;
import com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl;
import com.ibm.ws.sib.processor.impl.LinkHandler;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumerManager;
import com.ibm.ws.sib.processor.impl.interfaces.ProducerInputHandler;
import com.ibm.ws.sib.processor.impl.store.items.MessageItem;
import com.ibm.ws.sib.processor.runtime.impl.LocalQueuePoint;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author millwood
 * 
 *         This object is used to store the messages for a destination
 *         localisation.
 */
public class PtoPLocalMsgsItemStream extends PtoPMessageItemStream
{
    private static final TraceComponent tc =
                    SibTr.register(
                                   PtoPLocalMsgsItemStream.class,
                                   SIMPConstants.MP_TRACE_GROUP,
                                   SIMPConstants.RESOURCE_BUNDLE);

    /**
     * NLS for component
     */
    private static final TraceNLS nls =
                    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

    /**
     * A snap-shot of the localization information we were last passed
     * by dynamic admin.
     * <p>
     * Keeping a safe cloned copy of the definition they last gave us allows
     * us to tell when a particular value has changed, causing us to use the
     * setter method to update the current localization definition value.
     */
    private LocalizationDefinition _lastLocalizationDefinitionSetByAdmin = null;

    protected static final int TRANSACTION_BATCH_SIZE = 50;

    /**
     * The currently used value of isSendAllowed
     */
    private boolean _isSendAllowed;

    /**
     * The currently used value of alterationTime
     */
    private long _alterationTime;

    /**
     * The currently used value of the localization name.
     */
    private String _name;

    //private static final TraceNLS nls =
    //  TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

    /**
     * Warm start constructor invoked by the Message Store.
     */
    public PtoPLocalMsgsItemStream()
    {
        super();
        // This space intentionally blank
    }

    /**
     * <p>Cold start PtoPMessageItemStream constructor.</p>
     * 
     * @param destinationHandler
     */
    public PtoPLocalMsgsItemStream(BaseDestinationHandler destinationHandler,
                                   SIBUuid8 messagingEngineUuid)
    {
        super(destinationHandler, messagingEngineUuid, false);
    }

    /**
     * @throws SIResourceException
     */
    @Override
    public boolean reallocateMsgs()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "reallocateMsgs");

        //Attempt to reallocate the messages to another localisation of the destination,
        //or to the exception destination, or discard the messages.
        BaseDestinationHandler destinationHandler = getDestinationHandler();
        MessageProcessor messageProcessor = destinationHandler.getMessageProcessor();
        ExceptionDestinationHandlerImpl exceptionDestinationHandlerImpl = null;

        LocalTransaction transaction = getDestinationHandler().
                        getTxManager().
                        createLocalTransaction(false);
        Transaction msTran;
        try
        {
            msTran = getDestinationHandler().
                            getTxManager().resolveAndEnlistMsgStoreTransaction(transaction);
        } catch (SIResourceException e)
        {
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPLocalMsgsItemStream.reallocateMsgs",
                                        "1:172:1.85",
                                        this);

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(
                           tc,
                           "reallocateMsgs",
                           nls.getFormattedMessage("DESTINATION_DELETED_ERROR_CWSIP0550",
                                                   new Object[] { destinationHandler.getName(), destinationHandler.getUuid().toString() },
                                                   null));

            handleRollback(transaction);

            return false;
        }
        int transactionSize = 0;

        Item item = null;

        try
        {
            /*
             * Since we are a LocalMsgItemStream (not an XMit) then the only place to reallocate
             * is to the exception destination
             */

            if (destinationHandler.isToBeDeleted())
            {

                /*
                 * Reallocate any available messages
                 * 
                 * Introduce a new DeliveryDelayDeleteFilter which basically retrieves
                 * the messages which are either available or which are locked
                 * for delivery delay It is just a marker class.Earlier to
                 * delivery delay feature null was being passed
                 */
                while (null != (item = findFirstMatchingItem(new DeliveryDelayDeleteFilter())))
                {
                    MessageItem message = (MessageItem) item;

                    /*
                     * The next step is to determine whether the messaging engine should be
                     * discarding messages from deleted destinations, or moving applicable
                     * messages to the default exception destination
                     */
                    if (!messageProcessor.discardMsgsAfterQueueDeletion() && !destinationHandler.isTemporary())
                    {
                        /*
                         * The messages should be moved to the exception destination
                         */

                        if (exceptionDestinationHandlerImpl == null)
                        {
                            //Create an exception destination handler              
                            exceptionDestinationHandlerImpl = new ExceptionDestinationHandlerImpl(destinationHandler);
                        }

                        String destName = getDestinationHandler().getName();
                        if (getDestinationHandler().isLink())
                            destName = ((LinkHandler) getDestinationHandler()).getBusName();

                        final UndeliverableReturnCode rc =
                                        exceptionDestinationHandlerImpl.handleUndeliverableMessage(message
                                                                                                   , transaction
                                                                                                   , SIRCConstants.SIRC0032_DESTINATION_DELETED_ERROR
                                                                                                   , new String[] { destName, messageProcessor.getMessagingEngineName() }
                                                        );

                        if (rc == UndeliverableReturnCode.OK || rc == UndeliverableReturnCode.DISCARD)
                        {
                            try
                            {
                                // remove it from this itemstream
                                item.remove(msTran, NO_LOCK_ID);
                                transactionSize++;
                                if (rc == UndeliverableReturnCode.OK)
                                    transactionSize++;
                            } catch (MessageStoreException e)
                            {
                                // Cannot remove message, so FFDC
                                FFDCFilter.processException(
                                                            e,
                                                            "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPLocalMsgsItemStream.reallocateMsgs",
                                                            "1:254:1.85",
                                                            this);

                                SibTr.exception(tc, e);

                                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                    SibTr.exit(
                                               tc,
                                               "reallocateMsgs",
                                               nls.getFormattedMessage("DESTINATION_DELETED_ERROR_CWSIP0550",
                                                                       new Object[] { destinationHandler.getName(), destinationHandler.getUuid().toString() },
                                                                       null));
                            }
                        }
                        else
                        {

                            //We cannot put the message to the exception destination.  All we can
                            //do in this case is rollback the users transaction.

                            SIErrorException e = new SIErrorException(nls.getFormattedMessage(
                                                                                              "DESTINATION_DELETED_ERROR_CWSIP0550",
                                                                                              new Object[] { destinationHandler.getName(), destinationHandler.getUuid().toString() },
                                                                                              null));

                            // Cannot put message to exception destination, so FFDC
                            FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPLocalMsgsItemStream.reallocateMsgs",
                                                        "1:283:1.85",
                                                        this);

                            SibTr.exception(tc, e);

                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                SibTr.exit(
                                           tc,
                                           "reallocateMsgs",
                                           nls.getFormattedMessage("DESTINATION_DELETED_ERROR_CWSIP0550",
                                                                   new Object[] { destinationHandler.getName(), destinationHandler.getUuid().toString() },
                                                                   null));

                            handleRollback(transaction);

                            return false;
                        }
                    }
                    else
                    {
                        /*
                         * The messaging engine is set to discard messages
                         */

                        try
                        {
                            //Discard the message
                            // remove it from this itemstream
                            item.remove(msTran, NO_LOCK_ID);
                            transactionSize++;
                        } catch (MessageStoreException e)
                        {
                            // Cannot remove message, so FFDC
                            FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPLocalMsgsItemStream.reallocateMsgs",
                                                        "1:320:1.85",
                                                        this);

                            SibTr.exception(tc, e);

                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                SibTr.exit(
                                           tc,
                                           "reallocateMsgs",
                                           nls.getFormattedMessage("DESTINATION_DELETED_ERROR_CWSIP0550",
                                                                   new Object[] { destinationHandler.getName(), destinationHandler.getUuid().toString() },
                                                                   null));
                        }
                    }

                    if (transactionSize > TRANSACTION_BATCH_SIZE)
                    {
                        //Commit the transaction and start another one
                        try
                        {
                            transaction.commit();
                            transaction = getDestinationHandler().
                                            getTxManager().
                                            createLocalTransaction(false);
                            msTran = getDestinationHandler().
                                            getTxManager().
                                            resolveAndEnlistMsgStoreTransaction(transaction);
                            transactionSize = 0;
                        } catch (SIException e)
                        {
                            // Error with the transaction, so cannot put message to exception destination, so FFDC
                            FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPLocalMsgsItemStream.reallocateMsgs",
                                                        "1:355:1.85",
                                                        this);

                            SibTr.exception(tc, e);

                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                SibTr.exit(
                                           tc,
                                           "reallocateMsgs",
                                           nls.getFormattedMessage("DESTINATION_DELETED_ERROR_CWSIP0550",
                                                                   new Object[] { destinationHandler.getName(), destinationHandler.getUuid().toString() },
                                                                   null));
                        }
                    }
                } //end while loop
            }
        } catch (MessageStoreException e)
        {
            // Cannot browse item from itemstream, so FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPLocalMsgsItemStream.reallocateMsgs",
                                        "1:378:1.85",
                                        this);

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(
                           tc,
                           "reallocateMsgs",
                           nls.getFormattedMessage("DESTINATION_DELETED_ERROR_CWSIP0550",
                                                   new Object[] { destinationHandler.getName(), destinationHandler.getUuid().toString() },
                                                   null));

            handleRollback(transaction);

            return false;
        }

        try
        {
            transaction.commit();
        } catch (SIException e)
        {
            // Cannot commit transactiob, so FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPLocalMsgsItemStream.reallocateMsgs",
                                        "1:405:1.85",
                                        this);

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(
                           tc,
                           "reallocateMsgs",
                           nls.getFormattedMessage("DESTINATION_DELETED_ERROR_CWSIP0550",
                                                   new Object[] { destinationHandler.getName(), destinationHandler.getUuid().toString() },
                                                   null));

            //Try and rollback the transaction
            handleRollback(transaction);

            return false;
        }

        //Keep this section.  It is part of delete processing!
        Statistics statistics = null;
        long totalMsgCount = -1;
        try
        {
            statistics = getStatistics();
            totalMsgCount = statistics.getTotalItemCount();
        } catch (MessageStoreException e)
        {
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPLocalMsgsItemStream.reallocateMsgs",
                                        "1:438:1.85",
                                        this);

            // Exception should force a false result to be returned

            SibTr.exception(tc, e);
        }

        boolean result = false;

        // Check if the reallocation was completely successful.
        if (totalMsgCount == 0)
        {
            result = true;

            if ((isToBeDeleted()) || (destinationHandler.isToBeDeleted()))
            {
                result = false;

                //The localisation should be deleted, but this can only occur if there are
                //no inbound streams to the localisation.  This check is made here, and
                //if there are none, the localisation is removed from the destination.
                ProducerInputHandler inputHandler = (ProducerInputHandler) destinationHandler.getInputHandler(ProtocolType.UNICASTINPUT, null, null);
                boolean streamsEmpty = inputHandler.getInboundStreamsEmpty();

                if (streamsEmpty)
                {
                    transaction = getDestinationHandler().
                                    getTxManager().
                                    createLocalTransaction(false);
                    try
                    {
                        msTran = getDestinationHandler().
                                        getTxManager().resolveAndEnlistMsgStoreTransaction(transaction);
                    } catch (SIResourceException e)
                    {
                        FFDCFilter.processException(
                                                    e,
                                                    "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPLocalMsgsItemStream.reallocateMsgs",
                                                    "1:479:1.85",
                                                    this);

                        SibTr.exception(tc, e);

                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(
                                       tc,
                                       "reallocateMsgs",
                                       nls.getFormattedMessage("DESTINATION_DELETED_ERROR_CWSIP0550",
                                                               new Object[] { destinationHandler.getName(), destinationHandler.getUuid().toString() },
                                                               null));

                        handleRollback(transaction);

                        return false;
                    }

                    try
                    {
                        SIMPReferenceStream referenceStream = null;
                        while (null != (referenceStream = (SIMPReferenceStream) findFirstMatchingReferenceStream(null)))
                        {
                            referenceStream.removeAll(msTran);
                        }
                        removeItemStream(msTran, NO_LOCK_ID);
                    } catch (MessageStoreException e)
                    {
                        FFDCFilter.processException(
                                                    e,
                                                    "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPLocalMsgsItemStream.reallocateMsgs",
                                                    "1:511:1.85",
                                                    this);

                        SibTr.exception(tc, e);

                        //There is no point in throwing this exception, as this is an internal task driving
                        //this call.  Instead, just exit.

                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "reallocateMsgs", e);

                        return false;
                    }

                    try
                    {
                        transaction.commit();
                    } catch (SIException e)
                    {
                        FFDCFilter.processException(
                                                    e,
                                                    "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPLocalMsgsItemStream.reallocateMsgs",
                                                    "1:534:1.85",
                                                    this);

                        SibTr.exception(tc, e);

                        //There is no point in throwing this exception, as this is an internal task driving
                        //this call.  Instead, just exit.

                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "reallocateMsgs", e);

                        return false;
                    }
                    result = true;
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "reallocateMsgs", new Boolean(result));

        return result;
    }

    /**
     * <p>This method returns the LocalizationDefinition which is the
     * administrative object that defines the behaviour of the localisation.</p>
     * <p>For now we'll return back the same one as the admin folks gave us
     * the last time, but this does not represent the "current" values
     * necessarily.
     */
    public LocalizationDefinition getLocalizationDefinition()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getLocalizationDefinition");
            SibTr.exit(tc, "getLocalizationDefinition", _lastLocalizationDefinitionSetByAdmin);
        }

        return _lastLocalizationDefinitionSetByAdmin;
    }

    /**
     * Method updateLocalizationDefinition.
     * <p>Update the localisation definition associated with the local localisation.</p>
     * 
     * @param localizationDefinition Has no effect if null is passed.
     */
    public void updateLocalizationDefinition(
                                             LocalizationDefinition newLocalizationDefinition)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "updateLocalizationDefinition", newLocalizationDefinition);

        // If null is passed as the new localization definition, treat that
        // as a no-op.
        if (null != newLocalizationDefinition)
        {
            boolean isDestLimitsChanged = false;
            boolean isSendAllowedChanged = false;
            boolean isAlterationTimeChanged = false;
            boolean isNameChanged = false;

            // If the previous definition we had was null, then every attribute is
            // changing, as it's being set for the 1st time.
            if (null == _lastLocalizationDefinitionSetByAdmin)
            {
                isDestLimitsChanged = true;
                isSendAllowedChanged = true;
                isAlterationTimeChanged = true;
                isNameChanged = true;
            }
            else
            {
                // Check to see if the max messages property has changed.
                if ((_lastLocalizationDefinitionSetByAdmin.getDestinationHighMsgs() !=
                    newLocalizationDefinition.getDestinationHighMsgs())
                    || (_lastLocalizationDefinitionSetByAdmin.getDestinationLowMsgs() !=
                    newLocalizationDefinition.getDestinationLowMsgs()))
                {
                    isDestLimitsChanged = true;
                }

                // Check to see if put inhibited changed or not.
                if (_lastLocalizationDefinitionSetByAdmin.isSendAllowed() != newLocalizationDefinition.isSendAllowed())
                {
                    isSendAllowedChanged = true;
                }

                // Check to see if the alteration time has changed.
                if (_lastLocalizationDefinitionSetByAdmin.getAlterationTime() != newLocalizationDefinition.getAlterationTime())
                {
                    isAlterationTimeChanged = true;
                }

                // Check to see if the name has changed or not.
                String oldName = _lastLocalizationDefinitionSetByAdmin.getName();
                String newName = newLocalizationDefinition.getName();
                if ((null == newName && oldName != null)
                    || (null != newName && oldName == null))
                {
                    isNameChanged = true;
                }
            }

            // Take a snap-shot so we can compare the settings we've got with the
            // ones admin gives us next time.
            this._lastLocalizationDefinitionSetByAdmin =
                            newLocalizationDefinition;

            if (isDestLimitsChanged)
            {
                setDestLimits(newLocalizationDefinition.getDestinationHighMsgs(),
                              newLocalizationDefinition.getDestinationLowMsgs());
            }
            if (isSendAllowedChanged)
            {
                setSendAllowed(newLocalizationDefinition.isSendAllowed());
            }
            if (isAlterationTimeChanged)
            {
                setAlterationTime(newLocalizationDefinition.getAlterationTime());
            }
            if (isNameChanged)
            {
                setName(newLocalizationDefinition.getName());
            }

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "updateLocalizationDefinition");
    }

    /**
     * This method checks to see if rollback is required and
     * throws an SIException if rollback failed.
     * 
     * @param transaction The transaction to rollback.
     */
    void handleRollback(LocalTransaction transaction)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "handleRollback", transaction);

        // Roll back the transaction if we created it.
        if (transaction != null)
        {
            try
            {
                transaction.rollback();
            } catch (SIException e)
            {
                // FFDC
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPLocalMsgsItemStream.handleRollback",
                                            "1:695:1.85",
                                            this);

                SibTr.exception(tc, e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "handleRollback");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#createControlAdapter()
     */
    @Override
    public void createControlAdapter()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createControlAdapter");
        controlAdapter = new LocalQueuePoint(destinationHandler.getMessageProcessor(), this);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createControlAdapter");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#deregisterControlAdapterMBean()
     */
    @Override
    public void deregisterControlAdapterMBean()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "deregisterControlAdapterMBean");
        getControlAdapter().deregisterControlAdapterMBean();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deregisterControlAdapterMBean");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#registerControlAdapterAsMBean()
     */
    @Override
    public void registerControlAdapterAsMBean()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "registerControlAdapterAsMBean");
        getControlAdapter().registerControlAdapterAsMBean();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "registerControlAdapterAsMBean");
    }

    /**
     * <p> As well as supporting q depth threshold event notification,
     * this method checks that WLM advertisement is consistent with Q depth
     * 
     * @return true if destination is advertised on WLM
     */
    @Override
    public void notifyClients()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "notifyClients");

        long totalItems = getTotalMsgCount();

        synchronized (this)
        {
            if (wlmRemoved && ((_destLowMsgs == -1) || (totalItems <= _destLowMsgs)))
            {
                wlmRemoved = false;

                // re-advertise destination PUT on WLM
                if (!destinationHandler.isTemporary())
                {
                    updatePutRegistration(true);
                    destinationHandler.requestReallocation();
                }
                // Fire event if event notification is enabled
                // Also check for logging of events (510343)
                if (_isEventNotificationEnabled ||
                    (mp.getCustomProperties().getOutputLinkThresholdEventsToLog() && destinationHandler.isLink()) ||
                    (mp.getCustomProperties().getOutputDestinationThresholdEventsToLog() && !destinationHandler.isLink()))
                {
                    fireDepthThresholdReachedEvent(getControlAdapter(),
                                                   false,
                                                   totalItems, _destLowMsgs); // Reached low
                }
            }
            else if (!wlmRemoved
                     && (_destHighMsgs != -1)
                     && (_destLowMsgs != -1)
                     && (totalItems >= _destHighMsgs))
            {
                wlmRemoved = true;

                // remove destination PUT advertisement on WLM
                if (!destinationHandler.isTemporary())
                {
                    updatePutRegistration(false);
                    wlmRemoved = true;
                }
                // Fire event if event notification is enabled
                // Also check for logging of events (510343)
                if (_isEventNotificationEnabled ||
                    (mp.getCustomProperties().getOutputLinkThresholdEventsToLog() && destinationHandler.isLink()) ||
                    (mp.getCustomProperties().getOutputDestinationThresholdEventsToLog() && !destinationHandler.isLink()))
                {
                    fireDepthThresholdReachedEvent(getControlAdapter(),
                                                   true,
                                                   totalItems, _destHighMsgs); // Reached High
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "notifyClients", new Boolean(!wlmRemoved));

    }

    /**
     * Sets the name currently in use by this localization.
     * 
     * @param newIdentifier
     */
    private void setName(String newName)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setName", newName);

        this._name = newName;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setName");
    }

    /**
     * Gets the name currently in use by this localization.
     * 
     * @return
     */
    public String getName()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getName");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getName", _name);
        return _name;
    }

    /*
     * Low level override is no longer required, see BaseMessageItemStream
     * for the implementation of these methods:
     * 
     * public void setDefaultDestLimits()
     * protected void setDestLimits(long newDestHighMsgs,
     * long newDestLowMsgs)
     * 
     * (510343)
     */

    /**
     * Decide if we need MsgStore to tell us when depth thresholds have been met
     * (510343)
     * 
     * @return boolean
     */
    @Override
    protected boolean isThresholdNotificationRequired()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "isThresholdNotificationRequired");

        boolean result = false;

        if (!destinationHandler.isTemporary() ||
            _isEventNotificationEnabled ||
            (mp.getCustomProperties().getOutputLinkThresholdEventsToLog() && destinationHandler.isLink()) ||
            (mp.getCustomProperties().getOutputDestinationThresholdEventsToLog() && !destinationHandler.isLink()))
            result = true;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "isThresholdNotificationRequired", new Boolean(result));

        return result;
    }

    /**
     * Gets the current value of SendAllowed used by this localization.
     * 
     * @return
     */
    @Override
    public boolean isSendAllowed()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "isSendAllowed");

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "isSendAllowed", new Boolean(_isSendAllowed));

        return _isSendAllowed;
    }

    /**
     * Sets the current value of SendAllowed used by this localization.
     * 
     * @param newIsSendAllowedValue
     */
    public void setSendAllowed(boolean newIsSendAllowedValue)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setSendAllowed", new Boolean(newIsSendAllowedValue));

        this._isSendAllowed = newIsSendAllowedValue;
        updatePutRegistration(_isSendAllowed);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setSendAllowed");
    }

    /**
     * Gets the current alteration time for this localization.
     * 
     * @return
     */
    public long getAlterationTime()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getAlterationTime");

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getAlterationTime", new Long(_alterationTime));

        return _alterationTime;

    }

    /**
     * Sets the current alteration time fro this localization.
     * 
     * @param newAlterationTime
     */
    public void setAlterationTime(long newAlterationTime)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setAlterationTime", new Long(newAlterationTime));

        this._alterationTime = newAlterationTime;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setAlterationTime");
    }

    /**
     * registers/deregisters the GET capability to TRM
     * 
     * @param advertise True if GET capability should be advertised
     */
    void updateGetRegistration(boolean advertise)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "updateGetRegistration", new Boolean(advertise));

        destinationHandler.updateGetRegistration(advertise);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "updateGetRegistration");
    }

    /**
     * registers/deregisters the PUT capability to TRM
     * 
     * @param advertise True if PUT capability should be advertised
     */
    void updatePutRegistration(boolean advertise)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "updatePutRegistration", new Boolean(advertise));

        destinationHandler.updatePostRegistration(advertise);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "updatePutRegistration");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.LocalizationPoint#getConsumerManager()
     */
    @Override
    public ConsumerManager getConsumerManager()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getConsumerManager");

        ConsumerManager manager = (ConsumerManager) getOutputHandler();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getConsumerManager", manager);
        return manager;
    }

    /*
     * (non-Javadoc)
     */
    @Override
    public ConsumerManager createConsumerManager()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createConsumerManager");

        ConsumerDispatcher consumerDispatcher = (ConsumerDispatcher) getOutputHandler();
        if (consumerDispatcher == null)
        {
            consumerDispatcher = new ConsumerDispatcher(destinationHandler, this, new ConsumerDispatcherState());
            setOutputHandler(consumerDispatcher);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createConsumerManager", consumerDispatcher);
        return consumerDispatcher;
    }

    /*
     * (non-Javadoc)
     */
    @Override
    public void dereferenceConsumerManager()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "dereferenceConsumerManager");
        setOutputHandler(null);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "dereferenceConsumerManager");
    }

}
