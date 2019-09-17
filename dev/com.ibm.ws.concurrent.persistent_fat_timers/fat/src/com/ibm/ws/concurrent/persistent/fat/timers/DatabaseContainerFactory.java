package com.ibm.ws.concurrent.persistent.fat.timers;

import java.time.Duration;

import org.testcontainers.containers.Db2Container;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;

/**
 * TODO: Since this factory class is general it should be moved to fattest simplicity in the future. </br>
 * 
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
 * Postgre: Uses <a href="https://hub.docker.com/_/postgres">Offical Postgres Container</a> <br>
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
	 * [Postgre, DB2, Oracle, SQLServer, Derby]
	 * 
	 * If {fat.bucket.db.type} is not set with a value, 
	 * default to Derby Embedded.
	 * 
	 * @param databaseName - String passed to database container for those database containers 
	 * that can use a custom database.
	 * 
	 * @return JdbcDatabaseContainer - The test container.
	 * 
	 * @throws IllegalArgumentException - if database rotation {fat.test.databases} is not set or is false, 
	 * or database type {fat.bucket.db.type} is unsupported.
	 */
	public static JdbcDatabaseContainer<?> create(String databaseName) throws IllegalArgumentException{
		String dbRotation= System.getProperty("fat.test.databases");
		String dbProperty = System.getProperty("fat.bucket.db.type", "Derby");
		
		Log.info(c, "create", "System property: fat.test.databases is " + dbRotation);
		Log.info(c, "create", "System property: fat.bucket.db.type is " + dbProperty);
		
		if(!"true".equals(dbRotation)) {
			throw new IllegalArgumentException("To use a generic database, the FAT must be opted into database rotation by setting 'fat.test.databases: true' in the FAT project's bnd.bnd file");
		}

		DatabaseContainerType type = null;
		try {
			type = DatabaseContainerType.valueOf(dbProperty);
			Log.info(c, "create", "FOUND: database test-container type: " + type);
		} catch (IllegalArgumentException  e) {
			throw new IllegalArgumentException("No database test-container supported for " + dbProperty, e);
		}
		
		return initContainer(type, databaseName);
	}
	
	//Private Method: used to initialize test container.
	private static JdbcDatabaseContainer<?> initContainer(DatabaseContainerType dbContainerType, String databaseName) {
		//Check to see if JDBC Driver is available. 
		DatabaseContainerUtil.isJdbcDriverAvailable(dbContainerType);
		
		//Create container
		JdbcDatabaseContainer<?> cont = null;
		
		switch(dbContainerType) {
		case DB2:
			cont = new Db2Container().acceptLicense()
				  // Use 5m timeout for local runs, 15m timeout for remote runs
				  .withStartupTimeout(Duration.ofMinutes(FATRunner.FAT_TEST_LOCALRUN ? 5 : 15))
				  .withLogConsumer(dbContainerType::log);
			break;
		case Derby:
			cont = new DerbyNoopContainer<>()
					.withDatabaseName("memory:" + databaseName)
					.withLogConsumer(dbContainerType::log);
			break;
		case Oracle:
			//TODO replace this container with the official oracle-xe container if/when it is available without a license
			cont = new OracleContainer("oracleinanutshell/oracle-xe-11g")
					.withLogConsumer(dbContainerType::log);
			break;
		case Postgre:
			cont = new PostgreSQLContainer<>()
					.withDatabaseName(databaseName)
					.withLogConsumer(dbContainerType::log);
			break;
		case SQLServer:
			//TODO Update this to use the PRODUCTION SQL Server 2019 container when available.  Currently using a preview version.
			cont = new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2019-CTP3.1-ubuntu")
					.withLogConsumer(dbContainerType::log);
			break;
    	}
		
		return cont;
	}
}
