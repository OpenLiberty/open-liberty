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

package com.ibm.wsspi.sib.core;

  /**
   An OrderingContext object can be used when creating a 
   ProducerSession or ConsumerSession, or when using the send and receive methods 
   of SICoreConnection, to indicate that the corresponding messaging operations 
   should be ordered, and not executed concurrently. 
   <p> 
   An OrderingContext object is created by calling 
   SICoreConnection.createOrderingContext. It may be used with 
   the SICoreConnection used to create it, or another SICoreConnection which is 
   equivalent to the original according to the SICoreConnection.isEquivalentTo
   method. (Note that only OrderingContext objects returned from 
   SICoreConnection.createOrderingContext may be used; the Core SPI user cannot
   provide their own OrderingContext implementation.)  
   <p>
   Ordinarily, the order of messages sent from a given ProducerSession with the 
   same priority and Reliability is preserved, but messages sent from different 
   ProducerSessions may overtake one another. By specifying a common 
   OrderingContext object on the 
   SICoreConnection.createProducerSession or send calls, the scope of message 
   ordering may be extended beyond an individual ProducerSession.
   <p>
   Ordinarily, the Core SPI implementation will never use more than one thread
   to deliver messages at the same time to a single ConsumerSession, but 
   different threads may be used to deliver messages to different 
   ConsumerSessions concurrently. By specifying a common
   OrderingContext object on the ConsumerSession.registerAsynchConsumerCallback method
   the scope of ordered message delivery may be extended beyond an individual 
   ConsumerSession: there will only ever be one message (or message batch) at a 
   time delivered to any ConsumerSession sharing the same 
   OrderingContext. In the case of ConsumerSession attached to 
   different destinations, this simply means that the ConsumerSessions will 
   receive messages serially; in the case of ConsumerSessions attached to the 
   same destination, then message order will be preserved across the different 
   ConsumerSessions. Note that OrderingContext only applies to ConsumerSessions which  
   receive messages asynchronously.
   <p>
   OrderingContext is designed specifically to permit the 
   implementation of the JMS API, with its three-layer model
   of "Connection", "Session" and "Producer/Consumer" objects, and requirement for 
   ordering to be scoped by "Session". Use of OrderingContext 
   under other circumstances is strongly discouraged, since increasing the scope 
   of message ordering decreases the opportunity for concurrency within the Core 
   SPI implementation, and hence reduces scalability.
  <p>
  This class has no security implications.
  */
  public interface OrderingContext
  {
  }
