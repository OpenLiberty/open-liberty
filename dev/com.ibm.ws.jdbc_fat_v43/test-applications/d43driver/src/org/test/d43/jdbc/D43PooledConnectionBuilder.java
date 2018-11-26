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
package org.test.d43.jdbc;

import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.sql.ShardingKey;

import javax.sql.PooledConnection;
import javax.sql.PooledConnectionBuilder;

public class D43PooledConnectionBuilder implements PooledConnectionBuilder {
    private boolean built;

    private final D43ConnectionPoolDataSource d43PoolDataSource;

    private String password;
    private ShardingKey shardingKey;
    private ShardingKey superShardingKey;
    private String user;

    public D43PooledConnectionBuilder(D43ConnectionPoolDataSource ds) {
        this.d43PoolDataSource = ds;
    }

    @Override
    public PooledConnection build() throws SQLException {
        if (built)
            throw new IllegalStateException();
        else
            built = true;

        PooledConnection con;
        if (user == null && password == null)
            con = d43PoolDataSource.ds.getPooledConnection();
        else
            con = d43PoolDataSource.ds.getPooledConnection(user, password);

        con = (PooledConnection) Proxy.newProxyInstance(D43Handler.class.getClassLoader(),
                                                        new Class[] { PooledConnection.class },
                                                        new D43Handler(con, null, d43PoolDataSource, shardingKey, superShardingKey));
        return con;
    }

    @Override
    public D43PooledConnectionBuilder password(String value) {
        if (built)
            throw new IllegalStateException();
        password = value;
        return this;
    }

    @Override
    public D43PooledConnectionBuilder shardingKey(ShardingKey value) {
        if (built)
            throw new IllegalStateException();
        shardingKey = value;
        return this;
    }

    @Override
    public D43PooledConnectionBuilder superShardingKey(ShardingKey value) {
        if (built)
            throw new IllegalStateException();
        superShardingKey = value;
        return this;
    }

    @Override
    public D43PooledConnectionBuilder user(String value) {
        if (built)
            throw new IllegalStateException();
        user = value;
        return this;
    }
}