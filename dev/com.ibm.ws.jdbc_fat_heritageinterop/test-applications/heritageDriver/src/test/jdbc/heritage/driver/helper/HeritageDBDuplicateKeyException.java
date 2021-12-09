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
package test.jdbc.heritage.driver.helper;

import java.sql.SQLException;

import com.ibm.websphere.ce.cm.DuplicateKeyException;

/**
 * A custom exception class for testing.
 */
public class HeritageDBDuplicateKeyException extends DuplicateKeyException {
    private static final long serialVersionUID = 1L;

    public HeritageDBDuplicateKeyException() {
        super();
    }

    public HeritageDBDuplicateKeyException(SQLException x) {
        super(x);
    }

    public HeritageDBDuplicateKeyException(String message) {
        super(message);
    }

    public HeritageDBDuplicateKeyException(String message, String sqlState, int errCode) {
        super(message, sqlState, errCode);
    }
}