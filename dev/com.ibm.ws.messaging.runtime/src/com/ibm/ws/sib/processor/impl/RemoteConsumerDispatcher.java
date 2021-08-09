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
package com.ibm.ws.sib.processor.impl;

import java.util.List;
import java.util.Set;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.LockingCursor;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.PersistenceException;
import com.ibm.ws.sib.msgstore.Statistics;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.interfaces.BrowseCursor;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumableKey;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumerKey;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumerPoint;
import com.ibm.ws.sib.processor.impl.interfaces.DispatchableConsumerPoint;
import com.ibm.ws.sib.processor.impl.interfaces.DispatchableKey;
import com.ibm.ws.sib.processor.impl.interfaces.JSKeyGroup;
import com.ibm.ws.sib.processor.impl.interfaces.RefillKey;
import com.ibm.ws.sib.processor.impl.interfaces.RemoteDispatchableKey;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.impl.store.SIMPTransactionManager;
import com.ibm.ws.sib.processor.impl.store.items.AIMessageItem;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream;
import com.ibm.ws.sib.processor.matching.MatchingConsumerPoint;
import com.ibm.ws.sib.processor.matching.MessageProcessorMatchTarget;
import com.ibm.ws.sib.processor.matching.MessageProcessorSearchResults;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.transactions.PersistentTranId;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIDestinationLockedException;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException;

/**
 * The following more detailed responsibilities need to be carried out in addition to those in the design doc:
 * - Call AIH.consumerAttaching. This allows the AIH to create the stream if necessary, in particular for card=1.
 *   This methods blocks until a stream is created.
 *  OLD COMMENT (discuss with Ignacio):
 *   Ater calling AIH.consumerAttaching, wait for readyToIssueGet before calling AIH.issueGet, to allow for the
 *   stream to be created if necessary; notice that we may wait forever since currently the DME does not have a
 *   message type to send if it can't create the stream due to error.
 *
 *  SYNCHRONIZATION: Like the ConsumerDispatcher, updates synchronize either on consumerPoints
 *   or readyConsumerPointLock.
 *   consumerCardinality changes and reads are synchronized on consumerPoints
 *
 */
public class RemoteConsumerDispatcher extends ConsumerDispatcher
{
  //Trace
  private static final TraceComponent tc =
    SibTr.register(
      RemoteConsumerDispatcher.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

 
  private final String _destName;
  private final AnycastInputHandler _aih;

  // making a change to consumer cardinality is asynchronous. After the consumers are notified,
  // the RCD has to wait till all the consumers detach and only then can it remove all the items and request
  // the stream to be flushed. The cardinalityChange value is set to true while this change is in progress,
  // and no new consumer attachment is permitted.
  private boolean _cardinalityOne; // is consumer cardinality = 1
  private boolean _cardinalityChange; // usually false. set to true while a cardinality change is in progress

  private boolean _forceFlushInProgress; // usually false. set to true while a force flush is in progress

  private final SIMPTransactionManager _tranManager;

  private boolean _currentReachability; // will not allow new ConsumerPoints from attaching if this is false

  private long lockID;
  
  // Flag to indicate if the RCD has been created purely to cleanup the AIH (for a remote subscription)
  private boolean _pendingDelete = false;

  /**
   * Constructor
   * @param destination
   * @param destName The destination name. For durable subscriptions, this is the name of the pseudo-destination
   *         and not the name of the 'destination' parameter
   * @param itemStream
   * @param dispatcherState
   * @param aih
   * @param tranManager
   */
  public RemoteConsumerDispatcher(BaseDestinationHandler destination, String destName,
                                  PtoPMessageItemStream itemStream,
                                  ConsumerDispatcherState dispatcherState,
                                  AnycastInputHandler aih,
                                  SIMPTransactionManager tranManager,
                                  boolean card)
  {
    super(destination, itemStream, dispatcherState);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "RemoteConsumerDispatcher",
        new Object[]{destination, destName, itemStream, dispatcherState, aih, tranManager, Boolean.valueOf(card)});

    _destName = destName;
    _aih = aih;
    _tranManager = tranManager;

    _currentReachability = true;
    
    try
    {    
      // Cache a lock ID to lock the items with          
      lockID = _messageProcessor.getMessageStore().getUniqueLockID(AbstractItem.STORE_NEVER);
    }
    catch (PersistenceException e)
    {
      // No FFDC code needed          
      SibTr.exception(tc, e);
    }

    synchronized (consumerPoints)
    {
      _forceFlushInProgress = false;
      _cardinalityOne = card;
      _cardinalityChange = false;
      if (_cardinalityOne)
      {
        aih.rejectAll(); // reject all the messages, and tell the AIH to flush the stream
      }
    }

    aih.initRCD(this);

    if (aih.getAIProtocolStream() != null && aih.getAIProtocolStream().isUnableToOrder())
    {
      // PK69943 We are still at a point where we don't know if ordering is enabled.
      // So we callback to the destination handler so it can display a message if we
      // later find ordering is requested.
      destination.setIsUnableToOrder(true);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "RemoteConsumerDispatcher", this);
  }

  public final String getDestName()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
    {
      SibTr.entry(tc, "getDestName");
      SibTr.exit(tc, "getDestName", _destName);
    }
    return _destName;
  }

  public final SIBUuid8 getLocalisationUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getLocalisationUuid");
    
    SIBUuid8 uuid = _aih.getLocalisationUuid();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getLocalisationUuid", uuid);
    return uuid;
  }

  /*
   * Called when the receiveExclusive value has changed
   */
  public final void notifyReceiveExclusiveChange(boolean newValue)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "notifyReceiveExclusiveChange", new Boolean(newValue));
    boolean notifyConsumers = true;
    synchronized (consumerPoints)
    {
      _cardinalityOne = newValue;
      _cardinalityChange = true;

      if (consumerPoints.size() == 0)
      {
        notifyConsumers = false;
      }
      // since we have set cardinalityChange to true, we will not allow any more consumers till we have
      // finished processing the cardinality change
    }

    // if there were consumers, we tell the CD to close all consumers.
    if (notifyConsumers)
      super.closeAllConsumersForReceiveExclusive();
    else
    {
      // all consumers are gone so finish processing the cardinality change
      // NOTE: this causes us to flush the stream.
      // Any outstanding transactions will rollback
      try
      {
        cardinalityChangeConsumersDetached();
      }
      catch(SIResourceException e)
      {
        // Exception shouldn't occur so FFDC and rethrow as runtime exception
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.RemoteConsumerDispatcher.notifyReceiveExclusiveChange",
          "1:203:1.75",
          this);

        SIErrorException e2 =
          new SIErrorException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0002",
              new Object[] { "com.ibm.ws.sib.processor.impl.RemoteConsumerDispatcher.notifyReceiveExclusiveChange",
                             "1:211:1.75",
                             e },
              null));

        SibTr.exception(tc, e);
        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.RemoteConsumerDispatcher.notifyReceiveExclusiveChange",
            "1:219:1.75",
            e });
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "notifyReceiveExclusiveChange", e2);
        throw e2;
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "notifyReceiveExclusiveChange");
  }

  /** overiding the method in the superclass */
  public ConsumerKey attachConsumerPoint(
    ConsumerPoint consumerPoint,
    SelectionCriteria criteria,
    SIBUuid12 connectionUuid,
    boolean readAhead,
    boolean forwardScanning,
    JSConsumerSet consumerSet) throws SIDestinationLockedException, SISelectorSyntaxException, SIDiscriminatorSyntaxException, SINotPossibleInCurrentConfigurationException, SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this,tc, "attachConsumerPoint",
        new Object[]{consumerPoint,
                     criteria,
                     connectionUuid,
                     new Boolean(readAhead),
                     new Boolean(forwardScanning),
                     consumerSet});
    
    ConsumerKey returnKey = null;
    
    // There are a few times that we're unable to accept new consumers, e.g. forcing a
    // flush or changing cardinality. None of these events should last very long so
    // we just try again a few times, if it's still in this state then something else has
    // probably gone wrong so we bomb out. We obviously can't hold a lock while we're
    // retrying.
    int chances = 5;
    boolean repeat = true;
    while(repeat)
    {
      repeat = false;
      
      synchronized (consumerPoints)
      {
        if (!_currentReachability)
        { // don't allow an attachment if DME is not reachable or a CardinalityChange is in progress or a forced flush
          // is in progress
          // throw exception
          SIResourceException e = new SIResourceException(
            nls.getFormattedMessage("ANYCAST_STREAM_UNAVAILABLE_CWSIP0471",
                                    new Object[]{getDestName(), SIMPUtils.getMENameFromUuid(getLocalisationUuid().toString())},
                                    null));
          SibTr.exception(tc, e);
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "attachConsumerPoint", e);
          throw e;
        }
        else if (_cardinalityOne)
        { // enforce cardinality
          if (consumerPoints.size() != 0)
          {
            // throw exception
            SIResourceException e = new SIResourceException(
              nls.getFormattedMessage("CONSUMERCARDINALITY_LIMIT_REACHED_CWSIP0472",
                                      new Object[]{getDestName(), getLocalisationUuid().toString()},
                                      null));
            SibTr.exception(tc, e);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
              SibTr.exit(tc, "attachConsumerPoint", e);
            throw e;
          }
        }
        // The stream is in a funny state, try again in a few milliseconds
        else if (_cardinalityChange || _forceFlushInProgress)
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Unable to attach at this point " + _cardinalityChange + ":" +
                            _forceFlushInProgress);
          repeat = true;
        }
        
        // Work out how long we should wait for a response from the DME based on whether 
        // we are gathering or not
        long responseTimeout = 0;
        if (consumerPoint.isGatheringConsumer())
          responseTimeout = SIMPConstants.GATHERING_ANYCAST_RESPONSE_INTERVAL; // 10 seconds
        else
          responseTimeout = SIMPConstants.ANYCAST_RESPONSE_INTERVAL; // 5 minutes
        
        // do this in sync block to prevent two concurrent attachConsumerPoints from succeeding
        // when cardinalityOne, and to ensure that reachability changes are notified.
        if(_aih.consumerAttaching(responseTimeout))
        {
          returnKey = super.attachConsumerPoint(consumerPoint,
              criteria,
              connectionUuid,
              readAhead,
              forwardScanning,
              consumerSet);
        }
        else
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Unable to attach at this point, flush work in progress");
          repeat = true;
        }

        // If we're unable to attach at this exact point, but there's a probability that
        // we will in a split second, we sleep for a short time and try again (unless it's
        // time to give up).
        if(repeat)
        {
          if(chances > 0)
          {
            try
            {
              // Release the consumerPoints lock while we're waiting
              consumerPoints.wait(100);
            }
            catch (InterruptedException e)
            {
              // No FFDC code needed
            }
            chances--;
          }
          else
          {
            // throw exception, should be rare
            SIResourceException e =
              new SIResourceException(
                nls.getFormattedMessage(
                  "ANYCAST_STREAM_NOT_FLUSHED_CWSIP0512",
                  new Object[]{_messageProcessor.getMessagingEngineName(), _destName},
                  null));
            SibTr.exception(tc, e);
            
            // As we don't expect this to have taken so long, we FFDC just in case
            // someone is interested
            FFDCFilter.processException(e,
                                        "com.ibm.ws.sib.processor.impl.RemoteConsumerDispatcher.attachConsumerPoint",
                                        "1:445:1.97.2.21",
                                        this);
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
              SibTr.exit(tc, "attachConsumerPoint", e);
            
            throw e;
          }
        } // repeat
      } // end synchronized (consumerPoints)
    } // while
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "attachConsumerPoint", returnKey);
    return returnKey;
  }

  public void detachConsumerPoint(ConsumerKey consumerKey) throws SIResourceException, SINotPossibleInCurrentConfigurationException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this,tc, "detachConsumerPoint", consumerKey);

    SIErrorException e2 = null;

    boolean cardinalityOneFlush = false;
    boolean cardinalityChangeDone = false;
    boolean forceFlushDone = false;
    try
    {
      synchronized (consumerPoints)
      {
        if (_cardinalityOne && !_cardinalityChange)
          cardinalityOneFlush = true;
      }

      if (cardinalityOneFlush)
      {
        if(TraceComponent.isAnyTracingEnabled()  && tc.isDebugEnabled())
        {
          SibTr.debug(tc, "cardinalityOneFlush");
        }
        //this work results in a flush of the stream.
        //We cannot do this if the stream has outstanding transactions so we wrapper into
        //a flush work item
        AnycastInputHandler.FlushWorkItem flushWork =
          new AnycastInputHandler.FlushWorkItem() {
          public void performWorkItem()
          {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
              SibTr.entry(tc, "performWorkItem");

            if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
              SibTr.debug(tc, "cardinalityOneFlush");
            }

            // this call will acquire a lock on AIH such that it is guaranteed that all calls to
            // RDC.put() for messages on this stream will have returned before rejectAll() returns
            // Therefore we can safely go through itemStream and remove the messages that have
            // already been put and reject them.
            _aih.rejectAll(); // reject everything and start flushing the stream

            // remove all the messages in the itemStream. All of them should be in available state since there is
            // no consumer.
            try
            {
              removeAllMessagesItemStream();
            }
            catch (Exception e)
            {
              // log and throw exception
              FFDCFilter.processException(e,
                  "com.ibm.ws.sib.processor.impl.RemoteConsumerDispatcher.performWorkItem","1:516:1.97.2.21",this);
              SibTr.exception(tc, e);
              throw new SIErrorException(e);
            }

          }
        };
        //perform this work
        _aih.performFlushWork(flushWork,
                              false); //we do not want to force the flush
      }
    }
    catch (Exception e)
    {
      // log and throw exception
      FFDCFilter.processException(e,
        "com.ibm.ws.sib.processor.impl.RemoteConsumerDispatcher.detachConsumerPoint","1:532:1.97.2.21",this);
      SibTr.exception(tc, e);

      e2 = new SIErrorException(e);
    }
    // Note that for cardinalityOneFlush, we are calling the super class detachConsumerPoint AFTER removing all
    // the items, and initiating the stream flush since we don't want another consumer to attach earlier.
    // If cardinalityChange is true, there is no such danger since we won't allow any consumers to attach till
    // cardinalityChange is set to false
    super.detachConsumerPoint(consumerKey);

    synchronized (consumerPoints)
    {
      // Notify the aih if the last card > 1 consumer has detached
      boolean nomoreCPs = (consumerPoints.size() == 0);
      if (nomoreCPs)
      {
        if (!_cardinalityOne && !_cardinalityChange && !_forceFlushInProgress)
        {
          //NOTE: this protects itself against flushing with outstanding transactions
          _aih.lastCardNConsumerDetached();
        }
        if (_cardinalityChange)
        {
          cardinalityChangeDone = true;
        }
        if (_forceFlushInProgress)
        {
          forceFlushDone = true;
        }
      }
    } // end synchronized

    if (cardinalityChangeDone)
    {

      //we are flushing so we need to ensure that this flush
      //is only carried out if there are no outstanding transactions on the
      //stream
      if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
        SibTr.debug(tc, "cardinalityChangeDone");
      }

      AnycastInputHandler.FlushWorkItem flushWork =
        new AnycastInputHandler.FlushWorkItem(){
          public void performWorkItem()
          {
            if (TraceComponent.isAnyTracingEnabled() &&tc.isEntryEnabled())
              SibTr.entry(tc, "performWorkItem");

            if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
              SibTr.debug(tc, "cardinalityChangeDone");
            }
            try
            {
              cardinalityChangeConsumersDetached(); // all consumers are gone so finish processing the cardinality change
            }
            catch (SIResourceException e)
            {
              FFDCFilter.processException(e,
                "com.ibm.ws.sib.processor.impl.RemoteConsumerDispatcher.performWorkItem","1:594:1.97.2.21",this);
              SibTr.exception(tc, e);
              if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "performWorkItem", "SIErrorException");
              throw new SIErrorException(e);
            }

          }
      };
      _aih.performFlushWork(flushWork,
                            false); //we do not want to force the flush
    }

    if (forceFlushDone)
    {
      //again we are flushing and again we need to ensure that this flush
      //is only carried out if there are no outstanding transactions on the
      //stream
      if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
        SibTr.debug(tc, "forceFlushDone");
      }
      AnycastInputHandler.FlushWorkItem flushWork =
        new AnycastInputHandler.FlushWorkItem()
        {
          public void performWorkItem()
          {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
              SibTr.entry(tc, "performWorkItem");

            if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
              SibTr.debug(tc, "forceFlushDone");
            }
            try {
              forceFlushConsumersDetached(); // all consumers are gone so finish processing the force flush
            }
            catch (SIResourceException e)
            {
              FFDCFilter.processException(e,
                  "com.ibm.ws.sib.processor.impl.RemoteConsumerDispatcher.performWorkItem","1:634:1.97.2.21",this);
              SibTr.exception(tc, e);
              if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "performWorkItem", "SIErrorException");
              throw new SIErrorException(e);
            }
          }
      };
      _aih.performFlushWork(flushWork,
                            false); //we do not want to force the flush

    }
    if (e2 != null)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "detachConsumerPoint", e2);
      throw e2;
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "detachConsumerPoint");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.Browsable#getBrowseCursor(com.ibm.ws.sib.store.Filter)
   */
  public BrowseCursor getBrowseCursor(SelectionCriteria criteria) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getBrowseCursor", criteria);
      SibTr.exit(tc, "getBrowseCursor");
    }
    return _aih.newBrowseCursor(criteria);
  }

  /**
   * This method is called by the AIH in the assured ordered case when an arriving message completes a list
   * Note, the messages are not currently in the itemStream
   * @param undeliveredMessages The list of messages to deliver
   */
  public void put(List undeliveredMessages, boolean isRestoring) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this,tc, "put", undeliveredMessages);
    int length = undeliveredMessages.size();

    // read the RemoteQPConsumerKey for later on updating the prefetching stuff.
    // need to do this before adding to item stream since spilling will wipe out this data
    // and there is no guarantee how soon that could occur.
    RemoteQPConsumerKey[] rcks = new RemoteQPConsumerKey[length];

    // add them all in one transaction
    LocalTransaction tran = null;
    try
    {
      tran = _tranManager.createLocalTransaction(true);
      for (int i=0; i<length; i++)
      {
        AIMessageItem msg = (AIMessageItem) undeliveredMessages.get(i);
        RemoteDispatchableKey dkey = msg.getAIStreamKey().getRemoteDispatchableKey();
        if (dkey instanceof RemoteQPConsumerKey)
          rcks[i] = (RemoteQPConsumerKey) dkey;

        itemStream.addItem(msg, (Transaction) tran);
        registerForEvents(msg);

        // Identify the fact that the message originated on a remote queue point
        // in order for consumer key groups to find the correct getCursor later on
        msg.setLocalisingME(getLocalisationUuid());
      }
      tran.commit();
    }
    catch (Exception e)
    {
      // log error
      FFDCFilter.processException(e,
        "com.ibm.ws.sib.processor.impl.RemoteConsumerDispatcher.put","1:708:1.97.2.21",this);
      SibTr.exception(tc, e);

      if (tran != null)
      {
        try
        {
          tran.rollback();
        }
        catch (Exception e2)
        {
          // log error
          FFDCFilter.processException(e2,
            "com.ibm.ws.sib.processor.impl.RemoteConsumerDispatcher.put","1:721:1.97.2.21",this);
          SibTr.exception(tc, e2);
        }
      }

      if (!isRestoring)
      {
        // tell the consumers, since ordering may be violated. A consumer that wants ordering guarantees
        // should detach and reattach
        SIIncorrectCallException e3 = new SIIncorrectCallException(
          nls.getFormattedMessage("PUTEXCEPTION_DISCONNECT_CONSUMER_CWSIP0473",
                                  new Object[]{getDestName(), getLocalisationUuid().toString(), e},
                                  null));
        dispatchExceptionToConsumers(e3);
  
        // reject the messages and also notify the waiting ConsumerKey
        for (int i=0; i<length; i++)
        {
          AIMessageItem msg = (AIMessageItem) undeliveredMessages.get(i);
          AIStreamKey key = msg.getAIStreamKey();
          _aih.reject(key);
          resolve(key); // Resolve so that we re-request a msg and dont forget about the request that just failed
        }
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "put", e);
      return;

    }
    
    // successfully added to itemStream
    for (int i=0; i<length; i++)
    {
      AIMessageItem msg = (AIMessageItem) undeliveredMessages.get(i);

      if (msg.isReserved())
        msg.restoreAOData(lockID);
        
      // If the above failed then we may no longer be reserved
      if (!msg.isReserved())
      {
        // update prefetching stuff
        if (rcks[i] != null)
          rcks[i].messageReceived(msg.getAIStreamKey());
  
        // call dispatchInternal()
        dispatchInternalAndHandleException(msg);
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "put");
  }

  private void dispatchExceptionToConsumers(SIIncorrectCallException e)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "dispatchExceptionToConsumers", e);
    Object[] clonedConsumerPoints = null;

    synchronized (consumerPoints)
    {
      // we clone the list before notifying the exception, since the CP's may call methods on the CD in the
      // context of the notification, and don't want a deadlock
      clonedConsumerPoints = consumerPoints.toArray();
    }

    if (clonedConsumerPoints != null)
    {
      // dispatch exception to ConsumerPoints
      for (int i=0; i<clonedConsumerPoints.length; i++)
      {
        DispatchableKey ck = (DispatchableKey) clonedConsumerPoints[i];
        ck.notifyConsumerPointAboutException(e);
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "dispatchExceptionToConsumers");
  }

  /**
   * Called by the AIH when a prior issueGet() gets a NO_DATA or LOST_DATA in response
   * @param decision The decision event, defined as a constant in SIMPConstants
   */
  public void resolve(AIStreamKey key) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "resolve", key);
     
    RemoteQPConsumerKey ck = (RemoteQPConsumerKey)key.getRemoteDispatchableKey();
    ConsumableKey senderKey = ck.getConsumerPoint().getConsumerKey();
    
    boolean stillReady = false;
    boolean rckClosed = false;

    synchronized (_baseDestHandler.getReadyConsumerPointLock())
    {
      if (ck.isKeyReady())
      {
        stillReady = true; // is the consumer still ready
      }
      else if (senderKey.isKeyReady()) // If we are gathering then senderKey will be the GCK
      {
        // If we are here then the consumerKey that initiated the original request has been closed
        // but the containing gathering consumer key is still open so we want to do a request 
        // from a different rck
        rckClosed = true;
        stillReady = true; // is the consumer still ready
      }
      else 
      {
        // we update the prefetch info within the readyConsumerPointLock, since we want to avoid the
        // following ordered events: consumer is not ready -> this method releases the readyConsumerPointLock
        // -> consumer becomes ready and waiting but because there is an outstanding get, a get is not issued
        // -> this method decreases the outstanding gets by 1, resulting in a value of 0.
        // Now the consumer is waiting (possibly with infinite timeout), but no get has been issued.
        // So it can wait forever.
        // We use completedReceivedNoPrefetch() since we don't want this method to issue a get since
        // while we are holding the readyConsumerPointLock (as this lock is shared across all ConsumerPoints
        // and we don't want it to become a bottleneck)
        ck.completedReceivedNoPrefetch(key, false);
      }
    }

    if (!stillReady)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "resolve");
      // since the consumer is not ready, we don't care if get request was not satisfied
      return;
    }

    // must try to reissue the get if possible
    if (!rckClosed)
      ck.completedReceived(key, true); 
    else // original rck is closed but we have the gathering parent
    {
      // Cancel the original request state
      ck.completedReceived(key, false); 
      
      // reissue the request on another rck
      if (key.getOriginalTimeout() == SIMPConstants.INFINITE_TIMEOUT)
      {
        // Original consumerkey has been closed so reissue Get on the gathering consumerKey
        senderKey.waiting(key.getOriginalTimeout(), false);
      }
      else
      {
        // non-infinite timeout. see if there is enough time left to reissue get
        long currTime = System.currentTimeMillis();
        long expectedResponseTime = currTime + _aih.getRoundTripTime();
        long timeoutTime = key.getIssueTime() + key.getOriginalTimeout();
        if (expectedResponseTime < timeoutTime)
        {
          // Original consumerkey has been closed so reissue Get on the gathering consumerKey
          senderKey.waiting(timeoutTime - currTime, false);
        }
      }                
    }
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "resolve");
  }

  /**
   * The reachability to the DME has changed
   * @param b true if the DME is reachable, else false
   */
  public void reachabilityChange(boolean reachable)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this,tc, "reachabilityChange", Boolean.valueOf(reachable));
    Object[] clonedConsumerPoints = null;
    synchronized (consumerPoints)
    {      
      // If we went from reachable to not reachable - clone the consumer points
      if (_currentReachability && !reachable)
      {
        // we clone the list before notifying the exception, since the CP's may call methods on the CD in the
        // context of the notification, and don't want a deadlock
        clonedConsumerPoints = consumerPoints.toArray();
      }
      _currentReachability = reachable;
    }
    if (clonedConsumerPoints != null)
    {
      // unreachable. throw exception to ConsumerPoints
      SIResourceException e = new SIResourceException(
        nls.getFormattedMessage("ANYCAST_STREAM_UNAVAILABLE_CWSIP0471",
                                new Object[]{getDestName(), SIMPUtils.getMENameFromUuid(getLocalisationUuid().toString())},
                                null));
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.exception(tc, e);

      for (int i=0; i<clonedConsumerPoints.length; i++)
      {
        DispatchableKey ck = (DispatchableKey) clonedConsumerPoints[i];
        
        // Only close non gathering consumer points - the 
        
        ck.getConsumerPoint().implicitClose(null, e, _aih.getLocalisationUuid() );
      }
    }
    if (TraceComponent.isAnyTracingEnabled() &&  tc.isEntryEnabled()) SibTr.exit(tc, "reachabilityChange");
  }

  /**
   * The disconnectCardOneConsumer method is invoked by the Anycast Input Handler to notify it that the current
   * cardinality-one consumer must be disconnected.
   * This can happen when this RME becomes unreachable and the DME allows a consumer in a separate RME
   * to connect. As soon as this RME becomes reachable again, the DME sends ControlCardinalityInfo to
   * trigger this consumer's disconnection.
   */
  public void disconnectCardOneConsumer()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "disconnectCardOneConsumer");
    Object[] clonedConsumerPoints = null;
    synchronized (consumerPoints)
    {
      // we clone the list before notifying the exception, since the CP's may call methods on the CD in the
      // context of the notification, and don't want a deadlock
      clonedConsumerPoints = consumerPoints.toArray();
    }
    SILimitExceededException  e = new SILimitExceededException (
      nls.getFormattedMessage("CONSUMERCARDINALITY_LIMIT_REACHED_CWSIP0472",
                               new Object[]{getDestName(), getLocalisationUuid().toString()},
                               null));
    FFDCFilter.processException(e,"com.ibm.ws.sib.processor.impl.RemoteConsumerDispatcher.disconnectCardOneConsumer","1:945:1.97.2.21",this);
    SibTr.exception(tc, e);
    for (int i=0; i<clonedConsumerPoints.length; i++)
    {
      DispatchableKey ck = (DispatchableKey) clonedConsumerPoints[i];
      ck.notifyConsumerPointAboutException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "disconnectCardOneConsumer");
  }

  /** overiding the method in the superclass, to create a RemoteQPConsumerKey */
  protected DispatchableKey createConsumerKey(DispatchableConsumerPoint consumerPoint,
                                            SelectionCriteria criteria,
                                            SIBUuid12 connectionUuid,
                                            boolean readAhead,
                                            boolean forwardScanning,
                                            JSConsumerSet consumerSet) throws SISelectorSyntaxException, SIDiscriminatorSyntaxException, SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createConsumerKey", new Object[] { consumerPoint,
                                                          criteria,
                                                          connectionUuid,
                                                          new Boolean(readAhead),
                                                          new Boolean(forwardScanning),
                                                          consumerSet});

    RemoteQPConsumerKey consKey =
      new RemoteQPConsumerKey(consumerPoint,
                            this,criteria,
                            connectionUuid,
                            readAhead,
                            forwardScanning);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createConsumerKey", consKey);
    return consKey;
  }

  /**
   * overriding the method in the superclass, to create a RemoteQPConsumerKeyGroup
   */
  protected JSKeyGroup createConsumerKeyGroup(JSConsumerSet consumerSet)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createConsumerKeyGroup", consumerSet);

    JSKeyGroup ckg = new RemoteQPConsumerKeyGroup(this);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createConsumerKeyGroup", ckg);

    return ckg;
  }

  public RemoteConsumerDispatcher getResolvedDurableCD(RemoteConsumerDispatcher rcd)
    throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "getResolvedDurableCD", rcd);
    
    RemoteConsumerDispatcher returnedRCD = _aih.getResolvedDurableCD(rcd);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getResolvedDurableCD", returnedRCD);
    
    return returnedRCD;
  }

  /**
   * Overriding super class method to do nothing
   */
  protected void eventPostCommitAdd(SIMPMessage msg, TransactionCommon transaction)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "eventPostCommitAdd", new Object[]{msg, transaction});
      SibTr.exit(tc, "eventPostCommitAdd");
    }
  }

  /**
   * Overriding super class method
   */
  protected void eventUnlocked(SIMPMessage msg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "eventUnlocked", msg);

    AIMessageItem message = (AIMessageItem) msg;

    //  dispatch internal will add it to the timeoutManager if it can't find a ready consumer
    dispatchInternalAndHandleException(message);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "eventUnlocked");
  }
  
  /* Called by our overridden unlock method in SIMPItem
   */
  protected void eventPreUnlocked(SIMPMessage msg, TransactionCommon tran)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "eventPreUnlocked", new Object[]{ msg, tran});
    
    if ((msg.guessRedeliveredCount() + 1) >= _baseDestHandler.getMaxFailedDeliveries())
    {
      // Override the check of the threshold from the local case because we only
      // reroute to the exception destination if we are local to the queuepoint.
      // If the threshold is reached we remove the AIMessageItem and drive the expiry
      // code - which should flow the relevant redeliveryCount back to the DME
      // in a reject.
      
      boolean tranCreated = false;
      if (tran == null)
      {
        /* Create a new transaction under which to perform the reroute */
        tran = _tranManager.createLocalTransaction(false);
        tranCreated = true;
      }
      
      // Perform a forced expiry
      ((AIMessageItem)msg).setRejectTransactionID(tran.getPersistentTranId());
      // Increment the unlockCount
      _aih.incrementUnlockCount(msg.getMessage().getGuaranteedRemoteGetValueTick());
      
      // Remove msg
      try
      {
        if (msg.isInStore())
        {
          Transaction msTran = _messageProcessor.resolveAndEnlistMsgStoreTransaction(tran);
          msg.remove(msTran, msg.getLockID());
        }
        
        if (tranCreated)
          ((LocalTransaction)tran).commit();
        
        // If successful, make sure we dont try to unlock the msg
        msg.setRedeliveryCountReached();
      }
      catch (MessageStoreException e) 
      {
        // FFDC
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.RemoteConsumerDispatcher.eventPreUnlocked",
          "1:1090:1.97.2.21",
          this);

        SibTr.exception(tc, e);
        // Any exception will mean we wait until expiry for a reject
      }
      catch (SIException e)
      {
        // FFDC
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.RemoteConsumerDispatcher.eventPreUnlocked",
          "1:1102:1.97.2.21",
          this);

        SibTr.exception(tc, e);
        // Any exception will mean we wait until expiry for a reject
      }      
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "eventPreUnlocked");
  }

  private final void dispatchInternalAndHandleException(AIMessageItem msg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "dispatchInternalAndHandleException", msg);
    try
    {
      dispatchInternal(msg);
    }
    catch (SIException e)
    {
      // log exception
      FFDCFilter.processException(e,
        "com.ibm.ws.sib.processor.impl.RemoteConsumerDispatcher.dispatchInternalAndHandleException",
        "1:1126:1.97.2.21",this);
      SibTr.exception(tc, e);

    }
    catch (MessageStoreException e)
    {
      // log exception - this should only occur with remote durable + noLocal
      FFDCFilter.processException(e,
        "com.ibm.ws.sib.processor.impl.RemoteConsumerDispatcher.dispatchInternalAndHandleException",
        "1:1135:1.97.2.21",this);
      SibTr.exception(tc, e);

    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "dispatchInternalAndHandleException");
  }

  /**
   * This is a simplification of the ConsumerDispatcher.internalPut() method.
   * @param msg
   */
  private void dispatchInternal(AIMessageItem msg) throws SIException, MessageStoreException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "dispatchInternal", msg);

    // try to give it to the ConsumerKey that requested this message
    RemoteDispatchableKey dkey = msg.getAIStreamKey().getRemoteDispatchableKey();
    RemoteQPConsumerKey readyConsumer = null;
    if (dkey instanceof RemoteQPConsumerKey)
      readyConsumer = (RemoteQPConsumerKey) dkey;

    if (readyConsumer != null)
    {
      synchronized (_baseDestHandler.getReadyConsumerPointLock())
      {
        if (!readyConsumer.isKeyReady())
          readyConsumer = null;
        else
        {
          readyConsumer.markNotReady();
          dkey = (RemoteDispatchableKey) readyConsumer.getParent();
          removeReadyConsumer(dkey, !dkey.hasNonSpecificConsumers());
        }
      }
    }

    boolean accepted = false;
    if (readyConsumer != null)
    {
      // If noLocal is set to true and...
      // If the ProducerConnectionID is the same as the ConsumerConnection ID then we
      // do not deliver or store this message, but we DO accept it so that it is
      // removed from the subscription.
      if (dispatcherState.isNoLocal()
        && readyConsumer.getConnectionUuid().equals(msg.getProducerConnectionUuid()))
      {
        // Not only do we accept the message but we also immediately
        // remove it from the item stream.  Note: this must be an external
        // local transaction because the AIH will accept the message in
        // its preCommitRemove method.
        accepted = true;
        
        // 610985: We must lock the message before removing it as there's a chance
        // that another thread may have just locked this message. So if we were to
        // remove it without checking that it's ours (by attempting to lock it) then
        // it would come as a bit of a surprise to the other thread when it can no-longer
        // find the message it just locked
        // Even if we don't manage to lock it we consider this message delivered as
        // someone else is obviously on the ball with this one.
        if(msg.lockItemIfAvailable(lockID))
        {
          LocalTransaction tran = null;
          tran = _tranManager.createLocalTransaction(true);
          msg.remove((Transaction) tran,lockID);
          tran.commit();
        }

        // Also, give the LCP a chance to reissue the get in case it's blocked.
        ((JSLocalConsumerPoint) readyConsumer.consumerPoint).waitingNotify();
      } else {
        readyConsumer.updateLastNotReadyTime();
        accepted = readyConsumer.consumerPoint.put(msg, true);
      }
    }

    if (!accepted)
    {
      readyConsumer = null;
      boolean continueSearch = true;

      boolean newMatchRequired = false; // makes atmost one transition to true, and then to false
      MatchingConsumerPoint[] matchResults = null; // initialized when newMatchRequired set to true
      int startPoint, index; // real initialization occurs when matchResults initialized
      startPoint = index = 0;

      boolean grabCurrentReadyVersion = true;
      long newestReadyVersion = 0;
      while (continueSearch)
      {
        if (newMatchRequired)
        {
          // Get a search results object to use
          MessageProcessorSearchResults searchResults =
            (MessageProcessorSearchResults)_messageProcessor.
              getSearchResultsObjectPool().
                remove();

          // Defect 382250, set the unlockCount from MsgStore into the message
          // in the case where the message is being redelivered.
          JsMessage searchMsg = msg.getMessage();
          int redelCount = msg.guessRedeliveredCount();

          if (redelCount > 0)
          {
            if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
              SibTr.debug(tc, "Set deliverycount into message: " + redelCount);
            searchMsg.setDeliveryCount(redelCount);
          }

          // Search the match space.
          _messageProcessor.
            getMessageProcessorMatching().
              retrieveMatchingConsumerPoints(_baseDestHandler.getUuid(), // the uuid of the destination
                                             getUuid(), // the uuid of the remote consumer dispatcher
                                             searchMsg,
                                             searchResults);

          Object allResults[] = searchResults.getResults(_baseDestHandler.getName());

          Set matchSet =
            (Set) allResults[MessageProcessorMatchTarget.JS_CONSUMER_TYPE];
          matchResults =
           (MatchingConsumerPoint[])matchSet.toArray(new MatchingConsumerPoint[0]);

          //193008 - this result set might be empty
          int resultSize = matchResults.length;
          if(resultSize >0 )
          {
            startPoint = msg.getProducerSeed()%resultSize;
            index = startPoint;
          }
          else
          {
            //we have exhausted our search
            break;
          }

          newMatchRequired = false;
        }

        synchronized (_baseDestHandler.getReadyConsumerPointLock())
        {
          if (grabCurrentReadyVersion)
          { // this is done the first time in the while loop
            newestReadyVersion = readyConsumerVersion;
            grabCurrentReadyVersion = false;
          }

          if (matchResults == null)
          { // still searching non-specific ready consumers
            dkey = (RemoteDispatchableKey) nonSpecificReadyCPs.getFirst();
            readyConsumer = null;
            if (dkey != null)
            {
              if (dkey.getVersion() > newestReadyVersion)
              {
                dkey = null;
              }
            }
            if (dkey == null)
            { // done searching non-specific ready consumers
              newMatchRequired = true; // do a match the next time through the while loop
            }
            else
            {
              // may have a RemoteQPConsumerKeyGroup, so resolve to a particular RemoteQPConsumerKey
              readyConsumer = (RemoteQPConsumerKey) dkey.resolvedKey();
            }
          } // end if (matchResults == null)
          else
          { // searching through specific consumers
            readyConsumer = null;
            // Check for a ready consumer
            while (readyConsumer == null)
            {
              DispatchableKey ck =
                matchResults[index].getConsumerPointData();

              // Find a ready consumer that is old enough for us
              // SIB0113a We only want the remote consumer matches. There might
              // be local consumers in the matchspace too.
              if( (ck.isKeyReady())
                &&(ck.getVersion() <= newestReadyVersion)
                && ck instanceof RemoteDispatchableKey)
              {
                // Remember this consumer but we won't try to deliver the message
                // until the lock is released.
                readyConsumer = (RemoteQPConsumerKey) ck;
              }
              else
              {
                // Move on to the next consumer but check we haven't looped round
                // to the start.
                index = (index + 1) % matchResults.length;
                if (index == startPoint)
                  break;
              } // end else
            } // end while (readyConsumer == null)
            if (readyConsumer == null)
            { // done searching specific ready consumers
              continueSearch = false;
            }
          } // end else

          if (readyConsumer != null)
          {
            readyConsumer.markNotReady();
            dkey = (RemoteDispatchableKey) readyConsumer.getParent();
            removeReadyConsumer(dkey, !dkey.hasNonSpecificConsumers());

          }
        } // end synchronized
        if (readyConsumer != null)
        {
          readyConsumer.updateLastNotReadyTime();
          accepted = readyConsumer.consumerPoint.put(msg, true);
          continueSearch = !accepted;
        }
      } // end while
    } // end if (!accepted)
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "dispatchInternal");
  }

  public final long getRoundTripTime()
  {
    return _aih.getRoundTripTime();
  }

  public final AIStreamKey issueGet(SelectionCriteria[] criterias, long timeout, RemoteDispatchableKey ck, RefillKey refillCallback) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this,tc, "issueGet", new Object[] {criterias, Long.valueOf(timeout), ck});
    
    AIStreamKey key = _aih.issueGet(criterias, timeout, ck, null, refillCallback);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "issueGet");
    
    return key;
  }

  public final AIStreamKey[] issueGet(SelectionCriteria[] criterias, int count, RemoteDispatchableKey ck) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this,tc, "issueGet", new Object[] {criterias, Integer.valueOf(count), ck});
    AIStreamKey[] aikeys = _aih.issueGet(criterias, count, ck);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "issueGet", aikeys);
    return aikeys;
  }

  /**
   * The timeperiod after which a message that was added to the itemStream, and is still in unlocked state,
   * should be rejected. This is important to not starve other remote MEs
   */
  public long getRejectTimeout()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "getRejectTimeout");
    boolean localCardinalityOne;
    synchronized (consumerPoints)
    {
      localCardinalityOne = _cardinalityOne;
    }
    if (localCardinalityOne)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getRejectTimeout", new Long(AbstractItem.NEVER_EXPIRES));
      // there is no one else to starve
      return AbstractItem.NEVER_EXPIRES;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getRejectTimeout",
      new Long(_messageProcessor.getCustomProperties().get_unlocked_reject_interval()));
      return _messageProcessor.getCustomProperties().get_unlocked_reject_interval();

  }

  /**
   *
   * @return the AnyCastInputHandler for this remote consumer dispatcher
   */
        public AnycastInputHandler getAnycastInputHandler()
        {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                {
                        SibTr.entry(tc, "getAnycastInputHandler");
                        SibTr.exit(tc, "getAnycastInputHandler", new Object[] {_aih});
                }
                return _aih;
        }

  /*
   * Helper method. Called when all the consumers have been detached.
   *
   */
  private void cardinalityChangeConsumersDetached() throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "cardinalityChangeConsumersDetached");

    // Tell the AIH to start the flush.
    // this call will acquire a lock on AIH such that it is guaranteed that all calls to
    // RDC.put() for messages on this stream will have returned before rejectAll() returns
    // Therefore we can safely go through itemStream and remove the messages that have
    // already been put and reject them.
    _aih.rejectAll(); // reject everything and start flushing the stream

    // then delete all the messages, since know that put() must have completed
    removeAllMessagesItemStream();

    synchronized (consumerPoints)
    {
      // set cardinalityChange to false. Note that any consumers that try to attach right after this will not
      // be able to use the old stream since already initiated the flush
      _cardinalityChange = false;
      // tell the AIH to change its cardinality
      _aih.changeReceiveExclusive(_cardinalityOne);

    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "cardinalityChangeConsumersDetached");

  }

  private void removeAllMessagesItemStream() throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "removeAllMessagesItemStream");
    try
    {

      // remove all the messages in the itemStream. All of them should be in available state since there is
      // no consumer - although we could have some expiring, see below.
      LocalTransaction tran = _tranManager.createLocalTransaction(true);
      LockingCursor cursor = itemStream.newLockingItemCursor(null);
      AbstractItem item = null;
      PersistentTranId tranId = tran.getPersistentTranId();
      while ((item = cursor.next()) != null)
      {
        // since we are setting the rejectTransactionID, we will get an expiryCallback() which will
        // result in an aih.reject() call when the transaction commits. This is redundant code since
        // we have already called aih.rejectAll() but we are doing it anyway.
        ((AIMessageItem) item).setRejectTransactionID(tranId);
        item.remove((Transaction) tran, item.getLockID());
      }

      tran.commit();
      cursor.finished();

      // Now check to see if we have any messages still left on the ItemStream that would prevent us
      // from deleting it (524796)
      Statistics stats = itemStream.getStatistics();
      long expiringItems = stats.getExpiringItemCount();
      long items = stats.getTotalItemCount();
      
      if(items != 0)
      {
        // If the remaining items are expiring we give them a couple of seconds to complete (they should only
        // need to commit their transactions).
        int count = 0;
        while((expiringItems > 0) && count < 20)
        {
          try
          {
            Thread.sleep(100);
          }
          catch (InterruptedException e)
          {
            // No FFDC code needed
          }
          
          expiringItems = stats.getExpiringItemCount();
          count++;
        }
        
        // If there's still items left something must be wrong, we better throw an exception
        if((expiringItems > 0) || (stats.getTotalItemCount() > 0))
        {
          // Dump the current stats
          String statTxt = stats.getTotalItemCount() + ":" +
                           stats.getAddingItemCount() + ":" +
                           stats.getAvailableItemCount() + ":" +
                           stats.getExpiringItemCount() + ":" +
                           stats.getLockedItemCount() + ":" +
                           stats.getRemovingItemCount() + ":" +
                           stats.getUnavailableItemCount() + ":" +
                           stats.getUnavailableItemCount();
          
          SIErrorException e =
            new SIErrorException(
              nls.getFormattedMessage(
                "INTERNAL_MESSAGING_ERROR_CWSIP0005",
                new Object[] { "com.ibm.ws.sib.processor.impl.RemoteConsumerDispatcher.removeAllMessagesItemStream",
                    "1:1525:1.97.2.21",
                    statTxt
                },
                null));

          FFDCFilter.processException(e,
              "com.ibm.ws.sib.processor.impl.RemoteConsumerDispatcher.removeAllMessagesItemStream","1:1531:1.97.2.21",this);

          SibTr.exception(tc, e);

          // Now wrap it in an SIResourceException and throw that
          SIResourceException e2 = new SIResourceException(e);
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "removeAllMessagesItemStream", e2);

          throw e2;
        }
      }
    }
    catch (MessageStoreException e)
    {
      // No FFDC code needed
      SIResourceException e2 = new SIResourceException(e);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "removeAllMessagesItemStream", e2);

      throw e2;
    }
    catch (SIIncorrectCallException e)
    {
      // No FFDC code needed
      SIResourceException e2 = new SIResourceException(e);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "removeAllMessagesItemStream", e2);

      throw e2;
    }
    catch (SIConnectionLostException e)
    {
      // No FFDC code needed
      SIResourceException e2 = new SIResourceException(e);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "removeAllMessagesItemStream", e2);

      throw e2;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "removeAllMessagesItemStream");
  }

  /**
   * NOTE: thsi method causes the stream to flush.
   * This will cause any outstanding transactions to rollback.
   * Therefore this method should only be called it it is acceptable for that to happen.
   */
  public void closeAllConsumersForFlush()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this,tc, "closeAllConsumersForFlush");

    boolean notifyConsumers = true;
    Object[] clonedConsumerPoints = null;

    synchronized (consumerPoints)
    {

      _forceFlushInProgress = true;

      if (consumerPoints.size() == 0)
      {
        notifyConsumers = false;
      }
      else
      {
        // we clone the list before notifying the exception, since the CP's may call methods on the CD in the
        // context of the notification, and don't want a deadlock
        clonedConsumerPoints = consumerPoints.toArray();
      }

      // since we have set forceFlushInProgress to true, we will not allow any more consumers till we have
      // finished
    }

    // if there were consumers, we tell them to close.
    if (notifyConsumers)
    {
      // unreachable. throw exception to ConsumerPoints
      SIResourceException e = new SIResourceException(
        nls.getFormattedMessage("ANYCAST_STREAM_UNAVAILABLE_CWSIP0471",
                              new Object[]{getDestName(), SIMPUtils.getMENameFromUuid(getLocalisationUuid().toString())},
                              null));

      for (int i=0; i<clonedConsumerPoints.length; i++)
      {
        DispatchableKey ck = (DispatchableKey) clonedConsumerPoints[i];
        ck.getConsumerPoint().implicitClose(null,e,_aih.getLocalisationUuid());
      }
    }
    else
    {
      try
      {
        // all consumers are gone so finish processing the cardinality change
        // NOTE: this will flush the stream.
        forceFlushConsumersDetached();
      }
      catch (SIResourceException e)
      {
        // log and throw exception
        FFDCFilter.processException(e,
          "com.ibm.ws.sib.processor.impl.RemoteConsumerDispatcher.closeAllConsumersForFlush","1:1630:1.97.2.21",this);
        SibTr.exception(tc, e);

        SIErrorException e2 = new SIErrorException(e);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "closeAllConsumersForFlush", e2);
        throw e2;
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "closeAllConsumersForFlush");
  }

  /*
   * Helper method. Called when all the consumers have been detached.
   *
   */
  private void forceFlushConsumersDetached() throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "forceFlushConsumersDetached");
    // Tell the AIH to start the flush.
    // this call will acquire a lock on AIH such that it is guaranteed that all calls to
    // RDC.put() for messages on this stream will have returned before rejectAll() returns
    // Therefore we can safely go through itemStream and remove the messages that have
    // already been put and reject them.
    _aih.rejectAll(); // reject everything and start flushing the stream

    // then delete all the messages, since know that put() must have completed
    removeAllMessagesItemStream();

    synchronized (consumerPoints)
    {
      // set forceFlushInProgress to false. Note that any consumers that try to attach right after this will not
      // be able to use the old stream since already initiated the flush
      _forceFlushInProgress = false;
    }
    // tell the AIH that force flush completed
    _aih.closeAllConsumersForFlushDone();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "forceFlushConsumersDetached");
  }

  /**
   * @param transaction
   * @return
   */
  public boolean isNewTransactionAllowed(TransactionCommon transaction) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.entry(tc, "isNewTransactionAllowed", transaction);

      boolean allowed = true;

      synchronized(orderLock)
      {
        if (streamHasInDoubtRemoves)
        {
          if (_aih.getAIProtocolStream() != null)
            currentTran = _aih.getAIProtocolStream().getOrderedActiveTran();

          if (currentTran == null)
            streamHasInDoubtRemoves = false;
        }

        // If the current tran is null or the same as the tran provided - disallow new trans
        if (currentTran != null &&
            (transaction == null || !currentTran.equals(transaction.getPersistentTranId())))
          allowed = false;
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "isNewTransactionAllowed", new Boolean(allowed));
      return allowed;
  }

  /*
   * Flag used by the remote durable pubsub logic to handle RCDs that have been created
   * to perform flush work
   */
  public synchronized void setPendingDelete(boolean delete)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this,tc, "setPendingDelete", Boolean.valueOf(delete));
    
    _pendingDelete = delete;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setPendingDelete");
  }

  public synchronized boolean getPendingDelete()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(this,tc, "getPendingDelete");
      SibTr.exit(tc, "getPendingDelete", Boolean.valueOf(_pendingDelete));
    }
    
    return _pendingDelete;
  }
}
