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
    public static DatabaseVendor resolveDBProduct(String dbProductName, String dbProductVersion) {
        if (dbProductName == null || "".equals(dbProductName.trim()) || dbProductVersion == null || "".equals(dbProductVersion.trim())) {
            System.err.println("Cannot resolve database product " + dbProductName + " / " + dbProductVersion);
            return UNKNOWN;
        }

        final String lowerName = dbProductName.toLowerCase();
        final String lowerVers = dbProductVersion.toLowerCase();
        if (lowerName.contains("derby")) {
            return DERBY;
        }
        if (lowerName.contains("db2")) {
            if (lowerVers.contains("dsn")) {
                return DB2ZOS;
            }
            return DB2;
        }
        if (lowerName.contains("informix")) {
            return INFORMIX;
        }
        if (lowerName.contains("hdb")) {
            return HANA;
        }
        if (lowerName.contains("hsql")) {
            return HSQL;
        }
        if (lowerName.contains("mysql")) {
            return MYSQL;
        }
        if (lowerName.contains("oracle")) {
            return ORACLE;
        }
        if (lowerName.contains("postgres")) {
            return POSTGRES;
        }
        if (lowerName.contains("sqlserver") || lowerName.contains("microsoft sql server")) {
            return SQLSERVER;
        }
        if (lowerName.contains("sybase")) {
            return SYBASE;
        }

        return UNKNOWN;
    }

    /**
     * Checks if the given database product name matches on the given DatabaseVendors
     */
    public static boolean checkDBProductName(String dbProductName, DatabaseVendor vendor) {
        return checkDBProductName(dbProductName, "UNKNOWN", vendor);
    }

    /**
     * Checks if the given database product name & version matches on the given DatabaseVendors
     */
    public static boolean checkDBProductName(String dbProductName, String dbProductVersion, DatabaseVendor vendor) {
        if (dbProductName == null || "".equals(dbProductName.trim())) {
            return false;
        }

        final String lowerName = dbProductName.toLowerCase();
        final String lowerVers = dbProductVersion.toLowerCase();
        switch (vendor) {
            // Basing determination off product version using
            // info from https://www.ibm.com/support/knowledgecenter/en/SSEPEK_11.0.0/java/src/tpc/imjcc_c0053013.html
            case DB2:
                return lowerName.contains("db2");
            case DB2I:
                return lowerName.contains("qsq");
            case DB2LUW:
                return lowerName.contains("sql");
            case DB2VMVSE:
                return lowerName.contains("ari");
            case DB2ZOS:
                return lowerName.contains("db2") && lowerVers.contains("dsn");
            case DERBY:
                return lowerName.contains("derby");
            case INFORMIX:
                return lowerName.contains("informix");
            case HANA:
                return lowerName.contains("hdb");
            case HSQL:
                return lowerName.contains("hsql");
            case MYSQL:
                return lowerName.contains("mysql");
            case ORACLE:
                return lowerName.contains("oracle");
            case POSTGRES:
                return lowerName.contains("postgres");
            case SQLSERVER:
                return lowerName.contains("sqlserver") || lowerName.contains("microsoft sql server");
            case SYBASE:
                return lowerName.contains("sybase");
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
