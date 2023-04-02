package com.ibm.ws.sib.msgstore.transactions.impl;
/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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

import com.ibm.ws.sib.msgstore.MessageStoreException;

public interface WorkList
{
    public void addWork(WorkItem item);

    public void preCommit(PersistentTransaction transaction) throws MessageStoreException;

    public void commit(PersistentTransaction transaction) throws MessageStoreException;

    public void rollback(PersistentTransaction transaction) throws MessageStoreException;

    public void postComplete(PersistentTransaction transaction, boolean committed) throws MessageStoreException;

    public String toXmlString();
}
