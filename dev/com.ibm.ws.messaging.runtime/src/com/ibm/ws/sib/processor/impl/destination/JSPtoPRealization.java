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

package com.ibm.ws.sib.processor.impl.destination;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.BaseLocalizationDefinition;
import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.ws.sib.admin.LocalizationDefinition;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.OutOfCacheSpace;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.AnycastInputHandler;
import com.ibm.ws.sib.processor.impl.AnycastOutputHandler;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.ConsumerDispatcher;
import com.ibm.ws.sib.processor.impl.ConsumerDispatcherState;
import com.ibm.ws.sib.processor.impl.GatheringConsumerDispatcher;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.PtoPInputHandler;
import com.ibm.ws.sib.processor.impl.PtoPOutputHandler;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumerManager;
import com.ibm.ws.sib.processor.impl.interfaces.ControlHandler;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.JSConsumerManager;
import com.ibm.ws.sib.processor.impl.interfaces.LocalizationPoint;
import com.ibm.ws.sib.processor.impl.interfaces.OutputHandler;
import com.ibm.ws.sib.processor.impl.interfaces.PtoPRealization;
import com.ibm.ws.sib.processor.impl.store.filters.ClassEqualsFilter;
import com.ibm.ws.sib.processor.impl.store.itemstreams.MQLinkMessageItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPLocalMsgsItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPXmitMsgsItemStream;
import com.ibm.ws.sib.processor.runtime.impl.MPRuntimeEvent;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;

/**
 * @author nyoung
 * 
 *         <p>The PtoPRealization the PtoP state specific to a BaseDestinationHandler
 *         that represents a Queue.
 */
public class JSPtoPRealization extends AbstractProtoRealization implements PtoPRealization

{
    /**
     * Trace for the component
     */
    private static final TraceComponent tc =
                    SibTr.register(
                                   JSPtoPRealization.class,
                                   SIMPConstants.MP_TRACE_GROUP,
                                   SIMPConstants.RESOURCE_BUNDLE);

    /** NLS for component */
    static final TraceNLS nls =
                    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
    private static final TraceNLS nls_cwsik =
                    TraceNLS.getTraceNLS(SIMPConstants.CWSIK_RESOURCE_BUNDLE);

    /**
     * If the destination is localised on this ME, pToPLocalMsgsItemStream
     * provides a reference to the local localisation.
     */
    PtoPLocalMsgsItemStream _pToPLocalMsgsItemStream = null;

    /**
     * The GatheringConsumerDispatcher for this destination
     */
    private HashMap<SIBUuid12, GatheringConsumerDispatcher> gatheringConsumerDispatcher;

    /**
     * Warm start constructor invoked by the Message Store.
     */
    public JSPtoPRealization()
    {
        super();
    }

    /**
     * <p>Cold start constructor.</p>
     * <p>Create a new instance of a destination, passing in the name of the
     * destination and its definition. A destination represents a topicspace in
     * pub/sub or a queue in point to point.</p>
     * 
     * @param destinationName
     * @param destinationDefinition
     * @param messageProcessor
     * @param parentStream The Itemstream this DestinationHandler should be
     *            added into.
     * @param durableSubscriptionsTable Required only by topicspace
     *            destinations. Can be null if point to point (local or remote).
     * @param busName The name of the bus on which the destination resides
     */
    public JSPtoPRealization(
                             BaseDestinationHandler myBaseDestinationHandler,
                             MessageProcessor messageProcessor,
                             LocalisationManager localisationManager)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "JSPtoPRealization", new Object[] {
                                                               myBaseDestinationHandler, messageProcessor,
                                                               localisationManager });

        _baseDestinationHandler = myBaseDestinationHandler;
        _messageProcessor = messageProcessor;
        _destinationManager = messageProcessor.getDestinationManager();

        // Instantiate DA manager to interface to WLM
        _localisationManager = localisationManager;
        //  Remote
        _localisationManager.initialise(_messageProcessor.isSingleServer());

        _remoteSupport = new RemotePtoPSupport(myBaseDestinationHandler,
                        messageProcessor);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "JSPtoPRealization", this);
    }

    /**
     * Method initialise
     * <p>Initialise the PtoPRealization
     */
    @Override
    public void initialise()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "initialise", this);

        createInputHandlersForPtoP();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "initialise");
    }

    /**
     * Method createInputHandlersForPtoP.
     * <p> Create the PtoP and preMediated InputHandlers
     */
    @Override
    public void createInputHandlersForPtoP()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createInputHandlersForPtoP", this);
        //Create a point to point input handler
        _baseDestinationHandler.
                        setInputHandler(new PtoPInputHandler(_baseDestinationHandler,
                                        _remoteSupport.getTargetProtocolItemStream()));

        //TODO is this a good idea?
        //We don't yet know what type of InputHandler to create

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createInputHandlersForPtoP");
    }

    /**
     * Method reconstitute
     * <p>Recover a BaseDestinationHandler retrieved from the MessageStore.
     * 
     * @param processor
     * @param durableSubscriptionsTable
     * 
     * @throws Exception
     */
    @Override
    public void reconstitute(int startMode,
                             DestinationDefinition definition,
                             boolean isToBeDeleted,
                             boolean isSystem)
                    throws SIDiscriminatorSyntaxException, MessageStoreException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc,
                        "reconstitute",
                        new Object[] {
                                      Integer.valueOf(startMode),
                                      definition,
                                      Boolean.valueOf(isToBeDeleted),
                                      Boolean.valueOf(isSystem),
                                      this });

        _remoteSupport.reconstituteAnycastRMEPhaseOne();
        reconstituteLocalQueuePoint(startMode, isSystem);
        _remoteSupport.reconstituteAnycastRMEPhaseTwo(startMode, definition);
        reconstituteMQLink(startMode);
        reconstituteRemoteQueuePoints(startMode, isSystem);

        if (isToBeDeleted)
            setToBeDeleted();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "reconstitute");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.destination.AbstractProtoRealization#reconstituteEnoughForDeletion()
     */
    @Override
    public void reconstituteEnoughForDeletion()
                    throws
                    MessageStoreException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "reconstituteEnoughForDeletion", this);

        NonLockingCursor cursor =
                        _baseDestinationHandler.newNonLockingItemStreamCursor(
                                        new ClassEqualsFilter(PtoPLocalMsgsItemStream.class));
        PtoPMessageItemStream ptoPLocalMsgsItemStream =
                        (PtoPLocalMsgsItemStream) cursor.next();

        if (null != ptoPLocalMsgsItemStream)
            ptoPLocalMsgsItemStream.reconstitute(_baseDestinationHandler);

        cursor.finished();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "reconstituteEnoughForDeletion");
    }

    /**
     * Reconstitute the MQLink itemstream if it exists.
     * 
     * @return Success of failure of reconstitution of the MQLink itemstream
     */
    private void reconstituteMQLink(int startMode) throws MessageStoreException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "reconstituteMQLink", new Object[] { Integer.valueOf(startMode), this });

        int localisationCount = 0;
        MQLinkMessageItemStream mqlinkMessageItemStream = null;

        // There can be one or more localisations in the BaseDestinationHandler.
        NonLockingCursor cursor =
                        _baseDestinationHandler.newNonLockingItemStreamCursor(
                                        new ClassEqualsFilter(MQLinkMessageItemStream.class));

        do
        {
            mqlinkMessageItemStream = (MQLinkMessageItemStream) cursor.next();

            if (mqlinkMessageItemStream != null)
            {
                localisationCount++;
                mqlinkMessageItemStream.reconstitute(_baseDestinationHandler);

                attachLocalPtoPLocalisation(mqlinkMessageItemStream);

                /* Feature 176658.3.2 */
                //TODO: Check need all this for MQLinks
                assignQueuePointOutputHandler(
                                              mqlinkMessageItemStream.getOutputHandler(),
                                              mqlinkMessageItemStream.getLocalizingMEUuid());
                ConsumerDispatcher consumerDispatcher =
                                (ConsumerDispatcher) mqlinkMessageItemStream.getOutputHandler();
                consumerDispatcher.setReadyForUse();

                //If the local queue point is awaiting deletion ensure it is correctly configured
                if (mqlinkMessageItemStream.isToBeDeleted())
                {
                    dereferenceLocalisation(mqlinkMessageItemStream);
                }
            }
        } while (mqlinkMessageItemStream != null);

        cursor.finished();

        // Sanity - There should never be more than one mediation itemstream
        if (localisationCount > 1)
        {
            SIErrorException e =
                            new SIErrorException(
                                            nls.getFormattedMessage(
                                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0005",
                                                                    new Object[] {
                                                                                  "com.ibm.ws.sib.processor.impl.destination.JSPtoPRealization",
                                                                                  "1:458:1.24.1.7",
                                                                                  _baseDestinationHandler.getName() },
                                                                    null));

            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.destination.JSPtoPRealization.reconstituteMQLink",
                                        "1:454:1.24.1.25",
                                        this);

            SibTr.exception(tc, e);
            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0005",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.destination.JSPtoPRealization",
                                      "1:461:1.24.1.25",
                                      _baseDestinationHandler.getName() });

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "reconstituteMQLink", e);
            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "reconstituteMQLink");
    }

    /**
     * Reconstitute the local queue point if it exists.
     * 
     * @return Success of failure of reconstitution of the local queue point
     */
    private void reconstituteLocalQueuePoint(int startMode,
                                             boolean isSystem)
                    throws MessageStoreException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc,
                        "reconstituteLocalQueuePoint",
                        new Object[] {
                                      Integer.valueOf(startMode),
                                      Boolean.valueOf(isSystem),
                                      this });

        int localisationCount = 0;
        PtoPLocalMsgsItemStream ptoPLocalMsgsItemStream = null;

        // There can be one or more localisations in the BaseDestinationHandler.
        NonLockingCursor cursor =
                        _baseDestinationHandler.newNonLockingItemStreamCursor(
                                        new ClassEqualsFilter(PtoPLocalMsgsItemStream.class));

        do
        {
            ptoPLocalMsgsItemStream = (PtoPLocalMsgsItemStream) cursor.next();
            if (ptoPLocalMsgsItemStream != null)
            {
                ptoPLocalMsgsItemStream.reconstitute(_baseDestinationHandler);
                if (ptoPLocalMsgsItemStream.isToBeDeleted())
                {
                    ptoPLocalMsgsItemStream.setDeleteRequiredAtReconstitute(true);
                    //clean this up when we get a chance
                    _postMediatedItemStreamsRequiringCleanup.put(
                                                                 _messageProcessor.getMessagingEngineUuid(), ptoPLocalMsgsItemStream);
                    _baseDestinationHandler.setHasReconciledStreamsToBeDeleted(true);
                }
                else
                {
                    localisationCount++;
                    attachLocalPtoPLocalisation(ptoPLocalMsgsItemStream);

                    /* Feature 176658.3.2 */
                    assignQueuePointOutputHandler(
                                                  ptoPLocalMsgsItemStream.getOutputHandler(),
                                                  ptoPLocalMsgsItemStream.getLocalizingMEUuid());
                    ConsumerDispatcher consumerDispatcher =
                                    (ConsumerDispatcher) ptoPLocalMsgsItemStream.getOutputHandler();
                    consumerDispatcher.setReadyForUse();

                    //Restore the guess sets for system destinations
                    if (isSystem)
                    {
                        _localisationManager.addMEToQueuePointGuessSet(_pToPLocalMsgsItemStream.getLocalizingMEUuid());
                        ptoPLocalMsgsItemStream.
                                        updateLocalizationDefinition(_messageProcessor.
                                                        createLocalizationDefinition(_baseDestinationHandler.getName()));
                    }
                }
            }
        } while (ptoPLocalMsgsItemStream != null);

        cursor.finished();

        // also check if there is an AOContainerItemStream for Remote Get
        int aoCount = _remoteSupport.reconstituteLocalQueuePoint(startMode);

        // Sanity - There should never be more than one local msgs itemstream
        // or more than one aoContainerItemStream
        if ((localisationCount > 1) || (aoCount > 1))
        {
            SIErrorException e =
                            new SIErrorException(
                                            nls.getFormattedMessage(
                                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0005",
                                                                    new Object[] {
                                                                                  "com.ibm.ws.sib.processor.impl.destination.JSPtoPRealization",
                                                                                  "1:595:1.24.1.7",
                                                                                  _baseDestinationHandler.getName() },
                                                                    null));

            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.destination.JSPtoPRealization.reconstituteLocalQueuePoint",
                                        "1:560:1.24.1.25",
                                        this);

            SibTr.exception(tc, e);
            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0005",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.destination.JSPtoPRealization",
                                      "1:567:1.24.1.25",
                                      _baseDestinationHandler.getName() });

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "reconstituteLocalQueuePoint", e);

            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "reconstituteLocalQueuePoint");
    }

    /**
     * Reconstitute any remote queue points.
     */
    private void reconstituteRemoteQueuePoints(int startMode,
                                               boolean isSystem)
                    throws SIDiscriminatorSyntaxException, MessageStoreException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc,
                        "reconstituteRemoteQueuePoints",
                        new Object[] {
                                      Integer.valueOf(startMode),
                                      Boolean.valueOf(isSystem),
                                      this });

        // Next look for any remote transmission itemstreams to queue points
        int transmissionQCount = 0;
        PtoPXmitMsgsItemStream ptoPXmitMsgsItemStream = null;

        // There can be one or more transmission itemstreams in the BaseDestinationHandler.
        NonLockingCursor cursor =
                        _baseDestinationHandler.newNonLockingItemStreamCursor(
                                        new ClassEqualsFilter(PtoPXmitMsgsItemStream.class));

        do
        {
            ptoPXmitMsgsItemStream = (PtoPXmitMsgsItemStream) cursor.next();

            if (ptoPXmitMsgsItemStream != null)
            {
                ptoPXmitMsgsItemStream.reconstitute(_baseDestinationHandler);

                if (ptoPXmitMsgsItemStream.isToBeDeleted())
                {
                    ptoPXmitMsgsItemStream.setDeleteRequiredAtReconstitute(true);
                    SIBUuid8 remoteMEUuid = ptoPXmitMsgsItemStream.getLocalizingMEUuid();
                    _postMediatedItemStreamsRequiringCleanup.put(
                                                                 remoteMEUuid, ptoPXmitMsgsItemStream);
                    _baseDestinationHandler.setHasReconciledStreamsToBeDeleted(true);
                    _localisationManager.
                                    attachRemotePtoPLocalisation(ptoPXmitMsgsItemStream,
                                                                 _remoteSupport);
                    // Reconstitute source streams for PtoPOutputHandler
                    _remoteSupport.reconstituteSourceStreams(startMode,
                                                             (PtoPOutputHandler) ptoPXmitMsgsItemStream.getOutputHandler());
                    assignQueuePointOutputHandler(
                                                  ptoPXmitMsgsItemStream.getOutputHandler(),
                                                  ptoPXmitMsgsItemStream.getLocalizingMEUuid());
                }
                else
                {
                    transmissionQCount++;
                    _localisationManager.
                                    attachRemotePtoPLocalisation(ptoPXmitMsgsItemStream,
                                                                 _remoteSupport);
                    // Reconstitute source streams for PtoPOutputHandler
                    _remoteSupport.reconstituteSourceStreams(startMode,
                                                             (PtoPOutputHandler) ptoPXmitMsgsItemStream.getOutputHandler());

                    /* Feature 176658.3.2 */
                    assignQueuePointOutputHandler(
                                                  ptoPXmitMsgsItemStream.getOutputHandler(),
                                                  ptoPXmitMsgsItemStream.getLocalizingMEUuid());

                    //Restore the guess sets for system destinations
                    if (isSystem)
                    {
                        _localisationManager.addMEToQueuePointGuessSet(ptoPXmitMsgsItemStream.getLocalizingMEUuid());
                        _localisationManager.addMEToRemoteQueuePointGuessSet(ptoPXmitMsgsItemStream.getLocalizingMEUuid());
                    }
                }
            }
        } while (ptoPXmitMsgsItemStream != null);

        cursor.finished();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "reconstituteRemoteQueuePoints");

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.destination.AbstractProtoRealization#searchLocalPtoPOutputHandler(boolean, boolean, boolean)
     */
    @Override
    public OutputHandler getLocalPostMedPtoPOH(boolean localMessage,
                                               boolean forcePut,
                                               boolean singleServer)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc,
                        "getLocalPostMedPtoPOH",
                        new Object[] { Boolean.valueOf(localMessage),
                                      Boolean.valueOf(forcePut),
                                      Boolean.valueOf(singleServer),
                                      this });

        OutputHandler result = null;

        //check that the localisation is not full or send inhibited
        boolean isSendAllowed = true;
        boolean isAtQueueHighLimit = false;

        //Only check elsewhere in an ND environment.  In a single server
        //environment, the message must be put locally
        if (!singleServer)
        {
            try
            {
                //send allowed can only be false if forcePut is false
                isSendAllowed =
                                forcePut || _pToPLocalMsgsItemStream.isSendAllowed();

                //Only check queue high limit if the message was put locally
                //and if forcePut is false
                if (!forcePut && localMessage)
                {
                    isAtQueueHighLimit = _pToPLocalMsgsItemStream.isQHighLimit();
                }
            } catch (Exception e)
            {
                //No FFDC code needed
                isSendAllowed = true;
                isAtQueueHighLimit = false;
            }
        }

        if (isSendAllowed && !isAtQueueHighLimit)
        {
            result =
                            _localisationManager.
                                            getQueuePointOutputHandler(_messageProcessor.getMessagingEngineUuid());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getLocalPostMedPtoPOH", result);

        return result;
    }

    /**
     * Method getAnycastOutputHandler
     * <p>Called to get the AnycastOutputHandler for this Destination
     * 
     * @return
     */
    @Override
    public final synchronized AnycastOutputHandler getAnycastOutputHandler(DestinationDefinition definition,
                                                                           boolean restartFromStaleBackup)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc,
                        "getAnycastOutputHandler",
                        new Object[] {
                                      definition,
                                      Boolean.valueOf(restartFromStaleBackup),
                                      this });

        AnycastOutputHandler anycastOutputHandler =
                        _remoteSupport.getAnycastOutputHandler(definition, restartFromStaleBackup);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getAnycastOutputHandler", anycastOutputHandler);

        return anycastOutputHandler;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getControlHandler(com.ibm.ws.sib.common.ProtocolType, com.ibm.ws.sib.trm.topology.Cellule)
     */
    @Override
    public ControlHandler getControlHandler(SIBUuid8 sourceMEUuid)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "getControlHandler",
                        new Object[] { sourceMEUuid, this });

        ControlHandler msgHandler =
                        (ControlHandler) _localisationManager.
                                        getQueuePointOutputHandler(sourceMEUuid);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getControlHandler", msgHandler);

        return msgHandler;
    }

    /**
     * Methodget QueuePointOutputHandler
     * <p>Retrieves a queue point output handler
     * 
     * @param SIBUuid8
     * @return OutputHandler if it can be found or null
     */
    @Override
    public OutputHandler getQueuePointOutputHandler(SIBUuid8 meUuid)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getQueuePointOutputHandler", new Object[] { meUuid, this });

        OutputHandler result =
                        _localisationManager.
                                        getQueuePointOutputHandler(meUuid);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getQueuePointOutputHandler", result);
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getLocalPtoPConsumerDispatcher()
     */
    @Override
    public JSConsumerManager getLocalPtoPConsumerManager()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getLocalPtoPConsumerManager", this);

        JSConsumerManager consumerManager = null;

        if (_pToPLocalMsgsItemStream != null)
        {
            consumerManager =
                            (JSConsumerManager) _pToPLocalMsgsItemStream.getOutputHandler();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getLocalPtoPConsumerManager", consumerManager);

        return consumerManager;
    }

    /**
     * Method assignQueuePointOutputHandler.
     * 
     * @param outputHandler
     *            <p>Add the outputHandler to the set of queuePointOutputHanders</p>
     */
    @Override
    public void assignQueuePointOutputHandler(
                                              OutputHandler outputHandler,
                                              SIBUuid8 messagingEngineUuid)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "assignQueuePointOutputHandler",
                        new Object[] { outputHandler, messagingEngineUuid, this });

        _localisationManager.assignQueuePointOutputHandler(outputHandler,
                                                           messagingEngineUuid);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "assignQueuePointOutputHandler");
    }

    /**
     * Method dereferenceLocalisation
     * <p>Called back by a PtoPMessageItemStream when the Transaction containing it commits.
     * Removes the localisation and its associated OutputHandler
     * from the destination.
     * 
     * @param localisation The localisation to dereference.
     */
    @Override
    public void dereferenceLocalisation(LocalizationPoint ptoPMessageItemStream)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "dereferenceLocalisation", new Object[] { ptoPMessageItemStream, this });

        _localisationManager.dereferenceLocalisation(ptoPMessageItemStream);

        // Reset the reference to the local messages itemstream if it is being removed.
        if (ptoPMessageItemStream
                        .getLocalizingMEUuid()
                        .equals(_messageProcessor.getMessagingEngineUuid()))
        {
            _pToPLocalMsgsItemStream = null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "dereferenceLocalisation");
    }

    /**
     * Method localQueuePointRemoved
     * 
     * @param isDeleted
     * @param isSystem
     * @param isTemporary
     * @param messagingEngineUuid
     * @throws SIResourceException
     */
    @Override
    public void localQueuePointRemoved(boolean isDeleted,
                                       boolean isSystem,
                                       boolean isTemporary,
                                       SIBUuid8 messagingEngineUuid)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "localQueuePointRemoved",
                        new Object[] {
                                      Boolean.valueOf(isDeleted),
                                      Boolean.valueOf(isSystem),
                                      Boolean.valueOf(isTemporary),
                                      messagingEngineUuid,
                                      this });

        if (_pToPLocalMsgsItemStream != null)
        {
            // Remove the queuePoint from our queue points guess set, which we use to
            // guess where to send messages if WLM isnt working
            // and update TRM if necessary
            _localisationManager.localQueuePointRemoved(messagingEngineUuid);

            // Add the localisation into the set of those requiring clean-up
            _postMediatedItemStreamsRequiringCleanup.put(messagingEngineUuid, _pToPLocalMsgsItemStream);

            _pToPLocalMsgsItemStream.markAsToBeDeleted(
                            _baseDestinationHandler.getTransactionManager().createAutoCommitTransaction());

            // Close any locally attached consumers - producers are ok as there are
            // other localisations of the destination available.
            if (!_localisationManager.hasLocal())
                _remoteSupport.closeConsumers();

            // Dereference the localisation from the destinationHandler so that
            // if the localisation is re-created in WCCM, a new instance of the
            // localisation can be added back into the destinationHandler.
            dereferenceLocalisation(_pToPLocalMsgsItemStream);

            //Get the background clean-up thread to reallocate the messages and
            //clean up the localisation if possible.
            _destinationManager.markDestinationAsCleanUpPending(_baseDestinationHandler);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "localQueuePointRemoved");

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getQueuePoint(com.ibm.ws.sib.utils.SIBUuid8)
     */
    @Override
    public LocalizationPoint getQueuePoint(SIBUuid8 meUuid)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getQueuePoint", new Object[] { meUuid, this });

        PtoPMessageItemStream stream = null;

        if (meUuid.equals(_messageProcessor.getMessagingEngineUuid()))
        {
            stream = _pToPLocalMsgsItemStream;
        }
        else
        {
            stream = _localisationManager.getXmitQueuePoint(meUuid);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getQueuePoint", stream);

        return stream;
    }

    /**
     * Returns the guess set for queue points.
     * Unit tests only
     * 
     * @return
     */
    @Override
    public HashSet getQueuePointGuessSet()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getQueuePointGuessSet", this);

        HashSet theQueuePoints = _localisationManager.getQueuePointGuessSet();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getQueuePointGuessSet", theQueuePoints);

        return theQueuePoints;
    }

    /**
     * Method updateLocalisationDefinition.
     * <p>This method updates the destinationLocalizationDefinition associated with the
     * destinationHandler (if the destination is localised on this ME)
     * and performs any necessary modifications to the
     * message store and other components to reflect the new state of the
     * destinationHandler.</p>
     * 
     * @param destinationLocalizationDefinition
     *            <p>Updates the DestinationLocalizationDefinition associated with the
     *            destination.</p>
     */
    @Override
    public void updateLocalisationDefinition(BaseLocalizationDefinition destinationLocalizationDefinition,
                                             TransactionCommon transaction)
                    throws
                    SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "updateLocalisationDefinition", new Object[] { destinationLocalizationDefinition, transaction, this });

        if (_pToPLocalMsgsItemStream == null)
        {
            addNewLocalPtoPLocalization(
                                        transaction, _messageProcessor.getMessagingEngineUuid(), (LocalizationDefinition) destinationLocalizationDefinition, true);
        }
        else
        {
            //The destination localisation definition is stored off the itemstream that
            //holds the local destination localisations messages
            _pToPLocalMsgsItemStream.updateLocalizationDefinition(
                            (LocalizationDefinition) destinationLocalizationDefinition);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "updateLocalisationDefinition");
    }

    /**
     * Method addNewPtoPLocalisation
     * 
     * <p> Create a new PtoPMessageItemStream and add it to this Destination's Localisations.
     * <p>
     * In addition to creating and adding it, this function also performs all the
     * necessary updates to make it a recognized part of the Destination.
     * 
     * @param localisationIsRemote should be true if the localisation is remote.
     * @param transaction The Transaction to add under. Cannot be null.
     * @param messagingEngineUuid The uuid of the messaging engine that owns the localisation
     * @return destinationLocalizationDefinition.
     * 
     * @throws SIResourceException if the add fails due to a Message Store problem.
     */
    @Override
    public LocalizationPoint addNewPtoPLocalization(
                                                    boolean localisationIsRemote,
                                                    TransactionCommon transaction,
                                                    SIBUuid8 messagingEngineUuid,
                                                    BaseLocalizationDefinition destinationLocalizationDefinition,
                                                    boolean queuePoint) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "addNewPtoPLocalization",
                        new Object[] {
                                      Boolean.valueOf(localisationIsRemote),
                                      transaction,
                                      messagingEngineUuid,
                                      destinationLocalizationDefinition,
                                      Boolean.valueOf(queuePoint),
                                      this });

        LocalizationPoint newMsgItemStream = null;

        if (!localisationIsRemote)
        {
            newMsgItemStream =
                            addNewLocalPtoPLocalization(transaction,
                                                        messagingEngineUuid,
                                                        (LocalizationDefinition) destinationLocalizationDefinition,
                                                        queuePoint);
        }
        else
        {
            newMsgItemStream =
                            _localisationManager.
                                            addNewRemotePtoPLocalization(transaction,
                                                                         messagingEngineUuid,
                                                                         (LocalizationDefinition) destinationLocalizationDefinition,
                                                                         queuePoint,
                                                                         _remoteSupport);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "addNewPtoPLocalization", newMsgItemStream);

        return newMsgItemStream;
    }

    /**
     * Method addNewLocalPtoPLocalisation
     * <p> Create a new local PtoPMessageItemStream and add it to this Destination's
     * Localisations.
     * 
     * @param transaction The Transaction to add under. Cannot be null.
     * @param messagingEngineUuid The uuid of the messaging engine that owns the localisation
     * @return destinationLocalizationDefinition
     * 
     * @throws SIResourceException if the add fails due to a Message Store problem.
     */
    public LocalizationPoint addNewLocalPtoPLocalization(
                                                         TransactionCommon transaction,
                                                         SIBUuid8 messagingEngineUuid,
                                                         LocalizationDefinition destinationLocalizationDefinition,
                                                         boolean queuePoint) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "addNewLocalPtoPLocalization",
                        new Object[] {
                                      transaction,
                                      destinationLocalizationDefinition,
                                      Boolean.valueOf(queuePoint),
                                      this });

        PtoPMessageItemStream newMsgItemStream = null;

        // Add to the MessageStore
        try
        {
            newMsgItemStream =
                            new PtoPLocalMsgsItemStream(
                                            _baseDestinationHandler,
                                            messagingEngineUuid);

            _localisationManager.
                            addNewLocalPtoPLocalisation(transaction,
                                                        newMsgItemStream);

            Transaction msTran = _messageProcessor.resolveAndEnlistMsgStoreTransaction(transaction);
            _baseDestinationHandler.addItemStream(newMsgItemStream, msTran);

            // Set the default limits
            newMsgItemStream.setDefaultDestLimits();
            // Setup any message depth interval checking (510343)
            newMsgItemStream.setDestMsgInterval();

            //Update the localisation definition of the itemstream now that it has
            //been added into the message store
            ((PtoPLocalMsgsItemStream) newMsgItemStream).updateLocalizationDefinition(destinationLocalizationDefinition);

            attachLocalPtoPLocalisation(newMsgItemStream);
        } catch (OutOfCacheSpace e)
        {
            // No FFDC code needed

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "addNewLocalPtoPLocalization", "SIResourceException");

            throw new SIResourceException(e);
        } catch (MessageStoreException e)
        {
            // MessageStoreException shouldn't occur so FFDC.
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.destination.JSPtoPRealization.addNewLocalPtoPLocalization",
                                        "1:1137:1.24.1.25",
                                        this);

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "addNewLocalPtoPLocalization", e);

            throw new SIResourceException(e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "addNewLocalPtoPLocalization", newMsgItemStream);

        return newMsgItemStream;
    }

    /**
     * Method cleanupLocalisations.
     * <p>Cleanup any localisations of the destination that require it</p>
     */
    @Override
    public boolean cleanupLocalisations() throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "cleanupLocalisations", this);

        boolean allCleanedUp = ((RemotePtoPSupport) _remoteSupport).cleanupLocalisations();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "cleanupLocalisations", Boolean.valueOf(allCleanedUp));

        return allCleanedUp;
    }

    /**
     * Method clearLocalisingUuidsSet.
     * Clear the set of ME's that localise the destination
     */
    @Override
    public void clearLocalisingUuidsSet()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "clearLocalisingUuidsSet", this);

        _localisationManager.clearLocalisingUuidsSet();

        _pToPLocalMsgsItemStream = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "clearLocalisingUuidsSet");
        return;
    }

    /**
     * Method addAllLocalisationsForCleanUp.
     * <p>Add all the live localisations to the set requiring clean-up</p>
     */
    @Override
    public void addAllLocalisationsForCleanUp(boolean singleServer)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc,
                        "addAllLocalisationsForCleanUp",
                        new Object[] {
                                      Boolean.valueOf(singleServer),
                                      this });

        if (_pToPLocalMsgsItemStream != null && _pToPLocalMsgsItemStream.isInStore())
        {
            _postMediatedItemStreamsRequiringCleanup.put(_pToPLocalMsgsItemStream.getLocalizingMEUuid(), _pToPLocalMsgsItemStream);
        }

        super.addAllLocalisationsForCleanUp(singleServer);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "addAllLocalisationsForCleanUp");
    }

    @Override
    public LocalizationPoint getPtoPLocalLocalizationPoint()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getPtoPLocalLocalizationPoint", this);
            SibTr.exit(tc, "getPtoPLocalLocalizationPoint", _pToPLocalMsgsItemStream);
        }

        return _pToPLocalMsgsItemStream;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#registerControlAdapters()
     */
    @Override
    public void registerControlAdapters()
    {
        super.registerControlAdapters();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "registerControlAdapters", this);

        // register the publication point
        if (_pToPLocalMsgsItemStream != null)
        {
            _pToPLocalMsgsItemStream.registerControlAdapterAsMBean();
        }

        super.registerControlAdapters();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "registerControlAdapters");
    }

    /**
     * Method chooseConsumerDispatcher
     * 
     * <p> Retrieves a ConsumerDispatcher associated with this PtoP Destination.
     * 
     * @param definition
     * @param isReceiveAllowed
     * @param isToBeDeleted
     * @return consumerDispatcher
     * @throws SIResourceException
     */
    @Override
    public ConsumerManager chooseConsumerManager(
                                                 DestinationDefinition definition,
                                                 boolean isReceiveAllowed,
                                                 SIBUuid12 gatheringTargetUuid,
                                                 SIBUuid8 fixedMEUuid,
                                                 HashSet<SIBUuid8> scopedMEs)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc,
                        "chooseConsumerManager",
                        new Object[] {
                                      definition,
                                      Boolean.valueOf(isReceiveAllowed),
                                      gatheringTargetUuid,
                                      fixedMEUuid,
                                      scopedMEs });

        ConsumerManager consumerManager = null;
        boolean localCDAllowed = false;
        boolean gatherMessages = gatheringTargetUuid != null;

        // If we're fixed to a single ME we can't be a 'gathering' consumer
        if (fixedMEUuid != null)
        {
            gatherMessages = false;

            // Are we allowed to look for a local ConsumerDispatcher?
            if (fixedMEUuid.equals(_messageProcessor.getMessagingEngineUuid()))
                localCDAllowed = true;
        }
        else if (scopedMEs != null)
        {
            if (scopedMEs.contains(_messageProcessor.getMessagingEngineUuid()))
                localCDAllowed = true;
        }
        else
            localCDAllowed = true;

        // First look for a local ConsumerDispatcher (if we're allowed to)
        if ((gatherMessages || isReceiveAllowed) && localCDAllowed)
        {
            consumerManager =
                            getLocalPtoPConsumerManager();
        }

        //No local consumer dispatcher is available,
        if (consumerManager == null)
        {
            SIBUuid8 dmeUuid = null;

            // Look in WLM to see if a remote queue point can be found (unless we're fixed
            // to only the local one
            if ((fixedMEUuid == null) || !localCDAllowed)
                dmeUuid = _localisationManager.chooseRemoteQueuePoint(fixedMEUuid, scopedMEs);

            // If we found a remote one then we'll use it
            if (dmeUuid != null)
            {
                // If we have no local queue point but gather is set to true then we are a remote gatherer so
                // set the appropriate gathering uuid in the streams

                AnycastInputHandler aih = _remoteSupport.
                                getAnycastInputHandler(dmeUuid,
                                                       gatheringTargetUuid,
                                                       definition,
                                                       true); //create if necessary
                consumerManager = aih.getRCD();
            }
            // Otherwise, return the local one (if allowed)
            else if (localCDAllowed)
            {
                //Return the local (possibly get inhibited consumer dispatcher if
                //there is no other choice
                consumerManager =
                                getLocalPtoPConsumerManager();

                if (consumerManager == null)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "chooseConsumerManager", null);

                    //TODO This exception if fixed????
                    throw new SIResourceException(
                                    nls_cwsik.getFormattedMessage(
                                                                  "DELIVERY_ERROR_SIRC_26", // NO_LOCALISATIONS_FOUND_ERROR_CWSIP0032
                                                                  new Object[] { _baseDestinationHandler.getName() },
                                                                  null));
                }
            }
        }
        else if (gatherMessages) // We are a local message gathering consumer
        {
            if (gatheringConsumerDispatcher == null)
                gatheringConsumerDispatcher = new HashMap<SIBUuid12, GatheringConsumerDispatcher>();

            GatheringConsumerDispatcher gcd = gatheringConsumerDispatcher.get(gatheringTargetUuid);
            if (gcd == null)
            {
                gcd = new GatheringConsumerDispatcher(_baseDestinationHandler, getLocalPtoPConsumerManager());

                // Synchronize to ensure that we don't miss any change notifications between obtaining the current
                // set of localising messaging engines and creating all the AnycastInputHandler instances
                synchronized (gcd)
                {
                    // Create a GatheringConsumerDispatcher instance initially with no remote consumer dispatchers
                    gatheringConsumerDispatcher.put(gatheringTargetUuid, gcd);

                    // Register to receive notification of changes in destination localisation sets
                    gcd.registerChangeListener(scopedMEs, _baseDestinationHandler.getUuid(), definition, _remoteSupport);

                    // Get all currently "up" Me
                    Set<SIBUuid8> localisations = _localisationManager.getAllGetLocalisations();

                    Iterator<SIBUuid8> it = localisations.iterator();
                    while (it.hasNext())
                    {
                        final SIBUuid8 meUuid = it.next();
                        if (!meUuid.equals(_messageProcessor.getMessagingEngineUuid()) &&
                            (scopedMEs == null || (scopedMEs != null && scopedMEs.contains(meUuid))))
                        { // Our own uuid is not remote!
                            final AnycastInputHandler aih = _remoteSupport.getAnycastInputHandler(meUuid, null, definition, true);
                            gcd.addRemoteCD(meUuid, aih.getRCD());
                        }
                    }
                }
            }
            consumerManager = gcd;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "chooseConsumerManager", consumerManager);

        return consumerManager;
    }

    /**
     * Method attachPtoPLocalisation
     * <p> Attach a Localisation to this Destination's Localisations.
     * This entails:
     * <p>
     * 1. Initializing the Localisation with this BaseDestinationHandler's details.
     * 2. Adding the Localisation to this BaseDestinationHandler's list.
     * 3. Creating new input/output handlers as appropriate.
     * <p>
     * Feature 174199.2.7
     * 
     * @param ptoPMessageItemStream is the PtoPMessageItemStream to add.
     * @param localisationIsRemote should be true if the PtoPMessageItemStream is remote.
     * @param transaction is the Transaction to add it under.
     */
    @Override
    public void attachPtoPLocalisation(
                                       LocalizationPoint ptoPMessageItemStream,
                                       boolean localisationIsRemote) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "attachPtoPLocalisation",
                        new Object[] {
                                      ptoPMessageItemStream,
                                      Boolean.valueOf(localisationIsRemote),
                                      this });

        if (!localisationIsRemote)
        {
            attachLocalPtoPLocalisation(ptoPMessageItemStream);
        }
        else
        {
            _localisationManager.attachRemotePtoPLocalisation(ptoPMessageItemStream,
                                                              _remoteSupport);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "attachPtoPLocalisation");
    }

    /**
     * Method attachLocalPtoPLocalisation
     * 
     * <p> Attach a local Localisation to this Destination's Localisations.
     * 
     * @param ptoPMessageItemStream is the PtoPMessageItemStream to add.
     * @param localisationIsRemote should be true if the PtoPMessageItemStream is remote.
     * @param transaction is the Transaction to add it under.
     */
    @Override
    public void attachLocalPtoPLocalisation(LocalizationPoint ptoPMessageItemStream)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "attachLocalPtoPLocalisation",
                        new Object[] {
                                      ptoPMessageItemStream,
                                      this });

        /*
         * Always maintain a reference to the home localisation of a
         * destination, if one exists.
         */

        _pToPLocalMsgsItemStream = (PtoPLocalMsgsItemStream) ptoPMessageItemStream;
        _localisationManager.setLocal();

        ConsumerDispatcherState state = new ConsumerDispatcherState();

        /*
         * Now create the consumer dispatcher,
         * passing it the itemstream that has been created
         */
        final ConsumerDispatcher consumerDispatcher =
                        new ConsumerDispatcher(_baseDestinationHandler, (PtoPMessageItemStream) ptoPMessageItemStream, state);

        ptoPMessageItemStream.setOutputHandler(consumerDispatcher);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "attachLocalPtoPLocalisation");
    }

    /**
     * Method runtimeEventOccurred.
     * 
     * @param pevent
     */
    @Override
    public void runtimeEventOccurred(MPRuntimeEvent event)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "runtimeEventOccurred", new Object[] { event, this });

        // A local q point, fire against the control adapter belonging to the
        // ptoplocalmsgsitemstream
        if (_pToPLocalMsgsItemStream != null)
        {
            _pToPLocalMsgsItemStream.
                            getControlAdapter().
                            runtimeEventOccurred(event);
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "runtimeEventOccurred", "local queue point is null, cannot fire event");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "runtimeEventOccurred");
    }

    /**
     * Method flushQueuePointOutputHandler.
     * 
     */
    @Override
    public boolean flushQueuePointOutputHandler()
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "flushQueuePointOutputHandler", this);

        boolean done = _localisationManager.flushQueuePointOutputHandler();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "flushQueuePointOutputHandler", Boolean.valueOf(done));
        return done;
    }

    /**
     * Method registerDestination.
     * <p>Register the destination vith WLM via TRM</p>
     * 
     * System destinations and Temporary destinations are not registered
     * with WLM. The destinations themselves have their own addressing
     * mechanisms.
     */
    @Override
    public void registerDestination(boolean hasLocal,
                                    boolean isDeleted)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc,
                        "registerDestination",
                        new Object[] {
                                      Boolean.valueOf(hasLocal),
                                      Boolean.valueOf(isDeleted),
                                      this });
        // Dont register temporary destinations or system destinations.
        if (!_baseDestinationHandler.isTemporary() && !_baseDestinationHandler.isSystem())
        {
            if (_pToPLocalMsgsItemStream != null && !(_pToPLocalMsgsItemStream instanceof MQLinkMessageItemStream))
                _localisationManager.registerDestination();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "registerDestination");
    }

    @Override
    public void onExpiryReport()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "onExpiryReport", this);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "onExpiryReport");
    }

    /**
     * @return
     */
    @Override
    public boolean isLocalMsgsItemStream()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "isLocalMsgsItemStream", this);
            SibTr.exit(tc, "isLocalMsgsItemStream", Boolean.valueOf((_pToPLocalMsgsItemStream != null)));
        }
        return (_pToPLocalMsgsItemStream != null);
    }

    /**
     * Method checkAbleToSend
     * 
     * <p> Return the sendallowed value associated with the itemstream.
     * 
     * @param checkSendAllowed if true the sendAllowed value is checked.
     * @return
     */
    @Override
    public int checkAbleToSend()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkAbleToSend", this);

        int result = DestinationHandler.OUTPUT_HANDLER_NOT_FOUND;

        // If both destination and queuepoint are sendAllowed then we can put the message
        // If we are a linkHandler we have no queue point so only check the destination def
        // on the other hand if this is an MQLink then we will check sendAllowed on
        // the local itemstream.
        boolean isSendAllowedOnLocalization = true;
        if (_baseDestinationHandler.isMQLink() || !_baseDestinationHandler.isLink())
            isSendAllowedOnLocalization = _pToPLocalMsgsItemStream.isSendAllowed();

        // Check if the queue point is send allowed (which can be set at the individual
        // message point level)
        if (!isSendAllowedOnLocalization)
            result = DestinationHandler.OUTPUT_HANDLER_SEND_ALLOWED_FALSE;
        else if (_pToPLocalMsgsItemStream != null)
        {
            if (!_pToPLocalMsgsItemStream.isQFull())
                result = DestinationHandler.OUTPUT_HANDLER_FOUND;
            else
                result = DestinationHandler.OUTPUT_HANDLER_ALL_HIGH_LIMIT;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkAbleToSend", Integer.valueOf(result));
        return result;
    }

    /**
     * Method updateRemoteQueuePointSet
     * 
     * @param newQueuePointLocalisingMEUuids
     * @throws SIResourceException
     */
    @Override
    public void updateRemoteQueuePointSet(Set newQueuePointLocalisingMEUuids)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "updateRemoteQueuePointSet",
                        new Object[] {
                                      newQueuePointLocalisingMEUuids,
                                      this });

        _localisationManager.updateRemoteQueuePointSet(newQueuePointLocalisingMEUuids,
                                                       _postMediatedItemStreamsRequiringCleanup);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "updateRemoteQueuePointSet");

    }

    /**
     * Mark the DestinationHandler for deletion.
     */
    @Override
    public void setToBeDeleted()
    {
        super.setToBeDeleted();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setToBeDeleted", this);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setToBeDeleted");
    }

    /**
     * Get the High Message threshold
     */
    //117505
    public long getQhighMsgLimit()
    {
        long qHighlimit = 0;
        try {
            qHighlimit = _pToPLocalMsgsItemStream.getDestHighMsgs();
        } catch (Exception e) {
            //NO FFDC Code Needed
        }
        return qHighlimit;

    }
}
