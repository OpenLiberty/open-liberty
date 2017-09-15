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

import com.ibm.ws.sib.utils.SIBUuid12;

/**
 * Interface to manipulate a flow of outgoing messages from this
 * messaging engine.
 * This is the super interface for message flows that are request/response
 * (remote get) and also for message flows that are transmit only (remote put)
 * @author tpm100
 */
public interface SIMPDeliveryTransmitControllable extends SIMPControllable
{
  
  /**
   * The possible states for this stream (to be implemented by the 
   * streams themselves).
   */
  public static interface StreamState
  {
    public String toString();
    public int getValue();
  }
  
  /**
   * @return the StreamState of the stream
   * @author tpm
   */
  StreamState getStreamState();
  
  /**
   * @return the stream ID of this source stream.
   * @author tpm
   */
  SIBUuid12 getStreamID();
  
  /**
   * Return the health state of the stream set
   * 
   * @return HealthState    The state of the stream set
   */
  HealthState getHealthState();
  
}
