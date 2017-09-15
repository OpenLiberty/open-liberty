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

import java.util.Date;
import com.ibm.ws.sib.processor.SIMPConstants;



/**
 * An interface to manipulate a message on a stream
 * @author tpm100
 */
public interface SIMPTransmitMessageControllable extends SIMPRemoteMessageControllable
{
  public static class State
  {
    // A message meing sent to a DME.
    // Defined known states types.
    //Complete should not get used because we're not really interested
    //in completed transmit messages
    public static final State COMPLETE = new State(0, SIMPConstants.COMPLETE_STRING);
    public static final State COMMITTING = new State(1, SIMPConstants.COMMITTING_STRING);
    public static final State PENDING_SEND = new State(2, SIMPConstants.PENDINGSEND_STRING);
    public static final State PENDING_ACKNOWLEDGEMENT = new State(3, SIMPConstants.PENDINGACK_STRING);
    
    private int value;
    private String name;
    
    private static final State[] set = new State[]
      {COMPLETE, COMMITTING, PENDING_SEND, PENDING_ACKNOWLEDGEMENT};
    
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
    
  /**
   * @return the java.util.Date of when this message was produced
   * @author tpm
   */  
  Date getProducedTime();    
  
  /**
   * @return the value tick for this message
   * @author tpm
   */
  long getSequenceID();
  
  /**
   * @return a long for the value tick of the previous message
   * @author tpm
   */
  long getPreviousSequenceId();
}
