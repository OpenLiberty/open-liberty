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

// Import required classes.
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.ws.sib.processor.SubscriptionDefinition;

final class SubscriptionDefinitionImpl implements SubscriptionDefinition
{
  private static TraceComponent tc =
    SibTr.register(
      SubscriptionDefinitionImpl.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  
  private String destination = null;
  private String topic = null;
  private String selector = null;
  private int selectorDomain = 0;
  private String user = null;
  private boolean noLocal = false;
  private boolean supportsMultipleConsumers = false;
  private String durableHome = null;
  private String targetDestination = null;

  /**
   * Constructor SubscriptionDefinitionImpl
   *
   * <p>Creates a subscription definition.</p>
   *
   * 
   */
  protected SubscriptionDefinitionImpl()
  {
  }

  /**
   * Constructor SubscriptionDefinitionImpl
   *
   * <p>Creates a subscription definition.</p>
   *
   * @param dest      The destination name
   * @param topic     The topic
   * @param selector  The selector
   * @param user      The user
   * @param noLocal   The noLocal flag
   */
  protected SubscriptionDefinitionImpl(
    String dest,
    String topic,
    String selector,
    int selectorDomain,
    String user,
    boolean noLocal,
    String durableHome)
  {
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, "SubscriptionDefinitionImpl", 
        new Object[]{dest, topic, selector, new Integer(selectorDomain), user, new Boolean(noLocal), durableHome});

    this.destination = dest;
    this.topic = topic;
    this.selector = selector;
    this.selectorDomain = selectorDomain;
    this.user = user;
    this.noLocal = noLocal;
    this.durableHome = durableHome;
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "SubscriptionDefinitionImpl", this);
  }

  /**
   * Returns the destination.
   * @return String
   */
  public String getDestination()
  {
    if (tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getDestination");
      SibTr.exit(tc, "getDestination", destination);
    }

    return destination;
  }

  /**
   * Returns the selector.
   * @return String
   */
  public String getSelector()
  {
    if (tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getSelector");
      SibTr.exit(tc, "getSelector", selector);
    }

    return selector;
  }
  
  /**
   * Returns the messaging domain in which the selector was sspecified.
   * @return int
   */
  public int getSelectorDomain()
  {
    if (tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getSelectorDomain");
      SibTr.exit(tc, "getSelectorDomain", new Integer(selectorDomain));
    }

    return selectorDomain;
  }  
  
  /**
   * Returns the topic.
   * @return String
   */
  public String getTopic()
  {
    if (tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getTopic");
      SibTr.exit(tc, "getTopic", topic);
    }

    return topic;
  }

  /**
   * Returns the user.
   * @return String
   */
  public String getUser()
  {
    if (tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getUser");
      SibTr.exit(tc, "getUser", user);
    }

    return user;
  }

  /**
   * Sets the destination.
   * @param destination The destination to set
   */
  public void setDestination(String destination)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "setDestination", destination);

    this.destination = destination;

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "setDestination");
  }

  /**
   * Sets the selector.
   * @param selector The selector to set
   */
  public void setSelector(String selector)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "setSelector", selector);

    this.selector = selector;

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "setSelector");
  }

  /**
   * Sets the topic.
   * @param topic The topic to set
   */
  public void setTopic(String topic)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "setTopic", topic);

    this.topic = topic;

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "setTopic");
  }

  /**
   * Sets the user.
   * @param user The user to set
   */
  public void setUser(String user)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "setUser", user);

    this.user = user;

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "setUser");
  }

  /**
   * Returns the noLocal.
   * @return boolean
   */
  public boolean isNoLocal()
  {
    if (tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isNoLocal");
      SibTr.exit(tc, "isNoLocal", new Boolean(noLocal));
    }

    return noLocal;
  }

  /**
   * Sets the noLocal.
   * @param noLocal The noLocal to set
   */
  public void setNoLocal(boolean noLocal)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "setNoLocal", new Boolean(noLocal));

    this.noLocal = noLocal;

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "setNoLocal");
  }

  /**
   * @see com.ibm.ws.sib.processor.SubscriptionDefinition#isSupportsMultipleConsumers()
   */
  public boolean isSupportsMultipleConsumers()
  {
    if (tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isSupportsMultipleConsumers");
      SibTr.exit(tc, "isSupportsMultipleConsumers", new Boolean(supportsMultipleConsumers));
    }
    
    return supportsMultipleConsumers;
  }

  /**
   * @see com.ibm.ws.sib.processor.SubscriptionDefinition#setSupportsMultipleConsumers(boolean)
   */
  public void setSupportsMultipleConsumers(boolean supportsMultipleConsumers)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "setSupportsMultipleConsumers", new Boolean(supportsMultipleConsumers));

    this.supportsMultipleConsumers = supportsMultipleConsumers;

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "setSupportsMultipleConsumers");
  }

  /**
   * @see com.ibm.ws.sib.processor.SubscriptionDefinition@getDurableHome(String)
   */
  public String getDurableHome() 
  {
    if (tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getDurableHome");
      SibTr.exit(tc, "getDurableHome", durableHome);
    }
    
    return durableHome;
  }
  
  /**
   * @see com.ibm.ws.sib.processor.SubscriptionDefinition@setDurableHome(String)
   */
  public void setDurableHome(String uuid)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "setDurableHome", uuid);
    durableHome = uuid;
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "setDurableHome");
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.SubscriptionDefinition#getTargetDestination(java.lang.String)
   */
  public String getTargetDestination() 
  {
    if (tc.isEntryEnabled())
    {
	   SibTr.entry(tc, "getTargetDestination");
       SibTr.exit(tc, "getTargetDestination", targetDestination);
    }	
	return targetDestination;
  }
	
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.SubscriptionDefinition#setTargetDestination(java.lang.String)
   */
  public void setTargetDestination(String targetDestination) 
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "setTargetDestination", targetDestination);

    this.targetDestination = targetDestination;

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "setTargetDestination");
  }

}
