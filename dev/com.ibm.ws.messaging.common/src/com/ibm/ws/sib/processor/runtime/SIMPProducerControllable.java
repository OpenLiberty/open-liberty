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
package com.ibm.ws.sib.processor.runtime;

import com.ibm.wsspi.sib.core.OrderingContext;
/**
 * 
 */
public interface SIMPProducerControllable extends SIMPControllable
{
  /**
   * Locates the Connection relating to the Producer. 
   *
   * @return Connection  The Connection object. 
   *
   */
  SIMPConnectionControllable getConnection();

  /**
   * Locates the administration destination that the producer is producing to.  
   *
   * @return SIMPMessageHandlerControllable A ForeignBus, Queue or TopicSpace. 
   *
   */
  SIMPMessageHandlerControllable getMessageHandler();
  
  /**
   * Locates the ordering context for the producer.  
   *
   * @return OrderingContext  An OrderingContext or null if there is none. 
   *
   */
  OrderingContext getOrderingContext();
}
