/*******************************************************************************
 * Copyright (c) 2003, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.exception2x.ejb;

import java.rmi.AccessException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.logging.Logger;

import javax.activity.ActivityCompletedException;
import javax.activity.ActivityRequiredException;
import javax.activity.InvalidActivityException;
import javax.ejb.CreateException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.transaction.InvalidTransactionException;
import javax.transaction.TransactionRequiredException;
import javax.transaction.TransactionRolledbackException;

/**
 * This is an Entity Bean class with CMP fields
 */
@SuppressWarnings({ "null", "serial", "unused" })
public class TestExBean implements SessionBean {
    private static final String CLASS_NAME = TestExBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    private SessionContext sessionContext = null;
    public int i_value;

    /**
     * ejbActivate method comment
     *
     * @exception RemoteException The exception description.
     */
    @Override
    public void ejbActivate() throws RemoteException {
    }

    /**
     * ejbCreate method
     *
     * @param value int
     * @exception CreateException The exception description.
     */
    public void ejbCreate(int value) throws CreateException {
        i_value = value;
    }

    /**
     * ejbCreate method
     *
     * @param argId int
     * @param d1 int
     * @exception CreateException The exception description.
     */
    public void ejbCreate(int argId, int d1) throws CreateException {
        ejbCreate(argId);
        throw new NullPointerException("ejbCreate throwing NPE.");
    }

    /**
     * ejbPassivate method comment
     *
     * @exception RemoteException The exception description.
     */
    @Override
    public void ejbPassivate() throws RemoteException {
    }

    /**
     * ejbRemove method comment
     *
     * @exception RemoteException The exception description.
     * @exception RemoveException The exception description.
     */
    @Override
    public void ejbRemove() throws RemoteException {
    }

    /**
     * getSessionContext method comment
     *
     * @return SessionContext
     */
    public SessionContext getSessionContext() {
        return sessionContext;
    }

    /**
     * setSessionContext method comment
     *
     * @param ctx SessionContext
     * @exception RemoteException The exception description.
     */
    @Override
    public void setSessionContext(SessionContext ctx) throws RemoteException {
        sessionContext = ctx;
    }

    /**
     * unsetSessionContext method comment
     *
     * @exception RemoteException The exception description.
     */
    public void unsetSessionContext() throws RemoteException {
        sessionContext = null;
    }

    /*
     * -----
     */
    public int addMore(int moreValue) {
        return i_value + moreValue;
    }

    /*
     * -----
     */
    public void ejbHomeMyHomeMethod(int value) throws RemoteException {
        // do something with value
        svLogger.info("I am in myHomeMethod(" + value + ")");
    }

    /**
     * throwNoSuchObjectException method comment
     *
     * @exception NoSuchObjectException
     */
    public void throwNoSuchObjectException(String s) throws RemoteException {
        throw new NoSuchObjectException(s);
    }

    /**
     * throwRuntimeException method comment
     *
     * @exception RuntimeException
     */
    public void throwRuntimeException(String s) throws RemoteException {
        throw new RuntimeException(s);
    }

    /**
     * throwTransactionRequiredException method comment
     *
     * @exception TransactionRequiredException
     */
    public void throwTransactionRequiredException(String s) throws RemoteException {
        try {
            Object o = null;
            String x = o.toString();
        } catch (NullPointerException npe) {
            TransactionRequiredException ex = new TransactionRequiredException(s);
            ex.detail = npe;
            throw ex;
        }
    }

    /**
     * throwTransactionRolledbackException method comment
     *
     * @exception TransactionRolledbackException
     */
    public void throwTransactionRolledbackException(String s) throws RemoteException {
        try {
            Object o = null;
            String x = o.toString();
        } catch (NullPointerException npe) {
            TransactionRolledbackException ex = new TransactionRolledbackException(s);
            ex.detail = npe;
            throw ex;
        }
    }

    /**
     * throwInvalidTransactionException method comment
     *
     * @exception InvalidTransactionException
     */
    public void throwInvalidTransactionException(String s) throws RemoteException {
        try {
            Object o = null;
            String x = o.toString();
        } catch (NullPointerException npe) {
            InvalidTransactionException ex = new InvalidTransactionException(s);
            ex.detail = npe;
            throw ex;
        }
    }

    /**
     * throwAccessException method comment
     *
     * @exception AccessException
     */
    public void throwAccessException(String s) throws RemoteException {
        try {
            Object o = null;
            String x = o.toString();
        } catch (NullPointerException npe) {
            AccessException ex = new AccessException(s);
            ex.detail = npe;
            throw ex;
        }
    }

    /**
     * throwActivityRequiredException method comment
     *
     * @exception ActivityRequiredException
     */
    public void throwActivityRequiredException(String s) throws RemoteException {
        try {
            Object o = null;
            String x = o.toString();
        } catch (NullPointerException npe) {
            ActivityRequiredException ex = new ActivityRequiredException(s);
            ex.detail = npe;
            throw ex;
        }
    }

    /**
     * throwInvalidActivityException method comment
     *
     * @exception InvalidActivityException
     */
    public void throwInvalidActivityException(String s) throws RemoteException {
        try {
            Object o = null;
            String x = o.toString();
        } catch (NullPointerException npe) {
            InvalidActivityException ex = new InvalidActivityException(s);
            ex.detail = npe;
            throw ex;
        }
    }

    /**
     * throwActivityCompletedException method comment
     *
     * @exception ActivityCompletedException
     */
    public void throwActivityCompletedException(String s) throws RemoteException {
        try {
            Object o = null;
            String x = o.toString();
        } catch (NullPointerException npe) {
            ActivityCompletedException ex = new ActivityCompletedException(s);
            ex.detail = npe;
            throw ex;
        }
    }
}