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

import java.io.Serializable;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.tx.jta.DestroyXAResourceException;
import com.ibm.tx.jta.XAResourceFactory;
import com.ibm.tx.jta.XAResourceNotAvailableException;

/**
 * XAResourceFactory to be used in conjunction with the mock objects in the
 * native transaction manager tests.
 */
public class NoOpXAResourceFactory implements XAResourceFactory {

    /** {@inheritDoc} */
    @Override
    public void destroyXAResource(XAResource arg0) throws DestroyXAResourceException {
        // Nothing to do.
    }

    /** {@inheritDoc} */
    @Override
    public XAResource getXAResource(Serializable arg0) throws XAResourceNotAvailableException {
        /* NoOpXAResource doesn't do anything. */
        return new XAResource() {

            @Override
            public void commit(Xid xid, boolean onePhase) throws XAException {
            }

            @Override
            public void end(Xid xid, int flags) throws XAException {
                throw new XAException("Should not get called for end");
            }

            @Override
            public void forget(Xid xid) throws XAException {
            }

            @Override
            public int getTransactionTimeout() throws XAException {
                throw new XAException("Should not get called for getTransactionTimeout");
            }

            @Override
            public boolean isSameRM(XAResource theXAResource) throws XAException {
                return false;
            }

            @Override
            public int prepare(Xid xid) throws XAException {
                throw new XAException("Should not get called for prepare");
            }

            @Override
            public Xid[] recover(int flag) throws XAException {
                return new Xid[0];
            }

            @Override
            public void rollback(Xid xid) throws XAException {
            }

            @Override
            public boolean setTransactionTimeout(int seconds) throws XAException {
                throw new XAException("Should not get called for setTransactionTimeout");
            }

            @Override
            public void start(Xid xid, int flags) throws XAException {
                throw new XAException("Should not get called for start");
            }

        };
    }
}
