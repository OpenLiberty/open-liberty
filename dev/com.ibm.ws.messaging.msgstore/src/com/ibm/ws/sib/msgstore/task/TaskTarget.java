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
package com.ibm.ws.sib.msgstore.task;

import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;

public interface TaskTarget {
    public abstract void abortAdd(final PersistentTransaction transaction);

    public abstract void abortPersistLock(PersistentTransaction transaction);
    public abstract void abortPersistUnlock(PersistentTransaction transaction);
    public void abortRemove(final PersistentTransaction transaction) ;
    public abstract void abortUpdate(final PersistentTransaction transaction);
    public abstract void commitAdd(final PersistentTransaction transaction);
    public abstract void commitPersistLock(PersistentTransaction transaction);
    public abstract void commitPersistUnlock(PersistentTransaction transaction);
    public void commitRemove(final PersistentTransaction transaction) ;
    public abstract void commitUpdate(final PersistentTransaction transaction);
    public abstract void postAbortAdd(final PersistentTransaction transaction);
    public abstract void postAbortPersistLock(PersistentTransaction transaction);

    public abstract void postAbortPersistUnlock(PersistentTransaction transaction);
    public void postAbortRemove(final PersistentTransaction transaction);

    public abstract void postAbortUpdate(final PersistentTransaction transaction);
    public abstract void postCommitAdd(final PersistentTransaction transaction);
    public abstract void postCommitPersistLock(PersistentTransaction transaction);

    public abstract void postCommitPersistUnlock(PersistentTransaction transaction);

    public void postCommitRemove(final PersistentTransaction transaction);
    public abstract void postCommitUpdate(final PersistentTransaction transaction);
    public abstract void preCommitAdd(final PersistentTransaction transaction);
    public void preCommitRemove(final PersistentTransaction transaction) ;
    public abstract void preCommitUpdate(final PersistentTransaction transaction);
}
