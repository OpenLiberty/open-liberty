/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fat.derbyra.resourceadapter;

import java.util.concurrent.atomic.AtomicInteger;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * Wrapper for XAResource that allows us to simulate errors on XAResource.commit/rollback after
 * these methods have been invoked a specified number of times. We use this to cause in-doubt
 * transactions that require recovery.
 *
 * For example, if we have 3 resources participating in a transaction, we can wrap each of them
 * with TestXAResource and set the commit/rollback limit to 2. When the test case does
 * transaction.commit, the transaction manager will prepare all 3 resources, and then it will
 * attempt to commit them. The first two will commit successfully (because they are within the
 * limit) but the third will fail, leaving the transaction in an in-doubt state.
 */
public class DerbyXAResource implements XAResource {
    /**
     * Number of commits or rollbacks allowed before we simulate errors.
     */
    private AtomicInteger successLimit;

    /**
     * The real XAResource.
     */
    private final XAResource xaRes;

    /**
     * Construct a wrapper for the specified XA resource.
     *
     * @param xaRes XA resource to wrap.
     * @param successLimit limit of commits/rollbacks that should succeed for the group of
     *            XA resources before raising errors.
     */
    DerbyXAResource(XAResource xaRes, AtomicInteger successLimit) {
        this.xaRes = xaRes;
        this.successLimit = successLimit;
    }

    /**
     * Allow the commit (delegate to the real XA resource) if we haven't exceeded the limit
     * of commit/rollback attempts. If the limit is exceeded, then raise an error.
     */
    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        if (successLimit == null || successLimit.getAndDecrement() > 0)
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

    /**
     * Allow the rollback (delegate to the real XA resource) if we haven't exceeded the limit
     * of commit/rollback attempts. If the limit is exceeded, then raise an error.
     */
    @Override
    public void rollback(Xid xid) throws XAException {
        if (successLimit == null || successLimit.getAndDecrement() > 0)
            xaRes.rollback(xid);
        else {
            successLimit = null; // TODO remove this when
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