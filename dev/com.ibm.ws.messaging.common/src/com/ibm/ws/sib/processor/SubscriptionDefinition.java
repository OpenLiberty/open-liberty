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
 
package com.ibm.ws.sib.processor;


public interface SubscriptionDefinition {
    
    /**
     * Returns the destination.
     * @return String
     */
    public String getDestination();

    /**
     * Returns the selector.
     * @return String
     */
    public String getSelector();
    
    /**
     * Returns the messaging domain in which the selector was sspecified.
     * @return int
     */
    public int getSelectorDomain();
    
    /**
     * Returns the topic.
     * @return String
     */
    public String getTopic();

    /**
     * Returns the user.
     * @return String
     */
    public String getUser();

    /**
     * Sets the destination.
     * @param destination The destination to set
     */
    public void setDestination(String destination);

    /**
     * Sets the selector.
     * @param selector The selector to set
     */
    public void setSelector(String selector);

    /**
     * Sets the topic.
     * @param topic The topic to set
     */
    public void setTopic(String topic);

    /**
     * Sets the user.
     * @param user The user to set
     */
    public void setUser(String user);
    
    /**
     * Returns the noLocal.
     * @return boolean
     */
    public boolean isNoLocal();
    
    /**
     * Sets the noLocal.
     * @param noLocal The noLocal to set
     */
    public void setNoLocal(boolean noLocal);

    /**
     * Returns the supportsMultipleConsumers.
     * @return boolean
     */
    public boolean isSupportsMultipleConsumers();
    
    /**
     * Sets the supportsMultipleConsumers.
     * @param supportsMultipleConsumers The supportsMultipleConsumers to set
     */
    public void setSupportsMultipleConsumers(boolean supportsMultipleConsumers);
    
    /**
     * If this is a durable subscription, then get the name of the ME which
     * hosts it.
     * 
     * @return String Name of the ME.
     */
    public String getDurableHome();

    /**
     * Set the name of the ME which hosts ths
     * durable subscription.
     * 
     * @param uuid The name of the home ME.
     */
    public void setDurableHome(String uuid);
    
    /**
     * Set the name of the target destination for 
     * an internal subscription
     * 
     * @param String name of the the target destination
     */
    public void setTargetDestination(String target);
    
    /**
     * Get the name of the target destination for 
     * an internal subscription
     * @return String
     */
    public String getTargetDestination();
  }
    


