/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
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

import org.testcontainers.containers.Db2Container;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.utility.DockerImageName;

import com.ibm.websphere.simplicity.config.DataSourceProperties;
import com.ibm.websphere.simplicity.config.dsprops.Properties_db2_jcc;
import com.ibm.websphere.simplicity.config.dsprops.Properties_derby_client;
import com.ibm.websphere.simplicity.config.dsprops.Properties_derby_embedded;
import com.ibm.websphere.simplicity.config.dsprops.Properties_microsoft_sqlserver;
import com.ibm.websphere.simplicity.config.dsprops.Properties_oracle;
import com.ibm.websphere.simplicity.config.dsprops.Properties_postgresql;
import com.ibm.websphere.simplicity.log.Log;

/**
 * This is a current list of database testcontainers that are in the database rotation.
 */
@SuppressWarnings("rawtypes")
public enum DatabaseContainerType {
    DB2("jcc.jar", Db2Container.class.getCanonicalName(), Properties_db2_jcc.class, DockerImageName.parse("ibmcom/db2:11.5.7.0")),
    Derby("derby.jar", DerbyNoopContainer.class.getCanonicalName(), Properties_derby_embedded.class, DockerImageName.parse("")),
    DerbyClient("derbyclient.jar", DerbyClientContainer.class.getCanonicalName(), Properties_derby_client.class, DockerImageName.parse("")),
    Oracle("ojdbc8_g.jar", OracleContainer.class.getCanonicalName(), Properties_oracle.class, //
           DockerImageName.parse("kyleaure/oracle-18.4.0-expanded:1.0.slim").asCompatibleSubstituteFor("gvenzl/oracle-xe")),
    Postgres("postgresql.jar", PostgreSQLContainer.class.getCanonicalName(), Properties_postgresql.class, //
             DockerImageName.parse("postgres:9.6.12")),
    SQLServer("mssql-jdbc.jar", MSSQLServerContainer.class.getCanonicalName(), Properties_microsoft_sqlserver.class, //
              DockerImageName.parse("mcr.microsoft.com/mssql/server:2019-CU10-ubuntu-16.04"));

    private final String driverName;
    private final Class<DataSourceProperties> dsPropsClass;
    private final Class<? extends JdbcDatabaseContainer> containerClass;
    private final DockerImageName imageName;

    @SuppressWarnings("unchecked")
    DatabaseContainerType(final String driverName, final String containerClassName, final Class dsPropsClass, final DockerImageName imageName) {
        this.driverName = driverName;

        //Use reflection to get classes at runtime.
        Class containerClass = null;
        try {
            containerClass = Class.forName(containerClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not find the container class: " + containerClassName + " for testconatiner type: " + this.name(), e);
        }

        this.containerClass = containerClass;
        this.dsPropsClass = dsPropsClass;
        this.imageName = imageName;
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
     * Returns the default image name for this testcontainer type.
     *
     * @return String - Image Name
     */
    public DockerImageName getImageName() {
        return imageName;
    }

    /**
     * Returns an instance of this testcontainer's datasource properties.
     */
    public DataSourceProperties getDataSourceProps() throws ReflectiveOperationException {
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
     * @param  cont - A database container.
     * @return      DatabaseContainerType - type enum
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
