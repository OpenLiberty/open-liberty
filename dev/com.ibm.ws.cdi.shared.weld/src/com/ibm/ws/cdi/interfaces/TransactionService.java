/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.interfaces;

import javax.transaction.Synchronization;
import javax.transaction.UserTransaction;

/**
 *
 */
public interface TransactionService {

    /*
     * (non-Javadoc)
     *
     * @see org.jboss.weld.transaction.spi.TransactionServices#registerSynchronization()
     */
    public void registerSynchronization(Synchronization arg0);

    /*
     * (non-Javadoc)
     *
     * @see org.jboss.weld.transaction.spi.TransactionServices#isTransactionActive()
     */
    public boolean isTransactionActive();

    /*
     * (non-Javadoc)
     *
     * @see org.jboss.weld.transaction.spi.TransactionServices#getUserTransaction()
     */
    public UserTransaction getUserTransaction();

    /*
     * (non-Javadoc)
     *
     * @see org.jboss.weld.bootstrap.api.Service#cleanup()
     */
    public void cleanup();
}
