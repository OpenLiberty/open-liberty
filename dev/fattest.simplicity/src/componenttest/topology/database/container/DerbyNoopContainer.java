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

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * This is a Derby no-op database test container that is returned
 * when attempting to test against derby embedded.
 *
 * This test container overrides the start and stop methods
 * to prevent the creation of a docker container.
 *
 */
class DerbyNoopContainer extends JdbcDatabaseContainer<DerbyNoopContainer> {

    public DerbyNoopContainer(DockerImageName image) {
        // Calling super constructor like this since super("") doesn't compile with
        // Java 25 due to stricter annotation checking rules
        super(DockerImageName.parse(""));
    }

    public DerbyNoopContainer(String image) {
        // Calling super constructor like this since super("") doesn't compile with
        // Java 25 due to stricter annotation checking rules
        super(DockerImageName.parse(""));
    }

    public DerbyNoopContainer() {
        // Calling super constructor like this since super("") doesn't compile with
        // Java 25 due to stricter annotation checking rules
        super(DockerImageName.parse(""));
    }

    @Override
    public String getDockerImageName() {
        return "";
    }

    @Override
    public void start() {
        //DO NOTHING
    }

    @Override
    protected void doStart() {
        //DO NOTHING
    }

    @Override
    public void stop() {
        //DO NOTHING
    }

    @Override
    public void close() {
        //DO NOTHING
    }

    @Override
    public String getJdbcUrl() {
        return "jdbc:derby:memory:test;create=true";
    }

    @Override
    public String getUsername() {
        return "dbuser1";
    }

    @Override
    public String getPassword() {
        return "{xor}Oz0vKDtu";
    }

    @Override
    public Integer getFirstMappedPort() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getHost() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDriverClassName() {
        return "org.apache.derby.jdbc.AutoloadedDriver";
    }

    @Override
    protected String getTestQueryString() {
        return "";
    }
}
