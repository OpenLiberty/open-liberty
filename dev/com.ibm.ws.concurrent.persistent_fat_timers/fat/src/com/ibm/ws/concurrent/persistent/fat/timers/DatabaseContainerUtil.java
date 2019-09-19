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
package com.ibm.ws.concurrent.persistent.fat.timers;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MSSQLServerContainer;

import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.DataSource;
import com.ibm.websphere.simplicity.config.DataSourceProperties;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.dsprops.Properties;
import com.ibm.websphere.simplicity.config.dsprops.Properties_db2_jcc;
import com.ibm.websphere.simplicity.config.dsprops.Properties_derby_embedded;
import com.ibm.websphere.simplicity.config.dsprops.Properties_microsoft_sqlserver;
import com.ibm.websphere.simplicity.config.dsprops.Properties_oracle;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

public class DatabaseContainerUtil {
	//Logging Constants
	private static final Class<DatabaseContainerUtil> c = DatabaseContainerUtil.class;
	private static final String nl = System.getProperty("line.separator");
	
	/**
	 * Method that does any database setup necessary prior to making a connection 
	 * to the database from a servlet. 
	 * 
	 * @param cont - Test container being used.
	 * @param databaseName - Name to be given to database instance.
	 * @throws SQLException - Thrown if there is a failure during connection or initialization. 
	 */
	public static void initDatabase(JdbcDatabaseContainer<?> cont, String databaseName) throws SQLException {
		if(cont instanceof MSSQLServerContainer) {
			//Create database
	        try (Connection conn = cont.createConnection("")) {
	            Statement stmt = conn.createStatement();
	            stmt.execute("CREATE DATABASE [" + databaseName + "];");
	            stmt.close();
	        }

	        //Setup distributed connections
	        try (Connection conn = cont.createConnection("")) {
	            Statement stmt = conn.createStatement();
	            stmt.execute("EXEC sp_sqljdbc_xa_install");
	            stmt.close();
	        }
		}
	} 
	
    /**
     * Check to see if the JDBC driver necessary for this test-container is in the location
     * where the server expects to find it. <br>
     * 
     * JDBC drivers are not publicly available for some databases. In those cases the
     * driver will need to be provided by the user to run this test-container. 
     * @return boolean - true if and only if driver exists.  Otherwise, false.
     */
	public static boolean isJdbcDriverAvailable(JdbcDatabaseContainer<?> cont) {
		return isJdbcDriverAvailable(DatabaseContainerType.valueOf(cont));
	}
	
	/**
	 * @see #isJdbcDriverAvailable(JdbcDatabaseContainer<?> cont)
	 */
    public static boolean isJdbcDriverAvailable(DatabaseContainerType type) {  	
    	File temp = new File("publish/shared/resources/jdbc/" + type.getDriverName());
    	boolean result = temp.exists();
    	
    	if(result) {
    		Log.info(c, "isJdbcDriverAvailable", "FOUND: " + type + " JDBC driver in location: " + temp.getAbsolutePath());
    	} else {
    		Log.warning(c, "MISSING: " + type + " JDBC driver not in location: " + temp.getAbsolutePath());
    	}
    	 
        return result;
    }
	
    /**
     * For use when attempting to use <b>database rotation</b>. <br>
     * 
     * Retrieves database properties from the provided JdbcDatabaseContainer, such as;
     * username, password, etc. <br>
     * 
     * Using the ServerConfiguration API. Retrieves all &lt;dataSource&gt; elements and modifies 
     * those that have the <b>fat.modify=true</b> attribute set. <br>
     * 
     * This will replace datasource database &lt;properties... &gt; with the corresponding properties
     * for the provided JdbcDatabaseContainer. <br>
     * 
     * Finally updates server config.
     * 
     * @see com.ibm.websphere.simplicity.config.ServerConfiguration
     * 
     * @param serv - LibertyServer server instance being used for this FAT suite.
     * @param cont - JdbcDatabaseContainer instance being used for database connectivity.
     * 
     * @throws Exception 
     * @throws CloneNotSupportedException 
     */
    public static void setupDataSourceProperties(LibertyServer serv, JdbcDatabaseContainer<?> cont) throws CloneNotSupportedException, Exception {
    	//Properties common to all containers
    	String user = cont.getUsername();
    	String password = cont.getPassword();
    	
    	//Properties not supported by all databases
    	String serverName = null;
    	String port = null;
    	String databaseName = null;
    	
    	try { serverName = cont.getContainerIpAddress(); } catch (UnsupportedOperationException e) {}
    	
    	try { port = Integer.toString(cont.getFirstMappedPort()); } catch (UnsupportedOperationException e) {}
    	
    	try { databaseName = cont.getDatabaseName(); } catch (UnsupportedOperationException e) {}
    	
        //Log results
        Log.info(c, "setupDataSourceProperties", "Using datasource properties:"
        		+ nl + "DatabaseName: " + databaseName
        		+ nl + "Username: " + user + nl + "Password: " + password 
        		+ nl + "ServerName: " + serverName + nl + "Port: " + port);
        
        
        //Get current server config
    	ServerConfiguration cloneConfig = serv.getServerConfiguration().clone();
    	
    	//Get DataSource configs
    	ConfigElementList<DataSource> dataSources = cloneConfig.getDataSources();
    	
    	for(DataSource ds : dataSources) {
    		if(ds.getFatModify() != null && ds.getFatModify().equalsIgnoreCase("true")) {
    			Log.info(c, "setupDataSourceProperties", "FOUND: DataSource to be enlisted in database rotation.  ID: " + ds.getId());
    			
    			DatabaseContainerType type = DatabaseContainerType.valueOf(cont);
    	    	
    			//Update properties based on type    	
    	    	DataSourceProperties props = null;
    	    	
    	    	switch(type) {
    			case DB2:
    				props = new Properties_db2_jcc();
    				ds.clearDataSourceDBProperties();
    				ds.getProperties_db2_jcc().add((Properties_db2_jcc) props);
    				break; //Set general props
    			case Derby:
    				Properties_derby_embedded propsDerby = new Properties_derby_embedded();
    				ds.clearDataSourceDBProperties();
    				ds.getProperties_derby_embedded().add(propsDerby);
    				propsDerby.setUser(user);
    				propsDerby.setPassword(password);
    				propsDerby.setDatabaseName(databaseName);
    				propsDerby.setCreateDatabase("create");
    				serv.updateServerConfiguration(cloneConfig);
    				return;
    			case Oracle:
    				Properties_oracle propsOracle = new Properties_oracle();
    				ds.clearDataSourceDBProperties();
    				ds.getProperties_oracle().add(propsOracle);
    				propsOracle.setUser(user);
    				propsOracle.setPassword(password);
    				propsOracle.setURL(cont.getJdbcUrl());
    				serv.updateServerConfiguration(cloneConfig);
    				return;
    			case Postgre:
    				props = new Properties();
    				ds.clearDataSourceDBProperties();
    				ds.getProperties().add((Properties) props);
    				break; //Set general props
    			case SQLServer:
    				props = new Properties_microsoft_sqlserver();
    				ds.clearDataSourceDBProperties();
    				ds.getProperties_microsoft_sqlserver().add((Properties_microsoft_sqlserver) props);
    				break; //Set general props
    	    	}
    	    	
    	    	props.setUser(user);
    	    	props.setPassword(password);
    	    	
    	    	if(serverName != null)
    	    		props.setServerName(serverName);
    	    	
    	    	if(port != null)
    	    		props.setPortNumber(port);
    	    	    	    	
    	    	if(databaseName != null)
    	    		props.setDatabaseName(databaseName);
    	    	
    	    	serv.updateServerConfiguration(cloneConfig);
    		}//end if
    	}//end for
    }
}
