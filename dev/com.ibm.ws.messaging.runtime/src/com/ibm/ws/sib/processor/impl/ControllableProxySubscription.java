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

import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.sib.processor.impl.interfaces.OutputHandler;
import com.ibm.ws.sib.processor.matching.MessageProcessorMatchTarget;
import com.ibm.ws.sib.processor.matching.MonitoredConsumer;
import com.ibm.ws.sib.utils.SIBUuid12;

public class ControllableProxySubscription 
  extends MessageProcessorMatchTarget
  implements MonitoredConsumer
{  
  private SIBUuid12 _subscriptionUuid;
  private PubSubOutputHandler _theHandler;
  private String _theTopic;

  private boolean monitored = false;
  private boolean selector = false;
  private boolean wildcarded = false;
  private String topic = null;
  
  // A list of wildcarded registered monitors that match this subscription  
  private ArrayList _matchingWildcardMonitorList;
  
  // A list of non-wildcarded registered monitors that match this subscription
  private ArrayList _matchingExactMonitorList;
  
  /** Flag to indicate whether a proxy sub originated from a foreign bus where 
	  the home bus is secured */  
  private boolean _foreignSecuredProxy;
  
  /** Userid to be stored when securing foreign proxy subs */    
  private String _MESubUserId;
  
  public ControllableProxySubscription(PubSubOutputHandler handler, 
                                       String topic, 
                                       boolean foreignSecuredProxy,
                                       String MESubUserId)
  {
    super(JS_NEIGHBOUR_TYPE);
    _theHandler = handler;
    _theTopic = topic;
    _subscriptionUuid = new SIBUuid12();
    _foreignSecuredProxy = foreignSecuredProxy;
    _MESubUserId = MESubUserId;
    _matchingWildcardMonitorList = new ArrayList();
    _matchingExactMonitorList = new ArrayList();    
  }

  public final boolean equals(Object b)
  {
    boolean areEqual = false;
    if (b instanceof ControllableProxySubscription)
    {
      ControllableProxySubscription k = (ControllableProxySubscription) b;
      
      if (_theHandler.equals(k._theHandler) && _theTopic.equals(k._theTopic))
        areEqual = true;
    }
    return areEqual;    
  }

  public int hashCode()
  {
    return _theTopic.hashCode();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableSubscription#getSubscriptionUuid()
   */
  public SIBUuid12 getSubscriptionUuid()
  {
    return _subscriptionUuid;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableSubscription#getConsumerDispatcherState()
   */
  public ConsumerDispatcherState getConsumerDispatcherState()
  {
    return _theHandler.getConsumerDispatcherState();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableSubscription#getOutputhandler()
   */
  public OutputHandler getOutputHandler()
  {    
    return _theHandler;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableSubscription#isLocal()
   */
  public boolean isLocal()
  {
    return false;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableSubscription#isDurable()
   */
  public boolean isDurable()
  {
    return getConsumerDispatcherState().isDurable();
  }
  
  /**Returns the value of the userid associated with the subscription.
   *
   * @return The userid.
   *
   */
  public String getMESubUserId()
  {
	  return _MESubUserId;
  }
  
  /**Returns true if this proxy sub was from a foreign bus in a secured env.
   *
   * @return The userid.
   *
   */
  public boolean isForeignSecuredProxy()
  {
	  return _foreignSecuredProxy;
  }
  
  /**
   * @param userId
   */
  public void setMESubUserId(String userId)
  {
    _MESubUserId = userId;
  }
   
   
  /**
   * @param isFSP
   */
  public void setForeignSecuredProxy(boolean isFSP)
  {
    _foreignSecuredProxy = isFSP;
  } 
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.matching.MonitoredConsumer#setMonitored()
   */
  public void setMonitored()
  {
    monitored = true;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.matching.MonitoredConsumer#setWildcarded()
   */
  public void setWildcarded()
  {
    wildcarded = true;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.matching.MonitoredConsumer#setSelector()
   */
  public void setSelector()
  {
    selector = true;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.matching.MonitoredConsumer#isMonitored()
   */
  public boolean isMonitored()
  {
    return monitored;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.matching.MonitoredConsumer#isWildcarded()
   */
  public boolean isWildcarded()
  {
    return wildcarded;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.matching.MonitoredConsumer#isSelector()
   */
  public boolean isSelector()
  {
    return selector;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.matching.MonitoredConsumer#getMonitoredTopicExprList()
   */
  public List getMatchingWildcardMonitorList()
  {
    return _matchingWildcardMonitorList;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.matching.MonitoredConsumer#addMonitoredTopicExpr(java.lang.String)
   */
  public void addMatchingWildcardMonitor(String topicExpression)
  {
    _matchingWildcardMonitorList.add(topicExpression);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.matching.MonitoredConsumer#addMonitoredTopicExprList(java.util.List)
   */
  public void setMatchingWildcardMonitorList(List topicExprList)
  {
    _matchingWildcardMonitorList.addAll(topicExprList);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.matching.MonitoredConsumer#getMonitoredExactTopicExprList()
   */
  public List getMatchingExactMonitorList()
  {
    return _matchingExactMonitorList;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.matching.MonitoredConsumer#addMonitoredExactTopicExprList(java.util.List)
   */
  public void setMatchingExactMonitorList(List topicExprList)
  {
    _matchingExactMonitorList.addAll(topicExprList);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.matching.MonitoredConsumer#addMonitoredExactTopicExpr(java.lang.String)
   */
  public void addMatchingExactMonitor(String topicExpression)
  {
    _matchingExactMonitorList.add(topicExpression);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.matching.MonitoredConsumer#removeMonitoredWildcardTopicExpr(java.lang.String)
   */
  public boolean removeMatchingWildcardMonitor(String topicExpression)
  {
    return _matchingWildcardMonitorList.remove(topicExpression);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.matching.MonitoredConsumer#removeMonitoredExactTopicExpr(java.lang.String)
   */
  public boolean removeMatchingExactMonitor(String topicExpression)
  {
    return _matchingExactMonitorList.remove(topicExpression);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.matching.MonitoredConsumer#setTopic(java.lang.String)
   */
  public void setTopic(String topic)
  {
    this.topic = topic;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.matching.MonitoredConsumer#getTopic()
   */
  public String getTopic()
  {
    return topic;
  }  
       
}
