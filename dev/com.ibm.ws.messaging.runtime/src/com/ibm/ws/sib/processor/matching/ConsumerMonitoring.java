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
package com.ibm.ws.sib.processor.matching;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
                                                                     
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.ws.sib.matchspace.MatchTarget;
import com.ibm.wsspi.sib.core.ConsumerSetChangeCallback;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.ConnectionImpl;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;

/** 
 * This class manages the process of registering and driving callbacks on entities
 * that monitor consumers.
 * 
 * In particular this allows the support of WSN Registered publishers.
 */
public class ConsumerMonitoring
{
  private static final TraceComponent tc =
    SibTr.register(
      ConsumerMonitoring.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  // Object that keeps track of registered monitors
  private ConsumerMonitorRegistrar _consumerMonitorRegistrar;

  // Object that keeps track of registered monitors
  private SubscriptionRegistrar _subscriptionRegistrar;
  
  //------------------------------------------------------------------------------
  // Constructors for ConsumerMonitoring
  //------------------------------------------------------------------------------
  
  public ConsumerMonitoring(MessageProcessor messageProcessor,
                            MessageProcessorMatching mpm)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "ConsumerMonitoring", new Object[]{ messageProcessor, mpm});
      
    _consumerMonitorRegistrar = new ConsumerMonitorRegistrar(messageProcessor, mpm);
    _subscriptionRegistrar = new SubscriptionRegistrar(mpm);

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "ConsumerMonitoring", this);
  }

  /** 
   * Method addConsumer
   * 
   * A monitored consumer is being added to the system. 
   * 
   * This method will categorise the subscription depending on whether it is
   * wildcarded and/or has a selector.
   * 
   * Additionally it will update the subscriptions information in the consumer 
   * monitors table and add appropriate references into the monitored consumer
   * object itself.
   * 
   * @param key the key to use to remove this target
   * @param topic the topic to use
   * @param selectorString the selector string to use
   * @param target the target to use.
   * @exception SIDiscriminatorSyntaxException if the topic syntax is invalid
   */
  public void addConsumer(
    Object key,
    String topic,
    String selectorString,
    MatchTarget target,
    boolean isWildcarded)
  throws SIDiscriminatorSyntaxException                                         
  {
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, 
                  "addConsumer", 
                  new Object[]{key, 
                               topic, 
                               selectorString, 
                               target,
                               new Boolean(isWildcarded)});

    boolean selector = false;
    MonitoredConsumer mc = (MonitoredConsumer)target;
    // Is there a selector expression
    if (selectorString != null && selectorString.trim().length() != 0)
    {
      selector = true;
    }
    
    // Has a consumer already registered on this topicexpression? 
    //
    // If they have then, we can use the matching monitor lists in the existing consumer
    // to short circuit the monitor matching process.
    //
    // Note that in this case we are agnostic about selectors.....but need to 
    // make sure that the SubscriptionRegistrar adds the consumer in the 
    // correct place
    if(selector)
    {
      // This consumer has a selector, see if there is an existing subscription
      // registrar entry
      if(_subscriptionRegistrar.isKnownSelectorExpression(topic, isWildcarded))
      {
        // Found existing entry in selector table add this consumer to
        // that entry        
        addConsumerForKnownTopicExpr(topic, selector, isWildcarded, mc, true);        
      }
      else
      {
        // See if theres a corresponding non-selector entry
        if(_subscriptionRegistrar.isKnownNonSelectorExpression(topic, isWildcarded))
        {
          // Found existing entry in non-selector table, add a new row in the 
          // subscriptionRegistrar's selector table
          _subscriptionRegistrar.addNewConsumerForExpression(topic,mc,selector, isWildcarded);
          addConsumerForKnownTopicExpr(topic, selector, isWildcarded, mc, false);             
        }
        else
        {
          // An Entirely new expression, we'll have to do the matching
          // Add a new row in the table of subscriptions        
          _subscriptionRegistrar.addNewConsumerForExpression(topic,mc,selector, isWildcarded);          
          addConsumerForNewTopicExpr(topic, isWildcarded, mc);                  
        }
      }
    }
    else
    {
      // This consumer has no selector, see if there is an existing subscription
      // registrar entry
      if(_subscriptionRegistrar.isKnownNonSelectorExpression(topic, isWildcarded))
      {
        // Found existing entry in non-selector table add this consumer to
        // that entry        
        addConsumerForKnownTopicExpr(topic, selector, isWildcarded, mc, true);        
      }
      else
      {
        // See if theres a corresponding selector entry
        if(_subscriptionRegistrar.isKnownSelectorExpression(topic, isWildcarded))
        {
          // Found existing entry in selector table, add a new row in the 
          // subscriptionRegistrar's non-selector table
          _subscriptionRegistrar.addNewConsumerForExpression(topic,mc,selector, isWildcarded);
          addConsumerForKnownTopicExpr(topic, selector, isWildcarded, mc, false);             
        }
        else
        {
          // An Entirely new expression, we'll have to do the matching
          // Add a new row in the table of subscriptions        
          _subscriptionRegistrar.addNewConsumerForExpression(topic,mc,selector, isWildcarded);          
          addConsumerForNewTopicExpr(topic, isWildcarded, mc);                  
        }
      }      
    }

    // Set info into the Target itself
    if(selector)
      mc.setSelector();
    if(isWildcarded)
      mc.setWildcarded();
    mc.setTopic(topic);     
 
    if (tc.isEntryEnabled()) SibTr.exit(tc, "addConsumer");
  }

  /** 
   * Method removeConsumer
   * 
   * Remove a consumer from the Monitoring state
   * 
   * @param mc the consumer to be removed
   * @exception MatchingException if key undefined or on serious error
   */
  public void removeConsumer(MonitoredConsumer mc)
  {
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, 
                  "removeConsumer", 
                  new Object[]{mc});
      
    boolean selector = mc.isSelector();
    boolean isWildcarded = mc.isWildcarded();
    String topic = mc.getTopic();
    ArrayList consumerList = 
      _subscriptionRegistrar.getConsumerListForExpression(topic, selector, isWildcarded);
        
    //TODO: handle empty return
        
    if (tc.isDebugEnabled())
      SibTr.debug(tc, "Found existing entry with list: " + consumerList +
                      ", of size: " + consumerList.size());
        
    // Remove the consumer      
    boolean removed = consumerList.remove(mc);
    if(!removed)
    {
      // Couldn't find the consumer. This means we have an internal 
      // inconsistency in our tables, so throw an SIErrorException
    
      if (tc.isEntryEnabled()) 
        SibTr.exit(tc, "removeConsumer", "SIErrorException");
      
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
        new Object[] {
          "com.ibm.ws.sib.processor.matching.ConsumerMonitoring",
          "1:248:1.7"
          });  
        
      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0001",
          new Object[] {
            "com.ibm.ws.sib.processor.matching.ConsumerMonitoring",
            "1:256:1.7"
          },
          null));      
    }          
                 
    // Only remove the row in the table from the subscription registrar
    // if this was the last consumer in the list
    if(consumerList.isEmpty())
    {
      _subscriptionRegistrar.removeConsumerListForExpression(topic,selector,isWildcarded);                  
    }
        
    // 1. Need to remove sub references from consumer monitor tables
    // 2. Drive callbacks where appropriate
    ArrayList exactMonitorList = (ArrayList)mc.getMatchingExactMonitorList();
    ArrayList wildcardMonitorList = (ArrayList)mc.getMatchingWildcardMonitorList();
        
    _consumerMonitorRegistrar.
      removeConsumerFromRegisteredMonitors(mc, exactMonitorList, wildcardMonitorList);
          
    if (tc.isEntryEnabled()) SibTr.exit(tc, "removeConsumer"); 
  }

  /**
   * Method registerConsumerSetMonitor
   * 
   * Checks whether there are existing registrations on this topicspace and 
   * discriminator combination. If there are then we will match the existing set 
   * of subscriptions associated with it - we need only add the supplied callback to
   * the existing table in the ConsumerMonitorRegistrar.    
   * 
   * If there are no existing registrations on this topicspace and discriminator 
   * combination, then we have additional work to do. We must determine the set
   * of matching consumers and add a new entry to the ConsumerMonitorRegistrar.
   * 
   * Returns true if the potential set of consumers is currently greater than zero, 
   * false if it is zero.
   * 
   * @param topicSpace
   * @param topicSpaceUuid
   * @param discriminatorExpression
   * @param combinedExpression
   * @param callback
   * @param isWildcarded
   * @param wildcardStem
   * @param mpm
   * @return
   * @throws SIDiscriminatorSyntaxException
   * @throws SIErrorException
   */
  public boolean registerConsumerSetMonitor(
    ConnectionImpl connection, 
    DestinationHandler topicSpace,
    SIBUuid12 topicSpaceUuid,
    String discriminatorExpression,
    String combinedExpression,
    ConsumerSetChangeCallback callback,
    boolean isWildcarded,
    String wildcardStem,
    MessageProcessorMatching mpm)
  throws
      SIDiscriminatorSyntaxException,
      SIErrorException   
  {
    if (tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "registerConsumerSetMonitor",
          new Object[] { connection,
                         topicSpace, 
                         topicSpaceUuid,
                         discriminatorExpression,
                         combinedExpression,
                         callback,
                         new Boolean(isWildcarded),
                         wildcardStem }); 
             
    boolean areConsumers = false;

    // Test whether there are existing registrations on this topicspace. If so,
    // we will match the existing set of subscriptions associated with it.    
    if(_consumerMonitorRegistrar.checkExistingExpression(combinedExpression, isWildcarded))
    {
      // topic expression has been used by another registration, in this case
      // we need to add our callback entry to the registrations table only
      areConsumers = 
        _consumerMonitorRegistrar.
          registerCallbackOnExistingExpression(connection,
                                               combinedExpression,
                                               isWildcarded, 
                                               callback);      
    }
    else
    {
      // A new topic expression. We'll have to find the set of consumers that map
      // to it.
    
      Set localConsumers = new HashSet(); // We'll use these sets where we have
      Set remoteConsumers = new HashSet(); // searched the matchspace 
           
      // The style of processing depends on whether the discriminator in the 
      // registration is wildcarded
      if(!isWildcarded)
      {
        mpm.retrieveNonSelectorConsumers(topicSpace, 
                                         discriminatorExpression,
                                         localConsumers,
                                         remoteConsumers);
              
        // Now we need to check whether there are any subscriptions with selectors
        // that "might" match this expression.
        _subscriptionRegistrar.findMatchingSelectorSubs(combinedExpression, localConsumers);
      }
      else
      {
        // If wildcarded use MatchSpace direct evaluation code and string matching
        _subscriptionRegistrar.findCandidateSubsForWildcardExpr(combinedExpression, wildcardStem, localConsumers);
      }
      
      // Now we perform the actual registration of our monitor
      areConsumers = registerCallbackOnNewExpression(connection,
                                                     combinedExpression,
                                                     isWildcarded, 
                                                     callback,
                                                     localConsumers,
                                                     remoteConsumers);      
      
    }

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "registerConsumerSetMonitor", new Boolean(areConsumers));
    return areConsumers;  
  }

  /**
   * Method registerCallbackOnNewExpression
   * 
   * Adds a list of matching consumers in a new entry to the 
   * ConsumerMonitorRegistrar. 
   * 
   * Also walks through the list of matching consumers adding a reference to this
   * registration. This will ease the process of registration management when a 
   * subscriber is removed.
   * 
   * @param topicExpression
   * @param isWildcarded
   * @param callback
   * @param localConsumers
   * @param remoteConsumers
   * @return
   * @throws SIDiscriminatorSyntaxException
   * @throws SIErrorException
   */
  private boolean registerCallbackOnNewExpression(
    ConnectionImpl connection, 
    String topicExpression,
    boolean isWildcarded,
    ConsumerSetChangeCallback callback,
    Set localConsumers,
    Set remoteConsumers)
  {
    if (tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "registerCallbackOnNewExpression",
          new Object[] { connection,
                         topicExpression,
                         new Boolean(isWildcarded), 
                         callback, 
                         localConsumers, 
                         remoteConsumers}); 
             
    boolean areConsumers = false;

    // Need to locate consumers for this registration, note that there may be
    // none.
    ArrayList matchingConsumers = new ArrayList();
    
    if(localConsumers != null || remoteConsumers != null)
    {
      // Have some non-selector consumers that matched a fully qualified registration
      if(localConsumers != null)
      {
        matchingConsumers.addAll(localConsumers);
      }
      
      // Check through proxies
      if(remoteConsumers != null)
      {
        matchingConsumers.addAll(remoteConsumers);
      }  
    }

    // Next we walk through the list of matching consumers adding a reference to this
    // registration. This will ease the process of registration management when a 
    // subscriber is removed.
    areConsumers = !matchingConsumers.isEmpty();
    if(areConsumers)
    {
      // Add a reference to this registration to each consumer
      Iterator i = matchingConsumers.iterator();
      while (i.hasNext())
      {
        MonitoredConsumer mc = (MonitoredConsumer)i.next();     
      
        if (tc.isDebugEnabled()) 
          SibTr.debug(tc, "Add registration to consumer: " + mc);
        
        // Add the reference
        if(isWildcarded)
          mc.addMatchingWildcardMonitor(topicExpression);
        else
          mc.addMatchingExactMonitor(topicExpression);        
      } 
    }    
     
    // Now we've assembled a complete list perform the actual registration of our monitor
    _consumerMonitorRegistrar.
      registerCallbackOnNewExpression(connection,
                                      topicExpression, 
                                      isWildcarded, 
                                      callback, 
                                      matchingConsumers);
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "registerCallbackOnNewExpression", new Boolean(areConsumers));
    return areConsumers;  
  }

  /**
   * Method deregisterConsumerSetMonitor
   * 
   * Deregisters a previously registered callback.
   * 
   * @param callback
   * @throws SINotPossibleInCurrentConfigurationException
   */
  public void deregisterConsumerSetMonitor(ConnectionImpl connection,
                                           ConsumerSetChangeCallback callback)
  throws SINotPossibleInCurrentConfigurationException   
  {
    if (tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "deregisterConsumerSetMonitor",
          new Object[] { connection, callback}); 

     // Call the registrar to do the work
     _consumerMonitorRegistrar.deregisterMonitor(connection, callback);
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "deregisterConsumerSetMonitor");
 
  }

  /**
   * Method removeConsumerSetMonitors
   * 
   * Removes all callbacks associated with a connection.
   * 
   * @param connection
   * @throws SINotPossibleInCurrentConfigurationException
   */
  public void removeConsumerSetMonitors(ConnectionImpl connection)
  throws SINotPossibleInCurrentConfigurationException       
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "removeConsumerSetMonitors", new Object[] { connection });

    // Call the registrar
    _consumerMonitorRegistrar.removeConsumerSetMonitors(connection);

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "removeConsumerSetMonitors");
  }

  /** 
   * Method addConsumerForKnownTopicExpr
   * 
   * A monitored consumer is being added for a known expression. 
   * 
   * This method uses the monitoring information stored in an existing consumer
   * in order to update the information in the new consumer and in order to
   * add appropriate references into the consumer monitors table.
   * 
   * @param consumerList list of existing consumers for this topic expression
   * @param mc the consumer being added.
   */
  private void addConsumerForKnownTopicExpr(
    String topic, 
    boolean selector, 
    boolean isWildcarded,
    MonitoredConsumer mc,
    boolean matchedOnSelector)
  {
    if (tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "addConsumerForKnownTopicExpr",
          new Object[] { topic,
                         new Boolean(selector),
                         new Boolean(isWildcarded),
                         mc,
                         new Boolean(matchedOnSelector) });
                          
    ArrayList consumerList = null;
    if(matchedOnSelector)
    { 
      consumerList = 
        _subscriptionRegistrar.getConsumerListForExpression(topic, selector, isWildcarded);                              
    }
    else
    {
      consumerList = 
        _subscriptionRegistrar.getConsumerListForExpression(topic, !selector, isWildcarded);      
    }

    // This topic expression has already been categorised
    if (tc.isDebugEnabled())
      SibTr.debug(tc, "Found existing entry with list: " + consumerList +
                        ", of size: " + consumerList.size());
      
    // Retrieve the list of matching registered consumer monitors from
    // an existing consumer
    MonitoredConsumer existingmc = (MonitoredConsumer)consumerList.get(0);
    // And add to the new consumer
    ArrayList exactMonitorList = (ArrayList)existingmc.getMatchingExactMonitorList();
    mc.setMatchingExactMonitorList(exactMonitorList);
    ArrayList wildcardMonitorList = (ArrayList)existingmc.getMatchingWildcardMonitorList();
    mc.setMatchingWildcardMonitorList(wildcardMonitorList);      
      
    // Add the new consumer to the list in the row from the table of subscriptions
    if(matchedOnSelector)
    {     
      consumerList.add(mc);
    }
       
    // Finally, add this consumer to the appropriate places in the registered
    // consumer monitors tables
    _consumerMonitorRegistrar.
      addConsumerToRegisteredMonitors(mc,exactMonitorList,wildcardMonitorList);

    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "addConsumerForKnownTopicExpr");

  }

  /** 
   * Method addConsumerForNewTopicExpr
   * 
   * A monitored consumer is being added for a new topic expression. 
   * 
   * This method walks the registered monitor tables seeing which monitors match
   * the supplied consumer's topic expression.
   * 
   * @param consumerList list comprising the new consumer only
   * @param mc the consumer being added.
   * @param topicExpression the topic expression associated with the new consumer
   * @param isWildcarded 
   * @exception SIDiscriminatorSyntaxException if the topic syntax is invalid 
   */
  private void addConsumerForNewTopicExpr(
    String topicExpression,
    boolean isWildcarded,
    MonitoredConsumer mc)
  throws SIDiscriminatorSyntaxException
  {
    if (tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "addConsumerForNewTopicExpr",
          new Object[] { topicExpression, 
                         new Boolean(isWildcarded),
                         mc }); 

    // A consumer is being added on a new topic expression
    if (tc.isDebugEnabled())
        SibTr.debug(tc, "Add new reference");       
      
    // Now we walk the registered monitor tables seeing which monitors match
    // this consumer's topic expression
    if(isWildcarded)
    {
      // Use MatchSpace direct evaluation code once we've isolated candidate
      // expressions through string matching
      _consumerMonitorRegistrar.matchNewWildcardConsumerToMonitors(topicExpression, mc);                   
    }         
    else
    {
      // Exact topicExpression in subscription case
      _consumerMonitorRegistrar.matchNewExactConsumerToMonitors(topicExpression, mc);                  
    }
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "addConsumerForNewTopicExpr");
  }


  /**
   * @return
   */
  public ConsumerMonitorRegistrar getConsumerMonitorRegistrar()
  {
    if (tc.isEntryEnabled()) 
    {
      SibTr.entry(tc, "getConsumerMonitorRegistrar");
      SibTr.exit(tc, "getConsumerMonitorRegistrar", _consumerMonitorRegistrar);	
    }
    return _consumerMonitorRegistrar;
  }

  /**
   * @return
   */
  public SubscriptionRegistrar getSubscriptionRegistrar()
  {
    if (tc.isEntryEnabled()) 
    {
      SibTr.entry(tc, "getSubscriptionRegistrar");
      SibTr.exit(tc, "getSubscriptionRegistrar", _subscriptionRegistrar);	
    }
    return _subscriptionRegistrar;
  }

}
