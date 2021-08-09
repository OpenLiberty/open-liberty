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

import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.*;

public interface SIMPQueuedMessageControllable extends SIMPControllable
{
  public JsMessage getJsMessage() throws SIMPControllableNotFoundException, SIMPException;
  public String getState() throws SIMPException;
  /**
   * Get the transaction identifier XID of the transaction commiting the message or null 
   * if the message is already in commited state.
   *
   * @return a Transaction ID
   */  
  public String getTransactionId() throws SIMPException;
    
  /**
   * If the destination contains a poison message that cannot be processed, for example
   *  because the message is too big for to handle the administrator may 
   *  instruct us to discard the message. Optionally 
   *  the message is transferred to the exception destination. 
   *  This request is atomic and the message will have been moved by the time this 
   *  method returns.
   *     
   * @param discard  true of the message is to be discarded. 
   *                 false if the message is to be sent to the exception destination.
   */
  void moveMessage(boolean discard)
    throws SIMPControllableNotFoundException,
           SIMPRuntimeOperationFailedException;
  
  public static class State
  {
    public static final String LOCKED = SIMPConstants.LOCKED_STRING;
    public static final String UNLOCKED = SIMPConstants.UNLOCKED_STRING;
    public static final String PENDING_RETRY = SIMPConstants.PENDING_RETRY_STRING;
    public static final String BLOCKED = SIMPConstants.BLOCKED_STRING;
    public static final String COMMITTING = SIMPConstants.COMMIT_STRING;
    public static final String REMOVING = SIMPConstants.REMOVE_STRING;
    public static final String REMOTE_LOCKED = SIMPConstants.REMOTE_LOCKED_STRING;
  }
  
  /**
   * @return the value tick for this message
   * @author tpm
   */
  public long getSequenceID();
  
  /**
   * @return a long for the value tick of the previous message
   * @author tpm
   */
  public long getPreviousSequenceId();
  
  /**
   * Get the approximate length (bytes) of the queued message
   * @return length in bytes
   */
  public long getApproximateLength();
}
