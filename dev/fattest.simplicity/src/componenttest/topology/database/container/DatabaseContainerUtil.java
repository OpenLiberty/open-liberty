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

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.config.AuthData;
import com.ibm.websphere.simplicity.config.ConfigElement;
import com.ibm.websphere.simplicity.config.DataSource;
import com.ibm.websphere.simplicity.config.DataSourceProperties;
import com.ibm.websphere.simplicity.config.DatabaseStore;
import com.ibm.websphere.simplicity.config.Fileset;
import com.ibm.websphere.simplicity.config.JavaPermission;
import com.ibm.websphere.simplicity.config.Library;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.dsprops.Properties;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

/**
 * <pre>
 * When using <b>database rotation</b> the server configuration needs to be updated
 * for the database which is currenlty under test. This class updates server
 * configuration by following this routine:
 *
 * 1. Retrieves database specific properties from the provided JdbcDatabaseContainer, such as;
 * databaseName, server, port, username, password, etc.
 *
 * 2. Using the ServerConfiguration API.
 * Retrieves all {@code <dataSource>}, {@code <authData>}, and {@code<library>} elements
 * and modifies those that have the <b>fat.modify=true</b> attribute set. <br>
 *
 * 3. Replace the dataSource {@code <properties.derby.[embedded | client] ...>}
 * with the generic {@code <properties ...>} or specific {@code <properties.[database] ...>}
 * element for the provided JdbcDatabaseContainer. <br>
 *
 * </pre>
 *
 * @see com.ibm.websphere.simplicity.config.ServerConfiguration
 */
//TODO Change class to DatabaseContainerProperties
public final class DatabaseContainerUtil {
    //Logging Constants
    private static final Class<DatabaseContainerUtil> c = DatabaseContainerUtil.class;

    //Driver replacement key
    private static String DRIVER_KEY = "DB_DRIVER";
    private static String USER_KEY = "DB_USER";
    private static String PASS_KEY = "DB_PASS"; 

    private static final String toReplacementString(String key) {
        return "${env." + key + "}";
    }

    //Required fields
    private final LibertyServer server;
    private final ServerConfiguration serverClone;
    private final JdbcDatabaseContainer<?> databaseCont;
    private final DatabaseContainerType databaseType;
    private final boolean isModifiable;

    //Optional fields
    private boolean useGeneric = true;

    //Required updates
    private final Set<DataSource> datasources;
    private final Set<AuthData> authDatas;

    //Optional updates
    private Map<String, Fileset> libraries = Collections.emptyMap();
    private Set<JavaPermission> permissions = Collections.emptySet();

    ///// Constructor /////
    private DatabaseContainerUtil(LibertyServer serv, JdbcDatabaseContainer<?> cont) throws Exception {
        this.server = Objects.requireNonNull(serv);
        this.serverClone = server.getServerConfiguration().clone();

        this.databaseCont = Objects.requireNonNull(cont);
        this.databaseType = DatabaseContainerType.valueOf(databaseCont);

        boolean isDerby = DatabaseContainerType.valueOf(databaseCont) == DatabaseContainerType.Derby ||
                          DatabaseContainerType.valueOf(databaseCont) == DatabaseContainerType.DerbyClient;

        if (isDerby) {
            this.datasources = Collections.emptySet();
            this.authDatas = Collections.emptySet();
            this.isModifiable = false;
            return;
        }

        //Get a list of datasources that need to be updated
        Set<DataSource> dsSet = new HashSet<>(); 
        
        //Get general dataSources
        dsSet.addAll(serverClone.getDataSources());
        
        //Get datasources that are nested under databasestores
        for (DatabaseStore dbs : serverClone.getDatabaseStores()) {
            dsSet.addAll(dbs.getDataSources());
        }
        
        this.datasources = dsSet.stream()
                        .filter(ds -> ds.getFatModify() != null)
                        .filter(ds -> ds.getFatModify().equalsIgnoreCase("true"))
                        .collect(Collectors.toSet());

        //Get a set of distinct authDatas that could be updated
        this.authDatas = this.datasources.stream()
                        .flatMap(ds -> findAuthDataLocations(ds).stream())
                        .distinct()
                        .collect(Collectors.toSet());
        
        //TODO what about authData elements inside a <databaseStore> element?
        
        //If there is nothing to modify, this is not modifiable
        this.isModifiable = !this.datasources.isEmpty() || !this.authDatas.isEmpty();
    }

    ///// Builder /////

    /**
     * @param  serv LibertyServer server instance being used for this FAT suite.
     * @param  cont JdbcDatabaseContainer instance being used for database connectivity.
     * @return      instance of DatabaseContainerUtil
     */
    public static DatabaseContainerUtil build(LibertyServer server, JdbcDatabaseContainer<?> cont) {
        try {
            DatabaseContainerUtil instance = new DatabaseContainerUtil(server, cont);
            Log.info(c, "build", instance.toString());
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Failure while building database container util", e);
        }
    }

    ///// Configuration /////

    /**
     * Performs substitution of the library element with the name of the driver for the database used at runtime.
     * This is helpful during checkpoint tests since the JVM will return null for environment variables before checkpoint.
     *
     * Example (${env.DB_DRIVER} will be replaced):
     *
     * <pre>
     * <code>
     *   &lt;library id="JDBCLibrary" &gt;
     *     &lt;fileset dir="${shared.resource.dir}/jdbc" includes="${env.DB_DRIVER}" /&gt;
     *   &lt;/library&gt;
     * </code>
     * </pre>
     *
     * TODO consider requiring fat.modify = true for this replacement
     * @return this
     */
    public DatabaseContainerUtil withDriverReplacement() {
         this.libraries = new HashMap<>();

        //Get filesets
        for (Library lib : serverClone.getLibraries())
            for (Fileset fs : lib.getFilesets())
                if (fs.getIncludes().equals(toReplacementString(DRIVER_KEY)))
                    this.libraries.put(getElementId(lib), fs); //Reference library id here since it will be more recognizable
        
        this.permissions = serverClone.getJavaPermissions().stream()
                        .filter(p -> p.getCodeBase().contains(toReplacementString(DRIVER_KEY)))
                        .collect(Collectors.toSet());

        return this;
    }

    /**
     * Adds the environment variable `DB_DRIVER` to the server to avoid having to modify the library element.
     *
     * Example:
     *
     * <pre>
     * <code>
     *   &lt;library id="JDBCLibrary"&gt;
     *     &lt;fileset dir="${shared.resource.dir}/jdbc" includes="${env.DB_DRIVER}" /&gt;
     *   &lt;/library&gt;
     * </code>
     * </pre>
     *
     * @return this
     */
    public DatabaseContainerUtil withDriverVariable() {
        this.server.addEnvVar("DB_DRIVER", databaseType.getDriverName());
        return this;
    }

    /**
     * Adds the additional environment variables `DB_USER` and `DB_PASS` to the server to allow for
     * customized user/password variables that are not the system/admin authentication data.
     *
     * @param  user a database user
     * @param  pass a database user's password
     *
     * @return      this
     */
    public DatabaseContainerUtil withAuthVariables(String user, String pass) {
        this.server.addEnvVar("DB_USER", user);
        this.server.addEnvVar("DB_PASS", pass);
        return this;
    }

    /**
     * Performs substitution of the dataSource element {@code <properties.derby.embedded ... />}
     * with a set of generic properties {@code <properties .../>}
     *
     * NOTE: this is the default behavior
     *
     * @return this
     */
    public DatabaseContainerUtil withGenericProperties() {
        useGeneric = true;
        return this;
    }

    /**
     * Performs substitution of the dataSource element {@code <properties.derby.embedded ... />}
     * with a set of specific properties for the database under test {@code <properties.[database] .../>}
     *
     * @return this
     */
    public DatabaseContainerUtil withDatabaseProperties() {
        useGeneric = false;
        return this;
    }

    ///// Termination /////
    public void modify() throws Exception {
        
        final String m = "modify";
        
        //Skip modify if there is nothing to modify
        if (!isModifiable) {
            Log.info(c, m, "Nothing was found to be modifiable and therefore we will skip the modify step.");
            return;
        }
        
        //If a test suite legitimately wants to call this method outside of the Database Rotation SOE
        //Then we need to fail them on the IBMi SOE to avoid generic errors that arise when trying to infer datasource types.
        if (useGeneric && System.getProperty("os.name").equalsIgnoreCase("OS/400")) {
            throw new IllegalStateException("Attempting to modify the DataSource server configuration with a generic <properties /> element on an IBMi server. "
                                            + " IBMi ships with a JDK that has a DB2 driver globally available which means we cannot infer the datasource type. "
                                            + " Switch to use the setupDataSourceDatabaseProperties method.");
        }

        // modify datasources
        if (!datasources.isEmpty()) {
            
            //Create generic or specific properties
            DataSourceProperties commonProps = useGeneric ? new Properties() : databaseType.getDataSourceProps();
            
            //Common configuration
            commonProps.setServerName(databaseCont.getHost());
            commonProps.setPortNumber(Integer.toString(databaseCont.getFirstMappedPort()));
            try {
                commonProps.setDatabaseName(databaseCont.getDatabaseName());
            } catch (UnsupportedOperationException e) {
                if (databaseType.equals(DatabaseContainerType.SQLServer)) {
                    commonProps.setDatabaseName("TEST");
                }
            }

            //Specific configuration
            if (useGeneric) {
                //TODO this should not be required even when using general datasource properties
                // investigating here: https://github.com/OpenLiberty/open-liberty/issues/10066
                if (databaseType.equals(DatabaseContainerType.DB2)) {
                    commonProps.setExtraAttribute("driverType", "4");
                }

                if (databaseType.equals(DatabaseContainerType.SQLServer)) {
                    commonProps.setExtraAttribute("selectMethod", "cursor");
                }

                if (databaseType.equals(DatabaseContainerType.Oracle)) {
                    Class<?> clazz = databaseType.getContainerClass();
                    Method getSid = clazz.getMethod("getSid");
                    commonProps.setDatabaseName((String) getSid.invoke(databaseCont));
                    commonProps.setExtraAttribute("driverType", "thin");
                }
            } else {
                if (databaseType.equals(DatabaseContainerType.Oracle)) {
                    Class<?> clazz = databaseType.getContainerClass();
                    Method getSid = clazz.getMethod("getSid");
                    commonProps.setDatabaseName((String) getSid.invoke(databaseCont));
                }
            }

            //Update DataSources
            for (DataSource ds : datasources) {
                Log.info(c, m, "FOUND: DataSource to be enlisted in database rotation. ID: " + getElementId(ds));

                if(ds.getDataSourceProperties().size() != 1) {
                    throw new RuntimeException("Expected exactly one set of DataSoure properties for DataSource: " + getElementId(ds));
                }
                
                //Make a clone of common props and determine username/password
                DataSourceProperties clone = (DataSourceProperties) commonProps.clone();
                
                for(DataSourceProperties originalProps : ds.getDataSourceProperties()) {
                    if(canUpdate(originalProps)) {
                        clone.setUser(databaseCont.getUsername());
                        clone.setPassword(databaseCont.getPassword());
                    } else {
                        clone.setUser(originalProps.getUser());
                        clone.setPassword(originalProps.getPassword());
                    }
                }
                
                //Replace dataSource properties
                ds.replaceDatasourceProperties(clone);
            }
        }

        // Modify authDatas
        for (AuthData ad : authDatas) {
            if(!canUpdate(ad)) {
                Log.info(c, m, "SKIP: AuthData cannot be enlisted in database rotation. ID: " + getElementId(ad));
                continue;
            }
            
            Log.info(c, m, "FOUND: AuthData to be enlisted in database rotation.  ID: " + getElementId(ad));

            ad.setUser(databaseCont.getUsername());
            ad.setPassword(databaseCont.getPassword());
        }

        // Modify libraries
        for (Map.Entry<String, Fileset> entry : libraries.entrySet()) {
            Log.info(c, m, "FOUND: Library to be enlisted in database rotation.  ID: " + entry.getKey());
            
            //Replace includes with driver name
            entry.getValue().setIncludes(databaseType.getDriverName());
        }
        
        // Modify permissions
        for (JavaPermission permission : permissions) {
            Log.info(c, m, "FOUND: Permission to be enlisted in database rotation. ID: " + getElementId(permission));
            
            String codeBase = permission.getCodeBase();
            permission.setCodeBase(codeBase.replace(toReplacementString(DRIVER_KEY), databaseType.getDriverName()));
        }

        //Update config
        server.updateServerConfiguration(serverClone);
    }

    ///// Deprecated static methods ////

    /**
     * Instead use:
     * <code>
     * DatabaseContainerUtil.build(server, cont).withDriverReplacement().withDatabaseProperties().modify();
     * </code>
     */
    @Deprecated //TODO remove once Websphere Liberty repository is updated
    public static void setupDataSourcePropertiesForCheckpoint(LibertyServer serv, JdbcDatabaseContainer<?> cont) throws CloneNotSupportedException, Exception {
        DatabaseContainerUtil.build(serv, cont).withDriverReplacement().withDatabaseProperties().modify();
    }

    /**
     * Instead use:
     * <code>
     * DatabaseContainerUtil.build(server, cont).withDatabaseProperties().modify();
     * </code>
     */
    @Deprecated //TODO remove once Websphere Liberty repository is updated
    public static void setupDataSourceDatabaseProperties(LibertyServer serv, JdbcDatabaseContainer<?> cont) throws CloneNotSupportedException, Exception {
        DatabaseContainerUtil.build(serv, cont).withDatabaseProperties().modify();
    }

    /**
     * Instead use:
     * <code>
     * DatabaseContainerUtil.build(server, cont).modify();
     * </code>
     */
    @Deprecated
    public static void setupDataSourceProperties(LibertyServer serv, JdbcDatabaseContainer<?> cont) throws Exception {
        DatabaseContainerUtil.build(serv, cont).modify();
    }

    ///// Helper methods /////
    /**
     * Helper method gets the id of a configuration element, or generates a unique id based on that element
     */
    private String getElementId(ConfigElement element) {
        return element.getId() == null ? //
               element.getClass().getSimpleName() + "@" + Integer.toHexString(element.toString().hashCode()) : //
               element.getId();
    }
    
    
    /**
     * <pre>
     * Determine if we can update AuthData based on the following checks:
     * 
     * 1. The original AuthData set fat.modify to true (not null, false, or any other string)
     * 2. The original AuthData did not contain the username ${env.DB_USER} and password ${env.DB_PASS}
     * </pre>
     * 
     * @param props The original AuthData element
     * @return true if the original AuthData can be updated, false otherwise.
     */
    private boolean canUpdate(AuthData ad) {
        if(ad.getFatModify() == null || //
           ad.getFatModify().equalsIgnoreCase("false")) {
            return false;
        }
        
        if(ad.getUser().equalsIgnoreCase(toReplacementString(USER_KEY)) && //
           ad.getPassword().equalsIgnoreCase(toReplacementString(PASS_KEY))) {
            return false;
        }
        
        return ad.getFatModify().equalsIgnoreCase("true");
    }
    
    /**
     * <pre>
     * Determine if we can update DataSourceProperties based on the following checks:
     * 
     * 1. The original DataSourceProperties contained a username and password
     * 2. The original DataSourceProperties did not contain the username ${env.DB_USER} and password ${env.DB_PASS}
     * </pre>
     * 
     * @param props The original DataSourceProperties element
     * @return true if the original DataSourceProperties can be updated, false otherwise.
     */
    private boolean canUpdate(DataSourceProperties props) {
        if(props.getUser() == null && props.getPassword() == null) {
            return false;
        }
        
        if(props.getUser().equalsIgnoreCase(toReplacementString(USER_KEY)) && //
                        props.getPassword().equalsIgnoreCase(toReplacementString(PASS_KEY))) {
            return false;
        }
        
        return true;
    }

    /**
     * <pre>
     * Authentication data for a dataSource can be found in any of the following locations:
     * 
     * 1. Within the {@code <containerAuthData... />} element of the dataSource.
     * 2. Within the {@code <recoveryAuthData... />} element of the dataSource.
     * 3. Within the {@code <dataSource containerAuthDataRef... /> attribute of the dataSource
     * 
     * </pre>
     * 
     * @param ds The dataSource
     * @return a set of AuthDatas that the dataSource references or contains.
     */
    private Set<AuthData> findAuthDataLocations(DataSource ds) {
        
        Set<AuthData> authDataElements = new HashSet<>();
        
        authDataElements.addAll(ds.getContainerAuthDatas());
        
        if(ds.getContainerAuthDataRef() != null) {
            authDataElements.add(serverClone.getAuthDataElements().getById(ds.getContainerAuthDataRef()));
        }
        
        if(ds.getRecoveryAuthDataRef() != null) {
            authDataElements.add(serverClone.getAuthDataElements().getById(ds.getRecoveryAuthDataRef()));
        }
        
        return authDataElements;
    }

    @Override
    public String toString() {
        return "DatabaseContainerUtil"
                        + System.lineSeparator() + "[server=" + server.getServerName() + ", databaseType=" + databaseType + ", isModifiable=" + isModifiable 
                        + System.lineSeparator() + "datasources=" + datasources.stream().map(ds -> getElementId(ds)).collect(Collectors.toList())
                        + System.lineSeparator() + "authDatas=" + authDatas.stream().map(ad -> getElementId(ad)).collect(Collectors.toList()) + "]";
    }
    
    
}
