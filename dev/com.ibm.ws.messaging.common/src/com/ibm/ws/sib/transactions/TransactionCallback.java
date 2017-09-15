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
 * Callback used to notify the completion events for a transaction.
 */
public interface TransactionCallback
{
   /**
    * Notification that the transaction is about to be completed.  Completion
    * will take place immediately after all registered callbacks have been
    * notified.
    * @param transaction The transaction to which this notification applies
    */
   public void beforeCompletion(TransactionCommon transaction);
   
   /**
    * Notification that completion has just taken place.
    * @param transaction The transaction to which this notification applies
    * @param committed True if (and only if) the completion event was the
    * transaction being committed.
    */
   public void afterCompletion(TransactionCommon transaction, boolean committed);
}
