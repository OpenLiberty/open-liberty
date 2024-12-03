/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.recoverylog.custom.jdbc.impl;

import java.sql.Connection;
import java.sql.SQLException;

public class DBUtils {
    public enum DBProduct {
        DB2,
        Derby,
        Oracle,
        Postgresql,
        Sqlserver,
        Unknown,
    }

    public static DBProduct identifyDB(Connection conn) throws SQLException {
        final String dbName = conn.getMetaData().getDatabaseProductName().toLowerCase();

        if (dbName.contains("db2")) {
            return DBProduct.DB2;
        } else if (dbName.contains("derby")) {
            return DBProduct.Derby;
        } else if (dbName.contains("oracle")) {
            return DBProduct.Oracle;
        } else if (dbName.contains("postgresql")) {
            return DBProduct.Postgresql;
        } else if (dbName.contains("microsoft sql")) {
            return DBProduct.Sqlserver;
        } else {
            return DBProduct.Unknown;
        }
    }
}
