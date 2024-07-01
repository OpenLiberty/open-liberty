/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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
package com.ibm.ws.transaction.fat.util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.JdbcDatabaseContainer.NoDriverFoundException;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.containers.TestContainerSuite;
import componenttest.topology.database.container.DatabaseContainerFactory;
import componenttest.topology.database.container.DatabaseContainerType;

/**
 *
 */
public class TxTestContainerSuite extends TestContainerSuite {

    private static DatabaseContainerType databaseContainerType;
    public static JdbcDatabaseContainer<?> testContainer;

    public static void beforeSuite(DatabaseContainerType type) {
        Log.info(TxTestContainerSuite.class, "beforeSuite", type.toString());

        setType(type);

        if (testContainer == null) {
          testContainer = DatabaseContainerFactory.createType(databaseContainerType);
        }
        testContainer.setStartupAttempts(2);
        testContainer.start();

        Log.info(TxTestContainerSuite.class, "beforeSuite", "started test container of type: " + databaseContainerType);
    }

    public static void afterSuite(String ...tables) throws SQLException {
    	dropTables(tables);
    }
    
    public static void showTables() throws NoDriverFoundException, SQLException {
    	Log.info(TxTestContainerSuite.class, "showTables", "");
    	if (!isDerby()) {
    		try (Connection conn = testContainer.createConnection("")) {
    			final DatabaseMetaData metaData = conn.getMetaData();
    			final String[] types = {"TABLE"};
    			// Retrieving the columns in the database
    			try (ResultSet tables = metaData.getTables(null, null, "%", types)) {
    				while (tables.next()) {
    					Log.info(TxTestContainerSuite.class, "showTables", tables.getString("TABLE_NAME"));
    				}
    			}
    		}
        }
    }

    public static void dropTables(String ...tables) throws SQLException {
    	Log.entering(TxTestContainerSuite.class, "dropTables");
    	if (!isDerby()) {
    		try (Connection con = testContainer.createConnection(""); Statement stmt = con.createStatement()) {
    			if (tables.length != 0) {
    				Log.info(TxTestContainerSuite.class, "dropTables", "explicit");
    				for (String table : tables) {
    					dropTable(stmt, table);
    				}
    			} else {
    				final DatabaseMetaData metaData = con.getMetaData();
    				final String[] types = {"TABLE"};
    				// Retrieving the columns in the database
    				try (ResultSet existing = metaData.getTables(null, null, "%", types)) {
    					while (existing.next()) {
    						dropTable(stmt, existing.getString("TABLE_NAME"));
    					}
    				}
    			}
    		}
    	}
    }
    
    private static void dropTable(Statement stmt, String table) throws SQLException {
    	final boolean result;
    	final String ddl;

    	switch (databaseContainerType) {
    	case Oracle:
    		ddl = "DROP TABLE " + table;
    		break;
    	default:
    		ddl = "DROP TABLE IF EXISTS " + table;
    	}

    	result = stmt.execute(ddl);
		Log.info(TxTestContainerSuite.class, "dropTables", ddl + " returned " + result);
    }

	public static boolean isDerby() {
		return databaseContainerType == DatabaseContainerType.Derby;
	}

	public static void setType(DatabaseContainerType type) {
		databaseContainerType = type;
	}
}