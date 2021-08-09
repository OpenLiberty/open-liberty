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
/*
 * Change activity:
 *
 * Reason          Date    Origin     Description
 * --------------- ------  --------   --------------------------------------------
 *                 06-Aug-2004 pradine    Original
 * F1332-51592     13/10/11    vmadhuka   Persist redelivery count
 * ============================================================================
 */
package com.ibm.ws.sib.msgstore.test.persistence;

import com.ibm.ws.sib.msgstore.PersistenceException;
import com.ibm.ws.sib.msgstore.persistence.BatchingContext;
import com.ibm.ws.sib.msgstore.persistence.Persistable;
import com.ibm.ws.sib.transactions.PersistentTranId;

/**
 * @author pradine
 *
 */
public class BatchingContextImpl implements BatchingContext {

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.persistence.BatchingContext#setCapacity(int)
     */
    public void setCapacity(int capacity) {
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.persistence.BatchingContext#getCapacity()
     */
    public int getCapacity() {
        return 5;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.persistence.BatchingContext#setUseEnlistedConnections(boolean)
     */
    public void setUseEnlistedConnections(boolean useEnlistedConnections) {
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.persistence.BatchingContext#insert(com.ibm.ws.sib.msgstore.persistence.Persistable)
     */
    public void insert(Persistable tuple) {
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.persistence.BatchingContext#updateDataAndSize(com.ibm.ws.sib.msgstore.persistence.Persistable)
     */
    public void updateDataAndSize(Persistable tuple) {
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.persistence.BatchingContext#updateLockIDOnly(com.ibm.ws.sib.msgstore.persistence.Persistable)
     */
    public void updateLockIDOnly(Persistable tuple) {
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.persistence.BatchingContext#updateRedeliveredCountOnly(com.ibm.ws.sib.msgstore.persistence.Persistable)
     */
    public void updateRedeliveredCountOnly(Persistable tuple) {
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.persistence.BatchingContext#updateLogicalDeleteAndXID(com.ibm.ws.sib.msgstore.persistence.Persistable)
     */
    public void updateLogicalDeleteAndXID(Persistable tuple) {
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.persistence.BatchingContext#delete(com.ibm.ws.sib.msgstore.persistence.Persistable)
     */
    public void delete(Persistable tuple) {
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.persistence.BatchingContext#addIndoubtXID(com.ibm.ws.sib.msgstore.transactions.PersistentTranId)
     */
    public void addIndoubtXID(PersistentTranId xid) {
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.persistence.BatchingContext#updateXIDToCommitted(com.ibm.ws.sib.msgstore.transactions.PersistentTranId)
     */
    public void updateXIDToCommitted(PersistentTranId xid) {
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.persistence.BatchingContext#updateXIDToRolledback(com.ibm.ws.sib.msgstore.transactions.PersistentTranId)
     */
    public void updateXIDToRolledback(PersistentTranId xid) {
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.persistence.BatchingContext#deleteXID(com.ibm.ws.sib.msgstore.transactions.PersistentTranId)
     */
    public void deleteXID(PersistentTranId xid) {
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.persistence.BatchingContext#executeBatch()
     */
    public void executeBatch() throws PersistenceException {
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.persistence.BatchingContext#clear()
     */
    public void clear() {
    }
}
