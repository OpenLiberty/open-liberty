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

/**
 * @author nyoung
 *
 */

package com.ibm.ws.sib.processor.matching;

// Import required classes.
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import com.ibm.ws.sib.matchspace.SearchResults;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.ControllableProxySubscription;
import com.ibm.ws.sib.processor.impl.PubSubOutputHandler;
import com.ibm.ws.sib.processor.matching.TopicAuthorization;

public class MessageProcessorSearchResults implements SearchResults
{
  // Standard trace boilerplate

  private static final TraceComponent tc =
    SibTr.register(
      MessageProcessorSearchResults.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  // Final results (this copy is never also in the cache, and so can be reused)
  protected Object[] finalResults;

  // Cached final results
  protected Object[] cachedResults;

  // Indicates that finalResults have been post-processed.
  protected boolean postProcessed = false;

  // List of PubSubOutputHandlers extracted from traversal results
  protected List matchingPSOHs;

  // Indicates that the list of PSOHs has been generated since a reset of these results.
  protected boolean generatedMatchingPSOHs = false;

  // Static table of handlers.  This can be expanded as needed.
  protected static final Handler[] handlers =
    new Handler[MessageProcessorMatchTarget.NUM_TYPES];
  static {
    handlers[MessageProcessorMatchTarget.JS_SUBSCRIPTION_TYPE] =
      new SubscriptionFlexHandler();
    //(Handler) cls.newInstance();
    handlers[MessageProcessorMatchTarget.JS_CONSUMER_TYPE] =
      new ConsumerFlexHandler();

    handlers[MessageProcessorMatchTarget.JS_NEIGHBOUR_TYPE] =
      new NeighbourFlexHandler();

    handlers[MessageProcessorMatchTarget.ACL_TYPE] =
      new TopicAclFlexHandler();

    handlers[MessageProcessorMatchTarget.APPLICATION_SIG_TYPE] =
      new ApplicationSignatureFlexHandler();
  }

  // Reference to the topicspace associated with this search. This is needed for
  // security checks
  private DestinationHandler destinationHandler;

  // Constructor
  public MessageProcessorSearchResults(TopicAuthorization authorization)
  {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "MessageProcessorSearchResults", authorization);

    finalResults = new Object[MessageProcessorMatchTarget.NUM_TYPES];
    for (int i = 0; i < MessageProcessorMatchTarget.NUM_TYPES; i++)
    {
      if (handlers[i] != null)
      {
        finalResults[i] = handlers[i].initResult();
        handlers[i].setAuthorization(authorization);
      }
      else
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(
            tc,
            "MessageProcessorSearchResults",
            "No flex handler for "
              + MessageProcessorMatchTarget.TARGET_NAMES[i]);

        finalResults[i] = null;
      }
    }
    cachedResults = null;

    // reset reference to topicspace
    destinationHandler = null;

    // Initialise the PSOH list
    matchingPSOHs = new ArrayList();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "MessageProcessorSearchResults", this);

  }

  public void addObjects(List[] objs)
  {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "addObjects");

    if (objs.length > MessageProcessorMatchTarget.NUM_TYPES)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "addObjects", "IllegalStateException");
      throw new IllegalStateException();
    }
    for (int i = 0; i < objs.length; i++)
      if (objs[i] != null && handlers[i] != null)
        handlers[i].processIntermediateMatches(objs[i], finalResults[i]);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addObjects");

  }

  public void reset()
  {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "reset");

    if (finalResults != null)
    {
      for (int i = 0; i < MessageProcessorMatchTarget.NUM_TYPES; i++)
      {
        if (handlers[i] != null)
          handlers[i].resetResult(finalResults[i]);
      }
    }
    else
    {
      finalResults = new Object[MessageProcessorMatchTarget.NUM_TYPES];
      for (int i = 0; i < MessageProcessorMatchTarget.NUM_TYPES; i++)
      {
        if (handlers[i] != null)
          finalResults[i] = handlers[i].initResult();
        else
          finalResults[i] = null;
      }
    }
    cachedResults = null;
    postProcessed = false;

    // reset reference to topicspace
    destinationHandler = null;

    // clear the PSOHs list
    matchingPSOHs.clear();
    generatedMatchingPSOHs = false;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "reset");

  }

  public Object provideCacheable(Object rootVal)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "provideCacheable", rootVal);

    postProcess((String) rootVal);
    cachedResults = finalResults;
    finalResults = null;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "provideCacheable", cachedResults);

    return cachedResults;
  }

  public boolean acceptCacheable(Object cached)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "acceptCacheable", cached);
    boolean result = false;
    if (cached instanceof Object[])
    {
      cachedResults = (Object[]) cached;
      result = true;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "acceptCacheable", "result: " + new Boolean(result));

    return result;
  }

  // This method is called after MatchSpace traversal.  It returns the result vector and
  // also completes the processing (if not already done before caching) by calling all the
  // postProcessMatches methods of the handlers.

  public Object[] getResults(String topic)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getResults", topic);

    if (cachedResults != null)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getResults", cachedResults);
      return cachedResults;
    }
    postProcess(topic);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getResults", "finalResults: " + Arrays.toString(finalResults));
    return finalResults;
  }

  public Set getConsumerDispatchers(String topic)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getConsumerDispatchers", topic);

    Object allResults[] = getResults(topic);

    Set matchingConsumerDispatchers =
      (Set)allResults[MessageProcessorMatchTarget.JS_SUBSCRIPTION_TYPE];

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getConsumerDispatchers", matchingConsumerDispatchers);

    return matchingConsumerDispatchers;
  }

  public List getPubSubOutputHandlers(String topic)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getPubSubOutputHandlers", topic);

    // ensure that the PubSubOutputHandlers have been generated
    if (!generatedMatchingPSOHs)
    {
      Object allResults[] = getResults(topic);

      generateMatchingPSOHs(allResults);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getPubSubOutputHandlers", matchingPSOHs);

    return matchingPSOHs;
  }

  protected void postProcess(String topic)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "postProcess", topic);

    if (!postProcessed)
    {
      for (int i = 0; i < finalResults.length; i++)
      {
        if (handlers[i] != null)
        {
          handlers[i].postProcessMatches(destinationHandler,
                                         topic,
                                         finalResults,
                                         i);
        }
      }

      // Set the postprocessed flag
      postProcessed = true;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "postProcess");
  }

  protected void postProcess(int matchType, String topic)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "postProcess", new Object[]{new Integer(matchType), topic});

    handlers[matchType].postProcessMatches(destinationHandler,
                                           topic,
                                           finalResults,
                                           matchType);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "postProcess");
  }

  protected void generateMatchingPSOHs(Object[] theResults)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "generateMatchingPSOHs");

    if (!generatedMatchingPSOHs)
    {
      // Extract the wrappered PSOHs
      Set wrappedOutputHandlers =
        (Set)theResults[MessageProcessorMatchTarget.JS_NEIGHBOUR_TYPE];

      Iterator i = wrappedOutputHandlers.iterator();
      while (i.hasNext())
      {
        PubSubOutputHandler psoh = (PubSubOutputHandler)((ControllableProxySubscription)(i.next())).getOutputHandler();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "Found pubsuboutputhandler: " + psoh);

        // Add it to the matching list
        matchingPSOHs.add(psoh);
      }
      // Set the postprocessed flag
      generatedMatchingPSOHs = true;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "generateMatchingPSOHs");
  }

  /**
   * @param set a reference to the TopicSpace
   */
  public void setTopicSpace(DestinationHandler dh)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setTopicSpace", dh);

    destinationHandler = dh;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setTopicSpace");
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString()
  {
    StringBuffer buffer = new StringBuffer();
    if(finalResults != null)
    {
      buffer.append("final: ");
      for (int i = 0; i < MessageProcessorMatchTarget.NUM_TYPES; i++)
      {
        if (handlers[i] != null)
          buffer.append(handlers[i].toString(finalResults, i));
      }
    }
    else if(cachedResults != null)
    {
      buffer.append("cached: ");
      for (int i = 0; i < MessageProcessorMatchTarget.NUM_TYPES; i++)
      {
        if (handlers[i] != null)
          buffer.append(handlers[i].toString(cachedResults, i));
      }
    }
    return buffer.toString();
  }

  public static interface Handler
  {
    /** Create a result object of the appropriate class for this handler.
     *
     * @return a result object of the appropriate type
     */
    Object initResult();

    /** Reset a result object of the appropriate class for this handler
     *  to permit its re-use by another invocation of MessageProcessor.
     *
     * @param result the result object to reset
     */
    void resetResult(Object result);

    /** Accumulate intermediate results for this handler while traversing
     *  MatchSpace.
     *
     * @param targets vector of MatchTarget to process
     * @param result result object provided by the initResult method of this particular
     * handler
     */
    void processIntermediateMatches(List targets, Object result);

    /** Complete processing of results for this handler after completely traversing
     *  MatchSpace.
     *
     * @param topicSpace The topicSpace against which the search is being done
     * @param topic The topic in the message being processed
     * @param results Vector of results for all handlers; results of MatchTarget types
     * whose index is less than that of this MatchTarget type have already been postprocessed.
     * @param index Index in results vector of accumulated results of this MatchTarget
     * type
     */
    void postProcessMatches(DestinationHandler topicSpace,
                            String topic,
                            Object[] results,
                            int index);

    /** Complete processing of results for this handler after completely traversing
     *  MatchSpace.
     *
     *  ACL checking takes place at this point.
     *
     * @param results Vector of results for all handlers; results of MatchTarget types
     * whose index is less than that of this MatchTarget type have already been postprocessed.
     * @param index Index in results vector of accumulated results of this MatchTarget
     * type
     */

    void setAuthorization(TopicAuthorization authorization);

    /** Here in EventBroker we had the distributeMessage method as follows,
     *
     *      public boolean distributeMessage(RoutableMessage msg,
     *                          MPScratchPad scratch,
     *                          MessagePathway receivingConnection,
     *                          boolean fromClient,
     *                          SSConnMgr connMgr,
     *                          Object result,
     *                          boolean doClient)
     *
     *
     * This allowed us to establish the persistence of the delivered message, convert cross-stream
     * messages to an alternate message format. Delivery time security checks were done
     * as part of postProcessMatches.
     *
     * In the latest disthub this method was replaced by the MessageDistributor class with its
     * associated preMatch and postMatch calls, introduced to support GD.
     */

    /** Return info about the data managed by this handler.
     *
     * @return string
     */
    String toString(Object results[], int index);
  }

}
