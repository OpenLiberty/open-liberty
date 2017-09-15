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

import javax.jms.MessageConsumer;
import javax.jms.*;

/**
 * Contains provider specific methods relating to the javax.jms.MessageConsumer interface. 
 * 
 * @ibm-api
 * @ibm-was-base 
 */
public interface JmsMsgConsumer extends MessageConsumer
{
	
	/**
   * Get the Destination associated with this MessageConsumer.<p>
   * 
   * This method should be part of the standard JMS interfaces (to provide
   * symmetry with the MessageProducer, and also QueueReceiver/TopicSubscriber,
   * however it appears to have been omitted.
   * 
   * <!-- Javadoc'd: matrober 220803 -->
   * 
   * @return This consumer's Destination.
   * @throws JMSException If the JMS provider fails to get the destination for
   *                      this MessageProducer due to some internal error.
   */
  public Destination getDestination() throws JMSException;
  

}
