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
package com.ibm.ws.sib.transactions;

/**
 * Extension to the transaction callback interface.  This interface provides
 * an enhanced after completion method which will be called in preference to
 * the after completion method of TransactionCallback.
 */
public interface ExtendedTransactionCallback extends TransactionCallback
{
   /**
    * Provides notification that the transaction, or transaction branch has
    * completed.
    * @param transaction the transaction that has completed
    * @param tranId the persistent transaction identifier that identifies the transaction
    * or transaction branch being completed.
    * @param committed true if the transaction completion resulted in the unit of work
    * being committed successfully.
    */
   public void afterCompletion(TransactionCommon transaction, PersistentTranId tranId, boolean committed);
}
