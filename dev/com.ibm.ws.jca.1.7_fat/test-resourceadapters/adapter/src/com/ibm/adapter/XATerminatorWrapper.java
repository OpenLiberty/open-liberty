/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter;

import javax.resource.spi.XATerminator;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 * @author swai
 *
 *         <p>
 *         This class wraps around the xaTerminator object from the bootstrap
 *         context of the FVTAdapterImpl.
 *         </p>
 *
 *         <p>
 *         The reason to have a wrapper for xaTerminator object is to allow the
 *         reset EISTimer when prepare call is made. Whenever a prepare call is
 *         made, that means the messageProvider is still working properly.
 *         That's why it's necessary to reset the timeLeft instance variable of
 *         the EISTimer.
 *         </p>
 *
 *         <p>
 *         Since XATerminatorWrapper will be called by EIS (messageProvider),
 *         reset the timer everytime commit/rollback/prepare/forget/recover is
 *         called.
 *         </p>
 *
 */
public class XATerminatorWrapper implements XATerminator {

    private static final TraceComponent tc = Tr
                    .register(XATerminatorWrapper.class);

    /** xaTerminator instance. */
    private final XATerminator xaTerm;

    /**
     * <p>
     * Constructor
     * </p>
     *
     * @param xaTerminator
     *                         the XATerminator instance from the context
     */

    public XATerminatorWrapper(XATerminator xaTerm) {

        if (tc.isEntryEnabled())
            Tr.entry(tc, "<init>", new Object[] { xaTerm });
        this.xaTerm = xaTerm;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "<init>", this);
    }

    /**
     * <p>
     * Commit the imported transaction
     * </p>
     *
     * @param xid
     *                     the Xid of the transaction to be commited
     * @param onePhase
     *                     Do one phase commit or not. True or false
     *
     */
    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        String msg;

        xaTerm.commit(xid, onePhase);

    }

    /**
     * <p>
     * Forget the imported transaction
     * </p>
     *
     * @param xid
     *                the Xid of the transaction to be commited
     *
     */
    @Override
    public void forget(Xid xid) throws XAException {

        xaTerm.forget(xid);
    }

    /**
     * <p>
     * Prepare the imported transaction
     * </p>
     *
     * @param xid
     *                the Xid of the transaction to be commited
     *
     */
    @Override
    public int prepare(Xid xid) throws XAException {

        boolean rc_active, rc_indoubt;
        String msg;

        int rc = xaTerm.prepare(xid);

        if (tc.isDebugEnabled())
            Tr.debug(tc, "prepare", "Return code is " + rc);

        // Add indoubt trans into SetIndoubtTrans

        return (rc);
    }

    /**
     * <p>
     * Rollback the imported transaction
     * </p>
     *
     * @param xid
     *                the Xid of the transaction to be commited
     *
     */
    @Override
    public void rollback(Xid xid) throws XAException {

        boolean rc_active, rc_indoubt;
        String msg;

        xaTerm.rollback(xid);

    }

    /**
     * <p>
     * Recover the imported transaction
     * </p>
     *
     * @param flag
     *                 - One of TMSTARTRSCAN, TMENDRSCAN, TMNOFLAGS.
     *
     */
    @Override
    public Xid[] recover(int flag) throws XAException {
        boolean listMatch = true;

        Xid[] xidList = xaTerm.recover(flag);

        return xidList;
    }

    /**
     * <p>
     * Return the xaTerminator instance from the bootstrap context from
     * application server
     * </p>
     *
     * @return XATerminator instance.
     */

    public XATerminator getNativeXaTerm() {
        return xaTerm;
    }

}
