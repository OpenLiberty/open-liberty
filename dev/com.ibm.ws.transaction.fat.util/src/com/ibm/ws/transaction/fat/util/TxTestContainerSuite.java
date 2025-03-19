/*******************************************************************************
 * Copyright (c) 2022, 2025 IBM Corporation and others.
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

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.utility.DockerImageName;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.containers.TestContainerSuite;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.DatabaseContainerFactory;
import componenttest.topology.database.container.DatabaseContainerType;

/**
 *
 */
public class TxTestContainerSuite extends TestContainerSuite {
	private static final Class<?> c = TxTestContainerSuite.class;
	
    public static final String POSTGRES_DB = "testdb";
    public static final String POSTGRES_USER = "postgresUser";
    public static final String POSTGRES_PASS = "superSecret";
    
    //TODO Start using ImageBuilder
//    public static final DockerImageName POSTGRES_SSL = ImageBuilder.build("postgres-ssl:17").getDockerImageName();

    /*
     * The image here is generated using the Dockerfile in com.ibm.ws.jdbc_fat_postgresql/publish/files/postgresql-ssl
     * The command used in that directory was: docker build -t jonhawkes/postgresql-ssl:1.0 .
     * With the resulting image being pushed to docker hub.
     */
    public static final DockerImageName POSTGRES_SSL = DockerImageName.parse("jonhawkes/postgresql-ssl:1.0");

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
    
    public static void importServerCert(String source, String serverCert) {
        final String m = "importServerCert";

        String[] command = new String[] {
                                          "keytool", "-import", //
                                          "-alias", "server", //
                                          "-file", serverCert, //
                                          "-keystore", source, //
                                          "-storetype", "pkcs12", //
                                          "-storepass", "liberty", //
                                          "-noprompt"
        };

        String errorPrelude = "Could not import server certificate into client keystore: " + source;
        try {
            Process p = Runtime.getRuntime().exec(command);
            if (!p.waitFor(FATRunner.FAT_TEST_LOCALRUN ? 10 : 20, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                dumpOutput(m, "Keytool process timed out", p);
                throw new RuntimeException(errorPrelude + " timed out waiting for process to finish.");
            }
            if (p.exitValue() != 0) {
                dumpOutput(m, "Non 0 exit code from keytool", p);
                throw new RuntimeException(errorPrelude + " see logs for details");
            }
            dumpOutput(m, "Keytool command completed successfully", p);
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(errorPrelude, e);
        }
    }

    private static void dumpOutput(String method, String message, Process p) {
        String out = "stdOut:" + System.lineSeparator() + readInputStream(p.getInputStream());
        String err = "stdErr:" + System.lineSeparator() + readInputStream(p.getErrorStream());
        Log.info(c, method, message + //
                            System.lineSeparator() + out + //
                            System.lineSeparator() + err);
    }

    private static String readInputStream(InputStream is) {
        @SuppressWarnings("resource")
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

	public static boolean isDerby() {
		return databaseContainerType == DatabaseContainerType.Derby;
	}

	public static void setType(DatabaseContainerType type) {
		databaseContainerType = type;	}
}