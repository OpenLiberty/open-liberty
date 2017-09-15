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
package com.ibm.ws.sib.msgstore.transactions;

import com.ibm.ws.sib.transactions.AutoCommitTransaction;

/**
 * This interface is implemented by auto-commit transactions which can be used with the
 * Message Store.
 */
public interface ExternalAutoCommitTransaction extends AutoCommitTransaction, Transaction
{

}
