package com.ibm.ws.Transaction;

import javax.transaction.xa.Xid;

import com.ibm.wsspi.tx.UOWEventEmitter;

/*******************************************************************************
 * Copyright (c) 2002, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * 
 * <p> This interface is private to WAS.
 * Any use of this interface outside the WAS Express/ND codebase
 * is not supported.
 * 
 */
public interface UOWCoordinator extends UOWEventEmitter
{
    public static final int TXTYPE_LOCAL = 0; // A local transaction
    public static final int TXTYPE_INTEROP_GLOBAL = 1; // OTS-compliant global transaction
    public static final int TXTYPE_NONINTEROP_GLOBAL = 2; // Non-OTS-compliant global transaction
    public static final int TXTYPE_ACTIVITYSESSION = 3; // An ActivitySession

    boolean isGlobal();

    byte[] getTID();

    public Xid getXid();

    /**
     * Indicates the <i>type</i> of transaction on the thread.
     * This method is called by the EJB TranStrategy collaborators
     * to determine how to proceed with method dispatch, based on the application-configured
     * transaction attribute.
     * 
     * @return the <i>type</i> of transaction on the thread where the types are
     *         <dl>
     *         <dd><b>LOCAL</b>
     *         <dt>A local transaction.
     *         <dd><b>INTEROP_GLOBAL</b>
     *         <dt>an OTS compliant global transaction
     *         <dd><b>NONINTEROP_GLOBAL</b>
     *         <dt>a global transaction received from a foreign EJS that does not support
     *         transactional interoperability
     *         </dl>
     */
    public int getTxType();

    public boolean getRollbackOnly();

}
