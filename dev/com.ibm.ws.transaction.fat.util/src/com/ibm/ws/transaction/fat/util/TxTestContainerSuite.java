/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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

    public static void afterSuite(String ...tables) {
    	dropTables(tables);
    }
    
    public static void showTables() {
    	Log.info(TxTestContainerSuite.class, "showTables", "");
        try (Connection conn = testContainer.createConnection("")) {
        	
            DatabaseMetaData metaData = conn.getMetaData();
            String[] types = {"TABLE"};
            //Retrieving the columns in the database
            ResultSet tables = metaData.getTables(null, null, "%", types);
            while (tables.next()) {
            	Log.info(TxTestContainerSuite.class, "showTables", tables.getString("TABLE_NAME"));
            }
        } catch (SQLException e) {
        	Log.error(TxTestContainerSuite.class, "showTables", e);
        }
    }

    public static void dropTables(String ...tables) {
    	Log.entering(TxTestContainerSuite.class, "dropTables");
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
        		ResultSet existing = metaData.getTables(null, null, "%", types);
        		while (existing.next()) {
        			dropTable(stmt, existing.getString("TABLE_NAME"));
        		}
        	}
        } catch (SQLException e) {
        	Log.error(TxTestContainerSuite.class, "dropTables", e);
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