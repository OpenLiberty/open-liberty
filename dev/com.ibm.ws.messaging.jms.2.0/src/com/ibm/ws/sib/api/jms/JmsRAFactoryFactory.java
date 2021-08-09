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

package com.ibm.ws.sib.api.jms;

import javax.jms.*;

import com.ibm.ws.sib.api.jmsra.JmsJcaConnectionFactory;
import com.ibm.ws.sib.api.jmsra.JmsJcaManagedConnectionFactory;
import com.ibm.ws.sib.api.jmsra.JmsJcaManagedQueueConnectionFactory;
import com.ibm.ws.sib.api.jmsra.JmsJcaManagedTopicConnectionFactory;

/**
 * This interface defines additional methods for creating factories that will
 * be used by the JCA resource adaptor. These methods are not intended for
 * public consumption, and hence are in the ws package rather than
 * com.ibm.websphere.
 * 
 * This class is specifically NOT tagged as ibm-spi because by definition it is not
 * intended for use by either customers or ISV's.
 */

public interface JmsRAFactoryFactory
{

  public ConnectionFactory createConnectionFactory(
   JmsJcaConnectionFactory jcaConnectionFactory);
   
  public ConnectionFactory createConnectionFactory(
   JmsJcaConnectionFactory jcaConnectionFactory,
   JmsJcaManagedConnectionFactory jcaManagedConnectionFactory);
   
  public TopicConnectionFactory createTopicConnectionFactory(
   JmsJcaConnectionFactory jcaConnectionFactory);
   
  public TopicConnectionFactory createTopicConnectionFactory(
   JmsJcaConnectionFactory jcaConnectionFactory,
   JmsJcaManagedTopicConnectionFactory jcaManagedTopicConnectionFactory);

  public QueueConnectionFactory createQueueConnectionFactory(
   JmsJcaConnectionFactory jcaConnectionFactory);

  public QueueConnectionFactory createQueueConnectionFactory(
   JmsJcaConnectionFactory jcaConnectionFactory,
   JmsJcaManagedQueueConnectionFactory jcaManagedQueueConnectionFactory);

}
