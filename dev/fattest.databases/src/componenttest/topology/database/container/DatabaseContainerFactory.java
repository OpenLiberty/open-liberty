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
package componenttest.topology.database.container;

import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.function.Consumer;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.output.OutputFrame;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;

/**
 * This is a factory class that creates database test-containers.
 * The test container returned will be based on the {fat.bucket.db.type}
 * system property. </br>
 *
 * The {fat.bucket.db.type} property is set to different databases
 * by our test infrastructure when a fat-suite is enlisted in
 * database rotation by setting the property {fat.test.databases} to true.</br>
 *
 * <br> Container Information: <br>
 * DERBY: Uses a derby no-op test container <br>
 * DB2: Uses <a href="https://hub.docker.com/r/ibmcom/db2">Offical DB2 Container</a> <br>
 * Oracle: TODO replace this container with the official oracle-xe container if/when it is available without a license. <br>
 * Postgres: Uses <a href="https://hub.docker.com/_/postgres">Offical Postgres Container</a> <br>
 * MS SQL Server: Uses <a href="https://hub.docker.com/_/microsoft-mssql-server">Offical Microsoft SQL Container</a> <br>
 *
 * @see DatabaseContainerType
 */
public class DatabaseContainerFactory {
    private static final Class<DatabaseContainerFactory> c = DatabaseContainerFactory.class;

    /**
     * Used for <b>database rotation testing</b>.
     *
     * Reads the {fat.bucket.db.type} system property and
     * returns a container based on that property.
     * [Postgres, DB2, Oracle, SQLServer, Derby]
     *
     * If {fat.bucket.db.type} is not set with a value,
     * default to Derby Embedded.
     *
     * @param databaseName - String passed to derby embedded container. Example: "memory:test" or "test"
     *
     * @return JdbcDatabaseContainer - The test container.
     *
     * @throws IllegalArgumentException - if database rotation {fat.test.databases} is not set or is false,
     *                                      or database type {fat.bucket.db.type} is unsupported.
     */
    public static JdbcDatabaseContainer<?> create() throws IllegalArgumentException {
        String dbRotation = System.getProperty("fat.test.databases");
        String dbProperty = System.getProperty("fat.bucket.db.type", "Derby");

        Log.info(c, "create", "System property: fat.test.databases is " + dbRotation);
        Log.info(c, "create", "System property: fat.bucket.db.type is " + dbProperty);

        if (!"true".equals(dbRotation)) {
            throw new IllegalArgumentException("To use a generic database, the FAT must be opted into database rotation by setting 'fat.test.databases: true' in the FAT project's bnd.bnd file");
        }

        DatabaseContainerType type = null;
        try {
            type = DatabaseContainerType.valueOf(dbProperty);
            Log.info(c, "create", "FOUND: database test-container type: " + type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("No database test-container supported for " + dbProperty, e);
        }

        return initContainer(type);
    }

    //Private Method: used to initialize test container.
    private static JdbcDatabaseContainer<?> initContainer(DatabaseContainerType dbContainerType) {
        //Check to see if JDBC Driver is available.
        isJdbcDriverAvailable(dbContainerType);

        //Create container
        JdbcDatabaseContainer<?> cont = null;
        Class<?> clazz = dbContainerType.getContainerClass();

        try {
	        switch (dbContainerType) {
	            case DB2:
	                cont = (JdbcDatabaseContainer<?>) clazz.getConstructor().newInstance();
	                //Accept License agreement
	            	Method acceptDB2License = cont.getClass().getMethod("acceptLicense");
	            	acceptDB2License.invoke(cont);
	            	//Add startup timeout since DB2 tends to take longer than the default 3 minutes on build machines. 
	            	Method withStartupTimeout = cont.getClass().getMethod("withStartupTimeout", Duration.class);
	            	withStartupTimeout.invoke(cont, Duration.ofMinutes(FATRunner.FAT_TEST_LOCALRUN ? 5 : 15));
	                break;
	            case Derby:
	            	cont = (JdbcDatabaseContainer<?>) clazz.getConstructor().newInstance();
	                break;
	            case Oracle:          	
	            	cont = (JdbcDatabaseContainer<?>) clazz.getConstructor(String.class).newInstance("oracleinanutshell/oracle-xe-11g");
	                break;
	            case Postgres:
	            	cont = (JdbcDatabaseContainer<?>) clazz.getConstructor().newInstance();
	                break;
	            case SQLServer:
	            	cont = (JdbcDatabaseContainer<?>) clazz.getConstructor().newInstance();
	            	//Accept license agreement
	            	Method acceptSQLServerLicense = cont.getClass().getMethod("acceptLicense");
	            	acceptSQLServerLicense.invoke(cont);
	            	//Init Script
	            	Method initScript = cont.getClass().getMethod("withInitScript", String.class);
	            	initScript.invoke(cont, "resources/init-sqlserver.sql");
	                break;
	        }
	        
	        //Allow each container to log to output.txt
	        Method withLogConsumer = cont.getClass().getMethod("withLogConsumer", Consumer.class);
	        withLogConsumer.invoke(cont, (Consumer<OutputFrame>) dbContainerType::log);
        
        } catch (Exception e) {
        	e.printStackTrace();
        	fail("Unable to create a " + dbContainerType.name() + " TestContainer instance.");
        }
        
        return cont;
    }

    /**
     * Check to see if the JDBC driver necessary for this test-container is in the location
     * where the server expects to find it. <br>
     *
     * JDBC drivers are not publicly available for some databases. In those cases the
     * driver will need to be provided by the user to run this test-container.
     *
     * @return boolean - true if and only if driver exists. Otherwise, false.
     */
    private static boolean isJdbcDriverAvailable(DatabaseContainerType type) {
        File temp = new File("publish/shared/resources/jdbc/" + type.getDriverName());
        boolean result = temp.exists();

        if (result) {
            Log.info(c, "isJdbcDriverAvailable", "FOUND: " + type + " JDBC driver in location: " + temp.getAbsolutePath());
        } else {
            Log.warning(c, "MISSING: " + type + " JDBC driver not in location: " + temp.getAbsolutePath());
        }

        return result;
    }
}
