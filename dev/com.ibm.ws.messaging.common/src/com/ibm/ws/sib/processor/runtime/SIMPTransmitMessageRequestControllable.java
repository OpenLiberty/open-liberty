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

import com.ibm.ws.sib.processor.SIMPConstants;


/**
 * An interface to manipulate a message request that we have received
 * from a remote messaging engine
 */
public interface SIMPTransmitMessageRequestControllable extends SIMPRemoteMessageControllable
{
  public static class State
  {
    // A message being remotely got at a DME. 
    // Defined known states types.
    public static final State REQUEST = new State(0, SIMPConstants.REQUEST_STRING);
    public static final State PENDING_ACKNOWLEDGEMENT = new State(1, SIMPConstants.PENDINGACKMR_STRING);
    public static final State ACKNOWLEDGED = new State(2, SIMPConstants.ACKNOWLEDGED_STRING);
    public static final State REMOVING = new State(3, SIMPConstants.REMOVING_STRING);
    public static final State REJECT = new State(4, SIMPConstants.REJECT_STRING);
    
    private int value;
    private String name;
    
    private static final State[] set = new State[]
      {REQUEST, PENDING_ACKNOWLEDGEMENT, ACKNOWLEDGED, REMOVING, REJECT};
    
    private State(int value, String name)
    {
      this.value = value;
      this.name = name;
    }
    
    public int toInt()
    {
      return value;      
    }
    
    public String toString()
    {
      return name;
    }
    
    public State getState(int value)
    {
      return set[value];
    }
  }
  
  long getTick();
  
  /**
   * @return SIMPReceivedMessageRequestInfo containing information about the
   * received message request.
   * @author tpm
   */
  SIMPReceivedMessageRequestInfo getRequestMessageInfo();
  
  /**
   * Cancels the message request.
   * @param discard if true, the message being requested is also deleted from
   * this local queue. Otherwise the message is simply unlocked and is
   * made available to other consumers
   * 
   * @author tpm
   */
  void cancelMessageRequest(boolean discard);
  
 
   
}
