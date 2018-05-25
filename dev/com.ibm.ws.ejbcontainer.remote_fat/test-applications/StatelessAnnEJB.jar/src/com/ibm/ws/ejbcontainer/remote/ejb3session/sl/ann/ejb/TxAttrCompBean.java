/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb;

import static javax.ejb.TransactionAttributeType.MANDATORY;
import static javax.ejb.TransactionAttributeType.NEVER;
import static javax.ejb.TransactionAttributeType.NOT_SUPPORTED;
import static javax.ejb.TransactionAttributeType.REQUIRED;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;
import static javax.ejb.TransactionAttributeType.SUPPORTS;
import static javax.ejb.TransactionManagementType.CONTAINER;

import javax.ejb.CreateException;
import javax.ejb.LocalHome;
import javax.ejb.Remote;
import javax.ejb.RemoteHome;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionManagement;

import com.ibm.websphere.ejbcontainer.test.tools.FATTransactionHelper;

/**
 * Bean implementation class for Enterprise Bean: TxAttrComp
 **/
@SuppressWarnings("serial")
@Stateless(name = "TxAttrComp")
@LocalHome(TxAttrEJBLocalHome.class)
@Remote(TxAttrRemote.class)
@RemoteHome(TxAttrEJBHome.class)
@TransactionManagement(CONTAINER)
public class TxAttrCompBean implements SessionBean {
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
    @TransactionAttribute(REQUIRED)
    public boolean txRequired() {
        return FATTransactionHelper.isTransactionGlobal();
    }

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
    @TransactionAttribute(REQUIRED)
    public boolean txRequired(byte[] tid) {
        return FATTransactionHelper.isSameTransactionId(tid);
    }

    /**
     * Used to verify when a method with a MANDATORY transaction attribute is
     * called while thread is currently associated with a global transaction
     * causes the container to dispatch the method in the callers global
     * transaction context (e.g container does not begin a new transaction). The
     * caller must begin a global transaction prior to calling this method.
     *
     * @throws java.lang.IllegalStateException
     *             is thrown if method is dispatched while not in any
     *             transaction context.
     */
    @TransactionAttribute(MANDATORY)
    public boolean txMandatory(byte[] tid) {
        return FATTransactionHelper.isSameTransactionId(tid);
    }

    /**
     * Used to verify when a method with a MANDATORY transaction attribute is
     * called while calling thread is not currently associated with a global
     * transaction causes the container to throw a: -
     * javax.ejb.EJBTransactionRequiredException if using the EJB3.0
     * implementation; - javax.ejb.TransactionRequiredLocalExeption if using 2.1
     * local client - javax.transaction.TransactionRequiredException if using
     * a2.1 remote client
     *
     */
    @TransactionAttribute(MANDATORY)
    public void txMandatory() {}

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
     * @return boolean true if method is dispatched in a global tranaction with
     *         a global transaction ID that does not match the tid parameter.
     *         Otherwise boolean false is returned.
     *
     * @throws java.lang.IllegalStateException
     *             is thrown if method is dispatched while not in any
     *             transaction context.
     */
    @TransactionAttribute(REQUIRES_NEW)
    public boolean txRequiresNew(byte[] tid) {
        byte[] myTid = FATTransactionHelper.getTransactionId();
        if (myTid == null) {
            return false;
        }

        return (FATTransactionHelper.isSameTransactionId(tid) == false); // d186905
    }

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
    @TransactionAttribute(REQUIRES_NEW)
    public boolean txRequiresNew() {
        return FATTransactionHelper.isTransactionGlobal();
    }

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
    @TransactionAttribute(NEVER)
    public boolean txNever() {
        return FATTransactionHelper.isTransactionLocal();
    }

    /**
     * Used to verify when a method with a NEVER transaction attribute is called
     * while the thread is currently associated with a global transaction the
     * container throws a: - javax.ejb.EJBException if using an EJB3.0
     * implementation - java.rmi.RemoteException if using a 2.1 remote client -
     * javax.ejb.EJBException if using a 2.1 local client The caller must begin
     * a global transaction prior to calling this method.
     *
     */
    @TransactionAttribute(NEVER)
    public void txNever(byte[] tid) {}

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
    @TransactionAttribute(NOT_SUPPORTED)
    public boolean txNotSupported() {
        return FATTransactionHelper.isTransactionLocal();
    }

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
    @TransactionAttribute(SUPPORTS)
    public boolean txSupports() {
        return FATTransactionHelper.isTransactionLocal();
    }

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
    @TransactionAttribute(SUPPORTS)
    public boolean txSupports(byte[] tid) {
        return FATTransactionHelper.isSameTransactionId(tid);
    }

    public void ejbCreate() throws CreateException {}

    @Override
    public void ejbRemove() {}

    @Override
    public void ejbActivate() {}

    @Override
    public void ejbPassivate() {}

    @Override
    public void setSessionContext(SessionContext sc) {}
}