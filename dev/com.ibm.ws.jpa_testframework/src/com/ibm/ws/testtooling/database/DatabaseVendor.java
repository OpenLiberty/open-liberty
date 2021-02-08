/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.testtooling.database;

/**
 * Simple Utility class for mapping JDBC MetaData product names for JPA FAT DDL
 */
public enum DatabaseVendor {
    DB2("DB2"),
    DERBY("DERBY"),
    INFORMIX("INFORMIX"),
    HSQL("HSQL"),
    MYSQL("MYSQL"),
    ORACLE("ORACLE"),
    POSTGRES("POSTGRES"),
    SQLSERVER("SQLSERVER"),
    SYBASE("SYBASE"),
    UNKNOWN("UNKNOWN");

    private String name;

    private DatabaseVendor(String name) {
        this.name = name;
    }

    /**
     * Given a database name, returns the matching DatabaseVendor enumeration value.
     * Uses the JDBC string name for matching.
     */
    public static DatabaseVendor resolveDBProduct(String dbProductName) {
        if (dbProductName == null || "".equals(dbProductName.trim())) {
            System.err.println("Cannot resolve database product " + dbProductName);
            return UNKNOWN;
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
        if (toLower.contains("sqlserver") || toLower.contains("microsoft sql server")) {
            return SQLSERVER;
        }
        if (toLower.contains("sybase")) {
            return SYBASE;
        }

        return UNKNOWN;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
