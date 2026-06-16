/*******************************************************************************
 * Copyright (c) 2021, 2026 IBM Corporation and others.
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
package web.dbrotation;

import static componenttest.annotation.SkipIfSysProp.DB_Not_Default;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;

import org.junit.Test;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.OnlyIfSysProp;
import componenttest.annotation.SkipIfSysProp;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/*")
public class DbRotationServlet extends FATServlet {

    @Resource(lookup = "jdbc/dbRotation")
    private DataSource ds_dbRotation;

    @Resource(lookup = "jdbc/dbRotationContainerAuth")
    private DataSource ds_dbRotationContAuth;

    @Resource(lookup = "jdbc/dbRotationNestedContainerAuth")
    private DataSource ds_dbRotationNestContAuth;

    @Resource(name = "jdbc/dbRotationDDAuthRef", lookup = "jdbc/dbRotationDDAuth")
    private DataSource ds_dbRotationDDAuth;

    @Resource(lookup = "jdbc/dbRotationNoAuth")
    private DataSource ds_dbRotationNoAuth;

    // Ensure that when running with repeats that the database is re-created and
    // a test like this one where a table is created and data is added does not result
    // in a "Table already exists" exception
    @Test
    public void testDatabaseRotation() throws Exception {
        try (Connection con = ds_dbRotation.getConnection()) {
            try (Statement stmt = con.createStatement()) {
                stmt.executeUpdate("CREATE TABLE test (id INTEGER NOT NULL PRIMARY KEY)");
            }
            try (PreparedStatement stmt = con.prepareStatement("INSERT INTO test (id) VALUES (?)")) {
                stmt.setInt(1, 10);
                stmt.executeUpdate();
            }
        }
    }

    // Ensure container auth was modified in Database rotation.
    @Test
    public void testDatabaseRotationWithContainerAuth() throws Exception {
        try (Connection con = ds_dbRotationContAuth.getConnection()) {
            con.getMetaData();
        }
    }

    // Ensure nested container auth was modified in Database rotation.
    @Test
    public void testDatabaseRotationWithNestedContainerAuth() throws Exception {
        try (Connection con = ds_dbRotationNestContAuth.getConnection()) {
            con.getMetaData();
        }
    }

    // Ensure container auth used in a Deployment Descriptor was modified in Database rotation.
    @Test
    public void testDatabaseRotationWithDDAuth() throws Exception {
        try (Connection con = ds_dbRotationDDAuth.getConnection()) {
            con.getMetaData();
        }
    }

    // Ensure database supports multiple open connections from different users
    // Note: only for default database Derby or H2, real databases will require additional setup
    @Test
    @SkipIfSysProp(DB_Not_Default)
    public void testDatabaseRotationWithMultipleOpenConnections() throws Exception {
        try (Connection con = ds_dbRotationNoAuth.getConnection("user1", "password2")) {
            try (Connection con2 = ds_dbRotationDDAuth.getConnection()) {
                con.getMetaData();
            }
        }
    }

    // Ensure database supports opening a connection using application managed auth
    // Note: only for default database Derby or H2, real databases will require additional setup
    @Test
    @SkipIfSysProp(DB_Not_Default)
    public void testDatabaseRotationWithProgrammaticAuth() throws Exception {
        try (Connection con = ds_dbRotationNoAuth.getConnection("user1", "password2")) {
            con.getMetaData();
        }
    }

    // Ensure database rejects opening a connection when no authentication was configured
    // Note: only for real databases
    // -- Derby allows a no-auth connection https://db.apache.org/derby/docs/10.2/devguide/cdevcsecure36127.html
    // -- H2 will allow a no-auth connection if it is the first connection to the database,
    //       and we cannot control the order of tests
    @Test
    @OnlyIfSysProp(DB_Not_Default)
    @AllowedFFDC() //Ignore all FFDCs for this test
    public void testDatabaseRotationWithNoAuth() throws Exception {
        try (Connection con = ds_dbRotationNoAuth.getConnection()) {
            con.getMetaData();
        } catch (SQLException e) {
            //No auth data was provided, therefore expect an error!
        }
    }
}
