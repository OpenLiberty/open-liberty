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
package com.ibm.ws.sib.processor.impl.store.filters;

// Import required classes.
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.Filter;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.ConsumerDispatcherState;
import com.ibm.ws.sib.processor.impl.store.itemstreams.DurableSubscriptionItemStream;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * SubscriptionStateFilter.
 * <p> This class is a general filter for searching for durable subscriptions
 * on any particular itemstream. The filter can be set to match on Subscription ID
 * or Destination Name. The filter type is set with the relevant method.
 * 
 * <p>A filter is needed to find a particular item on an itemstream. We do a get
 * and supply a filter. A matching item is returned.
 */

public final class SubscriptionStateFilter implements Filter
{
  private static final TraceComponent tc =
    SibTr.register(
      SubscriptionStateFilter.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

 
	/**
	 * The subscription id on which to filter items 
   */	
  private String _subscriptionId = null;
  
  /** 
   * A consumer dispatcher state object to filter on
   */
  private ConsumerDispatcherState _consumerDispatcherState = null;
  
  /** 
   * The destination name on which to filter items 
   */  
  private SIBUuid12 _destination = null;
  
  /**
   * setter to create a filter based on subscriptionId
   * 
   * @param subscriptionId
   */

  public void setSubscriptionIDFilter(String subscriptionId)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "setSubscriptionIDFilter", subscriptionId);

    this._subscriptionId = subscriptionId;
    this._destination = null;
    this._consumerDispatcherState = null;
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "setSubscriptionIDFilter");
  }

  /**
   * setter to create a filter based on destination name
   * 
   * @param destination The destination uuid
   */

  public void setDestinationFilter(SIBUuid12 destination)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "setDestinationFilter", destination);

    this._destination = destination;
    this._subscriptionId = null;
    this._consumerDispatcherState = null;
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "setDestinationFilter");
  }

  /**
   * setter to create a filter based on a specific consumer dispatcher state object
   * 
   * @param consumerDispatcherState
   */

  public void setConsumerDispatcherStateFilter(ConsumerDispatcherState consumerDispatcherState)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "setConsumerDispatcherStateFilter", consumerDispatcherState);

    this._consumerDispatcherState = consumerDispatcherState;
    this._subscriptionId = null;
    this._destination = null;
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "setConsumerDispatcherStateFilter");
  }

  /**
   * Callback method from messagestore
   * 
   * @param item to test for match
   */

  public boolean filterMatches(AbstractItem item)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "filterMatches", item);

		/* Cast the incoming item to a subscriptionState object. if it is not, an
		 * exception will be thrown and the match will fail */

    ConsumerDispatcherState subState = null;

    if (item instanceof DurableSubscriptionItemStream) 
    {
      try
      {
        subState = ((DurableSubscriptionItemStream) item).getConsumerDispatcherState();
      }
      catch (Exception e)
      {
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.store.filters.SubscriptionStateFilter.filterMatches",
          "1:180:1.33.1.1",
          this);

        SibTr.exception(tc, e);
        
        if (tc.isEntryEnabled())
          SibTr.exit(tc, "filterMatches", e);
          
        return false;
      }
  
  		/* If this filter is a subscriptionId filter, check for a match */
  		
      if (_subscriptionId != null)
      {
        boolean retval = (subState.getSubscriberID().equals(_subscriptionId));
        if (tc.isEntryEnabled())
          SibTr.exit(tc, "filterMatches", Boolean.valueOf(retval));
        return retval;
      }
  
  		/* If this filter is a destination name filter, check for a match */
  		
      if (_destination != null)
      {
        boolean retval = (subState.getTopicSpaceUuid().equals(_destination));
        if (tc.isEntryEnabled())
          SibTr.exit(tc, "filterMatches", Boolean.valueOf(retval));
        return retval;
      }
      
      /* If this filter is for a specific consumer dispatcher state object, check for a match */

      if (_consumerDispatcherState != null)
      {
        boolean retval = (subState == _consumerDispatcherState);
        if (tc.isEntryEnabled())
          SibTr.exit(tc, "filterMatches", Boolean.valueOf(retval));
        return retval;
      }

      //The match succeeds if its a durable subscription and no specific matching is requested
      if ((_destination == null) && (_subscriptionId == null) && (_consumerDispatcherState == null))
      {
        if (tc.isEntryEnabled())
          SibTr.exit(tc, "filterMatches", Boolean.TRUE);
        return true;
      }
    }
    /* If filter type has not been set, the match fails */
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "filterMatches", Boolean.FALSE);

    return false;

  }

}
