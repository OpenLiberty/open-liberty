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

import com.ibm.wsspi.sib.core.SITransaction;

/**
 * A tagging interface which identifies an auto-commit transaction.
 * It allows objects returned from the TransactionFactory createAutoCommitTransaction()
 * method to be used with both the implementation and the Core SPI.
 */
public interface AutoCommitTransaction extends TransactionCommon, SITransaction
{
}
