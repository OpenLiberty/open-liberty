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
package com.ibm.ws.concurrent.persistent.fat.locking;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MSSQLServerContainer;

import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.DataSource;
import com.ibm.websphere.simplicity.config.DatabaseStore;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.dsprops.Properties;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

public class DatabaseContainerUtil {
    //Logging Constants
    private static final Class<DatabaseContainerUtil> c = DatabaseContainerUtil.class;

    /**
     * Method that does any database setup necessary prior to making a connection
     * to the database from a servlet.
     *
     * @param cont         - Test container being used.
     * @param databaseName - Name to be given to database instance.
     * @throws SQLException - Thrown if there is a failure during connection or initialization.
     */
    private static void initDatabase(JdbcDatabaseContainer<?> cont) throws SQLException {
        if (cont instanceof MSSQLServerContainer) {
            //Create database
            try (Connection conn = cont.createConnection("")) {
                Statement stmt = conn.createStatement();
                stmt.execute("CREATE DATABASE [TEST];");
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
        if (DatabaseContainerType.valueOf(cont) == DatabaseContainerType.Derby)
            return; //Derby used by default no need to change DS properties

        initDatabase(cont);

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
