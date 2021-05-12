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
    DB2I("DB2I"),
    DB2LUW("DB2LUW"),
    DB2VMVSE("DB2VMVSE"),
    DB2ZOS("DB2ZOS"),
    DERBY("DERBY"),
    INFORMIX("INFORMIX"),
    HANA("HANA"),
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
        if (toLower.contains("hdb")) {
            return HANA;
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

    /**
     * Checks if the given database product name matches on the given DatabaseVendors
     */
    public static boolean checkDBProductName(String dbProductName, DatabaseVendor vendor) {
        if (dbProductName == null || "".equals(dbProductName.trim())) {
            return false;
        }

        final String toLower = dbProductName.toLowerCase();
        switch (vendor) {
            // Basing determination off product version using
            // info from https://www.ibm.com/support/knowledgecenter/en/SSEPEK_11.0.0/java/src/tpc/imjcc_c0053013.html
            case DB2:
                return toLower.contains("db2");
            case DB2I:
                return toLower.contains("qsq");
            case DB2LUW:
                return toLower.contains("sql");
            case DB2VMVSE:
                return toLower.contains("ari");
            case DB2ZOS:
                return toLower.contains("dsn");
            case DERBY:
                return toLower.contains("derby");
            case INFORMIX:
                return toLower.contains("informix");
            case HANA:
                return toLower.contains("hdb");
            case HSQL:
                return toLower.contains("hsql");
            case MYSQL:
                return toLower.contains("mysql");
            case ORACLE:
                return toLower.contains("oracle");
            case POSTGRES:
                return toLower.contains("postgres");
            case SQLSERVER:
                return toLower.contains("sqlserver") || toLower.contains("microsoft sql server");
            case SYBASE:
                return toLower.contains("sybase");
            case UNKNOWN:
                return false;
        }

        return false;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
