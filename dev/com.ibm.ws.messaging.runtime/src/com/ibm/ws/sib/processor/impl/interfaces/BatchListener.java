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

import com.ibm.ws.sib.transactions.TransactionCommon;

/**
 * BatchListener should be implemented by any class wishing to receive
 * batching events
 * 
 * @author tevans
 */
public interface BatchListener {

  /**
   * @param currentTran
   */
  public void batchPrecommit(TransactionCommon currentTran);

  /**
   * 
   */
  public void batchCommitted();

  /**
   * 
   */
  public void batchRolledBack();
  
}
