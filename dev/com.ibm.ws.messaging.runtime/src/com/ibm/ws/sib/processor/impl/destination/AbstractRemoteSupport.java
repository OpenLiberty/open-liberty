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
 
package com.ibm.ws.sib.processor.impl.destination;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.InvalidAddOperation;
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
import com.ibm.ws.sib.processor.impl.DestinationManager;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.PtoPOutputHandler;
import com.ibm.ws.sib.processor.impl.RemoteConsumerDispatcher;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.ProducerInputHandler;
import com.ibm.ws.sib.processor.impl.store.filters.ClassEqualsFilter;
import com.ibm.ws.sib.processor.impl.store.itemstreams.AIContainerItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.AOContainerItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPReceiveMsgsItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.SourceProtocolItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.TargetProtocolItemStream;
import com.ibm.ws.sib.processor.runtime.impl.AnycastInputControl;
import com.ibm.ws.sib.processor.runtime.impl.ControlAdapter;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;


/**
 * @author Neil Young
 * 
 * <p>The AbstractRemoteSupport class is the parent class of RemotePtoPSupport
 * and RemotePubSubSupport. It holds abstract methods and code that is common to
 * each class.
 */
public abstract class AbstractRemoteSupport
{
  /** 
   * Trace for the component
   */
  private static final TraceComponent tc =
    SibTr.register(
        AbstractRemoteSupport.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
      
   
  /**
   * NLS for component
   */
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);       
  
  /** Reference to the associated BaseDestinationHandler */
  protected BaseDestinationHandler _baseDestinationHandler;
  protected MessageProcessor _messageProcessor;
  protected DestinationManager _destinationManager;
      
  /**
   * Protocol persistent storage stream.
   * <p>
   * Feature 171905.42
   */
  protected TargetProtocolItemStream _targetProtocolItemStream;
  
  /**
   * source protocol itemstream used to store StreamSet protocol items for
   * ptop and pubsub now. 
   */
  protected SourceProtocolItemStream _sourceProtocolItemStream;
  
  /**
   * It the destination is localised on this ME, anycastOutputHandler provides
   * a reference to the component that handler remote get requests. The aoContainerItemStream
   * is used by the anycastOutputHandler.
   */
  protected AnycastOutputHandler _anycastOutputHandler = null;
  protected AOContainerItemStream _aoContainerItemStream = null;

  /**
   * If this ME is not a localisation point, anycastInputHandlers provides a map of
   * references to the components that handle remote get interactions, one per localisation.
   * We assume that an ME that is a localisation point does not provide a remote consumer
   * dispatcher, since a local consumer dispatcher is available - apart from when performing
   * gathering.
   * This map is also used to obtain remote consumer dispatchers, by first obtaining the
   * corresponding anycast input handler and issuing getRCD on it.
   */
  protected HashMap<String, AnycastInputHandler> _anycastInputHandlers = new HashMap<String, AnycastInputHandler>();
  /** Keep track of individual aiContainerItemStreams to clean them up later */
  protected HashMap<String, AIContainerItemStream> _aiContainerItemStreams = new HashMap<String, AIContainerItemStream>();
  /** Keep track of individual rcdItemStreams to clean them up later */
  protected HashMap<String, PtoPReceiveMsgsItemStream> _rcdItemStreams = new HashMap<String, PtoPReceiveMsgsItemStream>();

  /** Field to indicate if the destination has been deleted */
  private boolean _toBeDeleted;

  /**
   * Constructor.
   * @param myBaseDestinationHandler
   * @param messageProcessor
   */
  public AbstractRemoteSupport(
    BaseDestinationHandler myBaseDestinationHandler,
    MessageProcessor messageProcessor)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "AbstractRemoteSupport",
        new Object[] {
          myBaseDestinationHandler, messageProcessor });

    _baseDestinationHandler = myBaseDestinationHandler;
    _messageProcessor = messageProcessor;
    _destinationManager = messageProcessor.getDestinationManager();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "AbstractRemoteSupport", this);
  }
   
  /**
   * Method reconstituteAnycastRMEPhaseOne
   * <p>Reconstitute any RMEs for anycast. A protocol stream for the AIH is recovered.
   * Any message item stream for the RCD is removed.
   * 
   * Once the existing streams are found, reconconstitution is deferred until we've
   * reconstituted any AnycastOutputHandlers, which will discover any links between
   * the two if message gathering has been in use (524796)
   */
  public void reconstituteAnycastRMEPhaseOne() 
    throws MessageStoreException, SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "reconstituteAnycastRMEPhaseOne");
    
    NonLockingCursor cursor =
    _baseDestinationHandler.newNonLockingItemStreamCursor(
        new ClassEqualsFilter(AIContainerItemStream.class));
      
    AIContainerItemStream aiTempItemStream = null;
    do
    {
      aiTempItemStream = (AIContainerItemStream) cursor.next();
      if (aiTempItemStream != null)
      {
        // NOTE: since this destination is PtoP it should NOT be
        // possible to end up recovering an aistream used for durable.
        // Still, bugs happen, so here's a sanity check
        if (aiTempItemStream.getDurablePseudoDestID() != null)
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
            SibTr.exit(tc, "reconstituteAnycastRME", "SIResourceException");
          throw new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0003",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.destination.AbstractRemoteSupport.reconstituteAnycastRMEPhaseOne",
              "1:230:1.25",
              null },
            null));
        }
        
        String key = SIMPUtils.getRemoteGetKey(aiTempItemStream.getDmeId(), aiTempItemStream.getGatheringTargetDestUuid());
        _aiContainerItemStreams.put(key, aiTempItemStream);
      }
    }
    while (aiTempItemStream != null);
    
    cursor.finished();
    
    cursor =
    _baseDestinationHandler.newNonLockingItemStreamCursor(
        new ClassEqualsFilter(PtoPReceiveMsgsItemStream.class));
    
    PtoPReceiveMsgsItemStream rcdTempItemStream = (PtoPReceiveMsgsItemStream)cursor.next();
    if (rcdTempItemStream != null && !rcdTempItemStream.isRestoring())
    {
      try
      {
        LocalTransaction tran =
        _baseDestinationHandler.getTransactionManager().createLocalTransaction(true);
        
        do
        {
          rcdTempItemStream.reconstitute(_baseDestinationHandler);
          // not really reallocate, just remove all items
          rcdTempItemStream.reallocateMsgs();
          rcdTempItemStream.remove((Transaction) tran, AbstractItem.NO_LOCK_ID);
          
          rcdTempItemStream = (PtoPReceiveMsgsItemStream)cursor.next();
        }
        while (rcdTempItemStream != null);

        tran.commit();
      }
      catch(Exception e)
      {
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.destination.AbstractRemoteSupport.reconstituteAnycastRMEPhaseOne",
          "1:273:1.25",
          this);
                      
        SibTr.exception(tc, e);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(
            tc,
            "reconstituteAnycastRMEPhaseOne",
            "SIResourceException");
        throw new SIResourceException(e);
      }
    }
    
    cursor.finished();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "reconstituteAnycastRMEPhaseOne");
  }

  
  /**
   * Method reconstituteAnycastRMEPhaseTwo
   * <p>Now finish the reconstitution of the the AIHs. We didn't used to do and leave
   * it to future consumers/protocol messages to kick it off but that would leave
   * messages locked for too long and complicates things when we want to clean up
   * a destination. So we do it all now instead (524796)
   */
  public void reconstituteAnycastRMEPhaseTwo(int startMode,
                                     DestinationDefinition definition) 
    throws MessageStoreException, SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, 
                  "reconstituteAnycastRMEPhaseTwo",
                  new Object[] { 
                  Integer.valueOf(startMode),
                  definition});
    
    // We need to reconstiture the AnycastInputHandlers/RCDs so that all parts of the destination
    // are ready for various reasons:
    //   - The inactivity timer is initiated to allow automatic cleanup if a new consumer doesn't attach
    //   - Incoming control messages for existing anycast streams are correctly processed
    //   - The asynchDeletionThread sees all of the destination, so it can correctly clean it up if it
    //     is deleted
    
    boolean flush = false;
    // We flush the streams if in START_FLUSH mode, but not if we're in recovery mode, because we won't
    // be able to send any messages to the DME
    if(   ((startMode & JsConstants.ME_START_FLUSH ) == JsConstants.ME_START_FLUSH )
       && ((startMode & JsConstants.ME_START_RECOVERY ) == 0 ) )
      flush = true;
    
    synchronized (_anycastInputHandlers)
    {
      for(Iterator i = _aiContainerItemStreams.values().iterator(); i.hasNext();)
      {
        // If the AIH hasn't been reconstituted yet (can be kicked off from inside
        // the local reconstitution when it chooses a ConsumerManager) we do it now.
        AIContainerItemStream stream = (AIContainerItemStream) i.next();
        String key = SIMPUtils.getRemoteGetKey(stream.getDmeId(), stream.getGatheringTargetDestUuid());
        AnycastInputHandler aih = _anycastInputHandlers.get(key);
       
        if(aih == null)
        {
          aih = createAIHandRCD(stream.getDmeId(),
              stream.getGatheringTargetDestUuid(),
              definition, 
              flush);
          _anycastInputHandlers.put(key, aih);
        }
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "reconstituteAnycastRMEPhaseTwo");
  }

  /**
   * Method reconstituteGD.
   * <p>176658.3.5 Reconstitute Guranteed Delivery ItemStreams 
   * @throws MessageStoreException
   */
  public void reconstituteGD() throws MessageStoreException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "reconstituteGD");

    // There should only be one TargetProtocolItemStream in the BaseDestinationHandler.
    NonLockingCursor cursor =
      _baseDestinationHandler.newNonLockingItemStreamCursor(
        new ClassEqualsFilter(TargetProtocolItemStream.class));
    _targetProtocolItemStream = (TargetProtocolItemStream) cursor.next();
    
    cursor.finished();

    // If missing and this is a pt-to-pt destination, then bad destination data from MsgStore
    if (_targetProtocolItemStream == null)
    {
      SIErrorException e =
        new SIErrorException(
          nls.getFormattedMessage(
            "DESTINATION_HANDLER_RECOVERY_ERROR_CWSIP0048",
            new Object[] { _baseDestinationHandler.getName()},
            null));

      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.destination.AbstractRemoteSupport.reconstituteGD",
        "1:381:1.25",
        this);

      SibTr.exception(tc, e);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "reconstituteGD", e);
      throw e;
    }
    
    cursor =
    _baseDestinationHandler.newNonLockingItemStreamCursor(
        new ClassEqualsFilter(SourceProtocolItemStream.class));
    _sourceProtocolItemStream = (SourceProtocolItemStream) cursor.next();

    cursor.finished();

    // If missing, then bad destination data from MsgStore
    if (_sourceProtocolItemStream == null)
    {
      SIErrorException e =
        new SIErrorException(
          nls.getFormattedMessage(
            "DESTINATION_HANDLER_RECOVERY_ERROR_CWSIP0048",
            new Object[] { _baseDestinationHandler.getName()},
            null));
  
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.destination.AbstractRemoteSupport.reconstituteGD",
        "1:442:1.25",
        this);
  
      SibTr.exception(tc, e);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "reconstituteGD", e);
      throw e;
    }
      
    _sourceProtocolItemStream.reconstitute(_baseDestinationHandler, _baseDestinationHandler.getTransactionManager());

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "reconstituteGD");
  }

  /**
   * Method reconstituteGDTargetStreams
   * @param mediationInputHandler
   * @throws MessageStoreException
   * @throws SIException
   */
  public void reconstituteGDTargetStreams() 
    throws MessageStoreException, SIException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "reconstituteGDTargetStreams");

    _targetProtocolItemStream.reconstitute(_baseDestinationHandler, 
                                           _baseDestinationHandler.getTransactionManager(), 
                                           (ProducerInputHandler)_baseDestinationHandler.getInputHandler());
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "reconstituteGDTargetStreams");
  }

  /**
   * Method reconstituteSourceStreams
   * @param startMode
   * @param outputHandler
   * @throws SIDiscriminatorSyntaxException
   * @throws MessageStoreException
   * @throws SIResourceException
   */
  public void reconstituteSourceStreams(int startMode,
                                        PtoPOutputHandler outputHandler) 
    throws SIDiscriminatorSyntaxException, MessageStoreException, SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, 
                  "reconstituteSourceStreams",
                  new Object[] { 
                  Integer.valueOf(startMode)});  

    _sourceProtocolItemStream.reconstituteSourceStreams( _baseDestinationHandler,
                                                         outputHandler, 
                                                         startMode);
  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "reconstituteSourceStreams");
  }


  /**
   * Method reconstituteLocalQueuePoint
   * <p>Reconstitute the remote components for a local queue point.
   * 
   * @return Success of failure of reconstitution of the local queue point
   */
  public abstract int reconstituteLocalQueuePoint(int startMode) 
    throws MessageStoreException, SIResourceException;

  /**
   * Method createGDProtocolItemStreams
   * @param transaction
   * @throws OutOfCacheSpace
   * @throws MessageStoreException
   * @throws SIResourceException
   */
  public void createGDProtocolItemStreams(TransactionCommon transaction)
    throws OutOfCacheSpace,MessageStoreException, SIResourceException                                                         
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createGDProtocolItemStreams");
    
    Transaction msTran = _messageProcessor.resolveAndEnlistMsgStoreTransaction(transaction);
    _targetProtocolItemStream = new TargetProtocolItemStream(_baseDestinationHandler, msTran);
    _sourceProtocolItemStream = new SourceProtocolItemStream(_baseDestinationHandler, msTran);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createGDProtocolItemStreams");
  }    
   
  /**
   * Method removeProtocolItems.
   * <p>This removes protocol items from ProtocolItemStream.
   * @param transaction
   * @throws SIResourceException
   */
  public void removeProtocolItems(TransactionCommon transaction)
    throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "removeProtocolItems", transaction );

    Transaction msTran = _messageProcessor.resolveAndEnlistMsgStoreTransaction(transaction);
    
    try
    {
      //Clean up destination stream state
      if (_targetProtocolItemStream != null)
      {
        _targetProtocolItemStream.removeAll(msTran);
      }
     
      if (_sourceProtocolItemStream != null)
      { 
        _sourceProtocolItemStream.removeAll(msTran);
      }
      
    }
    catch (MessageStoreException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.destination.AbstractRemoteSupport.removeProtocolItems",
        "1:636:1.25",
        this);

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "removeProtocolItems", e);

      throw new SIResourceException(e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeProtocolItems");
  }  

  /**
   * Method resetProtocolStreams.
   * <p>Cleanup any localisations of the destination that require it</p>
   */
  public void resetProtocolStreams()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "resetProtocolStreams");

    _targetProtocolItemStream = null;
    _sourceProtocolItemStream = null;
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "resetProtocolStreams");      
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getTargetProtocolItemStream()
   */
  public TargetProtocolItemStream getTargetProtocolItemStream()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
    {
      SibTr.entry(tc, "getTargetProtocolItemStream");
      SibTr.exit(tc, "getTargetProtocolItemStream", _targetProtocolItemStream);	
    }
    return _targetProtocolItemStream;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getSourceProtocolItemStream()
   */
  public SourceProtocolItemStream getSourceProtocolItemStream()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getSourceProtocolItemStream");
      SibTr.exit(tc, "getSourceProtocolItemStream", _sourceProtocolItemStream);
    }
    return _sourceProtocolItemStream;
  }  

  /**
   * Method getAnycastInputHandlerByPseudoDestId
   * <p> Overriden in pubsub implementation.
   * @param destID
   * @return
   */
  public abstract AnycastInputHandler getAnycastInputHandlerByPseudoDestId(SIBUuid12 destID);

  /**
   * Method getAnycastOutputHandlerByPseudoDestId
   * <p> Overriden in pubsub implementation.
   * @param destID
   * @return
   */
  public abstract AnycastOutputHandler getAnycastOutputHandlerByPseudoDestId(SIBUuid12 destID);

  /**
   * Method getPostReconstitutePseudoIds
   * <p> Overriden in pubsub implemenatation.
   * @return
   */
  public abstract Object[] getPostReconstitutePseudoIds(); 

  /**
   * Method getAnycastInputHandler
   * @param dmeId
   * @param definition
   * @param isToBeDeleted
   * @return
   */
  public AnycastInputHandler getAnycastInputHandler(SIBUuid8 dmeId,
                                                    SIBUuid12 gatheringTargetDestUuid,
                                                    DestinationDefinition definition,
                                                    boolean createAIH)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, 
                  "getAnycastInputHandler",
                  new Object[]{ 
                  dmeId,
                  gatheringTargetDestUuid,
                  definition,
                  Boolean.valueOf(createAIH)});

    AnycastInputHandler aih = null;
    String key = SIMPUtils.getRemoteGetKey(dmeId, gatheringTargetDestUuid);

    synchronized (_anycastInputHandlers)
    {
      aih = _anycastInputHandlers.get(key);
      if (aih == null && createAIH && !_toBeDeleted)
      {
        aih = createAIHandRCD(dmeId, gatheringTargetDestUuid, definition, false);
        _anycastInputHandlers.put(key, aih);
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getAnycastInputHandler", aih);

    return aih;
  }

  /**
   * Method getAnycastOutputHandler
   * @param consumerDispatcher
   * @param definition
   * @param restartFromStaleBackup
   * @return
   */
  public final AnycastOutputHandler getAnycastOutputHandler(DestinationDefinition definition,
                                                            boolean restartFromStaleBackup)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, 
                  "getAnycastOutputHandler",
                  new Object[]{ 
                    definition,
                    Boolean.valueOf(restartFromStaleBackup)});
                  
    if (_anycastOutputHandler == null)
    { // lazy creation of AnycastOutputHandler
      try
      {
        // check if aoContainerItemStream already exists
        if (_aoContainerItemStream == null)
        {
          // need to create one first
          AOContainerItemStream aostream = new AOContainerItemStream(null, null);
          LocalTransaction tran = _baseDestinationHandler.getTransactionManager().createLocalTransaction(true);
          _baseDestinationHandler.addItemStream(aostream, (Transaction) tran);
          tran.commit();

          _aoContainerItemStream = aostream;
        }

        // TODO - should be using something better than System.currentTimeMillis() for the dmeVersion.
        _anycastOutputHandler =
          new AnycastOutputHandler(
            definition.getName(), definition.getUUID(), 
            definition.isReceiveExclusive(),
            _baseDestinationHandler,
            null,
            _aoContainerItemStream,
            _messageProcessor,
            _destinationManager.getAsyncUpdateThread(),
            _destinationManager.getPersistLockThread(),
            System.currentTimeMillis(),
            restartFromStaleBackup);
      }
      catch (Exception e)
      {
        // Exception shouldn't occur so FFDC.
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.destination.AbstractRemoteSupport.getAnycastOutputHandler",
          "1:834:1.25",
          this);

        SibTr.exception(tc, e);
        
        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0003",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.destination.AbstractRemoteSupport",
              "1:842:1.25",
              e,
              _baseDestinationHandler.getName() });

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "getAnycastOutputHandlerInternal", "SIErrorException");
                    
        throw new SIErrorException(nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0003",
            new Object[] {
             "com.ibm.ws.sib.processor.impl.destination.AbstractRemoteSupport.getAnycastOutputHandler",
             "1:853:1.25",
              e,
              _baseDestinationHandler.getName() },
            null),
            e);
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getAnycastOutputHandler", _anycastOutputHandler);

    return _anycastOutputHandler;
  }  

  /**
   * Method createAIHandRCD
   * @param dmeId
   * @param definition
   * @param isToBeDeleted
   * @param restartFromStaleBackup
   * @return
   */
  private AnycastInputHandler createAIHandRCD(SIBUuid8 dmeId, 
                                              SIBUuid12 gatheringTargetDestUuid,
                                              DestinationDefinition definition,
                                              boolean restartFromStaleBackup)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, 
                  "createAIHandRCD",
                  new Object[]{ 
                  dmeId,
                  gatheringTargetDestUuid,
                  definition,
                  Boolean.valueOf(restartFromStaleBackup)});
                  
    AnycastInputHandler aih = null;
    AIContainerItemStream aiContainerItemStream = null;
    PtoPReceiveMsgsItemStream rcdItemStream = null;
    String key = SIMPUtils.getRemoteGetKey(dmeId, gatheringTargetDestUuid);

    try
    {
      // check if either item stream already exists
      aiContainerItemStream = _aiContainerItemStreams.get(key);
      rcdItemStream = _rcdItemStreams.get(key);
      if (aiContainerItemStream == null || rcdItemStream == null)
      {
        LocalTransaction tran = _baseDestinationHandler.getTransactionManager().createLocalTransaction(true);

        // create aiContainerItemStream if necessary
        if (aiContainerItemStream == null)
        {
          aiContainerItemStream = new AIContainerItemStream(dmeId, gatheringTargetDestUuid, null, null, null);
          _baseDestinationHandler.addItemStream(aiContainerItemStream, (Transaction) tran);
        }

        // create rcdItemStream if necessary
        if (rcdItemStream == null)
        {
          rcdItemStream = new PtoPReceiveMsgsItemStream(_baseDestinationHandler, dmeId, null);
          _baseDestinationHandler.addItemStream(rcdItemStream, (Transaction) tran);
          
          // Configure destination depth parameters for the PtoPReceiveMsgsItemStream (defect 559096)
          // Set the default limits on the Itemstream
          rcdItemStream.setDefaultDestLimits();
          // Setup any message depth interval checking in line with 510343
          rcdItemStream.setDestMsgInterval();
        }

        tran.commit();
        
        // If commit succeeded, add streams to map
        _aiContainerItemStreams.put(key, aiContainerItemStream);
        _rcdItemStreams.put(key, rcdItemStream);
      }

      aih =
        new AnycastInputHandler(
          definition.getName(),
          definition.getUUID(),
          definition.isReceiveExclusive(),
          _messageProcessor,
          aiContainerItemStream,
          dmeId,
          gatheringTargetDestUuid,
          _destinationManager.getAsyncUpdateThread(),
          _baseDestinationHandler,
          restartFromStaleBackup);
      RemoteConsumerDispatcher rcd =
        new RemoteConsumerDispatcher(
          _baseDestinationHandler, definition.getName(),
          rcdItemStream,
          new ConsumerDispatcherState(),
          aih,
          _baseDestinationHandler.getTransactionManager(),
          definition.isReceiveExclusive());
      rcd.setReadyForUse();

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "RCD created " + rcd);

    }
    catch (Exception e)
    {
      // Exception shouldn't occur so FFDC and rethrow as runtime exception
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.destination.AbstractRemoteSupport.createAIHandRCD",
        "1:961:1.25",
        this);

      SIErrorException e2 =
        new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] { "com.ibm.ws.sib.processor.impl.destination.AbstractRemoteSupport.createAIHandRCD", "1:968:1.25", e },
            null),
        e);

      SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.destination.AbstractRemoteSupport.createAIHandRCD",
          "1:976:1.25",
          e });

      // Don't throw exception if destination is being deleted, it is a message store invalid add
      // operation, and we're not being reconstituted with startMode == JsConstants.ME_START_FLUSH
      // Defect 222966
      if (_toBeDeleted && e instanceof InvalidAddOperation && !restartFromStaleBackup)
      {
        // Not throwing exception, make sure we return null
        aih = null;
      }
      else
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "createAIHandRCD", aih);
        
        throw e2;
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createAIHandRCD", aih);

    return aih;
  }

  /**
   * Method removeAnycastInputHandlerAndRCD
   * <p>Removes the AIH and the RCD instances for a given dme ID. Also removes the itemStreams
   * from the messageStore for the aiContainerItemStream and the rcdItemStream
   * 
   * @param dmeID the uuid of the dme which the instances of the aih and rcd 
   *          will be deleted.
   * @throws SIResourceException if there was a problem with removing the itemStreams 
   *                              from the messageStore
   * @return boolean as to whether the itemstreams were removed or not
   */
  public boolean removeAnycastInputHandlerAndRCD(String key) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "removeAnycastInputHandlerAndRCD", key);
    
    AnycastInputHandler aih = _anycastInputHandlers.get(key);
    AIContainerItemStream aiContainerItemStream = _aiContainerItemStreams.get(key);
    PtoPMessageItemStream rcdItemStream = _rcdItemStreams.get(key);
    boolean removed = true;
    
    if (aih != null)
    {
      boolean removeAIH = true;
      
      try
      {
        LocalTransaction tran = null;
    
        // 216208 Need to remove the RCD's item stream in case there are unassigned
        // messages; the transaction needs to commit to allow the AIMessageItems to
        // reject on postCommitRemove called by reallocateMsgs
        if (rcdItemStream != null)
        {
          /**
          // not really reallocate, just remove all items
          // this should actually not find any items to remove
          if (rcdItemStream.reallocateMsgs())
          {
            siTran = txManager.createLocalTransaction();
   
            rcdItemStream.remove(siTran, NO_LOCK_ID);
                      
            siTran.commit();
            // only now is it safe to remove the table entry
            rcdItemStreams.remove(dmeId);
          }
          else
          {
            removeAIH = false;
            removed = false;
          }
          */
          
          tran = _baseDestinationHandler.getTransactionManager().createLocalTransaction(true);
   
          rcdItemStream.remove((Transaction) tran, AbstractItem.NO_LOCK_ID);
                      
          tran.commit();
          // only now is it safe to remove the table entry
          _rcdItemStreams.remove(key);
        }
    
        if (removeAIH)
        {
          /** This is not necessary any more, as our callers call either
           * aih.forceFlushAtTarget or aih.destinationDeleted first, which
           * will result in aih.delete called
          // At this point there should not be any messages and thus no V/U
          aih.delete();
          */
        
          if (aiContainerItemStream != null)
          {
            tran = _baseDestinationHandler.getTransactionManager().createLocalTransaction(true);
    
            // aih.delete removed the completed prefix and protocol item stream inside of aiContainerItemStream
            // so it's ok to remove the container item stream now
            aiContainerItemStream.remove((Transaction) tran, AbstractItem.NO_LOCK_ID);
        
            tran.commit();
            // only now is it safe to remove the table entry
            _aiContainerItemStreams.remove(key);
          }

          // only now is it safe to remove the table entry
          _anycastInputHandlers.remove(key);
        }
      }
      catch (Exception e)
      {
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.destination.AbstractRemoteSupport.removeAnycastInputHandlerAndRCD",
          "1:1096:1.25",
          this);
    
        SibTr.exception(tc, e);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "removeAnycastInputHandlerAndRCD", e);
        
        throw new SIResourceException(e);
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
       SibTr.exit(tc, "removeAnycastInputHandlerAndRCD", Boolean.valueOf(removed));
    
    return removed;
  }

  /** 
   * Method closeRemoteConsumers
   * <p>Close any consumers that are consuming remotely from a partition of the queue
   * that has now been deleted
   */
  public void closeRemoteConsumers(Set newQueuePointLocalisingMEUuids,
                                   LocalisationManager daManager)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, 
                  "closeRemoteConsumers", 
                  new Object[]{ 
                    newQueuePointLocalisingMEUuids,
                    daManager});

    Iterator existingMEs = _anycastInputHandlers.keySet().iterator();
    Iterator newMEs = newQueuePointLocalisingMEUuids.iterator();
    
    while (existingMEs.hasNext())
    {
      boolean found = false;
      String key = (String) existingMEs.next();
      AnycastInputHandler aih = _anycastInputHandlers.get(key);
      SIBUuid8 dmeUuid = aih.getLocalisationUuid();
      
      while (newMEs.hasNext())
      {
        SIBUuid8 newMEUuid = new SIBUuid8((String)newMEs.next());
        
        if (newMEUuid.equals(dmeUuid))
        {
          found = true;
          break;
        }
      }
      
      if (!found)
      {
        //Check if the uuid is still advertised in WLM and only close the consumers
        //if it isnt.
        if(!daManager.isQueuePointStillAdvertisedForGet(dmeUuid))
        {
          closeRemoteConsumer(key);
        }
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "closeRemoteConsumers");

    return;
  }
  
  /**
   * Method closeRemoteConsumer
   * <p>Close the remote consumers for a given remote ME
   * @param remoteMEUuid
   * @throws SIResourceException
   */
  public void closeRemoteConsumers(SIBUuid8 remoteMEId)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "closeRemoteConsumers", remoteMEId);

    Iterator aihs = _anycastInputHandlers.keySet().iterator();
    
    while (aihs.hasNext())
    {
      String key = (String) aihs.next();
      AnycastInputHandler aih = _anycastInputHandlers.get(key);
      if (aih.getLocalisationUuid().equals(remoteMEId))
        closeRemoteConsumer(key);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "closeRemoteConsumers");

    return;
  }
  
  /**
   * Method closeRemoteConsumer
   * <p>Close the remote consumers for a given remote ME
   * @param remoteMEUuid
   * @throws SIResourceException
   */
  public void closeRemoteConsumer(String key)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "closeRemoteConsumer", key);

    AnycastInputHandler aih = _anycastInputHandlers.get(key);

    if (aih != null)
    {
      ConsumerDispatcher rcd = aih.getRCD();

      if (rcd != null) 
      {
        rcd.closeAllConsumersForDelete(_baseDestinationHandler);
      }
    } 
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "closeRemoteConsumer");

    return;
  }

  /**
   * Method closeConsumers
   * <p>Tidy up remote state associated with the consumers.
   * @throws SIResourceException
   */
  abstract public void closeConsumers() throws SIResourceException;

  /**
   * Method notifyAOHReceiveExclusiveChange
   * @param newValue
   */
  public void notifyAOHReceiveExclusiveChange(boolean newValue)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "notifyAOHReceiveExclusiveChange",
        new Object[] { Boolean.valueOf(newValue) });
    
    // only need to do this for queues, not for durable subscriptions
    if (_anycastOutputHandler != null)
      _anycastOutputHandler.notifyReceiveExclusiveChange(newValue); 
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "notifyAOHReceiveExclusiveChange");
         
  }
  
  /**
   * Method notifyReceiveAllowedRCD
   * <p>Notify Remote Consumer Dispatchers consumers on the RME for this destination
   * of the change to the receive allowed attribute</p>
   * @param isReceiveAllowed
   * @param destinationHandler
   */
  abstract public void notifyReceiveAllowedRCD(DestinationHandler destinationHandler);

  /**
   * Method notifyRCDReceiveExclusiveChange
   * <p>Notify Remote Consumer Dispatchers consumers on the RME for this destination
   * of the change to the receive exclusive attribute</p>
   * @param isReceiveExclusive
   */
  public void notifyRCDReceiveExclusiveChange(boolean isReceiveExclusive)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "notifyRCDReceiveExclusiveChange", new Object[] {Boolean.valueOf(isReceiveExclusive)});

    // Only do this for ptp as it does not seem to make sense for p/s
    synchronized (_anycastInputHandlers) 
    { 
      for(Iterator i=_anycastInputHandlers.keySet().iterator(); i.hasNext();) 
      { 
        AnycastInputHandler aih = _anycastInputHandlers.get(i.next()); 
        if (aih != null) 
        { 
          RemoteConsumerDispatcher rcd = aih.getRCD();

          rcd.notifyReceiveExclusiveChange(isReceiveExclusive);
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "notifyRCDReceiveExclusiveChange");
  }

  /**
   * Method to indicate that the destination has been deleted   
   */
  public void setToBeDeleted()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setToBeDeleted");
    
    synchronized (_anycastInputHandlers) 
    { 
      _toBeDeleted = true;
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setToBeDeleted");
  }

  public abstract Iterator<AnycastInputControl> getAIControlAdapterIterator();
  
  public abstract Iterator<ControlAdapter> getAOControlAdapterIterator();
}
