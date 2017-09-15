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

package com.ibm.ws.sib.processor.impl.store;

import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.transactions.TransactionCommon;

public abstract class AsyncUpdate
{
  /**
   * The method that executes the update.
   * Most error handling should be done by this method itself
   * and not exposed to the caller (by throwing Throwable).
   * The AsyncUpdateThread guarantees that if any
   * Throwable is thrown by this method, it will call rolledback() on
   * this AsyncUpdate.
   */
  public abstract void execute(TransactionCommon t) throws Throwable;

  /**
   * Commit notification method
   */
  public abstract void committed() throws SIResourceException, SINotPossibleInCurrentConfigurationException;

  /**
   * Rolledback notification method
   * @param e The exception that caused the rollback
   */
  public abstract void rolledback(Throwable e);
}
