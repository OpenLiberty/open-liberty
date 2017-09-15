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
package com.ibm.ws.rsadapter.jdbc.v41;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.rsadapter.jdbc.WSJdbcConnection;
import com.ibm.ws.rsadapter.jdbc.WSJdbcDatabaseMetaData;
import com.ibm.ws.rsadapter.jdbc.WSJdbcUtil;

public class WSJdbc41DatabaseMetaData extends WSJdbcDatabaseMetaData {

    public WSJdbc41DatabaseMetaData(DatabaseMetaData metaDataImpl, WSJdbcConnection connWrapper) throws SQLException {
        super(metaDataImpl, connWrapper);
    }

    @Override
    public boolean generatedKeyAlwaysReturned() throws SQLException {
        try {
            return mDataImpl.generatedKeyAlwaysReturned();
        } catch (IncompatibleClassChangeError e) {
            // If the JDBC driver was compiled with java 6
            throw new SQLFeatureNotSupportedException();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbc41DatabaseMetaData.generatedKeyAlwaysReturned", "38", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public ResultSet getPseudoColumns(String catalog, String schemaPattern,
                                      String tableNamePattern, String columnNamePattern) throws SQLException {
        ResultSet rset;
        try {
            rset = mDataImpl.getPseudoColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern);
        } catch (IncompatibleClassChangeError e) {
            // If the JDBC driver was compiled with java 6
            throw new SQLFeatureNotSupportedException();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbc41DatabaseMetaData.getPseudoColumns", "56", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
        rset = ((WSJdbcConnection) parentWrapper).createResultSetWrapper(rset, this);
        childWrappers.add(rset);
        return rset;
    }
}
