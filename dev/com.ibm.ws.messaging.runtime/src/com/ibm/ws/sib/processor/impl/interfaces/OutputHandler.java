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
package com.ibm.ws.sib.processor.impl.interfaces;

import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.processor.impl.store.items.MessageItem;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;

/**
 * An interface class for the different types of output class.
 */
public interface OutputHandler
{
  /**
   * Put a message on this OutputHandler for delivery to consumers/remote ME's 
   * 
   * @param msg The message to be delivered
   * @param tran The transaction to be used (must at least have an autocommit transaction)
   * @param inputHandlerStore The input handler putting this message 
   * @param storedByIH true if the message has already been stored in the Input Handler
   * @return true if the message was stored in the Input Handler (either before or during this call)
   * 
   * @throws SIResourceException  thrown if there is some problem with GD 
   */
  public boolean put(
    SIMPMessage msg,
    TransactionCommon transaction,
    InputHandlerStore inputHandlerStore,
    boolean storedByIH)
    throws SIResourceException, SIDiscriminatorSyntaxException;

  /** 
   * @param msg
   * @return boolean true if message can be sent to another ME
   */
  public boolean commitInsert(MessageItem msg)
    throws SIResourceException;
  
  /** 
   * @param  msg
   * @return boolean true if message can be sent to another ME
   */
  public boolean rollbackInsert(MessageItem msg)
    throws SIResourceException;

  /**  
   * @return boolean true if this outputhandler from wlm was guessed
   */
  public boolean isWLMGuess();
  
  /** 
   * @param  guess
   */
  public void setWLMGuess(boolean guess);
    
  /**  
   * @return boolean true if this outputhandler's itemstream has reached QHighMessages
   */
  public boolean isQHighLimit();
  
  /**
   * Get the Uuid of the ME that this OutputHandler represents
   * @return SIBUuid8
   */
  public SIBUuid8 getTargetMEUuid();
}
