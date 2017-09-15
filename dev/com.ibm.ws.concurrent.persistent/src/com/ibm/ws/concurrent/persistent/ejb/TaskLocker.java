/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.ejb;

/**
 * This interface is only to be used by EJB Container for singleton Timer Tasks that run in the
 * persistent executor transaction. For this pattern, a serializable trigger (which is also the task)
 * may implement this interface to be prompted to do its own locking/unlocking
 * before the persistent executor obtains the lock on the task entry in the database,
 * and after the persistent executor completes its updates to the database.
 */
public interface TaskLocker extends TimerTrigger {
    /**
     * This method is invoked in the persistent executor's transaction before it obtains a lock on the task entry
     * in the database.
     */
    void lock();

    /**
     * This method is invoked after the persistent executor completes its updates to the database.
     */
    void unlock();
}