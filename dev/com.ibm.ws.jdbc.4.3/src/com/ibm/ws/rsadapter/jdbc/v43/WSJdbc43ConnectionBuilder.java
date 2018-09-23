/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rsadapter.jdbc.v43;

import java.sql.Connection;
import java.sql.ConnectionBuilder;
import java.sql.SQLException;
import java.sql.ShardingKey;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.rsadapter.AdapterUtil;

public class WSJdbc43ConnectionBuilder implements ConnectionBuilder {
    private static final TraceComponent tc = Tr.register(WSJdbc43ConnectionBuilder.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);

    private final WSJdbc43DataSource ds;
    String password;
    Object shardingKey;
    Object superShardingKey;
    String user;

    public WSJdbc43ConnectionBuilder(WSJdbc43DataSource ds) {
        this.ds = ds;
    }

    @Override
    public Connection build() throws SQLException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, "build", ds, user, password == null ? null : "******", shardingKey, superShardingKey);

        return ds.getConnection(this);
    }

    @Override
    public ConnectionBuilder password(String value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "password", value == null ? null : "***");
        password = value;
        return this;
    }

    @Override
    public ConnectionBuilder shardingKey(ShardingKey value) {
        if (value != null && ds.isBackedByDriver())
            throw new UnsupportedOperationException("java.sql.Driver.createConnectionBuilder().shardingKey(value)");

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "shardingKey", value);
        shardingKey = value;
        return this;
    }

    @Override
    public ConnectionBuilder superShardingKey(ShardingKey value) {
        if (value != null && ds.isBackedByDriver())
            throw new UnsupportedOperationException("java.sql.Driver.createConnectionBuilder().superShardingKey(value)");

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "superShardingKey", value);
        superShardingKey = value;
        return this;
    }

    @Override
    public ConnectionBuilder user(String value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "user", value);
        user = value;
        return this;
    }
}