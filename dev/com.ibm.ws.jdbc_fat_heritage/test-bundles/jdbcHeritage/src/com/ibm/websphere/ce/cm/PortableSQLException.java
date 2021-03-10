/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.ce.cm;

import java.sql.SQLException;

/**
 * Copy of legacy exception class for testing.
 */
public abstract class PortableSQLException extends SQLException {
    private static final long serialVersionUID = 1L;

    protected PortableSQLException() {
    }

    protected PortableSQLException(SQLException x) {
        super(x.getMessage(), x.getSQLState(), x.getErrorCode());
        setNextException(x);
    }

    protected PortableSQLException(String message) {
        super(message);
    }

    public PortableSQLException(String message, String sqlState, int errCode) {
        super(message, sqlState, errCode);
    }
}