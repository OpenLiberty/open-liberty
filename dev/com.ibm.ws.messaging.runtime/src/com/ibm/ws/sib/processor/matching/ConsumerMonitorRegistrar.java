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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.wsspi.sib.core.ConsumerSetChangeCallback;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.ConnectionImpl;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;

/**
 * This class manages the set of consumer monitors that have been registered
 * with the system.
 *
 * The class was provided to support WSN Registered publishers.
 */
public class ConsumerMonitorRegistrar
{
  private static final TraceComponent tc =
    SibTr.register(
      ConsumerMonitorRegistrar.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  // Back references to the messageprocessor and mpm
  private MessageProcessor _messageProcessor;
  private MessageProcessorMatching _mpm;

  // Maps of registered consumer monitors
  private Map _registeredExactConsumerMonitors = new HashMap();
  private Map _registeredWildcardConsumerMonitors = new HashMap();

  // The callback index is keyed on callback object and allows us to
  // retrieve the topicexpression against which it was registered (specifically
  // when deregistering consumer set monitors)
  private Map _callbackIndex = new HashMap();

  private class RegisteredCallbacks
  {
    private ArrayList wrappedCallbacks;
    private ArrayList matchingConsumers;

    RegisteredCallbacks(ArrayList wrappedCallbacks,
                       ArrayList matchingConsumers)
    {
      this.wrappedCallbacks = wrappedCallbacks;
      this.matchingConsumers = matchingConsumers;
    }

    public ArrayList getWrappedCallbacks()
    {
      return wrappedCallbacks;
    }

    public ArrayList getMatchingConsumers()
    {
      return matchingConsumers;
    }
  }

  /**
   * TopicRecords are stored in the _callbackIndex and are used to help locate
   * appropriate registrar table entries using a key of callback.
   */
  private class TopicRecord
  {
    public String topicExpression;
    public boolean isWildcarded;

    TopicRecord(String topicExpression, boolean isWildcarded)
    {
      this.topicExpression = topicExpression;
      this.isWildcarded = isWildcarded;
    }
  }

  //------------------------------------------------------------------------------
  // Constructors for ConsumerMonitorRegistrar
  //------------------------------------------------------------------------------

  public ConsumerMonitorRegistrar(MessageProcessor messageProcessor,
                                  MessageProcessorMatching mpm)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "ConsumerMonitorRegistrar", new Object[]{ messageProcessor, mpm});

    // Set ref to messageProcessor and mpm
    _messageProcessor = messageProcessor;
    _mpm = mpm;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "ConsumerMonitorRegistrar", this);
  }

  /**
   * Method registerCallbackOnNewExpression
   *
   * Adds a list of matching consumers in a new entry to the appropriate wildcard
   * or exact expression table.
   *
   * @param topicExpression
   * @param isWildcarded
   * @param callback
   * @param matchingConsumers
   * @return
   */
  public boolean registerCallbackOnNewExpression(
    ConnectionImpl connection,
    String topicExpression,
    boolean isWildcarded,
    ConsumerSetChangeCallback callback,
    ArrayList matchingConsumers)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "registerCallbackOnNewExpression",
          new Object[] { connection,
                         topicExpression,
                         new Boolean(isWildcarded),
                         callback,
                         matchingConsumers});

    boolean areConsumers = false;

    // Add the new callback to the index, so we can find it when we deregister
    addCallbackToConnectionIndex(connection, topicExpression, isWildcarded, callback);

    // Set up the callback list for this registration
    ArrayList callbackList = new ArrayList();
    WrappedConsumerSetChangeCallback wcb = new WrappedConsumerSetChangeCallback(callback);
    callbackList.add(wcb);

    // Register the monitor in the appropriate table
    RegisteredCallbacks regMonitor =
      new RegisteredCallbacks(callbackList, matchingConsumers);

    if(isWildcarded)
      _registeredWildcardConsumerMonitors.put(topicExpression, regMonitor);
    else
      _registeredExactConsumerMonitors.put(topicExpression, regMonitor);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "registerCallbackOnNewExpression", new Boolean(areConsumers));
    return areConsumers;
  }

  /**
   * Method deregisterMonitor
   *
   * Deregisters a previously registered callback.
   *
   * @param callback
   * @throws SINotPossibleInCurrentConfigurationException
   */
  public void deregisterMonitor(ConnectionImpl connection,
                                ConsumerSetChangeCallback callback)
  throws SINotPossibleInCurrentConfigurationException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "deregisterMonitor",
          new Object[] { connection, callback});

    // Retrieve the callback from the index
    TopicRecord tRecord = retrieveCallbackFromConnectionIndex(connection, callback);

    if(tRecord != null)
    {
      // Have found the callback in the index, now remove the callback from
      // the appropriate registered monitors table.
      if(tRecord.isWildcarded)
      {
        removeCallbackFromRegisteredMonitors(callback,
                                             tRecord.topicExpression,
                                             true, // isWildcarded
                                             _registeredWildcardConsumerMonitors);
      }
      else
      {
        // Handle the non-wildcard case
        removeCallbackFromRegisteredMonitors(callback,
                                             tRecord.topicExpression,
                                             false, // isWildcarded
                                             _registeredExactConsumerMonitors);
      }
    }
    else
    {
      // Failed to find any reference to callback
      // Build the message for the Exception
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "deregisterMonitor", callback);

      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0005",
        new Object[] {
          "com.ibm.ws.sib.processor.matching.ConsumerMonitorRegistrar",
          "1:237:1.10",
          callback });
      throw new SINotPossibleInCurrentConfigurationException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0005",
          new Object[] {
            "com.ibm.ws.sib.processor.matching.ConsumerMonitorRegistrar",
            "1:244:1.10",
            callback },
          null));

    }

    // Now remove the callback from the index
    removeCallbackFromConnectionIndex(connection, callback);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "deregisterMonitor");
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
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "removeConsumerSetMonitors", new Object[] { connection });

    // Locate the connection in the callback index
    if(_callbackIndex.containsKey(connection))
    {
      // Have found registered callbacks for this connection
      Map connMap = (HashMap)_callbackIndex.get(connection);

      // Iterate through the map removing individual callbacks
      Iterator i = connMap.entrySet().iterator();
      while (i.hasNext())
      {
        Map.Entry entry = (Map.Entry)i.next();
        ConsumerSetChangeCallback callback = (ConsumerSetChangeCallback)entry.getKey();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "Found registered callback: " + callback);

        // Locate the TopicRecord for the callback
        TopicRecord tRecord = (TopicRecord)entry.getValue();
        if(tRecord != null)
        {
          // Have found the callback in the index, now remove the callback from
          // the appropriate registered monitors table.
          if(tRecord.isWildcarded)
          {
            removeCallbackFromRegisteredMonitors(callback,
                                                 tRecord.topicExpression,
                                                 true, // isWildcarded
                                                 _registeredWildcardConsumerMonitors);
          }
          else
          {
            // Handle the non-wildcard case
            removeCallbackFromRegisteredMonitors(callback,
                                                 tRecord.topicExpression,
                                                 false, // isWildcarded
                                                 _registeredExactConsumerMonitors);
          }
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeConsumerSetMonitors");
  }

  /**
   * Method removeCallbackFromRegisteredMonitors
   *
   * Removes a previously registered callback from the appropriate map.
   *
   * Also walks through the list of matching consumers removing references to this
   * registration.
   *
   * @param callback
   * @param topicExpression
   * @param isWildcarded
   * @param registeredMonitorsMap
   * @throws SINotPossibleInCurrentConfigurationException
   * @throws SIErrorException
   */
  public void removeCallbackFromRegisteredMonitors(
    ConsumerSetChangeCallback callback,
    String topicExpression,
    boolean isWildcarded,
    Map registeredMonitorsMap)
  throws SINotPossibleInCurrentConfigurationException,
         SIErrorException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "removeCallbackFromRegisteredMonitors",
          new Object[] { callback,
                         topicExpression,
                         new Boolean(isWildcarded)});

    ArrayList matchingConsumerList = null;

    RegisteredCallbacks rMonitor = (RegisteredCallbacks)registeredMonitorsMap.get(topicExpression);

    //Handle the case where we cannot find the monitor
    if(rMonitor != null)
    {
      // Have located the monitor record
      // Get the list of callbacks prepare to remove this one
      ArrayList callbackList = rMonitor.getWrappedCallbacks();
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Found existing entry with callbacks: " + callbackList);
      WrappedConsumerSetChangeCallback wcb = new WrappedConsumerSetChangeCallback(callback);
      boolean cbRemoved = callbackList.remove(wcb);
      if(!cbRemoved)
      {
        //Couldn't find the callback. This means we have an internal inconsistency
        // in our tables, so throw an SIErrorException

        // Build the message for the Exception
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "removeCallbackFromRegisteredMonitors", callback);

        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0005",
          new Object[] {
            "com.ibm.ws.sib.processor.matching.ConsumerMonitorRegistrar",
            "1:372:1.10",
            callback });
        throw new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0005",
            new Object[] {
              "com.ibm.ws.sib.processor.matching.ConsumerMonitorRegistrar",
              "1:379:1.10",
              callback },
            null));

      }

      // If the callbacklist is now empty, then we need to walk the matching
      // consumers, removing references and delete the record in the table
      if(callbackList.isEmpty())
      {
        // Get the list of matching consumers and remove the monitor references from
        // them.
        matchingConsumerList = rMonitor.getMatchingConsumers();
        // Are there consumers for this expression
        boolean areConsumers = !matchingConsumerList.isEmpty();

        if(areConsumers)
        {
          // Remove reference to this registration from each consumer
          Iterator i = matchingConsumerList.iterator();
          while (i.hasNext())
          {
            MonitoredConsumer mc = (MonitoredConsumer)i.next();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
              SibTr.debug(tc, "Remove registration from consumer: " + mc);

            // Remove the reference from the wildcarded list in the consumer
            boolean removed;
            if(isWildcarded)
              removed = mc.removeMatchingWildcardMonitor(topicExpression);
            else
              removed = mc.removeMatchingExactMonitor(topicExpression);

            if(!removed)
            {
              // Couldn't find the registration. This means we have an internal
              // inconsistency in our tables, so throw an SIErrorException

              // Build the message for the Exception
              if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "removeCallbackFromRegisteredMonitors", mc + ":" + topicExpression);

              SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0005",
                new Object[] {
                  "com.ibm.ws.sib.processor.matching.ConsumerMonitorRegistrar",
                  "1:425:1.10",
                  mc + ":" + topicExpression });
              throw new SIErrorException(
                nls.getFormattedMessage(
                  "INTERNAL_MESSAGING_ERROR_CWSIP0005",
                  new Object[] {
                    "com.ibm.ws.sib.processor.matching.ConsumerMonitorRegistrar",
                    "1:432:1.10",
                    mc + ":" + topicExpression },
                  null));
            }
          }
        }

        // Now remove the record from the table
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "No more callbacks for this topicexpression, delete record");

        registeredMonitorsMap.remove(topicExpression);
      }
    }
    else
    {
      // Failed to find the callback
      // Build the message for the Exception
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "removeCallbackFromRegisteredMonitors", callback);

      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0005",
        new Object[] {
          "com.ibm.ws.sib.processor.matching.ConsumerMonitorRegistrar",
          "1:456:1.10",
          callback });
      throw new SINotPossibleInCurrentConfigurationException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0005",
          new Object[] {
            "com.ibm.ws.sib.processor.matching.ConsumerMonitorRegistrar",
            "1:463:1.10",
            callback },
          null));
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeCallbackFromRegisteredMonitors");
  }

  /**
   * Method matchNewExactConsumerToMonitors
   *
   * This method searches through the consumer monitor tables looking for
   * monitor topic expressions that match the supplied consumer topic expression.
   *
   * This method is driven where the consumer's topic expression is fully qualified.
   * The style of matching differs between monitors themselves registered
   * n fully qualified topics and those that were registered with a wildcard.
   * We use string matching to match against the fully qualified monitors
   * and direct MatchSpace evaluation to match against wildcarded monitors.
   *
   * Where matches are found we check to see whether a callback needs to be
   * driven because there were previously no matches.
   *
   * @param topicExpression of the consumer
   * @param mc the consumer
   * @throws SIDiscriminatorSyntaxException
   */
  public void matchNewExactConsumerToMonitors(
    String topicExpression,
    MonitoredConsumer mc)
  throws SIDiscriminatorSyntaxException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "matchNewExactConsumerToMonitors",
          new Object[] { topicExpression,
                         mc });
    // We'll set any monitors that we find into the following lists which
    // will be added to the consumer itself. This will ease consumer removal
    // processing
    ArrayList exactMonitorList = new ArrayList();
    ArrayList wildcardMonitorList = new ArrayList();

    // Iterate through the map of monitors defined on exact topic expressions,
    // Note we can use string matching in this case
    Iterator i = _registeredExactConsumerMonitors.keySet().iterator();
    while (i.hasNext())
    {
      String registeredTopic = (String)i.next();

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Found registered topic: " + registeredTopic);

      // Process those registered topics that might match by virtue
      // of a common topic stem
      if(topicExpression.equals(registeredTopic))
      {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "topics match, Add consumer to list");

        addConsumerToMonitorList(registeredTopic,
                                 _registeredExactConsumerMonitors,
                                 mc,
                                 false); // exact

        // Finally add reference to the monitor to the list for the new consumer
        exactMonitorList.add(registeredTopic);
      }
    }

    // Iterate through the map of monitors defined on wildcard topic expressions
    i = _registeredWildcardConsumerMonitors.keySet().iterator();
    while (i.hasNext())
    {
      String registeredTopic = (String)i.next();
      // Retrieve the non-wildcarded stem
      String registeredTopicStem = _mpm.retrieveNonWildcardStem(registeredTopic);

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Found registered topic: " + registeredTopic + "with stem: " + registeredTopicStem);

      // Process those registered topics that might match by virtue
      // of a common topic stem
      if(topicExpression.startsWith(registeredTopicStem))
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "Drive direct evaluation for topic: " + registeredTopic);

        if(_mpm.evaluateDiscriminator(topicExpression,registeredTopic))
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Add consumer to list");

          addConsumerToMonitorList(registeredTopic,
                                   _registeredWildcardConsumerMonitors,
                                   mc,
                                   true); // wildcarded

          // Finally add reference to the monitor to the list for the new consumer
          wildcardMonitorList.add(registeredTopic);
        }
      }
    }

    // Finally add lists of references to the monitors into the new consumer
    mc.setMatchingExactMonitorList(exactMonitorList);
    mc.setMatchingWildcardMonitorList(wildcardMonitorList);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "matchNewExactConsumerToMonitors");

  }

  /**
   * Method matchNewWildcardConsumerToMonitors
   *
   * This method searches through the consumer monitor tables looking for
   * monitor topic expressions that match the supplied consumer topic expression.
   *
   * This method is driven where the consumer's topic expression is wildcarded.
   * The style of matching differs between monitors themselves registered with
   * a wildcard and those that were registered on fully qualified topics.
   * We can use direct MatchSpace evaluation to match against the fully qualified
   * monitors but the best that we can do with wildcarded monitor is to match
   * common non-wildcarded stems and conclude that the consumer "might" match
   * the monitor.
   *
   * Where matches are found we check to see whether a callback needs to be
   * driven because there were previously no matches.
   *
   * @param topicExpression of the consumer
   * @param mc the consumer
   * @throws SIDiscriminatorSyntaxException
   */
  public void matchNewWildcardConsumerToMonitors(
    String topicExpression,
    MonitoredConsumer mc)
  throws SIDiscriminatorSyntaxException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "matchNewWildcardConsumerToMonitors",
          new Object[] { topicExpression,
                         mc });
    // We'll set any monitors that we find into the following lists which
    // will be added to the consumer itself. This will ease consumer removal
    // processing
    ArrayList exactMonitorList = new ArrayList();
    ArrayList wildcardMonitorList = new ArrayList();

    // Use MatchSpace direct evaluation code once we've isolated candidate
    // expressions through string matching

    // Retrieve the non-wildcarded stem from the subscribers topic expression
    String stem = _mpm.retrieveNonWildcardStem(topicExpression);

    // Iterate through the map of monitors defined on exact topic expressions
    Iterator i = _registeredExactConsumerMonitors.keySet().iterator();
    while (i.hasNext())
    {
      String registeredTopic = (String)i.next();

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Found registered topic: " + registeredTopic);

      // Process those registered topics that might match by virtue
      // of a common topic stem
      if(registeredTopic.startsWith(stem))
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "Drive direct evaluation for topic: " + registeredTopic);

        if(_mpm.evaluateDiscriminator(registeredTopic,topicExpression))
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Add consumer to list");

          addConsumerToMonitorList(registeredTopic,
                                   _registeredExactConsumerMonitors,
                                   mc,
                                   false); // exact

          // Finally add reference to the monitor to the list for the new consumer
          exactMonitorList.add(registeredTopic);
        }
      }
    }

    // Iterate through the map of monitors defined on wildcard topic expressions
    i = _registeredWildcardConsumerMonitors.keySet().iterator();
    while (i.hasNext())
    {
      String registeredTopic = (String)i.next();
      // Retrieve the non-wildcarded stem
      String registeredTopicStem = _mpm.retrieveNonWildcardStem(registeredTopic);

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Found registered topic: " + registeredTopic + "with stem: " + registeredTopicStem);

      // Test candidates
      if(registeredTopicStem.length() >= stem.length())
      {
        if(registeredTopicStem.startsWith(stem))
        {
          // This is as far as we can get with such topic expressions, the
          // best that we can say is that there "might" be some overlap

          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Topic expressions might overlap, add consumer to list");

          addConsumerToMonitorList(registeredTopic,
                                   _registeredWildcardConsumerMonitors,
                                   mc,
                                   true); // wildcarded

          // Finally add reference to the monitor to the list for the new consumer
          wildcardMonitorList.add(registeredTopic);
        }
      }
      else
      {
        if(stem.startsWith(registeredTopicStem))
        {
          // This is as far as we can get with such topic expressions, the
          // best that we can say is that there "might" be some overlap
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Topic expressions might overlap, add consumer to list");

          addConsumerToMonitorList(registeredTopic,
                                   _registeredWildcardConsumerMonitors,
                                   mc,
                                   true); // wildcarded

          // Finally add reference to the monitor to the list for the new consumer
          wildcardMonitorList.add(registeredTopic);
        }
      }
    }

    // Finally add lists of references to the monitors into the new consumer
    mc.setMatchingExactMonitorList(exactMonitorList);
    mc.setMatchingWildcardMonitorList(wildcardMonitorList);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "matchNewWildcardConsumerToMonitors");
  }

  /**
   * Method addConsumerToRegisteredMonitors
   *
   * This method adds a new consumer to the appropriate places in each of the monitor
   * maps.
   *
   * @param mc
   * @param exactMonitorList
   * @param wildcardMonitorList
   */
  public void addConsumerToRegisteredMonitors(
    MonitoredConsumer mc,
    ArrayList exactMonitorList,
    ArrayList wildcardMonitorList)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "addConsumerToRegisteredMonitors",
          new Object[] { mc,
                         exactMonitorList,
                         wildcardMonitorList });
    // Add consumer to correct places in the maps
    addConsumerToRegisteredMonitorMap(mc, exactMonitorList, _registeredExactConsumerMonitors);

    // Now process the wildcard monitor list
    addConsumerToRegisteredMonitorMap(mc, wildcardMonitorList, _registeredWildcardConsumerMonitors);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addConsumerToRegisteredMonitors");

  }

  /**
   * Method addConsumerToRegisteredMonitorMap
   *
   * This method adds a new consumer to the appropriate places in a monitor
   * map.
   *
   * @param mc
   * @param monitorList
   * @param registeredMonitors
   */
  private void addConsumerToRegisteredMonitorMap(
    MonitoredConsumer mc,
    ArrayList monitorList,
    Map registeredMonitors)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "addConsumerToRegisteredMonitorMap",
          new Object[] { registeredMonitors,
                         mc,
                         monitorList });
    // Finally, add this consumer to the appropriate places in the registered
    // consumer monitors tables
    Iterator i = monitorList.iterator();
    while (i.hasNext())
    {
      String tExpr = (String)i.next();

      // Get the next monitor
      RegisteredCallbacks rMonitor = (RegisteredCallbacks)registeredMonitors.get(tExpr);

      // Retrieve the current list of consumers
      ArrayList matchingConsumerList = rMonitor.getMatchingConsumers();

      // There is no need to drive any callbacks as we are placing subscriptions
      // in the registrations table that correspond to existing subscriptions.
      // So simply add the new subscription to the list.
      matchingConsumerList.add(mc);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addConsumerToRegisteredMonitorMap");

  }

  /**
   * Method addConsumerToMonitorList
   *
   * This method adds a consumer to the list of consumers associated with
   * a topic expression in a registered monitor map.
   *
   * Checks to see whether a callback needs to be driven because there were
   * previously no matching consumers.
   *
   * @param registeredTopic
   * @param registeredMonitors
   * @param mc
   * @param isWildcarded
   */
  public void addConsumerToMonitorList(
    String registeredTopic,
    Map registeredMonitors,
    MonitoredConsumer mc,
    boolean isWildcarded)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "addConsumerToMonitorList",
          new Object[] { registeredTopic,
                         registeredMonitors,
                         mc,
                         new Boolean(isWildcarded) });

    RegisteredCallbacks rMonitor =
     (RegisteredCallbacks)registeredMonitors.get(registeredTopic);

    // Null monitor, really shouldn't happen, unless our locking model
    // has gone wrong (perhaps)
    if(rMonitor == null)
    {
      // Monitor map inconsistency
      // Build the message for the Exception
      String mapType = "exact";
      if(isWildcarded)
      {
        mapType = "wildcard";
      }

      // Build the message for the Exception
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "addConsumerToMonitorList", mapType + ":" + registeredTopic);

      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0005",
        new Object[] {
          "com.ibm.ws.sib.processor.matching.ConsumerMonitorRegistrar",
          "1:844:1.10",
          mapType + ":" + registeredTopic });
      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0005",
          new Object[] {
            "com.ibm.ws.sib.processor.matching.ConsumerMonitorRegistrar",
            "1:851:1.10",
            mapType + ":" + registeredTopic },
          null));
    }

    // Get the list of matching consumers
    ArrayList matchingConsumerList = rMonitor.getMatchingConsumers();

    // Handle the driving of the callbacks
    if(matchingConsumerList.isEmpty())
    {
      // This is a transition from 0 -> 1 consumer for this monitor.
      // Therefore we need to drive a callback
      driveRegisteredCallbacks(rMonitor, false);
    }

    // Add consumer to list
    matchingConsumerList.add(mc);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addConsumerToMonitorList");

  }

  /**
   * Method checkExistingExpression
   *
   * This method determines whether the ConsumerMonitorRegistrar has already
   * registered callbacks on a specified topicExpresson.
   *
   * @param topicExpression
   * @param isWildcarded
   * @return
   */
  public boolean checkExistingExpression(
    String topicExpression,
    boolean isWildcarded)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "checkExistingExpression",
          new Object[] {topicExpression, new Boolean(isWildcarded)});
    boolean alreadyRegistered = false;

    if(isWildcarded)
    {
      if(_registeredWildcardConsumerMonitors.containsKey(topicExpression))
        alreadyRegistered = true;
    }
    else
    {
      if(_registeredExactConsumerMonitors.containsKey(topicExpression))
        alreadyRegistered = true;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "checkExistingExpression", new Boolean(alreadyRegistered));

    return alreadyRegistered;
  }

  /**
   * Method registerCallbackOnExistingExpression
   *
   * Adds a new callback to an existing entry to the appropriate wildcard
   * or exact expression table.
   *
   * @param topicExpression
   * @param isWildcarded
   * @param callback
   * @return
   */
  public boolean registerCallbackOnExistingExpression(
    ConnectionImpl connection,
    String topicExpression,
    boolean isWildcarded,
    ConsumerSetChangeCallback callback)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "registerCallbackOnExistingExpression",
          new Object[] {connection, topicExpression, new Boolean(isWildcarded), callback });
    boolean areConsumers = false;
    RegisteredCallbacks rMonitor = null;

    // Add the callback to the index of callbacks
    addCallbackToConnectionIndex(connection, topicExpression, isWildcarded, callback);

    if(isWildcarded)
      rMonitor = (RegisteredCallbacks)_registeredWildcardConsumerMonitors.get(topicExpression);
    else
      rMonitor = (RegisteredCallbacks)_registeredExactConsumerMonitors.get(topicExpression);

    // Get the list of callbacks and add the new one
    ArrayList callbackList = rMonitor.getWrappedCallbacks();
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc, "Found existing entry with callbacks: " + callbackList);
    WrappedConsumerSetChangeCallback wcb = new WrappedConsumerSetChangeCallback(callback);
    
    //F011127
    //When we call the registerconsumermonitor() more than once with the same parameter,
    //there will be duplicate of callback in callbacklist.To overcome this leak we are checking before adding into the callbacklist
    Iterator it = callbackList.iterator();
    boolean alreadyExisting = false;
    while(it.hasNext()){
    	WrappedConsumerSetChangeCallback tmpwcb = (WrappedConsumerSetChangeCallback)it.next();
    	if(tmpwcb.equals(wcb)){
    		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
    		      SibTr.debug(tc, "The same callback is already registerd for the topic expression :"+topicExpression+" Hence registration will not be done!");
    		alreadyExisting = true;
    		break;
    	}
    }
    if(!alreadyExisting)
        callbackList.add(wcb);

    // Get the list of matching consumers
    ArrayList consumerList = rMonitor.getMatchingConsumers();
    // Are there consumers for this expression
    areConsumers = !consumerList.isEmpty();

    // Don't need to adjust the references in the consumer list as they will
    // already have a reference to this topic expression

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "registerCallbackOnExistingExpression", new Boolean(areConsumers));

    return areConsumers;
  }

  /**
   * Method removeConsumerFromRegisteredMonitors
   *
   * Removes a specified MonitoredConsumer from the appropriate places in the maps.
   *
   * @param mc
   * @param exactMonitorList
   * @param wildcardMonitorList
   */
  public void removeConsumerFromRegisteredMonitors(
    MonitoredConsumer mc,
    ArrayList exactMonitorList,
    ArrayList wildcardMonitorList)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "removeConsumerFromRegisteredMonitors",
          new Object[] { mc,
                         exactMonitorList,
                         wildcardMonitorList });
    // Remove consumer from correct places in the maps
    removeConsumerFromRegisteredMonitorMap(mc, exactMonitorList, _registeredExactConsumerMonitors);

    // Now process the wildcard monitor list
    removeConsumerFromRegisteredMonitorMap(mc, wildcardMonitorList, _registeredWildcardConsumerMonitors);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeConsumerFromRegisteredMonitors");

  }

  /**
   * Method removeConsumerFromRegisteredMonitorMap
   *
   * Removes a specified MonitoredConsumer from the appropriate places in the specified
   * monitors map.
   *
   * If  the number of consumers for a particular topic expression falls to zero then
   * the associated monitor callbacks will be driven.
   *
   * @param mc
   * @param monitorList
   * @param registeredMonitors
   */
  private void removeConsumerFromRegisteredMonitorMap(
    MonitoredConsumer mc,
    ArrayList monitorList,
    Map registeredMonitors)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "removeConsumerFromRegisteredMonitorMap",
          new Object[] { registeredMonitors,
                         mc,
                         monitorList });
    // Finally, remove this consumer from the appropriate places in the registered
    // consumer monitors tables
    Iterator i = monitorList.iterator();
    while (i.hasNext())
    {
      String tExpr = (String)i.next();
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Remove registration entry for consumer at: " + tExpr);
      // Get the next monitor
      RegisteredCallbacks rMonitor = (RegisteredCallbacks)registeredMonitors.get(tExpr);

      // Retrieve the current list of consumers
      ArrayList matchingConsumerList = rMonitor.getMatchingConsumers();

      boolean removed = matchingConsumerList.remove(mc);
      if(!removed)
      {
        // Couldn't find the consumer. This means we have an internal
        // inconsistency in our tables, so throw an SIErrorException

        // Build the message for the Exception
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "removeConsumerFromRegisteredMonitorMap", mc + ":" + tExpr);

        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0005",
          new Object[] {
            "com.ibm.ws.sib.processor.matching.ConsumerMonitorRegistrar",
            "1:1067:1.10",
            mc + ":" + tExpr });
        throw new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0005",
            new Object[] {
              "com.ibm.ws.sib.processor.matching.ConsumerMonitorRegistrar",
              "1:1074:1.10",
              mc + ":" + tExpr },
            null));
      }
      // Handle the driving of the callbacks
      if(matchingConsumerList.isEmpty())
      {
        // This is a transition from 1 -> 0 consumer for this monitor.
        // Therefore we need to drive a callback

        driveRegisteredCallbacks(rMonitor, true);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeConsumerFromRegisteredMonitorMap");

  }

  /**
   * Method driveRegisteredCallbacks
   *
   * Iterate over a set of callbacks, driving each on a separate thread.
   *
   * @param rMonitor
   * @param isEmpty
   */
  private void driveRegisteredCallbacks(
    RegisteredCallbacks rMonitor,
    boolean isEmpty)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "driveRegisteredCallbacks",
          new Object[] { rMonitor, new Boolean(isEmpty) });

    // Get the list of callbacks
    ArrayList callbackList = rMonitor.getWrappedCallbacks();
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc, "Iterate over wrapped callbacks");
    // Iterate over the callbacks
    Iterator iter = callbackList.iterator();
    while (iter.hasNext())
    {
      WrappedConsumerSetChangeCallback wcb = (WrappedConsumerSetChangeCallback)iter.next();

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Drive callback: " + wcb.getCallback());

      // set the type of transition into the wrapped callback
      wcb.transitionEvent(isEmpty);

      try
      {
        //start up a new thread (from the MP's thread pool)
        //to drive the callback
        _messageProcessor.startNewThread(new AsynchThread(wcb));
      }
      catch (InterruptedException e)
      {
        // No FFDC code needed

        //Trace only
        SibTr.exception(tc, e);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "driveRegisteredCallbacks");

  }

  /**
   * Method addCallbackToConnectionIndex
   *
   * Adds a new callback to the callback index.
   *
   * @param topicExpression
   * @param isWildcarded
   * @param callback
   * @return
   */
  public void addCallbackToConnectionIndex(
    ConnectionImpl connection,
    String topicExpression,
    boolean isWildcarded,
    ConsumerSetChangeCallback callback)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "addCallbackToConnectionIndex",
          new Object[] {connection, topicExpression, new Boolean(isWildcarded), callback });

    // Map of callbacks-to-expressions for a connection
    Map connMap = null;
    if(_callbackIndex.containsKey(connection))
    {
      // Already have registered callbacks for this connection
      connMap = (HashMap)_callbackIndex.get(connection);
    }
    else
    {
      // No registered callbacks for this connection
      connMap = new HashMap();
      _callbackIndex.put(connection,connMap);
    }

    // Add the new callback to the index, so we can find it when we deregister
    TopicRecord tRecord = new TopicRecord(topicExpression, isWildcarded);
    connMap.put(callback, tRecord);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addCallbackToConnectionIndex");

  }

  /**
   * Method retrieveCallbackFromConnectionIndex
   *
   * Retrieves an existing callback from the callback index.
   *
   * @param topicExpression
   * @param isWildcarded
   * @param callback
   * @return
   */
  public TopicRecord retrieveCallbackFromConnectionIndex(
    ConnectionImpl connection,
    ConsumerSetChangeCallback callback)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "retrieveCallbackFromConnectionIndex",
          new Object[] {connection, callback });

    TopicRecord tRecord = null;

    if(_callbackIndex.containsKey(connection))
    {
      // Have found registered callbacks for this connection
      Map connMap = (HashMap)_callbackIndex.get(connection);

      // Locate the specific callback
      tRecord = (TopicRecord)connMap.get(callback);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "retrieveCallbackFromConnectionIndex", tRecord);

    return tRecord;
  }

  /**
   * Method removeCallbackFromConnectionIndex
   *
   * Removes an existing callback from the callback index.
   *
   * @param topicExpression
   * @param isWildcarded
   * @param callback
   * @return
   */
  public TopicRecord removeCallbackFromConnectionIndex(
    ConnectionImpl connection,
    ConsumerSetChangeCallback callback)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "removeCallbackFromConnectionIndex",
          new Object[] {connection, callback });

    TopicRecord tRecord = null;

    if(_callbackIndex.containsKey(connection))
    {
      // Have found registered callbacks for this connection
      Map connMap = (HashMap)_callbackIndex.get(connection);

      // Locate the specific callback
      tRecord = (TopicRecord)connMap.remove(callback);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeCallbackFromConnectionIndex", tRecord);

    return tRecord;
  }

  /**
   * This class implements Runnable so that we can easily call it in a new Thread.
   * It's purpose is to call the ConsumerSetChangeCallback code inside a separate
   * Thread.
   *
   * @author nyoung
   */
  private class AsynchThread implements Runnable
  {

    private WrappedConsumerSetChangeCallback _wcb;
    /**
     * Create a new AsynchThread
     *
     * @param lcp The 'owning' LCP
     * @param isolatedRun true if this should be an isolated run :-o
     */
    AsynchThread(WrappedConsumerSetChangeCallback wcb)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "AsynchThread", new Object[]{ wcb});

      _wcb = wcb;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "AsynchThread", this);
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run()
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "run", this);

      // Call the wrapped consumerSetChange method which handles synchronization
      _wcb.consumerSetChange();

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "run");
    }
  }

  /**
   * Methods for integrity checks - used in unit testing
   *
   */
  public int getExactSubsSize(String topicExpression)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getExactSubsSize", topicExpression);
    int numSubs = 0;
    // Test whether table is now empty
    if(!_registeredExactConsumerMonitors.isEmpty())
    {
      if(_registeredExactConsumerMonitors.containsKey(topicExpression))
      {
        RegisteredCallbacks rMonitor =
         (RegisteredCallbacks)_registeredExactConsumerMonitors.get(topicExpression);

        // Get the list of matching consumers
        ArrayList consumerList = rMonitor.getMatchingConsumers();

        if(consumerList != null && !consumerList.isEmpty())
        {
          numSubs = consumerList.size();
        }
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getExactSubsSize", new Integer(numSubs));
   return  numSubs;
  }

  public int getWildcardSubsSize(String topicExpression)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getWildcardSubsSize", topicExpression);
    int numSubs = 0;
    // Test whether table is now empty
    if(!_registeredWildcardConsumerMonitors.isEmpty())
    {
      if(_registeredWildcardConsumerMonitors.containsKey(topicExpression))
      {
        RegisteredCallbacks rMonitor =
         (RegisteredCallbacks)_registeredWildcardConsumerMonitors.get(topicExpression);

        // Get the list of matching consumers
        ArrayList consumerList = rMonitor.getMatchingConsumers();

        if(consumerList != null && !consumerList.isEmpty())
        {
          numSubs = consumerList.size();
        }
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getWildcardSubsSize", new Integer(numSubs));
   return  numSubs;
  }

  public ArrayList getExactMonitorList(String topicExpression)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getExactMonitorList", topicExpression);
    ArrayList consumerList = null;
    // Test whether table is now empty
    if(!_registeredExactConsumerMonitors.isEmpty())
    {
      if(_registeredExactConsumerMonitors.containsKey(topicExpression))
      {
        RegisteredCallbacks rMonitor =
         (RegisteredCallbacks)_registeredExactConsumerMonitors.get(topicExpression);

        // Get the list of matching consumers
        consumerList = rMonitor.getMatchingConsumers();
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getExactMonitorList", consumerList);
   return  consumerList;
  }

  public ArrayList getWildcardMonitorList(String topicExpression)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getWildcardMonitorList", topicExpression);
    ArrayList consumerList = null;
    // Test whether table is now empty
    if(!_registeredWildcardConsumerMonitors.isEmpty())
    {
      if(_registeredWildcardConsumerMonitors.containsKey(topicExpression))
      {
        RegisteredCallbacks rMonitor =
         (RegisteredCallbacks)_registeredWildcardConsumerMonitors.get(topicExpression);

        // Get the list of matching consumers
        consumerList = rMonitor.getMatchingConsumers();
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getWildcardMonitorList", consumerList);
   return  consumerList;
  }

  public int getExactCallbackListSize(String topicExpression)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getExactCallbackListSize", topicExpression);
    int numCallbacks = 0;
    // Test whether table is now empty
    if(!_registeredExactConsumerMonitors.isEmpty())
    {
      if(_registeredExactConsumerMonitors.containsKey(topicExpression))
      {
        RegisteredCallbacks rMonitor =
         (RegisteredCallbacks)_registeredExactConsumerMonitors.get(topicExpression);

        // Get the list of matching callbacks
        ArrayList callbackList = rMonitor.getWrappedCallbacks();

        if(callbackList != null && !callbackList.isEmpty())
        {
          numCallbacks = callbackList.size();
        }
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getExactCallbackListSize", new Integer(numCallbacks));
   return  numCallbacks;
  }

  public int getWildcardCallbackListSize(String topicExpression)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getWildcardCallbackListSize", topicExpression);
    int numCallbacks = 0;
    // Test whether table is now empty
    if(!_registeredWildcardConsumerMonitors.isEmpty())
    {
      if(_registeredWildcardConsumerMonitors.containsKey(topicExpression))
      {
        RegisteredCallbacks rMonitor =
         (RegisteredCallbacks)_registeredWildcardConsumerMonitors.get(topicExpression);

        // Get the list of matching callbacks
        ArrayList callbackList = rMonitor.getWrappedCallbacks();

        if(callbackList != null && !callbackList.isEmpty())
        {
          numCallbacks = callbackList.size();
        }
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getWildcardCallbackListSize", new Integer(numCallbacks));
   return  numCallbacks;
  }
}
