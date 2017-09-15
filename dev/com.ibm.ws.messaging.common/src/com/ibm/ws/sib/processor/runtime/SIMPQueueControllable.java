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

import java.util.Map;

import com.ibm.websphere.sib.Reliability;
import com.ibm.ws.sib.processor.exceptions.*;
import com.ibm.ws.sib.processor.exceptions.SIMPException;
import com.ibm.wsspi.sib.core.DestinationType;

/**
 * The interface presented by a queue to perform dynamic
 * control operations.
 * <p>
 * The operations in this interface are specific to a queueing point.
 */
public interface SIMPQueueControllable extends SIMPMessageHandlerControllable
{
  /**
   * Get the SIMPLocalQueuePointControllable object
   * associated with this Queue.
   * 
   * @return A SIMPLocalQueuePointControllable object
   * 
   * @throws SIMPException if the queue is corrupt.
   */
  public SIMPLocalQueuePointControllable getLocalQueuePointControl()
    throws SIMPException;
  
  /**
   * Get an iterator over all of the SIMPRemoteQueuePointControllable objects
   * associated with this Queue.
   * 
   * @return An iterator containing SIMPRemoteQueuePointControllable objects.
   * 
   * @throws SIMPException if the queue is corrupt.
   */
  public SIMPIterator getRemoteQueuePointIterator() throws SIMPException;
  
  public SIMPRemoteQueuePointControllable getRemoteQueuePointControlByID(String id)
    throws SIMPInvalidRuntimeIDException,
           SIMPControllableNotFoundException,
           SIMPException;
           
  public SIMPRemoteQueuePointControllable getRemoteQueuePointControlByMEUuid(String meUuid)
    throws SIMPInvalidRuntimeIDException,
           SIMPControllableNotFoundException,
           SIMPException;
               
  public long getAlterationTime();

  public DestinationType getDestinationType();  

  public Map getDestinationContext();

  public boolean isSendAllowed();
  public boolean isReceiveAllowed();
  public boolean isReceiveExclusive();
  

  public int getDefaultPriority();

  public String getExceptionDestination();

  public int getMaxFailedDeliveries();

  public boolean isOverrideOfQOSByProducerAllowed();

  public Reliability getDefaultReliability();
  public Reliability getMaxReliability();
}
