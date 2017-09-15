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
package com.ibm.ws.sib.processor.impl.store.itemstreams;

// Import required classes.
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.OutOfCacheSpace;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.StreamIsFull;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.MPSelectionCriteria;
import com.ibm.ws.sib.processor.MPSelectionCriteriaFactory;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.ConsumerDispatcher;
import com.ibm.ws.sib.processor.impl.ConsumerDispatcherState;
import com.ibm.ws.sib.processor.impl.DestinationManager;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.SelectionCriteriaFactory;
import com.ibm.wsspi.sib.core.SelectorDomain;

/**
 *
 * Durable Sub Item object to be placed on an ItemStream. A subState object is attached
 */

public final class DurableSubscriptionItemStream extends SubscriptionItemStream
{
  // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  private static final TraceComponent tc =
    SibTr.register(
      DurableSubscriptionItemStream.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  /**
   * Object version number.
   */
  private static final int PERSISTENT_VERSION = 3;

  /**
   * This object will be the serialised object placed on the itemstream.
   */
  private BaseDestinationHandler _bdh;
  private ConsumerDispatcherState _subState;

  private int[] _domains;
  private String[] _selectors;
  private String[] _topics;
  private Map[] _selectorProperties;

  /**
   * The reference to the destination manager on this ME
   */
  private DestinationManager _destinationManager;

  /**
   * A durable subscription item stream can be in doubt.
   * This occurs when a transaction sending a message has been put in doubt -
   * e.g. server stop between prepare and commit.
   * The message item reference will be restored before MP has started
   * and this must be handled by putting the stream in doubt until
   * the reference and the stream are properly restored.
   * See defect 257231
   */
  private boolean _inDoubt=false;



  /**
   * Cold Constructor.  Create a durable subscription, then adds itself to the
   * given parent ItemStream.
   *
   * @param subState
   * @param destinationManager
   * @param parentItemStream    The ItemStream to add the consructed object to.
   * @param transaction         The transaction to use to add the constructed
   *         object to the Message Store.
   *
   * @throws MessageStoreException if there was a problem interacting with the
   *          Message Store.
   */
  public DurableSubscriptionItemStream(ConsumerDispatcherState subState,
    DestinationManager destinationManager,
    PubSubMessageItemStream parentItemStream,
    Transaction msTran)
    throws OutOfCacheSpace, MessageStoreException, StreamIsFull
  {
    super();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "DurableSubscriptionItemStream",
      new Object[]
        { subState, destinationManager, parentItemStream, msTran });

    _subState = subState;
    _destinationManager = destinationManager;

    /*
     * Always make the subscription initially unusable. This will change if
     * the durable subscription creation is committed successfully.
     */
    subState.setReady(false);

    setStorageStrategy(STORE_ALWAYS);

    parentItemStream.addReferenceStream(this, msTran);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "DurableSubscriptionItemStream", this);
  }

  /**
   * Blank Constructor. Required by any object that implements
   * ReferenceStream. This is
   * so the messagestore can instantiate the object
   *
   * public method as driven by MessageStore
   */
  public DurableSubscriptionItemStream()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "DurableSubscriptionItemStream");

    // Create an new empty subscription state
    this._subState = new ConsumerDispatcherState();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "DurableSubscriptionItemStream", this);
  }

  /*
   * (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPReferenceStream#getVersion()
   */
  public int getPersistentVersion()
  {
    return PERSISTENT_VERSION;
  }

  /*
   * Inherited javadoc.
   */
  public void getPersistentData(ObjectOutputStream oos)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getPersistentData", oos);

    try
    {
      HashMap hm = new HashMap();

      hm.put("subscriberID", _subState.getSubscriberID());
      hm.put("topicspace", _subState.getTopicSpaceUuid().toString());
      hm.put("topicspaceName", _subState.getTopicSpaceName());
      hm.put("topicspaceBusName", _subState.getTopicSpaceBusName());
      hm.put("targetDestination", _subState.getTargetDestination());
      hm.put("isToBeDeleted", new Boolean(toBeDeleted));

      hm.put("user", _subState.getUser());
      hm.put("isSIBServerSubject", new Boolean(_subState.isSIBServerSubject()));
      hm.put("topics", _subState.getTopics());
      hm.put("isCloned", new Boolean(_subState.isCloned()));
      hm.put("isNoLocal", new Boolean(_subState.isNoLocal()));

      // New for version 2
      SelectionCriteria[] selectionCriteriaList = _subState.getSelectionCriteriaList();

      String[] selectors = null;
      int[] domains = null;
      Map[] selectorProperties = null;
      if( selectionCriteriaList != null)
      {
        selectors = new String[selectionCriteriaList.length];
        domains = new int[selectionCriteriaList.length];
        selectorProperties = new HashMap[selectionCriteriaList.length];

        for( int i=0; i<selectionCriteriaList.length; i++)
        {
          selectors[i] = (selectionCriteriaList[i] == null) ?
                         null : selectionCriteriaList[i].getSelectorString();

          domains[i]   = (selectionCriteriaList[i] == null) ?
                          SelectorDomain.SIMESSAGE.toInt() : selectionCriteriaList[i].getSelectorDomain().toInt();

          selectorProperties[i] = null;
          // Set up selectorProperties if appropriate
          if(selectionCriteriaList[i] != null)
          {
            if(selectionCriteriaList[i] instanceof MPSelectionCriteria)
            {
              MPSelectionCriteria mpCriteria = (MPSelectionCriteria)selectionCriteriaList[i];
              selectorProperties[i] = mpCriteria.getSelectorProperties();
            }
          }

        }
      }
      hm.put("selectors",selectors);
      hm.put("domains",domains);
      hm.put("selectorProperties", selectorProperties);

      hm.put("userData", _subState.getUserData());

      oos.writeObject(hm);
    }
    catch (IOException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.store.itemstreams.DurableSubscriptionItemStream.getPersistentData",
        "1:275:1.95",
        this);

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getPersistentData", e);

      throw new SIErrorException(
        nls.getFormattedMessage(
          "SUBSCRIPTION_WARM_START_ERROR_CWSIP0148",
          new Object[] { _destinationManager.getLocalME().getMessagingEngineName(), e },
          null),
        e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getPersistentData");
  }

  public void initializeNonPersistent(BaseDestinationHandler baseDestHandler)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "initializeNonPersistent", baseDestHandler);
    _bdh = baseDestHandler;
    _destinationManager = _bdh.getDestinationManager();

    // Build selectionCriteriaList from the _topics, _selectors and _domains arrays
    SelectionCriteria[] selectionCriteriaList = null;
    // Defect 533945: the _selectors array may be null
    if(_selectors != null)
    {
      SelectionCriteriaFactory selFactory = _destinationManager.getLocalME().getSelectionCriteriaFactory();
      selectionCriteriaList = new SelectionCriteria[_selectors.length];
      for ( int i=0; i<_selectors.length; i++)
      {
        // We'll create SelectionCriteria based on the properties we've been passed.
        if(_selectorProperties[i] == null)
        {
          selectionCriteriaList[i] = selFactory.createSelectionCriteria(_topics[i],
                                                                        _selectors[i],
                                                                        SelectorDomain.getSelectorDomain(_domains[i]));
        }
        else
        {
          // Non-null selectorProperties, so we create MPSelectionCriteria
          selectionCriteriaList[i] =
            MPSelectionCriteriaFactory.
              getInstance().
                createSelectionCriteria(_topics[i],
                                        _selectors[i],
                                        SelectorDomain.getSelectorDomain(_domains[i]),
                                        _selectorProperties[i]);
        }
      } // eof iterate over selectors
    } // eof non-null _selectors array
    
    // Set the possibly null list into the ConsumerDispatcherState
    _subState.setSelectionCriteriaListOnRestore(selectionCriteriaList);

    // For durable home, always use the local ME's name since that's where
    // we're restoring it.
    _subState.setDurableHome(_destinationManager.getLocalME().getMessagingEngineName());
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "initializeNonPersistent");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.store.AbstractItem#restore(byte[])
   */
  public void restore(ObjectInputStream ois, int dataVersion)
  throws SevereMessageStoreException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc,
        "restore",
        new Object[] { ois, new Integer(dataVersion) });

   // checkPersistentVersionId(dataVersion);
    if (dataVersion > getPersistentVersion())
    {

      SibTr.error(tc, "ITEM_RESTORE_ERROR_CWSIP0261",
        new Object[] {new Integer(getPersistentVersion()),
                      new Integer(dataVersion)});

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "restore", "SIErrorException");

      throw new SevereMessageStoreException(
        nls.getFormattedMessage(
          "ITEM_RESTORE_ERROR_CWSIP0261",
          new Object[] {new Integer(getPersistentVersion()),
                        new Integer(dataVersion) },
          null));
    }

    BaseDestinationHandler baseDestHandler = null;

    PubSubMessageItemStream mis = (PubSubMessageItemStream)getItemStream();
    baseDestHandler  = (BaseDestinationHandler)mis.getItemStream();
    if (baseDestHandler != null) _destinationManager = baseDestHandler.getDestinationManager();

    // F001333-14610
    // Make sure we set the durable flag on our subState
    _subState.setDurable(true);

    //first we initialize the persistent data
    try
    {
      HashMap hm = (HashMap)ois.readObject();
      _subState.setSubscriberID((String)hm.get("subscriberID"));
      _subState.setTopicSpaceUuid(new SIBUuid12((String)hm.get("topicspace")));
      _subState.setTopicSpaceName((String)hm.get("topicspaceName"));
      _subState.setTopicSpaceBusName((String)hm.get("topicspaceBusName"));
      toBeDeleted = ((Boolean)hm.get("isToBeDeleted")).booleanValue();

      boolean isSIBServerSubject = ((Boolean)hm.get("isSIBServerSubject")).booleanValue();
      _subState.setUser((String)hm.get("user"), isSIBServerSubject);
      _topics = (String[])hm.get("topics");
      _subState.setIsCloned(((Boolean)hm.get("isCloned")).booleanValue());
      _subState.setNoLocal(((Boolean)hm.get("isNoLocal")).booleanValue());

      // selectorStrings and domains arrived in version 1 of DurableSubscriptionItemStreams
      if( dataVersion < 2 )
      {
        if (_selectors == null)
          this._selectors = new String[1];

        if (_domains == null)
          this._domains = new int[1];

        this._selectors[0] = (String)hm.get("selectorstring");
        this._domains[0] = ((Integer)hm.get("selectordomain")).intValue();
      }
      else
      {
        _subState.setTargetDestination((String)hm.get("targetDestination"));
        this._selectors = (String[])hm.get("selectors");
        this._domains = (int[])hm.get("domains");
        this._selectorProperties = (HashMap[])hm.get("selectorProperties");
        this._subState.setUserData((HashMap)hm.get("userData"));        
      }
      
      // SelectorProperties arrived in version 2 of DurableSubscriptionItemStreams
      if( dataVersion < PERSISTENT_VERSION)
      {
        if(_selectorProperties == null)
          this._selectorProperties = new HashMap[1];        
      }
      else
      {
        this._selectorProperties = (HashMap[])hm.get("selectorProperties");
      }
    }
    catch (Exception e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.store.itemstreams.DurableSubscriptionItemStream.restore",
        "1:434:1.95",
        this);

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "restore", e);

      throw new SIErrorException(
        nls.getFormattedMessage(
        "SUBSCRIPTION_WARM_START_ERROR_CWSIP0148",
        new Object[] { _destinationManager.getLocalME().getMessagingEngineName(), e },
          null),
        e);
    }

    if(baseDestHandler==null || _destinationManager==null)
    {
      //MP has not been initialized so we must put the stream into doubt
      _inDoubt=true;
    }
    else
    {
      initializeNonPersistent(baseDestHandler);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "restore");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.msgstore.AbstractItem#xmlWriteOn(com.ibm.ws.sib.msgstore.FormattedWriter)
   */
  public void xmlWriteOn(FormattedWriter writer) throws IOException
  {
    super.xmlWriteOn(writer);
    
    writer.newLine();
    writer.taggedValue("subscriberID", _subState.getSubscriberID());

    writer.newLine();
    writer.startTag("topics");
    writer.indent();

    String[] topics = _subState.getTopics();

    if (topics != null)
    {
      for (int i = 0; i < topics.length; i++)
      {
        writer.newLine();
        writer.taggedValue("topic", topics[i]);
      }
    }

    writer.outdent();
    writer.newLine();
    writer.endTag("topics");
    writer.newLine();
    writer.taggedValue("toBeDeleted", toBeDeleted);
  }

  /**
   * Returns the subState.
   * @return Object
   */
  public ConsumerDispatcherState getConsumerDispatcherState()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getConsumerDispatcherState");
      SibTr.exit(tc, "getConsumerDispatcherState", _subState);
    }

    return _subState;
  }

  /**
   * @see com.ibm.ws.sib.msgstore.AbstractItem#eventPostCommitAdd(Transaction)
   */
  public void eventPostCommitAdd(Transaction transaction) throws SevereMessageStoreException
  {
    super.eventPostCommitAdd(transaction);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "eventPostCommitAdd", transaction);

    HashMap durableSubsTable =
      _destinationManager.getDurableSubscriptionsTable();

    ConsumerDispatcher consumerDispatcher =
      (ConsumerDispatcher)durableSubsTable.get(_subState.getSubscriberID());

    synchronized (durableSubsTable)
    {
      _subState.setReady(true);
    }

    try
    {
      // Increment the number of known subscribers on the parent itemstream
      ((PubSubMessageItemStream)getItemStream()).incrementReferenceStreamCount();
    }
    catch (MessageStoreException e)
    {
      // FFDC
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.store.itemstreams.DurableSubscriptionItemStream.eventPostCommitAdd",
          "1:544:1.95",
          this);  
                  
      SibTr.exception(tc, e); 
      
      // If we cannot access the datastore here then the count of items on the subscription will be incorrect
      // by ~1      
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "eventPostCommitAdd");
  }

  /**
   * @see com.ibm.ws.sib.msgstore.AbstractItem#eventPostRollbackAdd(Transaction)
   */
  public void eventPostRollbackAdd(Transaction transaction) throws SevereMessageStoreException
  {
    super.eventPostRollbackAdd(transaction);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "eventPostRollbackAdd", transaction);

    // This case will need to call the proxy code.
    deleteDurableSubscription(true);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "eventPostRollbackAdd");
  }

  /**
   * Deletes the durable subscription from the list of durable subscriptions
   * and calls through to the consumer dispatcher to remove the subscription
   * from the MatchSpace.
   *
   * @param callProxyCode  If the delete needs to call the proxy handling code.
   */
  private void deleteDurableSubscription(
    boolean callProxyCode)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "deleteDurableSubscription",
        new Object[] {
          new Boolean(callProxyCode) });

    HashMap durableSubsTable =
      _destinationManager.getDurableSubscriptionsTable();

    synchronized (durableSubsTable)
    {
      // Get the consumerDispatcher from the durable subscriptions table
      ConsumerDispatcher consumerDispatcher =
        (ConsumerDispatcher) durableSubsTable.get(
          _subState.getSubscriberID());

      //If entire topicspace has been deleted, the subscription will have already
      //been removed.
      if (consumerDispatcher != null)
      {
        // Check if the subscription from the durable subscriptions table is this
        // subscription.  Its possible that the topicspace this subscription was on
        // was deleted and a new subscription with the same id has been added into
        // the table
        if (consumerDispatcher.dispatcherStateEquals(_subState))
        {
          // Remove consumerDispatcher from durable subscriptions table
          durableSubsTable.remove(_subState.getSubscriberID());

          // Delete consumerDispatcher ( implicit remove from matchspace)
          try
          {
            // Don't need to send the proxy delete event message
            // just need to reset the memory to the stat
            consumerDispatcher.deleteConsumerDispatcher(
              callProxyCode);
          }
          catch (SIException e)
          {
            FFDCFilter.processException(
              e,
              "com.ibm.ws.sib.processor.impl.store.itemstreams.DurableSubscriptionItemStream.deleteDurableSubscription",
              "1:626:1.95",
              this);

            SibTr.exception(tc, e);

            // Could not delete consumer dispatcher
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
              SibTr.exit(tc, "deleteDurableSubscription", e);
          }
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "deleteDurableSubscription");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPReferenceStream#hasInDoubtItems
   * defect 257231
   */
  public boolean hasInDoubtItems()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "hasInDoubtItems");
      SibTr.exit(tc, "hasInDoubtItems", new Boolean(_inDoubt));	
    }
    return _inDoubt;
  }

  /** 
   * PK83993 Allow the toBeDeleted flag to be cleared, if we fail to persist the deletion
   * of the item stream. This is necessary for durable subscriptions, as otherwise we
   * would need to leave the logically deleted stream available to applications (as we
   * can't remove our reference to it, or the application could create a duplicate one).
   */
  public void clearToBeDeleted()
  {
    SibTr.entry(tc, "clearToBeDeleted");
    toBeDeleted = false;
    SibTr.exit(tc, "clearToBeDeleted");
  }
  
}
