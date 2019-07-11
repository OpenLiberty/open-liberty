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
package componenttest.topology.database;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;
import java.util.ServiceLoader;

import com.ibm.websphere.simplicity.config.DataSource;
import com.ibm.websphere.simplicity.config.DataSourceProperties;
import com.ibm.websphere.simplicity.config.DatabaseStore;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

public class DerbyEmbeddedUtilities {
    private static final Class<?> C = DerbyEmbeddedUtilities.class;

    public static void createDB(LibertyServer server, String dataSourceID) throws Exception {
        createDB(server, dataSourceID, null, null);
    }

    public static void createDB(LibertyServer server, String dataSourceID, String user, String pass) throws Exception {
        final String m = "createDB";

        DataSource ds = findDataSource(server, dataSourceID);
        DataSourceProperties dsProps = null;
        if (ds.getProperties_derby_embedded().size() > 0) {
            dsProps = ds.getProperties_derby_embedded().get(0);
        } else if (ds.getProperties().size() > 0) {
            dsProps = ds.getProperties().get(0);
        } else {
            throw new IllegalStateException("No <properties.derby.embedded> or generic <properites> found for dataSource=" + dataSourceID + " in configuration "
                                            + server.getServerConfigurationPath());
        }

        String dbLocation = dsProps.getDatabaseName();
        assertTrue("Cannot pre-warm an in-memory database: " + dbLocation, !dbLocation.contains("memory:"));
        dbLocation = resolveVariables(server, dbLocation);
        File dbPath = new File(dbLocation);
        Log.info(C, m, "Using DB path: " + dbPath.getAbsolutePath());

        Properties props = new Properties();
        if (user != null) {
            props.put("user", user);
            props.put("password", pass);
        } else if (dsProps.getUser() != null) {
            props.put("user", dsProps.getUser());
            props.put("password", dsProps.getPassword());
        }

        String connectionURL = "jdbc:derby:" + dbLocation;
        Driver derbyDriver = null;
        for (Driver driver : ServiceLoader.load(Driver.class)) {
            Log.info(C, "prewarm", "Found driver: " + driver);
            if (driver.acceptsURL(connectionURL)) {
                derbyDriver = driver;
                break;
            }
        }
        if (derbyDriver == null)
            throw new IllegalStateException("No Derby JDBC Driver was found on the classpath.");
        Connection conn = derbyDriver.connect(connectionURL + ";create=true", props);
        Log.info(C, m, "Got connection: " + conn);
        conn.close();
        try {
            conn = derbyDriver.connect(connectionURL + ";shutdown=true", props);
            throw new IllegalStateException("Derby did not successfully shutdown.");
        } catch (SQLException expected) {
            // derby always emits an SQLException on successful shutdown
        }
    }

    private static String resolveVariables(LibertyServer server, String str) {
        return str.replace("${server.config.dir}", server.getServerRoot())//
                        .replace("${shared.resource.dir}", server.getServerSharedPath() + "/resources");
    }

    private static DataSource findDataSource(LibertyServer server, String id) throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        DataSource ds = config.getDataSources().getById(id);
        if (ds != null)
            return ds;

        // Check if nested under other config elements
        for (DatabaseStore dsStore : config.getDatabaseStores()) {
            DataSource nested = dsStore.getDataSources().getById(id);
            if (nested != null)
                return nested;
        }

        throw new IllegalStateException("Unable to find <dataSource> with id=" + id + " in configuration " + server.getServerConfigurationPath());
    }

}
