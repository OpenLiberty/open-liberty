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

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.output.OutputFrame;

import com.ibm.websphere.simplicity.log.Log;

/**
 * This is a current list of database test-containers that are in the database rotation.
 */
@SuppressWarnings("rawtypes")
public enum DatabaseContainerType {
    DB2("jcc.jar", "org.testcontainers.containers.", "Db2Container"),
    Derby("derby.jar", "componenttest.topology.database.container.", "DerbyNoopContainer"),
    DerbyClient("derbyclient.jar", "componenttest.topology.database.container.", "DerbyClientNoopContainer"),
    Oracle("ojdbc8_g.jar", "org.testcontainers.containers.", "OracleContainer"),
    Postgres("postgresql.jar", "org.testcontainers.containers.", "PostgreSQLContainer"),
    SQLServer("mssql-jdbc.jar", "componenttest.topology.database.container.", "SQLServerContainer");

    private String driverName;
    private Class<? extends JdbcDatabaseContainer> clazz;

    @SuppressWarnings("unchecked")
	DatabaseContainerType(final String driverName, final String packageName, final String className) {
        this.driverName = driverName;
        
        //Use reflection to get class at runtime.
        Class clazz = null;
		try {
			clazz = (Class<? extends JdbcDatabaseContainer>) Class.forName(packageName + className);
		} catch (ClassNotFoundException e) {
			fail("Could not find class: " + className);
		}
        this.clazz = clazz;
    }

    /**
     * Returns the common JDBC Driver name for this test-container type.
     * Example: 'ojdbc8_g.jar'
     *
     * @return String - JDBC Driver Name
     */
    public String getDriverName() {
        return driverName;
    }

    /**
     * Returns TestContainer class associated with this TestContainer type.
     *
     * @return Java Class
     */
    public Class getContainerClass() {
        return clazz;
    }

    /**
     * Given a JDBC TestContainer return the corresponding Database Container Type.
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
        Log.info(this.clazz, "output", msg);
    }
}
