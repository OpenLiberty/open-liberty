package com.ibm.tx.jta.impl;
/*******************************************************************************
 * Copyright (c) 2002, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import javax.transaction.xa.XAException;

public interface JCATranWrapper
{
    /**
     * Requests the prepare vote from the object
     *
     * @return javax.transaction.xa.XAResource.XA_OK or 
     * javax.transaction.xa.XAResource.XA_RDONLY
     * @exception XAException thrown with the following error codes:
     * <ul><li>XA_RBROLLBACK - transaction has rolled back</li>
     * <li>XAER_PROTO - routine was invoked in an inproper context</li>
     * <li>XA_RMERR - a resource manager error has occurred</li></ul>
     *
     * @see javax.transaction.xa.XAException
     */
    public int prepare(TxXATerminator xat) throws XAException;

    /**
     * Informs the object that the transaction is to be committed
     *
     * @exception XAException thrown with the following error codes:
     * <ul><li>XA_HEURMIX - the transaction branch has been heuristically
     * committed and rolled back</li>
     * <li>XA_HEURRB - the transaction branch has been heuristically rolled back</li>
     * <li>XAER_PROTO - the routine was invoked in an inproper context</li>
     * <li>XA_RMERR - a resource manager error has occurred</li></ul>
     *
     * @see javax.transaction.xa.XAException
     */
    public void commit() throws XAException;

    /**
     * Informs the object that the transaction is to be committed in one-phase
     *
     * @exception XAException thrown with the following error codes:
     * <ul><li>XA_RBROLLBACK - the transaction has rolled back</li>
     * <li>XA_HEURHAZ - the transaction branch may have been heuristically committed</li>
     * <li>XA_HEURRB - the transaction branch has been heuristically rolled back</li>
     * <li>XAER_PROTO - the routine was invoked in an inproper context</li>
     * <li>XA_RMERR - a resource manager error has occurred</li></ul>
     *
     * @see javax.transaction.xa.XAException
     */
    public void commitOnePhase() throws XAException;

    /**
     * Informs the object that the transaction is to be rolled back
     *
     * @exception XAException thrown with the following error codes:
     * <ul><li>XA_HEURMIX - the transaction branch has been committed
     * and heuristically rolled back</li>
     * <li>XAER_PROTO - the routine was invoked in an inproper context</li>
     * <li>XA_RMERR - a resource manager error has occurred</li></ul>
     *
     * @see javax.transaction.xa.XAException
     */
    public void rollback() throws XAException;

    /**
     * Informs the object that the transaction is to be forgotten
     *
     * @exception XAException
     */
    public void forget() throws XAException;

    /**
     *
     */
    public void suspend();

    /**
     * 
     */
    public void resume();

    /**
     * Returns the TransactionImpl object
     *
     * @return The TransactionImpl
     */
    public TransactionImpl getTransaction();

    /**
     * @return
     */
    public boolean isPrepared();

    /**
     * @return
     */
    public boolean hasAssociation();

    /**
     * 
     */
    public void addAssociation();

    /**
     * 
     */
    public void removeAssociation();
}