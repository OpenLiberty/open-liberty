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

import javax.sql.XAConnection;
import javax.sql.XAConnectionBuilder;

public class D43XAConnectionBuilder implements XAConnectionBuilder {
    private boolean built;

    private final D43XADataSource d43XADataSource;

    private String password;
    private ShardingKey shardingKey;
    private ShardingKey superShardingKey;
    private String user;

    public D43XAConnectionBuilder(D43XADataSource ds) {
        this.d43XADataSource = ds;
    }

    @Override
    public XAConnection build() throws SQLException {
        if (built)
            throw new IllegalStateException();
        else
            built = true;

        XAConnection con;
        if (user == null && password == null)
            con = d43XADataSource.ds.getXAConnection();
        else
            con = d43XADataSource.ds.getXAConnection(user, password);

        con = (XAConnection) Proxy.newProxyInstance(D43Handler.class.getClassLoader(),
                                                    new Class[] { XAConnection.class },
                                                    new D43Handler(con, null, d43XADataSource, shardingKey, superShardingKey));
        return con;
    }

    @Override
    public D43XAConnectionBuilder password(String value) {
        if (built)
            throw new IllegalStateException();
        password = value;
        return this;
    }

    @Override
    public D43XAConnectionBuilder shardingKey(ShardingKey value) {
        if (built)
            throw new IllegalStateException();
        shardingKey = value;
        return this;
    }

    @Override
    public D43XAConnectionBuilder superShardingKey(ShardingKey value) {
        if (built)
            throw new IllegalStateException();
        superShardingKey = value;
        return this;
    }

    @Override
    public D43XAConnectionBuilder user(String value) {
        if (built)
            throw new IllegalStateException();
        user = value;
        return this;
    }
}