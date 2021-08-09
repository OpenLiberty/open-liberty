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

import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;

/**
 SIUncoordinatedTransaction enables multiple updates to be grouped into single 
 atomic updates.
 <p>
 An application may call SICoreConnection.createUncoordinatedTransaction to 
 start a transaction that the application uses to define units of work involving 
 a single messaging engine.
 <p>
 This class has no security implications.
*/
public interface SIUncoordinatedTransaction extends SITransaction {
	
  /**
   Commits the transaction. If the transaction had already completed at the 
   time of the commit call, then SIInvalidCallException is thrown. If an 
   SIRollbackException is thrown, then the transaction has rolled back rather
   than committed. If any other exception is thrown, then the outcome of the
   transaction is unknown. 
   
   @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
   @throws com.ibm.wsspi.sib.core.exception.SIRollbackException
   @throws com.ibm.websphere.sib.exception.SIResourceException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
  */
  public void commit() 
    throws SIIncorrectCallException, 
           SIRollbackException,
           SIResourceException,
           SIConnectionLostException;

  /**
   Rolls back the transaction. If SIInvalidStateForOperationException is thrown, 
   then the transaction had already completed at the time the commit call was 
   issued. If any other exception is thrown, then the state of the transaction 
   is unknown, and can only be determined by examining the state of the 
   underlying data store (directly or indirectly) via later inquiry.
   
   @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
   @throws com.ibm.wsspi.sib.core.exception.SIRollbackException
   @throws com.ibm.websphere.sib.exception.SIResourceException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
  */
  public void rollback() 
    throws SIIncorrectCallException, 
           SIResourceException,
           SIConnectionLostException;

}

