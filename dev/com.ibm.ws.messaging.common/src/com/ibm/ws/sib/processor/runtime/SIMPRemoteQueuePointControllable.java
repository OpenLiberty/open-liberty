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

/**
 * The interface presented by a queueing point localization to perform dynamic
 * control operations.
 * <p>
 * The operations in this interface are specific to a queueing point.
 */
public interface SIMPRemoteQueuePointControllable extends SIMPXmitPoint
{ 
  /**
   * Get the remote consumer receiver. This exists if we are performing or have performed
   * remote get against the remote queue. 
   * 
   * SIB0113a - This only returns the non-gathering stream. 
   *
   * @return A RemoteConsumerReceiver or null if there is none. 
   */
  public SIMPRemoteConsumerReceiverControllable getRemoteConsumerReceiver();
   
  /**
   * As of the introduction of gathering consumers (SIB0113) it is now possible
   * to have multiple remote get streams. Admin now needs to iterate over them
   * and display a collections panel for the list. 
   * 
   * The old method above is kept for legacy code.
   */
  public SIMPIterator getRemoteConsumerReceiverIterator();
  
  /**
   * Get the number of message requests that have completed via this remote queue point.
   * This is required at this level to be able to retrieve the value even when
   * the individual remote consumer receivers have disappeared.
   * 
   */
  public long getNumberOfCompletedRequests();
}
