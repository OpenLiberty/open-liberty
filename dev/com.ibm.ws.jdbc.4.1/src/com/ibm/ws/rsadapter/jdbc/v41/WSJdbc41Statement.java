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

import java.sql.Statement;

import com.ibm.ws.rsadapter.jdbc.WSJdbcConnection;
import com.ibm.ws.rsadapter.jdbc.WSJdbcStatement;

public class WSJdbc41Statement extends WSJdbcStatement {

    /**
     * Do not use. Constructor exists only for PreparedStatement wrapper.
     */
    public WSJdbc41Statement() {
        super();
    }

    public WSJdbc41Statement(Statement stmtImplObject, WSJdbcConnection connWrapper, int theHoldability) {
        super(stmtImplObject, connWrapper, theHoldability);
    }
}