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

import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.cci.Connection;
import jakarta.resource.cci.Interaction;
import jakarta.resource.cci.InteractionSpec;
import jakarta.resource.cci.Record;
import jakarta.resource.cci.ResourceWarning;
import jakarta.resource.cci.ResultSet;
import jakarta.resource.spi.LazyEnlistableConnectionManager;

public class BVTInteraction implements Interaction {
    BVTConnection con;
    PreparedStatement stmt;

    BVTInteraction(BVTConnection con) {
        this.con = con;
    }

    @Override
    public void clearWarnings() throws ResourceException {
        try {
            con.mc.con.clearWarnings();
        } catch (SQLException x) {
            throw new ResourceException(x);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws ResourceException {
        con.interactions.remove(this);
        con = null;
        if (stmt != null)
            try {
                stmt.close();
                stmt = null;
            } catch (SQLException x) {
                throw new ResourceException(x);
            }
    }

    /** {@inheritDoc} */
    @Override
    public Record execute(InteractionSpec ispec, Record input) throws ResourceException {
        try {
            ((LazyEnlistableConnectionManager) con.cm).lazyEnlist(con.mc);
            if (stmt != null)
                stmt.close();
            // Index 0 is the sql. The others (if any) are the prepared statement parameters
            List<?> params = (List<?>) input;
            stmt = con.mc.con.prepareStatement((String) params.get(0));
            for (int i = 1; i < params.size(); i++)
                stmt.setObject(i, params.get(i));
            stmt.execute();
            java.sql.ResultSet rs = stmt.getResultSet();
            return rs == null ?
                            new BVTRecord() :
                            (Record) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { ResultSet.class }, new BVTRecord(rs));
        } catch (SQLException x) {
            throw new ResourceException(x);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean execute(InteractionSpec ispec, Record input, Record output) throws ResourceException {
        throw new NotSupportedException();
    }

    /** {@inheritDoc} */
    @Override
    public Connection getConnection() {
        return con;
    }

    /** {@inheritDoc} */
    @Override
    public ResourceWarning getWarnings() throws ResourceException {
        try {
            return new ResourceWarning(con.mc.con.getWarnings());
        } catch (SQLException x) {
            throw new ResourceException(x);
        }
    }
}
