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

import java.sql.Statement;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.rsadapter.AdapterUtil;
import com.ibm.ws.rsadapter.jdbc.WSJdbcConnection;
import com.ibm.ws.rsadapter.jdbc.v42.WSJdbc42Statement;

public class WSJdbc43Statement extends WSJdbc42Statement implements Statement {

    private static final TraceComponent tc = Tr.register(WSJdbc43Statement.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);

    /**
     * Do not use. Constructor exists only for PreparedStatement wrapper.
     */
    public WSJdbc43Statement() {
        super();
    }

    public WSJdbc43Statement(Statement stmtImplObject, WSJdbcConnection connWrapper, int theHoldability) {
        super(stmtImplObject, connWrapper, theHoldability);
    }
}