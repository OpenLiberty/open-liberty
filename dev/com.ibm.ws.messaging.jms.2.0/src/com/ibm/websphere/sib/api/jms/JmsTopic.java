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
 * Contains provider specific methods relating to the javax.jms.Topic interface.
 * 
 * @ibm-api
 * @ibm-was-base 
 */
public interface JmsTopic extends Topic, JmsDestination
{

  /**
   * Set the TopicSpace for this topic.
   * @param topicSpace
   * @throws JMSException if there is a problem setting this property.
   */
  void setTopicSpace(String topicSpace) throws JMSException;
  /**
   * Get the TopicSpace for this topic.
   * @return the TopicSpace.
   * @throws JMSException if there is a problem getting this property.
   */
  String getTopicSpace() throws JMSException;
  
  /**
   * Set the TopicName for this topic.
   * @param topicName
   * @throws JMSException if there is a problem setting this property.
   */
  void setTopicName(String topicName) throws JMSException;

  /**
   * Get the TopicName for this topic
   * @return the TopicName
   * @throws JMSException if there is a problem getting this property.
   */
  String getTopicName() throws JMSException;

}
