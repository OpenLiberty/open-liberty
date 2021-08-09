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

// Import required classes.
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.processor.impl.store.items.MessageItem;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;

/**
 * @author tevans
 */
/**
 * An interface class for the different types of inpt class.
 */

public interface InputHandler
{
 
  /**
   * Called by the system when processing a message
   * 
   * @param msg The message
   * @param transaction The transaction to use
   * @param sourceCellule The originator of the message
   * @throws SIConnectionLostException
   * @throws SIRollbackException
   * @throws SINotPossibleInCurrentConfigurationException
   * @throws SIIncorrectCallException
   * @throws SIResourceException
   */
  public void handleMessage(
    MessageItem msg,
    TransactionCommon transaction,
    SIBUuid8 sourceCellule)
  throws SIConnectionLostException, 
         SIRollbackException, 
         SINotPossibleInCurrentConfigurationException, 
         SIIncorrectCallException, 
         SIResourceException;
  
}
