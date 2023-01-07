/*******************************************************************************
 * Copyright (c) 2012,2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jca.adapter;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Queue;

import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.cci.Connection;
import jakarta.resource.cci.ConnectionMetaData;
import jakarta.resource.cci.Interaction;
import jakarta.resource.cci.LocalTransaction;
import jakarta.resource.cci.ResultSetInfo;
import jakarta.resource.spi.ConnectionEvent;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.IllegalStateException;

public class BVTConnection implements Connection, ConnectionMetaData, LocalTransaction {
    ConnectionManager cm;
    BVTManagedConnection mc;
    Queue<BVTInteraction> interactions = new LinkedList<BVTInteraction>();

    BVTConnection(BVTManagedConnection mc) {
        this.mc = mc;
    }

    /** {@inheritDoc} */
    @Override
    public void begin() throws ResourceException {
        try {
            mc.con.setAutoCommit(false);
            mc.notify(ConnectionEvent.LOCAL_TRANSACTION_STARTED, this, null);
        } catch (SQLException x) {
            throw new ResourceException(x);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws ResourceException {
        if (mc == null)
            throw new IllegalStateException("Already closed");
        mc.notify(ConnectionEvent.CONNECTION_CLOSED, this, null);
        mc = null;
        for (Interaction i = interactions.poll(); i != null; i = interactions.poll())
            i.close();
        interactions = null;
    }

    /** {@inheritDoc} */
    @Override
    public void commit() throws ResourceException {
        try {
            mc.con.commit();
            mc.con.setAutoCommit(true);
            mc.notify(ConnectionEvent.LOCAL_TRANSACTION_COMMITTED, this, null);
        } catch (SQLException x) {
            throw new ResourceException(x);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Interaction createInteraction() throws ResourceException {
        BVTInteraction i = new BVTInteraction(this);
        interactions.add(i);
        return i;
    }

    /** {@inheritDoc} */
    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConnectionMetaData getMetaData() throws ResourceException {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public String getEISProductName() throws ResourceException {
        try {
            return mc.con.getMetaData().getDatabaseProductName();
        } catch (SQLException x) {
            throw new ResourceException(x);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getEISProductVersion() throws ResourceException {
        try {
            return mc.con.getMetaData().getDatabaseProductVersion();
        } catch (SQLException x) {
            throw new ResourceException(x);
        }
    }

    /** {@inheritDoc} */
    @Override
    public ResultSetInfo getResultSetInfo() throws ResourceException {
        throw new NotSupportedException();
    }

    BVTConnection init(ConnectionManager cm) {
        this.cm = cm;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public void rollback() throws ResourceException {
        try {
            mc.con.rollback();
            mc.con.setAutoCommit(true);
            mc.notify(ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK, this, null);
        } catch (SQLException x) {
            throw new ResourceException(x);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getUserName() throws ResourceException {
        try {
            return mc.con.getMetaData().getUserName();
        } catch (SQLException x) {
            throw new ResourceException(x);
        }
    }
}
