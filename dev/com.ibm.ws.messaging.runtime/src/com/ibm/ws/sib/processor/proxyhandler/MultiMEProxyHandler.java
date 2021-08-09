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
package com.ibm.ws.sib.processor.proxyhandler;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.exception.WsException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.ConsumerDispatcherState;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.store.SIMPTransactionManager;
import com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPItemStream;
import com.ibm.ws.sib.processor.utils.LockManager;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.ws.util.ObjectPool;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;
import com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException;

/**
 * Root class for understanding if subscription/publication messages are 
 * to be sent from ME to ME.
 * 
 * Neighbours are created and deleted from this class.
 * Subscription events are driven from this class.
 */
public final class MultiMEProxyHandler extends SIMPItemStream
{
  private static final int NUM_MESSAGES = 2;
  
  private static final TraceComponent tc =
    SibTr.register(
      MultiMEProxyHandler.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  /** private copy of the Neighbours instance */
  private Neighbours _neighbours = null;

  /** Decide if we actually need to propagate all the publications
   * and ignore any subscription propagation. 
   * Indicates whether all publications should be sent from ME->ME */
  //private static final boolean propagateAllPublications = false;

  /** Cached reference to the MessageProcessor */
  private MessageProcessor _messageProcessor;

  /** Reference to an object pool, used for storing the SubscriptionMessageHandler 
   * objects
   */
  private ObjectPool _subscriptionMessagePool;

  /** The NeighbourProxyListener object that reads messages from the SYSTEM
   * queue to update its proxy list
   */
  private NeighbourProxyListener _proxyListener;

  /** A locking manager to make sure that subscribe/unsubscribe Events
   * are allowed to occur simultaneously and that Reset messages will
   * not occur at the same time as that
   */
  private LockManager _lockManager;

  /** The consumer session to register the consumer against */
  private ConsumerSession _proxyAsyncConsumer;
  
  /** The transaction manager instance */
  private SIMPTransactionManager _transactionManager;
  
  /** flag to indicate that we are in the middle of reconcilliation, 
   * so not to continue with other events
   */
  private boolean _reconciling = false;
  
  /** Indicator for if the messaging engine is started */
  private boolean _started = false;
  
  /**
   * This is the constructor called by the message store 
   * for recreation of the proxy handler.
   */
  public MultiMEProxyHandler()
  {
    super();
  }
 
  /** 
   * Constructor for the one and only MultiMEProxyHandler instance 
   * 
   * @param messageProcessor  The MessageProcessor object for this ME
   * @param txManager  The transaction manager instance to create Local transactions
   * under.
   */
  public MultiMEProxyHandler(MessageProcessor messageProcessor,
                             SIMPTransactionManager txManager)
  {
    super();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "MultiMEProxyHandler", 
        new Object[]{messageProcessor, txManager});
    
    initialiseNonPersistent(messageProcessor, txManager);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "MultiMEProxyHandler", this);
  }
  
  /** 
   * Called to recover Neighbours from the MessageStore.
   * 
   * @param messageProcessor  The message processor instance
   * @param txManager  The transaction manager instance to create Local transactions
   * under.
   */
  public void initialiseNonPersistent(MessageProcessor messageProcessor,
                                      SIMPTransactionManager txManager)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "initialiseNonPersistent", messageProcessor);

    //Release 1 has no Link Bundle IDs for this Neighbour

    // Cache the reference to the message processor
    _messageProcessor = messageProcessor;

    // Create the ObjectPool that will be used to store the 
    // subscriptionMessages.  Creating with an intial length of
    // 2, but this could be made a settable parameter for performance
    _subscriptionMessagePool = new ObjectPool("SubscriptionMessages", NUM_MESSAGES);

    // Create a new object to contain all the Neighbours
    _neighbours = new Neighbours(this, _messageProcessor.getMessagingEngineBus());

    // Create a new LockManager instance
    _lockManager = new LockManager();
    
    // Assign the transaction manager
    _transactionManager = txManager;
            
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "initialiseNonPersistent");
  }
  
  /**
   * When initialised is called, each of the neighbours are sent the 
   * set of subscriptions.
   * 
   * The flag that indicates that reconciling is complete is also set
   *
   */
  public void initalised() throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "initalised");
    
    try
    {    
      _lockManager.lockExclusive();

      // Flag that we are in a started state. 
      _started = true;
      
      // If the Neighbour Listener hasn't been created, create it
      if (_proxyListener == null)
        createProxyListener();
     
      // Indicate that reconciling is complete
      _reconciling = false;
      
      _neighbours.resetBusSubscriptionList();
    }      
    finally
    {
      _lockManager.unlockExclusive();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "initalised"); 
  }
  
  /**
   * Stops the proxy handler from processing any more messages
   */
  public void stop()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "stop");
    
    try
    {    
      _lockManager.lockExclusive();

      // Flag that we are in a started state. 
      _started = false;
    }      
    finally
    {
      _lockManager.unlockExclusive();
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "stop");    
  }
  
  /** 
   * removeUnusedNeighbours is called once all the Neighbours are defined from Admin.
   * 
   * This will generate the reset state method to be sent to all Neighbouring
   * ME's
   * 
   * Also, this will check all Neighbours and ensure that they are all still 
   * valid and remove those that aren't
   * 
   */
  public void removeUnusedNeighbours()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "removeUnusedNeighbours");

    final Enumeration neighbourList = _neighbours.getAllRecoveredNeighbours();
    
    LocalTransaction transaction = null;
    
    try
    {
      _lockManager.lockExclusive();
      
      transaction = _transactionManager.createLocalTransaction(true);
        
      // Enumerate through the Neighbours.
      while (neighbourList.hasMoreElements())
      {
        final Neighbour neighbour = (Neighbour) neighbourList.nextElement();
    
        // If the neigbour is a foreign neighbour and still has a link
        // definition associated with it then we leave it in the recovered
        // state. The link start operation will make the neighbour available.
        // Link deletion will remove the neighbour
       if (_messageProcessor.
              getDestinationManager().
              getLinkDefinition(neighbour.getBusId()) == null)
        {   
          // If the Neighbour was created at startup from the message 
          // store, then this Neighbour can be deleted as it hasn't 
          // been reinstated from Admin.
          // Delete this Neighbour with the forced option 
          // as we don't know the state of this Neighbour.
          _neighbours.removeRecoveredNeighbour(neighbour.getUUID(),
                                               (Transaction) transaction);
        }
      }
   
      transaction.commit();
      
    }
    catch (SIException e)
    {
      FFDCFilter.processException(
       e,
       "com.ibm.ws.sib.processor.proxyhandler.MultiMEProxyHandler.removeUnusedNeighbours",
       "1:310:1.96",
       this);
       
      try
      {
        if (transaction!=null)
          transaction.rollback();
      }
      catch (SIException e1)
      {
        // FFDC
        FFDCFilter.processException(
          e1,
          "com.ibm.ws.sib.processor.proxyhandler.MultiMEProxyHandler.removeUnusedNeighbours",
          "1:324:1.96",
          this);
          
        SibTr.exception(tc, e1);      
      }
            
      SibTr.exception(tc, e);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "removeUnusedNeighbours", "SIErrorException"); 
      throw new SIErrorException(e);
    }
    finally 
    {
      _lockManager.unlockExclusive();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeUnusedNeighbours");
  }

  /**
   * Forwards a subscription to all Neighbouring ME's
   *
   * To drive this subscribeEvent method, the subscription would
   * have to have been registered on this ME which means that we
   * can forward this subscription onto the Neighbours if required
   *
   * @param subState          The subscription definition
   * @param transaction       The transaction to send the proxy update 
   *                          message under 
   * @exception SIResourceException 
   */
  public void subscribeEvent(
    ConsumerDispatcherState subState,
    Transaction transaction)
    throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "subscribeEvent",
        new Object[] { subState, transaction });

    try
    {
      // Get the lock Manager lock 
      // multiple subscribes can happen at the same time - 
      // this is allowed.
      _lockManager.lock();
      
      if (!_started)
      {      
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "subscribeEvent", "Returning as stopped");
        return;
      }

      // Get the list of Buses that this subscription needs to be forwarded
      // onto
      final BusGroup[] buses = _neighbours.getAllBuses();

      // Declaration of a message handler that can be used for 
      // building up the proxy subscription message to be forwarded.
      SubscriptionMessageHandler messageHandler = null;

      // Loop through each of the Buses deciding if this
      // subscription event needs to be forwarded
      if (!_reconciling)      
        for (int i = 0; i < buses.length; ++i)
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(
              tc,
              "Forwarding topic " + subState.getTopic() + " to Bus " + buses[i]);
  
          // Send the proxy always
          messageHandler =
            buses[i].addLocalSubscription(
              subState,
              messageHandler,
              transaction,
              true);
          
          // If the subscription message isn't null, then add it back into 
          // the pool of subscription messages.
          if (messageHandler != null)
          {
            addMessageHandler(messageHandler);          
            messageHandler = null;
          }
        }
    }
    finally
    {
      // Release the lock that was obtained
      _lockManager.unlock();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "subscribeEvent");
  }

  /** 
   * Forwards the subscribe Events onto the Neighbouring ME's
   * 
   * This will be called for Subscriptions that have arrived from Neighbouring
   * ME's and need to be forwarded onto other ME's outside of the Bus that the 
   * message arrived from.
   * 
   * This method is called from the NeighbourProxyListener class and is called 
   * when a proxy subscription request is received.
   * 
   * @param topics  The list of Topics to send to the Neighbours
   * @param topicSpaces  The list of topicSpaces to be forwarded
   * @param BusId  The Bus that this message originated from.
   * 
   * @exception  SIResourceException  Thrown if there is an error sending 
   * a message to a Neighbour
   */
  protected void subscribeEvent(
    List topicSpaces,
    List topics,
    String busId,
    Transaction transaction) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "subscribeEvent",
        new Object[] { topics, topicSpaces, busId, transaction });

    try
    {
      // Get the lock Manager lock 
      // multiple subscribes can happen at the same time - 
      // this is allowed.
      _lockManager.lock();

      if (!_started)
      {      
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "subscribeEvent", "Returning as stopped");
        return;
      }
      
      if (!_reconciling)      
        // Call to subscribe.
        remoteSubscribeEvent(topicSpaces,
                             topics,
                             busId,
                             transaction,
                             true);
    }
    finally
    {
      _lockManager.unlock();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "subscribeEvent");
  }
  
  /** 
   * Forwards the subscribe Events onto the Neighbouring ME's
   * 
   * This will be called for Subscriptions that have arrived from Neighbouring
   * ME's and need to be forwarded onto other ME's outside of the bus that the 
   * message arrived from.
   * 
   * This method is called from the NeighbourProxyListener class and is called 
   * when a proxy subscription request is received.
   * 
   * @param topics  The list of Topics to send to the Neighbours
   * @param topicSpaces  The list of topicSpaces to be forwarded
   * @param busId  The bus that this message originated from.
   * @param sendProxy If the proxy message should be sent.
   * 
   * @exception  SIResourceException  Thrown if there is an error sending 
   * a message to a Neighbour
   */
  protected void remoteSubscribeEvent(
    List topicSpaces,
    List topics,
    String busId, 
    Transaction transaction,
    boolean sendProxy) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "remoteSubscribeEvent", new Object[] { topicSpaces, topics, busId, transaction});

    // Get the list of buses that this subscription needs to be forwarded
    // onto
    final BusGroup[] buses = _neighbours.getAllBuses();

    // Loop through each of the buses deciding if this
    // subscription event needs to be forwarded
    for (int i = 0; i < buses.length; ++i)
    {
      if (!buses[i].getName().equals(busId))
      {
        // Declaration of a message handler that can be used for 
        // building up the proxy subscription message to be forwarded.
        SubscriptionMessageHandler messageHandler = null;

        // Get the iterators for this message       
        final Iterator topicIterator = topics.listIterator();
        final Iterator tsIterator = topicSpaces.listIterator();

        // Iterate through each of the topics to decide if this needs
        // to be forwarded.
        while (topicIterator.hasNext())
        {
          final SIBUuid12 topicSpace = (SIBUuid12) tsIterator.next();
          final String topic = (String) topicIterator.next();

          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(
              tc,
              "Forwarding topic " + topicSpace + topic + " to bus " + buses[i]);

          // Add this subscription to this buse.
          // This method call passes through the fact that this method can
          // only be called by a proxy subscription that was registered on this
          // ME
          messageHandler = 
            buses[i].addRemoteSubscription(topicSpace, topic, messageHandler, sendProxy);
        }

        // If the subscription message isn't null, then add it back into 
        // the list of subscription messages.
        if (messageHandler != null)
        {
          // Send to all the Neighbours in this buse
          buses[i].sendToNeighbours(
            messageHandler.getSubscriptionMessage(),
            transaction,
            false);

          addMessageHandler(messageHandler);

          messageHandler = null;
        }
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "remoteSubscribeEvent");
  }

  /**
   * Forwards an unsubscribe to the Neighbouring ME's.
   * <p>
   * To drive this unsubscribeEvent method, the subscription would
   * have to have been deregistered on this ME which means that we
   * can forward this subscription deletion onto the Neighbours if required
   *
   * @param subState     The subscription definition
   * @param transaction  The transaction to send the delete proxy message under.
   * @param sendProxy    If the proxy delete message is to be sent     
   *                         
   * @exception  SIResourceException  Thrown when an attempt is
   *                                  made to remove a proxy subscription that
   *                                  that doesn't exist
   *
   */
  public void unsubscribeEvent(
    ConsumerDispatcherState subState,
    Transaction transaction,
    boolean sendProxy)
    throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "unsubscribeEvent",
        new Object[] { subState, transaction, new Boolean(sendProxy)});

    try
    {
      // Get the lock Manager lock 
      // multiple unsubscribes can happen at the same time - 
      // this is allowed.
      _lockManager.lock();

      if (!_started)
      {      
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "unsubscribeEvent", "Returning as stopped");
        return;
      }

      // Get the list of buses that this subscription needs to be forwarded
      // onto
      final BusGroup[] buses = _neighbours.getAllBuses();

      // Declaration of a message handler that can be used for 
      // building up the proxy subscription message to be forwarded.
      // null referenced so it will be created only if needed.
      SubscriptionMessageHandler messageHandler = null;

      // Loop through each of the Buses deciding if this
      // subscription event needs to be forwarded
      if (!_reconciling)      
        for (int i = 0; i < buses.length; ++i)
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(
              tc,
              "Forwarding topic "
                + subState.getTopicSpaceUuid()
                + ":"
                + subState.getTopic()
                + " to Bus " + buses[i]);
  
          messageHandler =
            buses[i].removeLocalSubscription(
              subState,
              messageHandler,
              transaction,
              sendProxy);
        }

      // If the subscription message isn't null, then add it back into 
      // the list of subscription messages.
      if (messageHandler != null)
      {
        addMessageHandler(messageHandler);
      }

    }
    finally
    {
      // Unlock the lock that was aquired.
      _lockManager.unlock();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "unsubscribeEvent");
  }

  /**
   * Forwards an unsubscribe to the Neighbouring ME's.
   * <p>
   * To drive this unsubscribeEvent method, the subscription would
   * have to have been deregistered on this ME which means that we
   * can forward this subscription deletion onto the Neighbours if required
   * 
   * This method is called from the NeighbourProxyListener instance and is
   * only called when a subscription is deregistered by a Neighbour
   *
   * @param topics  The list of Topics to send to the Neighbours
   * @param topicSpaces  The list of topicSpaces to be forwarded
   * @param busId  The bus that this message originated from.
   * 
   * @exception SIResourceException
   */
  public void unsubscribeEvent(
    List topicSpaces,
    List topics,
    String busId,
    Transaction transaction)
    throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "unsubscribeEvent",
        new Object[] { topicSpaces, topics, busId });

    try
    {
      // Get the lock Manager lock 
      // multiple unsubscribes can happen at the same time - 
      // this is allowed.
      _lockManager.lock();
      
      if (!_started)
      {      
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "unsubscribeEvent", "Returning as stopped");
        return;
      }
      
      if (!_reconciling)      
        // Call the unsubscribe event code
        remoteUnsubscribeEvent(topicSpaces,
                               topics, 
                               busId, 
                               transaction,
                               true);
      
    }
    finally
    {
      // Unlock the lock that was aquired.
      _lockManager.unlock();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "unsubscribeEvent");
  }
  
  /**
   * Forwards an unsubscribe to the Neighbouring ME's.
   * <p>
   * To drive this unsubscribeEvent method, the subscription would
   * have to have been deregistered on this ME which means that we
   * can forward this subscription deletion onto the Neighbours if required
   * 
   * This method is called from the NeighbourProxyListener instance and is
   * only called when a subscription is deregistered by a Neighbour
   *
   * @param topics  The list of Topics to send to the Neighbours
   * @param topicSpaces  The list of topicSpaces to be forwarded
   * @param busId  The bus that this message originated from.
   * @param sendProxy  If the proxy message should be sent.
   */
  protected void remoteUnsubscribeEvent(List topicSpaces,
                                        List topics,                                        
                                        String busId,
                                        Transaction transaction,
                                        boolean sendProxy) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "remoteUnsubscribeEvent", new Object[] { topicSpaces, topics, busId, transaction});

    // Get the list of buses that this subscription needs to be forwarded
    // onto
    final BusGroup[] buses = _neighbours.getAllBuses();

    // Loop through each of the Buses deciding if this
    // subscription event needs to be forwarded
    for (int i = 0; i < buses.length; ++i)
    {
      if (!buses[i].getName().equals(busId))
      {
        // Declaration of a message handler that can be used for 
        // building up the proxy subscription message to be forwarded.
        // null referenced so it will be created only if needed.
        SubscriptionMessageHandler messageHandler = null;

        // Get the iterators for this message       
        final Iterator topicIterator = topics.listIterator();
        final Iterator tsIterator = topicSpaces.listIterator();

        // Iterate through each of the topics to decide if this needs
        // to be forwarded.
        while (topicIterator.hasNext())
        {
          final SIBUuid12 topicSpace = (SIBUuid12) tsIterator.next();
          final String topic = (String) topicIterator.next();

          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(
              tc,
              "Forwarding topic "
                + topicSpace
                + ":"
                + topic
                + " to Bus " + buses[i]);

          // The false method is set for the local flag as this method is only
          // called by the remote ME case when it has forwarded on a Proxy deregister.
          messageHandler =
            buses[i].removeRemoteSubscription(
              topicSpace,
              topic,
              messageHandler,
              sendProxy);

          // If the subscription message isn't null, then add it back into 
          // the list of subscription messages.
          if (messageHandler != null)
          {
            // Send to all the Neighbours in this Bus
            buses[i].sendToNeighbours(
              messageHandler.getSubscriptionMessage(),
              transaction,
              false);

            addMessageHandler(messageHandler);

            // Set the message handler back to null to ensure that the
            // message is reset next time.
            messageHandler = null;
          }
        }
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "remoteUnsubscribeEvent");
  }

  /** 
   * When a topic space is created, there may be proxy subscriptions
   * already registered that need to attach to this topic space.
   *
   * @param destination  The destination object being created
   * @exception SIDiscriminatorSyntaxException 
   * @exception SISelectorSyntaxException
   * @exception SIResourceException
   */
  public void topicSpaceCreatedEvent(DestinationHandler destination) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "topicSpaceCreatedEvent", destination);
    
    try
    {
      _lockManager.lockExclusive();
      
      _neighbours.topicSpaceCreated(destination);
    }
    finally
    {
      _lockManager.unlockExclusive();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "topicSpaceCreatedEvent");
  }

  /**
   * When a topic space is deleted, there may be proxy subscriptions
   * that need removing from the match space and putting into "limbo"
   * until the delete proxy subscriptions request is made
   * 
   * @param destination  The destination topic space that has been deleted
   * 
   * @exception SIResourceException
   */
  public void topicSpaceDeletedEvent(DestinationHandler destination)
    throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "topicSpaceDeletedEvent", destination);

    try
    {
      _lockManager.lockExclusive();
    
      _neighbours.topicSpaceDeleted(destination);
    }
    finally
    {
      _lockManager.unlockExclusive();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "topicSpaceDeletedEvent");
  }
  
  /**
   * When a Link is started we want to send a reset message to the neighbouring
   * bus. 
   * 
   * If the neighbour was not found, then create it here as we don't want to start
   * sending messages to it until the link has started.
   * 
   * @param String The name of the foreign bus
   * @param SIBUuid8 The uuid of the link localising ME on the foreign bus 
   */
  public void linkStarted(String busId, SIBUuid8 meUuid) 
  
  throws SIIncorrectCallException, SIErrorException, SIDiscriminatorSyntaxException, 
         SISelectorSyntaxException, SIResourceException 
          
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "linkStarted", new Object[] {busId, meUuid});
         
    // Look for existing neighbour for the link
    Neighbour neighbour = getNeighbour(meUuid);
    
    if (neighbour == null)
    {
      // If the neighbour doesn't exist then create it now.
      LocalTransaction tran = _transactionManager.createLocalTransaction(true);
      
      neighbour = createNeighbour(meUuid, busId, (Transaction) tran);
      
      tran.commit();
    }
      
    // Reset the list of subscriptions
    _neighbours.resetBusSubscriptionList();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "linkStarted");
  }
  
  /**
   * When a link is deleted we need to clean up any neighbours that won`t
   * be deleted at restart time.
   * 
   * @param String The name of the foreign bus
   */
  public void cleanupLinkNeighbour(String busName) 
  throws SIRollbackException, 
         SIConnectionLostException, 
         SIIncorrectCallException, 
         SIResourceException, 
         SIErrorException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "cleanupLinkNeighbour");
      
    Neighbour neighbour = _neighbours.getBusNeighbour(busName);
    
    if (neighbour != null)
    {
      LocalTransaction tran = 
        _messageProcessor.getTXManager().createLocalTransaction(true);
      
      deleteNeighbourForced(neighbour.getUUID(), busName, (Transaction) tran);
      
      tran.commit();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "cleanupLinkNeighbour");
        
  }

  /** 
   * Adds a new Neighbouring ME. 
   * 
   * <p>
   * When creating a new ME, a durable subscription needs to be
   * created on the Neighbour followed by an exchange of all current subscriptions
   * This only needs to occur if this really is a new Neighbour, and not one that has
   * been recreated after restart.
   * 
   * @param meUUID         The ME UUID
   * @param busId       The bus that this ME belongs to.
   * @param transaction    The transaction for the createNeighbour 
   * 
   * @exception SIDestinationAlreadyExistsException  Thrown when the Neighbour already
   * exists and an attempt is made to create is again
   * @exception SICoreException  Thrown when creating the Proxy Listener instance
   * 
   */
  public Neighbour createNeighbour(
    SIBUuid8    meUUID,
    String      busId,
    Transaction transaction) throws SIDiscriminatorSyntaxException, SISelectorSyntaxException, SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "createNeighbour",
        new Object[] { meUUID, busId, transaction });
   
   Neighbour neighbour = null;
   try
   {
     _lockManager.lockExclusive();
     
     // Add the new Neighbour to the list of neighbours.
     neighbour = _neighbours.addNeighbour(
       meUUID,
       busId,
       transaction);
       
     // If we aren't reconciling, create the listener for the proxy destination
     if (!_reconciling && _proxyListener == null)
       createProxyListener();       
   }
   finally
   {
     _lockManager.unlockExclusive();
   }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createNeighbour", neighbour);
    return neighbour;
  }

  /**
   * Removes a Neighbour by taking a brutal approach to remove all the 
   * proxy Subscriptions on this ME pointing at other Neighbours.  
   * 
   * This will not leave the Neighbour marked as deleted and wait for the
   * delete message, it will simply zap everything in site.
   * 
   * This will forward on any deregistration Events to other Neighbours.
   * 
   * @param neighbourUUID The UUID for the Neighbour
   * @param busId  The bus that this ME belongs to. 
   * @param transaction  The transaction for deleting the Neighbour
   * 
   * @exception SIDestinationNotFoundException  Thrown if the Neighbour was not found
   * @exception SIResourceException
   */
  public void deleteNeighbourForced(
    SIBUuid8 meUUID,
    String busId,
    Transaction transaction)
    throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "deleteNeighbourForced",
        new Object[] { meUUID, busId, transaction } );
    
    try
    {
      _lockManager.lockExclusive();
    
      // Force remove the Neighbour
      _neighbours.removeNeighbour(meUUID, busId, transaction);
    }
    finally
    {
      _lockManager.unlockExclusive();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "deleteNeighbourForced");
  }
  
  /**
   * This is purely for unittests. Many unittests dont cleanup their neighbours
   * so we now need this to ensure certain tests are clean before starting.
   * 
   * @param neighbourUUID The UUID for the Neighbour
   * @param busId  The bus that this ME belongs to. 
   * @param transaction  The transaction for deleting the Neighbour
   * 
   * @exception SIDestinationNotFoundException  Thrown if the Neighbour was not found
   * @exception SIResourceException
   */
  public void deleteAllNeighboursForced(
    Transaction transaction)
    throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "deleteAllNeighboursForced",
        new Object[] { transaction } );
    
    try
    {
      _lockManager.lockExclusive();
      
      Enumeration neighbs = _neighbours.getAllNeighbours();
      while (neighbs.hasMoreElements())
      {
        Neighbour neighbour = (Neighbour)neighbs.nextElement();
        // Force remove the Neighbour
        _neighbours.removeNeighbour(neighbour.getUUID(), neighbour.getBusId(), transaction);
      }
      
      neighbs = _neighbours.getAllRecoveredNeighbours();
      while (neighbs.hasMoreElements())
      {
        Neighbour neighbour = (Neighbour)neighbs.nextElement();
        // Force remove the Neighbour
        _neighbours.removeNeighbour(neighbour.getUUID(), neighbour.getBusId(), transaction);
      }
    

    }
    finally
    {
      _lockManager.unlockExclusive();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "deleteAllNeighboursForced");
  }

  /**
   * Recovers the Neighbours from the MessageStore.
   * 
   * @exception WsException Thrown if there was a problem
   * recovering the Neighbours.
   *
   */
  public void recoverNeighbours() throws SIResourceException, MessageStoreException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "recoverNeighbours");
    
    try
    {
      // Lock the manager exclusively
      _lockManager.lockExclusive();
      
      // Indicate that we are now reconciling
      _reconciling = true;
      
      _neighbours.recoverNeighbours();
    }
    finally
    {
      _lockManager.unlockExclusive();
    }
   
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "recoverNeighbours");
  }

  /** 
   * Method reports all currently known ME's in a Enumeration format
   * 
   * @return  An Enumeration of all the Neighbours.
   */
  Enumeration reportAllNeighbours()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "reportAllNeighbours");

    // Call out to the Neighbours class to get the list of Neighbours.
    final Enumeration neighbours = _neighbours.getAllNeighbours();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "reportAllNeighbours");

    return neighbours;
  }
  
  /** Method that reports all the BusGroups that are available
   * 
   * @return BusGroup array.
   */
  protected BusGroup[] getAllBusGroups()
  {
    return _neighbours.getAllBuses();
  }

  /** Method gets the ObjectPool for the subscriptin messages
   * 
   * @return ObjectPool  The object pool containing the subscription 
   * messages
   */
  final ObjectPool getMessageObjectPool()
  {
    return _subscriptionMessagePool;
  }

  /** 
   * Adds a message back into the Pool of available messages.
   * 
   * @param messageHandler The message handler to add back to the pool
   */
  void addMessageHandler(SubscriptionMessageHandler messageHandler)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "addMessageHandler", messageHandler);

    final boolean inserted = _subscriptionMessagePool.add(messageHandler);
    // If the message wasn't inserted, then the pool was exceeded
    if (!inserted)
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(
          tc,
         "SubscriptionObjectPool size exceeded");

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addMessageHandler");
  }

  /** 
   * Returns the Message Handler to be used for this operation
   * 
   * It will check the message pool and if none are available,
   * it will create a new instance
   * 
   * @return SubscriptionMessageHandler  The message handling class.
   */
  SubscriptionMessageHandler getMessageHandler()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getMessageHandler");

    SubscriptionMessageHandler messageHandler =
      (SubscriptionMessageHandler) _subscriptionMessagePool.remove();

    if (messageHandler == null)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(
          tc,
          "doProxySubscribeOp",
          "Creating a new Message Handler as none available");

      messageHandler = new SubscriptionMessageHandler(this);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getMessageHandler", messageHandler);

    return messageHandler;
  }

  /**
   * Method that creates the NeighbourProxyListener instance for reading messages 
   * from the Neighbours.  It then registers the listener to start receiving
   * messages
   * 
   * @throws SIResourceException  Thrown if there are errors while
   * creating the 
   */
  private void createProxyListener() throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createProxyListener");

    // Create the proxy listener instance
    _proxyListener = new NeighbourProxyListener(_neighbours, this);

    /*
     * Now we can create our asynchronous consumer to listen on      
     * SYSTEM.MENAME.PROXY.QUEUE  Queue for receiving subscription
     * updates   
     */
    // 169897.1 modified parameters
    try
    {
      _proxyAsyncConsumer =
        _messageProcessor
          .getSystemConnection()
          .createSystemConsumerSession(
            _messageProcessor.getProxyHandlerDestAddr(), // destination name
            null, //Destination filter
            null, // SelectionCriteria - discriminator and selector
            Reliability.ASSURED_PERSISTENT, // reliability
            false, // enable read ahead
            false,
            null,
            false);

      // 169897.1 modified parameters

      _proxyAsyncConsumer.registerAsynchConsumerCallback(
        _proxyListener,
        0, 0, 1,
        null);

      _proxyAsyncConsumer.start(false);
    }
    catch (SIException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.proxyhandler.MultiMEProxyHandler.createProxyListener",
        "1:1271:1.96",
        this);
        
      SibTr.exception(tc, e);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "createProxyListener", "SIResourceException");
        
      // The Exceptions should already be NLS'd
      throw new SIResourceException(e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createProxyListener");
  }
  
  /**
   * Returns the proxy ASynch consumer for this ME
   * 
   * @return NeighbourProxyListener instance
   */
  public NeighbourProxyListener getProxyListener()
  {
    return _proxyListener;
  }

  /** Method that returns the Message processor instance for this Proxy
   * Handler instance
   * 
   * @return MessageProcessor  The message processor instance
   */
  final MessageProcessor getMessageProcessor()
  {
    return _messageProcessor;
  }
  
  /**
   * Returns the neighbour for the given UUID
   * @param neighbourUUID The uuid to find
   * @return The neighbour object
   */
  public final Neighbour getNeighbour(SIBUuid8 neighbourUUID)
  {
    return _neighbours.getNeighbour(neighbourUUID);
  }

  /**
   * Returns the neighbour for the given UUID
   * @param neighbourUUID The uuid to find
   * @param includeRecovered Also look for the neighbour in the recovered list
   * @return The neighbour object
   */
  public final Neighbour getNeighbour(SIBUuid8 neighbourUUID, boolean includeRecovered)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getNeighbour", new Object[] { neighbourUUID, Boolean.valueOf(includeRecovered)});
    
    Neighbour neighbour = _neighbours.getNeighbour(neighbourUUID); 
    
    if((neighbour == null) && includeRecovered)
      neighbour = _neighbours.getRecoveredNeighbour(neighbourUUID);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getNeighbour", neighbour);
    
    return neighbour;
  }
  
  /**
   * Returns the Neighbours instance.
   * @return the set of Neighbours
   */
  final Neighbours getNeighbours()
  {
    return _neighbours;
  }
  
  /**
   * Returns the Transaction manager instance
   * 
   * @return the Transaction manager instance
   */
  final SIMPTransactionManager getTransactionManager()
  {
    return _transactionManager;
  }

  public String toString()
  {
    //  instance iMultiBroker is 'this'                               
    return "Instance " + super.toString(); /* 170808 */
  }
  
  protected LockManager getLockManager()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {    
      SibTr.entry(tc, "getLockManager");
      SibTr.exit(tc, "getLockManager",_lockManager);
    }
    return _lockManager;
  }


}
