package com.ibm.ws.sib.msgstore.task;
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

import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink;
import com.ibm.ws.sib.msgstore.persistence.BatchingContext;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;
import com.ibm.ws.sib.msgstore.transactions.impl.TransactionState;

/**
 * Task to trigger an auto-commit transaction even when we don't need anything done....
 */
public class NullTask extends Task
{
    public NullTask(AbstractItemLink link) throws SevereMessageStoreException {super(link);}

    public int getPersistableInMemorySizeApproximation(TransactionState tranState) {return 0;}

    public void abort(PersistentTransaction transaction) {}

    public void commitExternal(PersistentTransaction transaction) {}

    public void commitInternal(PersistentTransaction transaction) {}

    public void persist(BatchingContext batchingContext, TransactionState transtate) {}

    public void postAbort(PersistentTransaction transaction) {}

    public void postCommit(PersistentTransaction transaction) {}

    public void preCommit(PersistentTransaction transaction) {}
}
