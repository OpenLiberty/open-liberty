/*******************************************************************************
 * Copyright (c) 2019, 2025 IBM Corporation and others.
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
package componenttest.topology.database.container;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.testcontainers.containers.Db2Container;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.oracle.OracleContainer;
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
 *
 * NOTE: the order of the elements in this enum matter because {@link #getAnonymousDriverName()}
 * uses the enum ordinal system to construct the name of the anonymous driver programmatically.
 */
@SuppressWarnings("rawtypes")
public enum DatabaseContainerType {
    DB2("jcc.jar", 8, //
        Collections.emptyList(), //
        Db2Container.class.getCanonicalName(), //
        Properties_db2_jcc.class, //
        DockerImageName.parse("icr.io/db2_community/db2:12.1.1.0")//
                        .asCompatibleSubstituteFor("icr.io/db2_community/db2")),
    Derby("derby.jar", 8, //
          Collections.emptyList(), //
          DerbyNoopContainer.class.getCanonicalName(), //
          Properties_derby_embedded.class, //
          DockerImageName.parse(""), //
          "DerbyEmbedded"),
    DerbyClient("derbyclient.jar", 8, //
                Collections.emptyList(), //
                DerbyClientContainer.class.getCanonicalName(), //
                Properties_derby_client.class, //
                DockerImageName.parse("")),
    Oracle("ojdbc8.jar", 8, //
           Collections.emptyList(), //
           OracleContainer.class.getCanonicalName(), //
           Properties_oracle.class, //
           // NOTE: avoid using 23-full-faststart due to breaking changes
           //       https://github.com/gvenzl/oci-oracle-free/issues/122
           DockerImageName.parse("ghcr.io/gvenzl/oracle-free:23.9-full-faststart")//
                           .asCompatibleSubstituteFor("gvenzl/oracle-free"), //
           "OracleDB"),
    Postgres("postgresql.jar", 8, //
             Collections.emptyList(), //
             PostgreSQLContainer.class.getCanonicalName(), //
             Properties_postgresql.class, //
             DockerImageName.parse("public.ecr.aws/docker/library/postgres:17-alpine")//
                             .asCompatibleSubstituteFor("postgres"), //
             "Postgre", "PostgreSQL"),
    SQLServer("mssql-jdbc.jar", 8, //
              Collections.emptyList(), //
              MSSQLServerContainer.class.getCanonicalName(), //
              Properties_microsoft_sqlserver.class, //
              DockerImageName.parse("mcr.microsoft.com/mssql/server:2022-latest")//
                              .asCompatibleSubstituteFor("mcr.microsoft.com/mssql/server"), //
              "MSSQLServer"),
    DerbyJava17Plus("derby.jar", 17, //
                    DerbyJava17PlusContainer.supportLibraries, //
                    DerbyJava17PlusContainer.class.getCanonicalName(), //
                    Properties_derby_embedded.class, //
                    DockerImageName.parse(""), //
                    "DerbyEmbeddedJava17Plus"),
    DerbyClientJava17Plus("derbyclient.jar", 17, //
                          DerbyClientJava17PlusContainer.supportLibraries, //
                          DerbyClientJava17PlusContainer.class.getCanonicalName(), //
                          Properties_derby_client.class, //
                          DockerImageName.parse(""));

    private final String driverName;
    private final int minJavaLevel;
    private final List<String> supportLibraries;
    private final Class<DataSourceProperties> dsPropsClass;
    private final Class<? extends JdbcDatabaseContainer> containerClass;
    private final DockerImageName imageName;
    private final List<String> aliases;

    @SuppressWarnings("unchecked")
    DatabaseContainerType(final String driverName, final int minJavaLevel, final List<String> supportLibraries,
                          final String containerClassName, final Class dsPropsClass,
                          final DockerImageName imageName, final String... aliases) {
        this.driverName = driverName;
        this.minJavaLevel = minJavaLevel;
        this.supportLibraries = supportLibraries;

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
        this.aliases = Arrays.asList(aliases);
    }

    /**
     * Returns the common JDBC Driver name for this testcontainer type.
     * Example: 'ojdbc8.jar'
     *
     * @return String - JDBC Driver Name
     */
    public String getDriverName() {
        return this.driverName;
    }

    /**
     * Returns the minimum java level for the JDBC Driver for this testcontainer type.
     * Example: 'ojdbc8.jar' will return 8
     *
     * @return int - minimum java level major version
     */
    public int getMinJavaLevel() {
        return this.minJavaLevel;
    }

    /**
     * Returns the support library names for this testcontainer type.
     * Example: 'derbytools.jar'
     *
     * @return List - of support libraries, empty list if none.
     */
    public List<String> getSupportLibraries() {
        return this.supportLibraries;
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
     * Returns anonymized support library names for this testcontainer type.
     * Example: 'lib1.jar'
     *
     * TODO investigate if we need to implement this, is it possible to determine the database
     * type based on support library names?
     *
     * @return List - of support libraries, empty list if none.
     */
    public List<String> getAnonymousSupportLibraries() {
        throw new UnsupportedOperationException("This functionality has not been implemented yet.");
    }

    /**
     * Returns a stream which includes the JDBC driver name, and the names
     * of all of the supporting libraries for this testcontainer type.
     *
     * Example: '{derby.jar, derbytools.jar, derbyshared.jar}'
     *
     * @return stream - all artifacts related to this testcontainer type
     */
    public Stream<String> streamAllArtifacts() {
        return Stream.concat(Stream.of(this.driverName), this.supportLibraries.stream());
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
     * Given a database name return the corresponding Database Container Type.
     * This is more forgiving then the implicit valueOf method which is case-sensitive
     * and does not allow for aliases
     *
     * @param  alias - the name of the database or an alias
     * @return       - DatabaseContainerType
     */
    public static DatabaseContainerType valueOfAlias(String alias) {
        IllegalArgumentException caught;

        // Find direct match
        try {
            return valueOf(alias);
        } catch (IllegalArgumentException e) {
            caught = e; //rethrow if we never find a match
        }

        // Find case insensitive
        for (DatabaseContainerType type : DatabaseContainerType.values()) {
            if (type.name().equalsIgnoreCase(alias)) {
                return type;
            }

            // Find case insensitive from list of aliases
            for (String typeAlias : type.aliases) {
                if (typeAlias.equalsIgnoreCase(alias)) {
                    return type;
                }
            }
        }

        // Did not find, throw original exception
        throw caught;
    }

    public <T> T cast(Object instance) {
        return (T) this.getContainerClass().cast(instance);
    }

    /**
     * Given a JDBC testcontainer return the corresponding Database Container Type.
     *
     * @param  cont - A database container.
     * @return      - DatabaseContainerType
     */
    public static DatabaseContainerType valueOf(JdbcDatabaseContainer cont) {
        for (DatabaseContainerType elem : values())
            if (elem.getContainerClass().equals(cont.getClass()))
                return elem;
        throw new IllegalArgumentException("Unrecognized JdbcDatabaseContainer class: " + cont.getClass().getCanonicalName());
    }

    public static List<String> images() {
        ArrayList<String> images = new ArrayList<>();
        for (DatabaseContainerType elem : values()) {
            if (!elem.getImageName().getUnversionedPart().isEmpty()) {
                images.add(elem.getImageName().asCanonicalNameString());
            }
        }
        return images;
    }

    //Private Method: used to setup logging for containers to this class.
    public void log(OutputFrame frame) {
        String msg = frame.getUtf8String();
        if (msg.endsWith("\n"))
            msg = msg.substring(0, msg.length() - 1);
        Log.info(this.containerClass, "[" + name() + "]", msg);
    }
}
