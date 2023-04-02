/*******************************************************************************
 * Copyright (c) 2012,2022 IBM Corporation and others.
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
package test.jca.adapter;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * Wrapper for XAResource that allows us to simulate errors on XAResource.commit/rollback after
 * these methods have been invoked a specified number of times. We use this to cause in-doubt
 * transactions that require recovery.
 *
 * For example, if we have 3 resources participating in a transaction,
 * and set the commit/rollback limit to 2. When the test case does
 * transaction.commit, the transaction manager will prepare all 3 resources, and then it will
 * attempt to commit them. The first two will commit successfully (because they are within the
 * limit) but the third will fail, leaving the transaction in an in-doubt state.
 */
public class BVTXAResource implements XAResource {

    private final BVTManagedConnectionFactory mcf;
    private final XAResource xaRes;

    BVTXAResource(XAResource xaRes, BVTManagedConnectionFactory mcf) {
        this.mcf = mcf;
        this.xaRes = xaRes;
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        if (mcf.adapter.xaResourceSuccessLimit.getAndDecrement() > 0)
            xaRes.commit(xid, onePhase);
        else {
            XAException x = new XAException("Simulating an error for commit.");
            x.errorCode = XAException.XAER_RMFAIL;
            throw x;
        }
    }

    @Override
    public void end(Xid xid, int flags) throws XAException {
        xaRes.end(xid, flags);
    }

    @Override
    public void forget(Xid xid) throws XAException {
        xaRes.forget(xid);
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        return xaRes.getTransactionTimeout();
    }

    @Override
    public boolean isSameRM(XAResource r) throws XAException {
        return xaRes.isSameRM(r);
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        return xaRes.prepare(xid);
    }

    @Override
    public Xid[] recover(int flag) throws XAException {
        return xaRes.recover(flag);
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        if (mcf.adapter.xaResourceSuccessLimit.getAndDecrement() > 0)
            xaRes.rollback(xid);
        else {
            XAException x = new XAException("Simulating an error for rollback.");
            x.errorCode = XAException.XAER_RMFAIL;
            throw x;
        }
    }

    @Override
    public boolean setTransactionTimeout(int s) throws XAException {
        return xaRes.setTransactionTimeout(s);
    }

    @Override
    public void start(Xid xid, int flags) throws XAException {
        xaRes.start(xid, flags);
    }
}
