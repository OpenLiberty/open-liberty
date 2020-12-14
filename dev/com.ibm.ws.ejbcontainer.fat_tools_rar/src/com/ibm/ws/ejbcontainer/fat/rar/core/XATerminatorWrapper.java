/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.fat.rar.core;

import java.util.logging.Logger;

import javax.resource.ResourceException;
import javax.resource.spi.XATerminator;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.ws.ejbcontainer.fat.rar.work.FVTWorkDispatcher;

/**
 * <p>This class wraps around the xaTerminator object from the bootstrap context
 * of the FVTAdapterImpl.</p>
 *
 * <p>The reason to have a wrapper for xaTerminator object is to allow the reset
 * EISTimer when prepare call is made. Whenever a prepare call is made, that means
 * the messageProvider is still working properly. That's why it's necessary to
 * reset the timeLeft instance variable of the EISTimer.</p>
 *
 * <p>Since XATerminatorWrapper will be called by EIS (messageProvider), reset the
 * timer everytime commit/rollback/prepare/forget/recover is called.</p>
 */
public class XATerminatorWrapper implements XATerminator {
    private final static String CLASSNAME = XATerminatorWrapper.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /** xaTerminator instance. */
    private final XATerminator xaTerm;

    /** EISTimer instance. */
    private EISTimer eisTimer;

    /** WorkDispatcher instance. */
    private FVTWorkDispatcher workDispatcher;

    /**
     * <p>Constructor</p>
     *
     * @param xaTerminator the XATerminator instance from the context
     */

    public XATerminatorWrapper(XATerminator xaTerm) {
        svLogger.entering(CLASSNAME, "<init>", new Object[] { xaTerm });
        this.xaTerm = xaTerm;
        svLogger.exiting(CLASSNAME, "<init>", this);
    }

    /**
     * <p>Commit the imported transaction</p>
     *
     * @param xid the Xid of the transaction to be commited
     * @param onePhase Do one phase commit or not. True or false
     *
     */
    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        String msg;
        // The messageProvider makes commit call. Reset the timer.
        eisTimer.resetTimeLeft();
        xaTerm.commit(xid, onePhase);

        // Remove indoubt trans from SetIndoubtTrans after successful commit
        try {
            if (onePhase) {
                // If the commit is one phase, then the Tx is not prepared
                // and the trans is still in active state. So if commit one
                // phase is successful, remove this xid from active trans
                // set.
                if (!workDispatcher.removeActiveTransFromSet(xid)) {
                    msg = "Xid doesn't exist in active list.";
                    throw new ResourceException(msg);
                }
            } else {
                // If the commit is two phase, then the Tx is prepared
                // and the trans is in indoubt state. So if commit two
                // phase is successful, remove this xid from indoubt trans
                // set.
                if (!workDispatcher.removeIndoubtTransFromSet(xid)) {
                    msg = "Xid doesn't exist in indoubt list.";
                    throw new ResourceException(msg);
                }
            }
        } catch (ResourceException re) {
            throw new XAException(re.getMessage());
        }
    }

    /**
     * <p>Forget the imported transaction</p>
     *
     * @param xid the Xid of the transaction to be commited
     *
     */
    @Override
    public void forget(Xid xid) throws XAException {
        // The messageProvider makes forget call. Reset the timer.
        eisTimer.resetTimeLeft();
        xaTerm.forget(xid);
    }

    /**
     * <p>Prepare the imported transaction</p>
     *
     * @param xid the Xid of the transaction to be commited
     *
     */
    @Override
    public int prepare(Xid xid) throws XAException {
        boolean rc_active, rc_indoubt;
        String msg;

        // The messageProvider makes prepare call. Reset the timer.
        eisTimer.resetTimeLeft();
        int rc = xaTerm.prepare(xid);
        svLogger.info("prepare: Return code is " + rc);

        // Add indoubt trans into SetIndoubtTrans
        if (rc == XAResource.XA_OK) {
            try {
                if (!workDispatcher.addIndoubtTransToSet(xid)) {
                    msg = "Xid already exist in indoubt list.";
                    throw new ResourceException(msg);
                }
                if (!workDispatcher.removeActiveTransFromSet(xid)) {
                    msg = "Xid doesn't exist in active list.";
                    throw new ResourceException(msg);
                }
            } catch (ResourceException re) {
                throw new XAException(re.getMessage());
            }
        } else if (rc == XAResource.XA_RDONLY) {
            // Since the rc is RDONLY, this means the tx is already committed.
            // Remove the tx from the active Trans set.
            try {
                if (!workDispatcher.removeActiveTransFromSet(xid)) {
                    msg = "Xid doesn't exist in active list even the tx is commited.";
                    throw new ResourceException(msg);
                }
            } catch (ResourceException re) {
                throw new XAException(re.getMessage());
            }
        }
        return (rc);
    }

    /**
     * <p>Rollback the imported transaction</p>
     *
     * @param xid the Xid of the transaction to be committed
     *
     */
    @Override
    public void rollback(Xid xid) throws XAException {
        boolean rc_active, rc_indoubt;
        String msg;
        // The messageProvider makes rollback call. Reset the timer.
        eisTimer.resetTimeLeft();
        xaTerm.rollback(xid);

        // Remove active trans from SetIActiveTrans after successful rollback
        try {
            rc_active = workDispatcher.removeActiveTransFromSet(xid);
            rc_indoubt = workDispatcher.removeIndoubtTransFromSet(xid);

            if (!(rc_active ^ rc_indoubt)) {
                msg = "Xid is NOT in either active and indoubt list.";

                if (rc_active) {
                    msg = "Xid is in both active and indoubt list.";
                }
                throw new ResourceException(msg);
            }
        } catch (ResourceException re) {
            throw new XAException(re.getMessage());
        }
    }

    /**
     * <p>Recover the imported transaction</p>
     *
     * @param flag - One of TMSTARTRSCAN, TMENDRSCAN, TMNOFLAGS.
     *
     */
    @Override
    public Xid[] recover(int flag) throws XAException {
        boolean listMatch = true;

        // The messageProvider makes recover call. Reset the timer.
        eisTimer.resetTimeLeft();
        Xid[] xidList = xaTerm.recover(flag);

        // Compare the list of xid from app server with the SetIndoubt Trans
        // from the TRA
        try {
            listMatch = workDispatcher.verifyIndoubtTrans(xidList);
            if (!listMatch) {
                svLogger.info("recover: List of indoubt trans is different from that of TRA.");
                throw new XAException("List of indoubt trans is different from that of TRA.");
            }
        } catch (ResourceException re) {
            // Chain ResourceException to XAException
            // ResourceException is caused by removeIndoubtTransFromSet and
            // this should not happen.
            {
                svLogger.info("recover: Unexpected exception. ERROR!");
            }
            throw new XAException(re.getMessage());
        }
        return xidList;
    }

    /**
     * <p>Return the xaTerminator instance from the bootstrap context from application server</p>
     *
     * @return XATerminator instance.
     */
    public XATerminator getNativeXaTerm() {
        return xaTerm;
    }

    /**
     * <p>Assign the EISTimer instance to this xaTermWrapper instance</p>
     *
     */
    public void setEISTimer(EISTimer eisTimer) {
        this.eisTimer = eisTimer;
    }

    /**
     * <p>Assign the WorkDispatcher instance to this xaTermWrapper instance</p>
     *
     */
    public void setWorkDispatcher(FVTWorkDispatcher workDispatcher) {
        this.workDispatcher = workDispatcher;
    }
}