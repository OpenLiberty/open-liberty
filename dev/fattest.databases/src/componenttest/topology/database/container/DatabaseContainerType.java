/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.database.container;

import java.lang.reflect.Constructor;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.output.OutputFrame;

import com.ibm.websphere.simplicity.config.DataSourceProperties;
import com.ibm.websphere.simplicity.log.Log;

/**
 * This is a current list of database testcontainers that are in the database rotation.
 */
@SuppressWarnings("rawtypes")
public enum DatabaseContainerType {
    DB2("jcc.jar", "org.testcontainers.containers.Db2Container", "Properties_db2_jcc"),
    Derby("derby.jar", "componenttest.topology.database.container.DerbyNoopContainer", "Properties_derby_embedded"),
    DerbyClient("derbyclient.jar", "componenttest.topology.database.container.DerbyClientContainer", "Properties_derby_client"),
    Oracle("ojdbc8_g.jar", "componenttest.topology.database.container.OracleContainer", "Properties_oracle"),
    Postgres("postgresql.jar", "org.testcontainers.containers.PostgreSQLContainer", "Properties_postgresql"),
    SQLServer("mssql-jdbc.jar", "componenttest.topology.database.container.SQLServerContainer", "Properties_microsoft_sqlserver");

    private final String driverName;
    private final Class<DataSourceProperties> dsPropsClass;
    private final Class<? extends JdbcDatabaseContainer> containerClass;
    

    @SuppressWarnings("unchecked")
	DatabaseContainerType(final String driverName, final String containerClassName, final String dataSourcePropertiesClassName) {
        this.driverName = driverName;
        
        //Use reflection to get classes at runtime.
        Class containerClass = null, dsPropsClass  = null;
		try {
			containerClass = (Class<? extends JdbcDatabaseContainer>) Class.forName(containerClassName);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Could not find the container class: " + containerClassName + " for testconatiner type: " + this.name(), e);
		}
		
		try {
			dsPropsClass = (Class<DataSourceProperties>) Class.forName("com.ibm.websphere.simplicity.config.dsprops." + dataSourcePropertiesClassName);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Could not find the datasource properties class: " + dataSourcePropertiesClassName + " for testconatiner type: " + this.name(), e);
		}
		
        this.containerClass = containerClass;
        this.dsPropsClass = dsPropsClass;
    }

    /**
     * Returns the common JDBC Driver name for this testcontainer type.
     * Example: 'ojdbc8_g.jar'
     *
     * @return String - JDBC Driver Name
     */
    public String getDriverName() {
        return driverName;
    }
    
    /**
     * Returns an anonymized JDBC Driver name for this testcontainer type.
     * Example: 'driver2.jar'
     *
     * @return String - JDBC Driver Name
     */
    public String getAnonymousDriverName() {
        return "driver" + this.ordinal() + ".jar";
    }

    /**
     * Returns the testcontainer class associated with this testcontainer type.
     *
     * @return Java Class
     */
    public Class getContainerClass() {
        return containerClass;
    }
    
    /**
     * Returns an instance of this testcontainer's datasource properties. 
     */
    public DataSourceProperties getDataSourceProps() throws ReflectiveOperationException{
    	DataSourceProperties props = null;
    	try {
    		Constructor ctor = this.dsPropsClass.getConstructor();
    		props = (DataSourceProperties) ctor.newInstance();
    	} catch (Exception e) {
    		throw new ReflectiveOperationException("Failed to create instance of DataSourceProperites using reflection.", e);
    	}
    	
    	return props;
    }

    /**
     * Given a JDBC testcontainer return the corresponding Database Container Type.
     *
     * @param cont - A database container.
     * @return DatabaseContainerType - type enum
     */
    public static DatabaseContainerType valueOf(JdbcDatabaseContainer cont) {
        for (DatabaseContainerType elem : values())
            if (elem.getContainerClass() == cont.getClass())
                return elem;
        throw new IllegalArgumentException("Unrecognized JdbcDatabaseContainer class: " + cont.getClass().getCanonicalName());
    }

    //Private Method: used to setup logging for containers to this class.
    public void log(OutputFrame frame) {
        String msg = frame.getUtf8String();
        if (msg.endsWith("\n"))
            msg = msg.substring(0, msg.length() - 1);
        Log.info(this.containerClass, "[" + name() + "]", msg);
    }
}
