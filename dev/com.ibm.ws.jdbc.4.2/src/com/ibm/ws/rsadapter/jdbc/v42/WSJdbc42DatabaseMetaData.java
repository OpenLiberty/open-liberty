/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rsadapter.jdbc.v42;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.rsadapter.jdbc.WSJdbcConnection;
import com.ibm.ws.rsadapter.jdbc.WSJdbcUtil;
import com.ibm.ws.rsadapter.jdbc.v41.WSJdbc41DatabaseMetaData;

public class WSJdbc42DatabaseMetaData extends WSJdbc41DatabaseMetaData implements DatabaseMetaData {

    public WSJdbc42DatabaseMetaData(DatabaseMetaData metaDataImpl, WSJdbcConnection connWrapper) throws SQLException {
        super(metaDataImpl, connWrapper);
    }

    @Override
    public boolean supportsRefCursors() throws SQLException {
        try {
            return mDataImpl.supportsRefCursors();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.v42.WSJdbc42DatabaseMetaData.supportsRefCursors", "2020", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public long getMaxLogicalLobSize() throws SQLException {
        try {
            return mDataImpl.getMaxLogicalLobSize();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbc42DatabaseMetaData.getMaxLogicalLobSize", "2712", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }
}
