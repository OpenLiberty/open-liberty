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

package com.ibm.ws.jpa;

/**
 *
 */
public enum DatabaseVendor {
    DB2,
    DERBY,
    INFORMIX,
    HSQL,
    MYSQL,
    ORACLE,
    POSTGRES,
    SQLSERVER,
    SYBASE;

    public static DatabaseVendor resolveDBProduct(String dbProductName) {
        if (dbProductName == null || "".equals(dbProductName.trim())) {
            System.err.println("Cannot resolve database product " + dbProductName);
            return null;
        }

        final String toLower = dbProductName.toLowerCase();
        if (toLower.contains("derby")) {
            return DERBY;
        }
        if (toLower.contains("db2")) {
            return DB2;
        }
        if (toLower.contains("informix")) {
            return INFORMIX;
        }
        if (toLower.contains("hsql")) {
            return HSQL;
        }
        if (toLower.contains("mysql")) {
            return MYSQL;
        }
        if (toLower.contains("oracle")) {
            return ORACLE;
        }
        if (toLower.contains("postgres")) {
            return POSTGRES;
        }
        if (toLower.contains("sqlserver")) {
            return SQLSERVER;
        }
        if (toLower.contains("sybase")) {
            return SYBASE;
        }

        return null;
    }
}
