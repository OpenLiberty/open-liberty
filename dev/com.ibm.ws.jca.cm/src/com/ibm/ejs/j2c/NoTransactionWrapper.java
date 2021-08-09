/*******************************************************************************
 * Copyright (c) 2001, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.j2c;

/*
 * Class name   : NoTransactionWrapper
 *
 * Scope        : EJB server
 *
 * Object model : 1 instance per MCWrapper (if required)
 *
 * NoTransactionWrapper is a wrapper for a resource adapter that has no transactional capability.  It is
 * not enlisted with a transaction manager and does not support any XAResource methods. As of defect
 * 129832, it also been decided that NoTransactionWrappers should no longer be registered for
 * synchronization.  Since we decided that NoTransaction RAs should be non-shareable regardless of the
 * <res-sharing-scope> setting, it doesn't make sense to register for synchronization.  We will just do
 * the proper cleanup based on the closing the handles.
 *
 * Because of the massive changes due to the "non synchronization registration", old code will be deleted
 * from this source file and not just commented out.  Reference a previous version of this file to see that
 * lines were deleted and/or changed.
 */

import javax.resource.ResourceException;

import com.ibm.ws.j2c.TranWrapper;

class NoTransactionWrapper implements TranWrapper {

    private String _hexString = ""; 

    /*
     * Constructor
     */

    protected NoTransactionWrapper() {
        _hexString = Integer.toHexString(this.hashCode()); 
    }

    /**
     * Reinitializes its own state such that it may be placed in the PoolManagers
     * pool for reuse.
     */
    public void cleanup() {
        // nothing to cleanup
    }

    /**
     * Reinitializes its own state such that it may be reused.
     */
    public void releaseResources() {
        // nothing to release.
    }

    public Object getCoordinator() {
        return null; // no coordinator
    }

    @Override
    public boolean addSync() throws ResourceException {
        // no-op for NoTransaction resources now that we don't register for synchronization
        return false;
    }

    @Override
    public void enlist() throws ResourceException {
        // noop for NoTransaction resource.
    }

    @Override
    public void delist() throws ResourceException {
        // noop for NoTransaction resource.
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();

        buf.append("NoTransactionWrapper@");
        buf.append(_hexString);

        return buf.toString();
    }

    /**
     * Indicates whether this TranWrapper implementation instance is RRS transactional
     * 
     * @exception ResourceException
     */
    @Override
    public boolean isRRSTransactional() {
        return false;
    }

}
