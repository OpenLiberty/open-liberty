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

import java.sql.CallableStatement;
import java.sql.SQLException;

import com.ibm.ws.rsadapter.impl.StatementCacheKey;
import com.ibm.ws.rsadapter.jdbc.WSJdbcConnection;
import com.ibm.ws.rsadapter.jdbc.WSJdbcUtil;
import com.ibm.ws.rsadapter.jdbc.v42.WSJdbc42CallableStatement;

public class WSJdbc43CallableStatement extends WSJdbc42CallableStatement implements CallableStatement {

    public WSJdbc43CallableStatement(CallableStatement cstmtImplObject, WSJdbcConnection connWrapper,
                                     int theHoldability, String cstmtSQL) throws SQLException {
        super(cstmtImplObject, connWrapper, theHoldability, cstmtSQL);
    }

    public WSJdbc43CallableStatement(CallableStatement cstmtImplObject, WSJdbcConnection connWrapper,
                                     int theHoldability, String cstmtSQL,
                                     StatementCacheKey cstmtKey) throws SQLException {
        super(cstmtImplObject, connWrapper, theHoldability, cstmtSQL, cstmtKey);
    }

    @Override
    public String enquoteLiteral(String val) throws SQLException {
        // KEEP CODE IN SYNC: This method is duplicated in WSJdbc43Statement, WSJdbc43PreparedStatement,
        // and WSJdbc43CallableStatement because multiple inheritance isn't allowed.
        try {
            return cstmtImpl.enquoteLiteral(val);
        } catch (SQLException ex) {
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public String enquoteIdentifier(String identifier, boolean alwaysQuote) throws SQLException {
        // KEEP CODE IN SYNC: This method is duplicated in WSJdbc43Statement, WSJdbc43PreparedStatement,
        // and WSJdbc43CallableStatement because multiple inheritance isn't allowed.
        try {
            return cstmtImpl.enquoteIdentifier(identifier, alwaysQuote);
        } catch (SQLException ex) {
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public boolean isSimpleIdentifier(String identifier) throws SQLException {
        // KEEP CODE IN SYNC: This method is duplicated in WSJdbc43Statement, WSJdbc43PreparedStatement,
        // and WSJdbc43CallableStatement because multiple inheritance isn't allowed.
        try {
            return cstmtImpl.isSimpleIdentifier(identifier);
        } catch (SQLException ex) {
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public String enquoteNCharLiteral(String val) throws SQLException {
        // KEEP CODE IN SYNC: This method is duplicated in WSJdbc43Statement, WSJdbc43PreparedStatement,
        // and WSJdbc43CallableStatement because multiple inheritance isn't allowed.
        try {
            return cstmtImpl.enquoteNCharLiteral(val);
        } catch (SQLException ex) {
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }
}