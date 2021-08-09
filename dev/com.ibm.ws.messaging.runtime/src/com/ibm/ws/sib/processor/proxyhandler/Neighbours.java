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

// Import required classes.
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.ForeignBusDefinition;
import com.ibm.ws.sib.admin.SIBExceptionNoLinkExists;
import com.ibm.ws.sib.admin.VirtualLinkDefinition;
import com.ibm.ws.sib.mfp.control.SubscriptionMessage;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.ConsumerDispatcher;
import com.ibm.ws.sib.processor.impl.ControllableProxySubscription;
import com.ibm.ws.sib.processor.impl.DestinationManager;
import com.ibm.ws.sib.processor.impl.PubSubOutputHandler;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.store.filters.ClassEqualsFilter;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException;

/**This class represents the set of all known neighbours of the broker.
 *
 * This class knows the complete Topology for this Neighbour.
 *
 */
final class Neighbours 
{
  // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  private static final TraceComponent tc =
    SibTr.register(
      Neighbours.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

 
  /**This table maps neighbour identifiers to Neighbour objects.
   * It is used as the main table to locate and store Neighbours.
   * 
   * This is keyed on the MEUUID.
   */
  private Hashtable _neighbours;
  
  /** This table is the set of Neighbours that are recovered from 
   * the message store.
   */
  private Hashtable _recoveredNeighbours;

  /** The list of topic spaces that this ME has registered 
   * and the topicSpaces ->TemporarySubscriptions are stored 
   * as a HashMap of elements keyed of the topic space name to 
   * return a TemporarySubscription
   */
  private HashMap _topicSpaces;

  /** This array holds all BusGroup objects. */
  private BusGroup[] _buses;

  /** The reference to the the Proxy Handler class */
  private MultiMEProxyHandler _proxyHandler;
  
  /** A reference to the Destination Manager instance */
  private DestinationManager _destinationManager;
  
  /**The name of this messaging engine's local bus */
  private String _localBusName;
  
  /**Constructor.
   *
   * Class that maintains the complete list of active Neighbours.
   * 
   * @param proxyHandler  The reference to the MultiMEProxyHandler 
   * @param localBusName  The name of the messaging engine's bus
   * 
   */
  Neighbours(MultiMEProxyHandler proxyHandler, String localBusName)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "Neighbours", new Object[]{proxyHandler, localBusName});

    _proxyHandler = proxyHandler;

    // Consutruct the instances for this object
    _neighbours = new Hashtable();
    _buses = new BusGroup[0];
    _topicSpaces = new HashMap();
    _recoveredNeighbours = new Hashtable();
    _localBusName = localBusName;
    
    // Get a reference to the Destination Manager
    _destinationManager = 
      _proxyHandler.getMessageProcessor().getDestinationManager();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "Neighbours", this);
  }

  /** 
   * Gets the complete list of all buses
   * 
   * @return BusGroup[]  The complete array of buses.
   */
  final BusGroup[] getAllBuses()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getAllBuses");
      SibTr.exit(tc, "getAllBuses", _buses);
    }

    return _buses;
  }

  /** 
   * Gets the complete list of all Neighbours for this ME
   * @return An enumeration of all the Neighbours
   */
  final Enumeration getAllNeighbours()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getAllNeighbours");
    }

    Enumeration allNeighbours = null;
    
    synchronized(_neighbours)
    {
      allNeighbours =((Hashtable) _neighbours.clone()).elements();
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.exit(tc, "getAllNeighbours");
    }    
    return allNeighbours;
  }
  
  /** 
   * Gets the complete list of all recoved Neighbours for this ME
   * @return An Enumeration of all the Recovered Neighbours from the MessageStore.
   */
  final Enumeration getAllRecoveredNeighbours()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
       SibTr.entry(tc, "getAllRecoveredNeighbours");  
    }
    
    Enumeration allRecoveredNeighbours = null;
    
    synchronized(_recoveredNeighbours)
    {
      allRecoveredNeighbours = _recoveredNeighbours.elements();
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
       SibTr.exit(tc, "getAllRecoveredNeighbours");
    }    
    return allRecoveredNeighbours;
  }

  /** 
   * When the Destination manager doesn't know about a Destination, the topic space
   * reference is created here until either the proxy is deleted, or the topic space
   * is created.
   * 
   * @param neighbourUuid the uuid of the remote ME
   * @param topicSpace  the name of the topic space destination 
   * @param topic  The topic registered.
   * @param warmRestarted  If the messaging engine has been warm restarted.
   */
  void addTopicSpaceReference(SIBUuid8 neighbourUuid, 
                              SIBUuid12 topicSpace, 
                              String topic, 
                              boolean warmRestarted)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "addTopicSpaceReference",
        new Object[] { neighbourUuid, topicSpace, topic, new Boolean(warmRestarted) });

    // Synchronize around the topic space references to stop removal and
    // additions occuring at the same time. 
    synchronized (_topicSpaces)
    {

      // From the list of topic spaces, get the HashMap that contains the 
      // list of topicSpaces to topics.
      TemporarySubscription sub =
        (TemporarySubscription) _topicSpaces.get(topicSpace);
      
      Neighbour neighbour = null;
      
      // If the ME has warm restarted, then the Neighbour object will actually
      // be in the recoveredNeighbours instance.
      if (warmRestarted)
      {
        synchronized(_recoveredNeighbours)
        {
          neighbour = (Neighbour)_recoveredNeighbours.get(neighbourUuid);
        }
      }
      else
        neighbour = getNeighbour(neighbourUuid);
        
      MESubscription subscription = 
        neighbour.getSubscription(topicSpace, topic);

      if (sub == null)
      {
        // Consruct a new TemporarySubscription object.
        sub = new TemporarySubscription(neighbourUuid, subscription);

        // Put the topic space reference into the list of topic
        // spaces, keyed on the topic space.
        _topicSpaces.put(topicSpace, sub);
      }
      else
      {
        // Add a topic to the list of topics on this topic space for this ME
        sub.addTopic(neighbourUuid, subscription);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addTopicSpaceReference");
  }
  
  /**
   * Returns a TemporarySubscription from a given topicSpace.
   */
  protected TemporarySubscription getTemporarySubscription(SIBUuid12 topicSpace)
  {
    return (TemporarySubscription)_topicSpaces.get(topicSpace);
  }
  
  /** 
  * Removes the topic space reference for the given ME on the given topic.
  * 
  * @param neighbourUuid the uuid of the remote ME
  * @param subscription  The subscription object representing this proxy
  * @param topicSpace  the name of the topic space destination 
  * @param topic  The topic registered.
  */
  void removeTopicSpaceReference(
    SIBUuid8 neighbourUuid,
    MESubscription subscription,
    SIBUuid12 topicSpace,
    String topic)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "removeTopicSpaceReference",
        new Object[] { neighbourUuid, subscription, topicSpace, topic });

    // Synchronize around the topic space references to stop removal and
    // additions occuring at the same time. 
    synchronized (_topicSpaces)
    {
      // From the list of topic spaces, get the HashMap that contains the 
      // list of topicSpaces to topics.
      final TemporarySubscription sub =
        (TemporarySubscription) _topicSpaces.get(topicSpace);

      if (sub == null)
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(
            tc,
            "TemporarySubscription not found");
      }
      else
      {
        // Remove a topic to the list of topics on this topic space for this ME
        if (sub.removeTopic(neighbourUuid, subscription))
          // If the topicSpace is empty of topics, then remove the topicSpace from
          // list of topic spaces.
          _topicSpaces.remove(topicSpace);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeTopicSpaceReference");
  }

  /**
   * If a topicSpace is created, check through the list of proxies
   * and register them against the real destination object
   * 
   * @param destination  The topicSpace that was created
   */
  void topicSpaceCreated(DestinationHandler destination) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "topicSpaceCreated", destination);

    // Synchronize around the topic space references to stop removal and
    // additions occuring at the same time. 
    synchronized (_topicSpaces)
    {
      // There are some proxies to register
      if (_topicSpaces.containsKey(destination.getUuid()))
      {
        final ArrayList topicSpacesCreated = new ArrayList();
        final ArrayList topicsCreated = new ArrayList();

        final TemporarySubscription tsubscription =
          (TemporarySubscription) _topicSpaces.get(destination.getUuid());

        // Get the list of all the subscriptions.
        final Iterator neighbourUuids = tsubscription.iMETopicList.keySet().iterator();

        // Iterate through the list of meNames to create PubSubOutputHandlers on.
        while (neighbourUuids.hasNext())
        {
          // Reset the created topicSpaces
          topicSpacesCreated.clear();

          // Reset the created topics
          topicsCreated.clear();

          // Get the next ME name.
          final SIBUuid8 neighbourUuid = (SIBUuid8) neighbourUuids.next();

          // Get the set of subscriptions that have been registered
          final Iterator subscriptions =
            ((List) tsubscription.iMETopicList.get(neighbourUuid)).iterator();

          Neighbour neighbour = getNeighbour(neighbourUuid);
          // Need a flag to indicate if this neighbour is
          // a recovered one or not.
          boolean recoveredNeighbour = (neighbour == null);
          
          // If the Neighbour wasn't found in the real list, check the recovered list as this may
          // contain it.
          if (neighbour == null)
          {
            synchronized(_recoveredNeighbours)
            {
              neighbour = (Neighbour)_recoveredNeighbours.get(neighbourUuid);
            }
          }

          // Iterate of the set of topics
          while (subscriptions.hasNext())
          {
            // Get the next topic.
            final MESubscription subscription = (MESubscription) subscriptions.next();

            // Create the proxy - this will get the create Destination
            // and attach a PubSubOutputHandler.
            final boolean proxyCreated =
              createProxy(neighbour, 
                          destination, 
                          subscription,
                          destination.getUuid(), 
                          subscription.getTopic(),
                          false);
                          
            try
            {
              // Add to MatchSpace, but only if it isn't a recovered neighbour.
              if (!recoveredNeighbour)
                subscription.eventPostCommitAdd(null);
            }
            catch (SevereMessageStoreException e)
            {
              // No FFDC code needed
              // Can never occur
            }

            // If the proxy is created, then we need to publish the event message
            // to any neighbours that are interested
            if (proxyCreated)
            {
              if (!recoveredNeighbour)
              {
                // Add the topicSpace and topic to the set of created topics.
                topicSpacesCreated.add(destination.getUuid());
                topicsCreated.add(subscription.getTopic());
              }
              else
              {
                // Add the OutputHandler to the list of known handlers.
                PubSubOutputHandler h = destination.getPubSubOutputHandler(neighbour.getUUID());
                neighbour.addPubSubOutputHandler(h);            
              }
            }
            else
            {
              // This is an error situation, as the destination now exists, 
              // so the proxy should have been created.
              final SIErrorException e =
                new SIErrorException(nls.getFormattedMessage(
                  "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                  new Object[] {
                    "com.ibm.ws.sib.processor.proxyhandler.Neighbours",
                    "1:463:1.113" },
                  null));
              
              FFDCFilter.processException(
                e,
                "com.ibm.ws.sib.processor.proxyhandler.Neighbours.topicSpaceCreated",
                "1:469:1.113",
                this);
                
              if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "topicSpaceCreated", e);  
              throw e;
            }
          }

          // Send the event message to the Neighbours interested
          // Get the Neighbour that represents this meName.
          _proxyHandler.subscribeEvent(
            topicSpacesCreated,
            topicsCreated,
            neighbour.getBusId(),
            null);
        }

        // Remove the Temporary subscriptions from the list.
        _topicSpaces.remove(destination.getUuid());
      }
      else
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "No proxy subscriptions to register");
      }

    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "topicSpaceCreated");
  }

  /**
   * If a topicSpace is deleted, remove all the PubSubOutputHandlers
   * registered against it and add them to an in memory reference table.
   *  
   * @param destination  The destination that was deleted
   * 
   * @exception SIResourceException
   * @exception SICoreException
   */
  void topicSpaceDeleted(DestinationHandler destination) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "topicSpaceDeleted", destination);

    // Synchronize around the topic space references to stop removal and
    // additions occuring at the same time. 
    synchronized (_topicSpaces)
    {
      // Get the list of the different PubSubOutputHandlers from the Destination
      // Clone the list as the original is locked
      final HashMap pubsubOutputHandlers = (HashMap)(destination.getAllPubSubOutputHandlers()).clone();
      
      // Unlock the list as leaving it locked results in deadlock
      destination.unlockPubsubOutputHandlers();

      // Check that there are some output handlers as they may have all been 
      // removed or none have ever been created on this topicSpace destination
      if (pubsubOutputHandlers != null && pubsubOutputHandlers.size() > 0)
      {
        final ArrayList topicSpacesDeleted = new ArrayList();
        final ArrayList topicsDeleted = new ArrayList();

        // Get the set of keys from the pubsubOutputHandlers which is infact the
        // list of meNames
        final Iterator neighbourUuids = pubsubOutputHandlers.keySet().iterator();

        // Iterate over the list of meNames
        while (neighbourUuids.hasNext())
        {
          // Reset the created topicSpaces
          topicSpacesDeleted.clear();

          // Reset the created topics
          topicsDeleted.clear();

          // Get the next me Name
          final SIBUuid8 neighbourUuid = (SIBUuid8) neighbourUuids.next();

          // Get the PubSubOutputHandler associated with this ME.
          final PubSubOutputHandler handler =
            (PubSubOutputHandler) pubsubOutputHandlers.get(neighbourUuid);

          final String topics[] = handler.getTopics();
          
          Neighbour neighbour = getNeighbour(neighbourUuid);
          
          // In the case that this neighbour is recovered, but not activated,
          // this neighbour should be got from the recovered list
          boolean warmRestarted = false;
          if (neighbour == null)
          {          
            synchronized(_recoveredNeighbours)
            {
              neighbour = (Neighbour)_recoveredNeighbours.get(neighbourUuid);
            }
            warmRestarted = true;
          } 
          
          // Cycle through the topics removing each one from this ME.
          // but only if there are topics to remove.
          if ( neighbour != null && topics != null)
          {
            for (int i = 0; i < topics.length; i++)
            {              
              // Remove this Handler from the MatchSpace 
              if (!warmRestarted)
              {
                MESubscription meSub = neighbour.getSubscription(destination.getUuid(), topics[i]);
              
                ControllableProxySubscription sub = meSub.getMatchspaceSub();              
                _proxyHandler
                  .getMessageProcessor()
                  .getMessageProcessorMatching()
                  .removePubSubOutputHandlerMatchTarget(sub);
                  
                destination.getSubscriptionIndex().remove(sub);
              }  
              
              // Remove the topic from the referenced topics in the handler.
              handler.removeTopic(topics[i]);
  
              // Add a reference into the reference list so when a delete arrives
              // The proxy can be removed.
              addTopicSpaceReference(neighbourUuid, destination.getUuid(), topics[i], warmRestarted);
  
              // Add the topicSpace and topic to the set of deleted topics.
              topicSpacesDeleted.add(destination.getUuid());
              topicsDeleted.add(topics[i]);
            }
          }
          
          // Having cleaned all the topics out from the handler
          // delete the handler.
          destination.deletePubSubOutputHandler(neighbourUuid);                                        
          
          
          // Publish the deleted event for the deleted topics on the topic space
          if ( neighbour!= null )
            _proxyHandler.unsubscribeEvent(
              topicSpacesDeleted,
              topicsDeleted,
              neighbour.getBusId(),
              null);
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "topicSpaceDeleted");
  }

  /** 
   * Returns the number of active Neighbours
   * 
   * @return int, the number of Neighbours
   */
  final int size()
  {
    int theSize = 0;
    synchronized(_neighbours)
    {
      theSize = _neighbours.size();
    }
    return theSize;
  }

  /** 
   * Adds a new Neighbouring ME. 
   * 
   * <p>
   * When creating a new ME, an exchange of all current subscriptions is required
   * to initialise the other Neighbour
   * 
   * @param meUUID    The UUID of the Neighbour.
   * @param busId  The bus that this ME belongs to.
   * @param transaction  The transaction to add the Neighbour under.
   *   
   * @exception SIStoreException  thrown if the Neighbour can't be added to the
   *                              item stream.
   * 
   */
  protected Neighbour addNeighbour(
    SIBUuid8 meUUID,
    String busId,
    Transaction transaction) 
    
    throws SIDiscriminatorSyntaxException, SISelectorSyntaxException, SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "addNeighbour",
        new Object[] {
          meUUID,
          busId});
          
    Neighbour neighbour = null;
    
    // Does this Neighbour exist already.
    synchronized(_neighbours)
    {
      if (_neighbours.containsKey(meUUID))
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
              SibTr.exit(
                tc,
                "addNeighbour","Neighbour already exists");
        return (Neighbour) _neighbours.get(meUUID);
      }
    }

    // Check that there isn't a matching Neighbour in the 
    // Recovered Neighbours list     
    synchronized(_recoveredNeighbours)
    {
      neighbour = (Neighbour)_recoveredNeighbours.get(meUUID);
    }
    
    if (neighbour!=null)
    {
      synchronized(_recoveredNeighbours)
      {
        _recoveredNeighbours.remove(meUUID);
      }
        
      // Iterate over the list of pubsub output handlers a
      // and add them back into the match space.
      HashSet handlers = neighbour.getPubSubOutputHandlers();
        
      if (handlers != null)
      {
        Iterator iter = handlers.iterator();
        
        while (iter.hasNext())
        {
          PubSubOutputHandler h = (PubSubOutputHandler)iter.next();
          String[] topics = h.getTopics();
          
          if (topics != null)
          {
            for (int i = 0; i<topics.length; i++)
            {
              MESubscription meSub = neighbour.getSubscription(h.getTopicSpaceUuid(), topics[i]);
              ControllableProxySubscription sub = 
                _proxyHandler.
                getMessageProcessor().
                  getMessageProcessorMatching().
                    addPubSubOutputHandlerMatchTarget(h, 
                                                      h.getTopicSpaceUuid(), 
                                                      h.getTopics()[i],
                                                      meSub.isForeignSecuredProxy(),
                                                      meSub.getMESubUserId());
              meSub.setMatchspaceSub(sub);
            }
          }
        }
      }
        
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
        SibTr.debug(tc, "Neighbour recovered " + neighbour);
    }
    else
    {     
      try
      {
        neighbour =
          new Neighbour(
            _proxyHandler,
            meUUID,
            busId,
            this);
          
        if (transaction == null)
          transaction = 
            _proxyHandler.getMessageProcessor().getTXManager().createAutoCommitTransaction();
              
        _proxyHandler.addItemStream(neighbour, transaction);
      }
      catch (MessageStoreException e)
      {
        // If the MessageStoreException is caught, then this is bad, log a FFDC
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.proxyhandler.Neighbours.addNeighbour",
          "1:755:1.113",
          this);
          
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
          SibTr.exit(tc, "addNeighbour", "SIResourceException");
            
        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0003",
          new Object[] {
            "com.ibm.ws.sib.processor.proxyhandler.Neighbours",
            "1:764:1.113",
            e,
            meUUID});
          
        throw new SIResourceException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0003",
            new Object[] {
              "com.ibm.ws.sib.processor.proxyhandler.Neighbours",
              "1:773:1.113",
              e,
              meUUID},
            null), 
          e);
      }
    }

    // Add the Neighbour to the list of Neighbours
    synchronized(_neighbours)
    {
      _neighbours.put(meUUID, neighbour);
    }
    
    // Get the BusGroup that this Neighbour belongs to.
    BusGroup group = findBus(busId);

    // If no group is found then create one
    if (group == null)
      group = createBus(busId);

    // Add the neighbour to the group
    group.addNeighbour(neighbour);

    //Assign the BusGroup to the Neighbour
    neighbour.setBus(group);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addNeighbour", neighbour);
      
    return neighbour;
  }

  /** 
   * Removes a Neighbouring ME. 
   * 
   * <p>
   * When deleting an ME, all proxy subscriptions that were registered need 
   * to be removed.
   * 
   * @param meUUID  The uuid of the Neighbouring ME.
   * @param busId  The bus that this ME belongs to.
   * @param transaction  The transaction in which to remove the Neighbour.
   * 
   * @exception  SIDestinationNotFoundException  Thrown if the Neighbour isn't 
   * known to this ME
   * @exception SIStoreException Thrown if the Neighbour can't be removed from 
   *                             the item stream.
   */
  protected void removeNeighbour(
    SIBUuid8 meUUID,
    String busId,
    Transaction transaction) throws SIConnectionLostException, SIResourceException, SIErrorException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "removeNeighbour",
        new Object[] { busId, transaction });
        
    boolean recoveredNeighbour = false;
    
    Neighbour neighbour = null;
    synchronized(_neighbours)
    {
      neighbour = (Neighbour) _neighbours.get(meUUID);
    }
    
    if (neighbour == null)
    {
      recoveredNeighbour = true;
      
      synchronized(_recoveredNeighbours)
      {
        neighbour = (Neighbour) _recoveredNeighbours.get(meUUID);
      }
    }
       
    // Does this Neighbour exist already.
    if (neighbour != null)
    {
      // Get the bus that the Neighbour belonged to.
      final BusGroup group = neighbour.getBus();

      if (group != null)
      {
        group.removeNeighbour(neighbour);

        if (group.getMembers().length == 0)
          deleteBus(group);
      }
        
      // Remove all the proxies for this Neighbour
      removeRegisteredProxies(neighbour, transaction, !recoveredNeighbour);
      
      // Remove Neighbour from the item stream.   
      try
      {
        neighbour.remove(transaction, neighbour.getLockID());
      }
      catch (MessageStoreException e)
      {
        // If the MessageStoreException is caught, then this is bad, log a FFDC
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.proxyhandler.Neighbours.removeRegisteredProxies",
          "1:879:1.113",
          this);
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
          SibTr.exit(tc, "removeRegisteredProxies", "SIResourceException");
        
        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0003",
          new Object[] {
            "com.ibm.ws.sib.processor.proxyhandler.Neighbours",
            "1:888:1.113",
            e,
            neighbour.getUUID()});
                  
        throw new SIResourceException(            
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0003",
            new Object[] {
              "com.ibm.ws.sib.processor.proxyhandler.Neighbours",
              "1:897:1.113",
              e,
              neighbour.getUUID()},
            null), 
          e);
      }
        
      // Remove the given neighbour object from the list of Neighbours
      if (!recoveredNeighbour)
      {
        synchronized(_neighbours)
        {
          _neighbours.remove(meUUID);
        }
      }
      else
      {
        synchronized(_recoveredNeighbours)
        {
          _recoveredNeighbours.remove(meUUID);
        }
      }
      
      // Ask the Neighbour to delete its destination.
      neighbour.deleteDestination();
      
    }
    else
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "removeNeighbour", "Neighbour Unknown");
        
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0005",
        new Object[] {
          "com.ibm.ws.sib.processor.proxyhandler.Neighbours",
          "1:932:1.113",
          meUUID});

      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0005",
          new Object[] {
            "com.ibm.ws.sib.processor.proxyhandler.Neighbours",
            "1:940:1.113",
            meUUID},
          null));
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeNeighbour");
  }
  
  /**
   * Removes a Neighbour that has been deleted by Admin, but it existed
   * in the MessageStore
   *
   * @param neighbourUuid  The UUID of the Neighbour
   * @param transaction    The transaction to remove the Neighbour with
   */
  protected void removeRecoveredNeighbour(SIBUuid8 neighbourUuid,
      Transaction transaction) 
                                          
  throws SIConnectionLostException, SIResourceException, SIErrorException                                         
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "removeRecoveredNeighbour",
        new Object[] { neighbourUuid, transaction });
    
    Neighbour neighbour = null;
    synchronized(_recoveredNeighbours)
    {
      neighbour  = 
        (Neighbour)_recoveredNeighbours.remove(neighbourUuid);
    }
    
    // Need to indicate that this neighbour was not recovered
    removeRegisteredProxies(neighbour, transaction, false);
    
    // Ask the Neighbour to delete its destination.
    neighbour.deleteDestination();
    
    // Ask the Neighbour to delete it remote system destinations that
    //  are pointing to the remoteME
    neighbour.deleteSystemDestinations();
    
    // Remove the neighbour from the proxy handler item stream, so that we won't
    // simply go through this same logic again next time we restart.
    try
    {
      neighbour.remove(transaction, neighbour.getLockID());
    }
    catch (MessageStoreException e)
    {
      // If the MessageStoreException is caught, then this is bad, log a FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.proxyhandler.Neighbours.removeNeighbour",
        "1:994:1.113",
        this);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "removeRecoveredNeighbour", "SIResourceException");

      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0003",
        new Object[] {
          "com.ibm.ws.sib.processor.proxyhandler.Neighbours",
          "1:1003:1.113",
          e,
          neighbour.getUUID()});

      throw new SIResourceException(            
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0003",
          new Object[] {
            "com.ibm.ws.sib.processor.proxyhandler.Neighbours",
            "1:1012:1.113",
            e,
            neighbour.getUUID()},
          null), 
        e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeRecoveredNeighbour");
  }
  
  /** 
   * Removes all the proxies associated with this Neighbour.
   * 
   * @param neighbour
   * @param transaction
   */
  private void removeRegisteredProxies(Neighbour neighbour, 
      Transaction transaction,
                                       boolean wasRecovered) throws SIResourceException 
                                       
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "removeRegisteredProxies", 
        new Object[] { neighbour, transaction, new Boolean(wasRecovered) } );

    // Get all the proxies that this Neighbour has registered
    final Hashtable registeredProxies = neighbour.getRegisteredProxies();

    // Check that there is actually some proxies to remove.
    if (!registeredProxies.isEmpty())
    {
      boolean proxiesDeregistered = false;

      final Enumeration subscriptions = registeredProxies.elements();

      final ArrayList topics = new ArrayList();
      final ArrayList topicSpaces = new ArrayList();

      // Loop through each of the subscriptions that where registered and
      // remove them.
      while (subscriptions.hasMoreElements())
      {
        final MESubscription subscription =
          (MESubscription) subscriptions.nextElement();
          
        // Remove the proxy from the list of proxies for this Neighbour.
        neighbour.proxyDeregistered(subscription.getTopicSpaceUuid(), 
                                    subscription.getTopic(), 
                                    transaction);
        
        DestinationHandler destination = 
          _destinationManager.getDestinationInternal(subscription.getTopicSpaceUuid(), false); 
        
        // delete the proxy subscription
        final boolean deleted =
          deleteProxy(
            destination,
            subscription,
            neighbour,
            subscription.getTopicSpaceUuid(),
            subscription.getTopic(),
            wasRecovered,
            true);
            

        // If a PubSubOutputHandler is deleted, then add this topic and
        // topic space to the set to be fed through the topic space deleted 
        // event.   
        if (deleted)
        {          
          topics.add(subscription.getTopic());
          topicSpaces.add(subscription.getTopicSpaceUuid());

          proxiesDeregistered = true;
        }

      }

      // If there were any proxies removed, then call the proxy handler code to 
      // publish that these subscriptions have been deleted
      if (proxiesDeregistered && wasRecovered)
      {
        _proxyHandler.unsubscribeEvent(
          topicSpaces,
          topics,
          neighbour.getBusId(),
          null);
      }


    }
    else
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(
          tc,
          "no proxies registered for neighbour " + neighbour.getUUID().toString());

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeRegisteredProxies");
  }

  /**Finds a Bus given its name.
   *
   * @param name  The name of the Bus to find
   * 
   * @return The BusGroup or null if not found.
   *
   */
  private BusGroup findBus(String name)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "findBus", name);

    // Cycle through the list of Buss.
    for (int i = 0; i < _buses.length; ++i)
      // Check if the Bus name is the one we are looking for
      if (_buses[i].getName().equals(name))
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "findBus", _buses[i]);

        return _buses[i];
      }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "findBus", "null");

    return null;
  }

  /** Creates a BusGroup with the given name.
   * 
   * As this is a new Bus, the current subscription state in
   * the MatchSpace is read and assigned to the Bus as active 
   * subscriptions.
   *
   * @param busId  The name of the Busgroup 
   *
   * @return The new BusGroup object representing the bus.
   * 
   */
  private BusGroup createBus(String busId)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createBus", busId);

    // Create a new group and add it to the list.
    boolean isLocalBus = busId.equals(_localBusName);
    final BusGroup group = new BusGroup(busId, _proxyHandler, isLocalBus);
    final BusGroup[] tempBuses = _buses;
    _buses = new BusGroup[tempBuses.length + 1];
    System.arraycopy(tempBuses, 0, _buses, 0, tempBuses.length);
    _buses[tempBuses.length] = group;

    // Do not need to propogate local subs to foreign buses
    // addSubscriptionsToBus(group, busId);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createBus", group);

    return group;
  }

  /**
   * Removes the Bus with the given name from the list of all Buss.
   * 
   * @param group  The group to be removed
   * 
   */
  private void deleteBus(BusGroup group)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "deleteBus", group);

    // Find the position in the list which contains the group, and remove
    // the group.

    final BusGroup[] t = _buses;
    for (int i = 0; i < _buses.length; i++)
    {
      if (_buses[i].equals(group))
      {
        _buses = new BusGroup[t.length - 1];
        if (i > 0)
          System.arraycopy(t, 0, _buses, 0, i);
        if (i < t.length - 1)
          System.arraycopy(t, i + 1, _buses, i, t.length - 1 - i);

        break;
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "deleteBus");
  }

/**
   * Gets the Neighbour based on the UUID supplied
   * 
   * @param neighbourUUID  The UUID for the Neighbour
   * 
   * @return the Neighbour object
   */
  Neighbour getNeighbour(SIBUuid8 neighbourUuid)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getNeighbour", neighbourUuid);

    Neighbour neighbour = null;
    synchronized(_neighbours)
    {
      neighbour = (Neighbour) _neighbours.get(neighbourUuid);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getNeighbour", neighbour);

    return neighbour;
  }
  
  /**
     * Gets the Recovered Neighbour based on the UUID supplied
     * 
     * @param neighbourUUID  The UUID for the Neighbour
     * 
     * @return the Neighbour object
     */
    Neighbour getRecoveredNeighbour(SIBUuid8 neighbourUuid)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "getRecoveredNeighbour", neighbourUuid);

      Neighbour neighbour = null;
      synchronized(_recoveredNeighbours)
      {
        neighbour = (Neighbour) _recoveredNeighbours.get(neighbourUuid);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getRecoveredNeighbour", neighbour);

      return neighbour;
    }
  
  /**
   * Gets the Neighbour based on the UUID supplied
   * 
   * @param neighbourUUID  The UUID for the Neighbour
   * 
   * @return the Neighbour object
   */
  Neighbour getBusNeighbour(String busId)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getBusNeighbour", busId);
      
    Neighbour neighbour = null;
    synchronized(_neighbours)
    {
      Iterator neighbours = _neighbours.keySet().iterator();
      while (neighbours.hasNext())
      {
        neighbour = getNeighbour((SIBUuid8) neighbours.next());
        if (neighbour.getBusId().equals(busId)) 
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getBusNeighbour", neighbour);
          return neighbour;
        }        
      }
    }

    synchronized(_recoveredNeighbours)
    {
      Iterator recoveredNeighbours = _recoveredNeighbours.keySet().iterator();
      while (recoveredNeighbours.hasNext())
      {
        neighbour = (Neighbour) _recoveredNeighbours.get(recoveredNeighbours.next());
        if (neighbour.getBusId().equals(busId)) 
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getBusNeighbour", neighbour);
          return neighbour;
        }        
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getBusNeighbour", null);
    return null;
  }

  /** Creates the proxy based on the meName, topicSpace and topic
  * for the originating ME.
  * 
  * This creates the PubSubHandler and adds it to the MatchSpace.
  * 
  * @param neighbour  The neighbour object that is creating the output handler.
  * @param destination  The destination object that the remote pubsuboutput handler 
  *                      should be created against
  * @param subscription  The Subscription object that has been registered for this proxy.
  * @param topicSpace  The topic space uuid
  * @param topic   The topic for the proxy subscription.
  * 
  * @return boolean  true, if a PubSubOutputHandler has been created on a destination
  */
  protected boolean createProxy(
    Neighbour neighbour,
    DestinationHandler destination,
    MESubscription subscription,
    SIBUuid12 topicSpace,
    String topic,
    boolean warmRestarted) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "createProxy",
        new Object[] { neighbour, 
                       destination, 
                       subscription, 
                       topicSpace, 
                       topic, 
                       new Boolean(warmRestarted) });

    boolean outputHandlerCreated = false;

    // Assign this Handler to the MatchSpace (as long as the topic space
    // exists, otherwise the proxy is in limbo until either D3 catches up 
    // with the topic space declaration, or the proxy is deleted.
    if (destination != null)
    {
      // Get the PubSubHandler from the destination object.
      PubSubOutputHandler handler = destination.getPubSubOutputHandler(neighbour.getUUID());

      // If there isn't an OutputHandler, then create one.
      if (handler == null)
      {
        handler = destination.createPubSubOutputHandler(neighbour);
      }

      // Set the forign topicspace mapping
      handler.setTopicSpaceMapping(subscription.getForeignTSName());
      
      // Set the topic in the output handler.
      handler.addTopic(topic);
      
      // Set the information needed for the commit
      subscription.registerForPostCommit(_proxyHandler, destination, handler, neighbour);

      outputHandlerCreated = true;

    }
    else
    {
      // Add a reference into the Neighbours class to indicate that we 
      // have a Proxy subscription, but no destination to add it to, so
      // we will wait until this destination is created before adding it.
      addTopicSpaceReference(neighbour.getUUID(), topicSpace, topic, warmRestarted);
      
      subscription.registerForPostCommit(neighbour, this);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createProxy", new Boolean(outputHandlerCreated));

    return outputHandlerCreated;
  }

  /**
  * Removes the reference to the PubSubOutHandler on the given topic space
  * for the topic on the ME.
  * 
  * @param destination  The destination object
  * @param subscription  The subscription representing this proxy
  * @param neighbour  The neighbour object
  * @param topicSpace  The name of the topicSpace destination.
  * @param topic  The name of the topic to remove.
  * @param wasRecovered  Indicates if the Neighbour is to be deleted
  * 
  * @return true if a PubSubOutputHandler was deleted from the destination.
  *          false if a reference was just removed
  */
  protected boolean deleteProxy(
    DestinationHandler destination,
    MESubscription subscription,
    Neighbour neighbour,
    SIBUuid12 topicSpace, 
    String topic,
    boolean wasRecovered,
    boolean deleteHandler )
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "deleteProxy",
        new Object[] { destination, neighbour, subscription, topicSpace, topic });

    boolean outputHandlerDeleted = false;
    /* 
     * If the destination object is not null, then we can remove
     * the PubSubOutputHandler for this Neighbour for this topic.
     */
    if (destination != null)
    {
      PubSubOutputHandler handler = destination.getPubSubOutputHandler(neighbour.getUUID());

      // If the handler is null, then it has already been removed, 
      // otherwise need to remove this instance from the MatchSpace.
      if (handler != null)
      {
        // Remove the topic from the output handler.
        handler.removeTopic(topic);
      
        // If we were asked not to delete the PubSubOuptutHandler then pass null into 
        // registerForPostCommit() which will prevent the eventPostRemove code from deleting it  
        if( !deleteHandler )  
          handler = null;
          
        // Register for a commit, but only if the neighbour is in the active state.
        if (wasRecovered)
          subscription.registerForPostCommit(_proxyHandler, destination, handler, neighbour);
        else
          subscription.registerForPostCommit(null, destination, handler, neighbour);          

        outputHandlerDeleted = true;
      }
      else
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "PubSubOutputHandler not found");
    }
    else
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Destination object not found");
        
      // Register on the subscription
      if (wasRecovered)
        subscription.registerForPostCommit(neighbour, this);
      else
        subscription.registerForPostCommit(null, destination, null, neighbour);

      // Clear up the topic Space references for this topic on this ME.
      removeTopicSpaceReference(neighbour.getUUID(), subscription, topicSpace, topic);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "deleteProxy", new Boolean(outputHandlerDeleted));

    return outputHandlerDeleted;
  }
  
  /**
   * Recovers the Neighbours from the MessageStore
   *
   * @exception MessageStoreException Thrown if there was a failure 
   *                        recovering the Neighbours.
   */
  protected void recoverNeighbours() throws MessageStoreException, SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "recoverNeighbours");
    
    NonLockingCursor cursor = null;
    
    try 
    {   
      cursor 
        = _proxyHandler.newNonLockingItemStreamCursor(
          new ClassEqualsFilter(Neighbour.class));
  
      AbstractItem item = null;
      while ((item = cursor.next()) != null)
      {
        Neighbour neighbour = null;
          neighbour = (Neighbour)item; 
        
        // Add the Neighbour into the list of recovered Neighbours
        synchronized(_recoveredNeighbours)
        {
          _recoveredNeighbours.put(neighbour.getUUID(), neighbour);
        }
          
        // Setup the non persistent state for this Neighbour.
        neighbour.intialiseNonPersistent(_proxyHandler, this);
          
        // Ask the neighbour to recover its subscriptions.
        neighbour.recoverSubscriptions(_proxyHandler);
  
        // Check that the Bus matches the current Bus.
        if (!neighbour.getBusId().equals(_proxyHandler.getMessageProcessor().getMessagingEngineBus()))
        {        
          // This neighbour lives on a Foreign Bus.
          // Make sure this link still exists before putting it into the neighbours list
          
          ForeignBusDefinition bus = _proxyHandler.getMessageProcessor().getForeignBus(neighbour.getBusId());
          // If there is no bus the bus definition will be null
          if (bus != null)
          {
            try
            {
              //see defect 295990
              //This is a bit of a hack, but there is a window wherby the link could have
              //been deleted but not actually removed before the ME restarted.
              //We can examine the link's locality set to ascertain whether 
              //this link is really 'there' or not.
              VirtualLinkDefinition vLinkDef = bus.getLink();
              if( vLinkDef!=null 
                  && vLinkDef.getLinkLocalitySet()!=null 
                  && vLinkDef.getLinkLocalitySet().contains(neighbour.getUUID().toString()))
              {
                try
                {              
                  addNeighbour(neighbour.getUUID(), neighbour.getBusId(), null);
                }
                catch (SIException e)
                {
                  // FFDC
                  FFDCFilter.processException(
                    e,
                    "com.ibm.ws.sib.processor.proxyhandler.Neighbours.recoverNeighbours",
                    "1:1534:1.113",
                    this);
                      
                  final SIErrorException finalE = new SIErrorException(
                    nls.getFormattedMessage(
                      "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                      new Object[] {
                        "com.ibm.ws.sib.processor.proxyhandler.Neighbours",
                        "1:1542:1.113",
                        e },
                      null),
                    e);
                      
                  SibTr.exception(tc, finalE);
                  SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                    new Object[] {
                      "com.ibm.ws.sib.processor.proxyhandler.Neighbours",
                      "1:1551:1.113",
                      e });
                  
                  if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "recoverNeighbours", finalE);
          
                  throw finalE;
                }
              }
              else
              {
                //the link's locality set was not correct
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                  SibTr.debug(tc, "Link no longer exists " + neighbour.getBusId() + " for neighbour " + neighbour.getUUID());              
              }
            }
            catch(SIBExceptionNoLinkExists e)
            {
              // No FFDC code needed
              //There is no link whatsoever, this probably means that the foreign bus still 
              // exists, but the link has been deleted
                
              if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Link no longer exists " + neighbour.getBusId() + " for neighbour " + neighbour.getUUID() + " : " + e);
            }
          }  
          else
          {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
              SibTr.debug(tc, "Bus no longer exists " + neighbour.getBusId() + " for neighbour " + neighbour.getUUID());          
          }
        } 
      }//end while
    }
    finally
    {
      if (cursor != null)
        cursor.finished();
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "recoverNeighbours");    
  }

  /**
   * A TemporarySubscription is a proxy subscription that doesn't have a destination 
   * to attach to.  Either the destination has been deleted and we haven't seen the proxy
   * deletes yet, or the destination hasn't been created yet
   * 
   * The TemporarySubscription contains a list of ME's as a HashMap.
   * The values are an ArrayList of topics that this ME has expressed
   * an interest in on the topicSpace.
   */
  class TemporarySubscription
  {
    private HashMap iMETopicList;

    /** 
     * Constructor for the TemporarySubscription object
     * 
     * @param neighbourUuid  The uuid of the Neighbouring ME.
     * @param subscription  The subscription being represented for this topic space.
     */
    TemporarySubscription(SIBUuid8 neighbourUuid, MESubscription subscription)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "TemporarySubscription", new Object[] { neighbourUuid, subscription });

      iMETopicList = new HashMap();

      final LinkedList topics = new LinkedList();
      topics.add(subscription);

      iMETopicList.put(neighbourUuid, topics);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "TemporarySubscription", this);
    }

    /** 
     * Adds a topic to the list of topics for this topic space
     *
     * @param neighbourUuid  The uuid of the Neighbouring ME.
     * @param subscription  The subscription being represented for this topic space.
     */
    protected void addTopic(SIBUuid8 neighbourUuid, MESubscription subscription)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "addTopic", new Object[] { neighbourUuid, subscription });

      List list = (List) iMETopicList.get(neighbourUuid);

      if (list == null)
        list = new LinkedList();

      list.add(subscription);

      iMETopicList.put(neighbourUuid, list);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "addTopic");
    }

    /** Removes the topic from the list of topics
     * 
     * @param neighbourUuid  The uuid of the Neighbouring ME.
     * @param subscription  The subscription being represented for this topic space.
     * 
     * @return true  if there are no more topics on referenced by any ME.
     */
    protected boolean removeTopic(SIBUuid8 neighbourUuid, MESubscription subscription)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "removeTopic", new Object[] { neighbourUuid, subscription });

      boolean returnVal = false;
      final List list = (List) iMETopicList.get(neighbourUuid);

      // If the list isn't empty, then remove the topic from the 
      // list
      if (list != null)
      {
        list.remove(subscription);

        // If the topic list is empty, then remove the neighbour from the list
        // of Neighbours interested in this topic space.
        if (list.isEmpty())
        {
          iMETopicList.remove(neighbourUuid);

          returnVal = iMETopicList.isEmpty();
        }
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "removeTopic", new Boolean(returnVal));
        
      return returnVal;
    }
  }
  
  /**
   * Adds the subscriptions to the Bus
   * 
   * @param group
   * @param BusId
   * @throws SIResourceException
   */
  private void addSubscriptionsToBus(BusGroup group, String busId) 
  throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "addSubscriptionsToBus", new Object[]{group, busId});
    
    // Get the complete list of Subscriptions from the MatchSpace 
    // and assign to the BusGroup.
    final ArrayList cdList =
      _proxyHandler
        .getMessageProcessor()
        .getMessageProcessorMatching()
        .getAllCDMatchTargets();

    // Iterate through the list of available "subscriptions".   
    final Iterator cdIterator = cdList.listIterator();
    while (cdIterator.hasNext())
    {
      // Get the Consumer Dispatcher for this element.
      final ConsumerDispatcher cd = (ConsumerDispatcher) cdIterator.next();

      // This won't actually send the subscriptions to the Neighbour
      // but generates the list of topics that are to be propagated.
      group.addLocalSubscription(cd.getConsumerDispatcherState(), null, null, false);

    }

    // Add proxy subscriptions that have been registered by
    // Neighbouring ME's.
    final ArrayList phList = 
      _proxyHandler
        .getMessageProcessor()
        .getMessageProcessorMatching()
        .getAllPubSubOutputHandlerMatchTargets();
        
    final ArrayList outputSeenList = new ArrayList();
    
    final Iterator phIterator = phList.listIterator();
    while (phIterator.hasNext())
    {
      // Get the PubSub OutputHandler for this element.
      final PubSubOutputHandler oh = (PubSubOutputHandler) phIterator.next();
      
      if (!outputSeenList.contains(oh) &&
           oh.neighbourOnDifferentBus(busId))
      {
        // This won't actually send the subscriptions to the Neighbour
        // but generates the list of topics that are to be propagated.
        final String topics[] = oh.getTopics();
        final SIBUuid12 topicSpaceUuid = oh.getTopicSpaceUuid();
        
        if (topics != null && topics.length > 0)
        {
          for (int i=0; i<topics.length; i++)        
          {
            group.addRemoteSubscription(topicSpaceUuid, topics[i], null, false);             
          }
        }
        
        // Add the neighbour to the list of seen Neighbours.
        outputSeenList.add(oh);
      }      
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "addSubscriptionsToBus");
  }
  
  /**
   * Resets the list of subscriptions that the bus knows about.
   * 
   */
  protected void resetBusSubscriptionList()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "resetBusSubscriptionList");

    // Iterate over the list of Buses
    for (int i=0; i<_buses.length; i++)
    {
      BusGroup group = _buses[i];
      
      // Reset the list of subscriptions
      group.reset();
      
      try      
      {
        // Add this list of subscriptions to the list for this bus
        addSubscriptionsToBus(group, group.getName());
        
        // Create the reset subscription message for this bus.
        SubscriptionMessage message = group.generateResetSubscriptionMessage();
        
        // If there is a reset message to send, then send it.
        LocalTransaction transaction = _proxyHandler.getMessageProcessor().getTXManager().createLocalTransaction(true);
        
        // Send to the list of all neighbours.
        group.sendToNeighbours(message, (Transaction) transaction, true);
          
        transaction.commit();
        
      }
      catch (SIException e)
      {
        // No FFDC code needed
          
        SibTr.exception(tc, e);
        
        group.resetListFailed();
                 
      }
      
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "resetBusSubscriptionList");
    
  }
  
  
}
