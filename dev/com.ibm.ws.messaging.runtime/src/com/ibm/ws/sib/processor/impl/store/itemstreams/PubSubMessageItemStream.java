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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.LocalizationDefinition;
import com.ibm.ws.sib.msgstore.DeliveryDelayDeleteFilter;
import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.NotInMessageStore;
import com.ibm.ws.sib.msgstore.OutOfCacheSpace;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.Statistics;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.store.filters.ClassEqualsFilter;
import com.ibm.ws.sib.processor.impl.store.items.MessageItem;
import com.ibm.ws.sib.processor.runtime.impl.LocalTopicSpaceControl;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;

/**
 * @author caseyj
 * 
 *         A PubSubMessageItemStream is used by the DestinationHandler for storing messages
 *         in the Message Store.
 */
public final class PubSubMessageItemStream extends BaseMessageItemStream
{
    /**
     * Trace.
     */
    private static final TraceComponent tc =
                    SibTr.register(
                                   PubSubMessageItemStream.class,
                                   SIMPConstants.MP_TRACE_GROUP,
                                   SIMPConstants.RESOURCE_BUNDLE);

    /**
     * NLS for component
     */
    static final TraceNLS nls =
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

    /**
     * The currently used value of SendAllowed
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

    /**
     * This will be set in the setDestHighMsgs to be slightly higher
     * than the destHigh limit for the topicspace.
     * This is so that we give slightly more space to remotely produced messages
     * as opposed to locally produced messages.
     * See defect 281311.
     */
    private long _remoteQueueHighLimit;

    /**
     * 486100
     * The qHigh limit could be reached because the totalItemCount on the statistics
     * includes the refStreams as well as the messages on this itemstream. We only
     * want to count the msgItems so we keep a record of the refStreams to take this
     * off the totalItemCount.
     */
    private long referenceStreamCount;

    /**
     * This flag is to used to know when to gracefully exit from deleteMsgsWithNoReferences()
     * This flag is set to true when ME is stopped and when the destination is deleted
     */
    private volatile boolean HasToStop = false;

    /**
     * <p>deleteMessageslock is used to syncronize deleteMsgsWithNoReferences()
     * and removeAllItemsWithNoRefCount().</p>
     * deleteMsgsWithNoReferences() is called from DeletePubSubMsgsThread context and
     * removeAllItemsWithNoRefCount() is called from AsynchDeletionThread.
     * This lock should be used by only these two methods
     */
    private final ReentrantLock deleteMessageslock = new ReentrantLock();

    /**
     * Warm start constructor invoked by the Message Store.
     * 
     * @throws MessageStoreException
     */
    public PubSubMessageItemStream()
    {
        super();
    }

    /**
     * Cold start constructor. The constructed object will be added to the
     * DestinationHandler ItemStream.
     * 
     * @param destinationHandler The DestinationHandler this PubSubMessageItemStream
     *            stores messages for.
     * @param transaction Transaction to use to add this object to the
     *            Message Store. Cannot be null.
     * 
     * @throws MessageStoreException
     */
    public PubSubMessageItemStream(
                                   BaseDestinationHandler destinationHandler,
                                   Transaction transaction)
        throws OutOfCacheSpace, MessageStoreException
    {
        super();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "PubSubMessageItemStream",
                        new Object[] { destinationHandler, transaction });

        // The PubSubMessageItemStream has to have the same storage strategy as its
        // parent destination.  Two reasons:
        //
        // 1.  The message store will not allow the ItemStream to
        // be stored if it has a more permanent storage strategy.
        // 2.  If the DestinationHandler is not persistently stored (e.g. if it is
        // a temporary destination) then this stream should also not be persistent.
        setStorageStrategy(destinationHandler.getStorageStrategy());

        destinationHandler.addItemStream(this, transaction);

        initializeNonPersistent(destinationHandler);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "PubSubMessageItemStream", this);
    }

    @Override
    public void removeItemStream(Transaction transaction, long lockID) throws MessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "removeItemStream", new Object[] { transaction, Long.valueOf(lockID) });

        deregisterControlAdapterMBean();
        remove(transaction, lockID);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "removeItemStream");
    }

    /**
     * Complete recovery of a PubSubMessageItemStream retrieved from the MessageStore.
     * Deleting the messages with no references is pushed after start of ME (to the
     * function deleteMsgsWithNoReferences)
     * 
     * @param destinationHandler to use in reconstitution.
     */
    public void reconstitute(BaseDestinationHandler destinationHandler)
                    throws SIRollbackException,
                    SIConnectionLostException,
                    SIIncorrectCallException,
                    SIResourceException,
                    SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "reconstitute", destinationHandler);

        initializeNonPersistent(destinationHandler);
        // If message depth intervals are configured, set the MsgStore watermarks
        // accordinly (510343)
        setDestMsgInterval();
        try
        {
            // F001338-55330
            // getStatistics() inturn will load all the metadata related to the
            // destination.With the introduction of the feature F001338-55330 this
            // will benefit the ME start up time
            Statistics statistics = getStatistics();
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "reconstitute - counts total items, available items",
                           new Object[] { Long.valueOf(statistics.getTotalItemCount()),
                                         Long.valueOf(statistics.getAvailableItemCount()) });
        } catch (MessageStoreException e)
        {
            // No FFDC code needed
            SibTr.exit(tc, "reconstitute", e);
        }

    }

    /**
     * Gives signal to deleteMsgsWithNoReferences() to gracefully exit. This happens in two cases
     * (i) When ME is stopped and (ii) When the destination is deleted
     * 
     * @param hasToStop
     */
    public void stopDeletingMsgsWihoutReferencesTask(boolean hasToStop) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "stopDeletingMsgsWihoutReferencesTask", hasToStop);

        this.HasToStop = hasToStop;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "stopDeletingMsgsWihoutReferencesTask", hasToStop);

    }

    private boolean HasToStop() {
        return HasToStop;
    }

    /**
     * <p>This method deletes the messages with no references. Previously these messages
     * were deleted during ME startup in reconstitute method.This method is called from
     * DeletePubSubMsgsThread context </p>
     * <p> This function gracefully exits in case if ME is stopped or corresponding destination
     * is deleted.</p>
     */
    public void deleteMsgsWithNoReferences()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "deleteMsgsWithNoReferences");

        NonLockingCursor cursor = null;
        try {
            if (deleteMessageslock.tryLock(0, TimeUnit.SECONDS)) {
                //trying with tryLock as incase if there is lock contention, then
                //removeAllItemsWithNoRefCount() might have acquired lock. In that case we gracefully
                // exit as the work is done by removeAllItemsWithNoRefCount()
                LocalTransaction transaction = destinationHandler.getTxManager().createLocalTransaction(true);

                // Remove any available messages that have no references
                cursor = newNonLockingItemCursor(
                                new ClassEqualsFilter(MessageItem.class));

                MessageItem messageItem = (MessageItem) cursor.next();

                while ((messageItem != null) && !HasToStop()) {
                    if (messageItem.getReferenceCount() == 0) {
                        try {
                            //The message is no longer required and can be deleted
                            messageItem.remove((Transaction) transaction, NO_LOCK_ID);
                        } catch (NotInMessageStore e) {
                            // No FFDC code needed
                            SibTr.exception(tc, e);
                            // It is possible that this item has just been removed, log and continue
                        }
                    }
                    messageItem = (MessageItem) cursor.next();
                }
                transaction.commit();
            }
        } catch (InterruptedException e) {
            // No FFDC code needed
            // code flow may never enter here as nobody would interrupt this thread
            // (may be in case if ME stopped in FORCE mode)
            SibTr.exception(tc, e);
        } catch (MessageStoreException e) {
            // MessageStoreException shouldn't occur so FFDC.
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.store.itemstreams.PubSubMessageItemStream.deleteMsgsWithNoReferences",
                                        "1:244:1.71",
                                        this);

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "deleteMsgsWithNoReferences", e);

        } catch (SIException e) {
            //logging FFDC here itself and not propagating exception to the callers
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.store.itemstreams.PubSubMessageItemStream.deleteMsgsWithNoReferences",
                                        "1:244:1.72",
                                        this);

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "deleteMsgsWithNoReferences", e);
        }

        finally
        {
            if (cursor != null)
                cursor.finished();

            if (deleteMessageslock.getHoldCount() > 0)
                deleteMessageslock.unlock(); //only unlock if it is acquired by this thread
        }
    }

    /**
     * See defect 281311
     * Behaves like the isQHighLimit method call
     * but allows an extra chunk of space for messages that have
     * come in remotely. This is to avoid a deadlock situation.
     * 
     * @return true if the destination is higher than the
     *         remoteQueueHighLimit for this topic space
     */
    public boolean isRemoteQueueHighLimit()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "isRemoteQueueHighLimit");

        boolean limited = false;

        if (_destHighMsgs != -1)
        {
            limited = (getTotalMsgCount() >= _remoteQueueHighLimit);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "isRemoteQueueHighLimit", Boolean.valueOf(limited));

        return limited;
    }

    /**
     * @return boolean true if this itemstream is below
     *         the destination's remote Low limit. This limit is halfway between the
     *         destination highLimit and the remoteHighLimit. It MUST be no less
     *         than the dest high limit otherwise we'll deadlock (see defect 281311)
     */
    public boolean isQLowRemoteLimit()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "isQLowRemoteLimit");

        long remoteLowLimit = _destHighMsgs;
        boolean isUnderDestinationLowLimit = false;

        // Find the halfway mark between the two high limits
        if (_remoteQueueHighLimit > _destHighMsgs)
        {
            remoteLowLimit = _remoteQueueHighLimit - ((_remoteQueueHighLimit - _destHighMsgs) / 2);
        }

        // Find out how deep the itemStream is
        isUnderDestinationLowLimit = (getTotalMsgCount() <= remoteLowLimit);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Queue depth=" + getTotalMsgCount() + ", RemoteLowLimit=" + remoteLowLimit);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "isQLowRemoteLimit", Boolean.valueOf(isUnderDestinationLowLimit));

        return isUnderDestinationLowLimit;
    }

    /**
     * Return this PubSubMessageItemStreams's DestinationHandler.
     * 
     * @return DestinationHandler
     */
    @Override
    public BaseDestinationHandler getDestinationHandler()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getDestinationHandler");
            SibTr.exit(tc, "getDestinationHandler", destinationHandler);
        }

        return destinationHandler;
    }

    /**
     * Count the available publications.
     * 
     * @return Number of available publications.
     */
    public long countAvailablePublications()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "countAvailablePublications");

        int subs = destinationHandler.getSubscriptionIndex().getTotalSubscriptions();

        // Need to compensate for subscription reference streams and the proxy
        // reference stream.
        long pubs = -1;

        try
        {
            pubs = getStatistics().getAvailableItemCount() - subs - 1;
        } catch (MessageStoreException e)
        {
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.store.itemstreams.PubSubMessageItemStream.countAvailablePublications",
                                        "1:465:1.74",
                                        this);

            SibTr.exception(tc, e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "countAvailablePublications", Long.valueOf(pubs));

        return pubs;
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
     * @param localizationDefinition
     */
    public void updateLocalizationDefinition(LocalizationDefinition newLocalizationDefinition)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "updateLocalizationDefinition", newLocalizationDefinition);

        // If null is passed as the new localization definition, treat that
        // as a no-op.
        if (null != newLocalizationDefinition)
        {
            boolean isDestHighMsgsChanged = false;
            boolean isDestLowMsgsChanged = false;
            boolean isSendAllowedChanged = false;
            boolean isAlterationTimeChanged = false;
            boolean isNameChanged = false;

            // If the previous definition we had was null, then every attribute is
            // changing, as it's being set for the 1st time.
            if (null == _lastLocalizationDefinitionSetByAdmin)
            {
                isDestHighMsgsChanged = true;
                isDestLowMsgsChanged = true;
                isSendAllowedChanged = true;
                isAlterationTimeChanged = true;
                isNameChanged = true;
            }
            else
            {
                // Check to see if the max messages property has changed.
                if (_lastLocalizationDefinitionSetByAdmin.getDestinationHighMsgs() != newLocalizationDefinition.getDestinationHighMsgs())
                {
                    isDestHighMsgsChanged = true;
                }

                if (_lastLocalizationDefinitionSetByAdmin.getDestinationLowMsgs() != newLocalizationDefinition.getDestinationLowMsgs())
                {
                    isDestLowMsgsChanged = true;
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

                // Check to see if the identifier has changed or not.
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

            if (isDestHighMsgsChanged)
            {
                setDestHighMsgs(newLocalizationDefinition.getDestinationHighMsgs());
            }
            //NOTE: the initialisation of destLowMessages should always
            //occur after the initialisation of destHighMessages
            if (isDestLowMsgsChanged)
            {
                setDestLowMsgs(newLocalizationDefinition.getDestinationLowMsgs());
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

        return;
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

        controlAdapter =
                        new LocalTopicSpaceControl(destinationHandler.getMessageProcessor(), this);

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
        LocalTopicSpaceControl control = (LocalTopicSpaceControl) getControlAdapter();
        control.deregisterControlAdapterMBean();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deregisterControlAdapterMBean");
    }

    @Override
    public void setDestHighMsgs(long newDestHighMsgs)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setDestHighMsgs", Long.valueOf(newDestHighMsgs));

        //set the generic dest high limit
        super.setDestHighMsgs(newDestHighMsgs);

        //now we set the _remoteQueueHighLimit, which is slightly higher than
        //the dest high msgs
        //See defect 281311
        long increase = (long) (_destHighMsgs * destinationHandler.getMessageProcessor().getCustomProperties().get_remote_queue_high_percentage_excess());
        if (increase == 0) {
            increase++; //should have at least a slight increase
        }
        long newRemoteQueueHigh = _destHighMsgs + increase;
        if (newRemoteQueueHigh < _destHighMsgs)
        {
            //the new dest high limit might be too big and now we've overflowed
            //We can't really do any better than setting the remote queue high to be
            //max long and output a warning
            _remoteQueueHighLimit = Long.MAX_VALUE;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                SibTr.debug(tc, "remote Queue High Limit might be too small to prevent deadlock");
            }
        }
        else
        {
            //this is ok
            _remoteQueueHighLimit = newRemoteQueueHigh;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            SibTr.debug(tc, "remoteQueueHighLimit=" + _remoteQueueHighLimit);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setDestHighMsgs");
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
        LocalTopicSpaceControl control = (LocalTopicSpaceControl) getControlAdapter();
        control.registerControlAdapterAsMBean();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "registerControlAdapterAsMBean");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#dereferenceControlAdapter()
     */
    @Override
    public void dereferenceControlAdapter()
    {
        controlAdapter.dereferenceControllable();
        controlAdapter = null;
    }

    /**
     * Sets the name currently in use by this localization.
     * 
     * @param newName
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
     * for the implementation of this method:
     * 
     * protected void setDestLimits(long newDestHighMsgs,
     * long newDestLowMsgs)
     * 
     * (510343)
     */

    /**
     * Gets the current value of SendAllowed used by this localization.
     * 
     * @return
     */
    public boolean isSendAllowed()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "isSendAllowed");

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "isSendAllowed", Boolean.valueOf(_isSendAllowed));

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
            SibTr.entry(tc, "setSendAllowed", Boolean.valueOf(newIsSendAllowedValue));

        this._isSendAllowed = newIsSendAllowedValue;

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
            SibTr.exit(tc, "getAlterationTime", Long.valueOf(_alterationTime));

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
            SibTr.entry(tc, "setAlterationTime", Long.valueOf(newAlterationTime));

        this._alterationTime = newAlterationTime;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setAlterationTime");
    }

    /**
     * Remove all the message items
     * 
     * @throws SIResourceException
     * 
     */
    public boolean removeAllItemsWithNoRefCount(Transaction tran) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "removeAllItemsWithNoRefCount", tran);

        Item item = null;

        boolean deletedItems = false;

        try
        {
            deleteMessageslock.lockInterruptibly();
            /*
             * Loop through all the items and check if they have any reference count.
             * Introduce a new DeliveryDelayDeleteFilter which basically retrieves
             * the messages which are either available or which are locked
             * for delivery delay It is just a marker class.Earlier to
             * delivery delay feature null was being passed
             */
            while (null != (item = findFirstMatchingItem(new DeliveryDelayDeleteFilter())))
            {
                try
                {
                    if (item.getReferenceCount() == 0)
                    {
                        item.remove(tran, NO_LOCK_ID);

                        deletedItems = true;
                    }
                } catch (NotInMessageStore e)
                {
                    // No FFDC code needed
                    SibTr.exception(tc, e);

                    // It is possible that this item has just been removed, log and continue
                }
            }
        } catch (InterruptedException e) {
            // No FFDC code needed
            // code flow may never enter here as nobody would interrupt this thread.
            // aborb interrrput and trace it.
            SibTr.exception(tc, e);

        } catch (MessageStoreException e)
        {
            // // FFDC
            FFDCFilter
                            .processException(
                                              e,
                                              "com.ibm.ws.sib.processor.impl.store.itemstreams.PubSubMessageItemStream.removeAllItemsWithNoRefCount",
                                              "1:840:1.74", this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "removeAllItemsWithNoRefCount", "SIResourceException");
            throw new SIResourceException(e);
        }

        finally {
            deleteMessageslock.unlock();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "removeAllItemsWithNoRefCount", Boolean.valueOf(deletedItems));
        return deletedItems;
    }

    @Override
    public long getTotalMsgCount()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getTotalMsgCount");

        long count = -1;
        try
        {
            count = getStatistics().getTotalItemCount() - getSubscriberCount();
        } catch (MessageStoreException e)
        {
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.store.itemstreams.PubSubMessageItemStream.getTotalMsgCount",
                                        "1:872:1.74",
                                        this);

            SibTr.exception(tc, e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getTotalMsgCount", count);
        return count;
    }

    private synchronized long getSubscriberCount()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getSubscriberCount");
            SibTr.exit(tc, "getSubscriberCount", Long.valueOf(referenceStreamCount));
        }
        return referenceStreamCount;
    }

    public synchronized void decrementReferenceStreamCount()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "decrementReferenceStreamCount");

        referenceStreamCount--;
        // Msgstore notifies us when the high/low watermark (destThreshold) is breached so we update
        // the high and low watermarks here

        // Allow the BaseMessageItemStream to calculate the necessary watermarks needed (510343)
        updateWatermarks(getTotalMsgCount());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "decrementReferenceStreamCount", Long.valueOf(referenceStreamCount));
    }

    public synchronized void incrementReferenceStreamCount()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "incrementReferenceStreamCount");

        referenceStreamCount++;
        // Msgstore notifies us when the high/low watermark (destThreshold) is breached so we update
        // the high and low watermarks here

        // Allow the BaseMessageItemStream to calculate the necessary watermarks needed (510343)
        updateWatermarks(getTotalMsgCount());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "incrementReferenceStreamCount", Long.valueOf(referenceStreamCount));
    }

    /**
     * Include the referenceStreamCount when setting the watermarks (called by
     * BaseMessageItemStream.updateWaterMarks() (which has no concept of referenceStreams)
     * 
     * (510343)
     * 
     * @param nextLowWatermark
     * @param nextHighWatermark
     * @throws SevereMessageStoreException
     */
    @Override
    protected void setWatermarks(long nextLowWatermark, long nextHighWatermark)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setWatermarks",
                        new Object[] { new Long(nextLowWatermark), new Long(nextHighWatermark), new Long(referenceStreamCount) });

        super.setWatermarks(nextLowWatermark + referenceStreamCount, nextHighWatermark + referenceStreamCount);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setWatermarks");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.ItemStream#getCountHighWaterMark()
     */
    @Override
    public long getCountHighWaterMark() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getCountHighWaterMark");

        long destHigh = _destHighMsgs + getSubscriberCount();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getCountHighWaterMark", Long.valueOf(destHigh));
        return destHigh;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.ItemStream#getCountLowWaterMark()
     */
    @Override
    public long getCountLowWaterMark() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getCountLowWaterMark");

        long destLow = _destLowMsgs + getSubscriberCount();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getCountLowWaterMark", Long.valueOf(destLow));
        return destLow;
    }

}
