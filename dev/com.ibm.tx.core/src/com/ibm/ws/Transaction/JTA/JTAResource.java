package com.ibm.ws.Transaction.JTA;
/*******************************************************************************
 * Copyright (c) 2002, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.ws.recoverylog.spi.RecoverableUnitSection;

public interface JTAResource extends StatefulResource
{
    /** The underlying XAResource is not enlisted in a transaction. */
    public static final int NOT_ASSOCIATED = 0;

    /** The underlying XAResource has been suspended. */
    public static final int SUSPENDED = 1;

    /** The underlying XAResource is currently involved in an active transaction. */
    public static final int ACTIVE = 2;

    /** The underlying XAResource is in a failed state */
    public static final int FAILED = 3;

    /** The underlying XAResource is in a rollback only state */
    public static final int ROLLBACK_ONLY = 4;

    /** The underlying XAResource is in an idle state awaiting completion */
    public static final int IDLE = 5;
    
    /** The underlying XAResource is not enlisted in a transaction, should
     *  use the TMJOIN when the resource gets issued start called. */
    public static final int NOT_ASSOCIATED_AND_TMJOIN = 6;

    public static final int DEFAULT_COMMIT_PRIORITY = 0;

    public static final int LAST_IN_COMMIT_PRIORITY = Integer.MIN_VALUE;

    public void start() throws XAException;

    public void end(int flag) throws XAException;
    
    public int  prepare() throws XAException;

    public void commit() throws XAException;

    public void commit_one_phase() throws XAException;

    public void rollback() throws XAException;
    
    public void forget() throws XAException;

    public Xid getXID();
    public int getState();
    public XAResource XAResource();

    public void destroy();

    public void setState(int state);

    public void log(RecoverableUnitSection rus) throws javax.transaction.SystemException;
    
    public String describe();

    public int getPriority();

    public enum JTAResourceVote
    {
        commit,
        rollback,
        readonly,
        heuristic,
        none;
    }
}