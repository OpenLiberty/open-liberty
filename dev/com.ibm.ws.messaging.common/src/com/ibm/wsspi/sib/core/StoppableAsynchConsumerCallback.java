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
  * StoppableAsynchConsumerCallback is an interface that can be implemented by the client 
  * application (or API layer), in order to receive messages asynchronously. It extends
  * the <code>com.ibm.wsspi.sib.core.AsynhConsumerCallback</code> and provides the ability
  * to be calledback when the consumer session associated with this consumer is stopped
  * due to the max sequential message failure threshold been reached. 
  * <p>
  * This class has no security implications.
  * 
  * @see com.ibm.wsspi.sib.core.AsynchConsumerCallback
  *
  * @see com.ibm.wsspi.sib.core.ConsumerSession#registerStoppableAsynchConsumerCallback
  * @see com.ibm.wsspi.sib.core.ConsumerSession#deregisterStoppableAsynchConsumerCallback
  */
public interface StoppableAsynchConsumerCallback extends AsynchConsumerCallback
{
  /**
   * Indicates that the consumer session has been stopped due to the
   * max sequential failures threshold been reached. 
   * 
   * Once this method has been called no messages will be sent to the asynch consumer
   * till the consumer session is started again. 
   *
   */
  public void consumerSessionStopped();
}
