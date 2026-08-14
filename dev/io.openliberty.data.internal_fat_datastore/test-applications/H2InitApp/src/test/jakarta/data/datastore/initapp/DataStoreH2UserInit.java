/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package test.jakarta.data.datastore.initapp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import javax.sql.DataSource;

/**
 * Initializes H2 database users for all test databases.
 * This listener runs at application startup to create users that are needed
 * by other applications in the test bucket.
 */
@WebListener
public class DataStoreH2UserInit implements ServletContextListener {

    @Resource(lookup = "java:comp/DefaultDataSource")
    DataSource defaultDataSource;

    @Resource(lookup = "jdbc/ServerDataSource")
    DataSource serverDataSource;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("DataStoreH2UserInit: Starting database user initialization");
        
        try {
            // Initialize users for defaultdb (DefaultDataSource in server.xml)
            initializeDefaultDb();

            // Initialize users for serverdb (ServerDataSource in server.xml)
            initializeServerDb();
            
            // Initialize users for testdb (used by @DataSourceDefinition in all test applications)
            initializeTestDb();
            
            System.out.println("DataStoreH2UserInit: Database user initialization complete");
        } catch (Exception e) {
            System.err.println("DataStoreH2UserInit: Failed to initialize database users");
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize H2 database users", e);
        }
    }

    private void initializeDefaultDb() throws Exception {
        // Use the injected DefaultDataSource to create users for serverdb
        //try (Connection conn = defaultDataSource.getConnection();
        // additional users are not required for defaultdb
        //}
    }

    private void initializeServerDb() throws Exception {
        // Use the injected ServerDataSource to create users for serverdb
        try (Connection conn = serverDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Create users needed by test applications
            stmt.execute("CREATE USER IF NOT EXISTS resrefuser1 PASSWORD 'resrefpwd1' ADMIN");
            stmt.execute("CREATE USER IF NOT EXISTS resrefuser2 PASSWORD 'resrefpwd2' ADMIN");
            stmt.execute("CREATE USER IF NOT EXISTS resrefuser3 PASSWORD 'resrefpwd3' ADMIN");
            stmt.execute("CREATE USER IF NOT EXISTS resrefuser4 PASSWORD 'resrefpwd4' ADMIN");
            
            System.out.println("DataStoreH2UserInit: Created users for serverdb");
        }
    }

    private void initializeTestDb() throws Exception {
        String url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
        try (Connection conn = DriverManager.getConnection(url, "servletuser1", "servletpwd1");
             Statement stmt = conn.createStatement()) {
            
            // Create users needed by test applications
            stmt.execute("CREATE USER IF NOT EXISTS ejbuser1 PASSWORD 'ejbpwd1' ADMIN");
            stmt.execute("CREATE USER IF NOT EXISTS ejbuser2 PASSWORD 'ejbpwd2' ADMIN");
            
            System.out.println("DataStoreH2UserInit: Created users for testdb");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // No cleanup needed
    }
}
