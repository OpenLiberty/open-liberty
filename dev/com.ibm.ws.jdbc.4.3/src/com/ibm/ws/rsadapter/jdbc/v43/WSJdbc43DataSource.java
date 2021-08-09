/*******************************************************************************
 * Copyright (c) 2018,2020 IBM Corporation and others.
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
import java.sql.SQLFeatureNotSupportedException;
import java.sql.ShardingKeyBuilder;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;

import com.ibm.ws.jca.adapter.WSConnectionManager;
import com.ibm.ws.rsadapter.impl.WSConnectionRequestInfoImpl;
import com.ibm.ws.rsadapter.impl.WSManagedConnectionFactoryImpl;
import com.ibm.ws.rsadapter.jdbc.WSJdbcDataSource;

public class WSJdbc43DataSource extends WSJdbcDataSource implements DataSource {

    public WSJdbc43DataSource(WSManagedConnectionFactoryImpl mcf, WSConnectionManager connMgr) {
        super(mcf, connMgr);
    }

    @Override
    public ConnectionBuilder createConnectionBuilder() throws SQLException {
        return new WSJdbc43ConnectionBuilder(this);
    }

    @Override
    public ShardingKeyBuilder createShardingKeyBuilder() throws SQLException {
        CommonDataSource ds = mcf.getUnderlyingDataSource();
        if (ds == null) // not available because data source is backed by java.sql.Driver
            throw new SQLFeatureNotSupportedException("java.sql.Driver.createShardingKeyBuilder");
        else
            return ds.createShardingKeyBuilder();
    }

    Connection getConnection(WSJdbc43ConnectionBuilder builder) throws SQLException {
        WSConnectionRequestInfoImpl conRequest = new WSConnectionRequestInfoImpl(mcf, cm, builder.user, builder.password, builder.shardingKey, builder.superShardingKey);
        return super.getConnection(conRequest);
    }

    /**
     * @return true if the data source is backed by a java.sql.Driver implementation, otherwise false.
     */
    final boolean isBackedByDriver() {
        return mcf.getUnderlyingDataSource() == null;
    }
}