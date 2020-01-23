/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class alters tables at runtime so customers do not have 
 * to migrate their DB by updating the schema.
 */
public class DynamicDBMigrator {
    private final static String CLASS = DynamicDBMigrator.class.getName();
    private Logger log = Logger.getLogger(CLASS);
    private String tableName;

    private static final String ALTER_ADD_COL_CLIENTMETADATA = "ALTER TABLE %s ADD %s CLOB NOT NULL DEFAULT '{}'";
    private static final String ALTER_ADD_COLUMN = "ALTER TABLE %s ADD %s %s NOT NULL DEFAULT '{}'";

    public DynamicDBMigrator(String tableName) {
        this.tableName = tableName;
    }

    public DynamicDBMigrator() {
    }

    public void execute(Connection conn) {
        addTable(conn);
    }

    public void addColumnToTable(Connection conn, String table, String column, String dataType) {
        String methodName = "addColumnToTable";
        PreparedStatement st = null;
        try {
            st = conn.prepareStatement(String.format(ALTER_ADD_COLUMN, table, column, dataType));
            st.execute();
        } catch (SQLException e) {
            // Don't fail, just log it - in case other DB migration efforts should be tried after this attempt
            log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(), e);
        } finally {
            closeStatement(st);
        }
    }

    private void addTable(Connection conn) {
        addClientMetadataTable(conn);
    }

    private void addClientMetadataTable(Connection conn) {
        String methodName = "addClientMetadataTable";
        PreparedStatement st = null;
        try {
            st = conn.prepareStatement(String.format(ALTER_ADD_COL_CLIENTMETADATA, tableName, TableConstants.COL_CC2_CLIENTMETADATA));
            st.execute();
        } catch (SQLException e) {
            // Don't fail, just log it - in case other DB migration efforts should be tried after this attempt
            log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(), e);
        } finally {
            closeStatement(st);
        }
    }

    private void closeStatement(Statement statement) {
        String methodName = "closeStatement";
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(), e);
            }
        }
    }
}
