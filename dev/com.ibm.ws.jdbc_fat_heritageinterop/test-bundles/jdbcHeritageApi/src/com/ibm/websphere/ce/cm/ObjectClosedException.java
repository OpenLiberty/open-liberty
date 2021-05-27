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
public class ObjectClosedException extends StaleConnectionException {
    private static final long serialVersionUID = 1L;

    public ObjectClosedException() {
        super();
    }

    public ObjectClosedException(SQLException x) {
        super(x);
    }

    public ObjectClosedException(String message) {
        super(message);
    }
}