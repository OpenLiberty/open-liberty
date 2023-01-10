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
package test.jdbc.heritage.driver;

import java.sql.SQLException;
import java.sql.SQLNonTransientException;

import javax.sql.XAConnection;
import javax.sql.XADataSource;

public class HDXADataSource extends HDDataSource implements XADataSource {
    private static final long serialVersionUID = 1L;

    @Override
    public XAConnection getXAConnection() throws SQLException {
        return new HDConnection(this, super.getConnection());
    }

    @Override
    public XAConnection getXAConnection(String username, String password) throws SQLException {
        if ("ConnectionRefused".equalsIgnoreCase(username))
            throw new SQLNonTransientException("Connection Refused for Testing Purposes", "08001", 40000);

        return new HDConnection(this, super.getConnection(username, password));
    }
}