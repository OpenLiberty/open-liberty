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
import java.sql.SQLException;
import java.sql.ShardingKey;

import com.ibm.ws.rsadapter.AdapterUtil;
import com.ibm.ws.rsadapter.impl.WSRdbManagedConnectionImpl;
import com.ibm.ws.rsadapter.jdbc.v41.WSJdbc41Connection;

public class WSJdbc43Connection extends WSJdbc41Connection implements Connection {

    public WSJdbc43Connection(WSRdbManagedConnectionImpl mc, Connection conn, Object key, Object currentThreadID) {
        super(mc, conn, key, currentThreadID);
    }

    @Override
    public void beginRequest() throws SQLException {
        AdapterUtil.suppressBeginAndEndRequest();
    }

    @Override
    public void endRequest() throws SQLException {
        AdapterUtil.suppressBeginAndEndRequest();
    }

    @Override
    public boolean setShardingKeyIfValid(ShardingKey shardingKey, int timeout) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean setShardingKeyIfValid(ShardingKey shardingKey, ShardingKey superShardingKey, int timeout) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setShardingKey(ShardingKey shardingKey) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setShardingKey(ShardingKey shardingKey, ShardingKey superShardingKey) throws SQLException {
        throw new UnsupportedOperationException();
    }

}