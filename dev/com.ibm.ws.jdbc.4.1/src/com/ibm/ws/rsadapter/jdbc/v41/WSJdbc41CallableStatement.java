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

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.rsadapter.impl.StatementCacheKey;
import com.ibm.ws.rsadapter.jdbc.WSJdbcCallableStatement;
import com.ibm.ws.rsadapter.jdbc.WSJdbcConnection;
import com.ibm.ws.rsadapter.jdbc.WSJdbcUtil;

public class WSJdbc41CallableStatement extends WSJdbcCallableStatement implements CallableStatement {

    public WSJdbc41CallableStatement(CallableStatement cstmtImplObject, WSJdbcConnection connWrapper,
                                     int theHoldability, String cstmtSQL) throws SQLException {
        super(cstmtImplObject, connWrapper, theHoldability, cstmtSQL);
    }

    public WSJdbc41CallableStatement(CallableStatement cstmtImplObject, WSJdbcConnection connWrapper,
                                     int theHoldability, String cstmtSQL,
                                     StatementCacheKey cstmtKey) throws SQLException {
        super(cstmtImplObject, connWrapper, theHoldability, cstmtSQL, cstmtKey);
    }

    @Override
    public <T> T getObject(int parameterIndex, Class<T> type) throws SQLException {
        try {
            return cstmtImpl.getObject(parameterIndex, type);
        } catch (IncompatibleClassChangeError e) {
            // If the JDBC driver was compiled with java 6
            throw new SQLFeatureNotSupportedException();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbc41CallableStatement.getObject", "59", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public <T> T getObject(String parameterName, Class<T> type) throws SQLException {
        try {
            return cstmtImpl.getObject(parameterName, type);
        } catch (IncompatibleClassChangeError e) {
            // If the JDBC driver was compiled with java 6
            throw new SQLFeatureNotSupportedException();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbc41CallableStatement.getObject", "82", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }
}