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
package com.ibm.ejs.j2c;

import javax.resource.spi.ManagedConnection;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.ws.jca.adapter.WSXAResource;

/**
 * Wrapper for XAResource that keeps tracks of the ManagedConnection that created it.
 * This is intended for use with XAResourceFactory, which has a method getXAResource
 * to obtain an XAResource, which it later cleans up via a destroyXAResource method.
 * JCA requires the ManagedConnection in order to implement the destroy.
 */
public class WSXAResourceImpl implements WSXAResource {

    /**
     * Managed connection that created the XAResource.
     */
    private final ManagedConnection mc;

    /**
     * The XA resource.
     */
    private final XAResource xa;

    /**
     * Construct an XAResource wrapper.
     * 
     * @param mc managed connection that created the XAResource
     * @param xa the XA resource
     */
    public WSXAResourceImpl(ManagedConnection mc, XAResource xa) {
        this.mc = mc;
        this.xa = xa;
    }

    /** {@inheritDoc} */
    @Override
    public final void commit(Xid xid, boolean onePhase) throws XAException {
        xa.commit(xid, onePhase);
    }

    /** {@inheritDoc} */
    @Override
    public final void end(Xid xid, int flags) throws XAException {
        xa.end(xid, flags);
    }

    /** {@inheritDoc} */
    @Override
    public final void forget(Xid xid) throws XAException {
        xa.forget(xid);
    }

    /** {@inheritDoc} */
    @Override
    public final int getTransactionTimeout() throws XAException {
        return xa.getTransactionTimeout();
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isSameRM(XAResource theXAResource) throws XAException {
        return xa.isSameRM(theXAResource);
    }

    /** {@inheritDoc} */
    @Override
    public final int prepare(Xid xid) throws XAException {
        return xa.prepare(xid);
    }

    /** {@inheritDoc} */
    @Override
    public final Xid[] recover(int flag) throws XAException {
        return xa.recover(flag);
    }

    /** {@inheritDoc} */
    @Override
    public final void rollback(Xid xid) throws XAException {
        xa.rollback(xid);
    }

    /** {@inheritDoc} */
    @Override
    public boolean setTransactionTimeout(int seconds) throws XAException {
        return xa.setTransactionTimeout(seconds);
    }

    /** {@inheritDoc} */
    @Override
    public final void start(Xid xid, int flags) throws XAException {
        xa.start(xid, flags);
    }

    /** {@inheritDoc} */
    @Override
    public final ManagedConnection getManagedConnection() {
        return mc;
    }
}
