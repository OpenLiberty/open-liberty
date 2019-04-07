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

import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jdbc.osgi.JDBCRuntimeVersion;
import com.ibm.ws.rsadapter.AdapterUtil;
import com.ibm.ws.rsadapter.ConnectionSharing;
import com.ibm.ws.rsadapter.impl.WSConnectionRequestInfoImpl;
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
        activate();

        try {
            // Setters are not permitted when multiple handles are sharing the same ManagedConnection,
            // except when the specified value is the same as the current value, which is a no-op.
            if (managedConn.getHandleCount() > 1 && !AdapterUtil.match(shardingKey, managedConn.getCurrentShardingKey()))
                throw createSharingException("setShardingKeyIfValid");

            boolean updated = managedConn.setShardingKeysIfValid(shardingKey, JDBCRuntimeVersion.SUPER_SHARDING_KEY_UNCHANGED, timeout);

            // Update the connection request information with the new value, so that
            // requests for shared connections will match based on the updated criteria.
            if (updated && managedConn.connectionSharing == ConnectionSharing.MatchCurrentState) {
                WSConnectionRequestInfoImpl cri = (WSConnectionRequestInfoImpl) managedConn.getConnectionRequestInfo();
                if (!cri.isCRIChangable())
                    managedConn.setCRI(cri = WSConnectionRequestInfoImpl.createChangableCRIFromNon(cri));

                cri.setShardingKey(shardingKey);
            }

            return updated;
        } catch (SQLException x) {
            FFDCFilter.processException(x, getClass().getName(), "65", this);
            throw proccessSQLException(x);
        } catch (NullPointerException x) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(x);
        }
    }

    @Override
    public boolean setShardingKeyIfValid(ShardingKey shardingKey, ShardingKey superShardingKey, int timeout) throws SQLException {
        activate();

        try {
            // Setters are not permitted when multiple handles are sharing the same ManagedConnection,
            // except when the specified value is the same as the current value, which is a no-op.
            if (managedConn.getHandleCount() > 1 &&
                (!AdapterUtil.match(shardingKey, managedConn.getCurrentShardingKey()) ||
                 !AdapterUtil.match(superShardingKey, managedConn.getCurrentSuperShardingKey())))
                throw createSharingException("setShardingKey");

            boolean updated = managedConn.setShardingKeysIfValid(shardingKey, superShardingKey, timeout);

            // Update the connection request information with the new value, so that
            // requests for shared connections will match based on the updated criteria.
            if (updated && managedConn.connectionSharing == ConnectionSharing.MatchCurrentState) {
                WSConnectionRequestInfoImpl cri = (WSConnectionRequestInfoImpl) managedConn.getConnectionRequestInfo();
                if (!cri.isCRIChangable())
                    managedConn.setCRI(cri = WSConnectionRequestInfoImpl.createChangableCRIFromNon(cri));

                cri.setShardingKey(shardingKey);
                cri.setSuperShardingKey(superShardingKey);
            }

            return updated;
        } catch (SQLException x) {
            FFDCFilter.processException(x, getClass().getName(), "100", this);
            throw proccessSQLException(x);
        } catch (NullPointerException x) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(x);
        }
    }

    @Override
    public void setShardingKey(ShardingKey shardingKey) throws SQLException {
        activate();

        try {
            // Setters are not permitted when multiple handles are sharing the same ManagedConnection,
            // except when the specified value is the same as the current value, which is a no-op.
            if (managedConn.getHandleCount() > 1 && !AdapterUtil.match(shardingKey, managedConn.getCurrentShardingKey()))
                throw createSharingException("setShardingKey");

            managedConn.setShardingKeys(shardingKey, JDBCRuntimeVersion.SUPER_SHARDING_KEY_UNCHANGED);

            // Update the connection request information with the new value, so that
            // requests for shared connections will match based on the updated criteria.
            if (managedConn.connectionSharing == ConnectionSharing.MatchCurrentState) {
                WSConnectionRequestInfoImpl cri = (WSConnectionRequestInfoImpl) managedConn.getConnectionRequestInfo();
                if (!cri.isCRIChangable())
                    managedConn.setCRI(cri = WSConnectionRequestInfoImpl.createChangableCRIFromNon(cri));

                cri.setShardingKey(shardingKey);
            }
        } catch (SQLException x) {
            FFDCFilter.processException(x, getClass().getName(), "71", this);
            throw proccessSQLException(x);
        } catch (NullPointerException x) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(x);
        }
    }

    @Override
    public void setShardingKey(ShardingKey shardingKey, ShardingKey superShardingKey) throws SQLException {
        activate();

        try {
            // Setters are not permitted when multiple handles are sharing the same ManagedConnection,
            // except when the specified value is the same as the current value, which is a no-op.
            if (managedConn.getHandleCount() > 1 &&
                (!AdapterUtil.match(shardingKey, managedConn.getCurrentShardingKey()) ||
                 !AdapterUtil.match(superShardingKey, managedConn.getCurrentSuperShardingKey())))
                throw createSharingException("setShardingKey");

            managedConn.setShardingKeys(shardingKey, superShardingKey);

            // Update the connection request information with the new value, so that
            // requests for shared connections will match based on the updated criteria.
            if (managedConn.connectionSharing == ConnectionSharing.MatchCurrentState) {
                WSConnectionRequestInfoImpl cri = (WSConnectionRequestInfoImpl) managedConn.getConnectionRequestInfo();
                if (!cri.isCRIChangable())
                    managedConn.setCRI(cri = WSConnectionRequestInfoImpl.createChangableCRIFromNon(cri));

                cri.setShardingKey(shardingKey);
                cri.setSuperShardingKey(superShardingKey);
            }
        } catch (SQLException x) {
            FFDCFilter.processException(x, getClass().getName(), "104", this);
            throw proccessSQLException(x);
        } catch (NullPointerException x) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(x);
        }
    }

}