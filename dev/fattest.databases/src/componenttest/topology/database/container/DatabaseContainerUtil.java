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

import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.DataSource;
import com.ibm.websphere.simplicity.config.DatabaseStore;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.dsprops.Properties;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

public final class DatabaseContainerUtil {
    //Logging Constants
    private static final Class<DatabaseContainerUtil> c = DatabaseContainerUtil.class;
    
    private DatabaseContainerUtil() {
    	//No objects should be created from this class
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
     * This will replace the datasource &lt;derby.embdedded.properties... &gt; with the corresponding properties
     * for the provided JdbcDatabaseContainer. <br>
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
        //Skip for Derby
    	if (DatabaseContainerType.valueOf(cont) == DatabaseContainerType.Derby)
            return; //Derby used by default no need to change DS properties

        //Get current server config
        ServerConfiguration cloneConfig = serv.getServerConfiguration().clone();

        //Get DataSource configs
        ConfigElementList<DataSource> dataSources = cloneConfig.getDataSources();

        //Inspect and modify datasource properties
        for (DataSource ds : dataSources) {
            if (ds.getFatModify() != null && ds.getFatModify().equalsIgnoreCase("true")) {
                modifyDataSourceProps(ds, cloneConfig, serv, cont);
            }
        }

        //Inspect and modify datasource properties that are nested under databasestore
        for (DatabaseStore dbs : cloneConfig.getDatabaseStores()) {
            for (DataSource ds : dbs.getDataSources()) {
                if (ds.getFatModify() != null && ds.getFatModify().equalsIgnoreCase("true")) {
                    modifyDataSourceProps(ds, cloneConfig, serv, cont);
                }
            }
        }
    }

    private static void modifyDataSourceProps(DataSource ds, ServerConfiguration cloneConfig, LibertyServer serv,
                                              JdbcDatabaseContainer<?> cont) throws CloneNotSupportedException, Exception {
        Log.info(c, "setupDataSourceProperties", "FOUND: DataSource to be enlisted in database rotation.  ID: " + ds.getId());

        //Create general properties
        Properties props = new Properties();
        props.setUser(cont.getUsername());
        props.setPassword(cont.getPassword());
        props.setURL(cont.getJdbcUrl());

        //TODO this should not be required even when using general datasource properties
        // investigating here: https://github.com/OpenLiberty/open-liberty/issues/10066
        if (DatabaseContainerType.valueOf(cont) == DatabaseContainerType.DB2) {
            props.setExtraAttribute("driverType", "4");
        }

        //Replace derby properties
        ds.clearDataSourceDBProperties();
        ds.getProperties().add(props);

        //Update config
        serv.updateServerConfiguration(cloneConfig);

    }

}
