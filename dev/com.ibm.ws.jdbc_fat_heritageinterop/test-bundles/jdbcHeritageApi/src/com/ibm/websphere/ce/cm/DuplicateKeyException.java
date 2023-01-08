/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.ce.cm;

import java.sql.SQLException;

/**
 * Copy of legacy exception class for testing.
 */
public class DuplicateKeyException extends PortableSQLException {
    private static final long serialVersionUID = 1L;

    public DuplicateKeyException() {
        super();
    }

    public DuplicateKeyException(SQLException x) {
        super(x);
    }

    public DuplicateKeyException(String message) {
        super(message);
    }

    public DuplicateKeyException(String message, String sqlState, int errCode) {
        super(message, sqlState, errCode);
    }
}