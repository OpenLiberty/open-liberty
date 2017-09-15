/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.adapter;

import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.Executor;

import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;

/**
 * WebSphere Application Server extensions to the ManagedConnection interface.
 */
public abstract class WSManagedConnection implements ManagedConnection {
    /**
     * Invoke to abort a connection that may be stuck waiting for a net work response or
     * the database to respond.
     *
     * @throws SQLFeatureNotSupportedException
     */
    public void abort(Executor e) throws SQLFeatureNotSupportedException {}

    /**
     * isAborted will return true if the connection was aborted.
     */
    public boolean isAborted() {
        return false;
    }

    /**
     * Invoked after completion of a z/OS RRS (Resource Recovery Services) global transaction.
     */
    public void afterCompletionRRS() {}

    /**
     * Invoked when enlisting in a z/OS RRS (Resource Recovery Services) global transaction.
     */
    public void enlistRRS() {}

    /**
     * Returns ConnectionRequestInfo reflecting the current state of this connection.
     *
     * @return ConnectionRequestInfo reflecting the current state of this connection.
     */
    public abstract ConnectionRequestInfo getConnectionRequestInfo();

    /**
     * Indicates whether or not this managed connection should enlist in application server managed transactions.
     *
     * @return true if this connection should be enlisted. False if it should not be enlisted.
     */
    public boolean isTransactional() {
        return true;
    }

    /**
     * Marks the managed connection as stale.
     */
    public void markStale() {}

    /**
     * Claim the unused managed connection as a victim connection,
     * which can then be reauthenticated and reused.
     */
    public void setClaimedVictim() {}
}