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

import java.util.List;

/**
 * This interface allows implementers to participate in the consumer monitoring
 * framework provided in order to support WSN Registered publishers.
 */
public interface MonitoredConsumer
{

  /**
   * setMonitored is not currently used, the assumption being that a consumer
   * that implements this interface will always be monitored. 
   */
  public void setMonitored();
  
  /**
   *  Method setWildcarded
   * 
   * Set a flag to signify that this subscription is wildcarded.
   */  
  public void setWildcarded();
  
  /**
   *  Method setSelector
   * 
   * Set a flag to signify that this subscription has a selector.
   */    
  public void setSelector();
  
  /**
   *  Method setTopic
   * 
   * Sets a convenience topic attribute.
   * 
   * @param topic
   */
  public void setTopic(String topic);

  /**
   * isMonitored is not currently used, the assumption being that a consumer
   * that implements this interface will always be monitored. 
   */
  public boolean isMonitored();
  
  
  /**
   *  Method isWildcarded
   * 
   * Returns value of flag that signifies whether this subscription is wildcarded.
   * @return
   */
  public boolean isWildcarded();
  
  /**
   *  Method isSelector
   * 
   * Returns value of flag that signifies whether this subscription has a selector.
   * @return
   */  
  public boolean isSelector();
  
  
  /**
   *  Method getTopic
   * 
   * Returns value of subscription's topic/discriminator.
   * @return
   */
  public String getTopic();

  /**
   * Method getMatchingWildcardMonitorList
   * 
   * Get the list of wildcarded registered consumer monitors that match this 
   * subscription.
   * 
   * @return
   */
  public List getMatchingWildcardMonitorList();

  /**
   *  Method setMatchingWildcardMonitorList
   * 
   * Set the list of wildcarded registered consumer monitors that match this 
   * subscription.
   * 
   * @param topicExprList
   */
  public void setMatchingWildcardMonitorList(List topicExprList);
  
  /**
   *  Method addMatchingWildcardMonitor
   * 
   * Add a new matching monitor to the list of wildcard registered consumer 
   * monitors that match this subscription.
   * 
   * @param topicExpression
   */      
  public void addMatchingWildcardMonitor(String topicExpression);
  
  /**
   *  Method removeMatchingWildcardMonitor
   * 
   * Remove a matching monitor from the list of wildcard registered consumer 
   * monitors that match this subscription.
   * 
   * @param topicExpression
   */        
  public boolean removeMatchingWildcardMonitor(String topicExpression);

  /**
   * Method getMatchingExactMonitorList
   * 
   * Get the list of exact registered consumer monitors that match this 
   * subscription.
   * 
   * @return
   */
  public List getMatchingExactMonitorList();
  
  /**
   *  Method setMatchingExactMonitorList
   * 
   * Set the list of exact registered consumer monitors that match this 
   * subscription.
   * 
   * @param topicExprList
   */  
  public void setMatchingExactMonitorList(List topicExprList);
  
  /**
   *  Method addMatchingExactMonitor
   * 
   * Add a new matching monitor to the list of exact registered consumer 
   * monitors that match this subscription.
   * 
   * @param topicExpression
   */    
  public void addMatchingExactMonitor(String topicExpression);
  
  /**
   *  Method removeMatchingExactMonitor
   * 
   * Remove a matching monitor from the list of exact registered consumer 
   * monitors that match this subscription.
   * 
   * @param topicExpression
   */      
  public boolean removeMatchingExactMonitor(String topicExpression);
}
