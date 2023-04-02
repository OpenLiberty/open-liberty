/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
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

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.utility.DockerImageName;

import componenttest.topology.database.DerbyNetworkUtilities;

/**
 * This is a Derby Client no-op database test container that is returned
 * when attempting to test against derby client.
 *
 * This class will start and stop a Derby Network instance (although locally, not in a container)
 */
class DerbyClientContainer extends JdbcDatabaseContainer<DerbyClientContainer> {

    private String user = "dbuser";
    private String pass = "dbpass";
    private String dbname = "memory:testdb";

    public DerbyClientContainer(DockerImageName image) {
        super("");
    }

    public DerbyClientContainer(String image) {
        super("");
    }

    public DerbyClientContainer() {
        super("");
    }

    @Override
    public String getDockerImageName() {
        return "";
    }

    @Override
    public void start() {
        try {
            DerbyNetworkUtilities.startDerbyNetwork();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doStart() {
        //DO NOTHING
    }

    @Override
    public void stop() {
        try {
            DerbyNetworkUtilities.stopDerbyNetwork();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        //DO NOTHING
    }

    @Override
    public String getJdbcUrl() {
        return "jdbc:derby://" + getHost() + ":" + getFirstMappedPort() + "/" + getDatabaseName();
    }

    @Override
    public DerbyClientContainer withUsername(String username) {
        user = username;
        return self();
    }

    @Override
    public String getUsername() {
        return user;
    }

    @Override
    public DerbyClientContainer withPassword(String password) {
        pass = password;
        return self();
    }

    @Override
    public String getPassword() {
        return pass;
    }

    @Override
    public DerbyClientContainer withDatabaseName(String dbName) {
        dbname = dbName;
        return self();
    }

    @Override
    public String getDatabaseName() {
        return dbname;
    }

    @Override
    public Integer getFirstMappedPort() {
        return 1527;
    }

    @Override
    public String getHost() {
        return "localhost";
    }

    @Override
    public String getDriverClassName() {
        return "org.apache.derby.jdbc.ClientDriver";
    }

    @Override
    protected String getTestQueryString() {
        throw new UnsupportedOperationException();
    }
}
