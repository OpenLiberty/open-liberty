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
package test.jdbc.heritage.driver;

import java.sql.SQLException;

/**
 * A vendor-specific SQLException subclass that really should have been java.sql.SQLFeatureNotSupportedException.
 * This allows for testing of exception identification/mapping.
 */
public class HeritageDBDoesNotImplementItException extends SQLException {
    private static final long serialVersionUID = 1L;

    public HeritageDBDoesNotImplementItException(SQLException x) {
        super(x);
    }

    public HeritageDBDoesNotImplementItException(String message, String sqlState, int errorCode) {
        super(message, sqlState, errorCode);
    }
}