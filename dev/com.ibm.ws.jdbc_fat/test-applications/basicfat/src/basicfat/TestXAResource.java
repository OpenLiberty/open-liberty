/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package basicfat;

import java.lang.reflect.Field;
import java.sql.Connection;
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
public class TestXAResource implements XAResource {
    /** Number of commits or rollbacks allowed before we simulate errors. */
    private final AtomicInteger successLimit;

    /** The real XAResource. */
    private final XAResource xaRes;

    /**
     * Construct a wrapper for the specified XA resource.
     *
     * @param xaRes XA resource to wrap.
     * @param successLimit limit of commits/rollbacks that should succeed for the group of
     *            XA resources before raising errors.
     */
    private TestXAResource(XAResource xaRes, AtomicInteger successLimit) {
        this.xaRes = xaRes;
        this.successLimit = successLimit;
    }

    /**
     * For a group of connections, replaces their XAResources with TestXAResource wrappers
     * and imposes a maximum of successful commits or rollbacks for the group as a whole.
     *
     * @param newLimit number of commits or rollbacks to allow before we simulate errors for
     *            XAResource.commit and XAResource.rollback.
     * @param connections a list of WSJdbcConnection.
     */
    public static final void assignSuccessLimit(int newLimit, Connection... connections) throws Exception {
        AtomicInteger limit = new AtomicInteger(newLimit);

        for (Connection con : connections) {
            Object mc = getFieldNuclear(con, "managedConn");

            XAResource wsXARes = (XAResource) mc.getClass().getMethod("getXAResource").invoke(mc);

            Class<?> WSRdbXaResourceImpl = wsXARes.getClass();
            Field WSRdbXaResourceImpl_ivXaRes = WSRdbXaResourceImpl.getDeclaredField("ivXaRes");
            WSRdbXaResourceImpl_ivXaRes.setAccessible(true);

            XAResource realXARes = (XAResource) WSRdbXaResourceImpl_ivXaRes.get(wsXARes);
            XAResource testXARes = new TestXAResource(realXARes, limit);
            WSRdbXaResourceImpl_ivXaRes.set(wsXARes, testXARes);
        }
    }

    /**
     * Allow the commit (delegate to the real XA resource) if we haven't exceeded the limit
     * of commit/rollback attempts. If the limit is exceeded, then raise an error.
     */
    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        if (successLimit.getAndDecrement() > 0)
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
     * For a group of connections, restore their original XAResources, so that any
     * success limit no longer interferes with normal use.
     *
     * @param connections a list of WSJdbcConnection.
     */
    public static final void removeSuccessLimit(Connection... connections) throws Exception {
        for (Connection con : connections) {
            Object mc = getFieldNuclear(con, "managedConn");
            if (mc == null)
                break;
            Object wsXARes = getFieldNuclear(mc, "xares");
            if (wsXARes == null)
                break; // nothing to replace

            Class<?> WSRdbXaResourceImpl = wsXARes.getClass();
            Field WSRdbXaResourceImpl_ivXaRes = WSRdbXaResourceImpl.getDeclaredField("ivXaRes");
            WSRdbXaResourceImpl_ivXaRes.setAccessible(true);

            XAResource testXARes = (XAResource) WSRdbXaResourceImpl_ivXaRes.get(wsXARes);
            if (testXARes instanceof TestXAResource) {
                XAResource realXARes = ((TestXAResource) testXARes).xaRes;
                WSRdbXaResourceImpl_ivXaRes.set(wsXARes, realXARes);
            }
        }
    }

    /**
     * Allow the rollback (delegate to the real XA resource) if we haven't exceeded the limit
     * of commit/rollback attempts. If the limit is exceeded, then raise an error.
     */
    @Override
    public void rollback(Xid xid) throws XAException {
        if (successLimit.getAndDecrement() > 0)
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

    private static Object getFieldNuclear(Object obj, String fName) {
        for (Class<?> clazz = obj.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                Field f1 = clazz.getDeclaredField(fName);
                f1.setAccessible(true);
                return f1.get(obj);
            } catch (Exception ignore) {
            }
        }
        throw new RuntimeException("Did not find field '" + fName + "' on " + obj.getClass());
    }
}