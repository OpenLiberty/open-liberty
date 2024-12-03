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

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.containers.TestContainerSuite;
import componenttest.topology.database.container.DatabaseContainerFactory;
import componenttest.topology.database.container.DatabaseContainerType;

/**
 *
 */
public class TxTestContainerSuite extends TestContainerSuite {
    public static final String POSTGRES_DB = "testdb";
    public static final String POSTGRES_USER = "postgresUser";
    public static final String POSTGRES_PASS = "superSecret";

    /*
     * The image here is generated using the Dockerfile in com.ibm.ws.jdbc_fat_postgresql/publish/files/postgresql-ssl
     * The command used in that directory was: docker build -t jonhawkes/postgresql-ssl:1.0 .
     * With the resulting image being pushed to docker hub.
     */
    public static final String POSTGRES_IMAGE = "jonhawkes/postgresql-ssl:1.0";

    private static DatabaseContainerType databaseContainerType;
    public static JdbcDatabaseContainer<?> testContainer;
    
    private static boolean isHealthy() {
    	return isDerby() || testContainer == null || testContainer.isRunning();
    }

    public static void assertHealthy() {
    	assertTrue(databaseContainerType + " is not healthy", isHealthy());
    }

    public static void assumeHealthy() {
    	assumeTrue(isHealthy());
    }

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

    public static void afterSuite(String ...tables) {
    	dropTables(tables);
    }
    
    public static void showTables() {
    	Log.info(TxTestContainerSuite.class, "showTables", "");
        try (Connection conn = testContainer.createConnection("")) {
        	
            DatabaseMetaData metaData = conn.getMetaData();
            String[] types = {"TABLE"};
            //Retrieving the columns in the database
            try (ResultSet tables = metaData.getTables(null, null, "%", types)) {
            	while (tables.next()) {
            		Log.info(TxTestContainerSuite.class, "showTables", tables.getString("TABLE_NAME"));
            	}
            }
        } catch (SQLException e) {
        	Log.error(TxTestContainerSuite.class, "showTables", e);
        }
    }

    public static void dropTables(String ...tables) {
    	Log.entering(TxTestContainerSuite.class, "dropTables");
    	if (testContainer != null) {
    		try (Connection conn = testContainer.createConnection(""); Statement stmt = conn.createStatement()) {
    			if (tables.length != 0) {
    				Log.info(TxTestContainerSuite.class, "dropTables", "explicit");
    				for (String table : tables) {
    					dropTable(stmt, table);
    				}
    			} else {
    				DatabaseMetaData metaData = conn.getMetaData();
    				String[] types = {"TABLE"};
    				//Retrieving the columns in the database
    				try (ResultSet existing = metaData.getTables(null, null, "%", types)) {
    					while (existing.next()) {
    						dropTable(stmt, existing.getString("TABLE_NAME"));
    					}
    				}
    			}
    		} catch (SQLException e) {
    			Log.error(TxTestContainerSuite.class, "dropTables", e);
    		}
    	}
    }
    
    private static void dropTable(Statement stmt, String table) {
    	try {
    		switch (databaseContainerType) {
    		case Oracle:
            	Log.info(TxTestContainerSuite.class, "dropTables", "DROP TABLE " + table);
				stmt.execute("DROP TABLE " + table);
    			break;
    		default:
            	Log.info(TxTestContainerSuite.class, "dropTables", "DROP TABLE IF EXISTS " + table);
				stmt.execute("DROP TABLE IF EXISTS " + table);
    		}
		} catch (Exception e) {
        	Log.error(TxTestContainerSuite.class, "dropTables", e);
		}
    }

	public static boolean isDerby() {
		return databaseContainerType == DatabaseContainerType.Derby;
	}

	public static void setType(DatabaseContainerType type) {
		databaseContainerType = type;	}
}