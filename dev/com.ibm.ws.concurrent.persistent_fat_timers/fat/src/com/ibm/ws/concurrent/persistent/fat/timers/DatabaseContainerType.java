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

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.Db2Container;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.OutputFrame;

import com.ibm.websphere.simplicity.log.Log;

/**
 * This is a current list of database test-containers that are in the database rotation.
 *
 * TODO: Since this enum is general it should be moved to fattest simplicity in the future
 */
@SuppressWarnings("rawtypes")
public enum DatabaseContainerType {
    DB2("db2jcc.jar", Db2Container.class),
    Derby("derby.jar", DerbyNoopContainer.class),
    Oracle("ojdbc8_g.jar", OracleContainer.class),
    Postgre("postgresql.jar", PostgreSQLContainer.class),
    SQLServer("mssql-jdbc.jar", MSSQLServerContainer.class);

	private String driverName;
	private Class<? extends JdbcDatabaseContainer> clazz;
	
    DatabaseContainerType(final String driverName, final Class<? extends JdbcDatabaseContainer> clazz) {
        this.driverName = driverName;
        this.clazz = clazz;
    }

    /**
     * Returns the common JDBC Driver name for this test-container type.
     * Example: 'ojdbc8_g.jar'
     * @return String - JDBC Driver Name
     */
    public String getDriverName() {
        return driverName;
    }
    
    /**
     * Returns test-container class associated with this test-container type.
     * @return Java Class
     */
    public Class getContainerClass() {
    	return clazz;
    }
    
    /**
     * Given a JDBC test-container return the corresponding Database Container Type.
     * @param cont - A database container. 
     * @return DatabaseContainerType - type enum
     */
    public static DatabaseContainerType valueOf(JdbcDatabaseContainer cont) {
    	for(DatabaseContainerType elem : values())
    		if(elem.getContainerClass() == cont.getClass())
    			return elem;
    	throw new IllegalArgumentException("Unrecognized JdbcDatabaseContainer class: " + cont.getClass().getCanonicalName());
    }
    
	//Private Method: used to setup logging for containers to this class. 
	public void log(OutputFrame frame) {
        String msg = frame.getUtf8String();
        if (msg.endsWith("\n"))
            msg = msg.substring(0, msg.length() - 1);
        Log.info(this.clazz, "output", msg);
	}
}
