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
package com.ibm.ws.sib.processor.impl.indexes;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.interfaces.ControllableSubscription;
import com.ibm.ws.sib.processor.utils.index.Index;
import com.ibm.ws.sib.processor.utils.index.IndexFilter;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Class to represent the various lookups for different subscription types
 */ 
public class SubscriptionIndex extends Index
{
  /** 
   * Trace for the component
   */
  private static final TraceComponent tc =
    SibTr.register(
      SubscriptionIndex.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);


  private int durableSubscriptions = 0;
  private int nonDurableSubscriptions = 0;
 
  public static class SubscriptionType extends Index.Type
  {
    //local or remote (proxy)
    public boolean local = true;
    public boolean durable = false;         
  }
  
  protected static class SubscriptionEntry extends Index.Entry
  {
    SubscriptionEntry(ControllableSubscription subscription, SubscriptionType type)
    {
      super(subscription.getSubscriptionUuid(), subscription, type);
    }
    
    ControllableSubscription getControllableSubscription()
    {
      return (ControllableSubscription) data;
    }
  }
  
  public SubscriptionIndex()
  {
    super();
  }
  
  public synchronized Entry put(ControllableSubscription subscription)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "put",
        new Object[] { subscription });

    SubscriptionType type = new SubscriptionType();
    type.durable = subscription.isDurable();
    type.local = subscription.isLocal();

    SubscriptionEntry entry = new SubscriptionEntry(subscription, type);
    add(entry);
    
    if(subscription.isDurable())
    {
      durableSubscriptions++;    
    }
    else
    {
      nonDurableSubscriptions++;
    }
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "put", entry);
      
    return entry;      
  }
  
  public synchronized void remove(ControllableSubscription subscription)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "remove", new Object[] { subscription });

    ControllableSubscription sub =
      (ControllableSubscription) remove(subscription.getSubscriptionUuid());
    if(sub != null)
    {
      if(sub.isDurable())
      {
        durableSubscriptions--;
      }
      else
      {
        nonDurableSubscriptions--;
      }
    }    

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "remove");
  }
    
  public synchronized ControllableSubscription findByUuid(SIBUuid12 uuid, IndexFilter filter)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "findByUuid", new Object[] { uuid, filter });
    
    ControllableSubscription controllableSubscription =
      (ControllableSubscription) get(uuid,filter);        
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "findByUuid", controllableSubscription);

    return controllableSubscription;
  }  
    
  public synchronized SubscriptionType getType(SIBUuid12 uuid)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "getType", new Object[] { uuid });

    SubscriptionType type = (SubscriptionType) super.getType(uuid).clone();  
          
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "getType", type);
  
    return type;
  }
  
  /**
   * Get number of durable subscriptions.
   * 
   * @return number of durable subscriptions.
   */
  public synchronized int getDurableSubscriptions()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "getDurableSubscriptions");

    if (tc.isEntryEnabled())
      SibTr.exit(
        tc,
        "getDurableSubscriptions",
        new Integer(durableSubscriptions));

    return durableSubscriptions;
  }

  /**
   * Get number of non-durable subscriptions.
   * 
   * @return number of non-durable subscriptions.
   */
  public synchronized int getNonDurableSubscriptions()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "getNonDurableSubscriptions");

    if (tc.isEntryEnabled())
      SibTr.exit(
        tc,
        "getNonDurableSubscriptions",
        new Integer(nonDurableSubscriptions));

    return nonDurableSubscriptions;
  }

  /**
   * Get total number of subscriptions.
   * 
   * @return total number of subscriptions.
   */
  public synchronized int getTotalSubscriptions()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "getTotalSubscriptions");

    int totalSubscriptions = durableSubscriptions + nonDurableSubscriptions;

    if (tc.isEntryEnabled())
      SibTr.exit(
        tc,
        "getTotalSubscriptions",
        new Integer(totalSubscriptions));

    return totalSubscriptions;
  }      
}
