/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb;

import javax.ejb.EJBException;

public interface TxAttrRemote {
    /**
     * Used to verify that when a method with a REQUIRED transaction attribute
     * is called while the calling thread is not currently associated with a
     * transaction context causes the container to begin a global transaction.
     *
     * @return boolean true if method is dispatched in a global transaction.
     *         boolean false if method is dispatched in a local transaction.
     *
     * @throws java.lang.IllegalStateException
     *             is thrown if method is dispatched while not in any
     *             transaction context.
     */
    public boolean txRequired() throws EJBException;

    /**
     * Used to verify that when a method with a REQUIRED transaction attribute
     * is called while calling thread is currently associated with a global
     * transaction causes the container to dispatch the method in the caller's
     * global transaction context (e.g container does not begin a new
     * transaction). The caller must begin a global transaction prior to calling
     * this method.
     *
     * @param tid
     *            is the global transaction ID for the transaction that was
     *            started prior to calling this method.
     *
     * @return boolean true if method is dispatched in the same transaction
     *         context with the same transaction ID as passed by tid parameter.
     *         Otherwise boolean false is returned.
     *
     * @throws java.lang.IllegalStateException
     *             is thrown if method is dispatched while not in any
     *             transaction context.
     */
    public boolean txRequired(byte[] tid) throws EJBException;

    /**
     * Used to verify when a method with a REQUIRES_NEW transaction attribute is
     * called while calling thread is currently associated with a global
     * transaction causes the container to dispatch the method in the a new
     * global transaction context (e.g container does begin a new global
     * transaction). The caller must begin a global transaction prior to calling
     * this method.
     *
     * @param tid
     *            is the global transaction ID for the transaction that was
     *            started prior to calling this method.
     *
     * @return boolean true if method is dispatched in a global transaction with
     *         a global transaction ID the does not match the tid parameter.
     *         Otherwise boolean false is returned.
     *
     * @throws java.lang.IllegalStateException
     *             is thrown if method is dispatched while not in any
     *             transaction context.
     */
    public boolean txRequiresNew(byte[] tid) throws EJBException;

    /**
     * Used to verify that when a method with a REQUIRES NEW transaction
     * attribute is called while the calling thread is not currently associated
     * with a transaction context causes the container to begin a global
     * transaction.
     *
     * @return boolean true if method is dispatched in a global transaction.
     *         boolean false if method is dispatched in a local transaction.
     *
     * @throws java.lang.IllegalStateException
     *             is thrown if method is dispatched while not in any
     *             transaction context.
     */
    public boolean txRequiresNew() throws EJBException;

    /**
     * Used to verify when a method with a MANDATORY transaction attribute is
     * called while thread is currently associated with a global transaction
     * causes the container to dispatch the method in the callers global
     * transaction context (e.g container does not begin a new transaction). The
     * caller must begin a global transaction prior to calling this method.
     *
     * @param tid
     *            is the global transaction ID for the transaction that was
     *            started prior to calling this method.
     *
     * @return boolean true if method is dispatched in the same transaction
     *         context with the same transaction ID as passed by tid parameter.
     *         Otherwise boolean false is returned.
     *
     * @throws java.lang.IllegalStateException
     *             is thrown if method is dispatched while not in any
     *             transaction context.
     */
    public boolean txMandatory(byte[] tid) throws EJBException;

    /**
     * Used to verify when a method with a MANDATORY transaction attribute is
     * called while calling thread is not currently associated with a global
     * transaction causes the container to throw a
     * javax.ejb.EJBTransactionRequiredException
     */
    public void txMandatory() throws EJBException;

    /**
     * Used to verify that when a method with a NEVER transaction attribute is
     * called causes the container to begin a local transaction.
     *
     * @return boolean true if method is dispatched in a local transaction.
     *         boolean false if method is dispatched in a global transaction.
     *
     * @throws java.lang.IllegalStateException
     *             is thrown if method is dispatched while not in any
     *             transaction context.
     */
    public boolean txNever() throws EJBException;

    /**
     * Used to verify when a method with a NEVER transaction attribute is called
     * while the thread is currently associated with a global transaction the
     * container throws a javax.ejb.EJBException. The caller must begin a global
     * transaction prior to calling this method.
     *
     */
    public void txNever(byte[] tid) throws EJBException;

    /**
     * Used to verify that when a method with a NOT_SUPPORTED transaction
     * attribute is called causes the container to begin a local transaction.
     *
     * @return boolean true if method is dispatched in a local transaction.
     *         boolean false if method is dispatched in a global transaction.
     *
     * @throws java.lang.IllegalStateException
     *             is thrown if method is dispatched while not in any
     *             transaction context.
     */
    public boolean txNotSupported() throws EJBException;

    /**
     * Used to verify that when a method with a SUPPORTS transaction attribute
     * is called while the calling thread is not associated with a global
     * transaction causes the container to begin a local transaction.
     *
     * @return boolean true if method is dispatched in a local transaction.
     *         boolean false if method is dispatched in a global transaction.
     *
     * @throws java.lang.IllegalStateException
     *             is thrown if method is dispatched while not in any
     *             transaction context.
     */
    public boolean txSupports() throws EJBException;

    /**
     * Used to verify that when a method with a SUPPORTS transaction attribute
     * is called while calling thread is currently associated with a global
     * transaction causes the container to dispatch the method in the caller's
     * global transaction context (e.g container does not begin a new
     * transaction). The caller must begin a global transaction prior to calling
     * this method.
     *
     * @param tid
     *            is the global transaction ID for the transaction that was
     *            started prior to calling this method.
     *
     * @return boolean true if method is dispatched in the same transaction
     *         context with the same transaction ID as passed by tid parameter.
     *         Otherwise boolean false is returned.
     *
     * @throws java.lang.IllegalStateException
     *             is thrown if method is dispatched while not in any
     *             transaction context.
     */
    public boolean txSupports(byte[] tid) throws EJBException;
}