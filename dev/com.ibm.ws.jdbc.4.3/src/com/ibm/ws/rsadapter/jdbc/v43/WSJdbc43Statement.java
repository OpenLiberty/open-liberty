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

import java.sql.SQLException;
import java.sql.Statement;

import com.ibm.ws.rsadapter.jdbc.WSJdbcConnection;
import com.ibm.ws.rsadapter.jdbc.WSJdbcUtil;
import com.ibm.ws.rsadapter.jdbc.v42.WSJdbc42Statement;

public class WSJdbc43Statement extends WSJdbc42Statement implements Statement {

    /**
     * Do not use. Constructor exists only for PreparedStatement wrapper.
     */
    public WSJdbc43Statement() {
        super();
    }

    public WSJdbc43Statement(Statement stmtImplObject, WSJdbcConnection connWrapper, int theHoldability) {
        super(stmtImplObject, connWrapper, theHoldability);
    }

    @Override
    public String enquoteLiteral(String val) throws SQLException {
        // KEEP CODE IN SYNC: This method is duplicated in WSJdbc43Statement, WSJdbc43PreparedStatement,
        // and WSJdbc43CallableStatement because multiple inheritance isn't allowed.
        try {
            return stmtImpl.enquoteLiteral(val);
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
            return stmtImpl.enquoteIdentifier(identifier, alwaysQuote);
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
            return stmtImpl.isSimpleIdentifier(identifier);
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
            return stmtImpl.enquoteNCharLiteral(val);
        } catch (SQLException ex) {
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            throw runtimeXIfNotClosed(nullX);
        }
    }
}