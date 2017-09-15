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
package com.ibm.websphere.sib.api.jms;

import javax.jms.*;

/**
 * Contains provider specific methods relating to the javax.jms.Queue interface.
 * 
 * @ibm-api
 * @ibm-was-base 
 */
public interface JmsQueue extends Queue, JmsDestination
{

  /**
   * Get the QueueName for this queue.
   * @return the QueueName
   * @throws JMSException if there is a problem returning this property. 
   */
  public String getQueueName() throws JMSException;
  
  
  /**
   * Set the QueueName for this queue. 
   * 
   * @param queueName
   * @throws JMSException if there is a problem setting this property.
   */
  public void setQueueName(String queueName) throws JMSException;
  
  
  /**
   * Set whether the SIB queue destination identified by this JMSQueue is dynamically
   * scoped to a single queue point if one exists on the messaging engine that the 
   * application is connected to.<p>
   *
   * If the destination is scoped to the local queue point all operations performed on
   * this JMSQueue object will be limited to the local queue point and behave as if the
   * queue consists of this single queue point. This includes message production,
   * message consumption and message browsing, both by this application
   * and any JMS application that receives a message containing this JMSQueue as the
   * JMSReplyTo property. <p>
   *
   * If the connected messaging engine does not own a queue point of the destination
   * this property is ignored. <p>
   * 
   * If the queue destination has a single queue point this property is ignored. <p>
   * 
   * If the queue destination resides in a different bus from the one that the application
   * is connected to, this option has no effect on the choice of the destinations queue points. <p>
   * 
   * Permitted values for the scopeToLocalQP property are as follows;
   * <ul>
   * <li>{@link ApiJmsConstants#SCOPE _TO_LOCAL_QP_ON} - Scope operations to a local queue point (if 
   *    configured)
   * <li>{@link ApiJmsConstants#SCOPE_TO_LOCAL_QP_OFF} - Do not scope operations to a local queue
   *    point (default)
   * </ul>
   * <p>
   *
   * This property was introduced in WebSphere Application Server V7 <p>
   *
   * @param scopeToLocalQP Should scoping be performed.
   * @throws JMSException If a validation failure occurs.
   */
  public void setScopeToLocalQP(String scopeToLocalQP) throws JMSException;

  /**
   * Get the current setting for the ScopeToLocalQP property for this JMSQueue.<p>
   *
   * @return String The current setting for ScopeToLocalQP
   */
  public String getScopeToLocalQP();

  /**
   * Set whether a MessageProducer for this JMSQueue should prefer a locally
   * connected queue point of the queue destination over any other queue points.<p>
   *
   * This property indicates whether a queue point on the connected messaging engine
   * is preferred over any other queue points unless the local one is unable to accept
   * messages at the time that they are sent. If the local queue point is unable to
   * accept messages then workload balancing of messages will occur across all available
   * queue points.<p>
   * 
   * If the connected messaging engine does not own a queue point of the destination
   * this property is ignored. <p>
   * 
   * If the queue destination has a single queue point this property is ignored. <p>
   * 
   * If the queue destination resides in a different bus from the one that the application
   * is connected to, this option has no effect on the choice of the destinations queue points. <p>
   * 
   * Permitted values for the preferLocal property are as follows;
   * <ul>
   * <li>{@link ApiJmsConstants#PRODUCER_PREFER_LOCAL_ON} - An available queue point on the connected
   *    messaging engine is preferred when sending messages (default)
   * <li>{@link ApiJmsConstants# PRODUCER_PREFER_LOCAL_OFF} - A queue point on the connected
   *    messaging engine is not preferred over any others
   * </ul>
   * <p>
   *
   * This property was introduced in WebSphere Application Server V7 <p>
   *
   * @param preferLocal Should a local queue point be preferred.
   * @throws JMSException If a validation failure occurs.
   */
  public void setProducerPreferLocal(String preferLocal) throws JMSException;

  /**
   * Get the current setting for the ProducerPreferLocal property for this JMSQueue.<p>
   *
   * @return String The current setting for ProducerPreferLocal
   */
  public String getProducerPreferLocal();

  /**
   * Set whether messages sent by a single MessageProducer to this JMSQueue will go to the
   * same queue point, or whether no such restriction will be applied, and different messages
   * will be sent to different queue points.<p>
   * 
   * This option only applies to MessageProducers where the queue is identified at the
   * time the MessageProducer is created, not at the time of sending messages. <p>
   *
   * If the queue destination has a single queue point this property is ignored. <p>
   * 
   * If the queue destination resides in a different bus from the one that the application
   * is connected to, this option has no effect on the choice of the destinations queue points. <p>
   * 
   * Permitted values for the bind property are as follows;
   * <ul>
   * <li>{@link ApiJmsConstants#PRODUCER_BIND_ON} - Send all messages to the same queue point
   * <li>{@link ApiJmsConstants#PRODUCER_BIND_OFF} - Allow messages to be sent to different queue
   *    points (default)
   * </ul>
   * <p>
   *
   * This property was introduced in WebSphere Application Server V7 <p>
   *
   * @param bind Should all messages be sent to the same queue point.
   * @throws JMSException If a validation failure occurs.
   */
  public void setProducerBind (String bind) throws JMSException;

  /**
   * Get the current setting for the ProducerBind property for this JMSQueue.<p>
   *
   * @return String The current setting for ProducerBind
   */
  public String getProducerBind();

  
  /**
   * Set whether messages on all queue points or only a single queue point are
   * visible to MessageConsumers and QueueBrowsers using this JMSQueue. <p>
   *
   * Enabling this property indicates that MessageConsumers and QueueBrowsers should
   * have messages from all queue points of this queue destination visible to them
   * for consuming or browsing. Disabling this property indicates that only messages
   * from a single queue point are visible to MessageConsumers and QueueBrowsers.
   * If the latter, a queue point on the messaging engine that the application is
   * connected to is preferred, if no such queue point exists the system will choose
   * a queue point from those available.<p>
   *
   * If the queue destination has a single queue point this property is ignored. <p>
   * 
   * Permitted values for the gatherMessages property are as follows;
   * <ul>
   * <li>{@link ApiJmsConstants#GATHER_MESSAGES_ON} - Allow gathering of messages from any
   *    available queue point
   * <li>{@link ApiJmsConstants#GATHER_MESSAGES_OFF} - Only process messages from the
   *    attached queue point (default)
   * </ul>
   * <p>
   *
   * This property was introduced in WebSphere Application Server V7 <p>
   *
   * @param gatherMessages Should messages be gathered from all queue points.
   * @throws JMSException If an invalid value is specified. 
   */
  public void setGatherMessages(String gatherMessages) throws JMSException;

  /**
   * Get the current setting for the GatherMessages property for this JMSQueue.<p>
   *
   * @return String The current setting for GatherMessages
   */
  public String getGatherMessages();


}
