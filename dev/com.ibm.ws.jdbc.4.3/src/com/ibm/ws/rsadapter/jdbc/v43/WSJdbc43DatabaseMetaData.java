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

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.rsadapter.jdbc.WSJdbcConnection;
import com.ibm.ws.rsadapter.jdbc.WSJdbcUtil;
import com.ibm.ws.rsadapter.jdbc.v42.WSJdbc42DatabaseMetaData;

public class WSJdbc43DatabaseMetaData extends WSJdbc42DatabaseMetaData implements DatabaseMetaData {

    public WSJdbc43DatabaseMetaData(DatabaseMetaData metaDataImpl, WSJdbcConnection connWrapper) throws SQLException {
        super(metaDataImpl, connWrapper);
    }

    @Override
    public boolean supportsSharding() throws SQLException {
        try {
            return mDataImpl.supportsSharding();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, getClass().getName(), "32", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

}
