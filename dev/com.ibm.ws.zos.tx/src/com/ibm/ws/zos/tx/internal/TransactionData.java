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
package com.ibm.ws.zos.tx.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.transaction.xa.XAResource;

/**
 * Common transaction data (local/global).
 */
public abstract class TransactionData {
    /**
     * Lock for reading and updating data source configuration.
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * The list of XA resource instances associated with the global transaction.
     */
    private final ArrayList<XAResource> xaResourceList;

    /**
     * Constructor.
     */
    TransactionData(ArrayList<XAResource> xaResourceList) {
        this.xaResourceList = xaResourceList;
    }

    /**
     * Retrieves the list of XA resources enlisted with the transaction.
     *
     * @return Retrieves the list of XA resources enlisted with the transaction.
     */
    public final List<XAResource> getXAResourceList() {
        return xaResourceList;
    }

    /**
     * Retrieves the lock associated with this object.
     *
     * @return Returns The ReaderWritter lock associated with the object.
     */
    protected final ReadWriteLock getLock() {
        return lock;
    }
}
