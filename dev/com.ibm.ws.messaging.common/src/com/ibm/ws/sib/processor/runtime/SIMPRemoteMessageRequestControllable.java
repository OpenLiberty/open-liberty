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
import com.ibm.ws.sib.processor.exceptions.SIMPRuntimeOperationFailedException;

/**
 * Interface to manipulate a message request that we have sent to a 
 * remote messaging engine
 * 
 * @author tpm100
 */
public interface SIMPRemoteMessageRequestControllable extends SIMPRemoteMessageControllable
{
  public static class State
  {
		// A message being requested by a RME. 
		// Defined known states types.
		public static final State REQUEST = new State(0, SIMPConstants.REQUEST_STRING);
		public static final State VALUE = new State(1, SIMPConstants.VALUE_STRING);
		public static final State LOCKED = new State(2, SIMPConstants.LOCKED_MR_STRING);
		public static final State ACKNOWLEDGED = new State(3, SIMPConstants.ACKNOWLEDGED_STRING);
		public static final State REJECT = new State(4, SIMPConstants.REJECT_STRING);
        public static final State COMPLETED = new State(5, SIMPConstants.COMPLETED_STRING);
    
		private int value;
		private String name;
    
		private static final State[] set = new State[]
			{REQUEST, VALUE, LOCKED, ACKNOWLEDGED, REJECT, COMPLETED};
    
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
   * Returns the SIMPRequestMessageInfo if the state of the SIMPRemoteMessageRequest
   * is REQUEST else null
   * 
   * @throws SIMPRuntimeOperationFailedException 
   * @return SIMPRequestMessageInfo
   */
  SIMPRequestMessageInfo getRequestMessageInfo() throws SIMPRuntimeOperationFailedException;
  
  /**
   * Returns the SIMPRequestedValueMessageUInfo if the state of the SIMPRemoteMessageRequest
   * is VALUE else null.
   * 
   * @throws SIMPRuntimeOperationFailedException
   * @return SIMPRequestedValueMessageInfo
   */
  SIMPRequestedValueMessageInfo getRequestedValueMessageInfo() throws SIMPRuntimeOperationFailedException;
}
