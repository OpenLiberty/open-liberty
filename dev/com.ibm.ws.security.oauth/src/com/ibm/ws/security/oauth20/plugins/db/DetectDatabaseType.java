/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.plugins.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation below borrowed from Connections source
 */

/**
 * Utility interface to determine DB variant. You can access this object in
 * Spring via the repository name.
 */
public interface DetectDatabaseType {

    /**
     * Description of DBTypes
     */
    public enum DBType {
        DB2(true, true),
        CLOUDSCAPE(false, true),
        ORACLE(false, true),
        MSSQL(true, true),
        DERBY(false, true),
        POSTGRESQL(false, false),
        UNKNOWN(false, true);

        /**
         * 
         * @param sqlLimitSupported
         */
        private DBType(boolean sqlLimitSupported, boolean clobSupported) {
            this.sqlLimitSupported = sqlLimitSupported;
            this.clobSupported = clobSupported;
        }

        /**
         * @return the sqlLimitSupported
         */
        public final boolean isSqlLimitSupported() {
            return sqlLimitSupported;
        }

        /**
             * @return the clobSupported
             */
        public final boolean isClobSupported() {
            return clobSupported;
        }

        private final boolean sqlLimitSupported;
        private final boolean clobSupported;
    }

    /**
     * Returns DBType
     * @return
     */
    public DBType getDbType();

    /**
     * 	Supported dbVendor:  "oracle", "derby", "db2" , "sqlserver"
     * @return
     */
    public DBType getDbType(String dbVendor);

    /**
     * Utility class for DB info detection
     */
    public class DetectionUtils {
        private static final String CLSNAME = DetectDatabaseType.class.getName();
        private static final Logger logger = Logger.getLogger(CLSNAME);

        /**
         * Utility method to detect DbInfo
         * @param conn
         * @return
         */
        public static DBType detectDbType(Connection conn) {

            final boolean FINER = logger.isLoggable(Level.FINER);

            if (conn == null) {
                if (FINER) {
                    logger.finer("Connection is null, dbtype UNKNOWN");
                }
                return DBType.UNKNOWN;
            }

            try {
                DatabaseMetaData dbm = conn.getMetaData();
                if (dbm == null) {
                    if (FINER) {
                        logger.finer("DB MetaInfo is null, dbtype UNKNOWN");
                    }
                    return DBType.UNKNOWN;
                }

                String driverName = dbm.getDriverName();

                if (FINER) {
                    logger.finer("DB driver name is: " + driverName);
                }

                if (driverName == null) {
                    if (FINER) {
                        logger.finer("DB MetaInfo is null, dbtype UNKNOWN");
                    }
                    return DBType.DB2;
                }
                else {
                    driverName = driverName.toUpperCase();
                    if (driverName.startsWith("CLOUDSCAPE")) {
                        if (FINER) {
                            logger.finer("DB driver name reports CLOUDSCAPE");
                        }
                        return DBType.CLOUDSCAPE;
                    }
                    else if (driverName.startsWith("IBM")) {
                        String productName = dbm.getDatabaseProductName();
                        if (productName == null) {

                            return DBType.DB2;
                        }
                        else {
                            productName = productName.toUpperCase();
                            if (productName.startsWith("CLOUDSCAPE")) {
                                return DBType.CLOUDSCAPE;
                            }
                            else {
                                return DBType.DB2;
                            }
                        }
                    }
                    else if (driverName.startsWith("ORACLE")) {
                        return DBType.ORACLE;
                    }
                    else if (driverName.contains("DERBY")) {
                        return DBType.DERBY;
                    }
                    else if (driverName.contains("POSTGRESQL")) {
                        return DBType.POSTGRESQL;
                    }
                    else {
                        Statement stmt = null;
                        try {
                            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                            stmt.execute("select @@version as ver");
                            ResultSet rs = stmt.getResultSet();
                            if (rs.next()) {
                                String s = rs.getString("ver");
                                if (s != null && s.toLowerCase().startsWith("microsoft")) {
                                    return DBType.MSSQL;
                                }
                            }
                        } finally {
                            if (stmt != null)
                                stmt.close();
                        }
                    }
                }
            } catch (SQLException sqlex) {
                if (FINER) {
                    logger.finer("Exception thrown during DB detection, return type UNKNOWN");
                }

                // do nothing - copied from Activities
                return DBType.UNKNOWN;
            }

            return DBType.UNKNOWN;
        }
    }

}
