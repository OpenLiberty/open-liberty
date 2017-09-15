/*******************************************************************************
 * Copyright (c) 1997, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.j2c;

import javax.resource.ResourceException;

/**
 * Interface name : TranWrapper
 * <p>
 * Scope : EJB server, WEB server
 * <p>
 * Object model : 1 concrete TranWrapper type per ManagedConnection
 * <p>
 * This interface is used by the ConnectionManager to make some generic calls
 * to a particular TranWrapper without knowing which particular type of
 * TranWrapper it's dealing with.
 */

public interface TranWrapper {

    /**
     * Tells the TranWrapper to register itself for synchronization calls
     * (beforeCompletion/afterCompletion) with the current transaction
     * (either Global or Local).
     * 
     * @return boolean true if registering for synchronization was done, false if not.
     * @exception ResourceException
     */
    abstract boolean addSync() throws ResourceException;

    /**
     * Tells the TranWrapper to enlist itself with the current transaction
     * (either Global or Local).
     * 
     * @exception ResourceException
     */
    abstract void enlist() throws ResourceException;

    /**
     * Tells the TranWrapper to delist itself from any involvement it
     * currently has in a transaction. This is for Local transactions only.
     * 
     * @exception ResourceException
     */
    abstract void delist() throws ResourceException;

    /**
     * Indicates whether the TranWrapper instance is RRS transactional
     * 
     * @exception ResourceException
     */
    abstract boolean isRRSTransactional();

}
