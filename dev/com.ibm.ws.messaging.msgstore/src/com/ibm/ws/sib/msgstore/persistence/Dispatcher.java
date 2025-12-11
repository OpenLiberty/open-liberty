/* ==============================================================================
 * Copyright (c) 2012, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * ==============================================================================
 */
package com.ibm.ws.sib.msgstore.persistence;

import java.util.Collection;

import com.ibm.ws.sib.msgstore.PersistenceException;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;

/**
 * Instances of this interface persist <tt>Tasks</tt> using the persistence
 * layer. The choice of when and how to persist is left up to the implementing
 * class.
 */
public interface Dispatcher
{
    /**
     * Starts the dispatcher.
     */
    public void start();

    /**
     * Stops the dispatcher.
     *
     * @param mode specifies the type of stop operation which is to
     *             be performed.
     * @param reason Reason for the stop being requested.
     */
    public void stop(int mode, Throwable reason);

    // Defect 338397
    /**
     * Used as a quick way to check the health of a dispatcher before giving it work
     * in situations in which the work cannot be rejected. For example, for a transaction
     * which requires both synchronous and asynchronous persistence, once we've done the
     * synchronous persistence, a transient persistence problem from a dispatcher will
     * not be reported from the dispatching method because we cannot guarantee to roll back the
     * synchronous work.
     * 
     * @return <tt>true</tt> if the dispatcher is experiencing no problems, else <tt>false</tt>
     */
    public boolean isHealthy();


    // SIB0112d.ms.2
    // Removed isFull() function as the file store no longer uses a dispatcher 
    // for STORE_MAYBE items. It was only in the file store case that we could 
    // reliably determine that we were full and try to do something about it.

    /**
     * Dispatches a list of <tt>Tasks</tt> to be persisted.<p>
     * The call can only be rejected in situations where the error can be
     * handled directly. Otherwise, the work must be accepted by the dispatcher
     * and coped with as well as possible.
     * 
     * @param tasks The collection of <tt>Tasks</tt> to dispatch
     * @param tran  The transaction associated with the <tt>Tasks</tt>
     * @param canReject <tt>true</tt> if the call can be rejected, <tt>false</tt> otherwise
     * 
     * @throws PersistenceException the dispatch was not accepted due to
     *     an error reported by the persistence layer
     */
    public void dispatch(Collection tasks, PersistentTransaction tran, boolean canReject) throws PersistenceException,SevereMessageStoreException;
}
