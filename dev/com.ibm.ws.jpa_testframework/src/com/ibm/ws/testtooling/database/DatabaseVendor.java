/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

package com.ibm.ws.testtooling.database;

/**
 * Simple Utility class for mapping JDBC MetaData product names for JPA FAT DDL
 */
public enum DatabaseVendor {
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
            if (lowerVers.contains("sql")) {
                return DB2LUW;
            } else if (lowerVers.contains("dsn")) {
                return DB2ZOS;
            } else if (lowerVers.contains("qsq")) {
                return DB2I;
            } else if (lowerVers.contains("ari")) {
                return DB2VMVSE;
            }
            // Fallback
            return DB2LUW;
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
     *
     * https://www.ibm.com/docs/en/db2-for-zos/12?topic=work-product-identifier-prdid-values-in-db2-zos
     *
     * The format of 8-byte PRDID values is pppvvrrm, with the following parts:
     * ppp
     * A three-letter product code. For example:
     * AQT - IBM Db2 Analytics Accelerator for z/OS
     * ARI - DB2® Server for VSE & VM
     * DSN - Db2 for z/OS
     * HTP - Non-secure HTTP URL connections for Db2 native REST services
     * HTS - secure HTTPS connections for Db2 native REST services
     * JCC - IBM® Data Server Driver for JDBC and SQLJ
     * LRT - Connections requesting log records from asynchronous log reader tasks
     * QSQ - DB2 for i
     * SQL - Db2 for Linux®, UNIX, and Windows
     *
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
            case DB2LUW:
                return lowerName.contains("db2") && lowerVers.contains("sql");
            case DB2ZOS:
                return lowerName.contains("db2") && lowerVers.contains("dsn");
            case DB2I:
                return lowerName.contains("db2") && lowerVers.contains("qsq");
            case DB2VMVSE:
                return lowerName.contains("db2") && lowerVers.contains("ari");
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
