/*******************************************************************************
 * Copyright (c) 1999, 2001 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.cpi;

import javax.transaction.*;

/**
 * PersisterTx provides utility methods pertaining to transactions
 * for use by the persisters.
 */

public interface PersisterTx
{

    /**
     * @return a boolean which is true if the transaction is scoped to
     *         the current business method on the bean.
     */
    public boolean beganInThisScope();

    /**
     * Persisters which require synchronization callbacks can register
     * with the transaction using this method.
     * 
     * @param s a Synchronization object on which the tx callbacks are
     *            to be fired.
     * 
     * @exception com.ibm.websphere.csi.CSIException thrown if unable to
     *                enlist for any reason.
     */
    public void registerSynchronization(Synchronization s)
                    throws CPIException;

} // PersisterTx

