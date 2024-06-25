/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.config.AuthData;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.DataSource;
import com.ibm.websphere.simplicity.config.DataSourceProperties;
import com.ibm.websphere.simplicity.config.DatabaseStore;
import com.ibm.websphere.simplicity.config.Fileset;
import com.ibm.websphere.simplicity.config.JavaPermission;
import com.ibm.websphere.simplicity.config.Library;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.Variable;
import com.ibm.websphere.simplicity.config.dsprops.Properties;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

public final class DatabaseContainerUtil {
    //Logging Constants
    private static final Class<DatabaseContainerUtil> c = DatabaseContainerUtil.class;

    private static String DRIVER_REPLACEMENT = "${env.DB_DRIVER}";

    private enum AuthLocation {
        inProperties,
        inAuthDataRef,
        inEmbeddedAuthData,
        none;

        public String id;

        public AuthLocation withID(String id) {
            this.id = id;
            return this;
        }
    }

    private DatabaseContainerUtil() {
        //No objects should be created from this class
    }

    /**
     * performs the same property substitution as {@link DatabaseContainerUtil#setupDataSourceDatabaseProperties(LibertyServer, JdbcDatabaseContainer)}
     * but ensures we find and swap ${DB_DRIVER} from all filesets because the JVM will return null for env vars before checkpoint.
     */
    public static void setupDataSourcePropertiesForCheckpoint(LibertyServer serv, JdbcDatabaseContainer<?> cont) throws CloneNotSupportedException, Exception {
        boolean isDerby = DatabaseContainerType.valueOf(cont) == DatabaseContainerType.Derby || DatabaseContainerType.valueOf(cont) == DatabaseContainerType.DerbyClient;

        //Get server config
        ServerConfiguration cloneConfig = serv.getServerConfiguration().clone();
        //Get datasources to be changed or skip if derby
        Map<String, DataSource> datasources = isDerby ? Collections.emptyMap() : getDataSources(cloneConfig);
        //Get authdatas to be changed or skip if derby
        Map<String, AuthData> authDatas = isDerby ? Collections.emptyMap() : getAuthDatas(cloneConfig);
        //Get libraries to be changed
        Map<String, Fileset> libraries = getLibraries(cloneConfig);
        //Modify those datasources
        modifyDataSourcePropsForDatabase(datasources, authDatas, libraries, cloneConfig, serv, cont, true);
    }

    public static void setupDataSourceEnvForCheckpoint(LibertyServer serv, JdbcDatabaseContainer<?> cont) throws Exception {
        java.util.Properties envProps = serv.getServerEnv();
        envProps.put(DB_DRIVER, DatabaseContainerType.valueOf(cont).getDriverName());
        boolean isDerby = DatabaseContainerType.valueOf(cont) == DatabaseContainerType.Derby || DatabaseContainerType.valueOf(cont) == DatabaseContainerType.DerbyClient;
        if (!isDerby) {
            envProps.put(DB_HOST, cont.getHost());
            envProps.put(DB_PORT, Integer.toString(cont.getFirstMappedPort()));
            envProps.put(DB_USER, cont.getUsername());
            envProps.put(DB_PASSWORD, cont.getPassword());
        }
        File serverEnvFile = new File(serv.getFileFromLibertyServerRoot("server.env").getAbsolutePath());
        try (OutputStream out = new FileOutputStream(serverEnvFile)) {
            envProps.store(out, "");
        }
    }

    /**
     * Performs the same property substitution as {@link DatabaseContainerUtil#setupDataSourceProperties(LibertyServer, JdbcDatabaseContainer)}
     * but ensures that we use properties.{database} instead of generic properties.
     */
    public static void setupDataSourceDatabaseProperties(LibertyServer serv, JdbcDatabaseContainer<?> cont) throws CloneNotSupportedException, Exception {
        //Skip for Derby and DerbyClient
        if (DatabaseContainerType.valueOf(cont) == DatabaseContainerType.Derby ||
            DatabaseContainerType.valueOf(cont) == DatabaseContainerType.DerbyClient)
            return; //Derby used by default no need to change DS properties

        //Get server config
        ServerConfiguration cloneConfig = serv.getServerConfiguration().clone();
        //Get datasources to be changed
        Map<String, DataSource> datasources = getDataSources(cloneConfig);
        //Get authdatas to be changed
        Map<String, AuthData> authDatas = getAuthDatas(cloneConfig);
        //Provide empty list of libraries
        Map<String, Fileset> libraries = Collections.emptyMap();
        //Modify those datasources
        modifyDataSourcePropsForDatabase(datasources, authDatas, libraries, cloneConfig, serv, cont, false);
    }

    /**
     * For use when attempting to use <b>database rotation</b>. <br>
     *
     * Retrieves database specific properties from the provided JdbcDatabaseContainer, such as;
     * username, password, etc. <br>
     *
     * Using the ServerConfiguration API. Retrieves all &lt;dataSource&gt; elements and modifies
     * those that have the <b>fat.modify=true</b> attribute set. <br>
     *
     * This will replace the datasource &lt;derby.*.properties... &gt; with the generic properties
     * for the provided JdbcDatabaseContainer. <br>
     *
     * @see              com.ibm.websphere.simplicity.config.ServerConfiguration
     *
     * @param  serv      - LibertyServer server instance being used for this FAT suite.
     * @param  cont      - JdbcDatabaseContainer instance being used for database connectivity.
     *
     * @throws Exception
     */
    public static void setupDataSourceProperties(LibertyServer serv, JdbcDatabaseContainer<?> cont) throws Exception {
        //Skip for Derby and DerbyClient
        if (DatabaseContainerType.valueOf(cont) == DatabaseContainerType.Derby ||
            DatabaseContainerType.valueOf(cont) == DatabaseContainerType.DerbyClient)
            return; //Derby used by default no need to change DS properties

        //If a test suite legitimately wants to call this method outside of the Database Rotation SOE
        //Then we need to fail them on the IBMi SOE to avoid generic errors that arise when trying to infer datasource types.
        if (System.getProperty("os.name").equalsIgnoreCase("OS/400")) {
            throw new IllegalStateException("Attempting to modify the DataSource server configuration with a generic <properties /> element on an IBMi server. "
                                            + " IBMi ships with a JDK that has a DB2 driver globally available which means we cannot infer the datasource type. "
                                            + " Switch to use the setupDataSourceDatabaseProperties method.");
        }

        //Get server config
        ServerConfiguration cloneConfig = serv.getServerConfiguration().clone();
        //Get datasources to be changed
        Map<String, DataSource> datasources = getDataSources(cloneConfig);
        //Get authdatas to be changed
        Map<String, AuthData> authDatas = getAuthDatas(cloneConfig);
        //Provide empty list of libraries
        Map<String, Fileset> libraries = Collections.emptyMap();
        //Modify those datasources
        modifyDataSourcePropsGeneric(datasources, authDatas, libraries, cloneConfig, serv, cont);
    }

    /*
     * Helper method to get a list of datasources that need to be updated
     */
    private static Map<String, DataSource> getDataSources(ServerConfiguration cloneConfig) {
        //Get a list of datasources that need to be updated
        Map<String, DataSource> datasources = new HashMapWithNullKeys<>();

        //Get general datasources
        for (DataSource ds : cloneConfig.getDataSources())
            if (ds.getFatModify() != null && ds.getFatModify().equals("true"))
                datasources.put(ds.getId(), ds);

        //Get datasources that are nested under databasestores
        for (DatabaseStore dbs : cloneConfig.getDatabaseStores())
            for (DataSource ds : dbs.getDataSources())
                if (ds.getFatModify() != null && ds.getFatModify().equals("true"))
                    datasources.put(ds.getId(), ds);

        return datasources;
    }

    /*
     * Helper method to get a list of AuthData elements that need to be updated
     */
    private static Map<String, AuthData> getAuthDatas(ServerConfiguration cloneConfig) {
        //Get a list of authDatas that need to be updated
        Map<String, AuthData> authDatas = new HashMapWithNullKeys<>();

        //Get general authDatas
        for (AuthData ad : cloneConfig.getAuthDataElements())
            if (ad.getFatModify() != null && ad.getFatModify().equals("true"))
                authDatas.put(ad.getId(), ad);

        //Get authDatas that are nested under datasources
        for (DataSource ds : cloneConfig.getDataSources()) {
            ConfigElementList<AuthData> ads = new ConfigElementList<>();
            ads.addAll(ds.getContainerAuthDatas());
            ads.addAll(ds.getRecoveryAuthDatas());
            for (AuthData ad : ads)
                if (ad.getFatModify() != null && ad.getFatModify().equals("true"))
                    authDatas.put(ad.getId(), ad);
        }

        return authDatas;
    }

    /*
     * Helper method to get a list of Libraries that need to be modified
     */
    private static Map<String, Fileset> getLibraries(ServerConfiguration cloneConfig) {
        //Get a list of authDatas that need to be updated
        Map<String, Fileset> filesets = new HashMapWithNullKeys<>();

        //Get filesets
        for (Library lib : cloneConfig.getLibraries())
            for (Fileset fs : lib.getFilesets())
                if (fs.getIncludes().equals(DRIVER_REPLACEMENT))
                    filesets.put(lib.getId(), fs); //Reference library id here since it will be more recognizable

        return filesets;
    }

    /*
     * Creates generic properties for each database
     */
    private static void modifyDataSourcePropsGeneric(Map<String, DataSource> datasources, Map<String, AuthData> authDatas, Map<String, Fileset> libraries,
                                                     ServerConfiguration cloneConfig, LibertyServer serv,
                                                     JdbcDatabaseContainer<?> cont) throws Exception {
        //Get database type
        DatabaseContainerType type = DatabaseContainerType.valueOf(cont);

        if (!datasources.isEmpty()) {
            //Create general properties
            DataSourceProperties props = new Properties();
            props.setUser(cont.getUsername());
            props.setPassword(cont.getPassword());
            props.setServerName(cont.getHost());
            props.setPortNumber(Integer.toString(cont.getFirstMappedPort()));
            try {
                props.setDatabaseName(cont.getDatabaseName());
            } catch (UnsupportedOperationException e) {
                if (type.equals(DatabaseContainerType.SQLServer)) {
                    props.setDatabaseName("TEST");
                }
            }

            //TODO this should not be required even when using general datasource properties
            // investigating here: https://github.com/OpenLiberty/open-liberty/issues/10066
            if (type.equals(DatabaseContainerType.DB2)) {
                props.setExtraAttribute("driverType", "4");
            }

            if (type.equals(DatabaseContainerType.SQLServer)) {
                props.setExtraAttribute("selectMethod", "cursor");
            }

            if (type.equals(DatabaseContainerType.Oracle)) {
                Class<?> clazz = type.getContainerClass();
                Method getSid = clazz.getMethod("getSid");
                props.setDatabaseName((String) getSid.invoke(cont));
                props.setExtraAttribute("driverType", "thin");
            }

            // Same properties just without auth data when using containerAuthDataRef, or containerAuthData.
            DataSourceProperties noAuthProps = (DataSourceProperties) props.clone();
            noAuthProps.setUser(null);
            noAuthProps.setPassword(null);

            for (Map.Entry<String, DataSource> entry : datasources.entrySet()) {
                Log.info(c, "setupDataSourceProperties", "FOUND: DataSource to be enlisted in database rotation.  ID: " + entry.getKey());

                AuthLocation authLoc = whereIsAuthData(entry.getValue());
                switch (authLoc) {
                    case inAuthDataRef:
                        assertAuthDataWillBeModified(entry.getKey(), authLoc.id, authDatas);
                        entry.getValue().replaceDatasourceProperties(noAuthProps);
                        break;
                    case inEmbeddedAuthData:
                        assertAuthDataWillBeModified(entry.getKey(), authLoc.id, authDatas);
                        entry.getValue().replaceDatasourceProperties(noAuthProps);
                        break;
                    case inProperties:
                        entry.getValue().replaceDatasourceProperties(props);
                        break;
                    case none:
                        Log.warning(c,
                                    "The datasource (" + entry.getKey()
                                       + ") did not have username/password set in properties element, ContainerAuthData element, or ContainerAuthDataRef attribute. "
                                       + "We will assume you have configured a DeploymentDescriptor that DOES reference a AuthData element that has been modified.");
                        entry.getValue().replaceDatasourceProperties(noAuthProps);
                        break;
                    default:
                        //Replace default properties
                        entry.getValue().replaceDatasourceProperties(props);
                        break;

                }
            }
        }

        for (Map.Entry<String, AuthData> entry : authDatas.entrySet()) {
            Log.info(c, "setupDataSourceProperties", "FOUND: AuthData to be enlisted in database rotation.  ID: " + entry.getKey());
            //Replace derby auth data
            entry.getValue().setUser(cont.getUsername());
            entry.getValue().setPassword(cont.getPassword());
        }

        for (Map.Entry<String, Fileset> entry : libraries.entrySet()) {
            Log.info(c, "setupDataSourceProperties", "FOUND: Library to be enlisted in database rotation.  ID: " + entry.getKey());
            //Replace includes with driver name
            entry.getValue().setIncludes(type.getDriverName());
            //Add all permissions for driver
            JavaPermission driverPermission = new JavaPermission();
            driverPermission.setCodeBase("${shared.resource.dir}/jdbc/" + type.getDriverName());
            driverPermission.setClassName("java.security.AllPermission");
            cloneConfig.getJavaPermissions().add(driverPermission);
        }

        //Update config
        serv.updateServerConfiguration(cloneConfig);
    }

    private static final String DB_DRIVER = "DB_DRIVER";
    private static final String DB_USER = "DB_USER";
    private static final String DB_PASSWORD = "DB_PASSWORD";
    private static final String DB_HOST = "DB_HOST";
    private static final String DB_PORT = "DB_PORT";

    /*
     * Creates properties for specific database
     */
    private static void modifyDataSourcePropsForDatabase(Map<String, DataSource> datasources, Map<String, AuthData> authDatas, Map<String, Fileset> libraries,
                                                         ServerConfiguration cloneConfig, LibertyServer serv,
                                                         JdbcDatabaseContainer<?> cont, boolean envAuthConfig) throws Exception {
        //Get database type
        DatabaseContainerType type = DatabaseContainerType.valueOf(cont);

        if (!datasources.isEmpty()) {
            if (envAuthConfig) {
                // NOTE that we must set a default port here so that config doesn't complain about a non-number.
                // The default will get overridden by the ENV value later
                cloneConfig.getVariables().add(new Variable(DB_PORT, null, "9999"));
            }
            //Create properties based on type
            DataSourceProperties props = type.getDataSourceProps();
            props.setUser(envAuthConfig ? "${" + DB_USER + "}" : cont.getUsername());
            props.setPassword(envAuthConfig ? "${" + DB_PASSWORD + "}" : cont.getPassword());
            props.setServerName(envAuthConfig ? "${" + DB_HOST + "}" : cont.getHost());
            props.setPortNumber(envAuthConfig ? "${" + DB_PORT + "}" : Integer.toString(cont.getFirstMappedPort()));
            try {
                props.setDatabaseName(cont.getDatabaseName());
            } catch (UnsupportedOperationException e) {
                if (type.equals(DatabaseContainerType.SQLServer)) {
                    props.setDatabaseName("TEST");
                }
            }

            if (type.equals(DatabaseContainerType.Oracle)) {
                Class<?> clazz = type.getContainerClass();
                Method getSid = clazz.getMethod("getSid");
                props.setDatabaseName((String) getSid.invoke(cont));
            }

            // Same properties just without auth data when using containerAuthDataRef, or containerAuthData.
            DataSourceProperties noAuthProps = (DataSourceProperties) props.clone();
            noAuthProps.setUser(null);
            noAuthProps.setPassword(null);

            for (Map.Entry<String, DataSource> entry : datasources.entrySet()) {
                Log.info(c, "setupDataSourceProperties", "FOUND: DataSource to be enlisted in database rotation.  ID: " + entry.getKey());

                AuthLocation authLoc = whereIsAuthData(entry.getValue());
                switch (authLoc) {
                    case inAuthDataRef:
                        assertAuthDataWillBeModified(entry.getKey(), authLoc.id, authDatas);
                        entry.getValue().replaceDatasourceProperties(noAuthProps);
                        break;
                    case inEmbeddedAuthData:
                        assertAuthDataWillBeModified(entry.getKey(), authLoc.id, authDatas);
                        entry.getValue().replaceDatasourceProperties(noAuthProps);
                        break;
                    case inProperties:
                        entry.getValue().replaceDatasourceProperties(props);
                        break;
                    case none:
                        Log.warning(c,
                                    "The datasource (" + entry.getKey()
                                       + ") did not have username/password set in properties element, ContainerAuthData element, or ContainerAuthDataRef attribute. "
                                       + "We will assume you have configured a DeploymentDescriptor that DOES reference a AuthData element that has been modified.");
                        entry.getValue().replaceDatasourceProperties(noAuthProps);
                        break;
                    default:
                        //Replace default properties
                        entry.getValue().replaceDatasourceProperties(props);
                        break;

                }
            }
        }

        for (Map.Entry<String, AuthData> entry : authDatas.entrySet()) {
            Log.info(c, "setupDataSourceProperties", "FOUND: AuthData to be enlisted in database rotation.  ID: " + entry.getKey());
            //Replace derby auth data
            entry.getValue().setUser(envAuthConfig ? "${" + DB_USER + "}" : cont.getUsername());
            entry.getValue().setPassword(envAuthConfig ? "${" + DB_PASSWORD + "}" : cont.getPassword());
        }

        for (Map.Entry<String, Fileset> entry : libraries.entrySet()) {
            Log.info(c, "setupDataSourceProperties", "FOUND: Library to be enlisted in database rotation.  ID: " + entry.getKey());
            //Replace includes with driver name
            entry.getValue().setIncludes(type.getDriverName());
            //Add all permissions for driver
            JavaPermission driverPermission = new JavaPermission();
            driverPermission.setCodeBase("${shared.resource.dir}/jdbc/" + type.getDriverName());
            driverPermission.setClassName("java.security.AllPermission");
            cloneConfig.getJavaPermissions().add(driverPermission);
        }

        //Update config
        serv.updateServerConfiguration(cloneConfig);
    }

    /**
     * @param id
     * @param authDatas
     */
    private static void assertAuthDataWillBeModified(String dsID, String authID, Map<String, AuthData> authDatas) {
        if (!authDatas.containsKey(authID)) {
            throw new RuntimeException("The datasource (" + dsID + ") references an AuthData element (" + authID + ") but that element is not modifiable.");
        }
    }

    /**
     * Determine if a datasource has a username/password configured on a properties element.
     *
     * @param  datasource
     * @return            true - username/password found, false - assume some other auth data reference was provided.
     */
    private static AuthLocation whereIsAuthData(DataSource datasource) {
        //Ensure server config doesn't container multiple Properties or AuthDatas per datasource. Uncommon.
        if (datasource.getDataSourceProperties().size() > 1 || datasource.getContainerAuthDatas().size() > 1) {
            throw new RuntimeException("Database rotation cannot handle cases where multiple DataSource property or Container AuthData elements exist."
                                       + " This is uncommon configuration and shouldn't be tested on a rotating basis.");
        }

        for (DataSourceProperties props : datasource.getDataSourceProperties()) {
            if (props.getUser() != null || props.getPassword() != null) {
                return AuthLocation.inProperties.withID(props.getId());
            }
        }

        if (datasource.getContainerAuthDatas().size() == 1) {
            return AuthLocation.inEmbeddedAuthData.withID(datasource.getContainerAuthDatas().get(0).getId());
        }

        if (datasource.getContainerAuthDataRef() != null) {
            return AuthLocation.inAuthDataRef.withID(datasource.getContainerAuthDataRef());
        }

        return AuthLocation.none.withID("NONE");
    }

    // It is possible to have multiple DataSource / AuthData elements with null IDs.
    // Make sure they all get put into a HashMap so we can modify them.
    @SuppressWarnings("serial")
    private static final class HashMapWithNullKeys<V> extends HashMap<String, V> implements Map<String, V> {
        int nullCounter = 0;

        @Override
        public V put(String key, V value) {
            if (key == null) {
                key = "null" + nullCounter++;
            }
            return super.put(key, value);
        }
    }
}
