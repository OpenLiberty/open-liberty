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

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.ibm.ws.rsadapter.impl.StatementCacheKey;
import com.ibm.ws.rsadapter.jdbc.WSJdbcConnection;
import com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement;

public class WSJdbc41PreparedStatement extends WSJdbcPreparedStatement implements PreparedStatement {

    /**
     * Do not use. Constructor exists only for CallableStatement wrapper.
     */
    public WSJdbc41PreparedStatement() {
        super();
    }

    public WSJdbc41PreparedStatement(PreparedStatement pstmtImplObject, WSJdbcConnection connWrapper,
                                     int theHoldability, String pstmtSQL) throws SQLException {
        super(pstmtImplObject, connWrapper, theHoldability, pstmtSQL);
    }

    public WSJdbc41PreparedStatement(PreparedStatement pstmtImplObject, WSJdbcConnection connWrapper,
                                     int theHoldability, String pstmtSQL,
                                     StatementCacheKey pstmtKey) throws SQLException {
        super(pstmtImplObject, connWrapper, theHoldability, pstmtSQL, pstmtKey);
    }
}