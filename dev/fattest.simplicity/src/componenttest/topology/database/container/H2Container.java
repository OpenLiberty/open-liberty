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
package componenttest.topology.database.container;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.utility.DockerImageName;

import componenttest.topology.database.H2Database;

/**
 * This is a H2 database test container that is returned
 * when attempting to test against H2.
 *
 * This test container overrides the start and stop methods
 * to prevent the creation of a docker container.
 *
 * To configure the H2 database see {@link #withDatabase(H2Database)}
 */
public class H2Container extends JdbcDatabaseContainer<H2Container> {

    // Default admin user / password
    private final String user = "dbuser";
    private final String pass = "dbpass";

    private H2Database db = H2Database.create(user, pass);

    public H2Container(DockerImageName image) {
        // Calling super constructor like this since super("") doesn't compile with
        // Java 25 due to stricter annotation checking rules
        super(DockerImageName.parse(""));
    }

    public H2Container(String image) {
        // Calling super constructor like this since super("") doesn't compile with
        // Java 25 due to stricter annotation checking rules
        super(DockerImageName.parse(""));
    }

    public H2Container() {
        // Calling super constructor like this since super("") doesn't compile with
        // Java 25 due to stricter annotation checking rules
        super(DockerImageName.parse(""));
    }

    //// Allow user to configure container by providing the database object ////

    /**
     * Pass a {@link H2Database} configuration object to this container
     * which will call the before and after methods as part of the container
     * lifecycle.
     *
     * @param  db the database configuration
     * @return    this
     */
    public H2Container withDatabase(H2Database db) {
        this.db = db;
        return this;
    }

    //// Lifecycle methods ////

    @Override
    public void start() {
        db.before();
    }

    @Override
    protected void doStart() {
        //DO NOTHING
    }

    @Override
    public void stop() {
        db.after();
    }

    @Override
    public void close() {
        //DO NOTHING
    }

    //// Utility methods ///

    @Override
    public Connection createConnection(String queryString) throws SQLException, NoDriverFoundException {
        return db.createConnection(queryString);
    }

    @Override
    public Connection createConnection(String queryString, Properties info) throws SQLException, NoDriverFoundException {
        return db.createConnection(queryString, info);
    }

    //// Getters ////

    @Override
    public String getJdbcUrl() {
        return db.getURL();
    }

    @Override
    public String getUsername() {
        return db.getAdminUser();
    }

    @Override
    public String getPassword() {
        return db.getAdminPassword();
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
        return db.getDriverClassName();
    }

    @Override
    protected String getTestQueryString() {
        return db.getTestQueryString();
    }
}
