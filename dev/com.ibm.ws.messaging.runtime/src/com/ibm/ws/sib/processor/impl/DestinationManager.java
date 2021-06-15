/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.processor.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.SIDestinationAddressFactory;
import com.ibm.websphere.sib.SIRCConstants;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.exception.WsRuntimeException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.BaseDestinationDefinition;
import com.ibm.ws.sib.admin.ControllableRegistrationService;
import com.ibm.ws.sib.admin.DestinationAliasDefinition;
import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.ws.sib.admin.DestinationForeignDefinition;
import com.ibm.ws.sib.admin.ForeignBusDefinition;
import com.ibm.ws.sib.admin.JsBus;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.admin.LocalizationDefinition;
import com.ibm.ws.sib.admin.MQLinkDefinition;
import com.ibm.ws.sib.admin.SIBExceptionBase;
import com.ibm.ws.sib.admin.SIBExceptionBusNotFound;
import com.ibm.ws.sib.admin.SIBExceptionDestinationNotFound;
import com.ibm.ws.sib.admin.SIBExceptionNoLinkExists;
import com.ibm.ws.sib.admin.VirtualLinkDefinition;
import com.ibm.ws.sib.comms.mq.MQLinkManager;
import com.ibm.ws.sib.comms.mq.MQLinkObject;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.OutOfCacheSpace;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.transactions.ExternalAutoCommitTransaction;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.MQLinkLocalization;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPDestinationAlreadyExistsException;
import com.ibm.ws.sib.processor.exceptions.SIMPDestinationCorruptException;
import com.ibm.ws.sib.processor.exceptions.SIMPErrorException;
import com.ibm.ws.sib.processor.exceptions.SIMPMQLinkCorruptException;
import com.ibm.ws.sib.processor.exceptions.SIMPNotPossibleInCurrentConfigurationException;
import com.ibm.ws.sib.processor.exceptions.SIMPNullParameterException;
import com.ibm.ws.sib.processor.exceptions.SIMPResourceException;
import com.ibm.ws.sib.processor.exceptions.SIMPTemporaryDestinationNotFoundException;
import com.ibm.ws.sib.processor.impl.exceptions.InvalidOperationException;
import com.ibm.ws.sib.processor.impl.indexes.DestinationIndex;
import com.ibm.ws.sib.processor.impl.indexes.DestinationTypeFilter;
import com.ibm.ws.sib.processor.impl.indexes.ForeignBusIndex;
import com.ibm.ws.sib.processor.impl.indexes.ForeignBusTypeFilter;
import com.ibm.ws.sib.processor.impl.indexes.LinkIndex;
import com.ibm.ws.sib.processor.impl.indexes.LinkTypeFilter;
import com.ibm.ws.sib.processor.impl.indexes.statemodel.State;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.LocalizationPoint;
import com.ibm.ws.sib.processor.impl.interfaces.StoppableThread;
import com.ibm.ws.sib.processor.impl.store.AsyncUpdateThread;
import com.ibm.ws.sib.processor.impl.store.MessageProcessorStore;
import com.ibm.ws.sib.processor.impl.store.SIMPTransactionManager;
import com.ibm.ws.sib.processor.impl.store.filters.ClassEqualsFilter;
import com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.SubscriptionItemStream;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.utils.AliasChainValidator;
import com.ibm.ws.sib.processor.utils.LockManager;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.processor.utils.StoppableThreadCache;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.trm.links.LinkException;
import com.ibm.ws.sib.trm.links.LinkManager;
import com.ibm.ws.sib.trm.links.LinkSelection;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.DestinationAvailability;
import com.ibm.wsspi.sib.core.DestinationListener;
import com.ibm.wsspi.sib.core.DestinationType;
import com.ibm.wsspi.sib.core.Distribution;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIDestinationLockedException;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SIInvalidDestinationPrefixException;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException;

import com.ibm.ws.kernel.service.util.CpuInfo;

/**
 * @author millwood
 *
 *         <p>The Destination Manager class is responsible for the creation,
 *         updating and deletion of destination objects. It maintains a hash
 *         table of destinations and can be queried to locate a destination
 *         of a particular name.</p>
 */
public final class DestinationManager extends SIMPItemStream
{
    /**
     * Initialise trace for the component.
     */
    private static final TraceComponent tc =
                    SibTr.register(DestinationManager.class, SIMPConstants.MP_TRACE_GROUP, SIMPConstants.RESOURCE_BUNDLE);
    private static final TraceComponent tc_cwsik =
                    SibTr.register((new Object() {}).getClass(), SIMPConstants.MP_TRACE_GROUP, SIMPConstants.CWSIK_RESOURCE_BUNDLE);

    /**
     * NLS for component.
     */
    private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
    private static final TraceNLS nls_cwsik =
                    TraceNLS.getTraceNLS(SIMPConstants.CWSIK_RESOURCE_BUNDLE);

    private SIMPTransactionManager txManager;

    /**
     * The destination manager maintains a reference to the messageProcessor to
     * provide access to required functions such as creating local UOWs
     */
    private MessageProcessor messageProcessor;

    /*
     * Destination indexes.
     */
    private DestinationIndex destinationIndex;
    private ForeignBusIndex foreignBusIndex;
    private LinkIndex linkIndex;

    /**
     * The destination manager maintainns an ME-wide hashmap for durable
     * subscriptions. This MUST be available to every TopicSpace since a
     * subsriptionId is unique across the system.
     */
    private HashMap durableSubscriptions;

    /**
     * The destination manager maintains an ME-wide hashmap for nondurable
     * shared subscriptions. This MUST be available to every TopicSpace since a
     * subsriptionId is unique across the system for all non-durable shared subscribers.
     */
    private ConcurrentHashMap<String, Object> nondurableSharedSubscriptions;

    private boolean reconciling = false;

    protected Object deletionThreadLock = new Object();
    /** Maintain a single async deletion thread for the ME */
    private AsynchDeletionThread asynchDeletionThread = null;

    /**
     * Used for asynchronous updates to the MS by the Remote Get protocol.
     * Eventually, should consolidate AsynchDeletionThread and AsyncUpdateThread, and
     * make this a threadpool rather than a single thread.
     */
    private AsyncUpdateThread asyncUpdateThread = null;

    /**
     * Used for asynchronously writing locks and value ticks to the MS by the Remote Get protocol
     * at the DME
     */
    private AsyncUpdateThread persistLockThread = null;

    //collection of destination listeners
    private final List<DestinationListenerDataObject> destinationListeners = new LinkedList<DestinationListenerDataObject>();

    private final List<SubscriptionItemStream> deletableSubscriptions = new ArrayList<SubscriptionItemStream>();

    /**
     * Reference to the MQLinkManager
     *
     */
    private MQLinkManager _mqlinkManager = null;

    /**
     * ThreadpoolExecutor for reconstitution.Same thread pool will be used for
     * reconstuting BaseDestinationHandler,LinkHandler,MQLinkHandler
     */
    private ThreadPoolExecutor _reconstituteThreadpool = null;

    /**
     * Warm start constructor invoked by the Message Store.
     *
     * @throws MessageStoreException
     */
    public DestinationManager()
    {
        super();
        // This space intentionally blank
    }

    /**
     * Cold start constructor. There should be only one destination
     * manager per ME.
     *
     * @param messageProcessor A reference to the owning MessageProcessor.
     * @param parentItemStream The ItemStream to add this object to.
     */
    protected DestinationManager(MessageProcessor messageProcessor, MessageProcessorStore parentItemStream)
    {
        super();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "DestinationManager", new Object[] { messageProcessor, parentItemStream });

        LocalTransaction transaction = messageProcessor.getTXManager().createLocalTransaction(true);

        try
        {
            parentItemStream.addItemStream(this, (Transaction) transaction);

            // 174199.2.4
            initializeNonPersistent(messageProcessor);

            transaction.commit();
        } catch (MessageStoreException e)
        {
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.DestinationManager.DestinationManager",
                                        "1:336:1.508.1.7",
                                        this);

            SibTr.exception(tc, e);

            handleRollback(transaction);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "DestinationManager", "SIErrorException");

            throw new SIErrorException(e);
        } catch (SIException e)
        {
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.DestinationManager.DestinationManager",
                                        "1:354:1.508.1.7",
                                        this);

            SibTr.exception(tc, e);

            handleRollback(transaction);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "DestinationManager", "SIErrorException");

            throw new SIErrorException(e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "DestinationManager", this);
    }

    /**
     * This method checks to see if rollback is required.
     *
     * @param transaction The transaction to rollback.
     */
    private void handleRollback(LocalTransaction transaction)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "handleRollback", transaction);

        // Roll back the transaction if we created it.
        if (transaction != null)
        {
            try
            {
                transaction.rollback();
            } catch (Throwable e)
            {
                // FFDC
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.DestinationManager.handleRollback",
                                            "1:394:1.508.1.7",
                                            this);

                SibTr.exception(tc, e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "handleRollback");
    }

    /**
     * Initialize non-persistent fields. These fields are common to both MS
     * reconstitution of DestinationManagers and initial creation.
     *
     * Feature 174199.2.4
     *
     * @param MessageProcessor
     */
    protected void initializeNonPersistent(MessageProcessor messageProcessor)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "initializeNonPersistent", messageProcessor);

        /*
         * Keep a local reference to the MessageProcessor object, as this allows us
         * to find the message store and to generate units of work when required.
         */
        this.messageProcessor = messageProcessor;
        txManager = messageProcessor.getTXManager();

        destinationIndex = new DestinationIndex(messageProcessor.getMessagingEngineBus());
        foreignBusIndex = new ForeignBusIndex();
        linkIndex = new LinkIndex();

        /*
         * Create the system-wide hashmap for durable subscriptions. This MUST be available
         * to every TopicSpace since a subsriptionId is unique across the system.
         */
        durableSubscriptions = new HashMap();

        //initializing nondurableSharedSubscriptions here as it is common flow for cold and warm start
        //however nondurableSharedSubscriptions not be restored from Message Store.
        nondurableSharedSubscriptions = new ConcurrentHashMap<String, Object>();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "initializeNonPersistent");
    }

    /**
     * get method for nondurableSharedSubscriptions of DestinationManager.
     *
     * @return nondurableSharedSubscriptions
     */
    public ConcurrentHashMap<String, Object> getNondurableSharedSubscriptions() {
        //Entry and Exit traces are not enabled as this would be called many a times
        //and it is trivial.
        return nondurableSharedSubscriptions;
    }

    private void putDestinationIntoIndoubtState(SIBUuid12 destDefUUID)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "putDestinationIntoIndoubtState");
        DestinationHandler destHand =
                        destinationIndex.findByUuid(destDefUUID, null);

        destinationIndex.putInDoubt(destHand);

        destHand.setIndoubt(true);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "putDestinationIntoIndoubtState");

    }

    private void putLinkIntoIndoubtState(String linkName)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "putLinkIntoIndoubtState", linkName);

        DestinationHandler destHand = linkIndex.findByName(linkName, null);

        if (destHand != null) {
            linkIndex.putInDoubt(destHand);

            destHand.setIndoubt(true);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "putLinkIntoIndoubtState");

    }

    /**
     * Before reconciliation, we need to move any inDoubt handlers
     * to the Unreconciled state.
     * If the destination gets reconciled then we have recovered.
     * If not, we might get moved back to the inDoubt state, arguing
     * that the corrupt WCCM file is stil causing problems,
     * or finally WCCM might now tell us to remove the destination
     *
     * @author tpm
     */
    public void moveAllInDoubtToUnreconciled()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "moveAllInDoubtToUnreconciled");

        DestinationTypeFilter filter = new DestinationTypeFilter();
        filter.LOCAL = Boolean.TRUE;
        filter.INDOUBT = Boolean.TRUE;
        SIMPIterator itr = destinationIndex.iterator(filter);
        while (itr.hasNext())
        {
            BaseDestinationHandler destHand = (BaseDestinationHandler) itr.next();
            destinationIndex.putUnreconciled(destHand);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "moveAllInDoubtToUnreconciled");
    }

    /**
     * At the end of the reconciliation phase of MessageProcessor startup,
     * we need to check each unreconciled destination to see if it is safe
     * to delete that destination, whether the destination should be
     * altered to a new locality set, or whether the destination should be put
     * into a InDoubt state.
     *
     * @author tpm
     */
    public void validateUnreconciled()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "validateUnreconciled");
        JsMessagingEngine engine =
                        messageProcessor.getMessagingEngine();

        //find all local, unreconciled destinations
        DestinationTypeFilter filter = new DestinationTypeFilter();
        filter.LOCAL = Boolean.TRUE;
        filter.UNRECONCILED = Boolean.TRUE;
        SIMPIterator itr = destinationIndex.iterator(filter);
        while (itr.hasNext())
        {
            BaseDestinationHandler bdh = (BaseDestinationHandler) itr.next();
            try
            {

                BaseDestinationDefinition baseDestDef =
                                engine.getSIBDestination(engine.getBusName(),
                                                         bdh.getName());

                //if no exception was thrown then
                //the destination does exist, so we need to
                //figure out why createDestLoclisationLocal was not called
                if (baseDestDef.getUUID().equals(bdh.getUuid()))
                {
                    //Only do this if the UUID of the destination is the same
                    Set<String> localitySet =
                                    engine.getSIBDestinationLocalitySet(engine.getBusName(),
                                                                        baseDestDef.getUUID().toString());

                    boolean qLocalisation = localitySet.contains(engine.getUuid().toString());

                    //Venu temp
                    // removing the code as it is for PEV
                    // has to be deleted at later point

                    //if this destination isn't truely local then we'll check to see if
                    //it is a PEV destination, in which case we will treat it as local anyway

                    DestinationDefinition destDef = (DestinationDefinition) baseDestDef;
                    SIBUuid12 destUUID = destDef.getUUID();

                    if (qLocalisation)
                    {
                        //So we do localise, either a qPt or MedPt
                        //but yet createDestLocalisation was not called.
                        //Possibly a corrupt file in WCCM - we put the destination
                        //into the "InDoubt" state and make it invisible
                        //and then throw an exception

                        try
                        {
                            putDestinationIntoIndoubtState(destUUID);
                            SibTr.error(tc, "DESTINATION_INDOUBT_ERROR_CWSIP0062",
                                        new Object[] { destDef.getName(), destUUID });
                            throw new SIErrorException(nls.getFormattedMessage(
                                                                               "DESTINATION_INDOUBT_ERROR_CWSIP0062",
                                                                               new Object[] { destDef.getName(), destUUID },
                                                                               null));
                        } catch (SIErrorException e)
                        {
                            FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ws.sib.processor.impl.DestinationManager.validateUnreconciled",
                                                        "1:582:1.508.1.7",
                                                        this);

                            SibTr.exception(tc, e);
                        }
                    }
                    else
                    {
                        //the reason for missing the createDestLoc call is that,
                        //since we were last running,
                        //the destination has changed so that it is no longer
                        //localised on this ME.

                        try
                        {
                            deleteDestinationLocalization(bdh.getDefinition().getUUID().toString(),
                                                          destDef,
                                                          localitySet);
                        } catch (SIException exception)
                        {
                            FFDCFilter.processException(
                                                        exception,
                                                        "com.ibm.ws.sib.processor.impl.DestinationManager.validateUnreconciled",
                                                        "1:607:1.508.1.7",
                                                        this);

                            SibTr.exception(tc, exception);

                            //play it safe
                            //and put the destination into indoubt (rather than delete)
                            putDestinationIntoIndoubtState(destUUID);
                        }
                    }
                }
            } catch (SIBExceptionDestinationNotFound e)
            {
                // No FFDC code needed

                SibTr.exception(tc, e);

                //the destination does not exist
                //it is ok to delete this exception, so we do nothing
                //(all unreconciled destinations are eventually deleted)
            } catch (SIBExceptionBase base)
            {
                FFDCFilter.processException(
                                            base,
                                            "com.ibm.ws.sib.processor.impl.DestinationManager.validateUnreconciled",
                                            "1:634:1.508.1.7",
                                            this);

                SibTr.exception(tc, base);

                //play it safe and put the destination into indoubt
                putDestinationIntoIndoubtState(bdh.getUuid());
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "validateUnreconciled");
    }

    /**
     * Starts a new thread for reconstitution
     *
     * @param runnable
     * @throws InterruptedException
     */
    private void startNewReconstituteThread(Runnable runnable) throws InterruptedException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "startNewReconstituteThread");

        if (_reconstituteThreadpool == null)
        {
            createReconstituteThreadPool();
        }

        _reconstituteThreadpool.execute(runnable);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "startNewReconstituteThread");
    }

    /**
     * If the reconstitute thread pool hasn't been created,
     * create one here.
     * maxThreadPoolsize is calculated as (numberOfProcessors * 2)
     */
    private synchronized void createReconstituteThreadPool()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createReconstituteThreadPool");

        if (_reconstituteThreadpool == null)
        {
            int maxThreadPoolSize;

            if (messageProcessor.getMessagingEngine().datastoreExists()) {
                //data store. Proceed and calcualte number of threads to use for reconstitution

                //get the thread pool size from the custom property
                maxThreadPoolSize = messageProcessor.getCustomProperties().get_max_reconstitute_threadpool_size();

                int noOfCores = CpuInfo.getAvailableProcessors().get();

                if (maxThreadPoolSize <= 0)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.info(tc, "INVALID_RECONSTITUTE_THREADPOOL_SIZE_CWSIP0068", new Object[] { maxThreadPoolSize });
                    maxThreadPoolSize = noOfCores;
                }

                if (maxThreadPoolSize > noOfCores)
                    SibTr.warning(tc, "INVALID_RECONSTITUTE_THREADPOOL_SIZE_CWSIP0069");

            }
            else {
                // File store: Don't use multiple threads as it may lead to contention
                maxThreadPoolSize = 1;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.info(tc, "MAX_RECONSTITUTE_THREADPOOL_SIZE_CWSIP0070", new Object[] { maxThreadPoolSize });

            _reconstituteThreadpool = new ThreadPoolExecutor(maxThreadPoolSize, maxThreadPoolSize, Long.MAX_VALUE, TimeUnit.MILLISECONDS,
                            new LinkedBlockingQueue<Runnable>());
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createReconstituteThreadPool");
    }

    /**
     * Reconstitute this DestinationManager's Destinations.
     * initializeNonPersistent() has to have already have been called.
     *
     * @throws MessageStoreException if a problem is encountered which cannot be isolated
     *             to a particular destination.
     */
    protected void reconstitute(final int startMode) throws MessageStoreException
    {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "reconstitute", new Integer(startMode));

        /*
         * Iterate through all contained BaseDestinationHandlers and take appropriate
         * actions.
         */
        NonLockingCursor cursor = newNonLockingItemStreamCursor(new ClassEqualsFilter(BaseDestinationHandler.class));

        AbstractItem item = null;

        // Don't do flush if we are asked to start in recovery mode
        if (((startMode & JsConstants.ME_START_FLUSH) == JsConstants.ME_START_FLUSH)
            && ((startMode & JsConstants.ME_START_RECOVERY) == 0))
        {
            // Log message to console saying we have started flush for this ME
            SibTr.info(
                       tc,
                       "FLUSH_REQUESTED_MESSAGE_CWSIP0780",
                       messageProcessor.getMessagingEngineName());
        }

        // We let a Message Store cursor error here fall right down to the
        // MessageProcessor to invoke a cold start, on the basis that a failure here
        // is in all probably terminal for any future Message Store operations.
        while (null != (item = cursor.next()))
        {
            final BaseDestinationHandler dh = (BaseDestinationHandler) item;

            /*
             * If the destination is corrupt we want to ignore it on all subsequent
             * restarts. Attempting a delete will require reconstituting some
             * sections of the destination to keep a transaction happy, which will
             * most probably run into the same trouble that provoked the corruption in
             * the first place.
             */
            if (dh.isToBeIgnored())
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Ignoring old corrupt destination " + dh.getName());
            }

            /*
             * If the destination has been recovered but was actually temporary
             * (as can happen with STORE_MAYBE) then remove it now. Otherwise,
             * make the destination available for messages.
             */
            else if (dh.isTemporary())
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Deleting temporary destination " + dh.getName());

                // 174199.2.13
                // Need to remove all the ItemStreams and ReferenceStreams under this
                // temporary destination in order to delete it
                try
                {
                    dh.deleteDirtyTemporary(messageProcessor, durableSubscriptions);
                } catch (SIException e)
                {
                    FFDCFilter.processException(
                                                e,
                                                "com.ibm.ws.sib.processor.impl.DestinationManager.reconstitute",
                                                "1:788:1.508.1.7",
                                                this);

                    /*
                     * 174199.2.13
                     * Not being able to delete a temporary destination probably indicates
                     * a serious health problem.
                     * However, we can try to carry on.
                     */
                    SibTr.exception(tc, e);
                    SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                                new Object[] { "com.ibm.ws.sib.processor.impl.DestinationManager", "1:799:1.508.1.7", SIMPUtils.getStackTrace(e), dh.getName() });
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Deleted temporary destination " + dh.getName());
            }
            else
            {
                try
                {
                    // add all the tasks to the threadpool
                    startNewReconstituteThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                // 174199.2.9
                                dh.reconstitute(messageProcessor, durableSubscriptions, startMode);
                            }
                            //729873
                            catch (SIResourceException mse)
                            {
                                FFDCFilter.processException(
                                                            mse,
                                                            "com.ibm.ws.sib.processor.impl.DestinationManager.reconstitute",
                                                            "1:826:1.508.1.7", this);
                                SibTr.error(tc, "DESTINATION_HANDLER_RECOVERY_ERROR_CWSIP0051",
                                            new Object[] { dh.getName(), SIMPUtils.getStackTrace(mse) });

                                SibTr.exception(tc, mse);

                                if (mse.getCause() instanceof SevereMessageStoreException) {

                                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                                        SibTr.event(tc, "Datasource has thrown a severe error!", mse);

                                    messageProcessor.getMessageStore().reportLocalError();
                                }
                            }

                            catch (Exception e)
                            {
                                FFDCFilter.processException(
                                                            e,
                                                            "com.ibm.ws.sib.processor.impl.DestinationManager.reconstitute",
                                                            "1:846:1.508.1.7",
                                                            this);

                                // If the destination has been detected as corrupt for any reason,
                                // we are
                                // content just to isolate the destination and allow reconstitution
                                // to carry on.
                                // Log to console
                                SibTr.error(
                                            tc,
                                            "DESTINATION_HANDLER_RECOVERY_ERROR_CWSIP0051",
                                            new Object[] { dh.getName(), SIMPUtils.getStackTrace(e) });

                                SibTr.exception(tc, e);
                            }

                            // Don't need to synchronize the add since no other thread should be
                            // interacting with the DestinationManager during reconstitution.
                            // 182637 - Currently, we are storing no information on whether the
                            // PtoPMessageItemStream was local or remote, or whether there was more than
                            // one.
                            DestinationIndex.Type type = new DestinationIndex.Type();
                            if (dh.isCorruptOrIndoubt())
                                type.state = State.CORRUPT;
                            else if (dh.isSystem())
                            {
                                // We need to add localisations for the system destinations here since they
                                // will not reconcile because they are not in wccm.
                                type.state = State.ACTIVE;

                                SIBUuid8 parsedME = SIMPUtils.parseME(dh.getName());

                                Set<String> queuePointLocalizingMEs = new HashSet<String>();
                                queuePointLocalizingMEs.add(parsedME.toString());

                                try
                                {
                                    dh.updateLocalizationSet(queuePointLocalizingMEs);
                                } catch (SIResourceException e)
                                {
                                    FFDCFilter.processException(
                                                                e,
                                                                "com.ibm.ws.sib.processor.impl.DestinationManager.reconstitute",
                                                                "1:890:1.508.1.7",
                                                                this);

                                    SibTr.exception(tc, e);
                                }

                            }
                            else
                                type.state = State.UNRECONCILED;
                            type.alias = new Boolean(dh.isAlias());
                            type.foreignDestination = new Boolean(dh.isForeign());
                            type.queue = new Boolean(!dh.isPubSub());
                            type.local = new Boolean(dh.hasLocal());
                            type.remote = new Boolean(dh.hasRemote());
                            destinationIndex.put(dh, type);

                            dh.registerControlAdapters();

                            Object[] uuids = dh.getPostReconstitutePseudoIds();

                            if (uuids != null)
                            {
                                for (int i = 0; i < uuids.length; i++)
                                {
                                    SIBUuid12 pseudoUuid = (SIBUuid12) uuids[i];

                                    destinationIndex.addPseudoUuid(dh, pseudoUuid);
                                }
                            }

                        }//end of run()
                    });
                } catch (InterruptedException e) {
                    FFDCFilter.processException(
                                                e,
                                                "com.ibm.ws.sib.processor.impl.DestinationManager.reconstitute",
                                                "1:926:1.508.1.7",
                                                this);
                }
            }
        }

        // Wait until the reconstitution of BaseDestinationHandler is completed
        if (_reconstituteThreadpool != null)
            waitUntilReconstitutionIsCompleted();
        cursor.finished();

        /*
         * Iterate through all contained LinkDestinationHandlers and take appropriate
         * actions.
         */
        cursor = newNonLockingItemStreamCursor(new ClassEqualsFilter(LinkHandler.class));

        item = null;

        // We let a Message Store cursor error here fall right down to the
        // MessageProcessor to invoke a cold start, on the basis that a failure here
        // is in all probably terminal for any future Message Store operations.
        while (null != (item = cursor.next()))
        {

            final LinkHandler linkHandler = (LinkHandler) item;

            /*
             * If the link is corrupt we want to ignore it on all subsequent
             * restarts. Attempting a delete will require reconstituting some
             * sections of the link to keep a transaction happy, which will
             * most probably run into the same trouble that provoked the corruption in
             * the first place.
             */
            if (linkHandler.isToBeIgnored())
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Ignoring old corrupt link " + linkHandler.getName());
            }
            else
            {
                try
                {
                    // add all the tasks to the threadpool
                    startNewReconstituteThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {

                            try
                            {
                                linkHandler.reconstitute(messageProcessor,
                                                         durableSubscriptions, startMode);
                            } catch (Exception e) {
                                FFDCFilter.processException(
                                                            e,
                                                            "com.ibm.ws.sib.processor.impl.DestinationManager.reconstitute",
                                                            "1:984:1.508.1.7",
                                                            this);

                                /*
                                 * Not being able to recover a link probably indicates
                                 * a serious health problem.
                                 * However, we will try to carry on. The data of the unrecoverable
                                 * link remains in the database for later recovery.
                                 */
                                // Log to console
                                SibTr.error(
                                            tc,
                                            "LINK_HANDLER_WARM_START_ERROR_CWSIP0056",
                                            new Object[] {
                                                          "com.ibm.ws.sib.processor.impl.DestinationManager",
                                                          "1:999:1.508.1.7", SIMPUtils.getStackTrace(e) });

                                SibTr.exception(tc, e);
                            }

                            LinkIndex.Type type = new LinkIndex.Type();
                            if (linkHandler.isCorruptOrIndoubt())
                                type.state = State.CORRUPT;
                            else if (linkHandler.isSystem())
                                type.state = State.ACTIVE;
                            else
                                type.state = State.UNRECONCILED;
                            type.mqLink = Boolean.FALSE;
                            type.local = new Boolean(linkHandler.hasLocal());
                            type.remote = new Boolean(!linkHandler.hasLocal());
                            linkIndex.put(linkHandler, type);

                            linkHandler.registerControlAdapters();
                        }// end of run()
                    });
                } catch (InterruptedException e) {
                    FFDCFilter.processException(
                                                e,
                                                "com.ibm.ws.sib.processor.impl.DestinationManager.reconstitute",
                                                "1:1023:1.508.1.7",
                                                this);
                }
            }
        }

        // Wait until the reconstitution of LinkHandler is completed
        if (_reconstituteThreadpool != null)
            waitUntilReconstitutionIsCompleted();
        cursor.finished();

        /*
         * Iterate through all contained MQLinkDestinationHandlers and take appropriate
         * actions.
         */
        cursor = newNonLockingItemStreamCursor(new ClassEqualsFilter(MQLinkHandler.class));

        item = null;

        // We let a Message Store cursor error here fall right down to the
        // MessageProcessor to invoke a cold start, on the basis that a failure here
        // is in all probably terminal for any future Message Store operations.
        while (null != (item = cursor.next()))
        {

            final MQLinkHandler linkHandler = (MQLinkHandler) item;

            /*
             * If the link is corrupt we want to ignore it on all subsequent
             * restarts. Attempting a delete will require reconstituting some
             * sections of the link to keep a transaction happy, which will
             * most probably run into the same trouble that provoked the corruption in
             * the first place.
             */
            if (linkHandler.isToBeIgnored())
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Ignoring old corrupt MQlink "
                                    + linkHandler.getName());
            }
            else
            {
                try
                {
                    // add all the tasks to the threadpool
                    startNewReconstituteThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                linkHandler.reconstitute(messageProcessor, durableSubscriptions, startMode);
                            } catch (Exception e)
                            {
                                FFDCFilter.processException(
                                                            e,
                                                            "com.ibm.ws.sib.processor.impl.DestinationManager.reconstitute",
                                                            "1:1082:1.508.1.7",
                                                            this);

                                /*
                                 * Not being able to recover a link probably indicates
                                 * a serious health problem.
                                 * However, we will try to carry on. The data of the unrecoverable
                                 * link remains in the database for later reset.
                                 */
                                SibTr.error(
                                            tc,
                                            "LINK_HANDLER_WARM_START_ERROR_CWSIP0056",
                                            new Object[] { linkHandler.getName(),
                                                          linkHandler.getUuid(),
                                                          SIMPUtils.getStackTrace(e) });

                                SibTr.exception(tc, e);
                            }

                            LinkIndex.Type type = new LinkIndex.Type();
                            if (linkHandler.isCorruptOrIndoubt())
                                type.state = State.CORRUPT;
                            else if (linkHandler.isSystem())
                                type.state = State.ACTIVE;
                            else
                                type.state = State.UNRECONCILED;
                            type.mqLink = Boolean.TRUE;
                            type.local = Boolean.TRUE;
                            type.remote = Boolean.FALSE;
                            linkIndex.put(linkHandler, type);

                            linkHandler.registerControlAdapters();
                        }//end of run()
                    });
                } catch (InterruptedException e) {
                    FFDCFilter.processException(
                                                e,
                                                "com.ibm.ws.sib.processor.impl.DestinationManager.reconstitute",
                                                "1:1120:1.508.1.7",
                                                this);
                }
            }
        }

        // Wait until the reconstitution of MQLinkHandler is completed
        if (_reconstituteThreadpool != null)
            waitUntilReconstitutionIsCompleted();
        cursor.finished();

        // Don't do flush if we are asked to start in recovery mode
        if (((startMode & JsConstants.ME_START_FLUSH) == JsConstants.ME_START_FLUSH)
            && ((startMode & JsConstants.ME_START_RECOVERY) == 0))
        {
            // Log message to console saying we have finished flush for this ME
            SibTr.info(
                       tc,
                       "FLUSH_COMPLETED_MESSAGE_CWSIP0783",
                       messageProcessor.getMessagingEngineName());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "reconstitute");
    }

    /**
     * Will wait until the reconstitution is completed
     * This will be called for each of BaseDestinationHandler, LinkHandler and MQLinkHandler
     * The threadpool will be destroyed
     */
    private void waitUntilReconstitutionIsCompleted()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "waitUntilReconstitutionISCompleted");

        // gracefully shutdown the pool
        _reconstituteThreadpool.shutdown();

        //awaitTermination() is a blocking call and caller thread will be blocked
        //until all tasks in pool have completed execution after a shutdown request
        try
        {
            _reconstituteThreadpool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e)
        {
            // No FFDC code needed
            SibTr.exception(tc, e);
        }

        //Destroy the thread pool
        _reconstituteThreadpool = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "waitUntilReconstitutionISCompleted");
    }

    /**
     * Gets the link destination from the set of destinations
     *
     * @param linkName
     * @return
     */
    public final LinkHandler getLink(String linkName)
    {
        LinkTypeFilter filter = new LinkTypeFilter();
        return (LinkHandler) linkIndex.findByName(linkName, filter);
    }

    /**
     * This method provides lookup of a destination by its address.
     * If the destination is not
     * found, it throws SIDestinationNotFoundException.
     *
     * @param destinationAddr
     * @return Destination
     * @throws SIDestinationNotFoundException
     * @throws SIMPNullParameterException
     * @throws SIMPDestinationCorruptException
     */
    public DestinationHandler getDestination(JsDestinationAddress destinationAddr, boolean includeInvisible)

                    throws SITemporaryDestinationNotFoundException, SIResourceException, SINotPossibleInCurrentConfigurationException
    {
        return getDestination(destinationAddr.getDestinationName(),
                              destinationAddr.getBusName(),
                              includeInvisible,
                              false);
    }

    /**
     * This method provides lookup of a destination by its address.
     * If the destination is not
     * found, it throws SIDestinationNotFoundException.
     *
     * @param destinationAddr
     * @return Destination
     * @throws SIDestinationNotFoundException
     * @throws SIMPNullParameterException
     * @throws SIMPDestinationCorruptException
     */
    public DestinationHandler getDestination(JsDestinationAddress destinationAddr, boolean includeInvisible, boolean createRemoteDest)

                    throws SITemporaryDestinationNotFoundException, SIResourceException, SINotPossibleInCurrentConfigurationException
    {
        return getDestination(destinationAddr.getDestinationName(),
                              destinationAddr.getBusName(),
                              includeInvisible,
                              createRemoteDest);
    }

    /**
     * Get a destination from its name. This method assumes we wish to look in
     * the local bus.
     *
     * @param destinationName
     * @return
     *
     */
    public DestinationHandler getDestination(String destinationName, boolean includeInvisible)
                    throws SITemporaryDestinationNotFoundException, SIResourceException, SINotPossibleInCurrentConfigurationException

    {
        return getDestination(destinationName, null, includeInvisible, false);
    }

    /**
     * Get a destination from its name. This is the full name, which is both
     * the destination name and its bus name.
     *
     * @param destinationName.
     * @param busName. Can be null, in which case the local bus will be assumed.
     * @return Destination
     *
     */
    public DestinationHandler getDestination(String destinationName, String busName, boolean includeInvisible)
                    throws SIResourceException, SITemporaryDestinationNotFoundException, SINotPossibleInCurrentConfigurationException
    {
        return getDestination(destinationName, busName, includeInvisible, false);
    }

    /**
     * Get a destination from its name. This is the full name, which is both
     * the destination name and its bus name.
     *
     * @param destinationName.
     * @param busName. Can be null, in which case the local bus will be assumed.
     * @return Destination
     *
     */
    public DestinationHandler getDestination(String destinationName,
                                             String busName,
                                             boolean includeInvisible,
                                             boolean createRemoteDest)
                    throws SIResourceException, SITemporaryDestinationNotFoundException, SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getDestination", new Object[] { destinationName, busName, new Boolean(includeInvisible), new Boolean(createRemoteDest) });

        DestinationHandler dh;
        try
        {
            dh = getDestinationInternal(destinationName, busName, null, includeInvisible, createRemoteDest);
        } catch (SIIncorrectCallException e)
        {
            // As we're not creating anything here (create = false) then we should never
            // get this.
            // This exception should not be thrown so FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.DestinationManager.getDestination",
                                        "1:1103:1.487",
                                        this);

            SIMPErrorException ee =
                            new SIMPErrorException(
                                            nls.getFormattedMessage(
                                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                                                                    new Object[] { "com.ibm.ws.sib.processor.impl.DestinationManager", "1:1092:1.487" },
                                                                    null));

            SibTr.exception(tc, e);

            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                        new Object[] { "com.ibm.ws.sib.processor.impl.DestinationManager", "1:1109:1.487" });

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "getDestination", ee);

            // This should never be thrown
            throw ee;
        }

        checkDestinationHandlerExists(
                                      dh != null,
                                      destinationName,
                                      messageProcessor.getMessagingEngineName());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getDestination", dh);

        return dh;
    }

    /**
     * Get a destination from its name. This is the full name, which is both
     * the destination name and its bus name.
     * <p>
     * This internal function has a validator to record destination names which
     * have been seen before in creating an alias chain. This is to facilitate
     * loop protection.
     *
     * @param destinationName
     * @param busName Can be null, in which case the local bus will be assumed.
     * @param validator Should be null when first called.
     * @return Destination
     * @throws SIIncorrectCallException
     */
    private DestinationHandler getDestinationInternal(
                                                      String destinationName,
                                                      String busName,
                                                      AliasChainValidator validator,
                                                      boolean includeInvisible,
                                                      boolean create)

                    throws SIResourceException, SITemporaryDestinationNotFoundException, SINotPossibleInCurrentConfigurationException, SIIncorrectCallException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "getDestinationInternal",
                        new Object[] { destinationName, busName, validator, new Boolean(includeInvisible) });

        /* Check that the destination name is not null. */
        checkDestinationHandlerExists(
                                      destinationName != null,
                                      destinationName,
                                      messageProcessor.getMessagingEngineName());

        boolean remoteTemporaryOrSystem = false;
        boolean system = false;
        boolean temporary = (destinationName.startsWith(SIMPConstants.TEMPORARY_QUEUE_DESTINATION_PREFIX) ||
                        destinationName.startsWith(SIMPConstants.TEMPORARY_PUBSUB_DESTINATION_PREFIX));
        if (!temporary)
            system = destinationName.startsWith(SIMPConstants.SYSTEM_DESTINATION_PREFIX);

        SIBUuid8 parsedME = null;

        DestinationHandler destination = null;

        //if it is a temporary or system destination then it won't have been defined
        //in WCCM. Therefore we need to try and work out where it is localized from
        //the destination name we have been given. The name should include the uuid
        //of the localizing ME.
        if (temporary || system)
        {
            if ((busName == null) ||
                (busName.equals(messageProcessor.getMessagingEngineBus())))
            {
                //get the ME uuid from the destination name
                parsedME = SIMPUtils.parseME(destinationName);
                if (parsedME != null)
                {
                    //work out if it is a local or remote destination
                    remoteTemporaryOrSystem = !parsedME.equals(messageProcessor.getMessagingEngineUuid());

                    //if it is remote then we may need to dynamically create the remote representation
                    //of that destination
                    if (remoteTemporaryOrSystem)
                    {
                        if (temporary)
                        {
                            //if this is a remote temporary destination then we will actually be
                            //using a specialized pt-pt system destination - a TDRECEIVER.
                            //There will be one of these per remote ME.
                            // System queues use their own streams.
                            JsDestinationAddress actualDestinationAddr =
                                            SIMPUtils.createJsSystemDestinationAddress(SIMPConstants.TDRECEIVER_SYSTEM_DESTINATION_PREFIX, parsedME);
                            destinationName = actualDestinationAddr.getDestinationName();
                            busName = actualDestinationAddr.getBusName();
                        }
                    }
                }
                else //couldn't parse the destination name, throw an excption
                {
                    SIMPErrorException e =
                                    new SIMPErrorException(
                                                    nls.getFormattedMessage(
                                                                            "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                                                                            new Object[] { "com.ibm.ws.sib.processor.impl.DestinationManager", "1:1092:1.487" },
                                                                            null));

                    e.setExceptionReason(SIRCConstants.SIRC0900_INTERNAL_MESSAGING_ERROR);
                    e.setExceptionInserts(new String[] { "com.ibm.ws.sib.processor.impl.DestinationManager",
                                                        "1:1097:1.487" });

                    // This exception should not be thrown so FFDC
                    FFDCFilter.processException(
                                                e,
                                                "com.ibm.ws.sib.processor.impl.DestinationManager.getDestinationInternal",
                                                "1:1103:1.487",
                                                this);

                    SibTr.exception(tc, e);

                    SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                                new Object[] { "com.ibm.ws.sib.processor.impl.DestinationManager", "1:1109:1.487" });

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "getDestinationInternal", e);

                    // This should never be thrown
                    throw e;
                }
            }
        }

        // Before we do checks which might involve non local bus destinations,
        // make sure the bus name reflects the local bus if not otherwise
        // specified.
        if (busName == null || busName.equals(""))
            busName = messageProcessor.getMessagingEngineBus();

        // See if we already have the destination loaded
        destination = getDestinationInternal(destinationName, busName, includeInvisible);

        // If the destination is not in memory (if, for instance,
        // it's an unloaded alias destination), then look it up in admin and create
        // if appropriate (we don't bother for foreign system or temporary destinations
        // as they cannot be configured)
        if ((destination == null) && !(temporary || system))
        {
            destination = loadDestination(destinationName, busName, validator, true);
        }

        // If the destination is on another bus, it isn't in memory and it doesn't
        // have a specific foreign destination definition in admin, then try to
        // get the default foreign destination for that bus.
        if (destination == null && !busName.equals(messageProcessor.getMessagingEngineBus()))
        {
            destination = findBus(busName);
        }

        // If this is a remote temporary or system queue there is no Admin config for it
        // to use as an identity understood across all MEs in the bus. So instead we use
        // a special catch-all 'queue' to allow us to send messages from this ME to another
        // one in the bus and for the other ME to understand what's happening.
        else if ((destination == null) && remoteTemporaryOrSystem && create)
        {
            //Lookup a system destination or a temporary destination.  These are always on the local
            //bus
            destination = createRemoteSystemDestination(destinationName, parsedME);
        }

        /*
         * If we've found the destination but it's corrupt, do not permit the
         * requester to receive it.
         */
        if (destination != null && destination.isCorruptOrIndoubt())
        {
            String message =
                            nls_cwsik.getFormattedMessage(
                                                          "DELIVERY_ERROR_SIRC_27", // DESTINATION_CORRUPT_ERROR_CWSIP0058
                                                          new Object[] { destinationName }, null);

            SIMPDestinationCorruptException e = new SIMPDestinationCorruptException(message);

            e.setExceptionReason(SIRCConstants.SIRC0027_DESTINATION_CORRUPT_ERROR);
            e.setExceptionInserts(new String[] { destinationName });

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "getDestinationInternal", e);

            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getDestinationInternal", destination);

        return destination;
    }

    /**
     * Lookup a destination definition via admin and create the corresponding
     * destination.
     *
     * This internal function has a collector to record destination names which
     * have been seen before in creating an alias chain. This is to facilitate
     * loop protection.
     *
     * @param busName
     * @param destinationIdentifier
     * @param collector Should be null when first called.
     * @param findByName if true the first parameter represent a destinationName, if false it is a uuid
     * @return
     */
    private DestinationHandler loadDestination(String destinationId, String busName, AliasChainValidator validator, boolean findByName) throws SINotPossibleInCurrentConfigurationException, SIMPDestinationCorruptException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "loadDestination", new Object[] { destinationId, busName, validator, Boolean.valueOf(findByName) });

        DestinationHandler destinationHandler = null;

        try
        {
            BaseDestinationDefinition bdd = null;
            if (findByName)
                bdd = messageProcessor.getMessagingEngine().getSIBDestination(busName, destinationId);
            else
                bdd = messageProcessor.getMessagingEngine().getSIBDestinationByUuid(busName, destinationId);

            // The returned destination can be remote or an alias.  We need to act
            // accordingly.
            if (bdd.isAlias())
            {
                // The destination is an alias.
                DestinationAliasDefinition add = (DestinationAliasDefinition) bdd;

                destinationHandler = createAliasDestination(add, validator, null);
            }
            else if (bdd.isForeign())
            {
                // The destination is foreign.
                DestinationForeignDefinition dfd = (DestinationForeignDefinition) bdd;

                destinationHandler = createForeignDestination(dfd, busName);
            }
            else
            {
                /*
                 * Assume the definition is local, but the actual destination is remote.
                 */

                // Try to add the remote destination.
                try
                {
                    DestinationDefinition dDef = (DestinationDefinition) bdd;

                    Set<String> queuePointLocalitySet =
                                    messageProcessor.getSIBDestinationLocalitySet(busName, dDef.getUUID().toString(), false);

                    if ((queuePointLocalitySet.contains(messageProcessor.getMessagingEngineUuid().toString())) ||
                        ((dDef.getDestinationType() == DestinationType.QUEUE) && queuePointLocalitySet.size() == 0) ||
                        ((dDef.getDestinationType() == DestinationType.PORT) && queuePointLocalitySet.size() == 0))
                    {
                        //Error case as destination should either be localised on this ME or should
                        //be localised somewhere
                        SIErrorException e = new SIErrorException(
                                        nls.getFormattedMessage(
                                                                "INTERNAL_CONFIGURATION_ERROR_CWSIP0006",
                                                                new Object[] { "DestintionManager", "1:1597:1.508.1.7", dDef.getName() },
                                                                null));
                        FFDCFilter.processException(
                                                    e,
                                                    "com.ibm.ws.sib.processor.impl.DestinationManager.loadDestination",
                                                    "1:1602:1.508.1.7",
                                                    this);

                    }
                    else
                    {
                        destinationHandler =
                                        createRemoteDestination(dDef, queuePointLocalitySet);
                    }
                } catch (SIException e)
                {
                    // No FFDC code needed

                    SibTr.exception(tc, e);

                    // TODO - handle this
                    // throw e;
                }
            }
        }

        // Catch Admin's SIBExceptionDestinationNotFound exception
        catch (SIBExceptionDestinationNotFound e)
        {
            // No FFDC code needed

            SibTr.exception(tc, e);

            // Exception not fatal - just return null to show no destiation has been
            // found.
        } catch (SIBExceptionBase e)
        {
            // No FFDC code needed

            SibTr.exception(tc, e);

            // TO DO - handle this
        } catch (SIResourceException e)
        {
            // No FFDC code needed
            if (!(e instanceof SIMPResourceException))
            {
                SIMPResourceException ee = new SIMPResourceException(e);
                ee.setExceptionReason(SIRCConstants.SIRC0901_INTERNAL_MESSAGING_ERROR);
                ee.setExceptionInserts(new String[] { "com.ibm.ws.sib.processor.impl.DestinationManager.loadDestination",
                                                     "1:1650:1.508.1.7",
                                                     SIMPUtils.getStackTrace(e) });
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "loadDestination", ee);
                throw ee;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "loadDestination", e);
            throw e;

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "loadDestination", destinationHandler);

        return destinationHandler;
    }

    /**
     * Get the destination.
     *
     * @param destinationUuid The uuid to find
     * @return DestinationHandler
     */
    public final DestinationHandler getDestinationInternal(
                                                           String destinationName,
                                                           String busName,
                                                           boolean includeInvisible)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "getDestinationInternal",
                        new Object[] { destinationName, busName, new Boolean(includeInvisible) });

        // Now look for the destination in the hash table
        DestinationTypeFilter destFilter = new DestinationTypeFilter();
        if (!includeInvisible)
        {
            destFilter.VISIBLE = Boolean.TRUE;
        }
        DestinationHandler dh = destinationIndex.findByName(destinationName, busName, destFilter);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getDestinationInternal", dh);
        return dh;
    }

    /**
     * Get the destination.
     *
     * @param destinationUuid The uuid to find
     * @return DestinationHandler
     */
    public final DestinationHandler getDestinationInternal(SIBUuid12 destinationUuid, boolean includeInvisible)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getDestinationInternal", new Object[] { destinationUuid, new Boolean(includeInvisible) });

        // Now look for the destination in the hash table
        DestinationTypeFilter destFilter = new DestinationTypeFilter();
        if (!includeInvisible)
        {
            destFilter.VISIBLE = Boolean.TRUE;
        }
        DestinationHandler dh = destinationIndex.findByUuid(destinationUuid, destFilter);
        if (dh == null)
        {
            LinkTypeFilter linkFilter = new LinkTypeFilter();
            if (!includeInvisible)
            {
                linkFilter.VISIBLE = Boolean.TRUE;
            }
            dh = linkIndex.findByUuid(destinationUuid, linkFilter);
        }
        if (dh == null)
        {
            ForeignBusTypeFilter busFilter = new ForeignBusTypeFilter();
            if (!includeInvisible)
            {
                busFilter.VISIBLE = Boolean.TRUE;
            }
            dh = foreignBusIndex.findByUuid(destinationUuid, busFilter);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getDestinationInternal", dh);

        return dh;
    }

    /**
     * Link a pseudo destination ID to a real destination handler. This is used to link destination
     * IDs created for remote durable pub/sub to the appropriate Pub/Sub destination handler which
     * hosts a localization of some durable subscription.
     *
     * @param destinationUuid The pseudo destination ID to link.
     * @param handler The pub/sub (well BaseDestinationHandler anyway) which the destination is linked to.
     */
    public final void addPseudoDestination(SIBUuid12 destinationUuid, BaseDestinationHandler handler)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "addPseudoDestination", new Object[] { destinationUuid, handler });
        destinationIndex.addPseudoUuid(handler, destinationUuid);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "addPseudoDestination");
    }

    /**
     * Remove a link for a pseudo desintation ID.
     *
     * @param destinationUuid The ID of the pseudo destination to remove.
     */
    public final void removePseudoDestination(SIBUuid12 destinationUuid)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "removePseudoDestination", destinationUuid);
        destinationIndex.removePseudoUuid(destinationUuid);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "removePseudoDestination");
    }

    /**
     * Reset a destination.
     *
     * @param destName
     *
     * @throws InvalidOperationException if the destination is not one which can
     *             be reset (e.g. it is an alias destination).
     * @throws SIDestinationNotFoundException
     */
    public void resetDestination(String destName)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "resetDestination", destName);

        try
        {
            DestinationHandler dh = destinationIndex.findByName(
                                                                destName,
                                                                messageProcessor.getMessagingEngineBus(),
                                                                null);

            checkDestinationHandlerExists(
                                          dh != null,
                                          destName,
                                          messageProcessor.getMessagingEngineBus());

            // Only applicable to BaseDestinationHandlers
            if (dh instanceof BaseDestinationHandler)
            {
                BaseDestinationHandler bdh = (BaseDestinationHandler) dh;

                // Need to ensure that this change is persisted - specifically that the
                // BDH "toBeIgnored" flag is persisted
                LocalTransaction siTran = txManager.createLocalTransaction(true);

                // Drive the object's reset method
                bdh.reset();

                // Change our entry in the appropriate index.
                destinationIndex.reset(dh);

                // Persist the change
                bdh.requestUpdate((Transaction) siTran);

                // commit the transaction
                siTran.commit();

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Have reset destination " + bdh.getName());
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Not a BDH, cannot reset destination " + dh.getName());
            }
        } catch (MessageStoreException e)
        {
            // No FFDC code needed

            SibTr.exception(tc, e);

            //throw e;
        } catch (SIException e)
        {
            // No FFDC code needed

            SibTr.exception(tc, e);

            //handleRollback(siTran);
            //        throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "resetDestination");
    }

    /**
     * Reset a link.
     *
     * @param linkName
     *
     * @throws InvalidOperationException if the link is not one which can
     *             be reset.
     * @throws SINotPossibleInCurrentConfigurationException
     */
    public void resetLink(String linkName)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "resetLink", linkName);

        try
        {
            DestinationHandler link = linkIndex.findByName(linkName, null);

            checkDestinationHandlerExists(
                                          link != null,
                                          linkName,
                                          messageProcessor.getMessagingEngineBus());

            // Only applicable to BaseDestinationHandlers and their children
            if (link instanceof LinkHandler)
            {
                LinkHandler linkhandler = (LinkHandler) link;

                // Need to ensure that this change is persisted - specifically that the
                // BDH "toBeIgnored" flag is persisted
                LocalTransaction siTran = txManager.createLocalTransaction(true);

                linkhandler.reset();

                // Change our entry in the appropriate index.
                linkIndex.reset(link);

                // Persist the change
                linkhandler.requestUpdate((Transaction) siTran);

                // commit the transaction
                siTran.commit();

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Have reset link " + linkhandler.getName());
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Not a LinkHandler, cannot reset handler for " + link.getName());
            }
        } catch (MessageStoreException e)
        {
            // No FFDC code needed

            SibTr.exception(tc, e);

            //throw e;
        } catch (SIException e)
        {
            // No FFDC code needed

            SibTr.exception(tc, e);

            //handleRollback(siTran);
            //        throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "resetLink");
    }

    /**
     * Returns the link definition of the link used to connect to the given busname
     *
     * @return VirtualLinkDefinition
     */
    public VirtualLinkDefinition getLinkDefinition(String busName)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getLinkDefinition", busName);

        ForeignBusDefinition foreignBus = messageProcessor.getForeignBus(busName);

        VirtualLinkDefinition link = null;

        if (foreignBus != null && foreignBus.hasLink())
        {
            try
            {
                link = foreignBus.getLink();
            } catch (SIBExceptionNoLinkExists e)
            {
                // SIBExceptionNoLinkExists shouldn't occur so FFDC.
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.DestinationManager.getLinkDefinition",
                                            "1:1951:1.508.1.7",
                                            this);

                SibTr.exception(tc, e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getLinkDefinition", link);

        return link;
    }

    /**
     * Returns the topicSpaceName of the foreign topicSpace
     *
     * @param String The busname of the foreign TS
     * @param SIBUuid12 The uuid of the TS on this bus
     * @return String The foreign TS name
     */
    public String getTopicSpaceMapping(String busName, SIBUuid12 topicSpace)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getTopicSpaceMapping", new Object[] { busName, topicSpace });

        VirtualLinkDefinition linkDef = getLinkDefinition(busName);
        //this is only called internally so we shall include invisible dests in the lookup
        String topicSpaceName = getDestinationInternal(topicSpace, true).getName();
        String mapping = null;

        if (linkDef != null && linkDef.getTopicSpaceMappings() != null)
            mapping = (String) linkDef.getTopicSpaceMappings().get(topicSpaceName);
        else
            // Local ME
            mapping = topicSpaceName;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getTopicSpaceMapping", mapping);
        return mapping;
    }

    /**
     * Method getDurableSubscriptionsTable.
     *
     * @return durableSubHashMap
     */

    public HashMap getDurableSubscriptionsTable()
    {
        return durableSubscriptions;
    }

    /**
     * Method destinationExists
     *
     * @param addr
     * @return boolean
     * @throws SIMPNullParameterException
     *             <p>This method returns true if the named destination is known in the
     *             destination manager, otherwise it returns false.</p>
     */
    public boolean destinationExists(JsDestinationAddress addr) throws SIMPNullParameterException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "destinationExists", new Object[] { addr });

        // Now look for the destination in the hash table
        boolean exists = destinationExists(addr.getDestinationName(), addr.getBusName());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "destinationExists", new Boolean(exists));

        return exists;
    }

    /**
     * Method destinationExists
     *
     * @param destinationName
     * @param busName
     * @return boolean
     * @throws SIMPNullParameterException
     *             <p>This method returns true if the named destination is known in the
     *             destination manager, otherwise it returns false.</p>
     */
    public boolean destinationExists(String destinationName, String busName) throws SIMPNullParameterException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "destinationExists", new Object[] { destinationName, busName });

        // Check that the destination name is not null.  If it is, throw a
        // SIMPNullParameterException
        if (destinationName == null)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(
                           tc,
                           "destinationExists",
                           "Destination name null");

            throw new SIMPNullParameterException(
                            nls_cwsik.getFormattedMessage(
                                                          "MISSING_PARAM_ERROR_CWSIP0044",
                                                          new Object[] { messageProcessor.getMessagingEngineName() }, null));

        }

        // Now look for the destination in the hash table
        boolean exists = destinationIndex.containsKey(busName, destinationName);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "destinationExists", new Boolean(exists));

        return exists;

    }

    /**
     * The Temporary Destination Name that is created is of the form
     * _T<User prefix>_<MEId>_<TempDestId> for Topicspace Destinations
     * or
     * _Q<User prefix>_<MEId>_<TempDestId> for Queue Destinations
     *
     * Where the User prefix is limited to 12 characters (others are ignored)
     * The MEId is the ME Uuid which will is a SIBUuid8
     * The TempDestId is a count value in hex
     *
     * eg _TSAMPLENAME_702CD771-5262B260_00016AE4
     *
     * @param modelDestinationName The distribution (ONE or ALL)
     * @param destinationPrefix The destination prefic, user specified
     *
     *            When the maximum value for the tempDestCount is reached (Integer.MAX_VALUE),
     *            we FFDC and throw an SIException to indicate that destinations may
     *            no longer be unique.
     *
     *            An SIException is also thrown if the temporary destination name already
     *            exists. The algorithm used should not generate the same named Temporary destination.
     *            (Unless the tempDestCount has wrapped)
     *
     * @return
     */
    protected SIDestinationAddress createTemporaryDestination(Distribution distribution, String destinationPrefix) throws SIResourceException, SIMPDestinationAlreadyExistsException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createTemporaryDestination", new Object[] { destinationPrefix, distribution });

        String name = createNewTemporaryDestinationName(destinationPrefix, messageProcessor.getMessagingEngineUuid(), distribution);

        DestinationType destinationType = DestinationType.QUEUE;

        if (distribution == Distribution.ALL)
        {
            destinationType = DestinationType.TOPICSPACE;
        }

        // Get destination definition and set specific attributes
        DestinationDefinition tempDestDefinition = messageProcessor.createDestinationDefinition(destinationType, name);

        // Set destination attributes applicable to temporary destinations
        tempDestDefinition.setMaxReliability(Reliability.RELIABLE_NONPERSISTENT);
        tempDestDefinition.setDefaultReliability(Reliability.RELIABLE_NONPERSISTENT);
        tempDestDefinition.setUUID(new SIBUuid12());

        // If the temporary destination name exists, then this is an error situation and
        // an exception is thrown.
        if (destinationExists(name, messageProcessor.getMessagingEngineBus()))
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "createTemporaryDestination", "Destination with name " + name + " already exists");

            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                        new Object[] { "com.ibm.ws.sib.processor.impl.DestinationManager", "1:2120:1.508.1.7" });

            throw new SIErrorException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                                                    new Object[] { "com.ibm.ws.sib.processor.impl.DestinationManager", "1:2125:1.508.1.7" },
                                                    null));
        }

        // Create the new SIDestinationAddress
        SIDestinationAddress address =
                        (
                        (SIDestinationAddressFactory) MessageProcessor.getSingletonInstance(
                                        SIMPConstants.SI_DESTINATION_ADDRESS_FACTORY)).createSIDestinationAddress(
                                                                                                                  name,
                                                                                                                  true);

        ((JsDestinationAddress) address).setBusName(messageProcessor.getMessagingEngineBus());
        ((JsDestinationAddress) address).setME(messageProcessor.getMessagingEngineUuid());

        Set<String> destinationLocalizingSet = new HashSet<String>();
        destinationLocalizingSet.add(messageProcessor.getMessagingEngineUuid().toString());

        LocalizationDefinition localizationDefinition =
                        messageProcessor.createLocalizationDefinition(tempDestDefinition.getName());
        localizationDefinition.setDestinationHighMsgs(messageProcessor.getHighMessageThreshold());

        createDestinationLocalization(
                                      tempDestDefinition,
                                      localizationDefinition,
                                      destinationLocalizingSet,
                                      true);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createTemporaryDestination", address);
        return address;
    }

    /**
     * Creates a new name for a temporary destination.
     * Uses the Message Store Tick count to generate the unique suffix for the temporary destination
     *
     * @param destinationPrefix
     * @param meUuid
     * @param distribution
     * @return
     * @throws SILimitExceededException
     */
    String createNewTemporaryDestinationName(String destinationPrefix,
                                             SIBUuid8 meUuid,
                                             Distribution distribution)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createNewTemporaryDestinationName",
                        new Object[] { destinationPrefix, meUuid, distribution });

        if (destinationPrefix != null)
        {
            if (destinationPrefix.length() > 12)
                destinationPrefix = destinationPrefix.substring(0, 12);
        }
        else
            destinationPrefix = "";

        // Get the next available temporary destination count.
        long count = messageProcessor.nextTick();

        // Suffix is the tempdest count as 8 char hex with leading zeros
        StringBuffer sb = new StringBuffer("0000000000000000" + Long.toHexString(count).toUpperCase());

        String uniqueSuffix = sb.substring(sb.length() - 16).toString();

        String tempPrefix = null;
        if (distribution == Distribution.ONE)
            tempPrefix = SIMPConstants.TEMPORARY_QUEUE_DESTINATION_PREFIX;
        else
            tempPrefix = SIMPConstants.TEMPORARY_PUBSUB_DESTINATION_PREFIX;

        // Generate the unique name for the temporary destination.
        String name =
                        tempPrefix
                                        + destinationPrefix
                                        + SIMPConstants.SYSTEM_DESTINATION_SEPARATOR
                                        + meUuid
                                        + uniqueSuffix;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createNewTemporaryDestinationName", name);

        return name;
    }

    /**
     * @param destinationName
     */
    protected void deleteTemporaryDestination(JsDestinationAddress destAddr) throws SIDestinationLockedException, SITemporaryDestinationNotFoundException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "deleteTemporaryDestination", destAddr);

        //this is an application only function so we don't want to see invisible dests
        DestinationHandler destinationHandler;
        try
        {
            destinationHandler = getDestination(destAddr, false);
        } catch (SINotPossibleInCurrentConfigurationException e1)
        {
            // No FFDC code needed, assume the destination has already been deleted
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "deleteTemporaryDestination", "SITemporaryDestinationNotFoundException");
            throw new SIMPTemporaryDestinationNotFoundException(
                            nls.getFormattedMessage(
                                                    "TEMPORARY_DESTINATION_NAME_ERROR_CWSIP0097",
                                                    new Object[] { destAddr.getDestinationName() },
                                                    null));
        }

        /*
         * CHECK: We cannot delete temporary destinations with
         * consumers/subscribers still attached.
         */
        if (destinationHandler.isTemporary())
        {
            // PUBSUB
            if (destinationHandler.isPubSub())
            {
                if (destinationHandler.getSubscriptionIndex().getNonDurableSubscriptions() > 0)
                {
                    SIDestinationLockedException e =
                                    new SIDestinationLockedException(
                                                    nls.getFormattedMessage("TEMPORARY_DESTINATION_IN_USE_ERROR_CWSIP0052",
                                                                            new Object[] { destAddr.getDestinationName() }, null));

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.exception(tc, e);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "deleteTemporaryDestination", e);

                    throw e;
                }
            }
            // POINT TO POINT
            else
            {
                // Temporary queues always and only have a local message itemstream.
                LocalizationPoint mis = destinationHandler.getQueuePoint(messageProcessor.getMessagingEngineUuid());
                ConsumerDispatcher cd = (ConsumerDispatcher) mis.getOutputHandler();

                if (cd.hasConsumersAttached())
                {
                    SIDestinationLockedException e =
                                    new SIDestinationLockedException(
                                                    nls.getFormattedMessage("TEMPORARY_DESTINATION_IN_USE_ERROR_CWSIP0052",
                                                                            new Object[] { destAddr.getDestinationName() }, null));

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.exception(tc, e);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "deleteTemporaryDestination", e);

                    throw e;
                }
            }
        }

        try
        {
            deleteDestinationLocalization(destinationHandler.getUuid().toString(), null, null);
        } catch (SINotPossibleInCurrentConfigurationException e1)
        {
            // No FFDC code needed, assume the destination has already been deleted
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "deleteTemporaryDestination", "SITemporaryDestinationNotFoundException");
            throw new SIMPTemporaryDestinationNotFoundException(
                            nls.getFormattedMessage(
                                                    "TEMPORARY_DESTINATION_NAME_ERROR_CWSIP0097",
                                                    new Object[] { destAddr },
                                                    null));
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deleteTemporaryDestination");
    }

    /**
     * Remove the given destination from the DestinationManager.
     *
     * This will only be a BaseDestinationHandler removing either a link
     * or a destination.
     *
     * @param dh The DestinationHandler to remove.
     */
    protected void removeDestination(DestinationHandler dh)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "removeDestination", dh);

        if (dh.isLink())
        {
            if (linkIndex.containsKey(dh))
            {
                linkIndex.remove(dh);
            }
        }
        else
        {
            if (destinationIndex.containsKey(dh))
            {
                destinationIndex.remove(dh);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "removeDestination");
    }

    /**
     * Method createTransmissionDestination.
     *
     * <p>Create a transmission queue destination for the remote ME
     * uuid specified.</p>
     *
     * @param remoteMEUuid
     */
    protected void createTransmissionDestination(SIBUuid8 remoteMEUuid) throws SIResourceException, SIMPDestinationAlreadyExistsException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createTransmissionDestination", remoteMEUuid);
        String destinationName = remoteMEUuid.toString();
        DestinationDefinition destinationDefinition;

        destinationDefinition = messageProcessor.createDestinationDefinition(DestinationType.QUEUE, destinationName);
        destinationDefinition.setMaxReliability(Reliability.ASSURED_PERSISTENT);
        destinationDefinition.setDefaultReliability(Reliability.ASSURED_PERSISTENT);
        Set<String> destinationLocalizingSet = new HashSet<String>();
        destinationLocalizingSet.add(messageProcessor.getMessagingEngineUuid().toString());

        // Create the transmission destination
        createDestinationLocalization(
                                      destinationDefinition,
                                      messageProcessor.createLocalizationDefinition(destinationDefinition.getName()),
                                      destinationLocalizingSet,
                                      false);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createTransmissionDestination");
    }

    /**
     * Return the ME of this DestinationManager
     *
     * @return MessageProcessor.
     */
    public MessageProcessor getLocalME()
    {
        return messageProcessor;
    }

    /**
     * Allows the DM to be alerted to the start of reconciliation. Currently simply sets
     * the reconciling flag.
     */
    public void prepareToReconcile()
    {
        moveAllInDoubtToUnreconciled();
        reconciling = true;
    }

    /**
     * This method is used to perform local Destination reconciliation tasks
     */
    public void reconcileLocal()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "reconcileLocal");

        // Set reconciling flag to false
        reconciling = false;

        DestinationTypeFilter filter = new DestinationTypeFilter();

        filter.LOCAL = Boolean.TRUE;
        filter.UNRECONCILED = Boolean.TRUE;

        SIMPIterator itr = destinationIndex.iterator(filter);

        while (itr.hasNext())
        {
            BaseDestinationHandler dh = (BaseDestinationHandler) itr.next();

            try
            {
                // Set the deletion flag in the DH persistently. A transaction per DH??
                LocalTransaction siTran = txManager.createLocalTransaction(true);

                dh.setToBeDeleted(true);
                //Adjust the destination lookups in Destination Manager
                destinationIndex.delete(dh);
                dh.requestUpdate((Transaction) siTran);
                // commit the transaction
                siTran.commit();

                if (!dh.isTemporary() && !dh.isSystem())
                    SibTr.info(tc, "LOCAL_DEST_DELETE_INFO_CWSIP00217",
                               new Object[] { dh.getName() });
            }

            catch (MessageStoreException e)
            {
                // No FFDC code needed

                SibTr.exception(tc, e);

                //throw e;
            }

            catch (SIException e)
            {
                // No FFDC code needed

                SibTr.exception(tc, e);

                //handleRollback(siTran);
                //        throw e;
            }

        }
        itr.finished();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "reconcileLocal");
    }

    /**
     * This method is used to perform local Destination reconciliation tasks
     */
    public void reconcileMQLinks()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "reconcileMQLinks");

        // Set reconciling flag to false
        reconciling = false;

        LinkTypeFilter filter = new LinkTypeFilter();
        filter.MQLINK = Boolean.TRUE;
        filter.UNRECONCILED = Boolean.TRUE;
        SIMPIterator itr = linkIndex.iterator(filter);

        //  Get the MQLinkManager
        MQLinkManager mqlinkManager = getMQLinkManager();

        while (itr.hasNext())
        {
            MQLinkHandler dh = (MQLinkHandler) itr.next();

            // Retrieve the uuid for the Handler.
            String mqLinkUuid = dh.getMqLinkUuid().toString();

            // Call MQLink Component to alert it to create appropriate resources for
            // a link that previously existed
            MQLinkObject mqlinkObj = null;
            try
            {
                mqlinkObj = mqlinkManager.create(messageProcessor.createMQLinkDefinition(mqLinkUuid),
                                                 (MQLinkLocalization) dh,
                                                 (ControllableRegistrationService) messageProcessor.getMEInstance(SIMPConstants.JS_MBEAN_FACTORY),
                                                 true); // We've reconciled and this destination is to be deleted
            } catch (SIResourceException e)
            {
                // No FFDC code needed

                SibTr.exception(tc, e);
            } catch (SIException e)
            {
                // No FFDC code needed

                SibTr.exception(tc, e);
            }

            // Store mqlinkObj in the handler
            dh.setMQLinkObject(mqlinkObj);
            // remove from the list - we won't attempt to delete it until requested
            // by the MQLink component
            linkIndex.cleanup(dh);
            linkIndex.defer(dh);
        }
        itr.finished();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "reconcileMQLinks");
    }

    /**
     * This method is used to perform local Destination reconciliation tasks
     */
    public void reconcileLocalLinks()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "reconcileLocalLinks");

        // Set reconciling flag to false
        reconciling = false;

        LinkTypeFilter filter = new LinkTypeFilter();
        filter.LOCAL = Boolean.TRUE;
        filter.UNRECONCILED = Boolean.TRUE;
        SIMPIterator itr = linkIndex.iterator(filter);

        while (itr.hasNext())
        {
            LinkHandler dh = (LinkHandler) itr.next();

            // Mark the destination for deletion
            try
            {
                // Set the deletion flag in the DH persistently. A transaction per DH??
                LocalTransaction siTran = txManager.createLocalTransaction(true);

                dh.setToBeDeleted(true);
                //Adjust the destination lookups in Destination Manager
                linkIndex.delete(dh);
                dh.requestUpdate((Transaction) siTran);
                // commit the transaction
                siTran.commit();

                SibTr.info(tc, "LOCAL_LINK_DELETE_INFO_CWSIP0065",
                           new Object[] { dh.getName(), dh.getUuid() });
            } catch (MessageStoreException e)
            {
                // No FFDC code needed

                SibTr.exception(tc, e);

                //throw e;
            } catch (SIException e)
            {
                // No FFDC code needed

                SibTr.exception(tc, e);

                //handleRollback(siTran);
                //        throw e;
            }
        }
        itr.finished();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "reconcileLocalLinks");
    }

    /**
     * This method is used to perform remote Destination reconciliation tasks
     */
    public void reconcileRemote()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "reconcileRemote");

        DestinationTypeFilter filter = new DestinationTypeFilter();
        //filter.REMOTE = Boolean.TRUE;
        filter.UNRECONCILED = Boolean.TRUE;
        SIMPIterator itr = destinationIndex.iterator(filter);

        while (itr.hasNext())
        {
            BaseDestinationHandler dh = (BaseDestinationHandler) itr.next();

            //Dont reconcile destinations awaiting deletion
            if (!dh.isToBeDeleted())
            {
                String destName = dh.getName();
                SIBUuid12 destUuid = dh.getUuid();

                //  CallBack to Admin
                try
                {
                    // Note:  Currently alias and foreign destinations are the only destinations that might
                    // have a busname other than the local bus, but aliases are not persisted
                    // over a restart so we can always look up destinations on the local bus.
                    // Passing in null, assumes the local bus.
                    BaseDestinationDefinition dDef =
                                    messageProcessor.getMessagingEngine().getSIBDestination(null, destName);

                    if (!(dDef.getUUID().equals(dh.getUuid())))
                    {
                        //The destination has a different uuid.  Mark the existing one for deletion
                        try
                        {
                            // Set the deletion flag in the DH persistently. A transaction per DH??
                            LocalTransaction siTran = txManager.createLocalTransaction(true);

                            dh.setToBeDeleted(true);
                            destinationIndex.delete(dh);
                            dh.requestUpdate((Transaction) siTran);
                            // commit the transaction
                            siTran.commit();

                            SibTr.info(tc, "REMOTE_DEST_DELETE_INFO_CWSIP0066",
                                       new Object[] { dh.getName(), dh.getUuid() });
                        } catch (MessageStoreException me)
                        {
                            // No FFDC code needed

                            SibTr.exception(tc, me);

                            //throw e;
                        } catch (SIException ce)
                        {
                            // No FFDC code needed

                            SibTr.exception(tc, ce);

                            //handleRollback(siTran);
                            //        throw e;
                        }
                    }
                    else
                    {

                        // Passing in null, assumes the local bus.
                        Set<String> queuePointLocalisationSet =
                                        messageProcessor.getMessagingEngine().getSIBDestinationLocalitySet(null, destUuid.toString());

                        // Need to update the destination definition
                        dh.updateDefinition(dDef);

                        // Update the set of localising messaging engines for the destinationHandler.
                        // Dont create remote localisations up front.  This can be done
                        // if WLM picks one of them
                        dh.updateLocalizationSet(queuePointLocalisationSet);

                        //      Alert the lookups object to handle the re-definition
                        destinationIndex.setLocalizationFlags(dh);
                        destinationIndex.create(dh);

                        // If we have localization streams that need to be deleted for a destination
                        // that still exists, we should mark the destination for cleanup.
                        if (dh.getHasReconciledStreamsToBeDeleted())
                        {
                            destinationIndex.cleanup(dh);
                        }
                    }
                } catch (SIBExceptionDestinationNotFound e)
                {
                    // No FFDC code needed

                    SibTr.exception(tc, e);

                    // Admin could not find the destination, mark it for deletion

                    try
                    {
                        // Set the deletion flag in the DH persistently. A transaction per DH??
                        LocalTransaction siTran = txManager.createLocalTransaction(true);

                        dh.setToBeDeleted(true);
                        destinationIndex.delete(dh);
                        dh.requestUpdate((Transaction) siTran);
                        // commit the transaction
                        siTran.commit();

                        SibTr.info(tc, "REMOTE_DEST_DELETE_INFO_CWSIP0066",
                                   new Object[] { dh.getName(), dh.getUuid() });
                    } catch (MessageStoreException me)
                    {
                        // No FFDC code needed

                        SibTr.exception(tc, e);

                        //throw e;
                    } catch (SIException ce)
                    {
                        // No FFDC code needed

                        SibTr.exception(tc, e);

                        //handleRollback(siTran);
                        //        throw e;
                    }
                } catch (SIBExceptionBase e)
                {
                    // No FFDC code needed

                    SibTr.exception(tc, e);

                    // TO DO - handle this
                } catch (SIException e)
                {
                    // No FFDC code needed

                    SibTr.exception(tc, e);

                    // TO DO - handle this
                }
            }
            else
            {
                // This destination doesn't have any streams left to reconcile (they must
                // have been removed prior to shutting down the ME) but we still need to
                // get the root DestinationHandler deleted, so set the state to DELETE_PENDING
                // so that the AsynchDeletionThread picks it up and removes it (524796).
                destinationIndex.delete(dh);
            }
        }
        itr.finished();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "reconcileRemote");
    }

    /**
     * This method is used to perform remote Temp Destination reconciliation tasks
     */
    public void reconcileRemoteTemporary()
    {}

    /**
     * This method is used to perform remote Link reconciliation tasks
     */
    public void reconcileRemoteLinks()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "reconcileRemoteLinks");

        LinkTypeFilter filter = new LinkTypeFilter();
        filter.REMOTE = Boolean.TRUE;
        filter.UNRECONCILED = Boolean.TRUE;
        SIMPIterator itr = linkIndex.iterator(filter);

        while (itr.hasNext())
        {
            LinkHandler linkHandler = (LinkHandler) itr.next();
            SIBUuid12 linkUuid = linkHandler.getUuid();
            ForeignBusDefinition foreignBusDefinition;

            //  CallBack to Admin
            try
            {
                // Get the foreign bus definition
                JsBus jsBus = (JsBus) messageProcessor.getMessagingEngine().getBus();
                foreignBusDefinition = jsBus.getForeignBusForLink(linkUuid.toString());

                //Check the link exists
                if (foreignBusDefinition != null)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "Retrieved foreignBusDefinition " + foreignBusDefinition.getName());
                    VirtualLinkDefinition vld = foreignBusDefinition.getLinkForNextHop();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "Retrieved link for next hop " + vld.getName());
                }
            } catch (SIBExceptionNoLinkExists e)
            {
                // No FFDC code needed
                //This is ok.  It means the link has been deleted from WCCM
                SibTr.exception(tc, e);
                foreignBusDefinition = null;
            } catch (SIBExceptionBusNotFound e)
            {
                // No FFDC code needed
                //This is ok.  It means the next hop bus has been deleted from WCCM
                SibTr.exception(tc, e);
                foreignBusDefinition = null;
            }

            if (foreignBusDefinition == null)
            {
                // Admin could not find the bus, mark the link for deletion

                LocalTransaction siTran = txManager.createLocalTransaction(true);
                try
                {
                    // Set the deletion flag in the DH persistently. A transaction per DH??
                    linkHandler.setToBeDeleted(true);
                    linkHandler.requestUpdate((Transaction) siTran);
                    // commit the transaction
                    siTran.commit();

                    SibTr.info(tc, "REMOTE_LINK_DELETE_INFO_CWSIP0067",
                               new Object[] { linkHandler.getName(), linkHandler.getUuid() });

                    linkIndex.delete(linkHandler);
                } catch (MessageStoreException me)
                {
                    // MessageStoreException shouldn't occur so FFDC.
                    FFDCFilter.processException(me,
                                                "com.ibm.ws.sib.processor.impl.DestinationManager.reconcileRemoteLinks", "1:2839:1.508.1.7", this);

                    SibTr.exception(tc, me);
                } catch (SIException ce)
                {
                    // MessageStoreException shouldn't occur so FFDC.
                    FFDCFilter.processException(
                                                ce,
                                                "com.ibm.ws.sib.processor.impl.DestinationManager.reconcileRemoteLinks",
                                                "1:2849:1.508.1.7",
                                                this);

                    SibTr.exception(tc, ce);

                    handleRollback(siTran);
                }
            }
            else
            {
                try
                {
                    //For inter-bus links, we route the messages to the inbound ME uuid
                    //advertised in WLM.  For MQ links we send to the outbound ME uuid.
                    //We cant tell from the linkHandler what type it is.  The way we
                    //decide is if the inbound ME uuid advertised in WLM is NULL then
                    //its an MQ link
                    LinkSelection s = null;
                    LinkManager linkManager = messageProcessor.getLinkManager();
                    try
                    {
                        s = linkManager.select(linkHandler.getUuid());
                    } catch (LinkException e)
                    {
                        // No FFDC code needed
                        SibTr.exception(tc, e);
                        throw new SIResourceException(e);
                    }

                    SIBUuid8 localisingME = null;
                    SIBUuid8 routingME = null;
                    if (s != null)
                    {
                        localisingME = s.getInboundMeUuid();
                        routingME = s.getOutboundMeUuid();

                        //If the inbound ME uuid is null, then assume its an MQ link
                        //and use the outbound ME uuid
                        if (localisingME == null)
                        {
                            localisingME = routingME;
                        }
                    }
                    else
                    {
                        localisingME = new SIBUuid8(SIMPConstants.UNKNOWN_UUID);
                        routingME = null;
                    }

                    linkHandler.updateLocalisationSet(localisingME, routingME);

                    linkIndex.create(linkHandler);
                } catch (SIException e)
                {
                    // SIException should not occur
                    FFDCFilter.processException(
                                                e,
                                                "com.ibm.ws.sib.processor.impl.DestinationManager.reconcileRemoteLinks",
                                                "1:2909:1.508.1.7",
                                                this);

                    SibTr.exception(tc, e);

                    // We`ve logged the error.  Now put the link into corrupt state.  It will
                    // stay in this state until the next reconcile, unless its manually reset.
                    linkIndex.corrupt(linkHandler);
                }
            }
        }
        itr.finished();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "reconcileRemoteLinks");
    }

    /**
     * <p>This method is used to delete a destination that is localised on this
     * ME. This could be an entire delete of the destination, or it could be
     * that this ME is no longer a localiser for the destination.
     * <p>If the entire destination is being deleted,
     * destinationDefinition should be null,
     * otherwise destinationDefinition should contain the set of ME's that do
     * still localise the destination.
     *
     * @param destinationUuid Uuid of the destination on which the delete
     *            operation will act.
     *
     * @param destinationDefinition null if the entire destination is to be
     *            deleted.
     *
     * @param queuePointLocalizingMEs Set of MEs which host the queue point.
     *
     *
     * @param isLink delete this link localisation
     */
    public void deleteDestinationLocalization(
                                              String destinationUuid,
                                              DestinationDefinition destinationDefinition,
                                              Set<String> queuePointLocalizingMEs) throws SINotPossibleInCurrentConfigurationException, SITemporaryDestinationNotFoundException, SIResourceException
    {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "deleteDestinationLocalization",
                        new Object[] { destinationUuid, destinationDefinition, queuePointLocalizingMEs });

        //The Uuid passed in should never be null, but rather than checking for null, we
        //just accept that a NullPointerException will be thrown.  If null is passed in,
        //its a problem in the admin component and not something the user can fix.

        BaseDestinationHandler destinationHandler = null;

        //The destination is being deleted on the local messaging engine.
        String messagingEngineName = messageProcessor.getMessagingEngineName();
        //SIBUuid messagingEngineUuid = messageProcessor.getMessagingEngineUuid();
        SIBUuid12 destUuid = new SIBUuid12(destinationUuid);

        DestinationTypeFilter filter = new DestinationTypeFilter();
        filter.ALIAS = Boolean.FALSE;
        filter.FOREIGN_DESTINATION = Boolean.FALSE;

        DestinationHandler dh = destinationIndex.findByUuid(destUuid, filter);

        checkDestinationHandlerExists(
                                      dh != null,
                                      destinationUuid,
                                      messageProcessor.getMessagingEngineName());

        destinationHandler = (BaseDestinationHandler) dh;
        destinationHandler.getControlAdapter().deregisterControlAdapterMBean(); // to test
        // The lock order in this method is as follows:
        // 1) Take the lock on the destinationManager is taken to stop 2 threads creating the same
        //    destination and also to synchronize dynamic deletes with the
        //    creation of aliases.  This stops an alias destination being created that targets a
        //    destination in the process of being deleted.
        // 2) Lock the reallocation lock manager - see defect 331733
        // 3) Lock the destinationHandler
        //
        // We cannot call close on a producer while we have (3), because
        // the lock heirarchy requires the lock on a producer/consumer is taken before
        // the reallocation lock.
        // To reconcile this we take the following approach.
        // - Obtain all the locks, determine the deletion logic required, and mark toBeDeleted.
        // - Release all the locks and close the producers/consumers.
        // - Reestablish all the locks and complete the deletion logic.

        // Take locks to determine the deletion logic required
        boolean isDeletingEntireDestination;
        synchronized (this) // Lock1
        {
            // Lock2
            synchronized (destinationHandler.getMediationConsumerLockObject())
            {
                LockManager reallocationLock = destinationHandler.getReallocationLockManager();
                if (!destinationHandler.isPubSub())
                    reallocationLock.lockExclusive();

                // Issue a try finally to ensure that lock manager is unlocked
                try
                {
                    synchronized (destinationHandler) // Lock4
                    {
                        // Check the destination still exists.
                        // This includes a check on the toBeDeleted flag, which is set in this
                        // block before we release any locks.
                        checkDestinationHandlerExists(
                                                      !(destinationHandler.isToBeDeleted()
                                                      || destinationHandler.isSystem()),
                                                      destinationHandler.getName(),
                                                      messageProcessor.getMessagingEngineName());

                        boolean destinationDefiniedLocally = destinationHandler.hasLocal();

                        //only allow this delete if the destination is defined locally or if the destination
                        //is a service
                        if (!destinationDefiniedLocally && !(destinationHandler.getDestinationType() == DestinationType.SERVICE))
                        {
                            /*
                             * The destination is not defined locally. Throw an
                             * exception.
                             */
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                SibTr.exit(
                                           tc,
                                           "deleteDestinationLocalization",
                                           "Destination not known " + destinationHandler.getName());

                            if (destinationHandler.isTemporary())
                            {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                    SibTr.exit(tc, "deleteDestinationLocalization", "SITemporaryDestinationNotFoundException");
                                throw new SIMPTemporaryDestinationNotFoundException(nls.getFormattedMessage(
                                                                                                            "DESTINATION_INSTANCE_NOT_FOUND_ERROR_CWSIP0042",
                                                                                                            new Object[] { destinationHandler.getName(), messagingEngineName },
                                                                                                            null));
                            }

                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                SibTr.exit(tc, "deleteDestinationLocalization", "SINotPossibleInCurrentConfigurationException");
                            throw new SINotPossibleInCurrentConfigurationException(
                                            nls.getFormattedMessage(
                                                                    "DESTINATION_INSTANCE_NOT_FOUND_ERROR_CWSIP0042",
                                                                    new Object[] { destinationHandler.getName(), messagingEngineName },
                                                                    null));
                        }

                        // If the destinationDefinition is null, then the entire destination
                        // is being deleted, otherwise only this localisation of the
                        // destination is being deleted.
                        isDeletingEntireDestination =
                                        (destinationDefinition == null)
                                                        || (queuePointLocalizingMEs == null)
                                                        || (queuePointLocalizingMEs.size() == 0);

                        // If we are deleting the whole destination, then we mark
                        // the destination handler to be deleted, preventing any
                        // other thread from duplicating our work.
                        // This prevents consumers from attaching (attachConsumerPoint in ConsumerManager)
                        // However, it does not prevent producers from attaching. See comments below
                        if (isDeletingEntireDestination)
                        {
                            // We need to ensure that if the cleanupBaseDestintaion method
                            // is active on the async deletion thread, it does not perform the
                            // delete of our msgstore data until this method is complete.
                            destinationHandler.setDeleteInProgress(true);
                            destinationHandler.setToBeDeleted(true);
                        }
                    } // Lock4
                } finally
                {
                    // Unlock the reallocation LM
                    if (!destinationHandler.isPubSub())
                        reallocationLock.unlockExclusive(); // Lock3
                }
            } // Lock 2
        } // Lock1

        // If we have determined that the whole destination is to be deleted,
        // then we need to close all active producers and consumers before
        // we make the destination eligible for deletion by the asynch deletion thread.
        if (isDeletingEntireDestination)
        {
            // The logic inside closeProducers has synchronized logic that ensures no
            // new producers can attach after this call completes (producers do not check isToBeDeleted)
            destinationHandler.closeProducers();
            // The list of consumers cannot change after the destination handler is marked
            // to be deleted (note this flag has been made volatile).
            destinationHandler.closeConsumers();
        }

        // Reestablish the locks to complete the deletion logic
        synchronized (this) // Lock1
        {
            // Lock2
            LockManager reallocationLock = destinationHandler.getReallocationLockManager();
            if (!destinationHandler.isPubSub())
                reallocationLock.lockExclusive();

            // Issue a try finally to ensure that lock manager is unlocked
            try
            {
                synchronized (destinationHandler) // Lock4
                {
                    // Complete the required deletion logic
                    if (isDeletingEntireDestination)
                    {
                        // Set the deletion flag in the DH persistently
                        // (we marked toBeDeleted nonpersistently above)
                        ExternalAutoCommitTransaction transaction = txManager.createAutoCommitTransaction();

                        if (!dh.isTemporary() && !dh.isSystem())
                            SibTr.info(tc, "LOCAL_DEST_DELETE_INFO_CWSIP00217",
                                       new Object[] { dh.getName() });

                        // Change the state of the destination to make it eligible for
                        // deletion, but do not allow the async deletion thread to pick
                        // it up until our processing is complete (and we have released our locks)
                        destinationIndex.delete(destinationHandler);
                        try
                        {
                            destinationHandler.requestUpdate(transaction);
                        } catch (MessageStoreException e)
                        {
                            // MessageStoreException shouldn't occur so FFDC.
                            FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ws.sib.processor.impl.DestinationManager.deleteDestinationLocalization",
                                                        "1:3152:1.508.1.7",
                                                        this);

                            SibTr.exception(tc, e);
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                SibTr.exit(tc, "deleteDestinationLocalization", "SIResourceException");
                            throw new SIResourceException(e);
                        }

                        //Add all the localisations to the set of those requiring clean-up
                        destinationHandler.addAllLocalisationsForCleanUp();

                        //Clear the set of ME's that localise the destination
                        destinationHandler.clearLocalisingUuidsSet();

                        //Delete all aliases that target this destination
                        destinationHandler.deleteTargettingAliases();
                    }
                    else
                    {
                        // Only this localisation being deleted.  There should be other
                        // localisations for the destination that are still available.
                        destinationHandler.updateLocalizationSet(
                                        queuePointLocalizingMEs);

                        //      Alert the lookups object to handle the re-definition
                        destinationIndex.setLocalizationFlags(destinationHandler);

                        // The attributes of the destinationDefinition may also have
                        // changed.  These need to be updated.
                        destinationHandler.updateDefinition(destinationDefinition);

                        destinationIndex.cleanup(destinationHandler);
                    }
                    // PM71953 - deregister destination from WLM before actual destination cleanup. When there
                    // are frequent destination deletion and recreation (with the same name), it is possible that
                    // the recreated destination gets deregistered from WLM instead of the destination that has
                    // been marked for deletion, this is because WLM registers/deregisters destination by destination
                    // name and not by UUID.
                    //
                    // The deregister code during cleanup destination task (by AsynchDeletionThread) is not removed
                    // in this APAR due to other dependencies. So there are two attempts to deregister the same
                    // destination. However, once deregistered, further deregister attempts for the same destination
                    // will be safely ignored.
                    destinationHandler.deregisterDestination();
                } // Lock4
            } finally
            {
                // Unlock the reallocation LM
                if (!destinationHandler.isPubSub())
                    reallocationLock.unlockExclusive(); // Lock3
            }
        } // Lock1

        // Make the destination eligible for deletion, then poke the asynchronous
        // deletion thread. We only use the destination handler lock here
        // (sync'd method), to avoid any interactions with locking that might
        // be performed on the asynchronous deletion thread.
        if (isDeletingEntireDestination)
        {
            destinationHandler.setDeleteInProgress(false);

            // Now start the asynch deletion thread
            startAsynchDeletion();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deleteDestinationLocalization");

        return;
    }

    /**
     * <p>This method is used to delete an Link destination that is
     * localised on this ME.
     *
     * @param linkUuid
     * @throws SINotPossibleInCurrentConfigurationException
     * @throws SIResourceException
     */
    public void deleteLinkLocalization(SIBUuid12 linkUuid)
                    throws SINotPossibleInCurrentConfigurationException,
                    SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "deleteLinkLocalization",
                        new Object[] { linkUuid });

        // Get the destination
        LinkTypeFilter filter = new LinkTypeFilter();
        filter.MQLINK = Boolean.FALSE;

        // includeInvisible?
        // filter.VISIBLE = Boolean.TRUE;

        LinkHandler linkHandler =
                        (LinkHandler) linkIndex.findByUuid(linkUuid, filter);

        // Check that we have a handler for this link.
        checkLinkExists(
                        linkHandler != null,
                        linkUuid.toString()); // Only have the Uuid to hand.

        //The lock on the destinationManager is taken to stop 2 threads creating the same
        //destination and also to synchronize dynamic deletes with the
        //creation of aliases.  This stops an alias destination being created that targets a
        //destination in the process of being deleted.
        synchronized (this)
        {
            synchronized (linkHandler)
            {
                // Mark the destination for deletion
                try
                {
                    // Set the deletion flag in the DH persistently. A transaction per DH??
                    LocalTransaction siTran = txManager.createLocalTransaction(true);

                    linkHandler.setToBeDeleted(true);
                    //Adjust the destination lookups in Destination Manager
                    linkIndex.delete(linkHandler);
                    linkHandler.requestUpdate((Transaction) siTran);
                    // commit the transaction
                    siTran.commit();

                    SibTr.info(tc, "LOCAL_LINK_DELETE_INFO_CWSIP0065",
                               new Object[] { linkHandler.getName(), linkHandler.getUuid() });
                } catch (MessageStoreException e)
                {
                    // MessageStoreException shouldn't occur so FFDC.
                    FFDCFilter.processException(
                                                e,
                                                "com.ibm.ws.sib.processor.impl.DestinationManager.deleteLinkLocalization",
                                                "1:3285:1.508.1.7",
                                                this);

                    SibTr.exception(tc, e);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "deleteLinkLocalization", "SIResourceException");
                    throw new SIResourceException(e);
                } catch (SIException e)
                {
                    // No FFDC code needed

                    SibTr.exception(tc, e);

                    //handleRollback(siTran);
                    //        throw e;
                }

                //Add all the localisations to the set of those requiring clean-up
                linkHandler.addAllLocalisationsForCleanUp();

                //Clear the set of ME's that localise the destination
                linkHandler.clearLocalisingUuidsSet();

            } // eof synchronized on linkHandler

            startAsynchDeletion();

            // Close all connections to the localisation
            // Ensure that before calling these methods, the destination is marked
            // to-be-deleted or is marked as deleted, to avoid any new producers and
            // consumers attaching after we have closed the currently attached ones.
            linkHandler.closeProducers();
            linkHandler.closeConsumers();

            //Delete all aliases that target this destination
            linkHandler.deleteTargettingAliases();

        } // eof synchronized on DestinationManager

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deleteLinkLocalization");

        return;
    }

    /**
     * <p>This method is used to alter a link that is localised on this ME.</p>
     *
     * @param vld
     * @param linkUuid
     * @throws SINotPossibleInCurrentConfigurationException
     * @throws SIResourceException
     * @throws SIConnectionLostException
     * @throws SIException
     * @throws SIBExceptionBase
     */
    public void alterLinkLocalization(VirtualLinkDefinition vld,
                                      SIBUuid12 linkUuid)
                    throws SINotPossibleInCurrentConfigurationException,
                    SIResourceException,
                    SIConnectionLostException,
                    SIException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "alterLinkLocalization",
                        new Object[] { vld, linkUuid });

        // Get the destination
        LinkTypeFilter filter = new LinkTypeFilter();
        filter.MQLINK = Boolean.FALSE;

        // includeInvisible?
        // filter.VISIBLE = Boolean.TRUE;

        LinkHandler linkHandler =
                        (LinkHandler) linkIndex.findByUuid(linkUuid, filter);

        // Check that we have a handler for this link.
        checkLinkExists(
                        linkHandler != null,
                        vld.getName());

        // Synchronize on the linkHandler object
        synchronized (linkHandler)
        {
            // We'll do this under a tranaction, in the spirit of the code that
            // alters destination information. This work is not coordinated with
            // the Admin component.
            // Create a local UOW
            LocalTransaction transaction = txManager.createLocalTransaction(true);
            // Try to alter the local link.
            try
            {
                // Update the virtual linkdefinition
                linkHandler.updateLinkDefinition(vld, transaction);

                // Alert the lookups object to handle the re-definition
                linkIndex.create(linkHandler);

                // If the update was successful then commit the unit of work
                transaction.commit();
            } catch (SIResourceException e)
            {
                // No FFDC code needed
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "alterLinkLocalization", e);

                handleRollback(transaction);

                throw e;
            } catch (RuntimeException e)
            {
                // FFDC
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.DestinationManager.alterLinkLocalization",
                                            "1:3407:1.508.1.7",
                                            this);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                {
                    SibTr.exception(tc, e);
                    SibTr.exit(tc, "alterLinkLocalization", e);
                }

                handleRollback(transaction);

                throw e;
            }
        } // eof synchronized on linkHandler

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "alterLinkLocalization");
    }

    /**
     * <p>This method is used to delete an MQLink destination that is
     * localised on this ME.
     *
     * @param mqLinkUuid
     * @throws SINotPossibleInCurrentConfigurationException
     * @throws SIResourceException
     */
    public void deleteMQLinkLocalization(SIBUuid8 mqLinkUuid)
                    throws SINotPossibleInCurrentConfigurationException,
                    SIResourceException
    {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "deleteMQLinkLocalization",
                        new Object[] { mqLinkUuid });

        // Get the destination
        LinkTypeFilter filter = new LinkTypeFilter();
        filter.MQLINK = Boolean.TRUE;

        // includeInvisible?
        // filter.VISIBLE = Boolean.TRUE;

        MQLinkHandler mqLinkHandler =
                        (MQLinkHandler) linkIndex.findByMQLinkUuid(mqLinkUuid, filter);

        checkMQLinkExists(
                          mqLinkHandler != null,
                          mqLinkUuid.toString());

        //The lock on the destinationManager is taken to stop 2 threads creating the same
        //destination and also to synchronize dynamic deletes with the
        //creation of aliases.  This stops an alias destination being created that targets a
        //destination in the process of being deleted.
        synchronized (this)
        {

            synchronized (mqLinkHandler)
            {
                //  Get the MQLinkManager
                MQLinkManager mqlinkManager = getMQLinkManager();

                // remove from the list - we won't attempt to delete it until requested
                // by the MQLink component
                linkIndex.cleanup(mqLinkHandler);
                linkIndex.defer(mqLinkHandler);

                // Call MQLink component in order to alert it to the deletion
                // MP will be called back to delete its resources.
                try
                {
                    mqlinkManager.delete(mqLinkHandler.getMQLinkObject());
                } catch (SIResourceException e)
                {
                    // No FFDC code needed

                    SibTr.exception(tc, e);
                } catch (SIException e)
                {
                    // No FFDC code needed

                    SibTr.exception(tc, e);
                }

                //Add all the localisations to the set of those requiring clean-up
                mqLinkHandler.addAllLocalisationsForCleanUp();

                //Clear the set of ME's that localise the destination
                mqLinkHandler.clearLocalisingUuidsSet();

                // Deregister the link from TRM/WLM
                mqLinkHandler.deregisterLink();

            } // eof syncronized on mqLinkHandler

            // Close all connections to the localisation
            // Ensure that before calling these methods, the destination is marked
            // to-be-deleted or is marked as deleted, to avoid any new producers and
            // consumers attaching after we have closed the currently attached ones.
            mqLinkHandler.closeProducers();
            mqLinkHandler.closeConsumers();

            //Delete all aliases that target this destination
            mqLinkHandler.deleteTargettingAliases();
        } // eof synchronized on DestinationManager

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deleteMQLinkLocalization");

        return;
    }

    /**
     * <p>This method is used to alter an MQLink that is localised on this ME.</p>
     *
     * @param vld
     * @param uuid
     * @param ld
     * @throws SINotPossibleInCurrentConfigurationException
     * @throws SIResourceException
     * @throws SIConnectionLostException
     * @throws SIException
     * @throws SIBExceptionBase
     */
    public void alterMQLinkLocalization(MQLinkDefinition mqld,
                                        LocalizationDefinition ld,
                                        VirtualLinkDefinition vld)
                    throws SINotPossibleInCurrentConfigurationException,
                    SIResourceException,
                    SIConnectionLostException,
                    SIException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "alterMQLinkLocalization",
                        new Object[] { vld, mqld, ld });

        // Get the destination
        LinkTypeFilter filter = new LinkTypeFilter();
        filter.MQLINK = Boolean.TRUE;

        // includeInvisible?
        // filter.VISIBLE = Boolean.TRUE;

        MQLinkHandler mqLinkHandler =
                        (MQLinkHandler) linkIndex.findByMQLinkUuid(mqld.getUuid(), filter);

        checkMQLinkExists(
                          mqLinkHandler != null,
                          vld.getName());

        synchronized (mqLinkHandler)
        {
            //  Get the wrapped mqlinkobject
            MQLinkObject mqlinkObject = mqLinkHandler.getMQLinkObject();

            // Call MQLink Component to propagate the update request
            mqlinkObject.update(mqld);

            // Now we look at those changes that MP needs to act upon.
            // We'll do this under a tranaction, in the spirit of the code that
            // alters destination information. This work is neither coordinated with
            // the MQLink component nor the Admin component.
            // Create a local UOW
            LocalTransaction transaction = txManager.createLocalTransaction(true);
            // Try to alter the local destination.
            try
            {
                // Update the virtual linkdefinition
                mqLinkHandler.updateLinkDefinition(vld, transaction);
                // Drive the update to the localization definition.
                if (ld != null)
                {
                    // Update the localisation definition too.
                    mqLinkHandler.updateLocalizationDefinition(ld, transaction);
                }

                // Alert the lookups object to handle the re-definition
                linkIndex.create(mqLinkHandler);

                // If the update was successful then commit the unit of work
                transaction.commit();
            } catch (SIResourceException e)
            {
                // No FFDC code needed
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "alterMQLinkLocalization", e);

                handleRollback(transaction);

                throw e;
            } catch (RuntimeException e)
            {
                // FFDC
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.DestinationManager.alterMQLinkLocalization",
                                            "1:3611:1.508.1.7",
                                            this);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                {
                    SibTr.exception(tc, e);
                    SibTr.exit(tc, "alterMQLinkLocalization", e);
                }

                handleRollback(transaction);

                throw e;
            }
        } // eof synchronized on mqLinkHandler

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "alterMQLinkLocalization");
    }

    /**
     * <p>This method is used to create a destination that is localised on this ME. The
     * destinationDefinition passed in may also include the uuid's of other ME's which also
     * localise the destination. If this is the case, the infrastructure to be able to send
     * messages to these ME's will also be configured.</p>
     *
     * @param destinationLocalizationDefinition
     * @param destinationDefinition
     * @param destinationLocalizingMEs
     */
    public void createDestinationLocalization(
                                              DestinationDefinition destinationDefinition,
                                              LocalizationDefinition destinationLocalizationDefinition,
                                              Set<String> destinationLocalizingMEs,
                                              boolean isTemporary) throws SIResourceException, SIMPDestinationAlreadyExistsException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "createDestinationLocalization",
                        new Object[] {
                                      destinationDefinition,
                                      destinationLocalizationDefinition,
                                      destinationLocalizingMEs,
                                      new Boolean(isTemporary) });

        // Create a local UOW
        LocalTransaction transaction = txManager.createLocalTransaction(true);

        // Try to create the local destination.
        try
        {
            createDestinationLocalization(
                                          destinationDefinition,
                                          destinationLocalizationDefinition,
                                          destinationLocalizingMEs,
                                          isTemporary,
                                          transaction);

            // If everything was successful then commit the unit of work
            transaction.commit();

            // Call the destinationListeners
            callDestinationListener(destinationDefinition.getName());
        } catch (SIIncorrectCallException e)
        {
            // No FFDC code needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                SibTr.exception(tc, e);
                SibTr.exit(tc, "createDestinationLocalization", e);
            }

            handleRollback(transaction);

            throw new SIResourceException(e);

        } catch (RuntimeException e)
        {
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.DestinationManager.createDestinationLocalization",
                                        "1:3733:1.508.1.7",
                                        this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                SibTr.exception(tc, e);
                SibTr.exit(tc, "createDestinationLocalization", e);
            }

            handleRollback(transaction);

            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createDestinationLocalization");
    }

    /**
     * <p>This method is used to create a destination that is localised on this ME.</p>
     *
     * @param destinationLocalizationDefinition
     * @param destinationDefinition
     * @param destinationLocalizingMEs
     * @param transaction
     */
    private void createDestinationLocalization(
                                               DestinationDefinition destinationDefinition,
                                               LocalizationDefinition destinationLocalizationDefinition,
                                               Set<String> destinationLocalizingMEs,
                                               boolean isTemporary,
                                               LocalTransaction transaction) throws SIResourceException, SIMPDestinationAlreadyExistsException
    {
//    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
//      SibTr.entry(
//        tc,
//        "createDestinationLocalization",
//        new Object[] {
//          destinationDefinition.getName(),
//          destinationDefinition.getUUID().toString(),
//          destinationDefinition,
//          destinationLocalizationDefinition,
//          destinationLocalizingMEs,
//          new Boolean(isTemporary),
//          transaction });

        boolean destinationHandlerCreated = false;
        BaseDestinationHandler destinationHandler = null;

        //The destination is being created on the local messaging engine.
        SIBUuid8 messagingEngineUuid = messageProcessor.getMessagingEngineUuid();

        checkValidLocalizationConfig(destinationDefinition,
                                     messagingEngineUuid,
                                     destinationLocalizationDefinition,
                                     destinationLocalizingMEs);

        //The lock on the destinationManager is taken to stop 2 threads creating the same
        //destination and also to synchronize dynamic deletes with the
        //creation of aliases.  This stops an alias destination being created that targets a
        //destination in the process of being deleted.
        synchronized (this)
        {
            //check if there are ANY with the same uuid
            DestinationTypeFilter filter = new DestinationTypeFilter();
            //Venu UUID temp
            DestinationHandler dh = destinationIndex.findByName(destinationDefinition.getName(), messageProcessor.getMessagingEngineBus(), filter);

            // If someone does a delete and add then it is possible the delete
            // might not have happened yet, so wait up to 5 seconds for the
            // deletion thread to finish. Then get the DH again and proceed. If
            // deletion takes more than 5 seconds this still won't work, but
            // deletion should not take 5 seconds.
            if (dh != null && dh.isToBeDeleted()) {
                synchronized (deletionThreadLock) {
                  try {
                      if (asynchDeletionThread != null && asynchDeletionThread.isRunning()) {
                          deletionThreadLock.wait(5000);
                      }
                  } catch (InterruptedException ie) {
                    // Just ignore this, someone told us to wake up so just bail.
                    Thread.currentThread().interrupt();
                    SibTr.debug(tc, "Wait for deletion thread to complete interrupted. Continuing without further waiting");
                  }
                }
                dh = destinationIndex.findByName(destinationDefinition.getName(), messageProcessor.getMessagingEngineBus(), filter);
            }

            if (dh == null)
            {
                // Create a new DestinationHandler, which is created locked
                // 174199.2.3
                destinationHandler =
                                new BaseDestinationHandler(
                                                destinationDefinition,
                                                messageProcessor,
                                                this,
                                                transaction,
                                                durableSubscriptions,
                                                messageProcessor.getMessagingEngineBus());

                destinationHandlerCreated = true;
                DestinationIndex.Type type = new DestinationIndex.Type();
                type.alias = new Boolean(destinationHandler.isAlias());
                type.foreignDestination = new Boolean(destinationHandler.isForeign());
                type.local = new Boolean(destinationHandler.hasLocal());
                type.queue = new Boolean(!destinationHandler.isPubSub());
                type.remote = new Boolean(destinationHandler.hasRemote());
                type.state = State.CREATE_IN_PROGRESS;
                //Venu temp UUID. setting UUID freshly
                destinationDefinition.setUUID(new SIBUuid12());
                destinationLocalizationDefinition.setUUID(new SIBUuid8());
                destinationIndex.put(destinationHandler, type);

            }
            else
            {
                if (dh instanceof BaseDestinationHandler) {
                    destinationHandler = (BaseDestinationHandler) dh;
                    //Venu temp UUID. setting Destination UUID from MessageStore dh
                    // LocalisationDefitnion UUID is alwasy set freshly
                    destinationDefinition.setUUID(dh.getDefinition().getUUID());
                    destinationLocalizationDefinition.setUUID(new SIBUuid8());
                }
                else
                {
                    //A destination exists with this name - perhaps an alias or a
                    //foreign destination.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(
                                   tc,
                                   "createDestinationLocalization",
                                   nls_cwsik.getFormattedMessage(
                                                                 "DELIVERY_ERROR_SIRC_31", // DESTINATION_ALREADY_EXISTS_ERROR_CWSIP0045
                                                                 new Object[] { destinationDefinition.getName() },
                                                                 null));

                    SIMPDestinationAlreadyExistsException e = new SIMPDestinationAlreadyExistsException(
                                    nls_cwsik.getFormattedMessage(
                                                                  "DELIVERY_ERROR_SIRC_31", // DESTINATION_ALREADY_EXISTS_ERROR_CWSIP0045
                                                                  new Object[] { destinationDefinition.getName() },
                                                                  null));

                    e.setExceptionReason(SIRCConstants.SIRC0031_DESTINATION_ALREADY_EXISTS_ERROR);
                    e.setExceptionInserts(new String[] { destinationDefinition.getName() });
                    throw e;
                }
            }
        }

        synchronized (destinationHandler)
        {
            //      // If we are corrupt, we don't want to attempt to create any points
            //      // whatsoever.
            //      if (destinationHandler.isCorrupt())
            //      {
            //        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            //          SibTr.debug(tc, "createDestinationLocalization",
            //            "Point " + destinationLocalizationDefinition.getIdentifier()
            //            + " not created since destination " + destinationHandler.getName()
            //            + " is corrupt.");
            //
            //        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            //          SibTr.exit(tc, "createDestinationLocalization");
            //
            //        return;
            //      }

            //If we didnt just create the destinationHandler, it is valid for this
            //create call to be made at startup or to create a local queue point
            if (!destinationHandlerCreated)
            {
                //---199538--------------------------------------------------------------
                //This USED to test whether the destination was reconciling
                //and, if not, would throw an exception. This is no longer the case, as
                //dynamic updates might cause this method to be called for a destination
                //that already exists i.e. when localising a qPt on a previously
                //remote destination.
                //-----------------------------------------------------------------------

                if (destinationHandler.isReconciled() && (destinationHandler.hasLocal() || destinationLocalizationDefinition == null))
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(
                                   tc,
                                   "createDestinationLocalization",
                                   nls_cwsik.getFormattedMessage(
                                                                 "DELIVERY_ERROR_SIRC_31", // DESTINATION_ALREADY_EXISTS_ERROR_CWSIP0045
                                                                 new Object[] { destinationDefinition.getName() },
                                                                 null));

                    SIMPDestinationAlreadyExistsException e = new SIMPDestinationAlreadyExistsException(
                                    nls_cwsik.getFormattedMessage(
                                                                  "DELIVERY_ERROR_SIRC_31", // DESTINATION_ALREADY_EXISTS_ERROR_CWSIP0045
                                                                  new Object[] { destinationDefinition.getName() },
                                                                  null));

                    e.setExceptionReason(SIRCConstants.SIRC0031_DESTINATION_ALREADY_EXISTS_ERROR);
                    e.setExceptionInserts(new String[] { destinationDefinition.getName() });
                    throw e;
                }

                // Only update destination handler objects with WCCM information if
                // we have not already detected them as corrupt.
                if (!destinationHandler.isCorruptOrIndoubt())
                {
                    // Destination already exists.  In the new WCCM world, the
                    // create should complete succesfully, with the new destination
                    // definition replacing the existing one.
                    // We update the localization definitions before we update the
                    // actual destination definition.

                    if (isQueue(destinationHandler.getDestinationType()))
                    {
                        if (destinationLocalizationDefinition != null)
                        {
                            // Update the localisation definition too.
                            destinationHandler.updateLocalizationDefinition(destinationLocalizationDefinition, transaction);
                        }
                    } // eof is q
                    else if (destinationHandler.getDestinationType() == DestinationType.TOPICSPACE)
                    {
                        destinationHandler.updateLocalizationDefinition(destinationLocalizationDefinition, transaction);
                    } // eof is topicspace

                    destinationHandler.updateDefinition(destinationDefinition);

                    // Update the set of localising messaging engines for the destinationHandler
                    // Dont create remote localisations up front.  This can be done
                    // if WLM picks one of them
                    destinationHandler.updateLocalizationSet(
                                    destinationLocalizingMEs);

                    // Alert the lookups object to handle the re-definition
                    DestinationIndex.Type type = new DestinationIndex.Type();
                    type.alias = new Boolean(destinationHandler.isAlias());
                    type.foreignDestination = new Boolean(destinationHandler.isForeign());
                    type.local = new Boolean(destinationHandler.hasLocal());
                    type.queue = new Boolean(!destinationHandler.isPubSub());
                    type.remote = new Boolean(destinationHandler.hasRemote());
                    type.state = destinationIndex.getState(destinationHandler);
                    destinationIndex.setType(destinationHandler, type);
                    destinationIndex.create(destinationHandler);

                    //Show that the destination is now reconciled
                    destinationHandler.setReconciled();

                    if (destinationHandler.getHasReconciledStreamsToBeDeleted())
                    {
                        //defect 259817
                        //there was some unfinished cleanup from before the ME previously shutdown
                        //that we now need to take care of.
                        destinationIndex.cleanup(destinationHandler);
                    }
                }
            }
            else // we did just create the destinationHandler.
            {
                if (isQueue(destinationHandler.getDestinationType()))
                {
                    if (destinationLocalizationDefinition != null)
                    {
                        // Create a new point to point localisation
                        destinationHandler.addNewPtoPLocalization(
                                                                  false,
                                                                  transaction,
                                                                  messagingEngineUuid,
                                                                  destinationLocalizationDefinition,
                                                                  true);
                    }
                } // eof is q
                else if (destinationHandler.getDestinationType() == DestinationType.TOPICSPACE)
                {
                    destinationHandler.addPubSubLocalisation(destinationLocalizationDefinition);
                } // eof is topicspace

                // Update the set of localising messaging engines for the destinationHandler
                // Dont create remote localisations up front.  This can be done
                // if WLM picks one of them
                destinationHandler.updateLocalizationSet(
                                destinationLocalizingMEs);

                // Put the newly created destination handler into the indexes
                destinationIndex.setLocalizationFlags(destinationHandler);

            } // eof we did just create the DH
        } // end of synchronization

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createDestinationLocalization");
    }

    /**
     * @param destinationDefinition
     * @param messagingEngineUuid
     * @param destinationLocalizationDefinition
     * @param mqLocalizationProxydefinition
     * @param queuePointLocalizingMEs
     */
    private void checkValidLocalizationConfig(DestinationDefinition destinationDefinition, SIBUuid8 messagingEngineUuid, LocalizationDefinition destinationLocalizationDefinition,
                                              Set<String> queuePointLocalizingMEs)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkValidLocalizationConfig", new Object[] {
                                                                          destinationDefinition, messagingEngineUuid,
                                                                          destinationLocalizationDefinition,
                                                                          queuePointLocalizingMEs });

        checkQueuePointContainsLocalME(queuePointLocalizingMEs, messagingEngineUuid, destinationDefinition, destinationLocalizationDefinition);
        checkQueuePointLocalizationExists(destinationDefinition, queuePointLocalizingMEs, destinationLocalizationDefinition, messagingEngineUuid);
        checkQueuePointLocalizingSize(queuePointLocalizingMEs, destinationDefinition);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkValidLocalizationConfig");
    }

    /**
     * Checks if this destination is a queue/service or port
     *
     * @param type
     */
    private boolean isQueue(DestinationType type)
    {
        boolean isQueue = false;
        if (type == DestinationType.QUEUE ||
            type == DestinationType.PORT)
            //  type == DestinationType.SERVICE)
            isQueue = true;

        return isQueue;
    }

    /**
     * <p>This method is used to alter a destination that is localised on this messaging
     * engine.</p>
     *
     * @param destinationLocalizationDefinition
     * @param destinationDefinition
     * @return
     * @throws SIIncorrectCallException
     * @throws SIMPDestinationAlreadyExistsException
     * @throws SINotPossibleInCurrentConfigurationException
     */
    public void alterDestinationLocalization(
                                             DestinationDefinition destinationDefinition,
                                             LocalizationDefinition destinationLocalizationDefinition,
                                             Set<String> destinationLocalizingMEs)
                    throws SIResourceException, SIIncorrectCallException, SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "alterDestinationLocalization",
                        new Object[] {
                                      destinationDefinition,
                                      destinationLocalizationDefinition,
                                      destinationLocalizingMEs });

        // Create a local UOW
        LocalTransaction transaction = txManager.createLocalTransaction(true);

        // Try to alter the local destination.
        try
        {
            alterDestinationLocalization(
                                         destinationDefinition,
                                         destinationLocalizationDefinition,
                                         destinationLocalizingMEs,
                                         transaction);

            // If everything was successful then commit the unit of work
            transaction.commit();
        } catch (SIResourceException e)
        {
            // No FFDC code needed

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "alterDestinationLocalization", e);

            handleRollback(transaction);

            throw e;
        } catch (SITemporaryDestinationNotFoundException e)
        {
            // No FFDC code needed

            // Should never occur as you can't alter a temporary destination.

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "alterDestinationLocalization", e);

            handleRollback(transaction);

            throw new SIErrorException(e);
        } catch (RuntimeException e)
        {
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.DestinationManager.alterDestinationLocalization",
                                        "1:4353:1.508.1.7",
                                        this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                SibTr.exception(tc, e);
                SibTr.exit(tc, "alterDestinationLocalization", e);
            }

            handleRollback(transaction);

            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "alterDestinationLocalization");
    }

    /**
     * <p>This method is used to alter a destination that is localised on this messaging
     * engine.</p>
     *
     * @param destinationLocalizationDefinition
     * @param destinationDefinition
     * @param transaction
     * @throws SINotPossibleInCurrentConfigurationException
     * @throws SITemporaryDestinationNotFoundException
     */
    private void alterDestinationLocalization(
                                              DestinationDefinition destinationDefinition,
                                              LocalizationDefinition destinationLocalizationDefinition,
                                              Set<String> destinationLocalizingMEs,
                                              LocalTransaction transaction)
                    throws SIResourceException, SITemporaryDestinationNotFoundException, SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "alterDestinationLocalization",
                        new Object[] {
                                      destinationDefinition,
                                      destinationLocalizationDefinition,
                                      destinationLocalizingMEs,
                                      transaction });

        BaseDestinationHandler destinationHandler = null;

        //The destination is being created on the local messaging engine.
        SIBUuid8 messagingEngineUuid = messageProcessor.getMessagingEngineUuid();

        DestinationTypeFilter filter = new DestinationTypeFilter();
        filter.VISIBLE = Boolean.TRUE;
        filter.ALIAS = Boolean.FALSE;
        filter.FOREIGN_DESTINATION = Boolean.FALSE;
        DestinationHandler dh = destinationIndex.findByUuid(destinationDefinition.getUUID(), filter);

        // Check the destination exists
        checkDestinationHandlerExists(
                                      dh != null,
                                      destinationDefinition.getName(),
                                      messageProcessor.getMessagingEngineName());

        destinationHandler = (BaseDestinationHandler) dh;

        //if the destination is a queue we need to check the configuration
        if (!destinationHandler.isPubSub())
        {
            boolean validAlteration = false;
            //See if the destination has a local queue point
            validAlteration = destinationHandler.hasLocal();
            //Check that the destination is not a system destination or a temporary destination
            validAlteration = validAlteration &&
                              !(dh.isTemporary() || dh.isSystem());
            if (!validAlteration)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(
                               tc,
                               "alterDestinationLocalization",
                               "Invalid destination alteration");

                SIMPNotPossibleInCurrentConfigurationException e = new SIMPNotPossibleInCurrentConfigurationException(
                                nls.getFormattedMessage(
                                                        "INVALID_DEST_ALTER_ERROR_CWSIP0045",
                                                        new Object[] { destinationDefinition.getName(), messageProcessor.getMessagingEngineName() },
                                                        null));
                throw e;
            }
        }//end check

        //A further check that the destination alter call is valid
        checkValidLocalizationConfig(destinationDefinition,
                                     messagingEngineUuid,
                                     destinationLocalizationDefinition,
                                     destinationLocalizingMEs);

        synchronized (destinationHandler)
        {
            // Treat as if the destination does not exist if it is to be deleted
            checkDestinationHandlerExists(
                                          !destinationHandler.isToBeDeleted(),
                                          destinationDefinition.getName(),
                                          messageProcessor.getMessagingEngineName());

            if (destinationHandler.getDestinationType() != destinationDefinition.getDestinationType())
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "alterDestinationLocalization", "SIErrorException");
                throw new SIErrorException(
                                nls.getFormattedMessage(
                                                        "INTERNAL_CONFIGURATION_ERROR_CWSIP0006",
                                                        new Object[] { messageProcessor.getMessagingEngineBus(), "1:4483:1.508.1.7", destinationHandler.getName() },
                                                        null));
            }

            // Only update destination handler objects with WCCM information if
            // we have not already detected them as corrupt.
            if (!destinationHandler.isCorruptOrIndoubt())
            {
                /*
                 * The create should complete succesfully, with the new destination
                 * definition replacing the existing one.
                 */
                if (isQueue(destinationHandler.getDestinationType()))
                {
                    if (destinationLocalizationDefinition != null)
                    {
                        // Update the localisation definition too.
                        destinationHandler.updateLocalizationDefinition(destinationLocalizationDefinition, transaction);
                    }
                } // eof is q
                else if (destinationHandler.getDestinationType() == DestinationType.TOPICSPACE)
                {
                    destinationHandler.updateLocalizationDefinition(destinationLocalizationDefinition, transaction);
                } // eof is topicspace

                destinationHandler.updateDefinition(destinationDefinition);

                destinationHandler.updateLocalizationSet(
                                destinationLocalizingMEs);

                // Alert the lookups object to handle the re-definition
                destinationIndex.setLocalizationFlags(destinationHandler);
            }

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "alterDestinationLocalization");

        return;
    }

    /**
     * @param destinationDefinition
     * @param queuePointLocalizingMEs
     * @param destinationLocalizationDefinition
     */
    private void checkQueuePointLocalizationExists(DestinationDefinition destinationDefinition,
                                                   Set<String> queuePointLocalizingMEs,
                                                   LocalizationDefinition destinationLocalizationDefinition,
                                                   SIBUuid8 messagingEngineUuid)
    {
        //If queue point locality set contains local ME, there must be a localization
        //definition passed in for the local queue point
        if (isQueue(destinationDefinition.getDestinationType())
            && ((queuePointLocalizingMEs != null)
                && (queuePointLocalizingMEs.contains(messagingEngineUuid.toString()))
                && (destinationLocalizationDefinition == null)))
        {
            throw new SIErrorException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_CONFIGURATION_ERROR_CWSIP0006",
                                                    new Object[] { "DestinationManager", "1:4565:1.508.1.7", destinationDefinition.getName() },
                                                    null));
        }
    }

    /**
     * Asserts that a DestinationHandler exists. Throws out the
     * appropriate exception if the condition has failed.
     *
     * @param condition This is the existence check. An expression here should
     *            evaluate to true if the destination exists, false otherwise.
     *            For example, a simple expression here could be (dh != null).
     * @param destName The destination we were looking for.
     * @param engineName The engine name for the destination we were looking for.
     *
     * @throws SITemporaryDestinationNotFoundException
     * @throws SINotPossibleInCurrentConfigurationException
     */
    private void checkDestinationHandlerExists(
                                               boolean condition, String destName, String engineName) throws SINotPossibleInCurrentConfigurationException, SITemporaryDestinationNotFoundException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "checkDestinationHandlerExists",
                        new Object[] { new Boolean(condition), destName, engineName });

        if (!condition)
        {
            if (destName.startsWith(SIMPConstants.TEMPORARY_QUEUE_DESTINATION_PREFIX) ||
                destName.startsWith(SIMPConstants.TEMPORARY_PUBSUB_DESTINATION_PREFIX))
            {
                SIMPTemporaryDestinationNotFoundException e = new SIMPTemporaryDestinationNotFoundException(
                                nls.getFormattedMessage(
                                                        "TEMPORARY_DESTINATION_NAME_ERROR_CWSIP0097",
                                                        new Object[] { destName },
                                                        null));

                SibTr.exception(tc, e);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "checkDestinationHandlerExists", e);

                throw e;
            }

            SIMPNotPossibleInCurrentConfigurationException e = new SIMPNotPossibleInCurrentConfigurationException(
                            nls_cwsik.getFormattedMessage(
                                                          "DELIVERY_ERROR_SIRC_15", // DESTINATION_NOT_FOUND_ERROR_CWSIP0042
                                                          new Object[] { destName, engineName },
                                                          null));

            e.setExceptionReason(SIRCConstants.SIRC0015_DESTINATION_NOT_FOUND_ERROR);
            e.setExceptionInserts(new String[] { destName, engineName });

            SibTr.exception(tc, e);

            // Log a warning message to assist in PD. We use the suppressor to avoid
            // multiple messages for the same destination.
            SibTr.warning(tc_cwsik,
                          SibTr.Suppressor.ALL_FOR_A_WHILE_SIMILAR_INSERTS,
                          "DELIVERY_ERROR_SIRC_15",
                          new Object[] { destName, engineName });

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "checkDestinationHandlerExists", e);

            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkDestinationHandlerExists");
    }

    /**
     * Asserts that a Bus exists. Throws out the
     * appropriate exception if the condition has failed.
     *
     * @param condition This is the existence check. An expression here should
     *            evaluate to true if the destination exists, false otherwise.
     *            For example, a simple expression here could be (dh != null).
     * @param foreignBusName The name of the foreign bus we were looking for.
     * @param cause Another exception that was the real cause of the non
     *            existence of a DestinationHandler. Make this null if there is no prior
     *            causing exception.
     *
     * @throws SINotPossibleInCurrentConfigurationException
     */
    private void checkBusExists(boolean condition, String foreignBusName, boolean linkError, Throwable cause)

                    throws SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "checkBusExists",
                        new Object[] { new Boolean(condition), foreignBusName, Boolean.valueOf(linkError), cause });

        if (!condition)
        {

            String errorMsg = "DELIVERY_ERROR_SIRC_38";
            int reason = SIRCConstants.SIRC0038_FOREIGN_BUS_NOT_FOUND_ERROR;
            if (linkError && cause != null)
            {
                reason = SIRCConstants.SIRC0041_FOREIGN_BUS_LINK_NOT_DEFINED_ERROR;
                errorMsg = "DELIVERY_ERROR_SIRC_41";
            }
            else if (cause != null)
            {
                reason = SIRCConstants.SIRC0039_FOREIGN_BUS_NOT_FOUND_ERROR;
                errorMsg = "DELIVERY_ERROR_SIRC_39";
            }

            SIMPNotPossibleInCurrentConfigurationException e = null;
            if (cause == null)
            {
                e = new SIMPNotPossibleInCurrentConfigurationException(
                                nls_cwsik.getFormattedMessage(
                                                              errorMsg,
                                                              new Object[] { foreignBusName, messageProcessor.getMessagingEngineName(), messageProcessor.getMessagingEngineBus() },
                                                              null));
                e.setExceptionInserts(new String[] { foreignBusName, messageProcessor.getMessagingEngineName(), messageProcessor.getMessagingEngineBus() });
                e.setExceptionReason(reason);
            }
            else
            {
                e = new SIMPNotPossibleInCurrentConfigurationException(
                                nls_cwsik.getFormattedMessage(
                                                              errorMsg,
                                                              new Object[] { foreignBusName, messageProcessor.getMessagingEngineName(), messageProcessor.getMessagingEngineBus(),
                                                                            SIMPUtils.getStackTrace(cause) },
                                                              null));
                e.setExceptionInserts(new String[] { foreignBusName, messageProcessor.getMessagingEngineName(), messageProcessor.getMessagingEngineBus(),
                                                    SIMPUtils.getStackTrace(cause) });
                e.setExceptionReason(reason);
            }

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "checkBusExists", e);

            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkBusExists");
    }

    /**
     * Asserts that an MQLink exists. Throws out the
     * appropriate exception if the condition has failed.
     *
     * @param condition This is the existence check. An expression here should
     *            evaluate to true if the destination exists, false otherwise.
     *            For example, a simple expression here could be (dh != null).
     * @param mqLinkName The name of the link we were looking for.
     *
     * @throws SINotPossibleInCurrentConfigurationException
     */
    private void checkMQLinkExists(boolean condition, String mqlinkName)

                    throws SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "checkMQLinkExists",
                        new Object[] { new Boolean(condition), mqlinkName });

        if (!condition)
        {

            SIMPNotPossibleInCurrentConfigurationException e =
                            new SIMPNotPossibleInCurrentConfigurationException(
                                            nls_cwsik.getFormattedMessage(
                                                                          "DELIVERY_ERROR_SIRC_42",
                                                                          new Object[] { mqlinkName, messageProcessor.getMessagingEngineName(),
                                                                                        messageProcessor.getMessagingEngineBus() },
                                                                          null));

            e.setExceptionInserts(new String[] { mqlinkName, messageProcessor.getMessagingEngineName(), messageProcessor.getMessagingEngineBus() });
            e.setExceptionReason(SIRCConstants.SIRC0042_MQ_LINK_NOT_FOUND_ERROR);

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "checkMQLinkExists", e);

            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkMQLinkExists");
    }

    /**
     * Asserts that a Foreign bus Link exists. Throws out the
     * appropriate exception if the condition has failed.
     *
     * @param condition This is the existence check. An expression here should
     *            evaluate to true if the destination exists, false otherwise.
     *            For example, a simple expression here could be (dh != null).
     * @param linkName The name of the link we were looking for.
     *
     * @throws SINotPossibleInCurrentConfigurationException
     */
    private void checkLinkExists(boolean condition, String linkName)

                    throws SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "checkLinkExists",
                        new Object[] { new Boolean(condition), linkName });

        if (!condition)
        {

            SIMPNotPossibleInCurrentConfigurationException e =
                            new SIMPNotPossibleInCurrentConfigurationException(
                                            nls_cwsik.getFormattedMessage(
                                                                          "DELIVERY_ERROR_SIRC_40",
                                                                          new Object[] { linkName, messageProcessor.getMessagingEngineName(),
                                                                                        messageProcessor.getMessagingEngineBus() },
                                                                          null));

            e.setExceptionInserts(new String[] { linkName, messageProcessor.getMessagingEngineName(), messageProcessor.getMessagingEngineBus() });
            e.setExceptionReason(SIRCConstants.SIRC0040_FOREIGN_BUS_LINK_NOT_FOUND_ERROR);

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "checkLinkExists", e);

            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkLinkExists");
    }

    /**
     * Checks that the Local ME is in the queue point localizing set
     *
     * @param queuePointLocalizingMEs
     */
    private void checkQueuePointContainsLocalME(Set<String> queuePointLocalizingMEs,
                                                SIBUuid8 messagingEngineUuid,
                                                DestinationDefinition destinationDefinition,
                                                LocalizationDefinition destinationLocalizationDefinition)
    {
        if (isQueue(destinationDefinition.getDestinationType())
            && (destinationLocalizationDefinition != null))
        {
            //Queue point locality set must contain local ME
            if ((queuePointLocalizingMEs == null) ||
                (!queuePointLocalizingMEs.contains(messagingEngineUuid.toString())))
            {
                throw new SIErrorException(
                                nls.getFormattedMessage(
                                                        "INTERNAL_CONFIGURATION_ERROR_CWSIP0006",
                                                        new Object[] { "DestinationManager", "1:4845:1.508.1.7", destinationDefinition.getName() },
                                                        null));
            }
        }
    }

    /**
     * Checks that the queuePointLocalising size is valid
     */
    private void checkQueuePointLocalizingSize(Set<String> queuePointLocalizingMEs,
                                               DestinationDefinition destinationDefinition)

    {
        //There must be at least one queue point
        if (((destinationDefinition.getDestinationType() != DestinationType.SERVICE) &&
            (queuePointLocalizingMEs.size() == 0)) ||
            ((destinationDefinition.getDestinationType() == DestinationType.SERVICE) &&
            (queuePointLocalizingMEs.size() != 0)))
        {
            throw new SIErrorException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_CONFIGURATION_ERROR_CWSIP0006",
                                                    new Object[] { "DestinationManager", "1:4867:1.508.1.7", destinationDefinition.getName() },
                                                    null));
        }

    }

    /**
     * <p>Public method to create a remote destination when passed a destinationDefinition and
     * a set of localising MEs. The create is performed transactionally and completes when
     * the transaction commits.</p>
     *
     * @param destinationDefinition
     * @param queuePointLocalizingMEs
     */
    public void createDestination(
                                  DestinationDefinition destinationDefinition,
                                  Set<String> queuePointLocalizingMEs) throws SIResourceException, SIIncorrectCallException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "createDestination",
                        new Object[] { destinationDefinition, queuePointLocalizingMEs });

        // Try to create the local destination.
        try
        {
            createRemoteDestination(destinationDefinition, queuePointLocalizingMEs);
        } catch (SIIncorrectCallException e)
        {
            // No FFDC code needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "createDestination", e);

            throw e;
        } catch (RuntimeException e)
        {
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.DestinationManager.createDestination",
                                        "1:4910:1.508.1.7",
                                        this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                SibTr.exception(tc, e);
                SibTr.exit(tc, "createDestination", e);
            }

            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createDestination");
    }

    /**
     * <p>Private method to create a remote destination when passed a destinationDefinition,
     * a set of localizing MEs and
     * a transaction. The create is performed transactionally and completes when the transaction
     * commits.</p>
     *
     * @param destinationDefinition
     * @param queuePointLocalizingMEs
     */
    private DestinationHandler createRemoteDestination(
                                                       DestinationDefinition destinationDefinition,
                                                       Set<String> queuePointLocalizingMEs) throws SIResourceException, SIIncorrectCallException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "createRemoteDestination",
                        new Object[] { destinationDefinition, queuePointLocalizingMEs });

        BaseDestinationHandler destinationHandler = null;

        //The lock on the destinationManager is taken to stop 2 threads creating the same
        //destination and also to synchronize dynamic deletes with the
        //creation of aliases.  This stops an alias destination being created that targets a
        //destination in the process of being deleted.
        synchronized (this)
        {
            DestinationTypeFilter filter = new DestinationTypeFilter();
            filter.VISIBLE = Boolean.TRUE;

            DestinationHandler dh =
                            destinationIndex.findByName(destinationDefinition.getName(), messageProcessor.getMessagingEngineBus(), filter);

            if (dh != null)
            {
                // DestinationHandler already created
                destinationHandler = (BaseDestinationHandler) dh;
            }
            else
            {
                // Create a new DestinationHandler, which is created locked
                LocalTransaction siTran = txManager.createLocalTransaction(false);

                destinationHandler =
                                new BaseDestinationHandler(
                                                destinationDefinition,
                                                messageProcessor,
                                                this,
                                                siTran,
                                                durableSubscriptions,
                                                messageProcessor.getMessagingEngineBus());

                // Update the set of localising messaging engines for the destinationHandler
                // Dont create remote localisations up front.  This can be done
                // if WLM picks one of them
                destinationHandler.updateLocalizationSet(
                                queuePointLocalizingMEs);

                DestinationIndex.Type type = new DestinationIndex.Type();
                type.alias = new Boolean(destinationHandler.isAlias());
                type.foreignDestination = new Boolean(destinationHandler.isForeign());
                type.local = new Boolean(destinationHandler.hasLocal());
                type.queue = new Boolean(!destinationHandler.isPubSub());
                type.remote = new Boolean(destinationHandler.hasRemote());
                type.state = State.CREATE_IN_PROGRESS;
                destinationIndex.put(destinationHandler, type);

                // If everything succesful then commit before other threads try to use it
                siTran.commit();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createRemoteDestination", destinationHandler);

        return destinationHandler;
    }

    /**
     * <p>Mark the destinationHandler as requiring
     * cleanup and start the asynchDeletionThread if it is not already running.</p>
     *
     * @param dh
     */
    public void markDestinationAsCleanUpPending(DestinationHandler dh)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "markDestinationAsCleanUpPending", dh);

        if (dh.isLink())
        {
            linkIndex.cleanup(dh);
        }
        else
        {
            destinationIndex.cleanup(dh);
        }

        //Start asynchDeletionThread if not already running
        startAsynchDeletion();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "markDestinationAsCleanUpPending");
    }

    /**
     * Create this message processor's asynchronous deletion thread.
     * <p>
     * This function will throw a runtime exception if creation fails.
     * <p>
     * Feature 183052
     */
    public void startAsynchDeletion()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "startAsynchDeletion");

        synchronized (deletionThreadLock)
        {

            // Create the asynchDeletionThread object if it doesnt already exist.
            if (asynchDeletionThread == null)
            {
                asynchDeletionThread = new AsynchDeletionThread(messageProcessor);
            }

            if (_isAsyncDeletionThreadStartable
                && !(asynchDeletionThread.isRunning())
                && !(asynchDeletionThread.isStopping()))
            {
                try
                {
                    messageProcessor.startNewSystemThread(asynchDeletionThread);

                    //Set the indicator that the asynchDeletionThread is running.  This must
                    //be set under the synchronization on the deletionThreadLock
                    asynchDeletionThread.setRunning(true);
                } catch (InterruptedException e)
                {
                    // FFDC
                    FFDCFilter.processException(
                                                e,
                                                "com.ibm.ws.sib.processor.impl.DestinationManager.startAsynchDeletion",
                                                "1:5073:1.508.1.7",
                                                this);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "startAsynchDeletion", e);

                    SibTr.exception(tc, e);

                    throw new WsRuntimeException(e);
                }
            }
            else
            {
                asynchDeletionThread.rerun();
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "startAsynchDeletion");
        }
    }

    /**
     * Generic stimulus off which all stop-type activity can be driven.
     * <p>
     * Like
     * <ul>
     * <li>Stopping the async deletion thread,
     * </ul>
     *
     */
    public void stop(int mode)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "stop");

        // Prevent any new async deletion activity kicking off a new background thread.
        setIsAsyncDeletionThreadStartable(false);

        // stop the async deletion thread if it's currently running.
        synchronized (deletionThreadLock)
        {
            if (asynchDeletionThread != null)
            {
                asynchDeletionThread.stopThread(messageProcessor.getStoppableThreadCache());
            }
        }

        //Make sure that this is null, so that unittests that only restart MP dont
        //see a half stopped asynch deletion thread.
        asynchDeletionThread = null;

        // Propogate the stop stimulus to the destination handlers...
        DestinationTypeFilter destFilter = new DestinationTypeFilter();
        destFilter.LOCAL = Boolean.TRUE;
        SIMPIterator itr = destinationIndex.iterator(destFilter);
        while (itr.hasNext())
        {
            DestinationHandler dh = (DestinationHandler) itr.next();
            dh.stop(mode);
        }
        itr.finished();

        //busses
        itr = foreignBusIndex.iterator();
        while (itr.hasNext())
        {
            DestinationHandler dh = (DestinationHandler) itr.next();
            dh.stop(mode);
        }
        itr.finished();

        //links
        LinkTypeFilter linkFilter = new LinkTypeFilter();
        linkFilter.LOCAL = Boolean.TRUE;
        itr = linkIndex.iterator(linkFilter); //d266910
        while (itr.hasNext())
        {
            DestinationHandler dh = (DestinationHandler) itr.next();
            dh.stop(mode);
        }
        itr.finished();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "stop");
    }

    /**
     * Generic stimulus off which all start-type activity can be driven.
     * <p>
     * Like starting the async deletion thread.
     *
     */
    public void start()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "start");

        // Propogate the stop stimulus to the destination handlers...
        DestinationTypeFilter destFilter = new DestinationTypeFilter();
        destFilter.LOCAL = Boolean.TRUE;
        SIMPIterator itr = destinationIndex.iterator(destFilter);
        while (itr.hasNext())
        {
            DestinationHandler dh = (DestinationHandler) itr.next();
            dh.start();
        }
        itr.finished();

        //busses
        itr = foreignBusIndex.iterator();
        while (itr.hasNext())
        {
            DestinationHandler dh = (DestinationHandler) itr.next();
            dh.start();
        }
        itr.finished();

        //links
        LinkTypeFilter linkFilter = new LinkTypeFilter();
        linkFilter.LOCAL = Boolean.TRUE;
        itr = linkIndex.iterator(linkFilter);
        while (itr.hasNext())
        {
            DestinationHandler dh = (DestinationHandler) itr.next();
            dh.start();
        }
        itr.finished();

        // Explicitly start the async deletion thread.
        startAsynchDeletion();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "start");
    }

    /**
     * Generic stimulus off which all destroy-type activity can be driven.
     * <p>
     * Like signalling the MQLink component to destroy appropriate resources.
     */
    public void destroy()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "destroy");

        // Iterate over the MQLinks, calling the MQLink component to
        // alert it to destroy resources
        LinkTypeFilter mqLinkFilter = new LinkTypeFilter();
        mqLinkFilter.MQLINK = Boolean.TRUE;
        SIMPIterator itr = linkIndex.iterator(mqLinkFilter);
        while (itr.hasNext())
        {
            MQLinkHandler mqLinkHandler = (MQLinkHandler) itr.next();
            try
            {
                mqLinkHandler.destroy();
            } catch (SIResourceException e)
            {
                // No FFDC code needed

                SibTr.exception(tc, e);

                // The MQLink component will have FFDC'd we'll trace
                // the problem but allow processing to continue
            } catch (SIException e)
            {
                // No FFDC code needed

                SibTr.exception(tc, e);

                // The MQLink component will have FFDC'd we'll trace
                // the problem but allow processing to continue
            }
        }
        itr.finished();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "destroy");
    }

    /**
     * The async deletion thread should not be started before the destination
     * handler is ready. ie: until it's really started up.
     */
    private volatile boolean _isAsyncDeletionThreadStartable = false;

    /**
     * Indicates whether the async deletion thread should be startable or not.
     *
     * @param isStartable
     */
    private void setIsAsyncDeletionThreadStartable(boolean isStartable)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setIsAsyncDeletionThreadStartable", new Boolean(isStartable));

        synchronized (deletionThreadLock)
        {
            _isAsyncDeletionThreadStartable = isStartable;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setIsAsyncDeletionThreadStartable");
    }

    /**
     * Notify that this message processor's asynchronous deletion thread is ending.
     * <p>
     * Feature 183052
     */
    void notifyAsynchDeletionEnd(AsynchDeletionThread thread)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "notifyAsynchDeletionEnd", thread);

        // Under the ADT lock, we'll notify any waiters and set the running
        // flag to false
        synchronized (deletionThreadLock)
        {
            thread.setRunning(false);
            deletionThreadLock.notifyAll();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "notifyAsynchDeletionEnd");

    }

    public JsDestinationAddress createSystemDestination(String prefix) throws SIResourceException, SIMPDestinationAlreadyExistsException, SIIncorrectCallException
    {
        return createSystemDestination(prefix, Reliability.ASSURED_PERSISTENT);
    }

    /**
     * Creates a System destination with the given prefix and reliability
     */
    public JsDestinationAddress createSystemDestination(String prefix, Reliability reliability) throws SIResourceException, SIMPDestinationAlreadyExistsException, SIIncorrectCallException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createSystemDestination", new Object[] { prefix, reliability });

        //there is no detailed prefix validation here as System Destinations are purely
        //internal
        if (prefix == null || prefix.length() > 24)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "createSystemDestination", "SIInvalidDestinationPrefixException");

            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0005",
                        new Object[] { "com.ibm.ws.sib.processor.impl.DestinationManager", "1:5324:1.508.1.7", prefix });

            throw new SIInvalidDestinationPrefixException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0005",
                                                    new Object[] { "com.ibm.ws.sib.processor.impl.DestinationManager", "1:5329:1.508.1.7", prefix },
                                                    null));
        }
        JsDestinationAddress destAddr =
                        SIMPUtils.createJsSystemDestinationAddress(prefix, messageProcessor.getMessagingEngineUuid());

        destAddr.setBusName(messageProcessor.getMessagingEngineBus());

        // Check and see if the destination already exists.
        //note that this is treated as an application call so we don't want to see
        //those that are being deleted
        DestinationHandler handler = getDestinationInternal(destAddr.getDestinationName(), destAddr.getBusName(), false);
        if (handler != null)
        {
            // The system destination exists for the destination prefix set so
            // just return.

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "createSystemDestination", destAddr);

            return destAddr;
        }

        DestinationDefinition destDef =
                        messageProcessor.createDestinationDefinition(DestinationType.QUEUE, destAddr.getDestinationName());
        destDef.setMaxReliability(reliability);
        destDef.setDefaultReliability(reliability);
        destDef.setUUID(new SIBUuid12());

        Set<String> destinationLocalizingSet = new HashSet<String>();
        destinationLocalizingSet.add(messageProcessor.getMessagingEngineUuid().toString());

        createDestinationLocalization(
                                      destDef,
                                      messageProcessor.createLocalizationDefinition(destDef.getName()),
                                      destinationLocalizingSet,
                                      false);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createSystemDestination", destAddr);

        return destAddr;
    }

    /**
     * Deletes the system destination from the given destination address
     */
    public void deleteSystemDestination(JsDestinationAddress destAddr) throws SIResourceException, SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "deleteSystemDestination", new Object[] { destAddr });

        BaseDestinationHandler destinationHandler = null;

        DestinationTypeFilter filter = new DestinationTypeFilter();
        filter.VISIBLE = Boolean.TRUE;
        filter.ALIAS = Boolean.FALSE;
        filter.FOREIGN_DESTINATION = Boolean.FALSE;
        DestinationHandler dh =
                        destinationIndex.findByName(destAddr.getDestinationName(), messageProcessor.getMessagingEngineBus(), filter);

        try
        {
            checkDestinationHandlerExists(
                                          dh != null,
                                          destAddr.getDestinationName(),
                                          messageProcessor.getMessagingEngineName());
        } catch (SITemporaryDestinationNotFoundException e)
        {
            // // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.DestinationManager.deleteSystemDestination",
                                        "1:5408:1.508.1.7",
                                        this);

            // This shouldn't occur as the Connection will create a valid system destination.
            // Or validate that the destination address passed in is a system destination

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "deleteSystemDestination", "SIErrorException");

            throw new SIErrorException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                                                    new Object[] { "com.ibm.ws.sib.processor.impl.DestinationManager",
                                                                  "1:5421:1.508.1.7",
                                                                  e,
                                                                  destAddr.getDestinationName() },
                                                    null),
                            e);
        }

        destinationHandler = (BaseDestinationHandler) dh;

        deleteSystemDestination(destinationHandler);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deleteSystemDestination");
    }

    /**
     * Deletes the system destination from the given destination address
     */
    private void deleteSystemDestination(BaseDestinationHandler destinationHandler) throws SINotPossibleInCurrentConfigurationException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "deleteSystemDestination", new Object[] { destinationHandler });

        synchronized (destinationHandler)
        {
            if (destinationHandler.isToBeDeleted() || !destinationHandler.isSystem())
            {
                // Treat as if the destination does not exist.
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(
                               tc,
                               "deleteSystemDestination",
                               "Destination not found as destination is to be deleted, or is system");

                throw new SIMPNotPossibleInCurrentConfigurationException(
                                nls_cwsik.getFormattedMessage(
                                                              "DELETE_SYSTEM_DEST_ERROR_CWSIP0046",
                                                              new Object[] { destinationHandler.getName(), messageProcessor.getMessagingEngineName() }, null));
            }

            // Set the deletion flag in the DH persistently.
            ExternalAutoCommitTransaction transaction = txManager.createAutoCommitTransaction();

            destinationHandler.setToBeDeleted(true);
            destinationIndex.delete(destinationHandler);
            try
            {
                destinationHandler.requestUpdate(transaction);
            } catch (MessageStoreException e)
            {
                // MessageStoreException shouldn't occur so FFDC.
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.DestinationManager.deleteSystemDestination",
                                            "1:5476:1.508.1.7",
                                            this);

                SibTr.exception(tc, e);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "deleteSystemDestination", "SIResourceException");
                throw new SIResourceException(e);
            }

            // Close all connections to the localisation
            // Ensure that before calling these methods, the destination is marked
            // to-be-deleted or is marked as deleted, to avoid any new producers and
            // consumers attaching after we have closed the currently attached ones.
            destinationHandler.closeProducers();
            destinationHandler.closeConsumers();

            //Add all the localisations to the set of those requiring clean-up
            destinationHandler.addAllLocalisationsForCleanUp();

            //Clear the set of ME's that localise the destination
            destinationHandler.clearLocalisingUuidsSet();

            startAsynchDeletion();

        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deleteSystemDestination");
    }

    public synchronized AsyncUpdateThread getAsyncUpdateThread()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getAsyncUpdateThread");

        if (asyncUpdateThread == null)
        {
            asyncUpdateThread = new AsyncUpdateThread(messageProcessor, messageProcessor.getTXManager(),
                            messageProcessor.getCustomProperties().get_anycast_batch_size(),
                            messageProcessor.getCustomProperties().get_anycast_batch_timeout());
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getAsyncUpdateThread", asyncUpdateThread);

        return asyncUpdateThread;
    }

    public synchronized AsyncUpdateThread getPersistLockThread()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getPersistLockThread");

        if (persistLockThread == null)
        {
            persistLockThread = new AsyncUpdateThread(messageProcessor, messageProcessor.getTXManager(),
                            messageProcessor.getCustomProperties().get_anycast_lock_batch_size(),
                            messageProcessor.getCustomProperties().get_anycast_lock_batch_timeout());
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getPersistLockThread", persistLockThread);

        return persistLockThread;
    }

    public synchronized AsynchDeletionThread getAsynchDeletionThread()
    {
        return asynchDeletionThread;
    }

    /**
     * Add a subscription to the list of subscriptions to be deleted.
     *
     */
    public void addSubscriptionToDelete(SubscriptionItemStream stream)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "addSubscriptionToDelete", stream);
        synchronized (deletableSubscriptions)
        {
            deletableSubscriptions.add(stream);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "addSubscriptionToDelete");
    }

    public List<SubscriptionItemStream> getSubscriptionsToDelete()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getSubscriptionsToDelete");
            SibTr.exit(tc, "getSubscriptionsToDelete", deletableSubscriptions);
        }
        return deletableSubscriptions;
    }

    public void removeSubscriptionAsDeleted(SubscriptionItemStream stream)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "removeSubscriptionAsDeleted", stream);

        synchronized (deletableSubscriptions)
        {
            deletableSubscriptions.remove(stream);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "removeSubscriptionAsDeleted");
    }

    // ALERT - NY TEMPORARY :-) This was added to support reconciliation unit test.

    /**
     * Create a remote Destination. If the Destination is already exists then
     * add a remote PtoPMessageItemStream.
     * <p>
     * We assume that the DestinationDefinitionImpl given is point to point.
     * There is no check as to whether the remove Destination/PtoPMessageItemStream already
     * exists.
     *
     * @param name
     * @param destinationDefinitionImpl
     *
     */
    public void addRemoteDestination(
                                     String name,
                                     Set<String> queuePointLocalizingSet,
                                     DestinationDefinition destinationDefinition) throws SIResourceException, SIIncorrectCallException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "addRemoteDestination", new Object[] { name, destinationDefinition });

        // Try to add the remote destination.
        try
        {
            createRemoteDestination(destinationDefinition, queuePointLocalizingSet);
        } catch (SIIncorrectCallException e)
        {
            // No FFDC code needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "addRemoteDestination", e);

            throw e;
        } catch (SIResourceException e)
        {
            // No FFDC code needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "addRemoteDestination", e);

            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "addRemoteDestination");
    }

    /**
     * @param destinationName
     * @param localizingME
     * @return
     */
    private DestinationHandler createRemoteSystemDestination(String destinationName, SIBUuid8 localizingME) throws SIResourceException, SIIncorrectCallException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createRemoteSystemDestination", new Object[] { destinationName, localizingME });

        DestinationHandler destinationHandler = null;
        DestinationDefinition destinationDefinition =
                        messageProcessor.createDestinationDefinition(DestinationType.QUEUE, destinationName);

        destinationDefinition.setDefaultReliability(Reliability.ASSURED_PERSISTENT);
        destinationDefinition.setMaxReliability(Reliability.ASSURED_PERSISTENT);

        Set<String> queuePointLocalizingMEs = new HashSet<String>();
        queuePointLocalizingMEs.add(localizingME.toString());

        try
        {
            destinationHandler =
                            createRemoteDestination(
                                                    destinationDefinition,
                                                    queuePointLocalizingMEs);
        } catch (SIIncorrectCallException e)
        {
            //No FFDC code needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "createRemoteSystemDestination", e);

            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createRemoteSystemDestination", destinationHandler);

        return destinationHandler;
    }

    /**
     * Method getDestination.
     *
     * @param destinationUuid
     * @return Destination
     * @throws SIDestinationNotFoundException
     *             <p>This method provides lookup of a destination by its uuid.
     *             If the destination is not
     *             found, it throws SIDestinationNotFoundException.</p>
     */
    public DestinationHandler getDestination(SIBUuid12 destinationUuid, boolean includeInvisible) throws SITemporaryDestinationNotFoundException, SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getDestination", destinationUuid);

        // Get the destination, include invisible dests
        DestinationHandler destinationHandler = getDestinationInternal(destinationUuid, includeInvisible);

        checkDestinationHandlerExists(
                                      destinationHandler != null,
                                      destinationUuid.toString(),
                                      messageProcessor.getMessagingEngineName());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getDestination", destinationHandler);

        return destinationHandler;
    }

    public DestinationHandler getDestinationByUuid(SIBUuid12 destinationUuid, boolean includeInvisible)
                    throws SITemporaryDestinationNotFoundException, SINotPossibleInCurrentConfigurationException, SIMPDestinationCorruptException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getDestinationByUuid", new Object[] { destinationUuid, Boolean.valueOf(includeInvisible) });

        DestinationHandler destinationHandler = getDestinationInternal(destinationUuid, includeInvisible);

        if (destinationHandler == null)
        {
            destinationHandler = loadDestination(destinationUuid.toString(), messageProcessor.getMessagingEngineBus(), null, false);
        }

        checkDestinationHandlerExists(
                                      destinationHandler != null,
                                      destinationUuid.toString(),
                                      messageProcessor.getMessagingEngineName());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getDestinationByUuid", destinationHandler);

        return destinationHandler;
    }

    /**
     * Lookup a destination by its uuid.
     *
     * @param mqLinkUuid
     * @param includeInvisible
     * @return MQLinkHandler
     *
     * @throws SIDestinationNotFoundException The link was not found
     * @throws SIMPMQLinkCorruptException
     */
    public MQLinkHandler getMQLinkLocalization(SIBUuid8 mqLinkUuid, boolean includeInvisible) throws SIMPMQLinkCorruptException, SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getMQLinkLocalization", mqLinkUuid);

        // Get the destination
        LinkTypeFilter filter = new LinkTypeFilter();
        filter.MQLINK = Boolean.TRUE;
        if (!includeInvisible)
            filter.VISIBLE = Boolean.TRUE;

        MQLinkHandler mqLinkHandler = (MQLinkHandler) linkIndex.findByMQLinkUuid(mqLinkUuid, filter);

        checkMQLinkExists(
                          mqLinkHandler != null,
                          mqLinkUuid.toString());

        if (mqLinkHandler.isCorruptOrIndoubt())
        {
            String message =
                            nls.getFormattedMessage(
                                                    "LINK_HANDLER_CORRUPT_ERROR_CWSIP0054",
                                                    new Object[] { mqLinkHandler.getName(), mqLinkUuid.toString() },
                                                    null);

            SIMPMQLinkCorruptException e = new SIMPMQLinkCorruptException(message);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "getMQLinkLocalization", e);

            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getMQLinkLocalization", mqLinkHandler);

        return mqLinkHandler;
    }

    /**
     * Find the mediated destinations and tell them that WAS is now open for
     * e-business.
     *
     * Feature 176658.3.8
     */
    void announceWASOpenForEBusiness()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "announceWASOpenForEBusiness");

        DestinationTypeFilter destFilter = new DestinationTypeFilter();
        SIMPIterator itr = destinationIndex.iterator(destFilter);
        while (itr.hasNext())
        {
            DestinationHandler dh = (DestinationHandler) itr.next();
            dh.announceWasOpenForEBusiness();
        }
        itr.finished();

        LinkTypeFilter linkFilter = new LinkTypeFilter();
        linkFilter.LOCAL = Boolean.TRUE;
        itr = linkIndex.iterator(linkFilter);
        while (itr.hasNext())
        {
            DestinationHandler dh = (DestinationHandler) itr.next();
            dh.announceWasOpenForEBusiness();
        }
        itr.finished();

        itr = foreignBusIndex.iterator();
        while (itr.hasNext())
        {
            DestinationHandler dh = (DestinationHandler) itr.next();
            dh.announceWasOpenForEBusiness();
        }
        itr.finished();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "announceWASOpenForEBusiness");
    }

    /**
     * Find the mediated destinations and tell them that WAS is now CLOSED for
     * e-business.
     *
     * Feature 176658.3.8
     */
    void announceWASClosedForEBusiness()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "announceWASClosedForEBusiness");

        DestinationTypeFilter destFilter = new DestinationTypeFilter();
        SIMPIterator itr = destinationIndex.iterator(destFilter);
        while (itr.hasNext())
        {
            DestinationHandler dh = (DestinationHandler) itr.next();
            dh.announceWasClosedForEBusiness();
        }
        itr.finished();

        LinkTypeFilter linkFilter = new LinkTypeFilter();
        linkFilter.LOCAL = Boolean.TRUE;
        itr = linkIndex.iterator(linkFilter);
        while (itr.hasNext())
        {
            DestinationHandler dh = (DestinationHandler) itr.next();
            dh.announceWasClosedForEBusiness();
        }
        itr.finished();

        itr = foreignBusIndex.iterator();
        while (itr.hasNext())
        {
            DestinationHandler dh = (DestinationHandler) itr.next();
            dh.announceWasClosedForEBusiness();
        }
        itr.finished();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "announceWASClosedForEBusiness");
    }

    /**
     * Find the mediated destinations and tell them that MP is about to stop.
     */
    void announceMPStopping()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "announceMPStopping");

        DestinationTypeFilter destFilter = new DestinationTypeFilter();
        SIMPIterator itr = destinationIndex.iterator(destFilter);
        while (itr.hasNext())
        {
            DestinationHandler dh = (DestinationHandler) itr.next();
            dh.announceMPStopping();
        }
        itr.finished();

        LinkTypeFilter linkFilter = new LinkTypeFilter();
        linkFilter.LOCAL = Boolean.TRUE;
        itr = linkIndex.iterator(linkFilter);
        while (itr.hasNext())
        {
            DestinationHandler dh = (DestinationHandler) itr.next();
            dh.announceMPStopping();
        }
        itr.finished();

        itr = foreignBusIndex.iterator();
        while (itr.hasNext())
        {
            DestinationHandler dh = (DestinationHandler) itr.next();
            dh.announceMPStopping();
        }
        itr.finished();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "announceMPStopping");
    }

    /**
     * Find the mediated destinations and tell them that the MP is now ready
     * for mediations to start work. In addition alert the MQLink component
     * that MP has started and set the flag to allow asynch deletion.
     *
     * Feature 176658.3.8
     */
    void announceMPStarted(int startMode)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "announceMPStarted");

        DestinationTypeFilter destFilter = new DestinationTypeFilter();
        SIMPIterator itr = destinationIndex.iterator(destFilter);
        while (itr.hasNext())
        {
            DestinationHandler dh = (DestinationHandler) itr.next();
            dh.announceMPStarted();
        }
        itr.finished();

        LinkTypeFilter linkFilter = new LinkTypeFilter();
        linkFilter.LOCAL = Boolean.TRUE;
        itr = linkIndex.iterator(linkFilter);
        while (itr.hasNext())
        {
            DestinationHandler dh = (DestinationHandler) itr.next();
            dh.announceMPStarted();
        }
        itr.finished();

        itr = foreignBusIndex.iterator();
        while (itr.hasNext())
        {
            DestinationHandler dh = (DestinationHandler) itr.next();
            dh.announceMPStarted();
        }
        itr.finished();

        // Iterate over the MQLinks, calling the MQLink component to
        // alert it that MP has started
        LinkTypeFilter mqLinkFilter = new LinkTypeFilter();
        mqLinkFilter.MQLINK = Boolean.TRUE;
        itr = linkIndex.iterator(mqLinkFilter);
        while (itr.hasNext())
        {
            MQLinkHandler mqLinkHandler = (MQLinkHandler) itr.next();
            try
            {
                mqLinkHandler.
                                announceMPStarted(startMode,
                                                  messageProcessor.getMessagingEngine());
            } catch (SIResourceException e)
            {
                // No FFDC code needed

                SibTr.exception(tc, e);

                // The MQLink component will have FFDC'd we'll trace
                // the problem but allow processing to continue
            } catch (SIException e)
            {
                // No FFDC code needed

                SibTr.exception(tc, e);

                // The MQLink component will have FFDC'd we'll trace
                // the problem but allow processing to continue
            }
        }
        itr.finished();

        //Allow the async deletion thread to start up now if it wants.
        setIsAsyncDeletionThreadStartable(true);

        // Explicitly start the async deletion thread if there is anything to do.
        startAsynchDeletion();

        //start DeletePubSubMsgsThread.
        startDeletePubSubMsgsThread();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "announceMPStarted");
    }

    /**
     * starts DeletePubSubMsgsThread
     */
    private void startDeletePubSubMsgsThread() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "startDeletePubSubMsgsThread");

        //every time create DeletePubSubMsgsThread as this is called from
        // a new DestinationManager.

        Thread delThread = new Thread((new DeletePubSubMsgsThread(messageProcessor)), "startDeletePubSubMsgsThread");
        delThread.setPriority(1);
        delThread.start();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "startDeletePubSubMsgsThread");
    }

    /**
     * <p>This method is used to create a link that is localised on this ME.</p>
     *
     * @param destinationLocalizationDefinition
     * @param destinationDefinition
     */
    public void createLinkLocalization(VirtualLinkDefinition virtualLinkDefinition)
                    throws SIResourceException, SIIncorrectCallException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createLinkLocalization", new Object[] { virtualLinkDefinition });

        // Create a local UOW
        LocalTransaction transaction = txManager.createLocalTransaction(false);

        // Try to create the local destination.
        try
        {
            createLinkLocalization(virtualLinkDefinition, transaction);

            // If everything was successful then commit the unit of work
            transaction.commit();
        } catch (SIIncorrectCallException e)
        {
            // No FFDC code needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "createLinkLocalization", e);

            handleRollback(transaction);

            throw e;
        } catch (RuntimeException e)
        {
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.DestinationManager.createLinkLocalization",
                                        "1:6049:1.508.1.7",
                                        this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                SibTr.exception(tc, e);
                SibTr.exit(tc, "createLinkLocalization", e);
            }

            handleRollback(transaction);

            // PK69423 Put the link in-doubt, so we do not delete it when we reconcile
            putLinkIntoIndoubtState(virtualLinkDefinition.getName());

            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createLinkLocalization");
    }

    /**
     * <p>This method is used to create a link that is localised on this ME.</p>
     */
    private void createLinkLocalization(
                                        VirtualLinkDefinition virtualLinkDefinition,
                                        LocalTransaction transaction) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createLinkLocalization", new Object[] { virtualLinkDefinition, transaction });

        boolean linkCreated = false;
        LinkHandler linkHandler = null;

        //Include links that are awaiting deletion as we may need to re-use their
        //stream state.
        LinkTypeFilter filter = new LinkTypeFilter();
        DestinationHandler dh = linkIndex.findByName(virtualLinkDefinition.getName(), filter);

        if (dh == null)
        {
            // Create a new LinkHandler, which is created locked
            linkHandler =
                            new LinkHandler(virtualLinkDefinition,
                                            messageProcessor,
                                            this,
                                            transaction,
                                            durableSubscriptions);

            linkCreated = true;
            // The link is localised here, set the isLocal flag.
            //TODO could we use a different flag to avoid confusion?
            linkHandler.setLocal();

            LinkIndex.Type type = new LinkIndex.Type();
            type.local = Boolean.TRUE;
            type.mqLink = Boolean.FALSE;
            type.remote = Boolean.FALSE;
            type.state = State.CREATE_IN_PROGRESS;
            linkIndex.put(linkHandler, type);

            linkHandler.registerControlAdapters();
        }
        else
        {
            linkHandler = (LinkHandler) dh;
        }

        // If the link is corrupt, do not attempt any update.
        if (!linkHandler.isCorruptOrIndoubt())
        {
            synchronized (linkHandler)
            {
                //If we didnt just create the linkHandler and the one that already
                //existed is for a different UUID, or there is already a localisation for
                //the linkHandler on this ME, then this could be a problem and further
                //tests are required.
                if (!linkCreated)
                {
                    //Use the existing linkHandler of the same name as the stream state associated with it needs
                    //to be associated with the new LinkHandler.  This is because the ME at the other end
                    //of the link in the other bus only knows the LinkHandler by name and cannot
                    //tell that it has been deleted and recreated, so could still be sending using the old
                    //stream state

                    synchronized (linkIndex) {

                        LinkIndex.Type type = null;
                        if (linkIndex.get(linkHandler.getUuid()) != null)
                        {
                            type = (LinkIndex.Type) linkIndex.getType(linkHandler);
                            linkIndex.remove(linkHandler);
                        }
                        else
                            type = new LinkIndex.Type();

                        linkHandler.updateUuid(virtualLinkDefinition.getUuid());

                        type.local = Boolean.TRUE;
                        type.remote = Boolean.FALSE;
                        type.mqLink = Boolean.FALSE;
                        type.state = State.ACTIVE;

                        // Alert the lookups object to handle the re-definition
                        linkIndex.put(linkHandler, type);

                        //Unset the "to-be-deleted" indicator
                        if (linkHandler.isToBeDeleted() || !linkHandler.getUuid().equals(virtualLinkDefinition.getUuid()))
                        {
                            linkHandler.setToBeDeleted(false);
                            linkIndex.cleanup(linkHandler);
                        }
                    }

                    linkHandler.updateLinkDefinition(virtualLinkDefinition, transaction);
                }

                //Get the uuid of the ME in the other bus from TRM, then
                //create a transmit q to it, to store messages originated from
                //this ME destined for the other bus

                LinkSelection s = null;
                LinkManager linkManager = messageProcessor.getLinkManager();
                try
                {
                    s = linkManager.select(linkHandler.getUuid());
                } catch (LinkException e)
                {
                    //Error during create of the link.  Trace an FFST.
                    FFDCFilter.processException(
                                                e,
                                                "com.ibm.ws.sib.processor.impl.DestinationManager.createLinkLocalization",
                                                "1:6182:1.508.1.7",
                                                this);

                    SibTr.exception(tc, e);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "createLinkLocalization", e);
                    throw new SIResourceException(e);
                }

                SIBUuid8 localisingME = null;
                SIBUuid8 routingME = null;
                if (s != null)
                {
                    localisingME = s.getInboundMeUuid();
                    routingME = s.getOutboundMeUuid();
                }
                else
                {
                    localisingME = new SIBUuid8(SIMPConstants.UNKNOWN_UUID);
                    routingME = null;
                }

                if (linkCreated || !linkHandler.hasRemote()) //PK76306 - if we don't have a remote localisation then create one
                {
                    linkHandler.addNewPtoPLocalization(true,
                                                       transaction,
                                                       localisingME,
                                                       null,
                                                       true);
                }
                else
                    linkHandler.updateLocalisationSet(localisingME, routingME);

            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createLinkLocalization");
    }

    /**
     * <p>Private method to create a remote link when passed a virtualLinkDefinition and
     * a transaction. The create is performed transactionally and completes when the transaction
     * commits.</p>
     *
     * @param virtualLinkDefinition
     * @param transaction
     * @return The created linkHandler
     */
    private LinkHandler createRemoteLink(
                                         VirtualLinkDefinition virtualLinkDefinition,
                                         LocalTransaction transaction) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createRemoteLink", new Object[] { virtualLinkDefinition, transaction });

        LinkHandler linkHandler = null;
        boolean linkCreated = false;

        synchronized (linkIndex)
        {
            DestinationHandler dh = linkIndex.findByUuid(virtualLinkDefinition.getUuid(), null);

            if (dh != null)
            {
                linkHandler = (LinkHandler) dh;
            }
            else
            {
                // Create a new LinkHandler, which is created locked
                linkHandler =
                                new LinkHandler(virtualLinkDefinition,
                                                messageProcessor,
                                                this,
                                                transaction,
                                                durableSubscriptions);

                LinkIndex.Type type = new LinkIndex.Type();
                type.local = Boolean.FALSE;
                type.mqLink = Boolean.FALSE;
                type.remote = Boolean.TRUE;
                type.state = State.ACTIVE;
                linkIndex.put(linkHandler, type);

                linkCreated = true;
            }
        }

        if (linkCreated)
        {
            synchronized (linkHandler)
            {
                LinkSelection s = null;
                LinkManager linkManager = messageProcessor.getLinkManager();
                try
                {
                    s = linkManager.select(linkHandler.getUuid());
                } catch (LinkException e)
                {
                    //Error during create of the link.  Trace an FFST.
                    FFDCFilter.processException(
                                                e,
                                                "com.ibm.ws.sib.processor.impl.DestinationManager.createRemoteLink",
                                                "1:6286:1.508.1.7",
                                                this);

                    SibTr.exception(tc, e);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "createRemoteLink", e);
                    throw new SIResourceException(e);
                }

                SIBUuid8 localisingME = null;
                if (s != null)
                {
                    localisingME = s.getInboundMeUuid();
                    if (localisingME == null)
                    {
                        //MQLinks dont have an inboundMEUuid so we send to the outboundMEUuid
                        localisingME = s.getOutboundMeUuid();
                    }
                }
                else
                {
                    localisingME = new SIBUuid8(SIMPConstants.UNKNOWN_UUID);
                }

                linkHandler.addNewPtoPLocalization(true, transaction, localisingME, null, true);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createRemoteLink", linkHandler);

        return linkHandler;
    }

    /**
     * <p>Private method to create a foreign bus when passed a linkName,
     * a bus definition and
     * a transaction. The create is performed transactionally and completes when the transaction
     * commits.</p>
     *
     * @param linkName
     * @param stringLinkUuid
     * @param foreignBusDefinition
     * @param transaction
     * @return
     */
    private BusHandler createForeignBus(
                                        String linkName,
                                        String stringLinkUuid,
                                        ForeignBusDefinition foreignBusDefinition) throws SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createForeignBus",
                        new Object[] { linkName, stringLinkUuid, foreignBusDefinition });

        SIBUuid12 linkUuid = new SIBUuid12(stringLinkUuid);
        BusHandler busHandler = null;

        synchronized (linkIndex)
        {
            LinkTypeFilter filter = new LinkTypeFilter();
            filter.VISIBLE = Boolean.TRUE;
            DestinationHandler linkHandler = linkIndex.findByUuid(linkUuid, filter);

            if (linkHandler == null)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(
                               tc,
                               "createForeignBus",
                               "Link not found");

                SIMPNotPossibleInCurrentConfigurationException e =
                                new SIMPNotPossibleInCurrentConfigurationException(
                                                nls_cwsik.getFormattedMessage(
                                                                              "DELIVERY_ERROR_SIRC_30", // LINK_NOT_FOUND_ERROR_CWSIP0047
                                                                              new Object[] { linkName, stringLinkUuid, foreignBusDefinition.getName() }, null));

                e.setExceptionReason(SIRCConstants.SIRC0030_LINK_NOT_FOUND_ERROR);
                e.setExceptionInserts(new String[] { linkName, stringLinkUuid, foreignBusDefinition.getName() });

                throw e;
            }

            // Create a new BusHandler, which is created locked
            busHandler =
                            new BusHandler(
                                            foreignBusDefinition,
                                            messageProcessor,
                                            this,
                                            durableSubscriptions,
                                            (LinkHandler) linkHandler);

            ForeignBusIndex.Type type = new ForeignBusIndex.Type();
            type.state = State.ACTIVE;
            foreignBusIndex.put(busHandler, type);

            // As a Bus is an extension of an Alias destination, register control adapter here
            busHandler.registerControlAdapters();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createForeignBus", busHandler);

        return busHandler;
    }

    /**
     * Method findBus.
     *
     * @param busName
     * @return busHandler
     * @throws SIDestinationNotFoundException
     * @throws SIMPNullParameterException
     *             <p>This method provides lookup of a foreign bus by its name.
     *             If the bus is not known to the ME, the method queries
     *             admin to see if the bus is a known foreign bus, if the
     *             address can still not be found the method throws
     *             SIDestinationNotFoundException.</p>
     */
    public BusHandler findBus(String busName) throws SIResourceException, SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "findBus", new Object[] { busName });

        // Get the bus
        ForeignBusTypeFilter filter = new ForeignBusTypeFilter();
        filter.VISIBLE = Boolean.TRUE;
        BusHandler busHandler = (BusHandler) foreignBusIndex.findByName(busName, filter);

        // If the bus was not found in DestinationLookups, ask Admin
        if (busHandler == null)
        {
            busHandler = findBusInternal(busName);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "findBus", busHandler);

        return busHandler;
    }

    /**
     * <p>Look up the named foreign bus definition from admin. If found, check the
     * link to the foreign bus is defined, then create a BusHandler to represent the
     * foreign bus and a link handler to represent the link, if one does not already
     * exist.</p>
     *
     * @param busName
     * @return
     */
    private BusHandler findBusInternal(String busName) throws SIResourceException, SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "findBusInternal", new Object[] { busName });

        BusHandler busHandler = null;

        //Get the bus definition from admin
        ForeignBusDefinition foreignBusDefinition = messageProcessor.getForeignBus(busName);

        // Check that the bus exists
        checkBusExists(foreignBusDefinition != null, busName, false, null);

        VirtualLinkDefinition virtualLinkDefinition = null;

        synchronized (linkIndex)
        {
            // Find the link for the bus
            try
            {
                virtualLinkDefinition = foreignBusDefinition.getLinkForNextHop();
            } catch (SIBExceptionBusNotFound e)
            {
                // No FFDC code needed

                checkBusExists(false, busName, false, e);
            } catch (SIBExceptionNoLinkExists e)
            {
                // No FFDC code needed

                checkBusExists(false, busName, true, e);
            }

            //Check that the VLD has localising MEs. If not, then return now with
            // an empty handler.
            Set localizingMEs = null;
            if (virtualLinkDefinition != null)
                localizingMEs = virtualLinkDefinition.getLinkLocalitySet();
            if (localizingMEs == null || localizingMEs.isEmpty())
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Empty/null localizing ME set");

                //return null busHandler
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "findBusInternal", "null");
                return null;
            }

            LinkTypeFilter filter = new LinkTypeFilter();
            filter.VISIBLE = Boolean.TRUE;
            LinkHandler linkHandler = (LinkHandler) linkIndex.findByName(virtualLinkDefinition.getName(), filter);

            if (linkHandler == null)
            {
                // Create a local UOW
                LocalTransaction siTran = txManager.createLocalTransaction(false);

                // Try to create the remote link handler.
                try
                {
                    linkHandler = createRemoteLink(virtualLinkDefinition, siTran);

                    // If everything was succesful then commit the unit of work
                    siTran.commit();
                } catch (SIException e)
                {
                    // No FFDC code needed

                    SibTr.exception(tc, e);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "findBusInternal", e);

                    handleRollback(siTran);

                    SIMPResourceException ee = new SIMPResourceException(e);
                    ee.setExceptionReason(SIRCConstants.SIRC0901_INTERNAL_MESSAGING_ERROR);
                    ee.setExceptionInserts(new String[] { "com.ibm.ws.sib.processor.impl.DestinationManager",
                                                         "1:6515:1.508.1.7",
                                                         SIMPUtils.getStackTrace(e) });
                    throw ee;
                }
            }

            // Try to create the bus handler.
            try
            {
                busHandler =
                                createForeignBus(linkHandler.getName(), linkHandler.getUuid().toString(), foreignBusDefinition);
            } catch (SIException e)
            {
                // No FFDC code needed
                SibTr.exception(tc, e);

                // TO DO - handle this
                // throw e;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "findBusInternal", busHandler);

        return busHandler;
    }

    /**
     * Create an alias destination when passed an appropriate
     * and a valid targetDestinationHandler.
     * <p>
     * Assumes that the destination to create does not already exist.
     *
     * @param destinationDefinition
     * @param destinationLocalizingMEs
     * @param busName
     */
    private DestinationHandler createAliasDestination(DestinationAliasDefinition add, AliasChainValidator validator, DestinationHandler baseDest) throws SINotPossibleInCurrentConfigurationException, SIMPDestinationCorruptException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createAliasDestination", new Object[] { add, validator, baseDest });

        String destinationName = add.getName();
        String busName = add.getBus();

        // Through the admin panels it is possible for an empty string to be specified.
        // An empty string means use the local bus name.
        if (busName == null || busName.equals(""))
            busName = messageProcessor.getMessagingEngineBus();

        // If the validator is null, we must be at the top of any alias chain.
        if (null == validator)
        {
            validator = new AliasChainValidator();
        }

        // Check that we are not going around in a loop with alias validation.
        validator.validate(destinationName, busName);

        AliasDestinationHandler aliasDestinationHandler = null;

        try
        {
            boolean aliasChainError = false;

            // If we are trying to plug in a destination handler rather than look it up then we need
            // to check that we are not an alias chain. If we are we recursively call into the chain
            // handing the plugin destination to the last one.
            if (baseDest != null)
            {
                String targetBus = add.getTargetBus();
                if (targetBus == null || targetBus.equals(""))
                    targetBus = messageProcessor.getMessagingEngineBus();

                // If the name of the current alias target is not the same as the plugin basedest, then
                // we are an alias chaing and we create the next part of the chain passing in the basedest
                if (!add.getTargetName().equals(baseDest.getName()) || !targetBus.equals(baseDest.getBus()))
                {
                    baseDest = getAliasDestinationInternal(add.getTargetName(), targetBus, validator, baseDest, false);
                    if (baseDest == null)
                        aliasChainError = true;
                }
            }

            //The lock on the destinationManager is taken to stop 2 threads creating the same
            //destination and also to synchronize dynamic deletes with the
            //creation of aliases.  This stops an alias destination being created that targets a
            //destination in the process of being deleted.
            synchronized (this)
            {
                DestinationHandler targetDH = baseDest;

                if (targetDH == null && !aliasChainError)
                {

                    targetDH =
                                    getDestinationInternal(
                                                           add.getTargetName(),
                                                           add.getTargetBus(),
                                                           validator,
                                                           false,
                                                           false);

                }

                if (targetDH == null)
                {
                    // Throw out exception detailing chain
                    // Add the destination name which has triggered the exception to the end
                    // of the list, making the problem obvious.
                    String chainText = validator.toStringPlus(add.getTargetName(), busName);

                    String targetName = add.getTargetName();
                    AliasChainValidator.CompoundName firstInChain = validator.getFirstInChain();

                    SIMPNotPossibleInCurrentConfigurationException e =
                                    new SIMPNotPossibleInCurrentConfigurationException(
                                                    nls_cwsik.getFormattedMessage(
                                                                                  "DELIVERY_ERROR_SIRC_45", // ALIAS_TARGET_DESTINATION_NOT_FOUND_EXCEPTION_CWSIP0057
                                                                                  new Object[]
                                                                                  { firstInChain.getDestName(), targetName },
                                                                                  null)
                                    );

                    e.setExceptionReason(SIRCConstants.SIRC0028_ALIAS_TARGET_DESTINATION_NOT_FOUND_EXCEPTION);
                    e.setExceptionInserts(new String[] { firstInChain.getDestName(), targetName });

                    SibTr.exception(tc, e);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "createAliasDestination", e);
                    throw e;
                }
                else if (targetDH.getDestinationType().equals(DestinationType.SERVICE))
                {
                    // Throw out exception detailing chain
                    // Add the destination name which has triggered the exception to the end
                    // of the list, making the problem obvious.
                    String chainText = validator.toStringPlus(targetDH.getName(), targetDH.getBus());

                    AliasChainValidator.CompoundName firstInChain = validator.getFirstInChain();

                    String nlsMessage =
                                    nls_cwsik.getFormattedMessage(
                                                                  "DELIVERY_ERROR_SIRC_29", // ALIAS_TARGETS_SERVICE_DESTINATION_ERROR_CWSIP0622
                                                                  new Object[]
                                                                  { firstInChain.getDestName(), targetDH.getName(), chainText, busName },
                                                                  null);

                    SIMPNotPossibleInCurrentConfigurationException e = new SIMPNotPossibleInCurrentConfigurationException(nlsMessage);

                    e.setExceptionReason(SIRCConstants.SIRC0029_ALIAS_TARGETS_SERVICE_DESTINATION_ERROR);
                    e.setExceptionInserts(new String[] { firstInChain.getDestName(), targetDH.getName(), chainText, busName });

                    SibTr.exception(tc, e);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "createAliasDestination", e);
                    throw e;
                }

                // Create a new DestinationHandler, which is created locked
                aliasDestinationHandler = new AliasDestinationHandler(add, messageProcessor, this, targetDH, busName);

                DestinationIndex.Type type = new DestinationIndex.Type();
                type.alias = Boolean.TRUE;
                type.foreignDestination = Boolean.FALSE;
                type.local = Boolean.FALSE;
                type.remote = Boolean.FALSE;
                type.queue = new Boolean(!aliasDestinationHandler.isPubSub());
                type.state = State.ACTIVE;
                destinationIndex.put(aliasDestinationHandler, type);
            }
        } catch (SITemporaryDestinationNotFoundException e)
        {
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.DestinationManager.createAliasDestination",
                                        "1:6701:1.508.1.7",
                                        this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "createAliasDestination", "SIErrorException");

            SIMPErrorException ee = new SIMPErrorException(
                            nls_cwsik.getFormattedMessage(
                                                          "DELIVERY_ERROR_SIRC_901",
                                                          new Object[] { "com.ibm.ws.sib.processor.impl.DestinationManager", "1:6710:1.508.1.7", e, destinationName },
                                                          null),
                            e);

            ee.setExceptionReason(SIRCConstants.SIRC0901_INTERNAL_MESSAGING_ERROR);
            ee.setExceptionInserts(new String[] { "com.ibm.ws.sib.processor.impl.DestinationManager",
                                                 "1:6716:1.508.1.7",
                                                 SIMPUtils.getStackTrace(e) });
            throw ee;
        } catch (SIIncorrectCallException e)
        {
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.DestinationManager.createAliasDestination",
                                        "1:6166:1.487",
                                        this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "createAliasDestination", "SIErrorException");

            SIMPErrorException ee = new SIMPErrorException(
                            nls_cwsik.getFormattedMessage(
                                                          "DELIVERY_ERROR_SIRC_901",
                                                          new Object[] { "com.ibm.ws.sib.processor.impl.DestinationManager", "1:6175:1.487", e, destinationName },
                                                          null),
                            e);

            ee.setExceptionReason(SIRCConstants.SIRC0901_INTERNAL_MESSAGING_ERROR);
            ee.setExceptionInserts(new String[] { "com.ibm.ws.sib.processor.impl.DestinationManager",
                                                 "1:6181:1.487",
                                                 SIMPUtils.getStackTrace(e) });
            throw ee;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createAliasDestination", aliasDestinationHandler);

        return aliasDestinationHandler;
    }

    /**
     * Create a foreign destination when passed an appropriate
     * and a valid targetDestinationHandler.
     * <p>
     * Assumes that the destination to create does not already exist.
     *
     * @param destinationDefinition
     * @param destinationLocalizingMEs
     * @param busName
     * @return
     */
    private DestinationHandler createForeignDestination(DestinationForeignDefinition dfd, String busName) throws SIResourceException, SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createForeignDestination", new Object[] { dfd, busName });

        ForeignDestinationHandler fdh = null;

        //The lock on the destinationManager is taken to stop 2 threads creating the same
        //destination and also to synchronize dynamic deletes with the
        //creation of aliases.  This stops an alias destination being created that targets a
        //destination in the process of being deleted.
        synchronized (this)
        {
            // Create a new DestinationHandler, which is created locked
            fdh = new ForeignDestinationHandler(dfd, messageProcessor, this, findBus(dfd.getBus()), busName);

            DestinationIndex.Type type = new DestinationIndex.Type();
            type.foreignDestination = Boolean.TRUE;
            type.alias = Boolean.FALSE;
            type.local = Boolean.FALSE;
            type.remote = Boolean.FALSE;
            type.queue = new Boolean(!fdh.isPubSub());
            type.state = State.ACTIVE;
            destinationIndex.put(fdh, type);

            fdh.registerControlAdapters();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createForeignDestination", fdh);

        return fdh;
    }

    protected boolean isReconciling()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "isReconciling");
            SibTr.exit(tc, "isReconciling", new Boolean(reconciling));
        }

        return reconciling;
    }

    /**
     * <p>This method is used to create a link that is localised on this ME.</p>
     *
     * @param linkLocalizationDefinition
     * @param virtualLinkDefinition
     * @param linkLocalizingMEs
     */
    public void createMQLinkLocalization(
                                         MQLinkDefinition mqld,
                                         LocalizationDefinition linkLocalizationDefinition,
                                         VirtualLinkDefinition virtualLinkDefinition,
                                         Set<String> linkLocalizingMEs)
                    throws SIIncorrectCallException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "createMQLinkLocalization",
                        new Object[] { mqld, linkLocalizationDefinition, virtualLinkDefinition, linkLocalizingMEs });

        boolean linkCreated = false;
        boolean engineResourcesRequired = false;
        MQLinkHandler mqLinkHandler = null;
        // Create a local UOW
        LocalTransaction transaction = txManager.createLocalTransaction(false);

        // Try to create the local destination.
        try
        {
            DestinationHandler dh = linkIndex.findByName(virtualLinkDefinition.getName(), null);

            if (dh == null)
            {
                // Create a new LinkHandler, which is created locked
                mqLinkHandler = createMQLinkHandler(mqld, virtualLinkDefinition, transaction);
                linkCreated = true;
            }
            else
            {
                mqLinkHandler = (MQLinkHandler) dh;
            }

            synchronized (mqLinkHandler)
            {
                // If the link is corrupt, do not attempt any update.
                if (!mqLinkHandler.isCorruptOrIndoubt())
                {
                    engineResourcesRequired = createMQLinkMPResources(mqLinkHandler,
                                                                      mqld,
                                                                      linkLocalizationDefinition,
                                                                      virtualLinkDefinition,
                                                                      linkLocalizingMEs,
                                                                      transaction,
                                                                      linkCreated);
                }

                // If everything was successful then commit the unit of work
                transaction.commit();

                // Now we tell the MQLink component to go ahead and create
                // the MQLink infrastructure, if appropriate
                if (!mqLinkHandler.isCorruptOrIndoubt())
                {
                    if (engineResourcesRequired)
                        createMQLinkEngineResources(mqld, mqLinkHandler);

                    //Whether we are reusing the Handler or not, we are now ready to
                    // register the link with WLM
                    mqLinkHandler.registerLink();
                }
            } // eof synchronized on MQLinkHandle
        } catch (RuntimeException e)
        {
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.DestinationManager.createMQLinkLocalization",
                                        "1:6885:1.508.1.7",
                                        this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                SibTr.exception(tc, e);
                SibTr.exit(tc, "createMQLinkLocalization", e);
            }

            handleRollback(transaction);

            // PK69423 Put the link in-doubt, so we do not delete it when we reconcile
            putLinkIntoIndoubtState(virtualLinkDefinition.getName());

            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createMQLinkLocalization");
    }

    /**
     * <p>This method is used to create a new MQLinkHandler and register it.</p>
     */
    private MQLinkHandler createMQLinkHandler(
                                              MQLinkDefinition mqld,
                                              VirtualLinkDefinition virtualLinkDefinition,
                                              LocalTransaction transaction) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "createMQLinkHandler",
                        new Object[] { mqld, virtualLinkDefinition, transaction });

        MQLinkHandler mqLinkHandler = null;

        // Create a new LinkHandler, which is created locked
        try
        {
            mqLinkHandler =
                            new MQLinkHandler(
                                            mqld,
                                            virtualLinkDefinition,
                                            messageProcessor,
                                            this,
                                            transaction,
                                            durableSubscriptions);
        } catch (OutOfCacheSpace e)
        {
            // No FFDC code needed
            SibTr.exception(tc, e);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "createMQLinkHandler", "SIResourceException");
            throw new SIResourceException(e);
        } catch (MessageStoreException e)
        {
            // MessageStoreException shouldn't occur so FFDC.
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.DestinationManager.createMQLinkHandler",
                                        "1:6948:1.508.1.7",
                                        this);

            SibTr.exception(tc, e);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "createMQLinkHandler", "SIResourceException");
            throw new SIResourceException(e);
        }

        mqLinkHandler.setLocal();
        LinkIndex.Type type = new LinkIndex.Type();
        type.local = Boolean.TRUE; //new Boolean(mqLinkHandler.hasLocal());
        type.mqLink = Boolean.TRUE;
        type.remote = Boolean.FALSE;
        type.state = State.ACTIVE;
        linkIndex.put(mqLinkHandler, type);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createMQLinkHandler", mqLinkHandler);

        return mqLinkHandler;
    }

    /**
     * <p>This method is used to create the MQLink resources for a link that is localised on this ME.</p>
     */
    private boolean createMQLinkMPResources(
                                            MQLinkHandler mqLinkHandler,
                                            MQLinkDefinition mqld,
                                            LocalizationDefinition linkLocalizationDefinition,
                                            VirtualLinkDefinition virtualLinkDefinition,
                                            Set<String> linkLocalizingMEs,
                                            LocalTransaction transaction,
                                            boolean linkCreated) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "createMQLinkMPResources",
                        new Object[] { mqLinkHandler,
                                      mqld,
                                      linkLocalizationDefinition,
                                      virtualLinkDefinition,
                                      linkLocalizingMEs,
                                      transaction,
                                      new Boolean(linkCreated) });

        boolean engineResourcesRequired = false;

        //For MQLinks, we want the QueueHigh messages to be the same as for
        //transmit queues.
        linkLocalizationDefinition.setDestinationHighMsgs(messageProcessor.getHighMessageThreshold());
        linkLocalizationDefinition.setDestinationLowMsgs((messageProcessor.getHighMessageThreshold() * 8) / 10);

        //If we didnt just create the linkHandler and the one that already
        //existed is for a different UUID, or there is already a localisation for
        //the linkHandler on this ME, then this could be a problem and further
        //tests are required.
        if (!linkCreated)
        {
            /*
             * 182456.6
             * For the move to WCCM, createDestination can be called for
             * destinations in WCCM that the message processor already
             * holds state about. This is ok as long as the destination has
             * not already been reconciled.
             */
//      if (mqLinkHandler.isReconciled())
//      {
//        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
//          SibTr.exit(
//            tc,
//            "createMQLinkMPResources",
//            "Link already exists");
//
//        throw new SIMPDestinationAlreadyExistsException(
//          nls.getFormattedMessage(
//            "LINK_ALREADY_EXISTS_ERROR_CWSIP0043",
//            new Object[] { virtualLinkDefinition.getName(),
//                           messageProcessor.getMessagingEngineName()},
//            null));
//      }

            // Handle uuid changes
            if (!(mqld.getUuid().equals(mqLinkHandler.getMqLinkUuid())))
            {
                // Need to set the toBeDeleted flag in the "old" handler
                // and set up a new handler
                deleteRecreateMQLinkHandler(
                                            mqld,
                                            mqLinkHandler,
                                            virtualLinkDefinition,
                                            linkLocalizationDefinition,
                                            transaction);

            }
            else
            {
                /*
                 * Link already exists. In the new WCCM world, the
                 * create should complete succesfully, with the new link
                 * definition replacing the existing one.
                 */
                mqLinkHandler.updateLinkDefinition(virtualLinkDefinition, transaction);

                // Flag that we need to tell the MQLink component to create its MQLink infrastructure.
                engineResourcesRequired = true;

                if (linkLocalizingMEs == null)
                {
                    linkLocalizingMEs = new HashSet<String>();
                    linkLocalizingMEs.add(messageProcessor.getMessagingEngineUuid().toString());
                }

                //        Alert the lookups object to handle the re-definition
                linkIndex.setLocalizationFlags(mqLinkHandler, true, false);

                //MQLinks have a real localisation associated with them, so it can
                //be updated here
                mqLinkHandler.updateLocalizationDefinition(linkLocalizationDefinition, transaction);

                // Alert the lookups object to handle the re-definition
                linkIndex.create(mqLinkHandler);

            }
        }
        else // we did just create the linkHandler.
        {
            //First add the localization for the MQlink
            mqLinkHandler.addNewMQLinkLocalisation(
                                                   transaction,
                                                   messageProcessor.getMessagingEngineUuid(),
                                                   linkLocalizationDefinition);

            // Flag that we need to tell the MQLink component to create its MQLink infrastructure.
            engineResourcesRequired = true;

        } // eof we did just create the linkHandler

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createMQLinkMPResources", new Boolean(engineResourcesRequired));

        return engineResourcesRequired;
    }

    /**
     * <p>This method calls out to the MQLink component to tell it to create resources
     * for a new MQLink.</p>
     */
    private void createMQLinkEngineResources(
                                             MQLinkDefinition mqld,
                                             MQLinkHandler mqLinkHandler)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "createMQLinkEngineResources",
                        new Object[] { mqld, mqLinkHandler });

        // Call the MQLinkManager to create the MQLink infrastructure.
        MQLinkManager mqlinkManager = getMQLinkManager();

        // Call MQLink Component
        MQLinkObject mqlinkObj = null;
        try
        {
            mqlinkObj = mqlinkManager.create(mqld, // MQLinkDefinition is a new Admin wrapper class
                                             (MQLinkLocalization) mqLinkHandler,
                                             (ControllableRegistrationService) messageProcessor.getMEInstance(SIMPConstants.JS_MBEAN_FACTORY),
                                             false); // Create full "live" link

            // Notify the MQLink component that mp has already started in the dynamic case
            // but not if this is part of startup processing
            if (messageProcessor.isStarted())
                mqlinkObj.mpStarted(JsConstants.ME_START_DEFAULT,
                                    messageProcessor.getMessagingEngine());
        } catch (SIResourceException e)
        {
            // No FFDC code needed

            SibTr.exception(tc, e);
        } catch (SIException e)
        {
            // No FFDC code needed

            SibTr.exception(tc, e);
        }
        // Store mqlinkObj in the handler
        mqLinkHandler.setMQLinkObject(mqlinkObj);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createMQLinkEngineResources");
    }

    /**
     * <p>This method is used in the deletion/recreeation of an MQ link handler.</p>
     */
    private void deleteRecreateMQLinkHandler(
                                             MQLinkDefinition mqld,
                                             LinkHandler oldLinkHandler,
                                             VirtualLinkDefinition virtualLinkDefinition,
                                             LocalizationDefinition linkLocalizationDefinition,
                                             LocalTransaction transaction) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "deleteRecreateMQLinkHandler",
                        new Object[] { mqld, oldLinkHandler, virtualLinkDefinition, linkLocalizationDefinition, transaction });

        MQLinkHandler newLinkHandler = null;
        // Need to set the toBeDeleted flag in the "old" handler
        oldLinkHandler.setToBeDeleted(true);

        //  Retain links for lookup by uuid so that inbound
        //messages from other ME's can still be processed.
        linkIndex.delete(oldLinkHandler);

        Transaction msTran = txManager.resolveAndEnlistMsgStoreTransaction(transaction);

        try
        {
            oldLinkHandler.requestUpdate(msTran);
        } catch (MessageStoreException e)
        {
            // MessageStoreException shouldn't occur so FFDC.
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.DestinationManager.deleteRecreateMQLinkHandler",
                                        "1:7180:1.508.1.7",
                                        this);

            SibTr.exception(tc, e);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "deleteRecreateMQLinkHandler", "SIResourceException");
            throw new SIResourceException(e);
        }

        // Create a new DestinationHandler, which is created locked
        try
        {
            newLinkHandler =
                            new MQLinkHandler(mqld,
                                            virtualLinkDefinition,
                                            messageProcessor,
                                            this,
                                            transaction,
                                            durableSubscriptions);
        } catch (OutOfCacheSpace e)
        {
            // No FFDC code needed
            SibTr.exception(tc, e);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "deleteRecreateMQLinkHandler", "SIResourceException");
            throw new SIResourceException(e);
        } catch (MessageStoreException e)
        {
            // MessageStoreException shouldn't occur so FFDC.
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.DestinationManager.deleteRecreateMQLinkHandler",
                                        "1:7214:1.508.1.7",
                                        this);

            SibTr.exception(tc, e);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "deleteRecreateMQLinkHandler", "SIResourceException");
            throw new SIResourceException(e);
        }

        LinkIndex.Type type = new LinkIndex.Type();
        type.local = Boolean.TRUE;
        type.mqLink = Boolean.TRUE;
        type.remote = Boolean.FALSE;
        type.state = State.ACTIVE;

        // Set up infrastructure for new destination
        linkIndex.put(newLinkHandler, type);

        // Create a new point to point localisation
        newLinkHandler.addNewMQLinkLocalisation(
                                                transaction,
                                                messageProcessor.getMessagingEngineUuid(),
                                                linkLocalizationDefinition);

        // Associate the mqlinkObj in the  "old" handler with the new one
        newLinkHandler.setMQLinkObject(((MQLinkHandler) (oldLinkHandler)).getMQLinkObject());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deleteRecreateMQLinkHandler", newLinkHandler);

        return;
    }

    public DestinationIndex getDestinationIndex()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getDestinationIndex");
            SibTr.exit(tc, "getDestinationIndex");
        }
        return destinationIndex;
    }

    public LinkIndex getLinkIndex()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getLinkIndex");
            SibTr.exit(tc, "getLinkIndex");
        }

        return linkIndex;
    }

    public ForeignBusIndex getForeignBusIndex()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getForeignBusIndex");
            SibTr.exit(tc, "getForeignBusIndex");
        }

        return foreignBusIndex;
    }

    /**
     * @see com.ibm.wsspi.sib.core.SICoreConnection#addDestinationListener(java.lang.String, com.ibm.ws.sib.processor.DestinationListener, com.ibm.wsspi.sib.core.DestinationType,
     *      com.ibm.ws.sib.processor.DestinationAvailability)
     * @throws SIDiscriminatorSyntaxException
     */
    protected SIDestinationAddress[] addDestinationListener(
                                                            String destinationNamePatternString,
                                                            DestinationListener destinationListener,
                                                            DestinationType destinationType,
                                                            DestinationAvailability destinationAvailability,
                                                            SICoreConnection connection) throws SIDiscriminatorSyntaxException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(
                        tc,
                        "addDestinationListener",
                        new Object[] { destinationNamePatternString, destinationListener, destinationType, destinationAvailability, connection });
        }

        SIDestinationAddress[] destinationAddresses = null;
        DestinationNamePattern destinationNamePattern = null;

        synchronized (this)
        {
            // Has a pattern been specified?
            if (destinationNamePatternString != null && destinationNamePatternString.trim().length() != 0)
            {
                // Instantiate a new DestinationNamePattern object
                destinationNamePattern =
                                new DestinationNamePattern(destinationNamePatternString,
                                                messageProcessor.getMessageProcessorMatching());

                // If the pattern is wildcarded then we can do some matchspace work up front. This is
                // what the prepare() method does.
                destinationNamePattern.prepare();
            }

            // Add the destinationListener to our list
            DestinationListenerDataObject listenerDataObject =
                            new DestinationListenerDataObject(destinationListener,
                                            destinationNamePattern,
                                            destinationType,
                                            destinationAvailability,
                                            connection);
            destinationListeners.add(listenerDataObject);

            if (!(destinationType == com.ibm.wsspi.sib.core.DestinationType.TOPICSPACE))
            {
                //Now compile a array of DestinationAddresses that match the destination type
                // and destination availability
                destinationAddresses = getDestinationAddresses(destinationNamePattern, destinationType, destinationAvailability);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.exit(tc, "addDestinationListener", new Object[] { destinationAddresses });
        }
        return destinationAddresses;
    }

    /*
     * This method supports the Core SPI implementation of addDestinationListener
     * which includes a destinationNamePattern parameter. It returns a list of
     * destination addresses that match the pattern, the destination availability
     * and the destination type.
     *
     * It is assumed that the addresses returned will be an array of either queue,
     * port or service destinations and not topicspace destinations.
     */
    private SIDestinationAddress[] getDestinationAddresses(
                                                           DestinationNamePattern destinationNamePattern,
                                                           DestinationType destinationType,
                                                           DestinationAvailability destinationAvailability)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc,
                        "getDestinationAddresses",
                        new Object[] { destinationNamePattern, destinationType, destinationAvailability });

        BaseDestinationHandler destinationHandler = null;
        SIDestinationAddress[] destinationAddresses = null;

        DestinationTypeFilter filter = new DestinationTypeFilter();
        filter.LOCAL = Boolean.TRUE;
        filter.FOREIGN_DESTINATION = Boolean.FALSE;
        filter.ALIAS = Boolean.FALSE;
        filter.QUEUE = Boolean.TRUE; //Any destination type apart from topicspace is treated as a queue
        filter.VISIBLE = Boolean.TRUE;

        SIMPIterator iterator = destinationIndex.iterator(filter);
        int i = 0;
        List<String> addresses = new Vector<String>();

        while (iterator.hasNext())
        {
            destinationHandler = (BaseDestinationHandler) iterator.next();
            if (isAvailable(destinationHandler, destinationAvailability) && (!(destinationHandler.isSystem())))
            {
                // Check that the destination is the type that we want
                if (destinationHandler.getDestinationType() == destinationType)
                {
                    String destinationName = destinationHandler.getName();

                    // Check whether the user has specified a pattern to match against.
                    if (destinationNamePattern == null || destinationNamePattern.match(destinationName))
                    {
                        // Add this destination to the Address list.
                        addresses.add(i, destinationName);
                        i++;
                    }
                }
            }
        }

        destinationAddresses = new SIDestinationAddress[addresses.size()];
        for (int cursor = 0; cursor < addresses.size(); cursor++)
        {
            // Create the new SIDestinationAddress
            destinationAddresses[cursor] =
                            ((SIDestinationAddressFactory) MessageProcessor.getSingletonInstance(
                                            SIMPConstants.SI_DESTINATION_ADDRESS_FACTORY)).createSIDestinationAddress(
                                                                                                                      addresses.get(cursor),
                                                                                                                      true);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getDestinationAddresses", new Object[] { destinationAddresses });
        return destinationAddresses;
    }

    /*
     * This method will check whether a destination is available.
     */
    private boolean isAvailable(
                                BaseDestinationHandler destinationHandler,
                                DestinationAvailability destinationAvailability)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "isAvailable", new Object[] { destinationHandler, destinationAvailability });

        boolean sendAvailable = false;
        boolean receiveAvailable = false;

        if (destinationAvailability == DestinationAvailability.SEND
            || destinationAvailability == DestinationAvailability.SEND_AND_RECEIVE)
        {
            // Destination must be a non-mediated destination with a local queue point
            if (destinationHandler.hasLocal())
            {
                sendAvailable = true;
            }

            if (sendAvailable)
            {
                if ((destinationHandler.definition.isSendAllowed())
                    && (isLocalizationAvailable(destinationHandler, destinationAvailability)))
                {
                    sendAvailable = true;
                }
                else
                {
                    sendAvailable = false;
                }
            }
        }

        if (destinationAvailability == DestinationAvailability.RECEIVE
            || destinationAvailability == DestinationAvailability.SEND_AND_RECEIVE)
        {
            // Destination must have a queue point on the ME to which the connection is connected
            if (destinationHandler.hasLocal())
            {
                receiveAvailable = true;
            }

            if (receiveAvailable)
            {
                if ((destinationHandler.definition.isReceiveAllowed())
                    && (isLocalizationAvailable(destinationHandler, destinationAvailability)))
                {
                    receiveAvailable = true;
                }
                else
                {
                    receiveAvailable = false;
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "isAvailable", new Object[] { new Boolean(sendAvailable || receiveAvailable) });

        return (sendAvailable || receiveAvailable);
    }

    /**
     * This method determines whether a localization for a destination is available.
     */
    private boolean isLocalizationAvailable(
                                            BaseDestinationHandler baseDestinationHandler,
                                            DestinationAvailability destinationAvailability)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "isLocalizationAvailable", new Object[] { baseDestinationHandler, destinationAvailability });

        boolean available = false;
        if (baseDestinationHandler.hasLocal())
        {
            // Look at the stream for the destination which represents all localizations
            LocalizationPoint localizationPoint = baseDestinationHandler.getQueuePoint(messageProcessor.getMessagingEngineUuid());

            if (destinationAvailability == DestinationAvailability.SEND)
            {
                available = localizationPoint.isSendAllowed();
            }
            else
            {
                available = true;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "isLocalizationAvailable", new Object[] { new Boolean(available) });
        return available;
    }

    /*
     * This method is used in UT only. It removes the destinationListener from the list if the equals method
     * returns true
     */
    protected void removeDestinationListener(DestinationListener destinationListener)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "removeDestinationListener", new Object[] { destinationListener });

        synchronized (this)
        {
            for (int i = 0; i < destinationListeners.size(); i++)
            {
                DestinationListenerDataObject listenerDataObject = destinationListeners.get(i);
                if (destinationListener.equals(listenerDataObject.getDestinationLister()))
                {
                    destinationListeners.remove(listenerDataObject);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "removeDestinationListener");
    }

    /*
     * This method removes the destinationListeners associated with a particular
     * connection.
     */
    protected void removeDestinationListener(SICoreConnection connection)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "removeDestinationListener", new Object[] { connection });

        synchronized (this)
        {
            for (int i = 0; i < destinationListeners.size(); i++)
            {
                DestinationListenerDataObject listenerDataObject = destinationListeners.get(i);
                if (connection.equals(listenerDataObject.getConnection()))
                {
                    destinationListeners.remove(listenerDataObject);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "removeDestinationListener");
    }

    /*
     * This method will call the destination listener if the destination that is created
     * meeting the criteria that the listener has set out.
     */
    private void callDestinationListener(String destinationName) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "callDestinationListener", new Object[] { destinationName });

        DestinationListenerDataObject listenerDataObject = null;
        DestinationListener destinationListener = null;
        DestinationAvailability listenersDestinationAvailability = null;
        SICoreConnection listenersConnection = null;
        DestinationNamePattern destinationNamePattern = null;

        BaseDestinationHandler destinationHandler = null;

        try
        {
            destinationHandler = (BaseDestinationHandler) getDestination(destinationName, false);
        } catch (SIMPDestinationCorruptException e)
        {
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.DestinationManager.callDestinationListener",
                                        "1:7589:1.508.1.7",
                                        this);

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "callDestinationListener", "Destination corrupt");

            // Finding that a restored destination has been marked as corrupt is no
            // reason to cause the transaction to fail.  Andrew Whitfield says that
            // a destination listener will not want to know about corrupt destinations
            // so we end up just returning.
            return;
        } catch (SIException e)
        {
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.DestinationManager.callDestinationListener",
                                        "1:7608:1.508.1.7",
                                        this);

            SibTr.exception(tc, e);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "callDestinationListener", "SIResourceException");
            throw new SIResourceException(e);
        }

        if (!(destinationHandler.definition.getDestinationType() == DestinationType.TOPICSPACE))
        {

            for (int i = 0; i < destinationListeners.size(); i++)
            {
                listenerDataObject = destinationListeners.get(i);
                destinationListener = listenerDataObject.getDestinationLister();
                listenersDestinationAvailability = listenerDataObject.getDestinationAvailability();
                listenersConnection = listenerDataObject.getConnection();
                destinationNamePattern = listenerDataObject.getDestinationNamePattern();

                SIDestinationAddress destinationAddress = null;

                if (isAvailable(destinationHandler, listenersDestinationAvailability)
                    && (!(destinationHandler.isSystem()))
                    && listenerDataObject.getDestinationType() == destinationHandler.definition.getDestinationType())
                {
                    // Check whether a destination name pattern was specified
                    if (destinationNamePattern == null || destinationNamePattern.match(destinationName))
                    {
                        // No pattern or the pattern matches the name.
                        destinationAddress =
                                        (
                                        (SIDestinationAddressFactory) MessageProcessor.getSingletonInstance(
                                                        SIMPConstants.SI_DESTINATION_ADDRESS_FACTORY)).createSIDestinationAddress(
                                                                                                                                  destinationHandler.getName(),
                                                                                                                                  true);
                        // call the listener
                        destinationListener.destinationAvailable(
                                                                 listenersConnection,
                                                                 destinationAddress,
                                                                 listenersDestinationAvailability);
                    }
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "callDestinationListener");
    }

    /**
     * Finds all the JsDestinationAddresses that belong to system destinations for the
     * ME that was passed in.
     *
     * @param meUuid
     * @return List of all the system destination addresses for the meUuid passed
     */
    public List<JsDestinationAddress> getAllSystemDestinations(SIBUuid8 meUuid)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getAllSystemDestinations", meUuid);

        DestinationTypeFilter filter = new DestinationTypeFilter();
        filter.LOCAL = Boolean.FALSE;
        filter.DELETE_PENDING = Boolean.FALSE;
        filter.DELETE_DEFERED = Boolean.FALSE;
        filter.ACTIVE = Boolean.TRUE;

        List<JsDestinationAddress> destAddresses = new ArrayList<JsDestinationAddress>();
        Iterator itr = getDestinationIndex().iterator(filter);
        while (itr.hasNext())
        {
            DestinationHandler destinationHandler = (DestinationHandler) itr.next();
            String destinationHandlerName = destinationHandler.getName();

            SIBUuid8 found = SIMPUtils.parseME(destinationHandlerName);
            if (found == null)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Couldn't parse uuid from " + destinationHandlerName);
            }

            if (found != null
                && found.equals(meUuid))
            {
                if (destinationHandler.isSystem())
                {
                    destAddresses.add(SIMPUtils.createJsDestinationAddress(destinationHandler.getName(), meUuid));
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getAllSystemDestinations", destAddresses);
        return destAddresses;
    }

    //Move the destination into ACTIVE state
    protected void activateDestination(DestinationHandler dh)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "activateDestination", dh);

        if (dh.isLink())
        {
            linkIndex.create(dh);
        }
        else
        {
            destinationIndex.create(dh);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "activateDestination");
    }

    /** PK54812 Move the destination into CORRUPT state */
    protected void corruptDestination(DestinationHandler dh)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "corruptDestination", dh);

        if (!dh.isLink() && destinationIndex.containsDestination(dh))
        {
            destinationIndex.corrupt(dh);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "corruptDestination");
    }

    /**
     * @return
     */
    public MQLinkManager getMQLinkManager()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getMQLinkManager");
        if (_mqlinkManager == null)
            _mqlinkManager = MQLinkManager.getInstance();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getMQLinkManager", _mqlinkManager);
        return _mqlinkManager;
    }

    /*
     * This method is used to obtain a aliasDestinationHandler when the target baseDest does not exist. (i.e.
     * it hasnt been reconstituted fully yet). We therefore take the baseDesthandler as a parameter and use
     * this to construct the alias.
     */
    public DestinationHandler getAliasDestination(SIBUuid12 gatheringTargetUuid, DestinationHandler baseDest, boolean includeInvisible)
                    throws SITemporaryDestinationNotFoundException, SINotPossibleInCurrentConfigurationException, SIMPDestinationCorruptException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getAliasDestination", new Object[] { gatheringTargetUuid, baseDest, Boolean.valueOf(includeInvisible) });

        DestinationHandler destinationHandler = getDestinationInternal(gatheringTargetUuid, includeInvisible);

        try
        {
            if (destinationHandler == null)
            {
                BaseDestinationDefinition bdd = messageProcessor.getMessagingEngine().getSIBDestinationByUuid(messageProcessor.getMessagingEngineBus(),
                                                                                                              gatheringTargetUuid.toString());

                // The returned destination can be remote or an alias.  We need to act
                // accordingly.
                if (bdd.isAlias())
                {
                    // The destination is an alias.
                    DestinationAliasDefinition add = (DestinationAliasDefinition) bdd;

                    destinationHandler = createAliasDestination(add, null, baseDest);
                }
            }
        }

        // Catch Admin's SIBExceptionDestinationNotFound exception
        catch (SIBExceptionDestinationNotFound e)
        {
            // No FFDC code needed

            SibTr.exception(tc, e);

            // Exception not fatal - just return null to show no destiation has been
            // found.
        } catch (SIBExceptionBase e)
        {
            // No FFDC code needed

            SibTr.exception(tc, e);

            // TO DO - handle this
        } catch (SIResourceException e)
        {
            // No FFDC code needed
            if (!(e instanceof SIMPResourceException))
            {
                SIMPResourceException ee = new SIMPResourceException(e);
                ee.setExceptionReason(SIRCConstants.SIRC0901_INTERNAL_MESSAGING_ERROR);
                ee.setExceptionInserts(new String[] { "com.ibm.ws.sib.processor.impl.DestinationManager.getAliasDestination",
                                                     "1:8107:1.508.1.7",
                                                     SIMPUtils.getStackTrace(e) });
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "getAliasDestination", ee);
                throw ee;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "getAliasDestination", e);
            throw e;

        }

        checkDestinationHandlerExists(
                                      destinationHandler != null,
                                      gatheringTargetUuid.toString(),
                                      messageProcessor.getMessagingEngineName());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getAliasDestination", destinationHandler);

        return destinationHandler;
    }

    /*
     * This method is used to obtain a aliasDestinationHandler when the target baseDest does not exist. (i.e.
     * it hasnt been reconstituted fully yet). We therefore take the baseDesthandler as a parameter and use
     * this to construct the alias.
     */
    public DestinationHandler getAliasDestinationInternal(String aliasName, String aliasBus, AliasChainValidator validator, DestinationHandler targetDest, boolean includeInvisible)
                    throws SITemporaryDestinationNotFoundException, SINotPossibleInCurrentConfigurationException, SIMPDestinationCorruptException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getAliasDestination", new Object[] { aliasName, aliasName, targetDest, Boolean.valueOf(includeInvisible) });

        DestinationHandler destinationHandler = getDestinationInternal(aliasName, aliasBus, includeInvisible);

        try
        {
            if (destinationHandler == null)
            {
                BaseDestinationDefinition bdd = messageProcessor.getMessagingEngine().getSIBDestination(aliasBus, aliasName);

                // The returned destination can be remote or an alias.  We need to act
                // accordingly.
                if (bdd.isAlias())
                {
                    // The destination is an alias.
                    DestinationAliasDefinition add = (DestinationAliasDefinition) bdd;

                    destinationHandler = createAliasDestination(add, validator, targetDest);
                }
            }
        }

        // Catch Admin's SIBExceptionDestinationNotFound exception
        catch (SIBExceptionDestinationNotFound e)
        {
            // No FFDC code needed

            SibTr.exception(tc, e);

            // Exception not fatal - just return null to show no destiation has been
            // found.
        } catch (SIBExceptionBase e)
        {
            // No FFDC code needed

            SibTr.exception(tc, e);

            // TO DO - handle this
        } catch (SIResourceException e)
        {
            // No FFDC code needed
            if (!(e instanceof SIMPResourceException))
            {
                SIMPResourceException ee = new SIMPResourceException(e);
                ee.setExceptionReason(SIRCConstants.SIRC0901_INTERNAL_MESSAGING_ERROR);
                ee.setExceptionInserts(new String[] { "com.ibm.ws.sib.processor.impl.DestinationManager.getAliasDestination",
                                                     "1:8188:1.508.1.7",
                                                     SIMPUtils.getStackTrace(e) });
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "getAliasDestination", ee);
                throw ee;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "getAliasDestination", e);
            throw e;

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getAliasDestination", destinationHandler);

        return destinationHandler;
    }

    /*
     * This method is used to alter the alias destination type of liberty profile at runtime.
     *
     * @param destinationAliasDefinition The new updated definition file of Alias destination type
     */
    public void alterDestinationAlias(DestinationAliasDefinition destinationAliasDefinition) throws SIIncorrectCallException, SIResourceException, SINotPossibleInCurrentConfigurationException, SITemporaryDestinationNotFoundException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "alterDestinationAlias", new Object[] { destinationAliasDefinition.getName() });
        // Create a local UOW
        LocalTransaction transaction = txManager.createLocalTransaction(true);

        // Try to alter the local destination.
        try
        {
            DestinationTypeFilter filter = new DestinationTypeFilter();
            filter.LOCAL = Boolean.TRUE;
            filter.VISIBLE = Boolean.TRUE;
            filter.ALIAS = Boolean.TRUE;
            filter.FOREIGN_DESTINATION = Boolean.FALSE;
            filter.QUEUE = Boolean.FALSE;
            filter.CORRUPT = Boolean.FALSE;
            filter.RESET_ON_RESTART = Boolean.FALSE;

            DestinationHandler dh = destinationIndex.findByName(destinationAliasDefinition.getName(), "defaultBus", filter);

            if (dh == null) {
                dh = getDestination(destinationAliasDefinition.getName(), "defaultBus", false, false);
            }

            if (!(dh.isSystem() || dh.isTemporary() || dh.isToBeDeleted() || dh.isDeleted())) //these should be invisible anyway
            {
                dh.updateDefinition(destinationAliasDefinition);
            }
            // If everything was successful then commit the unit of work
            transaction.commit();
        } catch (SIResourceException e)
        {
            // No FFDC code needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "alterDestinationAlias", e);
            handleRollback(transaction);
            throw e;
        } catch (RuntimeException e)
        {
            // FFDC
            FFDCFilter.processException(e, "com.ibm.ws.sib.processor.impl.DestinationManager.alterDestinationAlias", "1:4353:1.508.1.7", this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                SibTr.exception(tc, e);
                SibTr.exit(tc, "alterDestinationAlias", e);
            }
            handleRollback(transaction);
            throw e;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "alterDestinationAlias");
    }
}

/**
 * <p>Runnable class for DeletePubSubMsgsThread thread. This thread get started
 * during start of ME. It deletes pub-sub messages which are not having any
 * references </p>
 * <p> This thread gracefully exits if ME is stopped. Granualarity of graceful
 * exit is upto Message Item level i.e it checks for ME stop flag for each MessageItem
 * when traversing through MessageItems list</p>
 */
class DeletePubSubMsgsThread implements Runnable, StoppableThread {
    /**
     * Initialise trace for the component.
     */
    private static final TraceComponent tc =
                    SibTr.register(DestinationManager.class, SIMPConstants.MP_TRACE_GROUP, SIMPConstants.RESOURCE_BUNDLE);
    private static final TraceComponent tc_cwsik =
                    SibTr.register((new Object() {}).getClass(), SIMPConstants.MP_TRACE_GROUP, SIMPConstants.CWSIK_RESOURCE_BUNDLE);

    /**
     * NLS for component.
     */
    private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
    private static final TraceNLS nls_cwsik =
                    TraceNLS.getTraceNLS(SIMPConstants.CWSIK_RESOURCE_BUNDLE);

    private final MessageProcessor messageProcessor;
    private volatile boolean hasToStop = false;

    /**
     * Create a new Thread to delete pub/sub messages without any references
     *
     * @param mp The 'owning' MP object
     */
    DeletePubSubMsgsThread(MessageProcessor mp)
    {
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "DeletePubSubMsgsThread", new Object[] { mp });

        //store the variables
        messageProcessor = mp;

        messageProcessor.getStoppableThreadCache().registerThread(this);

        if (tc.isEntryEnabled())
            SibTr.exit(tc, "DeletePubSubMsgsThread", this);
    }

    public boolean HasToStop() {
        return hasToStop;
    }

    @Override
    public void run() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "run");

        DestinationManager destinationManager = messageProcessor.getDestinationManager();
        DestinationTypeFilter destFilter = new DestinationTypeFilter();
        destFilter.QUEUE = Boolean.FALSE;
        DestinationIndex destinationIndex = destinationManager.getDestinationIndex();

        /**
         * Parse through all the destinations and call deleteMsgsWithNoReferences for
         * topic destination. Ensure that proper checking is done i.e whether destination
         * is corrupt or deleted. In case if ME stops, run() method gracefully
         * exits.
         */
        SIMPIterator itr = destinationIndex.iterator(destFilter);
        while (itr.hasNext() && !HasToStop()) {
            BaseDestinationHandler dh = (BaseDestinationHandler) itr.next();
            if (dh.isPubSub() && !dh.isCorruptOrIndoubt() && !dh.isDeleted() && !dh.isTemporary()) {
                dh.deleteMsgsWithNoReferences();
            }
        }
        itr.finished();

        //deregistering here itself as this object enable for GC

        if (messageProcessor.getStoppableThreadCache().getThreads().contains(this))
            messageProcessor.getStoppableThreadCache().deregisterThread(this);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "run");
    }

    // This get called from MessageProcessor on ME getting stopped.
    @Override
    public void stopThread(StoppableThreadCache cache) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "stopThread");

        this.hasToStop = true;

        //Remove this thread from the thread cache
        cache.deregisterThread(this);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "stopThread");
    }

}
