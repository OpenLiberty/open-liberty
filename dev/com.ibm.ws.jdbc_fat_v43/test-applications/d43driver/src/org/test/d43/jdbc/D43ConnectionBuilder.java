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
import java.sql.Connection;
import java.sql.ConnectionBuilder;
import java.sql.SQLException;
import java.sql.ShardingKey;

public class D43ConnectionBuilder implements ConnectionBuilder {
    private final D43DataSource d43DataSource;

    private String password;
    private ShardingKey shardingKey;
    private ShardingKey superShardingKey;
    private String user;

    public D43ConnectionBuilder(D43DataSource ds) {
        this.d43DataSource = ds;
    }

    @Override
    public Connection build() throws SQLException {
        Connection con;
        if (user == null && password == null)
            con = d43DataSource.ds.getConnection();
        else
            con = d43DataSource.ds.getConnection(user, password);

        con = (Connection) Proxy.newProxyInstance(D43Handler.class.getClassLoader(),
                                                  new Class[] { Connection.class },
                                                  new D43Handler(con, null, d43DataSource, shardingKey, superShardingKey));
        return con;
    }

    @Override
    public D43ConnectionBuilder password(String value) {
        password = value;
        return this;
    }

    @Override
    public D43ConnectionBuilder shardingKey(ShardingKey value) {
        shardingKey = value;
        return this;
    }

    @Override
    public D43ConnectionBuilder superShardingKey(ShardingKey value) {
        superShardingKey = value;
        return this;
    }

    @Override
    public D43ConnectionBuilder user(String value) {
        user = value;
        return this;
    }
}