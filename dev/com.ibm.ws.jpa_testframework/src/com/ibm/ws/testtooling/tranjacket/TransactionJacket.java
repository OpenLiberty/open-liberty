/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.testtooling.tranjacket;

import javax.persistence.EntityManager;

public interface TransactionJacket {
    public void beginTransaction();

    public void commitTransaction();

    public void rollbackTransaction();

    public void markTransactionForRollback();

    public boolean isTransactionMarkedForRollback();

    public boolean isTransactionActive();

    public boolean isEntityManagerTransactionJacket();

    public boolean isJTAUserTransactionJacket();

    public boolean isApplicationManaged();

    public void joinTransaction(EntityManager em);
}
